package com.zelix.framework.attributes.code;

import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.base.AbstractStackMapAttribute;
import com.zelix.framework.attributes.stackmap.AbstractStackMapFrame;
import com.zelix.framework.attributes.stackmap.VerificationTypeInfo;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class StackMapTableAttribute extends AbstractStackMapAttribute {
	public StackMapTableAttribute(StructureNode parent, UTFStringWriter nameUtf8, int length) {
		super(parent, nameUtf8, length);
	}

	public StackMapTableAttribute(
		StructureNode parent,
		int nameIndex,
		String attributeName,
		ByteDataBuffer buf,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		PrintWriter printWriter,
		MultiMap<Integer, IGraphElement> elementsByOffset
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		byte[] attributeBytes = new byte[this.length];
		buf.read(attributeBytes);
		ByteDataBuffer frameBuffer = ByteDataBuffer.fromBytes(attributeBytes, false);
		if (this.length >= 2) {
			this.entryCount = frameBuffer.readUnsignedShort();
			HashMap<Integer, VerificationTypeInfo> primitiveTypes = GlobalUtils.newHashMap();
			HashMap<Integer, VerificationTypeInfo> objectTypes = GlobalUtils.newHashMap();
			AtomicInt previousOffset = new AtomicInt(-1);
			this.entries = new AbstractStackMapFrame[this.entryCount];

			for (int i = 0; i < this.entryCount; i++) {
				this.entries[i] = AbstractStackMapFrame.parseFrame(this, frameBuffer, elementsByOffset, classReferences, printWriter, previousOffset, primitiveTypes, objectTypes);
				if (!this.entries[i].isValid()) {
					super.isParsedSuccessfully = false;
				}
			}

			if (!super.isParsedSuccessfully) {
				super.rawData = attributeBytes;
			}
		} else {
			super.isParsedSuccessfully = false;
			printWriter.println("ERROR: " + this.getRawSourcePath() + " : Invalid StackMapTable attribute (B)");
			super.rawData = new byte[this.length];
			frameBuffer.read(super.rawData);
		}
	}

	@Override
	protected void write(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeShort(this.entryCount);
		AtomicInt previousOffset = new AtomicInt(-1);

		for (int i = 0; i < this.entryCount; i++) {
			((AbstractStackMapFrame)this.entries[i]).write(dataOutputStream, previousOffset);
		}
	}

	@Override
	protected void writeMapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap) throws IOException {
		dataOutputStream.writeShort(this.entryCount);
		AtomicInt previousOffset = new AtomicInt(-1);

		for (int i = 0; i < this.entryCount; i++) {
			((AbstractStackMapFrame)this.entries[i]).writeMapped(dataOutputStream, previousOffset, writerMap);
		}
	}

	public String getParentName() {
		return ((CodeAttribute)this.getParent()).getAsString();
	}
}
