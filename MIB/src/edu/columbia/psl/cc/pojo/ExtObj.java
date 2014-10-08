package edu.columbia.psl.cc.pojo;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeSet;

public class ExtObj {
	
	private int lineNumber;
	
	//Which field-writings affect the associated method
	private TreeSet<InstNode> writeFieldInsts = new TreeSet<InstNode>();
	
	//Which read-local vars affect the associated method
	private LinkedList<InstNode> loadLocalInsts = new LinkedList<InstNode>();
	
	//Which fields affected by the associated method
	private TreeSet<InstNode> affFieldInsts = new TreeSet<InstNode>();
	
	public ExtObj() {
		
	}
	
	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}
	
	public int getLineNumber() {
		return this.lineNumber;
	}
	
	public void setWriteFieldInsts(TreeSet<InstNode> writeFieldInsts) {
		this.writeFieldInsts = writeFieldInsts;
	}
	
	public TreeSet<InstNode> getWriteFieldInsts() {
		return this.writeFieldInsts;
	}
	
	public void setLoadLocalInsts(LinkedList<InstNode> writeLocalInsts) {
		this.loadLocalInsts = writeLocalInsts;
	}
	
	public LinkedList<InstNode> getLoadLocalInsts() {
		return this.loadLocalInsts;
	}
	
	public void setAffFieldInsts(TreeSet<InstNode> affFieldInsts) {
		this.affFieldInsts = affFieldInsts;
	}
	
	public TreeSet<InstNode> getAffFieldInsts() {
		return this.affFieldInsts;
	}
	
	public void addWriteFieldInst(InstNode fieldInst) {
		this.writeFieldInsts.add(fieldInst);
	}
	
	public void addLoadLocalInst(InstNode localInst) {
		this.loadLocalInsts.addFirst(localInst);
	}
	
	public void addAffFieldInst(InstNode affFieldInst) {
		this.affFieldInsts.add(affFieldInst);
	}

}
