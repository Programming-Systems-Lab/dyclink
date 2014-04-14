public class BytecodeTest {
	public static void main(String[] args) {
		char character = 'A';
		if (args[0].equals("hello")) {
			System.out.println("Hello " + character);
//			character++;
//			System.out.println("Hello " + character);
		} else {
			character = 'a';
			System.out.println(character + " is null");
		}
	}
}

/*
  // access flags 0x9
  public static main([Ljava/lang/String;)V
   L0
    LINENUMBER 3 L0
    BIPUSH 65
    ISTORE 1
   L1
    LINENUMBER 4 L1
    ALOAD 0
    ICONST_0
    AALOAD
    LDC "hello"
    INVOKEVIRTUAL java/lang/String.equals (Ljava/lang/Object;)Z
    IFEQ L2
   L3
    LINENUMBER 5 L3
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    NEW java/lang/StringBuilder
    DUP
    LDC "Hello "
    INVOKESPECIAL java/lang/StringBuilder.<init> (Ljava/lang/String;)V
    ILOAD 1
    INVOKEVIRTUAL java/lang/StringBuilder.append (C)Ljava/lang/StringBuilder;
    INVOKEVIRTUAL java/lang/StringBuilder.toString ()Ljava/lang/String;
    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
   L4
    LINENUMBER 8 L4
    GOTO L5
   L2
    LINENUMBER 9 L2
   FRAME APPEND [I]
    BIPUSH 97
    ISTORE 1
   L6
    LINENUMBER 10 L6
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    NEW java/lang/StringBuilder
    DUP
    ILOAD 1
    INVOKESTATIC java/lang/String.valueOf (C)Ljava/lang/String;
    INVOKESPECIAL java/lang/StringBuilder.<init> (Ljava/lang/String;)V
    LDC " is null"
    INVOKEVIRTUAL java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
    INVOKEVIRTUAL java/lang/StringBuilder.toString ()Ljava/lang/String;
    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
   L5
    LINENUMBER 12 L5
   FRAME SAME
    RETURN
   L7
    LOCALVARIABLE args [Ljava/lang/String; L0 L7 0
    LOCALVARIABLE character C L1 L7 1
    MAXSTACK = 4
    MAXLOCALS = 2

 */