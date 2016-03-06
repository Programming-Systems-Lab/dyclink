package cc.expbase;

public class MyObject {
	
	private int[] myArray;
	
	private int myI;
	
	private int myJ;
	
	public int add(int i, int j) {
		int ret = 0;
		if (i > 3) {
			ret = i + j;
		} else {
			ret = i - 5;
			ret = j + ret;
			ret = i * ret;
		}
		//System.out.println("Success to add");
		return ret;
	}
	
	public int loopAdd() {
		int ret = 0;
		for (int i = 0; i < 7; i++) {
			ret += this.add(i, 1);
		}
		//System.out.println("Success to loopadd");
		return ret;
	}
		
	public static void main(String[] args) {
		MyObject mo = new MyObject();
		System.out.println(mo.loopAdd());
	}
}

