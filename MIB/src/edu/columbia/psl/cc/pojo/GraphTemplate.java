package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import edu.columbia.psl.cc.datastruct.InstPool;

public class GraphTemplate {
	
	private String methodKey;
	
	private long threadId;
	
	private int threadMethodId;
	
	private int methodArgSize;
	
	private int methodReturnSize;
	
	private boolean staticMethod;
	
	private List<InstNode> path;
	
	private InstPool pool;
	
	private int depNum;
	
	private HashSet<InstNode> firstReadLocalVars;
	
	private HashSet<InstNode> firstReadFields;
	
	private Map<String, InstNode> writeFields;
	
	private int maxTime;
	
	public GraphTemplate() {
		
	}
	
	public GraphTemplate(GraphTemplate copy) {
		this.methodKey = copy.getMethodKey();
		this.methodArgSize = copy.getMethodArgSize();
		this.methodReturnSize = copy.getMethodReturnSize();
		this.threadId = copy.getThreadId();
		this.threadMethodId = copy.getThreadMethodId();
		this.depNum = copy.getDepNum();
		this.staticMethod = copy.isStaticMethod();
		this.firstReadFields = new HashSet<InstNode>(copy.getFirstReadFields());
		this.firstReadLocalVars = new HashSet<InstNode>(copy.getFirstReadLocalVars());
		this.maxTime = copy.getMaxTime();
		
		this.pool = new InstPool();
		this.path = new ArrayList<InstNode>();
		for (InstNode inst: copy.getInstPool()) {
			InstNode copyInst = new InstNode(inst);
			this.pool.add(copyInst);
		}
		
		for (int i = 0; i < copy.getPath().size(); i++) {
			InstNode pathNode = copy.getPath().get(i);
			this.path.add(this.pool.searchAndGet(pathNode.getFromMethod(), pathNode.getThreadId(), pathNode.getThreadMethodIdx(), pathNode.getIdx()));
		}
		
		for (String field: copy.getWriteFields().keySet()) {
			InstNode copyNode = copy.getWriteFields().get(field);
			InstNode fieldNode = this.pool.searchAndGet(copyNode.getFromMethod(), copyNode.getThreadId(), copyNode.getThreadMethodIdx(), copyNode.getIdx());
			this.writeFields.put(field, fieldNode);
		}
	}
		
	public void setMethodKey(String methodKey) {
		this.methodKey = methodKey;
	}
	
	public String getMethodKey() {
		return this.methodKey;
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
	
	public void setDepNum(int depNum) {
		this.depNum = depNum;
	}
	
	public int getDepNum() {
		return this.depNum;
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
	
	public void setFirstReadLocalVars(HashSet<InstNode> firstReadLocalVars) {
		this.firstReadLocalVars = firstReadLocalVars;
	}
	
	public HashSet<InstNode> getFirstReadLocalVars() {
		return this.firstReadLocalVars;
	}
	
	public void setFirstReadFields(HashSet<InstNode> firstReadFields) {
		this.firstReadFields = firstReadFields;
	}
	
	public HashSet<InstNode> getFirstReadFields() {
		return this.firstReadFields;
	}
	
	public void setWriteFields(Map<String, InstNode> writeFields) {
		this.writeFields = writeFields;
	}
	
	public Map<String, InstNode> getWriteFields() {
		return writeFields;
	}
	
	public void setThreadId(long threadId) {
		this.threadId = threadId;
	}
	
	public long getThreadId() {
		return this.threadId;
	}
	
	public void setThreadMethodId(int threadMethodId) {
		this.threadMethodId = threadMethodId;
	}
	
	public int getThreadMethodId() {
		return this.threadMethodId;
	}
	
	public void setMaxTime(int maxTime) {
		this.maxTime = maxTime;
	}
	
	public int getMaxTime() {
		return this.maxTime;
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
