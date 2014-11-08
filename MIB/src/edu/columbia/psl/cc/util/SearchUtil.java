package edu.columbia.psl.cc.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import edu.columbia.psl.cc.analysis.InstWrapper;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;

public class SearchUtil {
	
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
	
	public static HashMap<InstNode, HashSet<InstNode>> possibleAssignments(GraphTemplate target, GraphTemplate sub) {
		HashMap<InstNode, HashSet<InstNode>> ret = new HashMap<InstNode, HashSet<InstNode>>();
		
		for (InstNode childNode: sub.getInstPool()) {
			ret.put(childNode, possibleSingleAssignment(childNode, target));
		}
		
		return ret;
	}
	
	public static HashSet<InstNode> possibleSingleAssignment(InstNode subNode, GraphTemplate target) {
		HashSet<InstNode> ret = new HashSet<InstNode>();
		
		for (InstNode targetNode: target.getInstPool()) {
			if (targetNode.getOp().getOpcode() == subNode.getOp().getOpcode()) {
				ret.add(targetNode);
			}
		}
		
		return ret;
	}
	
	public static int[] generatePageRankRep(List<InstWrapper> pgList) {
		int[] ret = new int[pgList.size()];
		int counter = 0;
		for (InstWrapper iw: pgList) {
			ret[counter++] = iw.inst.getOp().getOpcode();
		}
		return ret;
	}

}
