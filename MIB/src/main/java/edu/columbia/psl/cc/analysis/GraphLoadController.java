package edu.columbia.psl.cc.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.analysis.PageRankSelector.GraphProfile;
import edu.columbia.psl.cc.analysis.PageRankSelector.ProfileWorker;
import edu.columbia.psl.cc.analysis.PageRankSelector.SubGraphCrawler;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.util.StringUtil;
import edu.columbia.psl.cc.util.TemplateLoader;
import edu.columbia.psl.cc.util.DBConnector.Comparison;

public class GraphLoadController {
	
	private static Logger logger = LogManager.getLogger(GraphLoadController.class);
	
	private final static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
	private final static int dominantDiff = 20; 
	
	public static void filterGraphs(HashMap<String, GraphTemplate> graphs) {
		HashSet<String> graphHistory = new HashSet<String>();
		for (Iterator<String> keyIT = graphs.keySet().iterator(); keyIT.hasNext();) {
			String key = keyIT.next();
			GraphTemplate graph = graphs.get(key);
			String recordKey = graph.getShortMethodKey() + ":" + graph.getVertexNum() + ":" + graph.getEdgeNum();
			double density = ((double)graph.getEdgeNum())/graph.getVertexNum();
			int diff = graph.getVertexNum() - graph.getChildDominant();
			if (graphHistory.contains(recordKey)) {
				keyIT.remove();
			} else if (graph.getVertexNum() <= MIBConfiguration.getInstance().getInstThreshold()) {
				keyIT.remove();
			} else if (density < 0.8) {
				logger.info("Low density graph: " + recordKey);
				keyIT.remove();
			} else if (graph.getChildDominant() != 0 && diff < dominantDiff) {
				logger.info("Child dominant graph: " + recordKey);
				keyIT.remove();
			}else {
				graphHistory.add(recordKey);
			}
		}
	}
	
