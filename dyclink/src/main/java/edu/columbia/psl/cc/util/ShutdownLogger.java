package edu.columbia.psl.cc.util;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class ShutdownLogger {
	
	private static final String msgPrefix = "ShutdownLogger-";
	
	private static int BUF_SIZE = 5000;
	
	//private static int FILE_SIZE = 500 * (int)Math.pow(10, 6); 
	
	private static StringBuilder buf = new StringBuilder();
	
	private static final String logPrefix = "log/shutdown";
	
	private static File shutdownLog;
	
	private static Object shutdownLogLock = new Object();
	
	static {
		synchronized(shutdownLogLock) {
			shutdownLog = new File(logPrefix + ".log");
			try {
				if (!shutdownLog.exists()) {
					shutdownLog.createNewFile();
					shutdownLog.setExecutable(true, false);
					shutdownLog.setReadable(true, false);
					shutdownLog.setWritable(true, false);
					//shutdownLog = new File(logPrefix + threadId + ".log");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
	}
	
	public static void appendException(Exception toRecord) {
		//int threadId = GlobalInfoRecorder.getThreadIndex();
		//System.out.println(msgPrefix + threadId + ":");
		//toRecord.printStackTrace();
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		toRecord.printStackTrace(pw);
		
		appendMessage(sw.toString());
	}
	
	private static void flushBuf(boolean forced) {
		if (buf.length() > BUF_SIZE || forced) {
			synchronized(shutdownLogLock) {
				try {
					Files.write(shutdownLog.toPath(), buf.toString().getBytes(), StandardOpenOption.APPEND);
					buf = new StringBuilder();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	public static void appendMessage(String msg) {		
		System.out.println(msgPrefix + ": " + msg);
		
		buf.append(msg + "\n");
		flushBuf(false);
	}
	
	public static void finalFlush() {
		flushBuf(true);
	}
	
}
