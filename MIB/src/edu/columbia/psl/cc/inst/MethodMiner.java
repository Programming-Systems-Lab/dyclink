package edu.columbia.psl.cc.inst;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.columbia.psl.cc.pojo.BytecodeCategory;
import edu.columbia.psl.cc.pojo.CodeTemplate;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.util.GsonManager;

public class MethodMiner extends MethodVisitor{
	
	private String owner;
	
	private String templateAnnot;
	
	private String testAnnot;
	
	private boolean isTemplate = false;
	
	private boolean isTest = false;
	
	private int[] repVector = new int[BytecodeCategory.getOpcodeCategory().size()];
	
	private HashMap<Integer, ArrayList<OpcodeObj>> records = genRecordTemplate();
	
	private ArrayList<OpcodeObj> sequence = new ArrayList<OpcodeObj>();
	
	private ClassMiner parent = null;
	
	private String key;
	
	public MethodMiner(MethodVisitor mv, String owner, String templateAnnot, String testAnnot, ClassMiner parent, String key) {
		super(Opcodes.ASM4, mv);
		this.owner = owner;
		this.templateAnnot = templateAnnot;
		this.testAnnot = testAnnot;
		this.parent = parent;
		this.key = key;
	}
	
	private static HashMap<Integer, ArrayList<OpcodeObj>> genRecordTemplate() {
		HashMap<Integer, ArrayList<OpcodeObj>> template = new HashMap<Integer, ArrayList<OpcodeObj>>();
		//Now we have 12 categories
		for (int i = 0; i < BytecodeCategory.getOpcodeCategory().size(); i++) {
			ArrayList<OpcodeObj> opjList = new ArrayList<OpcodeObj>();
			template.put(i, opjList);
		}
		return template;
	}
	
	private void recordOps(int category, int opcode) {
		OpcodeObj obj = BytecodeCategory.getOpcodeObj(opcode);
		this.records.get(category).add(obj);
		this.sequence.add(obj);
	}
	
	private void updateSingleCat(int catId, int opcode) {
		if (this.isTemplate || this.isTest) {
			repVector[catId]++;
			recordOps(catId, opcode);
		}
	}
	
	private void updateMethodRep(int opcode) {
		boolean found = false;
		for (Integer catId: BytecodeCategory.getOpcodeCategory().keySet()) {
			if (BytecodeCategory.getOpcodeSetByCat(catId).contains(opcode)) {
				updateSingleCat(catId, opcode);
				found = true;
				break ;
			}
		}
		
		if (!found) {
			System.err.println("Cannot find category for: " + opcode);
		}
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if (desc.equals(this.templateAnnot)) {
			this.isTemplate = true;
			System.out.println("Template annotated: " + desc);
		} else if (desc.equals(this.testAnnot)) {
			this.isTest = true;
			System.out.println("Test annotated: " + desc);
		}
		return this.mv.visitAnnotation(desc, visible);
	}
	
	@Override
	public void visitInsn(int opcode) {
		this.updateMethodRep(opcode);
		this.mv.visitInsn(opcode);
	}
	
	@Override
	public void visitIntInsn(int opcode, int operand) {
		this.updateMethodRep(opcode);
		this.mv.visitIntInsn(opcode, operand);
	}
	
	@Override
	public void visitVarInsn(int opcode, int var) {
		this.updateMethodRep(opcode);
		this.mv.visitVarInsn(opcode, var);
	}
	
	@Override
	public void visitTypeInsn(int opcode, String type) {
		this.updateMethodRep(opcode);
		this.mv.visitTypeInsn(opcode, type);
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		this.updateMethodRep(opcode);
		this.mv.visitFieldInsn(opcode, owner, name, desc);
	}
	
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		this.updateMethodRep(opcode);
		this.mv.visitMethodInsn(opcode, owner, name, desc);
	}
	
	@Override
	public void visitJumpInsn(int opcode, Label label) {
		this.updateMethodRep(opcode);
		this.mv.visitJumpInsn(opcode, label);
	}
	
	@Override
	public void visitLdcInsn(Object cst) {
		this.updateSingleCat(0, Opcodes.LDC);
		this.mv.visitLdcInsn(cst);
	}
	
	@Override
	public void visitIincInsn(int var, int increment) {
		this.updateSingleCat(6, Opcodes.IINC);
		this.mv.visitIincInsn(var, increment);
	}
	
	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label...labels) {
		//In jump set
		this.updateSingleCat(15, Opcodes.TABLESWITCH);
		this.mv.visitTableSwitchInsn(min, max, dflt, labels);
	}
	
	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		//In jump set
		this.updateSingleCat(15, Opcodes.LOOKUPSWITCH);
		this.mv.visitLookupSwitchInsn(dflt, keys, labels);
	}
	
	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		//In object set
		this.updateSingleCat(13, Opcodes.MULTIANEWARRAY);
		this.mv.visitMultiANewArrayInsn(desc, dims);
	}
	
	@Override
	public void visitEnd() {
		if (this.isTemplate || this.isTest) {
			//this.parent.updateVectorRecord(this.key, this.repVector, this.records, this.sequence);
			StringBuilder sb = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			for (OpcodeObj oo: this.sequence) {
				sb.append(oo.getCatId() + ",");
				sb2.append((char)(oo.getCatId() + 97));
			}
			CodeTemplate ct = new CodeTemplate();
			ct.setCatSequence(sb.substring(0, sb.length() - 1));
			ct.setCharSequence(sb2.toString());
			
			if (isTemplate) {
				GsonManager.writeJson(ct, this.key, true);
			} else if (isTest) {
				GsonManager.writeJson(ct, this.key, false);
			}
		}
		
		this.mv.visitEnd();
	}
}
