package edu.columbia.psl.cc.pojo;

import java.util.HashSet;
import java.util.Set;

//For serialization purpose, cannot set Var as an abstract class
public class Var {
	
	//The class that this var is used
	private String className;
	
	//The method this var is used
	private String methodName;
	
	//static, instance or local
	protected int silId;
	
	private int opcode;
	
	//private HashMap<Var, String> children = new HashMap<Var, String>();
	private HashSet<Var> children = new HashSet<Var>();
	
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
	
	public void setSilId(int silId) {
		this.silId = silId;
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
		//String edge = this.getSil() + "->" + child.getSil();
		//this.children.put(child, edge);
		this.children.add(child);
	}
	
	/*public HashMap<Var, String> getChildrenWithLabel() {
		return this.children;
	}*/
	
	public void setChildren(HashSet<Var> children) {
		this.children = children;
	}
	
	public Set<Var> getChildren() {
		//return this.children.keySet();
		return this.children;
	}
		
	public String getVarInfo() {
		if (this.silId < 2) {
			ObjVar ov = (ObjVar)this;
			return ov.getNativeClassName() + ":" + ov.getVarName();
		} else {
			LocalVar lv = (LocalVar)this;
			return String.valueOf(lv.getLocalVarId());
		}
	}
	
	@Override
	public String toString() {
		return this.opcode + ":" + this.className + ":" + this.methodName + ":" + this.getSil() + ":" + this.getVarInfo();
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

}
