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

public class ModuleTargetAttribute extends AbstractAttributeNode implements SimpleReferenceListener {
	private UTFStringWriter targetPlatform;


	public ModuleTargetAttribute(StructureNode parent, int nameIndex, String attributeName, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		super.rawData = new byte[this.length];
		buf.read(super.rawData);

		try (ByteDataBuffer dataBuffer = ByteDataBuffer.fromBytes(super.rawData, false)) {
			int targetPlatformIndex = dataBuffer.readUnsignedShort();
			if (targetPlatformIndex != 0) {
				TypeWriter targetPlatformWriter = parent.getConstantPoolEntry(targetPlatformIndex);
				if (targetPlatformWriter == null) {
					super.isParsedSuccessfully = false;
					throw new ClassParseFailedException(
						parent.getClassNode().getRawSourcePath() + " : Illegal constant pool index in attribute : " + targetPlatformIndex + " : File is probably corrupt (AP)"
					);
				}

				if (!(targetPlatformWriter instanceof UTFStringWriter)) {
					super.isParsedSuccessfully = false;
					throw new ClassParseFailedException(
						parent.getClassNode().getRawSourcePath() + " : Invalid attribute : " + targetPlatformIndex + " : '" + targetPlatformWriter.getClass().getName() + "' : File is probably corrupt (AQ)"
					);
				}

				this.targetPlatform = (UTFStringWriter)targetPlatformWriter;
				utf8References.put(this.targetPlatform, this);
			}
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		this.attributeNameUtf8.accept(visitor, this, this.getParent());
		if (super.isParsedSuccessfully && this.targetPlatform != null) {
			this.targetPlatform.accept(visitor, this, this.getParent());
		}
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			if (this.targetPlatform != null) {
				dataOutputStream.writeShort(this.targetPlatform.getTag());
			} else {
				dataOutputStream.writeShort(0);
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (super.isParsedSuccessfully) {
			if (this.targetPlatform == oldReference) {
				this.targetPlatform = newReference;
			} else {
				super.updateSimpleNameReference(oldReference, newReference);
			}
		}
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			if (this.targetPlatform != null) {
				TypeWriter mappedTargetPlatform = writerMap.get(this.targetPlatform);
				if (mappedTargetPlatform != null) {
					dataOutputStream.writeShort(mappedTargetPlatform.getTag());
				} else {
					dataOutputStream.writeShort(this.targetPlatform.getTag());
				}
			} else {
				dataOutputStream.writeShort(0);
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void remapDescriptors(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageRenameMap, HashMap<String, String> classNameMap) {
	}
}
