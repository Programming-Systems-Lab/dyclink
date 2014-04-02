package edu.columbia.cs.psl.kamino;

import java.util.HashMap;
import java.util.Map;

import edu.columbia.cs.psl.kamino.org.objectweb.asm.Label;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.Type;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.tree.AbstractInsnNode;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.tree.InsnNode;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.tree.IntInsnNode;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.tree.JumpInsnNode;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.tree.LabelNode;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.tree.LdcInsnNode;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.tree.MethodInsnNode;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.tree.MethodNode;
import edu.columbia.cs.psl.kamino.runtime.ControlLogger;

public class BasicBlockIdentifier extends MethodNode {

    private MethodVisitor nextMV;
    public Map<Label, Integer> label_frameID_map = new HashMap<Label, Integer>();
    private int currentFrameID = 0;
    private String className;

    public BasicBlockIdentifier(MethodVisitor nextMV, int api, int access,
            String className, String name, String desc, String signature,
            String[] exceptions) {
        super(api, access, name, desc, signature, exceptions);
        this.className = className;
        this.nextMV = nextMV;
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        
        // Iterate over instructions (1): Locate and record label for each basic block
        AbstractInsnNode insn = this.instructions.getFirst();
        while (insn != null) {
            switch (insn.getType()) {
                case AbstractInsnNode.FRAME:
                    currentFrameID++;
                    break;
                case AbstractInsnNode.JUMP_INSN:
                    if (insn.getOpcode() != Opcodes.GOTO) {
                        currentFrameID++;
                    }
                    break;
                case AbstractInsnNode.LABEL:
                    label_frameID_map.put((((LabelNode) insn).getLabel()),
                            currentFrameID);
                    break;
            }
            insn = insn.getNext();
        }
        
        // Iterate over instructions (2): Log basic blocks
        currentFrameID = 0;
        insn = this.instructions.getFirst();
        while (insn != null) {
            switch (insn.getType()) {
                // AbstractInsnNode.FRAME must be inserted just before instructions following unconditional branch (GOTO, THROW), target of jump,
                // or starts exception handler block
                case AbstractInsnNode.FRAME:
                    // Get the most recent label
                    AbstractInsnNode labelBefore = insn.getPrevious();
                    while (labelBefore.getType() != AbstractInsnNode.LABEL) {
                        labelBefore = labelBefore.getPrevious();
                    }
                    // Don't increment current frame in case of GOTO 
                    if (labelBefore.getPrevious().getOpcode() == Opcodes.GOTO) {
                        break;
                    }
                    currentFrameID++;

                    // Push logging information onto the stack
                    this.instructions.insertBefore(labelBefore, new LdcInsnNode(this.className));
                    this.instructions.insertBefore(labelBefore, new LdcInsnNode(this.name));
                    this.instructions.insertBefore(labelBefore, new LdcInsnNode(this.desc));
                    this.instructions.insertBefore(labelBefore, new IntInsnNode(Opcodes.SIPUSH, currentFrameID));  // to
                    this.instructions.insertBefore(labelBefore, new IntInsnNode(Opcodes.SIPUSH, currentFrameID + 1));  // from
                    this.instructions.insertBefore(labelBefore, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(ControlLogger.class),
                            "logEdge", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V", false));
                    break;

                case AbstractInsnNode.JUMP_INSN:
                    JumpInsnNode jin = (JumpInsnNode) insn;
                    if (insn.getOpcode() == Opcodes.GOTO) {
                        // Push logging information onto the stack
                        this.instructions.insertBefore(jin, new LdcInsnNode(this.className));
                        this.instructions.insertBefore(jin, new LdcInsnNode(this.name));
                        this.instructions.insertBefore(jin, new LdcInsnNode(this.desc));
                        this.instructions.insertBefore(jin, new IntInsnNode(Opcodes.SIPUSH, currentFrameID)); // to 
                        this.instructions.insertBefore(jin, new IntInsnNode(Opcodes.SIPUSH, label_frameID_map.get(jin.label.getLabel()))); // from
                        this.instructions.insertBefore(jin, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(ControlLogger.class),
                                "logEdge", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V", false));
                    } else {
                        // Check if conditional branch takes one or two operands, and duplicate them for Taken/Not-Taken check
                        switch (insn.getOpcode()) {
                            case Opcodes.IFNULL:
                            case Opcodes.IFNONNULL:
                            case Opcodes.IFEQ:
                            case Opcodes.IFNE:
                            case Opcodes.IFLE:
                            case Opcodes.IFLT:
                            case Opcodes.IFGE:
                            case Opcodes.IFGT:
                                this.instructions.insertBefore(insn, new InsnNode(Opcodes.DUP));
                                break;
                            default:
                                this.instructions.insertBefore(insn, new InsnNode(Opcodes.DUP2));
                                break;
                        }

                        // Taken/Not-Taken Check
                        LabelNode branchTaken = new LabelNode(new Label());
                        LabelNode isDone = new LabelNode(new Label());
                        this.instructions.insertBefore(insn, new JumpInsnNode(insn.getOpcode(), branchTaken));
                        this.instructions.insertBefore(insn, new InsnNode(Opcodes.ICONST_0));
                        this.instructions.insertBefore(insn, new JumpInsnNode(Opcodes.GOTO, isDone));
                        this.instructions.insertBefore(insn, branchTaken);
                        this.instructions.insertBefore(insn, new InsnNode(Opcodes.ICONST_1));
                        this.instructions.insertBefore(insn, isDone);

                        // Push logging information onto the stack
                        this.instructions.insertBefore(jin, new LdcInsnNode(this.className));
                        this.instructions.insertBefore(jin, new LdcInsnNode(this.name));
                        this.instructions.insertBefore(jin, new LdcInsnNode(this.desc));
                        this.instructions.insertBefore(jin, new IntInsnNode(Opcodes.SIPUSH, currentFrameID));
                        this.instructions.insertBefore(jin, new IntInsnNode(Opcodes.SIPUSH, label_frameID_map.get(jin.label.getLabel())));
                        this.instructions.insertBefore(jin, new IntInsnNode(Opcodes.SIPUSH, currentFrameID + 1));
                        this.instructions.insertBefore(jin, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(ControlLogger.class),
                                "logEdge", "(ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;III)V", false));
                    }

                    // Don't increment current frame in case of GOTO 
                    if (insn.getOpcode() != Opcodes.GOTO) {
                        currentFrameID++;
                    }
                    break;
            }
            insn = insn.getNext();
        }
        System.out.println("done");
        this.accept(nextMV);
        

        /*
         * // access flags 0x9 public static main([Ljava/lang/String;)V L0
         * //Basic block 0 starts - beginning of method LINENUMBER 4 L0 ALOAD 0
         * ICONST_0 AALOAD LDC "hello" INVOKEVIRTUAL java/lang/String.equals
         * (Ljava/lang/Object;)Z //Need to determine if jumping or not, and log
         * if we are or not IFEQ L1 //Basic block 1 starts - just after jump
         * instruction L2 LINENUMBER 5 L2 GETSTATIC java/lang/System.out :
         * Ljava/io/PrintStream; LDC "Hello" INVOKEVIRTUAL
         * java/io/PrintStream.println (Ljava/lang/String;)V //Always log that
         * we are jumping //Want to call public static
         * logEdge(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V
         * that is, public static void logEdge(String className, String
         * methodName, String methodDescriptor, int bbFrom, int bbTo) GOTO L3
         * //Basic block 2 starts - just after jump instruction
         * 
         * //by our current rule, insert logging here**uncreachable
         * 
         * L1 LINENUMBER 7 L1 FRAME SAME GETSTATIC java/lang/System.out :
         * Ljava/io/PrintStream; LDC "null" INVOKEVIRTUAL
         * java/io/PrintStream.println (Ljava/lang/String;)V //Basic block 3
         * starts - target of jump instruction //Log that we are going striaght
         * from 2->3 L3 LINENUMBER 8 L3 FRAME SAME RETURN L4 LOCALVARIABLE args
         * [Ljava/lang/String; L0 L4 0 MAXSTACK = 2 MAXLOCALS = 1
         */
    }
}
