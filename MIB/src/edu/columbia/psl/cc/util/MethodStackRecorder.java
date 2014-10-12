package edu.columbia.psl.cc.util;

import java.io.File;
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

public class MethodStackRecorder {
		
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
	
	//Key: inst idx, Val: line num, write field and load local vars
	private Map<Integer, ExtObj> extMethods = new HashMap<Integer, ExtObj>();
	
	//Record which insts might be affected by input params
	private HashSet<Integer> firstReadFields = new HashSet<Integer>();
	
	private HashSet<String> stopReadFields = new HashSet<String>();
	
	//Record which insts might be affecte by field written by parent method
	private HashSet<Integer> firstReadLocalVars = new HashSet<Integer>();
	
	private HashSet<Integer> shouldRecordReadLocalVars = new HashSet<Integer>();
	
	private int curMethod = -1;
	
	private ExtObj returnEo = new ExtObj();
	
	private List<InstNode> path = new ArrayList<InstNode>();
	
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
			start = 1;
		}
		
		while (count < methodArgSize) {
			this.shouldRecordReadLocalVars.add(start);
			start++;
			count++;
		}
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
	
	private void updateExtMethods(int idx, ExtObj eo) {
		this.extMethods.put(idx, eo);
		this.curMethod = idx;
	}
		
	private void updatePath(InstNode fullInst) {
		this.path.add(fullInst);
	}
	
	private void updateCachedMap(InstNode parent, InstNode child, boolean isControl) {
		if (isControl) {
			parent.increChild(child.getFromMethod(), child.getIdx(), MIBConfiguration.getControlWeight());
			child.registerParent(parent.getFromMethod(), parent.getIdx(), isControl);
		} else {
			parent.increChild(child.getFromMethod(), child.getIdx(), MIBConfiguration.getDataWeight());
			child.registerParent(parent.getFromMethod(), parent.getIdx(), isControl);		
		}
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
	
	public void handleLdc(int opcode, int instIdx, int times, String addInfo) {
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, instIdx, opcode, addInfo);
		
		this.updateControlRelation(fullInst);
		this.updatePath(fullInst);
		
		this.updateStackSimulator(times, fullInst);
		this.showStackSimulator();
	}
	
	public void handleOpcode(int opcode, int instIdx, String addInfo) {
		OpcodeObj oo = BytecodeCategory.getOpcodeObj(opcode);
		int opcat = oo.getCatId();
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, instIdx, opcode, addInfo);
		
		this.updateControlRelation(fullInst);
		this.updatePath(fullInst);
		
		if (BytecodeCategory.controlCategory().contains(opcat)) {
			this.updateControlInst(fullInst);
		} else if (BytecodeCategory.readFieldCategory().contains(opcat)) {
			//Add infor for field: owner + name + desc
			InstNode parent = this.fieldRecorder.get(addInfo);
			if (parent != null)
				this.updateCachedMap(parent, fullInst, false);
			
			if (curMethod >= 0) {
				this.extMethods.get(curMethod).addAffFieldInst(fullInst);
			}
			
			this.updateReadField(fullInst);
		} else if (BytecodeCategory.writeFieldCategory().contains(opcat)) {
			this.fieldRecorder.put(addInfo, fullInst);
			this.stopReadField(addInfo);
		}
		
		int inputSize = oo.getInList().size();
		if (inputSize > 0) {
			for (int i = 0; i < inputSize; i++) {
				//Should not return null here
				InstNode tmpInst = this.safePop();
				this.updateCachedMap(tmpInst, fullInst, false);
			}
		}
		this.updateStackSimulator(fullInst);
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
		
		int opcat = fullInst.getOp().getCatId();
		
		InstNode lastInst = null;
		if (!stackSimulator.isEmpty()) {
			lastInst = stackSimulator.peek();
		}
		
		this.updateControlRelation(fullInst);
		this.updatePath(fullInst);
		
		System.out.println("Check lastInst: " + lastInst);
		//The store instruction will be the sink. The inst on the stack will be source
		boolean hasUpdate = false;
		if (BytecodeCategory.writeCategory().contains(opcat)) {
			if (lastInst != null) {
				System.out.println("Update data dep");
				if (localVarIdx >= 0)
					this.localVarRecorder.put(localVarIdx, fullInst);
				
				this.updateCachedMap(lastInst, fullInst, false);
				for (int i = 0; i < fullInst.getOp().getInList().size(); i++)
					this.safePop();
			}
			this.stopLocalVar(localVarIdx);
		} else if (opcode == Opcodes.IINC) {
			InstNode parentInst = this.localVarRecorder.get(localVarIdx);
			if (parentInst != null)
				this.updateCachedMap(parentInst, fullInst, false);
			
			this.localVarRecorder.put(localVarIdx, fullInst);
			this.updateReadLocalVar(fullInst);
			this.stopLocalVar(localVarIdx);
		} else if (BytecodeCategory.readCategory().contains(opcat)) {
			//Search local var recorder;
			InstNode parentInst = this.localVarRecorder.get(localVarIdx);
			if (parentInst != null)
				this.updateCachedMap(parentInst, fullInst, false);
			
			this.updateReadLocalVar(fullInst);
		} else if (BytecodeCategory.dupCategory().contains(opcat)) {
			this.handleDup(opcode);
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
						this.updateCachedMap(tmpInst, fullInst, false);
					
					lastTmp = tmpInst;
				}
			}
		}
		
		if (!hasUpdate) 
			this.updateStackSimulator(fullInst);
		this.showStackSimulator();
	}
	
	public void handleMultiNewArray(String desc, int dim, int instIdx) {
		String addInfo = desc + " " + dim;
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, instIdx, Opcodes.MULTIANEWARRAY, addInfo);
		
		this.updateControlRelation(fullInst);
		this.updatePath(fullInst);
		
		//Parse method type
		for (int i = 0; i < dim; i++) {
			InstNode tmpInst = this.safePop();
			this.updateCachedMap(tmpInst, fullInst, false);
		}
		this.updateStackSimulator(fullInst);
		this.showStackSimulator();
	}
	
	public void handleMethod(int opcode, int instIdx, int linenum, String owner, String name, String desc) {
		//String addInfo = owner + "." + name + "." + desc;
		String addInfo = StringUtil.genKey(owner, name, desc);
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, instIdx, opcode, addInfo);
		System.out.println("Method full inst: " + fullInst);
		
		ExtObj eo = new ExtObj();
		this.updateControlRelation(fullInst);
		this.updatePath(fullInst);
		
		eo.setLineNumber(linenum);
		if (this.fieldRecorder.size() > 0) {
			for (InstNode inst: this.fieldRecorder.values()) {
				eo.addWriteFieldInst(inst);
			}
		}
		
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
		this.updateExtMethods(instIdx, eo);
		
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
		this.showStackSimulator();
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
	
	private void updateStackSimulator(InstNode fullInst) {
		int outputSize = fullInst.getOp().getOutList().size();
		this.updateStackSimulator(outputSize, fullInst);
	}
	
	private void updateStackSimulator(int times, InstNode fullInst) {
		System.out.println("Stack push: " + fullInst);
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
		if (this.curControlInst != null 
				&& !BytecodeCategory.dupCategory().contains(fullInst.getOp().getCatId())) {
			//Exclude dup, because they are not contained in data dep either
			this.updateCachedMap(this.curControlInst, fullInst, true);
			/*for (InstNode curControlInst: this.curControlInsts) {
				this.updateCachedMap(curControlInst, fullInst, true);
			}*/
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
