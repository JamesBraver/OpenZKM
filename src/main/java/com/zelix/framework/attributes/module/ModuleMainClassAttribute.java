package com.zelix.framework.attributes.module;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.base.AbstractAttributeNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ModuleMainClassAttribute extends AbstractAttributeNode implements SimpleReferenceListener, ReferenceUpdateListener {
	private ClassInfoWriter mainClass;


	public ModuleMainClassAttribute(
		StructureNode parent,
		int nameIndex,
		String attributeName,
		ByteDataBuffer buf,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		super.rawData = new byte[this.length];
		buf.read(super.rawData);

		try (ByteDataBuffer dataBuffer = ByteDataBuffer.fromBytes(super.rawData, false)) {
			int mainClassIndex = dataBuffer.readUnsignedShort();
			TypeWriter mainClassWriter = parent.getConstantPoolEntry(mainClassIndex);
			if (mainClassWriter == null) {
				super.isParsedSuccessfully = false;
				throw new ClassParseFailedException(
					parent.getClassNode().getRawSourcePath() + " : Illegal constant pool index in attribute : " + mainClassIndex + " : File is probably corrupt (X)"
				);
			}

			if (!(mainClassWriter instanceof ClassInfoWriter)) {
				super.isParsedSuccessfully = false;
				throw new ClassParseFailedException(
					parent.getClassNode().getRawSourcePath() + " : Invalid attribute : " + mainClassIndex + " : '" + mainClassWriter.getClass().getName() + "' : File is probably corrupt (Y)"
				);
			}

			this.mainClass = (ClassInfoWriter)mainClassWriter;
			classReferences.put(this.mainClass, this);
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		this.attributeNameUtf8.accept(visitor, this, this.getParent());
		if (super.isParsedSuccessfully) {
			this.mainClass.accept(visitor, this, this.getParent());
		}
	}

	@Override
	public void updateClassReference(ClassInfoWriter oldReference, ClassInfoWriter newReference) {
		if (this.mainClass == oldReference) {
			this.mainClass = newReference;
		}
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.mainClass.getTag());
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			TypeWriter mappedMainClass = writerMap.get(this.mainClass);
			if (mappedMainClass != null) {
				dataOutputStream.writeShort(mappedMainClass.getTag());
			} else {
				dataOutputStream.writeShort(this.mainClass.getTag());
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void remapDescriptors(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageRenameMap, HashMap<String, String> classNameMap) {
	}
}
