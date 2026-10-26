package com.zelix.tools.renamer;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.code.MethodMapping;
import com.zelix.framework.signature.AbstractMethodKey;
import com.zelix.framework.signature.FlaggedMethodSignature;
import com.zelix.framework.signature.MethodParameterKey;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.LibraryClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.repository.Hierarchy;
import com.zelix.tools.excludes.DefaultExcludes;
import com.zelix.tools.excludes.FixedClassManager;
import com.zelix.tools.parameterChanger.ParameterChangerExcludes;
import com.zelix.tools.renamer.method.AbstractMethodNameGenerator;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.KeyValuePair;
import com.zelix.utils.collections.MethodNameMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.ClassTreeManager;
import com.zelix.workspace.Workspace;

import java.util.*;
import java.util.Map.Entry;

public class MethodRenamer {
	private final MethodMapping methodMapping = new MethodMapping();
	private Hierarchy hierarchy;
	private final ClassTreeManager classTreeManager;
	private final ClassNode[] classNodes;
	private final AbstractClassNode[] processedClasses;
	private final Workspace workspace;
	private final int keepInnerClassInfo;
	private boolean aggressiveMethodRenaming;
	private boolean uniqueMethodNames;
	private boolean randomizeObfuscation;
	private final DefaultExcludes defaultExcludes;
	private final ParameterChangerExcludes parameterChangerExcludes;
	private final FixedClassManager fixedClassManager;
	private final ChangeLogContext changeLogContext;
	private final boolean isLooseChangeLogFile;
	private final HashMap<String, String> classNameMappings;
	private final DualKeyMap<String, MethodSignature, MethodSignature> renamedSignatures;
	private final DualKeyMap<String, MethodSignature, MethodSignature> originalSignatures;
	private Map<MethodNode, String> resourceProtectedMethods;
	private ScriptContext scriptContext;

	public MethodRenamer(
		DefaultExcludes defaultExcludes,
		ParameterChangerExcludes parameterChangerExcludes,
		FixedClassManager fixedClassManager,
		ChangeLogContext changeLogContext,
		boolean isLooseChangeLogFile,
		Workspace workspace,
		ClassNode[] classNodes,
		AbstractClassNode[] processedClasses,
		ClassTreeManager classTreeManager,
		ClassProvider classProvider,
		int keepInnerClassInfo,
		boolean aggressiveMethodRenaming,
		boolean uniqueMethodNames,
		boolean randomizeObfuscation,
		boolean isLicensed,
		HashMap<String, String> classNameMappings,
		DualKeyMap<String, MethodSignature, MethodSignature> renamedSignatures,
		DualKeyMap<String, MethodSignature, MethodSignature> originalSignatures,
		Map<MethodNode, String> resourceProtectedStaticMethods,
		AtomicReference<Map<AbstractMethodNode, AbstractMethodNode>> baseMethodMapRef,
		ScriptContext scriptContext
	) {
		this.hierarchy = new Hierarchy(workspace, classTreeManager, classProvider, scriptContext, classNodes.length, aggressiveMethodRenaming, isLicensed, false);
		baseMethodMapRef.set(this.hierarchy.getBaseMethodMap());
		this.classTreeManager = classTreeManager;
		this.classNodes = classNodes;
		this.processedClasses = processedClasses;
		this.workspace = workspace;
		this.keepInnerClassInfo = keepInnerClassInfo;
		this.aggressiveMethodRenaming = aggressiveMethodRenaming;
		this.uniqueMethodNames = uniqueMethodNames;
		this.randomizeObfuscation = randomizeObfuscation;
		this.defaultExcludes = defaultExcludes;
		this.parameterChangerExcludes = parameterChangerExcludes;
		this.fixedClassManager = fixedClassManager;
		this.changeLogContext = changeLogContext;
		this.isLooseChangeLogFile = isLooseChangeLogFile;
		this.classNameMappings = classNameMappings;
		this.renamedSignatures = renamedSignatures;
		this.originalSignatures = originalSignatures;
		this.resourceProtectedMethods = resourceProtectedStaticMethods;
		this.scriptContext = scriptContext;
	}

