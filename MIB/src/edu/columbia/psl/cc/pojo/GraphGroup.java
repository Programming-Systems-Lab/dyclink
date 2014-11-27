package edu.columbia.psl.cc.pojo;

import java.util.HashMap;

import org.apache.log4j.Logger;

public class GraphGroup extends HashMap<String, GraphTemplate>{
	
	private static Logger logger = Logger.getLogger(GraphGroup.class);
		
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static String groupKey(GraphTemplate graph) {
		return groupKey(graph.getVertexNum(), graph.getEdgeNum());
	}
	
	public static String groupKey(int nodeNum, int depNum) {
		return String.valueOf(nodeNum) + ":" + depNum;
	}
	
	public GraphTemplate getGraph(GraphTemplate graph) {
		String groupKey = groupKey(graph.getVertexNum(), graph.getEdgeNum());
		GraphTemplate existGraph = this.get(groupKey);
		
		if (existGraph == null) {
			return null;
		} else if (!existGraph.getLatestWriteFields().keySet().equals(graph.getLatestWriteFields().keySet())) {
			logger.info("Capture similar graph but write fields not matched");
			logger.info("Exist graph: " + existGraph.getLatestWriteFields().keySet());
			logger.info("Current graph: " + graph.getLatestWriteFields().keySet());
			return null;
		} else {
			return existGraph;
		}
	}

	public void addGraph(GraphTemplate graph) {
		String groupKey = groupKey(graph.getVertexNum(), graph.getEdgeNum());
		this.put(groupKey, graph);
	}
}
