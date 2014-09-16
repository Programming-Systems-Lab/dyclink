package edu.columbia.psl.cc.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import edu.columbia.psl.cc.datastruct.VarPairPool;
import edu.columbia.psl.cc.datastruct.VarPool;
import edu.columbia.psl.cc.pojo.Var;
import edu.columbia.psl.cc.pojo.VarPair;

public class SimilarityFlooding {
	
	private int maxRound;
	
	private double delta;
	
	private VarPairPool vpp = new VarPairPool();
	
	public VarPairPool getVarPairPool() {
		return this.vpp;
	}
	
	public void setMaxrRound(int maxRound) {
		this.maxRound = maxRound;
	}
	
	public void setDelta(double delta) {
		this.delta = delta;
	}
	
	public static VarPair findVarPairByVar2(HashMap<Var, VarPair>best, Var var2) {
		for (Var var1: best.keySet()) {
			VarPair tmpPair = best.get(var1);
			if (tmpPair.getVar2().equals(var2)) {
				return tmpPair;
			}
		}
		return null;
	}
	
	public static boolean allEngaged(Set<Var> cands) {
		for (Var v: cands) {
			if (!v.isEngaged())
				return false;
		}
		return true;
	}
	
	public static boolean allEngaged(HashMap<Var, List<VarPair>> cands) {
		//If all husbands are engaged, definitely true		
		for (Var v1: cands.keySet()) {
			List<VarPair> cList = cands.get(v1);
			if (!v1.isEngaged() && cList.size() > 0)
				return false;
		}
		return true;
	}
	
	public void constructVarPairPool(VarPool vPool1, VarPool vPool2) {
		//First pass to generate all var pair in the pool
		for (Var v1: vPool1) {
			for (Var v2: vPool2) {
				this.vpp.searchVarPairPool(v1, v2, true);
			}
		}
		
		//Second pass, create relationship between VarPair
		//Avoid cycle
		for (VarPair vp: this.vpp) {
			System.out.println("Current vp: " + vp);
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
			Set<Var> v1Set = v1Map.get(s1);
			Set<Var> v2Set = v2Map.get(s1);
			
			if (v2Set == null || v2Set.size() == 0)
				continue;
			
			for (Var v1: v1Set) {
				for (Var v2: v2Set) {
					if (v1.equals(v2))
						continue;
					
					VarPair childVp = this.vpp.searchVarPairPool(v1, v2, true);
					System.out.println("Child vp: " + childVp);
					//Add childVp as child of vp, add vp as parent of childVp
					VarPairPool.addVarPair(s1, vp, childVp, true);
					VarPairPool.addVarPair(s1, vp, childVp, false);
				}
			}
		}
	}
	
	public void convergeCalculation() {
		for (int i = 0; i < this.maxRound; i++) {
			this.vpp.updateVarPairSigma();
		}
	}
	
	public double getSimilaritySum() {
		double ret = 0;
		System.out.println("Check individual sigma");
		for (VarPair vp: this.vpp) {
			System.out.println(vp);
			System.out.println("Parent: " + vp.getAll(false));
			System.out.println("Child: " + vp.getAll(true));
			System.out.println(vp.getSigma());
			System.out.println();
			ret += vp.getSigma();
		}
		return ret;
	}
	
	public HashSet<VarPair> filter(HashSet<VarPair> oriSet, VarPair curParent) {
		HashSet<VarPair> ret = new HashSet<VarPair>();
		for (VarPair vp: oriSet) {
			if (vp.getVar1().equals(curParent.getVar1()) 
					|| vp.getVar2().equals(curParent.getVar2())) {
				continue ;
			}
			ret.add(vp);
		}
		return ret;
	}
	
	public Set<VarPair> exploreBackwardGraph(VarPair child, VarPair...curParent) {
		Set<VarPair> ret = new HashSet<VarPair>();
		
		HashSet<VarPair> parents = child.getAll(false);
		if (curParent.length > 0)
			parents = filter(parents, curParent[0]);
		if (parents.size() == 0) {
			ret.add(child);
			return ret;
		}
		
		for (VarPair p: parents) {
			Set<VarPair> pReturn = exploreBackwardGraph(p);
			ret.addAll(pReturn);
		}
		
		return ret;
	}
	
	public Set<VarPair> exploreForwardGraph(VarPair parent) {
		Set<VarPair> ret = new HashSet<VarPair>();
		
		HashSet<VarPair> children = parent.getAll(true);
		if (children.size() == 0) {
			//Trace back
			HashSet<VarPair> grand = parent.getAll(false);
			for (VarPair g: grand) {
				Set<VarPair> gReturn = this.exploreBackwardGraph(g, parent);				
				ret.addAll(gReturn);
			}
			ret.add(parent);
			return ret;
		}
		
		Set<VarPair> bestC = null;
		for (VarPair c: parent.getAll(true)) {
			Set<VarPair> cReturn = exploreForwardGraph(c);
			if (bestC == null)
				bestC = cReturn;
			else if (cReturn.size() > bestC.size())
				bestC = cReturn;
		}
		ret.add(parent);
		ret.addAll(bestC);
		return ret;
	}
	
