package edu.columbia.psl.cc.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.columbia.psl.cc.analysis.InstWrapper;
import edu.columbia.psl.cc.analysis.JaroWinklerDistance;
import edu.columbia.psl.cc.analysis.PageRankSelector;
import edu.columbia.psl.cc.analysis.StaticTester;
import edu.columbia.psl.cc.analysis.PageRankSelector.GraphProfile;
import edu.columbia.psl.cc.analysis.PageRankSelector.SegInfo;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class Locator {
	
	private static int assignmentThreshold = MIBConfiguration.getInstance().getAssignmentThreshold();
	
	private static double staticThreshold = MIBConfiguration.getInstance().getStaticThreshold();
	
	private static int simStrat = MIBConfiguration.getInstance().getSimStrategy();
	
	private static Logger logger = LogManager.getLogger(Locator.class);
		
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
		}
		
		return ret;
	}
	
	public static HashSet<InstNode> advSingleAssignment(GraphProfile subProfile, InstPool targetPool) {
		HashSet<InstNode> ret = new HashSet<InstNode>();
		InstNode subNode = subProfile.centroidWrapper.inst;
		
		for (InstNode targetNode: targetPool) {
			if (equalInst(subNode, targetNode)) {
				//Check backward/forward neighbors
				int[] targetCore = coreTracer(targetNode, targetPool);
				//double coreSim = JaroWinklerDistance.JARO_DISTANCE.proximity(subProfile.coreRep, targetCore);
				double coreSim = JaroWinklerDistance.JARO_WINKLER_DISTANCE.proximity(subProfile.coreRep, targetCore);
				
				System.out.println("Sub: " + subProfile.centroidWrapper.inst);
				System.out.println("Target: " + targetNode);
				System.out.println("Core sim: " + coreSim);
				
				if (coreSim > 0.75) {
					ret.add(targetNode);
				}
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
					} else {
						betterNum++;
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
					} else {
						betterNum++;
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
					si.match = (betterNum == 2);
					
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
	
	public static int[] coreTracer(InstNode inst, InstPool pool) {
		List<InstLevel> bqueue = new LinkedList<InstLevel>();
		Set<InstNode> backward = new HashSet<InstNode>();
		InstLevel il = new InstLevel();
		il.inst = inst;
		il.level = 0;
		bqueue.add(il);
		
		int edgeCount = 0;
		DirectedSparseGraph<InstNode, Integer> coregraph = new DirectedSparseGraph<InstNode, Integer>();
		
		while (!bqueue.isEmpty()) {			
			InstLevel curLevel = bqueue.remove(0);
			if (curLevel.level == 3)
				break ;
			
			InstNode curNode = curLevel.inst;
			backward.add(curLevel.inst);
			
			//backward
			for (String p: curNode.getInstDataParentList()) {
				InstNode pInst = pool.searchAndGet(p);
				if (pInst != null) {
					InstLevel pil = new InstLevel();
					pil.inst = pInst;
					pil.level = curLevel.level + 1;
					bqueue.add(pil);
					coregraph.addEdge(edgeCount++, pInst, curNode);
				}
			}
			
			for (String p: curNode.getWriteDataParentList()) {
				InstNode pInst = pool.searchAndGet(p);
				if (pInst != null) {
					InstLevel pil = new InstLevel();
					pil.inst = pInst;
					pil.level = curLevel.level + 1;
					bqueue.add(pil);
					coregraph.addEdge(edgeCount++, pInst, curNode);
				}
			}
			
			for (String p: curNode.getControlParentList()) {
				InstNode pInst = pool.searchAndGet(p);
				if (pInst != null) {
					InstLevel pil = new InstLevel();
					pil.inst = pInst;
					pil.level = curLevel.level + 1;
					bqueue.add(pil);
					coregraph.addEdge(edgeCount++, pInst, curNode);
				}
			}
 		}
		
		List<InstLevel> fqueue = new LinkedList<InstLevel>();
		Set<InstNode> forward = new HashSet<InstNode>();
		fqueue.add(il);
		
		while (!fqueue.isEmpty()) {
			InstLevel curLevel = fqueue.remove(0);
			if (curLevel.level == 3)
				break ;
			
			InstNode curNode = curLevel.inst;
			forward.add(curNode);
			
			for (String c: curNode.getChildFreqMap().keySet()) {
				InstNode cInst = pool.searchAndGet(c);
				if (cInst != null) {
					InstLevel cil = new InstLevel();
					cil.inst = cInst;
					cil.level = curLevel.level + 1;
					fqueue.add(cil);
					coregraph.addEdge(edgeCount++, curNode, cInst);
				}
			}
		}
		forward.remove(inst);
		
		List<Set<InstNode>> ret = new ArrayList<Set<InstNode>>();
		ret.add(backward);
		ret.add(forward);
		
		PageRank<InstNode, Integer> coreranker = new PageRank<InstNode, Integer>(coregraph, MIBConfiguration.getInstance().getPgAlpha());
		coreranker.setMaxIterations(MIBConfiguration.getInstance().getPgMaxIter());
		coreranker.evaluate();
		
		List<InstWrapper> coreRecord = new ArrayList<InstWrapper>();
		for (InstNode i: coregraph.getVertices()) {
			double rnk = coreranker.getVertexScore(i);
			InstWrapper iw = new InstWrapper(i, rnk);
			coreRecord.add(iw);
		}
		Collections.sort(coreRecord, PageRankSelector.pageRankSorter());
		int[] genRep = SearchUtil.generatePageRankRep(coreRecord);
		
		/*logger.info("Core inst: " + inst);
		logger.info("Backward: " + backward);
		logger.info("Forward: " + forward);*/
		//return ret;
		return genRep;
	}
	
	public static class InstLevel {
		
		public InstNode inst;
		
		int level;
	}

}
