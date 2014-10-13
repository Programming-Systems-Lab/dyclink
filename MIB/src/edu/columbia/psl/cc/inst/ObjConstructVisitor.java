package edu.columbia.psl.cc.inst;

import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ObjConstructVisitor extends  MethodVisitor{
	
	private String owner;
	
	public ObjConstructVisitor(MethodVisitor mv, String owner) {
		super(Opcodes.ASM4, mv);
		this.owner = owner;
	}
	
	@Override
	public void visitCode() {
		this.mv.visitCode();
		
		this.mv.visitTypeInsn(Opcodes.NEW, Type.getDescriptor(AtomicInteger.class));
		this.mv.visitInsn(Opcodes.DUP);
		this.mv.visitMethodInsn(Opcodes.INVOKESPECIAL, 
				Type.getType(AtomicInteger.class).getInternalName(), "<init>", "()V");
		this.mv.visitFieldInsn(Opcodes.PUTSTATIC, this.owner, "__mib_id", Type.getDescriptor(AtomicInteger.class));
	}

}
