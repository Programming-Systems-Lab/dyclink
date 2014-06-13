package edu.columbia.cs.psl.kamino;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import edu.columbia.cs.psl.kamino.org.objectweb.asm.tree.VarInsnNode;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.util.Printer;
import edu.columbia.cs.psl.kamino.runtime.FlowOutput;

//import edu.columbia.cs.psl.kamino.runtime.FlowToARFF;

public class BasicBlockIdentifier extends MethodNode {

	public Map<Label, Integer> label_frameID_map = new HashMap<Label, Integer>();
	Map<String, List<String>> label_bytecode_map = new HashMap<String, List<String>>();

	private MethodVisitor nextMV;
	private MethodPrintingMV methodPrintingMV = new MethodPrintingMV(null);
	private int currentFrameID = 0;
	private String className;

	public Map<Label, Integer> getLabelFrameIDMap() {
		return label_frameID_map;
	}

	public BasicBlockIdentifier(MethodVisitor nextMV, int api, int access, String className, String name, String desc, String signature,
	        String[] exceptions) {
		super(api, access, name, desc, signature, exceptions);
		this.className = className;
		this.nextMV = nextMV;
	}

	@Override
	public void visitEnd() {
		super.visitEnd();

		// Keep track of the byte code to use later for displaying similar flows in byte code form
		this.accept(methodPrintingMV);
//        for (String m : methodPrintingMV.visitList) {
//            System.out.println(m);
//        }

		// Iterate over instructions (1): Locate and record label for each basic block
		AbstractInsnNode insn = this.instructions.getFirst();
		// int nLabel = 0;
		Label lastLabel = null;
		int visitListLocation = 0;
		int lastRecordedLocation = 0;
		ArrayList<String> gotoList = new ArrayList<String>();
		while (insn != null) {
			switch (insn.getType()) {
				case AbstractInsnNode.FRAME:
					// Check to make sure we are not first frame after a goto
					AbstractInsnNode prev = insn.getPrevious();

					while (prev.getPrevious() != null && (prev.getType() == AbstractInsnNode.LINE || prev.getType() == AbstractInsnNode.LABEL)) {
						prev = prev.getPrevious();
					}
					if (prev.getOpcode() != Opcodes.GOTO) {
						currentFrameID++;
					}
					if (lastLabel != null) {
						label_frameID_map.put(lastLabel, currentFrameID);
					}
					break;

				case AbstractInsnNode.JUMP_INSN:
					if (insn.getOpcode() == Opcodes.GOTO) {
						gotoList.add(methodPrintingMV.visitList.get(visitListLocation).split(" ")[1]);
						System.out.println("gotoList: " + gotoList);
					}

					// FIXME LAN - turn this into a data structure of some kind
					System.out.println(methodPrintingMV.visitList.subList(lastRecordedLocation, visitListLocation + 1));
					lastRecordedLocation = visitListLocation + 1;
					currentFrameID++;
					break;

				case AbstractInsnNode.LABEL:
					// System.out.println("L" + nLabel + " = " + currentFrameID);
					// nLabel++;
					System.out.println("label: " + ((LabelNode) insn).getLabel());
					if (gotoList.size() > 0) {
						System.out.println("GOTOLIST: \"" + gotoList.get(0) + "\"");
						if (gotoList.get(0).equals(((LabelNode) insn).getLabel())) {
							System.out.println("TESTER TESTER " + ((LabelNode) insn).getLabel());
						}
					}
					label_frameID_map.put((((LabelNode) insn).getLabel()), currentFrameID);
					lastLabel = (((LabelNode) insn).getLabel());
					break;
			}
			insn = insn.getNext();
			if (methodPrintingMV.visitList.get(visitListLocation).contains("RETURN")) {
				System.out.println(methodPrintingMV.visitList.subList(lastRecordedLocation, visitListLocation + 1));
				lastRecordedLocation = visitListLocation + 1;
			}
			visitListLocation++;
		}

		// Iterate over instructions (2): Log basic blocks
		currentFrameID = 0; //NOTE - this will be incorrect at the label before a new frame starts, which probably should count as part of the newly starting frame (but won't here)
		insn = this.instructions.getFirst();
		while (insn != null) {
			switch (insn.getType()) {
			// CONTROL FLOW
			// Inserted before instructions following unconditional branch, target of jump, or starts exception handler block
				case AbstractInsnNode.FRAME:
					AbstractInsnNode prev = insn.getPrevious();
					while (prev.getPrevious() != null && (prev.getType() == AbstractInsnNode.LINE || prev.getType() == AbstractInsnNode.LABEL)) {
						prev = prev.getPrevious();
					}
					// Frame after GOTO will be found because it is definitely reached through a JUMP
					// Frame not after a GOTO could be a JUMP target or could be reached from a fall through so needs to be logged before the LABEL
					if (prev.getOpcode() != Opcodes.GOTO) {
						AbstractInsnNode insertBefore = prev.getNext();

						// Push logging information onto the stack
						this.instructions.insertBefore(insertBefore, new LdcInsnNode(this.className));
						this.instructions.insertBefore(insertBefore, new LdcInsnNode(this.name));
						this.instructions.insertBefore(insertBefore, new LdcInsnNode(this.desc));
						this.instructions.insertBefore(insertBefore, new IntInsnNode(Opcodes.SIPUSH, currentFrameID)); // from
						this.instructions.insertBefore(insertBefore, new IntInsnNode(Opcodes.SIPUSH, currentFrameID + 1)); // to
						// this.instructions.insertBefore(insertBefore, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(FlowToARFF.class),
						this.instructions.insertBefore(insertBefore, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(FlowOutput.class),
						        "logEdgeControl", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V", false));

						// Debug info
						this.instructions.insertBefore(insertBefore, new LdcInsnNode("BB   AbstractInsnNode.FRAME currentFrameID=" + currentFrameID));
						this.instructions.insertBefore(insertBefore, new InsnNode(Opcodes.POP));

						currentFrameID++;
					}
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
						// this.instructions.insertBefore(jumpInsn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(FlowToARFF.class),
						this.instructions.insertBefore(jumpInsn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(FlowOutput.class),
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
						// this.instructions.insertBefore(jumpInsn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(FlowToARFF.class),
						this.instructions.insertBefore(jumpInsn, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(FlowOutput.class),
						        "logEdgeControl", "(ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;III)V", false));

						// Debug Output
						this.instructions.insertBefore(insn, new LdcInsnNode("BB   AbstractInsnNode.JUMP currentFrameID=" + currentFrameID));
						this.instructions.insertBefore(insn, new InsnNode(Opcodes.POP));
					}
					currentFrameID++;
					break;

				// DATA FLOW
				case AbstractInsnNode.VAR_INSN:
					VarInsnNode vin = (VarInsnNode) insn;
					switch (insn.getOpcode()) {
					// WRITE: If local variable STORE, log location and add another directional pair entry in map (from one frame to another)
						case Opcodes.ASTORE: // store object in local variable
						case Opcodes.ISTORE: // integer
						case Opcodes.DSTORE: // double
						case Opcodes.FSTORE: // float
						case Opcodes.LSTORE: // long
						case Opcodes.IINC: // increment local variable

							// Push logging information onto the stack
							this.instructions.insertBefore(vin, new LdcInsnNode(this.className));
							this.instructions.insertBefore(vin, new LdcInsnNode(this.name));
							this.instructions.insertBefore(vin, new LdcInsnNode(this.desc));
							this.instructions.insertBefore(vin, new IntInsnNode(Opcodes.SIPUSH, vin.var));
							this.instructions.insertBefore(vin, new IntInsnNode(Opcodes.SIPUSH, currentFrameID));
							//                            this.instructions.insertBefore(vin, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(FlowToARFF.class),
							this.instructions.insertBefore(vin, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(FlowOutput.class),
							        "logEdgeWriteData", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V", false));

							// Debug Output
							this.instructions.insertBefore(insn, new LdcInsnNode("BB   WRITE in " + currentFrameID));
							this.instructions.insertBefore(insn, new InsnNode(Opcodes.POP));
							break;

						// READ: If local variable load, log location
						case Opcodes.ALOAD: // load object from local variable
						case Opcodes.ILOAD: // integer
						case Opcodes.DLOAD: // double
						case Opcodes.FLOAD: // float
						case Opcodes.LLOAD: // long

							// Push logging information onto the stack
							this.instructions.insertBefore(vin, new LdcInsnNode(this.className));
							this.instructions.insertBefore(vin, new LdcInsnNode(this.name));
							this.instructions.insertBefore(vin, new LdcInsnNode(this.desc));
							this.instructions.insertBefore(vin, new IntInsnNode(Opcodes.SIPUSH, vin.var));
							this.instructions.insertBefore(vin, new IntInsnNode(Opcodes.SIPUSH, currentFrameID));
							//							this.instructions.insertBefore(vin, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(FlowToARFF.class),
							this.instructions.insertBefore(vin, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(FlowOutput.class),
							        "logEdgeReadData", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;II)V", false));

							// Debug Output
							this.instructions.insertBefore(insn, new LdcInsnNode("BB   READ in " + currentFrameID));
							this.instructions.insertBefore(insn, new InsnNode(Opcodes.POP));
							break;
					}
			}
			insn = insn.getNext();
		}
		this.accept(nextMV);
	}
}
