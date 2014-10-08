package edu.columbia.psl.cc.util;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

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
	
	private InstPool pool = new InstPool();
	
	@Override
	public InstNode deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		JsonObject object = json.getAsJsonObject();
		String methodKey = object.get("fromMethod").getAsString();
		int idx = object.get("idx").getAsInt();
		int opcode = object.get("op").getAsInt();
		String addInfo = object.get("addInfo").getAsString();
		
		JsonArray dataParentObj = object.get("dataParentList").getAsJsonArray();
		ArrayList<String> dataParentList = new ArrayList<String>();
		for (int i = 0; i < dataParentObj.size(); i++) {
			dataParentList.add(dataParentObj.get(i).getAsString());
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
		
		InstNode inst = this.pool.searchAndGet(methodKey, idx, opcode, addInfo);
		inst.setFromMethod(methodKey);
		inst.setDataParentList(dataParentList);
		inst.setControlParentList(controlParentList);
		inst.setChildFreqMap(childFreqMap);
		//inst.setChildFreqMap((TreeMap<Integer, Double>)context.deserialize(object.get("childFreqpMap"), childToken.getType()));
		//inst.setParentList((ArrayList<Integer>)context.deserialize(object.get("parentList"), parentToken.getType()));
		return inst;
	}

	@Override
	public JsonElement serialize(InstNode inst, Type typeOfSrc,
			JsonSerializationContext context) {
		JsonObject result = new JsonObject();
		result.addProperty("fromMethod", inst.getFromMethod())
;		result.addProperty("idx", inst.getIdx());
		result.addProperty("op", inst.getOp().getOpcode());
		//For debuggin purpose, or we only need opcode
		result.addProperty("inst", inst.getOp().getInstruction());
		result.addProperty("addInfo", inst.getAddInfo());
		
		TypeToken<ArrayList<String>> listType = new TypeToken<ArrayList<String>>(){};
		result.add("dataParentList", context.serialize(inst.getDataParentList(), listType.getType()));
		result.add("controlParentList", context.serialize(inst.getControlParentList(), listType.getType()));
		
		TypeToken<TreeMap<String, Double>> mapType = new TypeToken<TreeMap<String, Double>>(){};
		result.add("childFreqMap", context.serialize(inst.getChildFreqMap(), mapType.getType()));
		
		return result;
	}
}
