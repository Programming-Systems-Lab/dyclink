package edu.columbia.psl.cc.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.util.MathUtils;
import org.apache.commons.math3.util.Precision;
import org.objectweb.asm.Opcodes;

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
import edu.columbia.psl.cc.pojo.SurrogateInst;
import edu.columbia.psl.cc.pojo.Var;
import edu.columbia.psl.cc.pojo.VarPair;

public class GraphUtil {
	
	public static List<InstNode> sortInstPool(InstPool instPool, boolean byStartTime) {
		Comparator<InstNode> comp = null;
		
		if (byStartTime) {
			comp = new Comparator<InstNode>() {
				@Override
				public int compare(InstNode i1, InstNode i2) {
					return i1.getStartTime() > i2.getStartTime()?1:(i1.getStartTime() < i2.getStartTime()?-1:0);
				}
			};
		} else {
			comp = new Comparator<InstNode>() {
				
				@Override
				public int compare(InstNode i1, InstNode i2) {
					return i1.getUpdateTime() > i2.getUpdateTime()?1:(i2.getUpdateTime() > i1.getUpdateTime()?-1: 0);
				}
			};
		}
		
		List<InstNode> sortedList = new ArrayList<InstNode>(instPool);
		Collections.sort(sortedList, comp);
		return sortedList;
	}
	
	public static InstNode lastSecondInst(InstPool instPool) {
		if (instPool.size() == 0)
			return null;
		
		/*Comparator<InstNode> updateComp = new Comparator<InstNode>() {
			
			@Override
			public int compare(InstNode i1, InstNode i2) {
				return i1.getUpdateTime() > i2.getUpdateTime()?1:(i2.getUpdateTime() > i1.getUpdateTime()?-1: 0);
			}
		};
		List<InstNode> sortedList = new ArrayList<InstNode>(instPool);
		Collections.sort(sortedList, updateComp);*/
		
		List<InstNode> sortedList = sortInstPool(instPool, false);
		return sortedList.get(sortedList.size() - 1);
	}
	
	public static int reindexInstPool(int base, InstPool instPool) {
		int maxStartTime = 0;
		for (InstNode inst: instPool) {
			inst.setStartTime(base + inst.getStartTime());
			inst.setUpdateTime(base + inst.getUpdateTime());
			
			if (inst.getStartTime() > maxStartTime)
				maxStartTime = inst.getStartTime();
		}
		return maxStartTime + 1;
	}
	
	private static void _parentRemove(String parentKey, InstPool pool, String removeKey) {
		String[] parentParsed = StringUtil.parseIdxKey(parentKey);
		InstNode inst = pool.searchAndGet(parentParsed[0], Integer.valueOf(parentParsed[1]));
		inst.getChildFreqMap().remove(removeKey);
	}
	
	private static InstNode _retrieveRealInst(String instKey, InstPool pool) {
		String[] instKeys = StringUtil.parseIdxKey(instKey);
		InstNode instNode = pool.searchAndGet(instKeys[0], 
				Integer.valueOf(instKeys[1]));
		return instNode;
	}
	
	public static void parentRemove(InstNode inst, InstPool pool, String instKey) {
		//Remove from inst data parent if any
		for (String dp: inst.getInstDataParentList()) {
			_parentRemove(dp, pool, instKey);
		}
		
		//Remove from write data parent if any
		for (String dp: inst.getWriteDataParentList()) {
			System.out.println("Child node: " + inst);
			System.out.println("Remove from parent node: " + dp);
			_parentRemove(dp, pool, instKey);
		}
		
		//Remove from control parent if any
		for (String cp: inst.getControlParentList()) {
			_parentRemove(cp, pool, instKey);
		}
	}
	
