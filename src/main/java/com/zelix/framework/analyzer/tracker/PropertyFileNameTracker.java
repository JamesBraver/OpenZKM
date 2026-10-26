package com.zelix.framework.analyzer.tracker;

import com.zelix.exceptions.FileException;
import com.zelix.framework.analyzer.AnalysisContext;
import com.zelix.framework.analyzer.AnalysisType;
import com.zelix.framework.analyzer.AnalyzingNode;
import com.zelix.framework.analyzer.MethodAnalyzer;
import com.zelix.framework.analyzer.reference.Reference;
import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.instructions.WideConstantPoolInsnNode;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.HierarchicalMultiMap;
import java.util.HashSet;
import java.util.List;

import static com.zelix.framework.instructions.OpcodeConstants.INVOKESPECIAL;
import static com.zelix.framework.instructions.OpcodeConstants.INVOKEVIRTUAL;

public class PropertyFileNameTracker implements VariableTracker {
	public final MethodAnalyzer methodAnalyzer;
	private HierarchicalMultiMap<Integer, Integer, Integer> visited;
	private HashSet<Reference> references;

	public PropertyFileNameTracker(MethodAnalyzer methodAnalyzer) {
		this.methodAnalyzer = methodAnalyzer;
		this.visited = new HierarchicalMultiMap<>(5, 97, 7);
		this.references = GlobalUtils.newHashSet(13);
	}

	public HashSet<Reference> getReferences() {
		return this.references;
	}

	@Override
	public boolean traceInsnSources(AnalysisContext context, int insnIndex, AnalyzingNode node) {
		AbstractInsnNode instruction = MethodAnalyzer.getInstructionListInternal(this.methodAnalyzer).get(insnIndex);
		if (instruction.getOpcode() != INVOKEVIRTUAL) {
			return false;
		} else {
			AbstractMethodCallWriter loadCall = (AbstractMethodCallWriter)((WideConstantPoolInsnNode)instruction).getTypeWriter();
			if (loadCall.getOwnerName().equals("java/util/Properties") && loadCall.getSignatureKey().equals("load(Ljava/io/InputStream;)V")) {
				boolean scanComplete = false;
				int nodeStartIndex = node.getStartIndex();
				int scanIndex = insnIndex;

				while (!scanComplete) {
					if (scanIndex > nodeStartIndex) {
						AbstractInsnNode previousInstruction = MethodAnalyzer.getInstructionListInternal(this.methodAnalyzer).get(--scanIndex);
						if (previousInstruction.getOpcode() == INVOKESPECIAL) {
							AbstractMethodCallWriter constructorCall = (AbstractMethodCallWriter)((WideConstantPoolInsnNode)previousInstruction).getTypeWriter();
							String ownerName = constructorCall.getOwnerName();
							String methodName = constructorCall.getName();

							try {
								if (methodName.equals("<init>") && (context.workspace.isClassInstanceOf(ownerName, "java/io/InputStream") || context.workspace.isClassInstanceOf(ownerName, "java/io/Reader"))) {
									List<String> parameterDescriptors = constructorCall.getParameterDescriptors();
									if (parameterDescriptors.size() == 1) {
										String parameterDescriptor = parameterDescriptors.get(0);
										if (parameterDescriptor.length() > 2) {
											parameterDescriptor = parameterDescriptor.substring(1, parameterDescriptor.length() - 1);
										}

										if (!parameterDescriptor.equals("java/io/InputStream") && !parameterDescriptor.equals("java/io/Reader")) {
											if (parameterDescriptor.equals("java/lang/String") && (ownerName.equals("java/io/FileInputStream") || ownerName.equals("java/io/FileReader"))) {
												int stackIndex = MethodAnalyzer.getFramesInternal(this.methodAnalyzer)[scanIndex].getOperandStack().length + 1;
												AnalysisContext tracingContext = new AnalysisContext(this.methodAnalyzer, context, AnalysisType.NORMAL, 0, true);
												tracingContext.swapCurrentReferences(this.references);
												if (scanIndex > nodeStartIndex) {
													this.methodAnalyzer.traceStackSources(tracingContext, node, scanIndex, stackIndex);
												} else {
													List<AnalyzingNode> predecessors = node.getPredecessors();

													for (int predecessorIndex = 0; predecessorIndex < predecessors.size(); predecessorIndex++) {
														AnalyzingNode predecessor = predecessors.get(predecessorIndex);
														this.methodAnalyzer.traceStackSources(tracingContext, predecessor, predecessor.getEndIndex() + 1, stackIndex);
													}
												}

												scanComplete = true;
											} else {
												scanComplete = true;
											}
										}
									}
								} else {
									scanComplete = true;
								}
							} catch (FileException ignored) {
								scanComplete = true;
							}
						} else {
							scanComplete = true;
						}
					} else {
						scanComplete = true;
					}
				}

				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public boolean isVisited(int step, int insnIndex, int position) {
		return this.visited
			.containsEntry(MethodAnalyzer.INTEGER_CACHE.valueOf(step), MethodAnalyzer.INTEGER_CACHE.valueOf(insnIndex), MethodAnalyzer.INTEGER_CACHE.valueOf(position));
	}

	@Override
	public VariableTracker createTracker(MethodAnalyzer methodAnalyzer) {
		return methodAnalyzer.createArrayElementTracker();
	}

	@Override
	public void markVisited(int step, int insnIndex, int position) {
		this.visited.put(MethodAnalyzer.INTEGER_CACHE.valueOf(step), MethodAnalyzer.INTEGER_CACHE.valueOf(insnIndex), MethodAnalyzer.INTEGER_CACHE.valueOf(position));
	}
}
