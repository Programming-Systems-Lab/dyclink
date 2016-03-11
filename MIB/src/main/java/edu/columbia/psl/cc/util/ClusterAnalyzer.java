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
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.columbia.psl.cc.config.MIBConfiguration;

public class ClusterAnalyzer {
	
	private final static String header = "method,real_label,1st,1st_sim,2nd,2nd_sim,3rd,3rd_sim,4th,4th_sim,5th,5th_sim,label_count,predict_label\n";
	
	public static Options options = new Options();
	
	private static Logger logger = LogManager.getLogger(ClusterAnalyzer.class);
	
	static {
		options.addOption("start", true, "Comp id (start)");
		options.addOption("end", true, "Comp id (end)");
		options.addOption("k", true, "Neightbor number");
		options.addOption("insts", true, "Instrution size");
		options.addOption("similarity", true, "Similarity threshold");
		options.addOption("filter", false, "Filter out read and next");
		options.addOption("break", false, "Break tie");
		
		options.getOption("start").setRequired(true);
		options.getOption("end").setRequired(true);
		options.getOption("k").setRequired(true);
		options.getOption("insts").setRequired(true);
		options.getOption("similarity").setRequired(true);
	}
	
	public static String composeInstructionKey(String rawInst) {
		String[] rawInstInfo = rawInst.split(" ");
		String key = rawInstInfo[0] + "@" + rawInstInfo[3];
		return key;
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
			
			int compIdStart = Integer.valueOf(commands.getOptionValue("start"));
			int compIdEnd = Integer.valueOf(commands.getOptionValue("end"));
			int kNum = Integer.valueOf(commands.getOptionValue("k"));
			int segSize = Integer.valueOf(commands.getOptionValue("insts"));
			double simThresh = Double.valueOf(commands.getOptionValue("similarity"));
			boolean filter = commands.hasOption("filter");
			boolean breakTie = commands.hasOption("break");
						
			String username = MIBConfiguration.getInstance().getDbusername();
			String dburl = MIBConfiguration.getInstance().getDburl();
			
			logger.info("Confirm query settings:");
			logger.info("DB url: " + dburl);
			logger.info("Comp id (start): " + compIdStart);
			logger.info("Comp id (end): " + compIdEnd); 
			logger.info("Instruction size: " + segSize);
			logger.info("Similarity threshold: " + simThresh);
			logger.info("Filter next and read: " + filter);
			logger.info("Break tie: " + breakTie);
			
			Class.forName("com.mysql.jdbc.Driver");
			Connection connect = DriverManager.getConnection(dburl, username, password);
			
			Set<String> allMethods = new HashSet<String>();
			String subQuery = "SELECT distinct sub FROM result_table2 WHERE comp_id between " + compIdStart + " and " + compIdEnd;
			PreparedStatement subStatement = connect.prepareStatement(subQuery);
			ResultSet subResult = subStatement.executeQuery();
			int subCount = 0;
			while (subResult.next()) {
				String subName = subResult.getString("sub");
				allMethods.add(subName);
				subCount++;
			}
			logger.info("# of sub methods: " + subCount);
			
			String targetQuery = "SELECT distinct target FROM result_table2 WHERE comp_id between " + compIdStart + " and " + compIdEnd;
			PreparedStatement targetStatement = connect.prepareStatement(targetQuery);
			ResultSet targetResult = targetStatement.executeQuery();
			int targetCount = 0;
			while (targetResult.next()) {
				String targetName = targetResult.getString("target");
				allMethods.add(targetName);
				targetCount++;
			}
			logger.info("# of target methods: " + targetCount);
			logger.info("# of total methods: " + allMethods.size());
			
			StringBuilder result = new StringBuilder();
			result.append(header);
			int validMethods = 0;
			int correctMethods = 0;
			for (String method: allMethods) {
				StringBuilder row = new StringBuilder();
				//String method = "R5P1Y14.darnley.A:solve:():Ljava.lang.String";
				String[] myInfo = method.split("\\.");
				String myLabel = myInfo[0];
				String myName = myInfo[1];
				String myMethod = method.split(":")[1];
				
				//Filter out readXX, or nextXX, which are some little utility functions
				if (filter) {
					if (myMethod.startsWith("read") || myMethod.startsWith("next")) {
						//System.out.println("Filter out utility method: " + method + "\n");
						continue ;
					}
				}
				
				logger.info("Me: " + method);
				row.append(method + "," + myLabel + ",");
				
				String knnQuery = "SELECT rt.* FROM result_table2 rt " +
									"INNER JOIN (SELECT sub, target, MAX(similarity) as sim " +
										"FROM result_table2 " +
										"WHERE (comp_id between ? and ?) and seg_size >= ? and similarity >= ? and (sub = ? or target = ?) " +    
										"GROUP BY sub, target) max_rec " +
										"ON rt.sub = max_rec.sub and rt.target = max_rec.target and rt.similarity = max_rec.sim and rt.seg_size >= ? " +
									"WHERE (comp_id between ? and ?) ORDER BY similarity desc;";
				PreparedStatement knnStatement = connect.prepareStatement(knnQuery);
				knnStatement.setInt(1, compIdStart);
				knnStatement.setInt(2, compIdEnd);
				knnStatement.setInt(3, segSize);
				knnStatement.setDouble(4, simThresh);
				knnStatement.setString(5, method);
				knnStatement.setString(6, method);
				knnStatement.setInt(7, segSize);
				knnStatement.setInt(8, compIdStart);
				knnStatement.setInt(9, compIdEnd);
				
				ResultSet knnResult = knnStatement.executeQuery();
				
				int count = 0;
				
				double lastSimilarity = 0;
				HashMap<String, List<Neighbor>> neighborRecord = new HashMap<String, List<Neighbor>>();
				HashSet<String> neighborTraceCache = new HashSet<String>();
				HashSet<String> neighborCache = new HashSet<String>();
				while (knnResult.next()) {				
					String knnSub = knnResult.getString("sub");
					String knnTarget = knnResult.getString("target");
					double similarity = knnResult.getDouble("similarity");
					
					if (count >= kNum) {
						if (breakTie) {
							break ;
						} else {
							if (similarity < lastSimilarity) {
								break ;
							}
						}
					}
					
					String neighbor = "";
					String trace = "";
					boolean checkSub = true;
					if (knnSub.equals(method)) {
						neighbor = knnTarget;
						checkSub = false;
					} else {
						neighbor = knnSub;
					}
					
					//Skip the the same username from different years?
					String[] neighborInfo = neighbor.split("\\.");
					String neighborLabel = neighborInfo[0];
					String neighborName = neighborInfo[1];
					String neighborMethod = neighbor.split(":")[1];
					
					//To be conservative, for a user project in the same year, only pick one
					//String neighborKey = neighborLabel + "-" + neighborName;
					
					/*if (neighborName.equals(myName))
						continue ;*/
					
					if (filter) {
						if (neighborMethod.startsWith("read") || neighborMethod.startsWith("next")) {
							//System.out.println("Filter neighbor utility method: " + neighbor);
							continue ;
						}
					}
										
					if (checkSub) {
						String subStart = knnResult.getString("s_start");
						//To be more precise, extract method name and inst id
						String subStartKey = composeInstructionKey(subStart);
						
						String subCentroid = knnResult.getString("s_centroid");
						String subCentroidKey = composeInstructionKey(subCentroid);
						
						String subEnd = knnResult.getString("s_end");
						String subEndKey = composeInstructionKey(subEnd);
						
						trace = subStartKey + "-" + subCentroidKey + "-" + subEndKey;
					} else {
						String targetStart = knnResult.getString("t_start");
						String targetStartKey = composeInstructionKey(targetStart);
						
						String targetCentroid = knnResult.getString("t_centroid");
						String targetCentroidKey = composeInstructionKey(targetCentroid);
						
						String targetEnd = knnResult.getString("t_end");
						String targetEndKey = composeInstructionKey(targetEnd);
						
						trace = targetStartKey + "-" + targetCentroidKey + "-" + targetEndKey;
					}
					
					//logger.info("Trace: " + trace);
					
					if (neighborCache.contains(neighbor)) {
						logger.info("Dup neighbor: " + neighbor);
						continue ;
					}
					neighborCache.add(neighbor);
					
					if (neighborTraceCache.contains(trace)) {
						logger.info("Dup trace: " + trace);
						continue ;
					}
					neighborTraceCache.add(trace);
					
					//Record the best
					if (count < 5)
						row.append(neighbor + "," + similarity + ",");
					
					Neighbor newNeighbor = new Neighbor();
					newNeighbor.methodName = neighbor;
					newNeighbor.username = neighborName;
					newNeighbor.label = neighborLabel;
					newNeighbor.similarity = similarity;
										
					if (!neighborRecord.containsKey(neighborLabel)) {
						List<Neighbor> neighborList = new ArrayList<Neighbor>();
						neighborList.add(newNeighbor);
						neighborRecord.put(neighborLabel, neighborList);
					} else {
						neighborRecord.get(neighborLabel).add(newNeighbor);
					}
					
					count++;
					lastSimilarity = similarity;
				}
				
				if (neighborRecord.size() == 0) {
					logger.info("Query no result\n");
					continue ;
				}
				
				validMethods++;
				
				int remained = 5- count;
				for (int i = 0; i < remained; i++) {
					row.append(" , ,");
				}
				
				//System.out.println("Check neighbor count: ");
				List<String> bestLabels = new ArrayList<String>();
				int bestLabelCount = Integer.MIN_VALUE;
				String countString = "";
				for (String label: neighborRecord.keySet()) {
					int labelCount = neighborRecord.get(label).size();
					logger.info("Label: " + label + " " + labelCount);
					String labelSummary = label + ":" + labelCount + "-";
					countString += labelSummary;
					
					if (labelCount >= bestLabelCount) {
						if (labelCount > bestLabelCount) {
							bestLabelCount = labelCount;
							bestLabels.clear();
							bestLabels.add(label);
						} else {
							bestLabels.add(label);
						}
					}
				}
				countString = countString.substring(0, countString.length() - 1);
				//System.out.println("Count string: " + countString);
				row.append(countString + ",");
				//System.out.println("Check best labels: " + bestLabels);
				
				String bestLabel = "";
				if (bestLabels.size() == 1) {
					bestLabel = bestLabels.get(0);
				} else {
					double bestSum = 0;
					for (String label: bestLabels) {
						double curSum = 0;
						List<Neighbor> neighbors = neighborRecord.get(label);
						for (Neighbor n: neighbors) {
							curSum += n.similarity;
						}
						
						if (curSum > bestSum) {
							bestSum = curSum;
							bestLabel = label;
						}	
					}
				}
				//System.out.println("Best label: " + bestLabel + "\n");
				row.append(bestLabel + "\n");
				result.append(row);
				logger.info("My label: " + myLabel);
				logger.info("Best label: " + bestLabel);
				
				if (myLabel.equals(bestLabel)) {
					correctMethods++;
				}
			}
			
			logger.info("Valid methods: " + validMethods);
			logger.info("Correct methods: " + correctMethods);
			logger.info("Precision: " + ((double)correctMethods)/validMethods);
			
			File resultDir = new File("./results");
			if (!resultDir.exists()) {
				resultDir.mkdir();
			}
			
			String simString = String.valueOf(simThresh).split("\\.")[1];
			String filterString = (filter==true?"f":"u");
			String fileName = resultDir.getAbsolutePath() + "/knn_result_" + kNum + "_" + segSize + "_" + simString + "_" + filterString + ".csv";
			BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
			bw.write(result.toString());
			bw.close();
			//System.out.println("Result path: " + fileName);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
	}
	
	public static class Neighbor {
		String methodName;
		
		String username;
		
		String label;
				
		double similarity;
	}

}
