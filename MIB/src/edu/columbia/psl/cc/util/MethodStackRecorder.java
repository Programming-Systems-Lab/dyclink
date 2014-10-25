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

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.ExtObj;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.pojo.StaticRep;
import edu.columbia.psl.cc.premain.MIBDriver;

public class MethodStackRecorder {
		
	private static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
	private AtomicInteger curTime = new AtomicInteger();
		
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
	
	//Val: line num, write field and load local vars
	private List<ExtObj> extMethods = new ArrayList<ExtObj>();
	
	//Record which insts might be affected by input params
	private HashSet<Integer> firstReadFields = new HashSet<Integer>();
	
	private HashSet<String> stopReadFields = new HashSet<String>();
	
	//Record which insts might be affecte by field written by parent method
	private HashSet<Integer> firstReadLocalVars = new HashSet<Integer>();
	
	private HashSet<Integer> shouldRecordReadLocalVars = new HashSet<Integer>();
	
	private ExtObj returnEo = new ExtObj();
	
	private List<InstNode> path = new ArrayList<InstNode>();
	
	//public Object objOnStack = null;
	
	public String curLabel = null;
	
	private InstPool pool = new InstPool();
	
	public MethodStackRecorder(String className, String methodName, String methodDesc, boolean staticMethod) {
		this.className = className;
		this.methodName = methodName;
		this.methodDesc = methodDesc;
		this.staticMethod = staticMethod;
		
		this.methodKey = StringUtil.genKey(className, methodName, methodDesc);
		Type methodType = Type.getMethodType(this.methodDesc);
		this.methodArgSize = methodType.getArgumentTypes().length;
		if (!methodType.getReturnType().getDescriptor().equals("V")) {
			this.methodReturnSize = 1;
		}
		
		int count = 0, start = 0;
		if (!this.staticMethod) {
			//Start from 0
			this.shouldRecordReadLocalVars.add(0);
			start = 1;
		}
		
		while (count < methodArgSize) {
			this.shouldRecordReadLocalVars.add(start);
			start++;
			count++;
		}
	}
	
	private int getCurTime() {
		return this.curTime.getAndIncrement();
	}
	
	private void stopLocalVar(int localVarId) {
		this.shouldRecordReadLocalVars.remove(localVarId);
	}
	
	private void updateReadLocalVar(InstNode localVarNode) {
		int localVarId = Integer.valueOf(localVarNode.getAddInfo());
		if (this.shouldRecordReadLocalVars.contains(localVarId)) {
			this.firstReadLocalVars.add(localVarNode.getIdx());
		}
	}
	
	private void stopReadField(String field) {
		this.stopReadFields.add(field);
	}
	
	private void updateReadField(InstNode fieldNode) {
		if (this.stopReadFields.contains(fieldNode.getAddInfo()))
			return ;
		
		this.firstReadFields.add(fieldNode.getIdx());
	}
	
	private void updateExtMethods(ExtObj eo) {
		this.extMethods.add(eo);
	}
		
	private void updatePath(InstNode fullInst) {
		this.path.add(fullInst);
	}
	
	private void updateTime(InstNode fullInst) {
		int curTime = this.getCurTime();
		if (fullInst.getStartTime() < 0) {
			fullInst.setStartTime(curTime);
			fullInst.setUpdateTime(curTime);	
		} else {
			fullInst.setUpdateTime(curTime);
		}
 	}
	
