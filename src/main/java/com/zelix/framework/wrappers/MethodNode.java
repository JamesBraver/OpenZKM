package com.zelix.framework.wrappers;

import com.zelix.event.ChangeListener;
import com.zelix.event.INotifier;
import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.*;
import com.zelix.framework.analyzer.MethodAnalyzer;
import com.zelix.framework.analyzer.StaticBlockAnalyzer;
import com.zelix.framework.analyzer.StringConcatAnalysis;
import com.zelix.framework.analyzer.dataset.MethodAnalysisResult;
import com.zelix.framework.analyzer.reference.Reference;
import com.zelix.framework.annotation.AnnotationContainer;
import com.zelix.framework.attributes.BootstrapMethodEntry;
import com.zelix.framework.attributes.BridgeAttribute;
import com.zelix.framework.attributes.UnknownAttribute;
import com.zelix.framework.attributes.annotations.AnnotationSettings;
import com.zelix.framework.attributes.annotations.RuntimeInvisibleAnnotationsAttribute;
import com.zelix.framework.attributes.base.AbstractAnnotationsAttributeNode;
import com.zelix.framework.attributes.base.AbstractTypeAnnotationsAttributeNode;
import com.zelix.framework.attributes.code.CodeAttribute;
import com.zelix.framework.attributes.code.TryCatchBlockInfo;
import com.zelix.framework.attributes.debug.LineNumberEntry;
import com.zelix.framework.attributes.declaration.ExceptionsAttribute;
import com.zelix.framework.attributes.declaration.MethodParametersAttribute;
import com.zelix.framework.attributes.declaration.SignatureAttribute;
import com.zelix.framework.code.InstructionBlock;
import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.code.MethodInvocation;
import com.zelix.framework.code.MethodMapping;
import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.constant.IntegerNode;
import com.zelix.framework.constant.LongNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.encryption.IntegerEntry;
import com.zelix.framework.encryption.LongEntry;
import com.zelix.framework.encryption.StringEntry;
import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.instructions.WideConstantPoolInsnNode;
import com.zelix.framework.listener.*;
import com.zelix.framework.reference.IGraphElement;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.reference.ReferenceGraphMap;
import com.zelix.framework.signature.*;
import com.zelix.framework.variable.Indexable;
import com.zelix.framework.variable.LocalVariableOperand;
import com.zelix.framework.variable.LocalVariableSlot;
import com.zelix.framework.variable.LocalVariableTable;
import com.zelix.framework.writer.*;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.framework.writer.reflect.ReflectionMethodPattern;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.ObfuscateConfig;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.config.TrimConfig;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.repository.ClassRepository;
import com.zelix.repository.Hierarchy;
import com.zelix.tools.excludes.DefaultExcludes;
import com.zelix.tools.excludes.ExcludeElement;
import com.zelix.tools.excludes.ExcludeStatementManager;
import com.zelix.tools.flow.Condition;
import com.zelix.tools.integerEncryption.IntegerEncryptionExcludes;
import com.zelix.tools.longEncryption.LongEncryptionExcludes;
import com.zelix.tools.parameterChanger.ParameterChangerExcludes;
import com.zelix.tools.parameterChanger.ParameterManager;
import com.zelix.tools.parameterChanger.ParameterPattern;
import com.zelix.tools.parameterChanger.dataset.ChangedMethod;
import com.zelix.tools.parameterChanger.extra.ExtraParameter;
import com.zelix.tools.parameterChanger.extra.ExtraParameterBuilder;
import com.zelix.tools.references.ObfuscateReferencesBuilder;
import com.zelix.tools.renamer.MethodRenamer;
import com.zelix.tools.renamer.method.AbstractMethodNameGenerator;
import com.zelix.tools.string.StringEncryptionExcludes;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.IntegerCache;
import com.zelix.utils.IntegerUtils;
import com.zelix.utils.collections.*;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.utils.dataset.MemberContext;
import com.zelix.utils.dataset.PriorityEntry;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.PhantomClassManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class MethodNode extends AbstractMethodNode implements ClassMappingChangeListener, ChangeListener {
	private LabelNode injectionAnchorLabel;
	private AnnotationSettings methodAnnotationConfig;

	public MethodNode(
		StructureNode classNode,
		ByteDataBuffer dataBuffer,
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References,
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences,
		MultiMap<StringNode, StringConstantUpdateListener> stringReferences,
		MultiMap<IntegerNode, IntegerConstants> integerReferences,
		MultiMap<LongNode, LongConstants> longReferences,
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences,
		MultiMap<MemberTypeWriter, TypeNode> memberReferences,
		MultiMap<InvokeDynamicCallWriter, IDynamicCallUpdatable> dynamicCallReferences,
		PrintWriter logWriter,
		ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributes
	) throws IOException {
		super(classNode, dataBuffer, utf8References, logWriter);
		this.attributes = new AttributesNode[this.attributeCount];
		MultiMap<Integer, IGraphElement> graphElementsByOffset = new MultiMap<>();
		CodeAttribute codeAttribute = null;

		for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
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
				graphElementsByOffset,
				unknownAttributes
			);
			if (this.attributes[attributeIndex] instanceof CodeAttribute) {
				this.codeAttrIndex = attributeIndex;
				codeAttribute = (CodeAttribute)this.attributes[attributeIndex];
			} else if (this.attributes[attributeIndex] instanceof ExceptionsAttribute) {
				this.exceptionsAttrIndex = attributeIndex;
			}

			if (this.attributes[attributeIndex] instanceof AbstractAnnotationsAttributeNode
				&& ((AbstractAnnotationsAttributeNode)this.attributes[attributeIndex]).getAnnotationByType("java/lang/invoke/MethodHandle$PolymorphicSignature", false) != null) {
				throw new ProcessingException(
					"Cannot open a class with signature polymorphic methods in this version : '"
						+ this.getMethodOwner().getRawSourcePath()
						+ "' : '"
						+ this.getDisplayName()
						+ "'"
				);
			}
		}

		if (codeAttribute != null) {
			codeAttribute.fixLabelOffsets(graphElementsByOffset, logWriter);
		}

		if (this.parameterDescriptors != null && TypeWriter.descriptorToHumanReadable(this.descriptor, true) != null) {
			this.setValid(true);
		} else {
			logWriter.println("ERROR: " + this.getRawSourcePath() + " : Invalid Method Descriptor");
			this.setValid(false);
		}

		if (this.isConstructor()) {
			classNode.subscribe(this);
		}
	}

	public MethodNode(AbstractClassNode owner, UTFStringWriter nameWriter, UTFStringWriter descriptorWriter, AttributesNode[] attributes, int generationTypeFlag) {
		super(owner, nameWriter, descriptorWriter, attributes, generationTypeFlag);

		for (int attributeIndex = 0; attributeIndex < attributes.length; attributeIndex++) {
			if (attributes[attributeIndex] instanceof CodeAttribute) {
				this.codeAttrIndex = attributeIndex;
			} else if (attributes[attributeIndex] instanceof ExceptionsAttribute) {
				this.exceptionsAttrIndex = attributeIndex;
			}

			attributes[attributeIndex].setParent(this);
		}

		this.parameterDescriptors = TypeWriter.parseParameterDescriptors(this.descriptor);
	}

	public boolean writesField(FieldNode field) {
		return this.codeAttrIndex != -1 ? ((CodeAttribute)this.attributes[this.codeAttrIndex]).isFieldWritten(field) : false;
	}

	@Override
	public void collectAttributeReferences(AttributesNode attribute, Set<ClassNode> referencedClasses, Set<ClassNode> signatureClasses, Set<FieldNode> referencedFields, Set<MethodNode> referencedMethods) {
	}

	public void applyReflectionObfuscation(
		List<AbstractMethodNode> reflectedMethods,
		Map<String, AbstractMethodCallWriter> writersByName,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		LocalVariableOperand decryptionLocalVarOperand,
		AbstractWorkspace workspace,
		ClassProvider classProvider,
		String reason,
		boolean insertThisVar
	) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex])
				.applyReflectionObfuscation(reflectedMethods, writersByName, constantPoolManager, typeWriters, decryptionLocalVarOperand, workspace, classProvider, reason, insertThisVar);
		}
	}

	public int getMaxStack() {
		return this.codeAttrIndex != -1 ? ((CodeAttribute)this.attributes[this.codeAttrIndex]).getMaxStack() : 0;
	}

	public int getCodeLengthWithSizeField() {
		return this.codeAttrIndex != -1 ? ((CodeAttribute)this.attributes[this.codeAttrIndex]).getCodeLength() : 0;
	}

	public LabelNode getInjectionAnchorLabel() {
		return this.injectionAnchorLabel;
	}

	public boolean hasMethodConfig() {
		return this.methodAnnotationConfig != null;
	}

	public void collectBootstrapMethods(Set<BootstrapMethodEntry> bootstrapMethods) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).collectBootstrapMethods(bootstrapMethods);
		}
	}

	public void collectLongNodes(Set<LongNode> longNodes) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).collectLongNodes(longNodes);
		}
	}

	public void collectStringNodes(Set<StringNode> stringNodes) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).collectStringNodes(stringNodes);
		}
	}

	public void collectIntegerNodes(Set<IntegerNode> integerNodes) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).collectIntegerNodes(integerNodes);
		}
	}

	@Override
	public void collectClassDependencies(Set<AbstractClassNode> referencedClasses, Set<AbstractClassNode> signatureClasses, Set<AbstractFieldNode> referencedFields, Set<AbstractMethodNode> referencedMethods) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).collectClassDependencies(referencedClasses, signatureClasses, referencedFields, referencedMethods);
		}

		if (this.exceptionsAttrIndex != -1) {
			Enumeration<String> exceptionInternalNames = ((ExceptionsAttribute)this.attributes[this.exceptionsAttrIndex]).getExceptionInternalNames();

			while (exceptionInternalNames.hasMoreElements()) {
				AbstractClassNode exceptionClassNode = ClassHierarchyNode.getClassNodeByInternalName(exceptionInternalNames.nextElement());
				if (exceptionClassNode != null) {
					if (exceptionClassNode.hasInnerClassInfo()) {
						signatureClasses.addAll(exceptionClassNode.getSelfAndAllInnerClasses());
					} else {
						signatureClasses.add(exceptionClassNode);
					}
				}
			}
		}
	}

	public int getMinReturnStackDepth() {
		return this.codeAttrIndex != -1 ? ((CodeAttribute)this.attributes[this.codeAttrIndex]).getMinReturnStackDepth() : -1;
	}

	@Override
	public InstructionListNode getInstructionList() {
		return this.codeAttrIndex != -1 ? ((CodeAttribute)this.attributes[this.codeAttrIndex]).getInstructionList() : null;
	}

	public TypeWriter getFirstLdcConstant() {
		return this.codeAttrIndex != -1 ? ((CodeAttribute)this.attributes[this.codeAttrIndex]).getFirstLdcConstant() : null;
	}

	public void encryptStringConstantsInPlace(NestedMultiMap<ClassNode, MethodNode, StringNode, PriorityEntry<WritableType>> stringConstantEntries, Set<StringNode> excludedStrings) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).encryptStringConstantsInPlace(stringConstantEntries, this, excludedStrings);
		}
	}

	public boolean insertDummyBranches(
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy,
		List<String> exceptionTypes,
		List<ClassInfoWriter> runtimeExceptionClasses,
		List<TypeWriter> typeWriters,
		MethodMapping methodMapping,
		boolean exceptionObfuscation,
		boolean avoidMonitors,
		PhantomClassManager phantomClassManager,
		ScriptContext scriptContext
	) {
		boolean branchesInserted = false;
		ArrayList<ClassInfoWriter> exceptionClasses = null;
		if (this.codeAttrIndex != -1) {
			if (this.methodAnnotationConfig != null && this.methodAnnotationConfig.containsKey("exceptionObfuscation")) {
				String exceptionObfuscationSetting = this.methodAnnotationConfig.get("exceptionObfuscation");
				int exceptionObfuscationConfigId = ObfuscateConfig.getExceptionConfigID(exceptionObfuscationSetting);
				if (exceptionObfuscationConfigId != (exceptionObfuscation ? 2 : 1)) {
					String exceptionObfuscationConfigName = ObfuscateConfig.getExceptionConfigNameByID(exceptionObfuscation ? 2 : 1);
					if (scriptContext.isVerbose()) {
						scriptContext.getWriter()
							.println(
								"Overriding Exception Obfuscation setting in method '"
									+ ExcludeStatementManager.getFormattedMethodSignature(this, true, classHierarchy)
									+ "' in class '"
									+ ExcludeStatementManager.getFormattedClassSignature(this.getMethodOwner(), classHierarchy, true)
									+ "' : '"
									+ exceptionObfuscationConfigName
									+ "' changed to '"
									+ exceptionObfuscationSetting
									+ "'"
							);
					}

					switch (exceptionObfuscationConfigId) {
						case 0:
							return false;
						case 1:
							exceptionObfuscation = false;
							break;
						case 2:
							exceptionObfuscation = true;
					}
				}
			}

			if (this.exceptionsAttrIndex != -1) {
				ExceptionsAttribute exceptionsAttribute = (ExceptionsAttribute)this.attributes[this.exceptionsAttrIndex];
				exceptionClasses = new ArrayList<>(exceptionsAttribute.getExceptionCount());
				Enumeration<ClassInfoWriter> exceptionClassEnumeration = exceptionsAttribute.getExceptionClasses();

				while (exceptionClassEnumeration.hasMoreElements()) {
					exceptionClasses.add(exceptionClassEnumeration.nextElement());
				}
			}

			branchesInserted = ((CodeAttribute)this.attributes[this.codeAttrIndex]).insertDummyBranches(typeResolver, classHierarchy, exceptionTypes, runtimeExceptionClasses, exceptionClasses, typeWriters, methodMapping, exceptionObfuscation, avoidMonitors, phantomClassManager);
		}

		return branchesInserted;
	}

	@Override
	public void updateAttributeIndexes() {
		this.codeAttrIndex = -1;
		this.exceptionsAttrIndex = -1;

		for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
			if (this.attributes[attributeIndex] instanceof CodeAttribute) {
				this.codeAttrIndex = attributeIndex;
			} else if (this.attributes[attributeIndex] instanceof ExceptionsAttribute) {
				this.exceptionsAttrIndex = attributeIndex;
			}
		}
	}

	public final void syncAnnotationsAndPruneSignature(ScriptContext scriptContext, boolean pruneSignatureIfChanged) {
		for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
			if (this.attributes[attributeIndex] instanceof AnnotationContainer) {
				((AnnotationContainer)this.attributes[attributeIndex]).syncNames();
			} else if (this.attributes[attributeIndex] instanceof CodeAttribute) {
				((CodeAttribute)this.attributes[attributeIndex]).syncAnnotationNames();
			}
		}

		if (pruneSignatureIfChanged && (this.isParameterDescriptorChanged() || this.isParameterObfuscationMarked())) {
			ArrayList<AttributesNode> retainedAttributes = new ArrayList<>();

			for (int retainedAttributeIndex = 0; retainedAttributeIndex < this.attributes.length; retainedAttributeIndex++) {
				if (this.attributes[retainedAttributeIndex] instanceof SignatureAttribute) {
					if (scriptContext.isVerbose()) {
						scriptContext.logMessage(
							"Generic signature information removed from method '"
								+ this.getSignature()
								+ "' in class '"
								+ this.getMethodOwner().getClassName()
								+ "' because its parameter list has been changed or obfuscated."
						);
					}
				} else {
					retainedAttributes.add(this.attributes[retainedAttributeIndex]);
				}
			}

			if (retainedAttributes.size() < this.attributeCount) {
				this.attributes = retainedAttributes.toArray(new AttributesNode[retainedAttributes.size()]);
				this.attributeCount = this.attributes.length;
				this.updateAttributeIndexes();
			}
		}
	}

	public boolean hasLongConstants() {
		return this.codeAttrIndex != -1 ? ((CodeAttribute)this.attributes[this.codeAttrIndex]).hasLongConstants() : false;
	}

	public int[] insertInstructionsAtOffset(List<AbstractInsnNode> instructions, int localSlotDelta, int maxStack, int insertIndex, String reason, List<Integer> localSlotsToMove) {
		return this.codeAttrIndex != -1 ? ((CodeAttribute)this.attributes[this.codeAttrIndex]).insertInstructionsAtOffset(instructions, localSlotDelta, maxStack, insertIndex, reason, localSlotsToMove) : null;
	}

	public void collectExcludedLongConstants(Set<LongNode> excludedLongs, LongEncryptionExcludes encryptionExcludes) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).collectExcludedLongNodes(excludedLongs, encryptionExcludes);
		}
	}

	public void obfuscateMethodParameters(
		ChangedMethod changedMethod,
		Map<AbstractClassNode, ChangedMethod> changedMethodsByClass,
		Map<MethodNode, ParameterPattern> parameterPatterns,
		Map<MethodNode, Long> parameterKeys,
		Set<MethodNode> parameterObfuscatedMethods,
		Map<MethodNode, LocalVariableOperand> keyOperands,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy,
		List<TypeWriter> typeWriters,
		Condition condition,
		Random random,
		ConstantPoolManager constantPoolManager,
		Hierarchy hierarchy,
		Set<MethodNode> parameterChangedMethods,
		ScriptContext scriptContext
	) {
		if (this.codeAttrIndex != -1) {
			boolean parameterObfuscated = parameterObfuscatedMethods.contains(this);
			((CodeAttribute)this.attributes[this.codeAttrIndex])
				.obfuscateMethodParameters(changedMethod, changedMethodsByClass, parameterPatterns, parameterKeys, parameterObfuscated, keyOperands, typeResolver, classHierarchy, typeWriters, condition, random, constantPoolManager, hierarchy, parameterChangedMethods, scriptContext);
		}
	}

	public boolean hasLocalVariableTable() {
		return this.codeAttrIndex != -1 ? ((CodeAttribute)this.attributes[this.codeAttrIndex]).hasLocalVariableTable() : false;
	}

	public boolean hasMethodParametersAttribute() {
		for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
			if (this.attributes[attributeIndex] instanceof MethodParametersAttribute) {
				return true;
			}
		}

		return false;
	}

	public void pruneMethodParameters(boolean trimNonPublic, boolean trimRenamed) {
		for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
			if (this.attributes[attributeIndex] instanceof MethodParametersAttribute) {
				if (trimNonPublic && !this.isPublic() && !this.isProtected()) {
					this.removeMethodParameters();
				} else if (trimRenamed && this.isNameChanged()) {
					this.removeMethodParameters();
				}
			}
		}
	}

	public void processLocalVariableTables(boolean trimNonPublic, boolean trimRenamed) {
		if (this.codeAttrIndex != -1) {
			CodeAttribute codeAttribute = (CodeAttribute)this.attributes[this.codeAttrIndex];
			if (trimNonPublic && !this.isPublic() && !this.isProtected()) {
				codeAttribute.removeLocalVariableTables();
			} else if (trimRenamed) {
				if (!this.isNameChanged()) {
					codeAttribute.syncLocalVariableTableSlots();
				} else {
					codeAttribute.removeLocalVariableTables();
				}
			} else {
				codeAttribute.syncLocalVariableTableSlots();
			}
		}
	}

	@Override
	public void collectInnerClassMethodCalls(Set<AbstractMethodNode> collectedMethods, Set<AbstractClassNode> innerClasses) {
		if (collectedMethods.add(this) && this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).collectInnerClassMethodCalls(collectedMethods, innerClasses);
		}
	}

	@Override
	public AbstractClassNode getClassNode() {
		return this.getMethodOwner();
	}

	public boolean hasStringConstants() {
		return this.codeAttrIndex != -1 ? ((CodeAttribute)this.attributes[this.codeAttrIndex]).hasStringConstants() : false;
	}

	public void collectRenameDependencies(Set<ClassNode> renamedClasses, Set<ClassNode> signatureClasses, Set<FieldNode> renamedFields, Set<MethodNode> renamedMethods) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).collectRenameDependencies(renamedClasses, signatureClasses, renamedFields, renamedMethods);
		}

		if (this.exceptionsAttrIndex != -1) {
			ClassNode methodOwner = this.getMethodOwner();
			Enumeration<String> exceptionInternalNames = ((ExceptionsAttribute)this.attributes[this.exceptionsAttrIndex]).getExceptionInternalNames();

			while (exceptionInternalNames.hasMoreElements()) {
				ClassNode exceptionClassNode = ClassHierarchyNode.getWorkspaceClassNode(exceptionInternalNames.nextElement());
				if (exceptionClassNode != null) {
					if (methodOwner.isInnerClass() && exceptionClassNode.hasInnerClasses()) {
						exceptionClassNode = (ClassNode)exceptionClassNode.findInnerClassByOrdinal(methodOwner.getInnerClassOrdinal());
					}

					signatureClasses.add(exceptionClassNode);
				}
			}
		}
	}

	public void injectLongXorDecryption(
		ChangedMethod changedMethod, Long xorKey, AtomicReference<LocalVariableSlot> localSlotRef, List<TypeWriter> typeWriters, ConstantPoolManager constantPoolManager, String reason, int keyLocalSlot
	) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).injectLongXorDecryption(changedMethod, xorKey, localSlotRef, typeWriters, constantPoolManager, reason, keyLocalSlot);
		}
	}

	public int getParamIndexBySlot(int slot) {
		int currentSlot = 0;
		List<String> parameterTypes = TypeWriter.parseParameterDescriptors(this.descriptor);
		int parameterCount = parameterTypes.size();

		for (int parameterIndex = 0; parameterIndex < parameterCount; parameterIndex++) {
			if (currentSlot == slot) {
				return parameterIndex;
			}

			String parameterType = parameterTypes.get(parameterIndex);
			if (!parameterType.equals("D") && !parameterType.equals("J")) {
				currentSlot++;
			} else {
				currentSlot += 2;
			}
		}

		return -1;
	}

	public void setCodeModified(boolean modified) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).setModified(modified);
		}
	}

	public void obfuscateControlFlow(
		Set<InstructionListNode> primaryFlowTargets,
		Set<InstructionListNode> secondaryFlowTargets,
		Condition groupCondition,
		Condition packageCondition,
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> memberObfuscationRefs,
		ArrayList<TypeWriter> typeWriters,
		ConstantPoolManager constantPoolManager,
		MultiMap<InstructionListNode, InstructionBlock> instructionBlocks,
		Map<InstructionListNode, MethodAnalyzer> methodAnalyzerCache,
		AbstractWorkspace workspace,
		TypeResolver typeResolver,
		ClassProvider classProvider,
		Random random
	) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).obfuscateControlFlow(primaryFlowTargets, secondaryFlowTargets, groupCondition, packageCondition, memberObfuscationRefs, typeWriters, constantPoolManager, instructionBlocks, methodAnalyzerCache, workspace, typeResolver, classProvider, random);
		}
	}

	@Override
	public final void collectAnnotationReferences(Set<AbstractClassNode> referencedClasses) {
		for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
			if (this.attributes[attributeIndex] instanceof AnnotationContainer) {
				((AnnotationContainer)this.attributes[attributeIndex]).collectClassReferences(referencedClasses);
			} else if (this.attributes[attributeIndex] instanceof CodeAttribute) {
				((CodeAttribute)this.attributes[attributeIndex]).collectAnnotationClassReferences(referencedClasses);
			}
		}
	}

	public boolean hasLineNumber(int lineNumber) {
		return this.codeAttrIndex != -1 ? ((CodeAttribute)this.attributes[this.codeAttrIndex]).hasLineNumber(lineNumber) : false;
	}

	public void setInjectionAnchorLabel(LabelNode injectionAnchorLabel) {
		this.injectionAnchorLabel = injectionAnchorLabel;
	}

	public void insertDummyCondition(
		Condition condition,
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> memberObfuscationRefs,
		List<TypeWriter> typeWriters,
		TypeResolver typeResolver,
		AbstractWorkspace workspace,
		StaticBlockAnalyzer staticBlockAnalyzer,
		Random random
	) {
		((CodeAttribute)this.attributes[this.codeAttrIndex]).insertDummyCondition(condition, memberObfuscationRefs, typeWriters, typeResolver, workspace, staticBlockAnalyzer, random);
	}

	public int indexOfInstruction(AbstractInsnNode instruction) {
		return this.codeAttrIndex != -1 ? ((CodeAttribute)this.attributes[this.codeAttrIndex]).indexOfInstruction(instruction) : -1;
	}

	@Override
	public String toCode() {
		StringBuffer codeBuffer = new StringBuffer();
		Enumeration<String> annotationTypeNames = this.getAnnotationTypeNames();

		while (annotationTypeNames.hasMoreElements()) {
			codeBuffer.append("   ");
			codeBuffer.append('@');
			codeBuffer.append(GlobalUtils.internalToBinaryName(annotationTypeNames.nextElement()));
			codeBuffer.append(Options.lineSeparator);
		}

		codeBuffer.append("   ");
		Map<Integer, String> localVariableMap = null;
		if (this.codeAttrIndex != -1) {
			localVariableMap = ((CodeAttribute)this.attributes[this.codeAttrIndex]).getLocalVariableMap();
		}

		codeBuffer.append(this.toDeclarationWithMappedLocals(localVariableMap));
		codeBuffer.append(this.buildThrowsClause());
		codeBuffer.append(" {");
		codeBuffer.append(Options.lineSeparator);
		int maxLocals = this.getMaxLocals();

		for (int localSlot = this.getFirstNonParamLocalSlot(); localSlot < maxLocals; localSlot++) {
			codeBuffer.append("      int local" + localSlot + ";");
			codeBuffer.append(Options.lineSeparator);
		}

		codeBuffer.append(Options.lineSeparator);
		StringWriter codeStringWriter = new StringWriter();
		PrintWriter codeWriter = new PrintWriter(codeStringWriter);
		this.printCode(codeWriter, 2);
		codeBuffer.append(codeStringWriter.toString());
		codeBuffer.append("}");
		return codeBuffer.toString();
	}

	public void remapLocalVariables(ConstantPoolManager constantPoolManager, ArrayList<TypeWriter> typeWriters, TypeResolver typeResolver, ClassHierarchy classHierarchy) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).remapLocalVariables(this.getFirstNonParamLocalSlot(), constantPoolManager, typeWriters, typeResolver, classHierarchy);
		}
	}

	public void changeParameterSignature(
		ParameterChangerExcludes parameterChangerExcludes,
		ChangeLogContext changeLogContext,
		ParameterManager parameterManager,
		ExtraParameterBuilder extraParameterBuilder,
		Hierarchy hierarchy,
		ReferenceGraph<AbstractMethodKey, AbstractMethodNode> referenceGraph,
		ClassHierarchyNode classHierarchyNode,
		String classInternalName,
		boolean isInterface,
		int keepInnerClassInfo,
		Map<MethodSignature, MethodNode> methodsBySignature,
		Map<MethodParameterKey, MethodNode> methodsByParameterKey,
		boolean aggressiveRenaming,
		HashMap<String, String> changeLogNames,
		Map<MethodNode, String> methodNewNames
	) {
		this.getMethodSignature();
		ParameterPattern parameterPattern = null;
		ClassNode workspaceClassNode = classHierarchyNode.getClassNodeIfWorkspace();
		boolean changeLogChangeApplied = false;
		if (changeLogContext != null && changeLogContext.hasParameterChangeEntry(this)) {
			changeLogChangeApplied = true;
			AbstractMethodNode baseMethod = hierarchy.getBaseMethodOrDefault(this);
			if (changeLogContext.hasParameterChange(baseMethod)) {
				ExtraParameter[] extraParameters = changeLogContext.getExtraParameters(baseMethod);
				parameterPattern = new ParameterPattern(this.getDescriptor(), extraParameters);
				if (!this.isConstructor()) {
					String newDescriptor = parameterPattern.getDescriptor();
					AbstractMethodKey newMethodKey = (AbstractMethodKey)(aggressiveRenaming ? new MethodSignature(this.methodName, newDescriptor) : new MethodParameterKey(this.methodName, newDescriptor));
					Set<AbstractMethodNode> referencingMethods = referenceGraph.getReferences(newMethodKey);
					if (referencingMethods != null && !referencingMethods.isEmpty()) {
						for (AbstractMethodNode referencingMethod : referencingMethods) {
							if (referencingMethod.isGeneratedMember()) {
								changeLogChangeApplied = false;
								changeLogContext.removeExtraParameters(baseMethod);
								changeLogContext.logWarning(
									"Method '"
										+ this.getSignature()
										+ "' in class '"
										+ this.getClassNameBinary()
										+ "' could not be changed to '"
										+ newMethodKey
										+ "' because of existing method '"
										+ referencingMethod.getSignature()
										+ "' in class '"
										+ referencingMethod.getClassNameBinary()
										+ "'. You may need to distribute this application as a whole. (C)"
								);
							} else {
								if (referencingMethod.isPrivate() || referencingMethod.isStatic()) {
									continue;
								}

								if (!this.isPrivate() && !this.isStatic()) {
									AbstractMethodNode referencingBaseMethod = hierarchy.getBaseMethodOrDefault(referencingMethod);
									if (baseMethod != referencingBaseMethod) {
										changeLogChangeApplied = false;
										changeLogContext.removeExtraParameters(baseMethod);
										changeLogContext.logWarning(
											"Method '"
												+ this.getSignature()
												+ "' in class '"
												+ this.getClassNameBinary()
												+ "' could not be changed to '"
												+ newMethodKey
												+ "' because of existing method '"
												+ referencingMethod.getSignature()
												+ "' in class '"
												+ referencingMethod.getClassNameBinary()
												+ "'"
												+ (!referencingMethod.isNameChanged() && !referencingMethod.isDescriptorChanged() ? "" : " which which has been changed to '" + referencingMethod.getMethodSignature() + "'")
												+ ". You may need to distribute this application as a whole. (D)"
										);
									}
								}
							}

							AbstractClassNode referencingClass = workspaceClassNode.getAnyReferencingClass(referencingMethod);
							if (referencingClass != null) {
								changeLogChangeApplied = false;
								changeLogContext.removeExtraParameters(baseMethod);
								changeLogContext.logWarning(
									"Method '"
										+ this.getSignature()
										+ "' in class '"
										+ this.getClassNameBinary()
										+ "' could not be changed to '"
										+ newMethodKey
										+ "' because of symbolic references to method '"
										+ referencingMethod.getSignature()
										+ "' in class '"
										+ this.getClassNameBinary()
										+ "' that appear in '"
										+ referencingClass.getClassNameBinary()
										+ "'. You may need to distribute this application as a whole. (E)"
								);
							}
						}
					}
				}
			}
		}

		if (!changeLogChangeApplied) {
			if (!this.isStatic()) {
				if (!this.isAbstract() && this.isPrivate()) {
					parameterPattern = extraParameterBuilder.buildPrivateMethodPattern(classHierarchyNode, classInternalName, this, referenceGraph, methodsBySignature, methodsByParameterKey, this.isParameterObfuscationMarked(), aggressiveRenaming);
				} else {
					parameterPattern = extraParameterBuilder.getOrBuildMethodPattern(classHierarchyNode, classInternalName, this, referenceGraph, isInterface, methodsBySignature, methodsByParameterKey, this.isParameterObfuscationMarked(), aggressiveRenaming);
				}
			} else if (!methodNewNames.containsKey(this) && (!this.isPublic() || !this.isStatic() || !this.getMethodSignature().equals(MethodSignature.MAIN_METHOD))) {
				parameterPattern = extraParameterBuilder.buildStaticMethodPattern(classHierarchyNode, classInternalName, this, referenceGraph, isInterface, methodsBySignature, methodsByParameterKey, this.isParameterObfuscationMarked(), aggressiveRenaming);
			}
		}

		MethodSignature newMethodSignature = parameterManager.buildSignatureWithPattern(this, parameterPattern);
		if (parameterPattern != null && !parameterPattern.getDescriptor().equals(this.getDescriptor())) {
			referenceGraph.addReference((AbstractMethodKey)(aggressiveRenaming ? newMethodSignature : newMethodSignature.getParameterKey()), this);
			methodsBySignature.put(newMethodSignature, this);
			methodsByParameterKey.put(newMethodSignature.getParameterKey(), this);
			this.setDescriptor(parameterPattern.getDescriptor());
			this.setParameterDescriptorChanged(true);
		} else {
			referenceGraph.addReference(newMethodSignature, this);
			methodsBySignature.put(newMethodSignature, this);
			methodsByParameterKey.put(newMethodSignature.getParameterKey(), this);
		}
	}

	@Override
	public final void filterAttributes(TrimExcludes trimExcludes, TrimConfig trimConfig, ScriptContext scriptContext, PrintWriter printWriter) {
		super.filterAttributes(trimExcludes, trimConfig, scriptContext, printWriter);
		if (trimConfig.deleteExceptionAttributes && this.exceptionsAttrIndex != -1) {
			for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
				if (this.attributes[attributeIndex] instanceof AbstractTypeAnnotationsAttributeNode) {
					((AbstractTypeAnnotationsAttributeNode)this.attributes[attributeIndex]).removeThrowsAnnotations();
				}
			}

			ArrayList<AttributesNode> retainedAttributes = new ArrayList<>();

			for (AttributesNode attribute : this.attributes) {
				if (!(attribute instanceof ExceptionsAttribute)) {
					if (attribute instanceof AbstractTypeAnnotationsAttributeNode) {
						if (!((AbstractTypeAnnotationsAttributeNode)attribute).isEmpty()) {
							retainedAttributes.add(attribute);
						}
					} else {
						retainedAttributes.add(attribute);
					}
				}
			}

			if (retainedAttributes.size() < this.attributeCount) {
				this.attributes = retainedAttributes.toArray(new AttributesNode[retainedAttributes.size()]);
				this.attributeCount = this.attributes.length;
				this.updateAttributeIndexes();
			}
		}
	}

	public void encryptIntegerConstants(
		NestedMultiMap<ClassNode, MethodNode, IntegerNode, PriorityEntry<WritableType>> integerConstantEntries,
		Set<IntegerNode> excludedIntegers,
		IntegerEncryptionExcludes encryptionExcludes,
		boolean encryptAllIntegers,
		List<TypeWriter> typeWriters,
		ConstantPoolManager constantPoolManager
	) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).encryptIntegerConstants(integerConstantEntries, this, excludedIntegers, encryptionExcludes, encryptAllIntegers, typeWriters, constantPoolManager);
		}
	}

	public void validateMethodAnalyzer(TypeResolver typeResolver, ClassHierarchy classHierarchy, ScriptContext scriptContext) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).validateMethodAnalyzer(typeResolver, classHierarchy, scriptContext);
		}
	}

	@Override
	public void stripSyntheticAttributes() {
		int previousAttributeCount = this.attributeCount;
		super.stripSyntheticAttributes();
		if (previousAttributeCount != this.attributeCount) {
			this.updateAttributeIndexes();
		}
	}

	public ClassNode getMethodOwner() {
		return (ClassNode)this.getParent();
	}

	public int getMaxLocals() {
		return this.codeAttrIndex != -1 ? ((CodeAttribute)this.attributes[this.codeAttrIndex]).getMaxLocals() : 0;
	}

	@Override
	public final void pruneAnnotations() {
		for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
			if (this.attributes[attributeIndex] instanceof AnnotationContainer) {
				((AnnotationContainer)this.attributes[attributeIndex]).updateConstants();
			} else if (this.attributes[attributeIndex] instanceof CodeAttribute) {
				((CodeAttribute)this.attributes[attributeIndex]).pruneAnnotations();
			}
		}
	}

	public int getInvokeStackShift() {
		int stackShift = 0;
		if (!this.isStatic()) {
			stackShift++;
		}

		if (!TypeWriter.getReturnDescriptor(this.descriptor).equals("V")) {
			stackShift--;
		}

		return stackShift;
	}

	public void encryptLowValueLongs(
		NestedMultiMap<ClassNode, MethodNode, LongNode, PriorityEntry<WideConstantPoolInsnNode>> longConstantEntries,
		Set<LongNode> excludedLongs,
		LongEncryptionExcludes encryptionExcludes,
		List<TypeWriter> typeWriters,
		ConstantPoolManager constantPoolManager
	) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).encryptLowValueLongs(longConstantEntries, this, excludedLongs, encryptionExcludes, typeWriters, constantPoolManager);
		}
	}

	public void removeMethodConfigAnnotation(ScriptContext scriptContext) {
		RuntimeInvisibleAnnotationsAttribute methodConfigAttribute = this.methodAnnotationConfig.getAttribute();
		ArrayList<AttributesNode> retainedAttributes = new ArrayList<>(this.attributeCount);

		for (AttributesNode attribute : this.attributes) {
			if (attribute == methodConfigAttribute) {
				methodConfigAttribute.removeAnnotationsByType("com/zelix/annotation/ZKMMethodLevel", scriptContext);
				if (methodConfigAttribute.getAnnotationCount() > 0) {
					retainedAttributes.add(attribute);
				}
			} else {
				retainedAttributes.add(attribute);
			}
		}

		if (retainedAttributes.size() < this.attributeCount) {
			this.attributes = retainedAttributes.toArray(new AttributesNode[retainedAttributes.size()]);
			this.attributeCount = this.attributes.length;
			this.updateAttributeIndexes();
		}
	}

	@Override
	public final void filterCodeAttribute(TrimExcludes trimExcludes, TrimConfig trimConfig, AttributesNode attribute, ScriptContext scriptContext, PrintWriter printWriter) {
		if (attribute instanceof CodeAttribute) {
			((CodeAttribute)attribute).trimUnknownAttributes(trimExcludes, trimConfig, scriptContext, printWriter);
		}
	}

	public void collectMethodCalls(MultiMap<AbstractMethodNode, InstructionListNode> callSitesByMethod) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).collectMethodCalls(callSitesByMethod);
		}
	}

	public void buildReferenceGraph(
		ReferenceGraphMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> referenceGraphMap, ObfuscateReferencesBuilder referencesBuilder, TypeResolver typeResolver, ClassHierarchy classHierarchy
	) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).buildReferenceGraph(referenceGraphMap, referencesBuilder, typeResolver, classHierarchy);
		}
	}

	public boolean removeDeadCode(TypeResolver typeResolver, AbstractWorkspace workspace, boolean verbose, ScriptContext scriptContext) {
		boolean deadCodeRemoved = false;
		if (this.codeAttrIndex != -1) {
			deadCodeRemoved = ((CodeAttribute)this.attributes[this.codeAttrIndex]).removeDeadCode(typeResolver, workspace, verbose, scriptContext);
		}

		return deadCodeRemoved;
	}

	public void collectSerialPersistentFields(List<FieldNode> serialFields, AbstractWorkspace workspace) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).collectSerialPersistentFields(serialFields, workspace);
		} else {
			throw new ProcessingException("No Code attribute  found.");
		}
	}

	public boolean handleAutoReflection(
		List<MethodInvocation> methodInvocations,
		String classInternalName,
		List<TypeWriter> typeWriters,
		boolean hasRenamedNames,
		boolean hasRenamedFields,
		boolean hasRenamedMethods,
		TypeResolver typeResolver,
		AbstractWorkspace workspace,
		boolean verbose,
		ScriptContext scriptContext
	) {
		return this.codeAttrIndex != -1
			? ((CodeAttribute)this.attributes[this.codeAttrIndex]).handleAutoReflection(methodInvocations, classInternalName, typeWriters, hasRenamedNames, hasRenamedFields, hasRenamedMethods, typeResolver, workspace, verbose, scriptContext)
			: false;
	}

	public void renameMethodWithHierarchy(
		DefaultExcludes defaultExcludes,
		ParameterChangerExcludes parameterChangerExcludes,
		ChangeLogContext changeLogContext,
		MethodRenamer methodRenamer,
		Hierarchy hierarchy,
		AbstractMethodNameGenerator nameGenerator,
		Map<MethodSignature, AbstractMethodNode> processedMethodsBySignature,
		MultiMap<String, String> libraryNamesByDescriptor,
		ClassHierarchyNode classHierarchyNode,
		String classInternalName,
		boolean isInterface,
		int keepInnerClassInfo,
		Map<MethodSignature, MethodNode> localMethodsBySignature,
		Map<MethodParameterKey, AbstractMethodNode> processedMethodsByParamKey,
		Map<MethodParameterKey, AbstractMethodNode> localMethodsByParamKey,
		MethodNameMap<String, AbstractMethodNode> interfaceMethodNames,
		MethodNameMap<String, AbstractMethodNode> classMethodNames,
		Set<String> usedNames,
		boolean aggressiveRenaming,
		HashMap<String, String> changeLogNames,
		Map<MethodNode, String> methodNewNames
	) {
		if (!this.isConstructor() && !this.isClinit()) {
			String newMethodName = null;
			ClassNode workspaceClassNode = classHierarchyNode.getClassNodeIfWorkspace();
			MethodSignature oldMethodSignature = this.getMethodSignature();
			if (!defaultExcludes.isMethodExcluded(this)) {
				AbstractMethodNode baseMethod = hierarchy.getBaseMethod(this);
				String baseMethodInternalName;
				if (baseMethod != null) {
					baseMethodInternalName = baseMethod.getInternalName();
				} else {
					baseMethodInternalName = classInternalName;
				}

				boolean changeLogRenameApplied = false;
				if (changeLogContext != null && changeLogContext.hasProcessedMethodMapping(baseMethodInternalName, oldMethodSignature)) {
					changeLogRenameApplied = true;
					newMethodName = changeLogContext.getNewMethodName(baseMethodInternalName, oldMethodSignature);
					if (newMethodName != null) {
						if (changeLogContext.isParameterObfuscatedMethod(baseMethod != null && baseMethod.isWorkspaceNode() ? (MethodNode)baseMethod : this)) {
							if (nameGenerator.isParameterObfuscated(this)) {
								this.setParameterObfuscationMarked(true);
							} else {
								String ownerClassDisplay = ExcludeStatementManager.formatClassForDisplay(workspaceClassNode, defaultExcludes);
								changeLogContext.logWarning(
									"Method '"
										+ this.toDeclarationStringSimple(false)
										+ "' in class '"
										+ ownerClassDisplay
										+ "' could not be parameter obfuscated as specified. You may need to distribute this application as a whole. (F)"
								);
							}
						}

						AbstractMethodNode existingMethod;
						AbstractMethodKey newMethodKey;
						if (aggressiveRenaming) {
							newMethodKey = new MethodSignature(newMethodName, this.getDescriptor());
							existingMethod = processedMethodsBySignature.get(newMethodKey);
						} else {
							newMethodKey = new MethodParameterKey(newMethodName, this.getDescriptor());
							existingMethod = processedMethodsByParamKey.get(newMethodKey);
						}

						if (existingMethod != null && oldMethodSignature.equals(existingMethod.getMethodSignature())) {
							AbstractClassNode referencingClass = workspaceClassNode.getAnyReferencingClass(existingMethod);
							if (referencingClass != null) {
								changeLogRenameApplied = false;
								String originalMethodName = methodRenamer.getOriginalMethodName(existingMethod.getInternalName(), newMethodKey);
								String declaringClassDisplay = ExcludeStatementManager.formatClassForDisplay(workspaceClassNode, defaultExcludes);
								String referencingClassDisplay = ExcludeStatementManager.formatClassForDisplay(referencingClass, defaultExcludes);
								String existingMethodClassDisplay = ExcludeStatementManager.formatClassForDisplay(existingMethod.getClassNode(), defaultExcludes);
								String declaringClassInternalName;
								if (baseMethod != null) {
									declaringClassInternalName = baseMethod.getClassNode().getInternalName();
								} else {
									declaringClassInternalName = this.getInternalName();
								}

								changeLogContext.removeProcessedMethodMapping(declaringClassInternalName, oldMethodSignature);
								changeLogContext.logWarning(
									"Method '"
										+ this.toDeclarationStringSimple(false)
										+ "' in class '"
										+ declaringClassDisplay
										+ "' could not be renamed to '"
										+ newMethodName
										+ "' because symbolic references to method '"
										+ existingMethodClassDisplay
										+ "."
										+ originalMethodName
										+ "' appear as '"
										+ referencingClassDisplay
										+ "."
										+ newMethodName
										+ "'. You may need to distribute this application as a whole. (B)"
								);
							}
						}
					}
				}

				if (!changeLogRenameApplied) {
					if (this.isSynthetic()
						&& (keepInnerClassInfo == 0 || keepInnerClassInfo == 2 && !workspaceClassNode.isNameChanged() && !this.isDescriptorChanged())
						&& this.methodName.startsWith("access$")
						&& IntegerUtils.isInteger(this.methodName.substring(7))) {
						localMethodsBySignature.put(this.getMethodSignature(), this);
						localMethodsByParamKey.put(this.getMethodParameterKey(), this);
						if (isInterface) {
							interfaceMethodNames.put(this.getNameDisplay(), this);
						}

						classMethodNames.put(this.getNameDisplay(), this);
					} else {
						boolean parameterObfuscated = nameGenerator.isParameterObfuscated(this);
						if (!this.isStatic()) {
							if (!this.isAbstract() && this.isPrivate()) {
								newMethodName = nameGenerator.generatePrivateMethodName(classHierarchyNode, classInternalName, this, processedMethodsBySignature, localMethodsBySignature, processedMethodsByParamKey, localMethodsByParamKey, interfaceMethodNames, classMethodNames, usedNames, aggressiveRenaming, parameterObfuscated);
							} else {
								newMethodName = nameGenerator.generateInheritedMethodName(classHierarchyNode, classInternalName, this, processedMethodsBySignature, isInterface, localMethodsBySignature, processedMethodsByParamKey, localMethodsByParamKey, interfaceMethodNames, classMethodNames, usedNames, aggressiveRenaming, parameterObfuscated);
							}
						} else if (!methodNewNames.containsKey(this)
							&& (!this.isPublic() || !this.isStatic() || !this.getMethodSignature().equals(MethodSignature.MAIN_METHOD))
							&& !Options.ZKM_ALLOW_RENAME_MAIN) {
							newMethodName = nameGenerator.generateMethodName(classHierarchyNode, classInternalName, this, processedMethodsBySignature, libraryNamesByDescriptor, isInterface, localMethodsBySignature, processedMethodsByParamKey, localMethodsByParamKey, interfaceMethodNames, classMethodNames, usedNames, aggressiveRenaming, parameterObfuscated);
						}
					}
				}
			}

			AtomicReference<MethodSignature> previousMethodSignatureRef = new AtomicReference<>();
			MethodSignature newMethodSignature = methodRenamer.registerMethodRename(this, newMethodName, previousMethodSignatureRef);
			if (newMethodName != null && !newMethodName.equals(this.getName())) {
				processedMethodsBySignature.put(newMethodSignature, this);
				localMethodsBySignature.put(newMethodSignature, this);
				processedMethodsByParamKey.put(newMethodSignature.getParameterKey(), this);
				localMethodsByParamKey.put(newMethodSignature.getParameterKey(), this);
				classMethodNames.put(newMethodSignature.getName(), this);
				if (isInterface) {
					interfaceMethodNames.put(newMethodSignature.getName(), this);
				}

				if (!previousMethodSignatureRef.isNull()) {
					MethodSignature previousMethodSignature = previousMethodSignatureRef.get();
					processedMethodsBySignature.put(previousMethodSignature, this);
					localMethodsBySignature.put(previousMethodSignature, this);
					processedMethodsByParamKey.put(previousMethodSignature.getParameterKey(), this);
					localMethodsByParamKey.put(previousMethodSignature.getParameterKey(), this);
				}

				this.setName(newMethodName);
			} else {
				processedMethodsBySignature.put(oldMethodSignature, this);
				localMethodsBySignature.put(oldMethodSignature, this);
				processedMethodsByParamKey.put(oldMethodSignature.getParameterKey(), this);
				localMethodsByParamKey.put(oldMethodSignature.getParameterKey(), this);
				if (isInterface) {
					interfaceMethodNames.put(newMethodSignature.getName(), this);
				}

				classMethodNames.put(this.getNameDisplay(), this);
			}
		}
	}

	public int analyzeControlFlow(
		MultiMap<InstructionListNode, InstructionBlock> instructionBlocks,
		TypeResolver typeResolver,
		ScriptContext scriptContext,
		ClassHierarchy classHierarchy,
		int analysisMode,
		int flowObfuscationConfigId,
		boolean licenseVerified,
		boolean keepBalancedLocks,
		Map<InstructionListNode, MethodAnalyzer> methodAnalyzerCache,
		boolean isParameterChanging,
		Random random
	) {
		if (this.codeAttrIndex != -1) {
			if (this.methodAnnotationConfig != null && this.methodAnnotationConfig.containsKey("obfuscateFlow")) {
				String flowObfuscationSetting = this.methodAnnotationConfig.get("obfuscateFlow");
				int annotationFlowConfigId = ObfuscateConfig.getFlowConfigID(flowObfuscationSetting);
				if (flowObfuscationConfigId != annotationFlowConfigId) {
					String previousFlowConfigName = ObfuscateConfig.getFlowConfigNameByID(flowObfuscationConfigId);
					flowObfuscationConfigId = annotationFlowConfigId;
					if (scriptContext.isVerbose()) {
						scriptContext.getWriter()
							.println(
								"Overriding "
									+ (isParameterChanging ? "Method Parameter Changing's " : "")
									+ "Flow Obfuscation setting in method '"
									+ ExcludeStatementManager.getFormattedMethodSignature(this, true, classHierarchy)
									+ "' in class '"
									+ ExcludeStatementManager.getFormattedClassSignature(this.getMethodOwner(), classHierarchy, true)
									+ "' : '"
									+ previousFlowConfigName
									+ "' changed to '"
									+ flowObfuscationSetting
									+ "'"
							);
					}
				}
			}

			if (flowObfuscationConfigId != 0) {
				return ((CodeAttribute)this.attributes[this.codeAttrIndex]).analyzeControlFlow(instructionBlocks, typeResolver, scriptContext, classHierarchy, analysisMode, flowObfuscationConfigId, licenseVerified, keepBalancedLocks, methodAnalyzerCache, random);
			}
		}

		return 0;
	}

	public CodeAttribute getCodeAttribute() {
		return this.codeAttrIndex != -1 ? (CodeAttribute)this.attributes[this.codeAttrIndex] : null;
	}

	@Override
	public final void resolveAnnotations(AbstractWorkspace workspace, ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext) {
		for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
			if (this.attributes[attributeIndex] instanceof AnnotationContainer) {
				((AnnotationContainer)this.attributes[attributeIndex]).resolveAnnotations(workspace, classProvider, phantomClassManager, scriptContext);
			} else if (this.attributes[attributeIndex] instanceof CodeAttribute) {
				((CodeAttribute)this.attributes[attributeIndex]).resolveAnnotations(workspace, classProvider, phantomClassManager, scriptContext);
			}
		}
	}

	public LocalVariableTable getLocalVariableTable() {
		LocalVariableTable localVariableTable = null;
		if (this.codeAttrIndex != -1) {
			localVariableTable = ((CodeAttribute)this.attributes[this.codeAttrIndex]).getInstructionList().getLocalVariableTable();
		}

		return localVariableTable;
	}

	public void encryptStringConstants(
		List<KeyValuePair<PriorityEntry<WritableType>, StringEntry>> stringConstantEntries,
		Map<LabelNode, AtomicInt> labelCounters,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> labelReplacements,
		Long xorKey,
		LocalVariableOperand xorKeyOperand,
		Indexable localSlotIndex,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy
	) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).encryptStringConstants(stringConstantEntries, this.isClinit(), labelCounters, labelReplacements, xorKey, xorKeyOperand, localSlotIndex, constantPoolManager, typeWriters, typeResolver, classHierarchy);
		}
	}

	public void pruneTypeAnnotations(HashSet<LabelNode> labelNodes, ReferenceGraph<LabelNode, IGraphElement> referenceGraph) {
		for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
			if (this.attributes[attributeIndex] instanceof AbstractTypeAnnotationsAttributeNode) {
				((AbstractTypeAnnotationsAttributeNode)this.attributes[attributeIndex]).removeAnnotationsForLabels(labelNodes, referenceGraph);
			}
		}
	}

	public void encryptLongConstants(
		List<KeyValuePair<PriorityEntry<WideConstantPoolInsnNode>, LongEntry>> longConstantEntries,
		Map<LabelNode, AtomicInt> labelCounters,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> labelReplacements,
		Long xorKey,
		LocalVariableOperand xorKeyOperand,
		Indexable localSlotIndex,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy
	) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).encryptLongConstants(longConstantEntries, this.isClinit(), labelCounters, labelReplacements, xorKey, xorKeyOperand, localSlotIndex, constantPoolManager, typeWriters, typeResolver, classHierarchy);
		}
	}

	public void buildMethodReferenceGraph(ReferenceGraph<MethodNode, MethodNode> graph, Set<MethodNode> visitedMethods) {
		visitedMethods.add(this);
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).buildMethodReferenceGraph(graph, visitedMethods);
		}
	}

	public void encryptIntegerConstantsWithLong(
		List<KeyValuePair<PriorityEntry<WritableType>, IntegerEntry>> integerConstantEntries,
		Map<LabelNode, AtomicInt> labelCounters,
		MultiMap<LabelNode, PriorityEntry<LabelNode>> labelReplacements,
		Long xorKey,
		LocalVariableOperand xorKeyOperand,
		Indexable localSlotIndex,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy
	) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex])
				.encryptIntegerConstantsWithLong(integerConstantEntries, this.isClinit(), labelCounters, labelReplacements, xorKey, xorKeyOperand, localSlotIndex, constantPoolManager, typeWriters, typeResolver, classHierarchy);
		}
	}

	public boolean addTypeAnnotationReferences(ReferenceGraph<LabelNode, IGraphElement> referenceGraph) {
		boolean typeAnnotationFound = false;

		for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
			if (this.attributes[attributeIndex] instanceof AbstractTypeAnnotationsAttributeNode) {
				((AbstractTypeAnnotationsAttributeNode)this.attributes[attributeIndex]).registerAnnotations(referenceGraph);
				typeAnnotationFound = true;
			}
		}

		return typeAnnotationFound;
	}

	@Override
	public boolean isWorkspaceNode() {
		return true;
	}

	public void collectExcludedStringNodes(Set<StringNode> excludedStrings, StringEncryptionExcludes encryptionTargetManager) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).collectExcludedStringNodes(excludedStrings, encryptionTargetManager);
		}
	}

	public void mangleMethodParameterNames() {
		for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
			if (this.attributes[attributeIndex] instanceof MethodParametersAttribute) {
				((MethodParametersAttribute)this.attributes[attributeIndex]).mangleParameterNames();
			}
		}
	}

	public void insertInstructionsAtOffsetSimple(List<AbstractInsnNode> instructions, int localSlotDelta, int maxStack, int insertIndex, String reason) {
		this.insertInstructionsAtOffset(instructions, localSlotDelta, maxStack, insertIndex, reason, null);
	}

	public void refreshLocalVariableTables() {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).obfuscateLocalVariableNames();
		}
	}

	public void removeLineNumberTables() {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).removeLineNumberTables();
		}
	}

	public void removeStackMapAttributes() {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).removeStackMapAttributes();
		}
	}

	public void removeMethodParameters() {
		ArrayList<AttributesNode> retainedAttributes = new ArrayList<>(this.attributes.length);

		for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
			if (!(this.attributes[attributeIndex] instanceof MethodParametersAttribute)) {
				retainedAttributes.add(this.attributes[attributeIndex]);
			}
		}

		if (retainedAttributes.size() < this.attributes.length) {
			AttributesNode[] attributesArray = new AttributesNode[retainedAttributes.size()];
			this.attributes = retainedAttributes.toArray(attributesArray);
			this.attributeCount = this.attributes.length;
			this.updateAttributeIndexes();
		}
	}

	public void removeLocalVariableTables() {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).removeLocalVariableTables();
		}
	}

	public String buildThrowsClause() {
		if (this.exceptionsAttrIndex == -1) {
			return "";
		} else {
			StringBuilder throwsClause = new StringBuilder();
			ExceptionsAttribute exceptionsAttribute = (ExceptionsAttribute)this.attributes[this.exceptionsAttrIndex];
			throwsClause.append(" throws ");
			if (exceptionsAttribute.getExceptionCount() > 0) {
				for (int exceptionIndex = 0; exceptionIndex < exceptionsAttribute.getExceptionCount(); exceptionIndex++) {
					throwsClause.append(exceptionsAttribute.getExceptionClass(exceptionIndex).getExternalName());
					if (exceptionIndex < exceptionsAttribute.getExceptionCount() - 1) {
						throwsClause.append(", ");
					}
				}
			}

			return throwsClause.toString();
		}
	}

	public void printCode(PrintWriter writer, int indent) {
		if (this.codeAttrIndex != -1) {
			CodeAttribute codeAttribute = (CodeAttribute)this.attributes[this.codeAttrIndex];
			codeAttribute.printInstructions(writer, indent);
			codeAttribute.printExceptionEntries(writer, indent);
		}
	}

	public void applyInstructionBlock(InstructionBlock instructionBlock, int maxStack, int maxLocals, String reason) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).applyInstructionBlock(instructionBlock, maxStack, maxLocals, reason);
		}
	}

	public int getMaxLocalsCompat() {
		return this.codeAttrIndex != -1 ? ((CodeAttribute)this.attributes[this.codeAttrIndex]).getMaxLocals() : 0;
	}

	public void pruneSignatureMetadata(int keepGenericsInformation) {
		if (keepGenericsInformation == 1) {
			ArrayList<AttributesNode> retainedAttributes = new ArrayList<>(this.attributes.length);

			for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
				if (!(this.attributes[attributeIndex] instanceof SignatureAttribute) && !(this.attributes[attributeIndex] instanceof BridgeAttribute)) {
					if (this.attributes[attributeIndex] instanceof AbstractTypeAnnotationsAttributeNode) {
						AbstractTypeAnnotationsAttributeNode typeAnnotationsAttribute = (AbstractTypeAnnotationsAttributeNode)this.attributes[attributeIndex];
						typeAnnotationsAttribute.removeSignatureAnnotations();
						if (!typeAnnotationsAttribute.isEmpty()) {
							retainedAttributes.add(this.attributes[attributeIndex]);
						}
					} else {
						if (this.attributes[attributeIndex] instanceof CodeAttribute) {
							((CodeAttribute)this.attributes[attributeIndex]).pruneInvalidTypeAnnotations();
						}

						retainedAttributes.add(this.attributes[attributeIndex]);
					}
				}
			}

			if (retainedAttributes.size() < this.attributes.length) {
				AttributesNode[] attributesArray = new AttributesNode[retainedAttributes.size()];
				this.attributes = retainedAttributes.toArray(attributesArray);
				this.attributeCount = this.attributes.length;
				this.updateAttributeIndexes();
			}

			if (this.isBridge()) {
				this.setBridge(false);
				if (this.isSynthetic()) {
					this.setSynthetic(false);
				}
			}

			if (this.isVarArgs()) {
				this.setVarargs(false);
			}
		}
	}

	public void setMaxLocals(int maxLocals) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).setMaxLocals(maxLocals);
		}
	}

	public boolean hasConditionalJump() {
		return this.codeAttrIndex != -1 ? ((CodeAttribute)this.attributes[this.codeAttrIndex]).hasConditionalJump() : false;
	}

	public void rebuildStackMap(TypeResolver typeResolver, ClassHierarchy classHierarchy, ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).rebuildStackMap(typeResolver, classHierarchy, constantPoolManager, typeWriters);
		}
	}

	public void setExceptionEntries(TryCatchBlockInfo[] exceptionEntries) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).setExceptionEntries(exceptionEntries);
		}
	}

	@Override
	public void onChange(INotifier notifier, Object changeKind, Object changedNode, Object changeArg) {
		if (changeKind instanceof AtomicInt && ((AtomicInt)changeKind).get() == 0 && changedNode instanceof ClassNode) {
			String constructorDisplayName = ((ClassNode)changedNode).getConstructorDisplayName();
			this.setName(constructorDisplayName);
		}
	}

	public void obfuscateStringConcat(
		StringConcatAnalysis stringConcatAnalysis,
		ConstantPoolManager constantPoolManager,
		Map<InvokeDynamicCallWriter, String> dynCallNames,
		MultiMap<InstructionListNode, InstructionBlock> instructionBlocks,
		List<TypeWriter> typeWriters,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy
	) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).obfuscateStringConcat(stringConcatAnalysis, constantPoolManager, dynCallNames, instructionBlocks, typeWriters, typeResolver, classHierarchy);
		}
	}

	public MultiMap<AbstractMethodCallWriter, String> analyzeReflectionCalls(
		Map<AbstractMethodCallWriter, ReflectionMethodPattern> reflectionPatterns,
		ClassRepository classRepository,
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldReadAccesses,
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldWriteAccesses,
		HierarchicalMultiMap<MethodID, InstructionListNode, Integer> methodAccesses,
		TypeResolver typeResolver,
		Map<MethodID, MethodAnalysisResult> methodAnalysisResults,
		Map<FieldID, HashSet<Reference>> fieldReferences,
		Map<ExcludeElement, HashSet<Reference>> stackReferences,
		Map<ExcludeElement, HashSet<Reference>> localVariableReferences,
		AbstractWorkspace workspace,
		Map<InstructionListNode, MethodAnalyzer> methodAnalyzerCache,
		DualKeyMap<AbstractClassNode, Reference, Integer> stringClassReferences,
		MultiMap<String, Reference> stringResourcePathReferences,
		DualKeyMap<AbstractFieldNode, Reference, Reference> stringFieldNameReferences,
		DualKeyMap<AbstractMethodNode, Reference, Reference> stringMethodNameReferences,
		DualKeyMap<String, Reference, Reference> stringInternalNameReferences,
		Set<StringNode> processedStringNodes,
		MultiMap<MethodNode, StructureNode> usageReferences,
		List<MethodInvocation> methodInvocations,
		boolean verbose
	) {
		if (this.codeAttrIndex != -1) {
			ArrayList<StructureNode> referencedStructures = new ArrayList<>();
			MultiMap<AbstractMethodCallWriter, String> reflectionTraces = ((CodeAttribute)this.attributes[this.codeAttrIndex])
				.analyzeReflectionCalls(
					reflectionPatterns, classRepository, fieldReadAccesses, fieldWriteAccesses, methodAccesses, typeResolver, methodAnalysisResults, fieldReferences, stackReferences, localVariableReferences, workspace, methodAnalyzerCache, stringClassReferences, stringResourcePathReferences, stringFieldNameReferences, stringMethodNameReferences, stringInternalNameReferences, processedStringNodes, referencedStructures, methodInvocations, verbose
				);
			if (referencedStructures.size() > 0) {
				usageReferences.put(this, referencedStructures);
			}

			return reflectionTraces;
		} else {
			return null;
		}
	}

	@Override
	public String getDisplayName() {
		return this.toDeclarationStringSimple(false);
	}

	public void recordInvokeOpcodes(HashMap<AbstractMethodCallWriter, Integer> opcodeCounts) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).recordInvokeOpcodes(opcodeCounts);
		}
	}

	public void collectExcludedIntegerNodes(Set<IntegerNode> excludedIntegers, IntegerEncryptionExcludes encryptionExcludes) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).collectExcludedIntegerNodes(excludedIntegers, encryptionExcludes);
		}
	}

	public void updateLineNumbers(ArrayList<LineNumberEntry> lineNumberTables) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).collectLineNumberTables(lineNumberTables);
		}
	}

	public boolean hasIntegerConstants() {
		return this.codeAttrIndex != -1 ? ((CodeAttribute)this.attributes[this.codeAttrIndex]).hasIntegerConstants() : false;
	}

	public void mapMemberAccessInsns(
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldReadAccesses,
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldWriteAccesses,
		HierarchicalMultiMap<MethodID, InstructionListNode, Integer> methodAccesses,
		IntegerCache integerCache
	) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).mapMemberAccessInsns(fieldReadAccesses, fieldWriteAccesses, methodAccesses, integerCache);
		}
	}

	public void rebuildStackMapTable(TypeResolver typeResolver, ClassHierarchy classHierarchy, ConstantPoolManager constantPoolManager, ArrayList<TypeWriter> typeWriters) {
		if (this.codeAttrIndex != -1) {
			((CodeAttribute)this.attributes[this.codeAttrIndex]).rebuildStackMapTable(typeResolver, classHierarchy, constantPoolManager, typeWriters);
		}
	}

	public ArrayList<ClassNode> getDescriptorWorkspaceClasses() {
		ArrayList<ClassNode> workspaceClasses = new ArrayList<>();
		String methodDescriptor = this.getDescriptor();
		List<String> parameterTypes = TypeWriter.parseParameterDescriptors(methodDescriptor);
		String returnDescriptor = TypeWriter.getReturnDescriptor(methodDescriptor);
		parameterTypes.add(returnDescriptor);

		for (int descriptorIndex = 0; descriptorIndex < parameterTypes.size(); descriptorIndex++) {
			String elementInternalName = AbstractClassNode.getArrayElementInternalName(parameterTypes.get(descriptorIndex));
			if (elementInternalName != null) {
				ClassNode workspaceClassNode = ClassHierarchyNode.getWorkspaceClassNode(elementInternalName);
				if (workspaceClassNode != null) {
					workspaceClasses.add(workspaceClassNode);
				}
			}
		}

		return workspaceClasses;
	}

	public void setMethodConfig(AnnotationSettings annotationSet) {
		this.methodAnnotationConfig = annotationSet;
		this.getMethodOwner().markMethodAnnotationConfig();
	}
}
