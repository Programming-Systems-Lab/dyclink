package edu.columbia.psl.cc.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.columbia.psl.cc.abs.AbstractGraph;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.CumuGraph;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;

public class CumuGraphRecorder extends GlobalGraphRecorder {
	
	private static final Logger logger = LogManager.getLogger(CumuGraphRecorder.class);
	
	private static HashMap<String, int[]> STATIC_RECORD = new HashMap<String, int[]>();
	
	private static HashMap<Integer, HashMap<String, int[]>> OBJ_RECORD = 
			new HashMap<Integer, HashMap<String, int[]>>();
	
	private static HashMap<String, InstNode> FIELD_MAP = new HashMap<String, InstNode>();
	
	private static HashSet<String> NON_INIT_FIELDS = new HashSet<String>();
	
	private static HashMap<Integer, InstNode> IDX_MAP = null;
	
	private static InstNode CALLER_CONTROL = null;
	
	//private static Stack<InstNode> CALLEE_LASTS = new Stack<InstNode>();
	private static Map<String, Stack<InstNode>> CALLEE_LASTS = new TreeMap<String, Stack<InstNode>>();
	
	private static final InstPool POOL = new InstPool();
	
	private static final Object STATIC_LOCK = new Object();
	
	private static final Object OBJ_LOCK = new Object();
	
	private static final Object FIELD_LOCK = new Object();
	
	private static final Object POOL_LOCK = new Object();
	
	public static String DUMP_FULL_NAME;
	
	public static String DUMP_GLOBAL_NAME;
	
	public static int DUMP_THREAD_ID;
	
	public static void registerStaticRecord(String methodKey, int[] methodInfo) {
		synchronized(STATIC_LOCK) {
			STATIC_RECORD.put(methodKey, methodInfo);
		}
	}
	
	public static int[] queryStaticRecord(String methodKey) {
		synchronized(STATIC_LOCK) {
			return STATIC_RECORD.get(methodKey);
		}
	}
	
	public static void registerWriterField(String fieldKey, InstNode writer) {
		synchronized(FIELD_LOCK) {
			FIELD_MAP.put(fieldKey, writer);
		}
	}
	
	public static void updateReaderField(String fieldKey, InstNode reader) {
		synchronized(FIELD_LOCK) {
			InstNode writer = FIELD_MAP.get(fieldKey);
			
			if (writer == null) {
				//logger.warn("Non-initialized field: " + fieldKey);
				NON_INIT_FIELDS.add(fieldKey);
				return ;
			}
			
			writer.increChild(reader.getThreadId(), reader.getThreadMethodIdx(), reader.getIdx(), 1.0);
			reader.registerParent(writer.getThreadId(), writer.getThreadMethodIdx(), writer.getIdx(), MIBConfiguration.WRITE_DATA_DEP);
		}
	}
	
	public static void registerObjRecord(int objId, String methodKey, int[] methodInfo) {
		synchronized(OBJ_LOCK) {
			if (OBJ_RECORD.containsKey(objId)) {
				HashMap<String, int[]> methodRecord = OBJ_RECORD.get(objId);
				methodRecord.put(methodKey, methodInfo);
			} else {
				HashMap<String, int[]> methodRecord = new HashMap<String, int[]>();
				methodRecord.put(methodKey, methodInfo);
				OBJ_RECORD.put(objId, methodRecord);
			}
		}
	}
	
	public static int[] queryObjRecord(int objId, String methodKey) {
		synchronized(OBJ_LOCK) {
			if (!OBJ_RECORD.containsKey(objId)) {
				return null;
			}
			
			return OBJ_RECORD.get(objId).get(methodKey);
		}
	}
	
