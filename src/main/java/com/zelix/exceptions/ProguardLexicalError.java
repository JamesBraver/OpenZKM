package com.zelix.exceptions;

public class ProguardLexicalError extends Error {
	public int errorCode;

	public ProguardLexicalError(boolean eofSeen, int lexState, int errorLine, int errorColumn, String errorAfter, char curChar, int errorCode) {
		this(formatLexicalErrorMessage(eofSeen, lexState, errorLine, errorColumn, errorAfter, curChar), errorCode);
	}

	public ProguardLexicalError(String message, int errorCode) {
		super(message);
		this.errorCode = errorCode;
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

	protected static String formatLexicalErrorMessage(boolean eofSeen, int lexState, int errorLine, int errorColumn, String errorAfter, int curChar) {
		return "Lexical error at line "
			+ errorLine
			+ ", column "
			+ errorColumn
			+ ".  Encountered: "
			+ (eofSeen ? "<EOF> " : "\"" + escapeString(String.valueOf((char)curChar)) + "\" (" + curChar + "), ")
			+ "after : \""
			+ escapeString(errorAfter)
			+ "\"";
	}

	@Override
	public String getMessage() {
		return super.getMessage();
	}
}
