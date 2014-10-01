package edu.columbia.psl.cc.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.pojo.StaticRep;

public class MethodStackRecorder {
	
	private String curLabel = null;
	
	private AtomicInteger instCounter = null;

	private Stack<String> stackSimulator = new Stack<String>();
	
	private String curControlVar = null;
	
	private Map<Integer, String> localVarRecorder = new HashMap<Integer, String>();
	
	private HashMap<String, TreeSet<String>> dataDep = new HashMap<String, TreeSet<String>>();
	
	private HashMap<String, TreeSet<String>> controlDep = new HashMap<String, TreeSet<String>>();
	
	private HashMap<String, ArrayList<String>> invokeMethodLookup = new HashMap<String, ArrayList<String>>();
	
	private List<String> path = new ArrayList<String>();
	
	private synchronized int getInstIdx(String label) {
		if (curLabel == null || !curLabel.equals(label)) {
			this.curLabel = label;
			this.instCounter = new AtomicInteger();
			return this.instCounter.getAndIncrement();
		}
		
		return this.instCounter.getAndIncrement();
	}
	
	public String genInstHead(OpcodeObj oo, String label) {
		return label + " " + this.getInstIdx(label) + " " + oo.getOpcode() + " " + oo.getInstruction();
	}
	
	private void updatePath(String fullInst) {
		this.path.add(fullInst);
	}
	
	private void updateInvokeMethod(String invokeMethod, String parentInst) {
		if (this.invokeMethodLookup.containsKey(invokeMethod)) {
			this.invokeMethodLookup.get(invokeMethod).add(parentInst);
		} else {
			ArrayList<String> parents = new ArrayList<String>();
			parents.add(parentInst);
			this.invokeMethodLookup.put(invokeMethod, parents);
		}
	}
	
	private void updateCachedMap(String parent, String child, boolean isControl) {
		if (isControl) {
			if (this.controlDep.containsKey(parent)) {
				this.controlDep.get(parent).add(child);
			} else {
				TreeSet<String> children = new TreeSet<String>();
				children.add(child);
				this.controlDep.put(parent, children);
			}
		} else {
			if (this.dataDep.containsKey(parent)) {
				this.dataDep.get(parent).add(child);
			} else {
				TreeSet<String> children = new TreeSet<String>();
				children.add(child);
				this.dataDep.put(parent, children);
			}
		}
		//System.out.println("Update map: " + this.dataDep);
	}
	
	private synchronized String safePop() {
		if (this.stackSimulator.size() > 0) {
			return this.stackSimulator.pop();
		}
		return null;
	}
	
	private synchronized void updateControlVar(String fullInst) {
		this.curControlVar = fullInst;
		
		/*if (this.controlVars.size() == 0) {
			this.controlVars.add(fullInst);
		} else {
			this.controlVars.remove(this.controlVars.size() - 1);
			this.controlVars.add(fullInst);
		}*/
	}
	
	public void handleOpcode(int opcode, String label, String addInfo) {
		OpcodeObj oo = BytecodeCategory.getOpcodeObj(opcode);
		int opcat = oo.getCatId();
		String fullInst = this.genInstHead(oo, label) + " " + addInfo;
		
		this.updateControlRelation(fullInst);
		this.updatePath(fullInst);
		
		if (BytecodeCategory.controlCategory().contains(opcat)) {
			this.updateControlVar(fullInst);
		}
		
		int inputSize = oo.getInList().size();
		if (inputSize > 0) {
			for (int i = 0; i < inputSize; i++) {
				//Should not return null here
				String tmpInst = this.safePop();
				this.updateCachedMap(tmpInst, fullInst, false);
			}
		}
		this.updateStackSimulator(oo, fullInst);
		this.showStackSimulator();
	}
	
