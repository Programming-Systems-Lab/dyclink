package edu.columbia.psl.cc.inst;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;

import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.pojo.BlockNode;
import edu.columbia.psl.cc.pojo.LabelInterval;
import edu.columbia.psl.cc.pojo.LocalVar;
import edu.columbia.psl.cc.pojo.MultiNewArrayNode;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.pojo.Var;
import edu.columbia.psl.cc.util.MethodStackRecorder;

public class DynamicMethodMiner extends AdviceAdapter {
	
	private static String methodStackRecorder = Type.getInternalName(MethodStackRecorder.class);
	
	private static String srHandleCommon = "handleOpcode";
	
	private static String srHCDesc = "(ILjava/lang/String;I)V";
	
	private static String srHCDescString = "(ILjava/lang/String;Ljava/lang/String;)V";
	
	private static String srHandleMultiArray = "handleMultiNewArray";
	
	private static String srHandleMultiArrayDesc = "(Ljava/lang/String;ILjava/lang/String;)V";
	
	private static String srHandleMethod = "handleMethod";
	
	private static String srHandleMethodDesc = "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V";
	
	private static String srGraphDump = "dumpGraph";
	
	private static String srGraphDumpDesc = "()V";
	
	private String className;
	
	private String myName;
	
	private String desc;
	
	private String templateAnnot;
	
	private String testAnnot;
	
	private boolean isTemplate = false;
	
	private boolean isTest = false;
	
	private LocalVariablesSorter lvs;
	
	private int localMsrId = -1;
	
	private Label curLabel = null;
	 
	public DynamicMethodMiner(MethodVisitor mv, String className, int access, String myName, String desc, String templateAnnot, String testAnnot) {
		super(Opcodes.ASM4, mv, access, myName, desc);
		this.className = className;
		this.myName = myName;
		this.desc = desc;
		this.templateAnnot = templateAnnot;
		this.testAnnot = testAnnot;
	}
	
	public void setLocalVariablesSorter(LocalVariablesSorter lvs) {
		this.lvs = lvs;
	}
	
	public LocalVariablesSorter getLocalVariablesSorter() {
		return this.lvs;
	}
	
	private boolean annotGuard() {
		return (this.isTemplate || this.isTest);
	}
	
	private boolean isReturn(int opcode) {
		if (opcode >= 172 && opcode <= 177)
			return true;
		else 
			return false;
	}
	
