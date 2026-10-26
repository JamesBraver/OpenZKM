package com.zelix.framework.writer;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.TypeNode;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.BaseMethodHandleWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class MethodHandleInfoWriter extends BaseMethodHandleWriter implements Writer {
	public final int referenceKind;
	public final int referenceIndex;

	public MethodHandleInfoWriter(int tag, ByteDataBuffer dataBuffer, AbstractConstantPoolWriter constantPool) throws IOException {
		super(tag, constantPool);
		this.referenceKind = dataBuffer.readUnsignedByte();
		this.referenceIndex = dataBuffer.readUnsignedShort();
	}

	@Override
	public TypeWriter resolve(
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences,
		PrintWriter errorWriter
	) {
		return this.createResolvedMethodHandle(utf8References, nameAndTypeReferences, classReferences, memberReferences, errorWriter);
	}


	public ResolvedMethodHandleWriter createResolvedMethodHandle(
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences,
		PrintWriter errorWriter
	) {
		MethodHandleKind methodHandleType = MethodHandleKind.fromReferenceKind(this.referenceKind);
		if (methodHandleType == null) {
			throw new ClassParseFailedException(this.constantPool.getOwnerSourcePath() + " : Invalid reference kind : " + this.referenceKind + " (1)");
		} else {
			TypeWriter referencedEntry = this.constantPool.getConstantPoolEntry(this.referenceIndex);
			if (referencedEntry.isResolvable()) {
				return null;
			} else {
				try {
					switch (methodHandleType) {
						case REF_GET_FIELD:
						case REF_GET_STATIC:
						case REF_PUT_FIELD:
						case REF_PUT_STATIC:
							if (referencedEntry instanceof FieldTypeWriter) {
								return new ResolvedMethodHandleWriter(this.getTag(), this.constantPool, methodHandleType, (FieldTypeWriter)referencedEntry, memberReferences);
							}

							throw new ClassParseFailedException(
								this.constantPool.getOwnerSourcePath()
									+ " : Invalid Constant Pool field index : "
									+ this.referenceIndex
									+ " : "
									+ TypeWriter.getConstantPoolTagName(this.getTypeTag())
									+ " entry"
							);
						case REF_INVOKE_VIRTUAL:
						case REF_INVOKE_STATIC:
						case REF_INVOKE_SPECIAL:
						case REF_NEW_INVOKE_SPECIAL:
							if (referencedEntry instanceof MethodCallWriter) {
								if (!MethodHandleKind.isValidMethodName(methodHandleType, ((MethodCallWriter)referencedEntry).getName())) {
									throw new ClassParseFailedException(
										this.constantPool.getOwnerSourcePath()
											+ " : Invalid method name for reference kind "
											+ methodHandleType
											+ " : "
											+ ((MethodCallWriter)referencedEntry).getName()
											+ " : "
											+ TypeWriter.getConstantPoolTagName(this.getTypeTag())
											+ " entry"
									);
								}

								return new ResolvedMethodHandleWriter(this.getTag(), this.constantPool, methodHandleType, (MethodCallWriter)referencedEntry, memberReferences);
							} else {
								if (referencedEntry instanceof InterfaceMethodCallWriter) {
									if (!MethodHandleKind.isValidMethodName(methodHandleType, ((InterfaceMethodCallWriter)referencedEntry).getName())) {
										throw new ClassParseFailedException(
											this.constantPool.getOwnerSourcePath()
												+ " : Invalid method name for reference kind "
												+ methodHandleType
												+ " : "
												+ ((InterfaceMethodCallWriter)referencedEntry).getName()
												+ " : "
												+ TypeWriter.getConstantPoolTagName(this.getTypeTag())
												+ " entry"
										);
									}

									return new ResolvedMethodHandleWriter(this.getTag(), this.constantPool, methodHandleType, (InterfaceMethodCallWriter)referencedEntry, memberReferences);
								}

								throw new ClassParseFailedException(
									this.constantPool.getOwnerSourcePath()
										+ " : Invalid Constant Pool field index : "
										+ this.referenceIndex
										+ " : "
										+ TypeWriter.getConstantPoolTagName(this.getTypeTag())
										+ " entry"
								);
							}
						case REF_INVOKE_INTERFACE:
							if (referencedEntry instanceof InterfaceMethodCallWriter) {
								if (!MethodHandleKind.isValidMethodName(methodHandleType, ((InterfaceMethodCallWriter)referencedEntry).getName())) {
									throw new ClassParseFailedException(
										this.constantPool.getOwnerSourcePath()
											+ " : Invalid method name for reference kind "
											+ methodHandleType
											+ " : "
											+ ((InterfaceMethodCallWriter)referencedEntry).getName()
											+ " : "
											+ TypeWriter.getConstantPoolTagName(this.getTypeTag())
											+ " entry"
									);
								}

								return new ResolvedMethodHandleWriter(this.getTag(), this.constantPool, methodHandleType, (InterfaceMethodCallWriter)referencedEntry, memberReferences);
							}

							throw new ClassParseFailedException(
								this.constantPool.getOwnerSourcePath()
									+ " : Invalid Constant Pool field index : "
									+ this.referenceIndex
									+ " : "
									+ TypeWriter.getConstantPoolTagName(this.getTypeTag())
									+ " entry"
							);
						default:
							throw new ClassParseFailedException(this.constantPool.getOwnerSourcePath() + " : Invalid reference kind : " + this.referenceKind + " (2)");
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
		}
	}

	@Override
	public boolean isResolvable() {
		return true;
	}

	@Override
	public void writeToStream(DataOutputStream dos) throws IOException {
		dos.writeByte(15);
		dos.writeByte(this.referenceKind);
		dos.writeShort(this.referenceIndex);
	}
}
