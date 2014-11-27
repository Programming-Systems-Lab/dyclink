package edu.columbia.psl.cc.pojo;

import java.util.HashMap;
import java.util.HashSet;

public class NameMap {
	
	private HashMap<String, String> globalNameMap;
	
	private HashSet<String> recursiveMethods;
	
	public void setGlobalNameMap(HashMap<String, String> globalNameMap) {
		this.globalNameMap = globalNameMap;
	}
	
	public HashMap<String, String> getGlobalNameMap() {
		return globalNameMap;
	}
	
	public void setRecursiveMethods(HashSet<String> recursiveMethods) {
		this.recursiveMethods = recursiveMethods;
	}
	
	public HashSet<String> getRecursiveMethods() {
		return this.recursiveMethods;
	}

}
