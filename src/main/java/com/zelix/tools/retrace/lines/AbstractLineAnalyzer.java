package com.zelix.tools.retrace.lines;

import com.zelix.exceptions.ClassResolveException;
import com.zelix.exceptions.StacktraceAnalysisException;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.signature.MethodParameterKey;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.repository.ClassResolver;
import com.zelix.tools.retrace.ChangeLogInput;
import com.zelix.tools.retrace.MethodCandidate;
import com.zelix.tools.retrace.StacktraceElement;
import com.zelix.tools.retrace.StacktraceLine;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

public abstract class AbstractLineAnalyzer {
	public static final String[] EMPTY_PARAMETER_TYPES = new String[0];
	public static String INDENT = StringUtils.repeat(4, 32);
	public MethodCandidate[] methodCandidates;
	public boolean resolved;
	public String className;
	public StacktraceElement stacktraceElement;
	public Integer lineNumber;
	public boolean classResolved;
	public boolean previousResolved;
	public String sourceFileName;

	public String[] deobfuscateParameterTypes(List<String> parameterTypes, StringBuilder parameterList, ChangeLogInput changeLogInput) {
		int parameterCount = parameterTypes.size();
		String[] oldParameterTypes = new String[parameterCount];

		for (int i = 0; i < parameterCount; i++) {
			String newParameterType = parameterTypes.get(i);
			oldParameterTypes[i] = this.deobfuscateClassName(newParameterType, changeLogInput);
			parameterList.append(oldParameterTypes[i]);
			if (i < parameterCount - 1) {
				parameterList.append(", ");
			}
		}

		return oldParameterTypes;
	}

	public final String getTranslatedLine(int parameterTypeFormat, ChangeLogInput changeLogInput, ClassResolver classResolver) {
		if (this.methodCandidates != null) {
			Arrays.sort((Object[])this.methodCandidates);
		}

		StringBuffer lineBuffer = new StringBuffer(64);
		if (this.stacktraceElement.isValid() && this.classResolved) {
			lineBuffer.append(INDENT + "at " + this.className);
			if (this.methodCandidates != null && this.methodCandidates.length != 0) {
				if (this.methodCandidates.length == 1) {
					lineBuffer.append("." + this.methodCandidates[0].getMethodName());
					this.appendParameterTypes(this.methodCandidates[0], parameterTypeFormat, lineBuffer);
					this.appendSourceInfo(lineBuffer);
				} else {
					String continuationIndent = StringUtils.repeat(lineBuffer.length() + 1, 32);

					for (int i = 0; i < this.methodCandidates.length; i++) {
						if (i == 0) {
							lineBuffer.append(".");
						} else {
							lineBuffer.append(continuationIndent);
						}

						lineBuffer.append(this.methodCandidates[i].getMethodName());
						this.appendParameterTypes(this.methodCandidates[i], parameterTypeFormat, lineBuffer);
						if (i == 0) {
							this.appendSourceInfo(lineBuffer);
							lineBuffer.append(Options.lineSeparator);
						} else if (i < this.methodCandidates.length - 1) {
							lineBuffer.append(Options.lineSeparator);
						}
					}
				}
			}

			lineBuffer.append(Options.lineSeparator);
		} else {
			String lineText = this.stacktraceElement.getLine();
			if (!this.stacktraceElement.hasError() && (!this.stacktraceElement.isValid() || this.classResolved)) {
				if (changeLogInput == null) {
					lineBuffer.append(lineText);
				} else if (lineText.indexOf("Exception in ") > -1 && lineText.indexOf("java.lang.NullPointerException") > -1 && lineText.indexOf("Cannot invoke \"") > -1) {
					this.deobfuscateNpeMessage(lineBuffer, lineText, changeLogInput, classResolver);
				} else {
					this.deobfuscateMessage(lineBuffer, lineText, changeLogInput);
				}
			} else {
				lineBuffer.append(INDENT);
				lineBuffer.append(lineText);
			}

			lineBuffer.append(Options.lineSeparator);
		}

		return lineBuffer.toString();
	}

