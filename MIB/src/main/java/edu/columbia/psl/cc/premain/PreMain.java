package edu.columbia.psl.cc.premain;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;

import edu.columbia.psl.cc.inst.MIBClassFileTransformer;

public class PreMain {

	public static void premain(String args, Instrumentation inst) {
		ClassFileTransformer classTransformer = new MIBClassFileTransformer();
		inst.addTransformer(classTransformer);
	}

}
