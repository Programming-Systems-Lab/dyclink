package edu.columbia.psl.cc.inst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import edu.columbia.psl.cc.abs.IMethodMiner;
import edu.columbia.psl.cc.abs.AbstractRecorder;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.util.CumuGraphRecorder;
import edu.columbia.psl.cc.util.CumuMethodRecorder;
import edu.columbia.psl.cc.util.GlobalGraphRecorder;
import edu.columbia.psl.cc.util.ObjectIdAllocater;
import edu.columbia.psl.cc.util.StringUtil;

public class CumuMethodMiner extends MethodVisitor implements IMethodMiner{
	
	private static Logger logger = LogManager.getLogger(DynamicMethodMiner.class);
	
	private static String cumuMethodRecorder = Type.getInternalName(CumuMethodRecorder.class);
	
	private static String cumuGraphRecorder = Type.getInternalName(CumuGraphRecorder.class);
	
	private String className;
	
	private String superName;
	
	private String myName;
	
	private String desc;
	
	private String fullKey;
	
	private String shortKey;
			
	private boolean annotGuard;
	
	private LocalVariablesSorter lvs;
	//private AdviceAdapter lvs;
	
	private int localMsrId = -1;
	
	private int access = -1;
	
	private int curLineNum = -1;
	
	private String templateAnnot;
	
	private String testAnnot;
	
	private List<Label> allLabels = new ArrayList<Label>();
	
	private Set<Label> errorHandles = new HashSet<Label>();
		
	private AtomicInteger indexer = new AtomicInteger();
	
	//Enable all instrumentation, if this is a constructor
	private boolean constructor = false;
	
	private boolean objIdOwner = false;
		
	//Control if the constructor should start passing object to recorder
	private boolean aload0Lock = false;
	
	private boolean isStatic = false;
	
	private boolean visitMethod = false;
	
	private Label curLabel;
	 
	public CumuMethodMiner(MethodVisitor mv, 
			String className, 
			String superName, 
			int access, 
			String myName, 
			String desc, 
			String templateAnnot, 
			String testAnnot, 
			boolean annotGuard, 
			boolean objIdOwner) {
		super(Opcodes.ASM5, mv);
		this.className = className;
		this.superName = superName;
		this.myName = myName;
		this.desc = desc;
		this.fullKey = StringUtil.genKey(className, myName, desc);
		this.shortKey = CumuGraphRecorder.registerGlobalName(className, myName, fullKey);
		this.templateAnnot = templateAnnot;
		this.testAnnot = testAnnot;
		this.annotGuard = annotGuard;
		this.access = access;
		this.objIdOwner = objIdOwner;
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
		//return !this.annotGuard || this.isTemplate || this.isTest;
		return !this.annotGuard;
	}
	
	private boolean isReturn(int opcode) {
		if (opcode >= 172 && opcode <= 177)
			return true;
		else 
			return false;
	}
		
