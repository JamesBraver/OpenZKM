package com.zelix.tools;

import com.zelix.enums.MixCaseType;
import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassRepository;
import com.zelix.tools.renamer.naming.NameGenerator;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.EnvironmentUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.StringUtils;
import com.zelix.utils.collections.CountingSet;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.collections.UnmodifiableMultiMap;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.dataset.Triplet;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.Workspace;

import java.util.*;
import java.util.Map.Entry;

public class InjectedClassManager {
	public static String generateAuxiliaryClassName(
		String existingDataClassName,
		String preferredPackageName,
		Collection<String> relatedPackageNames,
		Collection<ClassNode> targetClasses,
		ReadOnlyMap<String, String> packageRenameMap,
		ReadOnlyMap<String, String> classNameToInternalNameMap,
		ReadOnlyMap<String, String> internalToBinaryNameMap,
		Set<String> reservedClassNames,
		String namePrefix,
		MixCaseType mixedCaseType,
		boolean avoidNameClash,
		ClassRepository classRepository,
		List<String> reservedNames,
		boolean randomizeObfuscation,
		ScriptContext scriptContext
	) {
		HashSet<String> targetPackageNames = GlobalUtils.newHashSet();

		for (ClassNode targetClass : targetClasses) {
			targetPackageNames.add(targetClass.getPackageName());
		}

		if (existingDataClassName != null && targetPackageNames.contains(AbstractClassNode.getPackageName(existingDataClassName))) {
			return existingDataClassName;
		} else {
			String selectedPackageName = null;
			if (preferredPackageName != null && targetPackageNames.contains(preferredPackageName)) {
				selectedPackageName = preferredPackageName;
			}

			int targetClassCount = targetClasses.size();
			if (selectedPackageName == null) {
				HashSet<String> mappedRelatedPackageNames = null;
				if (relatedPackageNames != null) {
					mappedRelatedPackageNames = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(relatedPackageNames.size()));
					Iterator<String> relatedPackageNameIterator = relatedPackageNames.iterator();

					while (relatedPackageNameIterator.hasNext()) {
						String mappedPackageName = GlobalUtils.getMappedOrDefault(relatedPackageNameIterator.next(), packageRenameMap);
						mappedRelatedPackageNames.add(mappedPackageName);
					}
				}

				CountingSet<ClassNode> referencedClassCounts = new CountingSet<>(targetClassCount);

				for (ClassNode referencingClass : targetClasses) {
					if (mappedRelatedPackageNames == null || mappedRelatedPackageNames.contains(referencingClass.getPackageName())) {
						referencingClass.collectReferencedClasses(referencedClassCounts);
						referencedClassCounts.add(referencingClass);
					}
				}

				Iterator<ClassNode> referencedClassIterator = referencedClassCounts.iterator();

				while (referencedClassIterator.hasNext()) {
					ClassNode nextReferencedClass = referencedClassIterator.next();
					if (!targetClasses.contains(nextReferencedClass)) {
						referencedClassIterator.remove();
					}
				}

				CountingSet<String> packageReferenceCounts = new CountingSet<>();
				CountingSet<String> publicPackageCounts = new CountingSet<>();

				for (ClassNode referencedClass : referencedClassCounts) {
					String packageName = referencedClass.getPackageName();
					packageReferenceCounts.add(packageName);
					packageReferenceCounts.setCount(packageName, packageReferenceCounts.getCount(packageName) + referencedClassCounts.getCount(referencedClass) - 1);
					if (referencedClass.isPublic()) {
						publicPackageCounts.add(packageName);
						publicPackageCounts.setCount(packageName, publicPackageCounts.getCount(packageName) + referencedClassCounts.getCount(referencedClass) - 1);
					}
				}

				List<String> sortedPackageNames = publicPackageCounts.getSortedByCount(false);
				if (sortedPackageNames.size() > 0) {
					selectedPackageName = sortedPackageNames.get(0);
				} else {
					sortedPackageNames = packageReferenceCounts.getSortedByCount(false);
					if (sortedPackageNames.size() > 0) {
						selectedPackageName = sortedPackageNames.get(0);
					}
				}

				if (selectedPackageName == null) {
					selectedPackageName = "";
				}
			}

			DualKeyMap<String, String, ClassNode> classesByPackageAndSimpleName = new DualKeyMap<>();
			HashSet<String> existingLowerCaseNameKeys = GlobalUtils.newHashSet();
			HashSet<String> existingSimpleNames = GlobalUtils.newHashSet();

			for (ClassNode existingClass : targetClasses) {
				classesByPackageAndSimpleName.put(existingClass.getPackageName(), existingClass.getSimpleNameInternal(), existingClass);
				String lowerCaseNameKey = existingClass.getLowerCaseNameKey();
				existingLowerCaseNameKeys.add(lowerCaseNameKey);
				existingSimpleNames.add(AbstractClassNode.getSimpleNameFromInternalName(lowerCaseNameKey));
			}

			Map<String, ClassNode> classesInSelectedPackage = classesByPackageAndSimpleName.getGroup(selectedPackageName);
			NameGenerator nameGenerator = new NameGenerator(NameGenerator.LOWER_LETTERS_, NameGenerator.LOWER_LETTERS_NUMBERS_, reservedNames, randomizeObfuscation);
			String acceptedClassName = null;

			do {
				String generatedSimpleName = nameGenerator.next(namePrefix);
				String candidateInternalName = (selectedPackageName.length() > 0 ? selectedPackageName + '/' : "") + generatedSimpleName;
				if ((!avoidNameClash || !existingSimpleNames.contains(generatedSimpleName))
					&& !reservedClassNames.contains(candidateInternalName)
					&& (classNameToInternalNameMap == null || !classNameToInternalNameMap.containsKey(candidateInternalName))
					&& (internalToBinaryNameMap == null || !internalToBinaryNameMap.containsKey(candidateInternalName))
					&& (classesInSelectedPackage == null || !classesInSelectedPackage.containsKey(generatedSimpleName))
					&& !existingLowerCaseNameKeys.contains(candidateInternalName.toLowerCase())
					&& ClassHierarchyNode.getClassNodeByInternalName(candidateInternalName) == null
					&& classRepository.findClass(candidateInternalName) == null) {
					acceptedClassName = candidateInternalName;
				}
			} while (acceptedClassName == null);

			reservedClassNames.add(acceptedClassName);
			return acceptedClassName;
		}
	}

	public static String logCreatedAuxiliaryClasses(String optionName, Set<ClassNode> createdClasses, MultiMap<ClassNode, ArchiveNode> archivesByClass) {
		String headerLine = "\t" + optionName + " : Created special lookup class" + (createdClasses.size() == 1 ? "" : "es") + " as ";
		String continuationLine = StringUtils.justificateString("\t", 76, headerLine.length(), 32);
		StringBuilder textBuffer = new StringBuilder();

		for (ClassNode createdClass : createdClasses) {
			if (textBuffer.length() == 0) {
				textBuffer.append(headerLine);
			} else {
				textBuffer.append(continuationLine);
			}

			textBuffer.append('\'');
			textBuffer.append(createdClass.getBinaryName());
			textBuffer.append('\'');
			List<ArchiveNode> archives = archivesByClass.get(createdClass);
			if (archives != null) {
				for (int archiveIndex = 0; archiveIndex < archives.size(); archiveIndex++) {
					ArchiveNode archiveNode = archives.get(archiveIndex);
					if (archiveIndex == 0) {
						textBuffer.append(" in module" + (archives.size() > 1 ? "s '" : " '"));
					}

					textBuffer.append(archiveNode.getModuleName());
					if (archiveIndex < archives.size() - 1) {
						textBuffer.append("', ");
					} else {
						textBuffer.append('\'');
					}
				}

				textBuffer.append(GlobalUtils.LINE_SEPARATOR);
			} else {
				textBuffer.append(GlobalUtils.LINE_SEPARATOR);
			}
		}

		return textBuffer.toString();
	}

	public static void calculateInjectionVersion(
		AtomicInt injectClassMajorVersion, AtomicInt injectClassMinorVersion, AbstractClassNode[] classNodes, Integer assumeRuntimeVersion
	) {
		int minMajorVersion = Integer.MAX_VALUE;
		int minMinorVersion = Integer.MAX_VALUE;

		for (AbstractClassNode classNode : classNodes) {
			int majorVersion = classNode.getMajorVersion();
			if (majorVersion < minMajorVersion) {
				minMajorVersion = majorVersion;
				minMinorVersion = classNode.getMinorVersion();
			} else if (majorVersion == minMajorVersion && classNode.getMinorVersion() < minMinorVersion) {
				minMinorVersion = classNode.getMinorVersion();
			}
		}

		if (assumeRuntimeVersion != null) {
			int runtimeMajorVersion = EnvironmentUtils.getClassVersionForRuntime(assumeRuntimeVersion);
			if (runtimeMajorVersion > minMajorVersion) {
				minMajorVersion = runtimeMajorVersion;
				minMinorVersion = 0;
			}
		}

		injectClassMajorVersion.set(minMajorVersion);
		injectClassMinorVersion.set(minMinorVersion);
	}

	public static UnmodifiableMultiMap<ArchiveNode, ClassNode> filterNamedModules(
		UnmodifiableMultiMap<ArchiveNode, ClassNode> classesByModule, ReferenceGraph<ArchiveNode, ArchiveNode> moduleReferenceGraph, ReferenceGraph<ArchiveNode, String> modulePackageGraph
	) {
		MultiMap<ArchiveNode, ClassNode> namedModuleClasses = new MultiMap<>(classesByModule.size());
		ReferenceGraph<ArchiveNode, String> modulePackageReferences = new ReferenceGraph<>(classesByModule.size());

		for (Entry<ArchiveNode, List<ClassNode>> entry : classesByModule.entrySet()) {
			ArchiveNode moduleNode = entry.getKey();
			if (moduleNode.hasModuleName()) {
				List<ClassNode> classes = entry.getValue();
				namedModuleClasses.putAll(moduleNode, classes);

				for (ClassNode classNode : classes) {
					modulePackageReferences.addReference(moduleNode, classNode.getPackageName());
				}
			}
		}

		computeModuleDependencies(modulePackageGraph, moduleReferenceGraph, modulePackageReferences);
		return new UnmodifiableMultiMap<>(namedModuleClasses);
	}

	public static Set<ClassNode> collectEligibleClasses(Set<ClassNode> candidateClasses, Set<ClassNode> nonModuleSourceClasses, Set<ClassNode> classesNotInModules, Set<String> packageNames) {
		HashSet<ClassNode> eligibleClasses = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(candidateClasses.size()));

		for (ClassNode candidateClass : candidateClasses) {
			if (!candidateClass.isGeneratedClass()) {
				ArrayList<ClassNode> selfAndInnerClasses = new ArrayList<>();
				if (!candidateClass.hasInnerClassInfo()) {
					eligibleClasses.add(candidateClass);
				} else {
					for (AbstractClassNode selfOrInnerClass : candidateClass.getSelfAndAllInnerClasses()) {
						selfAndInnerClasses.add((ClassNode)selfOrInnerClass);
					}

					eligibleClasses.addAll(selfAndInnerClasses);
				}

				boolean fromNamedModule = false;
				boolean fromNonModuleSource = false;
				Enumeration<ClasspathResource> classSources = candidateClass.getClassSources();

				while (classSources.hasMoreElements()) {
					ClasspathResource classpathResource = classSources.nextElement();
					if (classpathResource.isZipEntry()) {
						if (classpathResource.getArchiveNode().hasModuleName()) {
							fromNamedModule = true;
						} else {
							fromNonModuleSource = true;
						}
					} else {
						fromNonModuleSource = true;
					}
				}

				if (fromNonModuleSource) {
					if (candidateClass.hasInnerClassInfo()) {
						nonModuleSourceClasses.addAll(selfAndInnerClasses);
						if (!fromNamedModule) {
							classesNotInModules.addAll(selfAndInnerClasses);
						}
					} else {
						nonModuleSourceClasses.add(candidateClass);
						if (!fromNamedModule) {
							classesNotInModules.add(candidateClass);
						}
					}
				}
			}
		}

		for (ClassNode nonModuleSourceClass : nonModuleSourceClasses) {
			packageNames.add(nonModuleSourceClass.getPackageName());
		}

		return eligibleClasses;
	}

	private static void ensureCommonPackages(ArchiveNode archiveNode, ReferenceGraph<ArchiveNode, ArchiveNode> moduleReferenceGraph, ReferenceGraph<ArchiveNode, String> modulePackageGraph) {
		if (modulePackageGraph.getReferences(archiveNode).isEmpty()) {
			StringBuilder messageBuffer = new StringBuilder();
			messageBuffer.append("There are modules that have packages in common but there is no one package that is common to them all : '");

			for (ArchiveNode referencedModule : moduleReferenceGraph.getReferences(archiveNode)) {
				messageBuffer.append(referencedModule.getModuleName());
				messageBuffer.append(", ");
			}

			messageBuffer.append(archiveNode.getModuleName());
			messageBuffer.append('\'');
			throw new ProcessingException(messageBuffer.toString());
		}
	}

	public static ClassNode[] generateAndRegisterClasses(
		Set<String> dataClasses,
		Map<ClassNode, String> classToDataClassNameMap,
		AtomicReference<String> dataClassName,
		AtomicReference<ClassNode> dataClassNode,
		Map<ArchiveNode, ClassNode> archiveToDataClassNodeMap,
		Map<ClassNode, ClassNode> classToDataClassNodeMap,
		Set<ClassNode> generatedDataClasses,
		Map<ArchiveNode, String> archiveToDataClassNameMap,
		int majorVersion,
		int minorVersion,
		AuxiliaryClassGenerator classGenerator,
		Workspace workspace,
		AuxiliaryWiringStrategy auxiliaryWiringStrategy
	) {

//      if (classGenerator.getClass().getSimpleName().equals("ObfuscateReferencesBuilder")) {
//         System.out.println("Generating data classes..., call by " + classGenerator.getClass().getSimpleName());
//         System.out.println("Need to generate " + dataClasses.size() + " data classes:");
//         System.out.println(dataClasses);
//         System.out.println("var1.size=" + classToDataClassNameMap.size() + ", var7.size=" + archiveToDataClassNameMap.size());
//         System.out.println("var1=" + classToDataClassNameMap);
//         System.out.println("var7=" + archiveToDataClassNameMap);
//         System.out.println("=====");
//      }

		ClassNode[] newAllClassesArray = null;
		HashMap<String, ClassNode> generatedClassesByClassName = GlobalUtils.newHashMap();

		for (String className : dataClasses) {
			ClassNode generatedDataClass = classGenerator.generate(className, majorVersion, minorVersion, auxiliaryWiringStrategy);
			newAllClassesArray = workspace.addClassToWorkspace(generatedDataClass);
			generatedClassesByClassName.put(className, generatedDataClass);
			generatedDataClasses.add(generatedDataClass);
			if (generatedDataClass.isJava6OrNewer()) {
				generatedDataClass.markAllMethodsModified();
			}
		}

		if (auxiliaryWiringStrategy == null || auxiliaryWiringStrategy.shouldAutoRegister()) {
			for (Entry<ClassNode, String> entry : classToDataClassNameMap.entrySet()) {
				ClassNode sourceClass = entry.getKey();
				String classDataClassName = entry.getValue();
				ClassNode sourceDataClass = generatedClassesByClassName.get(classDataClassName);
				classToDataClassNodeMap.put(sourceClass, sourceDataClass);
			}

			for (Entry<ArchiveNode, String> archiveEntry : archiveToDataClassNameMap.entrySet()) {
				ArchiveNode archiveNode = archiveEntry.getKey();
				String archiveDataClassName = archiveEntry.getValue();
				ClassNode archiveDataClass = generatedClassesByClassName.get(archiveDataClassName);
				archiveToDataClassNodeMap.put(archiveNode, archiveDataClass);
			}

			if (!dataClassName.isNull()) {
				ClassNode injectedDataClass = generatedClassesByClassName.get(dataClassName.get());
				dataClassNode.set(injectedDataClass);
			}
		}

		return newAllClassesArray;
	}

	public static String logCreatedClassesFromMap(String optionName, Set<ClassNode> createdClasses, Map<ArchiveNode, ClassNode> dataClassByArchive) {
		MultiMap<ClassNode, ArchiveNode> archivesByClass = new MultiMap<>();

		for (Entry<ArchiveNode, ClassNode> entry : dataClassByArchive.entrySet()) {
			archivesByClass.put(entry.getValue(), entry.getKey());
		}

		return logCreatedAuxiliaryClasses(optionName, createdClasses, archivesByClass);
	}

	public static String validateUserConfiguredName(
		String changelogClauseName, String configuredClassName, String packageParameterName, String configuredPackageName, String featureName, Set<String> packageNames, ChangeLogContext changeLogContext, ReadOnlyMap<String, String> internalToBinaryNameMap
	) {
		String internalClassName = GlobalUtils.binaryToInternalName(configuredClassName);
		if (internalToBinaryNameMap.containsKey(internalClassName)) {
			changeLogContext.logError(
				"The class '"
					+ configuredClassName
					+ "' appears in the change log as a "
					+ featureName
					+ " lookup class but that name is already used. Another name will be used. You must distribute this application as a whole. (1)"
			);
			return null;
		} else if (ClassHierarchyNode.getNodeByInternalName(internalClassName) != null) {
			ClassNode existingClassNode = ClassHierarchyNode.getWorkspaceClassNode(internalClassName);
			changeLogContext.logError(
				"The class '"
					+ configuredClassName
					+ "' appears in the change log as a "
					+ featureName
					+ " lookup class but that name is already used. Another name will be used. You must distribute this application as a whole."
					+ (existingClassNode == null ? "" : " : " + existingClassNode.getClassType())
					+ " (2)"
			);
			return null;
		} else {
			String internalPackageName = AbstractClassNode.getPackageName(internalClassName);
			if (configuredPackageName != null) {
				String configuredPackageInternalName = GlobalUtils.binaryToInternalName(configuredPackageName);
				String newPackageName = changeLogContext.getNewPackageName(configuredPackageInternalName);
				if (newPackageName == null && changeLogContext.hasOldPackageName(configuredPackageInternalName)) {
					newPackageName = configuredPackageInternalName;
				}

				if (newPackageName == null || !newPackageName.equals(internalPackageName)) {
					String warningMessage = "'"
						+ packageParameterName
						+ "' parameter in 'obfuscate' statement specifies package name '"
						+ configuredPackageName
						+ "' but input change log '"
						+ changeLogContext.getLogFileName()
						+ "' contains a '"
						+ changelogClauseName
						+ "' clause specifying package name '"
						+ GlobalUtils.internalToBinaryName(internalPackageName)
						+ "' which will be used.";
					changeLogContext.logWarning(warningMessage);
				}
			}

			if (!packageNames.contains(internalPackageName)) {
				String packageNamesText = GlobalUtils.toQuotedString(packageNames);
				changeLogContext.logError(
					"The class '"
						+ configuredClassName
						+ "' appears in the change log '"
						+ changelogClauseName
						+ "' clause as a "
						+ featureName
						+ " lookup class but it is not in a suitable package. Another name will be used. You must distribute this application as a whole. : {"
						+ packageNamesText
						+ "}"
				);
				return null;
			} else {
				return internalClassName;
			}
		}
	}

	private static void computeModuleDependencies(
		ReferenceGraph<ArchiveNode, String> modulePackageGraph, ReferenceGraph<ArchiveNode, ArchiveNode> moduleReferenceGraph, ReferenceGraph<ArchiveNode, String> modulePackageReferences
	) {
		ReferenceGraph<String, ArchiveNode> archivesByPackage = new ReferenceGraph<>();

		for (Entry<ArchiveNode, Set<String>> entry : modulePackageReferences.getAdjacencyEntries()) {
			ArchiveNode moduleNode = entry.getKey();
			Set<String> packageNames = entry.getValue();
			modulePackageGraph.addReferences(moduleNode, packageNames);

			for (String packageName : packageNames) {
				archivesByPackage.addReference(packageName, moduleNode);
			}
		}

		Iterator<Entry<String, Set<ArchiveNode>>> packageEntryIterator = archivesByPackage.getAdjacencyEntries().iterator();

		while (packageEntryIterator.hasNext()) {
			Set<ArchiveNode> modulesSharingPackage = packageEntryIterator.next().getValue();
			if (modulesSharingPackage.size() > 1) {
				Iterator<ArchiveNode> moduleIterator = modulesSharingPackage.iterator();
				HashSet<String> commonPackageNames = GlobalUtils.newHashSet(modulePackageGraph.getReferences(moduleIterator.next()));

				while (moduleIterator.hasNext()) {
					ArchiveNode otherModule = moduleIterator.next();
					Set<String> referencedPackageNames = modulePackageGraph.getReferences(otherModule);
					commonPackageNames.retainAll(referencedPackageNames);
				}

				for (ArchiveNode restrictedModule : modulesSharingPackage) {
					modulePackageGraph.setReferences(restrictedModule, GlobalUtils.newHashSet(commonPackageNames));
				}

				for (ArchiveNode sourceModule : modulesSharingPackage) {
					for (ArchiveNode targetModule : modulesSharingPackage) {
						if (sourceModule != targetModule) {
							moduleReferenceGraph.addReference(sourceModule, targetModule);
						}
					}
				}
			}
		}
	}

	public static String logCreatedTripletClasses(String optionName, MultiMap<Triplet, ArchiveNode> archivesByTriplet, Set<Triplet> createdTriplets) {
		String headerLine = "\t" + optionName + " : Created special lookup classes as ";
		String continuationLine = StringUtils.justificateString("\t", 76, headerLine.length(), 32);
		StringBuilder textBuffer = new StringBuilder();

		for (Triplet triplet : createdTriplets) {
			if (textBuffer.length() == 0) {
				textBuffer.append(headerLine);
			} else {
				textBuffer.append(continuationLine);
			}

			textBuffer.append('\'');
			textBuffer.append(((ClassNode)triplet.getFirst()).getBinaryName());
			textBuffer.append("', '");
			textBuffer.append(((ClassNode)triplet.getSecond()).getBinaryName());
			textBuffer.append("' and '");
			textBuffer.append(((ClassNode)triplet.getThird()).getBinaryName());
			textBuffer.append('\'');
			List<ArchiveNode> archives = archivesByTriplet.get(triplet);
			if (archives != null) {
				for (int archiveIndex = 0; archiveIndex < archives.size(); archiveIndex++) {
					ArchiveNode archiveNode = archives.get(archiveIndex);
					if (archiveIndex == 0) {
						textBuffer.append(" in module" + (archives.size() > 1 ? "s '" : " '"));
					}

					textBuffer.append(archiveNode.getModuleName());
					if (archiveIndex < archives.size() - 1) {
						textBuffer.append("', ");
					} else {
						textBuffer.append('\'');
					}
				}

				textBuffer.append(GlobalUtils.LINE_SEPARATOR);
			} else {
				textBuffer.append(GlobalUtils.LINE_SEPARATOR);
			}
		}

		return textBuffer.toString();
	}

	public static Set<String> planAuxiliaryClassNames(
		Set<ClassNode> eligibleClasses,
		String existingDataClassName,
		AtomicReference<String> dataClassName,
		Map<ArchiveNode, String> archiveToDataClassNameMap,
		Map<ClassNode, String> classToDataClassNameMap,
		String packageParameterName,
		String configuredPackageName,
		Set<ClassNode> nonModuleSourceClasses,
		Set<ClassNode> classesNotInModules,
		UnmodifiableMultiMap<ArchiveNode, ClassNode> classesByModule,
		ReferenceGraph<ArchiveNode, ArchiveNode> moduleReferenceGraph,
		ReferenceGraph<ArchiveNode, String> modulePackageGraph,
		ReadOnlyMap<String, String> packageRenameMap,
		ReadOnlyMap<String, String> classNameToInternalNameMap,
		ReadOnlyMap<String, String> internalToBinaryNameMap,
		Set<String> reservedClassNames,
		String namePrefix,
		MixCaseType mixedCaseType,
		boolean avoidNameClash,
		boolean randomizeObfuscation,
		String featureName,
		List<String> reservedNames,
		ClassRepository classRepository,
		ScriptContext scriptContext
	) {
		String selectedPackageName = null;
		if (configuredPackageName != null && configuredPackageName.length() > 0) {
			String configuredPackageInternalName = GlobalUtils.binaryToInternalName(configuredPackageName);
			String mappedPackageName = packageRenameMap.get(configuredPackageInternalName);
			if (mappedPackageName != null) {
				selectedPackageName = mappedPackageName;
			} else {
				scriptContext.logWarning("Package '" + configuredPackageName + "' specified in '" + packageParameterName + "' parameter but the package does not exist.  Another package will be used.");
			}
		}

		HashSet<String> createdDataClassNames = GlobalUtils.newHashSet((nonModuleSourceClasses.size() > 0 ? 1 : 0) + GlobalUtils.getInitialCapacity(classesByModule.size()));
		if (classesByModule.isEmpty()) {
			String generatedDataClassName = generateAuxiliaryClassName(existingDataClassName, selectedPackageName, null, eligibleClasses, packageRenameMap, classNameToInternalNameMap, internalToBinaryNameMap, reservedClassNames, namePrefix, mixedCaseType, avoidNameClash, classRepository, reservedNames, randomizeObfuscation, scriptContext);
			createdDataClassNames.add(generatedDataClassName);
			dataClassName.set(generatedDataClassName);

			for (ClassNode eligibleClass : eligibleClasses) {
				if (!eligibleClass.isGeneratedClass()) {
					classToDataClassNameMap.put(eligibleClass, generatedDataClassName);
				}
			}

			return createdDataClassNames;
		} else {
			for (Entry<ArchiveNode, List<ClassNode>> moduleEntry : classesByModule.entrySet()) {
				ArchiveNode moduleNode = moduleEntry.getKey();
				if (!archiveToDataClassNameMap.containsKey(moduleNode)) {
					ensureCommonPackages(moduleNode, moduleReferenceGraph, modulePackageGraph);
					if (!archiveToDataClassNameMap.containsKey(moduleNode)) {
						String moduleDataClassName = null;
						if (moduleReferenceGraph.hasMember(moduleNode)) {
							for (ArchiveNode referencedModule : moduleReferenceGraph.getReferences(moduleNode)) {
								if (archiveToDataClassNameMap.containsKey(referencedModule)) {
									moduleDataClassName = archiveToDataClassNameMap.get(referencedModule);
									DebugUtils.assertTrue(
										modulePackageGraph.getReferences(moduleNode).contains(AbstractClassNode.getPackageName(moduleDataClassName)),
										new String[]{"No common package names : '" + moduleNode.getModuleName() + "' and '" + referencedModule.getModuleName() + "'"}
									);
									break;
								}
							}
						}

						if (moduleDataClassName == null) {
							moduleDataClassName = generateAuxiliaryClassName(
								existingDataClassName, selectedPackageName, modulePackageGraph.getReferences(moduleNode), moduleEntry.getValue(), packageRenameMap, classNameToInternalNameMap, internalToBinaryNameMap, reservedClassNames, namePrefix, mixedCaseType, avoidNameClash, classRepository, reservedNames, randomizeObfuscation, scriptContext
							);
						}

						archiveToDataClassNameMap.put(moduleNode, moduleDataClassName);
					}
				}
			}

			if (!classesNotInModules.isEmpty() && dataClassName.isNull()) {
				HashSet<ClassNode> sourceClassesInModules = GlobalUtils.newHashSet(nonModuleSourceClasses);
				sourceClassesInModules.removeAll(classesNotInModules);
				String foundDataClassName = null;
				Iterator<ClassNode> sourceClassIterator = sourceClassesInModules.iterator();

				while (sourceClassIterator.hasNext()) {
					Enumeration<ClasspathResource> classSources = sourceClassIterator.next().getClassSources();

					while (classSources.hasMoreElements()) {
						ClasspathResource classpathResource = classSources.nextElement();
						if (classpathResource.isZipEntry()) {
							ArchiveNode archiveNode = classpathResource.getArchiveNode();
							if (archiveNode.hasModuleName()) {
								foundDataClassName = archiveToDataClassNameMap.get(archiveNode);
							}
						}
					}
				}

				String resolvedDataClassName = foundDataClassName;
				if (foundDataClassName == null) {
					resolvedDataClassName = generateAuxiliaryClassName(existingDataClassName, selectedPackageName, null, classesNotInModules, packageRenameMap, classNameToInternalNameMap, internalToBinaryNameMap, reservedClassNames, namePrefix, mixedCaseType, avoidNameClash, classRepository, reservedNames, randomizeObfuscation, scriptContext);
					createdDataClassNames.add(resolvedDataClassName);
				}

				dataClassName.set(resolvedDataClassName);

				for (ClassNode classNotInModule : classesNotInModules) {
					classToDataClassNameMap.put(classNotInModule, resolvedDataClassName);
				}
			}

			for (Entry<ArchiveNode, List<ClassNode>> registrationEntry : classesByModule.entrySet()) {
				ArchiveNode registrationModule = registrationEntry.getKey();
				String archiveDataClassName = archiveToDataClassNameMap.get(registrationModule);
				createdDataClassNames.add(archiveDataClassName);

				for (ClassNode classNode : registrationEntry.getValue()) {
					if (!classNode.isGeneratedClass()) {
						classToDataClassNameMap.put(classNode, archiveDataClassName);
					}
				}
			}

			return createdDataClassNames;
		}
	}

	public static void propagateArchiveMappings(
		Set<ClassNode> eligibleClasses,
		AtomicReference<String> dataClassName,
		Map<ArchiveNode, String> archiveToDataClassNameMap,
		Set<ClassNode> classesNotInModules,
		Map<ClassNode, String> classToDataClassNameMap,
		UnmodifiableMultiMap<ArchiveNode, ClassNode> classesByModule
	) {
		for (Entry<ArchiveNode, List<ClassNode>> entry : classesByModule.entrySet()) {
			ArchiveNode moduleNode = entry.getKey();
			String archiveDataClassName = archiveToDataClassNameMap.get(moduleNode);

			for (ClassNode classNode : entry.getValue()) {
				classToDataClassNameMap.put(classNode, archiveDataClassName);
			}
		}

		if (!classesNotInModules.isEmpty()) {
			String injectedDataClassName = dataClassName.get();

			for (ClassNode classNotInModule : classesNotInModules) {
				classToDataClassNameMap.put(classNotInModule, injectedDataClassName);
			}
		}
	}
}
