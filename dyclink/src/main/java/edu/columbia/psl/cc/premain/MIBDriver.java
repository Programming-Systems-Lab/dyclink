package edu.columbia.psl.cc.premain;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.abs.AbstractGraph;
import edu.columbia.psl.cc.analysis.HorizontalMerger;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.crawler.NativePackages;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.CumuGraph;
import edu.columbia.psl.cc.pojo.FieldNode;
import edu.columbia.psl.cc.pojo.FieldRecord;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.NameMap;
import edu.columbia.psl.cc.util.CumuGraphRecorder;
import edu.columbia.psl.cc.util.GlobalGraphRecorder;
import edu.columbia.psl.cc.util.GsonManager;
import edu.columbia.psl.cc.util.ObjectIdAllocater;
import edu.columbia.psl.cc.util.ShutdownLogger;
import edu.columbia.psl.cc.util.StringUtil;
import edu.uci.ics.jung.graph.event.GraphEvent.Type;

public class MIBDriver {
	
	private static Logger logger = LogManager.getLogger(MIBDriver.class);
	
	private static TypeToken<NameMap> nameMapToken = new TypeToken<NameMap>(){};
	
	private static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
	private static TypeToken<MIBConfiguration> configToken = new TypeToken<MIBConfiguration>(){};
	
	private static Class memorizedTargetClass;
		
