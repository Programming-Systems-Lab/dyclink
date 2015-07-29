package edu.columbia.psl.cc.util;

import java.util.HashMap;

import edu.columbia.psl.cc.pojo.FieldRecord;
import edu.columbia.psl.cc.pojo.InstNode;


public class FieldRecorder {
	
	private final static String connect = ":";
		
	private HashMap<String, FieldRecord> fieldRecordHistory = new HashMap<String, FieldRecord>();
		
	public static String toIndex(InstNode writeInst, InstNode readInst) {
		String ret = toIndex(writeInst) + connect + toIndex(readInst);
		return ret;
	}
	
	public static String toIndex(String writeKey, String readKey) {
		String ret = writeKey + connect + readKey;
		return ret;
	}
	
	public static String toIndex(InstNode inst) {
		return StringUtil.genIdxKey(inst.getThreadId(), inst.getThreadMethodIdx(), inst.getIdx());
	}
	
	public void registerHistory(InstNode writeInst, InstNode readInst) {
		String wrIndex = toIndex(writeInst, readInst);
		
		//System.out.println("Register: " + wrIndex);
		FieldRecord fr = null;
		if (this.fieldRecordHistory.containsKey(wrIndex)) {
			fr = this.fieldRecordHistory.get(wrIndex);
			fr.increFreq();
		} else {
			fr = new FieldRecord();
			fr.setWriteInst(writeInst);
			fr.setReadInst(readInst);
			fr.increFreq();
			this.fieldRecordHistory.put(wrIndex, fr);
		}
	}
	
	public void increHistoryFreq(String writeKey, String readKey) {
		String wrIndex = toIndex(writeKey, readKey);
		//System.out.println("Incre: " + wrIndex);
		this.fieldRecordHistory.get(wrIndex).increFreq();
	}
	
	public void removeHistory(String writeKey, String readKey) {
		String wrIndex = toIndex(writeKey, readKey);
		//System.out.println("Remove: " + wrIndex);
		this.fieldRecordHistory.remove(wrIndex);
	}
	
	public void repalceInst(InstNode original, InstNode replacer) {
		String oIndex = toIndex(original);
		String pIndex = toIndex(replacer);
		
		this.fieldRecordHistory.remove(oIndex);
		
		FieldRecord replaceFr = this.fieldRecordHistory.get(pIndex);
		replaceFr.increFreq();
	}
	
	public void increWriteDirectly(String rwKey) {
		this.fieldRecordHistory.get(rwKey).increFreq();
	}
	
	public HashMap<String, FieldRecord> getHistory() {
		return this.fieldRecordHistory;
	}
	
	public void cleanRecorder() {
		this.fieldRecordHistory.clear();
	}
}
