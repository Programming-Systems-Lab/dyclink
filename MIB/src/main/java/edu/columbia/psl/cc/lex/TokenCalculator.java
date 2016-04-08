package edu.columbia.psl.cc.lex;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.commons.InstructionAdapter;

import edu.columbia.psl.cc.lex.JavaParser.CompilationUnitContext;

public class TokenCalculator {
	
	private static FilenameFilter nameFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.toLowerCase().endsWith(".java");
		}
	};
	
	/*public static void instCalculator(File f) {
		MethodVisitor instCalculator = new MethodVisitor(Opcodes.ASM4) {
			HashSet<Integer> lines = new HashSet<Integer>();
			int insts = 0;
			
			public int getLines() {
				return lines.size();
			}
			
			public int getInsts() {
				return insts;
			}
			
			@Override
			public void visitLineNumber(int line, Label label) {
				lines.add(line);
			}
			
			@Override
			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				insts++;
			}
			
			@Override
			public void visitIincInsn(int var, int increment) {
				insts++;
			}
			
			@Override
			public void visitInsn(int opcode) {
				insts++;
			}
			
			@Override
			public void visitIntInsn(int ocpdoe, int operand) {
				insts++;
			}
			
			@Override
			public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object...bsmArgs) {
				insts++;
			}
			
			@Override
			public void visitJumpInsn(int opcode, Label label) {
				insts++;
			}
			
			@Override
			public void visitLdcInsn(Object cst) {
				insts++;
			}
			
			@Override
			public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
				insts++;
			}
			
			@Override
			public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
				insts++;
			}
			
			@Override
			public void visitMethodInsn(int opcode, String owner, String name, String desc) {
				insts++;
			}
			
			@Override
			public void visitMultiANewArrayInsn(String desc, int dims) {
				insts++;
			}
			
			@Override 
			public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
				insts++;
			}
			
			@Override
			public void visitTypeInsn(int opcode, String type) {
				insts++;
			}
			
			@Override
			public void visitVarInsn(int opcode, int var) {
				insts++;
			}
		};
		
		ClassVisitor cv = new ClassVisitor(Opcodes.ASM4) {
			@Override
			public MethodVisitor visitMethod(int access, 
					String name, 
					String desc, 
					String signature, 
					String[] exceptions) {
			}
			
		};
	}*/
	
	public static int[] instCalculator(File f) throws Exception {
		/*URL classUrl = f.toURI().toURL();
		System.out.println("Check url: " + classUrl);
		URL[] urls = new URL[]{classUrl};
		URLClassLoader classLoader = new URLClassLoader(urls);
		InputStream in = classLoader.getResourceAsStream(classUrl.getPath());
		System.out.println("Check in: " + in.available());*/
		
		Path path = Paths.get(f.toURI());
		//System.out.println("Check path: " + path);
		byte[] classData = Files.readAllBytes(path);
		//System.out.println("Check class data length: " + classData.length);
		ClassReader cr = new ClassReader(classData);
		List<Integer> instsRecorder = new ArrayList<Integer>();
		TreeSet<Integer> lineTracer = new TreeSet<Integer>();
		
		MethodInstCalculator instCalculator = new MethodInstCalculator(Opcodes.ASM5, instsRecorder, lineTracer);
		cr.accept(instCalculator, 0);
		//System.out.println("Instructions: " + instsRecorder.size());
		//System.out.println("Lines: " + lineTracer.size());
		//System.out.println("Check lines: " + lineTracer);
		int[] ret = {instsRecorder.size(), lineTracer.size()};
		//System.out.println("Inst calculator check line trace: " + lineTracer);
		return ret;
	}
	
	public static int[] tokenCalculator(File f) throws IOException {
		CharStream cs = new ANTLRFileStream(f.getAbsolutePath());
		//CharStream cs = new ANTLRInputStream(testString);
		JavaLexer lexer = new JavaLexer(cs);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		//TokenStreamPassThrough filter = new TokenStreamPassThrough(lexer);
		
		JavaParser parser = new JavaParser(tokens);
		CompilationUnitContext result = parser.compilationUnit();
		//System.out.println(result.getText());
		//System.out.println("Token number: " + tokens.getTokens().size());
		TreeSet<Integer> lineTracer = new TreeSet<Integer>();
		for (Token tok: tokens.getTokens()) {
			//System.out.println(tok.getText());
			//System.out.println(tok.getLine());
			lineTracer.add(tok.getLine());
		}
		int tokNum = tokens.getTokens().size();
		//double avgNum = (double)tokNum/lineTracer.size();
		//return avgNum;
		int[] ret = {tokNum, lineTracer.size()};
		//System.out.println("Token calculator check line trace: " + lineTracer);
		return ret;
	}
	
	public static void retrieveAllFiles(File f, List<File> recorder, String desiredExt) throws IOException {
		if (!f.exists()) {
			System.out.println("File " + f.getAbsolutePath() + " does not exist");
			return ;
		}
		
		if (f.isDirectory()) {
			//File[] files = f.listFiles(nameFilter);
			File[] files = f.listFiles();
			for (File newF: files) {
				retrieveAllFiles(newF, recorder, desiredExt);
			}
		} else {
			//String ext = Files.probeContentType(f.toPath());
			String fullPath = f.getAbsolutePath();
			int dot = fullPath.lastIndexOf(".");
		    String ext = fullPath.substring(dot + 1);
			//System.out.println("Check file: " + f.toPath());
			//System.out.println("Check ext: " + ext);
			
			if (ext == null) {
				System.exit(1);
			}
			
			if (ext.equalsIgnoreCase(desiredExt)) {
				recorder.add(f);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		List<String> dirs = new ArrayList<String>();
		dirs.add("./explibs/ejml_all/src");
		dirs.add("./explibs/commons-math3-3.2-src/src/main/java");
		dirs.add("./explibs/Jama/src");
		dirs.add("./explibs/colt/src");
		dirs.add("./explibs/java-codecs/src/main/java");
		dirs.add("./explibs/ojalgo/src");
		dirs.add("./explibs/plexus-cipher/src/main/java");
		List<File> recorder = new ArrayList<File>();
		
		String ext = "java";
		for (String dir: dirs) {
			File dirFile = new File(dir);
			retrieveAllFiles(dirFile, recorder, ext);
		}
		
		int totalTokens = 0;
		int totalLines = 0;
		for (File f: recorder) {
			int[] tokenInfo = tokenCalculator(f);
			totalTokens += tokenInfo[0];
			totalLines += tokenInfo[1];
		}
		
		System.out.println("Total tokens: " + totalTokens);
		System.out.println("Total lines: " + totalLines);
		//System.out.println("Avg tokens: " + avgTokens);
		
		List<String> classDirs = new ArrayList<String>();
		classDirs.add("./explibs/ejml_all/bin");
		classDirs.add("./explibs/commons-math3-3.2-src/bin");
		classDirs.add("./explibs/Jama/bin");
		classDirs.add("./explibs/colt/bin");
		classDirs.add("./explibs/java-codecs/bin");
		classDirs.add("./explibs/ojalgo/bin");
		classDirs.add("./explibs/plexus-cipher/bin");
		List<File> classRecorder = new ArrayList<File>();
		
		String classExt = "class";
		for (String classDir: classDirs) {
			File dirFile = new File(classDir);
			retrieveAllFiles(dirFile, classRecorder, classExt);
		}
		//System.out.println("Class recorder size: " + classRecorder.size());
		
		int totalInsts = 0;
		int totalInstsLines = 0;
		for (File f: classRecorder) {
			int[] instInfo = instCalculator(f);
			totalInsts += instInfo[0];
			totalInstsLines += instInfo[1];
		}
		
		System.out.println("Total insts: " + totalInsts);
		System.out.println("Total lines: " + totalInstsLines);
		
		double avgLine = ((double)(totalLines + totalInstsLines))/2;
		System.out.println("Avg lines: " + avgLine);
		
		double avgTokens = ((double)totalTokens)/avgLine;
		double avgInsts = ((double)totalInsts)/avgLine;
		
		System.out.println("Avg tokens: " + avgTokens);
		System.out.println("Avg insts: " + avgInsts);
	}
	
	public static class MethodInstCalculator extends ClassVisitor {
		
		public List<Integer> insts;
		
		public Set<Integer> lineTracer;
		
		public MethodInstCalculator(int api, ClassVisitor cv, List<Integer> recorder, Set<Integer> lineTracer) {
			super(api, cv);
			this.insts = recorder;
			this.lineTracer = lineTracer;
		}
		
		public MethodInstCalculator(int api, List<Integer> recorder, Set<Integer> lineTracer) {
			super(api);
			this.insts = recorder;
			this.lineTracer = lineTracer;
		}
		
		@Override
		public MethodVisitor visitMethod(int access, 
				String name, String desc, String signature, String[] exceptions) {
			MethodVisitor oriMv = new MethodVisitor(Opcodes.ASM5){};
			
			//System.out.println(name + desc);
			InstructionAdapter instMv = new InstructionAdapter(oriMv) {
				@Override
				public void visitLineNumber(int linenumber, Label label) {
					lineTracer.add(linenumber);
				}
				
				@Override
				public void visitInsn(int opcode) {
					//System.out.println(opcode);
					insts.add(opcode);
					super.visitInsn(opcode);
				}
				
				@Override
				public void visitFieldInsn(int opcode, String owner, String name, String desc) {
					//System.out.println(opcode);
					insts.add(opcode);
					super.visitFieldInsn(opcode, owner, name, desc);
				}
				
				@Override
				public void visitIincInsn(int var, int increment) {
					//System.out.println(Opcodes.IINC);
					insts.add(Opcodes.IINC);
					super.visitIincInsn(var, increment);
				}
				
				@Override
				public void visitIntInsn(int opcode, int operand) {
					//System.out.println(opcode);
					insts.add(opcode);
					super.visitIntInsn(opcode, operand);
				}
				
				@Override
				public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object...bsmArgs) {
					//System.out.println("Got a dynamic!");
					insts.add(Opcodes.INVOKEDYNAMIC);
					super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
					System.exit(1);
				}
				
				@Override
				public void visitJumpInsn(int opcode, Label label) {
					//System.out.println(opcode);
					insts.add(opcode);
					super.visitJumpInsn(opcode, label);
				}
				
				@Override
				public void visitLdcInsn(Object cst) {
					//System.out.println(Opcodes.LDC);
					insts.add(Opcodes.LDC);
					super.visitLdcInsn(cst);
				}
				
				@Override
				public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
					//System.out.println(Opcodes.LOOKUPSWITCH);
					insts.add(Opcodes.LOOKUPSWITCH);
					super.visitLookupSwitchInsn(dflt, keys, labels);
				}
				
				@Override
				public void visitMethodInsn(int opcode, String owner, String name, String desc) {
					//System.out.println(opcode);
					insts.add(opcode);
					super.visitMethodInsn(opcode, owner, name, desc);
				}
				
				@Override
				public void visitMultiANewArrayInsn(String desc, int dims) {
					//System.out.println(Opcodes.MULTIANEWARRAY);
					insts.add(Opcodes.MULTIANEWARRAY);
					super.visitMultiANewArrayInsn(desc, dims);
				}
				
				@Override 
				public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
					//System.out.println(Opcodes.TABLESWITCH);
					insts.add(Opcodes.TABLESWITCH);
					super.visitTableSwitchInsn(min, max, dflt, labels);
				}
				
				@Override
				public void visitTypeInsn(int opcode, String type) {
					//System.out.println(opcode);
					insts.add(opcode);
					super.visitTypeInsn(opcode, type);
				}
				
				@Override
				public void visitVarInsn(int opcode, int var) {
					//System.out.println(opcode);
					insts.add(opcode);
					super.visitVarInsn(opcode, var);
				}
			};
			return instMv;
		}
	}

}
