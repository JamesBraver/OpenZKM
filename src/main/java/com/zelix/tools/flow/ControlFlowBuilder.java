package com.zelix.tools.flow;

import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.analyzer.StaticBlockAnalyzer;
import com.zelix.framework.attributes.code.TryCatchBlockInfo;
import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.framework.hierarchy.GroupingsHierarchy;
import com.zelix.framework.hierarchy.Inheritance;
import com.zelix.framework.instructions.AbstractInsnNode;
import com.zelix.framework.instructions.InsnNode;
import com.zelix.framework.instructions.JumpInsnNode;
import com.zelix.framework.instructions.LabelNode;
import com.zelix.framework.instructions.NewArrayNode;
import com.zelix.framework.instructions.NewNode;
import com.zelix.framework.instructions.WideConstantPoolInsnNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.signature.MethodParameterKey;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.framework.variable.LocalVariableTable;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.LibraryClassNode;
import com.zelix.framework.wrappers.LibraryFieldNode;
import com.zelix.framework.wrappers.LibraryMethodNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.ClassInfoWriter;
import com.zelix.framework.writer.FieldTypeWriter;
import com.zelix.framework.writer.MethodCallWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.changelogs.FlowObfuscationEntry;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.LicenseCheckConstants4;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.tools.excludes.DefaultExcludes;
import com.zelix.tools.excludes.ExcludeElement;
import com.zelix.tools.excludes.ExcludeStatementManager;
import com.zelix.tools.flow.grouping.PackageGrouping;
import com.zelix.tools.flow.grouping.PackageGroupingManager;
import com.zelix.tools.initOrder.ClassInitializationOrderBuilder;
import com.zelix.tools.renamer.naming.AlphaNameGenerator;
import com.zelix.utils.ClassMetrics;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.CountingSet;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.KeyValuePair;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.NamedSet;
import com.zelix.utils.collections.OrderedSet;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyRegistryView;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.PriorityInstructionEntry;
import com.zelix.utils.dataset.RenameProperty;
import com.zelix.utils.references.AtomicReference;
import com.zelix.utils.references.IdentityReference;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.ClassTreeManager;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static com.zelix.framework.instructions.OpcodeConstants.*;

public class ControlFlowBuilder extends LicenseCheckConstants4 {
	private final List<PackageGroupingManager> packageGroupingManagers = new ArrayList<>();
	private List<Condition> conditions = new ArrayList<>();
	private MultiMap<String, Condition> conditionsByPackage = new MultiMap<>();
	private Map<Condition, Condition> packageConditionDefaults = GlobalUtils.newHashMap();
	private final Set<MethodNode> generatedMethods = GlobalUtils.newHashSet();
	private Set<AbstractClassNode> dummyJumpClasses = GlobalUtils.newHashSet();
	private Set<AbstractClassNode> conditionalBlockClasses = GlobalUtils.newHashSet();
	private ClassNode[] classNodes;
	private AbstractClassNode[] flowObfuscationClasses;
	private GroupingsHierarchy groupingsHierarchy;
	private final ChangeLogContext changeLogContext;
	private final AbstractWorkspace workspace;
	private final ClassMetrics classMetrics;
	private final StaticBlockAnalyzer staticBlockAnalyzer;
	private boolean deriveGroupingsFromChangeLog;
	private final Map<AbstractClassNode, PackageCondition> packageConditionByClass;
	private final boolean randomizeObfuscation;
	private final Random random;
	private final NamedSet<? extends AbstractClassNode>[] groupings;
	private final Map<AbstractClassNode, PackageGroupingManager> groupingManagerByClass;
	private Object licenseChecker;
	private Map<PackageGroupingManager, OrderedSet<String>> fieldTypesByGrouping;
	private MultiMap<String, ClassNode> linkedClassesByName;
	private MultiMap<AbstractClassNode, NamedSet<? extends AbstractClassNode>> groupingsByClass;

	public ControlFlowBuilder(
		ClassNode[] classNodes,
		AbstractClassNode[] flowObfuscationClasses,
		ScriptContext scriptContext,
		GroupingsHierarchy groupingsHierarchy,
		ChangeLogContext changeLogContext,
		AbstractWorkspace workspace,
		ClassMetrics classMetrics,
		StaticBlockAnalyzer staticBlockAnalyzer,
		boolean deriveGroupingsFromChangeLog,
		boolean randomizeObfuscation
	) {
		this.classNodes = classNodes;
		this.flowObfuscationClasses = flowObfuscationClasses;
		this.groupingsHierarchy = groupingsHierarchy;
		this.changeLogContext = changeLogContext;
		this.workspace = workspace;
		this.classMetrics = classMetrics;
		this.staticBlockAnalyzer = staticBlockAnalyzer;
		this.deriveGroupingsFromChangeLog = deriveGroupingsFromChangeLog;
		this.packageConditionByClass = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(flowObfuscationClasses.length));
		this.randomizeObfuscation = randomizeObfuscation;
		if (!this.randomizeObfuscation && !Options.ZKM_RANDOMIZE_OBFUSCATION) {
			this.random = GlobalUtils.newRandomWithSeed(this.classNodes.length);
		} else {
			this.random = GlobalUtils.newRandom(128);
		}

		this.random.nextBoolean();
		if (!deriveGroupingsFromChangeLog) {
			this.groupings = buildGroupingsFromClasspath(classNodes, flowObfuscationClasses, groupingsHierarchy, changeLogContext, workspace, null);
			this.groupingManagerByClass = buildGroupingManagers(classNodes, flowObfuscationClasses, changeLogContext, deriveGroupingsFromChangeLog, this.groupings, this.packageGroupingManagers);
		} else {
			if (this.groupingsHierarchy != null) {
				scriptContext.logWarning("'deriveGroupingsFromInputChangeLog' specified but 'groupings' statement present. 'groupings' statement will be ignored.");
			}

			if (changeLogContext != null) {
				if (changeLogContext.hasFlowObfuscationData()) {
					this.groupings = buildGroupingsFromChangeLog(this.flowObfuscationClasses, changeLogContext, null);
					this.groupingManagerByClass = deriveGroupingManagers(classNodes, this.flowObfuscationClasses, changeLogContext, deriveGroupingsFromChangeLog, this.groupings, this.packageGroupingManagers);
				} else {
					scriptContext.logSpecialError("'deriveGroupingsFromInputChangeLog' specified but input change log '" + changeLogContext.getLogFileName() + " has no flow obfuscation data.");
					this.groupings = null;
					this.groupingManagerByClass = null;
				}
			} else {
				scriptContext.logSpecialError("'deriveGroupingsFromInputChangeLog' specified but there is no input change log.");
				this.groupings = null;
				this.groupingManagerByClass = null;
			}
		}

