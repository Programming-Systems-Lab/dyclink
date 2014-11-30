package edu.columbia.psl.cc.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.inst.BlockAnalyzer.Block;
import edu.columbia.psl.cc.inst.BlockAnalyzer.InstTuple;
import edu.columbia.psl.cc.pojo.GraphGroup;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.pojo.StaticInitGraphTemplate;
import edu.columbia.psl.cc.pojo.StaticMethodMiner;

public class MethodStackRecorder {
	
	private static Logger logger = Logger.getLogger(MethodStackRecorder.class);
		
	private static TypeToken<GraphTemplate> GRAPH_TOKEN = new TypeToken<GraphTemplate>(){};
	
	public static int CONSTRUCTOR_DEFAULT = -5;
	
	private static String init = "<init>";
	
	private static String clinit = "<clinit>";
		
	private String className;
	
	private String methodName;
	
	private String methodDesc;
	
	private String methodKey;
	
	private String shortMethodKey;
	
	private boolean recursive = false;
	
	private boolean staticMethod;
	
	private int methodArgSize = 0;
	
	private int methodReturnSize = 0;
	
	//private StaticMethodMiner smm;
		
	private Stack<InstNode> stackSimulator = new Stack<InstNode>();
	
	//private HashSet<InstNode> curControlInsts= new HashSet<InstNode>();
	private InstNode curControlInst;
	
	//Key: local var idx, Val: inst node
	private Map<Integer, InstNode> localVarRecorder = new HashMap<Integer, InstNode>();
	
	//Key: field name, Val: inst node
	private HashMap<String, InstNode> latestWriteFieldRecorder = new HashMap<String, InstNode>();
	
	//private HashMap<String, HashSet<InstNode>> firstReadFields = new HashMap<String, HashSet<InstNode>>();
	
	//Record which insts might be affecte by field written by parent method
	private HashSet<InstNode> firstReadLocalVars = new HashSet<InstNode>();
	
	private HashSet<Integer> shouldRecordReadLocalVars = new HashSet<Integer>();
	
	private List<InstNode> path = new ArrayList<InstNode>();
	
	protected String curLabel = null;
	
	public int linenumber = 0;
	
	protected InstPool pool = new InstPool();
	
	private InstNode beforeReturn;
	
	private long threadId = -1;
	
	private int threadMethodId = -1;
	
	public int objId = 0;
	
	private HashMap<String, GraphGroup> calleeCache = new HashMap<String, GraphGroup>();
	
