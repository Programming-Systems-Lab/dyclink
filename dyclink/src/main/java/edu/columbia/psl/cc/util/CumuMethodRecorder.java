package edu.columbia.psl.cc.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.columbia.psl.cc.abs.AbstractRecorder;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.ClassMethodInfo;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.MethodNode;
import edu.columbia.psl.cc.pojo.OpcodeObj;

public class CumuMethodRecorder extends AbstractRecorder {
	
	private static Logger logger = LogManager.getLogger(CumuMethodRecorder.class);
	
	private static long METHOD_COUNT = 0;
	
	private static Object COUNT_LOCK = new Object();
			
	private String className;
	
	private String methodName;
	
	private String methodDesc;
	
	private String methodKey;
	
	private String shortMethodKey;
	
	private boolean isStatic;
	
	private boolean isSynthetic;
	
	private int methodArgSize = 0;
	
	private int methodReturnSize = 0;
		
	private Stack<InstNode> stackSimulator = new Stack<InstNode>();
	
	private InstNode lastInst;
		
	//Key: local var idx, Val: inst node
	private Map<Integer, InstNode> localVarRecorder = new HashMap<Integer, InstNode>();
					
	//Record which insts might be affecte by field written by parent method
	private HashSet<String> firstReadLocalVars = new HashSet<String>();
	
	private HashSet<Integer> shouldRecordReadLocalVars = new HashSet<Integer>();
		
	protected String curLabel = null;
	
	public int linenumber = 0;
		
	private InstNode beforeReturn;
	
	private int threadId = -1;
	
	private int threadMethodId = -1;
	
	private boolean registered = true;
	
	public int objId = 0;
	
	private boolean overTime = false;
	
	public boolean initConstructor = true;
	
	private HashMap<Integer, InstNode> callerIdxMap = null;
	
	String currentCallee = null;
	
	public CumuMethodRecorder(String className, 
			String methodName, 
			String methodDesc, 
			int access, 
			int objId) {
		
		if (TimeController.isOverTime()) {
			this.overTime = true;
			return ;
		}
		
		synchronized(COUNT_LOCK) {
			METHOD_COUNT++;
			if (METHOD_COUNT % 50000 == 0) {
				logger.info("# of method calls: " + METHOD_COUNT);
			}
		}
		
		this.className = className;
		this.methodName = methodName;
		this.methodDesc = methodDesc;
				
		this.methodKey = StringUtil.genKey(className, methodName, methodDesc);
		//System.out.println("Current method: " + this.methodKey);
		this.shortMethodKey = CumuGraphRecorder.getGlobalName(this.methodKey);		
		Type methodType = Type.getMethodType(this.methodDesc);
		this.methodArgSize = methodType.getArgumentTypes().length;
		if (methodType.getReturnType().getSort() == Type.VOID) {
			this.methodReturnSize = 0;
		} else if (methodType.getReturnType().getSort() == Type.DOUBLE 
				|| methodType.getReturnType().getSort() == Type.LONG) {
			this.methodReturnSize = 2;
		} else {
			this.methodReturnSize = 1;
		}
			
		this.isStatic = ((access & Opcodes.ACC_STATIC) != 0);
		this.isSynthetic = ((access & Opcodes.ACC_SYNTHETIC) != 0);
		this.objId = objId;
				
		ClassMethodInfo methodProfile = ClassInfoCollector.initiateClassMethodInfo(className, 
				methodName, 
				methodDesc, 
				this.isStatic);
		
		for (Integer idx: methodProfile.idxArray) {
			this.shouldRecordReadLocalVars.add(idx);
		}
		
		if (this.methodName.equals("<init>")) {			
			//For instance method, don't register, since the obj id is not ready
			this.registered = false;
			this.genMethodInfo();
		} else if (this.methodName.equals("<clinit>")) {
			this.genMethodInfo();
			int[] methodInfo = {this.threadId, this.threadMethodId};
			CumuGraphRecorder.registerStaticRecord(this.methodKey, methodInfo);
		} else {
			int[] probe = null;
			if (this.isStatic) {
				probe = CumuGraphRecorder.queryStaticRecord(this.methodKey);
			} else {
				probe = CumuGraphRecorder.queryObjRecord(this.objId, this.methodKey);
			}
			
			if (probe == null) {
				this.genMethodInfo();
				int[] methodInfo = {this.threadId, this.threadMethodId};
				if (this.isStatic) {
					CumuGraphRecorder.registerStaticRecord(this.methodKey, methodInfo);
				} else {
					CumuGraphRecorder.registerObjRecord(this.objId, this.methodKey, methodInfo);
				}
			} else {
				//For cumulating the graph, we need to reuse the original thread id and methodid
				//This is for ensuring the unique id for a method
				this.threadId = probe[0];
				this.threadMethodId = probe[1];
			}
		}
		
		this.callerIdxMap = CumuGraphRecorder.retrieveIdxMap();
		this.lastInst = CumuGraphRecorder.retrieveCallerControl();
	}
		
