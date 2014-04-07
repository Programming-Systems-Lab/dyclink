public class BytecodeTest {
    public static void main(String[] args) {
        char character = 'A';
        if (args[0].equals("hello")) {
            // FIXME - LAN try incrementing
            // character++;
            System.out.println("Hello " + character);
        } else {
        	// FIXME: LAN - this part is not working : should show a write for below
            character = 'a';
            // FIXME: LAN - this part is not working : should show a read for below
            System.out.println(character + " is null");
        }
    }
}

/*
 * // class version 51.0 (51)
 * // access flags 0x21
 * public class BytecodeTest {
 * 
 * // compiled from: BytecodeTest.java
 * 
 * // access flags 0x1
 * public <init>()V
 * L0
 * LINENUMBER 2 L0
 * ALOAD 0
 * INVOKESPECIAL java/lang/Object.<init> ()V
 * RETURN
 * L1
 * LOCALVARIABLE this LBytecodeTest; L0 L1 0
 * MAXSTACK = 1
 * MAXLOCALS = 1
 * 
 * // access flags 0x9
 * public static main([Ljava/lang/String;)V
 * L0
 * LINENUMBER 4 L0
 * BIPUSH 65
 * ISTORE 1
 * L1
 * LINENUMBER 5 L1
 * ALOAD 0
 * ICONST_0
 * AALOAD
 * LDC "hello"
 * INVOKEVIRTUAL java/lang/String.equals (Ljava/lang/Object;)Z
 * IFEQ L2
 * L3
 * LINENUMBER 6 L3
 * GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
 * NEW java/lang/StringBuilder
 * DUP
 * LDC "Hello "
 * INVOKESPECIAL java/lang/StringBuilder.<init> (Ljava/lang/String;)V
 * ILOAD 1
 * INVOKEVIRTUAL java/lang/StringBuilder.append (C)Ljava/lang/StringBuilder;
 * INVOKEVIRTUAL java/lang/StringBuilder.toString ()Ljava/lang/String;
 * INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
 * GOTO L4
 * L2
 * LINENUMBER 8 L2
 * FRAME APPEND [I]
 * GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
 * NEW java/lang/StringBuilder
 * DUP
 * ILOAD 1
 * INVOKESTATIC java/lang/String.valueOf (C)Ljava/lang/String;
 * INVOKESPECIAL java/lang/StringBuilder.<init> (Ljava/lang/String;)V
 * LDC " is null"
 * INVOKEVIRTUAL java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
 * INVOKEVIRTUAL java/lang/StringBuilder.toString ()Ljava/lang/String;
 * INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
 * L4
 * LINENUMBER 9 L4
 * FRAME SAME
 * RETURN
 * L5
 * LOCALVARIABLE args [Ljava/lang/String; L0 L5 0
 * LOCALVARIABLE character C L1 L5 1
 * MAXSTACK = 4
 * MAXLOCALS = 2
 * }
 */