	/**
	 * localVarIdx is not necessarily a local var
	 * @param opcode
	 * @param localVarIdx
	 */
	public void handleOpcode(int opcode, String label, int localVarIdx) {
		System.out.println("Handling now: " + opcode + " " + localVarIdx);
		OpcodeObj oo =BytecodeCategory.getOpcodeObj(opcode);
		int opcat = oo.getCatId();
		
		String fullInst = this.genInstHead(oo, label);
		if (localVarIdx >= 0) {
			fullInst = fullInst + " " + localVarIdx;
		}
		
		String lastInst = "";
		if (!stackSimulator.isEmpty()) {
			lastInst = stackSimulator.peek();
		}
		
		this.updateControlRelation(fullInst);
		this.updatePath(fullInst);
		
		System.out.println("Check lastInst: " + lastInst);
		//The store instruction will be the sink. The inst on the stack will be source
		boolean hasUpdate = false;
		if (BytecodeCategory.writeCategory().contains(opcat)) {
			if (lastInst.length() > 0) {
				System.out.println("Update data dep");
				if (localVarIdx >= 0)
					this.localVarRecorder.put(localVarIdx, fullInst);
				
				this.updateCachedMap(lastInst, fullInst, false);
				this.safePop();
			}
		} else if (opcode == Opcodes.IINC) {
			this.localVarRecorder.put(localVarIdx, fullInst);
		}else if (BytecodeCategory.readCategory().contains(opcat)) {
			//Search local var recorder;
			String parentInst = this.localVarRecorder.get(localVarIdx);
			if (parentInst != null)
				this.updateCachedMap(parentInst, fullInst, false);
		} else if (BytecodeCategory.dupCategory().contains(opcat)) {
			this.handleDup(opcode);
			hasUpdate = true;
		} else {
			if (BytecodeCategory.controlCategory().contains(opcat))
				this.updateControlVar(fullInst);
			
			int inputSize = oo.getInList().size();
			String lastTmp = "";
			if (inputSize > 0) {
				for (int i = 0; i < inputSize; i++) {
					//Should not return null here
					String tmpInst = this.safePop();
					if (!tmpInst.equals(lastTmp))
						this.updateCachedMap(tmpInst, fullInst, false);
					
					lastTmp = tmpInst;
				}
			}
		}
		
		if (!hasUpdate) 
			this.updateStackSimulator(oo, fullInst);
		this.showStackSimulator();
	}
	
	public void handleMultiNewArray(String desc, int dim, String label) {
		OpcodeObj oo = BytecodeCategory.getOpcodeObj(Opcodes.MULTIANEWARRAY);
		String fullInst = this.genInstHead(oo, label) + " " + desc + " " + dim;
		
		this.updateControlRelation(fullInst);
		this.updatePath(fullInst);
		
		//Parse method type
		for (int i = 0; i < dim; i++) {
			String tmpInst = this.safePop();
			this.updateCachedMap(tmpInst, fullInst, false);
		}
		this.updateStackSimulator(oo, fullInst);
		this.showStackSimulator();
	}
	
	public void handleMethod(int opcode, String label, String owner, String name, String desc) {
		OpcodeObj oo = BytecodeCategory.getOpcodeObj(opcode);
		String fullInst = this.genInstHead(oo, label) + " " + owner + " " + name + " " + desc;
		String methodKey = StringUtil.genKey(owner, name, desc);
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
			String tmpInst = this.safePop();
			this.updateCachedMap(tmpInst, fullInst, false);
			this.updateInvokeMethod(methodKey, tmpInst);
		}
		
