package com.zelix.framework.wrappers;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AttributesNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.SkippedAttribute;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.IOException;
import java.util.Map;

public class LibraryFieldNode extends AbstractFieldNode {
	public LibraryFieldNode(AbstractClassNode owner, UTFStringWriter nameWriter, UTFStringWriter descriptorWriter, AttributesNode[] attributes, int generationTypeFlag) {
		super(owner, nameWriter, descriptorWriter, attributes, generationTypeFlag);

		for (int i = 0; i < attributes.length; i++) {
			attributes[i].setParent(this);
		}
	}

	public LibraryFieldNode(StructureNode parent, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent, buf, utf8References);
		this.attributes = new AttributesNode[this.attributeCount];

		for (int i = 0; i < this.attributeCount; i++) {
			int nameIndex = buf.readUnsignedShort();
			TypeWriter constantPoolEntry = parent.getConstantPoolEntry(nameIndex);
			if (constantPoolEntry == null) {
				throw new ClassParseFailedException(
					parent.getClassNode().getRawSourcePath() + " : Illegal constant pool index in attribute : " + nameIndex + " : File is probably corrupt (H)"
				);
			}

			if (!(constantPoolEntry instanceof UTFStringWriter)) {
				throw new ClassParseFailedException(
					parent.getClassNode().getRawSourcePath() + " : Invalid attribute : " + nameIndex + " : '" + constantPoolEntry.getClass().getName() + "' : File is probably corrupt (I)"
				);
			}

			String attributeName = ((UTFStringWriter)constantPoolEntry).getValue();
			this.attributes[i] = new SkippedAttribute(parent, nameIndex, attributeName, buf, utf8References);
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
	}

	public void applyMappedFieldName(Map<FieldSignature, FieldSignature> fieldMappings) {
		FieldSignature fieldSignature = this.getFieldSignature();
		FieldSignature mappedSignature = fieldMappings.get(fieldSignature);
		if (mappedSignature != null && !mappedSignature.equals(fieldSignature)) {
			this.setName(mappedSignature.getName());
		}
	}

	@Override
	public boolean isWorkspaceNode() {
		return false;
	}
}
