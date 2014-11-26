package edu.columbia.psl.cc.inst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.pojo.OpcodeObj;

public class BlockAnalyzer {
	
	private static Logger logger = Logger.getLogger(BlockAnalyzer.class);
	
	private List<LabelBlock> labelList = new ArrayList<LabelBlock>();
	
	private List<Block> blockList = new ArrayList<Block>();
	
	private Set<String> blockStarts = new HashSet<String>();
	
	private LabelBlock curLb;
	
	public static boolean unionTags(boolean[] parentTags, boolean[] childTags) {
		int trueCount = 0;
		for (int i = 0; i < childTags.length; i++) {
			boolean curVal = parentTags[i] | childTags[i];
			childTags[i] = curVal;
			
			if (curVal)
				trueCount++;
		}
		
		if (trueCount == childTags.length)
			return true;
		else
			return false;
	}
	
	public static void unionCondMap(Map<String, boolean[]>parentMap, Map<String, boolean[]>childMap) {
		for (String pKey: parentMap.keySet()) {
			boolean[] parentTags = parentMap.get(pKey);
			if (childMap.containsKey(pKey)) {
				boolean[] childTags = childMap.get(pKey);
				
				boolean shouldRemoveTag = unionTags(parentTags, childTags);
				if (shouldRemoveTag) {
					childMap.remove(pKey);
				}
			} else {
				boolean[] copyTags = new boolean[parentTags.length];
				System.arraycopy(parentTags, 0, copyTags, 0, parentTags.length);
				childMap.put(pKey, copyTags);
			}
		}
	}
	
	public void setCurLabel(Label label) {
		String labelString = label.toString();
		if (this.labelList.size() == 0) {
			//First label shoud be a start of block
			this.blockStarts.add(labelString);
		}
		
		LabelBlock lb = new LabelBlock();
		lb.label = labelString;
		
		LabelBlock lastLb = null;
		if (this.curLb != null) {
			lastLb = this.curLb;
		}
		
		this.curLb = lb;
		this.labelList.add(lb);
		
		if (lastLb != null) {
			if (lastLb.instList.size() == 0)
				return ;
			
			InstTuple lastInst = lastLb.instList.get(lastLb.instList.size() - 1);
			
			OpcodeObj oo = BytecodeCategory.getOpcodeObj(lastInst.opcode);
			if (BytecodeCategory.controlCategory().contains(oo.getCatId()) 
					|| lastInst.opcode == Opcodes.TABLESWITCH 
					|| lastInst.opcode == Opcodes.LOOKUPSWITCH) {
				//If the end of last lable is control, this label should be the block start
				this.blockStarts.add(labelString);
			}
		}
	}
	
	public void registerInst(int instIdx, int opcode, Label...pointToLabels) {
		InstTuple it = new InstTuple();
		it.instIdx = instIdx;
		it.opcode = opcode;
		
		OpcodeObj oo = BytecodeCategory.getOpcodeObj(opcode);
		if (BytecodeCategory.controlCategory().contains(oo.getCatId()) 
				|| opcode == Opcodes.TABLESWITCH 
				|| opcode == Opcodes.LOOKUPSWITCH) {
			for (Label p: pointToLabels) {
				//All labels pointed by a control inst should be the start of block
				String pString = p.toString();
				this.blockStarts.add(pString);
				it.pointTos.add(pString);
			}
		}
		
		this.curLb.instList.add(it);
	}
	
	public void analyzeBlocks() {
		Block curBlock = null;
		logger.info("Block starts: " + this.blockStarts);
		for (LabelBlock lb: this.labelList) {
			if (this.blockStarts.contains(lb.label)) {				
				Block lastBlock = curBlock;
				curBlock = new Block();
				curBlock.startLabel = lb.label;
				this.blockList.add(curBlock);
				
				if (lastBlock != null) {
					if (lastBlock.instList.size() == 0) {
						logger.warn("Empty block: " + lastBlock.startLabel);
					} else {
						lastBlock.lastInst = lastBlock.instList.get(lastBlock.instList.size() - 1);
						
						if (lastBlock.lastInst.opcode != Opcodes.GOTO 
								&& lastBlock.lastInst.opcode != Opcodes.TABLESWITCH 
								&& lastBlock.lastInst.opcode != Opcodes.LOOKUPSWITCH) {
							lastBlock.childBlocks.add(curBlock.startLabel);
						}
						
						if (lastBlock.lastInst.pointTos.size() > 0) {
							lastBlock.childBlocks.addAll(lastBlock.lastInst.pointTos);
						}
					}
					
				}
			}
			
			curBlock.labels.add(lb.label);
			if (lb.instList.size() > 0) {
				curBlock.instList.addAll(lb.instList);
			}
		}
		
		HashMap<String, Block> blockCache = new HashMap<String, Block>();
		for (Block b: this.blockList) {
			boolean propagate = b.childBlocks.size() > 1?true: false;
			
			int count = 0;
			for (String cLabel: b.childBlocks) {
				Block childBlock = null;
				if (blockCache.containsKey(cLabel)) {
					childBlock = blockCache.get(cLabel);
				} else {
					childBlock = this.searchBlock(cLabel);
					blockCache.put(cLabel, childBlock);
				}
				
				//Propagate new tags from parent
				if (propagate) {
					boolean[] tags = new boolean[b.childBlocks.size()];
					tags[count++] = true;
					childBlock.condMap.put(b.startLabel, tags);
				}
			}
		}
		
		for (Block b: this.blockList) {
			for (String cLabel: b.childBlocks) {
				Block childBlock = blockCache.get(cLabel);
				
				//Merge children tag with parents
				//logger.info("Parent label: " + b.startLabel + " " + b.condMap.size());
				//logger.info("Child label: " + childBlock.startLabel);
				if (b.condMap.size() > 0) {
					//Prevent concurrent modification error
					if (!b.startLabel.equals(childBlock.startLabel))
						unionCondMap(b.condMap, childBlock.condMap);
				}
			}
		}
	}
	
	public Block searchBlock(String label) {
		for (Block b: this.blockList) {
			if (b.startLabel.equals(label)) {
				return b;
			}
		}
		//Not possible
		return null;
	}
	
	public List<Block> getBlockList() {
		return this.blockList;
	}
	
	public static class InstTuple {
		public int instIdx;
		
		public int opcode;
		
		public List<String> pointTos = new ArrayList<String>();
		
	}
	
	public static class LabelBlock {
		public String label;
		
		public List<InstTuple> instList = new ArrayList<InstTuple>();
	}
	
	public static class Block {
				
		public String startLabel;
		
		public Set<String> labels = new HashSet<String>();
		
		public List<InstTuple> instList = new ArrayList<InstTuple>();
		
		public Set<String> childBlocks = new HashSet<String>();
		
		public Map<String, boolean[]> condMap = new HashMap<String, boolean[]>();
		
		public InstTuple lastInst;
	}

}
