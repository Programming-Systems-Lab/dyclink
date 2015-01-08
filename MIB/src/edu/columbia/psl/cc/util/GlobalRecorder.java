package edu.columbia.psl.cc.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.GraphGroup;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.StaticMethodMiner;

public class GlobalRecorder {
	
	private static Logger logger = Logger.getLogger(GlobalRecorder.class);
	
	private static TypeToken<StaticMethodMiner> smmToken = new TypeToken<StaticMethodMiner>(){};
	
	//Key: class name, Val: short name + thread id of clinit
	private static HashMap<String, String> loadedClass = new HashMap<String, String>();
	
	//private static AtomicLong curDigit = new AtomicLong();
	
	private static AtomicLong curTime = new AtomicLong();
	
	//Key: full name, Val: short name
	private static HashMap<String, String> globalNameMap = new HashMap<String, String>();
	
	private static HashSet<String> undersizedMethods = new HashSet<String>();
	
	private static HashMap<String, StaticMethodMiner> staticMethodMinerMap = new HashMap<String, StaticMethodMiner>();
	
	private static HashMap<String, AtomicInteger> shortNameCounter = new HashMap<String, AtomicInteger>();
	
	private static HashMap<String, InstNode> globalWriteFieldRecorder = new HashMap<String, InstNode>();
	
	private static HashSet<String> recursiveMethodRecorder = new HashSet<String>();
	
	//private static HashMap<String, List<GraphTemplate>> graphRecorder = new HashMap<String, List<GraphTemplate>>();
	private static HashMap<String, HashMap<String, GraphTemplate>> graphRecorder = new HashMap<String, HashMap<String, GraphTemplate>>();
	
	private static HashMap<Long, GraphTemplate> latestGraphs = new HashMap<Long, GraphTemplate>();
	
	private static Object timeLock = new Object();
	
	private static Object loadClassLock = new Object();
	
	private static Object writeFieldLock = new Object();
	
	private static Object recursiveMethodLock = new Object();
	
	private static Object nameLock = new Object();
	
	private static Object undersizeLock = new Object();
	
	private static Object staticMethodMinerLock = new Object();
	
	private static Object vRecorderLock = new Object();
	
	private static Object graphRecorderLock = new Object();
	
	public static void registerLoadedClass(String className, String shortClinit) {
		synchronized(loadClassLock) {
			loadedClass.put(className, shortClinit);
		}
	}
	
	public static String getLoadedClass(String className) {
		synchronized(loadClassLock) {
			if (!loadedClass.containsKey(className))
				return null;
			else {
				return loadedClass.remove(className);
			}
		}
	}
	
	/**
	 * No need for locking, just for observing
	 * @return
	 */
	public static HashMap<String, String> getLoadedClasses() {
		return loadedClass;
	}
	
	public static long getCurTime() {
		synchronized(timeLock) {
			long uni = curTime.getAndIncrement();
			/*long ten = curDigit.get();
			if (uni == Long.MAX_VALUE) {
				curTime.set(0);
				curDigit.getAndIncrement();
			}
			long[] ret = {uni, ten};
			return ret;*/
			if (uni == Long.MAX_VALUE) {
				logger.error("Time incrmenter reaches limit");
				System.exit(-1);
			}
			return uni;
		}
	}
	
	public static void updateGlobalWriteFieldRecorder(String field, InstNode writeNode) {
		synchronized(writeFieldLock) {
			globalWriteFieldRecorder.put(field, writeNode);
		}
	}
	
	public static InstNode getWriteFieldNode(String field) {
		synchronized(writeFieldLock) {
			return globalWriteFieldRecorder.get(field);
		}
	}
	
	public static void replaceWriteFieldNodes(GraphTemplate graph, InstNode replace) {
		synchronized(writeFieldLock) {
			HashMap<String, String> writeNodes = graph.getLatestWriteFields();
			
			logger.info("Replacement for under-sized graph: " + graph.getMethodKey());
			for (String writeField: writeNodes.keySet()) {
				InstNode curNode = graph.getInstPool().searchAndGet(writeNodes.get(writeField));
				InstNode globalNode = globalWriteFieldRecorder.get(writeField);
				
				if (globalNode != null && curNode.equals(globalNode)) {
					globalWriteFieldRecorder.put(writeField, replace);
					logger.info(globalNode + " => " + replace); 
				}
			}
		}
	}
	
	/**
	 * Consider to use observer pattern in next version
	 * @param graph
	 */
	public static void replaceWriteFieldNodes(GraphTemplate graph) {
		synchronized(writeFieldLock) {
			HashMap<String, String> writeNodes = graph.getLatestWriteFields();
			
			logger.info("Replacement for graph group: " + graph.getMethodKey());
			//HashSet<String> toRemove = new HashSet<String>();
			HashSet<InstNode> toRemove = new HashSet<InstNode>();
			for (String writeField: writeNodes.keySet()) {
				InstNode curNode = graph.getInstPool().searchAndGet(writeNodes.get(writeField));
				InstNode globalNode = globalWriteFieldRecorder.get(writeField);
				
				InstNode remove = new InstNode();
				remove.setFromMethod(globalNode.getFromMethod());
				remove.setThreadId(globalNode.getThreadId());
				remove.setThreadMethodIdx(globalNode.getThreadMethodIdx());
				toRemove.add(remove);
				/*String globalId = StringUtil.genIdxKey(globalNode.getFromMethod(), 
						globalNode.getThreadId(), globalNode.getThreadMethodIdx(), globalNode.getIdx());
				toRemove.add(globalId);*/
				
				globalWriteFieldRecorder.put(writeField, curNode);
				logger.info(globalNode + " => " + curNode);
			}
			
			//Ensure the removed node not exist in global record
			for (String existField: globalWriteFieldRecorder.keySet()) {
				InstNode existNode = globalWriteFieldRecorder.get(existField);
				
				for (InstNode remove: toRemove) {
					Iterator<String> childKeyIT = existNode.getChildFreqMap().keySet().iterator();
					while (childKeyIT.hasNext()) {
						String childKey = childKeyIT.next();
						String[] parsed = StringUtil.parseIdxKey(childKey);
						if (Integer.valueOf(parsed[0]) == remove.getThreadId() 
								&& Integer.valueOf(parsed[1]) == remove.getThreadMethodIdx() 
								&& Integer.valueOf(parsed[2]) == remove.getIdx()) {
							childKeyIT.remove();
						}
					}
				}
			}
		}
	}
	
