package edu.columbia.psl.cc.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.columbia.psl.cc.abs.IMethodMiner;
import edu.columbia.psl.cc.abs.IRecorder;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.ClassMethodInfo;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.MethodNode;
import edu.columbia.psl.cc.pojo.OpcodeObj;

public class CumuMethodRecorder implements IRecorder {
	
	private static Logger logger = LogManager.getLogger(CumuMethodRecorder.class);
			
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
	
	protected InstPool pool = null;
	
	private boolean newGraph = true;
		
	private InstNode beforeReturn;
	
	private int threadId = -1;
	
	private int threadMethodId = -1;
	
	public int objId = 0;
	
	private boolean overTime = false;
	
	//private boolean stopRecord = false;
	
	public boolean initConstructor = true;
	
	public CumuMethodRecorder(String className, 
			String methodName, 
			String methodDesc, 
			int access, 
			int objId) {
		
		if (TimeController.isOverTime()) {
			this.overTime = true;
			return ;
		}
		
		this.className = className;
		this.methodName = methodName;
		this.methodDesc = methodDesc;
				
		this.methodKey = StringUtil.genKey(className, methodName, methodDesc);
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
		
		if (this.methodName.equals("<init>") || this.methodName.equals("<clinit>")) {
			this.threadId = ObjectIdAllocater.getThreadId();
			this.threadMethodId = ObjectIdAllocater.getThreadMethodIndex(this.threadId);
			
			//No matter what, this is going to be a new graph
			this.pool = new InstPool();
		} else {
			GraphTemplate probe = null;
			if (this.isStatic) {
				probe = CumuGraphRecorder.retrieveStaticGraph(this.methodKey);
			} else {
				probe = CumuGraphRecorder.retrieveObjGraph(this.objId, this.methodKey);
			}
			
			if (probe == null) {
				this.threadId = ObjectIdAllocater.getThreadId();
				this.threadMethodId = ObjectIdAllocater.getThreadMethodIndex(this.threadId);
				
				this.pool = new InstPool();
			} else {
				//For cumulating the graph, we need to reuse the original thread id and methodid
				//This is for ensuring the unique id for a method
				this.threadId = probe.getThreadId();
				this.threadMethodId = probe.getThreadMethodId();
				
				this.pool = probe.getInstPool();
				this.newGraph = false;
			}
		}
	}
	
	public static final int parseObjId(Object value) {
		if (value == null)
			return -1;
		
		Class<?> valueClass = value.getClass();
		try {
			Field idField = valueClass.getField(IMethodMiner.__mib_id);
			idField.setAccessible(true);
			int objId = idField.getInt(value);
			return objId;
		} catch (Exception ex) {
			logger.warn("Warning: object " + valueClass + " is not MIB-instrumented");
			return -1;
		}
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
	}
	
	public void handleLdc(int opcode, int instIdx, int times, String addInfo) {		
		if (this.overTime)
			return ;
		
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, 
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
		
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, 
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
			if (opcode == Opcodes.GETFIELD) {
				objId = parseObjId(this.stackSimulator.peek().getRelatedObj());
				//this.stackSimulator.peek().removeRelatedObj();
			} else if (opcode == Opcodes.PUTFIELD) {
				if (typeSort == Type.LONG || typeSort == Type.DOUBLE) {
					objId = parseObjId(this.stackSimulator.get(this.stackSimulator.size() - 3).getRelatedObj());
				} else {
					objId = parseObjId(this.stackSimulator.get(this.stackSimulator.size() - 2).getRelatedObj());
				}
			}
			
			String recordFieldKey = fieldKey;
			if (objId > 0) {
				recordFieldKey += (":" + objId);
			} else if (opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD){
				logger.error("Uinitialized obj: " + opcode + " " + fieldKey + " " + objId);
				logger.error("Current method: " + this.methodKey);
				System.exit(-1);
			}
			
			Class realOwner = ClassInfoCollector.retrieveCorrectClassByField(owner, name);
			System.out.println("Real owner: " + realOwner.getName());
			if (Type.getType(owner).getSort() != Type.ARRAY 
					&& StringUtil.shouldIncludeClass(realOwner.getName())) {
				if (opcode == Opcodes.PUTSTATIC || opcode == Opcodes.PUTFIELD) {
					System.out.println("Write field: " + recordFieldKey + " " + fullInst);
					CumuGraphRecorder.registerWriterField(recordFieldKey, fullInst);
				} else if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.GETFIELD) {
					System.out.println("Read field: " + recordFieldKey + " " + fullInst);
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
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, 
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
				this.beforeReturn = tmpInst;
				if (inputSize == 2) {
					this.safePop();
				}
			}
			return ;
		}
		
