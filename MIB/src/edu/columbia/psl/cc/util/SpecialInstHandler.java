package edu.columbia.psl.cc.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.infer.DepInferenceEngine;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.Var;

public class SpecialInstHandler {
	
	private static String idx = "idx";
	
	public static boolean isIdx(String onStack) {
		if (onStack.equals(idx))
			return true;
		else
			return false;
	}
	
	public static void handleDup(InstNode inst, List<String> inferBuf) {
		int removeCount = inst.getOp().getOutList().size() - inst.getOp().getInList().size();
		
		ListIterator<String> inferIT = inferBuf.listIterator();
		while (inferIT.hasNext() && removeCount > 0) {
			inferIT.next();
			inferIT.remove();
			removeCount--;
		}		
	}
	
	public static int locateParentVarForArrayLoad(InstNode inst, List<InstNode> insts, int idx, List<Var> parentVars) {
		List<String> simulateBuf = new ArrayList<String>();
		simulateBuf.addAll(inst.getOp().getInList());
		
		int count = 0;
		for (int i = idx - 1; i >= 0; i--) {
			InstNode curInst = insts.get(i);
			simulateBuf.remove(simulateBuf.size() - 1);
			if (DepInferenceEngine.noInput(curInst.getOp().getInList())) {
				simulateBuf.addAll(inst.getOp().getInList());
			} else if (curInst.isLoad() && simulateBuf.size() >= 0) {
				parentVars.add(curInst.getVar());
			}
			count++;
			
			if (simulateBuf.size() == 0)
				break ;
 		}
		return count;
	}
	
	public static int locateChildVarForArrayStore(InstNode inst, List<InstNode> insts, int idx) {		
		List<String> simulateBuf = new ArrayList<String>();
		simulateBuf.addAll(inst.getOp().getInList());
		
		Var childVar = null;
		Set<Var> parentVars = new HashSet<Var>();
		int ret = 0;
		boolean shouldRecord = true;
		
		//The instruction that changes "value" is the parent
		for (int i = idx - 1; i >= 0; i--) {
			InstNode curInst = insts.get(i);
			
			if (simulateBuf.size() == 2)
				shouldRecord = false;
			
			if (BytecodeCategory.dupCategory().contains(curInst.getOp().getCatId())) {
				handleDup(curInst, simulateBuf);
			} else {
				simulateBuf.remove(simulateBuf.size() - 1);
				if (!DepInferenceEngine.noInput(curInst.getOp().getInList())) {
					simulateBuf.addAll(curInst.getOp().getInList());
				} else if (curInst.isLoad() && shouldRecord){
					parentVars.add(curInst.getVar());
				}
			} 
			
			ret++;
			
			if (simulateBuf.size() == 0) {
				childVar = curInst.getVar();
				break ;
			}
		}
		
		if (childVar == null)
			System.err.println("Cannot locate child var for inst: " + inst);
		
		for (Var v: parentVars) {
			v.addChildren(childVar);
		}
		return ret;
	}
}
