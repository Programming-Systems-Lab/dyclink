package edu.columbia.psl.cc.inst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.datastruct.VarPool;
import edu.columbia.psl.cc.pojo.BlockNode;
import edu.columbia.psl.cc.pojo.CondNode;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.LabelInterval;
import edu.columbia.psl.cc.pojo.LocalVar;
import edu.columbia.psl.cc.pojo.MultiNewArrayNode;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.pojo.StaticRep;
import edu.columbia.psl.cc.pojo.SwitchNode;
import edu.columbia.psl.cc.pojo.Var;
import edu.columbia.psl.cc.util.GsonManager;
import edu.columbia.psl.cc.util.MethodStackRecorder;
import edu.columbia.psl.cc.util.ObjectIdAllocater;
import edu.columbia.psl.cc.util.StringUtil;

public class DynamicMethodMiner extends MethodVisitor {
	
	private static String methodStackRecorder = Type.getInternalName(MethodStackRecorder.class);
	
	private static String srHandleCommon = MIBConfiguration.getSrHandleCommon();
	
	private static String srHCDesc = MIBConfiguration.getSrHCDesc();
	
	private static String srHCDescString = MIBConfiguration.getSrHCDescString();
	
	private static String srHandleLdc = MIBConfiguration.getSrHandleLdc();
	
	private static String srHandleLdcDesc = MIBConfiguration.getSrHandleLdcDesc();
	
	private static String srHandleField = MIBConfiguration.getSrHandleField();
	
	private static String srHandleFieldDesc = MIBConfiguration.getSrHandleFieldDesc();
	
	private static String srHandleMultiArray = MIBConfiguration.getSrHandleMultiArray();
	
	private static String srHandleMultiArrayDesc = MIBConfiguration.getSrHandleMultiArrayDesc();
	
	private static String srHandleMethod = MIBConfiguration.getSrHandleMethod();
	
	private static String srHandleMethodDesc = MIBConfiguration.getSrHandleMethodDesc();
	
	private static String srGraphDump = MIBConfiguration.getSrGraphDump();
	
	private static String srGraphDumpDesc = MIBConfiguration.getSrGraphDumpDesc();
	
	private String className;
	
	private String myName;
	
	private String desc;
	
	private String templateAnnot;
	
	private String testAnnot;
	
	private boolean isStatic;
	
	private boolean isTemplate = false;
	
	private boolean isTest = false;
	
	private boolean annotGuard;
	
	private LocalVariablesSorter lvs;
	//private AdviceAdapter lvs;
	
	private int localMsrId = -1;
	
	private Label curLabel = null;
	
	private int curLineNum = -1;
	
	private List<Label> allLabels = new ArrayList<Label>();
	
	private int[] repVector = new int[BytecodeCategory.getOpcodeCategory().size()];
	
	private HashMap<Integer, ArrayList<OpcodeObj>> records = genRecordTemplate();
	
	private ArrayList<OpcodeObj> sequence = new ArrayList<OpcodeObj>();
	
	private AtomicInteger indexer = new AtomicInteger();
	
	private ArrayList<String> recentLoads = new ArrayList<String>();
	
	private boolean constructor = false;
	
	private boolean superVisited = false;
	 
	public DynamicMethodMiner(MethodVisitor mv, 
			String className, 
			int access, 
			String myName, 
			String desc, 
			String templateAnnot, 
			String testAnnot, 
			boolean annotGuard) {
		//super(Opcodes.ASM4, mv, access, myName, desc);
		super(Opcodes.ASM4, mv);
		this.className = className;
		this.myName = myName;
		this.desc = desc;
		this.templateAnnot = templateAnnot;
		this.testAnnot = testAnnot;
		this.annotGuard = annotGuard;
		this.isStatic = ((access & Opcodes.ACC_STATIC) != 0);
		this.constructor = myName.equals("<init>");
	}
	
