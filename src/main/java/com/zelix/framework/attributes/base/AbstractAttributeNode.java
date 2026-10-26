package com.zelix.framework.attributes.base;

import com.zelix.framework.AttributesNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.IOException;

public abstract class AbstractAttributeNode extends AttributesNode {
	public boolean isParsedSuccessfully = true;
	public byte[] rawData;

	public AbstractAttributeNode(StructureNode parent, int nameIndex, String attributeName, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
	}

	public AbstractAttributeNode(StructureNode parent, UTFStringWriter nameUtf8, int length) {
		super(parent, nameUtf8, length);
	}

	public final boolean isParsedSuccessfully() {
		return this.isParsedSuccessfully;
	}
}
