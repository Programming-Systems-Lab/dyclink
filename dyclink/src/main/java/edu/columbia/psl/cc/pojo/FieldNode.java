package edu.columbia.psl.cc.pojo;

import java.util.HashSet;

public class FieldNode extends InstNode {
	
	private HashSet<String> globalChildIdx = new HashSet<String>();
	
	public int writtenObj = -1;
	
	public void addGlobalChild(String childIdx) {
		this.globalChildIdx.add(childIdx);
	}
	
	public void setGlobalChildIdx(HashSet<String> globalChildIdx) {
		this.globalChildIdx = globalChildIdx;
	}
	
	public HashSet<String> getGlobalChildIdx() {
		return this.globalChildIdx;
	}
	
}
