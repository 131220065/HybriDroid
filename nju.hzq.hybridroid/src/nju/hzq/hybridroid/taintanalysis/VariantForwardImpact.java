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
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.examples.drivers.PDFCallGraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.pruned.PrunedCallGraph;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.ParamCallee;
import com.ibm.wala.ipa.slicer.Statement;
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
import com.ibm.wala.ssa.SSAInstructionFactory;
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
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.intset.OrdinalSet;

import nju.hzq.hybridroid.tools.CrossLanguageCallGraphTools;
import nju.hzq.patch.HybridCallBackResult;
import nju.hzq.stub.HzqStub;

public class VariantForwardImpact {
	private final String intent = "INTENT";
	private final String bundle = "BUNDLE";

	private CallGraph cg = null;
	private PointerAnalysis<InstanceKey> pa = null;
	private IClassHierarchy cha = null;
	private LinkedHashMap<LocalPointerKey, HashSet<LocalPointerKey>> impactMap = null;
	private HashSet<PointerKey> usedFieldKeys = null;
	private HashMap<PointerKey, HashSet<LocalPointerKey>> fieldMap = null;

	private HashMap<String, HashSet<LocalPointerKey>> globalMap = null;
	
	
	private HashSet<CGNode> accessedNodes = null;
	private HashSet<CGNode> rootNodes = null;
	
	
	private class LocalPointerKeyWithInstruction extends LocalPointerKey {
		private SSAInstruction inst = null;

		public LocalPointerKeyWithInstruction(CGNode node, int valueNumber, SSAInstruction inst) {
			super(node, valueNumber);
			this.inst = inst;
		}
		
