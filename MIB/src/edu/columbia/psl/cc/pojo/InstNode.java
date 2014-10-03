package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class InstNode implements Comparable<InstNode>{
	
	private Var var;
	
	private int idx;
	
	private OpcodeObj op;
	
	private String addInfo = "";
	
	private String fromMethod;
	
	private int linenumber;
	
	private ArrayList<Integer> parentList = new ArrayList<Integer>();
	
	//For freq map, the key is the inst idx, and the value is the frequency
	private TreeMap<Integer, Double> childFreqMap = new TreeMap<Integer, Double>();
	
	public void registerParent(int parentIdx) {
		if (!this.parentList.contains(parentIdx))
			this.parentList.add(parentIdx);
	}
	
	public void setParentList(ArrayList<Integer> parentList) {
		this.parentList = parentList;
	}
	
	public ArrayList<Integer> getParentList() {
		return this.parentList;
	}
	
	public void increChild(int childIdx, double amount) {
		if (this.childFreqMap.containsKey(childIdx)) {
			double count = this.childFreqMap.get(childIdx) + amount;
			this.childFreqMap.put(childIdx, count);
		} else {
			this.childFreqMap.put(childIdx, amount);
		}
	}
	
	public void setChildFreqMap(TreeMap<Integer, Double> childFreqMap) {
		this.childFreqMap = childFreqMap;
	}
	
	public TreeMap<Integer, Double> getChildFreqMap() {
		return this.childFreqMap;
	}
	
	public void setIdx(int idx) {
		this.idx = idx;
	}
	
	public int getIdx() {
		return this.idx;
	}
	
	public void setAddInfo(String addInfo) {
		this.addInfo = addInfo;
	}
	
	public String getAddInfo() {
		return this.addInfo;
	}
	
	public void setOp(OpcodeObj op) {
		this.op = op;
	}
	
	public OpcodeObj getOp() {
		return this.op;
	}
		
	public void setFromMethod(String fromMethod) {
		this.fromMethod = fromMethod;
	}
	
	public String getFromMehtod() {
		return this.fromMethod;
	}
	
	public void setLinenumber(int linenumber) {
		this.linenumber = linenumber;
	}
	
	public int getLinenumber() {
		return this.linenumber;
	}
		
	public void setVar(Var v) {
		this.var = v;
	}
	
	public Var getVar() {
		return this.var;
	}
	
	public boolean isLoad() {
		//Exclude aload series
		if (this.getOp().getCatId() == 1)
			return true;
		else 
			return false;
	}
	
	public boolean isArrayLoad() {
		if (this.getOp().getCatId() == 2)
			return true;
		else
			return false;
	}
	
	public boolean isStore() {
		if (this.getOp().getCatId() == 3)
			return true;
		else 
			return false;
	}
	
	public boolean isArrayStore() {
		if (this.getOp().getCatId() == 4)
			return true;
		else
			return false;
	}
	
	@Override
	public String toString() {
		return this.idx + " " + this.op.getOpcode() + " " + this.op.getInstruction() + " " + this.getAddInfo();
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof InstNode))
			return false;
		
		InstNode tmpNode = (InstNode)o;
		
		if (!tmpNode.toString().equals(this.toString()))
			return false;
		else
			return true;
	}

	@Override
	public int compareTo(InstNode other) {
		// TODO Auto-generated method stub
		return (this.getIdx() > other.getIdx())?1:((this.getIdx() < other.getIdx()))?-1:0;
	}

}
