package edu.columbia.psl.cc.premain;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;

import java.util.TreeMap;
import java.util.HashMap;
import java.util.TreeSet;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.StaticRep;
import edu.columbia.psl.cc.util.Analyzer;
import edu.columbia.psl.cc.util.DynamicGraphAnalyzer;
import edu.columbia.psl.cc.util.GsonManager;
import edu.columbia.psl.cc.util.StaticBytecodeCatAnalyzer;
import edu.columbia.psl.cc.util.TemplateLoader;

public class MIBDriver {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		MIBConfiguration mConfig = MIBConfiguration.getInstance();
		System.out.println("MIB Configuration");
		System.out.println(MIBConfiguration.getInstance());
		
		//Clean directory
		GsonManager.cleanDirs(mConfig.isCleanTemplate(), mConfig.isCleanTest());
		
		String className = args[0];
		String[] newArgs = new String[args.length - 1];
		
		for (int i = 1; i < args.length; i++) {
			newArgs[i - 1] = args[i];
		}
		
		System.out.println("Prepare class");
		try {
			Class targetClass = Class.forName(className);
			System.out.println("Confirm class: " + targetClass);
			
			Method mainMethod = targetClass.getMethod("main", String[].class);
			mainMethod.invoke(null, (Object)newArgs);
			
			if (mConfig.isOverallAnalysis()) {
				AnalysisService.invokeFinalAnalysis(mConfig);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