	public void appendParameterTypes(MethodCandidate candidate, int parameterTypeFormat, StringBuffer buffer) {
		String[] parameterTypes = candidate.getParameterTypes();
		if (parameterTypeFormat != 1 && parameterTypes != null && (parameterTypeFormat == 2 || parameterTypeFormat == 3)) {
			buffer.append("(");

			for (int i = 0; i < parameterTypes.length; i++) {
				if (parameterTypeFormat == 3) {
					buffer.append(parameterTypes[i]);
				} else if (parameterTypeFormat == 2) {
					String parameterType = parameterTypes[i];
					int lastDotIndex = parameterType.lastIndexOf(".");
					if (lastDotIndex > -1) {
						parameterType = parameterType.substring(lastDotIndex + 1);
					}

					buffer.append(parameterType);
				}

				if (i < parameterTypes.length - 1) {
					buffer.append(", ");
				}
			}

			buffer.append(")");
		}
	}

	public String getClassName() {
		return this.className;
	}

	public boolean isClassInitializer() {
		return this.methodCandidates != null && this.methodCandidates.length == 1 && this.methodCandidates[0].getMethodName().equals("static");
	}

	private void deobfuscateMessage(StringBuffer buffer, String text, ChangeLogInput changeLogInput) {
		StringTokenizer tokenizer = new StringTokenizer(text, "@: ()\",", true);
		ArrayList<String> tokens = new ArrayList<>(tokenizer.countTokens());

		while (tokenizer.hasMoreTokens()) {
			String nextToken = tokenizer.nextToken();
			tokens.add(nextToken);
		}

		for (int tokenIndex = 0; tokenIndex < tokens.size(); tokenIndex++) {
			String token = tokens.get(tokenIndex);
			switch (token) {
				case "@":
				case ":":
				case " ":
				case ")":
				case ",":
				case "\"":
				case "(":
					buffer.append(token);
					break;
				default:
					boolean slashSeparated = token.indexOf("/") > -1;
					int arrayDimensions = 0;
					String dottedToken;
					if (slashSeparated) {
						dottedToken = token.replace('/', '.');
					} else {
						dottedToken = token;
					}

					if (dottedToken.startsWith("[") && dottedToken.endsWith(";")) {
						while (dottedToken.charAt(arrayDimensions) == '[') {
							arrayDimensions++;
						}

						if (dottedToken.charAt(arrayDimensions) == 'L') {
							dottedToken = dottedToken.substring(arrayDimensions + 1, dottedToken.length() - 1);
						}
					}

					if (dottedToken.indexOf(46) > 0 && dottedToken.indexOf(46) < dottedToken.length() - 1) {
						boolean nameTranslated = false;
						int lastDotIndex = dottedToken.lastIndexOf(".");
						if (tokens.size() > tokenIndex + 1
							&& tokens.get(tokenIndex + 1).equals("(")
							&& tokens.indexOf(")") > tokenIndex + 1
							&& StringUtils.countOccurrences(dottedToken, '.') > 1
							&& lastDotIndex < dottedToken.length() - 1) {
							String newOwnerClassName = dottedToken.substring(0, lastDotIndex);
							String oldOwnerClassName = changeLogInput.getOldClassName(newOwnerClassName);
							if (oldOwnerClassName != null) {
								String newMethodName = dottedToken.substring(lastDotIndex + 1);
								StringBuilder parameterBuffer = new StringBuilder();

								for (int parameterIndex = tokenIndex + 2; parameterIndex < tokens.indexOf(")"); parameterIndex++) {
									String newTypeName = tokens.get(parameterIndex);
									String oldTypeName = changeLogInput.getOldTypeName(newTypeName);
									parameterBuffer.append(oldTypeName);
								}

								String[] oldMethodNames = changeLogInput.getOldMethodNames(oldOwnerClassName, newMethodName, null, parameterBuffer.toString());
								String memberSuffix;
								if (oldMethodNames != null && oldMethodNames.length > 0) {
									memberSuffix = "." + oldMethodNames[0];
								} else {
									memberSuffix = dottedToken.substring(lastDotIndex);
								}

								if (slashSeparated) {
									oldOwnerClassName = oldOwnerClassName.replace('.', '/');
								}

								buffer.append(oldOwnerClassName);
								buffer.append(memberSuffix);
								nameTranslated = true;
							}
						}

						if (!nameTranslated) {
							if (tokens.size() > tokenIndex + 1 && tokens.get(tokenIndex + 1).equals("(")) {
								buffer.append(token);
							} else {
								String oldClassName = changeLogInput.getOldClassName(dottedToken);
								if (oldClassName != null) {
									if (slashSeparated) {
										oldClassName = oldClassName.replace('.', '/');
									}

									buffer.append(oldClassName);
									if (arrayDimensions > 0) {
										for (int dimensionIndex = 0; dimensionIndex < arrayDimensions; dimensionIndex++) {
											buffer.append("[]");
										}
									}
								} else {
									buffer.append(token);
								}
							}
						}
					} else {
						buffer.append(token);
					}
			}
		}
	}

