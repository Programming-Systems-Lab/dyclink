package edu.columbia.psl.cc.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collection;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.util.GraphConstructor;
import edu.columbia.psl.cc.util.GsonManager;
import edu.columbia.psl.cc.analysis.PageRankSelector.WeightedEdge;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.uci.ics.jung.graph.Hypergraph;

public class PageRankTester {
	
	public static void main(String[] args) {
		String testGraphPath = args[0];
		File testGraphFile = new File(testGraphPath);
		
		TypeToken<GraphTemplate> token = new TypeToken<GraphTemplate>(){};
		GraphTemplate testGraph = GsonManager.readJsonGeneric(testGraphFile, token);
		System.out.println("Test graph: " + testGraph.getMethodName());
		System.out.println("Original size (v, e): " + testGraph.getVertexNum() + " " + testGraph.getEdgeNum());
		
		GraphConstructor constructor = new GraphConstructor();
		constructor.reconstructGraph(testGraph, true);
		constructor.cleanObjInit(testGraph);
		
		System.out.println("Reduced size (v, e): " + testGraph.getVertexNum() + " " + testGraph.getEdgeNum());
		
		PageRankSelector prs = new PageRankSelector(testGraph.getInstPool(), true, true);
		Hypergraph<InstNode, WeightedEdge> jungGraph = prs.convertToJungGraph();
		System.out.println("Jung graph size (v, e): " + jungGraph.getVertexCount() + " " + jungGraph.getEdgeCount());
		
		StringBuilder sb = new StringBuilder();
		for (InstNode inst: jungGraph.getVertices()) {
			sb.append("Current inst: " + inst + "\n");
			Collection<InstNode> children = jungGraph.getSuccessors(inst);
			
			for (InstNode c: children) {
				sb.append("Child: " + c + "\n");
				WeightedEdge we = jungGraph.findEdge(inst, c);
				sb.append("Weight: " + we.edgeWeight + "\n");
			}
			sb.append("\n");
		}
		
		File output = new File("./testgraph.txt");
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(output));
			bw.write(sb.toString());
			bw.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
