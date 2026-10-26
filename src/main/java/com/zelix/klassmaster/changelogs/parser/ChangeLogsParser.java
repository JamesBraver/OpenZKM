package com.zelix.klassmaster.changelogs.parser;

import com.zelix.exceptions.ChangeLogsParserException;
import com.zelix.exceptions.TokenMgrError3;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNodeBase;
import com.zelix.klassmaster.changelogs.parser.entry.*;
import com.zelix.klassmaster.changelogs.parser.flowObfuscation.*;
import com.zelix.klassmaster.changelogs.parser.name.*;
import com.zelix.klassmaster.changelogs.parser.signature.*;
import com.zelix.klassmaster.changelogs.parser.value.*;
import com.zelix.utils.StringUtils;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class ChangeLogsParser implements ChangeLogsTokenList, ChangeLogsTokenConstants {
	protected static LexicalBuffer3 lexicalBuffer = new LexicalBuffer3();
	private static boolean initializedOnce = false;
	private static final int[] la1Generation = new int[60];
	private static final LookaheadRecord[] lookaheadRecords = new LookaheadRecord[6];
	private static boolean rescanning = false;
	private static int lookaheadGcCounter = 0;
	private static final TokenMgrError3 lookaheadSuccess = new TokenMgrError3();
	private static List<int[]> expectedTokenSequences = new ArrayList<>();
	private static int errorTokenKind = -1;
	private static int[] errorTokenKinds = new int[100];
	private static int[] errorTokenSequence;
	public static ChangeLogsParser instance;
	public static ChangeLogsTokenManager tokenSource;
	private static Token3 lastPos;
	public static SimpleCharStream3 inputStream;
	public static Token3 nextToken;
	private static int[] la1TokenBits2;
	private static int[] la1TokenBits0;
	private static int[] la1TokenBits1;
	private static int lookaheadRemaining;
	private static int errorTokenEndPos;
	private static Token3 scanPos;
	private static int generationCounter;
	public static Token3 token;

	public ChangeLogsParser(Reader reader) {
		if (initializedOnce) {
			System.out.println("ERROR: Second call to constructor of static parser. ");
			throw new Error();
		} else {
			initializedOnce = true;
			instance = this;
			inputStream = new SimpleCharStream3(reader, 1, 1);
			tokenSource = new ChangeLogsTokenManager(inputStream);
			token = new Token3();
			token.next = nextToken = ChangeLogsTokenManager.getNextToken();
			generationCounter = 0;

			for (int i = 0; i < 60; i++) {
				la1Generation[i] = -1;
			}

			for (int recordIndex = 0; recordIndex < lookaheadRecords.length; recordIndex++) {
				lookaheadRecords[recordIndex] = new LookaheadRecord();
			}
		}
	}

	private static boolean memberModifierNextIsEnum() {
		return scanToken(26);
	}

	private static boolean nameTokenNextIsFinal() {
		return scanToken(30);
	}

	private static boolean nameTokenNextIsPublic() {
		return scanToken(38);
	}

	private static boolean nameTokenNextIsNative() {
		return scanToken(40);
	}

	private static boolean nameTokenNextIsPrivate() {
		return scanToken(47);
	}

	private static boolean nameTokenNextIsVolatile() {
		return scanToken(50);
	}

	private static boolean nameTokenNextIsProtected() {
		return scanToken(51);
	}

	private static boolean nameTokenNextIsInterface() {
		return scanToken(53);
	}

	public static final void parseType() throws ChangeLogsParserException {
		ChangeLogTypeNode typeNode = new ChangeLogTypeNode(34);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(typeNode);

		try {
			switch (nextToken.kind) {
				case 25:
					Token3 andToken = consumeToken(25);
					lexicalBuffer.closeNodeScope(typeNode, true);
					nodeScopeOpen = false;
					typeNode.setValue(andToken.image);
					break;
				case 26:
					Token3 enumToken = consumeToken(26);
					lexicalBuffer.closeNodeScope(typeNode, true);
					nodeScopeOpen = false;
					typeNode.setValue(enumToken.image);
					break;
				case 27:
					Token3 noneToken = consumeToken(27);
					lexicalBuffer.closeNodeScope(typeNode, true);
					nodeScopeOpen = false;
					typeNode.setValue(noneToken.image);
					break;
				case 28:
					Token3 trueToken = consumeToken(28);
					lexicalBuffer.closeNodeScope(typeNode, true);
					nodeScopeOpen = false;
					typeNode.setValue(trueToken.image);
					break;
				case 29:
				case 30:
				case 34:
				case 35:
				case 38:
				case 39:
				case 40:
				case 41:
				case 44:
				case 45:
				case 46:
				case 47:
				case 49:
				case 50:
				case 51:
				case 53:
				case 54:
				case 55:
				case 56:
				case 59:
				case 60:
				case 61:
				case 62:
				case 63:
				case 65:
				case 66:
				case 68:
				case 69:
				case 70:
				case 71:
				case 72:
				case 73:
				case 74:
				default:
					la1Generation[50] = generationCounter;
					consumeToken(-1);
					throw new ChangeLogsParserException();
				case 31:
					Token3 falseToken = consumeToken(31);
					lexicalBuffer.closeNodeScope(typeNode, true);
					nodeScopeOpen = false;
					typeNode.setValue(falseToken.image);
					break;
				case 32:
					Token3 lightToken = consumeToken(32);
					lexicalBuffer.closeNodeScope(typeNode, true);
					nodeScopeOpen = false;
					typeNode.setValue(lightToken.image);
					break;
				case 33:
					Token3 heavyToken = consumeToken(33);
					lexicalBuffer.closeNodeScope(typeNode, true);
					nodeScopeOpen = false;
					typeNode.setValue(heavyToken.image);
					break;
				case 36:
					Token3 normalToken = consumeToken(36);
					lexicalBuffer.closeNodeScope(typeNode, true);
					nodeScopeOpen = false;
					typeNode.setValue(normalToken.image);
					break;
				case 37:
					Token3 bridgeToken = consumeToken(37);
					lexicalBuffer.closeNodeScope(typeNode, true);
					nodeScopeOpen = false;
					typeNode.setValue(bridgeToken.image);
					break;
				case 42:
					Token3 executeToken = consumeToken(42);
					lexicalBuffer.closeNodeScope(typeNode, true);
					nodeScopeOpen = false;
					typeNode.setValue(executeToken.image);
					break;
				case 43:
					Token3 excludeToken = consumeToken(43);
					lexicalBuffer.closeNodeScope(typeNode, true);
					nodeScopeOpen = false;
					typeNode.setValue(excludeToken.image);
					break;
				case 48:
					Token3 enhancedToken = consumeToken(48);
					lexicalBuffer.closeNodeScope(typeNode, true);
					nodeScopeOpen = false;
					typeNode.setValue(enhancedToken.image);
					break;
				case 52:
					Token3 syntheticToken = consumeToken(52);
					lexicalBuffer.closeNodeScope(typeNode, true);
					nodeScopeOpen = false;
					typeNode.setValue(syntheticToken.image);
					break;
				case 57:
					Token3 aggressiveToken = consumeToken(57);
					lexicalBuffer.closeNodeScope(typeNode, true);
					nodeScopeOpen = false;
					typeNode.setValue(aggressiveToken.image);
					break;
				case 58:
					Token3 annotationToken = consumeToken(58);
					lexicalBuffer.closeNodeScope(typeNode, true);
					nodeScopeOpen = false;
					typeNode.setValue(annotationToken.image);
					break;
				case 64:
					Token3 nameNotChangedToken = consumeToken(64);
					lexicalBuffer.closeNodeScope(typeNode, true);
					nodeScopeOpen = false;
					typeNode.setValue(nameNotChangedToken.image);
					break;
				case 67:
					Token3 signatureNotChangedToken = consumeToken(67);
					lexicalBuffer.closeNodeScope(typeNode, true);
					nodeScopeOpen = false;
					typeNode.setValue(signatureNotChangedToken.image);
					break;
				case 75:
					Token3 integerToken = consumeToken(75);
					lexicalBuffer.closeNodeScope(typeNode, true);
					nodeScopeOpen = false;
					typeNode.setValue(integerToken.image);
					break;
				case 76:
					Token3 alphanumericToken = consumeToken(76);
					lexicalBuffer.closeNodeScope(typeNode, true);
					nodeScopeOpen = false;
					typeNode.setValue(alphanumericToken.image);
					break;
				case 77:
					Token3 nameToken = consumeToken(77);
					lexicalBuffer.closeNodeScope(typeNode, true);
					nodeScopeOpen = false;
					typeNode.setValue(nameToken.image);
			}
		} finally {
			if (nodeScopeOpen) {
				lexicalBuffer.closeNodeScope(typeNode, true);
			}
		}
	}

	private static boolean typeRefNextIsNormal() {
		return scanToken(36);
	}

	private static void initLa1TokenBits0() {
		la1TokenBits0 = new int[]{
         8192,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         0,
         -570425344,
         16777216,
         16777216,
         0,
         -570425344,
         0,
         -570425344,
         0,
         0,
         0,
         -570425344,
         16777216,
         0,
         16777216,
         32768,
         65536,
         -570425344,
         65536,
         65536,
         16777216,
         0,
         6291456,
         6291456,
         4194304,
         1140850688,
         1140850688,
         -1644167168,
         -1644167168,
         8388608,
         1048576,
         4194304,
         262144,
         -570425344,
         -1644167168,
         1048576,
         34603008,
         34603008,
         536870912,
         0,
         536870912,
         0,
         0,
         0
      };
	}

	private static boolean startsClassModifier(int lookaheadDepth) {
		lookaheadRemaining = lookaheadDepth;
		lastPos = scanPos = token;

		try {
			return !matchesClassModifier();
		} catch (TokenMgrError3 ignored) {
		} finally {
			saveLookaheadState(0, lookaheadDepth);
		}

		return true;
	}

	private static boolean startsFieldNameChangeModifier(int lookaheadDepth) {
		lookaheadRemaining = lookaheadDepth;
		lastPos = scanPos = token;

		try {
			return !matchesFieldModifier();
		} catch (TokenMgrError3 ignored) {
		} finally {
			saveLookaheadState(1, lookaheadDepth);
		}

		return true;
	}

	private static boolean startsTypeBeforeSignature(int lookaheadDepth) {
		lookaheadRemaining = lookaheadDepth;
		lastPos = scanPos = token;

		try {
			return !matchesTypeBeforeSignature();
		} catch (TokenMgrError3 ignored) {
		} finally {
			saveLookaheadState(4, lookaheadDepth);
		}

		return true;
	}

	public static final void parseMethodArguments() {
		ChangeLogMethodArgumentsNode methodArgumentsNode = new ChangeLogMethodArgumentsNode(30);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(methodArgumentsNode);

		try {
			parseQualifiedType();

			while (true) {
				switch (nextToken.kind) {
					case 20:
						consumeToken(20);
						parseQualifiedType();
						break;
					default:
						la1Generation[46] = generationCounter;
						lexicalBuffer.closeNodeScope(methodArgumentsNode, true);
						return;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(methodArgumentsNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(methodArgumentsNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private static boolean matchesClassModifier() {
		return nextIsAnyClassModifier();
	}

	private static boolean startsMethodNameChangeModifier(int lookaheadDepth) {
		lookaheadRemaining = lookaheadDepth;
		lastPos = scanPos = token;

		try {
			return !matchesMethodModifier();
		} catch (TokenMgrError3 ignored) {
		} finally {
			saveLookaheadState(3, lookaheadDepth);
		}

		return true;
	}

	public static final void parseParameterChangeData() {
		ChangeLogParameterChangeDataNode parameterChangeDataNode = new ChangeLogParameterChangeDataNode(19);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(parameterChangeDataNode);

		try {
			consumeToken(15);
			parseName();
			lexicalBuffer.closeNodeScope(parameterChangeDataNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(parameterChangeDataNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(parameterChangeDataNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private static boolean nameTokenNextIsEnhanced() {
		return scanToken(48);
	}

	public static final void parseMethodNameChange() {
		ChangeLogMethodNameChangeNode methodNameChangeNode = new ChangeLogMethodNameChangeNode(17);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(methodNameChangeNode);

		try {
			while (startsMethodNameChangeModifier(4)) {
				parseMemberModifier();
			}

			if (startsTypeBeforeSignature(Integer.MAX_VALUE)) {
				parseMethodReturnType();
			}

			parseMethodSignature();
			label64:
			switch (nextToken.kind) {
				case 24:
					consumeToken(24);
					if (startsNewMethodSignature(Integer.MAX_VALUE)) {
						parseNewMethodSignature();
						switch (nextToken.kind) {
							case 24:
								consumeToken(24);
								parseNewMethodSignature();
								break;
							default:
								la1Generation[30] = generationCounter;
						}

						switch (nextToken.kind) {
							case 15:
								parseParameterChangeData();
								break;
							default:
								la1Generation[31] = generationCounter;
						}

						switch (nextToken.kind) {
							case 16:
								parseParameterChangeLookupData();
								break label64;
							default:
								la1Generation[32] = generationCounter;
								break label64;
						}
					} else {
						switch (nextToken.kind) {
							case 25:
							case 26:
							case 27:
							case 28:
							case 31:
							case 32:
							case 33:
							case 34:
							case 36:
							case 37:
							case 42:
							case 43:
							case 45:
							case 48:
							case 52:
							case 57:
							case 58:
							case 59:
							case 64:
							case 67:
							case 75:
							case 76:
							case 77:
								parseNewMethodName();
								break label64;
							case 29:
							case 30:
							case 35:
							case 38:
							case 39:
							case 40:
							case 41:
							case 44:
							case 46:
							case 47:
							case 49:
							case 50:
							case 51:
							case 53:
							case 54:
							case 55:
							case 56:
							case 60:
							case 61:
							case 62:
							case 63:
							case 65:
							case 66:
							case 68:
							case 69:
							case 70:
							case 71:
							case 72:
							case 73:
							case 74:
							default:
								la1Generation[33] = generationCounter;
								consumeToken(-1);
								throw new ChangeLogsParserException();
						}
					}
				case 64:
					consumeToken(64);
					break;
				case 67:
					consumeToken(67);
					switch (nextToken.kind) {
						case 16:
							parseParameterChangeLookupData();
							break label64;
						default:
							la1Generation[34] = generationCounter;
							break label64;
					}
				case 68:
					consumeToken(68);
					switch (nextToken.kind) {
						case 16:
							parseParameterChangeLookupData();
							break label64;
						default:
							la1Generation[35] = generationCounter;
							break label64;
					}
				default:
					la1Generation[36] = generationCounter;
					consumeToken(-1);
					throw new ChangeLogsParserException();
			}

			switch (nextToken.kind) {
				case 63:
					parseManufactured();
					break;
				default:
					la1Generation[37] = generationCounter;
			}

			lexicalBuffer.closeNodeScope(methodNameChangeNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(methodNameChangeNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(methodNameChangeNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private static boolean nameTokenNextIsBridge() {
		return scanToken(37);
	}

	private static void saveLookaheadState(int specIndex, int lookaheadDepth) {
		LookaheadRecord record;
		for (record = lookaheadRecords[specIndex]; record.generation > generationCounter; record = record.next) {
			if (record.next == null) {
				record = record.next = new LookaheadRecord();
				break;
			}
		}

		record.generation = generationCounter + lookaheadDepth - lookaheadRemaining;
		record.first = token;
		record.lookaheadCount = lookaheadDepth;
	}

	public static final void parseFieldData() {
		ChangeLogFieldDataNode fieldDataNode = new ChangeLogFieldDataNode(46);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(fieldDataNode);

		try {
			switch (nextToken.kind) {
				case 75:
					parseInteger();
					break;
				case 76:
					parseAlphanumeric();
					break;
				default:
					la1Generation[57] = generationCounter;
					consumeToken(-1);
					throw new ChangeLogsParserException();
			}

			while (true) {
				switch (nextToken.kind) {
					case 76:
						parseAlphanumeric();
						break;
					default:
						la1Generation[58] = generationCounter;
						lexicalBuffer.closeNodeScope(fieldDataNode, true);
						return;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(fieldDataNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(fieldDataNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private static boolean classModifierNextIsFinal() {
		return scanToken(30);
	}

	public static final void parseNewMethodSignature() {
		ChangeLogNewMethodSignatureNode newMethodSignatureNode = new ChangeLogNewMethodSignatureNode(27);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(newMethodSignatureNode);

		try {
			parseName();
			consumeToken(14);
			switch (nextToken.kind) {
				case 25:
				case 26:
				case 27:
				case 28:
				case 31:
				case 32:
				case 33:
				case 36:
				case 37:
				case 42:
				case 43:
				case 48:
				case 52:
				case 57:
				case 58:
				case 64:
				case 67:
				case 75:
				case 76:
				case 77:
					parseMethodArguments();
					break;
				case 29:
				case 30:
				case 34:
				case 35:
				case 38:
				case 39:
				case 40:
				case 41:
				case 44:
				case 45:
				case 46:
				case 47:
				case 49:
				case 50:
				case 51:
				case 53:
				case 54:
				case 55:
				case 56:
				case 59:
				case 60:
				case 61:
				case 62:
				case 63:
				case 65:
				case 66:
				case 68:
				case 69:
				case 70:
				case 71:
				case 72:
				case 73:
				case 74:
				default:
					la1Generation[44] = generationCounter;
			}

			consumeToken(17);
			switch (nextToken.kind) {
				case 23:
					parseParameterObfuscated();
					break;
				default:
					la1Generation[45] = generationCounter;
			}

			lexicalBuffer.closeNodeScope(newMethodSignatureNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(newMethodSignatureNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(newMethodSignatureNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private static boolean matchesNewMethodSignature() {
		return matchesNameOrKeywordToken() ? true : scanToken(14);
	}

	private static boolean nameTokenNextIsEnum() {
		return scanToken(26);
	}

	public static final void parseManufactured() {
		ChangeLogManufacturedNode manufacturedNode = new ChangeLogManufacturedNode(21);
		lexicalBuffer.openNodeScope(manufacturedNode);

		try {
			consumeToken(63);
		} finally {
			lexicalBuffer.closeNodeScope(manufacturedNode, true);
		}
	}

	public static final void parsePackageNameNotChanged() {
		ChangeLogPackageNameNotChangedNode packageNameNotChangedNode = new ChangeLogPackageNameNotChangedNode(6);
		lexicalBuffer.openNodeScope(packageNameNotChangedNode);

		try {
			consumeToken(64);
		} finally {
			lexicalBuffer.closeNodeScope(packageNameNotChangedNode, true);
		}
	}

	private static boolean nextIsDotOrTypeRefToken() {
		return scanToken(22) ? true : matchesTypeToken();
	}

	private static boolean typeRefNextIsBridge() {
		return scanToken(37);
	}

	public static final void parseMethodParameterClassNames() {
		ChangeLogMethodParameterClassNamesNode methodParameterClassNamesNode = new ChangeLogMethodParameterClassNamesNode(3);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(methodParameterClassNamesNode);

		try {
			consumeToken(71);
			parseOldClassName();
			parseOldClassName();
			parseOldClassName();
			switch (nextToken.kind) {
				case 41:
					consumeToken(41);
					parseOldModuleName();
					break;
				default:
					la1Generation[16] = generationCounter;
			}

			lexicalBuffer.closeNodeScope(methodParameterClassNamesNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(methodParameterClassNamesNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(methodParameterClassNamesNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public static final void parseArrayLevel() {
		ChangeLogArrayLevelNode arrayLevelNode = new ChangeLogArrayLevelNode(40);
		lexicalBuffer.openNodeScope(arrayLevelNode);

		try {
			consumeToken(18);
			consumeToken(19);
		} finally {
			lexicalBuffer.closeNodeScope(arrayLevelNode, true);
		}
	}

	private static boolean memberModifierNextIsPrivate() {
		return scanToken(47);
	}

	private static boolean typeRefNextIsNone() {
		return scanToken(27);
	}

	public static final void parseMemberFlowObfuscationData() {
		ChangeLogMemberFlowObfuscationDataNode memberFlowObfuscationDataNode = new ChangeLogMemberFlowObfuscationDataNode(43);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(memberFlowObfuscationDataNode);

		try {
			consumeToken(60);
			parseFlowObfuscationClassName();
			consumeToken(29);
			parseMemberData();
			lexicalBuffer.closeNodeScope(memberFlowObfuscationDataNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(memberFlowObfuscationDataNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(memberFlowObfuscationDataNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	public static final void parseOldClassName() {
		ChangeLogOldClassNameNode oldClassNameNode = new ChangeLogOldClassNameNode(11);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(oldClassNameNode);

		try {
			parseNameList();
			lexicalBuffer.closeNodeScope(oldClassNameNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(oldClassNameNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(oldClassNameNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private static boolean scanToken(int kind) {
		if (scanPos == lastPos) {
			lookaheadRemaining--;
			if (scanPos.next == null) {
				lastPos = scanPos = scanPos.next = ChangeLogsTokenManager.getNextToken();
			} else {
				lastPos = scanPos = scanPos.next;
			}
		} else {
			scanPos = scanPos.next;
		}

		if (rescanning) {
			int tokenPos = 0;

			Token3 scannedToken;
			for (scannedToken = token; scannedToken != null && scannedToken != scanPos; scannedToken = scannedToken.next) {
				tokenPos++;
			}

			if (scannedToken != null) {
				addErrorToken(kind, tokenPos);
			}
		}

		if (scanPos.kind != kind) {
			return true;
		} else if (lookaheadRemaining == 0 && scanPos == lastPos) {
			throw lookaheadSuccess;
		} else {
			return false;
		}
	}

	private static boolean nameTokenNextIsHeavy() {
		return scanToken(33);
	}

	public static final void parseClassChange() {
		ChangeLogClassChangeNode classChangeNode = new ChangeLogClassChangeNode(7);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(classChangeNode);

		try {
			consumeToken(35);

			while (startsClassModifier(3)) {
				parseClassModifier();
			}

			parseOldClassName();
			switch (nextToken.kind) {
				case 24:
					consumeToken(24);
					parseNewClassName();
					break;
				case 64:
					consumeToken(64);
					break;
				default:
					la1Generation[19] = generationCounter;
					consumeToken(-1);
					throw new ChangeLogsParserException();
			}

			switch (nextToken.kind) {
				case 44:
					consumeToken(44);
					parseSourceName();
					break;
				default:
					la1Generation[20] = generationCounter;
			}

			label86:
			switch (nextToken.kind) {
				case 55:
					consumeToken(55);
					parseDisregardedNameList();

					while (true) {
						switch (nextToken.kind) {
							case 25:
							case 26:
							case 27:
							case 28:
							case 30:
							case 31:
							case 32:
							case 33:
							case 34:
							case 36:
							case 37:
							case 38:
							case 39:
							case 40:
							case 42:
							case 43:
							case 45:
							case 47:
							case 48:
							case 49:
							case 50:
							case 51:
							case 52:
							case 53:
							case 54:
							case 57:
							case 58:
							case 59:
							case 61:
							case 64:
							case 67:
							case 75:
							case 76:
							case 77:
								parseFieldNameChange();
								break;
							case 29:
							case 35:
							case 41:
							case 44:
							case 46:
							case 55:
							case 56:
							case 60:
							case 62:
							case 63:
							case 65:
							case 66:
							case 68:
							case 69:
							case 70:
							case 71:
							case 72:
							case 73:
							case 74:
							default:
								la1Generation[21] = generationCounter;
								break label86;
						}
					}
				default:
					la1Generation[22] = generationCounter;
			}

			label77:
			switch (nextToken.kind) {
				case 56:
					consumeToken(56);
					parseDisregardedNameList();

					while (true) {
						switch (nextToken.kind) {
							case 25:
							case 26:
							case 27:
							case 28:
							case 30:
							case 31:
							case 32:
							case 33:
							case 34:
							case 36:
							case 37:
							case 38:
							case 39:
							case 40:
							case 42:
							case 43:
							case 45:
							case 47:
							case 48:
							case 49:
							case 50:
							case 51:
							case 52:
							case 53:
							case 54:
							case 57:
							case 58:
							case 59:
							case 61:
							case 64:
							case 67:
							case 75:
							case 76:
							case 77:
								parseMethodNameChange();
								break;
							case 29:
							case 35:
							case 41:
							case 44:
							case 46:
							case 55:
							case 56:
							case 60:
							case 62:
							case 63:
							case 65:
							case 66:
							case 68:
							case 69:
							case 70:
							case 71:
							case 72:
							case 73:
							case 74:
							default:
								la1Generation[23] = generationCounter;
								break label77;
						}
					}
				default:
					la1Generation[24] = generationCounter;
			}

			label68:
			switch (nextToken.kind) {
				case 65:
					consumeToken(65);
					parseDisregardedNameList();

					while (true) {
						switch (nextToken.kind) {
							case 75:
								parseLineNumberChange();
								break;
							default:
								la1Generation[25] = generationCounter;
								break label68;
						}
					}
				default:
					la1Generation[26] = generationCounter;
			}

			lexicalBuffer.closeNodeScope(classChangeNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;
			caughtThrowable.printStackTrace();

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(classChangeNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(classChangeNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private static boolean nameTokenNextIsPackageInfo() {
		return scanToken(59);
	}

	private static boolean nameTokenNextIsInit() {
		return scanToken(34);
	}

	public static final void parseLineNumberChange() {
		ChangeLogLineNumberChangeNode lineNumberChangeNode = new ChangeLogLineNumberChangeNode(35);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(lineNumberChangeNode);

		try {
			parseOldLineNumber();
			consumeToken(24);
			parseNewLineNumber();
			label46:
			switch (nextToken.kind) {
				case 20:
				case 25:
					switch (nextToken.kind) {
						case 20:
							while (true) {
								consumeToken(20);
								parseNewLineNumber();
								switch (nextToken.kind) {
									case 20:
										break;
									default:
										la1Generation[51] = generationCounter;
										consumeToken(25);
										parseNewLineNumber();
										break label46;
								}
							}
						case 25:
							consumeToken(25);
							parseNewLineNumber();
							break label46;
						default:
							la1Generation[52] = generationCounter;
							consumeToken(-1);
							throw new ChangeLogsParserException();
					}
				default:
					la1Generation[53] = generationCounter;
			}

			lexicalBuffer.closeNodeScope(lineNumberChangeNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(lineNumberChangeNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(lineNumberChangeNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private static void addErrorToken(int kind, int tokenPos) {
		if (tokenPos < 100) {
			if (tokenPos == errorTokenEndPos + 1) {
				errorTokenKinds[errorTokenEndPos++] = kind;
			} else if (errorTokenEndPos != 0) {
				errorTokenSequence = new int[errorTokenEndPos];

				for (int i = 0; i < errorTokenEndPos; i++) {
					errorTokenSequence[i] = errorTokenKinds[i];
				}

				label39:
				for (int[] expectedSequence : expectedTokenSequences) {
					if (expectedSequence.length == errorTokenSequence.length) {
						for (int tokenIndex = 0; tokenIndex < errorTokenSequence.length; tokenIndex++) {
							if (expectedSequence[tokenIndex] != errorTokenSequence[tokenIndex]) {
								continue label39;
							}
						}

						expectedTokenSequences.add(errorTokenSequence);
						break;
					}
				}

				if (tokenPos != 0) {
					int[] tokenKinds = errorTokenKinds;
					errorTokenEndPos = tokenPos;
					tokenKinds[tokenPos - 1] = kind;
				}
			}
		}
	}

	public static final void parseNameList() {
		ChangeLogNameListNode nameListNode = new ChangeLogNameListNode(22);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(nameListNode);

		try {
			parseName();

			while (true) {
				switch (nextToken.kind) {
					case 21:
					case 22:
						switch (nextToken.kind) {
							case 21:
								consumeToken(21);
								break;
							case 22:
								consumeToken(22);
								break;
							default:
								la1Generation[39] = generationCounter;
								consumeToken(-1);
								throw new ChangeLogsParserException();
						}

						parseName();
						break;
					default:
						la1Generation[38] = generationCounter;
						lexicalBuffer.closeNodeScope(nameListNode, true);
						return;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(nameListNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(nameListNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private static boolean startsFieldTypeBeforeName(int lookaheadDepth) {
		lookaheadRemaining = lookaheadDepth;
		lastPos = scanPos = token;

		try {
			return !matchesFieldTypeBeforeName();
		} catch (TokenMgrError3 ignored) {
		} finally {
			saveLookaheadState(2, lookaheadDepth);
		}

		return true;
	}

	public static final void parseSourceName() {
		ChangeLogSourceNameNode sourceNameNode = new ChangeLogSourceNameNode(13);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(sourceNameNode);

		try {
			switch (nextToken.kind) {
				case 25:
				case 26:
				case 27:
				case 28:
				case 30:
				case 31:
				case 32:
				case 33:
				case 34:
				case 36:
				case 37:
				case 38:
				case 40:
				case 42:
				case 43:
				case 45:
				case 47:
				case 48:
				case 50:
				case 51:
				case 52:
				case 53:
				case 57:
				case 58:
				case 59:
				case 64:
				case 67:
				case 75:
				case 76:
				case 77:
					parseNameList();
					break;
				case 29:
				case 35:
				case 39:
				case 41:
				case 44:
				case 46:
				case 49:
				case 54:
				case 55:
				case 56:
				case 60:
				case 61:
				case 62:
				case 63:
				case 65:
				case 66:
				case 68:
				case 69:
				case 70:
				case 71:
				case 72:
				case 74:
				default:
					la1Generation[27] = generationCounter;
					consumeToken(-1);
					throw new ChangeLogsParserException();
				case 73:
					parseStringLiteral();
			}

			lexicalBuffer.closeNodeScope(sourceNameNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(sourceNameNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(sourceNameNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private static boolean typeRefNextIsAggressive() {
		return scanToken(57);
	}

	public static final void parseNewMethodName() {
		ChangeLogNewMethodNameNode newMethodNameNode = new ChangeLogNewMethodNameNode(18);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(newMethodNameNode);

		try {
			parseName();
			lexicalBuffer.closeNodeScope(newMethodNameNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(newMethodNameNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(newMethodNameNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private static boolean memberModifierNextIsTransient() {
		return scanToken(54);
	}

	public static final void parseFieldType() {
		ChangeLogFieldTypeNode fieldTypeNode = new ChangeLogFieldTypeNode(28);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(fieldTypeNode);

		try {
			parseQualifiedType();
			lexicalBuffer.closeNodeScope(fieldTypeNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(fieldTypeNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(fieldTypeNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private static boolean typeRefNextIsHeavy() {
		return scanToken(33);
	}

	public static ChangeLogsParserException generateParseException() {
		expectedTokenSequences.clear();
		boolean[] expectedTokenFlags = new boolean[78];
		if (errorTokenKind >= 0) {
			expectedTokenFlags[errorTokenKind] = true;
			errorTokenKind = -1;
		}

		for (int la1Index = 0; la1Index < 60; la1Index++) {
			if (la1Generation[la1Index] == generationCounter) {
				for (int bitIndex = 0; bitIndex < 32; bitIndex++) {
					if ((la1TokenBits0[la1Index] & 1 << bitIndex) != 0) {
						expectedTokenFlags[bitIndex] = true;
					}

					if ((la1TokenBits1[la1Index] & 1 << bitIndex) != 0) {
						expectedTokenFlags[32 + bitIndex] = true;
					}

					if ((la1TokenBits2[la1Index] & 1 << bitIndex) != 0) {
						expectedTokenFlags[64 + bitIndex] = true;
					}
				}
			}
		}

		for (int tokenKind = 0; tokenKind < 78; tokenKind++) {
			if (expectedTokenFlags[tokenKind]) {
				errorTokenSequence = new int[1];
				errorTokenSequence[0] = tokenKind;
				expectedTokenSequences.add(errorTokenSequence);
			}
		}

		errorTokenEndPos = 0;
		rescanToken();
		addErrorToken(0, 0);
		int[][] expectedSequences = new int[expectedTokenSequences.size()][];

		for (int i = 0; i < expectedTokenSequences.size(); i++) {
			expectedSequences[i] = expectedTokenSequences.get(i);
		}

		return new ChangeLogsParserException(token, expectedSequences, ChangeLogsTokenList.tokenImage);
	}

	public static final void parseAlphanumeric() {
		ChangeLogAlphanumericNode alphanumericNode = new ChangeLogAlphanumericNode(39);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(alphanumericNode);

		try {
			Token3 alphanumericToken = consumeToken(76);
			lexicalBuffer.closeNodeScope(alphanumericNode, true);
			nodeScopeOpen = false;
			alphanumericNode.setValue(alphanumericToken.image);
		} catch (Throwable rethrownThrowable) {
			if (nodeScopeOpen) {
				lexicalBuffer.closeNodeScope(alphanumericNode, true);
			}

			throw rethrownThrowable;
		}
	}

	private static boolean nameTokenNextIsSynthetic() {
		return scanToken(52);
	}

	private static boolean classModifierNextIsInterface() {
		return scanToken(53);
	}

	public static final void parseOldModuleName() {
		ChangeLogOldModuleNameNode oldModuleNameNode = new ChangeLogOldModuleNameNode(8);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(oldModuleNameNode);

		try {
			parseNameList();
			lexicalBuffer.closeNodeScope(oldModuleNameNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(oldModuleNameNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(oldModuleNameNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private static boolean typeRefNextIsExclude() {
		return scanToken(43);
	}

	public static void reInit(Reader reader) {
		inputStream.ReInit(reader, 1, 1);
		ChangeLogsTokenManager.ReInit(inputStream);
		token = new Token3();
		token.next = nextToken = ChangeLogsTokenManager.getNextToken();
		lexicalBuffer.clear();
		generationCounter = 0;

		for (int i = 0; i < 60; i++) {
			la1Generation[i] = -1;
		}

		for (int recordIndex = 0; recordIndex < lookaheadRecords.length; recordIndex++) {
			lookaheadRecords[recordIndex] = new LookaheadRecord();
		}
	}

	public static final void parseMemberData() {
		ChangeLogMemberDataNode memberDataNode = new ChangeLogMemberDataNode(47);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(memberDataNode);

		try {
			parseInteger();
			switch (nextToken.kind) {
				case 76:
					parseAlphanumeric();
					break;
				default:
					la1Generation[59] = generationCounter;
			}

			lexicalBuffer.closeNodeScope(memberDataNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(memberDataNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(memberDataNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private static boolean typeRefNextIsLight() {
		return scanToken(32);
	}

	private static boolean nameTokenNextIsIntegerLiteral() {
		return scanToken(75);
	}

	public static final void parseName() {
		ChangeLogNameNode nameNode = new ChangeLogNameNode(33);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(nameNode);

		try {
			switch (nextToken.kind) {
				case 25:
					Token3 andToken = consumeToken(25);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(andToken.image);
					break;
				case 26:
					Token3 enumToken = consumeToken(26);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(enumToken.image);
					break;
				case 27:
					Token3 noneToken = consumeToken(27);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(noneToken.image);
					break;
				case 28:
					Token3 trueToken = consumeToken(28);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(trueToken.image);
					break;
				case 29:
				case 35:
				case 39:
				case 41:
				case 44:
				case 46:
				case 49:
				case 54:
				case 55:
				case 56:
				case 60:
				case 61:
				case 62:
				case 63:
				case 65:
				case 66:
				case 68:
				case 69:
				case 70:
				case 71:
				case 72:
				case 73:
				case 74:
				default:
					la1Generation[49] = generationCounter;
					consumeToken(-1);
					throw new ChangeLogsParserException();
				case 30:
					Token3 finalToken = consumeToken(30);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(finalToken.image);
					break;
				case 31:
					Token3 falseToken = consumeToken(31);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(falseToken.image);
					break;
				case 32:
					Token3 lightToken = consumeToken(32);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(lightToken.image);
					break;
				case 33:
					Token3 heavyToken = consumeToken(33);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(heavyToken.image);
					break;
				case 34:
					Token3 initToken = consumeToken(34);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(initToken.image);
					break;
				case 36:
					Token3 normalToken = consumeToken(36);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(normalToken.image);
					break;
				case 37:
					Token3 bridgeToken = consumeToken(37);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(bridgeToken.image);
					break;
				case 38:
					Token3 publicToken = consumeToken(38);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(publicToken.image);
					break;
				case 40:
					Token3 nativeToken = consumeToken(40);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(nativeToken.image);
					break;
				case 42:
					Token3 executeToken = consumeToken(42);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(executeToken.image);
					break;
				case 43:
					Token3 excludeToken = consumeToken(43);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(excludeToken.image);
					break;
				case 45:
					Token3 clinitToken = consumeToken(45);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(clinitToken.image);
					break;
				case 47:
					Token3 privateToken = consumeToken(47);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(privateToken.image);
					break;
				case 48:
					Token3 enhancedToken = consumeToken(48);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(enhancedToken.image);
					break;
				case 50:
					Token3 volatileToken = consumeToken(50);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(volatileToken.image);
					break;
				case 51:
					Token3 protectedToken = consumeToken(51);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(protectedToken.image);
					break;
				case 52:
					Token3 syntheticToken = consumeToken(52);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(syntheticToken.image);
					break;
				case 53:
					Token3 interfaceToken = consumeToken(53);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(interfaceToken.image);
					break;
				case 57:
					Token3 aggressiveToken = consumeToken(57);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(aggressiveToken.image);
					break;
				case 58:
					Token3 annotationToken = consumeToken(58);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(annotationToken.image);
					break;
				case 59:
					Token3 packageInfoToken = consumeToken(59);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(packageInfoToken.image);
					break;
				case 64:
					Token3 nameNotChangedToken = consumeToken(64);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(nameNotChangedToken.image);
					break;
				case 67:
					Token3 signatureNotChangedToken = consumeToken(67);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(signatureNotChangedToken.image);
					break;
				case 75:
					Token3 integerLiteralToken = consumeToken(75);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(integerLiteralToken.image);
					break;
				case 76:
					Token3 alphanumericLiteralToken = consumeToken(76);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(alphanumericLiteralToken.image);
					break;
				case 77:
					Token3 nameToken = consumeToken(77);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeOpen = false;
					nameNode.setValue(nameToken.image);
			}
		} finally {
			if (nodeOpen) {
				lexicalBuffer.closeNodeScope(nameNode, true);
			}
		}
	}

	private static boolean typeRefNextIsEnhanced() {
		return scanToken(48);
	}

	public static final void parseObfuscateReferencesClassName() {
		ChangeLogObfuscateReferencesClassNameNode obfuscateReferencesClassNameNode = new ChangeLogObfuscateReferencesClassNameNode(2);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(obfuscateReferencesClassNameNode);

		try {
			consumeToken(70);
			parseOldClassName();
			switch (nextToken.kind) {
				case 41:
					consumeToken(41);
					parseOldModuleName();
					break;
				default:
					la1Generation[15] = generationCounter;
			}

			lexicalBuffer.closeNodeScope(obfuscateReferencesClassNameNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(obfuscateReferencesClassNameNode);
					nodeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					lexicalBuffer.closeNodeScope(obfuscateReferencesClassNameNode, true);
				}

				throw throwable2;
			}
		}
	}

	private static boolean nameTokenNextIsExclude() {
		return scanToken(43);
	}

	public static final void parseNewPackageName() {
		ChangeLogNewPackageNameNode newPackageNameNode = new ChangeLogNewPackageNameNode(10);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(newPackageNameNode);

		try {
			parseNameList();
			lexicalBuffer.closeNodeScope(newPackageNameNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(newPackageNameNode);
					nodeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					lexicalBuffer.closeNodeScope(newPackageNameNode, true);
				}

				throw throwable2;
			}
		}
	}

	private static boolean fieldChangeNextIsNameToken() {
		return matchesNameOrKeywordToken();
	}

	private static boolean memberModifierNextIsPublic() {
		return scanToken(38);
	}

	private static boolean typeRefNextIsAnd() {
		return scanToken(25);
	}

	private static boolean typeRefNextIsSignatureNotChanged() {
		return scanToken(67);
	}

	private static boolean typeRefNextIsAlphanumericLiteral() {
		return scanToken(76);
	}

	private static boolean nameTokenNextIsAnnotation() {
		return scanToken(58);
	}

	private static boolean typeRefNextIsSynthetic() {
		return scanToken(52);
	}

	private static boolean classModifierNextIsEnum() {
		return scanToken(26);
	}

	private static boolean typeRefNextIsName() {
		return scanToken(77);
	}

	private static boolean classModifierNextIsPublic() {
		return scanToken(38);
	}

	private static boolean typeRefNextIsAnnotation() {
		return scanToken(58);
	}

	private static boolean nextIsAnyClassModifier() {
		Token3 savedScanPos = scanPos;
		if (classModifierNextIsPublic()) {
			scanPos = savedScanPos;
			if (classModifierNextIsFinal()) {
				scanPos = savedScanPos;
				if (classModifierNextIsInterface()) {
					scanPos = savedScanPos;
					if (classModifierAltAbstract()) {
						scanPos = savedScanPos;
						if (classModifierAltSynthetic()) {
							scanPos = savedScanPos;
							if (classModifierNextIsEnum()) {
								scanPos = savedScanPos;
								if (classModifierAltAnnotation()) {
									return true;
								}
							}
						}
					}
				}
			}
		}

		return false;
	}

	private static boolean nextIsArrayLevelBracket() {
		return nextIsOpenBracketOrCloseBracket();
	}

	private static boolean typeRefNextIsNameNotChanged() {
		return scanToken(64);
	}

	private static boolean nameTokenNextIsFalse() {
		return scanToken(31);
	}

	private static boolean typeRefNextIsExecute() {
		return scanToken(42);
	}

	private static boolean nextIsOpenBracketOrCloseBracket() {
		return scanToken(18) ? true : scanToken(19);
	}

	private static boolean nameTokenAltNormal() {
		return scanToken(36);
	}

	private static boolean memberModifierAltFinal() {
		return scanToken(30);
	}

	private static boolean matchesMethodModifier() {
		return matchesMemberModifierToken();
	}

	private static boolean matchesTypeBeforeSignature() {
		if (matchesReturnTypePrefix()) {
			return true;
		} else {
			return matchesNameOrKeywordToken() ? true : scanToken(14);
		}
	}

	private static boolean nameTokenAltAnd() {
		return scanToken(25);
	}

	private static boolean typeTokenAltInteger() {
		return scanToken(75);
	}

	private static boolean classModifierAltAnnotation() {
		return scanToken(58);
	}

	private static boolean memberModifierAltProtected() {
		return scanToken(51);
	}

	private static boolean nameTokenAltClinit() {
		return scanToken(45);
	}

	private static boolean matchesFieldTypeBeforeName() {
		if (matchesFieldTypePrefix()) {
			return true;
		} else if (fieldChangeNextIsNameToken()) {
			return true;
		} else {
			Token3 savedScanPos = scanPos;
			if (scanToken(24)) {
				scanPos = savedScanPos;
				if (scanToken(64)) {
					return true;
				}
			}

			return false;
		}
	}

	private static boolean memberModifierAltAbstract() {
		return scanToken(49);
	}

	private static boolean matchesFieldModifier() {
		return matchesMemberModifierToken();
	}

	private static boolean typeTokenAltEnum() {
		return scanToken(26);
	}

	private static boolean nameTokenAltNone() {
		return scanToken(27);
	}

	public static final void parseModuleChange() {
		ChangeLogModuleChangeNode moduleChangeNode = new ChangeLogModuleChangeNode(4);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(moduleChangeNode);

		try {
			consumeToken(41);
			parseOldModuleName();
			consumeToken(64);
			lexicalBuffer.closeNodeScope(moduleChangeNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(moduleChangeNode);
					nodeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					lexicalBuffer.closeNodeScope(moduleChangeNode, true);
				}

				throw throwable2;
			}
		}
	}

	private static boolean nameTokenAltAlphanumeric() {
		return scanToken(76);
	}

	private static void initLa1TokenBits1() {
		la1TokenBits1 = new int[]{
         0,
         512,
         16384,
         8,
         0,
         0,
         0,
         0,
         1073741824,
         268435456,
         1073741824,
         268435456,
         1073741824,
         1073741824,
         512,
         512,
         512,
         238923127,
         0,
         0,
         4096,
         780119543,
         8388608,
         780119543,
         16777216,
         0,
         0,
         238923127,
         0,
         Integer.MIN_VALUE,
         0,
         0,
         0,
         238923127,
         0,
         0,
         0,
         Integer.MIN_VALUE,
         0,
         0,
         0,
         70385728,
         543064544,
         101780531,
         101780531,
         0,
         0,
         0,
         0,
         238923127,
         101780531,
         0,
         0,
         0,
         0,
         16384,
         0,
         0,
         0,
         0
      };
	}

	public static final void parseFlowObfuscationClassName() {
		ChangeLogFlowObfuscationClassNameNode flowObfuscationClassNameNode = new ChangeLogFlowObfuscationClassNameNode(44);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(flowObfuscationClassNameNode);

		try {
			parseNameList();
			lexicalBuffer.closeNodeScope(flowObfuscationClassNameNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(flowObfuscationClassNameNode);
					nodeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					lexicalBuffer.closeNodeScope(flowObfuscationClassNameNode, true);
				}

				throw throwable2;
			}
		}
	}

	public static final void parseNewClassName() {
		ChangeLogNewClassNameNode newClassNameNode = new ChangeLogNewClassNameNode(12);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(newClassNameNode);

		try {
			parseNameList();
			lexicalBuffer.closeNodeScope(newClassNameNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(newClassNameNode);
					nodeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					lexicalBuffer.closeNodeScope(newClassNameNode, true);
				}

				throw throwable2;
			}
		}
	}

	private static boolean matchesReturnTypePrefix() {
		return matchesQualifiedType();
	}

	private static Token3 consumeToken(int kind) {
		Token3 savedToken = token;
		if ((token = nextToken).next != null) {
			nextToken = nextToken.next;
		} else {
			nextToken = nextToken.next = ChangeLogsTokenManager.getNextToken();
		}

		if (token.kind != kind) {
			nextToken = token;
			token = savedToken;
			errorTokenKind = kind;
			throw generateParseException();
		} else {
			generationCounter++;
			if (++lookaheadGcCounter > 100) {
				lookaheadGcCounter = 0;

				for (int recordIndex = 0; recordIndex < lookaheadRecords.length; recordIndex++) {
					for (LookaheadRecord lookaheadRecord = lookaheadRecords[recordIndex]; lookaheadRecord != null; lookaheadRecord = lookaheadRecord.next) {
						if (lookaheadRecord.generation < generationCounter) {
							lookaheadRecord.first = null;
						}
					}
				}
			}

			return token;
		}
	}

	private static boolean typeTokenAltFalse() {
		return scanToken(31);
	}

	public static final void parseParameterChangeLookupData() {
		ChangeLogParameterChangeLookupDataNode parameterChangeLookupDataNode = new ChangeLogParameterChangeLookupDataNode(20);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(parameterChangeLookupDataNode);

		try {
			consumeToken(16);
			parseName();
			lexicalBuffer.closeNodeScope(parameterChangeLookupDataNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(parameterChangeLookupDataNode);
					nodeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					lexicalBuffer.closeNodeScope(parameterChangeLookupDataNode, true);
				}

				throw throwable2;
			}
		}
	}

	private static void initLa1TokenBits2() {
		la1TokenBits2 = new int[]{
			0,
			0,
			0,
			0,
			32,
			64,
			128,
			4,
			0,
			0,
			0,
			0,
			4,
			4,
			0,
			0,
			0,
			14345,
			1,
			1,
			0,
			14345,
			0,
			14345,
			0,
			2048,
			2,
			14857,
			1,
			0,
			0,
			0,
			0,
			14345,
			0,
			0,
			25,
			0,
			0,
			0,
			0,
			0,
			0,
			14345,
			14345,
			0,
			0,
			0,
			0,
			14345,
			14345,
			0,
			0,
			0,
			0,
			0,
			0,
			6144,
			4096,
			4096
		};
	}

	private static boolean nameTokenAltSignatureNotChanged() {
		return scanToken(67);
	}

	private static boolean memberModifierAltStatic() {
		return scanToken(39);
	}

	private static void rescanToken() {
		rescanning = true;

		for (int recordIndex = 0; recordIndex < 6; recordIndex++) {
			try {
				LookaheadRecord lookaheadRecord = lookaheadRecords[recordIndex];

				while (true) {
					if (lookaheadRecord.generation > generationCounter) {
						lookaheadRemaining = lookaheadRecord.lookaheadCount;
						lastPos = scanPos = lookaheadRecord.first;
						switch (recordIndex) {
							case 0:
								matchesClassModifier();
								break;
							case 1:
								matchesFieldModifier();
								break;
							case 2:
								matchesFieldTypeBeforeName();
								break;
							case 3:
								matchesMethodModifier();
								break;
							case 4:
								matchesTypeBeforeSignature();
								break;
							case 5:
								matchesNewMethodSignature();
						}
					}

					lookaheadRecord = lookaheadRecord.next;
					if (lookaheadRecord == null) {
						break;
					}
				}
			} catch (TokenMgrError3 ignored) {
			}
		}

		rescanning = false;
	}

	public static final void parseQualifiedType() {
		ChangeLogQualifiedTypeNode qualifiedTypeNode = new ChangeLogQualifiedTypeNode(31);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(qualifiedTypeNode);

		try {
			parseType();

			while (true) {
				switch (nextToken.kind) {
					case 22:
						consumeToken(22);
						parseType();
						break;
					default:
						la1Generation[47] = generationCounter;

						while (true) {
							switch (nextToken.kind) {
								case 18:
									parseArrayLevel();
									break;
								default:
									la1Generation[48] = generationCounter;
									lexicalBuffer.closeNodeScope(qualifiedTypeNode, true);
									return;
							}
						}
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(qualifiedTypeNode);
					nodeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					lexicalBuffer.closeNodeScope(qualifiedTypeNode, true);
				}

				throw throwable2;
			}
		}
	}

	private static boolean classModifierAltSynthetic() {
		return scanToken(52);
	}

	public static final void parseMethodSignature() {
		ChangeLogMethodSignatureNode methodSignatureNode = new ChangeLogMethodSignatureNode(26);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(methodSignatureNode);

		try {
			parseName();
			consumeToken(14);
			switch (nextToken.kind) {
				case 25:
				case 26:
				case 27:
				case 28:
				case 31:
				case 32:
				case 33:
				case 36:
				case 37:
				case 42:
				case 43:
				case 48:
				case 52:
				case 57:
				case 58:
				case 64:
				case 67:
				case 75:
				case 76:
				case 77:
					parseMethodArguments();
					break;
				case 29:
				case 30:
				case 34:
				case 35:
				case 38:
				case 39:
				case 40:
				case 41:
				case 44:
				case 45:
				case 46:
				case 47:
				case 49:
				case 50:
				case 51:
				case 53:
				case 54:
				case 55:
				case 56:
				case 59:
				case 60:
				case 61:
				case 62:
				case 63:
				case 65:
				case 66:
				case 68:
				case 69:
				case 70:
				case 71:
				case 72:
				case 73:
				case 74:
				default:
					la1Generation[43] = generationCounter;
			}

			consumeToken(17);
			lexicalBuffer.closeNodeScope(methodSignatureNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(methodSignatureNode);
					nodeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					lexicalBuffer.closeNodeScope(methodSignatureNode, true);
				}

				throw throwable2;
			}
		}
	}

	private static boolean matchesQualifiedType() {
		if (matchesTypeToken()) {
			return true;
		} else {
			Token3 savedScanPos;
			do {
				savedScanPos = scanPos;
			} while (!nextIsDotOrTypeRefToken());

			scanPos = savedScanPos;

			do {
				savedScanPos = scanPos;
			} while (!nextIsArrayLevelBracket());

			scanPos = savedScanPos;
			return false;
		}
	}

	public static final void parseStringLiteral() {
		ChangeLogStringLiteralNode stringLiteralNode = new ChangeLogStringLiteralNode(48);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(stringLiteralNode);

		try {
			Token3 stringLiteralToken = consumeToken(73);
			lexicalBuffer.closeNodeScope(stringLiteralNode, true);
			nodeOpen = false;
			String string = stringLiteralToken.image;
			string = StringUtils.replace(string.substring(1, string.length() - 1), "\"\"", "\"");
			stringLiteralNode.setValue(string);
		} catch (Throwable throwable) {
			if (nodeOpen) {
				lexicalBuffer.closeNodeScope(stringLiteralNode, true);
			}

			throw throwable;
		}
	}

	private static boolean matchesTypeToken() {
		Token3 savedScanPos = scanPos;
		if (typeRefNextIsName()) {
			scanPos = savedScanPos;
			if (typeRefNextIsNameNotChanged()) {
				scanPos = savedScanPos;
				if (typeRefNextIsSignatureNotChanged()) {
					scanPos = savedScanPos;
					if (typeRefNextIsAnnotation()) {
						scanPos = savedScanPos;
						if (typeRefNextIsBridge()) {
							scanPos = savedScanPos;
							if (typeTokenAltEnum()) {
								scanPos = savedScanPos;
								if (typeRefNextIsSynthetic()) {
									scanPos = savedScanPos;
									if (typeRefNextIsEnhanced()) {
										scanPos = savedScanPos;
										if (typeRefNextIsLight()) {
											scanPos = savedScanPos;
											if (typeRefNextIsHeavy()) {
												scanPos = savedScanPos;
												if (typeRefNextIsNormal()) {
													scanPos = savedScanPos;
													if (typeRefNextIsAggressive()) {
														scanPos = savedScanPos;
														if (typeRefNextIsNone()) {
															scanPos = savedScanPos;
															if (typeRefNextIsExecute()) {
																scanPos = savedScanPos;
																if (typeRefNextIsExclude()) {
																	scanPos = savedScanPos;
																	if (typeTokenAltFalse()) {
																		scanPos = savedScanPos;
																		if (typeTokenAltTrue()) {
																			scanPos = savedScanPos;
																			if (typeRefNextIsAnd()) {
																				scanPos = savedScanPos;
																				if (typeTokenAltInteger()) {
																					scanPos = savedScanPos;
																					if (typeRefNextIsAlphanumericLiteral()) {
																						return true;
																					}
																				}
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		return false;
	}

	public static final void parseFlowObfuscationPackageName() {
		ChangeLogFlowObfuscationPackageNameNode flowObfuscationPackageNameNode = new ChangeLogFlowObfuscationPackageNameNode(45);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(flowObfuscationPackageNameNode);

		try {
			parseNameList();
			lexicalBuffer.closeNodeScope(flowObfuscationPackageNameNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(flowObfuscationPackageNameNode);
					nodeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					lexicalBuffer.closeNodeScope(flowObfuscationPackageNameNode, true);
				}

				throw throwable2;
			}
		}
	}

	private static boolean matchesMemberModifierToken() {
		Token3 savedScanPos = scanPos;
		if (memberModifierNextIsPublic()) {
			scanPos = savedScanPos;
			if (memberModifierAltProtected()) {
				scanPos = savedScanPos;
				if (memberModifierNextIsPrivate()) {
					scanPos = savedScanPos;
					if (memberModifierAltFinal()) {
						scanPos = savedScanPos;
						if (memberModifierAltAbstract()) {
							scanPos = savedScanPos;
							if (memberModifierAltStatic()) {
								scanPos = savedScanPos;
								if (memberModifierAltVolatile()) {
									scanPos = savedScanPos;
									if (memberModifierNextIsTransient()) {
										scanPos = savedScanPos;
										if (memberModifierAltSynchronized()) {
											scanPos = savedScanPos;
											if (memberModifierAltNative()) {
												scanPos = savedScanPos;
												if (memberModifierAltSynthetic()) {
													scanPos = savedScanPos;
													if (memberModifierNextIsEnum()) {
														scanPos = savedScanPos;
														if (memberModifierAltBridge()) {
															return true;
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		return false;
	}

	public static final void parseOldFieldName() {
		ChangeLogOldFieldNameNode oldFieldNameNode = new ChangeLogOldFieldNameNode(15);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(oldFieldNameNode);

		try {
			parseName();
			lexicalBuffer.closeNodeScope(oldFieldNameNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(oldFieldNameNode);
					nodeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					lexicalBuffer.closeNodeScope(oldFieldNameNode, true);
				}

				throw throwable2;
			}
		}
	}

	private static boolean memberModifierAltBridge() {
		return scanToken(37);
	}

	private static boolean nameTokenAltAggressive() {
		return scanToken(57);
	}

	public static final void parseNewFieldName() {
		ChangeLogNewFieldNameNode newFieldNameNode = new ChangeLogNewFieldNameNode(16);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(newFieldNameNode);

		try {
			parseName();
			lexicalBuffer.closeNodeScope(newFieldNameNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(newFieldNameNode);
					nodeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					lexicalBuffer.closeNodeScope(newFieldNameNode, true);
				}

				throw throwable2;
			}
		}
	}

	public static final void parsePackageFlowObfuscationData() {
		ChangeLogPackageFlowObfuscationDataNode packageFlowObfuscationDataNode = new ChangeLogPackageFlowObfuscationDataNode(42);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(packageFlowObfuscationDataNode);

		try {
			consumeToken(62);
			parseFlowObfuscationClassName();
			switch (nextToken.kind) {
				case 46:
					consumeToken(46);
					parseFlowObfuscationPackageName();
					break;
				default:
					la1Generation[55] = generationCounter;
			}

			switch (nextToken.kind) {
				case 29:
					consumeToken(29);
					parseFieldData();
					break;
				default:
					la1Generation[56] = generationCounter;
			}

			lexicalBuffer.closeNodeScope(packageFlowObfuscationDataNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(packageFlowObfuscationDataNode);
					nodeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					lexicalBuffer.closeNodeScope(packageFlowObfuscationDataNode, true);
				}

				throw throwable2;
			}
		}
	}

	private static boolean classModifierAltAbstract() {
		return scanToken(49);
	}

	public static final ChangeLogsNodeBase parseInput() {
		ChangeLogInputNode inputNode = new ChangeLogInputNode(0);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(inputNode);

		try {
			try {
				switch (nextToken.kind) {
					case 13:
						consumeToken(13);
						break;
					default:
						la1Generation[0] = generationCounter;
				}

				while (true) {
					switch (nextToken.kind) {
						case 41:
							parseModuleChange();
							break;
						default:
							la1Generation[1] = generationCounter;

							while (true) {
								switch (nextToken.kind) {
									case 46:
										parsePackageChange();
										break;
									default:
										la1Generation[2] = generationCounter;

										while (true) {
											switch (nextToken.kind) {
												case 35:
													parseClassChange();
													break;
												default:
													la1Generation[3] = generationCounter;

													while (true) {
														switch (nextToken.kind) {
															case 69:
																parseAutoReflectionClassName();
																break;
															default:
																la1Generation[4] = generationCounter;

																while (true) {
																	switch (nextToken.kind) {
																		case 70:
																			parseObfuscateReferencesClassName();
																			break;
																		default:
																			la1Generation[5] = generationCounter;

																			while (true) {
																				switch (nextToken.kind) {
																					case 71:
																						parseMethodParameterClassNames();
																						break;
																					default:
																						la1Generation[6] = generationCounter;
																						label121:
																						switch (nextToken.kind) {
																							case 62:
																							case 66:
																								switch (nextToken.kind) {
																									case 62:
																										while (true) {
																											parsePackageFlowObfuscationData();
																											switch (nextToken.kind) {
																												case 62:
																													break;
																												default:
																													la1Generation[10] = generationCounter;

																													while (true) {
																														switch (nextToken.kind) {
																															case 60:
																																parseMemberFlowObfuscationData();
																																break;
																															default:
																																la1Generation[11] = generationCounter;
																																break label121;
																														}
																													}
																											}
																										}
																									case 66:
																										while (true) {
																											parseMainFlowObfuscationData();
																											switch (nextToken.kind) {
																												case 66:
																													break;
																												default:
																													la1Generation[7] = generationCounter;

																													while (true) {
																														switch (nextToken.kind) {
																															case 62:
																																parsePackageFlowObfuscationData();
																																break;
																															default:
																																la1Generation[8] = generationCounter;

																																while (true) {
																																	switch (nextToken.kind) {
																																		case 60:
																																			parseMemberFlowObfuscationData();
																																			break;
																																		default:
																																			la1Generation[9] = generationCounter;
																																			break label121;
																																	}
																																}
																														}
																													}
																											}
																										}
																									default:
																										la1Generation[12] = generationCounter;
																										consumeToken(-1);
																										throw new ChangeLogsParserException();
																								}
																							default:
																								la1Generation[13] = generationCounter;
																						}

																						consumeToken(0);
																						lexicalBuffer.closeNodeScope(inputNode, true);
																						nodeOpen = false;
																						return inputNode;
																				}
																			}
																	}
																}
														}
													}
											}
										}
								}
							}
					}
				}
			} catch (Throwable throwable) {
				if (nodeOpen) {
					lexicalBuffer.clearNodeScope(inputNode);
					nodeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}

				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (!(throwable instanceof ChangeLogsParserException)) {
					throw (Error)throwable;
				} else {
					throw (ChangeLogsParserException)throwable;
				}
			}
		} catch (Throwable throwable2) {
			if (!nodeOpen) {
				throw throwable2;
			} else {
				lexicalBuffer.closeNodeScope(inputNode, true);
				throw throwable2;
			}
		}
	}

	private static boolean memberModifierAltNative() {
		return scanToken(40);
	}

	public static final void parseFieldNameChange() {
		ChangeLogFieldNameChangeNode fieldNameChangeNode = new ChangeLogFieldNameChangeNode(14);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(fieldNameChangeNode);

		try {
			while (startsFieldNameChangeModifier(4)) {
				parseMemberModifier();
			}

			if (startsFieldTypeBeforeName(Integer.MAX_VALUE)) {
				parseFieldType();
			}

			parseOldFieldName();
			switch (nextToken.kind) {
				case 24:
					consumeToken(24);
					parseNewFieldName();
					break;
				case 64:
					consumeToken(64);
					break;
				default:
					la1Generation[28] = generationCounter;
					consumeToken(-1);
					throw new ChangeLogsParserException();
			}

			switch (nextToken.kind) {
				case 63:
					parseManufactured();
					break;
				default:
					la1Generation[29] = generationCounter;
			}

			lexicalBuffer.closeNodeScope(fieldNameChangeNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(fieldNameChangeNode);
					nodeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					lexicalBuffer.closeNodeScope(fieldNameChangeNode, true);
				}

				throw throwable2;
			}
		}
	}

	private static boolean typeTokenAltTrue() {
		return scanToken(28);
	}

	public static final void parseDisregardedNameList() {
		ChangeLogDisregardedNameListNode disregardedNameListNode = new ChangeLogDisregardedNameListNode(23);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(disregardedNameListNode);

		try {
			parseName();

			while (true) {
				switch (nextToken.kind) {
					case 22:
						consumeToken(22);
						parseName();
						break;
					default:
						la1Generation[40] = generationCounter;
						lexicalBuffer.closeNodeScope(disregardedNameListNode, true);
						return;
				}
			}
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(disregardedNameListNode);
					nodeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					lexicalBuffer.closeNodeScope(disregardedNameListNode, true);
				}

				throw throwable2;
			}
		}
	}

	private static boolean memberModifierAltVolatile() {
		return scanToken(50);
	}

	public static final void parseInteger() {
		ChangeLogIntegerNode integerNode = new ChangeLogIntegerNode(38);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(integerNode);

		try {
			Token3 integerLiteralToken = consumeToken(75);
			lexicalBuffer.closeNodeScope(integerNode, true);
			nodeOpen = false;
			integerNode.setValue(integerLiteralToken.image);
		} catch (Throwable throwable) {
			if (nodeOpen) {
				lexicalBuffer.closeNodeScope(integerNode, true);
			}

			throw throwable;
		}
	}

	private static boolean memberModifierAltSynthetic() {
		return scanToken(52);
	}

	public static final void parseParameterObfuscated() {
		ChangeLogParameterObfuscatedNode parameterObfuscatedNode = new ChangeLogParameterObfuscatedNode(32);
		lexicalBuffer.openNodeScope(parameterObfuscatedNode);

		try {
			consumeToken(23);
		} finally {
			lexicalBuffer.closeNodeScope(parameterObfuscatedNode, true);
		}
	}

	private static boolean nameTokenAltLight() {
		return scanToken(32);
	}

	public static final void parseMemberModifier() {
		ChangeLogMemberModifierNode memberModifierNode = new ChangeLogMemberModifierNode(25);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(memberModifierNode);

		try {
			switch (nextToken.kind) {
				case 26:
					Token3 enumToken = consumeToken(26);
					lexicalBuffer.closeNodeScope(memberModifierNode, true);
					nodeOpen = false;
					memberModifierNode.setValue(enumToken.image);
					break;
				case 27:
				case 28:
				case 29:
				case 31:
				case 32:
				case 33:
				case 34:
				case 35:
				case 36:
				case 41:
				case 42:
				case 43:
				case 44:
				case 45:
				case 46:
				case 48:
				case 53:
				case 55:
				case 56:
				case 57:
				case 58:
				case 59:
				case 60:
				default:
					la1Generation[42] = generationCounter;
					consumeToken(-1);
					throw new ChangeLogsParserException();
				case 30:
					Token3 finalToken = consumeToken(30);
					lexicalBuffer.closeNodeScope(memberModifierNode, true);
					nodeOpen = false;
					memberModifierNode.setValue(finalToken.image);
					break;
				case 37:
					Token3 bridgeToken = consumeToken(37);
					lexicalBuffer.closeNodeScope(memberModifierNode, true);
					nodeOpen = false;
					memberModifierNode.setValue(bridgeToken.image);
					break;
				case 38:
					Token3 publicToken = consumeToken(38);
					lexicalBuffer.closeNodeScope(memberModifierNode, true);
					nodeOpen = false;
					memberModifierNode.setValue(publicToken.image);
					break;
				case 39:
					Token3 staticToken = consumeToken(39);
					lexicalBuffer.closeNodeScope(memberModifierNode, true);
					nodeOpen = false;
					memberModifierNode.setValue(staticToken.image);
					break;
				case 40:
					Token3 nativeToken = consumeToken(40);
					lexicalBuffer.closeNodeScope(memberModifierNode, true);
					nodeOpen = false;
					memberModifierNode.setValue(nativeToken.image);
					break;
				case 47:
					Token3 privateToken = consumeToken(47);
					lexicalBuffer.closeNodeScope(memberModifierNode, true);
					nodeOpen = false;
					memberModifierNode.setValue(privateToken.image);
					break;
				case 49:
					Token3 abstractToken = consumeToken(49);
					lexicalBuffer.closeNodeScope(memberModifierNode, true);
					nodeOpen = false;
					memberModifierNode.setValue(abstractToken.image);
					break;
				case 50:
					Token3 volatileToken = consumeToken(50);
					lexicalBuffer.closeNodeScope(memberModifierNode, true);
					nodeOpen = false;
					memberModifierNode.setValue(volatileToken.image);
					break;
				case 51:
					Token3 protectedToken = consumeToken(51);
					lexicalBuffer.closeNodeScope(memberModifierNode, true);
					nodeOpen = false;
					memberModifierNode.setValue(protectedToken.image);
					break;
				case 52:
					Token3 syntheticToken = consumeToken(52);
					lexicalBuffer.closeNodeScope(memberModifierNode, true);
					nodeOpen = false;
					memberModifierNode.setValue(syntheticToken.image);
					break;
				case 54:
					Token3 transientToken = consumeToken(54);
					lexicalBuffer.closeNodeScope(memberModifierNode, true);
					nodeOpen = false;
					memberModifierNode.setValue(transientToken.image);
					break;
				case 61:
					Token3 synchronizedToken = consumeToken(61);
					lexicalBuffer.closeNodeScope(memberModifierNode, true);
					nodeOpen = false;
					memberModifierNode.setValue(synchronizedToken.image);
			}
		} finally {
			if (nodeOpen) {
				lexicalBuffer.closeNodeScope(memberModifierNode, true);
			}
		}
	}

	private static boolean nameTokenAltNameNotChanged() {
		return scanToken(64);
	}

	public static final void parseAutoReflectionClassName() {
		ChangeLogAutoReflectionClassNameNode autoReflectionClassNameNode = new ChangeLogAutoReflectionClassNameNode(1);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(autoReflectionClassNameNode);

		try {
			consumeToken(69);
			parseOldClassName();
			switch (nextToken.kind) {
				case 41:
					consumeToken(41);
					parseOldModuleName();
					break;
				default:
					la1Generation[14] = generationCounter;
			}

			lexicalBuffer.closeNodeScope(autoReflectionClassNameNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(autoReflectionClassNameNode);
					nodeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					lexicalBuffer.closeNodeScope(autoReflectionClassNameNode, true);
				}

				throw throwable2;
			}
		}
	}

	private static boolean startsNewMethodSignature(int lookaheadDepth) {
		lookaheadRemaining = lookaheadDepth;
		lastPos = scanPos = token;

		try {
			return !matchesNewMethodSignature();
		} catch (TokenMgrError3 ignored) {
		} finally {
			saveLookaheadState(5, lookaheadDepth);
		}

		return true;
	}

	public static final void parseOldPackageName() {
		ChangeLogOldPackageNameNode oldPackageNameNode = new ChangeLogOldPackageNameNode(9);
		boolean nodeOpen = true;
		lexicalBuffer.openNodeScope(oldPackageNameNode);

		try {
			parseNameList();
			lexicalBuffer.closeNodeScope(oldPackageNameNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(oldPackageNameNode);
					nodeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable throwable2) {
				if (nodeOpen) {
					lexicalBuffer.closeNodeScope(oldPackageNameNode, true);
				}

				throw throwable2;
			}
		}
	}

	public static final void parsePackageChange() {
		ChangeLogPackageChangeNode packageChangeNode = new ChangeLogPackageChangeNode(5);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(packageChangeNode);

		try {
			consumeToken(46);
			parseOldPackageName();
			label31:
			switch (nextToken.kind) {
				case 24:
					consumeToken(24);
					switch (nextToken.kind) {
						case 25:
						case 26:
						case 27:
						case 28:
						case 30:
						case 31:
						case 32:
						case 33:
						case 34:
						case 36:
						case 37:
						case 38:
						case 40:
						case 42:
						case 43:
						case 45:
						case 47:
						case 48:
						case 50:
						case 51:
						case 52:
						case 53:
						case 57:
						case 58:
						case 59:
						case 64:
						case 67:
						case 75:
						case 76:
						case 77:
							parseNewPackageName();
							break label31;
						case 29:
						case 35:
						case 39:
						case 41:
						case 44:
						case 46:
						case 49:
						case 54:
						case 55:
						case 56:
						case 60:
						case 61:
						case 62:
						case 63:
						case 65:
						case 66:
						case 68:
						case 69:
						case 70:
						case 71:
						case 72:
						case 73:
						case 74:
						default:
							la1Generation[17] = generationCounter;
							break label31;
					}
				case 64:
					parsePackageNameNotChanged();
					break;
				default:
					la1Generation[18] = generationCounter;
					consumeToken(-1);
					throw new ChangeLogsParserException();
			}

			lexicalBuffer.closeNodeScope(packageChangeNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(packageChangeNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(packageChangeNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private static boolean memberModifierAltSynchronized() {
		return scanToken(61);
	}

	public static final void parseOldLineNumber() {
		ChangeLogOldLineNumberNode oldLineNumberNode = new ChangeLogOldLineNumberNode(36);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(oldLineNumberNode);

		try {
			parseInteger();
			lexicalBuffer.closeNodeScope(oldLineNumberNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(oldLineNumberNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(oldLineNumberNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private static boolean matchesFieldTypePrefix() {
		return matchesQualifiedType();
	}

	private static boolean nameTokenAltTrue() {
		return scanToken(28);
	}

	public static final void parseMainFlowObfuscationData() {
		ChangeLogMainFlowObfuscationDataNode mainFlowObfuscationDataNode = new ChangeLogMainFlowObfuscationDataNode(41);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(mainFlowObfuscationDataNode);

		try {
			consumeToken(66);
			parseFlowObfuscationClassName();
			switch (nextToken.kind) {
				case 29:
					consumeToken(29);
					parseFieldData();
					break;
				default:
					la1Generation[54] = generationCounter;
			}

			lexicalBuffer.closeNodeScope(mainFlowObfuscationDataNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(mainFlowObfuscationDataNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(mainFlowObfuscationDataNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private static boolean matchesNameOrKeywordToken() {
		Token3 savedScanPos = scanPos;
		if (nameTokenAltName()) {
			scanPos = savedScanPos;
			if (nameTokenAltAlphanumeric()) {
				scanPos = savedScanPos;
				if (nameTokenNextIsPackageInfo()) {
					scanPos = savedScanPos;
					if (nameTokenNextIsIntegerLiteral()) {
						scanPos = savedScanPos;
						if (nameTokenAltAnd()) {
							scanPos = savedScanPos;
							if (nameTokenAltNameNotChanged()) {
								scanPos = savedScanPos;
								if (nameTokenAltSignatureNotChanged()) {
									scanPos = savedScanPos;
									if (nameTokenNextIsAnnotation()) {
										scanPos = savedScanPos;
										if (nameTokenNextIsBridge()) {
											scanPos = savedScanPos;
											if (nameTokenNextIsEnum()) {
												scanPos = savedScanPos;
												if (nameTokenNextIsSynthetic()) {
													scanPos = savedScanPos;
													if (nameTokenNextIsFinal()) {
														scanPos = savedScanPos;
														if (nameTokenNextIsPublic()) {
															scanPos = savedScanPos;
															if (nameTokenNextIsNative()) {
																scanPos = savedScanPos;
																if (nameTokenNextIsPrivate()) {
																	scanPos = savedScanPos;
																	if (nameTokenNextIsVolatile()) {
																		scanPos = savedScanPos;
																		if (nameTokenNextIsProtected()) {
																			scanPos = savedScanPos;
																			if (nameTokenNextIsInterface()) {
																				scanPos = savedScanPos;
																				if (nameTokenNextIsEnhanced()) {
																					scanPos = savedScanPos;
																					if (nameTokenAltLight()) {
																						scanPos = savedScanPos;
																						if (nameTokenNextIsHeavy()) {
																							scanPos = savedScanPos;
																							if (nameTokenAltNormal()) {
																								scanPos = savedScanPos;
																								if (nameTokenAltAggressive()) {
																									scanPos = savedScanPos;
																									if (nameTokenAltNone()) {
																										scanPos = savedScanPos;
																										if (nameTokenAltExecute()) {
																											scanPos = savedScanPos;
																											if (nameTokenNextIsExclude()) {
																												scanPos = savedScanPos;
																												if (nameTokenNextIsFalse()) {
																													scanPos = savedScanPos;
																													if (nameTokenAltTrue()) {
																														scanPos = savedScanPos;
																														if (nameTokenNextIsInit()) {
																															scanPos = savedScanPos;
																															if (nameTokenAltClinit()) {
																																return true;
																															}
																														}
																													}
																												}
																											}
																										}
																									}
																								}
																							}
																						}
																					}
																				}
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		return false;
	}

	public static final void parseNewLineNumber() {
		ChangeLogNewLineNumberNode newLineNumberNode = new ChangeLogNewLineNumberNode(37);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(newLineNumberNode);

		try {
			parseInteger();
			lexicalBuffer.closeNodeScope(newLineNumberNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(newLineNumberNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(newLineNumberNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private static boolean nameTokenAltName() {
		return scanToken(77);
	}

	public static final void parseClassModifier() {
		ChangeLogClassModifierNode classModifierNode = new ChangeLogClassModifierNode(24);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(classModifierNode);

		try {
			switch (nextToken.kind) {
				case 26:
					Token3 enumToken = consumeToken(26);
					lexicalBuffer.closeNodeScope(classModifierNode, true);
					nodeScopeOpen = false;
					classModifierNode.setValue(enumToken.image);
					break;
				case 30:
					Token3 finalToken = consumeToken(30);
					lexicalBuffer.closeNodeScope(classModifierNode, true);
					nodeScopeOpen = false;
					classModifierNode.setValue(finalToken.image);
					break;
				case 38:
					Token3 publicToken = consumeToken(38);
					lexicalBuffer.closeNodeScope(classModifierNode, true);
					nodeScopeOpen = false;
					classModifierNode.setValue(publicToken.image);
					break;
				case 49:
					Token3 abstractToken = consumeToken(49);
					lexicalBuffer.closeNodeScope(classModifierNode, true);
					nodeScopeOpen = false;
					classModifierNode.setValue(abstractToken.image);
					break;
				case 52:
					Token3 syntheticToken = consumeToken(52);
					lexicalBuffer.closeNodeScope(classModifierNode, true);
					nodeScopeOpen = false;
					classModifierNode.setValue(syntheticToken.image);
					break;
				case 53:
					Token3 interfaceToken = consumeToken(53);
					lexicalBuffer.closeNodeScope(classModifierNode, true);
					nodeScopeOpen = false;
					classModifierNode.setValue(interfaceToken.image);
					break;
				case 58:
					Token3 annotationToken = consumeToken(58);
					lexicalBuffer.closeNodeScope(classModifierNode, true);
					nodeScopeOpen = false;
					classModifierNode.setValue(annotationToken.image);
					break;
				default:
					la1Generation[41] = generationCounter;
					consumeToken(-1);
					throw new ChangeLogsParserException();
			}
		} finally {
			if (nodeScopeOpen) {
				lexicalBuffer.closeNodeScope(classModifierNode, true);
			}
		}
	}

	public static final void parseMethodReturnType() {
		ChangeLogMethodReturnTypeNode methodReturnTypeNode = new ChangeLogMethodReturnTypeNode(29);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(methodReturnTypeNode);

		try {
			parseQualifiedType();
			lexicalBuffer.closeNodeScope(methodReturnTypeNode, true);
		} catch (Throwable caughtThrowable) {
			Throwable throwable = caughtThrowable;

			try {
				if (true) {
					lexicalBuffer.clearNodeScope(methodReturnTypeNode);
					nodeScopeOpen = false;
				} else {
					lexicalBuffer.popNode();
				}
				if (throwable instanceof RuntimeException) {
					throw (RuntimeException)throwable;
				} else if (throwable instanceof ChangeLogsParserException) {
					throw (ChangeLogsParserException)throwable;
				} else {
					throw (Error)throwable;
				}
			} catch (Throwable rethrownThrowable) {
				if (nodeScopeOpen) {
					lexicalBuffer.closeNodeScope(methodReturnTypeNode, true);
				}

				throw rethrownThrowable;
			}
		}
	}

	private static boolean nameTokenAltExecute() {
		return scanToken(42);
	}

	static {
		initLa1TokenBits0();
		initLa1TokenBits1();
		initLa1TokenBits2();
	}
}
