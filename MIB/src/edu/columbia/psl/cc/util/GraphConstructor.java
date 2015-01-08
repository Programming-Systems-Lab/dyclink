package edu.columbia.psl.cc.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.google.gson.reflect.TypeToken;

import edu.columbia.psl.cc.config.MIBConfiguration;
import edu.columbia.psl.cc.datastruct.InstPool;
import edu.columbia.psl.cc.pojo.GraphTemplate;
import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.MethodNode;

public class GraphConstructor {
	
	private static TypeToken<GraphTemplate> graphToken = new TypeToken<GraphTemplate>(){};
	
	public static void reconstructGraph(GraphTemplate rawGraph) {
		String myId = StringUtil.genThreadWithMethodIdx(rawGraph.getThreadId(), rawGraph.getThreadMethodId());
		String baseDir = MIBConfiguration.getInstance().getCacheDir() + "/" + myId;
		for (InstNode inst: rawGraph.getInstPool()) {
			if (inst instanceof MethodNode) {
				MethodNode mn = (MethodNode)inst;
				String calleeId = mn.getCalleeInfo().domCalleeIdx;
				
				File f = new File(baseDir + "/" + calleeId);
				GraphTemplate callee = GsonManager.readJsonGeneric(f, graphToken);
				
				//Multiply graph
				HashSet<InstNode> instParents = GraphUtil.retrieveRequiredParentInsts(mn, rawGraph.getInstPool(), MIBConfiguration.INST_DATA_DEP);
			}
		}
	}

}