	public final boolean analyzeEntry(StacktraceLine entry, ChangeLogInput changeLogInput) {
		String newClassName = entry.getClassName();
		String oldClassName = changeLogInput.getOldClassName(newClassName);
		if (oldClassName != null) {
			this.classResolved = true;
			this.className = oldClassName;
			String sourceName = changeLogInput.getSourceName(oldClassName);
			if (sourceName != null) {
				this.sourceFileName = sourceName;
			}

			if (entry.hasLineNumber()) {
				int newLineNumber = entry.getLineNumber();
				if (newLineNumber < 0) {
					throw new StacktraceAnalysisException("Line number \"" + newLineNumber + "\" is negative in '" + entry.getLine() + "'.");
				}

				if (changeLogInput.hasLineNumberMappings(oldClassName)) {
					Integer oldLineNumber = changeLogInput.getOldLineNumber(oldClassName, newLineNumber);
					if (oldLineNumber == null) {
						throw new StacktraceAnalysisException(
							"Line number '" + newLineNumber + "' in '" + entry.getLine() + "' could not be matched. Change log does not correspond to obfuscated bytecode."
						);
					}

					this.lineNumber = oldLineNumber;
				} else {
					this.lineNumber = newLineNumber;
				}
			}

			this.resolved = true;
			return true;
		} else {
			this.className = newClassName;
			this.classResolved = false;
			this.resolved = false;
			return false;
		}
	}

	public boolean isElementParsed() {
		return this.stacktraceElement.isValid();
	}

	public void appendSourceInfo(StringBuffer buffer) {
		if (this.sourceFileName != null && this.sourceFileName.length() > 0) {
			buffer.append("(" + this.sourceFileName);
			if (this.lineNumber != null) {
				buffer.append(":" + this.lineNumber);
			}

			buffer.append(")");
		}
	}

	private AbstractMethodNode[] findMethods(ClassNode classNode, String methodName, String parameterTypes, ClassResolver classResolver) {
		MethodParameterKey methodKey = new MethodParameterKey(methodName, MethodSignature.fromHumanReadableParameters(parameterTypes));
		AbstractMethodNode[] foundMethods = classNode.findMethods(methodKey);
		return foundMethods != null && foundMethods.length > 0 ? foundMethods : this.findMethodsInHierarchy(classNode, methodKey, classResolver);
	}

