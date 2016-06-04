package edu.columbia.psl.cc.util;

import java.util.HashMap;

import edu.columbia.psl.cc.pojo.StaticMethodMiner;

public interface Analyzer<T> {
	
	public void analyzeTemplate();
	
	public void setTemplates(HashMap<String, T> templates);
	
	public void setAnnotGuard(boolean annotGuard);
	
	public HashMap<String, T> getTemplates();
	
	public void setTests(HashMap<String, T> templates);
	
	public HashMap<String, T> getTests();
}

