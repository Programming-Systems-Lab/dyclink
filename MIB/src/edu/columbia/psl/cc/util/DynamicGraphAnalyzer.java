package edu.columbia.psl.cc.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.analysis.MIBSimilarity;
import edu.columbia.psl.cc.analysis.SVDKernel;
import edu.columbia.psl.cc.analysis.ShortestPathKernel;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.CostObj;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;

public class DynamicGraphAnalyzer implements Analyzer {
	
	private MIBSimilarity scorer;
	
	public static <T> HashMap<String, T> loadTemplate(File dir, TypeToken<T> typeToken) {
		HashMap<String, T> ret = new HashMap<String, T>();
		if (!dir.isDirectory()) {
			T temp = GsonManager.readJsonGeneric(dir, typeToken);
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
				T value = GsonManager.readJsonGeneric(f, typeToken);
				ret.put(name, value);
			}
		}
		return ret;
	}
	
	public static TreeMap<InstNode, TreeSet<InstNode>> mergeDataControlMap(GraphTemplate gt) {
		TreeMap<InstNode, TreeSet<InstNode>> merged = gt.getDataGraph();
		for (InstNode ckey: gt.getControlGraph().keySet()) {
			if (merged.containsKey(ckey)) {
				merged.get(ckey).addAll(gt.getControlGraph().get(ckey));
			} else {
				merged.put(ckey, gt.getControlGraph().get(ckey));
			}
		}
		return merged;
	}
	
	public static void expandDepMap(HashMap<String, HashSet<InstNode>> nodeInfo, HashMap<String, TreeMap<InstNode, TreeSet<InstNode>>> depMaps) {
		for (String method: nodeInfo.keySet()) {
			HashSet<InstNode> allInsts = nodeInfo.get(method);
			TreeMap<InstNode, TreeSet<InstNode>> depMap = depMaps.get(method);
			
			for (InstNode inst: allInsts) {
				if (!depMap.containsKey(inst)) {
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
	
	public void setAnalyzer(MIBSimilarity scorer) {
		this.scorer = scorer;
	}
	
	public HashMap<String, TreeMap<InstNode, TreeSet<InstNode>>> preprocessGraph(HashMap<String, GraphTemplate> graphs) {
		HashMap<String, TreeMap<InstNode, TreeSet<InstNode>>> ret = new HashMap<String, TreeMap<InstNode, TreeSet<InstNode>>>();
		for (String mkey: graphs.keySet()) {
			GraphTemplate graph = graphs.get(mkey);
			System.out.println("Check method: " + mkey);
			System.out.println("Invoke method lookup: " + graph.getInvokeMethodLookup());
			System.out.println("Last 2nd inst: " + graph.getLastSecondInst());
			ret.put(mkey, mergeDataControlMap(graph));
		}
		return ret;
	}
	
	public void summarizeGraphs(HashMap<String, TreeMap<InstNode, TreeSet<InstNode>>> targetMap) {
		HashMap<String, HashSet<InstNode>> nodeInfo = new HashMap<String, HashSet<InstNode>>();
		for (String name: targetMap.keySet()) {
			TreeMap<InstNode, TreeSet<InstNode>> tmpTemp = targetMap.get(name);
			HashSet<InstNode> allNodes = new HashSet<InstNode>();
			allNodes.addAll(tmpTemp.keySet());
			
			System.out.println("Graph name: " + name);
			System.out.println("Original size: " + allNodes.size());
			for (InstNode parent: tmpTemp.navigableKeySet()) {
				allNodes.addAll(tmpTemp.get(parent));
			}
			System.out.println("Vertex number: " + allNodes.size());
			
			nodeInfo.put(name, allNodes);
		}
		expandDepMap(nodeInfo, targetMap);
	}
	
	public <T> void analyzeTemplate() {
		File templateDir = new File(MIBConfiguration.getTemplateDir());
		File testDir = new File(MIBConfiguration.getTestDir());
		
		TypeToken<GraphTemplate> typeToken = new TypeToken<GraphTemplate>(){};
		HashMap<String, GraphTemplate> templateGraphs = loadTemplate(templateDir, typeToken);
		HashMap<String, GraphTemplate> testGraphs = loadTemplate(testDir, typeToken);
		
		HashMap<String, TreeMap<InstNode, TreeSet<InstNode>>> templateMap = this.preprocessGraph(templateGraphs);
		HashMap<String, TreeMap<InstNode, TreeSet<InstNode>>> testMap = this.preprocessGraph(testGraphs);
		
		this.summarizeGraphs(templateMap);
		this.summarizeGraphs(testMap);
		
		//MIBSimilarity scorer = new ShortestPathKernel();
		MIBSimilarity scorer = new SVDKernel();
		//Score kernel
		for (String templateName: templateMap.keySet()) {
			TreeMap<InstNode, TreeSet<InstNode>> templateMethod = templateMap.get(templateName);
			System.out.println("Construct cost table: " + templateName);
			T templateCostTable = (T) scorer.constructCostTable(templateName, templateMethod);
			for (String testName: testMap.keySet()) {
				TreeMap<InstNode, TreeSet<InstNode>> testMethod = testMap.get(testName);
				System.out.println("Construct cost table: " + testName);
				T testCostTable = (T) scorer.constructCostTable(testName, testMethod);
				double graphScore = scorer.calculateSimilarity(templateCostTable, testCostTable);
				
				System.out.println(templateName + " vs " + testName + " " + graphScore);
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		DynamicGraphAnalyzer analyzer = new DynamicGraphAnalyzer();
		analyzer.analyzeTemplate();
	}

}
