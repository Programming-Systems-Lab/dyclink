package edu.columbia.psl.cc.pojo;

import java.util.HashMap;
import java.util.Map;

public class StaticRep {
	
	private boolean template = false;

	private String opCatString;
	
	private int[] opCatFreq;
	
	private HashMap<String, Integer> labelMap;
	
	public void setTemplate(boolean template) {
		this.template = template;
	}
	
	public boolean isTemplate() {
		return this.template;
	}
	
	public void setOpCatString(String opCatString) {
		this.opCatString = opCatString;
	}
	
	public String getOpCatString() {
		return this.opCatString;
	}
	
	public void setOpCatFreq(int[] opCatFreq) {
		this.opCatFreq = opCatFreq;
	}
	
	public int[] getOpCatFreq() {
		return this.opCatFreq;
	}
	
	public void setLabelMap(HashMap<String, Integer> labelMap) {
		this.labelMap = labelMap;
	}
	
	public HashMap<String, Integer> getLabelMap() {
		return this.labelMap;
	}

}
