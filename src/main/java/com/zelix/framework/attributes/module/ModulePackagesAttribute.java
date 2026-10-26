package com.zelix.framework.attributes.module;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.base.AbstractAttributeNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.PackageNameWriter;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ModulePackagesAttribute extends AbstractAttributeNode implements SimpleReferenceListener {
	private final PackageNameWriter[] packages;


	public ModulePackagesAttribute(StructureNode parent, int nameIndex, String attributeName, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		super.rawData = new byte[this.length];
		buf.read(super.rawData);

		try (ByteDataBuffer dataBuffer = ByteDataBuffer.fromBytes(super.rawData, false)) {
			int packageCount = dataBuffer.readUnsignedShort();
			this.packages = new PackageNameWriter[packageCount];

			for (int packageIndex = 0; packageIndex < packageCount; packageIndex++) {
				int packageNameIndex = dataBuffer.readUnsignedShort();
				TypeWriter packageNameWriter = parent.getConstantPoolEntry(packageNameIndex);
				if (packageNameWriter == null) {
					super.isParsedSuccessfully = false;
					throw new ClassParseFailedException(
						parent.getClassNode().getRawSourcePath() + " : Illegal constant pool index in attribute : " + packageNameIndex + " : File is probably corrupt (AD)"
					);
				}

				if (!(packageNameWriter instanceof PackageNameWriter)) {
					super.isParsedSuccessfully = false;
					throw new ClassParseFailedException(
						parent.getClassNode().getRawSourcePath() + " : Invalid attribute : " + packageNameIndex + " : '" + packageNameWriter.getClass().getName() + "' : File is probably corrupt (AE)"
					);
				}

				this.packages[packageIndex] = (PackageNameWriter)packageNameWriter;
			}
		}

		if (!GlobalUtils.isUnique(this.packages)) {
			StringBuilder errorMessageBuilder = new StringBuilder();
			errorMessageBuilder.append("Invalid 'Module' attribute in class '");
			errorMessageBuilder.append(this.getRawSourcePath());
			errorMessageBuilder.append("' : Duplicate package names : ");

			for (int i = 0; i < this.packages.length; i++) {
				errorMessageBuilder.append(this.packages[i].getValueAsString());
				if (i < this.packages.length - 1) {
					errorMessageBuilder.append(", ");
				}
			}

			DebugUtils.assertTrue(false, new String[]{errorMessageBuilder.toString()});
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		this.attributeNameUtf8.accept(visitor, this, this.getParent());
		if (super.isParsedSuccessfully) {
			PackageNameWriter[] packageWriters = this.packages;
			int packageCount = packageWriters.length;

			for (int i = 0; i < packageCount; i++) {
				packageWriters[i].accept(visitor, this, this.getParent());
			}
		}
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.packages.length);

			for (PackageNameWriter packageWriter : this.packages) {
				dataOutputStream.writeShort(packageWriter.getTag());
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.packages.length);

			for (PackageNameWriter packageWriter : this.packages) {
				dataOutputStream.writeShort(packageWriter.getTag());
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void remapDescriptors(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageRenameMap, HashMap<String, String> classNameMap) {
	}
}
