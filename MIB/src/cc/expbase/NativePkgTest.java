package cc.expbase;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class NativePkgTest {
		
	private int sumArray(int[] arr) {
		int sum = 0;
		for (int i = 0; i < arr.length; i++) {
			sum += arr[i];
		}
		return sum;
	}
	
	private int sumList(List<Integer> list) {
		int sum = 0;
		for (int i = 0; i < list.size(); i++) {
			sum += list.get(i);
		}
		return sum;
	}
	
	private int sumArrayByIt(int[] arr) {
		int sum = 0;
		for (Integer i: arr) {
			sum += i;
		}
		return sum;
	}
	
	private int sumListByIt(List<Integer> list) {
		int sum = 0;
		for (Integer i: list) {
			sum += i;
		}
		return sum;
	}
	
	private int sumSetByIt(Set<Integer> set) {
		int sum = 0;
		for (Integer i: set) {
			sum += i;
		}
		return sum;
	}
	
	private String constructString(String s) {
		String ret = "";
		for (int i = 0; i < s.length(); i++) {
			ret += s.charAt(i);
		}
		return ret;
	}
	
	public static void main(String[] args) {
		int[] arr = new int[]{1, 2, 3};
		
		List<Integer> list = new ArrayList<Integer>();
		list.add(1);
		list.add(2);
		list.add(3);
		
		Set<Integer> set = new HashSet<Integer>();
		set.add(1);
		set.add(2);
		set.add(3);
		
		String s = "123";
		
		NativePkgTest npt = new NativePkgTest();
		System.out.println(npt.sumArray(arr));
		System.out.println(npt.sumList(list));
		System.out.println(npt.sumListByIt(list));
		System.out.println(npt.sumArrayByIt(arr));
		System.out.println(npt.sumSetByIt(set));
		System.out.println(npt.constructString(s));
	}

}
