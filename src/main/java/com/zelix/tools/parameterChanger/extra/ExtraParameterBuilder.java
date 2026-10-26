package com.zelix.tools.parameterChanger.extra;

import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.signature.AbstractMethodKey;
import com.zelix.framework.signature.MethodParameterKey;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.repository.Hierarchy;
import com.zelix.tools.parameterChanger.ParameterChangerExcludes;
import com.zelix.tools.parameterChanger.ParameterManager;
import com.zelix.tools.parameterChanger.ParameterPattern;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.ClassHierarchyNode;

import java.util.*;

public class ExtraParameterBuilder {
	private final Hierarchy hierarchy;
	private final ParameterManager parameterManager;
	private final boolean allowExtraParameters;
	private final Random random;

	public ExtraParameterBuilder(ParameterChangerExcludes parameterChangerExcludes, ParameterManager parameterManager, boolean allowExtraParameters, Random random) {
		this.hierarchy = parameterManager.getHierarchy();
		this.parameterManager = parameterManager;
		this.allowExtraParameters = allowExtraParameters;
		this.random = random;
	}

	private static List<ParameterPattern> collectPatternsForTemplate(boolean hasLambdaSamParamCount, MethodSignature methodSignature, boolean isVarArgs, char[] extraParamTypes) {
		ArrayList<ParameterPattern> patterns = new ArrayList<>();
		String methodName = methodSignature.getName();
		String fullDescriptor = methodSignature.getFullDescriptor();
		String returnDescriptor = MethodSignature.extractReturnDescriptor(fullDescriptor);
		List<String> parameterDescriptors = MethodSignature.parseParameterDescriptors(fullDescriptor);
		ParameterPattern pattern = null;
		ExtraParameterPatternBuilder patternBuilder = new ExtraParameterPatternBuilder();
		HashSet<String> seenDescriptors = GlobalUtils.newHashSet();

		while (true) {
			pattern = patternBuilder.nextPatternInTemplate(methodName, hasLambdaSamParamCount, parameterDescriptors, returnDescriptor, extraParamTypes, pattern, isVarArgs);
			if (pattern == null) {
				return patterns;
			}

			String patternDescriptor = pattern.getDescriptor();
			if (seenDescriptors.add(patternDescriptor)) {
				new MethodSignature(methodName, patternDescriptor);
				patterns.add(pattern);
			}
		}
	}

	public boolean hasValidLambdaSamParams(MethodNode methodNode, ParameterPattern parameterPattern) {
		if (!methodNode.hasLambdaSamParamCount()) {
			return true;
		} else {
			int lambdaSamParamCount = methodNode.getLambdaSamParamCount();
			if (lambdaSamParamCount > 0) {
				ExtraParameter[] extraParameters = parameterPattern.getExtraParameters();
				ExtraParameter lastExtraParameter = extraParameters[extraParameters.length - 1];
				List<String> parameterDescriptors = MethodSignature.parseParameterDescriptors(methodNode.getDescriptor());
				if (lastExtraParameter.getParameterIndex() >= parameterDescriptors.size() + extraParameters.length - lambdaSamParamCount) {
					return false;
				}
			}

			return true;
		}
	}

	public ParameterPattern buildPrivateMethodPattern(
		ClassHierarchyNode classHierarchyNode,
		String classInternalName,
		MethodNode methodNode,
		ReferenceGraph<AbstractMethodKey, AbstractMethodNode> referenceGraph,
		Map<MethodSignature, MethodNode> methodsBySignature,
		Map<MethodParameterKey, MethodNode> methodsByParameterKey,
		boolean forceExtraParams,
		boolean useFullSignatureMatching
	) {
		return this.buildMethodPattern(classHierarchyNode, classInternalName, methodNode, referenceGraph, classHierarchyNode.isInterface(), methodsBySignature, methodsByParameterKey, forceExtraParams, useFullSignatureMatching);
	}

