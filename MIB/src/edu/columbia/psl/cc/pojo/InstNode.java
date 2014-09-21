package edu.columbia.psl.cc.pojo;

import java.util.List;
import java.util.ArrayList;

public class InstNode {
	
	private Var var;
	
	private OpcodeObj op;
	
	public void setOp(OpcodeObj op) {
		this.op = op;
	}
	
	public OpcodeObj getOp() {
		return this.op;
	}
	
	public void setVar(Var v) {
		this.var = v;
	}
	
	public Var getVar() {
		return this.var;
	}
	
	public boolean isLoad() {
		if (this.getOp().getCatId() == 1 || this.getOp().getCatId() == 2)
			return true;
		else 
			return false;
	}
	
	@Override
	public String toString() {
		return op + " " + var;
	}

}
