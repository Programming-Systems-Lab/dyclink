package edu.columbia.psl.cc.premain;

import org.apache.log4j.Logger;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

public class MIBTestExecutionListener extends RunListener{
	
	private static Logger logger = Logger.getLogger(MIBTestExecutionListener.class);
	
	public MIBTestExecutionListener() {
		logger.info("Create MIBTestExecutionListener");
		System.out.println("Create MIBTestExecutionListener");
	}
	
	@Override
	public void testRunStarted(Description description) {
		logger.info("Start test class: " + description.getClassName());
		System.out.println("Start test class: " + description.getClassName());
	}
	
	@Override
	public void testRunFinished(Result result) {
		logger.info("Number of test cases: " + result.getRunCount());
		System.out.println("Number of test cases: " + result.getRunCount());
	}
	
	@Override
	public void testStarted(Description description) {
		logger.info("Start test method: " + description.getMethodName());
		System.out.println("Start test method: " + description.getMethodName());
	}
	
	@Override
	public void testFinished(Description description) {
		logger.info("Finisehd test method: " + description.getMethodName());
		System.out.println("Finisehd test method: " + description.getMethodName());
	}
}
