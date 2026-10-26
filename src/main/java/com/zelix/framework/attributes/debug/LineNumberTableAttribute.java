package com.zelix.framework.attributes.debug;

import com.zelix.framework.AttributesNode;
import com.zelix.framework.ClearingFlag;
import com.zelix.framework.StructureNode;
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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LineNumberTableAttribute extends AttributesNode implements SimpleReferenceListener {
	public boolean isParsedSuccessfully = true;
	public int entryCount;
	public LineNumberEntry[] entries;
	public byte[] rawData;

	public LineNumberTableAttribute(
		StructureNode parent,
		int nameIndex,
		String attributeName,
		ByteDataBuffer buf,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		PrintWriter printWriter,
		MultiMap<Integer, IGraphElement> elementsByOffset
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		if (this.length >= 2) {
			this.entryCount = buf.readUnsignedShort();
			if (this.entryCount * 4 + 2 == this.length) {
				this.entries = new LineNumberEntry[this.entryCount];

				for (int i = 0; i < this.entryCount; i++) {
					this.entries[i] = new LineNumberEntry(this, buf, elementsByOffset);
				}
			} else {
				this.isParsedSuccessfully = false;
				printWriter.println("ERROR: " + this.getRawSourcePath() + " : Invalid LineNumberTable Attribute (B)");
				this.rawData = new byte[this.length];
				this.rawData[0] = (byte)(this.entryCount >>> 8 & 0xFF);
				this.rawData[1] = (byte)(this.entryCount >>> 0 & 0xFF);
				buf.read(this.rawData, 2, this.length - 2);
			}
		} else {
			this.isParsedSuccessfully = false;
			printWriter.println("ERROR: " + this.getRawSourcePath() + " : Invalid LineNumberTable Attribute (A)");
			this.rawData = new byte[this.length];
			buf.read(this.rawData);
		}
	}

	public void registerEntries(ReferenceGraph<LabelNode, IGraphElement> graph) {
		if (this.isParsedSuccessfully) {
			for (int i = 0; i < this.entries.length; i++) {
				this.entries[i].registerToGraph(graph);
			}
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this.getParent());
		if (this.isParsedSuccessfully) {
			for (int i = 0; i < this.entryCount; i++) {
				this.entries[i].acceptVisitor(visitor);
			}
		}
	}

	public int[] getLineNumbers() {
		int[] lineNumbers = new int[this.entryCount];
		int lineNumberIndex = 0;

		for (LineNumberEntry entry : this.entries) {
			lineNumbers[lineNumberIndex++] = entry.getLineNumber();
		}

		Arrays.sort(lineNumbers);
		return lineNumbers;
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (this.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.entryCount);

			for (int i = 0; i < this.entryCount; i++) {
				this.entries[i].writeToStream(dataOutputStream);
			}
		} else {
			dataOutputStream.write(this.rawData);
		}
	}

	public void removeEntriesForLabels(Set<LabelNode> deadLabels, ReferenceGraph<LabelNode, IGraphElement> graph) {
		if (this.isParsedSuccessfully && deadLabels.size() > 0) {
			ArrayList<LineNumberEntry> retainedEntries = new ArrayList<>(this.entries.length);

			for (int i = 0; i < this.entries.length; i++) {
				LabelNode labelNode = this.entries[i].getLabelNode();
				if (!deadLabels.contains(labelNode)) {
					retainedEntries.add(this.entries[i]);
				} else {
					graph.removeReference(labelNode, this.entries[i]);
					labelNode.removeFlag(ClearingFlag.CLEAR_USAGE_LINE_NUMBER);
				}
			}

			if (retainedEntries.size() < this.entries.length) {
				LineNumberEntry[] entryArray = new LineNumberEntry[retainedEntries.size()];
				this.entries = retainedEntries.toArray(entryArray);
				this.entryCount = this.entries.length;
				this.length = this.entryCount * 4 + 2;
			}
		}
	}

	public void addEntriesTo(List<LineNumberEntry> lineNumbers) {
		if (this.isParsedSuccessfully) {
			for (int i = 0; i < this.entries.length; i++) {
				lineNumbers.add(this.entries[i]);
			}
		}
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		if (this.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.entryCount);

			for (int i = 0; i < this.entryCount; i++) {
				this.entries[i].writeToStream(dataOutputStream);
			}
		} else {
			dataOutputStream.write(this.rawData);
		}
	}

	public boolean hasLineNumber(int lineNumber) {
		if (!this.isParsedSuccessfully) {
			return false;
		} else {
			for (int i = 0; i < this.entries.length; i++) {
				if (this.entries[i].getLineNumber() == lineNumber) {
					return true;
				}
			}

			return false;
		}
	}
}