	@Override
	public void onMethodEnter() {
		if (this.annotGuard()) {
			//Create the method stack recorder
			this.localMsrId = this.lvs.newLocal(Type.getType(MethodStackRecorder.class));
			System.out.println("Method Stack Recorder name: " + methodStackRecorder);
			this.mv.visitTypeInsn(Opcodes.NEW, methodStackRecorder);
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, methodStackRecorder, "<init>", "()V");
			this.mv.visitVarInsn(Opcodes.ASTORE, this.localMsrId);
			
			/*Label initLabel = new Label();
			this.mv.visitJumpInsn(Opcodes.GOTO, initLabel);
			this.mv.visitLabel(initLabel);
			int workaround = this.lvs.newLocal(Type.INT_TYPE);
			this.mv.visitInsn(Opcodes.ICONST_0);
			this.mv.visitVarInsn(Opcodes.ISTORE, workaround);*/
		}
	}
	
	/*@Override
	public void visitCode() {
		this.mv.visitCode();
		if (this.annotGuard()) {
			//Create the method stack recorder
			Label startLabel = new Label();
			this.mv.visitLabel(startLabel);
			System.out.println("Create new label: " + startLabel);
			this.localMsrId = this.lvs.newLocal(Type.getType(MethodStackRecorder.class));
			System.out.println("Method Stack Recorder name: " + methodStackRecorder);
			this.mv.visitTypeInsn(Opcodes.NEW, methodStackRecorder);
			this.mv.visitInsn(Opcodes.DUP);
			this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, methodStackRecorder, "<init>", "()V");
			this.mv.visitVarInsn(Opcodes.ASTORE, this.localMsrId);
		}
	}*/
	
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
		this.mv.visitLabel(label);
	}
	
	@Override
	public void visitLineNumber(int line, Label label) {
		this.mv.visitLineNumber(line, label);
	}
	
	public void convertConst(int cons) {
		if (cons > 5 && cons < 128) {
			this.mv.visitIntInsn(Opcodes.BIPUSH, cons);
		} else if (cons >=128) {
			this.mv.visitIntInsn(Opcodes.SIPUSH, cons);
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
	}
	
	private void handleOpcode(int opcode, int...addInfo) {
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.convertConst(opcode);
		this.mv.visitLdcInsn(this.curLabel.toString());
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
		this.mv.visitLdcInsn(this.curLabel.toString());
		this.mv.visitLdcInsn(addInfo);
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, methodStackRecorder, srHandleCommon, srHCDescString);
	}
	
	private void handleMultiNewArray(String desc, int dim) {
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.mv.visitLdcInsn(desc);
		this.convertConst(dim);
		this.mv.visitLdcInsn(this.curLabel.toString());
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, methodStackRecorder, srHandleMultiArray, srHandleMultiArrayDesc);
	}
	
	public void handleMethod(int opcode, String owner, String name, String desc) {
		this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
		this.convertConst(opcode);
		this.mv.visitLdcInsn(this.curLabel.toString());
		this.mv.visitLdcInsn(owner);
		this.mv.visitLdcInsn(name);
		this.mv.visitLdcInsn(desc);
		this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, methodStackRecorder, srHandleMethod, srHandleMethodDesc);
	}
	
	@Override
	public void visitInsn(int opcode) {
		if (this.annotGuard() && !isReturn(opcode)) {
			this.handleOpcode(opcode);
			this.mv.visitInsn(opcode);
		} else if (this.annotGuard() && isReturn(opcode)) {
			this.handleOpcode(opcode);
			this.mv.visitVarInsn(Opcodes.ALOAD, this.localMsrId);
			this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, methodStackRecorder, srGraphDump, srGraphDumpDesc);
			this.mv.visitInsn(opcode);
		} else {
			this.mv.visitInsn(opcode);
		}
	}
	
	@Override
	public void visitIntInsn(int opcode, int operand) {
		if (this.annotGuard()) {
			this.handleOpcode(opcode, operand);
		}
		this.mv.visitIntInsn(opcode, operand);
	}
	
	@Override
	public void visitVarInsn(int opcode, int var) {
		if (this.annotGuard()) {
			if (var == this.localMsrId) {
				System.out.println("Got the var created by MIB");
			} else {
				this.handleOpcode(opcode, var);
			}
		}
		this.mv.visitVarInsn(opcode, var);
	}
	
	@Override
	public void visitTypeInsn(int opcode, String type) {
		if (this.annotGuard()) {
			this.handleOpcode(opcode, type);
		}
		this.mv.visitTypeInsn(opcode, type);
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if (this.annotGuard()) {
			String fullField = owner + "." + name + "." + desc;
			this.handleOpcode(opcode, fullField);
		}
		this.mv.visitFieldInsn(opcode, owner, name, desc);
	}
	
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {
		if (this.annotGuard()) {
			//Definitely need a special handler for method
			this.handleMethod(opcode, owner, name, desc);
		}
		this.mv.visitMethodInsn(opcode, owner, name, desc);
	}
	
	@Override
	public void visitJumpInsn(int opcode, Label label) {
		if (this.annotGuard()) {
			String labelString = label.toString();
			this.handleOpcode(opcode, labelString);
		}
		this.mv.visitJumpInsn(opcode, label);
	}
	
	@Override
	public void visitLdcInsn(Object cst) {
		if (this.annotGuard()) {
			this.handleOpcode(Opcodes.LDC, cst.toString());
		}
		this.mv.visitLdcInsn(cst);
	}
	
	@Override
	public void visitIincInsn(int var, int increment) {
		if (this.annotGuard()) {
			this.handleOpcode(Opcodes.IINC, var);
		}
		this.mv.visitIincInsn(var, increment);
	}
	
	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label...labels) {
		if (this.annotGuard()) {
			String labelString = dflt.toString();
			this.handleOpcode(Opcodes.TABLESWITCH, labelString);
		}
		this.mv.visitTableSwitchInsn(min, max, dflt, labels);
	}
	
	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		if (this.annotGuard()) {
			String labelString = dflt.toString();
			this.handleOpcode(Opcodes.LOOKUPSWITCH, labelString);
		}
		this.mv.visitLookupSwitchInsn(dflt, keys, labels);
	}
	
	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		if (this.annotGuard()) {
			this.handleMultiNewArray(desc, dims);
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
	
	/*@Override
	public void visitEnd() {
		if (this.annotGuard()) {
			System.out.println("Method visit ends");
		}
		this.mv.visitEnd();
	}*/

}
