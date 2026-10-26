package com.zelix.utils;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

public class ZipFileWrapper extends ZipFile {
	public ZipFileWrapper(String name) throws IOException {
		super(name);
	}

	public ZipFileWrapper(File file) throws IOException {
		super(file);
	}

	@Override
	public void close() throws IOException {
		super.close();
	}
}
