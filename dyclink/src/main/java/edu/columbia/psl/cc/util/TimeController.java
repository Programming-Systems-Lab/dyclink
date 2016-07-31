package edu.columbia.psl.cc.util;

import java.util.concurrent.ConcurrentHashMap;

import edu.columbia.psl.cc.config.MIBConfiguration;

public class TimeController {
	
	private static long testMethodThresh = MIBConfiguration.getInstance().getTestMethodThresh() * 60 * 1000;
	
	private static ConcurrentHashMap<Integer, Long> timeMap = new ConcurrentHashMap<Integer, Long>();
	
	private static boolean enable = false;
	
	static {
		if (testMethodThresh > 0)
			enable = true;
	}
	
	public static void initTestMethodBaseTime() {		
		int threadId = ObjectIdAllocater.getThreadId();
		long baseTime = System.currentTimeMillis();
		timeMap.put(threadId, baseTime);
	}
	
	public static boolean isOverTime() {
		if (!enable)
			return false;
		
		if (testMethodExecutionTime() > testMethodThresh) {
			return true;
		} else
			return false;
	}
	
	public static long testMethodExecutionTime() {
		int threadId = ObjectIdAllocater.getThreadId();
		long baseTime = timeMap.get(threadId);
		return System.currentTimeMillis() - baseTime;
	}

}
