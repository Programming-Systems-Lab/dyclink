package cc.testbase;

import java.lang.reflect.Method;

public class TemplateOuter {
	
	private int myInt = 5;
	
	private Object myObj = new Object();
	
	public int returnInt() {
		return 5;
	}
	
	public Integer returnInteger() {
		return new Integer(87);
	}
	
	public static void main(String[] args) {
		TemplateOuter to = new TemplateOuter();
		TemplateInner ti = to.new TemplateInner();
		String result = ti.foo();
	}
	
	public class TemplateInner {
		
		private int myInt = 8;
		
		private String myObj = "TemplateInner";
		
		public String foo() {
			String ret = TemplateOuter.this.myInt + TemplateOuter.this.myObj.toString();
			//String ret2 = this.myInt + this.myObj.toString();
			return ret;
		}
	}

}
