package edu.columbia.psl.cc.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import edu.columbia.psl.cc.abs.AbstractGraph;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.CumuGraph;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;

public class CumuGraphRecorder extends GlobalGraphRecorder {
	
	private static HashMap<String, int[]> STATIC_RECORD = new HashMap<String, int[]>();
	
	private static HashMap<Integer, HashMap<String, int[]>> OBJ_RECORD = 
			new HashMap<Integer, HashMap<String, int[]>>();
	
	private static HashMap<String, InstNode> FIELD_MAP = new HashMap<String, InstNode>();
	
	private static HashMap<Integer, InstNode> IDX_MAP = null;
	
	private static InstNode CALLER_CONTROL = null;
	
	private static Stack<InstNode> CALLEE_RESULTS = new Stack<InstNode>();
	
	private static final InstPool POOL = new InstPool();
	
	private static final Object STATIC_LOCK = new Object();
	
	private static final Object OBJ_LOCK = new Object();
	
	private static final Object FIELD_LOCK = new Object();
	
	private static final Object POOL_LOCK = new Object();
	
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
		return IDX_MAP;
	}
	
	public static void registerCallerControl(InstNode toRegister) {
		CALLER_CONTROL = toRegister;
	}
	
	public static InstNode retrieveCallerControl() {
		return CALLER_CONTROL;
	}
	
	public static void pushCalleeResult(InstNode result) {
		CALLEE_RESULTS.push(result);
	}
	
	public static InstNode popCalleeResult(boolean twice) {
		InstNode result = CALLEE_RESULTS.pop();
		if (twice) {
			CALLEE_RESULTS.pop();
		}
		
		return result;
	}
}
