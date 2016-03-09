package edu.columbia.psl.cc.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.objectweb.asm.Opcodes;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.analysis.GraphReducer;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.FieldNode;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.MethodNode;
import edu.columbia.psl.cc.pojo.MethodNode.MetaGraph;

public class GraphConstructor {
	
	private static Logger logger = LogManager.getLogger(GraphConstructor.class);
	
	private static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
	private static String objInit = "java.lang.Object:<init>:():V";
	
	private double maxFreqFromParents(Collection<InstNode> parents, String myId) {
		double ret = 0;
		//System.out.println("My id: " + myId);
		for (InstNode p: parents) {
			//System.out.println("Parent: " + p.getFromMethod() + " " + p.getIdx());
			//System.out.println("Childern map: " + p.getChildFreqMap());
			double freq = p.getChildFreqMap().get(myId);
			if (freq > ret)
				ret = freq;
		}
		return ret;
	}
	
	private HashMap<Integer, HashSet<InstNode>> retrieveParentsWithIdx(HashMap<Integer, HashSet<String>> parentFromCaller, 
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
	
	private HashSet<InstNode> flattenParentMap(Collection<HashSet<InstNode>> allParents) {
		HashSet<InstNode> ret = new HashSet<InstNode>();
		for (HashSet<InstNode> partParents: allParents) {
			ret.addAll(partParents);
		}
		return ret;
	}
	
	private void calDepForMethodNodes(InstNode methodNode, InstPool instPool, List<InstNode> collecter) {
		if (collecter.contains(methodNode))
			return ;
		
		for (String key: methodNode.getChildFreqMap().keySet()) {
			InstNode childInst = instPool.searchAndGet(key);
			
			if ((childInst instanceof MethodNode)) {
				calDepForMethodNodes(childInst, instPool, collecter);
			}
		}
		collecter.add(methodNode);
	}
	
	public void reconstructGraph(GraphTemplate rawGraph, boolean addCallerLine) {
		String myId = StringUtil.genThreadWithMethodIdx(rawGraph.getThreadId(), rawGraph.getThreadMethodId());
		String baseDir = MIBConfiguration.getInstance().getCacheDir() + "/" + myId;
		
		File baseDirProbe = new File(baseDir);
		if (!baseDirProbe.exists()) {			
			if (MIBConfiguration.getInstance().isReduceGraph()) {
				GraphReducer gr = new GraphReducer(rawGraph);
				gr.reduceGraph();
			}
			
			return ;
		}
		
		try {
			List<InstNode> processQueue = new ArrayList<InstNode>();
			for (InstNode inst: rawGraph.getInstPool()) {
				if (processQueue.contains(inst))
					continue ;
				
				if (!(inst instanceof MethodNode)) {
					processQueue.add(inst);
				}
				
				calDepForMethodNodes(inst, rawGraph.getInstPool(), processQueue);
			}
			
			List<GraphTemplate> toMerge = new ArrayList<GraphTemplate>();
			InstNode lastBeforeReturn = rawGraph.getLastBeforeReturn();
			for (InstNode inst: processQueue) {
				if (inst instanceof MethodNode) {
					//logger.info("Method node: " + inst);
					MethodNode mn = (MethodNode)inst;
					
					String mnId = StringUtil.genIdxKey(mn.getThreadId(), mn.getThreadMethodIdx(), mn.getIdx());
					TreeMap<String, Double> childMap = mn.getChildFreqMap();
					Set<InstNode> cRemoveMn = new HashSet<InstNode>();
					
					HashMap<Integer, HashSet<InstNode>> parentFromCaller = null;
					HashSet<InstNode> allParents = null;
					double allFreq = 0;
					if (mn.getCalleeInfo().parentReplay != null && mn.getCalleeInfo().parentReplay.size() > 0) {
						parentFromCaller = retrieveParentsWithIdx(mn.getCalleeInfo().parentReplay, rawGraph.getInstPool());
						
						allParents = flattenParentMap(parentFromCaller.values());
						allFreq = maxFreqFromParents(allParents, mnId);
					}
										
					HashSet<InstNode> controlInsts = null;
					if (mn.getControlParentList().size() > 0) {
						controlInsts = GraphUtil.retrieveRequiredParentInsts(mn, 
								rawGraph.getInstPool(), 
								MIBConfiguration.CONTR_DEP);
						
						//No data parent
						if (allFreq == 0)
							allFreq = maxFreqFromParents(controlInsts, mnId);
					}
					
					if (allFreq == 0) {
						//No data, no control parent...
						for (double childVal: mn.getChildFreqMap().values()) {
							allFreq += childVal;
						}
						
						if (allFreq == 0)
							allFreq = 1;
					}
										
					for (MetaGraph meta: mn.getCalleeInfo().metaCallees) {
						String calleeId = meta.calleeIdx;
						double frac = meta.normFreq;
						
						double diff = frac - 0;
						if (Math.abs(diff) < MethodNode.EPSILON)
							continue ;
						
						//logger.info("Callee idx: " + calleeId);
						File f = new File(baseDir + "/" + calleeId);
						GraphTemplate callee = GsonManager.readJsonGeneric(f, graphToken);
						reconstructGraph(callee, false);
						toMerge.add(callee);
						
						if (addCallerLine) {
							for (InstNode ia: callee.getInstPool()) {
								ia.callerLine = inst.getLinenumber();
							}
						}
						
						//This means that the method call is the last inst, will only have 1 graph
						if (lastBeforeReturn != null && inst.equals(lastBeforeReturn)) {
							rawGraph.setLastBeforeReturn(callee.getLastBeforeReturn());
						}
						
						HashSet<String> cReads = callee.getFirstReadLocalVars();
						HashSet<InstNode> cReadNodes = new HashSet<InstNode>();
						for (String cString: cReads) {
							InstNode cReadNode = callee.getInstPool().searchAndGet(cString);
							
							if (cReadNode == null) {
								logger.error("Cannot find read node: " + cString);
								logger.error("Method node: " + inst);
								logger.error("Callee id: " + calleeId);
							}
							
							cReadNodes.add(cReadNode);
						}
						
						double freq = allFreq * frac;
						if (freq > Math.pow(10, -5)) {
							GraphUtil.multiplyGraph(callee, freq);
						}
						
						if (parentFromCaller != null || controlInsts != null) {
							//double freq = allFreq * frac;
							//GraphUtil.multiplyGraph(callee, freq);
							GraphUtil.dataDepFromParentToChildWithFreq(parentFromCaller, cReadNodes, freq);
														
							if (controlInsts != null) {
								for (InstNode controlInst: controlInsts) {
									double conFreq = controlInst.getChildFreqMap().get(mnId) * frac;
									GraphUtil.controlDepFromParentToChildWithFreq(controlInst, cReadNodes, conFreq);
								}
							}
						}
						
						if (childMap.size() > 0) {
							InstNode calleeChildReplace = callee.getLastBeforeReturn();
							if (calleeChildReplace == null) {
								logger.info("Current inst: " + inst);
								logger.info("Find no last inst in callee: " + calleeId);
								continue ;
							}
							
							for (String childKey: childMap.keySet()) {
								InstNode childNode = rawGraph.getInstPool().searchAndGet(childKey);
								double cFreq = childMap.get(childKey) * frac;	
								
								if (childNode == null) {
									/*logger.info("Current inst: " + inst);
									logger.info("Empty child: " + childKey);
									logger.info("Search toMerge");*/
									
									//In the merge
									for (GraphTemplate toM: toMerge) {
										childNode = toM.getInstPool().searchAndGet(childKey);
										if (childNode != null)
											break;
									}
								}
								
								if (childNode == null) {
									logger.error("Current inst: " + inst);
									logger.error("Missing inst: " + childKey);
									continue ;
								}
								
								if (cFreq > Math.pow(10, -5)) {
									calleeChildReplace.increChild(childNode.getThreadId(), 
											childNode.getThreadMethodIdx(), 
											childNode.getIdx(), 
											cFreq);
									childNode.registerParent(calleeChildReplace.getThreadId(), 
											calleeChildReplace.getThreadMethodIdx(), 
											calleeChildReplace.getIdx(), 
											MIBConfiguration.INST_DATA_DEP);
								}
								
								//childNode.getInstDataParentList().remove(mnId);
								cRemoveMn.add(childNode);
							}
						}
					}
					
					if (mn.getRegularState() != null && mn.getRegularState().instFrac > 0) {
						if (allParents != null) {
							//Only for inst changes type, e.g. inst->method
							double pFreq = allFreq * mn.getRegularState().instFrac;
							for (InstNode p: allParents) {
								p.resetChild(mnId, pFreq);
							}
						}
						
						if (controlInsts != null) {
							for (InstNode c: controlInsts) {
								double cFreq = c.getChildFreqMap().get(mnId) * mn.getRegularState().instFrac;
								c.resetChild(mnId, cFreq);
							}
						}
						
						if (childMap.size() > 0) {
							for (String childKey: childMap.keySet()) {
								double childFreq = childMap.get(childKey) * mn.getRegularState().instFrac;
								childMap.put(childKey, childFreq);
							}
						}
						mn.setStartTime(mn.getRegularState().startTime);
						mn.setUpdateTime(mn.getRegularState().updateTime);
						
						mn.setCalleeInfo(null);
					} else {
						//Remove from inst parent
						if (parentFromCaller != null) {
							for (Integer i: parentFromCaller.keySet()) {
								HashSet<InstNode> parents = parentFromCaller.get(i);
								for (InstNode pNode: parents) {
									pNode.getChildFreqMap().remove(mnId);
								}
							}
						}
						
						//Remove from control parent
						if (controlInsts != null) {
							for (InstNode pInst: controlInsts) {
								pInst.getChildFreqMap().remove(mnId);
							}
						}
						
						//Remove from children
						for (InstNode cInst: cRemoveMn) {
							cInst.getInstDataParentList().remove(mnId);
						}
						
						rawGraph.getInstPool().remove(inst);
					}
				} else if (addCallerLine) {
					inst.callerLine = inst.getLinenumber();
				}
			}
			
			for (GraphTemplate callee: toMerge) {
				InstPool calleePool = callee.getInstPool();
				GraphUtil.unionInstPools(rawGraph.getInstPool(), calleePool);
			}
			
			if (MIBConfiguration.getInstance().isReduceGraph()) {
				GraphReducer gr = new GraphReducer(rawGraph);
				gr.reduceGraph();
			}
		} catch (Exception ex) {
			logger.error("Exception: ", ex);
		}
	}
	
	public void cleanObjInit(GraphTemplate constructedGraph) {
		HashSet<InstNode> toRemove = new HashSet<InstNode>();
		InstPool pool = constructedGraph.getInstPool();
		for (InstNode inst: pool) {
			int opcode = inst.getOp().getOpcode();
			if (opcode == Opcodes.INVOKESPECIAL && !toRemove.contains(inst)) {
				String addInfo = inst.getAddInfo();
				
				if (addInfo != null && addInfo.startsWith(objInit)) {
					this.collectSingleParent(inst, pool, toRemove);
				}
			}
		}
		
		for (InstNode r: toRemove) {
			pool.remove(r);
		}
	}
	
	public void collectSingleParent(InstNode inst, InstPool pool, HashSet<InstNode> recorder) {
		recorder.add(inst);
		
		for (String parentKey: inst.getInstDataParentList()) {
			InstNode parentNode = pool.searchAndGet(parentKey);
			
			if (parentNode == null) {
				logger.error("Current node: " + inst);
				logger.error("Missing parent when cleaning obj init: " + parentKey);
				continue ;
			}
			
			//Single child, which is me
			if (parentNode.getChildFreqMap().size() == 1) {
				this.collectSingleParent(parentNode, pool, recorder);
			}
		}
	} 
	
	public static void main(String[] args) {
		//File testFile = new File("./test/cern.colt.matrix.linalg.Algebra:hypot:0:0:130.json");
		//File testFile = new File("/Users/mikefhsu/Mike/Research/ec2/mib_sandbox/jama_graphs/Jama.EigenvalueDecomposition:<init>:0:1:1515059.json");
		//File testFile = new File("/Users/mikefhsu/ccws/jvm-clones/MIB/test/org.ejml.alg.dense.decomposition.svd.SvdImplicitQrDecompose_D64:decompose:0:0:14.json");
		//File testFile = new File("/Users/mikefhsu/ccws/jvm-clones/MIB/test/cc.expbase.TemplateMethod:forMethod:0:0:8.json");
		//File testFile = new File("/Users/mikefhsu/MiKe/Research/ec2/mib_sandbox_v3/jama_graphs/Jama.Matrix:solve:0:1:3811439.json");
		File testFile = new File("/Users/mikefhsu/Mike/Research/ec2/codejam_mining/graphrepo11flat/R5P1Y11.vot.a:getArea:1:100:4426038.json");
		GraphTemplate testGraph = GsonManager.readJsonGeneric(testFile, graphToken);
		GraphConstructor constructor = new GraphConstructor();
		constructor.reconstructGraph(testGraph, true);
		System.out.println("Recorded graph size: " + testGraph.getVertexNum());
		System.out.println("Reduced graph size: " + testGraph.getInstPool().size());
		
		System.out.println("Recorded edge size: " + testGraph.getEdgeNum());
		int eCount = 0;
		int globalDeps = 0;
		
		//Exclude global deps here
		for (InstNode inst: testGraph.getInstPool()) {
			//System.out.println("Check inst rep op: " + inst.repOp);
			//System.out.println("Check original op: " + SearchUtil.getInstructionOp(inst));
			eCount += inst.getChildFreqMap().size();
			
			if (inst instanceof FieldNode) {
				int globalCount = ((FieldNode)inst).getGlobalChildIdx().size();
				globalDeps += globalCount;
			}
		}
		System.out.println("Reduced edge size with global deps: " + eCount);
		//This is what we want to check
		System.out.println("Reduced edge size without global deps: " + (eCount - globalDeps));
		
		System.out.println("Clean obj ref");
		constructor.cleanObjInit(testGraph);
		System.out.println("Reduced size: " + testGraph.getInstPool().size());
		//String fileName = "/Users/mikefhsu/Desktop/" + testGraph.getShortMethodKey() + "_re.json";
		//GsonManager.writeJsonGeneric(testGraph, fileName, graphToken, 8);
		
		/*System.out.println("Clean object init");
		constructor.cleanObjInit(testGraph);
		System.out.println("Clean object vertex size: " + testGraph.getInstPool().size());*/
		
		/*GraphReducer gr = new GraphReducer(testGraph);
		gr.reduceGraph();
		System.out.println("Reduce result: " + gr.getGraph().getInstPool().size());*/
		
		/*String methodName = testGraph.getMethodName();
		TreeSet<Integer> lineTrace = new TreeSet<Integer>();
		for (InstNode inst: testGraph.getInstPool()) {
			if (inst.getFromMethod().contains(methodName)) {
				int line = inst.getLinenumber();
				lineTrace.add(line);
			}
		}
		System.out.println("Line trace: " + lineTrace);*/
	}

}
