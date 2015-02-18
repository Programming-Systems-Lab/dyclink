package edu.columbia.psl.cc.util;

import java.io.Console;
import java.io.File;
import java.io.FilenameFilter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.HotZone;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;

public class DBConnector {
	
	private static Logger logger = Logger.getLogger(DBConnector.class);
	
	private static String graphRepo = "/Users/mikefhsu/Mike/Research/ec2/mib_sandbox/";
	
	private static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
	private static File searchFile(List<String> possibleDirs, final String fileEnd) {
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
	
	public boolean probeDB(String baseurl, String username, String password) {
		if (baseurl == null || username == null || password == null)
			return false;
		
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection connect = DriverManager.getConnection(baseurl, username, password);
			return connect.isValid(10);
			//String query = "SELECT User, Password FROM User WHERE User = \'" + username + "\' and Password = \'" + password + "\';";
			/*String query = "SELECT User, Password FROM User";
			System.out.println("Query string: " + query);
			Statement stmt = connect.createStatement();
			ResultSet result = stmt.executeQuery(query);
			
			while (result.next()) {
				if (result.getString("User").equals(username) && result.getString("Password").equals(password)) {
					return true;
				} else {
					return false;
				}
			}*/
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}
	
	public int writeCompTableResult(String url, 
			String username, 
			String password,
			double sThresh,
			double dThresh,
			Date now,
			Comparison compResult) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection connect = DriverManager.getConnection(url, username, password);
			String query = "INSERT INTO comp_table (inst_thresh, inst_cat, lib1, lib2, " +
					"method1, method2, " +
					"method_f_1, method_f_2, m_compare, " +
					"sub_crawl, sub_crawl_filter, s_threshold, d_threshold, time, timestamp)" + 
					" VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			
			Timestamp ts = new Timestamp(now.getTime());
			
			PreparedStatement pStmt = connect.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
			pStmt.setInt(1, compResult.inst_thresh);
			pStmt.setInt(2, compResult.inst_cat);
			pStmt.setString(3, compResult.lib1);
			pStmt.setString(4, compResult.lib2);
			pStmt.setInt(5, compResult.method1);
			pStmt.setInt(6, compResult.method2);
			pStmt.setInt(7, compResult.method_f_1);
			pStmt.setInt(8, compResult.method_f_2);
			pStmt.setInt(9, compResult.m_compare);
			pStmt.setInt(10, compResult.sub_crawl);
			pStmt.setInt(11, compResult.sub_crawl_filter);
			pStmt.setDouble(12, sThresh);
			pStmt.setDouble(13, dThresh);
			pStmt.setDouble(14, compResult.time);
			pStmt.setTimestamp(15, ts);
			
			int insertRow = pStmt.executeUpdate();
			
			if (insertRow == 0) {
				logger.error("Insertion fail: " + compResult.lib1 + " " + compResult.lib2);
				pStmt.close();
				connect.close();
				return -1;
			} else {
				ResultSet genKey = pStmt.getGeneratedKeys();
				if (genKey.next()) {
					int ret = genKey.getInt(1);
					pStmt.close();
					connect.close();
					return ret;
				} else {
					pStmt.close();
					connect.close();
					return -1;
				}
			}
		} catch (Exception ex) {
			logger.error("Exception: ", ex);
		}
		return -1;
	}
	
	public void writeDetailTableResult(String url, 
			String username, 
			String password, 
			int compId,
			List<HotZone> hotzones) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection connect = DriverManager.getConnection(url, username, password);
			String query = "INSERT INTO result_table (comp_id, sub, sid, target, tid, s_centroid, s_centroid_line, " +
					"t_start, t_start_line, t_start_caller, t_centroid, t_centroid_line, t_centroid_caller, t_end, t_end_line, t_end_caller, " +
					"seg_size, static_dist, dyn_dist, similarity) " + 
					"VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			
			PreparedStatement pStmt = connect.prepareStatement(query);
			for (int i = 0; i < hotzones.size(); i++) {
				HotZone hotzone = hotzones.get(i);
				pStmt.setInt(1, compId);
				pStmt.setString(2, hotzone.getSubGraphName());
				pStmt.setString(3, hotzone.getSubGraphId());
				pStmt.setString(4, hotzone.getTargetGraphName());
				pStmt.setString(5, hotzone.getTargetGraphId());
				pStmt.setString(6, hotzone.getSubCentroid().toString());
				pStmt.setInt(7, hotzone.getSubCentroid().getLinenumber());
				pStmt.setString(8, hotzone.getStartInst().toString());
				pStmt.setInt(9, hotzone.getStartInst().getLinenumber());
				pStmt.setInt(10, hotzone.getStartInst().callerLine);
				pStmt.setString(11, hotzone.getCentralInst().toString());
				pStmt.setInt(12, hotzone.getCentralInst().getLinenumber());
				pStmt.setInt(13, hotzone.getCentralInst().callerLine);
				pStmt.setString(14, hotzone.getEndInst().toString());
				pStmt.setInt(15, hotzone.getEndInst().getLinenumber());
				pStmt.setInt(16, hotzone.getEndInst().callerLine);
				pStmt.setInt(17, hotzone.getSegs().size());
				pStmt.setDouble(18, hotzone.getInstDistance());
				pStmt.setInt(19, hotzone.getLevDist());
				pStmt.setDouble(20, hotzone.getSimilarity());
				
				pStmt.addBatch();
				
				if ((i % 100) == 0) {
					pStmt.executeBatch();
				}
			}
			pStmt.executeBatch();
			
			pStmt.close();
			connect.close();
		} catch (Exception ex) {
			logger.error("Exception: ", ex);
		}
	}
	
	public static void analyzeResult(String url, String username, String password, int compId) {
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
					"INNER JOIN (SELECT sub, sid, target, t_start_caller, t_centroid_caller, t_end_caller, MAX(similarity) as sim " +
					"FROM result_table " +
					"WHERE comp_id=? and seg_size >=20 and target NOT LIKE '%ejml%' and sub NOT LIKE '%ejml%' and similarity >= 0.8 " +
					"GROUP BY sub, sid, target, t_start_caller, t_centroid_caller, t_end_caller) max_rec " +
					"ON rt.sub = max_rec.sub and rt.sid = max_rec.sid and rt.target = max_rec.target and rt.t_start_caller = max_rec.t_start_caller and rt.t_centroid_caller = max_rec.t_centroid_caller and rt.t_end_caller = max_rec.t_end_caller and rt.similarity = max_rec.sim " +
					"ORDER BY rt.sub, rt.sid, rt.target, rt.tid, rt.t_start_caller, rt.t_centroid_caller, rt.t_end_caller, rt.similarity;";
			
			PreparedStatement pStmt = connect.prepareStatement(query);
			pStmt.setInt(1, compId);
			ResultSet result = pStmt.executeQuery();
			
			HashMap<String, TreeSet<Integer>> graphCache = new HashMap<String, TreeSet<Integer>>();
			HashMap<String, GraphTemplate> targetCache = new HashMap<String, GraphTemplate>();
			while (result.next()) {
				//Get graph object
				String subId = result.getString("sid").replace("-", ":") + ".json";
				String subName = result.getString("sub");
				String targetId = result.getString("tid").replace("-", ":") + ".json";
				String targetName = result.getString("target");
				String tStartInst = result.getString("t_start");
				int segSize = result.getInt("seg_size");
				
				TreeSet<Integer> subTrace = null;
				if (!graphCache.containsKey(subId)) {
					File subFile = searchFile(possibleDirs, subId);
					//System.out.println("Test id: " + subId);
					GraphTemplate subGraph = GsonManager.readJsonGeneric(subFile, graphToken);
					//System.out.println("Test graph name: " + subGraph.getMethodName());
					
					subTrace = new TreeSet<Integer>();
					for (InstNode inst: subGraph.getInstPool()) {
						subTrace.add(inst.getLinenumber());
					}
					graphCache.put(subId, subTrace);
				} else {
					subTrace = graphCache.get(subId);
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
				
				System.out.println("Test graph: " + subName + " " + subId);
				System.out.println("Test line trace: " + subTrace);
				
				System.out.println("Target graph: " + targetName + " " + targetId);
				System.out.println("Target line trace: " + targetTrace);
				System.out.println();
				
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void main(String args[]) {
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
		
		DBConnector.analyzeResult(url, username, password, 25);
	}
	
	public static class Comparison {
		public int inst_thresh;
		
		public int inst_cat;
		
		public String lib1;
		
		public String lib2;
		
		public int method1;
		
		public int method2;
		
		public int method_f_1;
		
		public int method_f_2;
		
		public int m_compare;
		
		public int sub_crawl;
		
		public int sub_crawl_filter;
		
		public double time;
	}

}
