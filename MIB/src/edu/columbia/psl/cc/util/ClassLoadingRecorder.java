package edu.columbia.psl.cc.util;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class ClassLoadingRecorder {
	
	//Key: thread id, String: clinit rep string
	private static ConcurrentHashMap<Integer, String> classRecorder = new ConcurrentHashMap<Integer, String>();
	
	public static synchronized void addClass(int threadId, String clazz) {
		classRecorder.put(threadId, clazz);
	}
	
	public static String getCurrentClass(int threadId) {
		return classRecorder.get(threadId);
	}
}
