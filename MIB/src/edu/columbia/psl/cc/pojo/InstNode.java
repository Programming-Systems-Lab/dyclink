package edu.columbia.psl.cc.pojo;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

public class InstNode {
	
	private static String loadString = "load";
	
	private static String storeString = "store";
	
	//For vars, it's possible to have multiple one. For cond, it's only one
	private List<Var> vars = new ArrayList<Var>();
	
	private Set<InstNode> childrenNodes = new HashSet<InstNode>();
	
	private boolean load = false;
	
	public void setLoad(boolean load) {
		this.load = load;
	}
	
	public boolean isLoad() {
		return this.load;
	}
	
	public void addVar(Var v) {
		this.vars.add(v);
	}
	
	public List<Var> getVars() {
		return this.vars;
	}
	
	public void addChild(InstNode bn, String label) {
		this.childrenNodes.add(bn);
	}
	
	public Set<InstNode> getChildren() {
		return this.childrenNodes;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (load) {
			sb.append(loadString + " ");
		} else {
			sb.append(storeString + " ");
		}
		sb.append(this.vars.toString());
		return sb.toString();
	}
	

}
