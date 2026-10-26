package com.zelix.framework.attributes.debug;

import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.declaration.SignatureAttribute;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.variable.LocalVariableTable;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.IOException;
import java.util.HashMap;

public class LocalVariableTypeEntry extends LocalVariableEntry {
	public LocalVariableTypeEntry(
		StructureNode parent,
		ByteDataBuffer buf,
		LocalVariableTable localVariableTable,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<Integer, IGraphElement> elementsByOffset
	) throws IOException {
		super(parent, buf, localVariableTable, utf8References, elementsByOffset);
	}

	@Override
	public String getMnemonics() {
		return "LocalVariableTypeTable";
	}

	@Override
	public void remapDescriptor(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> classNameMap) {
		String signature = this.getDescriptorUtf8().getValue();
		String remappedSignature = SignatureAttribute.remapSignature(signature, keepInnerClassInfo, keepGenericsInformation, classNameMap, this);
		if (!signature.equals(remappedSignature)) {
			this.getDescriptorUtf8().setValue(remappedSignature);
		}
	}
}
