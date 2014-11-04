package edu.columbia.psl.cc.util;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.datastruct.GraphGroupController;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.ExtObj;
import edu.columbia.psl.cc.pojo.GraphGroup;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.pojo.StaticRep;
import edu.columbia.psl.cc.pojo.SurrogateInst;
import edu.columbia.psl.cc.premain.MIBDriver;

public class MethodStackRecorder {
		
	private static TypeToken<GraphTemplate> GRAPH_TOKEN = new TypeToken<GraphTemplate>(){};
	
	private AtomicLong curDigit = new AtomicLong();
	
	private AtomicLong curTime = new AtomicLong();
		
	private String className;
	
	private String methodName;
	
	private String methodDesc;
	
	private String methodKey;
	
	private boolean staticMethod;
	
	private int methodArgSize = 0;
	
	private int methodReturnSize = 0;
		
	private Stack<InstNode> stackSimulator = new Stack<InstNode>();
	
	private InstNode curControlInst = null;
	
	//Key: local var idx, Val: inst node
	private Map<Integer, InstNode> localVarRecorder = new HashMap<Integer, InstNode>();
	
	//Key: field name, Val: inst node
	private Map<String, InstNode> fieldRecorder = new HashMap<String, InstNode>();
	
	//Record which insts might be affected by input params
	//private HashSet<InstNode> firstReadFields = new HashSet<InstNode>();
	private HashMap<String, HashSet<InstNode>> firstReadFields = new HashMap<String, HashSet<InstNode>>();
	
	//Record which insts might be affecte by field written by parent method
	private HashSet<InstNode> firstReadLocalVars = new HashSet<InstNode>();
	
	private HashSet<Integer> shouldRecordReadLocalVars = new HashSet<Integer>();
	
	private List<InstNode> path = new ArrayList<InstNode>();
	
	public String curLabel = null;
	
	private InstPool pool = new InstPool();
	
	private InstNode lastBeforeReturn;
	
	private long threadId = -1;
	
	private int threadMethodId = -1;
	
	private int objId = 0;
	
	private long maxTime = -1;
	
	private HashMap<String, GraphGroup> calleeCache = new HashMap<String, GraphGroup>();
	
	public MethodStackRecorder(String className, 
			String methodName, 
			String methodDesc, 
			Object obj) {
		this.className = className;
		this.methodName = methodName;
		this.methodDesc = methodDesc;
				
		this.methodKey = StringUtil.genKey(className, methodName, methodDesc);
		Type methodType = Type.getMethodType(this.methodDesc);
		this.methodArgSize = methodType.getArgumentTypes().length;
		if (!methodType.getReturnType().getDescriptor().equals("V")) {
			this.methodReturnSize = 1;
		}
		
		this.threadId = Thread.currentThread().getId();
		this.threadMethodId = ObjectIdAllocater.getThreadMethodIndex(className, 
				methodName, 
				methodDesc, 
				this.threadId);
		
		if (obj == null) {
			this.staticMethod = true;
		} else {
			this.objId = ObjectIdAllocater.parseObjId(obj);
		}
		
		int start = 0;
		if (!this.staticMethod) {
			//Start from 0
			this.shouldRecordReadLocalVars.add(0);
			start = 1;
		}
		
		for (Type t: methodType.getArgumentTypes()) {
			this.shouldRecordReadLocalVars.add(start);
			if (t.getDescriptor().equals("D") || t.getDescriptor().equals("J")) {
				start += 2;
			} else {
				start += 1;
			}
		}
	}
	
	private long[] getCurTime() {
		long uni = this.curTime.getAndIncrement();
		long ten = this.curDigit.get();
		if (uni == Long.MAX_VALUE) {
			this.curTime.set(0);
			this.curDigit.getAndIncrement();
		}
		long[] ret = {uni, ten};
		return ret;
	}
	
	private void stopLocalVar(int localVarId) {
		this.shouldRecordReadLocalVars.remove(localVarId);
	}
	
	private void updateReadLocalVar(InstNode localVarNode) {
		int localVarId = Integer.valueOf(localVarNode.getAddInfo());
		if (this.shouldRecordReadLocalVars.contains(localVarId)) {
			this.firstReadLocalVars.add(localVarNode);
		}
	}
	
	private void updateReadField(InstNode fieldNode) {
		String key = fieldNode.getAddInfo();
		if (this.firstReadFields.keySet().contains(key)) {
			this.firstReadFields.get(key).add(fieldNode);
		} else {
			HashSet<InstNode> nodeSet = new HashSet<InstNode>();
			nodeSet.add(fieldNode);
			this.firstReadFields.put(key, nodeSet);
		}
		//this.firstReadFields.add(fieldNode);
	}
	
