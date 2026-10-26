package com.zelix.framework.analyzer;

import com.zelix.exceptions.*;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.analyzer.dataset.ExceptionAnalyzingInfo;
import com.zelix.framework.analyzer.dataset.FlagData;
import com.zelix.framework.analyzer.dataset.MethodAnalysisResult;
import com.zelix.framework.analyzer.reference.*;
import com.zelix.framework.analyzer.tracker.ArrayDimensionTracker;
import com.zelix.framework.analyzer.tracker.PropertyFileNameTracker;
import com.zelix.framework.analyzer.tracker.ReflectionMethodCallTracker;
import com.zelix.framework.analyzer.tracker.VariableTracker;
import com.zelix.framework.attributes.code.CodeAttribute;
import com.zelix.framework.attributes.code.ExceptionEntryNode;
import com.zelix.framework.attributes.code.StackMapAttribute;
import com.zelix.framework.attributes.code.TryCatchBlockPCType;
import com.zelix.framework.attributes.stackmap.StackMapEntry;
import com.zelix.framework.attributes.stackmap.VerificationTypeInfo;
import com.zelix.framework.code.InsnList;
import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.instructions.*;
import com.zelix.framework.reference.IControlFlowElement;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.signature.FieldID;
import com.zelix.framework.signature.MethodID;
import com.zelix.framework.variable.LocalVarOperationType;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.FieldTypeWriter;
import com.zelix.framework.writer.MethodCallWriter;
import com.zelix.framework.writer.MethodDescriptorWriter;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.framework.writer.reflect.ReflectionMethodPattern;
import com.zelix.framework.writer.reflect.ReflectionParameter;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.tools.excludes.ExcludeElement;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.IntegerCache;
import com.zelix.utils.collections.*;
import com.zelix.utils.collections.readOnly.ReadOnlyList;
import com.zelix.utils.dataset.AtomicBool;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.references.AtomicReference;
import com.zelix.utils.treeNode.TreeNavigator;
import com.zelix.workspace.AbstractWorkspace;

