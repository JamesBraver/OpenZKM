package com.zelix.utils;

public class WildcardMaskPatternUtils {
	public static String toClassNamePattern(String classNamePattern) {
		if (classNamePattern.equals("***")) {
			return "*.*";
		} else {
			String normalizedPattern = normalizeWildcard(classNamePattern.replace('?', '*').replace('%', '*'));
			return normalizedPattern.equals("*") ? "*.*" : normalizedPattern;
		}
	}

	public static String toPackageNamePattern(String packageNamePattern) {
		if (packageNamePattern.equals("***")) {
			return "*.";
		} else {
			String normalizedPattern = normalizeWildcard(packageNamePattern.replace('?', '*'));
			if (normalizedPattern.equals("*")) {
				return "*.";
			} else {
				if (normalizedPattern.charAt(normalizedPattern.length() - 1) != '.') {
					normalizedPattern = normalizedPattern + '.';
				}

				return normalizedPattern;
			}
		}
	}

	public static String toParameterTypePattern(String typePattern) {
		if (typePattern.equals("...")) {
			return "*";
		} else if (typePattern.equals("***")) {
			return "?";
		} else if (typePattern.indexOf("%") > -1) {
			return "?";
		} else {
			return typePattern.indexOf(60) > -1 ? "?" : normalizeWildcard(typePattern.replace('?', '*'));
		}
	}

	public static String toMethodNamePattern(String methodNamePattern) {
		return !methodNamePattern.equals("<init>") && !methodNamePattern.equals("***") && methodNamePattern.indexOf(46) <= -1 ? normalizeWildcard(methodNamePattern.replace('?', '*')) : "<init>";
	}

	public static String toFieldNamePattern(String fieldNamePattern) {
		return normalizeWildcard(fieldNamePattern.replace('?', '*'));
	}

	public static String normalizeWildcard(String wildcardPattern) {
		StringBuilder builder = new StringBuilder();
		boolean previousWasAsterisk = false;
		char[] characters = wildcardPattern.toCharArray();

		for (int charIndex = 0; charIndex < characters.length; charIndex++) {
			char currentChar = characters[charIndex];
			if (currentChar == '*') {
				if (!previousWasAsterisk) {
					builder.append(currentChar);
					previousWasAsterisk = true;
				}
			} else if (currentChar == '.' && charIndex > 0 && charIndex == characters.length - 3 && characters[charIndex + 1] == '*' && characters[charIndex + 2] == '*') {
				builder.append('*');
				builder.append(currentChar);
			} else {
				builder.append(currentChar);
				previousWasAsterisk = false;
			}
		}

		return builder.toString();
	}

	public static String quoteIfNeeded(String value) {
		value = value.trim();
		if (value.indexOf(32) <= -1 && value.length() != 0) {
			return value;
		} else {
			StringBuilder builder = new StringBuilder(value.length() + 2);
			builder.append('"');
			builder.append(value);
			builder.append('"');
			return builder.toString();
		}
	}
}
