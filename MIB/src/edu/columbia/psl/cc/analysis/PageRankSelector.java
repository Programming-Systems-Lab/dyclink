package edu.columbia.psl.cc.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.util.StringUtil;
import edu.columbia.psl.cc.util.TemplateLoader;
import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Hypergraph;


public class PageRankSelector {
	
	private static double alpha = 0.15;
	
	private static int maxIteration = 100;
	
	private static double epsilon = 1e-3;
	
	private static int instLimit = 3000;
	
	private static Comparator<InstWrapper> pageRankSorter = new Comparator<InstWrapper>() {
		public int compare(InstWrapper i1, InstWrapper i2) {
			return (i1.pageRank < i2.pageRank)?1:(i2.pageRank > i2.pageRank?-1:0);
		}
	};
	
	private InstPool myPool;
	
	public PageRankSelector(InstPool myPool) {
		this.myPool = myPool;
	}
	
	public DirectedSparseGraph<InstNode, Integer> convertToJungGraph() {
		DirectedSparseGraph<InstNode, Integer> retGraph = new DirectedSparseGraph<InstNode, Integer>();
		
		int edgeId = 0;
		HashMap<String, InstNode> cache = new HashMap<String, InstNode>();
		for (InstNode inst: myPool) {
			retGraph.addVertex(inst);
			
			for (String childKey: inst.getChildFreqMap().keySet()) {
				InstNode childNode = null;
				if (cache.containsKey(childKey)) {
					childNode = cache.get(childKey);
				} else {
					String[] keys = StringUtil.parseIdxKey(childKey);
					childNode = myPool.searchAndGet(keys[0], 
							Long.valueOf(keys[1]), Integer.valueOf(keys[2]), Integer.valueOf(keys[3]));
				}
				
				retGraph.addEdge(new Integer(edgeId++), inst, childNode);
			}
		}
		
		return retGraph;
	}
	
	public List<InstWrapper> computePageRank() {
		Hypergraph<InstNode, Integer> jungGraph = this.convertToJungGraph();
		System.out.println("Vertex size: " + jungGraph.getVertexCount());
		System.out.println("Edge size: " + jungGraph.getEdgeCount());
		
		PageRank<InstNode, Integer> ranker = new PageRank<InstNode, Integer>(jungGraph, alpha);
		
		ranker.setMaxIterations(maxIteration);
		ranker.setTolerance(epsilon);
		ranker.evaluate();
		
		List<InstWrapper> rankList = new ArrayList<InstWrapper>();
		for (InstNode inst: jungGraph.getVertices()) {
			InstWrapper iw = new InstWrapper(inst, ranker.getVertexScore(inst));
			rankList.add(iw);
		}
		
		Collections.sort(rankList, pageRankSorter);
		return rankList;
	}
	
	public InstPool selectRepPool() {
		InstPool ret = new InstPool();
		List<InstWrapper> sorted = this.computePageRank();
		for (int i = 0; i < instLimit; i++) {
			ret.add(sorted.get(i).inst);
		}
		return ret;
	}
	
	private static class InstWrapper {
		InstNode inst;
		
		double pageRank;
		
		public InstWrapper(InstNode inst, double pageRank) {
			this.inst = inst;
			this.pageRank = pageRank;
		}
	}
	
	public static void main(String[] args) {
		File f = new File("./tmp/JLex.CLexGen:userDeclare:():V:1.json");
		TypeToken<GraphTemplate> tt = new TypeToken<GraphTemplate>(){};
		GraphTemplate test = TemplateLoader.loadTemplateFile(f, tt);
		System.out.println("Inst node size: " + test.getInstPool().size());
		PageRankSelector selector = new PageRankSelector(test.getInstPool());
		List<InstWrapper> prSorted = selector.computePageRank();
		prSorted = prSorted.subList(0, 200);
		
		for (InstWrapper iw: prSorted) {
			System.out.println(iw.inst);
			System.out.println(iw.pageRank);
		}
	}

}
