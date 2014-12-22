package edu.columbia.psl.cc.inst;

import java.util.HashMap;
import java.util.ArrayList;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.OpcodeObj;

import org.apache.log4j.Logger;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;


public class ClassMiner extends ClassVisitor{
	
	private static Logger logger = Logger.getLogger(ClassMiner.class);
	
	private String classAnnot;
	
	private String templateAnnot;
	
	private String testAnnot;
	
	private String owner;
	
	private String superName;
	
	private boolean isAnnot = false;
	
	private HashMap<String, int[]> totalRepVectors = new HashMap<String, int[]>();
	
	private HashMap<String, HashMap<Integer, ArrayList<OpcodeObj>>> totalRecords = new HashMap<String, HashMap<Integer, ArrayList<OpcodeObj>>>();
	
	private HashMap<String, ArrayList<OpcodeObj>> totalSequence = new HashMap<String, ArrayList<OpcodeObj>>();
	
	private boolean isInterface;
	
	private boolean constructVisit = false;
	
	private boolean annotGuard = true;
		
	public ClassMiner(ClassVisitor cv, String owner, String classAnnot, String templateAnnot, String testAnnot) {
		super(Opcodes.ASM4, cv);
		this.owner = owner;
		this.classAnnot = classAnnot;
		this.templateAnnot = templateAnnot;
		this.testAnnot = testAnnot;
	}
	
	public void setAnnotGuard(boolean annotGuard) {
		this.annotGuard = annotGuard;
	}
	
	private boolean shouldInstrument() {
		if (this.annotGuard)
			return this.isAnnot && !this.isInterface;
		else
			return !this.isInterface;
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.superName = superName;
		this.cv.visit(version, access, name, signature, superName, interfaces);
		this.isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
		if (!isInterface) {
			logger.info("Instrumenting class " + name + " extends " + superName);
			this.cv.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, 
					MIBConfiguration.getMibIdGen(), "I", null, 1);
			this.cv.visitField(Opcodes.ACC_PUBLIC, MIBConfiguration.getMibId(), "I", null, null);
		} else {
			logger.info("Not instrument interface: " + name);
		}
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if (desc.equals(this.classAnnot)) {
			this.isAnnot = true;
		}
		return this.cv.visitAnnotation(desc, visible);
	}
		
	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		return this.cv.visitField(access, name, desc, signature, value);
	}
	
	@Override
	public MethodVisitor visitMethod(int access, final String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = this.cv.visitMethod(access, name, desc, signature, exceptions);
		//boolean isSynthetic = ((access & Opcodes.ACC_SYNTHETIC) != 0);
		//boolean isBridge = ((access & Opcodes.ACC_BRIDGE) != 0);
		//if (this.shouldInstrument() && isSynthetic && !isBridge)
		if (this.shouldInstrument()) {
			if (name.equals("<init>")) {
				constructVisit = true;
				//System.out.println("Constructor visit code");
			} else if (name.equals("toString") && Type.getReturnType(desc).equals(Type.getType(String.class))) {
				return mv;
			} else if (name.equals("equals") && Type.getReturnType(desc).equals(Type.BOOLEAN_TYPE)) {
				return mv;
			} else if (name.equals("hashCode") && Type.getReturnType(desc).equals(Type.INT_TYPE)) {
				return mv;
			}
			
			DynamicMethodMiner dmm =  new DynamicMethodMiner(mv, 
					this.owner, 
					this.superName, 
					access, 
					name, 
					desc, 
					this.templateAnnot, 
					this.testAnnot, 
					this.annotGuard);
			LocalVariablesSorter lvs = new LocalVariablesSorter(access, desc, dmm);
			
			//See the comment from https://github.com/pmahoney/asm-bug/blob/master/src/main/java/Main.java
			/*try {
				final Field field = LocalVariablesSorter.class.getDeclaredField("changed");
				field.setAccessible(true);
				field.setBoolean(lvs, true);
			} catch (Exception ex) {
				ex.printStackTrace();
			}*/
			
			dmm.setLocalVariablesSorter(lvs);
			return dmm.getLocalVariablesSorter();
		}
		return mv;
	}
	
	@Override
	public void visitEnd() {
		if (this.shouldInstrument()) {
			//Create the static id generator
			MethodVisitor mv = this.cv.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC + Opcodes.ACC_SYNCHRONIZED, 
					MIBConfiguration.getMibIdGenMethod(), "()I", null, null);
			mv = new MIBIDGenVisitor(mv, this.owner);
			mv.visitCode();
			mv.visitMaxs(0, 0);
			mv.visitEnd();
			
			//Create constructor if there is no constructor
			if (!constructVisit) {
				logger.info("Create new constructor: " + this.owner);
				MethodVisitor constMV = this.cv.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
				constMV = new MIBConstructVisitor(constMV, this.owner);
				constMV.visitCode();
				constMV.visitMaxs(0, 0);
				constMV.visitEnd();
			}
		}
		
		this.cv.visitEnd();
	}
	
	/*public void updateVectorRecord(String key, int[] repVector, HashMap<Integer, ArrayList<OpcodeObj>> records, ArrayList<OpcodeObj> sequence) {
		this.totalRepVectors.put(key, repVector);
		this.totalRecords.put(key, records);
		this.totalSequence.put(key, sequence);
	}*/

}
