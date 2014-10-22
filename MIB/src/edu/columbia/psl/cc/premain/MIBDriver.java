package edu.columbia.psl.cc.premain;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;

import java.util.TreeMap;
import java.util.HashMap;
import java.util.TreeSet;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.StaticRep;
import edu.columbia.psl.cc.util.Analyzer;
import edu.columbia.psl.cc.util.DynamicGraphAnalyzer;
import edu.columbia.psl.cc.util.GsonManager;
import edu.columbia.psl.cc.util.StaticBytecodeCatAnalyzer;
import edu.columbia.psl.cc.util.TemplateLoader;

public class MIBDriver {
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//Clean directory
		//GsonManager.cleanDirs();
		
		String className = args[0];
		String[] newArgs = new String[args.length - 1];
		
		for (int i = 1; i < args.length; i++) {
			newArgs[i] = args[i];
		}
		
		try {
			Class targetClass = Class.forName(className);
			System.out.println("Confirm class: " + targetClass);
			
			Method mainMethod = targetClass.getMethod("main", String[].class);
			mainMethod.invoke(null, (Object)newArgs);
			
			//Load templates for each analysis
			File templateDir = new File(MIBConfiguration.getTemplateDir());
			File testDir = new File(MIBConfiguration.getTestDir());
			TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
			HashMap<String, GraphTemplate> templates = TemplateLoader.loadTemplate(templateDir, graphToken);
			HashMap<String, GraphTemplate> tests = TemplateLoader.loadTemplate(testDir, graphToken);
			
			//Put the analysis here temporarily
			System.out.println("Dynamic Graph Analysis");
			Analyzer<GraphTemplate> dynamicAnalyzer = new DynamicGraphAnalyzer();
			dynamicAnalyzer.setTemplates(templates);
			dynamicAnalyzer.setTests(tests);
			dynamicAnalyzer.setAnnotGuard(MIBConfiguration.isAnnotGuard());
			dynamicAnalyzer.analyzeTemplate();
			
			/*File labelDir = new File(MIBConfiguration.getLabelmapDir());
			TypeToken<StaticRep> staticToken = new TypeToken<StaticRep>(){};
			HashMap<String, StaticRep> allReps = TemplateLoader.loadTemplate(labelDir, staticToken);
			
			HashMap<String, StaticRep> templateReps = new HashMap<String, StaticRep>();
			HashMap<String, StaticRep> testReps = new HashMap<String, StaticRep>();
			
			for (String name: allReps.keySet()) {
				StaticRep sr = allReps.get(name);
				
				if (sr.isTemplate()) {
					templateReps.put(name, sr);
				} else {
					testReps.put(name, sr);
				}
			}
			
			System.out.println("Static Analysis");
			Analyzer<StaticRep> staticAnalyzer = new StaticBytecodeCatAnalyzer();
			staticAnalyzer.setTemplates(templateReps);
			staticAnalyzer.setTests(testReps);
			staticAnalyzer.analyzeTemplate();*/
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
