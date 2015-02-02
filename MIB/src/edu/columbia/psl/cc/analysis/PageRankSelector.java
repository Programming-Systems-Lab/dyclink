package edu.columbia.psl.cc.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.HotZone;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.MethodNode;
import edu.columbia.psl.cc.pojo.NameMap;
import edu.columbia.psl.cc.util.GraphConstructor;
import edu.columbia.psl.cc.util.GraphUtil;
import edu.columbia.psl.cc.util.GsonManager;
import edu.columbia.psl.cc.util.SearchUtil;
import edu.columbia.psl.cc.util.StringUtil;
import edu.columbia.psl.cc.util.TemplateLoader;
import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.algorithms.scoring.PageRankWithPriors;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Hypergraph;


public class PageRankSelector {
	
	private static AtomicInteger threadIndex = new AtomicInteger();
	
	private static Logger logger = Logger.getLogger(PageRankSelector.class);
	
	private static double alpha = MIBConfiguration.getInstance().getPgAlpha();
	
	private static int maxIteration = MIBConfiguration.getInstance().getPgMaxIter();
	
	private static double epsilon = MIBConfiguration.getInstance().getPgEpsilon();
	
	private static int instLimit = MIBConfiguration.getInstance().getInstLimit();
	
	private static double simThreshold = MIBConfiguration.getInstance().getSimThreshold();
	
	private static String header = "template,test,pgrank_template,c_template,ct_line,s_test,s_line,c_test,c_line,e_test,e_line,seg_size,inst_dist,dist,similarity\n";
	
	private static Comparator<InstWrapper> pageRankSorter = new Comparator<InstWrapper>() {
		public int compare(InstWrapper i1, InstWrapper i2) {
			if (i1.pageRank < i2.pageRank) {
				return 1;
			} else if (i1.pageRank > i2.pageRank) {
				return -1;
			} else {
				if (i1.inst.getOp().getOpcode() > i2.inst.getOp().getOpcode()) {
					return 1;
				} else if (i1.inst.getOp().getOpcode() < i2.inst.getOp().getOpcode()) {
					return -1;
				} else {
					return 0;
				}
			}
		}
	};
	
	private static double levenSimilarity(int dist, int base) {
		double sim = 1 - ((double)dist/base);
		return sim;
	}
	
	private InstPool myPool;
	
	private HashMap<InstNode, Double> priors;
	
	private boolean partialPool;
	
	private boolean weighted;
	
	public PageRankSelector(InstPool myPool, boolean partialPool, boolean weighted) {
		this.myPool = myPool;
		this.partialPool = partialPool;
		this.weighted = weighted;
	}
	
	/**
	 * Key is opcode, double is their prior
	 * @param priors
	 */
	public void setPriors(HashMap<InstNode, Double> priors) {
		this.priors = priors;
	}
		
	public DirectedSparseGraph<InstNode, WeightedEdge> convertToJungGraph() {
		DirectedSparseGraph<InstNode, WeightedEdge> retGraph = new DirectedSparseGraph<InstNode, WeightedEdge>();
		
		int edgeId = 0;
		for (InstNode inst: myPool) {
			retGraph.addVertex(inst);
			int childCount = inst.getChildFreqMap().size();
			double totalFreq = 0;
			
			for (String childKey: inst.getChildFreqMap().keySet()) {
				
				InstNode childNode = myPool.searchAndGet(childKey);
				double childFreq = inst.getChildFreqMap().get(childKey);
				if (!partialPool) {
					if (childNode != null) {
						totalFreq += childFreq;
					} else {
						logger.warn("Empty child of " + inst);
						logger.warn("Please check if " + childKey + " is get/set static/field");
					}
				} else {
					if (childNode != null) {
						totalFreq += childFreq;
					}
				}
			}
			
			for (String childKey: inst.getChildFreqMap().keySet()) {
				//InstNode childNode = cache.get(childKey);
				InstNode childNode = myPool.searchAndGet(childKey);
				
				if (childNode != null) {
					double childFreq = inst.getChildFreqMap().get(childKey);
					WeightedEdge we = new WeightedEdge();
					we.edgeId = edgeId++;
					if (this.weighted) {
						we.edgeWeight = childFreq/totalFreq;
					} else {
						we.edgeWeight = ((double)1)/childCount;
					}
					retGraph.addEdge(we, inst, childNode);
				}
			}
		}
		
		return retGraph;
	}
	
