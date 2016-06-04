package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;

public class Dependency<T> {
	
	private ArrayList<T> parents = new ArrayList<T>();
	
	private T child;
	
	public void addParent(T parent) {
		this.parents.add(parent);
	}
	
	public ArrayList<T> getParents() {
		return this.parents;
	}
	
	public void setChild(T child) {
		this.child = child;
	}
	
	public T getChild() {
		return this.child;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (T parent: this.parents) {
			sb.append(parent.toString() + "->" + child.toString() + "\n");
		}
		return sb.toString();
	}

}
