package edu.columbia.psl.cc.util;

public class StringUtil {
	
	private static String pattern = ")";
	
	public static String genRelation(String s1, String s2) {
		return s1 + "->" + s2;
	}
	
	public static String cleanPunc(String oriString) {
		String ret = oriString.replaceAll("\\W", "_");
		return ret;
	}
	
	public static String parseDesc(String desc) {
		int idx = desc.lastIndexOf(pattern);
		
		if (idx == -1) {
			System.err.println("Incorrect desc: " + desc);
			return null;
		}
		String in = cleanPunc(desc.substring(0, idx + 1));
		String out = cleanPunc(desc.substring(idx + 1, desc.length()));
		
		return in + ":" + out;
	}

}
