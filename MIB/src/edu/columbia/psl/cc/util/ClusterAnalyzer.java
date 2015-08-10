package edu.columbia.psl.cc.util;

import java.io.BufferedWriter;
import java.io.Console;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.columbia.psl.cc.config.MIBConfiguration;

public class ClusterAnalyzer {
	
	private final static String header = "method,real_label,best_neighbor,similarity,label_count,predict_label\n";
	
	public static void main(String[] args) {
		Console console = System.console();
		try {
			if (console == null) {
				System.err.println("Null consoel!");
				System.exit(-1);
			}
			
			System.out.println("Comp id");
			int compId = Integer.valueOf(console.readLine());
			
			System.out.println("Password: ");
			char[] passArray = console.readPassword();
			final String password = new String(passArray);
			
			String username = MIBConfiguration.getInstance().getDbusername();
			String dburl = MIBConfiguration.getInstance().getDburl();
			
			Class.forName("com.mysql.jdbc.Driver");
			Connection connect = DriverManager.getConnection(dburl, username, password);
			
			Set<String> allMethods = new HashSet<String>();
			String subQuery = "SELECT distinct sub FROM result_table2 WHERE comp_id=" + compId;
			PreparedStatement subStatement = connect.prepareStatement(subQuery);
			ResultSet subResult = subStatement.executeQuery();
			int subCount = 0;
			while (subResult.next()) {
				String subName = subResult.getString("sub");
				allMethods.add(subName);
				subCount++;
			}
			System.out.println("# of sub methods: " + subCount);
			
			String targetQuery = "SELECT distinct target FROM result_table2 WHERE comp_id=" + compId;
			PreparedStatement targetStatement = connect.prepareStatement(targetQuery);
			ResultSet targetResult = targetStatement.executeQuery();
			int targetCount = 0;
			while (targetResult.next()) {
				String targetName = targetResult.getString("target");
				allMethods.add(targetName);
				targetCount++;
			}
			System.out.println("# of target methods: " + targetCount);
			System.out.println("# of totla: " + allMethods.size());
			
			StringBuilder result = new StringBuilder();
			
			//for (String method: allMethods) {
				StringBuilder row = new StringBuilder();
				String method = "R5P1Y14.darnley.A:solve:():Ljava.lang.String";
				String methodLabel = method.split("\\.")[0];
				row.append(method + "," + methodLabel + ",");
				
				String knnQuery = "SELECT rt.* FROM result_table2 rt " +
									"INNER JOIN (SELECT sub, target, MAX(similarity) as sim " +
										"FROM result_table2 " +
										"WHERE comp_id=179 and seg_size >=45 and similarity >= 0.82 and (sub = ? or target = ?) " +    
										"GROUP BY sub, target) max_rec " +
										"ON rt.sub = max_rec.sub and rt.target = max_rec.target and rt.similarity = max_rec.sim and rt.seg_size >= 45 " +
									"WHERE comp_id=? ORDER BY similarity desc;";
				PreparedStatement knnStatement = connect.prepareStatement(knnQuery);
				knnStatement.setString(1, method);
				knnStatement.setString(2, method);
				knnStatement.setInt(3, compId);
				
				ResultSet knnResult = knnStatement.executeQuery();
				
				int count = 0;
				System.out.println("Me: " + method);
				double lastSimilarity = 0;
				HashMap<String, List<Neighbor>> neighborRecord = new HashMap<String, List<Neighbor>>();
				HashSet<String> neighborCache = new HashSet<String>();
				while (knnResult.next()) {
					String knnSub = knnResult.getString("sub");
					String knnTarget = knnResult.getString("target");
					double similarity = knnResult.getDouble("similarity");
					
					String neighbor = "";
					String trace = "";
					boolean checkSub = true;
					if (knnSub.equals(method)) {
						neighbor = knnTarget;
						checkSub = false;
					} else {
						neighbor = knnSub;
					}
					
					//Record the best
					if (count == 0)
						row.append(neighbor + "," + similarity + ",");
					
					if (checkSub) {
						String subStart = knnResult.getString("s_start");
						String subCentroid = knnResult.getString("s_centroid");
						String subEnd = knnResult.getString("s_end");
						trace = subStart + "-" + subCentroid + "-" + subEnd;
					} else {
						String targetStart = knnResult.getString("t_start");
						String targetCentroid = knnResult.getString("t_centroid");
						String targetEnd = knnResult.getString("t_end");
						trace = targetStart + "-" + targetCentroid + "-" + targetEnd;
					}
					
					if (neighborCache.contains(trace)) {
						continue ;
					}
					neighborCache.add(trace);
					
					String neighborLabel = neighbor.split("\\.")[0];
					Neighbor newNeighbor = new Neighbor();
					newNeighbor.methodName = neighbor;
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
					
					//Don't break tie;
					if (count >= 5 && similarity < lastSimilarity) {
						break ;
					}
					lastSimilarity = similarity;
				}
				
				System.out.println("Check neighbor count: ");
				List<String> bestLabels = new ArrayList<String>();
				int bestLabelCount = 0;
				String countString = "";
				for (String label: neighborRecord.keySet()) {
					int labelCount = neighborRecord.get(label).size();
					System.out.println("Label: " + label + " " + labelCount);
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
				countString.substring(0, countString.length() - 1);
				row.append(countString + ",");
				
				String bestLabel = "";
				if (bestLabels.size() == 1) {
					bestLabel = bestLabels.get(0);
				} else {
					double bestSum = 0;
					for (String label: bestLabels) {
						double curSum = 0;
						List<Neighbor> neighbors = new ArrayList<Neighbor>();
						for (Neighbor n: neighbors) {
							curSum += n.similarity;
						}
						
						if (curSum > bestSum) {
							bestSum = curSum;
							bestLabel = label;
						}	
					}
				}
				System.out.println("Best label: " + bestLabel);
				row.append(bestLabel + "\n");
				result.append(row);
			//}
			
			BufferedWriter bw = new BufferedWriter(new FileWriter("./knn_result.csv"));
			bw.write(result.toString());
			bw.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
	}
	
	public static class Neighbor {
		String methodName;
		
		String label;
				
		double similarity;
	}

}
