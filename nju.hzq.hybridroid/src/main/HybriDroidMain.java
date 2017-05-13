package main;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Properties;

import org.omg.CORBA.DynAnyPackage.Invalid;

import com.ibm.wala.examples.drivers.PDFCallGraph;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Pair;

import kr.ac.kaist.wala.hybridroid.analysis.HybridCFGAnalysis;
import kr.ac.kaist.wala.hybridroid.utils.LocalFileReader;
import nju.hzq.hybridroid.slice.util.SliceTools;
import nju.hzq.hybridroid.taintanalysis.VariantForwardImpact;

public class HybriDroidMain {
	public static Properties walaProperties;
	public static long START;
	public static long END;
	/**
	 * HybriDroid main function. Now, There is CFG-building option only in
	 * HybriDroid.
	 * 
	 * @param args
	 * @throws IOException
	 * @throws IllegalArgumentException
	 * @throws CancelException
	 * @throws ParseException
	 * @throws Invalid
	 * @throws WalaException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws WalaException, FileNotFoundException, IOException, IllegalArgumentException, CancelException {
		// Load wala property. Now, 'PROP_ARG' is essential option, so else
		// branch cannot be reached.
		String propertyfile = "D:/WALA/HybriDroid/workspace/HybriDroid/kr.ac.kaist.wala.hybridroid/wala.properties";
		File propFile = new File(propertyfile);
		walaProperties = new Properties();
		walaProperties.load(new FileInputStream(propFile));

		// Load target file for analysis.
		long startTime = System.currentTimeMillis();
		START = startTime;
		/**
		 * Below is the switch case for HybriDroid functions. One function of
		 * the CommandLineOptionGroup must be one case in below.
		 */
		String targetPath = "D:/WALA/HybriDroid/hybridroidTestCases/hybridApp1.apk";
		HybridCFGAnalysis cfgAnalysis = new HybridCFGAnalysis(targetPath, LocalFileReader.androidJar(walaProperties).getPath());
		Pair<CallGraph, PointerAnalysis<InstanceKey>> p = cfgAnalysis.makeDefaultCallGraph();

		
		//Pair<CallGraph, PointerAnalysis<InstanceKey>> p = cfgAnalysis.makeCallGraphFromScan();
		//Pair<CallGraph, PointerAnalysis<InstanceKey>> p = cfgAnalysis.makeBridgeCallGraph();
		long endTime = System.currentTimeMillis();
		System.out.println("#Time: " + (endTime - startTime) / 1000 + "s");
		
		PDFCallGraph.runGraph(p.fst);
		//PDFCallGraph.runGraph(CrossLanguageCallGraphTools.prunCallGraphToRelatedJSNodes(p.fst));

		
		//SliceTools.doSliceFromScan(p.fst, p.snd);
		new VariantForwardImpact(p.fst, p.snd).mainOnce();
		
//		System.err.println("Graph Modeling for taint...");
//		ModeledCallGraphForTaint mcg = new ModeledCallGraphForTaint(p.fst);
//		
//		PrivateLeakageDetector pld = new PrivateLeakageDetector(mcg, p.snd);
//		pld.analyze();
//		
//		System.out.println("#AllTime: " + (System.currentTimeMillis() - startTime) / 1000 + "s");
//		
//		for(LeakWarning w : pld.getWarnings()){
//			System.out.println("=========");
//			System.out.println(w);
//			System.out.println("=========");
//			w.printPathFlow("leak.dot");
//		}
	}
}
