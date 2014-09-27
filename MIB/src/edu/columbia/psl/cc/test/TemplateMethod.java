package edu.columbia.psl.cc.test;

import java.util.Stack;

import org.objectweb.asm.Type;

import edu.columbia.psl.cc.annot.analyzeClass;
import edu.columbia.psl.cc.annot.extractTemplate;
import edu.columbia.psl.cc.annot.testTemplate;
import edu.columbia.psl.cc.util.MethodStackRecorder;

@analyzeClass
public class TemplateMethod {
	
	private static int sVar;
	
	private int iVar;
	
	Object lock1 = new Object();
	
	//@extractTemplate
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
	public void dummy2() {
		int[] b = new int[]{1, 1, 1};
		//int k = 2;
		//b[0] = k;
		int k = 5 + b[0];
		//int[][] b = new int[1][2];
		//b[0][1] = k; 
	}
	
	public void dummy3() {
		int[] b = new int[3];
		int[] d = new int[4];
		Object[] oo = new Object[5];
		int j = 5;
		int l = 6;
		b[j] = j + d[2];
		
		int k = j + b[2];
		
		int[][] c = new int[1][2];
		c[0][1] = k;
		
		/*int[] a = new int[]{1, 1, 1};
		for (int i = 0; i < a.length; i++) {
			a[i] += a[i] * i;
		}*/
	}
	
	@extractTemplate
	public void dummmy2(int a, int b) {
		//int a = this.iVar + sVar;
		int c = a + b;
	}
	
	//@extractTemplate
	public void dummyArray(int[] a) {
		int i = 2;
		int j = 5;
		a[i] = j;
	}
	
	//@extractTemplate
	public void dummy4(int a, int b) {
		int c = a + b;
		String ret = "";
		if (c > 5) {
			ret = "ret > 5"; 
		} else {
			ret = "ret <= 5";
		}
		System.out.println("Result: " + c);
	}
	
	//@extractTemplate
	public void dummyFor(int[] a) {
		int j = 5;
		for (int i = 0; i < a.length; i++) {
			a[i] += j; 
		}
	}
	
	public void dummyFor(double[] a) {
		int j = 5;
		for (int i = 0; i < a.length; i++) {
			a[i] += j;
		}
			
	}
	
	public void unidentified(String input1, String input2) {
		int a = 1;
		int b = 2;
		double c = a + b;
		double[] d = new double[5];
		d[0] += 5;
		c += 1;
		long aa = 10;
		long bb = 15;
		boolean cc = aa > bb;
	}
	
	public void simulateStack() {
		this.unidentified("abc", "cde");
		String label = "123";
		MethodStackRecorder msr = new MethodStackRecorder();
		msr.handleOpcode(21, "123", 1);
		msr.handleOpcode(21, label, 2);
		msr.handleOpcode(96, label, -1);
		msr.handleOpcode(54, label, 3);
		msr.handleOpcode(18, label, -1);
		msr.handleOpcode(58, label, 4);
		msr.handleOpcode(21, label, 3);
		msr.handleOpcode(8, label, -1);
		msr.handleOpcode(164, label, -1);
		msr.handleOpcode(18, label, -1);
		msr.handleOpcode(58, label, 4);
		msr.dumpGraph();
	}
	
	public static void main(String[] args) {
		TemplateMethod tm = new TemplateMethod();
		//tm.dummy4(1, 6);
		tm.dummmy2(1, 6);
		//int[] test = {1, 2, 3};
		//tm.dummyArray(test);
		//tm.simulateStack();
		/*Type methodType = Type.getMethodType("(II)V");
		System.out.println(methodType.getSize());
		System.out.println(methodType.getReturnType().getDescriptor());
		System.out.println(methodType.getArgumentTypes().length);
		System.out.println(methodType.getArgumentsAndReturnSizes());*/
	}
	
	public synchronized int add(int a, int b) {
		int[][] c = new int[4][5];
		this.iVar = 8;
		c[1][2] = 9;
		return a + b;
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
	
	public String dummyString(int a) {
		return String.valueOf(a);
	}
	
	public void textExternal(int a) {
		int a1 = 1;
		int a2 = 2;
		int a3 = 3;
		int a4 = a2;
		int b = a + parallelPrimitiveTemplate(a1, a2, a3);
		String c = this.dummyString(a);
		boolean f = (c instanceof Object);
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
	
	public void fakeInstanceMethod(int a, int b) {
		int c = a + 5;
		int d = 6;
		int e = b + c;
	} 
	
	public int instanceMethod(int a, int[] b) {
		int ret = a + this.iVar + b[0];
		switch(a) {
			case 0:
				b[0] = 0;
				break;
			case 2:
				b[0] = 1;
				break;
			case 6:
				b[0] = 6;
			default:
				b[0] = 2;
		}
		Object[] objArray = new Object[5];
		Object[][][] multiple = new Object[3][4][5];
		Object obj = new Object();
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