	public ScriptContext getScriptContext() {
		return this.scriptContext;
	}

	private void addConflictingSignatures(Set<MethodNode> methods) {
		for (MethodNode interfaceMethod : methods) {
			MethodSignature interfaceSignature = interfaceMethod.getMethodSignature();
			AbstractClassNode interfaceOwner = (AbstractClassNode)interfaceMethod.getParent();
			if (interfaceOwner.isInterface() && !this.hierarchy.isConflicting(interfaceSignature)) {
				String interfaceInternalName = interfaceOwner.getInternalName();
				String interfaceMethodName = interfaceSignature.getName();
				if (this.isObfuscatedName(interfaceMethodName)) {
					ClassHierarchyNode interfaceHierarchyNode = ClassHierarchyNode.getNodeByInternalName(interfaceInternalName);
					AtomicReference<AbstractMethodKey> interfaceClashingMethodRef = new AtomicReference<>();
					if (!this.hierarchy.checkInterfaceMethodChange(interfaceHierarchyNode, interfaceSignature, interfaceSignature, interfaceClashingMethodRef)) {
						this.hierarchy.addConflictingSignature(interfaceSignature);
					}
				} else {
					this.hierarchy.addConflictingSignature(interfaceSignature);
				}
			}
		}

		for (MethodNode classMethod : methods) {
			MethodSignature classSignature = classMethod.getMethodSignature();
			AbstractClassNode classOwner = (AbstractClassNode)classMethod.getParent();
			if (!classOwner.isInterface() && !this.hierarchy.isConflicting(classSignature)) {
				String classInternalName = classOwner.getInternalName();
				String classMethodName = classSignature.getName();
				if (this.isObfuscatedName(classMethodName)) {
					ClassHierarchyNode classHierarchyNode = ClassHierarchyNode.getNodeByInternalName(classInternalName);
					AtomicReference<AbstractMethodKey> classClashingMethodRef = new AtomicReference<>();
					if (!this.hierarchy.checkMethodChange(classHierarchyNode, classSignature, classSignature, classMethod, classClashingMethodRef)) {
						this.hierarchy.addConflictingSignature(classSignature);
					}
				} else {
					this.hierarchy.addConflictingSignature(classSignature);
				}
			}
		}
	}

	private boolean checkMethodChange(
		ClassHierarchyNode hierarchyNode, MethodSignature signature, MethodSignature newSignature, AbstractMethodNode methodNode, AtomicReference<AbstractMethodKey> clashingMethodRef
	) {
		return hierarchyNode.isInterface() ? this.hierarchy.checkInterfaceMethodChange(hierarchyNode, newSignature, signature, clashingMethodRef) : this.hierarchy.checkMethodChange(hierarchyNode, newSignature, signature, methodNode, clashingMethodRef);
	}

	public MethodSignature registerMethodRename(MethodNode method, String newName, AtomicReference<MethodSignature> objectArraySignatureRef) {
		objectArraySignatureRef.setNull();
		String classInternalName = method.getInternalName();
		MethodSignature originalSignature = method.getMethodSignature();
		MethodSignature renamedSignature;
		if (newName != null) {
			renamedSignature = new MethodSignature(newName, originalSignature.getFullDescriptor());
			this.methodMapping.putNewPair(method, originalSignature.getName(), newName);
			if (method.isParameterObfuscationMarked()) {
				MethodSignature objectArraySignature = new MethodSignature(newName, ParameterChangerExcludes.toObjectArrayDescriptor(method));
				objectArraySignatureRef.set(objectArraySignature);
			}
		} else {
			renamedSignature = originalSignature;
			this.methodMapping.putNewPair(method, method.getNameDisplay(), method.getNameDisplay());
		}

		MethodSignature discardedPreviousRenamedSignature = this.renamedSignatures.put(classInternalName, originalSignature, renamedSignature);
		MethodSignature previousOriginalSignature = this.originalSignatures.put(classInternalName, renamedSignature, originalSignature);
		if (previousOriginalSignature != null) {
			DebugUtils.assertTrue(
				false,
				new String[]{
					"Duplicate method name in class "
						+ GlobalUtils.internalToBinaryName(GlobalUtils.getMappedOrDefault(classInternalName, this.classNameMappings))
						+ ". Both '"
						+ originalSignature.toSignatureString(this.classNameMappings)
						+ "' and '"
						+ previousOriginalSignature.toSignatureString(this.classNameMappings)
						+ "' renamed to '"
						+ renamedSignature.getName()
						+ "'."
				}
			);
		}

		return renamedSignature;
	}

