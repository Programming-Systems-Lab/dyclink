package edu.columbia.cs.psl.kamino;

import edu.columbia.cs.psl.kamino.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.util.CheckMethodAdapter;

public class ControlFlowLoggingClassVisitor extends ClassVisitor {
    String className;

    public ControlFlowLoggingClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM5, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.className = name;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

        MethodVisitor methodWriter = super.visitMethod(access, name, desc, signature, exceptions); // this is methodwriter
        CheckMethodAdapter checker = new CheckMethodAdapter(methodWriter);
        BasicBlockIdentifier bbi = new BasicBlockIdentifier(checker, Opcodes.ASM5, access, className, name, desc, signature, exceptions);
        return bbi;
    }
}
