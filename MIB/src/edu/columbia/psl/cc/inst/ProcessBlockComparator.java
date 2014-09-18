package edu.columbia.psl.cc.inst;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.CheckClassAdapter;

import edu.columbia.psl.cc.annot.analyzeClass;
import edu.columbia.psl.cc.annot.extractTemplate;
import edu.columbia.psl.cc.annot.testTemplate;
import edu.columbia.psl.cc.datastruct.VarPairPool;
import edu.columbia.psl.cc.datastruct.VarPool;
import edu.columbia.psl.cc.pojo.CodeTemplate;
import edu.columbia.psl.cc.pojo.Var;
import edu.columbia.psl.cc.util.GraphUtil;
import edu.columbia.psl.cc.util.GsonManager;
import edu.columbia.psl.cc.util.LevenshteinDistance;
import edu.columbia.psl.cc.util.ShortestPathKernel;
import edu.columbia.psl.cc.util.SimilarityFlooding;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

public class ProcessBlockComparator {
	
	private static String classAnnot = Type.getType(analyzeClass.class).getDescriptor();
	
	private static String templateAnnot = Type.getType(extractTemplate.class).getDescriptor();
	
	private static String testAnnot = Type.getType(testTemplate.class).getDescriptor();
	
	private static HashSet<String> getClassNames() {
		HashSet<String> classNames = new HashSet<String>();
		classNames.add("edu.columbia.psl.cc.test.TemplateMethod");
		//classNames.add("edu.columbia.psl.cc.test.TemplateObjMethod");
		//classNames.add("edu.columbia.psl.cc.AESEngineManual");
		//classNames.add("edu.columbia.psl.cc.Rijndael");
		//classNames.add("edu.columbia.psl.cc.test.JohnExample");
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
		HashMap<String, VarPool> templateMap = new HashMap<String, VarPool>();
		for (File f: tempDir.listFiles(filter)) {
			//Probably not the best way, how to make it generic?
			CodeTemplate ct = GsonManager.readJson(f, type);
			ct.reconstructVars();
			HashSet<Var> vSet = ct.getVars();
			VarPool vp = new VarPool(vSet);				
			simCalculator.addData(f.getName(), ct.getCharSequence(), true);
			templateMap.put(f.getName(), vp);
		}
		
		HashMap<String, VarPool> testMap = new HashMap<String, VarPool>();
		for (File f: testDir.listFiles(filter)) {
			CodeTemplate ct = GsonManager.readJson(f, type);
			ct.reconstructVars();
			HashSet<Var> vSet = ct.getVars();
			VarPool vp = new VarPool(vSet);
			simCalculator.addData(f.getName(), ct.getCharSequence(), false);
			testMap.put(f.getName(), vp);
		}
		simCalculator.generateResult();
		
		//To use ShortestPathKernel, need to make sure all template's size is the same
		int maxSize = 0;
		for (String templateName: templateMap.keySet()) {
			VarPool templatePool = templateMap.get(templateName);
			if (templatePool.size() > maxSize) {
				maxSize = templatePool.size();
			}
		}
		
		for (String templateName: templateMap.keySet()) {
			VarPool templatePool = templateMap.get(templateName);
			int diff = maxSize - templatePool.size();
			if (diff > 0) {
				VarPool ret = ShortestPathKernel.addFakeVar(templatePool, diff);
				templateMap.put(templateName, ret);
			}
		}
		
		for (String templateName: templateMap.keySet()) {
			VarPool templatePool = templateMap.get(templateName);
			
			for (String testName: testMap.keySet()) {
				VarPool testPool = testMap.get(testName);
				
				/*SimilarityFlooding sf = new SimilarityFlooding();
				sf.setMaxrRound(10);
				sf.setDelta(0.1);
				GraphUtil.constructVarPairPool(sf.getVarPairPool(), templatePool, testPool);
				sf.convergeCalculation();
				sf.getMarried();
				System.out.println(templateName + " vs " + testName + " " + sf.getSimilaritySum());*/
				
				ShortestPathKernel spk = new ShortestPathKernel();
				int[][] templateTable = spk.constructCostTable(templatePool);
				int[][] testTable = spk.constructCostTable(testPool);
				int kernelScore = spk.scoreShortestPaths(templateTable, testTable);
				System.out.println(templateName + " vs " + testName + " " + kernelScore);
			}
		}
	}

}
