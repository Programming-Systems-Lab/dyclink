package example;

public class BasicTest {
	
	public int sum(int i, int j) {
		return i + j + 5;
	}
	
	public int sum(int j) {
		for (int i = 0; i < j; i++) {
			j++;
		}
		return j;
	}

}
