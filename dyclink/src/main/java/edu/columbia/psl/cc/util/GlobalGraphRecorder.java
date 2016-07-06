package edu.columbia.psl.cc.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.objectweb.asm.Opcodes;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.crawler.NativePackages;
import edu.columbia.psl.cc.pojo.FieldNode;
import edu.columbia.psl.cc.pojo.FieldRecord;
import edu.columbia.psl.cc.pojo.GraphGroup;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.StaticMethodMiner;

public class GlobalGraphRecorder {
	
	private static Logger logger = LogManager.getLogger(GlobalGraphRecorder.class);
	
	private static TypeToken<StaticMethodMiner> smmToken = new TypeToken<StaticMethodMiner>(){};
	
	//Key: class name, Val: short name + thread id of clinit
	private static HashMap<String, String> loadedClass = new HashMap<String, String>();
	
	//private static AtomicLong curDigit = new AtomicLong();
	
	private static AtomicLong curTime = new AtomicLong();
	
	//Key: full name, Val: short name
	private static HashMap<String, String> globalNameMap = new HashMap<String, String>();
	
	private static HashMap<String, Integer> nativePackages = new HashMap<String, Integer>();
	
	private static HashSet<String> undersizedMethods = new HashSet<String>();
	
	private static HashSet<String> untransformedClass = new HashSet<String>();
	
	private static HashMap<String, StaticMethodMiner> staticMethodMinerMap = new HashMap<String, StaticMethodMiner>();
	
	private static HashMap<String, AtomicInteger> shortNameCounter = new HashMap<String, AtomicInteger>();
	
	private static HashMap<String, InstNode> writeFieldMap = new HashMap<String, InstNode>();
	
	private static FieldRecorder fRecorder = new FieldRecorder();
		
	private static HashSet<String> recursiveMethodRecorder = new HashSet<String>();
	
	//private static HashMap<String, List<GraphTemplate>> graphRecorder = new HashMap<String, List<GraphTemplate>>();
	private static HashMap<String, HashMap<String, GraphTemplate>> graphRecorder = new HashMap<String, HashMap<String, GraphTemplate>>();
	
	private static HashMap<Integer, GraphTemplate> latestGraphs = new HashMap<Integer, GraphTemplate>();
	
	private static HashMap<Integer, List<GraphTemplate>> unIdChildGraphs = new HashMap<Integer, List<GraphTemplate>>();
	
	private static HashMap<Integer, LinkedList<HashSet<String>>> stopCallees = new HashMap<Integer, LinkedList<HashSet<String>>>();
	
	private static HashMap<Integer, Stack<Integer>> calleeLines = new HashMap<Integer, Stack<Integer>>();
		
	private static Object timeLock = new Object();
	
	private static Object loadClassLock = new Object();
	
	private static Object writeFieldLock = new Object();
	
	private static Object recursiveMethodLock = new Object();
	
	private static Object nameLock = new Object();
	
	private static Object undersizeLock = new Object();
	
	private static Object untransformedLock = new Object();
	
	private static Object staticMethodMinerLock = new Object();
	
	private static Object graphRecorderLock = new Object();
	
	private static Object unIdChildLock = new Object();
	
