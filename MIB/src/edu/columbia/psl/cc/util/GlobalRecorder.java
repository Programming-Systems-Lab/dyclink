package edu.columbia.psl.cc.util;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.StaticMethodMiner;

public class GlobalRecorder {
	
	private static Logger logger = Logger.getLogger(GlobalRecorder.class);
	
	private static TypeToken<StaticMethodMiner> smmToken = new TypeToken<StaticMethodMiner>(){};
	
	//Key: class name, Val: short name + thread id of clinit
	private static HashMap<String, String> loadedClass = new HashMap<String, String>();
	
	private static AtomicLong curDigit = new AtomicLong();
	
	private static AtomicLong curTime = new AtomicLong();
	
	//Key: full name, Val: short name
	private static HashMap<String, String> globalNameMap = new HashMap<String, String>();
	
	private static HashMap<String, StaticMethodMiner> staticMethodMinerMap = new HashMap<String, StaticMethodMiner>();
	
	private static HashMap<String, AtomicInteger> shortNameCounter = new HashMap<String, AtomicInteger>();
	
	private static HashMap<String, InstNode> globalWriteFieldRecorder = new HashMap<String, InstNode>();
	
	private static HashSet<String> recursiveMethodRecorder = new HashSet<String>();
	
	private static Object timeLock = new Object();
	
	private static Object loadClassLock = new Object();
	
	private static Object writeFieldLock = new Object();
	
	private static Object recursiveMethodLock = new Object();
	
	private static Object nameLock = new Object();
	
	private static Object staticMethodMinerLock = new Object();
	
	private static Object vRecorderLock = new Object();
	
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
	
	public static long[] getCurTime() {
		synchronized(timeLock) {
			long uni = curTime.getAndIncrement();
			long ten = curDigit.get();
			if (uni == Long.MAX_VALUE) {
				curTime.set(0);
				curDigit.getAndIncrement();
			}
			long[] ret = {uni, ten};
			return ret;
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
			HashMap<String, InstNode> writeNodes = graph.getLatestWriteFields();
			
			logger.info("Replacement for under-sized graph: " + graph.getMethodKey());
			for (String writeField: writeNodes.keySet()) {
				InstNode curNode = writeNodes.get(writeField);
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
			HashMap<String, InstNode> writeNodes = graph.getLatestWriteFields();
			
			logger.info("Replacement for graph group: " + graph.getMethodKey());
			//HashSet<String> toRemove = new HashSet<String>();
			HashSet<InstNode> toRemove = new HashSet<InstNode>();
			for (String writeField: writeNodes.keySet()) {
				InstNode curNode = writeNodes.get(writeField);
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
						if (parsed[0].equals(remove.getFromMethod()) 
								&& Long.valueOf(parsed[1]) == remove.getThreadId() 
								&& Integer.valueOf(parsed[2]) == remove.getThreadMethodIdx()) {
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
			
			String shortName = shortNameNoKey + shortNameCount;
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
}
