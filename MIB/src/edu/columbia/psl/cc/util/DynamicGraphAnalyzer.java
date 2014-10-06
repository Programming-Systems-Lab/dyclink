package edu.columbia.psl.cc.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import edu.columbia.psl.cc.pojo.GrownGraph;
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
	
	private void mergeHead(GraphTemplate parent, GraphTemplate child, int childIdx) {
		InstNode methodNode = parent.getInstPool().searchAndGet(parent.getMethodKey(), childIdx);
		
		int methodArgs = child.getMethodArgSize();
		if (methodArgs < methodNode.getParentList().size()) {
			//Instance method, remove aload
			String toRemove = methodNode.getParentList().get(methodNode.getParentList().size() - 1);
			String[] toRemoveInfo = StringUtil.parseIdxKey(toRemove);
			parent.getInstPool().searchAndRemove(toRemoveInfo[0], Integer.valueOf(toRemoveInfo[1]));
			methodNode.getParentList().remove(toRemove);
		} 
		
		if (methodArgs == 0)
			return ;
		
		//Search start insts in child
		InstNode[] childStarts = new InstNode[methodArgs];
		int count = 0;
		System.out.println("Check child path size: " + child.getPath().size());
		for (InstNode inst: child.getPath()) {
			if (BytecodeCategory.readCategory().contains(inst.getOp().getCatId()) &&
					count <= methodArgs) {
				childStarts[count++] = inst;
			}
		}
				
		InstNode[] parents = null;
		if (methodArgs > methodNode.getParentList().size()) {
			System.err.println("Invalid method description or recording: " + methodNode);
			return ;
		}
				
		parents = new InstNode[methodNode.getParentList().size()];
		for (int i = methodNode.getParentList().size() - 1, j = 0; i >= 0; i--, j++) {
			String mParentNode = methodNode.getParentList().get(i);
			String[] mParentInfo = StringUtil.parseIdxKey(mParentNode);
			parents[j] = parent.getInstPool().searchAndGet(mParentInfo[0], Integer.valueOf(mParentInfo[1]));
		} 
				
		//Merge. parent inst will be replaced by child inst
		for (InstNode p: parents) {
			ArrayList<String> pList = p.getParentList();
			String pKey = StringUtil.genIdxKey(p.getFromMethod(), p.getIdx());
					
			for (InstNode c: childStarts) {
				//parent.getInstPool().searchAndGet(c.getFromMethod(), c.getIdx(), c.getOp().getOpcode(), c.getAddInfo());
				parent.getInstPool().add(c);
				String cKey = StringUtil.genIdxKey(c.getFromMethod(), c.getIdx());
						
				for (String pp: pList) {
					String[] ppInfo = StringUtil.parseIdxKey(pp);
					InstNode ppNode = parent.getInstPool().searchAndGet(ppInfo[0], Integer.valueOf(ppInfo[1]));
					double freq = ppNode.getChildFreqMap().get(pKey);
					ppNode.getChildFreqMap().remove(pKey);
					ppNode.getChildFreqMap().put(cKey, freq);
				}
			}
			parent.getInstPool().remove(p);
		}
	}
	
	private void mergeGraphs(GraphTemplate parent, GraphTemplate child, int childIdx) {
		this.mergeHead(parent, child, childIdx);
		
		InstNode methodNode = parent.getInstPool().searchAndGet(parent.getMethodKey(), childIdx);
		String methodIdxKey = StringUtil.genIdxKey(methodNode.getFromMethod(), methodNode.getIdx());
		
		//Update children of method inst
		InstNode lastSecond = child.getLastSecondInst();
		parent.getInstPool().add(lastSecond);
		if (lastSecond != null) {
			String lastSecondKey = StringUtil.genIdxKey(lastSecond.getFromMethod(), lastSecond.getIdx());
			parent.getInstPool().add(lastSecond);
			
			for (String childKey: methodNode.getChildFreqMap().keySet()) {
				String[] childInfo = StringUtil.parseIdxKey(childKey);
				InstNode childInst = parent.getInstPool().searchAndGet(childInfo[0], Integer.valueOf(childInfo[1]));
				double freq = methodNode.getChildFreqMap().get(childKey);
				
				lastSecond.getChildFreqMap().clear();
				lastSecond.getChildFreqMap().put(childKey, freq);
				
				childInst.getParentList().remove(methodIdxKey);
				childInst.getParentList().add(lastSecondKey);
			}
		}
		parent.getInstPool().remove(methodNode);
	}
	
	private List<GrownGraph> collectAndMergeChildGraphs(GraphTemplate parentGraph) {
		HashMap<Integer, GraphTemplate> extMethodMap = GraphUtil.collectChildGraphs(parentGraph);
		List<GrownGraph> ret = new ArrayList<GrownGraph>();
		
		for (Integer methodInstIdx: extMethodMap.keySet()) {
			//Copy the parent graph
			GrownGraph copyParent = new GrownGraph(parentGraph);
			System.out.println("Show copy parent graph: ");
			copyParent.showGraph();
			
			//Should recursive, temporarily not
			GraphTemplate copyChild = new GraphTemplate(extMethodMap.get(methodInstIdx));
			System.out.println("Show copy child graph: ");
			copyChild.showGraph();
			
			//Merge
			this.mergeGraphs(copyParent, copyChild, methodInstIdx);
			copyParent.updateKeyMethods(methodInstIdx, copyParent.getExtMethods().get(methodInstIdx));
			ret.add(copyParent);
		}
		
		return ret;
	}
	
	public void analyzeTemplate() {		
		//MIBSimilarity<CostObj[][]> scorer = new ShortestPathKernel();
		MIBSimilarity<double[][]> scorer = new SVDKernel();
		//Score kernel
		for (String templateName: this.templates.keySet()) {
			GraphTemplate tempGraph = this.templates.get(templateName);
			System.out.println("Original temp graph: ");
			tempGraph.showGraph();
			double[][] templateCostTable = scorer.constructCostTable(templateName, tempGraph.getInstPool());
			
			List<GrownGraph> grownGraphs = this.collectAndMergeChildGraphs(tempGraph);
			HashMap<GrownGraph, double[][]> growCosts = new HashMap<GrownGraph, double[][]>();
			int growCount = 0;
			for (GrownGraph gGraph: grownGraphs) {
				System.out.println("Grown graph: ");
				gGraph.showGraph();
				String growName = templateName + growCount++;
				double[][] growCostTable = scorer.constructCostTable(growName, gGraph.getInstPool());
				growCosts.put(gGraph, growCostTable);
			}
			
			for (String testName: this.tests.keySet()) {
				GraphTemplate testGraph = this.tests.get(testName);
				double[][] testCostTable = scorer.constructCostTable(testName, testGraph.getInstPool());
				double graphScore = scorer.calculateSimilarity(templateCostTable, testCostTable);
				System.out.println(templateName + " vs " + testName + " " + graphScore);
				
				for (GrownGraph growGraph: growCosts.keySet()) {
					double growScore = scorer.calculateSimilarity(growCosts.get(growGraph), testCostTable);
					String growId = templateName + growGraph.getKeyLines();
					System.out.println(growId + " vs " + testName + " " + growScore);
				}
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
