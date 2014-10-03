package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.columbia.psl.cc.datastruct.InstPool;

public class GraphTemplate {
	
	private int methodArgSize;
	
	private int methodReturnSize;
		
	private InstNode lastSecondInst;
	
	private InstPool pool;
	
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
