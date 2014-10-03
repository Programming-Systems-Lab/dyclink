package edu.columbia.psl.cc.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.CostObj;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.util.GraphUtil;

public class SVDKernel implements MIBSimilarity<double[][]>{

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		double[][] A = {{3, 1, 1}, {-1, 3, 1}};
		SVDKernel sk = new SVDKernel();
		double[] s = sk.getSingularVector(A);
		System.out.println(Arrays.toString(s));
		
		double[] a1 = {1, 2, 3};
		double[] a2 = {4, 5, 6};
		System.out.println(sk.innerProduct(a1, a2));
	}
	
	public double[] getSingularVector(double[][] matrix) {
		RealMatrix m = MatrixUtils.createRealMatrix(matrix);
		SingularValueDecomposition svd = new SingularValueDecomposition(m);
		double[] sValues = svd.getSingularValues();
		return sValues;
	}
	
	public double innerProduct(double[] s1, double[] s2) {
		RealVector rv1 = new ArrayRealVector(s1);
		RealVector rv2 = new ArrayRealVector(s2);
		return rv1.dotProduct(rv2);
	}

	@Override
	public double calculateSimilarity(double[][] metric1, double[][] metric2) {
		double[] s1 = this.getSingularVector(metric1);
		double[] s2 = this.getSingularVector(metric2);
		
		System.out.println("s1 singular vector: " + Arrays.toString(s1));
		System.out.println("s2 singular vector: " + Arrays.toString(s2));
		
		int max = s1.length;
		boolean expandS1 = false;
		if (s2.length > s1.length) {
			max = s2.length;
			expandS1 = true;
		}
		
		if (expandS1) {
			double[] expandedS1 = new double[max];
			System.arraycopy(s1, 0, expandedS1, 0, s1.length);
			for (int i = s1.length; i < max; i++) {
				expandedS1[i] = 0;
			}
			return this.innerProduct(expandedS1, s2);
		} else {
			double[] expandedS2 = new double[max];
			System.arraycopy(s2, 0, expandedS2, 0, s2.length);
			for (int i = s2.length; i < max; i++) {
				expandedS2[i] = 0;
			}
			return this.innerProduct(s1, expandedS2);
		}
	}
	
	@Override
	public double[][] constructCostTable(String methodName,
			InstPool pool) {
		// List here is just for facilitate dumping cost table. Should remove after
		System.out.println("Check inst pool: " + pool);
		ArrayList<InstNode> allNodes = new ArrayList<InstNode>(pool);
		double[][] ret = new double[allNodes.size()][allNodes.size()];
		
		for (int i = 0; i < allNodes.size(); i++) {
			InstNode i1 = allNodes.get(i);
			for (int j = 0; j < allNodes.size(); j++) {
				InstNode i2 = allNodes.get(j);
				if (i1.getChildFreqMap().containsKey(i2.getIdx())) {
					double rawVal = (double)1/i1.getChildFreqMap().get(i2.getIdx());
					double roundVal = GraphUtil.roundValue(rawVal);
					ret[i][j] = roundVal;
				} else {
					//ret[i][j] = MIBConfiguration.getCostLimit();
					ret[i][j] = 0;
				}
			}
		}
		
		/*ShortestPathKernel spk = new ShortestPathKernel();
		CostObj[][] costTable = spk.constructCostTable(methodName, pool);
		for (int i = 0; i < allNodes.size(); i++) {
			for (int j = 0; j < allNodes.size(); j++) {
				ret[i][j] = costTable[i][j].getCost();
			}
		}*/
		
		//Debugging purpose, dump cost table
		StringBuilder sb = new StringBuilder();
		sb.append("head,");
		for (int k = 0; k < allNodes.size(); k++) {
			if (k == pool.size() - 1) {
				sb.append(allNodes.get(k).toString() + "\n");
			} else {
				sb.append(allNodes.get(k).toString() + ",");
			}
		}
				
		System.out.println("Check cost table");
		for (int m = 0; m < ret.length; m++) {
			StringBuilder rawBuilder = new StringBuilder();
			rawBuilder.append(allNodes.get(m) + ",");
			for (int n = 0; n < ret.length; n++) {
				rawBuilder.append(ret[m][n] + ",");
			}
			sb.append(rawBuilder.toString().substring(0, rawBuilder.length() - 1) + "\n");
		}
				
		try {
			File f = new File(MIBConfiguration.getCostTableDir() + methodName + ".csv");
			if (f.exists()) {
				f.delete();
			}
			f.createNewFile();
			BufferedWriter bw = new BufferedWriter(new FileWriter(f));
			bw.write(sb.toString());
			bw.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return ret;
	}

}
