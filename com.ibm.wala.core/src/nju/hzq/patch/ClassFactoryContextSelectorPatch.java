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
package nju.hzq.patch;

import java.util.Collection;
import java.util.Set;

import com.ibm.wala.analysis.reflection.JavaTypeContext;
import com.ibm.wala.analysis.typeInference.PointType;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

public class ClassFactoryContextSelectorPatch {
  public static Context classForNameNotStringConstant(CGNode caller, IR ir, SSAAbstractInvokeInstruction[] invokeInstructions) {
  //hzq: 不是String常量怎么办？找cast，替换成它的子类
    System.out.println("hzq: 不是String常量");
    SSAInstruction[] insts = ir.getInstructions();
    for(SSAInstruction inst : insts) {
      if(inst instanceof SSACheckCastInstruction) {
        SSACheckCastInstruction castInst = (SSACheckCastInstruction) inst;
        if(castInst.iindex < invokeInstructions[0].iindex) {
          continue;
        }
        //System.out.println("castIndex = " + castInst.iindex + ", forNameIndex = " + invokeInstructions[0].iindex);
        TypeReference[] castTypes = castInst.getDeclaredResultTypes();
        if(castTypes.length != 1) {
          return null;
        }
        TypeReference t = castTypes[0];
        System.out.println("castType = " + castTypes[0]);
        IClass castClass = caller.getClassHierarchy().lookupClass(t);
        if(castClass == null) {
          Assertions.UNREACHABLE();
          return null;
        }
        if(castClass.isInterface() || castClass.isAbstract()) {
          if(castClass.isInterface()) {
            Set<IClass> classes = caller.getClassHierarchy().getImplementors(t);
            if(classes.size() <= 0) {
              return null;
            }
            castClass = (IClass) classes.toArray()[0];
          }
          while(castClass.isInterface() || castClass.isAbstract()) {
             Collection<IClass> classes = caller.getClassHierarchy().getImmediateSubclasses(castClass);
            if(classes.size() < 2) {
              return null;
            }
            castClass = (IClass) classes.toArray()[1];
          }
          System.out.println("hzq: 实际Type = " + castClass);
          return new JavaTypeContext(new PointType(castClass));
          
        } else {
          System.out.println("hzq: 实际Type = " + castClass);
          return new JavaTypeContext(new PointType(castClass));
        }
      }
    }
    return null;
  }
}
