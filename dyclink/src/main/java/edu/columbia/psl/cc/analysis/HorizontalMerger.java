package edu.columbia.psl.cc.analysis;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.abs.AbstractGraph;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.CumuGraph;
import edu.columbia.psl.cc.pojo.GraphGroup;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.NameMap;
import edu.columbia.psl.cc.util.GlobalGraphRecorder;
import edu.columbia.psl.cc.util.GraphUtil;
import edu.columbia.psl.cc.util.GsonManager;
import edu.columbia.psl.cc.util.ShutdownLogger;
import edu.columbia.psl.cc.util.StringUtil;
import edu.columbia.psl.cc.util.TemplateLoader;

public class HorizontalMerger {
	
	//private static Logger logger = LogManager.getLogger(HorizontalMerger.class);
	
	private static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
	private static TypeToken<NameMap> nameMapToken = new TypeToken<NameMap>(){};
	
	private static HashMap<String, HashSet<String>> dumpRecord = new HashMap<String, HashSet<String>>();
	
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
		
	/**
	 * Load cached graphs from hardisk for mining
	 */
	public static void startExtraction() {
		File nameMapFile = new File(MIBConfiguration.getInstance().getLabelmapDir() + "/nameMap.json");
		NameMap nameMap = GsonManager.readJsonGeneric(nameMapFile, nameMapToken);
		HashSet<String> recursiveMethods = nameMap.getRecursiveMethods();
		
		ShutdownLogger.appendMessage("Start loading all cached graphs");
		String cacheDirString = MIBConfiguration.getInstance().getCacheDir();
		File cacheDir = new File(cacheDirString);
		HashMap<String, HashSet<GraphTemplate>> graphByNames = 
				TemplateLoader.loadCacheTemplates(cacheDir, graphToken, recursiveMethods);
		
		ShutdownLogger.appendMessage("Total method types in cache: " + graphByNames.keySet().size());
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
			if (GlobalGraphRecorder.getRecursiveMethods().contains(key)) {
				ShutdownLogger.appendMessage("Recursive method: " + key);
				
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
	
	public static void startExtractionFast(String appName, HashMap<String, HashMap<String, GraphTemplate>> graphs) {
		List<AbstractGraph> finalGraphs = new ArrayList<AbstractGraph>();
		for (String key: graphs.keySet()) {
			HashMap<String, GraphTemplate> graphGroups = graphs.get(key);
			
			if (!dumpRecord.containsKey(key)) {
				HashSet<String> dumpGroups = new HashSet<String>();
				dumpRecord.put(key, dumpGroups);
			}
			
			HashSet<String> dumpedGroups = dumpRecord.get(key);
			
			//Each map only has one graph
			//logger.info("Check: " + dumpedGroups);
			for (String groupKey: graphGroups.keySet()) {
				//logger.info(groupKey);
				if (dumpedGroups.contains(groupKey)) {
					continue ;
				}
				
				//System.out.println("Check dump: " + key + " " + groupKey);
				//writeGraphHelper(graphGroups.get(groupKey));
				finalGraphs.add(graphGroups.get(groupKey));
				dumpedGroups.add(groupKey);
			}
		}
		
		/*ShutdownLogger.appendMessage("Total graphs: " + finalGraphs.size());
		zipGraphsHelper(appName, finalGraphs);*/
		TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
		startExtractionFast(appName, finalGraphs, graphToken);
	}
	
	public static void startExtractionFast(String appName, List<AbstractGraph> graphs, TypeToken graphToken) {
		ShutdownLogger.appendMessage("Total graphs: " + graphs.size());
		zipGraphsHelper(appName, graphs, graphToken);
	}
	
	public static void startSeparation(String appName, List<AbstractGraph> graphs, TypeToken graphToken) {
		//ShutdownLogger.appendMessage("Graph size: " + graph.getInstPool().size());
		try {
			int init = MIBConfiguration.getInstance().getThreadInit();
			for (int i = 0; i < graphs.size(); i++) {
				int idx = init + i;
				String fileName = appName.replace("/", "-") + "-" + idx;
				AbstractGraph g = graphs.get(i);
				GsonManager.writeJsonGeneric(g, fileName, graphToken, MIBConfiguration.GRAPH_DIR);
			}
		} catch (Exception ex) {
			ShutdownLogger.appendException(ex);
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
		
		ShutdownLogger.appendMessage("Graph groups with freq: ");
		for (String groupKey: stats.keySet()) {
			ShutdownLogger.appendMessage(groupKey + ": " + stats.get(groupKey).size());
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
		
		ShutdownLogger.appendMessage("Graph groups with freq: ");
		for (String groupKey: stats.keySet()) {
			ShutdownLogger.appendMessage(groupKey + ": " + stats.get(groupKey).size());
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
			ShutdownLogger.appendMessage("Rep for " + groupKey + ": " + origin.getMethodKey() + " " + origin.getThreadMethodId());
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
	
	public static void writeCallees(String parentIdx, 
			HashMap<String, AbstractGraph> callees, 
			TypeToken graphToken) {
		String fullDirString = MIBConfiguration.getInstance().getCacheDir() + "/" + parentIdx;
		File fullDir = new File(fullDirString);
		
		if (!fullDir.exists()) {
			fullDir.mkdir();
		}
		
		for (String myKey: callees.keySet()) {
			try {
				AbstractGraph me = callees.get(myKey);
				if (me.calleeRequired == null) {
					System.out.println("Check graph: " + me.getMethodName());
					System.exit(-1);
				}
				
				if (me.calleeRequired.size() > 0) {
					writeCallees(myKey, me.calleeRequired, graphToken);
				}
				
				String dumpName = parentIdx + "/" + myKey;
				GsonManager.writeJsonGeneric(me, dumpName, graphToken, MIBConfiguration.CACHE_DIR);
			} catch (Exception ex) {
				ShutdownLogger.appendException(ex);
			}
		}
	}
	
	public static void writeGraphHelper(GraphTemplate groupRep) {
		String nameWithThread = StringUtil.genKeyWithId(groupRep.getShortMethodKey(), String.valueOf(groupRep.getThreadId()));
		String dumpName = StringUtil.genKeyWithId(nameWithThread, String.valueOf(groupRep.getThreadMethodId()));
		
		ShutdownLogger.appendMessage("Profiling methods: " + dumpName);
		
		if (groupRep.calleeRequired.size() > 0) {
			String parentDir = StringUtil.genThreadWithMethodIdx(groupRep.getThreadId(), groupRep.getThreadMethodId());
			writeCallees(parentDir, groupRep.calleeRequired, graphToken);
		}
		
		try {
			GsonManager.writeJsonGeneric(groupRep, dumpName, graphToken, MIBConfiguration.GRAPH_DIR);
		} catch (Exception ex) {
			ShutdownLogger.appendException(ex);
		}
	}
	
	/*public static void zipCalleesHelper(ZipOutputStream zipStream, 
			String parentDir, 
			HashMap<String, GraphTemplate> callees) {
		try {
			for (String calleeKey: callees.keySet()) {
				GraphTemplate g = callees.get(calleeKey);
					
				if (g.calleeRequired.size() > 0) {
					zipCalleesHelper(zipStream, parentDir, g.calleeRequired);
				}
					
				String entryName = "cache/" + parentDir + "/" + calleeKey + ".json";                       
				ZipEntry entry = new ZipEntry(entryName);
				zipStream.putNextEntry(entry);
					
				String jsonString = GsonManager.jsonString(g, graphToken);
				byte[] data = jsonString.getBytes();
				zipStream.write(data, 0, data.length);
				zipStream.closeEntry();
			}
		} catch (Exception ex) {
			ShutdownLogger.appendException(ex);
		}
	}*/
	
	public static void zipGraphsHelper(String appName, Collection<AbstractGraph> graphs, TypeToken graphToken) {
		try {
			File checkBase = new File(MIBConfiguration.getInstance().getGraphDir());
			if (!checkBase.exists()) {
				checkBase.mkdirs();
			}
		                       
		    String baseDir = checkBase.getAbsolutePath();
		    String zipFilePath = baseDir + "/" + appName + ".zip";
		    FileOutputStream zipFile = new FileOutputStream(zipFilePath);
		    ZipOutputStream zipStream = new ZipOutputStream(new BufferedOutputStream(zipFile));
		                       
		    for (AbstractGraph g: graphs) {
		    	if (!MIBConfiguration.getInstance().isCumuGraph() 
		    			&& g.calleeRequired.size() > 0) {
		    		String parentDir = StringUtil.genThreadWithMethodIdx(g.getThreadId(), g.getThreadMethodId());
		    		//zipCalleesHelper(zipStream, parentDir, g.calleeRequired);
		    		writeCallees(parentDir, g.calleeRequired, graphToken);
		    	}
		                               
		    	String className = g.getShortMethodKey().split(":")[0];
		    	String pkgName = null;
		    	if (className.contains("."))
		    		pkgName = StringUtil.parsePkgName(className);
		    	else
		    		pkgName = className;
		                               
		    	String nameWithThread = StringUtil.genKeyWithId(g.getShortMethodKey(), String.valueOf(g.getThreadId()));
		    	String dumpName = StringUtil.genKeyWithId(nameWithThread, String.valueOf(g.getThreadMethodId()));
		    	String entryName = pkgName + "/" + dumpName + ".json";
		    	ShutdownLogger.appendMessage("Entry: " + entryName);
		    	ZipEntry entry = new ZipEntry(entryName);
		    	zipStream.putNextEntry(entry);
		                               
		    	String jsonString = GsonManager.jsonString(g, graphToken);
		    	byte[] data = jsonString.getBytes();
		    	zipStream.write(data, 0, data.length);
		    	zipStream.closeEntry();
		    }
		    zipStream.close();
		} catch (Exception ex) {
			ShutdownLogger.appendException(ex);
		}
	}
	
	public static void main(String[] args) {
		AbstractGraph g = new CumuGraph();
		g.setInstPool(new InstPool());
		g.setMethodKey("abc");
		g.setShortMethodKey("abc");
		Collection<AbstractGraph> test = new HashSet<AbstractGraph>();
		test.add(g);
		TypeToken<CumuGraph> token = new TypeToken<CumuGraph>(){};
		zipGraphsHelper("abc", test, token);
	}
}
