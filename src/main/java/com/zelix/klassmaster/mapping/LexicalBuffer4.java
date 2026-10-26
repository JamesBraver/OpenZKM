package com.zelix.klassmaster.mapping;

import com.zelix.klassmaster.mapping.javaccInterface.MappingNode;
import java.util.ArrayList;
import java.util.List;

public class LexicalBuffer4 {
	private List<MappingNode> nodes = new ArrayList<>();
	private List<Integer> marks = new ArrayList<>();
	private int nodeCount = 0;
	private int mark = 0;
	private boolean nodeCreated;

	public void clear() {
		this.nodes.clear();
		this.marks.clear();
		this.nodeCount = 0;
		this.mark = 0;
	}

	public int nodeArity() {
		return this.nodeCount - this.mark;
	}

	public MappingNode popNode() {
		if (--this.nodeCount < this.mark) {
			this.mark = this.marks.remove(this.marks.size() - 1);
		}

		return this.nodes.remove(this.nodes.size() - 1);
	}

	public void openNodeScope(MappingNode node) {
		this.marks.add(this.mark);
		this.mark = this.nodeCount;
		node.open();
	}

	public void clearNodeScope(MappingNode node) {
		while (this.nodeCount > this.mark) {
			this.popNode();
		}

		this.mark = this.marks.remove(this.marks.size() - 1);
	}

	public void closeNodeScope(MappingNode node, boolean createNode) {
		if (createNode) {
			int arity = this.nodeArity();
			this.mark = this.marks.remove(this.marks.size() - 1);

			while (arity-- > 0) {
				MappingNode childNode = this.popNode();
				childNode.setParent(node);
				node.addChild(childNode, arity);
			}

			node.close();
			this.pushNode(node);
			this.nodeCreated = true;
		} else {
			this.mark = this.marks.remove(this.marks.size() - 1);
			this.nodeCreated = false;
		}
	}

	public void pushNode(MappingNode node) {
		this.nodes.add(node);
		this.nodeCount++;
	}
}
