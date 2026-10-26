package com.zelix.utils;

import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IBytecodeProvider;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;

import java.io.File;
import java.util.HashMap;
import java.util.jar.Manifest;

public class DecompileUtils {
	public static String decompile(byte[] classBytes) {
		final StringBuilder result = new StringBuilder();
		IBytecodeProvider provider = (externalPath, internalPath) -> classBytes;
		IResultSaver saver = new IResultSaver() {
			@Override
			public void saveFolder(String path) {
			}

			@Override
			public void copyFile(String source, String path, String entryName) {
			}

			@Override
			public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
				result.append(content);
			}

			@Override
			public void createArchive(String path, String archiveName, Manifest manifest) {
			}

			@Override
			public void saveDirEntry(String path, String archiveName, String entryName) {
			}

			@Override
			public void copyEntry(String source, String path, String archiveName, String entry) {
			}

			@Override
			public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
			}

			@Override
			public void closeArchive(String path, String archiveName) {
			}
		};
		IFernflowerLogger logger = new IFernflowerLogger() {
			@Override
			public void writeMessage(String message, Severity severity) {
			}

			@Override
			public void writeMessage(String message, Severity severity, Throwable t) {
			}

			@Override
			public void writeMessage(String message, Throwable t) {
			}
		};
		Fernflower fernflower = new Fernflower(provider, saver, new HashMap<>(), logger);

		try {
			fernflower.addSource(new File("cb.class"));
			fernflower.decompileContext();
		} catch (Throwable exception) {
		}

		return result.toString();
	}
}
