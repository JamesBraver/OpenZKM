package com.zelix.workspace.index;

import com.zelix.exceptions.ZKMException;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.ZipEntryWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class IndexListProcessor {
	private final ArrayList<IndexListSection> sections = new ArrayList<>();
	private ArchiveNode archiveNode;
	private String archivePath;
	private boolean isDeflated;
	private final String versionHeader;

	public IndexListProcessor(ArchiveNode archiveNode, ZipFile zipFile, ZipEntry zipEntry) throws IOException {
		InputStream inputStream = null;
		BufferedReader reader = null;
		this.archiveNode = archiveNode;
		this.archivePath = zipFile.getName();
		this.isDeflated = zipEntry.getMethod() == 8;

		try {
			inputStream = zipFile.getInputStream(zipEntry);
			reader = FileUtils.createReaderWithBomDetection(inputStream, "UTF-8", null);
			this.versionHeader = reader.readLine();
			if (this.versionHeader == null) {
				throw new ZKMException("Invalid INDEX.LIST (1)");
			}

			if (!this.versionHeader.startsWith("JarIndex-Version: ")) {
				throw new ZKMException("Invalid INDEX.LIST (2)");
			}

			boolean endOfFile = false;

			while (!endOfFile) {
				IndexListSection section = new IndexListSection(reader);
				if (!section.isEmpty()) {
					if (this.sections.isEmpty()) {
						section.setAsPrimarySection();
					}

					this.sections.add(section);
				}

				endOfFile = section.isEndOfFile();
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ignoredReaderClose) {
				}
			} else if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException ignoredInputStreamClose) {
				}
			}
		}
	}

	public void writeRemappedIndex(
		ZipOutputStream zipOutputStream, Long timestamp, ReferenceGraph<ArchiveNode, String> rootEntryGraph, ReferenceGraph<ArchiveNode, String> directoryEntryGraph, boolean deflated
	) throws IOException {
		HashMap<String, ArchiveNode> archivesByPath = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(rootEntryGraph.size() + directoryEntryGraph.size()));

		for (ArchiveNode rootMember : rootEntryGraph.getMembers()) {
			archivesByPath.put(rootMember.getFullPath(), rootMember);
		}

		for (ArchiveNode directoryMember : directoryEntryGraph.getMembers()) {
			archivesByPath.put(directoryMember.getFullPath(), directoryMember);
		}

		ZipEntryWriter indexListEntry = new ZipEntryWriter(zipOutputStream, "META-INF/INDEX.LIST", deflated);
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(indexListEntry.getOutputStream(), "UTF-8"));
		writer.println(this.versionHeader);
		writer.println();

		for (IndexListSection section : this.sections) {
			section.writeResolvedSection(writer, this.archiveNode, archivesByPath, rootEntryGraph, directoryEntryGraph);
		}

		writer.println();
		writer.flush();
		indexListEntry.writeEntry(timestamp);
	}
}
