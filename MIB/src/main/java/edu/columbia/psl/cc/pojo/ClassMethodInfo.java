package edu.columbia.psl.cc.pojo;

import org.objectweb.asm.Type;

public class ClassMethodInfo {
		
	public Type returnType;
	
	//The input args of method
	public Type[] args;
	
	//The word size of input arg
	public int argSize;
	
	public int endIdx;
}
