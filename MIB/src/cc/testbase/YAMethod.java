package cc.testbase;

import java.util.ArrayList;
import java.util.Stack;

public class YAMethod {

	public void addSomething(ArrayList<Integer> target) {
		target.add(3);
	}
	
	public static void main(String[] ars) {
		Stack<Integer> testStack = new Stack<Integer>();
		testStack.push(0);
		testStack.push(1);
		testStack.push(2);
		System.out.println(testStack.get(2));
		System.out.println(testStack.peek());
		(new Stack()).push(1);
	}

}
