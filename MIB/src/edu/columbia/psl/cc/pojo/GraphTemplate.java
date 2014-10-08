package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import edu.columbia.psl.cc.datastruct.InstPool;

public class GraphTemplate {
	
	private String methodKey;
	
	private int methodArgSize;
	
	private int methodReturnSize;
	
	private boolean staticMethod;
	
	//private Map<Integer, Integer> extMethods;
	
	private Map<Integer, ExtObj> extMethods;
	
	private ExtObj returnInfo;
	
	private List<InstNode> path;
	
	private InstPool pool;
	
	private HashSet<Integer> firstReadLocalVars;
	
	private HashSet<Integer> firstReadFields;
	
	public GraphTemplate() {
		
	}
	
	public GraphTemplate(GraphTemplate copy) {
		this.methodKey = copy.getMethodKey();
		this.methodArgSize = copy.getMethodArgSize();
		this.methodReturnSize = copy.getMethodReturnSize();
		this.staticMethod = copy.isStaticMethod();
		this.firstReadFields = new HashSet<Integer>(copy.getFirstReadFields());
		this.firstReadLocalVars = new HashSet<Integer>(copy.getFirstReadLocalVars());
		this.extMethods = new HashMap<Integer, ExtObj>();
		
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
		
		for (Integer i: copy.getExtMethods().keySet()) {
			ExtObj from = copy.getExtMethods().get(i);
			ExtObj eo = new ExtObj();
			eo.setLineNumber(from.getLineNumber());
			for (InstNode localInst: from.getLoadLocalInsts()) {
				eo.addLoadLocalInst(this.pool.searchAndGet(localInst.getFromMethod(), localInst.getIdx()));
			}
			for (InstNode writeFieldInst: from.getWriteFieldInsts()) {
				eo.addWriteFieldInst(this.pool.searchAndGet(writeFieldInst.getFromMethod(), writeFieldInst.getIdx()));
			}
			for (InstNode affFieldInst: from.getAffFieldInsts()) {
				eo.addAffFieldInst(this.pool.searchAndGet(affFieldInst.getFromMethod(), affFieldInst.getIdx()));
			}
			this.extMethods.put(i, eo);
		}
		
		this.returnInfo = new ExtObj();
		this.returnInfo.setLineNumber(copy.getReturnInfo().getLineNumber());
		for (InstNode localInst: copy.getReturnInfo().getLoadLocalInsts()) {
			this.returnInfo.addLoadLocalInst(this.pool.searchAndGet(localInst.getFromMethod(), localInst.getIdx()));
		}
		for (InstNode writeFieldInst: copy.getReturnInfo().getWriteFieldInsts()) {
			this.returnInfo.addWriteFieldInst(this.pool.searchAndGet(writeFieldInst.getFromMethod(), writeFieldInst.getIdx()));
		}
		for (InstNode affFieldInst: copy.getReturnInfo().getAffFieldInsts()) {
			this.returnInfo.addAffFieldInst(this.pool.searchAndGet(affFieldInst.getFromMethod(), affFieldInst.getIdx()));
		}
	}
		
	public void setMethodKey(String methodKey) {
		this.methodKey = methodKey;
	}
	
	public String getMethodKey() {
		return this.methodKey;
	}
	
	public void setExtMethods(Map<Integer, ExtObj> extMethods) {
		this.extMethods = extMethods;
	}
	
	public Map<Integer, ExtObj> getExtMethods() {
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
	
	public void setStaticMethod(boolean staticMethod) {
		this.staticMethod = staticMethod;
	}
	
	public boolean isStaticMethod() {
		return this.staticMethod;
	}
	
	public void setReturnInfo(ExtObj returnInfo) {
		this.returnInfo = returnInfo;
	}
	
	public ExtObj getReturnInfo() {
		return this.returnInfo;
	}
	
	public void setFirstReadLocalVars(HashSet<Integer> firstReadLocalVars) {
		this.firstReadLocalVars = firstReadLocalVars;
	}
	
	public HashSet<Integer> getFirstReadLocalVars() {
		return this.firstReadLocalVars;
	}
	
	public void setFirstReadFields(HashSet<Integer> firstReadFields) {
		this.firstReadFields = firstReadFields;
	}
	
	public HashSet<Integer> getFirstReadFields() {
		return this.firstReadFields;
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
