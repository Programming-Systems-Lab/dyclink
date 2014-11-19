package cc.testbase;

import java.util.ArrayList;
import java.util.Stack;

public class YAMethod {

	public void addSomething(ArrayList<Integer> target) {
		target.add(3);
	}
	
	public static void main(String[] ars) {
		YAMethod ym = new YAMethod();
		int b = TemplateMethod.parentVar;
	}

}
