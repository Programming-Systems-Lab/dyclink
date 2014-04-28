package edu.columbia.cs.psl.kamino;

import edu.columbia.cs.psl.kamino.org.objectweb.asm.ClassVisitor;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.MethodVisitor;
import edu.columbia.cs.psl.kamino.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.kamino.CallGraph;
import edu.columbia.cs.psl.kamino.MethodInformation;
import edu.columbia.cs.psl.kamino.MiniClassNode;

public class CallGraphBuildingClassVisitor extends ClassVisitor {

	String className;
	final CallGraph graph;
	MiniClassNode thisCN;

	public CallGraphBuildingClassVisitor(ClassVisitor cv, CallGraph graph) {
		super(Opcodes.ASM5, cv);
		this.graph = graph;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (version == 196653 || version < 50)
			version = 50;
		super.visit(version, access, name, signature, superName, interfaces);
		this.className = name;
		thisCN = graph.getClassNode(name);
		thisCN.interfaces = interfaces;
		thisCN.superName = superName;
	}

	@Override
	public MethodVisitor visitMethod(int access, final String name, final String desc, String signature, String[] exceptions) {
		final MethodInformation thisMIN = graph.getMethodNode(className, name, desc);
		thisMIN.setVisited(true);
		if ((access & Opcodes.ACC_NATIVE) != 0) thisMIN.setPure(false);
		return new MethodVisitor(Opcodes.ASM5, super.visitMethod(access, name, desc, signature, exceptions)) {
			@Override
			public void visitMethodInsn(int opcode, String _owner, String _name, String _desc, boolean intfc) {
				super.visitMethodInsn(opcode, _owner, _name, _desc, intfc);
				if (!(opcode == Opcodes.INVOKESPECIAL && _owner.equals("java/lang/Object")))
				    graph.addEdge(className, name, desc, _owner, _name, _desc);
				if (_owner.equals("java/lang/reflect/Method") && name.equals("invoke")) thisMIN.setCallsTaintedMethods(true);
				if (opcode == Opcodes.INVOKESTATIC) graph.addEdge(className, name, desc, _owner, "<clinit>", "()V");
			}

			@Override
			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				super.visitFieldInsn(opcode, owner, name, desc);
				thisMIN.setPure(false);
				if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) graph.addEdge(className, name, desc, owner, "<clinit>", "()V");
			}

			@Override
			public void visitTypeInsn(int opcode, String type) {
				super.visitTypeInsn(opcode, type);
				if (opcode == Opcodes.NEW) graph.addEdge(className, name, desc, type, "<clinit>", "()V");
			}
		};
	}
}
