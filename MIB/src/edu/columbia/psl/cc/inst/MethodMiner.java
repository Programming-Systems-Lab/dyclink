package edu.columbia.psl.cc.inst;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.datastruct.VarPool;
import edu.columbia.psl.cc.pojo.CodeTemplate;
import edu.columbia.psl.cc.pojo.Dependency;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.pojo.Var;
import edu.columbia.psl.cc.util.GsonManager;
import edu.columbia.psl.cc.util.StringUtil;

public class MethodMiner extends MethodVisitor{
	
	private String owner;
	
	private String templateAnnot;
	
	private String testAnnot;
	
	private boolean isTemplate = false;
	
	private boolean isTest = false;
	
	private int[] repVector = new int[BytecodeCategory.getOpcodeCategory().size()];
	
	private HashMap<Integer, ArrayList<OpcodeObj>> records = genRecordTemplate();
	
	private ArrayList<OpcodeObj> sequence = new ArrayList<OpcodeObj>();
	
	private String myName;
	
	private String myDesc;
	
	private ArrayList<Var> nonterminateVar = new ArrayList<Var>();
	
	private VarPool varPool = new VarPool();
	
	public MethodMiner(MethodVisitor mv, String owner, String templateAnnot, String testAnnot, String myName, String myDesc) {
		super(Opcodes.ASM4, mv);
		this.owner = owner;
		this.templateAnnot = templateAnnot;
		this.testAnnot = testAnnot;
		this.myName = myName;
		this.myDesc = myDesc;
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
	
	private int updateMethodRep(int opcode) {
		int catId = BytecodeCategory.getSetIdByOpcode(opcode);
		if (catId >= 0 ) {
			updateSingleCat(catId, opcode);
		} else {
			System.err.println("Cannot find category for: " + opcode);
		}
		return catId;
	}
	
	private void handleDataSource(Var var) {
		this.nonterminateVar.add(var);
	}
	
	private void handleDataSink(Var var) {
		for (Var parent: this.nonterminateVar) {
			parent.addChildren(var);
		}
		this.nonterminateVar.clear();
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
		int catId = this.updateMethodRep(opcode);
		
		if (catId < 0) {
			System.err.println("Invalid var opcode: " + opcode);
			return ;
		}
		//Local variable
		int silId = 2;
		Var v = this.varPool.searchVar(this.owner, this.myName, silId, String.valueOf(var));
		if (catId == 1) {
			this.handleDataSource(v);
		} else if (catId == 2) {
			this.handleDataSink(v);
		} else {
			System.err.println("Weird var opcode: " + opcode);
		}
		this.mv.visitVarInsn(opcode, var);
	}
	
	@Override
	public void visitTypeInsn(int opcode, String type) {
		this.updateMethodRep(opcode);
		this.mv.visitTypeInsn(opcode, type);
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		int catId = this.updateMethodRep(opcode);
		
		if (catId < 0) {
			System.err.println("Invalid field opcode: " + opcode);
			return ;
		}
		
		if (catId == 10) {
			int silId = (opcode == 178)?0: 1;
			Var dataSource = this.varPool.searchVar(this.owner, this.myName, silId, name + ":" + desc);
			this.handleDataSource(dataSource);
		} else {
			int silId = (opcode == 179)?0: 1;
			Var dataSink = this.varPool.searchVar(this.owner, this.myName, silId, name + ":" + desc);
			this.handleDataSink(dataSink);
		}
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
			ct.setVars(varPool);
			
			String key = StringUtil.cleanPunc(this.owner, "_") 
					+ "~" + StringUtil.cleanPunc(this.myName, "_") 
					+ "~" + StringUtil.parseDesc(this.myDesc);
			System.out.println("Check key: " + key);
			if (isTemplate) {
				GsonManager.writeJson(ct, key, true);
			} else if (isTest) {
				GsonManager.writeJson(ct, key, false);
			}
			
			System.out.println("Check var size: " + this.varPool.size());
			for (Var v: this.varPool) {
				if (v.getChildren().size() > 0) {
					System.out.print("Source: " + v + "->");
				}
				
				for (String edge: v.getChildren().keySet()) {
					System.out.println(edge);
					Set<Var> edgeChildren = v.getChildren().get(edge);
					for (Var ev: edgeChildren) {
						System.out.println("->" + "Sink: " +  ev);
					}
				}
			}
		}
		
		this.mv.visitEnd();
	}
}
