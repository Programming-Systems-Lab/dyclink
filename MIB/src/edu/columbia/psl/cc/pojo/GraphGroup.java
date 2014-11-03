package edu.columbia.psl.cc.pojo;

import java.util.HashMap;

public class GraphGroup extends HashMap<String, GraphTemplate>{
		
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static String groupKey(GraphTemplate graph) {
		return groupKey(graph.getInstPool().size(), graph.getDepNum());
	}
	
	public static String groupKey(int nodeNum, int depNum) {
		return String.valueOf(nodeNum) + ":" + depNum;
	}
	
	public GraphTemplate getGraph(GraphTemplate graph) {
		String groupKey = groupKey(graph.getInstPool().size(), graph.getDepNum());
		return this.get(groupKey);
	}

	public void addGraph(GraphTemplate graph) {
		String groupKey = groupKey(graph.getInstPool().size(), graph.getDepNum());
		this.put(groupKey, graph);
	}
}
