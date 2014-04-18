package edu.columbia.cs.psl.kamino;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;

public class Graph {

    public final static int DATA_EDGE = 'd'; // 100
    public final static int CONTROL_EDGE = 'c'; // 99

    DirectedSparseMultigraph<Node, Link> graph = new DirectedSparseMultigraph<Node, Link>();
    Map<String, Integer> edgeName_weight_map = new HashMap<String, Integer>();

    public class Node {
        private int id;

        public Node(int id) {
            this.id = id;
        }

        public String toString() {
            return "v" + id;
        }
    }

    public class Link {
        private int flow_type, weight;

        public Link(int flow_type, int weight) {
            this.flow_type = flow_type;
            this.weight = weight;
        }

        public String toString() {
            String type = (flow_type == DATA_EDGE) ? "Data " : "Control ";
            return "e-" + type + " weight:" + this.weight;
        }
    }

    public Graph(String filename) {
        try {
            // FIXME LAN - File name must match in each place (hacky)
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
            String logEntry;
            int lastVariableID = -1;
            while ((logEntry = reader.readLine()) != null) {
                String[] entryParts = logEntry.split(" ");
                if (entryParts[0].equals("Control")) {
                    int bbFrom = Integer.valueOf(entryParts[2].substring(5));
                    int bbTo = Integer.valueOf(entryParts[3].substring(3));

                    Node fromNode = new Node(bbFrom);
                    Node toNode = new Node(bbTo);
                    graph.addVertex(fromNode);
                    graph.addVertex(toNode);

                    Integer old_weight = edgeName_weight_map.get("Control-" + bbFrom + "-" + bbTo);
                    int weight = (old_weight != null) ? old_weight + 1 : 1;
                    edgeName_weight_map.put("Control-" + bbFrom + "-" + bbTo, weight);
                    Link link = new Link(CONTROL_EDGE, weight);
                    graph.addEdge(link, fromNode, toNode, EdgeType.DIRECTED);

                } else {
                    int bbFrom = (lastVariableID == -1) ? Integer.valueOf(entryParts[3].substring(6)) : lastVariableID;
                    int bbTo = Integer.valueOf(entryParts[3].substring(6));

                    Node fromNode = new Node(bbFrom);
                    Node toNode = new Node(bbTo);
                    graph.addVertex(toNode);

                    Integer old_weight = edgeName_weight_map.get("Data-" + bbFrom + "-" + bbTo);
                    int weight = (old_weight != null) ? old_weight + 1 : 1;
                    edgeName_weight_map.put("Data-" + bbFrom + "-" + bbTo, weight);
                    Link link = new Link(DATA_EDGE, weight);
                    graph.addEdge(link, fromNode, toNode, EdgeType.DIRECTED);
                    lastVariableID = bbTo;
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public String toString() {
        String toReturn = "";
        for (Entry<String, Integer> entry : edgeName_weight_map.entrySet()) {
            toReturn += entry.getKey() +": " + entry.getValue() +"\n";
        }
        return toReturn;
    }

    public static void main(String[] args) {
        Graph graph = new Graph("data/BytecodeTest.output");
        System.out.println(graph);
    }
}
