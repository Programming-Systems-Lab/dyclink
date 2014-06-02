package edu.columbia.cs.psl.kamino.runtime;

import edu.columbia.cs.psl.kamino.Constants;
import edu.columbia.cs.psl.kamino.Logger;

public class FlowToARFF {

	public static void logEdgeControl(String className, String methodName, String methodDescriptor, int bbFrom, int bbTo) {
		String output = className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
		        + methodDescriptor.replaceAll("\\p{P}", "_") + "," + Constants.CONTROL + ",?," + bbFrom + "," + bbTo + "," + Math.abs(bbTo - bbFrom);
		System.out.println(output);
		Logger.recordARFF(output);
	}

	public static void logEdgeControl(boolean branchTaken, String className, String methodName, String methodDescriptor, int bbFrom, int bbTaken,
	        int bbNotTaken) {
		if (branchTaken) {
			String output = className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
			        + methodDescriptor.replaceAll("\\p{P}", "_") + "," + Constants.CONTROL + ",?," + bbFrom + "," + bbTaken + ","
			        + Math.abs(bbTaken - bbFrom);
			System.out.println(output);
			Logger.recordARFF(output);
		} else {
			String output = className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
			        + methodDescriptor.replaceAll("\\p{P}", "_") + "," + Constants.CONTROL + ",?," + bbFrom + "," + bbNotTaken + ","
			        + Math.abs(bbNotTaken - bbFrom);
			System.out.println(output);
			Logger.recordARFF(output);
		}
	}

	public static void logEdgeReadData(String className, String methodName, String methodDescriptor, int variableID, int frameID) {
		String output = className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
		        + methodDescriptor.replaceAll("\\p{P}", "_") + "," + Constants.READ + "," + variableID + ",?," + frameID + ",?";
		System.out.println(output);
		Logger.recordARFF(output);
	}

	public static void logEdgeWriteData(String className, String methodName, String methodDescriptor, int variableID, int frameID) {
		String output = className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
		        + methodDescriptor.replaceAll("\\p{P}", "_") + "," + Constants.WRITE + "," + variableID + ",?," + frameID + ",?";
		System.out.println(output);
		Logger.recordARFF(output);
	}
}
