package cc.testbase;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;

import edu.columbia.psl.cc.annot.analyzeClass;
import edu.columbia.psl.cc.annot.extractTemplate;
import edu.columbia.psl.cc.annot.testTemplate;
import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.util.MethodStackRecorder;
import edu.columbia.psl.cc.util.ObjectIdAllocater;

@analyzeClass
public class TemplateMethod extends TemplateParent {
	
	private static int sVar;
	
	private int iVar;
	
	private double iDouble;
	
	ArrayList<Integer> fakeList = new ArrayList<Integer>();
	
	private String test;
	
	public Object UBV = new Object();
	
	public double[] u = new double[3];
	
	public double[] b = new double[3];
	
	public int m = 5;
 	
	public TemplateMethod() {
		
	}
	
	public TemplateMethod(String test1, String test2, String test3) {
		this.test = test1;
	}
	
	public void callee( Object A , double u[] , double gamma ,
            int colA0,
            int w0, int w1 ,
            double _temp[] ) {
		Object o1 = A;
		double[] d1 = u;
		double d2 = gamma;
		double c = colA0;
		int d = w0;
		int e = w1;
		double[] d3 = _temp;
	}
	
	public void byteTest(byte b1, double d1, long l1, int i1, Object o1, double[] dArray1, String s1) {
		byte input1 = b1;
		double input2 = d1;
		long input3 = l1;
		int input4 = i1;
		Object input5 = o1;
		double[] input6 = dArray1;
		String input7 = s1;
	} 
	
	public void caller() {
		double gamma = 1;
		int k = 8;
		this.callee(UBV,u,gamma,k+1,k,m,this.b);
	}
	
	private  static int classAdd(int i) {
		return i + i;
	}
	
	@Override
	public ArrayList setList(List l) {
		return new ArrayList();
	}
	
	public int add2(int a) {
		return a + a;
	}
	
	public int testAdd(int a) {
		int c = a + a;
		return c;
	}
	
	public int testAdd2(int a) {
		int c = this.add2(a);
		return c;
	}
	
	public void doubleIf(double input) {
		int ret = 0;
		if (input > 0) {
			ret = 1;
		} else {
			ret = 0;
		}
	}
	
	public void multiArray() {
		int[][] newArray = new int[3][4];
	}
	
	public void tryFakeList() {
		YAMethod ym = new YAMethod();
		fakeList.add(5);
		ym.addSomething(fakeList);
	}
	
	//@extractTemplate
	public int fieldTest() {
		this.iVar = 5;
		int ret = this.iVar + sVar;
		ret = this.iVar + this.pVar;
		return ret;
	}
	
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
			if (b < 5) {
				ret = "Testaaa";
			} else {
				ret = "Testbbb";
			}
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
		
	//@extractTemplate
	public int testField(int input) {
		int ret = this.iVar + sVar + input;
		Label l = new Label();
		l.getOffset();
		return ret;
	}
	
	//@extractTemplate
	public void testContinuousAnd(int keyBits) {
		//int ret = 0;
		if (keyBits != 128 && keyBits != 192 && keyBits != 256) {
			throw new RuntimeException("Invalid AES key size (" + keyBits + " bits)");
        }
	}
	
	//@extractTemplate
	public void dummyInvoke() {
		int a = 5; 
		int b= 3;
		int c = this.addInside(a, b);
	}
	
	//@extractTemplate
	public void dummyArray(int[] a) {
		int i = 2;
		int j = 5;
		a[i] = j;
	}
	
	@extractTemplate
	public String fakeString(int input) {
		//int a = input + 5;
		//String ret = "ret > 5: " + input;
		return "ret > 5";
	}
	
	@extractTemplate
	public void increArray(int[] input) {
		for (int i = 0; i < input.length; i++) {
			input[i] = 5;
		}
	}
	
	@extractTemplate
	public int sumArray(int[] input) {
		int sum = 0;
		for (int i = 0; i < input.length; i++) {
			sum += input[i];
		}
		return sum;
	}
	
	@extractTemplate
	public int invoke3Methods(int a, int b) {
		int c = a + b + this.pVar;
		String ret = "";
		if (c > 5) {
			ret = fakeString(c); 
		} else {
			ret = "ret <= 5";
		}
		
		int[] target = new int[5];
		this.increArray(target);
		int sum = this.sumArray(target);
		return sum;
	}
	
