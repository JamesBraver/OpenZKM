package com.zelix.repository;

import com.zelix.exceptions.ClasspathException;
import com.zelix.exceptions.FileException;
import com.zelix.exceptions.HierarchyClassNotFoundException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.reference.BitSetReferenceGraph;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.signature.AbstractMethodKey;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.tools.parameterChanger.ParameterPattern;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.*;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.dataset.MethodState;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.ClassTreeManager;
import com.zelix.workspace.Workspace;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class Hierarchy {
	private static final MethodState INSTANCE = new MethodState(1);
	private static final MethodState STATIC = new MethodState(2);
	private static final MethodState PRIVATE = new MethodState(3);
	private final ReferenceGraph<ClassHierarchyNode, ClassHierarchyNode> classDependencies = new ReferenceGraph<>();
	private DualKeyMap<ClassHierarchyNode, MethodSignature, AtomicReference<List<ClassHierarchyNode>>> interfaceMethodGroups = new DualKeyMap<>();
	private Map<AtomicReference<List<ClassHierarchyNode>>, AtomicReference<Object>> methodGroupContext = GlobalUtils.newHashMap();
	private Set<AbstractMethodKey> excludedMethodKeys = GlobalUtils.newHashSet();
	private ReferenceGraph<ClassHierarchyNode, ClassHierarchyNode> relatedInterfaces = new ReferenceGraph<>();
	private final ClassProvider classProvider;
	private final boolean useFullSignature;
	private final boolean useParameterPattern;
	private DualKeyMap<ClassHierarchyNode, AbstractMethodKey, MethodState> methodStateMap;
	private DualKeyMap<String, MethodSignature, AbstractClassNode> methodOwnerMap;
	private DualKeyMap<String, MethodSignature, AbstractClassNode> interfaceMethodImplMap;
	private DualKeyMap<String, MethodSignature, KeyValuePair<AbstractClassNode, AbstractMethodNode>> interfaceMethodImplPairMap;
	private Map<AbstractMethodNode, AbstractMethodNode> baseMethodMap;
	private BitSetReferenceGraph<AbstractMethodNode, AbstractMethodNode> methodOverrideGraph;

	public Hierarchy(Workspace workspace, ClassTreeManager classTreeManager, ClassProvider classProvider, ScriptContext scriptContext, int classCount, boolean useFullSignature, boolean fullInitialization, boolean useParameterPattern) {
		this.classProvider = classProvider;
		this.useFullSignature = useFullSignature;
		this.useParameterPattern = useParameterPattern;
		this.methodStateMap = new DualKeyMap<>(classCount, 20);
		this.methodOwnerMap = new DualKeyMap<>(classCount, 50);
		this.interfaceMethodImplMap = new DualKeyMap<>(classCount, 50);
		this.interfaceMethodImplPairMap = new DualKeyMap<>(101, 37);
		this.initialize(workspace, classTreeManager, scriptContext, fullInitialization);
	}

	public Map<AbstractMethodNode, AbstractMethodNode> getBaseMethodMap() {
		return new ReadOnlyMap<>(this.baseMethodMap);
	}

	public final void addConflictingSignature(MethodSignature methodSignature) {
		AbstractMethodKey methodKey;
		if (this.useFullSignature) {
			methodKey = methodSignature;
		} else {
			methodKey = methodSignature.getParameterKey();
		}

		this.excludedMethodKeys.add(methodKey);
	}

	private boolean checkConflictsInHierarchy(
		ClassHierarchyNode classHierarchyNode,
		AbstractMethodKey newMethodKey,
		AbstractMethodKey oldMethodKey,
		Set<ClassHierarchyNode> changedClasses,
		Set<MethodStateSnapshot> stateSnapshots,
		Set<ClassHierarchyNode> collectedInterfaces,
		AbstractMethodNode methodNode,
		AtomicReference<AbstractMethodKey> conflictingMethodKey
	) {
		int stateChangeResult;
		if (methodNode.isPrivate() || methodNode.isConstructor()) {
			stateChangeResult = this.setPrivateMethodState(classHierarchyNode, newMethodKey, conflictingMethodKey);
		} else if (methodNode.isStatic()) {
			stateChangeResult = this.setStaticMethodState(classHierarchyNode, newMethodKey, conflictingMethodKey);
		} else {
			stateChangeResult = this.setInstanceMethodState(classHierarchyNode, newMethodKey, oldMethodKey, conflictingMethodKey);
		}

		switch (stateChangeResult) {
			case -1:
				this.rollbackState(changedClasses, stateSnapshots, newMethodKey);
				return false;
			case 0:
				changedClasses.add(classHierarchyNode);
			case 1:
			case 2:
		}

		classHierarchyNode.collectInterfaces(collectedInterfaces);
		if (!methodNode.isPrivate() && !methodNode.isStatic() && !methodNode.isClinit()) {
			Enumeration<ClassHierarchyNode> subclassesEnumeration = classHierarchyNode.getSubclassesEnumeration();
			if (subclassesEnumeration != null) {
				while (subclassesEnumeration.hasMoreElements()) {
					ClassHierarchyNode subclassNode = subclassesEnumeration.nextElement();
					if (!this.checkConflictsInHierarchy(subclassNode, newMethodKey, oldMethodKey, changedClasses, stateSnapshots, collectedInterfaces, methodNode, conflictingMethodKey)) {
						return false;
					}
				}
			}
		}

		return true;
	}

	private void initialize(AbstractWorkspace workspace, ClassTreeManager classTreeManager, ScriptContext scriptContext, boolean fullInitialization) {
		List<ClassHierarchyNode> classRoots = classTreeManager.getAvailableClassRoots();
		int classRootCount = classRoots.size();
		if (!fullInitialization) {
			this.baseMethodMap = GlobalUtils.newHashMap();
		} else {
			Random random = GlobalUtils.newRandom(2049);
			AtomicReference<Boolean> integrityResult = new AtomicReference<>();

			for (int i = 0; i < classRootCount; i++) {
				ClassHierarchyNode classRoot = classRoots.get(i);
				AbstractClassNode classNode = classRoot.getClassNode();
				String internalName = classNode.getInternalName();
				String context = "looking for superclass of '" + classNode.getRawSourcePath() + "'";
				this.getOrLoadClassMethodMap(internalName, integrityResult, scriptContext, random, context);
				classRoot.buildMethodOverrideMaps(this, this.classProvider, integrityResult, scriptContext, random);
			}

			HierarchicalMultiMap<MethodSignature, ClassHierarchyNode, ClassHierarchyNode> implementationsBySignature = new HierarchicalMultiMap<>();
			DualKeyMap<ClassHierarchyNode, ClassHierarchyNode, String> relationMarkers = new DualKeyMap<>();
			DualKeyMap<ClassHierarchyNode, MethodSignature, AbstractClassNode> methodOwnersByClass = new DualKeyMap<>();

			for (int classRootIndex = 0; classRootIndex < classRootCount; classRootIndex++) {
				ClassHierarchyNode interfaceNode = classRoots.get(classRootIndex);
				AbstractClassNode interfaceClass = interfaceNode.getClassNode();
				if (interfaceClass.isInterface()) {
					String interfaceName = interfaceClass.getInternalName();

					for (Entry<MethodSignature, AbstractClassNode> interfaceImplEntry : this.getInterfaceMethodImplGroup(interfaceName).entrySet()) {
						MethodSignature methodSignature = interfaceImplEntry.getKey();
						AbstractClassNode implementationClass = interfaceImplEntry.getValue();
						ClassHierarchyNode implementationNode = ClassHierarchyNode.getNodeByInternalName(implementationClass.getInternalName());
						if (implementationNode != null && !implementationNode.isMissing()) {
							implementationsBySignature.put(methodSignature, implementationNode, interfaceNode);
						} else {
							methodOwnersByClass.put(interfaceNode, methodSignature, implementationClass);
						}
					}

					HashSet<ClassHierarchyNode> implementingClasses = GlobalUtils.newHashSet();
					this.classDependencies.setReferences(interfaceNode, implementingClasses);
					this.buildClassReferences(interfaceNode, interfaceNode, true, implementingClasses, relationMarkers);
					relationMarkers.put(interfaceNode, interfaceNode, "s");
				}
			}

			Enumeration<MethodSignature> signatureEnumeration = implementationsBySignature.keys();

			while (signatureEnumeration.hasMoreElements()) {
				MethodSignature groupSignature = signatureEnumeration.nextElement();
				MultiMap<ClassHierarchyNode, ClassHierarchyNode> interfacesByImplementation = implementationsBySignature.get(groupSignature);
				ArrayList<ConcurrentHashSet<ClassHierarchyNode>> implementationGroups = new ArrayList<>();
				Enumeration<ClassHierarchyNode> implementationEnumeration = interfacesByImplementation.keys();

				while (implementationEnumeration.hasMoreElements()) {
					ClassHierarchyNode implementationRoot = implementationEnumeration.nextElement();
					ConcurrentHashSet<ClassHierarchyNode> collectedGroup = new ConcurrentHashSet<>();
					this.collectImplementingClasses(collectedGroup, implementationRoot, interfacesByImplementation);
					implementationGroups.add(collectedGroup);
				}

				implementationEnumeration = interfacesByImplementation.keys();

				while (implementationEnumeration.hasMoreElements()) {
					ClassHierarchyNode sharedRoot = implementationEnumeration.nextElement();
					ConcurrentHashSet<ClassHierarchyNode> mergedGroup = null;
					Iterator<ConcurrentHashSet<ClassHierarchyNode>> groupIterator = implementationGroups.iterator();

					while (groupIterator.hasNext()) {
						ConcurrentHashSet<ClassHierarchyNode> candidateGroup = groupIterator.next();
						if (candidateGroup.contains(sharedRoot)) {
							if (mergedGroup == null) {
								mergedGroup = candidateGroup;
							} else {
								Enumeration<ClassHierarchyNode> memberEnumeration = candidateGroup.enumeration();

								while (memberEnumeration.hasMoreElements()) {
									ClassHierarchyNode mergedMember = memberEnumeration.nextElement();
									mergedGroup.add(mergedMember);
								}

								groupIterator.remove();
							}
						}
					}
				}

				for (int groupIndex = 0; groupIndex < implementationGroups.size(); groupIndex++) {
					ConcurrentHashSet<ClassHierarchyNode> implementationGroup = implementationGroups.get(groupIndex);
					Vector<ClassHierarchyNode> groupMembers = new Vector<>(implementationGroup.size());
					AtomicReference<List<ClassHierarchyNode>> groupMembersRef = new AtomicReference<>(groupMembers);
					this.methodGroupContext.put(groupMembersRef, new AtomicReference<>());

					Iterator<ClassHierarchyNode> groupMemberIterator = implementationGroup.iterator();

					while (groupMemberIterator.hasNext()) {
						ClassHierarchyNode groupMember = groupMemberIterator.next();
						groupMembers.addElement(groupMember);
						this.interfaceMethodGroups.put(groupMember, groupSignature, groupMembersRef);
					}

					AbstractClassNode determiningClass = null;

					for (int implementationIndex = 0; implementationIndex < groupMembers.size(); implementationIndex++) {
						ClassHierarchyNode memberNode = groupMembers.elementAt(implementationIndex);
						AbstractClassNode recordedOwner = methodOwnersByClass.get(memberNode, groupSignature);
						if (recordedOwner != null) {
							determiningClass = recordedOwner;
							break;
						}
					}

					if (determiningClass == null) {
						AbstractClassNode fallbackClass = null;

						for (int resolveIndex = 0; resolveIndex < groupMembers.size(); resolveIndex++) {
							AbstractClassNode memberClass = groupMembers.elementAt(resolveIndex).getClassNode();
							AbstractClassNode resolvedImplementation = this.resolveInterfaceMethodImpl(memberClass, groupSignature);
							if (memberClass.isWorkspaceNode() && workspace.containsMethodSignature(groupSignature, (ClassNode)memberClass)
								|| !memberClass.isWorkspaceNode() && memberClass.findMethod(groupSignature) != null) {
								if (resolvedImplementation == null) {
									determiningClass = memberClass;
									break;
								}

								fallbackClass = resolvedImplementation;
							}
						}

						if (determiningClass == null) {
							determiningClass = fallbackClass;
						}
					}

					AbstractMethodNode determiningMethod = determiningClass.findMethod(groupSignature);
					DebugUtils.assertNotNull(determiningClass, "Failed to find deemed determining interface " + groupSignature + " " + groupMembers);

					for (int detailIndex = 0; detailIndex < groupMembers.size(); detailIndex++) {
						ClassHierarchyNode member = groupMembers.elementAt(detailIndex);
						this.interfaceMethodImplPairMap.put(member.getInternalName(), groupSignature, new KeyValuePair<>(determiningClass, determiningMethod));
					}
				}
			}

			Enumeration<Map<ClassHierarchyNode, String>> relationGroupEnumeration = relationMarkers.getGroups();

			while (relationGroupEnumeration.hasMoreElements()) {
				Map<ClassHierarchyNode, String> relationGroup = relationGroupEnumeration.nextElement();
				if (relationGroup.size() > 1) {
					for (ClassHierarchyNode dependentClass : relationGroup.keySet()) {
						if (relationGroup.get(dependentClass) != "d") {
							for (ClassHierarchyNode relatedClass : relationGroup.keySet()) {
								if (dependentClass != relatedClass) {
									this.relatedInterfaces.addReference(dependentClass, relatedClass);
								}
							}
						}
					}
				}
			}

			this.baseMethodMap = Options.ZKM_USE_PARALLEL ? new ConcurrentHashMap<>() : GlobalUtils.newHashMap();
			if (Options.ZKM_USE_PARALLEL && Options.threadCount >= 2) {
				this.interfaceMethodImplMap
					.getEntrySet()
					.parallelStream()
					.forEach(implementationEntry -> this.buildMethodRedirectionMap((Entry<String, Map<MethodSignature, AbstractClassNode>>)implementationEntry));
			} else {
				for (Entry<String, Map<MethodSignature, AbstractClassNode>> implementationEntry : this.interfaceMethodImplMap.getEntrySet()) {
					this.buildMethodRedirectionMap(implementationEntry);
				}
			}
		}
	}

	public final AtomicReference<List<ClassHierarchyNode>> getMethodImplementationsRef(ClassHierarchyNode classHierarchyNode, MethodSignature methodSignature) {
		return this.interfaceMethodGroups.get(classHierarchyNode, methodSignature);
	}

	public final AbstractClassNode getMethodImplementationClass(AbstractMethodNode methodNode) {
		AbstractMethodNode baseMethod = this.baseMethodMap.get(methodNode);
		return baseMethod != null ? baseMethod.getClassNode() : null;
	}

	public final AbstractClassNode putInterfaceMethodImpl(String internalName, MethodSignature methodSignature, AbstractClassNode implementationClass) {
		return this.interfaceMethodImplMap.put(internalName, methodSignature, implementationClass);
	}

	private boolean checkConflictsInOuterClass(
		ClassHierarchyNode classHierarchyNode,
		AbstractMethodKey newMethodKey,
		AbstractMethodKey oldMethodKey,
		Set<ClassHierarchyNode> changedClasses,
		Set<MethodStateSnapshot> stateSnapshots,
		Set<ClassHierarchyNode> collectedInterfaces,
		AbstractMethodNode methodNode,
		AtomicReference<AbstractMethodKey> conflictingMethodKey
	) {
		ClassHierarchyNode outerClass = classHierarchyNode.getOuterClass();
		if (outerClass != null) {
			int stateChangeResult;
			if (methodNode.isPrivate()) {
				stateChangeResult = this.setPrivateMethodState(outerClass, newMethodKey, conflictingMethodKey);
			} else if (methodNode.isStatic()) {
				stateChangeResult = this.setStaticMethodState(outerClass, newMethodKey, conflictingMethodKey);
			} else {
				stateChangeResult = this.setInstanceMethodStateLenient(outerClass, newMethodKey, oldMethodKey, stateSnapshots, conflictingMethodKey);
			}

			switch (stateChangeResult) {
				case -1:
					this.rollbackState(changedClasses, stateSnapshots, newMethodKey);
					return false;
				case 0:
					changedClasses.add(outerClass);
				default:
					outerClass.collectInterfaces(collectedInterfaces);
					if (!this.checkConflictsInOuterClass(outerClass, newMethodKey, oldMethodKey, changedClasses, stateSnapshots, collectedInterfaces, methodNode, conflictingMethodKey)) {
						return false;
					}
					break;
				case 1:
				case 2:
					return true;
			}
		}

		return true;
	}

	public final AbstractClassNode putMethodOwner(String internalName, MethodSignature methodSignature, AbstractClassNode ownerClass) {
		return this.methodOwnerMap.put(internalName, methodSignature, ownerClass);
	}

	public final boolean checkMethodChange(
		ClassHierarchyNode classHierarchyNode, MethodSignature newMethodSignature, MethodSignature oldMethodSignature, AbstractMethodNode methodNode, AtomicReference<AbstractMethodKey> conflictingMethodKey
	) {
		AbstractMethodKey newMethodKey;
		AbstractMethodKey oldMethodKey;
		if (this.useFullSignature) {
			newMethodKey = newMethodSignature;
			oldMethodKey = oldMethodSignature;
		} else {
			newMethodKey = newMethodSignature.getParameterKey();
			oldMethodKey = oldMethodSignature.getParameterKey();
		}

		HashSet<ClassHierarchyNode> collectedInterfaces = GlobalUtils.newHashSet(17);
		HashSet<ClassHierarchyNode> changedClasses = GlobalUtils.newHashSet();
		LinkedHashSet<MethodStateSnapshot> stateSnapshots = new LinkedHashSet<>();
		if (this.checkConflictsInHierarchy(classHierarchyNode, newMethodKey, oldMethodKey, changedClasses, stateSnapshots, collectedInterfaces, methodNode, conflictingMethodKey)) {
			if (methodNode.isConstructor()) {
				return true;
			} else {
				boolean outerClassesOk = this.checkConflictsInOuterClass(classHierarchyNode, newMethodKey, oldMethodKey, changedClasses, stateSnapshots, collectedInterfaces, methodNode, conflictingMethodKey);
				if (outerClassesOk) {
					for (ClassHierarchyNode interfaceNode : collectedInterfaces) {
						if (!this.checkInterfaceImplConflict(interfaceNode, newMethodKey, oldMethodKey, changedClasses, stateSnapshots, conflictingMethodKey)) {
							return false;
						}
					}

					return true;
				} else {
					return false;
				}
			}
		} else {
			return false;
		}
	}

	public Map<AtomicReference<List<ClassHierarchyNode>>, MethodSignature> getAllImplementationRefs() {
		HashMap<AtomicReference<List<ClassHierarchyNode>>, MethodSignature> signaturesByImplementationRef = GlobalUtils.newHashMap();
		Iterator<Entry<ClassHierarchyNode, Map<MethodSignature, AtomicReference<List<ClassHierarchyNode>>>>> groupEntryIterator = this.interfaceMethodGroups
			.getEntrySet()
			.iterator();

		while (groupEntryIterator.hasNext()) {
			for (Entry<MethodSignature, AtomicReference<List<ClassHierarchyNode>>> entry : groupEntryIterator.next().getValue().entrySet()) {
				MethodSignature methodSignature = entry.getKey();
				AtomicReference<List<ClassHierarchyNode>> implementationsRef = entry.getValue();
				signaturesByImplementationRef.put(implementationsRef, methodSignature);
			}
		}

		return signaturesByImplementationRef;
	}

	private boolean checkImplementerStates(
		ClassHierarchyNode classHierarchyNode,
		AbstractMethodKey newMethodKey,
		AbstractMethodKey oldMethodKey,
		Set<ClassHierarchyNode> changedClasses,
		Set<MethodStateSnapshot> stateSnapshots,
		AtomicReference<AbstractMethodKey> conflictingMethodKey
	) {
		Set<ClassHierarchyNode> implementers = this.getImplementers(classHierarchyNode);
		if (implementers != null) {
			for (ClassHierarchyNode implementer : implementers) {
				int stateChangeResult;
				if (this.containsInterfaceMethodImpl(implementer.getInternalName(), new MethodSignature(oldMethodKey))) {
					stateChangeResult = this.setInstanceMethodState(implementer, newMethodKey, oldMethodKey, conflictingMethodKey);
				} else {
					stateChangeResult = this.setInstanceMethodStateLenient(implementer, newMethodKey, oldMethodKey, stateSnapshots, conflictingMethodKey);
				}

				switch (stateChangeResult) {
					case -1:
						this.rollbackState(changedClasses, stateSnapshots, newMethodKey);
						return false;
					case 0:
						changedClasses.add(classHierarchyNode);
					case 1:
					case 2:
				}
			}
		}

		return true;
	}

	public final boolean isConflicting(MethodSignature methodSignature) {
		AbstractMethodKey methodKey;
		if (this.useFullSignature) {
			methodKey = methodSignature;
		} else {
			methodKey = methodSignature.getParameterKey();
		}

		return this.excludedMethodKeys.contains(methodKey);
	}

	public final AbstractClassNode resolveInterfaceMethodImpl(AbstractClassNode classNode, MethodSignature methodSignature) {
		String internalName = classNode.getInternalName();
		HashSet<AbstractClassNode> seenClasses = null;
		AbstractClassNode currentImplementation = this.interfaceMethodImplMap.get(internalName, methodSignature);
		AbstractClassNode lastImplementation = null;

		while (currentImplementation != null) {
			lastImplementation = currentImplementation;
			currentImplementation = this.interfaceMethodImplMap.get(currentImplementation.getInternalName(), methodSignature);
			if (currentImplementation != null) {
				if (seenClasses == null) {
					seenClasses = GlobalUtils.newHashSet(13);
				}

				if (!seenClasses.add(currentImplementation)) {
					StringBuffer loopPathBuffer = new StringBuffer();
					Iterator<AbstractClassNode> classIterator = seenClasses.iterator();

					while (classIterator.hasNext()) {
						AbstractClassNode visitedClass = classIterator.next();
						loopPathBuffer.append("'" + visitedClass.getInternalName() + "'");
						if (classIterator.hasNext()) {
							loopPathBuffer.append(", ");
						}
					}

					DebugUtils.assertTrue(false, new String[]{"LOOP detected..." + methodSignature + Options.lineSeparator + loopPathBuffer});
				}
			}
		}

		return lastImplementation;
	}

	public final void setContextString(AtomicReference<List<ClassHierarchyNode>> implementationsRef, String contextString) {
		this.methodGroupContext.get(implementationsRef).set(contextString);
	}

	public Set<AbstractMethodNode> getMethodAndOverrides(AbstractMethodNode methodNode) {
		if (!methodNode.isStatic() && !methodNode.isPrivate()) {
			if (this.methodOverrideGraph == null) {
				this.initMethodOverrideGraph();
			}

			AbstractMethodNode baseMethod = this.getBaseMethod(methodNode);
			if (baseMethod == null) {
				baseMethod = methodNode;
			}

			Set<AbstractMethodNode> overrides = this.methodOverrideGraph.getReferences(baseMethod);
			HashSet<AbstractMethodNode> methodAndOverrides = null;
			if (overrides != null) {
				methodAndOverrides = GlobalUtils.newHashSet(overrides);
				methodAndOverrides.add(baseMethod);
			}

			return methodAndOverrides;
		} else {
			return null;
		}
	}

	public final boolean checkMethodChangeWithFlag(
		ClassHierarchyNode classHierarchyNode, MethodSignature newMethodSignature, MethodSignature oldMethodSignature, boolean specialStateCheck, AtomicReference<AbstractMethodKey> conflictingMethodKey
	) {
		AbstractMethodKey newMethodKey;
		AbstractMethodKey oldMethodKey;
		if (this.useFullSignature) {
			newMethodKey = newMethodSignature;
			oldMethodKey = oldMethodSignature;
		} else {
			newMethodKey = newMethodSignature.getParameterKey();
			oldMethodKey = oldMethodSignature.getParameterKey();
		}

		HashSet<ClassHierarchyNode> implementingClasses = GlobalUtils.newHashSet();
		HashSet<ClassHierarchyNode> dependentClasses = GlobalUtils.newHashSet();
		this.collectMethodImplementations(classHierarchyNode, newMethodKey, oldMethodKey, implementingClasses, dependentClasses);
		HashSet<ClassHierarchyNode> changedClasses = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(implementingClasses.size() + dependentClasses.size()));
		LinkedHashSet<MethodStateSnapshot> stateSnapshots = new LinkedHashSet<>();

		for (ClassHierarchyNode implementingClass : implementingClasses) {
			switch (this.setInstanceMethodState(implementingClass, newMethodKey, oldMethodKey, conflictingMethodKey)) {
				case -1:
					this.rollbackState(changedClasses, stateSnapshots, newMethodKey);
					return false;
				case 0:
					changedClasses.add(implementingClass);
				case 1:
				case 2:
				default:
					if (!this.checkImplementerStates(implementingClass, newMethodKey, oldMethodKey, changedClasses, stateSnapshots, conflictingMethodKey)) {
						return false;
					}
			}
		}

		for (ClassHierarchyNode dependentClass : dependentClasses) {
			int stateChangeResult;
			if (specialStateCheck) {
				stateChangeResult = this.setInstanceMethodStateStrict(dependentClass, newMethodKey, oldMethodKey, stateSnapshots, conflictingMethodKey);
			} else {
				stateChangeResult = this.setInstanceMethodStateLenient(dependentClass, newMethodKey, oldMethodKey, stateSnapshots, conflictingMethodKey);
			}

			switch (stateChangeResult) {
				case -1:
					this.rollbackState(changedClasses, stateSnapshots, newMethodKey);
					return false;
				case 0:
					changedClasses.add(dependentClass);
				case 1:
				case 2:
			}
		}

		return true;
	}

	public final boolean checkMethodChangeForGroup(
		AtomicReference<List<ClassHierarchyNode>> implementationsRef, MethodSignature newMethodSignature, MethodSignature oldMethodSignature, AtomicReference<AbstractMethodKey> conflictingMethodKey, ParameterPattern parameterPattern
	) {
		List<?> implementations = implementationsRef.get();
		AbstractMethodKey newMethodKey;
		AbstractMethodKey oldMethodKey;
		if (this.useFullSignature) {
			newMethodKey = newMethodSignature;
			oldMethodKey = oldMethodSignature;
		} else {
			newMethodKey = newMethodSignature.getParameterKey();
			oldMethodKey = oldMethodSignature.getParameterKey();
		}

		HashSet<ClassHierarchyNode> changedClasses = GlobalUtils.newHashSet();
		LinkedHashSet<MethodStateSnapshot> stateSnapshots = new LinkedHashSet<>();

		for (int implementationIndex = 0; implementationIndex < implementations.size(); implementationIndex++) {
			ClassHierarchyNode implementationNode = (ClassHierarchyNode)implementations.get(implementationIndex);
			switch (this.setInstanceMethodState(implementationNode, newMethodKey, oldMethodKey, conflictingMethodKey)) {
				case -1:
					this.rollbackState(changedClasses, stateSnapshots, newMethodKey);
					return false;
				case 0:
					changedClasses.add(implementationNode);
					break;
				case 1:
				case 2:
			}
		}

		if (!this.useParameterPattern) {
			this.setContextString(implementationsRef, newMethodKey.getName());
		} else {
			this.setContextParameterPattern(implementationsRef, parameterPattern);
		}

		return true;
	}

	public final Map<MethodSignature, AbstractClassNode> putAllMethodOwners(String internalName, Map<MethodSignature, AbstractClassNode> ownersBySignature) {
		return this.methodOwnerMap.putGroup(internalName, ownersBySignature);
	}

	private boolean containsInterfaceMethodImpl(String internalName, MethodSignature methodSignature) {
		return this.interfaceMethodImplMap.contains(internalName, methodSignature);
	}

	private int setInstanceMethodState(ClassHierarchyNode classHierarchyNode, AbstractMethodKey newMethodKey, AbstractMethodKey oldMethodKey, AtomicReference<AbstractMethodKey> conflictingMethodKey) {
		if (classHierarchyNode.isMissing()) {
			return 2;
		} else {
			MethodState newState = new MethodState(oldMethodKey);
			MethodState previousState = this.methodStateMap.put(classHierarchyNode, newMethodKey, newState);
			if (previousState == null) {
				return 0;
			} else if (previousState.methodInfo != null && previousState.methodInfo.equals(oldMethodKey)) {
				return 1;
			} else {
				this.methodStateMap.put(classHierarchyNode, newMethodKey, previousState);
				conflictingMethodKey.set(previousState.methodInfo);
				return -1;
			}
		}
	}

	public boolean isMethodRedirected(AbstractMethodNode methodNode) {
		return this.baseMethodMap.containsKey(methodNode);
	}

	private void buildMethodRedirectionMap(Entry<String, Map<MethodSignature, AbstractClassNode>> implementationEntry) {
		ClassHierarchyNode classHierarchyNode = ClassHierarchyNode.getNodeByInternalName(implementationEntry.getKey());
		if (classHierarchyNode != null && !classHierarchyNode.isMissing()) {
			AbstractClassNode classNode = classHierarchyNode.getClassNode();
			Map<MethodSignature, AbstractClassNode> implementationsBySignature = implementationEntry.getValue();
			if (implementationsBySignature != null) {
				for (MethodSignature methodSignature : implementationsBySignature.keySet()) {
					AbstractClassNode implementationClass = implementationsBySignature.get(methodSignature);
					AbstractMethodNode methodNode = classNode.findMethod(methodSignature);
					if (methodNode != null && !methodNode.isStatic() && !methodNode.isPrivate()) {
						AbstractMethodNode implementationMethod = implementationClass.findMethod(methodSignature);
						boolean anotherLevel;
						KeyValuePair<AbstractClassNode, AbstractMethodNode> details;
						if (implementationClass.isInterface() && (details = this.interfaceMethodImplPairMap.get(implementationClass.getInternalName(), methodSignature)) != null) {
							AbstractClassNode determiningInterface = details.getKey();
							implementationMethod = details.getValue();
						} else {
							do {
								anotherLevel = false;
								AbstractClassNode redirectedImplementation = this.interfaceMethodImplMap.get(implementationClass.getInternalName(), methodSignature);
								if (redirectedImplementation != null) {
									implementationClass = redirectedImplementation;
									implementationMethod = redirectedImplementation.findMethod(methodSignature);
									anotherLevel = true;
								}
							} while (anotherLevel);
						}

						this.baseMethodMap.put(methodNode, implementationMethod);
					}
				}
			}
		}
	}

	public boolean checkConstructorConflict(
		ClassHierarchyNode classHierarchyNode, MethodSignature newMethodSignature, MethodSignature oldMethodSignature, AbstractMethodNode methodNode, AtomicReference<AbstractMethodKey> conflictingMethodKey
	) {
		AbstractMethodKey methodKey;
		if (this.useFullSignature) {
			methodKey = newMethodSignature;
		} else {
			methodKey = newMethodSignature.getParameterKey();
		}

		return this.setPrivateMethodState(classHierarchyNode, methodKey, conflictingMethodKey) != -1;
	}

	private void collectImplementingClasses(
		ConcurrentHashSet<ClassHierarchyNode> implementingClasses, ClassHierarchyNode classHierarchyNode, MultiMap<ClassHierarchyNode, ClassHierarchyNode> implementersByInterface
	) {
		if (implementingClasses.add(classHierarchyNode)) {
			List<ClassHierarchyNode> implementers = implementersByInterface.get(classHierarchyNode);
			if (implementers != null) {
				for (int i = 0; i < implementers.size(); i++) {
					ClassHierarchyNode implementer = implementers.get(i);
					this.collectImplementingClasses(implementingClasses, implementer, implementersByInterface);
				}
			}
		}
	}

	public final boolean checkInterfaceMethodChange(ClassHierarchyNode classHierarchyNode, MethodSignature newMethodSignature, MethodSignature oldMethodSignature, AtomicReference<AbstractMethodKey> conflictingMethodKey) {
		return this.checkMethodChangeWithFlag(classHierarchyNode, newMethodSignature, oldMethodSignature, false, conflictingMethodKey);
	}

	public final String getContextString(AtomicReference<List<ClassHierarchyNode>> implementationsRef) {
		return (String)this.methodGroupContext.get(implementationsRef).get();
	}

	private final Set<ClassHierarchyNode> getImplementers(ClassHierarchyNode classHierarchyNode) {
		return this.classDependencies.getReferences(classHierarchyNode);
	}

	private int setPrivateMethodState(ClassHierarchyNode classHierarchyNode, AbstractMethodKey methodKey, AtomicReference<AbstractMethodKey> conflictingMethodKey) {
		if (classHierarchyNode.isMissing()) {
			return 2;
		} else {
			MethodState previousState = this.methodStateMap.put(classHierarchyNode, methodKey, PRIVATE);
			if (previousState == null) {
				return 0;
			} else if (previousState == PRIVATE) {
				return 1;
			} else if (previousState == STATIC) {
				this.methodStateMap.put(classHierarchyNode, methodKey, previousState);
				return 1;
			} else if (previousState == INSTANCE) {
				this.methodStateMap.put(classHierarchyNode, methodKey, INSTANCE);
				return 1;
			} else {
				this.methodStateMap.put(classHierarchyNode, methodKey, previousState);
				conflictingMethodKey.set(previousState.methodInfo);
				return -1;
			}
		}
	}

	public final boolean areMethodsDifferent(ClassHierarchyNode classHierarchyNode, MethodSignature newMethodSignature, MethodSignature oldMethodSignature) {
		AbstractMethodKey oldMethodKey;
		AbstractMethodKey newMethodKey;
		if (this.useFullSignature) {
			oldMethodKey = oldMethodSignature;
			newMethodKey = newMethodSignature;
		} else {
			oldMethodKey = oldMethodSignature.getParameterKey();
			newMethodKey = newMethodSignature.getParameterKey();
		}

		MethodState currentState = this.methodStateMap.get(classHierarchyNode, newMethodKey);
		return currentState == null ? false : currentState.methodInfo == null || !currentState.methodInfo.equals(oldMethodKey);
	}

	private void rollbackState(Set<ClassHierarchyNode> changedClasses, Set<MethodStateSnapshot> stateSnapshots, AbstractMethodKey methodKey) {
		for (ClassHierarchyNode changedClass : changedClasses) {
			this.methodStateMap.remove(changedClass, methodKey);
		}

		ArrayList<MethodStateSnapshot> reversedSnapshots = new ArrayList<>(stateSnapshots);
		Collections.reverse(reversedSnapshots);

		for (MethodStateSnapshot snapshot : reversedSnapshots) {
			this.methodStateMap.put(snapshot.classNode, methodKey, snapshot.previousState);
		}

		changedClasses.clear();
		reversedSnapshots.clear();
	}

	private Map<MethodSignature, AbstractClassNode> getMethodOwnerGroup(String internalName) {
		return this.methodOwnerMap.getGroup(internalName);
	}

	public final Map<MethodSignature, AbstractClassNode> putAllInterfaceMethodImpls(String internalName, Map<MethodSignature, AbstractClassNode> implementationsBySignature) {
		return this.interfaceMethodImplMap.putGroup(internalName, implementationsBySignature);
	}

	private void collectMethodImplementations(
		ClassHierarchyNode classHierarchyNode, AbstractMethodKey newMethodKey, AbstractMethodKey oldMethodKey, Set<ClassHierarchyNode> implementingClasses, Set<ClassHierarchyNode> dependentClasses
	) {
		AtomicReference<List<ClassHierarchyNode>> implementationsRef = this.getMethodImplementationsRef(classHierarchyNode, new MethodSignature(oldMethodKey));
		if (implementationsRef != null) {
			List<?> implementations = implementationsRef.get();

			for (int i = 0; i < implementations.size(); i++) {
				ClassHierarchyNode implementationNode = (ClassHierarchyNode)implementations.get(i);
				implementingClasses.add(implementationNode);
			}
		} else {
			implementingClasses.add(classHierarchyNode);
		}

		for (ClassHierarchyNode implementingClass : implementingClasses) {
			this.addRelatedInterfaces(implementingClass, dependentClasses);
		}
	}

	private boolean checkInterfaceImplConflict(
		ClassHierarchyNode classHierarchyNode,
		AbstractMethodKey newMethodKey,
		AbstractMethodKey oldMethodKey,
		Set<ClassHierarchyNode> changedClasses,
		Set<MethodStateSnapshot> stateSnapshots,
		AtomicReference<AbstractMethodKey> conflictingMethodKey
	) {
		switch (this.setInstanceMethodStateLenient(classHierarchyNode, newMethodKey, oldMethodKey, stateSnapshots, conflictingMethodKey)) {
			case -1:
				this.rollbackState(changedClasses, stateSnapshots, newMethodKey);
				return false;
			case 0:
				changedClasses.add(classHierarchyNode);
			case 1:
			case 2:
			default:
				return true;
		}
	}

	private final void buildClassReferences(
		ClassHierarchyNode originClass, ClassHierarchyNode classHierarchyNode, boolean checkOuterClasses, Set<ClassHierarchyNode> implementingClasses, DualKeyMap<ClassHierarchyNode, ClassHierarchyNode, String> relationMarkers
	) {
		if (checkOuterClasses && !classHierarchyNode.isInterface()) {
			checkOuterClasses = false;
			ArrayList<ClassHierarchyNode> outerClasses = new ArrayList<>(5);
			classHierarchyNode.collectOuterClasses(outerClasses);

			for (int outerIndex = 0; outerIndex < outerClasses.size(); outerIndex++) {
				ClassHierarchyNode outerClass = outerClasses.get(outerIndex);
				if (relationMarkers.contains(outerClass, originClass)) {
					break;
				}

				relationMarkers.put(outerClass, originClass, "d");
				implementingClasses.add(outerClass);
			}
		}

		Enumeration<ClassHierarchyNode> subclassesEnumeration = classHierarchyNode.getSubclassesEnumeration();
		if (subclassesEnumeration != null) {
			while (subclassesEnumeration.hasMoreElements()) {
				ClassHierarchyNode subclassNode = subclassesEnumeration.nextElement();
				relationMarkers.put(subclassNode, originClass, "i");
				this.buildClassReferences(originClass, subclassNode, checkOuterClasses, implementingClasses, relationMarkers);
				implementingClasses.add(subclassNode);
			}
		}

		Enumeration<ClassHierarchyNode> implementersEnumeration = classHierarchyNode.getImplementersEnumeration();
		if (implementersEnumeration != null) {
			while (implementersEnumeration.hasMoreElements()) {
				ClassHierarchyNode implementerNode = implementersEnumeration.nextElement();
				relationMarkers.put(implementerNode, originClass, "i");
				this.buildClassReferences(originClass, implementerNode, checkOuterClasses, implementingClasses, relationMarkers);
				if (!implementerNode.isInterface()) {
					implementingClasses.add(implementerNode);
				}
			}
		}
	}

	private int setStaticMethodState(ClassHierarchyNode classHierarchyNode, AbstractMethodKey methodKey, AtomicReference<AbstractMethodKey> conflictingMethodKey) {
		if (classHierarchyNode.isMissing()) {
			return 2;
		} else {
			MethodState previousState = this.methodStateMap.put(classHierarchyNode, methodKey, STATIC);
			if (previousState == null) {
				return 0;
			} else if (previousState == PRIVATE) {
				this.methodStateMap.put(classHierarchyNode, methodKey, previousState);
				return 1;
			} else if (previousState == STATIC) {
				return 1;
			} else if (previousState == INSTANCE) {
				this.methodStateMap.put(classHierarchyNode, methodKey, INSTANCE);
				return 1;
			} else {
				this.methodStateMap.put(classHierarchyNode, methodKey, previousState);
				conflictingMethodKey.set(previousState.methodInfo);
				return -1;
			}
		}
	}

	public final boolean checkGroupMethodChange(
		AtomicReference<List<ClassHierarchyNode>> implementationsRef, MethodSignature newMethodSignature, MethodSignature oldMethodSignature, AtomicReference<AbstractMethodKey> conflictingMethodKey
	) {
		return this.checkMethodChangeForGroup(implementationsRef, newMethodSignature, oldMethodSignature, conflictingMethodKey, null);
	}

	public final void setContextParameterPattern(AtomicReference<List<ClassHierarchyNode>> implementationsRef, ParameterPattern parameterPattern) {
		this.methodGroupContext.get(implementationsRef).set(parameterPattern);
	}

	public boolean isMethodInOverrideGraph(AbstractMethodNode methodNode) {
		if (this.methodOverrideGraph == null) {
			this.initMethodOverrideGraph();
		}

		return this.methodOverrideGraph.hasMember(methodNode);
	}

	private int setInstanceMethodStateLenient(
		ClassHierarchyNode classHierarchyNode, AbstractMethodKey newMethodKey, AbstractMethodKey oldMethodKey, Set<MethodStateSnapshot> stateSnapshots, AtomicReference<AbstractMethodKey> conflictingMethodKey
	) {
		if (classHierarchyNode.isMissing()) {
			return 2;
		} else {
			MethodState previousState = this.methodStateMap.put(classHierarchyNode, newMethodKey, INSTANCE);
			if (previousState == null) {
				return 0;
			} else if (previousState == PRIVATE || previousState == STATIC) {
				MethodStateSnapshot snapshot = new MethodStateSnapshot(classHierarchyNode, previousState, INSTANCE);
				stateSnapshots.add(snapshot);
				return 0;
			} else if (previousState == INSTANCE) {
				return 1;
			} else if (previousState.methodInfo != null && previousState.methodInfo.equals(oldMethodKey)) {
				this.methodStateMap.put(classHierarchyNode, newMethodKey, previousState);
				return 1;
			} else {
				this.methodStateMap.put(classHierarchyNode, newMethodKey, previousState);
				conflictingMethodKey.set(previousState.methodInfo);
				return -1;
			}
		}
	}

	public final Map<MethodSignature, AbstractClassNode> getOrLoadClassMethodMap(
		String internalName, AtomicReference<Boolean> integrityResult, ScriptContext scriptContext, Random random, String context
	) {
		Map<MethodSignature, AbstractClassNode> ownersBySignature = this.getMethodOwnerGroup(internalName);
		if (ownersBySignature == null) {
			AbstractClassNode classNode;
			try {
				classNode = this.classProvider.getClassNode(internalName, context);
			} catch (ClasspathException classpathException) {
				throw new HierarchyClassNotFoundException("Class '" + GlobalUtils.internalToBinaryName(classpathException.getClassName()) + "' not found while " + context + ". (1)");
			} catch (FileException fileException) {
				throw new HierarchyClassNotFoundException(fileException.getMessage());
			}

			ownersBySignature = classNode.buildMethodOverrideMap(this, integrityResult, scriptContext, random);
		}

		return ownersBySignature;
	}

	public boolean usesFullSignature() {
		return this.useFullSignature;
	}

	public final AbstractMethodNode getBaseMethodOrDefault(AbstractMethodNode methodNode) {
		AbstractMethodNode baseMethod = this.baseMethodMap.get(methodNode);
		return baseMethod != null ? baseMethod : methodNode;
	}

	private void initMethodOverrideGraph() {
		AbstractMethodNode[] baseMethods = new AbstractMethodNode[this.baseMethodMap.size()];
		this.baseMethodMap.keySet().toArray(baseMethods);
		this.methodOverrideGraph = new BitSetReferenceGraph<>(baseMethods, this.baseMethodMap.values().size());

		for (Entry<AbstractMethodNode, AbstractMethodNode> baseMethodEntry : this.baseMethodMap.entrySet()) {
			this.methodOverrideGraph.addReference(baseMethodEntry.getValue(), baseMethodEntry.getKey());
		}
	}

	public final ParameterPattern getContextParameterPattern(AtomicReference<List<ClassHierarchyNode>> implementationsRef) {
		return (ParameterPattern)this.methodGroupContext.get(implementationsRef).get();
	}

	private Map<MethodSignature, AbstractClassNode> getInterfaceMethodImplGroup(String internalName) {
		return this.interfaceMethodImplMap.getGroup(internalName);
	}

	public final boolean hasMethodState(ClassHierarchyNode classHierarchyNode, AbstractMethodKey methodKey) {
		return this.methodStateMap.contains(classHierarchyNode, methodKey);
	}

	public final AbstractMethodNode getBaseMethod(AbstractMethodNode methodNode) {
		return this.baseMethodMap.get(methodNode);
	}

	private int setInstanceMethodStateStrict(
		ClassHierarchyNode classHierarchyNode, AbstractMethodKey newMethodKey, AbstractMethodKey oldMethodKey, Set<MethodStateSnapshot> stateSnapshots, AtomicReference<AbstractMethodKey> conflictingMethodKey
	) {
		if (classHierarchyNode.isMissing()) {
			return 2;
		} else {
			MethodState previousState = this.methodStateMap.put(classHierarchyNode, newMethodKey, INSTANCE);
			if (previousState == null) {
				return 0;
			} else if (previousState == PRIVATE || previousState == STATIC) {
				this.methodStateMap.put(classHierarchyNode, newMethodKey, previousState);
				conflictingMethodKey.set(previousState.methodInfo);
				return -1;
			} else if (previousState == INSTANCE) {
				this.methodStateMap.put(classHierarchyNode, newMethodKey, previousState);
				conflictingMethodKey.set(previousState.methodInfo);
				return -1;
			} else if (previousState.methodInfo != null && previousState.methodInfo.equals(oldMethodKey)) {
				this.methodStateMap.put(classHierarchyNode, newMethodKey, previousState);
				return 1;
			} else {
				this.methodStateMap.put(classHierarchyNode, newMethodKey, previousState);
				conflictingMethodKey.set(previousState.methodInfo);
				return -1;
			}
		}
	}

	private void addRelatedInterfaces(ClassHierarchyNode classHierarchyNode, Set<ClassHierarchyNode> dependentClasses) {
		Set<ClassHierarchyNode> relatedInterfaceSet = this.relatedInterfaces.getReferences(classHierarchyNode);
		if (relatedInterfaceSet != null) {
			for (ClassHierarchyNode relatedInterface : relatedInterfaceSet) {
				dependentClasses.add(relatedInterface);
			}
		}
	}

	public static class MethodStateSnapshot {
		public final ClassHierarchyNode classNode;
		public final MethodState previousState;
		public final MethodState targetState;

		public MethodStateSnapshot(ClassHierarchyNode classNode, MethodState previousState, MethodState targetState) {
			this.classNode = classNode;
			this.previousState = previousState;
			this.targetState = targetState;
		}
	}
}
