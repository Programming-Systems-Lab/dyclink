package edu.columbia.psl.cc.test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import edu.columbia.psl.cc.annot.analyzeClass;
import edu.columbia.psl.cc.annot.extractTemplate;
import edu.columbia.psl.cc.annot.testTemplate;

@analyzeClass
public class TemplateObjMethod {
	
	private static AtomicInteger _mib_id = new AtomicInteger();
	
	private int mib_id = _mib_id.getAndIncrement();
	
	public TemplateObjMethod() {
		mib_id = _mib_id.getAndIncrement();
	}
		
	@extractTemplate
	public Integer sequenceObjTemplate(Integer a, Integer b, Integer c) {
		Integer d = a + 19;
		Integer e = d + b;
		Integer obj = e + c;
		return obj;
	}
	
	@extractTemplate
	public Double parallelObjTemplate(Double a, Double b, Double c) {
		Double d = a + b;
		Double e = c + 3;
		Double f = d + e;
		return f;
	}
	
	@testTemplate
	public String testObjSequence(String s1, String s2) {
		String s3 = s1 + " columbia";
		String s4 = s3 + s2;
		String s5 = s4 + "!!!";
		return s5;
	}
	
	public static void main(String[] args) {
		int[][] test = {{1, 2}, {3, 4}};
		System.out.println(Arrays.toString(test));
		Student a = new Student();
		a.id = 1;
		a.name = "test123";
		
		Student b = new Student();
		b.id = 1;
		b.name = "test123";
		System.out.println(a.equals(b));
		System.out.println("a hash: " + a.hashCode());
		System.out.println("b hash: " + b.hashCode());
		int aHash = System.identityHashCode(a);
		a.getName();
		int bHash = System.identityHashCode(b);
		System.out.println("a jvm hash: " + aHash);
		System.out.println("b jvm hash: " + bHash);
		
		Object obj = new Object();
	}
	
	public static class Student {
		int id;
		String name;
		
		public String getName() {
			return name;
		}
		
		@Override
		public String toString() {
			return this.id + " " + this.name;
		}
		
		@Override
		public boolean equals(Object tmp) {
			if (!(tmp instanceof Student))
				return false;
			
			Student tmpStudent = (Student)tmp;
			if (tmpStudent.toString().equals(this.toString()))
				return true;
			else
				return false;
		}
		
		@Override
		public int hashCode() {
			return this.toString().hashCode();
		}
	}
}
