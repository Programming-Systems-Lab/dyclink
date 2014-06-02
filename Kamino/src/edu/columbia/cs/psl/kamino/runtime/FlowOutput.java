package edu.columbia.cs.psl.kamino.runtime;

import edu.columbia.cs.psl.kamino.Constants;
import edu.columbia.cs.psl.kamino.Logger;

public class FlowOutput {

	public static void logEdgeControl(String className, String methodName, String methodDescriptor, int bbFrom, int bbTo) {
		Logger.recordOutput(className + "." + methodName + methodDescriptor + "," + Constants.CONTROL + "," + bbFrom + "," + bbTo);
	}

	public static void logEdgeControl(boolean branchTaken, String className, String methodName, String methodDescriptor, int bbFrom, int bbTaken,
	        int bbNotTaken) {
		if (branchTaken) {
			Logger.recordOutput(className + "." + methodName + methodDescriptor + "," + Constants.CONTROL + "," + bbFrom + "," + bbTaken);
		} else {
			Logger.recordOutput(className + "." + methodName + methodDescriptor + "," + Constants.CONTROL + "," + bbFrom + "," + bbNotTaken);
		}
	}

	public static void logEdgeReadData(String className, String methodName, String methodDescriptor, int variableID, int frameID) {
		Logger.recordOutput(className + "." + methodName + methodDescriptor + "," + Constants.READ + "," + variableID + "," +frameID);
	}

	public static void logEdgeWriteData(String className, String methodName, String methodDescriptor, int variableID, int frameID) {
		Logger.recordOutput(className + "." + methodName + methodDescriptor + "," + Constants.WRITE + "," + variableID + "," + frameID);
	}
}
