package edu.columbia.psl.cc.analysis;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.OpcodeObj;
import edu.columbia.psl.cc.util.GraphConstructor;
import edu.columbia.psl.cc.util.GsonManager;

public class StaticTester {
	
	private static double eucNorm = Math.sqrt(2);
	
	/**
	 * Generate distribution from a collection of instructions
	 * @param insts
	 * @return
	 */
	public static double[] genDistribution(Collection<InstNode> insts) {
		if (MIBConfiguration.getInstance().getSimStrategy() 
				== MIBConfiguration.INST_STRAT) {
			double[] ret = new double[256];
			
			for (InstNode inst: insts) {
				ret[inst.getOp().getOpcode()] += 1;
			}
			
			return ret;
		} else if (MIBConfiguration.getInstance().getSimStrategy() 
				== MIBConfiguration.SUBSUB_STRAT) {
			double[] ret = new double[61];
			
			for (InstNode inst: insts) {
				ret[inst.getOp().getSubSubCatId()] += 1;
			}
			
			return ret;
		} else if (MIBConfiguration.getInstance().getSimStrategy() 
				== MIBConfiguration.SUB_STRAT) {
			double[] ret = new double[43];
			
			for (InstNode inst: insts) {
				ret[inst.getOp().getSubCatId()] += 1;
			}
			
			return ret;
		} else {
			double[] ret = new double[21];
			
			for (InstNode inst: insts) {
				ret[inst.getOp().getCatId()] += 1;
			}
			
			return ret;
		}
	}
	
	/**
	 * Generate distribution from instruction distribution
	 * @param instDist
	 * @return
	 */
	public static double[] genDistribution(double[] instDist) {
		if (MIBConfiguration.getInstance().getSimStrategy() 
				== MIBConfiguration.INST_STRAT) {
			return instDist;
		} else if (MIBConfiguration.getInstance().getSimStrategy() 
				== MIBConfiguration.SUBSUB_STRAT) {
			double[] ret = new double[61];
			
			for (int i = 0; i < instDist.length; i++) {
				OpcodeObj oo = BytecodeCategory.getOpcodeObj(i);
				if (oo != null)
					ret[oo.getSubSubCatId()] += instDist[i];
			}
			
			return ret;
		} else if (MIBConfiguration.getInstance().getSimStrategy() 
				== MIBConfiguration.SUB_STRAT) {
			double[] ret = new double[43];
			
			for (int i = 0; i < instDist.length; i++) {
				OpcodeObj oo = BytecodeCategory.getOpcodeObj(i);
				if (oo != null)
					ret[oo.getSubCatId()] += instDist[i];
			}
			
			return ret;
		} else {
			double[] ret = new double[21];
			
			for (int i = 0; i < instDist.length; i++) {
				OpcodeObj oo = BytecodeCategory.getOpcodeObj(i);
				if (oo != null)
					ret[oo.getCatId()] += instDist[i];
			}
			
			return ret;
		}
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
		
		if (eucDistance > 0.2)
			return false;
		else
			return true;
		
	}
	
	public static void main(String[] args) {
		TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
		
		File tempFile = new File("./template/Jama.SingularValueDecomposition:<init>:0:0:2.json");
		GraphTemplate tempGraph = GsonManager.readJsonGeneric(tempFile, graphToken);
		GraphConstructor.reconstructGraph(tempGraph);
		double[] dist1 = StaticTester.genDistribution(tempGraph.getDist());
		double[] normDist1 = normalizeDist(dist1, tempGraph.getVertexNum());
		System.out.println(Arrays.toString(tempGraph.getDist()));
		System.out.println(Arrays.toString(normDist1));
		
		File testFile = new File("./test/cern.colt.matrix.linalg.SingularValueDecomposition:<init>:0:0:75.json");
		GraphTemplate testGraph = GsonManager.readJsonGeneric(testFile, graphToken);
		GraphConstructor.reconstructGraph(testGraph);
		double[] dist2 = StaticTester.genDistribution(testGraph.getDist());
		double[] normDist2 = normalizeDist(dist2, testGraph.getVertexNum());
		System.out.println(Arrays.toString(testGraph.getDist()));
		System.out.println(Arrays.toString(normDist2));
		
		System.out.println("Static distance: " + normalizeEucDistance(normDist1, normDist2));
		System.out.println("Should test: " + shouldTest(dist1, tempGraph.getVertexNum(), dist2, testGraph.getVertexNum()));
	}

}
