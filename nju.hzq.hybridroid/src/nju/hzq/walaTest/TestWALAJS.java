package nju.hzq.walaTest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Pair;

import nju.hzq.hybridroid.taintanalysis.VariantForwardImpact;

public class TestWALAJS {
	public static void main(String[] args)
			throws IllegalArgumentException, MalformedURLException, IOException, CancelException, WalaException {
		 com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.setTranslatorFactory(new
		 CAstRhinoTranslatorFactory());
		 Pair<CallGraph, PointerAnalysis<InstanceKey>> pair =
		 JSCallGraphBuilderUtil.makeHTMLCGPA(new
		 URL("file:///D:/WALA/MyTests/fibnachi/index.html"));
		 CallGraph cg = pair.fst;
		 new VariantForwardImpact(cg, pair.snd).mainOnce();
		// PointerAnalysis<InstanceKey> pa = pair.snd;
		// CallGraph CG =
		// com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil.makeHTMLCG(new
		// URL("file:///D:/WALA/MyTests/hello.js"));
		// SliceTools.doSliceFromScan(cg, pa);
//		AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope("D:/WALA/MyTests/ArrayTest.jar",
//				new File(CallGraphTestUtil.REGRESSION_EXCLUSIONS));
//
//		ClassHierarchy cha = ClassHierarchy.make(scope);
//
//		Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha);
//		AnalysisOptions options = new AnalysisOptions(scope, entrypoints);
//
//		com.ibm.wala.ipa.callgraph.CallGraphBuilder builder = Util.makeZeroCFABuilder(options, new AnalysisCache(), cha,
//				scope);
//		CallGraph cg = builder.makeCallGraph(options, null);
//		new VariantForwardImpact(cg, builder.getPointerAnalysis()).mainOnce();
	}
}
