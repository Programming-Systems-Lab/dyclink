package edu.columbia.cs.psl.kamino;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;

public class GraphConverter {

    public final static char DATA_EDGE = 'D';
    public final static char CONTROL_EDGE = 'C';

    Map<String, DirectedSparseMultigraph<Node, Link>> method_graph_map = new HashMap<String, DirectedSparseMultigraph<Node, Link>>();  // each method has a graph for data/control flow
    Map<String, Integer> edgeName_weight_map = new HashMap<String, Integer>();  // each edge has a weight = # data and/or #control flows
    Map<Integer, Integer> variable_lastFrameID_map = new HashMap<Integer, Integer>();  // each variable has a frame/basic block where it was last seen
    Map<String, Set<Integer>> method_nodeSet_map = new HashMap<String, Set<Integer>>();  // each method has a set of nodes (basic blocks) visited in it

    public class Node {
        private int id;

        public Node(int id) {
            this.id = id;
        }

        public String toString() {
            return "n" + id;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (!(obj instanceof Node))
                return false;
            if (obj == this)
                return true;
            return this.id == ((Node) obj).id;
        }
    }

    public class Link {
        private char flow_type;
        private int weight;

        public Link(char flow_type, int weight) {
            if (flow_type == DATA_EDGE || flow_type == CONTROL_EDGE) {
                this.flow_type = flow_type;
            } else {
                System.err.println("GraphConverter: Incorrect graph flow type given as link: " + flow_type);
            }
            this.weight = weight;
        }

        public String toString() {
            return this.weight + "" + flow_type;
        }
    }

