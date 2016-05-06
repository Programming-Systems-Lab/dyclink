package edu.columbia.psl.cc.util;

import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.columbia.psl.cc.config.MIBConfiguration;

public class CodeRelQueryInterface {
	
	private final static String header = "method,real_label,1st,1st_sim,2nd,2nd_sim,3rd,3rd_sim,4th,4th_sim,5th,5th_sim,label_count,predict_label\n";
	
	private final static Logger logger = LogManager.getLogger(CodeRelQueryInterface.class);
	
	public static Options options = new Options();
	
	static {
		options.addOption("compId", true, "Comp. ID");
		options.addOption("insts", true, "Instrution size");
		options.addOption("similarity", true, "Similarity threshold");
		options.addOption("filter", false, "Filter out read and next");
		
		options.getOption("compId").setRequired(true);
		options.getOption("insts").setRequired(true);
		options.getOption("similarity").setRequired(true);
	}
	
	public static void main(String[] args) {
		Console console = System.console();
		try {
			if (console == null) {
				System.err.println("Null consoel!");
				System.exit(-1);
			}
						
			System.out.println("Password: ");
			char[] passArray = console.readPassword();
			final String password = new String(passArray);
			
			CommandLineParser parser = new DefaultParser();
			CommandLine commands = parser.parse(options, args);
			
			int compId = Integer.valueOf(commands.getOptionValue("compId"));
			int segSize = Integer.valueOf(commands.getOptionValue("insts"));
			double simThresh = Double.valueOf(commands.getOptionValue("similarity"));
			boolean filter = commands.hasOption("filter");
						
			String username = MIBConfiguration.getInstance().getDbusername();
			String dburl = MIBConfiguration.getInstance().getDburl();
			
			System.out.println("Confirm query settings:");
			System.out.println("DB url: " + dburl);
			System.out.println("Instruction size: " + segSize);
			System.out.println("Similarity threshold: " + simThresh);
			System.out.println("Filter next and read: " + filter);
			
			Class.forName("com.mysql.jdbc.Driver");
			Connection connect = DriverManager.getConnection(dburl, username, password);
			
			String queryString = "SELECT * FROM result_table2 WHERE comp_id = " + compId 
					+ " and seg_size >= " + segSize 
					+ " and similarity >= " + simThresh;
			
			PreparedStatement queryStatement = connect.prepareStatement(queryString);
			ResultSet result = queryStatement.executeQuery();
			//Key is method pair
			HashMap<TreeSet<String>, CodeRel> codeRels = new HashMap<TreeSet<String>,CodeRel> ();
			while (result.next()) {
				String sub = result.getString("sub");
				String target = result.getString("target");
				
				if (sub.equals(target)) {
					logger.error("Suspicious: " + sub + " " + target);
					System.exit(-1);
				}
				
				String subMethodName = sub.split(":")[1];
				String targetMethodName = target.split(":")[1];
				if (filter) {
					if (subMethodName.startsWith("read") || targetMethodName.startsWith("read")) {
						continue ;
					}
					
					if (subMethodName.startsWith("next") || targetMethodName.startsWith("next")) {
						continue ;
					}
				}
				
				TreeSet<String> pair = new TreeSet<String>();
				pair.add(sub);
				pair.add(target);
								
				String subStart = result.getString("s_start");
				String subCentroid = result.getString("s_centroid");
				String subEnd = result.getString("s_end");
				
				String targetStart = result.getString("t_start");
				String targetCentroid = result.getString("t_centroid");
				String targetEnd = result.getString("t_end");
				
				if (subStart.equals(targetStart) 
						&& subCentroid.equals(targetCentroid) 
						&& subEnd.equals(targetEnd)) {
					continue ;
				}
				
				HashSet<String> trace = new HashSet<String>();
				String subStartKey = ClusterAnalyzer.composeInstructionKey(subStart);
				String subCentroidKey = ClusterAnalyzer.composeInstructionKey(subCentroid);
				String subEndKey = ClusterAnalyzer.composeInstructionKey(subEnd);
				trace.add(subStartKey);
				trace.add(subCentroidKey);
				trace.add(subEndKey);
				
				
				String targetStartKey = ClusterAnalyzer.composeInstructionKey(targetStart);
				String targetCentroidKey = ClusterAnalyzer.composeInstructionKey(targetCentroid);
				String targetEndKey = ClusterAnalyzer.composeInstructionKey(targetEnd);
				trace.add(targetStartKey);
				trace.add(targetCentroidKey);
				trace.add(targetEndKey);
				
				CodeRel cr = null;
				double sim = result.getDouble("similarity");
				if (codeRels.containsKey(pair)) {
					cr = codeRels.get(pair);
					cr.traces.add(trace);
					if (cr.sim < sim) {
						cr.sim = sim;
						cr.bestTrace = trace;
					}
				} else {
					cr = new CodeRel();
					cr.methodPair = pair;
					cr.traces.add(trace);
					cr.bestTrace = trace;
					cr.sim = sim;
					codeRels.put(pair, cr);
				}
			}
			
			List<CodeRel> crList = new ArrayList<CodeRel>(codeRels.values());
			HashSet<TreeSet<String>> toRemove = new HashSet<TreeSet<String>>();
			int dupRel = 0;
			for (int i = 0; i < crList.size(); i++) {
				CodeRel cr1 = crList.get(i);
				
				if (toRemove.contains(cr1.methodPair)) {
					continue ;
				}
				
				for (int j = i + 1; j < crList.size(); j++) {
					CodeRel cr2 = crList.get(j);
					
					if (cr1.bestTrace.equals(cr2.bestTrace)) {
						//If traces are the same, just keep one
						//logger.info("Dup rel: " + cr1.methodPair + " " + cr2.methodPair);
						dupRel++;
						toRemove.add(cr2.methodPair);
					}
				}
			}
			System.out.println("Total rel(before): " + crList.size());
			System.out.println("Dup rel: " + dupRel);
			
			for (TreeSet<String> r: toRemove) {
				codeRels.remove(r);
			}
			
			
			for (TreeSet<String> pair: codeRels.keySet()) {
				System.out.println(pair + "," + codeRels.get(pair).sim);
				//logger.info(codeRels.get(pair).bestTrace);
			}
			
			System.out.println("Total code rels: " + codeRels.size());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
	}
	
	public static class CodeRel {
		TreeSet<String> methodPair;
		
		HashSet<HashSet<String>> traces = new HashSet<HashSet<String>>();
		
		HashSet<String> bestTrace;
		
		double sim;
	}
}

