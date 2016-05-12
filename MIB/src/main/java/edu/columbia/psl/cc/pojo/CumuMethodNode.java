package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.HashMap;

import edu.columbia.psl.cc.abs.AbstractGraph;
import edu.columbia.psl.cc.pojo.MethodNode.MetaGraph;
import edu.columbia.psl.cc.util.StringUtil;

public class CumuMethodNode extends MethodNode {
	
	private HashMap<AbstractGraph, Integer> calleesDirect = new HashMap<AbstractGraph, Integer>();
	
	private HashMap<String, Integer> jvmCallees = new HashMap<String, Integer>();
	
	public static HashMap<AbstractGraph, Double> extractCallee(MethodNode ptr) {
		CumuMethodNode mn = (CumuMethodNode) ptr;
		
		HashMap<AbstractGraph, Double> ret = new HashMap<AbstractGraph, Double>();
		HashMap<AbstractGraph, Integer> graphFreqs = mn.calleesDirect;
		int totalFreq = mn.getMaxCalleeFreq();
		
		for (AbstractGraph g: graphFreqs.keySet()) {
			int gFreq = graphFreqs.get(g);
			double frac = ((double)gFreq)/totalFreq;
			frac = roundHelper(frac);
			double diff = Math.abs(frac - 0);
			if (diff > EPSILON) {
				ret.put(g, frac);
			}
		}
		
		return ret;
	}
	
	@Override
	public void registerCallee(AbstractGraph callee) {
		this.increCallFreq();
		if (this.calleesDirect.containsKey(callee)) {
			int newFreq = this.calleesDirect.get(callee) + 1;
			this.calleesDirect.put(callee, newFreq);
		} else {
			this.calleesDirect.put(callee, 1);
		}
	}
	
	public void registerJVMCallee(String jvmMethod) {
		this.increCallFreq();
		if (this.jvmCallees.containsKey(jvmMethod)) {
			int newFreq = this.jvmCallees.get(jvmMethod) + 1;
			this.jvmCallees.put(jvmMethod, newFreq);
		} else {
			this.jvmCallees.put(jvmMethod, 1);
		}
	}
	
	public void registerDomCalleeIdx(String domCalleeIdx, double normFreq) {
		//this.calleeInfo.domCalleeIdx = domCalleeIdx;
		MetaGraph mg = new MetaGraph();
		mg.calleeIdx = domCalleeIdx;
		mg.normFreq = normFreq;
		
		/*if (lastBeforeReturn != null) {
			String lastString = StringUtil.genIdxKey(lastBeforeReturn.getThreadId(), 
					lastBeforeReturn.getThreadMethodIdx(), 
					lastBeforeReturn.getIdx());
			mg.lastInstString = lastString;
		}*/
		
		if (this.calleeInfo.metaCallees == null) {
			this.calleeInfo.metaCallees = new ArrayList<MetaGraph>();
		}
		
		this.calleeInfo.metaCallees.add(mg);
	}
	
	@Override
	public void clearCallees() {
		this.calleesDirect.clear();
		this.jvmCallees.clear();
	}
	
	public HashMap<String, Integer> getJvmCallees() {
		return this.jvmCallees;
	}

}
