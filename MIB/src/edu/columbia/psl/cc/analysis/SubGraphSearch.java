package edu.columbia.psl.cc.analysis;

import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.util.GraphUtil;
import edu.columbia.psl.cc.util.StringUtil;
import edu.columbia.psl.cc.util.TemplateLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Map;
import java.util.Collection;

import com.google.gson.reflect.TypeToken;

public class SubGraphSearch {
	
	private static boolean atLeastOneMatch(HashSet<InstNode> children1, HashSet<InstNode> children2) {
		for (InstNode c1: children1) {
			for (InstNode c2: children2) {
				if (c1.getOp().getCatId() == c2.getOp().getCatId())
					return true;
			}
		}
		return false;
	}
	
	public static TreeMap<InstNode, HashSet<InstNode>> possibleAssignments(GraphTemplate targetGraph, GraphTemplate subGraph) {
		//For each v in sub graph, generate cand set;
		InstPool targetPool = targetGraph.getInstPool();
		InstPool subPool = subGraph.getInstPool();
		
		TreeMap<InstNode, HashSet<InstNode>> map = new TreeMap<InstNode, HashSet<InstNode>>();
		for (InstNode i: subPool) {
			HashSet<InstNode> cands = new HashSet<InstNode>();
			map.put(i, cands);
			for (InstNode j: targetPool) {
				if ((j.getOp().getCatId() == i.getOp().getCatId()) && 
						(j.getChildFreqMap().size() >= i.getChildFreqMap().size())) {
					cands.add(j);
				}
			}
		}
		
		//Global prune
		boolean changed = true;
		while (changed) {
			changed = false;
			for (InstNode i: subPool) {
				HashSet<InstNode> iChildren = GraphUtil.retrieveChildInsts(i, subPool);
				for (InstNode iChild: iChildren) {
					HashSet<InstNode> iMaps = map.get(i);
					
					Iterator<InstNode> iMapsIt = iMaps.iterator();
					while (iMapsIt.hasNext()) {
						InstNode iMap = iMapsIt.next();
						HashSet<InstNode> iMapChildren = GraphUtil.retrieveChildInsts(iMap, targetPool);
						HashSet<InstNode> iChildMaps = map.get(iChild);
						if (!atLeastOneMatch(iMapChildren, iChildMaps)) {
							iMapsIt.remove();
							
							if (iMaps.size() == 0)
								return null;
							
							changed = true;
						}
					}
				}
			}
		}
		return map;
	}
	
	/**
	 * Check the cat of each inst node to filter out most impossile assignments
	 */
	public static TreeMap<InstNode, HashSet<InstNode>> preAssignments(GraphTemplate targetGraph, GraphTemplate subGraph) {
		InstPool subPool = subGraph.getInstPool();
		InstPool targetPool = targetGraph.getInstPool();
		TreeMap<InstNode, HashSet<InstNode>> possibleAssignments = new TreeMap<InstNode, HashSet<InstNode>>();
		
		for (InstNode sInst: subPool) {
			HashSet<InstNode> myAssignment = new HashSet<InstNode>();
			
			for (InstNode tInst: targetPool) {
				if (tInst.getOp().getOpcode() == sInst.getOp().getOpcode()) {
					myAssignment.add(tInst);
				}
			}
			
			possibleAssignments.put(sInst, myAssignment);
		}
		
		return possibleAssignments;
	}
	
	public static void updatePossibleAssignments(GraphTemplate target, 
			GraphTemplate sub, 
			TreeMap<InstNode, HashSet<InstNode>> possibleAssignments) {
		InstPool tPool = target.getInstPool();
		InstPool sPool = sub.getInstPool();
		
		boolean anyChange = true;
		while (anyChange) {
			anyChange = false;
			
			for (InstNode sInst: sPool) {
				//Get possible assignment in sub
				HashSet<InstNode> sAssignments = possibleAssignments.get(sInst);
				
				HashSet<InstNode> toRemove = new HashSet<InstNode>();
				for (InstNode sAssignment: sAssignments) {
					//Get children of sub
					HashSet<InstNode> sChildren = GraphUtil.retrieveChildInsts(sInst, sPool);
					HashSet<InstNode> sAssignmentChildren = GraphUtil.retrieveChildInsts(sAssignment, tPool);
					for (InstNode sChild: sChildren) {
						boolean match = false;
						
						//Get possible assignment of sChild
						HashSet<InstNode> sChildAssignments = possibleAssignments.get(sChild);
						for (InstNode sChildAssignment: sChildAssignments) {
							if (sAssignmentChildren.contains(sChildAssignment)) {
								match = true;
								break ;
							}
						}
						
						if (!match) {
							toRemove.add(sAssignment);
							anyChange = true;
						}
					}
				}
				
				for (InstNode remove: toRemove) {
					sAssignments.remove(remove);
				}
			}
		}
	}
	
	public static <T> TreeMap<T, HashSet<T>> copyAssignments(TreeMap<T, HashSet<T>> toCopy) {
		TreeMap<T, HashSet<T>> ret = new TreeMap<T, HashSet<T>>();
		
		for (T t: toCopy.keySet()) {
			HashSet<T> value = new HashSet<T>(toCopy.get(t));
			ret.put(t, value);
		}
		
		return ret;
	}
	
