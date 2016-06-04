package edu.columbia.psl.cc.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import edu.columbia.psl.cc.config.MIBConfiguration;

public class ObjectIdAllocater {
	
	//private static Logger logger = LogManager.getLogger(ObjectIdAllocater.class);

	//Save 0 for method stack recorder to identify static method
	private static AtomicInteger indexer = new AtomicInteger(1);
	
	private static AtomicInteger threadCounter = new AtomicInteger(MIBConfiguration.getInstance().getThreadInit());
	
	private static ThreadLocal<Integer> threadIndexer = new ThreadLocal<Integer>() {
		@Override
		public Integer initialValue() {
			return threadCounter.getAndIncrement();
		}
	};
	
	private static ConcurrentHashMap<String, AtomicInteger> classMethodIndexer = new ConcurrentHashMap<String, AtomicInteger>();
	
	private static ConcurrentHashMap<String, AtomicInteger> threadMethodIndexer = new ConcurrentHashMap<String, AtomicInteger>();
	
	private static ConcurrentHashMap<Integer, AtomicInteger> threadMethodIndexerFast = new ConcurrentHashMap<Integer, AtomicInteger>();
			
	public static int getThreadId() {
		int threadId = threadIndexer.get();
		return threadId;
	}
			
	public static int getIndex() {
		return indexer.getAndIncrement();
	}
	
	public static int getClassMethodIndex(String className, String methodName, String desc) {
		Class<?> correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(className, methodName, desc, false);
		String methodKey = StringUtil.genKey(correctClass.getName(), methodName, desc);
		
		if (!classMethodIndexer.containsKey(methodKey)) {
			AtomicInteger ai = new AtomicInteger();
			classMethodIndexer.put(methodKey, ai);
		}
		return classMethodIndexer.get(methodKey).getAndIncrement();
	}
	
	public static int getThreadMethodIndex(String className, String methodName, String desc, int threadId) throws Exception{
		Class<?> correctClass = null;
		if (methodName.equals("<init>") || methodName.equals("<clinit>")) {
			correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(className, methodName, desc, true);
		} else {
			correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(className, methodName, desc, false);
		}
		
		if (correctClass == null) {
			throw new Exception("Cannot retrieve correct class: " + className + " " + methodName);
		}
		
		String methodKey = StringUtil.genKey(correctClass.getName(), methodName, desc);
		String threadMethodKey = StringUtil.genKeyWithId(methodKey, String.valueOf(threadId));
		return getThreadMethodIndex(threadMethodKey);
	}
	
	public static int getThreadMethodIndex(String threadMethodKey) {
		if (!threadMethodIndexer.containsKey(threadMethodKey)) {
			AtomicInteger ai = new AtomicInteger();
			threadMethodIndexer.put(threadMethodKey, ai);
		}
		return threadMethodIndexer.get(threadMethodKey).getAndIncrement();
	}
	
	public static int getThreadMethodIndex(int threadId) {
		if (!threadMethodIndexerFast.containsKey(threadId)) {
			AtomicInteger ai = new AtomicInteger();
			threadMethodIndexerFast.put(threadId, ai);
		}
		return threadMethodIndexerFast.get(threadId).getAndIncrement();
	}
	
	public static void setThreadMethodIndex(int threadId, int idx) {
		AtomicInteger ai = new AtomicInteger();
		ai.set(idx);
		threadMethodIndexerFast.put(threadId, ai);
	}
	
	public static ConcurrentHashMap<Integer, AtomicInteger> getThreadMethodIdxRecord() {
		return threadMethodIndexerFast;
	}
		
	public static void main(String[] args) {
		Object o = new Object();
		//System.out.println(getIndex());
		//System.out.println(getClassMethodIndex("a", "b", "c"));
	}
}
