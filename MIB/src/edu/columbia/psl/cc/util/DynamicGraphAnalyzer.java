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
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.CostObj;
import edu.columbia.psl.cc.pojo.ExtObj;
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
	
	private static void updateGrandPa(GraphTemplate parent, GraphTemplate child, List<String> grandPas, String loadVarKey) {
		for (String gp: grandPas) {
			String[] searchIdx = StringUtil.parseIdxKey(gp);
			InstNode dgNode = parent.getInstPool().searchAndGet(searchIdx[0], Integer.valueOf(searchIdx[1]));
			double freq = dgNode.getChildFreqMap().get(loadVarKey);
			for (Integer i: child.getFirstReadLocalVars()) {
				InstNode readNode = child.getInstPool().searchAndGet(child.getMethodKey(), i);
				String readNodeKey = StringUtil.genIdxKey(readNode.getFromMethod(), i);
				dgNode.getChildFreqMap().put(readNodeKey, freq);
			}
			dgNode.getChildFreqMap().remove(loadVarKey);
		}
	}
	
	private void mergeHead(GraphTemplate parent, GraphTemplate child, int methodIdx) {
		//InstNode methodNode = parent.getInstPool().searchAndGet(parent.getMethodKey(), methodIdx);
		ExtObj parentEo = parent.getExtMethods().get(methodIdx);
		
		for (InstNode loadVarNode: parentEo.getLoadLocalInsts()) {
			String loadVarKey = StringUtil.genIdxKey(loadVarNode.getFromMethod(), loadVarNode.getIdx());
			List<String> dataGrandPa = loadVarNode.getDataParentList();
			updateGrandPa(parent, child, dataGrandPa, loadVarKey);
			
			List<String> controlGrandPa = loadVarNode.getControlParentList();
			updateGrandPa(parent, child, controlGrandPa, loadVarKey);
			
			parent.getInstPool().searchAndRemove(loadVarNode.getFromMethod(), loadVarNode.getIdx());
		}
	}
	
	private void mergeGraphs(GraphTemplate parent, GraphTemplate child, int childIdx) {
		InstPool parentPool = parent.getInstPool();
		for (InstNode childInst: child.getInstPool()) {
			if (BytecodeCategory.returnOps().contains(childInst.getOp().getOpcode()))
				continue ;
			
			parentPool.add(childInst);
		}
		
		this.mergeHead(parent, child, childIdx);
		
		InstNode methodNode = parent.getInstPool().searchAndGet(parent.getMethodKey(), childIdx);
		String methodKey = StringUtil.genIdxKey(methodNode.getFromMethod(), methodNode.getIdx());
		if (!BytecodeCategory.staticMethod().contains(methodNode.getOp().getOpcode())) {
			//If method is instance level, remove the aload instruction from pool
			String aloadId = methodNode.getDataParentList().get(methodNode.getDataParentList().size() - 1);
			String[] parsed = StringUtil.parseIdxKey(aloadId);
			parent.getInstPool().searchAndRemove(parsed[0], Integer.valueOf(parsed[1]));
		}
		
		//Update children of method inst
		ExtObj returnEo = child.getReturnInfo();
		if (returnEo.getLoadLocalInsts().size() > 0) {
			//Should only be 1
			InstNode lastSecond = returnEo.getLoadLocalInsts().get(0);
			String lastSecondKey = StringUtil.genIdxKey(lastSecond.getFromMethod(), lastSecond.getIdx());
			lastSecond.getChildFreqMap().clear();
			
			for (String childKey: methodNode.getChildFreqMap().keySet()) {
				String[] childInfo = StringUtil.parseIdxKey(childKey);
				InstNode childInst = parent.getInstPool().searchAndGet(childInfo[0], Integer.valueOf(childInfo[1]));
				double freq = methodNode.getChildFreqMap().get(childKey);
				
				lastSecond.getChildFreqMap().put(childKey, freq);
				childInst.getDataParentList().remove(methodKey);
				childInst.getDataParentList().add(lastSecondKey);
			}
		}
		
		if (returnEo.getWriteFieldInsts().size() > 0) {
			ExtObj methodExtObj = parent.getExtMethods().get(childIdx);
			for (InstNode writeInst: returnEo.getWriteFieldInsts()) {
				for (InstNode affected: methodExtObj.getAffFieldInsts()) {
					if (affected.getAddInfo().equals(writeInst.getAddInfo())) {
						affected.registerParent(writeInst.getFromMethod(), writeInst.getIdx(), false);
						writeInst.getChildFreqMap().put(StringUtil.genIdxKey(affected.getFromMethod(), affected.getIdx()), MIBConfiguration.getDataWeight());
						
						if (parent.getFirstReadFields().contains(affected.getIdx()))
							parent.getFirstReadFields().remove(affected.getIdx());
					}
				}
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
			copyParent.updateKeyMethods(methodInstIdx, copyParent.getExtMethods().get(methodInstIdx).getLineNumber());
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
