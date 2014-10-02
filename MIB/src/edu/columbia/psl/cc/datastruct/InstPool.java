package edu.columbia.psl.cc.datastruct;

import java.util.HashSet;

import edu.columbia.psl.cc.pojo.InstNode;

public class InstPool extends HashSet<InstNode> {

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

}
