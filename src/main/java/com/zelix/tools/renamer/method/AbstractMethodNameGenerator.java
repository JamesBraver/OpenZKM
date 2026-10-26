package com.zelix.tools.renamer.method;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.signature.AbstractMethodKey;
import com.zelix.framework.signature.MethodParameterKey;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.Hierarchy;
import com.zelix.tools.excludes.DefaultExcludes;
import com.zelix.tools.parameterChanger.ParameterChangerExcludes;
import com.zelix.tools.renamer.MethodRenamer;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.KeyValuePair;
import com.zelix.utils.collections.MethodNameMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.ClassHierarchyNode;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractMethodNameGenerator {
	public final Hierarchy hierarchy;
	public final ParameterChangerExcludes parameterChangerExcludes;
	public final MethodRenamer methodRenamer;
	public final DefaultExcludes defaultExcludes;
	public final boolean randomizeObfuscation;
	public final ScriptContext scriptContext;
	private final HashMap unusedMap;

	public AbstractMethodNameGenerator(MethodRenamer methodRenamer, DefaultExcludes defaultExcludes, ParameterChangerExcludes parameterChangerExcludes, boolean randomizeObfuscation) {
		this.hierarchy = methodRenamer.getHierarchy();
		this.parameterChangerExcludes = parameterChangerExcludes;
		this.methodRenamer = methodRenamer;
		this.defaultExcludes = defaultExcludes;
		this.randomizeObfuscation = randomizeObfuscation;
		this.scriptContext = methodRenamer.getScriptContext();
		this.unusedMap = GlobalUtils.newHashMap();
	}

	private String createMethodName(
		String namePrefix,
		ClassHierarchyNode classHierarchyNode,
		MethodSignature originalSignature,
		String descriptor,
		Map<MethodSignature, AbstractMethodNode> processedMethodsBySignature,
		boolean isInterface,
		Map<MethodSignature, MethodNode> localMethodsBySignature,
		Map<MethodParameterKey, AbstractMethodNode> processedMethodsByParamKey,
		Map<MethodParameterKey, AbstractMethodNode> localMethodsByParamKey,
		MethodNameMap<String, AbstractMethodNode> interfaceMethodNames,
		MethodNameMap<String, AbstractMethodNode> classMethodNames,
		Set<String> usedNames,
		boolean aggressiveRenaming,
		boolean checkSpecialState,
		boolean parameterObfuscated
	) {
		String generatorDescriptor;
		if (aggressiveRenaming) {
			generatorDescriptor = originalSignature.getFullDescriptor();
		} else {
			generatorDescriptor = originalSignature.getParameterDescriptor();
		}

		String generatedName;
		MethodSignature candidateSignature;
		do {
			generatedName = this.getNextName(classHierarchyNode, generatorDescriptor);
			if (Options.ZKM_DEBUG_SHOW_OLD_NAMES) {
				generatedName = originalSignature.getName() + '_' + generatedName;
			}

			if (namePrefix != null) {
				generatedName = MethodSignature.createAccessorName(namePrefix, generatedName);
			}

			candidateSignature = new MethodSignature(generatedName, descriptor);
		} while (!this.isNameAvailable(classHierarchyNode, candidateSignature, originalSignature, processedMethodsBySignature, localMethodsBySignature, processedMethodsByParamKey, localMethodsByParamKey, interfaceMethodNames, classMethodNames, usedNames, namePrefix, aggressiveRenaming, isInterface, checkSpecialState, parameterObfuscated));

		return generatedName;
	}

	public final String generatePrivateMethodName(
		ClassHierarchyNode classHierarchyNode,
		String classInternalName,
		MethodNode methodNode,
		Map<MethodSignature, AbstractMethodNode> processedMethodsBySignature,
		Map<MethodSignature, MethodNode> localMethodsBySignature,
		Map<MethodParameterKey, AbstractMethodNode> processedMethodsByParamKey,
		Map<MethodParameterKey, AbstractMethodNode> localMethodsByParamKey,
		MethodNameMap<String, AbstractMethodNode> interfaceMethodNames,
		MethodNameMap<String, AbstractMethodNode> classMethodNames,
		Set<String> usedNames,
		boolean aggressiveRenaming,
		boolean parameterObfuscated
	) {
		return this.resolveMethodName(classHierarchyNode, classInternalName, methodNode, processedMethodsBySignature, classHierarchyNode.isInterface(), localMethodsBySignature, processedMethodsByParamKey, localMethodsByParamKey, interfaceMethodNames, classMethodNames, usedNames, aggressiveRenaming, parameterObfuscated);
	}

	public abstract String generateMethodName(
		ClassHierarchyNode classHierarchyNode,
		String classInternalName,
		MethodNode methodNode,
		Map<MethodSignature, AbstractMethodNode> processedMethodsBySignature,
		MultiMap<String, String> libraryNamesByDescriptor,
		boolean isInterface,
		Map<MethodSignature, MethodNode> localMethodsBySignature,
		Map<MethodParameterKey, AbstractMethodNode> processedMethodsByParamKey,
		Map<MethodParameterKey, AbstractMethodNode> localMethodsByParamKey,
		MethodNameMap<String, AbstractMethodNode> interfaceMethodNames,
		MethodNameMap<String, AbstractMethodNode> classMethodNames,
		Set<String> usedNames,
		boolean aggressiveRenaming,
		boolean parameterObfuscated
	);

	public abstract String getNextName(ClassHierarchyNode classHierarchyNode, String descriptor);

	public boolean isParameterObfuscated(MethodNode methodNode) {
		if (this.parameterChangerExcludes == null) {
			return false;
		} else {
			AbstractMethodNode baseMethod = this.hierarchy.getBaseMethod(methodNode);
			if (baseMethod != null) {
				return baseMethod.isWorkspaceNode() ? this.parameterChangerExcludes.isMethodExcluded((MethodNode)baseMethod) : false;
			} else {
				return this.parameterChangerExcludes.isMethodExcluded(methodNode);
			}
		}
	}

	public final boolean isNameAvailable(
		ClassHierarchyNode classHierarchyNode,
		MethodSignature candidateSignature,
		MethodSignature originalSignature,
		Map<MethodSignature, AbstractMethodNode> processedMethodsBySignature,
		Map<MethodSignature, MethodNode> localMethodsBySignature,
		Map<MethodParameterKey, AbstractMethodNode> processedMethodsByParamKey,
		Map<MethodParameterKey, AbstractMethodNode> localMethodsByParamKey,
		MethodNameMap<String, AbstractMethodNode> interfaceMethodNames,
		MethodNameMap<String, AbstractMethodNode> classMethodNames,
		Set<String> usedNames,
		String namePrefix,
		boolean aggressiveRenaming,
		boolean isInterface,
		boolean checkSpecialState,
		boolean parameterObfuscated
	) {
		MethodSignature objectArraySignature = null;
		if (parameterObfuscated) {
			objectArraySignature = new MethodSignature(candidateSignature.getName(), ParameterChangerExcludes.toObjectArrayDescriptor(candidateSignature));
		}

		if (candidateSignature.getName().equals(originalSignature.getName())) {
			return false;
		} else if (!interfaceMethodNames.isDisabled() && interfaceMethodNames.containsKey(candidateSignature.getName())) {
			return false;
		} else if (!classMethodNames.isDisabled() && classMethodNames.containsKey(candidateSignature.getName())) {
			return false;
		} else if ((!aggressiveRenaming || !localMethodsBySignature.containsKey(candidateSignature)) && (aggressiveRenaming || !localMethodsByParamKey.containsKey(candidateSignature.getParameterKey()))) {
			if (!parameterObfuscated || (!aggressiveRenaming || !localMethodsBySignature.containsKey(objectArraySignature)) && (aggressiveRenaming || !localMethodsByParamKey.containsKey(objectArraySignature.getParameterKey()))) {
				if ((!aggressiveRenaming || !processedMethodsBySignature.containsKey(candidateSignature)) && (aggressiveRenaming || !processedMethodsByParamKey.containsKey(candidateSignature.getParameterKey()))) {
					if (!parameterObfuscated || (!aggressiveRenaming || !processedMethodsBySignature.containsKey(objectArraySignature)) && (aggressiveRenaming || !processedMethodsByParamKey.containsKey(objectArraySignature.getParameterKey()))) {
						if (this.hierarchy.isConflicting(candidateSignature)) {
							return false;
						} else if (parameterObfuscated && this.hierarchy.isConflicting(objectArraySignature)) {
							return false;
						} else if (this.hierarchy.areMethodsDifferent(classHierarchyNode, candidateSignature, originalSignature)) {
							return false;
						} else if (parameterObfuscated && this.hierarchy.areMethodsDifferent(classHierarchyNode, objectArraySignature, originalSignature)) {
							return false;
						} else if (isInterface && !this.hierarchy.checkMethodChangeWithFlag(classHierarchyNode, candidateSignature, originalSignature, checkSpecialState, new AtomicReference<>())) {
							return false;
						} else {
							return parameterObfuscated && isInterface && !this.hierarchy.checkMethodChangeWithFlag(classHierarchyNode, objectArraySignature, originalSignature, checkSpecialState, new AtomicReference<>())
								? false
								: namePrefix == null || !usedNames.contains(MethodSignature.getFieldNameFromAccessor(candidateSignature.getName(), namePrefix));
						}
					} else {
						return false;
					}
				} else {
					return false;
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	private String createUnprefixedMethodName(
		ClassHierarchyNode classHierarchyNode,
		MethodSignature originalSignature,
		String descriptor,
		Map<MethodSignature, AbstractMethodNode> processedMethodsBySignature,
		boolean isInterface,
		Map<MethodSignature, MethodNode> localMethodsBySignature,
		Map<MethodParameterKey, AbstractMethodNode> processedMethodsByParamKey,
		Map<MethodParameterKey, AbstractMethodNode> localMethodsByParamKey,
		MethodNameMap<String, AbstractMethodNode> interfaceMethodNames,
		MethodNameMap<String, AbstractMethodNode> classMethodNames,
		Set<String> usedNames,
		boolean aggressiveRenaming,
		boolean parameterObfuscated
	) {
		return this.createMethodName(null, classHierarchyNode, originalSignature, descriptor, processedMethodsBySignature, isInterface, localMethodsBySignature, processedMethodsByParamKey, localMethodsByParamKey, interfaceMethodNames, classMethodNames, usedNames, aggressiveRenaming, true, parameterObfuscated);
	}

	public final String generateInheritedMethodName(
		ClassHierarchyNode classHierarchyNode,
		String classInternalName,
		MethodNode methodNode,
		Map<MethodSignature, AbstractMethodNode> processedMethodsBySignature,
		boolean isInterface,
		Map<MethodSignature, MethodNode> localMethodsBySignature,
		Map<MethodParameterKey, AbstractMethodNode> processedMethodsByParamKey,
		Map<MethodParameterKey, AbstractMethodNode> localMethodsByParamKey,
		MethodNameMap<String, AbstractMethodNode> interfaceMethodNames,
		MethodNameMap<String, AbstractMethodNode> classMethodNames,
		Set<String> usedNames,
		boolean aggressiveRenaming,
		boolean parameterObfuscated
	) {
		MethodSignature methodSignature = methodNode.getMethodSignature();
		AtomicReference<List<ClassHierarchyNode>> methodImplementationsRef = null;
		if (classHierarchyNode.isInterface()) {
			methodImplementationsRef = this.hierarchy.getMethodImplementationsRef(classHierarchyNode, methodSignature);
		}

		String contextString;
		if (methodImplementationsRef != null) {
			contextString = this.hierarchy.getContextString(methodImplementationsRef);
			if (contextString == null) {
				AbstractClassNode interfaceImplementationClass = this.hierarchy.getMethodImplementationClass(methodNode);
				if (interfaceImplementationClass != null) {
					String interfaceImplementationInternalName = interfaceImplementationClass.getInternalName();
					ClassHierarchyNode interfaceImplementationNode = ClassHierarchyNode.getNodeByInternalName(interfaceImplementationInternalName);
					if (interfaceImplementationNode == null || interfaceImplementationNode.isMissing()) {
						contextString = null;
					} else if (!interfaceImplementationNode.isWorkspaceClass()) {
						contextString = this.methodRenamer.getMappedMethodName(interfaceImplementationInternalName, methodSignature);
					} else {
						contextString = this.resolveMethodName(classHierarchyNode, classInternalName, methodNode, processedMethodsBySignature, isInterface, localMethodsBySignature, processedMethodsByParamKey, localMethodsByParamKey, interfaceMethodNames, classMethodNames, usedNames, aggressiveRenaming, parameterObfuscated);
					}
				} else {
					contextString = this.resolveMethodName(classHierarchyNode, classInternalName, methodNode, processedMethodsBySignature, isInterface, localMethodsBySignature, processedMethodsByParamKey, localMethodsByParamKey, interfaceMethodNames, classMethodNames, usedNames, aggressiveRenaming, parameterObfuscated);
				}

				if (contextString != null) {
					this.hierarchy.setContextString(methodImplementationsRef, contextString);
				}
			} else if (parameterObfuscated && !methodNode.getNameDisplay().equals(contextString)) {
				AbstractMethodNode baseMethod = this.hierarchy.getBaseMethod(methodNode);
				if (baseMethod != null && baseMethod.isParameterObfuscationMarked()) {
					methodNode.setParameterObfuscationMarked(true);
				}
			}
		} else {
			AbstractClassNode implementationClass = this.hierarchy.getMethodImplementationClass(methodNode);
			if (implementationClass != null) {
				ClassHierarchyNode implementationNode = ClassHierarchyNode.getNodeByInternalName(implementationClass.getInternalName());
				if (implementationNode != null && !implementationNode.isMissing()) {
					contextString = this.methodRenamer.getMappedMethodName(implementationClass.getInternalName(), methodSignature);
					if (parameterObfuscated && !methodNode.getNameDisplay().equals(contextString) && this.hierarchy.getBaseMethod(methodNode).isParameterObfuscationMarked()) {
						methodNode.setParameterObfuscationMarked(true);
					}
				} else {
					contextString = null;
				}
			} else {
				contextString = this.resolveMethodName(classHierarchyNode, classInternalName, methodNode, processedMethodsBySignature, isInterface, localMethodsBySignature, processedMethodsByParamKey, localMethodsByParamKey, interfaceMethodNames, classMethodNames, usedNames, aggressiveRenaming, parameterObfuscated);
			}
		}

		return contextString;
	}

	public final String resolveMethodName(
		ClassHierarchyNode classHierarchyNode,
		String classInternalName,
		MethodNode methodNode,
		Map<MethodSignature, AbstractMethodNode> processedMethodsBySignature,
		boolean isInterface,
		Map<MethodSignature, MethodNode> localMethodsBySignature,
		Map<MethodParameterKey, AbstractMethodNode> processedMethodsByParamKey,
		Map<MethodParameterKey, AbstractMethodNode> localMethodsByParamKey,
		MethodNameMap<String, AbstractMethodNode> interfaceMethodNames,
		MethodNameMap<String, AbstractMethodNode> classMethodNames,
		Set<String> usedNames,
		boolean aggressiveRenaming,
		boolean parameterObfuscated
	) {
		MethodSignature methodSignature = methodNode.getMethodSignature();
		String fullDescriptor = methodSignature.getFullDescriptor();
		AbstractMethodNode baseMethod = null;
		if (!methodNode.isPrivate() && !methodNode.isStatic()) {
			baseMethod = this.hierarchy.getBaseMethod(methodNode);
		}

		Object effectiveMethodNode;
		if (baseMethod != null) {
			effectiveMethodNode = baseMethod;
		} else {
			effectiveMethodNode = methodNode;
		}

		String namePrefix = this.defaultExcludes.getMethodPrefix((AbstractMethodNode)effectiveMethodNode);
		String resolvedName;
		if (namePrefix != null && this.defaultExcludes.isInMethodGroup((AbstractMethodNode)effectiveMethodNode)) {
			Set<AbstractMethodNode> samePrefixMethods = this.defaultExcludes.getMethodsWithSamePrefix((AbstractMethodNode)effectiveMethodNode);
			Set<AbstractMethodNode> methodGroup = this.defaultExcludes.getMethodGroup((AbstractMethodNode)effectiveMethodNode);
			AtomicReference<String> samePrefixMappingPrefixRef = new AtomicReference<>();
			KeyValuePair<?, ?> samePrefixMapping = this.methodRenamer.findExistingMethodMapping(samePrefixMethods, samePrefixMappingPrefixRef);
			AtomicReference<String> groupMappingPrefixRef = new AtomicReference<>();
			KeyValuePair<?, ?> groupMapping = this.methodRenamer.findExistingMethodMapping(methodGroup, groupMappingPrefixRef);
			if (samePrefixMapping == null && groupMapping == null) {
				ArrayList<AbstractMethodNameGenerator.PendingMethodChange> pendingChanges = new ArrayList<>(methodGroup.size());

				boolean nameAcceptedByAll;
				do {
					nameAcceptedByAll = true;
					pendingChanges.clear();
					resolvedName = this.createMethodName(namePrefix, classHierarchyNode, methodSignature, fullDescriptor, processedMethodsBySignature, isInterface, localMethodsBySignature, processedMethodsByParamKey, localMethodsByParamKey, interfaceMethodNames, classMethodNames, usedNames, aggressiveRenaming, true, parameterObfuscated);
					String nameSuffix = resolvedName.substring(namePrefix.length());

					for (AbstractMethodNode groupMethod : methodGroup) {
						if (groupMethod.isWorkspaceNode()) {
							MethodNode workspaceMethodNode = (MethodNode)groupMethod;
							String memberNamePrefix = this.defaultExcludes.getMethodPrefix(workspaceMethodNode);
							MethodSignature memberSignature = workspaceMethodNode.getMethodSignature();
							MethodSignature memberCandidateSignature = new MethodSignature(memberNamePrefix + nameSuffix, workspaceMethodNode.getDescriptor());
							if (!this.isNameAvailable(classHierarchyNode, memberCandidateSignature, memberSignature, processedMethodsBySignature, localMethodsBySignature, processedMethodsByParamKey, localMethodsByParamKey, interfaceMethodNames, classMethodNames, usedNames, null, aggressiveRenaming, isInterface, false, parameterObfuscated)) {
								nameAcceptedByAll = false;
								break;
							}

							pendingChanges.add(new AbstractMethodNameGenerator.PendingMethodChange(classHierarchyNode, memberCandidateSignature, memberSignature, workspaceMethodNode));
						}
					}
				} while (!nameAcceptedByAll);

				usedNames.add(MethodSignature.getFieldNameFromAccessor(resolvedName, namePrefix));

				for (AbstractMethodNameGenerator.PendingMethodChange pendingChange : pendingChanges) {
					AtomicReference<AbstractMethodKey> relatedMethodKeyRef = new AtomicReference<>();
					boolean changeRecorded;
					if (((ClassHierarchyNode)pendingChange.getClassNode()).isInterface()) {
						changeRecorded = this.hierarchy
							.checkInterfaceMethodChange((ClassHierarchyNode)pendingChange.getClassNode(), (MethodSignature)pendingChange.getNewSignature(), (MethodSignature)pendingChange.getOldSignature(), relatedMethodKeyRef);
					} else {
						changeRecorded = this.hierarchy
							.checkMethodChange(
								(ClassHierarchyNode)pendingChange.getClassNode(),
								(MethodSignature)pendingChange.getNewSignature(),
								(MethodSignature)pendingChange.getOldSignature(),
								(AbstractMethodNode)pendingChange.getMethodNode(),
								relatedMethodKeyRef
							);
					}

					if (!changeRecorded) {
						DebugUtils.assertTrue(
							false,
							new String[]{
								"Unable to record '"
									+ pendingChange.getOldSignature()
									+ "'->'"
									+ pendingChange.getNewSignature()
									+ "' in class '"
									+ ((ClassHierarchyNode)pendingChange.getClassNode()).getName()
									+ "' : '"
									+ ((MethodNode)pendingChange.getMethodNode()).toDeclarationWithParamNames()
									+ "' : '"
									+ (relatedMethodKeyRef.isNull() ? "null" : relatedMethodKeyRef.get().toHumanReadableString())
									+ "' : Related to '"
									+ methodNode.getDisplayName()
									+ "' in '"
									+ methodNode.getSourceName()
									+ "'"
							}
						);
					}
				}
			} else if (samePrefixMapping != null) {
				resolvedName = (String)samePrefixMapping.getValue();
			} else {
				resolvedName = namePrefix + ((String)groupMapping.getValue()).substring(groupMappingPrefixRef.get().length());
			}
		} else {
			resolvedName = this.createUnprefixedMethodName(classHierarchyNode, methodSignature, fullDescriptor, processedMethodsBySignature, isInterface, localMethodsBySignature, processedMethodsByParamKey, localMethodsByParamKey, interfaceMethodNames, classMethodNames, usedNames, aggressiveRenaming, parameterObfuscated);
		}

		if (parameterObfuscated && resolvedName != null && !resolvedName.equals(((AbstractMethodNode)effectiveMethodNode).getNameDisplay())) {
			((AbstractMethodNode)effectiveMethodNode).setParameterObfuscationMarked(true);
			if (effectiveMethodNode != methodNode) {
				methodNode.setParameterObfuscationMarked(true);
			}
		}

		return resolvedName;
	}

	public static class PendingMethodChange implements Serializable {
		public Object classNode;
		public Object newSignature;
		public Object oldSignature;
		public Object methodNode;

		public PendingMethodChange(Object classNode, Object newSignature, Object oldSignature, Object methodNode) {
			this.classNode = classNode;
			this.newSignature = newSignature;
			this.oldSignature = oldSignature;
			this.methodNode = methodNode;
		}

		public Object getMethodNode() {
			return this.methodNode;
		}

		public Object getClassNode() {
			return this.classNode;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof AbstractMethodNameGenerator.PendingMethodChange)) {
				return false;
			} else {
				AbstractMethodNameGenerator.PendingMethodChange other = (AbstractMethodNameGenerator.PendingMethodChange)obj;
				return (this.classNode == null && other.classNode == null || this.classNode != null && other.classNode != null && this.classNode.equals(other.classNode))
					&& (this.newSignature == null && other.newSignature == null || this.newSignature != null && other.newSignature != null && this.newSignature.equals(other.newSignature))
					&& (this.oldSignature == null && other.oldSignature == null || this.oldSignature != null && other.oldSignature != null && this.oldSignature.equals(other.oldSignature))
					&& (this.methodNode == null && other.methodNode == null || this.methodNode != null && other.methodNode != null && this.methodNode.equals(other.methodNode));
			}
		}

		@Override
		public int hashCode() {
			int hash = 0;
			if (this.classNode != null) {
				hash = 0 ^ this.classNode.hashCode();
			}

			if (this.newSignature != null) {
				hash ^= this.newSignature.hashCode();
			}

			if (this.oldSignature != null) {
				hash ^= this.oldSignature.hashCode();
			}

			if (this.methodNode != null) {
				hash ^= this.methodNode.hashCode();
			}

			return hash;
		}

		public Object getOldSignature() {
			return this.oldSignature;
		}

		public Object getNewSignature() {
			return this.newSignature;
		}
	}
}
