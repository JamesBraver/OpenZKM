package com.zelix.framework.attributes.declaration;

import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.utils.ByteUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.workspace.ClassHierarchyNode;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Map;

public class InnerClassNode extends StructureNode implements SimpleReferenceListener, ReferenceUpdateListener {
	public boolean isValid = true;
	public int innerAccessFlags;
	public ClassInfoWriter innerClass;
	public ClassInfoWriter outerClass;
	public UTFStringWriter innerName;
	public byte[] rawBytes;

	public InnerClassNode(
		StructureNode parent,
		ByteDataBuffer dataBuffer,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		PrintWriter printWriter
	) throws IOException {
		super(parent);
		int innerClassIndex = dataBuffer.readUnsignedShort();
		int outerClassIndex = dataBuffer.readUnsignedShort();
		int innerNameIndex = dataBuffer.readUnsignedShort();
		this.innerAccessFlags = dataBuffer.readUnsignedShort();
		TypeWriter innerClassEntry = this.getConstantPoolEntry(innerClassIndex);
		TypeWriter outerClassEntry = this.getConstantPoolEntry(outerClassIndex);
		TypeWriter innerNameEntry = this.getConstantPoolEntry(innerNameIndex);
		if (innerClassEntry instanceof ClassInfoWriter && (outerClassIndex == 0 || outerClassEntry instanceof ClassInfoWriter)) {
			this.innerClass = (ClassInfoWriter)innerClassEntry;
			classReferences.put(this.innerClass, this);
			if (outerClassIndex != 0) {
				this.outerClass = (ClassInfoWriter)outerClassEntry;
				classReferences.put(this.outerClass, this);
			}

			if (innerNameIndex != 0) {
				if (innerNameEntry instanceof UTFStringWriter) {
					this.innerName = (UTFStringWriter)innerNameEntry;
					utf8References.put(this.innerName, this);
				} else {
					this.isValid = false;
				}
			}
		} else {
			this.isValid = false;
		}

		if (!this.isValid) {
			this.rawBytes = new byte[8];
			this.rawBytes[0] = (byte)ByteUtils.getHighByte(innerClassIndex);
			this.rawBytes[1] = (byte)ByteUtils.getLowByte(innerClassIndex);
			this.rawBytes[2] = (byte)ByteUtils.getHighByte(outerClassIndex);
			this.rawBytes[3] = (byte)ByteUtils.getLowByte(outerClassIndex);
			this.rawBytes[4] = (byte)ByteUtils.getHighByte(innerNameIndex);
			this.rawBytes[5] = (byte)ByteUtils.getLowByte(innerNameIndex);
			this.rawBytes[6] = (byte)ByteUtils.getHighByte(this.innerAccessFlags);
			this.rawBytes[7] = (byte)ByteUtils.getLowByte(this.innerAccessFlags);
		}
	}

	public void deriveSimpleName() {
		if (this.innerName != null && this.innerName.getValue().length() > 0) {
			String internalName = this.innerClass.getInternalName();
			int lastSeparator = internalName.lastIndexOf(36);
			String simpleName;
			if (lastSeparator != -1) {
				simpleName = internalName.substring(lastSeparator + 1);
			} else {
				simpleName = "";
			}

			this.innerName.setValue(simpleName);
		}
	}

	public String getSimpleName() {
		if (this.innerName != null) {
			String simpleName = this.innerName.getValue();
			if (!simpleName.equals("")) {
				return simpleName;
			}
		}

		return null;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		if (this.innerClass != null) {
			this.innerClass.accept(visitor, this, this);
		}

		if (this.outerClass != null) {
			this.outerClass.accept(visitor, this, this);
		}

		if (this.innerName != null) {
			this.innerName.accept(visitor, this, this);
		}
	}

	@Override
	public void updateClassReference(ClassInfoWriter oldReference, ClassInfoWriter newReference) {
		if (oldReference == this.innerClass) {
			this.innerClass = newReference;
		}

		if (oldReference == this.outerClass) {
			this.outerClass = newReference;
		}
	}

	public boolean isInnerClassNotExcluded(TrimExcludes trimExcludes) {
		ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(this.innerClass.getInternalName());
		return classNode != null ? !trimExcludes.isClassOrInnerClassExcluded(classNode) : false;
	}

