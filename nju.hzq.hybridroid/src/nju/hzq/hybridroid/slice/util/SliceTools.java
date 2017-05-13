package nju.hzq.hybridroid.slice.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ipa.slicer.StatementWithInstructionIndex;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;

import nju.hzq.hybridroid.slice.AllImpact.VariantAllSlice;
import nju.hzq.hybridroid.slice.backImpact.VariantBackSlice;
import nju.hzq.hybridroid.slice.forwardImpact.VariantForwardSlice;

public class SliceTools {

	public static List<Statement> getSlicedNodesOf(IR ir, Collection<Statement> slice) {
		List<Statement> sList = new ArrayList<>();
		if(slice == null) {
			return sList;
		}
		for (Statement s : slice) {
			if (s.getNode() == null || s.getNode().getIR() == null) {
				continue;
			}
			if (s.getNode().getIR().equals(ir)) {
				// if(s instanceof StatementWithInstructionIndex) {
				if (!sList.contains(s)) {
					sList.add(s);
				}
				// }
			}
		}
		return sList;
	}

	/*
	 * public static List<Integer> getSlicedNodesOf(IR ir, Collection<Statement>
	 * slice) { List<Integer> instIndexList = new ArrayList<>();
	 * ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg =
	 * ir.getControlFlowGraph(); for(Statement s : slice) { if(s.getNode() ==
	 * null || s.getNode().getIR() == null) { continue; }
	 * if(s.getNode().getIR().equals(ir)) { if(s instanceof
	 * StatementWithInstructionIndex) { StatementWithInstructionIndex swii =
	 * (StatementWithInstructionIndex) s; int instIndex =
	 * swii.getInstructionIndex(); if(!instIndexList.contains(instIndex)) {
	 * instIndexList.add(instIndex); } } } } return instIndexList; }
	 */

	public static void printInstructionAndLineOfJavaByteCode(List<Statement> sList) {
		for(Statement s : sList) {
			if(s instanceof StatementWithInstructionIndex) {
				StatementWithInstructionIndex swii = (StatementWithInstructionIndex) s;
				System.out.println(swii.getNode().getIR().getInstructionString(swii.getInstructionIndex()));
				SourcePositionTools.getSourcePositionOf(swii);
			} else {
				//System.out.println("no instruction");
				//SourcePositionTools.getSourcePositionOf(s);
			}
		}
	}

	public static Collection<Statement> doSliceFromScan(CallGraph cg, PointerAnalysis<InstanceKey> pa) {
		System.out.println("请输入1(前向切片) 2(后向切片) 3(所有切片)");
		Scanner scan = new Scanner(System.in);
		int sliceFlag = scan.nextInt();
		while (sliceFlag < 1 || sliceFlag > 3) {
			System.out.println("输入无效，请重新输入：1(前向切片) 2(后向切片) 3(所有切片)");
			sliceFlag = scan.nextInt();
		}
		DataDependenceOptions ddo = DataDependenceOptions.NO_EXCEPTIONS;
		ControlDependenceOptions cdo = ControlDependenceOptions.NONE;
		int doIndex = -1;
		System.out.println("默认数据依赖选项为" + ddo + ",控制依赖选项为" + cdo);
		System.out.println("请选择数据依赖选项：1 full; 2 no_base_ptrs; 3 no_base_no_heap; 4 no_base_no_exceptions; 5 no_base_no_heap_no_exceptions; 6 no_heap; 7 no_heap_no_exceptions; 8 no_exceptions; 9 none; 10 no_base_no_heap_no_cast;");
		doIndex = scan.nextInt();
		if(doIndex >= 0 && doIndex < DataDependenceOptions.values().length) {
			ddo = DataDependenceOptions.values()[doIndex - 1];
			System.out.println("选择的选项为" + ddo);
		}
		System.out.println("请选择控制依赖选项：1 full; 2 none; 3 no_exceptional_edges;");
		doIndex = scan.nextInt();
		if(doIndex >= 0 && doIndex < ControlDependenceOptions.values().length) {
			cdo = ControlDependenceOptions.values()[doIndex - 1];
			System.out.println("选择的选项为" + cdo);
		}
		switch (sliceFlag) {
		case 1:
			return new VariantForwardSlice(cg, pa, ddo, cdo).mainOnce();
		case 2:
			return new VariantBackSlice(cg, pa, ddo, cdo).mainOnce();
		case 3:
			return new VariantAllSlice(cg, pa, ddo, cdo).mainOnce();
		default:
			break;
		}
		return null;
	}
}
