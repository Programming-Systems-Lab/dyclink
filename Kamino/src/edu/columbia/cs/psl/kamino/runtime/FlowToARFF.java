package edu.columbia.cs.psl.kamino.runtime;

import edu.columbia.cs.psl.kamino.Constants;
import edu.columbia.cs.psl.kamino.Logger;

public class FlowToARFF {

    public static void logEdgeControl(String className, String methodName, String methodDescriptor, int bbFrom, int bbTo) {
        System.out.println(className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
                + methodDescriptor.replaceAll("\\p{P}", "_") + "," + Constants.CONTROL + ",?," + bbFrom + "," + bbTo + "," + Math.abs(bbTo - bbFrom));

        Logger.recordARFF(className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
                + methodDescriptor.replaceAll("\\p{P}", "_") + "," + Constants.CONTROL + ",?," + bbFrom + "," + bbTo + "," + Math.abs(bbTo - bbFrom));
    }

    public static void logEdgeControl(boolean branchTaken, String className, String methodName, String methodDescriptor, int bbFrom, int bbTaken,
            int bbNotTaken) {
        if (branchTaken) {
            System.out.println(className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
                    + methodDescriptor.replaceAll("\\p{P}", "_") + "," + Constants.CONTROL + ",?," + bbFrom + "," + bbTaken + "," + Math.abs(bbTaken - bbFrom));

            Logger.recordARFF(className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
                    + methodDescriptor.replaceAll("\\p{P}", "_") + "," + Constants.CONTROL + ",?," + bbFrom + "," + bbTaken + "," + Math.abs(bbTaken - bbFrom));
        } else {
            System.out.println(className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
                    + methodDescriptor.replaceAll("\\p{P}", "_") + "," + Constants.CONTROL + ",?," + bbFrom + "," + bbNotTaken + "," + Math.abs(bbNotTaken - bbFrom));

            Logger.recordARFF(className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
                    + methodDescriptor.replaceAll("\\p{P}", "_") + "," + Constants.CONTROL + ",?," + bbFrom + "," + bbNotTaken + "," + Math.abs(bbNotTaken - bbFrom));
        }
    }

    public static void logEdgeReadData(String className, String methodName, String methodDescriptor, int variableID, int frameID) {
        System.out.println(className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
                + methodDescriptor.replaceAll("\\p{P}", "_") + "," + Constants.READ + "," + variableID + ",?," + frameID + ",?");

        Logger.recordARFF(className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
                + methodDescriptor.replaceAll("\\p{P}", "_") + "," + Constants.READ + "," + variableID + ",?," + frameID + ",?");
    }

    public static void logEdgeWriteData(String className, String methodName, String methodDescriptor, int variableID, int frameID) {
        System.out.println(className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
                + methodDescriptor.replaceAll("\\p{P}", "_") + "," + Constants.WRITE + "," + variableID + ",?," + frameID + ",?");

        Logger.recordARFF(className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
                + methodDescriptor.replaceAll("\\p{P}", "_") + "," + Constants.WRITE + "," + variableID + ",?," + frameID + ",?");
    }
}
