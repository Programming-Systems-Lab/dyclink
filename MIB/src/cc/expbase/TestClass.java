package cc.expbase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.Opcodes;

public class TestClass extends TemplateParent {
	
	public void setList(ArrayList list) {
		
	}
	
	public static void main(String[] args) {
		int mm = 0;
		TestClass tc = new TestClass();
		List l = null;
		if (mm == 0) {
			l = new ArrayList();
		} else {
			l = new LinkedList();
		}
		tc.setList(l);
		
		ArrayList<String> list = new ArrayList<String>();
		tc.setList(list);
		
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
