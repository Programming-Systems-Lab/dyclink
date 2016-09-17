package edu.columbia.psl.cc.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;

public class CumuChecker {
	
	public static void main(String[] args) {
		TypeToken<GraphTemplate> token = new TypeToken<GraphTemplate>(){};
		
		File benchRepo = new File(args[0]);
		if (!benchRepo.exists() || benchRepo.isFile()) {
			System.err.println("Invalid benchmark path: " + benchRepo.getAbsolutePath());
			System.exit(-1);
		}
		
		InstPool allPool = new InstPool();
		int expected = 0;
		for (File f: benchRepo.listFiles()) {
			if (f.getName().endsWith(".json")) {
				System.out.println("Partial graphs: " + f.getAbsolutePath());
				
				GraphTemplate g = GsonManager.readJsonGeneric(f, token);
				expected += g.getVertexNum();
				allPool.addAll(g.getInstPool());
			}
		}
		
		System.out.println("Expected inst size: " + expected);
		System.out.println("Real size: " + allPool.size());
		
		Comparator<InstNode> sorter = new Comparator<InstNode>() {
			public int compare(InstNode i1, InstNode i2) {
				int c1 = i1.getChildFreqMap().size();
				int c2 = i2.getChildFreqMap().size();
				if (c1 < c2) {
					return 1;
				} else if (c1 > c2) {
					return -1;
				} else {
					return 0;
				}
			}
		};
		
		List<InstNode> sorted = new ArrayList<InstNode>();
		for (InstNode i: allPool) {
			int childCount = i.getChildFreqMap().size();
			if (childCount >= 10) {
				sorted.add(i);
			}
		}
		
		Collections.sort(sorted, sorter);
		System.out.println("# >10: " + sorted.size());
		Map<String, InstNode> forSearch = new HashMap<String, InstNode>();
		for (InstNode i: sorted) {
			System.out.println("High-degree instruction: " + i);
			System.out.println("Line number: " + i.getLinenumber());
			System.out.println("Child count: " + i.getChildFreqMap().size());
			
			forSearch.put(i.toString(), i);
		}
		
		while (true) {
			System.out.println("Query: ");
			Scanner scanner = new Scanner(System.in);
			String q = scanner.nextLine();
			InstNode i = forSearch.get(q);
			System.out.println("Inst: " + i);
			System.out.println("Line number: " + i.getLinenumber());
			for (String child: i.getChildFreqMap().keySet()) {
				System.out.println("Child c: " + child);
			}
		}
	}

}
