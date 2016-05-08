package edu.columbia.psl.cc.abs;

import edu.columbia.psl.cc.crawler.NativePackages;
import edu.columbia.psl.cc.pojo.InstNode;

public interface IRecorder {
	
	public static final int CONSTRUCTOR_DEFAULT = -5;
	
	public static final double EPSILON = 0.0001;
	
	public static String defaultPkgId = String.valueOf(NativePackages.defaultId);
	
	public void handleLdc(int opcode, int instIdx, int times, String addInfo);
	
	public void handleField(int opcode, int instIdx, String owner, String name, String desc);
	
	public void handleOpcode(int opcode, int instIdx, String addInfo);
	
	public void handleOpcode(int opcode, int instIdx, int localVarIdx);
	
	public void handleMultiNewArray(String desc, int dim, int instIdx);
	
	public void handleRawMethod(int opcode, 
			int instIdx, 
			int linenum, 
			String owner, 
			String name, 
			String desc, 
			InstNode fullInst);
	
	public void handleMethod(int opcode, int instIdx, int linenum, String owner, String name, String desc);
	
	public void handleDup(int opcode);
	
	public void dumpGraph();

}
