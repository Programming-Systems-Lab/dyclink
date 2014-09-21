package edu.columbia.psl.cc.infer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.pojo.BlockNode;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.Var;

public class DepInferenceEngine {
	
	private static String noString = "no";
	
	private static boolean noInput(List<String> inList) {
		String pivot = inList.get(0);
		if (pivot.equals(noString)) {
			return true;
		} else {
			return false;
		}
	}
	
	public static void backwardInduct(BlockNode bn) {
		List<InstNode> insts = bn.getInsts();
		
		if (insts.size() == 0)
			return ;
		
		List<String> inferBuf = null;
		Map<Var, Set<Var>> depMap = new HashMap<Var, Set<Var>>();
		boolean shouldAnalyze = false;
		Var curVar = null;
		//From the end
		for (int i = insts.size() - 1; i >= 0; i--) {
			InstNode inst = insts.get(i);
			
			if (BytecodeCategory.writeCategory().contains(inst.getOp().getCatId())) {
				shouldAnalyze = true;
			}
			
			if (shouldAnalyze) {
				List<String> instInput = inst.getOp().getInList();			
				List<String> instOutput = inst.getOp().getOutList();
				
				if (inferBuf == null) {
					inferBuf = new ArrayList<String>();
					inferBuf.addAll(instInput);
					
					curVar = inst.getVar();
					if (!depMap.containsKey(curVar)) {
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
						depMap.get(curVar).add(inst.getVar());
					}
					inferBuf.remove(inferBuf.size() - 1);
				}
				
				if (inferBuf.size() == 0) {
					inferBuf = null;
					curVar = null;
				}
			}
		}
		
		//Check potential node
		for (Var v: depMap.keySet()) {
			System.out.println("Var: " + v);
			System.out.println("Parents:");
			for (Var p: depMap.get(v)) {
				System.out.println("  " + p);
			}
		}
	}

}
