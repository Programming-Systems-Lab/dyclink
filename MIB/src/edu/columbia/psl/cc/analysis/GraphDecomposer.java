package edu.columbia.psl.cc.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.util.StringUtil;
import edu.columbia.psl.cc.util.TemplateLoader;

public class GraphDecomposer {
	
	private double[][] adjMatrix;
	
	private double[][] diagonalMatrix;
	
	private ArrayList<InstNode> instIndex;
	
	public GraphDecomposer(Collection<InstNode> pool) {
		this.instIndex = new ArrayList<InstNode>(pool);
		this.adjMatrix = new double[pool.size()][pool.size()];
		this.diagonalMatrix = new double[pool.size()][pool.size()];
	}
	
	public static double sumMapVals(Collection<Double> vals) {
		double ret = 0;
		for (Double d: vals) {
			ret += d;
		}
		return ret;
	}
	
	/**
	 * Search on stackoverflow
	 * @param array
	 * @return
	 */
	public static int secondLargestIndex(double[] array) {
		double maxOne=array[0];
	    double maxTwo=array[1];
	    int firstIdx = 0;
	    int secondIdx = 0;

	    for(int i=0;i<array.length;i++){

	        if (array[i]>maxOne){
	            maxTwo=maxOne;
	            maxOne=array[i];
	            
	            secondIdx = firstIdx;
	            firstIdx = i;
	        } else if (array[i]>maxTwo) {
	            maxTwo=array[i];
	            secondIdx = i;
	        }

	    }

	    System.out.println(maxOne);
	    System.out.println(maxTwo);

        return secondIdx;
	}
	
	public ArrayList<InstNode> getInstIndex() {
		return this.instIndex;
	}
	
	public double[][] getAdjMatrix() {
		return this.adjMatrix;
	}
	
	public double[][] getDiagonalMatrix() {
		return this.diagonalMatrix;
	}
	
	public double[][] getLaplacianMatrix() {
		//D-A
		double[][] ret = new double[adjMatrix.length][adjMatrix.length];
		
		for (int i = 0; i < ret.length; i++) {
			for (int j = 0; j < ret.length; j++) {
				ret[i][j] = this.diagonalMatrix[i][j] - this.adjMatrix[i][j];
			}
		}
		
		return ret;
	}
	
	public void constructPMatrix() {
		for (int i = 0; i < this.instIndex.size(); i++) {
			InstNode i1 = this.instIndex.get(i);
			int base = i1.getChildFreqMap().size();
			for (int j = 0; j < this.instIndex.size(); j++) {
				if (i == j)
					continue ;
				
				if (base == 0)
					continue ;
				
				InstNode i2 = this.instIndex.get(j);
				String inst2Key = StringUtil.genIdxKey(i2.getFromMethod(), i2.getThreadId(), i2.getThreadMethodIdx(), i2.getIdx());
				if (i1.getChildFreqMap().containsKey(inst2Key)) {
					this.adjMatrix[i][j] = ((double)1)/base;
				}
			}
		}
	}
	
	/**
	 * Remove direction to try first
	 */
	public void constructAdjMatrix() {
		for (int i = 0; i < this.instIndex.size(); i++) {
			InstNode i1 = this.instIndex.get(i);
			//this.diagonalMatrix[i][i] += sumMapVals(i1.getChildFreqMap().values());
			this.diagonalMatrix[i][i] += i1.getChildFreqMap().size();
			for (int j = i; j < this.instIndex.size(); j++) {
				if (i == j)
					continue ;
				
				InstNode i2 = this.instIndex.get(j);
				String inst2Key = StringUtil.genIdxKey(i2.getFromMethod(), i2.getThreadId(), i2.getThreadMethodIdx(), i2.getIdx());
				if (i1.getChildFreqMap().containsKey(inst2Key)) {
					double freq = i1.getChildFreqMap().get(inst2Key);
					/*this.adjMatrix[i][j] = freq;
					this.adjMatrix[j][i] = freq;
					this.diagonalMatrix[j][j] += freq;*/
					
					this.adjMatrix[i][j] = 1;
					this.adjMatrix[j][i] = 1;
					this.diagonalMatrix[j][j] += 1;
				}
			}
		}
	}
	
	public EigenDecomposition decomposeGraph() {
		RealMatrix m = MatrixUtils.createRealMatrix(this.getLaplacianMatrix());
		EigenDecomposition decompose = new EigenDecomposition(m);
		return decompose;
	}
	
	public EigenDecomposition decomposeGraphOnP() {
		RealMatrix m = MatrixUtils.createRealMatrix(this.getAdjMatrix());
		EigenDecomposition decompose = new EigenDecomposition(m);
		return decompose;
	}
	
	public static void main(String[] args) {
		File f = new File("./template/cc.testbase.TemplateMethod:all3Methods:(II):I:1.json");
		TypeToken<GraphTemplate> type = new TypeToken<GraphTemplate>(){};
		GraphTemplate g = TemplateLoader.loadTemplateFile(f, type);
		System.out.println("Vertex num: " + g.getInstPool().size());
		
		GraphDecomposer gd = new GraphDecomposer(g.getInstPool());
		gd.constructPMatrix();
		/*gd.constructAdjMatrix();
		double[][] diag = gd.getLaplacianMatrix();
		for (int i = 0; i < diag.length; i++) {
			System.out.println(Arrays.toString(diag[i]));
		}
		EigenDecomposition decompose = gd.decomposeGraph();
		double[] eigenvalues = decompose.getRealEigenvalues();*/
		
		/*double[][] transMatrix = gd.getAdjMatrix();
		for (int i = 0; i < transMatrix.length; i++) {
			System.out.println(Arrays.toString(transMatrix[i]));
		}*/
		
		EigenDecomposition decompose = gd.decomposeGraphOnP();
		double[] eigenvalues = decompose.getRealEigenvalues();
		System.out.println("Eigen values: " + Arrays.toString(eigenvalues));
		
		int secondIdx = secondLargestIndex(eigenvalues);
		System.out.println("Check secondIdx: " + secondIdx);
		
		//double[] eigenvalues = decompose.getRealEigenvalues();
		RealVector v = decompose.getEigenvector(secondIdx);
		double[] fArray = v.toArray();
		System.out.println("Fiedler: " + Arrays.toString(fArray));
		
		fArray = v.toArray();
		ArrayList<InstNode> g1 = new ArrayList<InstNode>();
		ArrayList<InstNode> g2 = new ArrayList<InstNode>();
		for (int i = 0; i < fArray.length; i++) {
			if (fArray[i] > 0) {
				g1.add(gd.getInstIndex().get(i));
				System.out.println(gd.getInstIndex().get(i) + " f val " + fArray[i] + " g1");
			} else {
				g2.add(gd.getInstIndex().get(i));
				System.out.println(gd.getInstIndex().get(i) + " f val " + fArray[i] + " g2");
			}
			
		}
		System.out.println("Group1: " + g1);
		System.out.println("Group2: " + g2);
		for (InstNode i: g1) {
			System.out.println(i);
		}
	}
}
