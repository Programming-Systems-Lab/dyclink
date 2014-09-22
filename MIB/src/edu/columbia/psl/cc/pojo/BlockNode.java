package edu.columbia.psl.cc.pojo;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import org.objectweb.asm.Label;

public class BlockNode {
	
	private static String defaultLabel = "Nonlabel";
	
	private Label label;
	
	private List<InstNode> insts = new ArrayList<InstNode>();
	
	private Set<BlockNode> children = new HashSet<BlockNode>();
	
	private List<Var> controlVarsToChildren;
	
	public void setLabelObj(Label labelObj) {
		this.label = labelObj;
	}
	
	public Label getLabelObj() {
		return this.label;
	}
	
	public String getLabel() {
		return this.label.toString();
	}
	
	public void addInst(InstNode var) {
		this.insts.add(var);
	}
	
	public List<InstNode> getInsts() {
		return insts;
	}
	
	public void addChildBlock(BlockNode bn) {
		this.children.add(bn);
	}
	
	public Set<BlockNode> getChildrenBlock() {
		return this.children;
	}
	
	public void setControlDepVarsToChildren(List<Var> controlVarsToChildren) {
		this.controlVarsToChildren = controlVarsToChildren;
	}
	
	public List<Var> getControlDepVarsToChildren() {
		return this.controlVarsToChildren;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.label + "\n");
		for (InstNode in: this.insts) {
			sb.append(in + "\n");
		}
		return sb.toString();
	}
}