	@extractTemplate
	public int all3Methods(int a, int b) {
		//this.iVar = 5;
		//this.iDouble = 8;
		//double c = this.iDouble + a + b + this.iVar + sVar + pVar;
		int c = a + b;
		String ret = "";
		if (c > 5) {
			ret = "ret > 5";
		} else {
			ret = "ret <= 5";
		}
		
		int[] target = new int[5];
		for (int i = 0; i < target.length; i++) {
			target[i] = 5;
		}
		
		int sum = 0;
		for (int j = 0; j < target.length; j++) {
			sum += target[j];
		}
		return sum;
	}
	
	public int invokeParent(int a, int b) {
		int i = this.parentAdd(a, b);
		int ret = i + this.pVar;
		return ret;
	}
	
	//@extractTemplate
	public void dummyFor(int[] a) {
		int k = 100000;
		int j = 5;
		for (int i = 0; i < a.length; i++) {
			if (i < 2)
				a[i] += j; 
			else
				a[i] += 1;
		}
	}
	
	public void dummyObj(Object o) {
		
	}
	
	public void dummyFor(double[] a) {
		int j = 5;
		for (int i = 0; i < a.length; i++) {
			a[i] += j;
		}			
	}
	
	//@extractTemplate
	public void unidentified() {
		int i = 4;
		double[] d = new double[5];
		d[i] = 3;
		
		double a = d[i] + 5;
	}
	
	public double testDiff(int a, double b) {
		double c = 0;
		try {
			c = a + b;
		} finally {
			c--;
		}
		return c;
	}
	
	@Override
	public void interestingMethod() {
        System.out.println("Subclass's interesting method.");
    }
	
	public void forMethod() {
		for (int i = 0; i < 3; i++) {
			int c = this.add2(i);
		}
	}
	
	public void forClassMethod() {
		for (int i = 0; i < 3; i++) {
			classAdd(i);
		}
	}
	
	public void call3() {
		this.pVar = 5;
		double ret = 5 + this.iDouble + this.iVar;
	}
	
	public void call2() {
		this.iVar = 2;
		this.call3();
	}
	
	public void call1() {
		this.iDouble = 5.0 + this.pVar;
		this.call2();
		int ret = 5 + this.pVar + this.iVar;
	}
		
	public static void main(String[] args) {
		TemplateMethod tm = new TemplateMethod();
		//tm.call1();
		/*byte a = 1;
		double b = 2.0;
		long c = 3;
		int d = 4;
		Object e = new Object();
		double[] dArray = new double[4];
		String f = "abc";
		tm.byteTest(a, b, c, d, e, dArray, f);*/
		//tm.parentAdd(3, 5);
		//tm.testAdd2(5);
		//tm.caller();
		//System.out.println(Integer.MAX_VALUE);
		//System.out.println(Integer.MAX_VALUE + 1);
		//classAdd(5);
		//int a = 2;
		int[] b = {3, 4, 7};
		//tm.forMethod();
		//tm.forClassMethod();
		//TemplateParent tp = new TemplateMethod();
		//tp.interestingMethod();
		
		/*TemplateInterface ti = new TemplateMethod();
		Integer i = new Integer(1);
		ti.doIt(i);*/
		
		//tm.setList(new ArrayList());
		//tm.exampleMethod();
		//tm.invokeParent(3, 5);
		//System.out.println(tm.all3Methods(3, 5));
		//System.out.println(tm.sumArray(b));
		//System.out.println(tm.simpleIf(1, 2));
		System.out.println(tm.instanceMethod(0, b));
		//System.out.println("Test add: " + tm.testAdd(2));
		//System.out.println("Test add2: " + tm.testAdd2(5));
		//System.out.println("TemplateMethod: " + tm.invoke3Methods(3, 5));
		//System.out.println("TestMethod: " + tm.all3Methods(3, 5));
		/*System.out.println("cc/testbase/TemplateMethod.iVar.I".split("\\.").length);
		System.out.println(Type.INT);
		System.out.println(Type.INT_TYPE.getSort());
		System.out.println(Type.getType("I").getSort());
		System.out.println(Type.getType("Ljava/lang/Integer;").getSort());*/
		//System.out.println("-------Check result--------" + tm.fieldTest());
		//System.out.println("-------Check result--------" + tm.parentAdd(3, 5));
		//System.out.println("TempalteMethod: " + tm.addOutside(3, 5));
		//tm.testAdd3(1, 2, 3);
		//tm.setBoolean(true);
		//System.out.println("TestMethod: " + tm.addInside(3, 5));
		//TemplateMethod tm2 = new TemplateMethod("test1", "test2", "test3");
		//tm.testField(5);
		//tm.testContinuousAnd(128);
		//tm.dummyInvoke();
		//tm.dummy4(1, 6);
		//tm.dummmy2(1, 6);
		//int[] test = {1, 2, 3};
		//tm.dummyFor(test);
		//tm.dummyArray(test);
		//tm.unidentified();
		//tm.simulateStack();
		/*Type methodType = Type.getMethodType("(II)V");
		System.out.println(methodType.getSize());
		System.out.println(methodType.getReturnType().getDescriptor());
		System.out.println(methodType.getArgumentTypes().length);
		System.out.println(methodType.getArgumentsAndReturnSizes());*/
		
		/*long[] longArray = new long[5];
		System.out.println(Type.getType(longArray.getClass()).getElementType());
		long longVar = 5;
		System.out.println(Type.LONG_TYPE);
		System.out.println(Type.DOUBLE_TYPE);
		System.out.println(Type.INT_TYPE);*/
	}
	
