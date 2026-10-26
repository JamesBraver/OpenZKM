package com.zelix.framework.attributes.annotations;

import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.base.AbstractTypeAnnotationsAttributeNode;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.IOException;
import java.io.PrintWriter;

public class RuntimeVisibleTypeAnnotationsAttribute extends AbstractTypeAnnotationsAttributeNode {
	public RuntimeVisibleTypeAnnotationsAttribute(
		StructureNode parent,
		int nameIndex,
		String attributeName,
		ByteDataBuffer buf,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<Integer, IGraphElement> elementsByOffset,
		PrintWriter printWriter
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References, elementsByOffset, printWriter, "RuntimeVisibleTypeAnnotations");
	}
}
