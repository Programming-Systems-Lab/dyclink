package edu.columbia.psl.cc.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
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
	
	private void mergeHead(GraphTemplate parent, GraphTemplate child, int methodIdx) {
		InstNode methodNode = parent.getInstPool().searchAndGet(parent.getMethodKey(), methodIdx);
		String methodKey = StringUtil.genIdxKey(methodNode.getFromMethod(), methodNode.getIdx());
		ExtObj parentEo = parent.getExtMethods().get(methodIdx);
		
		//Update children for input params in child graph
		for (Integer firstRead: child.getFirstReadLocalVars()) {
			InstNode fInst = child.getInstPool().searchAndGet(child.getMethodKey(), firstRead);
			String fInstKey = StringUtil.genIdxKey(fInst.getFromMethod(), fInst.getIdx());
			int varId = Integer.valueOf(fInst.getAddInfo());
			
			if (!child.isStaticMethod()) {
				varId--;
			}
			
			//load local insts in parent method is reversed-ordered
			InstNode parentLoad = parentEo.getLoadLocalInsts().get(parentEo.getLoadLocalInsts().size() - varId - 1);
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
		for (InstNode pFieldInst: parentEo.getWriteFieldInsts()) {
			for (Integer fieldInstId: child.getFirstReadFields()) {
				InstNode fieldInst = child.getInstPool().searchAndGet(child.getMethodKey(), fieldInstId);
				
				if (pFieldInst.getAddInfo().equals(fieldInst.getAddInfo())) {
					pFieldInst.increChild(child.getMethodKey(), fieldInstId, MIBConfiguration.getDataWeight());
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
		
		for (InstNode parentLoad: parentEo.getLoadLocalInsts()) {
			parentLoad.getChildFreqMap().remove(methodKey);
		}
	}
	
	private void mergeGraphs(GraphTemplate parent, GraphTemplate child, int childIdx) {
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
		
		this.mergeHead(parent, child, childIdx);
		
		InstNode methodNode = parent.getInstPool().searchAndGet(parent.getMethodKey(), childIdx);
		String methodKey = StringUtil.genIdxKey(methodNode.getFromMethod(), methodNode.getIdx());
		if (!parent.isStaticMethod()) {
			//If method is instance level, remove the aload instruction from pool
			String aloadId = methodNode.getDataParentList().get(methodNode.getDataParentList().size() - 1);
			String[] parsed = StringUtil.parseIdxKey(aloadId);
			InstNode loadInst = parent.getInstPool().searchAndGet(parsed[0], Integer.valueOf(parsed[1]));
			
			this.parentRemove(loadInst, parent.getInstPool(), aloadId);
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
	
	/**
	 * Now merge all ext methods in one graph.
	 * Or we need separate grown graph for each ext methods?
	 * This will result in too many graphs to analyze
	 * @param parentGraph
	 * @return
	 */
	private List<GrownGraph> collectAndMergeChildGraphs(GraphTemplate parentGraph) {
		HashMap<Integer, GraphTemplate> extMethodMap = GraphUtil.collectChildGraphs(parentGraph);
		if (extMethodMap.size() == 0)
			return null;
		
		List<GrownGraph> ret = new ArrayList<GrownGraph>();
		
		/*for (Integer methodInstIdx: extMethodMap.keySet()) {
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
		}*/
		
		//Merge all
		GrownGraph copyParent = new GrownGraph(parentGraph);
		System.out.println("SHow copy parent graph: ");
		copyParent.showGraph();
		ExtObj lastRet = null;
		for (Integer methodInstIdx: extMethodMap.keySet()) {
			GraphTemplate copyChild = new GraphTemplate(extMethodMap.get(methodInstIdx));
			if (lastRet != null && lastRet.getWriteFieldInsts().size() > 0) {
				//Merge write inst that may affect the current method
				TreeSet<InstNode> curWriteInst = copyParent.getExtMethods().get(methodInstIdx).getWriteFieldInsts();
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
				curWriteInst.addAll(additional);
			}
			
			lastRet = copyChild.getReturnInfo();;
			if (copyChild.getExtMethods().size() > 0) {
				List<GrownGraph> recurChildren = this.collectAndMergeChildGraphs(copyChild);
				
				if (recurChildren != null && recurChildren.size() > 0) {
					copyChild = recurChildren.get(0);
				}
			}
			
			this.mergeGraphs(copyParent, copyChild, methodInstIdx);
			System.out.println("Merge result now: ");
			copyParent.showGraph();
			copyParent.updateKeyMethods(methodInstIdx, copyParent.getExtMethods().get(methodInstIdx).getLineNumber());
		}
		ret.add(copyParent);
		
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
			GraphVisualizer gv = new GraphVisualizer(tempGraph, tempGraph.getMethodKey());
			gv.convertToVisualGraph();
			double[][] templateCostTable = scorer.constructCostTable(templateName, tempGraph.getInstPool());
			
			List<GrownGraph> grownGraphs = this.collectAndMergeChildGraphs(tempGraph);
			HashMap<GrownGraph, double[][]> growCosts = new HashMap<GrownGraph, double[][]>();
			int growCount = 0;
			if (grownGraphs != null && grownGraphs.size() > 0) {
				for (GrownGraph gGraph: grownGraphs) {
					gGraph.showGraph();
					String growName = templateName + growCount++;
					System.out.println("Grown graph: " + growName);
					GraphVisualizer gv2 = new GraphVisualizer(gGraph, growName);
					gv2.convertToVisualGraph();
					double[][] growCostTable = scorer.constructCostTable(growName, gGraph.getInstPool());
					growCosts.put(gGraph, growCostTable);
				}
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
