package edu.columbia.psl.cc.pojo;

public class FieldRecord {
	
	private InstNode writeInst;
	
	private InstNode readInst;
	
	private double freq;
	
	public void setWriteInst(InstNode writeInst) {
		this.writeInst = writeInst;
	}
	
	public InstNode getWriteInst() {
		return this.writeInst;
	}
	
	public void setReadInst(InstNode readInst) {
		this.readInst = readInst;
	}
	
	public InstNode getReadInst() {
		return this.readInst;
	}
	
	public void increFreq() {
		this.freq++;
	}
	
	public double getFreq() {
		return this.freq;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.writeInst.toString() + "-");
		sb.append(this.readInst.toString());
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object in) {
		if (!(in instanceof FieldRecord))
			return false;
		
		FieldRecord tmp = (FieldRecord)in;
		if (!tmp.getWriteInst().equals(this.writeInst)) {
			return false;
		}
		
		if (!tmp.getReadInst().equals(this.readInst))
			return false;
		
		return true;
	}

}
