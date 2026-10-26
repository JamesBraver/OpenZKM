package com.zelix.workspace.index;

import com.zelix.exceptions.ZKMException;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.utils.GlobalUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class IndexListSection {
	private static String LEGACY_MARKER = "&";
	private ArrayList<String> entries = new ArrayList<>();
	private String sectionHeader;
	public boolean isEndOfFile;
	public boolean isPrimarySection;

	public IndexListSection(BufferedReader reader) throws IOException {
		boolean headerRead = false;

		String line;
		while ((line = reader.readLine()) != null) {
			if (line.equals("")) {
				if (headerRead) {
					return;
				}
			} else if (line.equals(LEGACY_MARKER)) {
				if (headerRead) {
					return;
				}
			} else if (!headerRead) {
				headerRead = true;
				this.sectionHeader = line;
			} else if (line.charAt(0) == ' ') {
				String lastEntry = this.entries.get(this.entries.size() - 1);
				String joinedEntry = lastEntry + line.substring(1, line.length() - 1);
				this.entries.add(joinedEntry);
			} else {
				this.entries.add(line);
			}
		}

		this.isEndOfFile = true;
	}

	public void setAsPrimarySection() {
		this.isPrimarySection = true;
	}

	private Set<String> resolveEntries(
		String sectionHeader, ArchiveNode archiveNode, Map<String, ArchiveNode> archiveMap, ReferenceGraph<ArchiveNode, String> rootEntryGraph, ReferenceGraph<ArchiveNode, String> directoryEntryGraph
	) {
		HashMap<String, ArchiveNode> candidatesByPath = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(archiveMap.size()));
		String archiveFullPath = archiveNode.getFullPath();
		if (this.isPrimarySection) {
			candidatesByPath.put(archiveFullPath, archiveNode);
		} else {
			for (Entry<String, ArchiveNode> archiveEntry : archiveMap.entrySet()) {
				String archiveEntryPath = archiveEntry.getKey();
				if (!archiveEntryPath.equals(archiveFullPath) && archiveEntryPath.endsWith(sectionHeader.replace("\\", "/"))) {
					candidatesByPath.put(archiveEntryPath, archiveEntry.getValue());
				}
			}
		}

		if (candidatesByPath.size() == 0) {
			return null;
		} else {
			ArchiveNode matchedArchive;
			if (candidatesByPath.size() > 1) {
				HashMap<String, ArchiveNode> matchesByPath = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(candidatesByPath.size()));

				label70:
				for (Entry<String, ArchiveNode> candidateEntry : candidatesByPath.entrySet()) {
					String candidateEntryPath = candidateEntry.getKey();
					ArchiveNode candidateArchive = candidateEntry.getValue();

					for (String entryName : this.entries) {
						if (entryName.indexOf("/") == -1 && !entryName.endsWith(".class")) {
							Set<String> candidateRootReferences = rootEntryGraph.getReferences(candidateArchive);
							if (!candidateRootReferences.contains(entryName)) {
								continue label70;
							}
						}
					}

					matchesByPath.put(candidateEntryPath, candidateArchive);
				}

				if (matchesByPath.size() == 0) {
					return null;
				}

				if (matchesByPath.size() > 1) {
					StringBuilder errorMessageBuilder = new StringBuilder();
					errorMessageBuilder.append("ERROR:");
					errorMessageBuilder.append(" More than one matching JAR file for name '");
					errorMessageBuilder.append(sectionHeader);
					errorMessageBuilder.append("' : '");
					int appendedPathCount = 0;
					Set<String> candidatePaths = matchesByPath.keySet();
					int candidatePathCount = candidatePaths.size();

					for (String candidatePath : candidatePaths) {
						errorMessageBuilder.append(candidatePath);
						errorMessageBuilder.append("'");
						if (++appendedPathCount < candidatePathCount) {
							errorMessageBuilder.append(", ");
						}
					}

					throw new ZKMException(errorMessageBuilder.toString());
				}

				matchedArchive = candidatesByPath.entrySet().iterator().next().getValue();
			} else {
				matchedArchive = candidatesByPath.entrySet().iterator().next().getValue();
			}

			TreeSet<String> distinctReferences = new TreeSet<>();
			Set<String> rootReferences = rootEntryGraph.getReferences(matchedArchive);
			if (rootReferences != null) {
				distinctReferences.addAll(rootReferences);
			}

			Set<String> directoryReferences = directoryEntryGraph.getReferences(matchedArchive);
			if (directoryReferences != null) {
				distinctReferences.addAll(directoryReferences);
			}

			return distinctReferences;
		}
	}

	public boolean isEmpty() {
		return this.entries.size() == 0;
	}

	public void writeResolvedSection(
		PrintWriter writer, ArchiveNode archiveNode, Map<String, ArchiveNode> archiveMap, ReferenceGraph<ArchiveNode, String> rootEntryGraph, ReferenceGraph<ArchiveNode, String> directoryEntryGraph
	) {
		writer.println(this.sectionHeader);
		Set<String> resolvedEntries = this.resolveEntries(this.sectionHeader, archiveNode, archiveMap, rootEntryGraph, directoryEntryGraph);
		if (resolvedEntries != null) {
			this.entries = new ArrayList<>(resolvedEntries);
		}

		for (String entryName : this.entries) {
			writer.println(entryName);
		}

		writer.println();
	}

	public boolean isEndOfFile() {
		return this.isEndOfFile;
	}
}
