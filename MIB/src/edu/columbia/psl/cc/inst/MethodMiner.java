package edu.columbia.psl.cc.inst;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.columbia.psl.cc.datastruct.BCTreeNodePool;
import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.datastruct.VarPool;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.BlockNode;
import edu.columbia.psl.cc.pojo.CodeTemplate;
import edu.columbia.psl.cc.pojo.CondNode;
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
	
	private List<Var> nonterminateVar = new ArrayList<Var>();
	
	private VarPool varPool = new VarPool();
	
	private List<BlockNode> cfg = new ArrayList<BlockNode>();
	
	private BlockNode curBlock;
	
	private boolean needNewBlock = false;
	
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
	
	public synchronized void needNewBlock(boolean needNewBlock) {
		this.needNewBlock = needNewBlock;
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
	
	private void genNewBlockNode(String label) {
		BlockNode block = new BlockNode();
		block.setLabel(label);
		this.cfg.add(block);
		this.curBlock = block;
	}
	
	private BlockNode getCurrentBlockNode() {
		return this.curBlock;
	}
	
	private void handleDataSource(Var var) {
		this.nonterminateVar.add(var);
	}
	
	private void summarizeDataSource() {
		if (this.nonterminateVar.size() == 0)
			return ;
		
		InstNode sourceNode = new InstNode();
		sourceNode.setLoad(true);
		for (Var sVar: this.nonterminateVar) {
			sourceNode.addVar(sVar);
		}
		this.nonterminateVar.clear();
		this.getCurrentBlockNode().addInst(sourceNode);
	}
		
	private void handleDataSink(Var var, String label, int...opcode) {
		this.summarizeDataSource();
		
		if (var != null) {
			InstNode sinkNode = new InstNode();
			sinkNode.addVar(var);
			sinkNode.setLoad(false);
			this.getCurrentBlockNode().addInst(sinkNode);
		} else {
			CondNode condNode = new CondNode();
			condNode.setOpcode(opcode[0]);
			condNode.setLabel(label);
			this.getCurrentBlockNode().addInst(condNode);
		}
	}
	
	private void checkAndGenBlock() {
		if (this.needNewBlock) {
			this.genNewBlockNode(null);
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
	public void visitLabel(Label label) {
		this.summarizeDataSource();
		this.genNewBlockNode(label.toString());
		this.needNewBlock(false);
		this.mv.visitLabel(label);
	}
	
	@Override
	public void visitLineNumber(int line, Label label) {
		//System.out.println("Check line: " + line + " " + label.toString());
		this.mv.visitLineNumber(line, label);
	}
	
	@Override
	public void visitInsn(int opcode) {
		this.checkAndGenBlock();
		this.updateMethodRep(opcode);
		this.mv.visitInsn(opcode);
	}
	
	@Override
	public void visitIntInsn(int opcode, int operand) {
		this.checkAndGenBlock();
		this.updateMethodRep(opcode);
		this.mv.visitIntInsn(opcode, operand);
	}
	
	@Override
	public void visitVarInsn(int opcode, int var) {
		this.checkAndGenBlock();
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
			this.handleDataSink(v, null);
		} else {
			System.err.println("Weird var opcode: " + opcode);
		}
		this.mv.visitVarInsn(opcode, var);
	}
	
	@Override
	public void visitTypeInsn(int opcode, String type) {
		this.checkAndGenBlock();
		this.updateMethodRep(opcode);
		this.mv.visitTypeInsn(opcode, type);
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		this.checkAndGenBlock();
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
			this.handleDataSink(dataSink, null);
		}
		this.mv.visitFieldInsn(opcode, owner, name, desc);
	}
	
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		this.checkAndGenBlock();
		this.updateMethodRep(opcode);
		this.mv.visitMethodInsn(opcode, owner, name, desc);
	}
	
	@Override
	public void visitJumpInsn(int opcode, Label label) {
		//System.out.println("Jump: " + opcode + " " + label);
		this.checkAndGenBlock();
		this.updateMethodRep(opcode);
		this.handleDataSink(null, label.toString(), opcode);
		this.needNewBlock(true);
		this.mv.visitJumpInsn(opcode, label);
	}
	
	@Override
	public void visitLdcInsn(Object cst) {
		this.checkAndGenBlock();
		this.updateSingleCat(0, Opcodes.LDC);
		this.mv.visitLdcInsn(cst);
	}
	
	@Override
	public void visitIincInsn(int var, int increment) {
		this.checkAndGenBlock();
		this.updateSingleCat(6, Opcodes.IINC);
		this.mv.visitIincInsn(var, increment);
	}
	
	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label...labels) {
		//In jump set
		this.checkAndGenBlock();
		this.updateSingleCat(15, Opcodes.TABLESWITCH);
		this.mv.visitTableSwitchInsn(min, max, dflt, labels);
	}
	
	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		//In jump set
		this.checkAndGenBlock();
		this.updateSingleCat(15, Opcodes.LOOKUPSWITCH);
		this.mv.visitLookupSwitchInsn(dflt, keys, labels);
	}
	
	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		//In object set
		this.checkAndGenBlock();
		this.updateSingleCat(13, Opcodes.MULTIANEWARRAY);
		this.mv.visitMultiANewArrayInsn(desc, dims);
	}
	
	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		this.checkAndGenBlock();
		this.mv.visitTryCatchBlock(start, end, handler, type);
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
			
			System.out.println("Check blocks");
			for (BlockNode node: this.cfg) {
				System.out.println(node);
			}
			
			/*CodeTemplate ct = new CodeTemplate();
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
			}*/
		}
		
		this.mv.visitEnd();
	}
}
