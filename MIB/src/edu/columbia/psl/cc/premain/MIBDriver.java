package edu.columbia.psl.cc.premain;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;

import java.util.TreeMap;
import java.util.HashMap;
import java.util.TreeSet;

import edu.columbia.psl.cc.util.GsonManager;

public class MIBDriver {
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//Clean directory
		GsonManager.cleanDirs();
		
		String className = args[0];
		String[] newArgs = new String[args.length - 1];
		
		for (int i = 1; i < args.length; i++) {
			newArgs[i] = args[i];
		}
		
		try {
			Class targetClass = Class.forName(className);
			System.out.println("Confirm class: " + targetClass);
			
			Method mainMethod = targetClass.getMethod("main", String[].class);
			System.out.println("Capture main method: " + mainMethod);
			mainMethod.invoke(null, (Object)newArgs);
			
			//Put the analysis here temporarily
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
