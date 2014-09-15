package edu.columbia.psl.cc.util;

import java.util.HashMap;
import java.util.Set;

import edu.columbia.psl.cc.datastruct.VarPairPool;
import edu.columbia.psl.cc.datastruct.VarPool;
import edu.columbia.psl.cc.pojo.Var;
import edu.columbia.psl.cc.pojo.VarPair;

public class SimilarityFlooding {
	
	private VarPairPool vpp = new VarPairPool();
	
	public VarPairPool getVarPairPool() {
		return this.vpp;
	}
	
	public void constructVarPairPool(VarPool vPool1, VarPool vPool2) {
		//First pass to generate all var pair in the pool
		for (Var v1: vPool1) {
			for (Var v2: vPool2) {
				this.vpp.searchVarPairPool(v1, v2);
			}
		}
		
		//Second pass, create relationship between VarPair
		//Avoid cycle
		for (VarPair vp: this.vpp) {
			this.summarizeVarPairChildren(vp);
		}
		
		//Update child and parent coefficient map
		for (VarPair vp: this.vpp) {
			VarPairPool.updateCoefficientMap(vp);
		}
	}
	
	private void summarizeVarPairChildren(VarPair vp) {
		HashMap<String, Set<Var>> v1Map = vp.getVar1().getChildren();
		HashMap<String, Set<Var>> v2Map = vp.getVar2().getChildren();
		
		//First pass to construct all relationship for this vp
		for (String s1: v1Map.keySet()) {
			Set<Var> v1List = v1Map.get(s1);
			Set<Var> v2List = v2Map.get(s1);
			
			if (v2List == null || v2List.size() == 0)
				continue;
			
			for (Var v1: v1List) {
				for (Var v2: v2List) {
					if (v1.equals(v2))
						continue;
					
					VarPair childVp = this.vpp.searchVarPairPool(v1, v2);
					//Add childVp as child of vp, add vp as parent of childVp
					VarPairPool.addVarPair(s1, vp, childVp, true);
					VarPairPool.addVarPair(s1, vp, childVp, false);
				}
			}
		}
	}
	
	public static void main(String[] args) {
		//Graph1
		VarPool vp1 = new VarPool();
		Var a = vp1.searchVar(10, "graph1", "method1", 2, "1");
		Var a1 = vp1.searchVar(11, "graph1", "method1", 1, "native:a1Var");
		Var a2 = vp1.searchVar(12, "graph1", "method1", 1, "native:a2Var");
		
		a.addChildren(a1);
		a.addChildren(a2);
		a1.addChildren(a2);
		
		VarPool vp2 = new VarPool();
		Var b = vp2.searchVar(10, "graph2", "method2", 2, "1");
		Var b1 = vp2.searchVar(11, "graph2", "method2", 1, "native2:b1Var");
		Var b2 = vp2.searchVar(12, "graph2", "method2", 1, "native2:b2Var");
		
		b.addChildren(b1);
		b.addChildren(b2);
		b2.addChildren(b1);
		
		System.out.println("Check var size: " + vp1.size());
		for (Var v: vp1) {
			if (v.getChildren().size() > 0) {
				System.out.print("Source: " + v + "->");
			} else {
				continue;
			}
			
			for (String edge: v.getChildren().keySet()) {
				System.out.println(edge);
				Set<Var> edgeChildren = v.getChildren().get(edge);
				for (Var ev: edgeChildren) {
					System.out.println("->" + "Sink: " +  ev);
				}
			}
		}
		
		System.out.println("Check var size: " + vp2.size());
		for (Var v: vp2) {
			if (v.getChildren().size() > 0) {
				System.out.print("Source: " + v + "->");
			} else {
				continue;
			}
			
			for (String edge: v.getChildren().keySet()) {
				System.out.println(edge);
				Set<Var> edgeChildren = v.getChildren().get(edge);
				for (Var ev: edgeChildren) {
					System.out.println("->" + "Sink: " +  ev);
				}
			}
		}
		
		SimilarityFlooding sf = new SimilarityFlooding();
		sf.constructVarPairPool(vp1, vp2);
		VarPairPool vpp = sf.getVarPairPool();
		for (VarPair tmp1: vpp) {
			if (tmp1.getChildren().size() > 0) {
				System.out.print("Source: " + tmp1 + "->");
			} else {
				continue;
			}
			
			HashMap<String, Set<VarPair>> childrenMap = tmp1.getChildren();
			for (String edge: childrenMap.keySet()) {
				System.out.println(edge);
				Set<VarPair> edgeChildren = tmp1.getChildren().get(edge);
				for (VarPair vp: edgeChildren) {
					System.out.println("->" + "Sink:" + vp);
				}
			}
		}
		
		System.out.println("Check sigma");
		//Temporarily let it run for 5 times
		for (int i = 0; i < 20; i++) {
			vpp.updateVarPairSigma();
			for (VarPair vp: vpp) {
				System.out.println(vp + " sigma: " + vp.getSigma());
			}
			System.out.println();
		}		
	}

}