	private void stopLocalVar(int localVarId) {
		this.shouldRecordReadLocalVars.remove(localVarId);
	}
	
	private void updateReadLocalVar(InstNode localVarNode) {
		int localVarId = Integer.valueOf(localVarNode.getAddInfo());
		if (this.shouldRecordReadLocalVars.contains(localVarId)) {
			String localVarIdxKey = StringUtil.genIdxKey(localVarNode.getThreadId(), 
					localVarNode.getThreadMethodIdx(), 
					localVarNode.getIdx());
			this.firstReadLocalVars.add(localVarIdxKey);
		}
	}
			
	private void updateCachedMap(InstNode parent, InstNode child, int depType) {
		if (depType == MIBConfiguration.INST_DATA_DEP) {
			parent.increChild(child.getThreadId(), child.getThreadMethodIdx(), child.getIdx(), MIBConfiguration.getInstance().getInstDataWeight());
			child.registerParent(parent.getThreadId(), parent.getThreadMethodIdx(), parent.getIdx(), depType);
		} else if (depType == MIBConfiguration.WRITE_DATA_DEP) {
			parent.increChild(child.getThreadId(), child.getThreadMethodIdx(), child.getIdx(), MIBConfiguration.getInstance().getWriteDataWeight());
			child.registerParent(parent.getThreadId(), parent.getThreadMethodIdx(), parent.getIdx(), depType);
		} else if (depType == MIBConfiguration.CONTR_DEP) {
			parent.increChild(child.getThreadId(), child.getThreadMethodIdx(), child.getIdx(), MIBConfiguration.getInstance().getControlWeight());
			child.registerParent(parent.getThreadId(), parent.getThreadMethodIdx(), parent.getIdx(), depType);
		}
	}
	
	private void updateStackSimulator(InstNode fullInst, int addOutput) {
		int outputSize = fullInst.getOp().getOutList().size() + addOutput;
		this.updateStackSimulator(outputSize, fullInst);
	}
	
	private void updateStackSimulator(int times, InstNode fullInst) {
		//System.out.println("Stack push: " + fullInst + " " + times);
		for (int i = 0; i < times; i++) {
			this.stackSimulator.push(fullInst);
		}
	}
	
	private void showStackSimulator() {
		System.out.println(this.stackSimulator);
	}
	
	public void updateCurLabel(String curLabel) {
		if (this.overTime)
			return ;
		
		this.curLabel = curLabel;
	}
	
	private void updateControlRelation(InstNode fullInst) {
		if (this.lastInst != null)
			this.updateCachedMap(this.lastInst, fullInst, MIBConfiguration.CONTR_DEP);
		
		this.lastInst = fullInst;
	}
	
	private synchronized InstNode safePop() {
		if (this.stackSimulator.size() > 0) {
			return this.stackSimulator.pop();
		}
		return null;
	}
	
	public void updateObjOnStack(Object obj, int traceBack) {
		if (this.overTime)
			return ;
		
		int idx = this.stackSimulator.size() - 1 - traceBack;
		InstNode latestInst = this.stackSimulator.get(idx);		
		latestInst.setRelatedObj(obj);
		//System.out.println("Update obj: " + latestInst);
	}
	
