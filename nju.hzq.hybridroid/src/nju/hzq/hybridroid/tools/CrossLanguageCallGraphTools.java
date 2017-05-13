package nju.hzq.hybridroid.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.pruned.PrunedCallGraph;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

public class CrossLanguageCallGraphTools {
	
	public static Scanner scan = new Scanner(System.in);
	
	public static Atom getLanguage(CGNode node) {
	    return node.getMethod().getReference().getDeclaringClass().getClassLoader().getLanguage();
	}
	
	public static boolean isJSNode(CGNode node) {
		return JavaScriptTypes.jsName.equals(getLanguage(node));
	}
	
	//根据方法名找节点
	public static List<CGNode> findMethods(CallGraph cg, String name) {
		List<CGNode> cgList = new ArrayList<>();
		for (Iterator<? extends CGNode> it = cg.iterator(); it.hasNext();) {
		    CGNode n = it.next();
		    if(isJSNode(n)) {
		    	if (n.getMethod().getDeclaringClass().getName().toString().endsWith("/" + name)) {
			    	cgList.add(n);
			    }
		    } else {//java
		    	if (n.getMethod().getName().toString().equals(name)) {
		    		cgList.add(n);
		    	}
		    }
		}
		return cgList;
	}
	
	//获取节点的所有前驱，放在keepNodes中
	public static void getPreNodesOf(CallGraph cg, CGNode targetNode, Set<CGNode> keepNodes) {
		if(keepNodes == null) {
			keepNodes = new HashSet<>();
		}
		
		if(!keepNodes.contains(targetNode)) {//节点没有访问过
			keepNodes.add(targetNode);//用于剪裁然后显示
			
			Iterator<CGNode> iterPredNodes = cg.getPredNodes(targetNode);
			
			while(iterPredNodes.hasNext()) {
				CGNode node = iterPredNodes.next();
				getPreNodesOf(cg, node, keepNodes);
			}
		}
	}
	
	//获取节点的所有后继，放在keepNodes中
	public static void getSuccNodesOf(CallGraph cg, CGNode targetNode, Set<CGNode> keepNodes) {
		if(keepNodes == null) {
			keepNodes = new HashSet<>();
		}
		
		if(!keepNodes.contains(targetNode)) {
			keepNodes.add(targetNode);//用于剪裁然后显示
			
			Iterator<CGNode> iterSuccNodes = cg.getSuccNodes(targetNode);
			
			while(iterSuccNodes.hasNext()) {
				CGNode node = iterSuccNodes.next();
				getSuccNodesOf(cg, node, keepNodes);
			}
		}
		
	}
	
	//获取节点的所有相关节点,放在keepNodes中
	public static void getAllRelatedNodesOf(CallGraph cg, CGNode targetNode, Set<CGNode> keepNodes) {
		getPreNodesOf(cg, targetNode, keepNodes);
		Set<CGNode> keepNodes2 = new HashSet<CGNode>();
		getSuccNodesOf(cg, targetNode, keepNodes2);
		keepNodes.addAll(keepNodes2);
	}
	
	public static CGNode getTargetNodeFromScan(CallGraph cg) {
		System.out.println("请输入方法名：");
		String methodName = scan.next();
		List<CGNode> findNodes = CrossLanguageCallGraphTools.findMethods(cg, methodName);
		if(findNodes.isEmpty()) {
			System.out.println("没有找到方法对应的节点!\n");
			return null;
		}
		CGNode targetNode = findNodes.get(0);
		if(findNodes.size() > 1) {
			System.out.println("找到多个相关节点，请选择一个：");
			for(int i = 0; i < findNodes.size(); i++) {
				System.out.println("序号 = " + (i + 1) + ": " + findNodes.get(i));
			}
			System.out.println("请选择序号：");
			int serialNum = scan.nextInt();
			//输入序号减去1为index
			while(serialNum <= 0 || serialNum > findNodes.size()) {
				System.out.println("序号输入不在有效范围，请重新输入：");
				serialNum = scan.nextInt();
			}
			targetNode = findNodes.get(serialNum - 1);
		}
		System.out.println("找到节点：" + targetNode);
		return targetNode;
	}
	
