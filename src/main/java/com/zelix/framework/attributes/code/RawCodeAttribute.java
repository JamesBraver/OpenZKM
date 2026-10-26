package com.zelix.framework.attributes.code;

import com.zelix.framework.AttributesNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.UnknownAttribute;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.AbstractNamedWriter;
import com.zelix.framework.writer.NameAndTypeWriter;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.ThreeLevelMultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.IOException;
import java.io.PrintWriter;

public class RawCodeAttribute extends AttributesNode {
	public int rawMaxStack;
	public int rawMaxLocals;
	public int rawCodeLength;
	public byte[] rawBytecode;
	public int rawExceptionTableLength;
	public byte[] rawExceptionTableData;
	public int rawAttributeCount;
	public byte[] rawAttributesData;

	public RawCodeAttribute(
		StructureNode parent,
		int nameIndex,
		String attributeName,
		ByteDataBuffer buf,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		PrintWriter printWriter,
		ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributeRefs
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		this.rawMaxStack = buf.readUnsignedShort();
		this.rawMaxLocals = buf.readUnsignedShort();
		this.rawCodeLength = buf.readInt();
		this.rawBytecode = new byte[this.rawCodeLength];
		buf.read(this.rawBytecode);
		this.rawExceptionTableLength = buf.readUnsignedShort();
		this.rawExceptionTableData = new byte[this.rawExceptionTableLength * 8];
		buf.read(this.rawExceptionTableData);
		this.rawAttributeCount = buf.readUnsignedShort();
		int rawAttributesLength = this.length - 8 - this.rawCodeLength - 2 - this.rawExceptionTableData.length - 2;
		this.rawAttributesData = new byte[rawAttributesLength];
		buf.read(this.rawAttributesData);
	}

	public byte[] getRawExceptionTableData() {
		return this.rawExceptionTableData;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this.getParent());
	}

	public int getRawMaxLocals() {
		return this.rawMaxLocals;
	}

	public int getRawMaxStack() {
		return this.rawMaxStack;
	}

	public int getRawAttributeCount() {
		return this.rawAttributeCount;
	}

	public byte[] getRawBytecode() {
		return this.rawBytecode;
	}

	public int getRawExceptionTableLength() {
		return this.rawExceptionTableLength;
	}

	public byte[] getRawAttributesData() {
		return this.rawAttributesData;
	}
}