	public static void transplantCalleeDepToCaller(InstNode parentNode, 
			InstNode childNode, 
			InstPool childPool) {
		String fInstKey = StringUtil.genIdxKey(childNode.getFromMethod(), childNode.getIdx());
		
		for (String c: childNode.getChildFreqMap().keySet()) {
			double freq = childNode.getChildFreqMap().get(c);
			String[] keySet = StringUtil.parseIdxKey(c);
			int cIdx= Integer.valueOf(keySet[1]);
			parentNode.increChild(keySet[0], cIdx, freq);
			
			InstNode cNode = childPool.searchAndGet(keySet[0], cIdx);
			//Try to remove in either inst data or write data parent
			boolean fromInstData = cNode.getInstDataParentList().remove(fInstKey);
			boolean fromWriteData = false;
			if (!fromInstData)
				fromWriteData = cNode.getWriteDataParentList().remove(fInstKey);
			
			if (fromInstData && fromWriteData)
				System.out.println("Warning: double data deps: " + fInstKey);
			
			if (parentNode != null) {
				if (fromInstData)
					cNode.registerParent(parentNode.getFromMethod(), parentNode.getIdx(), MIBConfiguration.INST_DATA_DEP);
				else
					cNode.registerParent(parentNode.getFromMethod(), parentNode.getIdx(), MIBConfiguration.WRITE_DATA_DEP);
			}
		}
		
		for (String cont: childNode.getControlParentList()) {
			String[] keySet = StringUtil.parseIdxKey(cont);
			int cIdx = Integer.valueOf(keySet[1]);
			InstNode contNode = childPool.searchAndGet(keySet[0], cIdx);
			System.out.println("Attempt: " + cont);
			System.out.println("Child node: " + fInstKey);
			System.out.println("Parent node: " + parentNode);
			double freq = contNode.getChildFreqMap().get(fInstKey);
			
			if (parentNode != null) {
				contNode.increChild(parentNode.getFromMethod(), parentNode.getIdx(), freq);
				parentNode.registerParent(contNode.getFromMethod(), contNode.getIdx(), MIBConfiguration.CONTR_DEP);
			}
			System.out.println("Check parentNode cont parent: " + parentNode.getControlParentList());
			System.out.println("Check cont parent child list: " + contNode.getChildFreqMap());
		}
		
		//Remove these first read local vars from child pool, 
		//if there is corresponding parent in parent pool
		parentRemove(childNode, childPool, StringUtil.genIdxKey(childNode.getFromMethod(), childNode.getIdx()));
		childPool.remove(childNode);
	}
	
	public static HashSet<InstNode> retrieveChildInsts(InstNode inst, InstPool pool) {
		HashSet<InstNode> allChildren = new HashSet<InstNode>();
		for (String cKey: inst.getChildFreqMap().keySet()) {
			InstNode cNode = _retrieveRealInst(cKey, pool);
			allChildren.add(cNode);
		}
		return allChildren;
	}
	
	/**
	 * For constructing surrogate branch, only inst data parents are required
	 * @param inst
	 * @param pool
	 * @param forSurrogate
	 * @return
	 */
	public static HashSet<InstNode> retrieveRequiredParentInsts(InstNode inst, InstPool pool, int depType) {
		HashSet<InstNode> allParents = new HashSet<InstNode>();
		
		if (depType == MIBConfiguration.CONTR_DEP) {
			for (String cPParent: inst.getControlParentList()) {
				InstNode cpNode = _retrieveRealInst(cPParent, pool);
				allParents.add(cpNode);
			}
		} else if (depType == MIBConfiguration.INST_DATA_DEP) {
			for (String dPParent: inst.getInstDataParentList()) {
				InstNode ppNode = _retrieveRealInst(dPParent, pool);
				allParents.add(ppNode);
			}
		} else if (depType == MIBConfiguration.WRITE_DATA_DEP) {
			for (String dPParent: inst.getWriteDataParentList()) {
				InstNode ppNode = _retrieveRealInst(dPParent, pool);
				allParents.add(ppNode);
			}
		}
		
		return allParents;
	}
	
	private static SurrogateInst generateSurrogate(InstNode inst, InstPool pool) {
		//possible read: xload, getfield, getstatic, all method calls from jvm
		String instKey = StringUtil.genIdxKey(inst.getFromMethod(), inst.getIdx());
		
		//For the first surrogate, don't copy parent for it. It's for storing children in method
		boolean shouldCopyParent = true;
		SurrogateInst newSur = new SurrogateInst(inst);
		if (inst.probeSurrogate() == 0) {
			inst.setMaxSurrogate((inst.getIdx() + 1 ) * MIBConfiguration.getInstance().getIdxExpandFactor());
			shouldCopyParent = false;
			
			newSur.getInstDataParentList().clear();
			newSur.getWriteDataParentList().clear();
			newSur.getControlParentList().clear();
			newSur.getChildFreqMap().clear();
			newSur.setIdx(inst.getIdx());
		} else {
			newSur.setIdx(inst.getMaxSurrogate());
			newSur.getInstDataParentList().clear();
			pool.add(newSur);
		}
		
		inst.addSurrogateInst(newSur);
		
		if (shouldCopyParent) {
			HashSet<InstNode> controlSet = retrieveRequiredParentInsts(inst, pool, MIBConfiguration.CONTR_DEP);
			for (InstNode instNode: controlSet) {
				double freq = instNode.getChildFreqMap().get(instKey);
				instNode.increChild(newSur.getFromMethod(), newSur.getIdx(), freq);
				newSur.registerParent(instNode.getFromMethod(), instNode.getIdx(), MIBConfiguration.CONTR_DEP);
			}
			
			HashSet<InstNode> writeSet = retrieveRequiredParentInsts(inst, pool, MIBConfiguration.WRITE_DATA_DEP);
			for (InstNode instNode: writeSet) {
				double freq = instNode.getChildFreqMap().get(instKey);
				instNode.increChild(newSur.getFromMethod(), newSur.getIdx(), freq);
				newSur.registerParent(instNode.getFromMethod(), instNode.getIdx(), MIBConfiguration.WRITE_DATA_DEP);
			}
			
			HashSet<InstNode> instParents = retrieveRequiredParentInsts(inst, pool, MIBConfiguration.INST_DATA_DEP);
			for (InstNode instParent: instParents) {
				SurrogateInst surParent = generateSurrogate(instParent, pool);
				
				surParent.increChild(newSur.getFromMethod(), newSur.getIdx(), MIBConfiguration.getInstance().getInstDataWeight());
				newSur.registerParent(surParent.getFromMethod(), surParent.getIdx(), MIBConfiguration.INST_DATA_DEP);
			}
		}
		
		return newSur;
	}
		
