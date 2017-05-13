package nju.hzq.hybridroid.slice.util;

import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Statement;

public class SourcePositionTools {
	public static void getSourcePositionOf(Statement s) {
		if (s.getKind() == Statement.Kind.NORMAL) { // ignore special kinds of statements
			  int bcIndex, instructionIndex = ((NormalStatement) s).getInstructionIndex();
			  try {
			  bcIndex = ((ShrikeBTMethod) s.getNode().getMethod()).getBytecodeIndex(instructionIndex);
			  try {
			  int src_line_number = s.getNode().getMethod().getLineNumber(bcIndex);
			  System.out.println ( "Source line number = " + src_line_number );
			  } catch (Exception e) {
			  //System.out.println("Bytecode index no good");
			  //System.out.println(e.getMessage());
			  }
			  } catch (Exception e ) {
			  //System.out.println("it's probably not a BT method (e.g. it's a fakeroot method)");
			  //System.out.println(e.getMessage());
			  }
			}

	}
}
