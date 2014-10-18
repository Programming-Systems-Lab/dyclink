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
	
	private static void parentRemove(InstNode inst, InstPool pool, String instKey) {
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
		
	public static void dataDepFromParentToChild(Map<Integer, InstNode> parentMap, InstPool childPool, HashSet<Integer> firstReadLocalVars, String childMethod) {
		for (Integer f: firstReadLocalVars) {
			InstNode fInst = childPool.searchAndGet(childMethod, f);
			
			int idx = Integer.valueOf(fInst.getAddInfo());
			InstNode parentNode = null;
			if (parentMap.containsKey(idx)) {
				parentNode = parentMap.get(idx);
				parentNode.getChildFreqMap().putAll(fInst.getChildFreqMap());
			}
			
			for (String c: fInst.getChildFreqMap().keySet()) {
				String[] keySet = StringUtil.parseIdxKey(c);
				int cIdx= Integer.valueOf(keySet[1]);
				InstNode cNode = childPool.searchAndGet(keySet[0], cIdx);
				cNode.getDataParentList().remove(StringUtil.genIdxKey(fInst.getFromMethod(), fInst.getIdx()));
				
				if (parentNode != null) {
					cNode.registerParent(parentNode.getFromMethod(), parentNode.getIdx(), false);
				}
			}
		}
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
