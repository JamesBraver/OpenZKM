package com.zelix.exceptions;

import com.zelix.klassmaster.changelogs.parser.ChangeLogsTokenList;
import com.zelix.klassmaster.changelogs.parser.Token3;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.CopyrightConstants;

public class ChangeLogsParserException extends RuntimeException implements ChangeLogsTokenList {
	protected String lineSeparator = Options.lineSeparator;
	protected boolean specialConstructor;
	public Token3 currentToken;
	public int[][] expectedTokenSequences;
	public String[] tokenImages;

	public ChangeLogsParserException(Token3 currentToken, int[][] expectedTokenSequences, String[] tokenImages) {
		super("");
		this.specialConstructor = true;
		this.currentToken = currentToken;
		this.expectedTokenSequences = expectedTokenSequences;
		this.tokenImages = tokenImages;
	}

	public ChangeLogsParserException() {
		this.specialConstructor = false;
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
					escapedBuffer.append("\"");
					break;
				case '\'':
					escapedBuffer.append("'");
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

			for (int sequenceIndex = 0; sequenceIndex < this.expectedTokenSequences.length; sequenceIndex++) {
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
			Token3 tokenCursor = this.currentToken.next;
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

			return messageBuffer + expectedText;
		}
	}
}