	/*private void removeReadFields(String field) {
		Iterator<InstNode> frIterator = this.firstReadFields.iterator();
		while (frIterator.hasNext()) {
			InstNode inst = frIterator.next();
			
			if (inst.getAddInfo().equals(field))
				frIterator.remove();
		}
	}*/
		
	private void updatePath(InstNode fullInst) {
		this.path.add(fullInst);
	}
	
	private void updateTime(InstNode fullInst) {
		long[] curTime = this.getCurTime();
		if (fullInst.getStartTime() < 0) {
			fullInst.setStartDigit(curTime[1]);
			fullInst.setStartTime(curTime[0]);
			fullInst.setUpdateDigit(curTime[1]);
			fullInst.setUpdateTime(curTime[0]);
		} else {
			fullInst.setUpdateDigit(curTime[1]);
			fullInst.setUpdateTime(curTime[0]);
		}
		
		/*if (curTime > this.maxTime) {
			maxTime = curTime;
		}*/
 	}
	
	private void updateCachedMap(InstNode parent, InstNode child, int depType) {
		if (depType == MIBConfiguration.INST_DATA_DEP) {
			parent.increChild(child.getFromMethod(), child.getThreadId(), child.getThreadMethodIdx(), child.getIdx(), MIBConfiguration.getInstance().getInstDataWeight());
			child.registerParent(parent.getFromMethod(), parent.getThreadId(), child.getThreadMethodIdx(), parent.getIdx(), depType);
		} else if (depType == MIBConfiguration.WRITE_DATA_DEP) {
			//write data dep only needs to be recorded once
			String childIdxKey = StringUtil.genIdxKey(child.getFromMethod(), child.getThreadId(), child.getThreadMethodIdx(), child.getIdx());
			if (parent.getChildFreqMap().containsKey(childIdxKey))
				return ;
			
			parent.increChild(child.getFromMethod(), child.getThreadId(), child.getThreadMethodIdx(), child.getIdx(), MIBConfiguration.getInstance().getWriteDataWeight());
			child.registerParent(parent.getFromMethod(), parent.getThreadId(), parent.getThreadMethodIdx(), parent.getIdx(), depType);
		} else if (depType == MIBConfiguration.CONTR_DEP) {
			parent.increChild(child.getFromMethod(), child.getThreadId(), child.getThreadMethodIdx(), child.getIdx(), MIBConfiguration.getInstance().getControlWeight());
			child.registerParent(parent.getFromMethod(), parent.getThreadId(), parent.getThreadMethodIdx(), parent.getIdx(), depType);
		}
		
		if (child.toString().equals("org.ejml.alg.dense.decomposition.svd.SvdImplicitQrDecompose_D64:makeSingularPositive:():V 1 0 55 103 dsub")) {
			System.out.println("Get the target: " + child);
			System.out.println("Its parent: " + parent);
		}
	}
	
	private synchronized InstNode safePop() {
		if (this.stackSimulator.size() > 0) {
			return this.stackSimulator.pop();
		}
		return null;
	}
	
	private synchronized void updateControlInst(InstNode fullInst) {
		this.curControlInst = fullInst;
		//this.curControlInsts.add(fullInst);
	}
	
	public void updateObjOnStack(Object obj, int traceBack) {
		int idx = this.stackSimulator.size() - 1 - traceBack;
		InstNode latestInst = this.stackSimulator.get(idx);
		//InstNode latestInst = this.stackSimulator.peek();
		latestInst.setRelatedObj(obj);
	}
	
	public void handleLdc(int opcode, int instIdx, int times, String addInfo) {
		System.out.println("Handling now: " + opcode + " " + instIdx + " " + addInfo);
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, this.threadId, this.threadMethodId, instIdx, opcode, addInfo);
		this.updateTime(fullInst);
		
		this.updateControlRelation(fullInst);
		this.updatePath(fullInst);
		
