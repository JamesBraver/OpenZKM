package com.zelix.exceptions;

import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.script.ZKMScriptToken;
import com.zelix.klassmaster.script.ZKMScriptTokenList;

public class ParsingException extends RuntimeException implements ZKMScriptTokenList {
	protected String lineSeparator = Options.lineSeparator;
	protected boolean specialConstructor;
	public ZKMScriptToken currentToken;
	public int[][] expectedTokenSequences;
	public String[] tokenImages;
	public int statementKind;

	public ParsingException() {
		this.specialConstructor = false;
	}

	public ParsingException(ZKMScriptToken currentToken, int[][] expectedTokenSequences, String[] tokenImages, int statementKind) {
		super("");
		this.specialConstructor = true;
		this.currentToken = currentToken;
		this.expectedTokenSequences = expectedTokenSequences;
		this.tokenImages = tokenImages;
		this.statementKind = statementKind;
	}

	protected String escapeString(String value) {
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

	@Override
	public String getMessage() {
		if (!this.specialConstructor) {
			return super.getMessage();
		} else {
			String expectedText = "";
			int maxSequenceLength = 0;
			boolean statementKeywordsExcluded = this.statementKind == 64
				|| this.statementKind == 79
				|| this.statementKind == 144
				|| this.statementKind == 154
				|| this.statementKind == 162
				|| this.statementKind == 170
				|| this.statementKind == 169
				|| this.statementKind == 98
				|| this.statementKind == 86
				|| this.statementKind == 94
				|| this.statementKind == 100
				|| this.statementKind == 146
				|| this.statementKind == 182
				|| this.statementKind == 165
				|| this.statementKind == 166
				|| this.statementKind == 192
				|| this.statementKind == 193
				|| this.statementKind == 161
				|| this.statementKind == 180
				|| this.statementKind == 181;

			for (int sequenceIndex = 0; sequenceIndex < this.expectedTokenSequences.length; sequenceIndex++) {
				if (statementKeywordsExcluded) {
					switch (this.expectedTokenSequences[sequenceIndex][0]) {
						case 37:
						case 39:
						case 41:
						case 42:
						case 43:
						case 45:
						case 46:
						case 47:
						case 48:
						case 49:
						case 56:
						case 60:
						case 61:
						case 64:
						case 69:
						case 71:
						case 80:
						case 81:
						case 86:
						case 90:
							continue;
						case 38:
						case 40:
						case 44:
						case 50:
						case 51:
						case 52:
						case 53:
						case 54:
						case 55:
						case 57:
						case 58:
						case 59:
						case 62:
						case 63:
						case 65:
						case 66:
						case 67:
						case 68:
						case 70:
						case 72:
						case 73:
						case 74:
						case 75:
						case 76:
						case 77:
						case 78:
						case 79:
						case 82:
						case 83:
						case 84:
						case 85:
						case 87:
						case 88:
						case 89:
					}
				}

				if (maxSequenceLength < this.expectedTokenSequences[sequenceIndex].length) {
					maxSequenceLength = this.expectedTokenSequences[sequenceIndex].length;
				}

				for (int tokenIndex = 0; tokenIndex < this.expectedTokenSequences[sequenceIndex].length; tokenIndex++) {
					expectedText = expectedText + this.tokenImages[this.expectedTokenSequences[sequenceIndex][tokenIndex]] + " ";
				}

				if (this.expectedTokenSequences[sequenceIndex][this.expectedTokenSequences[sequenceIndex].length - 1] != 0) {
					expectedText = expectedText + "...";
				}

				expectedText = expectedText + this.lineSeparator + "    ";
			}

			String afterText = this.currentToken.image;
			ZKMScriptToken tokenCursor = this.currentToken.next;
			StringBuilder parseContextBuffer = new StringBuilder();
			parseContextBuffer.append("While parsing " + this.tokenImages[this.statementKind] + " statement");
			String tokenDescription = this.currentToken.getDescription();
			if (tokenDescription.length() > 0) {
				parseContextBuffer.append(tokenDescription);
			}

			String messageBuffer = CopyrightConstants.NAME + " encountered \"";

			for (int i = 0; i < maxSequenceLength; i++) {
				if (i != 0) {
					messageBuffer = messageBuffer + " ";
				}

				if (tokenCursor.kind == 0) {
					messageBuffer = messageBuffer + this.tokenImages[0];
					break;
				}

				messageBuffer = messageBuffer + this.escapeString(tokenCursor.image);
				tokenCursor = tokenCursor.next;
			}

			messageBuffer = messageBuffer
				+ "\" "
				+ this.lineSeparator
				+ (afterText != null && afterText.length() > 0 ? "\tafter \"" + afterText + "\"" + this.lineSeparator : "")
				+ "\tat line "
				+ this.currentToken.next.beginLine
				+ ", column "
				+ this.currentToken.next.beginColumn
				+ "."
				+ this.lineSeparator;
			if (this.expectedTokenSequences.length == 1) {
				messageBuffer = messageBuffer + "Was expecting:" + this.lineSeparator + "    ";
			} else {
				messageBuffer = messageBuffer + "Was expecting one of:" + this.lineSeparator + "    ";
			}

			messageBuffer = messageBuffer + expectedText;
			if (parseContextBuffer.length() > 0) {
				messageBuffer = messageBuffer + this.lineSeparator + parseContextBuffer.toString();
			}

			return messageBuffer;
		}
	}
}
