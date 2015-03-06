package edu.columbia.psl.cc.util;

import java.io.File;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;

public class Debugger {
	
	private static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
	public static void main(String[] args) {
		String subName = "/Users/mikefhsu/Mike/Research/ec2/mib_sandbox_v2_beta/small2/org.ejml.alg.dense.decomposition.qr.QrHelperFunctions:rank1UpdateMultL:0:3:6648563.json";
		File subFile = new File(subName);
		GraphTemplate subGraph = GsonManager.readJsonGeneric(subFile, graphToken);
		GraphConstructor subGc = new GraphConstructor();
		subGc.reconstructGraph(subGraph, true);
		System.out.println("Original sub size: " + subGraph.getVertexNum());
		System.out.println("Reduced sub size: " + subGraph.getInstPool().size());
		
		System.out.println("Print sub");
		List<InstNode> sortedSub = GraphUtil.sortInstPool(subGraph.getInstPool(), true);
		for (InstNode i: sortedSub) {
			System.out.println(i);
		}
		
		String fileName = "/Users/mikefhsu/Mike/Research/ec2/mib_sandbox_v2_beta/small1/cern.colt.matrix.linalg.SingularValueDecomposition:<init>:0:0:3477305.json";
		String outputName = "./results/check.json";
		
		int threadId = 0;
		int methodId = 3477305;
		int instId = 601;
		
		int instCount = subGraph.getInstPool().size();
		
		File f = new File(fileName);
		GraphTemplate fileGraph = GsonManager.readJsonGeneric(f, graphToken);
		
		GraphConstructor gc = new GraphConstructor();
		gc.reconstructGraph(fileGraph, true);
		System.out.println("Original size: " + fileGraph.getVertexNum());
		System.out.println("Reduced size: " + fileGraph.getInstPool().size());
		
		System.out.println("Print target");
		List<InstNode> sortedInsts = GraphUtil.sortInstPool(fileGraph.getInstPool(), true);
		boolean start = false;
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
		for (int i = extStart; i <= extEnd; i++) {
			System.out.println(sortedInsts.get(i));
		}
		
		GsonManager.writeJsonGeneric(fileGraph, outputName, graphToken, -1);
	}

}