	public static void dataDepFromParentToChild(Map<Integer, InstNode> parentMap, 
			InstPool parentPool,
			InstPool childPool, 
			HashSet<Integer> firstReadLocalVars, 
			String childMethod) {
		HashMap<Integer, List<InstNode>> childSummary = new HashMap<Integer, List<InstNode>>();
		for (Integer f: firstReadLocalVars) {
			InstNode fInst = childPool.searchAndGet(childMethod, f);
			//It's possible that fInst is null, probably an aload for method node, which should be removed
			if (fInst == null)
				continue ;
			
			int idx = Integer.valueOf(fInst.getAddInfo());
			
			if (parentMap.containsKey(idx)) {
				if (childSummary.containsKey(idx)) {
					childSummary.get(idx).add(fInst);
				} else {
					List<InstNode> insts = new ArrayList<InstNode>();
					insts.add(fInst);
					childSummary.put(idx, insts);
				}
			}
		}
		
		for (Integer varKey: childSummary.keySet()) {
			List<InstNode> childInsts = childSummary.get(varKey);
			InstNode parentNode = parentMap.get(varKey);
			
			if (parentNode != null) {
				for (InstNode cInst: childInsts) {
					String cInstKey = StringUtil.genIdxKey(cInst.getFromMethod(), cInst.getIdx());
					
					//Check if the surrogate exists in parent pool
					SurrogateInst surrogate = parentNode.searchSurrogateInst(cInstKey);
					if (surrogate != null) {
						transplantCalleeDepToCaller(surrogate, cInst, childPool);
					} else {
						surrogate = generateSurrogate(parentNode, parentPool);
						surrogate.setRelatedChildMethodInst(cInstKey);
						transplantCalleeDepToCaller(surrogate, cInst, childPool);
					}
				}
				
				/*if (initSur) {
					//Transplant the first surrogate's children in method to parentNode
					int removeSurId = parentNode.getIdx() * MIBConfiguration.getInstance().getIdxExpandFactor();
					InstNode minSur = parentPool.searchAndGet(parentNode.getFromMethod(), removeSurId);
					SurrogateInst decMinSur = (SurrogateInst)minSur;
					
					//Create a surrogate for parent itself
					SurrogateInst mySur = new SurrogateInst(parentNode);
					mySur.setRelatedChildMethodInst(decMinSur.getRelatedChildMethodInst());
					transplantCalleeDepToCaller(mySur, decMinSur, childPool);
					
					parentNode.addSurrogateInst(mySur);
					parentNode.getSurrogateInsts().remove(decMinSur);
					parentPool.remove(minSur);
					
					//transplantCalleeDepToCaller(parentNode, my)
				}*/
			}
		}
	}
	
	/**
	 * This step is done right before dumping graph
	 * Transplant the surrogate to the real inst in pool
	 * @param pool
	 */
	public static void transplantFirstSurrogate(InstPool pool) {
		for (InstNode inst: pool) {
			if (inst.getSurrogateInsts().size() == 0)
				continue ;
			
			for (SurrogateInst si: inst.getSurrogateInsts()) {
				if (si.getIdx() == inst.getIdx()) {					
					//This si is only possible have control parent and data child (inst)
					for (String childInst: si.getChildFreqMap().keySet()) {
						double freq = si.getChildFreqMap().get(childInst);
						String[] cDecomp = StringUtil.parseIdxKey(childInst);
						inst.increChild(cDecomp[0], Integer.valueOf(cDecomp[1]), freq);
					}
					
					for (String controlP: si.getControlParentList()) {
						String[] conDecomp = StringUtil.parseIdxKey(controlP);
						inst.registerParent(conDecomp[0], Integer.valueOf(conDecomp[1]), MIBConfiguration.CONTR_DEP);
					}
					
					//Almost impossible to have write data dep
					for (String writeP: si.getWriteDataParentList()) {
						String[] writeDecomp = StringUtil.parseIdxKey(writeP);
						inst.registerParent(writeDecomp[0], Integer.valueOf(writeDecomp[1]), MIBConfiguration.WRITE_DATA_DEP);
					}
				}
			}
		}
	}
	