	public final ParameterPattern buildMethodPattern(
		ClassHierarchyNode classHierarchyNode,
		String classInternalName,
		MethodNode methodNode,
		ReferenceGraph<AbstractMethodKey, AbstractMethodNode> referenceGraph,
		boolean isInterface,
		Map<MethodSignature, MethodNode> methodsBySignature,
		Map<MethodParameterKey, MethodNode> methodsByParameterKey,
		boolean forceExtraParams,
		boolean useFullSignatureMatching
	) {
		boolean randomizeSearch = false;
		boolean shufflePatterns = false;
		if (this.allowExtraParameters) {
			int randomizeOneIn = 1;
			if (Options.extraParamProbability != null) {
				try {
					randomizeOneIn = Integer.parseInt(Options.extraParamProbability);
				} catch (Exception ignoredExtraParamParse) {
				}
			}

			if (forceExtraParams || Options.forceExtraParams || this.random.nextInt(randomizeOneIn) == 0) {
				randomizeSearch = true;
			}

			int shuffleOneIn = 4;
			if (Options.patternShuffleProbability != null) {
				try {
					shuffleOneIn = Integer.parseInt(Options.patternShuffleProbability);
				} catch (Exception ignoredShuffleParse) {
				}
			} else if (forceExtraParams || Options.forceExtraParams) {
				shuffleOneIn = 25;
			}

			if (this.random.nextInt(shuffleOneIn) == 0) {
				shufflePatterns = true;
			}
		}

		char[][] patterns;
		if (shufflePatterns) {
			patterns = new char[ExtraParameterPatternBuilder.PATTERNS.length][];
			System.arraycopy(ExtraParameterPatternBuilder.PATTERNS, 0, patterns, 0, ExtraParameterPatternBuilder.PATTERNS.length);
			GlobalUtils.shuffle((char[][])patterns, GlobalUtils.newRandom(131));
		} else {
			patterns = ExtraParameterPatternBuilder.PATTERNS;
		}

		MethodSignature methodSignature = methodNode.getMethodSignature();
		boolean varArgs = methodNode.isVarArgs();
		if (varArgs && MethodSignature.parseParameterDescriptors(methodSignature.getFullDescriptor()).size() == 0) {
			return null;
		} else {
			ParameterPattern pattern;
			if (!randomizeSearch && !Options.useRandomizedPatternSearch) {
				pattern = this.findPatternInOrder(classHierarchyNode, methodNode, methodSignature, varArgs, referenceGraph, isInterface, methodsBySignature, methodsByParameterKey, useFullSignatureMatching, patterns);
			} else {
				pattern = this.findPatternRandomized(classHierarchyNode, methodNode, methodSignature, varArgs, referenceGraph, isInterface, methodsBySignature, methodsByParameterKey, useFullSignatureMatching, patterns);
			}

			return pattern;
		}
	}

	public ParameterPattern buildStaticMethodPattern(
		ClassHierarchyNode classHierarchyNode,
		String classInternalName,
		MethodNode methodNode,
		ReferenceGraph<AbstractMethodKey, AbstractMethodNode> referenceGraph,
		boolean isInterface,
		Map<MethodSignature, MethodNode> methodsBySignature,
		Map<MethodParameterKey, MethodNode> methodsByParameterKey,
		boolean forceExtraParams,
		boolean useFullSignatureMatching
	) {
		return this.buildMethodPattern(classHierarchyNode, classInternalName, methodNode, referenceGraph, classHierarchyNode.isInterface(), methodsBySignature, methodsByParameterKey, forceExtraParams, useFullSignatureMatching);
	}

