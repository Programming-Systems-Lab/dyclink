package edu.columbia.psl.cc.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Enumerater {
	
	private static Logger logger = LogManager.getLogger(Enumerater.class);
	
	public static List<String> enumerate(List<String> graphRepos) {
		List<String> validDirs = new ArrayList<String>();
		for (String graphRepo: graphRepos) {
			File graphDir = new File(graphRepo);
			
			if (!graphDir.exists() || graphDir.isFile()) {
				logger.error("Invalid graph repo: " + graphDir.getAbsolutePath());
				continue ;
			}
			
			for (File usrDir: graphDir.listFiles()) {
				if (usrDir.getName().startsWith("."))
					continue ;
				
				validDirs.add(usrDir.getAbsolutePath());
			}
		}
		Collections.sort(validDirs);
		logger.info("# of valid usr dirs: " + validDirs.size());
		return validDirs;
	}
	
	public static List<String> enumerate(String[] graphrepos) {
		List<String> repoStrings = Arrays.asList(graphrepos);
		return enumerate(repoStrings);
	}
}
