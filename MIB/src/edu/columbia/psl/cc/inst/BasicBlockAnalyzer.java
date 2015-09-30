package edu.columbia.psl.cc.inst;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;

import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.pojo.OpcodeObj;

public class BasicBlockAnalyzer {
	
	private static int labelCount = 0;
	
	private String className;
	
	private String methodName;
		
	private Map<Label, BasicBlock> blocks = new HashMap<Label, BasicBlock>();
	
	private BasicBlock curBlock = null;
	
	public synchronized int getLabelCount() {
		return labelCount++;
	}
	
	public BasicBlockAnalyzer(String className, String methodName) {
		this.className = className;
		this.methodName = methodName;
	}
	
	public void signalLabel(Label l) {
		int labelId = getLabelCount();
		BasicBlock block = new BasicBlock();
		block.setLabel(l);
		block.setLabelId(labelId);
		this.blocks.put(l, block);
		
		if (this.curBlock != null) {
			this.curBlock.addChildren(block);
		}
		
		this.curBlock = block;
	}
	
	public void signalInst(int opcode, String addInfo) {
		OpcodeObj oo = BytecodeCategory.getOpcodeObj(opcode);
		String instInfo = oo.getInstruction() + " " + addInfo;
		this.curBlock.addInst(instInfo);
	}
	
	public void signalInst(int opcode) {
		OpcodeObj oo = BytecodeCategory.getOpcodeObj(opcode);
		String instInfo = oo.getInstruction();
		this.curBlock.addInst(instInfo);
		
		if (BytecodeCategory.returnOps().contains(oo.getCatId())) {
			this.curBlock = null;
		} 
	}
	
	public void signalJump(int opcode, Label label) {
		OpcodeObj oo = BytecodeCategory.getOpcodeObj(opcode);
		String instInfo = oo.getInstruction();
		this.curBlock.addInst(instInfo);
		
		this.curBlock.addDangling(label);
		
		if (oo.getOpcode() == Opcodes.GOTO) {
			this.curBlock = null;
		}
	}
	
	public void signalTableSwitch(int opcode, Label dflt, Label...labels) {
		OpcodeObj oo = BytecodeCategory.getOpcodeObj(opcode);
		String instInfo = oo.getInstruction();
		this.curBlock.addInst(instInfo);
		
		if (dflt != null) {
			this.curBlock.addDangling(dflt);
		}
		
		if (labels.length > 0) {
			for (Label l: labels) {
				this.curBlock.addDangling(l);
			}
		}
		this.curBlock = null;
	}
	
	public void signalLookupSwitch(int opcode, Label[] labels) {
		OpcodeObj oo = BytecodeCategory.getOpcodeObj(opcode);
		String instInfo = oo.getInstruction();
		this.curBlock.addInst(instInfo);
		
		for (Label l: labels) {
			this.curBlock.addDangling(l);
		}
		
		this.curBlock = null;
	}
	
	public void summarizeDanglings() {
		for (BasicBlock bb: this.blocks.values()) {
			if (bb.getDanglings().size() > 0) {
				for (Label dl: bb.getDanglings()) {
					BasicBlock dChild = this.blocks.get(dl);
					bb.addChildren(dChild);
				}
			}
		}
	}
	
	public void printBlockInfo() {
		for (BasicBlock bb: this.blocks.values()) {
			System.out.println("Basic block: " + bb.getLabelId());
			System.out.println("Instructions: " + bb.getInsts().size());
			for (String inst: bb.getInsts()) {
				System.out.println(inst);
			}
			System.out.println("Children blocks: " + bb.getChildren().size());
			for (BasicBlock cb: bb.getChildren()) {
				System.out.println(cb.getLabelId());
			}
		}
	}
	
	public static void main(String[] args) {
		System.out.println("Please input class file path: ");
		Scanner s = new Scanner(System.in);
		String path = s.nextLine();
		System.out.println("Confirm class file path: " + path);
		int startIdx = path.indexOf("edu/");
		int endIdx = path.indexOf(".class");
		String name = path.substring(startIdx, endIdx);
		System.out.println("Confirm class name: " + name);;
		
		File classFile = new File(path);
		if (!classFile.exists()) {
			System.err.println("Invalid class file path: " + classFile.getAbsolutePath());
			System.exit(-1);
		}
		
		try {
			byte[] classBuff = Files.readAllBytes(classFile.toPath());
			ClassReader cr = new ClassReader(classBuff);
			ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
			ClassMiner cm = new ClassMiner(new CheckClassAdapter(cw, false), name, null, null, null);
			cm.setAnnotGuard(false);
			cr.accept(cm, ClassReader.EXPAND_FRAMES);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static class BasicBlock {
		
		private HashSet<Label> danglings = new HashSet<Label>();
				
		private Label label;
		
		private int labelId;
		
		private List<String> insts = new ArrayList<String>();
		
		private List<BasicBlock> children = new ArrayList<BasicBlock>();
				
		public void setLabel(Label label) {
			this.label = label;
		}
		
		public Label getLabel() {
			return this.label;
		}
		
		public void setLabelId(int labelId) {
			this.labelId = labelId;
		}
		
		public int getLabelId() {
			return this.labelId;
		}
		
		public void addInst(String inst) {
			this.insts.add(inst);
		}
		
		public void setInsts(List<String> insts) {
			this.insts = insts;
		}
		
		public List<String> getInsts() {
			return this.insts;
		}
		
		public void addChildren(BasicBlock child) {
			this.children.add(child);
		}
		
		public void setChildren(List<BasicBlock> children) {
			this.children = children;
		}
		
		public List<BasicBlock> getChildren() {
			return this.children;
		}
		
		public void addDangling(Label l) {
			this.danglings.add(l);
		}
		
		public void setDanglings(HashSet<Label> danglings) {
			this.danglings = danglings;
		}
		
		public HashSet<Label> getDanglings() {
			return this.danglings;
		}
	}

}