	public void updateObjIdOnStack(int objId, int traceBack) {
		int idx = this.stackSimulator.size() - 1 - traceBack;
		InstNode latestInst = this.stackSimulator.get(idx);
		latestInst.setRelatedObjId(objId);
		//System.out.println("Update obj id: " + latestInst + " " + objId);
		//System.exit(-1);
	}
	
	public void handleLdc(int opcode, int instIdx, int times, String addInfo) {		
		if (this.overTime)
			return ;
		
		InstNode fullInst = CumuGraphRecorder.queryInst(this.methodKey, 
				this.threadId, 
				this.threadMethodId, 
				instIdx, 
				opcode, 
				addInfo, 
				InstPool.REGULAR);
		fullInst.setLinenumber(this.linenumber);
		//this.updateTime(fullInst);
		
		this.updateControlRelation(fullInst);
		this.updateStackSimulator(times, fullInst);
		//this.showStackSimulator();
	}
	
	public void handleField(int opcode, int instIdx, String owner, String name, String desc) {		
		if (this.overTime)
			return ;
		
		//Search the real owner of the field
		Class<?> targetClass = ClassInfoCollector.retrieveCorrectClassByField(owner, name);
		String fieldKey = targetClass.getName() + "." + name + "." + desc;
		
		OpcodeObj oo = BytecodeCategory.getOpcodeObj(opcode);
		int opcat = oo.getCatId();
		int typeSort = Type.getType(desc).getSort();
		
		InstNode fullInst = CumuGraphRecorder.queryInst(this.methodKey, 
				this.threadId, 
				this.threadMethodId, 
				instIdx, 
				opcode, 
				fieldKey, 
				InstPool.FIELD);
		fullInst.setLinenumber(this.linenumber);
		this.updateControlRelation(fullInst);
		
		if (MIBConfiguration.getInstance().isFieldTrack()) {
			int objId = 0;
			Object objOnStack = null;
			InstNode tmp = null;
			if (opcode == Opcodes.GETFIELD) {
				tmp = this.stackSimulator.peek();
				objOnStack = this.stackSimulator.peek().getRelatedObj();
				objId = parseObjId(objOnStack);
				//this.stackSimulator.peek().removeRelatedObj();
			} else if (opcode == Opcodes.PUTFIELD) {
				if (typeSort == Type.LONG || typeSort == Type.DOUBLE) {
					tmp = this.stackSimulator.get(this.stackSimulator.size() - 3);
					objOnStack = this.stackSimulator.get(this.stackSimulator.size() - 3).getRelatedObj();
					objId = parseObjId(objOnStack);
				} else {
					tmp = this.stackSimulator.get(this.stackSimulator.size() - 2);
					objOnStack = this.stackSimulator.get(this.stackSimulator.size() - 2).getRelatedObj();
					objId = parseObjId(objOnStack);
				}
			}
			
			String recordFieldKey = fieldKey;
			if (objId > 0) {
				recordFieldKey += (":" + objId);
			} else if (opcode == Opcodes.GETFIELD){
				logger.error("Uinitialized obj: " + opcode + " " + fieldKey + " " + objId);
				logger.error("Obj on stack: " + objOnStack);
				logger.error("Releveant inst: " + tmp);
				logger.error("Current method: " + this.methodKey);
				System.exit(-1);
			} else if (opcode == Opcodes.PUTFIELD) {
				if (tmp.getOp().getOpcode() == Opcodes.ALOAD 
						&& tmp.getAddInfo().equals("0")) {
					objId = tmp.getRelatedObjId();
				}
			}
			
			Class realOwner = ClassInfoCollector.retrieveCorrectClassByField(owner, name);
			//System.out.println("Real owner: " + realOwner.getName());
			if (Type.getType(owner).getSort() != Type.ARRAY 
					&& StringUtil.shouldIncludeClass(realOwner.getName())) {
				if (opcode == Opcodes.PUTSTATIC || opcode == Opcodes.PUTFIELD) {
					//System.out.println("Write field: " + recordFieldKey + " " + fullInst);
					CumuGraphRecorder.registerWriterField(recordFieldKey, fullInst);
				} else if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.GETFIELD) {
					//System.out.println("Read field: " + recordFieldKey + " " + fullInst);
					CumuGraphRecorder.updateReaderField(recordFieldKey, fullInst);
				} else {
					logger.error("Unrecognized field op: " + opcode);
					System.exit(-1);
				}
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
			InstNode curInst = null;
			for (int i = 0; i < inputSize; i++) {
				//Should not return null here
				InstNode tmpInst = this.safePop();
				if (!tmpInst.equals(curInst)) {
					this.updateCachedMap(tmpInst, fullInst, MIBConfiguration.INST_DATA_DEP);
				}
				curInst = tmpInst;
			}
		}
		this.updateStackSimulator(fullInst, addOutput);
		//this.showStackSimulator();
	}
	
