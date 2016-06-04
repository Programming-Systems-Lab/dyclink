package edu.columbia.psl.cc.pojo;

import java.util.HashMap;

import edu.columbia.psl.cc.util.StringUtil;

public class VarEdge {
	
	private HashMap<Var, String> children = new HashMap<Var, String>();
	
	public void addChild(String parentSil, Var childVar) {
		//Infer the relation
		String relation = StringUtil.genRelation(parentSil, childVar.getSil());
		this.children.put(childVar, relation);
	}
	
	public String getChildLabel(Var childVar) {
		return this.children.get(childVar);
	}
	
	public HashMap<Var, String> getChildren() {
		return this.children;
	}
}
