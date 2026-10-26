package com.zelix.tools.renamer.naming;

import com.zelix.tools.renamer.dictionary.JavaKeywords;
import java.util.ArrayList;

public class AlphaNameGenerator extends JavaKeywords {
	private ArrayList<String> usedNames = new ArrayList<>(1024);
	private char[] nameChars = new char[6];
	private int lowerCaseOnly;
	private String prefix;
	private final int firstCharCount;
	private final int followingCharCount;

	public AlphaNameGenerator(int lowerCaseOnly, String prefix) {
		this.lowerCaseOnly = lowerCaseOnly;
		this.prefix = prefix;
		if (lowerCaseOnly == 0) {
			this.firstCharCount = 52;
			this.followingCharCount = 62;
		} else {
			this.firstCharCount = 26;
			this.followingCharCount = 36;
		}
	}

	public AlphaNameGenerator(int lowerCaseOnly) {
		this(lowerCaseOnly, null);
	}

	public final String generateName(int index) {
		String candidateName;
		do {
			candidateName = this.generateNameOrNull(index);
		} while (candidateName == null);

		return candidateName;
	}

	public final String generateNameOrNull(int index) {
		if (index < this.usedNames.size()) {
			return this.usedNames.get(index);
		} else {
			int nameLength = 0;
			nameLength++;
			this.nameChars[0] = this.letterForIndex(index % this.firstCharCount);

			for (int remainingIndex = index / this.firstCharCount; remainingIndex > 0; remainingIndex /= this.followingCharCount) {
				this.nameChars[nameLength++] = this.alphaNumForIndex(remainingIndex % this.followingCharCount);
			}

			String candidateName = new String(this.nameChars, 0, nameLength);
			if (this.prefix != null) {
				candidateName = this.prefix + candidateName;
			}

			if (JavaKeywords.keywords.contains(candidateName)) {
				candidateName = null;
			}

			if (index == this.usedNames.size()) {
				this.usedNames.add(candidateName);
			}

			return candidateName;
		}
	}

	public final char letterForIndex(int index) {
		return index <= 25 ? (char)(97 + index) : (char)(39 + index);
	}

	public final char alphaNumForIndex(int index) {
		char nameChar;
		if (this.lowerCaseOnly == 0) {
			if (index <= 25) {
				nameChar = (char)(97 + index);
			} else if (index <= 51) {
				nameChar = (char)(39 + index);
			} else {
				nameChar = (char)(index - 4);
			}
		} else if (index <= 25) {
			nameChar = (char)(97 + index);
		} else {
			nameChar = (char)(22 + index);
		}

		return nameChar;
	}
}
