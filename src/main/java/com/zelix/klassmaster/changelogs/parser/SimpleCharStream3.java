package com.zelix.klassmaster.changelogs.parser;

import java.io.IOException;
import java.io.Reader;
public class SimpleCharStream3 {
	public static int bufpos = -1;
	protected static int column = 0;
	protected static int line = 1;
	protected static boolean prevCharIsCR = false;
	protected static boolean prevCharIsLF = false;
	protected static int maxNextCharInd = 0;
	protected static int inBuf = 0;
	protected static int tabSize = 8;
	public static int available;
	protected static char[] buffer;
	protected static int[] bufline;
	public static int bufsize;
	public static int tokenBegin;
	protected static int[] bufcolumn;
	protected static Reader inputStream;

	public SimpleCharStream3(Reader dstream, int startline, int startcolumn, int buffersize) {
		if (inputStream != null) {
			throw new Error(
				"\n   ERROR: Second call to the constructor of a static SimpleCharStream.\n       You must either use ReInit() or set the JavaCC option STATIC to false\n       during the generation of this class."
			);
		} else {
			inputStream = dstream;
			line = startline;
			column = startcolumn - 1;
			bufsize = buffersize;
			available = buffersize;
			buffer = new char[buffersize];
			bufline = new int[buffersize];
			bufcolumn = new int[buffersize];
		}
	}

	public SimpleCharStream3(Reader dstream, int startline, int startcolumn) {
		this(dstream, startline, startcolumn, 4096);
	}

	public static void backup(int amount) {
		inBuf += amount;
		if ((bufpos -= amount) < 0) {
			bufpos = bufpos + bufsize;
		}
	}

	public static int getBeginColumn() {
		return bufcolumn[tokenBegin];
	}

	public static char readChar() throws IOException {
		if (inBuf > 0) {
			inBuf--;
			if (++bufpos == bufsize) {
				bufpos = 0;
			}

			return buffer[bufpos];
		} else {
			if (++bufpos >= maxNextCharInd) {
				FillBuff();
			}

			char c = buffer[bufpos];
			UpdateLineColumn(c);
			return c;
		}
	}

	protected static void UpdateLineColumn(char c) {
		column++;
		if (prevCharIsLF) {
			prevCharIsLF = false;
			int previousLine = line;
			column = 1;
			line = previousLine + 1;
		} else if (prevCharIsCR) {
			prevCharIsCR = false;
			if (c == '\n') {
				prevCharIsLF = true;
			} else {
				int previousLineOnCr = line;
				column = 1;
				line = previousLineOnCr + 1;
			}
		}

		switch (c) {
			case '\t':
				column--;
				column = column + (tabSize - column % tabSize);
				break;
			case '\n':
				prevCharIsLF = true;
			case '\u000b':
			case '\f':
			default:
				break;
			case '\r':
				prevCharIsCR = true;
		}

		bufline[bufpos] = line;
		bufcolumn[bufpos] = column;
	}

	protected static void FillBuff() throws IOException {
		if (maxNextCharInd == available) {
			if (available == bufsize) {
				if (tokenBegin > 2048) {
					maxNextCharInd = 0;
					bufpos = 0;
					available = tokenBegin;
				} else if (tokenBegin < 0) {
					maxNextCharInd = 0;
					bufpos = 0;
				} else {
					ExpandBuff(false);
				}
			} else if (available > tokenBegin) {
				available = bufsize;
			} else if (tokenBegin - available < 2048) {
				ExpandBuff(true);
			} else {
				available = tokenBegin;
			}
		}

		try {
			int charsRead;
			if ((charsRead = inputStream.read(buffer, maxNextCharInd, available - maxNextCharInd)) == -1) {
				inputStream.close();
				throw new IOException();
			} else {
				maxNextCharInd += charsRead;
			}
		} catch (IOException ioException) {
			bufpos--;
			backup(0);
			if (tokenBegin == -1) {
				tokenBegin = bufpos;
			}

			throw ioException;
		}
	}

	public static int getColumn() {
		return bufcolumn[bufpos];
	}

	public static String GetImage() {
		return bufpos >= tokenBegin
			? new String(buffer, tokenBegin, bufpos - tokenBegin + 1)
			: new String(buffer, tokenBegin, bufsize - tokenBegin) + new String(buffer, 0, bufpos + 1);
	}

	public static char[] GetSuffix(int len) {
		char[] suffixChars = new char[len];
		if (bufpos + 1 >= len) {
			System.arraycopy(buffer, bufpos - len + 1, suffixChars, 0, len);
		} else {
			System.arraycopy(buffer, bufsize - (len - bufpos - 1), suffixChars, 0, len - bufpos - 1);
			System.arraycopy(buffer, 0, suffixChars, len - bufpos - 1, bufpos + 1);
		}

		return suffixChars;
	}

	public static int getLine() {
		return bufline[bufpos];
	}

	public static int getBeginLine() {
		return bufline[tokenBegin];
	}

	protected static void ExpandBuff(boolean wrapAround) {
		char[] newBuffer = new char[bufsize + 2048];
		int[] newBufline = new int[bufsize + 2048];
		int[] newBufcolumn = new int[bufsize + 2048];

		try {
			if (wrapAround) {
				System.arraycopy(buffer, tokenBegin, newBuffer, 0, bufsize - tokenBegin);
				System.arraycopy(buffer, 0, newBuffer, bufsize - tokenBegin, bufpos);
				buffer = newBuffer;
				System.arraycopy(bufline, tokenBegin, newBufline, 0, bufsize - tokenBegin);
				System.arraycopy(bufline, 0, newBufline, bufsize - tokenBegin, bufpos);
				bufline = newBufline;
				System.arraycopy(bufcolumn, tokenBegin, newBufcolumn, 0, bufsize - tokenBegin);
				System.arraycopy(bufcolumn, 0, newBufcolumn, bufsize - tokenBegin, bufpos);
				bufcolumn = newBufcolumn;
				maxNextCharInd = bufpos = bufpos + (bufsize - tokenBegin);
			} else {
				System.arraycopy(buffer, tokenBegin, newBuffer, 0, bufsize - tokenBegin);
				buffer = newBuffer;
				System.arraycopy(bufline, tokenBegin, newBufline, 0, bufsize - tokenBegin);
				bufline = newBufline;
				System.arraycopy(bufcolumn, tokenBegin, newBufcolumn, 0, bufsize - tokenBegin);
				bufcolumn = newBufcolumn;
				maxNextCharInd = bufpos = bufpos - tokenBegin;
			}
		} catch (Throwable expansionFailure) {
			throw new Error(expansionFailure.getMessage());
		}

		bufsize += 2048;
		available = bufsize;
		tokenBegin = 0;
	}

	public static char BeginToken() throws IOException {
		tokenBegin = -1;
		char c = readChar();
		tokenBegin = bufpos;
		return c;
	}

	public void ReInit(Reader dstream, int startline, int startcolumn) {
		this.ReInit(dstream, startline, startcolumn, 4096);
	}

	public void ReInit(Reader dstream, int startline, int startcolumn, int buffersize) {
		inputStream = dstream;
		line = startline;
		column = startcolumn - 1;
		if (buffer == null || buffersize != buffer.length) {
			bufsize = buffersize;
			available = buffersize;
			buffer = new char[buffersize];
			bufline = new int[buffersize];
			bufcolumn = new int[buffersize];
		}

		prevCharIsCR = false;
		prevCharIsLF = false;
		maxNextCharInd = 0;
		inBuf = 0;
		tokenBegin = 0;
		bufpos = -1;
	}
}
