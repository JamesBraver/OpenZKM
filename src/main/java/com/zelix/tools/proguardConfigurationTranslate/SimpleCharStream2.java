package com.zelix.tools.proguardConfigurationTranslate;

import java.io.IOException;
import java.io.Reader;

public class SimpleCharStream2 {
	public int bufpos = -1;
	protected int column = 0;
	protected int line = 1;
	protected boolean prevCharIsCR = false;
	protected boolean prevCharIsLF = false;
	protected int maxNextCharInd = 0;
	protected int inBuf = 0;
	protected int tabSize = 8;
	protected Reader inputStream;
	public int bufsize;
	public int available;
	protected char[] buffer;
	protected int[] bufline;
	protected int[] bufcolumn;
	public int tokenBegin;

	public SimpleCharStream2(Reader dstream, int startline, int startcolumn, int buffersize) {
		this.inputStream = dstream;
		this.line = startline;
		this.column = startcolumn - 1;
		this.available = this.bufsize = buffersize;
		this.buffer = new char[buffersize];
		this.bufline = new int[buffersize];
		this.bufcolumn = new int[buffersize];
	}

	public SimpleCharStream2(Reader dstream, int startline, int startcolumn) {
		this(dstream, startline, startcolumn, 4096);
	}

	public int getBeginLine() {
		return this.bufline[this.tokenBegin];
	}

	protected void FillBuff() throws IOException {
		if (this.maxNextCharInd == this.available) {
			if (this.available == this.bufsize) {
				if (this.tokenBegin > 2048) {
					this.bufpos = this.maxNextCharInd = 0;
					this.available = this.tokenBegin;
				} else if (this.tokenBegin < 0) {
					this.bufpos = this.maxNextCharInd = 0;
				} else {
					this.ExpandBuff(false);
				}
			} else if (this.available > this.tokenBegin) {
				this.available = this.bufsize;
			} else if (this.tokenBegin - this.available < 2048) {
				this.ExpandBuff(true);
			} else {
				this.available = this.tokenBegin;
			}
		}

		try {
			int charsRead;
			if ((charsRead = this.inputStream.read(this.buffer, this.maxNextCharInd, this.available - this.maxNextCharInd)) == -1) {
				this.inputStream.close();
				throw new IOException();
			} else {
				this.maxNextCharInd += charsRead;
			}
		} catch (IOException ioException) {
			this.bufpos--;
			this.backup(0);
			if (this.tokenBegin == -1) {
				this.tokenBegin = this.bufpos;
			}

			throw ioException;
		}
	}

	public int getEndColumn() {
		return this.bufcolumn[this.bufpos];
	}

	public char BeginToken() throws IOException {
		this.tokenBegin = -1;
		char firstChar = this.readChar();
		this.tokenBegin = this.bufpos;
		return firstChar;
	}

	public char readChar() throws IOException {
		if (this.inBuf > 0) {
			this.inBuf--;
			if (++this.bufpos == this.bufsize) {
				this.bufpos = 0;
			}

			return this.buffer[this.bufpos];
		} else {
			if (++this.bufpos >= this.maxNextCharInd) {
				this.FillBuff();
			}

			char currentChar = this.buffer[this.bufpos];
			this.UpdateLineColumn(currentChar);
			return currentChar;
		}
	}

	protected void UpdateLineColumn(int c) {
		this.column++;
		if (this.prevCharIsLF) {
			this.prevCharIsLF = false;
			this.line = this.line + (this.column = 1);
		} else if (this.prevCharIsCR) {
			this.prevCharIsCR = false;
			if (c == 10) {
				this.prevCharIsLF = true;
			} else {
				this.line = this.line + (this.column = 1);
			}
		}

		switch (c) {
			case 9:
				this.column--;
				this.column = this.column + (this.tabSize - this.column % this.tabSize);
				break;
			case 10:
				this.prevCharIsLF = true;
			case 11:
			case 12:
			default:
				break;
			case 13:
				this.prevCharIsCR = true;
		}

		this.bufline[this.bufpos] = this.line;
		this.bufcolumn[this.bufpos] = this.column;
	}

	public int getEndLine() {
		return this.bufline[this.bufpos];
	}

	public String GetImage() {
		return this.bufpos >= this.tokenBegin ? new String(this.buffer, this.tokenBegin, this.bufpos - this.tokenBegin + 1) : new String(this.buffer, this.tokenBegin, this.bufsize - this.tokenBegin) + new String(this.buffer, 0, this.bufpos + 1);
	}

	public void backup(int amount) {
		this.inBuf += amount;
		if ((this.bufpos -= amount) < 0) {
			this.bufpos = this.bufpos + this.bufsize;
		}
	}

	protected void ExpandBuff(boolean wrapAround) {
		char[] newBuffer = new char[this.bufsize + 2048];
		int[] newLineNumbers = new int[this.bufsize + 2048];
		int[] newColumnNumbers = new int[this.bufsize + 2048];

		try {
			if (wrapAround) {
				System.arraycopy(this.buffer, this.tokenBegin, newBuffer, 0, this.bufsize - this.tokenBegin);
				System.arraycopy(this.buffer, 0, newBuffer, this.bufsize - this.tokenBegin, this.bufpos);
				this.buffer = newBuffer;
				System.arraycopy(this.bufline, this.tokenBegin, newLineNumbers, 0, this.bufsize - this.tokenBegin);
				System.arraycopy(this.bufline, 0, newLineNumbers, this.bufsize - this.tokenBegin, this.bufpos);
				this.bufline = newLineNumbers;
				System.arraycopy(this.bufcolumn, this.tokenBegin, newColumnNumbers, 0, this.bufsize - this.tokenBegin);
				System.arraycopy(this.bufcolumn, 0, newColumnNumbers, this.bufsize - this.tokenBegin, this.bufpos);
				this.bufcolumn = newColumnNumbers;
				this.maxNextCharInd = this.bufpos = this.bufpos + (this.bufsize - this.tokenBegin);
			} else {
				System.arraycopy(this.buffer, this.tokenBegin, newBuffer, 0, this.bufsize - this.tokenBegin);
				this.buffer = newBuffer;
				System.arraycopy(this.bufline, this.tokenBegin, newLineNumbers, 0, this.bufsize - this.tokenBegin);
				this.bufline = newLineNumbers;
				System.arraycopy(this.bufcolumn, this.tokenBegin, newColumnNumbers, 0, this.bufsize - this.tokenBegin);
				this.bufcolumn = newColumnNumbers;
				this.maxNextCharInd = this.bufpos = this.bufpos - this.tokenBegin;
			}
		} catch (Throwable copyException) {
			throw new Error(copyException.getMessage());
		}

		this.bufsize += 2048;
		this.available = this.bufsize;
		this.tokenBegin = 0;
	}

	public int getBeginColumn() {
		return this.bufcolumn[this.tokenBegin];
	}
}
