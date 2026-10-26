package com.zelix.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipOutputStream;

public class TemporaryZipWriter {
	public File temporaryZipFile;
	private ZipOutputStream zipStream;

	public boolean isStreamUninitialized() {
		return this.zipStream == null;
	}

	public void close() throws IOException {
		if (this.zipStream != null) {
			this.zipStream.close();
		}
	}

	public ZipOutputStream getStream() throws IOException {
		if (this.zipStream == null) {
			this.temporaryZipFile = TempFileManager.createTempFile();
			this.zipStream = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(this.temporaryZipFile)));
		}

		return this.zipStream;
	}

	public String getTempFilePath() {
		return this.temporaryZipFile != null ? this.temporaryZipFile.getAbsolutePath() : null;
	}
}
