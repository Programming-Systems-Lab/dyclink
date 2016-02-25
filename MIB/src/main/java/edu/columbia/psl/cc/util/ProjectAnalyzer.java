package edu.columbia.psl.cc.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ProjectAnalyzer {
	
	public static HashMap<String, String> problemNameMap = new HashMap<String, String>();
	
	static {
		problemNameMap.put("R5P1Y11", "IC");
		problemNameMap.put("R5P1Y12", "PG");
		problemNameMap.put("R5P1Y13", "CH");
		problemNameMap.put("R5P1Y14", "MT");
	}
	
	public static String getProblemId(String method) {
		String year = extractYear(method);
		return problemNameMap.get(year);
	}
	
	public static String extractYear(String method) {
		String[] split = method.split("\\.");
		return split[0];
	}
	
	public static String extractPkg(String method) {
		String[] split = method.split("\\.");
		String pkg = split[0] + "." + split[1];
		return pkg;
	}
	
	public static void insertPkg(String pkg, HashSet<PkgFreq> record) {
		for (PkgFreq p: record) {
			if (p.pkg.equals(pkg)) {
				p.freq = p.freq + 1;
				return ;
			}
		}
		
		PkgFreq pf = new PkgFreq();
		pf.pkg = pkg;
		pf.freq = 1;
		record.add(pf);
	}
	
	public static void main(String[] args) {
		Console console = System.console();
		try {
			System.out.println("Neighbor result file: ");
			String fileLoc = console.readLine();
			
			File f = new File(fileLoc);
			if (!f.exists()) {
				System.err.println("Invalid file path: " + f.getAbsolutePath());
				System.exit(-1);
			}
			
			BufferedReader br = new BufferedReader(new FileReader(f));
			//Header
			br.readLine();
			
			String buf = "";
			HashMap<String, HashSet<PkgFreq>> record = 
					new HashMap<String, HashSet<PkgFreq>>();
			while ((buf = br.readLine()) != null) {
				String[] data = buf.split(",");
				String me = data[0];
				String myPkg = extractPkg(me);
				
				HashSet<PkgFreq> nPkgs = null;
				if (record.containsKey(myPkg)) {
					nPkgs = record.get(myPkg);
				} else {
					nPkgs = new HashSet<PkgFreq>();
					record.put(myPkg, nPkgs);
				}
				
				String fNeighbor = data[2];
				String fPkg = extractPkg(fNeighbor);
				insertPkg(fPkg, nPkgs);
				
				String sNeighbor = data[4];
				if (!sNeighbor.equals(" ")) {
					insertPkg(extractPkg(sNeighbor), nPkgs);
				}
				
				String tNeighbor = data[6];
				if (!tNeighbor.equals(" ")) {
					insertPkg(extractPkg(tNeighbor), nPkgs);
				}
				
				String foNeighbor = data[8];
				if (!foNeighbor.equals(" ")) {
					insertPkg(extractPkg(foNeighbor), nPkgs);
				}
				
				String fiNeighbor = data[10];
				if (!fiNeighbor.equals(" ")) {
					insertPkg(extractPkg(fiNeighbor), nPkgs);
				}
			}
			
			List<String> allPkgs = new ArrayList<String>(record.keySet());
			Collections.sort(allPkgs);
			
			StringBuilder headerBuilder = new StringBuilder();
			headerBuilder.append(" ,");
			for (String ap: allPkgs) {				
				headerBuilder.append(ap + ",");
			}
			String header = headerBuilder.substring(0, headerBuilder.length() - 1) + "\n";
			
			StringBuilder allRecord = new StringBuilder();
			allRecord.append(header);
			for (String curPkg: allPkgs) {
				HashSet<PkgFreq> nPkgs = record.get(curPkg);
				StringBuilder sb = new StringBuilder();
				sb.append(curPkg + ",");
				for (String nPkg: allPkgs) {
					boolean found = false;
					for (PkgFreq pf: nPkgs) {
						if (pf.pkg.equals(nPkg)) {
							sb.append(pf.freq + ",");
							found = true;
							break ;
						}
					}
					
					if (!found) {
						sb.append(0 + ",");
					}
				}
				allRecord.append(sb.substring(0, sb.length() - 1) + "\n");
			}
			
			System.out.println("Check results: ");
			for (String myPkg: record.keySet()) {
				System.out.println("MyPkg: " + myPkg);
				System.out.println("NeighborPkg: " + record.get(myPkg));
			}
			
			File resultFile = new File("./results/pkg.csv");
			System.out.println("Writing results to: " + resultFile.getAbsolutePath());
			BufferedWriter bw = new BufferedWriter(new FileWriter(resultFile));
			bw.write(allRecord.toString());
			bw.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static class PkgFreq {
		
		public String pkg;
		
		int freq;
		
		@Override
		public String toString() {
			return pkg + ":" + freq;
		}
	}
}
