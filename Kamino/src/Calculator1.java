public class Calculator1 {
	
	public static double calcAverage(int[] array) {
		int sum = 0;
		for (int i = 0; i < array.length; i++) {
			sum += array[i];
		}
		return sum / array.length;
	}

	public static void main(String[] args) {
		int array1[] = { 1, 2, 3, 4 };
		int array2[] = { 10, 20, 30, 40 };
		System.out.println(calcAverage(array1));
		System.out.println(calcAverage(array2));
	}
}