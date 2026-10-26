package com.zelix.tools.parameterChanger;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.hierarchy.Inheritance;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.reference.IReferenceGraph;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.signature.AbstractMethodKey;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.variable.LocalVariableOperand;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.LicenseCheckConstants2;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.repository.Hierarchy;
import com.zelix.tools.Obfuscator;
import com.zelix.tools.excludes.ExcludeStatementManager;
import com.zelix.tools.excludes.FixedClassManager;
import com.zelix.tools.flow.ControlFlowBuilder;
import com.zelix.tools.parameterChanger.dataset.ChangedMethod;
import com.zelix.tools.parameterChanger.extra.ExtraParameter;
import com.zelix.tools.parameterChanger.extra.ExtraParameterBuilder;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.*;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.dataset.AtomicBool;
import com.zelix.utils.dataset.Triplet;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.ClassTreeManager;
import com.zelix.workspace.Workspace;

import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

public class ParameterManager extends LicenseCheckConstants2 {
	private final Map<MethodNode, MethodSignature> baselineSignatureByMethod = GlobalUtils.newHashMap();
	private final Map<MethodNode, ParameterPattern> parameterPatternByMethod = GlobalUtils.newHashMap();
	private final Hierarchy hierarchy;
	private final ClassTreeManager classTreeManager;
	private final TypeResolver typeResolver;
	private final ClassNode[] allClassNodes;
	private final Workspace workspace;
	private final int keepInnerClassInfoMode;
	private final boolean useFullSignatureMatching;
	private final boolean randomizeProcessingOrder;
	private final ParameterChangerExcludes parameterChangerExcludes;
	private final FixedClassManager fixedClassManager;
	private final ChangeLogContext changeLogContext;
	private final boolean isLooseChangeLogFile;
	private final Map<MethodNode, Long> parameterKeysByMethod;
	private final Map<MethodNode, LocalVariableOperand> decryptionLocalVarByMethod;
	private final Set<MethodNode> methodsNeedingClassSeedXor;
	private final HashMap<String, String> classNameRemap;
	private final DualKeyMap<String, MethodSignature, MethodSignature> oldToNewMethodSignatureMap;
	private final DualKeyMap<String, MethodSignature, MethodSignature> newToOldMethodSignatureMap;
	private final AtomicReference<Triplet> defaultMpcClassTripletRef;
	private final Map<ArchiveNode, Triplet> archiveToMpcClassTripletMap;
	private final String methodParameterChangesPackage;
	private final IReferenceGraph<ClassNode, ClassNode> classReferenceGraph;
	private Map<MethodNode, String> resourceProtectedStaticMethods;
	private final AtomicBool enableParameterChanges;
	private final boolean isParameterObfuscationEnabled;
	private final ScriptContext scriptContext;
	private Object licenseCheckObject;
	private Map<MethodNode, ChangedMethod> changedMethodByMethod;

	public ParameterManager(
		ParameterChangerExcludes parameterChangerExcludes,
		FixedClassManager fixedClassManager,
		ChangeLogContext changeLogContext,
		boolean isLooseChangeLogFile,
		Workspace workspace,
		ClassNode[] workspaceClassNodes,
		AbstractClassNode[] abstractClassNodes,
		ClassTreeManager classTreeManager,
		TypeResolver typeResolver,
		int keepInnerClassInfoMode,
		boolean useFullSignatureMatching,
		Hierarchy hierarchy,
		boolean randomizeProcessingOrder,
		boolean isLicensedCopy,
		Map<MethodNode, Long> parameterKeysByMethod,
		Map<MethodNode, LocalVariableOperand> decryptionLocalVarByMethod,
		Set<MethodNode> methodsNeedingClassSeedXor,
		HashMap<String, String> classNameRemap,
		DualKeyMap<String, MethodSignature, MethodSignature> oldToNewMethodSignatureMap,
		DualKeyMap<String, MethodSignature, MethodSignature> newToOldMethodSignatureMap,
		AtomicReference<Triplet> defaultMpcClassTripletRef,
		Map<ArchiveNode, Triplet> archiveToMpcClassTripletMap,
		String methodParameterChangesPackage,
		IReferenceGraph<ClassNode, ClassNode> classReferenceGraph,
		Map<MethodNode, String> resourceProtectedStaticMethods,
		AtomicBool enableParameterChanges,
		boolean isParameterObfuscationEnabled,
		ScriptContext scriptContext
	) {
		this.hierarchy = hierarchy;
		this.classTreeManager = classTreeManager;
		this.typeResolver = typeResolver;
		ArrayList<ClassNode> classNodes = new ArrayList<>(workspaceClassNodes.length + 10);

		for (ClassNode classNode : workspaceClassNodes) {
			classNodes.add(classNode);
			if (classNode.hasInnerClasses()) {
				for (AbstractClassNode innerClass : classNode.getInnerClasses()) {
					classNodes.add((ClassNode)innerClass);
				}
			}
		}

		this.allClassNodes = classNodes.toArray(new ClassNode[classNodes.size()]);
		this.workspace = workspace;
		this.keepInnerClassInfoMode = keepInnerClassInfoMode;
		this.useFullSignatureMatching = useFullSignatureMatching;
		this.randomizeProcessingOrder = randomizeProcessingOrder;
		this.parameterChangerExcludes = parameterChangerExcludes;
		this.fixedClassManager = fixedClassManager;
		this.changeLogContext = changeLogContext;
		this.isLooseChangeLogFile = isLooseChangeLogFile;
		this.parameterKeysByMethod = parameterKeysByMethod;
		this.decryptionLocalVarByMethod = decryptionLocalVarByMethod;
		this.methodsNeedingClassSeedXor = methodsNeedingClassSeedXor;
		this.classNameRemap = classNameRemap;
		this.oldToNewMethodSignatureMap = oldToNewMethodSignatureMap;
		this.newToOldMethodSignatureMap = newToOldMethodSignatureMap;
		this.defaultMpcClassTripletRef = defaultMpcClassTripletRef;
		this.archiveToMpcClassTripletMap = archiveToMpcClassTripletMap;
		this.methodParameterChangesPackage = methodParameterChangesPackage;
		this.classReferenceGraph = classReferenceGraph;
		this.resourceProtectedStaticMethods = resourceProtectedStaticMethods;
		this.enableParameterChanges = enableParameterChanges;
		this.isParameterObfuscationEnabled = isParameterObfuscationEnabled;
		this.scriptContext = scriptContext;

		for (int i = 0; i < licenseCheckClassNames.length - 1; i++) {
			try {
				Class<?> licenseCheckClass = Class.forName(licenseCheckClassNames[i]);
				this.licenseCheckObject = licenseCheckClass.newInstance();
				break;
			} catch (Throwable ignored) {
			}
		}
	}

