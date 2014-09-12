package edu.columbia.psl.cc.test;

import java.util.Arrays;

import org.objectweb.asm.Opcodes;

public class TestClass {
	
	public static void main(String[] args) {
		int i = 10;
		int j = 0;
		int k = (i ^ 2);
		k++;
		
		int[] a = new int[3];
		
		for (int b = 0 ; b < 10; b++) {
			System.out.println("For loop");
		}
		
		boolean probe = true;
		while(probe) {
			System.out.println("While loop");
			probe = false;
		}
		
		do {
			System.out.println("At least print once");
		} while(probe);
		
		int[] test= {0, 0, 0};
		test[0]++;
		System.out.println(Arrays.toString(test));
	}

}
