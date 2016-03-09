package edu.columbia.psl.cc.inst;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.util.CheckClassAdapter;

import edu.columbia.psl.cc.annot.analyzeClass;
import edu.columbia.psl.cc.annot.extractTemplate;
import edu.columbia.psl.cc.annot.testTemplate;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.util.GlobalRecorder;
import edu.columbia.psl.cc.util.StringUtil;

public class MIBClassFileTransformer implements ClassFileTransformer {
	
	private static Logger logger = LogManager.getLogger(MIBClassFileTransformer.class);
	
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
		
		//Check protection domain
		if (protectionDomain != null) {			
			String codeLocation = protectionDomain.getCodeSource().getLocation().getPath();
			/*System.out.println("Class name: " + className);
			System.out.println("Code location: " + codeLocation);
			System.out.println("Is test class: " + StringUtil.isTestClass(codeLocation));*/
			if (StringUtil.isTestClass(codeLocation)) {
				MIBConfiguration.getInstance().getExcludeClass().add(name);
				return classfileBuffer;
			}
			
			if (codeLocation.matches(".*dyclink.*.jar")) {
				return classfileBuffer;
			}
		}
		
		if (StringUtil.shouldIncludeClass(name)) {
			//Start the instrumentation here;
			try {
				ClassReader cr = new ClassReader(classfileBuffer);
				/*ClassWriter preWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
					@Override
					protected String getCommonSuperClass(String type1, String type2) {
						try {
							return super.getCommonSuperClass(type1, type2);
						} catch (Exception ex) {
							return "java/lang/Unknown";
						}
					}
				};*/
				
				ClassWriter preWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				
				cr.accept(new ClassVisitor(Opcodes.ASM5, preWriter) {
					@Override
					public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
						// TODO Auto-generated method stub
						return new JSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc, signature, exceptions);
					}
				}, ClassReader.EXPAND_FRAMES);
				
				
				ClassReader analysisReader = new ClassReader(preWriter.toByteArray());
				ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				//ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
				ClassMiner cm = new ClassMiner(new CheckClassAdapter(cw, false), 
						name.replace(".", "/"), classAnnot, templateAnnot, testAnnot);
				cm.setAnnotGuard(MIBConfiguration.getInstance().isAnnotGuard());
				analysisReader.accept(cm, ClassReader.EXPAND_FRAMES);
				
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
				//ex.printStackTrace();
				logger.error("Fail to transform class: " + name + ", recover original class");
				logger.error("Message: ", ex);
				GlobalRecorder.registerUntransformedClass(name);
			}
		}
		return classfileBuffer;
	}

	

}
