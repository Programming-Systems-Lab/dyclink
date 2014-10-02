package edu.columbia.psl.cc.util;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.OpcodeObj;

public class InstNodeAdapter implements JsonSerializer<InstNode>, JsonDeserializer<InstNode>{

	@Override
	public InstNode deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		JsonObject object = json.getAsJsonObject();
		if (json.isJsonObject()) {
			InstNode inst = new InstNode();
			int idx = object.get("idx").getAsInt();
			int opcode = object.get("op").getAsInt();
			OpcodeObj oo = BytecodeCategory.getOpcodeObj(opcode);
			String addInfo = object.get("addInfo").getAsString();
			
			inst.setIdx(idx);
			inst.setOp(oo);
			inst.setAddInfo(addInfo);
			return inst;
		} else {
			String rawKey = json.getAsString();
			String[] parseKey = rawKey.split(" ");
			InstNode inst = new InstNode();
			
			for (int i = 0; i < parseKey.length; i++) {
				if (i == 0) {
					int idx = Integer.valueOf(parseKey[i]);
					inst.setIdx(idx);
				} else if (i == 1) {
					OpcodeObj oo = BytecodeCategory.getOpcodeObj(Integer.valueOf(parseKey[i]));
					inst.setOp(oo);
				} else if (i == 2) {
					String addInfo = parseKey[i];
					inst.setAddInfo(addInfo);
				}
			}
			return inst;
		}
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
		
		return result;
	}
}
