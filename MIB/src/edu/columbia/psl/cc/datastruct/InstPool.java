package edu.columbia.psl.cc.datastruct;

import java.util.TreeSet;

import edu.columbia.psl.cc.pojo.InstNode;

public class InstPool extends TreeSet<InstNode> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InstNode searchAndGet(int idx, int opcode, String addInfo) {
		for (InstNode inst: this) {
			if (inst.getIdx() == idx && 
					inst.getOp().getOpcode() == opcode && 
					inst.getAddInfo().equals(addInfo))
				return inst;
		}
		
		//Create new 
		InstNode probe = new InstNode();
		probe.setIdx(idx);
		probe.setOp(BytecodeCategory.getOpcodeObj(opcode));
		probe.setAddInfo(addInfo);
		this.add(probe);
		return probe;
	}
	
	public InstNode searchAndGet(int idx) {
		for (InstNode inst: this) {
			if (inst.getIdx() == idx)
				return inst;
		}
		
		System.err.println("Cannot find inst by idx: " + idx);
		return null;
	}

}
