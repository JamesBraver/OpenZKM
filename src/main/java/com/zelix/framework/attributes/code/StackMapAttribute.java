package com.zelix.framework.attributes.code;

import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.base.AbstractStackMapAttribute;
import com.zelix.framework.attributes.stackmap.StackMapEntry;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class StackMapAttribute extends AbstractStackMapAttribute {
	public StackMapAttribute(StructureNode parent, UTFStringWriter nameUtf8, int length) {
		super(parent, nameUtf8, length);
	}

	public StackMapAttribute(
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
		if (this.length >= 2) {
			this.entryCount = buf.readUnsignedShort();
			this.entries = new StackMapEntry[this.entryCount];

			for (int i = 0; i < this.entryCount; i++) {
				this.entries[i] = new StackMapEntry(this, buf, elementsByOffset, classReferences, printWriter);
				if (!this.entries[i].isValid()) {
					super.isParsedSuccessfully = false;
				}
			}

			if (!super.isParsedSuccessfully) {
				ByteArrayOutputStream byteStream = new ByteArrayOutputStream(this.length);
				DataOutputStream dataStream = new DataOutputStream(byteStream);
				this.write(dataStream);
				super.rawData = byteStream.toByteArray();
				this.entries = null;
			}
		} else {
			super.isParsedSuccessfully = false;
			printWriter.println("ERROR: " + this.getRawSourcePath() + " : Invalid StackMap attribute (A)");
			super.rawData = new byte[this.length];
			buf.read(super.rawData);
		}
	}

	@Override
	protected void write(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeShort(this.entryCount);

		for (int i = 0; i < this.entryCount; i++) {
			((StackMapEntry)this.entries[i]).writeToStream(dataOutputStream);
		}
	}

	@Override
	protected void writeMapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap) throws IOException {
		dataOutputStream.writeShort(this.entryCount);

		for (int i = 0; i < this.entryCount; i++) {
			((StackMapEntry)this.entries[i]).writeToStreamWithMapping(dataOutputStream, writerMap);
		}
	}
}