	private void applyLibraryMethodRenames(DualKeyMap<String, MethodSignature, MethodSignature> libraryMethodMappings) {
		if (this.changeLogContext != null) {
			Enumeration<String> classKeysEnumeration = libraryMethodMappings.getGroupKeys();

			while (classKeysEnumeration.hasMoreElements()) {
				String classInternalName = classKeysEnumeration.nextElement();

				for (Entry<MethodSignature, MethodSignature> signatureMapping : libraryMethodMappings.getGroup(classInternalName).entrySet()) {
					this.renamedSignatures.put(classInternalName, signatureMapping.getKey(), signatureMapping.getValue());
					this.originalSignatures.put(classInternalName, signatureMapping.getValue(), signatureMapping.getKey());
				}

				((LibraryClassNode)ClassHierarchyNode.getClassNodeByInternalName(classInternalName)).applyMappedMethodNames(this.renamedSignatures, this.methodMapping);
			}
		}
	}

	public Hierarchy getHierarchy() {
		return this.hierarchy;
	}

	private void validateRenameOverrides() {
		if (this.changeLogContext != null) {
			List<ClassHierarchyNode> classRoots = this.classTreeManager.getAvailableClassRoots();
			int classRootCount = classRoots.size();

			for (int interfacePassIndex = 0; interfacePassIndex < classRootCount; interfacePassIndex++) {
				ClassHierarchyNode interfaceRoot = classRoots.get(interfacePassIndex);
				if (interfaceRoot.isInterface()) {
					interfaceRoot.validateRenameOverrides(this);
				}
			}

			for (int classPassIndex = 0; classPassIndex < classRootCount; classPassIndex++) {
				ClassHierarchyNode classRoot = classRoots.get(classPassIndex);
				if (!classRoot.isInterface()) {
					classRoot.validateRenameOverrides(this);
				}
			}
		}
	}

	private boolean isObfuscatedName(String methodName) {
		return methodName.length() <= 2;
	}

	public String getMappedMethodName(String classInternalName, MethodSignature signature) {
		MethodSignature renamedSignature = GlobalUtils.getFromDualKeyMapOrDefault(classInternalName, signature, this.renamedSignatures);
		return renamedSignature != null ? renamedSignature.getName() : null;
	}

