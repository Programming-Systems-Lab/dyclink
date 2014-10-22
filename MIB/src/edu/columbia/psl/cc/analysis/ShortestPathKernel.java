package edu.columbia.psl.cc.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.datastruct.VarPool;
import edu.columbia.psl.cc.pojo.CostObj;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.Var;
import edu.columbia.psl.cc.premain.MIBDriver;
import edu.columbia.psl.cc.util.GraphUtil;
import edu.columbia.psl.cc.util.StringUtil;

public class ShortestPathKernel implements MIBSimilarity<CostObj[][]> {
		
	//private static int limit = (int)1E2;
	
	public static VarPool addFakeVar(VarPool smallPool, int diff) {
		VarPool ret = new VarPool(smallPool);
		for (int i = 0; i < diff; i++) {
			ret.searchVar(null, null, 3, null);
		}
		return ret;
	}
	
	public static double calConstant(int g1Num, int g2Num) {
		System.out.println("Check g1Num: " + g1Num);
		System.out.println("Check g2Num: " + g2Num);
		return 0.25 * (Math.pow(g1Num, 2) - g1Num) * (Math.pow(g2Num, 2) - g2Num);  
	}
	
	@Override
	public String getResult() {
		return null;
	}
	
	private int kernelMethod(CostObj x1, CostObj x2) {
		if (x1.getLabels().equals(x2.getLabels()) && 
				x1.getCost() == x2.getCost() &&
				x1.getCost() < MIBConfiguration.getInstance().getCostLimit() &&
				x1.getCost() > 0) {
			//System.out.println("Debugger: " + x1);
			return 1;
		} else {
			return 0;
		}
	}
	
	@Override
	public double calculateSimilarity(CostObj[][]g1, CostObj[][]g2) {
		int total = 0;
		for (int i = 0; i < g1.length; i++) {
			for (int j = 0; j < g1.length; j++) {
				for (int k = 0; k < g2.length; k++) {
					for (int l =0; l < g2.length; l++) {
						total += kernelMethod(g1[i][j], g2[k][l]);
					}
				}
			}
		}
		
		double constant = calConstant(g1.length, g2.length);
		double ret = (1/constant) * total;
		System.out.println("Check constant: " + constant);
		System.out.println("Check total: " + total);
		System.out.println("Check ret: " + ret);
		return ret;
	}
	
	public int[][] constructCostTable(VarPool vp1) {
		//Need fix position
		ArrayList<Var> varList = new ArrayList<Var>(vp1);
		int[][] costTable = new int[varList.size()][varList.size()];
		
		//Just for check the position of each vertice
		for (int i = 0; i < varList.size(); i++) {
			System.out.println(i + " " + varList.get(i));
		}
		
		for (int i = 0; i < costTable.length; i++) {
			for (int j = 0; j < costTable.length; j++) {
				if (i == j)
					continue;
				
				Var v1 = varList.get(i);
				Var v2 = varList.get(j);
				
				if (v1.getAll().contains(v2)) {
					costTable[i][j] = 1;
				} else {
					costTable[i][j] = MIBConfiguration.getInstance().getCostLimit();
				}
			}
		}
		
		for (int i = 0; i < costTable.length; i++) {
			for (int j = 0; j < costTable.length; j++) {
				for (int k = 0; k < costTable.length; k++) {
					if (costTable[i][j] > costTable[i][k] + costTable[k][j]) {
						costTable[i][j] = costTable[i][k] + costTable[k][j];
					}
				}
			}
		}
		
		System.out.println("Check cost table");
		for (int i = 0; i < costTable.length; i++) {
			StringBuilder sb = new StringBuilder();
			sb.append("For " + i + ": ");
			for (int j = 0; j < costTable.length; j++) {
				sb.append(costTable[i][j] + " ");
			}
			System.out.println(sb.toString());
		}
		
		return costTable;
	}
	
