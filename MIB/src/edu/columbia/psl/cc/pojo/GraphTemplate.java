package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

public class GraphTemplate {
	
	private int methodArgSize;
	
	private int methodReturnSize;
		
	private InstNode lastSecondInst;
	
	private HashMap<InstNode, ArrayList<InstNode>> invokeMethodLookup;

	private TreeMap<InstNode, TreeSet<InstNode>> dataGraph;
	
	private TreeMap<InstNode, TreeSet<InstNode>> controlGraph;
	
	public void setMethodArgSize(int methodArgSize) {
		this.methodArgSize = methodArgSize;
	}
	
	public int getMethodArgSize() {
		return this.methodArgSize;
	}
	
	public void setMethodReturnSize(int methodReturnSize) {
		this.methodReturnSize = methodReturnSize;
	} 
	
	public int getMethodReturnSize() {
		return this.methodReturnSize;
	}
 		
	public void setLastSecondInst(InstNode lastSecondInst) {
		this.lastSecondInst = lastSecondInst;
	}
	
	public InstNode getLastSecondInst() {
		return this.lastSecondInst;
	}
	
	public void setInvokeMethodLookup(HashMap<InstNode, ArrayList<InstNode>> invokeMethodLookup) {
		this.invokeMethodLookup = invokeMethodLookup;
	}
	
	public HashMap<InstNode, ArrayList<InstNode>> getInvokeMethodLookup() {
		return this.invokeMethodLookup;
	}
	
	public void setDataGraph(TreeMap<InstNode, TreeSet<InstNode>> dataGraph) {
		this.dataGraph = dataGraph;
	}
	
	public TreeMap<InstNode, TreeSet<InstNode>> getDataGraph() {
		return this.dataGraph;
	}
	
	public void setControlGraph(TreeMap<InstNode, TreeSet<InstNode>> controlGraph) {
		this.controlGraph = controlGraph;
	}
	
	public TreeMap<InstNode, TreeSet<InstNode>> getControlGraph() {
		return this.controlGraph;
	}

}
