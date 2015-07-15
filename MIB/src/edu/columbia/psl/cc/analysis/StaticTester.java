package edu.columbia.psl.cc.analysis;

import java.util.Collection;
import java.util.HashMap;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.util.GlobalRecorder;
import edu.columbia.psl.cc.util.SearchUtil;

public class StaticTester {
	
	private static double eucNorm = Math.sqrt(2);
		
	//private static int simStrategy = MIBConfiguration.getInstance().getSimStrategy();
	
	
	/**
	 * Generate distribution from a collection of instructions
	 * @param insts
	 * @return
	 */
	public static double[] genDistribution(Collection<InstNode> insts) {
		int baseLength = SearchUtil.baseLength();
		int totalLength = baseLength + GlobalRecorder.getNativePackages().size() + 1;
		//System.out.println("Base length: " + baseLength);
		//System.out.println("Native length: " + GlobalRecorder.getNativePackages().size());
		//System.out.println("Total length: " + totalLength);
		double[] ret = new double[totalLength];
		
		for (InstNode inst: insts) {
			int repOp = SearchUtil.getInstructionOp(inst);
			ret[repOp] += 1;
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
	
	public static double jaccardSimilarity(double[] v1, double[] v2) {
		double v1Square = 0;
		double v2Square = 0;
		double v1v2 = 0;
		
		for (int i = 0; i < v1.length; i++) {
			v1Square += Math.pow(v1[i], 2);
			v2Square += Math.pow(v2[i], 2);
			v1v2 += v1[i] * v2[i]; 
		}
		
		double similarity = v1v2/(v1Square + v2Square - v1v2);
		return similarity;
 	}
	
	public static boolean shouldTest(double[] d1, int v1, double[]d2, int v2) {
		double[] norm1 = normalizeDist(d1, v1);
		double[] norm2 = normalizeDist(d2, v2);
		
		double eucDistance = normalizeEucDistance(norm1, norm2);
		
		if (eucDistance > 0.2)
			return false;
		else
			return true;
		
	}
	
	public static void main(String[] args) {
		/*TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
		
		File tempFile = new File("./template/Jama.SingularValueDecomposition:<init>:0:0:2.json");
		GraphTemplate tempGraph = GsonManager.readJsonGeneric(tempFile, graphToken);
		GraphConstructor constructor = new GraphConstructor();
		constructor.reconstructGraph(tempGraph, true);
		double[] dist1 = StaticTester.genDistribution(tempGraph.getDist());
		double[] normDist1 = normalizeDist(dist1, tempGraph.getVertexNum());
		System.out.println(Arrays.toString(dist1));
		System.out.println(Arrays.toString(normDist1));
		
		File testFile = new File("./test/cern.colt.matrix.linalg.SingularValueDecomposition:<init>:0:0:75.json");
		GraphTemplate testGraph = GsonManager.readJsonGeneric(testFile, graphToken);
		constructor = new GraphConstructor();
		constructor.reconstructGraph(testGraph, true);
		double[] dist2 = StaticTester.genDistribution(testGraph.getDist());
		double[] normDist2 = normalizeDist(dist2, testGraph.getVertexNum());
		System.out.println(Arrays.toString(dist2));
		System.out.println(Arrays.toString(normDist2));
		
		System.out.println("Static distance: " + normalizeEucDistance(normDist1, normDist2));
		System.out.println("Should test: " + shouldTest(dist1, tempGraph.getVertexNum(), dist2, testGraph.getVertexNum()));*/
		double[] dist1 = {1, 0, 0};
		double[] dist2 = {0, 0, 1};
		System.out.println("Jaccard sim: " + jaccardSimilarity(dist1, dist2));
	}

}
