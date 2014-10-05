package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.columbia.psl.cc.datastruct.InstPool;

public class GraphTemplate {
	
	private String methodKey;
	
	private int methodArgSize;
	
	private int methodReturnSize;
	
	private List<Integer> extMethods;
		
	private InstNode lastSecondInst;
	
	private List<InstNode> path;
	
	private InstPool pool;
	
	public GraphTemplate() {
		
	}
	
	public GraphTemplate(GraphTemplate copy) {
		this.methodKey = copy.getMethodKey();
		this.methodArgSize = copy.getMethodArgSize();
		this.methodReturnSize = copy.getMethodReturnSize();
		this.extMethods = new ArrayList<Integer>(copy.getExtMethods());
		this.lastSecondInst = copy.getLastSecondInst();
		this.path = new ArrayList<InstNode>(copy.getPath());
		this.pool = new InstPool(copy.getInstPool());
	}
	
	public void setMethodKey(String methodKey) {
		this.methodKey = methodKey;
	}
	
	public String getMethodKey() {
		return this.methodKey;
	}
	
	public void setExtMethods(List<Integer> extMethods) {
		this.extMethods = extMethods;
	}
	
	public List<Integer> getExtMethods() {
		return this.extMethods;
	}
	
	public void setPath(List<InstNode> path) {
		this.path = path;
	}
	
	public List<InstNode> getPath() {
		return this.path;
	}
	
	public void setInstPool(InstPool pool) {
		this.pool = pool;
	}
	
	public InstPool getInstPool() {
		return this.pool;
	}
	
	public void setMethodArgSize(int methodArgSize) {
		this.methodArgSize = methodArgSize;
	}
	
	public int getMethodArgSize() {
		return this.methodArgSize;
	}
	
	public void setMethodReturnSize(int methodReturnSize) {
		this.methodReturnSize = methodReturnSize;
	} 
	
	public int getMethodReturnSize() {
		return this.methodReturnSize;
	}
 		
	public void setLastSecondInst(InstNode lastSecondInst) {
		this.lastSecondInst = lastSecondInst;
	}
	
	public InstNode getLastSecondInst() {
		return this.lastSecondInst;
	}

}
