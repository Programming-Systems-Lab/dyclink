package cc.expbase;

public class MyObject {
	
	private int[] myArray;
	
	private int myI;
	
	private int myJ;
	
	public int add(int i, int j) {
		int ret = 0;
		if (i > 5) {
			ret = i + j;
		} else {
			ret = i - 5;
			ret = j + ret;
			ret = i * ret;
		}
		return ret;
	}
	
	public int addObj(MyObject mo) {
		if (mo.myI <= 5) {
			int ret = mo.myI - 5 + mo.myJ;
			ret *= mo.myI;
			return ret;
		} else {
			int ret = mo.myI + mo.myJ;
			return ret;
		}
	}
	
	public static void main(String[] args) {
		MyObject mo = new MyObject();
		//int[] arr = {1, 2};
		//mo.setAndSum(arr);
		/*for (int i = 0; i < 10; i++) {
			mo.myI = i;
			mo.myJ = 1;
			System.out.println(mo.addObj(mo));
			System.out.println(mo.add(i, 1));
		}*/
		
		mo.myI = 2;
		mo.myJ = 1;
		System.out.println(mo.addObj(mo));
		System.out.println(mo.add(2, 1));
		
	}

}

