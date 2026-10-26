package com.zelix.framework.constant;

import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.logging.AbstractLogger;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RemappedConstantPoolWriter extends AbstractConstantPoolWriter {
	private final Map<TypeWriter, TypeWriter> mapping;

	public RemappedConstantPoolWriter(AbstractConstantPoolWriter sourceWriter, TypeWriter[] constantPoolEntries, Map<TypeWriter, TypeWriter> mapping) {
		super(sourceWriter.ownerClassNode);
		this.constantPoolEntries = constantPoolEntries;
		this.mapping = mapping;
		this.updateConstantPoolIndices();
	}

	@Override
	public void remapTargetClassName(String newClassName) {
	}

	@Override
	public void remapConstantNames(HashMap<String, String> classNameMappings, AbstractLogger logger) {
	}

	@Override
	public boolean shouldObfuscateConstants() {
		return true;
	}

	public void writeConstantPool(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeShort(this.constantPoolEntries.length);

		for (int i = 1; i < this.constantPoolEntries.length; i++) {
			if (this.constantPoolEntries[i] != null) {
				this.constantPoolEntries[i].writeToStream(dataOutputStream, this.mapping);
			}
		}
	}

	public Map<TypeWriter, TypeWriter> getEntryMapping() {
		return this.mapping;
	}

	@Override
	public void remapPackageNames(HashMap<String, String> packageMappings) {
	}
}
