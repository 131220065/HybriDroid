package nju.hzq.patch;

import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.strings.Atom;

import nju.hzq.stub.HzqStub;

public class HzqHybridTools {
	private static TypeReference jsinterAnnTR = null;
	
	public static boolean isBridgeMethod(IMethod m) {
		if(jsinterAnnTR == null) {
			jsinterAnnTR = TypeReference.find(ClassLoaderReference.Primordial,
					"Landroid/webkit/JavascriptInterface"); 
		}
		for (Annotation ann : m.getAnnotations()) {
			TypeReference annTr = ann.getType();
			if (annTr.getName().equals(jsinterAnnTR.getName()))
				return true;
		}
		return false;
	}
	
	public static Atom getLanguage(CGNode node) {
	    return node.getMethod().getReference().getDeclaringClass().getClassLoader().getLanguage();
	}
	
	public static boolean isJSNode(CGNode node) {
		return JavaScriptTypes.jsName.equals(getLanguage(node));
	}
}
