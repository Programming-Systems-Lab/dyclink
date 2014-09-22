package edu.columbia.psl.cc.util;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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
		while (removeCount > 0 && inferIT.hasNext()) {
			inferIT.remove();
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
		List<Var> parentVars = new ArrayList<Var>();
		int ret = 0;
		boolean shouldRecord = true;
		//The instruction that changes "value" is the parent
		for (int i = idx - 1; i >= 0; i--) {
			InstNode curInst = insts.get(i);
			System.out.println("Current inst: " + curInst);
			
			if (simulateBuf.size() == 2)
				shouldRecord = false;
			
			System.out.println("IsLoad: " + curInst.isLoad());
			System.out.println("Should record: " + shouldRecord);
			simulateBuf.remove(simulateBuf.size() - 1);
			if (!DepInferenceEngine.noInput(curInst.getOp().getInList())) {
				simulateBuf.addAll(inst.getOp().getInList());
			} else if (curInst.isLoad() && shouldRecord){
				System.out.println("Get a parent var: " + curInst.getVar());
				parentVars.add(curInst.getVar());
			}
			ret++;
			
			if (simulateBuf.size() == 0) {
				System.out.println("Child var: " + curInst);
				childVar = curInst.getVar();
				break ;
			}
		}
		
		System.out.println("Child var: " + childVar);
		System.out.println("Parent var: " + parentVars);
		
		if (childVar == null)
			System.err.println("Cannot locate child var for inst: " + inst);
		
		for (Var v: parentVars) {
			v.addChildren(childVar);
		}
		return ret;
	}
}
