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

public class ModuleHashesAttribute extends AbstractAttributeNode implements SimpleReferenceListener {
	private UTFStringWriter algorithmName;
	private ModuleHashEntry[] hashEntries;


	public ModuleHashesAttribute(StructureNode parent, int nameIndex, String attributeName, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		super.rawData = new byte[this.length];
		buf.read(super.rawData);

		try (ByteDataBuffer dataBuffer = ByteDataBuffer.fromBytes(super.rawData, false)) {
			int algorithmNameIndex = dataBuffer.readUnsignedShort();
			TypeWriter algorithmNameWriter = parent.getConstantPoolEntry(algorithmNameIndex);
			if (algorithmNameWriter == null) {
				super.isParsedSuccessfully = false;
				throw new ClassParseFailedException(
					parent.getClassNode().getRawSourcePath() + " : Illegal constant pool index in attribute : " + algorithmNameIndex + " : File is probably corrupt (V)"
				);
			}

			if (!(algorithmNameWriter instanceof UTFStringWriter)) {
				super.isParsedSuccessfully = false;
				throw new ClassParseFailedException(
					parent.getClassNode().getRawSourcePath() + " : Invalid attribute : " + algorithmNameIndex + " : '" + algorithmNameWriter.getClass().getName() + "' : File is probably corrupt (W)"
				);
			}

			this.algorithmName = (UTFStringWriter)algorithmNameWriter;
			utf8References.put(this.algorithmName, this);
			int hashEntryCount = dataBuffer.readUnsignedShort();
			this.hashEntries = new ModuleHashEntry[hashEntryCount];

			for (int i = 0; i < hashEntryCount; i++) {
				try {
					this.hashEntries[i] = new ModuleHashEntry(this, dataBuffer);
				} catch (ClassParseFailedException classParseException) {
					super.isParsedSuccessfully = false;
					throw classParseException;
				}
			}
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		this.attributeNameUtf8.accept(visitor, this, this.getParent());
		if (super.isParsedSuccessfully) {
			this.algorithmName.accept(visitor, this, this.getParent());

			for (int i = 0; i < this.hashEntries.length; i++) {
				this.hashEntries[i].acceptVisitor(visitor);
			}
		}
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.algorithmName.getTag());
			dataOutputStream.writeShort(this.hashEntries.length);

			for (int i = 0; i < this.hashEntries.length; i++) {
				this.hashEntries[i].write(dataOutputStream);
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (super.isParsedSuccessfully) {
			if (this.algorithmName == oldReference) {
				this.algorithmName = newReference;
			} else {
				super.updateSimpleNameReference(oldReference, newReference);
			}
		}
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			TypeWriter mappedAlgorithmName = writerMap.get(this.algorithmName);
			if (mappedAlgorithmName != null) {
				dataOutputStream.writeShort(mappedAlgorithmName.getTag());
			} else {
				dataOutputStream.writeShort(this.algorithmName.getTag());
			}

			dataOutputStream.writeShort(this.hashEntries.length);

			for (int i = 0; i < this.hashEntries.length; i++) {
				this.hashEntries[i].writeRemapped(dataOutputStream, writerMap);
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void remapDescriptors(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageRenameMap, HashMap<String, String> classNameMap) {
	}
}
