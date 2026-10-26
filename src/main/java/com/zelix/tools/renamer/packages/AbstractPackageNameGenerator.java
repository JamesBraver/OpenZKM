package com.zelix.tools.renamer.packages;

import com.zelix.enums.MixCaseType;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.repository.ClasspathScanner;
import com.zelix.tools.excludes.DefaultExcludes;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.MultiMap;
import java.util.Set;

public abstract class AbstractPackageNameGenerator {
	public ClasspathScanner classpathScanner;
	private final MixCaseType mixedCaseType;
	private final boolean randomizeObfuscation;

	public AbstractPackageNameGenerator(ClasspathScanner classpathScanner, MixCaseType mixedCaseType, boolean randomizeObfuscation) {
		this.classpathScanner = classpathScanner;
		this.mixedCaseType = mixedCaseType;
		this.randomizeObfuscation = randomizeObfuscation;
	}

	public abstract String generatePackageName(
		String parentPackageName,
		boolean isFromFilesystem,
		boolean requireLowercaseFirstChar,
		MultiMap<String, String> newPackageNames,
		DualKeyMap<String, String, ClassNode> excludedClasses,
		Set<String> excludedClassNames,
		ChangeLogContext changeLogContext,
		DefaultExcludes defaultExcludes
	);

	public boolean isRandomizeObfuscation() {
		return this.randomizeObfuscation;
	}

	public final boolean validateGeneratedName(
		String newName,
		String parentPackageName,
		boolean requireLowercaseFirstChar,
		MultiMap<String, String> newPackageNames,
		DualKeyMap<String, String, ClassNode> excludedClasses,
		Set<String> excludedClassNames,
		ChangeLogContext changeLogContext,
		DefaultExcludes defaultExcludes
	) {
		String newPackageName = parentPackageName + (parentPackageName.length() > 0 ? "/" : "") + newName;
		if (newPackageNames.containsKey(newPackageName)) {
			return false;
		} else if (changeLogContext != null && changeLogContext.hasNewPackageNameGroup(newPackageName)) {
			return false;
		} else if (changeLogContext != null && changeLogContext.hasNewClass(newPackageName)) {
			return false;
		} else if (excludedClasses.contains(parentPackageName, newName)) {
			return false;
		} else {
			return excludedClassNames != null && excludedClassNames.contains(newName)
				? false
				: (!requireLowercaseFirstChar || Character.isLowerCase(newName.charAt(0))) && (!defaultExcludes.isPackageExcluded(newPackageName) || !this.classpathScanner.packageExists(newPackageName));
		}
	}
}
