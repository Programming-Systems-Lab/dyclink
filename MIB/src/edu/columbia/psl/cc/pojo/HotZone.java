package edu.columbia.psl.cc.pojo;

import edu.columbia.psl.cc.datastruct.InstPool;

public class HotZone {
	
	private InstNode subStart;
	
	private InstNode subCentroid;
	
	//private double subPgRank;
	
	private InstNode subEnd;
	
	private String subTrace;
	
	private InstNode startInst;
	
	private InstNode centralInst;
	
	private InstNode endInst;
	
	private String targetTrace;
	
	//private int levDist;
	
	private double similarity;
	
	private double instDistance;
	
	private InstPool subGraph;
	
	private InstPool segs;
	
	private String subGraphName;
	
	private String subGraphId;
	
	private String targetGraphName;
	
	private String targetGraphId;
	
	public InstNode getSubStart() {
		return this.subStart;
	}
	
	public void setSubStart(InstNode subStart) {
		this.subStart = subStart;
	}
	
	public InstNode getSubCentroid() {
		return this.subCentroid;
	}
	
	public void setSubCentroid(InstNode subCentroid) {
		this.subCentroid = subCentroid;
	}
	
	public InstNode getSubEnd() {
		return this.subEnd;
	}
	
	public void setSubEnd(InstNode subEnd) {
		this.subEnd = subEnd;
	}
	
	public String getSubTrace() {
		return this.subTrace;
	}
	
	public void setSubTrace(String subTrace) {
		this.subTrace = subTrace;
	}
	
	/*public double getSubPgRank() {
		return this.subPgRank;
	}
	
	public void setSubPgRank(double subPgRank) {
		this.subPgRank = subPgRank;
	}*/

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
	
	public String getTargetTrace() {
		return this.targetTrace;
	}
	
	public void setTargetTrace(String targetTrace) {
		this.targetTrace = targetTrace;
	}

	/*public int getLevDist() {
		return levDist;
	}

	public void setLevDist(int levDist) {
		this.levDist = levDist;
	}*/

	public double getSimilarity() {
		return similarity;
	}

	public void setSimilarity(double similarity) {
		this.similarity = similarity;
	}
	
	public double getInstDistance() {
		return this.instDistance;
	}
	
	public void setInstDistance(double instDistance) {
		this.instDistance = instDistance;
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
	
	public void setSubGraphName(String subGraphName) {
		this.subGraphName = subGraphName;
	}
	
	public String getSubGraphName() {
		return this.subGraphName;
	}
	
	public void setSubGraphId(String subGraphId) {
		this.subGraphId = subGraphId;
	}
	
	public String getSubGraphId() {
		return this.subGraphId;
	}
	
	public void setTargetGraphName(String targetGraphName) {
		this.targetGraphName = targetGraphName;
	}
	
	public String getTargetGraphName() {
		return this.targetGraphName;
	}
	
	public void setTargetGraphId(String targetGraphId) {
		this.targetGraphId = targetGraphId;
	}
	
	public String getTargetGraphId() {
		return this.targetGraphId;
	}
}
