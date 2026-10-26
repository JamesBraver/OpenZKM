package com.zelix.framework.writer.base;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.TypeNode;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.*;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

//CONSTANT_Fieldref_info {
//    u1 tag;
//    u2 class_index;
//    u2 name_and_type_index;
//}
//CONSTANT_Methodref_info {
//    u1 tag;
//    u2 class_index;
//    u2 name_and_type_index;
//}
//CONSTANT_InterfaceMethodref_info {
//    u1 tag;
//    u2 class_index;
//    u2 name_and_type_index;
//}
public abstract class AbstractMemberRefInfoWriter extends TypeWriter implements Writer {
	public int classIndex;
	public int nameAndTypeIndex;

	public AbstractMemberRefInfoWriter(int tag, ByteDataBuffer dataBuffer, AbstractConstantPoolWriter constantPool) throws IOException {
		super(tag, constantPool);
		this.classIndex = dataBuffer.readUnsignedShort();
		this.nameAndTypeIndex = dataBuffer.readUnsignedShort();
	}

	@Override
	public final TypeWriter resolve(
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences,
		PrintWriter errorWriter
	) {
		try {
			TypeWriter classEntry = this.constantPool.getConstantPoolEntry(this.classIndex);
			TypeWriter nameAndTypeEntry = this.constantPool.getConstantPoolEntry(this.nameAndTypeIndex);
			if (classEntry.isResolvable() || nameAndTypeEntry.isResolvable()) {
				return null;
			} else if (!(classEntry instanceof ClassInfoWriter)) {
				String invalidClassIndexMessage = this.constantPool.getOwnerRawSourcePath()
					+ " : Invalid Constant Pool class index : "
					+ this.classIndex
					+ " : "
					+ TypeWriter.getConstantPoolTagName(this.getTypeTag())
					+ " entry";
				throw new ClassParseFailedException(invalidClassIndexMessage);
			} else if (!(nameAndTypeEntry instanceof NameAndTypeWriter)) {
				String invalidNameAndTypeIndexMessage = this.constantPool.getOwnerRawSourcePath()
					+ " : Invalid Constant Pool name and type index : "
					+ this.nameAndTypeIndex
					+ " : "
					+ TypeWriter.getConstantPoolTagName(this.getTypeTag())
					+ " entry";
				throw new ClassParseFailedException(invalidNameAndTypeIndexMessage);
			} else {
				MemberTypeWriter resolvedMemberWriter = this.createResolvedMemberWriter(this, (ClassInfoWriter)classEntry, (NameAndTypeWriter)nameAndTypeEntry, classReferences);
				if (nameAndTypeReferences != null) {
					nameAndTypeReferences.put((NameAndTypeWriter)nameAndTypeEntry, resolvedMemberWriter);
				}

				return resolvedMemberWriter;
			}
		} catch (ArrayIndexOutOfBoundsException indexOutOfRangeException) {
			String indexOutOfRangeMessage = this.constantPool.getOwnerRawSourcePath()
				+ " : Constant Pool index out of range : "
				+ indexOutOfRangeException.getMessage()
				+ " : "
				+ TypeWriter.getConstantPoolTagName(this.getTypeTag())
				+ " entry";
			throw new ClassParseFailedException(indexOutOfRangeMessage);
		}
	}

	@Override
	public boolean isResolvable() {
		return true;
	}

	public abstract MemberTypeWriter createResolvedMemberWriter(
		AbstractMemberRefInfoWriter rawMemberRefWriter,
		ClassInfoWriter owner,
		NameAndTypeWriter target,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences
	);

	@Override
	public void writeToStream(DataOutputStream dos) throws IOException {
		dos.writeByte(this.getTypeTag().getTagID());
		dos.writeShort(this.classIndex);
		dos.writeShort(this.nameAndTypeIndex);
	}
}
