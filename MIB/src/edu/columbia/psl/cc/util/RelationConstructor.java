package edu.columbia.psl.cc.util;

import java.util.List;
import java.util.Stack;

import edu.columbia.psl.cc.pojo.BlockNode;
import edu.columbia.psl.cc.pojo.CondNode;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.Var;

public class RelationConstructor {
	
	private List<BlockNode> blocks;
	
	public void setBlockNodes(List<BlockNode> blocks) {
		this.blocks = blocks;
	}
	
	public List<BlockNode> getBlockNodes() {
		return this.blocks;
	}
	
	public BlockNode searchBlockByLabel(String label) {
		for (BlockNode block: this.blocks) {
			if (block.getLabel().equals(label))
				return block;
		}
		System.err.println("RelationConstructor: Cannot find block: " + label);
		return null;
	}
	
	public void addChildrenToParent(InstNode parent, List<InstNode> children) {
		for (InstNode child: children) {
			this.addChildrenToParent(parent, child);
		}
	}
	
	public void addChildrenToParent(InstNode parent, InstNode child) {
		List<Var> parentVars = parent.getVars();
		List<Var> childVars = child.getVars();
		for (Var v: parentVars) {
			for (Var cv: childVars) {
				v.addChildren(cv);
			}
		}
	}
	
	public InstNode analyzeBlock(BlockNode block) {
		List<InstNode> insts = block.getInsts();
		Stack<InstNode> replayer = new Stack<InstNode>();
		InstNode ret;
		for (InstNode inst: insts) {
			if (replayer.isEmpty()) {
				replayer.push(inst);
			} else {
				InstNode stackInst = replayer.pop();
				
				if (inst.getClass().equals(CondNode.class)) {
					CondNode conInst = (CondNode)inst;
					
					//There must be a load on stack?
					BlockNode jumpBlock = this.searchBlockByLabel(conInst.getLabel());
					this.addChildrenToParent(stackInst, jumpBlock.getInsts());
				} else if (!inst.isLoad() && stackInst.isLoad()) {
					this.addChildrenToParent(stackInst, inst);
				}
			}
		}
		return replayer.pop();
	}
		
	public void constructRelation() {
		InstNode curInst;
		
		for (BlockNode block: blocks) {
			InstNode lastLoad = this.analyzeBlock(block);
		}
	}

}
