package edu.columbia.psl.cc.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.pojo.OpcodeObj;

public class GraphletGenerator {
	
	private static HashSet<Integer> excludeOpCat = getExcludeOpCat();
	
	private HashMap<Integer, HashSet<OpGL>> graphletRepo = new HashMap<Integer, HashSet<OpGL>>();
	
	public static HashSet<Integer> getExcludeOpCat() {
		HashSet<Integer> excludeSet = new HashSet<Integer>();
		//remove undeterminate ops like, dup, swap and method calls;
		excludeSet.add(6);
		excludeSet.add(7);
		excludeSet.add(16);
		return excludeSet;
	}
	
	public HashMap<Integer, HashSet<OpGL>> getGraphletRepo() {
		return this.graphletRepo;
	}
	
	private void updateGraphletRepo(int vNum, OpGL opGL) {
		if (this.graphletRepo.containsKey(vNum)) {
			this.graphletRepo.get(vNum).add(opGL);
		} else {
			HashSet<OpGL> graphletSet = new HashSet<OpGL>();
			graphletSet.add(opGL);
			this.graphletRepo.put(vNum, graphletSet);
		}
	}
	
	public void genTwoSet(ArrayList<OpcodeObj> sortedOps) {
		System.out.println("Generate two sets");
		for (int i = 0; i < sortedOps.size(); i++) {
			OpcodeObj o1 = sortedOps.get(i);
			if (excludeOpCat.contains(o1.getCatId()) 
					|| o1.getOutList().size() == 0
					|| BytecodeCategory.returnOps().contains(o1.getOpcode())) {
				continue ;
			}
			for (int j = 0; j < sortedOps.size(); j++) {
				if (i == j)
					continue ;
				
				OpcodeObj o2 = sortedOps.get(j);
				if (o1.getOutList().size() != o2.getInList().size())
					continue ;
				
				if (excludeOpCat.contains(o2.getCatId()))
					continue ;
				
				if (o1.getOutList().equals(o2.getInList())) {
					OpGL graphlet = new OpGL();
					int op1 = o1.getOpcode();
					int op2 = o2.getOpcode();
					int[] opIdx = {op1, op2};
					int[][] adjMatrix = {{0, 1}, {0, 0}};
					graphlet.setOpIdx(opIdx);
					graphlet.setAdjMatrix(adjMatrix);
					this.updateGraphletRepo(2, graphlet);
				}
			}
		}
	}
	
