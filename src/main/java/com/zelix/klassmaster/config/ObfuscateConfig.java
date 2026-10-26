package com.zelix.klassmaster.config;

import com.zelix.annotation.ExceptionObfuscationPolicy;
import com.zelix.annotation.FlowObfuscationPolicy;
import com.zelix.enums.MixCaseType;
import com.zelix.exceptions.DebugAssertFailedException;
import com.zelix.utils.dataset.ChangeLogFileData;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ObfuscateConfig implements Serializable {
	private static final Map<String, Integer> flowConfigIDByName = new HashMap<>(13);
	private static final Map<Integer, String> flowConfigNameByID = new HashMap<>(13);
	private static final Map<String, Integer> exceptionConfigIDByName = new HashMap<>(13);
	private static final Map<Integer, String> exceptionConfigNameByID = new HashMap<>(13);
	private static final Map<String, String> EnumObfuscationPolicyMap = new HashMap<>(13);
	public static final String unusedString = "z";
	public static final String DEFAULT_CHANGE_LOG_FILE_NAME = "ChangeLog.txt";
	public static final int optionValueId0_1 = 0;
	public static final int optionValueId1_1 = 1;
	public static final int optionValueId2_1 = 2;
	public static final int optionValueId0_2 = 0;
	public static final int optionValueId1_2 = 1;
	public static final int optionValueId2_2 = 2;
	public static final int optionValueId0_3 = 0;
	public static final int optionValueId1_3 = 1;
	public static final int optionValueId2_3 = 2;
	public static final int optionValueId2_4 = 2;
	public static final int optionValueId5_1 = 5;
	public static final int optionValueId0_4 = 0;
	public static final int optionValueId1_4 = 1;
	public static final int optionValueId2_5 = 2;
	public static final int optionValueId3_1 = 3;
	public static final int optionValueId0_5 = 0;
	public static final int optionValueId1_5 = 1;
	public static final int optionValueId2_6 = 2;
	public static final int optionValueId1_6 = 1;
	public static final int optionValueId4_1 = 4;
	public static final int optionValueId4_2 = 4;
	public static final int optionValueId3_2 = 3;
	public static final int optionValueId0_6 = 0;
	public static final int optionValueId1_7 = 1;
	public static final int optionValueId2_7 = 2;
	public static final int optionValueId0_7 = 0;
	public static final int optionValueId1_8 = 1;
	public static final int optionValueId0_8 = 0;
	public static final int optionValueId5_2 = 5;
	public static final int optionValueId1_9 = 1;
	public static final int optionValueId0_9 = 0;
	public static final int optionValueId1_10 = 1;
	public static final int optionValueId2_8 = 2;
	public static final int optionValueId1_11 = 1;
	public static final int optionValueId4_3 = 4;
	public static final int optionValueId0_10 = 0;
	public static final int optionValueId3_3 = 3;
	public static final int optionValueId0_11 = 0;
	public static final int optionValueId3_4 = 3;
	public static final int optionValueId3_5 = 3;
	public static final int optionValueId0_12 = 0;
	public static final int optionValueId1_12 = 1;
	public static final int optionValueId0_13 = 0;
	public static final int optionValueId0_14 = 0;
	public static final int optionValueId1_13 = 1;
	public static final int optionValueId1_14 = 1;
	public static final int optionValueId2_9 = 2;
	public static final int optionValueId4_4 = 4;
	public int keepInnerClassInfo = 1;
	public int keepGenericsInformation = 0;
	public boolean enableChangeLogFile = false;
	public boolean isLooseChangeLogFile = false;
	public boolean enableChangeLogOut = true;
	public String changeLogFileOut = "ChangeLog.txt";
	public int lineNumbers = 0;
	public int localVariableTables = 0;
	public int methodParameterOption = 3;
	public boolean aggressiveMethodRenaming = false;
	public boolean legalIdentifiers = true;
	public boolean useAsciiCharactersOnly = true;
	public boolean preverify = true;
	public boolean makeClassesPublic = false;
	public boolean keepBalancedLocks = false;
	public MixCaseType mixedCaseClassNames = MixCaseType.IF_IN_ARCHIVE;
	public boolean uniqueClassNames = false;
	public boolean uniqueMethodNames = false;
	public int methodParameterChanges = 0;
	public boolean thideFieldNames = false;
	public boolean hideStaticMethodNames = false;
	public int obfuscateControlFlow = 1;
	public int obfuscateParameters = 0;
	public int encryptStringLiterals = 4;
	public int encryptIntegerConstants = 0;
	public int encryptLongConstants = 0;
	public boolean unusedObfuscationFlag = true;
	public int exceptionObfuscation = 1;
	public boolean collapsePackagesWithDefault = false;
	public boolean randomizeObfuscation = false;
	public boolean useInputChangeLogGroupings = false;
	public boolean allClassesOpened = true;
	public int obfuscateReferences = 0;
	public int obfuscateReferenceStructures = 1;
	public int autoReflectionHandling = 1;
	public ChangeLogFileData[] changeLogFileIn;
	public String collapseTargetPackage;
	public String newNamesPrefix;
	public String autoReflectionPackage;
	public String autoReflectionHash;
	public String obfuscateReferencesPackage;
	public String assumeRuntimeVersionString;
	public Integer assumeRuntimeVersionInteger;
	public String newPackageNameFile;
	public String newClassNameFile;
	public String newFieldNameFile;
	public String newMethodNameFile;
	public transient boolean successReadConfigFromFile;
	public String methodParameterChangesPackage;
	public transient PrintWriter changeLogPrintWriter;
	public String useFileEncoding;

	public ObfuscateConfig() {
	}

	public ObfuscateConfig(String directory, String fileName) {
		File configFile = new File(directory, fileName);
		if (configFile.exists() && !configFile.isDirectory() && configFile.canRead()) {
			ObjectInputStream objectInputStream = null;

			try {
				FileInputStream fileInputStream = new FileInputStream(configFile);
				objectInputStream = new ObjectInputStream(fileInputStream);
				ObfuscateConfig loadedConfig = (ObfuscateConfig)objectInputStream.readObject();
				this.keepInnerClassInfo = loadedConfig.keepInnerClassInfo;
				this.mixedCaseClassNames = loadedConfig.mixedCaseClassNames;
				this.uniqueClassNames = loadedConfig.uniqueClassNames;
				this.uniqueMethodNames = loadedConfig.uniqueMethodNames;
				this.methodParameterChanges = loadedConfig.methodParameterChanges;
				this.keepGenericsInformation = loadedConfig.keepGenericsInformation;
				this.enableChangeLogFile = loadedConfig.enableChangeLogFile;
				this.isLooseChangeLogFile = loadedConfig.isLooseChangeLogFile;
				this.enableChangeLogOut = loadedConfig.enableChangeLogOut;
				this.changeLogFileIn = loadedConfig.changeLogFileIn;
				this.changeLogFileOut = loadedConfig.changeLogFileOut;
				this.lineNumbers = loadedConfig.lineNumbers;
				this.localVariableTables = loadedConfig.localVariableTables;
				this.methodParameterOption = loadedConfig.methodParameterOption;
				this.aggressiveMethodRenaming = loadedConfig.aggressiveMethodRenaming;
				this.legalIdentifiers = loadedConfig.legalIdentifiers;
				this.useAsciiCharactersOnly = loadedConfig.useAsciiCharactersOnly;
				this.preverify = loadedConfig.preverify;
				this.makeClassesPublic = loadedConfig.makeClassesPublic;
				this.keepBalancedLocks = loadedConfig.keepBalancedLocks;
				this.collapsePackagesWithDefault = loadedConfig.collapsePackagesWithDefault;
				this.collapseTargetPackage = loadedConfig.collapseTargetPackage;
				this.thideFieldNames = loadedConfig.thideFieldNames;
				this.hideStaticMethodNames = loadedConfig.hideStaticMethodNames;
				this.obfuscateControlFlow = loadedConfig.obfuscateControlFlow;
				this.obfuscateParameters = loadedConfig.obfuscateParameters;
				this.encryptStringLiterals = loadedConfig.encryptStringLiterals;
				this.encryptIntegerConstants = loadedConfig.encryptIntegerConstants;
				this.encryptLongConstants = loadedConfig.encryptLongConstants;
				this.unusedObfuscationFlag = loadedConfig.unusedObfuscationFlag;
				this.newNamesPrefix = loadedConfig.newNamesPrefix;
				this.randomizeObfuscation = loadedConfig.randomizeObfuscation;
				this.useInputChangeLogGroupings = loadedConfig.useInputChangeLogGroupings;
				this.allClassesOpened = loadedConfig.allClassesOpened;
				this.autoReflectionHandling = loadedConfig.autoReflectionHandling;
				this.autoReflectionPackage = loadedConfig.autoReflectionPackage;
				this.autoReflectionHash = loadedConfig.autoReflectionHash;
				this.exceptionObfuscation = loadedConfig.exceptionObfuscation;
				this.obfuscateReferences = loadedConfig.obfuscateReferences;
				this.obfuscateReferenceStructures = loadedConfig.obfuscateReferenceStructures;
				this.obfuscateReferencesPackage = loadedConfig.obfuscateReferencesPackage;
				this.assumeRuntimeVersionString = loadedConfig.assumeRuntimeVersionString;
				this.assumeRuntimeVersionInteger = loadedConfig.assumeRuntimeVersionInteger;
				this.newPackageNameFile = loadedConfig.newPackageNameFile;
				this.newClassNameFile = loadedConfig.newClassNameFile;
				this.newFieldNameFile = loadedConfig.newFieldNameFile;
				this.newMethodNameFile = loadedConfig.newMethodNameFile;
				this.successReadConfigFromFile = true;
			} catch (IOException ignoredDeserialize) {
			} catch (ClassNotFoundException ignoredClassNotFound) {
			} catch (ClassCastException ignoredClassCast) {
			} catch (DebugAssertFailedException debugAssertFailedException) {
				throw debugAssertFailedException;
			} catch (Throwable ignoredThrowable) {
			} finally {
				if (objectInputStream != null) {
					try {
						objectInputStream.close();
					} catch (IOException ignoredClose) {
					}
				}
			}
		}
	}

	public static int getFlowConfigID(String flowConfigName) {
		return flowConfigIDByName.get(flowConfigName);
	}

	public static String getFlowConfigNameByID(Integer flowConfigID) {
		return flowConfigNameByID.get(flowConfigID);
	}

	public static int getExceptionConfigID(String exceptionConfigName) {
		return exceptionConfigIDByName.get(exceptionConfigName);
	}

	public static String getExceptionConfigNameByID(Integer exceptionConfigID) {
		return exceptionConfigNameByID.get(exceptionConfigID);
	}

	public static String policyNameToConfigName(String policyName) {
		return EnumObfuscationPolicyMap.get(policyName);
	}

	public boolean successReadConfigFromFile() {
		return this.successReadConfigFromFile;
	}

	public void saveToFile(String directory, String fileName) {
		File configFile = new File(directory, fileName);
		if (!configFile.exists() || !configFile.isDirectory() && configFile.canWrite()) {
			ObjectOutputStream objectOutputStream = null;

			try {
				FileOutputStream fileOutputStream = new FileOutputStream(configFile);
				objectOutputStream = new ObjectOutputStream(fileOutputStream);
				objectOutputStream.writeObject(this);
			} catch (IOException ignoredSerialization) {
			} finally {
				if (objectOutputStream != null) {
					try {
						objectOutputStream.close();
					} catch (IOException ignoredClose) {
					}
				}
			}
		}
	}

	static {
		flowConfigIDByName.put("none", 0);
		flowConfigIDByName.put("light", 1);
		flowConfigIDByName.put("normal", 2);
		flowConfigIDByName.put("aggressive", 3);
		flowConfigIDByName.put("extraAggressive", 4);
		flowConfigNameByID.put(0, "none");
		flowConfigNameByID.put(1, "light");
		flowConfigNameByID.put(2, "normal");
		flowConfigNameByID.put(3, "aggressive");
		flowConfigNameByID.put(4, "extraAggressive");
		exceptionConfigIDByName.put("none", 0);
		exceptionConfigIDByName.put("light", 1);
		exceptionConfigIDByName.put("heavy", 2);
		exceptionConfigNameByID.put(0, "none");
		exceptionConfigNameByID.put(1, "light");
		exceptionConfigNameByID.put(2, "heavy");
		EnumObfuscationPolicyMap.put(FlowObfuscationPolicy.NONE.name(), "none");
		EnumObfuscationPolicyMap.put(FlowObfuscationPolicy.LIGHT.name(), "light");
		EnumObfuscationPolicyMap.put(FlowObfuscationPolicy.NORMAL.name(), "normal");
		EnumObfuscationPolicyMap.put(FlowObfuscationPolicy.AGGRESSIVE.name(), "aggressive");
		EnumObfuscationPolicyMap.put(ExceptionObfuscationPolicy.HEAVY.name(), "heavy");
	}
}
