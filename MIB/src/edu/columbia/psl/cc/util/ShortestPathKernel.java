package edu.columbia.psl.cc.util;

import java.util.ArrayList;
import java.util.Set;

import edu.columbia.psl.cc.datastruct.VarPool;
import edu.columbia.psl.cc.pojo.Var;

public class ShortestPathKernel {
	
	private static int limit = (int)1E3;
	
	public static VarPool addFakeVar(VarPool smallPool, int diff) {
		VarPool ret = new VarPool(smallPool);
		for (int i = 0; i < diff; i++) {
			ret.searchVar(null, null, 3, null);
		}
		return ret;
	}
	
	private int kernelMethod(int x1, int x2) {
		return x1 * x2;
	}
	
	public int scoreShortestPaths(int[][]g1, int[][]g2) {
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
		return total;
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
					costTable[i][j] = limit;
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
		
		System.out.println("Score kernel: " + spk.scoreShortestPaths(costTable1, costTable2));
		System.out.println("Score kernel: " + spk.scoreShortestPaths(costTable2, costTable3));
		System.out.println("Score kernel: " + spk.scoreShortestPaths(costTable1, costTable3));
	}
}
