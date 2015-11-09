package edu.columbia.psl.cc.visual;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;
import java.util.Scanner;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.analysis.InstWrapper;
import edu.columbia.psl.cc.analysis.PageRankSelector;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.util.GraphConstructor;
import edu.columbia.psl.cc.util.GraphUtil;
import edu.columbia.psl.cc.util.GsonManager;
import edu.columbia.psl.cc.util.StringUtil;

public class GephiDataCoverter {
	
	private static String outputPath = "/Users/mikefhsu/Desktop/grant_graph/";
	
	private static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
	private static String nodeHeader = "Id,Label,Attribute,Weight\n";
	
	private static String edgeHeader = "Source,Target,Type,Weight\n";
	
	public static void main(String[] args) {
		String graphFile = null;
		
		System.out.println("Input graph file: ");
		Scanner s = new Scanner(System.in);
		graphFile = s.nextLine();
		File gFile = new File(graphFile);
		if (!gFile.exists()) {
			System.err.println("Invalid graph file");
			System.exit(-1);
		}
		System.out.println("Confirm graph file path: " + gFile.getAbsolutePath());
				
		System.out.println("Output data file: ");
		String outputFile = s.nextLine();
		String nodeOutput = outputPath + outputFile + "_node.csv";
		String edgeOutput = outputPath + outputFile + "_edge.csv";
		System.out.println("Confirm node output: " + nodeOutput);
		System.out.println("Confirm edge output: " + edgeOutput);
		
		GraphTemplate graphObj = GsonManager.readJsonGeneric(gFile, graphToken);
		GraphConstructor gc = new GraphConstructor();
		gc.reconstructGraph(graphObj, false);
		gc.cleanObjInit(graphObj);
		
		InstPool exportPool = null;
		
		System.out.println("Extract sub?");
		boolean extractSub = Boolean.valueOf(s.nextLine());
		if (extractSub) {
			System.out.println("Start instruction: ");
			String startInst = s.nextLine();
			
			System.out.println("Seg size: ");
			int segSize = Integer.valueOf(s.nextLine());
			
			List<InstNode> sorted = GraphUtil.sortInstPool(graphObj.getInstPool(), true);
			exportPool = new InstPool();
			
			boolean start = false;
			int counter = 0;
			for (InstNode inst: sorted) {
				if (inst.toString().equals(startInst)) {
					start = true;
				}
				
				if (start) {
					exportPool.add(inst);
					counter++;
				}
				
				/*if (inst.toString().equals(targetCentroid)) {
					isCentroid = true;
				}
				
				if (start && !isCentroid) {
					before++;
				}*/
				
				if (counter == segSize) {
					break;
				}
			}
			System.out.println("Extracted sub seg size: " + exportPool.size());
		} else {
			exportPool = graphObj.getInstPool();
		}
		
		PageRankSelector selector = new PageRankSelector(exportPool, true, true);
		List<InstWrapper> results = selector.computePageRank();
		
		StringBuilder nodeSb = new StringBuilder();
		nodeSb.append(nodeHeader);
		
		StringBuilder edgeSb = new StringBuilder();
		edgeSb.append(edgeHeader);
		for (InstWrapper iw: results) {
			InstNode inst = iw.inst;
			double weight = iw.pageRank;
			String id = StringUtil.genIdxKey(inst.getThreadId(), inst.getThreadMethodIdx(), inst.getIdx());
			String label = inst.getOp().getInstruction();
			String attribute = String.valueOf(inst.repOp);
			
			String nodeRaw = id + "," + label + "," + attribute + "," + weight + "\n";
			nodeSb.append(nodeRaw);
			
			for (String cId: inst.getChildFreqMap().keySet()) {
				if (exportPool.searchAndGet(cId) == null) {
					continue ;
				}
				
				String aEdge = id + "," + cId + ",directed" + "," + inst.getChildFreqMap().get(cId) + "\n";
				edgeSb.append(aEdge);
			}
		}
		
		try {
			BufferedWriter nodeWriter = new BufferedWriter(new FileWriter(nodeOutput));
			nodeWriter.write(nodeSb.toString());
			nodeWriter.close();
			
			BufferedWriter edgeWriter = new BufferedWriter(new FileWriter(edgeOutput));
			edgeWriter.write(edgeSb.toString());
			edgeWriter.close();
			
			System.out.println("Data output completes");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
