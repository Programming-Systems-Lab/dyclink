package edu.columbia.psl.cc.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
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
import edu.columbia.psl.cc.util.StringUtil;
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
	
	private static Comparator<GraphTemplate> methodIdComp = new Comparator<GraphTemplate>() {
		public int compare(GraphTemplate g1, GraphTemplate g2) {
			int g1Id = g1.getThreadMethodId();
			int g2Id = g2.getThreadMethodId();
			//0 is impossible
			return g1Id > g2Id?1: (g1Id < g2Id? -1: 0);
		}
	};
	
	public static HashSet<String> cacheLatestGraphs(int dirIdx, 
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
	 * Load cached graphs from hardisk for mining
	 */
	public static void startExtraction() {
		File nameMapFile = new File(MIBConfiguration.getInstance().getLabelmapDir() + "/nameMap.json");
		NameMap nameMap = GsonManager.readJsonGeneric(nameMapFile, nameMapToken);
		HashSet<String> recursiveMethods = nameMap.getRecursiveMethods();
		
		logger.info("Start loading all cached graphs");
		String cacheDirString = MIBConfiguration.getInstance().getCacheDir();
		File cacheDir = new File(cacheDirString);
		HashMap<String, HashSet<GraphTemplate>> graphByNames = 
				TemplateLoader.loadCacheTemplates(cacheDir, graphToken, recursiveMethods);
		
		logger.info("Total method types in cache: " + graphByNames.keySet().size());
		for (String key: graphByNames.keySet()) {
			extractRepGraphs(graphByNames.get(key));
		}
	}
	
	/**
	 * Use graphs from memory directly
	 * @param graphs
	 */
	public static void startExtraction(HashMap<String, List<GraphTemplate>> graphs) {
		for (String key: graphs.keySet()) {
			if (GlobalRecorder.getRecursiveMethods().contains(key)) {
				logger.info("Recursive method: " + key);
				
				//Get the one withe the smallest thread method id
				List<GraphTemplate> gList = graphs.get(key);
				Collections.sort(gList, methodIdComp);
				GraphTemplate graphRep = gList.get(0);
				writeGraphHelper(graphRep);
			} else {
				extractRepGraphs(graphs.get(key));
			}
		}
	}
	
	public static void startExtractionFast(HashMap<String, HashMap<String, GraphTemplate>> graphs) {
		for (String key: graphs.keySet()) {
			HashMap<String, GraphTemplate> graphGroups = graphs.get(key);
			
			//Each map only has one graph
			for (String groupKey: graphGroups.keySet()) {
				writeGraphHelper(graphGroups.get(groupKey));
			}
		}
	}
	
	/**
	 * 
	 * Each graph pick one with smallest thread method id
	 * @param graphSet
	 * @return
	 */
	public static void extractRepGraphs(Collection<GraphTemplate> graphSet) {		
		HashMap<String, List<GraphTemplate>> stats = new HashMap<String, List<GraphTemplate>>();
		for (GraphTemplate graph: graphSet) {
			String groupKey = GraphGroup.groupKey(0, graph);
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
			List<GraphTemplate> statList = stats.get(groupKey);
			Collections.sort(statList, methodIdComp);
			GraphTemplate groupRep = statList.get(0);
			writeGraphHelper(groupRep);
		}
	}
	
	/**
	 * Extract representative graph for a single method
	 * @param graphSet
	 * @return
	 */
	public static GraphTemplate extractRepGraph(HashSet<GraphTemplate> graphSet) {
		if (graphSet.size() == 1) {
			GraphTemplate ret = graphSet.iterator().next();
			return ret;
		}
		
		HashMap<String, List<GraphTemplate>> stats = new HashMap<String, List<GraphTemplate>>();
		for (GraphTemplate graph: graphSet) {
			String groupKey = GraphGroup.groupKey(0, graph);
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
	
	public static void writeCallees(String parentIdx, HashMap<String, GraphTemplate> callees) {
		String fullDirString = MIBConfiguration.getInstance().getCacheDir() + "/" + parentIdx;
		File fullDir = new File(fullDirString);
		
		if (!fullDir.exists()) {
			fullDir.mkdir();
		}
		
		for (String myKey: callees.keySet()) {
			GraphTemplate me = callees.get(myKey);
			if (me.calleeRequired.size() > 0) {
				writeCallees(myKey, me.calleeRequired);
			}
			
			String dumpName = parentIdx + "/" + myKey;
			GsonManager.writeJsonGeneric(me, dumpName, graphToken, MIBConfiguration.CACHE_DIR);
		}
	}
	
	public static void writeGraphHelper(GraphTemplate groupRep) {
		String nameWithThread = StringUtil.genKeyWithId(groupRep.getShortMethodKey(), String.valueOf(groupRep.getThreadId()));
		String dumpName = StringUtil.genKeyWithId(nameWithThread, String.valueOf(groupRep.getThreadMethodId()));
		
		logger.info("Profiling methods: " + dumpName);
		
		if (groupRep.calleeRequired.size() > 0) {
			String parentDir = StringUtil.genThreadWithMethodIdx(groupRep.getThreadId(), groupRep.getThreadMethodId());
			writeCallees(parentDir, groupRep.calleeRequired);
		}
		
		if (MIBConfiguration.getInstance().isTemplateMode()) {
			GsonManager.writeJsonGeneric(groupRep, dumpName, graphToken, MIBConfiguration.TEMPLATE_DIR);
		} else {
			GsonManager.writeJsonGeneric(groupRep, dumpName, graphToken, MIBConfiguration.TEST_DIR);
		}
	}
	
	public static void main(String[] args) {
		File nameMapFile = new File(MIBConfiguration.getInstance().getLabelmapDir() + "/nameMap.json");
		NameMap nameMap = GsonManager.readJsonGeneric(nameMapFile, nameMapToken);
		HashSet<String> recursiveMethods = nameMap.getRecursiveMethods();
		
		logger.info("Start caching latest template graphs");
		HashSet<String> allTemplateNames = cacheLatestGraphs(MIBConfiguration.TEMPLATE_DIR, recursiveMethods);
		
		logger.info("Start caching latest test graphs");
		HashSet<String> allTestNames = cacheLatestGraphs(MIBConfiguration.TEST_DIR, recursiveMethods);
		
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
			GsonManager.writeJsonGeneric(rep, name, graphToken, MIBConfiguration.TEMPLATE_DIR);
		}
		
		logger.info("Start select representative test graphs");
		for (String name: allTestNames) {
			logger.info("Extracting rep graph for: " + name);
			HashSet<GraphTemplate> allGraphs = graphByNames.get(name);
			GraphTemplate rep = extractRepGraph(allGraphs);
			logger.info("Merge result: " + rep.getInstPool().size());
			GsonManager.writeJsonGeneric(rep, name, graphToken, MIBConfiguration.TEST_DIR);
		}
		
		logger.info("Representative graph selection ends");
	}

}
