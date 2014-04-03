package edu.columbia.cs.psl.kamino;

import edu.columbia.cs.psl.kamino.org.objectweb.asm.Label;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.Opcodes;

public class ControlFlowLoggingMethodVisitor extends MethodVisitor {
    private BasicBlockIdentifier bbi;
    private int basicBlockID = 0;

    public ControlFlowLoggingMethodVisitor(MethodVisitor mv) {
        super(Opcodes.ASM5, mv);
    }

    public void setBasicBlockIdentifier(BasicBlockIdentifier bbi) {
        this.bbi = bbi;
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        super.visitFrame(type, nLocal, local, nStack, stack);
        basicBlockID++;
    }

    @Override
    public void visitLabel(final Label label) {
        super.visitLabel(label);
        System.out.println("label visited " + label);
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        System.out.println("bbi map " + bbi.label_frameID_map);
        System.out.println("Found jump " + opcode + "   basicBlockID " + basicBlockID + "  at " + bbi.label_frameID_map.get(label) + " label "
                + label);
        super.visitInsn(Opcodes.ICONST_0);
        super.visitInsn(Opcodes.POP);
        super.visitJumpInsn(opcode, label);
    }
}
