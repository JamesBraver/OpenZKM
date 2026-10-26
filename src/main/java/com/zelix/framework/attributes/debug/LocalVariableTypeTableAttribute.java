package com.zelix.framework.attributes.debug;

import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.base.AbstractLocalVariableTableAttribute;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.variable.LocalVariableTable;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.IOException;
import java.io.PrintWriter;

public class LocalVariableTypeTableAttribute extends AbstractLocalVariableTableAttribute {
	public LocalVariableTypeTableAttribute(
		StructureNode parent,
		int nameIndex,
		String attributeName,
		ByteDataBuffer buf,
		LocalVariableTable methodLocalVariableTable,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		PrintWriter printWriter,
		MultiMap<Integer, IGraphElement> elementsByOffset
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, methodLocalVariableTable, utf8References, printWriter, elementsByOffset, "Invalid LocalVariableTypeTable Attribute");
	}

	@Override
	public final LocalVariableEntry readEntry(
		ByteDataBuffer buf,
		LocalVariableTable localVariableTable,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<Integer, IGraphElement> elementsByOffset
	) throws IOException {
		return new LocalVariableTypeEntry(this, buf, localVariableTable, utf8References, elementsByOffset);
	}
}
