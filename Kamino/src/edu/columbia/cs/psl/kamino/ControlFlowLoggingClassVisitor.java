package edu.columbia.cs.psl.kamino;

import edu.columbia.cs.psl.kamino.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.Opcodes;

public class ControlFlowLoggingClassVisitor extends ClassVisitor {
	public static boolean IS_RUNTIME_INST = true;

    String className;

    public ControlFlowLoggingClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    	if (version == 196653 || version < 50)
			throw new ClassFormatError();
    	super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor methodWriter = super.visitMethod(access, name, desc, signature, exceptions); // this is methodwriter
        BasicBlockIdentifier bbi = new BasicBlockIdentifier(methodWriter, Opcodes.ASM5, access, className, name, desc, signature, exceptions);
        return bbi;
    }
}
