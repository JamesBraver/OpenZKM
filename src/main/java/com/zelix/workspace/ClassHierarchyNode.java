package com.zelix.workspace;

import com.zelix.event.ChangeListener;
import com.zelix.event.INotifier;
import com.zelix.exceptions.ClasspathException;
import com.zelix.exceptions.FileException;
import com.zelix.exceptions.HierarchyClassNotFoundException;
import com.zelix.exceptions.MethodAnalyzerException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.WritableType;
import com.zelix.framework.constant.IntegerNode;
import com.zelix.framework.constant.LongNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.hierarchy.Inheritance;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.instructions.WideConstantPoolInsnNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.signature.AbstractMethodKey;
import com.zelix.framework.signature.MethodParameterKey;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.variable.LocalVariableOperand;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.LibraryClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.repository.Hierarchy;
import com.zelix.tools.excludes.DefaultExcludes;
import com.zelix.tools.integerEncryption.IntegerEncryptionBuilder;
import com.zelix.tools.integerEncryption.IntegerEncryptionExcludes;
import com.zelix.tools.longEncryption.LongEncryptionBuilder;
import com.zelix.tools.longEncryption.LongEncryptionExcludes;
import com.zelix.tools.parameterChanger.ParameterChangerBuilder;
import com.zelix.tools.parameterChanger.ParameterChangerExcludes;
import com.zelix.tools.parameterChanger.ParameterManager;
import com.zelix.tools.parameterChanger.extra.ExtraParameterBuilder;
import com.zelix.tools.renamer.ClassRenamer;
import com.zelix.tools.renamer.MethodRenamer;
import com.zelix.tools.renamer.method.AbstractMethodNameGenerator;
import com.zelix.tools.string.StringEncryptionBuilder;
import com.zelix.tools.string.StringEncryptionExcludes;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.CountingSet;
import com.zelix.utils.collections.KeyValuePairMultiMap;
import com.zelix.utils.collections.MethodNameMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.NestedMultiMap;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.PriorityEntry;
import com.zelix.utils.enumerations.EmptyEnumeration;
import com.zelix.utils.references.AtomicReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClassHierarchyNode implements ChangeListener, Comparable<ClassHierarchyNode> {
	private static boolean hierarchyValidationEnabled = false;
	private static Map<String, ClassHierarchyNode> hierarchyNodeRegistry = Options.ZKM_USE_PARALLEL
		? new ConcurrentHashMap<>()
		: GlobalUtils.newHashMap();
	private List<ClassHierarchyNode> directSubclasses = null;
	private List<ClassHierarchyNode> directInterfaces = null;
	private List<ClassHierarchyNode> directImplementers = null;
	private List<ClassHierarchyNode> directInnerClasses = null;
	private String internalName;
	private AbstractClassNode classNode;
	private int hierarchyDepth;
	private ClassHierarchyNode outerClass;
	private ClassHierarchyNode superClassNode;
	private ClassHierarchyNode superClassLink;

	private ClassHierarchyNode(String internalName, AbstractClassNode classNode) {
		this.internalName = internalName;
		this.classNode = classNode;
		if (this.classNode != null) {
			this.classNode.subscribe(this);
		}
	}

	public static ClassHierarchyNode getNodeByInternalName(String internalName) {
		return hierarchyNodeRegistry.get(internalName);
	}

	public static synchronized void clearRegistry() {
		hierarchyNodeRegistry = Options.ZKM_USE_PARALLEL ? new ConcurrentHashMap<>() : GlobalUtils.newHashMap();
	}

	public static synchronized void rebuildRegistry() {
		Collection<ClassHierarchyNode> nodes = hierarchyNodeRegistry.values();
		clearRegistry();

		for (ClassHierarchyNode node : nodes) {
			hierarchyNodeRegistry.put(node.internalName, node);
		}
	}

	public static void pruneRemovedNodes(HashSet<ClassNode> removedClasses) {
		for (ClassHierarchyNode node : hierarchyNodeRegistry.values()) {
			if (removedClasses.contains(node.getClassNodeIfWorkspace())) {
				node.superClassNode = null;
				node.superClassLink = null;
				node.directInnerClasses = null;
			} else {
				if (node.superClassNode != null && node.superClassNode.classNode != null && removedClasses.contains(node.superClassNode.classNode)) {
					node.superClassNode = null;
				}

				if (node.superClassLink != null && node.superClassLink.classNode != null && removedClasses.contains(node.superClassLink.classNode)) {
					node.superClassLink = null;
				}

				if (node.directInnerClasses != null) {
					Iterator<ClassHierarchyNode> innerClassIterator = node.directInnerClasses.iterator();

					while (innerClassIterator.hasNext()) {
						ClassHierarchyNode innerClass = innerClassIterator.next();
						if (innerClass.classNode != null && removedClasses.contains(innerClass.classNode)) {
							innerClassIterator.remove();
						}
					}

					if (node.directInnerClasses.size() == 0) {
						node.directInnerClasses = null;
					}
				}
			}
		}
	}

	public static String validateInheritanceLink(AbstractClassNode unopenedClass, AbstractClassNode openedClass, int linkType) {
		if (!hierarchyValidationEnabled) {
			return null;
		} else if (isMissingClass(unopenedClass.getInternalName()) && !isMissingClass(openedClass.getInternalName())) {
			String relationVerb = null;
			switch (linkType) {
				case 1:
					relationVerb = "extended";
					break;
				case 2:
					relationVerb = "implemented";
			}

			return "Class '"
				+ openedClass.getBinaryName()
				+ "' in file '"
				+ openedClass.getSourcePath()
				+ "' has been opened and is "
				+ relationVerb
				+ " by class '"
				+ unopenedClass.getBinaryName()
				+ "' in file '"
				+ unopenedClass.getSourcePath()
				+ "'. Class '"
				+ unopenedClass.getBinaryName()
				+ "' is used by the opened classes but it has not been opened.  Either '"
				+ openedClass.getBinaryName()
				+ "' should not be opened or '"
				+ unopenedClass.getBinaryName()
				+ "' must also be opened for obfuscation. (B)";
		} else {
			return null;
		}
	}

	public static void resetAllLinks() {
		for (ClassHierarchyNode node : hierarchyNodeRegistry.values()) {
			node.superClassNode = null;
			node.superClassLink = null;
			node.directInnerClasses = null;
		}
	}

	public static Enumeration<ClassHierarchyNode> getAllNodesEnumeration() {
		return Collections.enumeration(hierarchyNodeRegistry.values());
	}

	public static final boolean isMissingClass(String internalName) {
		ClassHierarchyNode node = hierarchyNodeRegistry.get(internalName);
		return node == null ? true : node.isMissing();
	}

	public static boolean isWorkspaceClassByInternalName(String internalName) {
		ClassHierarchyNode node = getNodeByInternalName(internalName);
		return node != null && node.isWorkspaceClass();
	}

	public static AbstractClassNode getClassNodeByInternalName(String internalName) {
		ClassHierarchyNode node = hierarchyNodeRegistry.get(internalName);
		return node == null ? null : node.classNode;
	}

	public static synchronized void resetAll() {
		Iterator<ClassHierarchyNode> nodeIterator = hierarchyNodeRegistry.values().iterator();

		while (nodeIterator.hasNext()) {
			nodeIterator.next().clear();
		}

		hierarchyNodeRegistry = Options.ZKM_USE_PARALLEL ? new ConcurrentHashMap<>() : GlobalUtils.newHashMap();
	}

	public static void setHierarchyValidationEnabled(boolean enabled) {
		hierarchyValidationEnabled = enabled;
	}

	public static synchronized ClassHierarchyNode getOrCreateNode(String internalName, AbstractClassNode classNode) {
		ClassHierarchyNode node = hierarchyNodeRegistry.get(internalName);
		if (node == null) {
			node = new ClassHierarchyNode(internalName, classNode);
			hierarchyNodeRegistry.put(internalName, node);
		}

		return node;
	}

	public static ClassNode getWorkspaceClassNode(String internalName) {
		ClassHierarchyNode node = getNodeByInternalName(internalName);
		return node == null ? null : node.getClassNodeIfWorkspace();
	}

	public final void detachFromParent() {
		if (this.superClassNode != null && this.superClassNode.directInnerClasses != null) {
			this.superClassNode.directInnerClasses.remove(this);
			if (this.superClassNode.directInnerClasses.size() == 0) {
				this.superClassNode.directInnerClasses = null;
			}
		}

		this.superClassNode = null;
		this.superClassLink = null;
		if (this.directInnerClasses != null) {
			for (int i = 0; i < this.directInnerClasses.size(); i++) {
				ClassHierarchyNode innerClass = this.directInnerClasses.get(i);
				innerClass.superClassNode = null;
				if (innerClass.superClassLink == this) {
					innerClass.superClassLink = null;
				}
			}
		}

		this.directInnerClasses = null;
	}

	public final boolean isInterface() {
		return this.classNode != null && this.classNode.isInterface();
	}

	public final void applyMethodParameterChanges(
		ParameterChangerExcludes parameterChangerExcludes,
		ChangeLogContext changeLogContext,
		ParameterManager parameterManager,
		ExtraParameterBuilder extraParameterBuilder,
		ReferenceGraph<AbstractClassNode, MethodNode> methodProcessingGraph,
		Hierarchy hierarchy,
		ReferenceGraph<AbstractMethodKey, AbstractMethodNode> ancestorMethodReferences,
		int keepInnerClassInfo,
		boolean useFullSignatureMatching,
		boolean randomizeProcessingOrder,
		HashMap<String, String> classNameRemap,
		Map<MethodNode, String> resourceProtectedStaticMethods,
		boolean isParameterObfuscationEnabled
	) {
		((ClassNode)this.classNode)
			.applyMethodParameterChanges(methodProcessingGraph.getReferences(this.classNode), parameterChangerExcludes, changeLogContext, parameterManager, extraParameterBuilder, hierarchy, ancestorMethodReferences, keepInnerClassInfo, useFullSignatureMatching, randomizeProcessingOrder, this, classNameRemap, resourceProtectedStaticMethods, isParameterObfuscationEnabled);
		if (this.directSubclasses != null) {
			int subclassCount = this.directSubclasses.size();

			for (int i = 0; i < subclassCount; i++) {
				this.directSubclasses.get(i).applyMethodParameterChanges(parameterChangerExcludes, changeLogContext, parameterManager, extraParameterBuilder, methodProcessingGraph, hierarchy, ancestorMethodReferences.deepClone(), keepInnerClassInfo, useFullSignatureMatching, randomizeProcessingOrder, classNameRemap, resourceProtectedStaticMethods, isParameterObfuscationEnabled);
			}
		}
	}

	public final ClassNode getClassNodeIfWorkspace() {
		return this.classNode != null && this.classNode.isWorkspaceNode() ? (ClassNode)this.classNode : null;
	}

	public final Enumeration<ClassHierarchyNode> getAncestorsFromLink() {
		if (this.superClassLink == null) {
			return new EmptyEnumeration<>();
		} else {
			HashSet<ClassHierarchyNode> seenAncestors = GlobalUtils.newHashSet(13);
			ArrayList<ClassHierarchyNode> ancestors = new ArrayList<>();
			ClassHierarchyNode currentAncestor = this.superClassLink;

			do {
				ancestors.add(currentAncestor);
				if (!seenAncestors.add(currentAncestor)) {
					StringBuilder ancestorChainBuffer = new StringBuilder();
					ancestorChainBuffer.append('[');

					for (int ancestorIndex = 0; ancestorIndex < ancestors.size(); ancestorIndex++) {
						ClassHierarchyNode ancestor = ancestors.get(ancestorIndex);
						ancestorChainBuffer.append(ancestor.getInternalName() + " " + ancestor.getClassNode().getRawSourcePath() + " " + currentAncestor.isMissing());
						if (ancestorIndex < ancestors.size() - 1) {
							ancestorChainBuffer.append(", ");
						}
					}

					ancestorChainBuffer.append(']');
					DebugUtils.assertTrue(false, new String[]{"Loop : " + ancestorChainBuffer.toString()});
				}

				currentAncestor = currentAncestor.getSuperClassLink();
			} while (currentAncestor != null);

			return Collections.enumeration(ancestors);
		}
	}

	public final void setOuterClass(ClassHierarchyNode node) {
		this.outerClass = node;
	}

	public final boolean isMissing() {
		return this.classNode == null;
	}

	public final void validateRenameOverrides(MethodRenamer methodRenamer) {
		if (!this.isMissing()) {
			this.classNode.validateRenameOverrides(this, methodRenamer);
		}

		if (this.directSubclasses != null) {
			int subclassCount = this.directSubclasses.size();

			for (int i = 0; i < subclassCount; i++) {
				this.directSubclasses.get(i).validateRenameOverrides(methodRenamer);
			}
		}
	}

	public final ClassHierarchyNode getSuperClassLink() {
		return this.superClassLink;
	}

	public final void accumulateSubtreeCounts(AtomicInt totalCount, AtomicInt secondaryTotalCount, CountingSet<ClassNode> counts, CountingSet<ClassNode> secondaryCounts) {
		int subtreeCount = counts.getCount(this.classNode);
		int secondarySubtreeCount = secondaryCounts.getCount(this.classNode);
		if (this.directSubclasses != null) {
			for (int i = 0; i < this.directSubclasses.size(); i++) {
				ClassHierarchyNode subclassNode = this.directSubclasses.get(i);
				AtomicInt childTotalCount = new AtomicInt(0);
				AtomicInt childSecondaryTotalCount = new AtomicInt(0);
				subclassNode.accumulateSubtreeCounts(childTotalCount, childSecondaryTotalCount, counts, secondaryCounts);
				subtreeCount += childTotalCount.get();
				secondarySubtreeCount += childSecondaryTotalCount.get();
			}
		}

		totalCount.set(subtreeCount);
		secondaryTotalCount.set(secondarySubtreeCount);
		if (this.classNode.isWorkspaceNode()) {
			counts.setCount((ClassNode)this.classNode, subtreeCount);
			secondaryCounts.setCount((ClassNode)this.classNode, secondarySubtreeCount);
		}
	}

	public final boolean hasImplementers() {
		return this.directImplementers != null && this.directImplementers.size() > 0;
	}

	public final void remapInnerClasses(HashMap<String, String> internalNameRemap, HashMap<String, String> classNameRemap) {
		if (this.directInnerClasses != null) {
			for (int i = 0; i < this.directInnerClasses.size(); i++) {
				ClassHierarchyNode innerClass = this.directInnerClasses.get(i);
				AbstractClassNode innerClassNode = this.directInnerClasses.get(i).classNode;
				if (innerClassNode.isWorkspaceNode()) {
					ClassNode workspaceClass = (ClassNode)innerClassNode;
					ClassHierarchyNode superLink = innerClass.getSuperClassLink();
					if (superLink != null) {
						String superClassInternalName = superLink.getInternalName();
						workspaceClass.remapClassNamePrefix(superClassInternalName, internalNameRemap.get(superClassInternalName), internalNameRemap, classNameRemap);
					}
				}
			}
		}
	}

	public final boolean tryRenameClass(ClassRenamer renamer, HashMap<String, String> classNameRemap, HashMap<String, String> internalNameRemap, Iterator<ClassHierarchyNode> nodeIterator) {
		HashMap<ClassHierarchyNode, String> simpleNamesByAncestor = null;
		if (renamer.getKeepInnerClassInfo() != 1 && this.superClassLink != null) {
			simpleNamesByAncestor = GlobalUtils.newHashMap(13);
			Enumeration<ClassHierarchyNode> ancestors = this.getAncestorsFromLink();

			while (ancestors.hasMoreElements()) {
				ClassHierarchyNode ancestor = ancestors.nextElement();
				String ancestorSimpleName = AbstractClassNode.getSimpleNameFromInnerClass(
					AbstractClassNode.getSimpleNameFromInternalName(GlobalUtils.getMappedOrDefault(ancestor.getInternalName(), classNameRemap))
				);
				simpleNamesByAncestor.put(ancestor, ancestorSimpleName);
			}
		}

		String newClassName = renamer.computeNewClassName(this, simpleNamesByAncestor);
		if (newClassName != null) {
			classNameRemap.put(this.internalName, newClassName);
			String previousInternalName = internalNameRemap.put(newClassName, this.internalName);
			DebugUtils.assertNull(previousInternalName, "Duplicate class '" + this.internalName + "' '" + newClassName + "' '" + previousInternalName + "' (A)");
			nodeIterator.remove();
			return true;
		} else {
			return false;
		}
	}

	public List<ClassNode> getOuterWorkspaceClasses() {
		ArrayList<ClassHierarchyNode> outerClassNodes = new ArrayList<>();
		this.collectOuterClasses(outerClassNodes);
		ArrayList<ClassNode> outerWorkspaceClasses = new ArrayList<>(outerClassNodes.size());

		for (ClassHierarchyNode outerClassNode : outerClassNodes) {
			if (outerClassNode.isWorkspaceClass()) {
				outerWorkspaceClasses.add(outerClassNode.getClassNodeIfWorkspace());
			}
		}

		return outerWorkspaceClasses;
	}

	public final void validateMethodRenames(MethodRenamer methodRenamer) {
		this.classNode.validateMethodRenames(this, methodRenamer);
		if (this.directSubclasses != null) {
			int subclassCount = this.directSubclasses.size();

			for (int i = 0; i < subclassCount; i++) {
				this.directSubclasses.get(i).validateMethodRenames(methodRenamer);
			}
		}
	}

	public final String getClassNameOrInternalName() {
		return this.classNode != null ? this.classNode.getClassName() : this.internalName;
	}

	public final Enumeration<ClassHierarchyNode> getImplementersEnumeration() {
		return this.directImplementers != null ? Collections.enumeration(this.directImplementers) : null;
	}

	public final ClassNode getOuterClassWrapper() {
		return this.outerClass != null ? this.outerClass.getClassNodeIfWorkspace() : null;
	}

	public final void addDirectInterface(ClassHierarchyNode node) {
		if (this.directInterfaces == null) {
			this.directInterfaces = new ArrayList<>(2);
		}

		this.directInterfaces.add(node);
	}

	public final void renameClassWithRepackagedName(
		ClassRenamer renamer, HashMap<String, String> classNameRemap, HashMap<String, String> internalNameRemap, Set<String> newClassNames, Set<String> nonFilesystemPackageNames, Set<String> packageNames
	) {
		String newClassName = renamer.getRepackagedName(this);
		if (newClassName == null) {
			newClassName = this.internalName;
		}

		classNameRemap.put(this.internalName, newClassName);
		String previousInternalName = internalNameRemap.put(newClassName, this.internalName);
		DebugUtils.assertNull(previousInternalName, "Duplicate new class name. Both '" + this.internalName + "' and '" + previousInternalName + "' mapped to '" + newClassName + "'");
		newClassNames.add(newClassName);
		if (!this.getClassNode().isFromFilesystem()) {
			nonFilesystemPackageNames.add(newClassName.toLowerCase());
		}

		packageNames.add(newClassName.toLowerCase());
	}

	public final ClassHierarchyNode getSuperClassNode() {
		return this.superClassNode;
	}

	public final void renameClassWithGeneratedName(ClassRenamer renamer, HashMap<String, String> classNameRemap, HashMap<String, String> internalNameRemap) {
		String newClassName = renamer.generateClassName(this, this.classNode != null ? this.classNode.isFromFilesystem() : false);
		classNameRemap.put(this.internalName, newClassName);
		String previousInternalName = internalNameRemap.put(newClassName, this.internalName);
		DebugUtils.assertNull(previousInternalName, "Duplicate class '" + this.internalName + "' '" + newClassName + "' '" + previousInternalName + "' (B)");
	}

	public final String getName() {
		return this.internalName.replace('/', '.');
	}

	public final void setSuperClassNode(ClassHierarchyNode node) {
		this.superClassNode = node;
	}

	public final void validateOverrideParamChanges(ParameterManager parameterManager) {
		if (!this.isMissing()) {
			this.classNode.validateOverrideParamChanges(this, parameterManager);
		}

		if (this.directSubclasses != null) {
			int subclassCount = this.directSubclasses.size();

			for (int i = 0; i < subclassCount; i++) {
				this.directSubclasses.get(i).validateOverrideParamChanges(parameterManager);
			}
		}
	}

	public final String getInternalName() {
		return this.internalName;
	}

	public final void collectInterfaces(Set<ClassHierarchyNode> interfaces) {
		if (this.directInterfaces != null) {
			for (int i = 0; i < this.directInterfaces.size(); i++) {
				ClassHierarchyNode interfaceNode = this.directInterfaces.get(i);
				interfaces.add(interfaceNode);
				interfaceNode.collectInterfaces(interfaces);
			}
		}
	}

	public final String[] getDirectInterfaceNames() {
		String[] internalNames;
		if (this.directInterfaces != null) {
			internalNames = new String[this.directInterfaces.size()];

			for (int i = 0; i < this.directInterfaces.size(); i++) {
				internalNames[i] = this.directInterfaces.get(i).getInternalName();
			}
		} else {
			internalNames = new String[0];
		}

		return internalNames;
	}

	public void encryptIntegerConstants(
		IntegerEncryptionExcludes encryptionExcludes,
		NestedMultiMap<ClassNode, MethodNode, IntegerNode, PriorityEntry<WritableType>> constantEntries,
		KeyValuePairMultiMap<ClassNode, IntegerNode, FieldNode> staticIntegerFieldPairs,
		KeyValuePairMultiMap<ClassNode, IntegerNode, FieldNode> instanceIntegerFieldPairs,
		IntegerEncryptionBuilder integerEncryptionBuilder,
		Inheritance inheritance,
		Set<AbstractClassNode> processedClasses,
		HashMap<ClassNode, Boolean> microEditionClasses,
		ParameterChangerBuilder parameterChangerBuilder,
		Map<MethodNode, Long> encryptionKeysByMethod,
		Map<MethodNode, LocalVariableOperand> operandsByMethod,
		Workspace workspace,
		List<ClassNode> encryptedClasses,
		ClassProvider classProvider,
		TypeResolver typeResolver,
		boolean unused,
		ScriptContext scriptContext,
		boolean processInterfaces
	) {
		if (this.classNode.isWorkspaceNode() && (!processInterfaces || this.classNode.isInterface()) && processedClasses.add(this.classNode)) {
			try {
				((ClassNode)this.classNode)
					.encryptIntegerConstants(
						inheritance,
						integerEncryptionBuilder,
						unused,
						workspace,
						classProvider,
						staticIntegerFieldPairs.get((ClassNode)this.classNode),
						instanceIntegerFieldPairs.get((ClassNode)this.classNode),
						constantEntries.getMultiMap((ClassNode)this.classNode),
						encryptionExcludes,
						Integer.MAX_VALUE,
						parameterChangerBuilder,
						encryptionKeysByMethod,
						operandsByMethod,
						encryptedClasses,
						typeResolver,
						workspace
					);
			} catch (MethodAnalyzerException classAnalyzerException) {
				scriptContext.logMessage("Couldn't encrypt Integer Constants in '" + this.classNode.getBinaryName() + "' : \"" + classAnalyzerException.getMessage() + "\" (A)");
			}

			if (this.classNode.hasInnerClasses()) {
				for (AbstractClassNode innerClass : this.classNode.getInnerClasses()) {
					try {
						((ClassNode)innerClass)
							.encryptIntegerConstants(
								inheritance,
								integerEncryptionBuilder,
								unused,
								workspace,
								classProvider,
								staticIntegerFieldPairs.get((ClassNode)this.classNode),
								instanceIntegerFieldPairs.get((ClassNode)this.classNode),
								constantEntries.getMultiMap((ClassNode)this.classNode),
								encryptionExcludes,
								Integer.MAX_VALUE,
								parameterChangerBuilder,
								encryptionKeysByMethod,
								operandsByMethod,
								encryptedClasses,
								typeResolver,
								workspace
							);
					} catch (MethodAnalyzerException innerClassAnalyzerException) {
						scriptContext.logMessage("Couldn't encrypt Integer Constants in '" + innerClass.getSourcePath() + "' : \"" + innerClassAnalyzerException.getMessage() + "\" (B)");
					}
				}
			}

			if (processInterfaces) {
				Enumeration<ClassHierarchyNode> implementers = this.getImplementersEnumeration();
				if (implementers != null) {
					while (implementers.hasMoreElements()) {
						implementers.nextElement()
							.encryptIntegerConstants(encryptionExcludes, constantEntries, staticIntegerFieldPairs, instanceIntegerFieldPairs, integerEncryptionBuilder, inheritance, processedClasses, microEditionClasses, parameterChangerBuilder, encryptionKeysByMethod, operandsByMethod, workspace, encryptedClasses, classProvider, typeResolver, unused, scriptContext, processInterfaces);
					}
				}
			} else {
				Enumeration<ClassHierarchyNode> subclasses = this.getSubclassesEnumeration();
				if (subclasses != null) {
					while (subclasses.hasMoreElements()) {
						subclasses.nextElement()
							.encryptIntegerConstants(encryptionExcludes, constantEntries, staticIntegerFieldPairs, instanceIntegerFieldPairs, integerEncryptionBuilder, inheritance, processedClasses, microEditionClasses, parameterChangerBuilder, encryptionKeysByMethod, operandsByMethod, workspace, encryptedClasses, classProvider, typeResolver, unused, scriptContext, processInterfaces);
					}
				}
			}
		}
	}

	public final AbstractClassNode getClassNode() {
		return this.classNode;
	}

	public final void addDirectInnerClass(ClassHierarchyNode node) {
		if (this.directInnerClasses == null) {
			this.directInnerClasses = new ArrayList<>(2);
		}

		this.directInnerClasses.add(node);
	}

	public void encryptLongConstants(
		LongEncryptionExcludes encryptionExcludes,
		NestedMultiMap<ClassNode, MethodNode, LongNode, PriorityEntry<WideConstantPoolInsnNode>> constantEntries,
		KeyValuePairMultiMap<ClassNode, LongNode, FieldNode> staticLongFieldPairs,
		KeyValuePairMultiMap<ClassNode, LongNode, FieldNode> instanceLongFieldPairs,
		LongEncryptionBuilder longEncryptionBuilder,
		Inheritance inheritance,
		Set<AbstractClassNode> processedClasses,
		HashMap<ClassNode, Boolean> microEditionClasses,
		ParameterChangerBuilder parameterChangerBuilder,
		Map<MethodNode, Long> encryptionKeysByMethod,
		Map<MethodNode, LocalVariableOperand> operandsByMethod,
		Workspace workspace,
		List<ClassNode> encryptedClasses,
		ClassProvider classProvider,
		TypeResolver typeResolver,
		boolean unused,
		ScriptContext scriptContext,
		boolean processInterfaces
	) {
		if (this.classNode.isWorkspaceNode() && (!processInterfaces || this.classNode.isInterface()) && processedClasses.add(this.classNode)) {
			try {
				((ClassNode)this.classNode)
					.encryptLongConstants(
						inheritance,
						longEncryptionBuilder,
						unused,
						workspace,
						classProvider,
						staticLongFieldPairs.get((ClassNode)this.classNode),
						instanceLongFieldPairs.get((ClassNode)this.classNode),
						constantEntries.getMultiMap((ClassNode)this.classNode),
						encryptionExcludes,
						Integer.MAX_VALUE,
						parameterChangerBuilder,
						encryptionKeysByMethod,
						operandsByMethod,
						encryptedClasses,
						typeResolver,
						workspace
					);
			} catch (MethodAnalyzerException classAnalyzerException) {
				scriptContext.logMessage("Couldn't encrypt Longs Constants in '" + this.classNode.getBinaryName() + "' : \"" + classAnalyzerException.getMessage() + "\" (A)");
			}

			if (this.classNode.hasInnerClasses()) {
				for (AbstractClassNode innerClass : this.classNode.getInnerClasses()) {
					try {
						((ClassNode)innerClass)
							.encryptLongConstants(
								inheritance,
								longEncryptionBuilder,
								unused,
								workspace,
								classProvider,
								staticLongFieldPairs.get((ClassNode)this.classNode),
								instanceLongFieldPairs.get((ClassNode)this.classNode),
								constantEntries.getMultiMap((ClassNode)this.classNode),
								encryptionExcludes,
								Integer.MAX_VALUE,
								parameterChangerBuilder,
								encryptionKeysByMethod,
								operandsByMethod,
								encryptedClasses,
								typeResolver,
								workspace
							);
					} catch (MethodAnalyzerException innerClassAnalyzerException) {
						scriptContext.logMessage("Couldn't encrypt Long Constants in '" + innerClass.getSourcePath() + "' : \"" + innerClassAnalyzerException.getMessage() + "\" (B)");
					}
				}
			}

			if (processInterfaces) {
				Enumeration<ClassHierarchyNode> implementers = this.getImplementersEnumeration();
				if (implementers != null) {
					while (implementers.hasMoreElements()) {
						implementers.nextElement()
							.encryptLongConstants(encryptionExcludes, constantEntries, staticLongFieldPairs, instanceLongFieldPairs, longEncryptionBuilder, inheritance, processedClasses, microEditionClasses, parameterChangerBuilder, encryptionKeysByMethod, operandsByMethod, workspace, encryptedClasses, classProvider, typeResolver, unused, scriptContext, processInterfaces);
					}
				}
			} else {
				Enumeration<ClassHierarchyNode> subclasses = this.getSubclassesEnumeration();
				if (subclasses != null) {
					while (subclasses.hasMoreElements()) {
						subclasses.nextElement()
							.encryptLongConstants(encryptionExcludes, constantEntries, staticLongFieldPairs, instanceLongFieldPairs, longEncryptionBuilder, inheritance, processedClasses, microEditionClasses, parameterChangerBuilder, encryptionKeysByMethod, operandsByMethod, workspace, encryptedClasses, classProvider, typeResolver, unused, scriptContext, processInterfaces);
					}
				}
			}
		}
	}

	@Override
	public int compareTo(ClassHierarchyNode o) {
		return this.compareToIgnoreCase(o);
	}

	public final void setDepth(int depth) {
		this.hierarchyDepth = depth;
	}

	public final boolean hasSubclasses() {
		return this.directSubclasses != null && this.directSubclasses.size() > 0;
	}

	@Override
	public final boolean equals(Object obj) {
		return obj instanceof ClassHierarchyNode ? this.internalName.equals(((ClassHierarchyNode)obj).internalName) : false;
	}

	@Override
	public final int hashCode() {
		return this.internalName.hashCode();
	}

	public final ClassHierarchyNode getOuterClass() {
		return this.outerClass;
	}

	public final boolean hasSuperClassLink() {
		return this.superClassLink != null;
	}

	private void clear() {
		this.outerClass = null;
		if (this.classNode != null) {
			this.classNode.unsubscribe(this);
			this.classNode = null;
		}

		this.directSubclasses = null;
		this.directInterfaces = null;
		this.directImplementers = null;
		this.superClassNode = null;
		this.superClassLink = null;
		this.directInnerClasses = null;
	}

	public int compareToIgnoreCase(ClassHierarchyNode other) {
		return this.getInternalName().compareToIgnoreCase(other.getInternalName());
	}

	public final void addDirectImplementer(ClassHierarchyNode node) {
		if (this.directImplementers == null) {
			this.directImplementers = new ArrayList<>(2);
		}

		this.directImplementers.add(node);
	}

	public final Enumeration<ClassHierarchyNode> getSubclassesEnumeration() {
		return this.directSubclasses != null ? Collections.enumeration(this.directSubclasses) : null;
	}

	public final boolean hasSuperClass() {
		return this.superClassNode != null;
	}

	public final void validateParamChangeRequests(ParameterManager parameterManager) {
		this.classNode.validateParamChangeRequests(this, parameterManager);
		if (this.directSubclasses != null) {
			int subclassCount = this.directSubclasses.size();

			for (int i = 0; i < subclassCount; i++) {
				this.directSubclasses.get(i).validateParamChangeRequests(parameterManager);
			}
		}
	}

	public final void collectWorkspaceSubclasses(ArrayList<ClassHierarchyNode> subclasses) {
		if (this.directSubclasses != null) {
			for (int i = 0; i < this.directSubclasses.size(); i++) {
				ClassHierarchyNode subclassNode = this.directSubclasses.get(i);
				if (subclassNode.isWorkspaceClass()) {
					subclasses.add(subclassNode);
				} else {
					subclassNode.collectWorkspaceSubclasses(subclasses);
				}
			}
		}
	}

	public final List<ClassHierarchyNode> getDirectSubclassesList() {
		return this.directSubclasses != null ? new ArrayList<>(this.directSubclasses) : null;
	}

	public final void renameClassWithChangeLog(
		ClassRenamer renamer, ChangeLogContext changeLogContext, HashMap<String, String> classNameRemap, HashMap<String, String> internalNameRemap, Set<ClassNode> excludedClasses, Set<ClassHierarchyNode> pendingNodes
	) {
		if (this.classNode.isWorkspaceNode() && !excludedClasses.contains(this.classNode)) {
			if (changeLogContext != null && changeLogContext.hasOldClass(this.internalName)) {
				String changeLogNewClassName = changeLogContext.getNewClassName(this.internalName);
				String repackagedPrefix = renamer.getRepackagedPrefix(this.internalName);
				String simpleName;
				if (changeLogNewClassName != null) {
					simpleName = ClassRenamer.getSimpleName(changeLogNewClassName);
				} else {
					simpleName = ClassRenamer.getSimpleName(this.internalName);
				}

				String repackagedClassName = repackagedPrefix + simpleName;
				classNameRemap.put(this.internalName, repackagedClassName);
				String previousInternalName = internalNameRemap.put(repackagedClassName, this.internalName);
				DebugUtils.assertNull(previousInternalName, "Duplicate class '" + this.internalName + "' '" + repackagedClassName + "' '" + previousInternalName + "' (C)");
			} else {
				HashMap<ClassHierarchyNode, String> simpleNamesByAncestor = null;
				if (renamer.getKeepInnerClassInfo() != 1 && this.superClassLink != null) {
					simpleNamesByAncestor = GlobalUtils.newHashMap(13);
					Enumeration<ClassHierarchyNode> ancestors = this.getAncestorsFromLink();

					while (ancestors.hasMoreElements()) {
						ClassHierarchyNode ancestor = ancestors.nextElement();
						String ancestorSimpleName = AbstractClassNode.getSimpleNameFromInnerClass(
							AbstractClassNode.getSimpleNameFromInternalName(GlobalUtils.getMappedOrDefault(ancestor.getInternalName(), classNameRemap))
						);
						simpleNamesByAncestor.put(ancestor, ancestorSimpleName);
					}
				}

				String newClassName = renamer.computeNewClassName(this, simpleNamesByAncestor);
				if (newClassName != null) {
					classNameRemap.put(this.internalName, newClassName);
					String conflictingInternalName = internalNameRemap.put(newClassName, this.internalName);
					DebugUtils.assertNull(conflictingInternalName, "Duplicate class '" + this.internalName + "' '" + newClassName + "' '" + conflictingInternalName + "' (D)");
				} else {
					pendingNodes.add(this);
				}
			}
		}

		if (this.directSubclasses != null) {
			int subclassCount = this.directSubclasses.size();

			for (int i = 0; i < subclassCount; i++) {
				this.directSubclasses.get(i).renameClassWithChangeLog(renamer, changeLogContext, classNameRemap, internalNameRemap, excludedClasses, pendingNodes);
			}
		}
	}

	public final void buildMethodOverrideMaps(Hierarchy hierarchy, ClassProvider classProvider, AtomicReference<Boolean> integrityResult, ScriptContext scriptContext, Random random) {
		if (this.directSubclasses != null) {
			int subclassCount = this.directSubclasses.size();

			for (int i = 0; i < subclassCount; i++) {
				ClassHierarchyNode subclassNode = this.directSubclasses.get(i);
				AbstractClassNode subclassClassNode = subclassNode.getClassNode();
				if (subclassClassNode == null) {
					String analysisContext = "analyzing subclasses of '" + this.getName() + "'" + (this.classNode != null ? " : '" + this.classNode.getRawSourcePath() + "'" : "");

					try {
						subclassClassNode = classProvider.getClassNode(subclassNode.getInternalName(), analysisContext);
					} catch (ClasspathException classpathException) {
						throw new HierarchyClassNotFoundException("Class '" + GlobalUtils.internalToBinaryName(classpathException.getClassName()) + "' not found while " + analysisContext + ". (3)");
					} catch (FileException fileException) {
						throw new HierarchyClassNotFoundException(fileException.getMessage());
					}
				}

				subclassClassNode.buildMethodOverrideMap(hierarchy, integrityResult, scriptContext, random);
				subclassNode.buildMethodOverrideMaps(hierarchy, classProvider, integrityResult, scriptContext, random);
			}
		}
	}

	public final void collectOuterClasses(List<ClassHierarchyNode> outerClasses) {
		ClassHierarchyNode currentOuterClass = this;

		while (currentOuterClass.outerClass != null) {
			currentOuterClass = currentOuterClass.outerClass;
			outerClasses.add(currentOuterClass);
		}
	}

	@Override
	public final void onChange(INotifier notifier, Object changeKind, Object changedNode, Object changeArg) {
		if (changeKind instanceof AtomicInt && ((AtomicInt)changeKind).get() == 0 && changedNode instanceof AbstractClassNode) {
			this.internalName = this.classNode.getInternalName();
		}
	}

	public final void collectAllDescendants(Map<ClassHierarchyNode, ClassHierarchyNode> descendants) {
		if (this.directSubclasses != null) {
			for (int subclassIndex = 0; subclassIndex < this.directSubclasses.size(); subclassIndex++) {
				ClassHierarchyNode subclassNode = this.directSubclasses.get(subclassIndex);
				descendants.put(subclassNode, subclassNode);
				subclassNode.collectAllDescendants(descendants);
			}
		}

		if (this.directImplementers != null) {
			for (int implementerIndex = 0; implementerIndex < this.directImplementers.size(); implementerIndex++) {
				ClassHierarchyNode implementerNode = this.directImplementers.get(implementerIndex);
				descendants.put(implementerNode, implementerNode);
				implementerNode.collectAllDescendants(descendants);
			}
		}
	}

	public final void collectWorkspaceImplementers(ArrayList<ClassHierarchyNode> implementers) {
		if (this.directImplementers != null) {
			for (int i = 0; i < this.directImplementers.size(); i++) {
				ClassHierarchyNode implementerNode = this.directImplementers.get(i);
				if (implementerNode.isWorkspaceClass()) {
					implementers.add(implementerNode);
				} else {
					implementerNode.collectWorkspaceImplementers(implementers);
				}
			}
		}
	}

	public final void processMethodRenaming(
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
		HashMap<String, String> changeLogNames,
		Map<MethodNode, String> methodNewNames
	) {
		if (this.classNode.isWorkspaceNode()) {
			((ClassNode)this.classNode)
				.processMethodRenaming(defaultExcludes, parameterChangerExcludes, changeLogContext, methodRenamer, hierarchy, nameGenerator, processedMethodsBySignature, libraryNamesByDescriptor, keepInnerClassInfo, processedMethodsByParamKey, interfaceMethodNames, classMethodNames, aggressiveRenaming, randomizeObfuscation, usedNames, this, changeLogNames, methodNewNames);
		} else {
			((LibraryClassNode)this.classNode).collectMethodLookupMaps(processedMethodsBySignature, libraryNamesByDescriptor, processedMethodsByParamKey, interfaceMethodNames, classMethodNames);
		}

		if (this.directSubclasses != null) {
			int subclassCount = this.directSubclasses.size();

			for (int i = 0; i < subclassCount; i++) {
				this.directSubclasses
					.get(i)
					.processMethodRenaming(
						defaultExcludes,
						parameterChangerExcludes,
						changeLogContext,
						methodRenamer,
						hierarchy,
						nameGenerator,
						GlobalUtils.clone(processedMethodsBySignature),
						new MultiMap<>(libraryNamesByDescriptor),
						keepInnerClassInfo,
						GlobalUtils.clone(processedMethodsByParamKey),
						interfaceMethodNames,
						GlobalUtils.clone(classMethodNames),
						aggressiveRenaming,
						randomizeObfuscation,
						usedNames,
						changeLogNames,
						methodNewNames
					);
			}
		}
	}

	public final int getDepth() {
		return this.hierarchyDepth;
	}

	public Enumeration<ClassHierarchyNode> getInterfacesEnumeration() {
		return this.directInterfaces != null ? Collections.enumeration(this.directInterfaces) : null;
	}

	public final void addDirectSubclass(ClassHierarchyNode node) {
		if (this.directSubclasses == null) {
			this.directSubclasses = new ArrayList<>(2);
		}

		this.directSubclasses.add(node);
	}

	public void encryptStrings(
		StringEncryptionExcludes encryptionTargetManager,
		KeyValuePairMultiMap<ClassNode, StringNode, FieldNode> stringFieldPairs,
		MultiMap<ClassNode, FieldNode> instanceFields,
		NestedMultiMap<ClassNode, MethodNode, StringNode, PriorityEntry<WritableType>> constantEntries,
		StringEncryptionBuilder stringEncryptionBuilder,
		boolean useGoto,
		boolean aggressiveEncryption,
		boolean useDoubleArrayStorage,
		boolean injectXorMask,
		Set<StringNode> processedStrings,
		HashMap<ClassNode, Boolean> microEditionClasses,
		Inheritance inheritance,
		Set<AbstractClassNode> processedClasses,
		ParameterChangerBuilder parameterChangerBuilder,
		Map<MethodNode, Long> encryptionKeysByMethod,
		Map<MethodNode, LocalVariableOperand> operandsByMethod,
		Workspace workspace,
		List<ClassNode> encryptedClasses,
		ClassProvider classProvider,
		TypeResolver typeResolver,
		ScriptContext scriptContext,
		boolean processInterfaces
	) {
		if (this.classNode.isWorkspaceNode() && (!processInterfaces || this.classNode.isInterface()) && processedClasses.add(this.classNode)) {
			boolean effectiveUseGoto = false;
			boolean useStringIntern = true;
			if (microEditionClasses.containsKey(this.classNode)) {
				effectiveUseGoto = true;
				useStringIntern = microEditionClasses.get(this.classNode);
			} else if (this.classNode.isJava6OrNewer()) {
				effectiveUseGoto = true;
			} else if (useGoto) {
				effectiveUseGoto = true;
			}

			try {
				((ClassNode)this.classNode)
					.encryptStringConstants(
						inheritance,
						stringEncryptionBuilder,
						aggressiveEncryption,
						useDoubleArrayStorage,
						effectiveUseGoto,
						useStringIntern,
						injectXorMask,
						workspace,
						classProvider,
						stringFieldPairs.get((ClassNode)this.classNode),
						instanceFields.get((ClassNode)this.classNode),
						constantEntries.getMultiMap((ClassNode)this.classNode),
						encryptionTargetManager,
						processedStrings,
						Integer.MAX_VALUE,
						parameterChangerBuilder,
						encryptionKeysByMethod,
						operandsByMethod,
						encryptedClasses,
						typeResolver,
						workspace
					);
			} catch (MethodAnalyzerException classAnalyzerException) {
				scriptContext.logMessage("Couldn't encrypt Strings in '" + this.classNode.getBinaryName() + "' : \"" + classAnalyzerException.getMessage() + "\" (A)");
			}

			if (this.classNode.hasInnerClasses()) {
				for (AbstractClassNode innerClass : this.classNode.getInnerClasses()) {
					try {
						((ClassNode)innerClass)
							.encryptStringConstants(
								inheritance,
								stringEncryptionBuilder,
								aggressiveEncryption,
								useDoubleArrayStorage,
								effectiveUseGoto,
								useStringIntern,
								injectXorMask,
								workspace,
								classProvider,
								stringFieldPairs.get((ClassNode)innerClass),
								instanceFields.get((ClassNode)innerClass),
								constantEntries.getMultiMap((ClassNode)innerClass),
								encryptionTargetManager,
								processedStrings,
								Integer.MAX_VALUE,
								parameterChangerBuilder,
								encryptionKeysByMethod,
								operandsByMethod,
								encryptedClasses,
								typeResolver,
								workspace
							);
					} catch (MethodAnalyzerException innerClassAnalyzerException) {
						scriptContext.logMessage("Couldn't encrypt Strings in '" + innerClass.getSourcePath() + "' : \"" + innerClassAnalyzerException.getMessage() + "\" (B)");
					}
				}
			}

			if (processInterfaces) {
				Enumeration<ClassHierarchyNode> implementers = this.getImplementersEnumeration();
				if (implementers != null) {
					while (implementers.hasMoreElements()) {
						implementers.nextElement()
							.encryptStrings(
								encryptionTargetManager, stringFieldPairs, instanceFields, constantEntries, stringEncryptionBuilder, useGoto, aggressiveEncryption, useDoubleArrayStorage, injectXorMask, processedStrings, microEditionClasses, inheritance, processedClasses, parameterChangerBuilder, encryptionKeysByMethod, operandsByMethod, workspace, encryptedClasses, classProvider, typeResolver, scriptContext, processInterfaces
							);
					}
				}
			} else {
				Enumeration<ClassHierarchyNode> subclasses = this.getSubclassesEnumeration();
				if (subclasses != null) {
					while (subclasses.hasMoreElements()) {
						subclasses.nextElement()
							.encryptStrings(
								encryptionTargetManager, stringFieldPairs, instanceFields, constantEntries, stringEncryptionBuilder, useGoto, aggressiveEncryption, useDoubleArrayStorage, injectXorMask, processedStrings, microEditionClasses, inheritance, processedClasses, parameterChangerBuilder, encryptionKeysByMethod, operandsByMethod, workspace, encryptedClasses, classProvider, typeResolver, scriptContext, processInterfaces
							);
					}
				}
			}
		}
	}

	public final void setSuperClassLink(ClassHierarchyNode node) {
		this.superClassLink = node;
	}

	public final boolean isWorkspaceClass() {
		return this.classNode != null && this.classNode.isWorkspaceNode();
	}
}
