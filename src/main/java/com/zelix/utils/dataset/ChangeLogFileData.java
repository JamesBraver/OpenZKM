package com.zelix.utils.dataset;

import java.io.Reader;
import java.io.Serializable;

public class ChangeLogFileData implements Serializable {
	private final String file;
	private transient Reader reader;

	public ChangeLogFileData(String file) {
		this.file = file;
	}

	public String getPath() {
		return this.file;
	}

	public void setReader(Reader reader) {
		this.reader = reader;
	}

	public Reader getReader() {
		return this.reader;
	}
}
