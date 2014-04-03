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
    public Map<Integer, Vector<Integer>> variable_frameID_map = new HashMap<Integer, Vector<Integer>>();

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
            // CONTROL FLOW
                case AbstractInsnNode.FRAME:
                case AbstractInsnNode.JUMP_INSN:
                    currentFrameID++;
                    break;
                /*
                 * Old way --- not working
                 * case AbstractInsnNode.FRAME:
                 * currentFrameID++;
                 * this.instructions.insertBefore(insn, new LdcInsnNode("BB: FRAME "));
                 * this.instructions.insertBefore(insn, new InsnNode(Opcodes.POP));
                 * break;
                 * case AbstractInsnNode.JUMP_INSN:
                 * if (insn.getOpcode() != Opcodes.GOTO) {
                 * currentFrameID++;
                 * this.instructions.insertBefore(insn, new LdcInsnNode("BB: JUMP "));
                 * this.instructions.insertBefore(insn, new InsnNode(Opcodes.POP));
                 * } else {
                 * currentFrameID++;
                 * this.instructions.insertBefore(insn, new LdcInsnNode("BB: GOTO "));
                 * this.instructions.insertBefore(insn, new InsnNode(Opcodes.POP));
                 * }
                 * break;
                 */
                case AbstractInsnNode.LABEL:
                    label_frameID_map.put((((LabelNode) insn).getLabel()), currentFrameID);
                    this.instructions.insertBefore(insn, new LdcInsnNode("BB: label_frameID_map " + (((LabelNode) insn).getLabel()) + " ==> "
                            + currentFrameID));
                    this.instructions.insertBefore(insn, new InsnNode(Opcodes.POP));
                    break;

                // DATA FLOW
                case AbstractInsnNode.VAR_INSN:
                    switch (insn.getOpcode()) {
                    // If local variable STORE then record basic block location
                        case Opcodes.ASTORE: // store object in local variable
                        case Opcodes.ISTORE: // integer
                        case Opcodes.DSTORE: // double
                        case Opcodes.FSTORE: // float
                        case Opcodes.LSTORE: // long
                            
                            // FIXME: LAN - need to record in a way that you can easily grab the currentFrameID + variable information (for dynamic execution)
                            if (!variable_frameID_map.containsKey(((VarInsnNode) insn).var)) {
                                // Create new local variable
                                Vector<Integer> frameIDVector = new Vector<Integer>();
                                frameIDVector.add(currentFrameID);
                                variable_frameID_map.put(((VarInsnNode) insn).var, frameIDVector);
                            } else {
                                // Record update to local variable
                                Vector<Integer> frameIDVector = variable_frameID_map.get(((VarInsnNode) insn).var);
                                frameIDVector.add(0, currentFrameID);
                                variable_frameID_map.put(((VarInsnNode) insn).var, frameIDVector);
                            }
                            System.out.println("Local variable " + ((VarInsnNode) insn).var + ": Frame " + variable_frameID_map.get(((VarInsnNode) insn).var));
                            this.instructions.insertBefore(insn, new LdcInsnNode("BB VARIABLE operand:" + ((VarInsnNode) insn).var + ": Frame " + variable_frameID_map.get(((VarInsnNode) insn).var)));
                            this.instructions.insertBefore(insn, new InsnNode(Opcodes.POP));
                            break;
                    }
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
                    // Get the most recent label
                    AbstractInsnNode labelBefore = insn.getPrevious();
                    while (labelBefore.getType() != AbstractInsnNode.LABEL) {
                        labelBefore = labelBefore.getPrevious();
                    }

                    // If previous basic block contained a GOTO then don't increment frame or record
                    if (labelBefore.getPrevious().getOpcode() == Opcodes.GOTO) {
                        break;
                    }
                    currentFrameID++;

                    // Push logging information onto the stack
                    this.instructions.insertBefore(labelBefore, new LdcInsnNode(this.className));
                    this.instructions.insertBefore(labelBefore, new LdcInsnNode(this.name));
                    this.instructions.insertBefore(labelBefore, new LdcInsnNode(this.desc));
                    this.instructions.insertBefore(labelBefore, new IntInsnNode(Opcodes.SIPUSH, currentFrameID)); // to
                    this.instructions.insertBefore(labelBefore, new IntInsnNode(Opcodes.SIPUSH, currentFrameID + 1)); // from
                    this.instructions.insertBefore(labelBefore, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(ControlLogger.class),
                            "logEdgeControl", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V", false));
                    break;

                case AbstractInsnNode.JUMP_INSN:
                    JumpInsnNode jumpInsn = (JumpInsnNode) insn;
                    if (insn.getOpcode() == Opcodes.GOTO) {
                        // Push logging information onto the stack
                        this.instructions.insertBefore(jumpInsn, new LdcInsnNode(this.className));
                        this.instructions.insertBefore(jumpInsn, new LdcInsnNode(this.name));
                        this.instructions.insertBefore(jumpInsn, new LdcInsnNode(this.desc));
                        this.instructions.insertBefore(jumpInsn, new IntInsnNode(Opcodes.SIPUSH, currentFrameID)); // to
                        this.instructions.insertBefore(jumpInsn, new IntInsnNode(Opcodes.SIPUSH, label_frameID_map.get(jumpInsn.label.getLabel()))); // from
                        this.instructions.insertBefore(jumpInsn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(ControlLogger.class),
                                "logEdgeControl", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V", false));
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
                        this.instructions.insertBefore(jumpInsn, new LdcInsnNode(this.className));
                        this.instructions.insertBefore(jumpInsn, new LdcInsnNode(this.name));
                        this.instructions.insertBefore(jumpInsn, new LdcInsnNode(this.desc));
                        this.instructions.insertBefore(jumpInsn, new IntInsnNode(Opcodes.SIPUSH, currentFrameID)); // from
                        this.instructions.insertBefore(jumpInsn, new IntInsnNode(Opcodes.SIPUSH, label_frameID_map.get(jumpInsn.label.getLabel()))); // taken
                        this.instructions.insertBefore(jumpInsn, new IntInsnNode(Opcodes.SIPUSH, currentFrameID + 1)); // not taken
                        this.instructions.insertBefore(jumpInsn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(ControlLogger.class),
                                "logEdgeControl", "(ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;III)V", false));

                        currentFrameID++;
                    }
                    break;

                // DATA FLOW
                case AbstractInsnNode.VAR_INSN:
                    VarInsnNode vin = (VarInsnNode) insn;
                    switch (insn.getOpcode()) {
                    // READ: If local variable load, record current location
                    // case Opcodes.ALOAD: // load object from local variable
                        case Opcodes.ILOAD: // integer
                        case Opcodes.DLOAD: // double
                        case Opcodes.FLOAD: // float
                        case Opcodes.LLOAD: // long

                            // Push logging information onto the stack
                            this.instructions.insertBefore(vin, new LdcInsnNode(this.className));
                            this.instructions.insertBefore(vin, new LdcInsnNode(this.name));
                            this.instructions.insertBefore(vin, new LdcInsnNode(this.desc));
                            this.instructions.insertBefore(vin, new IntInsnNode(Opcodes.SIPUSH, variable_frameID_map.get(vin.var).get(0))); // from
                            this.instructions.insertBefore(vin, new IntInsnNode(Opcodes.SIPUSH, currentFrameID)); // to
                            this.instructions.insertBefore(vin, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(ControlLogger.class),
                                    "logEdgeReadData", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V", false));
                            break;

                        // WRITE: If local variable STORE, record current location by updating the first position in the array of frames for the variable
                        // case Opcodes.ASTORE: // store object in local variable
                        case Opcodes.ISTORE: // integer
                        case Opcodes.DSTORE: // double
                        case Opcodes.FSTORE: // float
                        case Opcodes.LSTORE: // long
                        case Opcodes.IINC: // increment local variable
                            if (variable_frameID_map.containsValue(currentFrameID)) {
                                // FIXME: LAN - NEED TO ONLY LOG THE PLACES WHERE THIS ID IS
                            }
                            
                            for (int i = 0; i < variable_frameID_map.get(vin.var).size() - 1; i++) {                                
                                // Push logging information onto the stack
                                this.instructions.insertBefore(vin, new LdcInsnNode(this.className));
                                this.instructions.insertBefore(vin, new LdcInsnNode(this.name));
                                this.instructions.insertBefore(vin, new LdcInsnNode(this.desc));
                                this.instructions.insertBefore(vin, new IntInsnNode(Opcodes.SIPUSH, variable_frameID_map.get(vin.var).get(i))); // from
                                this.instructions.insertBefore(vin, new IntInsnNode(Opcodes.SIPUSH, variable_frameID_map.get(vin.var).get(i+1))); // to
                                this.instructions.insertBefore(vin, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(ControlLogger.class),
                                        "logEdgeWriteData", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V", false));
                            }
                            break;
                    }
            }
            insn = insn.getNext();
        }

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
