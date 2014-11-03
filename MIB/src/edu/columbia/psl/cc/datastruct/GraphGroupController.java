package edu.columbia.psl.cc.datastruct;

import java.util.HashMap;
import java.util.Map;

import edu.columbia.psl.cc.pojo.GraphGroup;
import edu.columbia.psl.cc.pojo.GraphTemplate;

public class GraphGroupController {
	
	//Key will be searchKye-objId, if static, objId is 0
	private static Map<String, GraphGroup> graphCache = new HashMap<String, GraphGroup>();
	
	public static GraphGroup getGraphGroup(String searchKey) {
		return graphCache.get(searchKey);
	}
	
	public static GraphTemplate getSimilarGraph(String searchKey, GraphTemplate curGraph) {
		if (graphCache.containsKey(searchKey)) {
			GraphGroup gGroup = graphCache.get(searchKey);
			return gGroup.getGraph(curGraph);
		}
		return null;
	}
	
	public synchronized static void insertNewGraphGroup(String searchKey, GraphGroup newGroup) {
		graphCache.put(searchKey, newGroup);
	}

}
