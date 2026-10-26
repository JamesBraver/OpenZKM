package com.zelix.framework.attributes.code;

import com.zelix.exceptions.DebugAssertFailedException;
import com.zelix.exceptions.InvalidBytecodeException;
import com.zelix.exceptions.MethodAnalyzerException;
import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.*;
import com.zelix.framework.analyzer.MethodAnalyzer;
import com.zelix.framework.analyzer.StackMapTableAnalyzer;
import com.zelix.framework.analyzer.StaticBlockAnalyzer;
import com.zelix.framework.analyzer.StringConcatAnalysis;
import com.zelix.framework.analyzer.dataset.MethodAnalysisResult;
import com.zelix.framework.analyzer.reference.Reference;
import com.zelix.framework.annotation.AnnotationContainer;
import com.zelix.framework.attributes.BootstrapMethodEntry;
import com.zelix.framework.attributes.UnknownAttribute;
import com.zelix.framework.attributes.base.AbstractLocalVariableTableAttribute;
import com.zelix.framework.attributes.base.AbstractStackMapAttribute;
import com.zelix.framework.attributes.base.AbstractTypeAnnotationsAttributeNode;
import com.zelix.framework.attributes.debug.LineNumberEntry;
import com.zelix.framework.attributes.debug.LineNumberTableAttribute;
import com.zelix.framework.attributes.debug.LocalVariableTableAttribute;
import com.zelix.framework.attributes.debug.LocalVariableTypeTableAttribute;
import com.zelix.framework.attributes.stackmap.AbstractStackMapEntry;
import com.zelix.framework.attributes.stackmap.AbstractStackMapFrame;
import com.zelix.framework.attributes.stackmap.FrameTypes;
import com.zelix.framework.attributes.stackmap.StackMapEntry;
import com.zelix.framework.code.InstructionBlock;
import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.code.MethodInvocation;
import com.zelix.framework.code.MethodMapping;
import com.zelix.framework.constant.*;
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
import com.zelix.framework.signature.FieldID;
import com.zelix.framework.signature.MethodID;
import com.zelix.framework.variable.Indexable;
import com.zelix.framework.variable.LocalVariableOperand;
import com.zelix.framework.variable.LocalVariableSlot;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.*;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;
import com.zelix.framework.writer.base.MemberTypeWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.framework.writer.reflect.ReflectionMethodPattern;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.config.TrimConfig;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.repository.ClassRepository;
import com.zelix.repository.Hierarchy;
import com.zelix.tools.excludes.ExcludeElement;
import com.zelix.tools.flow.Condition;
import com.zelix.tools.integerEncryption.IntegerEncryptionExcludes;
import com.zelix.tools.longEncryption.LongEncryptionExcludes;
import com.zelix.tools.parameterChanger.ParameterPattern;
import com.zelix.tools.parameterChanger.dataset.ChangedMethod;
import com.zelix.tools.references.ObfuscateReferencesBuilder;
import com.zelix.tools.string.StringEncryptionExcludes;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.utils.ClassMetrics;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.IntegerCache;
import com.zelix.utils.collections.*;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.ByteDataBuffer;
import com.zelix.utils.dataset.MemberContext;
import com.zelix.utils.dataset.PriorityEntry;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.PhantomClassManager;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class CodeAttribute extends AttributesNode {
	private int maxStack;
	private int maxLocals;
	private InstructionListNode instructionList;
	private int exceptionTableLength;
	private ExceptionEntryNode[] exceptionEntries;
	private int attributeCount;
	private AttributesNode[] attributes;

	public CodeAttribute(
		StructureNode parent,
		int nameIndex,
		String attributeName,
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
		ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributeRefs,
		MultiMap<Integer, IGraphElement> elementsByOffset
	) throws IOException {
		super(parent, nameIndex, attributeName, buf, utf8References);
		this.maxStack = buf.readUnsignedShort();
		this.maxLocals = buf.readUnsignedShort();
		this.instructionList = new InstructionListNode(this, buf, elementsByOffset, stringReferences, integerReferences, longReferences, classReferences, memberReferences, dynamicCallReferences);
		this.exceptionTableLength = buf.readUnsignedShort();
		this.exceptionEntries = new ExceptionEntryNode[this.exceptionTableLength];

		for (int exceptionIndex = 0; exceptionIndex < this.exceptionTableLength; exceptionIndex++) {
			this.exceptionEntries[exceptionIndex] = new ExceptionEntryNode(this, buf, elementsByOffset, classReferences);
		}

		this.attributeCount = buf.readUnsignedShort();
		this.attributes = new AttributesNode[this.attributeCount];

		for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
			this.attributes[attributeIndex] = AttributesNode.parseAttribute(
				this, buf, this.instructionList.getLocalVariableTable(), utf8References, nameAndTypeReferences, stringReferences, integerReferences, longReferences, classReferences, memberReferences, dynamicCallReferences, printWriter, elementsByOffset, unknownAttributeRefs
			);
			if (this.attributes[attributeIndex] instanceof AbstractStackMapAttribute) {
				((ClassNode)this.getClassNode()).setHasStackMapAttribute(true);
			} else if (this.attributes[attributeIndex] instanceof LineNumberTableAttribute) {
				((ClassNode)this.getClassNode()).setHasLineNumberTable(true);
			}
		}
	}

	public CodeAttribute(RawCodeAttribute rawCodeAttribute) throws IOException {
		super(rawCodeAttribute.getParent(), rawCodeAttribute.getNameUtf8(), rawCodeAttribute.getAttributeLength());
		this.maxStack = rawCodeAttribute.getRawMaxStack();
		this.maxLocals = rawCodeAttribute.getRawMaxLocals();
		MultiMap<Integer, IGraphElement> elementsByOffset = new MultiMap<>();
		MultiMap<StringNode, StringConstantUpdateListener> stringReferences = new MultiMap<>();
		MultiMap<IntegerNode, IntegerConstants> integerReferences = new MultiMap<>();
		MultiMap<LongNode, LongConstants> longReferences = new MultiMap<>();
		MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences = new MultiMap<>();
		MultiMap<MemberTypeWriter, TypeNode> memberReferences = new MultiMap<>();
		MultiMap<InvokeDynamicCallWriter, IDynamicCallUpdatable> dynamicCallReferences = new MultiMap<>();
		MultiMap<UTFStringWriter, SimpleReferenceListener> utf8References = new MultiMap<>();
		MultiMap<NameAndTypeWriter, AbstractNamedWriter> nameAndTypeReferences = new MultiMap<>();
		ThreeLevelMultiMap<String, String, UnknownAttribute, Object> unknownAttributeRefs = new ThreeLevelMultiMap<>();
		PrintWriter printWriter = new PrintWriter(new StringWriter());

		try {
			this.instructionList = new InstructionListNode(this, rawCodeAttribute.getRawBytecode(), elementsByOffset, stringReferences, integerReferences, longReferences, classReferences, memberReferences, dynamicCallReferences);
		} catch (IOException ignored) {
		}

		this.exceptionTableLength = rawCodeAttribute.getRawExceptionTableLength();
		this.exceptionEntries = new ExceptionEntryNode[this.exceptionTableLength];
		ByteDataBuffer exceptionTableBuffer = ByteDataBuffer.fromBytes(rawCodeAttribute.getRawExceptionTableData(), false);

		for (int exceptionIndex = 0; exceptionIndex < this.exceptionTableLength; exceptionIndex++) {
			this.exceptionEntries[exceptionIndex] = new ExceptionEntryNode(this, exceptionTableBuffer, elementsByOffset, classReferences);
		}

		this.attributeCount = rawCodeAttribute.getRawAttributeCount();
		this.attributes = new AttributesNode[this.attributeCount];
		ByteDataBuffer attributesBuffer = ByteDataBuffer.fromBytes(rawCodeAttribute.getRawAttributesData(), false);

		for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
			this.attributes[attributeIndex] = AttributesNode.parseAttribute(
				this, attributesBuffer, this.instructionList.getLocalVariableTable(), utf8References, nameAndTypeReferences, stringReferences, integerReferences, longReferences, classReferences, memberReferences, dynamicCallReferences, printWriter, elementsByOffset, unknownAttributeRefs
			);
		}

		this.fixLabelOffsets(elementsByOffset, printWriter);
		this.instructionList.recalculateOffsets();
	}

	public CodeAttribute(UTFStringWriter nameUtf8, int maxStack, int maxLocals, InstructionListNode instructionList, TryCatchBlockInfo[] tryCatchBlocks) {
		super(null, nameUtf8, instructionList.getCodeLengthWithSizeField() + tryCatchBlocks.length * 8);
		this.maxStack = maxStack;
		this.maxLocals = maxLocals;
		this.instructionList = instructionList;
		this.instructionList.setParent(this);
		this.exceptionTableLength = tryCatchBlocks.length;
		this.exceptionEntries = new ExceptionEntryNode[this.exceptionTableLength];

		for (int exceptionIndex = 0; exceptionIndex < this.exceptionTableLength; exceptionIndex++) {
			TryCatchBlockInfo tryCatchBlock = tryCatchBlocks[exceptionIndex];
			this.exceptionEntries[exceptionIndex] = new ExceptionEntryNode(this, tryCatchBlock.getExceptionType(), tryCatchBlock.getStartLabel(), tryCatchBlock.getEndLabel(), tryCatchBlock.getHandlerLabel());
		}

		this.attributeCount = 0;
		this.attributes = new AttributesNode[this.attributeCount];
	}

	public static int computeStackMapEntriesSize(AbstractStackMapEntry[] stackMapEntries) {
		int totalSize = 0;

		for (AbstractStackMapEntry stackMapEntry : stackMapEntries) {
			totalSize += stackMapEntry.getSerializedSize();
		}

		return totalSize;
	}

	public void mapMemberAccessInsns(
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldReadAccesses,
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldWriteAccesses,
		HierarchicalMultiMap<MethodID, InstructionListNode, Integer> methodAccesses,
		IntegerCache integerCache
	) {
		this.instructionList.mapMemberAccessInsns(fieldReadAccesses, fieldWriteAccesses, methodAccesses, integerCache);
	}

	public void obfuscateLocalVariableNames() {
		int numAttributes = this.attributes.length;

		for (int i = 0; i < numAttributes; i++) {
			if (this.attributes[i] instanceof AbstractLocalVariableTableAttribute) {
				((AbstractLocalVariableTableAttribute)this.attributes[i]).obfuscateNames();
			}
		}
	}

	public ExceptionEntryNode[] getExceptionEntries() {
		return this.exceptionEntries;
	}

	public void applyInstructionBlock(InstructionBlock instructionBlock, int maxStack, int maxLocals, String reason) {
		if (this.instructionList != null) {
			if (this.getMaxStack() < maxStack) {
				this.setMaxStack(maxStack);
			}

			if (this.getMaxLocals() < maxLocals) {
				this.setMaxLocals(maxLocals);
			}

			this.instructionList.applySingleInstructionBlock(instructionBlock, reason);
		}
	}

	public void rebuildStackMapTable(TypeResolver typeResolver, ClassHierarchy classHierarchy, ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters) {
		if (this.instructionList != null) {
			MethodAnalyzer methodAnalyzer = this.instructionList.createMethodAnalyzer(typeResolver, classHierarchy, true);
			if (!methodAnalyzer.hasStackMapLabels()) {
				this.instructionList.setModified(false);
				return;
			}

			if (!this.instructionList.isModified() && !Options.ZKM_REBUILD_STACK_MAPS) {
				return;
			}

			StackMapTableAttribute existingStackMapTable = this.getStackMapTableAttribute();
			Set<ClassInfoWriter> classInfoWriters;
			UTFStringWriter nameUtf8;
			if (existingStackMapTable != null) {
				ConstantPoolVisitor constantPoolVisitor = new ConstantPoolVisitor(31);
				existingStackMapTable.acceptVisitor(constantPoolVisitor);
				classInfoWriters = GlobalUtils.newHashSet(constantPoolVisitor.getClassInfos());
				nameUtf8 = existingStackMapTable.getNameUtf8();
			} else {
				nameUtf8 = constantPoolManager.createUtf8String("StackMapTable", typeWriters);
				classInfoWriters = Collections.emptySet();
			}

			StackMapTableAttribute newStackMapTable = new StackMapTableAttribute(this, nameUtf8, 0);
			StackMapTableAnalyzer stackMapTableAnalyzer = new StackMapTableAnalyzer(this.instructionList, methodAnalyzer);
			AbstractStackMapFrame[] chosenFrames;
			if (Options.ZKM_STACK_MAP_ALGORITHM.equals("1") || this.maxLocals == 0 || !methodAnalyzer.isUninitializedUsageConsistent() || false) {
				chosenFrames = stackMapTableAnalyzer.buildFrameStackMapTables(newStackMapTable, constantPoolManager, classInfoWriters, typeWriters, false, false);
			} else if (Options.ZKM_STACK_MAP_ALGORITHM.equals("2")) {
				chosenFrames = stackMapTableAnalyzer.buildFrameStackMapTables(newStackMapTable, constantPoolManager, classInfoWriters, typeWriters, true, false);
			} else if (Options.ZKM_STACK_MAP_ALGORITHM.equals("5") && this.maxLocals > 1 && this.maxLocals <= 20) {
				chosenFrames = stackMapTableAnalyzer.buildMinimalStackMapTables(this.maxLocals, newStackMapTable, constantPoolManager, classInfoWriters, typeWriters);
			} else {
				ArrayList<TypeWriter> utf8StringsNoUninitialized = new ArrayList<>(typeWriters);

				AbstractStackMapFrame[] framesNoUninitialized;
				try {
					framesNoUninitialized = stackMapTableAnalyzer.buildFrameStackMapTables(newStackMapTable, constantPoolManager, GlobalUtils.newHashSet(classInfoWriters), utf8StringsNoUninitialized, false, false);
				} catch (DebugAssertFailedException assertionFailure) {
					throw assertionFailure;
				}

				ArrayList<TypeWriter> utf8StringsWithUninitialized = new ArrayList<>(typeWriters);
				AbstractStackMapFrame[] framesWithUninitialized = stackMapTableAnalyzer.buildFrameStackMapTables(newStackMapTable, constantPoolManager, GlobalUtils.newHashSet(classInfoWriters), utf8StringsWithUninitialized, true, false);
				int sizeNoUninitialized = computeStackMapEntriesSize(framesNoUninitialized);
				int sizeWithUninitialized = computeStackMapEntriesSize(framesWithUninitialized);
				AbstractStackMapFrame lastFrame = framesNoUninitialized[framesNoUninitialized.length - 1];
				FrameTypes frameCategory = lastFrame.getFrameCategory();
				AbstractInsnNode labelInstruction = methodAnalyzer.getNodeForInstructionIndex(lastFrame.getLabelNode().getLabelID())
					.findLastInsn(this.instructionList.getInstructionListCopy());
				if (!Options.ZKM_STACK_MAP_ALGORITHM.equals("3")
					&& frameCategory != FrameTypes.SAME
					&& frameCategory != FrameTypes.SAME_EXTENDED
					&& frameCategory != FrameTypes.SAME_LOCALS_1_STACK_ITEM
					&& frameCategory != FrameTypes.SAME_LOCALS_1_STACK_ITEM_EXTENDED
					&& frameCategory != FrameTypes.CHOP
					&& labelInstruction.isReturn()) {
					ArrayList<TypeWriter> utf8StringsFixedLastFrame = new ArrayList<>(typeWriters);
					AbstractStackMapFrame[] framesFixedLastFrame = stackMapTableAnalyzer.buildFrameStackMapTables(newStackMapTable, constantPoolManager, GlobalUtils.newHashSet(classInfoWriters), utf8StringsFixedLastFrame, false, true);
					int sizeFixedLastFrame = computeStackMapEntriesSize(framesFixedLastFrame);
					if (sizeNoUninitialized < sizeWithUninitialized && sizeNoUninitialized < sizeFixedLastFrame) {
						GlobalUtils.mergeListsAndReportConflicts(utf8StringsNoUninitialized, typeWriters);
						chosenFrames = framesNoUninitialized;
					} else if (sizeWithUninitialized < sizeNoUninitialized && sizeWithUninitialized < sizeFixedLastFrame) {
						GlobalUtils.mergeListsAndReportConflicts(utf8StringsWithUninitialized, typeWriters);
						chosenFrames = framesWithUninitialized;
					} else {
						GlobalUtils.mergeListsAndReportConflicts(utf8StringsFixedLastFrame, typeWriters);
						chosenFrames = framesFixedLastFrame;
					}
				} else if (sizeWithUninitialized < sizeNoUninitialized) {
					GlobalUtils.mergeListsAndReportConflicts(utf8StringsWithUninitialized, typeWriters);
					chosenFrames = framesWithUninitialized;
				} else {
					GlobalUtils.mergeListsAndReportConflicts(utf8StringsNoUninitialized, typeWriters);
					chosenFrames = framesNoUninitialized;
				}
			}

			newStackMapTable.setEntries(chosenFrames);
			if (existingStackMapTable != null) {
				this.attributes[this.getStackMapTableIndex()] = newStackMapTable;
			} else if (newStackMapTable.getEntryCount() > 0) {
				AttributesNode[] newAttributes = new AttributesNode[this.attributeCount + 1];
				System.arraycopy(this.attributes, 0, newAttributes, 0, this.attributeCount);
				newAttributes[this.attributeCount] = newStackMapTable;
				this.attributes = newAttributes;
				this.attributeCount++;
			}

			this.instructionList.setModified(false);
			methodAnalyzer.release();
		}
	}

	public void encryptStringConstants(
		List<KeyValuePair<PriorityEntry<WritableType>, StringEntry>> stringConstantEntries,
		boolean isClinit,
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
		if (this.instructionList != null) {
			int matchingEntryCount = 0;
			if (this.exceptionTableLength > 0) {
				ExceptionEntryNode[] exceptionEntryArray = this.exceptionEntries;
				int entryCount = exceptionEntryArray.length;

				for (int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
					if (exceptionEntryArray[entryIndex].isLabelBased()) {
						matchingEntryCount++;
					}
				}
			}

			int[][] exceptionRanges;
			if (matchingEntryCount > 0) {
				this.instructionList.recalculateOffsets();
				int rangeIndex = 0;
				exceptionRanges = new int[matchingEntryCount][2];

				for (int exceptionIndex = 0; exceptionIndex < this.exceptionTableLength; exceptionIndex++) {
					ExceptionEntryNode exceptionEntry = this.exceptionEntries[exceptionIndex];
					if (exceptionEntry.isLabelBased()) {
						exceptionRanges[rangeIndex][0] = exceptionEntry.getStartLabelId();
						exceptionRanges[rangeIndex][1] = exceptionEntry.getEndLabelId();
						rangeIndex++;
					}
				}
			} else {
				exceptionRanges = new int[0][0];
			}

			this.instructionList.encryptStringConstants(stringConstantEntries, isClinit, labelCounters, exceptionRanges, labelReplacements, xorKey, xorKeyOperand, localSlotIndex, constantPoolManager, typeWriters, typeResolver, classHierarchy);
			if (!isClinit && this.maxLocals < localSlotIndex.get()) {
				this.setMaxLocals(localSlotIndex.get());
			}
		}
	}

	public int[] getLineNumbers() {
		LineNumberTableAttribute lineNumberTable = this.getLineNumberTableAttribute();
		return lineNumberTable != null ? lineNumberTable.getLineNumbers() : new int[0];
	}

	public void buildMethodReferenceGraph(ReferenceGraph<MethodNode, MethodNode> graph, Set<MethodNode> visitedMethods) {
		this.instructionList.buildMethodReferenceGraph(graph, visitedMethods);
	}

	public boolean isConstructor() {
		return ((AbstractMethodNode)this.getParent()).isConstructor();
	}

	public void trimUnknownAttributes(TrimExcludes trimExcludes, TrimConfig trimConfig, ScriptContext scriptContext, PrintWriter printWriter) {
		PrintWriter scriptWriter = scriptContext.getWriter();
		int numAttributes = this.attributes.length;
		ArrayList<AttributesNode> keptAttributes = new ArrayList<>();

		for (int attributeIndex = 0; attributeIndex < numAttributes; attributeIndex++) {
			if (this.attributes[attributeIndex] instanceof UnknownAttribute && trimConfig.deleteUnknownAttributes) {
				UnknownAttribute unknownAttribute = (UnknownAttribute)this.attributes[attributeIndex];
				if (scriptContext.isVerbose()) {
					scriptWriter.println(
						"\tDeleting unknown attribute '"
							+ unknownAttribute.getAttributeName()
							+ "' in method '"
							+ this.getMethodDisplayName()
							+ "' in class "
							+ GlobalUtils.internalToBinaryName(this.getInternalName())
					);
				}

				printWriter.println(
					"Deleting unknown attribute '"
						+ unknownAttribute.getAttributeName()
						+ "' in method '"
						+ this.getMethodDisplayName()
						+ "' in class "
						+ GlobalUtils.internalToBinaryName(this.getInternalName())
				);
			} else if (this.attributes[attributeIndex] instanceof AnnotationContainer && trimConfig.deleteAnnotationAttributes) {
				AnnotationContainer annotationContainer = (AnnotationContainer)this.attributes[attributeIndex];
				if (trimExcludes.hasAnnotationTypeExclusions() && !annotationContainer.trimExcludedAnnotations(trimExcludes, scriptContext, printWriter)) {
					keptAttributes.add(this.attributes[attributeIndex]);
				}
			} else {
				keptAttributes.add(this.attributes[attributeIndex]);
			}
		}

		int keptCount = keptAttributes.size();
		if (keptCount < numAttributes) {
			this.attributes = keptAttributes.toArray(new AttributesNode[keptCount]);
		}

		this.attributeCount = this.attributes.length;
		this.length = this.getAttributeLength();
	}

	public void encryptLowValueLongs(
		NestedMultiMap<ClassNode, MethodNode, LongNode, PriorityEntry<WideConstantPoolInsnNode>> longConstantEntries,
		MethodNode methodNode,
		Set<LongNode> excludedLongs,
		LongEncryptionExcludes encryptionExcludes,
		List<TypeWriter> typeWriters,
		ConstantPoolManager constantPoolManager
	) {
		this.instructionList.encryptLowValueLongs(longConstantEntries, methodNode, excludedLongs, encryptionExcludes, typeWriters, constantPoolManager);
	}

	public void removeLineNumberTables() {
		this.removeAttributesOfType(LineNumberTableAttribute.class);
	}

	public void pruneInvalidTypeAnnotations() {
		int numAttributes = this.attributes.length;
		Vector<AttributesNode> validAttributes = new Vector<>();

		for (int attributeIndex = 0; attributeIndex < numAttributes; attributeIndex++) {
			if (!(this.attributes[attributeIndex] instanceof LocalVariableTypeTableAttribute)) {
				if (this.attributes[attributeIndex] instanceof AbstractTypeAnnotationsAttributeNode) {
					AbstractTypeAnnotationsAttributeNode typeAnnotationsAttribute = (AbstractTypeAnnotationsAttributeNode)this.attributes[attributeIndex];
					typeAnnotationsAttribute.removeSignatureAnnotations();
					if (!typeAnnotationsAttribute.isEmpty()) {
						validAttributes.add(this.attributes[attributeIndex]);
					}
				} else {
					validAttributes.add(this.attributes[attributeIndex]);
				}
			}
		}

		int validCount = validAttributes.size();
		if (validCount < numAttributes) {
			this.attributes = new AttributesNode[validCount];
			this.attributes = validAttributes.toArray(this.attributes);
		}

		this.attributeCount = this.attributes.length;
		this.length = this.getAttributeLength();
	}

	public void collectIntegerNodes(Set<IntegerNode> integerNodes) {
		this.instructionList.collectIntegerNodes(integerNodes);
	}

	public void collectRenameDependencies(Set<ClassNode> renamedClasses, Set<ClassNode> signatureClasses, Set<FieldNode> renamedFields, Set<MethodNode> renamedMethods) {
		this.instructionList.collectClassDependenciesForRenaming(renamedClasses, signatureClasses, renamedFields, renamedMethods);
		AbstractClassNode classNode = this.getClassNode();

		for (int exceptionIndex = 0; exceptionIndex < this.exceptionEntries.length; exceptionIndex++) {
			ClassNode exceptionClass = ClassHierarchyNode.getWorkspaceClassNode(this.exceptionEntries[exceptionIndex].getExceptionName());
			if (exceptionClass != null) {
				if (classNode.isInnerClass() && exceptionClass.hasInnerClasses()) {
					exceptionClass = (ClassNode)exceptionClass.findInnerClassByOrdinal(classNode.getInnerClassOrdinal());
				}

				renamedClasses.add(exceptionClass);
			}
		}
	}

	public void collectClassDependencies(Set<AbstractClassNode> referencedClasses, Set<AbstractClassNode> signatureClasses, Set<AbstractFieldNode> referencedFields, Set<AbstractMethodNode> referencedMethods) {
		this.instructionList.collectClassDependencies(referencedClasses, signatureClasses, referencedFields, referencedMethods);

		for (int exceptionIndex = 0; exceptionIndex < this.exceptionEntries.length; exceptionIndex++) {
			AbstractClassNode exceptionClass = ClassHierarchyNode.getClassNodeByInternalName(this.exceptionEntries[exceptionIndex].getExceptionName());
			if (exceptionClass != null) {
				if (exceptionClass.hasInnerClassInfo()) {
					referencedClasses.addAll(exceptionClass.getSelfAndAllInnerClasses());
				} else {
					referencedClasses.add(exceptionClass);
				}
			}
		}
	}

	public void recordInvokeOpcodes(HashMap<AbstractMethodCallWriter, Integer> opcodeCounts) {
		if (this.instructionList != null) {
			this.instructionList.recordInvokeOpcode(opcodeCounts);
		}
	}

	public void collectMethodCalls(MultiMap<AbstractMethodNode, InstructionListNode> callSitesByMethod) {
		this.instructionList.collectMethodCalls(callSitesByMethod);
	}

	public boolean insertDummyBranches(
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy,
		List<String> exceptionTypes,
		List<ClassInfoWriter> runtimeExceptionClasses,
		List<ClassInfoWriter> configuredExceptionClasses,
		List<TypeWriter> typeWriters,
		MethodMapping methodMapping,
		boolean exceptionObfuscation,
		boolean avoidMonitors,
		PhantomClassManager phantomClassManager
	) {
		boolean branchesInserted = false;
		if (this.instructionList != null) {
			ArrayList<ExceptionEntryNode> addedExceptionEntries = new ArrayList<>();
			ArrayList<InstructionBlock> instructionBlocks = new ArrayList<>();
			this.instructionList
				.insertDummyBranches(typeResolver, classHierarchy, exceptionTypes, runtimeExceptionClasses, configuredExceptionClasses, new ArrayList<>(new ArraySet<>(this.exceptionEntries)), typeWriters, addedExceptionEntries, instructionBlocks, exceptionObfuscation, avoidMonitors, phantomClassManager);
			if (instructionBlocks.size() > 0 || addedExceptionEntries.size() > 0) {
				methodMapping.putNewPair(this.instructionList, instructionBlocks, addedExceptionEntries);
				branchesInserted = true;
			}
		}

		return branchesInserted;
	}

	public boolean hasLocalVariableTable() {
		int numAttributes = this.attributes.length;

		for (int attributeIndex = 0; attributeIndex < numAttributes; attributeIndex++) {
			if (this.attributes[attributeIndex] instanceof AbstractLocalVariableTableAttribute) {
				return true;
			}
		}

		return false;
	}

	public void removeLocalVariableTables() {
		this.removeAttributesOfType(AbstractLocalVariableTableAttribute.class);
	}

	public void remapLocalVariables(int firstNonParamLocalSlot, ConstantPoolManager constantPoolManager, ArrayList<TypeWriter> typeWriters, TypeResolver typeResolver, ClassHierarchy classHierarchy) {
		if (this.instructionList != null) {
			MultiMap<Integer, IGraphElement> labelNodesByOffset = new MultiMap<>();
			MethodAnalyzer methodAnalyzer = null;
			int numAttributes = this.attributes.length;

			for (int attributeIndex = 0; attributeIndex < numAttributes; attributeIndex++) {
				if (this.attributes[attributeIndex] instanceof LocalVariableTableAttribute) {
					LocalVariableTableAttribute localVariableTable = (LocalVariableTableAttribute)this.attributes[attributeIndex];
					if (methodAnalyzer == null) {
						methodAnalyzer = this.instructionList.getMethodAnalyzer(typeResolver, classHierarchy);
					}

					localVariableTable.addLocalVariableEntries(firstNonParamLocalSlot, this.maxLocals, methodAnalyzer, constantPoolManager, typeWriters, this.instructionList.getLocalVariableTable(), labelNodesByOffset);
				}
			}

			if (methodAnalyzer != null) {
				methodAnalyzer.release();
			}

			if (labelNodesByOffset.size() > 0) {
				this.instructionList.insertLabelNodes(labelNodesByOffset);
			}
		}
	}

	public StackMapAttribute getStackMapAttribute() {
		for (int i = 0; i < this.attributes.length; i++) {
			if (this.attributes[i] instanceof StackMapAttribute) {
				return (StackMapAttribute)this.attributes[i];
			}
		}

		return null;
	}

	public int[] insertInstructionsAtOffset(List<AbstractInsnNode> instructions, int localSlotDelta, int maxStack, int insertIndex, String reason, List<Integer> localSlotsToMove) {
		if (this.instructionList != null) {
			if (this.getMaxStack() < maxStack) {
				this.setMaxStack(maxStack);
			}

			int oldMaxLocals = this.getMaxLocals();
			this.setMaxLocals(oldMaxLocals + localSlotDelta);
			return this.instructionList.insertInstructionsAtOffset(instructions, insertIndex, reason, localSlotsToMove);
		} else {
			return null;
		}
	}

	public int getMaxLocals() {
		return this.maxLocals;
	}

	public int getStackMapAttributeIndex() {
		for (int i = 0; i < this.attributes.length; i++) {
			if (this.attributes[i] instanceof StackMapAttribute) {
				return i;
			}
		}

		return -1;
	}

	public final void resolveAnnotations(AbstractWorkspace workspace, ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext) {
		for (int i = 0; i < this.attributes.length; i++) {
			if (this.attributes[i] instanceof AnnotationContainer) {
				((AnnotationContainer)this.attributes[i]).resolveAnnotations(workspace, classProvider, phantomClassManager, scriptContext);
			}
		}
	}

	public void syncLocalVariableTableSlots() {
		List<String> parameterTypeDescriptors = this.getParameterTypeDescriptors();
		if (parameterTypeDescriptors != null && parameterTypeDescriptors.size() > 0) {
			int slotIndex = this.isStatic() ? 0 : 1;
			HashSet<Integer> retainedSlots = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(parameterTypeDescriptors.size()));

			for (int parameterIndex = 0; parameterIndex < parameterTypeDescriptors.size(); parameterIndex++) {
				String parameterDescriptor = parameterTypeDescriptors.get(parameterIndex);
				retainedSlots.add(INTEGER_CACHE.valueOf(slotIndex));
				slotIndex++;
				if (parameterDescriptor.equals("D") || parameterDescriptor.equals("J")) {
					slotIndex++;
				}
			}

			int numAttributes = this.attributes.length;

			for (int attributeIndex = 0; attributeIndex < numAttributes; attributeIndex++) {
				if (this.attributes[attributeIndex] instanceof AbstractLocalVariableTableAttribute) {
					AbstractLocalVariableTableAttribute localVariableTable = (AbstractLocalVariableTableAttribute)this.attributes[attributeIndex];
					localVariableTable.retainSlots(retainedSlots);
				}
			}
		} else {
			this.removeLocalVariableTables();
		}
	}

	@Override
	public void acceptVisitor(ConstantPoolVisitor visitor) {
		visitor.visit(this.attributeNameUtf8, this, this.getParent());

		for (int exceptionIndex = 0; exceptionIndex < this.exceptionEntries.length; exceptionIndex++) {
			this.exceptionEntries[exceptionIndex].acceptVisitor(visitor);
		}

		for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
			this.attributes[attributeIndex].acceptVisitor(visitor);
		}

		if (this.instructionList != null) {
			this.instructionList.acceptVisitor(visitor);
		}
	}

	public int getStackMapTableIndex() {
		for (int i = 0; i < this.attributes.length; i++) {
			if (this.attributes[i] instanceof StackMapTableAttribute) {
				return i;
			}
		}

		return -1;
	}

	public void validateMethodAnalyzer(TypeResolver typeResolver, ClassHierarchy classHierarchy, ScriptContext scriptContext) {
		if (this.instructionList != null && this.instructionList.isModified()) {
			try {
				this.instructionList.getMethodAnalyzer(typeResolver, classHierarchy);
			} catch (MethodAnalyzerException methodAnalyzerException) {
				scriptContext.logMessage(methodAnalyzerException + " (A)");
			} catch (InvalidBytecodeException invalidBytecodeException) {
				invalidBytecodeException.printStackTrace();
				throw new ProcessingException(invalidBytecodeException.getMessage(), invalidBytecodeException);
			}
		}
	}

	public void collectExcludedLongNodes(Set<LongNode> excludedLongs, LongEncryptionExcludes encryptionExcludes) {
		this.instructionList.collectExcludedLongConstants(excludedLongs, encryptionExcludes);
	}

	public boolean removeDeadCode(TypeResolver typeResolver, ClassHierarchy classHierarchy, boolean verbose, ScriptContext scriptContext) {
		boolean deadCodeRemoved = false;
		if (this.instructionList != null) {
			MethodAnalyzer methodAnalyzer = this.instructionList.getMethodAnalyzer(typeResolver, classHierarchy);
			deadCodeRemoved = this.removeDeadInstructions(methodAnalyzer, verbose, scriptContext);
			if (!methodAnalyzer.isReleased()) {
				methodAnalyzer.release();
			}
		}

		return deadCodeRemoved;
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
		if (this.instructionList != null) {
			this.instructionList.obfuscateControlFlow(primaryFlowTargets, secondaryFlowTargets, groupCondition, packageCondition, memberObfuscationRefs, typeWriters, constantPoolManager, instructionBlocks, methodAnalyzerCache, workspace, typeResolver, classProvider, random);
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
		return this.instructionList != null && this.instructionList.handleAutoReflection(methodInvocations, classInternalName, typeWriters, hasRenamedNames, hasRenamedFields, hasRenamedMethods, typeResolver, workspace, verbose, scriptContext);
	}

	public final void pruneAnnotations() {
		for (int i = 0; i < this.attributes.length; i++) {
			if (this.attributes[i] instanceof AnnotationContainer) {
				((AnnotationContainer)this.attributes[i]).updateConstants();
			}
		}
	}

	public boolean hasIntegerConstants() {
		return this.instructionList.hasIntegerConstants();
	}

	public void obfuscateMethodParameters(
		ChangedMethod changedMethod,
		Map<AbstractClassNode, ChangedMethod> changedMethodsByClass,
		Map<MethodNode, ParameterPattern> parameterPatterns,
		Map<MethodNode, Long> keyOperands,
		boolean isParameterTarget,
		Map<MethodNode, LocalVariableOperand> keyLocalSlots,
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
		if (this.instructionList != null) {
			this.instructionList.obfuscateMethodParameters(changedMethod, changedMethodsByClass, parameterPatterns, keyOperands, isParameterTarget, keyLocalSlots, typeResolver, classHierarchy, typeWriters, condition, random, constantPoolManager, hierarchy, parameterChangedMethods, scriptContext);
		}
	}

	public boolean isStatic() {
		return ((AbstractMethodNode)this.getParent()).isStatic();
	}

	public void collectLongNodes(Set<LongNode> longNodes) {
		this.instructionList.collectLongNodes(longNodes);
	}

	public int getMinReturnStackDepth() {
		return this.instructionList.getMinReturnStackDepth();
	}

	public void reorderExceptionHandlers(boolean exceptionObfuscation, List<ExceptionEntryNode> newExceptionEntries, TypeResolver typeResolver, ClassHierarchy classHierarchy, ScriptContext scriptContext) {
		if (newExceptionEntries.size() > 0) {
			List<ExceptionEntryNode>[] entriesByPosition = new List[this.exceptionEntries.length + 1];

			for (ExceptionEntryNode newEntry : newExceptionEntries) {
				int insertionIndex = this.exceptionEntries.length;
				boolean positionLocked = false;

				for (int exceptionIndex = this.exceptionEntries.length - 1; exceptionIndex >= 0; exceptionIndex--) {
					ExceptionEntryNode existingEntry = this.exceptionEntries[exceptionIndex];
					if (newEntry.isOutsideRange(existingEntry.getOffset(0), existingEntry.getOffset(1))) {
						if (!positionLocked && newEntry.getOffset(0) < existingEntry.getOffset(0)) {
							insertionIndex = exceptionIndex;
						} else {
							positionLocked = true;
						}
					} else {
						if (!newEntry.isInsideRange(existingEntry.getOffset(0), existingEntry.getOffset(1)) || newEntry.getOffset(0) == existingEntry.getOffset(0) && newEntry.getOffset(1) == existingEntry.getOffset(1)) {
							break;
						}

						insertionIndex = exceptionIndex;
					}
				}

				if (entriesByPosition[insertionIndex] == null) {
					entriesByPosition[insertionIndex] = new ArrayList<>();
				}

				entriesByPosition[insertionIndex].add(newEntry);
			}

			LinkedList<ExceptionEntryNode> reorderedEntries = new LinkedList<>();
			Collections.addAll(reorderedEntries, this.exceptionEntries);

			for (int positionIndex = entriesByPosition.length - 1; positionIndex >= 0; positionIndex--) {
				List<ExceptionEntryNode> entriesAtPosition = entriesByPosition[positionIndex];
				if (entriesAtPosition != null) {
					Collections.sort(entriesAtPosition, ExceptionEntryNode.getThrowsComparator());

					for (ExceptionEntryNode entry : entriesAtPosition) {
						if (positionIndex == entriesByPosition.length - 1) {
							reorderedEntries.add(entry);
						} else {
							reorderedEntries.add(positionIndex, entry);
						}
					}
				}
			}

			this.exceptionEntries = reorderedEntries.toArray(new ExceptionEntryNode[reorderedEntries.size()]);
			this.exceptionTableLength = this.exceptionEntries.length;
			new StringBuilder();
			if (exceptionObfuscation && scriptContext.isVerbose()) {
				scriptContext.getWriter()
					.println("\tException obfuscated method '" + ((MethodNode)this.getParent()).getSignature() + "' in class '" + this.getClassNameBinary() + "'");
			}
		}
	}

	public void rebuildStackMap(TypeResolver typeResolver, ClassHierarchy classHierarchy, ConstantPoolManager constantPoolManager, List<TypeWriter> typeWriters) {
		if (this.instructionList != null) {
			MethodAnalyzer methodAnalyzer = this.instructionList.createMethodAnalyzer(typeResolver, classHierarchy, true);
			if (!methodAnalyzer.hasStackMapLabels()) {
				this.instructionList.setModified(false);
				return;
			}

			if (!this.instructionList.isModified() && !Options.ZKM_REBUILD_STACK_MAPS) {
				return;
			}

			StackMapAttribute existingStackMap = this.getStackMapAttribute();
			Set<ClassInfoWriter> classInfoWriters;
			UTFStringWriter nameUtf8;
			if (existingStackMap != null) {
				nameUtf8 = existingStackMap.getNameUtf8();
				ConstantPoolVisitor constantPoolVisitor = new ConstantPoolVisitor(31);
				existingStackMap.acceptVisitor(constantPoolVisitor);
				classInfoWriters = GlobalUtils.newHashSet(constantPoolVisitor.getClassInfos());
			} else {
				nameUtf8 = constantPoolManager.createUtf8String("StackMap", typeWriters);
				classInfoWriters = Collections.emptySet();
			}

			StackMapAttribute newStackMap = new StackMapAttribute(this, nameUtf8, 0);
			StackMapEntry[] chosenEntries;
			if (Options.ZKM_STACK_MAP_ALGORITHM.equals("1")) {
				chosenEntries = methodAnalyzer.buildStackMapEntries(newStackMap, constantPoolManager, classInfoWriters, typeWriters, false);
			} else if (Options.ZKM_STACK_MAP_ALGORITHM.equals("2")) {
				chosenEntries = methodAnalyzer.buildStackMapEntries(newStackMap, constantPoolManager, classInfoWriters, typeWriters, true);
			} else {
				StackMapEntry[] entriesNoUninitialized = methodAnalyzer.buildStackMapEntries(newStackMap, constantPoolManager, classInfoWriters, typeWriters, false);
				StackMapEntry[] entriesWithUninitialized = methodAnalyzer.buildStackMapEntries(newStackMap, constantPoolManager, classInfoWriters, typeWriters, true);
				if (computeStackMapEntriesSize(entriesWithUninitialized) < computeStackMapEntriesSize(entriesNoUninitialized)) {
					chosenEntries = entriesWithUninitialized;
				} else {
					chosenEntries = entriesNoUninitialized;
				}
			}

			newStackMap.setEntries(chosenEntries);
			if (existingStackMap != null) {
				int stackMapIndex = this.getStackMapAttributeIndex();
				this.attributes[stackMapIndex] = newStackMap;
			} else if (newStackMap.getEntryCount() > 0) {
				AttributesNode[] newAttributes = new AttributesNode[this.attributeCount + 1];
				System.arraycopy(this.attributes, 0, newAttributes, 0, this.attributeCount);
				newAttributes[this.attributeCount] = newStackMap;
				this.attributes = newAttributes;
				this.attributeCount++;
			}

			this.instructionList.setModified(false);
			methodAnalyzer.release();
		}
	}

	public int getMaxStack() {
		return this.maxStack;
	}

	public int getCodeLength() {
		return this.instructionList.getCodeLengthWithSizeField();
	}

	public boolean isClinit() {
		return ((AbstractMethodNode)this.getParent()).isClinit();
	}

	public AbstractMethodNode getParentMethod() {
		return (AbstractMethodNode)this.getParent();
	}

	public void injectLongXorDecryption(
		ChangedMethod changedMethod, Long xorKey, AtomicReference<LocalVariableSlot> localSlotRef, List<TypeWriter> typeWriters, ConstantPoolManager constantPoolManager, String reason, int keyLocalSlot
	) {
		this.instructionList.injectLongXorDecryption(changedMethod, xorKey, (MethodNode)this.getParentMethod(), localSlotRef, typeWriters, constantPoolManager, reason, keyLocalSlot);
	}

	public StackMapTableAttribute getStackMapTableAttribute() {
		for (int i = 0; i < this.attributes.length; i++) {
			if (this.attributes[i] instanceof StackMapTableAttribute) {
				return (StackMapTableAttribute)this.attributes[i];
			}
		}

		return null;
	}

	public void collectStringNodes(Set<StringNode> stringNodes) {
		this.instructionList.collectStringNodes(stringNodes);
	}

	public TypeWriter getFirstLdcConstant() {
		return this.instructionList != null ? this.instructionList.getFirstLdcConstant() : null;
	}

	public InstructionListNode getInstructionList() {
		return this.instructionList;
	}

	public final void collectAnnotationClassReferences(Set<AbstractClassNode> classNodes) {
		for (int i = 0; i < this.attributeCount; i++) {
			if (this.attributes[i] instanceof AnnotationContainer) {
				((AnnotationContainer)this.attributes[i]).collectClassReferences(classNodes);
			}
		}
	}

	public void collectInnerClassMethodCalls(Set<AbstractMethodNode> collectedMethods, Set<AbstractClassNode> innerClasses) {
		this.instructionList.collectInnerClassMethodCalls(collectedMethods, innerClasses);
	}

	public int indexOfInstruction(AbstractInsnNode instruction) {
		return this.instructionList != null ? this.instructionList.indexOfInstruction(instruction) : -1;
	}

	@Override
	public void writeAttributeBody(DataOutputStream dataOutputStream) throws IOException {
		super.writeAttributeBody(dataOutputStream);
		dataOutputStream.writeShort(this.maxStack);
		dataOutputStream.writeShort(this.maxLocals);
		this.instructionList.writeInstructions(dataOutputStream);
		dataOutputStream.writeShort(this.exceptionTableLength);

		for (int exceptionIndex = 0; exceptionIndex < this.exceptionTableLength; exceptionIndex++) {
			this.exceptionEntries[exceptionIndex].write(dataOutputStream);
		}

		dataOutputStream.writeShort(this.attributeCount);

		for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
			this.attributes[attributeIndex].writeAttributeBody(dataOutputStream);
		}
	}

	private boolean removeDeadInstructions(MethodAnalyzer methodAnalyzer, boolean verbose, ScriptContext scriptContext) {
		int[] unreachableIndexes = methodAnalyzer.getUnreachableIndexes();
		if (unreachableIndexes.length <= 0) {
			return false;
		} else {
			ReferenceGraph<LabelNode, IGraphElement> referenceGraph = new ReferenceGraph<>();
			LineNumberTableAttribute lineNumberTable = null;
			LocalVariableTableAttribute localVariableTable = null;
			LocalVariableTypeTableAttribute localVariableTypeTable = null;
			StackMapAttribute stackMap = null;
			StackMapTableAttribute stackMapTable = null;
			this.instructionList.registerElementsToGraph(referenceGraph);

			for (int attributeIndex = 0; attributeIndex < this.attributes.length; attributeIndex++) {
				if (this.attributes[attributeIndex] instanceof LineNumberTableAttribute) {
					lineNumberTable = (LineNumberTableAttribute)this.attributes[attributeIndex];
					lineNumberTable.registerEntries(referenceGraph);
				} else if (this.attributes[attributeIndex] instanceof LocalVariableTableAttribute) {
					localVariableTable = (LocalVariableTableAttribute)this.attributes[attributeIndex];
					localVariableTable.registerToGraph(referenceGraph);
				} else if (this.attributes[attributeIndex] instanceof LocalVariableTypeTableAttribute) {
					localVariableTypeTable = (LocalVariableTypeTableAttribute)this.attributes[attributeIndex];
					localVariableTable.registerToGraph(referenceGraph);
				} else if (this.attributes[attributeIndex] instanceof StackMapTableAttribute) {
					stackMapTable = (StackMapTableAttribute)this.attributes[attributeIndex];
					stackMapTable.registerEntries(referenceGraph);
				} else if (this.attributes[attributeIndex] instanceof StackMapAttribute) {
					stackMap = (StackMapAttribute)this.attributes[attributeIndex];
					stackMap.registerEntries(referenceGraph);
				}
			}

			for (int exceptionIndex = 0; exceptionIndex < this.exceptionTableLength; exceptionIndex++) {
				this.exceptionEntries[exceptionIndex].registerToGraph(referenceGraph);
			}

			boolean typeAnnotationRefsFound = ((MethodNode)this.getParent()).addTypeAnnotationReferences(referenceGraph);
			HashSet<LabelNode> unreachableLabels = this.instructionList.collectLabels(unreachableIndexes);
			if (lineNumberTable != null) {
				lineNumberTable.removeEntriesForLabels(unreachableLabels, referenceGraph);
			}

			if (localVariableTable != null) {
				localVariableTable.removeDeadEntries(unreachableLabels, referenceGraph);
			}

			if (localVariableTypeTable != null) {
				localVariableTypeTable.removeDeadEntries(unreachableLabels, referenceGraph);
			}

			if (stackMap != null) {
				stackMap.removeEntriesForLabels(unreachableLabels, referenceGraph);
			}

			if (stackMapTable != null) {
				stackMapTable.removeEntriesForLabels(unreachableLabels, referenceGraph);
			}

			ArrayList<ExceptionEntryNode> keptEntries = new ArrayList<>(this.exceptionEntries.length);

			for (int entryIndex = 0; entryIndex < this.exceptionTableLength; entryIndex++) {
				if (!this.exceptionEntries[entryIndex].removeInGraph(unreachableLabels, unreachableIndexes, referenceGraph, methodAnalyzer)) {
					keptEntries.add(this.exceptionEntries[entryIndex]);
				}
			}

			if (this.exceptionTableLength > keptEntries.size()) {
				ExceptionEntryNode[] newExceptionEntries = new ExceptionEntryNode[keptEntries.size()];
				this.exceptionEntries = keptEntries.toArray(newExceptionEntries);
				this.exceptionTableLength = this.exceptionEntries.length;
			}

			if (typeAnnotationRefsFound) {
				((MethodNode)this.getParent()).pruneTypeAnnotations(unreachableLabels, referenceGraph);
			}

			this.instructionList.removeDeadInstructions(unreachableIndexes, verbose, scriptContext);
			methodAnalyzer.release();
			return true;
		}
	}

	public void removeAttributesOfType(Class<?> attributeType) {
		int numAttributes = this.attributes.length;
		ArrayList<AttributesNode> keptAttributes = new ArrayList<>();

		for (int attributeIndex = 0; attributeIndex < numAttributes; attributeIndex++) {
			if (!attributeType.isInstance(this.attributes[attributeIndex])) {
				keptAttributes.add(this.attributes[attributeIndex]);
			}
		}

		int keptCount = keptAttributes.size();
		if (keptCount < numAttributes) {
			this.attributes = new AttributesNode[keptCount];
			this.attributes = keptAttributes.toArray(this.attributes);
		}

		this.attributeCount = this.attributes.length;
		this.length = this.getAttributeLength();
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
		List<StructureNode> resolvedNodes,
		List<MethodInvocation> methodInvocations,
		boolean verbose
	) {
		//      if (!test.isEmpty()) {
		//         for (Map.Entry<MethodCallWriter, List<String>> methodCallWriterListEntry : test.entrySet()) {
		//         }
		//         System.out.println(.value.get(0));
		//      }
		return this.instructionList
			.analyzeReflectionCalls(
				reflectionPatterns, classRepository, fieldReadAccesses, fieldWriteAccesses, methodAccesses, stringInternalNameReferences, typeResolver, methodAnalysisResults, fieldReferences, stackReferences, localVariableReferences, workspace, methodAnalyzerCache, stringClassReferences, stringResourcePathReferences, stringFieldNameReferences, stringMethodNameReferences, processedStringNodes, resolvedNodes, methodInvocations, verbose
			);
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
		this.instructionList.insertDummyCondition(condition, memberObfuscationRefs, typeWriters, typeResolver, workspace, staticBlockAnalyzer, random);
	}

	public List<String> getParameterTypeDescriptors() {
		return ((AbstractMethodNode)this.getParent()).getNormalizedParameterDescriptors();
	}

	public boolean hasConditionalJump() {
		return this.instructionList.hasConditionalJump();
	}

	public void collectExcludedStringNodes(Set<StringNode> excludedStrings, StringEncryptionExcludes encryptionTargetManager) {
		this.instructionList.collectExcludedStringConstants(excludedStrings, encryptionTargetManager);
	}

	public String getMethodDisplayName() {
		return ((AbstractMethodNode)this.getParent()).getNameDisplay();
	}

	public Map<Integer, String> getLocalVariableMap() {
		for (int i = 0; i < this.attributes.length; i++) {
			if (this.attributes[i] instanceof LocalVariableTableAttribute) {
				return ((LocalVariableTableAttribute)this.attributes[i]).getIndexToNameMap();
			}
		}

		return null;
	}

	public void collectBootstrapMethods(Set<BootstrapMethodEntry> bootstrapMethods) {
		this.instructionList.collectBootstrapMethods(bootstrapMethods);
	}

	public String getAsString() {
		return this.getSourcePath() + " " + ((AbstractMethodNode)this.getParent()).getOriginalNameWithParams();
	}

	public void fixLabelOffsets(MultiMap<Integer, IGraphElement> elementsByOffset, PrintWriter logWriter) {
		this.instructionList.fixLabelOffsets(elementsByOffset, logWriter);
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
		this.instructionList.obfuscateStringConcat(stringConcatAnalysis, constantPoolManager, dynCallNames, instructionBlocks, typeWriters, typeResolver, classHierarchy);
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
		this.instructionList
			.applyReflectionObfuscation(reflectedMethods, writersByName, constantPoolManager, typeWriters, decryptionLocalVarOperand, workspace, classProvider, reason, insertThisVar);
	}

	public boolean isPrivate() {
		return ((AbstractMethodNode)this.getParent()).isPrivate();
	}

	public void removeStackMapAttributes() {
		this.removeAttributesOfType(AbstractStackMapAttribute.class);
	}

	public void buildReferenceGraph(
		ReferenceGraphMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> referenceGraphMap, ObfuscateReferencesBuilder referencesBuilder, TypeResolver typeResolver, ClassHierarchy classHierarchy
	) {
		this.instructionList.buildReferenceGraph(referenceGraphMap, referencesBuilder, typeResolver, classHierarchy);
	}

	public boolean optimizeLdcAndStackMaps(
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy,
		ConstantPoolManager constantPoolManager,
		List<TypeWriter> typeWriters,
		Map<TypeWriter, TypeWriter> typeWriterMapping,
		boolean rebuildStackMaps,
		boolean useLegacyStackMap
	) {
		if (this.instructionList == null) {
			return false;
		} else if (!this.instructionList.optimizeLdcInstructions(typeWriterMapping) && !this.instructionList.isModified()) {
			return false;
		} else {
			this.instructionList.recalculateOffsets();
			if (rebuildStackMaps) {
				if (useLegacyStackMap) {
					this.rebuildStackMap(typeResolver, classHierarchy, constantPoolManager, typeWriters);
				} else {
					this.rebuildStackMapTable(typeResolver, classHierarchy, constantPoolManager, typeWriters);
				}

				return true;
			} else {
				return false;
			}
		}
	}

	public final void syncAnnotationNames() {
		for (int i = 0; i < this.attributes.length; i++) {
			if (this.attributes[i] instanceof AnnotationContainer) {
				((AnnotationContainer)this.attributes[i]).syncNames();
			}
		}
	}

	@Override
	public void writeAttributeBodyRemapped(DataOutputStream dataOutputStream, Map<TypeWriter, TypeWriter> writerMap, ScriptContext scriptContext) throws IOException {
		super.writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		dataOutputStream.writeShort(this.maxStack);
		dataOutputStream.writeShort(this.maxLocals);
		this.instructionList.writeInstructionsRemapped(dataOutputStream, writerMap);
		dataOutputStream.writeShort(this.exceptionTableLength);

		for (int exceptionIndex = 0; exceptionIndex < this.exceptionTableLength; exceptionIndex++) {
			this.exceptionEntries[exceptionIndex].writeRemapped(dataOutputStream, writerMap);
		}

		dataOutputStream.writeShort(this.attributeCount);

		for (int attributeIndex = 0; attributeIndex < this.attributeCount; attributeIndex++) {
			if (this.attributes[attributeIndex] instanceof UnknownAttribute && this.attributes[attributeIndex].getAttributeLength() > 0) {
				UnknownAttribute unknownAttribute = (UnknownAttribute)this.attributes[attributeIndex];
				if (unknownAttribute.isSharedByManyClasses()) {
					scriptContext.logWarning("Method contain unknown attribute '" + unknownAttribute.getAttributeName() + "'. The integrity of this attribute may be affected by obfuscation.");
				} else {
					scriptContext.logWarning(
						"Method '"
							+ this.getMethodDisplayName()
							+ "' in class '"
							+ this.getBinaryName()
							+ "' contains unknown attribute '"
							+ unknownAttribute.getAttributeName()
							+ "'. The integrity of this attribute may be affected by obfuscation. Consider using the Trim function to delete it."
					);
				}
			}

			this.attributes[attributeIndex].writeAttributeBodyRemapped(dataOutputStream, writerMap, scriptContext);
		}
	}

	public void encryptLongConstants(
		List<KeyValuePair<PriorityEntry<WideConstantPoolInsnNode>, LongEntry>> longConstantEntries,
		boolean isClinit,
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
		if (this.instructionList != null) {
			int matchingEntryCount = 0;
			if (this.exceptionTableLength > 0) {
				ExceptionEntryNode[] exceptionEntryArray = this.exceptionEntries;
				int entryCount = exceptionEntryArray.length;

				for (int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
					if (exceptionEntryArray[entryIndex].isLabelBased()) {
						matchingEntryCount++;
					}
				}
			}

			int[][] exceptionRanges;
			if (matchingEntryCount > 0) {
				this.instructionList.recalculateOffsets();
				int rangeIndex = 0;
				exceptionRanges = new int[matchingEntryCount][2];

				for (int exceptionIndex = 0; exceptionIndex < this.exceptionTableLength; exceptionIndex++) {
					ExceptionEntryNode exceptionEntry = this.exceptionEntries[exceptionIndex];
					if (exceptionEntry.isLabelBased()) {
						exceptionRanges[rangeIndex][0] = exceptionEntry.getStartLabelId();
						exceptionRanges[rangeIndex][1] = exceptionEntry.getEndLabelId();
						rangeIndex++;
					}
				}
			} else {
				exceptionRanges = new int[0][0];
			}

			this.instructionList.encryptLongConstants(longConstantEntries, labelCounters, isClinit, exceptionRanges, labelReplacements, xorKey, xorKeyOperand, localSlotIndex, constantPoolManager, typeWriters, typeResolver, classHierarchy);
			if (!isClinit && this.maxLocals < localSlotIndex.get()) {
				this.setMaxLocals(localSlotIndex.get());
			}
		}
	}

	public String getMethodSignature() {
		return ((AbstractMethodNode)this.getParent()).getSignature();
	}

	public boolean hasLongConstants() {
		return this.instructionList.hasLongConstants();
	}

	public void encryptStringConstantsInPlace(
		NestedMultiMap<ClassNode, MethodNode, StringNode, PriorityEntry<WritableType>> stringConstantEntries, MethodNode methodNode, Set<StringNode> excludedStrings
	) {
		this.instructionList.encryptStringConstantsInPlace(stringConstantEntries, methodNode, excludedStrings);
	}

	public void printInstructions(PrintWriter writer, int indent) {
		if (this.instructionList != null) {
			this.instructionList.printInstructions(writer, indent);
		}
	}

	public void printExceptionEntries(PrintWriter writer, int indent) {
		writer.println("");
		StringBuffer indentBuffer = new StringBuffer(indent);

		for (int i = 0; i < indent; i++) {
			indentBuffer.append("   ");
		}

		for (int exceptionIndex = 0; exceptionIndex < this.exceptionEntries.length; exceptionIndex++) {
			this.exceptionEntries[exceptionIndex].printDetails(writer, indentBuffer);
		}
	}

	public void encryptIntegerConstants(
		NestedMultiMap<ClassNode, MethodNode, IntegerNode, PriorityEntry<WritableType>> integerConstantEntries,
		MethodNode methodNode,
		Set<IntegerNode> excludedIntegers,
		IntegerEncryptionExcludes encryptionExcludes,
		boolean encryptAllIntegers,
		List<TypeWriter> typeWriters,
		ConstantPoolManager constantPoolManager
	) {
		this.instructionList.encryptIntegerConstants(integerConstantEntries, methodNode, excludedIntegers, encryptionExcludes, encryptAllIntegers, typeWriters, constantPoolManager);
	}

	public void collectLineNumberTables(ArrayList<LineNumberEntry> lineNumberTables) {
		LineNumberTableAttribute lineNumberTable = this.getLineNumberTableAttribute();
		if (lineNumberTable != null) {
			lineNumberTable.addEntriesTo(lineNumberTables);
		}
	}

	@Override
	public int getAttributeLength() {
		int totalLength = 4 + this.instructionList.getCodeLengthWithSizeField() + 2 + this.exceptionTableLength * 8 + 2;

		for (int i = 0; i < this.attributeCount; i++) {
			totalLength += this.attributes[i].getSerializedSize();
		}

		this.length = totalLength;
		return totalLength;
	}

	public void collectSerialPersistentFields(List<FieldNode> serialFields, AbstractWorkspace workspace) {
		if (this.instructionList != null) {
			this.instructionList.collectSerialPersistentFields(serialFields, workspace);
		}
	}

	public final void remapAttributes(int keepInnerClassInfo, int keepGenericsInformation, HashMap<String, String> packageRenameMap, HashMap<String, String> classNameMap) {
		for (int i = 0; i < this.attributeCount; i++) {
			this.attributes[i].remapDescriptors(keepInnerClassInfo, keepGenericsInformation, packageRenameMap, classNameMap);
		}
	}

	public boolean hasLineNumber(int lineNumber) {
		for (int i = 0; i < this.attributes.length; i++) {
			if (this.attributes[i] instanceof LineNumberTableAttribute) {
				return ((LineNumberTableAttribute)this.attributes[i]).hasLineNumber(lineNumber);
			}
		}

		return false;
	}

	public void setModified(boolean modified) {
		if (this.instructionList != null) {
			this.instructionList.setModified(modified);
		}
	}

	public void updateClassMetrics(ClassMetrics classMetrics) {
		this.instructionList.updateClassMetrics(classMetrics);
	}

	public boolean hasStringConstants() {
		return this.instructionList.hasStringConstants();
	}

	public LineNumberTableAttribute getLineNumberTableAttribute() {
		for (int i = 0; i < this.attributes.length; i++) {
			if (this.attributes[i] instanceof LineNumberTableAttribute) {
				return (LineNumberTableAttribute)this.attributes[i];
			}
		}

		return null;
	}

	public boolean isFieldWritten(FieldNode field) {
		return this.instructionList != null && this.instructionList.isFieldWritten(field);
	}

	public void encryptIntegerConstantsWithLong(
		List<KeyValuePair<PriorityEntry<WritableType>, IntegerEntry>> integerConstantEntries,
		boolean isClinit,
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
		if (this.instructionList != null) {
			int matchingEntryCount = 0;
			if (this.exceptionTableLength > 0) {
				ExceptionEntryNode[] exceptionEntryArray = this.exceptionEntries;
				int entryCount = exceptionEntryArray.length;

				for (int entryIndex = 0; entryIndex < entryCount; entryIndex++) {
					if (exceptionEntryArray[entryIndex].isLabelBased()) {
						matchingEntryCount++;
					}
				}
			}

			int[][] exceptionRanges;
			if (matchingEntryCount > 0) {
				this.instructionList.recalculateOffsets();
				int rangeIndex = 0;
				exceptionRanges = new int[matchingEntryCount][2];

				for (int exceptionIndex = 0; exceptionIndex < this.exceptionTableLength; exceptionIndex++) {
					ExceptionEntryNode exceptionEntry = this.exceptionEntries[exceptionIndex];
					if (exceptionEntry.isLabelBased()) {
						exceptionRanges[rangeIndex][0] = exceptionEntry.getStartLabelId();
						exceptionRanges[rangeIndex][1] = exceptionEntry.getEndLabelId();
						rangeIndex++;
					}
				}
			} else {
				exceptionRanges = new int[0][0];
			}

			this.instructionList.encryptIntegerConstantsWithLong(integerConstantEntries, isClinit, labelCounters, exceptionRanges, labelReplacements, xorKey, xorKeyOperand, localSlotIndex, constantPoolManager, typeWriters, typeResolver, classHierarchy);
			if (!isClinit && this.maxLocals < localSlotIndex.get()) {
				this.setMaxLocals(localSlotIndex.get());
			}
		}
	}

	public void setMaxStack(int maxStack) {
		if (maxStack <= 65535) {
			this.maxStack = maxStack;
		} else {
			throw new ProcessingException("Stack size greater than 65535 in " + this.getAsString() + ". (" + maxStack + ")");
		}
	}

	public void setMaxLocals(int maxLocals) {
		if (maxLocals <= 65535) {
			this.maxLocals = maxLocals;
		} else {
			throw new ProcessingException("Local variable count greater than 65535 in " + this.getAsString() + ". (" + maxLocals + ")");
		}
	}

	public void collectExcludedIntegerNodes(Set<IntegerNode> excludedIntegers, IntegerEncryptionExcludes encryptionExcludes) {
		this.instructionList.collectExcludedIntegerConstants(excludedIntegers, encryptionExcludes);
	}

	public void setExceptionEntries(TryCatchBlockInfo[] tryCatchBlocks) {
		this.exceptionTableLength = tryCatchBlocks.length;
		this.exceptionEntries = new ExceptionEntryNode[this.exceptionTableLength];

		for (int exceptionIndex = 0; exceptionIndex < this.exceptionTableLength; exceptionIndex++) {
			TryCatchBlockInfo tryCatchBlock = tryCatchBlocks[exceptionIndex];
			this.exceptionEntries[exceptionIndex] = new ExceptionEntryNode(this, tryCatchBlock.getExceptionType(), tryCatchBlock.getStartLabel(), tryCatchBlock.getEndLabel(), tryCatchBlock.getHandlerLabel());
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
		Random random
	) {
		return this.instructionList != null ? this.instructionList.analyzeControlFlow(instructionBlocks, typeResolver, scriptContext, classHierarchy, analysisMode, flowObfuscationConfigId, licenseVerified, keepBalancedLocks, methodAnalyzerCache, random) : 0;
	}
}
