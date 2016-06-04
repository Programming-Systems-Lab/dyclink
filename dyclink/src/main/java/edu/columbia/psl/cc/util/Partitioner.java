package edu.columbia.psl.cc.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;

public class Partitioner {
	
	private static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
	private File graphFile;
	
	private GraphTemplate graph;
	
	public Partitioner(File graphFile) {
		this.graphFile = graphFile;
	}
	
	public void loadAndConstructGraph() {
		GraphTemplate rawGraph = GsonManager.readJsonGeneric(this.graphFile, graphToken);
		GraphConstructor gc = new GraphConstructor();
		gc.reconstructGraph(rawGraph, false);
		gc.cleanObjInit(rawGraph);
		this.graph = rawGraph;
	}
	
	public void partition() {
		for (InstNode inst: this.graph.getInstPool()) {
			int opCat = SearchUtil.getInstructionOp(inst);
			boolean isArrayStore = (opCat == 18 || opCat == 19 || opCat == 20);
			if (inst.getChildFreqMap().size() == 0 && !isArrayStore) {
				System.out.println("Possible sink: " + inst);
			}
		}
	}
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		
		System.out.println("Base repo: ");
		String baseRepo = scanner.nextLine();
		
		System.out.println("Lib:");
		String lib1 = scanner.nextLine();
		
		System.out.println("Target id:");
		String targetId = scanner.nextLine();
		
		List<String> possibleDir = new ArrayList<String>();
		possibleDir.add(baseRepo + lib1);
		
		File targetFile = TraceAnalyzer.searchFile(possibleDir, targetId);
		Partitioner partioner = new Partitioner(targetFile);
		partioner.loadAndConstructGraph();
		partioner.partition();
	}

}
