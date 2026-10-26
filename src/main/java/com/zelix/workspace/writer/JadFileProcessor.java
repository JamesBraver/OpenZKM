package com.zelix.workspace.writer;

import com.zelix.exceptions.InvalidJadAttributeException;
import com.zelix.exceptions.ZKMException;
import com.zelix.framework.manifest.AbstractManifestSection;
import com.zelix.framework.manifest.MainManifestAttributes;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.logging.AbstractLogger;
import com.zelix.utils.DebugUtils;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.IntegerUtils;
import com.zelix.utils.PathRemapper;
import com.zelix.utils.collections.OrderedMap;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.dataset.AtomicBool;
import com.zelix.workspace.ClassHierarchyNode;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Map;
import java.util.StringTokenizer;

public class JadFileProcessor {
	private static String SEPARATOR = "&";
	private OrderedMap<String, String> attributes = new OrderedMap<>();
	private String fullPath;
	private String fileName;
	private String searchKey;

	public JadFileProcessor(ClasspathResource jadFileResource) {
		if (jadFileResource == null) {
			throw new IllegalArgumentException("Null JAD file");
		} else {
			this.fullPath = jadFileResource.getFullPath();
			this.fileName = jadFileResource.getAsFile().getName();
			if (jadFileResource.getExpectedFinalHash() != null) {
				this.searchKey = jadFileResource.getExpectedFinalHash().toLowerCase();
			}

			BufferedReader jadReader = null;

			try {
				String encoding = Options.ZKM_JAD_ENCODING;
				if (encoding == null) {
					encoding = "UTF-8";
				}

				jadReader = FileUtils.createBufferedReader(this.fullPath, encoding);

				String attributeLine;
				while ((attributeLine = jadReader.readLine()) != null) {
					if (!attributeLine.equals("") && !attributeLine.equals(SEPARATOR)) {
						this.parseAttribute(attributeLine);
					}
				}

				this.checkRequiredAttribute("MIDlet-Name");
				this.checkRequiredAttribute("MIDlet-Jar-URL");
				this.checkRequiredAttribute("MIDlet-Jar-Size");
			} catch (IOException closeException) {
				throw new ZKMException("IOException while reading JAD : '" + this.fullPath + "' : " + closeException);
			} finally {
				if (jadReader != null) {
					try {
						jadReader.close();
					} catch (IOException readException) {
						throw new ZKMException("IOException while closing JAD : '" + this.fullPath + "' : " + readException);
					}
				}
			}
		}
	}

