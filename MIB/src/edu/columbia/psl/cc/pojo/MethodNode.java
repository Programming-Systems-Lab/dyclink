package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import edu.columbia.psl.cc.util.StringUtil;

public class MethodNode extends InstNode {
	
	private static Logger logger = Logger.getLogger(MethodNode.class);
	
	private static double domPass = 0.8;
	
	private CalleeInfo calleeInfo = new CalleeInfo();
	
	private HashMap<String, GraphWithFreq> callees = new HashMap<String, GraphWithFreq>();
	
	private int maxGraphFreq = 0;
	
	public static HashMap<GraphTemplate, Double> extractCallee(HashMap<String, GraphWithFreq> callees, 
			int maxGraphFreq) {
		
		/*logger.info("Callee graph original");
		for (String key: callees.keySet()) {
			logger.info(key+ " " + callees.get(key).freq); 
		}*/
		
		int totalFreq = 0;
		for (GraphWithFreq graF: callees.values()) {
			totalFreq += graF.freq;
		}
		
		HashMap<GraphTemplate, Double> ret = new HashMap<GraphTemplate, Double>();
		if (maxGraphFreq >= domPass * totalFreq) {
			for (GraphWithFreq graF: callees.values()) {
				if (graF.freq == maxGraphFreq) {
					ret.put(graF.callee, 1.0);
					return ret;
				}
			}
			
			logger.error("Cannot find graph with matched freq");
			return null;
		}
		
		double meanFreq = ((double)totalFreq)/callees.values().size();
		double stdSum = 0;
		for (GraphWithFreq graF: callees.values()) {
			stdSum += Math.pow(graF.freq - meanFreq, 2);
		}
		double std = Math.sqrt((stdSum) / (callees.values().size() - 1));
		double lowBound = meanFreq - std;
		//int lowBoundInt = (int)(lowBound + 0.5);
		
		List<GraphWithFreq> cache = new ArrayList<GraphWithFreq>();
		int newTotal = 0;
		for (GraphWithFreq graF: callees.values()) {
			if (graF.freq >= lowBound) {
				cache.add(graF);
				newTotal += graF.freq;
			}
		}
		
		logger.info("Callee graph filtering");
		for (GraphWithFreq graF: cache) {
			double frac = ((double)graF.freq)/newTotal;
			ret.put(graF.callee, frac);
			logger.info(graF.callee.getVertexNum() + ":" + graF.callee.getEdgeNum() + " " + frac);
		}
		return ret;
	}
	
	public void setCalleeInfo(CalleeInfo calleeInfo) {
		this.calleeInfo = calleeInfo;
	}
	
	public CalleeInfo getCalleeInfo() {
		return this.calleeInfo;
	}
	
	public void registerParentReplay(int idx, InstNode instParent) {
		String parentString = StringUtil.genIdxKey(instParent.getThreadId(), 
				instParent.getThreadMethodIdx(), 
				instParent.getIdx());
		
		if (this.calleeInfo.parentReplay == null) {
			this.calleeInfo.parentReplay = new HashMap<Integer, HashSet<String>>();
		}
		
		if (this.calleeInfo.parentReplay.containsKey(idx)) {
			this.calleeInfo.parentReplay.get(idx).add(parentString);
		} else {
			HashSet<String> parentSet = new HashSet<String>();
			parentSet.add(parentString);
			this.calleeInfo.parentReplay.put(idx, parentSet);
		}
	}
	
	/*public void registerChildReplace(InstNode lastBeforeReturn) {
		String lastString = StringUtil.genIdxKey(lastBeforeReturn.getThreadId(), 
				lastBeforeReturn.getThreadMethodIdx(), 
				lastBeforeReturn.getIdx());
		
		this.calleeInfo.childIdx = lastString;
	}*/
	
	public void registerDomCalleeIdx(String domCalleeIdx, 
			double normFreq, 
			InstNode lastBeforeReturn) {
		//this.calleeInfo.domCalleeIdx = domCalleeIdx;
		MetaGraph mg = new MetaGraph();
		mg.calleeIdx = domCalleeIdx;
		mg.normFreq = normFreq;
		
		if (lastBeforeReturn != null) {
			String lastString = StringUtil.genIdxKey(lastBeforeReturn.getThreadId(), 
					lastBeforeReturn.getThreadMethodIdx(), 
					lastBeforeReturn.getIdx());
			mg.lastInstString = lastString;
		}
		
		if (this.calleeInfo.metaCallees == null) {
			this.calleeInfo.metaCallees = new ArrayList<MetaGraph>();
		}
		
		this.calleeInfo.metaCallees.add(mg);
	}
		
	public void registerCallee(GraphTemplate callee) {
		//No need for linenumber actually.
		String groupKey = GraphGroup.groupKey(this.getLinenumber(), callee);
		GraphWithFreq gf = null;
		if (this.callees.containsKey(groupKey)) {
			//Can update the update time inst here. 
			//Not helpful for current approach, so save some time.
			gf = this.callees.get(groupKey);
			gf.freq++;
		} else {
			gf = new GraphWithFreq();
			gf.callee = callee;
			gf.freq = 1;
			this.callees.put(groupKey, gf);
		}
		
		if (gf.freq > this.maxGraphFreq) {
			this.maxGraphFreq = gf.freq;
		}
	}
	
	public void clearCallees() {
		this.callees.clear();
	}
		
	public HashMap<String, GraphWithFreq> getCallees() {
		return this.callees;
	}
	
	public int getMaxCalleeFreq() {
		return this.maxGraphFreq;
	}
	
	public static class GraphWithFreq {
		public GraphTemplate callee;
		
		public int freq = 0;
	}
	
	public static class CalleeInfo {
		
		public List<MetaGraph> metaCallees;
		
		public HashMap<Integer, HashSet<String>> parentReplay;
		
	}
	
	public static class MetaGraph {
		public String calleeIdx;
		
		public double normFreq;
		
		public String lastInstString;
	}
}
