package com.zelix.framework.modules;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.AccessNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.writer.ModuleNameWriter;
import com.zelix.framework.writer.PackageNameWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class ModuleOpensNode extends StructureNode {
	private PackageNameWriter packageName;
	private final AccessNode accessFlags;
	private final ModuleNameWriter[] toModules;

	public ModuleOpensNode(StructureNode parent, ByteDataBuffer dataBuffer) throws IOException {
		super(parent);
		int packageIndex = dataBuffer.readUnsignedShort();
		TypeWriter constantPoolEntry = parent.getConstantPoolEntry(packageIndex);
		if (constantPoolEntry == null) {
			throw new ClassParseFailedException(
				parent.getClassNode().getRawSourcePath() + " : Illegal constant pool index in attribute : " + packageIndex + " : File is probably corrupt (Z)"
			);
		} else if (!(constantPoolEntry instanceof PackageNameWriter)) {
			throw new ClassParseFailedException(
				parent.getClassNode().getRawSourcePath() + " : Invalid attribute : " + packageIndex + " : '" + constantPoolEntry.getClass().getName() + "' : File is probably corrupt (AA)"
			);
		} else {
			this.packageName = (PackageNameWriter)constantPoolEntry;
			this.accessFlags = new AccessNode(this, dataBuffer);
			int moduleCount = dataBuffer.readUnsignedShort();
			this.toModules = new ModuleNameWriter[moduleCount];

			for (int i = 0; i < moduleCount; i++) {
				int moduleNameIndex = dataBuffer.readUnsignedShort();
				constantPoolEntry = parent.getConstantPoolEntry(moduleNameIndex);
				if (constantPoolEntry == null) {
					throw new ClassParseFailedException(
						parent.getClassNode().getRawSourcePath() + " : Illegal constant pool index in attribute : " + moduleNameIndex + " : File is probably corrupt (AB)"
					);
				}

				if (!(constantPoolEntry instanceof ModuleNameWriter)) {
					throw new ClassParseFailedException(
						parent.getClassNode().getRawSourcePath() + " : Invalid attribute : " + moduleNameIndex + " : '" + constantPoolEntry.getClass().getName() + "' : File is probably corrupt (AC)"
					);
				}

				this.toModules[i] = (ModuleNameWriter)constantPoolEntry;
			}

			if (!GlobalUtils.isUnique(this.toModules)) {
				StringBuilder errorMessageBuilder = new StringBuilder();
				errorMessageBuilder.append("Invalid 'Module' attribute in class '");
				errorMessageBuilder.append(this.getRawSourcePath());
				errorMessageBuilder.append("' : Duplicate 'open to' module names : ");

				for (int moduleIndex = 0; moduleIndex < this.toModules.length; moduleIndex++) {
					errorMessageBuilder.append(this.toModules[moduleIndex].getValueAsString());
					if (moduleIndex < this.toModules.length - 1) {
						errorMessageBuilder.append(", ");
					}
				}

				DebugUtils.assertTrue(false, new String[]{errorMessageBuilder.toString()});
			}
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		this.packageName.accept(visitor, this, this.getParent());
		ModuleNameWriter[] moduleWriters = this.toModules;
		int moduleCount = moduleWriters.length;

		for (int i = 0; i < moduleCount; i++) {
			moduleWriters[i].accept(visitor, this, this.getParent());
		}
	}

	public void writeNodeRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> remappedConstants) throws IOException {
		this.writeNode(dataOutputStream);
	}

	public void writeNode(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeShort(this.packageName.getTag());
		dataOutputStream.writeShort(this.accessFlags.getAccessFlags());
		dataOutputStream.writeShort(this.toModules.length);

		for (ModuleNameWriter moduleNameWriter : this.toModules) {
			dataOutputStream.writeShort(moduleNameWriter.getTag());
		}
	}
}
