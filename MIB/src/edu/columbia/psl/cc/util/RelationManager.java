package edu.columbia.psl.cc.util;

public class RelationManager {
	
	private static String rwRelation = "read-write";
	
	private static String controlWrite = "control-write";
	
	private static String controlRead = "control-read";
	
	public static String getRWRelation() {
		return rwRelation;
	}
	
	public static String getControlWrite() {
		return controlWrite;
	}
	
	public static String getControlRead() {
		return controlRead;
	}

}
