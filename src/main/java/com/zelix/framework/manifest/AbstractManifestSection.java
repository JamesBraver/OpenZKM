package com.zelix.framework.manifest;

import com.zelix.exceptions.ZKMException;
import com.zelix.klassmaster.config.Options;
import com.zelix.logging.AbstractLogger;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.PathRemapper;
import com.zelix.utils.TempFileManager;
import com.zelix.utils.collections.ListValuedOrderedMap;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.dataset.AtomicBool;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

public abstract class AbstractManifestSection {
	private static String LEGACY_MARKER = "&";
	public ListValuedOrderedMap<String, String> attributes = new ListValuedOrderedMap<>();
	public boolean isEndOfFile;

	public AbstractManifestSection(BufferedReader reader) throws IOException {
		boolean contentSeen = false;
		String currentAttributeName = null;

		String nextLine;
		while ((nextLine = reader.readLine()) != null) {
			if (nextLine.length() == 0) {
				if (contentSeen) {
					return;
				}
			} else if (nextLine.equals(LEGACY_MARKER)) {
				if (contentSeen) {
					return;
				}
			} else {
				contentSeen = true;
				if (nextLine.charAt(0) == ' ') {
					this.attributes.add(currentAttributeName, nextLine);
				} else {
					currentAttributeName = this.parseAttribute(nextLine);
				}
			}
		}

		this.isEndOfFile = true;
	}

	private static String remapPackageName(String packageName, ReadOnlyMap<String, String> packageRemappingMap, AtomicBool remappedFlag) {
		return remapIdentifierPath(packageName, packageRemappingMap, true, remappedFlag);
	}

	public static String remapAttributeValue(
		String attributeName, String attributeValue, ReadOnlyMap<String, String> packageRemappingMap, ReadOnlyMap<String, String> classRemappingMap, AtomicBool collapsedFlag, String sourcePath, boolean warnOnCollapse, AbstractLogger logger
	) {
		String linePrefix = attributeName + ": ";
		collapsedFlag.set(false);
		String[] valueTokens = attributeValue.split("\\s*[,;:\"'=\\s]\\s*");
		AtomicBool remappedFlag = new AtomicBool();
		HashMap<String, String> remappedNameByToken = GlobalUtils.newHashMap();
		ArrayList<String> remappedTokens = new ArrayList<>();
		String[] valueTokenArray = valueTokens;
		int scanOffset = valueTokens.length;

		for (int tokenIndex = 0; tokenIndex < scanOffset; tokenIndex++) {
			String token = valueTokenArray[tokenIndex].trim();
			int dotIndex = token.indexOf(46);
			if (token.length() >= Options.minDottedNameLength && dotIndex > 0 && dotIndex < token.length() - 1 && token.indexOf(47) == -1 && isValidJavaIdentifierPath(token, ".")) {
				String remappedClassName = remapClassName(token, classRemappingMap, remappedFlag);
				if (remappedFlag.get()) {
					remappedNameByToken.put(token, remappedClassName);
					remappedTokens.add(token);
				} else {
					String remappedPackageName = remapPackageName(token, packageRemappingMap, remappedFlag);
					if (remappedFlag.get()) {
						remappedNameByToken.put(token, remappedPackageName);
						remappedTokens.add(token);
					}
				}
			}
		}

		ArrayList<String> valueSegments = new ArrayList<>();
		scanOffset = 0;

		for (String remappedToken : remappedTokens) {
			int tokenStart = attributeValue.indexOf(remappedToken, scanOffset);
			String textBeforeToken = attributeValue.substring(scanOffset, tokenStart);
			if (textBeforeToken.length() > 0) {
				valueSegments.add(textBeforeToken);
			}

			valueSegments.add(remappedToken);
			scanOffset = tokenStart + remappedToken.length();
		}

		if (scanOffset < attributeValue.length()) {
			String trailingText = attributeValue.substring(scanOffset, attributeValue.length());
			valueSegments.add(trailingText);
		}

		HashSet<String> seenTokens = GlobalUtils.newHashSet();
		HashSet<String> seenRemappedNames = GlobalUtils.newHashSet();
		StringBuilder valueBuilder = new StringBuilder();
		boolean mappedValueAppended = false;
		String pendingSegment = null;
		boolean emptyTargetDetected = false;
		boolean collapseDetected = false;
		boolean unmappedSegmentFound = false;
		int segmentCount = valueSegments.size();

		for (int segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++) {
			String segment = valueSegments.get(segmentIndex);
			if (remappedNameByToken.containsKey(segment)) {
				String mappedName = GlobalUtils.getMappedOrDefault(segment, remappedNameByToken);
				boolean tokenFirstSeen = seenTokens.add(segment);
				boolean duplicateTargetDetected = !seenRemappedNames.add(mappedName) && tokenFirstSeen && !Options.allowDuplicateManifestValues;
				collapseDetected = collapseDetected || duplicateTargetDetected;
				boolean targetEmpty = mappedName.length() == 0;
				emptyTargetDetected = emptyTargetDetected || targetEmpty;
				if (!targetEmpty && !duplicateTargetDetected) {
					if (pendingSegment != null && (pendingSegment.indexOf(59) > -1 || mappedValueAppended || valueBuilder.length() == 0)) {
						valueBuilder.append(pendingSegment);
						pendingSegment = null;
					}

					valueBuilder.append(mappedName);
					mappedValueAppended = true;
				}
			} else if (segmentIndex < segmentCount - 1) {
				unmappedSegmentFound = true;
				pendingSegment = segment;
			} else {
				if (pendingSegment != null && (pendingSegment.indexOf(59) > -1 || mappedValueAppended || valueBuilder.length() == 0)) {
					if (pendingSegment.trim().endsWith(",") && segment.trim().startsWith(",")) {
						int lastCommaIndex = pendingSegment.lastIndexOf(44);
						pendingSegment = pendingSegment.substring(0, lastCommaIndex) + (lastCommaIndex < pendingSegment.length() - 2 ? pendingSegment.substring(lastCommaIndex + 1) : "");
					}

					valueBuilder.append(pendingSegment);
					pendingSegment = null;
					mappedValueAppended = false;
				}

				valueBuilder.append(segment);
			}
		}

		String remappedValue = valueBuilder.toString();
		collapsedFlag.set(remappedValue.length() == 0 || remappedValue.length() == 1 && remappedValue.equals(";"));
		if (warnOnCollapse && unmappedSegmentFound && (emptyTargetDetected || collapseDetected)) {
			String warningMessage = "Package names were collapsed which may have caused invalid translation of the '"
				+ attributeName
				+ "' attribute line in manifest file in '"
				+ TempFileManager.resolveTempPathInString(sourcePath)
				+ "' : '"
				+ attributeValue
				+ (remappedValue.length() > 0 ? "' to '" + remappedValue + "'" : "'. Attribute now empty.");
			logger.warn("WARNING:", warningMessage);
		}

		return linePrefix + remappedValue;
	}