	public synchronized int getIndex() {
		return indexer.getAndIncrement();
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
	
	public void setLocalVariablesSorter(LocalVariablesSorter lvs) {
		this.lvs = lvs;
	}
	
	public LocalVariablesSorter getLocalVariablesSorter() {
		return this.lvs;
	}
	
	public boolean shouldInstrument() {
		return !this.annotGuard || this.isTemplate || this.isTest;
	}
	
	private boolean isReturn(int opcode) {
		if (opcode >= 172 && opcode <= 177)
			return true;
		else 
			return false;
	}
	
	private void recordOps(int category, int opcode) {
		OpcodeObj obj = BytecodeCategory.getOpcodeObj(opcode);
		this.records.get(category).add(obj);
		this.sequence.add(obj);
	}
	
	private void updateSingleCat(int catId, int opcode) {
		repVector[catId]++;
		recordOps(catId, opcode);
	}
	
	private void updateObjOnVStack() {
		//Store it in MethodStackRecorder
		this.mv.visitInsn(Opcodes.DUP);
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.mv.visitInsn(Opcodes.SWAP);
		this.mv.visitInsn(Opcodes.ICONST_0);
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
				methodStackRecorder, 
				MIBConfiguration.getObjOnStack(), 
				MIBConfiguration.getObjOnStackDesc());
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
	
	public void convertConst(int cons) {
		if (cons >= 0) {
			if (cons > 5 && cons < 128) {
				this.mv.visitIntInsn(Opcodes.BIPUSH, cons);
			} else if (cons >=128 && cons < 32768) {
				this.mv.visitIntInsn(Opcodes.SIPUSH, cons);
			} else if (cons >= 32768) {
				this.mv.visitLdcInsn(cons);
			}else if (cons == 0) {
				this.mv.visitInsn(Opcodes.ICONST_0);
			} else if (cons == 1) {
				this.mv.visitInsn(Opcodes.ICONST_1);
			} else if (cons == 2) {
				this.mv.visitInsn(Opcodes.ICONST_2);
			} else if (cons == 3) {
				this.mv.visitInsn(Opcodes.ICONST_3);
			} else if (cons == 4) {
				this.mv.visitInsn(Opcodes.ICONST_4);
			} else if (cons == 5) {
				this.mv.visitInsn(Opcodes.ICONST_5);
			}
		} else {
			if (cons == -1) {
				this.mv.visitInsn(Opcodes.ICONST_M1);
			} else if (cons <= -2 && cons > -129) {
				this.mv.visitIntInsn(Opcodes.BIPUSH, cons);
			} else if (cons <= -129 && cons > -32769) {
				this.mv.visitIntInsn(Opcodes.SIPUSH, cons);
			} else {
				this.mv.visitLdcInsn(cons);
			}
		}
		
	}
	
	private void handlLabel(Label label) {
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.mv.visitLdcInsn(label.toString());
		this.mv.visitFieldInsn(Opcodes.PUTFIELD, methodStackRecorder, "curLabel", Type.getDescriptor(String.class));
	}
		
	private void handleOpcode(int opcode, int...addInfo) {		
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.convertConst(opcode);
		this.convertConst(this.getIndex());
		if (addInfo.length == 0) {
			this.mv.visitInsn(Opcodes.ICONST_M1);
		} else {
			//this.mv.visitInsn(addInfo[0]);
			this.convertConst(addInfo[0]);
		}
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, methodStackRecorder, srHandleCommon, srHCDesc);
	}
		
