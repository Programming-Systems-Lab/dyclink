package cc.expbase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

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
		int[] aClone = a.clone();
		System.out.println("Check type: " + Type.getType("java/util/ArrayList").getSort());
		System.out.println("Array type: " + Type.ARRAY);
		System.out.println("Object type: " + Type.OBJECT);
		System.out.println("Boolean type: " + Type.BOOLEAN);
		System.out.println("Byte type: " + Type.BYTE);
		System.out.println("Short type: " + Type.SHORT);
		System.out.println("Int type: " + Type.INT);
		System.out.println("Long type: " + Type.LONG);
		System.out.println("Float type: " + Type.FLOAT);
		System.out.println("Double type: " + Type.DOUBLE);
		System.out.println("Method type: " + Type.METHOD);
		System.out.println("Void type: " + Type.VOID);
		System.out.println("Char type: " + Type.CHAR);
		
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
