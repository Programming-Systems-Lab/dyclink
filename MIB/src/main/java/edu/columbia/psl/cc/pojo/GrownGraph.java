package edu.columbia.psl.cc.pojo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GrownGraph extends GraphTemplate{
	
	private Map<Integer, Integer> keyMethods = new HashMap<Integer, Integer>();
	
	public GrownGraph() {
		
	}
	
	public GrownGraph(GraphTemplate copy) {
		super(copy);
	}
	
	public void updateKeyMethods(int methodIdx, int lineNum) {
		this.keyMethods.put(methodIdx, lineNum);
	}
	
	public Map<Integer, Integer> getKeyMethods() {
		return this.keyMethods;
	}
	
	public Collection<Integer> getKeyLines() {
		return this.keyMethods.values();
	}

}
