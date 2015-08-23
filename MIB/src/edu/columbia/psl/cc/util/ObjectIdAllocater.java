package edu.columbia.psl.cc.util;

import java.lang.reflect.Field;
import java.util.BitSet;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import edu.columbia.psl.cc.config.MIBConfiguration;

public class ObjectIdAllocater {
	
	private static Logger logger = Logger.getLogger(ObjectIdAllocater.class);

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
	
	private static BitSet threadRecorder = new BitSet();
	
	private static Object threadRecorderLock = new Object();
	
	private static int mainThreadId = -1;
	
	public static void setMainThreadId(int id) {
		mainThreadId = id;
	}
	
	public static int getMainThreadId() {
		return mainThreadId;
	}
	
	public static boolean isMainThreadAlive() {
		synchronized(threadRecorderLock) {
			return threadRecorder.get(mainThreadId);
		}
	}
	
	public static void clearMainThread() {
		synchronized(threadRecorderLock) {
			threadRecorder.clear(mainThreadId);
		}
	}
	
	public static void clearThreadId(int threadId) {
		synchronized(threadRecorderLock) {
			threadRecorder.clear(threadId);
		}
	}
	
	public static int getThreadId() {
		int threadId = threadIndexer.get();
		synchronized(threadRecorderLock) {
			threadRecorder.set(threadId);
		}
		return threadId;
	}
		
	/**
	 * Main thread is dead and I am the only one left.
	 * Quick but dirty remedy
	 * @return
	 */
	public static boolean secondaryDump(int threadId) {
		boolean mainAlive = threadRecorder.get(mainThreadId);
		if (mainAlive) {
			return false;
		}
		
		BitSet test = new BitSet();
		test.set(threadId);
		test.xor(threadRecorder);
		return test.isEmpty();
	}
		
	public static boolean isThreadRecorderEmpty() {
		logger.info("Check if thread recorder empty: " + threadRecorder);
		return threadRecorder.isEmpty();
	}
	
	public static void checkThreadRecorder() {
		logger.info("Check threadRecorder: " + threadRecorder);
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
	
	public static int getThreadMethodIndex(String className, String methodName, String desc, int threadId) {
		Class<?> correctClass = null;
		if (methodName.equals("<init>") || methodName.equals("<clinit>")) {
			correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(className, methodName, desc, true);
		} else {
			correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(className, methodName, desc, false);
		}
		
		if (correctClass == null) {
			System.out.println("Cannot retrieve correct class: " + className + " " + methodName);
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
	
	public static int parseObjId(Object value) {
		if (value == null)
			return -1;
		
		Class<?> valueClass = value.getClass();
		try {
			Field idField = valueClass.getField(MIBConfiguration.getMibId());
			idField.setAccessible(true);
			/*System.out.println("Traverse fields of " + valueClass);
			for (Field f: valueClass.getFields()) {
				System.out.println(f);
			}*/
			int objId = idField.getInt(value);
			return objId;
		} catch (Exception ex) {
			//ex.printStackTrace();
			//System.out.println("Warning: object " + valueClass + " is not MIB-instrumented");
			logger.warn("Warning: object " + valueClass + " is not MIB-instrumented");
			return -1;
		}
	}
	
	public static void main(String[] args) {
		Object o = new Object();
		//System.out.println(getIndex());
		//System.out.println(getClassMethodIndex("a", "b", "c"));
	}
}
