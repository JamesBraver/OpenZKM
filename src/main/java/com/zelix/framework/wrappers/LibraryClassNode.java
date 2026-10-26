package com.zelix.framework.wrappers;

import com.zelix.exceptions.ClassParseFailedException;
import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.AccessNode;
import com.zelix.framework.AttributesNode;
import com.zelix.framework.TypeNode;
import com.zelix.framework.annotation.AnnotationContainer;
import com.zelix.framework.attributes.BootstrapMethodsAttribute;
import com.zelix.framework.attributes.UnknownAttribute;
import com.zelix.framework.attributes.code.CodeAttribute;
import com.zelix.framework.attributes.code.TryCatchBlockInfo;
import com.zelix.framework.attributes.declaration.ExceptionsAttribute;
import com.zelix.framework.attributes.declaration.RecordAttribute;
import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.code.MethodMapping;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.constant.IntegerNode;
import com.zelix.framework.constant.LongNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.hierarchy.Inheritance;
import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.listener.IDynamicCallUpdatable;
import com.zelix.framework.listener.IntegerConstants;
import com.zelix.framework.listener.LongConstants;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.listener.SimpleReferenceListener;
import com.zelix.framework.listener.StringConstantUpdateListener;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.signature.MethodParameterKey;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.framework.variable.LocalVariableTable;
import com.zelix.framework.writer.AbstractNamedWriter;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.InvokeDynamicCallWriter;
import com.zelix.framework.writer.NameAndTypeWriter;
import com.zelix.framework.writer.SimpleConstantPoolWriter;
import com.zelix.framework.writer.UTFStringWriter;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.logging.AbstractLogger;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.MethodNameMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.ThreeLevelMultiMap;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.PhantomClassManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LibraryClassNode extends AbstractClassNode {
	public SimpleConstantPoolWriter constantPoolWriter;
	public LibraryFieldNode[] fieldNodes;
	public LibraryMethodNode[] methodNodes;

	public LibraryClassNode(ByteDataBuffer dataBuffer, ClasspathResource classpathResource) {
		super(classpathResource, dataBuffer.getSha1Hash(), 0);

		try {
			this.readClassHeader(dataBuffer);
			MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References = new MultiMap<>();
			MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences = new MultiMap<>();
			MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences = new MultiMap<>();
			MultiMap<StringNode, StringConstantUpdateListener> stringReferences = new MultiMap<>();
			MultiMap<IntegerNode, IntegerConstants> intReferences = new MultiMap<>();
			MultiMap<LongNode, LongConstants> longReferences = new MultiMap<>();
			MultiMap<MemberTypeWriter, TypeNode> memberReferences = new MultiMap<>();
			MultiMap<InvokeDynamicCallWriter, IDynamicCallUpdatable> dynamicCallReferences = new MultiMap<>();
			this.constantPoolWriter = new SimpleConstantPoolWriter(dataBuffer, this, utf8References, nameAndTypeReferences, classReferences, memberReferences);
			super.constantPoolSize = this.constantPoolWriter.getConstantPoolSize();
			this.access = new AccessNode(this, dataBuffer);
			int thisClassIndex = dataBuffer.readUnsignedShort();
			int superClassIndex = dataBuffer.readUnsignedShort();
			super.interfacesCount = dataBuffer.readUnsignedShort();
			int[] interfaceIndexes = new int[super.interfacesCount];

			for (int interfaceIndex = 0; interfaceIndex < super.interfacesCount; interfaceIndex++) {
				interfaceIndexes[interfaceIndex] = dataBuffer.readUnsignedShort();
			}

			this.resolveClassHierarchyTypes(thisClassIndex, superClassIndex, interfaceIndexes, null);
			this.fieldsCount = dataBuffer.readUnsignedShort();
			this.fieldNodes = new LibraryFieldNode[this.fieldsCount];

			for (int fieldIndex = 0; fieldIndex < this.fieldsCount; fieldIndex++) {
				this.fieldNodes[fieldIndex] = new LibraryFieldNode(this, dataBuffer, utf8References);
			}

			PrintWriter printWriter = new PrintWriter(new ByteArrayOutputStream());
			ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributes = new ThreeLevelMultiMap<>();
			this.methodCount = dataBuffer.readUnsignedShort();
			this.methodNodes = new LibraryMethodNode[this.methodCount];

			for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
				this.methodNodes[methodIndex] = new LibraryMethodNode(this, dataBuffer, utf8References, nameAndTypeReferences, classReferences, printWriter, unknownAttributes);
			}

			this.attributesCount = dataBuffer.readUnsignedShort();
			this.attributes = new AttributesNode[this.attributesCount];

			for (int attributeIndex = 0; attributeIndex < this.attributesCount; attributeIndex++) {
				this.attributes[attributeIndex] = AttributesNode.readLibraryAttribute(this, dataBuffer, utf8References, nameAndTypeReferences, classReferences, printWriter, unknownAttributes);
			}

			this.initializeInnerClassNode();
			this.constantPoolWriter.duplicateAndRemapConstants(utf8References, nameAndTypeReferences, stringReferences, intReferences, longReferences, classReferences, memberReferences, dynamicCallReferences);
		} catch (IOException ioException) {
			throw new ProcessingException(classpathResource.getFullPath() + " : " + ioException + " : File is probably corrupt (F)");
		} catch (ClassParseFailedException classParseException) {
			throw new ProcessingException(classpathResource.getFullPath() + " : \"" + classParseException.getMessage() + "\" : File is probably corrupt (G)");
		} finally {
			if (dataBuffer != null) {
				try {
					dataBuffer.close();
				} catch (IOException closeException) {
					throw new ProcessingException("IOException on closing : " + classpathResource.getFullPath() + " : " + closeException);
				}
			}
		}
	}

	@Override
	public AbstractMethodNode[] getMethods() {
		return this.getMethodNodes();
	}

	@Override
	public void refreshFieldRefsAndConstants(ScriptContext scriptContext, AbstractWorkspace workspace) {
		this.constantPoolWriter.resolveFieldReferences(scriptContext, workspace);

		for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
			if (this.attributes[attributeIndex] instanceof AnnotationContainer) {
				((AnnotationContainer)this.attributes[attributeIndex]).updateConstants();
			} else if (this.attributes[attributeIndex] instanceof RecordAttribute) {
				((RecordAttribute)this.attributes[attributeIndex]).updateConstants();
			} else if (this.attributes[attributeIndex] instanceof BootstrapMethodsAttribute) {
				((BootstrapMethodsAttribute)this.attributes[attributeIndex]).processRecordObjectMethods();
			}
		}

		for (int fieldIndex = 0; fieldIndex < this.fieldsCount; fieldIndex++) {
			this.fieldNodes[fieldIndex].pruneAnnotations();
		}

		for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
			this.methodNodes[methodIndex].pruneAnnotations();
		}
	}

	public void collectMethodLookupMaps(
		Map<MethodSignature, AbstractMethodNode> processedMethodsBySignature,
		MultiMap<String, String> libraryNamesByDescriptor,
		Map<MethodParameterKey, AbstractMethodNode> processedMethodsByParamKey,
		MethodNameMap<String, AbstractMethodNode> interfaceMethodNames,
		MethodNameMap<String, AbstractMethodNode> classMethodNames
	) {
		for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
			LibraryMethodNode methodNode = this.methodNodes[methodIndex];
			processedMethodsBySignature.put(methodNode.getMethodSignature(), methodNode);
			libraryNamesByDescriptor.put(methodNode.getDescriptor(), methodNode.getNameDisplay());
			processedMethodsByParamKey.put(methodNode.getMethodParameterKey(), methodNode);
			if (this.isInterface()) {
				interfaceMethodNames.put(methodNode.getNameDisplay(), methodNode);
			}

			classMethodNames.put(methodNode.getNameDisplay(), methodNode);
		}
	}

	public void applyMappedMethodNames(DualKeyMap<String, MethodSignature, MethodSignature> methodMappings, MethodMapping methodMapping) {
		Map<MethodSignature, MethodSignature> mappedSignatures = methodMappings.getGroup(this.getInternalName());

		for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
			this.methodNodes[methodIndex].applyMappedMethodName(mappedSignatures, methodMapping);
		}
	}

	private synchronized void addMethodNode(LibraryMethodNode methodNode) {
		LibraryMethodNode[] newMethodNodes = new LibraryMethodNode[this.methodCount + 1];
		System.arraycopy(this.methodNodes, 0, newMethodNodes, 0, this.methodCount);
		newMethodNodes[this.methodCount] = methodNode;
		methodNode.setParent(this);
		this.methodNodes = newMethodNodes;
		this.methodCount++;
	}

	public LibraryMethodNode createPublicStaticMethod(
		String methodName,
		String descriptor,
		ArrayList<AbstractInsnNode> instructions,
		int maxStack,
		int maxLocals,
		LocalVariableTable localVariableTable,
		TryCatchBlockInfo[] tryCatchBlocks,
		List<TypeWriter> typeWriters,
		Inheritance inheritance,
		String changeLogName,
		int generationType
	) {
		InstructionListNode instructionListNode = new InstructionListNode(instructions, localVariableTable, changeLogName);
		UTFStringWriter methodNameWriter = new UTFStringWriter(0, this.constantPoolWriter, methodName);
		typeWriters.add(methodNameWriter);
		UTFStringWriter descriptorWriter = new UTFStringWriter(0, this.constantPoolWriter, descriptor);
		typeWriters.add(descriptorWriter);
		UTFStringWriter codeNameWriter = new UTFStringWriter(0, this.constantPoolWriter, "Code");
		typeWriters.add(codeNameWriter);
		CodeAttribute codeAttribute = new CodeAttribute(codeNameWriter, maxStack, maxLocals, instructionListNode, tryCatchBlocks);
		AttributesNode[] methodAttributes = new AttributesNode[]{codeAttribute};
		LibraryMethodNode methodNode = new LibraryMethodNode(this, methodNameWriter, descriptorWriter, methodAttributes, false, generationType);
		methodNode.setPublic();
		methodNode.setStatic(true);
		this.addMethodNode(methodNode);
		inheritance.registerMethod(methodNode);
		return methodNode;
	}

	@Override
	public TypeWriter getConstantPoolEntry(int index) {
		return this.constantPoolWriter.getConstantPoolEntry(index);
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
	}

	public LibraryMethodNode[] getMethodNodes() {
		return this.methodNodes;
	}

	public LibraryFieldNode[] getFieldNodes() {
		return this.fieldNodes;
	}

	public void applyMappedFieldNames(DualKeyMap<String, FieldSignature, FieldSignature> fieldMappings) {
		for (int fieldIndex = 0; fieldIndex < this.fieldsCount; fieldIndex++) {
			this.fieldNodes[fieldIndex].applyMappedFieldName(fieldMappings.getGroup(this.getInternalName()));
		}
	}

	public void remapConstantsAndDescriptors(HashMap<String, String> classNameMappings, AbstractLogger logger) {
		this.constantPoolWriter.remapConstantNames(classNameMappings, logger);
		LibraryMethodNode[] methodNodeArray = this.getMethodNodes();
		int methodNodeCount = methodNodeArray.length;

		for (int i = 0; i < methodNodeCount; i++) {
			methodNodeArray[i].remapDescriptor(classNameMappings);
		}
	}

	@Override
	public AbstractFieldNode[] getFields() {
		return this.getFieldNodes();
	}

	private synchronized void addFieldNode(LibraryFieldNode fieldNode) {
		LibraryFieldNode[] newFieldNodes = new LibraryFieldNode[this.fieldsCount + 1];
		System.arraycopy(this.fieldNodes, 0, newFieldNodes, 0, this.fieldsCount);
		newFieldNodes[this.fieldsCount] = fieldNode;
		fieldNode.setParent(this);
		this.fieldNodes = newFieldNodes;
		this.fieldsCount++;
	}

	public void resolveConstantPoolMembers(AbstractWorkspace workspace, ClassProvider classProvider, PhantomClassManager phantomClassManager) {
		this.constantPoolWriter.resolveMemberReferences(workspace, classProvider, phantomClassManager);
	}

	public LibraryMethodNode getOrCreateStubMethod(MethodSignature methodSignature) {
		LibraryMethodNode methodNode = (LibraryMethodNode)this.findMethod(methodSignature);
		if (methodNode != null) {
			return methodNode;
		} else {
			ArrayList<TypeWriter> entries = new ArrayList<>();
			SimpleConstantPoolWriter poolWriter = (SimpleConstantPoolWriter)this.getConstantPoolWriter();
			UTFStringWriter methodNameWriter = poolWriter.createUtf8String(methodSignature.getName(), entries);
			UTFStringWriter descriptorWriter = poolWriter.createUtf8String(methodSignature.getFullDescriptor(), entries);
			UTFStringWriter exceptionsNameWriter = poolWriter.createUtf8String("Exceptions", entries);
			ClassInfoWriter[] exceptionClassInfos = new ClassInfoWriter[]{poolWriter.getOrCreateClassInfo("java/lang/Throwable", entries)};
			AttributesNode[] methodAttributes = new AttributesNode[]{new ExceptionsAttribute(exceptionsNameWriter, exceptionClassInfos)};
			methodNode = new LibraryMethodNode(this, methodNameWriter, descriptorWriter, methodAttributes, true, 3);
			poolWriter.appendEntriesToPool(entries);
			this.addMethodNode(methodNode);
			return methodNode;
		}
	}

	public LibraryFieldNode addStaticFieldIfAbsent(String fieldName, String descriptor, int accessLevel, boolean isFinal, int generationType) {
		if (this.findField(new FieldSignature(fieldName, descriptor)) != null) {
			return null;
		} else {
			ArrayList<TypeWriter> entries = new ArrayList<>(2);
			UTFStringWriter fieldNameWriter = new UTFStringWriter(0, this.constantPoolWriter, fieldName);
			entries.add(fieldNameWriter);
			UTFStringWriter descriptorWriter = new UTFStringWriter(0, this.constantPoolWriter, descriptor);
			entries.add(descriptorWriter);
			AttributesNode[] fieldAttributes = new AttributesNode[0];
			LibraryFieldNode fieldNode = new LibraryFieldNode(this, fieldNameWriter, descriptorWriter, fieldAttributes, generationType);
			fieldNode.setStatic(true);
			if (isFinal) {
				fieldNode.setFinal(true);
			}

			switch (accessLevel) {
				case 1:
					fieldNode.makePrivate();
					break;
				case 2:
					fieldNode.setDefaultAccess();
					break;
				case 3:
					fieldNode.setPublic();
					break;
				case 4:
					fieldNode.setPublic();
			}

			this.addFieldNode(fieldNode);
			this.constantPoolWriter.appendEntriesToPool(entries);
			this.markChanged();
			this.notify(new AtomicInt(4), this, null);
			return fieldNode;
		}
	}

	@Override
	public AbstractConstantPoolWriter getConstantPoolWriter() {
		return this.constantPoolWriter;
	}

	@Override
	public void remapReferences(
		int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> classNameMappings, HashSet<StringNode> processedStringNodes, Set<String> resourceFilePaths, HashMap<String, String> packageMappings, AbstractLogger logger
	) {
		this.constantPoolWriter.remapConstantNames(classNameMappings, logger);

		for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
			this.methodNodes[methodIndex].remapTypes(keepInnerClassInfo, keepGenericsInformation, packageMappings, classNameMappings);
		}

		for (int fieldIndex = 0; fieldIndex < this.fieldsCount; fieldIndex++) {
			this.fieldNodes[fieldIndex].remapTypes(keepInnerClassInfo, keepGenericsInformation, packageMappings, classNameMappings);
		}

		this.deriveInnerClassSimpleNames();

		for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
			this.attributes[attributeIndex].remapDescriptors(keepInnerClassInfo, keepGenericsInformation, packageMappings, classNameMappings);
		}
	}

	@Override
	public void resolveRefsAndSyncMetadata(ScriptContext scriptContext, AbstractWorkspace workspace, boolean pruneSignatureIfChanged) {
		this.constantPoolWriter.resolveMethodAndDynamicRefs(scriptContext, workspace);

		for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
			if (this.attributes[attributeIndex] instanceof AnnotationContainer) {
				((AnnotationContainer)this.attributes[attributeIndex]).syncNames();
			} else if (this.attributes[attributeIndex] instanceof RecordAttribute) {
				((RecordAttribute)this.attributes[attributeIndex]).syncAnnotationNames();
			}
		}

		for (int fieldIndex = 0; fieldIndex < this.fieldsCount; fieldIndex++) {
			this.fieldNodes[fieldIndex].syncAnnotationNames();
		}

		for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
			this.methodNodes[methodIndex].syncAnnotationNames();
		}
	}

	@Override
	public final boolean isWorkspaceNode() {
		return false;
	}
}
