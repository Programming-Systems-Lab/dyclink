package edu.columbia.psl.cc.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import edu.columbia.psl.cc.pojo.HotZone;
import edu.columbia.psl.cc.util.DBConnector.Comparison;

import java.util.List;
import java.util.ArrayList;

public class CSVConverter {
	
	public static void writeDetails(int compId, File csvFile) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(csvFile));
			String buf = "";
			
			int headCount = 0;
			int count = 0;
			
			Connection connect = DBConnector.getConnection();
			String query = "INSERT INTO result_table2 (comp_id, sub, sid, target, tid, s_start, s_centroid, s_centroid_line, s_centroid_caller, s_end, s_trace, " +
					"t_start, t_centroid, t_centroid_line, t_centroid_caller, t_end, t_trace, " +
					"seg_size, static_dist, similarity) " + 
					"VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			
			PreparedStatement pStmt = connect.prepareStatement(query);
			
			while ((buf = br.readLine()) != null) {
				if (headCount < 4) {
					headCount++;
					continue ;
				}
				
				String[] row = buf.split(",");
				String sub = row[0];
				String sid = row[1];
				String target = row[2];
				String tid = row[3];
				String s_start = row[4];
				String s_centroid = row[5];
				int s_centroid_line = Integer.valueOf(row[6]);
				int s_centroid_caller = Integer.valueOf(row[7]);
				String s_end = row[8];
				String s_trace = row[9];
				String t_start = row[10];
				String t_centroid = row[11];
				int t_centroid_line = Integer.valueOf(row[12]);
				int t_centroid_caller = Integer.valueOf(row[13]);
				String t_end = row[14];
				String t_trace = row[15];
				int seg_size = Integer.valueOf(row[16]);
				double inst_dist = Double.valueOf(row[17]);
				double similarity = Double.valueOf(row[18]);
				
				System.out.println("Check row data with compId: " + compId);
				System.out.println("sub sid target tid: " + sub + " " + sid + " " + target + " " + tid);
				System.out.println("s_start s_centroid: " + s_start + " " + s_centroid);
				System.out.println("s_centroid_line s_centroid_caller: " + s_centroid_line + " " + s_centroid_caller);
				System.out.println("s_end s_trace: " + s_end + " " + s_trace);
				System.out.println("t_start t_centroid: " + t_start + " " + t_centroid);
				System.out.println("t_centroid_line: " + t_centroid_line);
				System.out.println("t_end t_trace: " + t_end + " " + t_trace);
				System.out.println("seg_size inst_idst similarity: " + seg_size + " " + inst_dist + " " + similarity);
				
				pStmt.setInt(1, compId);
				pStmt.setString(2, sub);
				pStmt.setString(3, sid);
				pStmt.setString(4, target);
				pStmt.setString(5, tid);
				pStmt.setString(6, s_start);
				pStmt.setString(7, s_centroid);
				pStmt.setInt(8, s_centroid_line);
				pStmt.setInt(9, s_centroid_caller);
				pStmt.setString(10, s_end);
				pStmt.setString(11, s_trace);
				pStmt.setString(12, t_start);
				pStmt.setString(13, t_centroid);
				pStmt.setInt(14, t_centroid_line);
				pStmt.setInt(15, t_centroid_caller);
				pStmt.setString(16, t_end);
				pStmt.setString(17, t_trace);
				pStmt.setInt(18, seg_size);
				pStmt.setDouble(19, inst_dist);
				pStmt.setDouble(20, similarity);
				
				pStmt.addBatch();
				
				count++;
			}
			pStmt.executeBatch();
			
			pStmt.close();
			br.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		try {
			String dataLoc = args[0];
			System.out.println("Confirm data loc: " + dataLoc);
			File csvFile = new File(dataLoc);
			BufferedReader br = new BufferedReader(new FileReader(csvFile));
			
			String buf = "";
			int counter = 0;
			int compId = -1;
			while ((buf = br.readLine()) != null) {
				if (counter == 0) {
					counter++;
					continue ;
				} else if (counter == 1) {
					Comparison comp = new Comparison();
					String[] data = buf.split(",");
					comp.lib1 = data[0];
					comp.lib2 = data[1];
					comp.inst_thresh = Integer.valueOf(data[2]);
					comp.inst_cat = Integer.valueOf(data[3]);
					comp.method1 = Integer.valueOf(data[4]);
					comp.method2 = Integer.valueOf(data[5]);
					comp.method_f_1 = Integer.valueOf(data[6]);
					comp.method_f_2 = Integer.valueOf(data[7]);
					comp.m_compare = Integer.valueOf(data[8]);
					comp.sub_crawl = Integer.valueOf(data[9]);
					comp.sub_crawl_filter = Integer.valueOf(data[10]);
					double s_thresh = Double.valueOf(data[11]);
					double t_thresh = Double.valueOf(data[12]);
					comp.time = Double.valueOf(data[13]);
					DateFormat dateFormat = new SimpleDateFormat("EEE MMM dd kk:mm:ss z yyyy");
					Date d = dateFormat.parse(data[14]);
					Timestamp ts = new Timestamp(d.getTime());
					
					System.out.println("Check comp result");
					System.out.println(comp.lib1 + " " + comp.lib2);
					System.out.println("inst thresh and inst cat: " + comp.inst_thresh + " " + comp.inst_cat);
					System.out.println("method1 method2: " + comp.method1 + " " + comp.method2);
					System.out.println("method_f_1 method_f_2: " + comp.method_f_1 + " " + comp.method_f_2);
					System.out.println("m_compare: " + comp.m_compare);
					System.out.println("Sub crawl and filter: " + comp.sub_crawl + " " + comp.sub_crawl_filter);
					System.out.println("s_thresh and t_thresh: " + s_thresh + " " + t_thresh);
					System.out.println("time: " + comp.time);
					System.out.println("time stamp: " + ts);
					
					compId = DBConnector.writeCompTableResult(s_thresh, t_thresh, ts, comp);
					break ;
				}
			}
			br.close();
			
			if (compId >= 0) {
				writeDetails(compId, csvFile);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static class HotZoneWrapper extends HotZone {
		
	}

}
