package com.zelix.framework.writer;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.TypeNode;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.BaseMethodTypeWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class MethodTypeWriter extends BaseMethodTypeWriter implements Writer {
	public final int descriptorIndex;

	public MethodTypeWriter(int tag, ByteDataBuffer dataBuffer, AbstractConstantPoolWriter constantPool) throws IOException {
		super(tag, constantPool);
		this.descriptorIndex = dataBuffer.readUnsignedShort();
	}

	@Override
	public TypeWriter resolve(
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences,
		PrintWriter errorWriter
	) {
		return this.createResolvedWriter(utf8References, nameAndTypeReferences, classReferences, memberReferences, errorWriter);
	}

	public MethodDescriptorWriter createResolvedWriter(
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences,
		PrintWriter errorWriter
	) {
		try {
			TypeWriter utf8Entry = this.constantPool.getConstantPoolEntry(this.descriptorIndex);
			if (utf8Entry instanceof UTFStringWriter) {
				MethodDescriptorWriter methodDescriptorWriter = new MethodDescriptorWriter(this.getTag(), this.constantPool, (UTFStringWriter)utf8Entry);
				if (utf8References != null) {
					utf8References.put((UTFStringWriter)utf8Entry, methodDescriptorWriter);
				}

				String descriptor = methodDescriptorWriter.getDescriptor();
				if (descriptor.startsWith("(") && descriptor.indexOf(")") != -1 && descriptor.indexOf(")") < descriptor.length() - 1) {
					return methodDescriptorWriter;
				} else {
					throw new ClassParseFailedException(
						this.constantPool.getOwnerSourcePath()
							+ " : Invalid method descriptor : "
							+ descriptor
							+ " : "
							+ TypeWriter.getConstantPoolTagName(this.getTypeTag())
							+ " entry"
					);
				}
			} else {
				throw new ClassParseFailedException(
					this.constantPool.getOwnerSourcePath()
						+ " : Invalid Constant Pool UTF8 index : "
						+ this.descriptorIndex
						+ " : "
						+ TypeWriter.getConstantPoolTagName(this.getTypeTag())
						+ " entry"
				);
			}
		} catch (ArrayIndexOutOfBoundsException ignored) {
			String outOfRangeMessage = this.constantPool.getOwnerSourcePath()
				+ " : Constant Pool index out of range : "
				+ ignored.getMessage()
				+ " : "
				+ TypeWriter.getConstantPoolTagName(this.getTypeTag())
				+ " entry";
			throw new ClassParseFailedException(outOfRangeMessage);
		}
	}

	@Override
	public boolean isResolvable() {
		return true;
	}

	@Override
	public void writeToStream(DataOutputStream dos) throws IOException {
		dos.writeByte(BaseMethodTypeWriter.METHOD_TYPE.getTagID());
		dos.writeShort(this.descriptorIndex);
	}
}
