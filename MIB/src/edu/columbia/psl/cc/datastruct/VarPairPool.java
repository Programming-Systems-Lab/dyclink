package edu.columbia.psl.cc.datastruct;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import edu.columbia.psl.cc.pojo.Var;
import edu.columbia.psl.cc.pojo.VarPair;

public class VarPairPool extends LinkedHashSet<VarPair>{
	
	private static final long serialVersionUID = 1L;
	
	public static void addVarPair(String label, VarPair parent, VarPair child, boolean isAddChildren) {
		if (isAddChildren) {
			parent.updateChildren(label, child);
		} else {
			child.updateParents(label, parent);
		}
	}
	
	public static void updateCoefficientMap(VarPair vp) {		
		//Summarize all children for all labels
		HashSet<VarPair> allChildren = vp.getAll(true);
		if (allChildren.size() > 0) {
			double childWeight = ((double)1)/allChildren.size();
			for (VarPair cp: allChildren) {
				vp.updateChildCoefficient(cp, childWeight);
			}
		}
		
		//Summarize all parents for all labels
		HashSet<VarPair> allParents = vp.getAll(false);
		
		if (allParents.size() > 0) {
			double parentWeight = ((double)1)/allParents.size();
			for (VarPair pp: allParents) {
				vp.updateParentCoefficient(pp, parentWeight);
			}
		} else {
			System.err.println(vp + " got no parents");
		}
	}

	private boolean checkExistence(VarPair vp, Var v1, Var v2) {
		VarPair tmp = new VarPair(v1, v2);
		if (vp.equals(tmp))
			return true;
		else
			return false;
	}
	
	public double[] genSimilarityArray() {
		double[] ret = new double[this.size()];
		int count = 0;
		for (VarPair vp: this) {
			ret[count++] = vp.getSigma();
		}
		return ret;
	}
	
	public VarPair searchVarPairPool(Var v1, Var v2, boolean createNew) {
		for (VarPair vp: this) {
			if (this.checkExistence(vp, v1, v2))
				return vp;
		}
		
		if (createNew) {
			//Create new
			VarPair newVp = new VarPair(v1, v2);
			this.add(newVp);
			return newVp;
		} else {
			return null;
		}
	}
	
	public HashSet<VarPair> searchVarPairByVar1(Var v1) {
		HashSet<VarPair> ret = new HashSet<VarPair>();
		for (VarPair vp: this) {
			if (vp.getVar1().equals(v1)) {
				ret.add(vp);
			}
		}
		return ret;
	}
	
	public void updateVarPairSigma() {
		double maxSigma = 0;
		for (VarPair vp: this) {
			HashMap<String, Set<VarPair>> parents = vp.getParents();
			HashMap<String, Set<VarPair>> children = vp.getChildren();
			if ((parents == null || parents.size() == 0) 
					&& (children == null || children.size() == 0))
				continue;
			
			for (String label: parents.keySet()) {
				Set<VarPair> pUnderLabel = parents.get(label);
				
				for (VarPair p: pUnderLabel) {
					vp.increSigma(p.getChildCoefficient(vp));
				}
			}
			
			for (String label: children.keySet()) {
				Set<VarPair> cUnderLabel = children.get(label);
				
				for (VarPair c: cUnderLabel) {
					vp.increSigma(c.getParentCoefficient(vp));
				}
			}
			double curSigma = vp.getSigma();
			if (curSigma > maxSigma)
				maxSigma = curSigma;
		}
		
		//Normalize sigma
		for (VarPair vp: this) {
			vp.normalizeSigma(maxSigma);
		}
	}

}
