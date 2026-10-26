package com.zelix.tools.renamer.packages;

import com.zelix.enums.MixCaseType;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.repository.ClasspathScanner;
import com.zelix.tools.excludes.DefaultExcludes;
import com.zelix.tools.renamer.dictionary.DictionaryGenerator;
import com.zelix.tools.renamer.naming.NameGenerator;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.MultiMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimplePackageNameGenerator extends AbstractPackageNameGenerator {
	private final Map<String, DictionaryGenerator> dictionaryGenerators = GlobalUtils.newHashMap();
	private final String newNamesPrefix;
	private List<String> overrideNames;
	private char[] lowerFirstChars;
	private char[] lowerRestChars;
	private char[] mixedFirstChars;
	private char[] mixedRestChars;

	public SimplePackageNameGenerator(ClasspathScanner classpathScanner, boolean useAsciiCharactersOnly, boolean legalIdentifiers, boolean randomizeObfuscation, String newNamesPrefix, MixCaseType mixedCaseType, List<String> overrideNames) {
		super(classpathScanner, mixedCaseType, randomizeObfuscation);
		this.newNamesPrefix = newNamesPrefix;
		this.overrideNames = overrideNames;
		switch (mixedCaseType.getType()) {
			case 0:
				if (!useAsciiCharactersOnly) {
					this.lowerFirstChars = NameGenerator.UNICODES;
					this.lowerRestChars = NameGenerator.UNICODES;
					this.mixedFirstChars = NameGenerator.UNICODES;
					this.mixedRestChars = NameGenerator.UNICODES;
				} else if (legalIdentifiers) {
					this.lowerFirstChars = NameGenerator.LETTERS_;
					this.lowerRestChars = NameGenerator.LETTERS_NUMBERS_;
					this.mixedFirstChars = NameGenerator.LETTERS_;
					this.mixedRestChars = NameGenerator.LETTERS_NUMBERS_;
				} else {
					this.lowerFirstChars = NameGenerator.NUMBERS;
					this.lowerRestChars = NameGenerator.LETTERS_NUMBERS_;
					this.mixedFirstChars = NameGenerator.NUMBERS;
					this.mixedRestChars = NameGenerator.LETTERS_NUMBERS_;
				}
				break;
			case 1:
				if (!useAsciiCharactersOnly) {
					this.lowerFirstChars = NameGenerator.LOWER_UNICODES;
					this.lowerRestChars = NameGenerator.LOWER_UNICODES;
					this.mixedFirstChars = NameGenerator.LOWER_UNICODES;
					this.mixedRestChars = NameGenerator.LOWER_UNICODES;
				} else if (legalIdentifiers) {
					this.lowerFirstChars = NameGenerator.LOWER_LETTERS_;
					this.lowerRestChars = NameGenerator.LOWER_LETTERS_NUMBERS_;
					this.mixedFirstChars = NameGenerator.LOWER_LETTERS_;
					this.mixedRestChars = NameGenerator.LOWER_LETTERS_NUMBERS_;
				} else {
					this.lowerFirstChars = NameGenerator.NUMBERS;
					this.lowerRestChars = NameGenerator.LOWER_LETTERS_NUMBERS_;
					this.mixedFirstChars = NameGenerator.NUMBERS;
					this.mixedRestChars = NameGenerator.LOWER_LETTERS_NUMBERS_;
				}
				break;
			case 2:
				if (!useAsciiCharactersOnly) {
					this.lowerFirstChars = NameGenerator.LOWER_UNICODES;
					this.lowerRestChars = NameGenerator.LOWER_UNICODES;
					this.mixedFirstChars = NameGenerator.UNICODES;
					this.mixedRestChars = NameGenerator.UNICODES;
				} else if (legalIdentifiers) {
					this.lowerFirstChars = NameGenerator.LOWER_LETTERS_;
					this.lowerRestChars = NameGenerator.LOWER_LETTERS_NUMBERS_;
					this.mixedFirstChars = NameGenerator.LETTERS_;
					this.mixedRestChars = NameGenerator.LETTERS_NUMBERS_;
				} else {
					this.lowerFirstChars = NameGenerator.NUMBERS;
					this.lowerRestChars = NameGenerator.LOWER_LETTERS_NUMBERS_;
					this.mixedFirstChars = NameGenerator.NUMBERS;
					this.mixedRestChars = NameGenerator.LETTERS_NUMBERS_;
				}
		}
	}

	@Override
	public String generatePackageName(
		String parentPackageName,
		boolean isFromFilesystem,
		boolean requireLowercaseFirstChar,
		MultiMap<String, String> newPackageNames,
		DualKeyMap<String, String, ClassNode> excludedClasses,
		Set<String> excludedClassNames,
		ChangeLogContext changeLogContext,
		DefaultExcludes defaultExcludes
	) {
		DictionaryGenerator dictionaryGenerator = this.dictionaryGenerators.get(parentPackageName);
		if (dictionaryGenerator == null) {
			dictionaryGenerator = new DictionaryGenerator(this.lowerFirstChars, this.lowerRestChars, this.mixedFirstChars, this.mixedRestChars, this.overrideNames, this.isRandomizeObfuscation());
			this.dictionaryGenerators.put(parentPackageName, dictionaryGenerator);
		}

		boolean lowercaseFirstCharRequired = requireLowercaseFirstChar && (this.newNamesPrefix == null || this.newNamesPrefix.length() == 0 || Character.isLetter(this.newNamesPrefix.charAt(0)));

		String candidateName;
		do {
			candidateName = this.nextName(dictionaryGenerator, lowercaseFirstCharRequired ? false : isFromFilesystem);
			if (lowercaseFirstCharRequired && !Character.isLowerCase(candidateName.charAt(0))) {
				StringBuilder nameBuilder = new StringBuilder(candidateName);
				nameBuilder.setCharAt(0, Character.toLowerCase(candidateName.charAt(0)));
				candidateName = nameBuilder.toString();
			}
		} while (!this.validateGeneratedName(candidateName, parentPackageName, lowercaseFirstCharRequired, newPackageNames, excludedClasses, excludedClassNames, changeLogContext, defaultExcludes));

		return candidateName;
	}

	private String nextName(DictionaryGenerator dictionaryGenerator, boolean useMixedCase) {
		return dictionaryGenerator.next(this.newNamesPrefix, useMixedCase);
	}
}