    public GraphConverter(String filename) {
        try {
            // FIXME LAN - File name must match in each place (hacky)
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
            String logEntry;

            while ((logEntry = reader.readLine()) != null) {
                String[] entryParts = logEntry.split(" ");
                String method = entryParts[1];
                DirectedSparseMultigraph<Node, Link> graph;

                // If the graph for this method already exists add to it, otherwise create one
                if (method_graph_map.containsKey(method)) {
                    graph = method_graph_map.get(method);
                } else {
                    graph = new DirectedSparseMultigraph<Node, Link>();
                }

                if (entryParts[0].equals("Control")) {
                    int bbFrom = Integer.valueOf(entryParts[2].substring(5));
                    int bbTo = Integer.valueOf(entryParts[3].substring(3));

                    // Create nodes, check they don't already exist in the graph, and if not add them
                    Node fromNode = new Node(bbFrom);
                    Node toNode = new Node(bbTo);
                    if (method_nodeSet_map.containsKey(entryParts[1])) {
                        Set<Integer> nodeSet = method_nodeSet_map.get(entryParts[1]);
                        if (nodeSet.add(bbFrom)) {
                            graph.addVertex(fromNode);
                        }
                        if (nodeSet.add(bbTo)) {
                            graph.addVertex(toNode);
                        }
                        method_nodeSet_map.put(entryParts[1], nodeSet);
                    } else {
                        Set<Integer> nodeSet = new HashSet<Integer>();
                        nodeSet.add(bbFrom);
                        nodeSet.add(bbTo);
                        method_nodeSet_map.put(entryParts[1], nodeSet);
                        graph.addVertex(fromNode);
                        if (!fromNode.equals(toNode)) {
                            graph.addVertex(toNode);
                        }
                    }

                    // Find Node that already exists in the graph (added above)
                    for (Object obj : graph.getVertices().toArray()) {
                        if (((Node) obj).equals(fromNode)) {
                            fromNode = (Node) obj;
                        }
                        if (((Node) obj).equals(toNode)) {
                            toNode = (Node) obj;
                        }
                    }

                    // If the edge already exists in the graph, remove it and add a new one with weight++
                    for (Object old_link : graph.findEdgeSet(fromNode, toNode).toArray()) {
                        if (old_link != null && ((Link) old_link).flow_type == CONTROL_EDGE) {
                            graph.removeEdge((Link) old_link);
                        }
                    }

                    // Find the old weight if it exists, update it, and add the edge with it
                    String key = "Control-" + entryParts[1] + "-frame" + bbFrom + "-frame" + bbTo;
                    Integer old_weight = edgeName_weight_map.get(key);
                    int weight = (old_weight != null) ? old_weight + 1 : 1;
                    edgeName_weight_map.put(key, weight);
                    graph.addEdge(new Link(CONTROL_EDGE, weight), fromNode, toNode, EdgeType.DIRECTED);

                } else {
                    int variableID = Integer.valueOf(entryParts[2].substring(12));
                    int bbTo = Integer.valueOf(entryParts[3].substring(6));
                    int bbFrom = Integer.valueOf(entryParts[3].substring(6));

                    // Find where this variable was seen last (if it's been seen before)
                    if (variable_lastFrameID_map.containsKey(variableID)) {
                        bbFrom = variable_lastFrameID_map.get(variableID);
                    }

                    // Create nodes, check they don't already exist in the graph, and if not add them
                    Node fromNode = new Node(bbFrom);
                    Node toNode = new Node(bbTo);
                    if (method_nodeSet_map.containsKey(method)) {
                        Set<Integer> nodeSet = method_nodeSet_map.get(method);
                        if (nodeSet.add(bbFrom)) {
                            graph.addVertex(fromNode);
                        }
                        if (nodeSet.add(bbTo)) {
                            System.out.println(bbTo);
                            graph.addVertex(toNode);
                        }
                        method_nodeSet_map.put(method, nodeSet);
                    } else {
                        Set<Integer> nodeSet = new HashSet<Integer>();
                        nodeSet.add(bbFrom);
                        nodeSet.add(bbTo);
                        method_nodeSet_map.put(method, nodeSet);

                        // If first time seeing variable, these nodes will be the same, only add once
                        graph.addVertex(fromNode);
                        if (bbTo != bbFrom) {
                            graph.addVertex(toNode);
                        }
                    }

                    // First time seeing variable there is no to/from basic block information so don't add an edge
                    if (bbTo != bbFrom) {
                        // Find Node that already exists in the graph (added above)
                        for (Object obj : graph.getVertices().toArray()) {
                            if (((Node) obj).equals(fromNode)) {
                                fromNode = (Node) obj;
                            }
                            if (((Node) obj).equals(toNode)) {
                                toNode = (Node) obj;
                            }
                        }

                        // If the edge already exists in the graph, remove it and add a new one with weight++
                        for (Object old_link : graph.findEdgeSet(fromNode, toNode).toArray()) {
                            if (old_link != null && ((Link) old_link).flow_type == DATA_EDGE) {
                                graph.removeEdge((Link) old_link);
                            }
                        }

                        // Find the old weight if it exists, update it, and add the edge with it
                        String key = "Data-" + entryParts[1] + "-frame" + bbFrom + "-frame" + bbTo;
                        Integer old_weight = edgeName_weight_map.get(key);
                        int weight = (old_weight != null) ? old_weight + 1 : 1;
                        edgeName_weight_map.put(key, weight);
                        graph.addEdge(new Link(DATA_EDGE, weight), fromNode, toNode, EdgeType.DIRECTED);

                    }
                    variable_lastFrameID_map.put(variableID, bbTo);
                }
                method_graph_map.put(entryParts[1], graph);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String toString() {
        String toReturn = "";
        for (Entry<String, DirectedSparseMultigraph<Node, Link>> entry : method_graph_map.entrySet()) {
            toReturn += entry.getKey() + ": \n" + entry.getValue() + "\n\n";
        }
        return toReturn;
    }

    public static void main(String[] args) {
        GraphConverter graph = new GraphConverter("data/BytecodeTest.output");
        System.out.println(graph);
    }
}