	protected static String remapClassName(String className, ReadOnlyMap<String, String> classRemappingMap) {
		return remapClassName(className, classRemappingMap, new AtomicBool());
	}

	public static String remapResourceName(String resourceName, ReadOnlyMap<String, String> classRemappingMap, PathRemapper pathRemapper) {
		if (resourceName.endsWith(".class")) {
			return pathRemapper.remapClassFile(resourceName, classRemappingMap);
		} else {
			return pathRemapper.isPathMapped(resourceName) ? pathRemapper.getExactRemappedPath(resourceName) : pathRemapper.remapPathWithCache(resourceName);
		}
	}

	private static String remapIdentifierPath(String name, ReadOnlyMap<String, String> remappingMap, boolean asDottedName, AtomicBool remappedFlag) {
		String slashedName = name.trim().replace('.', '/');
		String remappedName = remappingMap.get(slashedName);
		if (remappedName == null) {
			remappedName = slashedName;
			remappedFlag.set(false);
		} else {
			remappedFlag.set(true);
		}

		return asDottedName ? remappedName.replace('/', '.') : remappedName;
	}

	public static boolean isValidJavaIdentifierPath(String name, String delimiters) {
		StringTokenizer tokenizer = new StringTokenizer(name, delimiters);

		while (tokenizer.hasMoreTokens()) {
			if (!Character.isJavaIdentifierStart(tokenizer.nextToken().charAt(0))) {
				return false;
			}
		}

		return true;
	}

