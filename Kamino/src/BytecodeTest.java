public class BytecodeTest {
	
	String globalString = "world";
	
	public String helloWorld(String str) {
		globalString = str;
		return "Hello " + str;
	}
	
	public static void main(String[] args) {
		BytecodeTest bt = new BytecodeTest();
		char character = 'A';
		String string = bt.helloWorld("world");
		if (args[0].equals("hello")) {
			character++;
			System.out.println("Hello " + character);
			System.out.println(string);
		} else {
			character = 'a';
			string = "WORLD";
			System.out.println(string + " is null");
		}
	}
}
