package edu.columbia.psl.cc.pojo;

import java.util.HashMap;
import java.util.Map;

import edu.columbia.psl.cc.inst.BlockAnalyzer.Block;

public class StaticMethodMiner {
	
	private String opCatString;
	
	private int[] opCatFreq;
	
	private HashMap<String, Integer> labelMap;
	
	private HashMap<String, Block> blockMap;
	
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
	
	public void setBlockMap(HashMap<String, Block> blockMap) {
		this.blockMap = blockMap;
	}
	
	public HashMap<String, Block> getBlockMap() {
		return this.blockMap;
	}

}