	public void renameMethods(AbstractMethodNameGenerator nameGenerator) {
		DualKeyMap<String, MethodSignature, MethodSignature> libraryMethodMappings = null;
		if (this.changeLogContext != null) {
			libraryMethodMappings = this.buildLibraryMethodMappings();
			this.validateRenameOverrides();
		}

		HashSet<MethodNode> conflictCandidateMethods = GlobalUtils.newHashSet();
		Enumeration<MethodNode> excludedMethodsEnumeration = this.defaultExcludes.getExcludedMethods();

		while (excludedMethodsEnumeration.hasMoreElements()) {
			MethodNode excludedMethod = excludedMethodsEnumeration.nextElement();
			AbstractMethodNode excludedBaseMethod = null;
			if (!excludedMethod.isPrivate() && !excludedMethod.isStatic()) {
				excludedBaseMethod = this.hierarchy.getBaseMethod(excludedMethod);
			}

			if (excludedBaseMethod != null) {
				if (excludedBaseMethod.isWorkspaceNode()) {
					this.defaultExcludes.excludeMethodDueToOther((MethodNode)excludedBaseMethod, excludedMethod);
					this.defaultExcludes.unexcludeMethodSilently(excludedMethod);
					conflictCandidateMethods.add((MethodNode)excludedBaseMethod);
				}
			} else {
				conflictCandidateMethods.add(excludedMethod);
			}
		}

		if (this.parameterChangerExcludes != null) {
			Enumeration<MethodNode> processableMethodsEnumeration = this.parameterChangerExcludes.getProcessableMethods();

			while (processableMethodsEnumeration.hasMoreElements()) {
				MethodNode processableMethod = processableMethodsEnumeration.nextElement();
				AbstractMethodNode processableBaseMethod = null;
				if (!processableMethod.isPrivate() && !processableMethod.isStatic()) {
					processableBaseMethod = this.hierarchy.getBaseMethod(processableMethod);
				}

				if (processableBaseMethod != null && processableBaseMethod.isWorkspaceNode()) {
					this.parameterChangerExcludes.excludeMethodDueToOther((MethodNode)processableBaseMethod, processableMethod);
					this.parameterChangerExcludes.excludeMethodSilently(processableMethod);
				}
			}

			processableMethodsEnumeration = this.parameterChangerExcludes.getProcessableMethods();

			while (processableMethodsEnumeration.hasMoreElements()) {
				MethodNode processableInterfaceMethod = processableMethodsEnumeration.nextElement();
				ClassNode processableOwnerClass = processableInterfaceMethod.getMethodOwner();
				MethodSignature processableSignature = processableInterfaceMethod.getMethodSignature();
				ClassHierarchyNode processableOwnerNode = ClassHierarchyNode.getNodeByInternalName(processableOwnerClass.getInternalName());
				if (processableOwnerClass.isInterface()) {
					AtomicReference<List<ClassHierarchyNode>> processableImplementationsRef = this.hierarchy.getMethodImplementationsRef(processableOwnerNode, processableSignature);
					if (processableImplementationsRef != null) {
						List<ClassHierarchyNode> processableImplementations = processableImplementationsRef.get();

						for (int implementationIndex = 0; implementationIndex < processableImplementations.size(); implementationIndex++) {
							ClassHierarchyNode implementationNode = processableImplementations.get(implementationIndex);
							if (implementationNode.isWorkspaceClass() && implementationNode != processableOwnerNode) {
								MethodNode implementationMethod = this.workspace.getMethodBySignature(implementationNode.getClassNodeIfWorkspace(), processableSignature);
								if (implementationMethod != null) {
									this.parameterChangerExcludes.excludeMethodDueToOther(implementationMethod, processableInterfaceMethod);
								}
							}
						}
					}
				}
			}
		}

		this.defaultExcludes.buildMethodGroups(this.hierarchy, this.classNameMappings);
		excludedMethodsEnumeration = this.defaultExcludes.getExcludedMethods();

		while (excludedMethodsEnumeration.hasMoreElements()) {
			MethodNode excludedInterfaceMethod = excludedMethodsEnumeration.nextElement();
			ClassNode excludedOwnerClass = excludedInterfaceMethod.getMethodOwner();
			MethodSignature excludedSignature = excludedInterfaceMethod.getMethodSignature();
			ClassHierarchyNode excludedOwnerNode = ClassHierarchyNode.getNodeByInternalName(excludedOwnerClass.getInternalName());
			if (excludedOwnerClass.isInterface()) {
				AtomicReference<List<ClassHierarchyNode>> excludedImplementationsRef = this.hierarchy.getMethodImplementationsRef(excludedOwnerNode, excludedSignature);
				if (excludedImplementationsRef != null) {
					List<ClassHierarchyNode> excludedImplementations = excludedImplementationsRef.get();

					for (int excludedImplementationIndex = 0; excludedImplementationIndex < excludedImplementations.size(); excludedImplementationIndex++) {
						ClassHierarchyNode excludedImplementationNode = excludedImplementations.get(excludedImplementationIndex);
						if (excludedImplementationNode.isWorkspaceClass() && excludedImplementationNode != excludedOwnerNode) {
							MethodNode excludedImplementationMethod = this.workspace.getMethodBySignature(excludedImplementationNode.getClassNodeIfWorkspace(), excludedSignature);
							if (excludedImplementationMethod != null) {
								this.defaultExcludes.excludeMethodDueToOther(excludedImplementationMethod, excludedInterfaceMethod);
							}
						}
					}
				}
			}
		}

		if (this.changeLogContext != null) {
			if (!this.isLooseChangeLogFile) {
				this.defaultExcludes.processMethodChangeLog(this.changeLogContext, this.hierarchy, this.classNameMappings, this.fixedClassManager);
				if (this.parameterChangerExcludes != null) {
					this.parameterChangerExcludes.applyChangeLogParamObfuscation(this.changeLogContext, this.hierarchy, this.defaultExcludes, this.fixedClassManager);
				}
			} else {
				this.changeLogContext.applyMethodExcludes(this.defaultExcludes, this.classNameMappings, this.workspace);
				if (this.parameterChangerExcludes != null) {
					this.changeLogContext.applyParameterObfuscationExcludes(this.parameterChangerExcludes, this.hierarchy);
				}
			}
		}

		this.addConflictingSignatures(conflictCandidateMethods);
		this.validateMethodRenames();
		if (this.changeLogContext != null) {
			this.applyLibraryMethodRenames(libraryMethodMappings);
		}

		HashSet<String> reservedFieldNames = GlobalUtils.newHashSet();
		List<ClassHierarchyNode> classRoots = this.classTreeManager.getAvailableClassRoots();
		int classRootCount = classRoots.size();
		MethodNameMap<String, AbstractMethodNode> sharedInterfaceMethodNames;
		if (this.uniqueMethodNames) {
			sharedInterfaceMethodNames = new MethodNameMap<>(false);
		} else {
			sharedInterfaceMethodNames = new MethodNameMap<>(true);
		}

		for (int interfacePassIndex = 0; interfacePassIndex < classRootCount; interfacePassIndex++) {
			ClassHierarchyNode interfaceRoot = classRoots.get(interfacePassIndex);
			if (interfaceRoot.isInterface()) {
				HashMap<MethodSignature, AbstractMethodNode> interfaceMethodsBySignature = GlobalUtils.newHashMap();
				HashMap<MethodParameterKey, AbstractMethodNode> interfaceMethodsByParameterKey = GlobalUtils.newHashMap();
				MultiMap<String, String> interfaceMethodNamesByDescriptor = new MultiMap<>(13);
				MethodNameMap<String, AbstractMethodNode> interfaceMethodNames;
				if (this.uniqueMethodNames) {
					interfaceMethodNames = new MethodNameMap<>(false);
				} else {
					interfaceMethodNames = new MethodNameMap<>(true);
				}

				interfaceRoot.processMethodRenaming(
					this.defaultExcludes, this.parameterChangerExcludes, this.changeLogContext, this, this.hierarchy, nameGenerator, interfaceMethodsBySignature, interfaceMethodNamesByDescriptor, this.keepInnerClassInfo, interfaceMethodsByParameterKey, sharedInterfaceMethodNames, interfaceMethodNames, this.aggressiveMethodRenaming, this.randomizeObfuscation, reservedFieldNames, this.classNameMappings, this.resourceProtectedMethods
				);
			}
		}

		for (int classPassIndex = 0; classPassIndex < classRootCount; classPassIndex++) {
			ClassHierarchyNode classRoot = classRoots.get(classPassIndex);
			if (!classRoot.isInterface()) {
				HashMap<MethodSignature, AbstractMethodNode> classMethodsBySignature = GlobalUtils.newHashMap();
				HashMap<MethodParameterKey, AbstractMethodNode> classMethodsByParameterKey = GlobalUtils.newHashMap();
				MultiMap<String, String> classMethodNamesByDescriptor = new MultiMap<>(13);
				MethodNameMap<String, AbstractMethodNode> classMethodNames;
				if (this.uniqueMethodNames) {
					classMethodNames = new MethodNameMap<>(false);
				} else {
					classMethodNames = new MethodNameMap<>(true);
				}

				classRoot.processMethodRenaming(
					this.defaultExcludes, this.parameterChangerExcludes, this.changeLogContext, this, this.hierarchy, nameGenerator, classMethodsBySignature, classMethodNamesByDescriptor, this.keepInnerClassInfo, classMethodsByParameterKey, sharedInterfaceMethodNames, classMethodNames, this.aggressiveMethodRenaming, this.randomizeObfuscation, reservedFieldNames, this.classNameMappings, this.resourceProtectedMethods
				);
			}
		}

		this.workspace.applyInnerClassMethodRenames(this.processedClasses, this.renamedSignatures, this.scriptContext);
		this.workspace.resolveRefsAndSyncMetadata(this.processedClasses, this.scriptContext, false);
		this.workspace.rebuildMethodRegistries();
	}

