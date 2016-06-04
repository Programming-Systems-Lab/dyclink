package edu.columbia.psl.cc.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.analysis.GraphReducer;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;

public class Debugger {
	
	private static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
	public static void main(String[] args) {
		String subName = args[0];
		File subFile = new File(subName);
		GraphTemplate subGraph = GsonManager.readJsonGeneric(subFile, graphToken);
		GraphConstructor subGc = new GraphConstructor();
		subGc.reconstructGraph(subGraph, true);
		subGc.cleanObjInit(subGraph);
		System.out.println("Original sub size: " + subGraph.getVertexNum());
		System.out.println("Reduced sub size: " + subGraph.getInstPool().size());
		
		System.out.println("Print sub");
		StringBuilder subBuilder = new StringBuilder();
		List<InstNode> sortedSub = GraphUtil.sortInstPool(subGraph.getInstPool(), true);
		for (InstNode i: sortedSub) {
			//System.out.println(i);
			subBuilder.append(i.toString() + "\n");
		}
		
		String fileName = args[1];
		String outputName = "./results/check.json";
		
		int threadId = 0;
		int methodId = 190;
		int instId = 18;
		
		int instCount = subGraph.getInstPool().size();
		
		File f = new File(fileName);
		GraphTemplate fileGraph = GsonManager.readJsonGeneric(f, graphToken);
		
		GraphConstructor gc = new GraphConstructor();
		gc.reconstructGraph(fileGraph, true);
		gc.cleanObjInit(fileGraph);
		System.out.println("Original size: " + fileGraph.getVertexNum());
		System.out.println("Reduced size: " + fileGraph.getInstPool().size());
		
		System.out.println("Print target");
		List<InstNode> sortedInsts = GraphUtil.sortInstPool(fileGraph.getInstPool(), true);
		int startIdx = 0;
		for (int j = 0; j< sortedInsts.size(); j++) {
			InstNode i = sortedInsts.get(j);
			if (i.getThreadId() == threadId && i.getThreadMethodIdx() == methodId && i.getIdx() == instId) {
				startIdx = j;
				break;
			}
		}
		
		int endIdx = startIdx + instCount - 1;
		int extStart = startIdx - 3;
		int extEnd = endIdx + 3;
		
		if (extStart < 0)
			extStart = 0;
		if (extEnd > sortedInsts.size() - 1)
			extEnd = sortedInsts.size() - 1;
		
		StringBuilder targetBuilder = new StringBuilder();
		for (int i = extStart; i <= extEnd; i++) {
			//System.out.println(sortedInsts.get(i));
			targetBuilder.append(sortedInsts.get(i).toString() + "\n");
		}
		
		try {
			GsonManager.writeJsonGeneric(fileGraph, outputName, graphToken, -1);
			BufferedWriter bw = new BufferedWriter(new FileWriter("results/sub3.txt"));
			bw.append(subBuilder.toString());
			bw.flush();
			bw.close();
			
			BufferedWriter bw2 = new BufferedWriter(new FileWriter("results/target3.txt"));
			bw2.append(targetBuilder.toString());
			bw2.flush();
			bw2.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
