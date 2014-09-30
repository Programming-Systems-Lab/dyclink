package edu.columbia.psl.cc.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.util.ShortestPathKernel.CostObj;

public class DynamicGraphAnalyzer {
	
	public static HashMap<String, TreeMap<String, TreeSet<String>>> loadTemplate(File dir, TypeToken typeToken) {
		HashMap<String, TreeMap<String, TreeSet<String>>> ret = new HashMap<String, TreeMap<String, TreeSet<String>>>();
		if (!dir.isDirectory()) {
			TreeMap<String, TreeSet<String>> temp = GsonManager.readJsonGeneric(dir, typeToken);
			ret.put(dir.getName(), temp);
		} else {
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(".json");
				}
			};
			
			for (File f: dir.listFiles(filter)) {
				String name = f.getName().replace(".json", "");
				TreeMap<String, TreeSet<String>> value = GsonManager.readJsonGeneric(f, typeToken);
				ret.put(name, value);
			}
		}
		return ret;
	}
	
	public static void expandDepMap(HashMap<String, HashSet<String>> nodeInfo, HashMap<String, TreeMap<String, TreeSet<String>>> depMaps) {
		for (String method: nodeInfo.keySet()) {
			HashSet<String> allInsts = nodeInfo.get(method);
			TreeMap<String, TreeSet<String>> depMap = depMaps.get(method);
			
			for (String inst: allInsts) {
				if (depMap.get(inst) == null) {
					depMap.put(inst, null);
				}
			}
		}
	}
	
	public static void compensateMap(HashMap<String, TreeMap<String, TreeSet<String>>> depMaps, int maxCount) {
		for (String method: depMaps.keySet()) {
			TreeMap<String, TreeSet<String>> depMap = depMaps.get(method);
			int diff = maxCount - depMap.size();
			for (int i = 0; i < diff; i++) {
				String fakeName = "fake" + i;
				depMap.put(fakeName, null);
			}
		}
	}
	
	public static void analyzeTemplate() {
		File templateDir = new File("./template");
		File testDir = new File("./test");
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				// TODO Auto-generated method stub
				return name.toLowerCase().endsWith(".json");
			}
		};
		
		TypeToken<TreeMap<String, TreeSet<String>>> typeToken = new TypeToken<TreeMap<String, TreeSet<String>>>(){};
		HashMap<String, TreeMap<String, TreeSet<String>>> templateMap = loadTemplate(templateDir, typeToken);
		HashMap<String, TreeMap<String, TreeSet<String>>> testMap = loadTemplate(testDir, typeToken);
		
		TypeToken<HashMap<String, Integer>> labelType = new TypeToken<HashMap<String, Integer>>(){};
		
		int maxCount = 0;
		HashMap<String, HashSet<String>> nodeInfo = new HashMap<String, HashSet<String>>();
		for (String name: templateMap.keySet()) {
			TreeMap<String, TreeSet<String>> tmpTemp = templateMap.get(name);
			HashSet<String> allNodes = new HashSet<String>();
			allNodes.addAll(tmpTemp.keySet());
			
			for (String parent: tmpTemp.navigableKeySet()) {
				allNodes.addAll(tmpTemp.get(parent));
			}
			System.out.println("Template: " + name);
			System.out.println("Node size: " + allNodes.size());
			
			nodeInfo.put(name, allNodes);
			
			if (allNodes.size() > maxCount)
				maxCount = allNodes.size();
		}
		System.out.println("Check maxCount: " + maxCount);
		expandDepMap(nodeInfo, templateMap);
		//compensateMap(templateMap, maxCount);
		
		HashMap<String, HashSet<String>> testNodeInfo = new HashMap<String, HashSet<String>>();
		for (String name: testMap.keySet()) {
			TreeMap<String, TreeSet<String>> tmpTest = testMap.get(name);
			HashSet<String> allNodes = new HashSet<String>();
			allNodes.addAll(tmpTest.keySet());
			
			for (String parent: tmpTest.navigableKeySet()) {
				allNodes.addAll(tmpTest.get(parent));
			}
			
			testNodeInfo.put(name, allNodes);
			
			System.out.println("Test: " + name);
			System.out.println("Node size: " + allNodes.size());
		}
		expandDepMap(testNodeInfo, testMap);
		
		ShortestPathKernel spk = new ShortestPathKernel();
		//Score kernel
		for (String templateName: templateMap.keySet()) {
			TreeMap<String, TreeSet<String>> templateMethod = templateMap.get(templateName);
			System.out.println("Construct cost table: " + templateName);
			CostObj[][] templateCostTable = spk.constructCostTable(templateName, templateMethod);
			for (String testName: testMap.keySet()) {
				TreeMap<String, TreeSet<String>> testMethod = testMap.get(testName);
				System.out.println("Construct cost table: " + testName);
				CostObj[][] testCostTable = spk.constructCostTable(testName, testMethod);
				double graphScore = spk.scoreShortestPaths(templateCostTable, testCostTable);
				
				System.out.println(templateName + " vs " + testName + " " + graphScore);
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		analyzeTemplate();
	}

}
