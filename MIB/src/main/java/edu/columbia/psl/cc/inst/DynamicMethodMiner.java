package edu.columbia.psl.cc.inst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import edu.columbia.psl.cc.config.IInstrumentInfo;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.util.GlobalRecorder;
import edu.columbia.psl.cc.util.MethodStackRecorder;
import edu.columbia.psl.cc.util.ObjectIdAllocater;
import edu.columbia.psl.cc.util.StringUtil;

public class DynamicMethodMiner extends MethodVisitor implements IInstrumentInfo{
	
	private static Logger logger = LogManager.getLogger(DynamicMethodMiner.class);
	
	private static String methodStackRecorder = Type.getInternalName(MethodStackRecorder.class);
	
	private static String globalRecorder = Type.getInternalName(GlobalRecorder.class);
	
	private String className;
	
	private String superName;
	
	private String myName;
	
	private String desc;
	
	private String fullKey;
	
	private String shortKey;
	
	private String templateAnnot;
	
	private String testAnnot;
	
	private boolean isStatic;
	
	private boolean isTemplate = false;
	
	private boolean isTest = false;
	
	private boolean annotGuard;
	
	private LocalVariablesSorter lvs;
	//private AdviceAdapter lvs;
	
	private int localMsrId = -1;
	
	private int virtualMsrId = -1;
	
	private Label curLabel = null;
	
	private int curLineNum = -1;
	
	private List<Label> allLabels = new ArrayList<Label>();
	
	private int[] repVector = new int[BytecodeCategory.getOpcodeCategory().size()];
	
	private HashMap<Integer, ArrayList<OpcodeObj>> records = genRecordTemplate();
	
	private ArrayList<OpcodeObj> sequence = new ArrayList<OpcodeObj>();
	
	private AtomicInteger indexer = new AtomicInteger();
	
	//Enable all instrumentation, if this is a constructor
	private boolean constructor = false;
	
	//Invoke the change of object id
	private boolean superVisited = false;
	
	//Control if the constructor should start passing object to recorder
	private boolean aload0Lock = false;
	
	private boolean visitMethod = false;
	
	//private BasicBlockAnalyzer bbAnalyzer;
	 