	public void handleOpcode(int opcode, int instIdx, String addInfo) {		
		if (this.overTime)
			return ;
		
		OpcodeObj oo = BytecodeCategory.getOpcodeObj(opcode);
		InstNode fullInst = CumuGraphRecorder.queryInst(this.methodKey, 
				this.threadId, 
				this.threadMethodId, 
				instIdx, 
				opcode, 
				addInfo, 
				InstPool.REGULAR);
		fullInst.setLinenumber(this.linenumber);
		//this.updateTime(fullInst);
		this.updateControlRelation(fullInst);
		
		int inputSize = oo.getInList().size();
		if (inputSize > 0) {
			for (int i = 0; i < inputSize; i++) {
				//Should not return null here
				InstNode tmpInst = this.safePop();
				if (tmpInst == null) {
					logger.error("Error pop: " + fullInst);
					logger.error("Input size: " + oo.getInList().size());
					logger.error("Current line: " + this.linenumber);
					//System.out.println("Tmp records: " + this.tmpRecords);
					System.exit(-1);
				}
				
				this.updateCachedMap(tmpInst, fullInst, MIBConfiguration.INST_DATA_DEP);
			}
		}
		this.updateStackSimulator(fullInst, 0);
		//this.showStackSimulator();
	}
	
	/**
	 * localVarIdx is not necessarily a local var
	 * @param opcode
	 * @param localVarIdx
	 */
	public void handleOpcode(int opcode, int instIdx, int localVarIdx) {				
		if (this.overTime)
			return ;
		
		//Don't record return inst, or need to remove it later
		if (BytecodeCategory.returnOps().contains(opcode)) {
			OpcodeObj returnOp = BytecodeCategory.getOpcodeObj(opcode);
			int inputSize = returnOp.getInList().size();
			
			if (inputSize > 0) {
				InstNode tmpInst = this.safePop();
				CumuGraphRecorder.pushCalleeLast(this.methodKey, tmpInst);
				
				this.beforeReturn = tmpInst;
				if (inputSize == 2) {
					this.safePop();
				}
			} else if (!this.methodName.equals("<clinit>")){
				CumuGraphRecorder.pushCalleeLast(this.methodKey, this.lastInst);
			}
			
			return ;
		}
		
		InstNode fullInst = null;
		if (localVarIdx >= 0) {
			fullInst = CumuGraphRecorder.queryInst(this.methodKey, 
					this.threadId, 
					this.threadMethodId, 
					instIdx, 
					opcode, 
					String.valueOf(localVarIdx), 
					InstPool.REGULAR);
		} else {
			fullInst = CumuGraphRecorder.queryInst(this.methodKey, 
					this.threadId, 
					this.threadMethodId, 
					instIdx, 
					opcode, 
					"", 
					InstPool.REGULAR);
		}
		fullInst.setLinenumber(this.linenumber);
		//this.updateTime(fullInst);
		
		int opcat = fullInst.getOp().getCatId();
		
		InstNode lastInst = null;
		if (!stackSimulator.isEmpty()) {
			lastInst = stackSimulator.peek();
		}
		
		if (!BytecodeCategory.dupCategory().contains(opcat)) {
			//Dup inst will be replaced later. No need to add any dep
			this.updateControlRelation(fullInst);
		}
		
		//The store instruction will be the sink. The inst on the stack will be source
		boolean hasUpdate = false;
		if (BytecodeCategory.writeCategory().contains(opcat)) {
			if (localVarIdx >= 0) {
				this.localVarRecorder.put(localVarIdx, fullInst);
			}
			
			this.updateCachedMap(lastInst, fullInst, MIBConfiguration.INST_DATA_DEP);
			for (int i = 0; i < fullInst.getOp().getInList().size(); i++) {
				this.safePop();
			}
			
			if (this.callerIdxMap != null && this.callerIdxMap.containsKey(localVarIdx)) {
				this.callerIdxMap.remove(localVarIdx);
			}
			//this.stopLocalVar(localVarIdx);
		} else if (opcode == Opcodes.IINC) {
			InstNode parentInst = this.localVarRecorder.get(localVarIdx);
			if (parentInst != null)
				this.updateCachedMap(parentInst, fullInst, MIBConfiguration.WRITE_DATA_DEP);
			
			this.localVarRecorder.put(localVarIdx, fullInst);
			if (this.callerIdxMap != null && this.callerIdxMap.containsKey(localVarIdx)) {
				InstNode parentFromCaller = this.callerIdxMap.get(localVarIdx);
				this.updateCachedMap(parentFromCaller, fullInst, MIBConfiguration.WRITE_DATA_DEP);
			}
			
			if (this.callerIdxMap != null) {
				this.callerIdxMap.remove(localVarIdx);
			}
			
			//this.updateReadLocalVar(fullInst);
			//this.stopLocalVar(localVarIdx);
		} else if (BytecodeCategory.readCategory().contains(opcat)) {
			//Search local var recorder;
			InstNode parentInst = this.localVarRecorder.get(localVarIdx);
			if (parentInst != null) {
				this.updateCachedMap(parentInst, fullInst, MIBConfiguration.WRITE_DATA_DEP);
			}
			
			if (this.callerIdxMap != null && this.callerIdxMap.containsKey(localVarIdx)) {
				InstNode parentFromCaller = this.callerIdxMap.get(localVarIdx);
				this.updateCachedMap(parentFromCaller, fullInst, MIBConfiguration.WRITE_DATA_DEP);
			}
			
			//this.updateReadLocalVar(fullInst);
		} else if (BytecodeCategory.dupCategory().contains(opcat)) {
			this.handleDup(opcode);
			//dup should not have any dep, no need to parentRemove
			CumuGraphRecorder.removeInst(fullInst);
			hasUpdate = true;
		} else {			
			int inputSize = fullInst.getOp().getInList().size();
			InstNode lastTmp = null;
			if (inputSize > 0) {
				for (int i = 0; i < inputSize; i++) {
					//Should not return null here
					InstNode tmpInst = this.safePop();
					if (!tmpInst.equals(lastTmp)) {
						this.updateCachedMap(tmpInst, fullInst, MIBConfiguration.INST_DATA_DEP);
					}
					
					lastTmp = tmpInst;
				}
			}
		}
		
		if (!hasUpdate) 
			this.updateStackSimulator(fullInst, 0);
		
		//this.showStackSimulator();
	}
	
