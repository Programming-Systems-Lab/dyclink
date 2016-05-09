package edu.columbia.psl.cc.inst;

import java.util.HashMap;
import java.util.ArrayList;

import edu.columbia.psl.cc.abs.IMethodMiner;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.util.StringUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;


public class ClassMiner extends ClassVisitor implements IMethodMiner{
	
	private static Logger logger = LogManager.getLogger(ClassMiner.class);
	
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
	
	private boolean annotGuard = true;
	
	private boolean objIdOwner = false;
		
	public ClassMiner(ClassVisitor cv, String owner, String classAnnot, String templateAnnot, String testAnnot) {
		super(Opcodes.ASM5, cv);
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
			//Only the object before Object has this objId, all children/grandchildren inherit its value
			String superReplace = this.superName.replace("/", ".");
			if (!StringUtil.shouldIncludeClass(superReplace)) {
				this.cv.visitField(Opcodes.ACC_PUBLIC, __mib_id, "I", null, null);
				this.objIdOwner = true;
			}
		} /*else {
			logger.info("Not instrument interface: " + name);
		}*/
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if (desc.equals(this.classAnnot)) {
			this.isAnnot = true;
		}
		return this.cv.visitAnnotation(desc, visible);
	}
		
	@Override
	public MethodVisitor visitMethod(int access, final String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = this.cv.visitMethod(access, name, desc, signature, exceptions);
		//boolean isSynthetic = ((access & Opcodes.ACC_SYNTHETIC) != 0);
		//boolean isBridge = ((access & Opcodes.ACC_BRIDGE) != 0);
		//if (this.shouldInstrument() && isSynthetic && !isBridge)
		if (this.shouldInstrument()) {
			if (!StringUtil.shouldIncludeMethod(name, desc)) {
				return mv;
			}
			
			if (!MIBConfiguration.getInstance().isCumuGraph()) {
				DynamicMethodMiner dmm =  new DynamicMethodMiner(mv, 
						this.owner, 
						this.superName, 
						access, 
						name, 
						desc, 
						this.templateAnnot, 
						this.testAnnot, 
						this.annotGuard, 
						this.objIdOwner);
				LocalVariablesSorter lvs = new LocalVariablesSorter(access, desc, dmm);
				dmm.setLocalVariablesSorter(lvs);
				return dmm.getLocalVariablesSorter();
			} else {
				CumuMethodMiner cmm = new CumuMethodMiner(mv, 
						this.owner, 
						this.superName, 
						access, 
						name, 
						desc, 
						this.templateAnnot, 
						this.testAnnot, 
						this.annotGuard, 
						this.objIdOwner);
				LocalVariablesSorter lvs = new LocalVariablesSorter(access, desc, cmm);
				cmm.setLocalVariablesSorter(lvs);
				return cmm.getLocalVariablesSorter();
			}
			
			//See the comment from https://github.com/pmahoney/asm-bug/blob/master/src/main/java/Main.java
			/*try {
				final Field field = LocalVariablesSorter.class.getDeclaredField("changed");
				field.setAccessible(true);
				field.setBoolean(lvs, true);
			} catch (Exception ex) {
				ex.printStackTrace();
			}*/
			
		}
		return mv;
	}
		
	/*public void updateVectorRecord(String key, int[] repVector, HashMap<Integer, ArrayList<OpcodeObj>> records, ArrayList<OpcodeObj> sequence) {
		this.totalRepVectors.put(key, repVector);
		this.totalRecords.put(key, records);
		this.totalSequence.put(key, sequence);
	}*/

}
