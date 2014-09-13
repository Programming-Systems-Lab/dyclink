package edu.columbia.psl.cc.pojo;

public class LocalVar extends Var{
	
	private int localVarId;
	
	public LocalVar() {
		this.silId = 0;
	}
	
	public void setLocalVarId(int localVarId) {
		this.localVarId = localVarId;
	}
	
	public int getLocalVarId() {
		return this.localVarId;
	}
	
	public String getVarInfo() {
		return String.valueOf(this.localVarId);
	}

}
