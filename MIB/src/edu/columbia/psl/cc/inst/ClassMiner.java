package edu.columbia.psl.cc.inst;

import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;

import edu.columbia.psl.cc.datastruct.VarPool;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.util.LevenshteinDistance;
import edu.columbia.psl.cc.util.StringUtil;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;

public class ClassMiner extends ClassVisitor{
	
	private String classAnnot;
	
	private String templateAnnot;
	
	private String testAnnot;
	
	private String owner;
	
	private boolean isAnnot = false;
	
	private HashMap<String, int[]> totalRepVectors = new HashMap<String, int[]>();
	
	private HashMap<String, HashMap<Integer, ArrayList<OpcodeObj>>> totalRecords = new HashMap<String, HashMap<Integer, ArrayList<OpcodeObj>>>();
	
	private HashMap<String, ArrayList<OpcodeObj>> totalSequence = new HashMap<String, ArrayList<OpcodeObj>>();
		
	public ClassMiner(ClassVisitor cv, String owner, String classAnnot, String templateAnnot, String testAnnot) {
		super(Opcodes.ASM4, cv);
		this.owner = owner;
		this.classAnnot = classAnnot;
		this.templateAnnot = templateAnnot;
		this.testAnnot = testAnnot;
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		System.out.println(name + " extends " + superName + "{");
		this.cv.visit(version, access, name, signature, superName, interfaces);
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		System.out.println("Annotation: " + desc);
		if (desc.equals(this.classAnnot)) {
			this.isAnnot = true;
		}
		return this.cv.visitAnnotation(desc, visible);
	}
	
	@Override
	public void visitAttribute(Attribute attr) {
		
	}
	
	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		
	}
	
	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		if (this.isAnnot) {
			System.out.println(desc + " " + name);
			return this.cv.visitField(access, name, desc, signature, value);
		} else {
			return null;
		}
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = this.cv.visitMethod(access, name, desc, signature, exceptions);
		if (this.isAnnot) {
			//mv = new MethodMiner(mv, this.owner, this.templateAnnot, this.testAnnot, name, desc);
			DynamicMethodMiner dmm = new DynamicMethodMiner(mv, this.owner, access, name, desc, this.templateAnnot, this.testAnnot);
			LocalVariablesSorter lvs = new LocalVariablesSorter(access, desc, dmm);
			dmm.setLocalVariablesSorter(lvs);
			return lvs;
		}
		return mv;
	}
	
	@Override
	public void visitEnd() {
		System.out.println("}");
		
		/*System.out.println("Results: ");
		for (String key: this.totalRepVectors.keySet()) {
			System.out.println("Key: " + key);
			System.out.println("Vector: " + Arrays.toString(this.totalRepVectors.get(key)));
			//System.out.println("Record: " + this.totalRecords.get(key));
			System.out.println("Sequence: ");
			StringBuilder sb = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			for (OpcodeObj oo: this.totalSequence.get(key)) {
				sb.append(oo.getCatId() + ",");
				sb2.append((char)(oo.getCatId() + 97));
			}
			String seqInId = sb.toString().substring(0, sb.length() - 1);
			String seqInChar = sb2.toString();
			System.out.println(seqInId);
			System.out.println(seqInChar);
			
			if (key.startsWith("test")) {
				this.simCalculator.addData(key, seqInChar, false);
			} else {
				this.simCalculator.addData(key, seqInChar, true);
			}
		}
		this.simCalculator.generateResult();*/
		this.cv.visitEnd();
	}
	
	/*public void updateVectorRecord(String key, int[] repVector, HashMap<Integer, ArrayList<OpcodeObj>> records, ArrayList<OpcodeObj> sequence) {
		this.totalRepVectors.put(key, repVector);
		this.totalRecords.put(key, records);
		this.totalSequence.put(key, sequence);
	}*/

}