	@Override
	public CostObj[][] constructCostTable(String methodName, InstPool pool) {
		ArrayList<InstNode> allNodes = new ArrayList<InstNode>(pool);
		
		//int[][] costTable = new int[allNodes.size()][allNodes.size()];
		CostObj[][] costTable = new CostObj[allNodes.size()][allNodes.size()];
		for (int i = 0; i < costTable.length; i++) {
			InstNode inst1 = allNodes.get(i);
			String startLabel = inst1.getOp().getInstruction();
			for (int j = 0; j < costTable.length; j++) {
				
				CostObj co = new CostObj();
				InstNode inst2 = allNodes.get(j);
				String inst2Key = StringUtil.genIdxKey(inst2.getFromMethod(), inst2.getIdx());
			
				String endLabel = inst2.getOp().getInstruction();
				co.addLabel(startLabel);
				co.addLabel(endLabel);
				
				if (i == j) {
					co.setCost(0);
					costTable[i][j] = co;
				} else if (!inst1.getChildFreqMap().containsKey(inst2Key)) {
					co.setCost(MIBConfiguration.getInstance().getCostLimit());
					costTable[i][j] = co;
					//costTable[i][j] = limit;
				} else {
					double rawCost = (double)1/inst1.getChildFreqMap().get(inst2Key);
					double cost = GraphUtil.roundValue(rawCost);
					co.setCost(cost);
					costTable[i][j] = co;
				}
			}
		}
		
		for (int i = 0; i < costTable.length; i++) {
			for (int j = 0; j < costTable.length; j++) {
				for (int k = 0; k < costTable.length; k++) {
					if (costTable[i][j].getCost() > costTable[i][k].getCost() + costTable[k][j].getCost()) {
						//costTable[i][j].cost = costTable[i][k].cost + costTable[k][j].cost;
						costTable[i][j].setCost(costTable[i][k].getCost() + costTable[k][j].getCost());
					}
				}
			}
		}
		
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
		for (int i = 0; i < costTable.length; i++) {
			StringBuilder rawBuilder = new StringBuilder();
			rawBuilder.append(allNodes.get(i) + ",");
			for (int j = 0; j < costTable.length; j++) {
				rawBuilder.append(costTable[i][j].getCost() + ",");
			}
			sb.append(rawBuilder.toString().substring(0, rawBuilder.length() - 1) + "\n");
		}
		
		try {
			File f = new File(MIBConfiguration.getInstance().getCostTableDir() + methodName + ".csv");
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
		
		return costTable;
	}
	
	public static void main(String[] args) {
		//Graph1
		VarPool vp1 = new VarPool();
		Var a = vp1.searchVar("graph1", "method1", 2, "1");
		Var a1 = vp1.searchVar("graph1", "method1", 1, "native:a1Var");
		Var a2 = vp1.searchVar("graph1", "method1", 1, "native:a2Var");
		
		a.addChildren(a1);
		a.addChildren(a2);
		a1.addChildren(a2);
		
		VarPool vp2 = new VarPool();
		Var b = vp2.searchVar("graph2", "method2", 2, "1");
		Var b1 = vp2.searchVar("graph2", "method2", 1, "native2:b1Var");
		Var b2 = vp2.searchVar("graph2", "method2", 1, "native2:b2Var");
		
		b.addChildren(b1);
		b.addChildren(b2);
		b2.addChildren(b1);
		
		VarPool vp3 = new VarPool();
		Var c = vp3.searchVar("graph3", "method3", 2, "1");
		Var c1 = vp3.searchVar("graph3", "method3", 1, "native3:c1Var");
		Var c2 = vp3.searchVar("graph3", "method3", 1, "native3:c2Var");
		
		c.addChildren(c1);
		c1.addChildren(c2);
		c2.addChildren(c);
		
		System.out.println("Check var size: " + vp1.size());
		for (Var v: vp1) {
			if (v.getChildren().size() > 0) {
				System.out.print("Source: " + v + "->");
			} else {
				continue;
			}
			
			for (String edge: v.getChildren().keySet()) {
				System.out.println(edge);
				Set<Var> edgeChildren = v.getChildren().get(edge);
				for (Var ev: edgeChildren) {
					System.out.println("->" + "Sink: " +  ev);
				}
			}
		}
		
		System.out.println("Check var size: " + vp2.size());
		for (Var v: vp2) {
			if (v.getChildren().size() > 0) {
				System.out.print("Source: " + v + "->");
			} else {
				continue;
			}
			
			for (String edge: v.getChildren().keySet()) {
				System.out.println(edge);
				Set<Var> edgeChildren = v.getChildren().get(edge);
				for (Var ev: edgeChildren) {
					System.out.println("->" + "Sink: " +  ev);
				}
			}
		}
		
		System.out.println("Check var size: " + vp3.size());
		for (Var v: vp3) {
			if (v.getChildren().size() > 0) {
				System.out.print("Source: " + v + "->");
			} else {
				continue;
			}
			
			for (String edge: v.getChildren().keySet()) {
				System.out.println(edge);
				Set<Var> edgeChildren = v.getChildren().get(edge);
				for (Var ev: edgeChildren) {
					System.out.println("->" + "Sink: " +  ev);
				}
			}
		}
				
		ShortestPathKernel spk = new ShortestPathKernel();
		int[][] costTable1 = spk.constructCostTable(vp1);
		int[][] costTable2 = spk.constructCostTable(vp2);
		int[][] costTable3 = spk.constructCostTable(vp3);
		
		//System.out.println("Score kernel: " + spk.scoreShortestPaths(costTable1, costTable2));
		//System.out.println("Score kernel: " + spk.scoreShortestPaths(costTable2, costTable3));
		//System.out.println("Score kernel: " + spk.scoreShortestPaths(costTable1, costTable3));
	}

	@Override
	public void calculateSimilarities(HashMap<String, GraphTemplate> gMap1,
			HashMap<String, GraphTemplate> gMap2) {
		// TODO Auto-generated method stub
		
	}
}
