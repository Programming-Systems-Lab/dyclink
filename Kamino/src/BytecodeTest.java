public class BytecodeTest {
	public static void main(String[] args) {
		char character = 'A';
		String string = "world";
		if (args[0].equals("hello")) {
			character++;
			System.out.println("Hello " + character);
			System.out.println("Hello " + string);
		} else {
			character = 'a';
			string = "WORLD";
			System.out.println(string + " is null");
		}
	}
}
