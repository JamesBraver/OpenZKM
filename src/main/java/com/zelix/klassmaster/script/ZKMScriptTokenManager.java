package com.zelix.klassmaster.script;

import com.zelix.exceptions.LexicalException;
import java.io.IOException;
import java.io.PrintStream;

public class ZKMScriptTokenManager implements ZKMScriptTokenList {
	public static final long[] jjbitVec0 = new long[]{0L, 0L, -1L, -1L};
	public static final long[] jjbitVec1 = new long[]{-2L, -1L, -1L, -1L};
	public static final int[] jjnextStates = new int[]{16, 17, 14, 18, 24};
	public static final String[] jjstrLiteralImages = new String[]{
		"",
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		"\ufeff",
		"=",
		"-",
		"[",
		"]",
		";",
		",",
		".",
		"/",
		"*",
		"+",
		"!",
		"^",
		"@",
		"(",
		")",
		"{",
		"}",
		"?",
		">",
		"||",
		"&&",
		"gc",
		"all",
		"and",
		"open",
		"enum",
		"true",
		"keep",
		"none",
		"asIs",
		"trim",
		"print",
		"light",
		"heavy",
		"false",
		"final",
		"ASCII",
		"bridge",
		"<link>",
		"<init>",
		"sparse",
		"search",
		"public",
		"static",
		"native",
		"delete",
		"normal",
		"throws",
		"random",
		"exclude",
		"saveAll",
		"extends",
		"package",
		"private",
		"execute",
		"moderate",
		"scramble",
		"abstract",
		"<clinit>",
		"enhanced",
		"volatile",
		"non-ASCII",
		"synthetic",
		"preverify",
		"unexclude",
		"classpath",
		"obfuscate",
		"protected",
		"interface",
		"transient",
		"randomize",
		"groupings",
		"annotation",
		"containing",
		"saveAllOld",
		"aggressive",
		"implements",
		"containedIn",
		"keepVisible",
		"trimExclude",
		"lineNumbers",
		"ifInArchive",
		"package-info",
		"fixedClasses",
		"synchronized",
		"trimUnexclude",
		"obfuscateFlow",
		"flowObfuscate",
		"changeLogFile",
		"localVariables",
		"hideFieldNames",
		"newNamesPrefix",
		"resetGroupings",
		"inSpecialClass",
		"extraAggressive",
		"changeLogFileIn",
		"resetExclusions",
		"uniqueClassNames",
		"methodParameters",
		"keepGenericsInfo",
		"changeLogFileOut",
		"legalIdentifiers",
		"allClassesOpened",
		"newClassNameFile",
		"newFieldNameFile",
		"lastModifiedTime",
		"makeClassesPublic",
		"uniqueMethodNames",
		"newMethodNameFile",
		"keepBalancedLocks",
		"+signatureClasses",
		"resetFixedClasses",
		"removeMethodCalls",
		"newNameCharacters",
		"deleteXMLComments",
		"openNestedArchives",
		"newPackageNameFile",
		"archiveCompression",
		"keepInnerClassInfo",
		"autoReflectionHash",
		"obfuscateParameters",
		"mixedCaseClassNames",
		"ifNameNotObfuscated",
		"resetTrimExclusions",
		"obfuscateReferences",
		"keepIfNotObfuscated",
		"expectedFinal_SHA256",
		"inReferencingClasses",
		"looseChangeLogFileIn",
		"obfuscateFlowExclude",
		"exceptionObfuscation",
		"accessedByReflection",
		"assumeRuntimeVersion",
		"encryptLongConstants",
		"hideStaticMethodNames",
		"encryptStringLiterals",
		"autoReflectionPackage",
		"longEncryptionExclude",
		"expectedInitial_SHA256",
		"obfuscateFlowUnexclude",
		"methodParameterChanges",
		"autoReflectionHandling",
		"deleteEmptyDirectories",
		"resetRemoveMethodCalls",
		"longEncryptionUnexclude",
		"encryptIntegerConstants",
		"ignoreMissingReferences",
		"stringEncryptionExclude",
		"deleteUnknownAttributes",
		"integerEncryptionExclude",
		"removeMethodCallsInclude",
		"removeMethodCallsExclude",
		"classInitializationOrder",
		"aggressiveMethodRenaming",
		"existingSerializedClasses",
		"stringEncryptionUnexclude",
		"resetAccessedByReflection",
		"deleteExceptionAttributes",
		"obfuscateExceptionsExclude",
		"integerEncryptionUnexclude",
		"obfuscateReferencesPackage",
		"keepVisibleIfNotObfuscated",
		"deleteAnnotationAttributes",
		"deleteSourceFileAttributes",
		"deleteDeprecatedAttributes",
		"obfuscateReferencesInclude",
		"obfuscateReferencesExclude",
		"accessedByReflectionExclude",
		"allowMethodParameterChanges",
		"keepVisibleMethodParameters",
		"collapsePackagesWithDefault",
		"obfuscateExceptionsUnexclude",
		"resetIgnoreMissingReferences",
		"obfuscateReferenceStructures",
		"resetObfuscateFlowExclusions",
		"resetLongEncryptionExclusions",
		"resetClassInitializationOrder",
		"methodParameterChangesInclude",
		"methodParameterChangesExclude",
		"methodParameterChangesPackage",
		"resetExistingSerializedClasses",
		"deleteDebugExtensionAttributes",
		"resetStringEncryptionExclusions",
		"resetIntegerEncryptionExclusions",
		"methodParameterObfuscationInclude",
		"methodParameterObfuscationExclude",
		"deriveGroupingsFromInputChangeLog",
		"deriveSubclassNamesFromSuperclass",
		"resetObfuscateReferenceExclusions",
		"resetObfuscateExceptionsExclusions",
		"keepMethodParametersIfNotObfuscated",
		"resetMethodParameterChangesExclusions",
		"resetMethodParameterObfuscationExclusions",
		"keepVisibleMethodParametersIfNotObfuscated",
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null,
		null
	};
	public static final String[] lexStateNames = new String[]{"DEFAULT", "IN_SINGLE_LINE_COMMENT", "IN_FORMAL_COMMENT", "IN_MULTI_LINE_COMMENT", "IN_STRING_LITERAL"};
	public static final int[] jjnewLexState = new int[]{
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		1,
		2,
		3,
		0,
		0,
		0,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		4,
		0,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1,
		-1
	};
	public static final long[] jjtoToken = new long[]{-16383L, -1L, -1L, 66453503L};
	public static final long[] jjtoSkip = new long[]{3646L, 0L, 0L, 0L};
	public static final long[] jjtoSpecial = new long[]{3584L, 0L, 0L, 0L};
	public static final long[] jjtoMore = new long[]{4544L, 0L, 0L, 655360L};
	public PrintStream debugStream = System.out;
	private final int[] jjrounds = new int[25];
	private final int[] jjstateSet = new int[50];
	private final StringBuilder jjimage = new StringBuilder();
	private StringBuilder image = this.jjimage;
	public int curLexState = 0;
	public int defaultLexState = 0;
	protected SimpleCharStream input_stream;
	public int jjmatchedPos;
	public int jjnewStateCnt;
	private int lengthOfMatch;
	private int jjimageLen;
	protected char curChar;
	public int jjround;
	public int jjmatchedKind;

	public ZKMScriptTokenManager(SimpleCharStream stream) {
		this.input_stream = stream;
	}

	private static final boolean jjCanMove_1(int highByte, int highIndex, int lowIndex, long highMask, long lowMask) {
		switch (highByte) {
			case 0:
				return (jjbitVec0[lowIndex] & lowMask) != 0L;
			default:
				return (jjbitVec1[highIndex] & highMask) != 0L;
		}
	}

	private static final boolean jjCanMove_0(int highByte, int highIndex, int lowIndex, long highMask, long lowMask) {
		switch (highByte) {
			case 0:
				return (jjbitVec0[lowIndex] & lowMask) != 0L;
			default:
				return false;
		}
	}