	public void handleMultiNewArray(String desc, int dim, int instIdx) {		
		if (this.overTime)
			return ;
		
		String addInfo = desc + " " + dim;
		InstNode fullInst = CumuGraphRecorder.queryInst(this.methodKey, 
				this.threadId, 
				this.threadMethodId, 
				instIdx, 
				Opcodes.MULTIANEWARRAY, 
				addInfo, 
				InstPool.REGULAR);
		fullInst.setLinenumber(this.linenumber);
		//this.updateTime(fullInst);
		
		this.updateControlRelation(fullInst);
		
		for (int i = 0; i < dim; i++) {
			InstNode tmpInst = this.safePop();
			this.updateCachedMap(tmpInst, fullInst, MIBConfiguration.INST_DATA_DEP);
		}
		this.updateStackSimulator(fullInst, 0);
		//this.showStackSimulator();
	}
	
	public void handleRawMethod(int opcode, int instIdx, int linenum, String owner, String name, String desc, InstNode fullInst) {		
		fullInst.setLinenumber(linenum);
		//this.updateTime(fullInst);
		this.updateControlRelation(fullInst);
		
		Type methodType = Type.getMethodType(desc);
		Type[] args = methodType.getArgumentTypes();
		
		for (int i = args.length - 1; i >= 0; i--) {
			Type t = args[i];
			if (t.getDescriptor().equals("D") || t.getDescriptor().equals("J")) {
				this.safePop();
				InstNode tmpInst = this.safePop();
				this.updateCachedMap(tmpInst, fullInst, MIBConfiguration.INST_DATA_DEP);
			} else {
				InstNode tmpInst = this.safePop();
				this.updateCachedMap(tmpInst, fullInst, MIBConfiguration.INST_DATA_DEP);
			}
		}
		
		if (!BytecodeCategory.staticMethodOps().contains(opcode)) {
			InstNode objRef = this.safePop();
			this.updateCachedMap(objRef, fullInst, MIBConfiguration.INST_DATA_DEP);
		}
		
		String returnType = methodType.getReturnType().getDescriptor();
		if (!returnType.equals("V")) {
			if (returnType.equals("D") || returnType.equals("J")) {
				this.updateStackSimulator(2, fullInst);
			} else {
				this.updateStackSimulator(1, fullInst);
			}
		}
		//this.showStackSimulator();
	}
				
