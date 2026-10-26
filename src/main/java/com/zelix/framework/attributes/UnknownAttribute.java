package com.zelix.framework.attributes;

import com.zelix.framework.AttributesNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.utils.ByteUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.ThreeLevelMultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class UnknownAttribute extends AttributesNode implements SimpleReferenceListener {
	public static final Object NON_UTF8_SENTINEL = new Object();
	private byte[] rawData = new byte[this.length];
	private boolean isParsedSuccessfully;
	private boolean isSharedByManyClasses;
	private int[] constantIndexes;
	private ArrayList<TypeWriter> constantPoolEntries;

	public UnknownAttribute(
		StructureNode parent,
		int nameIndex,
		String attributeName,
		ByteDataBuffer buf,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributeRefs
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		buf.read(this.rawData);
		if (this.rawData.length % 2 == 0) {
			String parentClassName = parent.getClass().getName();
			this.isParsedSuccessfully = true;
			this.isSharedByManyClasses = true;
			int entryCount = this.rawData.length / 2;
			this.constantIndexes = new int[entryCount];
			this.constantPoolEntries = new ArrayList<>(entryCount);
			int byteOffset = 0;

			for (int i = 0; i < entryCount; i++) {
				int constantPoolIndex = ByteUtils.combineBytes(this.rawData[byteOffset++], this.rawData[byteOffset++]);
				this.constantIndexes[i] = constantPoolIndex;
				TypeWriter constantPoolEntry = parent.getConstantPoolEntry(constantPoolIndex);
				if (constantPoolEntry != null && constantPoolEntry instanceof UTFStringWriter) {
					this.constantPoolEntries.add(constantPoolEntry);
					unknownAttributeRefs.put(parentClassName, this.getAttributeName(), this, constantPoolEntry);
					utf8References.put((UTFStringWriter)constantPoolEntry, this);
				} else {
					this.constantPoolEntries.add(null);
					unknownAttributeRefs.put(parentClassName, this.getAttributeName(), this, NON_UTF8_SENTINEL);
				}
			}
		}
	}

	public void clearResolvedConstant(int index) {
		this.constantPoolEntries.set(index, null);
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this.getParent());
	}

	public boolean isSharedByManyClasses() {
		return this.isSharedByManyClasses;
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (this.isParsedSuccessfully) {
			for (int i = 0; i < this.constantPoolEntries.size(); i++) {
				Object entry = this.constantPoolEntries.get(i);
				if (entry == null) {
					dataOutputStream.writeShort(this.constantIndexes[i]);
				} else {
					TypeWriter typeWriter = (TypeWriter)entry;
					dataOutputStream.writeShort(typeWriter.getTag());
				}
			}
		} else {
			dataOutputStream.write(this.rawData);
		}
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.attributeNameUtf8 == oldReference) {
			this.attributeNameUtf8 = newReference;
		} else if (this.isParsedSuccessfully) {
			for (int i = 0; i < this.constantPoolEntries.size(); i++) {
				if (this.constantPoolEntries.get(i) == oldReference) {
					this.constantPoolEntries.set(i, newReference);
					break;
				}
			}
		}
	}

	public void setParsedSuccessfully(boolean parsed) {
		this.isParsedSuccessfully = parsed;
		if (!this.isParsedSuccessfully) {
			this.constantIndexes = null;
			this.constantPoolEntries = null;
			this.isSharedByManyClasses = false;
		}
	}

	public void setSharedByManyClasses(boolean sharedByManyClasses) {
		this.isSharedByManyClasses = sharedByManyClasses;
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		if (this.isParsedSuccessfully) {
			for (int i = 0; i < this.constantPoolEntries.size(); i++) {
				Object entry = this.constantPoolEntries.get(i);
				if (entry == null) {
					dataOutputStream.writeShort(this.constantIndexes[i]);
				} else {
					TypeWriter typeWriter = (TypeWriter)entry;
					TypeWriter remappedWriter = writerMap.get(typeWriter);
					if (remappedWriter != null) {
						dataOutputStream.writeShort(remappedWriter.getTag());
					} else {
						dataOutputStream.writeShort(typeWriter.getTag());
					}
				}
			}
		} else {
			dataOutputStream.write(this.rawData);
		}
	}
}
