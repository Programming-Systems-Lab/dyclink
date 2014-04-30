package edu.columbia.cs.psl.kamino;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import edu.columbia.cs.psl.kamino.ControlFlowLoggingClassVisitor;
import edu.columbia.cs.psl.kamino.CallGraph;
import edu.columbia.cs.psl.kamino.MethodInformation;
import edu.columbia.cs.psl.kamino.MiniClassNode;

public class Instrumenter {
	public static int pass_number = 0;
	public static int MAX_SANDBOXES = 2;
	public static boolean IS_ANDROID_INST = Boolean.valueOf(System.getProperty("ANDROID", "false"));
	public static ClassLoader loader;
	public static HashSet<String> interfaces = new HashSet<String>();
	public static CallGraph callgraph = new CallGraph();

	static int nChanges = 0;
	static boolean analysisInvalidated = false;
	static boolean ANALYZE_ONLY;
	static String curPath;
	static HashSet<String> notInterfaces = new HashSet<String>();
	static HashSet<String> annotations = new HashSet<String>();
	static HashSet<String> notAnnotations = new HashSet<String>();

	private static File rootOutputDir;

	static void propogateUp(String owner, String name, String desc, MethodInformation toPropogate) {
		propogateUp(owner, name, desc, toPropogate, new HashSet<String>());
	}

	static void propogateUp(String owner, String name, String desc, MethodInformation toPropogate, HashSet<String> tried) {
		if (tried.contains(owner)) return;
		tried.add(owner);
		if (name.equals("<clinit>")) return;
		MiniClassNode c = callgraph.getClassNode(owner);
		if (!owner.equals(toPropogate.getOwner())) {
			MethodInformation m = callgraph.getMethodNodeIfExists(owner, name, desc);
			if (m != null) {
				boolean wasPure = m.isPure();
				boolean wasCallsTainted = m.callsTaintSourceMethods();
				if (wasPure && !toPropogate.isPure()) {
					m.setPure(false);
					analysisInvalidated = true;
					nChanges++;
				}
				if (!wasCallsTainted && toPropogate.callsTaintSourceMethods()) {
					m.setDoesNotCallTaintedMethods(false);
					m.setCallsTaintedMethods(true);
					analysisInvalidated = true;
					nChanges++;;
				}
			}
		}
		if (c.superName != null && !c.superName.equals(owner)) propogateUp(c.superName, name, desc, toPropogate, tried);
		if (c.interfaces != null) for (String s : c.interfaces)
			propogateUp(s, name, desc, toPropogate, tried);
	}

//	public static void preAnalysis() {
//		File graphDir = new File("pc-graphs");
//		if (!graphDir.exists()) graphDir.mkdir();
//		for (File f : graphDir.listFiles()) {
//			try {
//				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
//				CallGraph g = (CallGraph) ois.readObject();
//				callgraph.addAll(g);
//				ois.close();
//			} catch (Exception x) {
//				x.printStackTrace();
//			}
//		}
//	}

//	public static void finishedAnalysis() {
//		File graphDir = new File("pc-graphs");
//		if (!graphDir.exists()) graphDir.mkdir();
//		System.out.println("Analysis Completed: Beginning Instrumentation Phase");
//	}

	public static boolean isIgnoredClass(String owner) {
		if (IS_ANDROID_INST && !ControlFlowLoggingClassVisitor.IS_RUNTIME_INST) {
			return owner.startsWith("java/lang/Object") || owner.startsWith("java/lang/Number") || owner.startsWith("java/lang/Comparable")
			        || owner.startsWith("java/lang/ref/SoftReference") || owner.startsWith("java/lang/ref/Reference")
			        || owner.startsWith("java/lang/ref/FinalizerReference")
			        //																|| owner.startsWith("java/awt/image/BufferedImage")
			        //																|| owner.equals("java/awt/Image")
			        || owner.startsWith("edu/columbia/cs/psl/phosphor") || owner.startsWith("sun/awt/image/codec/");
		} else return owner.startsWith("java/lang/Object") || owner.startsWith("java/lang/Boolean") || owner.startsWith("java/lang/Character")
		        || owner.startsWith("java/lang/Byte") || owner.startsWith("java/lang/Short")
//					|| owner.startsWith("edu/columbia/cs/psl/microbench")
		        || owner.startsWith("java/lang/Number") || owner.startsWith("java/lang/Comparable")
		        || owner.startsWith("java/lang/ref/SoftReference") || owner.startsWith("java/lang/ref/Reference")
		        //																|| owner.startsWith("java/awt/image/BufferedImage")
		        //																|| owner.equals("java/awt/Image")
		        || owner.startsWith("edu/columbia/cs/psl/phosphor") || owner.startsWith("sun/awt/image/codec/");
	}

