package cc.expbase.global;

public abstract class Parent {
	
	public static String pStaticString = "Static123";
	
	public int pInt = 5;
	
	public double pDouble = 10.0;
	
	public String pString = "Obj123";
	
	public void setPInt(int val) {
		this.pInt = val;
	}
	
	public void setPDouble(double val) {
		this.pDouble = val;
	}
	
	public double parentMethod() {
		double ret = this.pDouble + this.pInt;
		return ret;
	}
	
	public abstract void printObjString();

}
