package com.zelix.tools.renamer.packages;

import com.zelix.framework.wrappers.ClassNode;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.Options;
import com.zelix.tools.excludes.DefaultExcludes;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.MultiMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PackageTreeNode extends PackageNode {
	public final PackageNode parent;
	public final String simpleName;
	private boolean fxmlFqnTag;

	public PackageTreeNode(PackageNode parent, String simpleName, boolean fromFilesystem) {
		this.parent = parent;
		this.simpleName = simpleName;
		if (!fromFilesystem) {
			this.setAllFromFilesystem(false);
		}
	}

	public String getSimpleName() {
		return this.simpleName;
	}

	public void buildPackageNameMap(Map<String, String> packageNameMap, DefaultExcludes defaultExcludes, ChangeLogContext changeLogContext, String inheritedNewName) {
		Iterator<PackageTreeNode> childPackageIterator = super.childPackages.iterator();

		while (childPackageIterator.hasNext()) {
			String newPackageName = inheritedNewName;
			PackageTreeNode childPackage = childPackageIterator.next();
			String oldPackageName = childPackage.getPackageName();
			if (changeLogContext != null && changeLogContext.hasOldPackageName(oldPackageName)) {
				String changeLogNewPackageName = changeLogContext.getNewPackageName(oldPackageName);
				if (changeLogNewPackageName != null) {
					newPackageName = changeLogNewPackageName;
				} else {
					newPackageName = oldPackageName;
				}
			} else if (defaultExcludes.isPackageExcluded(oldPackageName)) {
				newPackageName = oldPackageName;
			}

			packageNameMap.put(oldPackageName, newPackageName);
			childPackage.buildPackageNameMap(packageNameMap, defaultExcludes, changeLogContext, newPackageName);
		}
	}

	public void assignPackageNames(
		DefaultExcludes defaultExcludes,
		ChangeLogContext changeLogContext,
		AbstractPackageNameGenerator packageNameGenerator,
		Map<String, String> packageNameMap,
		MultiMap<String, String> assignedNames,
		DualKeyMap<String, String, ClassNode> usedClassNamesByPackage,
		MultiMap<String, ClassNode> classesByPackage,
		boolean randomizeObfuscation
	) {
		HashSet<String> excludedSimpleNames = GlobalUtils.newHashSet();
		List<ClassNode> parentPackageClasses = classesByPackage.get(this.parent.getPackageName());
		if (parentPackageClasses != null) {
			for (int classIndex = 0; classIndex < parentPackageClasses.size(); classIndex++) {
				ClassNode classNode = parentPackageClasses.get(classIndex);
				if (defaultExcludes.isClassExcluded(classNode)) {
					excludedSimpleNames.add(classNode.getSimpleNameInternal());
				}
			}
		}

		this.assignPackageName(defaultExcludes, changeLogContext, packageNameGenerator, packageNameMap, assignedNames, usedClassNamesByPackage, excludedSimpleNames);
		PackageTreeNode[] childPackageNodes = super.childPackages.toArray(new PackageTreeNode[super.childPackages.size()]);
		if (!randomizeObfuscation && !Options.ZKM_RANDOMIZE_OBFUSCATION) {
			Arrays.sort(childPackageNodes);
		} else {
			GlobalUtils.shuffle(Arrays.asList(childPackageNodes), GlobalUtils.newRandom(16));
		}

		for (int childIndex = 0; childIndex < childPackageNodes.length; childIndex++) {
			childPackageNodes[childIndex].assignPackageNames(defaultExcludes, changeLogContext, packageNameGenerator, packageNameMap, assignedNames, usedClassNamesByPackage, classesByPackage, randomizeObfuscation);
		}
	}

	@Override
	public void addOwnPackageName(ArrayList<String> packageNames) {
		String packageName = this.getPackageName();
		packageNames.add(packageName);
	}

	public PackageTreeNode getTopLevelPackage() {
		return this.parent.isRootNode() ? this : ((PackageTreeNode)this.parent).getTopLevelPackage();
	}

	@Override
	public boolean isRootNode() {
		return false;
	}

	private void assignPackageName(
		DefaultExcludes defaultExcludes,
		ChangeLogContext changeLogContext,
		AbstractPackageNameGenerator packageNameGenerator,
		Map<String, String> packageNameMap,
		MultiMap<String, String> assignedNames,
		DualKeyMap<String, String, ClassNode> usedClassNamesByPackage,
		Set<String> excludedSimpleNames
	) {
		String packageName = this.getPackageName();
		boolean requireLowercaseFirstChar = this.isTopLevelPackage() && this.fxmlFqnTag;
		if (!packageNameMap.containsKey(packageName)) {
			String parentPackageName = packageNameMap.get(this.parent.getPackageName());
			if (parentPackageName == null) {
				parentPackageName = this.parent.getPackageName();
			}

			String generatedPackageName = packageNameGenerator.generatePackageName(parentPackageName, this.isAllFromFilesystem(), requireLowercaseFirstChar, assignedNames, usedClassNamesByPackage, excludedSimpleNames, changeLogContext, defaultExcludes);
			String newPackageName = (parentPackageName.length() > 0 ? parentPackageName + "/" : "") + generatedPackageName;
			packageNameMap.put(packageName, newPackageName);
			assignedNames.put(newPackageName, packageName);
		}
	}

	@Override
	public void addSelfToMap(Map<String, PackageTreeNode> packageNodes) {
		String packageName = this.getPackageName();
		packageNodes.put(packageName, this);
	}

	public boolean isTopLevelPackage() {
		return this.parent.isRootNode();
	}

	public void markFxmlFqnTag() {
		this.fxmlFqnTag = true;
	}

	@Override
	public String getPackageName() {
		String parentPackageName = this.parent == null ? null : this.parent.getPackageName();
		return parentPackageName != null && parentPackageName.length() > 0 ? parentPackageName + "/" + this.simpleName : this.simpleName;
	}
}
