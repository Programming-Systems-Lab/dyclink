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
	
	private static String graphRepo = "/Users/mikefhsu/Mike/Research/ec2/mib_sandbox_v3/";
	
	private static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
	private static String header = "Method1,M1Trace,M1LOC,Method2,M2Trace,M2LOC";
	
	private static String summaryHeader = "Insts,Similarity,# Clones,LOC,LOC/Clone";
	
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
		HashMap<String, TreeSet<Integer>> instTraceMap = new HashMap<String, TreeSet<Integer>>();
		for (InstNode inst: insts) {
			String methodName = inst.getFromMethod();
			
			if (lineTraceMap.containsKey(methodName)) {
				lineTraceMap.get(methodName).add(inst.getLinenumber());
				instTraceMap.get(methodName).add(inst.getIdx());
			} else {
				TreeSet<Integer> lineTrace = new TreeSet<Integer>();
				lineTrace.add(inst.getLinenumber());
				lineTraceMap.put(methodName, lineTrace);
				
				TreeSet<Integer> instTrace = new TreeSet<Integer>();
				instTrace.add(inst.getIdx());
				instTraceMap.put(methodName, instTrace);
			}
		}
		
		int sum = 0;
		int instSum = 0;
		for (String methodName: lineTraceMap.keySet()) {
			int lineNum = lineTraceMap.get(methodName).size();
			sum += lineNum;
			
			int instNum = instTraceMap.get(methodName).size();
			instSum += instNum;
		}
		
		mt.unitTrace = lineTraceMap;
		mt.unitInstTrace = instTraceMap;
		mt.lineSum = sum;
		mt.instSum = instSum;
		
		return mt;
	}
	
	public void combineMap(HashMap<String, TreeSet<Integer>> toAdd, 
			HashMap<String, TreeSet<Integer>> recorder) {
		for (String addKey: toAdd.keySet()) {
			TreeSet<Integer> addVal = toAdd.get(addKey);
			if (!recorder.containsKey(addKey)) {
				recorder.put(addKey, addVal);
			} else {
				TreeSet<Integer> currentVal = recorder.get(addKey);
				currentVal.addAll(addVal);
			}
		}
	}
	
	public int sumUp(HashMap<String, TreeSet<Integer>> toSum) {
		int sum = 0;
		for (String key: toSum.keySet()) {
			sum += toSum.get(key).size();
		}
		return sum;
	}
	
	public void analyzeResult(String url, 
			String username, 
			String password, 
			int compId, 
			int instThreshold, 
			double simThreshold) {
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
					"WHERE comp_id=? and seg_size >=? and similarity >= ? " +
					"GROUP BY sub, target) max_rec " +
					"ON rt.sub = max_rec.sub and rt.target = max_rec.target and rt.similarity = max_rec.sim and rt.seg_size >= ? " +
					"WHERE comp_id=?;";
					//"ORDER BY rt.sub, rt.sid, rt.target, rt.tid, rt.t_start_caller, rt.t_centroid_caller, rt.t_end_caller, rt.similarity;";
			
			System.out.println("Inst threshold: " + instThreshold);
			System.out.println("Similarity threshold: " + simThreshold);
			
			PreparedStatement pStmt = connect.prepareStatement(query);
			pStmt.setInt(1, compId);
			pStmt.setInt(2, instThreshold);
			pStmt.setDouble(3, simThreshold);
			pStmt.setInt(4, instThreshold);
			pStmt.setInt(5, compId);
			ResultSet result = pStmt.executeQuery();
			
			HashMap<String, GraphTemplate> graphCache = new HashMap<String, GraphTemplate>();
			
			//TreeMap<TraceObject, Double> traceMap = new TreeMap<TraceObject, Double>(traceSorter);
			HashMap<TreeSet<String>, TraceObject> traceMap = new HashMap<TreeSet<String>, TraceObject>();
			double totalCloneLines  = 0;
			int totalCloneNum = 0;
			StringBuilder sb = new StringBuilder();
			sb.append(header + "\n");
			while (result.next()) {
				totalCloneNum++;
				//Get graph object
				String subId = result.getString("sid").replace("-", ":") + ".json";
				String subName = result.getString("sub");
				String subTrace = result.getString("s_trace");
				String targetId = result.getString("tid").replace("-", ":") + ".json";
				String targetName = result.getString("target");
				String targetTrace = result.getString("t_trace");
				String tStartInst = result.getString("t_start");
				int segSize = result.getInt("seg_size");
				double similarity = result.getDouble("similarity");
				//double staticSimilarity = result.getDouble("static_dist");
				
				TreeSet<String> traceKey = new TreeSet<String>();
				traceKey.add(subName);
				traceKey.add(targetName);
				
				TraceObject traceObject = null;
				if (traceMap.containsKey(traceKey)) {
					traceObject = traceMap.get(traceKey);
				} else {
					traceObject = new TraceObject();
					traceMap.put(traceKey, traceObject);
				}
								
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
					graphCache.put(subId, subGraph);
				} else {
					subGraph = graphCache.get(subId);
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
				System.out.println("Seg size: " + segSize);
				System.out.println("Real size: " + requiredSegments.size());
				
				MethodTrace targetSegTrace = this.constructLineTrace(requiredSegments);
				double cloneLines = ((double)(subGraph.methodTrace.lineSum + targetSegTrace.lineSum))/2;
				totalCloneLines += cloneLines;
				
				//String mapKey = subName + "-" + subTrace + "-" + targetName + "-" + targetTrace;
				String subTargetKey = subName + "-" + targetName;
				if (traceObject.graphMap.containsKey(subTargetKey)) {
					HashMap<String, GraphTuple> lineTraceMap = traceObject.graphMap.get(subTargetKey);
					
					String lineTraceKey = subTrace + "-" + targetTrace;
					if (lineTraceMap.containsKey(lineTraceKey)) {
						GraphTuple gt = lineTraceMap.get(lineTraceKey);
						int currentTotalLine = gt.getTotalLinenumber();
						int challenge = subGraph.methodTrace.lineSum + targetSegTrace.lineSum;
						
						if (challenge > currentTotalLine) {
							gt = new GraphTuple();
							gt.sub = subName;
							gt.subTrace = subTrace;
							gt.repSubId = subId;
							gt.subMethodTrace = subGraph.methodTrace;
							
							gt.target = targetName;
							gt.targetTrace = targetTrace;
							gt.repTargetId = targetId;
							gt.targetMethodTrace = targetSegTrace;
							
							lineTraceMap.put(lineTraceKey, gt);
						}
					} else {
						GraphTuple gt = new GraphTuple();
						gt.sub = subName;
						gt.subTrace = subTrace;
						gt.repSubId = subId;
						gt.subMethodTrace = subGraph.methodTrace;
						
						gt.target = targetName;
						gt.targetTrace = targetTrace;
						gt.repTargetId = targetId;
						gt.targetMethodTrace = targetSegTrace;
						
						lineTraceMap.put(lineTraceKey, gt);
					}
				} else {
					HashMap<String, GraphTuple> lineTraceMap = new HashMap<String, GraphTuple>();
					String lineTraceKey = subTrace + "-" + targetTrace;
					GraphTuple gt = new GraphTuple();
					gt.sub = subName;
					gt.subTrace = subTrace;
					gt.repSubId = subId;
					gt.subMethodTrace = subGraph.methodTrace;
					
					gt.target = targetName;
					gt.targetTrace = targetTrace;
					gt.repTargetId = targetId;
					gt.targetMethodTrace = targetSegTrace;
					
					lineTraceMap.put(lineTraceKey, gt);
					traceObject.graphMap.put(subTargetKey, lineTraceMap);
					System.out.println("Sub target key: " + subTargetKey);
					System.out.println("Line trace key: " + lineTraceKey);
					System.out.println("Target seg trace size: " + targetSegTrace.unitInstTrace);
				}
			}
			
			for (TreeSet<String> methodTup: traceMap.keySet()) {
				System.out.println("Method pair: " + methodTup);
				TraceObject to = traceMap.get(methodTup);
				
				//System.out.println("Clone types #: " + to.graphMap.size());
				HashMap<String, HashMap<String, TreeSet<Integer>>> totalRecorder = 
						new HashMap<String, HashMap<String, TreeSet<Integer>>>();
				HashMap<String, HashMap<String, TreeSet<Integer>>> totalInstRecorder = 
						new HashMap<String, HashMap<String, TreeSet<Integer>>>();
				//Only 2 keys: sub + target, target+ sub
				for (String subTargetKey: to.graphMap.keySet()) {
					System.out.println("Sub-target key: " + subTargetKey);
					
					//Merge line trace under the same sub+target
					HashMap<String, TreeSet<Integer>> subTraceUnion = new HashMap<String, TreeSet<Integer>>();
					HashMap<String, TreeSet<Integer>> subInstTraceUnion = new HashMap<String, TreeSet<Integer>>();
					
					HashMap<String, TreeSet<Integer>> targetTraceUnion = new HashMap<String, TreeSet<Integer>>();
					HashMap<String, TreeSet<Integer>> targetInstTraceUnion = new HashMap<String, TreeSet<Integer>>();
					
					HashMap<String, GraphTuple> cloneTraceMap = to.graphMap.get(subTargetKey);
					for (String lineTraceRep: cloneTraceMap.keySet()) {
						System.out.println("Line trace: " + lineTraceRep);
						GraphTuple gt = cloneTraceMap.get(lineTraceRep);
						
						System.out.println("Sub trace: " + gt.subMethodTrace.unitTrace);
						System.out.println("Sub lines: " + gt.subMethodTrace.lineSum);
						System.out.println("Sub insts: " + gt.subMethodTrace.instSum);
						System.out.println("Target seg trace: " + gt.targetMethodTrace.unitTrace);
						System.out.println("Target lines: " + gt.targetMethodTrace.lineSum);
						System.out.println("Target insts: " + gt.targetMethodTrace.instSum);
						
						this.combineMap(gt.subMethodTrace.unitTrace, subTraceUnion);
						this.combineMap(gt.subMethodTrace.unitInstTrace, subInstTraceUnion);
						
						this.combineMap(gt.targetMethodTrace.unitTrace, targetTraceUnion);
						this.combineMap(gt.targetMethodTrace.unitInstTrace, targetInstTraceUnion);
					}
					
					String[] keys = subTargetKey.split("-");
					if (totalRecorder.size() == 0) {
						totalRecorder.put(keys[0], subTraceUnion);
						totalInstRecorder.put(keys[0], subInstTraceUnion);
						
						totalRecorder.put(keys[1], targetTraceUnion);
						totalInstRecorder.put(keys[1], targetInstTraceUnion);
					} else {
						HashMap<String, TreeSet<Integer>> preSub = totalRecorder.get(keys[0]);
						this.combineMap(subTraceUnion, preSub);
						HashMap<String, TreeSet<Integer>> preSubInsts = totalInstRecorder.get(keys[0]);
						this.combineMap(subInstTraceUnion, preSubInsts);
						
						HashMap<String, TreeSet<Integer>> preTarget = totalRecorder.get(keys[1]);
						this.combineMap(targetTraceUnion, preTarget);
						HashMap<String, TreeSet<Integer>> preTargetInsts = totalInstRecorder.get(keys[1]);
						this.combineMap(targetInstTraceUnion, preTargetInsts);
					}
				}
				
				System.out.println("Combined result");
				for (String methodKey: totalRecorder.keySet()) {
					System.out.println("Method: " + methodKey);
					HashMap<String, TreeSet<Integer>> lineTrace = totalRecorder.get(methodKey);
					int lineNumber = this.sumUp(totalRecorder.get(methodKey));
					//int instNumber = this.sumUp(totalInstRecorder.get(methodKey));
					System.out.println("Line trace: " + totalRecorder.get(methodKey));
					System.out.println("Line number:" + this.sumUp(totalRecorder.get(methodKey)));
					//System.out.println("Inst number: " + this.sumUp(totalInstRecorder.get(methodKey)));
					sb.append(methodKey + "," + lineTrace.toString().replace(",", ":") + "," + lineNumber + ",");
				}
				sb.append("\n");
				System.out.println();
			}
			double avgLinePerClone = totalCloneLines/totalCloneNum;
			System.out.println("Clone number: " + totalCloneNum);
			System.out.println("Line number: " + totalCloneLines);
			System.out.println("Avg. line/clone: " + avgLinePerClone);
			
			sb.append("\n");
			sb.append(summaryHeader + "\n");
			sb.append(instThreshold + "," + simThreshold + "," + totalCloneNum + "," + totalCloneLines + "," + avgLinePerClone + "\n");
			
			String compName = lib1 + "-" + lib2 + "-" + simThreshold + "-" + instThreshold;
			File f = new File(MIBConfiguration.getInstance().getResultDir() + "/" + compName + ".csv");
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			bw.append(sb.toString());
			bw.flush();
			bw.close();
			
			/*StringBuilder sb = new StringBuilder();
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
			bw.close();*/
			
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
		
		System.out.println("Instruction threshold:");
		final int instThreshold = Integer.valueOf(console.readLine());
		
		System.out.println("Similarity threshold:");
		final double simThreshold = Double.valueOf(console.readLine());
		
		ExecutorService traceExecutor = Executors.newFixedThreadPool(MIBConfiguration.getInstance().getParallelFactor());
		for (int i = startId; i <= endId; i++) {
			final int compId = i;
			traceExecutor.submit(new Runnable() {
				@Override
				public void run() {
					TraceAnalyzer ta = new TraceAnalyzer();
					ta.analyzeResult(url, username, password, compId, instThreshold, simThreshold);
				}
			});
		}
		
		traceExecutor.shutdown();
		while (!traceExecutor.isTerminated());
		System.out.println("Trace analysis ends");
	}
	
	public static class TraceObject {
		
		//public HashMap<String, GraphTuple> graphMap = new HashMap<String, GraphTuple>();
		
		public HashMap<String, HashMap<String, GraphTuple>> graphMap = new HashMap<String, HashMap<String, GraphTuple>>();
	}
	
	public static class MethodTrace {
		
		public HashMap<String, TreeSet<Integer>> unitTrace;
		
		public HashMap<String, TreeSet<Integer>> unitInstTrace;
		
		public int lineSum;
		
		public int instSum;
	}
	
	public static class GraphTuple {
		
		public String sub;
		
		public String repSubId;
		
		public String target;
		
		public String repTargetId;
		
		public String subTrace;
		
		public String targetTrace;
		
		private MethodTrace subMethodTrace;
		
		private MethodTrace targetMethodTrace;
		
		public void setSubMethodTrace(MethodTrace subMethodTrace) {
			this.subMethodTrace = subMethodTrace;
		}
		
		public MethodTrace getSubMethodTrace() {
			return this.subMethodTrace;
		}
		
		public void setTargetMethodTrace(MethodTrace targetMethodTrace) {
			this.targetMethodTrace = targetMethodTrace;
		}
		
		public MethodTrace getTargetMethodTrace() {
			return this.targetMethodTrace;
		}
		
		public int getTotalLinenumber() {
			return subMethodTrace.lineSum + targetMethodTrace.lineSum;
		}
		
		@Override
		public String toString() {
			return sub + "-" + subTrace + "-" + target + "-" + targetTrace;
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof GraphTuple))
				return false;
			
			GraphTuple tmp = (GraphTuple)o;
			if (tmp.toString().equals(this.toString())) {
				return true;
			} else {
				return false;
			}
		}
		
		@Override
		public int hashCode() {
			return this.toString().hashCode();
		}
		
	}
}
