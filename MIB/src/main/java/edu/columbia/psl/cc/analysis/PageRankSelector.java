package edu.columbia.psl.cc.analysis;

import java.io.Console;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.lang3.text.translate.NumericEntityUnescaper.OPTION;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.ArgConfiguration;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.HotZone;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.MethodNode;
import edu.columbia.psl.cc.pojo.NameMap;
import edu.columbia.psl.cc.premain.MIBDriver;
import edu.columbia.psl.cc.util.DBConnector.Comparison;
import edu.columbia.psl.cc.util.DBConnector;
import edu.columbia.psl.cc.util.GraphConstructor;
import edu.columbia.psl.cc.util.GraphUtil;
import edu.columbia.psl.cc.util.GsonManager;
import edu.columbia.psl.cc.util.Locator;
import edu.columbia.psl.cc.util.SearchUtil;
import edu.columbia.psl.cc.util.StringUtil;
import edu.columbia.psl.cc.util.TemplateLoader;
import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.algorithms.scoring.PageRankWithPriors;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Hypergraph;


public class PageRankSelector {
	
	private static boolean shouldDB = true;
	
	private static int subCompNum = 0;
	
	private static int realSubCompNum = 0;
	
	private static int dbWriteNum = 10;
	
	private static Semaphore dbLock = new Semaphore(dbWriteNum, true);
	
	private static Object countLock = new Object();
	
	private static AtomicInteger profilerIndex = new AtomicInteger();
	
	private static AtomicInteger threadIndex = new AtomicInteger();
	
	private static Logger logger = LogManager.getLogger(PageRankSelector.class);
	
	private static double alpha = MIBConfiguration.getInstance().getPgAlpha();
	
	private static int maxIteration = MIBConfiguration.getInstance().getPgMaxIter();
	
	private static double epsilon = MIBConfiguration.getInstance().getPgEpsilon();
	
	private static int instLimit = MIBConfiguration.getInstance().getInstLimit();
	
	private static double staticThreshold = MIBConfiguration.getInstance().getStaticThreshold();
	
	private static double simThreshold = MIBConfiguration.getInstance().getSimThreshold();
	
	private static int simStrategy = MIBConfiguration.getInstance().getSimStrategy();
	
	private static int assignmentThreshold = MIBConfiguration.getInstance().getAssignmentThreshold();
	
	//private static double simDiff = Math.pow(10, -3);
	
	private static String sumHeader = "lib1,lib2,inst_thresh,inst_cat,method1,method2,method_f_1,method_f_2,m_compare,sub_crawl,sub_crawl_filter,s_threshold,t_threshold,time,timestamp\n";
	
	private static String header = "sub,sid,target,tid,s_start,s_centroid,s_centroid_line,s_centroid_caller,s_end,s_trace,t_start,t_centroid,t_centroid_line,t_centroid_caller,t_end,t_trace,seg_size,inst_dist,similarity\n";
		
	public static Comparator<InstWrapper> pageRankSorter() {
		Comparator<InstWrapper> ret = new Comparator<InstWrapper>() {
			public int compare(InstWrapper i1, InstWrapper i2) {
				BigDecimal bd1 = new BigDecimal(i1.pageRank).setScale(8, BigDecimal.ROUND_HALF_UP);
				BigDecimal bd2 = new BigDecimal(i2.pageRank).setScale(8, BigDecimal.ROUND_HALF_UP);
				
				int res = bd1.compareTo(bd2);
				if (res == -1) {
					return 1;
				} else if (res == 1) {
					return -1;
				} else {				
					int i1Number = i1.inst.repOp;
					int i2Number = i2.inst.repOp;
					
					if (i1Number > i2Number) {
						return 1;
					} else if (i1Number < i2Number) {
						return -1;
					} else {
						return 0;
					}
				}
			}
		};
		return ret;
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
		
		/*for (InstNode i: retGraph.getVertices()) {
			System.out.println("Instruction: " + i);
			System.out.println(retGraph.degree(i));
		}*/
		
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
			double ranking = ranker.getVertexScore(inst);
			if (Double.isNaN(ranking)) {
				logger.error("Suspect inst: " + inst);
				logger.error("Inst data parents: " + inst.getInstDataParentList());
				logger.error("Write data parents: " + inst.getWriteDataParentList());
				logger.error("Child map: " + inst.getChildFreqMap());
				logger.error("Page rank: " + ranking);
				System.exit(-1);
			}
			InstWrapper iw = new InstWrapper(inst, ranking);
			rankList.add(iw);
		}
		
