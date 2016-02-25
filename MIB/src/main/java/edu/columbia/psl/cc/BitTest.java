package edu.columbia.psl.cc;

import java.util.Arrays;

public class BitTest {
	
	public static void main(String[] args) {
		int bitmask = 0x000E;
		int val = 0x2222;
		System.out.println("Bit mask: " + bitmask);
		System.out.println(Integer.toBinaryString(bitmask));
		System.out.println("Val: " + val);
		System.out.println(Integer.toBinaryString(val));
		
		int maskResult = (val & bitmask);
		System.out.println("Mask result: " + maskResult);
		System.out.println(Integer.toBinaryString(maskResult));
		
		int[] bits = new int[4];
		for (int i = bits.length-1; i >= 0; i--) {
			System.out.println(Integer.toBinaryString(1<<i));
			System.out.println(Integer.toBinaryString(bitmask));
			bits[i] = ((bitmask & (1<<i)) > 0) ? 1: 0;
		}
		
		for (int i = 0; i < bits.length; i++) {
			System.out.println(bits[i]);
		}
		System.out.println(Arrays.toString(bits));
		
		System.out.println(Integer.toBinaryString(-1));
		System.out.println(-1 >>> 30);
		System.out.println(Integer.toBinaryString(-1 >>> 30));
		
		System.out.println("11111111111111111111111111111111".length());
	}

}