	public void handleMethod(int opcode, 
			int instIdx, 
			int linenum, 
			String owner, 
			String name, 
			String desc) {
		
		if (this.overTime)
			return ;
		
		ClassMethodInfo cmi = ClassInfoCollector.retrieveClassMethodInfo(owner, name, desc, opcode);
		int argSize = cmi.argSize;
		Type[] args = cmi.args;
		Type rType = cmi.returnType;
		int[] idxArray = cmi.idxArray;
					
		Class correctClass;
		if (opcode == Opcodes.INVOKESTATIC) {
			correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(owner, name, desc, false);
		} else {
			InstNode relatedInst = this.stackSimulator.get(stackSimulator.size() - argSize - 1);
			Object objOnStack = relatedInst.getRelatedObj();
			
			if (opcode == Opcodes.INVOKESPECIAL && name.equals("<init>")) {
				//constructor
				correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(owner, name, desc, true);
			} else if (opcode == Opcodes.INVOKESPECIAL && owner.equals(this.className)) {
				//private method
				correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(owner, name, desc, true);
			} else if (opcode == Opcodes.INVOKESPECIAL) {
				//super method, may be in grand parents
				correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(owner, name, desc, false);
			} else {
				//logger.info("Retrieve method by class name: " + name + objOnStack.getClass().getName());
				String internalName = Type.getInternalName(objOnStack.getClass());
				correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(internalName, name, desc, false);
			}
		}
		
		String realMethodKey = StringUtil.genKey(correctClass.getName(), name, desc);
		
		boolean sysCall = false;
		HashMap<Integer, InstNode> idxMap = new HashMap<Integer, InstNode>();
		InstNode controlToGlobal = null;
		MethodNode fullInst = null;
				
		if (Type.getType(owner).getSort() == Type.ARRAY 
				|| !StringUtil.shouldIncludeClass(correctClass.getName()) 
				|| !StringUtil.shouldIncludeMethod(name, desc)
				|| CumuGraphRecorder.checkUndersizedMethod(GlobalGraphRecorder.getGlobalName(realMethodKey)) 
				|| CumuGraphRecorder.checkUntransformedClass(correctClass.getName())) {
			//Similar to record system call
			String curMethodKey = StringUtil.genKey(owner, name, desc);
			InstNode ptr = CumuGraphRecorder.queryInst(this.methodKey, 
					this.threadId, 
					this.threadMethodId, 
					instIdx, 
					opcode, 
					curMethodKey, 
					InstPool.METHOD);
			
			fullInst = (MethodNode) ptr;
			fullInst.setLinenumber(this.linenumber);
			this.updateControlRelation(fullInst);
			
			fullInst.increJvmCallees(realMethodKey);	
			sysCall = true;
		}
		
		int stackPtr = this.stackSimulator.size();
		if (args.length > 0) {
			for (int i = args.length - 1; i >= 0 ;i--) {
				Type t = args[i];
				InstNode targetNode = null;
				int idx = idxArray[i];
				if (t.getDescriptor().equals("D") || t.getDescriptor().equals("J")) {
					//this.safePop();
					//targetNode = this.safePop();
					
					targetNode = this.stackSimulator.get(stackPtr - 2);
					stackPtr -= 2;
					//parentFromCaller.put(endIdx, targetNode);
					//this.updateCachedMap(targetNode, fullInst, MIBConfiguration.INST_DATA_DEP);
					//fullInst.registerParentReplay(idx, targetNode);
					if (sysCall) {
						this.updateCachedMap(targetNode, fullInst, MIBConfiguration.INST_DATA_DEP);
					} else {
						idxMap.put(idx, targetNode);
					}
				} else {
					//targetNode = this.safePop();
					
					targetNode = this.stackSimulator.get(stackPtr - 1);
					stackPtr--;
					
					//parentFromCaller.put(endIdx, targetNode);
					//this.updateCachedMap(targetNode, fullInst, MIBConfiguration.INST_DATA_DEP);
					//fullInst.registerParentReplay(idx, targetNode);
					if (sysCall) {
						this.updateCachedMap(targetNode, fullInst, MIBConfiguration.INST_DATA_DEP);
					} else {
						idxMap.put(idx, targetNode);
					}
				}
				
				if (!sysCall && i == args.length - 1) {
					controlToGlobal = targetNode;
				}
			}
		}
		
		if (opcode != Opcodes.INVOKESTATIC) {
			//loadNode can be anyload that load an object
			//InstNode loadNode = this.safePop();
			InstNode loadNode = this.stackSimulator.get(stackPtr - 1);
			stackPtr--;
			
			if (sysCall) {
				this.updateCachedMap(loadNode, fullInst, MIBConfiguration.INST_DATA_DEP);
			} else {
				idxMap.put(0, loadNode);
			}
		}
				
		/*String returnType = rType.getDescriptor();
		if (!returnType.equals("V")) {
			if (returnType.equals("D") || returnType.equals("J")) {
				if (sysCall) {
					this.updateStackSimulator(2, fullInst);
				}
			} else {
				if (sysCall) {
					this.updateStackSimulator(1, fullInst);
				}
			}
		}*/
		
		//Update globel recorder to allow callees to read
		if (!sysCall) {
			CumuGraphRecorder.registerIdxMap(idxMap);
			CumuGraphRecorder.registerCallerControl(controlToGlobal);
			this.currentCallee = realMethodKey;
		} else {
			//Take the method instruction as its last instruction, since it got no graph
			CumuGraphRecorder.pushCalleeLast(this.methodKey, fullInst);
			this.currentCallee = this.methodKey;
		}
		
		//this.handleRawMethod(opcode, instIdx, linenum, owner, name, desc, fullInst);
		//this.showStackSimulator();
		//System.out.println("Ready to pop: " + this.methodKey + "->" + realMethodKey + " " + sysCall);
	}
	
