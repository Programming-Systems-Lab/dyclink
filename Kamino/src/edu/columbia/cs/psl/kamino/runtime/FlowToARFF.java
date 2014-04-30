package edu.columbia.cs.psl.kamino.runtime;

import edu.columbia.cs.psl.kamino.Logger;

public class FlowToARFF {

    public static void logEdgeControl(String className, String methodName, String methodDescriptor, int bbFrom, int bbTo) {
        System.out.println(className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
                + methodDescriptor.replaceAll("\\p{P}", "_") + ", Control, " + bbFrom + ", " + bbTo);

        Logger.recordARFF(className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
                + methodDescriptor.replaceAll("\\p{P}", "_") + ", Control, " + bbFrom + ", " + bbTo);
    }

    public static void logEdgeControl(boolean branchTaken, String className, String methodName, String methodDescriptor, int bbFrom, int bbTaken,
            int bbNotTaken) {
        if (branchTaken) {
            System.out.println(className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
                    + methodDescriptor.replaceAll("\\p{P}", "_") + ", Control, " + bbFrom + ", " + bbTaken);

            Logger.recordARFF(className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
                    + methodDescriptor.replaceAll("\\p{P}", "_") + ", Control, " + bbFrom + ", " + bbTaken);
        } else {
            System.out.println(className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
                    + methodDescriptor.replaceAll("\\p{P}", "_") + ", Control, " + bbFrom + ", " + bbNotTaken);

            Logger.recordARFF(className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
                    + methodDescriptor.replaceAll("\\p{P}", "_") + ", Control, " + bbFrom + ", " + bbNotTaken);
        }
    }

    public static void logEdgeReadData(String className, String methodName, String methodDescriptor, int variableID, int frameID) {
        System.out.println(className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
                + methodDescriptor.replaceAll("\\p{P}", "_") + ", Read, var" + variableID + ", " + frameID);

        Logger.recordARFF(className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
                + methodDescriptor.replaceAll("\\p{P}", "_") + ", Read, var" + variableID + ", " + frameID);
    }

    public static void logEdgeWriteData(String className, String methodName, String methodDescriptor, int variableID, int frameID) {
        System.out.println(className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
                + methodDescriptor.replaceAll("\\p{P}", "_") + ", Write, var" + variableID + ", " + frameID);

        Logger.recordARFF(className.replaceAll("\\p{P}", "_") + "-" + methodName.replaceAll("\\p{P}", "_") + "-"
                + methodDescriptor.replaceAll("\\p{P}", "_") + ", Write, var" + variableID + ", " + frameID);
    }
}
