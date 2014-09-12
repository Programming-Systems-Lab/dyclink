package edu.columbia.psl.cc.inst;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;

import edu.columbia.psl.cc.annot.analyzeClass;
import edu.columbia.psl.cc.annot.extractTemplate;
import edu.columbia.psl.cc.annot.testTemplate;
import edu.columbia.psl.cc.pojo.CodeTemplate;
import edu.columbia.psl.cc.util.GsonManager;
import edu.columbia.psl.cc.util.LevenshteinDistance;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

public class ProcessBlockComparator {
	
	private static String classAnnot = Type.getType(analyzeClass.class).getDescriptor();
	
	private static String templateAnnot = Type.getType(extractTemplate.class).getDescriptor();
	
	private static String testAnnot = Type.getType(testTemplate.class).getDescriptor();
	
	private static HashSet<String> getClassNames() {
		HashSet<String> classNames = new HashSet<String>();
		//classNames.add("edu.columbia.psl.cc.test.TemplateObjMethod");
		//classNames.add("edu.columbia.psl.cc.AESEngineManual");
		//classNames.add("edu.columbia.psl.cc.Rijndael");
		classNames.add("edu.columbia.psl.cc.test.JohnExample");
		return classNames;
	}
	
	public static void main(String[] args) {		
		//Clean the dir
		GsonManager.cleanDirs();
				
		//Read bytecode from both class
		for (String className: getClassNames()) {
			try {
				ClassReader cr = new ClassReader(className);
				ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
				ClassMiner cm = new ClassMiner(new CheckClassAdapter(cw, false), 
						className.replace(".", "/"), classAnnot, templateAnnot, testAnnot);
				cr.accept(cm, 0);
				//byte[] toWrite = cw.toByteArray();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
		//Calculate similarity
		LevenshteinDistance simCalculator = new LevenshteinDistance();
		
		File tempDir = new File("./template");
		File testDir = new File("./test");
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				// TODO Auto-generated method stub
				return name.toLowerCase().endsWith(".json");
			}
		};
		
		//For generic purpose
		CodeTemplate type = new CodeTemplate();
		for (File f: tempDir.listFiles(filter)) {
			//Probably not the best way, how to make it generic?
			CodeTemplate ct = GsonManager.readJson(f, type);
			simCalculator.addData(f.getName(), ct.getCharSequence(), true);
		}
		
		for (File f: testDir.listFiles(filter)) {
			CodeTemplate ct = GsonManager.readJson(f, type);
			simCalculator.addData(f.getName(), ct.getCharSequence(), false);
		}
		
		simCalculator.generateResult();
	}

}
