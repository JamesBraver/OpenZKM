package com.zelix.klassmaster.script;

import com.zelix.klassmaster.script.base.ScriptNode;
import java.util.ArrayList;
import java.util.List;

public class LexicalBuffer {
	private List<ScriptNode> tokenList = new ArrayList<>();
	private List<Integer> marks = new ArrayList<>();
	private int tokenCount = 0;
	private int markIndex = 0;
	private boolean childrenAdopted;

	public void discardNode(ScriptNode node) {
		while (this.tokenCount > this.markIndex) {
			this.pop();
		}

		this.markIndex = this.marks.remove(this.marks.size() - 1);
	}

	public void push(ScriptNode node) {
		this.tokenList.add(node);
		this.tokenCount++;
	}

	public void endNode(ScriptNode node, boolean adoptChildren) {
		if (adoptChildren) {
			int childCount = this.tokensSinceMark();
			this.markIndex = this.marks.remove(this.marks.size() - 1);

			while (childCount-- > 0) {
				ScriptNode childNode = this.pop();
				childNode.setParent(node);
				node.setChild(childNode, childCount);
			}

			node.onParseComplete();
			this.push(node);
			this.childrenAdopted = true;
		} else {
			this.markIndex = this.marks.remove(this.marks.size() - 1);
			this.childrenAdopted = false;
		}
	}

	public void beginNode(ScriptNode node) {
		this.marks.add(this.markIndex);
		this.markIndex = this.tokenCount;
		node.onParseStart();
	}

	public int tokensSinceMark() {
		return this.tokenCount - this.markIndex;
	}

	public ScriptNode pop() {
		if (--this.tokenCount < this.markIndex) {
			this.markIndex = this.marks.remove(this.marks.size() - 1);
		}

		return this.tokenList.remove(this.tokenList.size() - 1);
	}
}
