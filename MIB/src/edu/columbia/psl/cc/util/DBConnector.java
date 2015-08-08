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
	
	private static String baseurl;
	
	private static String username;
	
	private static String password;
	
	private static Connection connect;
	
	public static void init(String baseurl, String username, String password) {
		DBConnector.baseurl = baseurl;
		DBConnector.username = username;
		DBConnector.password = password;
	}
	
	public static Connection getConnection() {
		if (baseurl == null || username == null || password == null)
			return null;
		try {
			if (connect == null || !connect.isValid(5)) {
				Class.forName("com.mysql.jdbc.Driver");
				connect = DriverManager.getConnection(baseurl, username, password);
			}
						
			return connect;
		} catch (Exception ex) {
			logger.error("Exception: ", ex);
			logger.info("Try reconnect to DB...");
			try {
				connect = DriverManager.getConnection(baseurl, username, password);
				return connect;
			} catch (Exception ex2) {
				logger.error("Second time fails to connect: ", ex2);
			}
		}
		return null;
	}
	
	public static boolean probeDB() {
		if (baseurl == null || username == null || password == null)
			return false;
		
		try {
			//Class.forName("com.mysql.jdbc.Driver");
			//Connection connect = DriverManager.getConnection(baseurl, username, password);
			Connection connect = DBConnector.getConnection();
			boolean probe = connect.isValid(10);
			//connect.close();
			return probe;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}
	
	public static int writeCompTableResult(double sThresh,
			double dThresh,
			Date now,
			Comparison compResult) {
		try {
			//Class.forName("com.mysql.jdbc.Driver");
			//Connection connect = DriverManager.getConnection(url, username, password);
			Connection connect = DBConnector.getConnection();
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
				//connect.close();
				return -1;
			} else {
				ResultSet genKey = pStmt.getGeneratedKeys();
				if (genKey.next()) {
					int ret = genKey.getInt(1);
					pStmt.close();
					//connect.close();
					return ret;
				} else {
					pStmt.close();
					//connect.close();
					return -1;
				}
			}
		} catch (Exception ex) {
			logger.error("Exception: ", ex);
		}
		return -1;
	}
	
	public static boolean writeDetailTableResult(int compId, List<HotZone> hotzones) {
		try {
			
			String writerLabel = "";
			if (hotzones.size() > 0) {
				HotZone zone = hotzones.get(0);
				writerLabel = zone.getTargetGraphName() + " vs " + zone.getSubGraphName();
				logger.info(writerLabel + " starts");
				logger.info("Hotzone to write: " + hotzones.size());
			} else {
				logger.info("Empty zones");
				return true;
			}
			
			//Class.forName("com.mysql.jdbc.Driver");
			//Connection connect = DriverManager.getConnection(url, username, password);
			Connection connect = DBConnector.getConnection();
			String query = "INSERT INTO result_table2 (comp_id, sub, sid, target, tid, s_start, s_centroid, s_centroid_line, s_centroid_caller, s_end, s_trace, " +
					"t_start, t_centroid, t_centroid_line, t_centroid_caller, t_end, t_trace, " +
					"seg_size, static_dist, similarity) " + 
					"VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			
			PreparedStatement pStmt = connect.prepareStatement(query);
			int hotSize = hotzones.size();
			for (int i = 0; i < hotSize; i++) {
				HotZone hotzone = hotzones.get(i);
				pStmt.setInt(1, compId);
				pStmt.setString(2, hotzone.getSubGraphName());
				pStmt.setString(3, hotzone.getSubGraphId());
				pStmt.setString(4, hotzone.getTargetGraphName());
				pStmt.setString(5, hotzone.getTargetGraphId());
				pStmt.setString(6, hotzone.getSubStart().toString());
				pStmt.setString(7, hotzone.getSubCentroid().toString());
				pStmt.setInt(8, hotzone.getSubCentroid().getLinenumber());
				pStmt.setInt(9, hotzone.getSubCentroid().callerLine);
				pStmt.setString(10, hotzone.getSubEnd().toString());
				pStmt.setString(11, hotzone.getSubTrace());
				pStmt.setString(12, hotzone.getStartInst().toString());
				pStmt.setString(13, hotzone.getCentralInst().toString());
				pStmt.setInt(14, hotzone.getCentralInst().getLinenumber());
				pStmt.setInt(15, hotzone.getCentralInst().callerLine);
				pStmt.setString(16, hotzone.getEndInst().toString());
				pStmt.setString(17, hotzone.getTargetTrace());
				pStmt.setInt(18, hotzone.getSegSize());
				pStmt.setDouble(19, hotzone.getInstDistance());
				pStmt.setDouble(20, hotzone.getSimilarity());
				
				pStmt.addBatch();
			}
			pStmt.executeBatch();
			
			pStmt.close();
			//connect.close();
			
			logger.info(writerLabel + " ends");
			return true;
		} catch (Exception ex) {
			logger.error("Exception: ", ex);
		}
		return false;
	}
	
	public static void main(String args[]) {
		
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