	public MethodStackRecorder(String className, 
			String methodName, 
			String methodDesc, 
			int objId) {
		this.className = className;
		this.methodName = methodName;
		this.methodDesc = methodDesc;
				
		this.methodKey = StringUtil.genKey(className, methodName, methodDesc);
		this.shortMethodKey = GlobalRecorder.getGlobalName(methodKey);
		Type methodType = Type.getMethodType(this.methodDesc);
		this.methodArgSize = methodType.getArgumentTypes().length;
		if (!methodType.getReturnType().getDescriptor().equals("V")) {
			this.methodReturnSize = 1;
		}
		
		//this.smm = GlobalRecorder.getStaticMethodMiner(this.shortMethodKey);
		//this.threadId = Thread.currentThread().getId();
		
		this.threadId = ObjectIdAllocater.getThreadId();
		this.threadMethodId = ObjectIdAllocater.getThreadMethodIndex(className, 
				methodName, 
				methodDesc, 
				this.threadId);
		
		if (objId == 0)
			this.staticMethod = true;
		
		this.objId = objId;
		
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
		
		logger.info("Enter " + this.className + 
				" " + this.methodName + 
				" " + this.methodKey + 
				" " + this.threadId + 
				" " + this.threadMethodId);
		
		//Load possible clinit
		if (this.methodName.equals(init)) {
			//Instead of using NEW, let init to attempt loading clinit
			this.checkNGetClInit(this.className);
		}
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
		
	private void updatePath(InstNode fullInst) {
		//this.path.add(fullInst);
	}
	
	private void updateTime(InstNode fullInst) {
		long[] curTime = GlobalRecorder.getCurTime();
		if (fullInst.getStartTime() < 0) {
			fullInst.setStartDigit(curTime[1]);
			fullInst.setStartTime(curTime[0]);
			fullInst.setUpdateDigit(curTime[1]);
			fullInst.setUpdateTime(curTime[0]);
		} else {
			fullInst.setUpdateDigit(curTime[1]);
			fullInst.setUpdateTime(curTime[0]);
		}
 	}
	
	private void updateCachedMap(InstNode parent, InstNode child, int depType) {
		if (depType == MIBConfiguration.INST_DATA_DEP) {
			parent.increChild(child.getFromMethod(), child.getThreadId(), child.getThreadMethodIdx(), child.getIdx(), MIBConfiguration.getInstance().getInstDataWeight());
			child.registerParent(parent.getFromMethod(), parent.getThreadId(), parent.getThreadMethodIdx(), parent.getIdx(), depType);
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
		//System.out.println(this.stackSimulator);
		logger.info(this.stackSimulator);
	}
	
	public void updateCurLabel(String curLabel) {
		this.curLabel = curLabel;
	}
	
	private void updateControlRelation(InstNode fullInst) {
		if (this.curControlInst != null)
			this.updateCachedMap(this.curControlInst, fullInst, MIBConfiguration.CONTR_DEP);
	}
	
	public void checkNGetClInit(String targetClass) {
		String cleanClass = StringUtil.cleanPunc(targetClass, ".");
		String targetDumpKey = GlobalRecorder.getLoadedClass(cleanClass);
		if (targetDumpKey == null) {
			logger.info("Current method: " + this.methodKey);
			logger.info("No record for: " + cleanClass);
			logger.info("Current map: " + GlobalRecorder.getLoadedClasses());
			return ;
		}
		
		logger.info(this.methodKey + " is loading " + cleanClass + " clinit");
		this.doLoadParent(targetDumpKey);
	}
		
	public void loadParent(String owner, String name, String desc) {
		String methodKey = StringUtil.genKey(owner, name, desc);
		logger.info("Attempt to load parent/self constructor: " + methodKey);
		
		//String searchKey = StringUtil.genKeyWithId(methodKey, String.valueOf(this.threadId));
		String shortName = GlobalRecorder.getGlobalName(methodKey);
		if (shortName != null) {
			String searchKey = StringUtil.genKeyWithId(GlobalRecorder.getGlobalName(methodKey), String.valueOf(this.threadId));
			logger.info("Search key: " + searchKey);
			this.doLoadParent(searchKey);
		} else {
			logger.info("Null short name in record: " + methodKey);
		}
		
	}
	
	private void doLoadParent(String searchKey) {
		String filePath = "";
		if (MIBConfiguration.getInstance().isTemplateMode()) {
			filePath = MIBConfiguration.getInstance().getTemplateDir() + "/" + searchKey + ".json";
		} else {
			filePath = MIBConfiguration.getInstance().getTestDir() + "/" + searchKey + ".json";
		}
		GraphTemplate parentGraph = TemplateLoader.loadTemplateFile(filePath, GRAPH_TOKEN);
		
		if (parentGraph == null) {
			logger.warn("Load no parent/clinit graph: " + searchKey);
			return ;
		}
		
		InstPool parentPool = parentGraph.getInstPool();
		
		GraphUtil.removeReturnInst(parentGraph.getInstPool());
		GraphUtil.unionInstPools(this.pool, parentPool);
	}
	
	private synchronized InstNode safePop() {
		if (this.stackSimulator.size() > 0) {
			return this.stackSimulator.pop();
		}
		return null;
	}
	
	public void updateObjOnStack(Object obj, int traceBack) {
		//logger.info("Logged obj: " + obj);
		//logger.info("Trace back: " + traceBack);
		//logger.info("Current statck: " + this.stackSimulator);
		int idx = this.stackSimulator.size() - 1 - traceBack;
		InstNode latestInst = this.stackSimulator.get(idx);
		latestInst.setRelatedObj(obj);
	}
	
	public void handleLdc(int opcode, int instIdx, int times, String addInfo) {
		logger.info("Handle ldc: " + opcode + " " + instIdx + " " + addInfo);
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, this.threadId, this.threadMethodId, instIdx, opcode, addInfo);
		fullInst.setLinenumber(this.linenumber);
		this.updateTime(fullInst);
		
		this.updateControlRelation(fullInst);
		this.updatePath(fullInst);
		
		this.updateStackSimulator(times, fullInst);
		this.showStackSimulator();
	}
	
	public void handleField(int opcode, int instIdx, String owner, String name, String desc) {
		logger.info("Handle field: " + opcode + " " + instIdx + " " + owner + " " + name + " " + desc);
		OpcodeObj oo = BytecodeCategory.getOpcodeObj(opcode);
		int opcat = oo.getCatId();
		int typeSort = Type.getType(desc).getSort();
		
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
		logger.info("Class owner of field with objId: " + targetClass + " " + desc + " " + objId);
		
		if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) {
			//JVM will load the owner, not the exact class
			if (!owner.equals(this.className))
				this.checkNGetClInit(targetClass.getName());
		}
		
		String fieldKey = targetClass.getName() + "." + name + "." + desc;
		
		if (objId > 0) {
			//fieldKey += objId;
			fieldKey = fieldKey + ":" + objId;
		}
		
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, this.threadId, this.threadMethodId, instIdx, opcode, fieldKey);
		fullInst.setLinenumber(this.linenumber);
		this.updateTime(fullInst);
		this.updateControlRelation(fullInst);
		this.updatePath(fullInst);
		
