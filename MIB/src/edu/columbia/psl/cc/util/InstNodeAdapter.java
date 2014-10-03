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
		int idx = object.get("idx").getAsInt();
		int opcode = object.get("op").getAsInt();
		String addInfo = object.get("addInfo").getAsString();
		
		JsonArray parentObj = object.get("parentList").getAsJsonArray();
		ArrayList<Integer> parentList = new ArrayList<Integer>();
		for (int i = 0; i < parentObj.size(); i++) {
			parentList.add(parentObj.get(i).getAsInt());
		}
		
		JsonObject childObj = object.get("childFreqMap").getAsJsonObject();
		TreeMap<Integer, Double> childFreqMap = new TreeMap<Integer, Double>();
		
		for (Entry<String, JsonElement> entry: childObj.entrySet()) {
			int instIdx = Integer.valueOf(entry.getKey());
			double instFreq = entry.getValue().getAsDouble();
			childFreqMap.put(instIdx, instFreq);
		}
		
		//TypeToken<TreeMap<Integer, Double>> childToken = new TypeToken<TreeMap<Integer, Double>>(){};
		//TypeToken<ArrayList<Integer>> parentToken = new TypeToken<ArrayList<Integer>>(){};
		
		InstNode inst = this.pool.searchAndGet(idx, opcode, addInfo);
		inst.setParentList(parentList);
		inst.setChildFreqMap(childFreqMap);
		//inst.setChildFreqMap((TreeMap<Integer, Double>)context.deserialize(object.get("childFreqpMap"), childToken.getType()));
		//inst.setParentList((ArrayList<Integer>)context.deserialize(object.get("parentList"), parentToken.getType()));
		return inst;
	}

	@Override
	public JsonElement serialize(InstNode inst, Type typeOfSrc,
			JsonSerializationContext context) {
		JsonObject result = new JsonObject();
		result.addProperty("idx", inst.getIdx());
		result.addProperty("op", inst.getOp().getOpcode());
		//For debuggin purpose, or we only need opcode
		result.addProperty("inst", inst.getOp().getInstruction());
		result.addProperty("addInfo", inst.getAddInfo());
		
		TypeToken<ArrayList<Integer>> listType = new TypeToken<ArrayList<Integer>>(){};
		result.add("parentList", context.serialize(inst.getParentList(), listType.getType()));
		
		TypeToken<TreeMap<Integer, Double>> mapType = new TypeToken<TreeMap<Integer, Double>>(){};
		result.add("childFreqMap", context.serialize(inst.getChildFreqMap(), mapType.getType()));
		
		return result;
	}
}
