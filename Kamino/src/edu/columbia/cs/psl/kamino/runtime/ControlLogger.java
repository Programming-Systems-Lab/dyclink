package edu.columbia.cs.psl.kamino.runtime;

public class ControlLogger {
	public static void doTest(String whoDidtest) {
		System.out.println("You called doTest: " + whoDidtest);
	}

	public static void logEdgeControl(String className, String methodName, String methodDescriptor, int bbFrom, int bbTo) {
		// FIXME: LAN - switch to logging instead of printing
		System.out.println("LogEdgeControl: " + className + "." + methodName + methodDescriptor + "  From:" + bbFrom + " To:" + bbTo);
	}

	public static void logEdgeControl(boolean branchTaken, String className, String methodName, String methodDescriptor, int bbFrom, int bbTaken,
	        int bbNotTaken) {
		// FIXME: LAN - switch to logging instead of printing
		System.out.println("LogEdgeControl: " + className + "." + methodName + methodDescriptor + "  From:" + bbFrom + " bbTaken:" + bbTaken
		        + " bbNotTaken:" + bbNotTaken + " Taken:" + branchTaken);
	}

	public static void logEdgeReadData(String className, String methodName, String methodDescriptor, int bbFrom, int bbTo) {
		// FIXME: LAN - switch to logging instead of printing
		System.out.println("LogEdgeReadData: " + className + "." + methodName + methodDescriptor + "  From:" + bbFrom + " To:" + bbTo);
	}

	public static void logEdgeWriteData(String className, String methodName, String methodDescriptor, int bbFrom, int bbTo) {
		// FIXME: LAN - switch to logging instead of printing
		System.out.println("LogEdgeWriteData: " + className + "." + methodName + methodDescriptor + "  From:" + bbFrom + " To:" + bbTo);
	}

	// FIXME: LAN - data flow simple case
	// print if any local variable that is primitive is read or written in more than one 
	// basic block then print it out    
}
