public class BytecodeTest {
	public static void main(String[] args) {
		char character = 'A';
		if (args[0].equals("hello")) {
			System.out.println("Hello " + character);
			character++;
			System.out.println("Hello " + character);
		} else {
			character = 'a';
			System.out.println(character + " is null");
		}
	}
}
