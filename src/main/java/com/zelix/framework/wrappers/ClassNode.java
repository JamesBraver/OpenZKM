package com.zelix.framework.wrappers;

import com.zelix.enums.TypeTags;
import com.zelix.event.ChangeListener;
import com.zelix.event.EventRegistry;
import com.zelix.event.INotifier;
import com.zelix.event.ObservableStructure;
import com.zelix.exceptions.*;
import com.zelix.framework.*;
import com.zelix.framework.analyzer.MethodAnalyzer;
import com.zelix.framework.analyzer.StaticBlockAnalyzer;
import com.zelix.framework.analyzer.StringConcatAnalysis;
import com.zelix.framework.analyzer.dataset.MethodAnalysisResult;
import com.zelix.framework.analyzer.reference.Reference;
import com.zelix.framework.annotation.AnnotationContainer;
import com.zelix.framework.attributes.*;
import com.zelix.framework.attributes.annotations.AnnotationSettings;
import com.zelix.framework.attributes.annotations.RuntimeInvisibleAnnotationsAttribute;
import com.zelix.framework.attributes.base.AbstractTypeAnnotationsAttributeNode;
import com.zelix.framework.attributes.code.CodeAttribute;
import com.zelix.framework.attributes.code.ExceptionEntryNode;
import com.zelix.framework.attributes.code.TryCatchBlockInfo;
import com.zelix.framework.attributes.debug.LineNumberEntry;
import com.zelix.framework.attributes.debug.SourceDebugExtensionAttribute;
import com.zelix.framework.attributes.debug.SourceFileAttribute;
import com.zelix.framework.attributes.declaration.*;
import com.zelix.framework.code.*;
import com.zelix.framework.constant.*;
import com.zelix.framework.encryption.IntegerEntry;
import com.zelix.framework.encryption.LongEntry;
import com.zelix.framework.encryption.StringEntry;
import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.framework.hierarchy.Inheritance;
import com.zelix.framework.hierarchy.MethodSource;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.instructions.InsnNode;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.instructions.WideConstantPoolInsnNode;
import com.zelix.framework.listener.*;
import com.zelix.framework.reference.IReferenceGraph;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.reference.ReferenceGraphMap;
import com.zelix.framework.signature.*;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.framework.variable.Indexable;
import com.zelix.framework.variable.LocalVariableIndexCounter;
import com.zelix.framework.variable.LocalVariableOperand;
import com.zelix.framework.variable.LocalVariableTable;
import com.zelix.framework.wrappers.property.ClassConstantsEntry;
import com.zelix.framework.wrappers.property.ClassMemberEntry;
import com.zelix.framework.wrappers.property.ClassNameEntry;
import com.zelix.framework.writer.*;
import com.zelix.framework.writer.base.*;
import com.zelix.framework.writer.reflect.ReflectionMethodPattern;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.ObfuscateConfig;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.config.TrimConfig;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.logging.AbstractLogger;
import com.zelix.repository.ClassProvider;
import com.zelix.repository.ClassRepository;
import com.zelix.repository.Hierarchy;
import com.zelix.tools.exceptions.ExceptionObfuscationBuilder;
import com.zelix.tools.exceptions.ObfuscateExceptionExclude;
import com.zelix.tools.excludes.DefaultExcludes;
import com.zelix.tools.excludes.ExcludeElement;
import com.zelix.tools.excludes.ExcludeStatementManager;
import com.zelix.tools.flow.Condition;
import com.zelix.tools.flow.ControlFlowBuilder;
import com.zelix.tools.flow.FlowObfuscateExclude;
import com.zelix.tools.flow.PackageCondition;
import com.zelix.tools.integerEncryption.IntegerEncryptionBuilder;
import com.zelix.tools.integerEncryption.IntegerEncryptionExcludes;
import com.zelix.tools.longEncryption.LongEncryptionBuilder;
import com.zelix.tools.longEncryption.LongEncryptionExcludes;
import com.zelix.tools.parameterChanger.ParameterChangerBuilder;
import com.zelix.tools.parameterChanger.ParameterChangerExcludes;
import com.zelix.tools.parameterChanger.ParameterManager;
import com.zelix.tools.parameterChanger.ParameterPattern;
import com.zelix.tools.parameterChanger.dataset.ChangedMethod;
import com.zelix.tools.parameterChanger.dataset.ParameterRotateEntry;
import com.zelix.tools.parameterChanger.extra.ExtraParameterBuilder;
import com.zelix.tools.references.ObfuscateReferencesBuilder;
import com.zelix.tools.renamer.MethodRenamer;
import com.zelix.tools.renamer.field.AbstractFieldNameGenerator;
import com.zelix.tools.renamer.method.AbstractMethodNameGenerator;
import com.zelix.tools.renamer.naming.AlphaNameGenerator;
import com.zelix.tools.string.StringEncryptionBuilder;
import com.zelix.tools.string.StringEncryptionExcludes;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.utils.ClassMetrics;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.IntegerCache;
import com.zelix.utils.collections.*;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.dataset.*;
import com.zelix.utils.enumerations.ArrayEnumeration;
import com.zelix.utils.enumerations.EmptyEnumeration;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.PhantomClassManager;
import com.zelix.workspace.Workspace;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class ClassNode extends AbstractClassNode implements ChangeListener, ReferenceUpdateListener, ConstantPoolChangeListener {
	private static Vector<ObservableStructure> classPropertyEntries;
	private boolean hasLineNumberTable = false;
	private boolean hasStackMapAttribute = false;
	private AtomicReference<String> progressMessage;
	private EventRegistry eventRegistry;
	public ConstantPoolManager constantPoolManager;
	private FieldNode[] fields;
	private MethodNode[] methods;
	private boolean hasMethodAnnotationConfig;
	private ReferenceGraph<AbstractMethodNode, AbstractClassNode> methodReferenceGraph;
	private AnnotationSettings classAnnotationSet;
	private ReferenceGraph<AbstractFieldNode, AbstractClassNode> fieldReferenceGraph;
	private AbstractMethodCallWriter stringDecodeMethod;

	public ClassNode(
		ByteDataBuffer dataBuffer,
		ClasspathResource classpathResource,
		AtomicReference<String> progressMessage,
		EventRegistry eventRegistry,
		PrintWriter logWriter,
		PrintWriter warningLogWriter,
		ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributes,
		int classType
	) throws IOException {
		super(classpathResource, dataBuffer.getSha1Hash(), classType);
		progressMessage.set("Opening " + classpathResource.getResourceName());
		this.progressMessage = progressMessage;
		this.eventRegistry = eventRegistry;

		try {
			this.readClassHeader(dataBuffer);
			MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References = new MultiMap<>(Math.max(20, (int)(super.constantPoolSize * 0.7)));
			MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences = new MultiMap<>(Math.max(10, (int)(super.constantPoolSize * 0.2)));
			MultiMap<StringNode, StringConstantUpdateListener> stringReferences = new MultiMap<>();
			MultiMap<IntegerNode, IntegerConstants> integerReferences = new MultiMap<>();
			MultiMap<LongNode, LongConstants> longReferences = new MultiMap<>();
			MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences = new MultiMap<>();
			MultiMap<MemberTypeWriter, TypeNode> memberReferences = new MultiMap<>();
			MultiMap<InvokeDynamicCallWriter, IDynamicCallUpdatable> dynamicCallReferences = new MultiMap<>();
			this.constantPoolManager = new ConstantPoolManager(
				dataBuffer, logWriter, this, utf8References, nameAndTypeReferences, classReferences, memberReferences, dynamicCallReferences
			);
			super.constantPoolSize = this.constantPoolManager.getConstantPoolSize();
			this.access = new AccessNode(this, dataBuffer);
			int thisClassIndex = dataBuffer.readUnsignedShort();
			int superClassIndex = dataBuffer.readUnsignedShort();
			super.interfacesCount = dataBuffer.readUnsignedShort();
			int[] interfaceIndexes = new int[super.interfacesCount];

			for (int interfaceIndex = 0; interfaceIndex < super.interfacesCount; interfaceIndex++) {
				interfaceIndexes[interfaceIndex] = dataBuffer.readUnsignedShort();
			}

			this.resolveClassHierarchyTypes(thisClassIndex, superClassIndex, interfaceIndexes, classReferences);
			this.fieldsCount = dataBuffer.readUnsignedShort();
			this.fields = new FieldNode[this.fieldsCount];

			for (int fieldIndex = 0; fieldIndex < this.fieldsCount; fieldIndex++) {
				this.fields[fieldIndex] = new FieldNode(
					this, dataBuffer, utf8References, nameAndTypeReferences, stringReferences, integerReferences, longReferences, classReferences, memberReferences, logWriter, unknownAttributes
				);
				this.fields[fieldIndex].subscribe(this);
			}

			this.methodCount = dataBuffer.readUnsignedShort();
			this.methods = new MethodNode[this.methodCount];

			for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
				this.methods[methodIndex] = new MethodNode(
					this,
					dataBuffer,
					utf8References,
					nameAndTypeReferences,
					stringReferences,
					integerReferences,
					longReferences,
					classReferences,
					memberReferences,
					dynamicCallReferences,
					logWriter,
					unknownAttributes
				);
				this.methods[methodIndex].subscribe(this);
				if (this.methods[methodIndex].isVarArgs() && this.methods[methodIndex].getParameterDescriptors().size() == 0) {
					this.methods[methodIndex].setVarargs(false);
					logWriter.println(
						"ERROR: Method '"
							+ this.methods[methodIndex].getSignature()
							+ "' in class '"
							+ this.methods[methodIndex].getSourcePath()
							+ "' has its ACC_VARARGS flag set yet it has no parameters. This could result in unpredictable behaviour so clearing ACC_VARARGS flag."
					);
				}
			}

			this.attributesCount = dataBuffer.readUnsignedShort();
			this.attributes = new AttributesNode[this.attributesCount];

			for (int attributeIndex = 0; attributeIndex < this.attributesCount; attributeIndex++) {
				this.attributes[attributeIndex] = AttributesNode.readAttribute(
					this,
					dataBuffer,
					utf8References,
					nameAndTypeReferences,
					stringReferences,
					integerReferences,
					longReferences,
					classReferences,
					memberReferences,
					dynamicCallReferences,
					logWriter,
					null,
					unknownAttributes
				);
				if (this.attributes[attributeIndex] instanceof BootstrapMethodsAttribute) {
					this.constantPoolManager.updateBootstrapMethodRefs((BootstrapMethodsAttribute)this.attributes[attributeIndex]);
				}
			}

			try {
				int extraByte = dataBuffer.readUnsignedByte();
				throw new ClassParseFailedException("Extra byte at the end of class file " + extraByte + " in '" + classpathResource.getFullPath() + "' : File is probably corrupt (AZ)");
			} catch (EOFException ignored) {
			} catch (IOException ignored2) {
			}

			this.initializeInnerClassNode();
			if (!Options.dontDuplicateConstants) {
				this.constantPoolManager
					.duplicateAndRemapConstants(
						utf8References, nameAndTypeReferences, stringReferences, integerReferences, longReferences, classReferences, memberReferences, dynamicCallReferences
					);
			}
		} catch (EOFException ignored3) {
			throw new ClassParseFailedException(classpathResource.getFullPath() + " : IO error. File is probably corrupt (E)");
		} catch (DebugAssertFailedException debugAssertFailure) {
			throw debugAssertFailure;
		} catch (LookupException lookupException) {
			throw new ClassParseFailedException(classpathResource.getFullPath() + " : File is probably corrupt. (F) : " + lookupException.getMessage());
		} catch (IOException ignored4) {
			throw new ClassParseFailedException(classpathResource.getFullPath() + " : IO error");
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

	public ClassNode(
		ByteDataBuffer dataBuffer,
		ClasspathResource classpathResource,
		AtomicReference<String> progressMessage,
		EventRegistry eventRegistry,
		PrintWriter logWriter,
		PrintWriter warningLogWriter,
		ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributes
	) throws IOException {
		this(dataBuffer, classpathResource, progressMessage, eventRegistry, logWriter, warningLogWriter, unknownAttributes, 0);
	}

	private MethodNode groupLongConstants(
		KeyValuePairMultiMap<MethodNode, LongNode, PriorityEntry<WideConstantPoolInsnNode>> longNodesByMethod,
		Set<LongNode> methodLongNodes,
		Set<LongNode> clinitLongNodes,
		List<KeyValuePair<LongNode, FieldNode>> longFieldPairs,
		ReferenceGraph<AtomicReference<Long>, LongNode> referenceGraph,
		Map<LongNode, AtomicReference<Long>> referenceHandles
	) {
		MethodNode clinitMethod = null;
		HashMap<Long, AtomicReference<Long>> methodHandlesByValue = GlobalUtils.newHashMap();
		HashMap<Long, AtomicReference<Long>> fieldHandlesByValue = GlobalUtils.newHashMap();
		ArrayList<LongNode> longNodesBuffer = new ArrayList<>();

		for (Entry<MethodNode, List<KeyValuePair<LongNode, PriorityEntry<WideConstantPoolInsnNode>>>> methodLongNodeEntry : longNodesByMethod.entrySet()) {
			MethodNode methodNode = methodLongNodeEntry.getKey();
			List<KeyValuePair<LongNode, PriorityEntry<WideConstantPoolInsnNode>>> longNodePairs = methodLongNodeEntry.getValue();
			longNodesBuffer.clear();

			for (KeyValuePair<LongNode, PriorityEntry<WideConstantPoolInsnNode>> longNodePair : longNodePairs) {
				longNodesBuffer.add(longNodePair.getKey());
			}

			if (methodNode.isClinit()) {
				clinitMethod = methodNode;
				clinitLongNodes.addAll(longNodesBuffer);
			} else {
				methodLongNodes.addAll(longNodesBuffer);
			}

			for (LongNode longNode : longNodesBuffer) {
				Long longValue = longNode.getValue();
				AtomicReference<Long> methodValueHandle = methodHandlesByValue.get(longValue);
				if (methodValueHandle == null) {
					methodValueHandle = new AtomicReference<>(longValue);
					methodHandlesByValue.put(longValue, new AtomicReference<>(longValue));
				}

				referenceGraph.addReference(methodValueHandle, longNode);
				referenceHandles.put(longNode, methodValueHandle);
			}
		}

		Iterator<KeyValuePair<LongNode, FieldNode>> fieldPairIterator = longFieldPairs.iterator();

		while (fieldPairIterator.hasNext()) {
			LongNode fieldLongNode = fieldPairIterator.next().getKey();
			Long fieldLongValue = fieldLongNode.getValue();
			AtomicReference<Long> fieldValueHandle = fieldHandlesByValue.get(fieldLongValue);
			if (fieldValueHandle == null) {
				fieldValueHandle = new AtomicReference<>(fieldLongValue);
				fieldHandlesByValue.put(fieldLongValue, fieldValueHandle);
			}

			referenceGraph.addReference(fieldValueHandle, fieldLongNode);
			referenceHandles.put(fieldLongNode, fieldValueHandle);
		}

		return clinitMethod;
	}

	private void emitMultiLongInit(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		FieldTypeWriter arrayField,
		FieldTypeWriter encryptedArrayField,
		int arraySlotIndex,
		AtomicReference<Long>[] valueReferences,
		ReferenceGraph<AtomicReference<Long>, LongNode> referenceGraph,
		Map<AtomicReference<Long>, LongEntry> entriesByReference,
		Map<FieldTypeWriter, AtomicReference<Long>> referencesByField,
		AtomicReference<LabelNode> switchLabelRef,
		AtomicInt labelCounter,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> labelCounters,
		LocalVariableIndexCounter localVariableIndexCounter,
		List<TypeWriter> typeWriters,
		LongEncryptionBuilder longEncryptionBuilder,
		MethodNode methodNode,
		AtomicReference<Integer> desInitSlotRef,
		Long encryptionKey,
		LocalVariableOperand localVariableOperand,
		Map<MethodNode, LocalVariableOperand> operandsByMethod,
		AtomicReference<Integer> globalKeySlotRef,
		boolean useDesEncryption,
		ParameterChangerBuilder parameterChangerBuilder,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		if (encryptionKey != null) {
			if (localVariableOperand == null) {
				localVariableOperand = parameterChangerBuilder.emitSaltEncodedLongLocal(encryptionKey, localVariableIndexCounter, instructions, methodNode, operandsByMethod, this.getConstantPoolManager(), typeWriters);
			}

			if (useDesEncryption) {
				int desInitSlot = localVariableIndexCounter.getAndIncrease();
				longEncryptionBuilder.buildDesInitCode(localVariableTable, instructions, desInitSlot, encryptionKey, localVariableOperand, localVariableIndexCounter, typeWriters, workspace, classProvider);
				desInitSlotRef.set(INTEGER_CACHE.valueOf(desInitSlot));
			}
		} else if (longEncryptionBuilder.hasGlobalKey()) {
			AbstractInsnNode.appendLongConst(longEncryptionBuilder.getGlobalKey(), instructions, this.constantPoolManager, typeWriters);
			int globalKeySlot = localVariableIndexCounter.get();
			localVariableIndexCounter.add(2);
			instructions.add(AbstractInsnNode.storeLong(globalKeySlot, localVariableTable, 12));
			globalKeySlotRef.set(INTEGER_CACHE.valueOf(globalKeySlot));
		}

		HashSet<TypeWriter> unusedConstants = GlobalUtils.newHashSet();
		longEncryptionBuilder.buildInitEncryptedLongs(
			localVariableTable, instructions, localVariableIndexCounter, unusedConstants, typeWriters, arrayField, encryptedArrayField, arraySlotIndex, valueReferences, referenceGraph, entriesByReference, switchLabelRef.get(), labelCounter, labelCounters, encryptionKey, useDesEncryption, workspace, classProvider
		);
		this.constantPoolManager.removeUnusedConstants(unusedConstants);
		if (!referencesByField.isEmpty()) {
			ArrayList<AbstractInsnNode> storeInstructions = new ArrayList<>();
			if (arrayField != null) {
				for (Entry<FieldTypeWriter, AtomicReference<Long>> fieldReferenceEntry : referencesByField.entrySet()) {
					FieldTypeWriter field = fieldReferenceEntry.getKey();
					AtomicReference<Long> reference = fieldReferenceEntry.getValue();
					LongEntry entry = entriesByReference.get(reference);
					longEncryptionBuilder.buildStoreWithLookup(localVariableTable, storeInstructions, field, entry, encryptionKey, localVariableOperand, typeWriters);
				}
			} else {
				storeInstructions.add(AbstractInsnNode.loadObject(arraySlotIndex, localVariableTable, 12));

				for (Entry<FieldTypeWriter, AtomicReference<Long>> localArrayFieldReferenceEntry : referencesByField.entrySet()) {
					FieldTypeWriter localArrayField = localArrayFieldReferenceEntry.getKey();
					AtomicReference<Long> localArrayReference = localArrayFieldReferenceEntry.getValue();
					LongEntry localArrayEntry = entriesByReference.get(localArrayReference);
					longEncryptionBuilder.buildStoreWithLookup(localVariableTable, storeInstructions, localArrayField, localArrayEntry, encryptionKey, localVariableOperand, typeWriters);
				}

				storeInstructions.add(InsnNode.of(POP));
			}

			instructions.addAll(storeInstructions);
		}
	}

	public void filterBootstrapMethods(Set<BootstrapMethodEntry> retainedEntries) {
		ArrayList<AttributesNode> retainedAttributes = new ArrayList<>();

		for (int i = 0; i < this.attributesCount; i++) {
			if (this.attributes[i] instanceof BootstrapMethodsAttribute) {
				BootstrapMethodsAttribute bootstrapMethods = (BootstrapMethodsAttribute)this.attributes[i];
				bootstrapMethods.filterEntries(retainedEntries);
				if (!bootstrapMethods.isEmpty()) {
					retainedAttributes.add(bootstrapMethods);
				}
			} else {
				retainedAttributes.add(this.attributes[i]);
			}
		}

		if (retainedAttributes.size() < this.attributesCount) {
			this.attributes = retainedAttributes.toArray(new AttributesNode[retainedAttributes.size()]);
			this.attributesCount = this.attributes.length;
		}
	}

	public void resolveExternalMembers(
		Set<MethodNode> bootstrapTargets, AbstractWorkspace workspace, ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext
	) {
		this.constantPoolManager.resolveExternalMembers(bootstrapTargets, workspace, classProvider, scriptContext == null ? null : scriptContext.getPhantomClassManager());

		for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
			if (this.attributes[attributeIndex] instanceof AnnotationContainer) {
				((AnnotationContainer)this.attributes[attributeIndex]).resolveAnnotations(workspace, classProvider, phantomClassManager, scriptContext);
			} else if (this.attributes[attributeIndex] instanceof EnclosingMethodAttribute) {
				((EnclosingMethodAttribute)this.attributes[attributeIndex]).resolveEnclosingMethod(workspace, classProvider);
			} else if (this.attributes[attributeIndex] instanceof RecordAttribute) {
				((RecordAttribute)this.attributes[attributeIndex]).resolveComponents(workspace, classProvider, phantomClassManager, scriptContext);
			}
		}

		for (int fieldIndex = 0; fieldIndex < this.fieldsCount; fieldIndex++) {
			this.fields[fieldIndex].resolveAnnotations(workspace, classProvider, phantomClassManager, scriptContext);
		}

		for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
			this.methods[methodIndex].resolveAnnotations(workspace, classProvider, phantomClassManager, scriptContext);
		}
	}

	public void collectBootstrapMethodRefs(Set<MethodNode> bootstrapTargets) {
		for (int i = 0; i < this.attributes.length; i++) {
			if (this.attributes[i] instanceof BootstrapMethodsAttribute) {
				((BootstrapMethodsAttribute)this.attributes[i]).collectMethodHandleRefs(bootstrapTargets);
			}
		}
	}

	public void collectAnnotationClassRefs(Set<AbstractClassNode> classReferences) {
		for (int attributeIndex = 0; attributeIndex < this.attributesCount; attributeIndex++) {
			if (this.attributes[attributeIndex] instanceof AnnotationContainer) {
				((AnnotationContainer)this.attributes[attributeIndex]).collectClassReferences(classReferences);
			}
		}

		for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
			this.methods[methodIndex].collectAnnotationReferences(classReferences);
		}

		for (int fieldIndex = 0; fieldIndex < this.fieldsCount; fieldIndex++) {
			this.fields[fieldIndex].collectAnnotationReferences(classReferences);
		}
	}

	@Override
	public AbstractMethodNode[] getMethods() {
		return this.getMethodNodes();
	}

	public int updateIntegerConstantValue(int oldValue, int newValue) {
		return this.constantPoolManager.updateIntegerConstantValue(oldValue, newValue);
	}

	public boolean hasMethodAnnotationConfig() {
		return this.hasMethodAnnotationConfig;
	}

	public void pruneInnerClassAttributes(HashSet<ClassNode> excludedClasses) {
		if (super.hasValidInnerClasses || super.hasInvalidInnerClasses) {
			InnerClassesAttribute innerClassesAttribute = null;
			EnclosingMethodAttribute enclosingMethodAttribute = null;

			for (int attributeIndex = 0; attributeIndex < this.attributesCount; attributeIndex++) {
				if (this.attributes[attributeIndex] instanceof InnerClassesAttribute) {
					innerClassesAttribute = (InnerClassesAttribute)this.attributes[attributeIndex];
				} else if (this.attributes[attributeIndex] instanceof EnclosingMethodAttribute) {
					enclosingMethodAttribute = (EnclosingMethodAttribute)this.attributes[attributeIndex];
				}
			}

			int remainingInnerClassCount = innerClassesAttribute.removeIncludedEntries(excludedClasses);
			boolean innerClassesEmpty = false;
			boolean classExcluded = false;
			if (remainingInnerClassCount == 0) {
				innerClassesEmpty = true;
				super.hasValidInnerClasses = false;
				super.hasInvalidInnerClasses = false;
			}

			if (excludedClasses.contains(this)) {
				classExcluded = true;
				this.clearInnerClassInfo();

				for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
					this.methods[methodIndex].stripSyntheticAttributes();
				}

				for (int fieldIndex = 0; fieldIndex < this.fieldsCount; fieldIndex++) {
					this.fields[fieldIndex].stripSyntheticAttributes();
				}
			}

			if (innerClassesEmpty || classExcluded) {
				ArrayList<AttributesNode> retainedAttributes = new ArrayList<>(this.attributesCount);

				for (int keptAttributeIndex = 0; keptAttributeIndex < this.attributesCount; keptAttributeIndex++) {
					if ((this.attributes[keptAttributeIndex] != innerClassesAttribute || !innerClassesEmpty) && (this.attributes[keptAttributeIndex] != enclosingMethodAttribute || !classExcluded)) {
						retainedAttributes.add(this.attributes[keptAttributeIndex]);
					}
				}

				this.attributes = retainedAttributes.toArray(new AttributesNode[retainedAttributes.size()]);
				this.attributesCount = retainedAttributes.size();
			}
		}
	}

	public ReferenceSetContainer buildReferenceSetContainer() {
		ReferenceSetContainer referenceSetContainer = new ReferenceSetContainer();
		this.collectClassAttributeRefs(
			referenceSetContainer.getClassReferenceSet(), referenceSetContainer.getSignatureClassReferenceSet(), referenceSetContainer.getFieldReferenceSet(), referenceSetContainer.getMethodReferenceSet()
		);
		return referenceSetContainer;
	}

	@Override
	public void refreshFieldRefsAndConstants(ScriptContext scriptContext, AbstractWorkspace workspace) {
		this.constantPoolManager.resolveFieldReferences(scriptContext, workspace);

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
			this.fields[fieldIndex].pruneAnnotations();
		}

		for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
			this.methods[methodIndex].pruneAnnotations();
		}
	}

	public ArrayEnumeration<MethodNode> getMethodEnumeration() {
		return new ArrayEnumeration<>(this.methods);
	}

	public void markAllMethodsModified() {
		for (int i = 0; i < this.methodCount; i++) {
			this.methods[i].setCodeModified(true);
		}
	}

	public void removeMethodLocalVariables() {
		for (int i = 0; i < this.methodCount; i++) {
			this.methods[i].removeLocalVariableTables();
		}
	}

	public void mangleParameterNames() {
		for (int i = 0; i < this.methodCount; i++) {
			this.methods[i].mangleMethodParameterNames();
		}
	}

	public void markMethodAnnotationConfig() {
		this.hasMethodAnnotationConfig = true;
	}

	public void removeMethodStackMaps() {
		if (this.hasStackMapAttributes()) {
			for (int i = 0; i < this.methodCount; i++) {
				this.methods[i].removeStackMapAttributes();
			}

			this.setHasStackMapAttribute(false);
		}
	}

	public void refreshClassPropertyEntries() {
		this.refreshFieldPropertyEntry();
		this.refreshMethodPropertyEntry();
		this.refreshConstantPropertyEntry();
	}

	public void removeMethodParameters() {
		for (int i = 0; i < this.methodCount; i++) {
			this.methods[i].removeMethodParameters();
		}
	}

	public void removeAllInnerClassMetadata() {
		if (super.hasValidInnerClasses || super.hasInvalidInnerClasses) {
			Vector<AttributesNode> retainedAttributes = new Vector<>(this.attributesCount);

			for (int attributeIndex = 0; attributeIndex < this.attributesCount; attributeIndex++) {
				if (!(this.attributes[attributeIndex] instanceof InnerClassesAttribute) && !(this.attributes[attributeIndex] instanceof EnclosingMethodAttribute)) {
					retainedAttributes.addElement(this.attributes[attributeIndex]);
				}
			}

			int retainedCount = retainedAttributes.size();
			if (retainedCount < this.attributesCount) {
				this.attributes = new AttributesNode[retainedCount];

				for (int keptAttributeIndex = 0; keptAttributeIndex < retainedCount; keptAttributeIndex++) {
					this.attributes[keptAttributeIndex] = retainedAttributes.elementAt(keptAttributeIndex);
				}

				this.attributesCount = retainedCount;
			}

			super.hasValidInnerClasses = false;
			super.hasInvalidInnerClasses = false;
			this.clearInnerClassInfo();

			for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
				this.methods[methodIndex].stripSyntheticAttributes();
			}

			for (int fieldIndex = 0; fieldIndex < this.fieldsCount; fieldIndex++) {
				this.fields[fieldIndex].stripSyntheticAttributes();
			}
		}

		ClassHierarchyNode.getNodeByInternalName(this.getInternalName()).detachFromParent();
	}

	public void refreshFieldPropertyEntry() {
		if (classPropertyEntries != null) {
			((ClassMemberEntry)classPropertyEntries.elementAt(1)).refreshMembers(this.fields);
		}
	}

	public void applyMethodParameterChanges(
		Set<MethodNode> referencingMethods,
		ParameterChangerExcludes parameterChangerExcludes,
		ChangeLogContext changeLogContext,
		ParameterManager parameterManager,
		ExtraParameterBuilder extraParameterBuilder,
		Hierarchy hierarchy,
		ReferenceGraph<AbstractMethodKey, AbstractMethodNode> referenceGraph,
		int keepInnerClassInfo,
		boolean aggressiveRenaming,
		boolean randomizeObfuscation,
		ClassHierarchyNode classHierarchyNode,
		HashMap<String, String> changeLogNames,
		Map<MethodNode, String> methodNewNames,
		boolean unused
	) {
		HashMap<MethodSignature, MethodNode> methodsBySignature = GlobalUtils.newHashMap();
		HashMap<MethodParameterKey, MethodNode> methodsByParameterKey = GlobalUtils.newHashMap();
		ArrayList<MethodNode> candidateMethods = new ArrayList<>(this.methodCount);

		for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
			MethodNode method = this.methods[methodIndex];
			candidateMethods.add(method);
			if (referencingMethods == null || !referencingMethods.contains(method)) {
				MethodSignature methodSignature = method.getMethodSignature();
				MethodParameterKey parameterKey = methodSignature.getParameterKey();
				methodsBySignature.put(methodSignature, method);
				methodsByParameterKey.put(parameterKey, method);
				referenceGraph.addReference((AbstractMethodKey)(aggressiveRenaming ? methodSignature : parameterKey), method);
			}
		}

		if (randomizeObfuscation || Options.ZKM_RANDOMIZE_OBFUSCATION) {
			GlobalUtils.shuffle(candidateMethods, GlobalUtils.newRandom(512));
		}

		if (changeLogContext != null && changeLogContext.hasParameterChangeData()) {
			Iterator<MethodNode> methodIterator = candidateMethods.iterator();

			while (methodIterator.hasNext()) {
				MethodNode changedMethod = methodIterator.next();
				AbstractMethodNode baseMethod = hierarchy.getBaseMethodOrDefault(changedMethod);
				if (changeLogContext.hasParameterChange(baseMethod)) {
					changedMethod.changeParameterSignature(
						parameterChangerExcludes, changeLogContext, parameterManager, extraParameterBuilder, hierarchy, referenceGraph, classHierarchyNode, this.getInternalName(), this.isInterface(), keepInnerClassInfo, methodsBySignature, methodsByParameterKey, aggressiveRenaming, changeLogNames, methodNewNames
					);
					methodIterator.remove();
				}
			}
		}

		for (MethodNode referencingMethod : candidateMethods) {
			if (referencingMethods != null && referencingMethods.contains(referencingMethod)) {
				referencingMethod.changeParameterSignature(
					parameterChangerExcludes, changeLogContext, parameterManager, extraParameterBuilder, hierarchy, referenceGraph, classHierarchyNode, this.getInternalName(), this.isInterface(), keepInnerClassInfo, methodsBySignature, methodsByParameterKey, aggressiveRenaming, changeLogNames, methodNewNames
				);
			}
		}
	}

	public void clearParameterChangeFlags() {
		for (MethodNode method : this.methods) {
			if (method.isParameterObfuscationMarked()) {
				method.setParameterObfuscationMarked(false);
			}
		}
	}

	public void markParameterAttributeFlags(Map<AbstractMethodNode, AbstractMethodNode> baseMethodMap) {
		for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
			MethodNode methodNode = this.methods[methodIndex];
			AbstractMethodNode baseMethodNode = GlobalUtils.getMappedOrDefault(methodNode, baseMethodMap);
			if (methodNode.hasLocalVariableTable() && baseMethodNode.isWorkspaceNode()) {
				baseMethodNode.setPreserveUnusedParameterSlots(true);
				methodNode.setPreserveUnusedParameterSlots(true);
			}

			if (methodNode.hasMethodParametersAttribute() && baseMethodNode.isWorkspaceNode()) {
				baseMethodNode.setMethodParametersAttributePresent(true);
				methodNode.setMethodParametersAttributePresent(true);
			}
		}
	}

	public void markLoadedFromFilesystem() {
		super.fromFilesystem = true;
	}

	public void refreshConstantPropertyEntry() {
		if (classPropertyEntries != null) {
			ClassConstantsEntry constantsEntry = (ClassConstantsEntry)classPropertyEntries.elementAt(3);
			ConstantWriter[] constantWriters = this.constantPoolManager.getAllConstantWriters();
			constantsEntry.refreshConstants(constantWriters);
		}
	}

	public void refreshMethodLocalVariables() {
		for (int i = 0; i < this.methodCount; i++) {
			this.methods[i].refreshLocalVariableTables();
		}
	}

	public int appendEntriesToConstantPool(List<TypeWriter> typeWriters) {
		return this.constantPoolManager.appendEntriesToPool(typeWriters);
	}

	public void trimAnnotationConfig(ScriptContext scriptContext) {
		if (this.classAnnotationSet != null) {
			RuntimeInvisibleAnnotationsAttribute classAnnotationsAttribute = this.classAnnotationSet.getAttribute();
			ArrayList<AttributesNode> retainedAttributes = new ArrayList<>(this.attributesCount);

			for (AttributesNode attribute : this.attributes) {
				if (attribute == classAnnotationsAttribute) {
					classAnnotationsAttribute.removeAnnotationsByType("com/zelix/annotation/ZKMClassLevel", scriptContext);
					if (classAnnotationsAttribute.getAnnotationCount() > 0) {
						retainedAttributes.add(attribute);
					}
				} else {
					retainedAttributes.add(attribute);
				}
			}

			if (retainedAttributes.size() < this.attributesCount) {
				this.attributes = retainedAttributes.toArray(new AttributesNode[retainedAttributes.size()]);
				this.attributesCount = this.attributes.length;
			}
		}

		if (this.hasMethodAnnotationConfig()) {
			for (MethodNode method : this.methods) {
				if (method.hasMethodConfig()) {
					method.removeMethodConfigAnnotation(scriptContext);
				}
			}
		}
	}

	public void makeClassPublicAndNonFinal(ScriptContext scriptContext) {
		PrintWriter writer = scriptContext.getWriter();
		if (!this.isPublic()) {
			this.setPublic();
			if (scriptContext.isVerbose()) {
				writer.println("\tChanging class '" + this.getClassNameBinary() + "' to be public.");
			}
		}

		if (this.isFinal()) {
			this.setFinal(false);
			if (scriptContext.isVerbose()) {
				writer.println("\tChanging class '" + this.getClassNameBinary() + "' to not be final.");
			}
		}
	}

	public MethodNode createUniqueMethod(
		String descriptor,
		ArrayList<AbstractInsnNode> instructions,
		int maxStack,
		int maxLocals,
		int accessLevel,
		LocalVariableTable localVariableTable,
		TryCatchBlockInfo[] tryCatchBlocks,
		String changeLogName,
		List<TypeWriter> typeWriters,
		Inheritance inheritance,
		AbstractWorkspace workspace,
		int generationType
	) {
		synchronized (inheritance.getMethodLock()) {
			String uniqueMethodName = this.generateUniqueMethodName(workspace.getOverrideMethodNames(), descriptor, inheritance, true);
			return this.createStaticMethod(uniqueMethodName, descriptor, instructions, maxStack, maxLocals, accessLevel, localVariableTable, tryCatchBlocks, changeLogName, typeWriters, inheritance, workspace, generationType);
		}
	}

	public final void addFieldClassReference(AbstractFieldNode fieldNode, AbstractClassNode classNode) {
		if (this.fieldReferenceGraph == null) {
			this.fieldReferenceGraph = new ReferenceGraph<>(10, 5);
		}

		this.fieldReferenceGraph.addReference(fieldNode, classNode);
	}

	public void obfuscateStaticInitializer(
		Condition condition,
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> memberObfuscationRefs,
		TypeResolver typeResolver,
		AbstractWorkspace workspace,
		StaticBlockAnalyzer staticBlockAnalyzer,
		Random random
	) {
		MethodNode staticInitializer = workspace.getMethodBySignature(this, MethodSignature.STATIC_INITIALIZER);
		ArrayList<TypeWriter> addedConstants = new ArrayList<>();
		if (staticInitializer == null) {
			staticInitializer = this.getOrCreateStaticInitializer(addedConstants, workspace, "Flow Obfuscation", 2);
		}

		staticInitializer.insertDummyCondition(condition, memberObfuscationRefs, addedConstants, typeResolver, workspace, staticBlockAnalyzer, random);
		if (addedConstants.size() > 0) {
			this.getConstantPoolManager().appendEntriesToPool(addedConstants);
		}
	}

	public void remapClassNamePrefix(String newPrefix, String oldPrefix, HashMap<String, String> renamedClasses, HashMap<String, String> classNamesByOldName) {
		String internalName = this.getInternalName();
		if (internalName.startsWith(oldPrefix)) {
			String remainingName = internalName.substring(oldPrefix.length());
			String newInternalName = newPrefix + remainingName;
			this.remapInternalName(newInternalName, renamedClasses, classNamesByOldName);
		}
	}

	public void collectAllReferences(HashSet<ClassNode> classReferences, HashSet<FieldNode> fieldReferences, HashSet<MethodNode> methodReferences) {
		this.collectClassAttributeRefs(classReferences, classReferences, fieldReferences, methodReferences);

		for (int fieldIndex = 0; fieldIndex < this.fieldsCount; fieldIndex++) {
			this.fields[fieldIndex].collectReferences(classReferences, classReferences, fieldReferences, methodReferences);
		}

		for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
			this.methods[methodIndex].collectReferences(classReferences, classReferences, fieldReferences, methodReferences);
		}

		this.constantPoolManager.collectExternalDependencies(classReferences, fieldReferences, methodReferences);
	}

	public FieldNode createUniqueField(String descriptor, int accessLevel, boolean isFinal, Inheritance inheritance, AbstractWorkspace workspace, int generationType) {
		synchronized (inheritance.getFieldLock()) {
			return this.createStaticFieldIfAbsent(this.generateDefaultFieldName(workspace.getOverrideFieldNames(), descriptor, inheritance), descriptor, accessLevel, isFinal, inheritance, workspace, generationType);
		}
	}

	public void collectStringTargets(
		NestedMultiMap<ClassNode, MethodNode, StringNode, PriorityEntry<WritableType>> stringConstantEntries,
		KeyValuePairMultiMap<ClassNode, StringNode, FieldNode> staticStringFields,
		MultiMap<ClassNode, FieldNode> instanceFields,
		StringEncryptionExcludes encryptionTargetManager,
		boolean encryptAllStrings,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy,
		ScriptContext scriptContext
	) {
		Set<StringNode> excludedStrings = this.buildStringExclusionSet(encryptionTargetManager, typeResolver, classHierarchy, scriptContext);
		this.collectStringFields(staticStringFields, instanceFields, excludedStrings);

		for (int methodIndex = 0; methodIndex < this.methods.length; methodIndex++) {
			if (encryptAllStrings || !this.methods[methodIndex].isGeneratedMember()) {
				this.methods[methodIndex].encryptStringConstantsInPlace(stringConstantEntries, excludedStrings);
			}
		}
	}

	public void collectReferencedClasses(CountingSet<ClassNode> referencedClasses) {
		this.constantPoolManager.collectReferencedClasses(referencedClasses);
	}

	public int updateUtf8ConstantValue(String oldValue, String newValue) {
		return this.constantPoolManager.updateUtf8StringValue(oldValue, newValue);
	}

	public void trimAttributes(TrimExcludes trimExcludes, TrimConfig trimConfig, ScriptContext scriptContext, PrintWriter printWriter) {
		PrintWriter writer = scriptContext.getWriter();
		if (trimConfig.deleteAnnotationAttributes && this.isAnnotation()) {
			if (trimExcludes.hasAnnotationTypeExclusions() && trimExcludes.isAnnotationTypeExcluded(this)) {
				String annotationRetainedMessage = "Retaining 'annotation' modifier on class "
					+ AbstractClassNode.internalToBinaryName(this.getInternalName())
					+ " because of annotation attribute exclusion.";
				if (scriptContext.isVerbose()) {
					writer.print('\t');
					writer.println(annotationRetainedMessage);
				}

				printWriter.println(annotationRetainedMessage);
			} else {
				this.setAnnotation(false);
			}
		}

		ArrayList<AttributesNode> retainedAttributes = new ArrayList<>();

		for (int attributeIndex = 0; attributeIndex < this.attributesCount; attributeIndex++) {
			if (this.attributes[attributeIndex] instanceof SourceFileAttribute) {
				if (!trimConfig.deleteSourceFileAttributes) {
					retainedAttributes.add(this.attributes[attributeIndex]);
				}
			} else if (this.attributes[attributeIndex] instanceof DeprecatedAttribute) {
				if (!trimConfig.deleteDeprecatedAttributes) {
					retainedAttributes.add(this.attributes[attributeIndex]);
				}
			} else if (this.attributes[attributeIndex] instanceof AnnotationContainer) {
				AnnotationContainer annotationContainer = (AnnotationContainer)this.attributes[attributeIndex];
				if (trimConfig.deleteAnnotationAttributes) {
					if (trimExcludes.hasAnnotationTypeExclusions() && !annotationContainer.trimExcludedAnnotations(trimExcludes, scriptContext, printWriter)) {
						retainedAttributes.add(this.attributes[attributeIndex]);
					}
				} else {
					retainedAttributes.add(this.attributes[attributeIndex]);
				}
			} else if (this.attributes[attributeIndex] instanceof RecordAttribute) {
				if (trimConfig.deleteAnnotationAttributes) {
					((RecordAttribute)this.attributes[attributeIndex]).trimExcludedAnnotations(trimExcludes, scriptContext, printWriter);
				}
			} else if (this.attributes[attributeIndex] instanceof SourceDebugExtensionAttribute) {
				if (!trimConfig.deleteDebugExtensionAttributes) {
					retainedAttributes.add(this.attributes[attributeIndex]);
				}
			} else if (this.attributes[attributeIndex] instanceof UnknownAttribute) {
				if (trimConfig.deleteUnknownAttributes) {
					UnknownAttribute unknownAttribute = (UnknownAttribute)this.attributes[attributeIndex];
					if (scriptContext.isVerbose()) {
						writer.println(
							"\tDeleting unknown attribute '" + unknownAttribute.getAttributeName() + "' in class " + AbstractClassNode.internalToBinaryName(this.getInternalName())
						);
					}

					printWriter.println("Deleting unknown attribute '" + unknownAttribute.getAttributeName() + "' in class " + AbstractClassNode.internalToBinaryName(this.getInternalName()));
				} else {
					retainedAttributes.add(this.attributes[attributeIndex]);
				}
			} else {
				retainedAttributes.add(this.attributes[attributeIndex]);
			}
		}

		if (retainedAttributes.size() < this.attributesCount) {
			this.attributes = retainedAttributes.toArray(new AttributesNode[retainedAttributes.size()]);
			this.attributesCount = this.attributes.length;
		}

		for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
			this.methods[methodIndex].filterAttributes(trimExcludes, trimConfig, scriptContext, printWriter);
		}

		for (int fieldIndex = 0; fieldIndex < this.fieldsCount; fieldIndex++) {
			this.fields[fieldIndex].filterAttributes(trimExcludes, trimConfig, scriptContext, printWriter);
		}
	}

	public void registerFieldsInWorkspace(AbstractWorkspace workspace) {
		for (int i = 0; i < this.fieldsCount; i++) {
			workspace.registerField(this, this.fields[i]);
		}
	}

	public void registerMethodsInWorkspace(AbstractWorkspace workspace) {
		for (int i = 0; i < this.methodCount; i++) {
			workspace.registerMethod(this.methods[i], this);
		}
	}

	public void refreshMethodPropertyEntry() {
		if (classPropertyEntries != null) {
			((ClassMemberEntry)classPropertyEntries.elementAt(2)).refreshMembers(this.methods);
		}
	}

	public Enumeration<AbstractClassNode> getClassesReferencingField(AbstractFieldNode fieldNode) {
		if (this.fieldReferenceGraph == null) {
			return new EmptyEnumeration<>();
		} else {
			Set<AbstractClassNode> referencingClasses = this.fieldReferenceGraph.getReferences(fieldNode);
			return (Enumeration<AbstractClassNode>)(referencingClasses != null ? Collections.enumeration(referencingClasses) : new EmptyEnumeration<>());
		}
	}

	private StringConcatAnalysis analyzeStringConcatFactories() {
		BootstrapMethodsAttribute bootstrapMethods = null;

		for (AttributesNode attribute : this.attributes) {
			if (attribute instanceof BootstrapMethodsAttribute) {
				bootstrapMethods = (BootstrapMethodsAttribute)attribute;
				break;
			}
		}

		if (bootstrapMethods != null) {
			StringConcatAnalysis stringConcatAnalysis = bootstrapMethods.analyzeStringConcatFactories();
			return stringConcatAnalysis.isEmpty() ? null : stringConcatAnalysis;
		} else {
			return null;
		}
	}

	public void applyExceptionObfuscation(
		Set<MethodNode> methods,
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> memberObfuscationRefs,
		Map<MethodNode, ?> methodAnalyzers,
		FlowObfuscateExclude flowObfuscateExclude,
		ObfuscateExceptionExclude obfuscateExceptionExclude,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy,
		ScriptContext scriptContext,
		List<String> exceptionTypes,
		boolean exceptionObfuscation,
		boolean avoidMonitors,
		PhantomClassManager phantomClassManager,
		ClassMetrics classMetrics,
		Inheritance inheritance,
		AbstractWorkspace workspace
	) {
		ArrayList<TypeWriter> addedConstants = new ArrayList<>();
		MethodMapping methodMapping = new MethodMapping();
		if ((obfuscateExceptionExclude == null || !obfuscateExceptionExclude.isClassExcluded(this))
			&& (!Options.ZKM_TREAT_EXCEPTION_OBFUSCATION_AS_FLOW_OBFUSCATION || flowObfuscateExclude == null || !flowObfuscateExclude.isClassExcluded(this))) {
			if (this.classAnnotationSet != null && this.classAnnotationSet.containsKey("exceptionObfuscation")) {
				String annotationConfigName = this.classAnnotationSet.get("exceptionObfuscation");
				int annotationConfigID = ObfuscateConfig.getExceptionConfigID(annotationConfigName);
				if (annotationConfigID != (exceptionObfuscation ? 2 : 1)) {
					String currentConfigName = ObfuscateConfig.getExceptionConfigNameByID(exceptionObfuscation ? 2 : 1);
					if (scriptContext.isVerbose()) {
						scriptContext.getWriter()
							.println(
								"Overriding Exception Obfuscation setting in class '"
									+ ExcludeStatementManager.getFormattedClassSignature(this, classHierarchy, true)
									+ "' : '"
									+ currentConfigName
									+ "' changed to '"
									+ annotationConfigName
									+ "'"
							);
					}

					switch (annotationConfigID) {
						case 0:
							return;
						case 1:
							exceptionObfuscation = false;
							break;
						case 2:
							exceptionObfuscation = true;
					}
				}
			}

			for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
				MethodNode method = this.methods[methodIndex];

				try {
					if (!this.methods[methodIndex].isGeneratedMember()
						&& (obfuscateExceptionExclude == null || !obfuscateExceptionExclude.isMethodExcluded(method))
						&& (!Options.ZKM_TREAT_EXCEPTION_OBFUSCATION_AS_FLOW_OBFUSCATION || flowObfuscateExclude == null || !flowObfuscateExclude.isMethodExcluded(method))) {
						List<ClassInfoWriter> runtimeExceptionSubclasses = this.constantPoolManager.findSubClasses(classHierarchy, "java/lang/RuntimeException");
						method.insertDummyBranches(typeResolver, classHierarchy, exceptionTypes, runtimeExceptionSubclasses, addedConstants, methodMapping, exceptionObfuscation, avoidMonitors, phantomClassManager, scriptContext);
					}
				} catch (MethodAnalyzerException analyzerException) {
					scriptContext.logMessage(
						"Couldn't fully process method '"
							+ this.methods[methodIndex].getDisplayNameWithParams(null)
							+ "' in class '"
							+ this.getRawSourcePath()
							+ "' : "
							+ analyzerException.getMessage()
					);
				} catch (InvalidBytecodeException invalidBytecodeException) {
					throw new ProcessingException(
						"Method '"
							+ this.methods[methodIndex].getDisplayNameWithParams(null)
							+ "' in class '"
							+ this.getSourcePath()
							+ "' appears to be invalid (5) : '"
							+ invalidBytecodeException.getMessage()
							+ "'"
					);
				}
			}

			if (methodMapping.size() > 0) {
				ExceptionObfuscationBuilder.obfuscate(this, methods, memberObfuscationRefs, methodAnalyzers, methodMapping, addedConstants, typeResolver, classHierarchy, classMetrics, inheritance, workspace, scriptContext);
				this.constantPoolManager.appendEntriesToPool(addedConstants);
				this.markChanged();
				this.notify(new AtomicInt(4), this, null);
			}
		}
	}

	public MethodNode createStaticMethod(
		String methodName,
		String descriptor,
		ArrayList<AbstractInsnNode> instructions,
		int maxStack,
		int maxLocals,
		int accessLevel,
		LocalVariableTable localVariableTable,
		TryCatchBlockInfo[] tryCatchBlocks,
		String changeLogName,
		List<TypeWriter> typeWriters,
		Inheritance inheritance,
		AbstractWorkspace workspace,
		int generationType
	) {
		return this.createMethodWithCode(methodName, descriptor, instructions, maxStack, maxLocals, accessLevel, true, localVariableTable, tryCatchBlocks, changeLogName, typeWriters, inheritance, workspace, generationType);
	}

	public InvokeDynamicCallWriter createInvokeDynamicCall(
		String name, String descriptor, AbstractMethodCallWriter bootstrapTarget, List<TypeWriter> typeWriters, ConstantPoolManager constantPoolManager
	) {
		BootstrapMethodsAttribute bootstrapMethods = this.getOrCreateBootstrapMethods(typeWriters);
		NameAndTypeWriter nameAndType = constantPoolManager.createNameAndType(name, descriptor, typeWriters);
		ResolvedMethodHandleWriter methodHandle = constantPoolManager.getOrCreateMethodHandle(MethodHandleKind.REF_INVOKE_STATIC, bootstrapTarget, typeWriters);
		TypeWriter[] bootstrapArguments = new TypeWriter[0];
		BootstrapMethodEntry bootstrapEntry = bootstrapMethods.addEntry(methodHandle, bootstrapArguments);
		InvokeDynamicCallWriter invokeDynamicCall = new InvokeDynamicCallWriter(0, constantPoolManager, nameAndType, bootstrapEntry);
		typeWriters.add(invokeDynamicCall);
		return invokeDynamicCall;
	}

	public void collectLongTargets(
		NestedMultiMap<ClassNode, MethodNode, LongNode, PriorityEntry<WideConstantPoolInsnNode>> longConstantEntries,
		KeyValuePairMultiMap<ClassNode, LongNode, FieldNode> staticLongFields,
		KeyValuePairMultiMap<ClassNode, LongNode, FieldNode> instanceLongFields,
		LongEncryptionExcludes encryptionExcludes,
		boolean includeGeneratedMethods,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy,
		ScriptContext scriptContext
	) {
		Set<LongNode> excludedLongNodes = this.collectExcludedLongNodes(encryptionExcludes, typeResolver, classHierarchy, scriptContext);
		this.collectLongFields(staticLongFields, instanceLongFields, excludedLongNodes);
		ArrayList<TypeWriter> addedConstants = new ArrayList<>();
		ConstantPoolManager poolManager = this.getConstantPoolManager();

		for (int methodIndex = 0; methodIndex < this.methods.length; methodIndex++) {
			if ((encryptionExcludes == null || !encryptionExcludes.isMethodExcluded(this.methods[methodIndex])) && (includeGeneratedMethods || !this.methods[methodIndex].isGeneratedMember())) {
				this.methods[methodIndex].encryptLowValueLongs(longConstantEntries, excludedLongNodes, encryptionExcludes, addedConstants, poolManager);
			}
		}

		if (!addedConstants.isEmpty()) {
			poolManager.appendEntriesToPool(addedConstants);
		}
	}

	public void buildReferenceGraphs(
		ReferenceGraphMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> referenceGraph,
		ObfuscateReferencesBuilder obfuscateReferencesBuilder,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy,
		ScriptContext scriptContext
	) {
		for (int i = 0; i < this.methodCount; i++) {
			try {
				this.methods[i].buildReferenceGraph(referenceGraph, obfuscateReferencesBuilder, typeResolver, classHierarchy);
			} catch (MethodAnalyzerException analyzerException) {
				scriptContext.logMessage(analyzerException.getMessage() + " : " + this.getSourcePath());
			} catch (InvalidBytecodeException invalidBytecodeException) {
				throw new ProcessingException(
					"Method '"
						+ this.methods[i].getDisplayNameWithParams(null)
						+ "' in class '"
						+ this.getSourcePath()
						+ "' appears to be invalid (7) : '"
						+ invalidBytecodeException.getMessage()
						+ "'"
				);
			}
		}
	}

	public MethodCallWriter getOrCreateClassMethodRef(AbstractMethodNode methodNode, List<TypeWriter> typeWriters) {
		return this.constantPoolManager.getOrCreateClassMethodRef(methodNode, typeWriters);
	}

	public void setAllMethodGenerationTypes(int generationType) {
		MethodNode[] methodNodes = this.methods;
		int methodNodeCount = methodNodes.length;

		for (int i = 0; i < methodNodeCount; i++) {
			methodNodes[i].setGenerationType(generationType);
		}

		this.constantPoolManager.clearStringObfuscation();
	}

	public void setClassAccessFlags(int accessFlags) {
		this.access.setAccessFlags(accessFlags);
		this.markChanged();
		this.notify(new AtomicInt(1), this, null);
	}

	public void pruneSignatureMetadata(int keepGenericsInformation) {
		if (keepGenericsInformation == 1) {
			ArrayList<AttributesNode> retainedAttributes = new ArrayList<>(this.attributes.length);

			for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
				if (!(this.attributes[attributeIndex] instanceof SignatureAttribute)) {
					if (this.attributes[attributeIndex] instanceof AbstractTypeAnnotationsAttributeNode) {
						AbstractTypeAnnotationsAttributeNode typeAnnotationsAttribute = (AbstractTypeAnnotationsAttributeNode)this.attributes[attributeIndex];
						typeAnnotationsAttribute.removeSignatureAnnotations();
						if (!typeAnnotationsAttribute.isEmpty()) {
							retainedAttributes.add(this.attributes[attributeIndex]);
						} else if (this.attributes[attributeIndex] instanceof RecordAttribute) {
							((RecordAttribute)this.attributes[attributeIndex]).removeSignatureAttributes();
							retainedAttributes.add(this.attributes[attributeIndex]);
						}
					} else {
						retainedAttributes.add(this.attributes[attributeIndex]);
					}
				}
			}

			if (retainedAttributes.size() < this.attributes.length) {
				AttributesNode[] attributeArray = new AttributesNode[retainedAttributes.size()];
				this.attributes = retainedAttributes.toArray(attributeArray);
				this.attributesCount = this.attributes.length;
			}

			for (int fieldIndex = 0; fieldIndex < this.fieldsCount; fieldIndex++) {
				this.fields[fieldIndex].pruneSignatureMetadata(keepGenericsInformation);
			}

			for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
				this.methods[methodIndex].pruneSignatureMetadata(keepGenericsInformation);
			}
		}
	}

	private LabelNode createJumpLabelBinding(AtomicReference<LabelNode> switchLabelRef, AtomicReference<AtomicInt> labelCounterRef, JumpType jumpType, Map<LabelNode, AtomicInt> labelReplacements) {
		AtomicInt labelCounter = null;
		LabelNode labelNode = new LabelNode(true, 1);
		if (jumpType == JumpType.GOTO) {
			labelCounter = new AtomicInt(-2);
			labelReplacements.put(labelNode, labelCounter);
		}

		switchLabelRef.set(labelNode);
		labelCounterRef.set(labelCounter);
		return labelNode;
	}

	public AbstractClassNode getAnyReferencingClass(AbstractMethodNode methodNode) {
		if (this.methodReferenceGraph == null) {
			return null;
		} else {
			Set<AbstractClassNode> referencingClasses = this.methodReferenceGraph.getReferences(methodNode);
			return referencingClasses != null ? referencingClasses.iterator().next() : null;
		}
	}

	private void mapIntegerFieldReferences(
		List<KeyValuePair<IntegerNode, FieldNode>> integerFieldPairs,
		Map<IntegerNode, AtomicReference<Integer>> referenceHandles,
		Map<FieldTypeWriter, AtomicReference<Integer>> referenceHandlesByField,
		List<KeyValuePair<IntegerNode, FieldNode>> ctorAssignedIntegerFields,
		List<TypeWriter> typeWriters
	) {
		for (KeyValuePair<IntegerNode, FieldNode> integerFieldPair : integerFieldPairs) {
			IntegerNode integerNode = integerFieldPair.getKey();
			FieldNode fieldNode = integerFieldPair.getValue();
			fieldNode.removeConstantValueAttribute();
			FieldTypeWriter fieldWriter = this.constantPoolManager.getOrCreateFieldRef(this.getInternalName(), fieldNode.getNameDisplay(), fieldNode.getDescriptor(), typeWriters, fieldNode);
			referenceHandlesByField.put(fieldWriter, referenceHandles.get(integerNode));
		}

		Iterator<KeyValuePair<IntegerNode, FieldNode>> fieldPairIterator = ctorAssignedIntegerFields.iterator();

		while (fieldPairIterator.hasNext()) {
			FieldNode ctorAssignedField = fieldPairIterator.next().getValue();
			boolean assignedByConstructor = false;

			for (int i = 0; i < this.methodCount; i++) {
				MethodNode methodNode = this.methods[i];
				if (methodNode.isConstructor() && methodNode.writesField(ctorAssignedField)) {
					assignedByConstructor = true;
					break;
				}
			}

			if (assignedByConstructor) {
				ctorAssignedField.removeConstantValueAttribute();
			}
		}
	}

	@Override
	public TypeWriter getConstantPoolEntry(int index) {
		return this.constantPoolManager.getConstantPoolEntry(index);
	}

	public boolean hasClassAnnotation() {
		return this.classAnnotationSet != null;
	}

	public void analyzeReflectionCalls(
		Set<ClassNode> classesToAnalyze,
		Map<AbstractMethodCallWriter, ReflectionMethodPattern> reflectionPatterns,
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldReadAccesses,
		ClassRepository classRepository,
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldWriteAccesses,
		HierarchicalMultiMap<MethodID, InstructionListNode, Integer> methodAccesses,
		PrintWriter printWriter,
		AtomicInt reflectionCount,
		AbstractWorkspace workspace,
		TypeResolver typeResolver,
		String indent,
		Map<MethodID, MethodAnalysisResult> methodAnalysisResults,
		Map<FieldID, HashSet<Reference>> fieldReferences,
		Map<ExcludeElement, HashSet<Reference>> stackReferences,
		HashMap<ExcludeElement, HashSet<Reference>> localVariableReferences,
		Map<InstructionListNode, MethodAnalyzer> methodAnalyzerCache,
		DualKeyMap<AbstractClassNode, Reference, Integer> stringClassReferences,
		MultiMap<String, Reference> stringResourcePathReferences,
		DualKeyMap<AbstractFieldNode, Reference, Reference> stringFieldNameReferences,
		DualKeyMap<AbstractMethodNode, Reference, Reference> stringMethodNameReferences,
		DualKeyMap<String, Reference, Reference> stringInternalNameReferences,
		Set<StringNode> processedStringNodes,
		MultiMap<MethodNode, StructureNode> usageReferences,
		AbstractLogger logger,
		List<MethodInvocation> methodInvocations,
		boolean verbose
	) {
		if (classesToAnalyze.contains(this)) {
			boolean classHeaderWritten = false;

			for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
				try {
					MultiMap<AbstractMethodCallWriter, String> notesByCallWriter = this.methods[methodIndex]
						.analyzeReflectionCalls(
							reflectionPatterns,
							classRepository,
							fieldReadAccesses,
							fieldWriteAccesses,
							methodAccesses,
							typeResolver,
							methodAnalysisResults,
							fieldReferences,
							stackReferences,
							localVariableReferences,
							workspace,
							methodAnalyzerCache,
							stringClassReferences,
							stringResourcePathReferences,
							stringFieldNameReferences,
							stringMethodNameReferences,
							stringInternalNameReferences,
							processedStringNodes,
							usageReferences,
							methodInvocations,
							verbose
						);
					if (notesByCallWriter != null && notesByCallWriter.size() > 0) {
						if (!classHeaderWritten) {
							printWriter.write(
								"In class "
									+ this.getBinaryName()
									+ (this.isInnerClass() ? " (multi-release version " + this.getInnerClassOrdinal() + ")" : "")
									+ Options.lineSeparator
							);
							classHeaderWritten = true;
						}

						printWriter.write(indent + "in method " + this.methods[methodIndex].toDeclarationStringSimple(false) + Options.lineSeparator);
						Enumeration<AbstractMethodCallWriter> callWriterEnumeration = notesByCallWriter.keys();

						while (callWriterEnumeration.hasMoreElements()) {
							AbstractMethodCallWriter callWriter = callWriterEnumeration.nextElement();
							List<String> callNotes = notesByCallWriter.get(callWriter);

							for (int noteIndex = 0; noteIndex < callNotes.size(); noteIndex++) {
								String noteSuffix;
								if (verbose) {
									noteSuffix = " " + callNotes.get(noteIndex);
								} else {
									noteSuffix = "";
								}

								printWriter.write(indent + indent + callWriter.toDisplayString() + noteSuffix + Options.lineSeparator);
								reflectionCount.increaseAndGet();
							}
						}
					}
				} catch (InvalidBytecodeException invalidBytecodeException) {
					throw new ProcessingException(
						"Method '"
							+ this.methods[methodIndex].getDisplayNameWithParams(null)
							+ "' in class '"
							+ this.getSourcePath()
							+ "' appears to be invalid (1) : '"
							+ invalidBytecodeException.getMessage()
							+ "'"
					);
				} catch (DebugAssertFailedException assertFailedException) {
					throw assertFailedException;
				} catch (Throwable unexpectedError) {
					logger.errorWithDetails(
						"ERROR: ",
						"Possible invalid class '" + this.getRawSourcePath() + "'",
						"Unexpected error while analyzing method '"
							+ this.methods[methodIndex].getDisplayNameWithParams(null)
							+ "' in class '"
							+ this.getBinaryName()
							+ "' ("
							+ this.getRawSourcePath()
							+ "). The class file may be invalid : "
							+ unexpectedError.getClass().getName()
							+ " '"
							+ unexpectedError.getMessage()
							+ "'"
					);
				}
			}
		}
	}

	public MethodNode createMethodWithCode(
		String methodName,
		String descriptor,
		ArrayList<AbstractInsnNode> instructions,
		int maxStack,
		int maxLocals,
		int accessLevel,
		boolean isStatic,
		LocalVariableTable localVariableTable,
		TryCatchBlockInfo[] tryCatchBlocks,
		String changeLogName,
		List<TypeWriter> typeWriters,
		Inheritance inheritance,
		AbstractWorkspace workspace,
		int generationType
	) {
		ConstantPoolManager constantPool = this.getConstantPoolManager();
		InstructionListNode instructionListNode = new InstructionListNode(instructions, localVariableTable, changeLogName);
		UTFStringWriter nameWriter = new UTFStringWriter(0, constantPool, methodName);
		typeWriters.add(nameWriter);
		UTFStringWriter descriptorWriter = new UTFStringWriter(0, constantPool, descriptor);
		typeWriters.add(descriptorWriter);
		UTFStringWriter codeNameWriter = new UTFStringWriter(0, constantPool, "Code");
		typeWriters.add(codeNameWriter);
		CodeAttribute codeAttribute = new CodeAttribute(codeNameWriter, maxStack, maxLocals, instructionListNode, tryCatchBlocks);
		AttributesNode[] methodAttributes = new AttributesNode[]{codeAttribute};
		MethodNode methodNode = new MethodNode(this, nameWriter, descriptorWriter, methodAttributes, generationType);
		switch (accessLevel) {
			case 1:
				methodNode.makePrivate();
				break;
			case 2:
				methodNode.setDefaultAccess();
				break;
			case 3:
				methodNode.makeProtected();
				break;
			case 4:
				methodNode.setPublic();
		}

		if (isStatic) {
			methodNode.setStatic(true);
		}

		this.addMethodNode(methodNode, workspace);
		if (inheritance != null) {
			inheritance.registerMethod(methodNode);
		}

		return methodNode;
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		this.access.acceptVisitor(visitor);
		if (super.thisClassWriter != null && !super.thisClassWriter.isResolvable()) {
			((ClassInfoWriter)super.thisClassWriter).accept(visitor, this, MethodSource.THIS_CLASS);
		}

		if (this.superClassWriter != null && !this.superClassWriter.isResolvable()) {
			((ClassInfoWriter)this.superClassWriter).accept(visitor, this, MethodSource.SUPER_CLASS);
		}

		for (AbstractClassInfoWriter interfaceWriter : super.interfaceWriters) {
			if (!interfaceWriter.isResolvable()) {
				((ClassInfoWriter)interfaceWriter).accept(visitor, this, MethodSource.INTERFACE_ENTRY);
			}
		}

		AttributesNode[] attributeNodes = this.attributes;
		int arrayLength = attributeNodes.length;

		for (int attributeIndex = 0; attributeIndex < arrayLength; attributeIndex++) {
			attributeNodes[attributeIndex].acceptVisitor(visitor);
		}

		FieldNode[] fieldNodes = this.fields;
		arrayLength = fieldNodes.length;

		for (int fieldIndex = 0; fieldIndex < arrayLength; fieldIndex++) {
			fieldNodes[fieldIndex].acceptVisitor(visitor);
		}

		MethodNode[] methodNodes = this.methods;
		arrayLength = methodNodes.length;

		for (int methodIndex = 0; methodIndex < arrayLength; methodIndex++) {
			methodNodes[methodIndex].acceptVisitor(visitor);
		}
	}

	public void initClinitChangedMethod(
		Map<MethodNode, ChangedMethod> changedMethodsByMethod, Inheritance inheritance, AbstractWorkspace workspace, ClassProvider classProvider, List<TypeWriter> typeWriters, ParameterChangerBuilder parameterChangerBuilder
	) {
		MethodNode staticInitializerMethod;
		if (!this.isInnerClass()) {
			staticInitializerMethod = workspace.getMethodBySignature(this, MethodSignature.STATIC_INITIALIZER);
		} else {
			staticInitializerMethod = this.findMethodNode(MethodSignature.STATIC_INITIALIZER);
		}

		if (staticInitializerMethod != null) {
			ChangedMethod changedMethod = changedMethodsByMethod.get(staticInitializerMethod);
			if (changedMethod != null) {
				changedMethod.buildSaltField(this, workspace, classProvider, typeWriters, inheritance, parameterChangerBuilder);
			}
		}
	}

	@Override
	public void updateClassReference(ClassInfoWriter oldReference, ClassInfoWriter newReference) {
		if (!this.isModule()) {
			if (super.thisClassWriter == oldReference) {
				super.thisClassWriter = newReference;
			}

			if (this.superClassWriter == oldReference) {
				this.superClassWriter = newReference;
			}
		}

		for (int i = 0; i < super.interfaceWriters.length; i++) {
			if (super.interfaceWriters[i] == oldReference) {
				super.interfaceWriters[i] = newReference;
				break;
			}
		}
	}

	public void renameFieldsWithPrefix(String oldPrefix, String newPrefix, DualKeyMap<String, String, String> renameMap, Workspace workspace) {
		for (int i = 0; i < this.fieldsCount; i++) {
			if (this.fields[i].getNameDisplay().startsWith(oldPrefix)) {
				this.fields[i].renameWithPrefix(oldPrefix, newPrefix, renameMap, this, workspace);
			}
		}
	}

	private void mapLongFieldReferences(
		List<KeyValuePair<LongNode, FieldNode>> longFieldPairs,
		Map<LongNode, AtomicReference<Long>> referenceHandles,
		Map<FieldTypeWriter, AtomicReference<Long>> referenceHandlesByField,
		List<KeyValuePair<LongNode, FieldNode>> ctorAssignedLongFields,
		List<TypeWriter> typeWriters
	) {
		for (KeyValuePair<LongNode, FieldNode> longFieldPair : longFieldPairs) {
			LongNode longNode = longFieldPair.getKey();
			FieldNode fieldNode = longFieldPair.getValue();
			fieldNode.removeConstantValueAttribute();
			FieldTypeWriter fieldWriter = this.constantPoolManager.getOrCreateFieldRef(this.getInternalName(), fieldNode.getNameDisplay(), fieldNode.getDescriptor(), typeWriters, fieldNode);
			referenceHandlesByField.put(fieldWriter, referenceHandles.get(longNode));
		}

		Iterator<KeyValuePair<LongNode, FieldNode>> fieldPairIterator = ctorAssignedLongFields.iterator();

		while (fieldPairIterator.hasNext()) {
			FieldNode ctorAssignedField = fieldPairIterator.next().getValue();
			boolean assignedByConstructor = false;

			for (int i = 0; i < this.methodCount; i++) {
				MethodNode methodNode = this.methods[i];
				if (methodNode.isConstructor() && methodNode.writesField(ctorAssignedField)) {
					assignedByConstructor = true;
					break;
				}
			}

			if (assignedByConstructor) {
				ctorAssignedField.removeConstantValueAttribute();
			}
		}
	}

	public void collectAttributeReferences(HashSet<ClassNode> classReferences, HashSet<ClassNode> signatureClassReferences, HashSet<FieldNode> fieldReferences, HashSet<MethodNode> methodReferences) {
		for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
			AttributesNode attribute = this.attributes[attributeIndex];
			if (attribute instanceof SignatureAttribute) {
				((SignatureAttribute)attribute).collectReferencedClasses(signatureClassReferences);
			} else if (attribute instanceof AnnotationContainer) {
				((AnnotationContainer)attribute).collectAllReferences(classReferences, signatureClassReferences, fieldReferences, methodReferences);
			} else if (attribute instanceof InnerClassesAttribute) {
				((InnerClassesAttribute)attribute).collectInnerClassReferences(classReferences, signatureClassReferences, fieldReferences, methodReferences);
			} else if (attribute instanceof PermittedSubclassesAttribute) {
				((PermittedSubclassesAttribute)attribute).collectAllReferences(classReferences, signatureClassReferences, fieldReferences, methodReferences);
			}
		}
	}

	private void collectClassAttributeRefs(HashSet<ClassNode> classReferences, HashSet<ClassNode> signatureClassReferences, HashSet<FieldNode> fieldReferences, HashSet<MethodNode> methodReferences) {
		for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
			AttributesNode attribute = this.attributes[attributeIndex];
			if (attribute instanceof SignatureAttribute) {
				((SignatureAttribute)attribute).collectReferencedClasses(signatureClassReferences);
			} else if (attribute instanceof AnnotationContainer) {
				((AnnotationContainer)attribute).collectAllReferences(classReferences, signatureClassReferences, fieldReferences, methodReferences);
			} else if (attribute instanceof RecordAttribute) {
				((RecordAttribute)attribute).collectAllReferences(classReferences, signatureClassReferences, fieldReferences, methodReferences);
			}
		}
	}

	public ConstantPoolManager getConstantPoolManager() {
		return this.constantPoolManager;
	}

	public void pruneMethodParameters(boolean trimNonPublic, boolean trimRenamed) {
		boolean renameTriggeredTrim = false;
		if (trimRenamed && this.isNameChanged()) {
			renameTriggeredTrim = true;
		}

		if (renameTriggeredTrim) {
			this.removeMethodParameters();
		} else {
			for (int i = 0; i < this.methodCount; i++) {
				this.methods[i].pruneMethodParameters(trimNonPublic, trimRenamed);
			}
		}
	}

	private synchronized void addField(FieldNode fieldNode, AbstractWorkspace workspace) {
		FieldNode[] newFields = new FieldNode[this.fieldsCount + 1];
		System.arraycopy(this.fields, 0, newFields, 0, this.fieldsCount);
		newFields[this.fieldsCount] = fieldNode;
		fieldNode.setParent(this);
		this.fields = newFields;
		this.fieldsCount++;
		if (!this.isInnerClass()) {
			workspace.registerField(this, fieldNode);
		}
	}

	@Override
	public int getFieldsCount() {
		return this.fields.length;
	}

	public MethodNode createPublicStaticMethod(
		String methodName,
		String descriptor,
		ArrayList<AbstractInsnNode> instructions,
		int maxStack,
		int maxLocals,
		LocalVariableTable localVariableTable,
		TryCatchBlockInfo[] tryCatchBlocks,
		String changeLogName,
		List<TypeWriter> typeWriters,
		Inheritance inheritance,
		AbstractWorkspace workspace,
		int generationType
	) {
		return this.createStaticMethod(methodName, descriptor, instructions, maxStack, maxLocals, 4, localVariableTable, tryCatchBlocks, changeLogName, typeWriters, inheritance, workspace, generationType);
	}

	public void shuffleMemberOrder(Random random) {
		GlobalUtils.shuffle((FieldNode[])this.fields, random);
		GlobalUtils.shuffle((MethodNode[])this.methods, random);
	}

	private synchronized void addMethodNode(MethodNode methodNode, AbstractWorkspace workspace) {
		MethodNode[] newMethods = new MethodNode[this.methodCount + 1];
		System.arraycopy(this.methods, 0, newMethods, 0, this.methodCount);
		newMethods[this.methodCount] = methodNode;
		methodNode.setParent(this);
		this.methods = newMethods;
		this.methodCount++;
		if (!this.isInnerClass()) {
			workspace.registerMethod(methodNode, this);
		}
	}

	public FieldNode findFieldNode(FieldSignature fieldSignature) {
		return (FieldNode)super.findField(fieldSignature);
	}

	private void collectStringFields(
		KeyValuePairMultiMap<ClassNode, StringNode, FieldNode> staticStringFields, MultiMap<ClassNode, FieldNode> instanceStringFields, Set<StringNode> excludedStrings
	) {
		for (int fieldIndex = 0; fieldIndex < this.fields.length; fieldIndex++) {
			FieldNode fieldNode = this.fields[fieldIndex];
			if (fieldNode.isString()) {
				ConstantValueAttribute constantValueAttribute = fieldNode.getConstantValueAttribute();
				if (constantValueAttribute != null) {
					StringNode stringNode = constantValueAttribute.getStringConstant();
					if (stringNode.getUtf8StringWriter().getLength() >= 2 && !excludedStrings.contains(stringNode)) {
						if (fieldNode.isStatic()) {
							staticStringFields.put(this, stringNode, fieldNode);
						} else {
							instanceStringFields.put(this, fieldNode);
						}
					}
				}
			}
		}
	}

	public String getExtendsClause() {
		return this.superClassWriter == null ? "" : "extends " + this.superClassWriter.getExternalName() + " ";
	}

	private void emitMultiIntegerInit(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		FieldTypeWriter arrayField,
		FieldTypeWriter encryptedArrayField,
		int arraySlotIndex,
		AtomicReference<Integer>[] valueReferences,
		ReferenceGraph<AtomicReference<Integer>, IntegerNode> referenceGraph,
		Map<AtomicReference<Integer>, IntegerEntry> entriesByReference,
		Map<FieldTypeWriter, AtomicReference<Integer>> referencesByField,
		AtomicReference<LabelNode> switchLabelRef,
		AtomicInt labelCounter,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> labelCounters,
		LocalVariableIndexCounter localVariableIndexCounter,
		List<TypeWriter> typeWriters,
		IntegerEncryptionBuilder integerEncryptionBuilder,
		MethodNode methodNode,
		AtomicReference<Integer> desInitSlotRef,
		Long encryptionKey,
		LocalVariableOperand localVariableOperand,
		Map<MethodNode, LocalVariableOperand> operandsByMethod,
		AtomicReference<Integer> globalKeySlotRef,
		boolean useDesEncryption,
		ParameterChangerBuilder parameterChangerBuilder,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		if (encryptionKey != null) {
			if (localVariableOperand == null) {
				localVariableOperand = parameterChangerBuilder.emitSaltEncodedLongLocal(encryptionKey, localVariableIndexCounter, instructions, methodNode, operandsByMethod, this.getConstantPoolManager(), typeWriters);
			}

			if (useDesEncryption) {
				int desInitSlot = localVariableIndexCounter.getAndIncrease();
				integerEncryptionBuilder.generateDesInitCode(localVariableTable, instructions, desInitSlot, encryptionKey, localVariableOperand, localVariableIndexCounter, typeWriters, workspace, classProvider);
				desInitSlotRef.set(INTEGER_CACHE.valueOf(desInitSlot));
			}
		} else if (integerEncryptionBuilder.hasGlobalKey()) {
			AbstractInsnNode.appendLongConst(integerEncryptionBuilder.getGlobalKey(), instructions, this.constantPoolManager, typeWriters);
			int globalKeySlot = localVariableIndexCounter.get();
			localVariableIndexCounter.add(2);
			instructions.add(AbstractInsnNode.storeLong(globalKeySlot, localVariableTable, 11));
			globalKeySlotRef.set(INTEGER_CACHE.valueOf(globalKeySlot));
		}

		HashSet<TypeWriter> unusedConstantWriters = GlobalUtils.newHashSet();
		integerEncryptionBuilder.generateInitEncryptedStringsAndLongsCode(
			localVariableTable, instructions, localVariableIndexCounter, unusedConstantWriters, typeWriters, arrayField, encryptedArrayField, arraySlotIndex, valueReferences, referenceGraph, entriesByReference, switchLabelRef.get(), labelCounter, labelCounters, encryptionKey, useDesEncryption, workspace, classProvider
		);
		this.constantPoolManager.removeUnusedConstants(unusedConstantWriters);
		if (!referencesByField.isEmpty()) {
			ArrayList<AbstractInsnNode> storeInstructions = new ArrayList<>();
			if (arrayField != null) {
				for (Entry<FieldTypeWriter, AtomicReference<Integer>> arrayFieldEntry : referencesByField.entrySet()) {
					FieldTypeWriter arrayFieldWriter = arrayFieldEntry.getKey();
					AtomicReference<Integer> arrayFieldReference = arrayFieldEntry.getValue();
					IntegerEntry arrayFieldIntegerEntry = entriesByReference.get(arrayFieldReference);
					integerEncryptionBuilder.generateStoreEncryptedIntegerCode(localVariableTable, storeInstructions, arrayFieldWriter, arrayFieldIntegerEntry, encryptionKey, localVariableOperand, typeWriters);
				}
			} else {
				storeInstructions.add(AbstractInsnNode.loadObject(arraySlotIndex, localVariableTable, 11));

				for (Entry<FieldTypeWriter, AtomicReference<Integer>> instanceFieldEntry : referencesByField.entrySet()) {
					FieldTypeWriter instanceFieldWriter = instanceFieldEntry.getKey();
					AtomicReference<Integer> instanceFieldReference = instanceFieldEntry.getValue();
					IntegerEntry instanceFieldIntegerEntry = entriesByReference.get(instanceFieldReference);
					integerEncryptionBuilder.generateStoreEncryptedIntegerCode(localVariableTable, storeInstructions, instanceFieldWriter, instanceFieldIntegerEntry, encryptionKey, localVariableOperand, typeWriters);
				}

				storeInstructions.add(InsnNode.of(POP));
			}

			instructions.addAll(storeInstructions);
		}
	}

	public MethodNode getOrCreateStaticInitializer(AbstractWorkspace workspace, String changeLogName, int generationType) {
		ArrayList<TypeWriter> generatedTypeWriters = new ArrayList<>();
		MethodNode staticInitializer = this.getOrCreateStaticInitializer(generatedTypeWriters, workspace, changeLogName, generationType);
		if (!generatedTypeWriters.isEmpty()) {
			this.constantPoolManager.appendEntriesToPool(generatedTypeWriters);
		}

		return staticInitializer;
	}

	private Set<IntegerNode> collectExcludedIntegerNodes(IntegerEncryptionExcludes encryptionExcludes, TypeResolver typeResolver, ClassHierarchy classHierarchy, ScriptContext scriptContext) {
		HashSet<IntegerNode> excludedIntegerNodes = GlobalUtils.newHashSet();
		if (encryptionExcludes != null && encryptionExcludes.hasClassWithExcludedFields(this)) {
			Enumeration<FieldNode> excludedFieldEnumeration = encryptionExcludes.getExcludedFieldsForClass(this);

			while (excludedFieldEnumeration.hasMoreElements()) {
				ConstantValueAttribute constantValueAttribute = excludedFieldEnumeration.nextElement().getConstantValueAttribute();
				if (constantValueAttribute != null) {
					IntegerNode integerNode = constantValueAttribute.getIntegerConstant();
					if (integerNode != null) {
						excludedIntegerNodes.add(integerNode);
					}
				}
			}

			for (int i = 0; i < this.methodCount; i++) {
				MethodNode methodNode = this.methods[i];
				if (methodNode.hasIntegerConstants() && (methodNode.isConstructor() || methodNode.isClinit())) {
					methodNode.collectExcludedIntegerNodes(excludedIntegerNodes, encryptionExcludes);
				}
			}
		}

		if (encryptionExcludes != null && encryptionExcludes.hasClassWithExcludedMethods(this)) {
			Enumeration<MethodNode> excludedMethodEnumeration = encryptionExcludes.getExcludedMethodsForClass(this);

			while (excludedMethodEnumeration.hasMoreElements()) {
				MethodNode excludedMethod = excludedMethodEnumeration.nextElement();
				if (excludedMethod.hasIntegerConstants()) {
					excludedMethod.collectIntegerNodes(excludedIntegerNodes);
				}
			}
		}

		return excludedIntegerNodes;
	}

	public void analyzeFlowForParameters(
		MultiMap<InstructionListNode, InstructionBlock> instructionBlocks,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy,
		Map<InstructionListNode, MethodAnalyzer> methodAnalyzerCache,
		int analysisMode,
		int flowObfuscationConfigId,
		boolean licenseVerified,
		boolean keepBalancedLocks,
		ScriptContext scriptContext,
		FlowObfuscateExclude flowObfuscateExclude,
		Random random
	) {
		if ((!this.isInterface() || this.isJava8OrNewer()) && !this.isGeneratedClass()) {
			PrintWriter logWriter = scriptContext.getWriter();
			ArrayList<TypeWriter> generatedTypeWriters = new ArrayList<>(7);
			if (flowObfuscateExclude == null || !flowObfuscateExclude.isClassExcluded(this)) {
				if (this.classAnnotationSet != null && this.classAnnotationSet.containsKey("obfuscateFlow")) {
					String newFlowConfigName = this.classAnnotationSet.get("obfuscateFlow");
					int newFlowConfigId = ObfuscateConfig.getFlowConfigID(newFlowConfigName);
					if (flowObfuscationConfigId != newFlowConfigId) {
						String oldFlowConfigName = ObfuscateConfig.getFlowConfigNameByID(flowObfuscationConfigId);
						flowObfuscationConfigId = newFlowConfigId;
						if (scriptContext.isVerbose()) {
							logWriter.println(
								"Overriding Method Parameter Changing's Flow Obfuscation setting in class '"
									+ ExcludeStatementManager.getFormattedClassSignature(this, classHierarchy, true)
									+ "' : '"
									+ oldFlowConfigName
									+ "' changed to '"
									+ newFlowConfigName
									+ "'"
							);
						}

						if (newFlowConfigId == 0) {
							return;
						}
					}
				}

				List<PriorityEntry<MethodNode>> methodsByCodeLength = this.getMethodsOrderedByCodeLength();
				int accumulatedObfuscatedBlocks = 0;
				for (int methodIndex = methodsByCodeLength.size() - 1; methodIndex >= 0; methodIndex--) {
					MethodNode methodNode = methodsByCodeLength.get(methodIndex).getValue();
					if (!methodNode.isGeneratedMember() && !methodNode.isClinit() && (flowObfuscateExclude == null || !flowObfuscateExclude.isMethodExcluded(methodNode))) {
						try {
							accumulatedObfuscatedBlocks += methodNode.analyzeControlFlow(instructionBlocks, typeResolver, scriptContext, classHierarchy, analysisMode, flowObfuscationConfigId, licenseVerified, keepBalancedLocks, methodAnalyzerCache, true, random);
						} catch (MethodAnalyzerException ignoredMethodAnalysis) {
						} catch (ClasspathException ignoredClasspath) {
						} catch (InvalidBytecodeException ignoredInvalidBytecode) {
						}

						if (false) {
							if (accumulatedObfuscatedBlocks > 2) {
								break;
							}
						}
					}
				}
			}

			if (generatedTypeWriters.size() > 0) {
				this.constantPoolManager.appendEntriesToPool(generatedTypeWriters);
				this.markChanged();
				this.notify(new AtomicInt(4), this, null);
			}
		}
	}

	private List<PriorityEntry<MethodNode>> getMethodsOrderedByCodeLength() {
		List<PriorityEntry<MethodNode>> methodsByCodeLength = new ArrayList<>(this.methodCount);

		for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
			MethodNode methodNode = this.methods[methodIndex];
			methodsByCodeLength.add(new PriorityEntry<>(methodNode.getCodeLengthWithSizeField(), methodNode));
		}

		Collections.sort(methodsByCodeLength);
		return methodsByCodeLength;
	}

	public void recalculateLocalVarTypes(TypeResolver typeResolver, ClassHierarchy classHierarchy, ScriptContext scriptContext) {
		ArrayList<TypeWriter> generatedTypeWriters = new ArrayList<>();

		for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
			try {
				this.methods[methodIndex].remapLocalVariables(this.constantPoolManager, generatedTypeWriters, typeResolver, classHierarchy);
			} catch (MethodAnalyzerException analyzerException) {
				scriptContext.logWarningInternal(
					"Method could not be analyzed in class '"
						+ this.getClassNode().getRawSourcePath()
						+ "' while attempting to update local variable types : '"
						+ analyzerException.getMessage()
						+ "'",
					true
				);
			} catch (InvalidBytecodeException invalidBytecodeException) {
				throw new ProcessingException(this.getRawSourcePath() + " " + this.methods[methodIndex].getDisplayName() + " : " + invalidBytecodeException.getMessage());
			}
		}

		this.constantPoolManager.appendEntriesToPool(generatedTypeWriters);
	}

	public void addReferencedSubclasses(String className, CountingSet<String> subclassNames, ClassHierarchy classHierarchy) {
		Iterator<ClassInfoWriter> subClassIterator = this.constantPoolManager.findSubClasses(classHierarchy, className).iterator();

		while (subClassIterator.hasNext()) {
			String subClassInternalName = subClassIterator.next().getInternalName();
			subclassNames.add(subClassInternalName);
		}
	}

	public BootstrapMethodsAttribute getOrCreateBootstrapMethods(List<TypeWriter> typeWriters) {
		for (int i = 0; i < this.attributesCount; i++) {
			if (this.attributes[i] instanceof BootstrapMethodsAttribute) {
				return (BootstrapMethodsAttribute)this.attributes[i];
			}
		}

		BootstrapMethodsAttribute bootstrapMethodsAttribute = new BootstrapMethodsAttribute(this, this.constantPoolManager.createUtf8String("BootstrapMethods", typeWriters));
		AttributesNode[] newAttributes = new AttributesNode[this.attributes.length + 1];
		System.arraycopy(this.attributes, 0, newAttributes, 0, this.attributes.length);
		newAttributes[this.attributes.length] = bootstrapMethodsAttribute;
		this.attributes = newAttributes;
		this.attributesCount = this.attributes.length;
		return bootstrapMethodsAttribute;
	}

	public void applyParameterObfuscation(
		Map<MethodNode, ParameterPattern> parameterPatternByMethod,
		Map<MethodNode, ChangedMethod> changedMethodByMethod,
		Map<AbstractClassNode, ChangedMethod> changedMethodByClass,
		List<MethodNode> methodsInClass,
		Map<MethodNode, Long> parameterKeysByMethod,
		Set<MethodNode> methodsToChange,
		Inheritance inheritance,
		Map<MethodNode, LocalVariableOperand> decryptionLocalVarByMethod,
		TypeResolver typeResolver,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		List<TypeWriter> typeWriters,
		ControlFlowBuilder controlFlowBuilder,
		ParameterChangerBuilder parameterChangerBuilder,
		Hierarchy hierarchy,
		Set<MethodNode> methodsNeedingClassSeedXor
	) {
		MethodNode staticInitializerMethod;
		if (!this.isInnerClass()) {
			staticInitializerMethod = workspace.getMethodBySignature(this, MethodSignature.STATIC_INITIALIZER);
		} else {
			staticInitializerMethod = this.findMethodNode(MethodSignature.STATIC_INITIALIZER);
		}

		ChangedMethod staticInitializerChange = null;
		if (staticInitializerMethod != null) {
			staticInitializerChange = changedMethodByMethod.get(staticInitializerMethod);
		}

		Condition classCondition = null;
		Random random = null;
		boolean conditionAppliesToClinit = true;
		if (controlFlowBuilder != null) {
			PackageCondition packageCondition = controlFlowBuilder.getPackageCondition(this);
			if (packageCondition != null) {
				StaticBlockAnalyzer staticBlockAnalyzer = controlFlowBuilder.getStaticBlockAnalyzer();
				classCondition = packageCondition.getPackageCondition();
				random = controlFlowBuilder.getRandom();
				conditionAppliesToClinit = Options.useStaticBlockEligibility ? staticBlockAnalyzer.hasOnlyLeafDependencies(classCondition.getOwnerClassNode()) : true;
			}
		}

		for (MethodNode methodNode : this.getMethodNodes()) {
			if (!methodNode.isGeneratedMember() || methodNode.isClinit()) {
				Condition methodCondition = classCondition;
				Random methodRandom = random;
				if (classCondition != null && methodNode.isClinit() && !conditionAppliesToClinit) {
					methodCondition = null;
					methodRandom = null;
				}

				ChangedMethod methodChange = null;
				if (staticInitializerChange != null && !methodNode.isClinit()) {
					methodChange = changedMethodByMethod.get(methodNode);
					if (methodChange != null) {
						methodChange.buildSaltField(this, workspace, classProvider, typeWriters, inheritance, parameterChangerBuilder);
					}
				}

				methodNode.obfuscateMethodParameters(
					methodChange == null ? staticInitializerChange : methodChange,
					changedMethodByClass,
					parameterPatternByMethod,
					parameterKeysByMethod,
					methodsToChange,
					decryptionLocalVarByMethod,
					typeResolver,
					workspace,
					typeWriters,
					methodCondition,
					methodRandom,
					this.getConstantPoolManager(),
					hierarchy,
					methodsNeedingClassSeedXor,
					parameterChangerBuilder.getScriptContext()
				);
			}
		}
	}

	public void collectEligibleMethods(Set<MethodNode> eligibleMethods, FlowObfuscateExclude flowObfuscateExclude, ParameterChangerExcludes parameterChangerExcludes) {
		for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
			MethodNode methodNode = this.methods[methodIndex];
			if (!methodNode.isGeneratedMember()
				&& !methodNode.isClinit()
				&& (flowObfuscateExclude == null || !flowObfuscateExclude.isMethodExcluded(methodNode))
				&& (parameterChangerExcludes == null || parameterChangerExcludes.isMethodExcluded(methodNode))
				&& methodNode.hasConditionalJump()) {
				eligibleMethods.add(methodNode);
			}
		}
	}

	public final void addMethodClassReference(AbstractMethodNode methodNode, AbstractClassNode classNode) {
		if (this.methodReferenceGraph == null) {
			this.methodReferenceGraph = new ReferenceGraph<>(10, 5);
		}

		this.methodReferenceGraph.addReference(methodNode, classNode);
	}

	public boolean applyReferenceObfuscation(
		boolean useBaseClass,
		ClassNode baseClassNode,
		Map<MemberContext, Integer> memberIds,
		MultiMap<InstructionListNode, MemberContext> memberContextsByInstruction,
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> memberObfuscationRefs,
		MultiMap<ClassNode, MemberContext> memberContextsByClass,
		List<MemberContext> memberContexts,
		Map<TypeWriter, MemberContext> memberContextByTypeWriter,
		Map<MemberContext, AbstractMethodCallWriter> methodCallWriters,
		Inheritance inheritance,
		int[] memberNamePrefixes,
		int[] memberNameSuffixes,
		Map<Integer, Long> encryptionKeysById,
		AbstractMethodCallWriter classResolverMethodRef,
		AbstractMethodCallWriter methodResolverMethodRef,
		AbstractMethodCallWriter indyBootstrapMethodRef,
		AtomicInt obfuscatedClassesCount,
		AtomicInt obfuscatedMethodsCount,
		AtomicInt obfuscatedReferencesCount,
		Map<MethodNode, Long> encryptionKeysByMethod,
		Map<MethodNode, LocalVariableOperand> operandsByMethod,
		Workspace workspace,
		TypeResolver typeResolver,
		ClassProvider classProvider,
		ObfuscateReferencesBuilder obfuscateReferencesBuilder,
		ScriptContext scriptContext
	) {
		boolean obfuscationApplied = false;
		DualKeyMap<ClassNode, MemberContext, AbstractMethodCallWriter> callWritersByMemberContext = new DualKeyMap<>();
		if (!this.isInterface() || this.isJava8OrNewer()) {
			boolean useInvokeDynamic = ObfuscateReferencesBuilder.shouldUseIndy(useBaseClass ? baseClassNode : this, this);
			ArrayList<TypeWriter> newConstantPoolEntries = new ArrayList<>();
			AtomicReference<AbstractMethodCallWriter> classResolverRef = new AtomicReference<>();
			AtomicReference<AbstractMethodCallWriter> methodResolverRef = new AtomicReference<>();
			AtomicReference<AbstractMethodCallWriter> indyBootstrapRef = new AtomicReference<>();
			new AtomicReference();
			Map<Types, Integer> idsByType = obfuscateReferencesBuilder.assignIdsToTypes(memberContextsByInstruction, classProvider, scriptContext.getPhantomClassManager());
			AtomicBool indyMembersPresent = new AtomicBool();
			AtomicBool nonIndyMembersPresent = new AtomicBool();
			if (!useBaseClass) {
				AtomicBool fieldMembersPresent = new AtomicBool();
				AtomicBool methodMembersPresent = new AtomicBool();
				HashSet<MemberContext> nonIndyMethodContexts = GlobalUtils.newHashSet();
				memberIds = obfuscateReferencesBuilder.assignIdsToMembers(useInvokeDynamic, indyMembersPresent, nonIndyMembersPresent, memberContextsByInstruction, idsByType.size(), nonIndyMethodContexts, fieldMembersPresent, methodMembersPresent, workspace);
				int[] saltTable = obfuscateReferencesBuilder.createSaltTable();
				memberNameSuffixes = obfuscateReferencesBuilder.getAccessKindCodes();
				encryptionKeysById = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(idsByType.size() + memberIds.size()));
				this.initReferenceObfuscation(
					useBaseClass,
					indyMembersPresent.get(),
					nonIndyMembersPresent.get(),
					idsByType,
					memberIds,
					saltTable,
					memberNameSuffixes,
					encryptionKeysById,
					newConstantPoolEntries,
					memberContextsByClass,
					inheritance,
					(ParameterRotateEntry)null,
					nonIndyMethodContexts,
					classProvider,
					workspace,
					obfuscateReferencesBuilder,
					callWritersByMemberContext,
					classResolverRef,
					methodResolverRef,
					indyBootstrapRef,
					fieldMembersPresent.get(),
					methodMembersPresent.get()
				);
			} else {
				Iterator<Entry<InstructionListNode, List<MemberContext>>> memberContextsIterator = memberContextsByInstruction.entrySet().iterator();

				while (memberContextsIterator.hasNext()) {
					for (MemberContext instructionMemberContext : memberContextsIterator.next().getValue()) {
						if (useInvokeDynamic && ObfuscateReferencesBuilder.isMemberEligible(instructionMemberContext, workspace)) {
							indyMembersPresent.set(true);
						} else {
							nonIndyMembersPresent.set(true);
						}
					}
				}

				if (indyMembersPresent.get() && indyBootstrapMethodRef != null) {
					indyBootstrapRef.set(this.constantPoolManager.getOrCreateClassMethodRef((AbstractMethodNode)indyBootstrapMethodRef.getTargetMember(), newConstantPoolEntries));
				}

				if (nonIndyMembersPresent.get()) {
					if (classResolverMethodRef != null) {
						classResolverRef.set(this.constantPoolManager.getOrCreateClassMethodRef((AbstractMethodNode)classResolverMethodRef.getTargetMember(), newConstantPoolEntries));
					}

					if (methodResolverMethodRef != null) {
						methodResolverRef.set(this.constantPoolManager.getOrCreateClassMethodRef((AbstractMethodNode)methodResolverMethodRef.getTargetMember(), newConstantPoolEntries));
					}

					for (MemberContext memberContext : memberContexts) {
						if (ObfuscateReferencesBuilder.needsArgumentArrayResolver(baseClassNode, this, memberContext, workspace)) {
							AbstractMethodCallWriter memberCallWriter = methodCallWriters.get(memberContext);
							MethodCallWriter methodCallWriter = this.constantPoolManager.getOrCreateClassMethodRef((AbstractMethodNode)memberCallWriter.getTargetMember(), newConstantPoolEntries);
							callWritersByMemberContext.put(baseClassNode, memberContext, methodCallWriter);
						}
					}
				}
			}

			ConstantPoolManager poolManager = this.getConstantPoolManager();
			MultiMap<InstructionListNode, InstructionBlock> blocksByInstruction = new MultiMap<>();
			MultiMap<InstructionListNode, ExceptionEntryNode> exceptionEntriesByInstruction = new MultiMap<>();
			HashMap<String, InvokeDynamicCallWriter> invokeDynamicWriters = GlobalUtils.newHashMap();

			for (int i = 0; i < this.methodCount; i++) {
				MethodNode method = this.methods[i];
				LocalVariableOperand localVariableOperand = operandsByMethod != null ? operandsByMethod.get(method) : null;
				Long encryptionKey = localVariableOperand != null ? encryptionKeysByMethod.get(method) : null;
				InstructionListNode instructionList = method.getInstructionList();
				if (instructionList != null && memberContextsByInstruction.containsKey(instructionList)) {
					ArrayList<ExceptionEntryNode> newExceptionEntries = new ArrayList<>();
					AtomicReference<BootstrapMethodsAttribute> bootstrapMethodsAttribute = new AtomicReference<>();
					List<InstructionBlock> patchedBlocks = instructionList.applyReferenceObfuscation(
						memberContextsByInstruction.get(instructionList),
						memberObfuscationRefs,
						memberIds,
						memberContextByTypeWriter,
						encryptionKeysById,
						classResolverRef.get(),
						methodResolverRef.get(),
						callWritersByMemberContext.getGroup(baseClassNode),
						indyBootstrapRef.get(),
						memberNameSuffixes,
						invokeDynamicWriters,
						bootstrapMethodsAttribute,
						encryptionKey,
						localVariableOperand,
						obfuscateReferencesBuilder,
						poolManager,
						newExceptionEntries,
						newConstantPoolEntries,
						workspace,
						classProvider,
						typeResolver,
						useInvokeDynamic
					);
					if (patchedBlocks.size() > 0) {
						blocksByInstruction.putAll(instructionList, patchedBlocks);
					}

					if (newExceptionEntries.size() > 0) {
						exceptionEntriesByInstruction.putAll(instructionList, newExceptionEntries);
					}
				}
			}

			if (newConstantPoolEntries.size() > 0) {
				poolManager.appendEntriesToPool(newConstantPoolEntries);
			}

			PrintWriter writer = null;
			if (scriptContext.isVerbose() && blocksByInstruction.size() > 0) {
				writer = scriptContext.getWriter();
				writer.println("\tObfuscated references in class '" + this.getSourcePath() + "'");
				obfuscatedClassesCount.increaseAndGet();
			}

			for (Entry<InstructionListNode, List<InstructionBlock>> blocksEntry : blocksByInstruction.entrySet()) {
				InstructionListNode patchedInstructionList = blocksEntry.getKey();
				List<InstructionBlock> instructionBlocks = blocksEntry.getValue();
				patchedInstructionList.applyPatchesWithoutStackCheck(instructionBlocks, "Reference Obfuscation");
				if (instructionBlocks != null && instructionBlocks.size() > 0) {
					obfuscationApplied = true;
				}

				List<ExceptionEntryNode> exceptionEntries = exceptionEntriesByInstruction.get(patchedInstructionList);
				if (exceptionEntries != null) {
					((CodeAttribute)patchedInstructionList.getParent()).reorderExceptionHandlers(false, exceptionEntries, typeResolver, workspace, scriptContext);
				}

				if (scriptContext.isVerbose()) {
					writer.println("\t\tmethod '" + ExcludeStatementManager.getFormattedMethodSignature(patchedInstructionList.getMethodNode(), false, workspace) + "'");
					obfuscatedMethodsCount.increaseAndGet();
					if (memberContextsByInstruction.containsKey(patchedInstructionList)) {
						for (MemberContext referencedMemberContext : memberContextsByInstruction.get(patchedInstructionList)) {
							int referenceCount = memberObfuscationRefs.getValues(referencedMemberContext, patchedInstructionList).size();
							String memberSignature;
							if (referencedMemberContext.isField()) {
								memberSignature = ExcludeStatementManager.getFormattedFieldSignature((AbstractFieldNode)referencedMemberContext.getMemberNode(), false, workspace);
							} else {
								memberSignature = ExcludeStatementManager.getFormattedMethodSignature((AbstractMethodNode)referencedMemberContext.getMemberNode(), false, workspace);
							}

							writer.println(
								"\t\t\t"
									+ referenceCount
									+ " reference"
									+ (referenceCount > 1 ? "s" : "")
									+ " to "
									+ (referencedMemberContext.isField() ? "field" : "method")
									+ " '"
									+ memberSignature
									+ "' in class '"
									+ referencedMemberContext.getOwnerClassSourcePath()
									+ "'"
							);
							obfuscatedReferencesCount.add(referenceCount);
						}
					}
				}
			}
		}

		return obfuscationApplied;
	}

	@Override
	public AbstractFieldNode findField(FieldSignature fieldSignature) {
		return this.findFieldNode(fieldSignature);
	}

	private Set<StringNode> buildStringExclusionSet(StringEncryptionExcludes encryptionTargetManager, TypeResolver typeResolver, ClassHierarchy classHierarchy, ScriptContext scriptContext) {
		if (this.isJava9OrNewer() && (encryptionTargetManager == null || !encryptionTargetManager.isClassExcluded(this)) && !Options.skipStringConcatAnalysis) {
			StringConcatAnalysis stringConcatAnalysis = this.analyzeStringConcatFactories();
			if (stringConcatAnalysis != null) {
				this.prepareStringConcat(stringConcatAnalysis, typeResolver, classHierarchy, scriptContext);
			}
		}

		HashSet<StringNode> excludedStringNodes = GlobalUtils.newHashSet();
		if (encryptionTargetManager != null && encryptionTargetManager.hasClassWithExcludedFields(this)) {
			Enumeration<FieldNode> excludedFields = encryptionTargetManager.getExcludedFieldsForClass(this);

			while (excludedFields.hasMoreElements()) {
				ConstantValueAttribute constantValueAttribute = excludedFields.nextElement().getConstantValueAttribute();
				if (constantValueAttribute != null) {
					StringNode stringNode = constantValueAttribute.getStringConstant();
					if (stringNode != null) {
						excludedStringNodes.add(stringNode);
					}
				}
			}

			for (int i = 0; i < this.methodCount; i++) {
				MethodNode method = this.methods[i];
				if (method.hasStringConstants() && (method.isConstructor() || method.isClinit())) {
					method.collectExcludedStringNodes(excludedStringNodes, encryptionTargetManager);
				}
			}
		}

		if (encryptionTargetManager != null && encryptionTargetManager.hasClassWithExcludedMethods(this)) {
			Enumeration<MethodNode> excludedMethods = encryptionTargetManager.getExcludedMethodsForClass(this);

			while (excludedMethods.hasMoreElements()) {
				MethodNode excludedMethod = excludedMethods.nextElement();
				if (excludedMethod.hasStringConstants()) {
					excludedMethod.collectStringNodes(excludedStringNodes);
				}
			}
		}

		return excludedStringNodes;
	}

	public void write(DataOutputStream dataOutputStream, TypeResolver typeResolver, ClassHierarchy classHierarchy, ScriptContext scriptContext) throws IOException {
		this.progressMessage.set("Saving " + this.getBinaryName());
		int attemptCount = 0;

		int stackMapsChanged;
		RemappedConstantPoolWriter constantPoolWriter;
		Map<TypeWriter, TypeWriter> typeWriterMap;
		do {
			HashSet<InstructionListNode> changedInstructionLists = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(this.getMethodCount()));
			this.constantPoolManager.reindexBootstrapMethods();
			ConstantPoolVisitor constantPoolVisitor = new ConstantPoolVisitor(this.constantPoolManager.getConstantPoolSize() / 4, true);
			this.acceptVisitor(constantPoolVisitor);
			constantPoolWriter = this.constantPoolManager.optimizeAndBuildNewPool(constantPoolVisitor, changedInstructionLists);
			if (constantPoolWriter.getConstantPoolSize() > 65535) {
				throw new ProcessingException(
					"Class '" + this.getRawSourcePath() + "' constant pool size exceeds " + 65535 + ". (" + constantPoolWriter.getConstantPoolSize() + ")"
				);
			}

			typeWriterMap = constantPoolWriter.getEntryMapping();
			GlobalUtils.newHashSet();
			stackMapsChanged = 0;

			for (MethodNode method : this.methods) {
				InstructionListNode instructionList = method.getInstructionList();
				if (instructionList != null && (instructionList.isModified() || changedInstructionLists.contains(instructionList) || Options.ZKM_REBUILD_STACK_MAPS && attemptCount == 0)) {
					boolean ldcAndStackMapsOptimized = this.optimizeLdcAndStackMaps(
						method, typeResolver, classHierarchy, this.constantPoolManager, typeWriterMap, !scriptContext.isPreverifyDisabled(), scriptContext.hasMicroEditionClasses()
					);
					stackMapsChanged = stackMapsChanged == 0 && !ldcAndStackMapsOptimized ? 0 : 1;
				}
			}

			if (stackMapsChanged != 0) {
				this.constantPoolManager.updateConstantPoolIndices();
			}
		} while (stackMapsChanged != 0 && attemptCount++ < 3);

		if (stackMapsChanged != 0) {
			throw new ProcessingException("Incompatible stack map change in class '" + this.getSourcePath() + "'");
		} else {
			dataOutputStream.writeInt(0xCAFEBABE);
			dataOutputStream.writeShort(super.minorVersion);
			dataOutputStream.writeShort(this.majorVersion);
			constantPoolWriter.writeConstantPool(dataOutputStream);
			dataOutputStream.writeShort(this.access.getAccessFlags());
			TypeWriter thisClassTypeWriter = typeWriterMap.get(super.thisClassWriter);
			if (thisClassTypeWriter != null) {
				dataOutputStream.writeShort(thisClassTypeWriter.getTag());
			} else {
				dataOutputStream.writeShort(super.thisClassWriter.getTag());
			}

			if (this.superClassWriter != null) {
				TypeWriter superClassTypeWriter = typeWriterMap.get(this.superClassWriter);
				if (superClassTypeWriter != null) {
					dataOutputStream.writeShort(superClassTypeWriter.getTag());
				} else {
					dataOutputStream.writeShort(this.isModule() ? 0 : this.superClassWriter.getTag());
				}
			} else {
				dataOutputStream.writeShort(0);
			}

			dataOutputStream.writeShort(super.interfacesCount);

			for (int interfaceIndex = 0; interfaceIndex < super.interfacesCount; interfaceIndex++) {
				TypeWriter interfaceTypeWriter = typeWriterMap.get(super.interfaceWriters[interfaceIndex]);
				if (interfaceTypeWriter != null) {
					dataOutputStream.writeShort(interfaceTypeWriter.getTag());
				} else {
					dataOutputStream.writeShort(super.interfaceWriters[interfaceIndex].getTag());
				}
			}

			if (this.fieldsCount > 65535) {
				throw new ProcessingException(
					"Class '" + AbstractClassNode.internalToBinaryName(this.getInternalName()) + "' field count exceeds " + 65535 + ". (" + this.fieldsCount + ")"
				);
			} else {
				dataOutputStream.writeShort(this.fieldsCount);

				for (int fieldIndex = 0; fieldIndex < this.fieldsCount; fieldIndex++) {
					this.fields[fieldIndex].writeToStreamWithRemap(dataOutputStream, typeWriterMap, scriptContext);
				}

				if (this.methodCount > 65535) {
					throw new ProcessingException(
						"Class '" + AbstractClassNode.internalToBinaryName(this.getInternalName()) + "' method count exceeds " + 65535 + ". (" + this.methodCount + ")"
					);
				} else {
					dataOutputStream.writeShort(this.methodCount);

					for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
						this.methods[methodIndex].writeToStreamWithRemap(dataOutputStream, typeWriterMap, scriptContext);
					}

					dataOutputStream.writeShort(this.attributesCount);

					for (int attributeIndex = 0; attributeIndex < this.attributesCount; attributeIndex++) {
						if (this.attributes[attributeIndex] instanceof UnknownAttribute && this.attributes[attributeIndex].getAttributeLength() > 0) {
							UnknownAttribute unknownAttribute = (UnknownAttribute)this.attributes[attributeIndex];
							if (unknownAttribute.isSharedByManyClasses()) {
								scriptContext.logWarning(
									"Classes contain unknown attribute '" + unknownAttribute.getAttributeName() + "'. The integrity of this attribute may be affected by obfuscation."
								);
							} else {
								scriptContext.logWarning(
									"Class '"
										+ this.getRawSourcePath()
										+ "' contains unknown attribute '"
										+ unknownAttribute.getAttributeName()
										+ "'. The integrity of this attribute may be affected by obfuscation. Consider using the Trim function to delete it."
								);
							}
						}

						this.attributes[attributeIndex].writeAttributeBodyRemapped(dataOutputStream, typeWriterMap, scriptContext);
					}

					dataOutputStream.flush();
					this.constantPoolManager.updateConstantPoolIndices();
					this.progressMessage.set(" ");
				}
			}
		}
	}

	public void remapInternalName(String newInternalName, HashMap<String, String> renamedClasses, HashMap<String, String> classNamesByOldName) {
		String oldInternalName = this.getInternalName();
		this.getConstantPoolManager().remapTargetClassName(newInternalName);
		this.internalName = newInternalName;
		super.lowerCaseNameKey = this.internalName.toLowerCase();

		for (int i = 0; i < this.attributesCount; i++) {
			if (this.attributes[i] instanceof SourceFileAttribute) {
				((SourceFileAttribute)this.attributes[i]).sourceFileName.setValue(this.getSimpleNameInternal() + ".java");
				break;
			}
		}

		this.markChanged();
		this.notify(new AtomicInt(0), this, null);
		renamedClasses.put(newInternalName, oldInternalName);
		classNamesByOldName.put(oldInternalName, newInternalName);
		ClassHierarchyNode.getNodeByInternalName(oldInternalName).remapInnerClasses(renamedClasses, classNamesByOldName);
	}

	public boolean removeDeadCodeInMethods(TypeResolver typeResolver, AbstractWorkspace workspace, boolean logRemovedInstructions, ScriptContext scriptContext) {
		boolean deadCodeRemoved = false;

		for (int i = 0; i < this.methodCount; i++) {
			try {
				if (this.methods[i].removeDeadCode(typeResolver, workspace, logRemovedInstructions, scriptContext)) {
					deadCodeRemoved = true;
				}
			} catch (MethodAnalyzerException methodAnalyzerException) {
				scriptContext.logWarningInternal(
					"Method could not be analyzed in class '"
						+ this.getClassNode().getRawSourcePath()
						+ "' while attempting the remove dead instructions : '"
						+ methodAnalyzerException.getMessage()
						+ "'",
					true
				);
			} catch (InvalidBytecodeException invalidBytecodeException) {
				throw new ProcessingException(
					"Method '"
						+ this.methods[i].getDisplayNameWithParams(null)
						+ "' in class '"
						+ this.getSourcePath()
						+ "' appears to be invalid (6) : '"
						+ invalidBytecodeException.getMessage()
						+ "'"
				);
			}
		}

		return deadCodeRemoved;
	}

	@Override
	public String toCode() {
		StringBuffer codeBuffer = new StringBuffer();
		Enumeration<String> classAnnotationTypeNames = this.getAnnotationTypeNames();

		while (classAnnotationTypeNames.hasMoreElements()) {
			codeBuffer.append('@');
			codeBuffer.append(internalToBinaryName(classAnnotationTypeNames.nextElement()));
			codeBuffer.append(Options.lineSeparator);
		}

		codeBuffer.append(this.access.getAccessFlagsString());
		codeBuffer.append(this.access.isInterface() ? "interface " : "class ");
		codeBuffer.append(super.thisClassWriter.getExternalName());
		codeBuffer.append(" ");
		codeBuffer.append(this.getExtendsClause() + this.getInterfacesDeclaration() + " {");
		codeBuffer.append(Options.lineSeparator);

		for (int fieldIndex = 0; fieldIndex < this.fieldsCount; fieldIndex++) {
			if (this.fields[fieldIndex].isValid()) {
				codeBuffer.append(Options.lineSeparator);
				Enumeration<String> fieldAnnotationTypeNames = this.fields[fieldIndex].getAnnotationTypeNames();

				while (fieldAnnotationTypeNames.hasMoreElements()) {
					codeBuffer.append("   ");
					codeBuffer.append('@');
					codeBuffer.append(AbstractClassNode.internalToBinaryName(fieldAnnotationTypeNames.nextElement()));
					codeBuffer.append(Options.lineSeparator);
				}

				codeBuffer.append("   ");
				codeBuffer.append(this.fields[fieldIndex].getAccessFlagsString());
				codeBuffer.append(TypeWriter.descriptorToHumanReadable(this.fields[fieldIndex].getDescriptor()));
				codeBuffer.append(" ");
				codeBuffer.append(this.fields[fieldIndex].getNameDisplay());
				codeBuffer.append(Options.lineSeparator);
			}
		}

		for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
			if (this.methods[methodIndex].isValid()) {
				codeBuffer.append(Options.lineSeparator);
				Enumeration<String> methodAnnotationTypeNames = this.methods[methodIndex].getAnnotationTypeNames();

				while (methodAnnotationTypeNames.hasMoreElements()) {
					codeBuffer.append("   ");
					codeBuffer.append('@');
					codeBuffer.append(AbstractClassNode.internalToBinaryName(methodAnnotationTypeNames.nextElement()));
					codeBuffer.append(Options.lineSeparator);
				}

				codeBuffer.append("   ");
				codeBuffer.append(this.methods[methodIndex].toDeclarationStringSimple(false));
				codeBuffer.append(this.methods[methodIndex].buildThrowsClause());
				codeBuffer.append(';');
				codeBuffer.append(Options.lineSeparator);
			}
		}

		codeBuffer.append("}");
		codeBuffer.append(Options.lineSeparator);
		return codeBuffer.toString();
	}

	public String getSourceFileNameOrNull() {
		for (int i = 0; i < this.attributesCount; i++) {
			if (this.attributes[i] instanceof SourceFileAttribute) {
				return ((SourceFileAttribute)this.attributes[i]).sourceFileName.getValue();
			}
		}

		return null;
	}

	public void analyzeMethodsForFlow(
		MultiMap<InstructionListNode, InstructionBlock> instructionBlocks,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy,
		Map<InstructionListNode, MethodAnalyzer> methodAnalyzerCache,
		int analysisMode,
		int flowObfuscationConfigId,
		boolean licenseVerified,
		boolean keepBalancedLocks,
		ControlFlowBuilder controlFlowBuilder,
		ScriptContext scriptContext,
		FlowObfuscateExclude flowObfuscateExclude,
		StaticBlockAnalyzer staticBlockAnalyzer,
		Random random
	) {
		if (!this.isInterface() || this.isJava8OrNewer()) {
			PrintWriter writer = scriptContext.getWriter();
			ArrayList<TypeWriter> typeWriters = new ArrayList<>(7);
			boolean prefixWithSpace = !licenseVerified;
			if (flowObfuscateExclude == null || !flowObfuscateExclude.isClassExcluded(this)) {
				if (this.classAnnotationSet != null && this.classAnnotationSet.containsKey("obfuscateFlow")) {
					String flowConfigName = this.classAnnotationSet.get("obfuscateFlow");
					int flowConfigId = ObfuscateConfig.getFlowConfigID(flowConfigName);
					if (flowObfuscationConfigId != flowConfigId) {
						String previousFlowConfigName = ObfuscateConfig.getFlowConfigNameByID(flowObfuscationConfigId);
						flowObfuscationConfigId = flowConfigId;
						if (scriptContext.isVerbose()) {
							writer.println(
								"Overriding Flow Obfuscation setting in class '"
									+ ExcludeStatementManager.getFormattedClassSignature(this, classHierarchy, true)
									+ "' : '"
									+ previousFlowConfigName
									+ "' changed to '"
									+ flowConfigName
									+ "'"
							);
						}

						if (flowConfigId == 0) {
							return;
						}
					}
				}

				List<PriorityEntry<MethodNode>> methodsByCodeLength = this.getMethodsOrderedByCodeLength();
				int accumulatedObfuscatedBlocks = 0;
				for (int i = methodsByCodeLength.size() - 1; i >= 0; i--) {
					MethodNode method = methodsByCodeLength.get(i).getValue();
					if ((flowObfuscateExclude == null || !flowObfuscateExclude.isMethodExcluded(method))
						&& (staticBlockAnalyzer == null || !staticBlockAnalyzer.isInitializerOrCallee(method))
						&& !controlFlowBuilder.isGeneratedMethod(method)) {
						try {
							int obfuscatedBlockCount = method.analyzeControlFlow(instructionBlocks, typeResolver, scriptContext, classHierarchy, analysisMode, flowObfuscationConfigId, licenseVerified, keepBalancedLocks, methodAnalyzerCache, false, random);
							accumulatedObfuscatedBlocks += obfuscatedBlockCount;
							if (scriptContext.isVerbose() && obfuscatedBlockCount > 0) {
								writer.println(
									"\tObfuscated flow in method '"
										+ ExcludeStatementManager.getFormattedMethodSignature(method, false, classHierarchy)
										+ "' in class '"
										+ ExcludeStatementManager.getFormattedClassSignature(this, classHierarchy, false)
										+ "'"
										+ (prefixWithSpace ? ' ' : "")
								);
								prefixWithSpace = false;
							}
						} catch (MethodAnalyzerException methodAnalyzerException) {
							scriptContext.logMessage(
								"Couldn't obfuscate flow in method '"
									+ ExcludeStatementManager.getFormattedMethodSignature(method, false, classHierarchy)
									+ "' in class '"
									+ ExcludeStatementManager.getFormattedClassSignature(this, classHierarchy, false)
									+ "' : "
									+ methodAnalyzerException.getMessage()
							);
						} catch (ClasspathException classpathException) {
							scriptContext.logWarning(
								"Method '"
									+ ExcludeStatementManager.getFormattedMethodSignature(method, false, classHierarchy)
									+ "' in class '"
									+ ExcludeStatementManager.getFormattedClassSignature(this, classHierarchy, false)
									+ "' could not be flow obfuscated because class '"
									+ classpathException.getClassName()
									+ "' could not be found in classpath."
							);
						} catch (InvalidBytecodeException invalidBytecodeException) {
							throw new ProcessingException(
								"Method '"
									+ ExcludeStatementManager.getFormattedMethodSignature(method, false, classHierarchy)
									+ "' in class '"
									+ this.getSourcePath()
									+ "' appears to be invalid (4) : '"
									+ invalidBytecodeException.getMessage()
									+ "'"
							);
						}

						if (false) {
							if (accumulatedObfuscatedBlocks > 2) {
								break;
							}
						}
					}
				}
			}

			if (typeWriters.size() > 0) {
				this.constantPoolManager.appendEntriesToPool(typeWriters);
				this.markChanged();
				this.notify(new AtomicInt(4), this, null);
			}
		}
	}

	private void injectStringArrayInitCode(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		FieldTypeWriter arrayField,
		FieldTypeWriter encryptedArrayField,
		int arraySlotIndex,
		AtomicReference<String>[] valueReferences,
		ReferenceGraph<AtomicReference<String>, StringNode> referenceGraph,
		Map<AtomicReference<String>, StringEntry> entriesByReference,
		Map<FieldTypeWriter, AtomicReference<String>> referencesByField,
		JumpType jumpType,
		AtomicReference<LabelNode> switchLabelRef,
		AtomicInt labelCounter,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> labelCounters,
		LocalVariableIndexCounter localVariableIndexCounter,
		List<TypeWriter> typeWriters,
		StringEncryptionBuilder stringEncryptionBuilder,
		MethodNode methodNode,
		boolean useDoubleArrayStorage,
		boolean injectXorMask,
		AtomicReference<Integer> desInitSlotRef,
		Long encryptionKey,
		LocalVariableOperand localVariableOperand,
		Map<MethodNode, LocalVariableOperand> operandsByMethod,
		boolean parametersChanged,
		Set<StringNode> processedStrings,
		ParameterChangerBuilder parameterChangerBuilder,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		if (encryptionKey != null) {
			if (localVariableOperand == null) {
				localVariableOperand = parameterChangerBuilder.emitSaltEncodedLongLocal(encryptionKey, localVariableIndexCounter, instructions, methodNode, operandsByMethod, this.getConstantPoolManager(), typeWriters);
			}

			if (parametersChanged) {
				int desInitSlotIndex = localVariableIndexCounter.getAndIncrease();
				stringEncryptionBuilder.injectDesCipherInitCode(localVariableTable, instructions, desInitSlotIndex, encryptionKey, localVariableOperand, localVariableIndexCounter, typeWriters, workspace, classProvider);
				desInitSlotRef.set(INTEGER_CACHE.valueOf(desInitSlotIndex));
			}
		}

		HashSet<TypeWriter> usedEntries = GlobalUtils.newHashSet();
		LabelNode switchLabel = switchLabelRef.get();
		Long desKey = parametersChanged ? encryptionKey : null;
		stringEncryptionBuilder.injectStringInitCode(localVariableTable, instructions, localVariableIndexCounter, usedEntries, typeWriters, arrayField, encryptedArrayField, arraySlotIndex, valueReferences, referenceGraph, entriesByReference, jumpType, switchLabel, labelCounter, labelCounters, injectXorMask, desKey, workspace, classProvider);
		this.constantPoolManager.removeUnusedConstants(usedEntries);

		for (TypeWriter stringConstant : usedEntries) {
			if (stringConstant.getTypeTag() == TypeTags.STRING) {
				processedStrings.add((StringNode)stringConstant);
			}
		}

		if (!referencesByField.isEmpty()) {
			ArrayList<AbstractInsnNode> storeInstructions = new ArrayList<>();
			if (useDoubleArrayStorage && arrayField != null) {
				for (Entry<FieldTypeWriter, AtomicReference<String>> doubleArrayFieldEntry : referencesByField.entrySet()) {
					FieldTypeWriter doubleArrayFieldTypeWriter = doubleArrayFieldEntry.getKey();
					AtomicReference<String> doubleArrayValueReference = doubleArrayFieldEntry.getValue();
					StringEntry doubleArrayStringEntry = entriesByReference.get(doubleArrayValueReference);
					stringEncryptionBuilder.injectDynamicFieldWrite(localVariableTable, storeInstructions, doubleArrayFieldTypeWriter, doubleArrayStringEntry, encryptionKey, localVariableOperand, typeWriters);
				}
			} else {
				if (arrayField != null) {
					storeInstructions.add(new WideConstantPoolInsnNode(GETSTATIC, arrayField));
				} else {
					storeInstructions.add(AbstractInsnNode.loadObject(arraySlotIndex, localVariableTable, 1));
				}

				for (Entry<FieldTypeWriter, AtomicReference<String>> fieldReferenceEntry : referencesByField.entrySet()) {
					FieldTypeWriter fieldTypeWriter = fieldReferenceEntry.getKey();
					AtomicReference<String> valueReference = fieldReferenceEntry.getValue();
					StringEntry stringEntry = entriesByReference.get(valueReference);
					stringEncryptionBuilder.injectDynamicFieldWrite(localVariableTable, storeInstructions, fieldTypeWriter, stringEntry, encryptionKey, localVariableOperand, typeWriters);
				}

				storeInstructions.add(InsnNode.of(POP));
			}

			instructions.addAll(storeInstructions);
		}
	}

	public void encryptLongConstants(
		Inheritance inheritance,
		LongEncryptionBuilder longEncryptionBuilder,
		boolean unused,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		List<KeyValuePair<LongNode, FieldNode>> staticLongFieldPairs,
		List<KeyValuePair<LongNode, FieldNode>> instanceLongFieldPairs,
		KeyValuePairMultiMap<MethodNode, LongNode, PriorityEntry<WideConstantPoolInsnNode>> constantEntries,
		LongEncryptionExcludes encryptionExcludes,
		int maxEncryptedConstants,
		ParameterChangerBuilder parameterChangerBuilder,
		Map<MethodNode, Long> encryptionKeysByMethod,
		Map<MethodNode, LocalVariableOperand> operandsByMethod,
		List<ClassNode> encryptedClasses,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy
	) {
		boolean isInterface = this.isInterface();
		if (encryptionExcludes == null || !encryptionExcludes.isClassExcluded(this)) {
			if (staticLongFieldPairs == null) {
				staticLongFieldPairs = new ArrayList<>(13);
			}

			if (instanceLongFieldPairs == null) {
				instanceLongFieldPairs = new ArrayList<>(0);
			}

			if (constantEntries == null) {
				constantEntries = new KeyValuePairMultiMap<>(13);
			}

			if (staticLongFieldPairs.size() != 0 || constantEntries.size() != 0) {
				if (maxEncryptedConstants < Integer.MAX_VALUE) {
					int totalConstantCount = constantEntries.valueCount();
					if (totalConstantCount > maxEncryptedConstants) {
						int excessConstantCount = totalConstantCount - maxEncryptedConstants;
						int removedCount = 0;
						Enumeration<MethodNode> constantMethodEnumeration = constantEntries.keys();

						label220:
						while (constantMethodEnumeration.hasMoreElements()) {
							MethodNode constantMethod = constantMethodEnumeration.nextElement();
							Iterator<KeyValuePair<LongNode, PriorityEntry<WideConstantPoolInsnNode>>> constantIterator = constantEntries.get(constantMethod).iterator();

							while (constantIterator.hasNext()) {
								KeyValuePair<LongNode, PriorityEntry<WideConstantPoolInsnNode>> removedEntry = constantIterator.next();
								constantIterator.remove();
								if (++removedCount >= excessConstantCount) {
									break label220;
								}
							}
						}
					}
				}

				HashSet<LongNode> methodLongNodes = GlobalUtils.newHashSet();
				HashSet<LongNode> clinitLongNodes = GlobalUtils.newHashSet();
				ReferenceGraph<AtomicReference<Long>, LongNode> referenceGraph = new ReferenceGraph<>();
				HashMap<LongNode, AtomicReference<Long>> referenceHandles = GlobalUtils.newHashMap();
				MethodNode clinitMethod = this.groupLongConstants(constantEntries, methodLongNodes, clinitLongNodes, staticLongFieldPairs, referenceGraph, referenceHandles);
				if (clinitMethod == null) {
					if (!this.isInnerClass()) {
						clinitMethod = workspace.getMethodBySignature(this, MethodSignature.STATIC_INITIALIZER);
					} else {
						clinitMethod = this.findMethodNode(MethodSignature.STATIC_INITIALIZER);
					}
				}

				HashSet<LongNode> allLongNodes = GlobalUtils.newHashSet();
				ArrayList<LongNode> fieldLongNodes = new ArrayList<>(staticLongFieldPairs.size());

				for (KeyValuePair<LongNode, FieldNode> longFieldPair : staticLongFieldPairs) {
					fieldLongNodes.add(longFieldPair.getKey());
				}

				allLongNodes.addAll(fieldLongNodes);
				allLongNodes.addAll(methodLongNodes);
				allLongNodes.addAll(clinitLongNodes);
				HashSet<LongNode> clinitAndFieldLongNodes = GlobalUtils.newHashSet(clinitLongNodes);
				clinitAndFieldLongNodes.addAll(fieldLongNodes);
				int initialMaxLocals = clinitMethod == null ? 0 : clinitMethod.getMaxLocalsCompat();
				clinitLongNodes.size();
				clinitAndFieldLongNodes.size();
				if (!isInterface || !methodLongNodes.isEmpty() || !fieldLongNodes.isEmpty()) {
					ArrayList<TypeWriter> typeWriters = new ArrayList<>();
					HashMap<FieldTypeWriter, AtomicReference<Long>> referencesByField = GlobalUtils.newHashMap();
					this.mapLongFieldReferences(staticLongFieldPairs, referenceHandles, referencesByField, instanceLongFieldPairs, typeWriters);
					if (clinitMethod == null) {
						clinitMethod = this.getOrCreateStaticInitializer(typeWriters, workspace, "Integer Constant Encryption", 11);
					}

					LocalVariableIndexCounter localVariableIndexCounter = new LocalVariableIndexCounter(clinitMethod);
					LocalVariableTable localVariableTable = clinitMethod.getLocalVariableTable();
					FieldTypeWriter arrayField = null;
					boolean multipleConstants = referenceGraph.size() > 1;
					boolean methodLongNodesPresent = methodLongNodes.size() > 0;
					AtomicReference<Long>[] valueReferences = null;
					if (multipleConstants) {
						valueReferences = new AtomicReference[referenceGraph.size()];
					}

					if (methodLongNodesPresent && null == null) {
						String arrayFieldDescriptor;
						if (referenceGraph.size() == 1) {
							arrayFieldDescriptor = "J";
						} else {
							arrayFieldDescriptor = "[J";
						}

						FieldNode encryptedLongsField = this.createUniqueField(arrayFieldDescriptor, isInterface ? 4 : 1, true, inheritance, workspace, 1);
						arrayField = this.constantPoolManager.getOrCreateFieldRef(this.getInternalName(), encryptedLongsField.getNameDisplay(), encryptedLongsField.getDescriptor(), typeWriters, workspace, classProvider, true);
					}

					if (multipleConstants && valueReferences.length > 65535) {
						throw new ProcessingException(
							"Long Constant Encryption can handle a maximumum of 65535 Longs. Number of Longs in class '" + this.getRawSourcePath() + "' is " + valueReferences.length + "."
						);
					}

					boolean encryptLongsInMethod = parameterChangerBuilder != null && parameterChangerBuilder.shouldEncryptLongsInMethod(clinitMethod);
					boolean indyEncryptionEnabled = arrayField != null && encryptLongsInMethod && LongEncryptionBuilder.isIndySupported(this);
					boolean parameterChangerPresent = parameterChangerBuilder != null;
					longEncryptionBuilder.buildIndyEncryptionCall(this, inheritance, workspace, classProvider, typeWriters, arrayField, parameterChangerPresent, multipleConstants, indyEncryptionEnabled, encryptLongsInMethod);
					if (encryptedClasses != null) {
						encryptedClasses.add(this);
					}

					KeyValuePairMultiMap<MethodNode, PriorityEntry<WideConstantPoolInsnNode>, LongEntry> lookupEntriesByMethod = new KeyValuePairMultiMap<>();
					HashMap<AtomicReference<Long>, LongEntry> entriesByReference = GlobalUtils.newHashMap();
					this.buildLongLookupEntries(
						lookupEntriesByMethod, entriesByReference, constantEntries, referenceGraph, referenceHandles, arrayField, longEncryptionBuilder.getIndyCallSiteWriter(this), multipleConstants, encryptLongsInMethod, longEncryptionBuilder.getDecryptionMethodWriter(this), valueReferences, localVariableIndexCounter
					);
					int arraySlotIndex = -1;
					if (!methodLongNodesPresent) {
						if (multipleConstants) {
							arraySlotIndex = localVariableIndexCounter.getAndIncrease();
						} else {
							arraySlotIndex = localVariableIndexCounter.get();
							localVariableIndexCounter.add(2);
						}
					}

					HashMap<LabelNode, AtomicInt> labelCountersByLabel = GlobalUtils.newHashMap();
					MultiMap<LabelNode, PriorityEntry<LabelNode>> labelCounters = new MultiMap<>();
					AtomicReference<AtomicInt> labelCounterRef = new AtomicReference<>();
					AtomicReference<LabelNode> switchLabelRef = new AtomicReference<>();
					LabelNode switchLabel = this.createSwitchLabelState(switchLabelRef, labelCounterRef, labelCountersByLabel);
					int slotIndexBeforeInit = localVariableIndexCounter.get();
					AtomicInt labelCounter = new AtomicInt(-2);
					Long encryptionKey = null;
					LocalVariableOperand localVariableOperand = null;
					if (parameterChangerBuilder != null) {
						encryptionKey = encryptionKeysByMethod != null ? encryptionKeysByMethod.get(clinitMethod) : null;
						localVariableOperand = operandsByMethod != null ? operandsByMethod.get(clinitMethod) : null;
						if (encryptionKey == null) {
							encryptionKey = parameterChangerBuilder.getOrCreateMethodEncryptionKey(clinitMethod);
						}
					}

					AtomicReference<Integer> desInitSlotRef = new AtomicReference<>();
					AtomicReference<Integer> globalKeySlotRef = new AtomicReference<>();
					ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
					if (longEncryptionBuilder.getThreadLocalMapField() != null) {
						longEncryptionBuilder.buildInitThreadLocalMap(instructions, typeWriters, this.constantPoolManager, workspace, classProvider);
					}

					if (multipleConstants) {
						this.emitMultiLongInit(
							localVariableTable,
							instructions,
							arrayField,
							longEncryptionBuilder.getEncryptedLongsArrayField(this),
							arraySlotIndex,
							valueReferences,
							referenceGraph,
							entriesByReference,
							referencesByField,
							switchLabelRef,
							labelCounter,
							labelCounters,
							localVariableIndexCounter,
							typeWriters,
							longEncryptionBuilder,
							clinitMethod,
							desInitSlotRef,
							encryptionKey,
							localVariableOperand,
							operandsByMethod,
							globalKeySlotRef,
							encryptLongsInMethod,
							parameterChangerBuilder,
							workspace,
							classProvider
						);
					} else {
						AtomicReference<Long> valueReference = referenceGraph.getMembersAsEnumeration().nextElement();
						this.emitSingleLongInit(
							localVariableTable,
							instructions,
							arrayField,
							referenceGraph.getAllReferences().nextElement().getValue(),
							switchLabelRef.get(),
							labelCounters,
							labelCounter,
							referencesByField,
							entriesByReference.get(valueReference),
							longEncryptionBuilder,
							clinitMethod,
							localVariableIndexCounter,
							desInitSlotRef,
							encryptionKey,
							localVariableOperand,
							operandsByMethod,
							globalKeySlotRef,
							encryptLongsInMethod,
							parameterChangerBuilder,
							typeWriters,
							workspace,
							classProvider
						);
					}

					localVariableOperand = operandsByMethod != null ? operandsByMethod.get(clinitMethod) : null;
					Enumeration<MethodNode> lookupMethodEnumeration = lookupEntriesByMethod.keys();

					while (lookupMethodEnumeration.hasMoreElements()) {
						MethodNode lookupMethod = lookupMethodEnumeration.nextElement();
						List<KeyValuePair<PriorityEntry<WideConstantPoolInsnNode>, LongEntry>> longEntries = lookupEntriesByMethod.get(lookupMethod);
						Object methodSlotCounter;
						if (lookupMethod.isClinit()) {
							if (localVariableIndexCounter.get() > slotIndexBeforeInit) {
								methodSlotCounter = new AtomicInt(slotIndexBeforeInit);
							} else {
								methodSlotCounter = localVariableIndexCounter;
							}
						} else {
							methodSlotCounter = new AtomicInt(lookupMethod.getMaxLocalsCompat());
						}

						Long methodEncryptionKey = encryptionKeysByMethod != null ? encryptionKeysByMethod.get(lookupMethod) : null;
						LocalVariableOperand methodOperand = operandsByMethod != null ? operandsByMethod.get(lookupMethod) : null;
						lookupMethod.encryptLongConstants(longEntries, labelCountersByLabel, labelCounters, methodEncryptionKey, methodOperand, (Indexable)methodSlotCounter, this.constantPoolManager, typeWriters, typeResolver, classHierarchy);
					}

					longEncryptionBuilder.buildEncryptedValueSwitch(
						localVariableTable, switchLabel, encryptionKey, localVariableOperand, localVariableIndexCounter, instructions, typeWriters, labelCounters, desInitSlotRef.get(), globalKeySlotRef.get(), this.constantPoolManager, workspace, classProvider
					);
					int addedSlotCount = localVariableTable.getSlotCount() - initialMaxLocals;
					ArrayList<Integer> slotIndices = new ArrayList<>(addedSlotCount);

					for (int slotIndex = initialMaxLocals; slotIndex < localVariableTable.getSlotCount(); slotIndex++) {
						slotIndices.add(slotIndex);
					}

					int insertionOffset = 0;
					LabelNode anchorLabel = clinitMethod.getInjectionAnchorLabel();
					if (anchorLabel != null) {
						insertionOffset = clinitMethod.indexOfInstruction(anchorLabel);
					}

					clinitMethod.insertInstructionsAtOffset(instructions, addedSlotCount, clinitMethod.getMaxStack(), insertionOffset, "Integer Constant Encryption", slotIndices);
					if (typeWriters.size() > 0) {
						this.constantPoolManager.appendEntriesToPool(typeWriters);
					}

					this.markChanged();
					this.notify(new AtomicInt(4), this, null);
				}
			}
		}
	}

	public FieldNode addStaticFieldIfAbsent(String fieldName, String descriptor, int accessLevel, Inheritance inheritance, AbstractWorkspace workspace, int generationType) {
		return this.createStaticFieldIfAbsent(fieldName, descriptor, accessLevel, false, inheritance, workspace, generationType);
	}

	public void processMethodRenaming(
		DefaultExcludes defaultExcludes,
		ParameterChangerExcludes parameterChangerExcludes,
		ChangeLogContext changeLogContext,
		MethodRenamer methodRenamer,
		Hierarchy hierarchy,
		AbstractMethodNameGenerator nameGenerator,
		Map<MethodSignature, AbstractMethodNode> processedMethodsBySignature,
		MultiMap<String, String> libraryNamesByDescriptor,
		int keepInnerClassInfo,
		Map<MethodParameterKey, AbstractMethodNode> processedMethodsByParamKey,
		MethodNameMap<String, AbstractMethodNode> interfaceMethodNames,
		MethodNameMap<String, AbstractMethodNode> classMethodNames,
		boolean aggressiveRenaming,
		boolean randomizeObfuscation,
		Set<String> usedNames,
		ClassHierarchyNode classHierarchyNode,
		HashMap<String, String> changeLogNames,
		Map<MethodNode, String> methodNewNames
	) {
		HashMap<MethodSignature, MethodNode> methodsBySignature = GlobalUtils.newHashMap();
		HashMap<MethodParameterKey, AbstractMethodNode> methodsByParameterKey = GlobalUtils.newHashMap();
		ArrayList<MethodNode> methodsToRename = new ArrayList<>(this.methodCount);

		for (int i = 0; i < this.methodCount; i++) {
			methodsToRename.add(this.methods[i]);
		}

		if (randomizeObfuscation || Options.ZKM_RANDOMIZE_OBFUSCATION) {
			GlobalUtils.shuffle(methodsToRename, GlobalUtils.newRandom(512));
		}

		Iterator<MethodNode> methodIterator = methodsToRename.iterator();

		while (methodIterator.hasNext()) {
			methodIterator.next()
				.renameMethodWithHierarchy(
					defaultExcludes,
					parameterChangerExcludes,
					changeLogContext,
					methodRenamer,
					hierarchy,
					nameGenerator,
					processedMethodsBySignature,
					libraryNamesByDescriptor,
					classHierarchyNode,
					this.getInternalName(),
					this.isInterface(),
					keepInnerClassInfo,
					methodsBySignature,
					processedMethodsByParamKey,
					methodsByParameterKey,
					interfaceMethodNames,
					classMethodNames,
					usedNames,
					aggressiveRenaming,
					changeLogNames,
					methodNewNames
				);
		}
	}

	public void initReferenceObfuscation(
		boolean useBaseClass,
		boolean hasIndyMembers,
		boolean hasNonIndyMembers,
		Map<Types, Integer> idsByType,
		Map<MemberContext, Integer> memberIds,
		int[] memberNamePrefixes,
		int[] memberNameSuffixes,
		Map<Integer, Long> encryptionKeysById,
		List<TypeWriter> typeWriters,
		MultiMap<ClassNode, MemberContext> memberContextsByClass,
		Inheritance inheritance,
		ParameterRotateEntry parameterRotateEntry,
		Set<MemberContext> memberContexts,
		ClassProvider classProvider,
		Workspace workspace,
		ObfuscateReferencesBuilder obfuscateReferencesBuilder,
		DualKeyMap<ClassNode, MemberContext, AbstractMethodCallWriter> methodCallWriters,
		AtomicReference<AbstractMethodCallWriter> classResolverMethodRef,
		AtomicReference<AbstractMethodCallWriter> methodResolverMethodRef,
		AtomicReference<AbstractMethodCallWriter> indyBootstrapMethodRef,
		boolean hasFieldReferences,
		boolean hasMethodReferences
	) {
		int totalReferenceCount = idsByType.size() + memberIds.size();
		if (idsByType.size() + memberIds.size() > 262144) {
			throw new ProcessingException(
				"Reference Obfuscation : Too many references specified : " + (idsByType.size() + memberIds.size()) + " > " + 262144 + " in class '" + this.getSourcePath() + "'"
			);
		} else {
			MethodNode clinitMethod = workspace.getMethodBySignature(this, MethodSignature.STATIC_INITIALIZER);
			if (clinitMethod == null) {
				clinitMethod = this.getOrCreateStaticInitializer(typeWriters, workspace, "Reference Obfuscation", 4);
			}

			FieldNode objectArrayField = this.createUniqueField("[Ljava/lang/Object;", this.isInterface() ? 4 : 1, true, inheritance, workspace, 4);
			String objectArrayFieldName = objectArrayField.getNameDisplay();
			FieldTypeWriter objectArrayFieldRef = this.constantPoolManager.getOrCreateFieldRef(this.getInternalName(), objectArrayFieldName, objectArrayField.getDescriptor(), typeWriters, workspace, classProvider, true);
			FieldNode stringArrayField = this.createUniqueField("[Ljava/lang/String;", this.isInterface() ? 4 : 1, true, inheritance, workspace, 4);
			String stringArrayFieldName = stringArrayField.getNameDisplay();
			FieldTypeWriter stringArrayFieldRef = this.constantPoolManager.getOrCreateFieldRef(this.getInternalName(), stringArrayFieldName, stringArrayField.getDescriptor(), typeWriters, workspace, classProvider, true);
			ConstantPoolManager poolManager = this.getConstantPoolManager();
			AbstractMethodCallWriter indexResolver = obfuscateReferencesBuilder.genIndexResolver(this, objectArrayFieldRef, stringArrayFieldRef, memberNamePrefixes, inheritance, workspace, classProvider, typeWriters, poolManager);
			obfuscateReferencesBuilder.injectLoaderToClinit(this, objectArrayFieldRef, stringArrayFieldRef, clinitMethod, idsByType, memberIds, memberNamePrefixes, encryptionKeysById, inheritance, parameterRotateEntry, workspace, classProvider, typeWriters, poolManager);
			AbstractMethodCallWriter classResolverMethod = obfuscateReferencesBuilder.genClassResolverMethod(this, indexResolver, stringArrayFieldRef, objectArrayFieldRef, inheritance, workspace, classProvider, typeWriters, poolManager);
			Integer objectClassId = idsByType.get(classProvider.getClassNode("java/lang/Object"));
			Long objectEncryptionKey = encryptionKeysById.get(objectClassId);
			LongNode keyLongNode = poolManager.getOrCreateLong(objectEncryptionKey, typeWriters);
			AbstractMethodCallWriter fieldResolverHelper = obfuscateReferencesBuilder.genFieldResolverHelper(this, inheritance, workspace, classProvider, typeWriters, poolManager);
			AbstractMethodCallWriter interfaceFieldResolver = obfuscateReferencesBuilder.genInterfaceFieldResolver(this, fieldResolverHelper, inheritance, workspace, classProvider, typeWriters, poolManager);
			classResolverMethodRef.set(obfuscateReferencesBuilder.genFieldResolver(useBaseClass, this, indexResolver, stringArrayFieldRef, objectArrayFieldRef, classResolverMethod, fieldResolverHelper, interfaceFieldResolver, inheritance, workspace, classProvider, typeWriters, keyLongNode, poolManager));
			AbstractMethodCallWriter methodResolverHelper = obfuscateReferencesBuilder.genMethodResolverHelper(this, inheritance, workspace, classProvider, typeWriters, poolManager);
			AbstractMethodCallWriter interfaceMethodResolver = obfuscateReferencesBuilder.genInterfaceMethodResolver(this, methodResolverHelper, inheritance, workspace, classProvider, typeWriters, poolManager);
			methodResolverMethodRef.set(obfuscateReferencesBuilder.genMethodResolver(useBaseClass, this, indexResolver, stringArrayFieldRef, objectArrayFieldRef, classResolverMethod, methodResolverHelper, interfaceMethodResolver, inheritance, workspace, classProvider, typeWriters, keyLongNode, poolManager));
			if (hasIndyMembers) {
				AbstractMethodCallWriter methodHandleResolver = obfuscateReferencesBuilder.genMethodHandleResolver(useBaseClass, this, memberNameSuffixes, classResolverMethodRef.get(), methodResolverMethodRef.get(), inheritance, workspace, classProvider, typeWriters, poolManager);
				AbstractMethodCallWriter indyInvokeTargetMethod = obfuscateReferencesBuilder.genIndyInvokeTargetMethod(useBaseClass, this, methodHandleResolver, inheritance, workspace, classProvider, typeWriters, poolManager);
				AbstractMethodCallWriter indyBootstrapMethod = obfuscateReferencesBuilder.genIndyBootstrapMethod(useBaseClass, this, indyInvokeTargetMethod, inheritance, workspace, classProvider, typeWriters, poolManager);
				indyBootstrapMethodRef.set(indyBootstrapMethod);
			}

			if (hasNonIndyMembers && hasMethodReferences) {
				obfuscateReferencesBuilder.genArgumentArrayResolvers(useBaseClass, this, methodCallWriters, memberIds, inheritance, memberContextsByClass, memberContexts, workspace, classProvider, typeWriters, poolManager);
			}
		}
	}

	public AbstractMethodCallWriter getStringDecodeMethod() {
		return this.stringDecodeMethod;
	}

	public String getInterfacesDeclaration() {
		String declaration;
		if (super.interfacesCount == 0) {
			declaration = "";
		} else {
			declaration = "implements ";

			for (int i = 0; i < super.interfacesCount; i++) {
				declaration = declaration + super.interfaceWriters[i].getExternalName();
				declaration = declaration + (i < super.interfacesCount - 1 ? ", " : " ");
			}
		}

		return declaration;
	}

	public void prunePermittedSubclasses(TrimExcludes trimExcludes) {
		int remainingSubclassCount = 0;
		ArrayList<AttributesNode> retainedAttributes = new ArrayList<>(this.attributesCount);
		PermittedSubclassesAttribute permittedSubclassesAttribute = null;

		for (int i = 0; i < this.attributesCount; i++) {
			if (this.attributes[i] instanceof PermittedSubclassesAttribute) {
				permittedSubclassesAttribute = (PermittedSubclassesAttribute)this.attributes[i];
				remainingSubclassCount = permittedSubclassesAttribute.trimExcludedSubclasses(trimExcludes);
			} else {
				retainedAttributes.add(this.attributes[i]);
			}
		}

		if (remainingSubclassCount == 0 && permittedSubclassesAttribute != null) {
			int retainedAttributeCount = retainedAttributes.size();
			this.attributes = retainedAttributes.toArray(new AttributesNode[retainedAttributeCount]);
			this.attributesCount = retainedAttributeCount;
			super.isSealed = false;
		}
	}

	public void pruneNestHostAttribute(TrimExcludes trimExcludes) {
		boolean nestHostRetained = true;
		ArrayList<AttributesNode> retainedAttributes = new ArrayList<>(this.attributesCount);

		for (int i = 0; i < this.attributesCount; i++) {
			if (this.attributes[i] instanceof NestHostAttribute) {
				ClassNode hostClassNode = ClassHierarchyNode.getWorkspaceClassNode(((NestHostAttribute)this.attributes[i]).getHostClassInternalName());
				if (hostClassNode != null && trimExcludes.isClassOrInnerClassExcluded(hostClassNode)) {
					break;
				}

				nestHostRetained = false;
			} else {
				retainedAttributes.add(this.attributes[i]);
			}
		}

		if (!nestHostRetained) {
			int retainedAttributeCount = retainedAttributes.size();
			this.attributes = retainedAttributes.toArray(new AttributesNode[retainedAttributeCount]);
			this.attributesCount = retainedAttributeCount;
		}
	}

	public void pruneUnusedReferences(TrimExcludes trimExcludes) {
		if (this.fieldReferenceGraph != null) {
			ReferenceGraph<AbstractFieldNode, AbstractClassNode> fieldReferenceGraphClone = this.fieldReferenceGraph.deepClone();
			Enumeration<AbstractFieldNode> fieldEnumeration = fieldReferenceGraphClone.getMembersAsEnumeration();

			while (fieldEnumeration.hasMoreElements()) {
				AbstractFieldNode field = fieldEnumeration.nextElement();
				if (field.isWorkspaceNode() && !trimExcludes.isFieldExcluded((FieldNode)field)) {
					this.fieldReferenceGraph.removeMember(field);
				} else {
					for (AbstractClassNode fieldReferencingClass : fieldReferenceGraphClone.getReferences(field)) {
						if (fieldReferencingClass.isWorkspaceNode() && !trimExcludes.isClassExcluded((ClassNode)fieldReferencingClass)) {
							this.fieldReferenceGraph.removeReference(field, fieldReferencingClass);
						}
					}
				}
			}
		}

		if (this.methodReferenceGraph != null) {
			ReferenceGraph<AbstractMethodNode, AbstractClassNode> methodReferenceGraphClone = this.methodReferenceGraph.deepClone();
			Enumeration<AbstractMethodNode> methodEnumeration = methodReferenceGraphClone.getMembersAsEnumeration();

			while (methodEnumeration.hasMoreElements()) {
				AbstractMethodNode method = methodEnumeration.nextElement();
				if (method.isWorkspaceNode() && !trimExcludes.isMethodExcluded((MethodNode)method)) {
					this.methodReferenceGraph.removeMember(method);
				} else {
					for (AbstractClassNode methodReferencingClass : methodReferenceGraphClone.getReferences(method)) {
						if (methodReferencingClass.isWorkspaceNode() && !trimExcludes.isClassExcluded((ClassNode)methodReferencingClass)) {
							this.methodReferenceGraph.removeReference(method, methodReferencingClass);
						}
					}
				}
			}
		}

		HashSet<BootstrapMethodEntry> bootstrapMethods = GlobalUtils.newHashSet();
		MethodNode[] methodArray = this.methods;
		int methodArrayLength = methodArray.length;

		for (int i = 0; i < methodArrayLength; i++) {
			methodArray[i].collectBootstrapMethods(bootstrapMethods);
		}

		RingQueue<BootstrapMethodEntry> bootstrapMethodQueue = new RingQueue<>();
		bootstrapMethodQueue.addAll(bootstrapMethods);

		while (!bootstrapMethodQueue.isEmpty()) {
			for (BootstrapMethodEntry dependency : bootstrapMethodQueue.pop().getDynamicConstantRefs()) {
				if (bootstrapMethods.add(dependency)) {
					bootstrapMethodQueue.push(dependency);
				}
			}
		}

		this.filterBootstrapMethods(bootstrapMethods);
		ConstantPoolVisitor constantPoolVisitor = new ConstantPoolVisitor(this.constantPoolManager.getConstantPoolSize());
		this.acceptVisitor(constantPoolVisitor);
		this.constantPoolManager.filterUnusedConstants(constantPoolVisitor);
	}

	public void trimNestMembersAttribute(TrimExcludes trimExcludes) {
		int remainingNestMemberCount = 0;
		ArrayList<AttributesNode> retainedAttributes = new ArrayList<>(this.attributesCount);
		NestMembersAttribute nestMembersAttribute = null;

		for (int i = 0; i < this.attributesCount; i++) {
			if (this.attributes[i] instanceof NestMembersAttribute) {
				nestMembersAttribute = (NestMembersAttribute)this.attributes[i];
				remainingNestMemberCount = nestMembersAttribute.trimExcludedNestMembers(trimExcludes);
			} else {
				retainedAttributes.add(this.attributes[i]);
			}
		}

		if (remainingNestMemberCount == 0 && nestMembersAttribute != null) {
			int retainedAttributeCount = retainedAttributes.size();
			this.attributes = retainedAttributes.toArray(new AttributesNode[retainedAttributeCount]);
			this.attributesCount = retainedAttributeCount;
		}
	}

	public void pruneExcludedInnerClasses(TrimExcludes trimExcludes) {
		int remainingInnerClassCount = 0;
		ArrayList<AttributesNode> retainedAttributes = new ArrayList<>(this.attributesCount);
		InnerClassesAttribute innerClassesAttribute = null;

		for (int i = 0; i < this.attributesCount; i++) {
			if (this.attributes[i] instanceof InnerClassesAttribute) {
				innerClassesAttribute = (InnerClassesAttribute)this.attributes[i];
				remainingInnerClassCount = innerClassesAttribute.removeNonExcludedEntries(trimExcludes);
			} else {
				retainedAttributes.add(this.attributes[i]);
			}
		}

		if (remainingInnerClassCount == 0 && innerClassesAttribute != null) {
			int retainedAttributeCount = retainedAttributes.size();
			this.attributes = retainedAttributes.toArray(new AttributesNode[retainedAttributeCount]);
			this.attributesCount = retainedAttributeCount;
			super.hasValidInnerClasses = false;
			super.hasInvalidInnerClasses = false;
			this.clearInnerClassInfo();
		}
	}

	private void populateStringEntryMappings(
		KeyValuePairMultiMap<MethodNode, PriorityEntry<WritableType>, StringEntry> lookupEntriesByMethod,
		Map<AtomicReference<String>, StringEntry> entriesByReference,
		KeyValuePairMultiMap<MethodNode, StringNode, PriorityEntry<WritableType>> constantEntries,
		ReferenceGraph<AtomicReference<String>, StringNode> referenceGraph,
		Map<StringNode, AtomicReference<String>> referenceHandles,
		FieldTypeWriter arrayField,
		boolean multipleConstants,
		boolean parametersChanged,
		InvokeDynamicCallWriter indyCallWriter,
		AbstractMethodCallWriter decryptMethodWriter,
		AtomicReference<String>[] valueReferences,
		LocalVariableIndexCounter localVariableIndexCounter
	) {
		int valueReferenceIndex = 0;
		Iterator<Entry<AtomicReference<String>, Set<StringNode>>> adjacencyEntryIterator = referenceGraph.getAdjacencyEntries().iterator();

		while (adjacencyEntryIterator.hasNext()) {
			AtomicReference<String> valueReference = adjacencyEntryIterator.next().getKey();
			String stringValue = valueReference.get();
			if (entriesByReference.get(valueReference) == null) {
				if (arrayField != null) {
					StringEntry stringEntry;
					if (multipleConstants) {
						valueReferences[valueReferenceIndex] = valueReference;
						if (indyCallWriter != null) {
							stringEntry = new StringEntry(indyCallWriter, valueReferenceIndex++);
						} else if (decryptMethodWriter != null) {
							if (parametersChanged) {
								stringEntry = new StringEntry(valueReferenceIndex++, decryptMethodWriter);
							} else {
								stringEntry = new StringEntry(decryptMethodWriter, valueReferenceIndex++);
							}
						} else {
							stringEntry = new StringEntry(arrayField, valueReferenceIndex++);
						}
					} else {
						stringEntry = new StringEntry(arrayField, -1);
					}

					entriesByReference.put(valueReference, stringEntry);
				} else {
					if (multipleConstants) {
						valueReferences[valueReferenceIndex] = valueReference;
					}

					StringEntry indexedStringEntry = new StringEntry(localVariableIndexCounter.get(), multipleConstants ? valueReferenceIndex++ : -1);
					entriesByReference.put(valueReference, indexedStringEntry);
				}
			}
		}

		for (Entry<MethodNode, List<KeyValuePair<StringNode, PriorityEntry<WritableType>>>> methodStringNodeEntry : constantEntries.entrySet()) {
			MethodNode methodNode = methodStringNodeEntry.getKey();

			for (KeyValuePair<StringNode, PriorityEntry<WritableType>> stringNodePair : methodStringNodeEntry.getValue()) {
				StringNode stringNode = stringNodePair.getKey();
				PriorityEntry<WritableType> priorityEntry = stringNodePair.getValue();
				AtomicReference<String> referenceHandle = referenceHandles.get(stringNode);
				StringEntry stringEntryByReference = entriesByReference.get(referenceHandle);
				lookupEntriesByMethod.put(methodNode, priorityEntry, stringEntryByReference);
			}
		}
	}

	public ArrayEnumeration<FieldNode> getFieldEnumeration() {
		return new ArrayEnumeration<>(this.fields);
	}

	public void applyLocalVarTablePolicy(boolean trimNonPublic, boolean trimRenamed) {
		boolean classRenamed = false;
		if (trimRenamed && this.isNameChanged()) {
			classRenamed = true;
		}

		if (classRenamed) {
			this.removeMethodLocalVariables();
		} else {
			for (int i = 0; i < this.methodCount; i++) {
				this.methods[i].processLocalVariableTables(trimNonPublic, trimRenamed);
			}
		}
	}

	private MethodNode buildStringReferenceGraph(
		KeyValuePairMultiMap<MethodNode, StringNode, PriorityEntry<WritableType>> constantEntries,
		Set<StringNode> methodStringNodes,
		Set<StringNode> clinitStringNodes,
		List<KeyValuePair<StringNode, FieldNode>> stringFieldPairs,
		ReferenceGraph<AtomicReference<String>, StringNode> referenceGraph,
		Map<StringNode, AtomicReference<String>> referenceHandles,
		boolean useDoubleArrayStorage
	) {
		MethodNode clinitMethod = null;
		HashMap<String, AtomicReference<String>> referenceHandlesByStringValue = GlobalUtils.newHashMap();
		HashMap<String, AtomicReference<String>> doubleArrayHandlesByStringValue = GlobalUtils.newHashMap();
		ArrayList<StringNode> currentMethodStringNodes = new ArrayList<>();

		for (Entry<MethodNode, List<KeyValuePair<StringNode, PriorityEntry<WritableType>>>> methodEntry : constantEntries.entrySet()) {
			MethodNode method = methodEntry.getKey();
			List<KeyValuePair<StringNode, PriorityEntry<WritableType>>> stringEntries = methodEntry.getValue();
			currentMethodStringNodes.clear();

			for (KeyValuePair<StringNode, PriorityEntry<WritableType>> stringEntry : stringEntries) {
				currentMethodStringNodes.add(stringEntry.getKey());
			}

			if (method.isClinit()) {
				clinitMethod = method;
				clinitStringNodes.addAll(currentMethodStringNodes);
			} else {
				methodStringNodes.addAll(currentMethodStringNodes);
			}

			for (StringNode methodStringNode : currentMethodStringNodes) {
				String methodStringValue = methodStringNode.getValueAsString();
				AtomicReference<String> referenceHandle = referenceHandlesByStringValue.get(methodStringValue);
				if (referenceHandle == null) {
					referenceHandle = new AtomicReference<>(methodStringValue);
					referenceHandlesByStringValue.put(methodStringValue, new AtomicReference<>(methodStringValue));
				}

				referenceGraph.addReference(referenceHandle, methodStringNode);
				referenceHandles.put(methodStringNode, referenceHandle);
			}
		}

		Iterator<KeyValuePair<StringNode, FieldNode>> stringFieldPairIterator = stringFieldPairs.iterator();

		while (stringFieldPairIterator.hasNext()) {
			StringNode pairedStringNode = stringFieldPairIterator.next().getKey();
			String fieldStringValue = pairedStringNode.getValueAsString();
			AtomicReference<String> fieldReferenceHandle;
			if (useDoubleArrayStorage) {
				fieldReferenceHandle = doubleArrayHandlesByStringValue.get(fieldStringValue);
				if (fieldReferenceHandle == null) {
					fieldReferenceHandle = new AtomicReference<>(fieldStringValue);
					doubleArrayHandlesByStringValue.put(fieldStringValue, fieldReferenceHandle);
				}
			} else {
				fieldReferenceHandle = referenceHandlesByStringValue.get(fieldStringValue);
				if (fieldReferenceHandle == null) {
					fieldReferenceHandle = new AtomicReference<>(fieldStringValue);
					referenceHandlesByStringValue.put(fieldStringValue, new AtomicReference<>(fieldStringValue));
				}
			}

			referenceGraph.addReference(fieldReferenceHandle, pairedStringNode);
			referenceHandles.put(pairedStringNode, fieldReferenceHandle);
		}

		return clinitMethod;
	}

	public List<FieldNode> getSerialPersistentFields(AbstractWorkspace workspace) {
		ArrayList<FieldNode> serialPersistentFields = new ArrayList<>();
		if (!workspace.isResourceRegistered(this.getInternalName(), "serialPersistentFields")) {
			return serialPersistentFields;
		} else {
			MethodNode staticInitializer = workspace.getMethodBySignature(this, MethodSignature.STATIC_INITIALIZER);
			if (staticInitializer == null) {
				throw new ProcessingException("No '<clinit>' found.");
			} else {
				staticInitializer.collectSerialPersistentFields(serialPersistentFields, workspace);
				return serialPersistentFields;
			}
		}
	}

	@Override
	public AbstractMethodNode findMethod(MethodSignature methodSignature) {
		return this.findMethodNode(methodSignature);
	}

	public void setStringDecodeMethod(AbstractMethodCallWriter methodCallWriter) {
		this.stringDecodeMethod = methodCallWriter;
	}

	public AbstractClassNode getAnyReferencingClass(AbstractFieldNode fieldNode) {
		if (this.fieldReferenceGraph == null) {
			return null;
		} else {
			Set<AbstractClassNode> referencingClasses = this.fieldReferenceGraph.getReferences(fieldNode);
			return referencingClasses != null ? referencingClasses.iterator().next() : null;
		}
	}

	public void resolveMethodTargets(Set<ClassNode> reflectionClasses, Map<AbstractMethodCallWriter, ReflectionMethodPattern> reflectionPatterns, AbstractWorkspace workspace, AbstractLogger logger) {
		this.constantPoolManager.resolveMethodTargets(reflectionClasses, reflectionPatterns, workspace, logger);
	}

	public void renameFieldsWithHierarchy(
		AbstractFieldNameGenerator dictionaryGenerator,
		DualKeyMap<String, FieldSignature, FieldSignature> oldToNewSignatures,
		DualKeyMap<String, FieldSignature, FieldSignature> newToOldSignatures,
		Map<String, FieldNode> groupFieldNames,
		MultiMap<String, String> existingNamesByDescriptor,
		MultiMap<String, String> assignedNamesByDescriptor,
		DefaultExcludes defaultExcludes,
		ChangeLogContext changeLogContext,
		HashMap<String, String> mappedClassNames,
		Map<AbstractFieldNode, String> changeLogFieldNames,
		HashMap<AbstractFieldNode, String> resolvedFieldNames,
		ReferenceGraph<AbstractClassNode, FieldSignature> referenceGraph,
		DualKeyMap<AbstractClassNode, FieldNode, AbstractClassNode> referencingClassByField,
		Map<FieldNode, String> accessorFieldNames,
		AtomicBool hasRenamedFields,
		ClassHierarchy classHierarchy,
		boolean aggressiveRenaming
	) {
		Map<FieldNode, String> fieldStringMap = null;
		HashMap<String, FieldNode> fieldNodesByName = GlobalUtils.newHashMap();
		Enumeration<AbstractFieldNode> referencedFields = this.getReferencedFieldNodes();
		if (referencedFields != null) {
			while (referencedFields.hasMoreElements()) {
				AbstractFieldNode abstractFieldNode = referencedFields.nextElement();
				if (abstractFieldNode.isWorkspaceNode() && !resolvedFieldNames.containsKey(abstractFieldNode)) {
					AbstractClassNode fieldOwnerClass = abstractFieldNode.getClassNode();
					if (!fieldOwnerClass.isInterface() || !classHierarchy.isSubclassOf(fieldOwnerClass.getInternalName(), this.getInternalName())) {
						hasRenamedFields.set(true);
						AbstractClassNode referenceOwnerClass = abstractFieldNode.getClassNode();
						AbstractClassNode referencingClass = this.getAnyReferencingClass(abstractFieldNode);

						for (int i = 0; i < this.fieldsCount; i++) {
							referencingClassByField.put(referenceOwnerClass, this.fields[i], referencingClass);
						}
					}
				}
			}
		}

		for (int fieldIndex = 0; fieldIndex < this.fieldsCount; fieldIndex++) {
			String fieldName = this.fields[fieldIndex].getNameDisplay();
			FieldSignature currentFieldSignature = this.fields[fieldIndex].getFieldSignature();
			if (changeLogFieldNames != null && changeLogFieldNames.containsKey(this.fields[fieldIndex])) {
				if (fieldStringMap == null) {
					fieldStringMap = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.fieldsCount));
				}

				String newFieldName = changeLogFieldNames.get(this.fields[fieldIndex]);
				if (newFieldName.equals(fieldName)) {
					fieldNodesByName.put(fieldName, this.fields[fieldIndex]);
					groupFieldNames.put(fieldName, this.fields[fieldIndex]);
					assignedNamesByDescriptor.put(this.fields[fieldIndex].getStaticDescriptorKey(), fieldName);
					fieldStringMap.put(this.fields[fieldIndex], fieldName);
				} else {
					int renameAllowed = 1;
					Enumeration<AbstractFieldNode> symbolicReferenceFields = this.getReferencedFieldNodes();
					if (symbolicReferenceFields != null) {
						while (symbolicReferenceFields.hasMoreElements()) {
							AbstractFieldNode referencedField = symbolicReferenceFields.nextElement();
							AbstractClassNode symbolicRefOwnerClass = referencedField.getClassNode();
							AbstractClassNode symbolicReferenceClass = this.getAnyReferencingClass(referencedField);
							String resolvedSymbolicFieldName = resolvedFieldNames.get(referencedField);
							if (resolvedSymbolicFieldName != null && resolvedSymbolicFieldName.equals(newFieldName)) {
								renameAllowed = 0;
								String thisBinaryName = AbstractClassNode.internalToBinaryName(GlobalUtils.getMappedOrDefault(this.getInternalName(), mappedClassNames));
								String symbolicRefBinaryName = AbstractClassNode.internalToBinaryName(GlobalUtils.getMappedOrDefault(symbolicReferenceClass.getInternalName(), mappedClassNames));
								String ownerBinaryName = AbstractClassNode.internalToBinaryName(GlobalUtils.getMappedOrDefault(symbolicRefOwnerClass.getInternalName(), mappedClassNames));
								FieldSignature oldFieldSignature = newToOldSignatures.get(symbolicRefOwnerClass.getInternalName(), referencedField.getFieldSignature());
								String symbolicRefFieldName;
								if (oldFieldSignature != null) {
									symbolicRefFieldName = oldFieldSignature.getName();
								} else {
									symbolicRefFieldName = newFieldName;
								}

								changeLogContext.logWarning(
									"Field '"
										+ fieldName
										+ "' in class '"
										+ thisBinaryName
										+ "' could not be renamed to '"
										+ newFieldName
										+ "' because symbolic references to field '"
										+ ownerBinaryName
										+ "."
										+ symbolicRefFieldName
										+ "' appear as '"
										+ symbolicRefBinaryName
										+ "."
										+ symbolicRefFieldName
										+ "' and '"
										+ ownerBinaryName
										+ "."
										+ symbolicRefFieldName
										+ "' is renamed to '"
										+ newFieldName
										+ "'. You may need to distribute this application as a whole. (G)"
								);
								changeLogContext.removeProcessedFieldMapping(this.getInternalName(), currentFieldSignature);
								String discardedNewName = changeLogFieldNames.remove(this.fields[fieldIndex]);
								break;
							}
						}
					}

					if (renameAllowed != 0) {
						Map<FieldNode, AbstractClassNode> referencingClassesByField = referencingClassByField.getGroup(this);
						if (referencingClassesByField != null) {
							for (Entry<FieldNode, AbstractClassNode> entry : referencingClassesByField.entrySet()) {
								FieldNode fieldNode = entry.getKey();
								ClassNode maskingOwnerClass = fieldNode.getOwnerClass();
								String resolvedOwnerFieldName = resolvedFieldNames.get(fieldNode);
								if (resolvedOwnerFieldName != null && resolvedOwnerFieldName.equals(newFieldName)) {
									renameAllowed = 0;
									String thisBinaryNameForMaskCheck = AbstractClassNode.internalToBinaryName(GlobalUtils.getMappedOrDefault(this.getInternalName(), mappedClassNames));
									String ownerInternalName = maskingOwnerClass.getInternalName();
									String maskingOwnerBinaryName = AbstractClassNode.internalToBinaryName(GlobalUtils.getMappedOrDefault(ownerInternalName, mappedClassNames));
									FieldSignature maskingOldFieldSignature = newToOldSignatures.get(ownerInternalName, fieldNode.getFieldSignature());
									String maskingFieldName;
									if (maskingOldFieldSignature != null) {
										maskingFieldName = maskingOldFieldSignature.getName();
									} else {
										maskingFieldName = newFieldName;
									}

									changeLogContext.logWarning(
										"Field '"
											+ fieldName
											+ "' in class '"
											+ thisBinaryNameForMaskCheck
											+ "' could not be renamed to '"
											+ newFieldName
											+ "' because symbolic references to it appearing as '"
											+ AbstractClassNode.internalToBinaryName(GlobalUtils.getMappedOrDefault(entry.getValue().getInternalName(), mappedClassNames))
											+ "."
											+ fieldName
											+ "' could be masked by references to field '"
											+ maskingOwnerBinaryName
											+ "."
											+ maskingFieldName
											+ "' which is renamed to '"
											+ newFieldName
											+ "'. You may need to distribute this application as a whole. (H)"
									);
									changeLogContext.removeProcessedFieldMapping(this.getInternalName(), currentFieldSignature);
									String discardedMaskedNewName = changeLogFieldNames.remove(this.fields[fieldIndex]);
									break;
								}
							}
						}
					}

					if (renameAllowed != 0) {
						fieldNodesByName.put(newFieldName, this.fields[fieldIndex]);
						groupFieldNames.put(newFieldName, this.fields[fieldIndex]);
						assignedNamesByDescriptor.put(this.fields[fieldIndex].getStaticDescriptorKey(), newFieldName);
						fieldStringMap.put(this.fields[fieldIndex], newFieldName);
					}
				}
			} else if (defaultExcludes.isFieldExcluded(this.fields[fieldIndex])) {
				fieldNodesByName.put(fieldName, this.fields[fieldIndex]);
				groupFieldNames.put(fieldName, this.fields[fieldIndex]);
				assignedNamesByDescriptor.put(this.fields[fieldIndex].getStaticDescriptorKey(), fieldName);
			}
		}

		ClassHierarchyNode classHierarchyNode = ClassHierarchyNode.getNodeByInternalName(this.getInternalName());
		ArrayList<AbstractFieldNode> shuffledFields = new ArrayList<>(this.fieldsCount);

		for (int listFillIndex = 0; listFillIndex < this.fieldsCount; listFillIndex++) {
			shuffledFields.add(this.fields[listFillIndex]);
		}

		if (aggressiveRenaming || Options.ZKM_RANDOMIZE_OBFUSCATION) {
			GlobalUtils.shuffle(shuffledFields, GlobalUtils.newRandom(256));
		}

		for (int shuffledFieldIndex = 0; shuffledFieldIndex < this.fieldsCount; shuffledFieldIndex++) {
			((FieldNode)shuffledFields.get(shuffledFieldIndex))
				.resolveAndApplyFieldRename(
					classHierarchyNode,
					dictionaryGenerator,
					oldToNewSignatures,
					newToOldSignatures,
					groupFieldNames,
					existingNamesByDescriptor,
					assignedNamesByDescriptor,
					fieldNodesByName,
					fieldStringMap,
					this.getInternalName(),
					defaultExcludes,
					changeLogFieldNames,
					resolvedFieldNames,
					mappedClassNames,
					referenceGraph,
					referencingClassByField,
					accessorFieldNames
				);
		}
	}

	private void emitSingleIntegerInit(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		FieldTypeWriter arrayField,
		int constantValue,
		LabelNode switchLabel,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> labelCounters,
		AtomicInt labelCounter,
		Map<FieldTypeWriter, AtomicReference<Integer>> referencesByField,
		IntegerEntry entry,
		IntegerEncryptionBuilder integerEncryptionBuilder,
		MethodNode methodNode,
		LocalVariableIndexCounter localVariableIndexCounter,
		AtomicReference<Integer> desInitSlotRef,
		Long encryptionKey,
		LocalVariableOperand localVariableOperand,
		Map<MethodNode, LocalVariableOperand> operandsByMethod,
		AtomicReference<Integer> globalKeySlotRef,
		boolean useDesEncryption,
		ParameterChangerBuilder parameterChangerBuilder,
		List<TypeWriter> typeWriters,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		if (encryptionKey != null) {
			if (localVariableOperand == null) {
				localVariableOperand = parameterChangerBuilder.emitSaltEncodedLongLocal(encryptionKey, localVariableIndexCounter, instructions, methodNode, operandsByMethod, this.getConstantPoolManager(), typeWriters);
			}

			if (useDesEncryption) {
				int encryptionKeyLocalIndex = localVariableIndexCounter.getAndIncrease();
				integerEncryptionBuilder.generateDesInitCode(localVariableTable, instructions, encryptionKeyLocalIndex, encryptionKey, localVariableOperand, localVariableIndexCounter, typeWriters, workspace, classProvider);
				desInitSlotRef.set(INTEGER_CACHE.valueOf(encryptionKeyLocalIndex));
			}
		} else if (integerEncryptionBuilder.hasGlobalKey()) {
			AbstractInsnNode.appendLongConst(integerEncryptionBuilder.getGlobalKey(), instructions, this.constantPoolManager, typeWriters);
			int globalKeyLocalIndex = localVariableIndexCounter.get();
			localVariableIndexCounter.add(2);
			instructions.add(AbstractInsnNode.storeLong(globalKeyLocalIndex, localVariableTable, 11));
			globalKeySlotRef.set(INTEGER_CACHE.valueOf(globalKeyLocalIndex));
		}

		LongNode keyLongConstant = this.constantPoolManager.getOrCreateLong(-1L, typeWriters);
		long packedValue = integerEncryptionBuilder.packIntWithRandomPrefix(constantValue);
		long combinedKey;
		if (useDesEncryption) {
			combinedKey = integerEncryptionBuilder.combineWithKey(packedValue, encryptionKey);
		} else if (encryptionKey != null) {
			combinedKey = packedValue ^ encryptionKey;
		} else {
			combinedKey = packedValue ^ integerEncryptionBuilder.getGlobalKey();
		}

		keyLongConstant.setValue(combinedKey);
		integerEncryptionBuilder.generateStoreEncryptedLongCode(localVariableTable, instructions, arrayField, keyLongConstant, entry, switchLabel, labelCounter, labelCounters);
		if (!referencesByField.isEmpty()) {
			ArrayList<AbstractInsnNode> storeInstructions = new ArrayList<>();
			if (arrayField != null) {
				storeInstructions.add(new WideConstantPoolInsnNode(GETSTATIC, arrayField));
			} else if (entry.hasIndex()) {
				storeInstructions.add(AbstractInsnNode.loadObject(entry.getLocalArrayIndex(), localVariableTable, 11));
				storeInstructions.add(AbstractInsnNode.loadInt(entry.getIndex(), localVariableTable, 11));
				storeInstructions.add(InsnNode.of(LALOAD));
			} else {
				storeInstructions.add(AbstractInsnNode.loadLong(entry.getLocalArrayIndex(), localVariableTable, 11));
			}

			Iterator<Entry<FieldTypeWriter, AtomicReference<Integer>>> fieldEntryIterator = referencesByField.entrySet().iterator();

			while (fieldEntryIterator.hasNext()) {
				FieldTypeWriter fieldTypeWriter = fieldEntryIterator.next().getKey();
				if (arrayField == null) {
					integerEncryptionBuilder.generateStoreEncryptedIntegerCode(localVariableTable, storeInstructions, fieldTypeWriter, entry, encryptionKey, localVariableOperand, typeWriters);
				}
			}

			storeInstructions.add(InsnNode.of(POP2));
			instructions.addAll(storeInstructions);
		}
	}

	private String generateUniqueFieldName(List<String> overrideNames, String fieldDescriptor, Inheritance inheritance, String namePrefix) {
		ArrayList<String> remainingNames = overrideNames != null ? new ArrayList<>(overrideNames) : null;
		AlphaNameGenerator nameGenerator = new AlphaNameGenerator(0, namePrefix);
		int nameIndex = 0;
		HashSet<FieldSignature> seenFieldSignatures = null;
		HashSet<String> seenReferencedFieldNames = null;
		Set<String> inheritedFieldNames = inheritance.getFieldNames(this);

		String fieldName;
		boolean clashDetected;
		do {
			clashDetected = false;

			do {
				if (remainingNames != null && !remainingNames.isEmpty()) {
					fieldName = remainingNames.remove(0);
				} else {
					fieldName = nameGenerator.generateNameOrNull(nameIndex++);
				}
			} while (fieldName == null);

			FieldSignature candidateSignature = new FieldSignature(fieldName, fieldDescriptor);
			if (inheritedFieldNames.contains(fieldName)) {
				clashDetected = true;
			} else {
				if (seenFieldSignatures == null) {
					seenFieldSignatures = GlobalUtils.newHashSet();

					for (int i = 0; i < this.fieldsCount; i++) {
						FieldSignature existingFieldSignature = new FieldSignature(this.fields[i].getOriginalName(), this.fields[i].getDescriptor());
						seenFieldSignatures.add(existingFieldSignature);
					}
				}

				if (seenFieldSignatures.contains(candidateSignature)) {
					clashDetected = true;
				}
			}

			if (!clashDetected && this.fieldReferenceGraph != null) {
				if (seenReferencedFieldNames == null) {
					seenReferencedFieldNames = GlobalUtils.newHashSet();
					Iterator<AbstractFieldNode> memberIterator = this.fieldReferenceGraph.getMembers().iterator();

					while (memberIterator.hasNext()) {
						String referencedFieldName = memberIterator.next().getNameDisplay();
						seenReferencedFieldNames.add(referencedFieldName);
					}
				}

				if (seenReferencedFieldNames.contains(fieldName)) {
					clashDetected = true;
				}
			}
		} while (clashDetected);

		return fieldName;
	}

	public MethodNode getOrCreateStaticInitializer(List<TypeWriter> typeWriters, AbstractWorkspace workspace, String changeLogName, int generationType) {
		MethodNode staticInitializer = workspace.getMethodBySignature(this, MethodSignature.STATIC_INITIALIZER);
		if (staticInitializer == null) {
			UTFStringWriter codeNameUtf8 = new UTFStringWriter(0, this.constantPoolManager, "Code");
			typeWriters.add(codeNameUtf8);
			LocalVariableTable localVariableTable = new LocalVariableTable(true, MethodSignature.STATIC_INITIALIZER.getFullDescriptor(), 0);
			ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
			instructions.add(InsnNode.of(RETURN));
			InstructionListNode instructionList = new InstructionListNode(instructions, localVariableTable, changeLogName);
			CodeAttribute codeAttribute = new CodeAttribute(codeNameUtf8, 0, 0, instructionList, new TryCatchBlockInfo[0]);
			UTFStringWriter methodNameUtf8 = new UTFStringWriter(0, this.constantPoolManager, MethodSignature.STATIC_INITIALIZER.getName());
			typeWriters.add(methodNameUtf8);
			UTFStringWriter methodDescriptorUtf8 = new UTFStringWriter(0, this.constantPoolManager, MethodSignature.STATIC_INITIALIZER.getFullDescriptor());
			typeWriters.add(methodDescriptorUtf8);
			AttributesNode[] methodAttributes = new AttributesNode[]{codeAttribute};
			staticInitializer = new MethodNode(this, methodNameUtf8, methodDescriptorUtf8, methodAttributes, generationType);
			staticInitializer.setDefaultAccess();
			staticInitializer.setStatic(true);
			this.addMethodNode(staticInitializer, workspace);
		}

		return staticInitializer;
	}

	public void validateMethodAnalyzers(TypeResolver typeResolver, ClassHierarchy classHierarchy, ScriptContext scriptContext) {
		MethodNode[] methodArray = this.methods;
		int methodArrayLength = methodArray.length;

		for (int i = 0; i < methodArrayLength; i++) {
			methodArray[i].validateMethodAnalyzer(typeResolver, classHierarchy, scriptContext);
		}
	}

	public FieldNode createStaticFieldIfAbsent(String fieldName, String descriptor, int accessLevel, boolean isFinal, Inheritance inheritance, AbstractWorkspace workspace, int generationType) {
		if (this.findFieldNode(new FieldSignature(fieldName, descriptor)) != null) {
			return null;
		} else {
			ArrayList<TypeWriter> typeWriters = new ArrayList<>(2);
			UTFStringWriter fieldNameUtf8 = new UTFStringWriter(0, this.constantPoolManager, fieldName);
			typeWriters.add(fieldNameUtf8);
			UTFStringWriter fieldDescriptorUtf8 = new UTFStringWriter(0, this.constantPoolManager, descriptor);
			typeWriters.add(fieldDescriptorUtf8);
			AttributesNode[] fieldAttributes;
			if (Options.ZKM_MARK_AS_SYNTHETIC) {
				UTFStringWriter syntheticNameUtf8 = new UTFStringWriter(0, this.constantPoolManager, "Synthetic");
				typeWriters.add(syntheticNameUtf8);
				SyntheticAttribute syntheticAttribute = new SyntheticAttribute(null, syntheticNameUtf8);
				fieldAttributes = new AttributesNode[]{syntheticAttribute};
			} else {
				fieldAttributes = new AttributesNode[0];
			}

			FieldNode fieldNode = new FieldNode(this, fieldNameUtf8, fieldDescriptorUtf8, fieldAttributes, generationType);
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
				case 3:
				default:
					break;
				case 4:
					fieldNode.setPublic();
			}

			this.addField(fieldNode, workspace);
			inheritance.registerField(fieldNode);
			this.constantPoolManager.appendEntriesToPool(typeWriters);
			this.markChanged();
			this.notify(new AtomicInt(4), this, null);
			return fieldNode;
		}
	}

	public boolean hasStringDecodeMethod() {
		return this.stringDecodeMethod != null;
	}

	private void prepareStringFieldRefs(
		List<KeyValuePair<StringNode, FieldNode>> stringFieldPairs,
		Map<StringNode, AtomicReference<String>> referenceHandles,
		Map<FieldTypeWriter, AtomicReference<String>> referencesByField,
		List<FieldNode> instanceFields,
		List<TypeWriter> typeWriters
	) {
		for (KeyValuePair<StringNode, FieldNode> stringFieldPair : stringFieldPairs) {
			StringNode stringNode = stringFieldPair.getKey();
			FieldNode fieldNode = stringFieldPair.getValue();
			fieldNode.removeConstantValueAttribute();
			FieldTypeWriter fieldTypeWriter = this.constantPoolManager.getOrCreateFieldRef(this.getInternalName(), fieldNode.getNameDisplay(), fieldNode.getDescriptor(), typeWriters, fieldNode);
			referencesByField.put(fieldTypeWriter, referenceHandles.get(stringNode));
		}

		for (FieldNode instanceField : instanceFields) {
			boolean writtenInConstructor = false;

			for (int i = 0; i < this.methodCount; i++) {
				MethodNode method = this.methods[i];
				if (method.isConstructor() && method.writesField(instanceField)) {
					writtenInConstructor = true;
					break;
				}
			}

			if (writtenInConstructor) {
				instanceField.removeConstantValueAttribute();
			}
		}
	}

	public void encryptIntegerConstants(
		Inheritance inheritance,
		IntegerEncryptionBuilder integerEncryptionBuilder,
		boolean unused,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		List<KeyValuePair<IntegerNode, FieldNode>> staticIntegerFieldPairs,
		List<KeyValuePair<IntegerNode, FieldNode>> instanceIntegerFieldPairs,
		KeyValuePairMultiMap<MethodNode, IntegerNode, PriorityEntry<WritableType>> constantEntries,
		IntegerEncryptionExcludes encryptionExcludes,
		int maxEncryptedConstants,
		ParameterChangerBuilder parameterChangerBuilder,
		Map<MethodNode, Long> encryptionKeysByMethod,
		Map<MethodNode, LocalVariableOperand> operandsByMethod,
		List<ClassNode> encryptedClasses,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy
	) {
		boolean isInterface = this.isInterface();
		if (encryptionExcludes == null || !encryptionExcludes.isClassExcluded(this)) {
			if (staticIntegerFieldPairs == null) {
				staticIntegerFieldPairs = new ArrayList<>(13);
			}

			if (instanceIntegerFieldPairs == null) {
				instanceIntegerFieldPairs = new ArrayList<>(0);
			}

			if (constantEntries == null) {
				constantEntries = new KeyValuePairMultiMap<>(13);
			}

			if (staticIntegerFieldPairs.size() != 0 || constantEntries.size() != 0) {
				if (maxEncryptedConstants < Integer.MAX_VALUE) {
					int integerEntryCount = constantEntries.valueCount();
					if (integerEntryCount > maxEncryptedConstants) {
						int excessIntegerCount = integerEntryCount - maxEncryptedConstants;
						int removedEntryCount = 0;
						Enumeration<MethodNode> methodEnumeration = constantEntries.keys();

						label220:
						while (methodEnumeration.hasMoreElements()) {
							MethodNode methodNode = methodEnumeration.nextElement();
							Iterator<KeyValuePair<IntegerNode, PriorityEntry<WritableType>>> integerEntryIterator = constantEntries.get(methodNode).iterator();

							while (integerEntryIterator.hasNext()) {
								KeyValuePair<IntegerNode, PriorityEntry<WritableType>> removedEntry = integerEntryIterator.next();
								integerEntryIterator.remove();
								if (++removedEntryCount >= excessIntegerCount) {
									break label220;
								}
							}
						}
					}
				}

				HashSet<IntegerNode> integerNodesInMethods = GlobalUtils.newHashSet();
				HashSet<IntegerNode> integerNodesInClinit = GlobalUtils.newHashSet();
				ReferenceGraph<AtomicReference<Integer>, IntegerNode> integerReferenceGraph = new ReferenceGraph<>();
				HashMap<IntegerNode, AtomicReference<Integer>> referenceByIntegerNode = GlobalUtils.newHashMap();
				MethodNode staticInitializer = this.buildIntegerReferenceGraph(constantEntries, integerNodesInMethods, integerNodesInClinit, staticIntegerFieldPairs, integerReferenceGraph, referenceByIntegerNode);
				if (staticInitializer == null) {
					if (!this.isInnerClass()) {
						staticInitializer = workspace.getMethodBySignature(this, MethodSignature.STATIC_INITIALIZER);
					} else {
						staticInitializer = this.findMethodNode(MethodSignature.STATIC_INITIALIZER);
					}
				}

				HashSet<IntegerNode> allIntegerNodes = GlobalUtils.newHashSet();
				ArrayList<IntegerNode> fieldIntegerNodes = new ArrayList<>(staticIntegerFieldPairs.size());

				for (KeyValuePair<IntegerNode, FieldNode> integerFieldPair : staticIntegerFieldPairs) {
					fieldIntegerNodes.add(integerFieldPair.getKey());
				}

				allIntegerNodes.addAll(fieldIntegerNodes);
				allIntegerNodes.addAll(integerNodesInMethods);
				allIntegerNodes.addAll(integerNodesInClinit);
				HashSet<IntegerNode> clinitAndFieldIntegerNodes = GlobalUtils.newHashSet(integerNodesInClinit);
				clinitAndFieldIntegerNodes.addAll(fieldIntegerNodes);
				int originalMaxLocals = staticInitializer == null ? 0 : staticInitializer.getMaxLocalsCompat();
				integerNodesInClinit.size();
				clinitAndFieldIntegerNodes.size();
				if (!isInterface || !integerNodesInMethods.isEmpty() || !fieldIntegerNodes.isEmpty()) {
					ArrayList<TypeWriter> typeWriters = new ArrayList<>();
					HashMap<FieldTypeWriter, AtomicReference<Integer>> fieldRefsByFieldType = GlobalUtils.newHashMap();
					this.mapIntegerFieldReferences(staticIntegerFieldPairs, referenceByIntegerNode, fieldRefsByFieldType, instanceIntegerFieldPairs, typeWriters);
					if (staticInitializer == null) {
						staticInitializer = this.getOrCreateStaticInitializer(typeWriters, workspace, "Integer Constant Encryption", 11);
					}

					LocalVariableIndexCounter localVariableIndexCounter = new LocalVariableIndexCounter(staticInitializer);
					LocalVariableTable localVariableTable = staticInitializer.getLocalVariableTable();
					FieldTypeWriter integerFieldRef = null;
					boolean multipleIntegers = integerReferenceGraph.size() > 1;
					boolean integersUsedInMethods = integerNodesInMethods.size() > 0;
					AtomicReference<Integer>[] integerReferences = null;
					if (multipleIntegers) {
						integerReferences = new AtomicReference[integerReferenceGraph.size()];
					}

					if (integersUsedInMethods && null == null) {
						String fieldDescriptor;
						if (integerReferenceGraph.size() == 1) {
							fieldDescriptor = "J";
						} else {
							fieldDescriptor = "[J";
						}

						FieldNode integerField = this.createUniqueField(fieldDescriptor, isInterface ? 4 : 1, true, inheritance, workspace, 1);
						integerFieldRef = this.constantPoolManager.getOrCreateFieldRef(this.getInternalName(), integerField.getNameDisplay(), integerField.getDescriptor(), typeWriters, workspace, classProvider, true);
					}

					if (multipleIntegers && integerReferences.length > 65535) {
						throw new ProcessingException(
							"Integer Constant Encryption can handle a maximumum of 65535 Integers. Number of Integers in class '"
								+ this.getRawSourcePath()
								+ "' is "
								+ integerReferences.length
								+ "."
						);
					}

					boolean integersEncryptableInMethod = parameterChangerBuilder != null && parameterChangerBuilder.shouldEncryptIntegersInMethod(staticInitializer);
					boolean indyCallSiteEnabled = integerFieldRef != null && integersEncryptableInMethod && IntegerEncryptionBuilder.isIndySupported(this);
					boolean parameterChangerPresent = parameterChangerBuilder != null;
					integerEncryptionBuilder.setupEncryption(this, inheritance, workspace, classProvider, typeWriters, integerFieldRef, parameterChangerPresent, multipleIntegers, indyCallSiteEnabled, integersEncryptableInMethod);
					if (encryptedClasses != null) {
						encryptedClasses.add(this);
					}

					KeyValuePairMultiMap<MethodNode, PriorityEntry<WritableType>, IntegerEntry> lookupEntriesByMethod = new KeyValuePairMultiMap<>();
					HashMap<AtomicReference<Integer>, IntegerEntry> integerEntriesByReference = GlobalUtils.newHashMap();
					this.buildIntegerLookupEntries(
						lookupEntriesByMethod, integerEntriesByReference, constantEntries, integerReferenceGraph, referenceByIntegerNode, integerFieldRef, integerEncryptionBuilder.getIndyCallSite(this), multipleIntegers, integersEncryptableInMethod, integerEncryptionBuilder.getDecryptMethod(this), integerReferences, localVariableIndexCounter
					);
					int integerArrayLocalIndex = -1;
					if (!integersUsedInMethods) {
						if (multipleIntegers) {
							integerArrayLocalIndex = localVariableIndexCounter.getAndIncrease();
						} else {
							integerArrayLocalIndex = localVariableIndexCounter.get();
							localVariableIndexCounter.add(2);
						}
					}

					HashMap<LabelNode, AtomicInt> caseIndexByLabel = GlobalUtils.newHashMap();
					MultiMap<LabelNode, PriorityEntry<LabelNode>> caseEntriesBySwitchLabel = new MultiMap<>();
					AtomicReference<AtomicInt> caseIndexReference = new AtomicReference<>();
					AtomicReference<LabelNode> switchLabelReference = new AtomicReference<>();
					LabelNode initialSwitchLabel = this.createSwitchLabelState(switchLabelReference, caseIndexReference, caseIndexByLabel);
					int switchBaseLocalIndex = localVariableIndexCounter.get();
					AtomicInt caseIndexCounter = new AtomicInt(-2);
					Long staticInitializerKey = null;
					LocalVariableOperand staticInitializerKeyOperand = null;
					if (parameterChangerBuilder != null) {
						staticInitializerKey = encryptionKeysByMethod != null ? encryptionKeysByMethod.get(staticInitializer) : null;
						staticInitializerKeyOperand = operandsByMethod != null ? operandsByMethod.get(staticInitializer) : null;
						if (staticInitializerKey == null) {
							staticInitializerKey = parameterChangerBuilder.getOrCreateMethodEncryptionKey(staticInitializer);
						}
					}

					AtomicReference<Integer> desKeyLocalIndex = new AtomicReference<>();
					AtomicReference<Integer> globalKeyLocalIndex = new AtomicReference<>();
					ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
					if (integerEncryptionBuilder.getThreadLocalMapField() != null) {
						integerEncryptionBuilder.initThreadLocalMapField(instructions, typeWriters, this.constantPoolManager, workspace, classProvider);
					}

					if (multipleIntegers) {
						this.emitMultiIntegerInit(
							localVariableTable,
							instructions,
							integerFieldRef,
							integerEncryptionBuilder.getEncryptedIntegersField(this),
							integerArrayLocalIndex,
							integerReferences,
							integerReferenceGraph,
							integerEntriesByReference,
							fieldRefsByFieldType,
							switchLabelReference,
							caseIndexCounter,
							caseEntriesBySwitchLabel,
							localVariableIndexCounter,
							typeWriters,
							integerEncryptionBuilder,
							staticInitializer,
							desKeyLocalIndex,
							staticInitializerKey,
							staticInitializerKeyOperand,
							operandsByMethod,
							globalKeyLocalIndex,
							integersEncryptableInMethod,
							parameterChangerBuilder,
							workspace,
							classProvider
						);
					} else {
						AtomicReference<Integer> integerReference = integerReferenceGraph.getMembersAsEnumeration().nextElement();
						this.emitSingleIntegerInit(
							localVariableTable,
							instructions,
							integerFieldRef,
							integerReferenceGraph.getAllReferences().nextElement().getValue(),
							switchLabelReference.get(),
							caseEntriesBySwitchLabel,
							caseIndexCounter,
							fieldRefsByFieldType,
							integerEntriesByReference.get(integerReference),
							integerEncryptionBuilder,
							staticInitializer,
							localVariableIndexCounter,
							desKeyLocalIndex,
							staticInitializerKey,
							staticInitializerKeyOperand,
							operandsByMethod,
							globalKeyLocalIndex,
							integersEncryptableInMethod,
							parameterChangerBuilder,
							typeWriters,
							workspace,
							classProvider
						);
					}

					staticInitializerKeyOperand = operandsByMethod != null ? operandsByMethod.get(staticInitializer) : null;
					Enumeration<MethodNode> lookupMethodEnumeration = lookupEntriesByMethod.keys();

					while (lookupMethodEnumeration.hasMoreElements()) {
						MethodNode lookupMethodNode = lookupMethodEnumeration.nextElement();
						List<KeyValuePair<PriorityEntry<WritableType>, IntegerEntry>> lookupEntries = lookupEntriesByMethod.get(lookupMethodNode);
						Object startingLocalVariableIndex;
						if (lookupMethodNode.isClinit()) {
							if (localVariableIndexCounter.get() > switchBaseLocalIndex) {
								startingLocalVariableIndex = new AtomicInt(switchBaseLocalIndex);
							} else {
								startingLocalVariableIndex = localVariableIndexCounter;
							}
						} else {
							startingLocalVariableIndex = new AtomicInt(lookupMethodNode.getMaxLocalsCompat());
						}

						Long methodKey = encryptionKeysByMethod != null ? encryptionKeysByMethod.get(lookupMethodNode) : null;
						LocalVariableOperand methodKeyOperand = operandsByMethod != null ? operandsByMethod.get(lookupMethodNode) : null;
						lookupMethodNode.encryptIntegerConstantsWithLong(lookupEntries, caseIndexByLabel, caseEntriesBySwitchLabel, methodKey, methodKeyOperand, (Indexable)startingLocalVariableIndex, this.constantPoolManager, typeWriters, typeResolver, classHierarchy);
					}

					integerEncryptionBuilder.generateEncryptedValueSwitch(
						localVariableTable, initialSwitchLabel, staticInitializerKey, staticInitializerKeyOperand, localVariableIndexCounter, instructions, typeWriters, caseEntriesBySwitchLabel, desKeyLocalIndex.get(), globalKeyLocalIndex.get(), this.constantPoolManager, workspace, classProvider
					);
					int newMaxLocals = localVariableTable.getSlotCount() - originalMaxLocals;
					ArrayList<Integer> localVariableIndices = new ArrayList<>(newMaxLocals);

					for (int slotIndex = originalMaxLocals; slotIndex < localVariableTable.getSlotCount(); slotIndex++) {
						localVariableIndices.add(slotIndex);
					}

					int insertionOffset = 0;
					LabelNode injectionAnchor = staticInitializer.getInjectionAnchorLabel();
					if (injectionAnchor != null) {
						insertionOffset = staticInitializer.indexOfInstruction(injectionAnchor);
					}

					LabelNode newInjectionAnchor = new LabelNode(4096);
					instructions.add(newInjectionAnchor);
					staticInitializer.setInjectionAnchorLabel(newInjectionAnchor);
					staticInitializer.insertInstructionsAtOffset(instructions, newMaxLocals, staticInitializer.getMaxStack(), insertionOffset, "Integer Constant Encryption", localVariableIndices);
					if (typeWriters.size() > 0) {
						this.constantPoolManager.appendEntriesToPool(typeWriters);
					}

					this.markChanged();
					this.notify(new AtomicInt(4), this, null);
				}
			}
		}
	}

	public MultiMap<Integer, Integer> remapAndScrambleLineNumbers(IntegerCache integerCache, MultiMap<Integer, Integer> lineNumbers, Random random) {
		ArrayList<LineNumberEntry> lineNumberTables = new ArrayList<>(Math.max(10, (int)(this.constantPoolManager.getConstantPoolSize() * 0.25)));

		for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
			this.methods[methodIndex].updateLineNumbers(lineNumberTables);
		}

		int tableCount = lineNumberTables.size();
		int maxLineNumber = 0;
		MultiMap<Integer, Integer> newLineNumbersByOriginal = new MultiMap<>(tableCount, 2);
		ArrayList<Integer> availableLineNumbers = new ArrayList<>();
		ArrayList<LineNumberEntry> tablesToScramble = new ArrayList<>(tableCount);
		if (lineNumbers != null) {
			int mappingCount = lineNumbers.size();
			MultiMap<Integer, Integer> candidatesByOriginalLine = new MultiMap<>(mappingCount);
			ArrayList<Integer> occupiedLineNumbers = new ArrayList<>(mappingCount);
			Enumeration<Integer> mappedLineEnumeration = lineNumbers.keys();

			while (mappedLineEnumeration.hasMoreElements()) {
				Integer mappedLine = mappedLineEnumeration.nextElement();
				List<Integer> mappedNewLines = lineNumbers.get(mappedLine);
				candidatesByOriginalLine.putAll(mappedLine, mappedNewLines);
				occupiedLineNumbers.addAll(mappedNewLines);
			}

			Collections.sort(occupiedLineNumbers);
			int maxOccupiedLine = occupiedLineNumbers.get(occupiedLineNumbers.size() - 1);
			Iterator<Integer> occupiedLineIterator = occupiedLineNumbers.iterator();
			int currentOccupiedLine = occupiedLineIterator.next();

			for (int candidateLine = 0; candidateLine < maxOccupiedLine; candidateLine++) {
				if (candidateLine < currentOccupiedLine) {
					availableLineNumbers.add(integerCache.valueOf(candidateLine));
				} else if (candidateLine == currentOccupiedLine && occupiedLineIterator.hasNext()) {
					currentOccupiedLine = occupiedLineIterator.next();
				}
			}

			for (int tableIndex = 0; tableIndex < lineNumberTables.size(); tableIndex++) {
				LineNumberEntry lineNumberTable = lineNumberTables.get(tableIndex);
				int tableLineNumber = lineNumberTable.getLineNumber();
				Integer originalLine = integerCache.valueOf(tableLineNumber);
				List<Integer> candidateLines = candidatesByOriginalLine.get(originalLine);
				if (candidateLines != null && candidateLines.size() > 0) {
					Integer newLine = candidateLines.get(0);
					lineNumberTable.setLineNumber(newLine);
					newLineNumbersByOriginal.put(originalLine, newLine);
					int assignedLine = newLine;
					if (assignedLine > maxLineNumber) {
						maxLineNumber = assignedLine;
					}

					candidatesByOriginalLine.remove(originalLine, newLine);
				} else {
					tablesToScramble.add(lineNumberTable);
				}
			}

			Enumeration<Integer> remainingLineNumbers = candidatesByOriginalLine.elements();

			while (remainingLineNumbers.hasMoreElements()) {
				availableLineNumbers.add(remainingLineNumbers.nextElement());
			}
		} else {
			tablesToScramble.addAll(lineNumberTables);
		}

		GlobalUtils.shuffle(tablesToScramble, random);
		Iterator<Integer> availableLineIterator = availableLineNumbers.iterator();

		for (LineNumberEntry tableToScramble : tablesToScramble) {
			int originalLineNumber = tableToScramble.getLineNumber();
			if (availableLineIterator.hasNext()) {
				Integer availableLine = availableLineIterator.next();
				int newLineNumber = availableLine;
				tableToScramble.setLineNumber(newLineNumber);
				newLineNumbersByOriginal.put(integerCache.valueOf(originalLineNumber), availableLine);
				if (newLineNumber > maxLineNumber) {
					maxLineNumber = newLineNumber;
				}
			} else {
				tableToScramble.setLineNumber(++maxLineNumber);
				newLineNumbersByOriginal.put(integerCache.valueOf(originalLineNumber), integerCache.valueOf(maxLineNumber));
			}
		}

		return newLineNumbersByOriginal;
	}

	public MethodNode findMethodNode(MethodSignature methodSignature) {
		return (MethodNode)super.findMethod(methodSignature);
	}

	public Enumeration<ObservableStructure> buildClassProperties() {
		this.eventRegistry.unsubscribeGroup("CLASS_PROPERTIES");
		classPropertyEntries = new Vector<>();
		ClassNameEntry nameEntry = new ClassNameEntry("name", this);
		this.eventRegistry.subscribe(this, nameEntry, "CLASS_PROPERTIES");
		classPropertyEntries.addElement(nameEntry);
		ClassMemberEntry fieldsEntry = new ClassMemberEntry("fields", this, this.fields, INTEGER_CACHE);
		this.eventRegistry.subscribe(this, fieldsEntry, "CLASS_PROPERTIES");
		classPropertyEntries.addElement(fieldsEntry);
		ClassMemberEntry methodsEntry = new ClassMemberEntry("methods", this, this.methods, INTEGER_CACHE);
		this.eventRegistry.subscribe(this, methodsEntry, "CLASS_PROPERTIES");
		classPropertyEntries.addElement(methodsEntry);
		ConstantWriter[] constantWriters = this.constantPoolManager.getAllConstantWriters();
		ClassConstantsEntry constantsEntry = new ClassConstantsEntry("constants", this, constantWriters, INTEGER_CACHE);
		this.eventRegistry.subscribe(this, constantsEntry, "CLASS_PROPERTIES");
		classPropertyEntries.addElement(constantsEntry);
		return classPropertyEntries.elements();
	}

	public void removeLineNumberMetadata() {
		if (this.hasLineNumberTable()) {
			for (int i = 0; i < this.methodCount; i++) {
				this.methods[i].removeLineNumberTables();
			}

			this.setHasLineNumberTable(false);
		}
	}

	@Override
	public AbstractFieldNode[] getFields() {
		return this.getFieldNodes();
	}

	private void emitSingleLongInit(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		FieldTypeWriter arrayField,
		long constantValue,
		LabelNode switchLabel,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> labelCounters,
		AtomicInt labelCounter,
		Map<FieldTypeWriter, AtomicReference<Long>> referencesByField,
		LongEntry entry,
		LongEncryptionBuilder longEncryptionBuilder,
		MethodNode methodNode,
		LocalVariableIndexCounter localVariableIndexCounter,
		AtomicReference<Integer> desInitSlotRef,
		Long encryptionKey,
		LocalVariableOperand localVariableOperand,
		Map<MethodNode, LocalVariableOperand> operandsByMethod,
		AtomicReference<Integer> globalKeySlotRef,
		boolean useDesEncryption,
		ParameterChangerBuilder parameterChangerBuilder,
		List<TypeWriter> typeWriters,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		if (encryptionKey != null) {
			if (localVariableOperand == null) {
				localVariableOperand = parameterChangerBuilder.emitSaltEncodedLongLocal(encryptionKey, localVariableIndexCounter, instructions, methodNode, operandsByMethod, this.getConstantPoolManager(), typeWriters);
			}

			if (useDesEncryption) {
				int desInitLocalIndex = localVariableIndexCounter.getAndIncrease();
				longEncryptionBuilder.buildDesInitCode(localVariableTable, instructions, desInitLocalIndex, encryptionKey, localVariableOperand, localVariableIndexCounter, typeWriters, workspace, classProvider);
				desInitSlotRef.set(INTEGER_CACHE.valueOf(desInitLocalIndex));
			}
		} else if (longEncryptionBuilder.hasGlobalKey()) {
			AbstractInsnNode.appendLongConst(longEncryptionBuilder.getGlobalKey(), instructions, this.constantPoolManager, typeWriters);
			int globalKeyLocalIndex = localVariableIndexCounter.get();
			localVariableIndexCounter.add(2);
			instructions.add(AbstractInsnNode.storeLong(globalKeyLocalIndex, localVariableTable, 12));
			globalKeySlotRef.set(INTEGER_CACHE.valueOf(globalKeyLocalIndex));
		}

		LongNode encryptedLongNode = this.constantPoolManager.getOrCreateLong(-1L, typeWriters);
		long encryptedValue;
		if (useDesEncryption) {
			encryptedValue = longEncryptionBuilder.combineWithKey(constantValue, encryptionKey);
		} else if (encryptionKey != null) {
			encryptedValue = constantValue ^ encryptionKey;
		} else {
			encryptedValue = constantValue ^ longEncryptionBuilder.getGlobalKey();
		}

		encryptedLongNode.setValue(encryptedValue);
		longEncryptionBuilder.buildStoreEncryptedLong(localVariableTable, instructions, arrayField, encryptedLongNode, entry, switchLabel, labelCounter, labelCounters);
		if (!referencesByField.isEmpty()) {
			ArrayList<AbstractInsnNode> lookupInstructions = new ArrayList<>();
			if (arrayField != null) {
				lookupInstructions.add(new WideConstantPoolInsnNode(GETSTATIC, arrayField));
			} else if (entry.hasIndex()) {
				lookupInstructions.add(AbstractInsnNode.loadObject(entry.getLocalArrayIndex(), localVariableTable, 12));
				lookupInstructions.add(AbstractInsnNode.loadInt(entry.getIndex(), localVariableTable, 12));
				lookupInstructions.add(InsnNode.of(LALOAD));
			} else {
				lookupInstructions.add(AbstractInsnNode.loadLong(entry.getLocalArrayIndex(), localVariableTable, 12));
			}

			Iterator<Entry<FieldTypeWriter, AtomicReference<Long>>> fieldEntryIterator = referencesByField.entrySet().iterator();

			while (fieldEntryIterator.hasNext()) {
				FieldTypeWriter lookupFieldRef = fieldEntryIterator.next().getKey();
				if (arrayField == null) {
					longEncryptionBuilder.buildStoreWithLookup(localVariableTable, lookupInstructions, lookupFieldRef, entry, encryptionKey, localVariableOperand, typeWriters);
				}
			}

			lookupInstructions.add(InsnNode.of(POP2));
			instructions.addAll(lookupInstructions);
		}
	}

	public MethodNode createPublicStaticMethod(
		String descriptor,
		ArrayList<AbstractInsnNode> instructions,
		int maxStack,
		int maxLocals,
		LocalVariableTable localVariableTable,
		TryCatchBlockInfo[] tryCatchBlocks,
		String changeLogName,
		List<TypeWriter> typeWriters,
		Inheritance inheritance,
		AbstractWorkspace workspace,
		int generationType
	) {
		return this.createUniqueMethod(descriptor, instructions, maxStack, maxLocals, 4, localVariableTable, tryCatchBlocks, changeLogName, typeWriters, inheritance, workspace, generationType);
	}

	public void validateConstantPoolSize(String featureName, String hint) {
		int optimizedSize = this.constantPoolManager.calculateOptimizedSize();
		if (optimizedSize > 65535) {
			throw new ProcessingException(
				featureName
					+ " : ConstantPool length too large in class '"
					+ this.getSourcePath()
					+ "' : "
					+ optimizedSize
					+ " > "
					+ 65535
					+ " : ("
					+ this.constantPoolManager.getConstantPoolSize()
					+ ") : "
					+ hint
			);
		}
	}

	public Enumeration<AbstractFieldNode> getReferencedFieldNodes() {
		return this.fieldReferenceGraph == null ? null : this.fieldReferenceGraph.getMembersAsEnumeration();
	}

	public AbstractMethodCallWriter getOrCreateAnyMethodRef(AbstractMethodNode methodNode, List<TypeWriter> typeWriters) {
		return this.constantPoolManager.getOrCreateAnyMethodRef(methodNode, typeWriters);
	}

	public boolean applyAutoReflectionHandling(
		MultiMap<MethodNode, MethodInvocation> methodInvocations,
		ClassNode sourceClass,
		IReferenceGraph<ClassNode, ClassNode> classReferenceGraph,
		boolean hasRenamedNames,
		boolean hasRenamedFields,
		boolean hasRenamedMethods,
		TypeResolver typeResolver,
		AbstractWorkspace workspace,
		boolean verbose,
		ScriptContext scriptContext
	) {
		boolean anyReflectionHandled = false;
		ArrayList<TypeWriter> typeWriters = new ArrayList<>();

		for (int i = 0; i < this.methodCount; i++) {
			MethodNode methodNode = this.methods[i];
			List<MethodInvocation> invocations = methodInvocations.get(methodNode);
			if (invocations != null && invocations.size() > 0) {
				boolean reflectionHandled = methodNode.handleAutoReflection(invocations, sourceClass.getInternalName(), typeWriters, hasRenamedNames, hasRenamedFields, hasRenamedMethods, typeResolver, workspace, verbose, scriptContext);
				anyReflectionHandled = anyReflectionHandled || reflectionHandled;
			}
		}

		if (anyReflectionHandled) {
			classReferenceGraph.addReference(sourceClass, this);
			if (typeWriters.size() > 0) {
				this.appendEntriesToConstantPool(typeWriters);
			}

			this.markChanged();
			this.notify(new AtomicInt(4), this, null);
		}

		return anyReflectionHandled;
	}

	public boolean hasLineNumberTable() {
		return this.hasLineNumberTable;
	}

	private Set<LongNode> collectExcludedLongNodes(LongEncryptionExcludes encryptionExcludes, TypeResolver typeResolver, ClassHierarchy classHierarchy, ScriptContext scriptContext) {
		HashSet<LongNode> excludedLongNodes = GlobalUtils.newHashSet();
		if (encryptionExcludes != null && encryptionExcludes.hasClassWithExcludedFields(this)) {
			Enumeration<FieldNode> excludedFields = encryptionExcludes.getExcludedFieldsForClass(this);

			while (excludedFields.hasMoreElements()) {
				ConstantValueAttribute constantValueAttribute = excludedFields.nextElement().getConstantValueAttribute();
				if (constantValueAttribute != null) {
					LongNode longConstant = constantValueAttribute.getLongConstant();
					if (longConstant != null) {
						excludedLongNodes.add(longConstant);
					}
				}
			}

			for (int i = 0; i < this.methodCount; i++) {
				MethodNode methodNode = this.methods[i];
				if (methodNode.hasLongConstants() && (methodNode.isConstructor() || methodNode.isClinit())) {
					methodNode.collectExcludedLongConstants(excludedLongNodes, encryptionExcludes);
				}
			}
		}

		if (encryptionExcludes != null && encryptionExcludes.hasClassWithExcludedMethods(this)) {
			Enumeration<MethodNode> excludedMethods = encryptionExcludes.getExcludedMethodsForClass(this);

			while (excludedMethods.hasMoreElements()) {
				MethodNode excludedMethod = excludedMethods.nextElement();
				if (excludedMethod.hasLongConstants()) {
					excludedMethod.collectLongNodes(excludedLongNodes);
				}
			}
		}

		return excludedLongNodes;
	}

	@Override
	public void remapReferences(
		int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> classNameMappings, HashSet<StringNode> processedStringNodes, Set<String> resourceFilePaths, HashMap<String, String> packageMappings, AbstractLogger logger
	) {
		this.constantPoolManager.remapConstantNames(classNameMappings, logger);

		for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
			this.methods[methodIndex].remapTypes(keepInnerClassInfo, keepGenericsInformation, packageMappings, classNameMappings);
		}

		for (int fieldIndex = 0; fieldIndex < this.fieldsCount; fieldIndex++) {
			this.fields[fieldIndex].remapTypes(keepInnerClassInfo, keepGenericsInformation, packageMappings, classNameMappings);
		}

		this.deriveInnerClassSimpleNames();

		for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
			this.attributes[attributeIndex].remapDescriptors(keepInnerClassInfo, keepGenericsInformation, packageMappings, classNameMappings);
		}

		this.constantPoolManager.remapStringConstantPool(classNameMappings, processedStringNodes, resourceFilePaths, packageMappings);
		this.constantPoolManager.remapDescriptorsInStrings(classNameMappings);
	}

	@Override
	public AbstractConstantPoolWriter getConstantPoolWriter() {
		return this.getConstantPoolManager();
	}

	public void setClassAnnotationSet(AnnotationSettings classAnnotationSet) {
		this.classAnnotationSet = classAnnotationSet;
	}

	public FieldNode[] getFieldNodes() {
		return this.fields;
	}

	@Override
	public void onChange(INotifier notifier, Object changeKind, Object changedNode, Object changeArg) {
		this.markChanged();
		this.notify(changeKind, changedNode, changeArg);
	}

	public void setHasStackMapAttribute(boolean value) {
		this.hasStackMapAttribute = value;
	}

	public void updatePreverification(ScriptContext scriptContext, TypeResolver typeResolver, ClassHierarchy classHierarchy) {
		ArrayList<TypeWriter> typeWriters = new ArrayList<>();

		for (int i = 0; i < this.methodCount; i++) {
			try {
				this.methods[i].rebuildStackMap(typeResolver, classHierarchy, this.constantPoolManager, typeWriters);
			} catch (MethodAnalyzerException methodAnalyzerException) {
				scriptContext.logWarning(
					"Couldn't update preverification in '"
						+ this.getRawSourcePath()
						+ "' : \""
						+ methodAnalyzerException.getMessage()
						+ "\". You must correct this error or preverify AFTER obfuscation. (A)"
				);
			} catch (InvalidBytecodeException invalidBytecodeException) {
				scriptContext.logWarning(
					"Couldn't update preverification in '"
						+ this.getRawSourcePath()
						+ "' : \""
						+ invalidBytecodeException.getMessage()
						+ "\". You must correct this error or preverify AFTER obfuscation. (B)"
				);
			} catch (ClasspathException classpathException) {
				scriptContext.logWarning(
					"Couldn't update preverification in '"
						+ this.getRawSourcePath()
						+ "' : \""
						+ classpathException.getMessage()
						+ "\". You must correct this error or preverify AFTER obfuscation. (C)"
				);
			}
		}

		this.constantPoolManager.appendEntriesToPool(typeWriters);
	}

	public void setHasLineNumberTable(boolean value) {
		this.hasLineNumberTable = value;
	}

	public String generateUniqueMethodName(List<String> overrideNames, String descriptor, Inheritance inheritance, boolean useParameterKeys) {
		return this.generateUniqueMethodName(overrideNames, descriptor, inheritance, null, useParameterKeys, null);
	}

	public String generateUniqueMethodName(String descriptor, Inheritance inheritance, boolean useParameterKeys) {
		return this.generateUniqueMethodName(null, descriptor, inheritance, null, useParameterKeys, null);
	}

	public final void collectMethodReferenceGraph(ReferenceGraph<MethodNode, MethodNode> graph, Set<MethodNode> visitedMethods) {
		MethodNode[] methodNodes = this.getMethodNodes();

		for (int i = 0; i < this.methodCount; i++) {
			methodNodes[i].buildMethodReferenceGraph(graph, visitedMethods);
		}
	}

	public void encryptStringAndInjectDecode(
		LocalVariableTable localVariableTable,
		List<AbstractInsnNode> instructions,
		FieldTypeWriter arrayField,
		StringNode stringNode,
		JumpType jumpType,
		LabelNode switchLabel,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> labelCounters,
		AtomicInt labelCounter,
		Map<FieldTypeWriter, AtomicReference<String>> referencesByField,
		StringEntry entry,
		boolean injectXorMask,
		AtomicReference<Integer> desInitSlotRef,
		StringEncryptionBuilder stringEncryptionBuilder,
		MethodNode methodNode,
		LocalVariableIndexCounter localVariableIndexCounter,
		AtomicReference<Integer> globalKeySlotRef,
		Long encryptionKey,
		LocalVariableOperand localVariableOperand,
		Map<MethodNode, LocalVariableOperand> operandsByMethod,
		boolean parametersChanged,
		Set<StringNode> processedStrings,
		ParameterChangerBuilder parameterChangerBuilder,
		List<TypeWriter> typeWriters,
		AbstractWorkspace workspace,
		ClassProvider classProvider
	) {
		if (encryptionKey != null) {
			if (localVariableOperand == null) {
				localVariableOperand = parameterChangerBuilder.emitSaltEncodedLongLocal(encryptionKey, localVariableIndexCounter, instructions, methodNode, operandsByMethod, this.getConstantPoolManager(), typeWriters);
			}

			if (parametersChanged) {
				int desInitLocalIndex = localVariableIndexCounter.getAndIncrease();
				stringEncryptionBuilder.injectDesCipherInitCode(localVariableTable, instructions, desInitLocalIndex, encryptionKey, localVariableOperand, localVariableIndexCounter, typeWriters, workspace, classProvider);
				globalKeySlotRef.set(INTEGER_CACHE.valueOf(desInitLocalIndex));
			}
		}

		stringEncryptionBuilder.injectStringLoadAndDecode(localVariableTable, instructions, arrayField, stringNode, entry, jumpType, switchLabel, labelCounter, labelCounters, injectXorMask, desInitSlotRef);
		if (!referencesByField.isEmpty()) {
			ArrayList<AbstractInsnNode> lookupInstructions = new ArrayList<>();
			if (arrayField != null) {
				lookupInstructions.add(new WideConstantPoolInsnNode(GETSTATIC, arrayField));
			} else {
				lookupInstructions.add(AbstractInsnNode.loadObject(entry.getLocalArrayIndex(), localVariableTable, 1));
			}

			Iterator<Entry<FieldTypeWriter, AtomicReference<String>>> fieldEntryIterator = referencesByField.entrySet().iterator();

			while (fieldEntryIterator.hasNext()) {
				FieldTypeWriter lookupFieldRef = fieldEntryIterator.next().getKey();
				if (arrayField == null || lookupFieldRef.getTargetMember() != arrayField.getTargetMember()) {
					stringEncryptionBuilder.injectDynamicFieldWrite(localVariableTable, lookupInstructions, lookupFieldRef, entry, encryptionKey, localVariableOperand, typeWriters);
				}
			}

			lookupInstructions.add(InsnNode.of(POP));
			instructions.addAll(lookupInstructions);
		}

		String originalString = stringNode.getDisplayValue();
		String encryptedString;
		if (parametersChanged) {
			encryptedString = stringEncryptionBuilder.desEncryptString(originalString, encryptionKey);
		} else {
			int[] xorKeyArray;
			if (!desInitSlotRef.isNull()) {
				xorKeyArray = stringEncryptionBuilder.generateXorMaskedKeyArray(desInitSlotRef.get());
			} else {
				xorKeyArray = stringEncryptionBuilder.getXorKeyArray();
			}

			encryptedString = StringEncryptionBuilder.simpleXorEncode(originalString, xorKeyArray);
		}

		if (encryptedString.length() > 65000) {
			byte[] encryptedBytes = GlobalUtils.toUTF8(encryptedString);
			if (encryptedBytes.length > 65535) {
				byte[] originalBytes = GlobalUtils.toUTF8(originalString);
				throw new ProcessingException(
					"String too large to be String encrypted in class '"
						+ this.getSourcePath()
						+ "'. Original String length "
						+ originalBytes.length
						+ ". Encrypted String length "
						+ encryptedBytes.length
						+ " Consider excluding the class from String Encryption. First 50 characters of original String are '"
						+ GlobalUtils.escapeString(originalString.substring(0, 50))
						+ "'"
				);
			}
		}

		stringNode.setValueFromString(encryptedString);
		processedStrings.add(stringNode);
	}

	public void removeMethodsMarkedForRemoval(ReadOnlyMap<MethodNode, MethodNode> removedMethods) {
		ArrayList<MethodNode> retainedMethods = new ArrayList<>();

		for (int i = 0; i < this.methods.length; i++) {
			if (!removedMethods.containsKey(this.methods[i])) {
				retainedMethods.add(this.methods[i]);
			}
		}

		this.methods = retainedMethods.toArray(new MethodNode[retainedMethods.size()]);
		this.methodCount = this.methods.length;
	}

	public void removeFieldsMarkedForRemoval(ReadOnlyMap<FieldNode, FieldNode> removedFields) {
		ArrayList<FieldNode> retainedFields = new ArrayList<>();

		for (int i = 0; i < this.fields.length; i++) {
			if (!removedFields.containsKey(this.fields[i])) {
				retainedFields.add(this.fields[i]);
			}
		}

		this.fields = retainedFields.toArray(new FieldNode[retainedFields.size()]);
		this.fieldsCount = this.fields.length;
	}

	public MethodNode[] getMethodNodes() {
		return this.methods;
	}

	public boolean hasStackMapAttributes() {
		return this.hasStackMapAttribute;
	}

	private String generateDefaultFieldName(List<String> overrideNames, String fieldDescriptor, Inheritance inheritance) {
		return this.generateUniqueFieldName(overrideNames, fieldDescriptor, inheritance, null);
	}

	private LabelNode createSwitchLabelState(AtomicReference<LabelNode> switchLabelRef, AtomicReference<AtomicInt> labelCounterRef, Map<LabelNode, AtomicInt> labelReplacements) {
		LabelNode initialSwitchLabel = new LabelNode(true, 1);
		AtomicInt initialCaseIndex = new AtomicInt(-2);
		labelReplacements.put(initialSwitchLabel, initialCaseIndex);
		switchLabelRef.set(initialSwitchLabel);
		labelCounterRef.set(initialCaseIndex);
		return initialSwitchLabel;
	}

	private void prepareStringConcat(StringConcatAnalysis stringConcatAnalysis, TypeResolver typeResolver, ClassHierarchy classHierarchy, ScriptContext scriptContext) {
		ConstantPoolManager poolManager = this.getConstantPoolManager();
		HashMap<InvokeDynamicCallWriter, String> argumentDescriptorsByCallWriter = GlobalUtils.newHashMap();
		MultiMap<InstructionListNode, InstructionBlock> patchBlocksByInstructionList = new MultiMap<>(GlobalUtils.getInitialCapacity(this.methodCount));
		ArrayList<TypeWriter> constantPoolEntries = new ArrayList<>();

		try {
			for (int i = 0; i < this.methodCount; i++) {
				this.methods[i].obfuscateStringConcat(stringConcatAnalysis, poolManager, argumentDescriptorsByCallWriter, patchBlocksByInstructionList, constantPoolEntries, typeResolver, classHierarchy);
			}
		} catch (InvalidBytecodeException ignored) {
			scriptContext.logWarning("java/lang/invoke/StringConcatFactory makeConcatWithConstants call in class '" + this.getRawSourcePath() + "' could not be processed. (1)");
			return;
		} catch (ProcessingException ignored2) {
			scriptContext.logWarning("java/lang/invoke/StringConcatFactory makeConcatWithConstants call in class '" + this.getRawSourcePath() + "' could not be processed. (2)");
			return;
		}

		stringConcatAnalysis.applyTransformations();

		for (Entry<InvokeDynamicCallWriter, String> callWriterEntry : argumentDescriptorsByCallWriter.entrySet()) {
			callWriterEntry.getKey().prependArgumentDescriptor(callWriterEntry.getValue());
		}

		if (constantPoolEntries.size() > 0) {
			this.appendEntriesToConstantPool(constantPoolEntries);
		}

		String preparationMessage = "Preparing class '" + this.getRawSourcePath() + "' for String Encryption";
		if (!patchBlocksByInstructionList.isEmpty()) {
			for (Entry<InstructionListNode, List<InstructionBlock>> patchEntry : patchBlocksByInstructionList.entrySet()) {
				InstructionListNode instructionListNode = patchEntry.getKey();
				instructionListNode.applyPatchesWithoutStackCheck(patchEntry.getValue(), preparationMessage);
			}
		}
	}

	public void collectMethodCalls(MultiMap<AbstractMethodNode, InstructionListNode> callSitesByMethod) {
		for (int i = 0; i < this.methodCount; i++) {
			this.methods[i].collectMethodCalls(callSitesByMethod);
		}
	}

	public void collectFlowMethodCandidates(MultiMap<ClassNode, PriorityInstructionEntry> flowCandidates) {
		if (!this.isInterface()) {
			for (int i = 0; i < this.methodCount; i++) {
				InstructionListNode instructionList = this.methods[i].getInstructionList();
				if (instructionList != null) {
					int minReturnStackDepth = this.methods[i].getMinReturnStackDepth();
					if (minReturnStackDepth == -1) {
						float codeLengthPriority = 1.0F - (float)(1.0 / instructionList.getByteCodeLength());
						PriorityInstructionEntry lengthBasedEntry = new PriorityInstructionEntry(codeLengthPriority, instructionList);
						flowCandidates.put(this, lengthBasedEntry);
					} else if (minReturnStackDepth < Integer.MAX_VALUE) {
						PriorityInstructionEntry stackDepthBasedEntry = new PriorityInstructionEntry(minReturnStackDepth, instructionList);
						flowCandidates.put(this, stackDepthBasedEntry);
					}
				}
			}
		}
	}

	@Override
	public int getMethodCount() {
		return this.methods.length;
	}

	public MultiMap<Integer, Integer> collectLineNumberIdentityMap(IntegerCache integerCache) {
		ArrayList<LineNumberEntry> lineNumberTables = new ArrayList<>(Math.max(10, (int)(this.constantPoolManager.getConstantPoolSize() * 0.25)));

		for (int i = 0; i < this.methodCount; i++) {
			this.methods[i].updateLineNumbers(lineNumberTables);
		}

		int lineNumberTableCount = lineNumberTables.size();
		MultiMap<Integer, Integer> lineNumberIdentityMap = new MultiMap<>(lineNumberTableCount, 1);
		HashSet<Integer> seenLineNumbers = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(lineNumberTableCount));

		for (int tableIndex = 0; tableIndex < lineNumberTables.size(); tableIndex++) {
			LineNumberEntry lineNumberTable = lineNumberTables.get(tableIndex);
			Integer lineNumber = integerCache.valueOf(lineNumberTable.getLineNumber());
			if (seenLineNumbers.add(lineNumber)) {
				lineNumberIdentityMap.put(lineNumber, lineNumber);
			}
		}

		return lineNumberIdentityMap;
	}

	@Override
	public void resolveRefsAndSyncMetadata(ScriptContext scriptContext, AbstractWorkspace workspace, boolean pruneSignatureIfChanged) {
		this.constantPoolManager.resolveMethodAndDynamicRefs(scriptContext, workspace);

		for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
			if (this.attributes[attributeIndex] instanceof AnnotationContainer) {
				((AnnotationContainer)this.attributes[attributeIndex]).syncNames();
			} else if (this.attributes[attributeIndex] instanceof EnclosingMethodAttribute) {
				((EnclosingMethodAttribute)this.attributes[attributeIndex]).syncMethodNameAndType();
			} else if (this.attributes[attributeIndex] instanceof RecordAttribute) {
				((RecordAttribute)this.attributes[attributeIndex]).syncAnnotationNames();
			}
		}

		for (int fieldIndex = 0; fieldIndex < this.fieldsCount; fieldIndex++) {
			this.fields[fieldIndex].syncAnnotationNames();
		}

		for (int methodIndex = 0; methodIndex < this.methodCount; methodIndex++) {
			this.methods[methodIndex].syncAnnotationsAndPruneSignature(scriptContext, pruneSignatureIfChanged);
		}
	}

	public void collectIntegerTargets(
		NestedMultiMap<ClassNode, MethodNode, IntegerNode, PriorityEntry<WritableType>> integerConstantEntries,
		KeyValuePairMultiMap<ClassNode, IntegerNode, FieldNode> staticIntegerFields,
		KeyValuePairMultiMap<ClassNode, IntegerNode, FieldNode> instanceIntegerFields,
		IntegerEncryptionExcludes encryptionExcludes,
		boolean includeGeneratedMethods,
		boolean encryptAllIntegers,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy,
		ScriptContext scriptContext
	) {
		Set<IntegerNode> excludedIntegerNodes = this.collectExcludedIntegerNodes(encryptionExcludes, typeResolver, classHierarchy, scriptContext);
		this.collectIntegerFields(staticIntegerFields, instanceIntegerFields, excludedIntegerNodes);
		ArrayList<TypeWriter> constantPoolEntries = new ArrayList<>();
		ConstantPoolManager poolManager = this.getConstantPoolManager();

		for (int i = 0; i < this.methods.length; i++) {
			if ((encryptionExcludes == null || !encryptionExcludes.isMethodExcluded(this.methods[i])) && (includeGeneratedMethods || !this.methods[i].isGeneratedMember())) {
				this.methods[i].encryptIntegerConstants(integerConstantEntries, excludedIntegerNodes, encryptionExcludes, encryptAllIntegers, constantPoolEntries, poolManager);
			}
		}

		if (!constantPoolEntries.isEmpty()) {
			poolManager.appendEntriesToPool(constantPoolEntries);
		}
	}

	public void collectMemberAccesses(
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldReadAccesses,
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldWriteAccesses,
		HierarchicalMultiMap<MethodID, InstructionListNode, Integer> methodAccesses,
		IntegerCache integerCache
	) {
		for (int i = 0; i < this.methods.length; i++) {
			this.methods[i].mapMemberAccessInsns(fieldReadAccesses, fieldWriteAccesses, methodAccesses, integerCache);
		}
	}

	public boolean optimizeLdcAndStackMaps(
		MethodNode methodNode,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy,
		ConstantPoolManager constantPoolManager,
		Map<TypeWriter, TypeWriter> typeWriterMapping,
		boolean rebuildStackMaps,
		boolean useStackMapTable
	) {
		if (rebuildStackMaps & !useStackMapTable) {
			rebuildStackMaps = this.isJava6OrNewer();
		}

		ArrayList<TypeWriter> constantPoolEntries = new ArrayList<>();

		boolean ldcAndStackMapsOptimized;
		try {
			ldcAndStackMapsOptimized = methodNode.getCodeAttribute().optimizeLdcAndStackMaps(typeResolver, classHierarchy, constantPoolManager, constantPoolEntries, typeWriterMapping, rebuildStackMaps, useStackMapTable);
		} catch (InvalidBytecodeException invalidBytecodeException) {
			throw new ProcessingException(this.getSourcePath() + " " + methodNode.getDisplayName() + " : " + invalidBytecodeException.getMessage());
		}

		if (!constantPoolEntries.isEmpty()) {
			constantPoolManager.appendEntriesToPool(constantPoolEntries);
		}

		return ldcAndStackMapsOptimized && (rebuildStackMaps || useStackMapTable) || !constantPoolEntries.isEmpty();
	}

	@Override
	public final boolean isWorkspaceNode() {
		return true;
	}

	public ObservableStructure getClassPropertyEntry(int observerIndex) {
		return classPropertyEntries.elementAt(observerIndex);
	}

	private void collectIntegerFields(
		KeyValuePairMultiMap<ClassNode, IntegerNode, FieldNode> staticIntegerFields, KeyValuePairMultiMap<ClassNode, IntegerNode, FieldNode> instanceIntegerFields, Set<IntegerNode> excludedIntegers
	) {
		for (int i = 0; i < this.fields.length; i++) {
			FieldNode field = this.fields[i];
			if (field.isIntLikeType()) {
				ConstantValueAttribute constantValueAttribute = field.getConstantValueAttribute();
				if (constantValueAttribute != null) {
					IntegerNode integerConstant = constantValueAttribute.getIntegerConstant();
					int constantValue = integerConstant.getValue();
					if ((Options.ZKM_INTEGER_ENCRYPT_LOW_VALUE_FIELDS || constantValue < -1 || constantValue > 1) && !excludedIntegers.contains(integerConstant)) {
						if (field.isStatic()) {
							staticIntegerFields.put(this, integerConstant, field);
						} else {
							instanceIntegerFields.put(this, integerConstant, field);
						}
					}
				}
			}
		}
	}

	public void encryptStringConstants(
		Inheritance inheritance,
		StringEncryptionBuilder stringEncryptionBuilder,
		boolean aggressiveEncryption,
		boolean useDoubleArrayStorage,
		boolean useGoto,
		boolean useStringIntern,
		boolean injectXorMask,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		List<KeyValuePair<StringNode, FieldNode>> stringFieldPairs,
		List<FieldNode> instanceFields,
		KeyValuePairMultiMap<MethodNode, StringNode, PriorityEntry<WritableType>> constantEntries,
		StringEncryptionExcludes encryptionTargetManager,
		Set<StringNode> processedStrings,
		int maxEncryptedStrings,
		ParameterChangerBuilder parameterChangerBuilder,
		Map<MethodNode, Long> encryptionKeysByMethod,
		Map<MethodNode, LocalVariableOperand> operandsByMethod,
		List<ClassNode> encryptedClasses,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy
	) {
		boolean isInterface = this.isInterface();
		if ((!isInterface || this.isJava8OrNewer() || aggressiveEncryption) && (encryptionTargetManager == null || !encryptionTargetManager.isClassExcluded(this))) {
			if (stringFieldPairs == null) {
				stringFieldPairs = new ArrayList<>(13);
			}

			if (instanceFields == null) {
				instanceFields = new ArrayList<>(0);
			}

			if (constantEntries == null) {
				constantEntries = new KeyValuePairMultiMap<>(13);
			}

			if (stringFieldPairs.size() != 0 || constantEntries.size() != 0) {
				if (maxEncryptedStrings < Integer.MAX_VALUE) {
					int constantCount = constantEntries.valueCount();
					if (constantCount > maxEncryptedStrings) {
						int excessConstantCount = constantCount - maxEncryptedStrings;
						int removedCount = 0;
						Enumeration<MethodNode> methodEnumeration = constantEntries.keys();

						label304:
						while (methodEnumeration.hasMoreElements()) {
							MethodNode method = methodEnumeration.nextElement();
							Iterator<KeyValuePair<StringNode, PriorityEntry<WritableType>>> entryIterator = constantEntries.get(method).iterator();

							while (entryIterator.hasNext()) {
								KeyValuePair<StringNode, PriorityEntry<WritableType>> entry = entryIterator.next();
								entryIterator.remove();
								if (++removedCount >= excessConstantCount) {
									break label304;
								}
							}
						}
					}
				}

				HashSet<StringNode> nonClinitStrings = GlobalUtils.newHashSet();
				HashSet<StringNode> clinitStrings = GlobalUtils.newHashSet();
				ReferenceGraph<AtomicReference<String>, StringNode> stringReferenceGraph = new ReferenceGraph<>();
				HashMap<StringNode, AtomicReference<String>> stringReferencesByNode = GlobalUtils.newHashMap();
				MethodNode clinitMethod = this.buildStringReferenceGraph(constantEntries, nonClinitStrings, clinitStrings, stringFieldPairs, stringReferenceGraph, stringReferencesByNode, useDoubleArrayStorage);
				if (clinitMethod == null) {
					if (!this.isInnerClass()) {
						clinitMethod = workspace.getMethodBySignature(this, MethodSignature.STATIC_INITIALIZER);
					} else {
						clinitMethod = this.findMethodNode(MethodSignature.STATIC_INITIALIZER);
					}
				}

				HashSet<StringNode> allStrings = GlobalUtils.newHashSet();
				ArrayList<StringNode> stringNodes = new ArrayList<>(stringFieldPairs.size());

				for (KeyValuePair<StringNode, FieldNode> stringFieldPair : stringFieldPairs) {
					stringNodes.add(stringFieldPair.getKey());
				}

				allStrings.addAll(stringNodes);
				allStrings.addAll(nonClinitStrings);
				allStrings.addAll(clinitStrings);
				HashSet<StringNode> distinctStringNodes = GlobalUtils.newHashSet(clinitStrings);
				distinctStringNodes.addAll(stringNodes);
				int clinitMaxLocals = clinitMethod == null ? 0 : clinitMethod.getMaxLocalsCompat();
				clinitStrings.size();
				int distinctStringNodeCount = distinctStringNodes.size();
				boolean useSeparateEncodingMethods = false;
				JumpType jumpType;
				if (isInterface) {
					if (!aggressiveEncryption && nonClinitStrings.size() == 0) {
						return;
					}

					if (useGoto) {
						jumpType = JumpType.GOTO;
					} else if (distinctStringNodeCount <= 5000 && clinitMaxLocals <= 50) {
						jumpType = JumpType.JSR;
					} else {
						jumpType = JumpType.GOTO;
					}
				} else {
					if (!aggressiveEncryption) {
						useSeparateEncodingMethods = true;
					}

					if (useGoto) {
						jumpType = JumpType.GOTO;
					} else if (distinctStringNodeCount <= 5000 && clinitMaxLocals <= 50) {
						jumpType = JumpType.JSR;
					} else {
						jumpType = JumpType.GOTO;
					}
				}

				ArrayList<TypeWriter> constantPoolEntries = new ArrayList<>();
				HashMap<FieldTypeWriter, AtomicReference<String>> stringReferencesByField = GlobalUtils.newHashMap();
				this.prepareStringFieldRefs(stringFieldPairs, stringReferencesByNode, stringReferencesByField, instanceFields, constantPoolEntries);
				if (clinitMethod == null) {
					clinitMethod = this.getOrCreateStaticInitializer(constantPoolEntries, workspace, "String Encryption", 1);
				}

				LocalVariableIndexCounter localIndexCounter = new LocalVariableIndexCounter(clinitMethod);
				LocalVariableTable localVariableTable = clinitMethod.getLocalVariableTable();
				FieldTypeWriter stringFieldRef = null;
				boolean multipleStrings = stringReferenceGraph.size() > 1;
				boolean stringsFromField = nonClinitStrings.size() > 0;
				AtomicReference<String>[] stringReferences = null;
				if (multipleStrings) {
					stringReferences = new AtomicReference[stringReferenceGraph.size()];
				}

				if (stringReferenceGraph.size() == 1) {
					HashSet<FieldNode> distinctFields = GlobalUtils.newHashSet();

					for (KeyValuePair<StringNode, FieldNode> fieldPair : stringFieldPairs) {
						distinctFields.add(fieldPair.getValue());
					}

					if (distinctFields.size() == 1) {
						FieldNode existingStringField = distinctFields.iterator().next();
						stringFieldRef = this.constantPoolManager.getOrCreateFieldRef(this.getInternalName(), existingStringField.getNameDisplay(), existingStringField.getDescriptor(), constantPoolEntries, workspace, classProvider, true);
						stringsFromField = true;
					}
				}

				if (stringsFromField && stringFieldRef == null) {
					String fieldDescriptor;
					if (stringReferenceGraph.size() == 1) {
						fieldDescriptor = "Ljava/lang/String;";
					} else {
						fieldDescriptor = "[Ljava/lang/String;";
					}

					FieldNode newStringField = this.createUniqueField(fieldDescriptor, isInterface ? 4 : 1, true, inheritance, workspace, 1);
					stringFieldRef = this.constantPoolManager.getOrCreateFieldRef(this.getInternalName(), newStringField.getNameDisplay(), newStringField.getDescriptor(), constantPoolEntries, workspace, classProvider, true);
				}

				if (useDoubleArrayStorage && stringsFromField && multipleStrings && stringReferences.length > 65535) {
					throw new ProcessingException(
						"String Encryption 'enhanced' setting can handle a maximumum of 65535 Strings. Number of Strings in class '"
							+ this.getRawSourcePath()
							+ "' is "
							+ stringReferences.length
							+ "."
					);
				}

				boolean arrayFieldEncryption = useDoubleArrayStorage && stringFieldRef != null && multipleStrings;
				boolean parameterChangerApplies = parameterChangerBuilder != null && parameterChangerBuilder.shouldEncryptStringsInMethod(clinitMethod) && !useSeparateEncodingMethods;
				boolean desIndyEnabled = arrayFieldEncryption && parameterChangerApplies && StringEncryptionBuilder.isDesIndyEncryptionEnabled(this);
				boolean xorMaskApplied = injectXorMask && !parameterChangerApplies;
				stringEncryptionBuilder.setupEncryptionSchema(this, inheritance, workspace, classProvider, constantPoolEntries, useSeparateEncodingMethods, useStringIntern, arrayFieldEncryption, xorMaskApplied, stringFieldRef, parameterChangerBuilder != null, desIndyEnabled, parameterChangerApplies);
				if (encryptedClasses != null) {
					encryptedClasses.add(this);
				}

				KeyValuePairMultiMap<MethodNode, PriorityEntry<WritableType>, StringEntry> stringEntriesByMethod = new KeyValuePairMultiMap<>();
				HashMap<AtomicReference<String>, StringEntry> stringEntriesByReference = GlobalUtils.newHashMap();
				this.populateStringEntryMappings(
					stringEntriesByMethod, stringEntriesByReference, constantEntries, stringReferenceGraph, stringReferencesByNode, stringFieldRef, multipleStrings, parameterChangerApplies, stringEncryptionBuilder.getIndyCallSiteRef(this), stringEncryptionBuilder.getDecryptorMethodRef(this), stringReferences, localIndexCounter
				);
				int stringArrayLocalIndex = -1;
				if (!stringsFromField) {
					stringArrayLocalIndex = localIndexCounter.getAndIncrease();
				}

				HashMap<LabelNode, AtomicInt> jumpCountersByLabel = GlobalUtils.newHashMap();
				MultiMap<LabelNode, PriorityEntry<LabelNode>> stubLabelsByLabel = new MultiMap<>();
				AtomicReference<AtomicInt> jumpCounterReference = new AtomicReference<>();
				AtomicReference<LabelNode> jumpLabelReference = new AtomicReference<>();
				LabelNode jumpLabel = this.createJumpLabelBinding(jumpLabelReference, jumpCounterReference, jumpType, jumpCountersByLabel);
				int baseLocalIndex = localIndexCounter.get();
				AtomicInt jumpStubCounter = new AtomicInt(-2);
				AtomicReference<Integer> xorMask = new AtomicReference<>();
				Long encryptionKey = null;
				LocalVariableOperand localVariableOperand = null;
				if (parameterChangerBuilder != null && (arrayFieldEncryption && distinctStringNodeCount > 0 || parameterChangerApplies)) {
					encryptionKey = encryptionKeysByMethod != null ? encryptionKeysByMethod.get(clinitMethod) : null;
					localVariableOperand = operandsByMethod != null ? operandsByMethod.get(clinitMethod) : null;
					if (encryptionKey == null) {
						encryptionKey = parameterChangerBuilder.getOrCreateMethodEncryptionKey(clinitMethod);
					}
				}

				AtomicReference<Integer> cipherKeyLocalIndex = new AtomicReference<>();
				ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
				if (stringEncryptionBuilder.getThreadLocalMapField() != null) {
					stringEncryptionBuilder.injectThreadLocalMapInit(instructions, constantPoolEntries, this.constantPoolManager, workspace, classProvider);
				}

				if (multipleStrings) {
					this.injectStringArrayInitCode(
						localVariableTable,
						instructions,
						stringFieldRef,
						stringEncryptionBuilder.getEncryptedStringsArrayField(this),
						stringArrayLocalIndex,
						stringReferences,
						stringReferenceGraph,
						stringEntriesByReference,
						stringReferencesByField,
						jumpType,
						jumpLabelReference,
						jumpStubCounter,
						stubLabelsByLabel,
						localIndexCounter,
						constantPoolEntries,
						stringEncryptionBuilder,
						clinitMethod,
						useDoubleArrayStorage,
						xorMaskApplied,
						cipherKeyLocalIndex,
						encryptionKey,
						localVariableOperand,
						operandsByMethod,
						parameterChangerApplies,
						processedStrings,
						parameterChangerBuilder,
						workspace,
						classProvider
					);
				} else {
					AtomicReference<String> stringReference = stringReferenceGraph.getMembersAsEnumeration().nextElement();
					this.encryptStringAndInjectDecode(
						localVariableTable,
						instructions,
						stringFieldRef,
						stringReferenceGraph.getAllReferences().nextElement(),
						jumpType,
						jumpLabelReference.get(),
						stubLabelsByLabel,
						jumpStubCounter,
						stringReferencesByField,
						stringEntriesByReference.get(stringReference),
						xorMaskApplied,
						xorMask,
						stringEncryptionBuilder,
						clinitMethod,
						localIndexCounter,
						cipherKeyLocalIndex,
						encryptionKey,
						localVariableOperand,
						operandsByMethod,
						parameterChangerApplies,
						processedStrings,
						parameterChangerBuilder,
						constantPoolEntries,
						workspace,
						classProvider
					);
				}

				Enumeration<MethodNode> mappedMethodEnumeration = stringEntriesByMethod.keys();

				while (mappedMethodEnumeration.hasMoreElements()) {
					MethodNode mappedMethod = mappedMethodEnumeration.nextElement();
					List<KeyValuePair<PriorityEntry<WritableType>, StringEntry>> stringEntries = stringEntriesByMethod.get(mappedMethod);
					Object localSlotLimit;
					if (mappedMethod.isClinit()) {
						if (localIndexCounter.get() > baseLocalIndex) {
							localSlotLimit = new AtomicInt(baseLocalIndex);
						} else {
							localSlotLimit = localIndexCounter;
						}
					} else {
						localSlotLimit = new AtomicInt(mappedMethod.getMaxLocalsCompat());
					}

					Long methodEncryptionKey = encryptionKeysByMethod != null ? encryptionKeysByMethod.get(mappedMethod) : null;
					LocalVariableOperand methodLocalVariableOperand = operandsByMethod != null ? operandsByMethod.get(mappedMethod) : null;
					mappedMethod.encryptStringConstants(
						stringEntries, jumpCountersByLabel, stubLabelsByLabel, useDoubleArrayStorage ? methodEncryptionKey : null, useDoubleArrayStorage ? methodLocalVariableOperand : null, (Indexable)localSlotLimit, this.constantPoolManager, constantPoolEntries, typeResolver, classHierarchy
					);
				}

				stringEncryptionBuilder.injectDecryptionStubCall(localVariableTable, jumpLabel, localIndexCounter, instructions, constantPoolEntries, jumpType, stubLabelsByLabel, cipherKeyLocalIndex.get(), xorMaskApplied, this.constantPoolManager, workspace, classProvider);
				int instructionOffset = localIndexCounter.get() - clinitMaxLocals;
				ArrayList<Integer> localVariableIndices = new ArrayList<>();

				for (int localIndex = clinitMaxLocals; localIndex < localIndexCounter.get(); localIndex++) {
					localVariableIndices.add(localIndex);
				}

				LabelNode anchorLabel = new LabelNode(4096);
				instructions.add(anchorLabel);
				clinitMethod.setInjectionAnchorLabel(anchorLabel);
				clinitMethod.insertInstructionsAtOffset(instructions, instructionOffset, clinitMethod.getMaxStack(), 0, "String Encryption", localVariableIndices);
				if (constantPoolEntries.size() > 0) {
					this.constantPoolManager.appendEntriesToPool(constantPoolEntries);
				}

				this.markChanged();
				this.notify(new AtomicInt(4), this, null);
			}
		}
	}

	private void collectLongFields(
		KeyValuePairMultiMap<ClassNode, LongNode, FieldNode> staticLongFields, KeyValuePairMultiMap<ClassNode, LongNode, FieldNode> instanceLongFields, Set<LongNode> excludedLongs
	) {
		for (int i = 0; i < this.fields.length; i++) {
			FieldNode field = this.fields[i];
			if (field.isLongType()) {
				ConstantValueAttribute constantValueAttribute = field.getConstantValueAttribute();
				if (constantValueAttribute != null) {
					LongNode longConstant = constantValueAttribute.getLongConstant();
					long constantValue = longConstant.getValue();
					if ((Options.ZKM_LONG_ENCRYPT_LOW_VALUE_FIELDS || constantValue < -1L || constantValue > 1L) && !excludedLongs.contains(longConstant)) {
						if (field.isStatic()) {
							staticLongFields.put(this, longConstant, field);
						} else {
							instanceLongFields.put(this, longConstant, field);
						}
					}
				}
			}
		}
	}

	public FieldNode addSerialVersionUidField(long serialVersionUid, Inheritance inheritance, AbstractWorkspace workspace, int generationType) {
		ArrayList<TypeWriter> constantPoolEntries = new ArrayList<>(4);
		UTFStringWriter nameUtf = new UTFStringWriter(0, this.constantPoolManager, "serialVersionUID");
		constantPoolEntries.add(nameUtf);
		UTFStringWriter descriptorUtf = new UTFStringWriter(0, this.constantPoolManager, "J");
		constantPoolEntries.add(descriptorUtf);
		UTFStringWriter attributeNameUtf = new UTFStringWriter(0, this.constantPoolManager, "ConstantValue");
		constantPoolEntries.add(attributeNameUtf);
		LongNode longConstant = new LongNode(0, this.constantPoolManager, serialVersionUid);
		constantPoolEntries.add(longConstant);
		ConstantValueAttribute constantValueAttribute = new ConstantValueAttribute(this, attributeNameUtf, longConstant);
		AttributesNode[] attributes = new AttributesNode[]{constantValueAttribute};
		FieldNode field = new FieldNode(this, nameUtf, descriptorUtf, attributes, generationType);
		field.setStatic(true);
		field.setFinal(true);
		field.setDefaultAccess();
		this.addField(field, workspace);
		if (inheritance != null) {
			inheritance.registerField(field);
		}

		this.constantPoolManager.appendEntriesToPool(constantPoolEntries);
		this.markChanged();
		this.notify(new AtomicInt(4), this, null);
		return field;
	}

	public void rebuildStackMapTables(TypeResolver typeResolver, ClassHierarchy classHierarchy) {
		ArrayList<TypeWriter> constantPoolEntries = new ArrayList<>();

		for (int i = 0; i < this.methodCount; i++) {
			try {
				this.methods[i].rebuildStackMapTable(typeResolver, classHierarchy, this.constantPoolManager, constantPoolEntries);
			} catch (InvalidBytecodeException invalidBytecodeException) {
				throw new ProcessingException(this.getRawSourcePath() + " " + this.methods[i].getDisplayName() + " : " + invalidBytecodeException.getMessage());
			}
		}

		this.constantPoolManager.appendEntriesToPool(constantPoolEntries);
	}

	public String generateUniqueMethodName(List<String> overrideNames, String descriptor, Inheritance inheritance, ChangeLogContext changeLogContext, boolean useParameterKeys, String namePrefix) {
		ArrayList<String> remainingNames = overrideNames != null ? new ArrayList<>(overrideNames) : null;
		AlphaNameGenerator nameGenerator = new AlphaNameGenerator(0, namePrefix);
		int nameIndex = 0;
		HashSet<MethodSignature> declaredSignatures = null;
		HashSet<? extends AbstractMethodKey> inheritedMethodKeys;
		if (useParameterKeys) {
			inheritedMethodKeys = GlobalUtils.newHashSet(inheritance.getMethodParameterKeys(this));
		} else {
			inheritedMethodKeys = GlobalUtils.newHashSet(inheritance.getMethodSignatures(this));
		}

		String candidateName;
		boolean nameTaken;
		do {
			nameTaken = false;

			do {
				if (remainingNames != null && !remainingNames.isEmpty()) {
					candidateName = remainingNames.remove(0);
				} else {
					candidateName = nameGenerator.generateNameOrNull(nameIndex++);
				}
			} while (candidateName == null);

			MethodSignature candidateSignature = new MethodSignature(candidateName, descriptor);
			MethodParameterKey parameterKey = candidateSignature.getParameterKey();
			if ((!useParameterKeys || !inheritedMethodKeys.contains(parameterKey)) && (!useParameterKeys || !inheritedMethodKeys.contains(candidateSignature))) {
				if (declaredSignatures == null) {
					declaredSignatures = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(this.methodCount));

					for (int i = 0; i < this.methodCount; i++) {
						MethodSignature methodSignature = new MethodSignature(this.methods[i].getOriginalName(), this.methods[i].getDescriptor());
						declaredSignatures.add(methodSignature);
					}
				}

				if (declaredSignatures.contains(candidateSignature)) {
					nameTaken = true;
				}
			} else {
				nameTaken = true;
			}
		} while (nameTaken);

		return candidateName;
	}

	private MethodNode buildIntegerReferenceGraph(
		KeyValuePairMultiMap<MethodNode, IntegerNode, PriorityEntry<WritableType>> integerConstantEntries,
		Set<IntegerNode> methodIntegerNodes,
		Set<IntegerNode> clinitIntegerNodes,
		List<KeyValuePair<IntegerNode, FieldNode>> integerFieldPairs,
		ReferenceGraph<AtomicReference<Integer>, IntegerNode> referenceGraph,
		Map<IntegerNode, AtomicReference<Integer>> referenceHandles
	) {
		MethodNode clinitMethod = null;
		HashMap<Integer, AtomicReference<Integer>> methodConstantReferences = GlobalUtils.newHashMap();
		HashMap<Integer, AtomicReference<Integer>> fieldConstantReferences = GlobalUtils.newHashMap();
		ArrayList<IntegerNode> methodConstants = new ArrayList<>();

		for (Entry<MethodNode, List<KeyValuePair<IntegerNode, PriorityEntry<WritableType>>>> entry : integerConstantEntries.entrySet()) {
			MethodNode method = entry.getKey();
			List<KeyValuePair<IntegerNode, PriorityEntry<WritableType>>> constantEntries = entry.getValue();
			methodConstants.clear();

			for (KeyValuePair<IntegerNode, PriorityEntry<WritableType>> constantEntry : constantEntries) {
				methodConstants.add(constantEntry.getKey());
			}

			if (method.isClinit()) {
				clinitMethod = method;
				clinitIntegerNodes.addAll(methodConstants);
			} else {
				methodIntegerNodes.addAll(methodConstants);
			}

			for (IntegerNode integerConstant : methodConstants) {
				Integer constantValue = integerConstant.getValue();
				AtomicReference<Integer> constantReference = methodConstantReferences.get(constantValue);
				if (constantReference == null) {
					constantReference = new AtomicReference<>(constantValue);
					methodConstantReferences.put(constantValue, new AtomicReference<>(constantValue));
				}

				referenceGraph.addReference(constantReference, integerConstant);
				referenceHandles.put(integerConstant, constantReference);
			}
		}

		Iterator<KeyValuePair<IntegerNode, FieldNode>> fieldPairIterator = integerFieldPairs.iterator();

		while (fieldPairIterator.hasNext()) {
			IntegerNode fieldConstant = fieldPairIterator.next().getKey();
			Integer fieldConstantValue = fieldConstant.getValue();
			AtomicReference<Integer> fieldConstantReference = fieldConstantReferences.get(fieldConstantValue);
			if (fieldConstantReference == null) {
				fieldConstantReference = new AtomicReference<>(fieldConstantValue);
				fieldConstantReferences.put(fieldConstantValue, fieldConstantReference);
			}

			referenceGraph.addReference(fieldConstantReference, fieldConstant);
			referenceHandles.put(fieldConstant, fieldConstantReference);
		}

		return clinitMethod;
	}

	private void buildIntegerLookupEntries(
		KeyValuePairMultiMap<MethodNode, PriorityEntry<WritableType>, IntegerEntry> lookupEntriesByMethod,
		Map<AtomicReference<Integer>, IntegerEntry> entriesByReference,
		KeyValuePairMultiMap<MethodNode, IntegerNode, PriorityEntry<WritableType>> constantEntries,
		ReferenceGraph<AtomicReference<Integer>, IntegerNode> referenceGraph,
		Map<IntegerNode, AtomicReference<Integer>> referenceHandles,
		FieldTypeWriter arrayField,
		InvokeDynamicCallWriter indyCallWriter,
		boolean multipleConstants,
		boolean useDesEncryption,
		AbstractMethodCallWriter decryptMethodWriter,
		AtomicReference<Integer>[] valueReferences,
		LocalVariableIndexCounter localVariableIndexCounter
	) {
		int entryIndex = 0;
		Iterator<Entry<AtomicReference<Integer>, Set<IntegerNode>>> adjacencyIterator = referenceGraph.getAdjacencyEntries().iterator();

		while (adjacencyIterator.hasNext()) {
			AtomicReference<Integer> adjacencyHandle = adjacencyIterator.next().getKey();
			Integer discardedAdjacencyInteger = adjacencyHandle.get();
			if (entriesByReference.get(adjacencyHandle) == null) {
				if (arrayField != null) {
					IntegerEntry callWriterEntry;
					if (multipleConstants) {
						valueReferences[entryIndex] = adjacencyHandle;
						if (indyCallWriter != null) {
							callWriterEntry = new IntegerEntry(indyCallWriter, entryIndex++);
						} else if (decryptMethodWriter != null) {
							if (useDesEncryption) {
								callWriterEntry = new IntegerEntry(entryIndex++, decryptMethodWriter);
							} else {
								callWriterEntry = new IntegerEntry(decryptMethodWriter, entryIndex++);
							}
						} else {
							callWriterEntry = new IntegerEntry(arrayField, entryIndex++);
						}
					} else {
						callWriterEntry = new IntegerEntry(arrayField, -1);
					}

					entriesByReference.put(adjacencyHandle, callWriterEntry);
				} else {
					if (multipleConstants) {
						valueReferences[entryIndex] = adjacencyHandle;
					}

					IntegerEntry localVariableEntry = new IntegerEntry(localVariableIndexCounter.get(), multipleConstants ? entryIndex++ : -1);
					entriesByReference.put(adjacencyHandle, localVariableEntry);
				}
			}
		}

		for (Entry<MethodNode, List<KeyValuePair<IntegerNode, PriorityEntry<WritableType>>>> methodEntry : constantEntries.entrySet()) {
			MethodNode method = methodEntry.getKey();

			for (KeyValuePair<IntegerNode, PriorityEntry<WritableType>> integerEntry : methodEntry.getValue()) {
				IntegerNode integerConstant = integerEntry.getKey();
				PriorityEntry<WritableType> writableTypeEntry = integerEntry.getValue();
				AtomicReference<Integer> referenceHandle = referenceHandles.get(integerConstant);
				IntegerEntry lookupEntry = entriesByReference.get(referenceHandle);
				lookupEntriesByMethod.put(method, writableTypeEntry, lookupEntry);
			}
		}
	}

	public void applyControlFlowObfuscation(
		Set<InstructionListNode> instructionNodes,
		Set<InstructionListNode> excludedNodes,
		Condition condition,
		Condition condition2,
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> memberObfuscationRefs,
		MultiMap<InstructionListNode, InstructionBlock> instructionBlocks,
		Map<InstructionListNode, MethodAnalyzer> methodAnalyzerCache,
		AbstractWorkspace workspace,
		TypeResolver typeResolver,
		ClassProvider classProvider,
		Random random
	) {
		if (!this.isInterface() || this.isJava8OrNewer()) {
			ArrayList<TypeWriter> typeWriters = new ArrayList<>(7);

			for (int i = 0; i < this.methodCount; i++) {
				MethodNode method = this.methods[i];

				try {
					method.obfuscateControlFlow(instructionNodes, excludedNodes, condition, condition2, memberObfuscationRefs, typeWriters, this.constantPoolManager, instructionBlocks, methodAnalyzerCache, workspace, typeResolver, classProvider, random);
				} catch (InvalidBytecodeException invalidBytecodeException) {
					throw new ProcessingException(
						"Method '" + this.methods[i].getOriginalNameWithParams() + "' in class '" + this.getSourcePath() + "' appears to be invalid (8) : '" + invalidBytecodeException.getMessage() + "'"
					);
				}
			}

			if (typeWriters.size() > 0) {
				this.constantPoolManager.appendEntriesToPool(typeWriters);
				this.markChanged();
				this.notify(new AtomicInt(4), this, null);
			}
		}
	}

	private void buildLongLookupEntries(
		KeyValuePairMultiMap<MethodNode, PriorityEntry<WideConstantPoolInsnNode>, LongEntry> lookupEntriesByMethod,
		Map<AtomicReference<Long>, LongEntry> entriesByReference,
		KeyValuePairMultiMap<MethodNode, LongNode, PriorityEntry<WideConstantPoolInsnNode>> constantEntries,
		ReferenceGraph<AtomicReference<Long>, LongNode> referenceGraph,
		Map<LongNode, AtomicReference<Long>> referenceHandles,
		FieldTypeWriter arrayField,
		InvokeDynamicCallWriter indyCallWriter,
		boolean multipleConstants,
		boolean useDesEncryption,
		AbstractMethodCallWriter decryptMethodWriter,
		AtomicReference<Long>[] valueReferences,
		LocalVariableIndexCounter localVariableIndexCounter
	) {
		int entryIndex = 0;
		Iterator<Entry<AtomicReference<Long>, Set<LongNode>>> adjacencyIterator = referenceGraph.getAdjacencyEntries().iterator();

		while (adjacencyIterator.hasNext()) {
			AtomicReference<Long> adjacencyHandle = adjacencyIterator.next().getKey();
			Long discardedAdjacencyLong = adjacencyHandle.get();
			if (entriesByReference.get(adjacencyHandle) == null) {
				if (arrayField != null) {
					LongEntry callWriterEntry;
					if (multipleConstants) {
						valueReferences[entryIndex] = adjacencyHandle;
						if (indyCallWriter != null) {
							callWriterEntry = new LongEntry(indyCallWriter, entryIndex++);
						} else if (decryptMethodWriter != null) {
							if (useDesEncryption) {
								callWriterEntry = new LongEntry(entryIndex++, decryptMethodWriter);
							} else {
								callWriterEntry = new LongEntry(decryptMethodWriter, entryIndex++);
							}
						} else {
							callWriterEntry = new LongEntry(arrayField, entryIndex++);
						}
					} else {
						callWriterEntry = new LongEntry(arrayField, -1);
					}

					entriesByReference.put(adjacencyHandle, callWriterEntry);
				} else {
					if (multipleConstants) {
						valueReferences[entryIndex] = adjacencyHandle;
					}

					int localVariableIndex = localVariableIndexCounter.get();
					int arraySlotIndex = multipleConstants ? entryIndex++ : -1;
					LongEntry localVariableEntry = new LongEntry(localVariableIndex, arraySlotIndex);
					entriesByReference.put(adjacencyHandle, localVariableEntry);
				}
			}
		}

		for (Entry<MethodNode, List<KeyValuePair<LongNode, PriorityEntry<WideConstantPoolInsnNode>>>> methodEntry : constantEntries.entrySet()) {
			MethodNode method = methodEntry.getKey();

			for (KeyValuePair<LongNode, PriorityEntry<WideConstantPoolInsnNode>> longEntry : methodEntry.getValue()) {
				LongNode longConstant = longEntry.getKey();
				PriorityEntry<WideConstantPoolInsnNode> writableTypeEntry = longEntry.getValue();
				AtomicReference<Long> referenceHandle = referenceHandles.get(longConstant);
				LongEntry lookupEntry = entriesByReference.get(referenceHandle);
				lookupEntriesByMethod.put(method, writableTypeEntry, lookupEntry);
			}
		}
	}
}
