package edu.columbia.psl.cc.analysis;

import edu.columbia.psl.cc.datastruct.BytecodeCategory;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.util.SearchUtil;

public class InstWrapper {
	public InstNode inst;
	
	public double pageRank;
		
	public InstWrapper(InstNode inst, double pageRank) {
		this.inst = inst;
		this.pageRank = pageRank;
	}
}
