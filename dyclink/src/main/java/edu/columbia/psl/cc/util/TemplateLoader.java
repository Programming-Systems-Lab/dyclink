package edu.columbia.psl.cc.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.GraphTemplate;

public class TemplateLoader {
	
	private static Logger logger = LogManager.getLogger(TemplateLoader.class);
	
	private static String skipMethod = "main:([Ljava.lang.String)";
	
	public static boolean IGINIT = true;
	
	private static FilenameFilter nameFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			//return name.toLowerCase().endsWith(".json") && !name.contains(skipMethod);
			return name.toLowerCase().endsWith(".json");
		}
	};
	
	public static boolean probeDir(File dir) {

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
	
	public static <T> HashMap<String, T> unzipDir(File dir, TypeToken<T> typeToken) {
		HashMap<String, T> ret = new HashMap<String, T>();
		if (!dir.isDirectory()) {
			logger.warn("Non-directory: " + dir.getAbsolutePath());
			return ret;
		}
		
		for (File zip: dir.listFiles()) {
			if (!zip.getName().endsWith(".zip")) {
				logger.warn("Non-zip file: " + zip.getName());
				continue ;
			}
			
			unzipFile(zip, typeToken, ret);
			logger.info("# loaded graphs: " + ret.size());
		}
		
		return ret;
	}
	
	public static <T> void unzipFile(File zipFile, 
			TypeToken<T> typeToken, 
			HashMap<String, T> container) {
		
		try {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
			ZipEntry curEntry = null;
			while ((curEntry = zis.getNextEntry()) != null) {
				String entryName = curEntry.getName();
				
				if (!entryName.endsWith(".json")) {
					logger.warn("Non-json file: " + entryName);
					continue ;
				}
				
				entryName = entryName.replace(".json", "");
				int start = entryName.indexOf("/") + 1;
				entryName = entryName.substring(start, entryName.length());
				
				if (MIBConfiguration.getInstance().isExclSpec()) {
					String methodName = entryName.split(":")[1];
					if (methodName.equals("toString") 
							|| methodName.equals("equals") 
							|| methodName.equals("hashCode")) {
						continue ;
					}
					
					if (IGINIT) {
						if (methodName.equals("<init>") || methodName.equals("<clinit>")) {
							continue ;
						}
					}
				}
				
				StringBuilder sb = new StringBuilder();
				byte[] buffer = new byte[1024];
				int read = 0;
				
				while ((read = zis.read(buffer, 0, 1024)) >= 0) {
					sb.append(new String(buffer, 0, read));
				}
				
				T value = GsonManager.readJsonGeneric(sb.toString(), typeToken);
				container.put(entryName, value);
				
				zis.closeEntry();
			}
			zis.close();
		} catch (Exception ex) {
			logger.error("Error: ", ex);
		}
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
			int parallelFactor = Runtime.getRuntime().availableProcessors();
			ExecutorService executor = Executors.newFixedThreadPool(parallelFactor);
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
	
	public static void main(String[] args) {
		File test = new File("graphs/cc.expbase.MyObject.zip");
		TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
		//unzipFile(test, graphToken);
	}

}