	public static List<GraphProfile> parallelizeProfiling(HashMap<String, GraphTemplate> graphs, 
			int parallelFactor) {
		List<GraphProfile> profiles = new ArrayList<GraphProfile>();
		
		try {
			ExecutorService profileExecutor = 
					Executors.newFixedThreadPool(parallelFactor);
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
	
	public static void constructCrawlerListExcludePkg(List<GraphProfile> subProfiles, 
			List<GraphProfile> targetProfiles, 
			List<SubGraphCrawler> crawlers) {
		int shouldCount = 0;
		int realCount = 0;
		for (GraphProfile subProfile: subProfiles) {
			String[] subParsed = subProfile.graph.getMethodKey().split(":");
			String subPkgWithClass = subParsed[0];
			String subPkg = StringUtil.extractPkg(subPkgWithClass);
			for (GraphProfile targetProfile: targetProfiles) {
				if (subProfile.graph.getMethodKey().equals(targetProfile.graph.getMethodKey())) {
					continue ;
				}
				shouldCount++;
				String[] targetParsed = targetProfile.graph.getMethodKey().split(":");
				String targetPkgWithClass = targetParsed[0];
				String targetPkg = StringUtil.extractPkg(targetPkgWithClass);
				
				if (subPkg.equals(targetPkg)) {
					boolean related = false;
					for (InstNode sInst: subProfile.graph.getInstPool()) {
						for (InstNode tInst: targetProfile.graph.getInstPool()) {
							if (sInst.getFromMethod().equals(tInst.getFromMethod())) {
								related = true;
								break ;
							}
						}
					}
					
					if (related)
						continue ;
				}
				realCount++;
				
				SubGraphCrawler crawler = new SubGraphCrawler();
				crawler.subGraphName = subProfile.graph.getMethodKey();
				crawler.targetGraphName = targetProfile.graph.getMethodKey();
				crawler.subGraphProfile = subProfile;
				crawler.targetGraph = targetProfile.graph;
				crawlers.add(crawler);
			}
		}
		logger.info("Should count: " + shouldCount);
		logger.info("Real count: " + realCount);
	}
	
	public static void constructCrawlerList(List<GraphProfile> subProfiles, 
			List<GraphProfile> targetProfiles, 
			List<SubGraphCrawler> crawlers) {
		for (GraphProfile subProfile: subProfiles) {
			for (GraphProfile targetProfile: targetProfiles) {				
				if (subProfile.graph.getMethodKey().equals(targetProfile.graph.getMethodKey())) {
					continue ;
				}
				
				SubGraphCrawler crawler = new SubGraphCrawler();
				crawler.subGraphName = subProfile.graph.getMethodKey();
				crawler.targetGraphName = targetProfile.graph.getMethodKey();
				crawler.subGraphProfile = subProfile;
				crawler.targetGraph = targetProfile.graph;
				crawlers.add(crawler);
			}
		}
	}
		
	public static Comparison normalLoad(String targetPath, 
			String testPath, 
			List<SubGraphCrawler> crawlers) {
		Comparison compResult = new Comparison();
		compResult.inst_thresh = MIBConfiguration.getInstance().getInstThreshold();
		compResult.inst_cat = MIBConfiguration.getInstance().getSimStrategy();
		
		//boolean probeTarget = TemplateLoader.probeDir(targetLoc);
		//boolean probeTest = TemplateLoader.probeDir(testLoc);
		
		File targetLoc = new File(targetPath);
		File testLoc = null;
		if (testPath != null)
			testLoc = new File(testPath);
		
		boolean probeTarget = (targetLoc != null && targetLoc.isDirectory());
		boolean probeTest = (testLoc != null && testLoc.isDirectory());
		String lib1Name = "";
		String lib2Name = "";
		
		HashMap<String, GraphTemplate> targets = null;
		HashMap<String, GraphTemplate> tests = null;
		int parallelFactor = Runtime.getRuntime().availableProcessors();
		//int parallelFactor = 1;
		
		if (probeTarget && probeTest) {
			logger.info("Comparison mode: " + targetLoc.getAbsolutePath() + " " + testLoc.getAbsolutePath());
			
			//targets = TemplateLoader.loadTemplate(targetLoc, graphToken);
			//tests = TemplateLoader.loadTemplate(testLoc, graphToken);
			targets = TemplateLoader.unzipDir(targetLoc, graphToken);
			tests = TemplateLoader.unzipDir(testLoc, graphToken);
			
			lib1Name = targetLoc.getName();
			lib2Name = testLoc.getName();
			
			compResult.lib1 = lib1Name;
			compResult.lib2 = lib2Name;
			compResult.method1 = targets.size();
			compResult.method2 = tests.size();
			
			filterGraphs(targets);
			filterGraphs(tests);
			
			compResult.method_f_1 = targets.size();
			compResult.method_f_2 = tests.size();
			
			logger.info("Target size: " + targets.size());
			logger.info("Test size: " + tests.size());
			
			List<GraphProfile> targetProfiles = parallelizeProfiling(targets, parallelFactor);
			List<GraphProfile> testProfiles = parallelizeProfiling(tests, parallelFactor);
			
			logger.info("Target profiles: " + targetProfiles.size());
			logger.info("Test profiles: " + testProfiles.size());
			
			constructCrawlerList(targetProfiles, testProfiles, crawlers);
			constructCrawlerList(testProfiles, targetProfiles, crawlers);
		} else if (probeTarget) {
			logger.info("Exhaustive mode: " + targetLoc.getAbsolutePath());
			targets = TemplateLoader.unzipDir(targetLoc, graphToken);
			lib1Name = targetLoc.getName();
			
			compResult.lib1 = lib1Name;
			compResult.lib2 = lib1Name;
			compResult.method1 = targets.size();
			compResult.method2 = targets.size();
			
			filterGraphs(targets);
			compResult.method_f_1 = targets.size();
			compResult.method_f_2 = targets.size();
			logger.info("Target size: " + targets.size());
			
			/*logger.info("Before merging");
			int bv = 0;
			for (GraphTemplate gt: targets.values()) {
				logger.info("Method name: " + gt.getMethodKey());
				logger.info("Inst size: " + gt.getInstPool().size());
				bv += gt.getInstPool().size();
			}
			logger.info("Before total vertices: " + bv);
			logger.info("Before avg vertices: " + ((double)bv)/targets.size());*/
			
			List<GraphProfile> targetProfiles = parallelizeProfiling(targets, parallelFactor);
			logger.info("Target profiles: " + targetProfiles.size());
			
			//For exp purpose...
			/*int vSum = 0;
			int eSum = 0;
			int vMax = Integer.MIN_VALUE;
			for (GraphProfile gp: targetProfiles) {
				if (gp.graph.getVertexNum() > vMax) {
					vMax = gp.graph.getInstPool().size();
				}
				
				vSum += gp.graph.getInstPool().size();
				for (InstNode in: gp.graph.getInstPool()) {
					eSum += in.getChildFreqMap().size();
				}
			}
			double avgVertex = ((double)vSum)/targetProfiles.size();
			double avgEdge = ((double)eSum)/targetProfiles.size();
			logger.info("Total vertices: " + vSum);
			logger.info("Avg vertices: " + avgVertex);
			logger.info("Max vertices: " + vMax);
			logger.info("Total edges: " + eSum);
			logger.info("Avg edges: " + avgEdge);*/
			
			if (MIBConfiguration.getInstance().isExclPkg()) {
				constructCrawlerListExcludePkg(targetProfiles, targetProfiles, crawlers);
			} else {
				constructCrawlerList(targetProfiles, targetProfiles, crawlers);
			}
		} else {
			logger.info("Empty repos for mining");
			System.exit(-1);
		}
		compResult.m_compare = crawlers.size();
		logger.info("Total number of comparisons: " + crawlers.size());
		
		return compResult;
	}
	
	public static void groupLoad(String graphRepoFileName, 
			HashMap<String, List<GraphProfile>> loadedByRepo, 
			HashMap<String, Integer> unfilters) {
		
		File graphRepo = new File(graphRepoFileName);
		if (!graphRepo.exists() || graphRepo.isFile()) {
			logger.error("Invalid graph repo: " + graphRepoFileName);
			return ;
		}
		
		List<GraphProfile> allProfiles = new ArrayList<GraphProfile>();
		int totalCount = 0;
		for (File usrDir: graphRepo.listFiles()) {
			if (usrDir.getName().startsWith(".") || usrDir.isFile()) {
				continue ;
			}
			
			HashMap<String, GraphTemplate> usrLoads = TemplateLoader.loadTemplate(usrDir, graphToken);
			totalCount += usrLoads.size();
			
			filterGraphs(usrLoads);
			
			int parallelFactor = Runtime.getRuntime().availableProcessors();
			List<GraphProfile> profiles = parallelizeProfiling(usrLoads, parallelFactor);
			allProfiles.addAll(profiles);
		}
		loadedByRepo.put(graphRepoFileName, allProfiles);
		unfilters.put(graphRepoFileName, totalCount);
	}
		
	public static void groupLoadWihtStruct(HashMap<String, HashMap<String, List<String>>> graphRepos, 
			HashMap<String, HashMap<String, List<GraphProfile>>> structLoads, 
			HashMap<String, HashMap<String, Integer>> unfilters) {
		for (String graphRepo: graphRepos.keySet()) {
			logger.info("Processing graph repo: " + graphRepo);
			HashMap<String, List<String>> usrDirs = graphRepos.get(graphRepo);
			HashMap<String, List<GraphProfile>> usrDirsWGraphs = new HashMap<String, List<GraphProfile>>();
			
			for (String usrDirString: usrDirs.keySet()) {
				File usrDir = new File(usrDirString);
				HashMap<String, GraphTemplate> usrLoads = TemplateLoader.loadTemplate(usrDir, graphToken);
				HashMap<String, Integer> usrLoadsCount = new HashMap<String, Integer>();
				usrLoadsCount.put(usrDirString, usrLoads.size());
				
				filterGraphs(usrLoads);
				logger.info("User method size: " + usrLoads.size());
				
				int parallelFactor = Runtime.getRuntime().availableProcessors();
				List<GraphProfile> profiles = parallelizeProfiling(usrLoads, parallelFactor);
				logger.info("User profiles: " + profiles.size());
				
				usrDirsWGraphs.put(usrDirString, profiles);
			}
			structLoads.put(graphRepo, usrDirsWGraphs);
		}
	}

}