	private void updateObjOnVStack() {		
		//Store it in MethodStackRecorder
		this.mv.visitInsn(Opcodes.DUP);
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.mv.visitInsn(Opcodes.SWAP);
		this.mv.visitInsn(Opcodes.ICONST_0);
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
				cumuMethodRecorder, 
				objOnStack, 
				objOnStackDesc, 
				false);
	}
	
	/*private void updateObjIdOnVStack() {
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.mv.visitVarInsn(Opcodes.ALOAD, 0);
		this.mv.visitFieldInsn(Opcodes.GETFIELD, this.className, __mib_id, "I");
		this.mv.visitInsn(Opcodes.ICONST_0);
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
				cumuMethodRecorder, 
				objIdOnStack, 
				objIdOnStackDesc, 
				false);
	}*/
	
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
				cumuMethodRecorder, 
				"linenumber", 
				Type.INT_TYPE.getDescriptor());
	}
	
	private void handleLabel(Label label) {
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.mv.visitLdcInsn(label.toString());
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
				cumuMethodRecorder, 
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
				cumuMethodRecorder, 
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
				cumuMethodRecorder, 
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
				cumuMethodRecorder, 
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
				cumuMethodRecorder, 
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
				cumuMethodRecorder, 
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
				cumuMethodRecorder, 
				srHandleMethod, 
				srHandleMethodDesc, 
				false);
		
		return idx;
	}
		
	public void initConstructorRecorder() {
		if (this.shouldInstrument()) {
			//Create the method stack recorder
			this.localMsrId = this.lvs.newLocal(Type.getType(CumuMethodRecorder.class));
			this.mv.visitTypeInsn(Opcodes.NEW, cumuMethodRecorder);
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitLdcInsn(this.className);
			this.mv.visitLdcInsn(this.myName);
			this.mv.visitLdcInsn(this.desc);
			
			this.mv.visitInsn(Opcodes.ICONST_0);
			this.convertConst(AbstractRecorder.CONSTRUCTOR_DEFAULT);
			
			this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, 
					cumuMethodRecorder, 
					"<init>", 
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ZI)V", 
					false);
			this.mv.visitVarInsn(Opcodes.ASTORE, this.localMsrId);
			
			this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
			this.mv.visitInsn(Opcodes.ICONST_0);
			this.mv.visitFieldInsn(Opcodes.PUTFIELD, cumuMethodRecorder, "initConstructor", "Z");
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
			this.mv.visitFieldInsn(Opcodes.PUTFIELD, cumuMethodRecorder, "objId", "I");
			
			this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
			this.mv.visitInsn(Opcodes.ICONST_1);
			this.mv.visitFieldInsn(Opcodes.PUTFIELD, cumuMethodRecorder, "initConstructor","Z");
			
			this.mv.visitVarInsn(Opcodes.ALOAD, 0);
			this.mv.visitFieldInsn(Opcodes.GETFIELD, this.className, __mib_id, "I");
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					cumuGraphRecorder, 
					"initUnIdGraphs", 
					"(I)V", 
					false);
		}
	}
	
	@Override
	public void visitCode() {
		this.mv.visitCode();
		
		Label initLabel = new Label();
		if (this.constructor) {
			this.mv.visitVarInsn(Opcodes.ALOAD, 0);
			this.mv.visitFieldInsn(Opcodes.GETFIELD, this.className, __mib_id, "I");
			this.mv.visitJumpInsn(Opcodes.IFNE, initLabel);
			
			this.mv.visitVarInsn(Opcodes.ALOAD, 0);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(ObjectIdAllocater.class), 
					"getIndex", 
					"()I", 
					false);
			this.mv.visitFieldInsn(Opcodes.PUTFIELD, this.className, __mib_id, "I");
			
			this.mv.visitLabel(initLabel);
		}
		
		/*if (this.constructor && this.objIdOwner) {
			this.mv.visitVarInsn(Opcodes.ALOAD, 0);
			this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, 
					Type.getInternalName(ObjectIdAllocater.class), 
					"getIndex", 
					"()I", 
					false);
			objTmp = this.lvs.newLocal(Type.INT_TYPE);
			this.mv.visitVarInsn(Opcodes.ISTORE, objTmp);
			this.mv.visitVarInsn(Opcodes.ILOAD, objTmp);
			this.mv.visitFieldInsn(Opcodes.PUTFIELD, this.className, __mib_id, "I");
		}*/
		
		if (this.shouldInstrument() && this.localMsrId < 0) {
			//logger.info("Visit method: " + this.myName + " " + this.shouldInstrument());
			
			//Create the method stack recorder
			this.localMsrId = this.lvs.newLocal(Type.getType(CumuMethodRecorder.class));
			this.mv.visitTypeInsn(Opcodes.NEW, cumuMethodRecorder);
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitLdcInsn(this.className);
			this.mv.visitLdcInsn(this.myName);
			this.mv.visitLdcInsn(this.desc);
			
			boolean isStatic = ((access & Opcodes.ACC_STATIC) != 0);
			this.convertConst(this.access);
			if (isStatic) {
				//this.mv.visitInsn(Opcodes.ICONST_1);
				this.mv.visitInsn(Opcodes.ICONST_0);
			} else if (this.constructor) {
				this.mv.visitVarInsn(Opcodes.ALOAD, 0);
				this.mv.visitFieldInsn(Opcodes.GETFIELD, this.className, __mib_id, "I");
				/*if (this.objIdOwner) {
					this.mv.visitVarInsn(Opcodes.ILOAD, objTmp);
				} else {
					this.mv.visitInsn(Opcodes.ICONST_0);
				}*/
			} else {
				this.mv.visitVarInsn(Opcodes.ALOAD, 0);
				this.mv.visitFieldInsn(Opcodes.GETFIELD, this.className, __mib_id, "I");
			}
			
			this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, 
					cumuMethodRecorder, 
					"<init>", 
					"(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V", 
					false);
			this.mv.visitVarInsn(Opcodes.ASTORE, this.localMsrId);
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
		/*if (desc.equals(this.templateAnnot)) {
			this.isTemplate = true;
		} else if (desc.equals(this.testAnnot)) {
			this.isTest = true;
		}*/
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
			if (opcode == Opcodes.ATHROW) {
				this.mv.visitInsn(Opcodes.DUP);
				this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
				this.mv.visitInsn(Opcodes.SWAP);
				int idx = this.getIndex();
				this.convertConst(idx);
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, cumuMethodRecorder, "handleAthrow", "(Ljava/lang/Exception;I)V", false);
				
				this.mv.visitInsn(opcode);
			} else if (!isReturn(opcode)) {
				instIdx = this.handleOpcode(opcode);
				this.mv.visitInsn(opcode);
			} else {
				instIdx = this.handleOpcode(opcode);
				this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
						cumuMethodRecorder, 
						srGraphDump, 
						srGraphDumpDesc, 
						false);
				this.mv.visitInsn(opcode);
			}
			
			if (opcode == Opcodes.AALOAD) {
				this.updateObjOnVStack();
			}
			
			//For static analysis
			//this.updateMethodRep(opcode);
			//this.bbAnalyzer.signalInst(opcode);
		} else {
			this.mv.visitInsn(opcode);
		}
	}
	
	@Override
	public void visitIntInsn(int opcode, int operand) {
		if (this.shouldInstrument()) {
			int instIdx = this.handleOpcode(opcode, operand);
			//this.updateMethodRep(opcode);
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
			//this.updateMethodRep(opcode);
			//this.bbAnalyzer.signalInst(opcode, String.valueOf(var));
		}
		this.mv.visitVarInsn(opcode, var);
		
		if (this.shouldInstrument() && opcode == Opcodes.ALOAD) {
			if (this.isStatic) {
				this.updateObjOnVStack();
			} else if (!this.constructor) {
				this.updateObjOnVStack();
			} else if (var != 0) {
				this.updateObjOnVStack();
			} else if (!this.aload0Lock) {
				this.updateObjOnVStack();
			} /*else {
				//Inner class set up a pointer to outer class
				//This happens before Object.<init>
				this.updateObjIdOnVStack();
			}*/
			
			/*if (opcode == Opcodes.ALOAD) {
				if (var != 0 || !this.aload0Lock) {
					this.updateObjOnVStack();
				}
			}*/
			
			/*if (opcode == Opcodes.ALOAD && var == 0 && this.aload0Lock) {
				//this.mv.visitInsn(Opcodes.DUP);
			} else if (opcode == Opcodes.ALOAD) {
				this.updateObjOnVStack();
			}*/
		}
	}
	
	@Override
	public void visitTypeInsn(int opcode, String type) {
		if (this.shouldInstrument()) {
			int instIdx = this.handleOpcode(opcode, type);
			//this.updateMethodRep(opcode);
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
			//this.updateMethodRep(opcode);
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
			int instIdx = this.handleMethod(opcode, owner, name, desc);
		}
		
		//For merging the graph on the fly, need to visit method before recording them
		this.mv.visitMethodInsn(opcode, owner, name, desc, itf);
		
		if (this.shouldInstrument()) {
			Type[] argTypes = Type.getMethodType(desc).getArgumentTypes();
			int traceBack = 0;
			for (Type t: argTypes) {
				if (t.getDescriptor().equals("D") || t.getDescriptor().equals("J")) {
					traceBack += 2;
				} else {
					traceBack += 1;
				}
			}
			
			//A bit complex here, this to handle passing object before the super or this has been initialized
			//Only for constructor. But if the object is not this or super, then passing obj should be fine
			if (opcode == Opcodes.INVOKESPECIAL 
					&& name.equals("<init>") 
					&& (!this.aload0Lock || (!owner.equals(this.superName) && !owner.equals(this.className)))) {				
				this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
				this.mv.visitInsn(Opcodes.SWAP);
				this.convertConst(traceBack);
				this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
						cumuMethodRecorder, 
						objOnStack, 
						objOnStackDesc, 
						false);
			}
						
			int returnSort = Type.getMethodType(desc).getReturnType().getSort();
						
			if (opcode != Opcodes.INVOKESTATIC)
				traceBack++;
			
			this.visitMethod = true;
			
			if (this.constructor 
					&& opcode == Opcodes.INVOKESPECIAL 
					&& (owner.equals(this.superName) || owner.equals(this.className))
					&& name.equals("<init>")) {
				this.aload0Lock = false;
				this.constructor = false;
				
				/*this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
				this.mv.visitVarInsn(Opcodes.ALOAD, 0);
				this.mv.visitFieldInsn(Opcodes.GETFIELD, this.className, __mib_id, "I");
				this.mv.visitFieldInsn(Opcodes.PUTFIELD, cumuMethodRecorder, "objId", "I");*/
			}
			
			//Handle method after
			this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
			this.convertConst(traceBack);
			if (returnSort == Type.DOUBLE || returnSort == Type.LONG) {
				this.convertConst(2);
			} else if (returnSort == Type.VOID){
				this.convertConst(0);
			} else {
				this.convertConst(1);
			}
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
					cumuMethodRecorder, 
					srHandleMethodAfter, 
					srHandleMethodAfterDesc, 
					false);
			
			//After the instruction is inserted into stack
			if (returnSort == Type.OBJECT || returnSort == Type.ARRAY)
				this.updateObjOnVStack();
		}
	}
	
	@Override
	public void visitJumpInsn(int opcode, Label label) {
		String labelString = label.toString();
		if (this.shouldInstrument()) {
			int instIdx = this.handleOpcode(opcode, labelString);
			//this.updateMethodRep(opcode);
			//this.bbAnalyzer.signalJump(opcode, label);
		}
		this.mv.visitJumpInsn(opcode, label);
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
			
			//this.updateMethodRep(Opcodes.LDC);
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
			//this.updateMethodRep(Opcodes.IINC);
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
			//this.updateMethodRep(Opcodes.TABLESWITCH);
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
			
			if (sb.length() == 0) {
				this.handleOpcode(Opcodes.LOOKUPSWITCH, "");
			} else {
				this.handleOpcode(Opcodes.LOOKUPSWITCH, sb.substring(0, sb.length() - 1));
			}
			
			//this.updateMethodRep(Opcodes.LOOKUPSWITCH);
			//this.bbAnalyzer.signalLookupSwitch(Opcodes.LOOKUPSWITCH, labels);
		}
		this.mv.visitLookupSwitchInsn(dflt, keys, labels);
	}
	
	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		if (this.shouldInstrument()) {
			int instIdx = this.handleMultiNewArray(desc, dims);
			//this.updateMethodRep(Opcodes.MULTIANEWARRAY);
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
		this.errorHandles.add(handler);
	}
	
	@Override
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int indes) {
		this.mv.visitLocalVariable(name, desc, signature, start, end, indes);
	}
	
	@Override
	public void visitFrame(int type,
            int nLocal,
            Object[] local,
            int nStack,
            Object[] stack) {
		//System.out.println("Visit frame: " + type + " " + nLocal + " " + nStack);
		this.mv.visitFrame(type, nLocal, local, nStack, stack);
	}
		
	@Override
	public void visitEnd() {
		if (this.indexer.get() < MIBConfiguration.getInstance().getInstThreshold() && !this.visitMethod) {
			CumuGraphRecorder.registerUndersizedMethod(this.shortKey);
		}
		this.mv.visitEnd();
	}
}

