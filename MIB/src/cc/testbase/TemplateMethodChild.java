package cc.testbase;

public class TemplateMethodChild extends TemplateMethod {
	
	public void callSuper() {
		super.exampleMethod();
	}
	
	public static void main(String args[]) {
		TemplateMethodChild tmc = new TemplateMethodChild();
		tmc.callSuper();
	}

}