		InstNode fullInst = null;
		if (localVarIdx >= 0) {
			fullInst = this.pool.searchAndGet(this.methodKey, 
					this.threadId, 
					this.threadMethodId, 
					instIdx, 
					opcode, 
					String.valueOf(localVarIdx), 
					InstPool.REGULAR);
		} else {
			fullInst = this.pool.searchAndGet(this.methodKey, 
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
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, 
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
	
	public void handleMethod(int opcode, int instIdx, int linenum, String owner, String name, String desc) {		
		if (this.overTime)
			return ;
		
		//long startTime = System.nanoTime();		
		String curMethodKey = StringUtil.genKey(owner, name, desc);
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, 
				this.threadId, 
				this.threadMethodId, 
				instIdx, 
				opcode, 
				curMethodKey, 
				InstPool.REGULAR);
		
		this.handleRawMethod(opcode, instIdx, linenum, owner, name, desc, fullInst);
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
	
	public void dumpGraph() {		
		if (this.overTime || TimeController.isOverTime() || !this.newGraph) {
			return ;
		}
				
		GraphTemplate gt = new GraphTemplate();
		
		gt.setMethodKey(this.methodKey);
		gt.setMethodName(this.methodName);
		gt.setMethodDesc(this.methodDesc);
		gt.setShortMethodKey(this.shortMethodKey);
		gt.setThreadId(this.threadId);
		gt.setThreadMethodId(this.threadMethodId);
		gt.setObjId(this.objId);
		gt.setMethodArgSize(this.methodArgSize);
		gt.setMethodReturnSize(this.methodReturnSize);
		gt.setStaticMethod(this.isStatic);
		gt.setFirstReadLocalVars(this.firstReadLocalVars);
		
		if (this.beforeReturn != null) {
			gt.setLastBeforeReturn(this.beforeReturn);
			//logger.info("Before return inst: " + this.beforeReturn);
		}
				
		HashMap<String, GraphTemplate> calleeRequired = new HashMap<String, GraphTemplate>();
		Iterator<InstNode> instIterator = this.pool.iterator();
		int edgeNum = 0, vertexNum = this.pool.size();
		int eDelta = 0, vDelta = 0;
		int maxChildVertex = 0;
		
		while (instIterator.hasNext()) {
			InstNode curInst = instIterator.next();
			curInst.removeRelatedObj();
			
			int childNum = curInst.getChildFreqMap().size();
			if (curInst instanceof MethodNode) {
				MethodNode mn = (MethodNode) curInst;
				//HashMap<GraphTemplate, Double> repCallees = MethodNode.extractCallee(mn.getCallees(), mn.getMaxCalleeFreq());
				HashMap<GraphTemplate, Double> repCallees = MethodNode.extractCallee(mn);
				
				int instParentNum = mn.getInstDataParentList().size();
				int controlParentNum = mn.getControlParentList().size();
				for (GraphTemplate repCallee: repCallees.keySet()) {
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
				
		gt.setEdgeNum(edgeNum);
		gt.setVertexNum(vertexNum);
		gt.setChildDominant(maxChildVertex);
		gt.calleeRequired = calleeRequired;		
		gt.setInstPool(this.pool);
		
		String dumpKey = StringUtil.genKeyWithId(this.shortMethodKey, String.valueOf(this.threadId));
		
		if (this.isStatic) {
			CumuGraphRecorder.registerStaticGraph(this.methodKey, gt);
		} else {
			CumuGraphRecorder.registerObjGraph(this.objId, gt);
		}
				
		//this.showStackSimulator();
		/*logger.info("Leave " + 
				" " + this.methodKey + 
				" " + this.threadId + 
				" " + this.threadMethodId);*/
	}
}

