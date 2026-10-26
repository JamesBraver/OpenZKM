package com.zelix.framework.attributes.declaration;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.AttributesNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.utils.enumerations.ArrayEnumeration;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

public class ExceptionsAttribute extends AttributesNode implements ReferenceUpdateListener {
	private int exceptionCount;
	private ClassInfoWriter[] exceptionClasses;

	public ExceptionsAttribute(UTFStringWriter nameUtf8, ClassInfoWriter[] exceptionClasses) {
		super(null, nameUtf8, exceptionClasses.length * 2 + 2);
		this.exceptionCount = exceptionClasses.length;
		this.exceptionClasses = exceptionClasses;
	}

	public ExceptionsAttribute(
		StructureNode parent,
		int nameIndex,
		String attributeName,
		ByteDataBuffer buf,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		this.exceptionCount = buf.readUnsignedShort();
		this.exceptionClasses = new ClassInfoWriter[this.exceptionCount];

		for (int i = 0; i < this.exceptionCount; i++) {
			int classIndex = buf.readUnsignedShort();
			TypeWriter classEntry = this.getConstantPoolEntry(classIndex);
			if (!(classEntry instanceof ClassInfoWriter)) {
				throw new ClassParseFailedException(this.getInternalName() + " : Invalid Class index in Exception Attribute");
			}

			this.exceptionClasses[i] = (ClassInfoWriter)classEntry;
			classReferences.put(this.exceptionClasses[i], this);
		}
	}

	public ClassInfoWriter getExceptionClass(int index) {
		return this.exceptionClasses[index];
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this);

		for (int i = 0; i < this.exceptionClasses.length; i++) {
			this.exceptionClasses[i].accept(visitor, this, this);
		}
	}

	@Override
	public void updateClassReference(ClassInfoWriter oldReference, ClassInfoWriter newReference) {
		for (int i = 0; i < this.exceptionClasses.length; i++) {
			if (this.exceptionClasses[i] == oldReference) {
				this.exceptionClasses[i] = newReference;
				break;
			}
		}
	}

	public int getExceptionCount() {
		return this.exceptionCount;
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		dataOutputStream.writeShort(this.exceptionCount);

		for (int i = 0; i < this.exceptionCount; i++) {
			dataOutputStream.writeShort(this.exceptionClasses[i].getTag());
		}
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		dataOutputStream.writeShort(this.exceptionCount);

		for (int i = 0; i < this.exceptionCount; i++) {
			TypeWriter remappedExceptionClass = writerMap.get(this.exceptionClasses[i]);
			if (remappedExceptionClass != null) {
				dataOutputStream.writeShort(remappedExceptionClass.getTag());
			} else {
				dataOutputStream.writeShort(this.exceptionClasses[i].getTag());
			}
		}
	}

	public Enumeration<ClassInfoWriter> getExceptionClasses() {
		ClassInfoWriter[] exceptionClasses = new ClassInfoWriter[this.exceptionClasses.length];

		for (int i = 0; i < this.exceptionClasses.length; i++) {
			exceptionClasses[i] = this.exceptionClasses[i];
		}

		return new ArrayEnumeration<>(exceptionClasses);
	}

	public Enumeration<String> getExceptionInternalNames() {
		String[] internalNames = new String[this.exceptionClasses.length];

		for (int i = 0; i < this.exceptionClasses.length; i++) {
			internalNames[i] = this.exceptionClasses[i].getInternalName();
		}

		return new ArrayEnumeration<>(internalNames);
	}
}
