package com.zelix.workspace;

import com.zelix.enums.MixCaseType;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClasspathScanner;
import com.zelix.tools.excludes.DefaultExcludes;
import com.zelix.tools.renamer.packages.AbstractPackageNameGenerator;
import com.zelix.tools.renamer.packages.PackageNode;
import com.zelix.tools.renamer.packages.PackageTreeNode;
import com.zelix.tools.renamer.packages.SimplePackageNameGenerator;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.references.AtomicReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class PackageManager extends PackageNode {
	private static String[] splitPackageName(String packageName) {
		StringTokenizer tokenizer = new StringTokenizer(packageName, "/");
		int segmentCount = tokenizer.countTokens();
		String[] segments = new String[segmentCount];

		for (int i = 0; i < segmentCount; i++) {
			segments[i] = tokenizer.nextToken();
		}

		return segments;
	}

	public static String normalizePackageName(String packageName, AtomicReference<String> errorMessage) {
		String name = packageName.trim();
		String errorText = null;
		if (name.length() > 0) {
			name = name.replace('.', '/');
			if (name.startsWith("/")) {
				errorText = "Leading package separator character";
			} else if (name.indexOf("//") > -1) {
				errorText = "Duplicate package separator character";
			} else {
				if (name.endsWith("/")) {
					name = name.substring(0, name.length() - 1);
				}

				StringTokenizer tokenizer = new StringTokenizer(name, "/");

				while (tokenizer.hasMoreTokens() && errorText == null) {
					String segment = tokenizer.nextToken();

					for (int charIndex = 0; charIndex < segment.length(); charIndex++) {
						if (!Character.isJavaIdentifierPart(segment.charAt(charIndex))) {
							errorText = "Invalid character '" + segment.charAt(charIndex) + "'";
							break;
						}
					}
				}
			}
		}

		errorMessage.set(errorText);
		return name;
	}

	public static String[] getPackageAncestors(String packageName) {
		String[] segments = splitPackageName(packageName);
		int ancestorCount = Math.max(0, segments.length - 1);
		String[] ancestors = new String[ancestorCount];
		StringBuffer pathBuffer = new StringBuffer();

		for (int i = 0; i < ancestorCount; i++) {
			if (i > 0) {
				pathBuffer.append('/');
			}

			pathBuffer.append(segments[i]);
			ancestors[i] = pathBuffer.toString();
		}

		return ancestors;
	}

	public void pruneMissingPackages(ChangeLogContext changeLog, Set<String> availablePackages) {
		if (changeLog != null) {
			ListIterator<String> availableIterator = new ArrayList<>(availablePackages).listIterator();

			while (availableIterator.hasNext()) {
				String[] ancestors = getPackageAncestors(availableIterator.next());
				Collections.addAll(availablePackages, ancestors);
			}

			Map<String, PackageTreeNode> packageTreeNodes = this.getPackageTreeNodes();
			ArrayList<String> oldPackageNames = changeLog.getOldPackageNames();

			for (int i = 0; i < oldPackageNames.size(); i++) {
				String oldPackageName = oldPackageNames.get(i);
				if (!packageTreeNodes.containsKey(oldPackageName) && !availablePackages.contains(oldPackageName)) {
					changeLog.removePackageMapping(oldPackageName);
				}
			}
		}
	}

	public List<String> getPackageNames() {
		ArrayList<String> packageNames = new ArrayList<>();
		this.collectPackageNames(packageNames);
		return packageNames;
	}

	public void rebuildPackageTree(AbstractClassNode[] classNodes) {
		super.childPackages.clear();

		for (int i = 0; i < classNodes.length; i++) {
			this.ensurePackagePath(classNodes[i].getInternalName());
		}

		this.markPackagesFromFilesystem(classNodes);
	}

	private HashMap<String, String> assignPackageNames(
		DefaultExcludes excludes,
		ChangeLogContext changeLog,
		AbstractPackageNameGenerator nameGenerator,
		MultiMap<String, ClassNode> classesByPackage,
		boolean collapsePackages,
		Map<String, CollapseInfo> collapseInfos,
		boolean randomize
	) {
		List<String> packageNames = this.getPackageNames();
		HashMap<String, String> newPackageNames = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(packageNames.size()));
		MultiMap<String, String> packageMembers = new MultiMap<>(packageNames.size());

		for (int i = 0; i < packageNames.size(); i++) {
			String packageName = packageNames.get(i);
			if (excludes.isPackageExcluded(packageName)) {
				newPackageNames.put(packageName, packageName);
				packageMembers.put(packageName, packageName);
			} else if (changeLog != null && changeLog.hasOldPackageName(packageName)) {
				String mappedName = changeLog.getNewPackageName(packageName);
				if (mappedName != null) {
					newPackageNames.put(packageName, mappedName);
					packageMembers.put(mappedName, packageName);
				} else {
					newPackageNames.put(packageName, packageName);
					packageMembers.put(packageName, packageName);
				}
			} else if (collapseInfos != null && collapseInfos.containsKey(packageName)) {
				CollapseInfo collapseInfo = collapseInfos.get(packageName);
				if (!collapseInfo.isCollapsed()) {
					String collapseTarget = collapseInfo.getPackageName();
					newPackageNames.put(packageName, collapseTarget);
					packageMembers.put(collapseTarget, packageName);
				}
			}
		}

		DualKeyMap<String, String, ClassNode> excludedClasses = new DualKeyMap<>();
		Enumeration<String> packageKeys = packageMembers.keys();

		while (packageKeys.hasMoreElements()) {
			String newPackageName = packageKeys.nextElement();
			List<String> oldPackageNames = packageMembers.get(newPackageName);

			for (int memberIndex = 0; memberIndex < oldPackageNames.size(); memberIndex++) {
				String oldPackageName = oldPackageNames.get(memberIndex);
				List<ClassNode> classes = classesByPackage.get(oldPackageName);
				if (classes != null) {
					for (int classIndex = 0; classIndex < classes.size(); classIndex++) {
						ClassNode classNode = classes.get(classIndex);
						if (excludes.isClassExcluded(classNode)) {
							excludedClasses.put(newPackageName, classNode.getSimpleNameInternal(), classNode);
						}
					}
				}
			}
		}

		if (!collapsePackages) {
			PackageTreeNode[] childPackages = super.childPackages.toArray(new PackageTreeNode[super.childPackages.size()]);
			if (!randomize && !Options.ZKM_RANDOMIZE_OBFUSCATION) {
				Arrays.sort((Object[])childPackages);
			} else {
				GlobalUtils.shuffle(Arrays.asList(childPackages), GlobalUtils.newRandom(8));
			}

			for (int childIndex = 0; childIndex < childPackages.length; childIndex++) {
				childPackages[childIndex].assignPackageNames(excludes, changeLog, nameGenerator, newPackageNames, packageMembers, excludedClasses, classesByPackage, randomize);
			}
		} else {
			Map<String, PackageTreeNode> packageTreeNodes = this.getPackageTreeNodes();

			for (String unmappedPackage : packageTreeNodes.keySet()) {
				if (newPackageNames.get(unmappedPackage) == null) {
					String collapseTargetName = collapseInfos.get(unmappedPackage).getPackageName();
					if (packageTreeNodes.get(collapseTargetName) == null) {
					}

					String generatedName = nameGenerator.generatePackageName(collapseTargetName, packageTreeNodes.get(unmappedPackage).isAllFromFilesystem(), false, packageMembers, excludedClasses, null, changeLog, excludes);
					if (collapseTargetName.length() > 0) {
						generatedName = collapseTargetName + "/" + generatedName;
					}

					newPackageNames.put(unmappedPackage, generatedName);
					packageMembers.put(generatedName, unmappedPackage);
				}
			}
		}

		this.rebuildRenamedPackageTree(packageMembers);
		return newPackageNames;
	}

	public HashMap<String, String> createPackageRenameMap(
		Enumeration<ClassNode> classes,
		DefaultExcludes excludes,
		ChangeLogContext changeLog,
		boolean isLooseChangeLogFile,
		boolean randomizeObfuscation,
		boolean useAsciiCharactersOnly,
		boolean legalIdentifiers,
		String newNamesPrefix,
		MixCaseType mixedCaseClassNames,
		ClasspathScanner classpathScanner,
		boolean collapsePackages,
		String defaultPackageName,
		Iterator<String> resourceFilePaths,
		List<String> overrideNames,
		ScriptContext logContext
	) {
		DualKeyMap<String, String, String> resourcePathsByPackage = this.splitResourceFilePaths(resourceFilePaths);
		this.validateResourceFilePaths(resourcePathsByPackage, excludes, changeLog);
		Map<String, CollapseInfo> collapseInfos = null;
		Map<String, PackageTreeNode> packageTreeNodes = this.getPackageTreeNodes();
		MultiMap<String, ClassNode> classesByPackage = new MultiMap<>();

		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			String packageName = classNode.getPackageName();
			classesByPackage.put(packageName, classNode);
			if (classNode.isFxmlFqnTag()) {
				PackageTreeNode packageTreeNode = packageTreeNodes.get(packageName);
				if (packageTreeNode != null) {
					packageTreeNode.getTopLevelPackage().markFxmlFqnTag();
				}
			}
		}

		if (collapsePackages) {
			collapseInfos = this.computeCollapseInfos(classesByPackage, excludes, changeLog, defaultPackageName, resourcePathsByPackage, logContext);
		}

		SimplePackageNameGenerator nameGenerator = new SimplePackageNameGenerator(classpathScanner, useAsciiCharactersOnly, legalIdentifiers, randomizeObfuscation, newNamesPrefix, mixedCaseClassNames, overrideNames);
		HashMap<String, String> packageRenameMap = this.assignPackageNames(excludes, changeLog, nameGenerator, classesByPackage, collapsePackages, collapseInfos, randomizeObfuscation);
		if (changeLog != null) {
			ArrayList<String> oldPackageNames = changeLog.getOldPackageNames();

			for (int i = 0; i < oldPackageNames.size(); i++) {
				String oldPackageName = oldPackageNames.get(i);
				if (!packageRenameMap.containsKey(oldPackageName)) {
					String newPackageName = changeLog.getNewPackageName(oldPackageName);
					if (newPackageName == null) {
						newPackageName = oldPackageName;
					}

					packageRenameMap.put(oldPackageName, newPackageName);
				}
			}
		}

		return packageRenameMap;
	}

	public Map<String, PackageTreeNode> getPackageTreeNodes() {
		HashMap<String, PackageTreeNode> packageTreeNodes = GlobalUtils.newHashMap();
		this.collectPackageNodes(packageTreeNodes);
		return packageTreeNodes;
	}

	public void rebuildRenamedPackageTree(MultiMap<String, String> newPackageNames) {
		Map<String, PackageTreeNode> packageTreeNodes = this.getPackageTreeNodes();
		super.childPackages.clear();
		TreeMap<String, Boolean> newPackageIsRoot = new TreeMap<>();

		for (String newPackageName : newPackageNames.keySet()) {
			boolean isRoot = true;

			for (String oldPackageName : newPackageNames.get(newPackageName)) {
				if (!packageTreeNodes.get(oldPackageName).isAllFromFilesystem()) {
					isRoot = false;
				}
			}

			newPackageIsRoot.put(newPackageName, isRoot);
		}

		for (Entry<String, Boolean> entry : newPackageIsRoot.entrySet()) {
			this.ensurePackagePath(entry.getKey() + '/', newPackageIsRoot);
		}
	}

	public HashMap<String, String> getPackageNameMappings(ChangeLogContext changeLog) {
		HashMap<String, String> renameMap = GlobalUtils.newHashMap();
		if (changeLog != null) {
			for (String oldPackageName : changeLog.getOldPackageNames()) {
				String newPackageName = changeLog.getNewPackageName(oldPackageName);
				if (newPackageName == null) {
					newPackageName = oldPackageName;
				}

				renameMap.put(oldPackageName, newPackageName);
			}
		}

		for (String packageName : this.getPackageNames()) {
			if (!renameMap.containsKey(packageName)) {
				renameMap.put(packageName, packageName);
			}
		}

		return renameMap;
	}

	private Map<String, CollapseInfo> computeCollapseInfos(
		MultiMap<String, ClassNode> classesByPackage, DefaultExcludes excludes, ChangeLogContext changeLog, String defaultPackageName, DualKeyMap<String, String, String> resourceFilePaths, ScriptContext logContext
	) {
		HashMap<String, String> packageTargetNames = this.getPackageTargetNames(excludes, changeLog, defaultPackageName);
		HashMap<String, CollapseInfo> collapseInfos = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(packageTargetNames.size()));
		MultiMap<String, String> packagesSharingTarget = new MultiMap<>();

		for (Entry<String, String> targetEntry : packageTargetNames.entrySet()) {
			packagesSharingTarget.put(targetEntry.getValue(), targetEntry.getKey());
		}

		Enumeration<String> targetNames = packagesSharingTarget.keys();

		while (targetNames.hasMoreElements()) {
			String targetName = targetNames.nextElement();
			Set<String> oldPackageNames = null;
			if (changeLog != null) {
				oldPackageNames = changeLog.getOldPackageNamesForNewName(targetName);
			}

			if (oldPackageNames == null) {
				oldPackageNames = GlobalUtils.newHashSet();
			}

			oldPackageNames.add(targetName);
			AtomicReference<String> newPackageNameRef = new AtomicReference<>();
			HashMap<String, ClassNode> classesBySimpleName = GlobalUtils.newHashMap();
			HashMap<String, String> renamedClassNames = GlobalUtils.newHashMap();

			for (String oldPackageName : oldPackageNames) {
				List<ClassNode> classes = classesByPackage.get(oldPackageName);
				if (classes != null && classes.size() > 0 && this.detectSimpleNameClashes(classesBySimpleName, classes, excludes, changeLog, newPackageNameRef)) {
					changeLog.logFatalError("Package mapping conflicts with class name exclusions : " + newPackageNameRef.get());
				}

				Map<String, String> resourcePaths = resourceFilePaths.getGroup(oldPackageName);
				if (resourcePaths != null && resourcePaths.size() > 0 && this.detectResourceNameClashes(renamedClassNames, resourcePaths, newPackageNameRef)) {
					changeLog.logFatalError("Package mapping conflict : " + newPackageNameRef.get());
				}
			}

			List<String> collapsiblePackages = packagesSharingTarget.get(targetName);
			Collections.sort(collapsiblePackages);
			if (collapsiblePackages.size() > 0) {
				for (int i = 0; i < collapsiblePackages.size(); i++) {
					String collapsiblePackage = collapsiblePackages.get(i);
					List<ClassNode> classesInPackage = classesByPackage.get(collapsiblePackage);
					CollapseInfo collapseInfo;
					if (classesInPackage != null) {
						if (this.detectSimpleNameClashes(classesBySimpleName, classesInPackage, excludes, changeLog, newPackageNameRef)) {
							collapseInfo = new CollapseInfo(targetName, true);
							collapseInfos.put(collapsiblePackage, collapseInfo);
							logContext.logWarning(
								"Cannot collapse package '"
									+ GlobalUtils.internalToBinaryName(collapsiblePackage)
									+ "' into '"
									+ GlobalUtils.internalToBinaryName(targetName)
									+ "' because : \""
									+ newPackageNameRef.get()
									+ "\""
							);
						} else {
							collapseInfo = new CollapseInfo(targetName);
							collapseInfos.put(collapsiblePackage, collapseInfo);
						}
					} else {
						collapseInfo = new CollapseInfo(targetName);
						collapseInfos.put(collapsiblePackage, collapseInfo);
					}

					Map<String, String> packageResourcePaths = resourceFilePaths.getGroup(collapsiblePackage);
					if (packageResourcePaths != null && packageResourcePaths.size() > 0 && this.detectResourceNameClashes(renamedClassNames, packageResourcePaths, newPackageNameRef)) {
						if (!collapseInfo.isCollapsed()) {
							collapseInfo.setCollapsed(true);
						}

						logContext.logWarning(
							"Cannot collapse package '"
								+ GlobalUtils.internalToBinaryName(collapsiblePackage)
								+ "' into '"
								+ GlobalUtils.internalToBinaryName(targetName)
								+ "' because : \""
								+ newPackageNameRef.get()
								+ "\""
						);
					}
				}
			} else {
				collapseInfos.put(collapsiblePackages.get(0), new CollapseInfo(targetName));
			}
		}

		return collapseInfos;
	}

	private DualKeyMap<String, String, String> splitResourceFilePaths(Iterator<String> resourceFilePaths) {
		DualKeyMap<String, String, String> resourcePathsByPackage = new DualKeyMap<>();
		List<String> packageNames = this.getPackageNames();
		Collections.sort(packageNames, Collections.reverseOrder());

		while (resourceFilePaths.hasNext()) {
			String resourcePath = resourceFilePaths.next().replace(SystemConstants.pathSep, '/');
			boolean matchedPackage = false;

			for (int i = 0; i < packageNames.size(); i++) {
				String packageName = packageNames.get(i);
				if (resourcePath.startsWith(packageName) && resourcePath.length() > packageName.length() && resourcePath.charAt(packageName.length()) == '/') {
					resourcePathsByPackage.put(packageName, resourcePath.substring(packageName.length()), resourcePath);
					matchedPackage = true;
					break;
				}
			}

			if (!matchedPackage) {
				String absolutePath;
				if (resourcePath.charAt(0) == '/') {
					absolutePath = resourcePath;
				} else {
					absolutePath = '/' + resourcePath;
				}

				resourcePathsByPackage.put("", absolutePath, resourcePath);
			}
		}

		return resourcePathsByPackage;
	}

	private void validateResourceFilePaths(DualKeyMap<String, String, String> resourceFilePaths, DefaultExcludes excludes, ChangeLogContext changeLog) {
		if (changeLog != null) {
			ArrayList<String> oldPackageNames = changeLog.getOldPackageNames();
			int packageCount = oldPackageNames.size();

			for (int i = 0; i < packageCount; i++) {
				String oldPackageName = oldPackageNames.get(i);
				String newPackageName = changeLog.getNewPackageName(oldPackageName);
				if (newPackageName == null) {
					newPackageName = oldPackageName;
				}

				if (!excludes.isPackageExcluded(oldPackageName) && excludes.isClassNameExcluded(newPackageName)) {
					changeLog.logFatalError(
						"Package \""
							+ GlobalUtils.internalToBinaryName(oldPackageName)
							+ "\" is mapped to \""
							+ GlobalUtils.internalToBinaryName(newPackageName)
							+ "\" but this would result in a clash with class \""
							+ GlobalUtils.internalToBinaryName(newPackageName)
							+ "\" that will not be renamed."
					);
				}
			}

			HashMap<String, String> reservedPaths = GlobalUtils.newHashMap();
			Enumeration<String> packageKeys = resourceFilePaths.getGroupKeys();

			while (packageKeys.hasMoreElements()) {
				String packageName = packageKeys.nextElement();
				if (excludes.isPackageExcluded(packageName) || changeLog.hasOldPackageName(packageName) && !changeLog.hasNewPackageName(packageName)) {
					Map<String, String> groupPaths = resourceFilePaths.getGroup(packageName);
					if (groupPaths != null) {
						for (String sourcePath : groupPaths.keySet()) {
							String targetPath = groupPaths.get(sourcePath);
							reservedPaths.put(targetPath, targetPath);
						}
					}
				}
			}

			packageKeys = resourceFilePaths.getGroupKeys();

			while (packageKeys.hasMoreElements()) {
				String mappedPackageName = packageKeys.nextElement();
				if (!excludes.isPackageExcluded(mappedPackageName) && changeLog.hasOldPackageName(mappedPackageName) && changeLog.hasNewPackageName(mappedPackageName)) {
					String mappedTarget = changeLog.getNewPackageName(mappedPackageName);
					Map<String, String> mappedGroupPaths = resourceFilePaths.getGroup(mappedPackageName);
					if (mappedGroupPaths != null) {
						for (String resourcePathTail : mappedGroupPaths.keySet()) {
							String existingTarget = mappedGroupPaths.get(resourcePathTail);
							String fullTargetPath = mappedTarget + resourcePathTail;
							String previousTarget = reservedPaths.put(fullTargetPath, existingTarget);
							if (previousTarget != null) {
								reservedPaths.put(fullTargetPath, previousTarget);
								changeLog.logFatalError(
									"Package \""
										+ GlobalUtils.internalToBinaryName(mappedPackageName)
										+ "\" is mapped to \""
										+ GlobalUtils.internalToBinaryName(mappedTarget)
										+ "\" but this would result in \""
										+ previousTarget
										+ "\" clashing with \""
										+ existingTarget
										+ "\""
								);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public boolean isRootNode() {
		return true;
	}

	public HashMap<String, String> getPackageTargetNames(DefaultExcludes excludes, ChangeLogContext changeLog, String defaultPackageName) {
		HashMap<String, String> targetNames = GlobalUtils.newHashMap();

		for (PackageTreeNode childPackage : super.childPackages) {
			String packageName = childPackage.getPackageName();
			String targetName;
			if (changeLog != null && changeLog.hasOldPackageName(packageName)) {
				String newPackageName = changeLog.getNewPackageName(packageName);
				if (newPackageName != null) {
					targetName = newPackageName;
				} else {
					targetName = packageName;
				}
			} else if (excludes.isPackageExcluded(packageName)) {
				targetName = packageName;
			} else {
				targetName = defaultPackageName;
			}

			targetNames.put(packageName, targetName);
			childPackage.buildPackageNameMap(targetNames, excludes, changeLog, targetName);
		}

		return targetNames;
	}

	@Override
	public void addSelfToMap(Map<String, PackageTreeNode> packageNodes) {
	}

	private void markPackagesFromFilesystem(AbstractClassNode[] classNodes) {
		Map<String, PackageTreeNode> packageTreeNodes = this.getPackageTreeNodes();
		MultiMap<PackageNode, AbstractClassNode> classesByPackageNode = new MultiMap<>(packageTreeNodes.size());

		for (AbstractClassNode classNode : classNodes) {
			PackageTreeNode packageTreeNode = packageTreeNodes.get(classNode.getPackageName());
			if (packageTreeNode != null) {
				classesByPackageNode.put(packageTreeNode, classNode);

				for (PackageNode ancestorNode = packageTreeNode.parent; !ancestorNode.isRootNode(); ancestorNode = ((PackageTreeNode)ancestorNode).parent) {
					classesByPackageNode.put(ancestorNode, classNode);
				}
			}
		}

		for (Entry<PackageNode, List<AbstractClassNode>> entry : classesByPackageNode.entrySet()) {
			PackageTreeNode packageNode = (PackageTreeNode)entry.getKey();
			boolean allFromFilesystem = true;
			Iterator<AbstractClassNode> classIterator = entry.getValue().iterator();

			while (classIterator.hasNext()) {
				if (!classIterator.next().isFromFilesystem()) {
					allFromFilesystem = false;
					break;
				}
			}

			packageNode.setAllFromFilesystem(allFromFilesystem);
		}
	}

	private boolean detectSimpleNameClashes(Map<String, ClassNode> classesBySimpleName, List<ClassNode> classes, DefaultExcludes excludes, ChangeLogContext changeLog, AtomicReference<String> errorMessage) {
		boolean clashDetected = false;
		ArrayList<String> addedSimpleNames = new ArrayList<>();

		for (int i = 0; i < classes.size(); i++) {
			ClassNode classNode = classes.get(i);
			String internalName = classNode.getInternalName();
			if (changeLog != null && changeLog.hasOldClass(internalName)) {
				String newClassName = changeLog.getNewClassName(internalName);
				if (newClassName == null) {
					newClassName = internalName;
				}

				String newSimpleName = AbstractClassNode.getSimpleNameFromInternalName(newClassName);
				ClassNode previousMappedClass = classesBySimpleName.put(newSimpleName, classNode);
				if (previousMappedClass != null && previousMappedClass != classNode) {
					classesBySimpleName.put(newSimpleName, previousMappedClass);
					errorMessage.set("Unqualified class names for classes '" + previousMappedClass.getBinaryName() + "' and '" + classNode.getBinaryName() + "' would clash (A)");
					clashDetected = true;
					break;
				}

				addedSimpleNames.add(newSimpleName);
			} else if (excludes.isClassExcluded(classNode) || classNode.getSimpleNameInternal().equals("package-info")) {
				String simpleName = classNode.getSimpleNameInternal();
				ClassNode previousExcludedClass = classesBySimpleName.put(classNode.getSimpleNameInternal(), classNode);
				if (previousExcludedClass != null && previousExcludedClass != classNode) {
					classesBySimpleName.put(simpleName, previousExcludedClass);
					errorMessage.set("Unqualified class names for classes '" + previousExcludedClass.getBinaryName() + "' and '" + classNode.getBinaryName() + "' would clash (B)");
					clashDetected = true;
					break;
				}

				addedSimpleNames.add(simpleName);
			}
		}

		if (clashDetected) {
			for (int nameIndex = 0; nameIndex < addedSimpleNames.size(); nameIndex++) {
				classesBySimpleName.remove(addedSimpleNames.get(nameIndex));
			}
		}

		return clashDetected;
	}

	@Override
	public String getPackageName() {
		return "";
	}

	private boolean detectResourceNameClashes(Map<String, String> resourcePaths, Map<String, String> newResourcePaths, AtomicReference<String> errorMessage) {
		boolean clashDetected = false;
		ArrayList<String> addedPaths = new ArrayList<>();

		for (Entry<String, String> entry : newResourcePaths.entrySet()) {
			String path = entry.getKey();
			String target = entry.getValue();
			String previousTarget = resourcePaths.put(path, target);
			if (previousTarget != null && !previousTarget.equals(target)) {
				clashDetected = true;
				errorMessage.set("Non-class files '" + previousTarget + "' and '" + target + "' would clash");
				resourcePaths.put(path, previousTarget);

				for (int i = 0; i < addedPaths.size(); i++) {
					resourcePaths.remove(addedPaths.get(i));
				}
			} else {
				addedPaths.add(path);
			}
		}

		return clashDetected;
	}

	private void ensurePackagePath(String internalName) {
		this.ensurePackagePath(internalName, null);
	}

	private void ensurePackagePath(String internalName, Map<String, Boolean> retainFlags) {
		int lastSeparator = internalName.lastIndexOf(47);
		if (lastSeparator != -1) {
			String[] segments = splitPackageName(internalName.substring(0, lastSeparator));
			if (segments.length > 0) {
				this.addPackagePath(0, segments, retainFlags);
			}
		}
	}

	public boolean isEmpty() {
		return super.childPackages == null || super.childPackages.size() == 0;
	}

	public static class CollapseInfo {
		private final String packageName;
		private boolean collapsed;

		public CollapseInfo(String packageName) {
			this.packageName = packageName;
		}

		public CollapseInfo(String packageName, boolean collapsed) {
			this.packageName = packageName;
			this.collapsed = collapsed;
		}

		public void setCollapsed(boolean collapsed) {
			this.collapsed = collapsed;
		}

		public boolean isCollapsed() {
			return this.collapsed;
		}

		public String getPackageName() {
			return this.packageName;
		}
	}
}
