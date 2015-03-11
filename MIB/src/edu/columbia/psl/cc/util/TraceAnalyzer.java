package edu.columbia.psl.cc.util;

import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;

public class TraceAnalyzer {
	
	private static String graphRepo = "/Users/mikefhsu/Mike/Research/ec2/mib_sandbox_v2/";
	
	private static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
	private static String header = "MethodPair,Seg_thresh,Avg_seg,Sim_thresh,Sim,Lines/Clone";
	
	private File searchFile(List<String> possibleDirs, final String fileEnd) {
		FilenameFilter ff = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				// TODO Auto-generated method stub
				return name.endsWith(fileEnd);
			}
		};
		
		for (String dir: possibleDirs) {
			File f = new File(dir);
			
			File[] files = f.listFiles(ff);
			if (files.length > 0) {
				return files[0];
			}
		}
		return null;
	}
	
	public MethodTrace constructLineTrace(Collection<InstNode> insts) {
		MethodTrace mt = new MethodTrace();
		
		HashMap<String, TreeSet<Integer>> lineTraceMap = new HashMap<String, TreeSet<Integer>>();
		for (InstNode inst: insts) {
			String methodName = inst.getFromMethod();
			
			if (lineTraceMap.containsKey(methodName)) {
				lineTraceMap.get(methodName).add(inst.getLinenumber());
			} else {
				TreeSet<Integer> lineTrace = new TreeSet<Integer>();
				lineTrace.add(inst.getLinenumber());
				lineTraceMap.put(methodName, lineTrace);
			}
		}
		
		int sum = 0;
		for (String methodName: lineTraceMap.keySet()) {
			int lineNum = lineTraceMap.get(methodName).size();
			sum += lineNum;
		}
		
		mt.unitTrace = lineTraceMap;
		mt.lineSum = sum;
		
		return mt;
	}
	
	public void analyzeResult(String url, String username, String password, int compId) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection connect = DriverManager.getConnection(url, username, password);
			
			String compQuery = "SELECT * FROM comp_table " + " WHERE comp_id = " + compId;
			PreparedStatement compStmt = connect.prepareStatement(compQuery);
			ResultSet compResult = compStmt.executeQuery();
			
			String lib1 = null, lib2 = null;
			if (compResult.first()) {
				lib1 = compResult.getString("lib1");
				lib2 = compResult.getString("lib2");
			} else {
				System.err.println("Empty comparison query result");
				System.exit(-1);
			}
			List<String> possibleDirs = new ArrayList<String>();
			possibleDirs.add(graphRepo + lib1);
			possibleDirs.add(graphRepo + lib2);
			System.out.println("Possible graph locations: " + possibleDirs);
			
			/*String query = "SELECT rt.* FROM result_table rt " +
					"INNER JOIN (SELECT sub, sid, target, MAX(similarity) as sim " +
					"FROM result_table " +
					"WHERE comp_id=? and seg_size >=20 and target NOT LIKE '%ejml%' and sub NOT LIKE '%ejml%'" +
					"GROUP BY sub, sid, target) max_rec " +
					"ON rt.sub = max_rec.sub and rt.sid = max_rec.sid and rt.target = max_rec.target and rt.similarity = max_rec.sim " +
					"ORDER BY rt.sub, rt.sid, rt.target, rt.tid, rt.similarity;";*/
			
			String query = "SELECT rt.* FROM result_table2 rt " +
					"INNER JOIN (SELECT sub, target, MAX(similarity) as sim " +
					"FROM result_table2 " +
					"WHERE comp_id=? and seg_size >=? and (target LIKE '%ejml%' or sub NOT LIKE '%ejml%') and similarity >= ? " +
					"GROUP BY sub, target) max_rec " +
					"ON rt.sub = max_rec.sub and rt.target = max_rec.target and rt.similarity = max_rec.sim " +
					"WHERE comp_id=?;";
					//"ORDER BY rt.sub, rt.sid, rt.target, rt.tid, rt.t_start_caller, rt.t_centroid_caller, rt.t_end_caller, rt.similarity;";
			
			int instThreshold = 300;
			double simThreshold = 0.795;
			
			PreparedStatement pStmt = connect.prepareStatement(query);
			pStmt.setInt(1, compId);
			pStmt.setInt(2, instThreshold);
			pStmt.setDouble(3, simThreshold);
			pStmt.setInt(4, compId);
			ResultSet result = pStmt.executeQuery();
			
			HashMap<String, GraphTemplate> graphCache = new HashMap<String, GraphTemplate>();
			HashMap<String, double[]> graphInfoCache = new HashMap<String, double[]>();
			
			//TreeMap<TraceObject, Double> traceMap = new TreeMap<TraceObject, Double>(traceSorter);
			HashMap<TreeSet<String>, TraceObject> traceMap = new HashMap<TreeSet<String>, TraceObject>();
			while (result.next()) {
				//Get graph object
				String subId = result.getString("sid").replace("-", ":") + ".json";
				String subName = result.getString("sub");
				String targetId = result.getString("tid").replace("-", ":") + ".json";
				String targetName = result.getString("target");
				String tStartInst = result.getString("t_start");
				int segSize = result.getInt("seg_size");
				double similarity = result.getDouble("similarity");
				double staticSimilarity = result.getDouble("static_dist");
				
				TreeSet<String> traceKey = new TreeSet<String>();
				traceKey.add(subName);
				traceKey.add(targetName);
				
				TraceObject traceObject = null;
				if (traceMap.containsKey(traceKey)) {
					traceObject = traceMap.get(traceKey);
				} else {
					traceObject = new TraceObject();
					traceObject.rep.put(subName, subId);
					traceObject.rep.put(targetName, targetId);
					traceMap.put(traceKey, traceObject);
				}
				
				traceObject.segSizes.add(segSize);
				traceObject.similarities.add(similarity);
				
				String graphLabel = null;
				double degree = 0;
				GraphTemplate subGraph = null;
				if (!graphCache.containsKey(subId)) {
					File subFile = searchFile(possibleDirs, subId);
					//System.out.println("Test id: " + subId);
					subGraph = GsonManager.readJsonGeneric(subFile, graphToken);
					//System.out.println("Test graph name: " + subGraph.getMethodName());
					GraphConstructor reconstructor = new GraphConstructor();
					reconstructor.reconstructGraph(subGraph, false);
					reconstructor.cleanObjInit(subGraph);
					MethodTrace mt = this.constructLineTrace(subGraph.getInstPool());
					subGraph.methodTrace = mt;
					
					/*double[] subInfo = {subGraph.getVertexNum(), subGraph.getEdgeNum()};
					graphLabel = subGraph.getVertexNum() + ":" + subGraph.getEdgeNum();
					
					graphCache.put(subId, subGraph);
					graphInfoCache.put(subId, subInfo);
					degree = subInfo[1]/subInfo[0];*/
				} else {
					subGraph = graphCache.get(subId);
				}
				
				/*if (degree < 1) {
					continue ;
				}*/
				if (!traceObject.methodsTraceMap.containsKey(subName)) {
					traceObject.methodsTraceMap.put(subName, subGraph.methodTrace);
				} else {
					int currentLinecount = traceObject.methodsTraceMap.get(subName).lineSum;
					if (subGraph.methodTrace.lineSum > currentLinecount) {
						traceObject.methodsTraceMap.put(subName, subGraph.methodTrace);
						traceObject.rep.put(subName, subId);
					}
				}
				
				GraphTemplate targetGraph = null;
				if (!graphCache.containsKey(targetId)) {
					//System.out.println("Target id: " + targetId);
					File targetFile = searchFile(possibleDirs, targetId);
					targetGraph = GsonManager.readJsonGeneric(targetFile, graphToken);
					GraphConstructor reconstructor = new GraphConstructor();
					reconstructor.reconstructGraph(targetGraph, false);
					reconstructor.cleanObjInit(targetGraph);
					MethodTrace mt = this.constructLineTrace(targetGraph.getInstPool());
					targetGraph.methodTrace = mt;
					//System.out.println("Target graph name: " + targetGraph.getMethodName());
					graphCache.put(targetId, targetGraph);
				} else {
					targetGraph = graphCache.get(targetId);
				}
				
				List<InstNode> segments = GraphUtil.sortInstPool(targetGraph.getInstPool(), true);
				int counter = 0;
				boolean startRecord = false;
				List<InstNode> requiredSegments = new ArrayList<InstNode>();
				for (InstNode s: segments) {
					if (s.toString().equals(tStartInst)) {
						startRecord = true;
					}
					
					if (startRecord) {
						//targetTrace.add(s.callerLine);
						requiredSegments.add(s);
						counter++;
						
						if (counter == segSize) {
							break ;
						}
					}
				}
				
				MethodTrace targetSegTrace = this.constructLineTrace(requiredSegments);
				if (!traceObject.methodsTraceMap.containsKey(targetName)) {
					traceObject.methodsTraceMap.put(targetName, targetSegTrace);
				} else {
					int currentLinecount = traceObject.methodsTraceMap.get(targetName).lineSum;
					if (targetSegTrace.lineSum > currentLinecount) {
						traceObject.methodsTraceMap.put(targetName, targetSegTrace);
						traceObject.rep.put(targetName, targetId);
					}
				}
			}
			
			StringBuilder sb = new StringBuilder();
			sb.append(header + "\n");
			for (TreeSet<String> methods: traceMap.keySet()) {
				StringBuilder methodPair = new StringBuilder();
				TraceObject to = traceMap.get(methods);
				for (String method: methods) {
					methodPair.append(method + "-");
				}
				sb.append(methodPair.substring(0, methodPair.length()) + ",");
				sb.append(instThreshold + ",");
				
				double avgSeg = to.getAvgSegSize();
				sb.append(avgSeg + ",");
				
				sb.append(simThreshold + ",");
				double avgSim = to.getAvgSim();
				sb.append(avgSim + ",");
				
				int lineSum = 0;
				for (String methodName: to.methodsTraceMap.keySet()) {
					MethodTrace mt = to.methodsTraceMap.get(methodName);
					lineSum += mt.lineSum;
				}
				double avgLine = ((double)lineSum)/to.methodsTraceMap.size();
				sb.append(avgLine + "\n");
				
				System.out.println("Method pair: " + methodPair.toString());
				System.out.println("Method rep: " + to.rep);
				System.out.println("Method trace:");
				for (String m: to.methodsTraceMap.keySet()) {
					System.out.println(m);
					System.out.println(to.methodsTraceMap.get(m).unitTrace);
				}
				System.out.println();
			}
			
			String compName = lib1 + "-" + lib2;
			File f = new File(MIBConfiguration.getInstance().getResultDir() + "/" + compName + ".csv");
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			bw.append(sb.toString());
			bw.flush();
			bw.close();
			
			compStmt.close();
			pStmt.close();
			connect.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		Console console = System.console();
		if (console == null) {
			System.err.println("Null console");
			System.exit(1);
		}
		
		System.out.println("Start comp id:");
		int startId = Integer.valueOf(console.readLine());
		
		System.out.println("End comp id:");
		int endId = Integer.valueOf(console.readLine());
		
		final String url = MIBConfiguration.getInstance().getDburl();
		final String username = MIBConfiguration.getInstance().getDbusername();
		System.out.println("DB URL: " + url);
		
		char[] passArray = console.readPassword("DB password: ");
		final String password = new String(passArray);
		
		ExecutorService traceExecutor = Executors.newFixedThreadPool(MIBConfiguration.getInstance().getParallelFactor());
		for (int i = startId; i <= endId; i++) {
			final int compId = i;
			traceExecutor.submit(new Runnable() {
				@Override
				public void run() {
					TraceAnalyzer ta = new TraceAnalyzer();
					ta.analyzeResult(url, username, password, compId);
				}
			});
		}
		
		traceExecutor.shutdown();
		while (!traceExecutor.isTerminated());
		System.out.println("Trace analysis ends");
	}
	
	public static class TraceObject {
		
		public TreeMap<String, String> rep = new TreeMap<String, String>();
		
		public TreeMap<String, MethodTrace> methodsTraceMap = 
				new TreeMap<String, MethodTrace>();
		
		public List<Integer> segSizes = new ArrayList<Integer>();
		
		public List<Double> staticDist = new ArrayList<Double>();
		
		public List<Double> similarities = new ArrayList<Double>();
		
		public String methods() {
			return methodsTraceMap.keySet().toString();
		}
		
		public double getAvgSegSize() {
			int sum = 0;
			for (Integer i: segSizes) {
				sum += i.intValue();
			}
			
			return ((double)sum)/segSizes.size();
		}
		
		public double getAvgStaticDist() {
			double sum = 0.0;
			for (Double d: staticDist) {
				sum += d.doubleValue();
			}
			
			return sum/staticDist.size();
		}
		
		public double getAvgSim() {
			double sum = 0.0;
			for (Double d: similarities) {
				sum += d.doubleValue();
			}
			
			return sum/similarities.size();
		}
	}
	
	public static class MethodTrace {
		
		public HashMap<String, TreeSet<Integer>> unitTrace;
		
		public int lineSum;
	}
}
