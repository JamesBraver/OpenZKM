package com.zelix.klassmaster.changelogs.parser;

import com.zelix.klassmaster.script.TokenMgrError;
import java.io.IOException;
import java.io.PrintStream;

public class ChangeLogsTokenManager implements ChangeLogsTokenList {
	public static PrintStream debugStream = System.out;
	public static final long[] jjbitVec0 = new long[]{0L, 0L, -1L, -1L};
	public static final long[] jjbitVec1 = new long[]{-2L, -1L, -1L, -1L};
	public static final int[] jjnextStates = new int[0];
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
		"\ufeff",
		"(",
		":",
		"::",
		")",
		"[",
		"]",
		",",
		"/",
		".",
		"*",
		"=>",
		"and",
		"enum",
		"none",
		"true",
		"Data:",
		"final",
		"false",
		"light",
		"heavy",
		"<init>",
		"Class:",
		"normal",
		"bridge",
		"public",
		"static",
		"native",
		"Module:",
		"execute",
		"exclude",
		"Source:",
		"<clinit>",
		"Package:",
		"private",
		"enhanced",
		"abstract",
		"volatile",
		"protected",
		"synthetic",
		"interface",
		"transient",
		"FieldsOf:",
		"MethodsOf:",
		"aggressive",
		"annotation",
		"package-info",
		"MemberClass:",
		"synchronized",
		"ForwardClass:",
		"Manufactured:",
		"NameNotChanged",
		"LineNumbersOf:",
		"TraceBackClass:",
		"SignatureNotChanged",
		"SignatureNotChanged:",
		"AutoReflectionClass:",
		"ObfuscateReferencesClass:",
		"MethodParameterChangeClasses:",
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
		4,
		0,
		-1,
		-1,
		-1,
		-1
	};
	public static final long[] jjtoToken = new long[]{-8191L, 15103L};
	public static final long[] jjtoSkip = new long[]{3646L, 0L};
	public static final long[] jjtoSpecial = new long[]{3584L, 0L};
	public static final long[] jjtoMore = new long[]{4544L, 1280L};
	private static final int[] jjrounds = new int[7];
	private static final int[] jjstateSet = new int[14];
	private static final StringBuilder jjimage = new StringBuilder();
	private static StringBuilder image = jjimage;
	public static int curLexState = 0;
	public static int defaultLexState = 0;
	protected static char curChar;
	public static int jjmatchedPos;
	public static int jjround;
	private static int jjimageLen;
	private static int lengthOfMatch;
	public static int jjmatchedKind;
	protected static SimpleCharStream3 input_stream;
	public static int jjnewStateCnt;

	public ChangeLogsTokenManager(SimpleCharStream3 stream) {
		if (input_stream != null) {
			throw new TokenMgrError("ERROR: Second call to constructor of static lexer. You must use ReInit() to initialize the static variables.", 1);
		} else {
			input_stream = stream;
		}
	}

	private static int jjMoveStringLiteralDfa8_0(long old0, long active0, long old1, long active1) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1)) == 0L) {
			return jjStartNfa_0(6, old0, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(7, active0, active1);
				return 8;
			}

			switch (curChar) {
				case ':':
					if ((active0 & 36028797018963968L) != 0L) {
						return jjStopAtPos(8, 55);
					}
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
				case 'j':
				case 'm':
				case 'n':
				case 'p':
				case 'q':
				case 's':
				default:
					break;
				case 'a':
					return jjMoveStringLiteralDfa9_0(active0, 1152921504606846976L, active1, 0L);
				case 'c':
					if ((active0 & 4503599627370496L) != 0L) {
						return jjStartNfaWithStates_0(8, 52, 7);
					}
					break;
				case 'd':
					if ((active0 & 2251799813685248L) != 0L) {
						return jjStartNfaWithStates_0(8, 51, 7);
					}
					break;
				case 'e':
					if ((active0 & 9007199254740992L) != 0L) {
						return jjStartNfaWithStates_0(8, 53, 7);
					}

					return jjMoveStringLiteralDfa9_0(active0, 0L, active1, 122L);
				case 'f':
					return jjMoveStringLiteralDfa9_0(active0, 72057594037927936L, active1, 0L);
				case 'h':
					return jjMoveStringLiteralDfa9_0(active0, 0L, active1, 1L);
				case 'i':
					return jjMoveStringLiteralDfa9_0(active0, 2882303761517117440L, active1, 0L);
				case 'k':
					return jjMoveStringLiteralDfa9_0(active0, 0L, active1, 4L);
				case 'l':
					return jjMoveStringLiteralDfa9_0(active0, 4611686018427387904L, active1, 0L);
				case 'o':
					return jjMoveStringLiteralDfa9_0(active0, 288230376151711744L, active1, 0L);
				case 'r':
					return jjMoveStringLiteralDfa9_0(active0, 0L, active1, 128L);
				case 't':
					if ((active0 & 18014398509481984L) != 0L) {
						return jjStartNfaWithStates_0(8, 54, 7);
					}
					break;
				case 'u':
					return jjMoveStringLiteralDfa9_0(active0, Long.MIN_VALUE, active1, 0L);
				case 'v':
					return jjMoveStringLiteralDfa9_0(active0, 144115188075855872L, active1, 0L);
			}

			return jjStartNfa_0(7, active0, active1);
		}
	}

	private static int jjMoveStringLiteralDfa6_0(long old0, long active0, long old1, long active1) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1)) == 0L) {
			return jjStartNfa_0(4, old0, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(5, active0, active1);
				return 6;
			}

			switch (curChar) {
				case ':':
					if ((active0 & 2199023255552L) != 0L) {
						return jjStopAtPos(6, 41);
					} else if ((active0 & 17592186044416L) != 0L) {
						return jjStopAtPos(6, 44);
					}
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
				case 'F':
				case 'G':
				case 'H':
				case 'I':
				case 'J':
				case 'K':
				case 'L':
				case 'M':
				case 'N':
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
				case 'i':
				case 'j':
				case 'k':
				case 'n':
				case 'p':
				case 'q':
				case 'r':
				default:
					return jjStartNfa_0(5, active0, active1);
				case 'C':
					return jjMoveStringLiteralDfa7_0(active0, 1152921504606846976L, active1, 0L);
				case 'O':
					return jjMoveStringLiteralDfa7_0(active0, 36028797018963968L, active1, 0L);
				case 'P':
					return jjMoveStringLiteralDfa7_0(active0, 0L, active1, 128L);
				case 'a':
					return jjMoveStringLiteralDfa7_0(active0, 9007199254740992L, active1, 68L);
				case 'c':
					return jjMoveStringLiteralDfa7_0(active0, -9222809086901354496L, active1, 0L);
				case 'd':
					return jjMoveStringLiteralDfa7_0(active0, 4611686018427387904L, active1, 0L);
				case 'e':
					if ((active0 & 4398046511104L) != 0L) {
						return jjStartNfaWithStates_0(6, 42, 7);
					} else if ((active0 & 8796093022208L) != 0L) {
						return jjStartNfaWithStates_0(6, 43, 7);
					} else {
						if ((active0 & 140737488355328L) != 0L) {
							return jjStartNfaWithStates_0(6, 47, 7);
						}

						return jjMoveStringLiteralDfa7_0(active0, 594826994533793792L, active1, 0L);
					}
				case 'f':
					return jjMoveStringLiteralDfa7_0(active0, 0L, active1, 32L);
				case 'l':
					return jjMoveStringLiteralDfa7_0(active0, 1125899906842624L, active1, 0L);
				case 'm':
					return jjMoveStringLiteralDfa7_0(active0, 0L, active1, 2L);
				case 'o':
					return jjMoveStringLiteralDfa7_0(active0, 2305843009213693952L, active1, 0L);
				case 's':
					return jjMoveStringLiteralDfa7_0(active0, 216172782113783808L, active1, 0L);
				case 't':
					return jjMoveStringLiteralDfa7_0(active0, 295020959964856320L, active1, 1L);
				case 'u':
					return jjMoveStringLiteralDfa7_0(active0, 0L, active1, 24L);
			}
		}
	}

	private static final int jjStopStringLiteralDfa_0(int pos, long active0, long active1) {
		switch (pos) {
			case 0:
				if ((active0 & 2097472L) != 0L) {
					return 2;
				} else if ((active0 & 8192L) != 0L) {
					return 6;
				} else {
					if ((active0 & -35201585512448L) == 0L && (active1 & 255L) == 0L) {
						return -1;
					}

					jjmatchedKind = 76;
					return 7;
				}
			case 1:
				if ((active0 & 256L) != 0L) {
					return 0;
				} else {
					if ((active0 & -35201585512448L) == 0L && (active1 & 255L) == 0L) {
						return -1;
					}

					jjmatchedKind = 76;
					jjmatchedPos = 1;
					return 7;
				}
			case 2:
				if ((active0 & 33554432L) != 0L) {
					return 7;
				} else {
					if ((active0 & -35201619066880L) == 0L && (active1 & 255L) == 0L) {
						return -1;
					}

					jjmatchedKind = 76;
					jjmatchedPos = 2;
					return 7;
				}
			case 3:
				if ((active0 & 469762048L) != 0L) {
					return 7;
				} else {
					if ((active0 & -35202088828928L) == 0L && (active1 & 255L) == 0L) {
						return -1;
					}

					jjmatchedKind = 76;
					jjmatchedPos = 3;
					return 7;
				}
			case 4:
				if ((active0 & 16106127360L) != 0L) {
					return 7;
				} else {
					if ((active0 & -35218731827200L) == 0L && (active1 & 255L) == 0L) {
						return -1;
					}

					jjmatchedKind = 76;
					jjmatchedPos = 4;
					return 7;
				}
			case 5:
				if ((active0 & 2130303778816L) != 0L) {
					return 7;
				} else {
					if ((active0 & -37383395344384L) == 0L && (active1 & 255L) == 0L) {
						return -1;
					}

					jjmatchedKind = 76;
					jjmatchedPos = 5;
					return 7;
				}
			case 6:
				if ((active0 & 153931627888640L) != 0L) {
					return 7;
				} else {
					if ((active0 & -211106232532992L) == 0L && (active1 & 255L) == 0L) {
						return -1;
					}

					jjmatchedKind = 76;
					jjmatchedPos = 6;
					return 7;
				}
			case 7:
				if ((active0 & 1970324836974592L) != 0L) {
					return 7;
				} else if ((active0 & 576460752303423488L) != 0L) {
					jjmatchedKind = 77;
					jjmatchedPos = 7;
					return 6;
				} else {
					if ((active0 & -578712552117108736L) == 0L && (active1 & 255L) == 0L) {
						return -1;
					}

					jjmatchedKind = 76;
					jjmatchedPos = 7;
					return 7;
				}
			case 8:
				if ((active0 & 33776997205278720L) != 0L) {
					return 7;
				} else {
					if ((active0 & -648518346341351424L) == 0L && (active1 & 255L) == 0L) {
						if ((active0 & 576460752303423488L) != 0L) {
							jjmatchedKind = 77;
							jjmatchedPos = 8;
							return 6;
						}

						return -1;
					}

					jjmatchedKind = 76;
					jjmatchedPos = 8;
					return 7;
				}
			case 9:
				if ((active0 & 432345564227567616L) != 0L) {
					return 7;
				} else {
					if ((active0 & -1152921504606846976L) == 0L && (active1 & 255L) == 0L) {
						if ((active0 & 576460752303423488L) != 0L) {
							jjmatchedKind = 77;
							jjmatchedPos = 9;
							return 6;
						}

						return -1;
					}

					jjmatchedKind = 76;
					jjmatchedPos = 9;
					return 7;
				}
			case 10:
				if ((active0 & -1152921504606846976L) == 0L && (active1 & 255L) == 0L) {
					if ((active0 & 576460752303423488L) != 0L) {
						jjmatchedKind = 77;
						jjmatchedPos = 10;
						return 6;
					}

					return -1;
				}

				jjmatchedKind = 76;
				jjmatchedPos = 10;
				return 7;
			case 11:
				if ((active0 & 2305843009213693952L) != 0L) {
					return 7;
				} else {
					if ((active0 & -4611686018427387904L) == 0L && (active1 & 255L) == 0L) {
						if ((active0 & 576460752303423488L) != 0L) {
							return 6;
						}

						return -1;
					}

					jjmatchedKind = 76;
					jjmatchedPos = 11;
					return 7;
				}
			case 12:
				if ((active1 & 255L) != 0L) {
					jjmatchedKind = 76;
					jjmatchedPos = 12;
					return 7;
				}

				return -1;
			case 13:
				if ((active1 & 1L) != 0L) {
					return 7;
				} else {
					if ((active1 & 252L) != 0L) {
						jjmatchedKind = 76;
						jjmatchedPos = 13;
						return 7;
					}

					return -1;
				}
			case 14:
				if ((active1 & 248L) != 0L) {
					jjmatchedKind = 76;
					jjmatchedPos = 14;
					return 7;
				}

				return -1;
			case 15:
				if ((active1 & 248L) != 0L) {
					jjmatchedKind = 76;
					jjmatchedPos = 15;
					return 7;
				}

				return -1;
			case 16:
				if ((active1 & 248L) != 0L) {
					jjmatchedKind = 76;
					jjmatchedPos = 16;
					return 7;
				}

				return -1;
			case 17:
				if ((active1 & 248L) != 0L) {
					jjmatchedKind = 76;
					jjmatchedPos = 17;
					return 7;
				}

				return -1;
			case 18:
				if ((active1 & 24L) != 0L) {
					return 7;
				} else {
					if ((active1 & 224L) != 0L) {
						if (jjmatchedPos != 18) {
							jjmatchedKind = 76;
							jjmatchedPos = 18;
						}

						return 7;
					}

					return -1;
				}
			case 19:
				if ((active1 & 192L) != 0L) {
					jjmatchedKind = 76;
					jjmatchedPos = 19;
					return 7;
				}

				return -1;
			case 20:
				if ((active1 & 192L) != 0L) {
					jjmatchedKind = 76;
					jjmatchedPos = 20;
					return 7;
				}

				return -1;
			case 21:
				if ((active1 & 192L) != 0L) {
					jjmatchedKind = 76;
					jjmatchedPos = 21;
					return 7;
				}

				return -1;
			case 22:
				if ((active1 & 192L) != 0L) {
					jjmatchedKind = 76;
					jjmatchedPos = 22;
					return 7;
				}

				return -1;
			case 23:
				if ((active1 & 192L) != 0L) {
					jjmatchedKind = 76;
					jjmatchedPos = 23;
					return 7;
				}

				return -1;
			case 24:
				if ((active1 & 128L) != 0L) {
					jjmatchedKind = 76;
					jjmatchedPos = 24;
					return 7;
				}

				return -1;
			case 25:
				if ((active1 & 128L) != 0L) {
					jjmatchedKind = 76;
					jjmatchedPos = 25;
					return 7;
				}

				return -1;
			case 26:
				if ((active1 & 128L) != 0L) {
					jjmatchedKind = 76;
					jjmatchedPos = 26;
					return 7;
				}

				return -1;
			case 27:
				if ((active1 & 128L) != 0L) {
					jjmatchedKind = 76;
					jjmatchedPos = 27;
					return 7;
				}

				return -1;
			default:
				return -1;
		}
	}

	private static int jjMoveStringLiteralDfa2_0(long old0, long active0, long old1, long active1) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1)) == 0L) {
			return jjStartNfa_0(0, old0, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(1, active0, active1);
				return 2;
			}

			switch (curChar) {
				case 'a':
					return jjMoveStringLiteralDfa3_0(active0, 18014991214968832L, active1, 4L);
				case 'b':
					return jjMoveStringLiteralDfa3_0(active0, 274877906944L, active1, 0L);
				case 'c':
					return jjMoveStringLiteralDfa3_0(active0, 576539917140623360L, active1, 0L);
				case 'd':
					if ((active0 & 33554432L) != 0L) {
						return jjStartNfaWithStates_0(2, 25, 7);
					}

					return jjMoveStringLiteralDfa3_0(active0, 2199023255552L, active1, 0L);
				case 'e':
					return jjMoveStringLiteralDfa3_0(active0, 36033195065475072L, active1, 0L);
				case 'f':
					return jjMoveStringLiteralDfa3_0(active0, 0L, active1, 64L);
				case 'g':
					return jjMoveStringLiteralDfa3_0(active0, 144115192370823168L, active1, 24L);
				case 'h':
					return jjMoveStringLiteralDfa3_0(active0, 281474976710656L, active1, 0L);
				case 'i':
					return jjMoveStringLiteralDfa3_0(active0, 140874927308800L, active1, 0L);
				case 'j':
				case 'k':
				case 'p':
				case 'q':
				default:
					return jjStartNfa_0(1, active0, active1);
				case 'l':
					return jjMoveStringLiteralDfa3_0(active0, 1161086426415104L, active1, 0L);
				case 'm':
					return jjMoveStringLiteralDfa3_0(active0, 1152921504606846976L, active1, 1L);
				case 'n':
					return jjMoveStringLiteralDfa3_0(active0, -6624795033474170880L, active1, 2L);
				case 'o':
					return jjMoveStringLiteralDfa3_0(active0, 2251799813685248L, active1, 0L);
				case 'r':
					return jjMoveStringLiteralDfa3_0(active0, 4611686087146864640L, active1, 0L);
				case 's':
					return jjMoveStringLiteralDfa3_0(active0, 562949953421312L, active1, 0L);
				case 't':
					return jjMoveStringLiteralDfa3_0(active0, 81065893341167616L, active1, 160L);
				case 'u':
					return jjMoveStringLiteralDfa3_0(active0, 17592521588736L, active1, 0L);
			}
		}
	}

	private static int jjMoveStringLiteralDfa24_0(long old1, long active1) {
		if ((active1 = active1 & old1) == 0L) {
			return jjStartNfa_0(22, 0L, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(23, 0L, active1);
				return 24;
			}

			switch (curChar) {
				case ':':
					if ((active1 & 64L) != 0L) {
						return jjStopAtPos(24, 70);
					}
				default:
					return jjStartNfa_0(23, 0L, active1);
				case 's':
					return jjMoveStringLiteralDfa25_0(active1, 128L);
			}
		}
	}

	private static int jjMoveStringLiteralDfa5_0(long old0, long active0, long old1, long active1) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1)) == 0L) {
			return jjStartNfa_0(3, old0, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(4, active0, active1);
				return 5;
			}

			switch (curChar) {
				case ':':
					if ((active0 & 34359738368L) != 0L) {
						return jjStopAtPos(5, 35);
					}
				case ';':
				case '<':
				case '=':
				case '?':
				case '@':
				case 'A':
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
				case 'k':
				case 'm':
				case 'n':
				case 'p':
				case 'q':
				default:
					break;
				case '>':
					if ((active0 & 17179869184L) != 0L) {
						return jjStopAtPos(5, 34);
					}
					break;
				case 'B':
					return jjMoveStringLiteralDfa6_0(active0, 0L, active1, 4L);
				case 'a':
					return jjMoveStringLiteralDfa6_0(active0, -8934578710749642752L, active1, 0L);
				case 'c':
					if ((active0 & 274877906944L) != 0L) {
						return jjStartNfaWithStates_0(5, 38, 7);
					}

					if ((active0 & 549755813888L) != 0L) {
						return jjStartNfaWithStates_0(5, 39, 7);
					}

					return jjMoveStringLiteralDfa6_0(active0, 2533274790395904L, active1, 64L);
				case 'd':
					return jjMoveStringLiteralDfa6_0(active0, 72066390130950144L, active1, 128L);
				case 'e':
					if ((active0 & 137438953472L) != 0L) {
						return jjStartNfaWithStates_0(5, 37, 7);
					}

					if ((active0 & 1099511627776L) != 0L) {
						return jjStartNfaWithStates_0(5, 40, 7);
					}

					return jjMoveStringLiteralDfa6_0(active0, 4523390836670464L, active1, 32L);
				case 'f':
					return jjMoveStringLiteralDfa6_0(active0, 9007199254740992L, active1, 0L);
				case 'g':
					return jjMoveStringLiteralDfa6_0(active0, 576531121047601152L, active1, 0L);
				case 'i':
					return jjMoveStringLiteralDfa6_0(active0, 19175482788413440L, active1, 0L);
				case 'l':
					if ((active0 & 68719476736L) != 0L) {
						return jjStartNfaWithStates_0(5, 36, 7);
					}
					break;
				case 'o':
					return jjMoveStringLiteralDfa6_0(active0, 0L, active1, 1L);
				case 'r':
					return jjMoveStringLiteralDfa6_0(active0, 8070450532247928832L, active1, 0L);
				case 's':
					return jjMoveStringLiteralDfa6_0(active0, 180143985094819840L, active1, 0L);
				case 't':
					return jjMoveStringLiteralDfa6_0(active0, 145135534866432L, active1, 24L);
				case 'u':
					return jjMoveStringLiteralDfa6_0(active0, 0L, active1, 2L);
			}

			return jjStartNfa_0(4, active0, active1);
		}
	}

	private static int jjMoveNfa_1(int startState, int curPos) {
		int startsAt = 0;
		jjnewStateCnt = 3;
		int stateIndex = 1;
		jjstateSet[0] = startState;
		int matchedKind = Integer.MAX_VALUE;

		while (true) {
			if (++jjround == Integer.MAX_VALUE) {
				ReInitRounds();
			}

			if (curChar < '@') {
				long lowCharMask = 1L << curChar;

				do {
					stateIndex--;
					switch (jjstateSet[stateIndex]) {
						case 0:
							if ((13312L & lowCharMask) != 0L && matchedKind > 9) {
								matchedKind = 9;
							}

							if (curChar == '\r') {
								jjstateSet[jjnewStateCnt++] = 1;
							}
							break;
						case 1:
							if (curChar == '\n' && matchedKind > 9) {
								matchedKind = 9;
							}
							break;
						case 2:
							if (curChar == '\r') {
								jjstateSet[jjnewStateCnt++] = 1;
							}
					}
				} while (stateIndex != startsAt);
			} else if (curChar < 128) {
				long midCharMask = 1L << (curChar & '?');

				do {
					int currentState = jjstateSet[--stateIndex];
				} while (stateIndex != startsAt);
			} else {
				int highByte = curChar >> '\b';
				int byteIndex = highByte >> 6;
				long highMask = 1L << (highByte & 63);
				byteIndex = (curChar & 255) >> 6;
				long lowMask = 1L << (curChar & '?');

				do {
					int wideCharState = jjstateSet[--stateIndex];
				} while (stateIndex != startsAt);
			}

			if (matchedKind != Integer.MAX_VALUE) {
				jjmatchedKind = matchedKind;
				jjmatchedPos = curPos;
				matchedKind = Integer.MAX_VALUE;
			}

			curPos++;
			stateIndex = jjnewStateCnt;
			int newStateCount = jjnewStateCnt;
			jjnewStateCnt = startsAt;
			if (newStateCount == (startsAt = 3 - startsAt)) {
				return curPos;
			}

			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				return curPos;
			}
		}
	}

	private static int jjMoveStringLiteralDfa10_0(long old0, long active0, long old1, long active1) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1)) == 0L) {
			return jjStartNfa_0(8, old0, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(9, active0, active1);
				return 10;
			}

			switch (curChar) {
				case 'e':
					return jjMoveStringLiteralDfa11_0(active0, -6917529027641081856L, active1, 64L);
				case 'f':
					return jjMoveStringLiteralDfa11_0(active0, 576460752303423488L, active1, 0L);
				case 'g':
				case 'h':
				case 'i':
				case 'j':
				case 'k':
				case 'p':
				case 'q':
				case 'r':
				default:
					return jjStartNfa_0(9, active0, active1);
				case 'l':
					return jjMoveStringLiteralDfa11_0(active0, 0L, active1, 4L);
				case 'm':
					return jjMoveStringLiteralDfa11_0(active0, 0L, active1, 128L);
				case 'n':
					return jjMoveStringLiteralDfa11_0(active0, 0L, active1, 1L);
				case 'o':
					return jjMoveStringLiteralDfa11_0(active0, 0L, active1, 24L);
				case 's':
					return jjMoveStringLiteralDfa11_0(active0, 5764607523034234880L, active1, 2L);
				case 't':
					return jjMoveStringLiteralDfa11_0(active0, 0L, active1, 32L);
			}
		}
	}

	private static int jjMoveStringLiteralDfa20_0(long old1, long active1) {
		if ((active1 = active1 & old1) == 0L) {
			return jjStartNfa_0(18, 0L, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(19, 0L, active1);
				return 20;
			}

			switch (curChar) {
				case 'e':
					return jjMoveStringLiteralDfa21_0(active1, 128L);
				case 'l':
					return jjMoveStringLiteralDfa21_0(active1, 64L);
				default:
					return jjStartNfa_0(19, 0L, active1);
			}
		}
	}

	private static int jjMoveStringLiteralDfa27_0(long old1, long active1) {
		if ((active1 = active1 & old1) == 0L) {
			return jjStartNfa_0(25, 0L, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(26, 0L, active1);
				return 27;
			}

			switch (curChar) {
				case 's':
					return jjMoveStringLiteralDfa28_0(active1, 128L);
				default:
					return jjStartNfa_0(26, 0L, active1);
			}
		}
	}

	private static int jjMoveStringLiteralDfa0_0() {
		switch (curChar) {
			case '"':
				return jjStopAtPos(0, 72);
			case '(':
				return jjStopAtPos(0, 14);
			case ')':
				return jjStopAtPos(0, 17);
			case '*':
				return jjStopAtPos(0, 23);
			case ',':
				return jjStopAtPos(0, 20);
			case '.':
				return jjStopAtPos(0, 22);
			case '/':
				jjmatchedKind = 21;
				return jjMoveStringLiteralDfa1_0(320L, 0L);
			case ':':
				jjmatchedKind = 15;
				return jjMoveStringLiteralDfa1_0(65536L, 0L);
			case '<':
				return jjMoveStringLiteralDfa1_0(35201551958016L, 0L);
			case '=':
				return jjMoveStringLiteralDfa1_0(16777216L, 0L);
			case 'A':
				return jjMoveStringLiteralDfa1_0(0L, 32L);
			case 'C':
				return jjMoveStringLiteralDfa1_0(34359738368L, 0L);
			case 'D':
				return jjMoveStringLiteralDfa1_0(536870912L, 0L);
			case 'F':
				return jjMoveStringLiteralDfa1_0(4647714815446351872L, 0L);
			case 'L':
				return jjMoveStringLiteralDfa1_0(0L, 2L);
			case 'M':
				return jjMoveStringLiteralDfa1_0(-7998390739186745344L, 128L);
			case 'N':
				return jjMoveStringLiteralDfa1_0(0L, 1L);
			case 'O':
				return jjMoveStringLiteralDfa1_0(0L, 64L);
			case 'P':
				return jjMoveStringLiteralDfa1_0(70368744177664L, 0L);
			case 'S':
				return jjMoveStringLiteralDfa1_0(17592186044416L, 24L);
			case 'T':
				return jjMoveStringLiteralDfa1_0(0L, 4L);
			case '[':
				return jjStopAtPos(0, 18);
			case ']':
				return jjStopAtPos(0, 19);
			case 'a':
				return jjMoveStringLiteralDfa1_0(432908514214543360L, 0L);
			case 'b':
				return jjMoveStringLiteralDfa1_0(137438953472L, 0L);
			case 'e':
				return jjMoveStringLiteralDfa1_0(294669183352832L, 0L);
			case 'f':
				return jjMoveStringLiteralDfa1_0(3221225472L, 0L);
			case 'h':
				return jjMoveStringLiteralDfa1_0(8589934592L, 0L);
			case 'i':
				return jjMoveStringLiteralDfa1_0(9007199254740992L, 0L);
			case 'l':
				return jjMoveStringLiteralDfa1_0(4294967296L, 0L);
			case 'n':
				return jjMoveStringLiteralDfa1_0(1168365322240L, 0L);
			case 'p':
				return jjMoveStringLiteralDfa1_0(578853564483371008L, 0L);
			case 's':
				return jjMoveStringLiteralDfa1_0(2310347158596878336L, 0L);
			case 't':
				return jjMoveStringLiteralDfa1_0(18014398777917440L, 0L);
			case 'v':
				return jjMoveStringLiteralDfa1_0(1125899906842624L, 0L);
			case '\ufeff':
				return jjStartNfaWithStates_0(0, 13, 6);
			default:
				return jjMoveNfa_0(3, 0);
		}
	}

	private static int jjStopAtPos(int pos, int kind) {
		jjmatchedKind = kind;
		jjmatchedPos = pos;
		return pos + 1;
	}

	private static int jjMoveStringLiteralDfa21_0(long old1, long active1) {
		if ((active1 = active1 & old1) == 0L) {
			return jjStartNfa_0(19, 0L, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(20, 0L, active1);
				return 21;
			}

			switch (curChar) {
				case 'C':
					return jjMoveStringLiteralDfa22_0(active1, 128L);
				case 'a':
					return jjMoveStringLiteralDfa22_0(active1, 64L);
				default:
					return jjStartNfa_0(20, 0L, active1);
			}
		}
	}

	protected static Token3 jjFillToken() {
		String literalImage = jjstrLiteralImages[jjmatchedKind];
		String tokenImage = literalImage == null ? SimpleCharStream3.GetImage() : literalImage;
		int beginLine = SimpleCharStream3.getBeginLine();
		int beginColumn = SimpleCharStream3.getBeginColumn();
		int endLine = SimpleCharStream3.getLine();
		int endColumn = SimpleCharStream3.getColumn();
		Token3 token = Token3.newToken(jjmatchedKind);
		token.kind = jjmatchedKind;
		token.image = tokenImage;
		token.beginLine = beginLine;
		token.endLine = endLine;
		token.beginColumn = beginColumn;
		token.endColumn = endColumn;
		return token;
	}

	private static void ReInitRounds() {
		jjround = -2147483647;
		int i = 7;

		while (i-- > 0) {
			jjrounds[i] = Integer.MIN_VALUE;
		}
	}

	private static int jjMoveStringLiteralDfa22_0(long old1, long active1) {
		if ((active1 = active1 & old1) == 0L) {
			return jjStartNfa_0(20, 0L, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(21, 0L, active1);
				return 22;
			}

			switch (curChar) {
				case 'l':
					return jjMoveStringLiteralDfa23_0(active1, 128L);
				case 's':
					return jjMoveStringLiteralDfa23_0(active1, 64L);
				default:
					return jjStartNfa_0(21, 0L, active1);
			}
		}
	}

	public static Token3 getNextToken() {
		Token3 specialToken = null;
		int curPos = 0;

		label129:
		while (true) {
			try {
				curChar = SimpleCharStream3.BeginToken();
			} catch (IOException ignoredBeginToken) {
				jjmatchedKind = 0;
				Token3 eofToken = jjFillToken();
				eofToken.specialToken = specialToken;
				return eofToken;
			}

			image = jjimage;
			image.setLength(0);
			jjimageLen = 0;

			while (true) {
				switch (curLexState) {
					case 0:
						try {
							SimpleCharStream3.backup(0);

							while (curChar <= ' ' && (4294981120L & 1L << curChar) != 0L) {
								curChar = SimpleCharStream3.BeginToken();
							}
						} catch (IOException ignoredWhitespaceSkip) {
							continue label129;
						}

						jjmatchedKind = Integer.MAX_VALUE;
						jjmatchedPos = 0;
						curPos = jjMoveStringLiteralDfa0_0();
						break;
					case 1:
						jjmatchedKind = Integer.MAX_VALUE;
						jjmatchedPos = 0;
						curPos = jjMoveStringLiteralDfa0_1();
						if (jjmatchedPos == 0 && jjmatchedKind > 12) {
							jjmatchedKind = 12;
						}
						break;
					case 2:
						jjmatchedKind = Integer.MAX_VALUE;
						jjmatchedPos = 0;
						curPos = jjMoveStringLiteralDfa0_2();
						if (jjmatchedPos == 0 && jjmatchedKind > 12) {
							jjmatchedKind = 12;
						}
						break;
					case 3:
						jjmatchedKind = Integer.MAX_VALUE;
						jjmatchedPos = 0;
						curPos = jjMoveStringLiteralDfa0_3();
						if (jjmatchedPos == 0 && jjmatchedKind > 12) {
							jjmatchedKind = 12;
						}
						break;
					case 4:
						jjmatchedKind = Integer.MAX_VALUE;
						jjmatchedPos = 0;
						curPos = jjMoveStringLiteralDfa0_4();
				}

				if (jjmatchedKind == Integer.MAX_VALUE) {
					break label129;
				}

				if (jjmatchedPos + 1 < curPos) {
					SimpleCharStream3.backup(curPos - jjmatchedPos - 1);
				}

				if ((jjtoToken[jjmatchedKind >> 6] & 1L << (jjmatchedKind & 63)) != 0L) {
					Token3 matchedToken = jjFillToken();
					matchedToken.specialToken = specialToken;
					if (jjnewLexState[jjmatchedKind] != -1) {
						curLexState = jjnewLexState[jjmatchedKind];
					}

					return matchedToken;
				}

				if ((jjtoSkip[jjmatchedKind >> 6] & 1L << (jjmatchedKind & 63)) != 0L) {
					if ((jjtoSpecial[jjmatchedKind >> 6] & 1L << (jjmatchedKind & 63)) != 0L) {
						Token3 newSpecialToken = jjFillToken();
						if (specialToken == null) {
							specialToken = newSpecialToken;
						} else {
							newSpecialToken.specialToken = specialToken;
							specialToken = specialToken.next = newSpecialToken;
						}

						SkipLexicalActions(newSpecialToken);
					} else {
						SkipLexicalActions(null);
					}

					if (jjnewLexState[jjmatchedKind] != -1) {
						curLexState = jjnewLexState[jjmatchedKind];
					}
					break;
				}

				MoreLexicalActions();
				if (jjnewLexState[jjmatchedKind] != -1) {
					curLexState = jjnewLexState[jjmatchedKind];
				}

				curPos = 0;
				jjmatchedKind = Integer.MAX_VALUE;

				try {
					curChar = SimpleCharStream3.readChar();
				} catch (IOException ignoredReadChar) {
					break label129;
				}
			}
		}

		int errorLine = SimpleCharStream3.getLine();
		int errorColumn = SimpleCharStream3.getColumn();
		String errorAfter = null;
		boolean eofSeen = false;

		try {
			SimpleCharStream3.readChar();
			SimpleCharStream3.backup(1);
		} catch (IOException ioException) {
			eofSeen = true;
			errorAfter = curPos <= 1 ? "" : SimpleCharStream3.GetImage();
			if (curChar != '\n' && curChar != '\r') {
				errorColumn++;
			} else {
				errorLine++;
				errorColumn = 0;
			}
		}

		if (!eofSeen) {
			SimpleCharStream3.backup(1);
			errorAfter = curPos <= 1 ? "" : SimpleCharStream3.GetImage();
		}

		throw new TokenMgrError(eofSeen, curLexState, errorLine, errorColumn, errorAfter, curChar, 0);
	}

	private static int jjMoveStringLiteralDfa0_1() {
		return jjMoveNfa_1(0, 0);
	}

	private static final boolean jjCanMove_1(int highByte, int highIndex, int lowIndex, long highMask, long lowMask) {
		switch (highByte) {
			case 0:
				return (jjbitVec0[lowIndex] & lowMask) != 0L;
			default:
				return (jjbitVec1[highIndex] & highMask) != 0L;
		}
	}

	private static int jjMoveStringLiteralDfa11_0(long old0, long active0, long old1, long active1) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1)) == 0L) {
			return jjStartNfa_0(9, old0, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(10, active0, active1);
				return 11;
			}

			switch (curChar) {
				case ':':
					if ((active0 & 1152921504606846976L) != 0L) {
						return jjStopAtPos(11, 60);
					}
					break;
				case 'O':
					return jjMoveStringLiteralDfa12_0(active0, 0L, active1, 2L);
				case 'a':
					return jjMoveStringLiteralDfa12_0(active0, 0L, active1, 4L);
				case 'd':
					if ((active0 & 2305843009213693952L) != 0L) {
						return jjStartNfaWithStates_0(11, 61, 7);
					}

					return jjMoveStringLiteralDfa12_0(active0, Long.MIN_VALUE, active1, 0L);
				case 'e':
					return jjMoveStringLiteralDfa12_0(active0, 0L, active1, 128L);
				case 'f':
					return jjMoveStringLiteralDfa12_0(active0, 0L, active1, 64L);
				case 'g':
					return jjMoveStringLiteralDfa12_0(active0, 0L, active1, 1L);
				case 'i':
					return jjMoveStringLiteralDfa12_0(active0, 0L, active1, 32L);
				case 'o':
					if ((active0 & 576460752303423488L) != 0L) {
						return jjStartNfaWithStates_0(11, 59, 6);
					}
					break;
				case 's':
					return jjMoveStringLiteralDfa12_0(active0, 4611686018427387904L, active1, 0L);
				case 't':
					return jjMoveStringLiteralDfa12_0(active0, 0L, active1, 24L);
			}

			return jjStartNfa_0(10, active0, active1);
		}
	}

	private static int jjMoveStringLiteralDfa16_0(long old1, long active1) {
		if ((active1 = active1 & old1) == 0L) {
			return jjStartNfa_0(14, 0L, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(15, 0L, active1);
				return 16;
			}

			switch (curChar) {
				case 'a':
					return jjMoveStringLiteralDfa17_0(active1, 32L);
				case 'b':
				case 'd':
				case 'e':
				case 'f':
				default:
					return jjStartNfa_0(15, 0L, active1);
				case 'c':
					return jjMoveStringLiteralDfa17_0(active1, 64L);
				case 'g':
					return jjMoveStringLiteralDfa17_0(active1, 24L);
				case 'h':
					return jjMoveStringLiteralDfa17_0(active1, 128L);
			}
		}
	}

	private static int jjMoveStringLiteralDfa3_0(long old0, long active0, long old1, long active1) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1)) == 0L) {
			return jjStartNfa_0(1, old0, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(2, active0, active1);
				return 3;
			}

			switch (curChar) {
				case 'a':
					return jjMoveStringLiteralDfa4_0(active0, 1407376494166016L, active1, 0L);
				case 'b':
					return jjMoveStringLiteralDfa4_0(active0, 1152921504606846976L, active1, 0L);
				case 'c':
					return jjMoveStringLiteralDfa4_0(active0, 2305847407260205056L, active1, 4L);
				case 'd':
					return jjMoveStringLiteralDfa4_0(active0, 137438953472L, active1, 0L);
				case 'e':
					if ((active0 & 134217728L) != 0L) {
						return jjStartNfaWithStates_0(3, 27, 7);
					} else {
						if ((active0 & 268435456L) != 0L) {
							return jjStartNfaWithStates_0(3, 28, 7);
						}

						return jjMoveStringLiteralDfa4_0(active0, 9007199254740992L, active1, 3L);
					}
				case 'f':
				case 'g':
				case 'j':
				case 'p':
				case 'q':
				default:
					return jjStartNfa_0(2, active0, active1);
				case 'h':
					return jjMoveStringLiteralDfa4_0(active0, 72057598332895232L, active1, 128L);
				case 'i':
					return jjMoveStringLiteralDfa4_0(active0, 36301063585792L, active1, 0L);
				case 'k':
					return jjMoveStringLiteralDfa4_0(active0, 576531121047601152L, active1, 0L);
				case 'l':
					return jjMoveStringLiteralDfa4_0(active0, 36037867989893120L, active1, 0L);
				case 'm':
					if ((active0 & 67108864L) != 0L) {
						return jjStartNfaWithStates_0(3, 26, 7);
					}

					return jjMoveStringLiteralDfa4_0(active0, 68719476736L, active1, 0L);
				case 'n':
					return jjMoveStringLiteralDfa4_0(active0, 18014398509481984L, active1, 24L);
				case 'o':
					return jjMoveStringLiteralDfa4_0(active0, 288230376151711744L, active1, 32L);
				case 'r':
					return jjMoveStringLiteralDfa4_0(active0, 144132780261900288L, active1, 0L);
				case 's':
					return jjMoveStringLiteralDfa4_0(active0, 36507222016L, active1, 0L);
				case 't':
					return jjMoveStringLiteralDfa4_0(active0, 7318899150290944L, active1, 0L);
				case 'u':
					return jjMoveStringLiteralDfa4_0(active0, -9223369837831520256L, active1, 64L);
				case 'v':
					return jjMoveStringLiteralDfa4_0(active0, 140746078289920L, active1, 0L);
				case 'w':
					return jjMoveStringLiteralDfa4_0(active0, 4611686018427387904L, active1, 0L);
			}
		}
	}

	private static int jjMoveStringLiteralDfa25_0(long old1, long active1) {
		if ((active1 = active1 & old1) == 0L) {
			return jjStartNfa_0(23, 0L, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(24, 0L, active1);
				return 25;
			}

			switch (curChar) {
				case 's':
					return jjMoveStringLiteralDfa26_0(active1, 128L);
				default:
					return jjStartNfa_0(24, 0L, active1);
			}
		}
	}

	private static int jjMoveStringLiteralDfa14_0(long old1, long active1) {
		if ((active1 = active1 & old1) == 0L) {
			return jjStartNfa_0(12, 0L, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(13, 0L, active1);
				return 14;
			}

			switch (curChar) {
				case ':':
					if ((active1 & 4L) != 0L) {
						return jjStopAtPos(14, 66);
					}
				default:
					return jjStartNfa_0(13, 0L, active1);
				case 'C':
					return jjMoveStringLiteralDfa15_0(active1, 32L);
				case 'a':
					return jjMoveStringLiteralDfa15_0(active1, 24L);
				case 'e':
					return jjMoveStringLiteralDfa15_0(active1, 64L);
				case 'r':
					return jjMoveStringLiteralDfa15_0(active1, 128L);
			}
		}
	}

	private static void jjCheckNAdd(int state) {
		if (jjrounds[state] != jjround) {
			jjstateSet[jjnewStateCnt++] = state;
			jjrounds[state] = jjround;
		}
	}

	private static int jjStartNfaWithStates_0(int pos, int kind, int state) {
		jjmatchedKind = kind;
		jjmatchedPos = pos;

		try {
			curChar = SimpleCharStream3.readChar();
		} catch (IOException ignored) {
			return pos + 1;
		}

		return jjMoveNfa_0(state, pos + 1);
	}

	private static int jjMoveStringLiteralDfa13_0(long old0, long active0, long old1, long active1) {
		long maskedActive0 = active0 & old0;
		long unusedActive0Mask = active0 & old0;
		if ((maskedActive0 | (active1 = active1 & old1)) == 0L) {
			return jjStartNfa_0(11, old0, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(12, 0L, active1);
				return 13;
			}

			switch (curChar) {
				case ':':
					if ((active1 & 2L) != 0L) {
						return jjStopAtPos(13, 65);
					}
					break;
				case 'd':
					if ((active1 & 1L) != 0L) {
						return jjStartNfaWithStates_0(13, 64, 7);
					}
					break;
				case 'e':
					return jjMoveStringLiteralDfa14_0(active1, 128L);
				case 'h':
					return jjMoveStringLiteralDfa14_0(active1, 24L);
				case 'n':
					return jjMoveStringLiteralDfa14_0(active1, 32L);
				case 'r':
					return jjMoveStringLiteralDfa14_0(active1, 64L);
				case 's':
					return jjMoveStringLiteralDfa14_0(active1, 4L);
			}

			return jjStartNfa_0(12, 0L, active1);
		}
	}

	public static void SkipLexicalActions(Token3 matchedToken) {
	}

	private static int jjMoveStringLiteralDfa1_0(long active0, long active1) {
		try {
			curChar = SimpleCharStream3.readChar();
		} catch (IOException ignored) {
			jjStopStringLiteralDfa_0(0, active0, active1);
			return 1;
		}

		switch (curChar) {
			case '*':
				if ((active0 & 256L) != 0L) {
					return jjStartNfaWithStates_0(1, 8, 0);
				}
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
			case ';':
			case '<':
			case '=':
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
			case 'd':
			case 'f':
			case 'h':
			case 'j':
			case 'k':
			case 'm':
			case 'p':
			case 'q':
			case 's':
			case 'v':
			case 'w':
			default:
				break;
			case '/':
				if ((active0 & 64L) != 0L) {
					return jjStopAtPos(1, 6);
				}
				break;
			case ':':
				if ((active0 & 65536L) != 0L) {
					return jjStopAtPos(1, 16);
				}
				break;
			case '>':
				if ((active0 & 16777216L) != 0L) {
					return jjStopAtPos(1, 24);
				}
				break;
			case 'a':
				return jjMoveStringLiteralDfa2_0(active0, -8646839813611192320L, active1, 1L);
			case 'b':
				return jjMoveStringLiteralDfa2_0(active0, 562949953421312L, active1, 64L);
			case 'c':
				return jjMoveStringLiteralDfa2_0(active0, 35184372088832L, active1, 0L);
			case 'e':
				return jjMoveStringLiteralDfa2_0(active0, 1224979107234709504L, active1, 128L);
			case 'g':
				return jjMoveStringLiteralDfa2_0(active0, 144115188075855872L, active1, 0L);
			case 'i':
				return jjMoveStringLiteralDfa2_0(active0, 36028819567542272L, active1, 26L);
			case 'l':
				return jjMoveStringLiteralDfa2_0(active0, 34359738368L, active1, 0L);
			case 'n':
				return jjMoveStringLiteralDfa2_0(active0, 297519050483826688L, active1, 0L);
			case 'o':
				return jjMoveStringLiteralDfa2_0(active0, 4612831778397224960L, active1, 0L);
			case 'r':
				return jjMoveStringLiteralDfa2_0(active0, 20407073518911488L, active1, 4L);
			case 't':
				return jjMoveStringLiteralDfa2_0(active0, 549755813888L, active1, 0L);
			case 'u':
				return jjMoveStringLiteralDfa2_0(active0, 274877906944L, active1, 32L);
			case 'x':
				return jjMoveStringLiteralDfa2_0(active0, 13194139533312L, active1, 0L);
			case 'y':
				return jjMoveStringLiteralDfa2_0(active0, 2310346608841064448L, active1, 0L);
		}

		return jjStartNfa_0(0, active0, active1);
	}

	private static int jjMoveNfa_4(int startState, int curPos) {
		int startsAt = 0;
		jjnewStateCnt = 3;
		int stateIndex = 1;
		jjstateSet[0] = startState;
		int matchedKind = Integer.MAX_VALUE;

		while (true) {
			if (++jjround == Integer.MAX_VALUE) {
				ReInitRounds();
			}

			if (curChar < '@') {
				long lowCharMask = 1L << curChar;

				do {
					stateIndex--;
					switch (jjstateSet[stateIndex]) {
						case 0:
							if ((-17179882497L & lowCharMask) != 0L) {
								if (matchedKind > 74) {
									matchedKind = 74;
								}
							} else if (curChar == '"') {
								jjstateSet[jjnewStateCnt++] = 2;
							}
							break;
						case 1:
							if (curChar == '"') {
								jjstateSet[jjnewStateCnt++] = 2;
							}
							break;
						case 2:
							if (curChar == '"' && matchedKind > 74) {
								matchedKind = 74;
							}
					}
				} while (stateIndex != startsAt);
			} else if (curChar < 128) {
				long midCharMask = 1L << (curChar & '?');

				do {
					stateIndex--;
					switch (jjstateSet[stateIndex]) {
						case 0:
							matchedKind = 74;
					}
				} while (stateIndex != startsAt);
			} else {
				int highByte = curChar >> '\b';
				int highIndex = highByte >> 6;
				long highMask = 1L << (highByte & 63);
				int lowIndex = (curChar & 255) >> 6;
				long lowMask = 1L << (curChar & '?');

				do {
					stateIndex--;
					switch (jjstateSet[stateIndex]) {
						case 0:
							if (jjCanMove_1(highByte, highIndex, lowIndex, highMask, lowMask) && matchedKind > 74) {
								matchedKind = 74;
							}
					}
				} while (stateIndex != startsAt);
			}

			if (matchedKind != Integer.MAX_VALUE) {
				jjmatchedKind = matchedKind;
				jjmatchedPos = curPos;
				matchedKind = Integer.MAX_VALUE;
			}

			curPos++;
			stateIndex = jjnewStateCnt;
			int newStateCount = jjnewStateCnt;
			jjnewStateCnt = startsAt;
			if (newStateCount == (startsAt = 3 - startsAt)) {
				return curPos;
			}

			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				return curPos;
			}
		}
	}

	public static void MoreLexicalActions() {
		jjimageLen = jjimageLen + (lengthOfMatch = jjmatchedPos + 1);
		switch (jjmatchedKind) {
			case 7:
				image.append(SimpleCharStream3.GetSuffix(jjimageLen));
				jjimageLen = 0;
				SimpleCharStream3.backup(1);
		}
	}

	private static int jjMoveStringLiteralDfa26_0(long old1, long active1) {
		if ((active1 = active1 & old1) == 0L) {
			return jjStartNfa_0(24, 0L, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(25, 0L, active1);
				return 26;
			}

			switch (curChar) {
				case 'e':
					return jjMoveStringLiteralDfa27_0(active1, 128L);
				default:
					return jjStartNfa_0(25, 0L, active1);
			}
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

	private static int jjMoveStringLiteralDfa0_3() {
		switch (curChar) {
			case '*':
				return jjMoveStringLiteralDfa1_3(2048L);
			default:
				return 1;
		}
	}

	private static int jjMoveNfa_0(int startState, int curPos) {
		int startsAt = 0;
		jjnewStateCnt = 7;
		int stateIndex = 1;
		jjstateSet[0] = startState;
		int matchedKind = Integer.MAX_VALUE;

		while (true) {
			if (++jjround == Integer.MAX_VALUE) {
				ReInitRounds();
			}

			if (curChar < '@') {
				long lowCharMask = 1L << curChar;

				do {
					stateIndex--;
					switch (jjstateSet[stateIndex]) {
						case 0:
							if (curChar == '*') {
								jjstateSet[jjnewStateCnt++] = 1;
							}
							break;
						case 1:
							if ((-140737488355329L & lowCharMask) != 0L && matchedKind > 7) {
								matchedKind = 7;
							}
							break;
						case 2:
							if (curChar == '*') {
								jjstateSet[jjnewStateCnt++] = 0;
							}
							break;
						case 3:
							if ((287992950628008447L & lowCharMask) != 0L) {
								if (matchedKind > 77) {
									matchedKind = 77;
								}

								jjCheckNAdd(6);
							} else if (curChar == '/') {
								jjstateSet[jjnewStateCnt++] = 2;
							}

							if ((287948901175001088L & lowCharMask) != 0L) {
								if (matchedKind > 76) {
									matchedKind = 76;
								}

								jjCheckNAdd(5);
							}

							if ((287948901175001088L & lowCharMask) != 0L) {
								if (matchedKind > 75) {
									matchedKind = 75;
								}

								jjCheckNAdd(4);
							}
							break;
						case 4:
							if ((287948901175001088L & lowCharMask) != 0L) {
								if (matchedKind > 75) {
									matchedKind = 75;
								}

								jjCheckNAdd(4);
							}
							break;
						case 5:
							if ((287948901175001088L & lowCharMask) != 0L) {
								if (matchedKind > 76) {
									matchedKind = 76;
								}

								jjCheckNAdd(5);
							}
							break;
						case 6:
							if ((287992950628008447L & lowCharMask) != 0L) {
								if (matchedKind > 77) {
									matchedKind = 77;
								}

								jjCheckNAdd(6);
							}
							break;
						case 7:
							if ((287992950628008447L & lowCharMask) != 0L) {
								if (matchedKind > 77) {
									matchedKind = 77;
								}

								jjCheckNAdd(6);
							}

							if ((287948901175001088L & lowCharMask) != 0L) {
								if (matchedKind > 76) {
									matchedKind = 76;
								}

								jjCheckNAdd(5);
							}
					}
				} while (stateIndex != startsAt);
			} else if (curChar < 128) {
				long midCharMask = 1L << (curChar & '?');

				do {
					stateIndex--;
					switch (jjstateSet[stateIndex]) {
						case 1:
							if (matchedKind > 7) {
								matchedKind = 7;
							}
						case 2:
						case 4:
						default:
							break;
						case 3:
							if ((-8646911290859585538L & midCharMask) != 0L) {
								if (matchedKind > 77) {
									matchedKind = 77;
								}

								jjCheckNAdd(6);
							}

							if ((576460743847706622L & midCharMask) != 0L) {
								if (matchedKind > 76) {
									matchedKind = 76;
								}

								jjCheckNAdd(5);
							}
							break;
						case 5:
							if ((576460743847706622L & midCharMask) != 0L) {
								if (matchedKind > 76) {
									matchedKind = 76;
								}

								jjCheckNAdd(5);
							}
							break;
						case 6:
							if ((-8646911290859585538L & midCharMask) != 0L) {
								if (matchedKind > 77) {
									matchedKind = 77;
								}

								jjCheckNAdd(6);
							}
							break;
						case 7:
							if ((-8646911290859585538L & midCharMask) != 0L) {
								if (matchedKind > 77) {
									matchedKind = 77;
								}

								jjCheckNAdd(6);
							}

							if ((576460743847706622L & midCharMask) != 0L) {
								if (matchedKind > 76) {
									matchedKind = 76;
								}

								jjCheckNAdd(5);
							}
					}
				} while (stateIndex != startsAt);
			} else {
				int highByte = curChar >> '\b';
				int highIndex = highByte >> 6;
				long highMask = 1L << (highByte & 63);
				int lowIndex = (curChar & 255) >> 6;
				long lowMask = 1L << (curChar & '?');

				do {
					stateIndex--;
					switch (jjstateSet[stateIndex]) {
						case 1:
							if (jjCanMove_0(highByte, highIndex, lowIndex, highMask, lowMask) && matchedKind > 7) {
								matchedKind = 7;
							}
						case 2:
						case 4:
						case 5:
						default:
							break;
						case 3:
						case 6:
							if (jjCanMove_1(highByte, highIndex, lowIndex, highMask, lowMask)) {
								if (matchedKind > 77) {
									matchedKind = 77;
								}

								jjCheckNAdd(6);
							}
							break;
						case 7:
							if (jjCanMove_1(highByte, highIndex, lowIndex, highMask, lowMask)) {
								if (matchedKind > 77) {
									matchedKind = 77;
								}

								jjCheckNAdd(6);
							}
					}
				} while (stateIndex != startsAt);
			}

			if (matchedKind != Integer.MAX_VALUE) {
				jjmatchedKind = matchedKind;
				jjmatchedPos = curPos;
				matchedKind = Integer.MAX_VALUE;
			}

			curPos++;
			stateIndex = jjnewStateCnt;
			int newStateCount = jjnewStateCnt;
			jjnewStateCnt = startsAt;
			if (newStateCount == (startsAt = 7 - startsAt)) {
				return curPos;
			}

			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				return curPos;
			}
		}
	}

	private static int jjMoveStringLiteralDfa0_2() {
		switch (curChar) {
			case '*':
				return jjMoveStringLiteralDfa1_2(1024L);
			default:
				return 1;
		}
	}

	public static void ReInit(SimpleCharStream3 stream) {
		jjnewStateCnt = 0;
		jjmatchedPos = 0;
		curLexState = defaultLexState;
		input_stream = stream;
		ReInitRounds();
	}

	private static int jjMoveStringLiteralDfa1_3(long active0) {
		try {
			curChar = SimpleCharStream3.readChar();
		} catch (IOException ignored) {
			return 1;
		}

		switch (curChar) {
			case '/':
				if ((active0 & 2048L) != 0L) {
					return jjStopAtPos(1, 11);
				}

				return 2;
			default:
				return 2;
		}
	}

	private static int jjMoveStringLiteralDfa23_0(long old1, long active1) {
		if ((active1 = active1 & old1) == 0L) {
			return jjStartNfa_0(21, 0L, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(22, 0L, active1);
				return 23;
			}

			switch (curChar) {
				case 'a':
					return jjMoveStringLiteralDfa24_0(active1, 128L);
				case 's':
					return jjMoveStringLiteralDfa24_0(active1, 64L);
				default:
					return jjStartNfa_0(22, 0L, active1);
			}
		}
	}

	private static final int jjStartNfa_0(int pos, long active0, long active1) {
		return jjMoveNfa_0(jjStopStringLiteralDfa_0(pos, active0, active1), pos + 1);
	}

	private static int jjStartNfaWithStates_4(int pos, int kind, int state) {
		jjmatchedKind = kind;
		jjmatchedPos = pos;

		try {
			curChar = SimpleCharStream3.readChar();
		} catch (IOException ignored) {
			return pos + 1;
		}

		return jjMoveNfa_4(state, pos + 1);
	}

	private static int jjMoveStringLiteralDfa1_2(long active0) {
		try {
			curChar = SimpleCharStream3.readChar();
		} catch (IOException ignored) {
			return 1;
		}

		switch (curChar) {
			case '/':
				if ((active0 & 1024L) != 0L) {
					return jjStopAtPos(1, 10);
				}

				return 2;
			default:
				return 2;
		}
	}

	private static int jjMoveStringLiteralDfa15_0(long old1, long active1) {
		if ((active1 = active1 & old1) == 0L) {
			return jjStartNfa_0(13, 0L, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(14, 0L, active1);
				return 15;
			}

			switch (curChar) {
				case 'C':
					return jjMoveStringLiteralDfa16_0(active1, 128L);
				case 'l':
					return jjMoveStringLiteralDfa16_0(active1, 32L);
				case 'n':
					return jjMoveStringLiteralDfa16_0(active1, 88L);
				default:
					return jjStartNfa_0(14, 0L, active1);
			}
		}
	}

	private static int jjMoveStringLiteralDfa12_0(long old0, long active0, long old1, long active1) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1)) == 0L) {
			return jjStartNfa_0(10, old0, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(11, active0, active1);
				return 12;
			}

			switch (curChar) {
				case ':':
					if ((active0 & 4611686018427387904L) != 0L) {
						return jjStopAtPos(12, 62);
					} else if ((active0 & Long.MIN_VALUE) != 0L) {
						return jjStopAtPos(12, 63);
					}
				default:
					return jjStartNfa_0(11, active0, active1);
				case 'C':
					return jjMoveStringLiteralDfa13_0(active0, 0L, active1, 24L);
				case 'e':
					return jjMoveStringLiteralDfa13_0(active0, 0L, active1, 65L);
				case 'f':
					return jjMoveStringLiteralDfa13_0(active0, 0L, active1, 2L);
				case 'o':
					return jjMoveStringLiteralDfa13_0(active0, 0L, active1, 32L);
				case 's':
					return jjMoveStringLiteralDfa13_0(active0, 0L, active1, 4L);
				case 't':
					return jjMoveStringLiteralDfa13_0(active0, 0L, active1, 128L);
			}
		}
	}

	private static int jjMoveStringLiteralDfa4_0(long old0, long active0, long old1, long active1) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1)) == 0L) {
			return jjStartNfa_0(2, old0, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(3, active0, active1);
				return 4;
			}

			switch (curChar) {
				case ':':
					if ((active0 & 536870912L) != 0L) {
						return jjStopAtPos(4, 29);
					}
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
				case 'O':
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
				case '_':
				case '`':
				case 'b':
				case 'j':
				case 'k':
				case 'm':
				case 'p':
				case 'q':
				case 'w':
				case 'x':
				default:
					break;
				case 'N':
					return jjMoveStringLiteralDfa5_0(active0, 0L, active1, 3L);
				case 'R':
					return jjMoveStringLiteralDfa5_0(active0, 0L, active1, 32L);
				case 'a':
					return jjMoveStringLiteralDfa5_0(active0, 5188357945682821120L, active1, 24L);
				case 'c':
					return jjMoveStringLiteralDfa5_0(active0, 17592186044416L, active1, 0L);
				case 'd':
					return jjMoveStringLiteralDfa5_0(active0, 36028797018963968L, active1, 0L);
				case 'e':
					if ((active0 & 2147483648L) != 0L) {
						return jjStartNfaWithStates_0(4, 31, 7);
					}

					return jjMoveStringLiteralDfa5_0(active0, 1299288492496388096L, active1, 4L);
				case 'f':
					return jjMoveStringLiteralDfa5_0(active0, Long.MIN_VALUE, active1, 0L);
				case 'g':
					return jjMoveStringLiteralDfa5_0(active0, 137438953472L, active1, 0L);
				case 'h':
					return jjMoveStringLiteralDfa5_0(active0, 2310346608841064448L, active1, 0L);
				case 'i':
					return jjMoveStringLiteralDfa5_0(active0, 824633720832L, active1, 0L);
				case 'l':
					if ((active0 & 1073741824L) != 0L) {
						return jjStartNfaWithStates_0(4, 30, 7);
					}

					return jjMoveStringLiteralDfa5_0(active0, 2199023255552L, active1, 0L);
				case 'n':
					return jjMoveStringLiteralDfa5_0(active0, 316659348799488L, active1, 0L);
				case 'o':
					return jjMoveStringLiteralDfa5_0(active0, 72057594037927936L, active1, 128L);
				case 'r':
					return jjMoveStringLiteralDfa5_0(active0, 9570149208162304L, active1, 0L);
				case 's':
					return jjMoveStringLiteralDfa5_0(active0, 18014432869220352L, active1, 64L);
				case 't':
					if ((active0 & 4294967296L) != 0L) {
						return jjStartNfaWithStates_0(4, 32, 7);
					}

					return jjMoveStringLiteralDfa5_0(active0, 289356293238423552L, active1, 0L);
				case 'u':
					return jjMoveStringLiteralDfa5_0(active0, 13194139533312L, active1, 0L);
				case 'v':
					return jjMoveStringLiteralDfa5_0(active0, 1099511627776L, active1, 0L);
				case 'y':
					if ((active0 & 8589934592L) != 0L) {
						return jjStartNfaWithStates_0(4, 33, 7);
					}
			}

			return jjStartNfa_0(3, active0, active1);
		}
	}

	private static int jjMoveStringLiteralDfa0_4() {
		switch (curChar) {
			case '"':
				return jjStartNfaWithStates_4(0, 73, 2);
			default:
				return jjMoveNfa_4(0, 0);
		}
	}

	private static int jjMoveStringLiteralDfa19_0(long old1, long active1) {
		if ((active1 = active1 & old1) == 0L) {
			return jjStartNfa_0(17, 0L, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(18, 0L, active1);
				return 19;
			}

			switch (curChar) {
				case ':':
					if ((active1 & 16L) != 0L) {
						return jjStopAtPos(19, 68);
					} else if ((active1 & 32L) != 0L) {
						return jjStopAtPos(19, 69);
					}
				default:
					return jjStartNfa_0(18, 0L, active1);
				case 'C':
					return jjMoveStringLiteralDfa20_0(active1, 64L);
				case 'g':
					return jjMoveStringLiteralDfa20_0(active1, 128L);
			}
		}
	}

	private static int jjMoveStringLiteralDfa17_0(long old1, long active1) {
		if ((active1 = active1 & old1) == 0L) {
			return jjStartNfa_0(15, 0L, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(16, 0L, active1);
				return 17;
			}

			switch (curChar) {
				case 'a':
					return jjMoveStringLiteralDfa18_0(active1, 128L);
				case 'e':
					return jjMoveStringLiteralDfa18_0(active1, 88L);
				case 's':
					return jjMoveStringLiteralDfa18_0(active1, 32L);
				default:
					return jjStartNfa_0(16, 0L, active1);
			}
		}
	}

	private static int jjMoveStringLiteralDfa9_0(long old0, long active0, long old1, long active1) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1)) == 0L) {
			return jjStartNfa_0(7, old0, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(8, active0, active1);
				return 9;
			}

			switch (curChar) {
				case ':':
					if ((active0 & 72057594037927936L) != 0L) {
						return jjStopAtPos(9, 56);
					}
					break;
				case 'C':
					return jjMoveStringLiteralDfa10_0(active0, 0L, active1, 4L);
				case 'N':
					return jjMoveStringLiteralDfa10_0(active0, 0L, active1, 24L);
				case 'R':
					return jjMoveStringLiteralDfa10_0(active0, 0L, active1, 64L);
				case 'a':
					return jjMoveStringLiteralDfa10_0(active0, 4611686018427387904L, active1, 129L);
				case 'c':
					return jjMoveStringLiteralDfa10_0(active0, 0L, active1, 32L);
				case 'e':
					if ((active0 & 144115188075855872L) != 0L) {
						return jjStartNfaWithStates_0(9, 57, 7);
					}
					break;
				case 'n':
					if ((active0 & 288230376151711744L) != 0L) {
						return jjStartNfaWithStates_0(9, 58, 7);
					}

					return jjMoveStringLiteralDfa10_0(active0, 576460752303423488L, active1, 0L);
				case 'r':
					return jjMoveStringLiteralDfa10_0(active0, Long.MIN_VALUE, active1, 2L);
				case 's':
					return jjMoveStringLiteralDfa10_0(active0, 1152921504606846976L, active1, 0L);
				case 'z':
					return jjMoveStringLiteralDfa10_0(active0, 2305843009213693952L, active1, 0L);
			}

			return jjStartNfa_0(8, active0, active1);
		}
	}

	private static int jjMoveStringLiteralDfa18_0(long old1, long active1) {
		if ((active1 = active1 & old1) == 0L) {
			return jjStartNfa_0(16, 0L, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(17, 0L, active1);
				return 18;
			}

			switch (curChar) {
				case 'd':
					if ((active1 & 8L) != 0L) {
						jjmatchedKind = 67;
						jjmatchedPos = 18;
					}

					return jjMoveStringLiteralDfa19_0(active1, 16L);
				case 'n':
					return jjMoveStringLiteralDfa19_0(active1, 128L);
				case 's':
					return jjMoveStringLiteralDfa19_0(active1, 96L);
				default:
					return jjStartNfa_0(17, 0L, active1);
			}
		}
	}

	private static int jjMoveStringLiteralDfa28_0(long old1, long active1) {
		if ((active1 = active1 & old1) == 0L) {
			return jjStartNfa_0(26, 0L, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(27, 0L, active1);
				return 28;
			}

			switch (curChar) {
				case ':':
					if ((active1 & 128L) != 0L) {
						return jjStopAtPos(28, 71);
					}
				default:
					return jjStartNfa_0(27, 0L, active1);
			}
		}
	}

	private static int jjMoveStringLiteralDfa7_0(long old0, long active0, long old1, long active1) {
		if (((active0 = active0 & old0) | (active1 = active1 & old1)) == 0L) {
			return jjStartNfa_0(5, old0, old1);
		} else {
			try {
				curChar = SimpleCharStream3.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(6, active0, active1);
				return 7;
			}

			switch (curChar) {
				case '-':
					return jjMoveStringLiteralDfa8_0(active0, 576460752303423488L, active1, 0L);
				case ':':
					if ((active0 & 70368744177664L) != 0L) {
						return jjStopAtPos(7, 46);
					}
					break;
				case '>':
					if ((active0 & 35184372088832L) != 0L) {
						return jjStopAtPos(7, 45);
					}
					break;
				case 'C':
					return jjMoveStringLiteralDfa8_0(active0, 4611686018427387904L, active1, 1L);
				case 'O':
					return jjMoveStringLiteralDfa8_0(active0, 72057594037927936L, active1, 0L);
				case 'a':
					return jjMoveStringLiteralDfa8_0(active0, 0L, active1, 128L);
				case 'b':
					return jjMoveStringLiteralDfa8_0(active0, 0L, active1, 2L);
				case 'c':
					return jjMoveStringLiteralDfa8_0(active0, 9007199254740992L, active1, 4L);
				case 'd':
					if ((active0 & 281474976710656L) != 0L) {
						return jjStartNfaWithStates_0(7, 48, 7);
					}
					break;
				case 'e':
					if ((active0 & 1125899906842624L) != 0L) {
						return jjStartNfaWithStates_0(7, 50, 7);
					}

					return jjMoveStringLiteralDfa8_0(active0, 2251799813685248L, active1, 0L);
				case 'f':
					return jjMoveStringLiteralDfa8_0(active0, 36028797018963968L, active1, 0L);
				case 'i':
					return jjMoveStringLiteralDfa8_0(active0, 436849163854938112L, active1, 0L);
				case 'l':
					return jjMoveStringLiteralDfa8_0(active0, 1152921504606846976L, active1, 32L);
				case 'n':
					return jjMoveStringLiteralDfa8_0(active0, 2323857407723175936L, active1, 0L);
				case 'r':
					return jjMoveStringLiteralDfa8_0(active0, 0L, active1, 24L);
				case 't':
					if ((active0 & 562949953421312L) != 0L) {
						return jjStartNfaWithStates_0(7, 49, 7);
					}

					return jjMoveStringLiteralDfa8_0(active0, Long.MIN_VALUE, active1, 64L);
			}

			return jjStartNfa_0(6, active0, active1);
		}
	}
}
