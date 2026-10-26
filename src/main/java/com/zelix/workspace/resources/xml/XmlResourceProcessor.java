package com.zelix.workspace.resources.xml;

import com.zelix.utils.FileUtils;
import com.zelix.utils.PathRemapper;
import com.zelix.utils.ZipEntryWriter;
import com.zelix.utils.dataset.AtomicInt;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class XmlResourceProcessor {
	public XmlResourceProcessor(
		ZipFile zipFile,
		ZipEntry zipEntry,
		Long lastModifiedTime,
		AtomicInt bomBytesToSkip,
		AtomicInt bomLength,
		AtomicInt byteOrder,
		String encoding,
		ZipOutputStream zipOutputStream,
		PathRemapper pathRemapper,
		XmlHandler handler,
		boolean deflated,
		boolean deleteXmlComments
	) throws IOException {
		InputStream inputStream = null;

		try {
			inputStream = zipFile.getInputStream(zipEntry);
			String remappedEntryName = pathRemapper.remapPathWithCache(zipEntry.getName());
			ZipEntryWriter outputZipEntry = new ZipEntryWriter(zipOutputStream, remappedEntryName, deflated);
			new XmlParser(inputStream, outputZipEntry.getOutputStream(), handler, encoding, bomBytesToSkip.get(), bomLength.get(), byteOrder.get(), FileUtils.getZipEntryPath(zipFile, zipEntry), deleteXmlComments);
			outputZipEntry.writeEntry(lastModifiedTime);
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException ignored) {
			}
		}
	}

	public XmlResourceProcessor(File sourceFile, AtomicInt bomBytesToSkip, AtomicInt bomLength, AtomicInt byteOrder, String encoding, File targetFile, XmlHandler handler, boolean deleteXmlComments) throws IOException {
		File targetDirectory = targetFile.getParentFile();
		if (targetDirectory != null && !targetDirectory.exists()) {
			targetDirectory.mkdirs();
		}

		FileInputStream fileInputStream = null;

		FileOutputStream fileOutputStream;
		try {
			fileInputStream = new FileInputStream(sourceFile);
			fileOutputStream = new FileOutputStream(targetFile);
			new XmlParser(fileInputStream, fileOutputStream, handler, encoding, bomBytesToSkip.get(), bomLength.get(), byteOrder.get(), sourceFile.getAbsolutePath(), deleteXmlComments);
		} finally {
			try {
				if (fileInputStream != null) {
					fileInputStream.close();
				}
			} catch (IOException ignoredInputClose) {
			}
		}

		try {
			if (fileOutputStream != null) {
				fileOutputStream.close();
			}
		} catch (IOException ignoredOutputClose) {
		}
	}

	public XmlResourceProcessor(
		File sourceFile, AtomicInt bomBytesToSkip, AtomicInt bomLength, AtomicInt byteOrder, String encoding, ZipOutputStream zipOutputStream, XmlHandler handler, boolean deflated, boolean deleteXmlComments
	) throws IOException {
		FileInputStream fileInputStream = null;

		try {
			fileInputStream = new FileInputStream(sourceFile);
			ZipEntryWriter outputZipEntry = new ZipEntryWriter(zipOutputStream, sourceFile.getName(), deflated);
			new XmlParser(fileInputStream, outputZipEntry.getOutputStream(), handler, encoding, bomBytesToSkip.get(), bomLength.get(), byteOrder.get(), sourceFile.getName(), deleteXmlComments);
			outputZipEntry.writeEntry();
		} finally {
			try {
				if (fileInputStream != null) {
					fileInputStream.close();
				}
			} catch (IOException ignored) {
			}
		}
	}
}
