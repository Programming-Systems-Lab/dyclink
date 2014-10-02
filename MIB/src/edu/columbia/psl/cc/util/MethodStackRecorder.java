package edu.columbia.psl.cc.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
		
	private Stack<InstNode> stackSimulator = new Stack<InstNode>();
	
	private InstNode curControlInst = null;
	
	//private HashSet<InstNode> curControlInsts = new HashSet<InstNode>(); 
	
	private Map<Integer, InstNode> localVarRecorder = new HashMap<Integer, InstNode>();
	
	private TreeMap<InstNode, TreeSet<InstNode>> dataDep = new TreeMap<InstNode, TreeSet<InstNode>>();
	
	private TreeMap<InstNode, TreeSet<InstNode>> controlDep = new TreeMap<InstNode, TreeSet<InstNode>>();
	
	private HashMap<InstNode, ArrayList<InstNode>> invokeMethodLookup = new HashMap<InstNode, ArrayList<InstNode>>();
	
	private List<InstNode> path = new ArrayList<InstNode>();
	
	private InstPool pool = new InstPool();
	
	/*public String genInstHead(OpcodeObj oo, String label) {
		return label + " " + this.getInstIdx(label) + " " + oo.getOpcode() + " " + oo.getInstruction();
	}*/
		
	private void updatePath(InstNode fullInst) {
		this.path.add(fullInst);
	}
	
	private void updateInvokeMethod(InstNode invokeMethod, InstNode parentInst) {
		if (this.invokeMethodLookup.containsKey(invokeMethod)) {
			this.invokeMethodLookup.get(invokeMethod).add(parentInst);
		} else {
			ArrayList<InstNode> parents = new ArrayList<InstNode>();
			parents.add(parentInst);
			this.invokeMethodLookup.put(invokeMethod, parents);
		}
	}
	
	private void updateCachedMap(InstNode parent, InstNode child, boolean isControl) {
		if (isControl) {
			if (this.controlDep.containsKey(parent)) {
				this.controlDep.get(parent).add(child);
			} else {
				TreeSet<InstNode> children = new TreeSet<InstNode>();
				children.add(child);
				this.controlDep.put(parent, children);
			}
		} else {
			if (this.dataDep.containsKey(parent)) {
				this.dataDep.get(parent).add(child);
			} else {
				TreeSet<InstNode> children = new TreeSet<InstNode>();
				children.add(child);
				this.dataDep.put(parent, children);
			}
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
		InstNode fullInst = this.pool.searchAndGet(instIdx, opcode, addInfo);
		
		this.updateControlRelation(fullInst);
		this.updatePath(fullInst);
		
		if (BytecodeCategory.controlCategory().contains(opcat)) {
			this.updateControlInst(fullInst);
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
			fullInst = this.pool.searchAndGet(instIdx, opcode, String.valueOf(localVarIdx));
		} else {
			fullInst = this.pool.searchAndGet(instIdx, opcode, "");
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
		}else if (BytecodeCategory.readCategory().contains(opcat)) {
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
		InstNode fullInst = this.pool.searchAndGet(instIdx, Opcodes.MULTIANEWARRAY, addInfo);
		
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
		String addInfo = owner + " " + name + " " + desc;
		InstNode fullInst = this.pool.searchAndGet(instIdx, opcode, addInfo);
		//String methodKey = StringUtil.genKey(owner, name, desc);
		System.out.println("Method full inst: " + fullInst);
		
		this.updateControlRelation(fullInst);
		this.updatePath(fullInst);
		
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
			this.updateInvokeMethod(fullInst, tmpInst);
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
	
	private void processInvokeMethodLookup() {
		for (InstNode fullInst: this.invokeMethodLookup.keySet()) {
			ArrayList<InstNode> parents = this.invokeMethodLookup.get(fullInst);
			
			/*for (int i = 0; i < parents.size(); i++) {
				String newParent = StringUtil.replaceLabel(parents.get(i), labelMap);
				parents.set(i, newParent);
			}*/
			Collections.reverse(parents);
		}
	}
	
	public void dumpGraph(String owner, String myName, String myDesc, boolean isTemplate) {
		//Load static map first
		String staticMapKey = MIBConfiguration.getLabelmapDir() + "/" + StringUtil.genKey(owner, myName, myDesc) + "_map.json";
		TypeToken<StaticRep> staticType = new TypeToken<StaticRep>(){};
		File staticFile = new File(staticMapKey);
		StaticRep staticRep = GsonManager.readJsonGeneric(staticFile, staticType);
		HashMap<String, Integer> labelMap = staticRep.getLabelMap();
		System.out.println("MethodStackRecorder: catString: " + staticRep.getOpCatString());
		System.out.println("MethodStackRecorder: catFreq: " + Arrays.toString(staticRep.getOpCatFreq()));
		
		//For serilization
		GraphTemplate gt = new GraphTemplate();
		this.processInvokeMethodLookup();
		gt.setInvokeMethodLookup(this.invokeMethodLookup);
		
		Type methodType = Type.getMethodType(myDesc);
		int argSize = methodType.getArgumentTypes().length;
		int returnSize = 1;
		if (methodType.getReturnType().getDescriptor().equals("V")) {
			returnSize = 0;
		}
		gt.setMethodArgSize(argSize);
		gt.setMethodReturnSize(returnSize);
		
		InstNode lastSecondInst = null;
		if (this.path.size() > 1) {
			lastSecondInst = this.path.get(this.path.size() - 2);
		}
		gt.setLastSecondInst(lastSecondInst);
		
		System.out.println("Data dependency:");
		int dataDepCount = 0;
		for (InstNode parent: this.dataDep.navigableKeySet()) {
			System.out.println("Source: " + parent);
			for (InstNode childInst: this.dataDep.get(parent)) {
				System.out.println("	Sink: " + childInst);
				dataDepCount++;
			}
		}
		System.out.println("Total data dependency: " + dataDepCount);
		gt.setDataGraph(this.dataDep);
		
		System.out.println("Control dependency:");
		int controlDepCount = 0;
		for (InstNode parent: this.controlDep.navigableKeySet()) {
			System.out.println("Source: " + parent);
			for (InstNode childInst: this.controlDep.get(parent)) {
				System.out.println("	Sink: " + childInst);
				controlDepCount++;
			}		
		}
		System.out.println("Total control dependency: " + controlDepCount);
		gt.setControlGraph(this.controlDep);
		
		String key = StringUtil.genKey(owner, myName, myDesc);
		TypeToken<GraphTemplate> typeToken = new TypeToken<GraphTemplate>(){};
		
		if (isTemplate) {
			GsonManager.writeJsonGeneric(gt, key, typeToken, 0);
		} else {
			GsonManager.writeJsonGeneric(gt, key, typeToken, 1);
		}
		GsonManager.writePath(key, this.path);
	}

}
