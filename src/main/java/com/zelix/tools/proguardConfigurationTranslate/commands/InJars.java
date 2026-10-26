package com.zelix.tools.proguardConfigurationTranslate.commands;

import com.zelix.tools.proguardConfigurationTranslate.ProguardScriptTranslator;
import com.zelix.tools.proguardConfigurationTranslate.base.KeepCommand;
import com.zelix.tools.proguardConfigurationTranslate.parser.ClassPathEntryNode;
import com.zelix.utils.FileUtils;
import com.zelix.utils.references.AtomicReference;
import java.util.Set;

public class InJars extends KeepCommand {
	public InJars(int commandId) {
		super(commandId);
	}

	private static String buildFilterExpression(Set<String> filters, AtomicReference<String> warningMessage) {
		StringBuilder expressionBuilder = new StringBuilder();
		int filterCount = filters.size();
		int filterIndex = 0;
		boolean previousNegated = false;
		String previousNegatedSuffix = null;
		expressionBuilder.append('{');

		for (String filter : filters) {
			String trimmedFilter = filter.trim();
			boolean negated = false;
			if (trimmedFilter.startsWith("!")) {
				negated = true;
				trimmedFilter = trimmedFilter.substring(1);
			}

			if (trimmedFilter.startsWith("**")) {
				trimmedFilter = trimmedFilter.substring(1);
			}

			if (filterIndex > 0) {
				if (previousNegated && (!negated || filterIndex != filterCount - 1)) {
					expressionBuilder.append(" && ");
					if (!getFilterSuffix(trimmedFilter).equals(previousNegatedSuffix)) {
						warningMessage.set(
							"File filters in ProGuard '-injars' command may be too complicated for translation : '"
								+ filter
								+ "' preceded by negated filter with '"
								+ previousNegatedSuffix
								+ "' suffix."
						);
					}
				} else {
					expressionBuilder.append(" || ");
				}
			}

			if (negated) {
				expressionBuilder.append("!");
				previousNegated = true;
				previousNegatedSuffix = getFilterSuffix(trimmedFilter);
			} else {
				previousNegated = false;
				previousNegatedSuffix = null;
			}

			expressionBuilder.append('"');
			expressionBuilder.append(trimmedFilter);
			expressionBuilder.append('"');
			filterIndex++;
		}

		expressionBuilder.append('}');
		return expressionBuilder.toString();
	}

	private static String getFilterSuffix(String filter) {
		int lastDotIndex = filter.lastIndexOf(".");
		return lastDotIndex > -1 && lastDotIndex < filter.length() - 1 ? filter.substring(lastDotIndex + 1) : "";
	}

	@Override
	public String getProguardCommandName() {
		return "-injars";
	}

	@Override
	protected void executeCommand(ProguardScriptTranslator scriptTranslator, int initialMessageCount, int initialWarningCount, int initialErrorCount) {
		this.getChildrenCount();
		ClassPathEntryNode classPathEntry = (ClassPathEntryNode)this.getChild(0);
		Set<String> jarPaths = classPathEntry.getJarPaths();
		int jarPathCount = jarPaths.size();
		int jarPathIndex = 0;

		for (String jarPath : jarPaths) {
			if (!FileUtils.isArchiveExtensionLenient(jarPath)) {
				char lastChar = jarPath.charAt(jarPath.length() - 1);
				if (lastChar != '*') {
					jarPath = jarPath + (lastChar != FileUtils.FILE_SEPARATOR_CHAR && lastChar != '/' && lastChar != '\\' ? FileUtils.FILE_SEPARATOR_CHAR : "") + "*";
				}
			}

			if (jarPathIndex == jarPathCount - 1 && classPathEntry.hasFileFilters()) {
				AtomicReference<String> warningMessage = new AtomicReference<>();
				String filterExpression = buildFilterExpression(classPathEntry.getFileFilters(), warningMessage);
				scriptTranslator.addOpenEntry(jarPath, filterExpression);
				if (!warningMessage.isNull()) {
					scriptTranslator.logWarning(warningMessage.get());
				}
			} else {
				scriptTranslator.addOpenEntry(jarPath, "");
			}

			jarPathIndex++;
		}

		if (classPathEntry.hasArchiveFilters()) {
			scriptTranslator.logWarning(
				"Archive level filters not supported in ProGuard '" + this.getProguardCommandName() + "' command : '" + classPathEntry.buildArchiveFilterString() + "' ignored in '" + classPathEntry.buildJarPathsAndFilters() + "'"
			);
		}
	}
}
