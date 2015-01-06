package edu.columbia.psl.cc.pojo;

import java.util.HashMap;

public class MethodNode extends InstNode {
	
	//0 for calledTime, 1 for updateTime
	private HashMap<String, long[]> calleeInfo = new HashMap<String, long[]>();
	
	public void registerCallee(String graphId, 
			long calledTime) {
		long[] info = null;
		
		if (this.calleeInfo.containsKey(graphId)) {
			info = this.calleeInfo.get(graphId);
			info[1] = calledTime;
		} else {
			info = new long[2];
			info[0] = calledTime;
			info[1] = calledTime;
			this.calleeInfo.put(graphId, info);
		}
	}
	
	public void setCalleeInfo(HashMap<String, long[]> calleeInfo) {
		this.calleeInfo = calleeInfo;
	}
	
	public HashMap<String, long[]> getCalleeInfo() {
		return this.calleeInfo;
	}
}
