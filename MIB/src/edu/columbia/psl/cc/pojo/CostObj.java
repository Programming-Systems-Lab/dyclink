package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.List;

public class CostObj {
	
	private List<String> labels = new ArrayList<String>();;
	
	private int cost;
	
	public void addLabel(String label) {
		this.labels.add(label);
	}
	
	public List<String> getLabels() {
		return labels;
	}
	
	public void setCost(int cost) {
		this.cost = cost;
	}
	
	public int getCost() {
		return this.cost;
	}
}
