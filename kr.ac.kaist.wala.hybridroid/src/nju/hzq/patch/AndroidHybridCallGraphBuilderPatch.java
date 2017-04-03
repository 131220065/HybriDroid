package nju.hzq.patch;

import com.ibm.wala.ssa.SSAInvokeInstruction;

import kr.ac.kaist.wala.hybridroid.callgraph.AndroidHybridCallGraphBuilder.HybridJavaConstraintVisitor;

public class AndroidHybridCallGraphBuilderPatch {
	public static void visitInvokePatch(HybridJavaConstraintVisitor hjcv, SSAInvokeInstruction instruction) {
		hjcv.hzqVisitInvokePatch(instruction);
	}
}
