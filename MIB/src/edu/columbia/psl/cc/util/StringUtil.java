package edu.columbia.psl.cc.util;

import java.util.HashMap;

public class StringUtil {
	
	private static String pattern = ")";
	
	public static String genRelation(String s1, String s2) {
		return s1 + "->" + s2;
	}
	
	public static String cleanPuncHelper(String oriString, String newVal) {
		String ret = oriString.replaceAll("\\W", newVal);
		return ret;
	}
	
	public static String cleanPunc(String oriString, Object...newVal) {
		if (newVal.length == 0) {
			return cleanPuncHelper(oriString, "");
		} else {
			String replaceBy = (String)newVal[0];
			return cleanPuncHelper(oriString, replaceBy);
		}
	}
	
	public static String parseDesc(String desc) {
		int idx = desc.lastIndexOf(pattern);
		
		if (idx == -1) {
			System.err.println("Incorrect desc: " + desc);
			return null;
		}
		String in = cleanPunc(desc.substring(0, idx + 1));
		String out = cleanPunc(desc.substring(idx + 1, desc.length()));
		
		return in + "~" + out;
	}
	
	public static String genKey(String className, String methodName, String methodDesc) {
		String key = StringUtil.cleanPunc(className, "_") 
				+ "~" + StringUtil.cleanPunc(methodName, "_") 
				+ "~" + StringUtil.parseDesc(methodDesc);
		return key;
	}
	
	public static String parseElement(String inst, int idx) {
		String[] instElements = inst.split(" ");
		String label = instElements[idx];
		return label;
	}
	
	public static String replaceLabel(String oriInst, HashMap<String, Integer> labelMap) {
		String opLabel = StringUtil.parseElement(oriInst, 0);
		String npLabel = "L" + labelMap.get(opLabel);
		String newInst = oriInst.replace(opLabel, npLabel);
		return newInst;
	}

}
