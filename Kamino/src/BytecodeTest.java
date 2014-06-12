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

/*
// class version 51.0 (51)
// access flags 0x21
public class BytecodeTest {

  // compiled from: BytecodeTest.java

  // access flags 0x0
  Ljava/lang/String; globalString

  // access flags 0x1
  public <init>()V
   L0
    LINENUMBER 1 L0
    ALOAD 0
    INVOKESPECIAL java/lang/Object.<init> ()V
   L1
    LINENUMBER 3 L1
    ALOAD 0
    LDC "world"
    PUTFIELD BytecodeTest.globalString : Ljava/lang/String;
   L2
    LINENUMBER 1 L2
    RETURN
   L3
    LOCALVARIABLE this LBytecodeTest; L0 L3 0
    MAXSTACK = 2
    MAXLOCALS = 1
    
  // access flags 0x1
  public helloWorld(Ljava/lang/String;)Ljava/lang/String;
   L0
    LINENUMBER 6 L0
    ALOAD 0
    ALOAD 1
    PUTFIELD BytecodeTest.globalString : Ljava/lang/String;
   L1
    LINENUMBER 7 L1
    NEW java/lang/StringBuilder
    DUP
    LDC "Hello "
    INVOKESPECIAL java/lang/StringBuilder.<init> (Ljava/lang/String;)V
    ALOAD 1
    INVOKEVIRTUAL java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
    INVOKEVIRTUAL java/lang/StringBuilder.toString ()Ljava/lang/String;
    ARETURN
   L2
    LOCALVARIABLE this LBytecodeTest; L0 L2 0
    LOCALVARIABLE str Ljava/lang/String; L0 L2 1
    MAXSTACK = 3
    MAXLOCALS = 2

  // access flags 0x9
  public static main([Ljava/lang/String;)V
   L0
    LINENUMBER 11 L0
    NEW BytecodeTest
    DUP
    INVOKESPECIAL BytecodeTest.<init> ()V
    ASTORE 1
   L1
    LINENUMBER 12 L1
    BIPUSH 65
    ISTORE 2
   L2
    LINENUMBER 13 L2
    ALOAD 1
    LDC "world"
    INVOKEVIRTUAL BytecodeTest.helloWorld (Ljava/lang/String;)Ljava/lang/String;
    ASTORE 3
   L3
    LINENUMBER 14 L3
    ALOAD 0
    ICONST_0
    AALOAD
    LDC "hello"
    INVOKEVIRTUAL java/lang/String.equals (Ljava/lang/Object;)Z
    IFEQ L4
***********************************
   L5
    LINENUMBER 15 L5
    ILOAD 2
    ICONST_1
    IADD
    I2C
    ISTORE 2
   L6
    LINENUMBER 16 L6
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    NEW java/lang/StringBuilder
    DUP
    LDC "Hello "
    INVOKESPECIAL java/lang/StringBuilder.<init> (Ljava/lang/String;)V
    ILOAD 2
    INVOKEVIRTUAL java/lang/StringBuilder.append (C)Ljava/lang/StringBuilder;
    INVOKEVIRTUAL java/lang/StringBuilder.toString ()Ljava/lang/String;
    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
   L7
    LINENUMBER 17 L7
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    ALOAD 3
    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
   L8
    LINENUMBER 18 L8
    GOTO L9
***********************************
   L4
    LINENUMBER 19 L4
    FRAME APPEND [BytecodeTest I java/lang/String]
    BIPUSH 97
    ISTORE 2
   L10
    LINENUMBER 20 L10
    LDC "WORLD"
    ASTORE 3
   L11
    LINENUMBER 21 L11
    GETSTATIC java/lang/System.out : Ljava/io/PrintStream;
    NEW java/lang/StringBuilder
    DUP
    ALOAD 3
    INVOKESTATIC java/lang/String.valueOf (Ljava/lang/Object;)Ljava/lang/String;
    INVOKESPECIAL java/lang/StringBuilder.<init> (Ljava/lang/String;)V
    LDC " is null"
    INVOKEVIRTUAL java/lang/StringBuilder.append (Ljava/lang/String;)Ljava/lang/StringBuilder;
    INVOKEVIRTUAL java/lang/StringBuilder.toString ()Ljava/lang/String;
    INVOKEVIRTUAL java/io/PrintStream.println (Ljava/lang/String;)V
***********************************
   L9
    LINENUMBER 23 L9
    FRAME SAME
    RETURN
   L12
    LOCALVARIABLE args [Ljava/lang/String; L0 L12 0
    LOCALVARIABLE bt LBytecodeTest; L1 L12 1
    LOCALVARIABLE character C L2 L12 2
    LOCALVARIABLE string Ljava/lang/String; L3 L12 3
    MAXSTACK = 4
    MAXLOCALS = 4
}
 
*/