	public void genThreeSet(ArrayList<OpcodeObj> sortedOps) {
		System.out.println("Generate three sets");
		for (int i = 0; i < sortedOps.size(); i++) {
			OpcodeObj o1 = sortedOps.get(i);
			if (excludeOpCat.contains(o1.getCatId()))
				continue ;
			for (int j = 0; j < sortedOps.size(); j++) {
				OpcodeObj o2 = sortedOps.get(j);
				if (excludeOpCat.contains(o2.getCatId()))
					continue ;
				for (int k = 0; k < sortedOps.size(); k++) {
					if (i == j && j == k)
						continue ;
					
					OpcodeObj o3 = sortedOps.get(k);
					
					if (excludeOpCat.contains(o3.getCatId()))
						continue ;
					
					OpGL graphlet = new OpGL();
					/*int op1 = o1.getOpcode();
					int op2 = o2.getOpcode();
					int op3 = o3.getOpcode();*/
					int op1 = o1.getCatId();
					int op2 = o2.getCatId();
					int op3 = o3.getCatId();
					int[] opIdx = {op1, op2, op3};
					int[][] adjMatrix = new int[3][3];
					graphlet.setOpIdx(opIdx);
					graphlet.setAdjMatrix(adjMatrix);
					
					if (o1.getOutList().size() != 0 
							&& o2.getOutList().size() != 0
							&& o1.getOutList().equals(o2.getInList()) 
							&& o2.getOutList().equals(o3.getInList())) {
						//1->2->3
						adjMatrix[0][1] = 1; 
						adjMatrix[1][2] = 1;
						this.updateGraphletRepo(3, graphlet);
					} else if (o1.getOutList().size() != 0
							&& o3.getOutList().size() != 0
							&& (o2.getInList().size() == o1.getOutList().size() + o3.getOutList().size())){
						//1->2<-3
						adjMatrix[0][1] = 1;
						adjMatrix[2][1] = 1;
						ArrayList<String> totalOut = new ArrayList<String>();
						totalOut.addAll(o1.getOutList());
						totalOut.addAll(o3.getOutList());
						
						if (o2.getInList().equals(totalOut))
							this.updateGraphletRepo(3, graphlet);
						else if (o1.getOutList().equals(o2.getInList()) 
								&& o3.getOutList().equals(o2.getInList()) 
								&& (BytecodeCategory.writeCategory().contains(o1.getCatId()) || BytecodeCategory.writeCategory().contains(o3.getCatId()) || BytecodeCategory.writeFieldCategory().contains(o1.getCatId()) || BytecodeCategory.writeFieldCategory().contains(o3.getCatId()))) {
							this.updateGraphletRepo(3, graphlet);
						}
					} else if ((BytecodeCategory.writeCategory().contains(o2.getCatId()) 
							|| o2.getOpcode() == 132)) {
						//1<-2->3, only possible when 2 is a write, and 1 3 are read
						if ((BytecodeCategory.readCategory().contains(o1.getCatId()) || o1.getOpcode() == 132) 
								&& (BytecodeCategory.readCategory().contains(o3.getCatId()) || o3.getOpcode() == 132)) {
							adjMatrix[1][0] = 1;
							adjMatrix[1][2] = 1;
							this.updateGraphletRepo(3, graphlet);
						}
					} else if (BytecodeCategory.writeFieldCategory().contains(o2.getCatId())) {
						if (BytecodeCategory.readFieldCategory().contains(o1.getCatId()) 
								&& BytecodeCategory.readFieldCategory().contains(o3.getCatId())) {
							adjMatrix[1][0] = 1;
							adjMatrix[1][2] = 1;
							this.updateGraphletRepo(3, graphlet);
						}
					} else if (o1.getOutList().size() != 0 
							&& o2.getOutList().size() != 0
							&& o3.getOutList().size() != 0
							&& o1.getOutList().equals(o2.getInList()) 
							&& o2.getOutList().equals(o3.getInList()) 
							&& o3.getOutList().equals(o1.getInList())) {
						//1->2, 2->3, 3->1
						adjMatrix[0][1] = 1;
						adjMatrix[1][2] = 1;
						adjMatrix[2][0] = 1;
						this.updateGraphletRepo(3, graphlet);
					}
					//1->2, 1->3, 3->2 is an impossible graphlet
				}
			}
		}
	}
	