	public void handleMethodAfter(int totalPop, int retSort) {
		for (int i = 0; i < totalPop; i++) {
			this.safePop();
		}
			
		//System.out.println("Pull callee: " + this.methodKey + " " + this.linenumber);
		InstNode calleeLast = CumuGraphRecorder.popCalleeLast(this.currentCallee);
		
		if (calleeLast == null) {
			logger.error("ERROR! empty inst(callee, method name): " + this.currentCallee + " " + this.methodKey);
			CumuGraphRecorder.showCalleeLasts();
			System.exit(-1);
		}
		
		switch(retSort) {
			case 0:
				//Only control
				this.lastInst = calleeLast;
				break ;
			case 1:
				this.lastInst = calleeLast;
				this.updateStackSimulator(1, calleeLast);
				break ;
			case 2:
				this.lastInst = calleeLast;
				this.updateStackSimulator(2, calleeLast);
				break ;
			default:
				logger.error("Invalid return type for after-method handler: " + retSort);
				System.exit(-1);
		}
		//this.showStackSimulator();
	}
			
	public void handleDup(int opcode) {		
		if (this.overTime)
			return ;
		
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
		//this.showStackSimulator();
	}
	
	public void genMethodInfo() {
		this.threadId = ObjectIdAllocater.getThreadId();
		this.threadMethodId = ObjectIdAllocater.getThreadMethodIndex(this.threadId);
	}
	
