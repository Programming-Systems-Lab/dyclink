package edu.columbia.psl.cc.premain;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.RollingFileAppender;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.util.GsonManager;

public class MIBTestExecutionListener extends RunListener{
	
	private static Logger logger = Logger.getLogger(MIBTestExecutionListener.class);
	
	static {
		System.out.println("Initialize logger");
		//BasicConfigurator.configure();
		PropertyConfigurator.configure("./target/log4j.properties");
	}
	
	public MIBTestExecutionListener() {
		logger.info("Create MIBTestExecutionListener");
	}
	
	@Override
	public void testRunStarted(Description description) {
		logger.info("Start test class: " + description);
		
		MIBConfiguration mConfig = MIBConfiguration.getInstance();
		logger.info("MIB Configuration");
		logger.info(MIBConfiguration.getInstance());
		
		//Set inst pool, cannot set it in static initializer, or there will be infinite loop
		InstPool.DEBUG = MIBConfiguration.getInstance().isDebug();
		
		//Clean directory
		GsonManager.cleanDirs(mConfig.isCleanTemplate(), mConfig.isCleanTest());
	}
	
	@Override
	public void testRunFinished(Result result) {
		logger.info("Number of test cases: " + result.getRunCount());
		MIBDriver.serializeNameMap();
		MIBDriver.selectDominantGraphs();
		logger.info("Complete summarizing graphs: ");
	}
	
	@Override
	public void testStarted(Description description) {
		logger.info("Start test method: " + description);
	}
	
	@Override
	public void testFinished(Description description) {
		logger.info("Finisehd test method: " + description);
	}
}
