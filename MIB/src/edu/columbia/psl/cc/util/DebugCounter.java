package edu.columbia.psl.cc.util;

import java.io.File;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;

public class DebugCounter {
	
	public static void main(String[] args) {
		String path = "/Users/mikefhsu/ccws/jvm-clones/MIB/test/cc.expbase.ChangeNode:changeList:0:0:2.json";
		File f = new File(path);
		TypeToken<GraphTemplate> gToken = new TypeToken<GraphTemplate>(){};
		
		GraphTemplate g = GsonManager.readJsonGeneric(f, gToken);
		System.out.println("Vertex num: " + g.getInstPool().size());
		
		int eNum = 0;
		for (InstNode i: g.getInstPool()) {
			eNum += i.getChildFreqMap().size();
		}
		System.out.println("Edge num: " + eNum);
	}

}
