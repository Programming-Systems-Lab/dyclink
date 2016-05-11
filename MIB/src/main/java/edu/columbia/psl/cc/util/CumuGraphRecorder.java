package edu.columbia.psl.cc.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.columbia.psl.cc.abs.AbstractGraph;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.CumuGraph;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;

public class CumuGraphRecorder extends GlobalGraphRecorder {
	
	private static HashMap<String, CumuGraph> STATIC_MAP = new HashMap<String, CumuGraph>();
	
	private static HashMap<Integer, HashMap<String, CumuGraph>> OBJ_MAP = 
			new HashMap<Integer, HashMap<String, CumuGraph>>();
	
	private static HashMap<String, InstNode> FIELD_MAP = new HashMap<String, InstNode>();
	
	private static final Object STATIC_LOCK = new Object();
	
	private static final Object OBJ_LOCK = new Object();
	
	private static final Object FIELD_LOCK = new Object();
	
	public static void registerStaticGraph(String methodKey, CumuGraph graph) {
		synchronized(STATIC_LOCK) {
			STATIC_MAP.put(methodKey, graph);
		}
	}
	
	public static CumuGraph retrieveStaticGraph(String methodKey) {
		synchronized(STATIC_LOCK) {
			return STATIC_MAP.get(methodKey);
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
	
	public static void registerObjGraph(CumuGraph graph) {
		synchronized(OBJ_LOCK) {
			int objId = graph.getObjId();
			String methodKey = graph.getMethodKey();
			if (OBJ_MAP.containsKey(objId)) {
				HashMap<String, CumuGraph> methodMap = OBJ_MAP.get(objId);
				
				if (!methodMap.containsKey(methodKey)) {
					methodMap.put(methodKey, graph);
				}
				
			} else {
				HashMap<String, CumuGraph> methodMap = new HashMap<String, CumuGraph>();
				methodMap.put(methodKey, graph);
				OBJ_MAP.put(objId, methodMap);
			}
		}
	}
	
	public static CumuGraph retrieveObjGraph(int objId, String methodKey) {
		synchronized(OBJ_LOCK) {
			if (!OBJ_MAP.containsKey(objId)) {
				return null;
			}
			
			return OBJ_MAP.get(objId).get(methodKey);
		}
	}
	
	public static List<AbstractGraph> getCumuGraphs() {
		List<AbstractGraph> allGraphs = new ArrayList<AbstractGraph>();
		synchronized(OBJ_LOCK) {
			for (Integer objId: OBJ_MAP.keySet()) {
				HashMap<String, CumuGraph> methodMap = OBJ_MAP.get(objId);
				allGraphs.addAll(methodMap.values());
			}
		}
		
		synchronized(STATIC_LOCK) {
			allGraphs.addAll(STATIC_MAP.values());
		}
		
		return allGraphs;
	}
}
