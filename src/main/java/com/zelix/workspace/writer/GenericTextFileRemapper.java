package com.zelix.workspace.writer;

import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.PathRemapper;
import com.zelix.utils.ZipEntryWriter;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.utils.references.AtomicReference;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class GenericTextFileRemapper {
	public GenericTextFileRemapper(File inputFile, AtomicInt bytesToSkip, AtomicInt bomLength, AtomicInt bomByteOrder, String encoding, File outputFile, ReadOnlyMap<String, String> remappingMap) throws IOException {
		File parentDirectory = outputFile.getParentFile();
		if (parentDirectory != null && !parentDirectory.exists()) {
			parentDirectory.mkdirs();
		}

		FileInputStream fileInputStream = null;

		FileOutputStream fileOutputStream;
		try {
			fileInputStream = new FileInputStream(inputFile);
			fileOutputStream = new FileOutputStream(outputFile);
			this.remapStream(fileInputStream, fileOutputStream, encoding, bytesToSkip.get(), bomLength.get(), bomByteOrder.get(), inputFile.getAbsolutePath(), remappingMap, new AtomicReference<>());
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

	public GenericTextFileRemapper(
		ZipFile zipFile,
		ZipEntry zipEntry,
		Long timestamp,
		AtomicInt bytesToSkip,
		AtomicInt bomLength,
		AtomicInt bomByteOrder,
		String encoding,
		ZipOutputStream zipOutputStream,
		PathRemapper pathRemapper,
		boolean isDeflated,
		ReadOnlyMap<String, String> remappingMap
	) throws IOException {
		InputStream zipEntryInputStream = null;

		try {
			zipEntryInputStream = zipFile.getInputStream(zipEntry);
			String remappedPath = pathRemapper.remapPathWithCache(zipEntry.getName());
			ZipEntryWriter zipEntryWriter = new ZipEntryWriter(zipOutputStream, remappedPath, isDeflated);
			this.remapStream(zipEntryInputStream, zipEntryWriter.getOutputStream(), encoding, bytesToSkip.get(), bomLength.get(), bomByteOrder.get(), zipEntry.getName(), remappingMap, new AtomicReference<>());
			zipEntryWriter.writeEntry(timestamp);
		} finally {
			try {
				if (zipEntryInputStream != null) {
					zipEntryInputStream.close();
				}
			} catch (IOException ignored) {
			}
		}
	}

	public GenericTextFileRemapper(
		File inputFile,
		AtomicInt bytesToSkip,
		AtomicInt bomLength,
		AtomicInt bomByteOrder,
		String encoding,
		ZipOutputStream zipOutputStream,
		boolean isDeflated,
		ReadOnlyMap<String, String> remappingMap,
		AtomicReference<byte[]> outputBytes
	) throws IOException {
		FileInputStream fileInputStream = null;

		try {
			fileInputStream = new FileInputStream(inputFile);
			ZipEntryWriter zipEntryWriter = new ZipEntryWriter(zipOutputStream, inputFile.getName(), isDeflated);
			this.remapStream(fileInputStream, zipEntryWriter.getOutputStream(), encoding, bytesToSkip.get(), bomLength.get(), bomByteOrder.get(), inputFile.getAbsolutePath(), remappingMap, outputBytes);
			zipEntryWriter.writeEntry();
		} finally {
			try {
				if (fileInputStream != null) {
					fileInputStream.close();
				}
			} catch (IOException ignored) {
			}
		}
	}

	private void remapStream(
		InputStream inputStream, OutputStream outputStream, String encoding, int bytesToSkip, int bomLength, int bomByteOrder, String sourceName, ReadOnlyMap<String, String> remappingMap, AtomicReference<byte[]> outputBytes
	) throws IOException {
		FileUtils.readBytes(inputStream, bytesToSkip);
		BufferedReader lineReader = null;
		ByteArrayOutputStream outputBuffer = null;
		PrintWriter lineWriter = null;
		boolean cleanupRequired = false;

		try {
			cleanupRequired = true;
			lineReader = new BufferedReader(new InputStreamReader(inputStream, encoding));
			outputBuffer = new ByteArrayOutputStream();
			if (encoding.equals("UTF-16") && bomByteOrder == 0) {
				encoding = "UTF-16LE";
			}

			lineWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputBuffer, encoding)));

			String line;
			while ((line = lineReader.readLine()) != null) {
				int commentIndex = line.indexOf(35);
				String comment;
				String contentPart;
				if (commentIndex > -1) {
					contentPart = line.substring(0, commentIndex);
					comment = line.substring(commentIndex);
				} else {
					contentPart = line;
					comment = null;
				}

				StringTokenizer contentTokenizer = new StringTokenizer(contentPart, " []{},\t", true);

				while (contentTokenizer.hasMoreTokens()) {
					String contentToken = contentTokenizer.nextToken();
					if (contentToken.length() > 1) {
						String internalClassName = GlobalUtils.binaryToInternalName(contentToken);
						String mappedClassName = GlobalUtils.getMappedOrDefault(internalClassName, remappingMap);
						if (!internalClassName.equals(mappedClassName)) {
							lineWriter.print(GlobalUtils.internalToBinaryName(mappedClassName));
						} else {
							lineWriter.print(contentToken);
						}
					} else {
						lineWriter.print(contentToken);
					}
				}

				if (comment != null) {
					lineWriter.println(comment);
				} else {
					lineWriter.println("");
				}
			}

			lineWriter.close();
			lineWriter = null;
			byte[] remappedBytes = outputBuffer.toByteArray();
			outputBytes.set(remappedBytes);
			outputStream.write(remappedBytes);
			outputStream.flush();
			cleanupRequired = false;
		} finally {
			if (cleanupRequired) {
				if (lineReader != null) {
					try {
						lineReader.close();
					} catch (IOException ignoredReaderCloseInFinally) {
					}
				}

				if (lineWriter != null) {
					lineWriter.close();
				}

				try {
					if (outputBuffer != null) {
						outputBuffer.close();
					}
				} catch (IOException ignoredBufferCloseInFinally) {
				}
			}
		}

		if (lineReader != null) {
			try {
				lineReader.close();
			} catch (IOException ignoredReaderClose) {
			}
		}

		try {
			if (outputBuffer != null) {
				outputBuffer.close();
			}
		} catch (IOException ignoredBufferClose) {
		}
	}
}
