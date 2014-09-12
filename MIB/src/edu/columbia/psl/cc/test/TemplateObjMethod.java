package edu.columbia.psl.cc.test;

import edu.columbia.psl.cc.annot.analyzeClass;
import edu.columbia.psl.cc.annot.extractTemplate;
import edu.columbia.psl.cc.annot.testTemplate;

@analyzeClass
public class TemplateObjMethod {
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
}
