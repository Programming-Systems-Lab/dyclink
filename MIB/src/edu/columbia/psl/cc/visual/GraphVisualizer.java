package edu.columbia.psl.cc.visual;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

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
		String tempDir = "./template";
		String filePath = tempDir + "/edu.columbia.psl.cc.test.TemplateMethod:invoke3Methods:(II):I.json";
		TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
		GraphTemplate targetGraph = TemplateLoader.loadTemplateFile(filePath, graphToken);
		String graphName = "Test123";
		GraphVisualizer gv = new GraphVisualizer(targetGraph, graphName);
		gv.convertToVisualGraph();
	}

}
