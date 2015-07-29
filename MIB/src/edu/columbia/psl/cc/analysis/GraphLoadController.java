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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.analysis.PageRankSelector.GraphProfile;
import edu.columbia.psl.cc.analysis.PageRankSelector.ProfileWorker;
import edu.columbia.psl.cc.analysis.PageRankSelector.SubGraphCrawler;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.GraphTemplate;
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
			} else if (diff > dominantDiff) {
				logger.info("Child dominant graph: " + recordKey);
				keyIT.remove();
			}else {
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
		
	public static Comparison normalLoad(File templateLoc, 
			File testLoc, 
			List<SubGraphCrawler> crawlers) {
		Comparison compResult = new Comparison();
		compResult.inst_thresh = MIBConfiguration.getInstance().getInstThreshold();
		compResult.inst_cat = MIBConfiguration.getInstance().getSimStrategy();
		
		boolean probeTemplate = TemplateLoader.probeDir(templateLoc.getAbsolutePath());
		boolean probeTest = TemplateLoader.probeDir(testLoc.getAbsolutePath());
		String lib1Name = "";
		String lib2Name = "";
		
		HashMap<String, GraphTemplate> templates = null;
		HashMap<String, GraphTemplate> tests = null;
		
		if (probeTemplate && probeTest) {
			logger.info("Comparison mode: templates vs tests");
			templates = TemplateLoader.loadTemplate(templateLoc, graphToken);
			tests = TemplateLoader.loadTemplate(testLoc, graphToken);
			
			lib1Name = templateLoc.getName();
			lib2Name = testLoc.getName();
			
			compResult.lib1 = lib1Name;
			compResult.lib2 = lib2Name;
			compResult.method1 = templates.size();
			compResult.method2 = tests.size();
			
			filterGraphs(templates);
			filterGraphs(tests);
			
			compResult.method_f_1 = templates.size();
			compResult.method_f_2 = tests.size();
			
			logger.info("Template size: " + templates.size());
			logger.info("Test size: " + tests.size());
			
			List<GraphProfile> templateProfiles = parallelizeProfiling(templates);
			List<GraphProfile> testProfiles = parallelizeProfiling(tests);
			
			logger.info("Template profiles: " + templateProfiles.size());
			logger.info("Test profiles: " + testProfiles.size());
			
			constructCrawlerList(templateProfiles, testProfiles, crawlers);
			constructCrawlerList(testProfiles, templateProfiles, crawlers);
		} else if (probeTemplate) {
			logger.info("Exhaustive mode: templates vs. templates");
			templates = TemplateLoader.loadTemplate(templateLoc, graphToken);
			lib1Name = templateLoc.getName();
			
			compResult.lib1 = lib1Name;
			compResult.lib2 = lib1Name;
			compResult.method1 = templates.size();
			compResult.method2 = templates.size();
			
			filterGraphs(templates);
			compResult.method_f_1 = templates.size();
			compResult.method_f_2 = templates.size();
			logger.info("Template size: " + templates.size());
			
			List<GraphProfile> templateProfiles = parallelizeProfiling(templates);
			logger.info("Template profiles: " + templateProfiles.size());
			
			constructCrawlerList(templateProfiles, templateProfiles, crawlers);
		} else if (probeTest) {
			logger.info("Exhaustive mode: tests vs. tests");
			tests = TemplateLoader.loadTemplate(testLoc, graphToken);
			lib2Name = testLoc.getName();
			
			compResult.lib1 = lib2Name;
			compResult.lib2 = lib2Name;
			compResult.method1 = tests.size();
			compResult.method2 = tests.size();
			
			filterGraphs(tests);
			compResult.method_f_1 = tests.size();
			compResult.method_f_2 = tests.size();
			logger.info("Test size: " + tests.size());
			
			List<GraphProfile> testProfiles = parallelizeProfiling(tests);
			logger.info("Test profiles: " + testProfiles.size());
			
			constructCrawlerList(testProfiles, testProfiles,crawlers);
		} else {
			logger.info("Empty repos for both templates and tests");
			System.exit(-1);
		}
		compResult.m_compare = crawlers.size();
		logger.info("Total number of comparisons: " + crawlers.size());
		
		return compResult;
	}
	
	public static void groupLoad(List<String> validRepos, 
			HashMap<String, List<GraphProfile>> loadedByRepo, 
			HashMap<String, Integer> unfilters) {
		
		for (String validRepo: validRepos) {
			logger.info("User repo: " + validRepo);
			
			File repoDir = new File(validRepo);
			HashMap<String, GraphTemplate> loads = TemplateLoader.loadTemplate(repoDir, graphToken);
			unfilters.put(validRepo, loads.size());
			
			filterGraphs(loads);
			logger.info("User method size: " + loads.size());
			
			List<GraphProfile> profiles = parallelizeProfiling(loads);
			logger.info("User profiles: " + profiles.size());
			
			loadedByRepo.put(validRepo, profiles);
		}
	}

}
