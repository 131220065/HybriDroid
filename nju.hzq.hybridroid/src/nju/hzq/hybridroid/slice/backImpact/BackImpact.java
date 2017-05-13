package nju.hzq.hybridroid.slice.backImpact;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.pruned.PrunedCallGraph;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.debug.Assertions;

import nju.hzq.hybridroid.tools.CrossLanguageCallGraphTools;
import nju.hzq.wala.callgraph.view.SWTCallGraph;

public class BackImpact {
	private CallGraph cg = null;
	private Iterable<? extends Entrypoint> entrypoints = null;
	
	public BackImpact(CallGraph cg, Iterable<? extends Entrypoint> iterable) {
		if(cg == null || iterable == null) {
			Assertions.UNREACHABLE("cg or entrypoints cannot be null\n");
		}
		this.cg = cg;
		this.entrypoints = iterable;
	}
	
	
	public boolean isEntrypoint(CGNode node) {
		Iterator<? extends Entrypoint> it = entrypoints.iterator();
		while(it.hasNext()) {
			Entrypoint entryPoint =  it.next();
			
			if(entryPoint.getMethod().equals(node.getMethod())) {
				return true;
			}
		}
		return false;
	}
	
	public void mainOnce() {
		CGNode targetNode = CrossLanguageCallGraphTools.getTargetNodeFromScan(cg);
		if(targetNode == null) {
			return;
		}
		
		Set<CGNode> keepNodes = new HashSet<>();//用于图的剪裁
		CrossLanguageCallGraphTools.getPreNodesOf(cg, targetNode, keepNodes);
		

		System.out.println("找到的入口节点为：");
		for(CGNode node : keepNodes) {
			if(isEntrypoint(node)) {
				System.out.println(node);
			}
		}
		
		PrunedCallGraph pcg = new PrunedCallGraph(cg, keepNodes);
		try {
			SWTCallGraph.run(pcg);
		} catch (WalaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
