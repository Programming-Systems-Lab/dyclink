package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

public class GraphTemplate {
		
	private String lastSecondInst;
	
	private HashMap<String, ArrayList<String>> invokeMethodLookup;

	private TreeMap<String, TreeSet<String>> dataGraph;
	
	private TreeMap<String, TreeSet<String>> controlGraph;
		
	public void setLastSecondInst(String lastSecondInst) {
		this.lastSecondInst = lastSecondInst;
	}
	
	public String getLastSecondInst() {
		return this.lastSecondInst;
	}
	
	public void setInvokeMethodLookup(HashMap<String, ArrayList<String>> invokeMethodLookup) {
		this.invokeMethodLookup = invokeMethodLookup;
	}
	
	public HashMap<String, ArrayList<String>> getInvokeMethodLookup() {
		return this.invokeMethodLookup;
	}
	
	public void setDataGraph(TreeMap<String, TreeSet<String>> dataGraph) {
		this.dataGraph = dataGraph;
	}
	
	public TreeMap<String, TreeSet<String>> getDataGraph() {
		return this.dataGraph;
	}
	
	public void setControlGraph(TreeMap<String, TreeSet<String>> controlGraph) {
		this.controlGraph = controlGraph;
	}
	
	public TreeMap<String, TreeSet<String>> getControlGraph() {
		return this.controlGraph;
	}

}
