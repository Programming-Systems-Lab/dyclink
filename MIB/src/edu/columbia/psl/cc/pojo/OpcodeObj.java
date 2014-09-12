package edu.columbia.psl.cc.pojo;

public class OpcodeObj {
	
	int catId;
	
	int opcode;
	
	String instruction;
	
	public void setCatId(int catId) {
		this.catId = catId;
	}
	
	public int getCatId() {
		return this.catId;
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

