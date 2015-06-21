package edu.columbia.psl.cc.crawler;

import java.util.List;
import java.util.ArrayList;

public class NativePackages {
	
	private List<String> nativePackages = new ArrayList<String>();
	
	public void addNativePackage(String nativePackage) {
		this.nativePackages.add(nativePackage);
	}
	
	public List<String> getNativePackages() {
		return this.nativePackages;
	}
	
	public void setNativePackages(List<String> nativePackages) {
		this.nativePackages = nativePackages;
	}
	

}
