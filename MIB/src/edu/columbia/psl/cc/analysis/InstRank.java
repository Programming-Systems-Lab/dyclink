package edu.columbia.psl.cc.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.math3.ml.distance.EuclideanDistance;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.util.GraphUtil;
import edu.columbia.psl.cc.util.TemplateLoader;

public class InstRank {
	
	private static EuclideanDistance ed = new EuclideanDistance();
	
	private InstPool pool;
		
	private double[] prior;
	
	private double[] pageRank;
	
	private HashMap<InstNode, Integer> instIndex = new HashMap<InstNode, Integer>();
	
	private double damping;
	
	private double epsilon;
	
	private int maxIteration;
	
	private static Comparator<InstWrapper> pageRankSorter = new Comparator<InstWrapper>() {
		public int compare(InstWrapper i1, InstWrapper i2) {
			return (i1.pageRank < i2.pageRank)?1:(i2.pageRank > i2.pageRank?-1:0);
		}
	};
	
	public InstRank(InstPool pool, double damping, double epsilon, int maxIteration) {
		this.pool = pool;
		this.prior = new double[this.pool.size()];
		this.pageRank = new double[this.pool.size()];
		this.damping = damping;
		this.epsilon = epsilon;
		this.maxIteration = maxIteration;
		
		for (int i = 0; i < this.prior.length; i++) {
			this.prior[i] = (1 - this.damping)/this.pool.size();
			this.pageRank[i] = (double)1/this.pool.size(); 
		}
		
		int counter = 0;
		for (InstNode inst: this.pool) {
			instIndex.put(inst, counter++);
		}
	}
	
	public static double calDistance(double[] v1, double[] v2) {
		return ed.compute(v1, v2);
	}
	
	public void computeInstRank() {
		boolean converge = false;
		
		HashMap<InstNode, HashSet<InstNode>> parentCache = new HashMap<InstNode, HashSet<InstNode>>();
		int iteration = 0;
		System.out.println("Ori pageRank: " + Arrays.toString(this.pageRank));
		while (!converge) {
			double[] oldPageRank = new double[this.pageRank.length];
			System.arraycopy(this.pageRank, 0, oldPageRank, 0, this.pageRank.length);
			System.out.println("Check old: " + Arrays.toString(oldPageRank));
			
			for (InstNode i: this.instIndex.keySet()) {
				int index = this.instIndex.get(i);
				HashSet<InstNode> parents = null;
				if (!parentCache.containsKey(i)) {
					parents = GraphUtil.retrieveAllParentInsts(i, this.pool);
					parentCache.put(i, parents);
				} else {
					parents = parentCache.get(i);
				}
				
				double valFromParents = 0;
				for (InstNode p: parents) {
					int pIndex = this.instIndex.get(p);
					double pRank = oldPageRank[pIndex];
					valFromParents += pRank/p.getChildFreqMap().size();
				}
				this.pageRank[index] = prior[index] + this.damping * valFromParents;
			}
			iteration++;
			
			double ep = calDistance(oldPageRank, this.pageRank);
			System.out.println("Current iteration: " + iteration);
			System.out.println("Current epsion: " + ep);
			if ((ep < this.epsilon) || iteration > this.maxIteration) {
				converge = true;
			}
		}
	}
	
	public List<InstWrapper> getPageRank() {
		List<InstWrapper> ret = new ArrayList<InstWrapper>();
		for (InstNode i: this.instIndex.keySet()) {
			int index = this.instIndex.get(i);
			double pageRank = this.pageRank[index];
			InstWrapper wrapper = new InstWrapper(i, pageRank);
			ret.add(wrapper);
		}
		Collections.sort(ret, pageRankSorter);
		return ret;
	}
	
	public static void main(String[] args) {
		File f = new File("./template/cc.testbase.TemplateMethod:increArray:([I):V:1.json");
		TypeToken<GraphTemplate> tt = new TypeToken<GraphTemplate>(){};
		GraphTemplate test = TemplateLoader.loadTemplateFile(f, tt);
		System.out.println("Inst node size: " + test.getInstPool().size());
		
		InstRank ranker = new InstRank(test.getInstPool(), 0.15, 1e-7, 50);
		ranker.computeInstRank();
		for (InstWrapper i: ranker.getPageRank()) {
			System.out.println(i.inst);
			System.out.println(i.pageRank);
		}
	}

}
