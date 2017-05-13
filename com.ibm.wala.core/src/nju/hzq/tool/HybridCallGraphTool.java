/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package nju.hzq.tool;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.strings.Atom;

public class HybridCallGraphTool {
  public static Atom getLanguage(CGNode node) {
    return node.getMethod().getReference().getDeclaringClass().getClassLoader().getLanguage();
  }

  public static boolean isJavaNode(CGNode node) {
    return ClassLoaderReference.Java.equals(getLanguage(node));
  }

}
