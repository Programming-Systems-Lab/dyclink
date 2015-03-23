package edu.columbia.psl.cc.analysis;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.pojo.InstNode;


public class PercentageSelector {
	
	public static List<InstNode> selectImportantInsts(List<InstWrapper> instsWithPg, double percentThresh) {
		double curSum = 0;
		List<InstNode> selected = new ArrayList<InstNode>();
		
		for (InstWrapper iw: instsWithPg) {
			selected.add(iw.inst);
			curSum += iw.pageRank;
			
			if (curSum >= percentThresh)
				break;
		}
		
		return selected;
	}
	
	public static List<InstWrapper> selectImportantInstWrappers(List<InstWrapper> instsWithPg, double percentThresh) {
		double curSum = 0;
		int instNum = (int)(instsWithPg.size() * percentThresh);
		List<InstWrapper> selected = new ArrayList<InstWrapper>();
		
		for (int i = 0; i < instNum; i++) {
			InstWrapper iw = instsWithPg.get(i);
			selected.add(iw);
			curSum += iw.pageRank;
		}
		
		return selected;
	}
	
	public static double[] generateImportantDistribution(List<InstNode> selected) {
		int simStrategy = MIBConfiguration.SUBSUB_STRAT;
		double[] subsubDistribution = StaticTester.genDistribution(selected, simStrategy);
		double[] normalizedDistribution = StaticTester.normalizeDist(subsubDistribution, selected.size());
		return normalizedDistribution;
	}

}
