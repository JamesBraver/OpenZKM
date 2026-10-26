package com.zelix.framework.attributes.declaration;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AttributesNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.AbstractClassInfoWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

public class InnerClassesAttribute extends AttributesNode implements SimpleReferenceListener {
	public boolean isValid = true;
	public int count;
	public InnerClassNode[] entries;
	public byte[] rawBytes;

	public InnerClassesAttribute(
		StructureNode parent,
		int nameIndex,
		String attributeName,
		ByteDataBuffer buf,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		PrintWriter printWriter
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		if (this.length >= 2) {
			this.count = buf.readUnsignedShort();
			if (this.count * 8 + 2 == this.length) {
				this.entries = new InnerClassNode[this.count];

				for (int i = 0; i < this.count; i++) {
					this.entries[i] = new InnerClassNode(this, buf, utf8References, classReferences, printWriter);
					if (this.isValid) {
						if (!this.entries[i].isValid) {
							this.setValid(false);
							printWriter.println("ERROR: " + this.getInternalName() + " : Invalid InnerClasses Attribute (3)");
						} else {
							String simpleName = this.entries[i].getSimpleName();
							if (simpleName != null) {
								String innerClassName = this.entries[i].getInnerClassName();
								String outerClassName = this.entries[i].getOuterClassName();
								if (outerClassName != null && outerClassName.length() > innerClassName.length() - simpleName.length()) {
									printWriter.println("ERROR: " + this.getInternalName() + " : Invalid InnerClasses Attribute (5) : '" + outerClassName + "' : '" + innerClassName + "' : '" + simpleName + "'");
								}
							}
						}
					}
				}

				if (!this.isValid) {
					ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream(this.length);
					DataOutputStream outputStream = new DataOutputStream(outputBuffer);
					this.writeEntries(outputStream);
					this.rawBytes = outputBuffer.toByteArray();
					this.entries = null;
				}
			} else {
				this.setValid(false);
				printWriter.println("ERROR: " + this.getInternalName() + " : Invalid InnerClasses Attribute (1)");
				this.rawBytes = new byte[this.length];
				this.rawBytes[0] = (byte)(this.count >>> 8 & 0xFF);
				this.rawBytes[1] = (byte)(this.count >>> 0 & 0xFF);
				buf.read(this.rawBytes, 2, this.length - 2);
			}
		} else {
			this.setValid(false);
			printWriter.println("ERROR: " + this.getInternalName() + " : Invalid InnerClasses Attribute (2)");
			this.rawBytes = new byte[this.length];
			buf.read(this.rawBytes);
		}

		if (this.isValid) {
			((AbstractClassNode)parent).setHasValidInnerClasses(true);
		} else {
			((AbstractClassNode)parent).setHasInvalidInnerClasses(true);
		}
	}

	public InnerClassNode findInnerClassEntry(AbstractClassInfoWriter classWriter) {
		if (this.isValid) {
			for (int i = 0; i < this.count; i++) {
				InnerClassNode entry = this.entries[i];
				if (entry.innerClass != null && entry.innerClass.getInternalName().equals(classWriter.getInternalName())) {
					return entry;
				}
			}
		}

		return null;
	}

	public void collectInnerClassReferences(HashSet<ClassNode> classReferences, HashSet<ClassNode> signatureClassReferences, HashSet<FieldNode> fieldReferences, HashSet<MethodNode> methodReferences) {
		if (this.isValid) {
			for (int i = 0; i < this.count; i++) {
				this.entries[i].collectInnerClassReferences(classReferences, signatureClassReferences, fieldReferences, methodReferences);
			}
		}
	}

	public void setValid(boolean valid) {
		this.isValid = valid;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this.getParent());
		if (this.entries != null) {
			for (int i = 0; i < this.entries.length; i++) {
				this.entries[i].acceptVisitor(visitor);
			}
		}
	}

	public int removeIncludedEntries(HashSet<ClassNode> includedClasses) {
		if (this.entries != null) {
			ArrayList<InnerClassNode> remainingEntries = new ArrayList<>(this.entries.length);

			for (int i = 0; i < this.entries.length; i++) {
				if (!this.entries[i].isInnerClassIncluded(includedClasses)) {
					remainingEntries.add(this.entries[i]);
				}
			}

			int remainingCount = remainingEntries.size();
			if (remainingCount < this.entries.length) {
				this.entries = remainingEntries.toArray(new InnerClassNode[remainingCount]);
				this.count = remainingCount;
			}
		}

		return this.count;
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (this.isValid) {
			this.writeEntries(dataOutputStream);
		} else {
			dataOutputStream.write(this.rawBytes);
		}
	}

	public void deriveSimpleNames() {
		if (this.isValid) {
			for (int i = 0; i < this.count; i++) {
				this.entries[i].deriveSimpleName();
			}
		}
	}

	public int removeNonExcludedEntries(TrimExcludes trimExcludes) {
		if (this.entries != null) {
			ArrayList<InnerClassNode> remainingEntries = new ArrayList<>(this.entries.length);

			for (int i = 0; i < this.entries.length; i++) {
				if (!this.entries[i].isInnerClassNotExcluded(trimExcludes)) {
					remainingEntries.add(this.entries[i]);
				}
			}

			int remainingCount = remainingEntries.size();
			if (remainingCount < this.entries.length) {
				this.entries = remainingEntries.toArray(new InnerClassNode[remainingCount]);
				this.count = remainingCount;
			}
		}

		return this.count;
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		if (this.isValid) {
			dataOutputStream.writeShort(this.count);

			for (int i = 0; i < this.count; i++) {
				this.entries[i].writeRemappedToStream(dataOutputStream, writerMap);
			}
		} else {
			dataOutputStream.write(this.rawBytes);
		}
	}

	protected void writeEntries(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeShort(this.count);

		for (int i = 0; i < this.count; i++) {
			this.entries[i].writeToStream(dataOutputStream);
		}
	}

	@Override
	public int getAttributeLength() {
		return 2 + this.count * 8;
	}
}
