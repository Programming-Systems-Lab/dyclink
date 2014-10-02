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
import edu.columbia.psl.cc.pojo.CostObj;
import edu.columbia.psl.cc.pojo.InstNode;

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
			TreeMap<InstNode, TreeSet<InstNode>> depMap) {
		// TODO Auto-generated method stub
		ArrayList<InstNode> allNodes = new ArrayList<InstNode>(depMap.navigableKeySet());
		
		double[][] ret = new double[depMap.keySet().size()][depMap.keySet().size()];
		for (int i = 0; i < allNodes.size(); i++) {
			for (int j = 0; j < allNodes.size(); j++) {
				InstNode inst1 = allNodes.get(i);
				InstNode inst2 = allNodes.get(j);
				
				if (depMap.get(inst1) != null && depMap.get(inst1).contains(inst2)) {
					ret[i][j] = 1;
				} else {
					ret[i][j] = MIBConfiguration.getCostLimit();
				}
			}
		}
		
		/*ShortestPathKernel spk = new ShortestPathKernel();
		CostObj[][] costTable = spk.constructCostTable(methodName, depMap);
		for (int i = 0; i < allNodes.size(); i++) {
			for (int j = 0; j < allNodes.size(); j++) {
				ret[i][j] = costTable[i][j].getCost();
			}
		}*/
		
		//Debugging purpose, dump cost table
		StringBuilder sb = new StringBuilder();
		sb.append("head,");
		for (int i = 0; i < allNodes.size(); i++) {
			if (i == allNodes.size() - 1) {
				sb.append(allNodes.get(i) + "\n");
			} else {
				sb.append(allNodes.get(i) + ",");
			}
		}
				
		System.out.println("Check cost table");
		for (int i = 0; i < ret.length; i++) {
			StringBuilder rawBuilder = new StringBuilder();
			rawBuilder.append(allNodes.get(i) + ",");
			for (int j = 0; j < ret.length; j++) {
				rawBuilder.append(ret[i][j] + ",");
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
