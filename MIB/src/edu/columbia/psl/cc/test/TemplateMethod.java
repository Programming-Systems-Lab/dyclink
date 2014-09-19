package edu.columbia.psl.cc.test;

import edu.columbia.psl.cc.annot.analyzeClass;
import edu.columbia.psl.cc.annot.extractTemplate;
import edu.columbia.psl.cc.annot.testTemplate;

@analyzeClass
public class TemplateMethod {
	
	private static int sVar;
	
	private int iVar;
	
	@extractTemplate
	public void dummy(int a, int b) {
		String ret = "";
		int c = 5;
		if (a > 3) {
			int d = 9;
			if (b < 5) {
				ret = "Test123";
			} else {
				ret = "Test456";
			}
		} else {
			ret = "Test789";
		}
	}
	
	//@extractTemplate
	public int sequencePrimitiveTemplate(int a, int b, int c) {
		int d = a + 5;
		int e = d + b;
		int f = e + c;
		return f;
	}
	
	//@extractTemplate
	public int parallelPrimitiveTemplate(int a, int b, int c) {
		int d = a + b;
		int e = c + 15;
		int f = d + e;
		return f;
	}
	
	public String ifTemplate(int a) {
		String ret;
		if (a > 5)
			ret = "Bigger than 5";
		else
			ret = "Smaller than 5";
		return ret;
	}
	
	//@extractTemplate
	public String ifTemplate(int a, int b, int c) {
		if (a > b) {
			if (c > 5) {
				String ret11 = "a is larger than b and c is bigger than 5";
				return ret11;
			} else {
				String ret12 = "a is larger than b and c is smaller than 5";
				return ret12;
			}
		} else if (b > a) {
			String ret2 = "b is larger";
			return ret2;
		} else {
			String ret3 = "equals";
			return ret3;
		}
	}
	
	public int instanceMethod(int a, int[] b) {
		int ret = a + this.iVar + b[0];
		return ret;
	}
	
	//@testTemplate
	public double testPrimitiveSequence(double d1, double d2) {
		double d3 = d2 + 8;
		double d4 = d3 + 15;
		double d5 = d4 + d1;
		return d5;
	}
	
	//@testTemplate
	public double testDoubleTemplate(double a, double b, double c) {
		double d = a + 5;
		double e = d + b;
		double f = e + c;
		return f;
	}
}