		for (int i = 0; i < 5; i++) {
			try {
				Class<?> instantiatedClass = Class.forName(this.getReflectionTargetName(i));
				this.licenseChecker = instantiatedClass.newInstance();
				break;
			} catch (Throwable ignored) {
			}
		}
	}

	private static NamedSet<? extends AbstractClassNode>[] buildGroupingsFromClasspath(
		ClassNode[] classNodes, AbstractClassNode[] flowObfuscationClasses, GroupingsHierarchy groupingsHierarchy, ChangeLogContext changeLogContext, ClassHierarchy classHierarchy, Map<String, String> classNameToInternalNameMap
	) {
		int classCount = classNodes.length;
		DualKeyMap<String, ClassNode, ClassNode> groupedClassesByName = new DualKeyMap<>();

		for (int classIndex = 0; classIndex < classCount; classIndex++) {
			ClassNode classNode = classNodes[classIndex];
			if ((!classNode.isInterface() || classNode.isJava8OrNewer()) && !classNode.isGeneratedClass()) {
				Enumeration<ClasspathResource> classSources = classNode.getClassSources();

				while (classSources.hasMoreElements()) {
					ClasspathResource classSource = classSources.nextElement();
					if (classSource.isFileSystemResource()) {
						groupedClassesByName.put("FILE SYSTEM", classNode, classNode);
					} else {
						ArchiveNode archiveNode = classSource.getArchiveNode();
						groupedClassesByName.put(archiveNode.getFullPath(), classNode, classNode);
					}
				}
			}
		}

		if (groupingsHierarchy != null) {
			ReadOnlyRegistryView<String, ClassNode, ClassNode> groupingsRegistryView = groupingsHierarchy.getGroupingClasses();
			ArrayList<String> groupKeys = new ArrayList<>();
			Enumeration<String> groupKeyEnumeration = groupingsRegistryView.getGroupKeys();

			while (groupKeyEnumeration.hasMoreElements()) {
				groupKeys.add(groupKeyEnumeration.nextElement());
			}

			for (int groupKeyIndex = 0; groupKeyIndex < groupKeys.size(); groupKeyIndex++) {
				String groupKey = groupKeys.get(groupKeyIndex);
				Map<ClassNode, ClassNode> writableGroupClasses = groupingsRegistryView.getReadOnlyGroup(groupKey).getWritableCopy();
				Iterator<ClassNode> classIterator = writableGroupClasses.keySet().iterator();

				while (classIterator.hasNext()) {
					ClassNode groupedClass = classIterator.next();
					if (groupedClass.isInterface() && !groupedClass.isJava8OrNewer()) {
						classIterator.remove();
					}
				}

				if (writableGroupClasses.size() > 0) {
					groupedClassesByName.putGroup(groupKey, writableGroupClasses);
				}
			}

			if (groupingsHierarchy.isVerbose()) {
				groupingsHierarchy.printGroupingParameters();
				Collections.sort(groupKeys);
				PrintWriter writer = groupingsHierarchy.getWriter();
				writer.println("\tMatched groupings:");

				for (int matchedGroupKeyIndex = 0; matchedGroupKeyIndex < groupKeys.size(); matchedGroupKeyIndex++) {
					String matchedGroupKey = groupKeys.get(matchedGroupKeyIndex);
					writer.println("\tGrouping \"" + matchedGroupKey + "\"");
					ReadOnlyMap<ClassNode, ClassNode> readOnlyGroup = groupingsRegistryView.getReadOnlyGroup(matchedGroupKey);
					ArrayList<RenameProperty> renameProperties = new ArrayList<>();
					Enumeration<ClassNode> listedClassEnumeration = readOnlyGroup.getKeyEnumeration();

					while (listedClassEnumeration.hasMoreElements()) {
						ClassNode listedClass = listedClassEnumeration.nextElement();
						renameProperties.add(new RenameProperty(listedClass.getBinaryName(), listedClass));
					}

					Collections.sort(renameProperties);

					for (int propertyIndex = 0; propertyIndex < renameProperties.size(); propertyIndex++) {
						RenameProperty renameProperty = renameProperties.get(propertyIndex);
						writer.println("\t\t" + ExcludeStatementManager.formatClassForDisplay((AbstractClassNode)renameProperty.getValue(), classHierarchy));
					}
				}
			}
		}

		NamedSet<? extends AbstractClassNode>[] allGroupings;
		if (changeLogContext != null && flowObfuscationClasses.length > classNodes.length) {
			NamedSet<? extends AbstractClassNode>[] changeLogGroupings = buildGroupingsFromChangeLog(flowObfuscationClasses, changeLogContext, classNameToInternalNameMap);
			allGroupings = new NamedSet[changeLogGroupings.length + groupedClassesByName.getGroupCount()];
			System.arraycopy(changeLogGroupings, 0, allGroupings, groupedClassesByName.getGroupCount(), changeLogGroupings.length);
		} else {
			allGroupings = new NamedSet[groupedClassesByName.getGroupCount()];
		}

		int groupingIndex = 0;
		Enumeration<String> groupNameEnumeration = groupedClassesByName.getGroupKeys();

		while (groupNameEnumeration.hasMoreElements()) {
			String groupName = groupNameEnumeration.nextElement();
			Map<ClassNode, ClassNode> groupClasses = groupedClassesByName.getGroup(groupName);
			allGroupings[groupingIndex++] = GlobalUtils.newNamedHashSet(groupName, groupClasses);
		}

		return allGroupings;
	}

	private static NamedSet<? extends AbstractClassNode>[] buildGroupingsFromChangeLog(AbstractClassNode[] changeLogClasses, ChangeLogContext changeLogContext, Map<String, String> classNameToInternalNameMap) {
		Map<String, KeyValuePair<FlowObfuscationEntry, FlowObfuscationEntry>> memberClassEntries = changeLogContext.getMemberClassEntries();
		ReferenceGraph<FlowObfuscationEntry, String> referenceGraph = new ReferenceGraph<>(memberClassEntries.size());

		for (Entry<String, KeyValuePair<FlowObfuscationEntry, FlowObfuscationEntry>> memberClassEntry : memberClassEntries.entrySet()) {
			String internalClassName = GlobalUtils.binaryToInternalName(memberClassEntry.getKey());
			FlowObfuscationEntry firstMemberEntry = memberClassEntry.getValue().getKey();
			referenceGraph.addReference(firstMemberEntry, internalClassName);
		}

		ArrayList<NamedSet<? extends AbstractClassNode>> changeLogGroupings = new ArrayList<>(referenceGraph.size());

		for (Entry<FlowObfuscationEntry, Set<String>> adjacencyEntry : referenceGraph.getAdjacencyEntries()) {
			FlowObfuscationEntry referencedEntry = adjacencyEntry.getKey();
			Set<String> referencingClassNames = adjacencyEntry.getValue();
			ArrayList<AbstractClassNode> referencingClasses = new ArrayList<>(referencingClassNames.size());

			for (String referencingClassName : referencingClassNames) {
				String mappedName = GlobalUtils.getMappedOrDefault(referencingClassName, classNameToInternalNameMap);
				AbstractClassNode classNode = ClassHierarchyNode.getClassNodeByInternalName(mappedName);
				DebugUtils.assertNotNullString(classNode, new String[]{referencingClassName, ", ", mappedName});
				if (!classNode.isInterface() || classNode.isJava8OrNewer()) {
					referencingClasses.add(classNode);
				}
			}

			if (referencingClasses.size() > 0) {
				NamedSet<AbstractClassNode> grouping = GlobalUtils.newNamedHashSet(referencedEntry.getInternalClassName() + "." + referencedEntry.getOriginalFieldName(), referencingClasses);
				changeLogGroupings.add(grouping);
			}
		}

		return changeLogGroupings.toArray(new NamedSet[changeLogGroupings.size()]);
	}

	public static List<AbstractInsnNode> createDummyValueInstructions(
		Condition condition, List<TypeWriter> cpEntries, ConstantPoolManager constantPoolManager, boolean useFieldNodeDescriptor, AbstractWorkspace workspace, ClassProvider classProvider, Random random
	) {
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		if (condition.isStringField()) {
			condition.getFieldDescriptor();
			String generatedStringName = new AlphaNameGenerator(0).generateName(random.nextInt(Integer.MAX_VALUE));
			instructions.add(AbstractInsnNode.ldcString(generatedStringName, constantPoolManager, cpEntries, false));
		} else if (condition.isObjectType()) {
			String objectTypeDescriptor = condition.getFieldDescriptor();
			ClassInfoWriter classInfoWriter = constantPoolManager.getOrCreateClassInfo(objectTypeDescriptor.substring(1, objectTypeDescriptor.length() - 1), cpEntries);
			instructions.add(new NewNode(classInfoWriter));
			instructions.add(InsnNode.of(DUP));
			MethodCallWriter constructorMethodRef = constantPoolManager.getOrCreateMethodRef(classInfoWriter.getInternalName(), "<init>", "()V", cpEntries, workspace, classProvider);
			instructions.add(new WideConstantPoolInsnNode(INVOKESPECIAL, constructorMethodRef));
		} else if (condition.isPrimitiveArray()) {
			instructions.add(AbstractInsnNode.ldcInt(random.nextInt(5) + 1, constantPoolManager, cpEntries));
			String primitiveArrayDescriptor = condition.getFieldDescriptor();
			String elementTypeDescriptor = primitiveArrayDescriptor.substring(primitiveArrayDescriptor.lastIndexOf(91) + 1);
			instructions.add(new NewArrayNode(elementTypeDescriptor));
		} else if (condition.isObjectArray()) {
			instructions.add(AbstractInsnNode.ldcInt(random.nextInt(5) + 1, constantPoolManager, cpEntries));
			String objectArrayDescriptor;
			if (useFieldNodeDescriptor) {
				objectArrayDescriptor = condition.getFieldNodeDescriptor();
			} else {
				objectArrayDescriptor = condition.getFieldDescriptor();
			}

			ClassInfoWriter componentClassInfo = constantPoolManager.getOrCreateClassInfo(objectArrayDescriptor.substring(objectArrayDescriptor.lastIndexOf(91) + 2, objectArrayDescriptor.length() - 1), cpEntries);
			instructions.add(new WideConstantPoolInsnNode(ANEWARRAY, componentClassInfo));
		}

		return instructions;
	}

	private static HashMap<AbstractClassNode, PackageGroupingManager> buildGroupingManagers(
		ClassNode[] classNodes,
		AbstractClassNode[] flowObfuscationClasses,
		ChangeLogContext changeLogContext,
		boolean deriveGroupingsFromChangeLog,
		NamedSet<? extends AbstractClassNode>[] groupings,
		List<PackageGroupingManager> packageGroupingManagers
	) {
		int groupingCount = groupings.length;
		HashSet<IdentityReference> clashingGroupingRefs = GlobalUtils.newHashSet();
		HashSet<HashSet<NamedSet<? extends AbstractClassNode>>> clashGroupSets = GlobalUtils.newHashSet();

		for (ClassNode classNode : classNodes) {
			if (!classNode.isInterface() || classNode.isJava8OrNewer()) {
				LinkedHashSet<IdentityReference> containingGroupings = new LinkedHashSet<>();

				for (int groupingIndex = 0; groupingIndex < groupingCount; groupingIndex++) {
					NamedSet<? extends AbstractClassNode> grouping = groupings[groupingIndex];
					if (grouping.contains(classNode)) {
						containingGroupings.add(new IdentityReference(grouping));
					}
				}

				if (containingGroupings.size() > 1) {
					HashSet<NamedSet<? extends AbstractClassNode>> overlappingGroups = GlobalUtils.newHashSet();

					for (IdentityReference groupingRef : containingGroupings) {
						overlappingGroups.add((NamedSet<? extends AbstractClassNode>)groupingRef.get());
						clashingGroupingRefs.add(groupingRef);
					}

					clashGroupSets.add(overlappingGroups);
				}
			}
		}

		ArrayList<PackageGrouping> packageGroupings = new ArrayList<>();

		for (int candidateGroupingIndex = 0; candidateGroupingIndex < groupingCount; candidateGroupingIndex++) {
			NamedSet<? extends AbstractClassNode> candidateGrouping = groupings[candidateGroupingIndex];
			if (!clashingGroupingRefs.contains(new IdentityReference(candidateGrouping))) {
				packageGroupings.add(new PackageGrouping(candidateGrouping));
			}
		}

		ArrayList<HashSet<NamedSet<? extends AbstractClassNode>>> sortedClashGroupSets = new ArrayList<>(clashGroupSets);
		Collections.sort(sortedClashGroupSets, (o1, o2) -> o1.size() - o2.size());
		Iterator<HashSet<NamedSet<? extends AbstractClassNode>>> clashGroupSetIterator = sortedClashGroupSets.iterator();

		while (clashGroupSetIterator.hasNext()) {
			boolean subsumed = false;
			HashSet<NamedSet<? extends AbstractClassNode>> clashGroupSet = clashGroupSetIterator.next();

			for (int candidateIndex = sortedClashGroupSets.size() - 1; candidateIndex >= 0; candidateIndex--) {
				HashSet<NamedSet<? extends AbstractClassNode>> candidateClashGroupSet = sortedClashGroupSets.get(candidateIndex);
				if (candidateClashGroupSet.size() <= clashGroupSet.size()) {
					break;
				}

				if (candidateClashGroupSet.containsAll(clashGroupSet)) {
					subsumed = true;
					break;
				}
			}

			if (subsumed) {
				clashGroupSets.remove(clashGroupSet);
				clashGroupSetIterator.remove();
			}
		}

		for (HashSet<NamedSet<? extends AbstractClassNode>> remainingClashGroupSet : clashGroupSets) {
			NamedSet<? extends AbstractClassNode>[] clashGroupings = remainingClashGroupSet.toArray(new NamedSet[remainingClashGroupSet.size()]);
			PackageGrouping intersectedGrouping = new PackageGrouping(clashGroupings[0]);
			int clashGroupingCount = clashGroupings.length;

			for (int mergeIndex = 1; mergeIndex < clashGroupingCount; mergeIndex++) {
				intersectedGrouping = intersectedGrouping.intersect(new PackageGrouping(clashGroupings[mergeIndex]));
			}

			packageGroupings.add(intersectedGrouping);
		}

		Collections.sort(packageGroupings);
		int packageGroupingCount = packageGroupings.size();

		for (PackageGrouping packageGrouping : packageGroupings) {
			PackageGroupingManager packageGroupingManager = new PackageGroupingManager(packageGrouping);
			packageGroupingManagers.add(packageGroupingManager);
		}

		for (int outerGroupingIndex = packageGroupingCount - 1; outerGroupingIndex >= 0; outerGroupingIndex--) {
			PackageGrouping outerGrouping = packageGroupings.get(outerGroupingIndex);

			for (int innerGroupingIndex = outerGroupingIndex - 1; innerGroupingIndex >= 0; innerGroupingIndex--) {
				PackageGrouping innerGrouping = packageGroupings.get(innerGroupingIndex);
				NamedSet<NamedSet<? extends AbstractClassNode>> commonGroups = outerGrouping.getCommonGroups(innerGrouping);
				if (commonGroups.size() > 0) {
					NamedSet<AbstractClassNode> commonClasses = GlobalUtils.newNamedSet();
					Iterator<NamedSet<? extends AbstractClassNode>> commonGroupIterator = commonGroups.iterator();

					while (commonGroupIterator.hasNext()) {
						commonClasses.addAll(commonGroupIterator.next());
					}

					NamedSet<NamedSet<? extends AbstractClassNode>> innerOnlyGroups = innerGrouping.getGroupsNotIn(outerGrouping);

					for (AbstractClassNode commonClass : commonClasses) {
						boolean inInnerOnlyGroup = false;

						for (NamedSet<? extends AbstractClassNode> innerOnlyGroup : innerOnlyGroups) {
							if (innerOnlyGroup.contains(commonClass)) {
								inInnerOnlyGroup = true;
							}
						}

						if (inInnerOnlyGroup) {
							PackageGroupingManager outerGroupingManager = packageGroupingManagers.get(outerGroupingIndex);
							outerGroupingManager.removeClass(commonClass);
						} else {
							PackageGroupingManager innerGroupingManager = packageGroupingManagers.get(innerGroupingIndex);
							innerGroupingManager.removeClass(commonClass);
						}
					}
				}
			}
		}

		HashMap<AbstractClassNode, PackageGroupingManager> groupingManagersByClass = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(flowObfuscationClasses.length));
		indexGroupingManagersByClass(classNodes, changeLogContext, deriveGroupingsFromChangeLog, packageGroupingManagers, groupingManagersByClass);
		return groupingManagersByClass;
	}

	public static ArrayList<PackageGroupingManager> createGroupingManagers(
		ClassNode[] classNodes,
		AbstractClassNode[] changeLogClasses,
		Map<String, String> classNameToInternalNameMap,
		ScriptContext scriptContext,
		GroupingsHierarchy groupingsHierarchy,
		ChangeLogContext changeLogContext,
		ClassHierarchy classHierarchy,
		boolean deriveGroupingsFromChangeLog,
		AtomicReference<Map<AbstractClassNode, PackageGroupingManager>> groupingManagerByClassRef
	) {
		HashMap<AbstractClassNode, PackageGroupingManager> groupingManagersByClass = null;
		ArrayList<PackageGroupingManager> groupingManagers = new ArrayList<>();
		if (!deriveGroupingsFromChangeLog) {
			NamedSet<? extends AbstractClassNode>[] classpathGroupings = buildGroupingsFromClasspath(classNodes, changeLogClasses, groupingsHierarchy, changeLogContext, classHierarchy, classNameToInternalNameMap);
			groupingManagersByClass = buildGroupingManagers(classNodes, changeLogClasses, changeLogContext, deriveGroupingsFromChangeLog, classpathGroupings, groupingManagers);
		} else {
			if (groupingsHierarchy != null) {
				scriptContext.logWarning("'deriveGroupingsFromInputChangeLog' specified but 'groupings' statement present. 'groupings' statement will be ignored.");
			}

			if (changeLogContext != null) {
				if (changeLogContext.hasFlowObfuscationData()) {
					NamedSet<? extends AbstractClassNode>[] changeLogGroupings = buildGroupingsFromChangeLog(changeLogClasses, changeLogContext, classNameToInternalNameMap);
					groupingManagersByClass = deriveGroupingManagers(classNodes, changeLogClasses, changeLogContext, deriveGroupingsFromChangeLog, changeLogGroupings, groupingManagers);
				} else {
					scriptContext.logSpecialError("'deriveGroupingsFromInputChangeLog' specified but input change log '" + changeLogContext.getLogFileName() + " has no flow obfuscation data.");
				}
			} else {
				scriptContext.logSpecialError("'deriveGroupingsFromInputChangeLog' specified but there is no input change log.");
			}
		}

		groupingManagerByClassRef.set(groupingManagersByClass);
		return groupingManagers;
	}

	private static HashMap<AbstractClassNode, PackageGroupingManager> deriveGroupingManagers(
		ClassNode[] classNodes,
		AbstractClassNode[] flowObfuscationClasses,
		ChangeLogContext changeLogContext,
		boolean deriveGroupingsFromChangeLog,
		NamedSet<? extends AbstractClassNode>[] groupings,
		List<PackageGroupingManager> packageGroupingManagers
	) {
		HashMap<AbstractClassNode, PackageGroupingManager> groupingManagersByClass = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(flowObfuscationClasses.length));

		for (int groupingIndex = 0; groupingIndex < groupings.length; groupingIndex++) {
			PackageGroupingManager packageGroupingManager = new PackageGroupingManager(new PackageGrouping(groupings[groupingIndex]));
			packageGroupingManagers.add(packageGroupingManager);
		}

		indexGroupingManagersByClass(classNodes, changeLogContext, deriveGroupingsFromChangeLog, packageGroupingManagers, groupingManagersByClass);

		for (int classIndex = 0; classIndex < classNodes.length; classIndex++) {
			String internalClassName = classNodes[classIndex].getInternalName();
			if (!changeLogContext.hasOldClass(internalClassName)) {
				String binaryClassName = GlobalUtils.internalToBinaryName(internalClassName);
				throw new ProcessingException(
					"'deriveGroupingsFromInputChangeLog' specified but opened class '" + binaryClassName + "' does not appear in log '" + changeLogContext.getLogFileName() + "'"
				);
			}
		}

		return groupingManagersByClass;
	}

	private static void indexGroupingManagersByClass(
		ClassNode[] classNodes, ChangeLogContext changeLogContext, boolean deriveGroupingsFromChangeLog, List<PackageGroupingManager> packageGroupingManagers, Map<AbstractClassNode, PackageGroupingManager> groupingManagerByClass
	) {
		for (PackageGroupingManager packageGroupingManager : packageGroupingManagers) {
			packageGroupingManager.collectPackageNames();

			for (AbstractClassNode groupedClass : packageGroupingManager.getGroupedClasses()) {
				groupingManagerByClass.put(groupedClass, packageGroupingManager);
			}
		}
	}

	private CountingSet<ClassNode> restrictToLinkedGrouping(String internalClassName, CountingSet<ClassNode> classReferenceCounts, ChangeLogContext changeLogContext) {
		if (this.linkedClassesByName == null) {
			this.linkedClassesByName = new MultiMap<>();

			for (Entry<String, KeyValuePair<FlowObfuscationEntry, FlowObfuscationEntry>> memberClassEntry : changeLogContext.getMemberClassEntries().entrySet()) {
				ClassNode workspaceClassNode = ClassHierarchyNode.getWorkspaceClassNode(GlobalUtils.binaryToInternalName(memberClassEntry.getKey()));
				if (workspaceClassNode != null) {
					KeyValuePair<FlowObfuscationEntry, FlowObfuscationEntry> memberClassEntryPair = memberClassEntry.getValue();
					FlowObfuscationEntry firstMemberEntry = memberClassEntryPair.getKey();
					FlowObfuscationEntry secondMemberEntry = memberClassEntryPair.getValue();
					this.linkedClassesByName.put(firstMemberEntry.getInternalClassName(), workspaceClassNode);
					this.linkedClassesByName.put(secondMemberEntry.getInternalClassName(), workspaceClassNode);
				}
			}
		}

		List<ClassNode> linkedClasses = this.linkedClassesByName.get(internalClassName);
		if (linkedClasses != null && linkedClasses.size() > 0) {
			HashSet<PackageGroupingManager> groupingManagersWithCommonClass = GlobalUtils.newHashSet(7);
			HashSet<PackageGroupingManager> groupingManagersContainingAll = GlobalUtils.newHashSet(7);

			for (PackageGroupingManager packageGroupingManager : this.packageGroupingManagers) {
				boolean allClassesContained = true;
				boolean commonClassFound = false;

				for (ClassNode linkedClass : linkedClasses) {
					if (!packageGroupingManager.containsClass(linkedClass)) {
						allClassesContained = false;
						break;
					}

					if (packageGroupingManager.containsCommonClass(linkedClass)) {
						commonClassFound = true;
					}
				}

				if (allClassesContained) {
					groupingManagersContainingAll.add(packageGroupingManager);
					if (commonClassFound) {
						groupingManagersWithCommonClass.add(packageGroupingManager);
					}
				}
			}

			PackageGroupingManager selectedGroupingManager = null;
			if (groupingManagersWithCommonClass.size() == 1) {
				selectedGroupingManager = groupingManagersWithCommonClass.iterator().next();
			} else if (groupingManagersContainingAll.size() == 1) {
				selectedGroupingManager = groupingManagersContainingAll.iterator().next();
			}

			if (selectedGroupingManager != null) {
				CountingSet<ClassNode> groupingReferenceCounts = GlobalUtils.clone(classReferenceCounts);
				groupingReferenceCounts.retainAll(selectedGroupingManager.getCommonClasses());
				return groupingReferenceCounts;
			} else {
				return null;
			}
		} else if (this.linkedClassesByName.size() == 0 && this.packageGroupingManagers.size() == 1) {
			PackageGroupingManager soleGroupingManager = this.packageGroupingManagers.get(0);
			CountingSet<ClassNode> defaultReferenceCounts = GlobalUtils.clone(classReferenceCounts);
			defaultReferenceCounts.retainAll(soleGroupingManager.getCommonClasses());
			return defaultReferenceCounts;
		} else {
			return null;
		}
	}

	private MethodNode createFieldSetter(ClassNode classNode, FieldNode field, List<TypeWriter> writers, Inheritance inheritance, AbstractWorkspace workspace) {
		String setterDescriptor = Condition.buildSetterMethodDescriptor(field.getDescriptor());
		LocalVariableTable localVariableTable = new LocalVariableTable(true, setterDescriptor, 5);
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		this.addFieldSetterInstructions(instructions, localVariableTable, classNode, field, writers);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		MethodNode setterMethod = classNode.createPublicStaticMethod(setterDescriptor, instructions, 1, 1, localVariableTable, tryCatchBlocks, "Flow Obfuscation", writers, inheritance, workspace, 2);
		this.generatedMethods.add(setterMethod);
		return setterMethod;
	}

	private Condition getOrCreateFieldCondition(
		FlowObfuscationEntry entry,
		AbstractClassNode classNode,
		String remappedClassName,
		ChangeLogContext changeLogContext,
		Random random,
		DualKeyMap<AbstractClassNode, String, Condition> conditionsByClass,
		Map<ExcludeElement, String> fieldRenames,
		DualKeyMap<String, ExcludeElement, String> methodRenames,
		Inheritance inheritance,
		AbstractWorkspace workspace
	) {
		Condition condition = null;
		String originalFieldName = entry.getOriginalFieldName();
		String originalFieldDescriptor = entry.getOriginalFieldDescriptor();
		String humanReadableFieldType = entry.getHumanReadableFieldType();
		String internalClassName = classNode.getInternalName();
		if (!entry.isClassOnlyEntry()) {
			if (classNode.isWorkspaceNode()) {
				condition = conditionsByClass.get(classNode, originalFieldName);
				if (condition == null) {
					if (this.isAlreadyFlowObfuscated(entry, classNode, inheritance, new AtomicReference<>())) {
						condition = this.createFieldCondition((ClassNode)classNode, 4, inheritance, workspace, random);
						String renamedFieldName = condition.getFieldName();
						fieldRenames.put(new ExcludeElement(internalClassName, originalFieldName, humanReadableFieldType), renamedFieldName);
						if (entry.hasOriginalSetter()) {
							methodRenames.put(classNode.getInternalName(), new ExcludeElement(entry.getOriginalSetterName(), entry.getHumanReadableFieldType(), "void"), condition.getSetterMethodName());
							methodRenames.put(classNode.getInternalName(), new ExcludeElement(entry.getOriginalGetterName(), "", entry.getHumanReadableFieldType()), condition.getGetterMethodName());
							if (entry.getRemappedBooleanGetterName() != null) {
								methodRenames.put(classNode.getInternalName(), new ExcludeElement(entry.getOriginalBooleanGetterName(), "", entry.getHumanReadableFieldType()), condition.getBooleanGetterMethodName());
							}
						}
					} else {
						condition = this.createConditionFromEntry(entry, (ClassNode)classNode, 4, inheritance, workspace, random);
					}

					Condition previousCondition = conditionsByClass.put(classNode, condition.getFieldName(), condition);
				} else if (originalFieldDescriptor != null && !condition.getFieldDescriptor().equals(originalFieldDescriptor)) {
					changeLogContext.logError(
						"The field '"
							+ originalFieldName
							+ "' in class '"
							+ classNode.getBinaryName()
							+ "' appears in the change log more than once but with different types.  You must distribute this application as a whole."
					);
				}
			} else {
				condition = conditionsByClass.get(classNode, originalFieldName);
				if (condition == null) {
					condition = this.createLibraryCondition(entry, (LibraryClassNode)classNode, 4, inheritance);
				}
			}
		} else if (classNode.isWorkspaceNode()) {
			condition = this.createFieldCondition((ClassNode)classNode, 4, inheritance, workspace, random);
			Condition previousClassOnlyCondition = conditionsByClass.put(classNode, condition.getFieldName(), condition);
		} else {
			changeLogContext.logFatalError(
				"Class '"
					+ classNode.getBinaryName()
					+ "' appears in the change log flow obfuscation data but has not been opened for obfuscation yet its flow obfuscation data is incomplete.  For incremental obfuscation, the flow data must be complete."
			);
		}

		return condition;
	}

	private void addFieldGetterInstructions(ArrayList<AbstractInsnNode> instructions, LocalVariableTable localVariableTable, AbstractClassNode classNode, AbstractFieldNode field, List<TypeWriter> writers) {
		String fieldDescriptor = field.getDescriptor();
		FieldTypeWriter fieldRef = classNode.getConstantPoolWriter().getOrCreateFieldRef(field.getInternalName(), field.getNameDisplay(), field.getDescriptor(), writers, field);
		instructions.add(new WideConstantPoolInsnNode(GETSTATIC, fieldRef));
		if (!fieldDescriptor.equals("I") && !fieldDescriptor.equals("Z")) {
			instructions.add(InsnNode.of(ARETURN));
		} else {
			instructions.add(InsnNode.of(IRETURN));
		}
	}

	public Set<Condition> getPackageConditions() {
		HashSet<Condition> packageConditions = GlobalUtils.newHashSet();
		Iterator<Entry<AbstractClassNode, PackageCondition>> packageConditionIterator = this.packageConditionByClass.entrySet().iterator();

		while (packageConditionIterator.hasNext()) {
			PackageCondition packageCondition = packageConditionIterator.next().getValue();
			packageConditions.add(packageCondition.getGroupCondition());
			packageConditions.add(packageCondition.getPackageCondition());
		}

		return packageConditions;
	}

	private MethodNode createBooleanGetterMethod(ClassNode classNode, String methodName, MethodNode getterMethod, List<TypeWriter> writers, Inheritance inheritance, AbstractWorkspace workspace, Random random) {
		String getterDescriptor = getterMethod.getDescriptor();
		LocalVariableTable localVariableTable = new LocalVariableTable(true, getterDescriptor, 5);
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		this.buildBooleanAccessorBody(instructions, localVariableTable, classNode, getterMethod, writers, random);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		MethodNode booleanGetterMethod = classNode.createPublicStaticMethod(methodName, getterDescriptor, instructions, 1, 1, localVariableTable, tryCatchBlocks, "Flow Obfuscation", writers, inheritance, workspace, 2);
		this.generatedMethods.add(booleanGetterMethod);
		return booleanGetterMethod;
	}

	public boolean isDummyJumpClass(AbstractClassNode classNode) {
		return this.dummyJumpClasses.contains(classNode);
	}

	public boolean hasPackageConditions() {
		Iterator<Entry<AbstractClassNode, PackageCondition>> packageConditionIterator = this.packageConditionByClass.entrySet().iterator();

		while (packageConditionIterator.hasNext()) {
			PackageCondition packageCondition = packageConditionIterator.next().getValue();
			if (packageCondition.getGroupCondition().isPositiveCondition() || packageCondition.getPackageCondition().isPositiveCondition()) {
				return true;
			}
		}

		return false;
	}

	private Condition createConditionFromEntry(FlowObfuscationEntry entry, ClassNode classNode, int accessModifier, Inheritance inheritance, AbstractWorkspace workspace, Random random) {
		PackageGroupingManager grouping = this.groupingManagerByClass.get(classNode);
		String fieldDescriptor = entry.getOriginalFieldDescriptor();
		this.addFieldTypeToGrouping(grouping, fieldDescriptor);
		int fieldAccessLevel = entry.hasOriginalSetter() ? 1 : 4;
		FieldNode field = classNode.addStaticFieldIfAbsent(entry.getOriginalFieldName(), fieldDescriptor, fieldAccessLevel, inheritance, workspace, 2);
		Condition condition;
		if (entry.hasOriginalSetter()) {
			ArrayList<TypeWriter> typeWriters = new ArrayList<>();
			MethodNode setterMethod = this.createStaticSetterMethod(classNode, entry.getOriginalSetterName(), field, typeWriters, inheritance, workspace);
			MethodNode getterMethod = this.createFieldGetterMethod(classNode, entry.getOriginalGetterName(), field, typeWriters, inheritance, workspace);
			MethodNode booleanGetterMethod = null;
			String booleanGetterName = entry.getOriginalBooleanGetterName();
			if ((fieldDescriptor.equals("I") || fieldDescriptor.equals("Z")) && booleanGetterName != null) {
				booleanGetterMethod = this.createBooleanGetterMethod(classNode, booleanGetterName, getterMethod, typeWriters, inheritance, workspace, random);
			}

			classNode.getConstantPoolManager().appendEntriesToPool(typeWriters);
			condition = new Condition(field, setterMethod, getterMethod, booleanGetterMethod, entry.getProcessingFlag());
		} else {
			condition = new Condition(field);
		}

		return condition;
	}

	private LibraryMethodNode createLibraryBooleanGetter(LibraryClassNode classNode, String methodName, AbstractMethodNode getterMethod, List<TypeWriter> writers, Inheritance inheritance, Random random) {
		String descriptor = getterMethod.getDescriptor();
		LocalVariableTable localVariableTable = new LocalVariableTable(true, descriptor, 5);
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		this.buildBooleanAccessorBody(instructions, localVariableTable, classNode, getterMethod, writers, random);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		return classNode.createPublicStaticMethod(methodName, descriptor, instructions, 1, 1, localVariableTable, tryCatchBlocks, writers, inheritance, "Flow Obfuscation", 2);
	}

	public boolean isConditionalBlockClass(AbstractClassNode classNode) {
		return this.conditionalBlockClasses.contains(classNode);
	}

	public Random getRandom() {
		return this.random;
	}

	public int getPackageGroupingCount() {
		return this.packageGroupingManagers.size();
	}

	private String getRandomFieldType(PackageGroupingManager grouping, Random random) {
		OrderedSet<String> fieldTypes = this.fieldTypesByGrouping.get(grouping);
		int fieldTypeIndex = random.nextInt(fieldTypes.size());
		return fieldTypes.get(fieldTypeIndex);
	}

	public int getConditionCount() {
		return this.conditions.size();
	}

	private void assignGroupingConditions(
		Iterator<ClassNode> classIterator,
		PackageGroupingManager grouping,
		Random random,
		DualKeyMap<AbstractClassNode, String, Condition> conditionsByClass,
		ReferenceGraph<AbstractClassNode, FieldSignature> fieldReferenceGraph,
		Inheritance inheritance,
		StaticBlockAnalyzer staticBlockAnalyzer,
		AbstractWorkspace workspace,
		int packageCount,
		FlowObfuscateExclude flowObfuscateExclude,
		ClassInitializationOrderBuilder initOrderBuilder,
		int pass
	) {
		while (classIterator.hasNext() && !grouping.hasAllConditions()) {
			ClassNode classNode = classIterator.next();
			classNode.getInternalName();
			if ((pass != 1 || staticBlockAnalyzer.hasOnlyLeafDependencies(classNode))
				&& !conditionsByClass.containsGroup(classNode)
				&& this.isFlowObfuscationEligible(classNode, flowObfuscateExclude, initOrderBuilder, workspace, packageCount == 1 && pass != 1 && pass != 2)) {
				if (grouping.getGroupingCondition() == null) {
					if (this.useFieldAccessors() && !Options.dontUseStringFlowLocals) {
						this.addClassArrayType(grouping, classNode);
					}

					Condition condition = this.createFieldCondition(classNode, 4, inheritance, workspace, random);
					this.conditions.add(condition);
					fieldReferenceGraph.addReference(classNode, condition.getFieldSignature());
					Condition previousCondition = conditionsByClass.put(classNode, condition.getFieldName(), condition);
					grouping.setGroupingCondition(condition);
				} else {
					boolean unmappedPackageFound = false;
					String packageName = null;
					if (pass != 1 && pass != 2 && pass != 3) {
						Iterator<String> packageNameIterator = grouping.getSortedPackageNames().iterator();

						while (packageNameIterator.hasNext()) {
							packageName = packageNameIterator.next();
							if (grouping.getPackageCondition(packageName) == null) {
								unmappedPackageFound = true;
								break;
							}
						}
					} else {
						packageName = classNode.getPackageName();
						if (grouping.getPackageCondition(packageName) != null) {
							continue;
						}

						unmappedPackageFound = true;
					}

					if (unmappedPackageFound) {
						Condition packageCondition = this.createFieldCondition(classNode, 4, inheritance, workspace, random);
						this.conditionsByPackage.put(packageName, packageCondition);
						conditionsByClass.put(classNode, packageCondition.getFieldName(), packageCondition);
						fieldReferenceGraph.addReference(classNode, packageCondition.getFieldSignature());
						grouping.putPackageCondition(packageName, packageCondition);
					}
				}
			}
		}
	}

	public boolean isGeneratedMethod(MethodNode methodNode) {
		return this.generatedMethods.contains(methodNode);
	}

	private Condition processFlowObfuscationEntry(
		FlowObfuscationEntry entry,
		ReferenceGraph<String, AbstractClassNode> classReferenceGraph,
		CountingSet<ClassNode> referencedClassCounts,
		DefaultExcludes defaultExcludes,
		Random random,
		DualKeyMap<AbstractClassNode, String, Condition> conditionsByClass,
		Map<String, String> classRenames,
		Map<ExcludeElement, String> fieldRenames,
		DualKeyMap<String, KeyValuePair<String, String>, KeyValuePair<String, String>> removedFields,
		Map<ExcludeElement, String> addedFields,
		DualKeyMap<String, ExcludeElement, String> methodRenames,
		Map<String, ExcludeElement> removedMethods,
		DualKeyMap<String, ExcludeElement, String> addedMethods,
		boolean traceBackClass,
		int packageCount,
		AbstractWorkspace workspace,
		Map<String, ClassNode> classNodesByOriginalName,
		FlowObfuscateExclude flowObfuscateExclude,
		ClassInitializationOrderBuilder initOrderBuilder,
		Inheritance inheritance
	) {
		String internalClassName = entry.getInternalClassName();
		entry.getOriginalFieldName();
		entry.getOriginalFieldDescriptor();
		entry.getHumanReadableFieldType();
		Condition condition = null;
		String binaryClassName = GlobalUtils.internalToBinaryName(internalClassName);
		this.changeLogContext.getOriginalFieldNames(internalClassName);
		if (!this.changeLogContext.hasOldClass(internalClassName) && !this.changeLogContext.wasClassPruned(internalClassName)) {
			this.changeLogContext
				.logFatalError(
					"Class '"
						+ binaryClassName
						+ "' appears as a Flow Obfuscation "
						+ (traceBackClass ? "TraceBackClass:" : "ForwardClass:")
						+ " but does not appear in the main part of the change log. (C)"
				);
		} else {
			String remappedClassName = entry.getRemappedClassName();
			if (!entry.isClassOnlyEntry() && !entry.isPartiallyMapped()) {
				this.changeLogContext
					.logFatalError(
						"Class '"
							+ binaryClassName
							+ "' appears as a Flow Obfuscation "
							+ (traceBackClass ? "TraceBackClass:" : "ForwardClass:")
							+ " but its entry in the main part of the change log is corrupt."
					);
			} else {
				AbstractClassNode classNode = ClassHierarchyNode.getClassNodeByInternalName(internalClassName);
				if (classNode != null) {
					AtomicReference<String> errorMessage = new AtomicReference<>();
					if (entry.isNonStringObjectField()) {
						PackageGroupingManager grouping = this.groupingManagerByClass.get(classNode);
						String fieldTypeInternalName = entry.getFieldTypeInternalName();
						AbstractClassNode fieldTypeClass = ClassHierarchyNode.getClassNodeByInternalName(fieldTypeInternalName);
						if (fieldTypeClass == null || !grouping.containsCommonClass(fieldTypeClass)) {
							errorMessage.set(
								"Type '"
									+ GlobalUtils.internalToBinaryName(fieldTypeInternalName)
									+ "' appears in Flow Obfuscation "
									+ (traceBackClass ? "TraceBackClass:" : "ForwardClass:")
									+ " '"
									+ binaryClassName
									+ "' but it is no longer available within the corresponding group. You must distribute this application as a whole."
							);
						}
					}

					if (errorMessage.isNull()) {
						if (this.isAlreadyFlowObfuscated(entry, classNode, inheritance, errorMessage)) {
							this.changeLogContext.logError(errorMessage.get());
						} else if (!classNode.isInterface()) {
							if (!this.isObfuscatedFieldExcluded(entry, classNode, removedFields, removedMethods, defaultExcludes, workspace, errorMessage)) {
								if (!classNode.isPublic() && packageCount != 1) {
									this.changeLogContext
										.logError(
											"Class '"
												+ binaryClassName
												+ "' appears as a Flow Obfuscation "
												+ (traceBackClass ? "TraceBackClass:" : "ForwardClass:")
												+ " but it is no longer public. Another class had to be used. You must distribute this application as a whole."
										);
								} else {
									condition = this.getOrCreateFieldCondition(entry, classNode, remappedClassName, this.changeLogContext, random, conditionsByClass, fieldRenames, methodRenames, inheritance, workspace);
								}
							} else {
								this.changeLogContext.logError(errorMessage.get());
							}
						} else {
							this.changeLogContext
								.logError(
									"Class '"
										+ binaryClassName
										+ "' appears as a Flow Obfuscation "
										+ (traceBackClass ? "TraceBackClass:" : "ForwardClass:")
										+ " but it is now an interface. Another class had to be used.  You must distribute this application as a whole."
								);
						}
					} else {
						this.changeLogContext.logError(errorMessage.get());
					}
				} else {
					AbstractClassNode referencingLibraryClass = this.getLibraryClassNode(internalClassName, classReferenceGraph);
					if (referencingLibraryClass == null) {
						ClassNode substituteClass = this.findSubstituteClass(entry, referencedClassCounts, classNodesByOriginalName, this.changeLogContext, defaultExcludes, workspace, flowObfuscateExclude, initOrderBuilder);
						if (substituteClass != null) {
							classRenames.put(internalClassName, substituteClass.getInternalName());
							String humanReadableDescriptor = entry.getHumanReadableFieldType();
							condition = this.getOrCreateFieldCondition(entry, substituteClass, remappedClassName, this.changeLogContext, random, conditionsByClass, fieldRenames, methodRenames, inheritance, workspace);
							addedFields.put(new ExcludeElement(substituteClass.getInternalName(), condition.getFieldName(), humanReadableDescriptor), entry.getRemappedFieldName());
							if (entry.hasOriginalSetter()) {
								if (condition.hasSetterMethod()) {
									AbstractMethodNode setterMethod = condition.getSetterMethod();
									addedMethods.put(substituteClass.getInternalName(), new ExcludeElement(setterMethod.getNameDisplay(), humanReadableDescriptor, "void"), entry.getRemappedSetterName());
								}

								if (condition.hasGetterMethod()) {
									AbstractMethodNode getterMethod = condition.getGetterMethod();
									addedMethods.put(substituteClass.getInternalName(), new ExcludeElement(getterMethod.getNameDisplay(), "", humanReadableDescriptor), entry.getRemappedGetterName());
								}

								if (condition.hasBooleanGetterMethod()) {
									AbstractMethodNode booleanGetterMethod = condition.getBooleanGetterMethod();
									addedMethods.put(substituteClass.getInternalName(), new ExcludeElement(booleanGetterMethod.getNameDisplay(), "", humanReadableDescriptor), entry.getRemappedBooleanGetterName());
								}
							}
						} else {
							this.changeLogContext
								.logError(
									"Class '"
										+ binaryClassName
										+ "' appears as a Flow Obfuscation "
										+ (traceBackClass ? "TraceBackClass:" : "ForwardClass:")
										+ " but is no longer available. Another class had to be used.  You must distribute this application as a whole."
								);
						}
					} else {
						this.changeLogContext
							.logFatalError(
								"Class '"
									+ binaryClassName
									+ "' appears as a Flow Obfuscation "
									+ (traceBackClass ? "TraceBackClass:" : "ForwardClass:")
									+ " but could not be found. However, it must be referenced by '"
									+ referencingLibraryClass.getBinaryName()
									+ "' which has not be opened."
							);
					}
				}
			}
		}

		return condition;
	}

	private void addFieldSetterInstructions(ArrayList<AbstractInsnNode> instructions, LocalVariableTable localVariableTable, AbstractClassNode classNode, AbstractFieldNode field, List<TypeWriter> writers) {
		String fieldDescriptor = field.getDescriptor();
		FieldTypeWriter fieldRef = classNode.getConstantPoolWriter().getOrCreateFieldRef(field.getInternalName(), field.getNameDisplay(), field.getDescriptor(), writers, field);
		if (!fieldDescriptor.equals("I") && !fieldDescriptor.equals("Z")) {
			instructions.add(AbstractInsnNode.loadObject(0, localVariableTable, 2));
		} else {
			instructions.add(AbstractInsnNode.loadInt(0, localVariableTable, 2));
		}

		instructions.add(new WideConstantPoolInsnNode(PUTSTATIC, fieldRef));
		instructions.add(InsnNode.of(RETURN));
	}

	private boolean isAlreadyFlowObfuscated(FlowObfuscationEntry entry, AbstractClassNode classNode, Inheritance inheritance, AtomicReference<String> errorMessage) {
		if (entry.isClassOnlyEntry()) {
			return false;
		} else {
			String fieldDescriptor = entry.getOriginalFieldDescriptor();
			String humanReadableDescriptor = entry.getHumanReadableFieldType();
			String originalClassName = entry.getOriginalClassName();
			String alreadyUsedMessage = "' is associated with a Flow Obfuscation "
				+ (entry.isTraceBackClassEntry() ? "TraceBackClass:" : "ForwardClass:")
				+ " but it has been used in an earlier flow obfuscation pass. You have probably used the wrong input change log! Otherwise, you must distribute this application as a whole.";
			Set<FieldSignature> fieldSignatures = inheritance.getFieldSignatures(classNode);
			Set<MethodParameterKey> methodParameterKeys = inheritance.getMethodParameterKeys(classNode);
			if (fieldSignatures != null && fieldSignatures.contains(new FieldSignature(entry.getOriginalFieldName(), fieldDescriptor))) {
				errorMessage.set("Field '" + humanReadableDescriptor + " " + entry.getOriginalFieldName() + "' in class '" + originalClassName + alreadyUsedMessage);
				return true;
			} else if (entry.getOriginalSetterName() != null
				&& methodParameterKeys != null
				&& methodParameterKeys.contains(new MethodParameterKey(entry.getOriginalSetterName(), Condition.buildSetterMethodDescriptor(fieldDescriptor)))) {
				errorMessage.set("Method '" + entry.getOriginalSetterName() + Condition.buildSetterMethodDescriptor(fieldDescriptor) + "' in class '" + originalClassName + alreadyUsedMessage);
				return true;
			} else if (entry.getOriginalGetterName() != null
				&& methodParameterKeys != null
				&& methodParameterKeys.contains(new MethodParameterKey(entry.getOriginalGetterName(), Condition.buildGetterMethodDescriptor(fieldDescriptor)))) {
				errorMessage.set("Method '" + entry.getOriginalGetterName() + Condition.buildGetterMethodDescriptor(fieldDescriptor) + "' in class '" + originalClassName + alreadyUsedMessage);
				return true;
			} else if (entry.getOriginalBooleanGetterName() != null
				&& methodParameterKeys != null
				&& methodParameterKeys.contains(new MethodParameterKey(entry.getOriginalBooleanGetterName(), Condition.buildGetterMethodDescriptor(fieldDescriptor)))) {
				errorMessage.set("Method '" + entry.getOriginalBooleanGetterName() + Condition.buildGetterMethodDescriptor(fieldDescriptor) + "' in class '" + originalClassName + alreadyUsedMessage);
				return true;
			} else {
				return false;
			}
		}
	}

	private void addClassArrayType(PackageGroupingManager grouping, AbstractClassNode classNode) {
		StringBuilder typeDescriptorBuffer = new StringBuilder(classNode.getInternalName().length() + 3);
		typeDescriptorBuffer.append('[');
		typeDescriptorBuffer.append('L');
		typeDescriptorBuffer.append(classNode.getInternalName());
		typeDescriptorBuffer.append(';');
		this.fieldTypesByGrouping.get(grouping).add(typeDescriptorBuffer.toString());
	}

	private MethodNode createFieldGetterMethod(ClassNode classNode, String methodName, FieldNode field, List<TypeWriter> writers, Inheritance inheritance, AbstractWorkspace workspace) {
		String descriptor = Condition.buildGetterMethodDescriptor(field.getDescriptor());
		LocalVariableTable localVariableTable = new LocalVariableTable(true, descriptor, 5);
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		this.addFieldGetterInstructions(instructions, localVariableTable, classNode, field, writers);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		MethodNode staticMethod = classNode.createPublicStaticMethod(methodName, descriptor, instructions, 1, 0, localVariableTable, tryCatchBlocks, "Flow Obfuscation", writers, inheritance, workspace, 2);
		this.generatedMethods.add(staticMethod);
		return staticMethod;
	}

	private Condition createFieldCondition(ClassNode classNode, int accessModifier, Inheritance inheritance, AbstractWorkspace workspace, Random random) {
		PackageGroupingManager grouping = this.groupingManagerByClass.get(classNode);
		String fieldType = this.getRandomFieldType(grouping, random);
		Condition condition;
		if (this.useFieldAccessors()) {
			FieldNode fieldWithAccessors = classNode.createUniqueField(fieldType, 1, false, inheritance, workspace, 2);
			ArrayList<TypeWriter> typeWriters = new ArrayList<>();
			MethodNode setterMethod = this.createFieldSetter(classNode, fieldWithAccessors, typeWriters, inheritance, workspace);
			MethodNode getterMethod = this.createStaticGetterMethod(classNode, fieldWithAccessors, typeWriters, inheritance, workspace);
			MethodNode booleanGetterMethod = null;
			if (fieldType.equals("I") || fieldType.equals("Z")) {
				booleanGetterMethod = this.createBooleanAccessorMethod(classNode, getterMethod, typeWriters, inheritance, workspace, random);
			}

			classNode.getConstantPoolManager().appendEntriesToPool(typeWriters);
			condition = new Condition(fieldWithAccessors, setterMethod, getterMethod, booleanGetterMethod, random);
		} else {
			FieldNode fieldWithoutAccessors = classNode.createUniqueField(fieldType, accessModifier, false, inheritance, workspace, 2);
			condition = new Condition(fieldWithoutAccessors);
		}

		return condition;
	}

	public ReferenceGraph<AbstractClassNode, FieldSignature> buildGroupingConditions(
		HashSet<String> packageNames,
		DefaultExcludes defaultExcludes,
		ClassTreeManager classTreeManager,
		Inheritance inheritance,
		AbstractWorkspace workspace,
		FlowObfuscateExclude flowObfuscateExclude,
		ClassInitializationOrderBuilder initOrderBuilder
	) {
		if (this.licenseChecker != null) {
			try {
				Class<?> licenseCheckerClass = this.licenseChecker.getClass();
				String licenseMethodName = this.getReflectionTargetName(0);
				Class<?>[] noParameterTypes = new Class[0];
				Method licenseMethod = licenseCheckerClass.getDeclaredMethod(licenseMethodName, noParameterTypes);
				if (licenseMethod != null) {
					licenseMethod.invoke(null);
				}
			} catch (Throwable ignored) {
			}
		}

		ReferenceGraph<AbstractClassNode, FieldSignature> classReferenceGraph = new ReferenceGraph<>();
		if (this.changeLogContext != null) {
			ArrayList<String> oldClassNames = this.changeLogContext.getOldClassNames();

			for (int oldClassIndex = 0; oldClassIndex < oldClassNames.size(); oldClassIndex++) {
				packageNames.add(AbstractClassNode.getPackageName(GlobalUtils.binaryToInternalName(oldClassNames.get(oldClassIndex))));
			}
		}

		int packageCount = packageNames.size();
		int classCount = this.classNodes.length;
		CountingSet<ClassNode> classReferenceCounts = new CountingSet<>(classCount);
		CountingSet<ClassNode> secondaryClassCounts = new CountingSet<>(1);

		for (int classIndex = 0; classIndex < classCount; classIndex++) {
			this.classNodes[classIndex].collectReferencedClasses(classReferenceCounts);
		}

		List<ClassHierarchyNode> classRoots = classTreeManager.getWorkspaceClassRoots();
		int classRootCount = classRoots.size();

		for (int classRootIndex = 0; classRootIndex < classRootCount; classRootIndex++) {
			ClassHierarchyNode classRoot = classRoots.get(classRootIndex);
			AtomicInt secondaryTotalCount;
			AtomicInt totalCount = secondaryTotalCount = new AtomicInt(0);
			classRoot.accumulateSubtreeCounts(totalCount, secondaryTotalCount, classReferenceCounts, secondaryClassCounts);
		}

		DualKeyMap<AbstractClassNode, String, Condition> conditionsByClassAndField = new DualKeyMap<>();
		this.fieldTypesByGrouping = this.buildFieldTypesByGrouping();
		if (this.changeLogContext != null) {
			this.buildConditionsFromChangeLog(packageNames, classReferenceCounts, defaultExcludes, flowObfuscateExclude, initOrderBuilder, this.random, conditionsByClassAndField, classReferenceGraph, inheritance, workspace);
		}

		for (PackageGroupingManager grouping : this.packageGroupingManagers) {
			for (Condition condition : this.conditions) {
				AbstractClassNode conditionOwnerClass = ClassHierarchyNode.getClassNodeByInternalName(condition.getOwnerInternalName());
				if (grouping.containsCommonClass(conditionOwnerClass)) {
					grouping.setGroupingCondition(condition);
					break;
				}
			}

			for (String packageName : grouping.getSortedPackageNames()) {
				List<Condition> packageConditions = this.conditionsByPackage.get(packageName);
				if (packageConditions != null) {
					for (Condition packageCondition : packageConditions) {
						AbstractClassNode packageConditionOwnerClass = ClassHierarchyNode.getClassNodeByInternalName(packageCondition.getOwnerInternalName());
						if (grouping.containsCommonClass(packageConditionOwnerClass)) {
							grouping.putPackageCondition(packageName, packageCondition);
							break;
						}
					}
				}
			}

			CountingSet<ClassNode> groupingReferencedClassCounts = GlobalUtils.clone(classReferenceCounts);
			groupingReferencedClassCounts.retainAll(grouping.getCommonClasses());
			List<ClassNode> referencedClasses = groupingReferencedClassCounts.getSortedByCount(false);
			Iterator<ClassNode> referencedClassIterator = referencedClasses.iterator();
			this.assignGroupingConditions(referencedClassIterator, grouping, this.random, conditionsByClassAndField, classReferenceGraph, inheritance, this.staticBlockAnalyzer, workspace, packageCount, flowObfuscateExclude, initOrderBuilder, 1);
			if (!grouping.hasAllConditions()) {
				this.assignGroupingConditions(referencedClassIterator, grouping, this.random, conditionsByClassAndField, classReferenceGraph, inheritance, this.staticBlockAnalyzer, workspace, packageCount, flowObfuscateExclude, initOrderBuilder, 2);
			}

			if (packageCount == 1 && !grouping.hasAllConditions()) {
				referencedClassIterator = referencedClasses.iterator();
				this.assignGroupingConditions(referencedClassIterator, grouping, this.random, conditionsByClassAndField, classReferenceGraph, inheritance, this.staticBlockAnalyzer, workspace, packageCount, flowObfuscateExclude, initOrderBuilder, 3);
			}

			Condition groupingCondition = grouping.getGroupingCondition();
			if (groupingCondition != null) {
				if (!grouping.hasAllConditions()) {
					referencedClassIterator = referencedClasses.iterator();
					this.assignGroupingConditions(referencedClassIterator, grouping, this.random, conditionsByClassAndField, classReferenceGraph, inheritance, this.staticBlockAnalyzer, workspace, packageCount, flowObfuscateExclude, initOrderBuilder, 4);
				}

				if (!grouping.hasAllConditions()) {
					AbstractClassNode groupingConditionOwnerClass = ClassHierarchyNode.getClassNodeByInternalName(groupingCondition.getOwnerInternalName());
					if (groupingConditionOwnerClass.isWorkspaceNode()) {
						for (String unmappedPackageName : grouping.getSortedPackageNames()) {
							if (grouping.getPackageCondition(unmappedPackageName) == null) {
								Condition newPackageCondition = this.createFieldCondition((ClassNode)groupingConditionOwnerClass, 4, inheritance, workspace, this.random);
								this.conditionsByPackage.put(unmappedPackageName, newPackageCondition);
								conditionsByClassAndField.put(groupingConditionOwnerClass, newPackageCondition.getFieldName(), newPackageCondition);
								grouping.putPackageCondition(unmappedPackageName, newPackageCondition);
							}
						}
					}
				}
			}
		}

		for (PackageGroupingManager groupingForDefaults : this.packageGroupingManagers) {
			Condition defaultGroupingCondition = groupingForDefaults.getGroupingCondition();

			for (Condition defaultPackageCondition : groupingForDefaults.getPackageConditions()) {
				this.packageConditionDefaults.put(defaultPackageCondition, defaultGroupingCondition);
			}
		}

		for (int classNodeIndex = 0; classNodeIndex < this.classNodes.length; classNodeIndex++) {
			ClassNode classNode = this.classNodes[classNodeIndex];
			PackageGroupingManager classGrouping = this.groupingManagerByClass.get(classNode);
			if (classGrouping != null) {
				Condition classGroupingCondition = classGrouping.getGroupingCondition();
				if (classGroupingCondition != null && !this.packageConditionByClass.containsKey(classNode)) {
					Condition classPackageCondition = classGrouping.getPackageCondition(classNode.getPackageName());
					this.packageConditionByClass.put(classNode, new PackageCondition(classGroupingCondition, classPackageCondition, classNode.getPackageName()));
				}

				if (classGroupingCondition != null && classNode.hasInnerClasses()) {
					PackageCondition registeredPackageCondition = this.packageConditionByClass.get(classNode);

					for (AbstractClassNode innerClass : classNode.getInnerClasses()) {
						this.packageConditionByClass.put(innerClass, registeredPackageCondition);
					}
				}
			}
		}

		return classReferenceGraph;
	}

	public Map<PackageGroupingManager, OrderedSet<String>> buildFieldTypesByGrouping() {
		HashMap<PackageGroupingManager, OrderedSet<String>> fieldTypesPerGrouping = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(this.packageGroupingManagers.size()));
		OrderedSet<String> defaultFieldTypes = new OrderedSet<>();
		if (Options.useObjectFlowLocals) {
			defaultFieldTypes.add("Ljava/lang/String;");
			defaultFieldTypes.add("[I");
			defaultFieldTypes.add("[Ljava/lang/String;");
		} else {
			defaultFieldTypes.add("I");
			defaultFieldTypes.add("Z");
			if (this.useFieldAccessors() && !Options.dontUseStringFlowLocals) {
				defaultFieldTypes.add("Ljava/lang/String;");
				defaultFieldTypes.add("[I");
				defaultFieldTypes.add("[Ljava/lang/String;");
			}
		}

		for (PackageGroupingManager grouping : this.packageGroupingManagers) {
			fieldTypesPerGrouping.put(grouping, new OrderedSet<>(defaultFieldTypes));
		}

		return fieldTypesPerGrouping;
	}

	public MultiMap<String, Condition> getPackageLevelConditions() {
		MultiMap<String, Condition> conditionsByPackageName = new MultiMap<>(this.conditionsByPackage.size());
		DualKeyMap<String, Condition, Condition> seenPackageConditions = new DualKeyMap<>();

		for (PackageCondition packageCondition : this.packageConditionByClass.values()) {
			String packageName = packageCondition.getPackageName();
			Condition condition = packageCondition.getPackageCondition();
			if (seenPackageConditions.put(packageName, condition, condition) == null) {
				conditionsByPackageName.put(packageName, condition);
			}
		}

		return conditionsByPackageName;
	}

	private MethodNode createBooleanAccessorMethod(ClassNode classNode, MethodNode getterMethod, List<TypeWriter> typeWriters, Inheritance inheritance, AbstractWorkspace workspace, Random random) {
		String descriptor = getterMethod.getDescriptor();
		LocalVariableTable localVariableTable = new LocalVariableTable(true, descriptor, 5);
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		this.buildBooleanAccessorBody(instructions, localVariableTable, classNode, getterMethod, typeWriters, random);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		MethodNode staticMethod = classNode.createPublicStaticMethod(descriptor, instructions, 1, 1, localVariableTable, tryCatchBlocks, "Flow Obfuscation", typeWriters, inheritance, workspace, 2);
		this.generatedMethods.add(staticMethod);
		return staticMethod;
	}

	private AbstractClassNode getLibraryClassNode(String className, ReferenceGraph<String, AbstractClassNode> referenceGraph) {
		Set<AbstractClassNode> referencingClasses = referenceGraph.getReferences(className);
		if (referencingClasses != null) {
			for (AbstractClassNode referencingClass : referencingClasses) {
				if (!referencingClass.isWorkspaceNode()) {
					return referencingClass;
				}
			}
		}

		return null;
	}

	public List<Condition> getGroupingConditions() {
		HashSet<Condition> groupingConditions = GlobalUtils.newHashSet();

		for (PackageCondition packageCondition : this.packageConditionByClass.values()) {
			groupingConditions.add(packageCondition.getGroupCondition());
		}

		return new ArrayList<>(groupingConditions);
	}

	private boolean isFlowObfuscationEligible(ClassNode classNode, FlowObfuscateExclude flowObfuscateExclude, ClassInitializationOrderBuilder initOrderBuilder, AbstractWorkspace workspace, boolean allowNonPublicClasses) {
		String internalClassName = classNode.getInternalName();
		Set<String> annotationTypeNames = classNode.getAnnotationTypeNameSet();
		return !classNode.isInterface()
			&& (classNode.isPublic() || allowNonPublicClasses)
			&& !classNode.hasInnerClassInfo()
			&& (flowObfuscateExclude == null || !flowObfuscateExclude.isClassFlowExcluded(classNode))
			&& (initOrderBuilder == null || !initOrderBuilder.isInInitializationOrder(classNode))
			&& !workspace.isClassInstanceOf(internalClassName, "org/eclipse/osgi/util/NLS")
			&& !workspace.isSubclassOf(internalClassName, "javax/ejb/EnterpriseBean")
			&& (annotationTypeNames == null || !annotationTypeNames.contains("javax/ejb/Stateful") && !annotationTypeNames.contains("javax/ejb/Stateless") && !annotationTypeNames.contains("javax/ejb/MessageDriven"));
	}

	private void buildBooleanAccessorBody(
		ArrayList<AbstractInsnNode> instructions, LocalVariableTable localVariableTable, AbstractClassNode classNode, AbstractMethodNode getterMethod, List<TypeWriter> typeWriters, Random random
	) {
		MethodCallWriter methodRef = classNode.getConstantPoolWriter().getOrCreateClassMethodRef(getterMethod, typeWriters);
		instructions.add(new WideConstantPoolInsnNode(INVOKESTATIC, methodRef));
		instructions.add(AbstractInsnNode.storeInt(0, localVariableTable, 2));
		instructions.add(AbstractInsnNode.loadInt(0, localVariableTable, 2));
		LabelNode jumpTarget = new LabelNode(true, 1);
		instructions.add(new JumpInsnNode(IFNE, jumpTarget));
		if (getterMethod.getDescriptor().equals("()Z")) {
			instructions.add(InsnNode.of(ICONST_1));
		} else {
			instructions.add(AbstractInsnNode.newInt(random.nextInt(126) + 1));
		}

		instructions.add(InsnNode.of(IRETURN));
		instructions.add(jumpTarget);
		instructions.add(InsnNode.of(ICONST_0));
		instructions.add(InsnNode.of(IRETURN));
	}

	private boolean isGroupedWithConditionClass(AbstractClassNode classNode, Condition condition) {
		AbstractClassNode conditionOwnerClass = ClassHierarchyNode.getClassNodeByInternalName(condition.getOwnerInternalName());
		if (this.groupingsByClass == null) {
			this.groupingsByClass = new MultiMap<>(this.flowObfuscationClasses.length);

			for (int groupingIndex = 0; groupingIndex < this.groupings.length; groupingIndex++) {
				Iterator<?> classIterator = this.groupings[groupingIndex].iterator();

				while (classIterator.hasNext()) {
					this.groupingsByClass.put((AbstractClassNode)classIterator.next(), this.groupings[groupingIndex]);
				}
			}
		}

		List<NamedSet<? extends AbstractClassNode>> classGroupings = this.groupingsByClass.get(classNode);
		if (classGroupings != null) {
			for (NamedSet<? extends AbstractClassNode> grouping : classGroupings) {
				if (!grouping.contains(conditionOwnerClass)) {
					return false;
				}
			}
		}

		return true;
	}

	private Condition createLibraryCondition(FlowObfuscationEntry entry, LibraryClassNode libraryClassNode, int unused, Inheritance inheritance) {
		PackageGroupingManager grouping = this.groupingManagerByClass.get(libraryClassNode);
		String fieldDescriptor = entry.getOriginalFieldDescriptor();
		this.addFieldTypeToGrouping(grouping, fieldDescriptor);
		libraryClassNode.getInternalName();
		LibraryFieldNode field = (LibraryFieldNode)libraryClassNode.findField(entry.getOriginalFieldName(), fieldDescriptor);
		if (field == null) {
			int fieldAccessLevel = entry.hasOriginalSetter() ? 1 : 4;
			String fieldName = entry.getOriginalFieldName();
			field = libraryClassNode.addStaticFieldIfAbsent(fieldName, fieldDescriptor, fieldAccessLevel, false, 2);
			inheritance.registerField(field);
		}

		Condition condition;
		if (entry.hasOriginalSetter()) {
			ArrayList<TypeWriter> typeWriters = new ArrayList<>();
			Object setterMethod = libraryClassNode.findMethod(new MethodSignature(entry.getOriginalSetterName(), Condition.buildSetterMethodDescriptor(fieldDescriptor)));
			if (setterMethod == null) {
				setterMethod = this.createLibrarySetterMethod(libraryClassNode, entry.getOriginalSetterName(), field, typeWriters, inheritance);
			}

			Object getterMethod = libraryClassNode.findMethod(new MethodSignature(entry.getOriginalGetterName(), Condition.buildGetterMethodDescriptor(fieldDescriptor)));
			if (getterMethod == null) {
				getterMethod = this.createLibraryGetterMethod(libraryClassNode, entry.getOriginalGetterName(), field, typeWriters, inheritance);
			}

			Object booleanGetterMethod = null;
			if (fieldDescriptor.equals("I") || fieldDescriptor.equals("Z")) {
				booleanGetterMethod = libraryClassNode.findMethod(new MethodSignature(entry.getOriginalBooleanGetterName(), Condition.buildGetterMethodDescriptor(fieldDescriptor)));
				if (booleanGetterMethod == null) {
					booleanGetterMethod = this.createLibraryBooleanGetter(libraryClassNode, entry.getOriginalBooleanGetterName(), (AbstractMethodNode)getterMethod, typeWriters, inheritance, this.random);
				}
			}

			libraryClassNode.getConstantPoolWriter().appendEntriesToPool(typeWriters);
			condition = new Condition(field, (AbstractMethodNode)setterMethod, (AbstractMethodNode)getterMethod, (AbstractMethodNode)booleanGetterMethod, entry.getProcessingFlag());
		} else {
			condition = new Condition(field);
		}

		return condition;
	}

	public List<PackageGroupingManager> getPackageGroupingManagers() {
		return this.packageGroupingManagers;
	}

	private ClassNode findSubstituteClass(
		FlowObfuscationEntry entry,
		CountingSet<ClassNode> classReferenceCounts,
		Map<String, ClassNode> substituteClassByClassName,
		ChangeLogContext changeLogContext,
		DefaultExcludes defaultExcludes,
		AbstractWorkspace workspace,
		FlowObfuscateExclude flowObfuscateExclude,
		ClassInitializationOrderBuilder initOrderBuilder
	) {
		if (entry.isClassOnlyEntry()) {
			return null;
		} else {
			String fieldName = entry.getOriginalFieldName();
			String internalClassName = entry.getInternalClassName();
			String remappedClassName = entry.getRemappedClassName();
			String fieldDescriptor = entry.getOriginalFieldDescriptor();
			if (substituteClassByClassName.containsKey(internalClassName)) {
				ClassNode substituteClass = substituteClassByClassName.get(internalClassName);
				String substituteInternalName = substituteClass.getInternalName();
				FieldNode field = workspace.getFieldByNameAndDescriptor(substituteInternalName, fieldName, fieldDescriptor);
				MethodNode setterMethod = entry.getOriginalSetterName() != null
					? workspace.getMethodBySignature(substituteClass, new MethodSignature(entry.getRemappedSetterName(), Condition.buildSetterMethodDescriptor(fieldDescriptor)))
					: null;
				MethodNode getterMethod = entry.getOriginalGetterName() != null
					? workspace.getMethodBySignature(substituteClass, new MethodSignature(entry.getRemappedGetterName(), Condition.buildGetterMethodDescriptor(fieldDescriptor)))
					: null;
				MethodNode booleanGetterMethod = entry.getOriginalBooleanGetterName() != null
					? workspace.getMethodBySignature(substituteClass, new MethodSignature(entry.getRemappedBooleanGetterName(), Condition.buildGetterMethodDescriptor(fieldDescriptor)))
					: null;
				if ((field == null || !defaultExcludes.isFieldExcluded(field))
					&& (setterMethod == null || !defaultExcludes.isMethodExcluded(setterMethod))
					&& (getterMethod == null || !defaultExcludes.isMethodExcluded(getterMethod))
					&& (booleanGetterMethod == null || !defaultExcludes.isMethodExcluded(booleanGetterMethod))) {
					return substituteClass;
				}
			}

			if (changeLogContext.hasNewClass(remappedClassName) && !changeLogContext.getOldClassName(remappedClassName).equals(internalClassName)) {
				return null;
			} else {
				CountingSet<ClassNode> linkedGroupingClassCounts = this.restrictToLinkedGrouping(internalClassName, classReferenceCounts, changeLogContext);
				if (linkedGroupingClassCounts != null) {
					String packageName = AbstractClassNode.getPackageName(internalClassName);

					for (ClassNode candidateClass : linkedGroupingClassCounts.getSortedByCount(false)) {
						String candidateInternalName = candidateClass.getInternalName();
						if (candidateClass.getPackageName().equals(packageName)
							&& !changeLogContext.hasOldClass(candidateInternalName)
							&& !defaultExcludes.isClassNameExcluded(candidateInternalName)
							&& this.isFlowObfuscationEligible(candidateClass, flowObfuscateExclude, initOrderBuilder, workspace, false)) {
							FieldNode candidateField = workspace.getFieldByNameAndDescriptor(candidateInternalName, fieldName, fieldDescriptor);
							MethodNode candidateSetterMethod = entry.getOriginalSetterName() != null
								? workspace.getMethodBySignature(candidateClass, new MethodSignature(entry.getRemappedSetterName(), Condition.buildSetterMethodDescriptor(fieldDescriptor)))
								: null;
							MethodNode candidateGetterMethod = entry.getOriginalGetterName() != null
								? workspace.getMethodBySignature(candidateClass, new MethodSignature(entry.getRemappedGetterName(), Condition.buildGetterMethodDescriptor(fieldDescriptor)))
								: null;
							MethodNode candidateBooleanGetterMethod = entry.getOriginalBooleanGetterName() != null
								? workspace.getMethodBySignature(candidateClass, new MethodSignature(entry.getRemappedBooleanGetterName(), Condition.buildGetterMethodDescriptor(fieldDescriptor)))
								: null;
							if ((candidateField == null || !defaultExcludes.isFieldExcluded(candidateField))
								&& (candidateSetterMethod == null || !defaultExcludes.isMethodExcluded(candidateSetterMethod))
								&& (candidateGetterMethod == null || !defaultExcludes.isMethodExcluded(candidateGetterMethod))
								&& (candidateBooleanGetterMethod == null || !defaultExcludes.isMethodExcluded(candidateBooleanGetterMethod))
								&& !substituteClassByClassName.containsValue(candidateClass)) {
								substituteClassByClassName.put(internalClassName, candidateClass);
								return candidateClass;
							}
						}
					}
				}

				return null;
			}
		}
	}

	public void selectFlowObfuscationTargets(
		Set<InstructionListNode> dummyJumpTargets,
		Set<InstructionListNode> conditionalBlockTargets,
		MultiMap<ClassNode, PriorityInstructionEntry> flowMethodCandidates,
		FlowObfuscateExclude flowObfuscateExclude,
		StaticBlockAnalyzer staticBlockAnalyzer
	) {
		for (PackageGroupingManager grouping : this.packageGroupingManagers) {
			if (grouping.getGroupingCondition() != null) {
				Set<AbstractClassNode> groupedClasses = grouping.getGroupedClasses();
				ArrayList<PriorityInstructionEntry> priorityCandidates = new ArrayList<>(groupedClasses.size());

				for (AbstractClassNode groupedClass : groupedClasses) {
					if (groupedClass.isWorkspaceNode()) {
						List<PriorityInstructionEntry> classCandidates = flowMethodCandidates.get((ClassNode)groupedClass);
						if (classCandidates != null) {
							priorityCandidates.addAll(classCandidates);
						}
					}
				}

				Collections.sort(priorityCandidates);
				Collections.reverse(priorityCandidates);
				int packageCount = grouping.getPackageNames().size();
				HashMap<String, InstructionListNode> dummyJumpTargetsByPackage = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(packageCount));
				HashMap<String, InstructionListNode> conditionalBlockTargetsByPackage = GlobalUtils.newHashMap(dummyJumpTargetsByPackage.size());

				for (PriorityInstructionEntry candidate : priorityCandidates) {
					InstructionListNode instructionListNode = (InstructionListNode)candidate.instructionListNode;
					String packageName = instructionListNode.getPackageName();
					if (packageCount <= dummyJumpTargetsByPackage.size() && packageCount <= conditionalBlockTargetsByPackage.size() || candidate.priority < 10.0F) {
						break;
					}

					if ((flowObfuscateExclude == null || !flowObfuscateExclude.isMethodExcluded((MethodNode)instructionListNode.getMethodNode()))
						&& (staticBlockAnalyzer == null || !staticBlockAnalyzer.isCalledByInitializer((MethodNode)instructionListNode.getMethodNode()))
						&& !instructionListNode.getClassNode().hasInnerClassInfo()
						&& !instructionListNode.isClinit()
						&& !instructionListNode.isPrivateMethod()) {
						if (!dummyJumpTargetsByPackage.containsKey(packageName)) {
							dummyJumpTargetsByPackage.put(packageName, instructionListNode);
						} else if (!conditionalBlockTargetsByPackage.containsKey(packageName)) {
							conditionalBlockTargetsByPackage.put(packageName, instructionListNode);
						}
					}
				}

				Iterator<Entry<String, InstructionListNode>> packageTargetIterator = dummyJumpTargetsByPackage.entrySet().iterator();

				while (packageTargetIterator.hasNext()) {
					InstructionListNode selectedDummyJumpTarget = packageTargetIterator.next().getValue();
					dummyJumpTargets.add(selectedDummyJumpTarget);
				}

				packageTargetIterator = conditionalBlockTargetsByPackage.entrySet().iterator();

				while (packageTargetIterator.hasNext()) {
					InstructionListNode selectedConditionalBlockTarget = packageTargetIterator.next().getValue();
					conditionalBlockTargets.add(selectedConditionalBlockTarget);
				}
			}
		}

		for (InstructionListNode dummyJumpTarget : dummyJumpTargets) {
			this.dummyJumpClasses.add(dummyJumpTarget.getClassNode());
		}

		for (InstructionListNode conditionalBlockTarget : conditionalBlockTargets) {
			this.conditionalBlockClasses.add(conditionalBlockTarget.getClassNode());
		}
	}

	public Set<MethodNode> getGeneratedMethods() {
		return GlobalUtils.newHashSet(this.generatedMethods);
	}

	public Map<AbstractClassNode, PackageGroupingManager> getGroupingManagerByClass() {
		return this.groupingManagerByClass;
	}

	private void addFieldTypeToGrouping(PackageGroupingManager groupingManager, String fieldDescriptor) {
		this.fieldTypesByGrouping.get(groupingManager).add(fieldDescriptor);
	}

	public boolean useFieldAccessors() {
		return this.classMetrics.isVeryLargeApplication();
	}

	private boolean isObfuscatedFieldExcluded(
		FlowObfuscationEntry entry,
		AbstractClassNode classNode,
		DualKeyMap<String, KeyValuePair<String, String>, KeyValuePair<String, String>> fieldMappingsToRemove,
		Map<String, ExcludeElement> methodMappingsToRemove,
		DefaultExcludes defaultExcludes,
		AbstractWorkspace workspace,
		AtomicReference<String> errorMessage
	) {
		boolean incompatibleMappingFound = false;
		String originalFieldName = entry.getOriginalFieldName();
		String internalClassName = entry.getInternalClassName();
		String originalClassName = entry.getOriginalClassName();
		String humanReadableDescriptor = entry.getHumanReadableFieldType();
		String fieldDescriptor = entry.getOriginalFieldDescriptor();
		if (originalFieldName != null) {
			String remappedFieldName = entry.getRemappedFieldName();
			FieldNode field = null;
			if (classNode.isWorkspaceNode()) {
				field = workspace.getFieldByNameAndDescriptor(internalClassName, remappedFieldName, fieldDescriptor);
			}

			if (field != null && field.isWorkspaceNode() && defaultExcludes.isFieldExcluded(field)) {
				errorMessage.set(
					"Class '"
						+ originalClassName
						+ "' appears as a Flow Obfuscation "
						+ (entry.isTraceBackClassEntry() ? "TraceBackClass:" : "ForwardClass:")
						+ " but it is now incompatible. You must distribute this application as a whole. : "
						+ field.getDisplayName()
				);
				KeyValuePair<String, String> fieldKey = new KeyValuePair<>(originalFieldName, humanReadableDescriptor);
				fieldMappingsToRemove.put(internalClassName, fieldKey, fieldKey);
				incompatibleMappingFound = true;
			}

			if (entry.getOriginalSetterName() != null) {
				String remappedSetterName = entry.getRemappedSetterName();
				MethodNode setterMethod = null;
				if (classNode.isWorkspaceNode()) {
					setterMethod = workspace.getMethodBySignature((ClassNode)classNode, new MethodSignature(remappedSetterName, Condition.buildSetterMethodDescriptor(fieldDescriptor)));
				}

				if (setterMethod != null && setterMethod.isWorkspaceNode() && defaultExcludes.isMethodExcluded(setterMethod)) {
					errorMessage.set(
						"Class '"
							+ originalClassName
							+ "' appears as a Flow Obfuscation "
							+ (entry.isTraceBackClassEntry() ? "TraceBackClass:" : "ForwardClass:")
							+ " but it is now incompatible. You must distribute this application as a whole. : "
							+ setterMethod.getDisplayName()
							+ " (A)"
					);
					methodMappingsToRemove.put(internalClassName, new ExcludeElement(entry.getOriginalSetterName(), humanReadableDescriptor, "void"));
					incompatibleMappingFound = true;
				}
			}

			if (entry.getOriginalGetterName() != null) {
				String remappedGetterName = entry.getRemappedGetterName();
				MethodNode getterMethod = null;
				if (classNode.isWorkspaceNode()) {
					getterMethod = workspace.getMethodBySignature((ClassNode)classNode, new MethodSignature(remappedGetterName, Condition.buildGetterMethodDescriptor(fieldDescriptor)));
				}

				if (getterMethod != null && getterMethod.isWorkspaceNode() && defaultExcludes.isMethodExcluded(getterMethod)) {
					errorMessage.set(
						"Class '"
							+ originalClassName
							+ "' appears as a Flow Obfuscation "
							+ (entry.isTraceBackClassEntry() ? "TraceBackClass:" : "ForwardClass:")
							+ " but it is now incompatible. You must distribute this application as a whole. : "
							+ getterMethod.getDisplayName()
							+ " (B)"
					);
					methodMappingsToRemove.put(internalClassName, new ExcludeElement(entry.getOriginalGetterName(), "", humanReadableDescriptor));
					incompatibleMappingFound = true;
				}
			}

			if (entry.getOriginalBooleanGetterName() != null) {
				String remappedBooleanGetterName = entry.getRemappedBooleanGetterName();
				MethodNode booleanGetterMethod = null;
				if (classNode.isWorkspaceNode()) {
					booleanGetterMethod = workspace.getMethodBySignature((ClassNode)classNode, new MethodSignature(remappedBooleanGetterName, Condition.buildGetterMethodDescriptor(fieldDescriptor)));
				}

				if (booleanGetterMethod != null && booleanGetterMethod.isWorkspaceNode() && defaultExcludes.isMethodExcluded(booleanGetterMethod)) {
					errorMessage.set(
						"Class '"
							+ originalClassName
							+ "' appears as a Flow Obfuscation "
							+ (entry.isTraceBackClassEntry() ? "TraceBackClass:" : "ForwardClass:")
							+ " but it is now incompatible. You must distribute this application as a whole. : "
							+ booleanGetterMethod.getDisplayName()
							+ " (C)"
					);
					methodMappingsToRemove.put(internalClassName, new ExcludeElement(entry.getOriginalBooleanGetterName(), "", humanReadableDescriptor));
					incompatibleMappingFound = true;
				}
			}
		}

		return incompatibleMappingFound;
	}

	private boolean buildConditionsFromChangeLog(
		HashSet<String> packageNames,
		CountingSet<ClassNode> classReferenceCounts,
		DefaultExcludes defaultExcludes,
		FlowObfuscateExclude flowObfuscateExclude,
		ClassInitializationOrderBuilder initOrderBuilder,
		Random random,
		DualKeyMap<AbstractClassNode, String, Condition> conditionsByClassAndField,
		ReferenceGraph<AbstractClassNode, FieldSignature> fieldReferenceGraph,
		Inheritance inheritance,
		AbstractWorkspace workspace
	) {
		HashMap<FlowObfuscationEntry, Condition> conditionsByEntry = GlobalUtils.newHashMap();
		HashMap<String, ClassNode> classNodesByOriginalName = GlobalUtils.newHashMap();
		HashMap<String, String> classRenames = GlobalUtils.newHashMap(7);
		HashMap<ExcludeElement, String> fieldRenames = GlobalUtils.newHashMap(7);
		DualKeyMap<String, KeyValuePair<String, String>, KeyValuePair<String, String>> removedFields = new DualKeyMap<>(7, 7);
		HashMap<ExcludeElement, String> addedFields = GlobalUtils.newHashMap(7);
		DualKeyMap<String, ExcludeElement, String> methodRenames = new DualKeyMap<>();
		HashMap<String, ExcludeElement> removedMethods = GlobalUtils.newHashMap();
		DualKeyMap<String, ExcludeElement, String> addedMethods = new DualKeyMap<>();
		boolean allEntriesResolved = true;
		ReferenceGraph<String, AbstractClassNode> classReferenceGraph = new ReferenceGraph<>();
		Map<String, KeyValuePair<FlowObfuscationEntry, FlowObfuscationEntry>> memberClassEntries = this.changeLogContext.getMemberClassEntries();

		for (Entry<String, KeyValuePair<FlowObfuscationEntry, FlowObfuscationEntry>> memberClassEntry : memberClassEntries.entrySet()) {
			AbstractClassNode referencedMemberClassNode = ClassHierarchyNode.getClassNodeByInternalName(GlobalUtils.binaryToInternalName(memberClassEntry.getKey()));
			if (referencedMemberClassNode != null && (!referencedMemberClassNode.isInterface() || referencedMemberClassNode.isJava8OrNewer())) {
				KeyValuePair<FlowObfuscationEntry, FlowObfuscationEntry> memberClassPair = memberClassEntry.getValue();
				FlowObfuscationEntry firstEntry = memberClassPair.getKey();
				FlowObfuscationEntry secondEntry = memberClassPair.getValue();
				classReferenceGraph.addReference(firstEntry.getInternalClassName(), referencedMemberClassNode);
				classReferenceGraph.addReference(secondEntry.getInternalClassName(), referencedMemberClassNode);
			}
		}

		Enumeration<FlowObfuscationEntry> flowObfuscationEntries = this.changeLogContext.getFlowObfuscationEntries();

		while (flowObfuscationEntries.hasMoreElements() && allEntriesResolved) {
			FlowObfuscationEntry flowObfuscationEntry = flowObfuscationEntries.nextElement();
			Condition traceBackCondition = this.processFlowObfuscationEntry(
				flowObfuscationEntry, classReferenceGraph, classReferenceCounts, defaultExcludes, random, conditionsByClassAndField, classRenames, fieldRenames, removedFields, addedFields, methodRenames, removedMethods, addedMethods, true, packageNames.size(), workspace, classNodesByOriginalName, flowObfuscateExclude, initOrderBuilder, inheritance
			);
			if (traceBackCondition != null) {
				AbstractClassNode traceBackOwnerClass = traceBackCondition.getOwnerClassNode();
				this.addFieldTypeToGrouping(this.groupingManagerByClass.get(traceBackOwnerClass), traceBackCondition.getFieldDescriptor());
				this.conditions.add(traceBackCondition);
				fieldReferenceGraph.addReference(traceBackOwnerClass, traceBackCondition.getFieldSignature());
				conditionsByEntry.put(flowObfuscationEntry, traceBackCondition);
			} else {
				allEntriesResolved = false;
			}
		}

		MultiMap<String, FlowObfuscationEntry> flowObfuscationReferences = this.changeLogContext.getFlowObfuscationReferences();
		Enumeration<String> classNames = flowObfuscationReferences.keys();

		while (classNames.hasMoreElements() && allEntriesResolved) {
			String binaryClassName = classNames.nextElement();
			String internalClassName = GlobalUtils.binaryToInternalName(binaryClassName);
			if (packageNames.contains(internalClassName)) {
				for (FlowObfuscationEntry referencingEntry : flowObfuscationReferences.get(binaryClassName)) {
					Condition forwardCondition = this.processFlowObfuscationEntry(
						referencingEntry, classReferenceGraph, classReferenceCounts, defaultExcludes, random, conditionsByClassAndField, classRenames, fieldRenames, removedFields, addedFields, methodRenames, removedMethods, addedMethods, false, packageNames.size(), workspace, classNodesByOriginalName, flowObfuscateExclude, initOrderBuilder, inheritance
					);
					if (forwardCondition != null) {
						this.conditionsByPackage.put(internalClassName, forwardCondition);
						conditionsByEntry.put(referencingEntry, forwardCondition);
						AbstractClassNode forwardOwnerClass = ClassHierarchyNode.getClassNodeByInternalName(forwardCondition.getOwnerInternalName());
						fieldReferenceGraph.addReference(forwardOwnerClass, forwardCondition.getFieldSignature());
						this.addFieldTypeToGrouping(this.groupingManagerByClass.get(forwardOwnerClass), forwardCondition.getFieldDescriptor());
					} else {
						allEntriesResolved = false;
					}
				}
			}
		}

		if (allEntriesResolved) {
			for (Entry<String, KeyValuePair<FlowObfuscationEntry, FlowObfuscationEntry>> groupingMemberClassEntry : memberClassEntries.entrySet()) {
				String memberClassBinaryName = groupingMemberClassEntry.getKey();
				AbstractClassNode memberClassNode = ClassHierarchyNode.getClassNodeByInternalName(GlobalUtils.binaryToInternalName(memberClassBinaryName));
				if (memberClassNode != null) {
					if (memberClassNode.isInterface() && !memberClassNode.isJava8OrNewer()) {
						this.changeLogContext.logMessage("Class '" + memberClassBinaryName + "' has been changed to be an interface. No action required.");
					} else {
						KeyValuePair<FlowObfuscationEntry, FlowObfuscationEntry> groupingEntryPair = groupingMemberClassEntry.getValue();
						FlowObfuscationEntry groupingConditionEntry = groupingEntryPair.getKey();
						FlowObfuscationEntry packageConditionEntry = groupingEntryPair.getValue();
						Condition groupingCondition = conditionsByEntry.get(groupingConditionEntry);
						Condition packageCondition = conditionsByEntry.get(packageConditionEntry);
						if (groupingCondition == null || packageCondition == null) {
							allEntriesResolved = false;
							this.changeLogContext.logError("Class '" + memberClassBinaryName + "' affected by a change in class grouping (B). You must distribute this application as a whole.");
						} else if (this.isGroupedWithConditionClass(memberClassNode, groupingCondition) && this.isGroupedWithConditionClass(memberClassNode, packageCondition)) {
							this.packageConditionByClass.put(memberClassNode, new PackageCondition(groupingCondition, packageCondition, memberClassNode.getPackageName()));
						} else {
							allEntriesResolved = false;
							this.changeLogContext.logError("Class '" + memberClassBinaryName + "' affected by a change in class grouping (A). You must distribute this application as a whole.");
						}
					}
				}
			}

			if (!allEntriesResolved) {
				this.packageConditionByClass.clear();
			}
		}

		for (String oldClassName : classRenames.keySet()) {
			String newClassName = classRenames.get(oldClassName);
			this.changeLogContext.updateClassMapping(oldClassName, newClassName);
		}

		for (ExcludeElement fieldRenameKey : fieldRenames.keySet()) {
			String renamedFieldName = fieldRenames.get(fieldRenameKey);
			this.changeLogContext.updateRawFieldMapping((String)fieldRenameKey.getLinkedClass(), (String)fieldRenameKey.getPrefix(), (String)fieldRenameKey.getSuffix(), renamedFieldName, 0);
		}

		Enumeration<String> removedFieldClassNames = removedFields.getGroupKeys();

		while (removedFieldClassNames.hasMoreElements()) {
			String removedFieldClassName = removedFieldClassNames.nextElement();

			for (KeyValuePair<String, String> removedFieldKey : removedFields.getGroup(removedFieldClassName).keySet()) {
				String fieldName = removedFieldKey.getKey();
				String fieldDescriptor = removedFieldKey.getValue();
				this.changeLogContext.removeRawFieldMapping(removedFieldClassName, fieldName, fieldDescriptor);
			}
		}

		for (ExcludeElement addedFieldKey : addedFields.keySet()) {
			String addedFieldName = addedFields.get(addedFieldKey);
			this.changeLogContext.addRawFieldMapping((String)addedFieldKey.getLinkedClass(), (String)addedFieldKey.getPrefix(), (String)addedFieldKey.getSuffix(), addedFieldName, 0);
		}

		for (Entry<String, Map<ExcludeElement, String>> methodRenameEntry : methodRenames.getEntrySet()) {
			for (Entry<ExcludeElement, String> methodRenamePair : methodRenameEntry.getValue().entrySet()) {
				ExcludeElement oldMethodKey = methodRenamePair.getKey();
				this.changeLogContext.updateRawMethodMapping(methodRenameEntry.getKey(), (String)oldMethodKey.getLinkedClass(), (String)oldMethodKey.getPrefix(), (String)oldMethodKey.getSuffix(), methodRenamePair.getValue(), 0);
			}
		}

		for (Entry<String, ExcludeElement> removedMethodEntry : removedMethods.entrySet()) {
			ExcludeElement removedMethodKey = removedMethodEntry.getValue();
			this.changeLogContext.removeRawMethodMapping(removedMethodEntry.getKey(), (String)removedMethodKey.getLinkedClass(), (String)removedMethodKey.getPrefix(), (String)removedMethodKey.getSuffix());
		}

		for (Entry<String, Map<ExcludeElement, String>> addedMethodEntry : addedMethods.getEntrySet()) {
			String addedMethodClassName = addedMethodEntry.getKey();

			for (Entry<ExcludeElement, String> addedMethodPair : addedMethodEntry.getValue().entrySet()) {
				ExcludeElement addedMethodKey = addedMethodPair.getKey();
				this.changeLogContext.addRawMethodMapping(addedMethodClassName, (String)addedMethodKey.getLinkedClass(), (String)addedMethodKey.getPrefix(), (String)addedMethodKey.getSuffix(), addedMethodPair.getValue(), 0);
			}
		}

		return allEntriesResolved;
	}

	private LibraryMethodNode createLibraryGetterMethod(LibraryClassNode libraryClassNode, String methodName, AbstractFieldNode fieldNode, List<TypeWriter> typeWriters, Inheritance inheritance) {
		String descriptor = Condition.buildGetterMethodDescriptor(fieldNode.getDescriptor());
		LocalVariableTable localVariableTable = new LocalVariableTable(true, descriptor, 5);
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		this.addFieldGetterInstructions(instructions, localVariableTable, libraryClassNode, fieldNode, typeWriters);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		return libraryClassNode.createPublicStaticMethod(methodName, descriptor, instructions, 1, 0, localVariableTable, tryCatchBlocks, typeWriters, inheritance, "Flow Obfuscation", 2);
	}

	private LibraryMethodNode createLibrarySetterMethod(LibraryClassNode libraryClassNode, String methodName, LibraryFieldNode libraryFieldNode, List<TypeWriter> typeWriters, Inheritance inheritance) {
		String descriptor = Condition.buildSetterMethodDescriptor(libraryFieldNode.getDescriptor());
		LocalVariableTable localVariableTable = new LocalVariableTable(true, descriptor, 5);
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		this.addFieldSetterInstructions(instructions, localVariableTable, libraryClassNode, libraryFieldNode, typeWriters);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		return libraryClassNode.createPublicStaticMethod(methodName, descriptor, instructions, 1, 1, localVariableTable, tryCatchBlocks, typeWriters, inheritance, "Flow Obfuscation", 2);
	}

	public List<String> getUnobfuscatedClasses() {
		HashSet<String> remainingGroupNames = GlobalUtils.newHashSet();
		HashSet<String> conditionalGroupNames = GlobalUtils.newHashSet();

		for (PackageGroupingManager grouping : this.packageGroupingManagers) {
			List<String> groupNames = grouping.getGroupNames();
			remainingGroupNames.addAll(groupNames);
			if (grouping.getGroupingCondition() != null) {
				conditionalGroupNames.addAll(groupNames);
			}
		}

		remainingGroupNames.removeAll(conditionalGroupNames);
		ArrayList<String> sortedGroupNames = new ArrayList<>(remainingGroupNames);
		Collections.sort(sortedGroupNames);
		return sortedGroupNames;
	}

	public Set<AbstractClassNode> getPackageConditionClasses() {
		HashSet<AbstractClassNode> conditionOwnerClasses = GlobalUtils.newHashSet();
		Iterator<Entry<AbstractClassNode, PackageCondition>> packageConditionIterator = this.packageConditionByClass.entrySet().iterator();

		while (packageConditionIterator.hasNext()) {
			PackageCondition packageCondition = packageConditionIterator.next().getValue();
			conditionOwnerClasses.add(packageCondition.getGroupCondition().getOwnerClassNode());
			conditionOwnerClasses.add(packageCondition.getPackageCondition().getOwnerClassNode());
		}

		return conditionOwnerClasses;
	}

	public StaticBlockAnalyzer getStaticBlockAnalyzer() {
		return this.staticBlockAnalyzer;
	}

	public PackageCondition getPackageCondition(AbstractClassNode classNode) {
		return this.packageConditionByClass.get(classNode);
	}

	private MethodNode createStaticSetterMethod(ClassNode classNode, String methodName, FieldNode fieldNode, List<TypeWriter> typeWriters, Inheritance inheritance, AbstractWorkspace workspace) {
		String descriptor = Condition.buildSetterMethodDescriptor(fieldNode.getDescriptor());
		LocalVariableTable localVariableTable = new LocalVariableTable(true, descriptor, 5);
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		this.addFieldSetterInstructions(instructions, localVariableTable, classNode, fieldNode, typeWriters);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		MethodNode staticMethod = classNode.createPublicStaticMethod(methodName, descriptor, instructions, 1, 1, localVariableTable, tryCatchBlocks, "Flow Obfuscation", typeWriters, inheritance, workspace, 2);
		this.generatedMethods.add(staticMethod);
		return staticMethod;
	}

	private MethodNode createStaticGetterMethod(ClassNode classNode, FieldNode fieldNode, List<TypeWriter> typeWriters, Inheritance inheritance, AbstractWorkspace workspace) {
		String descriptor = Condition.buildGetterMethodDescriptor(fieldNode.getDescriptor());
		LocalVariableTable localVariableTable = new LocalVariableTable(true, descriptor, 5);
		ArrayList<AbstractInsnNode> instructions = new ArrayList<>();
		this.addFieldGetterInstructions(instructions, localVariableTable, classNode, fieldNode, typeWriters);
		TryCatchBlockInfo[] tryCatchBlocks = new TryCatchBlockInfo[0];
		MethodNode staticMethod = classNode.createPublicStaticMethod(descriptor, instructions, 1, 0, localVariableTable, tryCatchBlocks, "Flow Obfuscation", typeWriters, inheritance, workspace, 2);
		this.generatedMethods.add(staticMethod);
		return staticMethod;
	}
}
