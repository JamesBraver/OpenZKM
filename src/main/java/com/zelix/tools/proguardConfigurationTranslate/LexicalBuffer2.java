package com.zelix.tools.proguardConfigurationTranslate;

import java.util.ArrayList;
import java.util.List;

public class LexicalBuffer2 {
	private List<ProguardNode> nodes = new ArrayList<>();
	private List<Integer> marks = new ArrayList<>();
	private int nodeCount = 0;
	private int mark = 0;
	private boolean nodeCreated;

	public void pushNode(ProguardNode node) {
		this.nodes.add(node);
		this.nodeCount++;
	}

	public void closeNodeScope(ProguardNode node, boolean createNode) {
		if (createNode) {
			int childCount = this.nodeArity();
			this.mark = this.marks.remove(this.marks.size() - 1);

			while (childCount-- > 0) {
				ProguardNode childNode = this.popNode();
				childNode.setParent(node);
				node.setChild(childNode, childCount);
			}

			node.onParseComplete();
			this.pushNode(node);
			this.nodeCreated = true;
		} else {
			this.mark = this.marks.remove(this.marks.size() - 1);
			this.nodeCreated = false;
		}
	}

	public void clearNodeScope(ProguardNode node) {
		while (this.nodeCount > this.mark) {
			this.popNode();
		}

		this.mark = this.marks.remove(this.marks.size() - 1);
	}

	public int nodeArity() {
		return this.nodeCount - this.mark;
	}

	public void openNodeScope(ProguardNode node) {
		this.marks.add(this.mark);
		this.mark = this.nodeCount;
		node.onParseStart();
	}

	public ProguardNode popNode() {
		if (--this.nodeCount < this.mark) {
			this.mark = this.marks.remove(this.marks.size() - 1);
		}

		return this.nodes.remove(this.nodes.size() - 1);
	}
}
