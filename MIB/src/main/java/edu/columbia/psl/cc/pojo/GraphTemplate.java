package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.util.TraceAnalyzer.MethodTrace;

public class GraphTemplate {
	
	private String methodKey;
	
	private String shortMethodKey;
	
	private String methodName;
	
	private String methodDesc;
	
	private int threadId;
	
	private int threadMethodId;
	
	private int objId;
	
	private int methodArgSize;
	
	private int methodReturnSize;
	
	private boolean staticMethod;
	
	private List<InstNode> path;
	
	private InstPool pool;
	
	private InstNode lastBeforeReturn;
	
	private int edgeNum;
	
	private int vertexNum;
	
	private HashSet<String> firstReadLocalVars;
			
	private int childDominant;
	
	public transient HashMap<String, GraphTemplate> calleeRequired;
	
	public transient MethodTrace methodTrace;
	
	public transient HashMap<String, HashSet<String>> fieldRelations;
	
	public transient boolean mustExist = false;
			
	public GraphTemplate() {
		
	}
	
	public GraphTemplate(GraphTemplate copy) {
		this.methodKey = copy.getMethodKey();
		this.shortMethodKey = copy.getShortMethodKey();
		this.methodArgSize = copy.getMethodArgSize();
		this.methodReturnSize = copy.getMethodReturnSize();
		this.threadId = copy.getThreadId();
		this.threadMethodId = copy.getThreadMethodId();
		this.edgeNum = copy.getEdgeNum();
		this.staticMethod = copy.isStaticMethod();
		this.firstReadLocalVars = new HashSet<String>(copy.getFirstReadLocalVars());
		//this.latestWriteFields = new HashMap<String, String>(copy.getLatestWriteFields());
		
		this.pool = new InstPool();
		this.path = new ArrayList<InstNode>();
		for (InstNode inst: copy.getInstPool()) {
			InstNode copyInst = new InstNode(inst);
			this.pool.add(copyInst);
		}
		
		/*for (int i = 0; i < copy.getPath().size(); i++) {
			InstNode pathNode = copy.getPath().get(i);
			this.path.add(this.pool.searchAndGet(pathNode.getFromMethod(), pathNode.getThreadId(), pathNode.getThreadMethodIdx(), pathNode.getIdx()));
		}*/
		
		/*for (String field: copy.getWriteFields().keySet()) {
			InstNode copyNode = copy.getWriteFields().get(field);
			InstNode fieldNode = this.pool.searchAndGet(copyNode.getFromMethod(), copyNode.getThreadId(), copyNode.getThreadMethodIdx(), copyNode.getIdx());
			this.writeFields.put(field, fieldNode);
		}*/
	}
		
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
	
	public void setLastBeforeReturn(InstNode lastBeforeReturn) {
		this.lastBeforeReturn = lastBeforeReturn;
	}
	
	public InstNode getLastBeforeReturn() {
		return this.lastBeforeReturn;
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
	
	/*public void setLatestWriteFields(HashMap<String, String> latestWriteFields) {
		this.latestWriteFields = latestWriteFields;
	}
	
	public HashMap<String, String> getLatestWriteFields() {
		return this.latestWriteFields;
	}*/
		
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
	
	/*public void setDist(double[] dist) {
		this.dist = dist;
	}
	
	public double[] getDist() {
		return this.dist;
	}*/
	
	public void showGraph() {
		for (InstNode inst: this.pool) {
			System.out.println("Parent inst: " + inst);
			
			for (String cInst: inst.getChildFreqMap().navigableKeySet()) {
				System.out.println(" " + cInst + " " + inst.getChildFreqMap().get(cInst));
			}
		}
	}

}
