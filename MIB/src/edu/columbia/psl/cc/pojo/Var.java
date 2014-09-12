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
	
	public void setSil(int silId) {
		if (silId == 0) {
			this.sil = "static";
		} else if (silId == 1){
			this.sil = "instance";
 		} else if (silId == 2) {
 			this.sil = "local";
 		} else {
 			System.err.println("No such sil ID");
 		}
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
