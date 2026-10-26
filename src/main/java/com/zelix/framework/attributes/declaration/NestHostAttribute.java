package com.zelix.framework.attributes.declaration;

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

public class NestHostAttribute extends AbstractAttributeNode implements SimpleReferenceListener, ReferenceUpdateListener {
	public ClassInfoWriter hostClass;

	public NestHostAttribute(
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
		try (ByteDataBuffer bodyBuffer = ByteDataBuffer.fromBytes(super.rawData, false)) {
			int classIndex = bodyBuffer.readUnsignedShort();
			TypeWriter classEntry = parent.getConstantPoolEntry(classIndex);
			if (classEntry == null) {
				super.isParsedSuccessfully = false;
				throw new ClassParseFailedException(
					parent.getClassNode().getRawSourcePath()
						+ " : Illegal constant pool index in 'NestHost' attribute : "
						+ classIndex
						+ " : '"
						+ this.getRawSourcePath()
						+ "' : File is probably corrupt (AV)"
				);
			}

			if (!(classEntry instanceof ClassInfoWriter)) {
				super.isParsedSuccessfully = false;
				throw new ClassParseFailedException(
					parent.getClassNode().getRawSourcePath()
						+ " : Invalid attribute : 'NestHost' : "
						+ classIndex
						+ " : '"
						+ this.getRawSourcePath()
						+ "' : File is probably corrupt (AW)"
				);
			}

			this.hostClass = (ClassInfoWriter)classEntry;
			classReferences.put(this.hostClass, this);
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		this.attributeNameUtf8.accept(visitor, this, this.getParent());
		if (super.isParsedSuccessfully) {
			this.hostClass.accept(visitor, this, this.getParent());
		}
	}

	public String getHostClassInternalName() {
		return this.hostClass.getInternalName();
	}

	@Override
	public void updateClassReference(ClassInfoWriter oldReference, ClassInfoWriter newReference) {
		if (this.hostClass == oldReference) {
			this.hostClass = newReference;
		}
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.hostClass.getTag());
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			TypeWriter remappedHostClass = writerMap.get(this.hostClass);
			if (remappedHostClass != null) {
				dataOutputStream.writeShort(remappedHostClass.getTag());
			} else {
				dataOutputStream.writeShort(this.hostClass.getTag());
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void remapDescriptors(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageRenameMap, HashMap<String, String> classNameMap) {
	}
}
