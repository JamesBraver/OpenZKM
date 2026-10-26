package com.zelix.framework.attributes.declaration;

import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.base.AbstractAttributeNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.tools.parameterChanger.ParameterPattern;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public class MethodParametersAttribute extends AbstractAttributeNode {
	private MethodParameterEntry[] parameters;

	public MethodParametersAttribute(
		StructureNode parent, int nameIndex, String attributeName, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References, PrintWriter printWriter
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		byte[] attributeBytes = new byte[this.length];
		buf.read(attributeBytes);
		ByteDataBuffer bodyBuffer = ByteDataBuffer.fromBytes(attributeBytes, false);

		try {
			if (this.length >= 2) {
				int parameterCount = bodyBuffer.readUnsignedByte();
				this.parameters = new MethodParameterEntry[parameterCount];

				for (int i = 0; i < parameterCount; i++) {
					this.parameters[i] = new MethodParameterEntry(this, bodyBuffer, utf8References);
					if (!this.parameters[i].isValid()) {
						super.isParsedSuccessfully = false;
						super.rawData = attributeBytes;
						printWriter.println("ERROR: " + this.getSourcePath() + " : " + this.getAttributeName() + " (A) " + this.parameters[i].getErrorMessage());
					}
				}
			} else {
				super.isParsedSuccessfully = false;
				super.rawData = attributeBytes;
				printWriter.println("ERROR: " + this.getSourcePath() + " : " + this.getAttributeName() + " : length=" + this.length);
			}
		} catch (IOException ignored) {
			super.isParsedSuccessfully = false;
			super.rawData = attributeBytes;
			printWriter.println("ERROR: " + this.getSourcePath() + " : " + this.getAttributeName() + " is possibly corrupt : " + ignored);
		} finally {
			bodyBuffer.close();
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this.getParent());
		if (super.isParsedSuccessfully) {
			int parameterCount = this.parameters.length;

			for (int i = 0; i < parameterCount; i++) {
				this.parameters[i].acceptVisitor(visitor);
			}
		}
	}

	public void insertParameters(ParameterPattern parameterPattern, boolean isStatic, List<String> parameterDescriptors, AbstractConstantPoolWriter constantPool, List<TypeWriter> typeWriters) {
		int parameterCount = parameterPattern.getParameterCount();
		parameterPattern.getExtraParameterCount();
		MethodParameterEntry[] parameterEntries = new MethodParameterEntry[parameterCount];
		int[] extraParameterIndexes = parameterPattern.getExtraParameterIndexes();
		int extraIndexCursor = 0;
		int nextExtraParameterIndex = extraParameterIndexes[0];
		int existingParameterIndex = 0;

		for (int parameterIndex = 0; parameterIndex < parameterCount; parameterIndex++) {
			if (parameterIndex == nextExtraParameterIndex) {
				parameterEntries[parameterIndex] = new MethodParameterEntry(this, constantPool, typeWriters);
				if (++extraIndexCursor < extraParameterIndexes.length) {
					nextExtraParameterIndex = extraParameterIndexes[extraIndexCursor];
				} else {
					nextExtraParameterIndex = 32767;
				}
			} else {
				parameterEntries[parameterIndex] = this.parameters[existingParameterIndex++];
			}
		}

		this.parameters = parameterEntries;
		this.getAttributeLength();
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			int parameterCount = this.parameters.length;
			dataOutputStream.writeByte(parameterCount);

			for (int i = 0; i < parameterCount; i++) {
				this.parameters[i].writeToStream(dataOutputStream);
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	public void mangleParameterNames() {
		int parameterCount = this.parameters.length;

		for (int i = 0; i < parameterCount; i++) {
			this.parameters[i].mangleName(i);
		}
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		if (super.isParsedSuccessfully) {
			int parameterCount = this.parameters.length;
			dataOutputStream.writeByte(parameterCount);

			for (int i = 0; i < parameterCount; i++) {
				this.parameters[i].writeToStreamRemapped(dataOutputStream, writerMap, scriptContext);
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public int getAttributeLength() {
		if (!super.isParsedSuccessfully) {
			return super.rawData.length;
		} else {
			int attributeLength = 1;
			int parameterCount = this.parameters.length;

			for (int i = 0; i < parameterCount; i++) {
				attributeLength += this.parameters[i].getEntrySizeInBytes();
			}

			this.length = attributeLength;
			return this.length;
		}
	}
}
