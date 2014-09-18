package edu.columbia.psl.cc.pojo;

import java.util.List;
import java.util.ArrayList;

public class BlockNode {
	
	private static String defaultLabel = "Nonlabel";
	
	private String label;
	
	private List<InstNode> insts = new ArrayList<InstNode>();
	
	public void setLabel(String label) {
		if (label == null)
			this.label = defaultLabel;
		else
			this.label = label;
	}
	
	public String getLabel() {
		return this.label;
	}
	
	public void addInst(InstNode var) {
		this.insts.add(var);
	}
	
	public List<InstNode> getInsts() {
		return insts;
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
