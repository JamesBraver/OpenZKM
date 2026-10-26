package com.zelix.framework.attributes.debug;

import com.zelix.framework.StructureNode;
import com.zelix.framework.analyzer.MethodAnalyzer;
import com.zelix.framework.attributes.base.AbstractLocalVariableTableAttribute;
import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.variable.LocalVariableSlot;
import com.zelix.framework.variable.LocalVariableTable;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.collections.KeyValuePair;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.utils.references.AtomicReference;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class LocalVariableTableAttribute extends AbstractLocalVariableTableAttribute {
	public LocalVariableTableAttribute(
		StructureNode parent,
		int nameIndex,
		String attributeName,
		ByteDataBuffer buf,
		LocalVariableTable localVariableTable,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		PrintWriter printWriter,
		MultiMap<Integer, IGraphElement> elementsByOffset
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, localVariableTable, utf8References, printWriter, elementsByOffset, "Invalid LocalVariableTable Attribute");
	}

	@Override
	public final LocalVariableEntry readEntry(
		ByteDataBuffer buf,
		LocalVariableTable localVariableTable,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<Integer, IGraphElement> elementsByOffset
	) throws IOException {
		return new LocalVariableEntry(this, buf, localVariableTable, utf8References, elementsByOffset);
	}

	public final void addLocalVariableEntries(
		int firstNonParamLocalSlot,
		int maxLocals,
		MethodAnalyzer methodAnalyzer,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		LocalVariableTable localVariableTable,
		MultiMap<Integer, IGraphElement> elementsByOffset
	) {
		if (this.isValid) {
			MultiMap<Integer, LocalVariableEntry> entriesBySlot = new MultiMap<>(maxLocals);

			for (LocalVariableEntry existingEntry : this.entries) {
				entriesBySlot.put(existingEntry.getIndex(), existingEntry);
			}

			int maxSlotIndex = localVariableTable.getMaxSlotIndex();
			boolean entryCreated = false;

			for (int slotIndex = 0; slotIndex < maxSlotIndex; slotIndex++) {
				if (!entriesBySlot.containsKey(slotIndex)) {
					LocalVariableSlot slot = localVariableTable.getVariable(slotIndex);
					if (slot != null && !slot.isWideSlotContinuation() && slot.hasFlag()) {
						AtomicReference<String> variableNameRef = new AtomicReference<>();
						KeyValuePair<Integer, Integer> bytecodeRange = methodAnalyzer.getLocalSlotBytecodeRange(slotIndex, variableNameRef);
						String variableName = variableNameRef.get();
						if (bytecodeRange.getKey() > -1 && bytecodeRange.getValue() > -1 && variableName != null) {
							LocalVariableEntry newEntry = new LocalVariableEntry(
								this, bytecodeRange.getKey(), bytecodeRange.getValue(), constantPoolManager.createUtf8String("a", typeWriters), constantPoolManager.createUtf8String(variableName, typeWriters), slot, elementsByOffset
							);
							entriesBySlot.put(slotIndex, newEntry);
							entryCreated = true;
						}
					}
				}
			}

			if (entryCreated) {
				ArrayList<LocalVariableEntry> sortedEntries = new ArrayList<>(entriesBySlot.valueCount());

				for (int orderedSlotIndex = 0; orderedSlotIndex < maxLocals; orderedSlotIndex++) {
					if (entriesBySlot.containsKey(orderedSlotIndex)) {
						for (LocalVariableEntry entry : entriesBySlot.get(orderedSlotIndex)) {
							sortedEntries.add(entry);
						}
					}
				}

				this.entries = sortedEntries.toArray(new LocalVariableEntry[sortedEntries.size()]);
				this.count = this.entries.length;
				this.length = this.count * 10 + 2;
			}
		}
	}
}
