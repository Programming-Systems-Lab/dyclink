package edu.columbia.psl.cc.datastruct;

import java.util.HashSet;
import java.util.Set;


import edu.columbia.psl.cc.pojo.InstNode;
import edu.columbia.psl.cc.pojo.CondNode;
import edu.columbia.psl.cc.pojo.Var;

public class BCTreeNodePool extends HashSet<InstNode> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InstNode searchBCTreeNode(Set<Var> node, boolean createNew) {
		for (InstNode n: this) {
			if (n.equals(node)) {
				return n;
			}
		}
		
		if (createNew) {
			InstNode bn = new InstNode();
			for (Var v: node) {
				bn.addVar(v);
			}
			this.add(bn);
			return bn;
		}
		return null;
	}
	
	public InstNode searchBCTreeNode(Var node, boolean createNew) {
		Set<Var> fakeSet = new HashSet<Var>();
		fakeSet.add(node);
		return this.searchBCTreeNode(fakeSet, createNew);
	}
	
	public CondNode searchCondNode(int opcode, String label, boolean createNew) {
		for (InstNode n: this) {
			if (!n.getClass().equals(CondNode.class))
				continue;
			
			CondNode cn = (CondNode)n;
			if (cn.getOpcode() == opcode && cn.getLabel().equals(label))
				return cn;
		}
		
		if (createNew) {
			CondNode cn = new CondNode();
			cn.setOpcode(opcode);
			cn.setLabel(label);
			cn.addChild(null, label);
			this.add(cn);
			return cn;
		}
		return null;
	}

}