	public static List<AbstractGraph> getCumuGraph() {		
		synchronized(POOL_LOCK) {
			ShutdownLogger.appendMessage("Total graph size: " + POOL.size());
			List<AbstractGraph> ret = new ArrayList<AbstractGraph>();
			
			int counter = 0;
			int edgeNum = 0;
			GraphTemplate sepAll = new GraphTemplate();
			InstPool sepPool = new InstPool();
			
			for (InstNode inst: POOL) {
				if (counter == 10000) {
					//Finalize sep graph info
					sepAll.setMethodKey(DUMP_FULL_NAME);
					sepAll.setShortMethodKey(DUMP_GLOBAL_NAME);
					sepAll.setThreadId(DUMP_THREAD_ID);
					sepAll.setInstPool(sepPool);
					sepAll.setVertexNum(sepPool.size());
					sepAll.setEdgeNum(edgeNum);
					ret.add(sepAll);
					
					//Reinitialize sep graph
					sepAll = new GraphTemplate();
					sepPool = new InstPool();
					counter = 0;
					edgeNum = 0;
				}
				
				sepPool.add(inst);
				edgeNum += inst.getChildFreqMap().size();
				counter++;
			}
			
			//Residual
			if (counter > 0) {
				sepAll.setMethodKey(DUMP_FULL_NAME);
				sepAll.setShortMethodKey(DUMP_GLOBAL_NAME);
				sepAll.setThreadId(DUMP_THREAD_ID);
				sepAll.setInstPool(sepPool);
				sepAll.setVertexNum(sepPool.size());
				sepAll.setEdgeNum(edgeNum);
				ret.add(sepAll);
			}
			
			ShutdownLogger.appendMessage("Separate graphs into: " + ret.size());
			return ret;
		}
	}
	
	public static InstNode queryInst(String methodKey, 
			int threadId, 
			int threadMethodIdx, 
			int idx, 
			int opcode, 
			String addInfo, 
			int request) {
		synchronized(POOL_LOCK) {			
			return POOL.searchAndGet(methodKey, threadId, threadMethodIdx, idx, opcode, addInfo, request);
		}
	}
	
	public static InstNode queryInst(String idxKey) {
		synchronized(POOL_LOCK) {
			return POOL.searchAndGet(idxKey);
		}
	}
	
	public static boolean removeInst(InstNode inst) {
		synchronized(POOL_LOCK) {
			return POOL.remove(inst);
		}
	}
	
	public static void registerIdxMap(HashMap<Integer, InstNode> toRegister) {
		IDX_MAP = toRegister;
	}
	
	public static HashMap<Integer, InstNode> retrieveIdxMap() {
		HashMap<Integer, InstNode> ret = IDX_MAP;
		IDX_MAP = null;
		return ret;
	}
	
	public static void registerCallerControl(InstNode toRegister) {
		CALLER_CONTROL = toRegister;
	}
	
	public static InstNode retrieveCallerControl() {
		InstNode ret = CALLER_CONTROL;
		CALLER_CONTROL = null;
		return ret;
	}
	
	public static void pushCalleeLast(String caller, InstNode inst) {
		//CALLEE_LASTS.push(result);
		
		if (CALLEE_LASTS.containsKey(caller)) {
			CALLEE_LASTS.get(caller).push(inst);
		} else {
			Stack<InstNode> insts = new Stack<InstNode>();
			insts.push(inst);
			CALLEE_LASTS.put(caller, insts);
		}		
		
		/*if (caller.equals("org.python.core.PyObject:__call__:(Lorg.python.core.PyObject+Lorg.python.core.PyObject+Lorg.python.core.PyObject):Lorg.python.core.PyObject")) {
			System.out.println("Push result: " + caller);
			CumuGraphRecorder.showCalleeLasts(caller);
		}*/	
	}
		
	public static InstNode popCalleeLast(String caller) {
		/*if (CALLEE_LASTS.size() > 1) {
			logger.error("Errornous callee results: " + CALLEE_LASTS);
			System.exit(-1);
		}*/
		
		Stack<InstNode> insts = CALLEE_LASTS.get(caller);
		if (insts == null || insts.size() == 0) {
			return null;
		}
		
		InstNode toReturn = insts.pop();
		return toReturn;
	}
	
	public static void showCalleeLasts(String target) {
		//System.out.println(CALLEE_LASTS);
		//logger.info(CALLEE_LASTS);
		Stack<InstNode> lasts = CALLEE_LASTS.get(target);
		if (lasts == null) {
			logger.error("Find no inst for target: " + target);
		} else {
			logger.info("Insts for " + target);
			logger.info(lasts);
		}
	}
	
	public static void showCalleeLastsContains(String contain) {
		for (String k: CALLEE_LASTS.keySet()) {
			if (k.contains(contain)) {
				logger.info(k + ": " + CALLEE_LASTS.get(k));
			}
		}
	}
}
