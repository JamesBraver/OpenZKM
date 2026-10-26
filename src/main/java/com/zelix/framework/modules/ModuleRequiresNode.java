package com.zelix.framework.modules;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.AccessNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.writer.ModuleNameWriter;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class ModuleRequiresNode extends StructureNode implements SimpleReferenceListener {
	private ModuleNameWriter moduleName;
	private final AccessNode accessFlags;
	private UTFStringWriter moduleVersion;

	public ModuleRequiresNode(StructureNode parent, ByteDataBuffer dataBuffer, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent);
		int moduleNameIndex = dataBuffer.readUnsignedShort();
		TypeWriter moduleNameEntry = parent.getConstantPoolEntry(moduleNameIndex);
		if (moduleNameEntry == null) {
			throw new ClassParseFailedException(
				parent.getClassNode().getRawSourcePath() + " : Illegal constant pool index in attribute : " + moduleNameIndex + " : File is probably corrupt (AJ)"
			);
		} else if (!(moduleNameEntry instanceof ModuleNameWriter)) {
			throw new ClassParseFailedException(
				parent.getClassNode().getRawSourcePath() + " : Invalid attribute : " + moduleNameIndex + " : '" + moduleNameEntry.getClass().getName() + "' : File is probably corrupt (AK)"
			);
		} else {
			this.moduleName = (ModuleNameWriter)moduleNameEntry;
			this.accessFlags = new AccessNode(this, dataBuffer);
			int moduleVersionIndex = dataBuffer.readUnsignedShort();
			if (moduleVersionIndex != 0) {
				TypeWriter moduleVersionEntry = parent.getConstantPoolEntry(moduleVersionIndex);
				if (moduleVersionEntry == null) {
					throw new ClassParseFailedException(
						parent.getClassNode().getRawSourcePath() + " : Illegal constant pool index in attribute : " + moduleVersionIndex + " : File is probably corrupt (AL)"
					);
				}

				if (!(moduleVersionEntry instanceof UTFStringWriter)) {
					throw new ClassParseFailedException(
						parent.getClassNode().getRawSourcePath() + " : Invalid attribute : " + moduleVersionIndex + " : '" + moduleVersionEntry.getClass().getName() + "' : File is probably corrupt (AM)"
					);
				}

				this.moduleVersion = (UTFStringWriter)moduleVersionEntry;
				utf8References.put(this.moduleVersion, this);
			}
		}
	}

	public void writeNode(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeShort(this.moduleName.getTag());
		dataOutputStream.writeShort(this.accessFlags.getAccessFlags());
		dataOutputStream.writeShort(this.moduleVersion == null ? 0 : this.moduleVersion.getTag());
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.moduleName, this, this.getParent());
		if (this.moduleVersion != null) {
			visitor.visit(this.moduleVersion, this, this.getParent());
		}
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.moduleVersion != null && this.moduleVersion == oldReference) {
			this.moduleVersion = newReference;
		}
	}

	public void writeNodeRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> remappedConstants) throws IOException {
		dataOutputStream.writeShort(this.moduleName.getTag());
		dataOutputStream.writeShort(this.accessFlags.getAccessFlags());
		if (this.moduleVersion != null) {
			TypeWriter remappedModuleVersion = remappedConstants.get(this.moduleVersion);
			if (remappedModuleVersion != null) {
				dataOutputStream.writeShort(remappedModuleVersion.getTag());
			} else {
				dataOutputStream.writeShort(this.moduleVersion.getTag());
			}
		} else {
			dataOutputStream.writeShort(0);
		}
	}
}
