package edu.columbia.psl.cc.premain;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.RollingFileAppender;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.NameMap;
import edu.columbia.psl.cc.util.GlobalRecorder;
import edu.columbia.psl.cc.util.GsonManager;
import edu.columbia.psl.cc.util.ObjectIdAllocater;
import edu.columbia.psl.cc.util.TimeController;

public class MIBTestExecutionListener extends RunListener{
	
	private static Logger logger = Logger.getLogger(MIBTestExecutionListener.class);
	
	private static long mb = 1024 * 1024;
	
	private List<String> overTimeTestCases = new ArrayList<String>();
	
	static {
		System.out.println("Initialize logger");
		//BasicConfigurator.configure();
		PropertyConfigurator.configure("./target/log4j.properties");
	}
	
	public MIBTestExecutionListener() {
		//logger.info("Create MIBTestExecutionListener");
	}
	
	@Override
	public void testRunStarted(Description description) {
		logger.info("Start testing");
		
		MIBConfiguration mConfig = MIBConfiguration.getInstance();
		logger.info("MIB Configuration");
		logger.info(mConfig);
		
		//Set inst pool, cannot set it in static initializer, or there will be infinite loop
		InstPool.DEBUG = mConfig.isDebug();
		
		MIBDriver.setupGlobalRecorder();
		
		//Clean directory
		//GsonManager.cleanDirs(mConfig.isCleanTemplate(), mConfig.isCleanTest());
		
		HashMap<Integer, Integer> threadMethodIdxRecord = mConfig.getThreadMethodIdxRecord();
		if (threadMethodIdxRecord != null && threadMethodIdxRecord.size() > 0) {
			HashMap<Integer, Integer> oldRecord = mConfig.getThreadMethodIdxRecord();
			
			for (Integer key: oldRecord.keySet()) {
				int newIdx = oldRecord.get(key) + 1;
				ObjectIdAllocater.setThreadMethodIndex(key, newIdx);
			}
		}
		
		TimeController.initTestMethodBaseTime();
		System.out.println("Max memory: " + (Runtime.getRuntime().maxMemory()/mb));
	}
	
	@Override
	public void testRunFinished(Result result) {
		logger.info("Number of test cases: " + result.getRunCount());
		System.out.println("Overtime test cases");
		for (String t: this.overTimeTestCases) {
			System.out.println(t);
		}
		MIBDriver.updateConfig();
		MIBDriver.serializeNameMap();
	}
	
	@Override
	public void testStarted(Description description) {
		logger.info("Start test method: " + description);
		System.out.println("Start test method: " + description);
		
		//Set up main method id
		int mainThreadId = ObjectIdAllocater.getThreadId();
		ObjectIdAllocater.setMainThreadId(mainThreadId);
		logger.info("Main thread id: " + mainThreadId);
		
		TimeController.initTestMethodBaseTime();
	}
	
	@Override
	public void testFinished(Description description) {
		logger.info("Finisehd test method: " + description);
		
		try {
			if (TimeController.isOverTime()) {
				System.out.println("Overtime: " + description);
				this.overTimeTestCases.add(description.toString());
			}
			
			System.out.println("Start graph extraction: " + description);
			//MIBDriver.selectDominantGraphsWithTestMethodName(description.getClassName() + "-" + description.getMethodName(), false);
			
			if (MIBConfiguration.getInstance().isFieldTrack()) {
				//Construct relations between w and r fields
				//logger.info("Construct global edges");
				int gEdgeNum = GlobalRecorder.constructGlobalRelations(false);
				logger.info("Global edges: " + gEdgeNum);
			}
			
			MIBDriver.selectDominantGraphs(false);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GlobalRecorder.clearContext();
			ObjectIdAllocater.clearMainThread();
			long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			System.out.println("Used memory: " + ((double)usedMemory/mb));
			System.out.println("Execution time: " + ((double)TimeController.testMethodExecutionTime())/1000);
			System.out.println();
		}
	}
}
