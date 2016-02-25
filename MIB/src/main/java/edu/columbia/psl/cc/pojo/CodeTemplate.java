package edu.columbia.psl.cc.pojo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import edu.columbia.psl.cc.datastruct.VarPool;

public class CodeTemplate {
	
	private String catSequence;
	
	private String charSequence;
	
	private HashSet<Var> vars;
	
	public void setCatSequence(String catSequence) {
		this.catSequence = catSequence;
	}
	
	public String getCatSequence() {
		return this.catSequence;
	}
	
	public void setCharSequence(String charSequence) {
		this.charSequence = charSequence;
	}
	
	public String getCharSequence() {
		return this.charSequence;
	}
	
	public void setVars(HashSet<Var> vars) {
		this.vars = vars;
	}
	
	public HashSet<Var> getVars() {
		return this.vars;
	}
	
	public Var searchVarByRep(String rep) {
		for (Var v: this.vars) {
			if (v.toString().equals(rep))
				return v;
		}
		System.err.println("CodeTemplate: Cannot find var for " + rep);
		return null;
	}
	
	/**
	 * Re-construct the relationships between vars after deserialization
	 */
	public void reconstructVars() {
		for (Var v1:  this.vars) {
			HashMap<String, Set<String>> childrenRepMap = v1.getChildrenRep();
			
			for (String label: childrenRepMap.keySet()) {
				Set<String> repSet = childrenRepMap.get(label);
				
				for (String s: repSet) {
					Var c = this.searchVarByRep(s);
					if (c != null) {
						v1.addChildren(c);
					}
				}
			}
		}
	}

}
