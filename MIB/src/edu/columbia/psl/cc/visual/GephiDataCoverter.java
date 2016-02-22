package edu.columbia.psl.cc.visual;

import java.awt.Color;
import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.gephi.data.attributes.api.AttributeColumn;
import org.gephi.data.attributes.api.AttributeController;
import org.gephi.data.attributes.api.AttributeModel;
import org.gephi.data.attributes.api.AttributeType;
import org.gephi.graph.api.DirectedGraph;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.GraphController;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Node;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperties;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.ProjectController;
import org.gephi.project.api.Workspace;
import org.gephi.ranking.api.Ranking;
import org.gephi.ranking.api.RankingController;
import org.gephi.ranking.api.Transformer;
import org.gephi.ranking.plugin.transformer.AbstractColorTransformer;
import org.gephi.ranking.plugin.transformer.AbstractSizeTransformer;
import org.openide.util.Lookup;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.analysis.InstWrapper;
import edu.columbia.psl.cc.analysis.PageRankSelector;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.util.GraphConstructor;
import edu.columbia.psl.cc.util.GraphUtil;
import edu.columbia.psl.cc.util.GsonManager;
import edu.columbia.psl.cc.util.StringUtil;

public class GephiDataCoverter {
	
	private static String outputPath = "/Users/mikefhsu/Desktop/grant_graph/";
	
	private static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
	private static String nodeHeader = "Id,Label,Attribute,Weight\n";
	
	private static String edgeHeader = "Source,Target,Type,Weight\n";
	
	public static void visualizeGraph(InstPool pool, List<InstWrapper> results) {
		ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
		pc.newProject();
		Workspace workspace = pc.getCurrentWorkspace();
		
		GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getModel();
		AttributeController ac = Lookup.getDefault().lookup(AttributeController.class);
		AttributeModel model = ac.getModel();
				
		AttributeColumn idColumn = model.getNodeTable().getColumn("Id");
		AttributeColumn lableColumn = model.getNodeTable().getColumn("Label");
		AttributeColumn attrColumn = model.getNodeTable().addColumn("Attribute", AttributeType.INT);
		AttributeColumn weightColumn = model.getNodeTable().addColumn("Weight", AttributeType.DOUBLE);
		
		AttributeColumn edgeWeightColumn = model.getEdgeTable().getColumn("Weight");
		
		System.out.println("Existing node attributes");
		for (AttributeColumn col: model.getNodeTable().getColumns()) {
			System.out.println(col);
		}
		
		System.out.println("Existing edge attributes");
		for (AttributeColumn col: model.getEdgeTable().getColumns()) {
			System.out.println(col);
		}
		
		
		Map<String, Node> nodeHistory = new HashMap<String, Node>();
		for (InstWrapper iw: results) {
			InstNode inst = iw.inst;
			double weight = iw.pageRank;
			String id = StringUtil.genIdxKey(inst.getThreadId(), inst.getThreadMethodIdx(), inst.getIdx());
			String label = inst.getOp().getInstruction();
			String attribute = String.valueOf(inst.repOp);
			
			Node tmp = graphModel.factory().newNode(id);
			tmp.getNodeData().setLabel(label);
			tmp.getNodeData().getAttributes().setValue(attrColumn.getIndex(), attribute);
			tmp.getNodeData().getAttributes().setValue(weightColumn.getIndex(), weight);
			
			//tmp.setLabel(label);
			//tmp.setAttribute("Attribute", attribute);
			//tmp.setAttribute("Weight", weight);
			nodeHistory.put(id, tmp);
		}
		
		for (String id: nodeHistory.keySet()) {
			Node curNode = nodeHistory.get(id);
			InstNode curInst = pool.searchAndGet(id);
			
			for (String cId: curInst.getChildFreqMap().keySet()) {
				if (pool.searchAndGet(cId) == null) {
					continue ;
				}
				
				Node childNode = nodeHistory.get(cId);
				float freq = curInst.getChildFreqMap().get(cId).floatValue();
				graphModel.factory().newEdge(curNode, childNode, freq, true);
				//Edge newEdge = graphModel.factory().newEdge(curNode, childNode);
				//newEdge.setWeight(freq);
			}	
		}
		
		DirectedGraph graph = graphModel.getDirectedGraph();
		System.out.println("Nodes: " + graph.getNodeCount());
		System.out.println("Edges: " + graph.getEdgeCount());
		
		RankingController rankingController = Lookup.getDefault().lookup(RankingController.class);
		Ranking weightRanking = rankingController.getModel().getRanking(Ranking.NODE_ELEMENT, weightColumn.getId());
		AbstractColorTransformer colorTransformer = (AbstractColorTransformer) rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_COLOR);
		
		float[] positions = {0f,0.33f,0.66f,1f};
		colorTransformer.setColorPositions(positions);
		Color[] colors = new Color[]{new Color(0x0000FF), new Color(0xFFFFFF),new Color(0x00FF00),new Color(0xFF0000)};
		colorTransformer.setColors(colors);
				
		AbstractSizeTransformer sizeTransformer = (AbstractSizeTransformer) rankingController.getModel().getTransformer(Ranking.NODE_ELEMENT, Transformer.RENDERABLE_SIZE);
		sizeTransformer.setMinSize(10);
		sizeTransformer.setMaxSize(50);
		rankingController.transform(weightRanking, sizeTransformer);
		
