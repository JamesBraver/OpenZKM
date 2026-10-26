package com.zelix.framework.modules;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class ModuleProvidesNode extends StructureNode implements ReferenceUpdateListener {
	private ClassInfoWriter serviceInterface;
	private ClassInfoWriter[] serviceImplementations;

	public ModuleProvidesNode(StructureNode parent, ByteDataBuffer dataBuffer, MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences) throws IOException {
		super(parent);
		int interfaceIndex = dataBuffer.readUnsignedShort();
		TypeWriter constantPoolEntry = parent.getConstantPoolEntry(interfaceIndex);
		if (constantPoolEntry == null) {
			throw new ClassParseFailedException(
				parent.getClassNode().getRawSourcePath() + " : Illegal constant pool index in attribute : " + interfaceIndex + " : File is probably corrupt (AF)"
			);
		} else if (!(constantPoolEntry instanceof ClassInfoWriter)) {
			throw new ClassParseFailedException(
				parent.getClassNode().getRawSourcePath()
					+ " : Invalid attribute : "
					+ interfaceIndex
					+ " : '"
					+ constantPoolEntry.getClass().getName()
					+ "' : File is probably corrupt (AG)"
			);
		} else {
			this.serviceInterface = (ClassInfoWriter)constantPoolEntry;
			classReferences.put(this.serviceInterface, this);
			int implementationCount = dataBuffer.readUnsignedShort();
			this.serviceImplementations = new ClassInfoWriter[implementationCount];

			for (int i = 0; i < implementationCount; i++) {
				int implementationIndex = dataBuffer.readUnsignedShort();
				constantPoolEntry = parent.getConstantPoolEntry(implementationIndex);
				if (constantPoolEntry == null) {
					throw new ClassParseFailedException(
						parent.getClassNode().getRawSourcePath() + " : Illegal constant pool index in attribute : " + implementationIndex + " : File is probably corrupt (AH)"
					);
				}

				if (!(constantPoolEntry instanceof ClassInfoWriter)) {
					throw new ClassParseFailedException(
						parent.getClassNode().getRawSourcePath() + " : Invalid attribute : " + implementationIndex + " : '" + constantPoolEntry.getClass().getName() + "' : File is probably corrupt (AI)"
					);
				}

				this.serviceImplementations[i] = (ClassInfoWriter)constantPoolEntry;
				classReferences.put(this.serviceImplementations[i], this);
			}
		}
	}

	public void writeNodeRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> remappedConstants) throws IOException {
		TypeWriter remappedWriter = remappedConstants.get(this.serviceInterface);
		if (remappedWriter != null) {
			dataOutputStream.writeShort(remappedWriter.getTag());
		} else {
			dataOutputStream.writeShort(this.serviceInterface.getTag());
		}

		dataOutputStream.writeShort(this.serviceImplementations.length);

		for (ClassInfoWriter implementation : this.serviceImplementations) {
			remappedWriter = remappedConstants.get(implementation);
			if (remappedWriter != null) {
				dataOutputStream.writeShort(remappedWriter.getTag());
			} else {
				dataOutputStream.writeShort(implementation.getTag());
			}
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.serviceInterface, this, this.getParent());

		for (ClassInfoWriter implementation : this.serviceImplementations) {
			visitor.visit(implementation, this, this.getParent());
		}
	}

	@Override
	public void updateClassReference(ClassInfoWriter oldReference, ClassInfoWriter newReference) {
		if (this.serviceInterface == oldReference) {
			this.serviceInterface = newReference;
		}

		for (int i = 0; i < this.serviceImplementations.length; i++) {
			if (this.serviceImplementations[i] == oldReference) {
				this.serviceImplementations[i] = newReference;
			}
		}
	}

	public void writeNode(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeShort(this.serviceInterface.getTag());
		dataOutputStream.writeShort(this.serviceImplementations.length);

		for (ClassInfoWriter implementation : this.serviceImplementations) {
			dataOutputStream.writeShort(implementation.getTag());
		}
	}
}
