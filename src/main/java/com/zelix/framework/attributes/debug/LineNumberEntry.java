package com.zelix.framework.attributes.debug;

import com.zelix.framework.SettingFlag;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;

public class LineNumberEntry extends StructureNode implements IGraphElement, Comparable<LineNumberEntry> {
	private int lineNumber;
	private LabelNode labelNode;

	public LineNumberEntry(StructureNode parent, ByteDataBuffer buf, MultiMap<Integer, IGraphElement> elementsByOffset) throws IOException {
		super(parent);
		int startPc = buf.readUnsignedShort();
		elementsByOffset.put(INTEGER_CACHE.valueOf(startPc), this);
		this.lineNumber = buf.readUnsignedShort();
	}

	@Override
	public SettingFlag getSettingFlag() {
		return SettingFlag.SET_USAGE_LINE_NUMBER;
	}

	@Override
	public void setLabel(Integer pos, LabelNode lbl) {
		this.labelNode = lbl;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
	}

	protected void writeToStream(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeShort(this.labelNode.getOffset());
		dataOutputStream.writeShort(this.lineNumber);
	}

	@Override
	public String getMnemonics() {
		return "LineNumberTable";
	}

	public int getLineNumber() {
		return this.lineNumber;
	}

	@Override
	public int compareTo(LineNumberEntry other) {
		return this.compare(other);
	}

	public LabelNode getLabelNode() {
		return this.labelNode;
	}

	public int compare(LineNumberEntry other) {
		if (this.labelNode.getOffset() < other.labelNode.getOffset()) {
			return -1;
		} else if (this.labelNode.getOffset() == other.labelNode.getOffset()) {
			if (this.lineNumber < other.lineNumber) {
				return -1;
			} else {
				return this.lineNumber == other.lineNumber ? 0 : 1;
			}
		} else {
			return 1;
		}
	}

	@Override
	public void registerToGraph(ReferenceGraph<LabelNode, IGraphElement> graph) {
		graph.addReference(this.labelNode, this);
	}
}
