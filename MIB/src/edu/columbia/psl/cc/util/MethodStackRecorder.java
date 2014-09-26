package edu.columbia.psl.cc.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.pojo.OpcodeObj;

public class MethodStackRecorder {
	
	private AtomicInteger instCounter = new AtomicInteger();

	private Stack<String> stackSimulator = new Stack<String>();
	
	private List<String> controlVars = new ArrayList<String>();
	
	private Map<Integer, String> localVarRecorder = new HashMap<Integer, String>();
	
	private Map<String, List<String>> dataDep = new HashMap<String, List<String>>();
	
	private Map<String, List<String>> controlDep = new HashMap<String, List<String>>();
	
	private synchronized int getInstIdx() {
		return this.instCounter.getAndIncrement();
	}
	
	private void updateCachedMap(String parent, String child, boolean isControl) {
		if (isControl) {
			if (this.controlDep.containsKey(parent)) {
				this.controlDep.get(parent).add(child);
			} else {
				List<String> children = new ArrayList<String>();
				children.add(child);
				this.controlDep.put(parent, children);
			}
		} else {
			if (this.dataDep.containsKey(parent)) {
				this.dataDep.get(parent).add(child);
			} else {
				List<String> children = new ArrayList<String>();
				children.add(child);
				this.dataDep.put(parent, children);
			}
		}
		System.out.println("Update map: " + this.dataDep);
	}
	
	private synchronized String safePop() {
		if (this.stackSimulator.size() > 0) {
			return this.stackSimulator.pop();
		}
		return null;
	}
	
	public void handleOpcode(int opcode, String addInfo) {
		OpcodeObj oo = BytecodeCategory.getOpcodeObj(opcode);
		int opcat = oo.getCatId();
		String fullInst = this.getInstIdx() + " " + oo.getInstruction() + " " + addInfo;
		
		this.updateControlRelation(fullInst);
		
		if (BytecodeCategory.controlCategory().contains(opcat))
			this.controlVars.add(fullInst);
		
		int inputSize = oo.getInList().size();
		if (inputSize > 0) {
			for (int i = 0; i < inputSize; i++) {
				//Should not return null here
				String tmpInst = this.safePop();
				this.updateCachedMap(tmpInst, fullInst, false);
			}
		}
		this.updateStackSimulator(oo, fullInst);
	}
	
	public void handleOpcode(int opcode, int localVarIdx) {
		OpcodeObj oo =BytecodeCategory.getOpcodeObj(opcode);
		int opcat = oo.getCatId();
		
		String fullInst = this.getInstIdx() + " " + oo.getInstruction() + " ";
		if (localVarIdx >= 0) {
			fullInst += localVarIdx;
		}
		
		String lastInst = "";
		if (!stackSimulator.isEmpty()) {
			lastInst = stackSimulator.peek();
		}
		
		this.updateControlRelation(fullInst);
		
		System.out.println("Check lastInst: " + lastInst);
		//The store instruction will be the sink. The inst on the stack will be source
		if (BytecodeCategory.writeCategory().contains(opcat)) {
			if (lastInst.length() > 0) {
				System.out.println("Update data dep");
				if (localVarIdx >= 0)
					this.localVarRecorder.put(localVarIdx, fullInst);
				
				this.updateCachedMap(lastInst, fullInst, false);
				this.safePop();
			}
		} else if (BytecodeCategory.readCategory().contains(opcat)) {
			//Search local var recorder;
			String parentInst = this.localVarRecorder.get(localVarIdx);
			if (parentInst != null)
				this.updateCachedMap(parentInst, fullInst, false);
		} else {
			if (BytecodeCategory.controlCategory().contains(opcat))
				this.controlVars.add(fullInst);
			
			int inputSize = oo.getInList().size();
			if (inputSize > 0) {
				for (int i = 0; i < inputSize; i++) {
					//Should not return null here
					String tmpInst = this.safePop();
					this.updateCachedMap(tmpInst, fullInst, false);
				}
			}
		}
		
		this.updateStackSimulator(oo, fullInst);
	}
	
	public void handleMultiNewArray(String desc, int dim) {
		OpcodeObj oo = BytecodeCategory.getOpcodeObj(Opcodes.MULTIANEWARRAY);
		String fullInst = this.getInstIdx() + " " + oo.getInstruction() + " " + desc + " " + dim;
		
		this.updateControlRelation(fullInst);
		
		//Parse method type
		for (int i = 0; i < dim; i++) {
			String tmpInst = this.safePop();
			this.updateCachedMap(tmpInst, fullInst, false);
		}
		this.updateStackSimulator(oo, fullInst);
	}
	
	public void handleMethod(int opcode, String owner, String name, String desc) {
		OpcodeObj oo = BytecodeCategory.getOpcodeObj(opcode);
		String fullInst = this.getInstIdx() + " " + oo.getInstruction() + " " + owner + " " + name + " " + desc;
		
		this.updateControlRelation(fullInst);
		
		Type methodType = Type.getMethodType(desc);
		int argSize = methodType.getArgumentTypes().length;
		String returnType = methodType.getReturnType().getDescriptor();
		for (int i = 0; i < argSize; i++) {
			String tmpInst = this.safePop();
			this.updateCachedMap(tmpInst, fullInst, false);
		}
		
		if (!returnType.equals("V")) {
			this.updateStackSimulator(1, fullInst);
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
		System.out.println("Check data dep: " + this.dataDep);
		System.out.println("Check stack simulator: " + this.stackSimulator);
	}
	
	private void updateControlRelation(String fullInst) {
		//Construct control relations
		for (String control: this.controlVars) {
			this.updateCachedMap(control, fullInst, true);
		}
	}
	
	public void dumpGraph() {
		System.out.println("Data dependency:");
		for (String parent: this.dataDep.keySet()) {
			System.out.println("Source: " + parent);
			for (String childInst: this.dataDep.get(parent)) {
				System.out.println("	Sink: " + childInst);
			}
		}
		
		System.out.println("Control dependency:");
		for (String parent: this.controlDep.keySet()) {
			System.out.println("Source: " + parent);
			for (String childInst: this.controlDep.get(parent)) {
				System.out.println("	Sinke: " + childInst);
			}
		}
	}

}
