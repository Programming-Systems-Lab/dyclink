package edu.columbia.psl.cc.analysis;

import java.sql.Types;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.ReducedNode;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.util.GraphUtil;
import edu.columbia.psl.cc.util.StringUtil;

public class GraphReducer {
	
	private static Logger logger = Logger.getLogger(GraphReducer.class);
	
	private static HashSet<Integer> replacedOps = BytecodeCategory.replacedOps();
	
	private static HashSet<Integer> methodOps = BytecodeCategory.methodOps();
	
	private GraphTemplate theGraph;
	
	public GraphReducer(GraphTemplate theGraph) {
		this.theGraph = theGraph;
	}
	
	public static int parseSort(String addInfo) {
		String[] first = addInfo.split(":");
		//System.out.println("First: " + first[0]);
		String[] second = first[0].split("\\.");
		//System.out.println("Second: " + second[second.length - 1]);
		String valueType = second[second.length - 1];
		int typeSort = Type.getType(valueType).getSort();
		return typeSort;
	}
	
	public static OpcodeObj parseMethodSort(String methodInfo) {
		String[] parsed = methodInfo.split(":");
		String returnType = parsed[parsed.length - 1];
		
		if (returnType.equals("Z") 
				|| returnType.equals("B") 
				|| returnType.equals("C") 
				|| returnType.equals("S") 
				|| returnType.equals("I")) {
			return BytecodeCategory.getOpcodeObj(Opcodes.ILOAD);
		} else if (returnType.equals("J")) {
			return BytecodeCategory.getOpcodeObj(Opcodes.LLOAD);
		} else if (returnType.equals("F")) {
			return BytecodeCategory.getOpcodeObj(Opcodes.FLOAD);
		} else if (returnType.equals("D")) {
			return BytecodeCategory.getOpcodeObj(Opcodes.DLOAD);
		} else {
			return BytecodeCategory.getOpcodeObj(Opcodes.ALOAD);
		}
	}
	
