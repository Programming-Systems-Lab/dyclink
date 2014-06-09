package edu.columbia.cs.psl.kamino;

public class ControlFlowEntry extends FlowEntry {

    public ControlFlowEntry(String entryStr) {
        String[] splitArray = entryStr.split(",");
        this.methodInfo = splitArray[0];
        this.flowType = splitArray[1].charAt(0);
        this.fromFrame = Integer.valueOf(splitArray[2]);
        this.toFrame = Integer.valueOf(splitArray[3]);
    }
}
