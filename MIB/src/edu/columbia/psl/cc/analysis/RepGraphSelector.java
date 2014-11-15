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
import edu.columbia.psl.cc.util.GsonManager;
import edu.columbia.psl.cc.util.TemplateLoader;

public class RepGraphSelector {
	
	private static Logger logger = Logger.getLogger(RepGraphSelector.class);
	
	private static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
	private static HashSet<String> cacheLatestGraphs(int dirIdx) {
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
			GsonManager.cacheGraph(name, 0);
		}
		
		return allNames;
	}
	
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
		
		int maxSize = 0;
		String groupKey = "";
		for (String gKey: stats.keySet()) {
			List<GraphTemplate> gTemplates = stats.get(gKey);
			if (gTemplates.size() > maxSize) {
				maxSize = gTemplates.size();
				groupKey = gKey;
			}
		}
		
		//Pick the one with the latest graph id
		Comparator<GraphTemplate> graphSorter = new Comparator<GraphTemplate>() {
			@Override
			public int compare(GraphTemplate g1, GraphTemplate g2) {
				if (g1.getThreadMethodId() < g2.getThreadMethodId()) {
					return 1;
				} else if (g1.getThreadMethodId() > g2.getThreadMethodId()) {
					return - 1;
				} else {
					//Impossible
					return 0;
				}
			}
		};
		List<GraphTemplate> repList = stats.get(groupKey);
		Collections.sort(repList, graphSorter);
		GraphTemplate rep = stats.get(groupKey).get(0);
		logger.info("Graph rep selection: " + rep.getMethodKey() + " " + rep.getThreadId());
		logger.info("Max group: " + groupKey);
		logger.info("Group size: " + maxSize);
		logger.info("Latest graph: " + rep.getThreadMethodId());
		return stats.get(groupKey).get(0);
	}
	
	public static void main(String[] args) {
		logger.info("Start caching latest template graphs");
		HashSet<String> allTemplateNames = cacheLatestGraphs(0);
		
		logger.info("Start caching latest test graphs");
		HashSet<String> allTestNames = cacheLatestGraphs(1);
		
		logger.info("Start loading all cached graphs");
		String cacheDirString = MIBConfiguration.getInstance().getCacheDir();
		File cacheDir = new File(cacheDirString);
		HashMap<String, HashSet<GraphTemplate>> graphByNames = TemplateLoader.loadCacheTemplates(cacheDir, graphToken);
		
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
