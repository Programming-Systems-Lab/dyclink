package edu.columbia.psl.cc.util;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Textifier;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.FieldNode;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.MethodNode;
import edu.columbia.psl.cc.pojo.MethodNode.CalleeInfo;
import edu.columbia.psl.cc.pojo.MethodNode.RegularState;

public class InstNodeAdapter implements JsonSerializer<InstNode>, JsonDeserializer<InstNode>{
	
	//private static Logger logger = Logger.getLogger(InstNodeAdapter.class);
	
	private InstPool pool = new InstPool();
	
	public InstNodeAdapter() {
		
	}
	
	static HashMap<String, Integer> opToInt = new HashMap<>();
	static
	{
		opToInt.put("literal", Opcodes.ICONST_0);
		opToInt.put("write", Opcodes.ISTORE);
		opToInt.put("end expression", Opcodes.NOP);
		opToInt.put("read", Opcodes.ILOAD);
		opToInt.put("invokeFunPre", Opcodes.INVOKEVIRTUAL);
		opToInt.put("<=", Opcodes.IF_ICMPLE);
		opToInt.put("=>", Opcodes.IF_ICMPGE);
		opToInt.put("==", Opcodes.IF_ICMPEQ);
		opToInt.put("<", Opcodes.IF_ICMPLT);
		opToInt.put(">", Opcodes.IF_ICMPGT);
		opToInt.put("!==", Opcodes.IF_ICMPNE);
		opToInt.put("conditional", Opcodes.IF_ICMPGT);
		opToInt.put("%", Opcodes.IREM);
		opToInt.put("/", Opcodes.IDIV);
		opToInt.put("+", Opcodes.IADD);
		opToInt.put("-", Opcodes.ISUB);
		opToInt.put("*", Opcodes.IMUL);
		opToInt.put("function enter", Opcodes.NOP);
		opToInt.put("function exit", Opcodes.NOP);
		opToInt.put("put field", Opcodes.PUTFIELD);

		opToInt.put("variable declaration", Opcodes.ISTORE);
		opToInt.put("return", Opcodes.IRETURN);

	}
	@Override
	public InstNode deserialize(JsonElement json, Type typeOfT,
			JsonDeserializationContext context) throws JsonParseException {
		JsonObject object = json.getAsJsonObject();
		String methodKey = object.get("fromMethod").getAsString();
		int threadId = object.get("threadId").getAsInt();
		int threadMethodIdx = object.get("threadMethodIdx").getAsInt();
		int idx = object.get("idx").getAsInt();
		int linenumber = object.get("linenumber").getAsInt();
		long startTime = object.get("startTime").getAsLong();
		long updateTime = object.get("updateTime").getAsLong();
//		int opcode = object.get("op").getAsInt();
		String op_str = object.get("op").getAsString();
		int opcode;
		if(opToInt.containsKey(op_str))
			opcode = opToInt.get(op_str);
		else
		{
			throw new IllegalStateException("Unknown op: " + op_str);
		}
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
		
		InstNode inst = null;
		JsonElement calleeProbe = object.get("calleeInfo");
		boolean shouldRepOp = true;
		if (calleeProbe == null) {
			JsonElement fieldProbe = object.get("globalChildIdx");
			if (fieldProbe == null) {
				inst = this.pool.searchAndGet(methodKey, threadId, threadMethodIdx, idx, opcode, addInfo, InstPool.REGULAR);
			} else {
				inst = this.pool.searchAndGet(methodKey, threadId, threadMethodIdx, idx, opcode, addInfo, InstPool.FIELD);
				FieldNode fInst = (FieldNode)inst;
				
				JsonArray globalChild = fieldProbe.getAsJsonArray();
				for (int i = 0; i < globalChild.size(); i++) {
					fInst.addGlobalChild(globalChild.get(i).getAsString());
				}
			}
		} else {
			inst = this.pool.searchAndGet(methodKey, threadId, threadMethodIdx, idx, opcode, addInfo, InstPool.METHOD);
			MethodNode mn = (MethodNode)inst;
			
			JsonObject calleeInfo = calleeProbe.getAsJsonObject();
			TypeToken<CalleeInfo> infoType = new TypeToken<CalleeInfo>(){};
			CalleeInfo info = context.deserialize(calleeInfo, infoType.getType());
			mn.setCalleeInfo(info);
			
			JsonElement rsProbe = object.get("rs");
			if (rsProbe != null) {
				JsonObject regularState = rsProbe.getAsJsonObject();
				TypeToken<RegularState> rsType = new TypeToken<RegularState>(){};
				RegularState rs = context.deserialize(regularState, rsType.getType());
				mn.setRegularState(rs);
			}
			shouldRepOp = false;
		}
		
		inst.setInstDataParentList(instDataParentList);
		inst.setWriteDataParentList(writeDataParentList);
		inst.setControlParentList(controlParentList);
		inst.setChildFreqMap(childFreqMap);
		inst.setStartTime(startTime);
		inst.setUpdateTime(updateTime);
		inst.setLinenumber(linenumber);
		if (shouldRepOp) {
			SearchUtil.repOp(inst);
		}
		
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
		result.addProperty("startTime", inst.getStartTime());
		result.addProperty("updateTime", inst.getUpdateTime());
		result.addProperty("linenumber", inst.getLinenumber());
		result.addProperty("op", inst.getOp().getOpcode());
		result.addProperty("addInfo", inst.getAddInfo());
		
		TypeToken<ArrayList<String>> listType = new TypeToken<ArrayList<String>>(){};
		result.add("instDataParentList", context.serialize(inst.getInstDataParentList(), listType.getType()));
		result.add("writeDataParentList", context.serialize(inst.getWriteDataParentList(), listType.getType()));
		result.add("controlParentList", context.serialize(inst.getControlParentList(), listType.getType()));
		
		TypeToken<TreeMap<String, Double>> mapType = new TypeToken<TreeMap<String, Double>>(){};
		result.add("childFreqMap", context.serialize(inst.getChildFreqMap(), mapType.getType()));
		
		if (inst instanceof MethodNode) {
			MethodNode mn = (MethodNode) inst;
			TypeToken<CalleeInfo> infoType = new TypeToken<CalleeInfo>(){};
			result.add("calleeInfo", context.serialize(mn.getCalleeInfo(), infoType.getType()));
			
			//Use instFrac rather than count, because this has been summarized
			if (mn.getRegularState().instFrac > 0) {
				TypeToken<RegularState> rsType = new TypeToken<RegularState>(){};
				result.add("rs", context.serialize(mn.getRegularState(), rsType.getType()));
			}
		} else if (inst instanceof FieldNode) {
			FieldNode fn = (FieldNode) inst;
			result.add("globalChildIdx", context.serialize(fn.getGlobalChildIdx(), listType.getType()));
		}
		
		return result;
	}
}
