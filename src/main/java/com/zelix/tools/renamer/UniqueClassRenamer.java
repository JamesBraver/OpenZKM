package com.zelix.tools.renamer;

import com.zelix.enums.MixCaseType;
import com.zelix.tools.renamer.dictionary.DictionaryGenerator;
import com.zelix.tools.renamer.packages.PackageTreeNode;
import com.zelix.utils.collections.MultiMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UniqueClassRenamer extends SimpleClassRenamer {
	private DictionaryGenerator dictionaryGenerator;

	public UniqueClassRenamer(
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
		super(classStruct, useAsciiCharactersOnly, legalIdentifiers, newNamesPrefix, keepInnerClassInfo, keepGenericsInformation, randomizeObfuscation, mixedCaseClassNames, repackages, invertedRepackages, packageTreeNodes, overrideNames);
	}

	@Override
	public DictionaryGenerator createDictionaryGenerator(char[] nonFilesystemFirstChars, char[] nonFilesystemSubsequentChars, char[] filesystemFirstChars, char[] filesystemSubsequentChars, List<String> overrideNames) {
		if (this.dictionaryGenerator == null) {
			this.dictionaryGenerator = new DictionaryGenerator(nonFilesystemFirstChars, nonFilesystemSubsequentChars, filesystemFirstChars, filesystemSubsequentChars, overrideNames, super.randomizeObfuscation);
		}

		return this.dictionaryGenerator;
	}
}
