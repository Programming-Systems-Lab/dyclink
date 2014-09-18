package edu.columbia.psl.cc.pojo;

import java.util.HashSet;
import java.util.Set;

public class BCTreeNode {
	
	//For vars, it's possible to have multiple one. For cond, it's only one
	private Set<Var> node = new HashSet<Var>();
	
	private Set<BCTreeNode> childrenNodes = new HashSet<BCTreeNode>();
	
	public void expandNode(Var v) {
		this.node.add(v);
	}
	
	public Set<Var> getNode() {
		return this.node;
	}
	
	public void addChild(BCTreeNode bn, String label) {
		this.childrenNodes.add(bn);
	}
	
	public Set<BCTreeNode> getChildren() {
		return this.childrenNodes;
	}
	

}
