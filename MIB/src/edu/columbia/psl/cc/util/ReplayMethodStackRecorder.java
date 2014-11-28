package edu.columbia.psl.cc.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import edu.columbia.psl.cc.pojo.InstNode;

public class ReplayMethodStackRecorder extends MethodStackRecorder{
	
	private static Logger logger = Logger.getLogger(ReplayMethodStackRecorder.class);
	
	private List<InstNode> needIdChanged = new ArrayList<InstNode>();
	
	public ReplayMethodStackRecorder(String className, 
			String methodName, 
			String methodDesc) {
		super(className, methodName, methodDesc, -5);
	}
	
	public void replaceId(int id) {
		for (InstNode change: needIdChanged) {
			String addInfo = change.getAddInfo();
			String[] addArray = addInfo.split(":");
			addInfo = addArray[0] + ":" + id;
			change.setAddInfo(addInfo);
		}
	}
}
