package com.zelix.framework.attributes;

import com.zelix.framework.AttributesNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.IOException;

public class DeprecatedAttribute extends AttributesNode {
	public DeprecatedAttribute(StructureNode parent, int nameIndex, String attributeName, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this.getParent());
	}
}