import java.util.*;
import java.util.Map.Entry;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class MethodAnalyzer {
	public static final IntegerCache INTEGER_CACHE = IntegerCache.getInstance();
	private List<AnalyzingNode> catchEntryNodes = new ArrayList<>();
	private List<AnalyzingNode> nodes = new ArrayList<>();
	private MultiMap<AnalyzingNode, ExceptionAnalyzingInfo> exceptionInfoByCatchNode = null;
	private DualKeyMap<AnalyzingNode, AnalyzingNode, AnalyzingNode> reachableNodesByCatchNode = null;
	private boolean released = false;
	private InstructionListNode instructionListNode;
	private String methodAnalyzing;
	private TypeResolver typeResolver;
	private ClassHierarchy classHierarchy;
	private boolean isStatic;
	private boolean isConstructor;
	private InsnList<AbstractInsnNode> insnList;
	private ExceptionEntryNode[] exceptions;
	private int maxLocals;
	private boolean forStackMapRebuild;
	private StackFrameType[] initialLocalTypes;
	private int localSlotCount;
	private boolean notPreverified;
	private AnalyzingNode headerNode;
	private List<ControlFlowNode> controlFlowNodes;
	private int minReturnStackDepth;
	private boolean uninitializedUsageConsistent;
	private DualKeyMap<VarInsnNode, Integer, Integer> jsrContinuationsByRetNode;
	private AnalyzingNode[] nodeByInstructionIndexCache;
	private FrameNode[] frames;

	public MethodAnalyzer(
		InsnList<AbstractInsnNode> insnList,
		ExceptionEntryNode[] exceptionEntries,
		int maxLocals,
		int maxStack,
		List<String> parameterTypeNames,
		boolean isStatic,
		boolean isConstructor,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy,
		InstructionListNode instructionListNode,
		boolean forStackMapRebuild
	) {
		this.instructionListNode = instructionListNode;
		this.methodAnalyzing = instructionListNode.getParentString();
		this.typeResolver = typeResolver;
		this.classHierarchy = classHierarchy;
		this.isStatic = isStatic;
		this.isConstructor = isConstructor;
		this.insnList = insnList;
		this.exceptions = exceptionEntries;
		this.maxLocals = maxLocals;
		this.forStackMapRebuild = forStackMapRebuild;
		AtomicInt slotCounter = new AtomicInt(0);

		try {
			this.initialLocalTypes = buildInitialLocalTypes(maxLocals, this.isStatic, parameterTypeNames, this.isConstructor, instructionListNode.getInternalName(), slotCounter);
		} catch (ArrayIndexOutOfBoundsException indexOutOfBoundsException) {
			throw new InvalidBytecodeException(
				"Method '"
					+ this.methodAnalyzing
					+ "' appears to be corrupt. : Invalid max_locals="
					+ maxLocals
					+ " : "
					+ parameterTypeNames
					+ " : "
					+ (this.isStatic ? "static " : "instance ")
					+ indexOutOfBoundsException.getMessage()
			);
		}

		this.localSlotCount = slotCounter.get();
		int nodeNameCounter = 0;
		int instructionCount = insnList.size();
		HashMap<LabelNode, AnalyzingNode> nodeByLabel = GlobalUtils.newHashMap();
		MultiMap<IControlFlowElement, AnalyzingNode> successorsByInsn = new MultiMap<>();

		for (int i = 0; i < instructionCount; i++) {
			AbstractInsnNode insn = insnList.get(i);
			if (insn instanceof IControlFlowElement) {
				((IControlFlowElement)insn).registerTargetNodes(nodeByLabel, successorsByInsn, this.nodes);
				if (insn.isJSR()) {
					this.notPreverified = true;
				}
			} else if (insn instanceof LabelNode) {
				((LabelNode)insn).setLabelID(i);
			}
		}

		ExceptionAnalyzingInfo[] exceptionInfos;
		if (exceptionEntries != null) {
			exceptionInfos = new ExceptionAnalyzingInfo[exceptionEntries.length];
			this.exceptionInfoByCatchNode = new MultiMap<>(exceptionEntries.length);
			int catchNodeNameCounter = 0;

			for (int exceptionEntryIndex = 0; exceptionEntryIndex < exceptionEntries.length; exceptionEntryIndex++) {
				LabelNode startPC = exceptionEntries[exceptionEntryIndex].getPCByType(TryCatchBlockPCType.START_PC);
				LabelNode endPC = exceptionEntries[exceptionEntryIndex].getPCByType(TryCatchBlockPCType.END_PC);
				LabelNode handlerPC = exceptionEntries[exceptionEntryIndex].getPCByType(TryCatchBlockPCType.HANDLER_PC);
				AnalyzingNode catchNode;
				if ((catchNode = nodeByLabel.get(handlerPC)) == null) {
					catchNode = new AnalyzingNode();
					this.nodes.add(catchNode);
					nodeByLabel.put(handlerPC, catchNode);
					catchNode.setName("catchNode" + catchNodeNameCounter++);
					this.catchEntryNodes.add(catchNode);
				} else if (!this.exceptionInfoByCatchNode.containsKey(catchNode) && !this.catchEntryNodes.contains(catchNode)) {
					this.catchEntryNodes.add(catchNode);
					catchNode.setName("catchNode" + catchNodeNameCounter++);
				}

				exceptionInfos[exceptionEntryIndex] = new ExceptionAnalyzingInfo(
					this,
					exceptionEntryIndex,
					startPC.getLabelID(),
					endPC.getLabelID(),
					handlerPC.getLabelID(),
					exceptionEntries[exceptionEntryIndex].getExceptionName(),
					exceptionEntries[exceptionEntryIndex].hasNoTargetException(),
					catchNode,
					exceptionEntries[exceptionEntryIndex]
				);
				ExceptionAnalyzingInfo exceptionInfo = exceptionInfos[exceptionEntryIndex];
				this.exceptionInfoByCatchNode.put(catchNode, exceptionInfo);
			}
		} else {
			exceptionInfos = new ExceptionAnalyzingInfo[0];
		}

		int headerInstructionIndex = 0;

		for (AbstractInsnNode leadingLabel = insnList.get(0); leadingLabel.isLabel(); leadingLabel = insnList.get(++headerInstructionIndex)) {
			AnalyzingNode headerCandidate = nodeByLabel.get(leadingLabel);
			if (headerCandidate != null) {
				this.headerNode = headerCandidate;
				break;
			}
		}

		if (this.headerNode == null) {
			this.headerNode = new AnalyzingNode();
			this.nodes.add(this.headerNode);
		}

		this.headerNode.setName("header");
		AnalyzingNode currentNode = this.headerNode;
		currentNode.setStartIndex(0);
		LinkedHashSet<AnalyzingNode> jsrEntryNodes = new LinkedHashSet<>();

		for (int insnIndex = headerInstructionIndex; insnIndex < instructionCount; insnIndex++) {
			AbstractInsnNode currentInsn = insnList.get(insnIndex);
			if (currentInsn instanceof IControlFlowElement) {
				currentNode.setEndIndex(insnIndex);
				List<AnalyzingNode> successors = successorsByInsn.get((IControlFlowElement)currentInsn);

				for (int successorIndex = 0; successorIndex < successors.size(); successorIndex++) {
					currentNode.addSuccessor(successors.get(successorIndex));
				}

				if (insnIndex + 1 < instructionCount) {
					AbstractInsnNode nextInsn = insnList.get(insnIndex + 1);
					AnalyzingNode continuationNode;
					if (!(nextInsn instanceof LabelNode) || (continuationNode = nodeByLabel.get(nextInsn)) == null) {
						continuationNode = new AnalyzingNode();
						this.nodes.add(continuationNode);
					}

					if (continuationNode.getName() == null) {
						continuationNode.setName("node" + nodeNameCounter++);
					}

					continuationNode.setStartIndex(insnIndex + 1);
					if (currentInsn.canFallThrough()) {
						currentNode.addSuccessor(continuationNode);
					} else if (currentInsn.isJSR()) {
						currentNode.setJsrContinuationNode(continuationNode);
						AnalyzingNode jsrTargetNode = successors.get(0);
						jsrTargetNode.addJsrCallSiteNode(currentNode);
						jsrEntryNodes.add(jsrTargetNode);
					}

					currentNode = continuationNode;
				}
			} else if (currentInsn instanceof LabelNode) {
				AnalyzingNode existingNode = nodeByLabel.get(currentInsn);
				if (existingNode != null && currentNode != existingNode) {
					AbstractInsnNode previousInsn = insnList.get(insnIndex - 1);
					currentNode.setEndIndex(insnIndex - 1);
					existingNode.setStartIndex(insnIndex);
					if (existingNode.getName() == null) {
						existingNode.setName("node" + nodeNameCounter++);
					}

					if (previousInsn.canFallThrough()) {
						currentNode.addSuccessor(existingNode);
					}

					currentNode = existingNode;
				}
			} else if (currentInsn.isMethodExit()) {
				currentNode.setEndIndex(insnIndex);
				if (insnIndex + 1 < instructionCount) {
					AbstractInsnNode insnAfterExit = insnList.get(insnIndex + 1);
					AnalyzingNode nodeAfterExit;
					if (!(insnAfterExit instanceof LabelNode) || (nodeAfterExit = nodeByLabel.get(insnAfterExit)) == null) {
						nodeAfterExit = new AnalyzingNode();
						this.nodes.add(nodeAfterExit);
					}

					if (nodeAfterExit.getName() == null) {
						nodeAfterExit.setName("node" + nodeNameCounter++);
					}

					nodeAfterExit.setStartIndex(insnIndex + 1);
					currentNode = nodeAfterExit;
				}
			}
		}

		Collections.sort(this.catchEntryNodes);
		Collections.sort(this.nodes);

		for (int nodeIndex = 0; nodeIndex < this.nodes.size(); nodeIndex++) {
			AnalyzingNode node = this.nodes.get(nodeIndex);
			if (node.getStartIndex() > node.getEndIndex()) {
				if (nodeIndex < this.nodes.size() - 1) {
					AnalyzingNode nextNode = this.nodes.get(nodeIndex + 1);
					node.setEndIndex(nextNode.getStartIndex() - 1);
				} else {
					node.setEndIndex(instructionCount - 1);
				}
			}
		}

		ExceptionAnalyzingInfo[][] exceptionHandlersByInsn = new ExceptionAnalyzingInfo[instructionCount][];
		if (this.catchEntryNodes.size() > 0) {
			this.reachableNodesByCatchNode = new DualKeyMap<>(this.catchEntryNodes.size());
			this.buildExceptionHandlerIndex(exceptionHandlersByInsn, exceptionInfos);
		}

		for (AnalyzingNode jsrEntryNode : this.resolveJsrTraversalOrder(jsrEntryNodes)) {
			this.linkJsrRetNodes(jsrEntryNode, jsrEntryNode, GlobalUtils.newHashSet(13));
			if (!jsrEntryNode.hasJsrRetNodes()) {
				Iterator<AnalyzingNode> jsrCallSiteIterator = jsrEntryNode.getJsrCallSiteNodes().iterator();

				while (jsrCallSiteIterator.hasNext()) {
					jsrCallSiteIterator.next().setJsrContinuationNode(null);
				}
			}
		}

		this.pruneDisconnectedGraph(this.reachableNodesByCatchNode);

		try {
			this.controlFlowNodes = this.buildControlFlowNodes();
		} catch (StackOverflowError ignoredControlFlowOverflow) {
			throw new ProcessingException("StackOverflowError : Try increasing the thread stack size (-Xss) (A)");
		}

		try {
			this.minReturnStackDepth = this.computeMinReturnStackDepth();
		} catch (StackOverflowError ignoredMinReturnDepthOverflow) {
			throw new ProcessingException(
				"StackOverflowError : Try increasing the thread stack size (-Xss) (B) : '" + instructionListNode.getClassNameBinary() + "' '" + instructionListNode.getMethodSignature() + "'"
			);
		}

		if (instructionListNode.getClassNode().hasInnerClassOrdinal()) {
			typeResolver.setInnerClassOrdinal(instructionListNode.getClassNode().getInnerClassOrdinal());
		} else {
			typeResolver.removeInnerClassOrdinal();
		}

		String analysisDescription = "analyzing the control flow of method '"
			+ instructionListNode.getMethodNode().getOriginalNameWithParams()
			+ "' in class '"
			+ instructionListNode.getRawSourcePath()
			+ "' : '"
			+ instructionListNode.getInternalName()
			+ "' (A)";

		try {
			boolean verifyStackFrameTypes = Options.verifyStackFrameTypes && !Options.dontVerifyStackFrameTypes;
			this.analyzeStackFrames(typeResolver, exceptionHandlersByInsn, verifyStackFrameTypes, false, analysisDescription);
		} catch (DebugAssertFailedException stackFramesAssertFailure) {
			throw stackFramesAssertFailure;
		}

		if (forStackMapRebuild && !Options.ZKM_STACK_MAP_ALGORITHM.equals("1")) {
			try {
				this.uninitializedUsageConsistent = this.analyzeUninitializedUsage();
			} catch (DebugAssertFailedException uninitializedUsageAssertFailure) {
				throw uninitializedUsageAssertFailure;
			} catch (Exception ignored) {
			} catch (StackOverflowError uninitializedUsageStackOverflow) {
				throw uninitializedUsageStackOverflow;
			}
		}

		int computedMaxStack = this.computeMaxStackDepth();
		int declaredMaxStack = ((CodeAttribute)instructionListNode.getParent()).getMaxStack();
		if (computedMaxStack != declaredMaxStack) {
			((CodeAttribute)instructionListNode.getParent()).setMaxStack(computedMaxStack);
		}

		typeResolver.removeInnerClassOrdinal();
	}

	public static StackFrameType[] buildInitialLocalTypes(int maxLocals, boolean isStatic, List<String> parameterTypeNames, boolean isConstructor, String internalName, AtomicInt slotCounter) {
		StackFrameType[] localTypes = StackFrameType.createEmptyFrame(maxLocals);
		int localSlot = 0;
		if (!isStatic) {
			localSlot++;
			localTypes[0] = StackFrameType.getInstance("L" + internalName + ";", !isConstructor);
		}

		if (parameterTypeNames != null) {
			for (int i = 0; i < parameterTypeNames.size(); i++) {
				String parameterTypeName = parameterTypeNames.get(i);
				localTypes[localSlot++] = StackFrameType.getResolved(parameterTypeName);
				slotCounter.increaseAndGet();
				if (parameterTypeName.equals("J") || parameterTypeName.equals("D")) {
					localTypes[localSlot++] = StackFrameType.UNINITIALIZED;
					slotCounter.increaseAndGet();
				}
			}
		}

		return localTypes;
	}

	public boolean analyzeUninitializedUsage() {
		for (AnalyzingNode startNode : this.getUninitializedStartNodes()) {
			BitSet startNodeUsageMask = new BitSet(this.maxLocals);
			if (this.instructionListNode.isConstructor()) {
				startNodeUsageMask.set(0);
			}

			this.markUninitializedLocalUsage(startNode, startNode.getEndIndex(), startNodeUsageMask);
		}

		this.propagateUninitializedUsage(this.catchEntryNodes);
		Iterator<TreeNavigator<ControlFlowNode>> navigatorIterator = this.buildControlFlowNavigators().iterator();

		while (navigatorIterator.hasNext()) {
			Iterator<TreeNavigator.TreeNode<ControlFlowNode>> treeNodeIterator = navigatorIterator.next().iterator();

			while (treeNodeIterator.hasNext()) {
				ControlFlowNode treeFlowNode = treeNodeIterator.next().getData();
				AnalyzingNode treeAnchorNode = treeFlowNode.getAnchorNode();
				BitSet treeAnchorUsageMask = this.frames[treeAnchorNode.getStartIndex()].getUninitializedUsageMask();

				for (AnalyzingNode treeSourceNode : treeFlowNode.getSourceNodes()) {
					this.markUninitializedLocalUsage(treeSourceNode, treeSourceNode.getEndIndex(), (BitSet)treeAnchorUsageMask.clone());
				}
			}
		}

		int propagationRound = -1;

		boolean propagationChanged;
		do {
			propagationChanged = this.propagateUninitializedUsage(this.catchEntryNodes);
		} while (propagationChanged && ++propagationRound < 10);

		boolean consistentUsage = !propagationChanged;
		if (consistentUsage) {
			label144:
			for (ControlFlowNode flowNode : this.controlFlowNodes) {
				AnalyzingNode anchorNode = flowNode.getAnchorNode();
				BitSet anchorUsageMask = this.frames[anchorNode.getStartIndex()].getUninitializedUsageMask();

				for (AnalyzingNode sourceNode : flowNode.getSourceNodes()) {
					BitSet sourceUsageMask = this.frames[sourceNode.getEndIndex()].getUninitializedUsageMask();

					for (int localSlotIndex = 0; localSlotIndex < sourceUsageMask.size(); localSlotIndex++) {
						if (anchorUsageMask.get(localSlotIndex) && !sourceUsageMask.get(localSlotIndex)) {
							consistentUsage = false;
							break label144;
						}
					}
				}
			}
		}

		if (consistentUsage) {
			label124:
			for (AnalyzingNode catchEntryNode : this.catchEntryNodes) {
				BitSet catchEntryUsageMask = this.frames[catchEntryNode.getStartIndex()].getUninitializedUsageMask();

				for (ExceptionAnalyzingInfo exceptionInfo : this.exceptionInfoByCatchNode.get(catchEntryNode)) {
					for (int handlerPc = exceptionInfo.start_pc; handlerPc < exceptionInfo.end_pc; handlerPc++) {
						if (this.frames[handlerPc] != null) {
							BitSet handlerUsageMask = this.frames[handlerPc].getUninitializedUsageMask();

							for (int handlerSlotIndex = 0; handlerSlotIndex < catchEntryUsageMask.size(); handlerSlotIndex++) {
								if (catchEntryUsageMask.get(handlerSlotIndex) && !handlerUsageMask.get(handlerSlotIndex)) {
									consistentUsage = false;
									break label124;
								}
							}
						}
					}
				}
			}
		}

		if (consistentUsage && !this.isStatic && !this.isConstructor) {
			boolean thisRefInAllFrames = true;

			for (int frameIndex = 0; frameIndex < this.frames.length; frameIndex++) {
				FrameNode frame = this.frames[frameIndex];
				if (frame != null && !frame.getLocalVariables()[0].isObjectReference()) {
					thisRefInAllFrames = false;
					break;
				}
			}

			if (thisRefInAllFrames) {
				BitSet thisUsageMask = new BitSet(this.maxLocals);
				thisUsageMask.set(0);

				for (FrameNode frameNode : this.frames) {
					if (frameNode != null) {
						frameNode.markUninitializedUsage(thisUsageMask);
					}
				}
			}
		}

		if (!consistentUsage) {
		}

		if (!consistentUsage) {
		}

		return consistentUsage;
	}

	private void tracePropertiesFileName(AnalysisContext analysisContext, AnalyzingNode analyzingNode, int instructionIndex) {
		WideConstantPoolInsnNode newInsn = (WideConstantPoolInsnNode)this.insnList.get(instructionIndex);
		String newTypeName = ((ClassInfoWriter)newInsn.getTypeWriter()).getInternalName();
		if (newTypeName.equals("java/util/Properties")) {
			FrameNode frame = this.frames[instructionIndex];
			PropertyFileNameTracker propertyFileNameTracker = this.createArrayElementTracker();
			this.traceVariableTrackerSources(analysisContext, propertyFileNameTracker, analyzingNode, instructionIndex + 1, frame.getOperandStack().length - 1);
			HashSet<Reference> references = propertyFileNameTracker.getReferences();
			if (references.size() == 0) {
				references.add(new ErrorReference(newInsn, "Couldn't determine property file name"));
			}

			analysisContext.currentReferences.addAll(references);
		} else {
			analysisContext.currentReferences.add(new ErrorReference(newInsn, "NEW instruction not handled with : " + newTypeName));
		}
	}

	private void mergeFrameAtInsn(int previousIndex, int instructionIndex, TreeSet<Integer> changedIndexes, boolean checkTypes, boolean unused, String methodSignature) {
		AbstractInsnNode insn = this.insnList.get(instructionIndex);

		FrameNode analyzedFrame;
		try {
			analyzedFrame = insn.analyzeStack(this.frames[previousIndex], checkTypes, unused, this.typeResolver, methodSignature);
		} catch (MethodAnalyzerException methodAnalyzerException) {
			throw methodAnalyzerException;
		} catch (InvalidBytecodeException invalidBytecodeException) {
			throw invalidBytecodeException;
		} catch (DebugAssertFailedException debugAssertFailure) {
			throw debugAssertFailure;
		}

		if (this.frames[instructionIndex] == null) {
			this.frames[instructionIndex] = analyzedFrame;
			changedIndexes.add(INTEGER_CACHE.valueOf(instructionIndex));
		} else {
			AtomicBool changed = new AtomicBool();
			FrameNode mergedFrame = this.frames[instructionIndex].mergeWith(this.typeResolver, analyzedFrame, changed, methodSignature);
			if (changed.get()) {
				this.frames[instructionIndex] = mergedFrame;
				changedIndexes.add(INTEGER_CACHE.valueOf(instructionIndex));
			}
		}
	}

	private void traceFieldSource(AnalysisContext analysisContext, int instructionIndex) {
		WideConstantPoolInsnNode insn = (WideConstantPoolInsnNode)this.insnList.get(instructionIndex);
		FieldTypeWriter fieldWriter = (FieldTypeWriter)insn.getTypeWriter();
		AbstractFieldNode targetField = (AbstractFieldNode)fieldWriter.getTargetMember();
		if (targetField != null && targetField.isWorkspaceNode()) {
			FieldID fieldId = targetField.toFieldId();
			HashSet<Reference> references;
			if (analysisContext.fieldReferencesCache.containsKey(fieldId)) {
				references = analysisContext.fieldReferencesCache.get(fieldId);
			} else {
				references = GlobalUtils.newHashSet(13);
				MultiMap<InstructionListNode, Integer> writeIndexesByMethod = analysisContext.fieldWritesByField.get(fieldId);
				if (writeIndexesByMethod != null) {
					Enumeration<InstructionListNode> methodEnumeration = writeIndexesByMethod.keys();

					while (methodEnumeration.hasMoreElements()) {
						InstructionListNode methodNode = methodEnumeration.nextElement();
						List<Integer> writeIndexes = writeIndexesByMethod.get(methodNode);

						try {
							HashSet<Reference> readReferences = methodNode.getOrCreateMethodAnalyzerFromCache(analysisContext.methodAnalyzerCache, analysisContext.typeResolver, this.classHierarchy)
								.traceFieldReadSources(analysisContext, (FieldNode)targetField, writeIndexes);
							Iterator<Reference> referenceIterator = readReferences.iterator();

							while (referenceIterator.hasNext()) {
								if (referenceIterator.next() instanceof NullReference) {
									referenceIterator.remove();
								}
							}

							references.addAll(readReferences);
						} catch (ZKMException zkmException) {
							analysisContext.currentReferences.add(new ErrorReference(insn, "Exception '" + zkmException.getMessage() + "'"));
						}
					}
				}

				analysisContext.fieldReferencesCache.put(fieldId, references);
			}

			if (references != null) {
				analysisContext.currentReferences.addAll(references);
			}
		} else {
			String ownerName = fieldWriter.getOwnerName();
			if (fieldWriter.getName().equals("TYPE")) {
				if (ownerName.equals("java/lang/Integer")) {
					analysisContext.currentReferences.add(new PrimitiveTypeReference("I"));
				} else if (ownerName.equals("java/lang/Byte")) {
					analysisContext.currentReferences.add(new PrimitiveTypeReference("B"));
				} else if (ownerName.equals("java/lang/Character")) {
					analysisContext.currentReferences.add(new PrimitiveTypeReference("C"));
				} else if (ownerName.equals("java/lang/Short")) {
					analysisContext.currentReferences.add(new PrimitiveTypeReference("S"));
				} else if (ownerName.equals("java/lang/Long")) {
					analysisContext.currentReferences.add(new PrimitiveTypeReference("J"));
				} else if (ownerName.equals("java/lang/Float")) {
					analysisContext.currentReferences.add(new PrimitiveTypeReference("F"));
				} else if (ownerName.equals("java/lang/Double")) {
					analysisContext.currentReferences.add(new PrimitiveTypeReference("D"));
				} else if (ownerName.equals("java/lang/Boolean")) {
					analysisContext.currentReferences.add(new PrimitiveTypeReference("Z"));
				} else if (ownerName.equals("java/lang/Void")) {
					analysisContext.currentReferences.add(new PrimitiveTypeReference("V"));
				} else {
					analysisContext.currentReferences.add(new ErrorReference(insn, "Source field is external: " + ownerName + ".TYPE"));
				}
			} else {
				analysisContext.currentReferences.add(new ErrorReference(insn, "Source field is external: " + ownerName + "." + fieldWriter.getName()));
			}
		}
	}

	private ExceptionAnalyzingInfo[] getApplicableHandlersAtInsn(int instructionIndex, ExceptionAnalyzingInfo[] exceptionHandlers) {
		ArrayList<ExceptionAnalyzingInfo> applicableHandlers = new ArrayList<>();

		label33:
		for (int handlerIndex = 0; handlerIndex < exceptionHandlers.length; handlerIndex++) {
			ExceptionAnalyzingInfo handler = exceptionHandlers[handlerIndex];
			if (instructionIndex >= handler.start_pc && instructionIndex < handler.end_pc) {
				for (int applicableIndex = 0; applicableIndex < applicableHandlers.size(); applicableIndex++) {
					ExceptionAnalyzingInfo applicableHandler = applicableHandlers.get(applicableIndex);
					if (handler.desc.equals(applicableHandler.desc)
						|| this.classHierarchy.isClassInstanceOf(handler.desc.substring(1, handler.desc.length() - 1), applicableHandler.desc.substring(1, applicableHandler.desc.length() - 1))) {
						continue label33;
					}
				}

				applicableHandlers.add(handler);
			}
		}

		ExceptionAnalyzingInfo[] handlerArray = new ExceptionAnalyzingInfo[applicableHandlers.size()];
		return applicableHandlers.toArray(handlerArray);
	}

	public int[] getUnreachableIndexes() {
		ArrayList<Integer> unreachableIndexes = new ArrayList<>();
		int instructionCount = this.insnList.size();
		boolean previousUnreachable = false;

		for (int instructionIndex = 0; instructionIndex < instructionCount; instructionIndex++) {
			AbstractInsnNode insn = this.insnList.get(instructionIndex);
			if (this.frames[instructionIndex] != null) {
				previousUnreachable = false;
			} else {
				boolean nextCodeReachable = false;
				if (insn.isLabel()) {
					LabelNode label = (LabelNode)insn;
					if (instructionIndex == instructionCount - 1) {
						nextCodeReachable = true;
					} else if (label.hasFlag(512)) {
						nextCodeReachable = true;
					} else {
						for (int nextIndex = instructionIndex + 1; nextIndex < instructionCount; nextIndex++) {
							if (!this.insnList.get(nextIndex).isLabel()) {
								if (this.frames[nextIndex] != null) {
									nextCodeReachable = true;
								}
								break;
							}
						}
					}
				}

				if (previousUnreachable || !nextCodeReachable) {
					unreachableIndexes.add(INTEGER_CACHE.valueOf(instructionIndex));
					previousUnreachable = true;
				}
			}
		}

		int[] indexArray = new int[unreachableIndexes.size()];

		for (int i = 0; i < indexArray.length; i++) {
			indexArray[i] = unreachableIndexes.get(i);
		}

		return indexArray;
	}

	private void traceVariableLoadSources(AnalysisContext analysisContext, VariableTracker variableTracker, AnalyzingNode analyzingNode, int startIndex, int localSlotIndex) {
		if (!variableTracker.isVisited(2, startIndex, localSlotIndex)) {
			variableTracker.markVisited(2, startIndex, localSlotIndex);
			int endIndex = analyzingNode.getEndIndex();

			for (int instructionIndex = startIndex; instructionIndex < endIndex; instructionIndex++) {
				AbstractInsnNode insn = this.insnList.get(instructionIndex);
				FrameNode frame = this.frames[instructionIndex];
				if (insn.isThatVarStore(localSlotIndex)) {
					return;
				}

				if (insn.isThatVarLoad(localSlotIndex)) {
					int stackTopIndex = frame.getOperandStack().length - 1;
					this.traceVariableTrackerSources(analysisContext, variableTracker, analyzingNode, instructionIndex + 1, stackTopIndex);
				}
			}

			List<AnalyzingNode> successors = analyzingNode.getSuccessors();
			if (successors != null && successors.size() > 0) {
				for (int successorIndex = 0; successorIndex < successors.size(); successorIndex++) {
					AnalyzingNode successor = successors.get(successorIndex);
					this.traceVariableLoadSources(analysisContext, variableTracker, successor, successor.getStartIndex(), localSlotIndex);
				}
			} else if (analyzingNode.hasJsrRetNodes()) {
				List<AnalyzingNode> jsrRetNodes = analyzingNode.getJsrRetNodes();

				for (int jsrRetIndex = 0; jsrRetIndex < jsrRetNodes.size(); jsrRetIndex++) {
					AnalyzingNode jsrRetNode = jsrRetNodes.get(jsrRetIndex);
					this.traceVariableLoadSources(analysisContext, variableTracker, jsrRetNode, jsrRetNode.getStartIndex(), localSlotIndex);
				}
			}
		}
	}

	private boolean isGetPropertyWithDefault(AbstractMethodCallWriter methodCall, AbstractWorkspace workspace) {
		return this.isGetPropertyCall(methodCall, "getProperty(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", workspace);
	}

	public List<ControlFlowNode> getControlFlowNodes() {
		return this.controlFlowNodes;
	}

	public boolean collectStackProducerIndexes(AnalyzingNode analyzingNode, int startIndex, int stackIndex, Set<Integer> producerIndexes, Set<AnalyzingNode> visitedNodes) {
		if (!visitedNodes.add(analyzingNode)) {
			return true;
		} else {
			boolean producerFound = false;
			int nodeStartIndex = analyzingNode.getStartIndex();

			for (int instructionIndex = startIndex - 1; !producerFound && instructionIndex >= nodeStartIndex; instructionIndex--) {
				AbstractInsnNode insn = this.insnList.get(instructionIndex);
				FrameNode frame = this.frames[instructionIndex];
				if (insn.isPureStackOpcode()) {
					StackFrameType[] operandStack = frame.getOperandStack();
					if (insn.isProducedByStackOp(operandStack, stackIndex)) {
						return false;
					}

					stackIndex = insn.getStackIndexOrigin(operandStack, stackIndex);
				} else {
					if (insn.isJump()) {
						return false;
					}

					if (insn.isLabel()
						&& (((LabelNode)insn).hasFlag(1) || ((LabelNode)insn).hasFlag(256) || ((LabelNode)insn).hasFlag(512) || ((LabelNode)insn).hasFlag(1024))) {
						return false;
					}

					if (frame.getStackSize() - 1 == stackIndex && insn.isStackProducer()) {
						producerIndexes.add(INTEGER_CACHE.valueOf(instructionIndex));
						producerFound = true;
					}
				}
			}

			if (!producerFound) {
				List<AnalyzingNode> predecessors = analyzingNode.getPredecessors();
				if (predecessors != null && predecessors.size() > 0) {
					for (int predecessorIndex = 0; predecessorIndex < predecessors.size(); predecessorIndex++) {
						AnalyzingNode predecessor = predecessors.get(predecessorIndex);
						if (!this.collectStackProducerIndexes(predecessor, predecessor.getEndIndex() + 1, stackIndex, producerIndexes, visitedNodes)) {
							return false;
						}
					}
				}
			}

			return true;
		}
	}

	public int computeMaxStackDepth() {
		if (!this.released && this.frames != null) {
			int maxStackDepth = 0;

			for (FrameNode frame : this.frames) {
				if (frame != null) {
					int stackDepth = frame.getStackDepth();
					if (stackDepth > maxStackDepth) {
						maxStackDepth = stackDepth;
					}
				}
			}

			return maxStackDepth;
		} else {
			throw new IllegalStateException("Closed object");
		}
	}

	private void appendReachableNodes(AnalyzingNode analyzingNode, DualKeyMap<AnalyzingNode, AnalyzingNode, Boolean> jsrReturnLinks, List<AnalyzingNode> orderedNodes) {
		orderedNodes.add(analyzingNode);
		Map<AnalyzingNode, Boolean> linkedNodes = jsrReturnLinks.getGroup(analyzingNode);
		if (linkedNodes != null) {
			Iterator<AnalyzingNode> nodeIterator = linkedNodes.keySet().iterator();

			while (nodeIterator.hasNext()) {
				this.appendReachableNodes(nodeIterator.next(), jsrReturnLinks, orderedNodes);
			}
		}
	}

	private void buildJsrReturnLinks(
		AnalyzingNode jsrEntryNode, AnalyzingNode analyzingNode, DualKeyMap<AnalyzingNode, AnalyzingNode, Boolean> jsrReturnLinks, Set<AnalyzingNode> visitedNodes, boolean viaCatchNode
	) {
		if (!visitedNodes.contains(analyzingNode)) {
			visitedNodes.add(analyzingNode);
			AnalyzingNode continuationNode = analyzingNode.getJsrContinuationNode();
			if (continuationNode != null) {
				LabelNode jsrTargetLabel = ((JumpInsnNode)this.insnList.get(analyzingNode.getEndIndex())).getTargetLabel();
				AnalyzingNode jsrTargetNode = this.getNodeByStartIndex(jsrTargetLabel.getLabelID());
				Boolean viaCatchValue = viaCatchNode ? Boolean.TRUE : Boolean.FALSE;
				if (jsrEntryNode != jsrTargetNode) {
					Boolean previousValue = jsrReturnLinks.put(jsrEntryNode, jsrTargetNode, viaCatchValue);
					if (previousValue != null && !previousValue && viaCatchValue) {
						jsrReturnLinks.put(jsrEntryNode, jsrTargetNode, previousValue);
					}
				}

				this.buildJsrReturnLinks(jsrEntryNode, continuationNode, jsrReturnLinks, visitedNodes, viaCatchNode);
			} else {
				Enumeration<AnalyzingNode> successorEnumeration = analyzingNode.enumerateSuccessors();
				if (successorEnumeration != null) {
					while (successorEnumeration.hasMoreElements()) {
						AnalyzingNode successorNode = successorEnumeration.nextElement();
						this.buildJsrReturnLinks(jsrEntryNode, successorNode, jsrReturnLinks, visitedNodes, viaCatchNode);
					}
				}
			}

			Enumeration<AnalyzingNode> catchEnumeration = analyzingNode.enumerateCatchNodes();

			while (catchEnumeration.hasMoreElements()) {
				AnalyzingNode catchNode = catchEnumeration.nextElement();
				this.buildJsrReturnLinks(jsrEntryNode, catchNode, jsrReturnLinks, visitedNodes, true);
			}
		}
	}

	public AnalyzedFrame getAnalyzedFrameAtInsn(int instructionIndex, boolean compactFrame) {
		return this.frames[instructionIndex] != null ? this.frames[instructionIndex].createAnalyzedFrame(compactFrame) : null;
	}

	private boolean isClassGetComponentTypeCall(AbstractMethodCallWriter methodCall, AbstractWorkspace workspace) {
		try {
			if (!methodCall.getSignatureKey().equals("getComponentType()Ljava/lang/Class;")) {
				return false;
			} else {
				String ownerName = methodCall.getOwnerName();
				return ownerName.equals("java/lang/Class") || workspace.isClassInstanceOf(ownerName, "java/lang/Class");
			}
		} catch (FileException ignored) {
			return false;
		}
	}

	private void traceVariableTrackerSources(AnalysisContext analysisContext, VariableTracker variableTracker, AnalyzingNode analyzingNode, int startIndex, int stackIndex) {
		if (!variableTracker.isVisited(1, startIndex, stackIndex)) {
			variableTracker.markVisited(1, startIndex, stackIndex);
			boolean scanComplete = false;
			int endIndex = analyzingNode.getEndIndex();

			for (int instructionIndex = startIndex; !scanComplete && instructionIndex <= endIndex; instructionIndex++) {
				AbstractInsnNode instruction = this.insnList.get(instructionIndex);
				FrameNode frame = this.frames[instructionIndex];
				if (instruction.isPureStackOpcode()) {
					int[] stackIndexMapping = instruction.mapOldStackIndexToNew(this.frames[instructionIndex - 1].getOperandStack(), frame.getOperandStack(), stackIndex);
					if (stackIndexMapping.length == 0) {
						scanComplete = true;
					} else if (stackIndexMapping.length == 2) {
						this.traceVariableTrackerSources(analysisContext, variableTracker, analyzingNode, instructionIndex + 1, stackIndexMapping[1]);
					}

					if (stackIndexMapping.length > 0) {
						stackIndex = stackIndexMapping[0];
					}
				} else if (instruction.consumesStackFromIndex(stackIndex, frame.getOperandStack().length)) {
					if (!variableTracker.traceInsnSources(analysisContext, instructionIndex, analyzingNode)) {
						switch (instruction.getOpcode()) {
							case PUTSTATIC:
							case PUTFIELD:
								this.traceFieldUsage(analysisContext, variableTracker, instructionIndex);
								break;
							case 255:
								VarInsnNode varInstruction = (VarInsnNode)instruction;
								LocalVarOperationType operationType = varInstruction.getOperationType();
								varInstruction.getOperationType();
								if (operationType == LocalVarOperationType.OBJECT_STORE) {
									this.traceVariableLoadSources(analysisContext, variableTracker, analyzingNode, instructionIndex + 1, varInstruction.getVariableIndex());
								}
						}
					}

					scanComplete = true;
				}
			}

			if (!scanComplete) {
				List<AnalyzingNode> successors = analyzingNode.getSuccessors();
				if (successors != null && successors.size() > 0) {
					for (int successorIndex = 0; successorIndex < successors.size(); successorIndex++) {
						AnalyzingNode successorNode = successors.get(successorIndex);
						this.traceVariableTrackerSources(analysisContext, variableTracker, successorNode, successorNode.getStartIndex(), stackIndex);
					}
				} else if (analyzingNode.hasJsrRetNodes()) {
					List<AnalyzingNode> jsrRetNodes = analyzingNode.getJsrRetNodes();

					for (int jsrRetNodeIndex = 0; jsrRetNodeIndex < jsrRetNodes.size(); jsrRetNodeIndex++) {
						AnalyzingNode jsrRetNode = jsrRetNodes.get(jsrRetNodeIndex);
						this.traceVariableTrackerSources(analysisContext, variableTracker, jsrRetNode, jsrRetNode.getStartIndex(), stackIndex);
					}
				} else {
					AbstractInsnNode endInstruction = this.insnList.get(endIndex);
				}
			}
		}
	}

	private void resolveLambdaSources(int instructionIndex, AtomicReference<Reference> lambdaNameRef, AtomicReference<Reference> interfaceSignatureRef, AtomicReference<Reference> interfaceClassRef) {
		AbstractInsnNode lambdaInstruction = this.insnList.get(instructionIndex);
		if (this.insnList.size() > instructionIndex + 1) {
			AbstractInsnNode nextInstruction = this.insnList.get(instructionIndex + 1);
			if (nextInstruction.isString()) {
				StringNode lambdaNameNode;
				if (nextInstruction.getOpcode() == LDC) {
					lambdaNameNode = (StringNode)((LdcNode)nextInstruction).getTypeWriter();
				} else {
					lambdaNameNode = (StringNode)((WideConstantPoolInsnNode)nextInstruction).getTypeWriter();
				}

				lambdaNameRef.set(new StringReference(lambdaNameNode));
				StringNode interfaceClassNameNode = null;

				for (int backwardIndex = instructionIndex - 1; backwardIndex > 0; backwardIndex--) {
					AbstractInsnNode backwardInstruction = this.insnList.get(backwardIndex);
					if (backwardInstruction.getOpcode() == INVOKEVIRTUAL && ((MethodCallWriter)((WideConstantPoolInsnNode)backwardInstruction).getTypeWriter()).getName().equals("getFunctionalInterfaceClass")) {
						AbstractInsnNode classNameInstruction = this.insnList.get(backwardIndex + 1);
						if (classNameInstruction.isString()) {
							if (classNameInstruction.getOpcode() == LDC) {
								interfaceClassNameNode = (StringNode)((LdcNode)classNameInstruction).getTypeWriter();
							} else {
								interfaceClassNameNode = (StringNode)((WideConstantPoolInsnNode)classNameInstruction).getTypeWriter();
							}

							interfaceClassRef.set(new StringReference(interfaceClassNameNode));
						}
						break;
					}
				}

				if (interfaceClassNameNode != null) {
					int instructionCount = this.insnList.size();

					for (int forwardIndex = instructionIndex + 2; forwardIndex < instructionCount; forwardIndex++) {
						AbstractInsnNode forwardInstruction = this.insnList.get(forwardIndex);
						if (forwardInstruction.getOpcode() == INVOKEVIRTUAL
							&& ((MethodCallWriter)((WideConstantPoolInsnNode)forwardInstruction).getTypeWriter()).getName().equals("getFunctionalInterfaceMethodSignature")) {
							if (forwardIndex + 1 < instructionCount) {
								AbstractInsnNode signatureInstruction = this.insnList.get(forwardIndex + 1);
								if (signatureInstruction.isString()) {
									StringNode interfaceSignatureNode;
									if (signatureInstruction.getOpcode() == LDC) {
										interfaceSignatureNode = (StringNode)((LdcNode)signatureInstruction).getTypeWriter();
									} else {
										interfaceSignatureNode = (StringNode)((WideConstantPoolInsnNode)signatureInstruction).getTypeWriter();
									}

									interfaceSignatureRef.set(new StringReference(interfaceSignatureNode));
								}
							}
							break;
						}
					}

					if (interfaceSignatureRef.isNull()) {
						interfaceSignatureRef.set(new ErrorReference(lambdaInstruction, "Unexpected usage (B) : " + lambdaInstruction.getOpcodeMnemonics() + " : " + this.methodAnalyzing));
					}
				} else {
					interfaceSignatureRef.set(new ErrorReference(lambdaInstruction, "Unexpected usage (C) : " + lambdaInstruction.getOpcodeMnemonics() + " : " + this.methodAnalyzing));
					interfaceClassRef.set(new ErrorReference(lambdaInstruction, "Unexpected usage (D) "));
				}
			} else {
				lambdaNameRef.set(new ErrorReference(nextInstruction, "Unexpected instruction : " + nextInstruction.getOpcodeMnemonics() + " : " + this.methodAnalyzing));
				interfaceSignatureRef.set(new ErrorReference(lambdaInstruction, "Unexpected usage (E) "));
				interfaceClassRef.set(new ErrorReference(lambdaInstruction, "Unexpected usage (F) "));
			}
		} else {
			lambdaNameRef.set(new ErrorReference(lambdaInstruction, "Unexpected usage (G) : " + this.methodAnalyzing + " : " + instructionIndex + " : " + this.insnList.size()));
			interfaceSignatureRef.set(new ErrorReference(lambdaInstruction, "Unexpected usage (H) "));
			interfaceClassRef.set(new ErrorReference(lambdaInstruction, "Unexpected usage (catchEntryNodes) "));
		}
	}

	private boolean propagateUninitializedUsage(Collection<AnalyzingNode> catchNodes) {
		boolean usagePropagated = false;

		for (AnalyzingNode catchNode : catchNodes) {
			BitSet uninitializedUsageMask = this.frames[catchNode.getStartIndex()].getUninitializedUsageMask();
			ArrayList<AnalyzingNode> reachableNodes = new ArrayList<>(this.reachableNodesByCatchNode.getGroup(catchNode).keySet());
			Collections.sort(reachableNodes);
			Collections.reverse(reachableNodes);

			for (AnalyzingNode reachableNode : reachableNodes) {
				int overlapEndIndex = this.computeCatchOverlapEndIndex(reachableNode, catchNode);
				if (this.markUninitializedLocalUsage(reachableNode, overlapEndIndex, (BitSet)uninitializedUsageMask.clone())) {
					usagePropagated = true;
				}
			}
		}

		return usagePropagated;
	}

	public void assignNodeIndexes() {
		for (int nodeIndex = 0; nodeIndex < this.nodes.size(); nodeIndex++) {
			this.nodes.get(nodeIndex).setNodeIndex(nodeIndex);
		}
	}

	public FrameNode[] getFrames() {
		return (FrameNode[])this.frames.clone();
	}

	private void traceArrayDimensionSource(AnalysisContext analysisContext, AnalyzingNode analyzingNode, int instructionIndex) {
		WideConstantPoolInsnNode constantInstruction = (WideConstantPoolInsnNode)this.insnList.get(instructionIndex);
		if (instructionIndex > analyzingNode.getStartIndex()) {
			AbstractInsnNode previousInstruction = this.insnList.get(instructionIndex - 1);
			int iValue;
			switch (previousInstruction.getOpcode()) {
				case ICONST_0:
					iValue = 0;
					break;
				case ICONST_1:
					iValue = 1;
					break;
				case ICONST_2:
					iValue = 2;
					break;
				case ICONST_3:
					iValue = 3;
					break;
				case ICONST_4:
					iValue = 4;
					break;
				case ICONST_5:
					iValue = 5;
					break;
				case LCONST_0:
				case LCONST_1:
				case FCONST_0:
				case FCONST_1:
				case FCONST_2:
				case DCONST_0:
				case DCONST_1:
				default:
					analysisContext.currentReferences.add(new ErrorReference(constantInstruction, "Instruction not handled 1) : " + previousInstruction + " " + analysisContext.analysisType));
					return;
				case BIPUSH:
					iValue = ((BiPushNode)previousInstruction).getValue();
			}

			FrameNode frame = this.frames[instructionIndex];
			if (iValue > 0) {
				ArrayDimensionTracker dimensionTracker = this.createArrayDimensionTracker();
				this.traceVariableTrackerSources(analysisContext, dimensionTracker, analyzingNode, instructionIndex + 1, frame.getOperandStack().length - 1);
				ArrayReference arrayReference = new ArrayReference(iValue, dimensionTracker.getErrors());

				for (int dimensionIndex = 0; dimensionIndex < iValue; dimensionIndex++) {
					HashSet<Reference> elementReferences = dimensionTracker.getElementReferences(dimensionIndex);
					if (elementReferences == null || elementReferences.size() == 0) {
						elementReferences = GlobalUtils.newHashSet(3);
						elementReferences.add(new ErrorReference(constantInstruction, dimensionTracker.getErrors().toString()));
					}

					arrayReference.setElementReferences(dimensionIndex, elementReferences);
				}

				analysisContext.currentReferences.add(arrayReference);
			} else {
				analysisContext.currentReferences.add(new ArrayReference(iValue));
			}
		} else {
			analysisContext.currentReferences.add(new ErrorReference(constantInstruction, "Instruction not handled 2) : " + analysisContext.analysisType));
		}
	}

	private void traceMethodCallSource(AnalysisContext analysisContext, AnalyzingNode analyzingNode, int instructionIndex) {
		WideConstantPoolInsnNode methodCallInstruction = (WideConstantPoolInsnNode)this.insnList.get(instructionIndex);
		AbstractMethodCallWriter methodCallWriter = (AbstractMethodCallWriter)methodCallInstruction.getTypeWriter();
		switch (analysisContext.analysisType) {
			case NORMAL:
				if (this.isGetPropertyWithDefault(methodCallWriter, analysisContext.workspace)) {
					if (analysisContext.propertyNameTrace) {
						analysisContext.currentReferences.add(new ErrorReference(methodCallInstruction, "Possible \"getProperty\" call loop"));
					} else {
						HashSet<Reference> withDefaultKeyReferences = GlobalUtils.newHashSet(13);
						HashSet<Reference> withDefaultOuterReferences = analysisContext.swapCurrentReferences(withDefaultKeyReferences);
						int withDefaultStackIndex = this.frames[instructionIndex].getStackSize();
						this.traceStackSources(analysisContext, analyzingNode, instructionIndex, withDefaultStackIndex);
						HashSet<Reference> withDefaultValueReferences = GlobalUtils.newHashSet(13);
						analysisContext.swapCurrentReferences(withDefaultValueReferences);
						this.traceStackSources(analysisContext, analyzingNode, instructionIndex, withDefaultStackIndex + 1);
						HashSet<Reference> withDefaultFileReferences = GlobalUtils.newHashSet(13);
						analysisContext.swapCurrentReferences(withDefaultFileReferences);
						this.traceStackSources(analysisContext, analyzingNode, instructionIndex, --withDefaultStackIndex);
						withDefaultOuterReferences.add(new PropertyGetterResult(withDefaultKeyReferences, withDefaultValueReferences, withDefaultFileReferences));
						analysisContext.swapCurrentReferences(withDefaultOuterReferences);
					}
				} else if (this.isGetPropertyNoDefault(methodCallWriter, analysisContext.workspace)) {
					if (analysisContext.propertyNameTrace) {
						analysisContext.currentReferences.add(new ErrorReference(methodCallInstruction, "Possible \"getProperty\" call loop"));
					} else {
						HashSet<Reference> withoutDefaultKeyReferences = GlobalUtils.newHashSet(13);
						HashSet<Reference> withoutDefaultOuterReferences = analysisContext.swapCurrentReferences(withoutDefaultKeyReferences);
						int withoutDefaultStackIndex = this.frames[instructionIndex].getStackSize();
						this.traceStackSources(analysisContext, analyzingNode, instructionIndex, withoutDefaultStackIndex);
						HashSet<Reference> withoutDefaultFileReferences = GlobalUtils.newHashSet(13);
						analysisContext.swapCurrentReferences(withoutDefaultFileReferences);
						this.traceStackSources(analysisContext, analyzingNode, instructionIndex, --withoutDefaultStackIndex);
						withoutDefaultOuterReferences.add(new PropertyGetterResult(withoutDefaultKeyReferences, withoutDefaultFileReferences));
						analysisContext.swapCurrentReferences(withoutDefaultOuterReferences);
					}
				} else {
					analysisContext.currentReferences.add(new ErrorReference(methodCallInstruction, "Method call not handled: '" + methodCallWriter.getValueAsString() + "' : " + analysisContext.analysisType));
				}
				break;
			case CLASS_REFERENCE:
			case METHODTYPE_REFERENCE:
				if (analysisContext.reflectionPatterns.containsKey(methodCallWriter)) {
					analysisContext.currentReferences.add(new PendingReference(instructionIndex, this.instructionListNode));
				} else {
					AbstractMethodNode targetMember = (AbstractMethodNode)methodCallWriter.getTargetMember();
					if (targetMember != null && targetMember.isWorkspaceNode()) {
						MethodNode targetMethodNode = (MethodNode)targetMember;
						MethodID methodId = targetMethodNode.toMethodId();
						MethodAnalysisResult returnSourceResult = null;
						if (analysisContext.methodAnalysisResults.containsKey(methodId)) {
							returnSourceResult = analysisContext.methodAnalysisResults.get(methodId);
						} else {
							InstructionListNode targetInstructionList = targetMethodNode.getInstructionList();
							if (targetInstructionList != null) {
								try {
									returnSourceResult = targetInstructionList.getOrCreateMethodAnalyzerFromCache(analysisContext.methodAnalyzerCache, analysisContext.typeResolver, this.classHierarchy).analyzeMethodReturnSource(analysisContext);
									analysisContext.methodAnalysisResults.put(methodId, returnSourceResult);
								} catch (ZKMException analysisException) {
									analysisContext.currentReferences
										.add(new ErrorReference(methodCallInstruction, "Exception '" + analysisException.getMessage() + "' : " + targetMethodNode.getInternalName() + "." + targetMethodNode.getName()));
								}
							} else {
								analysisContext.methodAnalysisResults.put(methodId, null);
							}
						}

						if (returnSourceResult != null) {
							if (returnSourceResult.hasStringNode()) {
								returnSourceResult.getStringNode();
								analysisContext.currentReferences.add(new StringReference(returnSourceResult.getStringNode()));
							} else if (returnSourceResult.hasParamSlot()) {
								int paramSlot = returnSourceResult.getParamSlot();
								int paramStackIndex = this.frames[instructionIndex].getStackSize() + targetMethodNode.getInvokeStackShift() + targetMethodNode.getParamIndexBySlot(paramSlot);
								AnalysisContext paramContext = new AnalysisContext(this, analysisContext, AnalysisType.NORMAL);
								this.traceStackSources(paramContext, analyzingNode, instructionIndex, paramStackIndex);
							} else if (returnSourceResult.hasErrorMessage()) {
								analysisContext.currentReferences.add(new ErrorReference(methodCallInstruction, returnSourceResult.getErrorMessage() + " : " + methodCallInstruction.getTypeWriter().getValueAsString()));
							} else if (returnSourceResult.hasReference()) {
								ArrayReference arrayReference = returnSourceResult.getReference();
								if (arrayReference.isElementTypeResolved()) {
									ArrayList<Reference> elementReferences = arrayReference.getFirstElementReferences();

									for (int elementIndex = 0; elementIndex < elementReferences.size(); elementIndex++) {
										Reference elementReference = elementReferences.get(elementIndex);
										if (elementReference instanceof MethodParameterReference) {
											MethodParameterReference elementParameterReference = (MethodParameterReference)elementReference;
											if (elementParameterReference.getInstructionList() == returnSourceResult.getInsnList()) {
												int elementSlot = elementParameterReference.getSlotIndex();
												int elementStackIndex = this.frames[instructionIndex].getStackSize() + targetMethodNode.getInvokeStackShift() + targetMethodNode.getParamIndexBySlot(elementSlot);
												AnalysisContext elementContext = new AnalysisContext(this, analysisContext, AnalysisType.NORMAL);
												this.traceStackSources(elementContext, analyzingNode, instructionIndex, elementStackIndex);
											} else {
												analysisContext.currentReferences
													.add(
														new ErrorReference(
															methodCallInstruction,
															"Method Parameter : "
																+ elementParameterReference.getSlotIndex()
																+ " in "
																+ elementParameterReference.getInstructionList().getMethodNode().toDeclarationWithParamNames()
																+ " : "
																+ methodCallInstruction.getTypeWriter().getValueAsString()
														)
													);
											}
										} else {
											analysisContext.currentReferences.add(elementReference);
										}
									}
								} else {
									analysisContext.currentReferences.add(new ErrorReference(methodCallInstruction, "Multiple sources : " + methodCallInstruction.getTypeWriter().getValueAsString()));
								}
							}
						} else {
							analysisContext.currentReferences
								.add(new ErrorReference(methodCallInstruction, "Method was abstract or in an interface: " + analysisContext.analysisType + " : " + methodCallInstruction.getTypeWriter().getValueAsString()));
						}
					} else if (methodCallWriter.getOwnerName().equals("java/lang/Object")
						&& methodCallWriter.getSignatureKey().equals("getClass()Ljava/lang/Class;")
						&& instructionIndex > analyzingNode.getStartIndex()) {
						String ownerTypeName = TypeResolver.toInternalName(this.frames[instructionIndex - 1].getOperandStack()[this.frames[instructionIndex].getStackSize() - 1].getDescriptor());
						analysisContext.currentReferences.add(new InstructionTypeReference(methodCallInstruction, ownerTypeName));
					} else {
						analysisContext.currentReferences.add(new ErrorReference(methodCallInstruction, "Method call not internal: " + analysisContext.analysisType));
					}
				}
				break;
			case USER_DEFINED_METHOD_1:
				ReflectionMethodPattern reflectionPattern = methodCallWriter.getPatternFromRegistry();
				if (reflectionPattern != null) {
					List<ReflectionParameter> reflectionParameters = reflectionPattern.getParameters();

					for (int reflectionParameterIndex = 0; reflectionParameterIndex < reflectionParameters.size(); reflectionParameterIndex++) {
						ReflectionParameter reflectionParameter = reflectionParameters.get(reflectionParameterIndex);
						if (reflectionParameter.isClassNameParameter() || reflectionParameter.isClassOrPropertiesNameParameter()) {
							int classNameStackIndex = this.frames[instructionIndex].getStackSize() + reflectionPattern.getMethodTypeStackOffset() + reflectionParameter.getParameterIndex();
							AnalysisContext reflectionContext = new AnalysisContext(this, analysisContext, AnalysisType.USER_DEFINED_METHOD_2, analysisContext.nestingLevel);
							this.traceStackSources(reflectionContext, analyzingNode, instructionIndex, classNameStackIndex);
							return;
						}
					}
				} else if (this.isClassGetComponentTypeCall(methodCallWriter, analysisContext.workspace)) {
					HashSet<Reference> targetObjectReferences = GlobalUtils.newHashSet(13);
					HashSet<Reference> accumulatedReferences = analysisContext.swapCurrentReferences(targetObjectReferences);
					int objectStackIndex = this.frames[instructionIndex].getStackSize() - 1;
					this.traceStackSources(analysisContext, analyzingNode, instructionIndex, objectStackIndex);
					analysisContext.swapCurrentReferences(accumulatedReferences);
					if (targetObjectReferences.size() == 0) {
						accumulatedReferences.add(new ErrorReference(methodCallInstruction, "Unknown object for '" + methodCallWriter.getValueAsString() + "' call"));
					} else {
						for (Reference objectReference : targetObjectReferences) {
							if (objectReference instanceof MethodParameterReference) {
								MethodParameterReference objectParameterReference = (MethodParameterReference)objectReference;
								HashSet<Reference> referencingMethodReferences = GlobalUtils.newHashSet(13);
								accumulatedReferences = analysisContext.swapCurrentReferences(referencingMethodReferences);
								this.analyzeReferencingMethods(analysisContext, objectParameterReference.getSlotIndex());
								analysisContext.swapCurrentReferences(accumulatedReferences);

								for (Reference referencingReference : referencingMethodReferences) {
									if (referencingReference instanceof StringReference) {
										accumulatedReferences.add(new ClassTypeReference(((StringReference)referencingReference).getStringNode()));
									} else if (referencingReference instanceof ErrorReference) {
										accumulatedReferences.add(objectReference);
									}
								}
							} else if (objectReference instanceof StringReference) {
								accumulatedReferences.add(new ClassTypeReference(((StringReference)objectReference).getStringNode()));
							} else if (objectReference instanceof ErrorReference) {
								accumulatedReferences.add(objectReference);
							}
						}
					}
				} else {
					analysisContext.currentReferences.add(new ErrorReference(methodCallInstruction, "Source is method: " + methodCallInstruction.getTypeWriter().getValueAsString() + " : " + analysisContext.analysisType));
				}
				break;
			case USER_DEFINED_METHOD_2:
				analysisContext.currentReferences.add(new ErrorReference(methodCallInstruction, "Source is method: " + methodCallInstruction.getTypeWriter().getValueAsString() + " : " + analysisContext.analysisType));
		}
	}

	private int computeCatchOverlapEndIndex(AnalyzingNode analyzingNode, AnalyzingNode catchNode) {
		int overlapEndIndex = analyzingNode.getStartIndex();

		for (ExceptionAnalyzingInfo exceptionInfo : this.exceptionInfoByCatchNode.get(catchNode)) {
			if (analyzingNode.getStartIndex() < exceptionInfo.end_pc && analyzingNode.getEndIndex() >= exceptionInfo.start_pc) {
				int exceptionEndIndex;
				if (exceptionInfo.end_pc > analyzingNode.getEndIndex()) {
					exceptionEndIndex = analyzingNode.getEndIndex();
				} else {
					exceptionEndIndex = Math.max(0, exceptionInfo.end_pc - 1);
				}

				if (exceptionEndIndex > overlapEndIndex) {
					overlapEndIndex = exceptionEndIndex;
				}
			}
		}

		return overlapEndIndex;
	}

	private void pruneDisconnectedGraph(DualKeyMap<AnalyzingNode, AnalyzingNode, AnalyzingNode> reachableNodesByCatchNode) {
		if (this.nodes.size() != 1) {
			int nodeCount = this.nodes.size();
			ArrayList<AnalyzingNode> unnamedNodes = null;

			for (int nodeIndex = 0; nodeIndex < nodeCount; nodeIndex++) {
				AnalyzingNode node = this.nodes.get(nodeIndex);
				if (node.getName() == null) {
					if (unnamedNodes == null) {
						unnamedNodes = new ArrayList<>();
					}

					unnamedNodes.add(node);
				}
			}

			if (unnamedNodes != null) {
				for (int unnamedNodeIndex = 0; unnamedNodeIndex < unnamedNodes.size(); unnamedNodeIndex++) {
					this.nodes.remove(unnamedNodes.get(unnamedNodeIndex));
				}
			}

			HashSet<AnalyzingNode> connectedNodes = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(this.nodes.size()));
			this.collectConnectedNodes(this.headerNode, connectedNodes);
			RingQueue<AnalyzingNode> catchNodeQueue = new RingQueue<>();

			for (int catchEntryIndex = 0; catchEntryIndex < this.catchEntryNodes.size(); catchEntryIndex++) {
				catchNodeQueue.push(this.catchEntryNodes.get(catchEntryIndex));
			}

			boolean progressMade = true;

			while (!catchNodeQueue.isEmpty() && progressMade) {
				int batchSize = catchNodeQueue.size();
				int previousConnectedCount = connectedNodes.size();

				for (int batchIndex = 0; batchIndex < batchSize; batchIndex++) {
					AnalyzingNode catchNode = catchNodeQueue.pop();
					Map<AnalyzingNode, AnalyzingNode> reachableGroup = reachableNodesByCatchNode.getGroup(catchNode);
					boolean groupConnected = false;
					if (reachableGroup != null) {
						for (AnalyzingNode reachableNode : reachableGroup.keySet()) {
							if (connectedNodes.contains(reachableNode)) {
								this.collectConnectedNodes(catchNode, connectedNodes);
								groupConnected = true;
								break;
							}
						}
					}

					if (!groupConnected) {
						catchNodeQueue.push(catchNode);
					}
				}

				progressMade = previousConnectedCount != connectedNodes.size();
			}

			while (!catchNodeQueue.isEmpty()) {
				AnalyzingNode unconnectedCatchNode = catchNodeQueue.pop();
				this.catchEntryNodes.remove(unconnectedCatchNode);
				this.exceptionInfoByCatchNode.removeAll(unconnectedCatchNode);
			}

			if (nodeCount > connectedNodes.size()) {
				this.nodes.clear();
				this.nodes.addAll(connectedNodes);
				Collections.sort(this.nodes);
				this.nodeByInstructionIndexCache = null;
			}
		}
	}

	public AnalyzingNode getNodeByIndex(int index) {
		if (index >= this.nodes.size()) {
			throw new IllegalArgumentException("Invalid index : " + index + " > " + this.nodes.size());
		} else {
			return this.nodes.get(index);
		}
	}

	private Set<AnalyzingNode> getUninitializedStartNodes() {
		TreeSet<AnalyzingNode> startNodes = new TreeSet<>((firstNode, secondNode) -> secondNode.compareByStartIndex(firstNode));

		for (AnalyzingNode node : this.nodes) {
			if (!node.hasSuccessors()) {
				startNodes.add(node);
			} else {
				List<AnalyzingNode> successors = node.getSuccessors();
				boolean backReferenceFound = false;

				for (AnalyzingNode successorNode : successors) {
					if (successorNode.hasPredecessors() && successorNode.getPredecessors().contains(node)) {
						backReferenceFound = true;
					}
				}

				if (!backReferenceFound) {
					startNodes.add(node);
				}
			}
		}

		return startNodes;
	}

	private void traceFieldUsage(AnalysisContext analysisContext, VariableTracker variableTracker, int instructionIndex) {
		FieldTypeWriter fieldWriter = (FieldTypeWriter)((WideConstantPoolInsnNode)this.insnList.get(instructionIndex)).getTypeWriter();
		AbstractFieldNode fieldNode = (AbstractFieldNode)fieldWriter.getTargetMember();
		if (fieldNode != null && fieldNode.isWorkspaceNode()) {
			FieldID fieldId = fieldNode.toFieldId();
			MultiMap<InstructionListNode, Integer> fieldWrites = analysisContext.fieldWritesByField.get(fieldId);
			if (fieldWrites != null) {
				if (fieldWrites.valueCount() == 1) {
					MultiMap<InstructionListNode, Integer> fieldReads = analysisContext.fieldReadsByField.get(fieldId);
					if (fieldReads != null) {
						Enumeration<InstructionListNode> readInstructionListEnumeration = fieldReads.keys();

						while (readInstructionListEnumeration.hasMoreElements()) {
							InstructionListNode readInstructionList = readInstructionListEnumeration.nextElement();
							if (readInstructionList == this.instructionListNode) {
								List<Integer> readIndexes = fieldReads.get(readInstructionList);

								for (int readIndexPosition = 0; readIndexPosition < readIndexes.size(); readIndexPosition++) {
									int readInstructionIndex = readIndexes.get(readIndexPosition);
									FrameNode readFrame = this.frames[readInstructionIndex];
									if (readFrame != null) {
										this.traceVariableTrackerSources(analysisContext, variableTracker, this.getNodeForInstructionIndex(readInstructionIndex), readInstructionIndex + 1, readFrame.getOperandStack().length - 1);
									}
								}
							} else {
								try {
									MethodAnalyzer methodAnalyzer = readInstructionList.getOrCreateMethodAnalyzerFromCache(analysisContext.methodAnalyzerCache, analysisContext.typeResolver, this.classHierarchy);
									List<Integer> remoteReadIndexes = fieldReads.get(readInstructionList);

									for (int remoteIndexPosition = 0; remoteIndexPosition < remoteReadIndexes.size(); remoteIndexPosition++) {
										int remoteInstructionIndex = remoteReadIndexes.get(remoteIndexPosition);
										FrameNode remoteFrame = methodAnalyzer.frames[remoteInstructionIndex];
										if (remoteFrame != null) {
											VariableTracker remoteTracker = variableTracker.createTracker(methodAnalyzer);
											methodAnalyzer.traceVariableTrackerSources(analysisContext, remoteTracker, methodAnalyzer.getNodeForInstructionIndex(remoteInstructionIndex), remoteInstructionIndex + 1, remoteFrame.getOperandStack().length - 1);
										}
									}
								} catch (ZKMException trackingException) {
									analysisContext.currentReferences.add(new ErrorReference(null, "Error while tracing field use : " + trackingException.getMessage()));
								}
							}
						}
					}
				} else {
					analysisContext.currentReferences.add(new ErrorReference(null, "Field set more than once"));
				}
			}
		} else {
			analysisContext.currentReferences.add(new ErrorReference(null, "Field is external : " + fieldWriter.getValueAsString()));
		}
	}

	public List<ControlFlowNode> buildControlFlowNodes() {
		this.assignNodeIndexes();
		this.resetSwitchAnalyzingNodes();
		int initialCapacity = GlobalUtils.getInitialCapacity(this.nodes.size());
		TreeMap<AnalyzingNode, ControlFlowNode> controlFlowNodeByAnalyzingNode = new TreeMap<>();
		HashSet<AnalyzingNode> seenNodes = GlobalUtils.newHashSet(initialCapacity);
		HashMap<AnalyzingNode, FlagData> flagDataByAnalyzingNode = GlobalUtils.newHashMap(initialCapacity);
		BitSet headerUsageMask = new BitSet(this.nodes.size());
		headerUsageMask.set(this.headerNode.getNodeIndex());
		seenNodes.add(this.headerNode);
		FlagData headerFlagData = new FlagData(this.headerNode, headerUsageMask);
		flagDataByAnalyzingNode.put(this.headerNode, headerFlagData);
		InsnList<AbstractInsnNode> insnListCopy = new InsnList<>(this.insnList);
		AnalyzingNode.collectControlFlowNodes(headerFlagData, controlFlowNodeByAnalyzingNode, seenNodes, flagDataByAnalyzingNode, insnListCopy);

		for (int i = 0; i < this.catchEntryNodes.size(); i++) {
			AnalyzingNode catchEntryNode = this.catchEntryNodes.get(i);
			if (!seenNodes.contains(catchEntryNode)) {
				BitSet catchEntryUsageMask = new BitSet(this.nodes.size());
				seenNodes.add(catchEntryNode);
				FlagData catchEntryFlagData = new FlagData(catchEntryNode, catchEntryUsageMask);
				flagDataByAnalyzingNode.put(catchEntryNode, catchEntryFlagData);
				catchEntryUsageMask.set(catchEntryNode.getNodeIndex());
				insnListCopy = new InsnList<>(this.insnList);
				AnalyzingNode.collectControlFlowNodes(catchEntryFlagData, controlFlowNodeByAnalyzingNode, seenNodes, flagDataByAnalyzingNode, insnListCopy);
			}
		}

		ArrayList<ControlFlowNode> controlFlowNodeList = new ArrayList<>();
		Iterator<ControlFlowNode> nodeIterator = controlFlowNodeByAnalyzingNode.values().iterator();

		while (nodeIterator.hasNext()) {
			controlFlowNodeList.add(nodeIterator.next());
		}

		nodeIterator = controlFlowNodeList.iterator();

		while (nodeIterator.hasNext()) {
			nodeIterator.next().computeReachableNodeSet(new InsnList<>(this.nodes), this.reachableNodesByCatchNode);
		}

		return controlFlowNodeList;
	}

	public Integer[] getStackMapLabelIndexes() {
		HashMap<Integer, Integer> instructionIndexByLabelOffset = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.nodes.size()), 3.0F);

		for (int i = 0; i < this.insnList.size(); i++) {
			if (this.insnList.get(i).isLabel()) {
				LabelNode labelNode = (LabelNode)this.insnList.get(i);
				if (labelNode.hasFlag(1) || labelNode.hasFlag(1024)) {
					instructionIndexByLabelOffset.put(INTEGER_CACHE.valueOf(labelNode.getOffset()), INTEGER_CACHE.valueOf(i));
				}
			}
		}

		Collection<Integer> labelIndexCollection = instructionIndexByLabelOffset.values();
		Integer[] sortedLabelIndexes = labelIndexCollection.toArray(new Integer[labelIndexCollection.size()]);
		Arrays.sort(sortedLabelIndexes);
		return sortedLabelIndexes;
	}

	public boolean hasActiveMonitorsAtInsn(int instructionIndex) {
		if (this.frames != null && this.frames[instructionIndex] != null) {
			Set<MonitorEnterNode> activeMonitors = this.frames[instructionIndex].getActiveMonitors();
			return activeMonitors != null && activeMonitors.size() > 0;
		} else {
			return false;
		}
	}

	private void analyzeStackFrames(TypeResolver typeResolver, ExceptionAnalyzingInfo[][] handlersByInstruction, boolean checkTypes, boolean unused, String methodSignature) {
		int instructionCount = this.insnList.size();
		this.frames = new FrameNode[instructionCount];
		FrameNode initialFrame = new FrameNode(this.initialLocalTypes);
		AbstractInsnNode firstInsn = this.insnList.get(0);
		this.frames[0] = firstInsn.analyzeStack(initialFrame, checkTypes, unused, typeResolver, methodSignature);
		TreeSet<Integer> pendingIndexes = new TreeSet<>();
		TreeSet<Integer> pendingJsrTargetIndexes = new TreeSet<>();
		RingQueue<Integer> retryQueue = new RingQueue<>();
		pendingIndexes.add(INTEGER_CACHE.valueOf(0));
		DualKeyMap<VarInsnNode, Integer, Integer> mergedRetContinuations = new DualKeyMap<>();
		HashSet<Integer> seenQueueHashes = null;
		HashSet<AnalyzingNode> allAnalyzingNodes = null;

		do {
			Integer currentIndex;
			int instructionIndex;
			if (!pendingIndexes.isEmpty()) {
				currentIndex = pendingIndexes.first();
				instructionIndex = currentIndex;
				pendingIndexes.remove(currentIndex);
			} else if (!pendingJsrTargetIndexes.isEmpty()) {
				currentIndex = pendingJsrTargetIndexes.first();
				instructionIndex = currentIndex;
				pendingJsrTargetIndexes.remove(currentIndex);
			} else {
				currentIndex = retryQueue.pop();
				instructionIndex = currentIndex;
			}

			AbstractInsnNode insn = this.insnList.get(instructionIndex);
			if (insn instanceof AbstractSwitchNode) {
				Iterator<LabelNode> switchTargetIterator = ((AbstractSwitchNode)insn).getTargetLabels().iterator();

				while (switchTargetIterator.hasNext()) {
					int switchTargetIndex = switchTargetIterator.next().getLabelID();
					this.mergeFrameAtInsn(instructionIndex, switchTargetIndex, pendingIndexes, checkTypes, unused, methodSignature);
				}
			} else if (insn instanceof JumpInsnNode && !insn.isJSR()) {
				int jumpTargetIndex = ((JumpInsnNode)insn).getTargetLabel().getLabelID();
				this.mergeFrameAtInsn(instructionIndex, jumpTargetIndex, pendingIndexes, checkTypes, unused, methodSignature);
			}

			if (insn.canFallThrough()) {
				if (insn.isRet()) {
					mergedRetContinuations.removeGroup(insn);
				}

				this.mergeFrameAtInsn(instructionIndex, instructionIndex + 1, pendingIndexes, checkTypes, unused, methodSignature);
			}

			ExceptionAnalyzingInfo[] applicableHandlers = handlersByInstruction[instructionIndex];
			if (applicableHandlers != null) {
				for (int handlerIndex = 0; handlerIndex < applicableHandlers.length; handlerIndex++) {
					int handlerPc = applicableHandlers[handlerIndex].handler_pc;
					LocalsInitializationState currentLocalsInitState = this.frames[instructionIndex].getLocalsInitState();
					LocalsInitializationState handlerLocalsInitState = null;
					if (this.frames[handlerPc] != null) {
						handlerLocalsInitState = this.frames[handlerPc].getLocalsInitState();
					}

					FrameNode handlerEntryFrame;
					if ((currentLocalsInitState != null || handlerLocalsInitState != null) && (currentLocalsInitState == null || handlerLocalsInitState == null || !currentLocalsInitState.hasSameLabel(handlerLocalsInitState))) {
						if (currentLocalsInitState != null && handlerLocalsInitState != null && !currentLocalsInitState.hasSameLabel(handlerLocalsInitState)) {
							handlerEntryFrame = FrameNode.mergeForExceptionHandler(
								typeResolver, this.frames[instructionIndex], this.frames[handlerPc].getLocalVariables(), handlerLocalsInitState, this.frames[handlerPc].getActiveMonitors()
							);
						} else {
							if (currentLocalsInitState == null || handlerLocalsInitState != null) {
								throw new MethodAnalyzerException("Method '" + this.methodAnalyzing + "' could not be analyzed (A). No action required.", this.methodAnalyzing);
							}

							if (this.frames[handlerPc] != null) {
								handlerEntryFrame = FrameNode.mergeForExceptionHandler(
									typeResolver, this.frames[instructionIndex], this.frames[handlerPc].getLocalVariables(), handlerLocalsInitState, this.frames[handlerPc].getActiveMonitors()
								);
							} else {
								handlerEntryFrame = this.frames[instructionIndex];
							}
						}
					} else {
						handlerEntryFrame = this.frames[instructionIndex];
					}

					FrameNode handlerFrame = new FrameNode(applicableHandlers[handlerIndex].desc, handlerEntryFrame.getLocalVariables(), handlerEntryFrame.getLocalsInitState(), this.frames[instructionIndex].getActiveMonitors());
					AbstractInsnNode handlerInsn = this.insnList.get(handlerPc);
					FrameNode analyzedHandlerFrame = handlerInsn.analyzeStack(handlerFrame, checkTypes, unused, typeResolver, methodSignature);
					if (this.frames[handlerPc] == null) {
						this.frames[handlerPc] = analyzedHandlerFrame;
						pendingIndexes.add(INTEGER_CACHE.valueOf(handlerPc));
					} else {
						AtomicBool handlerFrameChanged = new AtomicBool();
						FrameNode mergedHandlerFrame = this.frames[handlerPc].mergeWith(typeResolver, analyzedHandlerFrame, handlerFrameChanged, methodSignature);
						if (handlerFrameChanged.get()) {
							this.frames[handlerPc] = mergedHandlerFrame;
							pendingIndexes.add(INTEGER_CACHE.valueOf(handlerPc));
						}
					}
				}
			}

			if (insn.isJSR()) {
				LabelNode jsrTargetLabel = ((JumpInsnNode)insn).getTargetLabel();
				int jsrTargetIndex = jsrTargetLabel.getLabelID();
				FrameNode jsrTargetFrame = new FrameNode(
					this.frames[instructionIndex].getOperandStack(),
					this.frames[instructionIndex].getLocalVariables(),
					new LocalsInitializationState(jsrTargetLabel, this.maxLocals),
					this.frames[instructionIndex].getActiveMonitors()
				);
				FrameNode analyzedJsrTargetFrame = jsrTargetLabel.analyzeStack(jsrTargetFrame, checkTypes, unused, typeResolver, methodSignature);
				if (this.frames[jsrTargetIndex] == null) {
					this.frames[jsrTargetIndex] = analyzedJsrTargetFrame;
					pendingJsrTargetIndexes.add(INTEGER_CACHE.valueOf(jsrTargetIndex));
				} else {
					FrameNode existingJsrTargetFrame = this.frames[jsrTargetIndex];
					AtomicBool jsrTargetFrameChanged = new AtomicBool();
					FrameNode mergedJsrTargetFrame = existingJsrTargetFrame.mergeWith(typeResolver, jsrTargetFrame, jsrTargetFrameChanged, methodSignature);
					if (jsrTargetFrameChanged.get()) {
						this.frames[jsrTargetIndex] = mergedJsrTargetFrame;
						pendingJsrTargetIndexes.add(INTEGER_CACHE.valueOf(jsrTargetIndex));
					}
				}
			} else if (insn.isRet()) {
				VarInsnNode retInsn = (VarInsnNode)insn;
				DebugUtils.assertTrue(this.jsrContinuationsByRetNode != null, new String[]{this.methodAnalyzing + " " + instructionIndex});
				Iterator<Integer> continuationIterator = this.jsrContinuationsByRetNode.getGroup(retInsn).keySet().iterator();
				boolean retryQueued = false;

				while (continuationIterator.hasNext()) {
					Integer continuationKey = continuationIterator.next();
					int continuationIndex = continuationKey;
					int preContinuationIndex = continuationIndex - 1;
					if (this.frames[preContinuationIndex] == null) {
						AnalyzingNode preContinuationNode = this.getNodeForInstructionIndex(preContinuationIndex);
						if (allAnalyzingNodes == null) {
							allAnalyzingNodes = GlobalUtils.newHashSet(this.nodes);
						}

						if (allAnalyzingNodes.contains(preContinuationNode)) {
							retryQueue.push(currentIndex);
							retryQueued = true;
						}
					} else if (!mergedRetContinuations.contains(retInsn, continuationKey)) {
						FrameNode continuationEntryFrame = FrameNode.mergeForExceptionHandler(
							typeResolver, this.frames[instructionIndex], this.frames[preContinuationIndex].getLocalVariables(), this.frames[preContinuationIndex].getLocalsInitState(), this.frames[preContinuationIndex].getActiveMonitors()
						);
						AbstractInsnNode continuationInsn = this.insnList.get(continuationIndex);
						FrameNode analyzedContinuationFrame = continuationInsn.analyzeStack(continuationEntryFrame, checkTypes, unused, typeResolver, methodSignature);
						AtomicBool continuationFrameChanged = new AtomicBool();
						if (this.frames[continuationIndex] == null) {
							this.frames[continuationIndex] = analyzedContinuationFrame;
							pendingIndexes.add(continuationKey);
						} else {
							FrameNode mergedContinuationFrame = this.frames[continuationIndex].mergeWith(typeResolver, analyzedContinuationFrame, continuationFrameChanged, methodSignature);
							if (continuationFrameChanged.get()) {
								this.frames[continuationIndex] = mergedContinuationFrame;
								pendingIndexes.add(continuationKey);
							}
						}

						mergedRetContinuations.put(retInsn, continuationKey, continuationKey);
					}
				}

				if (retryQueued) {
					if (seenQueueHashes == null) {
						seenQueueHashes = GlobalUtils.newHashSet();
					}

					if (pendingIndexes.size() == 0) {
						Integer queueStateHash = retryQueue.hashCode();
						if (seenQueueHashes.contains(queueStateHash)) {
							throw new MethodAnalyzerException(
								"Method '" + this.methodAnalyzing + "' could not be analyzed (B). Please report this message to bugs" + CopyrightConstants.EMAIL + ".", this.methodAnalyzing
							);
						}

						seenQueueHashes.add(queueStateHash);
					} else {
						seenQueueHashes.clear();
					}
				}
			}
		} while (!pendingIndexes.isEmpty() || !pendingJsrTargetIndexes.isEmpty() || !retryQueue.isEmpty());
	}

	private MethodAnalysisResult analyzeMethodReturnSource(AnalysisContext analysisContext) {
		MethodAnalysisResult analysisResult = null;
		boolean methodStatic = this.instructionListNode.getMethodNode().isStatic();
		int areturnIndex = -1;
		int instructionCount = this.insnList.size();

		for (int i = 0; i < instructionCount; i++) {
			if (this.insnList.get(i).getOpcode() == ARETURN) {
				if (areturnIndex == -1) {
					areturnIndex = i;
				} else {
					analysisResult = new MethodAnalysisResult("Multiple ARETURNs", this.instructionListNode);
				}
			}
		}

		if (analysisResult == null) {
			if (areturnIndex > -1) {
				AnalyzingNode areturnNode = this.getNodeForInstructionIndex(areturnIndex);
				int areturnStackSize = this.frames[areturnIndex].getStackSize();
				HashSet<Reference> sourceReferences = GlobalUtils.newHashSet(13);
				AnalysisContext returnContext = new AnalysisContext(
					this,
					sourceReferences,
					false,
					AnalysisType.USER_DEFINED_METHOD_1,
					analysisContext.fieldReadsByField,
					analysisContext.fieldWritesByField,
					analysisContext.methodCallsByMethod,
					analysisContext.typeResolver,
					analysisContext.workspace,
					analysisContext.reflectionPatterns,
					methodStatic,
					analysisContext.methodAnalysisResults,
					analysisContext.fieldReferencesCache,
					analysisContext.stackSourceCache,
					analysisContext.localVarSourceCache,
					analysisContext.getNestingLevel() + 1,
					analysisContext.methodAnalyzerCache
				);
				this.traceStackSources(returnContext, areturnNode, areturnIndex, areturnStackSize);
				if (sourceReferences.size() == 0) {
					analysisResult = new MethodAnalysisResult("No source found", this.instructionListNode);
				} else if (sourceReferences.size() == 1) {
					Reference sourceReference = sourceReferences.iterator().next();
					if (sourceReference instanceof MethodParameterReference) {
						MethodParameterReference parameterReference = (MethodParameterReference)sourceReference;
						if (this.instructionListNode == parameterReference.getInstructionList()) {
							analysisResult = new MethodAnalysisResult(parameterReference.getSlotIndex(), this.instructionListNode);
						} else {
							analysisResult = new MethodAnalysisResult(
								"NESTING TOO DEEP. " + this.instructionListNode.getParentString() + " calls " + parameterReference.getInstructionList().getParentString(), this.instructionListNode
							);
						}
					} else if (sourceReference instanceof StringReference) {
						StringReference stringReference = (StringReference)sourceReference;
						analysisResult = new MethodAnalysisResult(stringReference.getStringNode(), this.instructionListNode);
					} else if (sourceReference instanceof ArrayReference) {
						analysisResult = new MethodAnalysisResult((ArrayReference)sourceReference, this.instructionListNode);
					} else if (sourceReference instanceof ErrorReference) {
						ErrorReference errorReference = (ErrorReference)sourceReference;
						analysisResult = new MethodAnalysisResult(errorReference.getErrorMessage(), this.instructionListNode);
					} else if (sourceReference instanceof PendingReference) {
						PendingReference pendingReference = (PendingReference)sourceReference;
						analysisResult = new MethodAnalysisResult("Pending '" + pendingReference.getParentString() + "'", this.instructionListNode);
					} else {
						analysisResult = new MethodAnalysisResult("UNEXPECTED NAME SOURCE", this.instructionListNode);
					}
				} else {
					ArrayReference arrayReference = new ArrayReference(sourceReferences.size());
					int referenceIndex = 0;

					for (Reference reference : sourceReferences) {
						arrayReference.setElementReference(referenceIndex++, reference);
					}

					analysisResult = new MethodAnalysisResult(arrayReference, this.instructionListNode);
				}
			} else {
				analysisResult = new MethodAnalysisResult("No ARETURN", this.instructionListNode);
			}
		} else {
			analysisResult = new MethodAnalysisResult("Multiple ARETURNs", this.instructionListNode);
		}

		return analysisResult;
	}

	private boolean isGetPropertyCall(AbstractMethodCallWriter methodCall, String signatureKey, AbstractWorkspace workspace) {
		try {
			if (!methodCall.getSignatureKey().equals(signatureKey)) {
				return false;
			} else {
				String ownerName = methodCall.getOwnerName();
				return ownerName.equals("java/util/Properties") || workspace.isClassInstanceOf(ownerName, "java/util/Properties");
			}
		} catch (FileException ignored) {
			return false;
		}
	}

	private void buildExceptionHandlerIndex(ExceptionAnalyzingInfo[][] handlersByInstruction, ExceptionAnalyzingInfo[] exceptionHandlers) {
		ArrayList<ExceptionAnalyzingInfo> handlerBuffer = new ArrayList<>();

		for (AnalyzingNode analyzingNode : this.nodes) {
			int startIndex = analyzingNode.getStartIndex();
			int endIndex = analyzingNode.getEndIndex();

			for (int instructionIndex = startIndex; instructionIndex <= endIndex; instructionIndex++) {
				AbstractInsnNode insn = this.insnList.get(instructionIndex);
				ExceptionAnalyzingInfo[] applicableHandlers = this.getApplicableHandlersAtInsn(instructionIndex, exceptionHandlers);

				for (int handlerIndex = 0; handlerIndex < applicableHandlers.length; handlerIndex++) {
					if (this.reachableNodesByCatchNode.put(applicableHandlers[handlerIndex].catchNode, analyzingNode, analyzingNode) == null) {
						analyzingNode.addCatchNode(applicableHandlers[handlerIndex].catchNode);
					}

					if (instructionIndex == applicableHandlers[handlerIndex].start_pc || instructionIndex == startIndex || insn.isVarStore() || !handlerBuffer.contains(applicableHandlers[handlerIndex])) {
						handlerBuffer.add(applicableHandlers[handlerIndex]);
					}
				}

				if (handlerBuffer.size() > 0) {
					ExceptionAnalyzingInfo[] handlerArray = new ExceptionAnalyzingInfo[handlerBuffer.size()];
					handlersByInstruction[instructionIndex] = handlerBuffer.toArray(handlerArray);
					handlerBuffer.clear();
				}
			}
		}
	}

	public PropertyFileNameTracker createArrayElementTracker() {
		return new PropertyFileNameTracker(this);
	}

	public Set<StackFrameType> collectMonitorStackTypes() {
		IdentityMap<StackFrameType, StackFrameType> monitorTypes = new IdentityMap<>();

		for (AbstractInsnNode insn : this.insnList) {
			if (insn.isMonitor()) {
				StackFrameType monitorType = ((AbstractMonitorNode)insn).getMonitorType();
				monitorTypes.put(monitorType, monitorType);
			}
		}

		return monitorTypes.keySet();
	}

	private void buildControlFlowTree(
		ControlFlowNode controlFlowNode,
		HashSet<ControlFlowNode> closureNodes,
		TreeNavigator<ControlFlowNode> navigator,
		ReferenceGraph<ControlFlowNode, ControlFlowNode> controlFlowGraph,
		ReferenceGraph<ControlFlowNode, ControlFlowNode> closureGraph,
		Set<ControlFlowNode> visitedNodes,
		boolean filterByClosure
	) {
		closureNodes.add(controlFlowNode);
		Set<ControlFlowNode> referencedNodes;
		if (filterByClosure) {
			referencedNodes = this.filterReferencesByClosure(controlFlowNode, closureNodes, controlFlowGraph, closureGraph);
		} else {
			referencedNodes = controlFlowGraph.getReferences(controlFlowNode);
		}

		if (referencedNodes != null) {
			for (ControlFlowNode referencedNode : referencedNodes) {
				if (!visitedNodes.contains(referencedNode)) {
					navigator.navigateTo(controlFlowNode);
					navigator.addChildToCurrent(referencedNode);
					visitedNodes.add(referencedNode);
					this.buildControlFlowTree(referencedNode, GlobalUtils.newHashSet(closureNodes), navigator, controlFlowGraph, closureGraph, visitedNodes, filterByClosure);
				}
			}
		}
	}

	public List<AnalyzingNode> getAnalyzingNodes() {
		return new ArrayList<>(this.nodes);
	}

	public AnalyzingNode getNodeForInstructionIndex(int instructionIndex) {
		if (this.nodeByInstructionIndexCache == null) {
			this.nodeByInstructionIndexCache = new AnalyzingNode[this.instructionListNode.getInstructionCount()];
		} else if (this.nodeByInstructionIndexCache[instructionIndex] != null) {
			return this.nodeByInstructionIndexCache[instructionIndex];
		}

		int lowIndex = 0;
		int highIndex = this.nodes.size() - 1;

		while (lowIndex <= highIndex) {
			int midIndex = (lowIndex + highIndex) / 2;
			AnalyzingNode candidateNode = this.nodes.get(midIndex);
			if (instructionIndex < candidateNode.getStartIndex()) {
				highIndex = midIndex - 1;
			} else {
				if (instructionIndex <= candidateNode.getEndIndex()) {
					this.nodeByInstructionIndexCache[instructionIndex] = candidateNode;
					return candidateNode;
				}

				lowIndex = midIndex + 1;
			}
		}

		return null;
	}

	public int getAnalyzingNodeCount() {
		return this.nodes.size();
	}

	public void resetSwitchAnalyzingNodes() {
		InsnList<AbstractInsnNode> insnListCopy = new InsnList<>(this.insnList);

		for (int i = 0; i < this.nodes.size(); i++) {
			AnalyzingNode analyzingNode = this.nodes.get(i);
			if (analyzingNode.getLastInsn(insnListCopy) instanceof AbstractSwitchNode) {
				analyzingNode.reorderSwitchSuccessors();
			}
		}
	}

	private HashSet<ControlFlowNode> filterReferencesByClosure(
		ControlFlowNode controlFlowNode, Set<ControlFlowNode> closureNodes, ReferenceGraph<ControlFlowNode, ControlFlowNode> controlFlowGraph, ReferenceGraph<ControlFlowNode, ControlFlowNode> closureGraph
	) {
		HashSet<ControlFlowNode> filteredNodes = GlobalUtils.newHashSet();
		Set<ControlFlowNode> referencedNodes = controlFlowGraph.getReferences(controlFlowNode);
		if (referencedNodes != null) {
			for (ControlFlowNode referencedNode : referencedNodes) {
				Set<ControlFlowNode> closureReferences = closureGraph.getReferences(referencedNode);
				if (closureNodes.containsAll(closureReferences)) {
					filteredNodes.add(referencedNode);
				}
			}
		}

		return filteredNodes;
	}

	private boolean markUninitializedLocalUsage(AnalyzingNode analyzingNode, int startIndex, BitSet usageMask) {
		boolean usageMarked = false;
		UninitializedUsageWalkItem walkItem = new UninitializedUsageWalkItem(analyzingNode, startIndex, usageMask);
		RingQueue<UninitializedUsageWalkItem> workQueue = null;
		boolean firstIteration = true;

		label71:
		while (firstIteration || workQueue != null && !workQueue.isEmpty()) {
			if (!firstIteration) {
				walkItem = workQueue.pop();
			}

			firstIteration = false;
			AnalyzingNode currentNode = walkItem.node;
			int fromIndex = walkItem.fromIndex;
			BitSet currentUsageMask = walkItem.usageMask;

			for (int instructionIndex = fromIndex; instructionIndex >= currentNode.getStartIndex(); instructionIndex--) {
				FrameNode frame = this.frames[instructionIndex];
				if (frame != null) {
					AbstractInsnNode insn = this.insnList.get(instructionIndex);

					for (int localSlotIndex = 0; localSlotIndex < this.maxLocals; localSlotIndex++) {
						if (frame.isLocalInitialized(localSlotIndex)) {
							if (insn.isThatVar(localSlotIndex)) {
								currentUsageMask.set(localSlotIndex);
							}
						} else {
							currentUsageMask.clear(localSlotIndex);
						}
					}

					if (frame.hasUninitializedUsageMask()) {
						frame.getUninitializedUsageMask();
					}

					if (!frame.markUninitializedUsage(currentUsageMask)) {
						continue label71;
					}

					usageMarked = true;
				}
			}

			if (currentNode.hasPredecessors()) {
				if (workQueue == null) {
					workQueue = new RingQueue<>(Math.max(5, this.nodes.size()));
				}

				for (AnalyzingNode predecessorNode : currentNode.getPredecessorsView()) {
					workQueue.push(new UninitializedUsageWalkItem(predecessorNode, predecessorNode.getEndIndex(), (BitSet)currentUsageMask.clone()));
				}
			}
		}

		return usageMarked;
	}

	private void analyzeReferencingMethods(AnalysisContext analysisContext, int paramSlotIndex) {
		MethodNode methodNode = (MethodNode)this.instructionListNode.getMethodNode();
		int invokeStackShift = methodNode.getInvokeStackShift();
		MethodID methodId = methodNode.toMethodId();
		if (analysisContext.enterReferencedMethod(methodId)) {
			MultiMap<InstructionListNode, Integer> callIndexesByCaller = analysisContext.methodCallsByMethod.get(methodId);
			if (callIndexesByCaller != null) {
				Enumeration<InstructionListNode> callers = callIndexesByCaller.keys();

				while (callers.hasMoreElements()) {
					InstructionListNode callerNode = callers.nextElement();

					try {
						MethodAnalyzer callerAnalyzer = callerNode.getOrCreateMethodAnalyzerFromCache(analysisContext.methodAnalyzerCache, analysisContext.typeResolver, this.classHierarchy);
						List<Integer> callIndexes = callIndexesByCaller.get(callerNode);

						for (int i = 0; i < callIndexes.size(); i++) {
							int callIndex = callIndexes.get(i);
							FrameNode callerFrame = callerAnalyzer.frames[callIndex];
							if (callerFrame != null) {
								int callerStackSize = callerFrame.getStackSize();
								int parameterStackIndex = callerStackSize + invokeStackShift + methodNode.getParamIndexBySlot(paramSlotIndex);
								AnalysisContext callContext = new AnalysisContext(this, analysisContext, callerNode.getMethodNode().isStatic());
								AnalyzingNode callAnalyzingNode = callerAnalyzer.getNodeForInstructionIndex(callIndex);
								callerAnalyzer.traceStackSources(callContext, callAnalyzingNode, callIndex, parameterStackIndex);
							}
						}
					} catch (ZKMException ignored) {
						analysisContext.currentReferences.add(new ErrorReference(null, "Exception '" + ignored.getMessage() + "' : " + callerNode.getInternalName()));
					}
				}
			}

			analysisContext.leaveReferencedMethod(methodId);
		}
	}

	public boolean hasControlFlowNodes() {
		return this.controlFlowNodes != null && this.controlFlowNodes.size() > 0;
	}

	public KeyValuePair<Integer, Integer> getLocalSlotBytecodeRange(int localSlotIndex, AtomicReference<String> typeDescriptorRef) {
		int startOffset = -1;
		int endOffset = -1;
		String typeDescriptor = null;
		int bytecodeOffset = 0;

		for (int instructionIndex = 0; instructionIndex < this.frames.length; instructionIndex++) {
			FrameNode frame = this.frames[instructionIndex];
			if (frame != null && frame.isLocalTypeValid(localSlotIndex)) {
				if (startOffset == -1) {
					startOffset = bytecodeOffset;
				}

				String localDescriptor = frame.getLocalTypeDescriptor(localSlotIndex);
				if (typeDescriptor == null && localDescriptor != null) {
					typeDescriptor = localDescriptor;
					typeDescriptorRef.set(localDescriptor);
				} else if (localDescriptor != null && !localDescriptor.equals(typeDescriptor)) {
					break;
				}

				endOffset = bytecodeOffset;
			}

			bytecodeOffset += this.insnList.get(instructionIndex).getBytecodeSize();
		}

		return new KeyValuePair<>(startOffset, endOffset);
	}

	public int computeMinReturnStackDepth() {
		int minDepth = Integer.MAX_VALUE;

		for (int i = 0; i < this.nodes.size(); i++) {
			AnalyzingNode analyzingNode = this.nodes.get(i);
			if (this.insnList.get(analyzingNode.getEndIndex()).isReturn()) {
				int pathInstructionCount = analyzingNode.getMinPathInstructionCount();
				if (pathInstructionCount < minDepth) {
					minDepth = pathInstructionCount;
				}
			}
		}

		return minDepth;
	}

	public void traceLocalVariableSource(AnalysisContext analysisContext, AnalyzingNode analyzingNode, int instructionIndex, int localSlotIndex) {
		ExcludeElement excludeElement = new ExcludeElement(this.instructionListNode, INTEGER_CACHE.valueOf(instructionIndex), INTEGER_CACHE.valueOf(localSlotIndex));
		analysisContext.traceCountsByElement.add(excludeElement);
		if (analysisContext.traceCountsByElement.getCount(excludeElement) > 9) {
			analysisContext.currentReferences.add(new ErrorReference(null, "Possible loop"));
		} else {
			HashSet<Reference> sourceReferences = analysisContext.localVarSourceCache.get(excludeElement);
			int nodeStartIndex = analyzingNode.getStartIndex();
			if (sourceReferences != null && instructionIndex - 1 >= nodeStartIndex && analysisContext.analysisType != AnalysisType.USER_DEFINED_METHOD_1 && analysisContext.analysisType != AnalysisType.USER_DEFINED_METHOD_2) {
				analysisContext.currentReferences.addAll(sourceReferences);
			} else {
				boolean sourceFound = false;
				sourceReferences = GlobalUtils.newHashSet(13);
				HashSet<Reference> outerReferences = analysisContext.swapCurrentReferences(sourceReferences);

				for (int scanIndex = instructionIndex - 1; scanIndex >= nodeStartIndex; scanIndex--) {
					AbstractInsnNode instruction = this.insnList.get(scanIndex);
					FrameNode frame = this.frames[scanIndex];
					if (instruction.isThatVarStore(localSlotIndex)) {
						if (scanIndex > nodeStartIndex) {
							this.traceStackSources(analysisContext, analyzingNode, scanIndex, frame.getStackSize());
							sourceFound = true;
							break;
						}

						List<AnalyzingNode> predecessors = analyzingNode.getPredecessors();
						if (predecessors != null && predecessors.size() > 0) {
							for (int predecessorIndex = 0; predecessorIndex < predecessors.size(); predecessorIndex++) {
								AnalyzingNode predecessor = predecessors.get(predecessorIndex);
								this.traceStackSources(analysisContext, predecessor, predecessor.getEndIndex() + 1, frame.getStackSize());
							}

							sourceFound = true;
							break;
						}
					}
				}

				if (!sourceFound) {
					List<AnalyzingNode> fallbackPredecessors = analyzingNode.getPredecessors();
					if (fallbackPredecessors != null && fallbackPredecessors.size() > 0) {
						for (int fallbackPredecessorIndex = 0; fallbackPredecessorIndex < fallbackPredecessors.size(); fallbackPredecessorIndex++) {
							AnalyzingNode fallbackPredecessor = fallbackPredecessors.get(fallbackPredecessorIndex);
							this.traceLocalVariableSource(analysisContext, fallbackPredecessor, fallbackPredecessor.getEndIndex() + 1, localSlotIndex);
						}
					} else if (analyzingNode.isCatchNode()) {
						this.traceCatchVariableSource(analysisContext, analyzingNode, localSlotIndex);
					} else if (analyzingNode.isHeader()) {
						int sourceSlotIndex;
						if (analysisContext.isStaticMethod) {
							sourceSlotIndex = localSlotIndex;
						} else {
							sourceSlotIndex = localSlotIndex - 1;
						}

						if (sourceSlotIndex < 0) {
							analysisContext.currentReferences.add(new ErrorReference(this.insnList.get(nodeStartIndex), "Source object reference : " + this.methodAnalyzing));
						} else if (sourceSlotIndex < this.localSlotCount) {
							switch (analysisContext.analysisType) {
								case NORMAL:
								case CLASS_REFERENCE:
									if (!((MethodNode)this.instructionListNode.getMethodNode()).isPrivate()) {
										analysisContext.setPrivateCallChain(false);
									}

									this.analyzeReferencingMethods(analysisContext, sourceSlotIndex);
									break;
								case METHODTYPE_REFERENCE:
								case USER_DEFINED_METHOD_1:
									analysisContext.currentReferences.add(new ErrorReference(this.insnList.get(nodeStartIndex), "Source is method parameter: " + analysisContext.analysisType));
									break;
								case USER_DEFINED_METHOD_2:
									analysisContext.currentReferences.add(new MethodParameterReference(sourceSlotIndex, this.instructionListNode));
							}
						} else {
							analysisContext.currentReferences
								.add(new ErrorReference(this.insnList.get(nodeStartIndex), "Variable Uninitialized : " + sourceSlotIndex + " " + this.localSlotCount + " " + analysisContext.analysisType + " " + this.methodAnalyzing));
						}
					} else {
						AbstractInsnNode nodeStartInstruction = this.insnList.get(nodeStartIndex);
						analysisContext.currentReferences.add(new ErrorReference(nodeStartInstruction, "No source found 2) : " + analysisContext.analysisType));
					}
				}

				sourceReferences = analysisContext.swapCurrentReferences(outerReferences);
				outerReferences.addAll(sourceReferences);
				if (instructionIndex - 1 >= nodeStartIndex && analysisContext.analysisType != AnalysisType.USER_DEFINED_METHOD_1 && analysisContext.analysisType != AnalysisType.USER_DEFINED_METHOD_2) {
					HashSet<Reference> cachedReferences = analysisContext.localVarSourceCache.put(excludeElement, sourceReferences);
					if (cachedReferences != null && cachedReferences.size() > 0) {
						sourceReferences.addAll(cachedReferences);
					}
				}
			}
		}
	}

	public boolean hasStackMapLabels() {
		for (int i = 0; i < this.insnList.size(); i++) {
			if (this.insnList.get(i).isLabel()) {
				LabelNode labelNode = (LabelNode)this.insnList.get(i);
				if (labelNode.hasFlag(1) || labelNode.hasFlag(1024)) {
					return true;
				}
			}
		}

		return false;
	}

	private ArrayList<TreeNavigator<ControlFlowNode>> buildControlFlowNavigators() {
		HashMap<AnalyzingNode, ControlFlowNode> controlFlowNodesByAnchorNode = GlobalUtils.newHashMap();

		for (ControlFlowNode controlFlowNode : this.controlFlowNodes) {
			controlFlowNodesByAnchorNode.put(controlFlowNode.getAnchorNode(), controlFlowNode);
		}

		ReferenceGraph<ControlFlowNode, ControlFlowNode> reachabilityGraph = new ReferenceGraph<>();

		for (ControlFlowNode reachingNode : this.controlFlowNodes) {
			for (ControlFlowNode reachableNode : this.controlFlowNodes) {
				if (reachingNode != reachableNode && reachingNode.containsReachableNode(reachableNode.getAnchorNode())) {
					reachabilityGraph.addReference(reachingNode, reachableNode);
				}
			}
		}

		ReferenceGraph<ControlFlowNode, ControlFlowNode> inverseReachabilityGraph = new ReferenceGraph<>();

		for (Entry<ControlFlowNode, Set<ControlFlowNode>> adjacencyEntry : reachabilityGraph.getAdjacencyEntries()) {
			ControlFlowNode entryKeyNode = adjacencyEntry.getKey();

			for (ControlFlowNode entryValueNode : adjacencyEntry.getValue()) {
				inverseReachabilityGraph.addReference(entryValueNode, entryKeyNode);
			}
		}

		ArrayList<ControlFlowNode> rootNodes = new ArrayList<>();

		for (ControlFlowNode candidateRootNode : this.controlFlowNodes) {
			if (!inverseReachabilityGraph.hasMember(candidateRootNode)) {
				rootNodes.add(candidateRootNode);
			}
		}

		ArrayList<TreeNavigator<ControlFlowNode>> navigators = new ArrayList<>();
		HashSet<ControlFlowNode> coveredNodes = GlobalUtils.newHashSet();

		for (ControlFlowNode rootNode : rootNodes) {
			TreeNavigator<ControlFlowNode> rootNavigator = new TreeNavigator<>(rootNode);
			coveredNodes.add(rootNode);
			navigators.add(rootNavigator);
			this.buildControlFlowTree(rootNode, GlobalUtils.newHashSet(), rootNavigator, reachabilityGraph, inverseReachabilityGraph, coveredNodes, true);
		}

		if (coveredNodes.size() < this.controlFlowNodes.size()) {
			for (ControlFlowNode memberNode : reachabilityGraph.getMembers()) {
				if (!coveredNodes.contains(memberNode)) {
					TreeNavigator<ControlFlowNode> memberNavigator = new TreeNavigator<>(memberNode);
					coveredNodes.add(memberNode);
					navigators.add(memberNavigator);

					for (ControlFlowNode childNode : reachabilityGraph.getReferences(memberNode)) {
						memberNavigator.navigateToRoot();
						memberNavigator.addChildToCurrent(childNode);
						coveredNodes.add(memberNode);
					}
				}
			}
		}

		if (coveredNodes.size() < this.controlFlowNodes.size()) {
			HashSet<ControlFlowNode> nodesInNavigators = GlobalUtils.newHashSet();

			for (TreeNavigator<ControlFlowNode> navigator : navigators) {
				nodesInNavigators.addAll(new ReadOnlyList<>(navigator.getAllDataAsEnumeration()));
			}

			HashSet<ControlFlowNode> uncoveredNodes = GlobalUtils.newHashSet(this.controlFlowNodes);
			uncoveredNodes.removeAll(nodesInNavigators);
			ControlFlowNode parentNode = null;

			for (ControlFlowNode uncoveredNode : uncoveredNodes) {
				Set<ControlFlowNode> candidateParents = inverseReachabilityGraph.getReferences(uncoveredNode);
				if (candidateParents != null) {
					TreeSet<ControlFlowNode> sortedParents = new TreeSet<>(candidateParents);

					for (ControlFlowNode candidateParent : sortedParents) {
						if (inverseReachabilityGraph.hasMember(candidateParent)) {
							parentNode = candidateParent;
							break;
						}
					}

					if (parentNode == null) {
						parentNode = sortedParents.iterator().next();
					}

					for (TreeNavigator<ControlFlowNode> candidateNavigator : navigators) {
						if (candidateNavigator.navigateTo(parentNode)) {
							candidateNavigator.addChildToCurrent(uncoveredNode);
							break;
						}
					}
				}
			}
		}

		return navigators;
	}

	public ReflectionMethodCallTracker analyzeReflectionMethodCall(
		int instructionIndex,
		AbstractMethodCallWriter methodCall,
		ReflectionMethodPattern reflectionPattern,
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldReadsByField,
		HierarchicalMultiMap<FieldID, InstructionListNode, Integer> fieldWritesByField,
		HierarchicalMultiMap<MethodID, InstructionListNode, Integer> methodCallsByMethod,
		Map<AbstractMethodCallWriter, ReflectionMethodPattern> reflectionPatterns,
		Map<MethodID, MethodAnalysisResult> methodAnalysisResults,
		Map<FieldID, HashSet<Reference>> fieldReferencesCache,
		Map<ExcludeElement, HashSet<Reference>> stackSourceCache,
		Map<ExcludeElement, HashSet<Reference>> localVarSourceCache,
		AbstractWorkspace workspace,
		Map<InstructionListNode, MethodAnalyzer> methodAnalyzerCache
	) {
		ReflectionMethodCallTracker tracker = new ReflectionMethodCallTracker(reflectionPattern);
		WideConstantPoolInsnNode methodCallInstruction = (WideConstantPoolInsnNode)this.insnList.get(instructionIndex);
		AnalyzingNode analyzingNode = this.getNodeForInstructionIndex(instructionIndex);
		FrameNode frame = this.frames[instructionIndex];
		if (frame == null) {
			HashSet<Reference> deadCodeReferences = GlobalUtils.newHashSet(5);
			deadCodeReferences.add(new ErrorReference(methodCallInstruction, "Appears in DEAD code. Can be ignored."));
			tracker.addParameterReferences(deadCodeReferences);
			return tracker;
		} else {
			int stackSize = frame.getStackSize();
			boolean analyzedMethodStatic = this.instructionListNode.getMethodNode().isStatic();
			if (reflectionPattern.isObjectNameType()) {
				HashSet<Reference> objectNameTypeReferences = GlobalUtils.newHashSet(5);
				objectNameTypeReferences.add(new ErrorReference(methodCallInstruction, "Analysis not yet implemented"));
				tracker.addParameterReferences(objectNameTypeReferences);
			} else if (reflectionPattern.isFunctionalInterfaceMethodType()) {
				if (this.instructionListNode.getMethodNode().getNameDisplay().equals("$deserializeLambda$")) {
					HashSet<Reference> methodNameReferences = GlobalUtils.newHashSet(5);
					HashSet<Reference> methodSignatureReferences = GlobalUtils.newHashSet(5);
					HashSet<Reference> interfaceClassReferences = GlobalUtils.newHashSet(5);
					AtomicReference<Reference> methodNameReference = new AtomicReference<>();
					AtomicReference<Reference> methodSignatureReference = new AtomicReference<>();
					AtomicReference<Reference> interfaceClassReference = new AtomicReference<>();
					this.resolveLambdaSources(instructionIndex, methodNameReference, methodSignatureReference, interfaceClassReference);
					methodNameReferences.add(methodNameReference.get());
					methodSignatureReferences.add(methodSignatureReference.get());
					interfaceClassReferences.add(interfaceClassReference.get());
					tracker.addParameterReferences(methodNameReferences);
					tracker.addParameterReferences(methodSignatureReferences);
					tracker.addParameterReferences(interfaceClassReferences);
				} else {
					HashSet<Reference> unexpectedUsageReferences = GlobalUtils.newHashSet(5);
					unexpectedUsageReferences.add(new ErrorReference(methodCallInstruction, "Unexpected usage (A) : " + this.methodAnalyzing));
					tracker.addParameterReferences(unexpectedUsageReferences);
				}
			} else {
				for (ReflectionParameter reflectionParameter : reflectionPattern.getParameters()) {
					int parameterStackIndex = stackSize + reflectionPattern.getMethodTypeStackOffset() + reflectionParameter.getParameterIndex();
					HashSet<Reference> parameterReferences = GlobalUtils.newHashSet(13);
					AnalysisType analysisType;
					switch (reflectionParameter.getLookupType()) {
						case CLASS_PARAM_TYPE:
							analysisType = AnalysisType.CLASS_REFERENCE;
							break;
						case METHOD_TYPE_PARAM_TYPE:
							analysisType = AnalysisType.METHODTYPE_REFERENCE;
							break;
						case OBJECT_PARAM_TYPE:
							analysisType = AnalysisType.OBJECT_TYPE;
							break;
						default:
							analysisType = AnalysisType.NORMAL;
					}

					AnalysisContext parameterContext = new AnalysisContext(
						this, parameterReferences, reflectionParameter.isArray(), analysisType, fieldReadsByField, fieldWritesByField, methodCallsByMethod, this.typeResolver, workspace, reflectionPatterns, analyzedMethodStatic, methodAnalysisResults, fieldReferencesCache, stackSourceCache, localVarSourceCache, methodAnalyzerCache
					);
					this.traceStackSources(parameterContext, analyzingNode, instructionIndex, parameterStackIndex);
					tracker.addParameterReferences(parameterReferences);
					if (!parameterContext.isPrivateCallChain()) {
						tracker.setPrivateCallChain(false);
					}
				}

				if (reflectionPattern.isInvokedOnClassInstance()) {
					int receiverStackIndex = stackSize - 1;
					HashSet<Reference> receiverReferences = GlobalUtils.newHashSet(13);
					AnalysisContext receiverContext = new AnalysisContext(
						this, receiverReferences, false, AnalysisType.CLASS_REFERENCE, fieldReadsByField, fieldWritesByField, methodCallsByMethod, this.typeResolver, workspace, reflectionPatterns, analyzedMethodStatic, methodAnalysisResults, fieldReferencesCache, stackSourceCache, localVarSourceCache, methodAnalyzerCache
					);
					this.traceStackSources(receiverContext, analyzingNode, instructionIndex, receiverStackIndex);
					tracker.addParameterReferences(receiverReferences);
				}
			}

			return tracker;
		}
	}

	public StackMapEntry[] buildStackMapEntries(StackMapAttribute stackMapAttribute, ConstantPoolManager constantPoolManager, Set<ClassInfoWriter> classInfoWriters, List<TypeWriter> utf8Strings, boolean useUninitializedLocals) {
		if (this.notPreverified) {
			throw new MethodAnalyzerException("'" + this.methodAnalyzing + "' not already preverified", this.methodAnalyzing);
		} else {
			Integer[] stackMapLabelIndexes = this.getStackMapLabelIndexes();
			HashMap<Integer, VerificationTypeInfo> primitiveTypesByTag = GlobalUtils.newHashMap();
			HashMap<String, VerificationTypeInfo> objectTypesByDescriptor = GlobalUtils.newHashMap();
			StackMapEntry[] stackMapEntries = new StackMapEntry[stackMapLabelIndexes.length];

			for (int i = 0; i < stackMapLabelIndexes.length; i++) {
				int labelInstructionIndex = stackMapLabelIndexes[i];
				LabelNode labelNode = (LabelNode)this.insnList.get(labelInstructionIndex);
				FrameNode frame = this.frames[labelInstructionIndex];
				stackMapEntries[i] = new StackMapEntry(stackMapAttribute, labelNode, frame, constantPoolManager, classInfoWriters, utf8Strings, useUninitializedLocals, primitiveTypesByTag, objectTypesByDescriptor);
			}

			return stackMapEntries;
		}
	}

	public void release() {
		if (!this.released) {
			for (int i = 0; i < this.nodes.size(); i++) {
				this.nodes.get(i).clearLinks();
			}

			this.insnList = null;
			this.frames = null;
			this.exceptions = null;
			this.headerNode = null;
			this.catchEntryNodes.clear();
			this.catchEntryNodes = null;
			this.nodes.clear();
			this.nodes = null;
			this.exceptionInfoByCatchNode.clear();
			this.exceptionInfoByCatchNode = null;
			this.initialLocalTypes = null;
			if (this.jsrContinuationsByRetNode != null) {
				this.jsrContinuationsByRetNode.clearAll();
			}

			this.nodeByInstructionIndexCache = null;
			this.controlFlowNodes.clear();
			this.controlFlowNodes = null;
			this.released = true;
		}
	}

	public boolean isReleased() {
		return this.released;
	}

	public ArrayDimensionTracker createArrayDimensionTracker() {
		return new ArrayDimensionTracker(this);
	}

	private void traceCatchVariableSource(AnalysisContext analysisContext, AnalyzingNode catchNode, int localSlotIndex) {
		HashSet<AnalyzingNode> nodesToTrace = GlobalUtils.newHashSet();
		List<ExceptionAnalyzingInfo> exceptionInfos = this.exceptionInfoByCatchNode.get(catchNode);
		BitSet visitedBitSet = new BitSet(this.nodes.size());

		for (int exceptionInfoIndex = 0; exceptionInfoIndex < exceptionInfos.size(); exceptionInfoIndex++) {
			ExceptionEntryNode exceptionEntryNode = exceptionInfos.get(exceptionInfoIndex).entryNode;
			LabelNode startPcLabel = exceptionEntryNode.getPCByType(TryCatchBlockPCType.START_PC);
			LabelNode endPcLabel = exceptionEntryNode.getPCByType(TryCatchBlockPCType.END_PC);
			int startPc = startPcLabel.getLabelID();
			int endPc = endPcLabel.getLabelID();
			AnalyzingNode tryStartNode = null;
			HashSet<AnalyzingNode> nodesInTryBlock = GlobalUtils.newHashSet();

			for (AnalyzingNode candidateNode : this.nodes) {
				if (candidateNode.getStartIndex() >= startPc && candidateNode.getStartIndex() < endPc) {
					nodesInTryBlock.add(candidateNode);
				} else if (candidateNode.getStartIndex() < startPc && candidateNode.getEndIndex() >= startPc) {
					tryStartNode = candidateNode;
				}
			}

			for (int pcIndex = startPc; pcIndex < endPc; pcIndex++) {
				if (this.insnList.get(pcIndex).isThatVarStore(localSlotIndex)) {
					AnalyzingNode storeNode = this.getNodeForInstructionIndex(pcIndex);
					if (pcIndex > storeNode.getStartIndex()) {
						this.traceStackSources(analysisContext, storeNode, pcIndex, this.frames[pcIndex].getStackSize());
						return;
					}

					List<AnalyzingNode> predecessors = storeNode.getPredecessors();
					if (predecessors != null && predecessors.size() > 0) {
						for (int predecessorIndex = 0; predecessorIndex < predecessors.size(); predecessorIndex++) {
							AnalyzingNode predecessor = predecessors.get(predecessorIndex);
							this.traceStackSources(analysisContext, predecessor, predecessor.getEndIndex() + 1, this.frames[pcIndex].getStackSize());
						}

						return;
					}
				}
			}

			if (tryStartNode != null) {
				visitedBitSet.clear();
				if (catchNode != tryStartNode && !catchNode.canReach(tryStartNode, visitedBitSet)) {
					this.traceLocalVariableSource(analysisContext, tryStartNode, startPc, localSlotIndex);
				} else {
					analysisContext.currentReferences.add(new ErrorReference(this.insnList.get(catchNode.getStartIndex()), "Loop through catch"));
				}
			}

			for (AnalyzingNode tryBlockNode : nodesInTryBlock) {
				int tryBlockNodeStartIndex = tryBlockNode.getStartIndex();
				if (!this.insnList.get(tryBlockNodeStartIndex).isThatVarStore(localSlotIndex) && !FrameNode.isUnknownType(this.frames[tryBlockNodeStartIndex].getLocalVariables()[localSlotIndex])) {
					List<AnalyzingNode> tryBlockPredecessors = tryBlockNode.getPredecessors();
					if (tryBlockPredecessors != null && tryBlockPredecessors.size() > 0) {
						for (int tryBlockPredecessorIndex = 0; tryBlockPredecessorIndex < tryBlockPredecessors.size(); tryBlockPredecessorIndex++) {
							AnalyzingNode tryBlockPredecessor = tryBlockPredecessors.get(tryBlockPredecessorIndex);
							if (!nodesInTryBlock.contains(tryBlockPredecessor) && (tryStartNode == null || tryBlockPredecessor != tryStartNode)) {
								nodesToTrace.add(tryBlockPredecessor);
							}
						}
					}
				}
			}
		}

		for (AnalyzingNode nodeToTrace : nodesToTrace) {
			visitedBitSet.clear();
			if (catchNode != nodeToTrace && !catchNode.canReach(nodeToTrace, visitedBitSet)) {
				this.traceLocalVariableSource(analysisContext, nodeToTrace, nodeToTrace.getEndIndex() + 1, localSlotIndex);
			} else {
				analysisContext.currentReferences.add(new ErrorReference(this.insnList.get(catchNode.getStartIndex()), "Loop through catch"));
			}
		}
	}

	private boolean isGetPropertyNoDefault(AbstractMethodCallWriter methodCall, AbstractWorkspace workspace) {
		return this.isGetPropertyCall(methodCall, "getProperty(Ljava/lang/String;)Ljava/lang/String;", workspace);
	}

	public AnalyzingNode getNodeByStartIndex(int startIndex) {
		int low = 0;
		int high = this.nodes.size() - 1;

		while (low <= high) {
			int mid = (low + high) / 2;
			AnalyzingNode node = this.nodes.get(mid);
			int nodeStartIndex = node.getStartIndex();
			if (startIndex < nodeStartIndex) {
				high = mid - 1;
			} else {
				if (startIndex <= nodeStartIndex) {
					return node;
				}

				low = mid + 1;
			}
		}

		return null;
	}

	private List<AnalyzingNode> resolveJsrTraversalOrder(Set<AnalyzingNode> jsrEntryNodes) {
		ArrayList<AnalyzingNode> orderedNodes = new ArrayList<>(jsrEntryNodes.size());
		if (jsrEntryNodes.size() <= 1) {
			if (jsrEntryNodes.size() == 1) {
				orderedNodes.add(jsrEntryNodes.iterator().next());
			}

			return orderedNodes;
		} else {
			DualKeyMap<AnalyzingNode, AnalyzingNode, Boolean> jsrReturnLinks = new DualKeyMap<>();

			for (AnalyzingNode jsrEntryNode : jsrEntryNodes) {
				this.buildJsrReturnLinks(jsrEntryNode, jsrEntryNode, jsrReturnLinks, GlobalUtils.newHashSet(13), false);
			}

			HashMap<AnalyzingNode, AnalyzingNode> groupKeyByMember = GlobalUtils.newHashMap(Math.max(GlobalUtils.getInitialCapacity(jsrEntryNodes.size()), 7));
			DualKeyMap<AnalyzingNode, AnalyzingNode, Boolean> returnLinksSnapshot = GlobalUtils.clone(jsrReturnLinks);
			Enumeration<AnalyzingNode> groupKeyEnumeration = returnLinksSnapshot.getGroupKeys();

			while (groupKeyEnumeration.hasMoreElements()) {
				AnalyzingNode groupKey = groupKeyEnumeration.nextElement();

				for (AnalyzingNode groupMember : returnLinksSnapshot.getGroup(groupKey).keySet()) {
					AnalyzingNode previousGroupKey = groupKeyByMember.put(groupMember, groupKey);
					if (previousGroupKey != null) {
						boolean linkFromGroupKey = returnLinksSnapshot.get(groupKey, groupMember);
						boolean linkFromPreviousGroupKey = returnLinksSnapshot.get(previousGroupKey, groupMember);
						if (linkFromPreviousGroupKey && !linkFromGroupKey) {
							Boolean removedPreviousGroupLink = jsrReturnLinks.remove(previousGroupKey, groupMember);
						} else if (!linkFromPreviousGroupKey && linkFromGroupKey) {
							groupKeyByMember.put(groupMember, previousGroupKey);
							Boolean removedGroupKeyLink = jsrReturnLinks.remove(groupKey, groupMember);
						} else {
							if (linkFromPreviousGroupKey && linkFromGroupKey) {
								throw new MethodAnalyzerException("Method '" + this.methodAnalyzing + "' could not be analyzed (C). No action required.", this.methodAnalyzing);
							}

							if (!linkFromPreviousGroupKey && !linkFromGroupKey) {
								DebugUtils.assertTrue(false, new String[]{"Method '" + this.methodAnalyzing + "' could not be analyzed (D)"});
							}
						}
					}
				}
			}

			for (AnalyzingNode ungroupedEntryNode : jsrEntryNodes) {
				if (!groupKeyByMember.containsKey(ungroupedEntryNode)) {
					this.appendReachableNodes(ungroupedEntryNode, jsrReturnLinks, orderedNodes);
				}
			}

			if (orderedNodes.size() != jsrEntryNodes.size()) {
				throw new MethodAnalyzerException("Method '" + this.methodAnalyzing + "' could not be analyzed (E). No action required.", this.methodAnalyzing);
			} else {
				return orderedNodes;
			}
		}
	}

	public FrameNode[] buildSpecialStackMapFrames() {
		Integer[] stackMapLabelIndexes = this.getStackMapLabelIndexes();
		FrameNode[] specialFrames = new FrameNode[this.frames.length];
		System.arraycopy(this.frames, 0, specialFrames, 0, this.frames.length);
		AtomicBool localsChanged = new AtomicBool();
		Integer[] labelIndexes = stackMapLabelIndexes;
		int labelIndexCount = stackMapLabelIndexes.length;

		for (int indexPosition = 0; indexPosition < labelIndexCount; indexPosition++) {
			int labelInstructionIndex = labelIndexes[indexPosition];
			if (((LabelNode)this.insnList.get(labelInstructionIndex)).hasFlag(1024)) {
				AnalyzingNode analyzingNode = this.getNodeByStartIndex(labelInstructionIndex);

				for (ExceptionAnalyzingInfo exceptionInfo : this.exceptionInfoByCatchNode.get(analyzingNode)) {
					Integer[] candidateIndexes = stackMapLabelIndexes;
					int candidateIndexCount = stackMapLabelIndexes.length;

					for (int candidatePosition = 0; candidatePosition < candidateIndexCount; candidatePosition++) {
						int candidateInstructionIndex = candidateIndexes[candidatePosition];
						if (candidateInstructionIndex >= exceptionInfo.start_pc && candidateInstructionIndex < exceptionInfo.end_pc) {
							FrameNode.mergeLocals(
								this.typeResolver, specialFrames[labelInstructionIndex].getLocalVariables(), specialFrames[candidateInstructionIndex].getLocalVariables(), false, localsChanged, "Building special StackMapTable"
							);
						}
					}
				}
			}
		}

		return specialFrames;
	}

	private HashSet<Reference> traceFieldReadSources(AnalysisContext analysisContext, FieldNode fieldNode, List<Integer> writeIndexes) {
		HashSet<Reference> sourceReferences = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(writeIndexes.size() + 2));
		boolean analyzedMethodStatic = this.instructionListNode.getMethodNode().isStatic();

		for (int i = 0; i < writeIndexes.size(); i++) {
			int writeIndex = writeIndexes.get(i);
			AnalyzingNode writeNode = this.getNodeForInstructionIndex(writeIndex);
			if (this.frames[writeIndex] != null) {
				int stackSize = this.frames[writeIndex].getStackSize() + (fieldNode.isStatic() ? 0 : 1);
				AnalysisContext writeContext = new AnalysisContext(
					this, sourceReferences, false, analysisContext.analysisType, analysisContext.fieldReadsByField, analysisContext.fieldWritesByField, analysisContext.methodCallsByMethod, analysisContext.typeResolver, analysisContext.workspace, analysisContext.reflectionPatterns, analyzedMethodStatic, analysisContext.methodAnalysisResults, analysisContext.fieldReferencesCache, analysisContext.stackSourceCache, analysisContext.localVarSourceCache, analysisContext.getNestingLevel() + 1, analysisContext.methodAnalyzerCache
				);
				this.traceStackSources(writeContext, writeNode, writeIndex, stackSize);
			}
		}

		return sourceReferences;
	}

	public void traceStackSources(AnalysisContext analysisContext, AnalyzingNode analyzingNode, int instructionIndex, int stackIndex) {
		ExcludeElement cacheKey = new ExcludeElement(this.instructionListNode, INTEGER_CACHE.valueOf(instructionIndex), INTEGER_CACHE.valueOf(stackIndex));
		HashSet<Reference> cachedReferences = analysisContext.stackSourceCache.get(cacheKey);
		if (cachedReferences != null && analysisContext.analysisType != AnalysisType.USER_DEFINED_METHOD_1 && analysisContext.analysisType != AnalysisType.USER_DEFINED_METHOD_2) {
			analysisContext.currentReferences.addAll(cachedReferences);
		} else {
			cachedReferences = GlobalUtils.newHashSet(13);
			HashSet<Reference> outerReferences = analysisContext.swapCurrentReferences(cachedReferences);
			if (analysisContext.getNestingLevel() > 3) {
				analysisContext.currentReferences.add(new ErrorReference(null, "Max Nesting Level Exceeded: " + analysisContext.getNestingLevel() + " " + analysisContext.analysisType));
			} else {
				boolean sourceFound = false;
				int nodeStartIndex = analyzingNode.getStartIndex();

				for (int insnIndex = instructionIndex - 1; !sourceFound && insnIndex >= nodeStartIndex; insnIndex--) {
					AbstractInsnNode insn = this.insnList.get(insnIndex);
					FrameNode frame = this.frames[insnIndex];
					if (analysisContext.analysisType == AnalysisType.OBJECT_TYPE) {
						String operandType = TypeResolver.toInternalName(frame.getOperandStack()[stackIndex].getDescriptor());
						analysisContext.currentReferences.add(new InstructionTypeReference(insn, operandType));
						sourceFound = true;
						break;
					}

					if (insn.isPureStackOpcode()) {
						stackIndex = insn.getStackIndexOrigin(frame.getOperandStack(), stackIndex);
					} else if (frame.getStackSize() - 1 == stackIndex) {
						if (insn.isStackProducer()) {
							switch (insn.getOpcode()) {
								case ACONST_NULL:
									analysisContext.currentReferences.add(new NullReference());
									break;
								case LDC:
									TypeWriter ldcTypeWriter = ((LdcNode)insn).getTypeWriter();
									if (ldcTypeWriter instanceof StringNode) {
										analysisContext.currentReferences.add(new StringReference((StringNode)ldcTypeWriter));
									} else if (ldcTypeWriter instanceof ClassInfoWriter) {
										analysisContext.currentReferences.add(new ClassReference((ClassInfoWriter)ldcTypeWriter));
									} else if (ldcTypeWriter instanceof MethodDescriptorWriter) {
										analysisContext.currentReferences.add(new MethodTypeReference((MethodDescriptorWriter)ldcTypeWriter));
									}
									break;
								case LDC_W:
									TypeWriter ldcWideTypeWriter = ((WideConstantPoolInsnNode)insn).getTypeWriter();
									if (ldcWideTypeWriter instanceof StringNode) {
										analysisContext.currentReferences.add(new StringReference((StringNode)ldcWideTypeWriter));
									} else if (ldcWideTypeWriter instanceof ClassInfoWriter) {
										analysisContext.currentReferences.add(new ClassReference((ClassInfoWriter)ldcWideTypeWriter));
									} else if (ldcWideTypeWriter instanceof MethodDescriptorWriter) {
										analysisContext.currentReferences.add(new MethodTypeReference((MethodDescriptorWriter)ldcWideTypeWriter));
									}
									break;
								case GETSTATIC:
								case GETFIELD:
									this.traceFieldSource(analysisContext, insnIndex);
									break;
								case INVOKEVIRTUAL:
								case INVOKESPECIAL:
								case INVOKESTATIC:
								case INVOKEINTERFACE:
									this.traceMethodCallSource(analysisContext, analyzingNode, insnIndex);
									break;
								case NEW:
									this.tracePropertiesFileName(analysisContext, analyzingNode, insnIndex);
									break;
								case ANEWARRAY:
									this.traceArrayDimensionSource(analysisContext, analyzingNode, insnIndex);
									break;
								case 255:
									VarInsnNode varInsn = (VarInsnNode)insn;
									if (varInsn.getOperationType() == LocalVarOperationType.OBJECT_LOAD) {
										this.traceLocalVariableSource(analysisContext, analyzingNode, insnIndex, varInsn.getVariableIndex());
									} else {
										analysisContext.currentReferences.add(new ErrorReference(insn, "Instruction not handled: " + analysisContext.analysisType));
									}
									break;
								default:
									analysisContext.currentReferences.add(new ErrorReference(insn, "Instruction not handled: " + analysisContext.analysisType));
							}

							sourceFound = true;
						}
					} else if (insn.isJSR()) {
						analysisContext.currentReferences.add(new ErrorReference(insn, "Instruction not handled: " + analysisContext.analysisType));
						sourceFound = true;
					}
				}

				if (!sourceFound) {
					List<AnalyzingNode> predecessors = analyzingNode.getPredecessors();
					if (predecessors != null && predecessors.size() > 0) {
						for (int predecessorIndex = 0; predecessorIndex < predecessors.size(); predecessorIndex++) {
							AnalyzingNode predecessor = predecessors.get(predecessorIndex);
							this.traceStackSources(analysisContext, predecessor, predecessor.getEndIndex() + 1, stackIndex);
						}
					} else {
						AbstractInsnNode nodeStartInsn = this.insnList.get(nodeStartIndex);
						analysisContext.currentReferences.add(new ErrorReference(nodeStartInsn, "No source found 1):" + analysisContext.analysisType));
					}
				}
			}

			cachedReferences = analysisContext.swapCurrentReferences(outerReferences);
			outerReferences.addAll(cachedReferences);
			if (analysisContext.analysisType != AnalysisType.USER_DEFINED_METHOD_1 && analysisContext.analysisType != AnalysisType.USER_DEFINED_METHOD_2) {
				HashSet<Reference> previousCachedReferences = analysisContext.stackSourceCache.put(cacheKey, cachedReferences);
				if (previousCachedReferences != null) {
					cachedReferences.addAll(previousCachedReferences);
				}
			}
		}
	}

	public boolean hasUninitializedTypesAtInsn(int instructionIndex) {
		return this.frames[instructionIndex].hasUninitializedTypes();
	}

	private void collectIntegerValues(List<Object> constantValues, AnalyzingNode analyzingNode, int startIndex, int stackIndex, Set<AnalyzingNode> visitedNodes) {
		if (!visitedNodes.contains(analyzingNode)) {
			visitedNodes.add(analyzingNode);
			boolean valueFound = false;
			int nodeStartIndex = analyzingNode.getStartIndex();

			for (int insnIndex = startIndex - 1; !valueFound && insnIndex >= nodeStartIndex; insnIndex--) {
				AbstractInsnNode insn = this.insnList.get(insnIndex);
				FrameNode frame = this.frames[insnIndex];
				if (insn.isPureStackOpcode()) {
					stackIndex = insn.getStackIndexOrigin(frame.getOperandStack(), stackIndex);
				} else if (frame.getStackSize() - 1 == stackIndex && insn.isStackProducer()) {
					switch (insn.getOpcode()) {
						case ICONST_0:
							constantValues.add(INTEGER_CACHE.valueOf(0));
							break;
						case ICONST_1:
							constantValues.add(INTEGER_CACHE.valueOf(1));
							break;
						case ICONST_2:
							constantValues.add(INTEGER_CACHE.valueOf(2));
							break;
						case ICONST_3:
							constantValues.add(INTEGER_CACHE.valueOf(3));
							break;
						case ICONST_4:
							constantValues.add(INTEGER_CACHE.valueOf(4));
							break;
						case ICONST_5:
							constantValues.add(INTEGER_CACHE.valueOf(5));
							break;
						case LCONST_0:
						case LCONST_1:
						case FCONST_0:
						case FCONST_1:
						case FCONST_2:
						case DCONST_0:
						case DCONST_1:
						default:
							constantValues.add("Instruction not handled: " + insn.getOpcodeMnemonics());
							break;
						case BIPUSH:
							BiPushNode biPushNode = (BiPushNode)insn;
							constantValues.add(INTEGER_CACHE.valueOf(biPushNode.getValue()));
					}

					valueFound = true;
				}
			}

			if (!valueFound) {
				List<AnalyzingNode> predecessors = analyzingNode.getPredecessors();
				if (predecessors != null && predecessors.size() > 0) {
					for (int predecessorIndex = 0; predecessorIndex < predecessors.size(); predecessorIndex++) {
						AnalyzingNode predecessor = predecessors.get(predecessorIndex);
						this.collectIntegerValues(constantValues, predecessor, predecessor.getEndIndex() + 1, stackIndex, visitedNodes);
					}
				} else {
					constantValues.add("No value found");
				}
			}
		}
	}

	public int getMinReturnStackDepth() {
		return this.minReturnStackDepth;
	}

	public boolean isUninitializedUsageConsistent() {
		return this.uninitializedUsageConsistent;
	}

	private void linkJsrRetNodes(AnalyzingNode jsrEntryNode, AnalyzingNode analyzingNode, HashSet<AnalyzingNode> visitedNodes) {
		if (!visitedNodes.contains(analyzingNode)) {
			visitedNodes.add(analyzingNode);
			AnalyzingNode jsrContinuationNode = analyzingNode.getJsrContinuationNode();
			if (jsrContinuationNode != null) {
				this.linkJsrRetNodes(jsrEntryNode, jsrContinuationNode, visitedNodes);
			} else {
				Enumeration<AnalyzingNode> successorEnumeration = analyzingNode.enumerateSuccessors();
				if (successorEnumeration != null && successorEnumeration.hasMoreElements()) {
					while (successorEnumeration.hasMoreElements()) {
						AnalyzingNode successor = successorEnumeration.nextElement();
						this.linkJsrRetNodes(jsrEntryNode, successor, visitedNodes);
					}
				} else {
					AbstractInsnNode lastInsn = this.insnList.get(analyzingNode.getEndIndex());
					if (lastInsn.isRet() && analyzingNode.getJsrCallSiteNode() == null) {
						VarInsnNode retInsn = (VarInsnNode)lastInsn;

						for (AnalyzingNode jsrCallSiteNode : jsrEntryNode.getJsrCallSiteNodes()) {
							if (this.jsrContinuationsByRetNode == null) {
								this.jsrContinuationsByRetNode = new DualKeyMap<>(13, 13);
							}

							Integer continuationStartIndex = INTEGER_CACHE.valueOf(jsrCallSiteNode.getJsrContinuationNode().getStartIndex());
							this.jsrContinuationsByRetNode.put(retInsn, continuationStartIndex, continuationStartIndex);
						}

						analyzingNode.setJsrCallSiteNode(jsrEntryNode);
						jsrEntryNode.addJsrRetNode(analyzingNode);
					}
				}
			}

			Enumeration<AnalyzingNode> catchNodeEnumeration = analyzingNode.enumerateCatchNodes();

			while (catchNodeEnumeration.hasMoreElements()) {
				AnalyzingNode catchNode = catchNodeEnumeration.nextElement();
				this.linkJsrRetNodes(jsrEntryNode, catchNode, visitedNodes);
			}
		}
	}

	public int getStackSizeAtInsn(int instructionIndex) {
		return this.frames[instructionIndex].getStackSize();
	}

	private void collectConnectedNodes(AnalyzingNode startNode, Set<AnalyzingNode> reachableNodes) {
		HashSet<AnalyzingNode> frontierNodes = GlobalUtils.newHashSet();
		HashSet<AnalyzingNode> nextFrontierNodes = GlobalUtils.newHashSet();
		frontierNodes.add(startNode);

		do {
			nextFrontierNodes.clear();

			for (AnalyzingNode frontierNode : frontierNodes) {
				if (reachableNodes.add(frontierNode)) {
					Enumeration<AnalyzingNode> successorEnumeration = frontierNode.enumerateSuccessors();
					if (successorEnumeration != null) {
						while (successorEnumeration.hasMoreElements()) {
							AnalyzingNode successor = successorEnumeration.nextElement();
							nextFrontierNodes.add(successor);
						}
					}

					AnalyzingNode jsrContinuationNode = frontierNode.getJsrContinuationNode();
					if (jsrContinuationNode != null) {
						nextFrontierNodes.add(jsrContinuationNode);
					}
				}
			}

			frontierNodes.clear();
			frontierNodes.addAll(nextFrontierNodes);
		} while (frontierNodes.size() > 0);
	}

	public static FrameNode[] getFramesInternal(MethodAnalyzer methodAnalyzer) {
		return methodAnalyzer.frames;
	}

	public static InsnList<AbstractInsnNode> getInstructionListInternal(MethodAnalyzer methodAnalyzer) {
		return methodAnalyzer.insnList;
	}

	public static void invokeCollectIntegerValues(
		MethodAnalyzer methodAnalyzer, List<Object> constantValues, AnalyzingNode analyzingNode, int startIndex, int stackIndex, Set<AnalyzingNode> visitedNodes
	) {
		methodAnalyzer.collectIntegerValues(constantValues, analyzingNode, startIndex, stackIndex, visitedNodes);
	}
}
