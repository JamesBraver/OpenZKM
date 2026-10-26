package com.zelix.tools;

import com.zelix.enums.MixCaseType;
import com.zelix.exceptions.*;
import com.zelix.framework.*;
import com.zelix.framework.analyzer.MethodAnalyzer;
import com.zelix.framework.analyzer.StaticBlockAnalyzer;
import com.zelix.framework.code.InstructionBlock;
import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.code.MethodInvocation;
import com.zelix.framework.constant.ConstantPoolManager;
import com.zelix.framework.constant.IntegerNode;
import com.zelix.framework.constant.LongNode;
import com.zelix.framework.constant.StringNode;
import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.framework.hierarchy.GroupingsHierarchy;
import com.zelix.framework.hierarchy.Inheritance;
import com.zelix.framework.hierarchy.TypeResolver;
import com.zelix.framework.instructions.WideConstantPoolInsnNode;
import com.zelix.framework.modules.ModuleInfoNode;
import com.zelix.framework.reference.IReferenceGraph;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.reference.ReferenceGraphMap;
import com.zelix.framework.signature.AbstractMethodKey;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.variable.LocalVariableOperand;
import com.zelix.framework.variable.LocalVariableSlot;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.constant.LicenseCheckConstant;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import com.zelix.klassmaster.script.statements.exclusion.*;
import com.zelix.logging.AbstractLogger;
import com.zelix.repository.ClassProvider;
import com.zelix.repository.ClasspathScanner;
import com.zelix.repository.Hierarchy;
import com.zelix.tools.autoReflection.AutoReflectionExcludes;
import com.zelix.tools.autoReflection.AutoReflectionHandlingBuilder;
import com.zelix.tools.exceptions.ObfuscateExceptionExclude;
import com.zelix.tools.excludes.DefaultExcludes;
import com.zelix.tools.excludes.ExcludeStatementManager;
import com.zelix.tools.excludes.FixedClassManager;
import com.zelix.tools.excludes.SerializedClassManager;
import com.zelix.tools.flow.Condition;
import com.zelix.tools.flow.ControlFlowBuilder;
import com.zelix.tools.flow.FlowObfuscateExclude;
import com.zelix.tools.flow.PackageCondition;
import com.zelix.tools.flow.grouping.PackageGroupingManager;
import com.zelix.tools.initOrder.ClassInitializationOrderBuilder;
import com.zelix.tools.integerEncryption.IntegerEncryptionBuilder;
import com.zelix.tools.integerEncryption.IntegerEncryptionExcludes;
import com.zelix.tools.longEncryption.LongEncryptionBuilder;
import com.zelix.tools.longEncryption.LongEncryptionExcludes;
import com.zelix.tools.obfuscateParameters.ObfuscateParameterBuilder;
import com.zelix.tools.parameterChanger.ParameterChangerBuilder;
import com.zelix.tools.parameterChanger.ParameterChangerExcludes;
import com.zelix.tools.parameterChanger.ParameterManager;
import com.zelix.tools.parameterChanger.ParameterPattern;
import com.zelix.tools.parameterChanger.dataset.ChangedMethod;
import com.zelix.tools.parameterChanger.dataset.ParameterRotateEntry;
import com.zelix.tools.parameterChanger.extra.ExtraParameterBuilder;
import com.zelix.tools.references.ObfuscateReferenceExcludes;
import com.zelix.tools.references.ObfuscateReferencesBuilder;
import com.zelix.tools.renamer.*;
import com.zelix.tools.renamer.field.SimpleFieldNameGenerator;
import com.zelix.tools.renamer.method.SimpleMethodNameGenerator;
import com.zelix.tools.string.StringEncryptionBuilder;
import com.zelix.tools.string.StringEncryptionExcludes;
import com.zelix.tools.trim.TrimExcludedMemberSet;
import com.zelix.utils.*;
import com.zelix.utils.collections.*;
import com.zelix.utils.collections.readOnly.ReadOnlyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyRegistryView;
import com.zelix.utils.dataset.*;
import com.zelix.utils.enumerations.ArrayEnumeration;
import com.zelix.utils.function.ZKMConsumer;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.*;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;

public class Obfuscator extends AbstractObfuscator {
	private List<String> newFieldNames;
	private List<String> newMethodNames;

