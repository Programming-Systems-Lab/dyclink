package edu.columbia.psl.cc.util;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.OpcodeObj;

public class InstNodeAdapter implements JsonSerializer<InstNode>, JsonDeserializer<InstNode>{
	
	private Logger logger = Logger.getLogger(InstNodeAdapter.class);
	
	private InstPool pool = new InstPool();
	
	@Override
	public InstNode deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		JsonObject object = json.getAsJsonObject();
		String methodKey = object.get("fromMethod").getAsString();
		long threadId = object.get("threadId").getAsLong();
		int threadMethodIdx = object.get("threadMethodIdx").getAsInt();
		int idx = object.get("idx").getAsInt();
		int linenumber = object.get("linenumber").getAsInt();
		long startDigit = object.get("startDigit").getAsLong();
		long startTime = object.get("startTime").getAsLong();
		long updateDigit = object.get("updateDigit").getAsLong();
		long updateTime = object.get("updateTime").getAsLong();
		int opcode = object.get("op").getAsInt();
		String addInfo = object.get("addInfo").getAsString();
		
		JsonArray instDataParentObj = object.get("instDataParentList").getAsJsonArray();
		ArrayList<String> instDataParentList = new ArrayList<String>();
		for (int i = 0; i < instDataParentObj.size(); i++) {
			instDataParentList.add(instDataParentObj.get(i).getAsString());
		}
		
		JsonArray writeDataParentObj = object.get("writeDataParentList").getAsJsonArray();
		ArrayList<String> writeDataParentList = new ArrayList<String>();
		for (int i = 0; i < writeDataParentObj.size(); i++) {
			writeDataParentList.add(writeDataParentObj.get(i).getAsString());
		}
		
		JsonArray controlParentObj = object.get("controlParentList").getAsJsonArray();
		ArrayList<String> controlParentList = new ArrayList<String>();
		for (int i = 0; i < controlParentObj.size(); i++) {
			controlParentList.add(controlParentObj.get(i).getAsString());
		}
		
		JsonObject childObj = object.get("childFreqMap").getAsJsonObject();
		TreeMap<String, Double> childFreqMap = new TreeMap<String, Double>();
		
		for (Entry<String, JsonElement> entry: childObj.entrySet()) {
			double instFreq = entry.getValue().getAsDouble();
			childFreqMap.put(entry.getKey(), instFreq);
		}
		
		//TypeToken<TreeMap<Integer, Double>> childToken = new TypeToken<TreeMap<Integer, Double>>(){};
		//TypeToken<ArrayList<Integer>> parentToken = new TypeToken<ArrayList<Integer>>(){};
		
		InstNode inst = this.pool.searchAndGet(methodKey, threadId, threadMethodIdx, idx, opcode, addInfo);
		if (BytecodeCategory.writeFieldCategory().contains(inst.getOp().getCatId())) {
			InstNode nodeInMemory = GlobalRecorder.getWriteFieldNode(inst.getAddInfo());
			if (nodeInMemory != null && nodeInMemory.equals(inst)) {
				this.pool.remove(inst);
				this.pool.add(nodeInMemory);
				return nodeInMemory;
			}
		}
		
		inst.setInstDataParentList(instDataParentList);
		inst.setWriteDataParentList(writeDataParentList);
		inst.setControlParentList(controlParentList);
		inst.setChildFreqMap(childFreqMap);
		inst.setStartDigit(startDigit);
		inst.setStartTime(startTime);
		inst.setUpdateDigit(updateDigit);
		inst.setUpdateTime(updateTime);
		inst.setLinenumber(linenumber);
		//inst.setChildFreqMap((TreeMap<Integer, Double>)context.deserialize(object.get("childFreqpMap"), childToken.getType()));
		//inst.setParentList((ArrayList<Integer>)context.deserialize(object.get("parentList"), parentToken.getType()));
		return inst;
	}

	@Override
	public JsonElement serialize(InstNode inst, Type typeOfSrc,
			JsonSerializationContext context) {
		JsonObject result = new JsonObject();
		result.addProperty("fromMethod", inst.getFromMethod());		
		result.addProperty("threadId", inst.getThreadId());
		result.addProperty("threadMethodIdx", inst.getThreadMethodIdx());
		result.addProperty("idx", inst.getIdx());
		result.addProperty("startDigit", inst.getStartDigit());
		result.addProperty("startTime", inst.getStartTime());
		result.addProperty("updateDigit", inst.getUpdateDigit());
		result.addProperty("updateTime", inst.getUpdateTime());
		result.addProperty("linenumber", inst.getLinenumber());
		result.addProperty("op", inst.getOp().getOpcode());
		//For debuggin purpose, or we only need opcode
		result.addProperty("inst", inst.getOp().getInstruction());
		result.addProperty("addInfo", inst.getAddInfo());
		
		TypeToken<ArrayList<String>> listType = new TypeToken<ArrayList<String>>(){};
		result.add("instDataParentList", context.serialize(inst.getInstDataParentList(), listType.getType()));
		result.add("writeDataParentList", context.serialize(inst.getWriteDataParentList(), listType.getType()));
		result.add("controlParentList", context.serialize(inst.getControlParentList(), listType.getType()));
		
		TypeToken<TreeMap<String, Double>> mapType = new TypeToken<TreeMap<String, Double>>(){};
		result.add("childFreqMap", context.serialize(inst.getChildFreqMap(), mapType.getType()));
		
		return result;
	}
}