	public static String registerGlobalName(String className, String methodName, String fullName) {
		String shortNameNoKey = StringUtil.cleanPunc(className, ".") + 
				":" + StringUtil.cleanPunc(methodName);
		
		synchronized(nameLock) {
			int shortNameCount = -1;
			if (shortNameCounter.containsKey(shortNameNoKey)) {
				shortNameCount = shortNameCounter.get(shortNameNoKey).getAndIncrement();
			} else {
				AtomicInteger ai = new AtomicInteger();
				shortNameCounter.put(shortNameNoKey, ai);
				shortNameCount = ai.getAndIncrement();
			}
			
			String shortName = shortNameNoKey + ":" + shortNameCount;
			globalNameMap.put(fullName, shortName);
			return shortName;
		}
	}
	
	public static String getGlobalName(String fullName) {
		return globalNameMap.get(fullName);
	}
	
	public static HashMap<String, String> getGlobalNameMap() {
		return globalNameMap;
	}
	
	public static void registerRecursiveMethod(String methodShortName) {
		synchronized(recursiveMethodLock) {
			recursiveMethodRecorder.add(methodShortName);
		}
	}
	
	public static boolean checkRecursiveMethod(String methodShortName) {
		synchronized(recursiveMethodLock) {
			return recursiveMethodRecorder.contains(methodShortName);
		}
	}
	
	public static HashSet<String> getRecursiveMethods() {
		return recursiveMethodRecorder;
	}
	
	public static StaticMethodMiner getStaticMethodMiner(String shortKey) {
		synchronized(staticMethodMinerLock) {
			if (staticMethodMinerMap.containsKey(shortKey)) {
				return staticMethodMinerMap.get(shortKey);
			} else {
				File staticFile = new File("./labelmap/" + StringUtil.appendMap(shortKey) + ".json");
				StaticMethodMiner smm = GsonManager.readJsonGeneric(staticFile, smmToken);
				staticMethodMinerMap.put(shortKey, smm);
				return smm; 
			}
		}
	}
	
	public static void registerGraph(String shortKey, GraphTemplate graph, boolean registerLatest) {
		synchronized(graphRecorderLock) {
			String groupKey = GraphGroup.groupKey(0, graph);
			
			if (graphRecorder.containsKey(shortKey)) {
				HashMap<String, GraphTemplate> subRecord = graphRecorder.get(shortKey);
				
				if (checkRecursiveMethod(shortKey)) {
					subRecord.clear();
					subRecord.put(groupKey, graph);
				} else {
					if (!subRecord.containsKey(groupKey))
						subRecord.put(groupKey, graph);
				}
				//graphRecorder.get(shortKey).add(graph);
			} else {
				//List<GraphTemplate> gQueue = new ArrayList<GraphTemplate>();
				//gQueue.add(graph);
				//graphRecorder.put(shortKey, gQueue);
				HashMap<String, GraphTemplate> subRecord = new HashMap<String, GraphTemplate>();
				subRecord.put(groupKey, graph);
				graphRecorder.put(shortKey, subRecord);
			}
			
			if (registerLatest) {
				long threadId = ObjectIdAllocater.getThreadId();
				latestGraphs.put(threadId, graph);
			}
		}
	}
	
	public static void recoverLatestGraph(GraphTemplate latestGraph) {
		synchronized(graphRecorderLock) {
			long threadId = ObjectIdAllocater.getThreadId();
			latestGraphs.put(threadId, latestGraph);
		}
	}
	
	/*public static GraphTemplate getLatestGraph(String shortKey) {
		synchronized(graphRecorderLock) {
			List<GraphTemplate> gQueue = graphRecorder.get(shortKey);
			if (gQueue == null || gQueue.size() == 0)
				return null;
			else {
				return gQueue.get(gQueue.size() - 1);
			}
		}
	}*/
	
	public static GraphTemplate getLatestGraph(long threadId) {
		synchronized(graphRecorderLock) {
			return latestGraphs.remove(threadId);
		}
	}
	
	/*public static HashMap<String, List<GraphTemplate>> getGraphs() {
		return graphRecorder;
	}*/
	
	public static HashMap<String, HashMap<String, GraphTemplate>> getGraphs() {
		return graphRecorder;
	}
	
	public static void registerUndersizedMethod(String methodKey) {
		synchronized(undersizeLock) {
			undersizedMethods.add(methodKey);
		}
	}
	
	public static boolean checkUndersizedMethod(String methodKey) {
		synchronized(undersizeLock) {
			return undersizedMethods.contains(methodKey);
		}
	}
	
	public static HashSet<String> getUndersizedMethods() {
		return undersizedMethods;
	}
	
	public static void clearContext() {
		synchronized(graphRecorderLock) {
			graphRecorder.clear();
			latestGraphs.clear();
		}
		
		synchronized(writeFieldLock) {
			globalWriteFieldRecorder.clear();
		}
		
		synchronized(timeLock) {
			//curDigit.set(0);
			curTime.set(0);
		}
		System.gc();
	}
}
