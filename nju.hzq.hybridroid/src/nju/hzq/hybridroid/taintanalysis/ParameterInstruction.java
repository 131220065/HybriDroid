package nju.hzq.hybridroid.taintanalysis;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;

public class ParameterInstruction extends SSAInstruction {
	
	private static final ParameterInstruction pi = new ParameterInstruction(-1);
	
	public static ParameterInstruction getInstance() {
		return pi;
	}

	protected ParameterInstruction(int iindex) {
		super(iindex);
	}

	@Override
	public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString(SymbolTable symbolTable) {
		return "Parameter";
	}

	@Override
	public void visit(IVisitor v) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isFallThrough() {
		// TODO Auto-generated method stub
		return false;
	}
	
}