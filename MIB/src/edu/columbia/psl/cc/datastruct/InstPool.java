package edu.columbia.psl.cc.datastruct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.util.StringUtil;

public class InstPool extends TreeSet<InstNode> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static Logger logger = Logger.getLogger(InstPool.class);
	
	public static boolean DEBUG;
	
	private HashMap<String, InstNode> instMap = new HashMap<String, InstNode>();
	
	public InstPool() {
		
	}
	
	private boolean _addInst(String idxKey, InstNode inst) {
		InstNode check = this.instMap.put(idxKey, inst);
		return super.add(inst) && (check != null);
	}
	
	private boolean _removeInst(InstNode inst) {
		String idxKey = StringUtil.genIdxKey(inst.getFromMethod(), inst.getThreadId(), inst.getThreadMethodIdx(), inst.getIdx());
		InstNode check = this.instMap.remove(idxKey);
		return super.remove(inst) && (check != null);
	}

	public InstNode searchAndGet(String methodKey, long threadId, int threadMethodIdx, int idx, int opcode, String addInfo) {
		String idxKey = StringUtil.genIdxKey(methodKey, threadId, threadMethodIdx, idx);
		if (this.instMap.containsKey(idxKey)) {
			return this.instMap.get(idxKey); 
		}
		
		/*for (InstNode inst: this) {
			if (inst.getFromMethod().equals(methodKey) && 
					inst.getThreadId() == threadId && 
					inst.getThreadMethodIdx() == threadMethodIdx && 
					inst.getIdx() == idx && 
					inst.getOp().getOpcode() == opcode && 
					inst.getAddInfo().equals(addInfo))
				return inst;
		}*/
		
		//Create new 
		InstNode probe = new InstNode();
		probe.setFromMethod(methodKey);
		probe.setThreadId(threadId);
		probe.setThreadMethodIdx(threadMethodIdx);
		probe.setIdx(idx);
		probe.setOp(BytecodeCategory.getOpcodeObj(opcode));
		probe.setAddInfo(addInfo);
		this._addInst(idxKey, probe);
		return probe;
	}
	
	public InstNode searchAndGet(String idxKey) {
		if (this.instMap.containsKey(idxKey)) {
			return this.instMap.get(idxKey);
		}
		
		if (DEBUG) {
			logger.warn("Cannot find inst by method key and idx: " +  idxKey);
		}
		return null;
	}
	
	public HashMap<String, InstNode> getInstMap() {
		return this.instMap;
	}
	
	@Override
	public boolean add(InstNode inst) {
		String idxKey = StringUtil.genIdxKey(inst.getFromMethod(), 
				inst.getThreadId(), 
				inst.getThreadMethodIdx(), 
				inst.getIdx());
		return this._addInst(idxKey, inst);
	}
	
	@Override
	public boolean remove(Object o) {
		if (!(o instanceof InstNode)) {
			logger.error("Non-compatible object type: " + o.getClass());
			return false;
		}
		
		InstNode inst = (InstNode) o;
		return this._removeInst(inst);
	}
	
	/*public InstNode searchAndGet(String methodKey, long threadId, int threadMethodIdx, int idx) {
		for (InstNode inst: this) {
			if (inst.getFromMethod().equals(methodKey) && 
					inst.getThreadId() == threadId && 
					inst.getThreadMethodIdx() == threadMethodIdx && 
					inst.getIdx() == idx)
				return inst;
		}
		
		
		if (DEBUG) {
			logger.warn("Cannot find inst by method key and idx: " +  methodKey + " " + threadId + " " + threadMethodIdx + " " + idx);
		}
		return null;
	}*/
}