	private void updateCachedMap(InstNode parent, InstNode child, int depType) {
		if (depType == MIBConfiguration.INST_DATA_DEP) {
			parent.increChild(child.getFromMethod(), child.getIdx(), MIBConfiguration.getInstance().getInstDataWeight());
			child.registerParent(parent.getFromMethod(), parent.getIdx(), depType);
		} else if (depType == MIBConfiguration.WRITE_DATA_DEP) {
			parent.increChild(child.getFromMethod(), child.getIdx(), MIBConfiguration.getInstance().getWriteDataWeight());
			child.registerParent(parent.getFromMethod(), parent.getIdx(), depType);
		} else if (depType == MIBConfiguration.CONTR_DEP) {
			parent.increChild(child.getFromMethod(), child.getIdx(), MIBConfiguration.getInstance().getControlWeight());
			child.registerParent(parent.getFromMethod(), parent.getIdx(), depType);
		}
		
		/*if (isControl) {
			parent.increChild(child.getFromMethod(), child.getIdx(), MIBConfiguration.getInstance().getControlWeight());
			child.registerParent(parent.getFromMethod(), parent.getIdx(), isControl);
			
			//p->surC, surC->p
			for (Integer i: child.getSurrogates().values()) {
				InstNode sur = this.pool.searchAndGet(child.getFromMethod(), i);
				parent.increChild(child.getFromMethod(), sur.getIdx(), MIBConfiguration.getInstance().getControlWeight());
				sur.registerParent(parent.getFromMethod(), parent.getIdx(), isControl);
			}
			
			//surP->surC
			for (Integer i: parent.getSurrogates().values()) {
				InstNode sur = this.pool.searchAndGet(parent.getFromMethod(), i);
				sur.increChild(child.getFromMethod(), child.getIdx(), MIBConfiguration.getInstance().getControlWeight());
			}
			
			for (Integer i: child.getSurrogates().values()) {
				InstNode sur = this.pool.searchAndGet(child.getFromMethod(), i);
				sur.registerParent(parent.getFromMethod(), parent.getIdx(), isControl);
			}
		} else {
			parent.increChild(child.getFromMethod(), child.getIdx(), MIBConfiguration.getInstance().getDataWeight());
			child.registerParent(parent.getFromMethod(), parent.getIdx(), isControl);
			
			for (Integer i: parent.getSurrogates().values()) {
				InstNode sur = this.pool.searchAndGet(parent.getFromMethod(), i);
				sur.increChild(child.getFromMethod(), child.getIdx(), MIBConfiguration.getInstance().getDataWeight());
			}
			
			for (Integer i: child.getSurrogates().values()) {
				InstNode sur = this.pool.searchAndGet(child.getFromMethod(), i);
				sur.registerParent(parent.getFromMethod(), parent.getIdx(), isControl);
			}
		}*/
		//System.out.println("Update map: " + this.dataDep);
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
	
	public void updateObjOnStack(Object obj) {
		InstNode latestInst = this.stackSimulator.peek();
		latestInst.setRelatedObj(obj);
	}
	
	public void handleLdc(int opcode, int instIdx, int times, String addInfo) {
		System.out.println("Handling now: " + opcode + " " + instIdx + " " + addInfo);
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, instIdx, opcode, addInfo);
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
			objId = parseObjId(this.stackSimulator.peek().getRelatedObj());
		} else if (opcode == Opcodes.PUTFIELD) {
			if (typeSort == Opcodes.LONG || typeSort == Opcodes.DOUBLE) {
				objId = parseObjId(this.stackSimulator.get(this.stackSimulator.size() - 3).getRelatedObj());
			} else {
				objId = parseObjId(this.stackSimulator.get(this.stackSimulator.size() - 2).getRelatedObj());
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
		
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, instIdx, opcode, fieldKey);
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
				
				if (this.extMethods.size() > 0) {
					this.extMethods.get(this.extMethods.size() - 1).addAffFieldInst(fullInst);
				}
				
				this.updateReadField(fullInst);
			}
		} else if (BytecodeCategory.writeFieldCategory().contains(opcat)) {
			if (opcode == Opcodes.PUTSTATIC || objId > 0) {
				this.fieldRecorder.put(fieldKey, fullInst);
				this.stopReadField(fieldKey);
			}
		}
		
		int addInput = 0, addOutput = 0;
		if (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC) {
			System.out.println("Add info: " + fieldKey);
			System.out.println("Type sort: " + typeSort + Type.DOUBLE);
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
		
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, instIdx, opcode, addInfo);
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
			fullInst = this.pool.searchAndGet(this.methodKey, instIdx, opcode, String.valueOf(localVarIdx));
		} else {
			fullInst = this.pool.searchAndGet(this.methodKey, instIdx, opcode, "");
		}
		this.updateTime(fullInst);
		
		int opcat = fullInst.getOp().getCatId();
		
		InstNode lastInst = null;
		if (!stackSimulator.isEmpty()) {
			lastInst = stackSimulator.peek();
		}
		
		if (!BytecodeCategory.dupCategory().contains(opcode)) {
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
				System.out.println("Access local var recorder: " + fullInst);
				System.out.println(parentInst);
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
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, instIdx, Opcodes.MULTIANEWARRAY, addInfo);
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
	
	private void handleUninstrumentedMethod(int opcode, int instIdx, int linenum, String owner, String name, String desc, InstNode fullInst, ExtObj eo) {
		System.out.println("Handling uninstrumented method: " + opcode + " " + instIdx + " " + owner + " " + name + " " + desc);
		this.updateTime(fullInst);
		
		this.updateControlRelation(fullInst);
		
		Type methodType = Type.getMethodType(desc);
		//+1 for object reference, if instance method
		Type[] args = methodType.getArgumentTypes();
		int argSize = 0;
		for (int i = 0; i < args.length; i++) {
			Type t = args[i];
			if (t.getDescriptor().equals("D") || t.getDescriptor().equals("L")) {
				argSize += 2;
			} else {
				argSize += 1;
			}
			eo.addLoadLocalInst(this.stackSimulator.get(this.stackSimulator.size() - argSize));
		}
		this.updateExtMethods(eo);
		
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
			if (returnType.equals("D") || returnType.equals("L")) {
				this.updateStackSimulator(2, fullInst);
			} else {
				this.updateStackSimulator(1, fullInst);
			}
		}
		this.showStackSimulator();
	}
	
	public void handleMethod(int opcode, int instIdx, int linenum, String owner, String name, String desc) {
		//String addInfo = owner + "." + name + "." + desc;
		//String addInfo = StringUtil.genKey(owner, name, desc);
		//InstNode fullInst = this.pool.searchAndGet(this.methodKey, instIdx, opcode, addInfo);
		
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
			if (BytecodeCategory.staticMethod().contains(opcode)) {
				correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(owner, name, desc, false);
			} else if (opcode == Opcodes.INVOKESPECIAL) {
				correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(owner, name, desc, true);
			} else {
				//For inovkeinterface, the bridge method created by JVM can help us locate the correct method
				Object objOnStack = this.stackSimulator.get(stackSimulator.size() - argSize - 1).getRelatedObj();
				System.out.println("Real obj on stack: " + objOnStack.getClass());
				correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(objOnStack.getClass().getName(), name, desc, false);
			}
			
			System.out.println("Method owner: " + correctClass.getName());
			String searchKey = StringUtil.genKey(correctClass.getName(), name, desc);
			System.out.println("Search key: " + searchKey);
			InstNode fullInst = this.pool.searchAndGet(this.methodKey, instIdx, opcode, searchKey);
			
			ExtObj eo = new ExtObj();
			//Don't update, because we will remove inst before leaving the method
			//this.updateControlRelation(fullInst);
			this.updatePath(fullInst);
			
			eo.setLineNumber(linenum);
			eo.setInstIdx(instIdx);
			this.updateExtMethods(eo);
			
			String filePath = MIBConfiguration.getInstance().getTemplateDir() + "/" + searchKey + ".json";
			GraphTemplate childGraph = TemplateLoader.loadTemplateFile(filePath, graphToken);
			
			//This means that the callee method is from jvm, keep the method inst in graph
			if (childGraph == null) {
				System.out.println("Null graph: " + searchKey);
				this.handleUninstrumentedMethod(opcode, instIdx, linenum, owner, name, desc, fullInst, eo);
				return ;
			}
			
			System.out.println("Child graph: " + childGraph.getMethodKey() + " " + childGraph.getInstPool().size());
			
			//Integrate two pools and update dependencies
			InstPool childPool = childGraph.getInstPool();
			GraphUtil.removeReturnInst(childPool);
			
			//Reindex child
			int baseTime = this.getCurTime();
			int reBase = GraphUtil.reindexInstPool(baseTime, childPool);
			this.curTime.set(reBase);
			
			//Search correct inst on inst, update local data dep dependency
			HashMap<Integer, InstNode> parentFromCaller = new HashMap<Integer, InstNode>();
			if (args.length > 0) {
				int startIdx = 0;
				if (!BytecodeCategory.staticMethod().contains(opcode)) {
					startIdx = 1;
				}
				int endIdx = startIdx + args.length - 1;
				
				for (int i = args.length - 1; i >= 0 ;i--) {
					Type t = args[i];
					InstNode targetNode = null;
					if (t.getDescriptor().equals("D") || t.getDescriptor().equals("L")) {
						this.safePop();
						targetNode = this.safePop();
					} else {
						targetNode = this.safePop();
					}
					parentFromCaller.put(endIdx--, targetNode);
				}
			}
			
			if (!BytecodeCategory.staticMethod().contains(opcode)) {
				//loadNode can be anyload that load an object
				InstNode loadNode = this.safePop();
				parentFromCaller.put(0, loadNode);
			}
			
			GraphUtil.dataDepFromParentToChild(parentFromCaller,
					this.pool,
					childPool, 
					childGraph.getFirstReadLocalVars(), 
					childGraph.getMethodKey());
			
			//Update control dep
			if (this.curControlInst != null) {
				GraphUtil.controlDepFromParentToChild(this.curControlInst, childPool);
			}
			
			//Update field data dep
			if (this.fieldRecorder.size() > 0) {
				GraphUtil.fieldDataDepFromParentToChild(this.fieldRecorder, 
						childPool, 
						childGraph.getFirstReadFields(), 
						childGraph.getMethodKey());
				
				for (InstNode inst: this.fieldRecorder.values()) {
					eo.addWriteFieldInst(inst);
				}
			}
			
			if (childGraph.getWriteFields().size() > 0) {
				this.fieldRecorder.putAll(childGraph.getWriteFields());
			}
			
			String returnType = methodType.getReturnType().getDescriptor();
			if (!returnType.equals("V")) {
				InstNode lastSecond = GraphUtil.lastSecondInst(childGraph.getInstPool());
				System.out.println("Check last second from child: " + lastSecond);
				if (returnType.equals("D") || returnType.equals("L")) {
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
		
		/*Type methodType = Type.getMethodType(desc);
		//+1 for object reference, if instance method
		Type[] args = methodType.getArgumentTypes();
		int argSize = 0;
		for (int i = 0; i < args.length; i++) {
			Type t = args[i];
			if (t.getDescriptor().equals("D") || t.getDescriptor().equals("L")) {
				argSize += 2;
			} else {
				argSize += 1;
			}
			eo.addLoadLocalInst(this.stackSimulator.get(this.stackSimulator.size() - argSize));
		}
		this.updateExtMethods(eo);
		
		if (!BytecodeCategory.staticMethod().contains(opcode)) {
			argSize++;
		}
		System.out.println("Arg size: " + argSize);
		String returnType = methodType.getReturnType().getDescriptor();
		for (int i = 0; i < argSize; i++) {
			InstNode tmpInst = this.safePop();
			this.updateCachedMap(tmpInst, fullInst, false);
			//this.updateInvokeMethod(methodKey, tmpInst);
		}
		
		if (!returnType.equals("V")) {
			if (returnType.equals("D") || returnType.equals("L")) {
				this.updateStackSimulator(2, fullInst);
			} else {
				this.updateStackSimulator(1, fullInst);
			}
		}
		this.showStackSimulator();*/
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
	
	public static int parseObjId(Object value) {
		if (value == null)
			return - 1;
		
		Class<?> valueClass = value.getClass();
		try {
			Field idField = valueClass.getField(MIBConfiguration.getMibId());
			int objId = idField.getInt(value);
			return objId;
		} catch (Exception ex) {
			System.out.println("Warning: object " + valueClass + " is not MIB-instrumented");
			return -1;
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
		//System.out.println("Check data dep: " + this.dataDep);
		//System.out.println("Check stack simulator: " + this.stackSimulator);
	}
	
	private void showStackSimulator() {
		System.out.println(this.stackSimulator);
	}
	
	private void updateControlRelation(InstNode fullInst) {		
		if (this.curControlInst != null) {
			//System.out.println("Check current control inst label: " + curControlInst.getAddInfo());
			//System.out.println("Check current label: " + this.curLabel);
			int cCatId = curControlInst.getOp().getCatId();
			
			InstNode lastNode = null;
			/*if (this.path.size() > 0)
				lastNode = this.path.get(this.path.size() - 1);*/
			
			if (this.stackSimulator.size() > 0)
				lastNode = this.stackSimulator.peek();
			
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
	
	public void dumpGraph(boolean isTemplate) {		
		//For serilization
		GraphTemplate gt = new GraphTemplate();
		if (this.path.size() > 1)
			this.returnEo.addLoadLocalInst(this.path.get(this.path.size() - 2));
		
		if (this.fieldRecorder.size() > 0) {
			for (InstNode inst: this.fieldRecorder.values()) {
				this.returnEo.addWriteFieldInst(inst);
			}
		}
		
		gt.setMethodKey(this.methodKey);
		gt.setMethodArgSize(this.methodArgSize);
		gt.setMethodReturnSize(this.methodReturnSize);
		gt.setStaticMethod(this.staticMethod);
		gt.setExtMethods(this.extMethods);
		gt.setFirstReadLocalVars(this.firstReadLocalVars);
		gt.setFirstReadFields(this.firstReadFields);
		gt.setWriteFields(this.fieldRecorder);
		gt.setReturnInfo(this.returnEo);
		gt.setPath(this.path);
		
		System.out.println("Instruction dependency:");
		
		int depCount = 0;
		Iterator<InstNode> instIterator = this.pool.iterator();
		while (instIterator.hasNext()) {
			InstNode curInst = instIterator.next();
			//System.out.println("Parent: " + curInst.toString());
			TreeMap<String, Double> children = curInst.getChildFreqMap();
			/*for (String c: children.navigableKeySet()) {
				String[] parsedKey = StringUtil.parseIdxKey(c);
				System.out.println("  Child: " + this.pool.searchAndGet(parsedKey[0], Integer.valueOf(parsedKey[1])) + " Freq: " + children.get(c));
			}*/
			depCount += children.size();
		}
		System.out.println("Total dependency count: " + depCount);
		
		gt.setInstPool(this.pool);
		
		TypeToken<GraphTemplate> typeToken = new TypeToken<GraphTemplate>(){};
		
		if (isTemplate) {
			GsonManager.writeJsonGeneric(gt, this.methodKey, typeToken, 0);
		} else {
			GsonManager.writeJsonGeneric(gt, this.methodKey, typeToken, 1);
		}
		GsonManager.writePath(this.methodKey, this.path);
	}

}
