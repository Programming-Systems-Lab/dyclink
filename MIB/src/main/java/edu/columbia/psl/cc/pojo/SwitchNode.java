package edu.columbia.psl.cc.pojo;

import java.util.List;
import java.util.ArrayList;

public class SwitchNode extends InstNode{
	
	private List<String> labels = new ArrayList<String>();
	
	public void addLabel(String label) {
		this.labels.add(label);
	}
	
	public List<String> getLabels() {
		return this.labels;
	}
	
	@Override
	public String toString() {
		return this.getOp() + ":" + this.labels;
	}

}