		Collections.sort(rankList, pageRankSorter());
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
			
	public static void initiateSubGraphMining(String templateDir, 
			String testDir, 
			String url, 
			String username, 
			String password, 
			boolean constructOnly) {
		long startTime = System.currentTimeMillis();
		
		File templateLoc = new File(templateDir);
		File testLoc = new File(testDir);

		List<SubGraphCrawler> crawlers = new ArrayList<SubGraphCrawler>();		
		Comparison compResult = GraphLoadController.normalLoad(templateLoc, testLoc, crawlers);
		
		if (constructOnly) {
			return ;
		}
		
		subGraphMiner(crawlers, compResult, startTime);
	}
	
	public static void subGraphMiner(List<SubGraphCrawler> crawlers, 
			Comparison compResult, 
			long startTime) {
		
		try {
			StringBuilder sb = new StringBuilder();
			
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
			Date now = new Date();
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
			sumBuilder.append(staticThreshold + ",");
			sumBuilder.append(simThreshold + ",");
			sumBuilder.append(compResult.time + ",");
			sumBuilder.append(now.toString() + "\n");
			sumBuilder.append("\n");
			sb.append(sumBuilder.toString());
			sb.append(header);
			
			//Write summary to DB
			int compResultId = -1;
			if (shouldDB)
				compResultId = DBConnector.writeCompTableResult(staticThreshold, simThreshold, now, compResult);
			
			String compareName = compResult.lib1 + "-" + compResult.lib2;
			String csvName = MIBConfiguration.getInstance().getResultDir() + "/" + compareName + now.getTime() + ".csv";
			
			int writerCount = 0;
			List<HotZone> buffer = new ArrayList<HotZone>();
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
							"," + hit.getSubGraphId() +
							"," + hit.getTargetGraphName() + 
							"," + hit.getTargetGraphId() +
							"," + hit.getSubStart() +
							"," + hit.getSubCentroid() + 
							"," + hit.getSubCentroid().getLinenumber() +
							"," + hit.getSubCentroid().callerLine +
							"," + hit.getSubEnd() +
							"," + hit.getSubTrace() +
							"," + hit.getStartInst() +  
							"," + hit.getCentralInst() + 
							"," + hit.getCentralInst().getLinenumber() + 
							"," + hit.getCentralInst().callerLine + 
							"," + hit.getEndInst() + 
							"," + hit.getTargetTrace() +
							"," + hit.getSegSize() + 
							"," + hit.getInstDistance() +
							"," + hit.getSimilarity() + "\n");
					sb.append(rawRecorder);
				}
				
				GsonManager.writeResult(csvName, sb);
				sb = new StringBuilder();
				
				//Write hotzones to DB
				if (compResultId >= 0 && zones.size() > 0) {
					buffer.addAll(zones);
				}
				