	public void writeRemappedJad(
		File outputFile, ReadOnlyMap<String, String> packageRemappingMap, ReadOnlyMap<String, String> classRemappingMap, PathRemapper pathRemapper, String sourcePath, AbstractLogger logger
	) {
		if (outputFile == null) {
			throw new IllegalArgumentException("Null output JAD file");
		} else {
			boolean packageClashDetected = false;
			HashSet<String> seenPackageNames = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(packageRemappingMap.size()));

			for (Entry<String, String> packageRemappingEntry : packageRemappingMap.entrySet()) {
				if (!seenPackageNames.add(packageRemappingEntry.getValue())) {
					packageClashDetected = true;
					break;
				}
			}

			PrintWriter jadWriter = null;

			try {
				String encoding = Options.ZKM_JAD_ENCODING;
				if (encoding == null) {
					encoding = "UTF-8";
				}

				jadWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), encoding)));
				Enumeration<String> attributeNameEnumeration = this.attributes.keysEnumeration();

				while (attributeNameEnumeration.hasMoreElements()) {
					String attributeName = attributeNameEnumeration.nextElement();
					String attributeValue = this.getAttribute(attributeName);
					if (attributeName.equals("MIDlet-Icon")) {
						jadWriter.println(attributeName + ": " + PathRemapper.remapDirectoryOfPath(attributeValue, packageRemappingMap));
					} else if (attributeName.startsWith("MIDlet-") && IntegerUtils.isInteger(attributeName.substring(7))) {
						jadWriter.println(MainManifestAttributes.remapMidletAttribute(attributeName, attributeValue, packageRemappingMap, classRemappingMap));
					} else {
						AtomicBool valueEmptied = new AtomicBool(false);
						String remappedAttributeValue = AbstractManifestSection.remapAttributeValue(attributeName, attributeValue, packageRemappingMap, classRemappingMap, valueEmptied, sourcePath, packageClashDetected, logger);
						if (!valueEmptied.get() || Options.ZKM_WRITE_EMPTY_MANIFEST_VALUES) {
							jadWriter.println(remappedAttributeValue);
						}
					}
				}

				jadWriter.println();
			} catch (IOException writeException) {
				throw new ZKMException("IOException while writing JAD : '" + outputFile.getAbsolutePath() + "' : " + writeException);
			} finally {
				if (jadWriter != null) {
					jadWriter.close();
				}
			}
		}
	}

	public String getJarUrl() {
		String jarUrl = this.attributes.get("MIDlet-Jar-URL");
		if (jarUrl != null) {
			int lastSeparatorIndex = jarUrl.lastIndexOf("/");
			String jarFileName;
			if (lastSeparatorIndex > -1) {
				if (lastSeparatorIndex >= jarUrl.length()) {
					throw new ZKMException("JAD file '" + this.fullPath + "' has an invalid MIDlet-Jar-URL value of '" + jarUrl + "'");
				}

				jarFileName = jarUrl.substring(lastSeparatorIndex + 1);
			} else {
				jarFileName = jarUrl;
			}

			int queryIndex = jarFileName.indexOf("?");
			if (queryIndex > -1) {
				jarFileName = jarFileName.substring(0, queryIndex);
			}

			return jarFileName;
		} else {
			throw new ZKMException("JAD file '" + this.fullPath + "' has no MIDlet-Jar-URL attribute");
		}
	}

	public String getFullPath() {
		return this.fullPath;
	}

	public String getFileName() {
		return this.fileName;
	}

	private String parseAttribute(String attributeLine) {
		int separatorIndex = attributeLine.indexOf(":");
		if (separatorIndex > 0 && separatorIndex != attributeLine.length() - 1) {
			String attributeName = attributeLine.substring(0, separatorIndex).trim();
			if (attributeName.equalsIgnoreCase("MIDlet-Name")) {
				attributeName = "MIDlet-Name";
			} else if (attributeName.equalsIgnoreCase("MIDlet-Jar-URL")) {
				attributeName = "MIDlet-Jar-URL";
			} else if (attributeName.equalsIgnoreCase("MIDlet-Jar-Size")) {
				attributeName = "MIDlet-Jar-Size";
			}

			if (this.attributes.containsKey(attributeName)) {
				throw new ZKMException("Two '" + attributeName + "' headers in the JAD: '" + attributeLine + "'");
			} else {
				String attributeValue = attributeLine.substring(separatorIndex + 1).trim();
				this.attributes.put(attributeName, attributeValue);
				return attributeName;
			}
		} else {
			throw new ZKMException("Invalid JAD attribute '" + attributeLine + "'");
		}
	}

	private void checkRequiredAttribute(String attributeName) {
		if (!this.attributes.containsKey(attributeName)) {
			throw new ZKMException("JAD file '" + this.fullPath + "' does not contain a " + attributeName + " attribute.");
		}
	}

	public String getAttribute(String attributeName) {
		return this.attributes.get(attributeName);
	}

	public long getJarSize() {
		String jarSizeText = this.attributes.get("MIDlet-Jar-Size");
		if (jarSizeText != null) {
			try {
				return Long.parseLong(jarSizeText);
			} catch (NumberFormatException parseException) {
				throw new InvalidJadAttributeException(
					"JAD file '" + this.fullPath + "' has an invalid MIDlet-Jar-Size value of '" + jarSizeText + "' : " + parseException.toString()
				);
			}
		} else {
			throw new InvalidJadAttributeException("JAD file '" + this.fullPath + "' has no MIDlet-Jar-Size attribute");
		}
	}

	public void setJarSize(long jarSize) {
		this.attributes.put("MIDlet-Jar-Size", String.valueOf(jarSize));
	}

	public void collectClassReferences(String archivePath, Map<ClassNode, String> classNameToArchivePathMap) {
		String locationSuffix = " in '" + archivePath + "'";
		Enumeration<String> attributeNameEnumeration = this.attributes.keysEnumeration();

		while (attributeNameEnumeration.hasMoreElements()) {
			String attributeName = attributeNameEnumeration.nextElement();
			if (attributeName.startsWith("MIDlet-") && IntegerUtils.isInteger(attributeName.substring(7))) {
				String midletAttributeValue = this.getAttribute(attributeName);
				StringTokenizer midletValueTokenizer = new StringTokenizer(midletAttributeValue, ",", true);
				int midletFieldIndex = 0;

				while (midletValueTokenizer.hasMoreTokens()) {
					String midletField = midletValueTokenizer.nextToken().trim();
					if (midletField.equals(",")) {
						midletFieldIndex++;
					} else {
						switch (midletFieldIndex) {
							case 0:
							case 1:
								break;
							case 2:
								ClassNode midletClassNode = ClassHierarchyNode.getWorkspaceClassNode(GlobalUtils.binaryToInternalName(midletField));
								if (midletClassNode != null) {
									classNameToArchivePathMap.put(midletClassNode, attributeName + locationSuffix);
								}
								break;
							default:
								DebugUtils.assertTrue(false, new String[]{"'" + attributeName + "' has invalid value '" + midletAttributeValue + "' : " + midletFieldIndex});
						}
					}
				}
			} else if (!attributeName.equals("MIDlet-Icon")) {
				String attributeValue = this.getAttribute(attributeName);
				StringTokenizer classCandidateTokenizer = new StringTokenizer(attributeValue, " ,", true);

				while (classCandidateTokenizer.hasMoreTokens()) {
					String classCandidate = classCandidateTokenizer.nextToken();
					if (classCandidate.length() >= Options.minDottedNameLength && classCandidate.indexOf(".") > -1 && classCandidate.indexOf("/") == -1) {
						int dotIndex = classCandidate.indexOf(".");
						if (dotIndex > 0 && dotIndex < classCandidate.length() - 1 && AbstractManifestSection.isValidJavaIdentifierPath(classCandidate, ".")) {
							ClassNode candidateClassNode = ClassHierarchyNode.getWorkspaceClassNode(GlobalUtils.binaryToInternalName(classCandidate));
							if (candidateClassNode != null) {
								classNameToArchivePathMap.put(candidateClassNode, attributeName + locationSuffix);
							}
						}
					}
				}
			}
		}
	}
}
