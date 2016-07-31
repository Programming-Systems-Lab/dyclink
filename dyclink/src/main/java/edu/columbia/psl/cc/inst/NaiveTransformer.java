package edu.columbia.psl.cc.inst;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.JSRInlinerAdapter;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.util.StringUtil;

public class NaiveTransformer implements ClassFileTransformer{
	
	@Override
	public byte[] transform(ClassLoader loader, 
			String className,
			Class<?> classBeingRedefined, 
			ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		String name = className.replace("/", ".");
		
		if (name.contains("site$py")) {
			System.out.println("Target class: " + name);
			
			if (protectionDomain != null) {
				String codeLocation = protectionDomain.getCodeSource().getLocation().getPath();
				System.out.println("Code location: " + codeLocation);
			}
		}
		
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
			
			if (codeLocation.matches(".*dyclink.*.jar")) {
				return classfileBuffer;
			}
		}
		
		if (StringUtil.shouldIncludeClass(name)) {
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
				
				boolean target = false;
				//if (className.contains("site$py")) {
				//if (className.equals("org/python/core/PyObject")) {
				if (!className.contains("dacapo") && !className.contains("Harness")) {
					target = true;
				} else {
					target = false;
				}
				final boolean decision = target;
				
				final String finalClassName = className;
				
				boolean begin = false;
				if (finalClassName.contains("site$py") 
						/*&& owner.equals("org/python/core/PyObject") 
						&& name.equals("__call__") 
						&& desc.equals("(Lorg/python/core/PyObject;)Lorg/python/core/PyObject;")*/) {
					begin = true;
				}
				
				final boolean shouldBegin = begin;
				ClassVisitor cv = new ClassVisitor(Opcodes.ASM5, cw) {
															
					@Override
					public MethodVisitor visitMethod(int access, final String name, String desc, String signature, String[] exceptions) {
						MethodVisitor mv = this.cv.visitMethod(access, name, desc, signature, exceptions);
						if (decision) {
							final String current = finalClassName + " " + name + " " + desc;
							final String message = "***Caller: " + current;
							MethodVisitor myMv = new MethodVisitor(Opcodes.ASM5, mv) {
								
								public void copy(String msg) {
									this.mv.visitInsn(Opcodes.DUP);
									this.mv.visitLdcInsn(current + " " + msg);
									this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "edu/columbia/psl/cc/util/CumuMethodRecorder", "showClass", "(Ljava/lang/Object;Ljava/lang/String;)V", false);
								}
								
								@Override
								public void visitCode() {
									this.mv.visitLdcInsn(message);
									this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "edu/columbia/psl/cc/util/CumuMethodRecorder", "get", "(Ljava/lang/String;)V", false);
									
									this.mv.visitCode();
									
									if (name.equals("f$0")) {
										this.mv.visitInsn(Opcodes.ICONST_1);
										this.mv.visitFieldInsn(Opcodes.PUTSTATIC, "edu/columbia/psl/cc/util/CumuMethodRecorder", "SHOW", "Z");
										
										System.out.println("Start: " + finalClassName + " " + name);
									}
								}
								
								@Override
								public void visitFieldInsn(int opcode,
				                           String owner,
				                           String name,
				                           String desc) {
									this.mv.visitFieldInsn(opcode, owner, name, desc);
									
									if (opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC) {
										Type retType = Type.getReturnType(desc);
										if (retType.getSort() == Type.OBJECT) {
											String msg = opcode + " " + owner + " " + name + " " + desc;
											this.copy(msg);
										}
									}
								}
								
								
								@Override
								public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
									String calleeMsg = "---Called: " + owner + " " + name + " " + desc;
									this.mv.visitLdcInsn(calleeMsg);
									this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "edu/columbia/psl/cc/util/CumuMethodRecorder", "get", "(Ljava/lang/String;)V", false);
									
									this.mv.visitMethodInsn(opcode, owner, name, desc, itf);
									
									Type retType = Type.getReturnType(desc);
									if (retType.getSort() == Type.OBJECT) {
										String msg = opcode + " "+ owner + " " + name + " " + desc;
										this.copy(msg);
									}
								}
								
								@Override
								public void visitLdcInsn(Object cst) {
									this.mv.visitLdcInsn(cst);
									
									if (cst instanceof Type) {
										int sort = ((Type) cst).getSort();
										
										if (sort == Type.OBJECT) {
											String msg = "ldc";
											this.copy(msg);
										}
									}
								}
								
								@Override
								public void visitVarInsn(int opcode, int var) {
									this.mv.visitVarInsn(opcode, var);
									
									if (opcode == Opcodes.ALOAD && !name.equals("<init>") && var != 0) {
										String msg = "ALOAD " + var;
										this.copy(msg);
									}
								}
								
								
							};
							
							return myMv;
						} else {
							return mv;
						}
						
					}
				};
				
				analysisReader.accept(cv, ClassReader.EXPAND_FRAMES);
				
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
				ex.printStackTrace();
			}
		}
		
		return classfileBuffer;
	}

}
