package cc.testbase;

import edu.columbia.psl.cc.annot.analyzeClass;
import edu.columbia.psl.cc.annot.extractTemplate;
import edu.columbia.psl.cc.annot.testTemplate;

@analyzeClass
public class JohnExample {
	
	public Object barrierLock = new Object();
	
	public void testArgs(String...input) {
		
	}
	
	protected final void recordModification(Object x) {
         synchronized (barrierLock) {
             Object a = x;
         }
    }
	
	public static void main(String[] args) {
		JohnExample je = new JohnExample();
		je.testArgs("abc");
	}
	
	//@extractTemplate
	public String foo0(int a, int b) {
		int c = a + b;
		
		String ret = "";
		if (c > 5) {
			ret = "Larger than 5";
		} else {
			ret = "Smaller or equal to 5";
		}
		return ret;
	}
	
	@extractTemplate
	public void foofoo(long[] arr1, long[] arr2) {
		for (int i = 0; i < arr1.length; i++) {
			if (i % 4 == 0)
				arr1[i] += i*6;
			else {
				arr1[i] += i;
			}
		}
	}
	
	
	//@extractTemplate
	public void foo1(long[] arr1, long[] arr2) {
		for (int i = 0; i < arr1.length; i++) {
			if (i % 4 == 0)
				arr1[i] += i*6;
			else {
				arr1[i] += i;
			}
		}
		
		for (int i = 0; i < arr2.length; i++) {
			arr2[i] *= (i * (i-1)) / 6;
		}
	}
	
	//@extractTemplate
	public void foo2(long[] a, long[] b) {
		for (int j = 0; j < b.length; j++) {
			b[j] /= j * (j-1) / 6 + 1;
		}
		
		for (int j = 0; j < a.length; j++) {
			if (j % 4 == 0)
				a[j] = j * 67 / 3;
			else
				a[j] += j;
		}
	}
	
	//@testTemplate
	public long foo3(long[] arr1, long[] arr2) {
		long ret = 0;
		
		for (int i = 0; i < arr1.length; i++) {
			long a2Tmp = arr2[i]/((i * i) * 3);
			if (i % 4 == 0) {
				ret += arr1[i] * a2Tmp * 100/ (arr1[i] + a2Tmp + 1);
			} else {
				ret += i * 1000 / 4;
			}
		}
		return ret;
	}
	
	public void foo4() {
		int[] a = new int[4];
		for (int i =0; i < a.length; i++) {
			a[i] = i+5;
		}
		int b = 0;
	}
	
	public void method1(int a, int b) {
		int result = method2(a, b);
	}
	
	public int method2(int c, int d) {
		int ret = c + d;
		return ret;
	}
	
}
