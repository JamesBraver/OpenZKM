package com.zelix.workspace.writer;

import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.PathRemapper;
import com.zelix.utils.StringUtils;
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

public class PropertiesFileRemapper {
	public PropertiesFileRemapper(
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
		ReadOnlyMap<String, String> remappingMap,
		AtomicReference<byte[]> outputBytes
	) throws IOException {
		InputStream zipEntryInputStream = null;

		try {
			zipEntryInputStream = zipFile.getInputStream(zipEntry);
			String remappedPath = pathRemapper.remapPathWithCache(zipEntry.getName());
			ZipEntryWriter zipEntryWriter = new ZipEntryWriter(zipOutputStream, remappedPath, isDeflated);
			remapStream(zipEntryInputStream, zipEntryWriter.getOutputStream(), encoding, bytesToSkip.get(), bomLength.get(), bomByteOrder.get(), zipEntry.getName(), remappingMap, outputBytes);
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

	public PropertiesFileRemapper(
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
			remapStream(fileInputStream, zipEntryWriter.getOutputStream(), encoding, bytesToSkip.get(), bomLength.get(), bomByteOrder.get(), inputFile.getAbsolutePath(), remappingMap, outputBytes);
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

	public PropertiesFileRemapper(File inputFile, AtomicInt bytesToSkip, AtomicInt bomLength, AtomicInt bomByteOrder, String encoding, File outputFile, ReadOnlyMap<String, String> remappingMap) throws IOException {
		FileInputStream fileInputStream = null;

		FileOutputStream fileOutputStream;
		try {
			File parentDirectory = outputFile.getParentFile();
			if (parentDirectory != null && !parentDirectory.exists()) {
				parentDirectory.mkdirs();
			}

			fileInputStream = new FileInputStream(inputFile);
			fileOutputStream = new FileOutputStream(outputFile);
			remapStream(fileInputStream, fileOutputStream, encoding, bytesToSkip.get(), bomLength.get(), bomByteOrder.get(), inputFile.getAbsolutePath(), remappingMap, new AtomicReference<>());
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

	private static void remapStream(
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
			lineWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputBuffer, encoding)));

			String line;
			while ((line = lineReader.readLine()) != null) {
				int contentStartIndex = StringUtils.skipWhitespace(line, 0);
				StringBuilder lineBuffer = new StringBuilder(line.length());
				String strippedLine;
				if (contentStartIndex > 0) {
					lineBuffer.append(line.substring(0, contentStartIndex));
					strippedLine = line.substring(contentStartIndex);
				} else {
					strippedLine = line;
				}

				if (!strippedLine.isEmpty()) {
					if (strippedLine.charAt(0) != '#' && strippedLine.charAt(0) != '!') {
						boolean separatorSeen = false;
						StringTokenizer propertyTokenizer = new StringTokenizer(strippedLine, " \t=:\\!#", true);

						while (propertyTokenizer.hasMoreTokens()) {
							String propertyToken = propertyTokenizer.nextToken();
							if (propertyToken.length() == 1) {
								if (propertyToken.equals(":") || propertyToken.equals("=")) {
									separatorSeen = true;
								}

								lineBuffer.append(propertyToken);
							} else if (separatorSeen) {
								String internalClassName = GlobalUtils.binaryToInternalName(propertyToken);
								String mappedClassName = GlobalUtils.getMappedOrDefault(internalClassName, remappingMap);
								if (!internalClassName.equals(mappedClassName)) {
									lineBuffer.append(GlobalUtils.internalToBinaryName(mappedClassName));
								} else {
									lineBuffer.append(propertyToken);
								}
							} else {
								lineBuffer.append(propertyToken);
							}
						}
					} else {
						lineBuffer.append(strippedLine);
					}

					lineWriter.println(lineBuffer.toString());
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
