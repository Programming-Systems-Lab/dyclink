package edu.columbia.psl.cc.util;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.pojo.LocalVar;
import edu.columbia.psl.cc.pojo.ObjVar;
import edu.columbia.psl.cc.pojo.Var;

public class VarAdapter implements JsonSerializer<Var>, JsonDeserializer<Var>{

	@Override
	public Var deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		JsonObject object = json.getAsJsonObject();
		Var var = null;
		
		JsonElement ncClassElement = object.get("nativeClassElement");
		JsonElement varNameElement = object.get("varName");
		JsonElement varIdElement = object.get("localVarId");
		if (ncClassElement != null) {
			var = new ObjVar();
			String ncClassName = ncClassElement.getAsString();
			String varName = varNameElement.getAsString();
			((ObjVar)var).setNativeClassName(ncClassName);
			((ObjVar)var).setVarName(varName);
		} else if (varIdElement != null) {
			var = new LocalVar();
			int localVarId = varIdElement.getAsInt();
			((LocalVar)var).setLocalVarId(localVarId);
		}
		
		JsonElement classElement = object.get("className");
		JsonElement methodElement = object.get("methodName");
		JsonElement silIdElement = object.get("silId");
		
		/*JsonElement childrenElement = object.get("children");
		JsonObject childrenObj = childrenElement.getAsJsonObject();
		HashMap<String, Set<Var>> childrenMap = new HashMap<String, Set<Var>>();
		TypeToken<Set<Var>> varSetType = new TypeToken<Set<Var>>(){};
		for (Map.Entry<String, JsonElement>entry: childrenObj.entrySet()) {
			childrenMap.put(entry.getKey(), context.<Set<Var>>deserialize(entry.getValue(), varSetType.getType()));
		}*/
		
		JsonElement childrenRepElement = object.get("childrenRep");
		JsonObject childrenRepObj = childrenRepElement.getAsJsonObject();
		HashMap<String, Set<String>> childrenRepMap = new HashMap<String, Set<String>>();
		TypeToken<Set<String>> varRepSetType = new TypeToken<Set<String>>(){};
		for (Map.Entry<String, JsonElement>entry: childrenRepObj.entrySet()) {
			childrenRepMap.put(entry.getKey(), context.<Set<String>>deserialize(entry.getValue(), varRepSetType.getType()));
		}
		
		var.setClassName(classElement.getAsString());
		var.setMethodName(methodElement.getAsString());
		var.setSilId(silIdElement.getAsInt());
		var.setChildren(new HashMap<String, Set<Var>>());
		var.setChildrenRep(childrenRepMap);
		return var;
	}

	@Override
	public JsonElement serialize(Var var, Type typeOfSrc,
			JsonSerializationContext context) {
		JsonObject result = new JsonObject();
		result.addProperty("className", var.getClassName());
		result.addProperty("methodName", var.getMethodName());
		result.addProperty("silId", var.getSilId());
		
		/*TypeToken<Map<String, Set<Var>>> tt = new TypeToken<Map<String, Set<Var>>>(){};		
		JsonElement varChildren = context.serialize(var.getChildren(), tt.getType());
		result.add("children", varChildren);*/
		
		TypeToken<Map<String, Set<String>>> tt = new TypeToken<Map<String, Set<String>>>(){};
		HashMap<String, Set<String>> childrenRepMap = new HashMap<String, Set<String>>();
		for (String label: var.getChildren().keySet()) {
			Set<Var> children = var.getChildren().get(label);
			Set<String> childrenRep = new HashSet<String>();
			for (Var v: children) {
				childrenRep.add(v.toString());
			}
			childrenRepMap.put(label, childrenRep);
		}
		JsonElement varChildrenRep = context.serialize(childrenRepMap, tt.getType());
		result.add("childrenRep", varChildrenRep);
		
		if (var.getSilId() < 2) {
			ObjVar ov = (ObjVar)var;
			result.addProperty("nativeClassName", ov.getNativeClassName());
			result.addProperty("varName", ov.getVarName());
		} else {
			LocalVar lv = (LocalVar)var;
			result.addProperty("localVarId", lv.getLocalVarId());
		}
		return result;
	}

}
