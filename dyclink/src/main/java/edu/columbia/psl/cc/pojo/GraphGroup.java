package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.columbia.psl.cc.abs.AbstractGraph;

public class GraphGroup extends HashMap<String, List<AbstractGraph>>{
			
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static String groupKey(int linenumber, AbstractGraph graph) {
		return groupKey(linenumber, graph.getVertexNum(), graph.getEdgeNum());
	}
	
	public static String groupKey(int linenumber, int nodeNum, int depNum) {
		return String.valueOf(linenumber) + ":" + nodeNum + ":" + depNum;
	}
	
	public AbstractGraph getGraph(int linenumber, AbstractGraph graph) {		
		String groupKey = groupKey(linenumber, graph.getVertexNum(), graph.getEdgeNum());
		List<AbstractGraph> graphsInGroup = this.get(groupKey);
		
		if (graphsInGroup == null || graphsInGroup.size() == 0) {
			return null;
		} else {
			AbstractGraph existGraph = graphsInGroup.get(0);
			return existGraph;
		}
	}

	public void addGraph(int linenumber, AbstractGraph graph) {
		String groupKey = groupKey(linenumber, graph.getVertexNum(), graph.getEdgeNum());
		if (this.containsKey(groupKey)) {
			this.get(groupKey).add(graph);
		} else {
			List<AbstractGraph> subGraphs = new ArrayList<AbstractGraph>();
			subGraphs.add(graph);
			this.put(groupKey, subGraphs);
		}
	}
}