	public void reduceGraph() {
		InstPool pool = this.theGraph.getInstPool();
		HashSet<String> readVars = this.theGraph.getFirstReadLocalVars();
		List<InstNode> sortedPool = GraphUtil.sortInstPool(pool, true);
		
		HashSet<InstNode> toRemove = new HashSet<InstNode>();
		for (int i = sortedPool.size() - 1; i >=0; i--) {
			InstNode inst = sortedPool.get(i);
			int opcode = inst.getOp().getOpcode();
			HashSet<Integer> inheritedInfo = new HashSet<Integer>();
			
			boolean needJump = false;
			boolean shouldReplace = replacedOps.contains(opcode);
			//boolean shouldReplaceMethod = methodOps.contains(opcode) && StringUtil.shouldIncludeClass(inst.getAddInfo());
			if (shouldReplace && !toRemove.contains(inst)) {
				OpcodeObj reduceOp = null;
				if (opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC) {
					int typeSort = parseSort(inst.getAddInfo());
					if (typeSort == Type.ARRAY || typeSort == Type.OBJECT) {
						reduceOp = BytecodeCategory.getOpcodeObj(Opcodes.ALOAD);
					} else if (typeSort == Type.BOOLEAN || typeSort == Type.BYTE || typeSort == Type.CHAR || typeSort == Type.INT || typeSort == Type.SHORT) {
						reduceOp = BytecodeCategory.getOpcodeObj(Opcodes.ILOAD);
					} else if (typeSort == Type.LONG) {
						reduceOp = BytecodeCategory.getOpcodeObj(Opcodes.LLOAD);
					} else if (typeSort == Type.DOUBLE) {
						reduceOp = BytecodeCategory.getOpcodeObj(Opcodes.DLOAD);
					} else if (typeSort == Type.FLOAT) {
						reduceOp = BytecodeCategory.getOpcodeObj(Opcodes.FLOAD);
					} else {
						logger.error("Uncategorized type: " + inst.getAddInfo() + " " + typeSort);
						System.exit(-1);
					}
				} else if (opcode == Opcodes.AALOAD) {
					reduceOp = BytecodeCategory.getOpcodeObj(Opcodes.ALOAD);
				} else if (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC) {
					int typeSort = parseSort(inst.getAddInfo());
					if (typeSort == Type.ARRAY || typeSort == Type.OBJECT) {
						reduceOp = BytecodeCategory.getOpcodeObj(Opcodes.ASTORE);
					} else if (typeSort == Type.BOOLEAN || typeSort == Type.BYTE || typeSort == Type.CHAR || typeSort == Type.INT || typeSort == Type.SHORT) {
						reduceOp = BytecodeCategory.getOpcodeObj(Opcodes.ISTORE);
					} else if (typeSort == Type.LONG) {
						reduceOp = BytecodeCategory.getOpcodeObj(Opcodes.LSTORE);
					} else if (typeSort == Type.DOUBLE) {
						reduceOp = BytecodeCategory.getOpcodeObj(Opcodes.DSTORE);
					} else if (typeSort == Type.FLOAT) {
						reduceOp = BytecodeCategory.getOpcodeObj(Opcodes.FSTORE);
					} else {
						logger.error("Upcategorized type: " + inst.getAddInfo() + " " + typeSort);
						System.exit(-1);
					}
					needJump = true;
				} else if (opcode == Opcodes.AASTORE) {
					reduceOp = BytecodeCategory.getOpcodeObj(Opcodes.ASTORE);
					needJump = true;
				} /*else if (shouldReplaceMethod) {
					reduceOp = parseMethodSort(inst.getAddInfo());
				}*/
				
				inst.originalOp = inst.getOp();
				inst.setOp(reduceOp);
				
				if (!needJump) {
					this.collectFamily(inst, toRemove, readVars, inheritedInfo, false);
				} else {
					InstNode parentLoad = null;
					for (String parentKey: inst.getInstDataParentList()) {
						InstNode parentInst = pool.searchAndGet(parentKey);
						
						if (parentInst == null) {
							logger.error("Cannot find parent to reduce graph: " + inst);
							logger.error("Parent key: " + parentKey);
							System.exit(-1);
						}
						
						if (parentLoad == null 
								|| parentInst.getStartTime() < parentLoad.getStartTime()) {
							parentLoad = parentInst;
						}
					}
					this.collectFamily(parentLoad, toRemove, readVars, inheritedInfo, true);
				}
				
				if (inheritedInfo.size() > 0) {
					String instKey = StringUtil.genIdxKey(inst.getThreadId(), inst.getThreadMethodIdx(), inst.getIdx());
					readVars.add(instKey);
					inst.inheritedInfo = inheritedInfo;
				}
			}
		}
		
		//System.out.println("Check remove");
		for (InstNode inst: toRemove) {
			//Remove directly, the jung graph conversion take care of null child
			//System.out.println(inst);
			pool.remove(inst);
		}
	}
	
	private void collectFamily(InstNode inst, 
			HashSet<InstNode> recorder, 
			HashSet<String> readVars, 
			HashSet<Integer> inheritedInfo, 
			boolean removeInst) {
		
		if (removeInst) {
			recorder.add(inst);
			String instKey = StringUtil.genIdxKey(inst.getThreadId(), inst.getThreadMethodIdx(), inst.getIdx());
			if (readVars != null && readVars.contains(instKey)) {
				readVars.remove(instKey);
				int var = Integer.parseInt(inst.getAddInfo());
				inheritedInfo.add(var);
			}
		}
		
		for (String parentKey: inst.getInstDataParentList()) {
			InstNode parentInst = this.theGraph.getInstPool().searchAndGet(parentKey);
			recorder.add(parentInst);
			
			if (readVars != null && readVars.contains(parentKey)) {
				readVars.remove(parentKey);
				int var = Integer.parseInt(parentInst.getAddInfo());
				inheritedInfo.add(var);
			}
			
			this.collectFamily(parentInst, recorder, readVars, inheritedInfo, false);
		}
	}
	
	public GraphTemplate getGraph() {
		return this.theGraph;
	}
	
	public static void main(String[] args) {
		String addInfo = "org.ejml.alg.dense.decomposition.svd.SvdImplicitQrDecompose_D64.qralg.Lorg/ejml/alg/dense/decomposition/svd/implicitqr/SvdImplicitQrAlgorithm;:2";
		System.out.println("Add info sort: " + parseSort(addInfo));
		System.out.println("Object sort: " + Type.BOOLEAN);
	}
}
