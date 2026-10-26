package com.zelix.tools.renamer;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.Options;
import com.zelix.logging.AbstractLogger;
import com.zelix.tools.AbstractObfuscator;
import com.zelix.tools.excludes.DefaultExcludes;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.ClassTreeManager;
import com.zelix.workspace.Workspace;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassStruct {
	public final Set<ClassNode> excludedClasses = GlobalUtils.newHashSet();
	public final Set<String> excludedClassNames = GlobalUtils.newHashSet();
	public final Map<String, ClassNode> classNodesByInternalName = GlobalUtils.newHashMap();
	private Set<ClassHierarchyNode> deferredClasses = GlobalUtils.newHashSet();
	private final ClassNode[] classNodes;
	private final AbstractClassNode[] remapClassNodes;
	public ChangeLogContext changeLogContext;
	private boolean randomizeObfuscation;
	private final ClassTreeManager classTreeManager;
	private final Workspace workspace;
	public final DefaultExcludes defaultExcludes;
	public final HashMap<String, String> newClassNames;
	private final HashMap<String, String> oldClassNamesByNewName;
	public Set<String> lowercasedNames;
	private ClassRenamer renamer;
	public Set<String> nonFilesystemNames;

	public ClassStruct(
		DefaultExcludes defaultExcludes,
		ChangeLogContext changeLogContext,
		boolean randomizeObfuscation,
		Workspace workspace,
		ClassNode[] classNodes,
		AbstractClassNode[] remapClassNodes,
		ClassTreeManager classTreeManager,
		HashMap<String, String> newClassNames,
		HashMap<String, String> oldClassNamesByNewName
	) {
		this.classNodes = classNodes;
		this.remapClassNodes = remapClassNodes;
		this.changeLogContext = changeLogContext;
		this.randomizeObfuscation = randomizeObfuscation;
		this.classTreeManager = classTreeManager;
		this.workspace = workspace;
		this.defaultExcludes = defaultExcludes;
		this.newClassNames = newClassNames;
		this.oldClassNamesByNewName = oldClassNamesByNewName;
	}

	private void checkNewClassNameClashes(MultiMap<String, String> invertedPackageRenameMap) {
		if (this.changeLogContext != null) {
			ArrayList<String> oldClassNames = this.changeLogContext.getOldClassNames();
			int oldClassNameCount = oldClassNames.size();

			for (int i = 0; i < oldClassNameCount; i++) {
				String oldClassName = oldClassNames.get(i);
				if (ClassHierarchyNode.isWorkspaceClassByInternalName(oldClassName) && (!this.defaultExcludes.isClassNameExcluded(oldClassName) || !this.changeLogContext.isSimpleNameChanged(oldClassName))) {
					String newClassName = this.changeLogContext.getNewClassName(oldClassName);
					if (newClassName != null) {
						ClassHierarchyNode newClassNameNode = ClassHierarchyNode.getNodeByInternalName(newClassName);
						if (newClassNameNode != null && !newClassNameNode.isWorkspaceClass()) {
							this.changeLogContext
								.logWarning(
									"\"" + GlobalUtils.internalToBinaryName(newClassName) + "\" is specified as a new class name but it clashes with an existing class that has not been opened."
								);
						}
					} else {
						newClassName = oldClassName;
					}

					if (invertedPackageRenameMap != null && invertedPackageRenameMap.containsKey(newClassName) && this.changeLogContext.hasLoadedClasses()) {
						String clashingPackageName = invertedPackageRenameMap.get(newClassName).get(0);
						this.changeLogContext
							.logFatalError(
								"Class \""
									+ GlobalUtils.internalToBinaryName(oldClassName)
									+ "\" is mapped to \""
									+ GlobalUtils.internalToBinaryName(newClassName)
									+ "\" but this will result in a clash with package \""
									+ GlobalUtils.internalToBinaryName(clashingPackageName)
									+ "\" that has been renamed to \""
									+ GlobalUtils.internalToBinaryName(newClassName)
									+ "\""
							);
					}

					String oldPackageName = AbstractClassNode.getPackageName(oldClassName);
					ArrayList<String> candidateClassNames = this.renamer.getCandidateClassNames(newClassName, oldPackageName);

					for (int candidateIndex = 0; candidateIndex < candidateClassNames.size(); candidateIndex++) {
						String candidateClassName = candidateClassNames.get(candidateIndex);
						if (!candidateClassName.equals(oldClassName) && ClassHierarchyNode.isWorkspaceClassByInternalName(candidateClassName) && this.defaultExcludes.isClassNameExcluded(candidateClassName)) {
							String newClassMarker = !this.changeLogContext.hasOldClass(candidateClassName) ? " new" : "";
							this.changeLogContext
								.logFatalError(
									"Class \""
										+ GlobalUtils.internalToBinaryName(oldClassName)
										+ "\" is mapped to \""
										+ GlobalUtils.internalToBinaryName(newClassName)
										+ "\" but this will result in a clash with excluded"
										+ newClassMarker
										+ " class \""
										+ GlobalUtils.internalToBinaryName(candidateClassName)
										+ "\" that will be also be renamed to \""
										+ GlobalUtils.internalToBinaryName(newClassName)
										+ "\""
								);
						}
					}
				}
			}
		}
	}

	public void obfuscateClassNames(ClassRenamer renamer, HashMap<String, String> sourceNamesByNewName, HashMap<String, String> packageRenameMap, MultiMap<String, String> invertedPackageRenameMap, AbstractLogger logger) {
		int keepInnerClassInfo = renamer.getKeepInnerClassInfo();
		int keepGenericsInformation = renamer.getKeepGenericsInformation();
		this.renamer = renamer;
		this.checkNewClassNameClashes(invertedPackageRenameMap);
		int classNodeCount = this.classNodes.length;
		this.nonFilesystemNames = renamer.nonFilesystemNames;
		this.lowercasedNames = renamer.lowercasedNames;

		for (int classIndex = 0; classIndex < classNodeCount; classIndex++) {
			this.classNodesByInternalName.put(this.classNodes[classIndex].getInternalName(), this.classNodes[classIndex]);
			if (this.defaultExcludes.isClassExcluded(this.classNodes[classIndex]) || this.classNodes[classIndex].getSimpleNameInternal().equals("package-info")) {
				this.classNodes[classIndex].getInternalName();
				this.excludedClasses.add(this.classNodes[classIndex]);
			}
		}

		Iterator<ClassNode> excludedClassIterator = this.excludedClasses.iterator();

		while (excludedClassIterator.hasNext()) {
			ClassHierarchyNode.getNodeByInternalName(excludedClassIterator.next().getInternalName()).renameClassWithRepackagedName(renamer, this.newClassNames, this.oldClassNamesByNewName, this.excludedClassNames, this.nonFilesystemNames, this.lowercasedNames);
		}

		List<ClassHierarchyNode> workspaceClassRoots = this.classTreeManager.getWorkspaceClassRoots();
		if (Options.ZKM_RANDOMIZE_OBFUSCATION) {
		}

		if (this.randomizeObfuscation || Options.ZKM_RANDOMIZE_OBFUSCATION) {
			GlobalUtils.shuffle(workspaceClassRoots, GlobalUtils.newRandom(2));
		}

		for (int rootIndex = 0; rootIndex < workspaceClassRoots.size(); rootIndex++) {
			workspaceClassRoots.get(rootIndex).renameClassWithChangeLog(renamer, this.changeLogContext, this.newClassNames, this.oldClassNamesByNewName, this.excludedClasses, this.deferredClasses);
		}

		int deferredClassCount = this.deferredClasses.size();

		int previousDeferredClassCount;
		do {
			previousDeferredClassCount = deferredClassCount;
			Iterator<ClassHierarchyNode> deferredClassIterator = this.deferredClasses.iterator();

			while (deferredClassIterator.hasNext()) {
				if (deferredClassIterator.next().tryRenameClass(renamer, this.newClassNames, this.oldClassNamesByNewName, deferredClassIterator)) {
					deferredClassCount = this.deferredClasses.size();
				}
			}
		} while (deferredClassCount > 0 && deferredClassCount != previousDeferredClassCount);

		Iterator<ClassHierarchyNode> finalRenameClassIterator = this.deferredClasses.iterator();

		while (finalRenameClassIterator.hasNext()) {
			finalRenameClassIterator.next().renameClassWithGeneratedName(renamer, this.newClassNames, this.oldClassNamesByNewName);
		}

		if (Options.ZKM_DEBUG_SHOW_OLD_NAMES) {
		}

		for (int renameIndex = 0; renameIndex < this.classNodes.length; renameIndex++) {
			String internalName = this.classNodes[renameIndex].getInternalName();
			String newClassName = this.newClassNames.get(internalName);
			if (newClassName != null && !newClassName.equals(internalName)) {
				this.classNodes[renameIndex].renameClassInternal(newClassName, sourceNamesByNewName);
			}
		}

		if (this.changeLogContext != null) {
			AbstractObfuscator.mergeClassMappings(this.newClassNames, this.oldClassNamesByNewName, sourceNamesByNewName, this.workspace.getClassRepository(), this.changeLogContext);
		}

		this.workspace.remapAllReferences(this.remapClassNodes, this.workspace.getModuleInfoNodes(), keepInnerClassInfo, keepGenericsInformation, this.newClassNames, packageRenameMap, logger);
		this.workspace.rebuildAllRegistries();
	}

	public boolean isRandomizeObfuscation() {
		return this.randomizeObfuscation;
	}
}
