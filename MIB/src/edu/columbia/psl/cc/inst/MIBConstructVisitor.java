package edu.columbia.psl.cc.inst;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.premain.MIBDriver;
import edu.columbia.psl.cc.util.ObjectIdAllocater;

public class MIBConstructVisitor extends MethodVisitor{
	
	private static String indexerClass = Type.getInternalName(ObjectIdAllocater.class);
	
	private String owner;
	
	public MIBConstructVisitor(MethodVisitor mv, String owner) {
		super(Opcodes.ASM4, mv);
		this.owner = owner;
	}
	
	@Override
	public void visitCode() {
		this.mv.visitCode();
		
		this.mv.visitVarInsn(Opcodes.ALOAD, 0);
		this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
		this.mv.visitVarInsn(Opcodes.ALOAD, 0);
		this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, indexerClass, "getIndex", "()I");
		//this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, this.owner, MIBConfiguration.getMIBIDGenMethod(), "()I");
		this.mv.visitFieldInsn(Opcodes.PUTFIELD, this.owner, MIBConfiguration.getMibId(), "I");
		this.mv.visitInsn(Opcodes.RETURN);
	}

}
