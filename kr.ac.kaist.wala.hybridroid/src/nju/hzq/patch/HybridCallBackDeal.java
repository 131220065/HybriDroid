package nju.hzq.patch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IInvokeInstruction.IDispatch;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

import nju.hzq.stub.HzqStub;

public class HybridCallBackDeal extends HybridCallBackResult{
	private Map<String, List<CGNode>> callbackMap;
	private final String alert = "alert";
	private final String confirm = "confirm";
	private final String prompt = "prompt";
	
	private CallGraph cg;
	private IClassHierarchy cha;
	private IClass webViewClient;
	
	public static void dealWithHybridCallBack(CallGraph cg) {
		HybridCallBackDeal hcbd = new HybridCallBackDeal(cg);
		hcbd.dealWithCallBack();
		HybridCallBackResult.callbackResult = hcbd;
	}
	
	private HybridCallBackDeal(CallGraph cg) {
		this.cg = cg;
		this.cha = cg.getClassHierarchy();
		callbackMap = new HashMap<>();
		callbackMap.put(alert, new ArrayList<CGNode>());
		callbackMap.put(confirm, new ArrayList<CGNode>());
		callbackMap.put(prompt, new ArrayList<CGNode>());
		webChromeClient = cha.lookupClass(TypeReference.find(ClassLoaderReference.Primordial, TypeName.findOrCreate("Landroid/webkit/WebChromeClient")));
		webViewClient = cha.lookupClass(TypeReference.find(ClassLoaderReference.Primordial, TypeName.findOrCreate("Landroid/webkit/WebViewClient")));
	}
	
	private void collectCallBackNodes() {
		Collection<IClass> wccSubClasses = cha.computeSubClasses(webChromeClient.getReference());
		for(IClass wcc : wccSubClasses) {
			if(!wcc.equals(webChromeClient)) {
				IMethod alertMethod = wcc.getMethod(Selector.make(onJSAlertSelector));
				if(alertMethod != null) {
					CGNode n = cg.getNode(alertMethod, Everywhere.EVERYWHERE);
					HzqStub.stubPrint("onJSAlert = " + n);
					if(n != null) {
						callbackMap.get(alert).add(n);
					}
				}
				IMethod confirmMethod = wcc.getMethod(Selector.make(onJSConfirmSelector));
				if(confirmMethod != null) {
					CGNode n = cg.getNode(confirmMethod, Everywhere.EVERYWHERE);
					HzqStub.stubPrint("onJSConfirm = " + n);
					if(n != null) {
						callbackMap.get(confirm).add(n);
					}
				}
				IMethod promptMethod = wcc.getMethod(Selector.make(onJSPromptSelector));
				if(promptMethod != null) {
					CGNode n = cg.getNode(promptMethod, Everywhere.EVERYWHERE);
					HzqStub.stubPrint("onJSPrompt = " + n);
					if(n != null) {
						callbackMap.get(prompt).add(n);
					}
				}
			}
		}
	}
	
	private void dealWithCallBack() {
		collectCallBackNodes();
		for(CGNode node : cg) {
			if(HzqHybridTools.isJSNode(node) && node.getMethod().getName().toString().equals("do")) {
				for(Iterator<CallSiteReference> icsr = node.getIR().iterateCallSites(); icsr.hasNext();){
					CallSiteReference csr = icsr.next();
					SSAAbstractInvokeInstruction[] invokes = node.getIR().getCalls(csr);
					for(SSAAbstractInvokeInstruction invoke : invokes) {
						if(invoke.getDeclaredTarget().getName().toString().equals("do")) {
							int methodVar = invoke.getUse(0);
							SSAInstruction methodDefInst = node.getDU().getDef(methodVar);
							if(methodDefInst instanceof AstGlobalRead) {
								AstGlobalRead agr = (AstGlobalRead) methodDefInst;
								if(agr.getGlobalName().equals("global " + alert)) {
									for(CGNode n : callbackMap.get(alert)) {
										node.addTarget(csr, n);
									}
								}
								if(agr.getGlobalName().equals("global " + confirm)) {
									for(CGNode n : callbackMap.get(confirm)) {
										node.addTarget(csr, n);
									}
								}
								if(agr.getGlobalName().equals("global " + prompt)) {
									for(CGNode n : callbackMap.get(prompt)) {
										node.addTarget(csr, n);
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
