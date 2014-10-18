package cc.testbase;

import java.util.Locale;

import org.apache.commons.math3.linear.RealMatrixFormat;
import org.apache.commons.math3.util.CompositeFormat;

public class TemplateParent {
	
	public int sVar = 0;
	
	public int pVar = 0;
	
	public Object obj;
	
	public static RealMatrixFormat getInstance(final Locale locale) {
		return new RealMatrixFormat(CompositeFormat.getDefaultNumberFormat(locale));
	}
	
	public int parentAdd(int a, int b) {
		obj = 5;
		return a + b;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
