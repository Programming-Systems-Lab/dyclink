package edu.columbia.psl.cc.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.analysis.LevenshteinDistance;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.StaticMethodMiner;

public class StaticBytecodeCatAnalyzer implements Analyzer<StaticMethodMiner>{
	
	private HashMap<String, StaticMethodMiner> templates;
	
	private HashMap<String, StaticMethodMiner> tests;
	
	public void setAnnotGuard(boolean annotGuard) {
		
	}
	
	public void setTemplates(HashMap<String, StaticMethodMiner> templates) {
		this.templates = templates;
	}
	
	@Override
	public HashMap<String, StaticMethodMiner> getTemplates() {
		return this.templates;
	}
	
	public void setTests(HashMap<String, StaticMethodMiner> tests) {
		this.tests = tests;
	}
	
	public HashMap<String, StaticMethodMiner> getTests() {
		return this.tests;
	}
	
	public HashMap<String, StaticMethodMiner> loadStaticMap(File dir) {
		HashMap<String, StaticMethodMiner> ret = new HashMap<String, StaticMethodMiner>();
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".json");
			}
		};
		
		TypeToken<StaticMethodMiner> staticType = new TypeToken<StaticMethodMiner>(){};
		if (!dir.isDirectory()) {
			String name = dir.getName().replace(".json", "");
			StaticMethodMiner staticRep = GsonManager.readJsonGeneric(dir, staticType);
			ret.put(name, staticRep);
		} else {
			for (File f: dir.listFiles(filter)) {
				String name = f.getName().replace(".json", "");
				StaticMethodMiner staticRep = GsonManager.readJsonGeneric(f, staticType);
				ret.put(name, staticRep);
			}
		}
		return ret;
	}

	@Override
	public void analyzeTemplate() {
		// TODO Auto-generated method stub
		
		LevenshteinDistance dist = new LevenshteinDistance();
		for (String tempName: this.templates.keySet()) {
			StaticMethodMiner tempRep = this.templates.get(tempName);
			for (String testName: this.tests.keySet()) {
				StaticMethodMiner testRep = tests.get(testName);
				System.out.println(tempName + " vs " + testName + " " + dist.calculateSimilarity(tempRep.getOpCatString(), testRep.getOpCatString()));
				
			}
		}
	}
}
