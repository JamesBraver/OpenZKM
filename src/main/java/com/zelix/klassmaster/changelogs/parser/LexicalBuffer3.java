package com.zelix.klassmaster.changelogs.parser;

import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import java.util.ArrayList;
import java.util.List;

public class LexicalBuffer3 {
	private List<ChangeLogsNode> nodes = new ArrayList<>();
	private List<Integer> marks = new ArrayList<>();
	private int nodeCount = 0;
	private int mark = 0;
	private boolean nodeCreated;

	public void openNodeScope(ChangeLogsNode node) {
		this.marks.add(this.mark);
		this.mark = this.nodeCount;
		node.open();
	}

	public void closeNodeScope(ChangeLogsNode node, boolean createNode) {
		if (createNode) {
			int childCount = this.nodeArity();
			this.mark = this.marks.remove(this.marks.size() - 1);

			while (childCount-- > 0) {
				ChangeLogsNode childNode = this.popNode();
				childNode.setParent(node);
				node.addChild(childNode, childCount);
			}

			node.close();
			this.pushNode(node);
			this.nodeCreated = true;
		} else {
			this.mark = this.marks.remove(this.marks.size() - 1);
			this.nodeCreated = false;
		}
	}

	public void clear() {
		this.nodes.clear();
		this.marks.clear();
		this.nodeCount = 0;
		this.mark = 0;
	}

	public int nodeArity() {
		return this.nodeCount - this.mark;
	}

	public void pushNode(ChangeLogsNode node) {
		this.nodes.add(node);
		this.nodeCount++;
	}

	public ChangeLogsNode popNode() {
		if (--this.nodeCount < this.mark) {
			this.mark = this.marks.remove(this.marks.size() - 1);
		}

		return this.nodes.remove(this.nodes.size() - 1);
	}

	public void clearNodeScope(ChangeLogsNode node) {
		while (this.nodeCount > this.mark) {
			this.popNode();
		}

		this.mark = this.marks.remove(this.marks.size() - 1);
	}
}