	private void logInterfaceRenameFailure(List<ClassHierarchyNode> interfaces, MethodSignature signature, String reason) {
		for (int i = 0; i < interfaces.size(); i++) {
			String interfaceInternalName = interfaces.get(i).getInternalName();
			FlaggedMethodSignature flaggedMethodSignature = this.changeLogContext.getFlaggedMethodSignature(interfaceInternalName, signature);
			if (flaggedMethodSignature != null) {
				this.changeLogContext.removeProcessedMethodMapping(interfaceInternalName, signature);
				this.changeLogContext
					.logWarning(
						"Could not rename method '"
							+ signature.getSignature(this.classNameMappings)
							+ "' to '"
							+ flaggedMethodSignature.getName()
							+ " in interface '"
							+ GlobalUtils.internalToBinaryName(GlobalUtils.getMappedOrDefault(interfaceInternalName, this.classNameMappings))
							+ "' because of '"
							+ reason
							+ "'."
					);
			}
		}
	}

	private void validateMethodRenames() {
		if (this.changeLogContext != null) {
			for (Entry<AtomicReference<List<ClassHierarchyNode>>, MethodSignature> implementationRefEntry : this.hierarchy.getAllImplementationRefs().entrySet()) {
				this.validateImplementations(implementationRefEntry.getKey(), implementationRefEntry.getValue());
			}

			List<ClassHierarchyNode> classRoots = this.classTreeManager.getAvailableClassRoots();
			int classRootCount = classRoots.size();

			for (int interfacePassIndex = 0; interfacePassIndex < classRootCount; interfacePassIndex++) {
				ClassHierarchyNode interfaceRoot = classRoots.get(interfacePassIndex);
				if (interfaceRoot.isInterface()) {
					interfaceRoot.validateMethodRenames(this);
				}
			}

			for (int classPassIndex = 0; classPassIndex < classRootCount; classPassIndex++) {
				ClassHierarchyNode classRoot = classRoots.get(classPassIndex);
				if (!classRoot.isInterface()) {
					classRoot.validateMethodRenames(this);
				}
			}
		}
	}

