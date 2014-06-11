package edu.columbia.cs.psl.kamino;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.columbia.cs.psl.kamino.org.objectweb.asm.Handle;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.Label;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.util.Printer;

public class MethodPrintingMV extends MethodVisitor {

    public ArrayList<String> visitList = new ArrayList<String>();
    public Map<Label, Integer> label_int_map = new HashMap<Label, Integer>();

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
        log(Printer.OPCODES[opcode] + " " + owner + "." + name + " : " + desc);
    }

    @Override
    public void visitMethodInsn(final int opcode, final String owner,
            final String name, final String desc, final boolean itf) {
        super.visitMethodInsn(opcode, owner, name, desc, itf);
        log(Printer.OPCODES[opcode] + " " + owner + "." + name + " " + desc);
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
        super.visitJumpInsn(opcode, label);
        log(Printer.OPCODES[opcode] + " " + label);
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
        // FIXME LAN - not sure this determining the correct type of frame
        super.visitFrame(type, nLocal, local, nStack, stack);
        String frame_type = "?";
        switch (type) {
            case Opcodes.F_SAME:
                frame_type = "FRAME SAME ";
                break;
            case Opcodes.F_SAME1:
                frame_type = "FRAME SAME1 ";
                break;
            case Opcodes.F_APPEND:
                frame_type = "FRAME APPEND ";
                break;
            case Opcodes.F_CHOP:
                frame_type = "FRAME CHOP ";
                break;
            case Opcodes.F_FULL:
                frame_type = "FRAME FULL ";
                break;
            case Opcodes.F_NEW:
                frame_type = "FRAME NEW ";
                break;
        }
        log(frame_type + ((nLocal > 0) ? Arrays.asList(local) : "") + ((nStack > 0) ? " " + Arrays.asList(stack) : ""));
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
        log(name + " " + desc + " " + bsm + " " + bsmArgs);
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        super.visitIincInsn(var, increment);
        log(var + " " + increment);
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);
        log("LABEL " + label.toString());
    }

    @Override
    public void visitLdcInsn(Object cst) {
        super.visitLdcInsn(cst);
        log("LDC " + cst);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        super.visitLineNumber(line, start);
        log("LINENUMBER " + line + " " + start);
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
        super.visitLocalVariable(name, desc, signature, start, end, index);
        log("LOCALVARIABLE " + name + " " + desc + " " + ((signature != null) ? signature + " " : "") + start.info + " L" + end + " " + index);
        //        LOCALVARIABLE this LBytecodeTest; St L55239066 E L564106814 0,

    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        super.visitLookupSwitchInsn(dflt, keys, labels);
        log(dflt + " " + keys + " " + labels);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack, maxLocals);
        log(maxStack + " " + maxLocals);
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        super.visitMultiANewArrayInsn(desc, dims);
        log(desc + " " + dims);
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        super.visitTableSwitchInsn(min, max, dflt, labels);
        log(min + " " + max + " " + dflt + " " + labels);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        super.visitTryCatchBlock(start, end, handler, type);
        log(start + " " + end + " " + handler + " " + type);
    }

    /*
     * void visitFieldInsn(int opcode, String owner, String name, String desc)
     * void visitInsn(int opcode)
     * void visitIntInsn(int opcode, int operand)
     * void visitJumpInsn(int opcode, Label label)
     * void visitMethodInsn(int opcode, String owner, String name, String desc)
     * void visitTypeInsn(int opcode, String type)
     * void visitVarInsn(int opcode, int var)
     */

}
