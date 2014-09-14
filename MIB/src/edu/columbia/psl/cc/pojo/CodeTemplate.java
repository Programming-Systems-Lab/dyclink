package edu.columbia.psl.cc.pojo;

import java.util.HashSet;

import edu.columbia.psl.cc.datastruct.VarPool;

public class CodeTemplate {
	
	private String catSequence;
	
	private String charSequence;
	
	private VarPool vars;
	
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
	
	public void setVars(VarPool vars) {
		this.vars = vars;
	}
	
	public VarPool getVars() {
		return this.vars;
	}

}
