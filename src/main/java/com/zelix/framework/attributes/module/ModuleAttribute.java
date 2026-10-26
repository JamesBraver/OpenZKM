package com.zelix.framework.attributes.module;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.AccessNode;
import com.zelix.framework.StructureNode;
import com.zelix.framework.attributes.base.AbstractAttributeNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.modules.ModuleExportsNode;
import com.zelix.framework.modules.ModuleOpensNode;
import com.zelix.framework.modules.ModuleProvidesNode;
import com.zelix.framework.modules.ModuleRequiresNode;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.ModuleNameWriter;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ModuleAttribute extends AbstractAttributeNode implements SimpleReferenceListener, ReferenceUpdateListener {
	private ModuleNameWriter moduleName;
	private final AccessNode moduleFlags;
	private UTFStringWriter moduleVersion;
	private final ModuleRequiresNode[] requiresNodes;
	private final ModuleExportsNode[] exportsNodes;
	private final ModuleOpensNode[] opensNodes;
	private final ClassInfoWriter[] usedServiceInterfaces;
	private final ModuleProvidesNode[] providesNodes;


	public ModuleAttribute(
		StructureNode parent,
		int nameIndex,
		String attributeName,
		ByteDataBuffer buf,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		super.rawData = new byte[this.length];
		buf.read(super.rawData);

		try (ByteDataBuffer bodyBuffer = ByteDataBuffer.fromBytes(super.rawData, false)) {
			int moduleNameIndex = bodyBuffer.readUnsignedShort();
			TypeWriter moduleNameEntry = parent.getConstantPoolEntry(moduleNameIndex);
			if (moduleNameEntry == null) {
				super.isParsedSuccessfully = false;
				throw new ClassParseFailedException(
					parent.getClassNode().getRawSourcePath() + " : Illegal constant pool index in attribute : " + moduleNameIndex + " : File is probably corrupt (J)"
				);
			}

			if (!(moduleNameEntry instanceof ModuleNameWriter)) {
				super.isParsedSuccessfully = false;
				throw new ClassParseFailedException(
					parent.getClassNode().getRawSourcePath() + " : Invalid attribute : " + moduleNameIndex + " : '" + moduleNameEntry.getClass().getName() + "' : File is probably corrupt (K)"
				);
			}

			this.moduleName = (ModuleNameWriter)moduleNameEntry;
			this.moduleFlags = new AccessNode(this, bodyBuffer);
			int moduleVersionIndex = bodyBuffer.readUnsignedShort();
			if (moduleVersionIndex != 0) {
				TypeWriter moduleVersionEntry = parent.getConstantPoolEntry(moduleVersionIndex);
				if (moduleVersionEntry == null) {
					super.isParsedSuccessfully = false;
					throw new ClassParseFailedException(
						parent.getClassNode().getRawSourcePath() + " : Illegal constant pool index in attribute : " + moduleVersionIndex + " : File is probably corrupt (L)"
					);
				}

				if (!(moduleVersionEntry instanceof UTFStringWriter)) {
					super.isParsedSuccessfully = false;
					throw new ClassParseFailedException(
						parent.getClassNode().getRawSourcePath() + " : Invalid attribute : " + moduleVersionIndex + " : '" + moduleVersionEntry.getClass().getName() + "' : File is probably corrupt (M)"
					);
				}

				this.moduleVersion = (UTFStringWriter)moduleVersionEntry;
				utf8References.put(this.moduleVersion, this);
			}

			int requiresCount = bodyBuffer.readUnsignedShort();
			this.requiresNodes = new ModuleRequiresNode[requiresCount];

			for (int requiresIndex = 0; requiresIndex < requiresCount; requiresIndex++) {
				try {
					this.requiresNodes[requiresIndex] = new ModuleRequiresNode(this, bodyBuffer, utf8References);
				} catch (ClassParseFailedException requiresParseFailure) {
					super.isParsedSuccessfully = false;
					throw requiresParseFailure;
				}
			}

			int exportsCount = bodyBuffer.readUnsignedShort();
			this.exportsNodes = new ModuleExportsNode[exportsCount];

			for (int exportsIndex = 0; exportsIndex < exportsCount; exportsIndex++) {
				try {
					this.exportsNodes[exportsIndex] = new ModuleExportsNode(this, bodyBuffer);
				} catch (ClassParseFailedException exportsParseFailure) {
					super.isParsedSuccessfully = false;
					throw exportsParseFailure;
				}
			}

			int opensCount = bodyBuffer.readUnsignedShort();
			this.opensNodes = new ModuleOpensNode[opensCount];

			for (int opensIndex = 0; opensIndex < opensCount; opensIndex++) {
				try {
					this.opensNodes[opensIndex] = new ModuleOpensNode(this, bodyBuffer);
				} catch (ClassParseFailedException opensParseFailure) {
					super.isParsedSuccessfully = false;
					throw opensParseFailure;
				}
			}

			int serviceInterfaceCount = bodyBuffer.readUnsignedShort();
			this.usedServiceInterfaces = new ClassInfoWriter[serviceInterfaceCount];

			for (int serviceInterfaceIndex = 0; serviceInterfaceIndex < serviceInterfaceCount; serviceInterfaceIndex++) {
				int serviceInterfaceRefIndex = bodyBuffer.readUnsignedShort();
				TypeWriter serviceInterfaceEntry = parent.getConstantPoolEntry(serviceInterfaceRefIndex);
				if (serviceInterfaceEntry == null) {
					super.isParsedSuccessfully = false;
					throw new ClassParseFailedException(
						parent.getClassNode().getRawSourcePath() + " : Illegal constant pool index in attribute : " + serviceInterfaceRefIndex + " : File is probably corrupt (N)"
					);
				}

				if (!(serviceInterfaceEntry instanceof ClassInfoWriter)) {
					super.isParsedSuccessfully = false;
					throw new ClassParseFailedException(
						parent.getClassNode().getRawSourcePath() + " : Invalid attribute : " + serviceInterfaceRefIndex + " : '" + serviceInterfaceEntry.getClass().getName() + "' : File is probably corrupt (O)"
					);
				}

				this.usedServiceInterfaces[serviceInterfaceIndex] = (ClassInfoWriter)serviceInterfaceEntry;
				classReferences.put(this.usedServiceInterfaces[serviceInterfaceIndex], this);
			}

			int providesCount = bodyBuffer.readUnsignedShort();
			this.providesNodes = new ModuleProvidesNode[providesCount];

			for (int providesIndex = 0; providesIndex < providesCount; providesIndex++) {
				try {
					this.providesNodes[providesIndex] = new ModuleProvidesNode(this, bodyBuffer, classReferences);
				} catch (ClassParseFailedException providesParseFailure) {
					super.isParsedSuccessfully = false;
					throw providesParseFailure;
				}
			}
		}
	}

	public String getModuleName() {
		return this.moduleName.getModuleName();
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		this.attributeNameUtf8.accept(visitor, this, this.getParent());
		this.moduleName.accept(visitor, this, this.getParent());
		if (this.moduleVersion != null) {
			this.moduleVersion.accept(visitor, this, this.getParent());
		}

		if (super.isParsedSuccessfully) {
			ModuleRequiresNode[] requiresNodeArray = this.requiresNodes;
			int elementCount = requiresNodeArray.length;

			for (int requiresIndex = 0; requiresIndex < elementCount; requiresIndex++) {
				requiresNodeArray[requiresIndex].acceptVisitor(visitor);
			}

			ModuleExportsNode[] exportsNodeArray = this.exportsNodes;
			elementCount = exportsNodeArray.length;

			for (int exportsIndex = 0; exportsIndex < elementCount; exportsIndex++) {
				exportsNodeArray[exportsIndex].acceptVisitor(visitor);
			}

			ModuleOpensNode[] opensNodeArray = this.opensNodes;
			elementCount = opensNodeArray.length;

			for (int opensIndex = 0; opensIndex < elementCount; opensIndex++) {
				opensNodeArray[opensIndex].acceptVisitor(visitor);
			}

			ClassInfoWriter[] serviceInterfaceArray = this.usedServiceInterfaces;
			elementCount = serviceInterfaceArray.length;

			for (int serviceInterfaceIndex = 0; serviceInterfaceIndex < elementCount; serviceInterfaceIndex++) {
				serviceInterfaceArray[serviceInterfaceIndex].accept(visitor, this, this.getParent());
			}

			ModuleProvidesNode[] providesNodeArray = this.providesNodes;
			elementCount = providesNodeArray.length;

			for (int providesIndex = 0; providesIndex < elementCount; providesIndex++) {
				providesNodeArray[providesIndex].acceptVisitor(visitor);
			}
		}
	}

	@Override
	public void updateClassReference(ClassInfoWriter oldReference, ClassInfoWriter newReference) {
		int serviceInterfaceCount = this.usedServiceInterfaces.length;

		for (int i = 0; i < serviceInterfaceCount; i++) {
			if (this.usedServiceInterfaces[i] == oldReference) {
				this.usedServiceInterfaces[i] = newReference;
			}
		}
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.moduleName.getTag());
			dataOutputStream.writeShort(this.moduleFlags.getAccessFlags());
			dataOutputStream.writeShort(this.moduleVersion == null ? 0 : this.moduleVersion.getTag());
			dataOutputStream.writeShort(this.requiresNodes.length);
			ModuleRequiresNode[] requiresNodeArray = this.requiresNodes;
			int elementCount = requiresNodeArray.length;

			for (int requiresIndex = 0; requiresIndex < elementCount; requiresIndex++) {
				requiresNodeArray[requiresIndex].writeNode(dataOutputStream);
			}

			dataOutputStream.writeShort(this.exportsNodes.length);
			ModuleExportsNode[] exportsNodeArray = this.exportsNodes;
			elementCount = exportsNodeArray.length;

			for (int exportsIndex = 0; exportsIndex < elementCount; exportsIndex++) {
				exportsNodeArray[exportsIndex].writeNode(dataOutputStream);
			}

			dataOutputStream.writeShort(this.opensNodes.length);
			ModuleOpensNode[] opensNodeArray = this.opensNodes;
			elementCount = opensNodeArray.length;

			for (int opensIndex = 0; opensIndex < elementCount; opensIndex++) {
				opensNodeArray[opensIndex].writeNode(dataOutputStream);
			}

			dataOutputStream.writeShort(this.usedServiceInterfaces.length);

			for (ClassInfoWriter serviceInterface : this.usedServiceInterfaces) {
				dataOutputStream.writeShort(serviceInterface.getTag());
			}

			dataOutputStream.writeShort(this.providesNodes.length);
			ModuleProvidesNode[] providesNodeArray = this.providesNodes;
			elementCount = providesNodeArray.length;

			for (int providesIndex = 0; providesIndex < elementCount; providesIndex++) {
				providesNodeArray[providesIndex].writeNode(dataOutputStream);
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.moduleVersion != null && this.moduleVersion == oldReference) {
			this.moduleVersion = newReference;
		} else {
			super.updateSimpleNameReference(oldReference, newReference);
		}
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		if (super.isParsedSuccessfully) {
			dataOutputStream.writeShort(this.moduleName.getTag());
			dataOutputStream.writeShort(this.moduleFlags.getAccessFlags());
			if (this.moduleVersion != null) {
				TypeWriter remappedModuleVersion = writerMap.get(this.moduleVersion);
				if (remappedModuleVersion != null) {
					dataOutputStream.writeShort(remappedModuleVersion.getTag());
				} else {
					dataOutputStream.writeShort(this.moduleVersion.getTag());
				}
			} else {
				dataOutputStream.writeShort(0);
			}

			dataOutputStream.writeShort(this.requiresNodes.length);
			ModuleRequiresNode[] requiresNodeArray = this.requiresNodes;
			int elementCount = requiresNodeArray.length;

			for (int requiresIndex = 0; requiresIndex < elementCount; requiresIndex++) {
				requiresNodeArray[requiresIndex].writeNodeRemapped(dataOutputStream, writerMap);
			}

			dataOutputStream.writeShort(this.exportsNodes.length);
			ModuleExportsNode[] exportsNodeArray = this.exportsNodes;
			elementCount = exportsNodeArray.length;

			for (int exportsIndex = 0; exportsIndex < elementCount; exportsIndex++) {
				exportsNodeArray[exportsIndex].writeNodeRemapped(dataOutputStream, writerMap);
			}

			dataOutputStream.writeShort(this.opensNodes.length);
			ModuleOpensNode[] opensNodeArray = this.opensNodes;
			elementCount = opensNodeArray.length;

			for (int opensIndex = 0; opensIndex < elementCount; opensIndex++) {
				opensNodeArray[opensIndex].writeNodeRemapped(dataOutputStream, writerMap);
			}

			dataOutputStream.writeShort(this.usedServiceInterfaces.length);

			for (ClassInfoWriter serviceInterface : this.usedServiceInterfaces) {
				ClassInfoWriter remappedServiceInterface = (ClassInfoWriter)writerMap.get(serviceInterface);
				if (remappedServiceInterface != null) {
					dataOutputStream.writeShort(remappedServiceInterface.getTag());
				} else {
					dataOutputStream.writeShort(serviceInterface.getTag());
				}
			}

			dataOutputStream.writeShort(this.providesNodes.length);
			ModuleProvidesNode[] providesNodeArray = this.providesNodes;
			elementCount = providesNodeArray.length;

			for (int providesIndex = 0; providesIndex < elementCount; providesIndex++) {
				providesNodeArray[providesIndex].writeNodeRemapped(dataOutputStream, writerMap);
			}
		} else {
			dataOutputStream.write(super.rawData);
		}
	}

	@Override
	public void remapDescriptors(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageRenameMap, HashMap<String, String> classNameMap) {
	}
}
