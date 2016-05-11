package edu.columbia.psl.cc.pojo;

import java.util.HashMap;
import java.util.HashSet;

import edu.columbia.psl.cc.abs.AbstractGraph;
import edu.columbia.psl.cc.util.TraceAnalyzer.MethodTrace;

public class GraphTemplate extends AbstractGraph {
	
	private InstNode lastBeforeReturn;
	
	public transient MethodTrace methodTrace;
	
	public transient HashMap<String, HashSet<String>> fieldRelations;
	
	public transient boolean mustExist = false;
		
	public void setLastBeforeReturn(InstNode lastBeforeReturn) {
		this.lastBeforeReturn = lastBeforeReturn;
	}
	
	public InstNode getLastBeforeReturn() {
		return this.lastBeforeReturn;
	}
	
	public void showGraph() {
		for (InstNode inst: this.pool) {
			System.out.println("Parent inst: " + inst);
			
			for (String cInst: inst.getChildFreqMap().navigableKeySet()) {
				System.out.println(" " + cInst + " " + inst.getChildFreqMap().get(cInst));
			}
		}
	}

}
