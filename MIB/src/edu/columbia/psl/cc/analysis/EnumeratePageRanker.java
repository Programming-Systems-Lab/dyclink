package edu.columbia.psl.cc.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class EnumeratePageRanker {
	
	private static Logger logger = LogManager.getLogger(EnumeratePageRanker.class);
	
	public static void main(String[] args) {
		//String graphRepo = args[0];
		String graphRepo = "/Users/mikefhsu/ccws/R3P1Y14/graphrepo";
		File graphDir = new File(graphRepo);
		
		if (graphDir.isFile()) {
			logger.error("Invalid graph repo: " + graphDir.getAbsolutePath());
			System.exit(-1);
		}
		
		List<String> validDirs = new ArrayList<String>();
		for (File usrDir: graphDir.listFiles()) {
			if (usrDir.getName().startsWith(".")) 
				continue ;
			
			validDirs.add(usrDir.getAbsolutePath());
		}
		Collections.sort(validDirs);
		
		//Construct pairs to execute subgraph crawler
		List<Tuple> allPairs = new ArrayList<Tuple>();
		for (int i = 0; i < validDirs.size(); i++) {
			for (int j = i + 1; j < validDirs.size(); j++) {
				Tuple repoPair = new Tuple();
				repoPair.repo1 = validDirs.get(i);
				repoPair.repo2 = validDirs.get(j);
				allPairs.add(repoPair);
			}
		}
	}
	
	public static class Tuple {
		String repo1;
		
		String repo2;
		
		@Override
		public String toString() {
			return repo1 + "-" + repo2;
		}
		
		@Override
		public int hashCode() {
			return this.toString().hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Tuple)) {
				return false;
			}
			
			Tuple t = (Tuple) o;
			if (t.repo1.equals(this.repo1) && t.repo2.equals(this.repo2)) {
				return true;
			} else {
				return false;
			}
		}
	}

}