	public void dumpGraph() {		
		if (this.overTime || TimeController.isOverTime()) {
			return ;
		}
		
		if (!this.registered) {
			int[] methodInfo = {this.threadId, this.threadMethodId};
			CumuGraphRecorder.registerObjRecord(this.objId, this.methodKey, methodInfo);
		}
		//System.out.println("End method: " + this.methodKey);
				
		/*if (this.beforeReturn != null) {
			this.graph.addLastBeforeReturn(this.beforeReturn);
		}
				
		HashMap<String, AbstractGraph> calleeRequired = new HashMap<String, AbstractGraph>();
		Iterator<InstNode> instIterator = this.pool.iterator();
		int edgeNum = 0, vertexNum = this.pool.size();
		int eDelta = 0, vDelta = 0;
		int maxChildVertex = 0;
		
		while (instIterator.hasNext()) {
			InstNode curInst = instIterator.next();
			curInst.removeRelatedObj();
			
			int childNum = curInst.getChildFreqMap().size();
			if (curInst instanceof CumuMethodNode) {
				CumuMethodNode mn = (CumuMethodNode) curInst;
				//HashMap<GraphTemplate, Double> repCallees = MethodNode.extractCallee(mn.getCallees(), mn.getMaxCalleeFreq());
				HashMap<AbstractGraph, Double> repCallees = MethodNode.extractCalleeDirect(mn);
				
				int instParentNum = mn.getInstDataParentList().size();
				int controlParentNum = mn.getControlParentList().size();
				for (AbstractGraph absRepCallee: repCallees.keySet()) {
					CumuGraph repCallee = (CumuGraph) absRepCallee;
					String repKey = StringUtil.genThreadWithMethodIdx(repCallee.getThreadId(), repCallee.getThreadMethodId());
					double normFreq = repCallees.get(repCallee);
					mn.registerDomCalleeIdx(repKey, normFreq, repCallee.getLastBeforeReturn());
					calleeRequired.put(repKey, repCallee);
										
					vDelta += (repCallee.getVertexNum());
					int firstReadNum = repCallee.getFirstReadLocalVars().size();
					int delta = repCallee.getEdgeNum() 
							+ firstReadNum 
							+ firstReadNum * controlParentNum
							+ childNum;
					eDelta += delta;
					
					if (repCallee.getVertexNum() > maxChildVertex) {
						maxChildVertex = repCallee.getVertexNum();
					}
				}
				mn.clearCallees();
				
				//If there is instFreq, the MethodNode eventually becomes InstNode, so keep v and e
				//else recompute the v and e
				if (mn.getRegularState().count == 0) {
					vDelta--;
					eDelta = eDelta - instParentNum - controlParentNum - childNum;
				}
			}
			edgeNum += childNum;
		}		
		vertexNum = vertexNum + vDelta;
		edgeNum = edgeNum + eDelta;
				
		this.graph.setEdgeNum(edgeNum);
		this.graph.setVertexNum(vertexNum);
		this.graph.setChildDominant(maxChildVertex);
		this.graph.calleeRequired = calleeRequired;	*/	
		
		/*String dumpKey = StringUtil.genKeyWithId(this.shortMethodKey, String.valueOf(this.threadId));
		if (this.isStatic) {
			CumuGraphRecorder.registerStaticGraph(this.methodKey, gt);
		} else {
			CumuGraphRecorder.registerObjGraph(this.objId, gt);
		}*/
				
		//this.showStackSimulator();
		/*logger.info("Leave " + 
				" " + this.methodKey + 
				" " + this.threadId + 
				" " + this.threadMethodId);*/
	}
}

