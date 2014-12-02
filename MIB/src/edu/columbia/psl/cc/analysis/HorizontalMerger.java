package edu.columbia.psl.cc.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.GraphGroup;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.NameMap;
import edu.columbia.psl.cc.util.GlobalRecorder;
import edu.columbia.psl.cc.util.GraphUtil;
import edu.columbia.psl.cc.util.GsonManager;
import edu.columbia.psl.cc.util.TemplateLoader;

public class HorizontalMerger {
	
	private static Logger logger = Logger.getLogger(HorizontalMerger.class);
	
	private static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
	private static TypeToken<NameMap> nameMapToken = new TypeToken<NameMap>(){};
	
	private static Comparator<GraphTemplate> graphSizeSorter = new Comparator<GraphTemplate>() {
		@Override
		public int compare(GraphTemplate g1, GraphTemplate g2) {			
			if (g1.getVertexNum() < g2.getVertexNum()) {
				return 1;
			} else if (g1.getVertexNum() > g2.getVertexNum()) {
				return -1;
			} else {
				if (g1.getEdgeNum() < g2.getEdgeNum())
					return 1;
				else if (g1.getEdgeNum() > g2.getEdgeNum())
					return -1;
				else {
					return 0;
				}
			}
		}
	};
	
	private static HashSet<String> cacheLatestGraphs(int dirIdx, 
			HashSet<String> recursiveMethods) {
		String dirString = "";
		if (dirIdx == 0) {
			dirString = MIBConfiguration.getInstance().getTemplateDir();
		} else if (dirIdx == 1) {
			dirString = MIBConfiguration.getInstance().getTestDir();
		} else {
			logger.error("Invalid dir idx: " + dirIdx);
			return null;
		}
		
		File dir = new File(dirString);
		
		HashSet<String> allNames = TemplateLoader.loadAllFileNames(dir);
		Iterator<String> allNameIt = allNames.iterator();
		while (allNameIt.hasNext()) {
			String name = allNameIt.next();
			if (recursiveMethods.contains(name)) {
				logger.info("Recursive method: " + name + ", no need for extraction");
				GsonManager.cacheGraph(name, dirIdx, true);
				allNameIt.remove();
			} else {
				GsonManager.cacheGraph(name, dirIdx, false);
			}
		}		
		return allNames;
	}
	
	/**
	 * Extract representative graph for a single method
	 * @param graphSet
	 * @return
	 */
	private static GraphTemplate extractRepGraph(HashSet<GraphTemplate> graphSet) {
		if (graphSet.size() == 1) {
			GraphTemplate ret = graphSet.iterator().next();
			return ret;
		}
		
		HashMap<String, List<GraphTemplate>> stats = new HashMap<String, List<GraphTemplate>>();
		for (GraphTemplate graph: graphSet) {
			String groupKey = GraphGroup.groupKey(graph);
			if (stats.containsKey(groupKey)) {
				stats.get(groupKey).add(graph);
			} else {
				List<GraphTemplate> statList = new ArrayList<GraphTemplate>();
				statList.add(graph);
				stats.put(groupKey, statList);
			}
		}
		logger.info("Graph groups with freq: ");
		for (String groupKey: stats.keySet()) {
			logger.info(groupKey + ": " + stats.get(groupKey).size());
		}
		
		//Pick the one with smallest thread method id
		List<GraphTemplate> toMerge = new ArrayList<GraphTemplate>();
		List<Integer> weights = new ArrayList<Integer>();
		for (String groupKey: stats.keySet()) {
			List<GraphTemplate> statList = stats.get(groupKey);
			
			int minId = Integer.MAX_VALUE;
			GraphTemplate origin = null;
			for (GraphTemplate g: statList) {
				if (g.getThreadMethodId() < minId) {
					minId = g.getThreadMethodId();
					origin = g;
				}
			}
			
			int times = statList.size();
			if (times > 1) {
				GraphUtil.multiplyGraph(origin, times);
			}
			toMerge.add(origin);
			weights.add(times);
			logger.info("Rep for " + groupKey + ": " + origin.getMethodKey() + " " + origin.getThreadMethodId());
		}
		//Collections.sort(toMerge, graphSizeSorter);
		
		int totalTimes = 0;
		for (int i = 0; i< weights.size(); i++) {
			totalTimes += weights.get(i).intValue();
		}
		int edgeThresh = totalTimes/2;
		
		int maxWeight = Integer.MIN_VALUE;
		int dominantIdx = -1;
		for (int i = 0; i < weights.size(); i++) {
			int weight = weights.get(i);
			if (weight > maxWeight) {
				maxWeight = weight;
				dominantIdx = i;
			}
		}
		
		GraphTemplate dominant = toMerge.get(dominantIdx);
		/*if (toMerge.size() > 1) {
			for (int i = 1; i < toMerge.size(); i++) {
				if (i == dominantIdx)
					continue ;
				
				GraphTemplate g = toMerge.get(i);
				GraphUtil.mergeGraph(dominant, g);
			}
		}
		
		for (InstNode inst: dominant.getInstPool()) {
			Map<String, Double> childMap =  inst.getChildFreqMap();
			Iterator<String> keyIT = childMap.keySet().iterator();
			while (keyIT.hasNext()) {
				String childKey = keyIT.next();
				double freq = childMap.get(childKey);
				
				if (freq < edgeThresh)
					keyIT.remove();
			}
		}*/
		//GraphTemplate repGraph = GraphUtil.mergeGraphWithNormalization(toMerge, weights);
		
		return dominant;
	}
	
	public static void main(String[] args) {
		File nameMapFile = new File(MIBConfiguration.getInstance().getLabelmapDir() + "/nameMap.json");
		NameMap nameMap = GsonManager.readJsonGeneric(nameMapFile, nameMapToken);
		HashSet<String> recursiveMethods = nameMap.getRecursiveMethods();
		
		logger.info("Start caching latest template graphs");
		HashSet<String> allTemplateNames = cacheLatestGraphs(0, recursiveMethods);
		
		logger.info("Start caching latest test graphs");
		HashSet<String> allTestNames = cacheLatestGraphs(1, recursiveMethods);
		
		logger.info("Start loading all cached graphs");
		String cacheDirString = MIBConfiguration.getInstance().getCacheDir();
		File cacheDir = new File(cacheDirString);
		HashMap<String, HashSet<GraphTemplate>> graphByNames = 
				TemplateLoader.loadCacheTemplates(cacheDir, graphToken, recursiveMethods);
		
		logger.info("Start select representative template graphs");
		for (String name: allTemplateNames) {
			logger.info("Extracting rep graph for: " + name);
			HashSet<GraphTemplate> allGraphs = graphByNames.get(name);
			GraphTemplate rep = extractRepGraph(allGraphs);
			logger.info("Merge result: " + rep.getInstPool().size());
			GsonManager.writeJsonGeneric(rep, name, graphToken, 0);
		}
		
		logger.info("Start select representative test graphs");
		for (String name: allTestNames) {
			logger.info("Extracting rep graph for: " + name);
			HashSet<GraphTemplate> allGraphs = graphByNames.get(name);
			GraphTemplate rep = extractRepGraph(allGraphs);
			logger.info("Merge result: " + rep.getInstPool().size());
			GsonManager.writeJsonGeneric(rep, name, graphToken, 1);
		}
		
		logger.info("Representative graph selection ends");
	}

}
