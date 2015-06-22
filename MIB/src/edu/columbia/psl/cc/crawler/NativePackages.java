package edu.columbia.psl.cc.crawler;

import java.io.File;
import java.util.HashMap;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.util.GsonManager;

public class NativePackages {
	
	//Rigt after the 255th bytecode, facilitate the static dist calculation
	public static int defaultId = 256;
	
	//Key: api, Val: id
	private HashMap<String, Integer> nativePackages = new HashMap<String, Integer>();
	
	public void addNativePackage(String nativePackage, int id) {
		if (!this.nativePackages.containsKey(nativePackage)) {
			this.nativePackages.put(nativePackage, id);
		}
	}
		
	public HashMap<String, Integer> getNativePackages() {
		return this.nativePackages;
	}
	
	public void setNativePackages(HashMap<String, Integer> nativePackages) {
		this.nativePackages = nativePackages;
	}
	
	public static void main(String[] args) {
		String npFileName = MIBConfiguration.getInstance().getLabelmapDir() + "/nativePackages.json";
		File npFile = new File(npFileName);
		if (npFile.exists()) {
			TypeToken<NativePackages> npToken = new TypeToken<NativePackages>(){};
			NativePackages nativePackages = GsonManager.readJsonGeneric(npFile, npToken);
			HashMap<String, Integer> nativeMap = nativePackages.getNativePackages();
			System.out.println("nativeMap: " + nativeMap);
		}
	}
}
