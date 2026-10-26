package com.zelix.tools.renamer;

import com.zelix.enums.MixCaseType;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.tools.excludes.ExcludeElement;
import com.zelix.tools.renamer.dictionary.DictionaryGenerator;
import com.zelix.tools.renamer.naming.NameGenerator;
import com.zelix.tools.renamer.packages.PackageTreeNode;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.workspace.ClassHierarchyNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleClassRenamer extends ClassRenamer {
	private Map<String, DictionaryGenerator> packageDictionaries = GlobalUtils.newHashMap();
	private Map<String, DictionaryGenerator> innerClassDictionaries = GlobalUtils.newHashMap();
	private char[] nonFilesystemFirstChars;
	private char[] nonFilesystemSubsequentChars;
	private char[] filesystemFirstChars;
	private char[] filesystemSubsequentChars;
	private String newNamesPrefix;
	private List<String> overrideNames;

	public SimpleClassRenamer(
		ClassStruct classStruct,
		boolean useAsciiCharactersOnly,
		boolean legalIdentifiers,
		String newNamesPrefix,
		int keepInnerClassInfo,
		int keepGenericsInformation,
		boolean randomizeObfuscation,
		MixCaseType mixedCaseClassNames,
		HashMap<String, String> repackages,
		MultiMap<String, String> invertedRepackages,
		Map<String, PackageTreeNode> packageTreeNodes,
		List<String> overrideNames
	) {
		super(classStruct, repackages, invertedRepackages, packageTreeNodes, keepInnerClassInfo, keepGenericsInformation, randomizeObfuscation);
		switch (mixedCaseClassNames.getType()) {
			case 0:
				if (!useAsciiCharactersOnly) {
					this.nonFilesystemFirstChars = (char[])NameGenerator.UNICODES.clone();
					this.nonFilesystemSubsequentChars = (char[])NameGenerator.UNICODES.clone();
					this.filesystemFirstChars = (char[])NameGenerator.UNICODES.clone();
					this.filesystemSubsequentChars = (char[])NameGenerator.UNICODES.clone();
				} else if (legalIdentifiers) {
					this.nonFilesystemFirstChars = (char[])NameGenerator.LETTERS_.clone();
					this.nonFilesystemSubsequentChars = (char[])NameGenerator.LETTERS_NUMBERS_.clone();
					this.filesystemFirstChars = (char[])NameGenerator.LETTERS_.clone();
					this.filesystemSubsequentChars = (char[])NameGenerator.LETTERS_NUMBERS_.clone();
				} else {
					this.nonFilesystemFirstChars = (char[])NameGenerator.NUMBERS.clone();
					this.nonFilesystemSubsequentChars = (char[])NameGenerator.LETTERS_NUMBERS_.clone();
					this.filesystemFirstChars = (char[])NameGenerator.NUMBERS.clone();
					this.filesystemSubsequentChars = (char[])NameGenerator.LETTERS_NUMBERS_.clone();
				}
				break;
			case 1:
				if (!useAsciiCharactersOnly) {
					this.nonFilesystemFirstChars = (char[])NameGenerator.LOWER_UNICODES.clone();
					this.nonFilesystemSubsequentChars = (char[])NameGenerator.LOWER_UNICODES.clone();
					this.filesystemFirstChars = (char[])NameGenerator.LOWER_UNICODES.clone();
					this.filesystemSubsequentChars = (char[])NameGenerator.LOWER_UNICODES.clone();
				} else if (legalIdentifiers) {
					this.nonFilesystemFirstChars = (char[])NameGenerator.LOWER_LETTERS_.clone();
					this.nonFilesystemSubsequentChars = (char[])NameGenerator.LOWER_LETTERS_NUMBERS_.clone();
					this.filesystemFirstChars = (char[])NameGenerator.LOWER_LETTERS_.clone();
					this.filesystemSubsequentChars = (char[])NameGenerator.LOWER_LETTERS_NUMBERS_.clone();
				} else {
					this.nonFilesystemFirstChars = (char[])NameGenerator.NUMBERS.clone();
					this.nonFilesystemSubsequentChars = (char[])NameGenerator.LOWER_LETTERS_NUMBERS_.clone();
					this.filesystemFirstChars = (char[])NameGenerator.NUMBERS.clone();
					this.filesystemSubsequentChars = (char[])NameGenerator.LOWER_LETTERS_NUMBERS_.clone();
				}
				break;
			case 2:
				if (!useAsciiCharactersOnly) {
					this.nonFilesystemFirstChars = (char[])NameGenerator.LOWER_UNICODES.clone();
					this.nonFilesystemSubsequentChars = (char[])NameGenerator.LOWER_UNICODES.clone();
					this.filesystemFirstChars = (char[])NameGenerator.UNICODES.clone();
					this.filesystemSubsequentChars = (char[])NameGenerator.UNICODES.clone();
				} else if (legalIdentifiers) {
					this.nonFilesystemFirstChars = (char[])NameGenerator.LOWER_LETTERS_.clone();
					this.nonFilesystemSubsequentChars = (char[])NameGenerator.LOWER_LETTERS_NUMBERS_.clone();
					this.filesystemFirstChars = (char[])NameGenerator.LETTERS_.clone();
					this.filesystemSubsequentChars = (char[])NameGenerator.LETTERS_NUMBERS_.clone();
				} else {
					this.nonFilesystemFirstChars = (char[])NameGenerator.NUMBERS.clone();
					this.nonFilesystemSubsequentChars = (char[])NameGenerator.LOWER_LETTERS_NUMBERS_.clone();
					this.filesystemFirstChars = (char[])NameGenerator.NUMBERS.clone();
					this.filesystemSubsequentChars = (char[])NameGenerator.LETTERS_NUMBERS_.clone();
				}
		}

		this.newNamesPrefix = newNamesPrefix;
		this.overrideNames = overrideNames;
		if (classStruct.isRandomizeObfuscation()) {
			GlobalUtils.shuffleWithFixedSeed(this.nonFilesystemSubsequentChars);
			GlobalUtils.shuffleWithFixedSeed(this.filesystemSubsequentChars);
		}
	}

	private String nextGeneratedName(String repackagedPrefix, String prefix, String className, HashMap<ClassHierarchyNode, String> ancestorNames, boolean useFilesystemAlphabet, boolean randomizeObfuscation) {
		if (prefix.equals("")) {
			DictionaryGenerator packageDictionary = this.packageDictionaries.get(repackagedPrefix);
			if (packageDictionary == null) {
				packageDictionary = this.createDictionaryGenerator(this.nonFilesystemFirstChars, this.nonFilesystemSubsequentChars, this.filesystemFirstChars, this.filesystemSubsequentChars, this.overrideNames);
				this.packageDictionaries.put(repackagedPrefix, packageDictionary);
			}

			return packageDictionary.next(this.newNamesPrefix, useFilesystemAlphabet);
		} else {
			DictionaryGenerator innerClassDictionary = this.innerClassDictionaries.get(prefix);
			if (innerClassDictionary == null) {
				innerClassDictionary = this.createDictionaryGenerator(this.nonFilesystemFirstChars, this.nonFilesystemSubsequentChars, this.filesystemFirstChars, this.filesystemSubsequentChars, this.overrideNames);
				this.innerClassDictionaries.put(prefix, innerClassDictionary);
			}

			String generatedName;
			do {
				generatedName = innerClassDictionary.next(this.newNamesPrefix, useFilesystemAlphabet);
				if (generatedName != null && Options.ZKM_UNIQUE_INNER_CLASS) {
					generatedName = generatedName + "__";
				}
			} while (generatedName == null || ancestorNames != null && ancestorNames.containsValue(generatedName));

			return generatedName;
		}
	}

	@Override
	public String generateClassName(ClassHierarchyNode node, boolean isFromFilesystem) {
		ClassNode classNode = node.getClassNodeIfWorkspace();
		ExcludeElement excludeElement = super.defaultExcludes.getExcludeElement(classNode);
		String namePrefix;
		String nameSuffix;
		if (excludeElement != null) {
			namePrefix = (String)excludeElement.getPrefix();
			nameSuffix = (String)excludeElement.getSuffix();
		} else {
			namePrefix = "";
			nameSuffix = "";
		}

		return this.generateValidClassName("", node.getInternalName(), namePrefix, nameSuffix, null, isFromFilesystem, classNode.hasFxmlUsageFlag(), super.randomizeObfuscation);
	}

	private String generateValidClassName(
		String prefix, String className, String namePrefix, String nameSuffix, HashMap<ClassHierarchyNode, String> ancestorNames, boolean isFromFilesystem, boolean hasFxmlUsageFlag, boolean randomizeObfuscation
	) {
		String repackagedPrefix = "";
		if (prefix.equals("")) {
			repackagedPrefix = this.getRepackagedPrefix(className);
		}

		boolean uppercaseStartRequired = hasFxmlUsageFlag
			&& prefix.length() == 0
			&& namePrefix.length() == 0
			&& (this.newNamesPrefix == null || this.newNamesPrefix.length() == 0 || !Character.isUpperCase(this.newNamesPrefix.charAt(0)) && Character.isLetter(this.newNamesPrefix.charAt(0)));

		String newClassName;
		do {
			boolean useFilesystemAlphabet = uppercaseStartRequired ? true : isFromFilesystem;
			String generatedName = this.nextGeneratedName(repackagedPrefix, prefix, className, ancestorNames, useFilesystemAlphabet, randomizeObfuscation);
			if (uppercaseStartRequired && !Character.isUpperCase(generatedName.charAt(0)) && Character.isLetter(generatedName.charAt(0))) {
				StringBuilder nameBuffer = new StringBuilder(generatedName);
				nameBuffer.setCharAt(0, Character.toUpperCase(generatedName.charAt(0)));
				generatedName = nameBuffer.toString();
			}

			newClassName = repackagedPrefix + prefix + namePrefix + generatedName + nameSuffix;
		} while (!this.validateGeneratedName(className, newClassName, isFromFilesystem, uppercaseStartRequired));

		return newClassName;
	}

	@Override
	public String generateClassNameWithPrefix(ClassHierarchyNode node, String prefix, HashMap<ClassHierarchyNode, String> ancestorNames, String className, ExcludeElement excludeElement, boolean isFromFilesystem) {
		String namePrefix;
		String nameSuffix;
		if (excludeElement != null) {
			namePrefix = (String)excludeElement.getPrefix();
			nameSuffix = (String)excludeElement.getSuffix();
			if (excludeElement.getLinkedClass() == null) {
				namePrefix = namePrefix + "zzzz";
			}
		} else {
			namePrefix = "";
			nameSuffix = "";
		}

		return this.generateValidClassName(prefix, className, namePrefix, nameSuffix, ancestorNames, isFromFilesystem, node.getClassNodeIfWorkspace().hasFxmlUsageFlag(), super.randomizeObfuscation);
	}

	public DictionaryGenerator createDictionaryGenerator(char[] nonFilesystemFirstChars, char[] nonFilesystemSubsequentChars, char[] filesystemFirstChars, char[] filesystemSubsequentChars, List<String> overrideNames) {
		return new DictionaryGenerator(nonFilesystemFirstChars, nonFilesystemSubsequentChars, filesystemFirstChars, filesystemSubsequentChars, overrideNames, super.randomizeObfuscation);
	}
}
