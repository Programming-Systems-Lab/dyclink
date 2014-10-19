package edu.columbia.psl.cc.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.math3.util.MathUtils;
import org.apache.commons.math3.util.Precision;

import com.google.gson.reflect.TypeToken;
import com.sun.xml.internal.ws.util.StringUtils;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.datastruct.VarPairPool;
import edu.columbia.psl.cc.datastruct.VarPool;
import edu.columbia.psl.cc.pojo.CostObj;
import edu.columbia.psl.cc.pojo.ExtObj;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.pojo.Var;
import edu.columbia.psl.cc.pojo.VarPair;

public class GraphUtil {
	
	public static void parentRemove(InstNode inst, InstPool pool, String instKey) {
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
	
	public static void transplantCalleeDepToCaller(InstNode parentNode, 
			InstPool parentPool, 
			InstNode childNode, 
			InstPool childPool) {
		String fInstKey = StringUtil.genIdxKey(childNode.getFromMethod(), childNode.getIdx());
		
		for (String c: childNode.getChildFreqMap().keySet()) {
			double freq = childNode.getChildFreqMap().get(c);
			String[] keySet = StringUtil.parseIdxKey(c);
			int cIdx= Integer.valueOf(keySet[1]);
			parentNode.increChild(keySet[0], cIdx, freq);
			
			InstNode cNode = childPool.searchAndGet(keySet[0], cIdx);
			cNode.getDataParentList().remove(fInstKey);
			
			if (parentNode != null) {
				cNode.registerParent(parentNode.getFromMethod(), parentNode.getIdx(), false);
			}
		}
		
		for (String cont: childNode.getControlParentList()) {
			String[] keySet = StringUtil.parseIdxKey(cont);
			int cIdx = Integer.valueOf(keySet[1]);
			InstNode contNode = childPool.searchAndGet(keySet[0], cIdx);
			double freq = contNode.getChildFreqMap().get(fInstKey);
			
			if (parentNode != null) {
				contNode.increChild(parentNode.getFromMethod(), parentNode.getIdx(), freq);
			}
		}
		
		//Remove these first read local vars from child pool, 
		//if there is corresponding parent in parent pool
		parentRemove(childNode, childPool, StringUtil.genIdxKey(childNode.getFromMethod(), childNode.getIdx()));
		childPool.remove(childNode);
	}
		
	public static void dataDepFromParentToChild(Map<Integer, InstNode> parentMap, 
			InstPool parentPool,
			InstPool childPool, 
			HashSet<Integer> firstReadLocalVars, 
			String childMethod) {
		HashMap<Integer, List<InstNode>> childSummary = new HashMap<Integer, List<InstNode>>();
		for (Integer f: firstReadLocalVars) {
			InstNode fInst = childPool.searchAndGet(childMethod, f);
			int idx =Integer.valueOf(fInst.getAddInfo());
			
			if (childSummary.containsKey(idx)) {
				childSummary.get(idx).add(fInst);
			} else {
				List<InstNode> insts = new ArrayList<InstNode>();
				insts.add(fInst);
				childSummary.put(idx, insts);
			}
		}
		
		for (Integer varKey: childSummary.keySet()) {
			List<InstNode> childInsts = childSummary.get(varKey);
			InstNode parentNode = parentMap.get(varKey);
			String parentKey = StringUtil.genIdxKey(parentNode.getFromMethod(), parentNode.getIdx());
			
			if (parentNode != null) {
				if (childInsts.size() == 1) {
					InstNode fInst = childInsts.get(0);
					transplantCalleeDepToCaller(parentNode, parentPool, fInst, childPool);
				} else if (childInsts.size() > 0) {
					int curPId = parentNode.getIdx();
					boolean expand = false;
					
					List<InstNode> allPPList = new ArrayList<InstNode>();
					List<InstNode> pChildList = new ArrayList<InstNode>();
					
					//Collect grand parent
					for (String dPParent: parentNode.getDataParentList()) {
						String[] dPParentKeys = StringUtil.parseIdxKey(dPParent);
						InstNode ppNode = parentPool.searchAndGet(dPParentKeys[0], 
								Integer.valueOf(dPParentKeys[1]));
						allPPList.add(ppNode);
					}
					for (String cPParent: parentNode.getControlParentList()) {
						String[] cPParentKeys = StringUtil.parseIdxKey(cPParent);
						InstNode cpNode = parentPool.searchAndGet(cPParentKeys[0], 
								Integer.valueOf(cPParentKeys[1]));
						allPPList.add(cpNode);
					}
					for (String cKey: parentNode.getChildFreqMap().keySet()) {
						String[] cKeys = StringUtil.parseIdxKey(cKey);
						InstNode cNode = parentPool.searchAndGet(cKeys[0], 
								Integer.valueOf(cKeys[1]));
						pChildList.add(cNode);
					}
					
					for (int i = 0; i < childInsts.size(); i++) {
						InstNode fInst = childInsts.get(i);
						InstNode copyParent = new InstNode(parentNode);
						copyParent.setIdx(curPId);
						
						if (i == 0) {
							parentPool.remove(parentNode);
						}
						
						if (!expand) {
							curPId *= 1000;
							expand = true;
						} else {
							curPId++;
						}
						
						for (InstNode pp: allPPList) {
							double freq = pp.getChildFreqMap().get(parentKey);
							pp.increChild(copyParent.getFromMethod(), copyParent.getIdx(), freq);
						}
						
						for (InstNode c: pChildList) {
							boolean control = BytecodeCategory.controlCategory().contains(c.getOp().getCatId());
							c.registerParent(copyParent.getFromMethod(), copyParent.getIdx(), control);
						}
						parentPool.add(copyParent);
						transplantCalleeDepToCaller(copyParent, parentPool, fInst, childPool);
					}
				}
			}
		}
		
		/*for (Integer f: firstReadLocalVars) {
			InstNode fInst = childPool.searchAndGet(childMethod, f);
			
			int idx = Integer.valueOf(fInst.getAddInfo());
			InstNode parentNode = null;
			//parent inherits children of same node in child method
			if (parentMap.containsKey(idx)) {
				parentNode = parentMap.get(idx);
				parentNode.getChildFreqMap().putAll(fInst.getChildFreqMap());
				String fInstKey = StringUtil.genIdxKey(fInst.getFromMethod(), fInst.getIdx());
				
				for (String c: fInst.getChildFreqMap().keySet()) {
					String[] keySet = StringUtil.parseIdxKey(c);
					int cIdx= Integer.valueOf(keySet[1]);
					InstNode cNode = childPool.searchAndGet(keySet[0], cIdx);
					cNode.getDataParentList().remove(fInstKey);
					
					if (parentNode != null) {
						cNode.registerParent(parentNode.getFromMethod(), parentNode.getIdx(), false);
					}
				}
				
				for (String cont: fInst.getControlParentList()) {
					String[] keySet = StringUtil.parseIdxKey(cont);
					int cIdx = Integer.valueOf(keySet[1]);
					InstNode contNode = childPool.searchAndGet(keySet[0], cIdx);
					double freq = contNode.getChildFreqMap().get(fInstKey);
					
					if (parentNode != null) {
						contNode.increChild(parentNode.getFromMethod(), parentNode.getIdx(), freq);
					}
				}
				
				//Remove these first read local vars from child pool, 
				//if there is corresponding parent in parent pool
				parentRemove(fInst, childPool, StringUtil.genIdxKey(fInst.getFromMethod(), fInst.getIdx()));
				childPool.remove(fInst);
			}
		}*/
	}
	
	public static void fieldDataDepFromParentToChild(Map<String, InstNode> parentMap, InstPool childPool, HashSet<Integer> firstReadFields, String childMethod) {
		for (Integer f: firstReadFields){
			InstNode fInst = childPool.searchAndGet(childMethod, f);
			
			if (parentMap.containsKey(fInst.getAddInfo())) {
				InstNode parentNode = parentMap.get(fInst.getAddInfo());
				
				parentNode.increChild(fInst.getFromMethod(), fInst.getIdx(), MIBConfiguration.getDataWeight());
				fInst.registerParent(parentNode.getFromMethod(), 
						parentNode.getIdx(), 
						false);
			}
		}
	}
	
	public static void controlDepFromParentToChild(InstNode controlFromParent, InstPool childPool) {
		for (InstNode cNode: childPool) {
			controlFromParent.increChild(cNode.getFromMethod(), cNode.getIdx(), MIBConfiguration.getControlWeight());
			cNode.registerParent(controlFromParent.getFromMethod(), controlFromParent.getIdx(), true);
		}
	}
	
	public static void removeReturnInst(InstPool pool) {
		Iterator<InstNode> poolIt = pool.iterator();
		InstNode returnInst = null;
		while (poolIt.hasNext()) {
			InstNode inst = poolIt.next();
			if (BytecodeCategory.returnOps().contains(inst.getOp().getOpcode())) {
				returnInst = inst;
			}
		}
		String returnInstKey = StringUtil.genIdxKey(returnInst.getFromMethod(), returnInst.getIdx());
		parentRemove(returnInst, pool, returnInstKey);
		pool.remove(returnInst);
	}
	
	public static void unionInstPools(InstPool parentPool, InstPool childPool) {
		Iterator<InstNode> poolIt = childPool.iterator();
		while (poolIt.hasNext()) {
			InstNode childInst = poolIt.next();
			parentPool.add(childInst);
		}
	}
	
	public static HashMap<Integer, GraphTemplate> collectChildGraphs(GraphTemplate parentGraph) {
		String tempDir = MIBConfiguration.getTemplateDir();
		TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
		
		HashMap<Integer, GraphTemplate> childGraphLib = new HashMap<Integer, GraphTemplate>();
		for (ExtObj eo: parentGraph.getExtMethods()) {
			int methodIdx = eo.getInstIdx();
			InstNode methodInst = parentGraph.getInstPool().searchAndGet(parentGraph.getMethodKey(), methodIdx);
			
			String filePath = tempDir + "/" + methodInst.getAddInfo() + ".json";
			GraphTemplate childGraph = TemplateLoader.loadTemplateFile(filePath, graphToken);
			if (childGraph != null)
				childGraphLib.put(methodIdx, childGraph);
		}
		return childGraphLib;
	}
	
	public static double roundValue(double value) {
		return Precision.round(value, MIBConfiguration.getPrecisionDigit());
	}
		
	private static void summarizeVarPairChildren(VarPairPool vpp, VarPair vp) {
		HashMap<String, Set<Var>> v1Map = vp.getVar1().getChildren();
		HashMap<String, Set<Var>> v2Map = vp.getVar2().getChildren();
		
		//First pass to construct all relationship for this vp
		for (String s1: v1Map.keySet()) {
			Set<Var> v1Set = v1Map.get(s1);
			Set<Var> v2Set = v2Map.get(s1);
			
			if (v2Set == null || v2Set.size() == 0)
				continue;
			
			for (Var v1: v1Set) {
				for (Var v2: v2Set) {
					if (v1.equals(v2))
						continue;
					
					VarPair childVp = vpp.searchVarPairPool(v1, v2, true);
					System.out.println("Child vp: " + childVp);
					//Add childVp as child of vp, add vp as parent of childVp
					VarPairPool.addVarPair(s1, vp, childVp, true);
					VarPairPool.addVarPair(s1, vp, childVp, false);
				}
			}
		}
	}
	
	public static void constructVarPairPool(VarPairPool vpp, VarPool vPool1, VarPool vPool2) {
		//First pass to generate all var pair in the pool
		for (Var v1: vPool1) {
			for (Var v2: vPool2) {
				vpp.searchVarPairPool(v1, v2, true);
			}
		}
		
		//Second pass, create relationship between VarPair
		//Avoid cycle
		for (VarPair vp: vpp) {
			System.out.println("Current vp: " + vp);
			summarizeVarPairChildren(vpp, vp);
		}
		
		//Update child and parent coefficient map
		for (VarPair vp: vpp) {
			VarPairPool.updateCoefficientMap(vp);
		}
	}
	
	public static void main (String[] args) {
		BigDecimal bd = new BigDecimal(0.11111);
		System.out.println(bd.setScale(3, RoundingMode.HALF_UP));
		System.out.println(Precision.round(0.11111, 3));
		System.out.println(Precision.round(0.1115,3));
		
	}

}
