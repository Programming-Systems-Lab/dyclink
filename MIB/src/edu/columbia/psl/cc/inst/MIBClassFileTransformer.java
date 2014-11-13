package edu.columbia.psl.cc.inst;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;

import edu.columbia.psl.cc.annot.analyzeClass;
import edu.columbia.psl.cc.annot.extractTemplate;
import edu.columbia.psl.cc.annot.testTemplate;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.premain.MIBDriver;

public class MIBClassFileTransformer implements ClassFileTransformer {
	
	private static String classAnnot = Type.getType(analyzeClass.class).getDescriptor();
	
	private static String templateAnnot = Type.getType(extractTemplate.class).getDescriptor();
	
	private static String testAnnot = Type.getType(testTemplate.class).getDescriptor();

	@Override
	public byte[] transform(ClassLoader loader, 
			String className,
			Class<?> classBeingRedefined, 
			ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		// TODO Auto-generated method stub
		
		String name = className.replace("/", ".");
		if (!name.startsWith("java") 
				&& !name.startsWith("sun") 
				&& !name.startsWith("edu.columbia.psl.cc") 
				&& !name.startsWith("com.google.gson") 
				&& !name.startsWith("com.apple.java")
				&& !name.startsWith("org.objectweb.asm")
				&& !name.startsWith("org.apache.commons.math3")
				&& !name.startsWith("org.apache.log4j")
				&& !name.startsWith("org.junit")
				&& !name.startsWith("junit.runner")
				&& !name.startsWith("org.hamcrest")
				&& !name.startsWith("org.evosuite")
				&& !name.startsWith("org.slf4j")) {
			//Start the instrumentation here;
			try {
				ClassReader cr = new ClassReader(name);
				ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
				ClassMiner cm = new ClassMiner(new CheckClassAdapter(cw, false), 
						name.replace(".", "/"), classAnnot, templateAnnot, testAnnot);
				cm.setAnnotGuard(MIBConfiguration.getInstance().isAnnotGuard());
				cr.accept(cm, ClassReader.EXPAND_FRAMES);
				
				if (MIBConfiguration.getInstance().isDebug()) {
					String debugDir = MIBConfiguration.getInstance().getDebugDir();
					File f = new File(debugDir);
					if (!f.exists()) {
						f.mkdir();
					}
					
					FileOutputStream fos = new FileOutputStream(debugDir + "/" + name + ".class");
					ByteArrayOutputStream bos = new ByteArrayOutputStream(cw.toByteArray().length);
					bos.write(cw.toByteArray());
					bos.writeTo(fos);
					fos.close();
				}
				
				return cw.toByteArray();
			} catch (Exception ex) {
				System.out.println("In the exception");
				ex.printStackTrace();
			}
		}
		return classfileBuffer;
	}

	

}