		if (BytecodeCategory.readFieldCategory().contains(opcat) && objId >= 0) {
			//Add info for field: owner + name + desc + objId
			//Only record static or the instrumented object
			if (opcode == Opcodes.GETSTATIC || objId > 0) {
				//InstNode parent = this.fieldRecorder.get(fieldKey);
				InstNode parent = GlobalRecorder.getWriteFieldNode(fieldKey);
				if (parent != null) {
					this.updateCachedMap(parent, fullInst, MIBConfiguration.WRITE_DATA_DEP);
				}
			}
		} else if (BytecodeCategory.writeFieldCategory().contains(opcat) && objId >= 0) {
			if (opcode == Opcodes.PUTSTATIC || objId > 0) {
				GlobalRecorder.updateGlobalWriteFieldRecorder(fieldKey, fullInst);
				this.latestWriteFieldRecorder.put(fieldKey, fullInst);
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
		this.showStackSimulator();
	}
	
	public void handleOpcode(int opcode, int instIdx, String addInfo) {
		logger.info("Handle opcode: " + opcode + " " + instIdx + " " + addInfo);
		OpcodeObj oo = BytecodeCategory.getOpcodeObj(opcode);
		int opcat = oo.getCatId();
		
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, this.threadId, this.threadMethodId, instIdx, opcode, addInfo);
		fullInst.setLinenumber(this.linenumber);
		this.updateTime(fullInst);
		this.updateControlRelation(fullInst);
		this.updatePath(fullInst);
		
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
		
		if (BytecodeCategory.controlCategory().contains(opcat) 
				|| opcode == Opcodes.TABLESWITCH 
				|| opcode == Opcodes.LOOKUPSWITCH) {
			this.curControlInst = fullInst;
		}
	}
	
	/**
	 * localVarIdx is not necessarily a local var
	 * @param opcode
	 * @param localVarIdx
	 */
	public void handleOpcode(int opcode, int instIdx, int localVarIdx) {
		logger.info("Handle opcode: " + opcode + " " + localVarIdx);
		
		InstNode fullInst = null;
		if (localVarIdx >= 0) {
			fullInst = this.pool.searchAndGet(this.methodKey, this.threadId, this.threadMethodId, instIdx, opcode, String.valueOf(localVarIdx));
		} else {
			fullInst = this.pool.searchAndGet(this.methodKey, this.threadId, this.threadMethodId, instIdx, opcode, "");
		}
		fullInst.setLinenumber(this.linenumber);
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
			int inputSize = fullInst.getOp().getInList().size();
			InstNode lastTmp = null;
			if (inputSize > 0) {
				for (int i = 0; i < inputSize; i++) {
					//Should not return null here
					InstNode tmpInst = this.safePop();
					if (!tmpInst.equals(lastTmp)) {
						this.updateCachedMap(tmpInst, fullInst, MIBConfiguration.INST_DATA_DEP);
						
						if (BytecodeCategory.returnOps().contains(fullInst.getOp().getOpcode())) {
							this.beforeReturn = tmpInst;
						}
					}
					
					lastTmp = tmpInst;
				}
			}
		}
		
