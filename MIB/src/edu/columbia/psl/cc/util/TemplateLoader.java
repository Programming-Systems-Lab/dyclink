package edu.columbia.psl.cc.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;

public class TemplateLoader {
	
	private static Logger logger = Logger.getLogger(TemplateLoader.class);
	
	private static String skipMethod = "main:([Ljava.lang.String)";
	
	private static FilenameFilter nameFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			return name.toLowerCase().endsWith(".json") && !name.contains(skipMethod);
		}
	};
	
	public static boolean probeDir(String dirName) {
		File dir = new File(dirName);
		if (!dir.isDirectory())
			return false;
		
		if (dir.listFiles(nameFilter).length > 0)
			return true;
		else
			return false;
	}
	
	public static HashSet<String> loadAllFileNames(File dir) {
		HashSet<String> ret = new HashSet<String>();
		if (!dir.isDirectory()) {
			ret.add(dir.getName());
		} else {
			for (File f: dir.listFiles(nameFilter)) {
				String name = f.getName().replace(".json", "");
				ret.add(name);
			}
		}
		return ret;
	}

	public static <T> HashMap<String, T> loadTemplate(File dir, TypeToken<T> typeToken) {
		HashMap<String, T> ret = new HashMap<String, T>();
		if (!dir.isDirectory()) {
			T temp = GsonManager.readJsonGeneric(dir, typeToken);
			ret.put(dir.getName(), temp);
		} else {			
			for (File f: dir.listFiles(nameFilter)) {
				String name = f.getName().replace(".json", "");
				T value = GsonManager.readJsonGeneric(f, typeToken);
				ret.put(name, value);
			}
		}
		return ret;
	}
	
	/**
	 * Cache usually contains large number of files. Parallalize the file loading
	 * @param dir
	 * @param typeToken
	 * @return
	 */
	public static <T> HashMap<String, HashSet<T>> loadCacheTemplates(File dir, 
			final TypeToken<T> typeToken, 
			HashSet<String> recursiveMethods) {
		HashMap<String, HashSet<T>> ret = new HashMap<String, HashSet<T>>();
		if (!dir.isDirectory()) {
			//Remove uuid
			String name = StringUtil.removeUUID(dir.getName());
			if (recursiveMethods.contains(name))
				return ret;
			
			T temp = GsonManager.readJsonGeneric(dir, typeToken);
			HashSet<T> retSet = new HashSet<T>();
			retSet.add(temp);
			
			ret.put(name, retSet);
			return ret;
		} else {
			ExecutorService executor = Executors.newFixedThreadPool(MIBConfiguration.getInstance().getParallelFactor());
			HashMap<String, HashSet<Future<T>>> futureMap = new HashMap<String, HashSet<Future<T>>>();
			for (final File f: dir.listFiles(nameFilter)) {
				String name = StringUtil.removeUUID(f.getName());
				
				if (recursiveMethods.contains(name))
					continue ;
				
				Future<T> worker = executor.submit(new Callable<T>() {

					@Override
					public T call() throws Exception {
						T value = GsonManager.readJsonGeneric(f, typeToken);
						return value;
					}
					
				});
				
				if (futureMap.containsKey(name)) {
					futureMap.get(name).add(worker);
				} else {
					HashSet<Future<T>> futureSet = new HashSet<Future<T>>();
					futureSet.add(worker);
					futureMap.put(name, futureSet);
				}
			}
			
			executor.shutdown();
			while (!executor.isTerminated());
			
			for (String name: futureMap.keySet()) {
				HashSet<Future<T>> futureSet = futureMap.get(name);
				
				for (Future<T> worker: futureSet) {
					try {
						if (ret.containsKey(name)) {
							ret.get(name).add(worker.get());
						} else {
							HashSet<T> retSet = new HashSet<T>();
							retSet.add(worker.get());
							ret.put(name, retSet);
						}
					} catch (Exception ex) {
						logger.error("Exception: ", ex);
					}
				}
			}
			
			return ret;
		}
	}
	
	public static <T> T loadTemplateFile(File tempF, TypeToken<T> typeToken) {
		T value = GsonManager.readJsonGeneric(tempF, typeToken);
		return value;
	}
	
	public static <T> T loadTemplateFile(String fileName, TypeToken<T> typeToken) {
		File f = new File(fileName);
		if (f.exists()) {
			return loadTemplateFile(f, typeToken);
		} else {
			logger.warn("File not exist: " + f.getName());
			return null;
		}
	}

}
