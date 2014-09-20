package edu.columbia.psl.cc.pojo;

import java.util.List;
import java.util.ArrayList;

public class InstNode {
	
	private List<Var> vars = new ArrayList<Var>();
	
	private OpcodeObj op;
	
	public void setOp(OpcodeObj op) {
		this.op = op;
	}
	
	public OpcodeObj getOp() {
		return this.op;
	}
	
	public void addVar(Var v) {
		this.vars.add(v);
	}
	
	public List<Var> getVars() {
		return this.vars;
	}
	
	public boolean isLoad() {
		if (this.getOp().getCatId() == 1 || this.getOp().getCatId() == 2)
			return true;
		else 
			return false;
	}
	
	@Override
	public String toString() {
		return op + " " + vars;
	}

}
