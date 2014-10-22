package edu.columbia.psl.cc.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import edu.columbia.psl.cc.visual.GraphVisualizer;

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
	
	private void parentRemove(InstNode inst, InstPool pool, String instKey) {
		//Remove data parent if any
		for (String dp: inst.getDataParentList()) {
			String[] dParsed = StringUtil.parseIdxKey(dp);
			InstNode dpInst = pool.searchAndGet(dParsed[0], Integer.valueOf(dParsed[1]));
			dpInst.getChildFreqMap().remove(instKey);
		}
		
		//Remove control parent if any
		for (String cp: inst.getControlParentList()) {
			String[] cParsed = StringUtil.parseIdxKey(cp);
			InstNode cpInst = pool.searchAndGet(cParsed[0], Integer.valueOf(cParsed[1]));
			cpInst.getChildFreqMap().remove(instKey);
		}
	}
	
	private void mergeHead(GraphTemplate parent, GraphTemplate child, ExtObj methodEo) {
		InstNode methodNode = parent.getInstPool().searchAndGet(parent.getMethodKey(), methodEo.getInstIdx());
		String methodKey = StringUtil.genIdxKey(methodNode.getFromMethod(), methodNode.getIdx());
		
		//Update children for input params in child graph
		for (Integer firstRead: child.getFirstReadLocalVars()) {
			InstNode fInst = child.getInstPool().searchAndGet(child.getMethodKey(), firstRead);
			String fInstKey = StringUtil.genIdxKey(fInst.getFromMethod(), fInst.getIdx());
			int varId = Integer.valueOf(fInst.getAddInfo());
			
			if (!child.isStaticMethod()) {
				varId--;
			}
			
			//load local insts in parent method is reversed-ordered
			InstNode parentLoad = methodEo.getLoadLocalInsts().get(methodEo.getLoadLocalInsts().size() - varId - 1);
			double freq = parentLoad.getChildFreqMap().get(methodKey);
			for (String fcID: fInst.getChildFreqMap().keySet()) {
				String[] parsed = StringUtil.parseIdxKey(fcID);
				InstNode fcInst = child.getInstPool().searchAndGet(parsed[0], Integer.valueOf(parsed[1]));
				parentLoad.increChild(fcInst.getFromMethod(), fcInst.getIdx(), fInst.getChildFreqMap().get(fcID));
				
				fcInst.getDataParentList().remove(fInstKey);
				fcInst.registerParent(parentLoad.getFromMethod(), parentLoad.getIdx(), false);
			}
			
			//Remvoe the load insts in child because of duplicates
			this.parentRemove(fInst, child.getInstPool(), fInstKey);
			
			parent.getInstPool().remove(fInst);
			child.getInstPool().remove(fInst);
		}
		
		//Construct relations between read/write field insts between parent and child insts
		System.out.println("Parent eo write: " + methodEo.getWriteFieldInsts());
		System.out.println("Child read insts: " + child.getFirstReadFields());
		for (InstNode pFieldInst: methodEo.getWriteFieldInsts()) {
			for (Integer fieldInstId: child.getFirstReadFields()) {
				InstNode fieldInst = child.getInstPool().searchAndGet(child.getMethodKey(), fieldInstId);
				
				if (pFieldInst.getAddInfo().equals(fieldInst.getAddInfo())) {
					pFieldInst.increChild(child.getMethodKey(), fieldInstId, MIBConfiguration.getDataWeight());
					fieldInst.registerParent(pFieldInst.getFromMethod(), pFieldInst.getIdx(), false);
				}
			}
		}
		
		//Propagate control parent from method node
		for (String cp: methodNode.getControlParentList()) {
			String[] cpIdx = StringUtil.parseIdxKey(cp);
			InstNode cpNode = parent.getInstPool().searchAndGet(cpIdx[0], Integer.valueOf(cpIdx[1]));
			
			double propFreq = cpNode.getChildFreqMap().get(methodKey);
			for (InstNode childInst: child.getInstPool()) {
				String childInstKey = StringUtil.genIdxKey(childInst.getFromMethod(), childInst.getIdx());
				cpNode.getChildFreqMap().put(childInstKey, propFreq);
			}
			cpNode.getChildFreqMap().remove(methodKey);
		}
		
		for (InstNode parentLoad: methodEo.getLoadLocalInsts()) {
			parentLoad.getChildFreqMap().remove(methodKey);
		}
	}
	
	private void mergeGraphs(GraphTemplate parent, GraphTemplate child, ExtObj methodEo) {
		InstPool parentPool = parent.getInstPool();
		Iterator<InstNode> poolIt = child.getInstPool().iterator();
		InstNode returnInst = null;
		while (poolIt.hasNext()) {
			InstNode childInst = poolIt.next();
			if (BytecodeCategory.returnOps().contains(childInst.getOp().getOpcode())) {
				returnInst = childInst;
				continue ;
			}
			parentPool.add(childInst);
		}
		String returnInstKey = StringUtil.genIdxKey(returnInst.getFromMethod(), returnInst.getIdx());
		this.parentRemove(returnInst, child.getInstPool(), returnInstKey);
		child.getInstPool().remove(returnInst);
		
		this.mergeHead(parent, child, methodEo);
		
		InstNode methodNode = parent.getInstPool().searchAndGet(parent.getMethodKey(), methodEo.getInstIdx());
		String methodKey = StringUtil.genIdxKey(methodNode.getFromMethod(), methodNode.getIdx());
		if (BytecodeCategory.objMethodOps().contains(methodNode.getOp().getOpcode())) {
			//If method is instance level, remove the aload instruction from pool
			String aloadId = methodNode.getDataParentList().get(methodNode.getDataParentList().size() - 1);
			String[] parsed = StringUtil.parseIdxKey(aloadId);
			InstNode loadInst = parent.getInstPool().searchAndGet(parsed[0], Integer.valueOf(parsed[1]));
			
			this.parentRemove(loadInst, parent.getInstPool(), aloadId);
			System.out.println("MIB Debug: remove load inst from " + parent.getMethodKey() + " " + loadInst);
			parent.getInstPool().remove(loadInst);
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
			for (InstNode writeInst: returnEo.getWriteFieldInsts()) {
				for (InstNode affected: methodEo.getAffFieldInsts()) {
					if (affected.getAddInfo().equals(writeInst.getAddInfo())) {
						affected.registerParent(writeInst.getFromMethod(), writeInst.getIdx(), false);
						writeInst.getChildFreqMap().put(StringUtil.genIdxKey(affected.getFromMethod(), affected.getIdx()), MIBConfiguration.getDataWeight());
						
						if (parent.getFirstReadFields().contains(affected.getIdx()))
							parent.getFirstReadFields().remove(affected.getIdx());
					}
				}
			}
		}
		
		System.out.println("MIB Debug: remove inst from " + parent.getMethodKey() + " " + methodNode);
		parent.getInstPool().remove(methodNode);
	}
	
	/**
	 * Now merge all ext methods in one graph.
	 * Or we need separate grown graph for each ext methods?
	 * This will result in too many graphs to analyze
	 * @param parentGraph
	 * @return
	 */
	private GrownGraph collectAndMergeChildGraphs(GraphTemplate parentGraph) {
		HashMap<Integer, GraphTemplate> extMethodMap = GraphUtil.collectChildGraphs(parentGraph);
		if (extMethodMap.size() == 0)
			return null;
		
		List<GrownGraph> ret = new ArrayList<GrownGraph>();
				
		//Merge all
		GrownGraph copyParent = new GrownGraph(parentGraph);
		System.out.println("Show copy parent graph: ");
		copyParent.showGraph();
		ExtObj lastRet = null;
		ExtObj lastExt = null;
		
		//Need to follow the original call sequence of methods
		//For method in the loop, jsut merge it for the first time
		HashSet<Integer> visitedMethods = new HashSet<Integer>();
		for (ExtObj methodEo: copyParent.getExtMethods()) {
			int methodInstIdx = methodEo.getInstIdx();
			if (extMethodMap.get(methodInstIdx) == null 
					|| visitedMethods.contains(methodInstIdx))
				continue ;
			
			visitedMethods.add(methodInstIdx);
			GraphTemplate copyChild = new GraphTemplate(extMethodMap.get(methodInstIdx));
			System.out.println("Current child: " + copyChild.getMethodKey());
			
			if ((lastRet != null && lastRet.getWriteFieldInsts().size() > 0) 
					|| (lastExt != null && lastExt.getWriteFieldInsts().size() > 0)) {
				System.out.println("Last ret: " + lastRet.getWriteFieldInsts().size());
				System.out.println("Last ext: " + lastExt.getWriteFieldInsts().size());
				//Merge write inst that may affect the current method
				TreeSet<InstNode> curWriteInst = methodEo.getWriteFieldInsts();
				TreeSet<InstNode> legacyWrite = lastRet.getWriteFieldInsts();
				TreeSet<InstNode> additional = new TreeSet<InstNode>();
				for (InstNode legInst: legacyWrite) {
					boolean shouldAdd = true;
					for (InstNode curInst: curWriteInst) {
						if (curInst.getAddInfo().equals(legInst.getAddInfo()))
							shouldAdd = false;
					}
					if (shouldAdd)
						additional.add(legInst);
				}
				
				for (InstNode lastExtInst: lastExt.getWriteFieldInsts()) {
					boolean shouldAdd = true;
					for (InstNode curInst: curWriteInst) {
						if (curInst.getAddInfo().equals(lastExtInst.getAddInfo()))
							shouldAdd = false;
					}
					if (shouldAdd)
						additional.add(lastExtInst);
				}
				
				System.out.println("Check additional add: " + additional);
				curWriteInst.addAll(additional);
				System.out.println("Check cur write inst: " + curWriteInst);
			}
			
			lastExt = methodEo;
			lastRet = copyChild.getReturnInfo();
			if (copyChild.getExtMethods().size() > 0) {
				//List<GrownGraph> recurChildren = this.collectAndMergeChildGraphs(copyChild);
				GrownGraph recurChildren = this.collectAndMergeChildGraphs(copyChild);
				
				if (recurChildren != null)
					copyChild = recurChildren;
			}
			
			this.mergeGraphs(copyParent, copyChild, methodEo);
			System.out.println("Merge result now: ");
			copyParent.showGraph();
			copyParent.updateKeyMethods(methodInstIdx, methodEo.getLineNumber());
		}
		//ret.add(copyParent);
		//return ret;
		
		return copyParent;
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
		
		if (this.tests == null || this.tests.size() == 0) {
			System.out.println("Exhaustive mode: template vs. template");
			scorer.calculateSimilarities(this.templates, this.templates);
		} else {
			System.out.println("Comparison mode: test vs template");
			scorer.calculateSimilarities(this.tests, this.templates);
		}
		GsonManager.writeResult(scorer.getResult());
		
		
		/*StringBuilder sb = new StringBuilder();
		HashMap<String, double[][]> cachedMap = new HashMap<String, double[][]>();
		List<String> sortedKeys = new ArrayList<String>(this.templates.keySet());
		Collections.sort(sortedKeys);
		
		for (int i = 0; i < sortedKeys.size(); i++) {
			String key1 = sortedKeys.get(i);
			
			//Skip main method for now;
			if (key1.contains(skipMethod))
				continue ;
			
			GraphTemplate graph1 = this.templates.get(key1);
			double[][] templateCostTable = null;
			if (cachedMap.containsKey(key1)) {
				templateCostTable = cachedMap.get(key1);
			} else {
				templateCostTable = scorer.constructCostTable(key1, graph1.getInstPool());
				cachedMap.put(key1, templateCostTable);
			}
			
			for (int j = i; j < sortedKeys.size(); j++) {
				String key2 = sortedKeys.get(j);
				
				if (key2.contains(skipMethod))
					continue ;
				
				GraphTemplate graph2 = this.templates.get(key2);
				double[][] templateCostTable2 = null;
				if (cachedMap.containsKey(key1)) {
					templateCostTable2 = cachedMap.get(key2);
				} else {
					templateCostTable2 = scorer.constructCostTable(key2, graph2.getInstPool());
					cachedMap.put(key2, templateCostTable2);
				}
				
				double simScore = scorer.calculateSimilarity(templateCostTable, templateCostTable2);
				String output = key1 + " vs. " + key2 + " " + simScore;
				sb.append(output + "\n");
				System.out.println(output);
			}
		}
		GsonManager.writeResult(sb);*/
		
		//Score kernel
		/*HashMap<String, double[][]> cachedGrown = new HashMap<String, double[][]>();		
		for (String templateName: this.templates.keySet()) {
			GraphTemplate tempGraph = this.templates.get(templateName);
			System.out.println("Original temp graph: ");
			tempGraph.showGraph();
			//GraphVisualizer gv = new GraphVisualizer(tempGraph, tempGraph.getMethodKey());
			//gv.convertToVisualGraph();
			double[][] templateCostTable = null;
			if (cachedGrown.containsKey(templateName)) {
				templateCostTable = cachedGrown.get(templateName);
			} else {
				templateCostTable = scorer.constructCostTable(templateName, tempGraph.getInstPool());
				cachedGrown.put(templateName, templateCostTable);
			}
			
			//List<GrownGraph> grownGraphs = this.collectAndMergeChildGraphs(tempGraph);
			GrownGraph grownGraph = this.collectAndMergeChildGraphs(tempGraph);
			HashMap<GrownGraph, double[][]> growCosts = new HashMap<GrownGraph, double[][]>();
			int growCount = 0;
			if (grownGraph != null) {
				//Now only one grown graph
				grownGraph.showGraph();
				String growName = templateName + growCount++;
				System.out.println("Grown graph: " + growName);
				//GraphVisualizer gv2 = new GraphVisualizer(gGraph, growName);
				//gv2.convertToVisualGraph();
				double[][] growCostTable = scorer.constructCostTable(growName, grownGraph.getInstPool());
				growCosts.put(grownGraph, growCostTable);
				TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
				GsonManager.writeJsonGeneric(grownGraph, growName, graphToken, 0);
				cachedGrown.put(templateName, growCostTable);
			} else {
				cachedGrown.put(templateName, templateCostTable);
			}
			
			if (this.annotGuard) {
				for (String testName: this.tests.keySet()) {
					GraphTemplate testGraph = this.tests.get(testName);
					double[][] testCostTable = scorer.constructCostTable(testName, testGraph.getInstPool());
					double graphScore = scorer.calculateSimilarity(templateCostTable, testCostTable);
					String oriOutput = templateName + " vs " + testName + " " + graphScore;
					sb.append(oriOutput + "\n");
					System.out.println(oriOutput);
					
					for (GrownGraph growGraph: growCosts.keySet()) {
						double growScore = scorer.calculateSimilarity(growCosts.get(growGraph), testCostTable);
						String growId = templateName + growGraph.getKeyLines();
						String grownOutput = growId + " vs " + testName + " " + growScore;
						sb.append(grownOutput + "\n");
						System.out.println(grownOutput);
					}
				}
			}
		}
		
		if (!this.annotGuard) {
			List<String> sortedName = new ArrayList<String>(cachedGrown.keySet());
			Collections.sort(sortedName);
			for (int i = 0; i < sortedName.size(); i++) {
				String temp1 = sortedName.get(i);
				double[][] costTable1 = cachedGrown.get(temp1);
				for (int j = i; j < sortedName.size(); j++) {
					String temp2 = sortedName.get(j);
					double[][] costTable2 = cachedGrown.get(temp2);
					
					double simScore = scorer.calculateSimilarity(costTable1, costTable2);
					String output = temp1 + " vs. " + temp2 + " " + simScore;
					sb.append(output + "\n");
					System.out.println(output);
				}
			}
		}
		
		GsonManager.writeResult(sb);*/
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
