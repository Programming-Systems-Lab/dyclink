package edu.columbia.psl.cc.premain;

import java.io.File;
import java.util.HashMap;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.util.Analyzer;
import edu.columbia.psl.cc.util.DynamicGraphAnalyzer;
import edu.columbia.psl.cc.util.TemplateLoader;

public class AnalysisService {
	
	public static void invokeFinalAnalysis(MIBConfiguration mConfig) {
		//Load templates for each analysis
		File templateDir = new File(mConfig.getTemplateDir());
		File testDir = new File(mConfig.getTestDir());
		TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
		HashMap<String, GraphTemplate> templates = TemplateLoader.loadTemplate(templateDir, graphToken);
		HashMap<String, GraphTemplate> tests = TemplateLoader.loadTemplate(testDir, graphToken);
		
		//Put the analysis here temporarily
		System.out.println("Dynamic Graph Analysis");
		Analyzer<GraphTemplate> dynamicAnalyzer = new DynamicGraphAnalyzer();
		dynamicAnalyzer.setTemplates(templates);
		dynamicAnalyzer.setTests(tests);
		dynamicAnalyzer.setAnnotGuard(mConfig.isAnnotGuard());
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
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
