package edu.columbia.psl.cc.inst;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import edu.columbia.psl.cc.config.MIBConfiguration;

public class MIBIDGenVisitor extends  MethodVisitor{
	
	private String owner;
	
	public MIBIDGenVisitor(MethodVisitor mv, String owner) {
		super(Opcodes.ASM4, mv);
		this.owner = owner;
	}
	
	@Override
	public void visitCode() {
		this.mv.visitCode();
		
		this.mv.visitFieldInsn(Opcodes.GETSTATIC, this.owner, MIBConfiguration.getMibIdGen(), "I");
		this.mv.visitInsn(Opcodes.DUP);
		this.mv.visitInsn(Opcodes.ICONST_1);
		this.mv.visitInsn(Opcodes.IADD);
		this.mv.visitFieldInsn(Opcodes.PUTSTATIC, this.owner, MIBConfiguration.getMibIdGen(), "I");
		this.mv.visitInsn(Opcodes.IRETURN);
	}

}
