package edu.columbia.psl.cc.abs;

import java.util.HashMap;
import java.util.HashSet;

import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.GraphTemplate;

public abstract class AbstractGraph {
	
	protected String methodKey;
	
	protected String shortMethodKey;
	
	protected String methodName;
	
	protected String methodDesc;
	
	protected int threadId;
	
	protected int threadMethodId;
	
	protected int objId;
	
	protected int methodArgSize;
	
	protected int methodReturnSize;
	
	protected boolean staticMethod;
	
	protected InstPool pool;
	
	protected int edgeNum;
	
	protected int vertexNum;
	
	protected HashSet<String> firstReadLocalVars;
			
	protected int childDominant;
	
	public transient HashMap<String, AbstractGraph> calleeRequired;
				
	public void setMethodKey(String methodKey) {
		this.methodKey = methodKey;
	}
	
	public String getMethodKey() {
		return this.methodKey;
	}
	
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
	
	public String getMethodName() {
		return this.methodName;
	}
	
	public void setMethodDesc(String methodDesc) {
		this.methodDesc = methodDesc;
	}
	
	public String getMethodDesc() {
		return this.methodDesc;
	}
	
	public void setShortMethodKey(String shortMethodKey) {
		this.shortMethodKey = shortMethodKey;
	}
	
	public String getShortMethodKey() {
		return this.shortMethodKey;
	}
		
	public void setInstPool(InstPool pool) {
		this.pool = pool;
	}
	
	public InstPool getInstPool() {
		return this.pool;
	}
		
	public void setEdgeNum(int edgeNum) {
		this.edgeNum = edgeNum;
	}
	
	public int getEdgeNum() {
		return this.edgeNum;
	}
	
	public void setVertexNum(int vertexNum) {
		this.vertexNum = vertexNum;
	}
	
	public int getVertexNum() {
		return this.vertexNum;
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
	
	public void setStaticMethod(boolean staticMethod) {
		this.staticMethod = staticMethod;
	}
	
	public boolean isStaticMethod() {
		return this.staticMethod;
	}
	
	public void setFirstReadLocalVars(HashSet<String> firstReadLocalVars) {
		this.firstReadLocalVars = firstReadLocalVars;
	}
	
	public HashSet<String> getFirstReadLocalVars() {
		return this.firstReadLocalVars;
	}
		
	public void setChildDominant(int childDominant) {
		this.childDominant = childDominant;
	}
	
	public int getChildDominant() {
		return this.childDominant;
	}
		
	public void setThreadId(int threadId) {
		this.threadId = threadId;
	}
	
	public int getThreadId() {
		return this.threadId;
	}
	
	public void setThreadMethodId(int threadMethodId) {
		this.threadMethodId = threadMethodId;
	}
	
	public int getThreadMethodId() {
		return this.threadMethodId;
	}
	
	public void setObjId(int objId) {
		this.objId = objId;
	}
	
	public int getObjId() {
		return this.objId;
	}
}

