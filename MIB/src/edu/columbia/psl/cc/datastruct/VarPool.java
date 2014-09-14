package edu.columbia.psl.cc.datastruct;

import java.util.HashSet;

import edu.columbia.psl.cc.pojo.LocalVar;
import edu.columbia.psl.cc.pojo.ObjVar;
import edu.columbia.psl.cc.pojo.Var;

public class VarPool extends HashSet<Var>{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Var genObjVar(int opcode, String className, String methodName, int silId, String varInfo) {
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
	
	private Var genLocalVar(int opcode, String className, String methodName, String varInfo) {
		LocalVar lv = new LocalVar();
		lv.setSilId(2);
		lv.setOpcode(opcode);
		lv.setClassName(className);
		lv.setMethodName(methodName);
		lv.setLocalVarId(Integer.valueOf(varInfo));
		return lv;
	}
	
	public Var searchVar(int opcode, String className, String methodName, int silId, String varInfo) {
		for (Var v: this) {
			if (v.getOpcode() == opcode && 
					v.getClassName().equals(className) && 
					v.getMethodName().equals(methodName) && 
					v.getSilId() == silId && 
					v.getVarInfo().equals(varInfo)) {
				return v;
			}
		}
		
		//Reach here means that this var is new
		Var v;
		if (silId == 0 || silId == 1) {
			v = this.genObjVar(opcode, className, methodName, silId, varInfo);
		} else {
			v = this.genLocalVar(opcode, className, methodName, varInfo); 
		}
		this.add(v);
		return v;
	}

}
