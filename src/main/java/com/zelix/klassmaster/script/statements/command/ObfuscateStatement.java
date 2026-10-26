package com.zelix.klassmaster.script.statements.command;

import com.zelix.enums.MixCaseType;
import com.zelix.klassmaster.config.ObfuscateConfig;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.exclusion.*;
import com.zelix.logging.AbstractLogger;
import com.zelix.utils.function.EmptyConsumer;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.PackageManager;
import com.zelix.workspace.Workspace;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class ObfuscateStatement extends ChangeLogFile {
	private Boolean allowMethodParameterChanges = null;
	private boolean hasMethodParameterChanges = false;

	public ObfuscateStatement(int nodeType) {
		super(nodeType);
	}

	protected void applyAllowParameterChanges(ObfuscateConfig config) {
		List<String> parameterValues = super.parameterValues.get("allowMethodParameterChanges");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null) {
				if (parameterValue.equals("true")) {
					this.allowMethodParameterChanges = Boolean.TRUE;
				} else {
					this.allowMethodParameterChanges = Boolean.FALSE;
				}
			}
		}
	}

	protected void applyLineNumbers(ObfuscateConfig config) {
		config.lineNumbers = 0;
		List<String> parameterValues = super.parameterValues.get("lineNumbers");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null) {
				if (parameterValue.equals("keep")) {
					config.lineNumbers = 2;
				} else if (parameterValue.equals("scramble")) {
					config.lineNumbers = 1;
				}
			}
		}
	}

	protected void applyUniqueClassNames(ObfuscateConfig config) {
		config.uniqueClassNames = false;
		List<String> parameterValues = super.parameterValues.get("uniqueClassNames");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null) {
				if (parameterValue.equals("true")) {
					config.uniqueClassNames = true;
				} else {
					config.uniqueClassNames = false;
				}
			}
		}
	}

	protected void applyKeepGenericsInfo(ObfuscateConfig config) {
		config.keepGenericsInformation = 0;
		List<String> parameterValues = super.parameterValues.get("keepGenericsInfo");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null && parameterValue.equals("false")) {
				config.keepGenericsInformation = 1;
			}
		}
	}

	protected void applyEncryptIntegerConstants(ObfuscateConfig config) {
		config.encryptIntegerConstants = 0;
		List<String> parameterValues = super.parameterValues.get("encryptIntegerConstants");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null) {
				if (parameterValue.equals("none")) {
					config.encryptIntegerConstants = 0;
				} else if (parameterValue.equals("normal")) {
					config.encryptIntegerConstants = 1;
				} else if (parameterValue.equals("aggressive")) {
					config.encryptIntegerConstants = 2;
				}
			}
		}
	}

	protected void applyMixedCaseClassNames(ObfuscateConfig config) {
		config.mixedCaseClassNames = MixCaseType.IF_IN_ARCHIVE;
		List<String> parameterValues = super.parameterValues.get("mixedCaseClassNames");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null) {
				if (parameterValue.equals("true")) {
					config.mixedCaseClassNames = MixCaseType.ALWAYS;
				} else if (parameterValue.equals("false")) {
					config.mixedCaseClassNames = MixCaseType.NEVER;
				}
			}
		}
	}

	protected void applyMakeClassesPublic(ObfuscateConfig config) {
		config.makeClassesPublic = false;
		List<String> parameterValues = super.parameterValues.get("makeClassesPublic");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null && parameterValue.equals("true")) {
				config.makeClassesPublic = true;
			}
		}
	}

	protected void applyReferenceStructures(ObfuscateConfig config) {
		config.obfuscateReferenceStructures = 1;
		List<String> parameterValues = super.parameterValues.get("obfuscateReferenceStructures");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null && parameterValue.equals("inReferencingClasses")) {
				config.obfuscateReferenceStructures = 0;
			}
		}
	}

	protected void applyPreverify(ObfuscateConfig config) {
		config.preverify = true;
		List<String> parameterValues = super.parameterValues.get("preverify");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null && parameterValue.equals("false")) {
				config.preverify = false;
			}
		}
	}

	@Override
	public String getCommandName() {
		return "obfuscate";
	}

	protected void applyLocalVariables(ObfuscateConfig config) {
		config.localVariableTables = 0;
		List<String> parameterValues = super.parameterValues.get("localVariables");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null) {
				if (parameterValue.equals("obfuscate")) {
					config.localVariableTables = 2;
				} else if (parameterValue.equals("keep")) {
					config.localVariableTables = 1;
				} else if (parameterValue.equals("keepVisibleMethodParameters")) {
					config.localVariableTables = 3;
				} else if (parameterValue.equals("keepVisibleMethodParametersIfNotObfuscated")) {
					config.localVariableTables = 4;
				} else if (parameterValue.equals("keepMethodParametersIfNotObfuscated")) {
					config.localVariableTables = 5;
				}
			}
		}
	}

	protected void applyKeepBalancedLocks(ObfuscateConfig config) {
		config.keepBalancedLocks = false;
		List<String> parameterValues = super.parameterValues.get("keepBalancedLocks");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null && parameterValue.equals("true")) {
				config.keepBalancedLocks = true;
			}
		}
	}

	protected void applyMethodParameters(ObfuscateConfig config) {
		config.methodParameterOption = 0;
		List<String> parameterValues = super.parameterValues.get("methodParameters");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null) {
				if (parameterValue.equals("delete")) {
					config.methodParameterOption = 0;
				} else if (parameterValue.equals("obfuscate")) {
					config.methodParameterOption = 2;
				} else if (parameterValue.equals("keep")) {
					config.methodParameterOption = 1;
				} else if (parameterValue.equals("keepVisible")) {
					config.methodParameterOption = 3;
				} else if (parameterValue.equals("keepVisibleIfNotObfuscated")) {
					config.methodParameterOption = 4;
				} else if (parameterValue.equals("keepIfNotObfuscated")) {
					config.methodParameterOption = 5;
				}
			}
		}
	}

	protected void applyAutoReflectionHandling(ObfuscateConfig config) {
		config.autoReflectionHandling = 0;
		List<String> parameterValues = super.parameterValues.get("autoReflectionHandling");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null && parameterValue.equals("normal")) {
				config.autoReflectionHandling = 1;
			}
		}
	}

	protected void applyObfuscateParameters(ObfuscateConfig config) {
		config.obfuscateParameters = 0;
		List<String> parameterValues = super.parameterValues.get("obfuscateParameters");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null) {
				if (parameterValue.equals("none")) {
					config.obfuscateParameters = 0;
				} else if (parameterValue.equals("normal")) {
					config.obfuscateParameters = 1;
				}
			}
		}
	}

	protected void applyHideFieldNames(ObfuscateConfig config) {
		config.thideFieldNames = false;
		List<String> parameterValues = super.parameterValues.get("hideFieldNames");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null && parameterValue.equals("true")) {
				config.thideFieldNames = true;
			}
		}
	}

	protected void applyLegalIdentifiers(ObfuscateConfig config) {
		config.legalIdentifiers = true;
		List<String> parameterValues = super.parameterValues.get("legalIdentifiers");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null && parameterValue.equals("false")) {
				config.legalIdentifiers = false;
			}
		}
	}

	protected void applyHideStaticMethodNames(ObfuscateConfig config) {
		config.hideStaticMethodNames = false;
		List<String> parameterValues = super.parameterValues.get("hideStaticMethodNames");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null && parameterValue.equals("true")) {
				config.hideStaticMethodNames = true;
			}
		}
	}

	@Override
	protected void applyStatementConfig(ObfuscateConfig config, ScriptContext scriptContext) {
		this.applyLegalIdentifiers(config);
		this.applyHideFieldNames(config);
		this.applyHideStaticMethodNames(config);
		this.applyObfuscateFlow(config);
		this.applyObfuscateParameters(config);
		this.applyEncryptStringLiterals(config);
		this.applyEncryptIntegerConstants(config);
		this.applyEncryptLongConstants(config);
		this.applyExceptionObfuscation(config);
		this.applyLineNumbers(config);
		this.applyLocalVariables(config);
		this.applyMethodParameters(config);
		this.applyCollapsePackages(config, scriptContext);
		this.applyKeepGenericsInfo(config);
		config.newNamesPrefix = this.getParameterValue("newNamesPrefix");
		this.applyDeriveGroupings(config);
		this.applyAutoReflectionHandling(config);
		this.applyObfuscateReferences(config);
		this.applyReferenceStructures(config);
		config.obfuscateReferencesPackage = this.getParameterValue("obfuscateReferencesPackage");
		config.autoReflectionPackage = this.getParameterValue("autoReflectionPackage");
		config.autoReflectionHash = this.getParameterValue("autoReflectionHash");
		config.newPackageNameFile = this.getParameterValue("newPackageNameFile");
		config.newClassNameFile = this.getParameterValue("newClassNameFile");
		config.newFieldNameFile = this.getParameterValue("newFieldNameFile");
		config.newMethodNameFile = this.getParameterValue("newMethodNameFile");
		this.applyNewNameCharacters(config);
		this.applyPreverify(config);
		this.applyMakeClassesPublic(config);
		this.applyKeepBalancedLocks(config);
		this.applyMixedCaseClassNames(config);
		this.applyUniqueClassNames(config);
		this.applyUniqueMethodNames(config);
		this.applyAllowParameterChanges(config);
		this.applyMethodParameterChanges(config);
		config.methodParameterChangesPackage = this.getParameterValue("methodParameterChangesPackage");
		if (this.allowMethodParameterChanges != null) {
			if (this.hasMethodParameterChanges) {
				if (this.allowMethodParameterChanges) {
					if (config.methodParameterChanges == 0) {
						scriptContext.logError(
							"\"allowMethodParameterChanges\" set to \"true\" but \"methodParameterChanges\" set to \"none\". The \"methodParameterChanges\" will take precedence. The \"allowMethodParameterChanges\" clause is deprecated."
						);
					}
				} else if (config.methodParameterChanges != 0) {
					scriptContext.logError(
						"\"allowMethodParameterChanges\" set to \"false\" but \"methodParameterChanges\" set to \""
							+ (config.methodParameterChanges == 1 ? "normal" : (config.methodParameterChanges == 2 ? "random" : "flowObfuscate"))
							+ "\". The \"methodParameterChanges\" will take precedence. The \"allowMethodParameterChanges\" clause is deprecated."
					);
				}
			} else if (this.allowMethodParameterChanges) {
				config.methodParameterChanges = 1;
			} else {
				config.methodParameterChanges = 0;
			}
		}

		if (config.autoReflectionHash != null && config.autoReflectionHash.length() > 0) {
			try {
				MessageDigest.getInstance(config.autoReflectionHash);
			} catch (NoSuchAlgorithmException ignoredHashAlgorithmLookup) {
				scriptContext.logSpecialError(
					"Hash algorithm \""
						+ config.autoReflectionHash
						+ "\" specified in \"autoReflectionHash\" parameter in \""
						+ this.getCommandName()
						+ "\" statement at line "
						+ this.getLineNumber()
						+ " could not be found."
				);
			}
		}

		config.assumeRuntimeVersionString = this.getParameterValue("assumeRuntimeVersion");
		if (config.assumeRuntimeVersionString != null) {
			String runtimeVersion = config.assumeRuntimeVersionString.trim();
			if (runtimeVersion.length() > 0) {
				String[] versionParts = runtimeVersion.split("\\.");

				try {
					config.assumeRuntimeVersionInteger = Integer.parseInt(versionParts[0]);
				} catch (NumberFormatException ignoredRuntimeVersionParse) {
					scriptContext.logError("\"assumeRuntimeVersion\" must be numeric : \"" + runtimeVersion + "\".");
				}
			}
		}

		if (!config.allClassesOpened) {
			if (config.isLooseChangeLogFile) {
				config.allClassesOpened = true;
				scriptContext.logError(
					"\"allClassesOpened\" is set to \"false\" when \"looseChangeLogFileIn\" is present in \""
						+ this.getCommandName()
						+ "\" statement at line "
						+ this.getLineNumber()
						+ ". This is not supported so \"allClassesOpened\" will be set to \"true\"."
				);
			} else if (config.changeLogFileIn == null || config.changeLogFileIn.length == 0) {
				config.allClassesOpened = true;
				scriptContext.logWarning(
					"\"allClassesOpened\" is set to \"false\" but no \"changeLogFileIn\" is present in \""
						+ this.getCommandName()
						+ "\" statement at line "
						+ this.getLineNumber()
						+ ". \"allClassesOpened\" will be set to \"true\"."
				);
			}
		}

		if (!config.allClassesOpened) {
			if (config.autoReflectionHandling == 1) {
				config.autoReflectionHandling = 0;
				scriptContext.logError(
					"\"autoReflectionHandling\" is set to \"normal\" while \"allClassesOpened\" is set to \"false\" in \""
						+ this.getCommandName()
						+ "\" statement at line "
						+ this.getLineNumber()
						+ ". This is not supported so \"autoReflectionHandling\" will be set to \"none\"."
				);
			}

			if (config.obfuscateReferences == 1) {
				config.obfuscateReferences = 0;
				scriptContext.logError(
					"\"obfuscateReferences\" is set to \"normal\" while \"allClassesOpened\" is set to \"false\" in \""
						+ this.getCommandName()
						+ "\" statement at line "
						+ this.getLineNumber()
						+ ". This is not supported so \"obfuscateReferences\" will be set to \"none\"."
				);
			}

			if (config.obfuscateParameters == 1) {
				config.obfuscateParameters = 0;
				scriptContext.logError(
					"\"obfuscateParameters\" is set to \"normal\" while \"allClassesOpened\" is set to \"false\" in \""
						+ this.getCommandName()
						+ "\" statement at line "
						+ this.getLineNumber()
						+ ". This is not supported so \"obfuscateParameters\" will be set to \"none\"."
				);
			}

			if (config.methodParameterChanges != 0) {
				if (this.allowMethodParameterChanges != null && this.hasMethodParameterChanges) {
					scriptContext.logError(
						"\"allowMethodParameterChanges\" is set to \"true\" while \"allClassesOpened\" is set to \"false\" in \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ ". This is not supported so \"allowMethodParameterChanges\" will be set to \"false\"."
					);
				} else {
					scriptContext.logError(
						"\"methodParameterChanges\" set to \""
							+ (config.methodParameterChanges == 1 ? "normal" : "random")
							+ "\" while \"allClassesOpened\" is set to \"false\" in \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ ". This is not supported so \"methodParameterChanges\" will be set to \"none\"."
					);
				}

				config.methodParameterChanges = 0;
			}
		} else {
			if (config.autoReflectionHandling == 0) {
				if (config.autoReflectionPackage != null && config.autoReflectionPackage.length() > 0) {
					scriptContext.logWarning(
						"\"autoReflectionHandling\" is set to \"none\" but \"autoReflectionPackage\" has a value of \""
							+ config.autoReflectionPackage
							+ "\" in \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ ". The \"autoReflectionPackage\" parameter will be ignored."
					);
				}

				if (config.autoReflectionHash != null && config.autoReflectionHash.length() > 0) {
					scriptContext.logWarning(
						"\"autoReflectionHandling\" is set to \"none\" but \"autoReflectionHash\" has a value of \""
							+ config.autoReflectionHash
							+ "\" in \""
							+ this.getCommandName()
							+ "\" statement at line "
							+ this.getLineNumber()
							+ ". The \"autoReflectionHash\" parameter will be ignored."
					);
				}
			}

			if (config.obfuscateReferences == 0 && config.obfuscateReferencesPackage != null && config.obfuscateReferencesPackage.length() > 0) {
				scriptContext.logWarning(
					"\"obfuscateReferences\" is set to \"none\" but \"obfuscateReferencesPackage\" has a value of \""
						+ config.obfuscateReferencesPackage
						+ "\" in \""
						+ this.getCommandName()
						+ "\" statement at line "
						+ this.getLineNumber()
						+ ". The \"obfuscateReferencesPackage\" parameter will be ignored."
				);
			}
		}

		if (config.obfuscateControlFlow == 4) {
			scriptContext.logWarning("\"obfuscateFlow\" is set to \"extraAggressive\" which is now deprecated because of JVM bugs.  It will be set to \"aggressive\" instead.");
			config.obfuscateControlFlow = 3;
		}

		if (config.uniqueMethodNames && config.aggressiveMethodRenaming) {
			scriptContext.logWarning(
				"Both \"uniqueMethodNames\" and \"aggressiveMethodRenaming\" are set to \"true\" in \""
					+ this.getCommandName()
					+ "\" statement at line "
					+ this.getLineNumber()
					+ " which is contradictory. So \"aggressiveMethodRenaming\" will be set to \"false\" instead."
			);
			config.aggressiveMethodRenaming = false;
		}
	}

	protected void applyEncryptStringLiterals(ObfuscateConfig config) {
		config.encryptStringLiterals = 4;
		List<String> parameterValues = super.parameterValues.get("encryptStringLiterals");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null) {
				if (parameterValue.equals("none") || parameterValue.equals("false")) {
					config.encryptStringLiterals = 0;
				} else if (parameterValue.equals("normal")) {
					config.encryptStringLiterals = 1;
				} else if (parameterValue.equals("aggressive")) {
					config.encryptStringLiterals = 2;
				} else if (parameterValue.equals("flowObfuscate")) {
					config.encryptStringLiterals = 3;
				}
			}
		}
	}

	protected void applyUniqueMethodNames(ObfuscateConfig config) {
		config.uniqueMethodNames = false;
		List<String> parameterValues = super.parameterValues.get("uniqueMethodNames");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null) {
				if (parameterValue.equals("true")) {
					config.uniqueMethodNames = true;
				} else {
					config.uniqueMethodNames = false;
				}
			}
		}
	}

	protected void applyObfuscateReferences(ObfuscateConfig config) {
		config.obfuscateReferences = 0;
		List<String> parameterValues = super.parameterValues.get("obfuscateReferences");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null && parameterValue.equals("normal")) {
				config.obfuscateReferences = 1;
			}
		}
	}

	protected void applyDeriveGroupings(ObfuscateConfig config) {
		config.useInputChangeLogGroupings = false;
		List<String> parameterValues = super.parameterValues.get("deriveGroupingsFromInputChangeLog");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null && parameterValue.equals("true")) {
				config.useInputChangeLogGroupings = true;
			}
		}
	}

	@Override
	protected void executeStatement(
		List<ExcludeStatement> excludeStatements,
		List<UnexcludeStatement> unexcludeStatements,
		ObfuscateConfig config,
		ScriptContext scriptContext,
		EmptyConsumer completionConsumer,
		AbstractLogger logger
	) throws IOException {
		Workspace workspace = scriptContext.getWorkspace();
		PrintWriter writer = scriptContext.getWriter();
		if (scriptContext.isVerbose()) {
			String keepGenericsInformationText = null;
			switch (config.keepGenericsInformation) {
				case 0:
					keepGenericsInformationText = "true";
					break;
				case 1:
					keepGenericsInformationText = "false";
			}

			writer.println("\tkeepGenericsInfo=" + keepGenericsInformationText);
			if (!config.legalIdentifiers) {
				writer.println("\tlegalIdentifiers=" + config.legalIdentifiers);
			}

			if (config.keepBalancedLocks) {
				writer.println("\tkeepBalancedLocks=" + config.keepBalancedLocks);
			}

			if (!config.preverify) {
				writer.println("\tpreverify=" + config.preverify);
			}

			if (config.makeClassesPublic) {
				writer.println("\tmakeClassesPublic=" + config.makeClassesPublic);
			}

			String mixedCaseClassNamesText = null;
			switch (config.mixedCaseClassNames.getType()) {
				case 0:
					mixedCaseClassNamesText = "true";
					break;
				case 1:
					mixedCaseClassNamesText = "false";
					break;
				case 2:
					mixedCaseClassNamesText = "ifInArchive";
			}

			writer.println("\tmixedCaseClassNames=" + mixedCaseClassNamesText);
			writer.println("\tuniqueClassNames=" + config.uniqueClassNames);
			writer.println("\tuniqueMethodNames=" + config.uniqueMethodNames);
			if (this.allowMethodParameterChanges != null) {
				writer.println("\tallowMethodParameterChanges=" + (this.allowMethodParameterChanges ? "true" : "false"));
			}

			if (this.hasMethodParameterChanges) {
				writer.print("\tmethodParameterChanges=");
				switch (config.methodParameterChanges) {
					case 0:
						writer.println("none");
						break;
					case 1:
						writer.println("normal");
						break;
					case 2:
						writer.println("random");
						break;
					case 3:
						writer.println("flowObfuscate");
				}

				if (config.methodParameterChanges != 0 && config.methodParameterChangesPackage != null && config.methodParameterChangesPackage.length() > 0) {
					writer.println("\tmethodParameterChangesPackage=\"" + config.methodParameterChangesPackage + "\"");
				}
			}

			if (config.thideFieldNames) {
				writer.println("\thideFieldNames=" + config.thideFieldNames);
			}

			if (config.hideStaticMethodNames) {
				writer.println("\thideStaticMethodNames=" + config.hideStaticMethodNames);
			}

			String obfuscateControlFlowText = null;
			switch (config.obfuscateControlFlow) {
				case 0:
					obfuscateControlFlowText = "none";
					break;
				case 1:
					obfuscateControlFlowText = "light";
					break;
				case 2:
					obfuscateControlFlowText = "normal";
					break;
				case 3:
					obfuscateControlFlowText = "aggressive";
					break;
				case 4:
					obfuscateControlFlowText = "extraAggressive";
			}

			writer.println("\tobfuscateFlow=" + obfuscateControlFlowText);
			String obfuscateParametersText = null;
			switch (config.obfuscateParameters) {
				case 0:
					obfuscateParametersText = "none";
					break;
				case 1:
					obfuscateParametersText = "normal";
			}

			writer.println("\tobfuscateParameters=" + obfuscateParametersText);
			String encryptStringLiteralsText = null;
			switch (config.encryptStringLiterals) {
				case 0:
					encryptStringLiteralsText = "none";
					break;
				case 1:
					encryptStringLiteralsText = "normal";
					break;
				case 2:
					encryptStringLiteralsText = "aggressive";
					break;
				case 3:
					encryptStringLiteralsText = "flowObfuscate";
					break;
				case 4:
					encryptStringLiteralsText = "enhanced";
			}

			writer.println("\tencryptStringLiterals=" + encryptStringLiteralsText);
			String encryptIntegerConstantsText = null;
			switch (config.encryptIntegerConstants) {
				case 0:
					encryptIntegerConstantsText = "none";
					break;
				case 1:
					encryptIntegerConstantsText = "normal";
					break;
				case 2:
					encryptIntegerConstantsText = "aggressive";
			}

			writer.println("\tencryptIntegerConstants=" + encryptIntegerConstantsText);
			String encryptLongConstantsText = null;
			switch (config.encryptLongConstants) {
				case 0:
					encryptLongConstantsText = "none";
					break;
				case 1:
					encryptLongConstantsText = "normal";
					break;
				case 2:
					encryptLongConstantsText = "aggressive";
			}

			writer.println("\tencryptLongConstants=" + encryptLongConstantsText);
			String lineNumbersText = null;
			switch (config.lineNumbers) {
				case 0:
					lineNumbersText = "delete";
					break;
				case 1:
					lineNumbersText = "scramble";
					break;
				case 2:
					lineNumbersText = "keep";
			}

			writer.println("\tlineNumbers=" + lineNumbersText);
			String localVariableTablesText = null;
			switch (config.localVariableTables) {
				case 0:
					localVariableTablesText = "delete";
					break;
				case 1:
					localVariableTablesText = "keep";
					break;
				case 2:
					localVariableTablesText = "obfuscate";
					break;
				case 3:
					localVariableTablesText = "keepVisibleMethodParameters";
					break;
				case 4:
					localVariableTablesText = "keepVisibleMethodParametersIfNotObfuscated";
					break;
				case 5:
					localVariableTablesText = "keepMethodParametersIfNotObfuscated";
			}

			writer.println("\tlocalVariables=" + localVariableTablesText);
			String methodParameterOptionText = null;
			switch (config.methodParameterOption) {
				case 0:
					methodParameterOptionText = "delete";
					break;
				case 1:
					methodParameterOptionText = "keep";
					break;
				case 2:
					methodParameterOptionText = "obfuscate";
					break;
				case 3:
					methodParameterOptionText = "keepVisible";
					break;
				case 4:
					methodParameterOptionText = "keepVisibleIfNotObfuscated";
					break;
				case 5:
					methodParameterOptionText = "keepIfNotObfuscated";
			}

			writer.println("\tmethodParameters=" + methodParameterOptionText);
			if (config.collapsePackagesWithDefault) {
				writer.println("\tcollapsePackagesWithDefault=\"" + config.collapseTargetPackage.replace('/', '.') + "\"");
			}

			if (config.newNamesPrefix != null && config.newNamesPrefix.length() > 0) {
				writer.println("\tnewNamesPrefix=\"" + config.newNamesPrefix + "\"");
			}

			writer.println("\tderiveGroupingsFromInputChangeLog=" + config.useInputChangeLogGroupings);
			String exceptionObfuscationText = null;
			switch (config.exceptionObfuscation) {
				case 0:
					exceptionObfuscationText = "none";
					break;
				case 1:
					exceptionObfuscationText = "light";
					break;
				case 2:
					exceptionObfuscationText = "heavy";
			}

			writer.println("\texceptionObfuscation=" + exceptionObfuscationText);
			writer.println("\tautoReflectionHandling=" + (config.autoReflectionHandling == 0 ? "none" : "normal"));
			if (config.autoReflectionPackage != null && config.autoReflectionPackage.length() > 0) {
				writer.println("\tautoReflectionPackage=\"" + config.autoReflectionPackage + "\"");
			}

			if (config.autoReflectionHash != null && config.autoReflectionHash.length() > 0) {
				writer.println("\tautoReflectionHash=" + config.autoReflectionHash);
			}

			writer.println("\tobfuscateReferences=" + (config.obfuscateReferences == 0 ? "none" : "normal"));
			if (config.obfuscateReferences != 0) {
				writer.println("\tobfuscateReferenceStructures=" + (config.obfuscateReferenceStructures == 0 ? "inReferencingClasses" : "inSpecialClass"));
				if (config.obfuscateReferencesPackage != null && config.obfuscateReferencesPackage.length() > 0) {
					writer.println("\tobfuscateReferencesPackage=\"" + config.obfuscateReferencesPackage + "\"");
				}
			}
		}

		if (config.assumeRuntimeVersionString != null && config.assumeRuntimeVersionString.length() > 0) {
			writer.println("\tassumeRuntimeVersion=\"" + config.assumeRuntimeVersionString + "\"");
		}

		if (config.newPackageNameFile != null && config.newPackageNameFile.length() > 0) {
			writer.println("\tnewPackageNameFile=\"" + config.newPackageNameFile + "\"");
		}

		if (config.newClassNameFile != null && config.newClassNameFile.length() > 0) {
			writer.println("\tnewClassNameFile=\"" + config.newClassNameFile + "\"");
		}

		if (config.newFieldNameFile != null && config.newFieldNameFile.length() > 0) {
			writer.println("\tnewFieldNameFile=\"" + config.newFieldNameFile + "\"");
		}

		if (config.newMethodNameFile != null && config.newMethodNameFile.length() > 0) {
			writer.println("\tnewMethodNameFile=\"" + config.newMethodNameFile + "\"");
		}

		if (!config.useAsciiCharactersOnly) {
			writer.println("\tnewNameCharacters=non-ASCII");
		}

		List<ObfuscateFlowExcludeStatement> obfuscateFlowExcludeStatements = scriptContext.getObfuscateFlowExcludeStatements();
		List<ObfuscateFlowUnexcludeStatement> obfuscateFlowUnexcludeStatements = scriptContext.getObfuscateFlowUnexcludeStatements();
		List<ObfuscateExceptionsExcludeStatement> obfuscateExceptionsExcludeStatements = scriptContext.getObfuscateExceptionsExcludeStatements();
		List<ObfuscateExceptionsUnexcludeStatement> obfuscateExceptionsUnexcludeStatements = scriptContext.getObfuscateExceptionsUnexcludeStatements();
		List<StringEncryptionExcludeStatement> encryptStringLiteralsExcludeStatements = scriptContext.getEncryptStringLiteralsExcludeStatements();
		List<StringEncryptionUnexcludeStatement> encryptStringLiteralsUnexcludeStatements = scriptContext.getEncryptStringLiteralsUnexcludeStatements();
		List<IntegerEncryptionExcludeStatement> integerEncryptionExcludeStatements = scriptContext.getIntegerEncryptionExcludeStatements();
		List<IntegerEncryptionUnexcludeStatement> integerEncryptionUnexcludeStatements = scriptContext.getIntegerEncryptionUnexcludeStatements();
		List<LongEncryptionExcludeStatement> longEncryptionExcludeStatements = scriptContext.getLongEncryptionExcludeStatements();
		List<LongEncryptionUnexcludeStatement> longEncryptionUnexcludeStatements = scriptContext.getLongEncryptionUnexcludeStatements();
		List<ExistingSerializedClassesStatement> existingSerializedClassesStatements = scriptContext.getExistingSerializedClassesStatements();
		List<FixedClassesStatement> fixedClassesStatements = scriptContext.getFixedClassesStatements();
		List<GroupingsStatement> groupingsStatements = scriptContext.getGroupingsStatements();
		List<AccessedByReflectionStatement> accessedByReflectionStatements = scriptContext.getAccessedByReflectionStatements();
		List<AccessedByReflectionExcludeStatement> accessedByReflectionExcludeStatements = scriptContext.getAccessedByReflectionExcludeStatements();
		List<ObfuscateReferencesIncludeStatement> obfuscateReferencesIncludeStatements = scriptContext.getObfuscateReferencesIncludeStatements();
		List<ObfuscateReferencesExcludeStatement> obfuscateReferencesExcludeStatements = scriptContext.getObfuscateReferencesExcludeStatements();
		List<MethodParameterChangesIncludeStatement> methodParameterChangesIncludeStatements = scriptContext.getMethodParameterChangesIncludeStatements();
		List<MethodParameterChangesExcludeStatement> methodParameterChangesExcludeStatements = scriptContext.getMethodParameterChangesExcludeStatements();
		List<MethodParameterObfuscationIncludeStatement> methodParameterObfuscationIncludeStatements = scriptContext.getMethodParameterObfuscationIncludeStatements();
		List<MethodParameterObfuscationExcludeStatement> methodParameterObfuscationExcludeStatements = scriptContext.getMethodParameterObfuscationExcludeStatements();
		boolean obfuscateReferenceInSpecialClass = config.obfuscateReferenceStructures == 1;
		boolean useAsciiCharactersOnly = config.useAsciiCharactersOnly;
		String newMethodNameFile = config.newMethodNameFile;
		String newFieldNameFile = config.newFieldNameFile;
		String newClassNameFile = config.newClassNameFile;
		String newPackageNameFile = config.newPackageNameFile;
		Integer assumeRuntimeVersionInteger = config.assumeRuntimeVersionInteger;
		String methodParameterChangesPackage = config.methodParameterChangesPackage;
		int methodParameterChanges = config.methodParameterChanges;
		boolean uniqueMethodNames = config.uniqueMethodNames;
		boolean uniqueClassNames = config.uniqueClassNames;
		MixCaseType mixedCaseClassNames = config.mixedCaseClassNames;
		boolean keepBalancedLocks = config.keepBalancedLocks;
		boolean makeClassesPublic = config.makeClassesPublic;
		boolean preverify = config.preverify;
		String obfuscateReferencesPackage = config.obfuscateReferencesPackage;
		int obfuscateReferences = config.obfuscateReferences;
		String autoReflectionHash = config.autoReflectionHash;
		String autoReflectionPackage = config.autoReflectionPackage;
		int autoReflectionHandling = config.autoReflectionHandling;
		int exceptionObfuscation = config.exceptionObfuscation;
		String collapsePackagesWithDefaultValue = config.collapseTargetPackage;
		boolean collapsePackagesWithDefault = config.collapsePackagesWithDefault;
		int methodParameterOption = config.methodParameterOption;
		int localVariableTables = config.localVariableTables;
		int lineNumbers = config.lineNumbers;
		int encryptLongConstants = config.encryptLongConstants;
		int encryptIntegerConstants = config.encryptIntegerConstants;
		int encryptStringLiterals = config.encryptStringLiterals;
		int obfuscateParameters = config.obfuscateParameters;
		int obfuscateControlFlow = config.obfuscateControlFlow;
		int keepGenericsInformation = config.keepGenericsInformation;
		int keepInnerClassInfo = config.keepInnerClassInfo;
		boolean deriveGroupingsFromInputChangeLog = config.useInputChangeLogGroupings;
		boolean allClassesOpened = config.allClassesOpened;
		boolean randomizeObfuscation = config.randomizeObfuscation;
		workspace.performObfuscation(
			config.changeLogFileIn,
			config.isLooseChangeLogFile,
			config.changeLogPrintWriter,
			config.changeLogFileOut,
			config.useFileEncoding,
			excludeStatements,
			unexcludeStatements,
			obfuscateFlowExcludeStatements,
			obfuscateFlowUnexcludeStatements,
			obfuscateExceptionsExcludeStatements,
			obfuscateExceptionsUnexcludeStatements,
			encryptStringLiteralsExcludeStatements,
			encryptStringLiteralsUnexcludeStatements,
			integerEncryptionExcludeStatements,
			integerEncryptionUnexcludeStatements,
			longEncryptionExcludeStatements,
			longEncryptionUnexcludeStatements,
			existingSerializedClassesStatements,
			fixedClassesStatements,
			groupingsStatements,
			accessedByReflectionStatements,
			accessedByReflectionExcludeStatements,
			obfuscateReferencesIncludeStatements,
			obfuscateReferencesExcludeStatements,
			methodParameterChangesIncludeStatements,
			methodParameterChangesExcludeStatements,
			methodParameterObfuscationIncludeStatements,
			methodParameterObfuscationExcludeStatements,
			config.legalIdentifiers,
			config.newNamesPrefix,
			config.thideFieldNames,
			config.hideStaticMethodNames,
			config.aggressiveMethodRenaming,
			randomizeObfuscation,
			allClassesOpened,
			deriveGroupingsFromInputChangeLog,
			keepInnerClassInfo,
			keepGenericsInformation,
			obfuscateControlFlow,
			obfuscateParameters,
			encryptStringLiterals,
			encryptIntegerConstants,
			encryptLongConstants,
			lineNumbers,
			localVariableTables,
			methodParameterOption,
			collapsePackagesWithDefault,
			collapsePackagesWithDefaultValue,
			exceptionObfuscation,
			autoReflectionHandling,
			autoReflectionPackage,
			autoReflectionHash,
			obfuscateReferences,
			obfuscateReferenceInSpecialClass,
			obfuscateReferencesPackage,
			preverify,
			makeClassesPublic,
			keepBalancedLocks,
			mixedCaseClassNames,
			uniqueClassNames,
			uniqueMethodNames,
			methodParameterChanges,
			methodParameterChangesPackage,
			assumeRuntimeVersionInteger,
			newPackageNameFile,
			newClassNameFile,
			newFieldNameFile,
			newMethodNameFile,
			useAsciiCharactersOnly,
			logger,
			completionConsumer,
			scriptContext
		);
	}

	protected void applyEncryptLongConstants(ObfuscateConfig config) {
		config.encryptLongConstants = 0;
		List<String> parameterValues = super.parameterValues.get("encryptLongConstants");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null) {
				if (parameterValue.equals("none")) {
					config.encryptLongConstants = 0;
				} else if (parameterValue.equals("normal")) {
					config.encryptLongConstants = 1;
				}
			}
		}
	}

	public void applyCollapsePackages(ObfuscateConfig config, ScriptContext scriptContext) {
		List<String> parameterValues = super.parameterValues.get("collapsePackagesWithDefault");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null) {
				config.collapsePackagesWithDefault = true;
				AtomicReference<String> errorMessageReference = new AtomicReference<>();
				String normalizedPackageName = PackageManager.normalizePackageName(parameterValue, errorMessageReference);
				if (!errorMessageReference.isNull()) {
					scriptContext.logFatalError("Invalid 'collapsePackagesWithDefault' value '" + parameterValue + "' : \"" + errorMessageReference.get() + "\"");
				}

				config.collapseTargetPackage = normalizedPackageName;
			} else {
				config.collapsePackagesWithDefault = false;
				config.collapseTargetPackage = null;
			}
		}
	}

	protected void applyMethodParameterChanges(ObfuscateConfig config) {
		config.methodParameterChanges = 0;
		List<String> parameterValues = super.parameterValues.get("methodParameterChanges");
		if (parameterValues != null && parameterValues.size() > 0) {
			this.hasMethodParameterChanges = true;
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null) {
				if (parameterValue.equals("flowObfuscate")) {
					config.methodParameterChanges = 3;
				} else if (parameterValue.equals("random")) {
					config.methodParameterChanges = 2;
				} else if (parameterValue.equals("normal")) {
					config.methodParameterChanges = 1;
				}
			}
		}
	}

	protected void applyExceptionObfuscation(ObfuscateConfig config) {
		config.exceptionObfuscation = 1;
		List<String> parameterValues = super.parameterValues.get("exceptionObfuscation");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null) {
				config.exceptionObfuscation = ObfuscateConfig.getExceptionConfigID(parameterValue);
			}
		}
	}

	protected void applyObfuscateFlow(ObfuscateConfig config) {
		config.obfuscateControlFlow = 1;
		List<String> parameterValues = super.parameterValues.get("obfuscateFlow");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null) {
				config.obfuscateControlFlow = ObfuscateConfig.getFlowConfigID(parameterValue);
			}
		}
	}

	@Override
	protected String getActionName() {
		return "Obfuscating";
	}

	protected void applyNewNameCharacters(ObfuscateConfig config) {
		config.useAsciiCharactersOnly = true;
		List<String> parameterValues = super.parameterValues.get("newNameCharacters");
		if (parameterValues != null && parameterValues.size() > 0) {
			String parameterValue = parameterValues.get(0);
			if (parameterValue != null && parameterValue.equals("non-ASCII")) {
				config.useAsciiCharactersOnly = false;
			}
		}
	}
}
