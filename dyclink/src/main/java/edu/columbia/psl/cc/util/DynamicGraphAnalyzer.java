package edu.columbia.psl.cc.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.columbia.psl.cc.analysis.MIBSimilarity;
import edu.columbia.psl.cc.analysis.SVDKernel;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;

public class DynamicGraphAnalyzer implements Analyzer<GraphTemplate> {
		
	private MIBSimilarity scorer;
	
	private HashMap<String, GraphTemplate> templates;
	
	private HashMap<String, GraphTemplate> tests;
	
	private boolean annotGuard;
	
	public static TreeMap<InstNode, TreeSet<InstNode>> mergeDataControlMap(GraphTemplate gt) {
		/*TreeMap<InstNode, TreeSet<InstNode>> merged = gt.getDataGraph();
		for (InstNode ckey: gt.getControlGraph().keySet()) {
			if (merged.containsKey(ckey)) {
				merged.get(ckey).addAll(gt.getControlGraph().get(ckey));
			} else {
				merged.put(ckey, gt.getControlGraph().get(ckey));
			}
		}
		return merged;*/
		return null;
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
	
	public void setAnnotGuard(boolean annotGuard) {
		this.annotGuard = annotGuard;
	}
	
	public void setTemplates(HashMap<String, GraphTemplate> templates) {
		this.templates = templates;
	}
	
	public HashMap<String, GraphTemplate> getTemplates() {
		return this.templates;
	}
	
	public void setTests(HashMap<String, GraphTemplate> tests) {
		this.tests = tests;
	}
	
	public HashMap<String, GraphTemplate> getTests() {
		return this.tests;
	}
	
	public void setAnalyzer(MIBSimilarity scorer) {
		this.scorer = scorer;
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
	
	/**
	 * If annot guard, compare the single test with all other templates
	 * Each template might have original graph and grown graph
	 * If no annot guard, compare all templates
	 * For each template, only compare the grown graph
	 */
	public void analyzeTemplate() {		
		//MIBSimilarity<CostObj[][]> scorer = new ShortestPathKernel();
		MIBSimilarity<double[][]> scorer = new SVDKernel();
		
		if (this.templates != null && this.templates.size() > 0 
				&& this.tests != null && this.tests.size() >0) {
			System.out.println("Comparison mode: template vs tests");
			scorer.calculateSimilarities(this.templates, this.tests);
		} else if (this.templates == null || this.templates.size() == 0) {
			System.out.println("Exhaustive mode: tests vs. tests");
			scorer.calculateSimilarities(this.tests, this.tests);
		} else if (this.tests == null || this.tests.size() == 0) {
			System.out.println("Exhaustive mode: templates vs. templates");
			scorer.calculateSimilarities(this.templates, this.templates);
		}
		GsonManager.writeResult("", scorer.getResult());
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
