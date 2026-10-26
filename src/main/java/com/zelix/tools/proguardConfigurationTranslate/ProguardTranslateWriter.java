package com.zelix.tools.proguardConfigurationTranslate;

import com.zelix.framework.AbstractClassNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class ProguardTranslateWriter {
	private final Map<String, String> packageMappings = new TreeMap<>();
	private final Map<String, String> classMappings = new LinkedHashMap<>();
	private final Set<String> warnings = new LinkedHashSet<>();
	private final MultiMap<String, String> fieldMappings = new MultiMap<>();
	private final MultiMap<String, String> methodMappings = new MultiMap<>();
	private final String sourceFileName;

	public ProguardTranslateWriter(String sourceFileName) {
		this.sourceFileName = sourceFileName;
	}

	private static String getPackagePrefix(String className, int packageDepth) {
		StringTokenizer classNameTokenizer = new StringTokenizer(className, ".");
		int nameTokenCount = classNameTokenizer.countTokens();
		StringBuilder prefixBuffer = new StringBuilder();

		for (int depthIndex = 0; depthIndex < packageDepth && depthIndex < nameTokenCount; depthIndex++) {
			if (prefixBuffer.length() > 0) {
				prefixBuffer.append(".");
			}

			prefixBuffer.append(classNameTokenizer.nextToken());
		}

		return prefixBuffer.toString();
	}

	public void addPackageMapping(String oldPackageName, String newPackageName) {
		String previousNewPackageName = this.packageMappings.put(oldPackageName, newPackageName);
		if (previousNewPackageName != null && !previousNewPackageName.equals(newPackageName)) {
			this.addWarning("Old package name '" + oldPackageName + "' is mapped to more than one new package name. : '" + previousNewPackageName + "' and '" + newPackageName + "'");
		}
	}

	public String buildTranslation() {
		StringBuilder outputBuffer = new StringBuilder(1000);
		outputBuffer.append("// Translated from '");
		outputBuffer.append(this.sourceFileName);
		outputBuffer.append("' by ");
		outputBuffer.append(CopyrightConstants.NAME);
		outputBuffer.append(' ');
		outputBuffer.append("version " + CopyrightConstants.VERSION + "");
		outputBuffer.append(' ');
		outputBuffer.append(GlobalUtils.getCurrentTime());
		outputBuffer.append(Options.lineSeparator);
		outputBuffer.append(Options.lineSeparator);
		if (this.warnings.size() > 0) {
			ArrayList<String> warningLines = new ArrayList<>(this.warnings.size() + 2);
			warningLines.add(this.warnings.size() + " warnings reported during ProGuard mapping file translation.");
			warningLines.add("");
			warningLines.addAll(this.warnings);
			String[] warningLinesArray = new String[warningLines.size()];
			warningLines.toArray(warningLinesArray);
			outputBuffer.append(GlobalUtils.generateComment(false, warningLinesArray));
			outputBuffer.append(Options.lineSeparator);
		}

		if (this.packageMappings.size() > 0) {
			HashSet<String> seenPackages = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(this.packageMappings.size()));

			for (Entry<String, String> packageMapping : this.packageMappings.entrySet()) {
				String oldPackageName = packageMapping.getKey();
				String newPackageName = packageMapping.getValue();
				StringBuilder prefixBuffer = new StringBuilder();
				StringTokenizer packageNameTokenizer = new StringTokenizer(oldPackageName, ".");
				int packageTokenCount = packageNameTokenizer.countTokens();

				for (int packageLevel = 1; packageLevel <= packageTokenCount - 1; packageLevel++) {
					prefixBuffer.append(packageNameTokenizer.nextToken());
					String packagePrefix = prefixBuffer.toString();
					if (!seenPackages.contains(packagePrefix)) {
						this.writePackageMapping(packagePrefix, getPackagePrefix(newPackageName, packageLevel), outputBuffer);
						seenPackages.add(packagePrefix);
					}

					if (packageLevel <= packageTokenCount - 2) {
						prefixBuffer.append(".");
					}
				}

				this.writePackageMapping(oldPackageName, newPackageName, outputBuffer);
				seenPackages.add(oldPackageName);
			}

			outputBuffer.append(Options.lineSeparator);
		}

		if (this.classMappings.size() > 0) {
			for (Entry<String, String> classMapping : this.classMappings.entrySet()) {
				outputBuffer.append("Class: ");
				String oldClassName = classMapping.getKey();
				outputBuffer.append(oldClassName);
				if (oldClassName.equals(classMapping.getValue())) {
					outputBuffer.append('\t');
					outputBuffer.append("NameNotChanged");
				} else {
					outputBuffer.append("\t=>\t");
					outputBuffer.append(classMapping.getValue());
				}

				outputBuffer.append(Options.lineSeparator);
				this.writeMemberMappings(oldClassName, "\tFieldsOf: ", this.fieldMappings.get(oldClassName), outputBuffer);
				this.writeMemberMappings(oldClassName, "\tMethodsOf: ", this.methodMappings.get(oldClassName), outputBuffer);
				outputBuffer.append(Options.lineSeparator);
			}
		}

		return outputBuffer.toString();
	}

	public void addWarning(String message) {
		String warningLine = "WARNING: " + message;
		this.warnings.add(warningLine);
	}

	public void writeMethodMapping(String className, String returnType, String oldName, String newName, List<String> argumentTypes) {
		StringBuilder mappingBuffer = new StringBuilder();
		mappingBuffer.append(returnType);
		mappingBuffer.append(' ');
		mappingBuffer.append(oldName);
		mappingBuffer.append('(');
		if (argumentTypes != null) {
			int argumentCount = argumentTypes.size();

			for (int argumentIndex = 0; argumentIndex < argumentCount; argumentIndex++) {
				mappingBuffer.append(argumentTypes.get(argumentIndex));
				if (argumentIndex < argumentCount - 1) {
					mappingBuffer.append(',');
				}
			}
		}

		mappingBuffer.append(')');
		if (oldName.equals(newName)) {
			mappingBuffer.append('\t');
			mappingBuffer.append("NameNotChanged");
		} else {
			mappingBuffer.append("\t=>\t");
			mappingBuffer.append(newName);
		}

		this.methodMappings.put(className, mappingBuffer.toString());
	}

	public void addClassMapping(String oldClassName, String newClassName) {
		String previousNewClassName = this.classMappings.put(oldClassName, newClassName);
		if (previousNewClassName != null) {
			if (!previousNewClassName.equals(newClassName)) {
				this.addWarning("Old class name '" + oldClassName + "' is mapped to more than one new class name. : '" + previousNewClassName + "' and '" + newClassName + "'");
			} else {
				this.addWarning("Class mapping '" + oldClassName + " -> " + newClassName + "' appears more than once.");
			}
		}

		String oldPackageName = AbstractClassNode.getPrefixBeforeLast(oldClassName, '.');
		if (oldPackageName.length() > 0) {
			String newPackageName = AbstractClassNode.getPrefixBeforeLast(newClassName, '.');
			this.addPackageMapping(oldPackageName, newPackageName);
		}
	}

	private void writePackageMapping(String oldPackageName, String newPackageName, StringBuilder sb) {
		sb.append("Package: ");
		sb.append(oldPackageName);
		if (oldPackageName.equals(newPackageName)) {
			sb.append('\t');
			sb.append("NameNotChanged");
		} else {
			sb.append("\t=>\t");
			sb.append(newPackageName);
		}

		sb.append(Options.lineSeparator);
	}

	private void writeMemberMappings(String className, String label, List<String> entries, StringBuilder sb) {
		if (entries != null && entries.size() > 0) {
			sb.append(label);
			sb.append(className);
			sb.append(Options.lineSeparator);

			for (String memberMapping : entries) {
				if (memberMapping.indexOf("<init>") == -1 && memberMapping.indexOf("<clinit>") == -1) {
					sb.append('\t');
					sb.append('\t');
					sb.append(memberMapping);
					sb.append(Options.lineSeparator);
				}
			}
		}
	}

	public void writeFieldMapping(String className, String fieldType, String oldName, String newName) {
		StringBuilder mappingBuffer = new StringBuilder();
		mappingBuffer.append(fieldType);
		mappingBuffer.append(' ');
		mappingBuffer.append(oldName);
		if (oldName.equals(newName)) {
			mappingBuffer.append('\t');
			mappingBuffer.append("NameNotChanged");
		} else {
			mappingBuffer.append("\t=>\t");
			mappingBuffer.append(newName);
		}

		this.fieldMappings.put(className, mappingBuffer.toString());
	}
}