		Ranking edgeWeightRanking = rankingController.getModel().getRanking(Ranking.EDGE_ELEMENT, edgeWeightColumn.getId());
		AbstractSizeTransformer edgeSizeTransformer = (AbstractSizeTransformer) rankingController.getModel().getTransformer(Ranking.EDGE_ELEMENT, Transformer.RENDERABLE_SIZE);
		sizeTransformer.setMinSize(3);
		sizeTransformer.setMaxSize(10);
		rankingController.transform(edgeWeightRanking, edgeSizeTransformer);
		
		AutoLayout autoLayout = new AutoLayout(30, TimeUnit.SECONDS);
		ForceAtlasLayout fLayout = new ForceAtlasLayout(null);
		
		AutoLayout.DynamicProperty adjustBySizeProperty = AutoLayout.createDynamicProperty("forceAtlas.adjustSizes.name", Boolean.TRUE, 0.2f);//True after 10% of layout time
		AutoLayout.DynamicProperty repulsionProperty = AutoLayout.createDynamicProperty("forceAtlas.repulsionStrength.name", new Double(500.), 0f);//500 for the complete period
		autoLayout.addLayout(fLayout, 1.0f, new AutoLayout.DynamicProperty[]{adjustBySizeProperty, repulsionProperty});
		autoLayout.execute();
		
		PreviewModel pModel = Lookup.getDefault().lookup(PreviewController.class).getModel();

		PreviewProperties prop = pModel.getProperties();
		prop.putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
		prop.putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(Color.BLACK));
		prop.putValue(PreviewProperty.EDGE_THICKNESS, new Float(3f));
		prop.putValue(PreviewProperty.NODE_LABEL_FONT, prop.getFontValue(PreviewProperty.NODE_LABEL_FONT).deriveFont(8));
		
		ExportController ec = Lookup.getDefault().lookup(ExportController.class);
		
		try {
			ec.exportFile(new File("~/Desktop/test.pdf"));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		//visualizeGraph(null, null);
		String graphFile = null;
		
		System.out.println("Input graph file: ");
		Scanner s = new Scanner(System.in);
		graphFile = s.nextLine();
		File gFile = new File(graphFile);
		if (!gFile.exists()) {
			System.err.println("Invalid graph file");
			System.exit(-1);
		}
		System.out.println("Confirm graph file path: " + gFile.getAbsolutePath());
				
		System.out.println("Output data file: ");
		String outputFile = s.nextLine();
		String nodeOutput = outputPath + outputFile + "_node.csv";
		String edgeOutput = outputPath + outputFile + "_edge.csv";
		System.out.println("Confirm node output: " + nodeOutput);
		System.out.println("Confirm edge output: " + edgeOutput);
		
		GraphTemplate graphObj = GsonManager.readJsonGeneric(gFile, graphToken);
		GraphConstructor gc = new GraphConstructor();
		gc.reconstructGraph(graphObj, false);
		gc.cleanObjInit(graphObj);
		
		InstPool exportPool = null;
		
		System.out.println("Extract sub?");
		boolean extractSub = Boolean.valueOf(s.nextLine());
		if (extractSub) {
			System.out.println("Start instruction: ");
			String startInst = s.nextLine();
			
			System.out.println("Seg size: ");
			int segSize = Integer.valueOf(s.nextLine());
			
			List<InstNode> sorted = GraphUtil.sortInstPool(graphObj.getInstPool(), true);
			exportPool = new InstPool();
			
			boolean start = false;
			int counter = 0;
			for (InstNode inst: sorted) {
				if (inst.toString().equals(startInst)) {
					start = true;
				}
				
				if (start) {
					exportPool.add(inst);
					counter++;
				}
								
				if (counter == segSize) {
					break;
				}
			}
			System.out.println("Extracted sub seg size: " + exportPool.size());
		} else {
			exportPool = graphObj.getInstPool();
		}
		
		PageRankSelector selector = new PageRankSelector(exportPool, true, true);
		List<InstWrapper> results = selector.computePageRank();
		
		StringBuilder nodeSb = new StringBuilder();
		nodeSb.append(nodeHeader);
		
		StringBuilder edgeSb = new StringBuilder();
		edgeSb.append(edgeHeader);
		for (InstWrapper iw: results) {
			InstNode inst = iw.inst;
			double weight = iw.pageRank;
			String id = StringUtil.genIdxKey(inst.getThreadId(), inst.getThreadMethodIdx(), inst.getIdx());
			String label = inst.getOp().getInstruction();
			String attribute = String.valueOf(inst.repOp);
			
			String nodeRaw = id + "," + label + "," + attribute + "," + weight + "\n";
			nodeSb.append(nodeRaw);
			
			for (String cId: inst.getChildFreqMap().keySet()) {
				if (exportPool.searchAndGet(cId) == null) {
					continue ;
				}
				
				String aEdge = id + "," + cId + ",directed" + "," + inst.getChildFreqMap().get(cId) + "\n";
				edgeSb.append(aEdge);
			}
		}
		
		try {
			BufferedWriter nodeWriter = new BufferedWriter(new FileWriter(nodeOutput));
			nodeWriter.write(nodeSb.toString());
			nodeWriter.close();
			
			BufferedWriter edgeWriter = new BufferedWriter(new FileWriter(edgeOutput));
			edgeWriter.write(edgeSb.toString());
			edgeWriter.close();
			
			System.out.println("Data output completes");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