		@Override
		public String toString() {
			return super.toString() + "\n" + (inst == null ? "" : ("via " + inst.toString(node.getIR().getSymbolTable())));
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

	public VariantForwardImpact(CallGraph cg, PointerAnalysis<InstanceKey> pa) {
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
		collectGlobal();
	}

	private void outImpact() {
		Set<LocalPointerKey> keySet = impactMap.keySet();
		for (LocalPointerKey lpk : keySet) {
			System.out.println(lpk);
			for (LocalPointerKey impactLpk : impactMap.get(lpk)) {
				System.out.println("    -> " + impactLpk);
			}
		}
		PDFCallGraph.runGraph(g);
	}

	private Set<CGNode> getKeepNodes() {
		Set<CGNode> keepNodes = new HashSet<>();
		for (LocalPointerKey lpk : impactMap.keySet()) {
			keepNodes.add(lpk.getNode());
		}
		return keepNodes;
	}

	private void runSWTCallGraph() {
		CallGraph prunedGraph = new PrunedCallGraph(cg, getKeepNodes());
		try {
			nju.hzq.wala.callgraph.view.SWTCallGraph.run(prunedGraph);
		} catch (WalaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		runSWTCallGraph();
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

	private void collectGlobal() {
		for (CGNode node : cg) {
			IR ir = node.getIR();
			if (ir == null)
				continue;
			for (SSAInstruction inst : ir.getInstructions()) {
				if (inst instanceof SSAGetInstruction) {
					HashSet<PointerKey> keys = getFieldPointerKeys(node, (SSAGetInstruction) inst);
					for (PointerKey pk : keys) {
						if (!fieldMap.containsKey(pk)) {
							fieldMap.put(pk, new HashSet<>());
						}
						fieldMap.get(pk).add(new LocalPointerKeyWithInstruction(node, inst.getDef(), inst));
					}
				} else if (inst instanceof SSAInvokeInstruction) {
					// java的方法调用指令
					SSAInvokeInstruction invokeInst = (SSAInvokeInstruction) inst;
					if (invokeInst.getDeclaredTarget().getDeclaringClass().getName()
							.equals(TypeName.findOrCreate("Landroid/content/Intent"))
							&& invokeInst.getDeclaredTarget().getName().toString().startsWith("get")
							&& invokeInst.getDeclaredTarget().getName().toString().endsWith("Extra")) {
						globalMap.get(intent).add(new LocalPointerKeyWithInstruction(node, inst.getDef(), inst));

					} else if (invokeInst.getDeclaredTarget().getDeclaringClass().getName()
							.equals(TypeName.findOrCreate("Landroid/os/Bundle"))
							&& invokeInst.getDeclaredTarget().getName().toString().startsWith("get")) {
						globalMap.get(bundle).add(new LocalPointerKeyWithInstruction(node, inst.getDef(), inst));
					}

				}
			}
		}
	}

	private class TaintVisitor implements JSInstructionVisitor {

		private CGNode node;
		private int var;
		private DefUse du;

		private Set<LocalPointerKey> willImpactKeys;
		private HashSet<LocalPointerKey> mapSet;

		public Set<LocalPointerKey> getWillImpactKeys() {
			return willImpactKeys;
		}

		public HashSet<LocalPointerKey> getMapSet() {
			return mapSet;
		}

		public TaintVisitor(LocalPointerKey lpk) {
			this.node = lpk.getNode();
			this.var = lpk.getValueNumber();
			du = node.getDU();
			willImpactKeys = new HashSet<>();
			mapSet = new HashSet<>();
			impactMap.put(lpk, mapSet);
		}

		private void visitAbstractInvoke(SSAAbstractInvokeInstruction invokeInst) {
			Set<CGNode> succNodes = cg.getPossibleTargets(node, invokeInst.getCallSite());
			boolean hasSuccNode = false;
			for (CGNode succNode : succNodes) {
				hasSuccNode = true;

				HzqStub.stubPrint(succNode.getMethod().toString());
				if (succNode.getMethod().getDeclaringClass().getClassLoader().getReference()
						.equals(ClassLoaderReference.Primordial)) {
					// 原生的
					if (invokeInst.getDef() > 0) {
						LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(node, invokeInst.getDef(), invokeInst);
						mapSet.add(impactLpk);
						if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
							willImpactKeys.add(impactLpk);
						}
					}
					if (!succNode.getMethod().isStatic()) {
						HzqStub.stubPrint("not static method");
						if (invokeInst.getUse(0) != var) {
							// 参数是污点，则规定返回值和实例是污点
							LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(node, invokeInst.getUse(0), invokeInst);
							mapSet.add(impactLpk);
							if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
								willImpactKeys.add(impactLpk);
							}
						}
					}
				} else {

					int i;
					for (i = 0; i < invokeInst.getNumberOfParameters(); i++) {
						if (invokeInst.getUse(i) == var) {
							break;
						}
					}
					LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(succNode, succNode.getIR().getParameter(i), invokeInst);
					mapSet.add(impactLpk);
					if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
						willImpactKeys.add(impactLpk);
					}
				}

			}
			if (!hasSuccNode) {
				LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(node, invokeInst.getDef(), invokeInst);
				mapSet.add(impactLpk);
				if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
					willImpactKeys.add(impactLpk);
				}
			}

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
							rootNodes.add(impactLpk.getNode());//add
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
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitAstLexicalWrite(AstLexicalWrite instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitAstGlobalRead(AstGlobalRead instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitAstGlobalWrite(AstGlobalWrite instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitSSAPut(instruction);
		}

		@Override
		public void visitAssert(AstAssertInstruction instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitEachElementGet(EachElementGetInstruction inst) {
			HzqStub.stubPrint(node.getIR().getInstructionString(inst.iindex));
			visitOtherInst(inst);
		}

		@Override
		public void visitEachElementHasNext(EachElementHasNextInstruction inst) {
			HzqStub.stubPrint(node.getIR().getInstructionString(inst.iindex));
			visitOtherInst(inst);
		}

		@Override
		public void visitIsDefined(AstIsDefinedInstruction inst) {
			HzqStub.stubPrint(node.getIR().getInstructionString(inst.iindex));
			visitOtherInst(inst);
		}

		@Override
		public void visitEcho(AstEchoInstruction inst) {
			HzqStub.stubPrint(node.getIR().getInstructionString(inst.iindex));
			visitOtherInst(inst);
		}

		@Override
		public void visitGoto(SSAGotoInstruction instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitArrayStore(SSAArrayStoreInstruction instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(node, instruction.getArrayRef(), instruction);
			mapSet.add(impactLpk);
			if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
				willImpactKeys.add(impactLpk);
			}
		}

		@Override
		public void visitBinaryOp(SSABinaryOpInstruction instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
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
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitConversion(SSAConversionInstruction instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitComparison(SSAComparisonInstruction instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			// visitOtherInst(instruction);
			// not deal
		}

		@Override
		public void visitSwitch(SSASwitchInstruction instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitReturn(SSAReturnInstruction instruction) {
			// return value
			boolean isRootNode = rootNodes.contains(node);
			Iterator<CGNode> iter = cg.getPredNodes(node);
			while (iter.hasNext()) {
				CGNode preNode = iter.next();
				
				if(!isRootNode && !accessedNodes.contains(preNode)) {
					continue;
				}
				
				if(isRootNode) {
					rootNodes.add(preNode);
				}
				
				Iterator<CallSiteReference> csIter = cg.getPossibleSites(preNode, node);
				while (csIter.hasNext()) {
					CallSiteReference csr = csIter.next();
					SSAAbstractInvokeInstruction[] invokeInsts = preNode.getIR().getCalls(csr);
					for (SSAAbstractInvokeInstruction invokeInst : invokeInsts) {
						LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(preNode, invokeInst.getDef(), instruction);
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
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitPut(SSAPutInstruction instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitSSAPut(instruction);
		}

		@Override
		public void visitInvoke(SSAInvokeInstruction invokeInst) {
			HzqStub.stubPrint(node.getIR().getInstructionString(invokeInst.iindex));
			Set<CGNode> succNodes = cg.getPossibleTargets(node, invokeInst.getCallSite());
			boolean hasSuccNode = false;
			for (CGNode succNode : succNodes) {
				hasSuccNode = true;

				HzqStub.stubPrint(succNode.getMethod().toString());
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
						LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(node, invokeInst.getDef(), invokeInst);
						mapSet.add(impactLpk);
						if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
							willImpactKeys.add(impactLpk);
						}
					}
					if (!succNode.getMethod().isStatic()) {
						HzqStub.stubPrint("not static method");
						if (invokeInst.getUse(0) != var) {
							// 参数是污点，则规定返回值和实例是污点
							LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(node, invokeInst.getUse(0), invokeInst);
							mapSet.add(impactLpk);
							if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
								willImpactKeys.add(impactLpk);
							}
						}
					}
				} else {
					int i;
					for (i = 0; i < invokeInst.getNumberOfParameters(); i++) {
						if (invokeInst.getUse(i) == var) {

							LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(succNode, succNode.getIR().getParameter(i), invokeInst);
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
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			// visitOtherInst(instruction);
			// not deal
		}

		@Override
		public void visitArrayLength(SSAArrayLengthInstruction instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitThrow(SSAThrowInstruction instruction) {
			// not deal
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			// visitOtherInst(instruction);
		}

		@Override
		public void visitMonitor(SSAMonitorInstruction instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitCheckCast(SSACheckCastInstruction instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitInstanceof(SSAInstanceofInstruction instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitPhi(SSAPhiInstruction instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitPi(SSAPiInstruction instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitJavaScriptInvoke(JavaScriptInvoke invokeInst) {
			boolean isNotStatic = invokeInst.getDeclaredTarget().getName().toString().equals("dispatch");
			HzqStub.stubPrint(node.getIR().getInstructionString(invokeInst.iindex));
			Set<CGNode> succNodes = cg.getPossibleTargets(node, invokeInst.getCallSite());
			boolean hasSuccNode = false;
			for (CGNode succNode : succNodes) {
				hasSuccNode = true;

				HzqStub.stubPrint(succNode.getMethod().toString());
				if (succNode.getMethod().getDeclaringClass().getName().toString().startsWith("Lpreamble.js")
						|| succNode.getMethod().getDeclaringClass().getName().toString().startsWith("Lprologue.js")) {
					// 原生的
					if (invokeInst.getDef() > 0) {
						LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(node, invokeInst.getDef(), invokeInst);
						mapSet.add(impactLpk);
						if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
							willImpactKeys.add(impactLpk);
						}
					}
					if (isNotStatic) {
						HzqStub.stubPrint("javascript dispatch (not static method)");
						if (invokeInst.getUse(1) != var) {
							// 参数是污点，则规定返回值和实例是污点
							//Javascript中，this变量是第二个参数（即getUse(1)）
							LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(node, invokeInst.getUse(1), invokeInst);
							mapSet.add(impactLpk);
							if (!impactMap.containsKey(impactLpk) && !willImpactKeys.contains(impactLpk)) {
								willImpactKeys.add(impactLpk);
							}
						}
					}
				} else {

					if (!CrossLanguageCallGraphTools.isJSNode(succNode)) {
						if (HybridCallBackResult.callbackResult != null
								&& succNode.getClassHierarchy().isSubclassOf(succNode.getMethod().getDeclaringClass(),
										HybridCallBackResult.callbackResult.webChromeClient)) {
							if (invokeInst.getUse(2) == var) {
								int paraVar = succNode.getIR().getParameter(2);
								LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(succNode, paraVar, invokeInst);
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
								if(i < 2) {
									continue;
								}
								paraVar = succNode.getIR().getParameter(i - 1);
							} else {
								paraVar = succNode.getIR().getParameter(i);
							}
							LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(succNode, paraVar, invokeInst);
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
					HzqStub.stubPrint("javascript dispatch (not static method)");
					if (invokeInst.getUse(1) != var) {
						// 参数是污点，则规定返回值和实例是污点
						//Javascript中，this变量是第二个参数（即getUse(1)）
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
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitJavaScriptPropertyRead(JavaScriptPropertyRead instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitJavaScriptPropertyWrite(JavaScriptPropertyWrite instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));

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
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitWithRegion(JavaScriptWithRegion instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitCheckRef(JavaScriptCheckReference instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitSetPrototype(SetPrototype instruction) {
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

		@Override
		public void visitPrototypeLookup(PrototypeLookup instruction) {
			// must impact
			HzqStub.stubPrint(node.getIR().getInstructionString(instruction.iindex));
			visitOtherInst(instruction);
		}

	}

	private Set<LocalPointerKey> getOnePKForwardImpacts(LocalPointerKey lpk) {
		CGNode node = lpk.getNode();
		int var = lpk.getValueNumber();

		TaintVisitor tv = new TaintVisitor(lpk);

		DefUse du = node.getDU();
		
		SSAInstruction defInst = du.getDef(var);

		if (defInst == null) {// 参数
			boolean isRootNode = false;
			if(rootNodes.contains(node)) {
				isRootNode = true;
			}
			int paraI = -1;
			for (int i = 0; i < node.getIR().getNumberOfParameters(); i++) {
				if (node.getIR().getParameter(i) == var) {
					paraI = i;
					break;
				}
			}
			System.err.println("hzq: paraI = " + paraI + ", var = " + var);

			Iterator<CGNode> iter = cg.getPredNodes(node);
			while (iter.hasNext()) {
				CGNode preNode = iter.next();
				
				if(!isRootNode && !accessedNodes.contains(preNode)) {
					continue;
				}
				if(isRootNode) {
					rootNodes.add(preNode);
				}
				
				Iterator<CallSiteReference> csIter = cg.getPossibleSites(preNode, node);
				while (csIter.hasNext()) {
					CallSiteReference csr = csIter.next();
					SSAAbstractInvokeInstruction[] invokeInsts = preNode.getIR().getCalls(csr);
					for (SSAAbstractInvokeInstruction invokeInst : invokeInsts) {
						if (CrossLanguageCallGraphTools.isJSNode(preNode)
								&& !CrossLanguageCallGraphTools.isJSNode(node)) {
							if (HybridCallBackResult.callbackResult != null
									&& node.getClassHierarchy().isSubclassOf(node.getMethod().getDeclaringClass(),
											HybridCallBackResult.callbackResult.webChromeClient)) {
								if (paraI != 2) {
									continue;
								}
							} else {
								// bridge
								if (paraI == 0) {
									continue;
								}
								paraI++;
							}
						}
						LocalPointerKey impactLpk = new LocalPointerKeyWithInstruction(preNode, invokeInst.getUse(paraI), ParameterInstruction.getInstance());
						tv.getMapSet().add(impactLpk);
						if (!impactMap.containsKey(impactLpk) && !tv.getWillImpactKeys().contains(impactLpk)) {
							tv.getWillImpactKeys().add(impactLpk);
						}
					}
				}
			}
		} else if(defInst instanceof SSAGetInstruction) {
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
		for(LocalPointerKey lpk : impactedKeys) {//这相当于广度优先搜索，所以可以在这里添加即可
			accessedNodes.add(lpk.getNode());
		}
		return impactedKeys;
	}

	/*public Set<LocalPointerKey> getForwardImpacts(CGNode node, int var) {

		Set<LocalPointerKey> keySet = new HashSet<>();
		LocalPointerKey lpk = (LocalPointerKey) pa.getHeapModel().getPointerKeyForLocal(node, var);
		Iterator<Object> iter = pa.getHeapGraph().getSuccNodes(lpk);
		while (iter.hasNext()) {
			Iterator<Object> aliasIter = pa.getHeapGraph().getPredNodes(iter.next());
			while (aliasIter.hasNext()) {
				Object key = aliasIter.next();
				if (key instanceof LocalPointerKey) {
					keySet.add((LocalPointerKey) key);
				}
			}
		}
		return keySet;
	}*/
}
