package edu.columbia.psl.cc.inst;

import java.io.File;

public class StaticAnalyzer {
	
	public static void processDir(File dir, String destPath) {
		for (File f: dir.listFiles()) {
			if (f.isDirectory()) {
				String check = destPath + "/" + f.getName();
				File checkDir = new File(check);
				if (!checkDir.exists()) {
					checkDir.mkdir();
				}
				processDir(f, destPath);
			} else {
				if (f.getName().endsWith(".class")) {
					
				}
			}
		}
	}
	
	public static void main(String[] args) {
		String codePath = args[1];
		File codebase = new File(codePath);
		if (!codebase.exists()) {
			codebase.mkdir();
		}
		
		String destPath = args[2];
		File dest = new File(destPath);
		if (!dest.exists()) {
			dest.mkdir();
		}
		
		
	}

}
