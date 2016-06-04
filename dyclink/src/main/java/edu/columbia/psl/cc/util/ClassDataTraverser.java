package edu.columbia.psl.cc.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.cli.Option;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;

public class ClassDataTraverser {
	public static ClassLoader loader;

	static String curPath;

	static int n = 0;
	
	//This jar requires swt.jar, which is not a default jar in jre
	//plutin.jar is not default jar used by users. 
	//It also got conflicts with jfxrt.jar (netscape.javascript.JSObject, netscape.javascript.JSException)
	static String excludeSwt = "jfxswt.jar";
	
	static String pluginJar = "plugin.jar";

	public static byte[] cleanClass(InputStream is) {
		try {
			n++;
			if (n % 1000 == 0)
				System.out.println("Processed: " + n);
			
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			int nRead;
			byte[] data = new byte[16384];

			while ((nRead = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}

			buffer.flush();
			//VMVMClassFileTransformer transformer = new VMVMClassFileTransformer();
			//FlowCloneTransformer transformer = new FlowCloneTransformer();
			ClassReader cr = new ClassReader(buffer.toByteArray());
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			cr.accept(new ClassVisitor(Opcodes.ASM5, cw) {
				@Override
				public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
					// TODO Auto-generated method stub
					return new JSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions), access, name, desc, signature, exceptions);
				}
			}, ClassReader.EXPAND_FRAMES);
			
			//byte[] ret = transformer.transform(Instrumenter.loader, cr.getClassName(), null, null, buffer.toByteArray());
			byte[] ret = cw.toByteArray();
			return ret;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	static Option help = new Option("help", "print this message");

	public static void main(String[] args) {
		String needToProcess = args[0];
		System.out.println("Input path: " + needToProcess);
		List<byte[]> datas = new ArrayList<byte[]>();
		collectDir(needToProcess, datas);
		System.out.println("Total classes: " + datas.size());
	}

	static File rootOutputDir;

	public static void collectDir(String inputFolder, List<byte[]> datas) {
		//System.out.println(inputFolder);
		File f = new File(inputFolder);
		if (!f.exists()) {
			System.err.println("Unable to read path " + inputFolder);
			System.exit(-1);
		}
		
		if (f.isDirectory()) {
			processDirectory(f, datas);
		} else if (inputFolder.endsWith(".jar") || inputFolder.endsWith(".war")) {
			processJar(f, datas);
		} else if (inputFolder.endsWith(".class"))
			try {
				processClass(new FileInputStream(f), datas);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		else if (inputFolder.endsWith(".zip")) {
			processZip(f, datas);
		} else {
			System.err.println("Unknown type for path " + inputFolder);
			System.exit(-1);
		}
	}

	private static void processClass(InputStream is, List<byte[]> classDatas) {

		try {
			byte[] c = cleanClass(is);
			classDatas.add(c);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void processDirectory(File f, List<byte[]> datas) {
		for (File fi : f.listFiles()) {
			if (fi.isDirectory()) {
				processDirectory(fi, datas);
			} else if (fi.getName().endsWith(".class")) {
				try {
					processClass(new FileInputStream(fi), datas);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			} else if (fi.getName().endsWith(".jar") || fi.getName().endsWith(".war")) {
				processJar(fi, datas);
			} else if (fi.getName().endsWith(".zip")) {
				processZip(fi, datas);
			} else {
				System.out.println("Irrelevant file: " + fi.getAbsolutePath());
			}
		}

	}

	public static void processJar(File f, List<byte[]> datas) {
		//System.out.println("Get jar: " + f.getPath());
		if (f.getName().equals(excludeSwt)) {
			//System.out.println("Exlude jar: " + f.getName());
			return ;
		}
		
		try {
			JarFile jar = new JarFile(f);
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry e = entries.nextElement();
				if (e.getName().endsWith(".class")) {
					
					if (f.getName().equals(pluginJar)) {
						if (e.getName().equals("netscape/javascript/JSObject.class") 
								|| e.getName().equals("netscape/javascript/JSException.class")) {
							System.out.println("Exclude duplicated class from plugin.jar: " + e.getName());
							continue ;
						}
					}
					
					try {
						byte[] clazz = cleanClass(jar.getInputStream(e));
						if (clazz == null) {
							System.out.println("Failed to instrument " + e.getName() + " in " + f.getName());
						} else {
							datas.add(clazz);
						}
					} catch (ZipException ex) {
						ex.printStackTrace();
						continue;
					}
				} 
			}
			jar.close();
		} catch (Exception e) {
			System.err.println("Unable to process jar: " + f.getAbsolutePath());
			e.printStackTrace();
		}

	}

	private static void processZip(File f, List<byte[]> datas) {
		try {
			ZipFile zip = new ZipFile(f);
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry e = entries.nextElement();

				if (e.getName().endsWith(".class")) {
					{
						byte[] clazz = cleanClass(zip.getInputStream(e));
						datas.add(clazz);
					}
				} else if (e.getName().endsWith(".jar")) {
					File tmpDir = new File("tmp");
					if (!tmpDir.exists()) {
						tmpDir.mkdir();
					}
					
					File tmp = new File("tmp/classfile");
					if (tmp.exists())
						tmp.delete();
					FileOutputStream fos = new FileOutputStream(tmp);
					byte buf[] = new byte[1024];
					int len;
					InputStream is = zip.getInputStream(e);
					while ((len = is.read(buf)) > 0) {
						fos.write(buf, 0, len);
					}
					is.close();
					fos.close();
					processJar(tmp, datas);
					//tmp.delete();
				} 
			}
		} catch (Exception e) {
			System.err.println("Unable to process zip: " + f.getAbsolutePath());
			e.printStackTrace();
		}
	}
	
	public static List<byte[]> filter(List<byte[]> classDatas, String name) {
		List<byte[]> ret = new ArrayList<byte[]>();
		for (byte[] classData: classDatas) {
			byte[] copy = Arrays.copyOf(classData, classData.length);
			
			ClassReader cr = new ClassReader(copy);
			if (cr.getClassName().equals(name)) {
				ret.add(classData);
				break ;
			}
		}
		return ret;
	}
}