	//从用户输入获取中间表示指令index
	public static int getInstructionIndexFromScan(CGNode node) {
		if(node == null) {
			Assertions.UNREACHABLE("node cannot be null");
		}
		SSAInstruction[] insts = node.getIR().getInstructions();
		System.out.println("SSAInstruction如下：");
		for(int i = 0; i < insts.length; i++) {
			System.out.println("序号 = " + (i + 1) + " : " + node.getIR().getInstructionString(i));
		}
		System.out.println("请输入序号选择其中一个instruction");
		int serialNum = 0;
		SSAInstruction inst = null;
		while(inst == null || (inst.getNumberOfDefs() == 0 && inst.getNumberOfUses() == 0)) {
			serialNum  = scan.nextInt();
			while(serialNum <= 0 || serialNum > insts.length) {
				System.out.println("输入序号不在有效范围，请重新输入：");
				serialNum = scan.nextInt();
			}
			inst = node.getIR().getInstructions()[serialNum - 1];
		}
//		SymbolTable st = node.getIR().getSymbolTable();
//		System.out.println("这条指令定义的变量：");
//		for(int i = 0; i < inst.getNumberOfDefs(); i++) {
//			int var = inst.getDef(i);
//			System.out.println(st.getValueString(var) );
//		}
//		System.out.println("这条指令使用的变量：");
//		for(int i = 0; i < inst.getNumberOfUses(); i++) {
//			int var = inst.getUse(i);
//			System.out.println(st.getValueString(var) );
//		}
		return serialNum - 1;
	}
	
	public static int getVarFromScan(CGNode node, int ii) {
		if(node == null) {
			Assertions.UNREACHABLE("node cannot be null");
		}
		System.out.println("您选择的指令是：");
		System.out.println(node.getIR().getInstructionString(ii));
		SymbolTable st = node.getIR().getSymbolTable();
		SSAInstruction inst = node.getIR().getInstructions()[ii];
		List<Integer> varList = new ArrayList<>();
		int defNum = inst.getNumberOfDefs();
		int useNum = inst.getNumberOfUses();
		int serialNumer = 1;
		System.out.println("这条指令定义的变量：");
		for(int i = 0; i < defNum; i++) {
			int var = inst.getDef(i);
			varList.add(var);
			System.out.println("序号 = " + serialNumer + " : " + st.getValueString(var));
			serialNumer++;
		}
		System.out.println("这条指令使用的变量：");
		for(int i = 0; i < useNum; i++) {
			int var = inst.getUse(i);
			varList.add(var);
			System.out.println("序号 = " + serialNumer + " : "  + st.getValueString(var) );
			serialNumer++;
		}
		System.out.println("请输入变量序号选择变量：");
		int serialNum = scan.nextInt();
		while(serialNum <= 0 || serialNum > varList.size()) {
			System.out.println("输入序号不在有效范围，请重新输入：");
			serialNum = scan.nextInt();
		}
		int var = varList.get(serialNum - 1);
		System.out.println("您选择的变量是: " + st.getValueString(var) );
		return var;
	}
	
	public static PointerKey getVarFromScan(CGNode node, PointerAnalysis<InstanceKey> pa, int ii) {
		if(node == null || pa == null) {
			Assertions.UNREACHABLE("node or pa cannot be null");
		}
		int var = getVarFromScan(node, ii);
		return pa.getHeapModel().getPointerKeyForLocal(node, var);
	}
	
	//从切片中获取nodes
	public static Set<CGNode> getNodesFromSlice(Collection<Statement> slice) {
		Set<CGNode> keepNodes = new HashSet<>();
		
		for(Statement s : slice) {
			if(!keepNodes.contains(s.getNode())) {
				keepNodes.add(s.getNode());
			}
		}
		
		return keepNodes;
	}
	
	
	public static Set<CGNode> collectNodesRelatedToJS(CallGraph cg) {
		Set<CGNode> keepNodes = new HashSet<>();
		for(CGNode n : cg) {
			if(isJSNode(n)) {
				if(n.getMethod().getName().toString().equals("ctor")) {
					continue;
				}
				String classString = n.getMethod().getDeclaringClass().getName().toString();
				if(classString.startsWith("Lprologue.js") || classString.startsWith("Lpreamble.js") || classString.startsWith("LFakeRoot")) {
					continue;
				}
				System.out.println("n = " + n);
				System.out.println("nmethod name = " + n.getMethod().getName());
				System.out.println("nmethod = " + n.getMethod());
				System.out.println("nclass name = " + n.getMethod().getDeclaringClass().getName());
				System.out.println("nclass = " + n.getMethod().getDeclaringClass());
				getSuccNodesOf(cg, n, keepNodes);
			}
		}
		return keepNodes;
	}
	
	public static CallGraph prunCallGraphToRelatedJSNodes(CallGraph cg) {
		return new PrunedCallGraph(cg, collectNodesRelatedToJS(cg));
	}
	
}
