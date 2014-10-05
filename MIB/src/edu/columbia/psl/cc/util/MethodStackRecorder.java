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
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.pojo.StaticRep;

public class MethodStackRecorder {
	
	private String className;
	
	private String methodName;
	
	private String methodDesc;
	
	private String methodKey;
		
	private Stack<InstNode> stackSimulator = new Stack<InstNode>();
	
	private InstNode curControlInst = null;
	
	private Map<Integer, InstNode> localVarRecorder = new HashMap<Integer, InstNode>();
	
	private Map<String, InstNode> fieldRecorder = new HashMap<String, InstNode>();
	
	private List<Integer> extMethods = new ArrayList<Integer>();
	
	private List<InstNode> path = new ArrayList<InstNode>();
	
	private InstPool pool = new InstPool();
	
	public MethodStackRecorder(String className, String methodName, String methodDesc) {
		this.className = className;
		this.methodName = methodName;
		this.methodDesc = methodDesc;
		this.methodKey = StringUtil.genKey(className, methodName, methodDesc);
	}
	
	/*public String genInstHead(OpcodeObj oo, String label) {
		return label + " " + this.getInstIdx(label) + " " + oo.getOpcode() + " " + oo.getInstruction();
	}*/
	
	private void updateExtMethods(int idx) {
		this.extMethods.add(idx);
	}
		
	private void updatePath(InstNode fullInst) {
		this.path.add(fullInst);
	}
	
	private void updateCachedMap(InstNode parent, InstNode child, boolean isControl) {
		if (isControl) {
			parent.increChild(child.getFromMethod(), child.getIdx(), MIBConfiguration.getControlWeight());
			child.registerParent(parent.getFromMethod(), parent.getIdx());
		} else {
			parent.increChild(child.getFromMethod(), child.getIdx(), MIBConfiguration.getDataWeight());
			child.registerParent(parent.getFromMethod(), parent.getIdx());		
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
		} else if (BytecodeCategory.writeFieldCategory().contains(opcat)) {
			this.fieldRecorder.put(addInfo, fullInst);
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
				this.safePop();
			}
		} else if (opcode == Opcodes.IINC) {
			InstNode parentInst = this.localVarRecorder.get(localVarIdx);
			if (parentInst != null)
				this.updateCachedMap(parentInst, fullInst, false);
			
				this.localVarRecorder.put(localVarIdx, fullInst);
		} else if (BytecodeCategory.readCategory().contains(opcat)) {
			//Search local var recorder;
			InstNode parentInst = this.localVarRecorder.get(localVarIdx);
			if (parentInst != null)
				this.updateCachedMap(parentInst, fullInst, false);
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
	
	public void handleMethod(int opcode, int instIdx, String owner, String name, String desc) {
		//String addInfo = owner + "." + name + "." + desc;
		String addInfo = StringUtil.genKey(owner, name, desc);
		InstNode fullInst = this.pool.searchAndGet(this.methodKey, instIdx, opcode, addInfo);
		System.out.println("Method full inst: " + fullInst);
		
		this.updateControlRelation(fullInst);
		this.updatePath(fullInst);
		this.updateExtMethods(instIdx);
		
		Type methodType = Type.getMethodType(desc);
		//+1 for object reference, if instance method
		int argSize = methodType.getArgumentTypes().length;
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
			this.updateStackSimulator(1, fullInst);
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
		
		Type methodType = Type.getMethodType(this.methodDesc);
		int argSize = methodType.getArgumentTypes().length;
		int returnSize = 1;
		if (methodType.getReturnType().getDescriptor().equals("V")) {
			returnSize = 0;
		}
		gt.setMethodKey(this.methodKey);
		gt.setMethodArgSize(argSize);
		gt.setMethodReturnSize(returnSize);
		gt.setExtMethods(this.extMethods);
		
		InstNode lastSecondInst = null;
		if (this.path.size() > 1) {
			lastSecondInst = this.path.get(this.path.size() - 2);
		}
		gt.setLastSecondInst(lastSecondInst);
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
