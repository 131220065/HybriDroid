package main;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.ipa.modref.JavaScriptModRef;
import com.ibm.wala.classLoader.CallSiteReference;
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
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.IntSetAction;

import nju.hzq.hybridroid.tools.CrossLanguageCallGraphTools;
import nju.hzq.wala.callgraph.view.SWTCallGraph;

public class JSSliceTest {

	public static Collection<Statement> slice(CallGraph cg, PointerAnalysis<InstanceKey> pa, DataDependenceOptions data, ControlDependenceOptions ctrl, String methodSuffix)
			throws IOException, WalaException, CancelException {

		SDG sdg = new SDG(cg, pa, new JavaScriptModRef(), data, ctrl);

		final Collection<Statement> ss = findTargetStatement(cg, methodSuffix);
		Collection<Statement> result = Slicer.computeBackwardSlice(sdg, ss);
		SlicerTest.dumpSlice(result);
		
		Set<CGNode> keepNodes = CrossLanguageCallGraphTools.getNodesFromSlice(ss);
		
		PrunedCallGraph pcg = new PrunedCallGraph(cg, keepNodes);
		SWTCallGraph.run(cg, ss);
		
		PDFCallGraph.runGraph(PDFSlice.pruneSDG(sdg, result));
		return result;
	}

	public static Collection<Statement> findTargetStatement(CallGraph CG, String methodSuffix) {
		final Collection<Statement> ss = HashSetFactory.make();
		for (CGNode n : getNodes(CG, "suffix:" + methodSuffix)) {
			for (Iterator<CGNode> callers = CG.getPredNodes(n); callers.hasNext();) {
				final CGNode caller = callers.next();
				for (Iterator<CallSiteReference> sites = CG.getPossibleSites(caller, n); sites.hasNext();) {
					caller.getIR().getCallInstructionIndices(sites.next()).foreach(new IntSetAction() {
						@Override
						public void act(int x) {
							ss.add(new NormalStatement(caller, x));
						}
					});
				}
			}
		}
		return ss;
	}

	public static Collection<CGNode> getNodes(CallGraph CG, String functionIdentifier) {
		return JSCallGraphUtil.getNodes(CG, functionIdentifier);
	}
}