	private void deobfuscateNpeMessage(StringBuffer buffer, String text, ChangeLogInput changeLogInput, ClassResolver classResolver) {
		String invocationText = null;
		boolean invocationTranslated = false;

		try {
			int cannotInvokeStart = text.indexOf("Cannot invoke \"");
			if (cannotInvokeStart > 0) {
				int invocationEndQuote = text.indexOf("\"", cannotInvokeStart + 15);
				if (invocationEndQuote > -1) {
					invocationText = text.substring(cannotInvokeStart + 15, invocationEndQuote);
					int openParenIndex = invocationText.indexOf(40);
					if (openParenIndex > 1) {
						int closeParenIndex = invocationText.indexOf(41);
						if (closeParenIndex == invocationText.length() - 1) {
							String methodReference = invocationText.substring(0, openParenIndex);
							int lastDotIndex = methodReference.lastIndexOf(".");
							if (lastDotIndex > 0 && lastDotIndex < methodReference.length() - 1) {
								String newClassName = methodReference.substring(0, lastDotIndex);
								String newMethodName = methodReference.substring(lastDotIndex + 1);
								String parameterList = invocationText.substring(openParenIndex + 1, closeParenIndex);
								StringBuilder parameterBuffer = new StringBuilder();
								StringTokenizer tokenizer = new StringTokenizer(parameterList, " ,");

								while (tokenizer.hasMoreTokens()) {
									String oldTypeName = changeLogInput.getOldTypeName(tokenizer.nextToken());
									parameterBuffer.append(oldTypeName);
									if (tokenizer.hasMoreTokens()) {
										parameterBuffer.append(", ");
									}
								}

								String oldClassName = changeLogInput.getOldClassName(newClassName);
								if (oldClassName == null) {
									oldClassName = newClassName;
								}

								if (classResolver != null) {
									ClassNode ownerClassNode = null;

									try {
										ownerClassNode = classResolver.resolveClass(GlobalUtils.binaryToInternalName(newClassName));
									} catch (ClassResolveException ignoredClassResolve) {
									}

									if (ownerClassNode != null) {
										AbstractMethodNode[] foundMethods = this.findMethods(ownerClassNode, newMethodName, parameterList, classResolver);
										if (foundMethods != null && foundMethods.length == 1) {
											AbstractMethodNode methodNode = foundMethods[0];
											if (methodNode.isWorkspaceNode()) {
												String resolvedOldClassName = changeLogInput.getOldClassName(methodNode.getBinaryName());
												String[] resolvedOldMethodNames = changeLogInput.getOldMethodNames(resolvedOldClassName, newMethodName, null, parameterBuffer.toString());
												buffer.append(text, 0, cannotInvokeStart + 15);
												buffer.append(oldClassName);
												buffer.append(".");
												buffer.append(resolvedOldMethodNames[0]);
												buffer.append('(');
												buffer.append((CharSequence)parameterBuffer);
												buffer.append(')');
												this.deobfuscateMessage(buffer, text.substring(invocationEndQuote), changeLogInput);
												invocationTranslated = true;
											}
										}
									}
								} else {
									String[] oldMethodNames = changeLogInput.getOldMethodNames(oldClassName, newMethodName, null, parameterBuffer.toString());
									if (oldMethodNames != null && oldMethodNames.length == 1) {
										buffer.append(text, 0, cannotInvokeStart + 15);
										buffer.append(oldClassName);
										buffer.append(".");
										buffer.append(oldMethodNames[0]);
										buffer.append('(');
										buffer.append((CharSequence)parameterBuffer);
										buffer.append(')');
										this.deobfuscateMessage(buffer, text.substring(invocationEndQuote), changeLogInput);
										invocationTranslated = true;
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception ignoredMessageParse) {
		}

		if (invocationText == null || !invocationTranslated) {
			this.deobfuscateMessage(buffer, text, changeLogInput);
		}
	}

	private AbstractMethodNode[] findMethodsInHierarchy(ClassNode classNode, MethodParameterKey methodKey, ClassResolver classResolver) {
		AbstractMethodNode[] foundMethods = classNode.findMethods(methodKey);
		if (foundMethods != null && foundMethods.length > 0) {
			return foundMethods;
		} else {
			String superclassName = classNode.getSuperclassName();
			if (!superclassName.equals("java/lang/Object")) {
				ClassNode superclassNode = classResolver.resolveClass(superclassName);
				foundMethods = this.findMethodsInHierarchy(superclassNode, methodKey, classResolver);
				if (foundMethods != null && foundMethods.length > 0) {
					return foundMethods;
				}
			}

			for (String interfaceName : classNode.getInterfaceNames()) {
				ClassNode interfaceNode = classResolver.resolveClass(interfaceName);
				foundMethods = this.findMethodsInHierarchy(interfaceNode, methodKey, classResolver);
				if (foundMethods != null && foundMethods.length > 0) {
					return foundMethods;
				}
			}

			return null;
		}
	}

	public String getWarningMessage() {
		return this.stacktraceElement.getError();
	}

	public String deobfuscateClassName(String className, ChangeLogInput changeLogInput) {
		int arraySuffixIndex = className.indexOf("[");
		String arraySuffix;
		if (arraySuffixIndex != -1) {
			arraySuffix = className.substring(arraySuffixIndex);
			className = className.substring(0, arraySuffixIndex);
		} else {
			arraySuffix = "";
		}

		String oldClassName = changeLogInput.getOldClassName(className);
		if (oldClassName == null) {
			oldClassName = className;
		}

		return oldClassName + arraySuffix;
	}

	public boolean isPreviousResolved() {
		return this.previousResolved;
	}

	public boolean hasWarning() {
		return this.stacktraceElement.hasError();
	}

	public boolean isClassResolved() {
		return this.classResolved;
	}
}
