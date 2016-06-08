package cc.expbase;

public class TestRun {
	
	public static int sum1(int[] arr) {
		int ret = 0;
		for (int i = 0; i < arr.length; i++) {
			ret += arr[i];
		}
		return ret;
	}
	
	public static long sum2(long[] data) {
		int j = 0;
		long sum = 0;
		while (j < data.length) {
			sum += data[j++];
		}
		
		return sum;
	}
	
	public static void main(String[] args) {
		int[]arr = {1, 2, 3};
		long[]data = {4L, 5L, 6L};
		
		System.out.println(sum1(arr));
		System.out.println(sum2(data));
	}

}