	public static String remapClassName(String className, ReadOnlyMap<String, String> classRemappingMap, AtomicBool remappedFlag) {
		boolean slashedForm;
		if (className.indexOf(47) > 0) {
			if (className.indexOf(46) != -1) {
				remappedFlag.set(false);
				return className;
			}

			slashedForm = true;
		} else {
			slashedForm = false;
		}

		boolean asDottedName = !slashedForm;
		return remapIdentifierPath(className, classRemappingMap, asDottedName, remappedFlag);
	}

	public abstract void write(
		PrintWriter writer,
		ReadOnlyMap<String, String> packageRemappingMap,
		ReadOnlyMap<String, String> classRemappingMap,
		PathRemapper pathRemapper,
		String sourcePath,
		boolean warnOnCollapse,
		AbstractLogger logger
	);

	public final boolean isEmpty() {
		return this.attributes.size() == 0;
	}

	public final String getRemappedAttribute(
		String attributeName, ReadOnlyMap<String, String> packageRemappingMap, ReadOnlyMap<String, String> classRemappingMap, AtomicBool collapsedFlag, String sourcePath, boolean warnOnCollapse, AbstractLogger logger
	) {
		String attributeValue = this.getAttributeValue(attributeName);
		return this.foldLine(remapAttributeValue(attributeName, attributeValue, packageRemappingMap, classRemappingMap, collapsedFlag, sourcePath, warnOnCollapse, logger));
	}

	public abstract String normalizeAttributeName(String attributeName);

	public final String getAttributeValue(String attributeName) {
		List<String> attributeValues = this.attributes.get(attributeName);
		if (attributeValues != null) {
			StringBuilder valueBuilder = new StringBuilder();

			for (int i = 0; i < attributeValues.size(); i++) {
				String valueLine = attributeValues.get(i);
				if (i > 0) {
					valueLine = valueLine.substring(1);
				}

				valueBuilder.append(valueLine);
			}

			return valueBuilder.toString();
		} else {
			return null;
		}
	}

	public final String getRawAttributeValue(String attributeName) {
		List<String> attributeValues = this.attributes.get(attributeName);
		if (attributeValues != null) {
			StringBuilder valueBuilder = new StringBuilder();

			for (int i = 0; i < attributeValues.size(); i++) {
				String valueLine = attributeValues.get(i);
				valueBuilder.append(valueLine);
				if (i < attributeValues.size() - 1) {
					valueBuilder.append(Options.lineSeparator);
				}
			}

			return valueBuilder.toString();
		} else {
			return null;
		}
	}

	public final boolean isEndOfFile() {
		return this.isEndOfFile;
	}

	public final String foldLine(String line) {
		int lineLength = line.length();
		if (lineLength <= 72) {
			return line;
		} else {
			int segmentEnd = 72;
			StringBuilder foldedLine = new StringBuilder(line.substring(0, 72));

			while (lineLength - segmentEnd > 71) {
				int segmentStart = segmentEnd;
				segmentEnd = segmentEnd + 72 - 1;
				foldedLine.append(Options.lineSeparator);
				foldedLine.append(' ');
				foldedLine.append(line.substring(segmentStart, segmentEnd));
			}

			if (segmentEnd < lineLength) {
				foldedLine.append(Options.lineSeparator);
				foldedLine.append(' ');
				foldedLine.append(line.substring(segmentEnd));
			}

			return foldedLine.toString();
		}
	}

	private String parseAttribute(String attributeLine) {
		int colonIndex = attributeLine.indexOf(":");
		if (colonIndex > 0 && colonIndex != attributeLine.length() - 1) {
			String attributeName = attributeLine.substring(0, colonIndex).trim();
			attributeName = this.normalizeAttributeName(attributeName);
			if (!this.attributes.containsKey(attributeName)) {
				String attributeValue = attributeLine.substring(colonIndex + 1);
				if (attributeValue.length() > 0 && attributeValue.charAt(0) == ' ') {
					attributeValue = attributeValue.substring(1);
				}

				this.attributes.add(attributeName, attributeValue);
				return attributeName;
			} else if (attributeName.equals("Name")) {
				throw new ZKMException("Two 'Name' headers in the one manifest section.  Header '" + attributeLine + "' must be preceded by a blank line.");
			} else {
				throw new ZKMException("Two '" + attributeName + "' headers in the one manifest section: '" + attributeLine + "'");
			}
		} else {
			throw new ZKMException("Invalid Manifest attribute '" + attributeLine + "' - " + GlobalUtils.toBracedHexList(attributeLine.getBytes()));
		}
	}
}
