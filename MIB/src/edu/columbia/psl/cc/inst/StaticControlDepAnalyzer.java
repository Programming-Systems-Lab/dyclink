package edu.columbia.psl.cc.inst;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.pojo.OpcodeObj;

public class StaticControlDepAnalyzer {
	
	private static Logger logger = Logger.getLogger(StaticControlDepAnalyzer.class);
	
	private String curLabel;
	
	private List<String> labels = new ArrayList<String>();
	
	private List<ControlInst> unfinalizedControlInsts = new ArrayList<ControlInst>();
	
	private List<ControlInst> finalizedControlInsts = new ArrayList<ControlInst>();
	
	public void registerLabel(Label label) {
		String labelString = label.toString();
		this.labels.add(labelString);
		this.curLabel = labelString;
		
		for (ControlInst ifInst: this.unfinalizedControlInsts) {
			if (ifInst.falseStart == null) {
				ifInst.falseStart = this.curLabel;
			}
		}
		
		Iterator<ControlInst> ifIterator = this.unfinalizedControlInsts.iterator();
		while (ifIterator.hasNext()) {
			ControlInst ifInst = ifIterator.next();
			
			if (ifInst.trueStart.equals(this.curLabel) && (ifInst.trueEnd == null)) {
				ifInst.trueEnd = this.curLabel;
			}
			
			if (ifInst.trueStart != null && ifInst.trueEnd != null 
					&& ifInst.falseStart != null && ifInst.falseEnd != null) {
				ifIterator.remove();
				this.finalizedControlInsts.add(ifInst);
			}
		}
	}
	
	private void analyzeUnfinalizedControlInsts(ControlInst gotoInst) {
		if (this.unfinalizedControlInsts.size() == 0)
			return ;
		
		String gotoLabel = gotoInst.trueCoverLabels.get(0);
		Iterator<ControlInst> ifIterator = this.unfinalizedControlInsts.iterator();
		while (ifIterator.hasNext()) {
			ControlInst ifInst = ifIterator.next();
			//if trueStart and trueEnd are the same, this means this if only has 1 branch
			String trueStart = ifInst.trueStart;
			ifInst.falseEnd = this.curLabel;
			ifInst.trueEnd = gotoLabel;
			
			if (trueStart.equals(gotoLabel)) {
				ifIterator.remove();
			}
		}
	}
	
	public void registerControlInst(int opcode, int idx, Label label) {
		OpcodeObj oo = BytecodeCategory.getOpcodeObj(opcode);
		ControlInst ci = new ControlInst();
		String controlLabel = label.toString();
		ci.opcode = opcode;
		ci.idx = idx;
		ci.inst = oo.getInstruction();
		ci.pointToLabel = controlLabel;
		ci.locatedLabel = this.curLabel;
		
		if (opcode == Opcodes.GOTO) {
			ci.trueCoverLabels.add(label.toString());
			
			if (this.unfinalizedControlInsts.size() > 0) {
				this.analyzeUnfinalizedControlInsts(ci);
			}
			this.finalizedControlInsts.add(ci);
		} else if (BytecodeCategory.controlCategory().contains(oo.getCatId())) {			
			int pos = this.labels.indexOf(controlLabel);
			if (pos >= 0) {
				//This means a loop
				for (int i = pos; i < this.labels.size(); i++) {
					ci.trueCoverLabels.add(this.labels.get(i));
				}
				
				this.finalizedControlInsts.add(ci);
			} else {
				//Need to identify the start and end label
				ci.trueStart = controlLabel;
				this.unfinalizedControlInsts.add(ci);
			}
		}
	}
	
	public void finalizeControlInsts() {
		if (this.unfinalizedControlInsts.size() > 0) {
			logger.error("Unfinalized control insts");
			for (ControlInst ci: this.unfinalizedControlInsts) {
				logger.error(ci);
				logger.error("True interval: " + ci.trueStart + " " + ci.trueEnd);
				logger.error("False interval: " + ci.falseStart + " " + ci.falseEnd); 
			}
		}
		
		for (ControlInst ci: this.finalizedControlInsts) {
			if (ci.opcode == Opcodes.GOTO)
				continue ;
			else {
				//true interval
				int tStart = this.labels.indexOf(ci.trueStart);
				int tEnd = this.labels.indexOf(ci.trueEnd);
				if (tStart >= 0 && tEnd >= 0) {
					for (int i = tStart; i <= tEnd; i++) {
						ci.trueCoverLabels.add(labels.get(i));
					}
				}
				
				int fStart = this.labels.indexOf(ci.falseStart);
				int fEnd = this.labels.indexOf(ci.falseEnd);
				
				if (fStart >= 0 && fEnd >= 0) {
					for (int i = fStart; i <= fEnd; i++) {
						ci.falseCoverLabels.add(labels.get(i));
					}
				}
			}
		}
	}
	
	public List<ControlInst> getFinalizedControlInsts() {
		return this.finalizedControlInsts;
	}
	
	public static class ControlInst {
		
		int opcode;
		
		int idx;
		
		String inst;
		
		String pointToLabel;
		
		String locatedLabel;
		
		transient String trueStart;
		
		transient String trueEnd;
		
		transient String falseStart;
		
		transient String falseEnd;
		
		List<String> trueCoverLabels = new ArrayList<String>();
		
		List<String> falseCoverLabels = new ArrayList<String>();
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("idx: " + idx + "\n");
			sb.append("inst: " + inst + " " + pointToLabel + "\n");
			sb.append("Located label: " + locatedLabel + "\n");
			
			return sb.toString();
		}
	}

}
