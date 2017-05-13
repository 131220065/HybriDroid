package nju.hzq.hybridroid.slice.forwardImpact;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.pruned.PrunedCallGraph;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.debug.Assertions;

import nju.hzq.hybridroid.tools.CrossLanguageCallGraphTools;
import nju.hzq.wala.callgraph.view.SWTCallGraph;

public class ForwardImpact {
	private CallGraph cg = null;
	
	public ForwardImpact(CallGraph cg) {
		if(cg == null) {
			Assertions.UNREACHABLE("cg or entrypoints cannot be null\n");
		}
		this.cg = cg;
	}
	
	
	public void mainOnce() {
		CGNode targetNode = CrossLanguageCallGraphTools.getTargetNodeFromScan(cg);
		if(targetNode == null) {
			return;
		}
		
		Set<CGNode> keepNodes = new HashSet<>();//����ͼ�ļ���
		CrossLanguageCallGraphTools.getSuccNodesOf(cg, targetNode, keepNodes);

		PrunedCallGraph pcg = new PrunedCallGraph(cg, keepNodes);
		try {
			SWTCallGraph.run(pcg);
		} catch (WalaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
