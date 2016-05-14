package cc.expbase.global;

public class Child extends Parent {
	
	public float cFloat;
	
	public Child(float val) {
		this.cFloat = val;
	}
	
	public void printObjString() {
		System.out.println(this.pString);
	}
	
	public static void printStaticString() {
		System.out.println(pStaticString);
	}
	
	public double parentMethod() {
		double parentVal = super.parentMethod();
		double ret = (double)cFloat + parentVal;
		return ret;
	}
	
	public int factorial(int n) {
		if (n == 0) {
			return 0;
		}
		
		if (n == 1) {
			return n;
		} else {
			return n * factorial(n - 1);
		}
	}
		
	public static void main(String[] args) {
		//Case 1
		printStaticString();
		
		//Case 2
		//Child c = new Child(5.2f);
		//c.printObjString();
		
		//Case 3
		/*System.out.println(c.parentMethod());
		c.setPDouble(6.8);
		c.setPInt(5);
		System.out.println(c.parentMethod());*/
		
		//Case 4
		//System.out.println(c.factorial(5));
	}

}
