package com.zelix.framework.annotation;

import com.zelix.framework.SettingFlag;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.reference.ReferenceGraph;
import java.util.HashSet;
public class LocalVarTargetNode implements IGraphElement {
	public final AnnotationTypeReferenceNode parent;
	private LabelNode startPos;
	private int start_pc;
	private LabelNode endPos;
	private int end_pc;
	private int index;

	public LocalVarTargetNode(AnnotationTypeReferenceNode parent) {
		this.parent = parent;
	}

	public void setStartPC(int startPc) {
		this.start_pc = startPc;
	}

	public boolean removeInGraph(HashSet<LabelNode> unreachableLabels, ReferenceGraph<LabelNode, IGraphElement> graph) {
		if (!unreachableLabels.contains(this.startPos) && !unreachableLabels.contains(this.endPos)) {
			return false;
		} else {
			graph.removeReference(this.startPos, this);
			graph.removeReference(this.endPos, this);
			AnnotationTypeReferenceNode.clearUsageAnnotation(this.parent, this.startPos, graph);
			AnnotationTypeReferenceNode.clearUsageAnnotation(this.parent, this.endPos, graph);
			return true;
		}
	}

	public int getLength() {
		return this.startPos != null && this.endPos != null ? this.endPos.getOffset() - this.startPos.getOffset() : this.end_pc - this.start_pc;
	}

	@Override
	public SettingFlag getSettingFlag() {
		return SettingFlag.SET_USAGE_ANNOTATION;
	}

	public LabelNode getStartPos() {
		return this.startPos;
	}

	@Override
	public void setLabel(Integer pos, LabelNode lbl) {
		if (this.start_pc == pos) {
			this.startPos = lbl;
		}

		if (this.end_pc == pos) {
			this.endPos = lbl;
		}
	}

	public int getEndPC() {
		return this.end_pc;
	}

	@Override
	public String getMnemonics() {
		return this.parent.getAttributesNode().getAttributeName();
	}

	public boolean isResolved() {
		return this.startPos != null && this.endPos != null;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getIndex() {
		return this.index;
	}

	public int getStartPC() {
		return this.start_pc;
	}

	public void setEndPC(int endPc) {
		this.end_pc = endPc;
	}

	@Override
	public void registerToGraph(ReferenceGraph<LabelNode, IGraphElement> graph) {
		graph.addReference(this.startPos, this);
		graph.addReference(this.endPos, this);
	}
}
