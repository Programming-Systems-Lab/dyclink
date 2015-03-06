package edu.columbia.psl.cc.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import edu.columbia.psl.cc.analysis.StaticTester;
import edu.columbia.psl.cc.analysis.PageRankSelector.GraphProfile;
import edu.columbia.psl.cc.analysis.PageRankSelector.SegInfo;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.InstNode;

public class Locator {
	
	private static int assignmentThreshold = MIBConfiguration.getInstance().getAssignmentThreshold();
	
	private static double staticThreshold = MIBConfiguration.getInstance().getStaticThreshold();
	
	private static int simStrat = MIBConfiguration.getInstance().getSimStrategy();
	
	public static int getInstructionOp(InstNode inst) {
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
	
	public static int searchBetter(int oriIdx, 
			int expOp, 
			int centroid, 
			boolean isHead,
			List<InstNode> sortedTarget) {
		//Search around little bit
		if (isHead) {
			int curExpand = 1;
			while (curExpand <= 3) {
				int backward = oriIdx - curExpand;
				if (backward >= 0) {
					InstNode check = sortedTarget.get(backward);
					int checkOp = getInstructionOp(check);
					
					if (checkOp == expOp) {
						return backward;
					}
				}
				
				int forward = oriIdx + curExpand;
				if (forward <= sortedTarget.size() - 1 && forward <= centroid - 1) {
					InstNode check = sortedTarget.get(forward);
					int checkOp = getInstructionOp(check);
					
					if (checkOp == expOp) {
						return forward;
					}
				}
				curExpand++;
			}
		} else {
			int curExpand = 1;
			while (curExpand <= 3) {
				int forward = oriIdx + curExpand;
				if (forward <= sortedTarget.size() - 1 && forward <= centroid - 1) {
					InstNode check = sortedTarget.get(forward);
					int checkOp = getInstructionOp(check);
					
					if (checkOp == expOp) {
						return forward;
					}
				}
				
				int backward = oriIdx - curExpand;
				if (backward >= 0) {
					InstNode check = sortedTarget.get(backward);
					int checkOp = getInstructionOp(check);
					
					if (checkOp == expOp) {
						return backward;
					}
				}
				curExpand++;
			}
		}
		return oriIdx;
	}
	
	public static HashSet<InstNode> possibleSingleAssignment(InstNode subNode, List<InstNode> targetPool) {
		HashSet<InstNode> ret = new HashSet<InstNode>();
		int subId = getInstructionOp(subNode);
		
		for (InstNode targetNode: targetPool) {
			
			int targetId = getInstructionOp(targetNode);			
			if (targetId == subId) {
				ret.add(targetNode);
			}
		}
		
		return ret;
	}
	
	public static HashMap<InstNode, SegInfo> locateSegments(HashSet<InstNode> assignments, 
			List<InstNode> sortedTarget, 
			GraphProfile subProfile) {
		HashMap<InstNode, SegInfo> candSegs = new HashMap<InstNode, SegInfo>();
		List<Double> staticScoreRecorder = new ArrayList<Double>();
		
		InstNode subStartNode = subProfile.startInst;
		int expStartOp = getInstructionOp(subStartNode);
		InstNode subCentroid = subProfile.centroidWrapper.inst;
		int expCentroidOpcode = subCentroid.getOp().getOpcode();
		InstNode subEndNode = subProfile.endInst;
		int expEndOp = getInstructionOp(subEndNode);
		
		for (InstNode inst: assignments) {
			List<InstNode> seg = new ArrayList<InstNode>();
			
			int startIdx = -1;
			int endIdx = -1;
			boolean match = true;
			int realCentroidOpcode = inst.getOp().getOpcode();
			if (realCentroidOpcode != expCentroidOpcode)
				match = false;
			
			for (int i = 0; i < sortedTarget.size(); i++) {
				InstNode curNode = sortedTarget.get(i);
				if (curNode.equals(inst)) {
					//collect backward
					startIdx = i - subProfile.before;
					if (startIdx < 0)
						startIdx = 0;
					
					endIdx = i + subProfile.after;
					if (endIdx > sortedTarget.size() - 1)
						endIdx = sortedTarget.size() - 1;
					
					InstNode startNode = sortedTarget.get(startIdx);
					InstNode endNode = sortedTarget.get(endIdx);
					
					int realStartOp = getInstructionOp(startNode);
					int realEndOp = getInstructionOp(endNode);
					
					if (realStartOp != expStartOp) {
						//Search around little bit
						int oriStart = startIdx;
						startIdx = searchBetter(startIdx, expStartOp, i, true, sortedTarget);
						if (oriStart == startIdx) {
							match = false;
						}
					}
					
					if (realEndOp != expEndOp) {
						int oriEnd = endIdx;
						endIdx = searchBetter(endIdx, expEndOp, i, false, sortedTarget);
						if (oriEnd == endIdx) {
							match = false;
						}
					}
					
					seg.addAll(sortedTarget.subList(startIdx, endIdx + 1));
					break ;
				}
			}
			
			//Temporarily set it as 0.8. Not consider the too-short assignment
			if (seg.size() < subProfile.pgRep.length * 0.8) {
				//logger.info("Give up too-short assignment: " + inst + " size " + seg.size());
				continue ;
			} else {				
				//Ori is with same size, seg is with a little buffer
				double[] segDist = StaticTester.genDistribution(seg, simStrat);
				
				double[] segDistNorm = StaticTester.normalizeDist(segDist, seg.size());
				double segDistance = StaticTester.normalizeEucDistance(subProfile.normDist, segDistNorm);
				
				TreeSet<Integer> lineTrace = new TreeSet<Integer>();
				for (InstNode s: seg) {
					lineTrace.add(s.callerLine);
				}
				
				if (segDistance < staticThreshold) {
					SegInfo si = new SegInfo();
					si.seg = seg;
					si.normInstDistribution = segDist;
					si.instDistWithSub = segDistance;
					si.lineTrace = lineTrace;
					si.match = match;
					
					candSegs.put(inst, si);
					staticScoreRecorder.add(si.instDistWithSub);
				}
			}
		}
		
		if (candSegs.size() <= assignmentThreshold) {
			return candSegs;
		} else {
			Collections.sort(staticScoreRecorder);
			double bound = staticScoreRecorder.get(assignmentThreshold - 1);
			//logger.info("candSegs after static filter: " + candSegs.size());
			//logger.info("Static score recorder: " + staticScoreRecorder);
			//logger.info("Bound: " + bound);
			
			Iterator<InstNode> candKeyIterator = candSegs.keySet().iterator();
			while (candKeyIterator.hasNext()) {
				InstNode tmpInst = candKeyIterator.next();
				SegInfo tmpSI = candSegs.get(tmpInst);
				
				if (tmpSI.instDistWithSub > bound) {
					//logger.info("Removed: " + tmpInst + " " + tmpSI.instDistWithSub);
					candKeyIterator.remove();
				}
			}
			//logger.info("candSeg after removal: " + candSegs.size());
			return candSegs;
		}
	}

}
