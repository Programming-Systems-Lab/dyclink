package edu.columbia.psl.cc.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import edu.columbia.psl.cc.pojo.BlockNode;

public class SCCDriver {

	private List<BlockNode> blockNodes;
	
	private int index = 1;
	
	private Stack<BlockNode> s = new Stack<BlockNode>();
	
	public SCCDriver(List<BlockNode> blockNodes) {
		this.blockNodes = blockNodes;
	}
	
	public List<Set<BlockNode>> calculateSCC() {
		List<Set<BlockNode>> ret = new ArrayList<Set<BlockNode>>();
		for (BlockNode bn: this.blockNodes) {
			if (bn.getIndex() == 0) {
				this.strongConnect(bn, ret);
			}
		}
		return ret;
	}
	
	public void strongConnect(BlockNode bn, List<Set<BlockNode>> recorder) {
		bn.setIndex(index);
		bn.setLowLink(index);
		this.index++;
		s.push(bn);
		
		List<BlockNode> children = bn.getChildrenBlock();
		for (BlockNode child: children) {
			if (child.getIndex() == 0) {
				this.strongConnect(child, recorder);
				int tmpLowLink = Math.min(bn.getLowLink(), child.getLowLink());
				bn.setLowLink(tmpLowLink);
			} else if (s.contains(child)) {
				bn.setLowLink(Math.min(bn.getLowLink(), child.getIndex()));
			}
		}
		
		Set<BlockNode> scc = new HashSet<BlockNode>();
		if (bn.getLowLink() == bn.getIndex()) {
			while (true) {
				BlockNode tmp = s.pop();
				scc.add(tmp);
				if (tmp.equals(bn))
					break ;
			}
			
			if (scc.size() > 1)
				recorder.add(scc);
		}
	}

}
