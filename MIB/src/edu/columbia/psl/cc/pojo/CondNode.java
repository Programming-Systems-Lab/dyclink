package edu.columbia.psl.cc.pojo;

public class CondNode extends InstNode {
	
	private static String defaultLabel = "straight";
	
	private String label;
	
	public static String getDefaultLabel() {
		return defaultLabel;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return this.label;
	}
	
	public boolean isGoto() {
		if (this.getOp().getOpcode() == 167)
			return true;
		else
			return false;
	}
	
	@Override
	public String toString() {
		return this.getOp() + ":" + this.label; 
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
		if (!cn.getOp().equals(this.getOp()))
			return false;
		
		if (cn.getLabel().equals(this.label))
			return false;
		
		return true;
	}
}

