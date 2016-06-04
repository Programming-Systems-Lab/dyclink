package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.List;

public class LocalVar extends Var{
	
	private int localVarId;
	
	private List<LabelInterval> intervals = new ArrayList<LabelInterval>();
	
	public void setLocalVarId(int localVarId) {
		this.localVarId = localVarId;
	}
	
	public int getLocalVarId() {
		return this.localVarId;
	}
	
	public void addLabelInterval(LabelInterval newInterval) {
		this.intervals.add(newInterval);
	}
	
	public List<LabelInterval> getIntervals() {
		return this.intervals;
	}
}
