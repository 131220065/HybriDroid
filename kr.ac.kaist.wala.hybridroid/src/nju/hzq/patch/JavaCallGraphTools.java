package nju.hzq.patch;

import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;

public class JavaCallGraphTools {

	public static void TestJavaTypeHierarchyManually(IClassHierarchy cha) {
		Scanner scan = new Scanner(System.in);
		System.out.println("����1(ԭ����2��Ӧ�ã�3����չ����һ���ַ�������1 Lcom/netease/cloudmusic/theme/ui/CustomThemeSwitch");
		int androidType = scan.nextInt();
		TypeReference tr = null;
		IClass klass = null;
		while (androidType != -1) {
			String classSelector = scan.next();
			ClassLoaderReference clr = null;
			if (androidType == 1)
				clr = ClassLoaderReference.Primordial;
			else if (androidType == 2) {
				clr = ClassLoaderReference.Application;
			} else if (androidType == 3) {
				clr = ClassLoaderReference.Extension;
			} else if (androidType == 4) {

			}
			tr = TypeReference.find(clr, classSelector);
			System.out.println("hzq: tr = " + tr);
			klass = cha.lookupClass(tr);
			System.out.println("hzq: klass = " + klass);

			System.out.println("����1��ȡ���࣬2��ȡ����, 3��ȡʵ��interface����");
			int dowhat = scan.nextInt();
			if (dowhat == 1) {
				IClass[] subClasses = (IClass[]) cha.getImmediateSubclasses(klass).toArray();

				for (IClass subClass : subClasses) {
					System.out.println("hzq: subClass = " + subClass);
				}
				System.out.println("\n");
			} else if (dowhat == 2) {
				System.out.println("hzq: superClass = " + klass.getSuperclass());
			} else if (dowhat == 3) {
				Set<IClass> implementClasses = cha.getImplementors(tr);

				for (IClass implementClass : implementClasses) {
					System.out.println("hzq: implementor = " + implementClass);
				}
				System.out.println("\n");
			}

			System.out.println("\n����1(ԭ����2��Ӧ�ã�3����չ����һ���ַ�������1 Lcom/netease/cloudmusic/theme/ui/CustomThemeSwitch");
			androidType = scan.nextInt();
		}
	}

	public static void TestJavaCallGraphManually(CallGraph cg) {
		// hzq: test start
		{
			//PDFCallGraph.run(cg);

			System.out.println(
					"\n����1(ԭ����2��Ӧ�ã�3����չ��4(ݔ��ir����һ���ַ�������1 Lcom/netease/cloudmusic/theme/ui/CustomThemeSwitch loadUrl(Ljava/lang/String;)V");
			Scanner scan = new Scanner(System.in);
			int androidType = scan.nextInt();
			CGNode findNode = null;
			while (androidType != -1) {

				try {
					ClassLoaderReference clr = null;
					if (androidType == 1)
						clr = ClassLoaderReference.Primordial;
					else if (androidType == 2) {
						clr = ClassLoaderReference.Application;
					} else if (androidType == 3) {
						clr = ClassLoaderReference.Extension;
					} else if (androidType == 4) {
						if (findNode == null) {
							System.out.println("ݔ��oЧ��Ո����ݔ�룡");
							androidType = scan.nextInt();
							continue;
						}
						System.out.println("hzq: ir = " + findNode.getIR());

						System.out.println(
								"\n����1(ԭ����2��Ӧ�ã�3����չ��4(ݔ��ir����һ���ַ�������1 Lcom/netease/cloudmusic/theme/ui/CustomThemeSwitch loadUrl(Ljava/lang/String;)V");
						androidType = scan.nextInt();
						continue;
					} else {
						if (findNode == null) {
							System.out.println("ݔ��oЧ��Ո����ݔ�룡");
							androidType = scan.nextInt();
							continue;
						}
					}

					String classSelector = scan.next();

					String methodSelector = scan.next();

					TypeReference tr = TypeReference.find(clr, classSelector);
					System.out.println("hzq: tr = " + tr);
					IClass klass = cg.getClassHierarchy().lookupClass(tr);
					System.out.println("hzq: klass = " + klass);

					Selector slt = Selector.make(methodSelector);
					IMethod findMethod = klass.getMethod(slt);
					findNode = cg.getNode(findMethod, Everywhere.EVERYWHERE);

					System.out.println("hzq: findNode = " + findNode);

					System.out.println("������1����ȡ�ڵ�ĺ�̣�2����ȡ�ڵ��ǰ�����������޲�����");
					int doWhat = scan.nextInt();
					if (doWhat == 1) {
						Iterator<CGNode> succNodes = cg.getSuccNodes(findNode);
						while (succNodes.hasNext()) {
							System.out.println("hzq: next = " + succNodes.next() + "\n");
						}
					} else if (doWhat == 2) {
						Iterator<CGNode> preNodes = cg.getPredNodes(findNode);
						while (preNodes.hasNext()) {
							System.out.println("hzq: pre = " + preNodes.next() + "\n");
						}
					}

					System.out.println(
							"\n����1(ԭ����2��Ӧ�ã�3����չ��4(ݔ��ir����һ���ַ�������1 Lcom/netease/cloudmusic/theme/ui/CustomThemeSwitch loadUrl(Ljava/lang/String;)V");
					androidType = scan.nextInt();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.out.println(
							"\n����1(ԭ����2��Ӧ�ã�3����չ��4(ݔ��ir����һ���ַ�������1 Lcom/netease/cloudmusic/theme/ui/CustomThemeSwitch loadUrl(Ljava/lang/String;)V");
					androidType = scan.nextInt();
				}
			}

			// scan.close();
		}
		// hzq: test end
	}
}
