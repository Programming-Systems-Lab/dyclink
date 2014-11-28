package edu.columbia.psl.cc.util;

import java.util.ArrayList;
import java.util.List;

public class ConstructorBuffer {
	
	public static int BUFF_LDC = 0;
	
	public static int BUFF_FIELD = 1;
	
	public static int BUFF_OPCODE_IIS = 2;
	
	public static int BUFF_OPCODE_III = 3;
	
	public static int BUFF_MULTINEWARRAY = 4;
	
	public static int BUFF_METHOD = 5;
	
	public static int BUFF_DUP = 6;
	
	public static int BUFF_LABEL = 7;
	
	private List<BuffObj> buffer = new ArrayList<BuffObj>();
	
	public void replay(MethodStackRecorder msr) {
		for (BuffObj bo: this.buffer) {
			List<String> info = bo.buffList;
			switch(bo.actionId) {
				case 0:
					msr.handleLdc(Integer.valueOf(info.get(0)), 
							Integer.valueOf(info.get(1)), 
							Integer.valueOf(info.get(2)), 
							info.get(3));
					break ;
				case 1:
					msr.handleField(Integer.valueOf(info.get(0)), 
							Integer.valueOf(info.get(1)), 
							info.get(2), 
							info.get(3), 
							info.get(4));
					break ;
				case 2:
					msr.handleOpcode(Integer.valueOf(info.get(0)), 
							Integer.valueOf(info.get(1)), 
							info.get(2));
					break ;
				case 3:
					msr.handleOpcode(Integer.valueOf(info.get(0)), 
							Integer.valueOf(info.get(1)), 
							Integer.valueOf(info.get(2)));
					break ;
				case 4:
					msr.handleMultiNewArray(info.get(0), 
							Integer.valueOf(info.get(1)), 
							Integer.valueOf(info.get(2)));
					break ;
				case 5:
					msr.handleMethod(Integer.valueOf(info.get(0)), 
							Integer.valueOf(info.get(1)), 
							Integer.valueOf(info.get(2)), 
							info.get(3), info.get(4), info.get(5));
					break ;
				case 6:
					msr.handleDup(Integer.valueOf(info.get(0)));
					break ;
				case 7:
					msr.updateCurLabel(info.get(0));
					break ;
			}
		}
	}
	
	public void buffLdc(int opcode, int instIdx, int times, String addInfo) {
		BuffObj bo = new BuffObj();
		bo.actionId = BUFF_LDC;
		bo.buffList.add(String.valueOf(opcode));
		bo.buffList.add(String.valueOf(instIdx));
		bo.buffList.add(String.valueOf(times));
		bo.buffList.add(addInfo);
		this.buffer.add(bo);
	}
	
	public void buffField(int opcode, int instIdx, String owner, String name, String desc) {
		BuffObj bo = new BuffObj();
		bo.actionId = BUFF_FIELD;
		bo.buffList.add(String.valueOf(opcode));
		bo.buffList.add(String.valueOf(instIdx));
		bo.buffList.add(owner);
		bo.buffList.add(name);
		bo.buffList.add(desc);
		this.buffer.add(bo);
	}
	
	public void buffOpcode(int opcode, int instIdx, String addInfo) {
		BuffObj bo = new BuffObj();
		bo.actionId = BUFF_OPCODE_IIS;
		bo.buffList.add(String.valueOf(opcode));
		bo.buffList.add(String.valueOf(instIdx));
		bo.buffList.add(addInfo);
		this.buffer.add(bo);
	}
	
	public void buffOpcode(int opcode, int instIdx, int localVarIdx) {
		BuffObj bo = new BuffObj();
		bo.actionId = BUFF_OPCODE_III;
		bo.buffList.add(String.valueOf(opcode));
		bo.buffList.add(String.valueOf(instIdx));
		bo.buffList.add(String.valueOf(localVarIdx));
		this.buffer.add(bo);
	}
	
	public void buffMultiNewArray(String desc, int dim, int instIdx) {
		BuffObj bo = new BuffObj();
		bo.actionId = BUFF_MULTINEWARRAY;
		bo.buffList.add(String.valueOf(desc));
		bo.buffList.add(String.valueOf(dim));
		bo.buffList.add(String.valueOf(instIdx));
		this.buffer.add(bo);
	}
	
	public void buffMethod(int opcode, 
			int instIdx, 
			int linenum, 
			String owner, 
			String name, 
			String desc) {
		BuffObj bo = new BuffObj();
		bo.actionId = BUFF_METHOD;
		bo.buffList.add(String.valueOf(opcode));
		bo.buffList.add(String.valueOf(instIdx));
		bo.buffList.add(String.valueOf(linenum));
		bo.buffList.add(owner);
		bo.buffList.add(name);
		bo.buffList.add(desc);
		this.buffer.add(bo);
	}
	
	public void buffDup(int opcode) {
		BuffObj bo = new BuffObj();
		bo.actionId = BUFF_DUP;
		bo.buffList.add(String.valueOf(opcode));
		this.buffer.add(bo);
	}
	
	public void buffLabel(String label) {
		BuffObj bo = new BuffObj();
		bo.actionId = BUFF_LABEL;
		bo.buffList.add(label);
		this.buffer.add(bo);
	}
	
	public static class BuffObj {
		int actionId;
		
		List<String> buffList = new ArrayList<String>(); 
	}

}
