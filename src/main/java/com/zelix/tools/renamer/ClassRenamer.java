package com.zelix.tools.renamer;

import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.Options;
import com.zelix.tools.excludes.DefaultExcludes;
import com.zelix.tools.excludes.ExcludeElement;
import com.zelix.tools.renamer.packages.PackageTreeNode;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.workspace.ClassHierarchyNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;

public abstract class ClassRenamer {
	public static final String[] reservedDeviceNames = new String[]{
		"CON",
		"PRN",
		"AUX",
		"CLOCK$",
		"NUL",
		"COM0",
		"COM1",
		"COM2",
		"COM3",
		"COM4",
		"COM5",
		"COM6",
		"COM7",
		"COM8",
		"COM9",
		"LPT0",
		"LPT1",
		"LPT2",
		"LPT3",
		"LPT4",
		"LPT5",
		"LPT6",
		"LPT7",
		"LPT8",
		"LPT9"
	};
	public static final Set<String> reservedNames = GlobalUtils.newHashSet(Arrays.asList(reservedDeviceNames));
	public final Set<String> generatedNames = GlobalUtils.newHashSet();
	public final Set<String> nonFilesystemNames = GlobalUtils.newHashSet();
	public final Set<String> lowercasedNames = GlobalUtils.newHashSet();
	public ClassStruct classStruct;
	private final HashMap<String, String> repackages;
	private final Map<String, PackageTreeNode> packageTreeNodes;
	public final int keepInnerClassInfo;
	public final int keepGenericsInformation;
	public final boolean randomizeObfuscation;
	public final Set<ClassNode> excludedClasses;
	public final Set<String> excludedClassNames;
	public final HashMap<String, String> newClassNames;
	public final Map<String, ClassNode> classNodesByInternalName;
	public final DefaultExcludes defaultExcludes;
	public final ChangeLogContext changeLogContext;
	private final MultiMap<String, String> invertedRepackages;

	public ClassRenamer(
		ClassStruct classStruct, HashMap<String, String> repackages, MultiMap<String, String> invertedRepackages, Map<String, PackageTreeNode> packageTreeNodes, int keepInnerClassInfo, int keepGenericsInformation, boolean randomizeObfuscation
	) {
		this.classStruct = classStruct;
		this.repackages = repackages;
		this.packageTreeNodes = packageTreeNodes;
		this.keepInnerClassInfo = keepInnerClassInfo;
		this.keepGenericsInformation = keepGenericsInformation;
		this.randomizeObfuscation = randomizeObfuscation;
		this.excludedClasses = classStruct.excludedClasses;
		this.excludedClassNames = classStruct.excludedClassNames;
		this.newClassNames = classStruct.newClassNames;
		this.classNodesByInternalName = classStruct.classNodesByInternalName;
		this.defaultExcludes = classStruct.defaultExcludes;
		this.changeLogContext = classStruct.changeLogContext;
		this.invertedRepackages = invertedRepackages;

		for (Entry<String, PackageTreeNode> entry : packageTreeNodes.entrySet()) {
			if (!entry.getValue().isAllFromFilesystem()) {
				this.nonFilesystemNames.add(entry.getKey().toLowerCase());
			}

			this.lowercasedNames.add(entry.getKey().toLowerCase());
		}
	}

	public static String getSimpleName(String internalName) {
		int lastSeparatorIndex = internalName.lastIndexOf(47);
		return lastSeparatorIndex == -1 ? internalName : internalName.substring(lastSeparatorIndex + 1);
	}

	public final boolean validateDerivedName(String currentName, String newName, boolean allowSameName, boolean isFromFilesystem, AtomicInt errorCode) {
		return this.validateName(currentName, newName, allowSameName, isFromFilesystem, false, errorCode);
	}

	public final boolean validateGeneratedName(String currentName, String newName, boolean isFromFilesystem, boolean requireUppercaseFirstChar) {
		return this.validateName(currentName, newName, false, isFromFilesystem, requireUppercaseFirstChar, new AtomicInt(0));
	}

