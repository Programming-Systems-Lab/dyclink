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
import java.util.Comparator;
import java.util.HashMap;
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
	
private static String graphRepo = "../";
	
	private static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
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
			
			String query = "SELECT rt.* FROM result_table rt " +
					"INNER JOIN (SELECT sub, sid, target, t_start_caller, MAX(similarity) as sim " +
					"FROM result_table " +
					"WHERE comp_id=? and seg_size >=20 and target NOT LIKE '%ejml%' and sub NOT LIKE '%ejml%' and similarity > ? " +
					"GROUP BY sub, sid, target) max_rec " +
					"ON rt.sub = max_rec.sub and rt.sid = max_rec.sid and rt.target = max_rec.target and rt.similarity = max_rec.sim;";
					//"ORDER BY rt.sub, rt.sid, rt.target, rt.tid, rt.t_start_caller, rt.t_centroid_caller, rt.t_end_caller, rt.similarity;";
			
			PreparedStatement pStmt = connect.prepareStatement(query);
			pStmt.setInt(1, compId);
			pStmt.setDouble(2, MIBConfiguration.getInstance().getSimThreshold());
			ResultSet result = pStmt.executeQuery();
			
			HashMap<String, TreeSet<Integer>> subCache = new HashMap<String, TreeSet<Integer>>();
			HashMap<String, double[]> subInfoCache = new HashMap<String, double[]>();
			HashMap<String, GraphTemplate> targetCache = new HashMap<String, GraphTemplate>();
			
			Comparator<TraceObject> traceSorter = new Comparator<TraceObject>() {
				public int compare(TraceObject to1, TraceObject to2) {
					String subRep1 = to1.sub + "-" + to1.subTrace.toString();
					String subRep2 = to2.sub + "-" + to2.subTrace.toString();
					int subCompare = subRep1.compareTo(subRep2);
					if (subCompare != 0)
						return subCompare;
					else {
						String targetRep1 = to1.target + "-" + to1.targetTrace.toString();
						String targetRep2 = to2.target + "-" + to2.targetTrace.toString();
						int targetCompare = targetRep1.compareTo(targetRep2);
						if (targetCompare != 0)
							return targetCompare;
						else {
							return 0;
						}
					}
				}
			};
			TreeMap<TraceObject, Double> traceMap = new TreeMap<TraceObject, Double>(traceSorter);
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
				
				TreeSet<Integer> subTrace = null;
				String subLabel = null;
				double degree = 0;
				if (!subCache.containsKey(subId)) {
					File subFile = searchFile(possibleDirs, subId);
					//System.out.println("Test id: " + subId);
					GraphTemplate subGraph = GsonManager.readJsonGeneric(subFile, graphToken);
					//System.out.println("Test graph name: " + subGraph.getMethodName());
					
					subTrace = new TreeSet<Integer>();
					for (InstNode inst: subGraph.getInstPool()) {
						subTrace.add(inst.getLinenumber());
					}
					double[] subInfo = {subGraph.getVertexNum(), subGraph.getEdgeNum()};
					subLabel = subGraph.getVertexNum() + ":" + subGraph.getEdgeNum();
					subCache.put(subId, subTrace);
					subInfoCache.put(subId, subInfo);
					degree = subInfo[1]/subInfo[0];
				} else {
					subTrace = subCache.get(subId);
					double[] subInfo = subInfoCache.get(subId);
					subLabel = subInfo[0] + ":" + subInfo[1];
					degree = subInfo[1]/subInfo[0];
				}
				
				if (degree < 1) {
					//System.out.println("Give up pair: " + subName + " vs " + targetName);
					//System.out.println("Sub degree: " + degree);
					continue ;
				}
				
				GraphTemplate targetGraph = null;
				if (!targetCache.containsKey(targetId)) {
					//System.out.println("Target id: " + targetId);
					File targetFile = searchFile(possibleDirs, targetId);
					targetGraph = GsonManager.readJsonGeneric(targetFile, graphToken);
					GraphConstructor reconstructor = new GraphConstructor();
					reconstructor.reconstructGraph(targetGraph, true);
					//System.out.println("Target graph name: " + targetGraph.getMethodName());
					targetCache.put(targetId, targetGraph);
				} else {
					targetGraph = targetCache.get(targetId);
				}
				
				String targetLabel = targetGraph.getVertexNum() + ":" + targetGraph.getEdgeNum();
				List<InstNode> segments = GraphUtil.sortInstPool(targetGraph.getInstPool(), true);
				int counter = 0;
				boolean startRecord = false;
				TreeSet<Integer> targetTrace = new TreeSet<Integer>();
				for (InstNode s: segments) {
					if (s.toString().equals(tStartInst)) {
						startRecord = true;
					}
					
					if (startRecord) {
						targetTrace.add(s.callerLine);
						counter++;
						
						if (counter == segSize) {
							break ;
						}
					}
				}
				
				TraceObject to = new TraceObject();
				to.sub = subName;
				to.subLabel = subLabel;
				to.subTrace = subTrace;
				to.target = targetName;
				to.targetLabel = targetLabel;
				to.targetTrace = targetTrace;
				to.segSize = segSize;
				to.staticSimilarity = staticSimilarity;
				
				if (!traceMap.containsKey(to)) {
					traceMap.put(to, similarity);
				} else {
					if (similarity > traceMap.get(to)) {
						traceMap.put(to, similarity);
					}
				}
			}
			
			StringBuilder sb = new StringBuilder();
			sb.append("*****************************************************************\n");
			sb.append(lib1 + " vs " + lib2 + "\n");
			for (TraceObject to: traceMap.keySet()) {
				sb.append("Test graph: " + to.sub + " " + to.subLabel + "\n");
				sb.append("Test line trace: ");
				sb.append(to.subTrace + "\n");
				sb.append("Target graph: " + to.target + " " + to.targetLabel + "\n");
				sb.append("Target line trace: ");
				sb.append(to.targetTrace + "\n");
				sb.append("Seg size: " + to.segSize + "\n");
				sb.append("Static similarity: " + to.staticSimilarity);
				sb.append("Similarity: " + traceMap.get(to) + "\n");
				sb.append("========================================\n");
				sb.append("\n");
			}
			sb.append("*****************************************************************\n");
			sb.append("\n");
			//System.out.println(sb.toString());
			
			String compName = lib1 + "-" + lib2;
			File f = new File(MIBConfiguration.getInstance().getResultDir() + "/" + compName + ".txt");
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
		public String sub;
		
		public String subLabel;
		
		public TreeSet<Integer> subTrace;
		
		public String target;
		
		public String targetLabel;
		
		public TreeSet<Integer> targetTrace;
		
		public double staticSimilarity;
		
		public int segSize;
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof TraceObject)) {
				return false;
			}
			
			TraceObject to = (TraceObject) o;
			if (!to.sub.equals(this.sub))
				return false;
			
			if (!to.subTrace.equals(this.subTrace))
				return false;
			
			if (!to.target.equals(this.target))
				return false;
			
			if (!to.targetTrace.equals(this.targetTrace))
				return false;
			
			return true;
		}
		
		@Override
		public int hashCode() {
			StringBuilder sb = new StringBuilder();
			sb.append(this.sub + "-");
			sb.append(this.subTrace.toString() + "-");
			sb.append(this.target + "-");
			sb.append(this.targetTrace.toString());
			return sb.toString().hashCode();
		}
	}
}
