package com.zelix.framework.attributes.declaration;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.base.AbstractAttributeNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.writer.AbstractNamedWriter;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.NameAndTypeWriter;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.workspace.AbstractWorkspace;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class EnclosingMethodAttribute extends AbstractAttributeNode implements ReferenceUpdateListener, AbstractNamedWriter {
	private ClassInfoWriter enclosingClass;
	private NameAndTypeWriter enclosingMethod;
	private AbstractMethodNode resolvedMethod;

	public EnclosingMethodAttribute(
		StructureNode parent,
		int nameIndex,
		String attributeName,
		ByteDataBuffer buf,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		PrintWriter printWriter
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		byte[] attributeBytes = new byte[this.length];
		buf.read(attributeBytes);
		ByteDataBuffer bodyBuffer = ByteDataBuffer.fromBytes(attributeBytes, false);
		if (this.length == 4) {
			int classIndex = bodyBuffer.readUnsignedShort();
			int methodIndex = bodyBuffer.readUnsignedShort();
			TypeWriter classEntry = this.getConstantPoolEntry(classIndex);
			if (classEntry != null && classEntry instanceof ClassInfoWriter) {
				this.enclosingClass = (ClassInfoWriter)classEntry;
				classReferences.put(this.enclosingClass, this);
			} else {
				super.isParsedSuccessfully = false;
				printWriter.println(
					"ERROR: " + this.getRawSourcePath() + " : Invalid EnclosingMethod Attribute (A) : index=" + classIndex + (classEntry != null ? " : tag=" + classEntry.getTypeTag() : "")
				);
			}

			if (methodIndex > 0) {
				TypeWriter methodEntry = this.getConstantPoolEntry(methodIndex);
				if (methodEntry != null && methodEntry instanceof NameAndTypeWriter) {
					this.enclosingMethod = (NameAndTypeWriter)methodEntry;
					nameAndTypeReferences.put(this.enclosingMethod, this);
					((AbstractClassNode)parent).markAsEnclosedByMethod();
				} else {
					super.isParsedSuccessfully = false;
					printWriter.println(
						"ERROR: " + this.getRawSourcePath() + " : Invalid EnclosingMethod Attribute (B) : index=" + methodIndex + (methodEntry != null ? " : tag=" + methodEntry.getTypeTag() : "")
					);
				}
			}

			if (!super.isParsedSuccessfully) {
				super.rawData = attributeBytes;
			}
		} else {
			super.isParsedSuccessfully = false;
			super.rawData = attributeBytes;
			printWriter.println("ERROR: " + this.getRawSourcePath() + " : Invalid EnclosingMethod Attribute (C) : length=" + this.length);
		}
	}

	@Override
	public NameAndTypeWriter setTarget(NameAndTypeWriter newTarget) {
		NameAndTypeWriter previousEnclosingMethod = this.enclosingMethod;
		this.enclosingMethod = newTarget;
		return previousEnclosingMethod;
	}

	public String getEnclosingClassName() {
		return super.isParsedSuccessfully ? this.enclosingClass.getInternalName() : null;
	}

	public void resolveEnclosingMethod(AbstractWorkspace workspace, ClassProvider classProvider) {
		if (super.isParsedSuccessfully) {
			AbstractClassNode classNode = this.getClassNode();
			Integer innerClassOrdinal = classNode.hasInnerClassOrdinal() ? classNode.getInnerClassOrdinal() : null;
			classNode = classProvider.getClassNode(this.enclosingClass.getInternalName(), innerClassOrdinal, "analyzing enclosing method in class '" + this.getSourcePath() + "' (A)");
			if (classNode != null && this.enclosingMethod != null) {
				this.resolvedMethod = classNode.findMethod(new MethodSignature(this.enclosingMethod.getName(), this.enclosingMethod.getDescriptor()));
			}
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this);
		if (super.isParsedSuccessfully) {
			if (this.enclosingClass != null) {
				this.enclosingClass.accept(visitor, this, this);
			}

			if (this.enclosingMethod != null) {
				this.enclosingMethod.accept(visitor, this, this);
			}
		}
	}

	@Override
	public void updateClassReference(ClassInfoWriter oldReference, ClassInfoWriter newReference) {
		if (this.enclosingClass == oldReference) {
			this.enclosingClass = newReference;
		}
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.enclosingClass.getTag());
			dataOutputStream.writeShort(this.enclosingMethod == null ? 0 : this.enclosingMethod.getTag());
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	public void syncMethodNameAndType() {
		if (super.isParsedSuccessfully && this.enclosingMethod != null) {
			if (this.resolvedMethod != null && !this.resolvedMethod.getName().equals(this.enclosingMethod.getName())) {
				this.enclosingMethod.setName(this.resolvedMethod.getName());
			}

			if (this.resolvedMethod != null && !this.resolvedMethod.getDescriptor().equals(this.enclosingMethod.getDescriptor())) {
				this.enclosingMethod.setDescriptor(this.resolvedMethod.getDescriptor());
			}
		}
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		if (super.isParsedSuccessfully) {
			ClassInfoWriter remappedEnclosingClass = (ClassInfoWriter)writerMap.get(this.enclosingClass);
			if (remappedEnclosingClass != null) {
				dataOutputStream.writeShort(remappedEnclosingClass.getTag());
			} else {
				dataOutputStream.writeShort(this.enclosingClass.getTag());
			}

			if (this.enclosingMethod == null) {
				dataOutputStream.writeShort(0);
			} else {
				NameAndTypeWriter remappedEnclosingMethod = (NameAndTypeWriter)writerMap.get(this.enclosingMethod);
				if (remappedEnclosingMethod != null) {
					dataOutputStream.writeShort(remappedEnclosingMethod.getTag());
				} else {
					dataOutputStream.writeShort(this.enclosingMethod.getTag());
				}
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}
}
