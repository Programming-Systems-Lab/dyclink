package edu.columbia.psl.cc.inst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
	
	public void setCurLabel(Label label) {
		String labelString = label.toString();
		if (this.labelList.size() == 0) {
			this.blockStarts.add(labelString);
		}
		
		LabelBlock lb = new LabelBlock();
		lb.label = labelString;
		
		this.curLb = lb;
		labelList.add(lb);
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
				String pString = p.toString();
				blockStarts.add(pString);
				it.pointTos.add(pString);
			}
		}
		
		this.curLb.instList.add(it);
	}
	
	public void analyzeBlocks() {
		Block curBlock = null;
		for (LabelBlock lb: this.labelList) {
			if (this.blockStarts.contains(lb.label)) {				
				Block lastBlock = curBlock;
				curBlock = new Block();
				curBlock.startLabel = lb.label;
				this.blockList.add(curBlock);
				
				if (lastBlock != null) {
					//Construct relations
					lastBlock.childBlocks.add(curBlock.startLabel);
					InstTuple lastInst = lastBlock.instList.get(lastBlock.instList.size() - 1);
					
					if (lastInst.pointTos.size() > 0) {
						lastBlock.childBlocks.addAll(lastInst.pointTos);
					}
				}
			}
			
			curBlock.labels.add(lb.label);
			curBlock.instList.addAll(lb.instList);
		}
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
	}

}
