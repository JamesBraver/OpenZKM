package com.zelix.framework.attributes.stackmap;

import com.zelix.framework.SettingFlag;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.reference.ReferenceGraph;

public abstract class AbstractStackMapEntry extends StructureNode implements IGraphElement {
	public boolean isValid = true;
	public int labelOffset;
	public LabelNode labelNode;

	public AbstractStackMapEntry(StructureNode parent) {
		super(parent);
	}

	public abstract int getSerializedSize();

	public final boolean isValid() {
		return this.isValid;
	}

	@Override
	public SettingFlag getSettingFlag() {
		return SettingFlag.SET_USAGE_STACK_MAP;
	}

	@Override
	public final void setLabel(Integer pos, LabelNode lbl) {
		this.labelNode = lbl;
	}

	@Override
	public abstract void acceptVisitor(ConstantPoolVisitor visitor);

	public LabelNode getLabelNode() {
		return this.labelNode;
	}

	@Override
	public void registerToGraph(ReferenceGraph<LabelNode, IGraphElement> graph) {
		graph.addReference(this.labelNode, this);
	}
}
