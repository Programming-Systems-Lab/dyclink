package edu.columbia.psl.cc.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.MethodNode;
import edu.columbia.psl.cc.pojo.MethodNode.MetaGraph;

public class GraphConstructor {
	
	private static Logger logger = Logger.getLogger(GraphConstructor.class);
	
	private static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
	private static double maxFreqFromParents(Collection<InstNode> parents, String myId) {
		double ret = 0;
		for (InstNode p: parents) {
			double freq = p.getChildFreqMap().get(myId);
			if (freq > ret)
				ret = freq;
		}
		return ret;
	}
	
	private static HashMap<Integer, HashSet<InstNode>> retrieveParentsWithIdx(HashMap<Integer, HashSet<String>> parentFromCaller, 
			InstPool callerPool) {
		HashMap<Integer, HashSet<InstNode>> ret = new HashMap<Integer, HashSet<InstNode>>();
		for (Integer idx: parentFromCaller.keySet()) {
			HashSet<String> parentStrings = parentFromCaller.get(idx);
			HashSet<InstNode> parents = new HashSet<InstNode>();
			
			for (String parentString: parentStrings) {
				InstNode parentNode = callerPool.searchAndGet(parentString);
				parents.add(parentNode);
			}
			ret.put(idx, parents);
		}
		return ret;
	}
	
	private static HashSet<InstNode> flattenParentMap(Collection<HashSet<InstNode>> allParents) {
		HashSet<InstNode> ret = new HashSet<InstNode>();
		for (HashSet<InstNode> partParents: allParents) {
			ret.addAll(partParents);
		}
		return ret;
	}
	
	public static void reconstructGraph(GraphTemplate rawGraph) {
		String myId = StringUtil.genThreadWithMethodIdx(rawGraph.getThreadId(), rawGraph.getThreadMethodId());
		String baseDir = MIBConfiguration.getInstance().getCacheDir() + "/" + myId;
		
		File baseDirProbe = new File(baseDir);
		if (!baseDirProbe.exists()) {
			return ;
		}
		
		try {
			List<GraphTemplate> toMerge = new ArrayList<GraphTemplate>();
			
			Iterator<InstNode> instIt = rawGraph.getInstPool().iterator();
			InstNode lastBeforeReturn = rawGraph.getLastBeforeReturn();
			while (instIt.hasNext()) {
				InstNode inst = instIt.next();
				
				if (inst instanceof MethodNode) {				
					MethodNode mn = (MethodNode)inst;
					
					String mnId = StringUtil.genIdxKey(mn.getThreadId(), mn.getThreadMethodIdx(), mn.getIdx());
					TreeMap<String, Double> childMap = mn.getChildFreqMap();
					for (MetaGraph meta: mn.getCalleeInfo().metaCallees) {
						String calleeId = meta.calleeIdx;
						double frac = meta.normFreq;
						
						File f = new File(baseDir + "/" + calleeId);
						GraphTemplate callee = GsonManager.readJsonGeneric(f, graphToken);
						reconstructGraph(callee);
						toMerge.add(callee);
						
						//This means that the method call is the last inst, will only have 1 graph
						if (lastBeforeReturn != null && inst.equals(lastBeforeReturn)) {
							rawGraph.setLastBeforeReturn(callee.getLastBeforeReturn());
						}
						
						HashSet<String> cReads = callee.getFirstReadLocalVars();
						HashSet<InstNode> cReadNodes = new HashSet<InstNode>();
						for (String cString: cReads) {
							InstNode cReadNode = callee.getInstPool().searchAndGet(cString);
							cReadNodes.add(cReadNode);
						}
						
						if (mn.getCalleeInfo().parentReplay.size() > 0) {
							HashMap<Integer, HashSet<InstNode>> parentFromCaller = retrieveParentsWithIdx(mn.getCalleeInfo().parentReplay, 
									rawGraph.getInstPool());
							
							HashSet<InstNode> allParents = flattenParentMap(parentFromCaller.values());
							double freq = maxFreqFromParents(allParents, mnId) * frac;
							GraphUtil.multiplyGraph(callee, freq);	
							GraphUtil.dataDepFromParentToChildWithFreq(parentFromCaller, cReadNodes, mnId, freq);
						}
						
						if (mn.getControlParentList().size() > 0) {
							HashSet<InstNode> controlInsts = GraphUtil.retrieveRequiredParentInsts(mn, 
									rawGraph.getInstPool(), 
									MIBConfiguration.CONTR_DEP);
							
							for (InstNode controlInst: controlInsts) {
								double freq = controlInst.getChildFreqMap().get(mnId) * frac;
								GraphUtil.controlDepFromParentToChildWithFreq(controlInst, cReadNodes, freq);
								controlInst.getChildFreqMap().remove(mnId);
							}
						}
						
						if (childMap.size() > 0) {
							String calleeChildReplaceId = meta.lastInstString;
							InstNode calleeChildReplace = callee.getInstPool().searchAndGet(calleeChildReplaceId);
							
							for (String childKey: childMap.keySet()) {
								InstNode childNode = rawGraph.getInstPool().searchAndGet(childKey);
								double cFreq = childMap.get(childKey) * frac;
								
								calleeChildReplace.increChild(childNode.getThreadId(), 
										childNode.getThreadMethodIdx(), 
										childNode.getIdx(), 
										cFreq);
								childNode.registerParent(calleeChildReplace.getThreadId(), 
										calleeChildReplace.getThreadMethodIdx(), 
										calleeChildReplace.getIdx(), 
										MIBConfiguration.INST_DATA_DEP);
								
								childNode.getInstDataParentList().remove(mnId);
							}
						}
					}
					instIt.remove();
				}
			}
			
			for (GraphTemplate callee: toMerge) {
				InstPool calleePool = callee.getInstPool();
				GraphUtil.unionInstPools(rawGraph.getInstPool(), calleePool);
			}
		} catch (Exception ex) {
			logger.error("Exception: ", ex);
		}
	}
	
	public static void main(String[] args) {
		File testFile = new File("./test/Jama.SingularValueDecomposition:<init>:0:0:2.json");
		GraphTemplate testGraph = GsonManager.readJsonGeneric(testFile, graphToken);
		reconstructGraph(testGraph);
		System.out.println("Recorded graph size: " + testGraph.getVertexNum());
		System.out.println("Actual graph size: " + testGraph.getInstPool().size());
		
		System.out.println("Recorded vertex size: " + testGraph.getEdgeNum());
		int eCount = 0;
		for (InstNode inst: testGraph.getInstPool()) {
			eCount += inst.getChildFreqMap().size();
		}
		System.out.println("Actual edge size: " + eCount);
		
		String fileName = testGraph.getShortMethodKey() + "_re";
		GsonManager.writeJsonGeneric(testGraph, fileName, graphToken, MIBConfiguration.TEST_DIR);
	}

}
