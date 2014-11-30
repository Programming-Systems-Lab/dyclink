package edu.columbia.psl.cc.datastruct;

import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.columbia.psl.cc.pojo.InstNode;

public class InstPool extends TreeSet<InstNode> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static Logger logger = Logger.getLogger(InstPool.class);
	
	public InstPool() {
		
	}
	
	public InstPool(InstPool contents) {
		this.addAll(contents);
		/*for (InstNode c: contents) {
			this.searchAndGet(c.getFromMethod(), c.getThreadId(), c.getThreadMethodIdx(), c.getIdx(), c.getOp().getOpcode(), c.getAddInfo());
		}*/
	}

	public InstNode searchAndGet(String methodKey, long threadId, int threadMethodIdx, int idx, int opcode, String addInfo) {
		for (InstNode inst: this) {
			if (inst.getFromMethod().equals(methodKey) && 
					inst.getThreadId() == threadId && 
					inst.getThreadMethodIdx() == threadMethodIdx && 
					inst.getIdx() == idx && 
					inst.getOp().getOpcode() == opcode && 
					inst.getAddInfo().equals(addInfo))
				return inst;
		}
		
		//Create new 
		InstNode probe = new InstNode();
		probe.setFromMethod(methodKey);
		probe.setThreadId(threadId);
		probe.setThreadMethodIdx(threadMethodIdx);
		probe.setIdx(idx);
		probe.setOp(BytecodeCategory.getOpcodeObj(opcode));
		probe.setAddInfo(addInfo);
		this.add(probe);
		return probe;
	}
	
	public InstNode searchAndGet(String methodKey, long threadId, int threadMethodIdx, int idx) {
		for (InstNode inst: this) {
			if (inst.getFromMethod().equals(methodKey) && 
					inst.getThreadId() == threadId && 
					inst.getThreadMethodIdx() == threadMethodIdx && 
					inst.getIdx() == idx)
				return inst;
		}
		
		logger.warn("Cannot find inst by method key and idx: " +  methodKey + " " + threadId + " " + threadMethodIdx + " " + idx);
		return null;
	}
}
