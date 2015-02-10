package edu.columbia.psl.cc.analysis;

import java.io.Console;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
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
import edu.columbia.psl.cc.util.DBConnector.Comparison;
import edu.columbia.psl.cc.util.DBConnector;
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
	
	private static int subCompNum = 0;
	
	private static int realSubCompNum = 0;
	
	private static Object countLock = new Object();
	
	private static AtomicInteger threadIndex = new AtomicInteger();
	
	private static Logger logger = Logger.getLogger(PageRankSelector.class);
	
	private static double alpha = MIBConfiguration.getInstance().getPgAlpha();
	
	private static int maxIteration = MIBConfiguration.getInstance().getPgMaxIter();
	
	private static double epsilon = MIBConfiguration.getInstance().getPgEpsilon();
	
	private static int instLimit = MIBConfiguration.getInstance().getInstLimit();
	
	private static double simThreshold = MIBConfiguration.getInstance().getSimThreshold();
	
	private static String sumHeader = "lib1,lib2,inst_thresh,inst_cat,method1,method2,method_f_1,method_f_2,m_compare,sub_crawl,sub_crawl_filter,time\n";
	
	private static String header = "sub,target,pgrank_sub,center_sub,center_sub_line,start_target,start_target_line,center_target,center_target_line,end_target,end_target_line,seg_size,inst_dist,dist,similarity\n";
	
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
				//logger.info("Give up too-short assignment: " + inst + " size " + seg.size());
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
					//logger.info("Give up less likely inst: " + inst + " " + si.instDistWithSub);
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
	
	public static void filterGraphs(HashMap<String, GraphTemplate> graphs) {
		HashSet<String> graphHistory = new HashSet<String>();
		for (Iterator<String> keyIT = graphs.keySet().iterator(); keyIT.hasNext();) {
			String key = keyIT.next();
			GraphTemplate graph = graphs.get(key);
			String recordKey = graph.getShortMethodKey() + ":" + graph.getVertexNum() + ":" + graph.getEdgeNum();
			if (graphHistory.contains(recordKey)) {
				keyIT.remove();
			} else if (graph.getVertexNum() <= MIBConfiguration.getInstance().getInstThreshold()) {
				keyIT.remove();
			} else {
				graphHistory.add(recordKey);
			}
		}
	}
	
	public static List<GraphProfile> parallelizeProfiling(HashMap<String, GraphTemplate> graphs) {
		List<GraphProfile> profiles = new ArrayList<GraphProfile>();
		
		try {
			ExecutorService profileExecutor = 
					Executors.newFixedThreadPool(MIBConfiguration.getInstance().getParallelFactor());
			List<Future<GraphProfile>> futureProfiles = new ArrayList<Future<GraphProfile>>();
			for (String fileName: graphs.keySet()) {
				GraphTemplate graph = graphs.get(fileName);
				ProfileWorker worker = new ProfileWorker();
				worker.fileName = fileName;
				worker.graph = graph;
				Future<GraphProfile> futureProfile = profileExecutor.submit(worker);
				futureProfiles.add(futureProfile);
			}
			profileExecutor.shutdown();
			while(!profileExecutor.isTerminated());
			
			for (Future<GraphProfile> futureProfile: futureProfiles) {
				GraphProfile graphProfile = futureProfile.get();
				if (graphProfile == null)
					continue ;
				
				profiles.add(graphProfile);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return profiles;
	}
	
	public static void constructCrawlerList(List<GraphProfile> subProfiles, 
			List<GraphProfile> targetProfiles, 
			List<SubGraphCrawler> crawlers) {
		for (GraphProfile subProfile: subProfiles) {
			for (GraphProfile targetProfile: targetProfiles) {
				if (subProfile.fileName.equals(targetProfile.fileName))
					continue ;
				
				SubGraphCrawler crawler = new SubGraphCrawler();
				crawler.subGraphName = subProfile.graph.getMethodKey();
				crawler.targetGraphName = targetProfile.graph.getMethodKey();
				crawler.subGraphProfile = subProfile;
				crawler.targetGraph = targetProfile.graph;
				crawlers.add(crawler);
			}
		}
	}
	
	public static void initiateSubGraphMining(String templateDir, 
			String testDir, 
			String url, 
			String username, 
			String password) {
		long startTime = System.currentTimeMillis();
		
		String lib1 = (new File(templateDir)).getName();
		String lib2 = (new File(testDir)).getName();
		
		Comparison compResult = new Comparison();
		compResult.inst_thresh = MIBConfiguration.getInstance().getInstThreshold();
		compResult.inst_cat = MIBConfiguration.getInstance().getSimStrategy();
		compResult.lib1 = lib1;
		compResult.lib2 = lib2;
		
		StringBuilder sb = new StringBuilder();
		TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
		
		HashMap<String, GraphTemplate> templates = null;
		HashMap<String, GraphTemplate> tests = null;
		
		boolean probeTemplate = TemplateLoader.probeDir(lib1);
		boolean probeTest = TemplateLoader.probeDir(lib2);
		if (probeTemplate && probeTest) {
			logger.info("Comparison mode: templates vs tests");
			templates = TemplateLoader.loadTemplate(new File(lib1), graphToken);
			tests = TemplateLoader.loadTemplate(new File(lib2), graphToken);
		} else if (probeTemplate) {
			logger.info("Exhaustive mode: templates vs. templates");
			templates = TemplateLoader.loadTemplate(new File(lib1), graphToken);
			tests = TemplateLoader.loadTemplate(new File(lib1), graphToken);
		} else if (probeTest) {
			logger.info("Exhaustive mode: tests vs. tests");
			templates = TemplateLoader.loadTemplate(new File(lib2), graphToken);
			tests = TemplateLoader.loadTemplate(new File(lib2), graphToken);
		} else {
			logger.info("Empty repos for both templates and tests");
			return ;
		}
		
		compResult.method1 = templates.size();
		compResult.method2 = tests.size();
		
		filterGraphs(templates);
		logger.info("Template size: " + templates.size());
		//logger.info(templates.keySet());
		filterGraphs(tests);
		logger.info("Test size: " + tests.size());
		//logger.info(tests.keySet());
		
		compResult.method_f_1 = templates.size();
		compResult.method_f_2 = tests.size();
		
		try {
			//Construct and profile tests (target graphs)
			List<GraphProfile> testProfiles = parallelizeProfiling(tests);
			
			//Construct and profile template (sub graphs)
			List<GraphProfile> templateProfiles = parallelizeProfiling(templates);
			
			List<SubGraphCrawler> crawlers = new ArrayList<SubGraphCrawler>();
			
			//Sub: template, Target: test
			constructCrawlerList(templateProfiles, testProfiles, crawlers);
			
			//Sub: test, Target: template
			constructCrawlerList(testProfiles, templateProfiles, crawlers);
			logger.info("Total number of comparisons: " + crawlers.size());
			compResult.m_compare = crawlers.size();
			
			ExecutorService executor = Executors.newFixedThreadPool(MIBConfiguration.getInstance().getParallelFactor());
			List<Future<List<HotZone>>> resultRecorder = new ArrayList<Future<List<HotZone>>>();
			
			for (SubGraphCrawler crawler: crawlers) {
				Future<List<HotZone>> hits = executor.submit(crawler);
				resultRecorder.add(hits);
			}
			executor.shutdown();
			while (!executor.isTerminated());
			
			logger.info("Sub graph comp num: " + subCompNum);
			logger.info("Real sub graph comp num: " + realSubCompNum);
			double execTime = (System.currentTimeMillis() - startTime)/1000.0;
			System.out.println("Execution time: " + (execTime));
			
			compResult.sub_crawl = subCompNum;
			compResult.sub_crawl_filter = realSubCompNum;
			compResult.time = execTime;
			
			//Record summary
			sb.append(sumHeader);
			StringBuilder sumBuilder = new StringBuilder();
			sumBuilder.append(compResult.lib1 + ",");
			sumBuilder.append(compResult.lib2 + ",");
			sumBuilder.append(compResult.inst_thresh + ",");
			sumBuilder.append(compResult.inst_cat + ",");
			sumBuilder.append(compResult.method1 + ",");
			sumBuilder.append(compResult.method2 + ",");
			sumBuilder.append(compResult.method_f_1 + ",");
			sumBuilder.append(compResult.method_f_2 + ",");
			sumBuilder.append(compResult.m_compare + ",");
			sumBuilder.append(compResult.sub_crawl + ",");
			sumBuilder.append(compResult.sub_crawl_filter + ",");
			sumBuilder.append(compResult.time + "\n");
			sumBuilder.append("\n");
			sb.append(sumBuilder.toString());
			sb.append(header);
			
			//Write summary to DB
			int compResultId = -1;
			if (url != null && username != null && password != null) {
				DBConnector connector = new DBConnector();
				compResultId = connector.writeCompTableResult(url, username, password, compResult);
			}
			
			for (Future<List<HotZone>> future: resultRecorder) {
				List<HotZone> zones = future.get();
				
				//Record hotzones
				for (HotZone hit: zones) {
					//logger.info("Start inst: " + hit.getStartInst());
					//logger.info("Centroid inst: " + hit.getCentralInst());
					//logger.info("End inst: " + hit.getEndInst());
					//logger.info("Distance: " + hit.getLevDist());
					//logger.info("Similarity: " + hit.getSimilarity());
					
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
				
				//Write hotzones to DB
				if (compResultId >= 0) {
					DBConnector connector = new DBConnector();
					connector.writeDetailTableResult(url, username, password, compResultId, zones);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		String compareName = lib1 + "-" + lib2;
		GsonManager.writeResult(compareName, sb);
	}
				
	public static void main(String[] args) {
		Console console = System.console();
		if (console == null) {
			System.err.println("Null console");
			System.exit(1);
		}
		
		String url = MIBConfiguration.getInstance().getDburl();
		String username = MIBConfiguration.getInstance().getDbusername();
		System.out.println("DB URL: " + url);
		
		char[] passArray = console.readPassword("DB password: ");
		String password = new String(passArray);
		
		DBConnector connector = new DBConnector();
		if (!connector.probeDB(url, username, password)) {
			System.out.println("No DB connection. Wanna execute experiment still?");
			Scanner scanner = new Scanner(System.in);
			
			try {
				boolean shouldExecute = scanner.nextBoolean();
				if (!shouldExecute) {
					System.out.println("Bye bye~~~");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				System.out.println("Leave system because of unexpected error");
				System.exit(1);
			}
		}
		
		String templateDir = MIBConfiguration.getInstance().getTemplateDir();
		String testDir = MIBConfiguration.getInstance().getTestDir();
				
		logger.info("Start PageRank analysis for Bytecode subgraph mining");
		logger.info("Similarity threshold: " + simThreshold);
		logger.info("Alpha: " + alpha);
		logger.info("Max iteration: " + maxIteration);
		logger.info("Epsilon: " + epsilon);
		
		initiateSubGraphMining(templateDir, testDir, url, username, password);
	}
	
	private static class SegInfo {
		List<InstNode> seg;
		
		double[] normInstDistribution;
		
		double instDistWithSub;
	}
	
	private static class GraphProfile {
		
		String fileName;
		
		GraphTemplate graph;
		
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
	
	private static class ConstructWorker implements Runnable {
		
		GraphTemplate rawGraph;

		@Override
		public void run() {
			logger.info("Test name: " + rawGraph.getMethodKey());
			logger.info("Inst node size: " + rawGraph.getInstPool().size());
			GraphConstructor constructor = new GraphConstructor();
			constructor.reconstructGraph(rawGraph);
		}
	}
	
	private static class ProfileWorker implements Callable<GraphProfile> {
		
		String fileName;
		
		GraphTemplate graph;
		
		public GraphProfile call() throws Exception {
			logger.info("Graph name: " + this.graph.getMethodKey());
			GraphConstructor constructor = new GraphConstructor();
			constructor.reconstructGraph(this.graph);
			return profileGraph();
		}
		
		public GraphProfile profileGraph() {
			if (this.graph.getInstPool().size() == 0) {
				logger.warn("Empty graph: " + this.graph.getMethodKey());
				return null;
			}
			List<InstNode> sortedSub = GraphUtil.sortInstPool(this.graph.getInstPool(), true);
			
			//Pick the most important node from sorteSob
			logger.info("Graph profile: " + this.graph.getMethodKey());
			PageRankSelector subSelector = new PageRankSelector(this.graph.getInstPool(), false, true);
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
			gp.fileName = this.fileName;
			gp.graph = this.graph;
			gp.centroidWrapper = subRank.get(0);
			gp.before = before;
			gp.after = after;
			gp.pgRep = subPGRep;
			//gp.instDist = subGraph.getDist();
			gp.instDist = StaticTester.genDistribution(this.graph.getDist());
			gp.normDist = StaticTester.normalizeDist(gp.instDist, gp.pgRep.length);
			
			return gp;
		}
	}
	
	private static class SubGraphCrawler implements Callable<List<HotZone>> {
		
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
			
			synchronized(countLock) {
				subCompNum += miAssignments.size();
				realSubCompNum += candSegs.size();
			}
			
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
