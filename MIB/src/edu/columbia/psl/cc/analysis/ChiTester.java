package edu.columbia.psl.cc.analysis;

public class ChiTester {
	
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
	
	public static double eucDistance(double[] n1, double[] n2) {
		double ret = 0;
		for (int i = 0; i < n1.length; i++) {
			double diff = n1[i] - n2[i];
			
			if (diff != 0) 
				ret += Math.pow(diff, 2);
		}
		ret = Math.sqrt(ret);
		return ret;
	}
	
	public static boolean shouldTest(double[] d1, int v1, double[]d2, int v2) {
		double[] norm1 = normalizeDist(d1, v1);
		double[] norm2 = normalizeDist(d2, v2);
		
		double eucDistance = eucDistance(norm1, norm2);
		
		if (eucDistance > 0.16)
			return false;
		else
			return true;
		
	}

}
