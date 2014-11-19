package edu.columbia.psl.cc.util;

public class ClassLoadingRecorder {
	
	private static String latestLoadedClass;
	
	public static synchronized void setLatestLoadedClass(String clazz) {
		latestLoadedClass = clazz;
	}
	
	public static synchronized String getLatestLoadedClass() {
		String ret = latestLoadedClass;
		latestLoadedClass = null;
		return ret;
	}
}
