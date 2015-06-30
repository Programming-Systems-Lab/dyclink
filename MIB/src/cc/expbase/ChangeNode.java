package cc.expbase;

import java.util.ArrayList;

public class ChangeNode {
	
	public static void main(String[] args) {
		ChangeNode cn = new ChangeNode();
		cn.changeList();
	}
	
	public void changeList() {
		ArrayList<Integer> a = null;
		ArrayList<Integer> javaList = new ArrayList<Integer>();
		ArrayList<Integer> myList = new MyList<Integer>();
		
		for (int i = 0; i < 5; i++) {
			if (i % 2 == 0) {
				a = myList; //3 times
			} else {
				a = javaList; //2 times
			}
			a.add(i);
		}
		System.out.println(a);
	}
	
	
	public static class MyList<E> extends ArrayList<E> {
		
		public int touched = 0;
		
		@Override
		public boolean add(E obj) {
			if (this.size() %2 == 0)
				this.touched++;
			return super.add(obj);
		}
	}
}
