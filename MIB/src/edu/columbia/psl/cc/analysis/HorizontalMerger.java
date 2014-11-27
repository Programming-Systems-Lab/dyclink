package edu.columbia.psl.cc.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.GraphGroup;
import edu.columbia.psl.cc.pojo.GraphTemplate;
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
			int g1Size = g1.getInstPool().size();
			int g2Size = g2.getInstPool().size();
			return g1Size < g2Size?1: (g1Size> g2Size?-1: 0);
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
		for (String name: allNames) {
			if (recursiveMethods.contains(name)) {
				GsonManager.cacheGraph(name, dirIdx, true);
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
		
		//Pick the one with the latest graph id
		List<GraphTemplate> toMerge = new ArrayList<GraphTemplate>();
		for (String groupKey: stats.keySet()) {
			List<GraphTemplate> statList = stats.get(groupKey);
			
			int maxId = -1;
			GraphTemplate latest = null;
			for (GraphTemplate g: statList) {
				if (g.getThreadMethodId() > maxId) {
					maxId = g.getThreadMethodId();
					latest = g;
				}
			}
			
			int times = statList.size();
			if (times > 1) {
				GraphUtil.multiplyGraph(latest, times);
			}
			toMerge.add(latest);
		}
		Collections.sort(toMerge, graphSizeSorter);
		
		GraphTemplate repGraph = toMerge.get(0);
		if (toMerge.size() > 1) {
			for (int i = 1; i < toMerge.size(); i++) {
				GraphTemplate g = toMerge.get(i);
				GraphUtil.mergeGraph(repGraph, g);
			}
		}
		
		return repGraph;
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
			HashSet<GraphTemplate> allGraphs = graphByNames.get(name);
			GraphTemplate rep = extractRepGraph(allGraphs);
			GsonManager.writeJsonGeneric(rep, name, graphToken, 0);
		}
		
		logger.info("Start select representative test graphs");
		for (String name: allTestNames) {
			HashSet<GraphTemplate> allGraphs = graphByNames.get(name);
			GraphTemplate rep = extractRepGraph(allGraphs);
			GsonManager.writeJsonGeneric(rep, name, graphToken, 1);
		}
		
		logger.info("Representative graph selection ends");
	}

}