	public DynamicMethodMiner(MethodVisitor mv, 
			String className, 
			String superName, 
			int access, 
			String myName, 
			String desc, 
			String templateAnnot, 
			String testAnnot, 
			boolean annotGuard) {
		//super(Opcodes.ASM4, mv, access, myName, desc);
		super(Opcodes.ASM5, mv);
		this.className = className;
		this.superName = superName;
		this.myName = myName;
		this.desc = desc;
		this.fullKey = StringUtil.genKey(className, myName, desc);
		this.shortKey = GlobalRecorder.registerGlobalName(className, myName, fullKey);
		this.templateAnnot = templateAnnot;
		this.testAnnot = testAnnot;
		this.annotGuard = annotGuard;
		this.isStatic = ((access & Opcodes.ACC_STATIC) != 0);
		this.constructor = myName.equals("<init>");
		if (this.constructor)
			this.aload0Lock = true;
		
		//this.bbAnalyzer = new BasicBlockAnalyzer(this.className, this.myName);
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
				objOnStack, 
				objOnStackDesc, 
				false);
	}
	
	private int updateMethodRep(int opcode) {
		int catId = BytecodeCategory.getSetIdByOpcode(opcode);
		if (catId >= 0 ) {
			updateSingleCat(catId, opcode);
		} else {
			logger.error("Cannot find category for: " + opcode);
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
	
	private void handleLinenumber(int linenumber) {
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.convertConst(linenumber);
		this.mv.visitFieldInsn(Opcodes.PUTFIELD, 
				methodStackRecorder, 
				"linenumber", 
				Type.INT_TYPE.getDescriptor());
	}
	
	private void handleLabel(Label label) {
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.mv.visitLdcInsn(label.toString());
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
				methodStackRecorder, 
				srUpdateCurLabel, 
				srUpdateCurLabelDesc, 
				false);
	}
		
	private int handleOpcode(int opcode, int...addInfo) {		
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.convertConst(opcode);
		int idx = this.getIndex();
		this.convertConst(idx);
		if (addInfo.length == 0) {
			this.mv.visitInsn(Opcodes.ICONST_M1);
		} else {
			this.convertConst(addInfo[0]);
		}
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
				methodStackRecorder, 
				srHandleCommon, 
				srHCDesc, 
				false);
		
		return idx;
	}
		
	private int handleOpcode(int opcode, String addInfo) {		
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.convertConst(opcode);
		int idx = this.getIndex();
		this.convertConst(idx);
		this.mv.visitLdcInsn(addInfo);
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
				methodStackRecorder, 
				srHandleCommon, 
				srHCDescString, 
				false);
		
		return idx;
	}
	
	private int handleField(int opcode, String owner, String name, String desc) {
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.convertConst(opcode);
		int idx = this.getIndex();
		this.convertConst(idx);
		this.mv.visitLdcInsn(owner);
		this.mv.visitLdcInsn(name);
		this.mv.visitLdcInsn(desc);
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
				methodStackRecorder, 
				srHandleField, 
				srHandleFieldDesc, 
				false);
		
		return idx;
	}
		
	private int handleLdc(int opcode, int times, String addInfo) {		
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.convertConst(opcode);
		int idx = this.getIndex();
		this.convertConst(idx);
		this.convertConst(times);
		this.mv.visitLdcInsn(addInfo);
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
				methodStackRecorder, 
				srHandleLdc, 
				srHandleLdcDesc, 
				false);
		
		return idx;
	}
	
	private int handleMultiNewArray(String desc, int dim) {		
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.mv.visitLdcInsn(desc);
		this.convertConst(dim);
		int idx = this.getIndex();
		this.convertConst(idx);
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
				methodStackRecorder, 
				srHandleMultiArray, 
				srHandleMultiArrayDesc, 
				false);
		
		return idx;
	}
	
	public int handleMethod(int opcode, String owner, String name, String desc) {		
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.convertConst(opcode);
		int idx = this.getIndex();
		this.convertConst(idx);
		this.convertConst(this.curLineNum);
		this.mv.visitLdcInsn(owner);
		this.mv.visitLdcInsn(name);
		this.mv.visitLdcInsn(desc);
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
				methodStackRecorder, 
				srHandleMethod, 
				srHandleMethodDesc, 
				false);
		
		return idx;
	}
	
	public void initConstructorRecorder() {
		if (this.shouldInstrument()) {
			//Create the method stack recorder
			this.localMsrId = this.lvs.newLocal(Type.getType(MethodStackRecorder.class));
			this.mv.visitTypeInsn(Opcodes.NEW, methodStackRecorder);
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitLdcInsn(this.className);
			this.mv.visitLdcInsn(this.myName);
			this.mv.visitLdcInsn(this.desc);
			
			this.mv.visitInsn(Opcodes.ICONST_0);
			this.convertConst(MethodStackRecorder.CONSTRUCTOR_DEFAULT);
			
			this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, 
					methodStackRecorder, 
					"<init>", 
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZI)V", 
					false);
			this.mv.visitVarInsn(Opcodes.ASTORE, this.localMsrId);
			
			this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
			this.mv.visitInsn(Opcodes.ICONST_0);
			this.mv.visitFieldInsn(Opcodes.PUTFIELD, methodStackRecorder, "initConstructor", "Z");
		}
	}
	
	public void initConstructor(boolean genObjId) {
		logger.info("Visit constructor: " + this.className + " " + this.myName + " " + this.shouldInstrument());
		
		if (this.shouldInstrument()) {
			String superReplace = this.superName.replace("/", ".");
			
			logger.info("Should gen ID: " + (!StringUtil.shouldIncludeClass(superReplace) && genObjId));
			if (!StringUtil.shouldIncludeClass(superReplace) && genObjId) {
				logger.info("Obj ID holder: " + this.className + " " + this.myName);
				this.mv.visitVarInsn(Opcodes.ALOAD, 0);
				this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
						Type.getInternalName(ObjectIdAllocater.class), 
						"getIndex", 
						"()I", 
						false);
				this.mv.visitFieldInsn(Opcodes.PUTFIELD, this.className, __mib_id, "I");
			}
			
			//Change the obj id of MethodStackRecorder
			this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
			this.mv.visitVarInsn(Opcodes.ALOAD, 0);
			this.mv.visitFieldInsn(Opcodes.GETFIELD, this.className, __mib_id, "I");
			this.mv.visitFieldInsn(Opcodes.PUTFIELD, methodStackRecorder, "objId", "I");
			
			this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
			this.mv.visitInsn(Opcodes.ICONST_1);
			this.mv.visitFieldInsn(Opcodes.PUTFIELD, methodStackRecorder, "initConstructor","Z");
			
			this.mv.visitVarInsn(Opcodes.ALOAD, 0);
			this.mv.visitFieldInsn(Opcodes.GETFIELD, this.className, __mib_id, "I");
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					globalRecorder, 
					"initUnIdGraphs", 
					"(I)V", 
					false);
		}
	}
	
	@Override
	public void visitCode() {
		this.mv.visitCode();
		if (this.constructor && !this.superVisited) {
			System.out.println("Init recorder for constructor: " + this.className);
			this.initConstructorRecorder();
			return ; 
		}
		
		if (this.shouldInstrument() && this.localMsrId < 0) {
			logger.info("Visit method: " + this.myName + " " + this.shouldInstrument());
			
			//Create the method stack recorder
			this.localMsrId = this.lvs.newLocal(Type.getType(MethodStackRecorder.class));
			this.mv.visitTypeInsn(Opcodes.NEW, methodStackRecorder);
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitLdcInsn(this.className);
			this.mv.visitLdcInsn(this.myName);
			this.mv.visitLdcInsn(this.desc);
			
			if (this.isStatic) {
				this.mv.visitInsn(Opcodes.ICONST_1);
				this.mv.visitInsn(Opcodes.ICONST_0);
			} else {
				this.mv.visitInsn(Opcodes.ICONST_0);
				this.mv.visitVarInsn(Opcodes.ALOAD, 0);
				this.mv.visitFieldInsn(Opcodes.GETFIELD, this.className, __mib_id, "I");
			}
			
			this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, 
					methodStackRecorder, 
					"<init>", 
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZI)V", 
					false);
			this.mv.visitVarInsn(Opcodes.ASTORE, this.localMsrId);
			
			if (this.myName.equals("<clinit>")) {
				this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
				this.mv.visitLdcInsn(this.superName);
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
						methodStackRecorder, 
						srCheckClInit, 
						srCheckClInitDesc, 
						false);
			}
		}
	}
	
	@Override
	public void visitLabel(Label label) {
		this.curLabel = label;
		this.allLabels.add(label);
		this.mv.visitLabel(label);
		
		if (this.shouldInstrument()) {
			this.handleLabel(label);
			//this.bbAnalyzer.signalLabel(label);
		}
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if (desc.equals(this.templateAnnot)) {
			this.isTemplate = true;
		} else if (desc.equals(this.testAnnot)) {
			this.isTest = true;
		}
		return this.mv.visitAnnotation(desc, visible);
	}
	
	@Override
	public void visitLineNumber(int line, Label label) {
		this.curLineNum = line;
		this.mv.visitLineNumber(line, label);
		
		if (this.shouldInstrument())
			this.handleLinenumber(line);
	}
	
	@Override
	public void visitInsn(int opcode) {
		if (this.shouldInstrument()) {
			int instIdx = -1;
			if (!isReturn(opcode)) {
				instIdx = this.handleOpcode(opcode);
				this.mv.visitInsn(opcode);
			} else {
				instIdx = this.handleOpcode(opcode);
				this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
						methodStackRecorder, 
						srGraphDump, 
						srGraphDumpDesc, 
						false);
				this.mv.visitInsn(opcode);
			}
			
			if (opcode == Opcodes.AALOAD) {
				this.updateObjOnVStack();
			}
			
			//For static analysis
			this.updateMethodRep(opcode);
			//this.bbAnalyzer.signalInst(opcode);
		} else {
			this.mv.visitInsn(opcode);
		}
	}
	
	@Override
	public void visitIntInsn(int opcode, int operand) {
		if (this.shouldInstrument()) {
			int instIdx = this.handleOpcode(opcode, operand);
			this.updateMethodRep(opcode);
			//this.bbAnalyzer.signalInst(opcode, String.valueOf(operand));
		}
		this.mv.visitIntInsn(opcode, operand);
		
		if (this.shouldInstrument() && opcode == Opcodes.NEWARRAY) {
			this.updateObjOnVStack();
		}
	}
	
	@Override
	public void visitVarInsn(int opcode, int var) {
		if (this.shouldInstrument()) {
			int instIdx = this.handleOpcode(opcode, var);
			this.updateMethodRep(opcode);
			//this.bbAnalyzer.signalInst(opcode, String.valueOf(var));
		}
		this.mv.visitVarInsn(opcode, var);
		
		if (this.shouldInstrument()) {
			if (opcode == Opcodes.ALOAD && var == 0 && this.aload0Lock) {
				//this.mv.visitInsn(Opcodes.DUP);
			} else if (opcode == Opcodes.ALOAD) {
				this.updateObjOnVStack();
			}
		}
	}
	
	@Override
	public void visitTypeInsn(int opcode, String type) {
		if (this.shouldInstrument()) {
			int instIdx = this.handleOpcode(opcode, type);
			this.updateMethodRep(opcode);
			//this.bbAnalyzer.signalInst(opcode, type);
		}
		this.mv.visitTypeInsn(opcode, type);
		
		if (this.shouldInstrument()) {
			if (opcode == Opcodes.NEW) {
				this.mv.visitInsn(Opcodes.DUP);
			} else if (opcode == Opcodes.CHECKCAST 
					|| opcode == Opcodes.ANEWARRAY) {
				this.updateObjOnVStack();
			}
		}
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if (this.shouldInstrument()) {
			int instIdx = this.handleField(opcode, owner, name, desc);
			this.updateMethodRep(opcode);
			//this.bbAnalyzer.signalInst(opcode, owner + " " + name + " " + desc);
		}
		this.mv.visitFieldInsn(opcode, owner, name, desc);
		
		if (this.shouldInstrument()) {
			if ((opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC)) {
				int sort = Type.getType(desc).getSort();
				if (sort == Type.OBJECT || sort == Type.ARRAY)
					this.updateObjOnVStack();
			}
		}
	}
	
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
		if (this.shouldInstrument()) {
			this.convertConst(this.curLineNum);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(GlobalRecorder.class), 
					"enqueueCalleeLine", 
					"(I)V", 
					false);
		}
		
		//For merging the graph on the fly, need to visit method before recording them
		this.mv.visitMethodInsn(opcode, owner, name, desc, itf);
		
		if (this.shouldInstrument()) {			
			//A bit complex here, this to handle passing object before the super or this has been initialized
			//Only for constructor. But if the object is not this or super, then passing obj should be fine
			if (opcode == Opcodes.INVOKESPECIAL 
					&& name.equals("<init>") 
					&& (!this.aload0Lock || (!owner.equals(this.superName) && !owner.equals(this.className)))) {
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
						objOnStack, 
						objOnStackDesc, 
						false);
				
			}
			
			int instIdx = this.handleMethod(opcode, owner, name, desc);
			this.updateMethodRep(opcode);
			
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(GlobalRecorder.class), 
					"dequeueCalleeLine", 
					"()V", 
					false);
			//this.bbAnalyzer.signalInst(opcode, owner + " " + name + " " + desc);
			
			int returnSort = Type.getMethodType(desc).getReturnType().getSort();
			if (returnSort == Type.OBJECT || returnSort == Type.ARRAY)
				this.updateObjOnVStack();
			
			this.visitMethod = true;
			
			/*if (owner.equals(this.className) && name.equals(this.myName) && desc.equals(this.desc)) {
				GlobalRecorder.registerRecursiveMethod(this.shortKey);
			}*/
		}
		
		//If the INVOKESPECIAL is visited, start instrument constructor
		System.out.println("Touch invoke special: " + this.className + " isConstructor: " + this.constructor + " " + name);
		if (this.constructor 
				&& opcode == Opcodes.INVOKESPECIAL 
				&& (owner.equals(this.superName) || owner.equals(this.className))
				&& name.equals("<init>")
				&& !this.superVisited) {
			logger.info("Super class is visited: " + owner + " " + name);
			logger.info("Start constructor recording: " + this.className + " " + this.myName);
			boolean genObjId = false;
			if (owner.equals(this.superName)) {
				genObjId = true;
			}
			logger.info("Possible cand to gen obj id: " + genObjId);
			this.initConstructor(genObjId);
			this.superVisited = true;
			this.aload0Lock = false;
			this.constructor = false;
		}
	}
	
	@Override
	public void visitJumpInsn(int opcode, Label label) {
		String labelString = label.toString();
		if (this.shouldInstrument()) {
			int instIdx = this.handleOpcode(opcode, labelString);
			this.updateMethodRep(opcode);
			//this.bbAnalyzer.signalJump(opcode, label);
		}
		this.mv.visitJumpInsn(opcode, label);
		
		/*if (this.shouldInstrument() && !this.constructor) {
			if (opcode != Opcodes.GOTO && opcode != Opcodes.JSR) {
				Label breakLabel = new Label();
				this.mv.visitLabel(breakLabel);
				this.blockAnalyzer.setCurLabel(breakLabel);
				this.handleLabel(breakLabel);
				logger.info("Break label: " + breakLabel);
			}
		}*/
	}
	
	@Override
	public void visitLdcInsn(Object cst) {
		if (this.shouldInstrument()) {
			int instIdx = -1;
			if (cst instanceof Double || cst instanceof Long) {
				instIdx = this.handleLdc(Opcodes.LDC, 2, cst.toString());
			} else {
				instIdx = this.handleLdc(Opcodes.LDC, 1, cst.toString());
			}
			
			this.updateMethodRep(Opcodes.LDC);
			//this.bbAnalyzer.signalInst(Opcodes.LDC, cst.toString());
		}
		this.mv.visitLdcInsn(cst);
		
		if (this.shouldInstrument()) {
			if (cst instanceof String) {
				this.updateObjOnVStack();
			} else if (cst instanceof Type) {
				int sort = ((Type)cst).getSort();
				if (sort == Type.OBJECT || sort == Type.ARRAY) {
					this.updateObjOnVStack();
				}
			}
		}
	}
	
	@Override
	public void visitIincInsn(int var, int increment) {
		if (this.shouldInstrument()) {
			int instIdx = this.handleOpcode(Opcodes.IINC, var);
			this.updateMethodRep(Opcodes.IINC);
			//this.bbAnalyzer.signalInst(Opcodes.IINC, var + " " + increment);
		}
		this.mv.visitIincInsn(var, increment);
	}
	
	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label...labels) {
		if (this.shouldInstrument()) {
			StringBuilder sb = new StringBuilder();
			String defaultLabel = dflt.toString();
			
			for (Label l: labels) {
				sb.append(l.toString() + ",");
			}
			
			//System.out.println("min max: " + min + " " + max);
			//System.out.println("default: " + defaultLabel);
			//System.out.println("all labels: " + sb.toString());
			int instIdx = this.handleOpcode(Opcodes.TABLESWITCH, sb.substring(0, sb.length() - 1));
			this.updateMethodRep(Opcodes.TABLESWITCH);
			//this.bbAnalyzer.signalTableSwitch(Opcodes.TABLESWITCH, dflt, labels);
		}
		this.mv.visitTableSwitchInsn(min, max, dflt, labels);
	}
	
	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		if (this.shouldInstrument()) {
			//String labelString = dflt.toString();
			StringBuilder sb = new StringBuilder();
			for (Label l: labels) {
				sb.append(l.toString() + ",");
			}
			int instIdx = this.handleOpcode(Opcodes.LOOKUPSWITCH, sb.substring(0, sb.length() - 1));
			this.updateMethodRep(Opcodes.LOOKUPSWITCH);
			//this.bbAnalyzer.signalLookupSwitch(Opcodes.LOOKUPSWITCH, labels);
		}
		this.mv.visitLookupSwitchInsn(dflt, keys, labels);
	}
	
	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		if (this.shouldInstrument()) {
			int instIdx = this.handleMultiNewArray(desc, dims);
			this.updateMethodRep(Opcodes.MULTIANEWARRAY);
			//this.bbAnalyzer.signalInst(Opcodes.MULTIANEWARRAY, desc + " " + dims);
		}
		this.mv.visitMultiANewArrayInsn(desc, dims);
		
		if (this.shouldInstrument()) {
			this.updateObjOnVStack();
		}
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
		
	@Override
	public void visitEnd() {		
		if (this.indexer.get() < MIBConfiguration.getInstance().getInstThreshold() && !this.visitMethod) {
			GlobalRecorder.registerUndersizedMethod(this.shortKey);
		}
		//this.bbAnalyzer.summarizeDanglings();
		//this.bbAnalyzer.printBlockInfo();
		
		this.mv.visitEnd();
	}
}
