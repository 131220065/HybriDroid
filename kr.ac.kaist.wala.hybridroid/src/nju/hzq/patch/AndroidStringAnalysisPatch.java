package nju.hzq.patch;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

import kr.ac.kaist.wala.hybridroid.analysis.string.ArgumentHotspot;
import kr.ac.kaist.wala.hybridroid.analysis.string.Hotspot;

public class AndroidStringAnalysisPatch {
	public static IClassHierarchy subClassOfWebViewPatch(List<Hotspot> hotspots, AnalysisScope scope) {
		IClassHierarchy cha;
		try {
			cha = ClassHierarchy.make(scope);
			Collection<IClass> webViewSubClasses = cha
					.computeSubClasses(TypeReference.find(ClassLoaderReference.Primordial, "Landroid/webkit/WebView"));
			for (IClass webViewSubClass : webViewSubClasses) {// WebView的子类（包括WebView）
				TypeName tn = webViewSubClass.getName();
				String cDescriptor = tn.toString().substring(1);
				System.out.println("hzq: subclass = " + cDescriptor);
				if (!cDescriptor.contains("/")) {// add
					cDescriptor += '/';
				}
				hotspots.add(new ArgumentHotspot(ClassLoaderReference.Application, cDescriptor,
						"loadUrl(Ljava/lang/String;)V", 0));
			}
			return cha;
		} catch (ClassHierarchyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // 类层次？
		return null;

	}
	
	public static List<String> unzipDexFiles(String apkPath, String toFolderPath) {
		//解压apk中的*.dex解压到指定文件夹，并返回dexfile的绝对路径，如果没有dexfile，返回空ArrayList
				List<String> dexPaths = new ArrayList<>();
				
				try {
					ZipFile apkZipFile = new ZipFile(new File(apkPath));
					Enumeration<? extends ZipEntry> entries = apkZipFile.entries();
					ZipEntry entry = null;
					while(entries.hasMoreElements()) {
						entry = entries.nextElement();
						if(!entry.isDirectory() && entry.getName().endsWith(".dex")) {
							//解压
							String toFilePath = toFolderPath + "/" + entry.getName();
							File toFile = new File(toFilePath);
							BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(toFile));
							BufferedInputStream bis = new BufferedInputStream(apkZipFile.getInputStream(entry));
							int count = 0, bufferSize = 1024; 
							byte[] buffer = new byte[bufferSize];
							while((count = bis.read(buffer, 0, bufferSize)) != -1) {
								bos.write(buffer, 0, count);
							}

							bos.flush();
							bos.close();
							bis.close();
							
							dexPaths.add(toFilePath);
						}
					}
					
					apkZipFile.close();
				} catch (ZipException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
				return dexPaths;
	}
}