		this.updateStackSimulator(times, fullInst);
		this.showStackSimulator();
	}
	
	public void handleField(int opcode, int instIdx, String owner, String name, String desc) {
		System.out.println("Handling now: " + opcode + " " + instIdx + " " + owner + " " + name + " " + desc);
		OpcodeObj oo = BytecodeCategory.getOpcodeObj(opcode);
		int opcat = oo.getCatId();
		int typeSort = Type.getType(desc).getSort();
		
		//int objId = parseObjId(this.objOnStack);
		int objId = 0;
		
		if (opcode == Opcodes.GETFIELD) {
			objId = ObjectIdAllocater.parseObjId(this.stackSimulator.peek().getRelatedObj());
		} else if (opcode == Opcodes.PUTFIELD) {
			if (typeSort == Type.LONG || typeSort == Type.DOUBLE) {
				objId = ObjectIdAllocater.parseObjId(this.stackSimulator.get(this.stackSimulator.size() - 3).getRelatedObj());
			} else {
				objId = ObjectIdAllocater.parseObjId(this.stackSimulator.get(this.stackSimulator.size() - 2).getRelatedObj());
			}
		}
		
		//Search the real owner of the field
		Class<?> targetClass = ClassInfoCollector.retrieveCorrectClassByField(owner, name);
		System.out.println("Target class: " + targetClass + " " + " " + desc);
		System.out.println("Object id: " + objId);
		String fieldKey = targetClass.getName() + "." + name + "." + desc;
		
		if (objId > 0) {
			fieldKey += objId;
		}
		
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, this.threadId, this.threadMethodId, instIdx, opcode, fieldKey);
		this.updateTime(fullInst);
		this.updateControlRelation(fullInst);
		this.updatePath(fullInst);
		
		if (BytecodeCategory.readFieldCategory().contains(opcat)) {
			//Add info for field: owner + name + desc + objId
			//Only record static or the instrumented object
			if (opcode == Opcodes.GETSTATIC || objId > 0) {
				InstNode parent = this.fieldRecorder.get(fieldKey);
				if (parent != null)
					this.updateCachedMap(parent, fullInst, MIBConfiguration.WRITE_DATA_DEP);
				else
					this.updateReadField(fullInst);
			}
		} else if (BytecodeCategory.writeFieldCategory().contains(opcat)) {
			if (opcode == Opcodes.PUTSTATIC || objId > 0) {
				this.fieldRecorder.put(fieldKey, fullInst);
				//this.removeReadFields(fieldKey);
			}
		}
		
		int addInput = 0, addOutput = 0;
		if (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC) {
			if (typeSort == Type.DOUBLE || typeSort == Type.LONG) {
				addInput++;
			}
		} else if (opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC) {
			if (typeSort == Type.DOUBLE || typeSort == Type.LONG) {
				addOutput++;
			}
		}
		
		int inputSize = oo.getInList().size() + addInput;
		if (inputSize > 0) {
			for (int i = 0; i < inputSize; i++) {
				//Should not return null here
				InstNode tmpInst = this.safePop();
				this.updateCachedMap(tmpInst, fullInst, MIBConfiguration.INST_DATA_DEP);
			}
		}
		this.updateStackSimulator(fullInst, addOutput);
		this.showStackSimulator();
	}
	
	public void handleOpcode(int opcode, int instIdx, String addInfo) {
		System.out.println("Handling now: " + opcode + " " + instIdx + " " + addInfo);
		OpcodeObj oo = BytecodeCategory.getOpcodeObj(opcode);
		int opcat = oo.getCatId();
		
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, this.threadId, this.threadMethodId, instIdx, opcode, addInfo);
		this.updateTime(fullInst);
		this.updateControlRelation(fullInst);
		this.updatePath(fullInst);
		
		if (BytecodeCategory.controlCategory().contains(opcat) 
				|| opcode == Opcodes.TABLESWITCH 
				|| opcode == Opcodes.LOOKUPSWITCH) {
			this.updateControlInst(fullInst);
		}
		
		int inputSize = oo.getInList().size();
		if (inputSize > 0) {
			for (int i = 0; i < inputSize; i++) {
				//Should not return null here
				InstNode tmpInst = this.safePop();
				this.updateCachedMap(tmpInst, fullInst, MIBConfiguration.INST_DATA_DEP);
			}
		}
		this.updateStackSimulator(fullInst, 0);
		this.showStackSimulator();
	}
	
	/**
	 * localVarIdx is not necessarily a local var
	 * @param opcode
	 * @param localVarIdx
	 */
	public void handleOpcode(int opcode, int instIdx, int localVarIdx) {
		System.out.println("Handling now: " + opcode + " " + localVarIdx);
		
		InstNode fullInst = null;
		if (localVarIdx >= 0) {
			fullInst = this.pool.searchAndGet(this.methodKey, this.threadId, this.threadMethodId, instIdx, opcode, String.valueOf(localVarIdx));
		} else {
			fullInst = this.pool.searchAndGet(this.methodKey, this.threadId, this.threadMethodId, instIdx, opcode, "");
		}
		this.updateTime(fullInst);
		
		int opcat = fullInst.getOp().getCatId();
		
		InstNode lastInst = null;
		if (!stackSimulator.isEmpty()) {
			lastInst = stackSimulator.peek();
		}
		
		if (!BytecodeCategory.dupCategory().contains(opcat)) {
			//Dup inst will be replaced later. No need to add any dep
			this.updateControlRelation(fullInst);
		}
		this.updatePath(fullInst);
		
		System.out.println("Check lastInst: " + lastInst);
		//The store instruction will be the sink. The inst on the stack will be source
		boolean hasUpdate = false;
		if (BytecodeCategory.writeCategory().contains(opcat)) {
			if (lastInst != null) {
				if (localVarIdx >= 0) {
					this.localVarRecorder.put(localVarIdx, fullInst);
				}
				
				this.updateCachedMap(lastInst, fullInst, MIBConfiguration.INST_DATA_DEP);
				for (int i = 0; i < fullInst.getOp().getInList().size(); i++)
					this.safePop();
			}
			this.stopLocalVar(localVarIdx);
		} else if (opcode == Opcodes.IINC) {
			InstNode parentInst = this.localVarRecorder.get(localVarIdx);
			if (parentInst != null)
				this.updateCachedMap(parentInst, fullInst, MIBConfiguration.WRITE_DATA_DEP);
			
			this.localVarRecorder.put(localVarIdx, fullInst);
			this.updateReadLocalVar(fullInst);
			this.stopLocalVar(localVarIdx);
		} else if (BytecodeCategory.readCategory().contains(opcat)) {
			//Search local var recorder;
			InstNode parentInst = this.localVarRecorder.get(localVarIdx);
			if (parentInst != null) {
				this.updateCachedMap(parentInst, fullInst, MIBConfiguration.WRITE_DATA_DEP);
			}
			
			this.updateReadLocalVar(fullInst);
		} else if (BytecodeCategory.dupCategory().contains(opcat)) {
			this.handleDup(opcode);
			//dup should not have any dep, no need to parentRemove
			this.pool.remove(fullInst);
			hasUpdate = true;
		} else {
			if (BytecodeCategory.controlCategory().contains(opcat)) {
				this.updateControlInst(fullInst);
			}
			
			int inputSize = fullInst.getOp().getInList().size();
			InstNode lastTmp = null;
			if (inputSize > 0) {
				for (int i = 0; i < inputSize; i++) {
					//Should not return null here
					InstNode tmpInst = this.safePop();
					if (!tmpInst.equals(lastTmp))
						this.updateCachedMap(tmpInst, fullInst, MIBConfiguration.INST_DATA_DEP);
					
					lastTmp = tmpInst;
				}
			}
		}
		
		if (!hasUpdate) 
			this.updateStackSimulator(fullInst, 0);
		this.showStackSimulator();
	}
	
	public void handleMultiNewArray(String desc, int dim, int instIdx) {
		System.out.println("Handling now: " + desc + " " + dim + " " + instIdx);
		String addInfo = desc + " " + dim;
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, this.threadId, this.threadMethodId, instIdx, Opcodes.MULTIANEWARRAY, addInfo);
		this.updateTime(fullInst);
		
		this.updateControlRelation(fullInst);
		this.updatePath(fullInst);
		
		for (int i = 0; i < dim; i++) {
			InstNode tmpInst = this.safePop();
			this.updateCachedMap(tmpInst, fullInst, MIBConfiguration.INST_DATA_DEP);
		}
		this.updateStackSimulator(fullInst, 0);
		this.showStackSimulator();
	}
	
	private void handleUninstrumentedMethod(int opcode, int instIdx, int linenum, String owner, String name, String desc, InstNode fullInst) {
		System.out.println("Handling uninstrumented/undersize method: " + opcode + " " + instIdx + " " + owner + " " + name + " " + desc);
		this.updateTime(fullInst);
		
		this.updateControlRelation(fullInst);
		
		Type methodType = Type.getMethodType(desc);
		//+1 for object reference, if instance method
		Type[] args = methodType.getArgumentTypes();
		int argSize = 0;
		for (int i = 0; i < args.length; i++) {
			Type t = args[i];
			if (t.getDescriptor().equals("D") || t.getDescriptor().equals("J")) {
				argSize += 2;
			} else {
				argSize += 1;
			}
		}
		
		if (!BytecodeCategory.staticMethod().contains(opcode)) {
			argSize++;
		}
		System.out.println("Arg size: " + argSize);
		String returnType = methodType.getReturnType().getDescriptor();
		for (int i = 0; i < argSize; i++) {
			InstNode tmpInst = this.safePop();
			this.updateCachedMap(tmpInst, fullInst, MIBConfiguration.INST_DATA_DEP);
			//this.updateInvokeMethod(methodKey, tmpInst);
		}
		
		if (!returnType.equals("V")) {
			if (returnType.equals("D") || returnType.equals("J")) {
				this.updateStackSimulator(2, fullInst);
			} else {
				this.updateStackSimulator(1, fullInst);
			}
		}
		this.showStackSimulator();
	}
	
	public void handleMethod(int opcode, int instIdx, int linenum, String owner, String name, String desc) {
		System.out.println("Handling now: " + opcode + " " + instIdx + " " + owner + " " + name + " " + desc);
		try {
			Type methodType = Type.getMethodType(desc);
			Type[] args = methodType.getArgumentTypes();
			int argSize = 0;
			for (int i = 0; i < args.length; i++) {
				if (args[i].getSort() == Type.DOUBLE || args[i].getSort() == Type.LONG) {
					argSize += 2;
				} else {
					argSize++;
				}
			}
			System.out.println("Arg size: " + argSize);
			
			//Load the correct graph
			Class<?> correctClass = null;
			int objId = -1;
			if (BytecodeCategory.staticMethod().contains(opcode)) {
				correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(owner, name, desc, false);
				objId = 0;
			} else {
				InstNode relatedInst = this.stackSimulator.get(stackSimulator.size() - argSize - 1);
				Object objOnStack = relatedInst.getRelatedObj();
				//methodId = ObjectIdAllocater.parseObjId(objOnStack);
				objId = ObjectIdAllocater.parseObjId(objOnStack);
				
				if (opcode == Opcodes.INVOKESPECIAL) {
					correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(owner, name, desc, true);
				} else {
					correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(objOnStack.getClass().getName(), name, desc, false);
				}
			}
			
			System.out.println("Method owner: " + correctClass.getName());
			String methodKey = StringUtil.genKey(correctClass.getName(), name, desc);
			String searchKey = StringUtil.genKeyWithId(methodKey, String.valueOf(this.threadId));
			String searchKeyWithObjId = StringUtil.genKeyWithObjId(searchKey, objId);
			//System.out.println("Search key and obj id: " + searchKey + " " + this.objId);
			InstNode fullInst = this.pool.searchAndGet(this.methodKey, this.threadId, this.threadMethodId, instIdx, opcode, searchKey);
			
			//Don't update, because we will remove inst before leaving the method
			//this.updateControlRelation(fullInst);
			this.updatePath(fullInst);
			
			String filePath = "";
			if (MIBConfiguration.getInstance().isTemplateMode()) {
				filePath = MIBConfiguration.getInstance().getTemplateDir() + "/" + searchKey + ".json";
			} else {
				filePath = MIBConfiguration.getInstance().getTestDir() + "/" + searchKey + ".json";
			}
			GraphTemplate childGraph = TemplateLoader.loadTemplateFile(filePath, GRAPH_TOKEN);
			
			//This means that the callee method is from jvm, keep the method inst in graph
			boolean removeReturn = true;
			if (childGraph == null) {
				System.out.println("Null graph: " + searchKey);
				this.handleUninstrumentedMethod(opcode, instIdx, linenum, owner, name, desc, fullInst);
				return ;
			} else if (childGraph.getInstPool().size() < MIBConfiguration.getInstance().getInstThreshold()){
				System.out.println("Graph size is too small: " + childGraph.getInstPool().size());
				
				//Change the inst idx for preventing this inst will be removed in the future
				int oldInstIdx = fullInst.getIdx();
				int newInstIdx = (1 + oldInstIdx) * MIBConfiguration.getInstance().getIdxExpandFactor();
				this.pool.remove(fullInst);
				fullInst = this.pool.searchAndGet(this.methodKey, this.threadId, this.threadMethodId, newInstIdx, opcode, searchKey);
				
				//Only update field data deps
				if (this.fieldRecorder.size() > 0) {
					GraphUtil.fieldDataDepFromParentInstToChildGraph(this.fieldRecorder, fullInst, childGraph);
				}
				
				if (childGraph.getFirstReadFields().size() > 0) {
					//Change all inst to method inst
					//this.firstReadFields.addAll(childGraph.getFirstReadFields());
					for (String key: childGraph.getFirstReadFields().keySet()) {
						if (this.firstReadFields.containsKey(key)) {
							this.firstReadFields.get(key).add(fullInst);
						} else {
							HashSet<InstNode> uniSet = new HashSet<InstNode>();
							uniSet.add(fullInst);
							this.firstReadFields.put(key, uniSet);
						}
					}
				}
				
				if (childGraph.getWriteFields().size() > 0) {
					//this.fieldRecorder.putAll(childGraph.getWriteFields());
					for (String key: childGraph.getWriteFields().keySet()) {
						this.fieldRecorder.put(key, fullInst);
					}
				}
				this.handleUninstrumentedMethod(opcode, instIdx, linenum, owner, name, desc, fullInst);
				return ;
			} else if (this.calleeCache.containsKey(searchKeyWithObjId)) {
				GraphGroup gGroup = this.calleeCache.get(searchKeyWithObjId);
				//GraphGroup gGroup = GraphGroupController.getGraphGroup(searchKeyWithObjId);
				
				//Check if there is similar graph
				GraphTemplate rep = gGroup.getGraph(childGraph);
				
				if (rep != null) {
					System.out.println("Find similar graph in cache: " + searchKeyWithObjId);
					System.out.println("nodeNum depNum: " + childGraph.getInstPool().size() + " " + childGraph.getDepNum());
					System.out.println("All group keys now: " + gGroup.keySet());
					
					//Guess that this graph is the same
					childGraph = rep;
					removeReturn = false;
				} else {
					System.out.println("Find no similar graph in cache: " + searchKeyWithObjId);
					System.out.println("Existing graph group key: " + gGroup.keySet());
					System.out.println("Current graph key: " + GraphGroup.groupKey(childGraph));
					gGroup.addGraph(childGraph);
				}
			} else {
				System.out.println("Caller: " + this.methodKey);
				System.out.println("Create new graph group for: " + searchKey);
				GraphGroup gGroup = new GraphGroup();
				gGroup.addGraph(childGraph);
				this.calleeCache.put(searchKeyWithObjId, gGroup);
				//GraphGroupController.insertNewGraphGroup(searchKeyWithObjId, gGroup);
			}
			
			System.out.println("Child graph analysis: " + childGraph.getMethodKey() + " " + childGraph.getThreadId() + " " + childGraph.getThreadMethodId());
			System.out.println("Child graph size: " + childGraph.getInstPool().size());
						
			InstPool childPool = childGraph.getInstPool();
			if (removeReturn)
				GraphUtil.removeReturnInst(childPool);
			
			//Reindex child
			long[] baseTime = this.getCurTime();
			long[] reBase = GraphUtil.reindexInstPool(baseTime, childPool);
			this.curDigit.set(reBase[1]);
			this.curTime.set(reBase[0]);
			
			//GraphUtil.synchronizeInstPools(this.pool, childPool);
			
			//Search correct inst, update local data dep dependency
			HashMap<Integer, InstNode> parentFromCaller = new HashMap<Integer, InstNode>();
			if (args.length > 0) {
				int startIdx = 0;
				if (!BytecodeCategory.staticMethod().contains(opcode)) {
					startIdx = 1;
				}
				//int endIdx = startIdx + args.length - 1;
				
				int endIdx = startIdx;
				for (int i = args.length - 1; i >= 0; i--) {
					Type t = args[i];
					if (t.getDescriptor().equals("D") || t.getDescriptor().equals("J")) {
						endIdx += 2;
					} else {
						endIdx += 1;
					}
				}
				endIdx--;
				System.out.println("start end: " + startIdx + " " + endIdx);
				
				for (int i = args.length - 1; i >= 0 ;i--) {
					Type t = args[i];
					InstNode targetNode = null;
					if (t.getDescriptor().equals("D") || t.getDescriptor().equals("J")) {
						this.safePop();
						targetNode = this.safePop();
						
						endIdx -=1;
						parentFromCaller.put(endIdx, targetNode);
						endIdx -= 1;
					} else {
						targetNode = this.safePop();
						parentFromCaller.put(endIdx, targetNode);
						
						endIdx -= 1;
					}
					//parentFromCaller.put(endIdx--, targetNode);
				}
			}
			
			if (!BytecodeCategory.staticMethod().contains(opcode)) {
				//loadNode can be anyload that load an object
				InstNode loadNode = this.safePop();
				parentFromCaller.put(0, loadNode);
			}
			
			System.out.println("Check parent map: " + parentFromCaller);
			
			GraphUtil.dataDepFromParentToChild(parentFromCaller, this.pool, childGraph);
			
			//Update control dep
			if (this.curControlInst != null) {
				GraphUtil.controlDepFromParentToChild(this.curControlInst, childPool);
			}
			
			//Update field data dep
			if (this.fieldRecorder.size() > 0) {
				GraphUtil.fieldDataDepFromParentToChild(this.fieldRecorder, childGraph);
				//this.firstReadFields.addAll(childGraph.getFirstReadFields());
				for (String key: childGraph.getFirstReadFields().keySet()) {
					if (this.firstReadFields.containsKey(key)) {
						this.firstReadFields.get(key).addAll(childGraph.getFirstReadFields().get(key));
					} else {
						this.firstReadFields.put(key, childGraph.getFirstReadFields().get(key));
					}
				}
			}
			
			if (childGraph.getWriteFields().size() > 0) {
				this.fieldRecorder.putAll(childGraph.getWriteFields());
			}
			
			String returnType = methodType.getReturnType().getDescriptor();
			if (!returnType.equals("V")) {
				//InstNode lastSecond = GraphUtil.lastSecondInst(childGraph.getInstPool());
				InstNode lastSecond = childGraph.getLastBeforeReturn();
				if (returnType.equals("D") || returnType.equals("J")) {
					if (lastSecond != null)
						this.updateStackSimulator(2, lastSecond);
				} else {
					this.updateStackSimulator(1, lastSecond);
				}
			}
			this.showStackSimulator();
			this.pool.remove(fullInst);
			GraphUtil.unionInstPools(this.pool, childPool);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void handleDup(int opcode) {
		InstNode dupInst = null;
		InstNode dupInst2 = null;
		Stack<InstNode> stackBuf;
		switch (opcode) {
			case 89:
				dupInst = this.stackSimulator.peek();
				this.stackSimulator.push(dupInst);
				break ;
			case 90:
				dupInst = this.stackSimulator.peek();
				stackBuf = new Stack<InstNode>();
				for (int i = 0; i < 2; i++) {
					stackBuf.push(this.safePop());
				}
				
				this.stackSimulator.push(dupInst);
				while(!stackBuf.isEmpty()) {
					this.stackSimulator.push(stackBuf.pop());
				}
				break ;
			case 91:
				dupInst = this.stackSimulator.peek();
				stackBuf = new Stack<InstNode>();
				for (int i = 0; i < 3; i++) {
					stackBuf.push(this.safePop());
				}
				
				this.stackSimulator.push(dupInst);
				//Should only push three times
				while (!stackBuf.isEmpty()) {
					this.stackSimulator.push(stackBuf.pop());
				}
				break ;
			case 92:
				dupInst = this.stackSimulator.get(this.stackSimulator.size() - 1);
	 			dupInst2 = this.stackSimulator.get(this.stackSimulator.size() - 2);
	 			
	 			this.stackSimulator.push(dupInst2);
	 			this.stackSimulator.push(dupInst);
	 			break ;
			case 93:
				dupInst = this.stackSimulator.get(this.stackSimulator.size() - 1);
	 			dupInst2 = this.stackSimulator.get(this.stackSimulator.size() - 2);
	 			stackBuf = new Stack<InstNode>();
	 			for (int i = 0; i < 3; i++) {
	 				stackBuf.push(this.safePop());
	 			}
	 			
	 			this.stackSimulator.push(dupInst2);
	 			this.stackSimulator.push(dupInst);
	 			while (!stackBuf.isEmpty()) {
	 				this.stackSimulator.push(stackBuf.pop());
	 			}
	 			break ;
			case 94:
				dupInst = this.stackSimulator.get(this.stackSimulator.size() - 1);
	 			dupInst2 = this.stackSimulator.get(this.stackSimulator.size() - 2);
	 			stackBuf = new Stack<InstNode>();
	 			for (int i =0 ; i < 4; i++) {
	 				stackBuf.push(this.safePop());
	 			}
	 			
	 			this.stackSimulator.push(dupInst2);
	 			this.stackSimulator.push(dupInst);
	 			while (!stackBuf.isEmpty()) {
	 				this.stackSimulator.push(stackBuf.pop());
	 			}
	 			break ;
			case 95:
				dupInst = this.stackSimulator.get(this.stackSimulator.size() - 1);
				dupInst2 = this.stackSimulator.get(this.stackSimulator.size() - 2);
				this.stackSimulator.push(dupInst);
				this.stackSimulator.push(dupInst2);
				break ;
		}
	}
	
	private void updateStackSimulator(InstNode fullInst, int addOutput) {
		int outputSize = fullInst.getOp().getOutList().size() + addOutput;
		this.updateStackSimulator(outputSize, fullInst);
	}
	
	private void updateStackSimulator(int times, InstNode fullInst) {
		System.out.println("Stack push: " + fullInst + " " + times);
		for (int i = 0; i < times; i++) {
			this.stackSimulator.push(fullInst);
		}
		
		if (!BytecodeCategory.returnOps().contains(fullInst.getOp().getOpcode())) {
			this.lastBeforeReturn = fullInst;
		}
	}
	
	private void showStackSimulator() {
		System.out.println(this.stackSimulator);
	}
	
	private void updateControlRelation(InstNode fullInst) {		
		if (this.curControlInst != null) {
			int cCatId = curControlInst.getOp().getCatId();
			
			//Get the last second, because the current node is in the pool
			InstNode lastNode = null;
			if (this.path.size() > 0)
				lastNode = this.path.get(this.path.size() - 1);
			
			if (BytecodeCategory.controlCategory().contains(cCatId)) {
				if (lastNode != null && lastNode.equals(this.curControlInst)) {
					if (!this.curControlInst.getAddInfo().equals(this.curLabel)) {
						this.curControlInst = null;
						return ;
					}
				}
			} else {
				//TableSwitch and LookupSwitch
				String[] allLabels = curControlInst.getAddInfo().split(",");
				boolean found = false;
				for (String l: allLabels) {
					if (l.equals(this.curLabel)) {
						found = true;
						break;
					}
				}
				
				if (!found) {
					this.curControlInst = null;
					return ;
				}
			}
			
			if (!BytecodeCategory.dupCategory().contains(fullInst.getOp().getCatId()))
				this.updateCachedMap(this.curControlInst, fullInst, MIBConfiguration.CONTR_DEP);
		}
	}
	
	public void dumpGraph() {		
		//For serilization
		GraphTemplate gt = new GraphTemplate();
		
		gt.setMethodKey(this.methodKey);
		gt.setThreadId(this.threadId);
		gt.setThreadMethodId(this.threadMethodId);
		gt.setThreadId(Thread.currentThread().getId());
		gt.setObjId(this.objId);
		gt.setMethodArgSize(this.methodArgSize);
		gt.setMethodReturnSize(this.methodReturnSize);
		gt.setStaticMethod(this.staticMethod);
		//gt.setMaxTime(this.maxTime);
		gt.setFirstReadLocalVars(this.firstReadLocalVars);
		gt.setFirstReadFields(this.firstReadFields);
		gt.setWriteFields(this.fieldRecorder);
		gt.setLastBeforeReturn(this.lastBeforeReturn);
		//gt.setPath(this.path);
		
		System.out.println("Instruction dependency:");
		int depCount = 0;
		Iterator<InstNode> instIterator = this.pool.iterator();
		while (instIterator.hasNext()) {
			InstNode curInst = instIterator.next();
			TreeMap<String, Double> children = curInst.getChildFreqMap();
			depCount += children.size();
		}
		System.out.println("Total dependency count: " + depCount);
		
		gt.setInstPool(this.pool);
		gt.setDepNum(depCount);
		
		TypeToken<GraphTemplate> typeToken = new TypeToken<GraphTemplate>(){};
		//String dumpKey = StringUtil.genKeyWithMethodId(this.methodKey, this.id);
		String dumpKey = StringUtil.genKeyWithId(this.methodKey, String.valueOf(this.threadId));
		if (MIBConfiguration.getInstance().isTemplateMode()) {
			GsonManager.cacheGraph(dumpKey, 0);
			GsonManager.writeJsonGeneric(gt, dumpKey, typeToken, 0);
		} else {
			GsonManager.cacheGraph(dumpKey, 1);
			GsonManager.writeJsonGeneric(gt, dumpKey, typeToken, 1);
		}
		GsonManager.writePath(dumpKey, this.path);
		
		//Debuggin, check graph group
		System.out.println("Graph groups:");
		for (String searchKey: calleeCache.keySet()) {
			System.out.println("Group name: " + searchKey);
			GraphGroup gGroup = calleeCache.get(searchKey);
			System.out.println(gGroup.keySet());
		}
	}

}
