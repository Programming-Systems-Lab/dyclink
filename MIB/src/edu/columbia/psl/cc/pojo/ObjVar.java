package edu.columbia.psl.cc.pojo;

public class ObjVar extends Var{
	
	private String nativeClassName;
	
	private String varName;
	
	//sil to indicate class or instance level var. 0 for class, 1 for instance
	public ObjVar(int sil) {
		this.silId = sil;
	}
	
	public void setNativeClassName(String nativeClassName) {
		this.nativeClassName = nativeClassName;
	}
	
	public String getNativeClassName() {
		return this.nativeClassName;
	}
	
	public void setVarName(String varName) {
		this.varName = varName;	
	}
	
	public String getVarName() {
		return this.varName;
	}
	
	public String getVarInfo() {
		return this.nativeClassName + ":" + this.varName;
	}
	

}
