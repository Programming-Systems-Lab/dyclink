package edu.columbia.psl.cc.pojo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CondNode extends BCTreeNode {
	
	private static String defaultLabel = "straight";
	
	private int opcode;
	
	private String label;
	
	private Map<String, BCTreeNode> labelMap = new HashMap<String, BCTreeNode>();
	
	public static String getDefaultLabel() {
		return defaultLabel;
	}

	public void setOpcode(int opcode) {
		this.opcode = opcode;
	}
	
	public int getOpcode() {
		return this.opcode;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return this.label;
	}
	
	@Override
	public void addChild(BCTreeNode bn, String label) {
		if (label == null) {
			this.labelMap.put(defaultLabel, bn);
		} else {
			this.labelMap.put(label, bn);
		}
	}
	
	@Override
	public Set<BCTreeNode> getChildren() {
		Set<BCTreeNode> ret = new HashSet<BCTreeNode>();
		for (String label: this.labelMap.keySet()) {
			ret.add(this.labelMap.get(label));
		}
		return ret;
	}
	
	public BCTreeNode getChildByLabel(String label) {
		return this.labelMap.get(label);
	}
	
	@Override
	public String toString() {
		return this.opcode + ":" + this.label; 
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof CondNode))
			return false;
		
		CondNode cn = (CondNode)o;
		if (cn.getOpcode() != this.opcode)
			return false;
		
		if (cn.getLabel().equals(this.label))
			return false;
		
		return true;
	}
}

