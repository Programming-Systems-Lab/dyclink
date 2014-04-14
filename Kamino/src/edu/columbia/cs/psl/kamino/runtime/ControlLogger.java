package edu.columbia.cs.psl.kamino.runtime;

public class ControlLogger {
	public static void doTest(String whoDidtest) {
		System.out.println("You called doTest: " + whoDidtest);
	}

	public static void logEdgeControl(String className, String methodName, String methodDescriptor, String frame, int bbFrom, int bbTo) {
		// FIXME: LAN - switch to logging instead of printing
		System.out.println("1  LogEdgeControl: " + frame + className + "." + methodName + methodDescriptor + "  From:" + bbFrom + " To:" + bbTo);
	}

	public static void logEdgeControl(boolean branchTaken, String className, String methodName, String methodDescriptor, int bbFrom, int bbTaken,
	        int bbNotTaken) {
		// FIXME: LAN - switch to logging instead of printing
		System.out.println("2  LogEdgeControl: JUMP " + className + "." + methodName + methodDescriptor + "  From:" + bbFrom + " bbTaken:" + bbTaken
		        + " bbNotTaken:" + bbNotTaken + " Taken:" + branchTaken);
	}

	public static void logEdgeReadData(String className, String methodName, String methodDescriptor, int bbFrom, int bbTo) {
		// FIXME: LAN - switch to logging instead of printing
		System.out.println("3  LogEdgeReadData: " + className + "." + methodName + methodDescriptor + "  From:" + bbFrom + " To:" + bbTo);
	}

	public static void logEdgeWriteData(String className, String methodName, String methodDescriptor, int bbFrom, int bbTo) {
		// FIXME: LAN - switch to logging instead of printing
		System.out.println("4  LogEdgeWriteData: " + className + "." + methodName + methodDescriptor + "  From:" + bbFrom + " To:" + bbTo);
	}
}