	private ParameterPattern findPatternInOrder(
		ClassHierarchyNode classHierarchyNode,
		MethodNode methodNode,
		MethodSignature methodSignature,
		boolean isVarArgs,
		ReferenceGraph<AbstractMethodKey, AbstractMethodNode> referenceGraph,
		boolean isInterface,
		Map<MethodSignature, MethodNode> methodsBySignature,
		Map<MethodParameterKey, MethodNode> methodsByParameterKey,
		boolean useFullSignatureMatching,
		char[][] patterns
	) {
		String methodName = methodSignature.getName();
		String fullDescriptor = methodSignature.getFullDescriptor();
		String returnDescriptor = MethodSignature.extractReturnDescriptor(fullDescriptor);
		List<String> parameterDescriptors = MethodSignature.parseParameterDescriptors(fullDescriptor);
		ParameterPattern pattern = null;
		AtomicInt templateIndex = new AtomicInt();
		ExtraParameterPatternBuilder patternBuilder = new ExtraParameterPatternBuilder();
		HashSet<String> seenDescriptors = GlobalUtils.newHashSet();

		while (true) {
			pattern = patternBuilder.nextPatternAcrossTemplates(methodName, methodNode.hasLambdaSamParamCount(), parameterDescriptors, returnDescriptor, templateIndex, pattern, isVarArgs, patterns);
			if (pattern == null) {
				return null;
			}

			String patternDescriptor = pattern.getDescriptor();
			if (seenDescriptors.add(patternDescriptor)) {
				MethodSignature candidateSignature = new MethodSignature(methodName, patternDescriptor);
				if (this.isPatternApplicable(classHierarchyNode, methodNode, pattern, candidateSignature, methodSignature, referenceGraph, methodsBySignature, methodsByParameterKey, useFullSignatureMatching, isInterface)) {
					return pattern;
				}
			}
		}
	}

	public ParameterPattern getOrBuildMethodPattern(
		ClassHierarchyNode classHierarchyNode,
		String classInternalName,
		MethodNode methodNode,
		ReferenceGraph<AbstractMethodKey, AbstractMethodNode> referenceGraph,
		boolean isInterface,
		Map<MethodSignature, MethodNode> methodsBySignature,
		Map<MethodParameterKey, MethodNode> methodsByParameterKey,
		boolean forceExtraParams,
		boolean useFullSignatureMatching
	) {
		MethodSignature methodSignature = methodNode.getMethodSignature();
		AtomicReference<List<ClassHierarchyNode>> implementationsRef = null;
		if (classHierarchyNode.isInterface()) {
			implementationsRef = this.hierarchy.getMethodImplementationsRef(classHierarchyNode, methodSignature);
		}

		ParameterPattern pattern;
		if (implementationsRef != null) {
			pattern = this.hierarchy.getContextParameterPattern(implementationsRef);
			if (pattern == null) {
				AbstractMethodNode interfaceBaseMethod = this.hierarchy.getBaseMethod(methodNode);
				if (interfaceBaseMethod != null) {
					ClassHierarchyNode interfaceBaseMethodClass = ClassHierarchyNode.getNodeByInternalName(interfaceBaseMethod.getInternalName());
					if (interfaceBaseMethodClass == null || interfaceBaseMethodClass.isMissing()) {
						pattern = null;
					} else if (!interfaceBaseMethodClass.isWorkspaceClass()) {
						pattern = this.parameterManager.getParameterPattern(interfaceBaseMethod);
					} else {
						pattern = this.buildMethodPattern(classHierarchyNode, classInternalName, methodNode, referenceGraph, isInterface, methodsBySignature, methodsByParameterKey, forceExtraParams, useFullSignatureMatching);
					}
				} else {
					pattern = this.buildMethodPattern(classHierarchyNode, classInternalName, methodNode, referenceGraph, isInterface, methodsBySignature, methodsByParameterKey, forceExtraParams, useFullSignatureMatching);
				}

				if (pattern != null) {
					this.hierarchy.setContextParameterPattern(implementationsRef, pattern);
				}
			}
		} else {
			AbstractMethodNode baseMethod = this.hierarchy.getBaseMethod(methodNode);
			if (baseMethod != null) {
				ClassHierarchyNode baseMethodClass = ClassHierarchyNode.getNodeByInternalName(baseMethod.getInternalName());
				if (baseMethodClass != null && !baseMethodClass.isMissing()) {
					pattern = this.parameterManager.getParameterPattern(baseMethod);
				} else {
					pattern = null;
				}
			} else {
				pattern = this.buildMethodPattern(classHierarchyNode, classInternalName, methodNode, referenceGraph, isInterface, methodsBySignature, methodsByParameterKey, forceExtraParams, useFullSignatureMatching);
			}
		}

		return pattern;
	}

