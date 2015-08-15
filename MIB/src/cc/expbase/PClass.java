package cc.expbase;

public abstract class PClass {
	
	protected int pInt = 18;
	
	public int cInt = 15;
	
	public PClass() {
		this.myMethod();
	}
	
	public void pMethod() {
		System.out.println("I am pMethod");
	}
	
	public abstract void myMethod();

}
