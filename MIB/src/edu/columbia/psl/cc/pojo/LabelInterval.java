package edu.columbia.psl.cc.pojo;

public class LabelInterval {
	
	private int startOffset;
	
	private int endOffset;
	
	public void setStartOffset(int startOffset) {
		this.startOffset = startOffset;
	}
	
	public int getStartOffset() {
		return this.startOffset;
	}
	
	public void setEndOffset(int endOffset) {
		this.endOffset = endOffset;
	}
	
	public int getEndOffset() {
		return this.endOffset;
	}
	
	@Override
	public String toString() {
		return String.valueOf(this.startOffset) + "-" + String.valueOf(this.endOffset);
	}

}
