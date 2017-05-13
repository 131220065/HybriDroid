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
package nju.hzq.stub;

public class HzqStub {
  private static final boolean STUB = true;
  
  public static void stubPrint(String message) {
    if(STUB) {  
      StackTraceElement[] stacks = new Throwable().getStackTrace();    
  
      System.err.println("hzq: " + message + "\n\t##in class " + stacks[1].getClassName() + "." + stacks[1].getMethodName() );
    }
  }
  
  public static void stubModified(String message) {
    
  }
}
