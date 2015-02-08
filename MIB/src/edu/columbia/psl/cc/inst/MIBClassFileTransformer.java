package edu.columbia.psl.cc.inst;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;

import org.apache.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;

import edu.columbia.psl.cc.annot.analyzeClass;
import edu.columbia.psl.cc.annot.extractTemplate;
import edu.columbia.psl.cc.annot.testTemplate;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.util.GlobalRecorder;
import edu.columbia.psl.cc.util.StringUtil;

public class MIBClassFileTransformer implements ClassFileTransformer {
	
	private static Logger logger = Logger.getLogger(MIBClassFileTransformer.class);
	
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
		//System.out.println("Class Name: " + name);
		
		//Check protection domain
		if (protectionDomain != null) {			
			String codeLocation = protectionDomain.getCodeSource().getLocation().getPath();
			//System.out.println("Class name: " + className);
			//System.out.println("Code location: " + codeLocation);
			//System.out.println("Is test class: " + StringUtil.isTestClass(codeLocation));
			if (StringUtil.isTestClass(codeLocation)) {
				MIBConfiguration.getInstance().getExcludeClass().add(name);
				return classfileBuffer;
			}
			
			/*if (protectionDomain.getCodeSource().getLocation().getPath().contains("test")) {
				return classfileBuffer;
			}*/
		}
		
		if (StringUtil.shouldIncludeClass(name)) {
			//Start the instrumentation here;
			try {				
				//byte[] copyBytes = new byte[classfileBuffer.length];
				//System.arraycopy(classfileBuffer, 0, copyBytes, 0, classfileBuffer.length);
				//ClassReader cr = new ClassReader(name);
				ClassReader cr = new ClassReader(classfileBuffer);
				ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
				//ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
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
				//ex.printStackTrace();
				logger.error("Fail to transform class: " + className);
				logger.error("Message: ", ex);
				GlobalRecorder.registerUntransformedClass(className);
			}
		}
		return classfileBuffer;
	}

	

}
