package edu.columbia.psl.cc.pojo;

public class SurrogateInst extends InstNode{

	String relatedChildMethodInst;
	
	public SurrogateInst() {
		
	}
	
	public SurrogateInst(InstNode oriInst) {
		super(oriInst);
	}
	
	public void setRelatedChildMethodInst(String relatedChildMethodInst) {
		this.relatedChildMethodInst = relatedChildMethodInst;
	}
	
	public String getRelatedChildMethodInst() {
		return this.relatedChildMethodInst;
	}

}
