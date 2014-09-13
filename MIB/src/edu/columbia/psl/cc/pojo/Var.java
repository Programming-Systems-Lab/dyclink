package edu.columbia.psl.cc.pojo;

import java.util.HashMap;
import java.util.Set;

public abstract class Var {
	
	//The class that this var is used
	private String className;
	
	//The method this var is used
	private String methodName;
	
	//static, instance or local
	protected int silId;
	
	private int opcode;
	
	private HashMap<Var, String> children = new HashMap<Var, String>();
	
	public void setOpcode(int opcode) {
		this.opcode = opcode;
	}
	
	public int getOpcode() {
		return this.opcode;
	}
	
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
	
	public int getSilId() {
		return this.silId;
	};
	
	public String getSil() {
		if (this.silId == 0) {
			return "static";
		} else if (this.silId == 1) {
			return "instance";
		} else {
			return "local";
		}
	}
	
	public void addChildren(Var child) {
		String edge = this.getSil() + "->" + child.getSil();
		this.children.put(child, edge);
	}
	
	public HashMap<Var, String> getChildrenWithLabel() {
		return this.children;
	}
	
	public Set<Var> getChildren() {
		return this.children.keySet();
	}
		
	public abstract String getVarInfo();
	
	@Override
	public String toString() {
		return this.opcode + ":" + this.className + ":" + this.methodName + ":" + this.getSil() + ":" + this.getVarInfo();
	}

}
