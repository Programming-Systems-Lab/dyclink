package edu.columbia.psl.cc.pojo;

public class FakeVar extends Var {
	
	private int fakeId;
	
	public FakeVar() {
		this.setClassName("fakeClass");
		this.setMethodName("fakeMethod");
		this.setSilId(3);
	}
	
	public void setFakeId(int fakeId) {
		this.fakeId = fakeId;
	}
	
	public int getFakeId() {
		return this.fakeId;
	}

}
