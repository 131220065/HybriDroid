package nju.hzq.hybridroid.taintanalysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import com.ibm.wala.cast.ir.ssa.AbstractReflectivePut;
import com.ibm.wala.cast.ir.ssa.AstAssertInstruction;
import com.ibm.wala.cast.ir.ssa.AstEchoInstruction;
import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.ir.ssa.AstIsDefinedInstruction;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.cast.ir.ssa.CAstBinaryOp;
import com.ibm.wala.cast.ir.ssa.EachElementGetInstruction;
import com.ibm.wala.cast.ir.ssa.EachElementHasNextInstruction;
import com.ibm.wala.cast.js.ssa.JSInstructionVisitor;
import com.ibm.wala.cast.js.ssa.JavaScriptCheckReference;
import com.ibm.wala.cast.js.ssa.JavaScriptInstanceOf;
import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyRead;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyWrite;
import com.ibm.wala.cast.js.ssa.JavaScriptTypeOfInstruction;
import com.ibm.wala.cast.js.ssa.JavaScriptWithRegion;
import com.ibm.wala.cast.js.ssa.PrototypeLookup;
import com.ibm.wala.cast.js.ssa.SetPrototype;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.pruned.PrunedCallGraph;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction.Operator;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAComparisonInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAConversionInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.intset.OrdinalSet;

import nju.hzq.hybridroid.tools.CrossLanguageCallGraphTools;
import nju.hzq.patch.HybridCallBackResult;
import nju.hzq.stub.HzqStub;
import nju.hzq.tool.HybridCallGraphTool;

public class Not_Use_VariantForwardImpact {
	private final String intent = "INTENT";
	private final String bundle = "BUNDLE";

	private CallGraph cg = null;
	private PointerAnalysis<InstanceKey> pa = null;
	private IClassHierarchy cha = null;
	private LinkedHashMap<LocalPointerKey, HashSet<LocalPointerKey>> impactMap = null;
	private HashSet<PointerKey> usedFieldKeys = null;
	private HashMap<PointerKey, HashSet<LocalPointerKey>> fieldMap = null;

	private HashMap<IClass, CGNode> handlerMap = null;

	private HashMap<String, HashSet<LocalPointerKey>> globalMap = null;

	private HashSet<CGNode> accessedNodes = null;
	private HashSet<CGNode> rootNodes = null;

	private LinkedHashMap<String, HashSet<LocalPointerKey>> rootTaint = null;

	private IClass handler = null;
	private IClass LOG = null;
	private IClass ISharedPreferences = null;
	private IClass OutPutStream = null;
	private IClass Writer = null;
	private IClass IDataOutput = null;
	private IClass URL = null;
	private IClass IHttpClient = null;
	private IClass WebView = null;

	private LinkedHashMap<String, HashSet<LocalPointerKey>> warnings = null;

