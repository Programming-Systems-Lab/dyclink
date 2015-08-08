package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import edu.columbia.psl.cc.util.GlobalRecorder;
import edu.columbia.psl.cc.util.StringUtil;

public class MethodNode extends InstNode {
	
	private static Logger logger = Logger.getLogger(MethodNode.class);
	
	private static final double domPass = 0.8;
	
	public static final double EPSILON = Math.pow(10, -4);
	
	private CalleeInfo calleeInfo = new CalleeInfo();
	
	private HashMap<String, GraphWithFreq> callees = new HashMap<String, GraphWithFreq>();
		
	private RegularState rs = new RegularState();
	
	private int maxGraphFreq = 0;
	
	public static double roundHelper(double frac) {
		frac = frac * 100;
		frac = Math.round(frac);
		frac = frac/100;
		return frac;
	}
	
	public static void globalRWRemoveHelper(GraphTemplate callee) {
		HashMap<String, HashSet<String>> cRW = callee.fieldRelations;
		for (String w: cRW.keySet()) {
			HashSet<String> rs = cRW.get(w);
			
			for (String r: rs) {
				GlobalRecorder.removeHistory(w, r);
			}
		}
	}
	
	/**
	 * Filter out graphs whose frequency < (mean - std)
	 * @param mn
	 * @return
	 */
	public static HashMap<GraphTemplate, Double> extractCallee(MethodNode mn) {
		HashMap<String, GraphWithFreq> callees = mn.callees;
		int maxGraphFreq = mn.getMaxCalleeFreq();
		int instFreq = 0;
		if (mn.rs.count > 0) {
			instFreq = mn.rs.count;
		}
		
		/*logger.info("Callee graph original");
		for (String key: callees.keySet()) {
			logger.info(key+ " " + callees.get(key).freq); 
		}*/
		
		int totalFreq = 0;
		for (GraphWithFreq graF: callees.values()) {
			totalFreq += graF.freq;
		}
		totalFreq += instFreq;
		
		HashMap<GraphTemplate, Double> ret = new HashMap<GraphTemplate, Double>();
		if (maxGraphFreq >= domPass * totalFreq) {
			List<GraphWithFreq> toRemove = new ArrayList<GraphWithFreq>();
			boolean found = false ;
			for (GraphWithFreq graF: callees.values()) {
				if (graF.freq == maxGraphFreq) {
					ret.put(graF.callee, 1.0);
					found = true;
					//logger.info("Dominant callee graph: " + graF.callee.getThreadMethodId() + " " + graF.freq);
					//return ret;
				} else {
					toRemove.add(graF);
				}
			}
			
			for (GraphWithFreq removed: toRemove) {
				GraphTemplate callee = removed.callee;
				if (removed.callee.mustExist)
					continue ;
				
				//System.out.println("Because of dom: " + callee.getMethodName() + " " + callee.getThreadMethodId());
				globalRWRemoveHelper(callee);
			}
			
			if (found) {
				return ret;
			} else {
				logger.error("Cannot find graph with matched freq");
				return null;
			}
		}
		
		double meanFreq = 0;
		double stdSum = 0;
		double std = 0;
		
		if (instFreq != 0) {
			meanFreq = ((double)totalFreq)/(callees.values().size() + 1);
		} else {
			meanFreq = ((double)totalFreq)/callees.values().size();
		}
		
		for (GraphWithFreq graF: callees.values()) {
			stdSum += Math.pow(graF.freq - meanFreq, 2);
		}
		if (instFreq != 0) {
			stdSum += Math.pow(instFreq - meanFreq, 2);
		}
		
		if (instFreq != 0) {
			std = Math.sqrt((stdSum)/(callees.values().size()));
		} else {
			std = Math.sqrt((stdSum)/(callees.values().size() - 1));
		}
		
		double lowBound = meanFreq - std;
		
		/*double meanFreq = ((double)totalFreq)/callees.values().size();
		double stdSum = 0;
		for (GraphWithFreq graF: callees.values()) {
			stdSum += Math.pow(graF.freq - meanFreq, 2);
		}
		double std = Math.sqrt((stdSum) / (callees.values().size() - 1));
		double lowBound = meanFreq - std;*/
		
		List<GraphWithFreq> cache = new ArrayList<GraphWithFreq>();
		int newTotal = 0;
		for (GraphWithFreq graF: callees.values()) {
			if (graF.freq >= lowBound) {
				cache.add(graF);
				newTotal += graF.freq;
			}
		}
		
		//Summarize the fraction here.
		if (mn.rs.count > 0) {
			newTotal += mn.rs.count;
			mn.rs.instFrac = ((double)mn.rs.count)/newTotal;
		}
		
		//logger.info("Callee graph filtering: " + mn.getFromMethod());
		for (int i = 0; i < cache.size(); i++) {
			GraphWithFreq graF = cache.get(i);
			
			double frac = ((double)graF.freq)/newTotal;;
			frac = roundHelper(frac);
			
			double diff = Math.abs(frac - 0);
			if (diff > EPSILON) {
				ret.put(graF.callee, frac);
				//logger.info(graF.callee.getVertexNum() + ":" + graF.callee.getEdgeNum() + " " + frac);
			} else if (!graF.callee.mustExist){
				//System.out.println("Because of frac: " + graF.callee.getMethodName() + " " + graF.callee.getThreadMethodId());
				globalRWRemoveHelper(graF.callee);
			}
		}
		return ret;
	}
		
