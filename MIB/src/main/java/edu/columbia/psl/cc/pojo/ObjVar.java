package edu.columbia.psl.cc.pojo;

public class ObjVar extends Var{
	
	private String nativeClassName;
	
	private String varName;
		
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
}