	public int getAccessFlags() {
		return this.innerAccessFlags;
	}

	public String getEnclosingClassName() {
		if (this.outerClass == null) {
			return null;
		} else {
			String outerClassName = this.outerClass.getInternalName();
			String enclosingClassName = this.innerClass.getInternalName();

			while (enclosingClassName != null && !outerClassName.startsWith(enclosingClassName)) {
				int lastSeparator = enclosingClassName.lastIndexOf("$");
				if (lastSeparator > 0) {
					enclosingClassName = enclosingClassName.substring(0, lastSeparator);
				} else {
					enclosingClassName = null;
				}
			}

			return enclosingClassName;
		}
	}

	public void collectInnerClassReferences(HashSet<ClassNode> classReferences, HashSet<ClassNode> signatureClassReferences, HashSet<FieldNode> fieldReferences, HashSet<MethodNode> methodReferences) {
		if (this.outerClass != null) {
			ClassNode outerClassNode = ClassHierarchyNode.getWorkspaceClassNode(this.outerClass.getInternalName());
			if (outerClassNode != null) {
				signatureClassReferences.add(outerClassNode);
			}
		}

		if (this.innerClass != null) {
			ClassNode innerClassNode = ClassHierarchyNode.getWorkspaceClassNode(this.innerClass.getInternalName());
			if (innerClassNode != null) {
				signatureClassReferences.add(innerClassNode);
			}
		}
	}

	public String getOuterClassName() {
		if (this.outerClass != null) {
			String outerClassName = this.outerClass.getInternalName();
			if (!outerClassName.equals("")) {
				return outerClassName;
			}
		}

		return null;
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.innerName == oldReference) {
			this.innerName = newReference;
		}
	}

	public void writeToStream(DataOutputStream dataOutputStream) throws IOException {
		if (this.isValid) {
			dataOutputStream.writeShort(this.innerClass.getTag());
			dataOutputStream.writeShort(this.outerClass == null ? 0 : this.outerClass.getTag());
			dataOutputStream.writeShort(this.innerName == null ? 0 : this.innerName.getTag());
			dataOutputStream.writeShort(this.innerAccessFlags);
		} else {
			dataOutputStream.write(this.rawBytes);
		}
	}

	public String getInnerClassName() {
		if (this.innerClass != null) {
			String innerClassName = this.innerClass.getInternalName();
			if (!innerClassName.equals("")) {
				return innerClassName;
			}
		}

		return null;
	}

	public boolean isInnerClassIncluded(HashSet<ClassNode> includedClasses) {
		ClassNode classNode = ClassHierarchyNode.getWorkspaceClassNode(this.innerClass.getInternalName());
		return classNode != null ? includedClasses.contains(classNode) : false;
	}

	public void writeRemappedToStream(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> remappedConstants) throws IOException {
		if (this.isValid) {
			TypeWriter remappedInnerClass = remappedConstants.get(this.innerClass);
			if (remappedInnerClass != null) {
				dataOutputStream.writeShort(remappedInnerClass.getTag());
			} else {
				dataOutputStream.writeShort(this.innerClass.getTag());
			}

			if (this.outerClass != null) {
				TypeWriter remappedOuterClass = remappedConstants.get(this.outerClass);
				if (remappedOuterClass != null) {
					dataOutputStream.writeShort(remappedOuterClass.getTag());
				} else {
					dataOutputStream.writeShort(this.outerClass.getTag());
				}
			} else {
				dataOutputStream.writeShort(0);
			}

			if (this.innerName == null) {
				dataOutputStream.writeShort(0);
			} else {
				UTFStringWriter remappedInnerName = (UTFStringWriter)remappedConstants.get(this.innerName);
				if (remappedInnerName != null) {
					dataOutputStream.writeShort(remappedInnerName.getTag());
				} else {
					dataOutputStream.writeShort(this.innerName.getTag());
				}
			}

			dataOutputStream.writeShort(this.innerAccessFlags);
		} else {
			dataOutputStream.write(this.rawBytes);
		}
	}

	public boolean isStatic() {
		return (this.innerAccessFlags & 8) != 0;
	}
}
