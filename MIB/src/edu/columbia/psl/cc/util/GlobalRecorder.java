package edu.columbia.psl.cc.util;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;

public class GlobalRecorder {
	
	private static Logger logger = Logger.getLogger(GlobalRecorder.class);
	
	private static String latestLoadedClass;
	
	private static AtomicLong curDigit = new AtomicLong();
	
	private static AtomicLong curTime = new AtomicLong();
	
	private static HashMap<String, InstNode> globalWriteFieldRecorder = new HashMap<String, InstNode>();
	
	private static Object timeLock = new Object();
	
	private static Object loadClassLock = new Object();
	
	private static Object writeFieldLock = new Object();
	
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
}