	public int empty(int a, double b) {
		int c = a + 5;
		return c;
	}
	
	public void setBoolean(boolean a) {
		
	}
	
	//@extractTemplate
	public int add3(int i1, int i2, int i3) {
		return i1 + i2 + i3;
	}
	
	//@extractTemplate
	public int testAdd3(int i1, int i2, int i3) {
		return add3(i2, i3, i1);
	}
	
	//@extractTemplate
	public int add(int i1, int i2) {
		this.iVar = i1 + 5;
		return i1 + i2;
	}
	
	//@testTemplate
	public int addInside(int a, int b) {
		//int a = this.iVar + sVar;
		int c = a + b;
		return c;
	}
	
	//@extractTemplate
	public int addOutside(int a, int b) {
		int c = this.add(a, b);
		int d = this.iVar;
		return c;
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
	
	public String simpleIf(int a, int b) {
		int  c = a + b;
		String ret = "";
		if (c > 5) {
			ret = "c > 5";
		} else {
			ret = "c <= 5";
		}
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
	
	public void ifTest(int a, int b) {
		int d = 0;
		if (a > 5) {
			if (b > 5) {
				d = 5;
			} else {
				d = 3;
			}
		}
		int e = 8;
	}
	
	public void tryCatchTest(int[] a) {
		try {
			a[5] = 8;
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			a[0] = 0;
		}
	}
	
	public void forTest(int[] b) {
		for (int i = 0; i < b.length; i++) {
			if (i == 0)
				b[i] = 5;
			else
				b[i] = 8;
		}
		
		int count = 0;
		while (true) {
			b[count++] = 10;
			
			if (count == 10)
				break;
		}
		
		count = 0;
		do {
			b[count++] = 19;
		} while (b.length < 10);
	}
	
	public void fakeInstanceMethod(int a, int b) {
		int c = a + 5;
		int d = 6;
		int e = b + c;
	}
	
	public void multipleControl(int a, int b) {
		int c = 0;
		if (a == 5) {
			c = 8;
		}
		
		int efg = 678;
		
		if (a < 6) {
			if (a >3) {
				c = 32;
			}
		} else {
			c = 38;
		}
		
		if (a < 5) {
			int ab = 900;
			int cd = 100000;
			double dkjl = 90000;
			String kl = "ababababa";
		} else if (a > 3) {
			int ab = 345;
			int de = 678;
		} else {
			int ab = 90;
			int cd = 10000;
			double dkjl = 9000;
			String kl = "abababa";
		}
		
		int k = 300;
	}
	
	public int instanceMethod(int a, int[] b) {
		int ret = a + 5;
		//this.iVar = 5;
		switch(a) {
			case 0:
				b[0] = 0;
				break ;
			case 2:
				b[0] = 1;
				break ;
			default:
				b[0] = 7;
				break ;
			case 6:
				b[0] = 6;
				break ;
		}
		int d = 89;
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