	public static void fieldDataDepFromParentToChild(Map<String, InstNode> parentMap, 
			InstPool childPool, 
			HashSet<Integer> firstReadFields, 
			String childMethod) {
		for (Integer f: firstReadFields){
			InstNode fInst = childPool.searchAndGet(childMethod, f);
			//fInst is possible to be null, like alod
			if (fInst == null)
				continue ;
			
			if (parentMap.containsKey(fInst.getAddInfo())) {
				InstNode parentNode = parentMap.get(fInst.getAddInfo());
				
				parentNode.increChild(fInst.getFromMethod(), 
						fInst.getIdx(), 
						MIBConfiguration.getInstance().getWriteDataWeight());
				fInst.registerParent(parentNode.getFromMethod(), 
						parentNode.getIdx(), 
						MIBConfiguration.WRITE_DATA_DEP);
			}
		}
	}
	
	public static void controlDepFromParentToChild(InstNode controlFromParent, InstPool childPool) {
		List<InstNode> sortedChild = sortInstPool(childPool, true);
		
		HashSet<InstNode> affectedSet = new HashSet<InstNode>();
		for (InstNode inst: sortedChild) {
			affectedSet.add(inst);
			
			//Stop at the first control node in child method
			if (BytecodeCategory.controlCategory().contains(inst.getOp().getCatId()) 
					|| inst.getOp().getOpcode() == Opcodes.TABLESWITCH 
					|| inst.getOp().getOpcode() == Opcodes.LOOKUPSWITCH) {
				break;
			}
		}
		
		for (InstNode cNode: affectedSet) {
			controlFromParent.increChild(cNode.getFromMethod(), 
					cNode.getIdx(), 
					MIBConfiguration.getInstance().getControlWeight());
			cNode.registerParent(controlFromParent.getFromMethod(), controlFromParent.getIdx(), MIBConfiguration.CONTR_DEP);
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
	
	public static void unionInst(String instParent, 
			InstNode parentNode, InstNode childNode, 
			InstPool parentPool, InstPool childPool, int depType) {
		String[] keys = StringUtil.parseIdxKey(instParent);
		InstNode instParentNode = childPool.searchAndGet(keys[0], Integer.valueOf(keys[1]));
		String childInstKey = StringUtil.genIdxKey(childNode.getFromMethod(), childNode.getIdx());
		if (!parentNode.getInstDataParentList().contains(instParent)) {
			parentNode.registerParent(instParentNode.getFromMethod(), instParentNode.getIdx(), depType);
		}
		double freq = instParentNode.getChildFreqMap().get(childInstKey);
		instParentNode.increChild(parentNode.getFromMethod(), parentNode.getIdx(), freq);
	}
	
	public static void unionInstPools(InstPool parentPool, InstPool childPool) {
		Iterator<InstNode> poolIt = childPool.iterator();
		while (poolIt.hasNext()) {
			InstNode childInst = poolIt.next();
			
			if (parentPool.contains(childInst)) {
				InstNode sameInst = parentPool.searchAndGet(childInst.getFromMethod(), childInst.getIdx());
				
				for (String instParent: childInst.getInstDataParentList()) {
					unionInst(instParent, sameInst, childInst, parentPool, childPool, MIBConfiguration.INST_DATA_DEP);
				}
				
				for (String instParent: childInst.getWriteDataParentList()) {
					unionInst(instParent, sameInst, childInst, parentPool, childPool, MIBConfiguration.WRITE_DATA_DEP);
				}
				
				for (String instParent: childInst.getControlParentList()) {
					unionInst(instParent, sameInst, childInst, parentPool, childPool, MIBConfiguration.CONTR_DEP);
				}
				
				for (String ccKey: childInst.getChildFreqMap().keySet()) {
					String[] ccKeys = StringUtil.parseIdxKey(ccKey);
					double freq = childInst.getChildFreqMap().get(ccKey);
					
					String ccMethod = ccKeys[0];
					int ccIdx = Integer.valueOf(ccKeys[1]);
					sameInst.increChild(ccMethod, ccIdx, freq);
					
					//No need to register parent, because sameInst and childInst are the same
				}
			} else {
				parentPool.add(childInst);
			}
		}
	}
	
	public static double roundValue(double value) {
		return Precision.round(value, MIBConfiguration.getInstance().getPrecisionDigit());
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
