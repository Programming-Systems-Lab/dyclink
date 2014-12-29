package cc.expbase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.math3.linear.RealMatrixFormat;
import org.apache.commons.math3.util.CompositeFormat;

public class TemplateParent implements TemplateInterface{
	
	public static int parentVar = 10000;
	
	public int sVar = 0;
	
	public int pVar = 0;
	
	public Object obj;
	
	@Override
	public String doIt(Number n) {
		System.out.println("Template Parent do: " + n);
		return "";
	}
	
	public static RealMatrixFormat getInstance(final Locale locale) {
		return new RealMatrixFormat(CompositeFormat.getDefaultNumberFormat(locale));
	}
	
	public static String classMethod() {
		return "classMethod";
	}
	
	public List setList(List list) {
		return new ArrayList();
	}
	
	public int parentAdd(int a, int b) {
		DummySet.setParent(this);
		return a + b;
	}
	
	public void interestingMethod() {
        System.out.println("Superclass's interesting method.");
    }

    void exampleMethod() {
    	int i = 5;
        this.interestingMethod();
    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
