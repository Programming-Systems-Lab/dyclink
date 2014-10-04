package edu.columbia.psl.cc.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.analysis.LevenshteinDistance;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.StaticRep;

public class StaticBytecodeCatAnalyzer implements Analyzer<StaticRep>{
	
	private HashMap<String, StaticRep> templates;
	
	private HashMap<String, StaticRep> tests;
	
	public void setTemplates(HashMap<String, StaticRep> templates) {
		this.templates = templates;
	}
	
	@Override
	public HashMap<String, StaticRep> getTemplates() {
		return this.templates;
	}
	
	public void setTests(HashMap<String, StaticRep> tests) {
		this.tests = tests;
	}
	
	public HashMap<String, StaticRep> getTests() {
		return this.tests;
	}
	
	public HashMap<String, StaticRep> loadStaticMap(File dir) {
		HashMap<String, StaticRep> ret = new HashMap<String, StaticRep>();
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".json");
			}
		};
		
		TypeToken<StaticRep> staticType = new TypeToken<StaticRep>(){};
		if (!dir.isDirectory()) {
			String name = dir.getName().replace(".json", "");
			StaticRep staticRep = GsonManager.readJsonGeneric(dir, staticType);
			ret.put(name, staticRep);
		} else {
			for (File f: dir.listFiles(filter)) {
				String name = f.getName().replace(".json", "");
				StaticRep staticRep = GsonManager.readJsonGeneric(f, staticType);
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
			StaticRep tempRep = this.templates.get(tempName);
			for (String testName: this.tests.keySet()) {
				StaticRep testRep = tests.get(testName);
				System.out.println(tempName + " vs " + testName + " " + dist.calculateSimilarity(tempRep.getOpCatString(), testRep.getOpCatString()));
				
			}
		}
	}
}