	public DirectedSparseGraph<InstNode, Integer> convertToJungGraph2() {
		DirectedSparseGraph<InstNode, Integer> retGraph = new DirectedSparseGraph<InstNode, Integer>();
		
		int edgeId = 0;
		for (InstNode inst: myPool) {
			retGraph.addVertex(inst);
			int childCount = inst.getChildFreqMap().size();
			for (String childKey: inst.getChildFreqMap().keySet()) {
				/*InstNode childNode = null;
				if (cache.containsKey(childKey)) {
					childNode = cache.get(childKey);
				} else {
					String[] keys = StringUtil.parseIdxKey(childKey);
					childNode = myPool.searchAndGet(keys[0], 
							Long.valueOf(keys[1]), Integer.valueOf(keys[2]), Integer.valueOf(keys[3]));
					
					cache.put(childKey, childNode);
				}*/
				
				InstNode childNode = myPool.searchAndGet(childKey);
				if (!partialPool) {
					if (childNode != null) {
						retGraph.addEdge(new Integer(edgeId++), inst, childNode);
					} else {
						logger.warn("Empty child of " + inst);
						logger.warn("Please check if " + childKey + " is get/set static/field");
					}
				} else {
					if (childNode != null) {
						retGraph.addEdge(new Integer(edgeId++), inst, childNode);
					}
				}
			}
		}
		
		return retGraph;
	}
	
	public List<InstWrapper> computePageRank() {
		Hypergraph<InstNode, WeightedEdge> jungGraph = this.convertToJungGraph();
		//Hypergraph<InstNode, Integer> jungGraph = this.convertToJungGraph2();
		
		/*System.out.println("Check edge");
		for (WeightedEdge we: jungGraph.getEdges()) {
			System.out.println("Source: " + jungGraph.getSource(we));
			System.out.println("Sink: " + jungGraph.getDest(we));
			System.out.println("Edge: " + we.edgeId + " " + we.edgeWeight);
		}*/
		
		Transformer<WeightedEdge, Double> edgeWeights = new Transformer<WeightedEdge, Double>() {
			public Double transform(final WeightedEdge edge) {
				//System.out.println("Weighted edge: " + edge.edgeId + " " + edge.edgeWeight);
				return edge.edgeWeight;
			}
		};
		
		//PageRankWithPriors<InstNode, Integer> ranker = null;
		PageRankWithPriors<InstNode, WeightedEdge> ranker = null;
		if (this.priors == null) {
			//logger.info("Rank without priors");
			ranker = new PageRank<InstNode, WeightedEdge>(jungGraph, edgeWeights, alpha);
			//ranker.setEdgeWeights(edgeWeights);
			//ranker = new PageRank<InstNode, Integer>(jungGraph, alpha);
		} else {
			//logger.info("Rank with priors");
			Transformer<InstNode, Double> transformer = new Transformer<InstNode, Double>() {
				@Override
				public Double transform(InstNode inst) {
					double prior = priors.get(inst);
					return prior;
				}
			};
			ranker = new PageRankWithPriors<InstNode, WeightedEdge>(jungGraph, transformer, alpha);
			ranker.setEdgeWeights(edgeWeights);
			//ranker = new PageRankWithPriors<InstNode, Integer>(jungGraph, transformer, alpha);
		}
		
		List<InstWrapper> rankList = new ArrayList<InstWrapper>();
		ranker.setMaxIterations(maxIteration);
		ranker.setTolerance(epsilon);
		ranker.evaluate();
		
		for (InstNode inst: jungGraph.getVertices()) {
			InstWrapper iw = new InstWrapper(inst, ranker.getVertexScore(inst));
			rankList.add(iw);
		}
		
		Collections.sort(rankList, pageRankSorter);
		return rankList;
	}
	
	public InstPool selectRepPool() {
		InstPool ret = new InstPool();
		List<InstWrapper> sorted = this.computePageRank();
		for (int i = 0; i < instLimit; i++) {
			ret.add(sorted.get(i).inst);
		}
		return ret;
	}
	
