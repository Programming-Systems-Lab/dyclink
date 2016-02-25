package edu.columbia.psl.cc.pojo;

import java.util.ArrayList;
import java.util.List;

public class OpcodeObj {
	
	private int catId;
	
	private int subCatId;
	
	private int subSubCatId;
	
	private int opcode;
	
	private String instruction;
	
	//Probably need to change to pojo some time in the future?
	private List<String> inList = new ArrayList<String>();
	
	private List<String> outList = new ArrayList<String>();
	
	public void setCatId(int catId) {
		this.catId = catId;
	}
	
	public int getCatId() {
		return this.catId;
	}
	
	public void setSubCatId(int subCatId) {
		this.subCatId = subCatId;
	}
	
	public int getSubCatId() {
		return this.subCatId;
	}
	
	public void setSubSubCatId(int subSubCatId) {
		this.subSubCatId = subSubCatId;
	}
	
	public int getSubSubCatId() {
		return this.subSubCatId;
	}
	
	public void setOpcode(int opcode) {
		this.opcode = opcode;
	}
	
	public int getOpcode() {
		return this.opcode;
	}
	
	public String getOpcodeHex() {
		return "0x" + Integer.toHexString(this.opcode);
	}
	
	public void setInstruction(String instruction) {
		this.instruction = instruction;
	}
	
	public String getInstruction() {
		return this.instruction;
	}
	
	public void setInList(List<String> inList) {
		this.inList = inList;
	}
	
	public List<String> getInList() {
		return this.inList;
	}
	
	public void setOutList(List<String> outList) {
		this.outList = outList;
	}
	
	public List<String> getOutList() {
		return this.outList;
	}
	
	@Override
	public String toString() {
		String result = this.catId + ":" + this.opcode + ":" + getOpcodeHex() + this.instruction;
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof OpcodeObj))
			return false;
		
		OpcodeObj tmpOo = (OpcodeObj)obj;
		if (tmpOo.toString().equals(this.toString()))
			return true;
		else
			return false;
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
}

