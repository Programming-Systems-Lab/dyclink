package edu.columbia.cs.psl.kamino;

public class FlowEntry {

    String methodInfo;
    char flowType;
    int variableID = -1;
    int fromFrame;
    int toFrame;
    boolean controlFlowOnly = true;

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
    
    /**
     * 
     * @param controlFlowOnly 
     *             The boolean value indicating whether to display control flow information only 
     */
    public void setControlFlowOnly(boolean controlFlowOnly) {
        this.controlFlowOnly = controlFlowOnly;
    }

    public String toString() {
        // FIXME LAN - Only outputting control flow information
        if (controlFlowOnly) {
            if (this.flowType == Constants.CONTROL) {
                return String.valueOf(this.flowType) + this.toFrame;
            }
        } else {
            return String.valueOf(this.flowType) + this.fromFrame + ((this.variableID >= 0) ? "V" + variableID : "");

        }
        return "";

        // return String.valueOf(this.flowType) + this.fromFrame + "~" + this.toFrame + ((this.variableID >= 0) ? "V" + variableID : "");

    }
}