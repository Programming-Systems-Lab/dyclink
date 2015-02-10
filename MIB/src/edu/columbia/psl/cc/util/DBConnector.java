package edu.columbia.psl.cc.util;

import java.io.Console;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.apache.log4j.Logger;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.HotZone;

public class DBConnector {
	
	private Logger logger = Logger.getLogger(DBConnector.class);
	
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
			Comparison compResult) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			Connection connect = DriverManager.getConnection(url, username, password);
			String query = "INSERT INTO comp_table (inst_thresh, inst_cat, lib1, lib2, " +
					"method1, method2, " +
					"method_f_1, method_f_2, m_compare, " +
					"sub_crawl, sub_crawl_filter, time)" + 
					" VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			
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
			pStmt.setDouble(12, compResult.time);
			
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
			String query = "INSERT INTO result_table (comp_id, sub, target, s_centroid, s_centroid_line, " +
					"t_start, t_start_line, t_centroid, t_centroid_line, t_end, t_end_line, " +
					"seg_size, static_dist, dyn_dist, similarity) " + 
					"VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			
			PreparedStatement pStmt = connect.prepareStatement(query);
			for (int i = 0; i < hotzones.size(); i++) {
				HotZone hotzone = hotzones.get(i);
				pStmt.setInt(1, compId);
				pStmt.setString(2, hotzone.getSubGraphName());
				pStmt.setString(3, hotzone.getTargetGraphName());
				pStmt.setString(4, hotzone.getSubCentroid().toString());
				pStmt.setInt(5, hotzone.getSubCentroid().getLinenumber());
				pStmt.setString(6, hotzone.getStartInst().toString());
				pStmt.setInt(7, hotzone.getStartInst().getLinenumber());
				pStmt.setString(8, hotzone.getCentralInst().toString());
				pStmt.setInt(9, hotzone.getCentralInst().getLinenumber());
				pStmt.setString(10, hotzone.getEndInst().toString());
				pStmt.setInt(11, hotzone.getEndInst().getLinenumber());
				pStmt.setInt(12, hotzone.getSegs().size());
				pStmt.setDouble(13, hotzone.getInstDistance());
				pStmt.setInt(14, hotzone.getLevDist());
				pStmt.setDouble(15, hotzone.getSimilarity());
				
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
	
	public static void main(String args[]) {
		Console console = System.console();
		if (console == null) {
			System.err.println("Null consol");
			System.exit(1);
		}
		
		//String url = console.readLine("DB url: ");
		//String username = console.readLine("DB username: ");
		String url = MIBConfiguration.getInstance().getDburl();
		String username = MIBConfiguration.getInstance().getDbusername();
		char[] passArray = console.readPassword("DB password: ");
		String password = new String(passArray);
		DBConnector connector = new DBConnector();
		System.out.println("Prob db: " + connector.probeDB(url, username, password));
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
