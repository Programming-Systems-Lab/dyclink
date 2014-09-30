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

public class StaticBytecodeCatAnalyzer implements Analyzer{
	
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
		File labelDir = new File(MIBConfiguration.getLabelmapDir());
		HashMap<String, StaticRep> allReps = this.loadStaticMap(labelDir);
		
		HashMap<String, String> templateReps = new HashMap<String, String>();
		HashMap<String, String> testReps = new HashMap<String, String>();
		
		for (String name: allReps.keySet()) {
			StaticRep sr = allReps.get(name);
			
			if (sr.isTemplate()) {
				templateReps.put(name, sr.getOpCatString());
			} else {
				testReps.put(name, sr.getOpCatString());
			}
		}
		
		LevenshteinDistance dist = new LevenshteinDistance();
		for (String tempName: templateReps.keySet()) {
			String tempRep = templateReps.get(tempName);
			for (String testName: testReps.keySet()) {
				String testRep = testReps.get(testName);
				System.out.println(tempName + " vs " + testName + " " + dist.calculateSimilarity(tempRep, testRep));
				
			}
		}
	}

}
