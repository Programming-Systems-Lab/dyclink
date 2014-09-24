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
	
	private List<BlockNode> children = new ArrayList<BlockNode>();
	
	private List<Var> controlVarsToChildren;
	
	private int index;
	
	private int lowLink;
	
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
	
	public List<BlockNode> getChildrenBlock() {
		return this.children;
	}
	
	public void setControlDepVarsToChildren(List<Var> controlVarsToChildren) {
		this.controlVarsToChildren = controlVarsToChildren;
	}
	
	public List<Var> getControlDepVarsToChildren() {
		return this.controlVarsToChildren;
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return this.index;
	}
	
	public void setLowLink(int lowLink) {
		this.lowLink = lowLink;
	}
	
	public int getLowLink() {
		return this.lowLink;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.label.getOffset() + " " + this.label + "\n");
		for (InstNode in: this.insts) {
			sb.append(in + "\n");
		}
		return sb.toString();
	}
	
	@Override
	public boolean equals(Object tmp) {
		if (!(tmp instanceof BlockNode))
			return false;
		
		BlockNode tmpBn = (BlockNode)tmp;
		if (tmpBn.getLabel().equals(this.getLabel()))
			return true;
		else
			return false;
	}
	
	@Override
	public int hashCode() {
		return this.getLabel().hashCode();
	}
}
