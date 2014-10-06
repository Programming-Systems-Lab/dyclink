package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import edu.columbia.psl.cc.datastruct.InstPool;

public class GraphTemplate {
	
	private String methodKey;
	
	private int methodArgSize;
	
	private int methodReturnSize;
	
	private Map<Integer, Integer> extMethods;
		
	private InstNode lastSecondInst;
	
	private List<InstNode> path;
	
	private InstPool pool;
	
	public GraphTemplate() {
		
	}
	
	public GraphTemplate(GraphTemplate copy) {
		this.methodKey = copy.getMethodKey();
		this.methodArgSize = copy.getMethodArgSize();
		this.methodReturnSize = copy.getMethodReturnSize();
		this.extMethods = new HashMap<Integer, Integer>(copy.getExtMethods());
		this.pool = new InstPool();
		this.path = new ArrayList<InstNode>();
		for (InstNode inst: copy.getInstPool()) {
			InstNode copyInst = new InstNode(inst);
			this.pool.add(copyInst);
		}
		for (int i = 0; i < copy.getPath().size(); i++) {
			InstNode pathNode = copy.getPath().get(i);
			this.path.add(this.pool.searchAndGet(pathNode.getFromMethod(), pathNode.getIdx()));
		}
		this.lastSecondInst = this.pool.searchAndGet(copy.getLastSecondInst().getFromMethod(), copy.getLastSecondInst().getIdx());
	}
		
	public void setMethodKey(String methodKey) {
		this.methodKey = methodKey;
	}
	
	public String getMethodKey() {
		return this.methodKey;
	}
	
	public void setExtMethods(Map<Integer, Integer> extMethods) {
		this.extMethods = extMethods;
	}
	
	public Map<Integer, Integer> getExtMethods() {
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
	
	public void showGraph() {
		for (InstNode inst: this.pool) {
			System.out.println("Parent inst: " + inst);
			
			for (String cInst: inst.getChildFreqMap().navigableKeySet()) {
				System.out.println(" " + cInst + " " + inst.getChildFreqMap().get(cInst));
			}
		}
	}

}
