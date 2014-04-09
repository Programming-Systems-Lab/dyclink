package edu.columbia.cs.psl.kamino;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

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
import edu.columbia.cs.psl.kamino.org.objectweb.asm.tree.VarInsnNode;
import edu.columbia.cs.psl.kamino.runtime.ControlLogger;

public class BasicBlockIdentifier extends MethodNode {

    private MethodVisitor nextMV;
    public Map<Label, Integer> label_frameID_map = new HashMap<Label, Integer>();
    public Map<Integer, Vector<DirectionalPair>> variableID_frames_map = new HashMap<Integer, Vector<DirectionalPair>>();

    private int currentFrameID = 0;
    private String className;

    public BasicBlockIdentifier(MethodVisitor nextMV, int api, int access, String className, String name, String desc, String signature,
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
                    label_frameID_map.put((((LabelNode) insn).getLabel()), currentFrameID);
                    break;
            }
            insn = insn.getNext();
        }

        // Iterate over instructions (2): Log basic blocks
        currentFrameID = 0;
        insn = this.instructions.getFirst();
        while (insn != null) {
            switch (insn.getType()) {
            // CONTROL FLOW
            // Inserted before instructions following unconditional branch, target of jump, or starts exception handler block
                case AbstractInsnNode.FRAME:
                    // Push logging information onto the stack
                    this.instructions.insertBefore(insn, new LdcInsnNode(this.className));
                    this.instructions.insertBefore(insn, new LdcInsnNode(this.name));
                    this.instructions.insertBefore(insn, new LdcInsnNode(this.desc));
                    this.instructions.insertBefore(insn, new IntInsnNode(Opcodes.SIPUSH, currentFrameID)); // from
                    this.instructions.insertBefore(insn, new IntInsnNode(Opcodes.SIPUSH, currentFrameID + 1)); // to
                    this.instructions.insertBefore(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(ControlLogger.class),
                            "logEdgeControl", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V", false));

                    currentFrameID++;
                    // Debug info
                    this.instructions.insertBefore(insn, new LdcInsnNode("BB   AbstractInsnNode.FRAME currentFrameID=" + currentFrameID));
                    this.instructions.insertBefore(insn, new InsnNode(Opcodes.POP));

                    break;

                case AbstractInsnNode.JUMP_INSN:
                    JumpInsnNode jumpInsn = (JumpInsnNode) insn;
                    if (insn.getOpcode() == Opcodes.GOTO) {
                        // Push logging information onto the stack
                        this.instructions.insertBefore(jumpInsn, new LdcInsnNode(this.className));
                        this.instructions.insertBefore(jumpInsn, new LdcInsnNode(this.name));
                        this.instructions.insertBefore(jumpInsn, new LdcInsnNode(this.desc));
                        this.instructions.insertBefore(jumpInsn, new IntInsnNode(Opcodes.SIPUSH, currentFrameID)); // from
                        this.instructions.insertBefore(jumpInsn, new IntInsnNode(Opcodes.SIPUSH, label_frameID_map.get(jumpInsn.label.getLabel()))); // to
                        this.instructions.insertBefore(jumpInsn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(ControlLogger.class),
                                "logEdgeControl", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V", false));

                        // Debug Output
                        this.instructions.insertBefore(insn, new LdcInsnNode("BB   AbstractInsnNode.GOTO currentFrameID=" + currentFrameID));
                        this.instructions.insertBefore(insn, new InsnNode(Opcodes.POP));
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
                        LabelNode branchChoice = new LabelNode(new Label());
                        LabelNode isDone = new LabelNode(new Label());
                        this.instructions.insertBefore(insn, new JumpInsnNode(insn.getOpcode(), branchChoice));
                        this.instructions.insertBefore(insn, new InsnNode(Opcodes.ICONST_0));
                        this.instructions.insertBefore(insn, new JumpInsnNode(Opcodes.GOTO, isDone));
                        this.instructions.insertBefore(insn, branchChoice);
                        this.instructions.insertBefore(insn, new InsnNode(Opcodes.ICONST_1));
                        this.instructions.insertBefore(insn, isDone);

                        // Push logging information onto the stack
                        this.instructions.insertBefore(jumpInsn, new LdcInsnNode(this.className));
                        this.instructions.insertBefore(jumpInsn, new LdcInsnNode(this.name));
                        this.instructions.insertBefore(jumpInsn, new LdcInsnNode(this.desc));
                        this.instructions.insertBefore(jumpInsn, new IntInsnNode(Opcodes.SIPUSH, currentFrameID)); // from
                        this.instructions.insertBefore(jumpInsn, new IntInsnNode(Opcodes.SIPUSH, label_frameID_map.get(jumpInsn.label.getLabel()))); // taken
                        this.instructions.insertBefore(jumpInsn, new IntInsnNode(Opcodes.SIPUSH, currentFrameID + 1)); // not taken
                        this.instructions.insertBefore(jumpInsn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(ControlLogger.class),
                                "logEdgeControl", "(ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;III)V", false));

                        // Debug Output
                        this.instructions.insertBefore(insn, new LdcInsnNode("BB   AbstractInsnNode.JUMP currentFrameID=" + currentFrameID));
                        this.instructions.insertBefore(insn, new InsnNode(Opcodes.POP));

                        currentFrameID++;
                    }
                    // Debug info

                    break;

                // DATA FLOW
                case AbstractInsnNode.VAR_INSN:
                    VarInsnNode vin = (VarInsnNode) insn;
                    switch (insn.getOpcode()) {
                    // WRITE: If local variable STORE, log location and add another directional pair entry in map (from one frame to another)
                    // case Opcodes.ASTORE: // store object in local variable
                        case Opcodes.ISTORE: // integer
                        case Opcodes.DSTORE: // double
                        case Opcodes.FSTORE: // float
                        case Opcodes.LSTORE: // long
                        case Opcodes.IINC: // increment local variable
                            Vector<DirectionalPair> frameIDs = new Vector<DirectionalPair>();
                            if (!variableID_frames_map.containsKey(vin.var)) {
                                // If initialization of local variable then no direction, i.e. start = end = currentFrameID
                                frameIDs.add(new DirectionalPair(currentFrameID, currentFrameID));
                                variableID_frames_map.put(vin.var, frameIDs);
                            } else {
                                // Only keep track of writes (so reads will know where their info is coming from most recently)
                                frameIDs = variableID_frames_map.get(vin.var);
                                frameIDs.add(new DirectionalPair(frameIDs.lastElement().getEndFrameID(), currentFrameID));
                                variableID_frames_map.put(vin.var, frameIDs);
                            }
                            // Push logging information onto the stack
                            this.instructions.insertBefore(vin, new LdcInsnNode(this.className));
                            this.instructions.insertBefore(vin, new LdcInsnNode(this.name));
                            this.instructions.insertBefore(vin, new LdcInsnNode(this.desc));
                            this.instructions.insertBefore(vin, new IntInsnNode(Opcodes.SIPUSH, frameIDs.lastElement().getStartFrameID())); // from
                            this.instructions.insertBefore(vin, new IntInsnNode(Opcodes.SIPUSH, frameIDs.lastElement().getEndFrameID())); // to
                            this.instructions.insertBefore(vin, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(ControlLogger.class),
                                    "logEdgeWriteData", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V", false));

                            // Debug output
                            this.instructions.insertBefore(insn, new LdcInsnNode("BB    WRITE from " + frameIDs.lastElement().getStartFrameID()
                                    + " to " + frameIDs.lastElement().getEndFrameID()));
                            this.instructions.insertBefore(insn, new InsnNode(Opcodes.POP));
                            break;

                        // READ: If local variable load, log location
                        // case Opcodes.ALOAD: // load object from local variable
                        case Opcodes.ILOAD: // integer
                        case Opcodes.DLOAD: // double
                        case Opcodes.FLOAD: // float
                        case Opcodes.LLOAD: // long
                            // Push logging information onto the stack
                            this.instructions.insertBefore(vin, new LdcInsnNode(this.className));
                            this.instructions.insertBefore(vin, new LdcInsnNode(this.name));
                            this.instructions.insertBefore(vin, new LdcInsnNode(this.desc));
                            this.instructions.insertBefore(vin, new IntInsnNode(Opcodes.SIPUSH, variableID_frames_map.get(vin.var).lastElement()
                                    .getEndFrameID())); // from
                            this.instructions.insertBefore(vin, new IntInsnNode(Opcodes.SIPUSH, currentFrameID)); // to
                            this.instructions.insertBefore(vin, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(ControlLogger.class),
                                    "logEdgeReadData", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V", false));

                            // Debug output
                            this.instructions.insertBefore(insn, new LdcInsnNode("BB    READ from "
                                    + variableID_frames_map.get(vin.var).lastElement().getEndFrameID() + " to " + currentFrameID));
                            this.instructions.insertBefore(insn, new InsnNode(Opcodes.POP));
                            break;
                    }
            }
            insn = insn.getNext();
        }
        System.out.println(variableID_frames_map);
        System.out.println("done");
        this.accept(nextMV);

        /*
         * // access flags 0x9
         * public static main([Ljava/lang/String;)V
         * 
         * L0
         * ** Basic block 0 starts - beginning of method
         * LINENUMBER 4 L0
         * ALOAD 0
         * ICONST_0
         * AALOAD
         * LDC "hello"
         * INVOKEVIRTUAL java/lang/String.equals(Ljava/lang/Object;)Z
         * ** Need to determine if jumping or not, and log if we are or not
         * IFEQ L1
         * 
         * ** Basic block 1 starts - just after jump instruction
         * L2
         * LINENUMBER 5 L2
         * GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
         * LDC "Hello"
         * INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
         * ** Always log that we are jumping
         * ** Want to call public static logEdge(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V that is,
         * ** public static void logEdge(String className, String methodName, String methodDescriptor, int bbFrom, int bbTo)
         * GOTO L3
         * 
         * ** Basic block 2 starts - just after jump instruction
         * ** by our current rule, insert logging here**unreachable
         * L1
         * LINENUMBER 7 L1
         * FRAME SAME
         * GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
         * LDC "null"
         * INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
         * 
         * ** Basic block 3 starts - target of jump instruction
         * ** Log that we are going straight from 2->3
         * L3
         * LINENUMBER 8 L3
         * FRAME SAME
         * RETURN
         * 
         * L4
         * LOCALVARIABLE args [Ljava/lang/String; L0 L4 0
         * MAXSTACK = 2
         * MAXLOCALS = 1
         */
    }
}
