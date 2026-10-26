package com.zelix.exceptions;

import com.zelix.klassmaster.mapping.Token4;

public class MappingParserException extends RuntimeException {
	protected String lineSeparator = System.getProperty("line.separator", "\n");
	public Token4 currentToken;
	public int[][] expectedTokenSequences;
	public String[] tokenImages;

	public MappingParserException() {
	}

	public MappingParserException(Token4 currentToken, int[][] expectedTokenSequences, String[] tokenImages) {
		super(formatParseErrorMessage(currentToken, expectedTokenSequences, tokenImages));
		this.currentToken = currentToken;
		this.expectedTokenSequences = expectedTokenSequences;
		this.tokenImages = tokenImages;
	}

	public static String escapeString(String value) {
		StringBuffer escapedBuffer = new StringBuffer();

		for (int i = 0; i < value.length(); i++) {
			switch (value.charAt(i)) {
				case '\u0000':
					break;
				case '\b':
					escapedBuffer.append("\\b");
					break;
				case '\t':
					escapedBuffer.append("\\t");
					break;
				case '\n':
					escapedBuffer.append("\\n");
					break;
				case '\f':
					escapedBuffer.append("\\f");
					break;
				case '\r':
					escapedBuffer.append("\\r");
					break;
				case '"':
					escapedBuffer.append("\\\"");
					break;
				case '\'':
					escapedBuffer.append("\\'");
					break;
				case '\\':
					escapedBuffer.append("\\\\");
					break;
				default:
					char currentChar;
					if ((currentChar = value.charAt(i)) >= ' ' && currentChar <= '~') {
						escapedBuffer.append(currentChar);
					} else {
						String hexCode = "0000" + Integer.toString(currentChar, 16);
						escapedBuffer.append("\\u" + hexCode.substring(hexCode.length() - 4, hexCode.length()));
					}
			}
		}

		return escapedBuffer.toString();
	}

	private static String formatParseErrorMessage(Token4 currentToken, int[][] expectedTokenSequences, String[] tokenImages) {
		String lineSeparator = System.getProperty("line.separator", "\n");
		StringBuffer expectedBuffer = new StringBuffer();
		int maxSequenceLength = 0;

		for (int sequenceIndex = 0; sequenceIndex < expectedTokenSequences.length; sequenceIndex++) {
			if (maxSequenceLength < expectedTokenSequences[sequenceIndex].length) {
				maxSequenceLength = expectedTokenSequences[sequenceIndex].length;
			}

			for (int tokenIndex = 0; tokenIndex < expectedTokenSequences[sequenceIndex].length; tokenIndex++) {
				expectedBuffer.append(tokenImages[expectedTokenSequences[sequenceIndex][tokenIndex]]).append(' ');
			}

			if (expectedTokenSequences[sequenceIndex][expectedTokenSequences[sequenceIndex].length - 1] != 0) {
				expectedBuffer.append("...");
			}

			expectedBuffer.append(lineSeparator).append("    ");
		}

		String messageBuffer = "Encountered \"";
		Token4 tokenCursor = currentToken.next;

		for (int i = 0; i < maxSequenceLength; i++) {
			if (i != 0) {
				messageBuffer = messageBuffer + " ";
			}

			if (tokenCursor.kind == 0) {
				messageBuffer = messageBuffer + tokenImages[0];
				break;
			}

			messageBuffer = messageBuffer + " " + tokenImages[tokenCursor.kind];
			messageBuffer = messageBuffer + " \"";
			messageBuffer = messageBuffer + escapeString(tokenCursor.image);
			messageBuffer = messageBuffer + " \"";
			tokenCursor = tokenCursor.next;
		}

		messageBuffer = messageBuffer + "\" at line " + currentToken.next.beginLine + ", column " + currentToken.next.beginColumn;
		messageBuffer = messageBuffer + "." + lineSeparator;
		if (expectedTokenSequences.length == 1) {
			messageBuffer = messageBuffer + "Was expecting:" + lineSeparator + "    ";
		} else {
			messageBuffer = messageBuffer + "Was expecting one of:" + lineSeparator + "    ";
		}

		return messageBuffer + expectedBuffer.toString();
	}
}
