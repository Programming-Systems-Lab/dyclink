package edu.columbia.psl.cc.datastruct;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicInteger;

import edu.columbia.psl.cc.pojo.FakeVar;
import edu.columbia.psl.cc.pojo.LocalVar;
import edu.columbia.psl.cc.pojo.ObjVar;
import edu.columbia.psl.cc.pojo.Var;

public class VarPool extends HashSet<Var>{
	
	private static final long serialVersionUID = 1L;
	
	private static AtomicInteger fakeIdCreator = new AtomicInteger();
	
	public VarPool() {
		
	}
	
	public VarPool(Collection<Var> input) {
		for (Var v: input) {
			this.add(v);
		}
	}
	
	private static int genFakeId() {
		return fakeIdCreator.getAndIncrement();
	}

	private static Var genObjVar(String className, String methodName, int silId, String varInfo) {
		ObjVar ov = new ObjVar();
		ov.setSilId(silId);
		ov.setClassName(className);
		ov.setMethodName(methodName);
		
		//Parse varInfo
		String[] info = varInfo.split(":");
		ov.setNativeClassName(info[0]);
		ov.setVarName(info[1]);
		return ov;
	}
	
	/**
	 * When generate a local var, don't know its start/end label
	 * Add labels when analyze it
	 * @param className
	 * @param methodName
	 * @param varInfo
	 * @return
	 */
	private static Var genLocalVar(String className, String methodName, String varInfo) {
		LocalVar lv = new LocalVar();
		lv.setSilId(2);
		lv.setClassName(className);
		lv.setMethodName(methodName);
		lv.setLocalVarId(Integer.valueOf(varInfo));
		return lv;
	}
	
	private Var genFakeVar() {
		FakeVar fakeVar = new FakeVar();
		fakeVar.setMethodName("fakeMethod");
		fakeVar.setClassName("fakeClass");
		fakeVar.setSilId(3);
		fakeVar.setFakeId(genFakeId());
		return fakeVar;
	}
		
	public Var searchVar(String className, String methodName, int silId, String varInfo) {
		for (Var v: this) {
			if (v.getClassName().equals(className) && 
					v.getMethodName().equals(methodName) && 
					v.getSilId() == silId && 
					v.getVarInfo().equals(varInfo)) {
				return v;
			}
		}
		
		//Reach here means that this var is new
		Var v;
		if (silId == 0 || silId == 1) {
			v = genObjVar(className, methodName, silId, varInfo);
		} else if (silId == 2){
			v = genLocalVar(className, methodName, varInfo); 
		} else {
			v = genFakeVar();
		}
		this.add(v);
		return v;
	}

}
