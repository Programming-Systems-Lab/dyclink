package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

public class GraphGroup extends HashMap<String, List<GraphTemplate>>{
	
	private static Logger logger = Logger.getLogger(GraphGroup.class);
		
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
		if (graph.getLatestWriteFields().size() > 0) {
			logger.info("Graph writes fields. Choose not to replace: " + graph.getMethodKey() + " " + graph.getThreadId() + " " + graph.getThreadMethodId());
			return null;
		}
		
		String groupKey = groupKey(linenumber, graph.getVertexNum(), graph.getEdgeNum());
		List<GraphTemplate> graphsInGroup = this.get(groupKey);
		
		if (graphsInGroup == null || graphsInGroup.size() == 0) {
			return null;
		} else {
			GraphTemplate existGraph = graphsInGroup.get(0);
			return existGraph;
		}
		
		/*if (existGraph == null) {
			return null;
		} else {
			return existGraph;
		}*/
		
		/*if (existGraph == null) {
			return null;
		} else if (!existGraph.getLatestWriteFields().keySet().equals(graph.getLatestWriteFields().keySet())) {
			logger.info("Capture similar graph but write fields not matched");
			logger.info("Exist graph: " + existGraph.getLatestWriteFields().keySet());
			logger.info("Current graph: " + graph.getLatestWriteFields().keySet());
			return null;
		} else {
			return existGraph;
		}*/
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