	public final boolean validateName(String currentName, String newName, boolean allowSameName, boolean isFromFilesystem, boolean requireUppercaseFirstChar, AtomicInt errorCode) {
		if (this.changeLogContext == null || !this.changeLogContext.hasNewClass(newName) && !this.changeLogContext.isInjectedClassName(newName)) {
			if (this.excludedClassNames.contains(newName)) {
				errorCode.set(2);
				return false;
			} else if (this.generatedNames.contains(newName)) {
				errorCode.set(3);
				return false;
			} else if (this.packageTreeNodes.get(newName) != null) {
				errorCode.set(4);
				return false;
			} else if (!allowSameName && currentName.equals(newName)) {
				errorCode.set(5);
				return false;
			} else if (newName.length() == 1 && TypeWriter.isPrimitiveDescriptor(newName)) {
				errorCode.set(6);
				return false;
			} else if ((Options.checkReservedClassNames || !isFromFilesystem) && reservedNames.contains(AbstractClassNode.getSimpleNameFromInternalName(newName).toUpperCase())) {
				errorCode.set(7);
				return false;
			} else if (this.nonFilesystemNames.contains(newName.toLowerCase())) {
				errorCode.set(8);
				return false;
			} else if (!isFromFilesystem && this.lowercasedNames.contains(newName.toLowerCase())) {
				errorCode.set(9);
				return false;
			} else if (requireUppercaseFirstChar && !Character.isUpperCase(AbstractClassNode.getSimpleNameFromInternalName(newName).charAt(0))) {
				errorCode.set(10);
				return false;
			} else {
				errorCode.set(0);
				return true;
			}
		} else {
			errorCode.set(1);
			return false;
		}
	}

	public abstract String generateClassName(ClassHierarchyNode node, boolean isFromFilesystem);

	public int getKeepGenericsInformation() {
		return this.keepGenericsInformation;
	}

	public String getRepackagedPrefix(String internalName) {
		String repackagedPrefix = "";
		int lastSeparatorIndex = internalName.lastIndexOf(47);
		if (lastSeparatorIndex != -1) {
			repackagedPrefix = internalName.substring(0, lastSeparatorIndex);
			repackagedPrefix = this.repackages.get(repackagedPrefix);
			if (repackagedPrefix.length() > 0) {
				repackagedPrefix = repackagedPrefix + "/";
			}
		}

		return repackagedPrefix;
	}

	public int getKeepInnerClassInfo() {
		return this.keepInnerClassInfo;
	}

	public String getRepackagedName(ClassHierarchyNode node) {
		String internalName = node.getInternalName();
		int lastSeparatorIndex = internalName.lastIndexOf(47);
		if (lastSeparatorIndex != -1) {
			String repackagedPrefix = this.getRepackagedPrefix(internalName);
			String simpleName = internalName.substring(lastSeparatorIndex + 1);
			String repackagedName = repackagedPrefix + simpleName;
			return repackagedName.equals(internalName) ? null : repackagedName;
		} else {
			return null;
		}
	}

	private String getInnerClassSeparator(String internalName) {
		int separatorIndex = internalName.lastIndexOf("$");
		String separator;
		if (separatorIndex > -1) {
			separator = internalName.substring(separatorIndex, separatorIndex + 1);
		} else {
			separator = "";
		}

		return separator;
	}

	public ArrayList<String> getCandidateClassNames(String newClassName, String oldPackageName) {
		ArrayList<String> candidateClassNames = new ArrayList<>();
		if (newClassName != null && this.invertedRepackages != null) {
			String repackagedPackage;
			if (oldPackageName.length() > 0) {
				repackagedPackage = this.repackages.get(oldPackageName);
			} else {
				repackagedPackage = oldPackageName;
			}

			int lastSeparatorIndex = newClassName.lastIndexOf(47);
			String simpleName = newClassName.substring(lastSeparatorIndex + 1);
			List<String> candidatePackages = this.invertedRepackages.get(repackagedPackage);
			if (candidatePackages != null) {
				for (int i = 0; i < candidatePackages.size(); i++) {
					String candidatePackage = candidatePackages.get(i);
					candidateClassNames.add(candidatePackage + "/" + simpleName);
				}
			} else {
				candidateClassNames.add(newClassName);
			}
		}

		return candidateClassNames;
	}

	public abstract String generateClassNameWithPrefix(
		ClassHierarchyNode node,
		String prefix,
		HashMap<ClassHierarchyNode, String> ancestorNames,
		String className,
		ExcludeElement excludeElement,
		boolean isFromFilesystem
	);