		if (!hasUpdate) 
			this.updateStackSimulator(fullInst, 0);
		this.showStackSimulator();
	}
	
	public void handleMultiNewArray(String desc, int dim, int instIdx) {
		logger.info("Handle MultiNewArray: " + desc + " " + dim + " " + instIdx);
		String addInfo = desc + " " + dim;
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, this.threadId, this.threadMethodId, instIdx, Opcodes.MULTIANEWARRAY, addInfo);
		fullInst.setLinenumber(this.linenumber);
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
		logger.info("Handling uninstrumented/undersize method: " + opcode + " " + instIdx + " " + owner + " " + name + " " + desc);
		fullInst.setLinenumber(this.linenumber);
		this.updateTime(fullInst);
		this.updateControlRelation(fullInst);
		
		Type methodType = Type.getMethodType(desc);
		Type[] args = methodType.getArgumentTypes();
		
		logger.info("Arg size: " + args.length);
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
		
		if (!BytecodeCategory.staticMethod().contains(opcode)) {
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
		this.showStackSimulator();
	}
	
	public void handleMethod(int opcode, int instIdx, int linenum, String owner, String name, String desc) {
		logger.info("Handle method: " + opcode + " " + instIdx + " " + owner + " " + name + " " + desc);
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
			logger.info("Arg size: " + argSize);
			
			if (this.className.equals(owner) 
					&& this.methodName.equals(name) 
					&& this.methodDesc.equals(desc)) {
				//To stop horizontal merge
				this.recursive = true;
			}
			
			//Load the correct graph
			Class<?> correctClass = null;
			int objId = -1;
			if (owner.equals("java/lang/Class") && name.equals("forName")) {
				Object objOnStack = (this.stackSimulator.peek()).getRelatedObj();
				String realOwner = objOnStack.toString();
				this.checkNGetClInit(realOwner);
				correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(owner, name, desc, false);
			} else if (owner.equals("java/lang/Class") 
					&& name.equals("newInstance") 
					&& desc.equals("()Ljava/lang/Object;")) {
				InstNode relatedInst = this.stackSimulator.get(stackSimulator.size() - argSize - 1);
				Object objOnStack = relatedInst.getRelatedObj();
				Class classOnStack = (Class)objOnStack;
				//loadParent can load self constructor too
				this.loadParent(classOnStack.getName(), "<init>", "()V");
				correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(owner, name, desc, false);
			} else if (BytecodeCategory.staticMethod().contains(opcode)) {
				correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(owner, name, desc, false);
				objId = 0;
				//Static member may load the clinit, Class.forName is another possible way
				this.checkNGetClInit(correctClass.getName());
			} else {
				InstNode relatedInst = this.stackSimulator.get(stackSimulator.size() - argSize - 1);
				Object objOnStack = relatedInst.getRelatedObj();
				//methodId = ObjectIdAllocater.parseObjId(objOnStack);
				objId = ObjectIdAllocater.parseObjId(objOnStack);
				
				if (objOnStack == null && this.objId != CONSTRUCTOR_DEFAULT) {
					logger.info("Responsible inst for null obj: " + relatedInst);
				}
				
				if (opcode == Opcodes.INVOKESPECIAL) {
					correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(owner, name, desc, true);
				} else {
					correctClass = ClassInfoCollector.retrieveCorrectClassByMethod(objOnStack.getClass().getName(), name, desc, false);
				}
			}
			
			logger.info("Method owner: " + correctClass.getName());			
			String methodKey = StringUtil.genKey(correctClass.getName(), name, desc);
			String shortMethodKey = GlobalRecorder.getGlobalName(methodKey);
			
			String fullKeyWithThreadId = StringUtil.genKeyWithId(methodKey, String.valueOf(this.threadId));
			String shortKeyWithThreadId = null;
			if (shortMethodKey == null) {
				shortKeyWithThreadId = fullKeyWithThreadId;
			} else {
				shortKeyWithThreadId = StringUtil.genKeyWithId(shortMethodKey, String.valueOf(this.threadId));
			}
			
			String fullKeyWithThreadObjId = StringUtil.genKeyWithObjId(fullKeyWithThreadId, objId);
			logger.info("Full key with thread obj id: " + fullKeyWithThreadObjId);
			logger.info("Short key with thread id: " + shortKeyWithThreadId);
			InstNode fullInst = this.pool.searchAndGet(this.methodKey, this.threadId, this.threadMethodId, instIdx, opcode, methodKey);
			
			//Don't update, because we will remove inst before leaving the method
			//this.updateControlRelation(fullInst);
			this.updatePath(fullInst);
			
			String filePath = "";
			if (MIBConfiguration.getInstance().isTemplateMode()) {
				filePath = MIBConfiguration.getInstance().getTemplateDir() + "/" + shortKeyWithThreadId + ".json";
			} else {
				filePath = MIBConfiguration.getInstance().getTestDir() + "/" + shortKeyWithThreadId + ".json";
			}
			GraphTemplate childGraph = TemplateLoader.loadTemplateFile(filePath, GRAPH_TOKEN);
			
			//This means that the callee method is from jvm, keep the method inst in graph
			boolean removeReturn = true;
			if (childGraph == null) {
				logger.info("Graph not found: " + shortKeyWithThreadId);
				this.handleUninstrumentedMethod(opcode, instIdx, linenum, owner, name, desc, fullInst);
				return ;
			} else if (childGraph.getInstPool().size() < MIBConfiguration.getInstance().getInstThreshold()){
				logger.info("Graph under-sized: " + childGraph.getInstPool().size());
				
				//Change the inst idx for preventing this inst will be removed in the future
				int oldInstIdx = fullInst.getIdx();
				int newInstIdx = (1 + oldInstIdx) * MIBConfiguration.getInstance().getIdxExpandFactor();
				this.pool.remove(fullInst);
				fullInst = this.pool.searchAndGet(this.methodKey, this.threadId, this.threadMethodId, newInstIdx, opcode, methodKey);
				
				//if any field in global record id written by this child, change it to the rep inst
				if (childGraph.getLatestWriteFields().size() > 0) {
					GlobalRecorder.replaceWriteFieldNodes(childGraph, fullInst);
				}
				
				this.handleUninstrumentedMethod(opcode, instIdx, linenum, owner, name, desc, fullInst);
				return ;
			} else if (this.calleeCache.containsKey(fullKeyWithThreadObjId)) {
				GraphGroup gGroup = this.calleeCache.get(fullKeyWithThreadObjId);
				
				//Check if there is similar graph
				GraphTemplate rep = gGroup.getGraph(childGraph);
				if (rep != null) {
					logger.info("Find similar graph in cache: " + fullKeyWithThreadObjId);
					logger.info(childGraph.getThreadMethodId() + " replaced by " + rep.getThreadMethodId());
					logger.info("Child graph feature (node dep): " + childGraph.getInstPool().size() + " " + childGraph.getEdgeNum());
					logger.info("All group keys now: " + gGroup.keySet());
					
					//Guess that this graph is the same
					childGraph = rep;
					removeReturn = false;
				} else {
					logger.info("Find no similar graph in cache: " + fullKeyWithThreadObjId);
					logger.info("Existing graph group key: " + gGroup.keySet());
					logger.info("Current graph key: " + GraphGroup.groupKey(childGraph));
					gGroup.addGraph(childGraph);
				}
			} else {
				logger.info("Caller " + this.methodKey + " " + this.threadId + " " + this.threadMethodId);
				logger.info("creates new graph group for: " + fullKeyWithThreadObjId);
				GraphGroup gGroup = new GraphGroup();
				gGroup.addGraph(childGraph);
				this.calleeCache.put(fullKeyWithThreadObjId, gGroup);
			}
			
			logger.info("Child graph analysis: " + childGraph.getMethodKey() + " " + childGraph.getThreadId() + " " + childGraph.getThreadMethodId());
			logger.info("Child graph size: " + childGraph.getInstPool().size());
			logger.info("Recorded vertex edge size: " + childGraph.getVertexNum() + " " + childGraph.getEdgeNum());
						
			//Remove return
			InstPool childPool = childGraph.getInstPool();
			if (removeReturn) {
				GraphUtil.removeReturnInst(childPool);
			} else {
				HashMap<String, InstNode> instCached = new HashMap<String, InstNode>();
				for (InstNode cInst: childPool) {
					this.updateTime(cInst);
					String instKey = StringUtil.genIdxKey(cInst.getFromMethod(), 
							cInst.getThreadId(), 
							cInst.getThreadMethodIdx(), 
							cInst.getIdx());
					instCached.put(instKey, cInst);
				}
				
				for (InstNode cInst: childPool) {
					for (String parentKey: cInst.getInstDataParentList()) {
						InstNode parentNode = instCached.get(parentKey);
						
						//Parent node is null if it's the interface between two methods
						if (parentNode == null) {
							String[] parentArray = StringUtil.parseIdxKey(parentKey);
							parentNode = this.pool.searchAndGet(parentArray[0], 
									Long.valueOf(parentArray[1]), 
									Integer.valueOf(parentArray[2]), 
									Integer.valueOf(parentArray[2]));
						}
						
						if (parentNode != null) {
							parentNode.increChild(cInst.getFromMethod(), 
									cInst.getThreadId(), 
									cInst.getThreadMethodIdx(), 
									cInst.getIdx(), 
									MIBConfiguration.getInstance().getInstDataWeight());
						} else {
							logger.warn("Parent instruction " + parentKey + " for child " + cInst);
						}
					}
				}
				
				if (childGraph.getLatestWriteFields().size() > 0 && childGraph.getObjId() >= 0) {
					GlobalRecorder.replaceWriteFieldNodes(childGraph);
				}
			}
			
			//Search for correct inst, update local data dep dependency
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
				}
			}
			
			if (!BytecodeCategory.staticMethod().contains(opcode)) {
				//loadNode can be anyload that load an object
				InstNode loadNode = this.safePop();
				parentFromCaller.put(0, loadNode);
			}
			
			if (parentFromCaller.size() > 0) {
				GraphUtil.dataDepFromParentToChild(parentFromCaller, this.pool, childGraph);
			}
			
			//Update control dep
			if (this.curControlInst != null) {
				//GraphUtil.controlDepFromParentToChild(this.curControlInst, childPool);
				//Just use input, or the graph volume will be too large
				GraphUtil.controlDepFromParentToChild(this.curControlInst, childGraph.getFirstReadLocalVars());
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
			logger.error(ex);
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
	
	public void dumpGraph() {
		GraphTemplate gt = new GraphTemplate();
		
		gt.setMethodKey(this.methodKey);
		gt.setShortMethodKey(this.shortMethodKey);
		gt.setThreadId(this.threadId);
		gt.setThreadMethodId(this.threadMethodId);
		gt.setThreadId(this.threadId);
		gt.setObjId(this.objId);
		gt.setMethodArgSize(this.methodArgSize);
		gt.setMethodReturnSize(this.methodReturnSize);
		gt.setStaticMethod(this.staticMethod);
		gt.setFirstReadLocalVars(this.firstReadLocalVars);
		gt.setLatestWriteFields(this.latestWriteFieldRecorder);
		//gt.setFirstReadFields(this.firstReadFields);
		//gt.setWriteFields(this.fieldRecorder);
		
		if (this.beforeReturn != null) {
			gt.setLastBeforeReturn(this.beforeReturn);
			logger.info("Before return inst: " + this.beforeReturn);
		} else {
			logger.info("No before return inst");
		}
		//gt.setPath(this.path);
		
		int edgeCount = 0;
		Iterator<InstNode> instIterator = this.pool.iterator();
		while (instIterator.hasNext()) {
			InstNode curInst = instIterator.next();
			TreeMap<String, Double> children = curInst.getChildFreqMap();
			edgeCount += children.size();
		}
		
		gt.setEdgeNum(edgeCount);
		gt.setVertexNum(this.pool.size());
		gt.setInstPool(this.pool);
		
		logger.info("Total edge count: " + gt.getEdgeNum());
		logger.info("Total vertex count: " + gt.getVertexNum());
		
		//String dumpKey = StringUtil.genKeyWithId(this.methodKey, String.valueOf(this.threadId));
		String dumpKey = StringUtil.genKeyWithId(this.shortMethodKey, String.valueOf(this.threadId));
		TypeToken<GraphTemplate> typeToken = new TypeToken<GraphTemplate>(){};
		if (MIBConfiguration.getInstance().isTemplateMode()) {
			GsonManager.cacheGraph(dumpKey, 0, false);
			GsonManager.writeJsonGeneric(gt, dumpKey, typeToken, 0);
		} else {
			GsonManager.cacheGraph(dumpKey, 1, false);
			GsonManager.writeJsonGeneric(gt, dumpKey, typeToken, 1);
		}
		//GsonManager.writePath(dumpKey, this.path);
		
		if (this.methodName.equals(clinit)) {
			GlobalRecorder.registerLoadedClass(StringUtil.cleanPunc(this.className, "."), 
					dumpKey);
		}
		
		if (this.recursive) {
			GlobalRecorder.registerRecursiveMethod(dumpKey);
		}
		
		//Debuggin, check graph group
		logger.info("Graph groups:");
		for (String searchKey: this.calleeCache.keySet()) {
			logger.info("Group name: " + searchKey);
			GraphGroup gGroup = this.calleeCache.get(searchKey);
			logger.info(gGroup.keySet());
		}
		
		logger.info("Leave " + this.className + 
				" " + this.methodName + 
				" " + this.methodKey + 
				" " + this.threadId + 
				" " + this.threadMethodId);
	}

}