	public static byte[] instrumentClass(String path, InputStream is, boolean renameInterfaces) {
		try {
			curPath = path;
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			int nRead;
			byte[] data = new byte[16384];

			while ((nRead = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}

			buffer.flush();
			PreMain.PCLoggingTransformer transformer = new PreMain.PCLoggingTransformer();
			byte[] ret = transformer.transform(Instrumenter.loader, path, null, null, buffer.toByteArray());
			curPath = null;
			return ret;
		} catch (Exception ex) {
			curPath = null;
			ex.printStackTrace();
			return null;
		}
	}

	public static void main(String[] args) {
		System.out.println("Arguments:");
		for (String arg : args) {
			System.out.println(arg);
		}
		System.out.println();
		ControlFlowLoggingClassVisitor.IS_RUNTIME_INST = false;
		ANALYZE_ONLY = true;
//		preAnalysis();
		_main(args);
		System.out.println("Analysis Completed: Beginning Instrumentation Phase");
//		finishedAnalysis();
		ANALYZE_ONLY = false;
		_main(args);
		System.out.println("Instrumentation Completed");
	}

	public static void _main(String[] args) {

		String inputFolder = args[0];

		rootOutputDir = new File(args[1]);
		if (!rootOutputDir.exists()) rootOutputDir.mkdir();

		// Setup the class loader
		final ArrayList<URL> urls = new ArrayList<URL>();
		Path input = FileSystems.getDefault().getPath(inputFolder);
		try {
			if (Files.isDirectory(input))
				Files.walkFileTree(input, new FileVisitor<Path>() {

					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						return FileVisitResult.CONTINUE;
					}

					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						if (file.getFileName().toString().endsWith(".jar")) urls.add(file.toUri().toURL());
						return FileVisitResult.CONTINUE;
					}

					public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}

					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}
				});
			else if (inputFolder.endsWith(".jar")) urls.add(new File(inputFolder).toURI().toURL());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			urls.add(new File(inputFolder).toURI().toURL());
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}

		if (args.length == 3) {
			System.out.println("Using extra classpath file: " + args[2]);
			try {
				Scanner s = new Scanner(new File(args[2]));
				while (s.hasNextLine()) {
					urls.add(new File(s.nextLine()).getCanonicalFile().toURI().toURL());
				}
				s.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		URL[] urlArray = new URL[urls.size()];
		urlArray = urls.toArray(urlArray);
		loader = new URLClassLoader(urlArray, Instrumenter.class.getClassLoader());
		PreMain.bigLoader = loader;

		File f = new File(inputFolder);
		if (!f.exists()) {
			System.err.println("Unable to read path " + inputFolder);
			System.exit(-1);
		}
		if (f.isDirectory())
			processDirectory(f, rootOutputDir, true);
		else if (inputFolder.endsWith(".jar"))
			processJar(f, rootOutputDir);
		else if (inputFolder.endsWith(".class"))
			try {
				processClass(f.getName(), new FileInputStream(f), rootOutputDir);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		else if (inputFolder.endsWith(".zip")) {
			processZip(f, rootOutputDir);
		} else {
			System.err.println("Unknown type for path " + inputFolder);
			System.exit(-1);
		}
	}

	private static void processClass(String name, InputStream is, File outputDir) {
		try {
			FileOutputStream fos = new FileOutputStream(outputDir.getPath() + File.separator + name);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();

			if (!ANALYZE_ONLY) {
				byte[] c = instrumentClass(outputDir.getAbsolutePath(), is, true);
				bos.write(c);
				bos.writeTo(fos);
				fos.close();
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static void processDirectory(File f, File parentOutputDir, boolean isFirstLevel) {
		if (f.getName().equals(".AppleDouble")) return;
		File thisOutputDir;
		if (isFirstLevel) {
			thisOutputDir = parentOutputDir;
		} else {
			thisOutputDir = new File(parentOutputDir.getAbsolutePath() + File.separator + f.getName());
			thisOutputDir.mkdir();
		}
		for (File fi : f.listFiles()) {
			if (fi.isDirectory())
				processDirectory(fi, thisOutputDir, false);
			else if (fi.getName().endsWith(".class"))
				try {
					processClass(fi.getName(), new FileInputStream(fi), thisOutputDir);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			else if (fi.getName().endsWith(".jar"))
				processJar(fi, thisOutputDir);
			else if (fi.getName().endsWith(".zip"))
				processZip(fi, thisOutputDir);
			else {
				File dest = new File(thisOutputDir.getPath() + File.separator + fi.getName());
				FileChannel source = null;
				FileChannel destination = null;

				try {
					source = new FileInputStream(fi).getChannel();
					destination = new FileOutputStream(dest).getChannel();
					destination.transferFrom(source, 0, source.size());
				} catch (Exception ex) {
					System.err.println("error copying file " + fi);
					ex.printStackTrace();
				} finally {
					if (source != null) {
						try {
							source.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					if (destination != null) {
						try {
							destination.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}

			}
		}

	}

	public static void processJar(File f, File outputDir) {
		try {
			JarFile jar = new JarFile(f);
			JarOutputStream jos = null;
			jos = new JarOutputStream(new FileOutputStream(outputDir.getPath() + File.separator + f.getName()));
			Enumeration<JarEntry> entries = jar.entries();
			while (entries.hasMoreElements()) {
				JarEntry e = entries.nextElement();
				if (e.getName().endsWith(".class")) {
					{
						if (!ANALYZE_ONLY) {
							try {
								JarEntry outEntry = new JarEntry(e.getName());
								jos.putNextEntry(outEntry);
								byte[] clazz = instrumentClass(f.getAbsolutePath(), jar.getInputStream(e), true);
								if (clazz == null) {
									System.out.println("Failed to instrument " + e.getName() + " in " + f.getName());
									InputStream is = jar.getInputStream(e);
									byte[] buffer = new byte[1024];
									while (true) {
										int count = is.read(buffer);
										if (count == -1) break;
										jos.write(buffer, 0, count);
									}
								} else {
									jos.write(clazz);
								}
								jos.closeEntry();
							} catch (ZipException ex) {
								ex.printStackTrace();
								continue;
							}
						}
					}

				} else {
					JarEntry outEntry = new JarEntry(e.getName());
					if (e.isDirectory()) {
						jos.putNextEntry(outEntry);
						jos.closeEntry();
					} else if (e.getName().startsWith("META-INF") && (e.getName().endsWith(".SF") || e.getName().endsWith(".RSA"))) {
						// don't copy this
					} else if (e.getName().equals("META-INF/MANIFEST.MF")) {
						Scanner s = new Scanner(jar.getInputStream(e));
						jos.putNextEntry(outEntry);

						String curPair = "";
						while (s.hasNextLine()) {
							String line = s.nextLine();
							if (line.equals("")) {
								curPair += "\n";
								if (!curPair.contains("SHA1-Digest:")) jos.write(curPair.getBytes());
								curPair = "";
							} else {
								curPair += line + "\n";
							}
						}
						s.close();
						jos.closeEntry();
					} else {
						try {
							jos.putNextEntry(outEntry);
							InputStream is = jar.getInputStream(e);
							byte[] buffer = new byte[1024];
							while (true) {
								int count = is.read(buffer);
								if (count == -1) break;
								jos.write(buffer, 0, count);
							}
							jos.closeEntry();
						} catch (ZipException ex) {
							ex.printStackTrace();
							System.out.println("Ignoring above warning from improper source zip...");
						}
					}

				}

			}
			if (jos != null) {
				jos.close();
			}
			jar.close();
		} catch (Exception e) {
			System.err.println("Unable to process jar: " + f.getAbsolutePath());
			e.printStackTrace();
			File dest = new File(outputDir.getPath() + File.separator + f.getName());
			FileChannel source = null;
			FileChannel destination = null;

			try {
				source = new FileInputStream(f).getChannel();
				destination = new FileOutputStream(dest).getChannel();
				destination.transferFrom(source, 0, source.size());
			} catch (Exception ex) {
				System.err.println("Unable to copy file: " + f.getAbsolutePath());
				ex.printStackTrace();
			} finally {
				if (source != null) {
					try {
						source.close();
					} catch (IOException e2) {
						e2.printStackTrace();
					}
				}
				if (destination != null) {
					try {
						destination.close();
					} catch (IOException e2) {
						e2.printStackTrace();
					}
				}
			}
		}

	}

	private static void processZip(File f, File outputDir) {
		try {
			ZipFile zip = new ZipFile(f);
			ZipOutputStream zos = null;
			zos = new ZipOutputStream(new FileOutputStream(outputDir.getPath() + File.separator + f.getName()));
			Enumeration<? extends ZipEntry> entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry e = entries.nextElement();

				if (e.getName().endsWith(".class")) {
					{
						if (!ANALYZE_ONLY) {
							ZipEntry outEntry = new ZipEntry(e.getName());
							zos.putNextEntry(outEntry);

							byte[] clazz = instrumentClass(f.getAbsolutePath(), zip.getInputStream(e), true);
							if (clazz == null) {
								InputStream is = zip.getInputStream(e);
								byte[] buffer = new byte[1024];
								while (true) {
									int count = is.read(buffer);
									if (count == -1) break;
									zos.write(buffer, 0, count);
								}
							} else zos.write(clazz);
							zos.closeEntry();
						}
					}

				} else if (e.getName().endsWith(".jar")) {
					ZipEntry outEntry = new ZipEntry(e.getName());
					File tmp = new File("/tmp/classfile");
					if (tmp.exists()) tmp.delete();
					FileOutputStream fos = new FileOutputStream(tmp);
					byte buf[] = new byte[1024];
					int len;
					InputStream is = zip.getInputStream(e);
					while ((len = is.read(buf)) > 0) {
						fos.write(buf, 0, len);
					}
					is.close();
					fos.close();
					File tmp2 = new File("tmp2");
					if (!tmp2.exists()) tmp2.mkdir();
					processJar(tmp, new File("tmp2"));

					zos.putNextEntry(outEntry);
					is = new FileInputStream("tmp2/classfile");
					byte[] buffer = new byte[1024];
					while (true) {
						int count = is.read(buffer);
						if (count == -1) break;
						zos.write(buffer, 0, count);
					}
					is.close();
					zos.closeEntry();
				} else {
					ZipEntry outEntry = new ZipEntry(e.getName());
					if (e.isDirectory()) {
						zos.putNextEntry(outEntry);
						zos.closeEntry();
					} else if (e.getName().startsWith("META-INF") && (e.getName().endsWith(".SF") || e.getName().endsWith(".RSA"))) {
						// don't copy this
					} else if (e.getName().equals("META-INF/MANIFEST.MF")) {
						Scanner s = new Scanner(zip.getInputStream(e));
						zos.putNextEntry(outEntry);

						String curPair = "";
						while (s.hasNextLine()) {
							String line = s.nextLine();
							if (line.equals("")) {
								curPair += "\n";
								if (!curPair.contains("SHA1-Digest:")) zos.write(curPair.getBytes());
								curPair = "";
							} else {
								curPair += line + "\n";
							}
						}
						s.close();
						zos.write("\n".getBytes());
						zos.closeEntry();
					} else {
						zos.putNextEntry(outEntry);
						InputStream is = zip.getInputStream(e);
						byte[] buffer = new byte[1024];
						while (true) {
							int count = is.read(buffer);
							if (count == -1) break;
							zos.write(buffer, 0, count);
						}
						zos.closeEntry();
					}
				}

			}
			zos.close();
			zip.close();
		} catch (Exception e) {
			System.err.println("Unable to process zip: " + f.getAbsolutePath());
			e.printStackTrace();
			File dest = new File(outputDir.getPath() + File.separator + f.getName());
			FileChannel source = null;
			FileChannel destination = null;

			try {
				source = new FileInputStream(f).getChannel();
				destination = new FileOutputStream(dest).getChannel();
				destination.transferFrom(source, 0, source.size());
			} catch (Exception ex) {
				System.err.println("Unable to copy zip: " + f.getAbsolutePath());
				ex.printStackTrace();
			} finally {
				if (source != null) {
					try {
						source.close();
					} catch (IOException e2) {
						e2.printStackTrace();
					}
				}
				if (destination != null) {
					try {
						destination.close();
					} catch (IOException e2) {
						e2.printStackTrace();
					}
				}
			}
		}
	}
}
