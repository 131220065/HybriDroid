package nju.hzq.hybridroid.slice.util;

import java.util.Collection;

import com.ibm.wala.cast.ipa.callgraph.AstHeapModel;
import com.ibm.wala.cast.js.ipa.modref.JavaScriptModRef;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.modref.ExtendedHeapModel;
import com.ibm.wala.ipa.modref.ModRef;

import nju.hzq.hybridroid.tools.CrossLanguageCallGraphTools;

public class HybridModRef<T extends InstanceKey> extends ModRef<T>{
	private JavaScriptModRef<InstanceKey> jsModRef = new JavaScriptModRef<>();
	
	private HybridModRef() {
		// TODO Auto-generated constructor stub
	}
	
	public static ModRef make() {
		return new HybridModRef<>();
	}

	@Override
	protected RefVisitor makeRefVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis<T> pa, ExtendedHeapModel h) {
		if(CrossLanguageCallGraphTools.isJSNode(n)) {
			return jsModRef.makeRefVisitorPublic(n, result, (PointerAnalysis<InstanceKey>) pa, h);
		} else {
			return super.makeRefVisitor(n, result, pa, h);
		}
	}
	
	@Override
	protected ModVisitor makeModVisitor(CGNode n, Collection<PointerKey> result, PointerAnalysis<T> pa, ExtendedHeapModel h,
		   boolean ignoreAllocHeapDefs) {
		if(CrossLanguageCallGraphTools.isJSNode(n)) {
			return jsModRef.makeModVisitorPublic(n, result, (PointerAnalysis<InstanceKey>) pa, h, ignoreAllocHeapDefs);
		}
		return super.makeModVisitor(n, result, pa, h, ignoreAllocHeapDefs);
	}
	
	@Override
	public ExtendedHeapModel makeHeapModel(PointerAnalysis pa, CGNode n) {
		if(CrossLanguageCallGraphTools.isJSNode(n)) {
			return (AstHeapModel)pa.getHeapModel();
		}
	    return super.makeHeapModel(pa, n);
	}
}
