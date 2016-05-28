package edu.columbia.psl.cc.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

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
	
	private static HashMap<Integer, InstNode> IDX_MAP = null;
	
	private static InstNode CALLER_CONTROL = null;
	
	private static Stack<InstNode> CALLEE_LASTS = new Stack<InstNode>();
	
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
				logger.warn("Non-initialized field: " + fieldKey);
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
	
	public static List<AbstractGraph> getCumuGraphs() {
		List<AbstractGraph> allGraphs = new ArrayList<AbstractGraph>();
		
		synchronized(POOL_LOCK) {
			GraphTemplate all = new GraphTemplate();
			int edge = 0;
			for (InstNode inst: POOL) {
				edge += inst.getChildFreqMap().size();
			}
			all.setMethodKey(DUMP_FULL_NAME);
			all.setShortMethodKey(DUMP_GLOBAL_NAME);
			all.setThreadId(DUMP_THREAD_ID);
			all.setInstPool(POOL);
			all.setVertexNum(POOL.size());
			all.setEdgeNum(edge);
			allGraphs.add(all);
		}
		
		return allGraphs;
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
	
	public static void pushCalleeLast(InstNode result) {
		CALLEE_LASTS.push(result);
		System.out.println("Push result: " + result);
		System.out.println("Pushed stack: " + CALLEE_LASTS);
	}
	
	public static InstNode popCalleeLast() {
		if (CALLEE_LASTS.size() > 1) {
			logger.error("Errornous callee results: " + CALLEE_LASTS);
			System.exit(-1);
		}
		
		InstNode result = CALLEE_LASTS.pop();
		System.out.println("Pop result: " + result);
		System.out.println("Popped stack: " + CALLEE_LASTS);
		return result;
	}
}
