package com.zelix.framework.attributes.module;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.writer.ModuleNameWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class ModuleHashEntry extends StructureNode {
	private ModuleNameWriter moduleNameWriter;
	private final byte[] hash;
	private boolean isParsed;

	public ModuleHashEntry(StructureNode parent, ByteDataBuffer dataBuffer) throws IOException {
		super(parent);
		int moduleNameIndex = dataBuffer.readUnsignedShort();
		TypeWriter moduleNameConstant = parent.getConstantPoolEntry(moduleNameIndex);
		if (moduleNameConstant == null) {
			throw new ClassParseFailedException(
				parent.getClassNode().getRawSourcePath() + " : Illegal constant pool index in attribute : " + moduleNameIndex + " : File is probably corrupt (T)"
			);
		} else if (!(moduleNameConstant instanceof ModuleNameWriter)) {
			throw new ClassParseFailedException(
				parent.getClassNode().getRawSourcePath() + " : Invalid attribute : " + moduleNameIndex + " : '" + moduleNameConstant.getClass().getName() + "' : File is probably corrupt (U)"
			);
		} else {
			this.moduleNameWriter = (ModuleNameWriter)moduleNameConstant;
			int hashLength = dataBuffer.readUnsignedShort();
			this.hash = new byte[hashLength];

			for (int i = 0; i < hashLength; i++) {
				this.hash[i] = (byte)dataBuffer.readUnsignedByte();
			}

			this.isParsed = true;
		}
	}

	public void write(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeShort(this.moduleNameWriter.getTag());
		dataOutputStream.writeShort(this.hash.length);

		for (int i = 0; i < this.hash.length; i++) {
			dataOutputStream.writeByte(this.hash[i]);
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		this.moduleNameWriter.accept(visitor, this, this.getParent());
	}

	public void writeRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> remappedConstants) throws IOException {
		TypeWriter mappedModuleName = remappedConstants.get(this.moduleNameWriter);
		if (mappedModuleName != null) {
			dataOutputStream.writeShort(mappedModuleName.getTag());
		} else {
			dataOutputStream.writeShort(this.moduleNameWriter.getTag());
		}

		dataOutputStream.writeShort(this.hash.length);

		for (int i = 0; i < this.hash.length; i++) {
			dataOutputStream.writeByte(this.hash[i]);
		}
	}
}
