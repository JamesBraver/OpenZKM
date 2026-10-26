package com.zelix.framework.attributes.base;

import com.zelix.framework.AttributesNode;
import com.zelix.framework.ClearingFlag;
import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.stackmap.AbstractStackMapEntry;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public abstract class AbstractStackMapAttribute extends AttributesNode {
	public boolean isParsedSuccessfully = true;
	public AbstractStackMapEntry[] entries;
	public int entryCount;
	public byte[] rawData;

	public AbstractStackMapAttribute(StructureNode parent, int nameIndex, String attributeName, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
	}

	public AbstractStackMapAttribute(StructureNode parent, UTFStringWriter nameUtf8, int length) {
		super(parent, nameUtf8, length);
	}

	public final void registerEntries(ReferenceGraph<LabelNode, IGraphElement> graph) {
		for (int i = 0; i < this.entryCount; i++) {
			this.entries[i].registerToGraph(graph);
		}
	}

	protected abstract void write(DataOutputStream dataOutputStream) throws IOException;

	protected abstract void writeMapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap) throws IOException;

	public int getEntryCount() {
		return this.entryCount;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this.getParent());

		for (int i = 0; i < this.entryCount; i++) {
			this.entries[i].acceptVisitor(visitor);
		}
	}

	@Override
	public final void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (this.isParsedSuccessfully) {
			this.write(dataOutputStream);
		} else {
			dataOutputStream.write(this.rawData);
		}
	}

	public void removeEntriesForLabels(Set<LabelNode> deadLabels, ReferenceGraph<LabelNode, IGraphElement> graph) {
		if (this.isParsedSuccessfully && deadLabels.size() > 0) {
			ArrayList<AbstractStackMapEntry> remainingEntries = new ArrayList<>(this.entryCount);

			for (int i = 0; i < this.entryCount; i++) {
				LabelNode labelNode = this.entries[i].getLabelNode();
				if (!deadLabels.contains(labelNode)) {
					remainingEntries.add(this.entries[i]);
				} else {
					graph.removeReference(labelNode, this.entries[i]);
					labelNode.removeFlag(ClearingFlag.CLEAR_USAGE_STACK_MAP);
				}
			}

			if (remainingEntries.size() < this.entries.length) {
				AbstractStackMapEntry[] entryArray = new AbstractStackMapEntry[remainingEntries.size()];
				this.entries = remainingEntries.toArray(entryArray);
				this.entryCount = this.entries.length;
				this.getAttributeLength();
			}
		}
	}

	@Override
	public final void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		if (this.isParsedSuccessfully) {
			this.writeMapped(dataOutputStream, writerMap);
		} else {
			dataOutputStream.write(this.rawData);
		}
	}

	public final void setEntries(AbstractStackMapEntry[] entries) {
		this.entryCount = entries.length;
		this.entries = entries;
	}

	@Override
	public final int getAttributeLength() {
		int attributeLength = 2;

		for (int i = 0; i < this.entryCount; i++) {
			attributeLength += this.entries[i].getSerializedSize();
		}

		this.length = attributeLength;
		return attributeLength;
	}

	@Override
	public final UTFStringWriter getNameUtf8() {
		return this.attributeNameUtf8;
	}
}
