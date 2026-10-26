package com.zelix.framework;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.framework.attributes.BootstrapMethodsAttribute;
import com.zelix.framework.attributes.BridgeAttribute;
import com.zelix.framework.attributes.DeprecatedAttribute;
import com.zelix.framework.attributes.SkippedAttribute;
import com.zelix.framework.attributes.SyntheticAttribute;
import com.zelix.framework.attributes.UnknownAttribute;
import com.zelix.framework.attributes.annotations.AnnotationDefaultAttribute;
import com.zelix.framework.attributes.annotations.RuntimeInvisibleAnnotationsAttribute;
import com.zelix.framework.attributes.annotations.RuntimeInvisibleParameterAnnotationsAttribute;
import com.zelix.framework.attributes.annotations.RuntimeInvisibleTypeAnnotationsAttribute;
import com.zelix.framework.attributes.annotations.RuntimeVisibleAnnotationsAttribute;
import com.zelix.framework.attributes.annotations.RuntimeVisibleParameterAnnotationsAttribute;
import com.zelix.framework.attributes.annotations.RuntimeVisibleTypeAnnotationsAttribute;
import com.zelix.framework.attributes.code.CodeAttribute;
import com.zelix.framework.attributes.code.RawCodeAttribute;
import com.zelix.framework.attributes.code.StackMapAttribute;
import com.zelix.framework.attributes.code.StackMapTableAttribute;
import com.zelix.framework.attributes.debug.LineNumberTableAttribute;
import com.zelix.framework.attributes.debug.LocalVariableTableAttribute;
import com.zelix.framework.attributes.debug.LocalVariableTypeTableAttribute;
import com.zelix.framework.attributes.debug.SourceDebugExtensionAttribute;
import com.zelix.framework.attributes.debug.SourceFileAttribute;
import com.zelix.framework.attributes.declaration.ConstantValueAttribute;
import com.zelix.framework.attributes.declaration.EnclosingMethodAttribute;
import com.zelix.framework.attributes.declaration.ExceptionsAttribute;
import com.zelix.framework.attributes.declaration.InnerClassesAttribute;
import com.zelix.framework.attributes.declaration.MethodParametersAttribute;
import com.zelix.framework.attributes.declaration.NestHostAttribute;
import com.zelix.framework.attributes.declaration.NestMembersAttribute;
import com.zelix.framework.attributes.declaration.PermittedSubclassesAttribute;
import com.zelix.framework.attributes.declaration.RecordAttribute;
import com.zelix.framework.attributes.declaration.SignatureAttribute;
import com.zelix.framework.attributes.module.ModuleAttribute;
import com.zelix.framework.attributes.module.ModuleHashesAttribute;
import com.zelix.framework.attributes.module.ModuleMainClassAttribute;
import com.zelix.framework.attributes.module.ModulePackagesAttribute;
import com.zelix.framework.attributes.module.ModuleResolutionAttribute;
import com.zelix.framework.attributes.module.ModuleTargetAttribute;
import com.zelix.framework.attributes.module.ModuleVersionAttribute;
import com.zelix.framework.constant.IntegerNode;
import com.zelix.framework.constant.LongNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.listener.IDynamicCallUpdatable;
import com.zelix.framework.listener.IntegerConstants;
import com.zelix.framework.listener.LongConstants;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.listener.StringConstantUpdateListener;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.variable.LocalVariableTable;
import com.zelix.framework.wrappers.LibraryClassNode;
import com.zelix.framework.writer.AbstractNamedWriter;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.InvokeDynamicCallWriter;
import com.zelix.framework.writer.NameAndTypeWriter;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.ThreeLevelMultiMap;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.dataset.ByteDataBuffer;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public abstract class AttributesNode extends StructureNode implements SimpleReferenceListener {
	public static final ReadOnlyMap<String, Integer> ATTRIBUTE_FLAG_BY_NAME;
	public UTFStringWriter attributeNameUtf8;
	private String attributeName;
	public int length;

	public AttributesNode(StructureNode parent, UTFStringWriter nameUtf8, int length) {
		super(parent);
		this.attributeNameUtf8 = nameUtf8;
		this.attributeName = nameUtf8.getValue();
		this.length = length;
	}

	public AttributesNode(StructureNode parent, int nameIndex, String attributeName, ByteDataBuffer buf, MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References) throws IOException {
		super(parent);
		this.attributeNameUtf8 = (UTFStringWriter)this.getConstantPoolEntry(nameIndex);
		this.attributeName = attributeName;
		this.length = buf.readInt();
		utf8References.put(this.attributeNameUtf8, this);
	}

	public static AttributesNode parseAttribute(
		StructureNode parent,
		ByteDataBuffer buf,
		LocalVariableTable localVariableTable,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		MultiMap<StringNode, StringConstantUpdateListener> stringReferences,
		MultiMap<IntegerNode, IntegerConstants> integerReferences,
		MultiMap<LongNode, LongConstants> longReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences,
		MultiMap<InvokeDynamicCallWriter, IDynamicCallUpdatable> dynamicCallReferences,
		PrintWriter printWriter,
		MultiMap<Integer, IGraphElement> elementsByOffset,
		ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributeRefs
	) throws IOException {
		int attributeNameIndex = buf.readUnsignedShort();
		TypeWriter attributeNameTypeWriter = parent.getConstantPoolEntry(attributeNameIndex);
		if (attributeNameTypeWriter == null) {
			throw new ClassParseFailedException(
				parent.getClassNode().getRawSourcePath() + " : Illegal constant pool index in attribute : " + attributeNameIndex + " : File is probably corrupt (A)"
			);
		} else if (!(attributeNameTypeWriter instanceof UTFStringWriter)) {
			throw new ClassParseFailedException(
				parent.getClassNode().getRawSourcePath() + " : Invalid attribute : " + attributeNameIndex + " : '" + attributeNameTypeWriter.getClass().getName() + "' : File is probably corrupt (B)"
			);
		} else {
			String attributeNameValue = ((UTFStringWriter)attributeNameTypeWriter).getValue();
			switch (attributeNameValue) {
				case "ConstantValue":
					return new ConstantValueAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, stringReferences, integerReferences, longReferences);
				case "Code":
					return new CodeAttribute(
						parent,
						attributeNameIndex,
						attributeNameValue,
						buf,
						utf8References,
						nameAndTypeReferences,
						stringReferences,
						integerReferences,
						longReferences,
						classReferences,
						memberReferences,
						dynamicCallReferences,
						printWriter,
						unknownAttributeRefs,
						elementsByOffset
					);
				case "Exceptions":
					return new ExceptionsAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, classReferences);
				case "SourceFile":
					return new SourceFileAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References);
				case "Signature":
					return new SignatureAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References);
				case "LineNumberTable":
					return new LineNumberTableAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, printWriter, elementsByOffset);
				case "LocalVariableTable":
					return new LocalVariableTableAttribute(parent, attributeNameIndex, attributeNameValue, buf, localVariableTable, utf8References, printWriter, elementsByOffset);
				case "LocalVariableTypeTable":
					return new LocalVariableTypeTableAttribute(parent, attributeNameIndex, attributeNameValue, buf, localVariableTable, utf8References, printWriter, elementsByOffset);
				case "InnerClasses":
					return new InnerClassesAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, classReferences, printWriter);
				case "Synthetic":
					return new SyntheticAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References);
				case "Deprecated":
					return new DeprecatedAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References);
				case "Bridge":
					return new BridgeAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References);
				case "RuntimeVisibleAnnotations":
					return new RuntimeVisibleAnnotationsAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, printWriter);
				case "RuntimeInvisibleAnnotations":
					return new RuntimeInvisibleAnnotationsAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, printWriter);
				case "RuntimeVisibleParameterAnnotations":
					return new RuntimeVisibleParameterAnnotationsAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, printWriter);
				case "RuntimeInvisibleParameterAnnotations":
					return new RuntimeInvisibleParameterAnnotationsAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, printWriter);
				case "RuntimeVisibleTypeAnnotations":
					return new RuntimeVisibleTypeAnnotationsAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, elementsByOffset, printWriter);
				case "RuntimeInvisibleTypeAnnotations":
					return new RuntimeInvisibleTypeAnnotationsAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, elementsByOffset, printWriter);
				case "AnnotationDefault":
					return new AnnotationDefaultAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, printWriter);
				case "EnclosingMethod":
					return new EnclosingMethodAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, nameAndTypeReferences, classReferences, printWriter);
				case "StackMap":
					return new StackMapAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, classReferences, printWriter, elementsByOffset);
				case "StackMapTable":
					return new StackMapTableAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, classReferences, printWriter, elementsByOffset);
				case "BootstrapMethods":
					return new BootstrapMethodsAttribute(
						parent, attributeNameIndex, attributeNameValue, buf, utf8References, stringReferences, integerReferences, longReferences, classReferences, printWriter
					);
				case "SourceDebugExtension":
					return new SourceDebugExtensionAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References);
				case "MethodParameters":
					return new MethodParametersAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, printWriter);
				case "Module":
					return new ModuleAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, classReferences);
				case "ModulePackages":
					return new ModulePackagesAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References);
				case "ModuleVersion":
					return new ModuleVersionAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References);
				case "ModuleMainClass":
					return new ModuleMainClassAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, classReferences);
				case "ModuleTarget":
					return new ModuleTargetAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References);
				case "ModuleHashes":
					return new ModuleHashesAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References);
				case "ModuleResolution":
					return new ModuleResolutionAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References);
				case "NestHost":
					return new NestHostAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, classReferences);
				case "NestMembers":
					return new NestMembersAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, classReferences);
				case "Record":
					return new RecordAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, classReferences, unknownAttributeRefs, printWriter);
				case "PermittedSubclasses":
					return new PermittedSubclassesAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, classReferences, printWriter);
				default:
					return new UnknownAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, unknownAttributeRefs);
			}
		}
	}

	public static AttributesNode readLibraryAttribute(
		StructureNode parent,
		ByteDataBuffer buf,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		PrintWriter printWriter,
		ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributeRefs
	) throws IOException {
		int attributeNameIndex = buf.readUnsignedShort();
		TypeWriter attributeNameTypeWriter = parent.getConstantPoolEntry(attributeNameIndex);
		if (attributeNameTypeWriter == null) {
			throw new ClassParseFailedException(
				parent.getClassNode().getRawSourcePath() + " : Illegal constant pool index in attribute : " + attributeNameIndex + " : File is probably corrupt (C)"
			);
		} else if (!(attributeNameTypeWriter instanceof UTFStringWriter)) {
			throw new ClassParseFailedException(
				parent.getClassNode().getRawSourcePath() + " : Invalid attribute : " + attributeNameIndex + " : '" + attributeNameTypeWriter.getClass().getName() + "' : File is probably corrupt (D)"
			);
		} else {
			String attributeNameValue = ((UTFStringWriter)attributeNameTypeWriter).getValue();
			switch (attributeNameValue) {
				case "InnerClasses":
					return new InnerClassesAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, classReferences, printWriter);
				case "EnclosingMethod":
					return new EnclosingMethodAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, nameAndTypeReferences, classReferences, printWriter);
				case "NestHost":
					return new NestHostAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, classReferences);
				case "NestMembers":
					return new NestMembersAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, classReferences);
				case "Code":
					return new RawCodeAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, nameAndTypeReferences, printWriter, unknownAttributeRefs);
				case "RuntimeVisibleAnnotations":
					if (parent instanceof LibraryClassNode) {
						return new RuntimeVisibleAnnotationsAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, printWriter);
					}

					return new SkippedAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References);
				case "RuntimeInvisibleAnnotations":
					if (parent instanceof LibraryClassNode) {
						return new RuntimeInvisibleAnnotationsAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, printWriter);
					}

					return new SkippedAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References);
				case "Record":
					return new RecordAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References, classReferences, unknownAttributeRefs, printWriter);
				default:
					return new SkippedAttribute(parent, attributeNameIndex, attributeNameValue, buf, utf8References);
			}
		}
	}

	public static AttributesNode readAttribute(
		StructureNode parent,
		ByteDataBuffer buf,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		MultiMap<StringNode, StringConstantUpdateListener> stringReferences,
		MultiMap<IntegerNode, IntegerConstants> integerReferences,
		MultiMap<LongNode, LongConstants> longReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences,
		MultiMap<InvokeDynamicCallWriter, IDynamicCallUpdatable> dynamicCallReferences,
		PrintWriter printWriter,
		MultiMap<Integer, IGraphElement> elementsByOffset,
		ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributeRefs
	) throws IOException {
		return parseAttribute(
			parent,
			buf,
			(LocalVariableTable)null,
			utf8References,
			nameAndTypeReferences,
			stringReferences,
			integerReferences,
			longReferences,
			classReferences,
			memberReferences,
			dynamicCallReferences,
			printWriter,
			elementsByOffset,
			unknownAttributeRefs
		);
	}

	public final int getSerializedSize() {
		return 6 + this.getAttributeLength();
	}

	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeShort(this.attributeNameUtf8.getTag());
		dataOutputStream.writeInt(this.getAttributeLength());
	}

	@Override
	public void updateSimpleNameReference(UTFStringWriter oldReference, UTFStringWriter newReference) {
		if (this.attributeNameUtf8 == oldReference) {
			this.attributeNameUtf8 = newReference;
		}
	}

	public int exchangeLength(int newLength) {
		int previousLength = this.length;
		this.length = newLength;
		return previousLength;
	}

	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		UTFStringWriter remappedNameUtf8 = (UTFStringWriter)writerMap.get(this.attributeNameUtf8);
		if (remappedNameUtf8 != null) {
			dataOutputStream.writeShort(remappedNameUtf8.getTag());
		} else {
			dataOutputStream.writeShort(this.attributeNameUtf8.getTag());
		}

		dataOutputStream.writeInt(this.getAttributeLength());
	}

	public int getAttributeLength() {
		return this.length;
	}

	public void remapDescriptors(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageRenameMap, HashMap<String, String> classNameMap) {
	}

	public UTFStringWriter getNameUtf8() {
		return this.attributeNameUtf8;
	}

	public String getAttributeName() {
		return this.attributeName;
	}

	static {
		HashMap<String, Integer> attributeFlagsByName = GlobalUtils.newHashMap();
		attributeFlagsByName.put("ConstantValue", 1);
		attributeFlagsByName.put("Code", 2);
		attributeFlagsByName.put("Exceptions", 4);
		attributeFlagsByName.put("SourceFile", 8);
		attributeFlagsByName.put("LineNumberTable", 16);
		attributeFlagsByName.put("SourceDebugExtension", 32);
		attributeFlagsByName.put("LocalVariableTable", 64);
		attributeFlagsByName.put("LocalVariableTypeTable", 128);
		attributeFlagsByName.put("InnerClasses", 256);
		attributeFlagsByName.put("Synthetic", 512);
		attributeFlagsByName.put("Deprecated", 1024);
		attributeFlagsByName.put("StackMap", 2048);
		attributeFlagsByName.put("StackMapTable", 4096);
		attributeFlagsByName.put("Signature", 8192);
		attributeFlagsByName.put("Bridge", 16384);
		attributeFlagsByName.put("EnclosingMethod", 32768);
		attributeFlagsByName.put("RuntimeVisibleAnnotations", 65536);
		attributeFlagsByName.put("RuntimeInvisibleAnnotations", 131072);
		attributeFlagsByName.put("RuntimeVisibleParameterAnnotations", 262144);
		attributeFlagsByName.put("RuntimeInvisibleParameterAnnotations", 524288);
		attributeFlagsByName.put("AnnotationDefault", 1048576);
		attributeFlagsByName.put("RuntimeVisibleTypeAnnotations", 2097152);
		attributeFlagsByName.put("RuntimeInvisibleTypeAnnotations", 4194304);
		attributeFlagsByName.put("BootstrapMethods", 8388608);
		attributeFlagsByName.put("MethodParameters", 16777216);
		ATTRIBUTE_FLAG_BY_NAME = new ReadOnlyMap<>(attributeFlagsByName);
	}
}
