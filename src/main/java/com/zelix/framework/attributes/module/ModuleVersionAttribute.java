package com.zelix.framework.attributes.module;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.base.AbstractAttributeNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ModuleVersionAttribute extends AbstractAttributeNode implements SimpleReferenceListener {
	private UTFStringWriter moduleVersion;


	public ModuleVersionAttribute(StructureNode parent, int nameIndex, String attributeName, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		super.rawData = new byte[this.length];
		buf.read(super.rawData);

		try (ByteDataBuffer dataBuffer = ByteDataBuffer.fromBytes(super.rawData, false)) {
			int moduleVersionIndex = dataBuffer.readUnsignedShort();
			TypeWriter moduleVersionWriter = parent.getConstantPoolEntry(moduleVersionIndex);
			if (moduleVersionWriter == null) {
				super.isParsedSuccessfully = false;
				throw new ClassParseFailedException(
					parent.getClassNode().getRawSourcePath() + " : Illegal constant pool index in attribute : " + moduleVersionIndex + " : File is probably corrupt (AT)"
				);
			}

			if (!(moduleVersionWriter instanceof UTFStringWriter)) {
				super.isParsedSuccessfully = false;
				throw new ClassParseFailedException(
					parent.getClassNode().getRawSourcePath() + " : Invalid attribute : " + moduleVersionIndex + " : '" + moduleVersionWriter.getClass().getName() + "' : File is probably corrupt (AU)"
				);
			}

			this.moduleVersion = (UTFStringWriter)moduleVersionWriter;
			utf8References.put(this.moduleVersion, this);
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		this.attributeNameUtf8.accept(visitor, this, this.getParent());
		if (super.isParsedSuccessfully) {
			this.moduleVersion.accept(visitor, this, this.getParent());
		}
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.moduleVersion.getTag());
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.moduleVersion == oldReference) {
			this.moduleVersion = newReference;
		} else {
			super.updateSimpleNameReference(oldReference, newReference);
		}
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		this.writeAttributeBody(dataOutputStream);
	}

	@Override
	public void remapDescriptors(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageRenameMap, HashMap<String, String> classNameMap) {
	}
}
