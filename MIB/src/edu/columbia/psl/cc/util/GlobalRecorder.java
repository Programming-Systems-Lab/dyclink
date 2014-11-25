package edu.columbia.psl.cc.util;

import java.io.File;
import java.util.HashMap;
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
	
	private static String latestLoadedClass;
	
	private static AtomicLong curDigit = new AtomicLong();
	
	private static AtomicLong curTime = new AtomicLong();
	
	//Key: full name, Val: short name
	private static HashMap<String, String> globalNameMap = new HashMap<String, String>();
	
	private static HashMap<String, StaticMethodMiner> staticMethodMinerMap = new HashMap<String, StaticMethodMiner>();
	
	private static HashMap<String, AtomicInteger> shortNameCounter = new HashMap<String, AtomicInteger>();
	
	private static HashMap<String, InstNode> globalWriteFieldRecorder = new HashMap<String, InstNode>();
	
	private static Object timeLock = new Object();
	
	private static Object loadClassLock = new Object();
	
	private static Object writeFieldLock = new Object();
	
	private static Object nameLock = new Object();
	
	private static Object staticMethodMinerLock = new Object();
	
	public static void setLatestLoadedClass(String clazz) {
		synchronized(loadClassLock) {
			latestLoadedClass = clazz;
		}
	}
	
	public static String getLatestLoadedClass() {
		synchronized(loadClassLock) {
			String ret = latestLoadedClass;
			latestLoadedClass = null;
			return ret;
		}
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
	
	public static void replaceWriteFieldNodes(GraphTemplate graph) {
		synchronized(writeFieldLock) {
			HashMap<String, InstNode> writeNodes = graph.getLatestWriteFields();
			
			logger.info("Replacement for graph group: " + graph.getMethodKey());
			for (String writeField: writeNodes.keySet()) {
				InstNode curNode = writeNodes.get(writeField);
				InstNode globalNode = globalWriteFieldRecorder.get(writeField);
				
				globalWriteFieldRecorder.put(writeField, curNode);
				logger.info(globalNode + " => " + curNode);
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
