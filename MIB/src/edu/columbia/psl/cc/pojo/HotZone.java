package edu.columbia.psl.cc.pojo;

import edu.columbia.psl.cc.datastruct.InstPool;

public class HotZone {
	
	private InstNode startInst;
	
	private InstNode centralInst;
	
	private InstNode endInst;
	
	private double levDist;
	
	private double similarity;
	
	private InstPool subGraph;
	
	private InstPool segs;

	public InstNode getStartInst() {
		return startInst;
	}

	public void setStartInst(InstNode startInst) {
		this.startInst = startInst;
	}

	public InstNode getCentralInst() {
		return centralInst;
	}

	public void setCentralInst(InstNode centralInst) {
		this.centralInst = centralInst;
	}

	public InstNode getEndInst() {
		return endInst;
	}

	public void setEndInst(InstNode endInst) {
		this.endInst = endInst;
	}

	public double getLevDist() {
		return levDist;
	}

	public void setLevDist(double levDist) {
		this.levDist = levDist;
	}

	public double getSimilarity() {
		return similarity;
	}

	public void setSimilarity(double similarity) {
		this.similarity = similarity;
	}

	public InstPool getSubGraph() {
		return subGraph;
	}

	public void setSubGraph(InstPool subGraph) {
		this.subGraph = subGraph;
	}

	public InstPool getSegs() {
		return segs;
	}

	public void setSegs(InstPool segs) {
		this.segs = segs;
	}
}
