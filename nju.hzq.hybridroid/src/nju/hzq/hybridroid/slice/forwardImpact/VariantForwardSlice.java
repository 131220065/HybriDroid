package nju.hzq.hybridroid.slice.forwardImpact;


import java.util.Collection;
import java.util.Set;

import com.ibm.wala.core.tests.slicer.SlicerTest;
import com.ibm.wala.examples.drivers.PDFCallGraph;
import com.ibm.wala.examples.drivers.PDFSlice;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.pruned.PrunedCallGraph;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.SDG;
import com.ibm.wala.ipa.slicer.Slicer;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;

import nju.hzq.hybridroid.slice.util.HybridModRef;
import nju.hzq.hybridroid.tools.CrossLanguageCallGraphTools;
import nju.hzq.wala.callgraph.view.SWTCallGraph;

public class VariantForwardSlice {
	private CallGraph cg = null;
	private PointerAnalysis<InstanceKey> pa = null;
	private SDG<InstanceKey> sdg = null;
	
	public VariantForwardSlice(CallGraph cg, PointerAnalysis<InstanceKey> pa, DataDependenceOptions ddo, ControlDependenceOptions cdo) {
		if(cg == null || pa == null) {
			Assertions.UNREACHABLE("cg or pa cannot be null\n");
		}
		this.cg = cg;
		this.pa = pa;
		this.sdg = new SDG<>(cg, pa, HybridModRef.make(), ddo, cdo);
	}
	
	
	public Collection<Statement> mainOnce() {
		CGNode targetNode = CrossLanguageCallGraphTools.getTargetNodeFromScan(cg);
		if(targetNode == null) {
			return null;
		}
		int iindex = CrossLanguageCallGraphTools.getInstructionIndexFromScan(targetNode);
		
		Statement s = null;
		
		/*System.out.println("�Ƿ�Ҫѡ�����еı�����������Ƭ��(����1ѡ������� ����������ֱ��������ָ�������Ƭ)");
		if(new Scanner(System.in).nextInt() == 1) {
			int var = CrossLanguageCallGraphTools.getVarFromScan(targetNode, iindex);
			s = new ParamCaller(targetNode, iindex, var);
		} else {
			s = new NormalStatement(targetNode, iindex);
		}*/
		
		s = new NormalStatement(targetNode, iindex);
		
		try {
			
			Collection<Statement> slice = Slicer.computeForwardSlice(sdg, s);
			SlicerTest.dumpSlice(slice);
			
			Set<CGNode> keepNodes = CrossLanguageCallGraphTools.getNodesFromSlice(slice);
			
			PrunedCallGraph pcg = new PrunedCallGraph(cg, keepNodes);
			try {
				SWTCallGraph.run(pcg, slice);
			} catch (WalaException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			Graph<Statement> g = PDFSlice.pruneSDG(sdg, slice);
			PDFCallGraph.runGraph(g);
		    return slice;
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CancelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
}

