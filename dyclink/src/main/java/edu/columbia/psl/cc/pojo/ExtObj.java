package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.TreeSet;

public class ExtObj {
	
	private int instIdx;
	
	private int lineNumber;
	
	//Which field-writings affect the associated method
	private TreeSet<InstNode> writeFieldInsts = new TreeSet<InstNode>();
	
	//Which read-local vars affect the associated method
	private ArrayList<InstNode> loadLocalInsts = new ArrayList<InstNode>();
	
	//Which fields affected by the associated method
	private TreeSet<InstNode> affFieldInsts = new TreeSet<InstNode>();
	
	public ExtObj() {
		
	}
	
	public void setInstIdx(int instIdx) {
		this.instIdx = instIdx;
	}
	
	public int getInstIdx() {
		return this.instIdx;
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
	
	public void setLoadLocalInsts(ArrayList<InstNode> writeLocalInsts) {
		this.loadLocalInsts = writeLocalInsts;
	}
	
	public ArrayList<InstNode> getLoadLocalInsts() {
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
		this.loadLocalInsts.add(localInst);
	}
	
	public void addAffFieldInst(InstNode affFieldInst) {
		this.affFieldInsts.add(affFieldInst);
	}

}