	public final void validateMethodRename(ClassHierarchyNode hierarchyNode, AbstractClassNode classNode, AbstractMethodNode methodNode) {
		String classInternalName = classNode.getInternalName();
		MethodSignature originalSignature = methodNode.getMethodSignature();
		MethodSignature newSignature = this.changeLogContext.getNewMethodSignature(classInternalName, originalSignature);
		if (newSignature != null) {
			AtomicReference<AbstractMethodKey> clashingMethodRef = new AtomicReference<>();
			if (this.hierarchy.isConflicting(newSignature)) {
				if (this.aggressiveMethodRenaming && !newSignature.equals(originalSignature) || !this.aggressiveMethodRenaming && !newSignature.getParameterKey().equals(originalSignature.getParameterKey())) {
					if (hierarchyNode.isWorkspaceClass()) {
						this.changeLogContext
							.logWarning(
								"Method '"
									+ methodNode.getSignature()
									+ "' in class '"
									+ classNode.getClassNameBinary()
									+ "' could not be renamed to '"
									+ newSignature.getName()
									+ "' because of a possible name clash. (1)"
							);
						this.changeLogContext.removeProcessedMethodMapping(classInternalName, originalSignature);
					} else {
						this.changeLogContext
							.logSpecialError(
								"Method '"
									+ methodNode.getSignature()
									+ "' in class '"
									+ classNode.getClassNameBinary()
									+ "' could not be renamed to '"
									+ newSignature.getName()
									+ "' because of a possible name clash. (2)"
							);
						this.changeLogContext.removeProcessedMethodMapping(classInternalName, originalSignature);
					}
				}
			} else if ((this.aggressiveMethodRenaming && newSignature.equals(originalSignature) || !this.aggressiveMethodRenaming && newSignature.getParameterKey().equals(originalSignature.getParameterKey())) && !this.isObfuscatedName(originalSignature.getName())) {
				this.hierarchy.addConflictingSignature(newSignature);
			} else if (!this.checkMethodChange(hierarchyNode, originalSignature, newSignature, methodNode, clashingMethodRef)) {
				String discardedMappedClassName = GlobalUtils.getMappedOrDefault(classInternalName, this.classNameMappings);
				AbstractMethodKey clashingMethodKey = clashingMethodRef.get();
				String clashDescription = "";
				if (clashingMethodKey != null) {
					clashDescription = " with method '" + clashingMethodKey.getSignature(this.classNameMappings) + "'";
				}

				if (hierarchyNode.isWorkspaceClass()) {
					this.changeLogContext
						.logWarning(
							"Method '"
								+ methodNode.getSignature()
								+ "' in class '"
								+ classNode.getClassNameBinary()
								+ "' could not be renamed to '"
								+ newSignature.getName()
								+ "' because of a name clash"
								+ clashDescription
								+ ". (3)"
						);
					this.changeLogContext.removeProcessedMethodMapping(classInternalName, originalSignature);
				} else {
					this.changeLogContext
						.logSpecialError(
							"Method '"
								+ methodNode.getSignature()
								+ "' in class '"
								+ classNode.getClassNameBinary()
								+ "' could not be renamed to '"
								+ newSignature.getName()
								+ "' because of a possible name clash"
								+ clashDescription
								+ ". (4)"
						);
					this.changeLogContext.removeProcessedMethodMapping(classInternalName, originalSignature);
				}
			}
		}
	}

