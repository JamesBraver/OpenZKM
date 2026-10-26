package com.zelix.framework.attributes.base;

import com.zelix.framework.AttributesNode;
import com.zelix.framework.ClearingFlag;
import com.zelix.framework.SettingFlag;
import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.debug.LocalVariableEntry;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.variable.LocalVariableTable;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.utils.dataset.PriorityEntry;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class AbstractLocalVariableTableAttribute extends AttributesNode {
	public boolean isValid = true;
	public final LocalVariableTable localVariableTable;
	public int count;
	public LocalVariableEntry[] entries;
	public byte[] rawBytes;

	public AbstractLocalVariableTableAttribute(
		StructureNode parent,
		int nameIndex,
		String attributeName,
		ByteDataBuffer buf,
		LocalVariableTable localVariableTable,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		PrintWriter printWriter,
		MultiMap<Integer, IGraphElement> elementsByOffset,
		String errorMessage
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		this.localVariableTable = localVariableTable;
		byte[] attributeBytes = new byte[this.length];
		buf.read(attributeBytes);
		ByteDataBuffer dataBuffer = ByteDataBuffer.fromBytes(attributeBytes, false);

		try {
			if (this.length >= 2) {
				this.count = dataBuffer.readUnsignedShort();
				if (this.count * 10 + 2 == this.length) {
					this.entries = new LocalVariableEntry[this.count];

					for (int i = 0; i < this.count; i++) {
						this.entries[i] = this.readEntry(dataBuffer, this.localVariableTable, utf8References, elementsByOffset);
						if (this.isValid && !this.entries[i].isValid()) {
							this.isValid = false;
							printWriter.println("ERROR: " + this.getRawSourcePath() + " : " + errorMessage + " (C)");
							this.rawBytes = attributeBytes;
							break;
						}
					}
				} else {
					this.isValid = false;
					printWriter.println("ERROR: " + this.getRawSourcePath() + " : " + errorMessage + " (A) : count=" + this.count + " length=" + this.length);
					this.rawBytes = attributeBytes;
				}
			} else {
				this.isValid = false;
				printWriter.println("ERROR: " + this.getRawSourcePath() + " : " + errorMessage + " (B) : length=" + this.length);
				this.rawBytes = attributeBytes;
			}
		} finally {
			dataBuffer.close();
		}
	}

	public void removeDeadEntries(HashSet<LabelNode> deadLabels, ReferenceGraph<LabelNode, IGraphElement> graph) {
		if (this.isValid && deadLabels.size() > 0) {
			ArrayList<LocalVariableEntry> remainingEntries = new ArrayList<>(this.entries.length);

			for (int i = 0; i < this.entries.length; i++) {
				if (deadLabels.contains(this.entries[i].getStartLabel()) && deadLabels.contains(this.entries[i].getEndLabel())) {
					graph.removeReference(this.entries[i].getStartLabel(), this.entries[i]);
					graph.removeReference(this.entries[i].getEndLabel(), this.entries[i]);
					this.clearUsageIfUnreferenced(this.entries[i].getStartLabel(), graph);
					this.clearUsageIfUnreferenced(this.entries[i].getEndLabel(), graph);
				} else {
					remainingEntries.add(this.entries[i]);
				}
			}

			if (remainingEntries.size() < this.entries.length) {
				LocalVariableEntry[] entryArray = new LocalVariableEntry[remainingEntries.size()];
				this.entries = remainingEntries.toArray(entryArray);
				this.count = this.entries.length;
				this.length = this.count * 10 + 2;
			}

			for (LocalVariableEntry entry : this.entries) {
				if (!entry.getStartLabel().hasFlag(2)) {
					entry.getStartLabel().addUsage(SettingFlag.SET_USAGE_LOCAL_VARIABLE);
				}

				if (!entry.getEndLabel().hasFlag(2)) {
					entry.getEndLabel().addUsage(SettingFlag.SET_USAGE_LOCAL_VARIABLE);
				}
			}
		}
	}

	public final void obfuscateNames() {
		if (this.isValid) {
			for (int i = 0; i < this.entries.length; i++) {
				this.entries[i].obfuscateName();
			}
		}
	}

	public void retainSlots(Set<Integer> parameterSlotIndexes) {
		if (this.isValid) {
			ArrayList<LocalVariableEntry> remainingEntries = new ArrayList<>(this.entries.length);

			for (int i = 0; i < this.entries.length; i++) {
				LocalVariableEntry entry = this.entries[i];
				if (parameterSlotIndexes.contains(INTEGER_CACHE.valueOf(entry.getIndex()))) {
					remainingEntries.add(entry);
				}
			}

			if (remainingEntries.size() < this.entries.length) {
				LocalVariableEntry[] entryArray = new LocalVariableEntry[remainingEntries.size()];
				this.entries = remainingEntries.toArray(entryArray);
				this.count = this.entries.length;
				this.length = this.count * 10 + 2;
			}
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		if (this.isValid) {
			visitor.visit(this.attributeNameUtf8, this, this.getParent());

			for (int i = 0; i < this.entries.length; i++) {
				this.entries[i].acceptVisitor(visitor);
			}
		}
	}

	public abstract LocalVariableEntry readEntry(
		ByteDataBuffer buf,
		LocalVariableTable localVariableTable,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<Integer, IGraphElement> elementsByOffset
	) throws IOException;

	public void registerToGraph(ReferenceGraph<LabelNode, IGraphElement> graph) {
		if (this.isValid) {
			for (int i = 0; i < this.entries.length; i++) {
				this.entries[i].registerToGraph(graph);
			}
		}
	}

	@Override
	public final void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (this.isValid) {
			this.writeEntries(dataOutputStream);
		} else {
			dataOutputStream.write(this.rawBytes);
		}
	}

	public final void writeEntries(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeShort(this.entries.length);

		for (int i = 0; i < this.entries.length; i++) {
			this.entries[i].writeEntry(dataOutputStream);
		}
	}

	public Map<Integer, String> getIndexToNameMap() {
		if (!this.isValid) {
			return null;
		} else {
			ArrayList<PriorityEntry<String>> priorityEntries = new ArrayList<>(this.entries.length);
			HashMap<Integer, String> namesByIndex = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.entries.length));

			for (LocalVariableEntry entry : this.entries) {
				if (entry.isValid()) {
					priorityEntries.add(new PriorityEntry<>(entry.getIndex(), entry.getVariableName()));
				}
			}

			Collections.sort(priorityEntries);
			int currentPriority = -1;

			for (PriorityEntry<String> priorityEntry : priorityEntries) {
				if (priorityEntry.getPriority() > currentPriority) {
					currentPriority = priorityEntry.getPriority();
					namesByIndex.put(INTEGER_CACHE.valueOf(currentPriority), priorityEntry.getValue());
				}
			}

			return namesByIndex;
		}
	}

	private boolean clearUsageIfUnreferenced(LabelNode label, ReferenceGraph<LabelNode, IGraphElement> graph) {
		if (!this.isValid) {
			return false;
		} else {
			Set<IGraphElement> references = graph.getReferences(label);
			if (references != null) {
				Iterator<IGraphElement> referenceIterator = references.iterator();

				while (referenceIterator.hasNext()) {
					if (referenceIterator.next() instanceof LocalVariableEntry) {
						return false;
					}
				}
			}

			label.removeFlag(ClearingFlag.CLEAR_USAGE_LOCAL_VARIABLE);
			return true;
		}
	}

	@Override
	public final void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		if (this.isValid) {
			dataOutputStream.writeShort(this.entries.length);

			for (int i = 0; i < this.entries.length; i++) {
				this.entries[i].writeRemappedEntry(dataOutputStream, writerMap);
			}
		} else {
			dataOutputStream.write(this.rawBytes);
		}
	}

	@Override
	public final int getAttributeLength() {
		return this.isValid ? 2 + this.entries.length * 10 : this.rawBytes.length;
	}

	@Override
	public void remapDescriptors(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageRenameMap, HashMap<String, String> classNameMap) {
		if (this.isValid) {
			for (int i = 0; i < this.entries.length; i++) {
				this.entries[i].remapDescriptor(keepInnerClassInfo, keepGenericsInformation, classNameMap);
			}
		}
	}
}
