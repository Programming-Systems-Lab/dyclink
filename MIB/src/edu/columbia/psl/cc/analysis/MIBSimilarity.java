package edu.columbia.psl.cc.analysis;

import java.util.TreeMap;
import java.util.TreeSet;

import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.InstNode;

public interface MIBSimilarity<T> {
	
	public double calculateSimilarity(T metric1, T metric2);
	
	public T constructCostTable(String methodName, InstPool depMap);

}
