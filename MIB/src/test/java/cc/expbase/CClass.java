package cc.expbase;

public class CClass extends PClass{
	
	public int cInt;
	
	public CClass() {
		System.out.println("Current cInt: " + this.cInt);
		this.cInt = 28;
		System.out.println("After setting: " + this.cInt);
	}
	
	@Override
	public void myMethod() {
		//this.cInt = 19;
		int i = super.cInt;
		int j = this.cInt;
		super.cInt = 100;
		this.cInt = 1000;
		System.out.println("Check i: " + i);
		System.out.println("myMethod in CClass: " + j);
		int k = super.cInt + this.cInt;
		System.out.println("Check k: " + k);
	}
	
	public static void main(String[] args) {
		CClass c = new CClass();
		System.out.println(c.cInt);
	}
}
