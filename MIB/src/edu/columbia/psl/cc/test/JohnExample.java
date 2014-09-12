package edu.columbia.psl.cc.test;

import edu.columbia.psl.cc.annot.analyzeClass;
import edu.columbia.psl.cc.annot.extractTemplate;
import edu.columbia.psl.cc.annot.testTemplate;

@analyzeClass
public class JohnExample {
	
	@extractTemplate
	public void foo1(long[] arr1, long[] arr2) {
		for (int i = 0; i < arr1.length; i++) {
			if (i % 4 == 0)
				arr1[i] += i*6;
			else {
				arr1[i] += i;
			}
		}
		
		for (int i = 0; i < arr2.length; i++) {
			arr2[i] *= i * (i-1) / 6;
		}
	}
	
	@extractTemplate
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
	
	@testTemplate
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

}
