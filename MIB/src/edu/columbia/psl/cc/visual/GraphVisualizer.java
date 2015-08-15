package edu.columbia.psl.cc.visual;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.swingViewer.View;
import org.graphstream.ui.swingViewer.Viewer;
import org.graphstream.ui.swingViewer.util.DefaultCamera;


import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.util.StringUtil;
import edu.columbia.psl.cc.util.TemplateLoader;

public class GraphVisualizer {

	private GraphTemplate template;
	
	private String graphName;
	
	public GraphVisualizer(GraphTemplate template, String graphName) {
		this.template = template;
		this.graphName = graphName;
	}
	
	public static void constructEdge(Graph g, String m1, String m2, double similarity) {
		if (g.getNode(m1) == null) {
			g.addNode(m1);
		}
		Node n1 = g.getNode(m1);
		
		if (g.getNode(m2) == null) {
			g.addNode(m2);
		}
		Node n2 = g.getNode(m2);
		
		String edgeId = m1 + "-" + m2;
		if (g.getEdge("edgeId") != null) {
			System.err.println("Existing edge: " + edgeId);
			System.exit(-1);
		}
		
		Edge newEdge = g.addEdge(edgeId, n1, n2, true);
		if (newEdge == null) {
			System.err.println("Fail to create edge: " + edgeId);
			System.exit(-1);
		}
		newEdge.addAttribute("ui.label", similarity);
	}
	
	public static void renderCluster(String csv, String graphName) {
		File f = new File(csv);
		if (!f.exists()) {
			System.err.println("Invalid file: " + f.getAbsolutePath());
			return ;
		}
		
		try {
			Graph graph = new SingleGraph(graphName);
			graph.setAutoCreate(true);
			graph.setStrict(true);
			Viewer viewer = graph.display();
			
			DefaultCamera camera = new DefaultCamera(viewer.getGraphicGraph());
			camera.setZoom(0.5);
			camera.setAutoFitView(true);
			
			BufferedReader br = new BufferedReader(new FileReader(f));
			int count = 0;
			String buf= "";
			while ((buf = br.readLine()) != null) {
				if (count == 0) {
					//Ignore header
					count++;
					continue ;
				}
				
				String[] row = buf.split(",");
				String method = row[0];
				System.out.println("Current method: " + method);
				
				String firstN = row[2];
				double firstS = Double.valueOf(row[3]);
				
				constructEdge(graph, method, firstN, firstS);
				System.out.println("1st neightbor: " + firstN + " " + firstS);
				
				String secondN = row[4];
				double secondS = 0;
				if (secondN.trim().length() > 0) {
					secondS = Double.valueOf(row[5]);
					constructEdge(graph, method, secondN, secondS);					
					System.out.println("Second neighbor: " + secondN + " " + secondS);
				}
				String thirdN = row[6];
				double thirdS = 0;
				if (thirdN.trim().length() > 0) {
					thirdS = Double.valueOf(row[7]);
					constructEdge(graph, method, thirdN, thirdS);
					System.out.println("Third neighbor: " + thirdN + " " + thirdS);
				}
				String forthN = row[8];
				double forthS = 0;
				if (forthN.trim().length() > 0) {
					forthS = Double.valueOf(row[9]);
					constructEdge(graph, method, forthN, forthS);
					System.out.println("Fourth neighbor: " + forthN + " " + forthS);
				}
				String fifthN = row[10];
				double fifthS = 0;
				if (fifthN.trim().length() > 0) {
					fifthS = Double.valueOf(row[11]);
					constructEdge(graph, method, fifthN, fifthS);
					System.out.println("Fifth neighbor: " + forthN + " " + forthS);
				}
				System.out.println();
				count++;
			}
			
			for (Node n: graph.getNodeSet()) {
				n.addAttribute("ui.label", n.getId());
			}
			System.out.println("Total nodes: " + graph.getNodeCount());
			System.out.println("Total edges: " + graph.getEdgeCount());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void convertToVisualGraph() {
		Graph graph = new SingleGraph(this.graphName);
		graph.setAutoCreate(true);
		graph.setStrict(false);
		Viewer viewer = graph.display();
		//Viewer viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_SWING_THREAD);
        //View view = viewer.addDefaultView(false);/**/   // false indicates "no JFrame".
		//View view = viewer.getDefaultView();
		DefaultCamera camera = new DefaultCamera(viewer.getGraphicGraph());
		camera.setZoom(0.5);
		camera.setAutoFitView(true);
		//camera.setViewPercent(15);
		//View view = viewer.getDefaultView();
		//view.resizeFrame(800, 600);
		
		for (InstNode inst: template.getInstPool()) {
			//String pId = StringUtil.genIdxKey(inst.getFromMethod(), inst.getIdx());
			String pId = inst.getIdx() + " " + inst.getOp().getInstruction();
			for (String c: inst.getChildFreqMap().keySet()) {
				InstNode cInst = template.getInstPool().searchAndGet(c);
				String cId = cInst.getIdx() + " " + cInst.getOp().getInstruction();
				String eId = pId + "-" + cId;
				
				graph.addEdge(eId, pId, cId, true);
				graph.getEdge(eId).addAttribute("ui.label", inst.getChildFreqMap().get(cId));
			}
		}
		
		for (Node n: graph.getNodeSet()) {
			n.addAttribute("ui.label", n.getId());
		}
	}
	
	public static void main(String[] args) {
		/*String tempDir = "./template";
		String filePath = tempDir + "/edu.columbia.psl.cc.test.TemplateMethod:invoke3Methods:(II):I.json";
		TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
		GraphTemplate targetGraph = TemplateLoader.loadTemplateFile(filePath, graphToken);
		String graphName = "Test123";
		GraphVisualizer gv = new GraphVisualizer(targetGraph, graphName);
		gv.convertToVisualGraph();*/
		
		String clusterData = "./results/knn_result_45_85_f.csv";
		GraphVisualizer.renderCluster(clusterData, "DyCLINK_KNN");
	}
	
	public static class GraphNode {
		String methodName;
		
		Map<GraphNode, Double> neighbors = new HashMap<GraphNode, Double>();
		
		@Override
		public boolean equals(Object o) {
			if ((o instanceof GraphNode))
				return false;
			
			GraphNode go = (GraphNode)o;
			if (!go.methodName.equals(this.methodName))
				return false;
			else
				return true;
		}
		
		@Override
		public int hashCode() {
			return this.methodName.hashCode();
		}
	}

}
