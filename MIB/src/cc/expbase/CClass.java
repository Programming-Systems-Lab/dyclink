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
		this.cInt = 19;
		System.out.println("myMethod in CClass: " + this.cInt);
	}
	
	public static void main(String[] args) {
		CClass c = new CClass();
		System.out.println(c.cInt);
	}
}
