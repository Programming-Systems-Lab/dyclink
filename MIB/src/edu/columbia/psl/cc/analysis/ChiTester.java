package edu.columbia.psl.cc.analysis;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.util.GraphConstructor;
import edu.columbia.psl.cc.util.GsonManager;

public class ChiTester {
	
	private static double eucNorm = Math.sqrt(2);
	
	public static double[] genDistribution(Collection<InstNode> insts) {
		double[] ret = new double[256];
		
		for (InstNode inst: insts) {
			ret[inst.getOp().getOpcode()] += 1;
		}
		
		return ret;
	}
	
	public static void sumDistribution(double[] caller, double[] callee) {
		for (int i = 0; i < caller.length; i++) {
			caller[i] = caller[i] + callee[i];
		}
	}
	
	public static double[] normalizeDist(double[] d1, int v1) {
		double[] ret = new double[d1.length];
		for (int i = 0; i < d1.length; i++) {
			ret[i] = d1[i]/v1;
		}
		return ret;
	}
	
	public static double normalizeEucDistance(double[] n1, double[] n2) {
		double ret = 0;
		for (int i = 0; i < n1.length; i++) {
			double diff = n1[i] - n2[i];
			
			if (diff != 0) 
				ret += Math.pow(diff, 2);
		}
		ret = Math.sqrt(Math.abs(ret))/eucNorm;
		return ret;
	}
	
	public static boolean shouldTest(double[] d1, int v1, double[]d2, int v2) {
		double[] norm1 = normalizeDist(d1, v1);
		double[] norm2 = normalizeDist(d2, v2);
		
		double eucDistance = normalizeEucDistance(norm1, norm2);
		
		if (eucDistance > 0.3)
			return false;
		else
			return true;
		
	}
	
	public static void main(String[] args) {
		TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
		
		File tempFile = new File("./template/Jama.SingularValueDecomposition:<init>:0:0:15000003.json");
		GraphTemplate tempGraph = GsonManager.readJsonGeneric(tempFile, graphToken);
		GraphConstructor.reconstructGraph(tempGraph);
		double[] normDist1 = normalizeDist(tempGraph.getDist(), tempGraph.getVertexNum());
		System.out.println(Arrays.toString(tempGraph.getDist()));
		System.out.println(Arrays.toString(normDist1));
		
		File testFile = new File("./test/org.la4j.decomposition.SingularValueDecompositor:decompose:0:0:13591563.json");
		GraphTemplate testGraph = GsonManager.readJsonGeneric(testFile, graphToken);
		GraphConstructor.reconstructGraph(testGraph);
		double[] normDist2 = normalizeDist(testGraph.getDist(), testGraph.getVertexNum());
		System.out.println(Arrays.toString(testGraph.getDist()));
		System.out.println(Arrays.toString(normDist2));
		
		System.out.println("Static distance: " + normalizeEucDistance(normDist1, normDist2));
		System.out.println("Should test: " + shouldTest(tempGraph.getDist(), tempGraph.getVertexNum(), testGraph.getDist(), testGraph.getVertexNum()));
	}

}
