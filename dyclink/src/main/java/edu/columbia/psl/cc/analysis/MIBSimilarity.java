package edu.columbia.psl.cc.analysis;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;

public interface MIBSimilarity<T> {
	
	public String getResult();
	
	public void calculateSimilarities(HashMap<String, GraphTemplate> gMap1, HashMap<String, GraphTemplate> gMap2);
	
	public double calculateSimilarity(T templateCostTable, T testCostTable);
	
	public T constructCostTable(String methodName, InstPool depMap);

}
