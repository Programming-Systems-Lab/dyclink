package edu.columbia.psl.cc.analysis;

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

public class GraphReducer {
	
	private static Logger logger = Logger.getLogger(GraphReducer.class);
	
	private static HashSet<Integer> replacedOps = BytecodeCategory.replacedOps();
	
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
	
	public void reduceGraph() {
		InstPool pool = this.theGraph.getInstPool();
		List<InstNode> sortedPool = GraphUtil.sortInstPool(pool, true);
		
		HashSet<InstNode> toRemove = new HashSet<InstNode>();
		for (int i = sortedPool.size() - 1; i >=0; i--) {
			InstNode inst = sortedPool.get(i);
			int opcode = inst.getOp().getOpcode();
			
			if (replacedOps.contains(opcode) && !toRemove.contains(inst)) {
				OpcodeObj reduceOp = null;
				if (opcode == Opcodes.GETFIELD || opcode == Opcodes.GETSTATIC) {
					int typeSort = parseSort(inst.getAddInfo());
					if (typeSort == Type.ARRAY || typeSort == Type.OBJECT) {
						reduceOp = BytecodeCategory.getOpcodeObj(Opcodes.ALOAD);
					} else if (typeSort == Type.BYTE || typeSort == Type.CHAR || typeSort == Type.INT || typeSort == Type.SHORT) {
						reduceOp = BytecodeCategory.getOpcodeObj(Opcodes.ILOAD);
					} else if (typeSort == Type.LONG) {
						reduceOp = BytecodeCategory.getOpcodeObj(Opcodes.LLOAD);
					} else if (typeSort == Type.DOUBLE) {
						reduceOp = BytecodeCategory.getOpcodeObj(Opcodes.DLOAD);
					} else if (typeSort == Type.FLOAT) {
						reduceOp = BytecodeCategory.getOpcodeObj(Opcodes.FLOAD);
					} else {
						logger.error("Uncategorized type: " + inst.getAddInfo());
						System.exit(-1);
					}
				} else if (opcode == Opcodes.AALOAD) {
					reduceOp = BytecodeCategory.getOpcodeObj(Opcodes.ALOAD);
				}
				inst.originalOp = inst.getOp();
				inst.setOp(reduceOp);
				
				this.collectFamily(inst, toRemove);
			}
		}
		
		//System.out.println("Check remove");
		for (InstNode inst: toRemove) {
			//Remove directly, the jung graph conversion take care of null child
			//System.out.println(inst);
			pool.remove(inst);
		}
	}
	
	private void collectFamily(InstNode inst, HashSet<InstNode> recorder) {
		for (String parentKey: inst.getInstDataParentList()) {
			InstNode parentInst = this.theGraph.getInstPool().searchAndGet(parentKey);
			recorder.add(parentInst);
			this.collectFamily(parentInst, recorder);
		}
	}
	
	public GraphTemplate getGraph() {
		return this.theGraph;
	}
	
	public static void main(String[] args) {
		String addInfo = "org.ejml.alg.dense.decomposition.svd.SvdImplicitQrDecompose_D64.qralg.Lorg/ejml/alg/dense/decomposition/svd/implicitqr/SvdImplicitQrAlgorithm;:2";
		System.out.println("Add info sort: " + parseSort(addInfo));
		System.out.println("Object sort: " + Type.OBJECT);
	}
}
