package com.zelix.framework.attributes.declaration;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.base.AbstractAttributeNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.workspace.ClassHierarchyNode;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class PermittedSubclassesAttribute extends AbstractAttributeNode implements SimpleReferenceListener, ReferenceUpdateListener {
	private ClassInfoWriter[] permittedSubclasses;

	public PermittedSubclassesAttribute(
		StructureNode parent,
		int nameIndex,
		String attributeName,
		ByteDataBuffer buf,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		PrintWriter printWriter
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		byte[] attributeBytes = new byte[this.length];
		buf.read(attributeBytes);
		try (ByteDataBuffer bodyBuffer = ByteDataBuffer.fromBytes(attributeBytes, false)) {
			int subclassCount = bodyBuffer.readUnsignedShort();
			this.permittedSubclasses = new ClassInfoWriter[subclassCount];

			for (int i = 0; i < subclassCount; i++) {
				int classIndex = bodyBuffer.readUnsignedShort();
				TypeWriter classEntry = this.getConstantPoolEntry(classIndex);
				if (classEntry instanceof ClassInfoWriter) {
					this.permittedSubclasses[i] = (ClassInfoWriter)classEntry;
					classReferences.put(this.permittedSubclasses[i], this);
				} else {
					super.isParsedSuccessfully = false;
					super.rawData = attributeBytes;
					printWriter.println("ERROR: " + this.getSourcePath() + " : " + this.getAttributeName() + " (A) Invalid permitted class index " + classIndex);
				}
			}
		}

		if (super.isParsedSuccessfully) {
			((AbstractClassNode)parent).setSealed(true);
		}
	}

	public void collectAllReferences(HashSet<ClassNode> classReferences, HashSet<ClassNode> signatureClassReferences, HashSet<FieldNode> fieldReferences, HashSet<MethodNode> methodReferences) {
		if (super.isParsedSuccessfully) {
			ClassInfoWriter[] subclasses = this.permittedSubclasses;
			int subclassCount = subclasses.length;

			for (int i = 0; i < subclassCount; i++) {
				ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(subclasses[i].getInternalName());
				if (classNode != null) {
					signatureClassReferences.add(classNode);
				}
			}
		}
	}

	public int trimExcludedSubclasses(TrimExcludes trimExcludes) {
		if (super.isParsedSuccessfully) {
			ArrayList<ClassInfoWriter> remainingSubclasses = new ArrayList<>(this.permittedSubclasses.length);

			for (ClassInfoWriter subclass : this.permittedSubclasses) {
				ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(subclass.getInternalName());
				if (classNode == null || trimExcludes.isClassOrInnerClassExcluded(classNode)) {
					remainingSubclasses.add(subclass);
				}
			}

			if (remainingSubclasses.size() < this.permittedSubclasses.length) {
				this.permittedSubclasses = remainingSubclasses.toArray(new ClassInfoWriter[remainingSubclasses.size()]);
			}
		}

		return this.permittedSubclasses.length;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this.getParent());
		ClassInfoWriter[] subclasses = this.permittedSubclasses;
		int subclassCount = subclasses.length;

		for (int i = 0; i < subclassCount; i++) {
			subclasses[i].accept(visitor, this, this);
		}
	}

	@Override
	public void updateClassReference(ClassInfoWriter oldReference, ClassInfoWriter newReference) {
		for (int i = 0; i < this.permittedSubclasses.length; i++) {
			if (this.permittedSubclasses[i] == oldReference) {
				this.permittedSubclasses[i] = newReference;
				break;
			}
		}
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.permittedSubclasses.length);

			for (ClassInfoWriter subclass : this.permittedSubclasses) {
				dataOutputStream.writeShort(subclass.getTag());
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		super.updateSimpleNameReference(oldReference, newReference);
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.permittedSubclasses.length);

			for (ClassInfoWriter subclass : this.permittedSubclasses) {
				TypeWriter remappedSubclass = writerMap.get(subclass);
				if (remappedSubclass != null) {
					dataOutputStream.writeShort(remappedSubclass.getTag());
				} else {
					dataOutputStream.writeShort(subclass.getTag());
				}
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public int getAttributeLength() {
		int attributeLength = 2 + this.permittedSubclasses.length * 2;
		this.exchangeLength(attributeLength);
		return attributeLength;
	}

	@Override
	public void remapDescriptors(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageRenameMap, HashMap<String, String> classNameMap) {
	}
}