	public void setCalleeInfo(CalleeInfo calleeInfo) {
		this.calleeInfo = calleeInfo;
	}
	
	public CalleeInfo getCalleeInfo() {
		return this.calleeInfo;
	}
	
	public void setRegularState(RegularState rs) {
		this.rs = rs;
	}
	
	public RegularState getRegularState() {
		return this.rs;
	}
		
	public void registerParentReplay(int idx, InstNode instParent) {
		String parentString = StringUtil.genIdxKey(instParent.getThreadId(), 
				instParent.getThreadMethodIdx(), 
				instParent.getIdx());
		
		if (this.calleeInfo.parentReplay == null) {
			this.calleeInfo.parentReplay = new HashMap<Integer, HashSet<String>>();
		}
		
		if (this.calleeInfo.parentReplay.containsKey(idx)) {
			this.calleeInfo.parentReplay.get(idx).add(parentString);
		} else {
			HashSet<String> parentSet = new HashSet<String>();
			parentSet.add(parentString);
			this.calleeInfo.parentReplay.put(idx, parentSet);
		}
	}
		
	public void registerDomCalleeIdx(String domCalleeIdx, 
			double normFreq, 
			InstNode lastBeforeReturn) {
		//this.calleeInfo.domCalleeIdx = domCalleeIdx;
		MetaGraph mg = new MetaGraph();
		mg.calleeIdx = domCalleeIdx;
		mg.normFreq = normFreq;
		
		if (lastBeforeReturn != null) {
			String lastString = StringUtil.genIdxKey(lastBeforeReturn.getThreadId(), 
					lastBeforeReturn.getThreadMethodIdx(), 
					lastBeforeReturn.getIdx());
			mg.lastInstString = lastString;
		}
		
		if (this.calleeInfo.metaCallees == null) {
			this.calleeInfo.metaCallees = new ArrayList<MetaGraph>();
		}
		
		this.calleeInfo.metaCallees.add(mg);
	}
		
	public void registerCallee(GraphTemplate callee) {
		//No need for linenumber actually.
		String groupKey = GraphGroup.groupKey(this.getLinenumber(), callee);
		GraphWithFreq gf = null;
		if (this.callees.containsKey(groupKey)) {
			//Can update the update time inst here. 
			//Not helpful for current approach, so save some time.
			gf = this.callees.get(groupKey);
			gf.freq++;
			
			//The rw history of callee has been registered, need to remove
			globalRWRemoveHelper(callee);
			
			//Incre history freq
			HashMap<String, HashSet<String>> curRW = gf.callee.fieldRelations;
			for (String w: curRW.keySet()) {
				HashSet<String> rs = curRW.get(w);
				for (String r: rs) {
					GlobalRecorder.increHistoryFreq(w, r);
				}
			}
			
			//Remove write fields
			//HashMap<String, InstNode> cWriteFields = callee.writeFields;
			//GlobalRecorder.removeWriteFields(cWriteFields.keySet());
			
			//Insert the original relations back
			//GlobalRecorder.registerAllWriteFields(gf.callee.writeFields);
		} else {
			gf = new GraphWithFreq();
			gf.callee = callee;
			gf.freq = 1;
			this.callees.put(groupKey, gf);
		}
		
		if (gf.freq > this.maxGraphFreq) {
			this.maxGraphFreq = gf.freq;
		}
	}
	
	public void clearCallees() {
		this.callees.clear();
	}
		
	public HashMap<String, GraphWithFreq> getCallees() {
		return this.callees;
	}
	
	public int getMaxCalleeFreq() {
		return this.maxGraphFreq;
	}
	
	/**
	 * Record callee graph with frequency in runtime
	 * @author mikefhsu
	 *
	 */
	public static class GraphWithFreq {
		public GraphTemplate callee;
		
		public int freq = 0;
	}
	
	/**
	 * Record parent info and the necessary info. for callee graphs
	 * @author mikefhsu
	 *
	 */
	public static class CalleeInfo {
		
		public List<MetaGraph> metaCallees;
		
		public HashMap<Integer, HashSet<String>> parentReplay;
		
	}
	
	/**
	 * Required infor for callee graphs
	 * @author mikefhsu
	 *
	 */
	public static class MetaGraph {
		public String calleeIdx;
		
		public double normFreq;
		
		public String lastInstString;
	}
	
	public static class RegularState {
		public transient int count;
		
		public double instFrac;
		
		public long startTime;
		
		public long updateTime;
	}
}
