package edu.columbia.psl.cc.inst;

import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ObjClassVisitor extends ClassVisitor{
	
	private String owner;
	
	public ObjClassVisitor(ClassVisitor cv, String owner) {
		super(Opcodes.ASM5, cv);
		this.owner = owner;
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.cv.visit(version, access, name, signature, superName, interfaces);
		this.cv.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC + Opcodes.ACC_FINAL, 
				"__MIB_ID_GEN", Type.getDescriptor(AtomicInteger.class), null, null);
		this.cv.visitField(Opcodes.ACC_PUBLIC, "__MIB_ID", "I", null, null);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = this.cv.visitMethod(access, name, desc, signature, exceptions);
		if (name.equals("<cinit>")) {
			mv.visitCode();
			mv.visitTypeInsn(Opcodes.NEW, Type.getDescriptor(AtomicInteger.class));
			mv.visitInsn(Opcodes.DUP);
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, 
					Type.getType(AtomicInteger.class).getInternalName(), "<init>", "()V");
			mv.visitFieldInsn(Opcodes.PUTSTATIC, this.owner, "__MIB_ID_GEN", Type.getDescriptor(AtomicInteger.class));
		} else if (name.equals("<init>")) {
			mv.visitIntInsn(Opcodes.ALOAD, 0);
			mv.visitFieldInsn(Opcodes.GETSTATIC, this.owner, "__MIB_ID_GEN", Type.getDescriptor(AtomicInteger.class));
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getDescriptor(AtomicInteger.class), "getAndIncrement", "()I");
			mv.visitFieldInsn(Opcodes.PUTFIELD, this.owner, "__MIB_ID", "I");
		}
		return mv;
	}

}
