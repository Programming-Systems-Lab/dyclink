package edu.columbia.cs.psl.kamino.runtime;

public class ControlLogger {
    public static void doTest(String whoDidtest) {
        System.out.println("You called doTest: " + whoDidtest);
    }

    public static void logEdge(String className, String methodName, String methodDescriptor, int bbFrom, int bbTo) {
        // TODO: LAN - switch to logging instead of printing
        System.out.println("LogEdge: " + className + "." + methodName + methodDescriptor + "  From:" + bbFrom + " To:" + bbTo);
    }

    public static void logEdge(boolean branchTaken, String className, String methodName, String methodDescriptor, int bbFrom, int bbTaken,
            int bbNotTaken) {
        // TODO: LAN - switch to logging instead of printing
        System.out.println("LogEdge: " + className + "." + methodName + methodDescriptor + "  From:" + bbFrom + " bbTaken:" + bbTaken
                + " bbNotTaken:" + bbNotTaken + " Taken:" + branchTaken);
    }

    // TODO: LAN - data flow simple case
    // print if any local variable that is primitive is read or written in more than one basic block then print it out
    // var instructions - take variable index as argument
    // local variables
    // non static methods 0=this
    // aload = objects, iload = integers
}
