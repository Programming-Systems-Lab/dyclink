package edu.columbia.psl.cc.infer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.datastruct.VarPool;
import edu.columbia.psl.cc.pojo.BlockNode;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.pojo.Var;
import edu.columbia.psl.cc.util.RelationManager;

public class DepInferenceEngine {
	
	private List<BlockNode> blocks = new ArrayList<BlockNode>();
	
	private VarPool vp = null;
		
	public void setBlocks(List<BlockNode> blocks) {
		this.blocks = blocks;
	}
	
	public List<BlockNode> getBlocks() {
		return this.blocks;
	} 
	
	public void setVarPool(VarPool vp) {
		this.vp = vp;
	}
	
	public VarPool getVarPool() {
		return this.vp;
	}
	
	public void executeInference() {
		//Backward for mining data dependency and extract control var from blocks
		for (BlockNode bn: this.blocks) {
			backwardInduct(bn);
		}
		
		for (BlockNode bn: this.blocks) {
			forwardInduct(bn);
		}
 	}
	
	private static boolean noInput(List<String> inList) {
		if (inList.size() == 0)
			return true;
		else
			return false;
	}
	
	private static void constructRelation(Var parentVar, InstNode childInst) {
		if (BytecodeCategory.readCategory().contains(childInst.getOp().getCatId())) {
			parentVar.addChildren(childInst.getVar(), RelationManager.getControlRead());
		} else if (BytecodeCategory.writeCategory().contains(childInst.getOp().getCatId())) {
			parentVar.addChildren(childInst.getVar(), RelationManager.getControlWrite());
		}
	}
	
	public static void forwardInduct(BlockNode bn) {
		List<Var> controlVars = bn.getControlDepVarsToChildren();
		Set<BlockNode> children = bn.getChildrenBlock();
		
		for (Var v: controlVars) {
			for (BlockNode child: children) {
				List<InstNode> insts = child.getInsts();
				
				for (InstNode inst: insts) {
					if (inst.getVar() == null)
						continue ;
					
					constructRelation(v, inst);
				}
			}
		}
	}
	
	public static void backwardInduct(BlockNode bn) {
		List<InstNode> insts = bn.getInsts();
		
		if (insts.size() == 0)
			return ;
		
		List<String> inferBuf = null;
		Map<Var, Set<Var>> depMap = new HashMap<Var, Set<Var>>();
		boolean shouldAnalyze = false;
		int extractControlNum = 0;
		Var curVar = null;
		List<Var> controlVars = new ArrayList<Var>();
		
		InstNode lastInst = insts.get(insts.size() - 1);
		int lastInstInSize = lastInst.getOp().getInList().size();
		
		if (BytecodeCategory.controlCategory().contains(lastInst.getOp().getCatId()) 
				&& lastInstInSize > 0) {
			extractControlNum = lastInstInSize;
			shouldAnalyze = true;
		}
		
		//From the end
		for (int i = insts.size() - 1; i >= 0; i--) {
			InstNode inst = insts.get(i);
			
			int opcat = inst.getOp().getCatId();
			if (BytecodeCategory.writeCategory().contains(opcat)) {
				shouldAnalyze = true;
			}
			
			if (shouldAnalyze) {
				List<String> instInput = inst.getOp().getInList();			
				List<String> instOutput = inst.getOp().getOutList();
				
				if (inferBuf == null) {
					inferBuf = new ArrayList<String>();
					inferBuf.addAll(instInput);
					
					curVar = inst.getVar();
					if (!depMap.containsKey(curVar) && curVar != null) {
						Set<Var> parents = new HashSet<Var>();
						depMap.put(curVar, parents);
					}
					continue;
				}
				
				if (!noInput(instInput)) {
					inferBuf.remove(inferBuf.size() - 1);
					inferBuf.addAll(instInput);
				} else {
					//If the inst is load, it might affect curVar
					if (inst.isLoad()) {
						if (curVar != null) {
							depMap.get(curVar).add(inst.getVar());
							inst.getVar().addChildren(curVar);
						}
						
						if (extractControlNum > 0) {
							controlVars.add(inst.getVar());
							extractControlNum--;
						}
					}
					inferBuf.remove(inferBuf.size() - 1);
				}
				
				if (inferBuf.size() == 0) {
					inferBuf = null;
					curVar = null;
					shouldAnalyze = false;
				}
			}
		}
		
		bn.setDataDepMap(depMap);
		bn.setControlDepVarsToChildren(controlVars);
		
		//Check potential node
		System.out.println("Block: " + bn.getLabel());
		System.out.println("Data dependency");
		for (Var v: bn.getDataDepMap().keySet()) {
			System.out.println("Var: " + v);
			System.out.println("Parents:");
			for (Var p: depMap.get(v)) {
				System.out.println("  " + p);
			}
		}
		
		System.out.println("Control vars");
		for (Var v: bn.getControlDepVarsToChildren()) {
			System.out.println(v);
		}
		System.out.println();
	}

}