	public void getSubGraph() {
		Set<VarPair> bestGraph = null;
		for (VarPair vp: this.vpp) {
			Set<VarPair> myGraph = this.exploreForwardGraph(vp);
			
			System.out.print("Current vp: " + vp);
			System.out.println(myGraph);
			
			if (bestGraph == null)
				bestGraph = myGraph;
			else if (myGraph.size() > bestGraph.size())
				bestGraph = myGraph;
		}
		System.out.println("Best graph size: " + bestGraph.size());
	}
	
	public void getMarried() {
		//First pass to flatten all varpair. V1-centric
		HashMap<Var, List<VarPair>> candMap = new HashMap<Var, List<VarPair>>();
		HashMap<Var, VarPair> bestMap = new HashMap<Var, VarPair>();
		HashSet<Var> husbands = new HashSet<Var>();
		HashSet<Var> wives = new HashSet<Var>();
		for (VarPair vp: this.vpp) {
			Var v1 = vp.getVar1();
			husbands.add(v1);
			wives.add(vp.getVar2());
			if (!candMap.containsKey(v1)) {
				List<VarPair> cands = new ArrayList<VarPair>();
				cands.add(vp);
				candMap.put(v1, cands);
			} else {
				candMap.get(v1).add(vp);
			}
		}
		
		/*HashSet<Var> totalEngagedSet;
		if (husbands.size() < wives.size()) {
			totalEngagedSet = husbands;
		} else if (wives.size() < husbands.size()) {
			totalEngagedSet = wives;
		} else {
			totalEngagedSet = new HashSet<Var>();
			totalEngagedSet.addAll(husbands);
			totalEngagedSet.addAll(wives);
		}*/
		
		while (!allEngaged(candMap)) {
			System.out.println("CandMap: " + candMap);
			for (Var v1: candMap.keySet()) {
				List<VarPair> cands = candMap.get(v1);
				if (cands == null || cands.size() == 0 || v1.isEngaged())
					continue;
				
				Collections.sort(cands, new CandSorter());

				//Get the best
				VarPair best = cands.remove(0);
				System.out.println("Var1: " + v1);
				System.out.println("Var2: " + best.getVar2());
				if (!best.getVar2().isEngaged()) {
					best.getVar2().setEngaged(true);
					v1.setEngaged(true);
					bestMap.put(v1, best);
					System.out.println("Insert: " + best + " " + best.getSigma());
				} else {
					//Find the VarPair that v2 belongs to
					VarPair curCouple = findVarPairByVar2(bestMap, best.getVar2());
					if (best.getSigma() > curCouple.getSigma()) {
						Var curHusband = curCouple.getVar1();
						bestMap.remove(curHusband);
						curHusband.setEngaged(false);
						v1.setEngaged(true);
						bestMap.put(v1, best);
						System.out.println("Update old: " + curCouple + " " + curCouple.getSigma());
						System.out.println("New: " + best + " " + best.getSigma());
					} else {
						System.out.println("No update: " + curCouple.getSigma() + " " + best.getSigma());
					}
				}
			}
			
			System.out.println("Husbands");
			for (Var h: husbands) {
				System.out.println(h + " engaged: " + h.isEngaged());
			}
			
			System.out.println("Wives");
			for (Var w: wives) {
				System.out.println(w + " engaged: " + w.isEngaged());
			}
			System.out.println();
		}
		
		//Print best match
		System.out.println("Traverse best match:");
		for (Var v1: bestMap.keySet()) {
			VarPair bMatch = bestMap.get(v1);
			System.out.println(bMatch + " " + bMatch.getSigma());
		}
	}
	
	public void cleanVarPool() {
		this.vpp = new VarPairPool();
	}
	
	public static class CandSorter implements Comparator<VarPair> {

		@Override
		public int compare(VarPair o1, VarPair o2) {
			if (o1.getSigma() < o2.getSigma())
				return 1;
			else if (o1.getSigma() > o2.getSigma())
				return -1;
			else
				return 0;
		}
		
	}
	
	public static void main(String[] args) {
		//Graph1
		VarPool vp1 = new VarPool();
		Var a = vp1.searchVar("graph1", "method1", 2, "1");
		Var a1 = vp1.searchVar("graph1", "method1", 1, "native:a1Var");
		Var a2 = vp1.searchVar("graph1", "method1", 1, "native:a2Var");
		
		a.addChildren(a1);
		a.addChildren(a2);
		a1.addChildren(a2);
		
		VarPool vp2 = new VarPool();
		Var b = vp2.searchVar("graph2", "method2", 2, "1");
		Var b1 = vp2.searchVar("graph2", "method2", 1, "native2:b1Var");
		Var b2 = vp2.searchVar("graph2", "method2", 1, "native2:b2Var");
		
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
		sf.setMaxrRound(20);
		sf.setDelta(0.1);
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
		sf.getSubGraph();
		/*System.out.println("Check sigma");
		sf.convergeCalculation();
		for (VarPair vp : vpp) {
			System.out.println(vp + " sigma: " + vp.getSigma());
		}
		sf.getMarried();
		System.out.println("Similarity sum: " + sf.getSimilaritySum());*/
	}

}