	public Obfuscator(
		Workspace workspace,
		ClassNode[] workspaceClasses,
		ModuleInfoNode[] moduleInfoNodes,
		PackageManager packageManager,
		AtomicReference<String> status,
		ChangeLogFileData[] changelogIn,
		boolean isLooseChangeLogFile,
		PrintWriter changeLogPrintWriter,
		String changeLogFileOut,
		String useFileEncoding,
		List<? extends ExclusionInclusionStatement> excludeStatements,
		List<UnexcludeStatement> unexcludeStatements,
		List<ObfuscateFlowExcludeStatement> obfuscateFlowExcludeStatements,
		List<ObfuscateFlowUnexcludeStatement> obfuscateFlowUnexcludeStatements,
		List<ObfuscateExceptionsExcludeStatement> obfuscateExceptionsExcludeStatements,
		List<ObfuscateExceptionsUnexcludeStatement> obfuscateExceptionsUnexcludeStatements,
		List<StringEncryptionExcludeStatement> encryptStringLiteralsExcludeStatements,
		List<StringEncryptionUnexcludeStatement> encryptStringLiteralsUnexcludeStatements,
		List<IntegerEncryptionExcludeStatement> integerEncryptionExcludeStatements,
		List<IntegerEncryptionUnexcludeStatement> integerEncryptionUnexcludeStatements,
		List<LongEncryptionExcludeStatement> longEncryptionExcludeStatements,
		List<LongEncryptionUnexcludeStatement> longEncryptionUnexcludeStatements,
		List<ExistingSerializedClassesStatement> existingSerializedClassesStatements,
		List<FixedClassesStatement> fixedClassesStatements,
		List<GroupingsStatement> groupingsStatements,
		List<AccessedByReflectionStatement> accessedByReflectionStatements,
		List<AccessedByReflectionExcludeStatement> accessedByReflectionExcludeStatements,
		List<ObfuscateReferencesIncludeStatement> obfuscateReferencesIncludeStatements,
		List<ObfuscateReferencesExcludeStatement> obfuscateReferencesExcludeStatements,
		List<MethodParameterChangesIncludeStatement> methodParameterChangesIncludeStatements,
		List<MethodParameterChangesExcludeStatement> methodParameterChangesExcludeStatements,
		List<MethodParameterObfuscationIncludeStatement> methodParameterObfuscationIncludeStatements,
		List<MethodParameterObfuscationExcludeStatement> methodParameterObfuscationExcludeStatements,
		boolean legalIdentifiers,
		String newNamesPrefix,
		boolean thideFieldNames,
		boolean hideStaticMethodNames,
		boolean aggressiveMethodRenaming,
		boolean randomizeObfuscation,
		boolean allClassesOpened,
		boolean deriveGroupingsFromInputChangeLog,
		int keepInnerClassInfo,
		int keepGenericsInformation,
		int obfuscateControlFlow,
		int obfuscateParameters,
		int encryptStringLiterals,
		int encryptIntegerConstants,
		int encryptLongConstants,
		int lineNumbers,
		int localVariableTables,
		int methodParameterOption,
		boolean collapsePackagesWithDefault,
		String collapsePackagesWithDefaultValue,
		int exceptionObfuscation,
		int autoReflectionHandling,
		String autoReflectionPackage,
		String autoReflectionHash,
		AtomicReference<ClassNode> referenceObfuscationDataClassNode,
		Map<ArchiveNode, ClassNode> archiveToAutoReflectHandlingDataClassNodeMap,
		IReferenceGraph<ClassNode, ClassNode> obfuscateReferencesClassGraph,
		int obfuscateReferences,
		boolean obfuscateReferenceInSpecialClass,
		String obfuscateReferencesPackage,
		AtomicReference<ClassNode> autoReflectHandlingDataClassNode,
		Map<ArchiveNode, ClassNode> archiveToReferenceObfuscationDataClassNodeMap,
		IReferenceGraph<ClassNode, ClassNode> autoReflectionClassReferenceGraph,
		boolean preverify,
		boolean makeClassesPublic,
		boolean keepBalancedLocks,
		MixCaseType mixedCaseClassNames,
		boolean uniqueClassNames,
		boolean uniqueMethodNames,
		int methodParameterChanges,
		String methodParameterChangesPackage,
		Integer assumeRuntimeVersionStringInteger,
		String newPackageNameFile,
		String newClassNameFile,
		List<String> newFieldNameList,
		List<String> newMethodNameList,
		boolean useAsciiCharactersOnly,
		UnmodifiableMultiMap<ArchiveNode, ClassNode> dependencyMultiMap,
		AtomicReference<Triplet> defaultMpcClassTripletRef,
		Map<ArchiveNode, Triplet> archiveToMpcClassTripletMap,
		IReferenceGraph<ClassNode, ClassNode> methodParameterChangeReferenceGraph,
		Map<MethodNode, String> resourceProtectedStaticMethods,
		AbstractLogger logger,
		ZKMConsumer completionCallback,
		ScriptContext scriptContext
	) throws IOException {
		super(workspace, workspaceClasses, packageManager);
		this.newFieldNames = newFieldNameList;
		this.newMethodNames = newMethodNameList;
		AtomicBool enableMPC = new AtomicBool(methodParameterChanges != 0);
		if (workspace.isClasspathChanged()) {
			logger.fatal("FATAL ERROR:", "Classpath has been changed but the classes have not been reopened. You must reopen the classes. (A)");
		} else {
			scriptContext.getMessageCount();
			scriptContext.getWarningCount();
			scriptContext.getErrorCount();
			int initialSpecialErrorCount = scriptContext.getSpecialErrorCount();
			scriptContext.clearMessageCache();

			try {
				TypeResolver typeResolver = new TypeResolver(workspace, super.classProvider);
				this.verifyAndPruneReferences(typeResolver, workspace, super.enableVerbose, scriptContext);
				GroupingsHierarchy groupingsHierarchy = null;
				if (groupingsStatements != null && groupingsStatements.size() > 0) {
					groupingsHierarchy = new GroupingsHierarchy(workspace, groupingsStatements, scriptContext);
				}

				ChangeLogContext changeLogContext = null;
				if (changelogIn != null && changelogIn.length > 0) {
					status.set("Parsing Input Change Log");
					changeLogContext = this.loadAndMergeChangeLogs(changelogIn, logger, scriptContext, isLooseChangeLogFile, aggressiveMethodRenaming);
				}

				ClassMetrics classMetrics = new ClassMetrics(super.classes.length * 5);

				for (ClassNode metricClassNode : super.classes) {
					metricClassNode.collectClassMetrics(classMetrics);
				}

				classMetrics.finalizeMetrics();
				AbstractClassNode[] targetClasses;
				if (changeLogContext != null) {
					this.pruneChangeLogByAvailability(changeLogContext, allClassesOpened);
					targetClasses = this.buildChangeLogClassSet(changeLogContext);
				} else {
					targetClasses = (AbstractClassNode[])super.classes.clone();
				}

				AtomicInt injectObfHelperClassMajorVersion = new AtomicInt(0);
				AtomicInt injectObfHelperClassMinorVersion = new AtomicInt(0);
				InjectedClassManager.calculateInjectionVersion(
					injectObfHelperClassMajorVersion, injectObfHelperClassMinorVersion, targetClasses, assumeRuntimeVersionStringInteger
				);
				ClassHierarchyNode.setHierarchyValidationEnabled(true);
				workspace.clearHierarchyCaches();
				Inheritance inheritance = new Inheritance(targetClasses, super.classProvider);
				FixedClassManager fixedClassManager = null;
				if (fixedClassesStatements != null && fixedClassesStatements.size() > 0) {
					fixedClassManager = new FixedClassManager(workspace, fixedClassesStatements, scriptContext);
					scriptContext.setFixedClassManager(fixedClassManager);
				}

				SerializedClassManager serializedClassManager = null;
				if (existingSerializedClassesStatements != null && existingSerializedClassesStatements.size() > 0) {
					serializedClassManager = new SerializedClassManager(workspace, existingSerializedClassesStatements, scriptContext, fixedClassManager, inheritance);
				}

				AutoReflectionExcludes autoReflectionExcludes = null;
				if (autoReflectionHandling != 0
					&& workspace.hasTrackedMethodInvocations()
					&& (
						accessedByReflectionStatements != null && accessedByReflectionStatements.size() > 0
							|| accessedByReflectionExcludeStatements != null && accessedByReflectionExcludeStatements.size() > 0
					)) {
					autoReflectionExcludes = new AutoReflectionExcludes(workspace, accessedByReflectionStatements, accessedByReflectionExcludeStatements, super.enableVerbose, scriptContext);
				}

				FlowObfuscateExclude flowObfuscateExclude;
				if ((obfuscateControlFlow != 0 || exceptionObfuscation != 0 && Options.ZKM_TREAT_EXCEPTION_OBFUSCATION_AS_FLOW_OBFUSCATION || methodParameterChanges == 3)
					&& (
						fixedClassManager != null
							|| obfuscateFlowExcludeStatements != null && obfuscateFlowExcludeStatements.size() > 0
							|| obfuscateFlowUnexcludeStatements != null && obfuscateFlowUnexcludeStatements.size() > 0
					)) {
					flowObfuscateExclude = new FlowObfuscateExclude(workspace, obfuscateFlowExcludeStatements, obfuscateFlowUnexcludeStatements, changeLogContext, scriptContext);
				} else {
					flowObfuscateExclude = null;
				}

				ObfuscateExceptionExclude obfuscateExceptionExclude;
				if (exceptionObfuscation != 0
					&& (
						fixedClassManager != null
							|| obfuscateExceptionsExcludeStatements != null && obfuscateExceptionsExcludeStatements.size() > 0
							|| obfuscateExceptionsUnexcludeStatements != null && obfuscateExceptionsUnexcludeStatements.size() > 0
					)) {
					obfuscateExceptionExclude = new ObfuscateExceptionExclude(workspace, obfuscateExceptionsExcludeStatements, obfuscateExceptionsUnexcludeStatements, changeLogContext, scriptContext);
				} else {
					obfuscateExceptionExclude = null;
				}

				ClassInitializationOrderBuilder initializationOrderBuilder = null;
				if (scriptContext.hasReferenceGraph()) {
					initializationOrderBuilder = new ClassInitializationOrderBuilder(scriptContext);
				}

				ObfuscateReferenceExcludes obfuscateReferenceExcludes = null;
				HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> referenceSitesByMember = null;
				ObfuscateReferencesBuilder obfuscateReferencesBuilder = null;
				ClassTreeManager classTreeManager = super.classTreeManager;
				ClassProvider classProvider = super.classProvider;
				PhantomClassManager phantomClassManager = scriptContext.getPhantomClassManager();
				boolean controlFlowObfuscationEnabled = obfuscateControlFlow != 0;
				StaticBlockAnalyzer staticBlockAnalyzer = new StaticBlockAnalyzer(targetClasses, workspace, classTreeManager, scriptContext, flowObfuscateExclude, classProvider, phantomClassManager, controlFlowObfuscationEnabled);
				List<String> overrideNames = null;
				if (obfuscateReferences != 0) {
					if (obfuscateReferencesIncludeStatements != null && obfuscateReferencesIncludeStatements.size() > 0
						|| obfuscateReferencesExcludeStatements != null && obfuscateReferencesExcludeStatements.size() > 0) {
						ReferenceGraphMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> referenceSiteGraph = new ReferenceGraphMap<>();
						if (newClassNameFile != null && newClassNameFile.length() > 0) {
							overrideNames = loadOverrideNamesFromFile(newClassNameFile, "newClassNameFile", scriptContext);
						}

						obfuscateReferencesBuilder = new ObfuscateReferencesBuilder(enableMPC, overrideNames, randomizeObfuscation);

						for (ClassNode referenceClassNode : super.classes) {
							if (referenceClassNode.hasInnerClassInfo()) {
								Iterator<AbstractClassNode> referenceInnerClassIterator = referenceClassNode.getSelfAndAllInnerClasses().iterator();

								while (referenceInnerClassIterator.hasNext()) {
									((ClassNode)referenceInnerClassIterator.next()).buildReferenceGraphs(referenceSiteGraph, obfuscateReferencesBuilder, typeResolver, workspace, scriptContext);
								}
							} else {
								referenceClassNode.buildReferenceGraphs(referenceSiteGraph, obfuscateReferencesBuilder, typeResolver, workspace, scriptContext);
							}
						}

						obfuscateReferenceExcludes = new ObfuscateReferenceExcludes(
							workspace, referenceSiteGraph.flattenToGraph(), obfuscateReferencesIncludeStatements, obfuscateReferencesExcludeStatements, scriptContext
						);
						referenceSitesByMember = obfuscateReferenceExcludes.filterReferencesToObfuscate(referenceSiteGraph, staticBlockAnalyzer, fixedClassManager);
					} else {
						scriptContext.logWarning("You elected to obfuscate references but you have not specified any references to be obfuscate.");
					}
				}

				DefaultExcludes defaultExcludes = new DefaultExcludes(workspace, excludeStatements, unexcludeStatements, targetClasses, scriptContext);
				if (serializedClassManager != null) {
					serializedClassManager.applyDefaultExclusions(defaultExcludes, scriptContext.getWriter());
				}

				workspace.refreshClassChangeData(this, 3, 2);
				if (scriptContext.getSpecialErrorCount() > initialSpecialErrorCount) {
					logger.fatalErrorWithDetails("FATAL ERROR:", "Serious Errors detected during obfuscation (A).", scriptContext.getSpecialErrorsAsString());
				}

				StringEncryptionExcludes stringEncryptionExcludes = null;
				if (encryptStringLiterals != 0
					&& (
						fixedClassManager != null
							|| encryptStringLiteralsExcludeStatements != null && encryptStringLiteralsExcludeStatements.size() > 0
							|| encryptStringLiteralsUnexcludeStatements != null && encryptStringLiteralsUnexcludeStatements.size() > 0
					)) {
					stringEncryptionExcludes = new StringEncryptionExcludes(workspace, encryptStringLiteralsExcludeStatements, encryptStringLiteralsUnexcludeStatements, scriptContext);
				}

				if (encryptIntegerConstants != 0 && workspace.isIntegerConstantsEncrypted()) {
					scriptContext.logWarning(
						"Integer Constant Encryption : Cannot perform Integer Constant Encryption more than once on the same classes. Setting of the 'encryptIntegerConstants' parameter of the 'obfuscate' statement will be ignored."
					);
					encryptIntegerConstants = 0;
				}

				IntegerEncryptionExcludes integerEncryptionExcludes = null;
				if (encryptIntegerConstants != 0
					&& (
						fixedClassManager != null
							|| integerEncryptionExcludeStatements != null && integerEncryptionExcludeStatements.size() > 0
							|| integerEncryptionUnexcludeStatements != null && integerEncryptionUnexcludeStatements.size() > 0
					)) {
					integerEncryptionExcludes = new IntegerEncryptionExcludes(workspace, integerEncryptionExcludeStatements, integerEncryptionUnexcludeStatements, scriptContext);
				}

				if (encryptLongConstants != 0 && workspace.isLongConstantsEncrypted()) {
					scriptContext.logWarning(
						"Long Constant Encryption : Cannot perform Long Constant Encryption more than once on the same classes. Setting of the 'encryptLongConstants' parameter of the 'obfuscate' statement will be ignored."
					);
					encryptLongConstants = 0;
				}

				LongEncryptionExcludes longEncryptionExcludes = null;
				if (encryptLongConstants != 0
					&& (
						fixedClassManager != null
							|| longEncryptionExcludeStatements != null && longEncryptionExcludeStatements.size() > 0
							|| longEncryptionUnexcludeStatements != null && longEncryptionUnexcludeStatements.size() > 0
					)) {
					longEncryptionExcludes = new LongEncryptionExcludes(workspace, longEncryptionExcludeStatements, longEncryptionUnexcludeStatements, scriptContext);
				}

				ParameterChangerExcludes parameterChangeExcludes = null;
				Set<MethodNode> parameterChangeMethods = null;
				MultiMap<MethodNode, MethodNode> relatedMethodsByMethod = null;
				if (enableMPC.get()) {
					if (encryptStringLiterals == 4
						|| encryptIntegerConstants != 0
						|| encryptLongConstants != 0
						|| obfuscateReferences != 0
						|| methodParameterChanges == 3
						|| Options.paramChangeAllMethods) {
						if (!methodParameterChangeReferenceGraph.isEmpty()) {
							enableMPC.set(false);
							scriptContext.logWarning(
								"Method Parameter List Changing : Cannot allow method parameter changing more than once on the same classes. Setting of the 'methodParameterChanges' parameter of the 'obfuscate' statement will be ignored."
							);
						} else {
							AtomicReference<Set<MethodNode>> parameterChangeMethodsRef = new AtomicReference<>();
							AtomicReference<MultiMap<MethodNode, MethodNode>> changeRelatedMethodsRef = new AtomicReference<>();
							parameterChangeExcludes = this.buildParameterExcludes(
								false,
								parameterChangeMethodsRef,
								changeRelatedMethodsRef,
								keepInnerClassInfo,
								methodParameterChangesIncludeStatements,
								methodParameterChangesExcludeStatements,
								defaultExcludes,
								autoReflectionExcludes,
								fixedClassManager,
								scriptContext
							);
							parameterChangeMethods = parameterChangeMethodsRef.get();
							relatedMethodsByMethod = changeRelatedMethodsRef.get();
						}
					}
				} else if (changeLogContext != null && changeLogContext.hasParameterChangeData() && isLooseChangeLogFile) {
					scriptContext.logWarning(
						"Input change log '"
							+ changeLogContext.getLogFileName()
							+ "' contains method parameter change data but the 'methodParameterChanges' parameter of the 'obfuscate' statement is missing or is set to 'none'. The method parameter change data will be ignored because the change log is specified as loose."
					);
				}

				ParameterChangerExcludes parameterObfuscationExcludes = null;
				if (workspace.isMethodParameterObfuscated()) {
					ClassNode[] classNodes = super.classes;
					int parameterFlagClassCount = classNodes.length;

					for (int i = 0; i < parameterFlagClassCount; i += 1) {
						ClassNode parameterFlagClassNode = classNodes[i];
						parameterFlagClassNode.clearParameterChangeFlags();
						if (parameterFlagClassNode.hasInnerClasses()) {
							Iterator<AbstractClassNode> parameterFlagInnerClassIterator = parameterFlagClassNode.getInnerClasses().iterator();

							while (parameterFlagInnerClassIterator.hasNext()) {
								((ClassNode)parameterFlagInnerClassIterator.next()).clearParameterChangeFlags();
							}
						}
					}
				}

				if (obfuscateParameters != 0) {
					if (workspace.isMethodParameterObfuscated()) {
						scriptContext.logWarning(
							"Parameter Obfuscation : Cannot allow method parameter obfuscation more than once on the same classes. Setting of the 'obfuscateParameters' parameter of the 'obfuscate' statement will be ignored."
						);
						obfuscateParameters = 0;
					} else {
						workspace.markMethodParameterObfuscated();
						AtomicReference<Set<MethodNode>> parameterObfuscationMethodsRef = new AtomicReference<>();
						AtomicReference<MultiMap<MethodNode, MethodNode>> obfuscationRelatedMethodsRef = new AtomicReference<>();
						parameterObfuscationExcludes = this.buildParameterExcludes(
							true,
							parameterObfuscationMethodsRef,
							obfuscationRelatedMethodsRef,
							keepInnerClassInfo,
							methodParameterObfuscationIncludeStatements,
							methodParameterObfuscationExcludeStatements,
							defaultExcludes,
							autoReflectionExcludes,
							fixedClassManager,
							scriptContext
						);
						parameterChangeMethods = parameterObfuscationMethodsRef.get();
						relatedMethodsByMethod = obfuscationRelatedMethodsRef.get();
					}
				}

				if (fixedClassManager != null) {
					fixedClassManager.applyFixedClassExclusions(defaultExcludes, flowObfuscateExclude, obfuscateExceptionExclude, stringEncryptionExcludes, integerEncryptionExcludes, longEncryptionExcludes, scriptContext.getWriter());
				}

				GlobalUtils.newHashMap();
				ControlFlowBuilder controlFlowBuilder = null;
				if (changeLogContext != null) {
					if (!isLooseChangeLogFile) {
						defaultExcludes.processPackageChangeLog(changeLogContext, fixedClassManager);
						defaultExcludes.processClassChangeLog(changeLogContext, fixedClassManager);
					} else {
						changeLogContext.applyDefaultExcludes(defaultExcludes);
					}
				}

				this.validateOpenedClassHierarchy(logger);
				String embeddedLicenseHash = this.getEmbeddedLicenseHash();
				long computedLicenseHash = this.computeLicenseNameHash(embeddedLicenseHash, false);
				long logLicenseHash = this.extractLicenseHashFromLog(scriptContext.getLogFilePath());
				boolean licenseHashMatched = logLicenseHash == computedLicenseHash;
				if (!licenseHashMatched) {
					scriptContext.incrementIndent();
				}

				if (false) {
					long logFileTimestamp = this.getLogFileLastModified(scriptContext.getLogFilePath());
					long licenseExpiryTimestamp = this.computeLicenseNameHash(embeddedLicenseHash, true);
					if (!this.isLicenseTimestampValid(logFileTimestamp, licenseExpiryTimestamp, workspace)) {
						scriptContext.incrementIndent();
					}
				}

				ReferenceGraph<AbstractClassNode, FieldSignature> fieldReferenceGraph = null;
				boolean localVariableTablesKept = localVariableTables == 1 || localVariableTables == 2;
				Random random;
				if (!randomizeObfuscation && !Options.ZKM_RANDOMIZE_OBFUSCATION) {
					random = GlobalUtils.newRandomWithSeed(super.classes.length);
					random.nextBoolean();
				} else {
					random = GlobalUtils.newRandom(67);
				}

				HashSet<MethodNode> flowObfuscationGeneratedMethods = GlobalUtils.newHashSet();
				if (obfuscateControlFlow != 0) {
					//String var701 = xorWithRepeatingKey("\u0002'5&2$$N$+4s\"(+N?*?s'+*Mm,=s.))Cm*=6a(7\u001a92<s,\"1R\"! s()e_,&;s\"+$I>", "MESSAGE:");
					status.set("Obfuscating control flow");
					if (false) {
						scriptContext.logMessage("Obfuscating control flow" + (licenseHashMatched ? "" : (Options.useDotIndentSuffix ? " ." : " ")));
					}

					controlFlowBuilder = new ControlFlowBuilder(
						super.classes, targetClasses, scriptContext, groupingsHierarchy, changeLogContext, workspace, classMetrics, staticBlockAnalyzer, deriveGroupingsFromInputChangeLog, randomizeObfuscation
					);
					HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> flowReferenceSites = null;
					if (this.isIndyReferenceAvailable(obfuscateReferenceExcludes, enableMPC, parameterChangeExcludes, injectObfHelperClassMajorVersion, scriptContext)) {
						flowReferenceSites = new HierarchicalMultiMap<>(Options.ZKM_USE_PARALLEL);
					}

					fieldReferenceGraph = this.runControlFlowPass(
						targetClasses,
						obfuscateControlFlow,
						scriptContext,
						flowObfuscateExclude,
						initializationOrderBuilder,
						controlFlowBuilder,
						defaultExcludes,
						staticBlockAnalyzer,
						flowReferenceSites,
						inheritance,
						typeResolver,
						random,
						randomizeObfuscation,
						licenseHashMatched,
						keepBalancedLocks
					);
					flowObfuscationGeneratedMethods.addAll(controlFlowBuilder.getGeneratedMethods());
					if (flowReferenceSites != null && !flowReferenceSites.isEmpty()) {
						this.mergeReferenceSites(flowReferenceSites, referenceSitesByMember, obfuscateReferenceExcludes, "Flow Obfuscation");
						flowReferenceSites.clear();
					}
				}

				Thread.yield();
				Map<MethodNode, Long> parameterObfuscationKeyByMethod = null;
				if (changeLogContext != null) {
					if (changeLogContext.hasParameterChangeData()) {
						changeLogContext.buildParameterChangeData(super.classProvider);
						if (enableMPC.get()) {
							parameterObfuscationKeyByMethod = changeLogContext.getMethodEncryptionKeys();
						}
					}

					if (changeLogContext.hasObfuscatedParameters()) {
						changeLogContext.collectParameterObfuscatedMethods(super.classProvider);
					}
				}

				if (keepInnerClassInfo != 1) {
					this.propagateInnerExclusion(keepInnerClassInfo, defaultExcludes, changeLogContext);
				}

				HashMap<String, String> packageRenameMap;
				if (defaultExcludes.hasNoProcessablePackages()) {
					packageRenameMap = packageManager.getPackageNameMappings(changeLogContext);
					if (collapsePackagesWithDefault) {
						if (!packageManager.isEmpty()) {
							scriptContext.logWarning("You elected to collapse packages but all packages excluded from being renamed.");
						} else {
							scriptContext.logWarning("You elected to collapse packages but no packages found to collapse.");
						}
					}
				} else {
					List<String> packageOverrideNames = null;
					if (newPackageNameFile != null && newPackageNameFile.length() > 0) {
						packageOverrideNames = loadOverrideNamesFromFile(newPackageNameFile, "newPackageNameFile", scriptContext);
					}

					packageRenameMap = packageManager.createPackageRenameMap(
						new ArrayEnumeration<>(super.classes),
						defaultExcludes,
						changeLogContext,
						isLooseChangeLogFile,
						randomizeObfuscation,
						useAsciiCharactersOnly,
						legalIdentifiers,
						newNamesPrefix,
						mixedCaseClassNames,
						super.classRepository.getMainClasspathScanner(),
						collapsePackagesWithDefault,
						collapsePackagesWithDefaultValue,
						workspace.getResourceFilePathsIterator(),
						packageOverrideNames,
						scriptContext
					);
					ModuleInfoNode[] moduleInfoNodeArray = moduleInfoNodes;
					int moduleInfoNodeCount = moduleInfoNodes.length;

					for (int moduleIndex = 0; moduleIndex < moduleInfoNodeCount; moduleIndex += 1) {
						ModuleInfoNode moduleInfoNode = moduleInfoNodeArray[moduleIndex];
						moduleInfoNode.remapPackageNames(packageRenameMap);
						if (moduleInfoNode.hasInnerClasses()) {
							for (AbstractClassNode moduleInnerClassNode : moduleInfoNode.getInnerClasses()) {
								((ModuleInfoNode)moduleInnerClassNode).remapPackageNames(packageRenameMap);
							}
						}
					}
				}

				workspace.updatePackageRenameMap(packageRenameMap);
				Thread.yield();
				if (scriptContext.getSpecialErrorCount() > initialSpecialErrorCount) {
					logger.fatalErrorWithDetails("FATAL ERROR:", "Serious Errors detected during obfuscation (B).", scriptContext.getSpecialErrorsAsString());
				}

				int mapInitialCapacity = GlobalUtils.getInitialCapacity(super.classes.length);
				HashMap<String, String> newClassNameToSourceName = GlobalUtils.newHashMap(mapInitialCapacity);
				HashMap<String, String> oldToNewClassNames = GlobalUtils.newHashMap(mapInitialCapacity);
				HashMap<String, String> newToOldClassNames = GlobalUtils.newHashMap(mapInitialCapacity);
				if (defaultExcludes.hasNoProcessableClasses() && defaultExcludes.hasNoProcessablePackages()) {
					boolean classMappingsChanged = this.initializeClassMappings(oldToNewClassNames, newToOldClassNames, newClassNameToSourceName, changeLogContext);
					if (classMappingsChanged || keepInnerClassInfo != 0 && keepGenericsInformation != 1) {
						workspace.remapAllReferences(targetClasses, workspace.getModuleInfoNodes(), keepInnerClassInfo, keepGenericsInformation, oldToNewClassNames, packageRenameMap, logger);
						workspace.rebuildAllRegistries();
					}

					if (classMappingsChanged) {
						workspace.rebuildFieldRegistries();
						workspace.rebuildMethodRegistries();
						workspace.createHierarchySnapshot();
					}
				} else {
					status.set("Obfuscating class names");
					this.runClassNameObfuscation(
						defaultExcludes,
						changeLogContext,
						randomizeObfuscation,
						useAsciiCharactersOnly,
						legalIdentifiers,
						newNamesPrefix,
						keepInnerClassInfo,
						keepGenericsInformation,
						mixedCaseClassNames,
						uniqueClassNames,
						newClassNameFile,
						packageRenameMap,
						newClassNameToSourceName,
						oldToNewClassNames,
						newToOldClassNames,
						targetClasses,
						logger,
						scriptContext
					);
					workspace.createHierarchySnapshot();
				}

				if (typeResolver != null) {
					typeResolver.clearCache();
				}

				Thread.yield();
				if (Options.shuffleClassMembers) {
					this.shuffleMemberOrder();
				}

				HierarchicalMultiMap<String, Integer, Integer> lineNumberMappings = null;
				switch (lineNumbers) {
					case 0:
						status.set("Removing line numbers");
						this.removeAllLineNumberMetadata();
						break;
					case 1:
						status.set("Scrambling line numbers");
						lineNumberMappings = new HierarchicalMultiMap<>(super.classes.length, 75, 2);
						this.scrambleAndRecordLineNumbers(lineNumberMappings, newToOldClassNames, fixedClassManager, changeLogContext, isLooseChangeLogFile, scriptContext);
				}

				if (changeLogContext != null && changeLogContext.hasLineNumberMappings()) {
					if (lineNumberMappings == null) {
						lineNumberMappings = new HierarchicalMultiMap<>(super.classes.length, 75, 2);
					}

					this.mergeLineMappings(lineNumberMappings, changeLogContext);
				}

				status.set("Obfuscating method names");
				Thread.yield();
				if (changeLogContext != null) {
					changeLogContext.buildProcessedMethodMappings(oldToNewClassNames, newToOldClassNames, workspace, typeResolver, super.classRepository);
				}

				if (scriptContext.getSpecialErrorCount() > initialSpecialErrorCount) {
					logger.fatalErrorWithDetails("FATAL ERROR:", "Serious Errors detected during obfuscation (E).", scriptContext.getSpecialErrorsAsString());
				}

				DualKeyMap<String, MethodSignature, MethodSignature> oldToNewMethodSignatures = new DualKeyMap<>(super.classes.length, 25);
				DualKeyMap<String, MethodSignature, MethodSignature> newToOldMethodSignatures = new DualKeyMap<>(super.classes.length, 25);
				AtomicReference<Map<AbstractMethodNode, AbstractMethodNode>> baseMethodMapRef = new AtomicReference<>();
				this.runMethodNameObfuscation(
					defaultExcludes,
					obfuscateParameters != 0 ? parameterObfuscationExcludes : null,
					fixedClassManager,
					changeLogContext,
					isLooseChangeLogFile,
					keepInnerClassInfo,
					aggressiveMethodRenaming,
					uniqueMethodNames,
					randomizeObfuscation,
					licenseHashMatched,
					useAsciiCharactersOnly,
					legalIdentifiers,
					newNamesPrefix,
					hideStaticMethodNames,
					newToOldClassNames,
					oldToNewMethodSignatures,
					newToOldMethodSignatures,
					targetClasses,
					resourceProtectedStaticMethods,
					this.newMethodNames,
					baseMethodMapRef,
					scriptContext
				);
				workspace.methodNameObfFinish();
				if (changeLogContext != null) {
					changeLogContext.buildProcessedFieldMappings(oldToNewClassNames, newToOldClassNames, fieldReferenceGraph, workspace, super.classRepository);
					if (!isLooseChangeLogFile) {
						defaultExcludes.processFieldChangeLog(changeLogContext, newToOldClassNames, fixedClassManager);
					} else {
						changeLogContext.applyFieldExcludes(defaultExcludes, newToOldClassNames, workspace);
					}
				}

				if (scriptContext.getSpecialErrorCount() > initialSpecialErrorCount) {
					logger.fatalErrorWithDetails("FATAL ERROR:", "Serious Errors detected during obfuscation (C).", scriptContext.getSpecialErrorsAsString());
				}

				DualKeyMap<String, FieldSignature, FieldSignature> oldToNewFieldSignatures = new DualKeyMap<>(super.classes.length, 20);
				DualKeyMap<String, FieldSignature, FieldSignature> newToOldFieldSignatures = new DualKeyMap<>(super.classes.length, 20);
				if (defaultExcludes.hasNoProcessableFields() && (obfuscateControlFlow == 0 || changeLogContext == null)) {
					if (this.mergeFieldMappings(oldToNewFieldSignatures, newToOldFieldSignatures, changeLogContext)) {
						workspace.refreshFieldRefsAndConstants(targetClasses, scriptContext);
					}
				} else {
					status.set("Obfuscating field names");
					this.runFieldNameObfuscation(
						defaultExcludes,
						changeLogContext,
						keepInnerClassInfo,
						useAsciiCharactersOnly,
						legalIdentifiers,
						randomizeObfuscation,
						newNamesPrefix,
						thideFieldNames,
						newToOldClassNames,
						oldToNewFieldSignatures,
						newToOldFieldSignatures,
						targetClasses,
						this.newFieldNames,
						scriptContext
					);
					workspace.fieldNameObfFinish();
				}

				if (scriptContext.getSpecialErrorCount() > initialSpecialErrorCount) {
					logger.fatalErrorWithDetails("FATAL ERROR:", "Serious Errors detected during obfuscation (D).", scriptContext.getSpecialErrorsAsString());
				}

				this.applyLocalVarTablePolicy(localVariableTables, status);
				this.applyMethodParameterPolicy(methodParameterOption, status);
				this.markParameterAttributeFlags(baseMethodMapRef.get());
				ReferenceGraph<ClassNode, ClassNode> localClassReferenceGraph = null;
				if (enableMPC.get()) {
					localClassReferenceGraph = ParameterChangerBuilder.buildInnerClassReferenceGraph(super.classTreeManager);
				}

				if (keepInnerClassInfo != 0) {
					status.set("Removing inner class information");
					this.applyInnerClassPolicy(keepInnerClassInfo, newToOldClassNames, fixedClassManager, scriptContext);
				}

				if (keepGenericsInformation != 0) {
					this.applyAttributeStripMode(keepGenericsInformation);
				}

				workspace.updateStringReferences(newToOldClassNames, oldToNewClassNames, packageRenameMap, scriptContext);
				if (scriptContext.getSpecialErrorCount() > initialSpecialErrorCount) {
					logger.fatalErrorWithDetails("FATAL ERROR:", "Serious Errors detected during obfuscation (F).", scriptContext.getSpecialErrorsAsString());
				}

				HashMap<ClassNode, Boolean> microEditionClasses = workspace.detectMicroEditionClasses();
				scriptContext.setMicroEditionClassesFound(!microEditionClasses.isEmpty());
				NestedMultiMap<ClassNode, MethodNode, StringNode, PriorityEntry<WritableType>> stringEncryptionTargetsByMethod = new NestedMultiMap<>(super.classes.length);
				KeyValuePairMultiMap<ClassNode, StringNode, FieldNode> staticStringFieldTargets = new KeyValuePairMultiMap<>(super.classes.length);
				MultiMap<ClassNode, FieldNode> instanceStringFieldTargets = new MultiMap<>(super.classes.length);
				if (encryptStringLiterals != 0) {
					ClassNode[] stringTargetClasses = super.classes;
					int stringTargetClassCount = stringTargetClasses.length;

					for (int stringTargetIndex = 0; stringTargetIndex < stringTargetClassCount; stringTargetIndex += 1) {
						ClassNode stringTargetClassNode = stringTargetClasses[stringTargetIndex];
						if ((fixedClassManager == null || !fixedClassManager.isInFixedClasses(stringTargetClassNode)) && (stringEncryptionExcludes == null || !stringEncryptionExcludes.isClassExcluded(stringTargetClassNode))) {
							stringTargetClassNode.collectStringTargets(stringEncryptionTargetsByMethod, staticStringFieldTargets, instanceStringFieldTargets, stringEncryptionExcludes, false, typeResolver, workspace, scriptContext);
							if (stringTargetClassNode.hasInnerClasses()) {
								Iterator<AbstractClassNode> stringTargetInnerClassIterator = stringTargetClassNode.getInnerClasses().iterator();

								while (stringTargetInnerClassIterator.hasNext()) {
									((ClassNode)stringTargetInnerClassIterator.next()).collectStringTargets(stringEncryptionTargetsByMethod, staticStringFieldTargets, instanceStringFieldTargets, stringEncryptionExcludes, false, typeResolver, workspace, scriptContext);
								}
							}
						}
					}

					Iterator<Entry<ClassNode, KeyValuePairMultiMap<MethodNode, StringNode, PriorityEntry<WritableType>>>> stringTargetClassEntryIterator = stringEncryptionTargetsByMethod.entrySet().iterator();

					while (stringTargetClassEntryIterator.hasNext()) {
						Iterator<Entry<MethodNode, List<KeyValuePair<StringNode, PriorityEntry<WritableType>>>>> stringTargetMethodEntryIterator = stringTargetClassEntryIterator.next().getValue().entrySet().iterator();

						while (stringTargetMethodEntryIterator.hasNext()) {
							MethodNode stringTargetMethodToCheck = stringTargetMethodEntryIterator.next().getKey();
							if (!stringTargetMethodToCheck.isClinit() && staticBlockAnalyzer.isCalledBySuperInitializer(stringTargetMethodToCheck)) {
								stringTargetMethodEntryIterator.remove();
								if (scriptContext.isVerbose()) {
									scriptContext.logMessage(
										"String Encryption : Strings in method '"
											+ stringTargetMethodToCheck.getSignature()
											+ "' in class '"
											+ stringTargetMethodToCheck.getClassNameBinary()
											+ "' will not be encrypted because the method is called by the static initializer of a super class."
									);
								}
							}
						}
					}
				}

				NestedMultiMap<ClassNode, MethodNode, IntegerNode, PriorityEntry<WritableType>> integerEncryptionTargetsByMethod = new NestedMultiMap<>(super.classes.length);
				KeyValuePairMultiMap<ClassNode, IntegerNode, FieldNode> staticIntegerFieldTargets = new KeyValuePairMultiMap<>(super.classes.length);
				KeyValuePairMultiMap<ClassNode, IntegerNode, FieldNode> instanceIntegerFieldTargets = new KeyValuePairMultiMap<>(super.classes.length);
				if (encryptIntegerConstants != 0) {
					ClassNode[] integerTargetClasses = super.classes;
					int integerTargetClassCount = integerTargetClasses.length;

					for (int integerTargetIndex = 0; integerTargetIndex < integerTargetClassCount; integerTargetIndex += 1) {
						ClassNode integerTargetClassNode = integerTargetClasses[integerTargetIndex];
						if ((fixedClassManager == null || !fixedClassManager.isInFixedClasses(integerTargetClassNode)) && (integerEncryptionExcludes == null || !integerEncryptionExcludes.isClassExcluded(integerTargetClassNode))) {
							boolean encryptLowValueIntegers = encryptIntegerConstants == 2;
							integerTargetClassNode.collectIntegerTargets(integerEncryptionTargetsByMethod, staticIntegerFieldTargets, instanceIntegerFieldTargets, integerEncryptionExcludes, false, encryptLowValueIntegers, typeResolver, workspace, scriptContext);
							if (integerTargetClassNode.hasInnerClasses()) {
								for (AbstractClassNode integerTargetInnerClassNode : integerTargetClassNode.getInnerClasses()) {
									encryptLowValueIntegers = encryptIntegerConstants == 2;
									((ClassNode)integerTargetInnerClassNode).collectIntegerTargets(integerEncryptionTargetsByMethod, staticIntegerFieldTargets, instanceIntegerFieldTargets, integerEncryptionExcludes, false, encryptLowValueIntegers, typeResolver, workspace, scriptContext);
								}
							}
						}
					}

					Iterator<Entry<ClassNode, KeyValuePairMultiMap<MethodNode, IntegerNode, PriorityEntry<WritableType>>>> integerTargetClassEntryIterator = integerEncryptionTargetsByMethod.entrySet().iterator();

					while (integerTargetClassEntryIterator.hasNext()) {
						Iterator<Entry<MethodNode, List<KeyValuePair<IntegerNode, PriorityEntry<WritableType>>>>> integerTargetMethodEntryIterator = integerTargetClassEntryIterator.next().getValue().entrySet().iterator();

						while (integerTargetMethodEntryIterator.hasNext()) {
							MethodNode integerTargetMethodToCheck = integerTargetMethodEntryIterator.next().getKey();
							if (!integerTargetMethodToCheck.isClinit() && staticBlockAnalyzer.isCalledBySuperInitializer(integerTargetMethodToCheck)) {
								integerTargetMethodEntryIterator.remove();
								if (scriptContext.isVerbose()) {
									scriptContext.logMessage(
										"Integer Constant Encryption : Values in method '"
											+ integerTargetMethodToCheck.getSignature()
											+ "' in class '"
											+ integerTargetMethodToCheck.getClassNameBinary()
											+ "' will not be encrypted because the method is called by the static initializer of a super class. (A)"
									);
								}
							}
						}
					}
				}

				NestedMultiMap<ClassNode, MethodNode, LongNode, PriorityEntry<WideConstantPoolInsnNode>> longEncryptionTargetsByMethod = new NestedMultiMap<>(super.classes.length);
				KeyValuePairMultiMap<ClassNode, LongNode, FieldNode> staticLongFieldTargets = new KeyValuePairMultiMap<>(super.classes.length);
				KeyValuePairMultiMap<ClassNode, LongNode, FieldNode> instanceLongFieldTargets = new KeyValuePairMultiMap<>(super.classes.length);
				if (encryptLongConstants != 0) {
					ClassNode[] longTargetClasses = super.classes;
					int longTargetClassCount = longTargetClasses.length;

					for (int longTargetIndex = 0; longTargetIndex < longTargetClassCount; longTargetIndex += 1) {
						ClassNode longTargetClassNode = longTargetClasses[longTargetIndex];
						if ((fixedClassManager == null || !fixedClassManager.isInFixedClasses(longTargetClassNode)) && (longEncryptionExcludes == null || !longEncryptionExcludes.isClassExcluded(longTargetClassNode))) {
							longTargetClassNode.collectLongTargets(longEncryptionTargetsByMethod, staticLongFieldTargets, instanceLongFieldTargets, longEncryptionExcludes, false, typeResolver, workspace, scriptContext);
							if (longTargetClassNode.hasInnerClasses()) {
								Iterator<AbstractClassNode> longTargetInnerClassIterator = longTargetClassNode.getInnerClasses().iterator();

								while (longTargetInnerClassIterator.hasNext()) {
									((ClassNode)longTargetInnerClassIterator.next()).collectLongTargets(longEncryptionTargetsByMethod, staticLongFieldTargets, instanceLongFieldTargets, longEncryptionExcludes, false, typeResolver, workspace, scriptContext);
								}
							}
						}
					}

					Iterator<Entry<ClassNode, KeyValuePairMultiMap<MethodNode, LongNode, PriorityEntry<WideConstantPoolInsnNode>>>> longTargetClassEntryIterator = longEncryptionTargetsByMethod.entrySet().iterator();

					while (longTargetClassEntryIterator.hasNext()) {
						Iterator<Entry<MethodNode, List<KeyValuePair<LongNode, PriorityEntry<WideConstantPoolInsnNode>>>>> longTargetMethodEntryIterator = longTargetClassEntryIterator.next()
							.getValue()
							.entrySet()
							.iterator();

						while (longTargetMethodEntryIterator.hasNext()) {
							MethodNode longTargetMethodToCheck = longTargetMethodEntryIterator.next().getKey();
							if (!longTargetMethodToCheck.isClinit() && staticBlockAnalyzer.isCalledBySuperInitializer(longTargetMethodToCheck)) {
								longTargetMethodEntryIterator.remove();
								if (scriptContext.isVerbose()) {
									scriptContext.logMessage(
										"Integer Constant Encryption : Values in method '"
											+ longTargetMethodToCheck.getSignature()
											+ "' in class '"
											+ longTargetMethodToCheck.getClassNameBinary()
											+ "' will not be encrypted because the method is called by the static initializer of a super class. (A)"
									);
								}
							}
						}
					}
				}

				HashSet<MethodNode> methodsEligibleForParameterChange = null;
				HashSet<ClassNode> desIndyStringEncryptionClasses = null;
				HashSet<ClassNode> indyIntegerEncryptionClasses = null;
				HashSet<ClassNode> indyLongEncryptionClasses = null;
				if (enableMPC.get() && parameterChangeExcludes != null) {
					parameterChangeExcludes.applyDefaultExclusions();
					methodsEligibleForParameterChange = GlobalUtils.newHashSet();
					desIndyStringEncryptionClasses = GlobalUtils.newHashSet();
					indyIntegerEncryptionClasses = GlobalUtils.newHashSet();
					indyLongEncryptionClasses = GlobalUtils.newHashSet();
					boolean changeTargetsFound = false;
					if (referenceSitesByMember != null) {
						Iterator<Entry<MemberContext, MultiMap<InstructionListNode, WideConstantPoolInsnNode>>> referenceSiteEntryIterator = referenceSitesByMember.entrySet().iterator();

						while (referenceSiteEntryIterator.hasNext()) {
							for (InstructionListNode referenceInstructionNode : referenceSiteEntryIterator.next().getValue().keySet()) {
								changeTargetsFound = true;
								MethodNode referenceMethod = (MethodNode)referenceInstructionNode.getMethodNode();
								if (!referenceMethod.isGeneratedMember()) {
									methodsEligibleForParameterChange.add(referenceMethod);
								}
							}
						}
					}

					if (encryptStringLiterals == 4) {
						for (Entry<ClassNode, KeyValuePairMultiMap<MethodNode, StringNode, PriorityEntry<WritableType>>> stringTargetClassEntry : stringEncryptionTargetsByMethod.entrySet()) {
							for (MethodNode stringTargetMethod : stringTargetClassEntry.getValue().keySet()) {
								changeTargetsFound = true;
								if (!stringTargetMethod.isGeneratedMember()) {
									methodsEligibleForParameterChange.add(stringTargetMethod);
								}
							}

							ClassNode stringIndyClassNode = stringTargetClassEntry.getKey();
							if (StringEncryptionBuilder.isDesIndyEncryptionEnabled(stringIndyClassNode)) {
								desIndyStringEncryptionClasses.add(stringIndyClassNode);
							}
						}

						for (ClassNode staticStringFieldClassNode : staticStringFieldTargets.keySet()) {
							if (StringEncryptionBuilder.isDesIndyEncryptionEnabled(staticStringFieldClassNode)) {
								desIndyStringEncryptionClasses.add(staticStringFieldClassNode);
							}
						}
					}

					if (encryptIntegerConstants != 0) {
						for (Entry<ClassNode, KeyValuePairMultiMap<MethodNode, IntegerNode, PriorityEntry<WritableType>>> integerTargetClassEntry : integerEncryptionTargetsByMethod.entrySet()) {
							for (MethodNode integerTargetMethod : integerTargetClassEntry.getValue().keySet()) {
								changeTargetsFound = true;
								if (!integerTargetMethod.isGeneratedMember()) {
									methodsEligibleForParameterChange.add(integerTargetMethod);
								}
							}

							ClassNode integerIndyClassNode = integerTargetClassEntry.getKey();
							if (IntegerEncryptionBuilder.isIndySupported(integerIndyClassNode)) {
								indyIntegerEncryptionClasses.add(integerIndyClassNode);
							}
						}

						for (ClassNode staticIntegerFieldClassNode : staticIntegerFieldTargets.keySet()) {
							if (IntegerEncryptionBuilder.isIndySupported(staticIntegerFieldClassNode)) {
								indyIntegerEncryptionClasses.add(staticIntegerFieldClassNode);
							}
						}
					}

					if (encryptLongConstants != 0) {
						for (Entry<ClassNode, KeyValuePairMultiMap<MethodNode, LongNode, PriorityEntry<WideConstantPoolInsnNode>>> longTargetClassEntry : longEncryptionTargetsByMethod.entrySet()) {
							for (MethodNode longTargetMethod : longTargetClassEntry.getValue().keySet()) {
								changeTargetsFound = true;
								if (!longTargetMethod.isGeneratedMember()) {
									methodsEligibleForParameterChange.add(longTargetMethod);
								}
							}

							ClassNode longIndyClassNode = longTargetClassEntry.getKey();
							if (LongEncryptionBuilder.isIndySupported(longIndyClassNode)) {
								indyLongEncryptionClasses.add(longIndyClassNode);
							}
						}

						for (ClassNode staticLongFieldClassNode : staticLongFieldTargets.keySet()) {
							if (LongEncryptionBuilder.isIndySupported(staticLongFieldClassNode)) {
								indyLongEncryptionClasses.add(staticLongFieldClassNode);
							}
						}
					}

					if (methodParameterChanges == 3) {
						ClassNode[] parameterChangeClasses = super.classes;
						int parameterChangeClassCount = parameterChangeClasses.length;

						for (int parameterChangeIndex = 0; parameterChangeIndex < parameterChangeClassCount; parameterChangeIndex += 1) {
							ClassNode parameterChangeClassNode = parameterChangeClasses[parameterChangeIndex];
							if (fixedClassManager == null || !fixedClassManager.isInFixedClasses(parameterChangeClassNode)) {
								parameterChangeClassNode.collectEligibleMethods(methodsEligibleForParameterChange, flowObfuscateExclude, null);
							}
						}
					}

					if (controlFlowBuilder != null
						&& (controlFlowBuilder.useFieldAccessors() || controlFlowBuilder.hasPackageConditions())
						&& this.isIndyReferenceAvailable(obfuscateReferenceExcludes, enableMPC, parameterChangeExcludes, injectObfHelperClassMajorVersion, scriptContext)) {
						Iterator<Condition> packageConditionIterator = controlFlowBuilder.getPackageConditions().iterator();

						while (packageConditionIterator.hasNext()) {
							ClassNode staticInitializerClassNode = (ClassNode)packageConditionIterator.next().getOwnerClassNode();
							if (staticInitializerClassNode.isWorkspaceNode()) {
								MethodNode staticInitializerMethod = workspace.getMethodBySignature(staticInitializerClassNode, MethodSignature.STATIC_INITIALIZER);
								if (staticInitializerMethod == null) {
									ArrayList<TypeWriter> constantPoolEntries = new ArrayList<>();
									staticInitializerMethod = staticInitializerClassNode.getOrCreateStaticInitializer(constantPoolEntries, workspace, "Flow Obfuscation", 2);
									staticInitializerClassNode.appendEntriesToConstantPool(constantPoolEntries);
									parameterChangeMethods.add(staticInitializerMethod);
								}

								methodsEligibleForParameterChange.add(staticInitializerMethod);
							}
						}
					}

					if (Options.paramChangeAllMethods) {
						ClassNode[] parameterScanClasses = super.classes;
						int parameterScanClassCount = parameterScanClasses.length;

						for (int parameterScanIndex = 0; parameterScanIndex < parameterScanClassCount; parameterScanIndex += 1) {
							MethodNode[] classMethods = parameterScanClasses[parameterScanIndex].getMethodNodes();
							int methodCount = classMethods.length;

							for (int methodIndex = 0; methodIndex < methodCount; methodIndex += 1) {
								MethodNode eligibleMethod = classMethods[methodIndex];
								if (!eligibleMethod.isGeneratedMember()) {
									methodsEligibleForParameterChange.add(eligibleMethod);
								}
							}
						}
					}

					if (methodsEligibleForParameterChange == null || methodsEligibleForParameterChange.size() == 0) {
						scriptContext.logMessage(
							"Method Parameter List Changing : 'methodParameterChanges' is not 'none' but no method parameter lists "
								+ (changeTargetsFound ? "available" : "required")
								+ " to be changed."
						);
					}
				}

				workspace.buildNameMappings(targetClasses);
				int dependencyCount = dependencyMultiMap.size();
				ReferenceGraph<ArchiveNode, ArchiveNode> moduleDependencyGraph = new ReferenceGraph<>(GlobalUtils.getInitialCapacity(dependencyCount));
				ReferenceGraph<ArchiveNode, String> modulePackageGraph = new ReferenceGraph<>(GlobalUtils.getInitialCapacity(dependencyCount));
				UnmodifiableMultiMap<ArchiveNode, ClassNode> namedModuleClasses = InjectedClassManager.filterNamedModules(dependencyMultiMap, moduleDependencyGraph, modulePackageGraph);
				Map<MethodNode, ParameterPattern> parameterPatternsByMethod = null;
				Map<MethodNode, ChangedMethod> changedMethodsByMethod = null;
				HashMap<MethodNode, LocalVariableOperand> decryptionLocalVarsByMethod = null;
				ParameterChangerBuilder parameterChangerBuilder = null;
				HashSet<MethodNode> methodsNeedingClassSeedXor = GlobalUtils.newHashSet();
				if (methodsEligibleForParameterChange != null && methodsEligibleForParameterChange.size() > 0) {
					if (parameterObfuscationKeyByMethod == null) {
						parameterObfuscationKeyByMethod = GlobalUtils.newHashMap();
					}

					Hierarchy hierarchy = new Hierarchy(workspace, super.classTreeManager, super.classProvider, scriptContext, super.classes.length, aggressiveMethodRenaming, licenseHashMatched, true);
					parameterObfuscationKeyByMethod = ParameterChangerBuilder.buildMethodEncryptionKeys(parameterChangeMethods, parameterObfuscationKeyByMethod, randomizeObfuscation, super.classes.length, changeLogContext, hierarchy);
					decryptionLocalVarsByMethod = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(parameterObfuscationKeyByMethod.size()));
					Set<AbstractClassNode> packageConditionClasses = controlFlowBuilder == null ? null : controlFlowBuilder.getPackageConditionClasses();
					if (overrideNames == null && newClassNameFile != null && newClassNameFile.length() > 0) {
						overrideNames = loadOverrideNamesFromFile(newClassNameFile, "newClassNameFile", scriptContext);
					}

					parameterChangerBuilder = new ParameterChangerBuilder(
						parameterObfuscationKeyByMethod,
						stringEncryptionTargetsByMethod.keys(),
						staticStringFieldTargets.keys(),
						integerEncryptionTargetsByMethod.keys(),
						staticIntegerFieldTargets.keys(),
						longEncryptionTargetsByMethod.keys(),
						staticLongFieldTargets.keys(),
						localClassReferenceGraph,
						super.classes.length,
						namedModuleClasses,
						newNamesPrefix,
						mixedCaseClassNames,
						uniqueClassNames,
						injectObfHelperClassMajorVersion.get(),
						injectObfHelperClassMinorVersion.get(),
						classMetrics.isVeryLargeApplication(),
						changeLogContext,
						workspace,
						workspace.getClassNameToInternalNameMap(),
						workspace.getMethodRegistryReadOnlyView(),
						packageConditionClasses,
						staticBlockAnalyzer,
						initializationOrderBuilder,
						overrideNames,
						super.classRepository,
						randomizeObfuscation,
						scriptContext
					);
					ParameterManager parameterManager = new ParameterManager(
						parameterChangeExcludes,
						fixedClassManager,
						changeLogContext,
						isLooseChangeLogFile,
						workspace,
						super.classes,
						targetClasses,
						super.classTreeManager,
						typeResolver,
						keepInnerClassInfo,
						aggressiveMethodRenaming,
						hierarchy,
						randomizeObfuscation,
						licenseHashMatched,
						parameterObfuscationKeyByMethod,
						decryptionLocalVarsByMethod,
						methodsNeedingClassSeedXor,
						newToOldClassNames,
						oldToNewMethodSignatures,
						newToOldMethodSignatures,
						defaultMpcClassTripletRef,
						archiveToMpcClassTripletMap,
						methodParameterChangesPackage,
						methodParameterChangeReferenceGraph,
						resourceProtectedStaticMethods,
						enableMPC,
						obfuscateParameters != 0,
						scriptContext
					);
					Random parameterChangerRandom;
					if (!randomizeObfuscation && !Options.ZKM_RANDOMIZE_OBFUSCATION) {
						parameterChangerRandom = GlobalUtils.newRandomWithSeed(super.classes.length);
						parameterChangerRandom.nextBoolean();
					} else {
						parameterChangerRandom = GlobalUtils.newRandom(93);
					}

					boolean parameterChangeEnabled = methodParameterChanges == 2 || methodParameterChanges == 3;
					ExtraParameterBuilder extraParameterBuilder = new ExtraParameterBuilder(parameterChangeExcludes, parameterManager, parameterChangeEnabled, parameterChangerRandom);
					AtomicReference<ClassNode[]> updatedClassArrayRef = new AtomicReference<>();
					parameterPatternsByMethod = parameterManager.applyMethodParameterChanges(
						methodsEligibleForParameterChange, desIndyStringEncryptionClasses, indyIntegerEncryptionClasses, indyLongEncryptionClasses, relatedMethodsByMethod, flowObfuscationGeneratedMethods, extraParameterBuilder, inheritance, classMetrics.isLargeApplication() ? controlFlowBuilder : null, parameterChangerBuilder, updatedClassArrayRef, classMetrics.isVeryLargeApplication()
					);
					changedMethodsByMethod = parameterManager.getChangedMethodMap();
					if (!updatedClassArrayRef.isNull()) {
						super.classes = updatedClassArrayRef.get();
					}

					if (!enableMPC.get()) {
						parameterChangerBuilder = null;
					} else {
						workspace.setMethodRegistryFromClasses(targetClasses);
					}
				}

				if (obfuscateParameters != 0) {
					ReferenceGraph<MethodNode, MethodNode> methodCallGraph = new ReferenceGraph<>();
					ClassNode[] referenceGraphClasses = super.classes;
					int referenceGraphClassCount = referenceGraphClasses.length;

					for (int referenceGraphIndex = 0; referenceGraphIndex < referenceGraphClassCount; referenceGraphIndex += 1) {
						ClassNode referenceGraphClassNode = referenceGraphClasses[referenceGraphIndex];
						if (referenceGraphClassNode.hasInnerClasses()) {
							for (AbstractClassNode referenceGraphInnerClassNode : referenceGraphClassNode.getInnerClasses()) {
								if (referenceGraphInnerClassNode.isWorkspaceNode()) {
									((ClassNode)referenceGraphInnerClassNode).collectMethodReferenceGraph(methodCallGraph, GlobalUtils.newHashSet());
								}
							}
						}
					}

					for (Entry<MethodNode, Set<MethodNode>> methodCallEntry : methodCallGraph.getAdjacencyEntries()) {
						relatedMethodsByMethod.putAll(methodCallEntry.getKey(), methodCallEntry.getValue());
					}

					MultiMap<ClassNode, MethodNode> markedParameterMethodsByClass = new MultiMap<>(super.classes.length, 5, Options.ZKM_USE_PARALLEL);
					HierarchicalMultiMap<ClassNode, MethodNode, AbstractMethodNode> parameterObfuscationLinksByClass = new HierarchicalMultiMap<>(super.classes.length, 5, Options.ZKM_USE_PARALLEL);

					for (MethodNode parameterChangeMethod : parameterChangeMethods) {
						if (parameterChangeMethod.isParameterObfuscationMarked()) {
							AbstractMethodNode baseOrSelfMethod = GlobalUtils.getMappedOrDefault(parameterChangeMethod, baseMethodMapRef.get());
							if (!baseOrSelfMethod.isMethodParametersAttributePresent()) {
								markedParameterMethodsByClass.put(parameterChangeMethod.getMethodOwner(), parameterChangeMethod);
								List<MethodNode> relatedMethods = relatedMethodsByMethod.get(parameterChangeMethod);
								if (relatedMethods != null) {
									for (MethodNode relatedMethod : relatedMethods) {
										parameterObfuscationLinksByClass.put(relatedMethod.getMethodOwner(), relatedMethod, parameterChangeMethod);
									}
								}
							} else {
								parameterChangeMethod.setParameterObfuscationMarked(false);
								scriptContext.getWriter().println(
									"\tEXcluded from Method Parameter Obfuscation \""
										+ ExcludeStatementManager.getFormattedMethodSignature(parameterChangeMethod, false, null)
										+ "\" in class \""
										+ ExcludeStatementManager.getFormattedClassSignature(parameterChangeMethod.getMethodOwner(), null, false)
										+ "\" because of the presence of a MethodParameters attribute."
										+ (
											parameterChangeMethod != baseOrSelfMethod
												? " "
													+ ExcludeStatementManager.getFormattedMethodSignature(baseOrSelfMethod, false, null)
													+ "\" in class \""
													+ ExcludeStatementManager.getFormattedClassSignature(parameterChangeMethod.getMethodOwner(), null, false)
												: ""
										)
								);
							}
						}
						else if (parameterObfuscationExcludes.isMethodExcluded(parameterChangeMethod) && parameterChangeMethod.isNameChanged()) {
							scriptContext.getWriter().println(
								"\tEXcluded from Method Parameter Obfuscation \""
									+ ExcludeStatementManager.getFormattedMethodSignature(parameterChangeMethod, false, null)
									+ "\" in class \""
									+ ExcludeStatementManager.getFormattedClassSignature(parameterChangeMethod.getMethodOwner(), null, false)
									+ "\" because its name or the name of its containing class have been obfuscated."
							);
						}
					}

					if (!markedParameterMethodsByClass.isEmpty()) {
						new ObfuscateParameterBuilder(
							super.classes,
							workspace,
							markedParameterMethodsByClass,
							parameterObfuscationLinksByClass,
							newToOldClassNames,
							oldToNewMethodSignatures,
							newToOldMethodSignatures,
							decryptionLocalVarsByMethod,
							flowObfuscationGeneratedMethods,
							inheritance,
							super.classProvider,
							scriptContext,
							classMetrics.isLargeApplication()
						);
						workspace.rebuildMethodRegistries();
						workspace.setMethodRegistryFromClasses(targetClasses);
					}
				}

				if (relatedMethodsByMethod != null) {
					relatedMethodsByMethod.clear();
				}

				if (parameterChangeMethods != null) {
					parameterChangeMethods.clear();
				}

				boolean heavyExceptionObfuscation = false;
				if (exceptionObfuscation != 0) {
					if (exceptionObfuscation == 1) {
						status.set("Performing light exception obfuscation");
					} else if (exceptionObfuscation == 2) {
						status.set("Performing heavy exception obfuscation");
						heavyExceptionObfuscation = true;
					}

					List<PackageGroupingManager> packageGroupingManagers;
					Map<AbstractClassNode, PackageGroupingManager> packageGroupingManagerByClass;
					if (controlFlowBuilder != null) {
						packageGroupingManagers = controlFlowBuilder.getPackageGroupingManagers();
						packageGroupingManagerByClass = controlFlowBuilder.getGroupingManagerByClass();
					} else {
						AtomicReference<Map<AbstractClassNode, PackageGroupingManager>> packageGroupingManagerRef = new AtomicReference<>();
						packageGroupingManagers = ControlFlowBuilder.createGroupingManagers(
							super.classes, targetClasses, workspace.getClassNameToInternalNameMap(), scriptContext, groupingsHierarchy, changeLogContext, workspace, deriveGroupingsFromInputChangeLog, packageGroupingManagerRef
						);
						packageGroupingManagerByClass = packageGroupingManagerRef.get();
					}

					if (packageGroupingManagers != null && packageGroupingManagerByClass != null) {
						HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> exceptionReferenceSites = null;
						if (this.isIndyReferenceAvailable(obfuscateReferenceExcludes, enableMPC, parameterChangeExcludes, injectObfHelperClassMajorVersion, scriptContext)) {
							exceptionReferenceSites = new HierarchicalMultiMap<>();
						}

						this.runExceptionObfuscationPass(
							packageGroupingManagers, packageGroupingManagerByClass, fixedClassManager, flowObfuscateExclude, obfuscateExceptionExclude, inheritance, flowObfuscationGeneratedMethods, exceptionReferenceSites, decryptionLocalVarsByMethod, typeResolver, workspace, scriptContext, heavyExceptionObfuscation, keepBalancedLocks, classMetrics
						);
						if (exceptionReferenceSites != null && !exceptionReferenceSites.isEmpty()) {
							this.mergeReferenceSites(exceptionReferenceSites, referenceSitesByMember, obfuscateReferenceExcludes, "Exception Obfuscation");
							exceptionReferenceSites.clear();
						}
					}
				}

				HashSet<StringNode> encryptedStringNodes = null;
				if (encryptStringLiterals != 0) {
					status.set("Encrypting strings");
					if (parameterChangerBuilder != null && changeLogContext != null && changeLogContext.hasParameterChangeData()) {
						Map<AbstractClassNode, Long> stringEncryptionKeys = changeLogContext.getClassEncryptionKeys();
						parameterChangerBuilder.setClassEncryptionKeys(stringEncryptionKeys);
					}

					encryptedStringNodes = GlobalUtils.newHashSet();
					switch (encryptStringLiterals) {
						case 1:
							StringEncryptionBuilder defaultStringEncryptionBuilder = new StringEncryptionBuilder(false, false);
							this.runStringEncryptionPass(
								stringEncryptionExcludes, stringEncryptionTargetsByMethod, staticStringFieldTargets, instanceStringFieldTargets, defaultStringEncryptionBuilder, false, false, encryptedStringNodes, inheritance, microEditionClasses, parameterChangerBuilder, parameterObfuscationKeyByMethod, decryptionLocalVarsByMethod, typeResolver, classMetrics.isVeryLargeApplication(), scriptContext
							);
							break;
						case 2:
							StringEncryptionBuilder legacyStringEncryptionBuilder = new StringEncryptionBuilder(false, false);
							this.runStringEncryptionPass(
								stringEncryptionExcludes, stringEncryptionTargetsByMethod, staticStringFieldTargets, instanceStringFieldTargets, legacyStringEncryptionBuilder, true, false, encryptedStringNodes, inheritance, microEditionClasses, parameterChangerBuilder, parameterObfuscationKeyByMethod, decryptionLocalVarsByMethod, typeResolver, classMetrics.isVeryLargeApplication(), scriptContext
							);
							break;
						case 3:
							StringEncryptionBuilder complexLoopStringEncryptionBuilder = new StringEncryptionBuilder(true, false);
							this.runStringEncryptionPass(
								stringEncryptionExcludes, stringEncryptionTargetsByMethod, staticStringFieldTargets, instanceStringFieldTargets, complexLoopStringEncryptionBuilder, true, false, encryptedStringNodes, inheritance, microEditionClasses, parameterChangerBuilder, parameterObfuscationKeyByMethod, decryptionLocalVarsByMethod, typeResolver, classMetrics.isVeryLargeApplication(), scriptContext
							);
							break;
						case 4:
							StringEncryptionBuilder doubleArrayStringEncryptionBuilder = new StringEncryptionBuilder(true, true);
							this.runStringEncryptionPass(
								stringEncryptionExcludes, stringEncryptionTargetsByMethod, staticStringFieldTargets, instanceStringFieldTargets, doubleArrayStringEncryptionBuilder, true, true, encryptedStringNodes, inheritance, microEditionClasses, parameterChangerBuilder, parameterObfuscationKeyByMethod, decryptionLocalVarsByMethod, typeResolver, classMetrics.isVeryLargeApplication(), scriptContext
							);
					}
				}

				if (stringEncryptionTargetsByMethod != null) {
					stringEncryptionTargetsByMethod.clear();
				}

				if (staticStringFieldTargets != null) {
					staticStringFieldTargets.clear();
				}

				if (instanceStringFieldTargets != null) {
					instanceStringFieldTargets.clear();
				}

				if (encryptIntegerConstants != 0) {
					status.set("Encrypting integer constants");
					if (parameterChangerBuilder != null && changeLogContext != null && changeLogContext.hasParameterChangeData()) {
						Map<AbstractClassNode, Long> integerEncryptionKeys = changeLogContext.getClassEncryptionKeys();
						parameterChangerBuilder.setClassEncryptionKeys(integerEncryptionKeys);
					}

					IntegerEncryptionBuilder integerEncryptionBuilder = new IntegerEncryptionBuilder();
					this.encryptIntegerConstants(integerEncryptionExcludes, integerEncryptionTargetsByMethod, staticIntegerFieldTargets, instanceIntegerFieldTargets, integerEncryptionBuilder, inheritance, microEditionClasses, parameterChangerBuilder, parameterObfuscationKeyByMethod, decryptionLocalVarsByMethod, typeResolver, classMetrics.isVeryLargeApplication(), scriptContext);
				}

				if (encryptLongConstants != 0) {
					status.set("Encrypting long constants");
					if (parameterChangerBuilder != null && changeLogContext != null && changeLogContext.hasParameterChangeData()) {
						Map<AbstractClassNode, Long> longEncryptionKeys = changeLogContext.getClassEncryptionKeys();
						parameterChangerBuilder.setClassEncryptionKeys(longEncryptionKeys);
					}

					LongEncryptionBuilder longEncryptionBuilder = new LongEncryptionBuilder();
					this.encryptLongConstants(longEncryptionExcludes, longEncryptionTargetsByMethod, staticLongFieldTargets, instanceLongFieldTargets, longEncryptionBuilder, inheritance, microEditionClasses, parameterChangerBuilder, parameterObfuscationKeyByMethod, decryptionLocalVarsByMethod, typeResolver, classMetrics.isVeryLargeApplication(), scriptContext);
				}

				if (autoReflectionHandling != 0) {
					if (!obfuscateReferencesClassGraph.isEmpty()) {
						scriptContext.logSpecialError("Auto Reflection Handling : Cannot perform autoReflection handling more than once on the same classes.");
					} else if (microEditionClasses.size() == 0) {
						if (defaultExcludes.hasNoProcessablePackages() && defaultExcludes.hasNoProcessableClasses() && defaultExcludes.hasNoProcessableFields() && defaultExcludes.hasNoProcessableMethods()) {
							scriptContext.logMessage(
								"Auto Reflection Handling : Will not perform automatic Reflection API handling because no packages, classes, fields or methods have been renamed."
							);
						} else {
							if (super.enableVerbose) {
								if (accessedByReflectionStatements != null && accessedByReflectionStatements.size() != 0) {
									scriptContext.logMessage(
										"Auto Reflection Handling : There is an 'accessedByReflection' statement in effect so only classes, fields and methods specified by that statement are eligible for AutoReflection mapping."
									);
								} else {
									scriptContext.logMessage(
										"Auto Reflection Handling : There is no 'accessedByReflection' statement in effect so all classes, fields and methods are by default eligible for AutoReflection mapping."
									);
								}
							}

							ParameterRotateEntry autoReflectionRotateEntry = null;
							if (Options.analyzeAutoReflectionClass && !Options.dontRotateReflectionParams && parameterChangerBuilder != null && parameterChangerBuilder.hasMpcClassTriplets() && namedModuleClasses.isEmpty()) {
								autoReflectionRotateEntry = new ParameterRotateEntry(parameterChangerBuilder, defaultMpcClassTripletRef.get(), archiveToMpcClassTripletMap, workspace, super.classProvider, random);
							}

							if (overrideNames == null && newClassNameFile != null && newClassNameFile.length() > 0) {
								overrideNames = loadOverrideNamesFromFile(newClassNameFile, "newClassNameFile", scriptContext);
							}

							MultiMap<MethodNode, MethodInvocation> trackedMethodInvocations = workspace.getTrackedMethodInvocations();
							boolean inclusionStatementsPresent = accessedByReflectionStatements != null && accessedByReflectionStatements.size() > 0;
							boolean exclusionStatementsPresent = accessedByReflectionExcludeStatements != null && accessedByReflectionExcludeStatements.size() > 0;
							int helperClassMajorVersion = injectObfHelperClassMajorVersion.get();
							int helperClassMinorVersion = injectObfHelperClassMinorVersion.get();
							boolean veryLargeApplication = classMetrics.isVeryLargeApplication();
							this.runAutoReflectionPass(
								trackedMethodInvocations,
								inheritance,
								typeResolver,
								workspace,
								namedModuleClasses,
								moduleDependencyGraph,
								modulePackageGraph,
								archiveToAutoReflectHandlingDataClassNodeMap,
								obfuscateReferencesClassGraph,
								autoReflectHandlingDataClassNode,
								changeLogContext,
								autoReflectionPackage,
								autoReflectionExcludes,
								inclusionStatementsPresent,
								exclusionStatementsPresent,
								autoReflectionHash,
								randomizeObfuscation,
								newNamesPrefix,
								mixedCaseClassNames,
								uniqueClassNames,
								helperClassMajorVersion,
								helperClassMinorVersion,
								veryLargeApplication,
								autoReflectionRotateEntry,
								parameterObfuscationKeyByMethod,
								decryptionLocalVarsByMethod,
								fixedClassManager,
								overrideNames,
								scriptContext
							);
						}
					} else {
						scriptContext.logMessage("Auto Reflection Handling : Will not perform automatic Reflection API handling because CLDC classes are present.");
					}
				}

				if (controlFlowBuilder != null && (controlFlowBuilder.useFieldAccessors() || controlFlowBuilder.hasPackageConditions())) {
					HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> staticInitializerReferenceSites = null;
					if (this.isIndyReferenceAvailable(obfuscateReferenceExcludes, enableMPC, parameterChangeExcludes, injectObfHelperClassMajorVersion, scriptContext)) {
						staticInitializerReferenceSites = new HierarchicalMultiMap<>();
					}

					for (Condition packageCondition : controlFlowBuilder.getPackageConditions()) {
						AbstractClassNode packageConditionClassNode = packageCondition.getOwnerClassNode();
						if (packageConditionClassNode.isWorkspaceNode()) {
							((ClassNode)packageConditionClassNode).obfuscateStaticInitializer(packageCondition, staticInitializerReferenceSites, typeResolver, workspace, staticBlockAnalyzer, controlFlowBuilder.getRandom());
						}
					}

					if (staticInitializerReferenceSites != null && !staticInitializerReferenceSites.isEmpty()) {
						this.mergeReferenceSites(staticInitializerReferenceSites, referenceSitesByMember, obfuscateReferenceExcludes, "Flow Obfuscation");
						staticInitializerReferenceSites.clear();
					}
				}

				if (obfuscateReferenceExcludes != null && (!obfuscateReferenceExcludes.hasNoExcludedClasses() || !obfuscateReferenceExcludes.hasNoIncludedMethods() || !obfuscateReferenceExcludes.hasNoIncludedFields())) {
					if (!autoReflectionClassReferenceGraph.isEmpty()) {
						scriptContext.logSpecialError("Reference Obfuscation : Cannot perform Reference Obfuscation more than once on the same classes.");
					} else if (!scriptContext.hasMicroEditionClasses()) {
						ParameterRotateEntry referenceObfuscationRotateEntry = null;
						if (Options.analyzeReferenceDataClass && !Options.dontRotateReferenceParams && parameterChangerBuilder != null && parameterChangerBuilder.hasMpcClassTriplets() && namedModuleClasses.isEmpty()) {
							referenceObfuscationRotateEntry = new ParameterRotateEntry(parameterChangerBuilder, defaultMpcClassTripletRef.get(), archiveToMpcClassTripletMap, workspace, super.classProvider, random);
						}

						this.runReferenceObfuscationPass(
							obfuscateReferenceExcludes,
							obfuscateReferenceInSpecialClass,
							injectObfHelperClassMajorVersion.get(),
							injectObfHelperClassMinorVersion.get(),
							changeLogContext,
							newNamesPrefix,
							obfuscateReferencesPackage,
							mixedCaseClassNames,
							uniqueClassNames,
							namedModuleClasses,
							moduleDependencyGraph,
							modulePackageGraph,
							referenceObfuscationDataClassNode,
							archiveToReferenceObfuscationDataClassNodeMap,
							autoReflectionClassReferenceGraph,
							referenceSitesByMember,
							obfuscateReferencesBuilder,
							referenceObfuscationRotateEntry,
							parameterObfuscationKeyByMethod,
							decryptionLocalVarsByMethod,
							workspace,
							fixedClassManager,
							inheritance,
							typeResolver,
							classMetrics.isVeryLargeApplication(),
							scriptContext
						);
					} else {
						scriptContext.logMessage("Reference Obfuscation : Will not perform obfuscation of references because CLDC classes are present.");
					}
				}

				if (enableMPC.get() && parameterPatternsByMethod != null && !parameterPatternsByMethod.isEmpty() && methodParameterChanges == 3) {
					Map<InstructionListNode, MethodAnalyzer> methodAnalyzerCache = Options.ZKM_USE_PARALLEL
						? new ConcurrentHashMap<>()
						: GlobalUtils.newHashMap();
					int flowObfuscationConfigId;
					if (!Options.lightMpcFlowObfuscation) {
						flowObfuscationConfigId = 3;
					} else {
						flowObfuscationConfigId = 1;
					}

					ArrayList<ClassNode> analysisClasses = new ArrayList<>(super.classes.length + 5);

					for (int classIndex = 0; classIndex < super.classes.length; classIndex += 1) {
						ClassNode analyzedClassNode = super.classes[classIndex];
						analysisClasses.add(analyzedClassNode);
						if (analyzedClassNode.hasInnerClasses()) {
							for (AbstractClassNode analyzedClassInnerNode : analyzedClassNode.getInnerClasses()) {
								analysisClasses.add((ClassNode)analyzedClassInnerNode);
							}
						}
					}

					MultiMap<InstructionListNode, InstructionBlock> flowAnalysisBlocks;
					if (Options.ZKM_USE_PARALLEL && Options.threadCount >= 2) {
						flowAnalysisBlocks = new MultiMap<>(true);
						Vector<ProcessingException> analysisExceptions = new Vector<>();
						analysisClasses.parallelStream().forEach(classNode -> {
							try {
								classNode.analyzeFlowForParameters(flowAnalysisBlocks, typeResolver, workspace, methodAnalyzerCache, 1, flowObfuscationConfigId, licenseHashMatched, keepBalancedLocks, scriptContext, flowObfuscateExclude, random);
							} catch (ProcessingException processingException) {
								analysisExceptions.add(processingException);
							}
						});
						if (!analysisExceptions.isEmpty()) {
							throw (ProcessingException)analysisExceptions.get(0);
						}
					} else {
						flowAnalysisBlocks = new MultiMap<>();

						for (ClassNode analysisClassNode : analysisClasses) {
							analysisClassNode.analyzeFlowForParameters(flowAnalysisBlocks, typeResolver, workspace, methodAnalyzerCache, 1, flowObfuscationConfigId, licenseHashMatched, keepBalancedLocks, scriptContext, flowObfuscateExclude, random);
						}
					}

					HierarchicalMultiMap<AbstractClassNode, InstructionListNode, InstructionBlock> instructionBlocksByClass = new HierarchicalMultiMap<>();

					for (Entry<InstructionListNode, List<InstructionBlock>> instructionEntry : flowAnalysisBlocks.entrySet()) {
						InstructionListNode instructionNode = instructionEntry.getKey();
						instructionBlocksByClass.putAll(instructionNode.getClassNode(), instructionNode, instructionEntry.getValue());
					}

					PrintWriter writer = scriptContext.getWriter();

					for (Entry<AbstractClassNode, MultiMap<InstructionListNode, InstructionBlock>> classInstructionEntry : instructionBlocksByClass.entrySet()) {
						ClassNode parameterObfuscationClassNode = (ClassNode)classInstructionEntry.getKey();
						MethodNode ownerStaticInitializerMethod = null;
						Long staticInitializerKey = null;
						MultiMap<InstructionListNode, InstructionBlock> instructionBlocksByInstruction = classInstructionEntry.getValue();
						ArrayList<TypeWriter> parameterChangeConstantPoolEntries = new ArrayList<>();

						for (Entry<InstructionListNode, List<InstructionBlock>> instructionBlockEntry : instructionBlocksByInstruction.entrySet()) {
							InstructionListNode parameterInstructionNode = instructionBlockEntry.getKey();
							MethodNode ownerMethod = (MethodNode)parameterInstructionNode.getMethodNode();
							ParameterPattern parameterPattern = parameterPatternsByMethod.get(ownerMethod);
							Long parameterObfuscationValue = parameterObfuscationKeyByMethod.get(ownerMethod);
							if (parameterPattern != null) {
								if (ownerStaticInitializerMethod == null && methodsNeedingClassSeedXor.contains(ownerMethod)) {
									ownerStaticInitializerMethod = parameterObfuscationClassNode.findMethodNode(MethodSignature.STATIC_INITIALIZER);
									staticInitializerKey = changedMethodsByMethod.get(ownerStaticInitializerMethod).getMethodSalt();
								}

								parameterInstructionNode.obfuscateMethodParams(parameterObfuscationValue, methodsNeedingClassSeedXor.contains(ownerMethod) ? staticInitializerKey : null, parameterPattern, instructionBlockEntry.getValue(), parameterChangeConstantPoolEntries, random);
								if (scriptContext.isVerbose()) {
									writer.println("\tObfuscated flow using new method parameters in method '" + ownerMethod.getOriginalNameWithParams() + "' in class '" + ownerMethod.getClassNameBinary());
								}
							}
						}

						if (!parameterChangeConstantPoolEntries.isEmpty()) {
							parameterObfuscationClassNode.appendEntriesToConstantPool(parameterChangeConstantPoolEntries);
						}
					}
				}

				if (makeClassesPublic) {
					ClassNode[] visibilityClasses = super.classes;
					int visibilityClassCount = visibilityClasses.length;

					for (int visibilityIndex = 0; visibilityIndex < visibilityClassCount; visibilityIndex += 1) {
						ClassNode visibilityClassNode = visibilityClasses[visibilityIndex];
						if (fixedClassManager != null && !fixedClassManager.isInFixedClasses(visibilityClassNode)) {
						}

						if (visibilityClassNode.hasInnerClassInfo()) {
							Iterator<AbstractClassNode> visibilityInnerClassIterator = visibilityClassNode.getSelfAndAllInnerClasses().iterator();

							while (visibilityInnerClassIterator.hasNext()) {
								((ClassNode)visibilityInnerClassIterator.next()).makeClassPublicAndNonFinal(scriptContext);
							}
						} else {
							visibilityClassNode.makeClassPublicAndNonFinal(scriptContext);
						}
					}
				}

				if (scriptContext.getSpecialErrorCount() > initialSpecialErrorCount) {
					logger.fatalErrorWithDetails("FATAL ERROR:", "Serious Errors detected during obfuscation (G).", scriptContext.getSpecialErrorsAsString());
				}

				Thread.yield();
				if (true) {
					if (Options.shuffleClassMembers) {
						this.shuffleMemberOrder();
					}
				}

				if (changeLogPrintWriter != null) {
					status.set("Writing change log");
				}

				new ChangeLogsWriter(
					changeLogContext != null ? changelogIn : null,
					changeLogPrintWriter,
					changeLogFileOut,
					useFileEncoding,
					packageRenameMap,
					oldToNewClassNames,
					newToOldClassNames,
					newToOldFieldSignatures,
					newToOldMethodSignatures,
					lineNumberMappings,
					newClassNameToSourceName,
					controlFlowBuilder,
					fieldReferenceGraph,
					autoReflectHandlingDataClassNode.get(),
					archiveToAutoReflectHandlingDataClassNodeMap,
					referenceObfuscationDataClassNode.get(),
					archiveToReferenceObfuscationDataClassNodeMap,
					parameterPatternsByMethod,
					changedMethodsByMethod,
					parameterObfuscationKeyByMethod,
					defaultMpcClassTripletRef.get(),
					archiveToMpcClassTripletMap,
					changeLogContext,
					super.integerCache,
					workspace.getModuleInfoEnumeration(),
					new ArrayEnumeration<>(super.classes),
					aggressiveMethodRenaming
				);
				if (super.enableVerbose && preverify) {
					scriptContext.getWriter().println("\tStackMap algorithm=" + Options.ZKM_STACK_MAP_ALGORITHM);
				}

				if (preverify) {
					this.recalculateStackMaps(typeResolver, fixedClassManager);
				}

				if (preverify) {
					if (!scriptContext.isPluginMode() && microEditionClasses.size() > 0) {
						this.updatePreverification(microEditionClasses, typeResolver, scriptContext);
					}
				} else {
					this.removeAllStackMaps();
				}

				scriptContext.setPreverifyDisabled(!preverify);
				if (localVariableTablesKept) {
					this.recalculateLocalVarTypes(typeResolver, scriptContext);
				}

				typeResolver.clearCache();
				workspace.clearHierarchyCaches();
				if (encryptedStringNodes != null) {
					workspace.removeStringNodeReferences(encryptedStringNodes);
				}

				workspace.validateMethodAnalyzers(typeResolver, scriptContext);
				status.set("Reporting messages...");
				workspace.remapResourceFilePaths(packageRenameMap, logger);
				status.set(" ");
			} catch (HierarchyClassNotFoundException hierarchyException) {
				logger.specialError("FILE ERROR:", hierarchyException.getMessage());
			} catch (MethodAnalyzerException analyzerException) {
				ArrayList<String> obfuscationPassNames = new ArrayList<>();
				if (obfuscateControlFlow != 0) {
					obfuscationPassNames.add("Flow Obfuscation");
				}

				if (exceptionObfuscation != 0) {
					obfuscationPassNames.add("Exception Obfuscation");
				}

				if (encryptStringLiterals != 0) {
					obfuscationPassNames.add("String Encryption");
				}

				if (encryptIntegerConstants != 0) {
					obfuscationPassNames.add("Integer Constant Encryption");
				}

				if (encryptLongConstants != 0) {
					obfuscationPassNames.add("Long Constant Encryption");
				}

				if ((encryptStringLiterals == 4 || encryptIntegerConstants != 0 || encryptLongConstants != 0 || obfuscateReferences != 0 || methodParameterChanges == 3) && enableMPC.get()) {
					obfuscationPassNames.add("Method Parameter List Changing");
				}

				if (obfuscateParameters != 0) {
					obfuscationPassNames.add("Method Parameter Obfuscation");
				}

				logger.specialError(
					"SERIOUS ERROR:",
					"Method '"
						+ analyzerException.getMethodName()
						+ "' could not be analyzed (F). "
						+ (obfuscationPassNames.isEmpty() ? "Consider excluding it." : "Consider excluding it from " + GlobalUtils.toString(obfuscationPassNames) + ".")
				);
			} catch (ProcessingException processingException) {
				logger.specialError("SERIOUS ERROR:", processingException.getMessage());
			} catch (InvalidBytecodeException bytecodeException) {
				logger.specialError("SERIOUS ERROR:", bytecodeException.getMessage());
			} finally {
				completionCallback.accept();
				ClassHierarchyNode.setHierarchyValidationEnabled(false);
			}
		}
	}

	private void markParameterAttributeFlags(Map<AbstractMethodNode, AbstractMethodNode> baseMethodMap) {
		for (ClassNode classNode : super.classes) {
			classNode.markParameterAttributeFlags(baseMethodMap);
			if (classNode.hasInnerClasses()) {
				for (AbstractClassNode innerClassNode : classNode.getInnerClasses()) {
					((ClassNode)innerClassNode).markParameterAttributeFlags(baseMethodMap);
				}
			}
		}
	}

	private static String xorWithRepeatingKey(String data, String key) {
		char[] dataChars = data.toCharArray();
		char[] keyChars = key.toCharArray();
		StringBuilder xorBuffer = new StringBuilder(dataChars.length);

		for (int i = 0; i < dataChars.length; i++) {
			char dataChar = dataChars[i];
			char keyChar;
			if (i < keyChars.length - 1) {
				keyChar = keyChars[i];
			} else {
				keyChar = keyChars[i % keyChars.length];
			}

			xorBuffer.append((char)((byte)dataChar ^ (byte)keyChar));
		}

		return xorBuffer.toString();
	}

	public static void collectInheritedMethods(
		AbstractClassNode classNode,
		ReferenceGraph<AbstractMethodKey, AbstractMethodNode> referenceGraph,
		boolean useFullSignature,
		Set<AbstractClassNode> visitedClasses,
		ClassProvider classProvider,
		String reason
	) {
		if (visitedClasses.add(classNode) && !classNode.getInternalName().equals("java/lang/Object")) {
			String superclassName = classNode.getSuperclassName();
			AbstractClassNode superclassNode = classProvider.getClassNode(superclassName, reason);

			for (AbstractMethodNode superclassMethod : superclassNode.getMethods()) {
				referenceGraph.addReference((AbstractMethodKey)(useFullSignature ? superclassMethod.getMethodSignature() : superclassMethod.getMethodParameterKey()), superclassMethod);
			}

			collectInheritedMethods(superclassNode, referenceGraph, useFullSignature, visitedClasses, classProvider, reason);
			String[] interfaceNames = classNode.getInterfaceNames();

			for (int interfaceIndex = 0; interfaceIndex < interfaceNames.length; interfaceIndex++) {
				String interfaceName = interfaceNames[interfaceIndex];
				AbstractClassNode interfaceNode = classProvider.getClassNode(interfaceName, reason);

				for (AbstractMethodNode interfaceMethod : interfaceNode.getMethods()) {
					referenceGraph.addReference((AbstractMethodKey)(useFullSignature ? interfaceMethod.getMethodSignature() : interfaceMethod.getMethodParameterKey()), interfaceMethod);
				}

				collectInheritedMethods(interfaceNode, referenceGraph, useFullSignature, visitedClasses, classProvider, reason);
			}
		}
	}

	private void runExceptionObfuscationPass(
		List<PackageGroupingManager> packageGroupings,
		Map<AbstractClassNode, PackageGroupingManager> groupingsByClass,
		FixedClassManager fixedClassManager,
		FlowObfuscateExclude flowObfuscateExclude,
		ObfuscateExceptionExclude obfuscateExceptionExclude,
		Inheritance inheritance,
		Set<MethodNode> methods,
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> memberObfuscationRefs,
		Map<MethodNode, LocalVariableOperand> decryptionOperandsByMethod,
		TypeResolver typeResolver,
		Workspace workspace,
		ScriptContext scriptContext,
		boolean exceptionObfuscation,
		boolean avoidMonitors,
		ClassMetrics classMetrics
	) {
		PhantomClassManager phantomClassManager = scriptContext.getPhantomClassManager();
		ArrayList<String> specifiedExceptionTypes = new ArrayList<>();
		String useExceptionOption = Options.ZKM_EXCEPTION_OBFUSCATION_USE_EXCEPTION;
		if (useExceptionOption != null) {
			StringTokenizer exceptionTypeTokens = new StringTokenizer(useExceptionOption, ";");

			while (exceptionTypeTokens.hasMoreTokens()) {
				String binaryExceptionClassName = exceptionTypeTokens.nextToken();
				String internalExceptionClassName = GlobalUtils.binaryToInternalName(binaryExceptionClassName);
				if (super.classProvider.getClassNode(internalExceptionClassName, false) != null) {
					if (!internalExceptionClassName.equals("java/lang/Throwable") && !workspace.isClassInstanceOf(internalExceptionClassName, "java/lang/Throwable")) {
						scriptContext.logWarning(
							"Class '" + binaryExceptionClassName + "' specified in ZKM_EXCEPTION_OBFUSCATION_USE_EXCEPTION String '" + useExceptionOption + "' is not a Throwable. It will not be used."
						);
					} else {
						specifiedExceptionTypes.add(internalExceptionClassName);
					}
				} else {
					scriptContext.logWarning(
						"Class '" + binaryExceptionClassName + "' specified in ZKM_EXCEPTION_OBFUSCATION_USE_EXCEPTION String '" + useExceptionOption + "' could not be found. It will not be used."
					);
				}
			}
		}

		HashMap<PackageGroupingManager, List<String>> exceptionTypesByGrouping = GlobalUtils.newHashMap();
		String dontUseExceptionOption = Options.ZKM_EXCEPTION_OBFUSCATION_DONT_USE_EXCEPTION;
		ArrayList<String> excludedExceptionTypes = new ArrayList<>();
		if (dontUseExceptionOption != null) {
			StringTokenizer excludedTypeTokens = new StringTokenizer(dontUseExceptionOption, ";");

			while (excludedTypeTokens.hasMoreTokens()) {
				String excludedExceptionInternalName = GlobalUtils.binaryToInternalName(excludedTypeTokens.nextToken());
				excludedExceptionTypes.add(excludedExceptionInternalName);
			}
		}

		for (PackageGroupingManager packageGrouping : packageGroupings) {
			CountingSet<String> subclassReferenceCounts = new CountingSet<>();

			for (AbstractClassNode commonClass : packageGrouping.getCommonClasses()) {
				if (commonClass instanceof ClassNode) {
					((ClassNode)commonClass).addReferencedSubclasses("java/lang/RuntimeException", subclassReferenceCounts, workspace);
				}
			}

			ArrayList<String> selectedExceptionTypes = new ArrayList<>();
			List<String> sortedExceptionTypes = subclassReferenceCounts.getSortedByCount(false);
			float maxSubclassCount = -1.0F;

			label134:
			for (String exceptionInternalName : sortedExceptionTypes) {
				String exceptionBinaryName = workspace.getInternalToBinaryName(exceptionInternalName);

				for (String excludedTypePattern : excludedExceptionTypes) {
					if (StringUtils.matchesWildcard(exceptionInternalName, excludedTypePattern) || exceptionBinaryName != null && StringUtils.matchesWildcard(exceptionBinaryName, excludedTypePattern)) {
						continue label134;
					}
				}

				int subclassCount = subclassReferenceCounts.getCount(exceptionInternalName);
				if (maxSubclassCount == -1.0F) {
					maxSubclassCount = subclassCount;
				}

				if (maxSubclassCount / subclassCount > 0.5) {
					selectedExceptionTypes.add(exceptionInternalName);
				}
			}

			Collections.sort(selectedExceptionTypes, new Comparator<String>() {
				@Override
				public int compare(String o1, String o2) {
					if (o1.length() == o2.length()) {
						return 0;
					} else {
						return o1.length() < o2.length() ? -1 : 1;
					}
				}
			});
			if (specifiedExceptionTypes.size() > 0) {
				if (selectedExceptionTypes.size() > 0) {
					for (String selectedExceptionType : selectedExceptionTypes) {
						if (!specifiedExceptionTypes.contains(selectedExceptionType)) {
							specifiedExceptionTypes.add(selectedExceptionType);
						}
					}
				}

				selectedExceptionTypes = specifiedExceptionTypes;
			}

			exceptionTypesByGrouping.put(packageGrouping, selectedExceptionTypes);
		}

		ArrayList<AbstractClassNode> classesToObfuscate = new ArrayList<>(super.classes.length);

		for (int i = 0; i < super.classes.length; i++) {
			ClassNode classNode = super.classes[i];
			if (!classNode.isGeneratedClass()
				&& (fixedClassManager == null || !fixedClassManager.isInFixedClasses(classNode))
				&& (obfuscateExceptionExclude == null || !obfuscateExceptionExclude.isClassExcluded(classNode))
				&& (!Options.ZKM_TREAT_EXCEPTION_OBFUSCATION_AS_FLOW_OBFUSCATION || flowObfuscateExclude == null || !flowObfuscateExclude.isClassExcluded(classNode))) {
				classesToObfuscate.add(classNode);
				if (classNode.hasInnerClasses()) {
					for (AbstractClassNode innerClass : classNode.getInnerClasses()) {
						classesToObfuscate.add(innerClass);
					}
				}
			}
		}

		if (Options.ZKM_USE_PARALLEL && Options.threadCount >= 2) {
			Vector<ProcessingException> processingExceptions = new Vector<>();
			classesToObfuscate.parallelStream().forEach(classNode -> {
				try {
					this.obfuscateExceptionsInClass((ClassNode)classNode, exceptionTypesByGrouping, groupingsByClass, flowObfuscateExclude, obfuscateExceptionExclude, inheritance, methods, memberObfuscationRefs, decryptionOperandsByMethod, typeResolver, workspace, scriptContext, exceptionObfuscation, avoidMonitors, classMetrics, phantomClassManager);
				} catch (ProcessingException processingException) {
					processingExceptions.add(processingException);
				}
			});
			if (!processingExceptions.isEmpty()) {
				throw (ProcessingException)processingExceptions.get(0);
			}
		} else {
			for (AbstractClassNode classToObfuscate : classesToObfuscate) {
				this.obfuscateExceptionsInClass((ClassNode)classToObfuscate, exceptionTypesByGrouping, groupingsByClass, flowObfuscateExclude, obfuscateExceptionExclude, inheritance, methods, memberObfuscationRefs, decryptionOperandsByMethod, typeResolver, workspace, scriptContext, exceptionObfuscation, avoidMonitors, classMetrics, phantomClassManager);
			}
		}
	}

	private void obfuscateParameterNames() {
		for (ClassNode classNode : super.classes) {
			classNode.mangleParameterNames();
			if (classNode.hasInnerClasses()) {
				Iterator<AbstractClassNode> innerClassIterator = classNode.getInnerClasses().iterator();

				while (innerClassIterator.hasNext()) {
					((ClassNode)innerClassIterator.next()).mangleParameterNames();
				}
			}
		}
	}

	public long decodeScrambledLong(String scrambledLicense) {
		char[] scrambledChars = scrambledLicense.toCharArray();
		char[] decodedChars = new char[scrambledChars.length];

		for (int i = 0; i < scrambledChars.length; i++) {
			char scrambledChar = scrambledChars[i];
			if (scrambledChar >= '0' && scrambledChar <= '9') {
				if (scrambledChar > '0') {
					scrambledChar = (char)(':' - scrambledChar + 48);
				}
			} else if (scrambledChar >= 'A' && scrambledChar <= 'J') {
				scrambledChar = (char)(scrambledChar - 17);
			} else if (scrambledChar >= 'K' && scrambledChar <= 'T') {
				scrambledChar = (char)(scrambledChar - 27);
			} else if (scrambledChar >= 'U' && scrambledChar <= 'Z') {
				scrambledChar = (char)(scrambledChar - '%');
			} else if (scrambledChar >= 'a' && scrambledChar <= 'd') {
				scrambledChar = (char)(scrambledChar - '+');
			} else if (scrambledChar >= 'e' && scrambledChar <= 'n') {
				scrambledChar = (char)(scrambledChar - '5');
			} else if (scrambledChar >= 'o' && scrambledChar <= 'x') {
				scrambledChar = (char)(scrambledChar - '?');
			}

			decodedChars[i] = scrambledChar;
		}

		Class<?>[] parseParameterTypes = new Class[]{String.class};
		long decodedLong = 0L;

		try {
			Method parseLong = Long.class.getMethod(xorWithRepeatingKey(LicenseCheckConstant.getScrambledMethodName(), LicenseCheckConstant.getMethodNameXorKey()), parseParameterTypes);
			Object[] invokeArgs = new Object[]{new String(decodedChars)};
			decodedLong = (Long)parseLong.invoke(null, invokeArgs);
		} catch (DebugAssertFailedException debugAssertFailedException) {
			throw debugAssertFailedException;
		} catch (Exception ignored) {
		}

		return decodedLong;
	}

	private void applyLocalVarTablePolicy(int localVariableTableMode, AtomicReference<String> status) {
		switch (localVariableTableMode) {
			case 0:
				status.set("Removing local variables");
				this.removeAllLocalVars();
			case 1:
			default:
				break;
			case 2:
				status.set("Obfuscating local variables");
				this.obfuscateLocalVariableNames();
				break;
			case 3:
				status.set("Removing most local variables");
				this.removeSelectiveLocalVars(true, false);
				break;
			case 4:
				status.set("Removing most local variables");
				this.removeSelectiveLocalVars(true, true);
				break;
			case 5:
				status.set("Removing most local variables");
				this.removeSelectiveLocalVars(false, true);
		}
	}

	private void applyMethodParameterPolicy(int methodParameterMode, AtomicReference<String> status) {
		switch (methodParameterMode) {
			case 0:
				status.set("Removing method parameters");
				this.removeAllMethodParameters();
			case 1:
			default:
				break;
			case 2:
				status.set("Obfuscating method parameters");
				this.obfuscateParameterNames();
				break;
			case 3:
				status.set("Removing most method parameters");
				this.removeSelectiveParameters(true, false);
				break;
			case 4:
				status.set("Removing most method parameters");
				this.removeSelectiveParameters(true, true);
				break;
			case 5:
				status.set("Removing most method parameters");
				this.removeSelectiveParameters(false, true);
		}
	}

	private String getEmbeddedLicenseHash() {
		return LicenseCheckConstant.getScrambledLicenseNameHash();
	}

	public boolean verifyAndPruneReferences(TypeResolver typeResolver, Workspace workspace, boolean verbose, ScriptContext scriptContext) {
		boolean classPruned = false;
		HashMap<ClassNode, String> unresolvedClasses = GlobalUtils.newHashMap();
		PhantomClassManager phantomClassManager = scriptContext.getPhantomClassManager();

		for (ClassNode classNode : super.classes) {
			if (this.verifyWithPhantomFallback(classNode, unresolvedClasses, typeResolver, workspace, verbose, scriptContext, phantomClassManager)) {
				classPruned = true;
			}

			if (classNode.hasInnerClasses()) {
				for (AbstractClassNode innerClass : classNode.getInnerClasses()) {
					if (this.verifyWithPhantomFallback((ClassNode)innerClass, unresolvedClasses, typeResolver, workspace, verbose, scriptContext, phantomClassManager)) {
						classPruned = true;
					}
				}
			}
		}

		if (!unresolvedClasses.isEmpty()) {
			TrimExcludedMemberSet trimExcludedMemberSet = scriptContext.getTrimExcludedMemberSet();

			for (ClassNode classToRemove : super.classes) {
				if (unresolvedClasses.containsKey(classToRemove)) {
					String missingClassName = unresolvedClasses.get(classToRemove);
					super.classes = workspace.removeClassFromWorkspace(classToRemove);
					if (trimExcludedMemberSet != null) {
						trimExcludedMemberSet.removeClass(classToRemove);
					}

					scriptContext.logError(
						"Class '"
							+ classToRemove.getSourcePath()
							+ "' will be ignored because 1) it references class '"
							+ missingClassName
							+ "' that could not be found and 2) that missing class was specified to be ignored and 3) the missing class could not be safely ignored in '"
							+ classToRemove.getClassNameBinary()
							+ "'."
					);
				}
			}
		}

		return classPruned;
	}

	private void removeAllLineNumberMetadata() {
		for (ClassNode classNode : super.classes) {
			classNode.removeLineNumberMetadata();
			if (classNode.hasInnerClasses()) {
				Iterator<AbstractClassNode> innerClassIterator = classNode.getInnerClasses().iterator();

				while (innerClassIterator.hasNext()) {
					((ClassNode)innerClassIterator.next()).removeLineNumberMetadata();
				}
			}
		}
	}

	private void obfuscateLocalVariableNames() {
		for (ClassNode classNode : super.classes) {
			classNode.refreshMethodLocalVariables();
			if (classNode.hasInnerClasses()) {
				Iterator<AbstractClassNode> innerClassIterator = classNode.getInnerClasses().iterator();

				while (innerClassIterator.hasNext()) {
					((ClassNode)innerClassIterator.next()).refreshMethodLocalVariables();
				}
			}
		}
	}

	private void shuffleMemberOrder() {
		Random random = GlobalUtils.newRandom(93);

		for (ClassNode classNode : super.classes) {
			classNode.shuffleMemberOrder(random);
			if (classNode.hasInnerClasses()) {
				Iterator<AbstractClassNode> innerClassIterator = classNode.getInnerClasses().iterator();

				while (innerClassIterator.hasNext()) {
					((ClassNode)innerClassIterator.next()).shuffleMemberOrder(random);
				}
			}
		}
	}

	private void encryptIntegerConstants(
		IntegerEncryptionExcludes encryptionExcludes,
		NestedMultiMap<ClassNode, MethodNode, IntegerNode, PriorityEntry<WritableType>> integerConstantEntries,
		KeyValuePairMultiMap<ClassNode, IntegerNode, FieldNode> staticIntegerFieldPairs,
		KeyValuePairMultiMap<ClassNode, IntegerNode, FieldNode> instanceIntegerFieldPairs,
		IntegerEncryptionBuilder integerEncryptionBuilder,
		Inheritance inheritance,
		HashMap<ClassNode, Boolean> microEditionClasses,
		ParameterChangerBuilder parameterChangerBuilder,
		Map<MethodNode, Long> encryptionKeysByMethod,
		Map<MethodNode, LocalVariableOperand> operandsByMethod,
		TypeResolver typeResolver,
		boolean unused,
		ScriptContext scriptContext
	) {
		super.workspace.markIntegerConstantsEncrypted();
		ArrayList<ClassNode> verboseEncryptionClasses;
		if (scriptContext.isVerbose()) {
			verboseEncryptionClasses = new ArrayList<>(integerConstantEntries.size());
		} else {
			verboseEncryptionClasses = null;
		}

		HashSet<AbstractClassNode> encryptedClasses = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(super.workspace.getClassesCount()));

		for (ClassHierarchyNode rootInterface : super.classTreeManager.getRootInterfaces()) {
			if (!rootInterface.getClassNodeIfWorkspace().isGeneratedClass()) {
				rootInterface.encryptIntegerConstants(
					encryptionExcludes, integerConstantEntries, staticIntegerFieldPairs, instanceIntegerFieldPairs, integerEncryptionBuilder, inheritance, encryptedClasses, microEditionClasses, parameterChangerBuilder, encryptionKeysByMethod, operandsByMethod, super.workspace, verboseEncryptionClasses, super.classProvider, typeResolver, unused, scriptContext, true
				);
			}
		}

		for (ClassHierarchyNode workspaceClassRoot : super.classTreeManager.getWorkspaceClassRoots()) {
			if (!workspaceClassRoot.isInterface() && !workspaceClassRoot.getClassNodeIfWorkspace().isGeneratedClass()) {
				workspaceClassRoot.encryptIntegerConstants(
					encryptionExcludes, integerConstantEntries, staticIntegerFieldPairs, instanceIntegerFieldPairs, integerEncryptionBuilder, inheritance, encryptedClasses, microEditionClasses, parameterChangerBuilder, encryptionKeysByMethod, operandsByMethod, super.workspace, verboseEncryptionClasses, super.classProvider, typeResolver, unused, scriptContext, false
				);
			}
		}

		if (scriptContext.isVerbose() && verboseEncryptionClasses != null) {
			PrintWriter writer = scriptContext.getWriter();
			Obfuscator.IntegerEncryptionComparator integerEncryptionComparator = new Obfuscator.IntegerEncryptionComparator(this);
			Collections.sort(verboseEncryptionClasses, integerEncryptionComparator);

			for (ClassNode encryptedClass : verboseEncryptionClasses) {
				writer.println("\tInteger Constant Encryption performed in class '" + encryptedClass.getSourcePath() + "'");
			}
		}
	}

	private void scrambleAndRecordLineNumbers(
		HierarchicalMultiMap<String, Integer, Integer> lineNumberMappings,
		HashMap<String, String> newToOldClassNameMap,
		FixedClassManager fixedClassManager,
		ChangeLogContext changeLogContext,
		boolean isLooseChangeLogFile,
		ScriptContext scriptContext
	) {
		Random random = GlobalUtils.newRandom(4096);

		for (int i = 0; i < super.classes.length; i++) {
			MultiMap<Integer, Integer> classLineNumbers = null;
			ClassNode classNode = super.classes[i];
			String mappedInternalName = GlobalUtils.getMappedOrDefault(classNode.getInternalName(), newToOldClassNameMap);
			if (changeLogContext != null) {
				classLineNumbers = changeLogContext.getLineNumberMappings(mappedInternalName);
			}

			if (fixedClassManager != null && fixedClassManager.isInFixedClasses(classNode)) {
				if (classLineNumbers != null && !classLineNumbers.isEmpty() && (scriptContext.isVerbose() || !isLooseChangeLogFile)) {
					scriptContext.logWarning(
						"Did not remap line numbers in class '"
							+ GlobalUtils.internalToBinaryName(mappedInternalName)
							+ "' as specified in input change log '"
							+ changeLogContext.getLogFileName()
							+ "' because it is a fixed class."
					);
				}

				MultiMap<Integer, Integer> identityLineNumbers = classNode.collectLineNumberIdentityMap(super.integerCache);
				lineNumberMappings.put(classNode.getInternalName(), identityLineNumbers);
			} else {
				MultiMap<Integer, Integer> scrambledLineNumbers = classNode.remapAndScrambleLineNumbers(super.integerCache, classLineNumbers, random);
				lineNumberMappings.put(classNode.getInternalName(), scrambledLineNumbers);
				if (classNode.hasInnerClasses()) {
					for (AbstractClassNode innerClass : classNode.getInnerClasses()) {
						((ClassNode)innerClass).removeLineNumberMetadata();
						scriptContext.logWarning(
							"Cannot scramble line numbers for multi-release versions.  Line numbers for version '"
								+ innerClass.getInnerClassOrdinal()
								+ "' of class '"
								+ classNode.getClassNameBinary()
								+ "' deleted."
						);
					}
				}
			}
		}
	}

	public static List<String> loadOverrideNamesFromFile(String fileName, String parameterName, ScriptContext scriptContext) {
		File defaultDirectory = scriptContext.getDefaultDirectory();
		fileName = FileUtils.resolveRelativePath(FileUtils.normalizePathSeparators(fileName), defaultDirectory);
		File overrideNamesFile;
		if (FileUtils.isRelativePath(fileName)) {
			overrideNamesFile = new File(defaultDirectory, fileName);
		} else {
			overrideNamesFile = new File(fileName);
		}

		if (overrideNamesFile.exists()) {
			if (!overrideNamesFile.isDirectory()) {
				OrderedSet<String> overrideNames = new OrderedSet<>();
				BufferedReader reader = null;

				try {
					AtomicReference<String> detectedEncoding = new AtomicReference<>();
					reader = FileUtils.createReaderWithBomDetection(new FileInputStream(overrideNamesFile), "UTF-8", detectedEncoding);

					String line;
					while ((line = reader.readLine()) != null) {
						line = line.trim();
						if (line.length() > 0 && (line.length() < 2 || line.charAt(0) != '/' && line.charAt(1) != '/') && (Options.dontTreatHashAsComment || line.charAt(0) != '#')) {
							String rawLine = line;
							if (!Options.skipNameListUnescape) {
								line = GlobalUtils.unescapeString(line);
							}
							if (Options.validateNameListIdentifiers) {
								if (!Character.isJavaIdentifierStart(line.charAt(0))) {
									scriptContext.logWarning(
										"Line '" + rawLine + "' in file '" + overrideNamesFile.getAbsolutePath() + "' specified by '" + parameterName + "' parameter is not a valid Java identifier and will not be used. (A)"
									);
									continue;
								}
							}
							for (int charIndex = 0; charIndex < line.length(); charIndex++) {
								if (Character.isWhitespace(line.charAt(charIndex))) {
									scriptContext.logWarning(
										"Line '" + rawLine + "' in file '" + overrideNamesFile.getAbsolutePath() + "' specified by '" + parameterName + "' parameter contains white space at " + charIndex
											+ ". If you have specified a change log that needs to be parsable then it may cause unexpected problems."
									);
								}
							}

							overrideNames.add(line);
						}
					}
				} catch (FileNotFoundException fileNotFoundException) {
					scriptContext.logFatalError("Could not read file '" + overrideNamesFile.getAbsolutePath() + "' specified by '" + parameterName + "' parameter : '" + fileNotFoundException.getMessage() + "'.");
				} catch (IOException ioException) {
					scriptContext.logFatalError("Error reading file '" + overrideNamesFile.getAbsolutePath() + "' specified by '" + parameterName + "' parameter : '" + ioException.getMessage() + "'.");
				} finally {
					if (reader != null) {
						try {
							reader.close();
						} catch (IOException ignored) {
						}
					}
				}

				return new ArrayList<>(overrideNames);
			}

			scriptContext.logFatalError("File '" + overrideNamesFile.getAbsolutePath() + "' specified by '" + parameterName + "' parameter is a directory.");
		} else {
			scriptContext.logFatalError("File '" + overrideNamesFile.getAbsolutePath() + "' specified by '" + parameterName + "' parameter does not exist.");
		}

		return null;
	}

	public final void runClassNameObfuscation(
		DefaultExcludes excludes,
		ChangeLogContext changeLogContext,
		boolean randomizeObfuscation,
		boolean useAsciiCharactersOnly,
		boolean legalIdentifiers,
		String newNamesPrefix,
		int keepInnerClassInfo,
		int keepGenericsInformation,
		MixCaseType mixedCaseClassNames,
		boolean uniqueClassNames,
		String newClassNameFile,
		HashMap<String, String> packageNameMap,
		HashMap<String, String> classNameToSourceFileMap,
		HashMap<String, String> oldToNewClassNameMap,
		HashMap<String, String> newToOldClassNameMap,
		AbstractClassNode[] changeLogClasses,
		AbstractLogger logger,
		ScriptContext scriptContext
	) {
		MultiMap<String, String> invertedPackageNameMap = AbstractObfuscator.invertClassNameMap(packageNameMap);
		ClassStruct classStruct = new ClassStruct(excludes, changeLogContext, randomizeObfuscation, super.workspace, super.classes, changeLogClasses, super.classTreeManager, oldToNewClassNameMap, newToOldClassNameMap);
		List<String> overrideNames = null;
		if (newClassNameFile != null && newClassNameFile.length() > 0) {
			overrideNames = loadOverrideNamesFromFile(newClassNameFile, "newClassNameFile", scriptContext);
		}

		Object renamer;
		if (uniqueClassNames) {
			renamer = new UniqueClassRenamer(classStruct, useAsciiCharactersOnly, legalIdentifiers, newNamesPrefix, keepInnerClassInfo, keepGenericsInformation, randomizeObfuscation, mixedCaseClassNames, packageNameMap, invertedPackageNameMap, super.packageManager.getPackageTreeNodes(), overrideNames);
		} else {
			renamer = new SimpleClassRenamer(classStruct, useAsciiCharactersOnly, legalIdentifiers, newNamesPrefix, keepInnerClassInfo, keepGenericsInformation, randomizeObfuscation, mixedCaseClassNames, packageNameMap, invertedPackageNameMap, super.packageManager.getPackageTreeNodes(), overrideNames);
		}

		classStruct.obfuscateClassNames((ClassRenamer)renamer, classNameToSourceFileMap, packageNameMap, invertedPackageNameMap, logger);
	}

	private void removeAllLocalVars() {
		for (ClassNode classNode : super.classes) {
			classNode.removeMethodLocalVariables();
			if (classNode.hasInnerClasses()) {
				Iterator<AbstractClassNode> innerClassIterator = classNode.getInnerClasses().iterator();

				while (innerClassIterator.hasNext()) {
					((ClassNode)innerClassIterator.next()).removeMethodLocalVariables();
				}
			}
		}
	}

	private boolean verifyWithPhantomFallback(
		ClassNode classNode, Map<ClassNode, String> unresolvedClasses, TypeResolver typeResolver, AbstractWorkspace workspace, boolean logRemovedInstructions, ScriptContext scriptContext, PhantomClassManager phantomClassManager
	) {
		boolean classPruned = false;

		try {
			return classNode.removeDeadCodeInMethods(typeResolver, workspace, logRemovedInstructions, scriptContext);
		} catch (ClasspathException classpathException) {
			if (Options.usePhantomClassFallback && phantomClassManager != null) {
				String errorMessage = classpathException.getMessage();
				int firstQuoteIndex = errorMessage.indexOf(39);
				int secondQuoteIndex = errorMessage.indexOf(39, firstQuoteIndex + 1);
				if (secondQuoteIndex > firstQuoteIndex) {
					String missingClassName = errorMessage.substring(firstQuoteIndex + 1, secondQuoteIndex).trim();
					if (phantomClassManager.isInIgnoredClasses(GlobalUtils.binaryToInternalName(missingClassName))) {
						int lastColonIndex = errorMessage.lastIndexOf(58);
						if (lastColonIndex > 0) {
							int openQuoteIndex = errorMessage.indexOf("'", lastColonIndex);
							if (openQuoteIndex > lastColonIndex) {
								int closeQuoteIndex = errorMessage.indexOf("'", openQuoteIndex + 1);
								if (closeQuoteIndex > openQuoteIndex) {
									errorMessage.substring(openQuoteIndex + 1, closeQuoteIndex).trim();
									unresolvedClasses.put(classNode, missingClassName);
									return classPruned;
								}
							}
						}
					}
				}
			}

			throw classpathException;
		}
	}

	private void analyzeAutoReflectionClass(
		ClassNode classNode, boolean useDoubleArrayStorage, boolean useGoto, ParameterRotateEntry parameterRotateEntry, TypeResolver typeResolver, ClassHierarchy classHierarchy, ScriptContext scriptContext
	) {
		this.analyzeHierarchyReferences(classNode, useDoubleArrayStorage, classNode.isJava6OrNewer(), useGoto, Integer.MAX_VALUE, true, false, parameterRotateEntry, typeResolver, classHierarchy, scriptContext, 9);
	}

	public final void runMethodNameObfuscation(
		DefaultExcludes excludes,
		ParameterChangerExcludes parameterChangerExcludes,
		FixedClassManager fixedClassManager,
		ChangeLogContext changeLogContext,
		boolean isLooseChangeLogFile,
		int keepInnerClassInfo,
		boolean aggressiveMethodRenaming,
		boolean uniqueMethodNames,
		boolean randomizeObfuscation,
		boolean licenseVerified,
		boolean useAsciiCharactersOnly,
		boolean legalIdentifiers,
		String newNamesPrefix,
		boolean hideStaticMethodNames,
		HashMap<String, String> newToOldClassNameMap,
		DualKeyMap<String, MethodSignature, MethodSignature> oldToNewMethodMappings,
		DualKeyMap<String, MethodSignature, MethodSignature> newToOldMethodMappings,
		AbstractClassNode[] changeLogClasses,
		Map<MethodNode, String> resourceProtectedStaticMethods,
		List<String> newMethodNameList,
		AtomicReference<Map<AbstractMethodNode, AbstractMethodNode>> baseMethodMapRef,
		ScriptContext scriptContext
	) {
		MethodRenamer methodRenamer = new MethodRenamer(
			excludes, parameterChangerExcludes, fixedClassManager, changeLogContext, isLooseChangeLogFile, super.workspace, super.classes, changeLogClasses, super.classTreeManager, super.classProvider, keepInnerClassInfo, aggressiveMethodRenaming, uniqueMethodNames, randomizeObfuscation, licenseVerified, newToOldClassNameMap, oldToNewMethodMappings, newToOldMethodMappings, resourceProtectedStaticMethods, baseMethodMapRef, scriptContext
		);
		SimpleMethodNameGenerator methodNameGenerator = new SimpleMethodNameGenerator(methodRenamer, excludes, parameterChangerExcludes, useAsciiCharactersOnly, legalIdentifiers, randomizeObfuscation, newNamesPrefix, hideStaticMethodNames, newMethodNameList);
		methodRenamer.renameMethods(methodNameGenerator);
	}

	private void obfuscateHierarchyReferences(
		ClassHierarchyNode classHierarchyNode,
		ClassNode classNode,
		boolean useBaseClass,
		Map<ClassNode, ClassNode> baseClassNodeByClass,
		DualKeyMap<ClassNode, MemberContext, Integer> memberIds,
		int[] memberNamePrefixes,
		int[] memberNameSuffixes,
		DualKeyMap<ClassNode, Integer, Long> encryptionKeysById,
		HierarchicalMultiMapWrapper<ClassNode, InstructionListNode, MemberContext> memberContextsByInstruction,
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> memberObfuscationRefs,
		MultiMap<ClassNode, MemberContext> memberContextsByClass,
		Map<TypeWriter, MemberContext> memberContextByTypeWriter,
		DualKeyMap<ClassNode, MemberContext, AbstractMethodCallWriter> methodCallWriters,
		AtomicBool obfuscationApplied,
		IReferenceGraph<ClassNode, ClassNode> referenceClassGraph,
		Map<ClassNode, AtomicReference<AbstractMethodCallWriter>> classResolverMethodRef,
		Map<ClassNode, AtomicReference<AbstractMethodCallWriter>> methodResolverMethodRef,
		Map<ClassNode, AtomicReference<AbstractMethodCallWriter>> indyBootstrapMethodRef,
		AtomicInt obfuscatedClassesCount,
		AtomicInt obfuscatedMethodsCount,
		AtomicInt obfuscatedReferencesCount,
		Inheritance inheritance,
		TypeResolver typeResolver,
		Map<MethodNode, Long> encryptionKeysByMethod,
		Map<MethodNode, LocalVariableOperand> operandsByMethod,
		ObfuscateReferencesBuilder obfuscateReferencesBuilder,
		ScriptContext scriptContext
	) {
		ClassNode baseClassNode = baseClassNodeByClass.get(classNode);
		if (memberContextsByInstruction.containsKey(classNode)) {
			ClassNode targetClassNode = useBaseClass ? baseClassNode : classNode;
			Map<MemberContext, Integer> baseClassMemberIds = useBaseClass ? memberIds.getGroup(baseClassNode) : memberIds.getGroup(classNode);
			MultiMap<InstructionListNode, MemberContext> innerMemberContexts = memberContextsByInstruction.getInnerMapCopy(classNode);
			Map<MemberContext, AbstractMethodCallWriter> baseClassMethodCallWriters = useBaseClass ? methodCallWriters.getGroup(baseClassNode) : null;
			Map<Integer, Long> baseClassEncryptionKeys = useBaseClass ? encryptionKeysById.getGroup(baseClassNode) : null;
			AbstractMethodCallWriter classResolverCallWriter = null;
			AbstractMethodCallWriter methodResolverCallWriter = null;
			AbstractMethodCallWriter indyBootstrapCallWriter = null;
			if (useBaseClass) {
				classResolverCallWriter = classResolverMethodRef.get(baseClassNode).get();
				methodResolverCallWriter = methodResolverMethodRef.get(baseClassNode).get();
				indyBootstrapCallWriter = indyBootstrapMethodRef.get(baseClassNode).get();
			}

			try {
				if (classNode.applyReferenceObfuscation(
					useBaseClass,
					targetClassNode,
					baseClassMemberIds,
					innerMemberContexts,
					memberObfuscationRefs,
					memberContextsByClass,
					memberContextsByClass.get(classNode),
					memberContextByTypeWriter,
					baseClassMethodCallWriters,
					inheritance,
					memberNamePrefixes,
					memberNameSuffixes,
					baseClassEncryptionKeys,
					classResolverCallWriter,
					methodResolverCallWriter,
					indyBootstrapCallWriter,
					obfuscatedClassesCount,
					obfuscatedMethodsCount,
					obfuscatedReferencesCount,
					encryptionKeysByMethod,
					operandsByMethod,
					super.workspace,
					typeResolver,
					super.classProvider,
					obfuscateReferencesBuilder,
					scriptContext
				)) {
					obfuscationApplied.set(true);
					if (useBaseClass) {
						referenceClassGraph.addReference(baseClassNode, classNode);
					}
				}
			} catch (MethodAnalyzerException methodAnalyzerException) {
				scriptContext.logMessage("Couldn't obfuscate references in '" + classNode.getSourcePath() + "' : \"" + methodAnalyzerException.getMessage());
			} catch (InvalidBytecodeException invalidBytecodeException) {
				scriptContext.logMessage("Couldn't obfuscate references in '" + classNode.getSourcePath() + "' : \"" + invalidBytecodeException.getMessage() + "\"");
			}
		}

		if (!classHierarchyNode.isInterface() && !classNode.isInnerClass()) {
			Enumeration<ClassHierarchyNode> subclassEnumeration = classHierarchyNode.getSubclassesEnumeration();
			if (subclassEnumeration != null) {
				while (subclassEnumeration.hasMoreElements()) {
					ClassHierarchyNode subclassNode = subclassEnumeration.nextElement();
					this.obfuscateHierarchyReferences(
						subclassNode,
						subclassNode.getClassNodeIfWorkspace(),
						useBaseClass,
						baseClassNodeByClass,
						memberIds,
						memberNamePrefixes,
						memberNameSuffixes,
						encryptionKeysById,
						memberContextsByInstruction,
						memberObfuscationRefs,
						memberContextsByClass,
						memberContextByTypeWriter,
						methodCallWriters,
						obfuscationApplied,
						referenceClassGraph,
						classResolverMethodRef,
						methodResolverMethodRef,
						indyBootstrapMethodRef,
						obfuscatedClassesCount,
						obfuscatedMethodsCount,
						obfuscatedReferencesCount,
						inheritance,
						typeResolver,
						encryptionKeysByMethod,
						operandsByMethod,
						obfuscateReferencesBuilder,
						scriptContext
					);
				}
			}
		}
	}

	private void runReferenceObfuscationPass(
		ObfuscateReferenceExcludes obfuscateReferenceExcludes,
		boolean obfuscateReferenceInSpecialClass,
		int majorVersion,
		int minorVersion,
		ChangeLogContext changeLogContext,
		String newNamesPrefix,
		String obfuscateReferencesPackage,
		MixCaseType mixedCaseClassNames,
		boolean uniqueClassNames,
		UnmodifiableMultiMap<ArchiveNode, ClassNode> namedModuleClasses,
		ReferenceGraph<ArchiveNode, ArchiveNode> moduleDependencyGraph,
		ReferenceGraph<ArchiveNode, String> modulePackageNameGraph,
		AtomicReference<ClassNode> referenceObfuscationDataClassNode,
		Map<ArchiveNode, ClassNode> archiveToDataClassNodeMap,
		IReferenceGraph<ClassNode, ClassNode> autoReflectionClassGraph,
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> memberObfuscationRefs,
		ObfuscateReferencesBuilder obfuscateReferencesBuilder,
		ParameterRotateEntry parameterRotateEntry,
		Map<MethodNode, Long> encryptionKeysByMethod,
		Map<MethodNode, LocalVariableOperand> operandsByMethod,
		Workspace workspace,
		FixedClassManager fixedClassManager,
		Inheritance inheritance,
		TypeResolver typeResolver,
		boolean licenseVerified,
		ScriptContext scriptContext
	) {
		HashMap<TypeWriter, MemberContext> memberContextByTypeWriter = GlobalUtils.newHashMap();
		ReferenceGraphMap<ClassNode, InstructionListNode, MemberContext> memberReferenceGraphMap = new ReferenceGraphMap<>();
		ReferenceGraph<ClassNode, MemberContext> classMemberReferenceGraph = new ReferenceGraph<>();
		boolean referencesFound = false;

		for (Entry<MemberContext, MultiMap<InstructionListNode, WideConstantPoolInsnNode>> memberContextEntry : memberObfuscationRefs.entrySet()) {
			MemberContext memberContext = memberContextEntry.getKey();
			MemberNode memberNode = memberContext.getMemberNode();
			boolean memberIncluded = memberNode.isField() && obfuscateReferenceExcludes.isFieldIncluded((AbstractFieldNode)memberNode) || memberNode.isMethod() && obfuscateReferenceExcludes.isMethodIncluded((AbstractMethodNode)memberNode);
			if (memberIncluded) {
				for (Entry<InstructionListNode, List<WideConstantPoolInsnNode>> referenceEntry : memberContextEntry.getValue().entrySet()) {
					InstructionListNode referenceInstruction = referenceEntry.getKey();
					List<WideConstantPoolInsnNode> constantPoolInsns = referenceEntry.getValue();
					ClassNode instructionClass = (ClassNode)referenceInstruction.getClassNode();
					if ((fixedClassManager == null || !fixedClassManager.isInFixedClasses(instructionClass))
						&& memberIncluded
						&& (memberNode.isField() && obfuscateReferenceExcludes.isFieldReferenceIncluded((AbstractFieldNode)memberNode, referenceInstruction) || memberNode.isMethod() && obfuscateReferenceExcludes.isMethodReferenceIncluded((AbstractMethodNode)memberNode, referenceInstruction))) {
						referencesFound = true;
						memberReferenceGraphMap.addReference(instructionClass, referenceInstruction, memberContext);
						classMemberReferenceGraph.addReference(instructionClass, memberContext);

						for (WideConstantPoolInsnNode constantPoolInsn : constantPoolInsns) {
							memberContextByTypeWriter.put(constantPoolInsn.getTypeWriter(), memberContext);
						}
					}
				}
			}
		}

		HierarchicalMultiMapWrapper<ClassNode, InstructionListNode, MemberContext> memberContextsByInstruction = memberReferenceGraphMap.toHierarchicalMultiMap();
		MultiMap<ClassNode, MemberContext> memberContextsByClass = classMemberReferenceGraph.toMultiMap();
		if (referencesFound) {
			boolean useDoubleArrayStorage = obfuscateReferenceInSpecialClass;
			HashMap<ClassNode, AtomicReference<AbstractMethodCallWriter>> classResolverMethodRefs = GlobalUtils.newHashMap();
			HashMap<ClassNode, AtomicReference<AbstractMethodCallWriter>> methodResolverMethodRefs = GlobalUtils.newHashMap();
			HashMap<ClassNode, AtomicReference<AbstractMethodCallWriter>> indyBootstrapMethodRefs = GlobalUtils.newHashMap();
			DualKeyMap<ClassNode, MemberContext, AbstractMethodCallWriter> methodCallWriters = new DualKeyMap<>();
			DualKeyMap<ClassNode, MemberContext, Integer> memberIdsByClass = new DualKeyMap<>();
			int[] memberNamePrefixes = null;
			int[] memberNameSuffixes = null;
			DualKeyMap<ClassNode, Integer, Long> encryptionKeysByClass = null;
			HashMap<ClassNode, ClassNode> classToDataClassNodeMap = GlobalUtils.newHashMap();
			HashSet<ClassNode> generatedDataClasses = GlobalUtils.newHashSet();
			if (obfuscateReferenceInSpecialClass) {
				encryptionKeysByClass = new DualKeyMap<>();
				ReadOnlyMap<String, String> packageRenameMap = workspace.getPackageRenameMap();
				ReadOnlyMap<String, String> classNameToInternalNameMap = workspace.getClassNameToInternalNameMap();
				ReadOnlyMap<String, String> internalToBinaryNameMap = workspace.getInternalToBinaryNameMap();
				HashMap<ArchiveNode, String> archiveToDataClassNameMap = GlobalUtils.newHashMap();
				HashMap<ClassNode, String> classToDataClassNameMap = GlobalUtils.newHashMap();
				AtomicReference<String> referenceObfuscationDataClassName = new AtomicReference<>();
				Set<String> dataClassNames = obfuscateReferencesBuilder.planAuxiliaryClassName(
					GlobalUtils.toSet(super.classes),
					referenceObfuscationDataClassName,
					archiveToDataClassNameMap,
					classToDataClassNameMap,
					changeLogContext,
					obfuscateReferencesPackage,
					moduleDependencyGraph,
					modulePackageNameGraph,
					namedModuleClasses,
					packageRenameMap,
					classNameToInternalNameMap,
					internalToBinaryNameMap,
					newNamesPrefix,
					mixedCaseClassNames,
					uniqueClassNames,
					workspace,
					super.classRepository,
					scriptContext
				);
				super.classes = InjectedClassManager.generateAndRegisterClasses(
					dataClassNames,
					classToDataClassNameMap,
					referenceObfuscationDataClassName,
					referenceObfuscationDataClassNode,
					archiveToDataClassNodeMap,
					classToDataClassNodeMap,
					generatedDataClasses,
					archiveToDataClassNameMap,
					majorVersion,
					minorVersion,
					obfuscateReferencesBuilder,
					workspace,
					null
				);
				MultiMap<InstructionListNode, MemberContext> referencesByInstruction = memberContextsByInstruction.flattenToMultiMap();
				Map<Types, Integer> idsByType = obfuscateReferencesBuilder.assignIdsToTypes(referencesByInstruction, super.classProvider, scriptContext.getPhantomClassManager());
				ReferenceGraph<ClassNode, ClassNode> sourceClassesByDataClass = new ReferenceGraph<>();

				for (Entry<ClassNode, ClassNode> classToDataClassEntry : classToDataClassNodeMap.entrySet()) {
					ClassNode dataClassNode = classToDataClassEntry.getValue();
					ClassNode classNode = classToDataClassEntry.getKey();
					sourceClassesByDataClass.addReference(dataClassNode, classToDataClassEntry.getKey());
					if (!classNode.isFromFilesystem() && dataClassNode.isFromFilesystem()) {
						dataClassNode.markLoadedFromFilesystem();
					}
				}

				for (ClassNode generatedDataClass : generatedDataClasses) {
					boolean useIndy = ObfuscateReferencesBuilder.shouldUseIndy(generatedDataClass, null);
					AtomicBool hasIndyMembers = new AtomicBool();
					AtomicBool hasNonIndyMembers = new AtomicBool();
					AtomicBool hasFieldReferences = new AtomicBool();
					AtomicBool hasMethodReferences = new AtomicBool();
					HashSet<MemberContext> nonIndyMembers = GlobalUtils.newHashSet();
					MultiMap<InstructionListNode, MemberContext> filteredReferencesByInstruction = referencesByInstruction;
					if (generatedDataClasses.size() > 1) {
						Set<ClassNode> sourceClasses = sourceClassesByDataClass.getReferences(generatedDataClass);
						filteredReferencesByInstruction = new MultiMap<>(referencesByInstruction.size());

						for (Entry<InstructionListNode, List<MemberContext>> instructionEntry : referencesByInstruction.entrySet()) {
							InstructionListNode instructionNode = instructionEntry.getKey();
							if (sourceClasses.contains(instructionNode.getClassNode())) {
								filteredReferencesByInstruction.putAll(instructionNode, instructionEntry.getValue());
							}
						}

						if (filteredReferencesByInstruction.isEmpty()) {
							continue;
						}
					}

					Map<MemberContext, Integer> memberIds = obfuscateReferencesBuilder.assignIdsToMembers(
						useIndy, hasIndyMembers, hasNonIndyMembers, filteredReferencesByInstruction, idsByType.size(), nonIndyMembers, hasFieldReferences, hasMethodReferences, workspace
					);
					memberIdsByClass.putGroup(generatedDataClass, memberIds);
					useDoubleArrayStorage = idsByType.size() + memberIds.size() <= 65535;
					memberNamePrefixes = obfuscateReferencesBuilder.createSaltTable();
					if (memberNameSuffixes == null) {
						memberNameSuffixes = obfuscateReferencesBuilder.getAccessKindCodes();
					}

					HashMap<Integer, Long> encryptionKeysById = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(idsByType.size() + memberIdsByClass.getGroupCount()));
					encryptionKeysByClass.putGroup(generatedDataClass, encryptionKeysById);
					classResolverMethodRefs.put(generatedDataClass, new AtomicReference<>());
					methodResolverMethodRefs.put(generatedDataClass, new AtomicReference<>());
					indyBootstrapMethodRefs.put(generatedDataClass, new AtomicReference<>());
					ArrayList<TypeWriter> typeWriters = new ArrayList<>();
					ClassNode[] inheritanceClasses = new ClassNode[]{generatedDataClass};
					Inheritance classInheritance = new Inheritance(inheritanceClasses, super.classProvider);
					if (parameterRotateEntry != null) {
						ArchiveNode currentArchiveNode = null;

						for (Entry<ArchiveNode, ClassNode> archiveEntry : archiveToDataClassNodeMap.entrySet()) {
							if (archiveEntry.getValue() == generatedDataClass) {
								currentArchiveNode = archiveEntry.getKey();
								break;
							}
						}

						parameterRotateEntry.setCurrentArchiveNode(currentArchiveNode);
					}

					generatedDataClass.initReferenceObfuscation(
						true,
						hasIndyMembers.get(),
						hasNonIndyMembers.get(),
						idsByType,
						memberIdsByClass.getGroup(generatedDataClass),
						memberNamePrefixes,
						memberNameSuffixes,
						encryptionKeysByClass.getGroup(generatedDataClass),
						typeWriters,
						memberContextsByClass,
						classInheritance,
						parameterRotateEntry,
						nonIndyMembers,
						super.classProvider,
						workspace,
						obfuscateReferencesBuilder,
						methodCallWriters,
						classResolverMethodRefs.get(generatedDataClass),
						methodResolverMethodRefs.get(generatedDataClass),
						indyBootstrapMethodRefs.get(generatedDataClass),
						hasFieldReferences.get(),
						hasMethodReferences.get()
					);
					generatedDataClass.appendEntriesToConstantPool(typeWriters);
				}
			}

			AtomicInt obfuscatedClassCount = new AtomicInt();
			AtomicInt obfuscatedMethodCount = new AtomicInt();
			AtomicInt obfuscatedReferenceCount = new AtomicInt();
			AtomicBool obfuscationApplied = new AtomicBool();
			ClassTreeManager workspaceClassTreeManager = workspace.getClassTreeManager();

			for (ClassNode candidateClass : super.classes) {
				if (candidateClass.isInterface() && !generatedDataClasses.contains(candidateClass) && !candidateClass.isGeneratedClass()) {
					ClassHierarchyNode hierarchyNode = ClassHierarchyNode.getNodeByInternalName(candidateClass.getInternalName());
					this.obfuscateHierarchyReferences(
						hierarchyNode,
						candidateClass,
						obfuscateReferenceInSpecialClass,
						classToDataClassNodeMap,
						memberIdsByClass,
						memberNamePrefixes,
						memberNameSuffixes,
						encryptionKeysByClass,
						memberContextsByInstruction,
						memberObfuscationRefs,
						memberContextsByClass,
						memberContextByTypeWriter,
						methodCallWriters,
						obfuscationApplied,
						autoReflectionClassGraph,
						classResolverMethodRefs,
						methodResolverMethodRefs,
						indyBootstrapMethodRefs,
						obfuscatedClassCount,
						obfuscatedMethodCount,
						obfuscatedReferenceCount,
						inheritance,
						typeResolver,
						encryptionKeysByMethod,
						operandsByMethod,
						obfuscateReferencesBuilder,
						scriptContext
					);
					if (candidateClass.hasInnerClasses()) {
						for (AbstractClassNode innerClass : candidateClass.getInnerClasses()) {
							this.obfuscateHierarchyReferences(
								hierarchyNode,
								(ClassNode)innerClass,
								obfuscateReferenceInSpecialClass,
								classToDataClassNodeMap,
								memberIdsByClass,
								memberNamePrefixes,
								memberNameSuffixes,
								encryptionKeysByClass,
								memberContextsByInstruction,
								memberObfuscationRefs,
								memberContextsByClass,
								memberContextByTypeWriter,
								methodCallWriters,
								obfuscationApplied,
								autoReflectionClassGraph,
								classResolverMethodRefs,
								methodResolverMethodRefs,
								indyBootstrapMethodRefs,
								obfuscatedClassCount,
								obfuscatedMethodCount,
								obfuscatedReferenceCount,
								inheritance,
								typeResolver,
								encryptionKeysByMethod,
								operandsByMethod,
								obfuscateReferencesBuilder,
								scriptContext
							);
						}
					}
				}
			}

			for (ClassHierarchyNode workspaceClassRoot : workspaceClassTreeManager.getWorkspaceClassRoots()) {
				ClassNode rootClassNode = workspaceClassRoot.getClassNodeIfWorkspace();
				if (!rootClassNode.isInterface() && !generatedDataClasses.contains(rootClassNode) && !rootClassNode.isGeneratedClass()) {
					this.obfuscateHierarchyReferences(
						workspaceClassRoot,
						rootClassNode,
						obfuscateReferenceInSpecialClass,
						classToDataClassNodeMap,
						memberIdsByClass,
						memberNamePrefixes,
						memberNameSuffixes,
						encryptionKeysByClass,
						memberContextsByInstruction,
						memberObfuscationRefs,
						memberContextsByClass,
						memberContextByTypeWriter,
						methodCallWriters,
						obfuscationApplied,
						autoReflectionClassGraph,
						classResolverMethodRefs,
						methodResolverMethodRefs,
						indyBootstrapMethodRefs,
						obfuscatedClassCount,
						obfuscatedMethodCount,
						obfuscatedReferenceCount,
						inheritance,
						typeResolver,
						encryptionKeysByMethod,
						operandsByMethod,
						obfuscateReferencesBuilder,
						scriptContext
					);
					if (rootClassNode.hasInnerClasses()) {
						for (AbstractClassNode rootInnerClass : rootClassNode.getInnerClasses()) {
							this.obfuscateHierarchyReferences(
								workspaceClassRoot,
								(ClassNode)rootInnerClass,
								obfuscateReferenceInSpecialClass,
								classToDataClassNodeMap,
								memberIdsByClass,
								memberNamePrefixes,
								memberNameSuffixes,
								encryptionKeysByClass,
								memberContextsByInstruction,
								memberObfuscationRefs,
								memberContextsByClass,
								memberContextByTypeWriter,
								methodCallWriters,
								obfuscationApplied,
								autoReflectionClassGraph,
								classResolverMethodRefs,
								methodResolverMethodRefs,
								indyBootstrapMethodRefs,
								obfuscatedClassCount,
								obfuscatedMethodCount,
								obfuscatedReferenceCount,
								inheritance,
								typeResolver,
								encryptionKeysByMethod,
								operandsByMethod,
								obfuscateReferencesBuilder,
								scriptContext
							);
						}
					}
				}
			}

			if (obfuscationApplied.get()) {
				if (scriptContext.isVerbose()) {
					scriptContext.getWriter()
						.println(
							"\tReference Obfuscation : Obfuscated "
								+ obfuscatedReferenceCount.get()
								+ " reference"
								+ (obfuscatedReferenceCount.get() == 1 ? "" : 's')
								+ " in "
								+ obfuscatedMethodCount.get()
								+ " method"
								+ (obfuscatedMethodCount.get() == 1 ? "" : 's')
								+ " in "
								+ obfuscatedClassCount.get()
								+ " class"
								+ (obfuscatedClassCount.get() == 1 ? "" : "es")
								+ "."
						);
				}

				if (obfuscateReferenceInSpecialClass) {
					HashSet<ClassNode> autoReflectionClasses = GlobalUtils.newHashSet(autoReflectionClassGraph.getMembers());

					for (ClassNode autoReflectionClass : autoReflectionClasses) {
						if (Options.analyzeReferenceDataClass) {
							boolean useGoto = licenseVerified && parameterRotateEntry == null;
							this.analyzeReferenceDataClass(autoReflectionClass, useDoubleArrayStorage, useGoto, parameterRotateEntry, typeResolver, workspace, scriptContext);
						}

						if (autoReflectionClass.getConstantPoolManager().getConstantPoolSize() > 65535) {
							if (Options.analyzeReferenceDataClass) {
							}

							autoReflectionClass.validateConstantPoolSize(
								"Reference Obfuscation", "Consider limiting the number of references being obscured or using 'inReferencingClasses' setting. (B)"
							);
						}
					}

					Iterator<ClassNode> generatedDataClassIterator = generatedDataClasses.iterator();

					while (generatedDataClassIterator.hasNext()) {
						ClassNode remainingDataClass = generatedDataClassIterator.next();
						if (!autoReflectionClasses.contains(remainingDataClass)) {
							super.classes = workspace.removeClassFromWorkspace(remainingDataClass);
							generatedDataClassIterator.remove();
						}
					}

					if (!referenceObfuscationDataClassNode.isNull() && !autoReflectionClasses.contains(referenceObfuscationDataClassNode.get())) {
						referenceObfuscationDataClassNode.setNull();
					}

					Iterator<Entry<ArchiveNode, ClassNode>> archiveEntryIterator = archiveToDataClassNodeMap.entrySet().iterator();

					while (archiveEntryIterator.hasNext()) {
						Entry<ArchiveNode, ClassNode> archiveClassEntry = archiveEntryIterator.next();
						if (!autoReflectionClasses.contains(archiveClassEntry.getValue())) {
							archiveEntryIterator.remove();
						}
					}

					if (scriptContext.isVerbose()) {
						PrintWriter writer = scriptContext.getWriter();
						String logMessage = InjectedClassManager.logCreatedClassesFromMap("Reference Obfuscation", generatedDataClasses, archiveToDataClassNodeMap);
						writer.print(logMessage);
					}
				}
			} else {
				for (ClassNode unreferencedDataClass : generatedDataClasses) {
					super.classes = workspace.removeClassFromWorkspace(unreferencedDataClass);
				}

				referenceObfuscationDataClassNode.setNull();
				archiveToDataClassNodeMap.clear();
				autoReflectionClassGraph.clear();
				scriptContext.logMessage("Reference Obfuscation : No matching references found so no processing required or done.");
			}
		}
	}

	public final void runFieldNameObfuscation(
		DefaultExcludes excludes,
		ChangeLogContext changeLogContext,
		int keepInnerClassInfo,
		boolean useAsciiCharactersOnly,
		boolean legalIdentifiers,
		boolean randomizeObfuscation,
		String newNamesPrefix,
		boolean thideFieldNames,
		HashMap<String, String> newToOldClassNameMap,
		DualKeyMap<String, FieldSignature, FieldSignature> oldToNewFieldMappings,
		DualKeyMap<String, FieldSignature, FieldSignature> newToOldFieldMappings,
		AbstractClassNode[] changeLogClasses,
		List<String> newFieldNameList,
		ScriptContext scriptContext
	) {
		FieldRenamer fieldRenamer = new FieldRenamer(excludes, changeLogContext, super.workspace, super.classes, changeLogClasses, super.classTreeManager, newToOldClassNameMap, oldToNewFieldMappings, newToOldFieldMappings, randomizeObfuscation);
		SimpleFieldNameGenerator fieldNameGenerator = new SimpleFieldNameGenerator(keepInnerClassInfo, useAsciiCharactersOnly, legalIdentifiers, randomizeObfuscation, newNamesPrefix, thideFieldNames, newFieldNameList);
		fieldRenamer.renameFields(fieldNameGenerator, scriptContext);
	}

	private boolean isIndyReferenceAvailable(
		ObfuscateReferenceExcludes obfuscateReferenceExcludes, AtomicBool enableMPC, ParameterChangerExcludes parameterChangerExcludes, AtomicInt majorVersion, ScriptContext scriptContext
	) {
		return !Options.dontUseIndyReferences
			&& Options.ZKM_OBFUSCATE_REFERENCES_INDY
			&& obfuscateReferenceExcludes != null
			&& !scriptContext.hasMicroEditionClasses()
			&& enableMPC.get()
			&& parameterChangerExcludes != null
			&& (EnvironmentUtils.isJava8OrNewer(majorVersion.get()) || EnvironmentUtils.isJava7OrNewer(majorVersion.get()) && !Options.dontUseIndyOnJava7);
	}

	private void runStringEncryptionPass(
		StringEncryptionExcludes encryptionTargetManager,
		NestedMultiMap<ClassNode, MethodNode, StringNode, PriorityEntry<WritableType>> stringConstantEntries,
		KeyValuePairMultiMap<ClassNode, StringNode, FieldNode> staticStringFields,
		MultiMap<ClassNode, FieldNode> instanceFields,
		StringEncryptionBuilder stringEncryptionBuilder,
		boolean aggressiveEncryption,
		boolean useDoubleArrayStorage,
		Set<StringNode> processedStrings,
		Inheritance microEditionClasses,
		HashMap<ClassNode, Boolean> inheritance,
		ParameterChangerBuilder parameterChangerBuilder,
		Map<MethodNode, Long> encryptionKeysByMethod,
		Map<MethodNode, LocalVariableOperand> operandsByMethod,
		TypeResolver typeResolver,
		boolean injectXorMask,
		ScriptContext scriptContext
	) {
		boolean useGoto = Options.useGOTO;
		ArrayList<ClassNode> encryptedClasses;
		if (scriptContext.isVerbose()) {
			encryptedClasses = new ArrayList<>(stringConstantEntries.size());
		} else {
			encryptedClasses = null;
		}

		HashSet<AbstractClassNode> processedClasses = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(super.workspace.getClassesCount()));

		for (ClassHierarchyNode rootInterface : super.classTreeManager.getRootInterfaces()) {
			if (!rootInterface.getClassNodeIfWorkspace().isGeneratedClass()) {
				rootInterface.encryptStrings(
					encryptionTargetManager, staticStringFields, instanceFields, stringConstantEntries, stringEncryptionBuilder, useGoto, aggressiveEncryption, useDoubleArrayStorage, injectXorMask, processedStrings, inheritance, microEditionClasses, processedClasses, parameterChangerBuilder, encryptionKeysByMethod, operandsByMethod, super.workspace, encryptedClasses, super.classProvider, typeResolver, scriptContext, true
				);
			}
		}

		for (ClassHierarchyNode workspaceClassRoot : super.classTreeManager.getWorkspaceClassRoots()) {
			if (!workspaceClassRoot.isInterface() && !workspaceClassRoot.getClassNodeIfWorkspace().isGeneratedClass()) {
				workspaceClassRoot.encryptStrings(
					encryptionTargetManager,
					staticStringFields,
					instanceFields,
					stringConstantEntries,
					stringEncryptionBuilder,
					useGoto,
					aggressiveEncryption,
					useDoubleArrayStorage,
					injectXorMask,
					processedStrings,
					inheritance,
					microEditionClasses,
					processedClasses,
					parameterChangerBuilder,
					encryptionKeysByMethod,
					operandsByMethod,
					super.workspace,
					encryptedClasses,
					super.classProvider,
					typeResolver,
					scriptContext,
					false
				);
			}
		}

		if (scriptContext.isVerbose() && encryptedClasses != null) {
			PrintWriter writer = scriptContext.getWriter();
			Obfuscator.StringEncryptionComparator comparator = new Obfuscator.StringEncryptionComparator(this);
			Collections.sort(encryptedClasses, comparator);

			for (ClassNode encryptedClass : encryptedClasses) {
				writer.println("\tString Encryption performed in class '" + encryptedClass.getSourcePath() + "'");
			}
		}
	}

	private void updatePreverification(HashMap<ClassNode, Boolean> microEditionClasses, TypeResolver typeResolver, ScriptContext scriptContext) {
		for (ClassNode microEditionClass : microEditionClasses.keySet()) {
			microEditionClass.updatePreverification(scriptContext, typeResolver, super.workspace);
			if (microEditionClass.hasInnerClasses()) {
				Iterator<AbstractClassNode> innerClassIterator = microEditionClass.getInnerClasses().iterator();

				while (innerClassIterator.hasNext()) {
					((ClassNode)innerClassIterator.next()).updatePreverification(scriptContext, typeResolver, super.workspace);
				}
			}
		}
	}

	private void mergeReferenceSites(
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> memberObfuscationRefs,
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> referenceSites,
		ObfuscateReferenceExcludes obfuscateReferenceExcludes,
		String reason
	) {
		for (Entry<MemberContext, MultiMap<InstructionListNode, WideConstantPoolInsnNode>> memberEntry : memberObfuscationRefs.entrySet()) {
			MemberContext memberContext = memberEntry.getKey();
			MemberNode memberNode = memberContext.getMemberNode();
			if (memberNode.isField()) {
				obfuscateReferenceExcludes.includeFieldAnyReference((AbstractFieldNode)memberNode, reason);
			} else {
				obfuscateReferenceExcludes.includeMethodAnyReference((AbstractMethodNode)memberNode, reason);
			}

			for (Entry<InstructionListNode, List<WideConstantPoolInsnNode>> instructionEntry : memberEntry.getValue().entrySet()) {
				InstructionListNode instructionNode = instructionEntry.getKey();

				for (WideConstantPoolInsnNode constantPoolInsn : instructionEntry.getValue()) {
					referenceSites.put(memberContext, instructionNode, constantPoolInsn);
				}
			}
		}
	}

	private ParameterChangerExcludes buildParameterExcludes(
		boolean isParameterObfuscationMode,
		AtomicReference<Set<MethodNode>> referencedMethods,
		AtomicReference<MultiMap<MethodNode, MethodNode>> methodReferenceGraph,
		int keepInnerClassInfo,
		List<? extends ExclusionInclusionStatement> includeStatements,
		List<? extends ExclusionInclusionStatement> excludeStatements,
		DefaultExcludes excludes,
		AutoReflectionExcludes autoReflectionExcludes,
		FixedClassManager fixedClassManager,
		ScriptContext scriptContext
	) throws IOException {
		Set<MethodNode> referencedMethodSet = GlobalUtils.newHashSet();
		MultiMap<MethodNode, MethodNode> methodReferenceMap;
		if (methodReferenceGraph.isNull()) {
			ReferenceGraph<MethodNode, MethodNode> collectedReferenceGraph = new ReferenceGraph<>();

			for (ClassNode classNode : super.classes) {
				classNode.collectMethodReferenceGraph(collectedReferenceGraph, referencedMethodSet);
			}

			methodReferenceMap = collectedReferenceGraph.toMultiMap();
		} else {
			methodReferenceMap = methodReferenceGraph.get();
			referencedMethodSet = referencedMethods.get();
		}

		ParameterChangerExcludes parameterChangerExcludes = new ParameterChangerExcludes(
			isParameterObfuscationMode,
			super.workspace,
			keepInnerClassInfo,
			includeStatements,
			excludeStatements,
			scriptContext.getTrimExcludedMemberSet(),
			super.workspace.getMethodsReferencingStrings(),
			super.workspace.getEntryPointSignatures(),
			super.workspace.getProcessedPackageResources(),
			super.workspace.getProcessingErrors(),
			methodReferenceMap,
			excludes,
			autoReflectionExcludes,
			fixedClassManager,
			scriptContext
		);
		referencedMethods.set(referencedMethodSet);
		methodReferenceGraph.set(methodReferenceMap);
		return parameterChangerExcludes;
	}

	private void analyzeHierarchyReferences(
		ClassNode classNode,
		boolean useDoubleArrayStorage,
		boolean useGoto,
		boolean injectXorMask,
		int maxEncryptedStrings,
		boolean encryptAllStrings,
		boolean unused,
		ParameterRotateEntry parameterRotateEntry,
		TypeResolver typeResolver,
		ClassHierarchy classHierarchy,
		ScriptContext scriptContext,
		int keyLocalSlot
	) {
		String analysisMessage = "analyzing fields and methods in the inheritance hierarchy of class '" + classNode.getBinaryName() + "' (" + classNode.getClassType() + ")";
		HashSet<StringNode> processedStrings = GlobalUtils.newHashSet();
		StringEncryptionBuilder stringEncryptionBuilder = new StringEncryptionBuilder(false, useDoubleArrayStorage);
		ClassNode[] inheritanceClasses = new ClassNode[]{classNode};
		Inheritance inheritance = new Inheritance(inheritanceClasses, super.classProvider);
		NestedMultiMap<ClassNode, MethodNode, StringNode, PriorityEntry<WritableType>> stringConstantEntries = new NestedMultiMap<>(super.classes.length);
		KeyValuePairMultiMap<ClassNode, StringNode, FieldNode> staticStringFields = new KeyValuePairMultiMap<>(super.classes.length);
		MultiMap<ClassNode, FieldNode> instanceFields = new MultiMap<>(super.classes.length);
		StringEncryptionExcludes encryptionTargetManager = new StringEncryptionExcludes(super.workspace, new ArrayList<>(), new ArrayList<>(), scriptContext);
		classNode.collectStringTargets(stringConstantEntries, staticStringFields, instanceFields, null, encryptAllStrings, typeResolver, classHierarchy, scriptContext);
		if (parameterRotateEntry != null) {
			ChangedMethod changedMethod = parameterRotateEntry.getChangedMethod();
			ConstantPoolManager constantPoolManager = classNode.getConstantPoolManager();
			ArrayList<TypeWriter> constantPoolEntries = new ArrayList<>();
			Iterator<Entry<ClassNode, KeyValuePairMultiMap<MethodNode, StringNode, PriorityEntry<WritableType>>>> classEntryIterator = stringConstantEntries.entrySet().iterator();

			while (classEntryIterator.hasNext()) {
				Iterator<Entry<MethodNode, List<KeyValuePair<StringNode, PriorityEntry<WritableType>>>>> methodEntryIterator = classEntryIterator.next().getValue().entrySet().iterator();

				while (methodEntryIterator.hasNext()) {
					MethodNode methodNode = methodEntryIterator.next().getKey();
					if (!parameterRotateEntry.hasEncryptionKey(methodNode)) {
						AtomicReference<LocalVariableSlot> encryptionKeySlot = new AtomicReference<>();
						Long methodEncryptionKey = parameterRotateEntry.createEncryptionKey(methodNode);
						methodNode.injectLongXorDecryption(changedMethod, methodEncryptionKey, encryptionKeySlot, constantPoolEntries, constantPoolManager, analysisMessage, keyLocalSlot);
						parameterRotateEntry.setOperand(methodNode, encryptionKeySlot.get());
					}
				}
			}

			if (!constantPoolEntries.isEmpty()) {
				classNode.appendEntriesToConstantPool(constantPoolEntries);
			}
		}

		Workspace superWorkspace = super.workspace;
		ClassProvider superClassProvider = super.classProvider;
		List<KeyValuePair<StringNode, FieldNode>> stringFieldPairs = staticStringFields.get(classNode);
		List<FieldNode> classInstanceFields = instanceFields.get(classNode);
		KeyValuePairMultiMap<MethodNode, StringNode, PriorityEntry<WritableType>> constantEntries = stringConstantEntries.getMultiMap(classNode);
		ParameterChangerBuilder parameterChangerBuilder = parameterRotateEntry == null ? null : parameterRotateEntry.getParameterChangerBuilder();
		Map<MethodNode, LocalVariableOperand> operandsByMethod = parameterRotateEntry == null ? null : parameterRotateEntry.getOperandsByMethod();
		Map<MethodNode, Long> encryptionKeysByMethod = parameterRotateEntry == null ? null : parameterRotateEntry.getEncryptionKeysByMethod();
		classNode.encryptStringConstants(
			inheritance, stringEncryptionBuilder, true, useDoubleArrayStorage, useGoto, true, injectXorMask, superWorkspace, superClassProvider, stringFieldPairs, classInstanceFields, constantEntries, encryptionTargetManager, processedStrings, maxEncryptedStrings, parameterChangerBuilder, encryptionKeysByMethod, operandsByMethod, null, typeResolver, classHierarchy
		);
	}

	private void analyzeReferenceDataClass(
		ClassNode classNode, boolean useDoubleArrayStorage, boolean useGoto, ParameterRotateEntry parameterRotateEntry, TypeResolver typeResolver, ClassHierarchy classHierarchy, ScriptContext scriptContext
	) {
		boolean java6OrNewer = classNode.isJava6OrNewer();
		boolean encryptAllStrings = !Options.dontEncryptGeneratedMembers;
		this.analyzeHierarchyReferences(classNode, useDoubleArrayStorage, java6OrNewer, useGoto, Integer.MAX_VALUE, encryptAllStrings, true, parameterRotateEntry, typeResolver, classHierarchy, scriptContext, 4);
	}

	private ReferenceGraph<AbstractClassNode, FieldSignature> runControlFlowPass(
		AbstractClassNode[] changeLogClasses,
		int obfuscateControlFlow,
		ScriptContext scriptContext,
		FlowObfuscateExclude flowObfuscateExclude,
		ClassInitializationOrderBuilder initializationOrderBuilder,
		ControlFlowBuilder controlFlowBuilder,
		DefaultExcludes excludes,
		StaticBlockAnalyzer staticBlockAnalyzer,
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> memberObfuscationRefs,
		Inheritance inheritance,
		TypeResolver typeResolver,
		Random random,
		boolean randomizeObfuscation,
		boolean licenseVerified,
		boolean keepBalancedLocks
	) {
		HashSet<String> packageNames = GlobalUtils.newHashSet();

		for (int packageNameIndex = 0; packageNameIndex < super.classes.length; packageNameIndex++) {
			packageNames.add(super.classes[packageNameIndex].getPackageName());
		}

		int packageNameCount = packageNames.size();
		ReferenceGraph<AbstractClassNode, FieldSignature> groupingConditionGraph = controlFlowBuilder.buildGroupingConditions(packageNames, excludes, super.classTreeManager, inheritance, super.workspace, flowObfuscateExclude, initializationOrderBuilder);
		if (controlFlowBuilder.getConditionCount() == 0) {
			scriptContext.logWarning("Couldn't obfuscate control flow. All candidate 'intersection' classes may be interfaces, non-public or excluded.");
			return null;
		} else {
			if (controlFlowBuilder.getConditionCount() < controlFlowBuilder.getPackageGroupingCount()) {
				List<String> unobfuscatedClassNames = controlFlowBuilder.getUnobfuscatedClasses();
				StringBuffer unobfuscatedClassList = new StringBuffer();
				Iterator<String> unobfuscatedClassIterator = unobfuscatedClassNames.iterator();

				while (unobfuscatedClassIterator.hasNext()) {
					unobfuscatedClassList.append("'" + unobfuscatedClassIterator.next() + "'");
					if (unobfuscatedClassIterator.hasNext()) {
						unobfuscatedClassList.append(", ");
					}
				}

				scriptContext.logWarning(
					"Couldn't obfuscate control flow in some or all of the classes in the groupings "
						+ unobfuscatedClassList
						+ ". All candidate 'intersection' classes in these groupings may be interfaces, non-public or excluded."
				);
			}

			int classCount = super.classes.length;
			MultiMap<InstructionListNode, InstructionBlock> instructionBlocks = new MultiMap<>(classCount, Options.ZKM_USE_PARALLEL);
			ArrayList<ClassNode> targetClasses = new ArrayList<>(super.classes.length + 5);

			for (int targetClassIndex = 0; targetClassIndex < super.classes.length; targetClassIndex++) {
				ClassNode classNode = super.classes[targetClassIndex];
				targetClasses.add(classNode);
				if (classNode.hasInnerClasses()) {
					for (AbstractClassNode innerClass : classNode.getInnerClasses()) {
						targetClasses.add((ClassNode)innerClass);
					}
				}
			}

			Map<InstructionListNode, MethodAnalyzer> methodAnalyzerCache = Options.ZKM_USE_PARALLEL
				? new ConcurrentHashMap<>()
				: GlobalUtils.newHashMap();
			if (Options.ZKM_USE_PARALLEL && Options.threadCount >= 2) {
				Vector<ProcessingException> analysisExceptions = new Vector<>();
				targetClasses.parallelStream().forEach(classNode -> {
					try {
						PackageCondition packageCondition = controlFlowBuilder.getPackageCondition(classNode);
						if (packageCondition != null && packageCondition.getGroupCondition() != null) {
							Thread.yield();
							classNode.analyzeMethodsForFlow(instructionBlocks, typeResolver, super.workspace, methodAnalyzerCache, 1, obfuscateControlFlow, licenseVerified, keepBalancedLocks, controlFlowBuilder, scriptContext, flowObfuscateExclude, staticBlockAnalyzer, random);
						}
					} catch (ProcessingException processingException) {
						analysisExceptions.add(processingException);
					}
				});
				if (!analysisExceptions.isEmpty()) {
					throw (ProcessingException)analysisExceptions.get(0);
				}
			} else {
				for (ClassNode targetClass : targetClasses) {
					PackageCondition analysisPackageCondition = controlFlowBuilder.getPackageCondition(targetClass);
					if (analysisPackageCondition != null && analysisPackageCondition.getGroupCondition() != null) {
						Thread.yield();
						targetClass.analyzeMethodsForFlow(instructionBlocks, typeResolver, super.workspace, methodAnalyzerCache, 1, obfuscateControlFlow, licenseVerified, keepBalancedLocks, controlFlowBuilder, scriptContext, flowObfuscateExclude, staticBlockAnalyzer, random);
					}
				}
			}

			MultiMap<ClassNode, PriorityInstructionEntry> flowCandidatesByClass = new MultiMap<>(classCount, 5);

			for (int candidateIndex = 0; candidateIndex < classCount; candidateIndex++) {
				super.classes[candidateIndex].collectFlowMethodCandidates(flowCandidatesByClass);
			}

			HashSet<InstructionListNode> instructionNodes = GlobalUtils.newHashSet(packageNameCount);
			HashSet<InstructionListNode> excludedNodes = GlobalUtils.newHashSet(packageNameCount);
			if (super.classes.length > 1) {
				controlFlowBuilder.selectFlowObfuscationTargets(instructionNodes, excludedNodes, flowCandidatesByClass, flowObfuscateExclude, staticBlockAnalyzer);
			}

			if (Options.ZKM_USE_PARALLEL && Options.threadCount >= 2) {
				Vector<ProcessingException> applyExceptions = new Vector<>();
				targetClasses.parallelStream().forEach(classNode -> {
					try {
						PackageCondition packageCondition = controlFlowBuilder.getPackageCondition(classNode);
						if (packageCondition != null) {
							Condition groupCondition = packageCondition.getGroupCondition();
							if (groupCondition != null) {
								Condition packageLevelCondition = packageCondition.getPackageCondition();
								classNode.applyControlFlowObfuscation(instructionNodes, excludedNodes, groupCondition, packageLevelCondition, memberObfuscationRefs, instructionBlocks, methodAnalyzerCache, super.workspace, typeResolver, super.classProvider, random);
							}
						}
					} catch (ProcessingException processingException) {
						applyExceptions.add(processingException);
					}
				});
				if (!applyExceptions.isEmpty()) {
					throw (ProcessingException)applyExceptions.get(0);
				}
			} else {
				for (ClassNode classToObfuscate : targetClasses) {
					PackageCondition packageCondition = controlFlowBuilder.getPackageCondition(classToObfuscate);
					if (packageCondition != null) {
						Condition groupCondition = packageCondition.getGroupCondition();
						if (groupCondition != null) {
							Condition packageLevelCondition = packageCondition.getPackageCondition();
							classToObfuscate.applyControlFlowObfuscation(instructionNodes, excludedNodes, groupCondition, packageLevelCondition, memberObfuscationRefs, instructionBlocks, methodAnalyzerCache, super.workspace, typeResolver, super.classProvider, random);
						}
					}
				}
			}

			instructionBlocks.clear();
			Iterator<Entry<InstructionListNode, MethodAnalyzer>> cacheEntryIterator = methodAnalyzerCache.entrySet().iterator();

			while (cacheEntryIterator.hasNext()) {
				cacheEntryIterator.next().getValue().release();
			}

			return groupingConditionGraph;
		}
	}

	private void recalculateStackMaps(TypeResolver typeResolver, FixedClassManager fixedClassManager) {
		if (Options.ZKM_USE_PARALLEL && Options.threadCount >= 2) {
			ArrayList<ClassNode> classesToRebuild = new ArrayList<>(super.classes.length);

			for (ClassNode classNode : super.classes) {
				if (classNode.isJava6OrNewer() && (fixedClassManager == null || !fixedClassManager.isInFixedClasses(classNode))) {
					classesToRebuild.add(classNode);
					if (classNode.hasInnerClasses()) {
						for (AbstractClassNode innerClass : classNode.getInnerClasses()) {
							classesToRebuild.add((ClassNode)innerClass);
						}
					}
				}
			}

			Vector<ProcessingException> processingExceptions = new Vector<>();
			classesToRebuild.parallelStream().forEach(classNode -> {
				try {
					classNode.rebuildStackMapTables(typeResolver, super.workspace);
				} catch (ProcessingException processingException) {
					processingExceptions.add(processingException);
				}
			});
			if (!processingExceptions.isEmpty()) {
				throw (ProcessingException)processingExceptions.get(0);
			}
		} else {
			for (ClassNode sequentialClassNode : super.classes) {
				if (sequentialClassNode.isJava6OrNewer() && (fixedClassManager == null || !fixedClassManager.isInFixedClasses(sequentialClassNode))) {
					sequentialClassNode.rebuildStackMapTables(typeResolver, super.workspace);
					if (sequentialClassNode.hasInnerClasses()) {
						Iterator<AbstractClassNode> innerClassIterator = sequentialClassNode.getInnerClasses().iterator();

						while (innerClassIterator.hasNext()) {
							((ClassNode)innerClassIterator.next()).rebuildStackMapTables(typeResolver, super.workspace);
						}
					}
				}
			}
		}
	}

	private void removeAllMethodParameters() {
		for (ClassNode classNode : super.classes) {
			classNode.removeMethodParameters();
			if (classNode.hasInnerClasses()) {
				Iterator<AbstractClassNode> innerClassIterator = classNode.getInnerClasses().iterator();

				while (innerClassIterator.hasNext()) {
					((ClassNode)innerClassIterator.next()).removeMethodParameters();
				}
			}
		}
	}

	private long computeLicenseNameHash(String embeddedLicenseHash, boolean addOffset) {
		return this.decodeScrambledLong(embeddedLicenseHash) + (addOffset ? 1272694208093L : 0L);
	}

	private void removeSelectiveLocalVars(boolean trimNonPublic, boolean trimRenamed) {
		for (ClassNode classNode : super.classes) {
			classNode.applyLocalVarTablePolicy(trimNonPublic, trimRenamed);
			if (classNode.hasInnerClasses()) {
				Iterator<AbstractClassNode> innerClassIterator = classNode.getInnerClasses().iterator();

				while (innerClassIterator.hasNext()) {
					((ClassNode)innerClassIterator.next()).applyLocalVarTablePolicy(trimNonPublic, trimRenamed);
				}
			}
		}
	}

	private void removeAllStackMaps() {
		for (ClassNode classNode : super.classes) {
			classNode.removeMethodStackMaps();
			if (classNode.hasInnerClasses()) {
				Iterator<AbstractClassNode> innerClassIterator = classNode.getInnerClasses().iterator();

				while (innerClassIterator.hasNext()) {
					((ClassNode)innerClassIterator.next()).removeMethodStackMaps();
				}
			}
		}
	}

	private void mergeLineMappings(HierarchicalMultiMap<String, Integer, Integer> lineNumberMappings, ChangeLogContext changeLogContext) {
		for (String newClassName : changeLogContext.getNewClassNames()) {
			if (!ClassHierarchyNode.isWorkspaceClassByInternalName(newClassName)) {
				String oldClassName = changeLogContext.getOldClassName(newClassName);
				if (oldClassName == null) {
					oldClassName = newClassName;
				}

				MultiMap<Integer, Integer> classLineNumbers = changeLogContext.getLineNumberMappings(oldClassName);
				if (classLineNumbers != null) {
					lineNumberMappings.put(newClassName, classLineNumbers);
				}
			}
		}
	}

	private void recalculateLocalVarTypes(TypeResolver typeResolver, ScriptContext scriptContext) {
		if (Options.ZKM_USE_PARALLEL && Options.threadCount >= 2) {
			ArrayList<ClassNode> classesToRecalculate = new ArrayList<>(super.classes.length);

			for (ClassNode classNode : super.classes) {
				classesToRecalculate.add(classNode);
				if (classNode.hasInnerClasses()) {
					for (AbstractClassNode innerClass : classNode.getInnerClasses()) {
						classesToRecalculate.add((ClassNode)innerClass);
					}
				}
			}

			Vector<ProcessingException> processingExceptions = new Vector<>();
			classesToRecalculate.parallelStream().forEach(classNode -> {
				try {
					classNode.recalculateLocalVarTypes(typeResolver, super.workspace, scriptContext);
				} catch (ProcessingException processingException) {
					processingExceptions.add(processingException);
				}
			});
			if (!processingExceptions.isEmpty()) {
				throw (ProcessingException)processingExceptions.get(0);
			}
		} else {
			for (ClassNode sequentialClassNode : super.classes) {
				sequentialClassNode.recalculateLocalVarTypes(typeResolver, super.workspace, scriptContext);
				if (sequentialClassNode.hasInnerClasses()) {
					Iterator<AbstractClassNode> innerClassIterator = sequentialClassNode.getInnerClasses().iterator();

					while (innerClassIterator.hasNext()) {
						((ClassNode)innerClassIterator.next()).recalculateLocalVarTypes(typeResolver, super.workspace, scriptContext);
					}
				}
			}
		}
	}

	private long extractLicenseHashFromLog(String logFilePath) {
		if (logFilePath != null) {
			File logFile = new File(logFilePath);
			if (logFile.exists()) {
				BufferedReader reader = null;
				CircularBuffer lineBuffer = new CircularBuffer(4);

				long licenseHash;
				try {
					reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8));

					String line;
					while ((line = reader.readLine()) != null) {
						lineBuffer.add(line);
						if (line.trim().equals(super.workspace.getCopyright())) {
							return GlobalUtils.longHashCodeAbs2(((String)lineBuffer.get(2)).trim());
						}
					}

					licenseHash = 0L;
				} catch (IOException ignoredReadFailure) {
					return 0L;
				} finally {
					if (reader != null) {
						try {
							reader.close();
						} catch (IOException ignoredCloseFailure) {
						}
					}
				}

				return licenseHash;
			}
		}

		return 0L;
	}

	private long getLogFileLastModified(String logFilePath) {
		long lastModified = 0L;
		if (logFilePath != null) {
			File logFile = new File(logFilePath);
			if (logFile.exists()) {
				lastModified = logFile.lastModified();
			}
		}

		return lastModified;
	}

	private boolean isLicenseTimestampValid(long logFileTimestamp, long licenseExpiryTimestamp, Workspace workspace) {
		if (logFileTimestamp > licenseExpiryTimestamp) {
			return false;
		}

		long earlyTolerance = this.decodeScrambledLong(xorWithRepeatingKey("h!s8^\r66?{", LicenseCheckConstant.getMethodNameXorKey()));
		long lateTolerance = this.decodeScrambledLong(xorWithRepeatingKey("\u0006/\u0004$\t\rS\u0012?", LicenseCheckConstant.getMethodNameXorKey()));
		if (logFileTimestamp < licenseExpiryTimestamp - earlyTolerance) {
			return false;
		}

		long latestAllowedTimestamp = logFileTimestamp + lateTolerance;
		List<Object> classpathEntries = ClasspathScanner.parseAndExpandClasspath(System.getProperty("java.class.path"), GlobalUtils.newHashSet());

		try {
			for (Object classpathEntry : classpathEntries) {
				if (classpathEntry instanceof File) {
					if (((File)classpathEntry).lastModified() > latestAllowedTimestamp) {
						return false;
					}
				} else if (classpathEntry instanceof ZipFileWrapper) {
					File archiveFile = new File(((ZipFileWrapper)classpathEntry).getName());
					if (archiveFile.exists()) {
						if (archiveFile.lastModified() > latestAllowedTimestamp) {
							return false;
						}

						if (archiveFile.getName().endsWith("ZKM.jar")) {
							ZipEntry workspaceClassEntry = ((ZipFileWrapper)classpathEntry).getEntry(workspace.getClass().getName().replace('.', '/') + ".class");
							if (workspaceClassEntry != null) {
								long workspaceClassTimestamp = workspaceClassEntry.getTime();
								if (workspaceClassTimestamp > latestAllowedTimestamp) {
									return false;
								}

								if (workspaceClassTimestamp != -1L && logFileTimestamp > workspaceClassTimestamp + earlyTolerance) {
									return false;
								}
							}
						}
					}
				}
			}
		} finally {
			for (Object classpathEntry : classpathEntries) {
				if (classpathEntry instanceof ZipFileWrapper) {
					try {
						((ZipFileWrapper)classpathEntry).close();
					} catch (IOException ignoredCloseFailure) {
					}
				}
			}
		}

		return true;
	}

	private boolean runAutoReflectionPass(
		MultiMap<MethodNode, MethodInvocation> trackedMethodInvocations,
		Inheritance inheritance,
		TypeResolver typeResolver,
		Workspace workspace,
		UnmodifiableMultiMap<ArchiveNode, ClassNode> namedModuleClasses,
		ReferenceGraph<ArchiveNode, ArchiveNode> moduleDependencyGraph,
		ReferenceGraph<ArchiveNode, String> modulePackageNameGraph,
		Map<ArchiveNode, ClassNode> archiveToDataClassNodeMap,
		IReferenceGraph<ClassNode, ClassNode> classReferenceGraph,
		AtomicReference<ClassNode> autoReflectHandlingDataClassNode,
		ChangeLogContext changeLogContext,
		String autoReflectionPackage,
		AutoReflectionExcludes autoReflectionExcludes,
		boolean hasReflectionStatements,
		boolean hasReflectionExcludes,
		String autoReflectionHash,
		boolean randomizeObfuscation,
		String newNamesPrefix,
		MixCaseType mixedCaseClassNames,
		boolean uniqueClassNames,
		int majorVersion,
		int minorVersion,
		boolean licenseVerified,
		ParameterRotateEntry parameterRotateEntry,
		Map<MethodNode, Long> encryptionKeysByMethod,
		Map<MethodNode, LocalVariableOperand> operandsByMethod,
		FixedClassManager fixedClassManager,
		List<String> overrideClassNames,
		ScriptContext scriptContext
	) {
		boolean anyHandlingApplied = false;
		if (trackedMethodInvocations.isEmpty()) {
			scriptContext.logMessage("Auto Reflection Handling : No remaining unresolved Reflection API calls found so no processing required or done.");
			return false;
		} else {
			boolean preJava14ClassFound = false;
			int classCount = super.classes.length;
			PrintWriter writer = scriptContext.getWriter();

			for (int i = 0; i < classCount; i++) {
				preJava14ClassFound = !super.classes[i].isJava1_4OrNewer() || preJava14ClassFound;
			}

			String hashAlgorithm = preJava14ClassFound ? "SHA-1" : "SHA-512";
			if (autoReflectionHash != null && autoReflectionHash.length() > 0) {
				if (super.enableVerbose) {
					writer.println("\tAuto Reflection Handling hash algorithm changed from '" + hashAlgorithm + "' to '" + autoReflectionHash + "' as specified.");
				}

				hashAlgorithm = autoReflectionHash;
			}

			HashMap<String, String> classMappings = GlobalUtils.newHashMap();
			DualKeyMap<String, FieldSignature, FieldSignature> fieldMappings = new DualKeyMap<>();
			DualKeyMap<String, MethodSignature, MethodSignature> methodMappings = new DualKeyMap<>();
			ReadOnlyMap<String, String> packageRenameMap = workspace.getPackageRenameMap();
			ReadOnlyMap<String, String> classNameToInternalNameMap = workspace.getClassNameToInternalNameMap();
			ReadOnlyMap<String, String> internalToBinaryNameMap = workspace.getInternalToBinaryNameMap();
			AutoReflectionHandlingBuilder autoReflectionBuilder = new AutoReflectionHandlingBuilder(autoReflectionExcludes, overrideClassNames, workspace, hashAlgorithm, randomizeObfuscation);
			ReadOnlyRegistryView<AbstractClassNode, FieldSignature, FieldSignature> fieldRegistryView = workspace.getFieldRegistryReadOnlyView();
			ReadOnlyRegistryView<AbstractClassNode, MethodSignature, MethodSignature> methodRegistryView = workspace.getMethodRegistryReadOnlyView();
			autoReflectionBuilder.collectReflectionMappings(trackedMethodInvocations, classNameToInternalNameMap, internalToBinaryNameMap, fieldRegistryView, methodRegistryView, classMappings, fieldMappings, methodMappings, hasReflectionStatements, hasReflectionExcludes, workspace, super.classProvider, scriptContext);
			if (classMappings.size() <= 0 && fieldMappings.getGroupCount() <= 0 && methodMappings.getGroupCount() <= 0) {
				scriptContext.logMessage("Auto Reflection Handling : No matching renamed classes, fields or methods so no processing required or done.");
			} else {
				HashMap<ArchiveNode, String> archiveToDataClassNameMap = GlobalUtils.newHashMap();
				HashMap<ClassNode, String> classToDataClassNameMap = GlobalUtils.newHashMap();
				HashSet<ClassNode> generatedDataClasses = GlobalUtils.newHashSet();
				AtomicReference<String> autoReflectHandlingDataClassName = new AtomicReference<>();
				Set<String> dataClassNames = autoReflectionBuilder.planAuxiliaryClassName(
					GlobalUtils.toSet(super.classes),
					autoReflectHandlingDataClassName,
					archiveToDataClassNameMap,
					classToDataClassNameMap,
					changeLogContext,
					autoReflectionPackage,
					moduleDependencyGraph,
					modulePackageNameGraph,
					namedModuleClasses,
					packageRenameMap,
					classNameToInternalNameMap,
					internalToBinaryNameMap,
					newNamesPrefix,
					mixedCaseClassNames,
					uniqueClassNames,
					workspace,
					super.classRepository,
					scriptContext
				);
				HashMap<ClassNode, ClassNode> classToDataClassNodeMap = GlobalUtils.newHashMap();
				super.classes = InjectedClassManager.generateAndRegisterClasses(
					dataClassNames,
					classToDataClassNameMap,
					autoReflectHandlingDataClassName,
					autoReflectHandlingDataClassNode,
					archiveToDataClassNodeMap,
					classToDataClassNodeMap,
					generatedDataClasses,
					archiveToDataClassNameMap,
					majorVersion,
					minorVersion,
					autoReflectionBuilder,
					workspace,
					null
				);
				classCount = super.classes.length;
				boolean hasRenamedNames = classMappings.size() > 0;
				boolean hasRenamedFields = fieldMappings.getGroupCount() > 0;
				boolean hasRenamedMethods = methodMappings.getGroupCount() > 0;
				ArrayList<ClassNode> handledClasses = new ArrayList<>();

				for (int classIndex = 0; classIndex < classCount; classIndex++) {
					ClassNode classNode = super.classes[classIndex];
					if ((fixedClassManager == null || !fixedClassManager.isInFixedClasses(classNode)) && !classNode.isGeneratedClass()) {
						ClassNode dataClassNode = classToDataClassNodeMap.get(classNode);
						boolean handlingApplied = classNode.applyAutoReflectionHandling(trackedMethodInvocations, dataClassNode, classReferenceGraph, hasRenamedNames, hasRenamedFields, hasRenamedMethods, typeResolver, workspace, super.enableVerbose, scriptContext);
						if (handlingApplied) {
							handledClasses.add(classNode);
						}

						anyHandlingApplied = anyHandlingApplied || handlingApplied;
						if (classNode.hasInnerClassInfo()) {
							for (AbstractClassNode innerClass : classNode.getInnerClasses()) {
								ClassNode innerDataClassNode = classToDataClassNodeMap.get((ClassNode)innerClass);
								handlingApplied = ((ClassNode)innerClass).applyAutoReflectionHandling(trackedMethodInvocations, innerDataClassNode, classReferenceGraph, hasRenamedNames, hasRenamedFields, hasRenamedMethods, typeResolver, workspace, super.enableVerbose, scriptContext);
								if (handlingApplied) {
									handledClasses.add((ClassNode)innerClass);
								}

								anyHandlingApplied = anyHandlingApplied || handlingApplied;
							}
						}
					}
				}

				if (anyHandlingApplied) {
					HashSet<ClassNode> graphMembers = GlobalUtils.newHashSet(classReferenceGraph.getMembers());
					MultiMap<ClassNode, ArchiveNode> archivesByClass = GlobalUtils.invertMultiMap(namedModuleClasses);
					HashSet<ArchiveNode> retainedArchives = GlobalUtils.newHashSet();

					for (ClassNode handledClass : handledClasses) {
						if (archivesByClass.containsKey(handledClass)) {
							for (ArchiveNode containingArchive : archivesByClass.get(handledClass)) {
								retainedArchives.add(containingArchive);
							}
						}
					}

					Iterator<ClassNode> generatedDataClassIterator = generatedDataClasses.iterator();

					while (generatedDataClassIterator.hasNext()) {
						ClassNode generatedDataClass = generatedDataClassIterator.next();
						if (!graphMembers.contains(generatedDataClass)) {
							super.classes = workspace.removeClassFromWorkspace(generatedDataClass);
							generatedDataClassIterator.remove();
						}
					}

					Iterator<Entry<ArchiveNode, ClassNode>> dataClassEntryIterator = archiveToDataClassNodeMap.entrySet().iterator();

					while (dataClassEntryIterator.hasNext()) {
						ArchiveNode archiveNode = dataClassEntryIterator.next().getKey();
						if (!retainedArchives.contains(archiveNode)) {
							dataClassEntryIterator.remove();
						}
					}

					if (!autoReflectHandlingDataClassNode.isNull() && !graphMembers.contains(autoReflectHandlingDataClassNode.get())) {
						autoReflectHandlingDataClassNode.setNull();
					}

					for (ClassNode graphMember : classReferenceGraph.getMembers()) {
						Iterator<ClassNode> referenceIterator = classReferenceGraph.getReferences(graphMember).iterator();

						while (referenceIterator.hasNext()) {
							if (!referenceIterator.next().isFromFilesystem() && graphMember.isFromFilesystem()) {
								graphMember.markLoadedFromFilesystem();
							}
						}
					}

					if (scriptContext.isVerbose()) {
						String createdClassesLog = InjectedClassManager.logCreatedClassesFromMap("Auto Reflection Handling", generatedDataClasses, archiveToDataClassNodeMap);
						writer.print(createdClassesLog);
						if (AutoReflectionHandlingBuilder.minNameLength != 11) {
							writer.println("\tAuto Reflection Handling : Minimum reasonable AutoReflection object name length changed from 11 to " + AutoReflectionHandlingBuilder.minNameLength);
						}

						autoReflectionBuilder.logIncludedMappings(internalToBinaryNameMap, classMappings, fieldMappings, methodMappings, scriptContext.getWriter());
					}

					try {
						for (ClassNode dataClass : generatedDataClasses) {
							if (parameterRotateEntry != null) {
								ArchiveNode owningArchive = null;

								for (Entry<ArchiveNode, ClassNode> dataClassEntry : archiveToDataClassNodeMap.entrySet()) {
									if (dataClassEntry.getValue() == dataClass) {
										owningArchive = dataClassEntry.getKey();
										break;
									}
								}

								parameterRotateEntry.setCurrentArchiveNode(owningArchive);
							}

							int mappingCount = autoReflectionBuilder.buildLookupMappings(classMappings, fieldMappings, methodMappings, internalToBinaryNameMap.keySet(), dataClass, hashAlgorithm, parameterRotateEntry, workspace, super.classProvider, scriptContext);
							dataClass.updateIntegerConstantValue(Integer.MAX_VALUE, GlobalUtils.getInitialCapacity(mappingCount));
						}
					} catch (UnsupportedEncodingException unsupportedEncodingException) {
						throw new ProcessingException("Auto Reflection Handling : UnsupportedEncodingException : " + unsupportedEncodingException.getMessage());
					}

					if (Options.analyzeAutoReflectionClass) {
						for (ClassNode analyzedDataClass : generatedDataClasses) {
							this.analyzeAutoReflectionClass(analyzedDataClass, true, licenseVerified && parameterRotateEntry == null, parameterRotateEntry, typeResolver, workspace, scriptContext);
						}
					}

					for (ClassNode validatedClass : generatedDataClasses) {
						if (validatedClass.getConstantPoolManager().getConstantPoolSize() > 65535) {
							validatedClass.validateConstantPoolSize(
								"Auto Reflection Handling",
								"Consider using ZKM Script 'accessedByReflection' statement to specify only those classes, fields and methods which are actually accessed by Reflection and for which the Reflection access has not been resolved by " + CopyrightConstants.NAME + "."
							);
						}
					}
				} else {
					for (ClassNode removedDataClass : generatedDataClasses) {
						super.classes = workspace.removeClassFromWorkspace(removedDataClass);
					}

					autoReflectHandlingDataClassNode.setNull();
					archiveToDataClassNodeMap.clear();
					classReferenceGraph.clear();
					scriptContext.logMessage(
						"Auto Reflection Handling : No remaining unresolved Reflection API calls could be processed with the matching renamed classes, fields or methods. No processing done."
					);
				}
			}

			return anyHandlingApplied;
		}
	}

	private void obfuscateExceptionsInClass(
		ClassNode classNode,
		HashMap<PackageGroupingManager, List<String>> exceptionTypesByGrouping,
		Map<AbstractClassNode, PackageGroupingManager> groupingsByClass,
		FlowObfuscateExclude flowObfuscateExclude,
		ObfuscateExceptionExclude obfuscateExceptionExclude,
		Inheritance inheritance,
		Set<MethodNode> methods,
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> memberObfuscationRefs,
		Map<MethodNode, LocalVariableOperand> decryptionOperandsByMethod,
		TypeResolver typeResolver,
		Workspace workspace,
		ScriptContext scriptContext,
		boolean exceptionObfuscation,
		boolean avoidMonitors,
		ClassMetrics classMetrics,
		PhantomClassManager phantomClassManager
	) {
		PackageGroupingManager packageGrouping = groupingsByClass.get(classNode);
		if (packageGrouping == null && classNode.isInnerClass()) {
			packageGrouping = groupingsByClass.get(classNode.getOuterClass());
		}

		List<String> exceptionTypes = exceptionTypesByGrouping.get(packageGrouping);
		classNode.applyExceptionObfuscation(methods, memberObfuscationRefs, decryptionOperandsByMethod, flowObfuscateExclude, obfuscateExceptionExclude, typeResolver, workspace, scriptContext, exceptionTypes, exceptionObfuscation, avoidMonitors, phantomClassManager, classMetrics, inheritance, workspace);
	}

	private void removeSelectiveParameters(boolean trimNonPublic, boolean trimRenamed) {
		for (ClassNode classNode : super.classes) {
			classNode.pruneMethodParameters(trimNonPublic, trimRenamed);
			if (classNode.hasInnerClasses()) {
				Iterator<AbstractClassNode> innerClassIterator = classNode.getInnerClasses().iterator();

				while (innerClassIterator.hasNext()) {
					((ClassNode)innerClassIterator.next()).pruneMethodParameters(trimNonPublic, trimRenamed);
				}
			}
		}
	}

	private void encryptLongConstants(
		LongEncryptionExcludes encryptionExcludes,
		NestedMultiMap<ClassNode, MethodNode, LongNode, PriorityEntry<WideConstantPoolInsnNode>> longConstantEntries,
		KeyValuePairMultiMap<ClassNode, LongNode, FieldNode> staticLongFieldPairs,
		KeyValuePairMultiMap<ClassNode, LongNode, FieldNode> instanceLongFieldPairs,
		LongEncryptionBuilder longEncryptionBuilder,
		Inheritance inheritance,
		HashMap<ClassNode, Boolean> microEditionClasses,
		ParameterChangerBuilder parameterChangerBuilder,
		Map<MethodNode, Long> encryptionKeysByMethod,
		Map<MethodNode, LocalVariableOperand> operandsByMethod,
		TypeResolver typeResolver,
		boolean unused,
		ScriptContext scriptContext
	) {
		super.workspace.markLongConstantsEncrypted();
		ArrayList<ClassNode> verboseEncryptionClasses;
		if (scriptContext.isVerbose()) {
			verboseEncryptionClasses = new ArrayList<>(longConstantEntries.size());
		} else {
			verboseEncryptionClasses = null;
		}

		HashSet<AbstractClassNode> encryptedClasses = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(super.workspace.getClassesCount()));

		for (ClassHierarchyNode rootInterface : super.classTreeManager.getRootInterfaces()) {
			if (!rootInterface.getClassNodeIfWorkspace().isGeneratedClass()) {
				rootInterface.encryptLongConstants(encryptionExcludes, longConstantEntries, staticLongFieldPairs, instanceLongFieldPairs, longEncryptionBuilder, inheritance, encryptedClasses, microEditionClasses, parameterChangerBuilder, encryptionKeysByMethod, operandsByMethod, super.workspace, verboseEncryptionClasses, super.classProvider, typeResolver, unused, scriptContext, true);
			}
		}

		for (ClassHierarchyNode workspaceClassRoot : super.classTreeManager.getWorkspaceClassRoots()) {
			if (!workspaceClassRoot.isInterface() && !workspaceClassRoot.getClassNodeIfWorkspace().isGeneratedClass()) {
				workspaceClassRoot.encryptLongConstants(encryptionExcludes, longConstantEntries, staticLongFieldPairs, instanceLongFieldPairs, longEncryptionBuilder, inheritance, encryptedClasses, microEditionClasses, parameterChangerBuilder, encryptionKeysByMethod, operandsByMethod, super.workspace, verboseEncryptionClasses, super.classProvider, typeResolver, unused, scriptContext, false);
			}
		}

		if (scriptContext.isVerbose() && verboseEncryptionClasses != null) {
			PrintWriter writer = scriptContext.getWriter();
			Obfuscator.LongEncryptionComparator longEncryptionComparator = new Obfuscator.LongEncryptionComparator(this);
			Collections.sort(verboseEncryptionClasses, longEncryptionComparator);

			for (ClassNode encryptedClass : verboseEncryptionClasses) {
				writer.println("\tLong Constant Encryption performed in class '" + encryptedClass.getSourcePath() + "'");
			}
		}
	}

	public static class StringEncryptionComparator implements Comparator<ClassNode> {
		public final Obfuscator obfuscator;

		public StringEncryptionComparator(Obfuscator obfuscator) {
			this.obfuscator = obfuscator;
		}

		public int compare(ClassNode o1, ClassNode o2) {
			return this.compareBySourcePath(o1, o2);
		}

		public int compareBySourcePath(ClassNode classNode1, ClassNode classNode2) {
			return classNode1.getSourcePath().compareTo(classNode2.getSourcePath());
		}
	}

	public static class LongEncryptionComparator implements Comparator<ClassNode> {
		public final Obfuscator obfuscator;

		public LongEncryptionComparator(Obfuscator obfuscator) {
			this.obfuscator = obfuscator;
		}

		public int compare(ClassNode o1, ClassNode o2) {
			return this.compareBySourcePath(o1, o2);
		}

		public int compareBySourcePath(ClassNode classNode1, ClassNode classNode2) {
			return classNode1.getSourcePath().compareTo(classNode2.getSourcePath());
		}
	}

	public static class IntegerEncryptionComparator implements Comparator<ClassNode> {
		public final Obfuscator obfuscator;

		public IntegerEncryptionComparator(Obfuscator obfuscator) {
			this.obfuscator = obfuscator;
		}

		public int compareBySourcePath(ClassNode classNode1, ClassNode classNode2) {
			return classNode1.getSourcePath().compareTo(classNode2.getSourcePath());
		}

		public int compare(ClassNode o1, ClassNode o2) {
			return this.compareBySourcePath(o1, o2);
		}
	}
}