	/**
	 * Too many four-node templates, 199.
	 * We also need the label for each node, so impossible to include all.
	 * Just include 1 graphlet for xastore
	 * @param sortedOps
	 */
	public void genFourSet(ArrayList<OpcodeObj> sortedOps) {
		System.out.println("Genreate four sets");
		for (int i = 0; i < sortedOps.size(); i++) {
			OpcodeObj o1 = sortedOps.get(i);
			
			if (excludeOpCat.contains(o1.getCatId()))
				continue ;
			for (int j = 0; j< sortedOps.size(); j++) {
				OpcodeObj o2 = sortedOps.get(j);
				
				if (excludeOpCat.contains(o2.getCatId()))
					continue ;
				for (int k = 0; k < sortedOps.size(); k++) {
					OpcodeObj o3 = sortedOps.get(k);
					
					if (excludeOpCat.contains(o3.getCatId()))
						continue ;
					for (int l = 0; l < sortedOps.size(); l++) {
						OpcodeObj o4 = sortedOps.get(l);
						
						if (excludeOpCat.contains(o4.getCatId()))
							continue ;
						
						if (i == j && i == k && i== l)
							continue ;
						
						OpGL graphlet = new OpGL();
						int op1 = o1.getCatId();
						int op2 = o2.getCatId();
						int op3 = o3.getCatId();
						int op4 = o4.getCatId();
						int[] opIdx = {op1, op2, op3, op4};
						int[][] adjMatrix = new int[4][4];
						graphlet.setOpIdx(opIdx);
						graphlet.setAdjMatrix(adjMatrix);
						
						if (o1.getOutList().equals(o4.getInList()) 
								&& o2.getOutList().equals(o4.getInList()) 
								&& o3.getOutList().equals(o3.getOutList())) {
							//1->4, 2->4, 3->4
							adjMatrix[0][3] = 1;
							adjMatrix[1][3] = 1;
							adjMatrix[2][3] = 1;
							this.updateGraphletRepo(4, graphlet);
						}
					}
				}
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		HashMap<Integer, OpcodeObj> opcodeTable = BytecodeCategory.getOpcodeTable();
		
		//Sort the map
		Comparator<OpcodeObj> opComp = new Comparator<OpcodeObj>() {
			public int compare(OpcodeObj o1, OpcodeObj o2) {
				return o1.getOpcode() > o2.getOpcode()?1:(o1.getOpcode() < o2.getOpcode())?-1:0;
			}
		};
		
		ArrayList<OpcodeObj> opcodes = new ArrayList<OpcodeObj>(opcodeTable.values());
		Collections.sort(opcodes, opComp);
		
		GraphletGenerator gg = new GraphletGenerator();
		//gg.genTwoSet(opcodes);
		gg.genThreeSet(opcodes);
		//gg.genFourSet(opcodes);
		
		HashMap<Integer, HashSet<OpGL>> graphletRepo = gg.getGraphletRepo();
		int count = 0;
		StringBuilder sb = new StringBuilder();
		for (Integer i: graphletRepo.keySet()) {
			HashSet<OpGL> graphlets = graphletRepo.get(i);
			count += graphlets.size();
			
			System.out.println("Node number: " + i);
			for (OpGL g: graphlets) {
				System.out.println(g);
				sb.append(g);
			}
		}
		System.out.println("Total graphlet number: " + count);
		
		File f = new File("./graphlet/graphlets.txt");
		if (f.exists())
			f.delete();
		
		try {
			BufferedWriter bw  = new BufferedWriter(new FileWriter(f));
			bw.write(sb.toString());
			bw.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static class OpGL {
		
		private int[] opIdx;
		
		private int[][] adjMatrix;
		
		public int getSize() {
			return this.adjMatrix.length;
		}
		
		public void setOpIdx(int[] opIdx) {
			this.opIdx = opIdx;
		}
		
		public int[] getOpIdx() {
			return opIdx;
		}
		
		public void setAdjMatrix(int[][] adjMatrix) {
			this.adjMatrix = adjMatrix;
		}
		
		public int[][] getAdjMatrix() {
			return this.adjMatrix;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof OpGL))
				return false;
			
			OpGL tmp = (OpGL)obj;
			if (!Arrays.equals(this.getOpIdx(), tmp.getOpIdx()))
				return false;
			
			for (int i = 0; i < tmp.getAdjMatrix().length; i++) {
				if (!Arrays.equals(tmp.getAdjMatrix()[i], tmp.getAdjMatrix()[i]))
					return false;
			}
			return true;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("Opcodes: " + Arrays.toString(this.opIdx) + "\n");
			sb.append("AdjMatrix: " + "\n");
			for (int i = 0; i < adjMatrix.length; i++) {
				sb.append(Arrays.toString(adjMatrix[i]) + "\n");
			}
			return sb.toString();
		}
		
		@Override
		public int hashCode() {
			return this.toString().hashCode();
		}
	}
}
