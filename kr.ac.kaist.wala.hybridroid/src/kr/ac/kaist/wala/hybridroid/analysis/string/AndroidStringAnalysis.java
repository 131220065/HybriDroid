/*******************************************************************************
* Copyright (c) 2016 IBM Corporation and KAIST.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
* KAIST - initial API and implementation
*******************************************************************************/
package kr.ac.kaist.wala.hybridroid.analysis.string;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.jar.JarFile;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.dalvik.classLoader.DexFileModule;
import com.ibm.wala.dalvik.ipa.callgraph.impl.AndroidEntryPoint;
import com.ibm.wala.dalvik.util.AndroidEntryPointLocator;
import com.ibm.wala.dalvik.util.AndroidEntryPointLocator.LocatorFlags;
import com.ibm.wala.examples.drivers.PDFCallGraph;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.ClassHierarchyClassTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.strings.Atom;

import kr.ac.kaist.wala.hybridroid.analysis.FieldDefAnalysis;
import kr.ac.kaist.wala.hybridroid.analysis.HybridCFGAnalysis;
import kr.ac.kaist.wala.hybridroid.analysis.resource.AndroidResourceAnalysis;
import kr.ac.kaist.wala.hybridroid.analysis.string.constraint.ConstraintGraph;
import kr.ac.kaist.wala.hybridroid.analysis.string.constraint.ConstraintVisitor;
import kr.ac.kaist.wala.hybridroid.analysis.string.constraint.IBox;
import kr.ac.kaist.wala.hybridroid.analysis.string.constraint.IConstraintNode;
import kr.ac.kaist.wala.hybridroid.analysis.string.constraint.VarBox;
import kr.ac.kaist.wala.hybridroid.analysis.string.constraint.solver.ForwardSetSolver;
import kr.ac.kaist.wala.hybridroid.analysis.string.constraint.solver.domain.value.IStringValue;
import kr.ac.kaist.wala.hybridroid.analysis.string.constraint.solver.domain.value.IValue;
import kr.ac.kaist.wala.hybridroid.analysis.string.constraint.solver.domain.value.StringBotValue;
import kr.ac.kaist.wala.hybridroid.analysis.string.constraint.solver.domain.value.StringTopValue;
import kr.ac.kaist.wala.hybridroid.analysis.string.model.StringModel;
import kr.ac.kaist.wala.hybridroid.callgraph.AndroidMethodTargetSelector;
import kr.ac.kaist.wala.hybridroid.callgraph.ResourceCallGraphBuilder;
import kr.ac.kaist.wala.hybridroid.models.AndroidHybridAppModel;
import kr.ac.kaist.wala.hybridroid.util.data.Pair;
import nju.hzq.patch.AndroidStringAnalysisPatch;
import nju.hzq.patch.HzqHybridTools;
import nju.hzq.patch.JavaCallGraphTools;
import nju.hzq.stub.HzqStub;

/**
 * 
 * @author Sungho Lee
 */
public class AndroidStringAnalysis implements StringAnalysis{
	public static boolean DEBUG = false;
	private AnalysisScope scope;
	private WorkList worklist;
	private List<Hotspot> hotspots;
	private Set<IBox> spotBoxSet;
	private Map<IConstraintNode, Set<String>> result;
	private Map<Hotspot, Set<HotspotDescriptor>> descMap;
	private BridgeInfo bi;
	private AndroidResourceAnalysis ara;
	private CallGraph cg;
	private PointerAnalysis<InstanceKey> pa;
	
	public AndroidStringAnalysis(){
		scopeInit();
		worklist = new WorkList();
		result = new HashMap<IConstraintNode, Set<String>>();
		descMap = new HashMap<Hotspot, Set<HotspotDescriptor>>();
		bi = new BridgeInfo();
	}
	
	public AndroidStringAnalysis(AndroidResourceAnalysis ra){
		this();
		this.ara = ra;
		StringModel.setResourceAnalysis(ra, null);
	}
	