	public static HashMap<InstNode, SegInfo> locateSegments(HashSet<InstNode> assignments, 
			List<InstNode> sortedTarget, 
			GraphProfile subProfile) {
		HashMap<InstNode, SegInfo> candSegs = new HashMap<InstNode, SegInfo>();
		for (InstNode inst: assignments) {
			List<InstNode> seg = new ArrayList<InstNode>();
			
			for (int i = 0; i < sortedTarget.size(); i++) {
				InstNode curNode = sortedTarget.get(i);
				if (curNode.equals(inst)) {
					//collect backward
					int start = i - subProfile.before;
					if (start < 0)
						start = 0;
					
					int end = i + subProfile.after;
					if (end > sortedTarget.size() - 1)
						end = sortedTarget.size() - 1;
					
					seg.addAll(sortedTarget.subList(start, end + 1));
					break ;
				}
			}
			
			//Temporarily set it as 0.8. Not consider the too-short assignment
			if (seg.size() < subProfile.pgRep.length * 0.8) {
				logger.info("Give up too-short assignment: " + inst + " size " + seg.size());
				continue ;
			} else {
				double[] segDist = StaticTester.genDistribution(seg);
				//double[] subDist = subProfile.instDist;
				
				SegInfo si = new SegInfo();
				si.seg = seg;
				si.normInstDistribution = StaticTester.normalizeDist(segDist, seg.size());;
				si.instDistWithSub = StaticTester.normalizeEucDistance(subProfile.normDist, 
						si.normInstDistribution);
				
				if (si.instDistWithSub <= 0.2) {
					candSegs.put(inst, si);
				} else {
					logger.info("Give up less likely inst: " + inst + " " + si.instDistWithSub);
				}
				
				/*if (ChiTester.shouldTest(subDist, subProfile.pgRep.length, segDist, seg.size())) {
					candSegs.put(inst, seg);
				} else {
					logger.info("Give up less likely inst: " + inst);
				}*/
			}
		}
		return candSegs;
	}
		
	public static GraphProfile profileGraph(GraphTemplate subGraph) {
		if (subGraph.getInstPool().size() == 0) {
			return null;
		}
		List<InstNode> sortedSub = GraphUtil.sortInstPool(subGraph.getInstPool(), true);
		
		//Pick the most important node from sorteSob
		logger.info("Sub graph profile: " + subGraph.getMethodKey());
		PageRankSelector subSelector = new PageRankSelector(subGraph.getInstPool(), false, true);
		List<InstWrapper> subRank = subSelector.computePageRank();
		int[] subPGRep = SearchUtil.generatePageRankRep(subRank);
		//logger.info("Sub graph PageRank: " + Arrays.toString(subPGRep));
		
		//Use the most important inst as the central to collect insts in target
		InstNode subCentroid = subRank.get(0).inst;
		int before = 0, after = 0;
		boolean recordBefore = true;
		for (int i = 0; i < sortedSub.size(); i++) {
			InstNode curNode = sortedSub.get(i);
			
			if (curNode.equals(subCentroid)) {
				recordBefore = false;
				continue ;
			}
			
			if (recordBefore) {
				before++;
			} else {
				after++;
			}
		}
		
		GraphProfile gp = new GraphProfile();
		gp.centroidWrapper = subRank.get(0);
		gp.before = before;
		gp.after = after;
		gp.pgRep = subPGRep;
		//gp.instDist = subGraph.getDist();
		gp.instDist = StaticTester.genDistribution(subGraph.getDist());
		gp.normDist = StaticTester.normalizeDist(gp.instDist, gp.pgRep.length);
		
		return gp;
	}
	
	
	
