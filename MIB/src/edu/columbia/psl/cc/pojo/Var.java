package edu.columbia.psl.cc.pojo;

public class Var {
	
	private String className;
	
	private String methodName;
	
	//static, instance or local
	private String sil;
	
	private String varInfo;
	
	public void setClassName(String className) {
		this.className = className;
	}
	
	public String getClassName() {
		return this.className;
	}
	
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	
	public String getMethodName() {
		return this.methodName;
	}
	
	public void setSil(String sil) {
		this.sil = sil;
	}
	
	public String getSil() {
		return this.sil;
	}
		
	public void setVarInfo(String varInfo) {
		this.varInfo = varInfo;
	}
	
	public String getVarInfo() {
		return this.varInfo;
	}
	
	@Override
	public String toString() {
		return this.className + ":" + this.methodName + ":" + this.sil + ":" + this.varInfo;
	}

}
