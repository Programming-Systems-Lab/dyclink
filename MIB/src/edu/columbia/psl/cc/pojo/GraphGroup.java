package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

public class GraphGroup extends HashMap<String, List<GraphTemplate>>{
			
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static String groupKey(int linenumber, GraphTemplate graph) {
		return groupKey(linenumber, graph.getVertexNum(), graph.getEdgeNum());
	}
	
	public static String groupKey(int linenumber, int nodeNum, int depNum) {
		return String.valueOf(linenumber) + ":" + nodeNum + ":" + depNum;
	}
	
	public GraphTemplate getGraph(int linenumber, GraphTemplate graph) {		
		String groupKey = groupKey(linenumber, graph.getVertexNum(), graph.getEdgeNum());
		List<GraphTemplate> graphsInGroup = this.get(groupKey);
		
		if (graphsInGroup == null || graphsInGroup.size() == 0) {
			return null;
		} else {
			GraphTemplate existGraph = graphsInGroup.get(0);
			return existGraph;
		}
	}

	public void addGraph(int linenumber, GraphTemplate graph) {
		String groupKey = groupKey(linenumber, graph.getVertexNum(), graph.getEdgeNum());
		if (this.containsKey(groupKey)) {
			this.get(groupKey).add(graph);
		} else {
			List<GraphTemplate> subGraphs = new ArrayList<GraphTemplate>();
			subGraphs.add(graph);
			this.put(groupKey, subGraphs);
		}
	}
}
