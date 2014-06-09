package edu.columbia.cs.psl.kamino;

public class DataFlowEntry extends FlowEntry {

    public DataFlowEntry(String entryStr) {
        String[] splitArray = entryStr.split(",");
        this.methodInfo = splitArray[0];
        this.flowType = splitArray[1].charAt(0);
        this.variableID = Integer.valueOf(splitArray[2]);
        this.toFrame = Integer.valueOf(splitArray[3]);
    }
}