	public String getOriginalMethodName(String classInternalName, AbstractMethodKey renamedMethodKey) {
		MethodSignature originalSignature = null;
		if (renamedMethodKey instanceof MethodSignature) {
			originalSignature = this.originalSignatures.get(classInternalName, (MethodSignature)renamedMethodKey);
		} else if (renamedMethodKey instanceof MethodParameterKey) {
			MethodParameterKey parameterKey = (MethodParameterKey)renamedMethodKey;
			Map<MethodSignature, MethodSignature> originalSignaturesByRenamed = this.originalSignatures.getGroup(classInternalName);
			if (originalSignaturesByRenamed != null) {
				for (Entry<MethodSignature, MethodSignature> signatureMapping : originalSignaturesByRenamed.entrySet()) {
					MethodSignature renamedSignature = signatureMapping.getKey();
					if (parameterKey.matchesSignature(renamedSignature)) {
						originalSignature = signatureMapping.getValue();
						break;
					}
				}
			}
		}

		return originalSignature != null ? originalSignature.getName() : renamedMethodKey.getName();
	}

	public final void validateRenameOverride(ClassHierarchyNode hierarchyNode, AbstractClassNode classNode, AbstractMethodNode methodNode) {
		String classInternalName = classNode.getInternalName();
		MethodSignature originalSignature = methodNode.getMethodSignature();
		FlaggedMethodSignature flaggedMethodSignature = this.changeLogContext.getFlaggedMethodSignature(classInternalName, originalSignature);
		if (flaggedMethodSignature != null && !methodNode.isStatic() && !methodNode.isPrivate()) {
			AbstractClassNode implementationClass = this.hierarchy.getMethodImplementationClass(methodNode);
			if (implementationClass != null) {
				ClassHierarchyNode implementationNode = ClassHierarchyNode.getNodeByInternalName(implementationClass.getInternalName());
				if (implementationNode != null && !implementationNode.isMissing()) {
					flaggedMethodSignature = this.changeLogContext.reconcileMethodMapping(classInternalName, implementationClass.getInternalName(), originalSignature, flaggedMethodSignature, this.classNameMappings);
					if (!flaggedMethodSignature.equals(flaggedMethodSignature)) {
					}
				} else if (!originalSignature.getName().equals(flaggedMethodSignature.getName())) {
					String mappedClassName = GlobalUtils.getMappedOrDefault(classInternalName, this.classNameMappings);
					this.changeLogContext
						.logWarning(
							"Method '"
								+ originalSignature.getSignature(this.classNameMappings)
								+ "' in class '"
								+ GlobalUtils.internalToBinaryName(mappedClassName)
								+ "' could not be renamed to '"
								+ flaggedMethodSignature.getName()
								+ "' because it now overrides a method in '"
								+ implementationClass.getClassNameBinary()
								+ "' which has not been opened."
						);
					this.changeLogContext.removeProcessedMethodMapping(classInternalName, originalSignature);
				}
			}
		}
	}