				if (buffer.size() >= 5000) {
					ExecutorService dbExecutor = Executors.newFixedThreadPool(1);
					DBWriter dbWriter = new DBWriter(compResultId, buffer);
					Future<Boolean> submitFuture = dbExecutor.submit(dbWriter);
					boolean submitResult = submitFuture.get(120, TimeUnit.SECONDS);
					try {
						if (submitResult)
							writerCount++;
						else {
							logger.error("Write in time but fails: " + buffer.size());
						}
					} catch (Exception ex) {
						ex.printStackTrace();
						submitFuture.cancel(true);
						logger.error("Fail to write " + buffer.size() + " zones");
					} finally {
						logger.info("Buffer size: " + buffer.size());
						buffer.clear();
						dbExecutor.shutdown();
						while (!dbExecutor.isTerminated());
						logger.info("Current writer after dbExecutor: " + writerCount);
					}
				}
			}
			
			if (buffer.size() > 0) {
				ExecutorService dbExecutor = Executors.newFixedThreadPool(1);
				DBWriter dbWriter = new DBWriter(compResultId, buffer);
				Future<Boolean> submitFuture = dbExecutor.submit(dbWriter);
				boolean submitResult = submitFuture.get(60, TimeUnit.SECONDS);
				try {					
					if (submitResult)
						writerCount++;
					else {
						logger.error("Write in time but fails: " + buffer.size());
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					submitFuture.cancel(true);
					logger.error("Fail to write " + buffer.size() + " zones");
				} finally {
					logger.info("Buffer size (residue): " + buffer.size());
					buffer.clear();
					dbExecutor.shutdown();
					while (!dbExecutor.isTerminated());
					logger.info("Current writer after dbExecutor (residue): " + writerCount);
				}
			}
			
			logger.info("Total writer: " + writerCount);
			logger.info("Storing data ends");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
				
	public static void main(String[] args) throws Exception {
		Console console = System.console();
		if (console == null) {
			System.err.println("Null console");
			System.exit(1);
		}
		
		String url = MIBConfiguration.getInstance().getDburl();
		String username = MIBConfiguration.getInstance().getDbusername();
		System.out.println("DB URL: " + url);
		
		CommandLineParser cParser = new DefaultParser();
		Options options = ArgConfiguration.getOptions();
		
		boolean constructOnly = false;
		String targetDir = null;
		String testDir = null;
		String[] graphrepos = null;
		
		CommandLine cLine = cParser.parse(options, args);
		if (cLine.hasOption(ArgConfiguration.CONSTRUCT)) {
			constructOnly = true;
		}
		
		if (cLine.hasOption(ArgConfiguration.TARGET) && cLine.hasOption(ArgConfiguration.TEST)) {
			targetDir = cLine.getOptionValue(ArgConfiguration.TARGET);
			testDir = cLine.getOptionValue(ArgConfiguration.TEST);
		} else {
			graphrepos = cLine.getOptionValues(ArgConfiguration.GREPO);
		}
		
		String password = null;
		
		if (cLine.hasOption(ArgConfiguration.DB_PW)) {
			System.out.println("Try connect to DB...");
			password = cLine.getOptionValue(ArgConfiguration.DB_PW);
		} else {
			System.out.println("Store result into DB?");
			Scanner scanner = new Scanner(System.in);
			shouldDB = scanner.nextBoolean();
			if (shouldDB) {
				char[] passArray = console.readPassword("DB password: ");
				password = new String(passArray);
			} else {
				password = null;
			}
		}
				
		DBConnector.init(url, username, password);
		if (shouldDB && !DBConnector.probeDB()) {
			System.out.println("No DB connection. Wanna execute experiment still?");			
			try {
				Scanner scanner = new Scanner(System.in);
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
		
		if (MIBConfiguration.getInstance().isNativeClass()) {
			MIBDriver.setupNativePackages();
		}
				
		logger.info("Start PageRank analysis for Bytecode subgraph mining");
		logger.info("Similarity strategy: " + MIBConfiguration.getInstance().getSimStrategy());
		logger.info("Assignemnt threshold: " + assignmentThreshold);
		logger.info("Static threshold: " + staticThreshold);
		logger.info("Dynamic threshold: " + simThreshold);
		logger.info("Construct only: " + constructOnly);
		
		if (graphrepos == null || graphrepos.length == 0) {
			logger.info("Normal load");
			if (targetDir == null || testDir == null) {
				logger.error("Invalid graph directory: " + targetDir + " " + testDir);
				System.exit(-1);
			}
			logger.info("Lib1 direcotry: " + (new File(targetDir)).getAbsolutePath());
			logger.info("Lib2 direcotry: " + (new File(testDir)).getAbsolutePath());
			
			initiateSubGraphMining(targetDir, 
					testDir, 
					url, 
					username, 
					password, 
					constructOnly);
		} else {
			logger.info("Group load: " + graphrepos.length);
			
			HashMap<String, List<GraphProfile>> graphRepoWithInstances = new HashMap<String, List<GraphProfile>>();
			HashMap<String, Integer> unfilterRecord = new HashMap<String, Integer>();
			String graphRepoNames = "";
			int totalUnfilters = 0;
			for (String graphRepo: graphrepos) {
				logger.info("Repo loc: " + graphRepo);
				int lastIdx = graphRepo.lastIndexOf("/");
				String simpleName = graphRepo.substring(lastIdx + 1);
				graphRepoNames = graphRepoNames + simpleName + "-";
				GraphLoadController.groupLoad(graphRepo, graphRepoWithInstances, unfilterRecord);
				totalUnfilters += unfilterRecord.get(graphRepo);
				
				logger.info("# of graphs: " + unfilterRecord.get(graphRepo));
				logger.info("# of graphs after filter: " + graphRepoWithInstances.get(graphRepo).size());
			}
			
			if (constructOnly)
				return ;
			
			graphRepoNames = graphRepoNames.substring(0, graphRepoNames.length() - 1);
			//Combine all graphs from all repos together;
			List<GraphProfile> allGraphs = new ArrayList<GraphProfile>();
			for (List<GraphProfile> repoGraphs: graphRepoWithInstances.values()) {
				allGraphs.addAll(repoGraphs);
			}
			
			logger.info("Total graph number in all repos: " + totalUnfilters);
			logger.info("Total graph number in all repos after filter: " + allGraphs.size());
			
			List<SubGraphCrawler> crawlers = new ArrayList<SubGraphCrawler>();
			GraphLoadController.constructCrawlerListExcludePkg(allGraphs, allGraphs, crawlers);
			
			Comparison compResult = new Comparison();
			compResult.inst_thresh = MIBConfiguration.getInstance().getInstThreshold();
			compResult.inst_cat = MIBConfiguration.getInstance().getSimStrategy();
			compResult.lib1 = graphRepoNames;
			compResult.lib2 = graphRepoNames;
			compResult.method1 = totalUnfilters;
			compResult.method2 = totalUnfilters;
			compResult.method_f_1 = allGraphs.size();
			compResult.method_f_2 = allGraphs.size();
			compResult.m_compare = crawlers.size();
			
			long startTime = System.currentTimeMillis();
			subGraphMiner(crawlers, compResult, startTime);
			
			
			//List<String> validRepos = Enumerater.enumerate(graphrepos);
			//logger.info("Valid usr repose: " + validRepos.size());
			
			//Key: usr repo, val: graph profiles under this usr repo
			/*HashMap<String, Integer> unfilters = new HashMap<String, Integer>();
			HashMap<String, List<GraphProfile>> loadedByRepo = new HashMap<String, List<GraphProfile>>();
			
			GraphLoadController.groupLoad(validRepos, loadedByRepo, unfilters);
			
			if (constructOnly)
				return ;
			
			for (int i = 0; i < validRepos.size(); i++) {
				for (int j = i + 1; j < validRepos.size(); j++) {
					List<SubGraphCrawler> crawlers = new ArrayList<SubGraphCrawler>();
					String lib1 = validRepos.get(i);
					String lib2 = validRepos.get(j);
					
					logger.info("Lib1 direcotry: " + (new File(lib1)).getAbsolutePath());
					logger.info("Lib2 direcotry: " + (new File(lib2)).getAbsolutePath());
					
					List<GraphProfile> templateProfiles = loadedByRepo.get(lib1);
					List<GraphProfile> testProfiles = loadedByRepo.get(lib2);
					
					GraphLoadController.constructCrawlerList(templateProfiles, testProfiles, crawlers);
					GraphLoadController.constructCrawlerList(testProfiles, templateProfiles, crawlers);
										
					Comparison compResult = new Comparison();
					compResult.inst_thresh = MIBConfiguration.getInstance().getInstThreshold();
					compResult.inst_cat = MIBConfiguration.getInstance().getSimStrategy();
					String[] lib1Sep = lib1.split("/");
					String[] lib2Sep = lib2.split("/");
					compResult.lib1 = lib1Sep[lib1Sep.length - 1];
					compResult.lib2 = lib2Sep[lib2Sep.length - 1];
					compResult.method1 = unfilters.get(lib1);
					compResult.method2 = unfilters.get(lib2);
					compResult.method_f_1 = templateProfiles.size();
					compResult.method_f_2 = testProfiles.size();
					compResult.m_compare = crawlers.size();
					
					long startTime = System.currentTimeMillis();
					subGraphMiner(crawlers, compResult, startTime);
				}
			}*/
			
		}
		
		try {
			if (DBConnector.getConnection() != null)
				DBConnector.getConnection().close();
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
		System.out.println("Process ends");
	}
	
	public static class SegInfo {
		public List<InstNode> seg;
		
		public double[] normInstDistribution;
		
		public double instDistWithSub;
		
		//public TreeSet<Integer> lineTrace;
		public String lineTrace;
		
		public boolean match;
	}
	
	public static class GraphProfile {
		
		public String fileName;
		
		public GraphTemplate graph;
		
		//The geographic start
		public InstNode startInst;
		
		//The inst with highest pg
		public InstWrapper centroidWrapper;
		
		//The geographic end
		public InstNode endInst;
		
		public int before;
		
		public int after;
		
		public int[] coreRep;
		
		public int[] pgRep;
		
		public double[] normDist;
		
		public String lineTrace;
	}
	
	public static class WeightedEdge {
		int edgeId;
		
		double edgeWeight;
	}
	
	private static class DBWriter implements Callable<Boolean>{
		
		int compResultId;
		
		List<HotZone> zones;
		
		public DBWriter(int compResultId, List<HotZone> zones) {
			this.compResultId = compResultId;
			this.zones = zones;
			
		}
		
		@Override
		public Boolean call() {
			return DBConnector.writeDetailTableResult(compResultId, zones);
		}
	}
	
	private static class ConstructWorker implements Runnable {
		
		GraphTemplate rawGraph;

		@Override
		public void run() {
			logger.info("Test name: " + rawGraph.getMethodKey());
			logger.info("Inst node size: " + rawGraph.getInstPool().size());
			GraphConstructor constructor = new GraphConstructor();
			constructor.reconstructGraph(rawGraph, true);
		}
	}
	
	public static class ProfileWorker implements Callable<GraphProfile> {
		
		String fileName;
		
		GraphTemplate graph;
		
		int profilerIdx = profilerIndex.getAndIncrement();
		
		public GraphProfile call() throws Exception {
			logger.info("Graph name with profiler idx: " + this.graph.getMethodKey() + " " + this.profilerIdx);
			GraphConstructor constructor = new GraphConstructor();
			constructor.reconstructGraph(this.graph, true);
			constructor.cleanObjInit(this.graph);			
			return profileGraph();
		}
		
		public GraphProfile profileGraph() {
			if (this.graph.getInstPool().size() == 0) {
				logger.warn("Empty graph: " + this.graph.getMethodKey());
				return null;
			}
			List<InstNode> sortedSub = GraphUtil.sortInstPool(this.graph.getInstPool(), true);
			
			InstNode startSub = sortedSub.get(0);
			InstNode endSub = sortedSub.get(sortedSub.size() - 1);
						
			//If the graph is reduced, some node will be removed and we choose not to clean them, save some time
			boolean partialPool = MIBConfiguration.getInstance().isReduceGraph();
			PageRankSelector subSelector = new PageRankSelector(this.graph.getInstPool(), partialPool, true);
			List<InstWrapper> subRank = subSelector.computePageRank();
			
			/*System.out.println("Check sub graph rank");
			for (int i = 0; i < 20; i++) {
				InstWrapper iw = subRank.get(i);
				System.out.println(iw.inst);
				System.out.println(iw.pageRank);
			}*/
			
			/*List<InstWrapper> selectedSub = PercentageSelector.selectImportantInstWrappers(subRank);
			int[] subPGRep = SearchUtil.generatePageRankRep(selectedSub);*/
			int[] subPGRep = SearchUtil.generatePageRankRep(subRank);
			//logger.info("Sub graph PageRank: " + Arrays.toString(subPGRep));
			
			//Use the most important inst as the central to collect insts in target
			InstNode subCentroid = subRank.get(0).inst;
			int before = 0, after = 0;
			boolean recordBefore = true;
			//TreeSet<Integer> lineTrace = new TreeSet<Integer>();
			for (int i = 0; i < sortedSub.size(); i++) {
				InstNode curNode = sortedSub.get(i);
				//lineTrace.add(curNode.callerLine);
				
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
			
			String lineTrace = startSub.callerLine + ":" + subCentroid.callerLine + ":" + endSub.callerLine;
			//List<Set<InstNode>> neighbors = Locator.coreTracer(subCentroid, this.graph.getInstPool());
			//int[] coreRep = Locator.coreTracer(subCentroid, this.graph.getInstPool());
			
			GraphProfile gp = new GraphProfile();
			gp.fileName = this.fileName;
			gp.graph = this.graph;
			gp.startInst = startSub;
			gp.centroidWrapper = subRank.get(0);
			gp.endInst = endSub;
			gp.before = before;
			gp.after = after;
			//gp.coreRep = coreRep;
			gp.pgRep = subPGRep;
			double[] instDist = StaticTester.genDistribution(this.graph.getInstPool());
			gp.normDist = StaticTester.normalizeDist(instDist, subRank.size());
			gp.lineTrace = lineTrace;
						
			return gp;
		}
	}
	
	public static class SubGraphCrawler implements Callable<List<HotZone>> {
		
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
			
			HashSet<InstNode> miAssignments = 
					Locator.possibleSingleAssignment(subProfile.centroidWrapper.inst, sortedTarget);
			//HashSet<InstNode> miAssignments = Locator.advSingleAssignment(subProfile, targetGraph.getInstPool());
			logger.info("Target graph vs Sub graph: " + targetGraphName + " " + subGraphName);
			logger.info("Thread index: " + crawlerId);
			logger.info("Possible assignments: " + miAssignments.size());
			logger.info("Sub-graph size: " + subProfile.pgRep.length);
			HashMap<InstNode, SegInfo> candSegs = Locator.locateSegments(miAssignments, sortedTarget, subProfile);
			logger.info("Real assignments: " + candSegs.size());
			List<HotZone> hits = new ArrayList<HotZone>();
			
			synchronized(countLock) {
				subCompNum += miAssignments.size();
				realSubCompNum += candSegs.size();
			}
			
			List<Double> simCollector = new ArrayList<Double>();
			//int expCentroidOpcode = subProfile.centroidWrapper.inst.getOp().getOpcode();
			for (InstNode cand: candSegs.keySet()) {
				SegInfo segInfo = candSegs.get(cand);
				List<InstNode> segments = segInfo.seg;
				InstPool segPool = new InstPool();
				
				for (InstNode segInst: segments) {
					segPool.add(segInst);
				}
				
				PageRankSelector ranker = new PageRankSelector(segPool, true, true);
				List<InstWrapper> ranks = ranker.computePageRank();
				int[] candPGRep = SearchUtil.generatePageRankRep(ranks);
				//int realCentroidOpcode = ranks.get(0).inst.getOp().getOpcode();
				//boolean centerMatch = (expCentroidOpcode == realCentroidOpcode);
				boolean centerMatch = (Locator.equalInst(subProfile.centroidWrapper.inst, ranks.get(0).inst));
				
				/*List<InstWrapper> selectedRanks = PercentageSelector.selectImportantInstWrappers(ranks);
				int[] candPGRep = SearchUtil.generatePageRankRep(selectedRanks);*/
				
				/*int dist = 0;
				if (candPGRep.length == 0) {
					dist = subProfile.pgRep.length;
				} else {
					dist = LevenshteinDistance.calculateDistance(subProfile.pgRep, candPGRep);
				}
				double sim = LevenshteinDistance.levenSimilarity(dist, subProfile.pgRep.length);*/
								
				double sim = -1;
				if (candPGRep.length == 0) {
					sim = 0;
				} else {
					JaroWinklerDistance measurer = null;
					if (segInfo.match && centerMatch) {
						measurer = new JaroWinklerDistance(0.8, 5);
					} else {
						measurer = new JaroWinklerDistance();
					}
					sim = measurer.proximity(subProfile.pgRep, candPGRep);
				}
								
				//Don't compare double directly, give 0.5% threshold
				if (sim > simThreshold) {
					/*System.out.println("Check target ranks");
					for (int i = 0; i < 20; i++) {
						InstWrapper iw = ranks.get(i);
						System.out.println(iw.inst);
						System.out.println(iw.pageRank);
					}
					
					int[] subCopy = new int[20];
					System.arraycopy(subProfile.pgRep, 0, subCopy, 0, 20);
					System.out.println("Sub pg rank: " + Arrays.toString(subCopy));
					System.out.println("Sub linetrace: " + subProfile.lineTrace);
					int[] candCopy = new int[20];
					System.arraycopy(candPGRep, 0, candCopy, 0, 20);
					System.out.println("Can pg rank: " + Arrays.toString(candCopy));
					System.out.println("Can linetrace: " + segInfo.lineTrace);
					System.out.println("Dynamic similarity: " + sim);
					
					System.out.println("Sub important: " + subProfile.centroidWrapper.inst);
					System.out.println("Can important: " + ranks.get(0).inst);*/
					
					HotZone zone = new HotZone();
					zone.setSubStart(subProfile.startInst);
					zone.setSubCentroid(subProfile.centroidWrapper.inst);
					zone.setSubEnd(subProfile.endInst);
					zone.setSubTrace(subProfile.lineTrace);
					//zone.setSubPgRank(subProfile.centroidWrapper.pageRank);
					zone.setStartInst(segments.get(0));
					zone.setCentralInst(cand);
					zone.setEndInst(segments.get(segments.size() - 1));
					zone.setTargetTrace(segInfo.lineTrace);
					//zone.setLevDist(dist);
					zone.setSimilarity(sim);
					zone.setInstDistance(segInfo.instDistWithSub);
					zone.setSegSize(segPool.size());
					//zone.setSegs(segPool);
					zone.setSubGraphName(subGraphName);
					zone.setSubGraphId(subProfile.graph.getThreadId() + "-" + subProfile.graph.getThreadMethodId());
					zone.setTargetGraphName(targetGraphName);
					zone.setTargetGraphId(targetGraph.getThreadId() + "-" + targetGraph.getThreadMethodId());
					hits.add(zone);
					
					simCollector.add(zone.getSimilarity());
				}
			}
			
			int limit = 5;
			if (hits.size() >= limit) {
				logger.info("Too many hits: " + hits.size());
				Collections.sort(simCollector, Collections.reverseOrder());
				double bound = simCollector.get(limit - 1);
				
				Iterator<HotZone> hzIterator = hits.iterator();
				while (hzIterator.hasNext()) {
					HotZone hz = hzIterator.next();
					if (hz.getSimilarity() < bound) {
						hzIterator.remove();
					}
				}
			}
			logger.info("Crawler hits: " + this.crawlerId + " " + hits.size());
			return hits;
		}
	}
}
