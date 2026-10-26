package com.zelix.ui.process;

import com.zelix.UIConfig;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.klassmaster.config.FilterConfig;
import com.zelix.klassmaster.config.ObfuscateConfig;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.config.ReferenceObfuscateConfig;
import com.zelix.klassmaster.config.RuleFilterConfig;
import com.zelix.klassmaster.config.TrimConfig;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.log.AbstractScriptLogger;
import com.zelix.klassmaster.script.statements.exclusion.ExcludeStatement;
import com.zelix.klassmaster.script.statements.exclusion.ObfuscateReferencesIncludeStatement;
import com.zelix.klassmaster.script.statements.exclusion.TrimExcludeStatement;
import com.zelix.repository.ClasspathManager;
import com.zelix.ui.GUI;
import com.zelix.utils.StringUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public abstract class ScriptWriterContext {
	public static final String continuationIndent = StringUtils.justificateString("", 76, 12, 32);
	public TrimConfig trimConfig = new TrimConfig(SystemConstants.userDirectory, "ZKM_TO.ser");
	public GUI gui;
	public ScriptContext scriptContext;
	public UIConfig uiConfig;
	public ObfuscateConfig obfuscateConfig;
	public String outputArchivePath;
	public ReferenceObfuscateConfig referenceObfuscateConfig;
	public ClasspathManager classpathManager;
	public boolean skipTrim;
	public ClasspathResource[] openResources;
	public FilterConfig excludeConfig;
	public boolean openResourcesChanged;
	public RuleFilterConfig trimExcludeConfig;
	public boolean skipTrimExclude;

	public ScriptWriterContext(GUI gui, ScriptContext scriptContext, UIConfig uiConfig) {
		this.gui = gui;
		this.scriptContext = scriptContext;
		this.uiConfig = uiConfig;
		gui.setEnabled(false);
		gui.setDisable(true);
		this.showStartDialog();
	}

	public abstract void restoreGui();

	public abstract void showObfuscateOptions();

	public abstract void promptOutputArchivePath(String defaultPath);

	public void handleStepResult(Integer stepResult) throws IOException {
		int stepResultCode = stepResult;
		this.referenceObfuscateConfig.saveToFile(SystemConstants.userDirectory, "ZKM_OB_REF.ser");
		if (stepResultCode == 1) {
			if (this.outputArchivePath == null) {
				this.promptOutputArchiveLastPath();
			} else {
				this.promptOutputArchivePath(this.outputArchivePath);
			}
		} else if (stepResultCode == 2 || stepResultCode == 5) {
			this.showObfuscateOptions();
		}
	}

	public abstract void promptOutputArchiveLastPath();

	public abstract void showStartDialog();

	public final String generateScript(String generatorName, ClasspathResource[] openResources) {
		StringBuilder scriptBuffer = new StringBuilder(1000);
		scriptBuffer.append(AbstractScriptLogger.generateHeaderComment(generatorName));
		String rawClasspath = this.classpathManager.getRawClasspath();
		if (rawClasspath != null && rawClasspath.length() > 0) {
			StringTokenizer classpathTokens = new StringTokenizer(rawClasspath, SystemConstants.pathSeparator);
			int classpathEntryCount = classpathTokens.countTokens();

			for (int classpathIndex = 0; classpathIndex < classpathEntryCount; classpathIndex++) {
				String classpathEntry = classpathTokens.nextToken();
				if (classpathIndex == 0) {
					scriptBuffer.append(Options.lineSeparator + StringUtils.justificateString("classpath", 76, 12, 32) + "\"" + classpathEntry + "\"");
				} else {
					scriptBuffer.append(StringUtils.justificateString("", 76, 12, 32) + "\"" + classpathEntry + "\"");
				}

				if (classpathIndex < classpathEntryCount - 1) {
					scriptBuffer.append(Options.lineSeparator);
				} else {
					scriptBuffer.append(";" + Options.lineSeparator);
				}
			}
		}

		if (openResources != null && openResources.length > 0) {
			scriptBuffer.append(Options.lineSeparator + StringUtils.justificateString("open", 76, 12, 32));
			boolean precedingLineWritten = false;
			if (!this.uiConfig.getOpenNestedArchives()) {
				scriptBuffer.append("openNestedArchives=false" + Options.lineSeparator);
				precedingLineWritten = true;
			}

			for (int resourceIndex = 0; resourceIndex < openResources.length; resourceIndex++) {
				if (precedingLineWritten) {
					scriptBuffer.append(continuationIndent);
				}

				scriptBuffer.append("\"" + openResources[resourceIndex].getResourceName() + "\"");
				if (resourceIndex < openResources.length - 1) {
					scriptBuffer.append(Options.lineSeparator);
				} else {
					scriptBuffer.append(";" + Options.lineSeparator);
				}

				precedingLineWritten = true;
			}
		}

		if (!this.skipTrimExclude && this.trimExcludeConfig != null) {
			List<String> trimExcludes = this.trimExcludeConfig.getParameters();
			if (trimExcludes.size() > 0) {
				scriptBuffer.append(TrimExcludeStatement.buildStatementText(trimExcludes, 12));
			}
		}

		if (!this.skipTrim && this.trimConfig != null) {
			ArrayList<String> trimOptions = new ArrayList<>();
			trimOptions.add("deleteSourceFileAttributes=" + this.trimConfig.deleteSourceFileAttributes);
			trimOptions.add("deleteDeprecatedAttributes=" + this.trimConfig.deleteDeprecatedAttributes);
			trimOptions.add("deleteAnnotationAttributes=" + this.trimConfig.deleteAnnotationAttributes);
			trimOptions.add("deleteExceptionAttributes=" + this.trimConfig.deleteExceptionAttributes);
			trimOptions.add("deleteUnknownAttributes=" + this.trimConfig.deleteUnknownAttributes);
			scriptBuffer.append(Options.lineSeparator + StringUtils.justificateString("trim", 76, 12, 32));

			for (int trimOptionIndex = 0; trimOptionIndex < trimOptions.size(); trimOptionIndex++) {
				if (trimOptionIndex > 0) {
					scriptBuffer.append(continuationIndent);
				}

				scriptBuffer.append(trimOptions.get(trimOptionIndex));
				if (trimOptionIndex < trimOptions.size() - 1) {
					scriptBuffer.append(Options.lineSeparator);
				}
			}

			scriptBuffer.append(";" + Options.lineSeparator);
		}

		List<String> excludes = this.excludeConfig.getParameters();
		if (excludes.size() > 0) {
			scriptBuffer.append(ExcludeStatement.buildStatementText(excludes, 12));
		}

		if (this.referenceObfuscateConfig != null) {
			List<String> referenceObfuscateExcludes = this.referenceObfuscateConfig.getParameters();
			if (referenceObfuscateExcludes.size() > 0) {
				scriptBuffer.append(ObfuscateReferencesIncludeStatement.buildStatementText(referenceObfuscateExcludes, ObfuscateReferencesIncludeStatement.getCommandNameLength() + 1));
			}
		}

		if (this.obfuscateConfig != null) {
			ArrayList<String> obfuscateOptions = new ArrayList<>();
			if (this.obfuscateConfig.enableChangeLogFile) {
				String changeLogFileInKeyword;
				if (this.obfuscateConfig.isLooseChangeLogFile) {
					changeLogFileInKeyword = "looseChangeLogFileIn";
				} else {
					changeLogFileInKeyword = "changeLogFileIn";
				}

				obfuscateOptions.add(changeLogFileInKeyword + "=\"" + this.obfuscateConfig.changeLogFileIn[0].getPath() + "\"");
			} else {
				obfuscateOptions.add("changeLogFileIn=\"\"");
			}

			if (this.obfuscateConfig.enableChangeLogOut) {
				obfuscateOptions.add("changeLogFileOut=\"" + this.obfuscateConfig.changeLogFileOut + "\"");
			} else {
				obfuscateOptions.add("changeLogFileOut=\"\"");
			}

			if (this.obfuscateConfig.keepInnerClassInfo == 0) {
				obfuscateOptions.add("keepInnerClassInfo=true");
			} else if (this.obfuscateConfig.keepInnerClassInfo == 2) {
				obfuscateOptions.add("keepInnerClassInfo=ifNameNotObfuscated");
			}

			if (this.obfuscateConfig.keepGenericsInformation == 1) {
				obfuscateOptions.add("keepGenericsInfo=false");
			}

			switch (this.obfuscateConfig.obfuscateControlFlow) {
				case 0:
					obfuscateOptions.add("obfuscateFlow=none");
					break;
				case 1:
					obfuscateOptions.add("obfuscateFlow=light");
					break;
				case 2:
					obfuscateOptions.add("obfuscateFlow=normal");
					break;
				case 3:
					obfuscateOptions.add("obfuscateFlow=aggressive");
					break;
				case 4:
					obfuscateOptions.add("obfuscateFlow=extraAggressive");
			}

			String exceptionObfuscationValue;
			switch (this.obfuscateConfig.exceptionObfuscation) {
				case 1:
					exceptionObfuscationValue = "light";
					break;
				case 2:
					exceptionObfuscationValue = "heavy";
					break;
				default:
					exceptionObfuscationValue = "none";
			}

			obfuscateOptions.add("exceptionObfuscation=" + exceptionObfuscationValue);
			switch (this.obfuscateConfig.encryptStringLiterals) {
				case 0:
					obfuscateOptions.add("encryptStringLiterals=none");
					break;
				case 1:
					obfuscateOptions.add("encryptStringLiterals=normal");
					break;
				case 2:
					obfuscateOptions.add("encryptStringLiterals=aggressive");
					break;
				case 3:
					obfuscateOptions.add("encryptStringLiterals=flowObfuscate");
					break;
				case 4:
					obfuscateOptions.add("encryptStringLiterals=enhanced");
			}

			switch (this.obfuscateConfig.encryptIntegerConstants) {
				case 0:
					obfuscateOptions.add("encryptIntegerConstants=none");
					break;
				case 1:
					obfuscateOptions.add("encryptIntegerConstants=normal");
					break;
				case 2:
					obfuscateOptions.add("encryptIntegerConstants=aggressive");
			}

			switch (this.obfuscateConfig.encryptLongConstants) {
				case 0:
					obfuscateOptions.add("encryptLongConstants=none");
					break;
				case 1:
					obfuscateOptions.add("encryptLongConstants=normal");
			}

			String mixedCaseClassNamesValue;
			switch (this.obfuscateConfig.mixedCaseClassNames.getType()) {
				case 0:
					mixedCaseClassNamesValue = "true";
					break;
				case 1:
					mixedCaseClassNamesValue = "false";
					break;
				default:
					mixedCaseClassNamesValue = "ifInArchive";
			}

			obfuscateOptions.add("mixedCaseClassNames=" + mixedCaseClassNamesValue);
			if (this.obfuscateConfig.aggressiveMethodRenaming) {
				obfuscateOptions.add("aggressiveMethodRenaming=true");
			}

			if (this.obfuscateConfig.randomizeObfuscation) {
				obfuscateOptions.add("randomize=true");
			}

			if (this.obfuscateConfig.collapsePackagesWithDefault) {
				obfuscateOptions.add("collapsePackagesWithDefault=\"" + this.obfuscateConfig.collapseTargetPackage + "\"");
			}

			if (!this.obfuscateConfig.legalIdentifiers) {
				obfuscateOptions.add("legalIdentifiers=false");
			}

			String localVariableTablesType;
			switch (this.obfuscateConfig.localVariableTables) {
				case 0:
					localVariableTablesType = "delete";
					break;
				case 1:
					localVariableTablesType = "keep";
					break;
				case 2:
					localVariableTablesType = "obfuscate";
					break;
				case 3:
					localVariableTablesType = "keepVisibleMethodParameters";
					break;
				case 4:
				default:
					localVariableTablesType = "keepVisibleMethodParametersIfNotObfuscated";
					break;
				case 5:
					localVariableTablesType = "keepMethodParametersIfNotObfuscated";
			}

			obfuscateOptions.add("localVariables=" + localVariableTablesType);
			switch (this.obfuscateConfig.lineNumbers) {
				case 0:
					obfuscateOptions.add("lineNumbers=delete");
					break;
				case 1:
					obfuscateOptions.add("lineNumbers=scramble");
					break;
				case 2:
					obfuscateOptions.add("lineNumbers=keep");
			}

			if (this.obfuscateConfig.newNamesPrefix != null && this.obfuscateConfig.newNamesPrefix.length() > 0) {
				obfuscateOptions.add("newNamesPrefix=\"" + this.obfuscateConfig.newNamesPrefix + "\"");
			}

			obfuscateOptions.add("autoReflectionHandling=" + (this.obfuscateConfig.autoReflectionHandling == 1 ? "normal" : "none"));
			obfuscateOptions.add("obfuscateReferences=" + (this.obfuscateConfig.obfuscateReferences == 1 ? "normal" : "none"));
			if (this.obfuscateConfig.obfuscateReferences != 0 && this.obfuscateConfig.obfuscateReferenceStructures != 1) {
				obfuscateOptions.add("obfuscateReferenceStructures=inReferencingClasses");
			}

			if (this.obfuscateConfig.methodParameterChanges != 0) {
				String methodParameterChangesValue;
				switch (this.obfuscateConfig.methodParameterChanges) {
					case 1:
						methodParameterChangesValue = "normal";
						break;
					case 2:
						methodParameterChangesValue = "random";
						break;
					case 3:
						methodParameterChangesValue = "flowObfuscate";
						break;
					default:
						methodParameterChangesValue = null;
				}

				obfuscateOptions.add("methodParameterChanges=" + methodParameterChangesValue);
			}

			if (this.obfuscateConfig.keepBalancedLocks) {
				obfuscateOptions.add("keepBalancedLocks=true");
			}

			if (this.obfuscateConfig.obfuscateParameters != 0) {
				obfuscateOptions.add("obfuscateParameters=normal");
			}

			if (!this.obfuscateConfig.preverify) {
				obfuscateOptions.add("preverify=false");
			}

			scriptBuffer.append(Options.lineSeparator + StringUtils.justificateString("obfuscate", 76, 12, 32));

			for (int obfuscateOptionIndex = 0; obfuscateOptionIndex < obfuscateOptions.size(); obfuscateOptionIndex++) {
				if (obfuscateOptionIndex > 0) {
					scriptBuffer.append(continuationIndent);
				}

				scriptBuffer.append(obfuscateOptions.get(obfuscateOptionIndex));
				if (obfuscateOptionIndex < obfuscateOptions.size() - 1) {
					scriptBuffer.append(Options.lineSeparator);
				}
			}

			scriptBuffer.append(";" + Options.lineSeparator);
		}

		if (this.outputArchivePath != null && this.outputArchivePath.length() > 0) {
			scriptBuffer.append(
				Options.lineSeparator + StringUtils.justificateString("saveAll", 76, 12, 32) + "archiveCompression=all \"" + this.outputArchivePath + "\";" + Options.lineSeparator
			);
		}

		return scriptBuffer.toString();
	}
}