		if (!returnType.equals("V")) {
			this.updateStackSimulator(1, fullInst);
		}
		this.showStackSimulator();
	}
	
	public void handleDup(int opcode) {
		String dupString = "";
		String dupString2 = "";
		Stack<String> stackBuf;
		switch (opcode) {
			case 89:
				dupString = this.stackSimulator.peek();
				this.stackSimulator.push(dupString);
				break ;
			case 90:
				dupString = this.stackSimulator.peek();
				stackBuf = new Stack<String>();
				for (int i = 0; i < 2; i++) {
					stackBuf.push(this.safePop());
				}
				
				this.stackSimulator.push(dupString);
				while(!stackBuf.isEmpty()) {
					this.stackSimulator.push(stackBuf.pop());
				}
				break ;
			case 91:
				dupString = this.stackSimulator.peek();
				stackBuf = new Stack<String>();
				for (int i = 0; i < 3; i++) {
					stackBuf.push(this.safePop());
				}
				
				this.stackSimulator.push(dupString);
				//Should only push three times
				while (!stackBuf.isEmpty()) {
					this.stackSimulator.push(stackBuf.pop());
				}
				break ;
			case 92:
				dupString = this.stackSimulator.get(this.stackSimulator.size() - 1);
	 			dupString2 = this.stackSimulator.get(this.stackSimulator.size() - 2);
	 			
	 			this.stackSimulator.push(dupString2);
	 			this.stackSimulator.push(dupString);
	 			break ;
			case 93:
				dupString = this.stackSimulator.get(this.stackSimulator.size() - 1);
	 			dupString2 = this.stackSimulator.get(this.stackSimulator.size() - 2);
	 			stackBuf = new Stack<String>();
	 			for (int i = 0; i < 3; i++) {
	 				stackBuf.push(this.safePop());
	 			}
	 			
	 			this.stackSimulator.push(dupString2);
	 			this.stackSimulator.push(dupString);
	 			while (!stackBuf.isEmpty()) {
	 				this.stackSimulator.push(stackBuf.pop());
	 			}
	 			break ;
			case 94:
				dupString = this.stackSimulator.get(this.stackSimulator.size() - 1);
	 			dupString2 = this.stackSimulator.get(this.stackSimulator.size() - 2);
	 			stackBuf = new Stack<String>();
	 			for (int i =0 ; i < 4; i++) {
	 				stackBuf.push(this.safePop());
	 			}
	 			
	 			this.stackSimulator.push(dupString2);
	 			this.stackSimulator.push(dupString);
	 			while (!stackBuf.isEmpty()) {
	 				this.stackSimulator.push(stackBuf.pop());
	 			}
	 			break ;
		}
	}
	
	private void updateStackSimulator(OpcodeObj oo, String fullInst) {
		int outputSize = oo.getOutList().size();
		this.updateStackSimulator(outputSize, fullInst);
	}
	
	private void updateStackSimulator(int times, String fullInst) {
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
	
	private void updateControlRelation(String fullInst) {
		//Construct control relations
		if (this.curControlVar != null)
			this.updateCachedMap(this.curControlVar, fullInst, true);
		/*for (String control: this.controlVars) {
			this.updateCachedMap(control, fullInst, true);
		}*/
	}
	
	private void processInvokeMethodLookup(HashMap<String, Integer> labelMap) {
		for (String mkey: this.invokeMethodLookup.keySet()) {
			ArrayList<String> parents = this.invokeMethodLookup.get(mkey);
			
			for (int i = 0; i < parents.size(); i++) {
				String newParent = StringUtil.replaceLabel(parents.get(i), labelMap);
				parents.set(i, newParent);
			}
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
		this.processInvokeMethodLookup(labelMap);
		gt.setInvokeMethodLookup(this.invokeMethodLookup);
		
		String lastSecondInst = "";
		if (this.path.size() > 1) {
			lastSecondInst = StringUtil.replaceLabel(this.path.get(this.path.size() - 2), labelMap);
		}
		gt.setLastSecondInst(lastSecondInst);
		
		System.out.println("Data dependency:");
		TreeMap<String, TreeSet<String>> dataGraph = new TreeMap<String, TreeSet<String>>();
		int dataDepCount = 0;
		for (String parent: this.dataDep.keySet()) {
			String newParent = StringUtil.replaceLabel(parent, labelMap);
			System.out.println("Source: " + newParent);
			TreeSet<String> children = new TreeSet<String>();
			for (String childInst: this.dataDep.get(parent)) {
				String newChild = StringUtil.replaceLabel(childInst, labelMap);
				children.add(newChild);
				System.out.println("	Sink: " + newChild);
				dataDepCount++;
			}
			dataGraph.put(newParent, children);
		}
		System.out.println("Total data dependency: " + dataDepCount);
		gt.setDataGraph(dataGraph);
		
		System.out.println("Control dependency:");
		TreeMap<String, TreeSet<String>> controlGraph = new TreeMap<String, TreeSet<String>>();
		int controlDepCount = 0;
		for (String parent: this.controlDep.keySet()) {
			String newParent = StringUtil.replaceLabel(parent, labelMap);
			System.out.println("Source: " + newParent);
			TreeSet<String> children = new TreeSet<String>();
			for (String childInst: this.controlDep.get(parent)) {
				String newChild = StringUtil.replaceLabel(childInst, labelMap);
				children.add(newChild);
				System.out.println("	Sink: " + newChild);
				controlDepCount++;
			}
			controlGraph.put(newParent, children);			
		}
		System.out.println("Total control dependency: " + controlDepCount);
		gt.setControlGraph(controlGraph);
		
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
