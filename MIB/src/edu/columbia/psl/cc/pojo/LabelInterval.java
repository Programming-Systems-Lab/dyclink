package edu.columbia.psl.cc.pojo;

public class LabelInterval {
	
	private String startLabel;
	
	private String endLabel;
	
	public void setStartLabel(String startLabel) {
		this.startLabel = startLabel;
	}
	
	public String getStartLabel() {
		return this.startLabel;
	}
	
	public void setEndLabel(String endLabel) {
		this.endLabel = endLabel;
	}
	
	public String getEndLabel() {
		return this.endLabel;
	}
	
	@Override
	public String toString() {
		return this.startLabel.toString() + "-" + this.endLabel.toString();
	}

}
