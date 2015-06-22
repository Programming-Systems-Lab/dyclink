package edu.columbia.psl.cc.crawler;

import java.util.HashMap;

public class NativePackages {
	
	//Key: api, Val: id
	private HashMap<String, Integer> nativePackages = new HashMap<String, Integer>();
	
	public void addNativePackage(String nativePackage, int id) {
		if (!this.nativePackages.containsKey(nativePackage)) {
			this.nativePackages.put(nativePackage, id);
		}
	}
		
	public HashMap<String, Integer> getNativePackages() {
		return this.nativePackages;
	}
	
	public void setNativePackages(HashMap<String, Integer> nativePackages) {
		this.nativePackages = nativePackages;
	}
	

}
