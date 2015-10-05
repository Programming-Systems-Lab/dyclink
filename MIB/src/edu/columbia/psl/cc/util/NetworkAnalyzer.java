package edu.columbia.psl.cc.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.log4j.Logger;

import cern.colt.Arrays;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.analysis.InstWrapper;
import edu.columbia.psl.cc.analysis.JaroWinklerDistance;
import edu.columbia.psl.cc.analysis.PageRankSelector;
import edu.columbia.psl.cc.analysis.PercentageSelector;
import edu.columbia.psl.cc.analysis.StaticTester;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;

public class NetworkAnalyzer {
	
	private static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		
		System.out.println("Base repo: ");
		String baseRepo = scanner.nextLine();
		
		System.out.println("Lib1:");
		String lib1 = scanner.nextLine();
		
		System.out.println("Lib2:");
		String lib2 = scanner.nextLine();
		
		System.out.println("Target id:");
		String targetId = scanner.nextLine();
		
		System.out.println("Target start instruction:");
		String targetStart = scanner.nextLine();
		
		System.out.println("Target seg size:");
		int targetSegSize = Integer.valueOf(scanner.nextLine());
		
		System.out.println("Sub id:");
		String subId = scanner.nextLine();
		
		System.out.println("Percentage of instructions:");
		double printPercent = Double.valueOf(scanner.nextLine());
		
		List<String> possibleDirs = new ArrayList<String>();
		possibleDirs.add(baseRepo + lib1);
		possibleDirs.add(baseRepo + lib2);
		File targetFile = TraceAnalyzer.searchFile(possibleDirs, targetId);
		File subFile = TraceAnalyzer.searchFile(possibleDirs, subId);
				
		//Construct the full graph
		//Clean object init
		GraphTemplate targetGraph = GsonManager.readJsonGeneric(targetFile, graphToken);
		GraphConstructor gc = new GraphConstructor();
		gc.reconstructGraph(targetGraph, false);
		gc.cleanObjInit(targetGraph);
		
		GraphTemplate subGraph = GsonManager.readJsonGeneric(subFile, graphToken);
		gc.reconstructGraph(subGraph, false);
		gc.cleanObjInit(subGraph);
		
		//profile sub
		List<InstNode> sortedSub = GraphUtil.sortInstPool(subGraph.getInstPool(), true);
		
		//Pick the most important node from sorteSob
		System.out.println("Profiling sub graph: " + subGraph.getMethodName());
		
		//If the graph is reduced, some node will be removed and we choose not to clean them, save some time
		boolean partialPool = MIBConfiguration.getInstance().isReduceGraph();
		PageRankSelector subSelector = new PageRankSelector(subGraph.getInstPool(), partialPool, true);
		List<InstWrapper> subRank = subSelector.computePageRank();
		
		List<InstWrapper> selectedSub = PercentageSelector.selectImportantInstWrappers(subRank, printPercent);
		System.out.println("Sub: Top " + printPercent + " instruction info");
		int sCounter = 0;
		for (InstWrapper iw: selectedSub) {
			System.out.println(sCounter++ + " Instruction: " + iw.inst);
			System.out.println("Linenumber: " + iw.inst.getLinenumber());
			System.out.println("Page rank: " + iw.pageRank + "\n");
		}
		
		int[] subAllRep = SearchUtil.generatePageRankRep(subRank);
		int[] subPGRep = SearchUtil.generatePageRankRep(selectedSub);
		//int[] subPGRep = SearchUtil.generatePageRankRep(subRank);
		int segSize = sortedSub.size();
		System.out.println("Seg size: " + segSize);
		
		//Extract seg from target
		List<InstNode> sortedTarget = GraphUtil.sortInstPool(targetGraph.getInstPool(), true);
		InstPool targetSeg = new InstPool();
		int counter = 0;
		boolean start = false;
		
		int before = 0;
		boolean isCentroid = false;
		for (InstNode inst: sortedTarget) {
			if (inst.toString().equals(targetStart)) {
				start = true;
			}
			
			if (start) {
				targetSeg.add(inst);
				counter++;
			}
			
			/*if (inst.toString().equals(targetCentroid)) {
				isCentroid = true;
			}
			
			if (start && !isCentroid) {
				before++;
			}*/
			
			if (counter == targetSegSize) {
				break;
			}
		}
		System.out.println("Target seg size: " + targetSeg.size());
		//System.out.println("Before: " + before);
		//System.out.println("After: " + (targetSeg.size() - before - 1));
		
		PageRankSelector targetSelector = new PageRankSelector(targetSeg, true, true);
		List<InstWrapper> targetRank = targetSelector.computePageRank();
		List<InstWrapper> selectedTarget = PercentageSelector.selectImportantInstWrappers(targetRank, printPercent);
		
		System.out.println("Target seg: Top " + printPercent + " instruction info");
		int tCounter = 0;
		for (InstWrapper iw: selectedTarget) {
			System.out.println(tCounter++ + " Instruction: " + iw.inst);
			System.out.println("Linenumber: " + iw.inst.getLinenumber());
			System.out.println("Page rank: " + iw.pageRank + "\n");
		}
		
		JaroWinklerDistance measurer = new JaroWinklerDistance();
		JaroWinklerDistance boostM = new JaroWinklerDistance(0.8, 5);
		
		//Print 0.25, 0.5, 0.75, all
		for (double p = 0.25; p <= 0.75; p += 0.25) {
			List<InstWrapper> t = PercentageSelector.selectImportantInstWrappers(targetRank, p);
			List<InstWrapper> s = PercentageSelector.selectImportantInstWrappers(subRank, p);
			
			int[] targetP = SearchUtil.generatePageRankRep(t);
			int[] subP = SearchUtil.generatePageRankRep(s);
			
			System.out.println("Percentage: " + p);
			System.out.println("S rep: " + Arrays.toString(subP));
			System.out.println("T rep: " + Arrays.toString(targetP));
			System.out.println("Similarity: " + measurer.proximity(subP, targetP));
			System.out.println();
		}
		
		int[] targetAllRep = SearchUtil.generatePageRankRep(targetRank);
		
		System.out.println("S all rep: " + Arrays.toString(subAllRep));
		System.out.println("T all rep: " + Arrays.toString(targetAllRep));
		
		System.out.println("Similarity: " + measurer.proximity(subAllRep, targetAllRep));
		System.out.println("Boosted: " + boostM.proximity(subAllRep, targetAllRep));
	}

}
