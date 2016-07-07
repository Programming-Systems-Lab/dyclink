package edu.columbia.psl.cc.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.LocalVariablesSorter;

import edu.columbia.psl.cc.inst.DynamicMethodMiner;

public class StaticMethodAnalyzer {
	
	public final static Logger logger = LogManager.getLogger(StaticMethodAnalyzer.class);
	
	public static Options options = new Options(); 
	
	static {
		options.addOption("cb", true, "codebase");
		options.getOption("cb").setRequired(true);
	}
	
	public static void main(String[] args) throws Exception {
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);
		String codebase = null;
		
		codebase = cmd.getOptionValue("cb");
		if (codebase == null) {
			System.err.println("Please specify input directory");
			System.exit(-1);
		}
		
		//String codebase = args[0];
		System.out.println("Codebase: " + codebase);
		
		List<byte[]> container = new ArrayList<byte[]>();
		ClassDataTraverser.collectDir(codebase, container);
		int classCount = container.size();
		
		final Map<String, Set<Integer>> methodRecords = new TreeMap<String, Set<Integer>>();
		
		for (int i = 0; i < container.size(); i++) {
			byte[] classdata = container.get(i);
			
			try {
				ClassReader analysisReader = new ClassReader(classdata);
				
				ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
										
					String owner;
					
					String superName;
					
					boolean objIdOwner = false;
					
					@Override
					public void visit(int version, 
							int access, 
							String name, 
							String signature, 
							String superName, 
							String[] interfaces) {
						super.visit(version, access, name, signature, superName, interfaces);
						this.owner = name;
						this.superName = superName;
						
						boolean isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
						if (isInterface) {
							MethodCounter.interfaceCounter++;
						} else {
							String superReplace = this.superName.replace("/", ".");
							if (!StringUtil.shouldIncludeClass(superReplace)) {
								//this.cv.visitField(Opcodes.ACC_PUBLIC, __mib_id, "I", null, null);
								this.objIdOwner = true;
							}
						}
						
					}
					
					@Override
					public MethodVisitor visitMethod(int access, 
							String name, 
							String desc, 
							String signature, 
							String[] exceptions) {
						MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
						//System.out.println("Method visitor: " + mv);
						MethodCounter.totalMethodCounter++;
						
						if (!StringUtil.shouldIncludeMethod(name, desc)) {
							MethodCounter.excludeTHE++;
							return mv;
						}
						
						boolean isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0;
						if (isAbstract) {
							MethodCounter.absMethod++;
							return mv;
						}
						
						boolean isSynthetic = (access & Opcodes.ACC_SYNTHETIC) != 0;
						if (isSynthetic) {
							MethodCounter.syntheticMethod++;
							return mv;
						}
						
						if (name.equals("<init>") || name.equals("<clinit>")) {
							MethodCounter.constructMethod++;
							return mv;
						}
						
						String fullKey = StringUtil.genKey(this.owner, name, desc);
						String shortKey = GlobalGraphRecorder.registerGlobalName(this.owner, name, fullKey);
						MethodCounter.potentialMethods.add(shortKey);
						
						DynamicMethodMiner dmm =  new DynamicMethodMiner(mv, 
								this.owner, 
								this.superName, 
								access, 
								name, 
								desc, 
								null, 
								null, 
								false, 
								this.objIdOwner);
						LocalVariablesSorter lvs = new LocalVariablesSorter(access, desc, dmm);
						dmm.setLocalVariablesSorter(lvs);
						return dmm.getLocalVariablesSorter();
					}
				};
				analysisReader.accept(cv, ClassReader.EXPAND_FRAMES);
				//byte[] bytes = cw.toByteArray();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		System.out.println("# class: " + classCount);
		System.out.println("# interface: " + MethodCounter.interfaceCounter);
		System.out.println("# totalMethodCounter: " + MethodCounter.totalMethodCounter);
		System.out.println("# exclude toString, hashCode and equals: " + MethodCounter.excludeTHE);
		System.out.println("# abstract method: " + MethodCounter.absMethod);
		System.out.println("# synthethic method: " + MethodCounter.syntheticMethod);
		System.out.println("# construct method: " + MethodCounter.constructMethod);
		System.out.println("# under-sized method: " + GlobalGraphRecorder.getUndersizedMethods().size());
		
		for (String under: GlobalGraphRecorder.getUndersizedMethods()) {
			MethodCounter.potentialMethods.remove(under);
		}
		
		System.out.println("# potential methods: " + MethodCounter.potentialMethods.size());
		/*for (String m: MethodCounter.potentialMethods) {
			System.out.println(m);
		}*/
	}
	
	public static class MethodCounter {
		
		public static int interfaceCounter;
		
		public static int totalMethodCounter;
		
		public static int excludeTHE;
		
		public static int absMethod;
		
		public static int syntheticMethod;
		
		public static int constructMethod;
		
		public static List<String> potentialMethods = new ArrayList<String>();
	}

}
