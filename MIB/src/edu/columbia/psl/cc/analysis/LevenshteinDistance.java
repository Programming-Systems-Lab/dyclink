package edu.columbia.psl.cc.analysis;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.CostObj;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;

public class LevenshteinDistance implements MIBSimilarity<String>{
	
	private HashMap<String, String> templateMap = new HashMap<String, String>();
	
	private HashMap<String, String> targetMap = new HashMap<String, String>();
	
	public void addData(String name, String sequence, boolean isTemplate) {
		if (isTemplate) {
			templateMap.put(name, sequence);
		} else {
			targetMap.put(name, sequence);
		}
	}
	
	@Override
	public String getResult() {
		return null;
	}
	
	public void generateResult() {
		System.out.println("Comparison result: ");
		for (String targetKey: targetMap.keySet()) {
			String targetSeq = targetMap.get(targetKey);
			for (String tempKey: templateMap.keySet()) {
				String tempSeq = templateMap.get(tempKey);
				System.out.println("Target seq: " + targetSeq);
				System.out.println("Test seq: " + tempSeq);
				System.out.println(targetKey + " vs " + tempKey + " " + this.calculateSimilarity(targetSeq, tempSeq));
			}
		}
	}
	
	public static int calculateSimilarity(int[] a0, int[] a1) {
		int len0 = a0.length + 1;
		int len1 = a1.length + 1;
		                                                     
	    int[] cost = new int[len0];                                                     
	    int[] newcost = new int[len0];                                                  
	                              
	    for (int i = 0; i < len0; i++) cost[i] = i;                                     
	                                  
	    for (int j = 1; j < len1; j++) {                                                
	        // initial cost of skipping prefix in String s1                             
	        newcost[0] = j;                                                             
	 
	        // transformation cost for each letter in s0                                
	        for(int i = 1; i < len0; i++) {                                             
	            int match = (a0[i - 1] == a1[j - 1]) ? 0: 1;
	 
	            // computing cost for each transformation                               
	            int cost_replace = cost[i - 1] + match;                                 
	            int cost_insert  = cost[i] + 1;                                         
	            int cost_delete  = newcost[i - 1] + 1;                                  
	 
	            // keep minimum cost                                                    
	            newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
	        }                                                                           
	 
	        // swap cost/newcost arrays                                                 
	        int[] swap = cost; cost = newcost; newcost = swap;                          
	    }                                                                               
	 
	    // the distance is the cost for transforming all letters in both strings        
	    return cost[len0 - 1];     
	}
	
	@Override
	public double calculateSimilarity(String s0, String s1) {                          
	    int len0 = s0.length() + 1;                                                     
	    int len1 = s1.length() + 1;                                                     
	 
	    // the array of distances                                                       
	    int[] cost = new int[len0];                                                     
	    int[] newcost = new int[len0];                                                  
	 
	    // initial cost of skipping prefix in String s0                                 
	    for (int i = 0; i < len0; i++) cost[i] = i;                                     
	 
	    // dynamicaly computing the array of distances                                  
	 
	    // transformation cost for each letter in s1                                    
	    for (int j = 1; j < len1; j++) {                                                
	        // initial cost of skipping prefix in String s1                             
	        newcost[0] = j;                                                             
	 
	        // transformation cost for each letter in s0                                
	        for(int i = 1; i < len0; i++) {                                             
	            // matching current letters in both strings                             
	            int match = (s0.charAt(i - 1) == s1.charAt(j - 1)) ? 0 : 1;             
	 
	            // computing cost for each transformation                               
	            int cost_replace = cost[i - 1] + match;                                 
	            int cost_insert  = cost[i] + 1;                                         
	            int cost_delete  = newcost[i - 1] + 1;                                  
	 
	            // keep minimum cost                                                    
	            newcost[i] = Math.min(Math.min(cost_insert, cost_delete), cost_replace);
	        }                                                                           
	 
	        // swap cost/newcost arrays                                                 
	        int[] swap = cost; cost = newcost; newcost = swap;                          
	    }                                                                               
	 
	    // the distance is the cost for transforming all letters in both strings        
	    return cost[len0 - 1];                                                          
	}
	
	public static void main(String[] args) {
		String s1 = "bagcbbgcbbgcbp";
		String s2 = "bbgcbagcbbgcbp";
		
		LevenshteinDistance ld = new LevenshteinDistance();
		System.out.println("Test: " + ld.calculateSimilarity(s1, s2));
		System.out.println("Test: "+ org.apache.commons.lang3.StringUtils.getLevenshteinDistance(s1, s2));
		
		//int[] a0 = {(int)'b', (int)'a', (int)'g', (int)'c', (int)'b', (int)'b', (int)'g', (int)'c', (int)'b', (int)'b', (int)'g', (int)'c', (int)'b', (int)'p'};
		//int[] a1 = {(int)'b', (int)'b', (int)'g', (int)'c', (int)'b', (int)'a', (int)'g', (int)'c', (int)'b', (int)'b', (int)'g', (int)'c', (int)'b', (int)'p'};
		int[] a0 = {2, 5, 4, 8, 9, 1, 2};
		int[] a1 = {2, 4, 5, 9, 8, 1, 2};
		System.out.println("Test array: " + calculateSimilarity(a0, a1));
	}

	@Override
	public String constructCostTable(String methodName,
			InstPool pool) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void calculateSimilarities(HashMap<String, GraphTemplate> gMap1,
			HashMap<String, GraphTemplate> gMap2) {
		// TODO Auto-generated method stub
		
	}
}
