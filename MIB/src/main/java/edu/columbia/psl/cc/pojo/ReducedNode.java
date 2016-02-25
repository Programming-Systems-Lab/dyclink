package edu.columbia.psl.cc.pojo;

public class ReducedNode extends InstNode{
	
	private OpcodeObj originalOp;
	
	public ReducedNode(InstNode original) {
		this.threadId = original.threadId;
		this.threadMethodIdx = original.threadMethodIdx;
		this.idx = original.idx;
		this.startTime = original.startTime;
		this.updateTime = original.updateTime;
		this.fromMethod = original.fromMethod;
		this.linenumber = original.linenumber;
		this.addInfo = original.addInfo;
		this.originalOp = original.op;
		this.instDataParentList = original.instDataParentList;
		this.writeDataParentList = original.writeDataParentList;
		this.controlParentList = original.controlParentList;
		this.childFreqMap = original.childFreqMap;
	}
	
	public OpcodeObj getOriginalOp() {
		return this.originalOp;
	}
}
