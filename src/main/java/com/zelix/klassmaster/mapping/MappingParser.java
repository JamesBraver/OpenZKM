package com.zelix.klassmaster.mapping;

import com.zelix.exceptions.MappingParserException;
import com.zelix.klassmaster.mapping.base.MappingNodeBase;
import com.zelix.klassmaster.mapping.entry.MappingClassBlockNode;
import com.zelix.klassmaster.mapping.entry.MappingClassNameChangeNode;
import com.zelix.klassmaster.mapping.entry.MappingFieldNameChangeNode;
import com.zelix.klassmaster.mapping.entry.MappingInputNode;
import com.zelix.klassmaster.mapping.entry.MappingMethodNameChangeNode;
import com.zelix.klassmaster.mapping.javaccInterface.MappingConstants;
import com.zelix.klassmaster.mapping.javaccInterface.MappingTokenConstants;
import com.zelix.klassmaster.mapping.name.MappingMethodNameNode;
import com.zelix.klassmaster.mapping.name.MappingNameListNode;
import com.zelix.klassmaster.mapping.name.MappingNameNode;
import com.zelix.klassmaster.mapping.name.MappingNewClassNameNode;
import com.zelix.klassmaster.mapping.name.MappingNewFieldNameNode;
import com.zelix.klassmaster.mapping.name.MappingNewMethodNameNode;
import com.zelix.klassmaster.mapping.name.MappingOldClassNameNode;
import com.zelix.klassmaster.mapping.name.MappingOldFieldNameNode;
import com.zelix.klassmaster.mapping.signature.MappingArrayLevelNode;
import com.zelix.klassmaster.mapping.signature.MappingMethodArgumentsNode;
import com.zelix.klassmaster.mapping.signature.MappingMethodSignatureNode;
import com.zelix.klassmaster.mapping.signature.MappingQualifiedTypeNode;
import com.zelix.klassmaster.mapping.signature.MappingTypeNode;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class MappingParser implements MappingConstants, MappingTokenConstants {
	protected static LexicalBuffer4 lexicalBuffer = new LexicalBuffer4();
	private static boolean initializedOnce = false;
	private static final int[] la1Generation = new int[15];
	private static final LookaheadRecord[] lookaheadRecords = new LookaheadRecord[2];
	private static boolean rescanning = false;
	private static int lookaheadGcCounter = 0;
	private static final TokenMgrError4 lookaheadSuccess = new TokenMgrError4();
	private static List<int[]> expectedTokenSequences = new ArrayList<>();
	private static int errorTokenKind = -1;
	private static int[] errorTokenKinds = new int[100];
	private static int[] la1TokenBits0;
	private static int lookaheadRemaining;
	public static Token4 token;
	public static MappingParser instance;
	private static int[] errorTokenSequence;
	private static Token4 scanPos;
	public static Token4 nextToken;
	public static MappingTokenManager tokenSource;
	public static SimpleCharStream4 inputStream;
	private static Token4 lastPos;
	private static int generationCounter;
	private static int errorTokenEndPos;

	public MappingParser(Reader reader) {
		if (initializedOnce) {
			System.out.println("ERROR: Second call to constructor of static parser. ");
			throw new Error();
		} else {
			initializedOnce = true;
			inputStream = new SimpleCharStream4(reader, 1, 1);
			tokenSource = new MappingTokenManager(inputStream);
			token = new Token4();
			token.next = nextToken = MappingTokenManager.getNextToken();
			generationCounter = 0;

			for (int i = 0; i < 15; i++) {
				la1Generation[i] = -1;
			}

			for (int recordIndex = 0; recordIndex < lookaheadRecords.length; recordIndex++) {
				lookaheadRecords[recordIndex] = new LookaheadRecord();
			}
		}
	}

	public static final void parseOldFieldName() {
		MappingOldFieldNameNode oldFieldNameNode = new MappingOldFieldNameNode(7);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(oldFieldNameNode);
		boolean controlFlowSemaphore = false;

		try {
			controlFlowSemaphore = true;
			parseName();
			controlFlowSemaphore = false;
		} catch (Throwable caughtThrowable) {
			if (true) {
				lexicalBuffer.clearNodeScope(oldFieldNameNode);
				nodeScopeOpen = false;
			} else {
				lexicalBuffer.popNode();
			}
			if (caughtThrowable instanceof RuntimeException) {
				throw (RuntimeException)caughtThrowable;
			}

			if (caughtThrowable instanceof MappingParserException) {
				throw (MappingParserException)caughtThrowable;
			}

			throw (Error)caughtThrowable;
		} finally {
			if (controlFlowSemaphore && nodeScopeOpen) {
				lexicalBuffer.closeNodeScope(oldFieldNameNode, true);
			}
		}

		lexicalBuffer.closeNodeScope(oldFieldNameNode, true);
	}

	public static final void parseMethodSignature() {
		MappingMethodSignatureNode methodSignatureNode = new MappingMethodSignatureNode(11);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(methodSignatureNode);
		boolean controlFlowSemaphore = false;

		try {
			controlFlowSemaphore = true;
			parseMethodName();
			consumeToken(14);
			switch (nextToken.kind) {
				case 30:
					parseMethodArguments();
					break;
				default:
					la1Generation[9] = generationCounter;
			}

			consumeToken(15);
			controlFlowSemaphore = false;
		} catch (Throwable caughtThrowable) {
			if (true) {
				lexicalBuffer.clearNodeScope(methodSignatureNode);
				nodeScopeOpen = false;
			} else {
				lexicalBuffer.popNode();
			}
			if (caughtThrowable instanceof RuntimeException) {
				throw (RuntimeException)caughtThrowable;
			}

			if (caughtThrowable instanceof MappingParserException) {
				throw (MappingParserException)caughtThrowable;
			}

			throw (Error)caughtThrowable;
		} finally {
			if (controlFlowSemaphore && nodeScopeOpen) {
				lexicalBuffer.closeNodeScope(methodSignatureNode, true);
			}
		}

		lexicalBuffer.closeNodeScope(methodSignatureNode, true);
	}

	private static boolean nextIsOpenOrCloseBracket() {
		return scanToken(16) ? true : scanToken(17);
	}

	public static final void parseName() {
		MappingNameNode nameNode = new MappingNameNode(15);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(nameNode);

		try {
			switch (nextToken.kind) {
				case 25:
					Token4 packageInfoToken = consumeToken(25);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeScopeOpen = false;
					nameNode.setName(packageInfoToken.image);
					break;
				case 29:
					Token4 integerToken = consumeToken(29);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeScopeOpen = false;
					nameNode.setName(integerToken.image);
					break;
				case 30:
					Token4 nameToken = consumeToken(30);
					lexicalBuffer.closeNodeScope(nameNode, true);
					nodeScopeOpen = false;
					nameNode.setName(nameToken.image);
					break;
				default:
					la1Generation[14] = generationCounter;
					consumeToken(-1);
					throw new MappingParserException();
			}
		} finally {
			if (nodeScopeOpen) {
				lexicalBuffer.closeNodeScope(nameNode, true);
			}
		}
	}

	public static final void parseClassBlock() {
		MappingClassBlockNode classBlockNode = new MappingClassBlockNode(1);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(classBlockNode);
		boolean controlFlowSemaphore = false;

		try {
			controlFlowSemaphore = true;
			parseClassNameChange();

			label125:
			while (true) {
				switch (nextToken.kind) {
					case 12:
						consumeToken(12);
						break;
					case 13:
						consumeToken(13);
						break;
					default:
						la1Generation[2] = generationCounter;
						consumeToken(-1);
						throw new MappingParserException();
				}

				switch (nextToken.kind) {
					case 12:
					case 13:
						break;
					default:
						la1Generation[3] = generationCounter;
						if (matchesFieldNameChange(Integer.MAX_VALUE)) {
							parseFieldNameChange();

							while (true) {
								switch (nextToken.kind) {
									case 12:
										consumeToken(12);
										break;
									case 13:
										consumeToken(13);
										break;
									default:
										la1Generation[4] = generationCounter;
										consumeToken(-1);
										throw new MappingParserException();
								}

								switch (nextToken.kind) {
									case 12:
									case 13:
								}
							}
						}

						label137:
						while (matchesMethodNameChange(Integer.MAX_VALUE)) {
							parseMethodNameChange();

							while (true) {
								switch (nextToken.kind) {
									case 12:
										consumeToken(12);
										break;
									case 13:
										consumeToken(13);
										break;
									default:
										la1Generation[6] = generationCounter;
										consumeToken(-1);
										throw new MappingParserException();
								}

								switch (nextToken.kind) {
									case 12:
									case 13:
										break;
									default:
										la1Generation[7] = generationCounter;
										continue label137;
								}
							}
						}

						controlFlowSemaphore = false;
						break label125;
				}
			}
		} catch (Throwable caughtThrowable) {
			if (true) {
				lexicalBuffer.clearNodeScope(classBlockNode);
				nodeScopeOpen = false;
			} else {
				lexicalBuffer.popNode();
			}
			if (caughtThrowable instanceof RuntimeException) {
				throw (RuntimeException)caughtThrowable;
			}

			if (caughtThrowable instanceof MappingParserException) {
				throw (MappingParserException)caughtThrowable;
			}

			throw (Error)caughtThrowable;
		} finally {
			if (controlFlowSemaphore && nodeScopeOpen) {
				lexicalBuffer.closeNodeScope(classBlockNode, true);
			}
		}

		lexicalBuffer.closeNodeScope(classBlockNode, true);
	}

	private static boolean nextIsArrayLevelBracket() {
		return nextIsOpenOrCloseBracket();
	}

	private static boolean nextIsIdentifierAsTypeName() {
		return scanToken(30);
	}

	private static boolean nextIsPackageInfoAsMethodName() {
		return scanToken(25);
	}

	public static final void parseType() {
		MappingTypeNode typeNode = new MappingTypeNode(16);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(typeNode);

		try {
			Token4 nameToken = consumeToken(30);
			lexicalBuffer.closeNodeScope(typeNode, true);
			nodeScopeOpen = false;
			typeNode.setName(nameToken.image);
		} finally {
			if (nodeScopeOpen) {
				lexicalBuffer.closeNodeScope(typeNode, true);
			}
		}
	}

	private static Token4 consumeToken(int kind) {
		Token4 savedToken = token;
		if ((token = nextToken).next != null) {
			nextToken = nextToken.next;
		} else {
			nextToken = nextToken.next = MappingTokenManager.getNextToken();
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

	private static boolean nextIsIdentifierAsMethodName() {
		return scanToken(30);
	}

	public static final void parseFieldNameChange() {
		MappingFieldNameChangeNode fieldNameChangeNode = new MappingFieldNameChangeNode(3);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(fieldNameChangeNode);
		boolean controlFlowSemaphore = false;

		try {
			controlFlowSemaphore = true;
			parseQualifiedType();
			parseOldFieldName();
			consumeToken(22);
			parseNewFieldName();
			controlFlowSemaphore = false;
		} catch (Throwable caughtThrowable) {
			if (true) {
				lexicalBuffer.clearNodeScope(fieldNameChangeNode);
				nodeScopeOpen = false;
			} else {
				lexicalBuffer.popNode();
			}
			if (caughtThrowable instanceof RuntimeException) {
				throw (RuntimeException)caughtThrowable;
			}

			if (caughtThrowable instanceof MappingParserException) {
				throw (MappingParserException)caughtThrowable;
			}

			throw (Error)caughtThrowable;
		} finally {
			if (controlFlowSemaphore && nodeScopeOpen) {
				lexicalBuffer.closeNodeScope(fieldNameChangeNode, true);
			}
		}

		lexicalBuffer.closeNodeScope(fieldNameChangeNode, true);
	}

	private static boolean matchesFieldNameChangeExpansion() {
		if (matchesQualifiedType()) {
			return true;
		} else if (nextIsName()) {
			return true;
		} else if (scanToken(22)) {
			return true;
		} else if (nextIsName()) {
			return true;
		} else {
			Token4 savedScanPos = scanPos;
			if (scanToken(12)) {
				scanPos = savedScanPos;
				if (scanToken(13)) {
					return true;
				}
			}

			return false;
		}
	}

	public static void reInit(Reader reader) {
		inputStream.ReInit(reader, 1, 1);
		MappingTokenManager.ReInit(inputStream);
		token = new Token4();
		token.next = nextToken = MappingTokenManager.getNextToken();
		lexicalBuffer.clear();
		generationCounter = 0;

		for (int i = 0; i < 15; i++) {
			la1Generation[i] = -1;
		}

		for (int recordIndex = 0; recordIndex < lookaheadRecords.length; recordIndex++) {
			lookaheadRecords[recordIndex] = new LookaheadRecord();
		}
	}

	public static final void parseArrayLevel() {
		MappingArrayLevelNode arrayLevelNode = new MappingArrayLevelNode(17);
		lexicalBuffer.openNodeScope(arrayLevelNode);

		try {
			consumeToken(16);
			consumeToken(17);
		} finally {
			lexicalBuffer.closeNodeScope(arrayLevelNode, true);
		}
	}

	public static final void parseNewFieldName() {
		MappingNewFieldNameNode newFieldNameNode = new MappingNewFieldNameNode(8);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(newFieldNameNode);
		boolean controlFlowSemaphore = false;

		try {
			controlFlowSemaphore = true;
			parseName();
			controlFlowSemaphore = false;
		} catch (Throwable caughtThrowable) {
			if (true) {
				lexicalBuffer.clearNodeScope(newFieldNameNode);
				nodeScopeOpen = false;
			} else {
				lexicalBuffer.popNode();
			}
			if (caughtThrowable instanceof RuntimeException) {
				throw (RuntimeException)caughtThrowable;
			}

			if (caughtThrowable instanceof MappingParserException) {
				throw (MappingParserException)caughtThrowable;
			}

			throw (Error)caughtThrowable;
		} finally {
			if (controlFlowSemaphore && nodeScopeOpen) {
				lexicalBuffer.closeNodeScope(newFieldNameNode, true);
			}
		}

		lexicalBuffer.closeNodeScope(newFieldNameNode, true);
	}

	private static void rescanToken() {
		rescanning = true;

		for (int recordIndex = 0; recordIndex < 2; recordIndex++) {
			try {
				LookaheadRecord lookaheadRecord = lookaheadRecords[recordIndex];

				while (true) {
					if (lookaheadRecord.generation > generationCounter) {
						lookaheadRemaining = lookaheadRecord.lookaheadCount;
						lastPos = scanPos = lookaheadRecord.first;
						switch (recordIndex) {
							case 0:
								matchesFieldNameChangeExpansion();
								break;
							case 1:
								matchesMethodNameChangeExpansion();
						}
					}

					lookaheadRecord = lookaheadRecord.next;
					if (lookaheadRecord == null) {
						break;
					}
				}
			} catch (TokenMgrError4 ignored) {
			}
		}

		rescanning = false;
	}

	private static boolean matchesMethodNameChangeExpansion() {
		if (matchesQualifiedType()) {
			return true;
		} else {
			return nextIsMethodName() ? true : scanToken(14);
		}
	}

	private static boolean nextIsDotOrTypeName() {
		return scanToken(20) ? true : nextIsIdentifierAsTypeName();
	}

	private static boolean nextIsIdentifierAsName() {
		return scanToken(30);
	}

	private static boolean matchesQualifiedType() {
		if (nextIsIdentifierAsTypeName()) {
			return true;
		} else {
			Token4 savedScanPos;
			do {
				savedScanPos = scanPos;
			} while (!nextIsDotOrTypeName());

			scanPos = savedScanPos;

			do {
				savedScanPos = scanPos;
			} while (!nextIsArrayLevelBracket());

			scanPos = savedScanPos;
			return false;
		}
	}

	private static boolean nextIsPackageInfoAsName() {
		return scanToken(25);
	}

	public static final void parseQualifiedType() {
		MappingQualifiedTypeNode qualifiedTypeNode = new MappingQualifiedTypeNode(13);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(qualifiedTypeNode);
		boolean controlFlowSemaphore = false;

		try {
			controlFlowSemaphore = true;
			parseType();

			label81:
			while (true) {
				switch (nextToken.kind) {
					case 20:
						consumeToken(20);
						parseType();
						break;
					default:
						la1Generation[11] = generationCounter;

						while (true) {
							switch (nextToken.kind) {
								case 16:
									parseArrayLevel();
									break;
								default:
									la1Generation[12] = generationCounter;
									controlFlowSemaphore = false;
									break label81;
							}
						}
				}
			}
		} catch (Throwable caughtThrowable) {
			if (true) {
				lexicalBuffer.clearNodeScope(qualifiedTypeNode);
				nodeScopeOpen = false;
			} else {
				lexicalBuffer.popNode();
			}
			if (caughtThrowable instanceof RuntimeException) {
				throw (RuntimeException)caughtThrowable;
			}

			if (caughtThrowable instanceof MappingParserException) {
				throw (MappingParserException)caughtThrowable;
			}

			throw (Error)caughtThrowable;
		} finally {
			if (controlFlowSemaphore && nodeScopeOpen) {
				lexicalBuffer.closeNodeScope(qualifiedTypeNode, true);
			}
		}

		lexicalBuffer.closeNodeScope(qualifiedTypeNode, true);
	}

	private static boolean matchesFieldNameChange(int lookaheadDepth) {
		lookaheadRemaining = lookaheadDepth;
		lastPos = scanPos = token;

		try {
			return !matchesFieldNameChangeExpansion();
		} catch (TokenMgrError4 ignored) {
		} finally {
			saveLookaheadState(0, lookaheadDepth);
		}

		return true;
	}

	public static final void parseClassNameChange() {
		MappingClassNameChangeNode classNameChangeNode = new MappingClassNameChangeNode(2);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(classNameChangeNode);
		boolean controlFlowSemaphore = false;

		try {
			controlFlowSemaphore = true;
			parseOldClassName();
			consumeToken(22);
			parseNewClassName();
			consumeToken(21);
			controlFlowSemaphore = false;
		} catch (Throwable caughtThrowable) {
			if (true) {
				lexicalBuffer.clearNodeScope(classNameChangeNode);
				nodeScopeOpen = false;
			} else {
				lexicalBuffer.popNode();
			}
			if (caughtThrowable instanceof RuntimeException) {
				throw (RuntimeException)caughtThrowable;
			}

			if (caughtThrowable instanceof MappingParserException) {
				throw (MappingParserException)caughtThrowable;
			}

			throw (Error)caughtThrowable;
		} finally {
			if (controlFlowSemaphore && nodeScopeOpen) {
				lexicalBuffer.closeNodeScope(classNameChangeNode, true);
			}
		}

		lexicalBuffer.closeNodeScope(classNameChangeNode, true);
	}

	private static void initLa1TokenBits0() {
		la1TokenBits0 = new int[]{2048, 1644167168, 12288, 12288, 12288, 12288, 12288, 12288, 1048576, 1073741824, 262144, 1048576, 65536, 1669332992, 1644167168};
	}

	private static boolean nextIsMethodName() {
		Token4 savedScanPos = scanPos;
		if (nextIsIdentifierAsMethodName()) {
			scanPos = savedScanPos;
			if (nextIsIntegerLiteralAsMethodName()) {
				scanPos = savedScanPos;
				if (nextIsInitAsMethodName()) {
					scanPos = savedScanPos;
					if (nextIsClinitAsMethodName()) {
						scanPos = savedScanPos;
						if (nextIsPackageInfoAsMethodName()) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	public static final void parseMethodNameChange() {
		MappingMethodNameChangeNode methodNameChangeNode = new MappingMethodNameChangeNode(4);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(methodNameChangeNode);
		boolean controlFlowSemaphore = false;

		try {
			controlFlowSemaphore = true;
			parseQualifiedType();
			parseMethodSignature();
			consumeToken(22);
			parseNewMethodName();
			controlFlowSemaphore = false;
		} catch (Throwable caughtThrowable) {
			if (true) {
				lexicalBuffer.clearNodeScope(methodNameChangeNode);
				nodeScopeOpen = false;
			} else {
				lexicalBuffer.popNode();
			}
			if (caughtThrowable instanceof RuntimeException) {
				throw (RuntimeException)caughtThrowable;
			}

			if (caughtThrowable instanceof MappingParserException) {
				throw (MappingParserException)caughtThrowable;
			}

			throw (Error)caughtThrowable;
		} finally {
			if (controlFlowSemaphore && nodeScopeOpen) {
				lexicalBuffer.closeNodeScope(methodNameChangeNode, true);
			}
		}

		lexicalBuffer.closeNodeScope(methodNameChangeNode, true);
	}

	public static final void parseMethodName() {
		MappingMethodNameNode methodNameNode = new MappingMethodNameNode(14);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(methodNameNode);

		try {
			switch (nextToken.kind) {
				case 23:
					Token4 initToken = consumeToken(23);
					lexicalBuffer.closeNodeScope(methodNameNode, true);
					nodeScopeOpen = false;
					methodNameNode.setName(initToken.image);
					break;
				case 24:
					Token4 clinitToken = consumeToken(24);
					lexicalBuffer.closeNodeScope(methodNameNode, true);
					nodeScopeOpen = false;
					methodNameNode.setName(clinitToken.image);
					break;
				case 25:
					Token4 packageInfoToken = consumeToken(25);
					lexicalBuffer.closeNodeScope(methodNameNode, true);
					nodeScopeOpen = false;
					methodNameNode.setName(packageInfoToken.image);
					break;
				case 26:
				case 27:
				case 28:
				default:
					la1Generation[13] = generationCounter;
					consumeToken(-1);
					throw new MappingParserException();
				case 29:
					Token4 integerToken = consumeToken(29);
					lexicalBuffer.closeNodeScope(methodNameNode, true);
					nodeScopeOpen = false;
					methodNameNode.setName(integerToken.image);
					break;
				case 30:
					Token4 nameToken = consumeToken(30);
					lexicalBuffer.closeNodeScope(methodNameNode, true);
					nodeScopeOpen = false;
					methodNameNode.setName(nameToken.image);
			}
		} finally {
			if (nodeScopeOpen) {
				lexicalBuffer.closeNodeScope(methodNameNode, true);
			}
		}
	}

	private static boolean nextIsIntegerLiteralAsMethodName() {
		return scanToken(29);
	}

	public static final void parseNameList() {
		MappingNameListNode nameListNode = new MappingNameListNode(10);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(nameListNode);
		boolean controlFlowSemaphore = false;

		try {
			controlFlowSemaphore = true;
			parseName();

			label71:
			while (true) {
				switch (nextToken.kind) {
					case 20:
						consumeToken(20);
						parseName();
						break;
					default:
						la1Generation[8] = generationCounter;
						controlFlowSemaphore = false;
						break label71;
				}
			}
		} catch (Throwable caughtThrowable) {
			if (true) {
				lexicalBuffer.clearNodeScope(nameListNode);
				nodeScopeOpen = false;
			} else {
				lexicalBuffer.popNode();
			}
			if (caughtThrowable instanceof RuntimeException) {
				throw (RuntimeException)caughtThrowable;
			}

			if (caughtThrowable instanceof MappingParserException) {
				throw (MappingParserException)caughtThrowable;
			}

			throw (Error)caughtThrowable;
		} finally {
			if (controlFlowSemaphore && nodeScopeOpen) {
				lexicalBuffer.closeNodeScope(nameListNode, true);
			}
		}

		lexicalBuffer.closeNodeScope(nameListNode, true);
	}

	public static final void parseOldClassName() {
		MappingOldClassNameNode oldClassNameNode = new MappingOldClassNameNode(5);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(oldClassNameNode);
		boolean controlFlowSemaphore = false;

		try {
			controlFlowSemaphore = true;
			parseNameList();
			controlFlowSemaphore = false;
		} catch (Throwable caughtThrowable) {
			if (true) {
				lexicalBuffer.clearNodeScope(oldClassNameNode);
				nodeScopeOpen = false;
			} else {
				lexicalBuffer.popNode();
			}
			if (caughtThrowable instanceof RuntimeException) {
				throw (RuntimeException)caughtThrowable;
			}

			if (caughtThrowable instanceof MappingParserException) {
				throw (MappingParserException)caughtThrowable;
			}

			throw (Error)caughtThrowable;
		} finally {
			if (controlFlowSemaphore && nodeScopeOpen) {
				lexicalBuffer.closeNodeScope(oldClassNameNode, true);
			}
		}

		lexicalBuffer.closeNodeScope(oldClassNameNode, true);
	}

	private static boolean nextIsClinitAsMethodName() {
		return scanToken(24);
	}

	private static void saveLookaheadState(int specIndex, int lookaheadDepth) {
		LookaheadRecord lookaheadRecord;
		for (lookaheadRecord = lookaheadRecords[specIndex]; lookaheadRecord.generation > generationCounter; lookaheadRecord = lookaheadRecord.next) {
			if (lookaheadRecord.next == null) {
				lookaheadRecord = lookaheadRecord.next = new LookaheadRecord();
				break;
			}
		}

		lookaheadRecord.generation = generationCounter + lookaheadDepth - lookaheadRemaining;
		lookaheadRecord.first = token;
		lookaheadRecord.lookaheadCount = lookaheadDepth;
	}

	private static boolean nextIsName() {
		Token4 savedScanPos = scanPos;
		if (nextIsIdentifierAsName()) {
			scanPos = savedScanPos;
			if (nextIsIntegerLiteralAsName()) {
				scanPos = savedScanPos;
				if (nextIsPackageInfoAsName()) {
					return true;
				}
			}
		}

		return false;
	}

	public static final MappingNodeBase parseInput() {
		MappingInputNode inputNode = new MappingInputNode(0);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(inputNode);

		try {
			switch (nextToken.kind) {
				case 11:
					consumeToken(11);
					break;
				default:
					la1Generation[0] = generationCounter;
			}

			while (true) {
				switch (nextToken.kind) {
					case 25:
					case 29:
					case 30:
						parseClassBlock();
						break;
					default:
						la1Generation[1] = generationCounter;
						consumeToken(0);
						lexicalBuffer.closeNodeScope(inputNode, true);
						nodeScopeOpen = false;
						return inputNode;
				}
			}
		} catch (Throwable caughtThrowable) {
			if (nodeScopeOpen) {
				lexicalBuffer.clearNodeScope(inputNode);
				nodeScopeOpen = false;
			} else {
				lexicalBuffer.popNode();
			}

			if (caughtThrowable instanceof RuntimeException) {
				throw (RuntimeException)caughtThrowable;
			} else if (caughtThrowable instanceof MappingParserException) {
				throw (MappingParserException)caughtThrowable;
			} else {
				throw (Error)caughtThrowable;
			}
		} finally {
			if (nodeScopeOpen) {
				lexicalBuffer.closeNodeScope(inputNode, true);
			}
		}
	}

	public static final void parseNewClassName() {
		MappingNewClassNameNode newClassNameNode = new MappingNewClassNameNode(6);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(newClassNameNode);
		boolean controlFlowSemaphore = false;

		try {
			controlFlowSemaphore = true;
			parseNameList();
			controlFlowSemaphore = false;
		} catch (Throwable caughtThrowable) {
			if (true) {
				lexicalBuffer.clearNodeScope(newClassNameNode);
				nodeScopeOpen = false;
			} else {
				lexicalBuffer.popNode();
			}
			if (caughtThrowable instanceof RuntimeException) {
				throw (RuntimeException)caughtThrowable;
			}

			if (caughtThrowable instanceof MappingParserException) {
				throw (MappingParserException)caughtThrowable;
			}

			throw (Error)caughtThrowable;
		} finally {
			if (controlFlowSemaphore && nodeScopeOpen) {
				lexicalBuffer.closeNodeScope(newClassNameNode, true);
			}
		}

		lexicalBuffer.closeNodeScope(newClassNameNode, true);
	}

	public static final void parseMethodArguments() {
		MappingMethodArgumentsNode methodArgumentsNode = new MappingMethodArgumentsNode(12);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(methodArgumentsNode);
		boolean controlFlowSemaphore = false;

		try {
			controlFlowSemaphore = true;
			parseQualifiedType();

			label71:
			while (true) {
				switch (nextToken.kind) {
					case 18:
						consumeToken(18);
						parseQualifiedType();
						break;
					default:
						la1Generation[10] = generationCounter;
						controlFlowSemaphore = false;
						break label71;
				}
			}
		} catch (Throwable caughtThrowable) {
			if (true) {
				lexicalBuffer.clearNodeScope(methodArgumentsNode);
				nodeScopeOpen = false;
			} else {
				lexicalBuffer.popNode();
			}
			if (caughtThrowable instanceof RuntimeException) {
				throw (RuntimeException)caughtThrowable;
			}

			if (caughtThrowable instanceof MappingParserException) {
				throw (MappingParserException)caughtThrowable;
			}

			throw (Error)caughtThrowable;
		} finally {
			if (controlFlowSemaphore && nodeScopeOpen) {
				lexicalBuffer.closeNodeScope(methodArgumentsNode, true);
			}
		}

		lexicalBuffer.closeNodeScope(methodArgumentsNode, true);
	}

	private static boolean scanToken(int kind) {
		if (scanPos == lastPos) {
			lookaheadRemaining--;
			if (scanPos.next == null) {
				lastPos = scanPos = scanPos.next = MappingTokenManager.getNextToken();
			} else {
				lastPos = scanPos = scanPos.next;
			}
		} else {
			scanPos = scanPos.next;
		}

		if (rescanning) {
			int tokenPos = 0;

			Token4 scannedToken;
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

	public static final void parseNewMethodName() {
		MappingNewMethodNameNode newMethodNameNode = new MappingNewMethodNameNode(9);
		boolean nodeScopeOpen = true;
		lexicalBuffer.openNodeScope(newMethodNameNode);
		boolean controlFlowSemaphore = false;

		try {
			controlFlowSemaphore = true;
			parseMethodName();
			controlFlowSemaphore = false;
		} catch (Throwable caughtThrowable) {
			if (true) {
				lexicalBuffer.clearNodeScope(newMethodNameNode);
				nodeScopeOpen = false;
			} else {
				lexicalBuffer.popNode();
			}
			if (caughtThrowable instanceof RuntimeException) {
				throw (RuntimeException)caughtThrowable;
			}

			if (caughtThrowable instanceof MappingParserException) {
				throw (MappingParserException)caughtThrowable;
			}

			throw (Error)caughtThrowable;
		} finally {
			if (controlFlowSemaphore && nodeScopeOpen) {
				lexicalBuffer.closeNodeScope(newMethodNameNode, true);
			}
		}

		lexicalBuffer.closeNodeScope(newMethodNameNode, true);
	}

	private static boolean nextIsIntegerLiteralAsName() {
		return scanToken(29);
	}

	private static boolean matchesMethodNameChange(int lookaheadDepth) {
		lookaheadRemaining = lookaheadDepth;
		lastPos = scanPos = token;

		try {
			return !matchesMethodNameChangeExpansion();
		} catch (TokenMgrError4 ignored) {
		} finally {
			saveLookaheadState(1, lookaheadDepth);
		}

		return true;
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

	private static boolean nextIsInitAsMethodName() {
		return scanToken(23);
	}

	public static MappingParserException generateParseException() {
		expectedTokenSequences.clear();
		boolean[] expectedTokenFlags = new boolean[31];
		if (errorTokenKind >= 0) {
			expectedTokenFlags[errorTokenKind] = true;
			errorTokenKind = -1;
		}

		for (int la1Index = 0; la1Index < 15; la1Index++) {
			if (la1Generation[la1Index] == generationCounter) {
				for (int bitIndex = 0; bitIndex < 32; bitIndex++) {
					if ((la1TokenBits0[la1Index] & 1 << bitIndex) != 0) {
						expectedTokenFlags[bitIndex] = true;
					}
				}
			}
		}

		for (int tokenKind = 0; tokenKind < 31; tokenKind++) {
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

		return new MappingParserException(token, expectedSequences, MappingTokenConstants.tokenImage);
	}

	static {
		initLa1TokenBits0();
	}
}