	private void revertInterfaceMappings(List<ClassHierarchyNode> implementationNodes, MethodSignature methodSignature, String reason) {
		Iterator<ClassHierarchyNode> implementationNodeIterator = implementationNodes.iterator();

		while (implementationNodeIterator.hasNext()) {
			AbstractClassNode classNode = implementationNodeIterator.next().getClassNode();
			if (classNode != null) {
				AbstractMethodNode method = classNode.findMethod(methodSignature);
				if (method != null && this.changeLogContext.hasParameterChangeEntry(method)) {
					ExtraParameter[] extraParameters = this.changeLogContext.getExtraParameters(method);
					this.changeLogContext.removeExtraParameters(method);
					if (extraParameters.length > 0) {
						MethodSignature changedSignature = ChangeLogContext.buildChangedMethodSignature(method, extraParameters);
						this.changeLogContext
							.logWarning(
								"Could not change parameter list in method '"
									+ method.getOriginalNameWithParams()
									+ "' to '"
									+ changedSignature.toFullString()
									+ " in interface '"
									+ method.getClassNameBinary()
									+ "' because of '"
									+ reason
									+ "'."
							);
					}
				}
			}
		}
	}

	private void mergeChangeLogMethods(
		ChangeLogContext changeLogContext, Set<MethodNode> methodsToChange, Set<MethodNode> methodsToMerge, ParameterChangerExcludes parameterChangerExcludes, Hierarchy hierarchy
	) {
		if (changeLogContext != null && changeLogContext.hasParameterChangeData()) {
			HashMap<MethodSignature, AbstractClassNode> ownerBySignature = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(methodsToMerge.size()));

			for (MethodNode mergedMethod : methodsToMerge) {
				MethodSignature methodSignature = mergedMethod.getMethodSignature();
				AbstractClassNode interfaceImpl = hierarchy.resolveInterfaceMethodImpl(mergedMethod.getMethodOwner(), methodSignature);
				ownerBySignature.put(methodSignature, (AbstractClassNode)(interfaceImpl != null ? interfaceImpl : mergedMethod.getMethodOwner()));
			}

			for (AbstractMethodNode changedMethodNode : changeLogContext.getChangedParameterMethods()) {
				MethodNode changedMethod = (MethodNode)changedMethodNode;
				AbstractMethodNode baseMethod = hierarchy.getBaseMethodOrDefault(changedMethod);
				if (!ownerBySignature.isEmpty()) {
					ExtraParameter[] extraParameters = changeLogContext.getExtraParameters(baseMethod);
					ParameterPattern parameterPattern = new ParameterPattern(changedMethod.getDescriptor(), extraParameters);
					MethodSignature changedSignature = new MethodSignature(changedMethod.getNameDisplay(), parameterPattern.getDescriptor());
					if (ownerBySignature.get(changedSignature) == baseMethod.getClassNode()) {
						changeLogContext.removeExtraParameters(baseMethod);
						changeLogContext.logWarning(
							"Method '"
								+ changedMethod.getSignature()
								+ "' in class '"
								+ changedMethod.getClassNameBinary()
								+ "' could not be changed to '"
								+ changedSignature
								+ "' because of existing synthetic method. You may need to distribute this application as a whole. (A)"
						);
						continue;
					}
				}

				if (!methodsToChange.contains(changedMethod)) {
					methodsToChange.add(changedMethod);
				}
			}
		}
	}

	private AbstractMethodNode resolveBaseMethodOrSelf(MethodNode methodNode) {
		if (!methodNode.isPrivate() && !methodNode.isStatic() && !methodNode.isConstructor()) {
			Object baseMethod = this.hierarchy.getBaseMethod(methodNode);
			if (baseMethod == null) {
				baseMethod = methodNode;
			}

			return (AbstractMethodNode)baseMethod;
		} else {
			return methodNode;
		}
	}

	public ParameterPattern getParameterPattern(AbstractMethodNode methodNode) {
		return this.parameterPatternByMethod.get(methodNode);
	}

	public Map<MethodNode, ChangedMethod> getChangedMethodMap() {
		return this.changedMethodByMethod;
	}

	private boolean isEligibleExcludedMethod(MethodNode methodNode, ParameterChangerExcludes parameterChangerExcludes, ChangeLogContext changeLogContext, AtomicReference<MethodNode> baseMethodRef) {
		AbstractMethodNode baseMethod = this.resolveBaseMethodOrSelf(methodNode);
		if (!baseMethod.isWorkspaceNode()) {
			return false;
		} else {
			baseMethodRef.set((MethodNode)baseMethod);
			return parameterChangerExcludes.isMethodExcluded((MethodNode)baseMethod) && (changeLogContext == null || changeLogContext.isLooseChangeLog() || !changeLogContext.hasNoParameterChange(baseMethod));
		}
	}

	public Hierarchy getHierarchy() {
		return this.hierarchy;
	}

	private void refreshMethodSignatureMaps() {
		this.oldToNewMethodSignatureMap.clearAll();
		this.newToOldMethodSignatureMap.clearAll();

		for (ClassNode classNode : this.allClassNodes) {
			if (!classNode.isInnerClass()) {
				String internalName = classNode.getInternalName();

				for (MethodNode method : classNode.getMethodNodes()) {
					if (!method.isClinit()) {
						MethodSignature baselineSignature = this.baselineSignatureByMethod.get(method);
						MethodSignature currentSignature = method.getMethodSignature();
						this.oldToNewMethodSignatureMap.put(internalName, baselineSignature, currentSignature);
						this.newToOldMethodSignatureMap.put(internalName, currentSignature, baselineSignature);
					}
				}
			}
		}
	}

	private boolean checkMethodChangeByType(
		ClassHierarchyNode classHierarchyNode, MethodSignature currentSignature, MethodSignature newSignature, AbstractMethodNode methodNode, AtomicReference<AbstractMethodKey> conflictingKeyRef
	) {
		return classHierarchyNode.isInterface()
			? this.hierarchy.checkInterfaceMethodChange(classHierarchyNode, newSignature, currentSignature, conflictingKeyRef)
			: this.hierarchy.checkMethodChange(classHierarchyNode, newSignature, currentSignature, methodNode, conflictingKeyRef);
	}

	private void processClassesForChangeLog() {
		if (this.changeLogContext != null) {
			List<ClassHierarchyNode> classRoots = this.classTreeManager.getAvailableClassRoots();
			int classRootCount = classRoots.size();

			for (int interfaceIndex = 0; interfaceIndex < classRootCount; interfaceIndex++) {
				ClassHierarchyNode interfaceRoot = classRoots.get(interfaceIndex);
				if (interfaceRoot.isInterface()) {
					interfaceRoot.validateOverrideParamChanges(this);
				}
			}

			for (int classIndex = 0; classIndex < classRootCount; classIndex++) {
				ClassHierarchyNode classRoot = classRoots.get(classIndex);
				if (!classRoot.isInterface()) {
					classRoot.validateOverrideParamChanges(this);
				}
			}
		}
	}

	public MethodSignature buildSignatureWithPattern(MethodNode methodNode, ParameterPattern parameterPattern) {
		MethodSignature methodSignature = methodNode.getMethodSignature();
		MethodSignature newSignature;
		if (parameterPattern != null) {
			this.parameterPatternByMethod.put(methodNode, parameterPattern);
			String parameterDescriptor = parameterPattern.getDescriptor();
			newSignature = new MethodSignature(methodSignature.getName(), parameterDescriptor);
		} else {
			newSignature = methodSignature;
		}

		return newSignature;
	}

	public final void validateSignatureChange(ClassHierarchyNode classHierarchyNode, AbstractClassNode classNode, AbstractMethodNode methodNode) {
		String internalName = classNode.getInternalName();
		MethodSignature methodSignature = methodNode.getMethodSignature();
		if (this.changeLogContext.hasParameterChange(methodNode)) {
			ExtraParameter[] extraParameters = this.changeLogContext.getExtraParameters(methodNode);
			MethodSignature changedSignature = ChangeLogContext.buildChangedMethodSignature(methodNode, extraParameters);
			AtomicReference<AbstractMethodKey> conflictingKeyRef = new AtomicReference<>();
			if (this.hierarchy.isConflicting(changedSignature)) {
				if (this.useFullSignatureMatching && !changedSignature.equals(methodSignature) || !this.useFullSignatureMatching && !changedSignature.getParameterKey().equals(methodSignature.getParameterKey())) {
					if (classHierarchyNode.isWorkspaceClass()) {
						this.changeLogContext
							.logWarning(
								"Method '"
									+ methodNode.getSignature()
									+ "' in class '"
									+ classNode.getClassNameBinary()
									+ "' could not have its parameter list changed to '"
									+ changedSignature.toFullString()
									+ "' because of a possible clash. (1)"
							);
					} else {
						this.changeLogContext
							.logSpecialError(
								"Method '"
									+ methodNode.getSignature()
									+ "' in class '"
									+ classNode.getClassNameBinary()
									+ "' could not have its parameter list changed to '"
									+ changedSignature.toFullString()
									+ "' because of a possible clash. (2)"
							);
					}

					MethodSignature oldSignature = methodSignature;
					if (methodNode.isNameChanged()) {
						oldSignature = new MethodSignature(methodNode.getOriginalName(), methodNode.getOriginalDescriptor());
					}

					this.changeLogContext.removeProcessedMethodMapping(internalName, oldSignature);
				}
			} else if (!this.checkMethodChangeByType(classHierarchyNode, methodSignature, changedSignature, methodNode, conflictingKeyRef)) {
				AbstractMethodKey conflictingMethodKey = conflictingKeyRef.get();
				String clashDetail = "";
				if (conflictingMethodKey != null) {
					clashDetail = " with method '" + conflictingMethodKey.getSignature(this.classNameRemap) + "'";
				}

				if (classHierarchyNode.isWorkspaceClass()) {
					this.changeLogContext
						.logWarning(
							"Method '"
								+ methodNode.getSignature()
								+ "' in class '"
								+ classNode.getClassNameBinary()
								+ "' could not have its parameter list changed to '"
								+ changedSignature.toFullString()
								+ "' because of a name clash"
								+ clashDetail
								+ ". (3)"
						);
				} else {
					this.changeLogContext
						.logSpecialError(
							"Method '"
								+ methodNode.getSignature()
								+ "' in class '"
								+ classNode.getClassNameBinary()
								+ "' could not have its parameter list changed to '"
								+ changedSignature.toFullString()
								+ "' because of a possible name clash"
								+ clashDetail
								+ ". (4)"
						);
				}

				MethodSignature previousSignature = methodSignature;
				if (methodNode.isNameChanged()) {
					previousSignature = new MethodSignature(methodNode.getOriginalName(), methodNode.getOriginalDescriptor());
				}

				this.changeLogContext.removeProcessedMethodMapping(internalName, previousSignature);
			}
		}
	}

	public Map<MethodNode, ParameterPattern> applyMethodParameterChanges(
		Set<MethodNode> methodsToChange,
		Set<ClassNode> indyStringClasses,
		Set<ClassNode> indyIntegerClasses,
		Set<ClassNode> indyLongClasses,
		MultiMap<MethodNode, MethodNode> methodReferenceMap,
		Set<MethodNode> flowObfuscatedMethods,
		ExtraParameterBuilder extraParameterBuilder,
		Inheritance inheritance,
		ControlFlowBuilder controlFlowBuilder,
		ParameterChangerBuilder parameterChangerBuilder,
		AtomicReference<ClassNode[]> updatedClassesRef,
		boolean isLargeProject
	) {
		this.captureBaselineSignatures();
		List<KeyValuePair<AbstractMethodNode, ExtraParameter[]>> deferredUpdates = null;
		if (this.changeLogContext != null) {
			if (this.changeLogContext.hasParameterChangeData()) {
				deferredUpdates = this.changeLogContext.getExternalParameterChanges();
			}

			this.processClassesForChangeLog();
		}

		Enumeration<MethodNode> processableMethodsEnumeration = this.parameterChangerExcludes.getProcessableMethods();

		while (processableMethodsEnumeration.hasMoreElements()) {
			MethodNode method = processableMethodsEnumeration.nextElement();
			AbstractMethodNode baseMethod = null;
			if (!method.isPrivate() && !method.isStatic()) {
				baseMethod = this.hierarchy.getBaseMethod(method);
			}

			if (baseMethod != null && baseMethod.isWorkspaceNode()) {
				this.parameterChangerExcludes.excludeMethodDueToOther((MethodNode)baseMethod, method);
				this.parameterChangerExcludes.excludeMethodSilently(method);
			}
		}

		processableMethodsEnumeration = this.parameterChangerExcludes.getProcessableMethods();

		while (processableMethodsEnumeration.hasMoreElements()) {
			MethodNode interfaceMethod = processableMethodsEnumeration.nextElement();
			ClassNode interfaceOwner = interfaceMethod.getMethodOwner();
			MethodSignature methodSignature = interfaceMethod.getMethodSignature();
			ClassHierarchyNode ownerHierarchyNode = ClassHierarchyNode.getNodeByInternalName(interfaceOwner.getInternalName());
			if (interfaceOwner.isInterface()) {
				AtomicReference<List<ClassHierarchyNode>> implementationsRef = this.hierarchy.getMethodImplementationsRef(ownerHierarchyNode, methodSignature);
				if (implementationsRef != null) {
					List<ClassHierarchyNode> implementations = implementationsRef.get();

					for (int implementationIndex = 0; implementationIndex < implementations.size(); implementationIndex++) {
						ClassHierarchyNode implementation = implementations.get(implementationIndex);
						if (implementation.isWorkspaceClass() && implementation != ownerHierarchyNode) {
							MethodNode implementationMethod = this.workspace.getMethodBySignature(implementation.getClassNodeIfWorkspace(), methodSignature);
							if (implementationMethod != null) {
								this.parameterChangerExcludes.excludeMethodDueToOther(implementationMethod, interfaceMethod);
							}
						}
					}
				}
			}
		}

		if (this.changeLogContext != null) {
			if (!this.isLooseChangeLogFile) {
				this.parameterChangerExcludes.applyChangeLogParamChanges(this.changeLogContext, this.hierarchy, this.fixedClassManager);
			} else {
				this.changeLogContext.applyParameterChangeExcludes(this.parameterChangerExcludes, methodsToChange, this.classNameRemap, this.workspace);
			}
		}

		this.mergeChangeLogMethods(this.changeLogContext, methodsToChange, flowObfuscatedMethods, this.parameterChangerExcludes, this.hierarchy);
		ReferenceGraph<AbstractClassNode, MethodNode> referenceGraph = this.buildMethodProcessingGraph(
			methodsToChange, methodReferenceMap, this.parameterChangerExcludes, this.changeLogContext, this.hierarchy, this.scriptContext
		);
		if (referenceGraph.isEmpty() && Options.skipMpcWhenNoCandidates) {
			this.enableParameterChanges.set(false);
			return null;
		} else {
			MultiMap<ClassNode, MethodNode> referencedMethodsByClass = new MultiMap<>();
			MultiMap<ClassNode, MethodNode> unreferencedMethodsByClass = this.splitMethodsByReachability(referencedMethodsByClass, methodReferenceMap, methodsToChange, referenceGraph);
			HashSet<MethodNode> processableMethods = GlobalUtils.newHashSet();
			Enumeration<MethodNode> remainingMethodsEnumeration = this.parameterChangerExcludes.getProcessableMethods();

			while (remainingMethodsEnumeration.hasMoreElements()) {
				MethodNode processableMethod = remainingMethodsEnumeration.nextElement();
				processableMethods.add(processableMethod);
			}

			if (!flowObfuscatedMethods.isEmpty()) {
				this.markConflictingSignatures(flowObfuscatedMethods);
			}

			this.markConflictingSignatures(processableMethods);
			this.markUnreferencedConflicts(referenceGraph, processableMethods);
			this.reconcileChangeLogMappings();
			if (deferredUpdates != null) {
				this.applyDeferredUpdates(deferredUpdates);
			}

			List<ClassHierarchyNode> classRoots = this.classTreeManager.getWorkspaceClassRoots();
			int classRootCount = classRoots.size();

			for (int interfaceIndex = 0; interfaceIndex < classRootCount; interfaceIndex++) {
				ClassHierarchyNode interfaceRoot = classRoots.get(interfaceIndex);
				if (interfaceRoot.isInterface()) {
					ReferenceGraph<AbstractMethodKey, AbstractMethodNode> interfaceAncestorReferenceGraph = new ReferenceGraph<>();
					this.collectAncestorReferences(interfaceRoot.getClassNode(), interfaceAncestorReferenceGraph);
					interfaceRoot.applyMethodParameterChanges(
						this.parameterChangerExcludes,
						this.changeLogContext,
						this,
						extraParameterBuilder,
						referenceGraph,
						this.hierarchy,
						interfaceAncestorReferenceGraph,
						this.keepInnerClassInfoMode,
						this.useFullSignatureMatching,
						this.randomizeProcessingOrder,
						this.classNameRemap,
						this.resourceProtectedStaticMethods,
						this.isParameterObfuscationEnabled
					);
				}
			}

			for (int classIndex = 0; classIndex < classRootCount; classIndex++) {
				ClassHierarchyNode classRoot = classRoots.get(classIndex);
				if (!classRoot.isInterface()) {
					ReferenceGraph<AbstractMethodKey, AbstractMethodNode> classAncestorReferenceGraph = new ReferenceGraph<>();
					this.collectAncestorReferences(classRoot.getClassNode(), classAncestorReferenceGraph);
					classRoot.applyMethodParameterChanges(
						this.parameterChangerExcludes,
						this.changeLogContext,
						this,
						extraParameterBuilder,
						referenceGraph,
						this.hierarchy,
						classAncestorReferenceGraph,
						this.keepInnerClassInfoMode,
						this.useFullSignatureMatching,
						this.randomizeProcessingOrder,
						this.classNameRemap,
						this.resourceProtectedStaticMethods,
						this.isParameterObfuscationEnabled
					);
				}
			}

			if (this.scriptContext.isVerbose()) {
				TreeSet<ClassNode> changedClasses = new TreeSet<>();
				MultiMap<ClassNode, MethodNode> changedMethodsByClass = new MultiMap<>();

				for (MethodNode patternMethod : this.parameterPatternByMethod.keySet()) {
					ClassNode patternMethodOwner = patternMethod.getMethodOwner();
					changedClasses.add(patternMethodOwner);
					changedMethodsByClass.put(patternMethodOwner, patternMethod);
				}

				Comparator<MethodNode> methodComparator = new Comparator<MethodNode>() {
					@Override
					public int compare(MethodNode o1, MethodNode o2) {
						int nameComparison = o1.getOriginalName().toLowerCase().compareTo(o2.getOriginalName().toLowerCase());
						return nameComparison == 0 ? o1.getOriginalDescriptor().toLowerCase().compareTo(o2.getOriginalDescriptor().toLowerCase()) : nameComparison;
					}
				};
				int classCount = 0;
				int methodCount = 0;
				PrintWriter writer = this.scriptContext.getWriter();

				for (ClassNode changedClass : changedClasses) {
					classCount++;
					writer.println("\tChanged parameter lists in class '" + ExcludeStatementManager.getFormattedClassSignature(changedClass, this.workspace, false) + "'");
					List<MethodNode> changedMethods = changedMethodsByClass.get(changedClass);
					Collections.sort(changedMethods, methodComparator);

					for (MethodNode changedMethod : changedMethods) {
						methodCount++;
						String remappedParameters = MethodSignature.formatRemappedParameters(this.parameterPatternByMethod.get(changedMethod).getDescriptor(), this.classNameRemap);
						StringBuilder lineBuilder = new StringBuilder();
						lineBuilder.append("\t\tmethod '" + changedMethod.getAccessFlagsString() + changedMethod.getSignature() + "' => '" + changedMethod.getOriginalName() + remappedParameters + "'");
						if (changedMethod.isNameChanged()) {
							lineBuilder.append(" (");
							lineBuilder.append(changedMethod.getDisplayName());
							lineBuilder.append(')');
						}

						writer.println(lineBuilder.toString());
					}
				}

				if (this.scriptContext.isVerbose()) {
					this.scriptContext
						.getWriter()
						.println(
							"\tMethod Parameter List Changing : Changed parameter lists in "
								+ (isLargeProject ? "" : " ")
								+ methodCount
								+ " method"
								+ (methodCount == 1 ? "" : 's')
								+ " in "
								+ (controlFlowBuilder != null ? "" : " ")
								+ classCount
								+ " class"
								+ (classCount == 1 ? "" : "es")
								+ "."
						);
				}
			}

			this.changedMethodByMethod = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(unreferencedMethodsByClass.size()));
			if (parameterChangerBuilder.isVeryLargeApplication() && (unreferencedMethodsByClass.size() + referencedMethodsByClass.size() > 31 || Options.forceParamLookupMethod) && !Options.dontUseParamLookupMethod && Options.ZKM_METHOD_PARAM_CHANGING_LOOKUP) {
				ReadOnlyMap<String, String> packageRenameMap = this.workspace.getPackageRenameMap();
				ReadOnlyMap<String, String> classNameToInternalNameMap = this.workspace.getClassNameToInternalNameMap();
				ReadOnlyMap<String, String> internalToBinaryNameMap = this.workspace.getInternalToBinaryNameMap();
				parameterChangerBuilder.buildMethodParameterChanges(
					unreferencedMethodsByClass,
					referencedMethodsByClass,
					indyStringClasses,
					indyIntegerClasses,
					indyLongClasses,
					this.defaultMpcClassTripletRef,
					this.archiveToMpcClassTripletMap,
					this.classReferenceGraph,
					this.methodParameterChangesPackage,
					packageRenameMap,
					classNameToInternalNameMap,
					internalToBinaryNameMap,
					this.changedMethodByMethod,
					updatedClassesRef,
					inheritance,
					this.changeLogContext
				);
			}

			HashMap<AbstractClassNode, ChangedMethod> clinitChangedMethodByClass = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.changedMethodByMethod.size()));

			for (Entry<MethodNode, ChangedMethod> changedMethodEntry : this.changedMethodByMethod.entrySet()) {
				MethodNode clinitMethod = changedMethodEntry.getKey();
				if (clinitMethod.isClinit()) {
					clinitChangedMethodByClass.put(clinitMethod.getMethodOwner(), changedMethodEntry.getValue());
				}
			}

			ClassProvider classProvider = this.workspace.getClassProvider();

			for (ClassNode classNode : this.allClassNodes) {
				ArrayList<TypeWriter> staticInitEntries = new ArrayList<>();
				classNode.initClinitChangedMethod(this.changedMethodByMethod, inheritance, this.workspace, classProvider, staticInitEntries, parameterChangerBuilder);
				if (!staticInitEntries.isEmpty()) {
					classNode.appendEntriesToConstantPool(staticInitEntries);
				}
			}

			for (ClassNode obfuscationTargetClass : this.allClassNodes) {
				if ((this.fixedClassManager == null || !this.fixedClassManager.isInFixedClasses(obfuscationTargetClass)) && !obfuscationTargetClass.isGeneratedClass()) {
					ArrayList<TypeWriter> obfuscationEntries = new ArrayList<>();
					obfuscationTargetClass.applyParameterObfuscation(
						this.parameterPatternByMethod,
						this.changedMethodByMethod,
						clinitChangedMethodByClass,
						unreferencedMethodsByClass.get(obfuscationTargetClass),
						this.parameterKeysByMethod,
						methodsToChange,
						inheritance,
						this.decryptionLocalVarByMethod,
						this.typeResolver,
						this.workspace,
						classProvider,
						obfuscationEntries,
						controlFlowBuilder,
						parameterChangerBuilder,
						this.hierarchy,
						this.methodsNeedingClassSeedXor
					);
					if (!obfuscationEntries.isEmpty()) {
						obfuscationTargetClass.appendEntriesToConstantPool(obfuscationEntries);
					}
				}
			}

			this.workspace.resolveRefsAndSyncMetadata(this.allClassNodes, this.scriptContext, true);
			KeyValuePairMultiMap<ClassNode, MethodNode, ParameterPattern> patternsByClassAndMethod = new KeyValuePairMultiMap<>();

			for (Entry<MethodNode, ParameterPattern> patternEntry : this.parameterPatternByMethod.entrySet()) {
				MethodNode annotatedMethod = patternEntry.getKey();
				ParameterPattern parameterPattern = patternEntry.getValue();
				annotatedMethod.updateParameterAnnotations(parameterPattern);
				patternsByClassAndMethod.put(annotatedMethod.getMethodOwner(), annotatedMethod, parameterPattern);
			}

			for (Entry<ClassNode, List<KeyValuePair<MethodNode, ParameterPattern>>> classEntry : patternsByClassAndMethod.entrySet()) {
				ClassNode patternClass = classEntry.getKey();
				ConstantPoolManager constantPoolManager = patternClass.getConstantPoolManager();
				ArrayList<TypeWriter> attributeEntries = new ArrayList<>();

				for (KeyValuePair<MethodNode, ParameterPattern> methodPatternPair : classEntry.getValue()) {
					methodPatternPair.getKey().updateMethodParameters(methodPatternPair.getValue(), constantPoolManager, attributeEntries);
				}

				if (!attributeEntries.isEmpty()) {
					patternClass.appendEntriesToConstantPool(attributeEntries);
				}
			}

			this.workspace.rebuildMethodRegistries();
			this.refreshMethodSignatureMaps();
			return this.parameterPatternByMethod;
		}
	}

	private void collectAncestorReferences(AbstractClassNode classNode, ReferenceGraph<AbstractMethodKey, AbstractMethodNode> referenceGraph) {
		HashSet<AbstractClassNode> visitedClasses = GlobalUtils.newHashSet();
		Obfuscator.collectInheritedMethods(
			classNode, referenceGraph, this.useFullSignatureMatching, visitedClasses, this.workspace.getClassProvider(), "Method Parameter List Changing"
		);
	}

	public final void validateOverrideMapping(ClassHierarchyNode classHierarchyNode, AbstractClassNode classNode, AbstractMethodNode methodNode) {
		if (this.changeLogContext.hasParameterChangeEntry(methodNode)) {
			ExtraParameter[] extraParameters = this.changeLogContext.getExtraParameters(methodNode);
			if (!methodNode.isStatic() && !methodNode.isPrivate()) {
				AbstractClassNode implementationClass = this.hierarchy.getMethodImplementationClass(methodNode);
				if (implementationClass != null) {
					AbstractMethodNode implementationMethod = implementationClass.findMethod(methodNode.getMethodSignature());
					ClassHierarchyNode implementationHierarchyNode = ClassHierarchyNode.getNodeByInternalName(implementationClass.getInternalName());
					if (implementationHierarchyNode != null && !implementationHierarchyNode.isMissing()) {
						ExtraParameter[] propagatedParameters = this.changeLogContext.propagateParameterChange(methodNode, implementationMethod, extraParameters);
						if (extraParameters != null && propagatedParameters != null ? !GlobalUtils.areEquals(extraParameters, propagatedParameters) : extraParameters != propagatedParameters) {
						}
					} else if (extraParameters.length > 0) {
						MethodSignature implementationSignature = implementationMethod.getMethodSignature();
						MethodSignature changedSignature = ChangeLogContext.buildChangedMethodSignature(methodNode, extraParameters);
						if (!implementationSignature.equals(changedSignature)) {
							this.changeLogContext
								.logWarning(
									"Method '"
										+ methodNode.getOriginalNameWithParams()
										+ "' in class '"
										+ methodNode.getClassNameBinary()
										+ "' could not have its parameter list changed to '"
										+ changedSignature.toFullString()
										+ "' because it now overrides a method in '"
										+ implementationClass.getClassNameBinary()
										+ "' which has not been opened."
								);
							this.changeLogContext.removeExtraParameters(methodNode);
						}
					}
				}
			}
		}
	}

	private void reconcileChangeLogMappings() {
		if (this.changeLogContext != null) {
			for (Entry<AtomicReference<List<ClassHierarchyNode>>, MethodSignature> implementationRefEntry : this.hierarchy.getAllImplementationRefs().entrySet()) {
				this.reconcileInterfaceMappings(implementationRefEntry.getKey(), implementationRefEntry.getValue());
			}

			List<ClassHierarchyNode> classRoots = this.classTreeManager.getAvailableClassRoots();
			int classRootCount = classRoots.size();

			for (int interfaceIndex = 0; interfaceIndex < classRootCount; interfaceIndex++) {
				ClassHierarchyNode interfaceRoot = classRoots.get(interfaceIndex);
				if (interfaceRoot.isInterface()) {
					interfaceRoot.validateParamChangeRequests(this);
				}
			}

			for (int classIndex = 0; classIndex < classRootCount; classIndex++) {
				ClassHierarchyNode classRoot = classRoots.get(classIndex);
				if (!classRoot.isInterface()) {
					classRoot.validateParamChangeRequests(this);
				}
			}
		}
	}

	private void captureBaselineSignatures() {
		for (ClassNode classNode : this.allClassNodes) {
			if (!classNode.isInnerClass()) {
				String internalName = classNode.getInternalName();

				for (MethodNode method : classNode.getMethodNodes()) {
					MethodSignature currentSignature = method.getMethodSignature();
					MethodSignature baselineSignature = GlobalUtils.getFromDualKeyMapOrDefault(internalName, currentSignature, this.newToOldMethodSignatureMap);
					this.baselineSignatureByMethod.put(method, baselineSignature);
				}
			}
		}
	}

	private void markUnreferencedConflicts(ReferenceGraph<AbstractClassNode, MethodNode> referenceGraph, Set<MethodNode> processableMethods) {
		HashSet<MethodNode> referencedMethods = GlobalUtils.newHashSet();
		Iterator<Entry<AbstractClassNode, Set<MethodNode>>> adjacencyEntryIterator = referenceGraph.getAdjacencyEntries().iterator();

		while (adjacencyEntryIterator.hasNext()) {
			for (MethodNode referencedMethod : adjacencyEntryIterator.next().getValue()) {
				referencedMethods.add(referencedMethod);
			}
		}

		HashSet<MethodNode> unreferencedMethods = GlobalUtils.newHashSet();
		Enumeration<MethodNode> allMethodsEnumeration = this.parameterChangerExcludes.getAllMethods();

		while (allMethodsEnumeration.hasMoreElements()) {
			MethodNode method = allMethodsEnumeration.nextElement();
			if (!referencedMethods.contains(method)) {
				unreferencedMethods.add(method);
			}
		}

		Set<MethodNode> implementationMethods = this.resolveImplementationMethods(unreferencedMethods);
		implementationMethods.removeAll(processableMethods);
		this.markConflictingSignatures(implementationMethods);
	}

	private Set<MethodNode> resolveImplementationMethods(Collection<MethodNode> methods) {
		HashSet<MethodNode> implementationMethods = GlobalUtils.newHashSet();

		for (MethodNode method : methods) {
			MethodSignature methodSignature = method.getMethodSignature();
			AbstractClassNode implementationClass = null;
			if (!method.isPrivate() && !method.isStatic()) {
				implementationClass = this.hierarchy.getMethodImplementationClass(method);
			}

			if (implementationClass != null) {
				ClassHierarchyNode hierarchyNode = ClassHierarchyNode.getNodeByInternalName(implementationClass.getInternalName());
				if (hierarchyNode != null && hierarchyNode.isWorkspaceClass()) {
					ClassNode classNode = hierarchyNode.getClassNodeIfWorkspace();
					MethodNode implementationMethod = this.workspace.getMethodBySignature(classNode, methodSignature);
					implementationMethods.add(implementationMethod);
				}
			} else {
				implementationMethods.add(method);
			}
		}

		return implementationMethods;
	}

	private ReferenceGraph<AbstractClassNode, MethodNode> buildMethodProcessingGraph(
		Set<MethodNode> methodsToChange, MultiMap<MethodNode, MethodNode> methodReferenceMap, ParameterChangerExcludes parameterChangerExcludes, ChangeLogContext changeLogContext, Hierarchy hierarchy, ScriptContext scriptContext
	) {
		HashSet<MethodNode> changedMethods = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(methodsToChange.size()));
		AtomicReference<MethodNode> baseMethodRef = new AtomicReference<>();

		for (MethodNode candidateMethod : methodsToChange) {
			if (this.isEligibleExcludedMethod(candidateMethod, parameterChangerExcludes, changeLogContext, baseMethodRef)) {
				MethodNode baseMethod = baseMethodRef.get();
				if (changedMethods.add(baseMethod)) {
					Set<AbstractMethodNode> overrides = hierarchy.getMethodAndOverrides(baseMethod);
					if (overrides != null) {
						for (AbstractMethodNode override : overrides) {
							changedMethods.add((MethodNode)override);
						}
					}
				}
			}
		}

		RingQueue<MethodNode> methodQueue = new RingQueue<>(Math.max((int)(changedMethods.size() * 1.5), 5));
		methodQueue.addAll(changedMethods);

		while (!methodQueue.isEmpty()) {
			MethodNode queuedMethod = methodQueue.pop();
			List<MethodNode> referencingMethods = methodReferenceMap.get(queuedMethod);
			if (referencingMethods != null) {
				for (MethodNode referencingMethod : referencingMethods) {
					if (!changedMethods.contains(referencingMethod) && this.isEligibleExcludedMethod(referencingMethod, parameterChangerExcludes, changeLogContext, baseMethodRef)) {
						MethodNode referencedBaseMethod = baseMethodRef.get();
						if (changedMethods.add(referencedBaseMethod)) {
							methodQueue.push(referencedBaseMethod);
							Set<AbstractMethodNode> referencedOverrides = hierarchy.getMethodAndOverrides(referencedBaseMethod);
							if (referencedOverrides != null) {
								for (AbstractMethodNode referencedOverride : referencedOverrides) {
									if (changedMethods.add((MethodNode)referencedOverride)) {
										methodQueue.push((MethodNode)referencedOverride);
									}
								}
							}
						}
					}
				}
			}
		}

		ReferenceGraph<AbstractClassNode, MethodNode> referenceGraph = new ReferenceGraph<>(GlobalUtils.getInitialCapacity(this.allClassNodes.length));

		for (MethodNode changedMethod : changedMethods) {
			if (changedMethod.isLambdaMetaFactoryMarked()) {
				AbstractMethodNode lambdaBaseMethod = hierarchy.getBaseMethod(changedMethod);
				if (lambdaBaseMethod != null) {
					if (lambdaBaseMethod != changedMethod && lambdaBaseMethod.isWorkspaceNode()) {
						lambdaBaseMethod.setLambdaSamParamCount(changedMethod.getLambdaSamParamCount());
					}

					ClassHierarchyNode hierarchyNode = ClassHierarchyNode.getNodeByInternalName(lambdaBaseMethod.getClassNode().getInternalName());
					if (hierarchyNode != null) {
						MethodSignature methodSignature = lambdaBaseMethod.getMethodSignature();
						AtomicReference<List<ClassHierarchyNode>> implementationsRef = hierarchy.getMethodImplementationsRef(hierarchyNode, methodSignature);
						if (implementationsRef != null) {
							Iterator<ClassHierarchyNode> implementationIterator = implementationsRef.get().iterator();

							while (implementationIterator.hasNext()) {
								MethodNode implementationMethod = implementationIterator.next().getClassNodeIfWorkspace().findMethodNode(methodSignature);
								if (implementationMethod != null) {
									implementationMethod.setLambdaSamParamCount(changedMethod.getLambdaSamParamCount());
								}
							}
						}
					}
				}
			}

			referenceGraph.addReference(changedMethod.getMethodOwner(), changedMethod);
		}

		return referenceGraph;
	}

	private void applyDeferredUpdates(List<KeyValuePair<AbstractMethodNode, ExtraParameter[]>> deferredUpdates) {
		for (KeyValuePair<AbstractMethodNode, ExtraParameter[]> updatePair : deferredUpdates) {
			AbstractMethodNode method = updatePair.getKey();
			MethodSignature changedSignature = ChangeLogContext.buildChangedMethodSignature(method, updatePair.getValue());
			method.setDescriptor(changedSignature.getFullDescriptor());
			method.setParameterDescriptorChanged(true);
		}
	}

	private MultiMap<ClassNode, MethodNode> splitMethodsByReachability(
		MultiMap<ClassNode, MethodNode> unreferencedMethodsByClass, MultiMap<MethodNode, MethodNode> methodReferenceMap, Set<MethodNode> methodsToChange, ReferenceGraph<AbstractClassNode, MethodNode> referenceGraph
	) {
		MultiMap<ClassNode, MethodNode> unreferencedMethodsByOwner = new MultiMap<>();
		ReferenceGraph<MethodNode, MethodNode> methodReferenceGraph = new ReferenceGraph<>();

		for (Entry<MethodNode, List<MethodNode>> referenceEntry : methodReferenceMap.entrySet()) {
			MethodNode referencedMethod = referenceEntry.getKey();

			for (MethodNode callerMethod : referenceEntry.getValue()) {
				methodReferenceGraph.addReference(callerMethod, referencedMethod);
			}
		}

		for (MethodNode method : this.parameterKeysByMethod.keySet()) {
			ClassNode methodOwner = method.getMethodOwner();
			if (!referenceGraph.isReferencedBy(methodOwner, method)) {
				if (methodsToChange.contains(method)) {
					unreferencedMethodsByOwner.put(methodOwner, method);
				} else {
					Set<MethodNode> referencingMethods = methodReferenceGraph.getReferences(method);
					if (referencingMethods != null) {
						for (MethodNode referencingMethod : referencingMethods) {
							if (referenceGraph.isReferencedBy(referencingMethod.getMethodOwner(), referencingMethod)) {
								unreferencedMethodsByOwner.put(methodOwner, method);
								break;
							}
						}
					}
				}
			} else if (ParameterChangerBuilder.canChangeParameters(method, this.hierarchy) && !Options.dontChangeReferencedMethods) {
				unreferencedMethodsByClass.put(methodOwner, method);
			}
		}

		return unreferencedMethodsByOwner;
	}

	private void reconcileInterfaceMappings(AtomicReference<List<ClassHierarchyNode>> implementationsRef, MethodSignature methodSignature) {
		List<ClassHierarchyNode> implementationNodes = implementationsRef.get();
		AbstractMethodNode changedMethod = null;
		AbstractMethodNode methodWithoutExtraParameters = null;
		ExtraParameter[] extraParameters = null;
		MethodSignature changedSignature = null;
		Iterator<ClassHierarchyNode> classNodeIterator = implementationNodes.iterator();

		while (classNodeIterator.hasNext()) {
			AbstractClassNode classNode = classNodeIterator.next().getClassNode();
			if (classNode != null) {
				AbstractMethodNode method = classNode.findMethod(methodSignature);
				if (method != null && this.changeLogContext.hasParameterChangeEntry(method)) {
					extraParameters = this.changeLogContext.getExtraParameters(method);
					if (extraParameters.length > 0) {
						changedSignature = ChangeLogContext.buildChangedMethodSignature(method, extraParameters);
						changedMethod = method;
					} else {
						methodWithoutExtraParameters = method;
					}
					break;
				}
			}
		}

		classNodeIterator = implementationNodes.iterator();

		while (classNodeIterator.hasNext()) {
			AbstractClassNode otherClassNode = classNodeIterator.next().getClassNode();
			if (otherClassNode != null) {
				AbstractMethodNode otherMethod = otherClassNode.findMethod(methodSignature);
				if (otherMethod != null && this.changeLogContext.hasParameterChangeEntry(otherMethod)) {
					ExtraParameter[] otherExtraParameters = this.changeLogContext.getExtraParameters(otherMethod);
					if (otherExtraParameters.length > 0) {
						if (methodWithoutExtraParameters != null) {
							this.revertInterfaceMappings(
								implementationNodes, methodSignature, "clashing mappings in between related interfaces (1) : " + methodWithoutExtraParameters.getOriginalNameWithParams() + " : " + otherMethod.getOriginalNameWithParams()
							);
							changedSignature = null;
							break;
						}

						if (!GlobalUtils.areEquals(extraParameters, otherExtraParameters)) {
							this.revertInterfaceMappings(
								implementationNodes, methodSignature, "clashing mappings in between related interfaces (2) : " + changedMethod.getOriginalNameWithParams() + " : " + otherMethod.getOriginalNameWithParams()
							);
							changedSignature = null;
							break;
						}
					}
				}
			}
		}

		if (changedSignature != null) {
			AtomicReference<AbstractMethodKey> conflictingKeyRef = new AtomicReference<>();
			ParameterPattern parameterPattern = new ParameterPattern(changedMethod.getDescriptor(), extraParameters);
			if (!this.hierarchy.checkMethodChangeForGroup(implementationsRef, changedSignature, methodSignature, conflictingKeyRef, parameterPattern)) {
				this.revertInterfaceMappings(implementationNodes, methodSignature, "clashing mappings in between related interface sets");
			}
		}
	}

	private void markConflictingSignatures(Set<MethodNode> methods) {
		for (MethodNode method : methods) {
			MethodSignature methodSignature = method.getMethodSignature();
			AbstractClassNode methodOwner = (AbstractClassNode)method.getParent();
			if (methodOwner.isInterface() && !method.isClinit() && !this.hierarchy.isConflicting(methodSignature)) {
				ClassHierarchyNode ownerHierarchyNode = ClassHierarchyNode.getNodeByInternalName(methodOwner.getInternalName());
				AtomicReference<AbstractMethodKey> conflictingKeyRef = new AtomicReference<>();
				if (!this.hierarchy.checkInterfaceMethodChange(ownerHierarchyNode, methodSignature, methodSignature, conflictingKeyRef)) {
					this.hierarchy.addConflictingSignature(methodSignature);
				}
			}
		}

		for (MethodNode otherMethod : methods) {
			MethodSignature otherSignature = otherMethod.getMethodSignature();
			AbstractClassNode otherMethodOwner = (AbstractClassNode)otherMethod.getParent();
			if (!otherMethodOwner.isInterface() && !otherMethod.isClinit() && !this.hierarchy.isConflicting(otherSignature)) {
				ClassHierarchyNode otherOwnerHierarchyNode = ClassHierarchyNode.getNodeByInternalName(otherMethodOwner.getInternalName());
				AtomicReference<AbstractMethodKey> otherConflictingKeyRef = new AtomicReference<>();
				boolean changeAllowed;
				if (otherMethod.isConstructor()) {
					changeAllowed = this.hierarchy.checkConstructorConflict(otherOwnerHierarchyNode, otherSignature, otherSignature, otherMethod, otherConflictingKeyRef);
				} else {
					changeAllowed = this.hierarchy.checkMethodChange(otherOwnerHierarchyNode, otherSignature, otherSignature, otherMethod, otherConflictingKeyRef);
				}

				if (!changeAllowed) {
					this.hierarchy.addConflictingSignature(otherSignature);
				}
			}
		}
	}
}
