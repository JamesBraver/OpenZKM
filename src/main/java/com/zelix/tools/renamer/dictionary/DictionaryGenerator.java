package com.zelix.tools.renamer.dictionary;

import com.zelix.tools.renamer.naming.NameGenerator;
import com.zelix.utils.GlobalUtils;
import java.util.List;
import java.util.Set;

public class DictionaryGenerator {
	private final Set<String> used = GlobalUtils.newHashSet();
	private final NameGenerator nonMixedCaseNameGenerator;
	private final NameGenerator mixedCaseNameGenerator;

	public DictionaryGenerator(char[] nonMixedCaseInitialChars, char[] nonMixedCaseExtendedChars, char[] mixedCaseInitialChars, char[] mixedCaseExtendedChars, List<String> overrideNames, boolean randomizeObfuscation) {
		this.nonMixedCaseNameGenerator = new NameGenerator(nonMixedCaseInitialChars, nonMixedCaseExtendedChars, overrideNames, randomizeObfuscation);
		this.mixedCaseNameGenerator = new NameGenerator(mixedCaseInitialChars, mixedCaseExtendedChars, overrideNames, randomizeObfuscation);
	}

	public final String next(String prefix, boolean mixedCase) {
		NameGenerator nameGenerator;
		if (mixedCase) {
			nameGenerator = this.mixedCaseNameGenerator;
		} else {
			nameGenerator = this.nonMixedCaseNameGenerator;
		}

		String candidateName;
		do {
			if (prefix != null) {
				candidateName = nameGenerator.next(prefix);
			} else {
				candidateName = nameGenerator.nextName();
			}
		} while (!this.used.add(candidateName));

		return candidateName;
	}
}
