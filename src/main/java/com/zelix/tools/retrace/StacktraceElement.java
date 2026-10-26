package com.zelix.tools.retrace;

import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.dataset.AtomicInt;
import java.util.StringTokenizer;

public class StacktraceElement implements StacktraceLine {
	private String linePrefix = "";
	private boolean readed = true;
	private String leftPart = "";
	private String rightPart = "";
	private String line;
	private String methodDescriptor;
	private String slashSubStr;
	private AtomicInt lineNumber;
	private String methodName;
	private String errorMessage;
	private String stackTraceLine;

	public StacktraceElement(String line) {
		this.line = line.trim();
		int atKeywordIndex = this.indexOfAtKeyword();
		if (atKeywordIndex > -1) {
			this.linePrefix = this.line.substring(0, atKeywordIndex);
			String afterAtKeyword = this.line.substring(atKeywordIndex + 3).trim();
			int openParenIndex = afterAtKeyword.indexOf("(");
			if (openParenIndex > -1) {
				if (this.readStackTraceLine(afterAtKeyword.substring(0, openParenIndex).trim())) {
					this.readLineNumber(afterAtKeyword.substring(openParenIndex));
				} else {
					this.readed = false;
				}
			} else {
				this.readed = this.readStackTraceLine(afterAtKeyword.trim());
			}
		} else {
			this.readed = false;
		}
	}

	@Override
	public boolean isValid() {
		return this.readed && this.errorMessage == null;
	}

	@Override
	public String getLine() {
		return this.line;
	}

	private int indexOfAtKeyword() {
		boolean atKeywordFound = false;
		int atKeywordIndex = -1;
		int searchPosition = -1;

		while (!atKeywordFound && (searchPosition = this.line.indexOf("at ", searchPosition + 1)) > -1) {
			if (searchPosition <= 0) {
				atKeywordIndex = 0;
				break;
			}

			switch (this.line.charAt(searchPosition - 1)) {
				case '\t':
				case ' ':
					atKeywordFound = true;
					atKeywordIndex = searchPosition;
			}
		}

		return atKeywordIndex;
	}

	public String getMethodDescriptor() {
		return this.methodDescriptor;
	}

	public String getError() {
		return this.errorMessage;
	}

	public void setError(String message) {
		this.errorMessage = message;
	}

	private void readLineNumber(String sourceInfo) {
		if (sourceInfo.length() >= 2) {
			String tracingLine = sourceInfo;
			int openParenIndex = sourceInfo.indexOf("(", 1);
			int closeParenIndex = sourceInfo.indexOf(")");
			if (openParenIndex > closeParenIndex) {
				String descriptorCandidate = sourceInfo.substring(0, openParenIndex).trim();
				if (TypeWriter.isValidMethodDescriptor(descriptorCandidate)) {
					this.methodDescriptor = descriptorCandidate;
					tracingLine = sourceInfo.substring(openParenIndex);
				}
			}

			int colonIndex = tracingLine.indexOf(":");
			int rightBrIndex = tracingLine.lastIndexOf(")");
			if (colonIndex > -1 && rightBrIndex > -1 && rightBrIndex > colonIndex) {
				String lineNumberText = tracingLine.substring(colonIndex + 1, rightBrIndex).trim();

				try {
					int origLineNumber = Integer.parseInt(lineNumberText);
					this.lineNumber = new AtomicInt(origLineNumber);
					this.leftPart = tracingLine.substring(0, colonIndex + 1);
					this.rightPart = tracingLine.substring(rightBrIndex);
				} catch (NumberFormatException ignored) {
					this.leftPart = tracingLine;
				}
			} else {
				this.leftPart = tracingLine;
			}
		}
	}

	public String getMethodName() {
		return this.methodName;
	}

	@Override
	public boolean hasLineNumber() {
		return this.lineNumber != null;
	}

	@Override
	public int getLineNumber() {
		return this.lineNumber != null ? this.lineNumber.get() : -1;
	}

	public boolean hasError() {
		return this.errorMessage != null;
	}

	@Override
	public String getClassName() {
		return this.stackTraceLine != null ? this.stackTraceLine.replace('/', '.') : null;
	}

	private boolean readStackTraceLine(String classAndMethod) {
		int slashPos = classAndMethod.indexOf("/");
		if (slashPos > -1) {
			DebugUtils.assertTrue(slashPos > 0 && slashPos < classAndMethod.length() - 2, new String[]{"Invalid stack trace line '" + classAndMethod + "' : " + slashPos + " : '" + classAndMethod + "'"});
			this.slashSubStr = classAndMethod.substring(slashPos);
			classAndMethod = classAndMethod.substring(slashPos + 1);
		}

		StringTokenizer tokenizer = new StringTokenizer(classAndMethod, ".");
		int tokenCount = tokenizer.countTokens();
		if (tokenCount > 1) {
			int tokenIndex = 0;

			StringBuilder classNameBuffer;
			for (classNameBuffer = new StringBuilder(); tokenizer.hasMoreTokens(); tokenIndex++) {
				String nextToken = tokenizer.nextToken();
				if (tokenIndex == 0) {
					classNameBuffer.append(nextToken);
				} else if (tokenIndex == tokenCount - 1) {
					this.methodName = nextToken.trim();
					int spaceIndex = this.methodName.indexOf(" ");
					if (spaceIndex > -1) {
						this.methodName = this.methodName.substring(0, spaceIndex);
					}
				} else {
					classNameBuffer.append("." + nextToken);
				}
			}

			this.stackTraceLine = classNameBuffer.toString();
			this.stackTraceLine = this.stackTraceLine.replace('/', '.');
			return true;
		} else {
			return false;
		}
	}
}
