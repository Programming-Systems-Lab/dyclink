package edu.columbia.psl.cc.util;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import edu.columbia.psl.cc.pojo.BlockNode;
import edu.columbia.psl.cc.pojo.CondNode;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.Var;

public class RelationMiner {
	
	private List<BlockNode> blocks;
	
	private Set<String> dontMergeSet;
		
	public void setBlockNodes(List<BlockNode> blocks) {
		this.blocks = blocks;
	}
	
	public List<BlockNode> getBlockNodes() {
		return this.blocks;
	}
	
	public void setDontMergeSet(Set<String> dontMergeSet) {
		this.dontMergeSet = dontMergeSet;
	}
	
	public Set<String> getDontMergeList() {
		return this.dontMergeSet;
	}
	
	private boolean shouldMerge(BlockNode curBlock) {
		List<InstNode> insts = curBlock.getInsts();
		if (insts.size() == 0)
			return true;
		
		InstNode last = insts.get(insts.size() - 1);
		
		if (!last.getClass().equals(CondNode.class)) {
			return true;
		} else {
			return false;
		}
	}
	
	private void merge(BlockNode curBlock, BlockNode bn) {
		//Add inst from bn to curBlock
		for (InstNode inst: bn.getInsts()) {
			curBlock.addInst(inst);
		}
	}
	
	public void mergeBlockNodes() {
		BlockNode curBlock = null;
		Iterator<BlockNode> blockIT = this.blocks.iterator();
		
		while (blockIT.hasNext()) {
			BlockNode bn = blockIT.next();
			
			if (curBlock == null) {
				curBlock = bn;
			} else {
				if (this.dontMergeSet.contains(bn.getLabel())) {
					curBlock = bn;
				} else if (this.shouldMerge(curBlock)) {
					this.merge(curBlock, bn);
					blockIT.remove();
				} else {
					curBlock = bn;
				}
			} 
		}
	}
	
	private BlockNode searchBlockByLabel(String label) {
		for (BlockNode block: this.blocks) {
			if (block.getLabel().equals(label))
				return block;
		}
		System.err.println("RelationConstructor: Cannot find block: " + label);
		return null;
	}
	
	private void addChildrenToParent(InstNode parent, List<InstNode> children) {
		for (InstNode child: children) {
			this.addChildrenToParent(parent, child);
		}
	}
	
	private void addChildrenToParent(InstNode parent, InstNode child) {
		Var parentVar = parent.getVar();
		Var childVar = child.getVar();
		parentVar.addChildren(childVar);
	}
	
	private void connectLastLoadWithCurBlock(InstNode lastLoad, BlockNode block) {
		List<InstNode> insts = block.getInsts();
		for (InstNode inst: insts) {
			this.addChildrenToParent(lastLoad, inst);
		}
	}
	
	private InstNode analyzeBlock(BlockNode block) {
		List<InstNode> insts = block.getInsts();
		Stack<InstNode> replayer = new Stack<InstNode>();
		for (InstNode inst: insts) {
			if (replayer.isEmpty()) {
				replayer.push(inst);
			} else {
				InstNode stackInst = replayer.peek();
				if (inst.getClass().equals(CondNode.class)) {
					//This must be the end of a block
					CondNode conInst = (CondNode)inst;
					
					//There must be a load on stack?
					BlockNode jumpBlock = this.searchBlockByLabel(conInst.getLabel());
					this.addChildrenToParent(stackInst, jumpBlock.getInsts());
				} else if (!inst.isLoad() && stackInst.isLoad()) {
					//If the last inst is not load (so store), and the one on stack is load
					this.addChildrenToParent(stackInst, inst);
					replayer.pop();
				}
			}
		}
		
		if (replayer.size() > 0) {
			InstNode potentialRet = replayer.pop();
			if (potentialRet.isLoad()) {
				return potentialRet;
			}
		}
		
		return null;
	}
		
	public void constructRelation() {
		InstNode lastLoad = null;
		for (BlockNode block: blocks) {
			if (lastLoad != null) {
				System.out.println("Last load: " + lastLoad);
				this.connectLastLoadWithCurBlock(lastLoad, block);
			}
			lastLoad = this.analyzeBlock(block);
		}
	}
	
	private String checkDestination(InstNode inst) {
		if (!inst.getClass().equals(CondNode.class))
			return null;
		
		CondNode cn = (CondNode)inst;
		return cn.getLabel();
	}
	
	public void constructCFG() {
		BlockNode parentBlock = null;
		for (BlockNode bn: this.blocks) {
			if (parentBlock == null) {
				parentBlock = bn;
				continue;
			}
			
			//Check parent
			List<InstNode> parentInsts = parentBlock.getInsts();
			if (parentInsts.size() != 0) {
				InstNode lastInst = parentInsts.get(parentInsts.size() - 1);
				String destLabel = this.checkDestination(lastInst);
				if (destLabel != null) {
					BlockNode dest = this.searchBlockByLabel(destLabel);
					parentBlock.addChildBlock(dest);
					
					CondNode cond = (CondNode)lastInst;
					if (!cond.isGoto()) {
						parentBlock.addChildBlock(bn);
					}
				}
			} else {
				parentBlock.addChildBlock(bn);
			}
			parentBlock = bn;
		}
	}

}
