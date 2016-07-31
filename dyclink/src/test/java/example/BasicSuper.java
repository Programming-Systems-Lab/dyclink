package example;

public abstract class BasicSuper {
	
	int superId;
	
	public BasicSuper() {
		this.superMethod();
	}
	
	public BasicSuper(int i) {
		
	}
	
	public void superMethod() {
		int[][] arr = new int[3][2];
	};
	
	public void testException() throws Exception {
		System.out.println("Test 123");
		throw new Exception();
	}

}
