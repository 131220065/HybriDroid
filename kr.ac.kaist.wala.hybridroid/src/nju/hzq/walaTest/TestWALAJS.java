package nju.hzq.walaTest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.examples.drivers.PDFCallGraph;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;

public class TestWALAJS {
	public static void main(String[] args) throws IllegalArgumentException, MalformedURLException, IOException, CancelException, WalaException {
		com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
	    CallGraph CG = com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil.makeHTMLCG(new URL("file:///D:/WALA/HybriDroid/hybridroidTestCases/hybridApp1/assets/index.html"));
		//CallGraph CG = com.ibm.wala.cast.js.test.JSCallGraphBuilderUtil.makeHTMLCG(new URL("file:///D:/WALA/MyTests/hello.js"));
		System.out.println(CG.getNumberOfNodes());
	    PDFCallGraph.runCGNodeGraph(CG);
	}
}
