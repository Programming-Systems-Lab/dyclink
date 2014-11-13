package edu.columbia.psl.cc.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;

import com.google.gson.reflect.TypeToken;

public class TemplateLoader {
	
	private static String skipMethod = "main:([Ljava.lang.String)";
	
	public static boolean probeDir(String dirName) {
		File dir = new File(dirName);
		if (!dir.isDirectory())
			return false;
		
		FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".json") && !name.contains(skipMethod);
			}
		};
		
		if (dir.listFiles(filter).length > 0)
			return true;
		else
			return false;
	}

	public static <T> HashMap<String, T> loadTemplate(File dir, TypeToken<T> typeToken) {
		HashMap<String, T> ret = new HashMap<String, T>();
		if (!dir.isDirectory()) {
			T temp = GsonManager.readJsonGeneric(dir, typeToken);
			ret.put(dir.getName(), temp);
		} else {
			FilenameFilter filter = new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.toLowerCase().endsWith(".json") && !name.contains(skipMethod);
				}
			};
			
			for (File f: dir.listFiles(filter)) {
				String name = f.getName().replace(".json", "");
				T value = GsonManager.readJsonGeneric(f, typeToken);
				ret.put(name, value);
			}
		}
		return ret;
	}
	
	public static <T> T loadTemplateFile(File tempF, TypeToken<T> typeToken) {
		T value = GsonManager.readJsonGeneric(tempF, typeToken);
		return value;
	}
	
	public static <T> T loadTemplateFile(String fileName, TypeToken<T> typeToken) {
		File f = new File(fileName);
		if (f.exists()) {
			System.out.println("Start loading graph: " + fileName);
			return loadTemplateFile(f, typeToken);
		} else {
			System.out.println("File not exist: " + f.getName());
			return null;
		}
	}

}
