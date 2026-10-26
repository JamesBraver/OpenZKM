package com.zelix.framework.attributes.base;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.MemberNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.annotation.AnnotationContainer;
import com.zelix.framework.attributes.code.CodeAttribute;
import com.zelix.framework.attributes.declaration.RecordAttribute;
import com.zelix.framework.attributes.declaration.RecordComponentNode;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.IOException;

public abstract class AbstractOwnedAttributeNode extends AbstractAttributeNode implements AnnotationContainer {
	public AbstractOwnedAttributeNode(StructureNode parent, int nameIndex, String attributeName, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
	}

	@Override
	public String getName() {
		StructureNode parentNode = this.getParent();
		if (parentNode instanceof AbstractClassNode) {
			return ((AbstractClassNode)parentNode).getRawSourcePath();
		} else if (parentNode instanceof MemberNode) {
			return ((MemberNode)parentNode).getDisplayName();
		} else if (parentNode instanceof CodeAttribute) {
			return ((MethodNode)((CodeAttribute)parentNode).getParent()).getDisplayName();
		} else {
			return parentNode instanceof RecordComponentNode ? ((RecordAttribute)((RecordComponentNode)parentNode).getParent()).getRawSourcePath() : "";
		}
	}

	public boolean isOwnedByClass() {
		return this.getParent() instanceof AbstractClassNode;
	}
}