	public final String computeNewClassName(ClassHierarchyNode node, HashMap<ClassHierarchyNode, String> ancestorNames) {
		ExcludeElement excludeElement = null;
		String prefix = "";
		String className = node.getInternalName();
		ClassNode classNode = node.getClassNodeIfWorkspace();
		if (this.keepInnerClassInfo == 0 && node.hasSuperClass() && node.hasSuperClassLink()) {
			ClassHierarchyNode superClassNode = node.getSuperClassLink();
			String superClassInternalName = superClassNode.getInternalName();
			if (this.classNodesByInternalName.get(superClassInternalName) == null) {
				throw new ProcessingException(
					"Missing outer class : " + GlobalUtils.internalToBinaryName(superClassInternalName) + "' for '" + GlobalUtils.internalToBinaryName(className) + "'"
				);
			}

			if (ancestorNames != null) {
				for (ClassHierarchyNode ancestorNode : ancestorNames.keySet()) {
					ClassNode ancestorClassNode = ancestorNode.getClassNodeIfWorkspace();
					if (!this.newClassNames.containsKey(ancestorNode.getInternalName()) && !this.excludedClasses.contains(ancestorClassNode)) {
						return null;
					}
				}
			}

			String superClassNewName;
			Object newNameLookup;
			if ((newNameLookup = this.newClassNames.get(superClassInternalName)) == null) {
				if (!this.excludedClasses.contains(superClassNode.getClassNodeIfWorkspace())) {
					return null;
				}

				superClassNewName = superClassInternalName;
			} else {
				superClassNewName = (String)newNameLookup;
			}

			String innerClassSimpleName = classNode.getInnerClassSimpleName();
			if (innerClassSimpleName != null) {
				int superClassNameLength = superClassInternalName.length();
				int classNameLength = className.length();
				int innerClassNameLength = innerClassSimpleName.length();
				if (classNameLength - innerClassNameLength > superClassNameLength) {
					String separator = className.substring(superClassNameLength, classNameLength - innerClassNameLength);
					prefix = superClassNewName + separator;
					className = innerClassSimpleName;
				} else {
					String fallbackSeparator = this.getInnerClassSeparator(className);
					prefix = superClassNewName + fallbackSeparator;
					className = className.substring(superClassInternalName.length() + 1);
				}
			} else {
				String innerClassSeparator = this.getInnerClassSeparator(className);
				prefix = superClassNewName + innerClassSeparator;
				className = className.substring(superClassInternalName.length() + 1);
			}
		} else {
			excludeElement = this.defaultExcludes.getExcludeElement(node.getClassNodeIfWorkspace());
			if (excludeElement != null) {
				String namePrefix = (String)excludeElement.getPrefix();
				String nameSuffix = (String)excludeElement.getSuffix();
				ClassNode linkedClass = (ClassNode)excludeElement.getLinkedClass();
				if (linkedClass != null) {
					String linkedClassInternalName = linkedClass.getInternalName();
					String linkedClassNewName;
					if ((linkedClassNewName = this.newClassNames.get(linkedClassInternalName)) == null) {
						if (!this.excludedClasses.contains(linkedClass)) {
							return null;
						}

						String candidateNewName = this.getRepackagedPrefix(node.getClassNodeIfWorkspace().getInternalName()) + namePrefix + getSimpleName(linkedClassInternalName) + nameSuffix;
						AtomicInt clashCode = new AtomicInt(0);
						if (!this.validateDerivedName(className, candidateNewName, true, classNode.isFromFilesystem(), clashCode)) {
							throw new ProcessingException(
								"Error renaming "
									+ node.getClassNodeIfWorkspace().getSourcePath()
									+ ". Check <link> search paths. Code="
									+ clashCode.get()
									+ " : "
									+ namePrefix
									+ " : "
									+ candidateNewName
									+ " : "
									+ nameSuffix
									+ " : "
									+ className
									+ " (1)"
							);
						}

						this.generatedNames.add(candidateNewName);
						if (!classNode.isFromFilesystem()) {
							this.nonFilesystemNames.add(candidateNewName.toLowerCase());
						}

						this.lowercasedNames.add(candidateNewName.toLowerCase());
						return candidateNewName;
					}

					String candidateNewNameFromLinkedRename = this.getRepackagedPrefix(node.getClassNodeIfWorkspace().getInternalName()) + namePrefix + getSimpleName(linkedClassNewName) + nameSuffix;
					AtomicInt clashCodeFromLinkedRename = new AtomicInt(0);
					if (!this.validateDerivedName(className, candidateNewNameFromLinkedRename, true, classNode.isFromFilesystem(), clashCodeFromLinkedRename)) {
						throw new ProcessingException(
							"Error renaming "
								+ node.getClassNodeIfWorkspace().getSourcePath()
								+ ". Check <link> search paths. Code="
								+ clashCodeFromLinkedRename.get()
								+ " : "
								+ namePrefix
								+ " : "
								+ candidateNewNameFromLinkedRename
								+ " : "
								+ nameSuffix
								+ " : "
								+ className
								+ " (2)"
						);
					}

					this.generatedNames.add(candidateNewNameFromLinkedRename);
					if (!classNode.isFromFilesystem()) {
						this.nonFilesystemNames.add(candidateNewNameFromLinkedRename.toLowerCase());
						this.lowercasedNames.add(candidateNewNameFromLinkedRename.toLowerCase());
					}

					return candidateNewNameFromLinkedRename;
				}
			}
		}

		String newClassName = this.generateClassNameWithPrefix(node, prefix, ancestorNames, className, excludeElement, classNode.isFromFilesystem());
		this.generatedNames.add(newClassName);
		if (!classNode.isFromFilesystem()) {
			this.nonFilesystemNames.add(newClassName.toLowerCase());
		}

		this.lowercasedNames.add(newClassName.toLowerCase());
		return newClassName;
	}
}
