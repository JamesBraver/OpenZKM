package com.zelix.klassmaster.mapping;

import com.zelix.exceptions.MappingLexicalError;
import com.zelix.klassmaster.mapping.javaccInterface.MappingTokenConstants;
import java.io.IOException;
import java.io.PrintStream;

public class MappingTokenManager implements MappingTokenConstants {
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
		"\ufeff",
		"\n",
		"\r",
		"(",
		")",
		"[",
		"]",
		",",
		"/",
		".",
		":",
		"->",
		"<init>",
		"<clinit>",
		"package-info",
		null,
		null,
		null,
		null,
		null
	};
	public static final String[] lexStateNames = new String[]{"DEFAULT", "IN_SINGLE_LINE_COMMENT", "IN_FORMAL_COMMENT", "IN_MULTI_LINE_COMMENT", "IN_STRING_LITERAL"};
	public static final int[] jjnewLexState = new int[]{-1, -1, -1, -1, 1, 2, 3, 0, 0, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 4, 0, -1, -1, -1};
	public static final long[] jjtoToken = new long[]{1811937281L};
	public static final long[] jjtoSkip = new long[]{910L};
	public static final long[] jjtoSpecial = new long[]{896L};
	public static final long[] jjtoMore = new long[]{335545456L};
	private static final int[] jjrounds = new int[6];
	private static final int[] jjstateSet = new int[12];
	private static final StringBuilder jjimage = new StringBuilder();
	private static StringBuilder image = jjimage;
	public static int curLexState = 0;
	public static int defaultLexState = 0;
	public static int jjmatchedPos;
	public static int jjround;
	private static int jjimageLen;
	protected static char curChar;
	protected static SimpleCharStream4 input_stream;
	public static int jjnewStateCnt;
	public static int jjmatchedKind;
	private static int lengthOfMatch;

	public MappingTokenManager(SimpleCharStream4 stream) {
		if (input_stream != null) {
			throw new MappingLexicalError("ERROR: Second call to constructor of static lexer. You must use ReInit() to initialize the static variables.", 1);
		} else {
			input_stream = stream;
		}
	}

	private static int jjMoveStringLiteralDfa1_2(long active0) {
		try {
			curChar = SimpleCharStream4.readChar();
		} catch (IOException ignored) {
			return 1;
		}

		switch (curChar) {
			case '/':
				if ((active0 & 256L) != 0L) {
					return jjStopAtPos(1, 8);
				}

				return 2;
			default:
				return 2;
		}
	}

	private static int jjMoveNfa_1(int startState, int curPos) {
		int stateSetStart = 0;
		jjnewStateCnt = 3;
		int stateSetIndex = 1;
		jjstateSet[0] = startState;
		int matchedKind = Integer.MAX_VALUE;

		while (true) {
			if (++jjround == Integer.MAX_VALUE) {
				ReInitRounds();
			}

			if (curChar < '@') {
				long charMaskLow = 1L << curChar;

				do {
					stateSetIndex--;
					switch (jjstateSet[stateSetIndex]) {
						case 0:
							if ((13312L & charMaskLow) != 0L && matchedKind > 7) {
								matchedKind = 7;
							}

							if (curChar == '\r') {
								jjstateSet[jjnewStateCnt++] = 1;
							}
							break;
						case 1:
							if (curChar == '\n' && matchedKind > 7) {
								matchedKind = 7;
							}
							break;
						case 2:
							if (curChar == '\r') {
								jjstateSet[jjnewStateCnt++] = 1;
							}
					}
				} while (stateSetIndex != stateSetStart);
			} else if (curChar < 128) {
				long charMaskHigh = 1L << (curChar & '?');

				do {
					int currentState = jjstateSet[--stateSetIndex];
				} while (stateSetIndex != stateSetStart);
			} else {
				int highByte = curChar >> '\b';
				int highIndex = highByte >> 6;
				long highMask = 1L << (highByte & 63);
				highIndex = (curChar & 255) >> 6;
				long lowMask = 1L << (curChar & '?');

				do {
					int wideCharState = jjstateSet[--stateSetIndex];
				} while (stateSetIndex != stateSetStart);
			}

			if (matchedKind != Integer.MAX_VALUE) {
				jjmatchedKind = matchedKind;
				jjmatchedPos = curPos;
				matchedKind = Integer.MAX_VALUE;
			}

			curPos++;
			stateSetIndex = jjnewStateCnt;
			int previousStateCount = jjnewStateCnt;
			jjnewStateCnt = stateSetStart;
			if (previousStateCount == (stateSetStart = 3 - stateSetStart)) {
				return curPos;
			}

			try {
				curChar = SimpleCharStream4.readChar();
			} catch (IOException ignored) {
				return curPos;
			}
		}
	}

	private static int jjMoveStringLiteralDfa0_4() {
		switch (curChar) {
			case '"':
				return jjStartNfaWithStates_4(0, 27, 2);
			default:
				return jjMoveNfa_4(0, 0);
		}
	}

	public static void ReInit(SimpleCharStream4 stream) {
		jjnewStateCnt = 0;
		jjmatchedPos = 0;
		curLexState = defaultLexState;
		input_stream = stream;
		ReInitRounds();
	}

	private static int jjMoveStringLiteralDfa0_2() {
		switch (curChar) {
			case '*':
				return jjMoveStringLiteralDfa1_2(256L);
			default:
				return 1;
		}
	}

	private static int jjMoveStringLiteralDfa9_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return jjStartNfa_0(7, old0);
		} else {
			try {
				curChar = SimpleCharStream4.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(8, active0);
				return 9;
			}

			switch (curChar) {
				case 'n':
					return jjMoveStringLiteralDfa10_0(active0, 33554432L);
				default:
					return jjStartNfa_0(8, active0);
			}
		}
	}

	private static int jjMoveStringLiteralDfa10_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return jjStartNfa_0(8, old0);
		} else {
			try {
				curChar = SimpleCharStream4.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(9, active0);
				return 10;
			}

			switch (curChar) {
				case 'f':
					return jjMoveStringLiteralDfa12_0(active0, 33554432L);
				default:
					return jjStartNfa_0(9, active0);
			}
		}
	}

	private static int jjMoveStringLiteralDfa4_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return jjStartNfa_0(2, old0);
		} else {
			try {
				curChar = SimpleCharStream4.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(3, active0);
				return 4;
			}

			switch (curChar) {
				case 'a':
					return jjMoveStringLiteralDfa5_0(active0, 33554432L);
				case 'n':
					return jjMoveStringLiteralDfa5_0(active0, 16777216L);
				case 't':
					return jjMoveStringLiteralDfa5_0(active0, 8388608L);
				default:
					return jjStartNfa_0(3, active0);
			}
		}
	}

	private static int jjMoveStringLiteralDfa0_0() {
		switch (curChar) {
			case '\n':
				return jjStopAtPos(0, 12);
			case '\r':
				return jjStopAtPos(0, 13);
			case '"':
				return jjStopAtPos(0, 26);
			case '(':
				return jjStopAtPos(0, 14);
			case ')':
				return jjStopAtPos(0, 15);
			case ',':
				return jjStopAtPos(0, 18);
			case '-':
				return jjMoveStringLiteralDfa1_0(4194304L);
			case '.':
				return jjStopAtPos(0, 20);
			case '/':
				jjmatchedKind = 19;
				return jjMoveStringLiteralDfa1_0(80L);
			case ':':
				return jjStopAtPos(0, 21);
			case '<':
				return jjMoveStringLiteralDfa1_0(25165824L);
			case '[':
				return jjStopAtPos(0, 16);
			case ']':
				return jjStopAtPos(0, 17);
			case 'p':
				return jjMoveStringLiteralDfa1_0(33554432L);
			case '\ufeff':
				return jjStartNfaWithStates_0(0, 11, 5);
			default:
				return jjMoveNfa_0(3, 0);
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

	private static int jjStopAtPos(int pos, int kind) {
		jjmatchedKind = kind;
		jjmatchedPos = pos;
		return pos + 1;
	}

	private static int jjMoveNfa_4(int startState, int curPos) {
		int stateSetStart = 0;
		jjnewStateCnt = 3;
		int stateSetIndex = 1;
		jjstateSet[0] = startState;
		int matchedKind = Integer.MAX_VALUE;

		while (true) {
			if (++jjround == Integer.MAX_VALUE) {
				ReInitRounds();
			}

			if (curChar < '@') {
				long charMaskLow = 1L << curChar;

				do {
					stateSetIndex--;
					switch (jjstateSet[stateSetIndex]) {
						case 0:
							if ((-17179882497L & charMaskLow) != 0L) {
								if (matchedKind > 28) {
									matchedKind = 28;
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
							if (curChar == '"' && matchedKind > 28) {
								matchedKind = 28;
							}
					}
				} while (stateSetIndex != stateSetStart);
			} else if (curChar < 128) {
				long charMaskHigh = 1L << (curChar & '?');

				do {
					stateSetIndex--;
					switch (jjstateSet[stateSetIndex]) {
						case 0:
							matchedKind = 28;
					}
				} while (stateSetIndex != stateSetStart);
			} else {
				int highByte = curChar >> '\b';
				int highIndex = highByte >> 6;
				long highMask = 1L << (highByte & 63);
				int lowIndex = (curChar & 255) >> 6;
				long lowMask = 1L << (curChar & '?');

				do {
					stateSetIndex--;
					switch (jjstateSet[stateSetIndex]) {
						case 0:
							if (jjCanMove_1(highByte, highIndex, lowIndex, highMask, lowMask) && matchedKind > 28) {
								matchedKind = 28;
							}
					}
				} while (stateSetIndex != stateSetStart);
			}

			if (matchedKind != Integer.MAX_VALUE) {
				jjmatchedKind = matchedKind;
				jjmatchedPos = curPos;
				matchedKind = Integer.MAX_VALUE;
			}

			curPos++;
			stateSetIndex = jjnewStateCnt;
			int previousStateCount = jjnewStateCnt;
			jjnewStateCnt = stateSetStart;
			if (previousStateCount == (stateSetStart = 3 - stateSetStart)) {
				return curPos;
			}

			try {
				curChar = SimpleCharStream4.readChar();
			} catch (IOException ignored) {
				return curPos;
			}
		}
	}

	private static int jjMoveStringLiteralDfa8_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return jjStartNfa_0(6, old0);
		} else {
			try {
				curChar = SimpleCharStream4.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(7, active0);
				return 8;
			}

			switch (curChar) {
				case 'i':
					return jjMoveStringLiteralDfa9_0(active0, 33554432L);
				default:
					return jjStartNfa_0(7, active0);
			}
		}
	}

	private static int jjMoveStringLiteralDfa12_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return jjStartNfa_0(9, old0);
		} else {
			try {
				curChar = SimpleCharStream4.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(10, active0);
				return 11;
			}

			switch (curChar) {
				case 'o':
					if ((active0 & 33554432L) != 0L) {
						return jjStopAtPos(11, 25);
					}
				default:
					return jjStartNfa_0(10, active0);
			}
		}
	}

	public static void SkipLexicalActions(Token4 matchedToken) {
	}

	private static int jjMoveStringLiteralDfa3_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return jjStartNfa_0(1, old0);
		} else {
			try {
				curChar = SimpleCharStream4.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(2, active0);
				return 3;
			}

			switch (curChar) {
				case 'i':
					return jjMoveStringLiteralDfa4_0(active0, 25165824L);
				case 'k':
					return jjMoveStringLiteralDfa4_0(active0, 33554432L);
				default:
					return jjStartNfa_0(2, active0);
			}
		}
	}

	private static void jjCheckNAdd(int state) {
		if (jjrounds[state] != jjround) {
			jjstateSet[jjnewStateCnt++] = state;
			jjrounds[state] = jjround;
		}
	}

	private static int jjMoveStringLiteralDfa2_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return jjStartNfa_0(0, old0);
		} else {
			try {
				curChar = SimpleCharStream4.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(1, active0);
				return 2;
			}

			switch (curChar) {
				case 'c':
					return jjMoveStringLiteralDfa3_0(active0, 33554432L);
				case 'l':
					return jjMoveStringLiteralDfa3_0(active0, 16777216L);
				case 'n':
					return jjMoveStringLiteralDfa3_0(active0, 8388608L);
				default:
					return jjStartNfa_0(1, active0);
			}
		}
	}

	private static void ReInitRounds() {
		jjround = -2147483647;
		int i = 6;

		while (i-- > 0) {
			jjrounds[i] = Integer.MIN_VALUE;
		}
	}

	private static final int jjStartNfa_0(int pos, long active0) {
		return jjMoveNfa_0(jjStopStringLiteralDfa_0(pos, active0), pos + 1);
	}

	protected static Token4 jjFillToken() {
		String literalImage = jjstrLiteralImages[jjmatchedKind];
		String curTokenImage = literalImage == null ? SimpleCharStream4.GetImage() : literalImage;
		int beginLine = SimpleCharStream4.getBeginLine();
		int beginColumn = SimpleCharStream4.getBeginColumn();
		int endLine = SimpleCharStream4.getEndLine();
		int endColumn = SimpleCharStream4.getEndColumn();
		Token4 token = Token4.newToken(jjmatchedKind, curTokenImage);
		token.beginLine = beginLine;
		token.endLine = endLine;
		token.beginColumn = beginColumn;
		token.endColumn = endColumn;
		return token;
	}

	private static int jjMoveStringLiteralDfa5_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return jjStartNfa_0(3, old0);
		} else {
			try {
				curChar = SimpleCharStream4.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(4, active0);
				return 5;
			}

			switch (curChar) {
				case '>':
					if ((active0 & 8388608L) != 0L) {
						return jjStopAtPos(5, 23);
					}
				default:
					return jjStartNfa_0(4, active0);
				case 'g':
					return jjMoveStringLiteralDfa6_0(active0, 33554432L);
				case 'i':
					return jjMoveStringLiteralDfa6_0(active0, 16777216L);
			}
		}
	}

	private static int jjMoveStringLiteralDfa1_0(long active0) {
		try {
			curChar = SimpleCharStream4.readChar();
		} catch (IOException ignored) {
			jjStopStringLiteralDfa_0(0, active0);
			return 1;
		}

		switch (curChar) {
			case '*':
				if ((active0 & 64L) != 0L) {
					return jjStartNfaWithStates_0(1, 6, 0);
				}
				break;
			case '/':
				if ((active0 & 16L) != 0L) {
					return jjStopAtPos(1, 4);
				}
				break;
			case '>':
				if ((active0 & 4194304L) != 0L) {
					return jjStopAtPos(1, 22);
				}
				break;
			case 'a':
				return jjMoveStringLiteralDfa2_0(active0, 33554432L);
			case 'c':
				return jjMoveStringLiteralDfa2_0(active0, 16777216L);
			case 'i':
				return jjMoveStringLiteralDfa2_0(active0, 8388608L);
		}

		return jjStartNfa_0(0, active0);
	}

	private static int jjMoveNfa_0(int startState, int curPos) {
		int stateSetStart = 0;
		jjnewStateCnt = 6;
		int stateSetIndex = 1;
		jjstateSet[0] = startState;
		int matchedKind = Integer.MAX_VALUE;

		while (true) {
			if (++jjround == Integer.MAX_VALUE) {
				ReInitRounds();
			}

			if (curChar < '@') {
				long charMaskLow = 1L << curChar;

				do {
					stateSetIndex--;
					switch (jjstateSet[stateSetIndex]) {
						case 0:
							if (curChar == '*') {
								jjstateSet[jjnewStateCnt++] = 1;
							}
							break;
						case 1:
							if ((-140737488355329L & charMaskLow) != 0L && matchedKind > 5) {
								matchedKind = 5;
							}
							break;
						case 2:
							if (curChar == '*') {
								jjstateSet[jjnewStateCnt++] = 0;
							}
							break;
						case 3:
							if ((287957766255919615L & charMaskLow) != 0L) {
								if (matchedKind > 30) {
									matchedKind = 30;
								}

								jjCheckNAdd(5);
							} else if (curChar == '/') {
								jjstateSet[jjnewStateCnt++] = 2;
							}

							if ((287948901175001088L & charMaskLow) != 0L) {
								if (matchedKind > 29) {
									matchedKind = 29;
								}

								jjCheckNAdd(4);
							}
							break;
						case 4:
							if ((287948901175001088L & charMaskLow) != 0L) {
								if (matchedKind > 29) {
									matchedKind = 29;
								}

								jjCheckNAdd(4);
							}
							break;
						case 5:
							if ((287957766255919615L & charMaskLow) != 0L) {
								if (matchedKind > 30) {
									matchedKind = 30;
								}

								jjCheckNAdd(5);
							}
					}
				} while (stateSetIndex != stateSetStart);
			} else if (curChar < 128) {
				long charMaskHigh = 1L << (curChar & '?');

				do {
					stateSetIndex--;
					switch (jjstateSet[stateSetIndex]) {
						case 1:
							if (matchedKind > 5) {
								matchedKind = 5;
							}
						case 2:
						case 4:
						default:
							break;
						case 3:
						case 5:
							if ((-8646911290859585538L & charMaskHigh) != 0L) {
								if (matchedKind > 30) {
									matchedKind = 30;
								}

								jjCheckNAdd(5);
							}
					}
				} while (stateSetIndex != stateSetStart);
			} else {
				int highByte = curChar >> '\b';
				int highIndex = highByte >> 6;
				long highMask = 1L << (highByte & 63);
				int lowIndex = (curChar & 255) >> 6;
				long lowMask = 1L << (curChar & '?');

				do {
					stateSetIndex--;
					switch (jjstateSet[stateSetIndex]) {
						case 1:
							if (jjCanMove_0(highByte, highIndex, lowIndex, highMask, lowMask) && matchedKind > 5) {
								matchedKind = 5;
							}
						case 2:
						case 4:
						default:
							break;
						case 3:
						case 5:
							if (jjCanMove_1(highByte, highIndex, lowIndex, highMask, lowMask)) {
								if (matchedKind > 30) {
									matchedKind = 30;
								}

								jjCheckNAdd(5);
							}
					}
				} while (stateSetIndex != stateSetStart);
			}

			if (matchedKind != Integer.MAX_VALUE) {
				jjmatchedKind = matchedKind;
				jjmatchedPos = curPos;
				matchedKind = Integer.MAX_VALUE;
			}

			curPos++;
			stateSetIndex = jjnewStateCnt;
			int previousStateCount = jjnewStateCnt;
			jjnewStateCnt = stateSetStart;
			if (previousStateCount == (stateSetStart = 6 - stateSetStart)) {
				return curPos;
			}

			try {
				curChar = SimpleCharStream4.readChar();
			} catch (IOException ignored) {
				return curPos;
			}
		}
	}

	public static Token4 getNextToken() {
		Token4 specialToken = null;
		int curPos = 0;

		label129:
		while (true) {
			try {
				curChar = SimpleCharStream4.BeginToken();
			} catch (IOException ignoredBeginToken) {
				jjmatchedKind = 0;
				Token4 eofToken = jjFillToken();
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
							SimpleCharStream4.backup(0);

							while (curChar <= ' ' && (4294971904L & 1L << curChar) != 0L) {
								curChar = SimpleCharStream4.BeginToken();
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
						if (jjmatchedPos == 0 && jjmatchedKind > 10) {
							jjmatchedKind = 10;
						}
						break;
					case 2:
						jjmatchedKind = Integer.MAX_VALUE;
						jjmatchedPos = 0;
						curPos = jjMoveStringLiteralDfa0_2();
						if (jjmatchedPos == 0 && jjmatchedKind > 10) {
							jjmatchedKind = 10;
						}
						break;
					case 3:
						jjmatchedKind = Integer.MAX_VALUE;
						jjmatchedPos = 0;
						curPos = jjMoveStringLiteralDfa0_3();
						if (jjmatchedPos == 0 && jjmatchedKind > 10) {
							jjmatchedKind = 10;
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
					SimpleCharStream4.backup(curPos - jjmatchedPos - 1);
				}

				if ((jjtoToken[jjmatchedKind >> 6] & 1L << (jjmatchedKind & 63)) != 0L) {
					Token4 matchedToken = jjFillToken();
					matchedToken.specialToken = specialToken;
					if (jjnewLexState[jjmatchedKind] != -1) {
						curLexState = jjnewLexState[jjmatchedKind];
					}

					return matchedToken;
				}

				if ((jjtoSkip[jjmatchedKind >> 6] & 1L << (jjmatchedKind & 63)) != 0L) {
					if ((jjtoSpecial[jjmatchedKind >> 6] & 1L << (jjmatchedKind & 63)) != 0L) {
						Token4 matchedSpecialToken = jjFillToken();
						if (specialToken == null) {
							specialToken = matchedSpecialToken;
						} else {
							matchedSpecialToken.specialToken = specialToken;
							specialToken = specialToken.next = matchedSpecialToken;
						}

						SkipLexicalActions(matchedSpecialToken);
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
					curChar = SimpleCharStream4.readChar();
				} catch (IOException ignoredReadChar) {
					break label129;
				}
			}
		}

		int errorLine = SimpleCharStream4.getEndLine();
		int errorColumn = SimpleCharStream4.getEndColumn();
		String errorAfter = null;
		boolean EOFSeen = false;

		try {
			SimpleCharStream4.readChar();
			SimpleCharStream4.backup(1);
		} catch (IOException ignoredEofProbe) {
			EOFSeen = true;
			errorAfter = curPos <= 1 ? "" : SimpleCharStream4.GetImage();
			if (curChar != '\n' && curChar != '\r') {
				errorColumn++;
			} else {
				errorLine++;
				errorColumn = 0;
			}
		}

		if (!EOFSeen) {
			SimpleCharStream4.backup(1);
			errorAfter = curPos <= 1 ? "" : SimpleCharStream4.GetImage();
		}

		throw new MappingLexicalError(EOFSeen, curLexState, errorLine, errorColumn, errorAfter, curChar, 0);
	}

	private static final boolean jjCanMove_1(int highByte, int highIndex, int lowIndex, long highMask, long lowMask) {
		switch (highByte) {
			case 0:
				return (jjbitVec0[lowIndex] & lowMask) != 0L;
			default:
				return (jjbitVec1[highIndex] & highMask) != 0L;
		}
	}

	private static int jjStartNfaWithStates_4(int pos, int kind, int state) {
		jjmatchedKind = kind;
		jjmatchedPos = pos;

		try {
			curChar = SimpleCharStream4.readChar();
		} catch (IOException ignored) {
			return pos + 1;
		}

		return jjMoveNfa_4(state, pos + 1);
	}

	private static int jjMoveStringLiteralDfa0_1() {
		return jjMoveNfa_1(0, 0);
	}

	private static int jjMoveStringLiteralDfa7_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return jjStartNfa_0(5, old0);
		} else {
			try {
				curChar = SimpleCharStream4.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(6, active0);
				return 7;
			}

			switch (curChar) {
				case '-':
					return jjMoveStringLiteralDfa8_0(active0, 33554432L);
				case '>':
					if ((active0 & 16777216L) != 0L) {
						return jjStopAtPos(7, 24);
					}
				default:
					return jjStartNfa_0(6, active0);
			}
		}
	}

	private static int jjStartNfaWithStates_0(int pos, int kind, int state) {
		jjmatchedKind = kind;
		jjmatchedPos = pos;

		try {
			curChar = SimpleCharStream4.readChar();
		} catch (IOException ignored) {
			return pos + 1;
		}

		return jjMoveNfa_0(state, pos + 1);
	}

	private static final int jjStopStringLiteralDfa_0(int pos, long active0) {
		switch (pos) {
			case 0:
				if ((active0 & 524368L) != 0L) {
					return 2;
				} else if ((active0 & 33554432L) != 0L) {
					jjmatchedKind = 30;
					return 5;
				} else {
					if ((active0 & 2048L) != 0L) {
						return 5;
					}

					return -1;
				}
			case 1:
				if ((active0 & 64L) != 0L) {
					return 0;
				} else {
					if ((active0 & 33554432L) != 0L) {
						jjmatchedKind = 30;
						jjmatchedPos = 1;
						return 5;
					}

					return -1;
				}
			case 2:
				if ((active0 & 33554432L) != 0L) {
					jjmatchedKind = 30;
					jjmatchedPos = 2;
					return 5;
				}

				return -1;
			case 3:
				if ((active0 & 33554432L) != 0L) {
					jjmatchedKind = 30;
					jjmatchedPos = 3;
					return 5;
				}

				return -1;
			case 4:
				if ((active0 & 33554432L) != 0L) {
					jjmatchedKind = 30;
					jjmatchedPos = 4;
					return 5;
				}

				return -1;
			case 5:
				if ((active0 & 33554432L) != 0L) {
					jjmatchedKind = 30;
					jjmatchedPos = 5;
					return 5;
				}

				return -1;
			case 6:
				if ((active0 & 33554432L) != 0L) {
					jjmatchedKind = 30;
					jjmatchedPos = 6;
					return 5;
				}

				return -1;
			case 7:
				if ((active0 & 33554432L) != 0L) {
					if (jjmatchedPos < 6) {
						jjmatchedKind = 30;
						jjmatchedPos = 6;
					}

					return -1;
				}

				return -1;
			case 8:
				if ((active0 & 33554432L) != 0L) {
					if (jjmatchedPos < 6) {
						jjmatchedKind = 30;
						jjmatchedPos = 6;
					}

					return -1;
				}

				return -1;
			case 9:
				if ((active0 & 33554432L) != 0L) {
					if (jjmatchedPos < 6) {
						jjmatchedKind = 30;
						jjmatchedPos = 6;
					}

					return -1;
				}

				return -1;
			case 10:
				if ((active0 & 33554432L) != 0L) {
					if (jjmatchedPos < 6) {
						jjmatchedKind = 30;
						jjmatchedPos = 6;
					}

					return -1;
				}

				return -1;
			default:
				return -1;
		}
	}

	private static int jjMoveStringLiteralDfa0_3() {
		switch (curChar) {
			case '*':
				return jjMoveStringLiteralDfa1_3(512L);
			default:
				return 1;
		}
	}

	private static int jjMoveStringLiteralDfa1_3(long active0) {
		try {
			curChar = SimpleCharStream4.readChar();
		} catch (IOException ignored) {
			return 1;
		}

		switch (curChar) {
			case '/':
				if ((active0 & 512L) != 0L) {
					return jjStopAtPos(1, 9);
				}

				return 2;
			default:
				return 2;
		}
	}

	public static void MoreLexicalActions() {
		jjimageLen = jjimageLen + (lengthOfMatch = jjmatchedPos + 1);
		switch (jjmatchedKind) {
			case 5:
				image.append(SimpleCharStream4.GetSuffix(jjimageLen));
				jjimageLen = 0;
				SimpleCharStream4.backup(1);
		}
	}

	private static int jjMoveStringLiteralDfa6_0(long old0, long active0) {
		if ((active0 = active0 & old0) == 0L) {
			return jjStartNfa_0(4, old0);
		} else {
			try {
				curChar = SimpleCharStream4.readChar();
			} catch (IOException ignored) {
				jjStopStringLiteralDfa_0(5, active0);
				return 6;
			}

			switch (curChar) {
				case 'e':
					return jjMoveStringLiteralDfa7_0(active0, 33554432L);
				case 't':
					return jjMoveStringLiteralDfa7_0(active0, 16777216L);
				default:
					return jjStartNfa_0(5, active0);
			}
		}
	}
}
