package edu.columbia.psl.cc.premain;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;

import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.jar.JarFile;

import org.apache.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.analysis.HorizontalMerger;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.NameMap;
import edu.columbia.psl.cc.pojo.StaticMethodMiner;
import edu.columbia.psl.cc.util.Analyzer;
import edu.columbia.psl.cc.util.DynamicGraphAnalyzer;
import edu.columbia.psl.cc.util.GlobalRecorder;
import edu.columbia.psl.cc.util.GsonManager;
import edu.columbia.psl.cc.util.StaticBytecodeCatAnalyzer;
import edu.columbia.psl.cc.util.TemplateLoader;

public class MIBDriver {
	
	private static Logger logger = Logger.getLogger(MIBDriver.class);
	
	private static TypeToken<NameMap> nameMapToken = new TypeToken<NameMap>(){};
	
	private static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
	public static String extractMainClassName(String jarFile) {
		try {
			JarFile jFile = new JarFile(new File(jarFile));
			String mainClassName = jFile.getManifest().getMainAttributes().getValue("Main-Class");
			return mainClassName;
		} catch (Exception ex) {
			logger.error(ex);
		}
		return null;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MIBConfiguration mConfig = MIBConfiguration.getInstance();
		logger.info("MIB Configuration");
		logger.info(MIBConfiguration.getInstance());
		
		//Clean directory
		GsonManager.cleanDirs(mConfig.isCleanTemplate(), mConfig.isCleanTest());
		
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
			logger.info("Confirm class: " + targetClass);
			
			Method mainMethod = targetClass.getMethod("main", String[].class);
			mainMethod.invoke(null, (Object)newArgs);
			
			//Dump name map
			logger.info("Dump nameMap: " + targetClass);
			NameMap nameMap = new NameMap();
			nameMap.setGlobalNameMap(GlobalRecorder.getGlobalNameMap());
			nameMap.setRecursiveMethods(GlobalRecorder.getRecursiveMethods());
			GsonManager.writeJsonGeneric(nameMap, "nameMap", nameMapToken, 2);
			
			//Dump all graphs in memory
			logger.info("Dump all graphs: " + targetClass);
			HashMap<String, List<GraphTemplate>> allGraphs = GlobalRecorder.getGraphs();
			for (String shortKey: allGraphs.keySet()) {
				GsonManager.cacheDirectGraphs(shortKey, allGraphs.get(shortKey));
			}
			
			logger.info("Select dominant graphs: " + targetClass);
			HorizontalMerger.startExtraction();
			
			if (mConfig.isOverallAnalysis()) {
				AnalysisService.invokeFinalAnalysis(mConfig);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