	public final boolean isPatternApplicable(
		ClassHierarchyNode classHierarchyNode,
		MethodNode methodNode,
		ParameterPattern parameterPattern,
		MethodSignature candidateSignature,
		MethodSignature originalSignature,
		ReferenceGraph<AbstractMethodKey, AbstractMethodNode> referenceGraph,
		Map<MethodSignature, MethodNode> methodsBySignature,
		Map<MethodParameterKey, MethodNode> methodsByParameterKey,
		boolean useFullSignatureMatching,
		boolean isInterface
	) {
		MethodParameterKey parameterKey = candidateSignature.getParameterKey();
		AbstractMethodKey memberLookupKey = (AbstractMethodKey)(useFullSignatureMatching ? candidateSignature : parameterKey);
		if (candidateSignature.getFullDescriptor().equals(originalSignature.getFullDescriptor())) {
			return false;
		} else if ((!useFullSignatureMatching || !methodsBySignature.containsKey(candidateSignature)) && (useFullSignatureMatching || !methodsByParameterKey.containsKey(candidateSignature.getParameterKey()))) {
			if (!originalSignature.isConstructor() && referenceGraph.hasMember(memberLookupKey)) {
				return false;
			} else if (this.hierarchy.isConflicting(candidateSignature)) {
				return false;
			} else if (this.hierarchy.areMethodsDifferent(classHierarchyNode, candidateSignature, originalSignature)) {
				return false;
			} else {
				return isInterface && !this.hierarchy.checkMethodChangeWithFlag(classHierarchyNode, candidateSignature, originalSignature, true, new AtomicReference<>()) ? false : this.hasValidLambdaSamParams(methodNode, parameterPattern);
			}
		} else {
			return false;
		}
	}

	private ParameterPattern findPatternRandomized(
		ClassHierarchyNode classHierarchyNode,
		MethodNode methodNode,
		MethodSignature methodSignature,
		boolean isVarArgs,
		ReferenceGraph<AbstractMethodKey, AbstractMethodNode> referenceGraph,
		boolean isInterface,
		Map<MethodSignature, MethodNode> methodsBySignature,
		Map<MethodParameterKey, MethodNode> methodsByParameterKey,
		boolean useFullSignatureMatching,
		char[][] patterns
	) {
		String methodName = methodSignature.getName();
		HashSet<String> seenDescriptors = GlobalUtils.newHashSet();

		for (int templateIndex = 0; templateIndex < patterns.length; templateIndex++) {
			char[] template = patterns[templateIndex];
			List<ParameterPattern> candidatePatterns = collectPatternsForTemplate(methodNode.hasLambdaSamParamCount(), methodSignature, isVarArgs, template);
			if (candidatePatterns.size() > 1) {
				GlobalUtils.shuffle(candidatePatterns, this.random);
			}

			for (ParameterPattern pattern : candidatePatterns) {
				String patternDescriptor = pattern.getDescriptor();
				if (seenDescriptors.add(patternDescriptor)) {
					MethodSignature candidateSignature = new MethodSignature(methodName, patternDescriptor);
					if (this.isPatternApplicable(classHierarchyNode, methodNode, pattern, candidateSignature, methodSignature, referenceGraph, methodsBySignature, methodsByParameterKey, useFullSignatureMatching, isInterface)) {
						return pattern;
					}
				}
			}
		}

		return null;
	}
}