	private void scopeInit(){
		scope = AnalysisScope.createJavaAnalysisScope();
		scope.setLoaderImpl(ClassLoaderReference.Primordial, "com.ibm.wala.dalvik.classLoader.WDexClassLoaderImpl");
		scope.setLoaderImpl(ClassLoaderReference.Application, "com.ibm.wala.dalvik.classLoader.WDexClassLoaderImpl");
	}

	public void setExclusion(String exclusions){
		File exclusionsFile = new File(exclusions);

		try {
			InputStream fs = exclusionsFile.exists() ? new FileInputStream(exclusionsFile) : AndroidHybridAppModel.class.getResourceAsStream(exclusions);
			scope.setExclusions(new FileOfClasses(fs));
			fs.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void addAnalysisScope(String path) {
		if (path.endsWith(".apk")) {
			try {
				List<String> dexFilePaths = AndroidStringAnalysisPatch.unzipDexFiles(path, ara.getDir());
				if(dexFilePaths.size() > 1) {
					System.out.println("妫�娴嬪埌multiDex :");
					for(int i = 0; i < dexFilePaths.size(); i++) {
						System.out.println("搴忓彿 = " + i + " : " + dexFilePaths.get(i));
					}
					/*System.out.println("璇疯緭鍏ュ簭鍙烽�夋嫨鍏朵腑涓�涓猟ex杩涜鍒嗘瀽锛屾垨鑰呰緭鍏�-1閫夋嫨鍏ㄩ儴dex鏂囦欢杩涜鍒嗘瀽锛�");
					int index = new Scanner(System.in).nextInt();
					while(index >= dexFilePaths.size() || index < -1) {
						System.out.println("杈撳叆搴忓彿瓒呭嚭鑼冨洿,璇烽噸鏂拌緭鍏�");
						index = new Scanner(System.in).nextInt();
					}*/
					int index = -1;
					if(index == -1) {
						System.out.println("鍒嗘瀽鍏ㄩ儴dex鏂囦欢");
						for (String dexFilePath : dexFilePaths) {
							scope.addToScope(ClassLoaderReference.Application, DexFileModule.make(new File(dexFilePath)));
						}
					} else {
						System.out.println("浠呭垎鏋愭枃浠�" + dexFilePaths.get(index));
						scope.addToScope(ClassLoaderReference.Application, DexFileModule.make(new File(dexFilePaths.get(index))));
					}
				} else if(dexFilePaths.size() == 1){
					scope.addToScope(ClassLoaderReference.Application, DexFileModule.make(new File(dexFilePaths.get(0))));
				} else {
					Assertions.UNREACHABLE();
				}
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			
		} else {
			throw new InternalError("Support only apk format as target file");
		}
	}
	
//	@Override
//	public void addAnalysisScope(String path){
//		// TODO Auto-generated method stub
//		if(path.endsWith(".apk")){
//			try {
//				scope.addToScope(ClassLoaderReference.Application, DexFileModule.make(new File(path)));
//			} catch (IllegalArgumentException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}else{
//			throw new InternalError("Support only apk format as target file");	
//		}
//	}

	public void setupAndroidLibs(String... libs){
		try{
			for(String lib : libs){
				if(lib.endsWith(".dex"))
					scope.addToScope(ClassLoaderReference.Primordial, DexFileModule.make(new File(lib)));
				else if(lib.endsWith(".jar"))
					scope.addToScope(ClassLoaderReference.Primordial, new JarFileModule(new JarFile(new File(lib))));
			}
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void solve(ConstraintGraph cg){
		ForwardSetSolver fss = new ForwardSetSolver();
		Map<IConstraintNode, IValue> res = fss.solve(cg);
		for(IBox n : spotBoxSet){
			IValue v = res.get(n);
			if(v instanceof IStringValue && (v instanceof StringTopValue) == false && (v instanceof StringBotValue) == false){
				result.put(n, (Set<String>)v.getDomain().getOperator().gamma(v));
			}
		}
	}
	
	@Override
	public void analyze(List<Hotspot> hotspots) throws ClassHierarchyException, IllegalArgumentException, CallGraphBuilderCancelException {
		// TODO Auto-generated method stub
		this.hotspots = hotspots;
		
		IClassHierarchy cha = AndroidStringAnalysisPatch.subClassOfWebViewPatch(hotspots, scope);
		
		/*for(IClass klass : cha) {
			for(IMethod method : klass.getAllMethods()) {
				HybridCFGAnalysis.hybridCache.getIR(method);
			}
		}
		
		System.out.println("寤虹珛鎵�鏈夌殑IR瀹屾垚");
		new Scanner(System.in).nextInt();*/
		
		if(cg == null || pa == null){
			Pair<CallGraph, PointerAnalysis<InstanceKey>> p = buildCG(cha);
			cg = p.fst();
			pa = p.snd();
			//PDFCallGraph.runGraph(cg);
		}
		
		Set<IBox> boxSet = findHotspots(cg, pa, hotspots);
		this.spotBoxSet = boxSet;
		IBox[] boxes = boxSet.toArray(new IBox[0]);

		System.err.println("Field Def analysis...");
		FieldDefAnalysis fda = new FieldDefAnalysis(cg, pa);

		System.err.println("Build Constraint Graph...");
		IBox[] targets = boxes;
		ConstraintGraph graph = buildConstraintGraph(cg, fda, targets);
		
		System.err.println("Optimize Constraint Graph...");
		graph.optimize();
		
		System.err.println("Solving the constraints...");
		solve(graph);
		makeDescriptors();
		collectBridgeInfo(cg, pa);
	}
	
	private void collectBridgeInfo(CallGraph cg, PointerAnalysis<InstanceKey> pa){
		final TypeReference wvType = TypeReference.find(ClassLoaderReference.Application, "Landroid/webkit/WebView");
		final Selector addJSSelector = Selector.make("addJavascriptInterface(Ljava/lang/Object;Ljava/lang/String;)V");
		IClassHierarchy cha = cg.getClassHierarchy();
		IClass wvClass = cha.lookupClass(wvType);
		File file = new File(AndroidHybridAppModel.class.getClassLoader().getResource("hzqbridge.js").getFile());
		java.io.FileWriter fw = null;
		try {
			fw = new java.io.FileWriter(file);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		for(CGNode n : cg){
			IR ir = n.getIR();
			if(ir == null)
				continue;
			
			for(Iterator<CallSiteReference> icsr = ir.iterateCallSites(); icsr.hasNext();){
				for(SSAAbstractInvokeInstruction callInst : ir.getCalls(icsr.next())){
					MethodReference mr = callInst.getDeclaredTarget();
					IClass receiver = cha.lookupClass(mr.getDeclaringClass());
					if((receiver != null && (receiver.equals(wvClass) || cha.isSubclassOf(receiver, wvClass))) && mr.getSelector().equals(addJSSelector)){
						int bridge = callInst.getUse(1);
						int name = callInst.getUse(2);
						String bname = null;//hzq: add
						SymbolTable st = n.getIR().getSymbolTable();
						if(st.isConstant(name)) {
							bname = (String) st.getConstantValue(name);
						} else {
							HzqStub.stubPrint("bridge name is not constant");
						}
						PointerKey pk = pa.getHeapModel().getPointerKeyForLocal(n, bridge);
						for(InstanceKey ik : pa.getPointsToSet(pk)){
							TypeReference tr = ik.getConcreteType().getReference();
							try {
								
								for(IMethod m : ik.getConcreteType().getAllMethods()) {
									if(HzqHybridTools.isBridgeMethod(m))
										fw.write(bname + "." + m.getName() + "();\n");
								}
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							bi.addBridge(n, bridge, tr);
//							for(Iterator<com.ibm.wala.util.collections.Pair<CGNode, NewSiteReference>> ip = ik.getCreationSites(cg); ip.hasNext(); ){
//								com.ibm.wala.util.collections.Pair<CGNode, NewSiteReference> p = ip.next();
//								bi.addBridge(n, bridge, tr, p.fst, p.snd);
//							}
						}
					}
				}
			}
		}
		try {
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void makeDescriptors(){
		Set<HotspotDescriptor> descSet = getAllDescriptors();
		
		for(IConstraintNode n : result.keySet()){
			for(HotspotDescriptor desc : descSet){
				if(desc.getConstNode().equals(n)){
					desc.setValues(result.get(n));
				}
			}
		}
	}
	
	public List<Hotspot> getSpots(){
		return hotspots;
	}
	
	@SuppressWarnings("unchecked")
	public Map<IBox, Set<String>> getSpotString(){
		Map<IBox, Set<String>> res = new HashMap<IBox, Set<String>>();
		for(IBox b : spotBoxSet){
			VarBox var = (VarBox) b;
			Object v = getValue(var.getNode(), var.getVar());
			if(v != null){
				if(v instanceof String){
					Set<String> ss = new HashSet<String>();
					ss.add((String)v);
					res.put(b, ss);
				}else
					res.put(b, (Set<String>)v);
			}
		}
		return res;
	}
	
	public boolean isSolvedString(CGNode node, int var){
		IR ir = node.getIR();
		if(ir != null && ir.getSymbolTable().isConstant(var))
			return true;
		
		VarBox box = new VarBox(node, -1, var);
		if(result.containsKey(box))
			return true;
		
		return false;
	}
	
	public Object getValue(CGNode node, int var){
		IR ir = node.getIR();
		if(ir != null && ir.getSymbolTable().isConstant(var))
			return ir.getSymbolTable().getConstantValue(var);
		
		VarBox box = new VarBox(node, -1, var);
		if(result.containsKey(box))
			return result.get(box);
		
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private Pair<CallGraph, PointerAnalysis<InstanceKey>> buildCG(IClassHierarchy cha) throws ClassHierarchyException, IllegalArgumentException, CallGraphBuilderCancelException{
		
		//test
//		IClass klass = cha.lookupClass(TypeReference.find(ClassLoaderReference.Primordial, "Landroid/util/Log"));
//		for(IMethod m : klass.getAllMethods())
//			System.out.println(m);
//		System.exit(-1);
		//test-end
		AnalysisOptions options = new AnalysisOptions();
		//IRFactory<IMethod> irFactory = new DexIRFactory();
		//AnalysisCache cache = new AnalysisCache(irFactory);
		options.setReflectionOptions(ReflectionOptions.FULL);//hzq: test
		options.setAnalysisScope(scope);
		options.setEntrypoints(getEntrypoints(cha, scope, options, HybridCFGAnalysis.hybridCache));
		options.setSelector(new ClassHierarchyClassTargetSelector(cha));
//		options.setSelector(new ClassHierarchyMethodTargetSelector(cha));
		options.setSelector(new AndroidMethodTargetSelector(cha));
//		CallGraphBuilder cgb = new nCFABuilder(0, cha, options, cache, null, null);
//		CallGraphBuilder cgb = ZeroXCFABuilder.make(cha, options, cache, null, null, 0);
		CallGraphBuilder<InstanceKey> cgb = ResourceCallGraphBuilder.make(cha, options, HybridCFGAnalysis.hybridCache, null, null, 0, ara);
		return Pair.make(cgb.makeCallGraph(options, null), cgb.getPointerAnalysis());
	}
	
	private Iterable<Entrypoint> getEntrypoints(final IClassHierarchy cha, AnalysisScope scope, AnalysisOptions option, AnalysisCache cache){
		Iterable<Entrypoint> entrypoints = null;
		
		if(cha.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lgeneratedharness/GeneratedAndroidHarness")) == null){
			Set<LocatorFlags> flags = HashSetFactory.make();
			//hzq: deleted
			flags.add(LocatorFlags.INCLUDE_CALLBACKS);
			flags.add(LocatorFlags.EP_HEURISTIC);
			flags.add(LocatorFlags.CB_HEURISTIC);
			AndroidEntryPointLocator eps = new AndroidEntryPointLocator(flags);
			List<AndroidEntryPoint> es = eps.getEntryPoints(cha);
			final List<Entrypoint> entries = new ArrayList<Entrypoint>();
			for (AndroidEntryPoint e : es) {
				entries.add(e);
			}
	
			entrypoints = new Iterable<Entrypoint>() {
				@Override
				public Iterator<Entrypoint> iterator() {
					return entries.iterator();
				}
			};
		}else{
			IClass root = cha.lookupClass(TypeReference.find(ClassLoaderReference.Primordial, "Lgeneratedharness/GeneratedAndroidHarness"));
			IMethod rootMethod = root.getMethod(new Selector(Atom.findOrCreateAsciiAtom("androidMain"), Descriptor.findOrCreate(null, TypeName.findOrCreate("V"))));
			Entrypoint droidelEntryPoint = new DefaultEntrypoint(rootMethod, cha);
			
			final List<Entrypoint> entry = new ArrayList<Entrypoint>();
			entry.add(droidelEntryPoint);
			
			entrypoints = new Iterable<Entrypoint>(){
				@Override
				public Iterator<Entrypoint> iterator(){
					return entry.iterator();
				}
			};
		}
		return entrypoints;
	}
	
	private Set<IBox> findHotspots(CallGraph cg, PointerAnalysis<InstanceKey> pa, List<Hotspot> hotspots){
		Set<IBox> boxes = new HashSet<IBox>();
		for(CGNode node : cg){
			IR ir = node.getIR();
			
			if(ir == null)
				continue;

			SSAInstruction[] insts = ir.getInstructions();
			for(int i=0; i<insts.length; i++){
				SSAInstruction inst = insts[i];
				
				if(inst == null)
					continue;
				
				for(Hotspot hotspot : hotspots){
					if(isHotspot(inst, hotspot)){

						int useVar = inst.getUse(hotspot.index() + 1);
						IBox nBox = new VarBox(node, i, useVar);
						boxes.add(nBox);
						int receiverVar = inst.getUse(0);
						LocalPointerKey usePK = (LocalPointerKey)pa.getHeapModel().getPointerKeyForLocal(node, useVar);
						LocalPointerKey receiverPK = (LocalPointerKey)pa.getHeapModel().getPointerKeyForLocal(node, receiverVar);
						InstanceKey[] uses = getInstanceKeys(pa, usePK);
						InstanceKey[] receivers = getInstanceKeys(pa, receiverPK);
						
						HeapGraph<InstanceKey> hg = pa.getHeapGraph();
						Set<Pointing> rpSet = new HashSet<Pointing>();
						rpSet.add(new Pointing(receiverPK.getNode(), receiverPK.getValueNumber()));
						
						for(InstanceKey receiver : receivers){
							for(Iterator<Object> ipk = hg.getPredNodes(receiver); ipk.hasNext();){
								Object o = ipk.next();
								if(o instanceof LocalPointerKey){
									LocalPointerKey rpk = (LocalPointerKey) o;
									rpSet.add(new Pointing(rpk.getNode(), rpk.getValueNumber()));
								}
							}
						}
						
						Set<Pointing> upSet = new HashSet<Pointing>();
						upSet.add(new Pointing(usePK.getNode(), usePK.getValueNumber()));
						
						for(InstanceKey use : uses){
							for(Iterator<Object> ipk = hg.getPredNodes(use); ipk.hasNext();){
								Object o = ipk.next();
								if(o instanceof LocalPointerKey){
									LocalPointerKey upk = (LocalPointerKey) o;
									rpSet.add(new Pointing(upk.getNode(), upk.getValueNumber()));
								}
							}
						}
						putHotspotDesciptor(hotspot, new HotspotDescriptor(node, inst.iindex, nBox, rpSet, upSet));
					}
				}
			}
		}
		return boxes;
	}
	
	private void putHotspotDesciptor(Hotspot h, HotspotDescriptor desc){
		if(!descMap.containsKey(h)){
			descMap.put(h, new HashSet<HotspotDescriptor>());
		}
		descMap.get(h).add(desc);
	}
	
	private InstanceKey[] getInstanceKeys(PointerAnalysis<InstanceKey> pa, PointerKey pk){
		OrdinalSet<InstanceKey> ikSet = pa.getPointsToSet(pk);
		InstanceKey[] iks = new InstanceKey[ikSet.size()];
		
		int index = 0;
		for(InstanceKey ik : ikSet){
			iks[index++] = ik;
		}
		return iks;
	}
	
	private boolean isHotspot(SSAInstruction inst, Hotspot hotspot){
		if(hotspot instanceof ArgumentHotspot){
			ArgumentHotspot argHotspot = (ArgumentHotspot) hotspot;
			if(inst instanceof SSAAbstractInvokeInstruction){
				SSAAbstractInvokeInstruction invokeInst = (SSAAbstractInvokeInstruction) inst;
				MethodReference targetMr = invokeInst.getDeclaredTarget();
				TypeReference cTRef = targetMr.getDeclaringClass();
				Selector mSelector = targetMr.getSelector();
				if(cTRef.equals(argHotspot.getClassDescriptor()) 
						&& mSelector.equals(argHotspot.getMethodDescriptor()))
					return true;
			}
		}

		return false;
	}
	
	private ConstraintGraph buildConstraintGraph(CallGraph cg, FieldDefAnalysis fda, IBox... initials){
		StringModel.init(cg.getClassHierarchy());
		ConstraintGraph graph = new ConstraintGraph();
//		ConstraintVisitor v = new ConstraintVisitor(cg, fda, graph, new InteractionConstraintMonitor(cg, InteractionConstraintMonitor.CLASSTYPE_ALL, InteractionConstraintMonitor.NODETYPE_NONE));
		ConstraintVisitor v = new ConstraintVisitor(cg, fda, graph, null);//new GraphicalDebugMornitor(initials));
		
		for(IBox initial : initials)
			worklist.add(initial);
		
		while(!worklist.isEmpty()){
			IBox box = worklist.pop();
			Set<IBox> res = box.visit(v);
			
			for(IBox next : res)
				worklist.add(next);
		}
		
		if(DEBUG){
			System.out.println("--- constraint visitor warning ---");
			for(String str : v.getWarnings()){
				System.out.println("[Warning] " + str);
			}
			System.out.println("----------");
		}
		return graph;
	}

	class WorkList{
		private List<IBox> list;
		private Set<IBox> visited;
		public WorkList(){
			list = new ArrayList<IBox>();
			visited = new HashSet<IBox>();
		}
		
		public void add(IBox box){
			if(!visited.contains(box)){
				list.add(box);
				visited.add(box);
			}
		}
		
		public IBox pop(){
			IBox box = list.get(0);
			list.remove(0);
			return box;
		}
		
		public boolean isEmpty(){
			return list.isEmpty();
		}
		
		public int size(){
			return list.size();
		}
	}
	
	
	public Set<HotspotDescriptor> getDescriptors(Hotspot h){
		return descMap.get(h);
	}
	
	public Set<HotspotDescriptor> getAllDescriptors(){
		Set<HotspotDescriptor> res = new HashSet<HotspotDescriptor>();
		for(Set<HotspotDescriptor> desc : descMap.values()){
			res.addAll(desc);
		}
		return res;
	}
	
	public class HotspotDescriptor{
		private CGNode node;
		private int iindex;
		private Set<Pointing> receiverAlias;
		private Set<Pointing> argAlias;
		private Set<String> values;
		private IConstraintNode constNode;
		
		private HotspotDescriptor(CGNode node, int iindex, IConstraintNode constNode, Set<Pointing> receiver, Set<Pointing> arg){
			this.node = node;
			this.iindex = iindex;
			this.constNode = constNode;
			this.receiverAlias = receiver;
			this.argAlias = arg;
		}
		
		private void setValues(Set<String> values){
			this.values = values;
		}
		
		private IConstraintNode getConstNode(){
			return constNode;
		}
		
		public Set<Pointing> getReceiverAlias(){
			return receiverAlias;
		}
		
		public CGNode getNode(){
			return node;
		}
		
		public SSAInstruction getInstruction(){
			return node.getIR().getInstructions()[iindex];
		}
		
		public Set<Pointing> getSpotAlias(){
			return argAlias;
		}
		
		public Set<String> getValues(){
			if(values != null)
				return values;
			else
				return Collections.emptySet();
		}
		
		@Override
		public String toString(){
			String res = "[HD]\n";
			res += "\tNode: " + constNode + "\n";
			res += "\tValue: " + values;
			return res;
		}
	}
	
	public static class Pointing{
		private CGNode node;
		private int varNum;
		
		public static Pointing make(CGNode node, int varNum){
			return new Pointing(node, varNum);
		}
		
		private Pointing(CGNode node, int varNum){
			this.node = node;
			this.varNum = varNum;
		}
		
		@Override
		public int hashCode(){
			return node.hashCode() + varNum;
		}
		
		@Override
		public boolean equals(Object o){
			if(o instanceof Pointing){
				Pointing p = (Pointing) o;
				if(p.node.getMethod().equals(node.getMethod()) && p.node.getContext().equals(node.getContext()) && p.varNum == varNum)
					return true;
			}
			return false;
		}
		
		@Override
		public String toString(){
			return "[" + varNum + "] in " + node;
		}
	}
	
	public static class BridgeInfo{
		private Map<Pair<Atom, Integer>, Set<BridgeDescription>> bridgeDescMap;
		
		private BridgeInfo(){
			bridgeDescMap = new HashMap<Pair<Atom, Integer>, Set<BridgeDescription>>();
		}

		private void addBridge(CGNode node, int var, TypeReference tr){
//			System.out.println("#FindBridge: " + node + " (var: " + var + ")");
			Pair<Atom, Integer> p = Pair.make(Atom.findOrCreateAsciiAtom(node.toString()), var);
			if(!bridgeDescMap.containsKey(p)){
				bridgeDescMap.put(p, new HashSet<BridgeDescription>());
			}
			bridgeDescMap.get(p).add(new BridgeDescription(tr));
		}
		
		public Set<BridgeDescription> getDescriptionsOfBridge(CGNode node, int var){
//			System.out.println("#FindingBridge: " + node + " (var: " + var + ")");
			Pair<Atom, Integer> p = Pair.make(Atom.findOrCreateAsciiAtom(node.toString()), var);
			if(bridgeDescMap.containsKey(p))
				return bridgeDescMap.get(p);
			return Collections.emptySet();
		}
				
		public static class BridgeDescription{
			private final TypeReference tr;

			private BridgeDescription(TypeReference tr){
				this.tr = tr;
			}
			
			public TypeReference getTypeReference(){
				return tr;
			}
		}
	}
	
	public BridgeInfo getBridgeInfo(){
		return bi;
	}
	
	public CallGraph getCGusedInSA(){
		return cg;
	}
	
	public PointerAnalysis<InstanceKey> getPAusedInSA(){
		return pa;
	}
}