	public static boolean search(GraphTemplate target, 
			GraphTemplate sub, 
			ArrayList<InstNode> subIndex, 
			TreeMap<InstNode, InstNode> assignments, 
			TreeMap<InstNode, HashSet<InstNode>> possibleAssignments) {
		updatePossibleAssignments(target, sub, possibleAssignments);
		
		int assigned = assignments.size();
		if (assigned == sub.getInstPool().size())
			return true;
		
		InstNode onDeck = subIndex.get(assigned);
		HashSet<InstNode> onDeckPossibleAssignments = possibleAssignments.get(onDeck);
		Iterator<InstNode> onDeckPossibleAssignmentsIt = onDeckPossibleAssignments.iterator();
		while (onDeckPossibleAssignmentsIt.hasNext()) {
			InstNode pAssignment = onDeckPossibleAssignmentsIt.next();
			if (!assignments.containsKey(onDeck)) {
				assignments.put(onDeck, pAssignment);
				
				TreeMap<InstNode, HashSet<InstNode>> newPossibleAssignments = copyAssignments(possibleAssignments);
				HashSet<InstNode> singleAssignment = new HashSet<InstNode>();
				singleAssignment.add(pAssignment);
				newPossibleAssignments.put(onDeck, singleAssignment);
				
				if (search(target, sub, subIndex, assignments, newPossibleAssignments))
					return true;
				
				assignments.remove(onDeck);
			}
			onDeckPossibleAssignmentsIt.remove();
			updatePossibleAssignments(target, sub, possibleAssignments);
		}
		
		return false;
	}
	
	public static void main(String[] args) {
		File tempDir = new File("./template");
		File testDir = new File("./test");
		TypeToken<GraphTemplate> type = new TypeToken<GraphTemplate>(){};
		HashMap<String, GraphTemplate> templates = TemplateLoader.loadTemplate(tempDir, type);
		HashMap<String, GraphTemplate> tests = TemplateLoader.loadTemplate(testDir, type);
		
		for (String temp: templates.keySet()) {
			GraphTemplate tempGraph = templates.get(temp);
			GraphUtil.removeReturnInst(tempGraph.getInstPool());
			ArrayList<InstNode> tempIndex = new ArrayList<InstNode>(tempGraph.getInstPool());
			
			for (String test: tests.keySet()) {
				GraphTemplate testGraph = tests.get(test);
				
				TreeMap<InstNode, HashSet<InstNode>> possibleAssignments = 
						preAssignments(testGraph, tempGraph);
				
				System.out.println("Pre assignments: ");
				for (InstNode p: possibleAssignments.keySet()) {
					System.out.println("Inst in sub: " + p);
					System.out.println("Possible assignment: ");
					for (InstNode pAssignment: possibleAssignments.get(p)) {
						System.out.println(pAssignment);
					}
					System.out.println();
				}
				
				TreeMap<InstNode, InstNode> assignments = new TreeMap<InstNode, InstNode>();
				search(testGraph, tempGraph, tempIndex, assignments, possibleAssignments);
				System.out.println("Assignment result: ");
				for (InstNode tempInst: assignments.keySet()) {
					System.out.println("Inst in template: " + tempInst);
					System.out.println("Inst in test: " + assignments.get(tempInst));
				}
			}
		}
		
		/*File mainF = new File("./template/cc.testbase.TemplateMethod:testAdd2:(I):I:1.json");
		File subF = new File("./template/cc.testbase.TemplateMethod:add2:(I):I:1.json");
		
		
		GraphTemplate mainGraph = TemplateLoader.loadTemplateFile(mainF, type);
		GraphTemplate subGraph = TemplateLoader.loadTemplateFile(subF, type);
		GraphUtil.removeReturnInst(subGraph.getInstPool());
		
		System.out.println("Main graph size: " + mainGraph.getInstPool().size());
		System.out.println("Sub graph size: " + subGraph.getInstPool().size());
		
		//System.out.println(possibleAssignments(mainGraph, subGraph));
		ArrayList<InstNode> subIndex = new ArrayList<InstNode>(subGraph.getInstPool());
		TreeMap<InstNode, HashSet<InstNode>> possibleAssignments = preAssignments(mainGraph, subGraph);
		System.out.println("Pre assignments: ");
		for (InstNode p: possibleAssignments.keySet()) {
			System.out.println("Inst in sub: " + p);
			System.out.println("Possible assignment: ");
			for (InstNode pAssignment: possibleAssignments.get(p)) {
				System.out.println(pAssignment);
			}
			System.out.println();
		}
		
		TreeMap<InstNode, InstNode> assignments = new TreeMap<InstNode, InstNode>();
		search(mainGraph, subGraph, subIndex, assignments, possibleAssignments);
		System.out.println("Assignemnt restuls: ");
		for (InstNode sInst: assignments.keySet()) {
			System.out.println("Inst in sub: " + sInst);
			System.out.println("Inst in target: " + assignments.get(sInst));
			System.out.println();
		}*/
	}

}
