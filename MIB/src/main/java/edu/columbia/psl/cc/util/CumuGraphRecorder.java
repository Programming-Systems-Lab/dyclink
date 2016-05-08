package edu.columbia.psl.cc.util;

import java.util.HashMap;

import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;

public class CumuGraphRecorder {
	
	private static HashMap<String, GraphTemplate> STATIC_MAP = new HashMap<String, GraphTemplate>();
	
	private static HashMap<Integer, GraphTemplate> OBJ_MAP = new HashMap<Integer, GraphTemplate>();
	
	private static HashMap<String, InstNode> FIELD_MAP = new HashMap<String, InstNode>();
	
	private static final Object STATIC_LOCK = new Object();
	
	private static final Object OBJ_LOCK = new Object();
	
	private static final Object FIELD_LOCK = new Object();
	
	public static void registerStaticGraph(String shortKey, GraphTemplate graph) {
		synchronized(STATIC_LOCK) {
			STATIC_MAP.put(shortKey, graph);
		}
	}
	
	public static GraphTemplate retrieveStaticGraph(String shortKey) {
		synchronized(STATIC_LOCK) {
			return STATIC_MAP.get(shortKey);
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
		}
	}
	
	public static void registerObjGraph(int objId, GraphTemplate graph) {
		synchronized(OBJ_LOCK) {
			OBJ_MAP.put(objId, graph);
		}
	}
	
	public static GraphTemplate retrieveObjGraph(int objId) {
		synchronized(OBJ_LOCK) {
			return OBJ_MAP.get(objId);
		}
	}

}