	private void collectIClass() {
		handler = cha.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/os/Handler"));
		LOG = cha.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/util/Log"));
		ISharedPreferences = cha.lookupClass(
				TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/content/SharedPreferences"));
		OutPutStream = cha
				.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/io/OutputStream"));
		IDataOutput = cha
				.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/io/DataOutput"));
		URL = cha.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/net/URL"));
		IHttpClient = cha.lookupClass(
				TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lorg/apache/http/client/HttpClient"));
		WebView = cha
				.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Landroid/webkit/WebView"));
		Writer = cha.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Ljava/io/Writer"));
	}

	private class LocalPointerKeyWithInstruction extends LocalPointerKey {
		private SSAInstruction inst = null;
		private HashSet<Short> sourceIndexs = null;

		public LocalPointerKeyWithInstruction(CGNode node, int valueNumber, SSAInstruction inst) {
			super(node, valueNumber);
			this.inst = inst;
			sourceIndexs = new HashSet<>();
		}

		public void setIndex(short index) {
			sourceIndexs.add(index);
		}

		public HashSet<Short> getSourceIndexs() {
			return sourceIndexs;
		}

		public boolean addIndexs(HashSet<Short> indexs) {
			int lastSize = sourceIndexs.size();
			sourceIndexs.addAll(indexs);
			if (lastSize == sourceIndexs.size()) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			String str = super.toString() + "\n"
					+ (inst == null ? "" : ("via " + inst.toString(node.getIR().getSymbolTable())));
			str += "\nsourceIndexs = [";
			for (int index : sourceIndexs) {
				str += index + ", ";
			}
			str += "]";
			return str;
		}

		@Override
		public boolean equals(Object obj) {
			return super.equals(obj);
		}

		@Override
		public final int hashCode() {
			return super.hashCode();
		}

	}

	private Graph<LocalPointerKey> g = new Graph<LocalPointerKey>() {

		@Override
		public Iterator<LocalPointerKey> iterator() {
			// TODO Auto-generated method stub
			return impactMap.keySet().iterator();
		}

		@Override
		public int getNumberOfNodes() {
			// TODO Auto-generated method stub
			return impactMap.keySet().size();
		}

		@Override
		public void addNode(LocalPointerKey n) {
			Assertions.UNREACHABLE();
		}

		@Override
		public void removeNode(LocalPointerKey n) throws UnsupportedOperationException {
			Assertions.UNREACHABLE();
		}

		@Override
		public boolean containsNode(LocalPointerKey n) {
			return impactMap.containsKey(n);
		}

		@Override
		public Iterator<LocalPointerKey> getPredNodes(LocalPointerKey n) {
			Assertions.UNREACHABLE();
			return null;
		}

		@Override
		public int getPredNodeCount(LocalPointerKey n) {
			Assertions.UNREACHABLE();
			return 0;
		}

		@Override
		public Iterator<LocalPointerKey> getSuccNodes(LocalPointerKey n) {
			return impactMap.get(n).iterator();
		}

		@Override
		public int getSuccNodeCount(LocalPointerKey N) {
			// TODO Auto-generated method stub
			return impactMap.get(N).size();
		}

		@Override
		public void addEdge(LocalPointerKey src, LocalPointerKey dst) {
			Assertions.UNREACHABLE();
		}

		@Override
		public void removeEdge(LocalPointerKey src, LocalPointerKey dst) throws UnsupportedOperationException {
			Assertions.UNREACHABLE();
		}

		@Override
		public void removeAllIncidentEdges(LocalPointerKey node) throws UnsupportedOperationException {
			Assertions.UNREACHABLE();
		}

		@Override
		public void removeIncomingEdges(LocalPointerKey node) throws UnsupportedOperationException {
			Assertions.UNREACHABLE();
		}

		@Override
		public void removeOutgoingEdges(LocalPointerKey node) throws UnsupportedOperationException {
			Assertions.UNREACHABLE();
		}

		@Override
		public boolean hasEdge(LocalPointerKey src, LocalPointerKey dst) {
			Assertions.UNREACHABLE();
			return false;
		}

		@Override
		public void removeNodeAndEdges(LocalPointerKey n) throws UnsupportedOperationException {
			Assertions.UNREACHABLE();
		}
	};

	public Not_Use_VariantForwardImpact(CallGraph cg, PointerAnalysis<InstanceKey> pa) {
		this.cg = cg;
		this.pa = pa;
		cha = cg.getClassHierarchy();
		impactMap = new LinkedHashMap<>();
		fieldMap = new HashMap<>();
		usedFieldKeys = new HashSet<>();
		globalMap = new HashMap<>();
		globalMap.put(intent, new HashSet<>());
		globalMap.put(bundle, new HashSet<>());
		accessedNodes = new HashSet<>();
		rootNodes = new HashSet<>();
		handlerMap = new HashMap<>();
		rootTaint = new LinkedHashMap<>();
		warnings = new LinkedHashMap<>();
		collectIClass();
		collectGlobal();
	}

	private void outImpact() {
		/*
		 * Set<LocalPointerKey> keySet = impactMap.keySet(); for
		 * (LocalPointerKey lpk : keySet) { System.out.println(lpk); for
		 * (LocalPointerKey impactLpk : impactMap.get(lpk)) {
		 * System.out.println("    -> " + impactLpk); } }
		 */
		System.out.println("\n");
		System.out.println("leakage warnings:");
		System.out.println();
		for (String key : warnings.keySet()) {
			HashSet<LocalPointerKey> lpks = warnings.get(key);
			System.out.println(key + "; num = " + lpks.size() + " : ");
			for (LocalPointerKey lpk : lpks) {
				System.out.println(lpk);
			}
			System.out.println();
		}
		System.out.println("\n\n");
		//PDFCallGraph.runGraph(g);
	}

	private Set<CGNode> getKeepNodes() {
		Set<CGNode> keepNodes = new HashSet<>();
		for (LocalPointerKey lpk : impactMap.keySet()) {
			keepNodes.add(lpk.getNode());
		}
		return keepNodes;
	}

	private void runSWTCallGraph() {
		Set<CGNode> keepNodes = getKeepNodes();
		if (keepNodes.size() == 0) {
			return;
		}
		CallGraph prunedGraph = new PrunedCallGraph(cg, keepNodes);
		try {
			nju.hzq.wala.callgraph.view.SWTCallGraph.run(prunedGraph);
		} catch (WalaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void privateDataLeakageDetect() {
		if (rootTaint.size() == 0) {
			System.out.println("didn't discover taint value");
			return;
		} else {
			System.out.println("\n\ndiscovered taint values : ");
			System.out.println();
			short index = 0; // set index
			for (String key : rootTaint.keySet()) {
				HashSet<LocalPointerKey> lpks = rootTaint.get(key);
				System.out.println("type = " + key + "; num = " + lpks.size() + " : ");
				for (LocalPointerKey taintLpk : lpks) {
					((LocalPointerKeyWithInstruction) taintLpk).setIndex(index);
					System.out.println(taintLpk);
					index++;
				}
				System.out.println();
			}
			Set<LocalPointerKey> workSet = new HashSet<>();
			for (HashSet<LocalPointerKey> lpkSet : rootTaint.values()) {
				workSet.addAll(lpkSet);
			}
			for (LocalPointerKey lpk : workSet) {
				rootNodes.add(lpk.getNode());
				accessedNodes.add(lpk.getNode());
			}
			while (!workSet.isEmpty()) {
				workSet = getOneForwardImpacts(workSet);
			}
			//computeIndexs();
			outImpact();
		}

	}

	private void computeIndexs() {
		Set<LocalPointerKey> keySet = impactMap.keySet();
		HzqStub.stubPrint("deal with lpkWithInst + size = " + keySet.size());
		for(HashSet<LocalPointerKey> valueLpks : impactMap.values()) {
			for(LocalPointerKey kLpk : keySet) {
				if(valueLpks.contains(kLpk)) {
					valueLpks.remove(kLpk);
					valueLpks.add(kLpk);
				}
			}
		}
		HzqStub.stubPrint("start compute indexs");
		boolean isChanged = true;
		while (isChanged) {
			isChanged = false;
			for (LocalPointerKey keyLpk : keySet) {
				HashSet<Short> sourceIndexs = ((LocalPointerKeyWithInstruction) keyLpk).getSourceIndexs();
				HashSet<LocalPointerKey> valueLpks = impactMap.get(keyLpk);
				for (LocalPointerKey vLpk : valueLpks) {
					if (((LocalPointerKeyWithInstruction) vLpk).addIndexs(sourceIndexs)) {
						isChanged = true;
					}
				}
			}
		}
		HzqStub.stubPrint("end compute indexs");

	}

	public void mainOnce() {
		CGNode node = CrossLanguageCallGraphTools.getTargetNodeFromScan(cg);
		if (node == null) {
			return;
		}
		int ii = CrossLanguageCallGraphTools.getInstructionIndexFromScan(node);
		int var = CrossLanguageCallGraphTools.getVarFromScan(node, ii);
		Set<LocalPointerKey> workSet = new HashSet<>();
		workSet.add(new LocalPointerKey(node, var));
		rootNodes.add(node);
		accessedNodes.add(node);
		while (!workSet.isEmpty()) {
			workSet = getOneForwardImpacts(workSet);
		}
		outImpact();
	}

	private HashSet<PointerKey> getFieldPointerKeys(CGNode node, SSAFieldAccessInstruction inst) {
		HashSet<PointerKey> keys = new HashSet<>();
		FieldReference fr = inst.getDeclaredField();
		IField f = cha.resolveField(fr);
		if (f != null) {
			if (inst.isStatic()) {
				PointerKey pk = pa.getHeapModel().getPointerKeyForStaticField(f);
				keys.add(pk);
			} else {
				int owner = inst.getUse(0);

				PointerKey ownerPK = pa.getHeapModel().getPointerKeyForLocal(node, owner);
				for (InstanceKey ownerIK : (OrdinalSet<InstanceKey>) pa.getPointsToSet(ownerPK)) {
					PointerKey pk = pa.getHeapModel().getPointerKeyForInstanceField(ownerIK, f);
					keys.add(pk);
				}
			}
		}
		return keys;
	}

	private void hashMapAdd(HashMap<String, HashSet<LocalPointerKey>> map, String key, LocalPointerKey lpk) {
		if (!map.containsKey(key)) {
			map.put(key, new HashSet<>());
		}
		map.get(key).add(lpk);
	}

	private void collectGlobal() {
		for (CGNode entryNode : cg.getEntrypointNodes()) {
			IClass nodeClass = entryNode.getMethod().getDeclaringClass();
			if (entryNode.getMethod().getSelector().equals(Selector.make("handleMessage(Landroid/os/Message;)V"))
					&& cha.isSubclassOf(nodeClass, handler)) {
				handlerMap.put(nodeClass, entryNode);
			}
		}
		for (CGNode node : cg) {
			IR ir = node.getIR();
			if (ir == null)
				continue;
			SymbolTable st = ir.getSymbolTable();
			for (SSAInstruction inst : ir.getInstructions()) {
				if (inst == null) {
					continue;
				}
				for (int i = 0; i < inst.getNumberOfUses(); i++) {
					int useVar = inst.getUse(i);
					if (st.isStringConstant(useVar)) {
						String constantValue = st.getStringValue(useVar);
						if (constantValue.startsWith("content://sms") || constantValue.startsWith("content://icc")) {
							hashMapAdd(rootTaint, "Contacts or Short Message",
									new LocalPointerKeyWithInstruction(node, useVar, inst));
						}
					}
				}

				if (inst instanceof SSAGetInstruction) {
					HashSet<PointerKey> keys = getFieldPointerKeys(node, (SSAGetInstruction) inst);
					for (PointerKey pk : keys) {
						if (!fieldMap.containsKey(pk)) {
							fieldMap.put(pk, new HashSet<>());
						}
						fieldMap.get(pk).add(new LocalPointerKeyWithInstruction(node, inst.getDef(), inst));
					}
					SSAGetInstruction ssagi = (SSAGetInstruction) inst;
					if (ssagi.getDeclaredFieldType().getName().equals(TypeName.findOrCreate("Landroid/net/Uri"))) {
						if (ssagi.getDeclaredField().getDeclaringClass().getName().toString()
								.startsWith("Landroid/provider/ContactsContract")) {
							hashMapAdd(rootTaint, "Contacts or Short Message",
									new LocalPointerKeyWithInstruction(node, inst.getDef(), inst));
						} else if (ssagi.getDeclaredField().getDeclaringClass().getName()
								.equals(TypeName.findOrCreate("Landroid/provider/Telephony$Sms"))) {
							hashMapAdd(rootTaint, "Contacts or Short Message",
									new LocalPointerKeyWithInstruction(node, inst.getDef(), inst));
						}
					}

				} else if (inst instanceof SSAInvokeInstruction) {
					// java的方法调用指令
					SSAInvokeInstruction invokeInst = (SSAInvokeInstruction) inst;
					TypeName className = invokeInst.getDeclaredTarget().getDeclaringClass().getName();
					String methodName = invokeInst.getDeclaredTarget().getName().toString();
					if (inst.getDef() <= 0) {
						continue;
					}
					LocalPointerKey lpk = new LocalPointerKeyWithInstruction(node, inst.getDef(), inst);
					if (className.equals(TypeName.findOrCreate("Landroid/content/Intent"))
							&& methodName.startsWith("get") && methodName.endsWith("Extra")) {
						globalMap.get(intent).add(lpk);

					} else if (className.equals(TypeName.findOrCreate("Landroid/os/Bundle"))
							&& methodName.startsWith("get")) {
						globalMap.get(bundle).add(lpk);
					} else if (className.equals(TypeName.findOrCreate("Landroid/telephony/TelephonyManager"))
							&& methodName.equals("getLine1Number")) {
						hashMapAdd(rootTaint, "TelephonyManager.getLine1Number", lpk);
					} else if (className.equals(TypeName.findOrCreate("Landroid/location/LocationManager"))
							&& methodName.equals("getLastKnownLocation")) {
						hashMapAdd(rootTaint, "LocationManager.getLastKnownLocation", lpk);
					} /*
						 * else if(className.equals(TypeName.findOrCreate(
						 * "Landroid/content/ContentResolver")) &&
						 * methodName.equals("query")) { hashMapAdd(rootTaint,
						 * "ContentResolver.query", lpk); }
						 */

				}
			}
		}
	}

	private class TaintVisitor implements JSInstructionVisitor {

		private CGNode node;
		private int var;
		private DefUse du;
		private LocalPointerKey currentLpk;

		private Set<LocalPointerKey> willImpactKeys;
		private HashSet<LocalPointerKey> mapSet;

		public Set<LocalPointerKey> getWillImpactKeys() {
			return willImpactKeys;
		}

		public HashSet<LocalPointerKey> getMapSet() {
			return mapSet;
		}

		public TaintVisitor(LocalPointerKey lpk) {
			currentLpk = lpk;
			this.node = lpk.getNode();
			this.var = lpk.getValueNumber();
			du = node.getDU();
			willImpactKeys = new HashSet<>();
			mapSet = new HashSet<>();
			impactMap.put(lpk, mapSet);
		}

		private void visitSSAPut(SSAFieldAccessInstruction instruction) {
			// 给域赋值
			HashSet<PointerKey> keys = getFieldPointerKeys(node, instruction);
			for (PointerKey pk : keys) {
				if (!fieldMap.containsKey(pk)) {
					// 没有使用它的地方，则无法通过域赋值传递
					return;
				}

				if (!usedFieldKeys.contains(pk)) {
					HashSet<LocalPointerKey> getFieldSet = fieldMap.get(pk);
					mapSet.addAll(getFieldSet);
					usedFieldKeys.add(pk);
					for (LocalPointerKey impactLpk : getFieldSet) {
						if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
							willImpactKeys.add(impactLpk);
							rootNodes.add(impactLpk.getNode());// add
						}
					}
				}
			}
		}

		private void visitOtherInst(SSAInstruction instruction) {
			if (instruction.getDef() <= 0) {
				return;
			}
			LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(node, instruction.getDef(), instruction);
			mapSet.add(impactLpk);
			if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
				willImpactKeys.add(impactLpk);
			}
		}

		@Override
		public void visitAstLexicalRead(AstLexicalRead instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitAstLexicalWrite(AstLexicalWrite instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitAstGlobalRead(AstGlobalRead instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitAstGlobalWrite(AstGlobalWrite instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitSSAPut(instruction);
		}

		@Override
		public void visitAssert(AstAssertInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitEachElementGet(EachElementGetInstruction inst) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(inst.iindex));
			visitOtherInst(inst);
		}

		@Override
		public void visitEachElementHasNext(EachElementHasNextInstruction inst) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(inst.iindex));
			visitOtherInst(inst);
		}

		@Override
		public void visitIsDefined(AstIsDefinedInstruction inst) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(inst.iindex));
			visitOtherInst(inst);
		}

		@Override
		public void visitEcho(AstEchoInstruction inst) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(inst.iindex));
			visitOtherInst(inst);
		}

		@Override
		public void visitGoto(SSAGotoInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitArrayStore(SSAArrayStoreInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(node, instruction.getArrayRef(),
					instruction);
			mapSet.add(impactLpk);
			if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
				willImpactKeys.add(impactLpk);
			}
		}

		@Override
		public void visitBinaryOp(SSABinaryOpInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			if (instruction.getOperator().equals(Operator.AND) || instruction.getOperator().equals(Operator.OR)) {
				return;
			}
			if (instruction.getOperator() instanceof CAstBinaryOp) {
				return;
			}
			visitOtherInst(instruction);
		}

		@Override
		public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitConversion(SSAConversionInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitComparison(SSAComparisonInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			// visitOtherInst(instruction);
			// not deal
		}

		@Override
		public void visitSwitch(SSASwitchInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitReturn(SSAReturnInstruction instruction) {
			// return value

			if (!CrossLanguageCallGraphTools.isJSNode(node) && HybridCallGraphTool.isBridgeMethod(node.getMethod())) {
				hashMapAdd(warnings, "leakage from Java to Javascript via bridge return", currentLpk);
				// new LocalPointerKeyWithInstruction(node, var, instruction));
			}

			boolean isRootNode = rootNodes.contains(node);
			Iterator<CGNode> iter = cg.getPredNodes(node);
			while (iter.hasNext()) {
				CGNode preNode = iter.next();
				if (CrossLanguageCallGraphTools.isFakeRootNode(preNode)) {
					continue;
				}

				if (!isRootNode && !accessedNodes.contains(preNode)) {
					continue;
				}

				if (isRootNode) {
					rootNodes.add(preNode);
				}

				Iterator<CallSiteReference> csIter = cg.getPossibleSites(preNode, node);
				while (csIter.hasNext()) {
					CallSiteReference csr = csIter.next();
					SSAAbstractInvokeInstruction[] invokeInsts = preNode.getIR().getCalls(csr);
					for (SSAAbstractInvokeInstruction invokeInst : invokeInsts) {
						LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(preNode, invokeInst.getDef(),
								instruction);
						mapSet.add(impactLpk);
						if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
							willImpactKeys.add(impactLpk);
						}
					}
				}
			}
		}

		@Override
		public void visitGet(SSAGetInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitPut(SSAPutInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitSSAPut(instruction);
		}

		@Override
		public void visitInvoke(SSAInvokeInstruction invokeInst) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(invokeInst.iindex));

			// 判断是否泄露
			TypeReference classRef = invokeInst.getDeclaredTarget().getDeclaringClass();
			String methodName = invokeInst.getDeclaredTarget().getName().toString();
			IClass declaredClass = cha.lookupClass(classRef);
			// LocalPointerKey currentLpk = new
			// LocalPointerKeyWithInstruction(node, var, invokeInst);
			// HzqStub.stubPrint("declared class = " +
			// declaredClass.toString());
			if (declaredClass != null) {
				if (declaredClass.equals(LOG)) {
					hashMapAdd(warnings, "leakage via Log.**", currentLpk);
				} else if (methodName.startsWith("put") && cha.implementsInterface(declaredClass, ISharedPreferences)
						&& invokeInst.getUse(0) != var) {
					hashMapAdd(warnings, "leakage via SharedPreferences.put**", currentLpk);
				} else if (methodName.startsWith("write") && invokeInst.getUse(0) != var) {
					if (cha.isSubclassOf(declaredClass, OutPutStream)) {
						hashMapAdd(warnings, "leakage via OutPutStream.write", currentLpk);
					} else if (cha.isSubclassOf(declaredClass, Writer)) {
						hashMapAdd(warnings, "leakage via Writer.write", currentLpk);
					} else if (cha.implementsInterface(declaredClass, IDataOutput)) {
						hashMapAdd(warnings, "leakage via DataOutput.write**", currentLpk);
					}
				} else if (methodName.equals("append") && cha.isSubclassOf(declaredClass, Writer)
						&& invokeInst.getUse(0) != var) {
					hashMapAdd(warnings, "leakage via Writer.append", currentLpk);
				} else if (methodName.equals("openConnection") && cha.isSubclassOf(declaredClass, URL)
						&& invokeInst.getUse(0) == var) {
					hashMapAdd(warnings, "leakage via URL.openConnection", currentLpk);
				} else if (methodName.equals("excute") && cha.implementsInterface(declaredClass, IHttpClient)
						&& invokeInst.getUse(0) != var) {
					hashMapAdd(warnings, "leakage via HttpClient.execute", currentLpk);
				} else if (methodName.equals("loadUrl") && cha.isSubclassOf(declaredClass, WebView)
						&& invokeInst.getUse(0) != var) {
					hashMapAdd(warnings, "leakage via WebView.loadUrl", currentLpk);
				}
			}

			if (!invokeInst.isStatic() && invokeInst.getDeclaredTarget().getSelector()
					.equals(Selector.make("sendMessage(Landroid/os/Message;)Z"))) {
				PointerKey pk = pa.getHeapModel().getPointerKeyForLocal(node, invokeInst.getUse(0));
				OrdinalSet<InstanceKey> instanceSet = pa.getPointsToSet(pk);
				for (InstanceKey ik : instanceSet) {
					IClass klass = ik.getConcreteType();
					if (cha.isSubclassOf(klass, handler)) {
						// handler.sendMessage(msg);
						if (invokeInst.getUse(1) != var) {
							return;
						}
						CGNode handleMessageNode = handlerMap.get(klass);
						if (handleMessageNode == null) {
							return;
						}
						LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(handleMessageNode,
								handleMessageNode.getIR().getParameter(1), invokeInst);
						mapSet.add(impactLpk);
						if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
							willImpactKeys.add(impactLpk);
							rootNodes.add(impactLpk.getNode());
						}
						// once support
						return;
					}
				}
			}

			Set<CGNode> succNodes = cg.getPossibleTargets(node, invokeInst.getCallSite());
			boolean hasSuccNode = false;
			for (CGNode succNode : succNodes) {
				hasSuccNode = true;

				// HzqStub.stubPrint(succNode.getMethod().toString());
				if (succNode.getMethod().getDeclaringClass().getClassLoader().getReference()
						.equals(ClassLoaderReference.Primordial)) {

					if (succNode.getMethod().getDeclaringClass().getName()
							.equals(TypeName.findOrCreate("Landroid/content/Intent"))
							&& succNode.getMethod().getName().toString().startsWith("put")
							&& succNode.getMethod().getName().toString().endsWith("Extra")) {
						mapSet.addAll(globalMap.get(intent));
						for (LocalPointerKey impactLpk : globalMap.get(intent)) {
							if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
								willImpactKeys.add(impactLpk);
								rootNodes.add(impactLpk.getNode());
							}
						}
						return;
					}
					if (succNode.getMethod().getDeclaringClass().getName()
							.equals(TypeName.findOrCreate("Landroid/os/Bundle"))
							&& succNode.getMethod().getName().toString().startsWith("put")) {
						mapSet.addAll(globalMap.get(bundle));
						for (LocalPointerKey impactLpk : globalMap.get(bundle)) {
							if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
								willImpactKeys.add(impactLpk);
								rootNodes.add(impactLpk.getNode());
							}
						}
						return;
					}

					// 原生的
					if (invokeInst.getDef() > 0) {
						LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(node, invokeInst.getDef(),
								invokeInst);
						mapSet.add(impactLpk);
						if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
							willImpactKeys.add(impactLpk);
						}
					}
					if (!succNode.getMethod().isStatic()) {
						// HzqStub.stubPrint("not static method");
						if (invokeInst.getUse(0) != var) {
							// 参数是污点，则规定返回值和实例是污点
							LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(node, invokeInst.getUse(0),
									invokeInst);
							mapSet.add(impactLpk);
							if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
								willImpactKeys.add(impactLpk);
							}
						}
					}
				} else {
					int i;
					for (i = 0; i < invokeInst.getNumberOfParameters(); i++) {
						if (invokeInst.getUse(i) == var && succNode.getIR() != null) {

							LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(succNode,
									succNode.getIR().getParameter(i), invokeInst);
							mapSet.add(impactLpk);
							if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
								willImpactKeys.add(impactLpk);
							}
						}
					}
				}

			}
			if (!hasSuccNode && invokeInst.hasDef()) {
				LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(node, invokeInst.getDef(), invokeInst);
				mapSet.add(impactLpk);
				if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
					willImpactKeys.add(impactLpk);
				}
			}
		}

		@Override
		public void visitNew(SSANewInstruction instruction) {
			//HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			// visitOtherInst(instruction);
			// if there is a param, it's an array allocation such as "String[] a = new String[2];"
			// not deal
		}

		@Override
		public void visitArrayLength(SSAArrayLengthInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitThrow(SSAThrowInstruction instruction) {
			// not deal
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			// visitOtherInst(instruction);
		}

		@Override
		public void visitMonitor(SSAMonitorInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitCheckCast(SSACheckCastInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitInstanceof(SSAInstanceofInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitPhi(SSAPhiInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitPi(SSAPiInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitJavaScriptInvoke(JavaScriptInvoke invokeInst) {
			boolean isNotStatic = invokeInst.getDeclaredTarget().getName().toString().equals("dispatch");
			// HzqStub.stubPrint(node.getIR().getInstructionString(invokeInst.iindex));
			// LocalPointerKey currentLpk = new
			// LocalPointerKeyWithInstruction(node, var, invokeInst);
			if (isNotStatic && node.getIR().getSymbolTable().isConstant(invokeInst.getUse(0))) {
				// 判断是否泄露
				String methodName = (String) node.getIR().getSymbolTable().getConstantValue(invokeInst.getUse(0));
				if (methodName.equals("submit") && invokeInst.getNumberOfParameters() == 2) {
					hashMapAdd(warnings, "leakage via javascript submit", currentLpk);
				} else if (methodName.equals("send") && invokeInst.getNumberOfParameters() == 3) {
					hashMapAdd(warnings, "leakage via javascript send", currentLpk);
				}
			}

			Set<CGNode> succNodes = cg.getPossibleTargets(node, invokeInst.getCallSite());
			boolean hasSuccNode = false;
			for (CGNode succNode : succNodes) {
				hasSuccNode = true;

				// HzqStub.stubPrint(succNode.getMethod().toString());
				if (succNode.getMethod().getDeclaringClass().getName().toString().startsWith("Lpreamble.js")
						|| succNode.getMethod().getDeclaringClass().getName().toString().startsWith("Lprologue.js")) {
					// 原生的
					if (invokeInst.getDef() > 0) {
						LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(node, invokeInst.getDef(),
								invokeInst);
						mapSet.add(impactLpk);
						if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
							willImpactKeys.add(impactLpk);
						}
					}
					if (isNotStatic) {
						// HzqStub.stubPrint("javascript dispatch (not static
						// method)");
						if (invokeInst.getUse(1) != var) {
							// 参数是污点，则规定返回值和实例是污点
							// Javascript中，this变量是第二个参数（即getUse(1)）
							LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(node, invokeInst.getUse(1),
									invokeInst);
							mapSet.add(impactLpk);
							if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
								willImpactKeys.add(impactLpk);
							}
						}
					}
				} else {

					if (!CrossLanguageCallGraphTools.isJSNode(succNode)) {
						hashMapAdd(warnings, "leakage from Javascript to Java", currentLpk);
						if (HybridCallBackResult.callbackResult != null
								&& succNode.getClassHierarchy().isSubclassOf(succNode.getMethod().getDeclaringClass(),
										HybridCallBackResult.callbackResult.webChromeClient)) {
							if (invokeInst.getUse(2) == var) {
								int paraVar = succNode.getIR().getParameter(3);
								LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(succNode, paraVar,
										invokeInst);
								mapSet.add(impactLpk);
								if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
									willImpactKeys.add(impactLpk);
								}
							}
							continue;
						}
					}

					int i;
					for (i = 0; i < invokeInst.getNumberOfParameters(); i++) {
						if (invokeInst.getUse(i) == var) {
							int paraVar = -1;
							if (!CrossLanguageCallGraphTools.isJSNode(succNode)) {
								if (i < 2) {
									continue;
								}
								paraVar = succNode.getIR().getParameter(i - 1);
							} else {
								paraVar = succNode.getIR().getParameter(i);
							}
							LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(succNode, paraVar,
									invokeInst);
							mapSet.add(impactLpk);
							if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
								willImpactKeys.add(impactLpk);
							}
						}
					}

				}

			}
			if (!hasSuccNode) {
				LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(node, invokeInst.getDef(), invokeInst);
				mapSet.add(impactLpk);
				if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
					willImpactKeys.add(impactLpk);
				}
				if (isNotStatic) {
					// HzqStub.stubPrint("javascript dispatch (not static
					// method)");
					if (invokeInst.getUse(1) != var) {
						// 参数是污点，则规定返回值和实例是污点
						// Javascript中，this变量是第二个参数（即getUse(1)）
						impactLpk = new LocalPointerKeyWithInstruction(node, invokeInst.getUse(1), invokeInst);
						mapSet.add(impactLpk);
						if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
							willImpactKeys.add(impactLpk);
						}
					}
				}
			}
		}

		@Override
		public void visitTypeOf(JavaScriptTypeOfInstruction instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitJavaScriptPropertyRead(JavaScriptPropertyRead instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitJavaScriptPropertyWrite(JavaScriptPropertyWrite instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));

			AbstractReflectivePut arp = instruction;
			LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(node, arp.getObjectRef(), instruction);
			mapSet.add(impactLpk);
			if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
				willImpactKeys.add(impactLpk);
			}
			if (!node.getIR().getSymbolTable().isConstant(arp.getMemberRef())) {// constant
																				// value
																				// not
																				// impact
				impactLpk = new LocalPointerKeyWithInstruction(node, arp.getMemberRef(), instruction);
				mapSet.add(impactLpk);
				if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
					willImpactKeys.add(impactLpk);
				}
			}
		}

		@Override
		public void visitJavaScriptInstanceOf(JavaScriptInstanceOf instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitWithRegion(JavaScriptWithRegion instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitCheckRef(JavaScriptCheckReference instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitSetPrototype(SetPrototype instruction) {
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitPrototypeLookup(PrototypeLookup instruction) {
			// must impact
			// HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

	}

	private Set<LocalPointerKey> getOnePKForwardImpacts(LocalPointerKey lpk) {
		CGNode node = lpk.getNode();
		int var = lpk.getValueNumber();

		TaintVisitor tv = new TaintVisitor(lpk);

		DefUse du = node.getDU();

		SSAInstruction defInst = du.getDef(var);

		if (defInst == null) {// 参数 or 其他没有定义的
			boolean isRootNode = false;
			if (rootNodes.contains(node)) {
				isRootNode = true;
			}
			int paraI = -1;
			for (int i = 0; i < node.getIR().getNumberOfParameters(); i++) {
				if (node.getIR().getParameter(i) == var) {
					paraI = i;
					break;
				}
			}
			// System.err.println("hzq: paraI = " + paraI + ", var = " + var);

			if (paraI >= 0) {
				Iterator<CGNode> iter = cg.getPredNodes(node);
				while (iter.hasNext()) {
					CGNode preNode = iter.next();
					if (CrossLanguageCallGraphTools.isFakeRootNode(preNode)) {
						continue;
					}

					if (!isRootNode && !accessedNodes.contains(preNode)) {
						continue;
					}
					if (isRootNode) {
						rootNodes.add(preNode);
					}

					Iterator<CallSiteReference> csIter = cg.getPossibleSites(preNode, node);
					while (csIter.hasNext()) {
						CallSiteReference csr = csIter.next();
						SSAAbstractInvokeInstruction[] invokeInsts = preNode.getIR().getCalls(csr);
						for (SSAAbstractInvokeInstruction invokeInst : invokeInsts) {
							if (CrossLanguageCallGraphTools.isJSNode(preNode)
									&& !CrossLanguageCallGraphTools.isJSNode(node)) {
								/*
								 * if (HybridCallBackResult.callbackResult !=
								 * null &&
								 * node.getClassHierarchy().isSubclassOf(node.
								 * getMethod().getDeclaringClass(),
								 * HybridCallBackResult.callbackResult.
								 * webChromeClient)) { if (paraI != 3) {
								 * continue; } else { paraI = 2; } } else { //
								 * bridge if (paraI == 0) { continue; } paraI++;
								 * }
								 */
								continue;
							}
							LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(preNode,
									invokeInst.getUse(paraI), ParameterInstruction.getInstance());
							tv.getMapSet().add(impactLpk);
							if (!impactMap.containsKey(impactLpk) && !tv.getWillImpactKeys().contains(impactLpk)) {
								tv.getWillImpactKeys().add(impactLpk);
							}
						}
					}
				}
			}

		} else if (defInst instanceof SSAGetInstruction) {
			tv.visitSSAPut((SSAFieldAccessInstruction) defInst);
		}

		Iterator<SSAInstruction> insts = du.getUses(var);
		while (insts.hasNext()) {
			SSAInstruction inst = insts.next();
			inst.visit(tv);
		}
		return tv.getWillImpactKeys();
	}

	public Set<LocalPointerKey> getOneForwardImpacts(Set<LocalPointerKey> seeds) {
		Set<LocalPointerKey> impactedKeys = new HashSet<>();
		for (LocalPointerKey key : seeds) {
			impactedKeys.addAll(getOnePKForwardImpacts(key));
		}
		for (LocalPointerKey lpk : impactedKeys) {// 这相当于广度优先搜索，所以可以在这里添加即可
			accessedNodes.add(lpk.getNode());
		}
		return impactedKeys;
	}

	/*
	 * public Set<LocalPointerKey> getForwardImpacts(CGNode node, int var) {
	 * 
	 * Set<LocalPointerKey> keySet = new HashSet<>(); LocalPointerKey lpk =
	 * (LocalPointerKey) pa.getHeapModel().getPointerKeyForLocal(node, var);
	 * Iterator<Object> iter = pa.getHeapGraph().getSuccNodes(lpk); while
	 * (iter.hasNext()) { Iterator<Object> aliasIter =
	 * pa.getHeapGraph().getPredNodes(iter.next()); while (aliasIter.hasNext())
	 * { Object key = aliasIter.next(); if (key instanceof LocalPointerKey) {
	 * keySet.add((LocalPointerKey) key); } } } return keySet; }
	 */
}
