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
import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.pojo.CostObj;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;

public class DynamicGraphAnalyzer implements Analyzer<GraphTemplate> {
	
	private MIBSimilarity scorer;
	
	private HashMap<String, GraphTemplate> templates;
	
	private HashMap<String, GraphTemplate> tests;
	
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
	
	public HashMap<String, TreeMap<InstNode, TreeSet<InstNode>>> preprocessGraph(HashMap<String, GraphTemplate> graphs) {
		HashMap<String, TreeMap<InstNode, TreeSet<InstNode>>> ret = new HashMap<String, TreeMap<InstNode, TreeSet<InstNode>>>();
		for (String mkey: graphs.keySet()) {
			GraphTemplate graph = graphs.get(mkey);
			System.out.println("Check method: " + mkey);
			//System.out.println("Invoke method lookup: " + graph.getInvokeMethodLookup());
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
	
	private void mergeGraphs(GraphTemplate parent, GraphTemplate child, int childIdx) {
		InstNode methodNode = parent.getInstPool().searchAndGet(parent.getMethodKey(), childIdx);
		
		int methodArgs = child.getMethodArgSize();
		int methodRet = child.getMethodReturnSize();
		
		//Search start insts in child
		InstNode[] starts = new InstNode[methodArgs];
		int count = 0;
		for (InstNode inst: child.getPath()) {
			if (BytecodeCategory.readCategory().contains(inst.getOp().getCatId()) &&
					count <= methodArgs) {
				starts[count++] = inst;
			}
		}
		
		InstNode[] parents = null;
		if (methodArgs < methodNode.getParentList().size()) {
			//Instance method
			parents = new InstNode[methodNode.getParentList().size() - 1];
			for (int i = methodNode.getParentList().size() - 2, j = 0; i >=0; i--, j++) {
				parents[j] = parent.getInstPool().searchAndGet(parent.getMethodKey(), methodNode.getParentList().get(i));
			}
		} else if (methodArgs == methodNode.getParentList().size()) {
			//Class method
			parents = new InstNode[methodNode.getParentList().size()];
			for (int i = methodNode.getParentList().size() - 1, j = 0; i >= 0; i--, j++) {
				parents[j] = parent.getInstPool().searchAndGet(parent.getMethodKey(), methodNode.getParentList().get(i));
			}
		} else {
			System.err.println("Invalid method description or recording: " + methodNode);
		}
	}
	
	private void collectAndMergeChildGraphs(GraphTemplate parentGraph) {
		HashMap<Integer, GraphTemplate> extMethodMap = GraphUtil.collectChildGraphs(parentGraph);
		
		for (Integer methodInstIdx: extMethodMap.keySet()) {
			//Copy the parent graph
			GraphTemplate copyParent = new GraphTemplate(parentGraph);
			GraphTemplate childGraph = extMethodMap.get(methodInstIdx);
			System.out.println("Copy parent: " + copyParent.getMethodKey());
			System.out.println("Child graph: " + childGraph.getLastSecondInst());
			//Merge
		}
	}
	
	public void analyzeTemplate() {		
		MIBSimilarity<CostObj[][]> scorer = new ShortestPathKernel();
		//MIBSimilarity scorer = new SVDKernel();
		//Score kernel
		for (String templateName: this.templates.keySet()) {
			GraphTemplate tempGraph = this.templates.get(templateName);
			this.collectAndMergeChildGraphs(tempGraph);
			CostObj[][] templateCostTable = scorer.constructCostTable(templateName, tempGraph.getInstPool());
			
			for (String testName: this.tests.keySet()) {
				GraphTemplate testGraph = this.tests.get(testName);
				CostObj[][] testCostTable = scorer.constructCostTable(testName, testGraph.getInstPool());
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
