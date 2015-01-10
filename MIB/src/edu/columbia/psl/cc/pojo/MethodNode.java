package edu.columbia.psl.cc.pojo;

import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;

import edu.columbia.psl.cc.util.StringUtil;

public class MethodNode extends InstNode {
	
	private static Logger logger = Logger.getLogger(MethodNode.class);
	
	private CalleeInfo calleeInfo = new CalleeInfo();
	
	private HashMap<String, GraphWithFreq> callees = new HashMap<String, GraphWithFreq>();
	
	private int maxGraphFreq = 0;
	
	public static GraphTemplate extractCallee(HashMap<String, GraphWithFreq> callees, int maxGraphFreq) {
		GraphTemplate ret = null;
		for (GraphWithFreq graF: callees.values()) {
			if (graF.freq == maxGraphFreq) {
				if (ret == null) {
					ret = graF.callee;
				} else {
					if (graF.callee.getVertexNum() >= ret.getVertexNum() 
							&& graF.callee.getEdgeNum() > ret.getEdgeNum()) {
						ret = graF.callee;
					}
				}
			}
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
	
	public void registerChildReplace(InstNode lastBeforeReturn) {
		String lastString = StringUtil.genIdxKey(lastBeforeReturn.getThreadId(), 
				lastBeforeReturn.getThreadMethodIdx(), 
				lastBeforeReturn.getIdx());
		
		this.calleeInfo.childIdx = lastString;
	}
	
	public void registerDomCalleeIdx(String domCalleeIdx) {
		this.calleeInfo.domCalleeIdx = domCalleeIdx;
	}
		
	public void registerCallee(GraphTemplate callee) {
		String groupKey = GraphGroup.groupKey(this.getLinenumber(), callee);
		GraphWithFreq gf = null;
		if (this.callees.containsKey(groupKey)) {
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
		
		public String domCalleeIdx;
		
		public HashMap<Integer, HashSet<String>> parentReplay;
		
		public String childIdx;
	}
}
