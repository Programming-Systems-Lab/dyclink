package edu.columbia.psl.cc.pojo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

//For serialization purpose, cannot set Var as an abstract class
public class Var {
	
	//The class that this var is used
	private String className;
	
	//The method this var is used
	private String methodName;
	
	//static, instance or local
	protected int silId;
	
	//private int opcode;
	
	//private HashSet<Var> children = new HashSet<Var>();	
	private HashMap<String, Set<Var>> children = new HashMap<String, Set<Var>>();
	
	//For breaking infinite loop when serializing
	private HashMap<String, Set<String>> childrenRep = new HashMap<String ,Set<String>>();
	
	//For calculating stable marriage
	private boolean engaged = false;
	
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
		} else if (this.silId == 2){
			return "local";
		} else {
			return "fake";
		}
	}
	
	public void setEngaged(boolean engaged) {
		this.engaged = engaged;
	}
	
	public boolean isEngaged() {
		return this.engaged;
	}
	
	public void addChildren(Var child) {
		String edge = this.getSil() + "-" + child.getSil();
		//this.children.put(child, edge);
		if (this.children.keySet().contains(edge)) {
			this.children.get(edge).add(child);
		} else {
			Set<Var> edgeSet = new HashSet<Var>();
			edgeSet.add(child);
			this.children.put(edge, edgeSet);
		}
	}
	
	public Set<Var> getChildrenWithLabel(String label) {
		return this.children.get(label);
	}
	
	public void setChildren(HashMap<String, Set<Var>>children) {
		this.children = children;
	}
	
	public HashMap<String, Set<Var>> getChildren() {
		return this.children;
	}
	
	public void setChildrenRep(HashMap<String, Set<String>>childrenRep) {
		this.childrenRep = childrenRep;
	}
	
	public HashMap<String, Set<String>> getChildrenRep() {
		return this.childrenRep;
	}
	
	public HashSet<Var> getAll() {
		HashSet<Var> ret = new HashSet<Var>();
		for (String label: this.children.keySet()) {
			ret.addAll(this.children.get(label));
		}
		return ret;
	}
		
	public String getVarInfo() {
		if (this.silId < 2) {
			ObjVar ov = (ObjVar)this;
			return ov.getNativeClassName() + ":" + ov.getVarName();
		} else if (this.silId == 2){
			LocalVar lv = (LocalVar)this;
			StringBuilder sb = new StringBuilder();
			sb.append(lv.getLocalVarId());
			
			if (lv.getIntervals().size() > 0) {
				for (LabelInterval interval: lv.getIntervals()) {
					sb.append(interval + "&");
				}
				return sb.toString().substring(0, sb.length() - 1);
			} else {
				return sb.toString();
			}
		} else {
			FakeVar fv = (FakeVar)this;
			return String.valueOf(fv.getFakeId());
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Var))
			return false;
		
		Var tmpV = (Var)o;
		
		if (!tmpV.getClassName().equals(this.className))
			return false;
		
		if (!tmpV.getMethodName().equals(this.methodName))
			return false;
		
		if (tmpV.getSilId() != this.silId)
			return false;
		
		if (!tmpV.getVarInfo().equals(this.getVarInfo()))
			return false;
		
		//Consider children?
		
		return true;
	}
	
	@Override
	public String toString() {
		return this.className + ":" + this.methodName + ":" + this.getSilId() + ":" + this.getVarInfo();
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

}
