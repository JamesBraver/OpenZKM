package com.zelix.utils;

import java.io.StringWriter;
import java.util.StringTokenizer;

public class StringUtils {
	public static int skipWhitespace(String str, int fromIndex) {
		int stringLength = str.length();
		if (stringLength == 0) {
			return -1;
		} else {
			int currentIndex = fromIndex;

			for (char currentChar = str.charAt(fromIndex); Character.isWhitespace(currentChar); currentChar = str.charAt(currentIndex)) {
				if (++currentIndex == stringLength) {
					return -1;
				}
			}

			return currentIndex;
		}
	}

	public static String justificateString(String str, int justification, int width, int padChar) {
		int stringLength = str.length();
		StringBuilder builder;
		switch (justification) {
			case 76:
				builder = new StringBuilder(str);
				int rightPaddingCount = width - stringLength;

				for (int i = 0; i < rightPaddingCount; i++) {
					builder.append((char)padChar);
				}
				break;
			case 82:
				builder = new StringBuilder();
				int leftPaddingCount = width - stringLength;

				for (int paddingIndex = 0; paddingIndex < leftPaddingCount; paddingIndex++) {
					builder.append((char)padChar);
				}

				builder.append(str);
				break;
			default:
				throw new IllegalArgumentException("Invalid justification");
		}

		return builder.toString().substring(0, width);
	}

	public static int countOccurrences(String str, String sub) {
		if (str != null && str.length() > 0 && sub != null && sub.length() > 0) {
			int substringLength = sub.length();
			int occurrenceCount = -1;
			int searchIndex = 0 - substringLength;

			do {
				searchIndex += substringLength;
				searchIndex = str.indexOf(sub, searchIndex);
				occurrenceCount++;
			} while (searchIndex != -1);

			return occurrenceCount;
		} else {
			return 0;
		}
	}

	public static String escapeJavaString(String str) {
		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < str.length(); i++) {
			char currentChar = str.charAt(i);
			switch (currentChar) {
				case '\u0000':
					break;
				case '\b':
					builder.append("\\b");
					break;
				case '\t':
					builder.append("\\t");
					break;
				case '\n':
					builder.append("\\n");
					break;
				case '\f':
					builder.append("\\f");
					break;
				case '\r':
					builder.append("\\r");
					break;
				case '"':
					builder.append("\\\"");
					break;
				case '\'':
					builder.append("\\'");
					break;
				case '\\':
					builder.append("\\\\");
					break;
				default:
					if (currentChar >= ' ' && currentChar <= '~') {
						builder.append(currentChar);
					} else {
						builder.append("\\u" + justificateString(Integer.toHexString(currentChar), 82, 4, 48));
					}
			}
		}

		return builder.toString();
	}

	private static boolean matchWildcardHelper(String value, int valueIndex, String[] patternTokens, int tokenIndex) {
		if (valueIndex >= value.length()) {
			if (tokenIndex >= patternTokens.length) {
				return true;
			} else {
				while (tokenIndex < patternTokens.length) {
					if (!patternTokens[tokenIndex].equals("*")) {
						return false;
					}

					tokenIndex++;
				}

				return true;
			}
		} else if (tokenIndex >= patternTokens.length) {
			return false;
		} else {
			String patternToken = patternTokens[tokenIndex];
			if (!patternToken.equals("*")) {
				if (!value.regionMatches(valueIndex, patternToken, 0, patternToken.length())) {
					return false;
				} else {
					valueIndex += patternToken.length();
					return matchWildcardHelper(value, valueIndex, patternTokens, tokenIndex + 1);
				}
			} else {
				while (patternToken.equals("*")) {
					if (++tokenIndex >= patternTokens.length) {
						break;
					}

					patternToken = patternTokens[tokenIndex];
				}

				if (patternToken.equals("*")) {
					return true;
				} else {
					while (valueIndex < value.length()) {
						int matchIndex = value.indexOf(patternToken, valueIndex);
						if (matchIndex == -1) {
							return false;
						}

						if (matchWildcardHelper(value, matchIndex + patternToken.length(), patternTokens, tokenIndex + 1)) {
							return true;
						}

						valueIndex = matchIndex + 1;
					}

					return false;
				}
			}
		}
	}

	public static int countOccurrences(String str, char ch) {
		if (str != null && str.length() > 0) {
			char[] characterArray = str.toCharArray();
			int occurrenceCount = 0;
			char[] characters = characterArray;
			int characterCount = characterArray.length;

			for (int i = 0; i < characterCount; i++) {
				if (characters[i] == ch) {
					occurrenceCount++;
				}
			}

			return occurrenceCount;
		} else {
			return 0;
		}
	}

	public static boolean matchesWildcard(String value, String pattern) {
		if (!pattern.equals("*") && !value.equals(pattern)) {
			if (value.indexOf("*") == -1 && pattern.indexOf("*") == -1) {
				return value.equals(pattern);
			} else {
				StringTokenizer tokenizer = new StringTokenizer(pattern, "*", true);
				String[] tokens = new String[tokenizer.countTokens()];
				int tokenIndex = 0;

				while (tokenizer.hasMoreTokens()) {
					tokens[tokenIndex++] = tokenizer.nextToken();
				}

				return matchWildcardHelper(value, 0, tokens, 0);
			}
		} else {
			return true;
		}
	}

	public static int indexOfAny(String str, int fromIndex, char[] chars) {
		int stringLength = str.length();

		for (int charIndex = fromIndex; charIndex < stringLength; charIndex++) {
			char currentChar = str.charAt(charIndex);

			for (int i = 0; i < chars.length; i++) {
				if (currentChar == chars[i]) {
					return charIndex;
				}
			}
		}

		return -1;
	}

	public static String replace(String str, String target, String replacement) {
		if (str == null) {
			throw new IllegalArgumentException("Null String");
		} else {
			int stringLength = str.length();
			StringWriter writer = new StringWriter(stringLength);
			char[] characters = str.toCharArray();
			int targetLength = target.length();
			int searchIndex = 0;

			int matchIndex;
			while (searchIndex < stringLength && (matchIndex = str.indexOf(target, searchIndex)) > -1) {
				writer.write(characters, searchIndex, matchIndex - searchIndex);
				writer.write(replacement);
				searchIndex = matchIndex + targetLength;
			}

			writer.write(characters, searchIndex, stringLength - searchIndex);
			return writer.toString();
		}
	}

	public static String repeat(int count, int ch) {
		StringBuilder builder = new StringBuilder(count);

		for (int i = 0; i < count; i++) {
			builder.append((char)ch);
		}

		return builder.toString();
	}
}
