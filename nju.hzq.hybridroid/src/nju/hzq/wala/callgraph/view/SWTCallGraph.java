package nju.hzq.wala.callgraph.view;

import java.io.File;
import java.util.Collection;
import java.util.Properties;
import java.util.jar.JarFile;

import org.eclipse.jface.window.ApplicationWindow;

import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.examples.drivers.PDFTypeHierarchy;
import com.ibm.wala.examples.drivers.PDFWalaIR;
import com.ibm.wala.examples.properties.WalaExamplesProperties;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.InferGraphRoots;
import com.ibm.wala.util.io.CommandLine;
import com.ibm.wala.util.io.FileProvider;

import nju.hzq.wala.swtui.SWTTreeViewer;
import nju.hzq.wala.swtui.ViewIRAction;

/**
 * 
 * This application is a WALA client: it invokes an SWT TreeViewer to visualize
 * a Call Graph
 * 
 * @author sfink
 */
public class SWTCallGraph {

  private final static boolean CHECK_GRAPH = false;
  
  
  public static ApplicationWindow run(CallGraph cg, Collection<Statement> slice) throws WalaException {
	    //自己添加的
	    Properties wp = null;
	    try {
	      wp = WalaProperties.loadProperties();
	      wp.putAll(WalaExamplesProperties.loadProperties());
	    } catch (WalaException e) {
	      e.printStackTrace();
	      Assertions.UNREACHABLE();
	    }
	    String psFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDFWalaIR.PDF_FILE;
	    String dotFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDFTypeHierarchy.DOT_FILE;
	    String dotExe = wp.getProperty(WalaExamplesProperties.DOT_EXE);
	    String gvExe = wp.getProperty(WalaExamplesProperties.PDFVIEW_EXE);

	    // create and run the viewer
	    final SWTTreeViewer v = new SWTTreeViewer();
	    v.setGraphInput(cg);
	    v.setRootsInput(InferGraphRoots.inferRoots(cg));
	    v.getPopUpActions().add(new ViewIRAction(v, cg, psFile, dotFile, dotExe, gvExe, slice));
	    v.run();
	    return v.getApplicationWindow();
	  }
  
  
  public static ApplicationWindow run(CallGraph cg) throws WalaException {
    //自己添加的
    Properties wp = null;
    try {
      wp = WalaProperties.loadProperties();
      wp.putAll(WalaExamplesProperties.loadProperties());
    } catch (WalaException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
    String psFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDFWalaIR.PDF_FILE;
    String dotFile = wp.getProperty(WalaProperties.OUTPUT_DIR) + File.separatorChar + PDFTypeHierarchy.DOT_FILE;
    String dotExe = wp.getProperty(WalaExamplesProperties.DOT_EXE);
    String gvExe = wp.getProperty(WalaExamplesProperties.PDFVIEW_EXE);

    // create and run the viewer
    final SWTTreeViewer v = new SWTTreeViewer();
    v.setGraphInput(cg);
    v.setRootsInput(InferGraphRoots.inferRoots(cg));
    v.getPopUpActions().add(new ViewIRAction(v, cg, psFile, dotFile, dotExe, gvExe));
    v.run();
    return v.getApplicationWindow();
  }

 
}
