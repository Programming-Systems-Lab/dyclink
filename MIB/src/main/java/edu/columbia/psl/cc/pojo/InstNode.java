package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.util.StringUtil;

public class InstNode {
		
	protected int threadId;
	
	protected int threadMethodIdx;
	
	protected int idx;
	
	protected OpcodeObj op;
	
	public transient OpcodeObj originalOp;
	
	protected String addInfo = "";
	
	public HashSet<Integer> inheritedInfo;
	
	protected String fromMethod = "";
	
	protected int linenumber;
			
	protected long startTime = -1;
	
	protected long updateTime = -1;
	
	protected ArrayList<String> instDataParentList = new ArrayList<String>();
	
	protected ArrayList<String> writeDataParentList = new ArrayList<String>();
	
	protected ArrayList<String> controlParentList = new ArrayList<String>();
	
	//For freq map, the key is the inst method + idx, and the value is the frequency
	protected TreeMap<String, Double> childFreqMap = new TreeMap<String, Double>();
	
	private Object relatedObj = null;
	
	public transient int callerLine;
	
	public transient int repOp = -1;
	
	public InstNode() {
		
	}
	
	public InstNode(InstNode copy) {
		this.threadId = copy.getThreadId();
		this.threadMethodIdx = copy.getThreadMethodIdx();
		this.idx = copy.getIdx();
		this.startTime = copy.getStartTime();
		this.updateTime = copy.getUpdateTime();
		this.op = copy.getOp();
		this.addInfo = copy.getAddInfo();
		this.fromMethod = copy.getFromMethod();
		this.instDataParentList = new ArrayList<String>(copy.getInstDataParentList());
		this.writeDataParentList = new ArrayList<String>(copy.getWriteDataParentList());
		this.controlParentList = new ArrayList<String>(copy.getControlParentList());
		this.childFreqMap = new TreeMap<String, Double>(copy.getChildFreqMap());
	}
	
	/**
	 * depType: 1 for inst data dep, 2 for writ data dep, 3, for control dep
	 * @param fromMethod
	 * @param parentIdx
	 * @param isControl
	 */
	public void registerParent(int threadId, int threadMethodIdx, int parentIdx, int depType) {
		String idxKey = StringUtil.genIdxKey(threadId, threadMethodIdx,  parentIdx);
		if (depType == MIBConfiguration.INST_DATA_DEP && !this.instDataParentList.contains(idxKey)) {
			this.instDataParentList.add(idxKey);
		} else if (depType == MIBConfiguration.WRITE_DATA_DEP && !this.writeDataParentList.contains(idxKey)) {
			this.writeDataParentList.add(idxKey);
		} else if (depType == MIBConfiguration.CONTR_DEP && !this.controlParentList.contains(idxKey)){
			this.controlParentList.add(idxKey);
		}
	}
	
	public void increChild(int threadId, int threadMethodIdx, int childIdx, double amount) {
		String idxKey = StringUtil.genIdxKey(threadId, threadMethodIdx, childIdx);
		if (this.childFreqMap.containsKey(idxKey)) {
			double count = this.childFreqMap.get(idxKey) + amount;
			this.childFreqMap.put(idxKey, count);
		} else {
			this.childFreqMap.put(idxKey, amount);
		}
	}
	
	public void increChildForced(int rheadId, int threadMethodIdx, int childIdx, double amount) {
		String idxKey = StringUtil.genIdxKey(threadId, threadMethodIdx, childIdx);
		this.childFreqMap.put(idxKey, amount);
	}
	
	public void resetChild(String idxKey, double amount) {
		this.childFreqMap.put(idxKey, amount);
	}
		
	public void setInstDataParentList(ArrayList<String> instDataParentList) {
		this.instDataParentList = instDataParentList;
	}
	
	public ArrayList<String> getInstDataParentList() {
		return this.instDataParentList;
	}
	
	public void setWriteDataParentList(ArrayList<String> writeDataParentList) {
		this.writeDataParentList = writeDataParentList;
	}
	
	public ArrayList<String> getWriteDataParentList() {
		return this.writeDataParentList;
	}
	
	public void setControlParentList(ArrayList<String> controlParentList) {
		this.controlParentList = controlParentList;
	}
	
	public ArrayList<String> getControlParentList() {
		return this.controlParentList;
	}
	
	public void updateChild(int threadId, int threadMethodIdx, int childIdx, double amount) {
		String idxKey = StringUtil.genIdxKey(threadId, threadMethodIdx, childIdx);
		this.childFreqMap.put(idxKey, amount);
	}
	
	public void setChildFreqMap(TreeMap<String, Double> childFreqMap) {
		this.childFreqMap = childFreqMap;
	}
	
	public TreeMap<String, Double> getChildFreqMap() {
		return this.childFreqMap;
	}
	
	public void setThreadId(int threadId) {
		this.threadId = threadId;
	}
	
	public int getThreadId() {
		return this.threadId;
	}
	
	public void setThreadMethodIdx(int threadMethodIdx) {
		this.threadMethodIdx = threadMethodIdx;
	}
	
	public int getThreadMethodIdx() {
		return this.threadMethodIdx;
	}
	
	public void setIdx(int idx) {
		this.idx = idx;
	}
	
	public int getIdx() {
		return this.idx;
	}
	
	/*public void setStartDigit(long startDigit) {
		this.startDigit = startDigit;
	}
	
	public long getStartDigit() {
		return this.startDigit;
	}*/
	
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	public long getStartTime() {
		return this.startTime;
	}
	
	/*public void setUpdateDigit(long updateDigit) {
		this.updateDigit = updateDigit;
	}
	
	public long getUpdateDigit() {
		return this.updateDigit;
	}*/
	
	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}
	
	public long getUpdateTime() {
		return this.updateTime;
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
	
	public String getFromMethod() {
		return this.fromMethod;
	}
	
	public void setLinenumber(int linenumber) {
		this.linenumber = linenumber;
		this.callerLine = this.linenumber;
	}
	
	public int getLinenumber() {
		return this.linenumber;
	}
	
	public void setRelatedObj(Object relatedObj) {
		this.relatedObj = relatedObj;
	}
	
	public Object getRelatedObj() {
		return this.relatedObj;
	}
	
	public void removeRelatedObj() {
		this.relatedObj = null;
	}
	
	/*public void setMaxSurrogate(int baseId) {
		this.maxSurrogate.set(baseId);
	}
	
	public int getMaxSurrogate() {
		return this.maxSurrogate.getAndIncrement();
	}
	
	public int probeSurrogate() {
		return this.maxSurrogate.get();
	}*/
	
	@Override
	public String toString() {
		//return this.fromMethod + " " + this.threadId + " " + this.threadMethodIdx + " " + this.idx;
		return this.fromMethod + " " + this.threadId + " " + this.threadMethodIdx + " " + this.idx + " " + this.op.getOpcode() + " " + this.op.getInstruction() + " " + this.getAddInfo();
	}
	
	@Override
	public int hashCode() {
		String myId = StringUtil.genIdxKey(this.threadId, this.threadMethodIdx, this.idx);
		return myId.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof InstNode))
			return false;
		
		InstNode tmpNode = (InstNode)o;
		
		String myId = StringUtil.genIdxKey(this.threadId, this.threadMethodIdx, this.idx);
		String tmpId = StringUtil.genIdxKey(tmpNode.getThreadId(), tmpNode.getThreadMethodIdx(), tmpNode.getIdx());
		
		if (!myId.equals(tmpId))
			return false;
		else
			return true;
	}
}
