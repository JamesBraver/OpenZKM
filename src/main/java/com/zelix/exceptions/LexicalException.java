package com.zelix.exceptions;

import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.script.ZKMScriptTokenList;

public class LexicalException extends Error implements ZKMScriptTokenList {
	protected static String lineSeparator = Options.lineSeparator;
	public int errorCode;

	public LexicalException(boolean eofSeen, int lexState, int errorLine, int errorColumn, String errorAfter, char curChar, int errorCode) {
		this(formatLexicalErrorMessage(eofSeen, lexState, errorLine, errorColumn, errorAfter, curChar), errorCode);
	}

	public LexicalException(String message, int errorCode) {
		super(message);
		this.errorCode = errorCode;
	}

	protected static String formatLexicalErrorMessage(boolean eofSeen, int lexState, int errorLine, int errorColumn, String errorAfter, int curChar) {
		String lexStateDescription = null;
		String detailMessage = null;
		switch (lexState) {
			case 0:
				break;
			case 1:
			case 2:
			case 3:
				lexStateDescription = "comment";
				break;
			case 4:
				lexStateDescription = "string literal";
				if (curChar == 10 || curChar == 13) {
					detailMessage = "String not terminated at the end of the line.";
				}
				break;
			default:
				lexStateDescription = "unknown lexical state";
		}

		return CopyrightConstants.NAME + " encountered "
			+ (eofSeen ? "unexpected <EOF> " : "\"" + escapeString(String.valueOf((char)curChar)) + "\" (ascii " + curChar + "),")
			+ lineSeparator
			+ "\tat line "
			+ errorLine
			+ ", column "
			+ errorColumn
			+ (eofSeen ? "" : lineSeparator + "\tafter : \"" + escapeString(errorAfter) + "\"")
			+ lineSeparator
			+ "LEXICAL ERROR "
			+ (lexStateDescription == null ? "" : "while inside a " + lexStateDescription + ".")
			+ (detailMessage == null ? "" : " " + detailMessage);
	}

	protected static final String escapeString(String value) {
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
		switch (this.errorCode) {
			case 1:
			case 2:
			case 3:
				return CopyrightConstants.NAME + " internal parser error (" + this.errorCode + ")";
			default:
				return super.getMessage();
		}
	}
}
