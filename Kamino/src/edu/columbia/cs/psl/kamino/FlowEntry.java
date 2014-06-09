package edu.columbia.cs.psl.kamino;

public class FlowEntry {

    String methodInfo;
    char flowType;
    int variableID = -1;
    int fromFrame;
    int toFrame;

    public FlowEntry() {
    }

    public FlowEntry(String methodInfo, char flowType, int variableID, int fromFrame, int toFrame) {
        this.methodInfo = methodInfo;
        this.flowType = flowType;
        this.variableID = variableID;
        this.fromFrame = fromFrame;
        this.toFrame = toFrame;
    }

    public void setFromFrame(int frameID) {
        this.fromFrame = frameID;
    }

    public static char determineFlowType(String entryStr) {
        return entryStr.split(",")[1].charAt(0);
    }

    public String toString() {
        return String.valueOf(this.flowType) + fromFrame + "~" + toFrame + ((variableID >= 0) ? "V" + variableID : "");
        //        return this.methodInfo + "," + this.flowType + ((variableID == -1) ? "v" + variableID : "") + "f" + fromFrame + "->" + "f" + toFrame;
    }
}