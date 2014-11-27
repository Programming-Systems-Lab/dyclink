package edu.columbia.psl.cc.premain;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;

import java.util.TreeMap;
import java.util.HashMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.google.gson.reflect.TypeToken;

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
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MIBConfiguration mConfig = MIBConfiguration.getInstance();
		logger.info("MIB Configuration");
		logger.info(MIBConfiguration.getInstance());
		
		//Clean directory
		GsonManager.cleanDirs(mConfig.isCleanTemplate(), mConfig.isCleanTest());
		
		String className = args[0];
		String[] newArgs = new String[args.length - 1];
		
		for (int i = 1; i < args.length; i++) {
			newArgs[i - 1] = args[i];
		}
		
		try {
			Class targetClass = Class.forName(className);
			logger.info("Confirm class: " + targetClass);
			
			Method mainMethod = targetClass.getMethod("main", String[].class);
			mainMethod.invoke(null, (Object)newArgs);
			
			//Dump name map
			NameMap nameMap = new NameMap();
			nameMap.setGlobalNameMap(GlobalRecorder.getGlobalNameMap());
			nameMap.setRecursiveMethods(GlobalRecorder.getRecursiveMethods());
			GsonManager.writeJsonGeneric(nameMap, "nameMap", nameMapToken, 2);
			
			if (mConfig.isOverallAnalysis()) {
				AnalysisService.invokeFinalAnalysis(mConfig);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
