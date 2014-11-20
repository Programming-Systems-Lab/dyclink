package cc.testbase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

public class YAMethod {

	public void addSomething(ArrayList<Integer> target) {
		target.add(3);
	}
	
	public static void main(String[] ars) {
		YAMethod ym = new YAMethod();
		int b = TemplateMethod.parentVar;
		
		HashMap<String, Object> h1 = new HashMap<String, Object>();
		HashMap<String, Object> h2 = new HashMap<String, Object>();
		
		System.out.println(h1.keySet().equals(h2.keySet()));
	}

}
