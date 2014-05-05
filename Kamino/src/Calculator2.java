public class Calculator2 {
	
	public static void main(String[] args) {
		int array1[] = { 1, 2, 3, 4 };
		int array2[] = { 10, 20, 30, 40 };
		int sum1 = 0;
		int sum2 = 0;
		
		for (int i = 0; i < 4; i++) {
			sum1 = array1[i] + sum1;
		}
		for (int i = 0; i < 4; i++) {
			sum2 = array2[i] + sum2;
		}
		
		System.out.println(sum1 / 4);
		System.out.println(sum2 / 4);
	}
}
