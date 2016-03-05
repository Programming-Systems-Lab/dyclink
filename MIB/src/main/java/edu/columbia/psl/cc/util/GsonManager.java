package edu.columbia.psl.cc.util;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.NameMap;
import edu.columbia.psl.cc.pojo.Var;
import edu.columbia.psl.cc.premain.MIBDriver;

public class GsonManager {
	
	private static Logger logger = LogManager.getLogger(GsonManager.class);
			
	public static <T> T readJson(File f, T type) {
		GsonBuilder gb = new GsonBuilder();
		gb.setPrettyPrinting();
		gb.registerTypeAdapter(Var.class, new VarAdapter());
		Gson gson = gb.create();
		try {
			JsonReader jr = new JsonReader(new FileReader(f));
			T ret = gson.fromJson(jr, type.getClass());
			jr.close();
			return ret;
		} catch (Exception ex) {
			logger.error("Exception: ", ex);
		}
		return null;
	}
	
	/**
	 * 0 for template, 1 for test, 2 for labelmap
	 * @param obj
	 * @param fileName
	 * @param typeToken
	 * @param isTemplate
	 */
	public static <T> void writeJsonGeneric(T obj, 
			String fileName, 
			TypeToken<T> typeToken, 
			int dirIdx) throws Exception {
		String toWrite = jsonString(obj, typeToken);
		
		File f;
		if (dirIdx == MIBConfiguration.GRAPH_DIR) {
			f = new File(MIBConfiguration.getInstance().getGraphDir() + "/" + fileName + ".json");
		} else if (dirIdx == MIBConfiguration.LABEL_MAP_DIR) {
			f = new File(MIBConfiguration.getInstance().getLabelmapDir() + "/" + fileName + ".json");
		} else if (dirIdx == MIBConfiguration.CACHE_DIR){
			f = new File(MIBConfiguration.getInstance().getCacheDir() + "/" + fileName);
		} else {
			f = new File(fileName);
		}
		
		if (!f.exists()) {
			f.createNewFile();
		}
		
		BufferedWriter bw = new BufferedWriter(new FileWriter(f));
		bw.write(toWrite);
		bw.close();
	}
	
	public static <T> String jsonString(T obj, TypeToken<T> token) {
		GsonBuilder gb = new GsonBuilder();
		gb.setPrettyPrinting();
		gb.registerTypeAdapter(InstNode.class, new InstNodeAdapter());
		Gson gson = gb.enableComplexMapKeySerialization().create();
		String toWrite = gson.toJson(obj, token.getType());
		
		return toWrite;
	}
	
	public static boolean copyFile(File source, File cache) throws IOException{
		if (!cache.exists()) {
			cache.createNewFile();
		}
		
		FileChannel sourceChannel = new FileInputStream(source).getChannel();
		FileChannel cacheChannel = new FileOutputStream(cache).getChannel();
		
		try {
			cacheChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
			sourceChannel.close();
			cacheChannel.close();
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (sourceChannel != null)
				sourceChannel.close();
			
			if (cacheChannel != null)
				cacheChannel.close();
		}
		return false;
	}
		
	public static <T> T readJsonGeneric(File f, TypeToken typeToken) {
		GsonBuilder gb = new GsonBuilder();
		gb.setPrettyPrinting();
		gb.registerTypeAdapter(InstNode.class, new InstNodeAdapter());
		//Gson gson = gb.enableComplexMapKeySerialization().create();
		Gson gson = gb.create();
		try {
			JsonReader jr = new JsonReader(new FileReader(f));
			T ret = gson.fromJson(jr, typeToken.getType());
			jr.close();
			return ret;
		} catch (Exception ex) {
			logger.error("Excpetion: ", ex);
		}
		return null;
	}
	
	public static <T> T readJsonGeneric(String contents, TypeToken typeToken) {
		GsonBuilder gb = new GsonBuilder();
		gb.setPrettyPrinting();
		gb.registerTypeAdapter(InstNode.class, new InstNodeAdapter());
		//Gson gson = gb.enableComplexMapKeySerialization().create();
		Gson gson = gb.create();
		try {
			T ret = gson.fromJson(contents, typeToken.getType());
			return ret;
		} catch (Exception ex) {
			logger.error("Excpetion: ", ex);
		}
		return null;
	}
		
	private static void cleanHelper(String fileName) {
		File dir = new File(fileName);
		if (!dir.isDirectory()) {
			dir.delete();
		} else {
			for (File f: dir.listFiles()) {
				cleanHelper(f.getAbsolutePath());
			}
		}
	}
	
	public static void cleanDir(File dir) {
		if (!dir.isDirectory()) {
			dir.delete();
			dir.mkdir();
		} else {
			cleanHelper(dir.getAbsolutePath());
		}
	}
		
	public static void writeResult(String fileName, StringBuilder sb) {
		writeResult(fileName, sb.toString());
	}
	
	public static void writeResult(String fileName, String resultString) {
		//Date now = new Date();
		//String name = MIBConfiguration.getInstance().getResultDir() + "/" + compareName + now.getTime() + ".csv"; 
		File result = new File(fileName);
		
		try {
			if (!result.exists())
				result.createNewFile();
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(result, true));
			bw.write(resultString);
			bw.close();
		} catch (Exception ex) {
			logger.error("Exception: ", ex);
		}
	}

}
