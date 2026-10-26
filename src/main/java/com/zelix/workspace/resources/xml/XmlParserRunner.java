package com.zelix.workspace.resources.xml;

import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.dataset.AtomicInt;
import com.zelix.workspace.AbstractWorkspace;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class XmlParserRunner {
	public XmlParserRunner(String resourcePath, String xmlContent, AtomicInt bomBytesToSkip, AtomicInt bomLength, AtomicInt byteOrder, String encoding, XmlHandler handler) throws IOException {
		HashMap<ClassNode, String> classReferences = GlobalUtils.newHashMap(13);
		ByteArrayInputStream byteArrayStream = null;

		try {
			byteArrayStream = new ByteArrayInputStream(xmlContent.getBytes());
			new XmlParser(byteArrayStream, handler, encoding, bomBytesToSkip.get(), bomLength.get(), byteOrder.get(), classReferences, null, null, null, resourcePath);
		} finally {
			try {
				if (byteArrayStream != null) {
					byteArrayStream.close();
				}
			} catch (IOException ignored) {
			}
		}
	}

	public XmlParserRunner(
		ZipFile zipFile,
		ZipEntry zipEntry,
		AtomicInt bomBytesToSkip,
		AtomicInt bomLength,
		AtomicInt byteOrder,
		String encoding,
		AbstractWorkspace workspace,
		XmlHandler handler,
		Map<ClassNode, String> classReferences,
		Map<FieldNode, String> fieldReferences,
		Map<MethodNode, String> methodReferences,
		DualKeyMap<MethodNode, String, Object> resourceToClassReferences
	) throws IOException {
		InputStream inputStream = null;

		try {
			inputStream = zipFile.getInputStream(zipEntry);
			new XmlParser(inputStream, handler, encoding, bomBytesToSkip.get(), bomLength.get(), byteOrder.get(), classReferences, fieldReferences, methodReferences, resourceToClassReferences, FileUtils.getResolvedZipEntryPath(zipFile, zipEntry));
			handler.resolveDeferredReferences(fieldReferences, methodReferences, resourceToClassReferences);
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException ignored) {
			}
		}
	}

	public XmlParserRunner(
		File sourceFile,
		AtomicInt bomBytesToSkip,
		AtomicInt bomLength,
		AtomicInt byteOrder,
		String encoding,
		AbstractWorkspace workspace,
		XmlHandler handler,
		Map<ClassNode, String> classReferences,
		Map<FieldNode, String> fieldReferences,
		Map<MethodNode, String> methodReferences,
		DualKeyMap<MethodNode, String, Object> resourceToClassReferences
	) throws IOException {
		FileInputStream fileInputStream = null;

		try {
			fileInputStream = new FileInputStream(sourceFile);
			new XmlParser(fileInputStream, handler, encoding, bomBytesToSkip.get(), bomLength.get(), byteOrder.get(), classReferences, fieldReferences, methodReferences, resourceToClassReferences, sourceFile.getAbsolutePath());
			handler.resolveDeferredReferences(fieldReferences, methodReferences, resourceToClassReferences);
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
