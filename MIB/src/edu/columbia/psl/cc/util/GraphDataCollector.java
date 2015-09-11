package edu.columbia.psl.cc.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.pojo.GraphTemplate;

public class GraphDataCollector {
	
	public static void findMaxGraph(String path) {
		File graphrepo = new File(path);
		TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
		
		HashMap<String, GraphTemplate> allGraphs = TemplateLoader.loadTemplate(graphrepo, graphToken);
		int maxFreq = Integer.MIN_VALUE;
		GraphTemplate maxGraph = null;
		HashSet<String> uniqueMethod = new HashSet<String>();
		for (GraphTemplate gt: allGraphs.values()) {
			if (gt.getVertexNum() > maxFreq) {
				maxFreq = gt.getVertexNum();
				maxGraph = gt;
			}
			uniqueMethod.add(gt.getShortMethodKey());
		}
		System.out.println("Method num: " + uniqueMethod.size());
		System.out.println("Max graph: " + maxGraph.getMethodName() + " " + maxGraph.getVertexNum() + " " + maxGraph.getEdgeNum());
	}
	
	public static void load(String path, List<File> recorder) {
		File f = new File(path);
		
		for (File child: f.listFiles()) {
			if (child.isDirectory()) {
				load(child.getAbsolutePath(), recorder);
			} else {
				if (child.getName().endsWith(".json")) {
					recorder.add(child);
				}
			}
		}
	}
	
	public static void main(String[] args) {
		Scanner s = new Scanner(System.in);
		
		System.out.println("Graph repo: ");
		String repo = s.nextLine();
		findMaxGraph(repo);
		
		/*System.out.println("Destination: ");
		String dest = s.nextLine();
		File destDir = new File(dest);
		
		System.out.println("Confirm repo: " + repo);
		List<File> recorder = new ArrayList<File>();
		load(repo, recorder);
		System.out.println("File number: " + recorder.size());
		for (File f: recorder) {
			//System.out.println(f.getAbsolutePath());
			//Copy file
			String destFileName = destDir.getAbsolutePath() + "/" + f.getName();
			
			try {
				File destFile = new File(destFileName);
				if (!destFile.exists()) {
					destFile.createNewFile();
				}
				
				FileChannel source = new FileInputStream(f).getChannel();
				FileChannel destination = new FileOutputStream(destFile).getChannel();
				destination.transferFrom(source, 0, source.size());
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}*/
	}

}
