package edu.columbia.cs.psl.kamino.runtime;

import java.io.File;

import edu.columbia.cs.psl.kamino.Logger;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;

public class ControlLogger {

    public final static int DATA_EDGE = 'd';
    public final static int CONTROL_EDGE = 'c';

    static Logger logger = new Logger(new File("data/BytecodeTest.output"));
    DirectedSparseMultigraph<Integer, Integer> graph = new DirectedSparseMultigraph<Integer, Integer>();

    public static void doTest(String whoDidtest) {
        System.out.println("You called doTest:" + whoDidtest);
    }

    public void logEdgeControl(String className, String methodName, String methodDescriptor, int bbFrom, int bbTo) {
        System.out.println("LogEdgeControl:" + className + "." + methodName + methodDescriptor + "  From:" + bbFrom + " To:" + bbTo);
        logger.record("Control, " + className + "." + methodName + methodDescriptor + "  From:" + bbFrom + " To:" + bbTo + "\n");

        addToGraph(CONTROL_EDGE, bbFrom, bbTo);
    }

    public void addToGraph(int type, int from, int to) {
        graph.addVertex(from);
        graph.addVertex(to);
        graph.addEdge(type, from, to, EdgeType.DIRECTED);
    }
    
    public static void logEdgeControl(boolean branchTaken, String className, String methodName, String methodDescriptor, int bbFrom, int bbTaken,
            int bbNotTaken) {
        System.out.println("LogEdgeControl:" + className + "." + methodName + methodDescriptor + "  From:" + bbFrom + " bbTaken:" + bbTaken
                + " bbNotTaken:" + bbNotTaken + " Taken:" + branchTaken);

//        graph.addVertex(bbFrom);
        if (branchTaken) {
            logger.record("Control, " + className + "." + methodName + methodDescriptor + "  From:" + bbFrom + " To:" + bbTaken + "\n");
//            graph.addVertex(bbTaken);
//            graph.addEdge(CONTROL_EDGE, bbFrom, bbTaken, EdgeType.DIRECTED);
        } else {
            logger.record("Control, " + className + "." + methodName + methodDescriptor + "  From:" + bbFrom + " To:" + bbNotTaken + "\n");
//            graph.addVertex(bbNotTaken);
//            graph.addEdge(CONTROL_EDGE, bbFrom, bbNotTaken, EdgeType.DIRECTED);
        }
    }

    public static void logEdgeReadData(String className, String methodName, String methodDescriptor, int variableID, int frameID) {
        System.out.println("LogEdgeReadData:" + className + "." + methodName + methodDescriptor + "  variable_id:" + variableID + " Frame:" + frameID);
        logger.record("DataRead, " + className + "." + methodName + methodDescriptor + "  variable_id:" + variableID + " Frame:" + frameID + "\n");
        
//        graph.addVertex(bbFrom);
//        graph.addVertex(frameID);
//        graph.addEdge(DATA_EDGE, bbFrom, frameID, EdgeType.DIRECTED);
    }

    public static void logEdgeWriteData(String className, String methodName, String methodDescriptor, int variableID, int frameID) {
        System.out.println("LogEdgeWriteData:" + className + "." + methodName + methodDescriptor + "  variable_id:" + variableID + " Frame:" + frameID);
        logger.record("DataWrite, " + className + "." + methodName + methodDescriptor + "  variable_id:" + variableID + " Frame:" + frameID + "\n");
        
//        graph.addVertex(bbFrom);
//        graph.addVertex(frameID);
//        graph.addEdge(DATA_EDGE, bbFrom, frameID, EdgeType.DIRECTED);
    }
}