	private static Object stopCalleeLock = new Object();
	
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
			if (uni == Long.MAX_VALUE) {
				logger.error("Time incrmenter reaches limit");
				System.exit(-1);
			}
			return uni;
		}
	}
	
	public static void registerWriteField(String field, InstNode writeField) {
		if (!MIBConfiguration.getInstance().isFieldTrack()) {
			return ;
		}
		
		synchronized(writeFieldLock) {
			//logger.info("Register field with writer: " + field + " " + writeField);
			writeFieldMap.put(field, writeField);
		}
	}
	
	public static InstNode getWriteField(String field) {
		if (!MIBConfiguration.getInstance().isFieldTrack()) {
			return null;
		}
		
		synchronized(writeFieldLock) {
			return writeFieldMap.get(field);
		}
	}
		
	public static void removeWriteFields(Collection<String> fields) {
		if (!MIBConfiguration.getInstance().isFieldTrack()) {
			return ;
		}
		
		synchronized(writeFieldLock) {
			for (String f: fields) {
				writeFieldMap.remove(f);
			}
		}
	}
		
	public static void registerRWFieldHistory(InstNode writeInst, InstNode readInst) {
		if (!MIBConfiguration.getInstance().isFieldTrack()) {
			return ;
		}
		
		synchronized(writeFieldLock) {
			fRecorder.registerHistory(writeInst, readInst);
		}
	}
	
	public static void increHistoryFreq(String writeKey, String readKey) {
		if (!MIBConfiguration.getInstance().isFieldTrack()) {
			return ;
		}
		
		synchronized(writeFieldLock) {
			fRecorder.increHistoryFreq(writeKey, readKey);
		}
	}
	
	public static void removeHistory(String writeKey, String readKey) {
		if (!MIBConfiguration.getInstance().isFieldTrack()) {
			return ;
		}
		
		synchronized(writeFieldLock) {
			fRecorder.removeHistory(writeKey, readKey);
		}
	}
		
	/**
	 * If forced, put freq directly. Only for secondary dump.
	 * @param forced
	 * @return
	 */
	public static int constructGlobalRelations(boolean forced) {
		if (!MIBConfiguration.getInstance().isFieldTrack()) {
			return -1;
		}
		
		synchronized(writeFieldLock) {
			int counter = 0;
			
			//Sampling 10% for checking
			StringBuilder dumpFields = new StringBuilder();
			int totalSize = fRecorder.getHistory().values().size();
			int sampleNum = -1;
			if (totalSize <= 50) {
				sampleNum = totalSize;
			} else {
				sampleNum = (int) (totalSize * 0.1);
			}			
			int div = (int)((double)totalSize/sampleNum);
			
			for (FieldRecord fr: fRecorder.getHistory().values()) {
				FieldNode wInst = (FieldNode)fr.getWriteInst();
				FieldNode rInst = (FieldNode)fr.getReadInst();
				double freq = fr.getFreq();
				
				if (freq > 0) {
					String wIdx = StringUtil.genIdxKey(wInst.getThreadId(), wInst.getThreadMethodIdx(), wInst.getIdx());
					String rIdx = StringUtil.genIdxKey(rInst.getThreadId(), rInst.getThreadMethodIdx(), rInst.getIdx());
					
					if (forced) {
						wInst.increChildForced(rInst.getThreadId(), rInst.getThreadMethodIdx(), rInst.getIdx(), freq);
					} else {
						wInst.increChild(rInst.getThreadId(), rInst.getThreadMethodIdx(), rInst.getIdx(), freq);
					}
					rInst.registerParent(wInst.getThreadId(), wInst.getThreadMethodIdx(), wInst.getIdx(), MIBConfiguration.WRITE_DATA_DEP);	
					wInst.addGlobalChild(rIdx);
					
					if (counter % div == 0) {	
						//logger.info(wIdx + " " + rIdx + " " + div);
						dumpFields.append(wIdx + " " + rIdx + "\n");
					}
					counter++;
				}
			}
			
			if (dumpFields.length() > 0) {
				try {
					String path = MIBConfiguration.getInstance().getGraphDir() + "/fields.txt";
					
					FileWriter fw = new FileWriter(path);
					BufferedWriter bw = new BufferedWriter(fw);
					bw.append(dumpFields);
					bw.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
			
			return counter;
		}
	}
					
	public static String registerGlobalName(String className, String methodName, String fullName) {
		String shortNameNoKey = StringUtil.cleanPunc(className, ".") + 
				":" + StringUtil.cleanPunc(methodName);
		
		synchronized(nameLock) {
			if (globalNameMap.containsKey(fullName)) {
				return globalNameMap.get(fullName);
			}
			
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
	
	public static void setGlobalNameMap(HashMap<String, String> globalNameMap) {
		GlobalGraphRecorder.globalNameMap = globalNameMap;
	}
	
	public static HashMap<String, String> getGlobalNameMap() {
		return globalNameMap;
	}
	
	public static void setNativePackages(HashMap<String, Integer> nativePackages) {
		GlobalGraphRecorder.nativePackages = nativePackages;
	}
	
	public static HashMap<String, Integer> getNativePackages() {
		return nativePackages;
	}
	
	public static int getNativePackageId(String packageName) {
		if (nativePackages.containsKey(packageName)) {
			return nativePackages.get(packageName);
		} else {
			return NativePackages.defaultId;
		}
	}
		
	public static void setShortNameCounter(HashMap<String, Integer> rawCounter) {
		for (String key: rawCounter.keySet()) {
			int lastCounter = rawCounter.get(key);
			AtomicInteger ai = new AtomicInteger();
			ai.set(lastCounter);
			shortNameCounter.put(key, ai);
		}
	}
	
	public static HashMap<String, Integer> getShortNameCounter() {
		HashMap<String, Integer> rawCounter = new HashMap<String, Integer>();
		for (String key: shortNameCounter.keySet()) {
			int val = shortNameCounter.get(key).get();
			rawCounter.put(key, val);
		}
		return rawCounter;
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
	
	public static void setRecursiveMethods(HashSet<String> oriRecursiveMethods) {
		recursiveMethodRecorder = oriRecursiveMethods;
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
			if (graph.calleeRequired == null) {
				System.out.println("Suspicious graph: " + shortKey);
				System.exit(-1);
			}
			
			String groupKey = GraphGroup.groupKey(0, graph);
			
			if (graphRecorder.containsKey(shortKey)) {
				HashMap<String, GraphTemplate> subRecord = graphRecorder.get(shortKey);
				
				if (checkRecursiveMethod(shortKey)) {
					if (!subRecord.containsKey(groupKey)) {
						subRecord.put(groupKey, graph);
						graph.mustExist = true;
					} else {
						GraphTemplate inRecord = subRecord.get(groupKey);
						if (graph.getThreadId() == inRecord.getThreadId() 
								&& graph.getThreadMethodId() < inRecord.getThreadMethodId()) {
							subRecord.put(groupKey, graph);
							graph.mustExist = true;
							inRecord.mustExist = false;
						}
					}
				} else {
					if (!subRecord.containsKey(groupKey)) {
						subRecord.put(groupKey, graph);
						graph.mustExist = true;
					}
				}
			} else {
				HashMap<String, GraphTemplate> subRecord = new HashMap<String, GraphTemplate>();
				subRecord.put(groupKey, graph);
				graphRecorder.put(shortKey, subRecord);
				graph.mustExist = true;
			}
			
			if (registerLatest) {
				registerLatestGraph(graph);
			}
			
			//Rarely happens. If the parent constructor calls the child method before child constructor
			if (!graph.isStaticMethod() && graph.getObjId() == 0) {
				logger.info("Register method touched before constructor: " + graph.getMethodKey());
				GlobalGraphRecorder.registerUnIdGraph(graph);
			}
		}
	}
		
	public static void registerLatestGraph(GraphTemplate latestGraph) {
		synchronized(graphRecorderLock) {
			int threadId = ObjectIdAllocater.getThreadId();
			latestGraphs.put(threadId, latestGraph);
		}
	}
		
	public static GraphTemplate getLatestGraph(int threadId) {
		synchronized(graphRecorderLock) {
			return latestGraphs.remove(threadId);
		}
	}
	
	public static void checkLatestGraphs() {
		//System.out.println(latestGraphs);
		for (Integer key: latestGraphs.keySet()) {
			System.out.println("Thread id: " + key);
			System.out.println(latestGraphs.get(key).getMethodKey());
		}
	}
		
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
	
	public static void setUndersizedMethods(HashSet<String> oriUndersizedMethods) {
		undersizedMethods = oriUndersizedMethods;
	}
	
	public static HashSet<String> getUndersizedMethods() {
		return undersizedMethods;
	}
	
	public static void registerUntransformedClass(String className) {
		synchronized(untransformedLock) {
			untransformedClass.add(className);
		}
	}
	
	public static void setUntransformedClass(HashSet<String> oriUntransformedClass) {
		untransformedClass = oriUntransformedClass;
	}
	
	public static boolean checkUntransformedClass(String className) {
		synchronized(untransformedLock) {
			return untransformedClass.contains(className);
		}
	}
	
	public static HashSet<String> getUntransformedClass() {
		return untransformedClass;
	}
	
	public static void registerUnIdGraph(GraphTemplate unIdGraph) {
		synchronized(unIdChildLock) {
			int threadId = ObjectIdAllocater.getThreadId();
			
			if (unIdChildGraphs.containsKey(threadId)) {
				unIdChildGraphs.get(threadId).add(unIdGraph);
			} else {
				List<GraphTemplate> childQueue = new ArrayList<GraphTemplate>();
				childQueue.add(unIdGraph);
				unIdChildGraphs.put(threadId, childQueue);
			}
		}
	}
	
	public static void initUnIdGraphs(int objId) {
		List<GraphTemplate> toGives = null;
		synchronized(unIdChildLock) {
			int threadId = ObjectIdAllocater.getThreadId();
			if (!unIdChildGraphs.containsKey(threadId)) {
				return ;
			}
			toGives = unIdChildGraphs.remove(threadId);
		}
		
		//Rarely happens...
		if (toGives != null) {
			for (GraphTemplate toGive: toGives) {
				toGive.setObjId(objId);
				
				for (InstNode inst: toGive.getInstPool()) {
					int opcode = inst.getOp().getOpcode();
					if (opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD) {
						try {
							String[] parsedInfo = inst.getAddInfo().split(":");
							String newInfo = parsedInfo[0];
							int curObjId = Integer.valueOf(parsedInfo[parsedInfo.length - 1]);
							if (curObjId == 0) {
								newInfo = newInfo + ":" + objId;
							}
							inst.setAddInfo(newInfo);
						} catch (Exception ex) {
							logger.info("Exception: " + ex);
						}
					}
				}
			}
		}
	}
	
	public static void enqueueStopCallees() {
		synchronized(stopCalleeLock) {
			int threadId = ObjectIdAllocater.getThreadId();
			//System.out.println("Enqueue stop callee record: " + threadId);
			HashSet<String> emptyCallees = new HashSet<String>();
			if (stopCallees.containsKey(threadId)) {
				stopCallees.get(threadId).add(emptyCallees);
			} else {
				LinkedList<HashSet<String>> queue = new LinkedList<HashSet<String>>();
				queue.add(emptyCallees);
				stopCallees.put(threadId, queue);
			}
		}
	}
	
	public static void dequeueStopCallees() {
		synchronized(stopCalleeLock) {
			int threadId = ObjectIdAllocater.getThreadId();
			stopCallees.get(threadId).removeLast();
			
			if (stopCallees.get(threadId).size() == 0) {
				stopCallees.remove(threadId);
			}
		}
	}
	
	public static void registerStopCallee(String calleeKey, int myLine) {
		synchronized(stopCalleeLock) {
			int threadId = ObjectIdAllocater.getThreadId();
			String fullKey = calleeKey + "-" + myLine;
			stopCallees.get(threadId).getLast().add(fullKey);
		}
	}
	
	public static void enqueueCalleeLine(int linenumber) {
		synchronized(stopCalleeLock) {
			int threadId = ObjectIdAllocater.getThreadId();
			//System.out.println("Enter calle line: " + threadId + " " + linenumber);
			
			if (!calleeLines.containsKey(threadId)) {
				Stack<Integer> stack = new Stack<Integer>();
				calleeLines.put(threadId, stack);
			}
			
			calleeLines.get(threadId).push(linenumber);
		}
	}
	
	public static void dequeueCalleeLine() {
		synchronized(stopCalleeLock) {
			int threadId = ObjectIdAllocater.getThreadId();
			calleeLines.get(threadId).pop();
			//System.out.println("Leave callee line: " + threadId + " " + calleeLines.get(threadId).pop());
		}
	}
	
	public static boolean shouldStopMe(String calleeKey) {
		synchronized(stopCalleeLock) {
			int threadId = ObjectIdAllocater.getThreadId();
			if (!stopCallees.containsKey(threadId)) {
				//this means that this method is the first under current thread
				return false;
			}
			
			if (stopCallees.size() == 0) {
				logger.error("Suspicious stop callees: " + calleeKey);
				return false;
			}
			
			if (calleeLines.get(threadId).size() == 0) {
				logger.error("No callee line information: " + calleeKey);
				System.exit(-1);
			}
			
			Integer myLine = calleeLines.get(threadId).peek();
			if (myLine == null) {
				logger.error("Error retrieve callee line for: " + calleeKey);
				return false;
			}
			
			String fullKey = calleeKey + "-" + myLine.toString();
			
			/*if (calleeKey.contains("<init>")) {
				System.out.println("Stop callees: " + stopCallees.get(threadId));
				System.out.println("Query: " + fullKey);
				System.out.println("Result: " + stopCallees.get(threadId).getLast().contains(fullKey));
			}*/
			return stopCallees.get(threadId).getLast().contains(fullKey);
		}
	}
		
	public static void clearContext() {
		synchronized(graphRecorderLock) {
			graphRecorder.clear();
			latestGraphs.clear();
		}
		
		synchronized(writeFieldLock) {
			writeFieldMap.clear();
			fRecorder.cleanRecorder();
		}
		
		synchronized(unIdChildLock) {
			unIdChildGraphs.clear();
		}
		
		synchronized(timeLock) {
			//curDigit.set(0);
			curTime.set(0);
		}
		
		synchronized(stopCalleeLock) {
			stopCallees.clear();
			calleeLines.clear();
		}
		System.gc();
	}
	
	public static void main(String[] args) {
		
	}
}
