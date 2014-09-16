package edu.columbia.psl.cc.pojo;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

public class VarPair {
	
	private Var v1;
	
	private Var v2;
	
	//Key is for label, but not sure if we should count label at this point
	private HashMap<String, Set<VarPair>> children = new HashMap<String, Set<VarPair>>();
	
	private HashMap<String, Set<VarPair>> parents = new HashMap<String, Set<VarPair>>();
	
	private HashMap<VarPair, Double> childCoefficientMap = new HashMap<VarPair, Double>();
	
	private HashMap<VarPair, Double> parentCoefficientMap = new HashMap<VarPair, Double>();
	
	private double sigma = 1;
	
	public VarPair(Var v1, Var v2) {
		if (v1.equals(v2)) {
			System.err.println("v1 and v2 are equal. Invalid");
		} else {
			this.v1 = v1;
			this.v2 = v2;
		}
	}
	
	public void setVar1(Var v1) {
		this.v1 = v1;
	}
	
	public Var getVar1() {
		return v1;
	}
	
	public void setVar2(Var v2) {
		this.v2 = v2;
	}
	
	public Var getVar2() {
		return this.v2;
	}
	
	public boolean containChildrenLabel(String label) {
		return this.children.containsKey(label);
	}
	
	public HashMap<String, Set<VarPair>> getChildren() {
		return this.children;
	}
	
	public synchronized void updateChildren(String label, VarPair child) {
		if (this.children.containsKey(label)) {
			this.children.get(label).add(child);
		} else {
			Set<VarPair> childSet = new HashSet<VarPair>();
			childSet.add(child);
			this.children.put(label, childSet);
		}
	}
	
	public HashSet<VarPair> getAll(boolean isChildren) {
		HashSet<VarPair> ret = new HashSet<VarPair>();
		if (isChildren) {
			for (String s: this.children.keySet()) {
				ret.addAll(this.children.get(s));
			}
		} else {
			for (String s: this.parents.keySet()) {
				ret.addAll(this.parents.get(s));
			}
		}
		
		return ret;
	}
	
	public boolean containParentLabel(String label) {
		return this.parents.containsKey(label);
	}
	
	public HashMap<String, Set<VarPair>> getParents() {
		return this.parents;
	}
	
	public synchronized void updateParents(String label, VarPair parent) {
		if (this.parents.containsKey(label)) {
			this.parents.get(label).add(parent);
		} else {
			Set<VarPair> parentSet = new HashSet<VarPair>();
			parentSet.add(parent);
			this.parents.put(label, parentSet);
		}
	}
		
	public synchronized void updateChildCoefficient(VarPair child, double val) {
		this.childCoefficientMap.put(child, val);
	}
	
	public HashMap<VarPair, Double> getChildCoefficient() {
		return  this.childCoefficientMap;
	}
	
	public double getChildCoefficient(VarPair child) {
		return this.childCoefficientMap.get(child);
	}
	
	public synchronized void updateParentCoefficient(VarPair parent, double val) {
		this.parentCoefficientMap.put(parent, val);
	}
	
	public double getParentCoefficient(VarPair parent) {
		return this.parentCoefficientMap.get(parent);
	}
	
	public HashMap<VarPair, Double> getParentCoefficient() {
		return this.parentCoefficientMap;
	}
	
	public synchronized void increSigma(double neighborWeight) {
		this.sigma += neighborWeight;
	}
	
	public synchronized void normalizeSigma(double maxSig) {
		this.sigma /= maxSig;
	}
	
	public double getSigma() {
		return this.sigma;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof VarPair))
			return false;
		
		VarPair tmpVp = (VarPair)o;
		if (tmpVp.getVar1().equals(this.getVar1()) 
				&& tmpVp.getVar2().equals(this.getVar2()))
			return true;
		else
			return false;
	}
	
	@Override
	public String toString() {
		return v1 + " + " + v2;
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

}