	public static void initiateSubGraphMining(String templateDir, String testDir) {		
		StringBuilder sb = new StringBuilder();
		sb.append(header);
		TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
		
		HashMap<String, GraphTemplate> templates = null;
		HashMap<String, GraphTemplate> tests = null;
		
		boolean probeTemplate = TemplateLoader.probeDir("./template");
		boolean probeTest = TemplateLoader.probeDir("./test");
		if (probeTemplate && probeTest) {
			logger.info("Comparison mode: templates vs tests");
			templates = TemplateLoader.loadTemplate(new File("./template"), graphToken);
			tests = TemplateLoader.loadTemplate(new File("./test"), graphToken);
		} else if (probeTemplate) {
			logger.info("Exhaustive mode: templates vs. templates");
			templates = TemplateLoader.loadTemplate(new File("./template"), graphToken);
			tests = TemplateLoader.loadTemplate(new File("./template"), graphToken);
		} else if (probeTest) {
			logger.info("Exhaustive mode: tests vs. tests");
			templates = TemplateLoader.loadTemplate(new File("./test"), graphToken);
			tests = TemplateLoader.loadTemplate(new File("./test"), graphToken);
		} else {
			logger.info("Empty repos for both templates and tests");
			return ;
		}
		
		List<SubGraphCrawler> crawlers = new ArrayList<SubGraphCrawler>();
		for (String templateName: templates.keySet()) {
			GraphTemplate tempGraph = templates.get(templateName);
			
			if (tempGraph.getVertexNum() < MIBConfiguration.getInstance().getInstThreshold()) {
				continue ;
			}
			
			GraphConstructor.reconstructGraph(tempGraph);
			
			GraphProfile tempProfile = profileGraph(tempGraph);
			if (tempProfile == null) {
				logger.warn("Empty graph: " + tempGraph.getMethodKey());
				continue ;
			}
			
			logger.info("Template name: " + tempGraph.getMethodKey());
			logger.info("Inst node size: " + tempGraph.getInstPool().size());
			
			for (String testName: tests.keySet()) {
				if (testName.equals(templateName)) {
					continue ;
				}
				
				GraphTemplate testGraph = tests.get(testName);
				GraphConstructor.reconstructGraph(testGraph);
				
				logger.info("Test name: " + testGraph.getMethodKey());
				logger.info("Inst node size: " + testGraph.getInstPool().size());
				
				SubGraphCrawler crawler = new SubGraphCrawler();
				crawler.subGraphName = tempGraph.getMethodKey();
				crawler.targetGraphName = testGraph.getMethodKey();
				crawler.subGraphProfile = tempProfile;
				crawler.targetGraph = testGraph;
				crawlers.add(crawler);
			}
		}
		
		ExecutorService executor = Executors.newFixedThreadPool(MIBConfiguration.getInstance().getParallelFactor());
		List<Future<List<HotZone>>> resultRecorder = new ArrayList<Future<List<HotZone>>>();
		
		for (SubGraphCrawler crawler: crawlers) {
			Future<List<HotZone>> hits = executor.submit(crawler);
			resultRecorder.add(hits);
		}
		executor.shutdown();
		while (!executor.isTerminated());
		
		try {
			for (Future<List<HotZone>> future: resultRecorder) {
				List<HotZone> zones = future.get();
				
				for (HotZone hit: zones) {
					logger.info("Start inst: " + hit.getStartInst());
					logger.info("Centroid inst: " + hit.getCentralInst());
					logger.info("End inst: " + hit.getEndInst());
					logger.info("Distance: " + hit.getLevDist());
					logger.info("Similarity: " + hit.getSimilarity());
					
					StringBuilder rawRecorder = new StringBuilder();
					rawRecorder.append(hit.getSubGraphName() + 
							"," + hit.getTargetGraphName() + 
							"," + hit.getSubPgRank() +
							"," + hit.getSubCentroid() + 
							"," + hit.getSubCentroid().getLinenumber() +
							"," + hit.getStartInst() + 
							"," + hit.getStartInst().getLinenumber() + 
							"," + hit.getCentralInst() + 
							"," + hit.getCentralInst().getLinenumber() +
							"," + hit.getEndInst() + 
							"," + hit.getEndInst().getLinenumber() +
							"," + hit.getSegs().size() + 
							"," + hit.getInstDistance() +
							"," + hit.getLevDist() + 
							"," + hit.getSimilarity() + "\n");
					sb.append(rawRecorder);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		GsonManager.writeResult(sb);
	}
				
	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		String templateDir = MIBConfiguration.getInstance().getTemplateDir();
		String testDir = MIBConfiguration.getInstance().getTestDir();
				
		logger.info("Start PageRank analysis for Bytecode subgraph mining");
		logger.info("Similarity threshold: " + simThreshold);
		logger.info("Alpha: " + alpha);
		logger.info("Max iteration: " + maxIteration);
		logger.info("Epsilon: " + epsilon);
		
		initiateSubGraphMining(templateDir, testDir);
		System.out.println("Execution time: " + (System.currentTimeMillis() - startTime));
	}
	
	private static class SegInfo {
		List<InstNode> seg;
		
		double[] normInstDistribution;
		
		double instDistWithSub;
	}
	
	private static class GraphProfile {
		InstWrapper centroidWrapper;
		
		int before;
		
		int after; 
		
		int[] pgRep;
		
		double[] instDist;
		
		double[] normDist;
	}
	
	private static class WeightedEdge {
		int edgeId;
		
		double edgeWeight;
	}
	
	private static class SubGraphCrawler implements Callable<List<HotZone>>{
		
		String subGraphName;
		
		String targetGraphName;
		
		GraphProfile subGraphProfile;
		
		GraphTemplate targetGraph;
		
		int crawlerId = threadIndex.getAndIncrement();;
		
		@Override
		public List<HotZone> call() throws Exception {
			List<HotZone> hits = this.subGraphSearch(subGraphProfile, targetGraph, subGraphName, targetGraphName);
			return hits;
		}
		
		public List<HotZone> subGraphSearch(GraphProfile subProfile, 
				GraphTemplate targetGraph, 
				String subGraphName, 
				String targetGraphName) {
			List<InstNode> sortedTarget = GraphUtil.sortInstPool(targetGraph.getInstPool(), true);
			
			double geoPercent = ((double)(subProfile.before + 1))/ (subProfile.before + 1 + subProfile.after);
			HashSet<InstNode> miAssignments = SearchUtil.possibleSingleAssignment(subProfile.centroidWrapper.inst, 
					sortedTarget, 
					geoPercent);
			logger.info("Target graph vs Sub graph: " + targetGraphName + " " + subGraphName);
			logger.info("Thread index: " + crawlerId);
			logger.info("Possible assignments: " + miAssignments.size());
			logger.info("Sub-graph size: " + subProfile.pgRep.length);
			HashMap<InstNode, SegInfo> candSegs = locateSegments(miAssignments, sortedTarget, subProfile);
			logger.info("Real assignments: " + candSegs.size());
			List<HotZone> hits = new ArrayList<HotZone>();
			
			for (InstNode cand: candSegs.keySet()) {
				SegInfo segInfo = candSegs.get(cand);
				List<InstNode> segments = segInfo.seg;
				InstPool segPool = new InstPool();
				segPool.addAll(segments);
				
				PageRankSelector ranker = new PageRankSelector(segPool, true, true);
				List<InstWrapper> ranks = ranker.computePageRank();
				int[] candPGRep = SearchUtil.generatePageRankRep(ranks);
				
				int dist = 0;
				if (candPGRep.length == 0) {
					dist = subProfile.pgRep.length;
				} else {
					dist = LevenshteinDistance.calculateSimilarity(subProfile.pgRep, candPGRep);
				}
				
				double sim = levenSimilarity(dist, subProfile.pgRep.length);
				
				if (sim >= simThreshold) {
					HotZone zone = new HotZone();
					zone.setSubCentroid(subProfile.centroidWrapper.inst);
					zone.setSubPgRank(subProfile.centroidWrapper.pageRank);
					zone.setStartInst(segments.get(0));
					zone.setCentralInst(cand);
					zone.setEndInst(segments.get(segments.size() - 1));
					zone.setLevDist(dist);
					zone.setSimilarity(sim);
					zone.setInstDistance(segInfo.instDistWithSub);
					zone.setSegs(segPool);
					zone.setSubGraphName(subGraphName);
					zone.setTargetGraphName(targetGraphName);
					hits.add(zone);
				}
			}
			logger.info("Crawler ends: " + this.crawlerId);
			return hits;
		}
	}
}
