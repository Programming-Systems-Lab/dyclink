package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.List;

public class CostObj {
	
	private List<String> labels = new ArrayList<String>();;
	
	private double cost;
	
	public void addLabel(String label) {
		this.labels.add(label);
	}
	
	public List<String> getLabels() {
		return labels;
	}
	
	public void setCost(double cost) {
		this.cost = cost;
	}
	
	public double getCost() {
		return this.cost;
	}
	
	@Override
	public String toString() {
		return labels.toString() + " " + cost;
	}
}
