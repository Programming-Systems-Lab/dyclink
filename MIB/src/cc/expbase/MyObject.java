package cc.expbase;

public class MyObject {
	
	private int[] myArray;
	
	public int setAndSum(int[] arr) {
		this.myArray = arr;
		
		int ret = 0;
		for (int i = 0; i < arr.length; i++) {
			ret += arr[i];
		}
		
		return ret;
	}
	
	public static void main(String[] args) {
		MyObject mo = new MyObject();
		int[] arr = {1, 2};
		
		mo.setAndSum(arr);
	}

}
