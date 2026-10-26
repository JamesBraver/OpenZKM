package com.zelix.framework.manifest;

import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.logging.AbstractLogger;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.PathRemapper;
import com.zelix.utils.ZipEntryWriter;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.dataset.AtomicInt;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ManifestFile {
	private List<IndividualManifestSection> individualSections = new ArrayList<>();
	private String sourceZipPath;
	private boolean isDeflated;
	private MainManifestAttributes mainAttributes;

	public ManifestFile(ZipFile zipFile, ZipEntry zipEntry) throws IOException {
		InputStream hello = null;
		BufferedReader reader = null;
		this.sourceZipPath = zipFile.getName();
		this.isDeflated = zipEntry.getMethod() == 8;

		try {
			hello = zipFile.getInputStream(zipEntry);
			reader = FileUtils.createReaderWithBomDetection(hello, "UTF-8", null);
			this.mainAttributes = new MainManifestAttributes(reader);
			boolean endOfFile = this.mainAttributes.isEndOfFile();

			while (!endOfFile) {
				IndividualManifestSection section = new IndividualManifestSection(reader);
				if (!section.isEmpty()) {
					this.individualSections.add(section);
				}

				endOfFile = section.isEndOfFile();
			}
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ignoredReaderClose) {
				}
			} else if (hello != null) {
				try {
					hello.close();
				} catch (IOException ignoredStreamClose) {
				}
			}
		}
	}

	public static boolean isMultiReleaseVersionEntry(String resourceName, AtomicInt version) {
		version.set(-1);
		if (resourceName.startsWith("META-INF/versions/") && resourceName.length() > 18) {
			String versionPath = resourceName.substring(18);
			int slashIndex = versionPath.indexOf(47);
			if (slashIndex > 0) {
				String versionText = versionPath.substring(0, slashIndex);

				try {
					int parsedVersion = Integer.parseInt(versionText);
					version.set(parsedVersion);
					return true;
				} catch (NumberFormatException ignoredVersionParse) {
					return false;
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public static boolean hasDuplicateValues(ReadOnlyMap<String, String> remappingMap) {
		boolean duplicateFound = false;
		HashSet<String> seenValues = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(remappingMap.size()));

		for (Entry<String, String> entry : remappingMap.entrySet()) {
			if (!seenValues.add(entry.getValue())) {
				duplicateFound = true;
				break;
			}
		}

		return duplicateFound;
	}

	public boolean isMicroEdition() {
		return this.mainAttributes.isMicroEdition();
	}

	public boolean isCldc10() {
		return this.mainAttributes.isCldc10();
	}

	public String getMainAttributeValue(String attributeName) {
		return this.mainAttributes.getAttributeValue(attributeName);
	}

	public void collectAttributeReferences(String sourcePath, Map<ClassNode, String> classReferences, Map<MethodNode, String> entryPointMethods, Map<MethodNode, String> protectedStaticMethods) {
		this.mainAttributes.collectAttributeReferences(sourcePath, classReferences, entryPointMethods, protectedStaticMethods);

		for (int i = 0; i < this.individualSections.size(); i++) {
			this.individualSections.get(i).collectAttributeReferences(sourcePath, classReferences);
		}
	}

	public void write(
		ZipOutputStream zipOutput, Long globalTimestamp, ReadOnlyMap<String, String> packageRemappingMap, ReadOnlyMap<String, String> classRemappingMap, PathRemapper pathRemapper, boolean deflated, AbstractLogger logger
	) throws IOException {
		ZipEntryWriter manifestEntry = new ZipEntryWriter(zipOutput, "META-INF/MANIFEST.MF", deflated);
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(manifestEntry.getOutputStream(), "UTF-8"));
		boolean duplicateValuesFound = hasDuplicateValues(packageRemappingMap);
		this.mainAttributes.write(writer, packageRemappingMap, classRemappingMap, pathRemapper, this.sourceZipPath, duplicateValuesFound, logger);

		for (int i = 0; i < this.individualSections.size(); i++) {
			IndividualManifestSection section = this.individualSections.get(i);
			section.write(writer, packageRemappingMap, classRemappingMap, pathRemapper, this.sourceZipPath, duplicateValuesFound, logger);
		}

		writer.flush();
		manifestEntry.writeEntry(globalTimestamp);
	}

	public boolean isDeflated() {
		return this.isDeflated;
	}
}
