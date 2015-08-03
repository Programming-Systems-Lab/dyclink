package edu.columbia.psl.cc.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.columbia.psl.cc.analysis.StaticTester;
import edu.columbia.psl.cc.analysis.PageRankSelector.GraphProfile;
import edu.columbia.psl.cc.analysis.PageRankSelector.SegInfo;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.pojo.InstNode;

public class Locator {
	
	private static int assignmentThreshold = MIBConfiguration.getInstance().getAssignmentThreshold();
	
	private static double staticThreshold = MIBConfiguration.getInstance().getStaticThreshold();
	
	private static int simStrat = MIBConfiguration.getInstance().getSimStrategy();
	
	private static Logger logger = Logger.getLogger(Locator.class);
		
	public static boolean equalInst(InstNode i1, InstNode i2) {
		return (i1.repOp == i2.repOp);
	}
		
	public static int searchBetter(int oriIdx, 
			InstNode expInst, 
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
					
					if (equalInst(expInst, check)) {
						return backward;
					}
				}
				
				int forward = oriIdx + curExpand;
				if (forward <= sortedTarget.size() - 1 && forward <= centroid - 1) {
					InstNode check = sortedTarget.get(forward);
					
					if (equalInst(expInst, check)) {
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
					
					if (equalInst(expInst, check)) {
						return forward;
					}
				}
				
				int backward = oriIdx - curExpand;
				if (backward >= 0) {
					InstNode check = sortedTarget.get(backward);
					
					if (equalInst(expInst, check)) {
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
		//int subId = SearchUtil.getInstructionOp(subNode);
				
		for (InstNode targetNode: targetPool) {
			if (equalInst(subNode, targetNode)) {
				ret.add(targetNode);
			}
			
			/*int targetId = SearchUtil.getInstructionOp(targetNode);
			if (targetId == subId) {
				ret.add(targetNode);
			}*/
		}
		
		return ret;
	}
			
	public static HashMap<InstNode, SegInfo> locateSegments(HashSet<InstNode> assignments, 
			List<InstNode> sortedTarget, 
			GraphProfile subProfile) {
		HashMap<InstNode, SegInfo> candSegs = new HashMap<InstNode, SegInfo>();
		List<Double> staticScoreRecorder = new ArrayList<Double>();
		
		InstNode subStartNode = subProfile.startInst;
		//int expStartOp = SearchUtil.getInstructionOp(subStartNode);
		//InstNode subCentroid = subProfile.centroidWrapper.inst;
		InstNode subEndNode = subProfile.endInst;
		//int expEndOp = SearchUtil.getInstructionOp(subEndNode);
		
		for (InstNode inst: assignments) {
			List<InstNode> seg = new ArrayList<InstNode>();
			
			int startIdx = -1;
			int endIdx = -1;
			int betterNum = 0;
			StringBuilder lineBuilder = new StringBuilder();
			
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
										
					if (!equalInst(subStartNode, startNode)) {
						//Search around little bit
						int oriStart = startIdx;
						startIdx = searchBetter(startIdx, subStartNode, i, true, sortedTarget);
						if (oriStart != startIdx) {
							betterNum++;
						}
					} 
					InstNode finalStartNode = sortedTarget.get(startIdx);
					lineBuilder.append(finalStartNode.callerLine + ":");
					lineBuilder.append(inst.callerLine + ":");
						
					if (!equalInst(subEndNode, endNode)) {
						int oriEnd = endIdx;
						endIdx = searchBetter(endIdx, subEndNode, i, false, sortedTarget);
						if (oriEnd != endIdx) {
							betterNum++;
						}
					}
					InstNode finalEndNode = sortedTarget.get(endIdx);
					lineBuilder.append(finalEndNode.callerLine);
					
					seg.addAll(sortedTarget.subList(startIdx, endIdx + 1));
					break ;
				}
			}
			
			//Temporarily set it as 0.8. Not consider the too-short assignment
			if (seg.size() < subProfile.pgRep.length * 0.8) {
				//System.out.println("Give up too-short assignment: " + inst + " size " + seg.size() + " " + subProfile.pgRep.length);
				//logger.info("Give up too-short assignment: " + inst + " size " + seg.size() + " " + subProfile.pgRep.length);
				continue ;
			} else {				
				//Ori is with same size, seg is with a little buffer
				double[] segDist = StaticTester.genDistribution(seg);
				
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
					si.lineTrace = lineBuilder.toString();
					si.match = (betterNum > 0);
					
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