	private void handleOpcode(int opcode, String addInfo) {		
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.convertConst(opcode);
		this.convertConst(this.getIndex());
		this.mv.visitLdcInsn(addInfo);
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, methodStackRecorder, srHandleCommon, srHCDescString);
	}
	
	private void handleField(int opcode, String owner, String name, String desc) {
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.convertConst(opcode);
		this.convertConst(this.getIndex());
		this.mv.visitLdcInsn(owner);
		this.mv.visitLdcInsn(name);
		this.mv.visitLdcInsn(desc);
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, methodStackRecorder, srHandleField, srHandleFieldDesc);
	}
		
	private void handleLdc(int opcode, int times, String addInfo) {		
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.convertConst(opcode);
		this.convertConst(this.getIndex());
		this.convertConst(times);
		this.mv.visitLdcInsn(addInfo);
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, methodStackRecorder, srHandleLdc, srHandleLdcDesc);
	}
	
	private void handleMultiNewArray(String desc, int dim) {		
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.mv.visitLdcInsn(desc);
		this.convertConst(dim);
		this.convertConst(this.getIndex());
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, methodStackRecorder, srHandleMultiArray, srHandleMultiArrayDesc);
	}
	
	public void handleMethod(int opcode, String owner, String name, String desc) {		
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.convertConst(opcode);
		this.convertConst(this.getIndex());
		this.convertConst(this.curLineNum);
		this.mv.visitLdcInsn(owner);
		this.mv.visitLdcInsn(name);
		this.mv.visitLdcInsn(desc);
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, methodStackRecorder, srHandleMethod, srHandleMethodDesc);
	}
	
	public void initConstructor() {
		System.out.println("Initialize constructor: " + this.myName + " " + this.shouldInstrument());
		/*this.mv.visitVarInsn(Opcodes.ALOAD, 0);
		//this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, this.className, MIBConfiguration.getMIBIDGenMethod(), "()I");
		this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ObjectIdAllocater.class), "getIndex", "()I");
		this.mv.visitFieldInsn(Opcodes.PUTFIELD, this.className, MIBConfiguration.getMIBID(), "I");*/
		
		if (this.shouldInstrument()) {
			//Create the method stack recorder
			this.localMsrId = this.lvs.newLocal(Type.getType(MethodStackRecorder.class));
			System.out.println("Method Stack Recorder name: " + methodStackRecorder);
			this.mv.visitTypeInsn(Opcodes.NEW, methodStackRecorder);
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitLdcInsn(this.className);
			this.mv.visitLdcInsn(this.myName);
			this.mv.visitLdcInsn(this.desc);
			
			if (this.isStatic) {
				//this.mv.visitInsn(Opcodes.ICONST_1);
				this.mv.visitInsn(Opcodes.ACONST_NULL);
			} else {
				//this.mv.visitInsn(Opcodes.ICONST_0);
				//this.mv.visitVarInsn(Opcodes.ALOAD, 0);
				//this.mv.visitFieldInsn(Opcodes.GETFIELD, this.className, MIBConfiguration.getMibId(), Type.INT_TYPE.getDescriptor());
				this.mv.visitVarInsn(Opcodes.ALOAD, 0);
			}
			
			this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, 
					methodStackRecorder, 
					"<init>", 
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V");
			this.mv.visitVarInsn(Opcodes.ASTORE, this.localMsrId);
		}
	}
	
	@Override
	public void visitCode() {
		this.mv.visitCode();
		if (this.constructor && !this.superVisited) {
			//For some reasons, AdviceAdapter does not work. Do it by myself
			//Give obj id before visit super class
			this.mv.visitVarInsn(Opcodes.ALOAD, 0);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ObjectIdAllocater.class), "getIndex", "()I");
			this.mv.visitFieldInsn(Opcodes.PUTFIELD, this.className, MIBConfiguration.getMibId(), "I");
			return ; 
		}
		
		if (this.shouldInstrument() && this.localMsrId < 0) {			
			System.out.println("Visit code: " + this.myName + " " + this.shouldInstrument());
			//Create the method stack recorder
			this.localMsrId = this.lvs.newLocal(Type.getType(MethodStackRecorder.class));
			System.out.println("Method Stack Recorder name: " + methodStackRecorder);
			this.mv.visitTypeInsn(Opcodes.NEW, methodStackRecorder);
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitLdcInsn(this.className);
			this.mv.visitLdcInsn(this.myName);
			this.mv.visitLdcInsn(this.desc);
			
			if (this.isStatic) {
				//this.mv.visitInsn(Opcodes.ICONST_1);
				this.mv.visitInsn(Opcodes.ACONST_NULL);
			} else {
				//this.mv.visitInsn(Opcodes.ICONST_0);
				
				//this.mv.visitVarInsn(Opcodes.ALOAD, 0);
				//this.mv.visitFieldInsn(Opcodes.GETFIELD, this.className, MIBConfiguration.getMibId(), Type.INT_TYPE.getDescriptor());
				this.mv.visitVarInsn(Opcodes.ALOAD, 0);
			}
			
			this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, 
					methodStackRecorder, 
					"<init>", 
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V");
			this.mv.visitVarInsn(Opcodes.ASTORE, this.localMsrId);
		}
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if (desc.equals(this.templateAnnot)) {
			this.isTemplate = true;
			System.out.println("Template annotated: " + desc);
			System.out.println("Method name: " + this.myName);
		} else if (desc.equals(this.testAnnot)) {
			this.isTest = true;
			System.out.println("Test annotated: " + desc);
			System.out.println("Method name: " + this.myName);
		}
		return this.mv.visitAnnotation(desc, visible);
	}
	
	@Override
	public void visitLabel(Label label) {
		this.curLabel = label;
		this.allLabels.add(label);
		this.mv.visitLabel(label);
		
		if (this.shouldInstrument() && !this.constructor)
			this.handlLabel(label);
	}
	
	@Override
	public void visitLineNumber(int line, Label label) {
		this.curLineNum = line;
		this.mv.visitLineNumber(line, label);
	}
	
	@Override
	public void visitInsn(int opcode) {
		if (this.shouldInstrument() && !this.constructor) {
			if (!isReturn(opcode)) {
				this.handleOpcode(opcode);
				this.mv.visitInsn(opcode);
			} else {
				this.handleOpcode(opcode);
				this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, methodStackRecorder, srGraphDump, srGraphDumpDesc);
				this.mv.visitInsn(opcode);
			}
			
			//For static analysis
			this.updateMethodRep(opcode);
		} else {
			this.mv.visitInsn(opcode);
		}
	}
	
	@Override
	public void visitIntInsn(int opcode, int operand) {
		if (this.shouldInstrument() && !this.constructor) {
			this.handleOpcode(opcode, operand);
			this.updateMethodRep(opcode);
		}
		this.mv.visitIntInsn(opcode, operand);
	}
	
	@Override
	public void visitVarInsn(int opcode, int var) {
		if (this.shouldInstrument() && !this.constructor) {
			this.handleOpcode(opcode, var);
			this.updateMethodRep(opcode);
		}
		this.mv.visitVarInsn(opcode, var);
		
		if (this.shouldInstrument() && !this.constructor) {
			if (opcode == Opcodes.ALOAD) {
				this.updateObjOnVStack();
			}
		}
	}
	
	@Override
	public void visitTypeInsn(int opcode, String type) {
		if (this.shouldInstrument() && !this.constructor) {
			this.handleOpcode(opcode, type);
			this.updateMethodRep(opcode);
		}
		this.mv.visitTypeInsn(opcode, type);
		
		if (this.shouldInstrument() && !this.constructor) {
			if (opcode == Opcodes.NEW) {
				this.mv.visitInsn(Opcodes.DUP);
			}
		}
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if (this.shouldInstrument() && !this.constructor) {
			this.handleField(opcode, owner, name, desc);
			this.updateMethodRep(opcode);
		}
		this.mv.visitFieldInsn(opcode, owner, name, desc);
		
		if (this.shouldInstrument() && !this.constructor) {
			if ((opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC) 
					&& Type.getType(desc).getSort() == Type.OBJECT) {
				this.updateObjOnVStack();
			}
		}
	}
	
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		//For merging the graph on the fly, need to visit method before recording them
		this.mv.visitMethodInsn(opcode, owner, name, desc);
		//System.out.println("Method should instrument: " + opcode + " " + owner + " " + name + " " + this.shouldInstrument() + " " + this.constructor);
		if (this.shouldInstrument() && !this.constructor) {
			if (opcode == Opcodes.INVOKESPECIAL && name.equals("<init>")) {
				Type[] argTypes = Type.getMethodType(desc).getArgumentTypes();
				int traceBack = 0;
				for (Type t: argTypes) {
					if (t.getDescriptor().equals("D") || t.getDescriptor().equals("J")) {
						traceBack += 2;
					} else {
						traceBack += 1;
					}
				}
				
				this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
				this.mv.visitInsn(Opcodes.SWAP);
				this.convertConst(traceBack);
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
						methodStackRecorder, 
						MIBConfiguration.getObjOnStack(), 
						MIBConfiguration.getObjOnStackDesc());
				
			}
			
			this.handleMethod(opcode, owner, name, desc);
			this.updateMethodRep(opcode);
			
			if (Type.getMethodType(desc).getReturnType().getSort() == Type.OBJECT)
				this.updateObjOnVStack();
		}
		
		//If the INVOKESPECIAL is visited, start instrument constructor
		if (this.constructor && opcode == Opcodes.INVOKESPECIAL && !this.superVisited) {
			this.initConstructor();
			this.superVisited = true;
			this.constructor = false;
		}
	}
	
	@Override
	public void visitJumpInsn(int opcode, Label label) {
		if (this.shouldInstrument() && !this.constructor) {
			String labelString = label.toString();
			this.handleOpcode(opcode, labelString);
			
			this.updateMethodRep(opcode);
		}
		this.mv.visitJumpInsn(opcode, label);
	}
	
	@Override
	public void visitLdcInsn(Object cst) {
		if (this.shouldInstrument() && !this.constructor) {
			if (cst instanceof Double || cst instanceof Long) {
				this.handleLdc(Opcodes.LDC, 2, cst.toString());
			} else {
				this.handleLdc(Opcodes.LDC, 1, cst.toString());
			}
			
			this.updateMethodRep(Opcodes.LDC);
		}
		this.mv.visitLdcInsn(cst);
	}
	
	@Override
	public void visitIincInsn(int var, int increment) {
		if (this.shouldInstrument() && !this.constructor) {
			this.handleOpcode(Opcodes.IINC, var);
			this.updateMethodRep(Opcodes.IINC);
		}
		this.mv.visitIincInsn(var, increment);
	}
	
	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label...labels) {
		if (this.shouldInstrument() && !this.constructor) {
			StringBuilder sb = new StringBuilder();
			for (Label l: labels) {
				sb.append(l.toString() + ",");
			}
			//String labelString = dflt.toString();
			//System.out.println("min max: " + min + " " + max);
			//System.out.println("default: " + labelString);
			//System.out.println("all labels: " + sb.toString());
			this.handleOpcode(Opcodes.TABLESWITCH, sb.substring(0, sb.length() - 1));
			this.updateMethodRep(Opcodes.TABLESWITCH);
		}
		this.mv.visitTableSwitchInsn(min, max, dflt, labels);
	}
	
	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		if (this.shouldInstrument() && !this.constructor) {
			//String labelString = dflt.toString();
			StringBuilder sb = new StringBuilder();
			for (Label l: labels) {
				sb.append(l.toString() + ",");
			}
			this.handleOpcode(Opcodes.LOOKUPSWITCH, sb.substring(0, sb.length() - 1));
			this.updateMethodRep(Opcodes.LOOKUPSWITCH);
		}
		this.mv.visitLookupSwitchInsn(dflt, keys, labels);
	}
	
	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		if (this.shouldInstrument() && !this.constructor) {
			this.handleMultiNewArray(desc, dims);
			this.updateMethodRep(Opcodes.MULTIANEWARRAY);
		}
		this.mv.visitMultiANewArrayInsn(desc, dims);
	}
	
	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
		//Temporarily ignore. Error handling should not affect program similarity?
		this.mv.visitTryCatchBlock(start, end, handler, type);
	}
	
	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int indes) {
		this.mv.visitLocalVariable(name, desc, signature, start, end, indes);
	}
	
	/*@Override
	public void onMethodExit(int opcode) {
		System.out.println("On Method Exit: " + opcode);
		if (this.annotGuard()) {
			System.out.println("On Method Exit: " + opcode);
			//this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
			//this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, methodStackRecorder, srGraphDump, srGraphDumpDesc);
		}
	}*/
	
	@Override
	public void visitEnd() {
		if (this.shouldInstrument()) {
			StringBuilder sb = new StringBuilder();
			for (OpcodeObj oo: this.sequence) {
				sb.append((char)(oo.getCatId() + 97));
			}
			
			//Generate label map
			HashMap<String, Integer> labelMap = new HashMap<String, Integer>();
			for (Label l: this.allLabels) {
				labelMap.put(l.toString(), l.getOffset());
			}
			
			StaticRep sr = new StaticRep();
			sr.setTemplate(this.isTemplate);
			sr.setOpCatString(sb.toString());
			sr.setOpCatFreq(this.repVector);
			sr.setLabelMap(labelMap);
			
			String key = StringUtil.genKey(className, myName, desc) + "_map";
			//TypeToken<HashMap<String, Integer>> typeToken = new TypeToken<HashMap<String, Integer>>(){};
			TypeToken<StaticRep> typeToken = new TypeToken<StaticRep>(){};
			GsonManager.writeJsonGeneric(sr, key, typeToken, 2);
		}
		this.mv.visitEnd();
	}
}