	private DualKeyMap<String, MethodSignature, MethodSignature> buildLibraryMethodMappings() {
		DualKeyMap<String, MethodSignature, MethodSignature> libraryMethodMappings = new DualKeyMap<>();
		if (this.changeLogContext != null) {
			for (String classInternalName : this.changeLogContext.getNewClassNames()) {
				if (!ClassHierarchyNode.isWorkspaceClassByInternalName(classInternalName)) {
					ArrayList<KeyValuePair<MethodSignature, FlaggedMethodSignature>> signatureMappings = this.changeLogContext.getMethodSignatureMappings(classInternalName);
					if (signatureMappings != null) {
						for (KeyValuePair<MethodSignature, FlaggedMethodSignature> signatureMapping : signatureMappings) {
							libraryMethodMappings.put(classInternalName, signatureMapping.getKey(), signatureMapping.getValue());
						}
					}
				}
			}
		}

		return libraryMethodMappings;
	}

	public KeyValuePair<?, ?> findExistingMethodMapping(Set<AbstractMethodNode> methods, AtomicReference<String> namePrefixRef) {
		namePrefixRef.set(null);
		if (methods != null) {
			for (AbstractMethodNode methodNode : methods) {
				KeyValuePair<?, ?> existingMapping = this.methodMapping.getPair(methodNode);
				if (existingMapping != null) {
					namePrefixRef.set(this.defaultExcludes.getMethodPrefix(methodNode));
					return existingMapping;
				}
			}
		}

		return null;
	}

	private void validateImplementations(AtomicReference<List<ClassHierarchyNode>> implementationsRef, MethodSignature signature) {
		List<ClassHierarchyNode> implementations = implementationsRef.get();
		MethodSignature agreedNewSignature = null;

		for (int implementationIndex = 0; implementationIndex < implementations.size(); implementationIndex++) {
			ClassHierarchyNode implementationNode = implementations.get(implementationIndex);
			MethodSignature implementationNewSignature = this.changeLogContext.getNewMethodSignature(implementationNode.getInternalName(), signature);
			if (implementationNewSignature != null) {
				if (agreedNewSignature == null) {
					agreedNewSignature = implementationNewSignature;
				} else if (!implementationNewSignature.equals(agreedNewSignature)) {
					this.logInterfaceRenameFailure(implementations, signature, "clashing mappings in between related interfaces");
					agreedNewSignature = null;
					break;
				}
			}
		}

		if (agreedNewSignature != null) {
			AtomicReference<AbstractMethodKey> clashingMethodRef = new AtomicReference<>();
			if (!this.hierarchy.checkGroupMethodChange(implementationsRef, agreedNewSignature, signature, clashingMethodRef)) {
				this.logInterfaceRenameFailure(implementations, signature, "clashing mappings in between related interface sets");
			}
		}
	}
}
