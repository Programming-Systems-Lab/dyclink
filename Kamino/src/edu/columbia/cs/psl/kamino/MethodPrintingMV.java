package edu.columbia.cs.psl.kamino;

import java.util.ArrayList;

import edu.columbia.cs.psl.kamino.org.objectweb.asm.Label;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.util.Printer;

public class MethodPrintingMV extends MethodVisitor {

    public ArrayList<String> visitList = new ArrayList<String>();

    public MethodPrintingMV(MethodVisitor mv) {
        super(Opcodes.ASM5, mv);
    }

    public void log(String str) {
        visitList.add(str);
    }

    public void print(String str) {
        System.out.println(str);
    }

    @Override
    public void visitInsn(int opcode) {
        super.visitInsn(opcode);
        log(Printer.OPCODES[opcode]);
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
        super.visitIntInsn(opcode, operand);
        log(Printer.OPCODES[opcode] + " " + operand);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        super.visitVarInsn(opcode, var);
        log(Printer.OPCODES[opcode] + " " + var);
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
        super.visitTypeInsn(opcode, type);
        log(Printer.OPCODES[opcode] + " " + type);
    }

    @Override
    public void visitFieldInsn(final int opcode, final String owner,
            final String name, final String desc) {
        super.visitFieldInsn(opcode, owner, name, desc);
        log(Printer.OPCODES[opcode] + " " + owner + " " + name + " " + desc);
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner,
            final String name, final String desc, final boolean itf) {
        super.visitMethodInsn(opcode, owner, name, desc, itf);
        log(Printer.OPCODES[opcode] + " " + owner + " " + name + " " + desc + " " + itf);
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        super.visitJumpInsn(opcode, label);
        log(Printer.OPCODES[opcode] + " " + label);
    }

}