	private int jjMoveStringLiteralDfa9_0(long old1, long active1, long old2, long active2, long old3, long active3) {
		if (((active1 = active1 & old1) | (active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(7, 0L, old1, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(8, 0L, active1, active2, active3);
				return 9;
			}

			switch (this.curChar) {
				case 'C':
					return this.jjMoveStringLiteralDfa10_0(active1, 17592186044416L, active2, 290L, active3, 0L);
				case 'D':
				case 'G':
				case 'H':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'Q':
				case 'S':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'b':
				case 'f':
				case 'j':
				case 'k':
				case 'q':
				case 'w':
				case 'x':
				default:
					return this.jjStartNfa_0(8, 0L, active1, active2, active3);
				case 'E':
					return this.jjMoveStringLiteralDfa10_0(active1, 0L, active2, 4899951578951189504L, active3, 0L);
				case 'F':
					return this.jjMoveStringLiteralDfa10_0(active1, 2322855752630272L, active2, 67174400L, active3, 0L);
				case 'I':
					return this.jjMoveStringLiteralDfa10_0(active1, 268435456L, active2, 0L, active3, 0L);
				case 'N':
					return this.jjMoveStringLiteralDfa10_0(active1, 576462951326679040L, active2, 0L, active3, 0L);
				case 'O':
					return this.jjMoveStringLiteralDfa10_0(active1, 0L, active2, 135680L, active3, 0L);
				case 'P':
					return this.jjMoveStringLiteralDfa10_0(active1, 0L, active2, 128L, active3, 0L);
				case 'R':
					return this.jjMoveStringLiteralDfa10_0(active1, 0L, active2, 1166573040977315840L, active3, 0L);
				case 'a':
					return this.jjMoveStringLiteralDfa10_0(active1, 54607244993495040L, active2, 144115188210073601L, active3, 391L);
				case 'c':
					return this.jjMoveStringLiteralDfa10_0(active1, 1152921779484753920L, active2, 70437742592064L, active3, 1024L);
				case 'd':
					if ((active1 & 33554432L) != 0L) {
						return this.jjStartNfaWithStates_0(9, 89, 26);
					}

					return this.jjMoveStringLiteralDfa10_0(active1, 4611686019501129728L, active2, 4L, active3, 8192L);
				case 'e':
					if ((active1 & 67108864L) != 0L) {
						this.jjmatchedKind = 90;
						this.jjmatchedPos = 9;
					}

					return this.jjMoveStringLiteralDfa10_0(active1, 2449993381661638656L, active2, 20890720927752L, active3, 0L);
				case 'g':
					if ((active1 & 16777216L) != 0L) {
						return this.jjStartNfaWithStates_0(9, 88, 26);
					}

					return this.jjMoveStringLiteralDfa10_0(active1, 0L, active2, 32768L, active3, 64L);
				case 'h':
					return this.jjMoveStringLiteralDfa10_0(active1, -8935141660703064064L, active2, 412316860416L, active3, 0L);
				case 'i':
					return this.jjMoveStringLiteralDfa10_0(active1, 73183493944770560L, active2, 549755822080L, active3, 0L);
				case 'l':
					return this.jjMoveStringLiteralDfa10_0(active1, 69256347648L, active2, 72339069014638592L, active3, 65536L);
				case 'm':
					return this.jjMoveStringLiteralDfa10_0(active1, 0L, active2, 16L, active3, 0L);
				case 'n':
					if ((active1 & 8388608L) != 0L) {
						return this.jjStartNfaWithStates_0(9, 87, 26);
					}

					return this.jjMoveStringLiteralDfa10_0(active1, 8589934592L, active2, 34394341376L, active3, 32L);
				case 'o':
					return this.jjMoveStringLiteralDfa10_0(active1, 0L, active2, 36591746972385280L, active3, 49152L);
				case 'p':
					return this.jjMoveStringLiteralDfa10_0(active1, 8796093022208L, active2, 2164260864L, active3, 0L);
				case 'r':
					return this.jjMoveStringLiteralDfa10_0(active1, 4400193994752L, active2, 579842867254525952L, active3, 0L);
				case 's':
					if ((active1 & 134217728L) != 0L) {
						return this.jjStartNfaWithStates_0(9, 91, 26);
					}

					return this.jjMoveStringLiteralDfa10_0(active1, 9288691411320832L, active2, -6917520222958125056L, active3, 6144L);
				case 't':
					return this.jjMoveStringLiteralDfa10_0(active1, 4503599627370496L, active2, 4832362496L, active3, 8L);
				case 'u':
					return this.jjMoveStringLiteralDfa10_0(active1, 140737488355328L, active2, 0L, active3, 528L);
				case 'v':
					return this.jjMoveStringLiteralDfa10_0(active1, 4294967296L, active2, 1073741824L, active3, 0L);
				case 'y':
					return this.jjMoveStringLiteralDfa10_0(active1, 0L, active2, 18014398509744128L, active3, 0L);
				case 'z':
					return this.jjMoveStringLiteralDfa10_0(active1, 34359738368L, active2, 0L, active3, 0L);
			}
		}
	}

	private int jjMoveStringLiteralDfa5_0(long old0, long active0, long old1, long active1, long old2, long active2, long old3, long active3) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1) | (active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(3, old0, old1, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(4, active0, active1, active2, active3);
				return 5;
			}

			switch (this.curChar) {
				case '>':
					if ((active0 & 9007199254740992L) != 0L) {
						return this.jjStartNfaWithStates_0(5, 53, 9);
					}

					if ((active0 & 18014398509481984L) != 0L) {
						return this.jjStopAtPos(5, 54);
					}
				case '?':
				case '@':
				case 'B':
				case 'D':
				case 'H':
				case 'J':
				case 'K':
				case 'N':
				case 'P':
				case 'Q':
				case 'U':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'j':
				case 'k':
				case 'q':
				case 'w':
				default:
					break;
				case 'A':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 35184372088832L, active2, 8796093022208L, active3, 0L);
				case 'C':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 17179869184L, active2, -9223372036854742784L, active3, 0L);
				case 'E':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 140737488355328L, active2, 0L, active3, 8L);
				case 'F':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 4611686018427387904L, active2, 0L, active3, 0L);
				case 'G':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 8796093022208L, active2, 0L, active3, 0L);
				case 'I':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 4503599627370496L, active2, 576461302059237376L, active3, 64L);
				case 'L':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 0L, active2, 4611686018427387904L, active3, 0L);
				case 'M':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 0L, active2, 36028797018963968L, active3, 49152L);
				case 'O':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 0L, active2, 2305843009213693952L, active3, 6144L);
				case 'R':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 0L, active2, 1073741824L, active3, 0L);
				case 'S':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 4096L, active2, 0L, active3, 32L);
				case 'T':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 0L, active2, 1024L, active3, 0L);
				case 'V':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 1099511627776L, active2, 0L, active3, 0L);
				case 'a':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 3485786111593152832L, active2, 0L, active3, 0L);
				case 'b':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 274877907072L, active2, 0L, active3, 0L);
				case 'c':
					if ((active0 & 144115188075855872L) != 0L) {
						return this.jjStartNfaWithStates_0(5, 57, 26);
					}

					if ((active0 & 288230376151711744L) != 0L) {
						return this.jjStartNfaWithStates_0(5, 58, 26);
					}

					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 17729625392128L, active2, 1454838601568290952L, active3, 0L);
				case 'd':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 562949953421317L, active2, 134217728L, active3, 391L);
				case 'e':
					if ((active0 & 4503599627370496L) != 0L) {
						return this.jjStartNfaWithStates_0(5, 52, 26);
					}

					if ((active0 & 36028797018963968L) != 0L) {
						return this.jjStartNfaWithStates_0(5, 55, 26);
					}

					if ((active0 & 576460752303423488L) != 0L) {
						return this.jjStartNfaWithStates_0(5, 59, 26);
					}

					if ((active0 & 1152921504606846976L) != 0L) {
						this.jjmatchedKind = 60;
						this.jjmatchedPos = 5;
					}

					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, -8895382770486861824L, active2, 4029135404417606L, active3, 9744L);
				case 'f':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 524288L, active2, 4096L, active3, 0L);
				case 'g':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 8589934600L, active2, 4415226380288L, active3, 0L);
				case 'h':
					if ((active0 & 72057594037927936L) != 0L) {
						return this.jjStartNfaWithStates_0(5, 56, 26);
					}
					break;
				case 'i':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 2199850584576L, active2, 72341268037894144L, active3, 65536L);
				case 'l':
					if ((active0 & 2305843009213693952L) != 0L) {
						return this.jjStartNfaWithStates_0(5, 61, 26);
					}

					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 144115188109443074L, active2, 0L, active3, 0L);
				case 'm':
					if ((active0 & Long.MIN_VALUE) != 0L) {
						this.jjmatchedKind = 63;
						this.jjmatchedPos = 5;
					}

					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 4398182825984L, active2, 1L, active3, 0L);
				case 'n':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 68719476736L, active2, 2164260896L, active3, 0L);
				case 'o':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 72057594037927936L, active2, 0L, active3, 0L);
				case 'p':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 65536L, active2, 144115192376066048L, active3, 0L);
				case 'r':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 38654722048L, active2, 0L, active3, 0L);
				case 's':
					if ((active0 & 4611686018427387904L) != 0L) {
						return this.jjStartNfaWithStates_0(5, 62, 26);
					}

					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 67108864L, active2, 18015498021371904L, active3, 0L);
				case 't':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 576460752303423536L, active2, 35790848L, active3, 0L);
				case 'u':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 2147483648L, active2, 0L, active3, 0L);
				case 'v':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 0L, active2, 16L, active3, 0L);
				case 'x':
					return this.jjMoveStringLiteralDfa6_0(active0, 0L, active1, 1073741824L, active2, 0L, active3, 0L);
			}

			return this.jjStartNfa_0(4, active0, active1, active2, active3);
		}
	}

	private final int jjStartNfa_0(int pos, long active0, long active1, long active2, long active3) {
		return this.jjMoveNfa_0(this.jjStopStringLiteralDfa_0(pos, active0, active1, active2, active3), pos + 1);
	}

	private int jjMoveStringLiteralDfa14_0(long old1, long active1, long old2, long active2, long old3, long active3) {
		if (((active1 = active1 & old1) | (active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(12, 0L, old1, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(13, 0L, active1, active2, active3);
				return 14;
			}

			switch (this.curChar) {
				case 'C':
					return this.jjMoveStringLiteralDfa15_0(active1, 0L, active2, 4294967296L, active3, 0L);
				case 'D':
				case 'G':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'O':
				case 'Q':
				case 'T':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'b':
				case 'd':
				case 'g':
				case 'j':
				case 'k':
				case 'q':
				case 'v':
				case 'w':
				default:
					return this.jjStartNfa_0(13, 0L, active1, active2, active3);
				case 'E':
					return this.jjMoveStringLiteralDfa15_0(active1, 0L, active2, 16777216L, active3, 4096L);
				case 'F':
					return this.jjMoveStringLiteralDfa15_0(active1, 0L, active2, 2305843009213726728L, active3, 0L);
				case 'H':
					return this.jjMoveStringLiteralDfa15_0(active1, 0L, active2, 268435520L, active3, 0L);
				case 'I':
					return this.jjMoveStringLiteralDfa15_0(active1, 0L, active2, 32L, active3, 0L);
				case 'N':
					return this.jjMoveStringLiteralDfa15_0(active1, 0L, active2, 256L, active3, 1024L);
				case 'P':
					return this.jjMoveStringLiteralDfa15_0(active1, 0L, active2, 8388608L, active3, 0L);
				case 'R':
					return this.jjMoveStringLiteralDfa15_0(active1, 0L, active2, 0L, active3, 2048L);
				case 'S':
					return this.jjMoveStringLiteralDfa15_0(active1, 0L, active2, 8192L, active3, 0L);
				case 'U':
					return this.jjMoveStringLiteralDfa15_0(active1, 0L, active2, 2147483648L, active3, 0L);
				case 'a':
					return this.jjMoveStringLiteralDfa15_0(active1, 0L, active2, 36029346774777856L, active3, 49152L);
				case 'c':
					return this.jjMoveStringLiteralDfa15_0(active1, 1152921504606846976L, active2, 135680L, active3, 64L);
				case 'e':
					if ((active1 & 35184372088832L) != 0L) {
						return this.jjStartNfaWithStates_0(14, 109, 26);
					}

					return this.jjMoveStringLiteralDfa15_0(active1, 9288674231451648L, active2, 1330954436503931009L, active3, 24L);
				case 'f':
					return this.jjMoveStringLiteralDfa15_0(active1, 1125899906842624L, active2, 0L, active3, 0L);
				case 'h':
					return this.jjMoveStringLiteralDfa15_0(active1, 0L, active2, 72057595111669760L, active3, 65536L);
				case 'i':
					return this.jjMoveStringLiteralDfa15_0(active1, 576460752303423488L, active2, -9223299400363671548L, active3, 0L);
				case 'l':
					return this.jjMoveStringLiteralDfa15_0(active1, -9025213653250473984L, active2, 1126312257273856L, active3, 0L);
				case 'm':
					return this.jjMoveStringLiteralDfa15_0(active1, 360287970189639680L, active2, 0L, active3, 8192L);
				case 'n':
					if ((active1 & 70368744177664L) != 0L) {
						return this.jjStartNfaWithStates_0(14, 110, 26);
					}

					return this.jjMoveStringLiteralDfa15_0(active1, 0L, active2, 17592253153282L, active3, 0L);
				case 'o':
					return this.jjMoveStringLiteralDfa15_0(active1, 0L, active2, 849939670237184L, active3, 0L);
				case 'p':
					return this.jjMoveStringLiteralDfa15_0(active1, 0L, active2, 4611686018427387904L, active3, 0L);
				case 'r':
					return this.jjMoveStringLiteralDfa15_0(active1, 5066549580791808L, active2, 134217728L, active3, 423L);
				case 's':
					if ((active1 & 140737488355328L) != 0L) {
						return this.jjStartNfaWithStates_0(14, 111, 26);
					}

					return this.jjMoveStringLiteralDfa15_0(active1, 6917529027641081856L, active2, 576460752304473104L, active3, 512L);
				case 't':
					return this.jjMoveStringLiteralDfa15_0(active1, 0L, active2, 288265594883538944L, active3, 0L);
				case 'u':
					return this.jjMoveStringLiteralDfa15_0(active1, 2251799813685248L, active2, 0L, active3, 0L);
				case 'x':
					return this.jjMoveStringLiteralDfa15_0(active1, 0L, active2, 65536L, active3, 0L);
				case 'y':
					return this.jjMoveStringLiteralDfa15_0(active1, 0L, active2, 8796093022208L, active3, 0L);
			}
		}
	}

	private int jjStopAtPos(int pos, int kind) {
		this.jjmatchedKind = kind;
		this.jjmatchedPos = pos;
		return pos + 1;
	}

	private int jjMoveStringLiteralDfa0_2() {
		switch (this.curChar) {
			case '*':
				return this.jjMoveStringLiteralDfa1_2(1024L);
			default:
				return 1;
		}
	}

	private int jjMoveStringLiteralDfa0_3() {
		switch (this.curChar) {
			case '*':
				return this.jjMoveStringLiteralDfa1_3(2048L);
			default:
				return 1;
		}
	}

	private int jjMoveStringLiteralDfa0_1() {
		return this.jjMoveNfa_1(0, 0);
	}

	private int jjMoveStringLiteralDfa10_0(long old1, long active1, long old2, long active2, long old3, long active3) {
		if (((active1 = active1 & old1) | (active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(8, 0L, old1, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(9, 0L, active1, active2, active3);
				return 10;
			}

			switch (this.curChar) {
				case 'A':
					return this.jjMoveStringLiteralDfa11_0(active1, 0L, active2, 4L, active3, 0L);
				case 'B':
				case 'D':
				case 'E':
				case 'F':
				case 'G':
				case 'H':
				case 'J':
				case 'K':
				case 'L':
				case 'Q':
				case 'S':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'h':
				case 'j':
				case 'k':
				case 'q':
				case 'v':
				case 'w':
				default:
					return this.jjStartNfa_0(9, 0L, active1, active2, active3);
				case 'C':
					return this.jjMoveStringLiteralDfa11_0(active1, 6917529027641081856L, active2, 0L, active3, 0L);
				case 'I':
					return this.jjMoveStringLiteralDfa11_0(active1, 0L, active2, Long.MIN_VALUE, active3, 0L);
				case 'M':
					return this.jjMoveStringLiteralDfa11_0(active1, 0L, active2, 1099513724928L, active3, 0L);
				case 'N':
					return this.jjMoveStringLiteralDfa11_0(active1, 0L, active2, 8L, active3, 0L);
				case 'O':
					return this.jjMoveStringLiteralDfa11_0(active1, 9007199254740992L, active2, 0L, active3, 0L);
				case 'P':
					return this.jjMoveStringLiteralDfa11_0(active1, 0L, active2, 0L, active3, 8192L);
				case 'R':
					return this.jjMoveStringLiteralDfa11_0(active1, 0L, active2, 18014398509744128L, active3, 0L);
				case 'a':
					return this.jjMoveStringLiteralDfa11_0(active1, 576463226204585984L, active2, 549755814016L, active3, 0L);
				case 'b':
					return this.jjMoveStringLiteralDfa11_0(active1, 1099511627776L, active2, 135680L, active3, 0L);
				case 'c':
					return this.jjMoveStringLiteralDfa11_0(active1, 1125899906842624L, active2, 2451084097196392448L, active3, 6144L);
				case 'd':
					return this.jjMoveStringLiteralDfa11_0(active1, 0L, active2, 36028797018963968L, active3, 49152L);
				case 'e':
					if ((active1 & 536870912L) != 0L) {
						this.jjmatchedKind = 93;
						this.jjmatchedPos = 10;
					} else {
						if ((active1 & 1073741824L) != 0L) {
							return this.jjStartNfaWithStates_0(10, 94, 26);
						}

						if ((active1 & 4294967296L) != 0L) {
							return this.jjStartNfaWithStates_0(10, 96, 26);
						}
					}

					return this.jjMoveStringLiteralDfa11_0(active1, 1224983548230893568L, active2, 1817624667477805056L, active3, 65600L);
				case 'f':
					return this.jjMoveStringLiteralDfa11_0(active1, 8589934592L, active2, 0L, active3, 0L);
				case 'g':
					return this.jjMoveStringLiteralDfa11_0(active1, 0L, active2, 1048576L, active3, 48L);
				case 'i':
					return this.jjMoveStringLiteralDfa11_0(active1, 6835114034069504L, active2, 8628224000L, active3, 8L);
				case 'l':
					return this.jjMoveStringLiteralDfa11_0(active1, 17729624997888L, active2, 67174688L, active3, 1024L);
				case 'm':
					return this.jjMoveStringLiteralDfa11_0(active1, 54606145481867264L, active2, 134217728L, active3, 391L);
				case 'n':
					if ((active1 & 268435456L) != 0L) {
						return this.jjStartNfaWithStates_0(10, 92, 26);
					}

					return this.jjMoveStringLiteralDfa11_0(active1, 0L, active2, 4611686018427396096L, active3, 0L);
				case 'o':
					return this.jjMoveStringLiteralDfa11_0(active1, -8935141660703064064L, active2, 446676598786L, active3, 0L);
				case 'p':
					return this.jjMoveStringLiteralDfa11_0(active1, 0L, active2, 17592186044432L, active3, 512L);
				case 'r':
					return this.jjMoveStringLiteralDfa11_0(active1, 0L, active2, 72636486909953L, active3, 0L);
				case 's':
					if ((active1 & 2147483648L) != 0L) {
						return this.jjStartNfaWithStates_0(10, 95, 26);
					}

					return this.jjMoveStringLiteralDfa11_0(active1, 144572584913010688L, active2, 8796093022208L, active3, 0L);
				case 't':
					return this.jjMoveStringLiteralDfa11_0(active1, 0L, active2, 562952394506304L, active3, 0L);
				case 'u':
					return this.jjMoveStringLiteralDfa11_0(active1, 68719476736L, active2, 0L, active3, 0L);
				case 'x':
					return this.jjMoveStringLiteralDfa11_0(active1, 0L, active2, 288265560523801600L, active3, 0L);
				case 'y':
					return this.jjMoveStringLiteralDfa11_0(active1, 0L, active2, 4415763251200L, active3, 0L);
			}
		}
	}

	private int jjMoveStringLiteralDfa16_0(long old1, long active1, long old2, long active2, long old3, long active3) {
		if (((active1 = active1 & old1) | (active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(14, 0L, old1, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(15, 0L, active1, active2, active3);
				return 16;
			}

			switch (this.curChar) {
				case 'A':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 3940649673957376L, active3, 0L);
				case 'B':
				case 'C':
				case 'D':
				case 'F':
				case 'G':
				case 'H':
				case 'I':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'P':
				case 'Q':
				case 'T':
				case 'V':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'g':
				case 'j':
				case 'k':
				case 'q':
				case 'u':
				case 'v':
				case 'w':
				default:
					return this.jjStartNfa_0(15, 0L, active1, active2, active3);
				case 'E':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 17179869184L, active3, 0L);
				case 'N':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 2097152L, active3, 0L);
				case 'O':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 281474976710656L, active3, 0L);
				case 'R':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 1099511627776L, active3, 0L);
				case 'S':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 33554432L, active3, 0L);
				case 'U':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 4398046511104L, active3, 0L);
				case 'W':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 144115188075855872L, active3, 0L);
				case 'a':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 1048576L, active3, 0L);
				case 'b':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 0L, active3, 384L);
				case 'c':
					if ((active1 & 144115188075855872L) != 0L) {
						return this.jjStartNfaWithStates_0(16, 121, 26);
					}

					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 1166573041002481664L, active3, 4096L);
				case 'd':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 72057595111669760L, active3, 65536L);
				case 'e':
					if ((active1 & 576460752303423488L) != 0L) {
						return this.jjStartNfaWithStates_0(16, 123, 26);
					}

					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 36039802876854404L, active3, 49152L);
				case 'f':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 32L, active3, 2048L);
				case 'h':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 134217728L, active3, 7L);
				case 'i':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 4611686568183201792L, active3, 8L);
				case 'l':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, -9223372036854677496L, active3, 0L);
				case 'm':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 256L, active3, 1024L);
				case 'n':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 576531194330480640L, active3, 0L);
				case 'o':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 2594108569737495568L, active3, 0L);
				case 'p':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 0L, active3, 32L);
				case 'r':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 34359738368L, active3, 512L);
				case 's':
					if ((active1 & 288230376151711744L) != 0L) {
						return this.jjStartNfaWithStates_0(16, 122, 26);
					} else if ((active1 & 1152921504606846976L) != 0L) {
						return this.jjStartNfaWithStates_0(16, 124, 26);
					} else if ((active1 & 2305843009213693952L) != 0L) {
						return this.jjStartNfaWithStates_0(16, 125, 26);
					} else if ((active1 & 4611686018427387904L) != 0L) {
						return this.jjStartNfaWithStates_0(16, 126, 26);
					} else {
						if ((active1 & Long.MIN_VALUE) != 0L) {
							this.jjmatchedKind = 127;
							this.jjmatchedPos = 16;
						} else {
							if ((active2 & 1L) != 0L) {
								return this.jjStartNfaWithStates_0(16, 128, 26);
							}

							if ((active2 & 2L) != 0L) {
								return this.jjStartNfaWithStates_0(16, 129, 26);
							}
						}

						return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 412317401152L, active3, 16L);
					}
				case 't':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 18031991232795136L, active3, 8192L);
				case 'x':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 67108864L, active3, 0L);
				case 'y':
					return this.jjMoveStringLiteralDfa17_0(active1, 0L, active2, 0L, active3, 64L);
			}
		}
	}

	private void jjCheckNAddTwoStates(int state1, int state2) {
		this.jjCheckNAdd(state1);
		this.jjCheckNAdd(state2);
	}

	private int jjMoveStringLiteralDfa41_0(long old3, long active3) {
		if ((active3 = active3 & old3) == 0L) {
			return this.jjStartNfa_0(39, 0L, 0L, 0L, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(40, 0L, 0L, 0L, active3);
				return 41;
			}

			switch (this.curChar) {
				case 'd':
					if ((active3 & 65536L) != 0L) {
						return this.jjStartNfaWithStates_0(41, 208, 26);
					}
				default:
					return this.jjStartNfa_0(40, 0L, 0L, 0L, active3);
			}
		}
	}

	private int jjMoveStringLiteralDfa27_0(long old2, long active2, long old3, long active3) {
		if (((active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(25, 0L, 0L, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(26, 0L, 0L, active2, active3);
				return 27;
			}

			switch (this.curChar) {
				case 'E':
					return this.jjMoveStringLiteralDfa28_0(active2, 0L, active3, 16384L);
				case 'F':
				case 'G':
				case 'H':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'N':
				case 'O':
				case 'P':
				case 'Q':
				case 'R':
				case 'S':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'a':
				case 'b':
				case 'c':
				case 'h':
				case 'j':
				case 'k':
				case 'm':
				case 'o':
				case 'p':
				case 'q':
				case 'v':
				case 'w':
				default:
					return this.jjStartNfa_0(26, 0L, 0L, active2, active3);
				case 'I':
					return this.jjMoveStringLiteralDfa28_0(active2, 0L, active3, 65536L);
				case 'd':
					return this.jjMoveStringLiteralDfa28_0(active2, 0L, active3, 3L);
				case 'e':
					if ((active2 & 288230376151711744L) != 0L) {
						return this.jjStartNfaWithStates_0(27, 186, 26);
					}

					return this.jjMoveStringLiteralDfa28_0(active2, Long.MIN_VALUE, active3, 0L);
				case 'f':
					return this.jjMoveStringLiteralDfa28_0(active2, 0L, active3, 8192L);
				case 'g':
					return this.jjMoveStringLiteralDfa28_0(active2, 0L, active3, 4L);
				case 'i':
					return this.jjMoveStringLiteralDfa28_0(active2, 0L, active3, 32L);
				case 'l':
					return this.jjMoveStringLiteralDfa28_0(active2, 0L, active3, 4096L);
				case 'n':
					return this.jjMoveStringLiteralDfa28_0(active2, 4611686018427387904L, active3, 640L);
				case 'r':
					return this.jjMoveStringLiteralDfa28_0(active2, 0L, active3, 1024L);
				case 's':
					if ((active2 & 576460752303423488L) != 0L) {
						return this.jjStartNfaWithStates_0(27, 187, 26);
					} else if ((active2 & 1152921504606846976L) != 0L) {
						return this.jjStartNfaWithStates_0(27, 188, 26);
					} else {
						if ((active2 & 2305843009213693952L) != 0L) {
							return this.jjStartNfaWithStates_0(27, 189, 26);
						}

						return this.jjMoveStringLiteralDfa28_0(active2, 0L, active3, 72L);
					}
				case 't':
					return this.jjMoveStringLiteralDfa28_0(active2, 0L, active3, 32784L);
				case 'u':
					return this.jjMoveStringLiteralDfa28_0(active2, 0L, active3, 2048L);
				case 'x':
					return this.jjMoveStringLiteralDfa28_0(active2, 0L, active3, 256L);
			}
		}
	}

	private int jjMoveStringLiteralDfa40_0(long old3, long active3) {
		if ((active3 = active3 & old3) == 0L) {
			return this.jjStartNfa_0(38, 0L, 0L, 0L, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(39, 0L, 0L, 0L, active3);
				return 40;
			}

			switch (this.curChar) {
				case 'e':
					return this.jjMoveStringLiteralDfa41_0(active3, 65536L);
				case 's':
					if ((active3 & 32768L) != 0L) {
						return this.jjStartNfaWithStates_0(40, 207, 26);
					}
				default:
					return this.jjStartNfa_0(39, 0L, 0L, 0L, active3);
			}
		}
	}

	private int jjMoveStringLiteralDfa26_0(long old2, long active2, long old3, long active3) {
		if (((active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(24, 0L, 0L, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(25, 0L, 0L, active2, active3);
				return 26;
			}

			switch (this.curChar) {
				case 'E':
					return this.jjMoveStringLiteralDfa27_0(active2, 0L, active3, 256L);
				case 'I':
					return this.jjMoveStringLiteralDfa27_0(active2, 0L, active3, 128L);
				case 'a':
					return this.jjMoveStringLiteralDfa27_0(active2, 0L, active3, 33284L);
				case 'b':
					return this.jjMoveStringLiteralDfa27_0(active2, 0L, active3, 8192L);
				case 'c':
					return this.jjMoveStringLiteralDfa27_0(active2, 0L, active3, 4096L);
				case 'd':
					return this.jjMoveStringLiteralDfa27_0(active2, -8935141660703064064L, active3, 0L);
				case 'e':
					if ((active2 & 18014398509481984L) != 0L) {
						return this.jjStartNfaWithStates_0(26, 182, 26);
					}

					return this.jjMoveStringLiteralDfa27_0(active2, 1729382256910270464L, active3, 1024L);
				case 'l':
					return this.jjMoveStringLiteralDfa27_0(active2, 0L, active3, 2048L);
				case 'n':
					return this.jjMoveStringLiteralDfa27_0(active2, 2305843009213693952L, active3, 0L);
				case 'o':
					return this.jjMoveStringLiteralDfa27_0(active2, 4611686018427387904L, active3, 0L);
				case 's':
					if ((active2 & 36028797018963968L) != 0L) {
						return this.jjStartNfaWithStates_0(26, 183, 26);
					}

					if ((active2 & 72057594037927936L) != 0L) {
						this.jjmatchedKind = 184;
						this.jjmatchedPos = 26;
					}

					return this.jjMoveStringLiteralDfa27_0(active2, 0L, active3, 81960L);
				case 't':
					if ((active2 & 144115188075855872L) != 0L) {
						return this.jjStartNfaWithStates_0(26, 185, 26);
					}
				case 'F':
				case 'G':
				case 'H':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'N':
				case 'O':
				case 'P':
				case 'Q':
				case 'R':
				case 'S':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'f':
				case 'g':
				case 'h':
				case 'i':
				case 'j':
				case 'k':
				case 'm':
				case 'p':
				case 'q':
				case 'r':
				default:
					return this.jjStartNfa_0(25, 0L, 0L, active2, active3);
				case 'u':
					return this.jjMoveStringLiteralDfa27_0(active2, 0L, active3, 83L);
			}
		}
	}

	private int jjMoveStringLiteralDfa23_0(long old2, long active2, long old3, long active3) {
		if (((active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(21, 0L, 0L, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(22, 0L, 0L, active2, active3);
				return 23;
			}

			switch (this.curChar) {
				case 'C':
					return this.jjMoveStringLiteralDfa24_0(active2, 0L, active3, 8L);
				case 'E':
					return this.jjMoveStringLiteralDfa24_0(active2, 0L, active3, 2048L);
				case 'S':
					return this.jjMoveStringLiteralDfa24_0(active2, 0L, active3, 1024L);
				case 'a':
					return this.jjMoveStringLiteralDfa24_0(active2, 144255925564211200L, active3, 4L);
				case 'c':
					return this.jjMoveStringLiteralDfa24_0(active2, 288230376151711744L, active3, 32L);
				case 'd':
					return this.jjMoveStringLiteralDfa24_0(active2, 4398046511104L, active3, 0L);
				case 'e':
					if ((active2 & 68719476736L) != 0L) {
						return this.jjStartNfaWithStates_0(23, 164, 26);
					} else if ((active2 & 137438953472L) != 0L) {
						return this.jjStartNfaWithStates_0(23, 165, 26);
					} else {
						if ((active2 & 274877906944L) != 0L) {
							return this.jjStartNfaWithStates_0(23, 166, 26);
						}

						return this.jjMoveStringLiteralDfa24_0(active2, 576480543512723456L, active3, 0L);
					}
				case 'g':
					if ((active2 & 1099511627776L) != 0L) {
						return this.jjStartNfaWithStates_0(23, 168, 26);
					}
				case 'D':
				case 'F':
				case 'G':
				case 'H':
				case 'I':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'N':
				case 'O':
				case 'P':
				case 'Q':
				case 'R':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'b':
				case 'f':
				case 'h':
				case 'j':
				case 'k':
				case 'm':
				case 'p':
				case 'q':
				case 'v':
				case 'w':
				default:
					return this.jjStartNfa_0(22, 0L, 0L, active2, active3);
				case 'i':
					return this.jjMoveStringLiteralDfa24_0(active2, 0L, active3, 384L);
				case 'l':
					return this.jjMoveStringLiteralDfa24_0(active2, 18014398509481984L, active3, 0L);
				case 'n':
					return this.jjMoveStringLiteralDfa24_0(active2, -9187343239835811840L, active3, 16385L);
				case 'o':
					return this.jjMoveStringLiteralDfa24_0(active2, 8796093022208L, active3, 8192L);
				case 'r':
					if ((active2 & 549755813888L) != 0L) {
						return this.jjStartNfaWithStates_0(23, 167, 26);
					}

					return this.jjMoveStringLiteralDfa24_0(active2, 0L, active3, 16L);
				case 's':
					return this.jjMoveStringLiteralDfa24_0(active2, 2305843009213693952L, active3, 4096L);
				case 't':
					return this.jjMoveStringLiteralDfa24_0(active2, 1229201223295434752L, active3, 66048L);
				case 'u':
					return this.jjMoveStringLiteralDfa24_0(active2, 4625302370425765888L, active3, 32768L);
				case 'x':
					return this.jjMoveStringLiteralDfa24_0(active2, 0L, active3, 66L);
			}
		}
	}

	private int jjMoveStringLiteralDfa8_0(long old1, long active1, long old2, long active2, long old3, long active3) {
		if (((active1 = active1 & old1) | (active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(6, 0L, old1, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(7, 0L, active1, active2, active3);
				return 8;
			}

			switch (this.curChar) {
				case 'B':
					return this.jjMoveStringLiteralDfa9_0(active1, 0L, active2, 18014398509744128L, active3, 0L);
				case 'C':
				case 'D':
				case 'E':
				case 'G':
				case 'H':
				case 'J':
				case 'K':
				case 'M':
				case 'O':
				case 'Q':
				case 'R':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'j':
				case 'q':
				case 'w':
				case 'x':
				default:
					return this.jjStartNfa_0(7, 0L, active1, active2, active3);
				case 'F':
					return this.jjMoveStringLiteralDfa9_0(active1, 0L, active2, 8192L, active3, 0L);
				case 'I':
					if ((active1 & 4096L) != 0L) {
						return this.jjStartNfaWithStates_0(8, 76, 7);
					}

					return this.jjMoveStringLiteralDfa9_0(active1, 0L, active2, 33554432L, active3, 0L);
				case 'L':
					return this.jjMoveStringLiteralDfa9_0(active1, 0L, active2, 2L, active3, 0L);
				case 'N':
					return this.jjMoveStringLiteralDfa9_0(active1, 54043195528445952L, active2, 0L, active3, 0L);
				case 'P':
					return this.jjMoveStringLiteralDfa9_0(active1, 4398046511104L, active2, 144115188075855872L, active3, 0L);
				case 'S':
					return this.jjMoveStringLiteralDfa9_0(active1, 0L, active2, 2199023255552L, active3, 0L);
				case 'a':
					return this.jjMoveStringLiteralDfa9_0(active1, 281474976710656L, active2, 0L, active3, 0L);
				case 'b':
					return this.jjMoveStringLiteralDfa9_0(active1, 536870912L, active2, 72339069014638592L, active3, 66576L);
				case 'c':
					if ((active1 & 8192L) != 0L) {
						return this.jjStartNfaWithStates_0(8, 77, 26);
					}

					return this.jjMoveStringLiteralDfa9_0(active1, 68719476736L, active2, 22007412424704L, active3, 0L);
				case 'd':
					if ((active1 & 262144L) != 0L) {
						return this.jjStartNfaWithStates_0(8, 82, 26);
					}

					return this.jjMoveStringLiteralDfa9_0(active1, 576462951595114496L, active2, 0L, active3, 0L);
				case 'e':
					if ((active1 & 32768L) != 0L) {
						return this.jjStartNfaWithStates_0(8, 79, 26);
					} else {
						if ((active1 & 131072L) != 0L) {
							this.jjmatchedKind = 81;
							this.jjmatchedPos = 8;
						} else {
							if ((active1 & 524288L) != 0L) {
								return this.jjStartNfaWithStates_0(8, 83, 26);
							}

							if ((active1 & 2097152L) != 0L) {
								return this.jjStartNfaWithStates_0(8, 85, 26);
							}
						}

						return this.jjMoveStringLiteralDfa9_0(active1, 4620693357268566016L, active2, 1454847397938137540L, active3, 64L);
					}
				case 'f':
					return this.jjMoveStringLiteralDfa9_0(active1, 72057594037927936L, active2, 0L, active3, 0L);
				case 'g':
					return this.jjMoveStringLiteralDfa9_0(active1, 2322718313676800L, active2, 4611686018427387912L, active3, 0L);
				case 'h':
					if ((active1 & 65536L) != 0L) {
						return this.jjStartNfaWithStates_0(8, 80, 26);
					}

					return this.jjMoveStringLiteralDfa9_0(active1, 0L, active2, 36028797018963969L, active3, 49152L);
				case 'i':
					return this.jjMoveStringLiteralDfa9_0(active1, 1146756268032L, active2, 2097152L, active3, 32L);
				case 'k':
					return this.jjMoveStringLiteralDfa9_0(active1, 0L, active2, 34359738368L, active3, 0L);
				case 'l':
					return this.jjMoveStringLiteralDfa9_0(active1, 158329707954176L, active2, 0L, active3, 0L);
				case 'm':
					return this.jjMoveStringLiteralDfa9_0(active1, 0L, active2, 1024L, active3, 0L);
				case 'n':
					return this.jjMoveStringLiteralDfa9_0(active1, 1157425104250994688L, active2, 633391712747520L, active3, 0L);
				case 'o':
					return this.jjMoveStringLiteralDfa9_0(active1, 8388608L, active2, 576460753378213904L, active3, 8704L);
				case 'p':
					return this.jjMoveStringLiteralDfa9_0(active1, 0L, active2, 2251800350556160L, active3, 0L);
				case 'r':
					return this.jjMoveStringLiteralDfa9_0(active1, 2307567043446046720L, active2, 134217760L, active3, 391L);
				case 's':
					if ((active1 & 4194304L) != 0L) {
						return this.jjStartNfaWithStates_0(8, 86, 26);
					}

					return this.jjMoveStringLiteralDfa9_0(active1, 144115480133632000L, active2, -9223372028264841216L, active3, 8L);
				case 't':
					if ((active1 & 1048576L) != 0L) {
						return this.jjStartNfaWithStates_0(8, 84, 26);
					}

					return this.jjMoveStringLiteralDfa9_0(active1, -8935141660568846336L, active2, 962076873216L, active3, 0L);
				case 'u':
					return this.jjMoveStringLiteralDfa9_0(active1, 8797166764032L, active2, 2306968909120536576L, active3, 6144L);
				case 'v':
					return this.jjMoveStringLiteralDfa9_0(active1, 67108864L, active2, 1099511627776L, active3, 0L);
				case 'y':
					return (active1 & 16384L) != 0L ? this.jjStartNfaWithStates_0(8, 78, 26) : this.jjMoveStringLiteralDfa9_0(active1, 0L, active2, 2164260864L, active3, 0L);
			}
		}
	}

	private final int jjStopStringLiteralDfa_0(int pos, long active0, long active1, long active2, long active3) {
		switch (pos) {
			case 0:
				if ((active0 & 4194624L) != 0L) {
					return 2;
				} else if ((active0 & 8388608L) != 0L) {
					return 25;
				} else {
					if ((active0 & 27021597764222976L) == 0L && (active1 & 512L) == 0L) {
						if ((active0 & -27021666483699712L) == 0L && (active1 & -2305843009213694465L) == 0L && (active2 & -1L) == 0L && (active3 & 131071L) == 0L) {
							if ((active0 & 65536L) != 0L) {
								return 7;
							}

							if ((active0 & 16793600L) == 0L && (active1 & 2305843009213693952L) == 0L) {
								return -1;
							}

							return 26;
						}

						this.jjmatchedKind = 213;
						return 26;
					}

					return 13;
				}
			case 1:
				if ((active0 & 256L) != 0L) {
					return 0;
				} else if ((active0 & 9007199254740992L) != 0L) {
					return 12;
				} else {
					if ((active0 & -27021735203176448L) == 0L && (active1 & -513L) == 0L && (active2 & -1L) == 0L && (active3 & 131071L) == 0L) {
						if ((active0 & 68719476736L) != 0L) {
							return 26;
						}

						return -1;
					}

					this.jjmatchedKind = 213;
					this.jjmatchedPos = 1;
					return 26;
				}
			case 2:
				if ((active0 & 9007199254740992L) != 0L) {
					return 11;
				} else {
					if ((active0 & -27022147520036864L) == 0L && (active1 & -9007199254741505L) == 0L && (active2 & -36028797018963969L) == 0L && (active3 & 131071L) == 0L) {
						if ((active0 & 412316860416L) == 0L && (active1 & 9007199254740992L) == 0L && (active2 & 36028797018963968L) == 0L) {
							return -1;
						}

						return 26;
					}

					if (this.jjmatchedPos != 2) {
						this.jjmatchedKind = 213;
						this.jjmatchedPos = 2;
					}

					return 26;
				}
			case 3:
				if ((active0 & 9007199254740992L) != 0L) {
					return 10;
				} else if ((active1 & 4096L) != 0L) {
					if (this.jjmatchedPos != 3) {
						this.jjmatchedKind = 215;
						this.jjmatchedPos = 3;
					}

					return 7;
				} else {
					if ((active0 & -27091966508400640L) == 0L && (active1 & -1154047474843783681L) == 0L && (active2 & -72339069014642725L) == 0L && (active3 & 57343L) == 0L) {
						if ((active0 & 69818988363776L) == 0L && (active1 & 1154047474843779072L) == 0L && (active2 & 72339069014642724L) == 0L && (active3 & 73728L) == 0L) {
							return -1;
						}

						return 26;
					}

					if (this.jjmatchedPos != 3) {
						this.jjmatchedKind = 213;
						this.jjmatchedPos = 3;
					}

					return 26;
				}
			case 4:
				if ((active0 & -31525197391593472L) == 0L && (active1 & -4609L) == 0L && (active2 & -1L) == 0L && (active3 & 131071L) == 0L) {
					if ((active0 & 9007199254740992L) != 0L) {
						return 8;
					}

					if ((active1 & 4096L) != 0L) {
						this.jjmatchedKind = 215;
						this.jjmatchedPos = 4;
						return 7;
					}

					if ((active0 & 4433230883192832L) != 0L) {
						return 26;
					}

					return -1;
				}

				this.jjmatchedKind = 213;
				this.jjmatchedPos = 4;
				return 26;
			case 5:
				if ((active1 & -2101761L) == 0L && (active2 & -3958276756602883L) == 0L && (active3 & 131055L) == 0L) {
					if ((active1 & 4096L) != 0L) {
						if (this.jjmatchedPos != 5) {
							this.jjmatchedKind = 215;
							this.jjmatchedPos = 5;
						}

						return 7;
					}

					if ((active0 & 9007199254740992L) != 0L) {
						return 9;
					}

					if ((active0 & -31525197391593472L) == 0L && (active1 & 2097152L) == 0L && (active2 & 3958276756602882L) == 0L && (active3 & 16L) == 0L) {
						return -1;
					}

					return 26;
				}

				if (this.jjmatchedPos != 5) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 5;
				}

				return 26;
			case 6:
				if ((active1 & -8623493696L) == 0L && (active2 & -1L) == 0L && (active3 & 131071L) == 0L) {
					if ((active1 & 4096L) != 0L) {
						if (this.jjmatchedPos != 6) {
							this.jjmatchedKind = 215;
							this.jjmatchedPos = 6;
						}

						return 7;
					}

					if ((active1 & 8623489087L) != 0L) {
						return 26;
					}

					return -1;
				}

				if (this.jjmatchedPos != 6) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 6;
				}

				return 26;
			case 7:
				if ((active1 & -8589942784L) == 0L && (active2 & -1L) == 0L && (active3 & 131071L) == 0L) {
					if ((active1 & 8589938688L) != 0L) {
						this.jjmatchedKind = 215;
						this.jjmatchedPos = 7;
						return 7;
					}

					if ((active1 & 3520L) != 0L) {
						return 26;
					}

					return -1;
				}

				this.jjmatchedKind = 213;
				this.jjmatchedPos = 7;
				return 26;
			case 8:
				if ((active1 & -146037276672L) == 0L && (active2 & -1454838601568290945L) == 0L && (active3 & 131071L) == 0L) {
					if ((active1 & 8589934592L) != 0L) {
						if (this.jjmatchedPos != 8) {
							this.jjmatchedKind = 215;
							this.jjmatchedPos = 8;
						}

						return 7;
					}

					if ((active1 & 4096L) != 0L) {
						return 7;
					}

					if ((active1 & 137447333888L) == 0L && (active2 & 1454838601568290944L) == 0L) {
						return -1;
					}

					return 26;
				}

				if (this.jjmatchedPos != 8) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 8;
				}

				return 26;
			case 9:
				if ((active1 & -8858370048L) == 0L && (active2 & -1099511627777L) == 0L && (active3 & 131071L) == 0L) {
					if ((active1 & 8589934592L) != 0L) {
						if (this.jjmatchedPos != 9) {
							this.jjmatchedKind = 215;
							this.jjmatchedPos = 9;
						}

						return 7;
					}

					if ((active1 & 260046848L) == 0L && (active2 & 1099511627776L) == 0L) {
						return -1;
					}

					return 26;
				}

				if (this.jjmatchedPos != 9) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 9;
				}

				return 26;
			case 10:
				if ((active1 & -17179869184L) == 0L && (active2 & -72339069014638593L) == 0L && (active3 & 65535L) == 0L) {
					if ((active1 & 8589934592L) != 0L) {
						if (this.jjmatchedPos != 10) {
							this.jjmatchedKind = 215;
							this.jjmatchedPos = 10;
						}

						return 7;
					}

					if ((active1 & 8321499136L) == 0L && (active2 & 72339069014638592L) == 0L && (active3 & 65536L) == 0L) {
						return -1;
					}

					return 26;
				}

				if (this.jjmatchedPos != 10) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 10;
				}

				return 26;
			case 11:
				if ((active1 & -68719476736L) == 0L && (active2 & -1L) == 0L && (active3 & 131071L) == 0L) {
					if ((active1 & 8589934592L) != 0L) {
						return 7;
					}

					if ((active1 & 51539607552L) != 0L) {
						return 26;
					}

					return -1;
				}

				this.jjmatchedKind = 213;
				this.jjmatchedPos = 11;
				return 26;
			case 12:
				if ((active1 & -2323268069490688L) == 0L && (active2 & -67174401L) == 0L && (active3 & 131071L) == 0L) {
					if ((active1 & 2323199350013952L) == 0L && (active2 & 67174400L) == 0L) {
						return -1;
					}

					return 26;
				}

				if (this.jjmatchedPos != 12) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 12;
				}

				return 26;
			case 13:
				if ((active1 & -35184372088832L) == 0L && (active2 & -1L) == 0L && (active3 & 131071L) == 0L) {
					if ((active1 & 34084860461056L) != 0L) {
						return 26;
					}

					return -1;
				}

				this.jjmatchedKind = 213;
				this.jjmatchedPos = 13;
				return 26;
			case 14:
				if ((active1 & -281474976710656L) == 0L && (active2 & -1L) == 0L && (active3 & 131071L) == 0L) {
					if ((active1 & 246290604621824L) != 0L) {
						return 26;
					}

					return -1;
				}

				this.jjmatchedKind = 213;
				this.jjmatchedPos = 14;
				return 26;
			case 15:
				if ((active1 & -144115188075855872L) == 0L && (active2 & -1L) == 0L && (active3 & 131071L) == 0L) {
					if ((active1 & 143833713099145216L) != 0L) {
						return 26;
					}

					return -1;
				}

				this.jjmatchedKind = 213;
				this.jjmatchedPos = 15;
				return 26;
			case 16:
				if ((active2 & -412316860420L) == 0L && (active3 & 131071L) == 0L) {
					if ((active1 & -144115188075855872L) == 0L && (active2 & 412316860419L) == 0L) {
						return -1;
					}

					return 26;
				}

				if (this.jjmatchedPos != 16) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 16;
				}

				return 26;
			case 17:
				if ((active2 & -128L) == 0L && (active3 & 131071L) == 0L) {
					if ((active2 & 124L) != 0L) {
						return 26;
					}

					return -1;
				}

				this.jjmatchedKind = 213;
				this.jjmatchedPos = 17;
				return 26;
			case 18:
				if ((active2 & -13651536370475008L) == 0L && (active3 & 131071L) == 0L) {
					if ((active2 & 13651536370474880L) != 0L) {
						return 26;
					}

					return -1;
				}

				if (this.jjmatchedPos != 18) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 18;
				}

				return 26;
			case 19:
				if ((active2 & -18014398511579136L) == 0L && (active3 & 131071L) == 0L) {
					if ((active2 & 18014398511570944L) != 0L) {
						return 26;
					}

					return -1;
				}

				if (this.jjmatchedPos != 19) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 19;
				}

				return 26;
			case 20:
				if ((active2 & -33554432L) == 0L && (active3 & 131071L) == 0L) {
					if ((active2 & 31457280L) != 0L) {
						return 26;
					}

					return -1;
				}

				this.jjmatchedKind = 213;
				this.jjmatchedPos = 20;
				return 26;
			case 21:
				if ((active2 & -2147483648L) == 0L && (active3 & 131064L) == 0L) {
					if ((active2 & 2113929216L) == 0L && (active3 & 7L) == 0L) {
						return -1;
					}

					return 26;
				}

				if (this.jjmatchedPos != 21) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 21;
				}

				return 26;
			case 22:
				if ((active2 & -68719476736L) == 0L && (active3 & 131071L) == 0L) {
					if ((active2 & 66571993088L) != 0L) {
						return 26;
					}

					return -1;
				}

				this.jjmatchedKind = 213;
				this.jjmatchedPos = 22;
				return 26;
			case 23:
				if ((active2 & -2199023255552L) == 0L && (active3 & 131071L) == 0L) {
					if ((active2 & 2130303778816L) != 0L) {
						return 26;
					}

					return -1;
				}

				this.jjmatchedKind = 213;
				this.jjmatchedPos = 23;
				return 26;
			case 24:
				if ((active2 & -35184372088832L) == 0L && (active3 & 131071L) == 0L) {
					if ((active2 & 32985348833280L) != 0L) {
						return 26;
					}

					return -1;
				}

				this.jjmatchedKind = 213;
				this.jjmatchedPos = 24;
				return 26;
			case 25:
				if ((active2 & -18014398509481984L) == 0L && (active3 & 131071L) == 0L) {
					if ((active2 & 17979214137393152L) != 0L) {
						return 26;
					}

					return -1;
				}

				this.jjmatchedKind = 213;
				this.jjmatchedPos = 25;
				return 26;
			case 26:
				if ((active2 & -288230376151711744L) == 0L && (active3 & 65535L) == 0L) {
					if ((active2 & 270215977642229760L) == 0L && (active3 & 65536L) == 0L) {
						return -1;
					}

					return 26;
				}

				if (this.jjmatchedPos != 26) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 26;
				}

				return 26;
			case 27:
				if ((active2 & -4611686018427387904L) == 0L && (active3 & 131071L) == 0L) {
					if ((active2 & 4323455642275676160L) != 0L) {
						return 26;
					}

					return -1;
				}

				this.jjmatchedKind = 213;
				this.jjmatchedPos = 27;
				return 26;
			case 28:
				if ((active3 & 131064L) != 0L) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 28;
					return 26;
				} else {
					if ((active2 & -4611686018427387904L) == 0L && (active3 & 7L) == 0L) {
						return -1;
					}

					return 26;
				}
			case 29:
				if ((active3 & 131040L) != 0L) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 29;
					return 26;
				} else {
					if ((active3 & 24L) != 0L) {
						return 26;
					}

					return -1;
				}
			case 30:
				if ((active3 & 131008L) != 0L) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 30;
					return 26;
				} else {
					if ((active3 & 32L) != 0L) {
						return 26;
					}

					return -1;
				}
			case 31:
				if ((active3 & 130944L) != 0L) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 31;
					return 26;
				} else {
					if ((active3 & 64L) != 0L) {
						return 26;
					}

					return -1;
				}
			case 32:
				if ((active3 & 126976L) != 0L) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 32;
					return 26;
				} else {
					if ((active3 & 3968L) != 0L) {
						return 26;
					}

					return -1;
				}
			case 33:
				if ((active3 & 122880L) != 0L) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 33;
					return 26;
				} else {
					if ((active3 & 4096L) != 0L) {
						return 26;
					}

					return -1;
				}
			case 34:
				if ((active3 & 114688L) != 0L) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 34;
					return 26;
				} else {
					if ((active3 & 8192L) != 0L) {
						return 26;
					}

					return -1;
				}
			case 35:
				if ((active3 & 114688L) != 0L) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 35;
					return 26;
				}

				return -1;
			case 36:
				if ((active3 & 98304L) != 0L) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 36;
					return 26;
				} else {
					if ((active3 & 16384L) != 0L) {
						return 26;
					}

					return -1;
				}
			case 37:
				if ((active3 & 98304L) != 0L) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 37;
					return 26;
				}

				return -1;
			case 38:
				if ((active3 & 98304L) != 0L) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 38;
					return 26;
				}

				return -1;
			case 39:
				if ((active3 & 98304L) != 0L) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 39;
					return 26;
				}

				return -1;
			case 40:
				if ((active3 & 65536L) != 0L) {
					this.jjmatchedKind = 213;
					this.jjmatchedPos = 40;
					return 26;
				} else {
					if ((active3 & 32768L) != 0L) {
						return 26;
					}

					return -1;
				}
			default:
				return -1;
		}
	}

	private int jjMoveStringLiteralDfa15_0(long old1, long active1, long old2, long active2, long old3, long active3) {
		if (((active1 = active1 & old1) | (active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(13, 0L, old1, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(14, 0L, active1, active2, active3);
				return 15;
			}

			switch (this.curChar) {
				case 'A':
					return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 17592186044416L, active3, 0L);
				case 'B':
				case 'D':
				case 'E':
				case 'G':
				case 'I':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'N':
				case 'P':
				case 'Q':
				case 'S':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '`':
				case 'b':
				case 'g':
				case 'h':
				case 'j':
				case 'p':
				case 'q':
				case 'u':
				case 'w':
				default:
					return this.jjStartNfa_0(14, 0L, active1, active2, active3);
				case 'C':
					return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 134217728L, active3, 7L);
				case 'F':
					return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 0L, active3, 512L);
				case 'H':
					return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 8192L, active3, 0L);
				case 'O':
					return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 0L, active3, 384L);
				case 'R':
					return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 8796093022208L, active3, 0L);
				case '_':
					return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 33554432L, active3, 0L);
				case 'a':
					return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, -9223372036577799360L, active3, 1024L);
				case 'c':
					return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 18014399046680576L, active3, 0L);
				case 'd':
					if ((active1 & 9007199254740992L) != 0L) {
						return this.jjStartNfaWithStates_0(15, 117, 26);
					}

					return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 2252899327410176L, active3, 0L);
				case 'e':
					if ((active1 & 18014398509481984L) != 0L) {
						return this.jjStartNfaWithStates_0(15, 118, 26);
					} else if ((active1 & 36028797018963968L) != 0L) {
						return this.jjStartNfaWithStates_0(15, 119, 26);
					} else {
						if ((active1 & 72057594037927936L) != 0L) {
							return this.jjStartNfaWithStates_0(15, 120, 26);
						}

						return this.jjMoveStringLiteralDfa16_0(active1, 7205759403792793600L, active2, 1125899973951488L, active3, 10240L);
					}
				case 'f':
					return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 8589934592L, active3, 0L);
				case 'i':
					return this.jjMoveStringLiteralDfa16_0(active1, 144115188075855872L, active2, 864726312827257880L, active3, 0L);
				case 'k':
					return this.jjMoveStringLiteralDfa16_0(active1, 1152921504606846976L, active2, 0L, active3, 0L);
				case 'l':
					return this.jjMoveStringLiteralDfa16_0(active1, -8646911284551352320L, active2, 2305843421530554368L, active3, 0L);
				case 'm':
					return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 36028797018963968L, active3, 49152L);
				case 'n':
					return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 1167140408304601120L, active3, 16L);
				case 'o':
					if ((active1 & 1125899906842624L) != 0L) {
						return this.jjStartNfaWithStates_0(15, 114, 26);
					}

					return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 72128036870291456L, active3, 65536L);
				case 'r':
					return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 524289L, active3, 72L);
				case 's':
					if ((active1 & 281474976710656L) != 0L) {
						return this.jjStartNfaWithStates_0(15, 112, 26);
					} else if ((active1 & 562949953421312L) != 0L) {
						return this.jjStartNfaWithStates_0(15, 113, 26);
					} else {
						if ((active1 & 4503599627370496L) != 0L) {
							return this.jjStartNfaWithStates_0(15, 116, 26);
						}

						return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 144115188075855872L, active3, 0L);
					}
				case 't':
					if ((active1 & 2251799813685248L) != 0L) {
						return this.jjStartNfaWithStates_0(15, 115, 26);
					}

					return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 4611968077524893826L, active3, 0L);
				case 'v':
					return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 4L, active3, 0L);
				case 'x':
					return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 16777216L, active3, 4096L);
				case 'y':
					return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 0L, active3, 32L);
				case 'z':
					return this.jjMoveStringLiteralDfa16_0(active1, 0L, active2, 2199023255552L, active3, 0L);
			}
		}
	}

	protected ZKMScriptToken jjFillToken() {
		String literalImage = jjstrLiteralImages[this.jjmatchedKind];
		String tokenImage = literalImage == null ? this.input_stream.GetImage() : literalImage;
		int beginLine = this.input_stream.getBeginLine();
		int beginColumn = this.input_stream.getBeginColumn();
		int endLine = this.input_stream.getColumn();
		int endColumn = this.input_stream.getEndColumn();
		ZKMScriptToken token = ZKMScriptToken.newToken(this.jjmatchedKind);
		token.kind = this.jjmatchedKind;
		token.image = tokenImage;
		token.beginLine = beginLine;
		token.endLine = endLine;
		token.beginColumn = beginColumn;
		token.endColumn = endColumn;
		return token;
	}

	private int jjMoveStringLiteralDfa1_0(long active0, long active1, long active2, long active3) {
		try {
			this.curChar = this.input_stream.readChar();
		} catch (IOException ignored) {
			this.jjStopStringLiteralDfa_0(0, active0, active1, active2, active3);
			return 1;
		}

		switch (this.curChar) {
			case '&':
				if ((active0 & 34359738368L) != 0L) {
					return this.jjStopAtPos(1, 35);
				}
			case '\'':
			case '(':
			case ')':
			case '+':
			case ',':
			case '-':
			case '.':
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
			case ':':
			case ';':
			case '<':
			case '=':
			case '>':
			case '?':
			case '@':
			case 'A':
			case 'B':
			case 'C':
			case 'D':
			case 'E':
			case 'F':
			case 'G':
			case 'H':
			case 'I':
			case 'J':
			case 'K':
			case 'L':
			case 'M':
			case 'N':
			case 'O':
			case 'P':
			case 'Q':
			case 'R':
			case 'T':
			case 'U':
			case 'V':
			case 'W':
			case 'X':
			case 'Y':
			case 'Z':
			case '[':
			case '\\':
			case ']':
			case '^':
			case '_':
			case '`':
			case 'd':
			case 'j':
			case 'k':
			case 'q':
			case 'v':
			case 'w':
			case 'z':
			case '{':
			default:
				break;
			case '*':
				if ((active0 & 256L) != 0L) {
					return this.jjStartNfaWithStates_0(1, 8, 0);
				}
				break;
			case '/':
				if ((active0 & 64L) != 0L) {
					return this.jjStopAtPos(1, 6);
				}
				break;
			case 'S':
				return this.jjMoveStringLiteralDfa2_0(active0, 2251799813685248L, active1, 0L, active2, 0L, active3, 0L);
			case 'a':
				return this.jjMoveStringLiteralDfa2_0(active0, -8646348334597931008L, active1, 216172790739369994L, active2, 0L, active3, 0L);
			case 'b':
				return this.jjMoveStringLiteralDfa2_0(active0, 0L, active1, 137439084800L, active2, 1454838601568290944L, active3, 0L);
			case 'c':
				if ((active0 & 68719476736L) != 0L) {
					return this.jjStartNfaWithStates_0(1, 36, 26);
				}

				return this.jjMoveStringLiteralDfa2_0(active0, 0L, active1, 640L, active2, 18014398509744128L, active3, 0L);
			case 'e':
				return this.jjMoveStringLiteralDfa2_0(active0, 1225264971667996672L, active1, -2821914184336277504L, active2, -1653075701521181653L, active3, 131071L);
			case 'f':
				return this.jjMoveStringLiteralDfa2_0(active0, 0L, active1, 4294967296L, active2, 512L, active3, 0L);
			case 'g':
				return this.jjMoveStringLiteralDfa2_0(active0, 0L, active1, 67108864L, active2, 1108101562368L, active3, 0L);
			case 'h':
				return this.jjMoveStringLiteralDfa2_0(active0, 4611686018427387904L, active1, 2322718313676800L, active2, 0L, active3, 0L);
			case 'i':
				return this.jjMoveStringLiteralDfa2_0(active0, 19281035904679936L, active1, 2218350608384L, active2, 2097408L, active3, 0L);
			case 'l':
				return this.jjMoveStringLiteralDfa2_0(active0, 9007336693694464L, active1, 9007474132713472L, active2, 36029346774777856L, active3, 0L);
			case 'm':
				return this.jjMoveStringLiteralDfa2_0(active0, 0L, active1, 134217728L, active2, 0L, active3, 0L);
			case 'n':
				return this.jjMoveStringLiteralDfa2_0(active0, 1374389534720L, active1, 288529443323413504L, active2, 70441763880960L, active3, 0L);
			case 'o':
				return this.jjMoveStringLiteralDfa2_0(active0, 2305851805306716160L, active1, 1099796846656L, active2, 144115190240149504L, active3, 0L);
			case 'p':
				return this.jjMoveStringLiteralDfa2_0(active0, 36029346774777856L, active1, 0L, active2, 4L, active3, 0L);
			case 'r':
				return this.jjMoveStringLiteralDfa2_0(active0, 4611351766892544L, active1, 69798739984L, active2, 16L, active3, 0L);
			case 's':
				return this.jjMoveStringLiteralDfa2_0(active0, 17592186044416L, active1, 2305843009213693952L, active2, 524288L, active3, 0L);
			case 't':
				return this.jjMoveStringLiteralDfa2_0(active0, 288230376151711744L, active1, 0L, active2, 4415226380288L, active3, 0L);
			case 'u':
				return this.jjMoveStringLiteralDfa2_0(active0, 144115188075855872L, active1, 0L, active2, 276824128L, active3, 0L);
			case 'x':
				return this.jjMoveStringLiteralDfa2_0(active0, 0L, active1, 35184372088869L, active2, 2199056949248L, active3, 0L);
			case 'y':
				return this.jjMoveStringLiteralDfa2_0(active0, 0L, active1, 34359746560L, active2, 0L, active3, 0L);
			case '|':
				if ((active0 & 17179869184L) != 0L) {
					return this.jjStopAtPos(1, 34);
				}
		}

		return this.jjStartNfa_0(0, active0, active1, active2, active3);
	}

	private int jjMoveStringLiteralDfa17_0(long old1, long active1, long old2, long active2, long old3, long active3) {
		long maskedActive1 = active1 & old1;
		long duplicatedMaskedActive1 = active1 & old1;
		if ((maskedActive1 | (active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(15, 0L, old1, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(16, 0L, 0L, active2, active3);
				return 17;
			}

			switch (this.curChar) {
				case '2':
					return this.jjMoveStringLiteralDfa18_0(active2, 8192L, active3, 0L);
				case 'C':
					return this.jjMoveStringLiteralDfa18_0(active2, 1073741824L, active3, 0L);
				case 'E':
					return this.jjMoveStringLiteralDfa18_0(active2, 343597383680L, active3, 0L);
				case 'H':
					return this.jjMoveStringLiteralDfa18_0(active2, 33554432L, active3, 0L);
				case 'I':
					return this.jjMoveStringLiteralDfa18_0(active2, 137438953472L, active3, 0L);
				case 'P':
					return this.jjMoveStringLiteralDfa18_0(active2, 72057594037927936L, active3, 65536L);
				case 'U':
					return this.jjMoveStringLiteralDfa18_0(active2, 70368744177664L, active3, 0L);
				case 'a':
					return this.jjMoveStringLiteralDfa18_0(active2, 136314880L, active3, 15L);
				case 'b':
					return this.jjMoveStringLiteralDfa18_0(active2, 281474976710656L, active3, 0L);
				case 'c':
					return this.jjMoveStringLiteralDfa18_0(active2, 67108864L, active3, 0L);
				case 'd':
					return this.jjMoveStringLiteralDfa18_0(active2, 2199291691008L, active3, 0L);
				case 'e':
					if ((active2 & 8L) != 0L) {
						return this.jjStartNfaWithStates_0(17, 131, 26);
					}

					return this.jjMoveStringLiteralDfa18_0(active2, 1166574140488981248L, active3, 15360L);
				case 'f':
					return this.jjMoveStringLiteralDfa18_0(active2, 8796093022208L, active3, 384L);
				case 'g':
					return this.jjMoveStringLiteralDfa18_0(active2, 576460752303423488L, active3, 0L);
				case 'h':
					if ((active2 & 64L) != 0L) {
						return this.jjStartNfaWithStates_0(17, 134, 26);
					}
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
				case ':':
				case ';':
				case '<':
				case '=':
				case '>':
				case '?':
				case '@':
				case 'A':
				case 'B':
				case 'D':
				case 'F':
				case 'G':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'N':
				case 'O':
				case 'Q':
				case 'R':
				case 'S':
				case 'T':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'j':
				case 'm':
				case 'q':
				case 'v':
				default:
					return this.jjStartNfa_0(16, 0L, 0L, active2, active3);
				case 'i':
					return this.jjMoveStringLiteralDfa18_0(active2, -9061242415908782080L, active3, 16L);
				case 'k':
					return this.jjMoveStringLiteralDfa18_0(active2, 8388608L, active3, 0L);
				case 'l':
					return this.jjMoveStringLiteralDfa18_0(active2, 16777216L, active3, 0L);
				case 'n':
					if ((active2 & 16L) != 0L) {
						return this.jjStartNfaWithStates_0(17, 132, 26);
					}

					return this.jjMoveStringLiteralDfa18_0(active2, 288269958571361280L, active3, 0L);
				case 'o':
					if ((active2 & 32L) != 0L) {
						return this.jjStartNfaWithStates_0(17, 133, 26);
					}

					return this.jjMoveStringLiteralDfa18_0(active2, 4611686568720072704L, active3, 512L);
				case 'p':
					return this.jjMoveStringLiteralDfa18_0(active2, 0L, active3, 64L);
				case 'r':
					return this.jjMoveStringLiteralDfa18_0(active2, 8594129024L, active3, 0L);
				case 's':
					if ((active2 & 4L) != 0L) {
						return this.jjStartNfaWithStates_0(17, 130, 26);
					}

					return this.jjMoveStringLiteralDfa18_0(active2, 4294983680L, active3, 0L);
				case 't':
					return this.jjMoveStringLiteralDfa18_0(active2, 39987038878957568L, active3, 49184L);
				case 'u':
					return this.jjMoveStringLiteralDfa18_0(active2, 65536L, active3, 0L);
				case 'w':
					return this.jjMoveStringLiteralDfa18_0(active2, 2305843009213693952L, active3, 0L);
				case 'x':
					return this.jjMoveStringLiteralDfa18_0(active2, 19327352832L, active3, 0L);
			}
		}
	}

	public void SkipLexicalActions(ZKMScriptToken matchedToken) {
	}

	private int jjMoveStringLiteralDfa1_3(long active0) {
		try {
			this.curChar = this.input_stream.readChar();
		} catch (IOException ignored) {
			return 1;
		}

		switch (this.curChar) {
			case '/':
				if ((active0 & 2048L) != 0L) {
					return this.jjStopAtPos(1, 11);
				}

				return 2;
			default:
				return 2;
		}
	}

	private int jjMoveStringLiteralDfa0_0() {
		switch (this.curChar) {
			case '!':
				return this.jjStopAtPos(0, 25);
			case '"':
				return this.jjStopAtPos(0, 209);
			case '&':
				return this.jjMoveStringLiteralDfa1_0(34359738368L, 0L, 0L, 0L);
			case '(':
				return this.jjStopAtPos(0, 28);
			case ')':
				return this.jjStopAtPos(0, 29);
			case '*':
				return this.jjStartNfaWithStates_0(0, 23, 25);
			case '+':
				this.jjmatchedKind = 24;
				return this.jjMoveStringLiteralDfa1_0(0L, 2305843009213693952L, 0L, 0L);
			case ',':
				return this.jjStopAtPos(0, 20);
			case '-':
				return this.jjStartNfaWithStates_0(0, 16, 7);
			case '.':
				return this.jjStopAtPos(0, 21);
			case '/':
				this.jjmatchedKind = 22;
				return this.jjMoveStringLiteralDfa1_0(320L, 0L, 0L, 0L);
			case ';':
				return this.jjStopAtPos(0, 19);
			case '<':
				return this.jjMoveStringLiteralDfa1_0(27021597764222976L, 512L, 0L, 0L);
			case '=':
				return this.jjStopAtPos(0, 15);
			case '>':
				return this.jjStopAtPos(0, 33);
			case '?':
				return this.jjStopAtPos(0, 32);
			case '@':
				return this.jjStopAtPos(0, 27);
			case 'A':
				return this.jjMoveStringLiteralDfa1_0(2251799813685248L, 0L, 0L, 0L);
			case '[':
				return this.jjStopAtPos(0, 17);
			case ']':
				return this.jjStopAtPos(0, 18);
			case '^':
				return this.jjStopAtPos(0, 26);
			case 'a':
				return this.jjMoveStringLiteralDfa1_0(18004502904832L, 9007199330238720L, 54044295317684304L, 0L);
			case 'b':
				return this.jjMoveStringLiteralDfa1_0(4503599627370496L, 0L, 0L, 0L);
			case 'c':
				return this.jjMoveStringLiteralDfa1_0(0L, 2322718598955008L, 144115737831669760L, 0L);
			case 'd':
				return this.jjMoveStringLiteralDfa1_0(1152921504606846976L, 0L, 3958276756602882L, 1552L);
			case 'e':
				return this.jjMoveStringLiteralDfa1_0(1099511627776L, 35184372089893L, 2203357159424L, 0L);
			case 'f':
				return this.jjMoveStringLiteralDfa1_0(1688849860263936L, 292057776128L, 0L, 0L);
			case 'g':
				return this.jjMoveStringLiteralDfa1_0(68719476736L, 4194304L, 0L, 0L);
			case 'h':
				return this.jjMoveStringLiteralDfa1_0(281474976710656L, 2199023255552L, 2097152L, 0L);
			case 'i':
				return this.jjMoveStringLiteralDfa1_0(0L, 17596615753728L, 70446053605888L, 0L);
			case 'k':
				return this.jjMoveStringLiteralDfa1_0(4398046511104L, 1154047405050560512L, 72339069014642720L, 73728L);
			case 'l':
				return this.jjMoveStringLiteralDfa1_0(140737488355328L, 76562295324409856L, 2164293632L, 0L);
			case 'm':
				return this.jjMoveStringLiteralDfa1_0(0L, 144678138029277248L, 134217984L, 391L);
			case 'n':
				return this.jjMoveStringLiteralDfa1_0(2882312557610139648L, 630508345878384640L, 9L, 0L);
			case 'o':
				return this.jjMoveStringLiteralDfa1_0(549755813888L, 137439084544L, 1454838601568290948L, 0L);
			case 'p':
				return this.jjMoveStringLiteralDfa1_0(144185556820033536L, 8590213144L, 0L, 0L);
			case 'r':
				return this.jjMoveStringLiteralDfa1_0(Long.MIN_VALUE, -4611536484843913216L, -1729373047426644992L, 55400L);
			case 's':
				return this.jjMoveStringLiteralDfa1_0(396316767208603648L, 34393301122L, 4415226380288L, 0L);
			case 't':
				return this.jjMoveStringLiteralDfa1_0(4611723401822732288L, 69794267136L, 0L, 0L);
			case 'u':
				return this.jjMoveStringLiteralDfa1_0(0L, 288511851128455168L, 0L, 0L);
			case 'v':
				return this.jjMoveStringLiteralDfa1_0(0L, 2048L, 0L, 0L);
			case '{':
				return this.jjStopAtPos(0, 30);
			case '|':
				return this.jjMoveStringLiteralDfa1_0(17179869184L, 0L, 0L, 0L);
			case '}':
				return this.jjStopAtPos(0, 31);
			case '\ufeff':
				return this.jjStartNfaWithStates_0(0, 14, 26);
			default:
				return this.jjMoveNfa_0(3, 0);
		}
	}

	private void jjCheckNAddStates(int start, int end) {
		do {
			this.jjCheckNAdd(jjnextStates[start]);
		} while (start++ != end);
	}

	private int jjMoveStringLiteralDfa25_0(long old2, long active2, long old3, long active3) {
		if (((active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(23, 0L, 0L, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(24, 0L, 0L, active2, active3);
				return 25;
			}

			switch (this.curChar) {
				case 'O':
					return this.jjMoveStringLiteralDfa26_0(active2, 0L, active3, 8192L);
				case 'a':
					return this.jjMoveStringLiteralDfa26_0(active2, 0L, active3, 8L);
				case 'b':
					return this.jjMoveStringLiteralDfa26_0(active2, 0L, active3, 16L);
				case 'c':
					return this.jjMoveStringLiteralDfa26_0(active2, 576460752303423488L, active3, 34816L);
				case 'd':
					if ((active2 & 281474976710656L) != 0L) {
						return this.jjStartNfaWithStates_0(25, 176, 26);
					}

					return this.jjMoveStringLiteralDfa26_0(active2, 18014398509481984L, active3, 0L);
				case 'e':
					if ((active2 & 35184372088832L) != 0L) {
						return this.jjStartNfaWithStates_0(25, 173, 26);
					} else if ((active2 & 70368744177664L) != 0L) {
						return this.jjStartNfaWithStates_0(25, 174, 26);
					} else if ((active2 & 140737488355328L) != 0L) {
						return this.jjStartNfaWithStates_0(25, 175, 26);
					} else if ((active2 & 4503599627370496L) != 0L) {
						return this.jjStartNfaWithStates_0(25, 180, 26);
					} else {
						if ((active2 & 9007199254740992L) != 0L) {
							return this.jjStartNfaWithStates_0(25, 181, 26);
						}

						return this.jjMoveStringLiteralDfa26_0(active2, 36028797018963968L, active3, 16384L);
					}
				case 'h':
					return this.jjMoveStringLiteralDfa26_0(active2, 0L, active3, 512L);
				case 'i':
					return this.jjMoveStringLiteralDfa26_0(active2, 4611686018427387904L, active3, 0L);
				case 'k':
					return this.jjMoveStringLiteralDfa26_0(active2, 0L, active3, 4L);
				case 'l':
					return this.jjMoveStringLiteralDfa26_0(active2, 144115188075855872L, active3, 67L);
				case 'n':
					return this.jjMoveStringLiteralDfa26_0(active2, 0L, active3, 384L);
				case 'o':
					return this.jjMoveStringLiteralDfa26_0(active2, 2305843009213693952L, active3, 0L);
				case 'p':
					return this.jjMoveStringLiteralDfa26_0(active2, 0L, active3, 1024L);
				case 'r':
					return this.jjMoveStringLiteralDfa26_0(active2, -7998392938210000896L, active3, 65536L);
				case 's':
					if ((active2 & 562949953421312L) != 0L) {
						return this.jjStartNfaWithStates_0(25, 177, 26);
					} else if ((active2 & 1125899906842624L) != 0L) {
						return this.jjStartNfaWithStates_0(25, 178, 26);
					} else if ((active2 & 2251799813685248L) != 0L) {
						return this.jjStartNfaWithStates_0(25, 179, 26);
					}
				case 'P':
				case 'Q':
				case 'R':
				case 'S':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'f':
				case 'g':
				case 'j':
				case 'm':
				case 'q':
				case 't':
				case 'v':
				case 'w':
				default:
					return this.jjStartNfa_0(24, 0L, 0L, active2, active3);
				case 'u':
					return this.jjMoveStringLiteralDfa26_0(active2, 288230376151711744L, active3, 32L);
				case 'x':
					return this.jjMoveStringLiteralDfa26_0(active2, 0L, active3, 4096L);
			}
		}
	}

	private int jjMoveStringLiteralDfa21_0(long old2, long active2, long old3, long active3) {
		if (((active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(19, 0L, 0L, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(20, 0L, 0L, active2, active3);
				return 21;
			}

			switch (this.curChar) {
				case '6':
					if ((active2 & 33554432L) != 0L) {
						return this.jjStartNfaWithStates_0(21, 153, 26);
					}
				case '7':
				case '8':
				case '9':
				case ':':
				case ';':
				case '<':
				case '=':
				case '>':
				case '?':
				case '@':
				case 'A':
				case 'B':
				case 'C':
				case 'D':
				case 'F':
				case 'G':
				case 'H':
				case 'I':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'N':
				case 'O':
				case 'P':
				case 'Q':
				case 'R':
				case 'S':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'j':
				case 'k':
				case 'q':
				case 'r':
				case 'v':
				case 'w':
				default:
					break;
				case 'E':
					return this.jjMoveStringLiteralDfa22_0(active2, 0L, active3, 32L);
				case 'a':
					return this.jjMoveStringLiteralDfa22_0(active2, 0L, active3, 384L);
				case 'b':
					return this.jjMoveStringLiteralDfa22_0(active2, 3940649673949184L, active3, 32768L);
				case 'c':
					return this.jjMoveStringLiteralDfa22_0(active2, 4625724582890831872L, active3, 2048L);
				case 'd':
					return this.jjMoveStringLiteralDfa22_0(active2, 569083166720L, active3, 0L);
				case 'e':
					if ((active2 & 67108864L) != 0L) {
						return this.jjStartNfaWithStates_0(21, 154, 26);
					}

					return this.jjMoveStringLiteralDfa22_0(active2, 1008806359480664064L, active3, 8L);
				case 'f':
					return this.jjMoveStringLiteralDfa22_0(active2, 0L, active3, 8192L);
				case 'g':
					if ((active2 & 268435456L) != 0L) {
						return this.jjStartNfaWithStates_0(21, 156, 26);
					}
					break;
				case 'h':
					return this.jjMoveStringLiteralDfa22_0(active2, 36028797018963968L, active3, 16384L);
				case 'i':
					return this.jjMoveStringLiteralDfa22_0(active2, -9223370937343148032L, active3, 0L);
				case 'l':
					return this.jjMoveStringLiteralDfa22_0(active2, 2305847407260205056L, active3, 0L);
				case 'm':
					return this.jjMoveStringLiteralDfa22_0(active2, 72057594037927936L, active3, 65536L);
				case 'n':
					return this.jjMoveStringLiteralDfa22_0(active2, 0L, active3, 64L);
				case 'o':
					return this.jjMoveStringLiteralDfa22_0(active2, 0L, active3, 5120L);
				case 'p':
					return this.jjMoveStringLiteralDfa22_0(active2, 0L, active3, 512L);
				case 's':
					if ((active2 & 134217728L) != 0L) {
						this.jjmatchedKind = 155;
						this.jjmatchedPos = 21;
					} else {
						if ((active2 & 536870912L) != 0L) {
							return this.jjStartNfaWithStates_0(21, 157, 26);
						}

						if ((active2 & 1073741824L) != 0L) {
							return this.jjStartNfaWithStates_0(21, 158, 26);
						}
					}

					return this.jjMoveStringLiteralDfa22_0(active2, 2199023255552L, active3, 7L);
				case 't':
					return this.jjMoveStringLiteralDfa22_0(active2, 8800387989504L, active3, 16L);
				case 'u':
					return this.jjMoveStringLiteralDfa22_0(active2, 1152939577829228544L, active3, 0L);
				case 'x':
					return this.jjMoveStringLiteralDfa22_0(active2, 18014398509481984L, active3, 0L);
			}

			return this.jjStartNfa_0(20, 0L, 0L, active2, active3);
		}
	}

	private int jjMoveStringLiteralDfa35_0(long old3, long active3) {
		if ((active3 = active3 & old3) == 0L) {
			return this.jjStartNfa_0(33, 0L, 0L, 0L, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(34, 0L, 0L, 0L, active3);
				return 35;
			}

			switch (this.curChar) {
				case 'n':
					return this.jjMoveStringLiteralDfa36_0(active3, 16384L);
				case 'u':
					return this.jjMoveStringLiteralDfa36_0(active3, 98304L);
				default:
					return this.jjStartNfa_0(34, 0L, 0L, 0L, active3);
			}
		}
	}

	private int jjMoveNfa_4(int startState, int curPos) {
		int startsAt = 0;
		this.jjnewStateCnt = 3;
		int i = 1;
		this.jjstateSet[0] = startState;
		int kind = Integer.MAX_VALUE;

		while (true) {
			if (++this.jjround == Integer.MAX_VALUE) {
				this.ReInitRounds();
			}

			if (this.curChar < '@') {
				long lowAsciiBitMask = 1L << this.curChar;

				do {
					i--;
					switch (this.jjstateSet[i]) {
						case 0:
							if ((-17179882497L & lowAsciiBitMask) != 0L) {
								if (kind > 211) {
									kind = 211;
								}
							} else if (this.curChar == '"') {
								this.jjstateSet[this.jjnewStateCnt++] = 2;
							}
							break;
						case 1:
							if (this.curChar == '"') {
								this.jjstateSet[this.jjnewStateCnt++] = 2;
							}
							break;
						case 2:
							if (this.curChar == '"' && kind > 211) {
								kind = 211;
							}
					}
				} while (i != startsAt);
			} else if (this.curChar < 128) {
				long highAsciiBitMask = 1L << (this.curChar & '?');

				do {
					i--;
					switch (this.jjstateSet[i]) {
						case 0:
							kind = 211;
					}
				} while (i != startsAt);
			} else {
				int highByte = this.curChar >> '\b';
				int highBitVecIndex = highByte >> 6;
				long highByteBitMask = 1L << (highByte & 63);
				int lowBitVecIndex = (this.curChar & 255) >> 6;
				long lowByteBitMask = 1L << (this.curChar & '?');

				do {
					i--;
					switch (this.jjstateSet[i]) {
						case 0:
							if (jjCanMove_1(highByte, highBitVecIndex, lowBitVecIndex, highByteBitMask, lowByteBitMask) && kind > 211) {
								kind = 211;
							}
					}
				} while (i != startsAt);
			}

			if (kind != Integer.MAX_VALUE) {
				this.jjmatchedKind = kind;
				this.jjmatchedPos = curPos;
				kind = Integer.MAX_VALUE;
			}

			curPos++;
			if ((i = this.jjnewStateCnt) == (startsAt = 3 - (this.jjnewStateCnt = startsAt))) {
				return curPos;
			}

			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				return curPos;
			}
		}
	}

	private int jjMoveStringLiteralDfa20_0(long old2, long active2, long old3, long active3) {
		if (((active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(18, 0L, 0L, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(19, 0L, 0L, active2, active3);
				return 20;
			}

			switch (this.curChar) {
				case '5':
					return this.jjMoveStringLiteralDfa21_0(active2, 33554432L, active3, 0L);
				case '6':
				case '7':
				case '8':
				case '9':
				case ':':
				case ';':
				case '<':
				case '=':
				case '>':
				case '?':
				case '@':
				case 'B':
				case 'F':
				case 'G':
				case 'H':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'N':
				case 'P':
				case 'Q':
				case 'R':
				case 'S':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'g':
				case 'h':
				case 'j':
				case 'k':
				case 'p':
				case 'q':
				case 'v':
				case 'w':
				case 'y':
				default:
					return this.jjStartNfa_0(19, 0L, 0L, active2, active3);
				case 'A':
					return this.jjMoveStringLiteralDfa21_0(active2, 0L, active3, 16L);
				case 'C':
					return this.jjMoveStringLiteralDfa21_0(active2, 36028797018963968L, active3, 16384L);
				case 'D':
					return this.jjMoveStringLiteralDfa21_0(active2, 144115188075855872L, active3, 0L);
				case 'E':
					return this.jjMoveStringLiteralDfa21_0(active2, 18014398509481984L, active3, 0L);
				case 'I':
					return this.jjMoveStringLiteralDfa21_0(active2, 0L, active3, 8192L);
				case 'O':
					return this.jjMoveStringLiteralDfa21_0(active2, 0L, active3, 32768L);
				case 'a':
					return this.jjMoveStringLiteralDfa21_0(active2, 72200530549538816L, active3, 65536L);
				case 'b':
					return this.jjMoveStringLiteralDfa21_0(active2, 17592186044416L, active3, 0L);
				case 'c':
					return this.jjMoveStringLiteralDfa21_0(active2, 2305856211943161856L, active3, 384L);
				case 'd':
					return this.jjMoveStringLiteralDfa21_0(active2, 67108864L, active3, 0L);
				case 'e':
					if ((active2 & 8388608L) != 0L) {
						return this.jjStartNfaWithStates_0(20, 151, 26);
					} else {
						if ((active2 & 16777216L) != 0L) {
							return this.jjStartNfaWithStates_0(20, 152, 26);
						}

						return this.jjMoveStringLiteralDfa21_0(active2, 671088640L, active3, 7L);
					}
				case 'f':
					return this.jjMoveStringLiteralDfa21_0(active2, 576460752303423488L, active3, 0L);
				case 'i':
					return this.jjMoveStringLiteralDfa21_0(active2, 3940649673949184L, active3, 4096L);
				case 'l':
					return this.jjMoveStringLiteralDfa21_0(active2, 482110078976L, active3, 0L);
				case 'm':
					return this.jjMoveStringLiteralDfa21_0(active2, 1099511627776L, active3, 0L);
				case 'n':
					return this.jjMoveStringLiteralDfa21_0(active2, 292733980342484992L, active3, 2592L);
				case 'o':
					return this.jjMoveStringLiteralDfa21_0(active2, 0L, active3, 64L);
				case 'r':
					return this.jjMoveStringLiteralDfa21_0(active2, 1152922054362660864L, active3, 1024L);
				case 's':
					if ((active2 & 2097152L) != 0L) {
						return this.jjStartNfaWithStates_0(20, 149, 26);
					} else {
						if ((active2 & 4194304L) != 0L) {
							return this.jjStartNfaWithStates_0(20, 150, 26);
						}

						return this.jjMoveStringLiteralDfa21_0(active2, 281474976710656L, active3, 0L);
					}
				case 't':
					return this.jjMoveStringLiteralDfa21_0(active2, -9223372002495037440L, active3, 0L);
				case 'u':
					return this.jjMoveStringLiteralDfa21_0(active2, 19327352832L, active3, 0L);
				case 'x':
					return this.jjMoveStringLiteralDfa21_0(active2, 4620798770798395392L, active3, 0L);
				case 'z':
					return this.jjMoveStringLiteralDfa21_0(active2, 0L, active3, 8L);
			}
		}
	}

	private int jjMoveStringLiteralDfa32_0(long old3, long active3) {
		if ((active3 = active3 & old3) == 0L) {
			return this.jjStartNfa_0(30, 0L, 0L, 0L, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(31, 0L, 0L, 0L, active3);
				return 32;
			}

			switch (this.curChar) {
				case 'O':
					return this.jjMoveStringLiteralDfa33_0(active3, 65536L);
				case 'e':
					if ((active3 & 128L) != 0L) {
						return this.jjStartNfaWithStates_0(32, 199, 26);
					}

					if ((active3 & 256L) != 0L) {
						return this.jjStartNfaWithStates_0(32, 200, 26);
					}
					break;
				case 'g':
					if ((active3 & 512L) != 0L) {
						return this.jjStartNfaWithStates_0(32, 201, 26);
					}
					break;
				case 'n':
					return this.jjMoveStringLiteralDfa33_0(active3, 4096L);
				case 's':
					if ((active3 & 1024L) != 0L) {
						return this.jjStartNfaWithStates_0(32, 202, 26);
					}

					if ((active3 & 2048L) != 0L) {
						return this.jjStartNfaWithStates_0(32, 203, 26);
					}

					return this.jjMoveStringLiteralDfa33_0(active3, 16384L);
				case 't':
					return this.jjMoveStringLiteralDfa33_0(active3, 8192L);
				case 'x':
					return this.jjMoveStringLiteralDfa33_0(active3, 32768L);
			}

			return this.jjStartNfa_0(31, 0L, 0L, 0L, active3);
		}
	}

	private int jjMoveStringLiteralDfa18_0(long old2, long active2, long old3, long active3) {
		if (((active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(16, 0L, 0L, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(17, 0L, 0L, active2, active3);
				return 18;
			}

			switch (this.curChar) {
				case '5':
					return this.jjMoveStringLiteralDfa19_0(active2, 8192L, active3, 0L);
				case '6':
				case '7':
				case '8':
				case '9':
				case ':':
				case ';':
				case '<':
				case '=':
				case '>':
				case '?':
				case '@':
				case 'B':
				case 'D':
				case 'F':
				case 'G':
				case 'H':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'N':
				case 'O':
				case 'P':
				case 'Q':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'g':
				case 'h':
				case 'j':
				case 'k':
				case 'q':
				case 'v':
				case 'w':
				case 'y':
				default:
					return this.jjStartNfa_0(17, 0L, 0L, active2, active3);
				case 'A':
					return this.jjMoveStringLiteralDfa19_0(active2, 33554432L, active3, 0L);
				case 'C':
					return this.jjMoveStringLiteralDfa19_0(active2, 2199023255552L, active3, 0L);
				case 'E':
					return this.jjMoveStringLiteralDfa19_0(active2, 2305843009213693952L, active3, 0L);
				case 'I':
					return this.jjMoveStringLiteralDfa19_0(active2, 32768L, active3, 0L);
				case 'R':
					return this.jjMoveStringLiteralDfa19_0(active2, 576460752303423488L, active3, 0L);
				case 'S':
					return this.jjMoveStringLiteralDfa19_0(active2, 1152921504606846976L, active3, 0L);
				case 'a':
					return this.jjMoveStringLiteralDfa19_0(active2, 72057595124252672L, active3, 65536L);
				case 'b':
					return this.jjMoveStringLiteralDfa19_0(active2, 34359738368L, active3, 0L);
				case 'c':
					return this.jjMoveStringLiteralDfa19_0(active2, 19327352832L, active3, 0L);
				case 'd':
					if ((active2 & 512L) != 0L) {
						return this.jjStartNfaWithStates_0(18, 137, 26);
					} else {
						if ((active2 & 4096L) != 0L) {
							return this.jjStartNfaWithStates_0(18, 140, 26);
						}

						return this.jjMoveStringLiteralDfa19_0(active2, 65536L, active3, 0L);
					}
				case 'e':
					return this.jjMoveStringLiteralDfa19_0(active2, 36033203655426048L, active3, 49152L);
				case 'f':
					return this.jjMoveStringLiteralDfa19_0(active2, 281474976710656L, active3, 0L);
				case 'i':
					return this.jjMoveStringLiteralDfa19_0(active2, 0L, active3, 32L);
				case 'l':
					return this.jjMoveStringLiteralDfa19_0(active2, 8796428566528L, active3, 8L);
				case 'm':
					return this.jjMoveStringLiteralDfa19_0(active2, 2097152L, active3, 512L);
				case 'n':
					return this.jjMoveStringLiteralDfa19_0(active2, 4611758174012178432L, active3, 7L);
				case 'o':
					return this.jjMoveStringLiteralDfa19_0(active2, 18014398510399488L, active3, 16L);
				case 'p':
					return this.jjMoveStringLiteralDfa19_0(active2, 0L, active3, 4096L);
				case 'r':
					return this.jjMoveStringLiteralDfa19_0(active2, 17592722915328L, active3, 10240L);
				case 's':
					if ((active2 & 128L) != 0L) {
						return this.jjStartNfaWithStates_0(18, 135, 26);
					} else if ((active2 & 256L) != 0L) {
						return this.jjStartNfaWithStates_0(18, 136, 26);
					} else {
						if ((active2 & 1024L) != 0L) {
							return this.jjStartNfaWithStates_0(18, 138, 26);
						}

						if ((active2 & 2048L) != 0L) {
							this.jjmatchedKind = 139;
							this.jjmatchedPos = 18;
						}

						return this.jjMoveStringLiteralDfa19_0(active2, 301917096894267392L, active3, 1024L);
					}
				case 't':
					return this.jjMoveStringLiteralDfa19_0(active2, 148055842045820928L, active3, 64L);
				case 'u':
					return this.jjMoveStringLiteralDfa19_0(active2, 16777216L, active3, 384L);
				case 'x':
					return this.jjMoveStringLiteralDfa19_0(active2, 343597383680L, active3, 0L);
				case 'z':
					return this.jjMoveStringLiteralDfa19_0(active2, Long.MIN_VALUE, active3, 0L);
			}
		}
	}

	private int jjMoveStringLiteralDfa31_0(long old3, long active3) {
		if ((active3 = active3 & old3) == 0L) {
			return this.jjStartNfa_0(29, 0L, 0L, 0L, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(30, 0L, 0L, 0L, active3);
				return 31;
			}

			switch (this.curChar) {
				case 'E':
					return this.jjMoveStringLiteralDfa32_0(active3, 32768L);
				case 'a':
					return this.jjMoveStringLiteralDfa32_0(active3, 8192L);
				case 'd':
					return this.jjMoveStringLiteralDfa32_0(active3, 384L);
				case 'n':
					return this.jjMoveStringLiteralDfa32_0(active3, 2048L);
				case 'o':
					return this.jjMoveStringLiteralDfa32_0(active3, 4608L);
				case 's':
					if ((active3 & 64L) != 0L) {
						return this.jjStartNfaWithStates_0(31, 198, 26);
					}

					return this.jjMoveStringLiteralDfa32_0(active3, 1024L);
				case 't':
					return this.jjMoveStringLiteralDfa32_0(active3, 65536L);
				case 'u':
					return this.jjMoveStringLiteralDfa32_0(active3, 16384L);
				default:
					return this.jjStartNfa_0(30, 0L, 0L, 0L, active3);
			}
		}
	}

	private int jjMoveStringLiteralDfa4_0(long old0, long active0, long old1, long active1, long old2, long active2, long old3, long active3) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1) | (active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(2, old0, old1, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(3, active0, active1, active2, active3);
				return 4;
			}

			switch (this.curChar) {
				case 'A':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 4328525826L, active2, 0L, active3, 0L);
				case 'B':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 1152921504606846976L, active2, 0L, active3, 0L);
				case 'C':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 144115188075855872L, active2, 0L, active3, 0L);
				case 'D':
				case 'H':
				case 'J':
				case 'K':
				case 'L':
				case 'P':
				case 'Q':
				case 'T':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'b':
				case 'j':
				case 'q':
				case 'x':
				default:
					return this.jjStartNfa_0(3, active0, active1, active2, active3);
				case 'E':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 1073741824L, active2, 2164260864L, active3, 0L);
				case 'F':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 2199023255552L, active2, 0L, active3, 0L);
				case 'G':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 1125899906842624L, active2, 0L, active3, 0L);
				case 'I':
					if ((active0 & 2251799813685248L) != 0L) {
						return this.jjStartNfaWithStates_0(4, 51, 26);
					}

					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 0L, active2, 4128L, active3, 0L);
				case 'M':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 72057594037927936L, active2, 0L, active3, 8192L);
				case 'N':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 2147483648L, active2, 4L, active3, 0L);
				case 'O':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 274877906944L, active2, 0L, active3, 0L);
				case 'R':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 0L, active2, 276824128L, active3, 0L);
				case 'S':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 0L, active2, 2097152L, active3, 0L);
				case 'U':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 68719476736L, active2, 0L, active3, 0L);
				case 'V':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 536870912L, active2, 72339069014638592L, active3, 65536L);
				case 'a':
					return this.jjMoveStringLiteralDfa5_0(active0, 2305843009213693952L, active1, 39591293747224L, active2, 144115188075855881L, active3, 0L);
				case 'c':
					return this.jjMoveStringLiteralDfa5_0(active0, 72057594037927936L, active1, 32768L, active2, 33562624L, active3, 0L);
				case 'd':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 17179869184L, active2, 256L, active3, 0L);
				case 'e':
					if ((active0 & 562949953421312L) != 0L) {
						return this.jjStartNfaWithStates_0(4, 49, 26);
					}

					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 576478344691073024L, active2, 1099511660544L, active3, 0L);
				case 'f':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 0L, active2, 16384L, active3, 0L);
				case 'g':
					return this.jjMoveStringLiteralDfa5_0(active0, 4503599627370496L, active1, 2322718313676800L, active2, 70437463654400L, active3, 0L);
				case 'h':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 34359746560L, active2, 0L, active3, 0L);
				case 'i':
					return this.jjMoveStringLiteralDfa5_0(active0, 432345564227567616L, active1, 36028797018963968L, active2, 16L, active3, 0L);
				case 'k':
					return this.jjMoveStringLiteralDfa5_0(active0, 9007199254740992L, active1, 0L, active2, 0L, active3, 0L);
				case 'l':
					if ((active0 & 1125899906842624L) != 0L) {
						return this.jjStartNfaWithStates_0(4, 50, 26);
					}

					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 31526296903221248L, active2, 0L, active3, 0L);
				case 'm':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 128L, active2, 524800L, active3, 0L);
				case 'n':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 2305843009213695492L, active2, 4415226380288L, active3, 0L);
				case 'o':
					return this.jjMoveStringLiteralDfa5_0(active0, Long.MIN_VALUE, active1, 562949955518464L, active2, 134217728L, active3, 391L);
				case 'p':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 4194304L, active2, 131072L, active3, 0L);
				case 'r':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 524608L, active2, 8589934592L, active3, 0L);
				case 's':
					return this.jjMoveStringLiteralDfa5_0(active0, 36028797018963968L, active1, 137440198656L, active2, 1472853549833848960L, active3, 0L);
				case 't':
					if ((active0 & 70368744177664L) != 0L) {
						return this.jjStartNfaWithStates_0(4, 46, 26);
					} else {
						if ((active0 & 140737488355328L) != 0L) {
							return this.jjStartNfaWithStates_0(4, 47, 26);
						}

						return this.jjMoveStringLiteralDfa5_0(active0, 1170935903116328960L, active1, 4611835552017156096L, active2, -1725412983963646974L, active3, 55416L);
					}
				case 'u':
					return this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 288511851128422433L, active2, 0L, active3, 0L);
				case 'v':
					return this.jjMoveStringLiteralDfa5_0(active0, 576460752303423488L, active1, Long.MIN_VALUE, active2, 412316860416L, active3, 1536L);
				case 'w':
					return this.jjMoveStringLiteralDfa5_0(active0, 4611686018427387904L, active1, 0L, active2, 36028797018963968L, active3, 0L);
				case 'y':
					return (active0 & 281474976710656L) != 0L ? this.jjStartNfaWithStates_0(4, 48, 26) : this.jjMoveStringLiteralDfa5_0(active0, 0L, active1, 0L, active2, 4300210176L, active3, 0L);
			}
		}
	}

	private int jjMoveStringLiteralDfa19_0(long old2, long active2, long old3, long active3) {
		if (((active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(17, 0L, 0L, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(18, 0L, 0L, active2, active3);
				return 19;
			}

			switch (this.curChar) {
				case '2':
					return this.jjMoveStringLiteralDfa20_0(active2, 33554432L, active3, 0L);
				case '6':
					if ((active2 & 8192L) != 0L) {
						return this.jjStartNfaWithStates_0(19, 141, 26);
					}
				case '3':
				case '4':
				case '5':
				case '7':
				case '8':
				case '9':
				case ':':
				case ';':
				case '<':
				case '=':
				case '>':
				case '?':
				case '@':
				case 'A':
				case 'B':
				case 'C':
				case 'D':
				case 'G':
				case 'H':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'N':
				case 'Q':
				case 'R':
				case 'S':
				case 'T':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'b':
				case 'f':
				case 'j':
				case 'k':
				case 'm':
				case 'p':
				case 'q':
				case 'v':
				case 'w':
				default:
					return this.jjStartNfa_0(18, 0L, 0L, active2, active3);
				case 'E':
					return this.jjMoveStringLiteralDfa20_0(active2, 4620728402054217728L, active3, 0L);
				case 'F':
					return this.jjMoveStringLiteralDfa20_0(active2, 0L, active3, 1024L);
				case 'I':
					return this.jjMoveStringLiteralDfa20_0(active2, 4503599627370496L, active3, 512L);
				case 'O':
					return this.jjMoveStringLiteralDfa20_0(active2, 549755813888L, active3, 0L);
				case 'P':
					return this.jjMoveStringLiteralDfa20_0(active2, 140737488355328L, active3, 0L);
				case 'U':
					return this.jjMoveStringLiteralDfa20_0(active2, 288230376151711744L, active3, 0L);
				case 'a':
					return this.jjMoveStringLiteralDfa20_0(active2, -9223370933048180736L, active3, 0L);
				case 'c':
					return this.jjMoveStringLiteralDfa20_0(active2, 481036337152L, active3, 0L);
				case 'd':
					return this.jjMoveStringLiteralDfa20_0(active2, 16777216L, active3, 0L);
				case 'e':
					if ((active2 & 65536L) != 0L) {
						return this.jjStartNfaWithStates_0(19, 144, 26);
					}

					return this.jjMoveStringLiteralDfa20_0(active2, 576539917142720512L, active3, 2048L);
				case 'g':
					return this.jjMoveStringLiteralDfa20_0(active2, 142606336L, active3, 7L);
				case 'h':
					return this.jjMoveStringLiteralDfa20_0(active2, 144115188075855872L, active3, 0L);
				case 'i':
					return this.jjMoveStringLiteralDfa20_0(active2, 17592991350784L, active3, 72L);
				case 'l':
					return this.jjMoveStringLiteralDfa20_0(active2, 2219428544512L, active3, 0L);
				case 'n':
					if ((active2 & 32768L) != 0L) {
						return this.jjStartNfaWithStates_0(19, 143, 26);
					} else if ((active2 & 131072L) != 0L) {
						return this.jjStartNfaWithStates_0(19, 145, 26);
					} else {
						if ((active2 & 262144L) != 0L) {
							this.jjmatchedKind = 146;
							this.jjmatchedPos = 19;
						} else if ((active2 & 524288L) != 0L) {
							return this.jjStartNfaWithStates_0(19, 147, 26);
						}

						return this.jjMoveStringLiteralDfa20_0(active2, 18014407099416576L, active3, 16L);
					}
				case 'o':
					return this.jjMoveStringLiteralDfa20_0(active2, 0L, active3, 32L);
				case 'r':
					return this.jjMoveStringLiteralDfa20_0(active2, 112027040730841088L, active3, 114688L);
				case 's':
					if ((active2 & 16384L) != 0L) {
						return this.jjStartNfaWithStates_0(19, 142, 26);
					} else {
						if ((active2 & 1048576L) != 0L) {
							return this.jjStartNfaWithStates_0(19, 148, 26);
						}

						return this.jjMoveStringLiteralDfa20_0(active2, 0L, active3, 8576L);
					}
				case 't':
					return this.jjMoveStringLiteralDfa20_0(active2, 1152921504606846976L, active3, 4096L);
				case 'u':
					return this.jjMoveStringLiteralDfa20_0(active2, 281509403557888L, active3, 0L);
				case 'x':
					return this.jjMoveStringLiteralDfa20_0(active2, 2305847407260205056L, active3, 0L);
			}
		}
	}

	private int jjMoveStringLiteralDfa38_0(long old3, long active3) {
		if ((active3 = active3 & old3) == 0L) {
			return this.jjStartNfa_0(36, 0L, 0L, 0L, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(37, 0L, 0L, 0L, active3);
				return 38;
			}

			switch (this.curChar) {
				case 'a':
					return this.jjMoveStringLiteralDfa39_0(active3, 65536L);
				case 'o':
					return this.jjMoveStringLiteralDfa39_0(active3, 32768L);
				default:
					return this.jjStartNfa_0(37, 0L, 0L, 0L, active3);
			}
		}
	}

	private int jjStartNfaWithStates_0(int pos, int kind, int state) {
		this.jjmatchedKind = kind;
		this.jjmatchedPos = pos;

		try {
			this.curChar = this.input_stream.readChar();
		} catch (IOException ignored) {
			return pos + 1;
		}

		return this.jjMoveNfa_0(state, pos + 1);
	}

	private int jjStartNfaWithStates_4(int pos, int kind, int state) {
		this.jjmatchedKind = kind;
		this.jjmatchedPos = pos;

		try {
			this.curChar = this.input_stream.readChar();
		} catch (IOException ignored) {
			return pos + 1;
		}

		return this.jjMoveNfa_4(state, pos + 1);
	}

	private int jjMoveNfa_1(int startState, int curPos) {
		int startsAt = 0;
		this.jjnewStateCnt = 3;
		int i = 1;
		this.jjstateSet[0] = startState;
		int kind = Integer.MAX_VALUE;

		while (true) {
			if (++this.jjround == Integer.MAX_VALUE) {
				this.ReInitRounds();
			}

			if (this.curChar < '@') {
				long lowAsciiBitMask = 1L << this.curChar;

				do {
					i--;
					switch (this.jjstateSet[i]) {
						case 0:
							if ((13312L & lowAsciiBitMask) != 0L && kind > 9) {
								kind = 9;
							}

							if (this.curChar == '\r') {
								this.jjstateSet[this.jjnewStateCnt++] = 1;
							}
							break;
						case 1:
							if (this.curChar == '\n' && kind > 9) {
								kind = 9;
							}
							break;
						case 2:
							if (this.curChar == '\r') {
								this.jjstateSet[this.jjnewStateCnt++] = 1;
							}
					}
				} while (i != startsAt);
			} else if (this.curChar < 128) {
				long highAsciiBitMask = 1L << (this.curChar & '?');

				do {
					int unusedStateSetEntry = this.jjstateSet[--i];
				} while (i != startsAt);
			} else {
				int highByte = this.curChar >> '\b';
				int bitVecIndex = highByte >> 6;
				long highByteBitMask = 1L << (highByte & 63);
				bitVecIndex = (this.curChar & 255) >> 6;
				long lowByteBitMask = 1L << (this.curChar & '?');

				do {
					int unusedStateSetEntryHighByte = this.jjstateSet[--i];
				} while (i != startsAt);
			}

			if (kind != Integer.MAX_VALUE) {
				this.jjmatchedKind = kind;
				this.jjmatchedPos = curPos;
				kind = Integer.MAX_VALUE;
			}

			curPos++;
			if ((i = this.jjnewStateCnt) == (startsAt = 3 - (this.jjnewStateCnt = startsAt))) {
				return curPos;
			}

			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				return curPos;
			}
		}
	}

	public void MoreLexicalActions() {
		this.jjimageLen = this.jjimageLen + (this.lengthOfMatch = this.jjmatchedPos + 1);
		switch (this.jjmatchedKind) {
			case 7:
				this.image.append(this.input_stream.GetSuffix(this.jjimageLen));
				this.jjimageLen = 0;
				this.input_stream.backup(1);
		}
	}

	private int jjMoveStringLiteralDfa3_0(long old0, long active0, long old1, long active1, long old2, long active2, long old3, long active3) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1) | (active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(1, old0, old1, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(2, active0, active1, active2, active3);
				return 3;
			}

			switch (this.curChar) {
				case '-':
					return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 4096L, active2, 0L, active3, 0L);
				case '.':
				case '/':
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
				case ':':
				case ';':
				case '<':
				case '=':
				case '>':
				case '?':
				case '@':
				case 'A':
				case 'B':
				case 'D':
				case 'E':
				case 'G':
				case 'H':
				case 'J':
				case 'K':
				case 'L':
				case 'O':
				case 'Q':
				case 'R':
				case 'S':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'b':
				case 'f':
				case 'j':
				default:
					return this.jjStartNfa_0(2, active0, active1, active2, active3);
				case 'C':
					return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 27021597764222976L, active2, 0L, active3, 0L);
				case 'F':
					return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 36028797018963968L, active2, 0L, active3, 0L);
				case 'I':
					return this.jjMoveStringLiteralDfa4_0(active0, 2251799813685248L, active1, 0L, active2, 0L, active3, 0L);
				case 'M':
					return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 576460752303423488L, active2, 0L, active3, 0L);
				case 'N':
					return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 4398046511104L, active2, 1L, active3, 0L);
				case 'P':
					return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 0L, active2, 8L, active3, 0L);
				case 'a':
					return this.jjMoveStringLiteralDfa4_0(active0, 1125899906842624L, active1, 4504699139001472L, active2, 512L, active3, 0L);
				case 'c':
					return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 34359738400L, active2, 0L, active3, 0L);
				case 'd':
					return this.jjMoveStringLiteralDfa4_0(active0, -9218868437227405312L, active1, 2097152L, active2, 0L, active3, 0L);
				case 'e':
					if ((active0 & 2199023255552L) != 0L) {
						return this.jjStartNfaWithStates_0(3, 41, 26);
					} else {
						if ((active0 & 8796093022208L) != 0L) {
							return this.jjStartNfaWithStates_0(3, 43, 26);
						}

						return this.jjMoveStringLiteralDfa4_0(active0, 1152921504606846976L, active1, 4755952958469308486L, active2, -1707330346977696510L, active3, 55416L);
					}
				case 'g':
					return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 2305843009213693952L, active2, 2164260864L, active3, 0L);
				case 'h':
					return this.jjMoveStringLiteralDfa4_0(active0, 140737488355328L, active1, 562949953421312L, active2, 134217744L, active3, 391L);
				case 'i':
					return this.jjMoveStringLiteralDfa4_0(active0, 594475150812905472L, active1, 512L, active2, 4415226380288L, active3, 1536L);
				case 'k':
					return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 8589934600L, active2, 0L, active3, 0L);
				case 'l':
					return this.jjMoveStringLiteralDfa4_0(active0, 144115188075855872L, active1, 134217729L, active2, 144115188075855872L, active3, 0L);
				case 'm':
					if ((active0 & 1099511627776L) != 0L) {
						return this.jjStartNfaWithStates_0(3, 40, 26);
					}

					if ((active0 & 35184372088832L) != 0L) {
						this.jjmatchedKind = 45;
						this.jjmatchedPos = 3;
					}

					return this.jjMoveStringLiteralDfa4_0(active0, 2305843009213693952L, active1, 69793218560L, active2, 0L, active3, 0L);
				case 'n':
					if ((active0 & 549755813888L) != 0L) {
						this.jjmatchedKind = 39;
						this.jjmatchedPos = 3;
					}

					return this.jjMoveStringLiteralDfa4_0(active0, 9077567998918656L, active1, 2322722609692672L, active2, 4L, active3, 0L);
				case 'o':
					return this.jjMoveStringLiteralDfa4_0(active0, 4611686018427387904L, active1, -9223372036846387200L, active2, 36029218202583104L, active3, 0L);
				case 'p':
					if ((active0 & 4398046511104L) != 0L) {
						this.jjmatchedKind = 42;
						this.jjmatchedPos = 3;
					}

					return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 1154064997236604928L, active2, 72339069014642720L, active3, 73728L);
				case 'q':
					return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 288511851128422400L, active2, 0L, active3, 0L);
				case 'r':
					return this.jjMoveStringLiteralDfa4_0(active0, 108086391056891904L, active1, 35184439197696L, active2, 1103811837952L, active3, 0L);
				case 's':
					if ((active0 & 17592186044416L) != 0L) {
						return this.jjStartNfaWithStates_0(3, 44, 26);
					}

					return this.jjMoveStringLiteralDfa4_0(active0, 562949953421312L, active1, 65536L, active2, 2748779102208L, active3, 0L);
				case 't':
					return this.jjMoveStringLiteralDfa4_0(active0, 288230376151711744L, active1, 72057594323411200L, active2, 0L, active3, 0L);
				case 'u':
					return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 137443278848L, active2, 1454838601568815232L, active3, 0L);
				case 'v':
					return this.jjMoveStringLiteralDfa4_0(active0, 281474976710656L, active1, 16400L, active2, 0L, active3, 0L);
				case 'w':
					return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 274877906944L, active2, 0L, active3, 0L);
				case 'x':
					return this.jjMoveStringLiteralDfa4_0(active0, 0L, active1, 32768L, active2, 0L, active3, 0L);
			}
		}
	}

	private void ReInitRounds() {
		this.jjround = -2147483647;
		int i = 25;

		while (i-- > 0) {
			this.jjrounds[i] = Integer.MIN_VALUE;
		}
	}

	private int jjMoveNfa_0(int startState, int curPos) {
		int startsAt = 0;
		this.jjnewStateCnt = 25;
		int i = 1;
		this.jjstateSet[0] = startState;
		int kind = Integer.MAX_VALUE;

		while (true) {
			if (++this.jjround == Integer.MAX_VALUE) {
				this.ReInitRounds();
			}

			if (this.curChar < '@') {
				long lowAsciiBitMask = 1L << this.curChar;

				do {
					i--;
					switch (this.jjstateSet[i]) {
						case 0:
							if (this.curChar == '*') {
								this.jjstateSet[this.jjnewStateCnt++] = 1;
							}
							break;
						case 1:
							if ((-140737488355329L & lowAsciiBitMask) != 0L && kind > 7) {
								kind = 7;
							}
							break;
						case 2:
							if (this.curChar == '*') {
								this.jjstateSet[this.jjnewStateCnt++] = 0;
							}
							break;
						case 3:
							if ((287997348674519551L & lowAsciiBitMask) != 0L) {
								if (kind > 215) {
									kind = 215;
								}

								this.jjCheckNAdd(7);
							} else if (this.curChar == '<') {
								this.jjstateSet[this.jjnewStateCnt++] = 13;
							} else if (this.curChar == '/') {
								this.jjstateSet[this.jjnewStateCnt++] = 2;
							}

							if ((287962164302430719L & lowAsciiBitMask) != 0L) {
								if (kind > 214) {
									kind = 214;
								}

								this.jjCheckNAdd(5);
							}

							if ((287957766255919615L & lowAsciiBitMask) != 0L) {
								if (kind > 213) {
									kind = 213;
								}

								this.jjCheckNAddStates(0, 4);
							}

							if ((287948901175001088L & lowAsciiBitMask) != 0L) {
								if (kind > 212) {
									kind = 212;
								}

								this.jjCheckNAdd(4);
							}
							break;
						case 4:
							if ((287948901175001088L & lowAsciiBitMask) != 0L) {
								if (kind > 212) {
									kind = 212;
								}

								this.jjCheckNAdd(4);
							}
							break;
						case 5:
							if ((287962164302430719L & lowAsciiBitMask) != 0L) {
								if (kind > 214) {
									kind = 214;
								}

								this.jjCheckNAdd(5);
							}
							break;
						case 6:
							if ((287997348674519551L & lowAsciiBitMask) != 0L) {
								if (kind > 215) {
									kind = 215;
								}

								this.jjCheckNAdd(7);
							}
							break;
						case 7:
							if ((576227724826231295L & lowAsciiBitMask) != 0L) {
								if (kind > 215) {
									kind = 215;
								}

								this.jjCheckNAdd(7);
							}
							break;
						case 8:
							if (this.curChar == '>') {
								this.jjCheckNAdd(9);
							}
							break;
						case 9:
							if ((287957766255919615L & lowAsciiBitMask) != 0L) {
								if (kind > 216) {
									kind = 216;
								}

								this.jjCheckNAdd(9);
							}
						case 10:
						case 11:
						case 12:
						case 13:
						case 20:
						case 21:
						case 22:
						case 23:
						default:
							break;
						case 14:
							if (this.curChar == '<') {
								this.jjstateSet[this.jjnewStateCnt++] = 13;
							}
							break;
						case 15:
							if ((287957766255919615L & lowAsciiBitMask) != 0L) {
								if (kind > 213) {
									kind = 213;
								}

								this.jjCheckNAddStates(0, 4);
							}
							break;
						case 16:
							if ((287957766255919615L & lowAsciiBitMask) != 0L) {
								if (kind > 213) {
									kind = 213;
								}

								this.jjCheckNAdd(16);
							}
							break;
						case 17:
							if ((287957766255919615L & lowAsciiBitMask) != 0L) {
								this.jjCheckNAddTwoStates(17, 14);
							}
							break;
						case 18:
							if ((287957766255919615L & lowAsciiBitMask) != 0L) {
								this.jjCheckNAddTwoStates(18, 24);
							}
							break;
						case 19:
							if (this.curChar == '>' && kind > 217) {
								kind = 217;
							}
							break;
						case 24:
							if (this.curChar == '<') {
								this.jjstateSet[this.jjnewStateCnt++] = 23;
							}
							break;
						case 25:
							if ((576227724826231295L & lowAsciiBitMask) != 0L) {
								if (kind > 215) {
									kind = 215;
								}

								this.jjCheckNAdd(7);
							}

							if ((287962164302430719L & lowAsciiBitMask) != 0L) {
								if (kind > 214) {
									kind = 214;
								}

								this.jjCheckNAdd(5);
							}
							break;
						case 26:
							if ((576227724826231295L & lowAsciiBitMask) != 0L) {
								if (kind > 215) {
									kind = 215;
								}

								this.jjCheckNAdd(7);
							} else if (this.curChar == '<') {
								this.jjstateSet[this.jjnewStateCnt++] = 23;
							}

							if ((287962164302430719L & lowAsciiBitMask) != 0L) {
								if (kind > 214) {
									kind = 214;
								}

								this.jjCheckNAdd(5);
							} else if (this.curChar == '<') {
								this.jjstateSet[this.jjnewStateCnt++] = 13;
							}

							if ((287957766255919615L & lowAsciiBitMask) != 0L) {
								this.jjCheckNAddTwoStates(18, 24);
							}

							if ((287957766255919615L & lowAsciiBitMask) != 0L) {
								this.jjCheckNAddTwoStates(17, 14);
							}

							if ((287957766255919615L & lowAsciiBitMask) != 0L) {
								if (kind > 213) {
									kind = 213;
								}

								this.jjCheckNAdd(16);
							}
					}
				} while (i != startsAt);
			} else if (this.curChar < 128) {
				long highAsciiBitMask = 1L << (this.curChar & '?');

				do {
					i--;
					switch (this.jjstateSet[i]) {
						case 1:
							if (kind > 7) {
								kind = 7;
							}
						case 2:
						case 4:
						case 8:
						case 14:
						case 19:
						case 24:
						default:
							break;
						case 3:
							if ((-8646911290859585538L & highAsciiBitMask) != 0L) {
								if (kind > 213) {
									kind = 213;
								}

								this.jjCheckNAddStates(0, 4);
							}

							if ((-8646911290859585538L & highAsciiBitMask) != 0L) {
								if (kind > 215) {
									kind = 215;
								}

								this.jjCheckNAdd(7);
							}

							if ((-8646911290859585538L & highAsciiBitMask) != 0L) {
								if (kind > 214) {
									kind = 214;
								}

								this.jjCheckNAdd(5);
							}
							break;
						case 5:
							if ((-8646911290859585538L & highAsciiBitMask) != 0L) {
								if (kind > 214) {
									kind = 214;
								}

								this.jjCheckNAdd(5);
							}
							break;
						case 6:
							if ((-8646911290859585538L & highAsciiBitMask) != 0L) {
								if (kind > 215) {
									kind = 215;
								}

								this.jjCheckNAdd(7);
							}
							break;
						case 7:
							if ((-8646911290859585537L & highAsciiBitMask) != 0L) {
								if (kind > 215) {
									kind = 215;
								}

								this.jjCheckNAdd(7);
							}
							break;
						case 9:
							if ((-8646911290859585538L & highAsciiBitMask) != 0L) {
								if (kind > 216) {
									kind = 216;
								}

								this.jjstateSet[this.jjnewStateCnt++] = 9;
							}
							break;
						case 10:
							if (this.curChar == 'k') {
								this.jjstateSet[this.jjnewStateCnt++] = 8;
							}
							break;
						case 11:
							if (this.curChar == 'n') {
								this.jjstateSet[this.jjnewStateCnt++] = 10;
							}
							break;
						case 12:
							if (this.curChar == 'i') {
								this.jjstateSet[this.jjnewStateCnt++] = 11;
							}
							break;
						case 13:
							if (this.curChar == 'l') {
								this.jjstateSet[this.jjnewStateCnt++] = 12;
							}
							break;
						case 15:
							if ((-8646911290859585538L & highAsciiBitMask) != 0L) {
								if (kind > 213) {
									kind = 213;
								}

								this.jjCheckNAddStates(0, 4);
							}
							break;
						case 16:
							if ((-8646911290859585538L & highAsciiBitMask) != 0L) {
								if (kind > 213) {
									kind = 213;
								}

								this.jjCheckNAdd(16);
							}
							break;
						case 17:
							if ((-8646911290859585538L & highAsciiBitMask) != 0L) {
								this.jjCheckNAddTwoStates(17, 14);
							}
							break;
						case 18:
							if ((-8646911290859585538L & highAsciiBitMask) != 0L) {
								this.jjCheckNAddTwoStates(18, 24);
							}
							break;
						case 20:
							if (this.curChar == 'k') {
								this.jjstateSet[this.jjnewStateCnt++] = 19;
							}
							break;
						case 21:
							if (this.curChar == 'n') {
								this.jjstateSet[this.jjnewStateCnt++] = 20;
							}
							break;
						case 22:
							if (this.curChar == 'i') {
								this.jjstateSet[this.jjnewStateCnt++] = 21;
							}
							break;
						case 23:
							if (this.curChar == 'l') {
								this.jjstateSet[this.jjnewStateCnt++] = 22;
							}
							break;
						case 25:
							if ((-8646911290859585537L & highAsciiBitMask) != 0L) {
								if (kind > 215) {
									kind = 215;
								}

								this.jjCheckNAdd(7);
							}

							if ((-8646911290859585538L & highAsciiBitMask) != 0L) {
								if (kind > 214) {
									kind = 214;
								}

								this.jjCheckNAdd(5);
							}
							break;
						case 26:
							if ((-8646911290859585537L & highAsciiBitMask) != 0L) {
								if (kind > 215) {
									kind = 215;
								}

								this.jjCheckNAdd(7);
							}

							if ((-8646911290859585538L & highAsciiBitMask) != 0L) {
								this.jjCheckNAddTwoStates(18, 24);
							}

							if ((-8646911290859585538L & highAsciiBitMask) != 0L) {
								this.jjCheckNAddTwoStates(17, 14);
							}

							if ((-8646911290859585538L & highAsciiBitMask) != 0L) {
								if (kind > 214) {
									kind = 214;
								}

								this.jjCheckNAdd(5);
							}

							if ((-8646911290859585538L & highAsciiBitMask) != 0L) {
								if (kind > 213) {
									kind = 213;
								}

								this.jjCheckNAdd(16);
							}
					}
				} while (i != startsAt);
			} else {
				int highByte = this.curChar >> '\b';
				int highBitVecIndex = highByte >> 6;
				long highByteBitMask = 1L << (highByte & 63);
				int lowBitVecIndex = (this.curChar & 255) >> 6;
				long lowByteBitMask = 1L << (this.curChar & '?');

				do {
					i--;
					switch (this.jjstateSet[i]) {
						case 1:
							if (jjCanMove_0(highByte, highBitVecIndex, lowBitVecIndex, highByteBitMask, lowByteBitMask) && kind > 7) {
								kind = 7;
							}
						case 2:
						case 4:
						case 8:
						case 10:
						case 11:
						case 12:
						case 13:
						case 14:
						case 19:
						case 20:
						case 21:
						case 22:
						case 23:
						case 24:
						default:
							break;
						case 3:
							if (jjCanMove_1(highByte, highBitVecIndex, lowBitVecIndex, highByteBitMask, lowByteBitMask)) {
								if (kind > 214) {
									kind = 214;
								}

								this.jjCheckNAdd(5);
							}

							if (jjCanMove_1(highByte, highBitVecIndex, lowBitVecIndex, highByteBitMask, lowByteBitMask)) {
								if (kind > 215) {
									kind = 215;
								}

								this.jjCheckNAdd(7);
							}

							if (jjCanMove_1(highByte, highBitVecIndex, lowBitVecIndex, highByteBitMask, lowByteBitMask)) {
								if (kind > 213) {
									kind = 213;
								}

								this.jjCheckNAddStates(0, 4);
							}
							break;
						case 5:
							if (jjCanMove_1(highByte, highBitVecIndex, lowBitVecIndex, highByteBitMask, lowByteBitMask)) {
								if (kind > 214) {
									kind = 214;
								}

								this.jjCheckNAdd(5);
							}
							break;
						case 6:
							if (jjCanMove_1(highByte, highBitVecIndex, lowBitVecIndex, highByteBitMask, lowByteBitMask)) {
								if (kind > 215) {
									kind = 215;
								}

								this.jjCheckNAdd(7);
							}
							break;
						case 7:
							if (jjCanMove_1(highByte, highBitVecIndex, lowBitVecIndex, highByteBitMask, lowByteBitMask)) {
								if (kind > 215) {
									kind = 215;
								}

								this.jjCheckNAdd(7);
							}
							break;
						case 9:
							if (jjCanMove_1(highByte, highBitVecIndex, lowBitVecIndex, highByteBitMask, lowByteBitMask)) {
								if (kind > 216) {
									kind = 216;
								}

								this.jjstateSet[this.jjnewStateCnt++] = 9;
							}
							break;
						case 15:
							if (jjCanMove_1(highByte, highBitVecIndex, lowBitVecIndex, highByteBitMask, lowByteBitMask)) {
								if (kind > 213) {
									kind = 213;
								}

								this.jjCheckNAddStates(0, 4);
							}
							break;
						case 16:
							if (jjCanMove_1(highByte, highBitVecIndex, lowBitVecIndex, highByteBitMask, lowByteBitMask)) {
								if (kind > 213) {
									kind = 213;
								}

								this.jjCheckNAdd(16);
							}
							break;
						case 17:
							if (jjCanMove_1(highByte, highBitVecIndex, lowBitVecIndex, highByteBitMask, lowByteBitMask)) {
								this.jjCheckNAddTwoStates(17, 14);
							}
							break;
						case 18:
							if (jjCanMove_1(highByte, highBitVecIndex, lowBitVecIndex, highByteBitMask, lowByteBitMask)) {
								this.jjCheckNAddTwoStates(18, 24);
							}
							break;
						case 25:
							if (jjCanMove_1(highByte, highBitVecIndex, lowBitVecIndex, highByteBitMask, lowByteBitMask)) {
								if (kind > 214) {
									kind = 214;
								}

								this.jjCheckNAdd(5);
							}

							if (jjCanMove_1(highByte, highBitVecIndex, lowBitVecIndex, highByteBitMask, lowByteBitMask)) {
								if (kind > 215) {
									kind = 215;
								}

								this.jjCheckNAdd(7);
							}
							break;
						case 26:
							if (jjCanMove_1(highByte, highBitVecIndex, lowBitVecIndex, highByteBitMask, lowByteBitMask)) {
								if (kind > 213) {
									kind = 213;
								}

								this.jjCheckNAdd(16);
							}

							if (jjCanMove_1(highByte, highBitVecIndex, lowBitVecIndex, highByteBitMask, lowByteBitMask)) {
								if (kind > 214) {
									kind = 214;
								}

								this.jjCheckNAdd(5);
							}

							if (jjCanMove_1(highByte, highBitVecIndex, lowBitVecIndex, highByteBitMask, lowByteBitMask)) {
								if (kind > 215) {
									kind = 215;
								}

								this.jjCheckNAdd(7);
							}

							if (jjCanMove_1(highByte, highBitVecIndex, lowBitVecIndex, highByteBitMask, lowByteBitMask)) {
								this.jjCheckNAddTwoStates(17, 14);
							}

							if (jjCanMove_1(highByte, highBitVecIndex, lowBitVecIndex, highByteBitMask, lowByteBitMask)) {
								this.jjCheckNAddTwoStates(18, 24);
							}
					}
				} while (i != startsAt);
			}

			if (kind != Integer.MAX_VALUE) {
				this.jjmatchedKind = kind;
				this.jjmatchedPos = curPos;
				kind = Integer.MAX_VALUE;
			}

			curPos++;
			if ((i = this.jjnewStateCnt) == (startsAt = 25 - (this.jjnewStateCnt = startsAt))) {
				return curPos;
			}

			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				return curPos;
			}
		}
	}

	private int jjMoveStringLiteralDfa7_0(long old1, long active1, long old2, long active2, long old3, long active3) {
		if (((active1 = active1 & old1) | (active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(5, 0L, old1, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(6, 0L, active1, active2, active3);
				return 7;
			}

			switch (this.curChar) {
				case '-':
					return this.jjMoveStringLiteralDfa8_0(active1, 8589934592L, active2, 0L, active3, 0L);
				case '>':
					if ((active1 & 512L) != 0L) {
						return this.jjStopAtPos(7, 73);
					}
				case '.':
				case '/':
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9':
				case ':':
				case ';':
				case '<':
				case '=':
				case '?':
				case '@':
				case 'A':
				case 'B':
				case 'D':
				case 'F':
				case 'G':
				case 'H':
				case 'J':
				case 'K':
				case 'N':
				case 'P':
				case 'Q':
				case 'R':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'j':
				case 'k':
				case 'p':
				case 'q':
				case 'v':
				case 'w':
				case 'y':
				default:
					return this.jjStartNfa_0(6, 0L, active1, active2, active3);
				case 'C':
					return this.jjMoveStringLiteralDfa8_0(active1, 0L, active2, 17L, active3, 0L);
				case 'E':
					return this.jjMoveStringLiteralDfa8_0(active1, 0L, active2, 70437463654400L, active3, 0L);
				case 'I':
					return this.jjMoveStringLiteralDfa8_0(active1, 4096L, active2, 4294967296L, active3, 0L);
				case 'L':
					return this.jjMoveStringLiteralDfa8_0(active1, 0L, active2, 1048576L, active3, 0L);
				case 'M':
					return this.jjMoveStringLiteralDfa8_0(active1, 0L, active2, 2L, active3, 0L);
				case 'O':
					return this.jjMoveStringLiteralDfa8_0(active1, 33554432L, active2, 0L, active3, 0L);
				case 'S':
					return this.jjMoveStringLiteralDfa8_0(active1, 0L, active2, 4194304L, active3, 0L);
				case 'a':
					return this.jjMoveStringLiteralDfa8_0(active1, 1153502063926181888L, active2, -9223372036720525304L, active3, 391L);
				case 'b':
					return this.jjMoveStringLiteralDfa8_0(active1, 2147483648L, active2, 0L, active3, 0L);
				case 'c':
					return this.jjMoveStringLiteralDfa8_0(active1, 140737488879616L, active2, 8796093022208L, active3, 0L);
				case 'd':
					if ((active1 & 1024L) != 0L) {
						return this.jjStartNfaWithStates_0(7, 74, 26);
					}

					return this.jjMoveStringLiteralDfa8_0(active1, 36028797018996736L, active2, 18014398543306752L, active3, 0L);
				case 'e':
					if ((active1 & 64L) != 0L) {
						return this.jjStartNfaWithStates_0(7, 70, 26);
					} else if ((active1 & 128L) != 0L) {
						return this.jjStartNfaWithStates_0(7, 71, 26);
					} else {
						if ((active1 & 2048L) != 0L) {
							return this.jjStartNfaWithStates_0(7, 75, 26);
						}

						return this.jjMoveStringLiteralDfa8_0(active1, -8929512160900153344L, active2, 146367400206417952L, active3, 16L);
					}
				case 'f':
					return this.jjMoveStringLiteralDfa8_0(active1, 16384L, active2, 2305843009213693952L, active3, 6144L);
				case 'g':
					return this.jjMoveStringLiteralDfa8_0(active1, 35184376283136L, active2, 2199023255552L, active3, 0L);
				case 'h':
					return this.jjMoveStringLiteralDfa8_0(active1, 4294967296L, active2, 0L, active3, 8192L);
				case 'i':
					return this.jjMoveStringLiteralDfa8_0(active1, 72057594667081728L, active2, 72340726872015872L, active3, 65544L);
				case 'l':
					return this.jjMoveStringLiteralDfa8_0(active1, 283675073708032L, active2, 276824128L, active3, 0L);
				case 'm':
					return this.jjMoveStringLiteralDfa8_0(active1, 0L, active2, 1610612736L, active3, 0L);
				case 'n':
					return this.jjMoveStringLiteralDfa8_0(active1, 34495004672L, active2, 5188714170270351360L, active3, 0L);
				case 'o':
					return this.jjMoveStringLiteralDfa8_0(active1, 578792266710122496L, active2, 1125899906978304L, active3, 0L);
				case 'r':
					return this.jjMoveStringLiteralDfa8_0(active1, 1099511627776L, active2, 2164260864L, active3, 544L);
				case 's':
					return this.jjMoveStringLiteralDfa8_0(active1, 171141183886589952L, active2, 256L, active3, 0L);
				case 't':
					if ((active1 & 256L) != 0L) {
						return this.jjStartNfaWithStates_0(7, 72, 26);
					}

					return this.jjMoveStringLiteralDfa8_0(active1, 137439150080L, active2, 1490867398589352068L, active3, 49216L);
				case 'u':
					return this.jjMoveStringLiteralDfa8_0(active1, 2305843284091600896L, active2, 524288L, active3, 1024L);
				case 'x':
					return this.jjMoveStringLiteralDfa8_0(active1, 4611686087146864640L, active2, 17592186044416L, active3, 0L);
				case 'z':
					return this.jjMoveStringLiteralDfa8_0(active1, 2097152L, active2, 0L, active3, 0L);
			}
		}
	}

	private int jjMoveStringLiteralDfa33_0(long old3, long active3) {
		if ((active3 = active3 & old3) == 0L) {
			return this.jjStartNfa_0(31, 0L, 0L, 0L, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(32, 0L, 0L, 0L, active3);
				return 33;
			}

			switch (this.curChar) {
				case 'b':
					return this.jjMoveStringLiteralDfa34_0(active3, 65536L);
				case 'c':
					return this.jjMoveStringLiteralDfa34_0(active3, 32768L);
				case 'e':
					return this.jjMoveStringLiteralDfa34_0(active3, 8192L);
				case 'i':
					return this.jjMoveStringLiteralDfa34_0(active3, 16384L);
				case 's':
					if ((active3 & 4096L) != 0L) {
						return this.jjStartNfaWithStates_0(33, 204, 26);
					}
				default:
					return this.jjStartNfa_0(32, 0L, 0L, 0L, active3);
			}
		}
	}

	private int jjMoveStringLiteralDfa34_0(long old3, long active3) {
		if ((active3 = active3 & old3) == 0L) {
			return this.jjStartNfa_0(32, 0L, 0L, 0L, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(33, 0L, 0L, 0L, active3);
				return 34;
			}

			switch (this.curChar) {
				case 'd':
					if ((active3 & 8192L) != 0L) {
						return this.jjStartNfaWithStates_0(34, 205, 26);
					}
				default:
					return this.jjStartNfa_0(33, 0L, 0L, 0L, active3);
				case 'f':
					return this.jjMoveStringLiteralDfa35_0(active3, 65536L);
				case 'l':
					return this.jjMoveStringLiteralDfa35_0(active3, 32768L);
				case 'o':
					return this.jjMoveStringLiteralDfa35_0(active3, 16384L);
			}
		}
	}

	private int jjMoveStringLiteralDfa13_0(long old1, long active1, long old2, long active2, long old3, long active3) {
		if (((active1 = active1 & old1) | (active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(11, 0L, old1, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(12, 0L, active1, active2, active3);
				return 13;
			}

			switch (this.curChar) {
				case 'A':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 34359738368L, active3, 0L);
				case 'B':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 8796093022208L, active3, 0L);
				case 'C':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 16384L, active3, 0L);
				case 'E':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 65536L, active3, 0L);
				case 'F':
					return this.jjMoveStringLiteralDfa14_0(active1, 576460752303423488L, active2, 0L, active3, 0L);
				case 'I':
					return this.jjMoveStringLiteralDfa14_0(active1, 70368744177664L, active2, 0L, active3, 0L);
				case 'L':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 4194304L, active3, 0L);
				case 'N':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 281474976710656L, active3, 0L);
				case 'O':
					return this.jjMoveStringLiteralDfa14_0(active1, 2251799813685248L, active2, 0L, active3, 0L);
				case 'R':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 8589934592L, active3, 0L);
				case 'S':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 0L, active3, 8L);
				case 'U':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 67108864L, active3, 0L);
				case 'V':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 524288L, active3, 0L);
				case '_':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 8192L, active3, 0L);
				case 'a':
					return this.jjMoveStringLiteralDfa14_0(active1, -8935141660703064064L, active2, 412350414848L, active3, 8192L);
				case 'b':
					return this.jjMoveStringLiteralDfa14_0(active1, 144115188075855872L, active2, 0L, active3, 0L);
				case 'c':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 0L, active3, 32L);
				case 'e':
					return this.jjMoveStringLiteralDfa14_0(active1, 5066549580791808L, active2, 2305843009347911690L, active3, 6535L);
				case 'g':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 144115188075888640L, active3, 512L);
				case 'h':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 1099513724932L, active3, 0L);
				case 'i':
					return this.jjMoveStringLiteralDfa14_0(active1, 126100789566373888L, active2, 1693265086644224L, active3, 0L);
				case 'l':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 18016597532999680L, active3, 0L);
				case 'm':
					return this.jjMoveStringLiteralDfa14_0(active1, 281474976710656L, active2, 128L, active3, 0L);
				case 'n':
					return this.jjMoveStringLiteralDfa14_0(active1, 10273836649938944L, active2, 2442133568L, active3, 64L);
				case 'o':
					return this.jjMoveStringLiteralDfa14_0(active1, 1152921504606846976L, active2, 17592186044416L, active3, 0L);
				case 'p':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 288265560523800576L, active3, 0L);
				case 'r':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 1202601842828118016L, active3, 49152L);
				case 's':
					if ((active1 & 1099511627776L) != 0L) {
						return this.jjStartNfaWithStates_0(13, 104, 26);
					} else if ((active1 & 2199023255552L) != 0L) {
						return this.jjStartNfaWithStates_0(13, 105, 26);
					} else if ((active1 & 8796093022208L) != 0L) {
						return this.jjStartNfaWithStates_0(13, 107, 26);
					} else {
						if ((active1 & 17592186044416L) != 0L) {
							return this.jjStartNfaWithStates_0(13, 108, 26);
						}

						return this.jjMoveStringLiteralDfa14_0(active1, 6917529027641081856L, active2, 576460752303559472L, active3, 1024L);
					}
				case 't':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, -9148992204465766399L, active3, 65552L);
				case 'u':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 1024L, active3, 0L);
				case 'v':
					return this.jjMoveStringLiteralDfa14_0(active1, 35184372088832L, active2, 0L, active3, 0L);
				case 'x':
					if ((active1 & 4398046511104L) != 0L) {
						return this.jjStartNfaWithStates_0(13, 106, 26);
					}
				case 'D':
				case 'G':
				case 'H':
				case 'J':
				case 'K':
				case 'M':
				case 'P':
				case 'Q':
				case 'T':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '`':
				case 'd':
				case 'f':
				case 'j':
				case 'k':
				case 'q':
				case 'w':
				default:
					return this.jjStartNfa_0(12, 0L, active1, active2, active3);
				case 'y':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 4611686018427387904L, active3, 0L);
				case 'z':
					return this.jjMoveStringLiteralDfa14_0(active1, 0L, active2, 549755813888L, active3, 0L);
			}
		}
	}

	private int jjMoveStringLiteralDfa6_0(long old0, long active0, long old1, long active1, long old2, long active2, long old3, long active3) {
		long maskedActive0 = active0 & old0;
		long duplicatedMaskedActive0 = active0 & old0;
		if ((maskedActive0 | (active1 = active1 & old1) | (active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(4, old0, old1, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(5, 0L, active1, active2, active3);
				return 6;
			}

			switch (this.curChar) {
				case 'A':
					return this.jjMoveStringLiteralDfa7_0(active1, 0L, active2, 562949953421312L, active3, 0L);
				case 'B':
				case 'F':
				case 'H':
				case 'I':
				case 'J':
				case 'K':
				case 'O':
				case 'Q':
				case 'T':
				case 'V':
				case 'W':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'j':
				case 'p':
				case 'q':
				case 'v':
				case 'w':
				default:
					return this.jjStartNfa_0(5, 0L, active1, active2, active3);
				case 'C':
					return this.jjMoveStringLiteralDfa7_0(active1, 281474976714752L, active2, 0L, active3, 0L);
				case 'D':
					return this.jjMoveStringLiteralDfa7_0(active1, 0L, active2, 2251799813685248L, active3, 16L);
				case 'E':
					return this.jjMoveStringLiteralDfa7_0(active1, 0L, active2, 22007949295616L, active3, 0L);
				case 'G':
					return this.jjMoveStringLiteralDfa7_0(active1, 0L, active2, 0L, active3, 512L);
				case 'L':
					return this.jjMoveStringLiteralDfa7_0(active1, 2322718313676800L, active2, 0L, active3, 0L);
				case 'M':
					return this.jjMoveStringLiteralDfa7_0(active1, -8935141660703064064L, active2, 420906795008L, active3, 0L);
				case 'N':
					return this.jjMoveStringLiteralDfa7_0(active1, 0L, active2, 4608L, active3, 0L);
				case 'P':
					return this.jjMoveStringLiteralDfa7_0(active1, 562949953421312L, active2, 134217728L, active3, 391L);
				case 'R':
					return this.jjMoveStringLiteralDfa7_0(active1, 0L, active2, 524288L, active3, 0L);
				case 'S':
					return this.jjMoveStringLiteralDfa7_0(active1, 0L, active2, 1125899906842624L, active3, 1024L);
				case 'U':
					return this.jjMoveStringLiteralDfa7_0(active1, 0L, active2, 34359738368L, active3, 0L);
				case 'X':
					return this.jjMoveStringLiteralDfa7_0(active1, 0L, active2, 2L, active3, 0L);
				case 'a':
					return this.jjMoveStringLiteralDfa7_0(active1, 144116425027158016L, active2, 1454838601570388352L, active3, 0L);
				case 'b':
					return this.jjMoveStringLiteralDfa7_0(active1, 0L, active2, 2305843009213693952L, active3, 6144L);
				case 'c':
					return this.jjMoveStringLiteralDfa7_0(active1, 5368709376L, active2, 8798257283072L, active3, 0L);
				case 'd':
					return this.jjMoveStringLiteralDfa7_0(active1, 76561193665298432L, active2, 0L, active3, 0L);
				case 'e':
					if ((active1 & 1L) != 0L) {
						return this.jjStartNfaWithStates_0(6, 64, 26);
					} else {
						if ((active1 & 8L) != 0L) {
							this.jjmatchedKind = 67;
							this.jjmatchedPos = 6;
						} else {
							if ((active1 & 16L) != 0L) {
								return this.jjStartNfaWithStates_0(6, 68, 26);
							}

							if ((active1 & 32L) != 0L) {
								return this.jjStartNfaWithStates_0(6, 69, 26);
							}
						}

						return this.jjMoveStringLiteralDfa7_0(active1, 6674514445312L, active2, 54043196636012561L, active3, 49152L);
					}
				case 'f':
					return this.jjMoveStringLiteralDfa7_0(active1, 274877906944L, active2, 276824128L, active3, 0L);
				case 'g':
					return this.jjMoveStringLiteralDfa7_0(active1, 35184372088832L, active2, 576460752303423488L, active3, 0L);
				case 'h':
					return this.jjMoveStringLiteralDfa7_0(active1, 576460752303423488L, active2, 32768L, active3, 0L);
				case 'i':
					return this.jjMoveStringLiteralDfa7_0(active1, 4611703610615545856L, active2, 131072L, active3, 0L);
				case 'k':
					return this.jjMoveStringLiteralDfa7_0(active1, 0L, active2, 8L, active3, 0L);
				case 'l':
					if ((active1 & 2L) != 0L) {
						this.jjmatchedKind = 65;
						this.jjmatchedPos = 6;
					}

					return this.jjMoveStringLiteralDfa7_0(active1, 1188950318839236736L, active2, Long.MIN_VALUE, active3, 0L);
				case 'm':
					return this.jjMoveStringLiteralDfa7_0(active1, 2147483648L, active2, 0L, active3, 0L);
				case 'n':
					return this.jjMoveStringLiteralDfa7_0(active1, 1125900196249600L, active2, 2748779069472L, active3, 64L);
				case 'o':
					return this.jjMoveStringLiteralDfa7_0(active1, 34359738368L, active2, 4611686018427387904L, active3, 0L);
				case 'r':
					return this.jjMoveStringLiteralDfa7_0(active1, 8796093022208L, active2, 70437463671808L, active3, 0L);
				case 's':
					if ((active1 & 4L) != 0L) {
						return this.jjStartNfaWithStates_0(6, 66, 26);
					}

					return this.jjMoveStringLiteralDfa7_0(active1, 27021598368202752L, active2, 216455356602122244L, active3, 65536L);
				case 't':
					return this.jjMoveStringLiteralDfa7_0(active1, 2305843009222353472L, active2, 4300210176L, active3, 8224L);
				case 'u':
					return this.jjMoveStringLiteralDfa7_0(active1, 32768L, active2, 0L, active3, 0L);
				case 'x':
					return this.jjMoveStringLiteralDfa7_0(active1, 140737488355328L, active2, 0L, active3, 8L);
			}
		}
	}

	private int jjMoveStringLiteralDfa11_0(long old1, long active1, long old2, long active2, long old3, long active3) {
		if (((active1 = active1 & old1) | (active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(9, 0L, old1, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(10, 0L, active1, active2, active3);
				return 11;
			}

			switch (this.curChar) {
				case 'C':
					return this.jjMoveStringLiteralDfa12_0(active1, 0L, active2, 1048576L, active3, 0L);
				case 'D':
					return this.jjMoveStringLiteralDfa12_0(active1, 0L, active2, 536870912L, active3, 0L);
				case 'E':
					return this.jjMoveStringLiteralDfa12_0(active1, 0L, active2, 0L, active3, 48L);
				case 'F':
				case 'G':
				case 'H':
				case 'J':
				case 'K':
				case 'O':
				case 'Q':
				case 'R':
				case 'S':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'b':
				case 'h':
				case 'j':
				case 'q':
				case 'u':
				case 'v':
				case 'x':
				default:
					return this.jjStartNfa_0(10, 0L, active1, active2, active3);
				case 'I':
					return this.jjMoveStringLiteralDfa12_0(active1, 0L, active2, 281474976710656L, active3, 0L);
				case 'L':
					return this.jjMoveStringLiteralDfa12_0(active1, 0L, active2, 32768L, active3, 0L);
				case 'M':
					return this.jjMoveStringLiteralDfa12_0(active1, 0L, active2, 648518347415093248L, active3, 65536L);
				case 'N':
					return this.jjMoveStringLiteralDfa12_0(active1, 281474976710656L, active2, 0L, active3, 0L);
				case 'P':
					return this.jjMoveStringLiteralDfa12_0(active1, 144115188075855872L, active2, 36028797018963968L, active3, 49152L);
				case 'a':
					return this.jjMoveStringLiteralDfa12_0(active1, 17592186044416L, active2, 2306405959167123753L, active3, 15360L);
				case 'c':
					return this.jjMoveStringLiteralDfa12_0(active1, 0L, active2, 4902203378764874752L, active3, 0L);
				case 'd':
					if ((active1 & 34359738368L) != 0L) {
						return this.jjStartNfaWithStates_0(11, 99, 26);
					}

					return this.jjMoveStringLiteralDfa12_0(active1, -7710162493338812416L, active2, 412316860416L, active3, 0L);
				case 'e':
					return this.jjMoveStringLiteralDfa12_0(active1, 54606145481867264L, active2, 19150194157551616L, active3, 391L);
				case 'f':
					return this.jjMoveStringLiteralDfa12_0(active1, 4507997673881600L, active2, 1166573040977451520L, active3, 0L);
				case 'g':
					return this.jjMoveStringLiteralDfa12_0(active1, 0L, active2, 4294967296L, active3, 0L);
				case 'i':
					return this.jjMoveStringLiteralDfa12_0(active1, 140737488355328L, active2, 2201464340544L, active3, 512L);
				case 'k':
					return this.jjMoveStringLiteralDfa12_0(active1, 0L, active2, 144115188075855872L, active3, 0L);
				case 'l':
					return this.jjMoveStringLiteralDfa12_0(active1, 6919852845466386432L, active2, 549755813888L, active3, 0L);
				case 'm':
					return this.jjMoveStringLiteralDfa12_0(active1, 576462951326679040L, active2, 524290L, active3, 0L);
				case 'n':
					return this.jjMoveStringLiteralDfa12_0(active1, 8796093022208L, active2, -9223372028260630528L, active3, 8L);
				case 'o':
					if ((active1 & 8589934592L) != 0L) {
						return this.jjStartNfaWithStates_0(11, 97, 7);
					}

					return this.jjMoveStringLiteralDfa12_0(active1, 137438953472L, active2, 67174400L, active3, 0L);
				case 'p':
					return this.jjMoveStringLiteralDfa12_0(active1, 9007199254740992L, active2, 4415226380288L, active3, 0L);
				case 'r':
					return this.jjMoveStringLiteralDfa12_0(active1, 0L, active2, 148L, active3, 64L);
				case 's':
					if ((active1 & 17179869184L) != 0L) {
						return this.jjStartNfaWithStates_0(11, 98, 26);
					}

					return this.jjMoveStringLiteralDfa12_0(active1, 1161084278931456L, active2, 0L, active3, 0L);
				case 't':
					return this.jjMoveStringLiteralDfa12_0(active1, 274877906944L, active2, 17592219598848L, active3, 0L);
				case 'w':
					return this.jjMoveStringLiteralDfa12_0(active1, 0L, active2, 34359738368L, active3, 0L);
				case 'y':
					return this.jjMoveStringLiteralDfa12_0(active1, 0L, active2, 70437463654400L, active3, 0L);
			}
		}
	}

	private int jjMoveStringLiteralDfa12_0(long old1, long active1, long old2, long active2, long old3, long active3) {
		if (((active1 = active1 & old1) | (active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(10, 0L, old1, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(11, 0L, active1, active2, active3);
				return 12;
			}

			switch (this.curChar) {
				case 'C':
					return this.jjMoveStringLiteralDfa13_0(active1, Long.MIN_VALUE, active2, 412316860416L, active3, 0L);
				case 'D':
				case 'G':
				case 'H':
				case 'J':
				case 'K':
				case 'M':
				case 'O':
				case 'P':
				case 'Q':
				case 'R':
				case 'S':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'b':
				case 'h':
				case 'j':
				case 'k':
				case 'q':
				case 'v':
				default:
					return this.jjStartNfa_0(11, 0L, active1, active2, active3);
				case 'E':
					return this.jjMoveStringLiteralDfa13_0(active1, 0L, active2, 0L, active3, 64L);
				case 'F':
					return this.jjMoveStringLiteralDfa13_0(active1, 54043195528445952L, active2, 1125899906842624L, active3, 0L);
				case 'I':
					return this.jjMoveStringLiteralDfa13_0(active1, 1125899906842624L, active2, 0L, active3, 0L);
				case 'L':
					return this.jjMoveStringLiteralDfa13_0(active1, 1152921504606846976L, active2, 0L, active3, 0L);
				case 'N':
					return this.jjMoveStringLiteralDfa13_0(active1, 288230376151711744L, active2, 0L, active3, 0L);
				case 'T':
					return this.jjMoveStringLiteralDfa13_0(active1, 72057594037927936L, active2, 0L, active3, 0L);
				case 'a':
					return this.jjMoveStringLiteralDfa13_0(active1, 6917810502617792512L, active2, 182397983931760768L, active3, 49152L);
				case 'c':
					return this.jjMoveStringLiteralDfa13_0(active1, 0L, active2, 5L, active3, 0L);
				case 'd':
					return this.jjMoveStringLiteralDfa13_0(active1, 0L, active2, 8796093022208L, active3, 0L);
				case 'e':
					if ((active1 & 68719476736L) != 0L) {
						return this.jjStartNfaWithStates_0(12, 100, 26);
					} else {
						if ((active1 & 274877906944L) != 0L) {
							return this.jjStartNfaWithStates_0(12, 102, 26);
						}

						if ((active1 & 549755813888L) != 0L) {
							this.jjmatchedKind = 103;
							this.jjmatchedPos = 12;
						}

						return this.jjMoveStringLiteralDfa13_0(active1, 587793418650910720L, active2, 1526896200908277776L, active3, 65536L);
					}
				case 'f':
					return this.jjMoveStringLiteralDfa13_0(active1, 0L, active2, 18295873486454784L, active3, 0L);
				case 'g':
					return this.jjMoveStringLiteralDfa13_0(active1, 8796093022208L, active2, 8594145280L, active3, 8L);
				case 'i':
					return this.jjMoveStringLiteralDfa13_0(active1, 4543182045970432L, active2, -8646893142039068672L, active3, 0L);
				case 'l':
					return this.jjMoveStringLiteralDfa13_0(active1, 0L, active2, 9216L, active3, 0L);
				case 'm':
					return this.jjMoveStringLiteralDfa13_0(active1, 0L, active2, 10L, active3, 0L);
				case 'n':
					return this.jjMoveStringLiteralDfa13_0(active1, 0L, active2, 34359738368L, active3, 544L);
				case 'o':
					return this.jjMoveStringLiteralDfa13_0(active1, 140737488355328L, active2, 2442166336L, active3, 0L);
				case 'p':
					return this.jjMoveStringLiteralDfa13_0(active1, 0L, active2, 70437463654400L, active3, 0L);
				case 'r':
					return this.jjMoveStringLiteralDfa13_0(active1, 0L, active2, 4611686018427387904L, active3, 8192L);
				case 's':
					return this.jjMoveStringLiteralDfa13_0(active1, 17592186044416L, active2, 288L, active3, 1024L);
				case 't':
					return this.jjMoveStringLiteralDfa13_0(active1, 562949953421312L, active2, 2306411474041438208L, active3, 6535L);
				case 'u':
					return this.jjMoveStringLiteralDfa13_0(active1, 144115188075855872L, active2, 135680L, active3, 0L);
				case 'w':
					if ((active1 & 137438953472L) != 0L) {
						this.jjmatchedKind = 101;
						this.jjmatchedPos = 12;
					}

					return this.jjMoveStringLiteralDfa13_0(active1, 0L, active2, 67174400L, active3, 0L);
				case 'x':
					return this.jjMoveStringLiteralDfa13_0(active1, 0L, active2, 0L, active3, 16L);
			}
		}
	}

	private int jjMoveStringLiteralDfa36_0(long old3, long active3) {
		if ((active3 = active3 & old3) == 0L) {
			return this.jjStartNfa_0(34, 0L, 0L, 0L, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(35, 0L, 0L, 0L, active3);
				return 36;
			}

			switch (this.curChar) {
				case 's':
					if ((active3 & 16384L) != 0L) {
						return this.jjStartNfaWithStates_0(36, 206, 26);
					}

					return this.jjMoveStringLiteralDfa37_0(active3, 98304L);
				default:
					return this.jjStartNfa_0(35, 0L, 0L, 0L, active3);
			}
		}
	}

	private int jjMoveStringLiteralDfa39_0(long old3, long active3) {
		if ((active3 = active3 & old3) == 0L) {
			return this.jjStartNfa_0(37, 0L, 0L, 0L, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(38, 0L, 0L, 0L, active3);
				return 39;
			}

			switch (this.curChar) {
				case 'n':
					return this.jjMoveStringLiteralDfa40_0(active3, 32768L);
				case 't':
					return this.jjMoveStringLiteralDfa40_0(active3, 65536L);
				default:
					return this.jjStartNfa_0(38, 0L, 0L, 0L, active3);
			}
		}
	}

	public ZKMScriptToken getNextToken() {
		ZKMScriptToken specialToken = null;
		int curPos = 0;

		label129:
		while (true) {
			try {
				this.curChar = this.input_stream.BeginToken();
			} catch (IOException ignoredEndOfInput) {
//        	 var40.printStackTrace();
				this.jjmatchedKind = 0;
				ZKMScriptToken eofToken = this.jjFillToken();
				eofToken.specialToken = specialToken;
				return eofToken;
			}

			this.image = this.jjimage;
			this.image.setLength(0);
			this.jjimageLen = 0;

			while (true) {
				switch (this.curLexState) {
					case 0:
						try {
							this.input_stream.backup(0);

							while (this.curChar <= ' ' && (4294981120L & 1L << this.curChar) != 0L) {
								this.curChar = this.input_stream.BeginToken();
							}
						} catch (IOException ignoredSkipWhitespace) {
							continue label129;
						}

						this.jjmatchedKind = Integer.MAX_VALUE;
						this.jjmatchedPos = 0;
						curPos = this.jjMoveStringLiteralDfa0_0();
						break;
					case 1:
						this.jjmatchedKind = Integer.MAX_VALUE;
						this.jjmatchedPos = 0;
						curPos = this.jjMoveStringLiteralDfa0_1();
						if (this.jjmatchedPos == 0 && this.jjmatchedKind > 12) {
							this.jjmatchedKind = 12;
						}
						break;
					case 2:
						this.jjmatchedKind = Integer.MAX_VALUE;
						this.jjmatchedPos = 0;
						curPos = this.jjMoveStringLiteralDfa0_2();
						if (this.jjmatchedPos == 0 && this.jjmatchedKind > 12) {
							this.jjmatchedKind = 12;
						}
						break;
					case 3:
						this.jjmatchedKind = Integer.MAX_VALUE;
						this.jjmatchedPos = 0;
						curPos = this.jjMoveStringLiteralDfa0_3();
						if (this.jjmatchedPos == 0 && this.jjmatchedKind > 12) {
							this.jjmatchedKind = 12;
						}
						break;
					case 4:
						this.jjmatchedKind = Integer.MAX_VALUE;
						this.jjmatchedPos = 0;
						curPos = this.jjMoveStringLiteralDfa0_4();
				}

				if (this.jjmatchedKind == Integer.MAX_VALUE) {
					break label129;
				}

				if (this.jjmatchedPos + 1 < curPos) {
					this.input_stream.backup(curPos - this.jjmatchedPos - 1);
				}

				if ((jjtoToken[this.jjmatchedKind >> 6] & 1L << (this.jjmatchedKind & 63)) != 0L) {
					ZKMScriptToken matchedToken = this.jjFillToken();
					matchedToken.specialToken = specialToken;
					if (jjnewLexState[this.jjmatchedKind] != -1) {
						this.curLexState = jjnewLexState[this.jjmatchedKind];
					}

					return matchedToken;
				}

				if ((jjtoSkip[this.jjmatchedKind >> 6] & 1L << (this.jjmatchedKind & 63)) != 0L) {
					if ((jjtoSpecial[this.jjmatchedKind >> 6] & 1L << (this.jjmatchedKind & 63)) != 0L) {
						ZKMScriptToken skippedToken = this.jjFillToken();
						if (specialToken == null) {
							specialToken = skippedToken;
						} else {
							skippedToken.specialToken = specialToken;
							specialToken = specialToken.next = skippedToken;
						}

						this.SkipLexicalActions(skippedToken);
					} else {
						this.SkipLexicalActions(null);
					}

					if (jjnewLexState[this.jjmatchedKind] != -1) {
						this.curLexState = jjnewLexState[this.jjmatchedKind];
					}
					break;
				}

				this.MoreLexicalActions();
				if (jjnewLexState[this.jjmatchedKind] != -1) {
					this.curLexState = jjnewLexState[this.jjmatchedKind];
				}

				curPos = 0;
				this.jjmatchedKind = Integer.MAX_VALUE;

				try {
					this.curChar = this.input_stream.readChar();
				} catch (IOException ignoredReadChar) {
					break label129;
				}
			}
		}

		int line = this.input_stream.getColumn();
		int column = this.input_stream.getEndColumn();
		String errorImage = null;
		boolean unexpectedEof = false;

		try {
			this.input_stream.readChar();
			this.input_stream.backup(1);
		} catch (IOException ignoredBackup) {
			unexpectedEof = true;
			errorImage = curPos <= 1 ? "" : this.input_stream.GetImage();
			if (this.curChar != '\n' && this.curChar != '\r') {
				column++;
			} else {
				line++;
				column = 0;
			}
		}

		if (!unexpectedEof) {
			this.input_stream.backup(1);
			errorImage = curPos <= 1 ? "" : this.input_stream.GetImage();
		}

		throw new LexicalException(unexpectedEof, this.curLexState, line, column, errorImage, this.curChar, 0);
	}

	private int jjMoveStringLiteralDfa0_4() {
		switch (this.curChar) {
			case '"':
				return this.jjStartNfaWithStates_4(0, 210, 2);
			default:
				return this.jjMoveNfa_4(0, 0);
		}
	}

	private int jjMoveStringLiteralDfa2_0(long old0, long active0, long old1, long active1, long old2, long active2, long old3, long active3) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1) | (active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(0, old0, old1, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(1, active0, active1, active2, active3);
				return 2;
			}

			switch (this.curChar) {
				case 'C':
					return this.jjMoveStringLiteralDfa3_0(active0, 2251799813685248L, active1, 0L, active2, 0L, active3, 0L);
				case 'D':
				case 'E':
				case 'F':
				case 'G':
				case 'H':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'O':
				case 'P':
				case 'Q':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'j':
				case 'q':
				default:
					return this.jjStartNfa_0(1, active0, active1, active2, active3);
				case 'I':
					return this.jjMoveStringLiteralDfa3_0(active0, 17592186044416L, active1, 4294967296L, active2, 0L, active3, 0L);
				case 'N':
					return this.jjMoveStringLiteralDfa3_0(active0, 0L, active1, 0L, active2, 512L, active3, 0L);
				case 'R':
					return this.jjMoveStringLiteralDfa3_0(active0, 0L, active1, 0L, active2, 16384L, active3, 0L);
				case 'S':
					return this.jjMoveStringLiteralDfa3_0(active0, 0L, active1, 17592186044416L, active2, 0L, active3, 0L);
				case 'a':
					return this.jjMoveStringLiteralDfa3_0(active0, 396598242185314304L, active1, 2322718314790912L, active2, 549755813888L, active3, 0L);
				case 'b':
					return this.jjMoveStringLiteralDfa3_0(active0, 144115188075855872L, active1, 0L, active2, 0L, active3, 0L);
				case 'c':
					return this.jjMoveStringLiteralDfa3_0(active0, 0L, active1, 1108101562377L, active2, 18014402810085392L, active3, 0L);
				case 'd':
					if ((active0 & 274877906944L) != 0L) {
						return this.jjStartNfaWithStates_0(2, 38, 26);
					}

					return this.jjMoveStringLiteralDfa3_0(active0, 0L, active1, 2199023255616L, active2, 2097152L, active3, 0L);
				case 'e':
					return this.jjMoveStringLiteralDfa3_0(active0, 4947802324992L, active1, 1154047405050609696L, active2, 72339069014642724L, active3, 73728L);
				case 'f':
					return this.jjMoveStringLiteralDfa3_0(active0, 0L, active1, 137439084544L, active2, 1454838601568290944L, active3, 0L);
				case 'g':
					return this.jjMoveStringLiteralDfa3_0(active0, 140737488355328L, active1, 4503599694479360L, active2, 1099511627776L, active3, 0L);
				case 'h':
					return this.jjMoveStringLiteralDfa3_0(active0, 0L, active1, 1024L, active2, 0L, active3, 0L);
				case 'i':
					return this.jjMoveStringLiteralDfa3_0(active0, 13616351998377984L, active1, 2594354930135334928L, active2, 2199023255552L, active3, 0L);
				case 'k':
					return this.jjMoveStringLiteralDfa3_0(active0, 0L, active1, 144115188075855872L, active2, 0L, active3, 0L);
				case 'l':
					if ((active0 & 137438953472L) != 0L) {
						this.jjmatchedKind = 37;
						this.jjmatchedPos = 2;
					}

					return this.jjMoveStringLiteralDfa3_0(active0, 1153484454560268288L, active1, 9007199254743552L, active2, 184102261851422722L, active3, 16L);
				case 'm':
					return this.jjMoveStringLiteralDfa3_0(active0, 0L, active1, Long.MIN_VALUE, active2, 412316860416L, active3, 0L);
				case 'n':
					return this.jjMoveStringLiteralDfa3_0(active0, -9204222942345428992L, active1, 36802932736L, active2, 10754195456L, active3, 0L);
				case 'o':
					return this.jjMoveStringLiteralDfa3_0(active0, 0L, active1, 274882363392L, active2, 32768L, active3, 0L);
				case 'p':
					return this.jjMoveStringLiteralDfa3_0(active0, 0L, active1, 134217728L, active2, 33562624L, active3, 0L);
				case 'r':
					return this.jjMoveStringLiteralDfa3_0(active0, 6917529027641081856L, active1, 128L, active2, 4415226380288L, active3, 1536L);
				case 's':
					return this.jjMoveStringLiteralDfa3_0(active0, 0L, active1, 4683893146046693632L, active2, -1729373459742981120L, active3, 55400L);
				case 't':
					return this.jjMoveStringLiteralDfa3_0(active0, 576460752303423488L, active1, 598134326034436L, active2, 70437874696256L, active3, 391L);
				case 'u':
					return this.jjMoveStringLiteralDfa3_0(active0, 3298534883328L, active1, 0L, active2, 0L, active3, 0L);
				case 'v':
					return this.jjMoveStringLiteralDfa3_0(active0, 0L, active1, 33554434L, active2, 0L, active3, 0L);
				case 'w':
					return this.jjMoveStringLiteralDfa3_0(active0, 0L, active1, 630508345878380544L, active2, 9L, active3, 0L);
				case 'x':
					return this.jjMoveStringLiteralDfa3_0(active0, 0L, active1, 17179869184L, active2, 256L, active3, 0L);
			}
		}
	}

	private int jjMoveStringLiteralDfa28_0(long old2, long active2, long old3, long active3) {
		if (((active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(26, 0L, 0L, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(27, 0L, 0L, active2, active3);
				return 28;
			}

			switch (this.curChar) {
				case 'c':
					return this.jjMoveStringLiteralDfa29_0(active2, 0L, active3, 1408L);
				case 'e':
					if ((active3 & 1L) != 0L) {
						return this.jjStartNfaWithStates_0(28, 192, 26);
					} else if ((active3 & 2L) != 0L) {
						return this.jjStartNfaWithStates_0(28, 193, 26);
					} else {
						if ((active3 & 4L) != 0L) {
							return this.jjStartNfaWithStates_0(28, 194, 26);
						}

						return this.jjMoveStringLiteralDfa29_0(active2, 0L, active3, 24L);
					}
				case 'f':
					return this.jjMoveStringLiteralDfa29_0(active2, 0L, active3, 65536L);
				case 'g':
					return this.jjMoveStringLiteralDfa29_0(active2, 0L, active3, 512L);
				case 'i':
					return this.jjMoveStringLiteralDfa29_0(active2, 0L, active3, 32832L);
				case 'o':
					return this.jjMoveStringLiteralDfa29_0(active2, 0L, active3, 32L);
				case 'r':
					if ((active2 & Long.MIN_VALUE) != 0L) {
						return this.jjStartNfaWithStates_0(28, 191, 26);
					}
				case 'd':
				case 'h':
				case 'j':
				case 'k':
				case 'l':
				case 'm':
				case 'n':
				case 'p':
				case 'q':
				case 't':
				case 'v':
				case 'w':
				default:
					return this.jjStartNfa_0(27, 0L, 0L, active2, active3);
				case 's':
					if ((active2 & 4611686018427387904L) != 0L) {
						return this.jjStartNfaWithStates_0(28, 190, 26);
					}

					return this.jjMoveStringLiteralDfa29_0(active2, 0L, active3, 2048L);
				case 'u':
					return this.jjMoveStringLiteralDfa29_0(active2, 0L, active3, 12288L);
				case 'x':
					return this.jjMoveStringLiteralDfa29_0(active2, 0L, active3, 16384L);
			}
		}
	}

	private int jjMoveStringLiteralDfa1_2(long active0) {
		try {
			this.curChar = this.input_stream.readChar();
		} catch (IOException ignored) {
			return 1;
		}

		switch (this.curChar) {
			case '/':
				if ((active0 & 1024L) != 0L) {
					return this.jjStopAtPos(1, 10);
				}

				return 2;
			default:
				return 2;
		}
	}

	private int jjMoveStringLiteralDfa30_0(long old3, long active3) {
		if ((active3 = active3 & old3) == 0L) {
			return this.jjStartNfa_0(28, 0L, 0L, 0L, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(29, 0L, 0L, 0L, active3);
				return 30;
			}

			switch (this.curChar) {
				case 'L':
					return this.jjMoveStringLiteralDfa31_0(active3, 512L);
				case 'a':
					return this.jjMoveStringLiteralDfa31_0(active3, 1024L);
				case 'c':
					return this.jjMoveStringLiteralDfa31_0(active3, 8192L);
				case 'i':
					return this.jjMoveStringLiteralDfa31_0(active3, 4096L);
				case 'l':
					return this.jjMoveStringLiteralDfa31_0(active3, 16384L);
				case 'n':
					return this.jjMoveStringLiteralDfa31_0(active3, 32832L);
				case 'o':
					return this.jjMoveStringLiteralDfa31_0(active3, 67584L);
				case 's':
					if ((active3 & 32L) != 0L) {
						return this.jjStartNfaWithStates_0(30, 197, 26);
					}
				default:
					return this.jjStartNfa_0(29, 0L, 0L, 0L, active3);
				case 'u':
					return this.jjMoveStringLiteralDfa31_0(active3, 384L);
			}
		}
	}

	private int jjMoveStringLiteralDfa22_0(long old2, long active2, long old3, long active3) {
		if (((active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(20, 0L, 0L, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(21, 0L, 0L, active2, active3);
				return 22;
			}

			switch (this.curChar) {
				case 'E':
					return this.jjMoveStringLiteralDfa23_0(active2, 0L, active3, 66L);
				case 'F':
				case 'G':
				case 'H':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'O':
				case 'Q':
				case 'R':
				case 'S':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'b':
				case 'g':
				case 'h':
				case 'j':
				case 'p':
				case 'q':
				case 'v':
				case 'w':
				default:
					return this.jjStartNfa_0(21, 0L, 0L, active2, active3);
				case 'I':
					return this.jjMoveStringLiteralDfa23_0(active2, 0L, active3, 1L);
				case 'N':
					return this.jjMoveStringLiteralDfa23_0(active2, 0L, active3, 8192L);
				case 'P':
					return this.jjMoveStringLiteralDfa23_0(active2, 0L, active3, 4L);
				case 'a':
					return this.jjMoveStringLiteralDfa23_0(active2, 36310271995674624L, active3, 16384L);
				case 'c':
					return this.jjMoveStringLiteralDfa23_0(active2, 1170935903116328960L, active3, 0L);
				case 'd':
					return this.jjMoveStringLiteralDfa23_0(active2, 481036337152L, active3, 8L);
				case 'e':
					if ((active2 & 2147483648L) != 0L) {
						return this.jjStartNfaWithStates_0(22, 159, 26);
					} else {
						if ((active2 & 17179869184L) != 0L) {
							return this.jjStartNfaWithStates_0(22, 162, 26);
						}

						return this.jjMoveStringLiteralDfa23_0(active2, 72058143793741824L, active3, 67584L);
					}
				case 'f':
					return this.jjMoveStringLiteralDfa23_0(active2, 144115188075855872L, active3, 32768L);
				case 'i':
					return this.jjMoveStringLiteralDfa23_0(active2, 8796093022208L, active3, 0L);
				case 'k':
					return this.jjMoveStringLiteralDfa23_0(active2, 140737488355328L, active3, 0L);
				case 'l':
					return this.jjMoveStringLiteralDfa23_0(active2, 4625302370425765888L, active3, 0L);
				case 'm':
					return this.jjMoveStringLiteralDfa23_0(active2, 0L, active3, 1024L);
				case 'n':
					return this.jjMoveStringLiteralDfa23_0(active2, 1099511627776L, active3, 4096L);
				case 'o':
					return this.jjMoveStringLiteralDfa23_0(active2, Long.MIN_VALUE, active3, 0L);
				case 'r':
					return this.jjMoveStringLiteralDfa23_0(active2, 576460752303423488L, active3, 0L);
				case 's':
					if ((active2 & 4294967296L) != 0L) {
						return this.jjStartNfaWithStates_0(22, 160, 26);
					} else if ((active2 & 8589934592L) != 0L) {
						return this.jjStartNfaWithStates_0(22, 161, 26);
					} else {
						if ((active2 & 34359738368L) != 0L) {
							return this.jjStartNfaWithStates_0(22, 163, 26);
						}

						return this.jjMoveStringLiteralDfa23_0(active2, 2199023255552L, active3, 0L);
					}
				case 't':
					return this.jjMoveStringLiteralDfa23_0(active2, 17592186044416L, active3, 400L);
				case 'u':
					return this.jjMoveStringLiteralDfa23_0(active2, 2309788056934154240L, active3, 512L);
				case 'x':
					return this.jjMoveStringLiteralDfa23_0(active2, 288230376151711744L, active3, 32L);
			}
		}
	}

	private int jjMoveStringLiteralDfa37_0(long old3, long active3) {
		if ((active3 = active3 & old3) == 0L) {
			return this.jjStartNfa_0(35, 0L, 0L, 0L, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(36, 0L, 0L, 0L, active3);
				return 37;
			}

			switch (this.curChar) {
				case 'c':
					return this.jjMoveStringLiteralDfa38_0(active3, 65536L);
				case 'i':
					return this.jjMoveStringLiteralDfa38_0(active3, 32768L);
				default:
					return this.jjStartNfa_0(36, 0L, 0L, 0L, active3);
			}
		}
	}

	private void jjCheckNAdd(int state) {
		if (this.jjrounds[state] != this.jjround) {
			this.jjstateSet[this.jjnewStateCnt++] = state;
			this.jjrounds[state] = this.jjround;
		}
	}

	private int jjMoveStringLiteralDfa24_0(long old2, long active2, long old3, long active3) {
		if (((active2 = active2 & old2) | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(22, 0L, 0L, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(23, 0L, 0L, active2, active3);
				return 24;
			}

			switch (this.curChar) {
				case 'C':
					return this.jjMoveStringLiteralDfa25_0(active2, 0L, active3, 512L);
				case 'D':
				case 'F':
				case 'G':
				case 'H':
				case 'I':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'N':
				case 'P':
				case 'Q':
				case 'R':
				case 'S':
				case 'T':
				case 'U':
				case 'V':
				case 'W':
				case 'X':
				case 'Y':
				case 'Z':
				case '[':
				case '\\':
				case ']':
				case '^':
				case '_':
				case '`':
				case 'a':
				case 'b':
				case 'f':
				case 'h':
				case 'j':
				case 'k':
				case 'm':
				case 'p':
				case 'q':
				case 'r':
				case 'v':
				case 'w':
				default:
					return this.jjStartNfa_0(23, 0L, 0L, active2, active3);
				case 'E':
					return this.jjMoveStringLiteralDfa25_0(active2, 0L, active3, 4096L);
				case 'O':
					return this.jjMoveStringLiteralDfa25_0(active2, Long.MIN_VALUE, active3, 0L);
				case 'c':
					return this.jjMoveStringLiteralDfa25_0(active2, 0L, active3, 71L);
				case 'd':
					return this.jjMoveStringLiteralDfa25_0(active2, 13616351998377984L, active3, 0L);
				case 'e':
					if ((active2 & 4398046511104L) != 0L) {
						return this.jjStartNfaWithStates_0(24, 170, 26);
					}

					return this.jjMoveStringLiteralDfa25_0(active2, 76279718688587776L, active3, 65536L);
				case 'g':
					return this.jjMoveStringLiteralDfa25_0(active2, 36169534507319296L, active3, 16384L);
				case 'i':
					return this.jjMoveStringLiteralDfa25_0(active2, 2305843009213693952L, active3, 16L);
				case 'l':
					return this.jjMoveStringLiteralDfa25_0(active2, 288230376151711744L, active3, 40L);
				case 'n':
					if ((active2 & 8796093022208L) != 0L) {
						return this.jjStartNfaWithStates_0(24, 171, 26);
					}

					return this.jjMoveStringLiteralDfa25_0(active2, 576460752303423488L, active3, 0L);
				case 'o':
					return this.jjMoveStringLiteralDfa25_0(active2, 0L, active3, 384L);
				case 's':
					if ((active2 & 2199023255552L) != 0L) {
						return this.jjStartNfaWithStates_0(24, 169, 26);
					} else {
						if ((active2 & 17592186044416L) != 0L) {
							return this.jjStartNfaWithStates_0(24, 172, 26);
						}

						return this.jjMoveStringLiteralDfa25_0(active2, 4611686018427387904L, active3, 32768L);
					}
				case 't':
					return this.jjMoveStringLiteralDfa25_0(active2, 0L, active3, 8192L);
				case 'u':
					return this.jjMoveStringLiteralDfa25_0(active2, 1315051091192184832L, active3, 1024L);
				case 'x':
					return this.jjMoveStringLiteralDfa25_0(active2, 0L, active3, 2048L);
			}
		}
	}

	private int jjMoveStringLiteralDfa29_0(long old2, long active2, long old3, long active3) {
		long maskedActive2 = active2 & old2;
		long duplicatedMaskedActive2 = active2 & old2;
		if ((maskedActive2 | (active3 = active3 & old3)) == 0L) {
			return this.jjStartNfa_0(27, 0L, 0L, old2, old3);
		} else {
			try {
				this.curChar = this.input_stream.readChar();
			} catch (IOException ignored) {
				this.jjStopStringLiteralDfa_0(28, 0L, 0L, 0L, active3);
				return 29;
			}

			switch (this.curChar) {
				case 'N':
					return this.jjMoveStringLiteralDfa30_0(active3, 65536L);
				case 'c':
					return this.jjMoveStringLiteralDfa30_0(active3, 16384L);
				case 'e':
					return this.jjMoveStringLiteralDfa30_0(active3, 512L);
				case 'i':
					return this.jjMoveStringLiteralDfa30_0(active3, 2048L);
				case 'l':
					return this.jjMoveStringLiteralDfa30_0(active3, 1408L);
				case 'n':
					return this.jjMoveStringLiteralDfa30_0(active3, 32L);
				case 'o':
					return this.jjMoveStringLiteralDfa30_0(active3, 32832L);
				case 's':
					if ((active3 & 8L) != 0L) {
						return this.jjStartNfaWithStates_0(29, 195, 26);
					} else {
						if ((active3 & 16L) != 0L) {
							return this.jjStartNfaWithStates_0(29, 196, 26);
						}

						return this.jjMoveStringLiteralDfa30_0(active3, 12288L);
					}
				default:
					return this.jjStartNfa_0(28, 0L, 0L, 0L, active3);
			}
		}
	}
}