	public static String extractMainClassName(String jarFile) {
		try {
			JarFile jFile = new JarFile(new File(jarFile));
			String mainClassName = jFile.getManifest().getMainAttributes().getValue("Main-Class");
			return mainClassName;
		} catch (Exception ex) {
			logger.error("Exception: ", ex);
		}
		return null;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//BasicConfigurator.configure();
		
		logger.info("MIB Configuration");
		logger.info(MIBConfiguration.getInstance());
		
		//Set inst pool, cannot set it in static initializer, or there will be infinite loop
		InstPool.DEBUG = MIBConfiguration.getInstance().isDebug();
		setupGlobalRecorder();
				
		//Clean directory
		//GsonManager.cleanDirs(mConfig.isCleanTemplate(), mConfig.isCleanTest());
		
		//Read existing info
		if (MIBConfiguration.getInstance().getThreadMethodIdxRecord().size() > 0) {
			HashMap<Integer, Integer> oldRecord = MIBConfiguration.getInstance().getThreadMethodIdxRecord();
			
			for (Integer key: oldRecord.keySet()) {
				int newIdx = oldRecord.get(key);
				ObjectIdAllocater.setThreadMethodIndex(key, newIdx);
			}
		}
		
		//Add shutdown hook to serialize graphs
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphing();
				ShutdownLogger.finalFlush();
			}
		});
				
		String className = null;
		String[] newArgs;
		if (args[0].equals("-jar")) {
			//Execute a jar, find the main method in jar
			String jarName = args[1];
			className = extractMainClassName(jarName);
			
			if (className == null) {
				logger.error("Cannot find maint class name for jar: " + jarName);
				System.exit(-1);
			}
			
			newArgs = new String[args.length - 2];
			for (int i = 2; i < args.length; i++) {
				newArgs[i - 2] = args[i];
			}
		} else {
			className = args[0];
			newArgs = new String[args.length - 1];
			
			for (int i = 1; i < args.length; i++) {
				newArgs[i - 1 ] = args[i];
			}
		}
		
		try {
			Class targetClass = Class.forName(className);
			memorizedTargetClass = targetClass;
			logger.info("Confirm class: " + targetClass);
			logger.info("Confirm args: " + Arrays.toString(newArgs));
			//logger.info("Class loader: " + targetClass.getClassLoader().getClass().getName() + " " + System.identityHashCode(targetClass.getClassLoader()));
			
			if (MIBConfiguration.getInstance().isCumuGraph()) {
				String fullName = StringUtil.genKey(className, "main", "([Ljava/lang/String;)V");
				String shortName = CumuGraphRecorder.getGlobalName(fullName);
				int threadId = ObjectIdAllocater.getThreadId();
				
				CumuGraphRecorder.DUMP_FULL_NAME = fullName;
				CumuGraphRecorder.DUMP_GLOBAL_NAME = shortName;
				CumuGraphRecorder.DUMP_THREAD_ID = threadId;
			}
			
			Method mainMethod = targetClass.getMethod("main", String[].class);
			mainMethod.setAccessible(true);
			mainMethod.invoke(null, (Object)newArgs);
		} catch (Exception ex) {
			logger.error("Exception: ", ex);
		}
	}
	
	private static void graphing() {		
		//int threadIdInit = MIBConfiguration.getInstance().getThreadInit();
		//String now = String.valueOf((new Date()).getTime());
		//String fileName = memorizedTargetClass.getName() + "-" + threadIdInit + "-" + now;
		String fileName = memorizedTargetClass.getName();
		ShutdownLogger.appendMessage("Graphing: " + fileName);
		
		//Dump name map
		serializeNameMap();
		
		/*if (MIBConfiguration.getInstance().isFieldTrack()) {
			GlobalGraphRecorder.constructGlobalRelations(false);
		}*/
		
		//Dump all graphs in memory
		selectDominantGraphs(fileName);
		
		//Update configuration
		//logger.info("Update configuration");
		updateConfig();
	}
	
	public static void serializeNameMap() {
		NameMap nameMap = new NameMap();
		nameMap.setGlobalNameMap(GlobalGraphRecorder.getGlobalNameMap());
		nameMap.setShortNameCounter(GlobalGraphRecorder.getShortNameCounter());
		nameMap.setRecursiveMethods(GlobalGraphRecorder.getRecursiveMethods());
		nameMap.setUndersizedMethods(GlobalGraphRecorder.getUndersizedMethods());
		nameMap.setUntransformedClass(GlobalGraphRecorder.getUntransformedClass());
		
		try {
			String fileName = "nameMap";
			GsonManager.writeJsonGeneric(nameMap, fileName, nameMapToken, MIBConfiguration.LABEL_MAP_DIR);
		} catch (Exception ex) {
			ShutdownLogger.appendException(ex);
		}
	}
	
	public static void setupGlobalRecorder() {
		String fileName = MIBConfiguration.getInstance().getLabelmapDir() + "/nameMap.json";
		File file = new File(fileName);
				
		if (file.exists()) {
			TypeToken<NameMap> nameToken = new TypeToken<NameMap>(){};
			NameMap nameMap = GsonManager.readJsonGeneric(file, nameToken);
			
			if (nameMap.getGlobalNameMap() != null)
				GlobalGraphRecorder.setGlobalNameMap(nameMap.getGlobalNameMap());
			if (nameMap.getShortNameCounter() != null)
				GlobalGraphRecorder.setShortNameCounter(nameMap.getShortNameCounter());
			if (nameMap.getRecursiveMethods() != null)
				GlobalGraphRecorder.setRecursiveMethods(nameMap.getRecursiveMethods());
			if (nameMap.getUndersizedMethods() != null)
				GlobalGraphRecorder.setUndersizedMethods(nameMap.getUndersizedMethods());
			if (nameMap.getUntransformedClass() != null)
				GlobalGraphRecorder.setUntransformedClass(nameMap.getUntransformedClass());
			//System.out.println("Show name map from last execution: " + GlobalRecorder.getGlobalNameMap().size());
		}
		
		setupNativePackages();
	}
	
	public static void setupNativePackages() {
		String npFileName = MIBConfiguration.getInstance().getLabelmapDir() + "/nativePackages.json";
		File npFile = new File(npFileName);
		if (npFile.exists()) {
			TypeToken<NativePackages> npToken = new TypeToken<NativePackages>(){};
			NativePackages nativePackages = GsonManager.readJsonGeneric(npFile, npToken);
			HashMap<String, Integer> nativeMap = nativePackages.getNativePackages();
			
			if (nativeMap != null) {
				GlobalGraphRecorder.setNativePackages(nativeMap);
			}
		}
	}
	
	public synchronized static void updateConfig() {
		ConcurrentHashMap<Integer, AtomicInteger> threadMethodIdxRecord = ObjectIdAllocater.getThreadMethodIdxRecord();
		HashMap<Integer, Integer> toSerialize = new HashMap<Integer, Integer>();
		for (Integer i: threadMethodIdxRecord.keySet()) {
			//Get thread method id is a ++, so no need to increment here
			int newIdx = threadMethodIdxRecord.get(i).get() ;
			toSerialize.put(i, newIdx);
		}
		
		if (toSerialize.size() > 0) {
			try {
				MIBConfiguration config = MIBConfiguration.getInstance();
				config.setThreadMethodIdxRecord(toSerialize);
				
				String fileName = "./config/mib_config.json";
				GsonManager.writeJsonGeneric(config, fileName, configToken, -1);
			} catch (Exception ex) {
				ShutdownLogger.appendException(ex);
			}
		}
	}
	
	public synchronized static void selectDominantGraphs(String appName) {
		//Dump all graphs in memory
		if (!MIBConfiguration.getInstance().isCumuGraph()) {
			HashMap<String, HashMap<String, GraphTemplate>> allGraphs = GlobalGraphRecorder.getGraphs();
			HorizontalMerger.startExtractionFast(appName, allGraphs);
		} else {
			List<AbstractGraph> allGraphs = CumuGraphRecorder.getCumuGraphs();
			TypeToken<CumuGraph> graphToken = new TypeToken<CumuGraph>(){};
			HorizontalMerger.startExtractionFast(appName, allGraphs, graphToken);
		}
		
	}
}
