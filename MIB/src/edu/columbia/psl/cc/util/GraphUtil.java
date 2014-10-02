package edu.columbia.psl.cc.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.datastruct.VarPairPool;
import edu.columbia.psl.cc.datastruct.VarPool;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.pojo.Var;
import edu.columbia.psl.cc.pojo.VarPair;

public class GraphUtil {
	
	/*public InstNode parseInstNode(String rawInst, String methodName) {
		String label = StringUtil.parseElement(rawInst, 0);
		int opcode = Integer.valueOf(StringUtil.parseElement(rawInst, 2));
		OpcodeObj opObj = BytecodeCategory.getOpcodeObj(opcode);
		
		InstNode inst = new InstNode();
		inst.setOp(opObj);
		inst.setThisMethodName(methodName);
		inst.setRawInst(rawInst);
		
		return inst;
	}
	
	public void instantizetGraph(GraphTemplate graphTemplate) {
		TreeMap<String, TreeSet<String>> rawGraph = graphTemplate.getDataGraph();
	}
	
	public void unrollGraph(GraphTemplate graph, HashMap<String, GraphTemplate> graphLib) {
		HashMap<String, ArrayList<String>> invokeMethodLookup = graph.getInvokeMethodLookup();
		
		List<GraphTemplate> childGraphs = new ArrayList<GraphTemplate>();
		
		//Find children graph
		for (String mkey: invokeMethodLookup.keySet()) {
			ArrayList<String> parentInsts = invokeMethodLookup.get(mkey);
			
			GraphTemplate childGraph = graphLib.get(mkey);
			int startIdx = parentInsts.size() - childGraph.getMethodArgSize();
			
			for (int i = startIdx; startIdx < parentInsts.size(); i++) {
				String parentInst = parentInsts.get(i);
			}
		}
	}*/
	
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

}
