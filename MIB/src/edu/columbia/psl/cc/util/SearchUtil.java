package edu.columbia.psl.cc.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import edu.columbia.psl.cc.analysis.InstWrapper;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;

public class SearchUtil {
	
	private static int simStrat = MIBConfiguration.getInstance().getSimStrategy();
	
	private static boolean nativeClass = MIBConfiguration.getInstance().isNativeClass();
	
	private static int baseLength;
	
	static {
		if (simStrat == MIBConfiguration.INST_STRAT) {
			baseLength = 256;
		} else if (simStrat == MIBConfiguration.SUBSUB_STRAT) {
			baseLength = 63;
		} else if (simStrat == MIBConfiguration.SUB_STRAT) {
			baseLength = 43;
		} else {
			baseLength = 21;
		}
	}
	
	/**
	 * Not really hash, but differentiate native call from the same pkg
	 * @param inst
	 * @return
	 */
	public static void repOp(InstNode inst) {
		int rawOp = SearchUtil.getInstructionOp(inst);
		
		if (rawOp >= SearchUtil.baseLength()) {
			//Native call
			inst.repOp = rawOp * (inst.getInstDataParentList().size() + 1) + inst.getChildFreqMap().size();
		} else {
			inst.repOp = rawOp;
		}
		/*System.out.println("Check rawOp: " + rawOp);
		System.out.println("Check repOp: " + inst.repOp);
		System.out.println(inst);*/
	}
	
	public static int[] generateBytecodeFreq(InstPool pool) {
		int[] ret = new int[256];
		
		for (InstNode inst: pool) {
			int opcode = inst.getOp().getOpcode();
			ret[opcode] = ret[opcode] + 1;
		}
		
		return ret;
	}
	
	public static double[] generatePriorByPageRank(List<InstWrapper> wrappers) {
		double[] ret = new double[256];
		
		for (InstWrapper wrap: wrappers) {
			int opcode = wrap.inst.getOp().getOpcode();
			ret[opcode] = ret[opcode] + wrap.pageRank;
		}
		
		return ret;
	}
	
	public static HashMap<InstNode, Double> redistribute(int[] priors, InstPool pool) {
		//Should be 256
		HashMap<InstNode, Double> priorRecorder = new HashMap<InstNode, Double>();
		int sum = 0;
		for (InstNode inst: pool) {
			int bytecodeFreq = priors[inst.getOp().getOpcode()];
			sum += bytecodeFreq;
			priorRecorder.put(inst, Double.valueOf(bytecodeFreq));
		}
		
		for (InstNode inst: priorRecorder.keySet()) {
			double prior = priorRecorder.get(inst)/sum;
			priorRecorder.put(inst, prior);
		}
		return priorRecorder;
	}
	
	public static HashMap<InstNode, Double> redistribute(double[] priors, InstPool pool) {
		//Should be 256
		HashMap<InstNode, Double> priorRecorder = new HashMap<InstNode, Double>();
		double sum = 0;
		for (InstNode inst: pool) {
			double prScore= priors[inst.getOp().getOpcode()];
			sum += prScore;
			priorRecorder.put(inst, prScore);
		}
		
		for (InstNode inst: priorRecorder.keySet()) {
			double prior = priorRecorder.get(inst)/sum;
			priorRecorder.put(inst, prior);
		}
		return priorRecorder;
	}
	
	public static List<InstNode> possibleAssignmentSequence(GraphTemplate templateGraph, GraphTemplate subGraph) {
		HashSet<InstNode> allCands = new HashSet<InstNode>();
		
		for (InstNode childNode: subGraph.getInstPool()) {
			for (InstNode targetNode: templateGraph.getInstPool()) {
				if (targetNode.getOp().getOpcode() == childNode.getOp().getOpcode()) {
					allCands.add(targetNode);
				}
			}
		}
		
		List<InstNode> sortByStart = GraphUtil.sortInstPool(allCands, true);
		return sortByStart;
	}
	
	/*public static HashMap<InstNode, HashSet<InstNode>> possibleAssignments(GraphTemplate target, GraphTemplate sub) {
		HashMap<InstNode, HashSet<InstNode>> ret = new HashMap<InstNode, HashSet<InstNode>>();
		
		for (InstNode childNode: sub.getInstPool()) {
			ret.put(childNode, possibleSingleAssignment(childNode, target.getInstPool()));
		}
		
		return ret;
	}*/
	
	public static int[] generatePageRankRep(List<InstWrapper> pgList) {
		int[] ret = new int[pgList.size()];
		int counter = 0;
		for (InstWrapper iw: pgList) {
			//int repOp = SearchUtil.getInstructionOp(iw.inst);
			ret[counter++] = iw.inst.repOp;
		}
		return ret;
	}
	
	public static int getInstructionOp(InstNode inst) {		
		if (nativeClass && BytecodeCategory.methodOps().contains(inst.getOp().getOpcode())) {
			return baseLength() + StringUtil.extractPkgId(inst.getAddInfo());
		}
		
		if (simStrat == MIBConfiguration.INST_STRAT) {
			return inst.getOp().getOpcode();
		} else if (simStrat == MIBConfiguration.SUBSUB_STRAT) {
			return inst.getOp().getSubSubCatId();
		} else if (simStrat == MIBConfiguration.SUB_STRAT) {
			return inst.getOp().getSubCatId();
		} else {
			return inst.getOp().getCatId();
		}
	}
	
	public static int baseLength() {
		return baseLength;
		/*if (simStrat == MIBConfiguration.INST_STRAT) {
			return 256;
		} else if (simStrat == MIBConfiguration.SUBSUB_STRAT) {
			return 63;
		} else if (simStrat == MIBConfiguration.SUB_STRAT) {
			return 43;
		} else {
			return 21;
		}*/
	}
}
