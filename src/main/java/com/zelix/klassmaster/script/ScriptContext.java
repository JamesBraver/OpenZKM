package com.zelix.klassmaster.script;

import com.zelix.exceptions.LookupException;
import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.reference.LinkedReferenceGraph;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.klassmaster.script.log.AbstractScriptLogger;
import com.zelix.klassmaster.script.statements.exclusion.*;
import com.zelix.repository.ClassRepository;
import com.zelix.repository.ClasspathManager;
import com.zelix.tools.excludes.FixedClassManager;
import com.zelix.tools.trim.TrimExcludedMemberSet;
import com.zelix.utils.FileUtils;
import com.zelix.utils.io.DirectoryScanner;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.PhantomClassManager;
import com.zelix.workspace.Workspace;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class ScriptContext extends AbstractScriptLogger {
	private List<ExcludeStatement> excludeStatements = new ArrayList<>();
	private List<IgnoreMissingReferencesStatement> ignoreMissingReferencesStatements = new ArrayList<>();
	private List<TrimExcludeStatement> trimExcludeStatements = new ArrayList<>();
	private List<UnexcludeStatement> unexcludeStatements = new ArrayList<>();
	private List<TrimUnexcludeStatement> trimUnexcludeStatements = new ArrayList<>();
	private List<ObfuscateReferencesIncludeStatement> obfuscateReferencesIncludeStatements = new ArrayList<>();
	private List<ObfuscateReferencesExcludeStatement> obfuscateReferencesExcludeStatements = new ArrayList<>();
	private List<AccessedByReflectionStatement> accessedByReflectionStatements = new ArrayList<>();
	private List<AccessedByReflectionExcludeStatement> accessedByReflectionExcludeStatements = new ArrayList<>();
	private List<ObfuscateFlowExcludeStatement> obfuscateFlowExcludeStatements = new ArrayList<>();
	private List<ObfuscateFlowUnexcludeStatement> obfuscateFlowUnexcludeStatements = new ArrayList<>();
	private List<ObfuscateExceptionsExcludeStatement> obfuscateExceptionsExcludeStatements = new ArrayList<>();
	private List<ObfuscateExceptionsUnexcludeStatement> obfuscateExceptionsUnexcludeStatements = new ArrayList<>();
	private List<StringEncryptionExcludeStatement> encryptStringLiteralsExcludeStatements = new ArrayList<>();
	private List<StringEncryptionUnexcludeStatement> encryptStringLiteralsUnexcludeStatements = new ArrayList<>();
	private List<IntegerEncryptionExcludeStatement> integerEncryptionExcludeStatements = new ArrayList<>();
	private List<IntegerEncryptionUnexcludeStatement> integerEncryptionUnexcludeStatements = new ArrayList<>();
	private List<LongEncryptionExcludeStatement> longEncryptionExcludeStatements = new ArrayList<>();
	private List<LongEncryptionUnexcludeStatement> longEncryptionUnexcludeStatements = new ArrayList<>();
	private List<ExistingSerializedClassesStatement> existingSerializedClassesStatements = new ArrayList<>();
	private List<FixedClassesStatement> fixedClassesStatements = new ArrayList<>();
	private List<GroupingsStatement> groupingsStatements = new ArrayList<>();
	private List<RemoveMethodCallsIncludeStatement> removeMethodCallsIncludeStatements = new ArrayList<>();
	private List<RemoveMethodCallsExcludeStatement> removeMethodCallsExcludeStatements = new ArrayList<>();
	private List<MethodParameterChangesIncludeStatement> methodParameterChangesIncludeStatements = new ArrayList<>();
	private List<MethodParameterChangesExcludeStatement> methodParameterChangesExcludeStatements = new ArrayList<>();
	private List<MethodParameterObfuscationIncludeStatement> methodParameterObfuscationIncludeStatements = new ArrayList<>();
	private List<MethodParameterObfuscationExcludeStatement> methodParameterObfuscationExcludeStatements = new ArrayList<>();
	private LinkedReferenceGraph<String, String> referenceGraph = new LinkedReferenceGraph<>();
	private Workspace workspace;
	private ClasspathManager mainClasspathManager;
	private boolean pluginMode;
	private String logFilePath;
	private final boolean implementsClauseMatching;
	private boolean microEditionClassesFound;
	private FixedClassManager fixedClassManager;
	private String trimLogFilePath;
	private File defaultDirectory;
	private PhantomClassManager phantomClassManager;
	private String defaultMethodParamObfuscationExcludePath;
	private String defaultMethodParamChangesExcludePath;
	private TrimExcludedMemberSet trimExcludedMemberSet;
	private String defaultExcludePath;
	private boolean preverifyDisabled;
	private boolean classesLoaded;
	private String defaultTrimExcludePath;
	private List<ClasspathManager> versionedClasspathManagers;

	public ScriptContext(Workspace workspace, ClasspathManager classpathManager, boolean verbose, String logFilePath, PrintWriter logWriter) {
		super(verbose);
		this.workspace = workspace;
		this.mainClasspathManager = classpathManager;

		try {
			this.initDefaultPaths(
				"ZKM_TrimLog.txt",
				"defaultExclude.txt",
				"defaultTrimExclude.txt",
				"defaultMethodParameterChangesExclude.txt",
				"defaultMethodParameterObfuscationExclude.txt",
				SystemConstants.userDirectory
			);
		} catch (ProcessingException ignored) {
		}

		if (logFilePath == null) {
			this.logFilePath = "ZKM_log.txt";
		} else {
			this.logFilePath = logFilePath;
		}

		this.validateLogFilePaths(this.logFilePath, this.trimLogFilePath);
		this.logFilePath = new File(this.logFilePath).getAbsolutePath();
		super.writer = logWriter;
		this.implementsClauseMatching = false;
	}

	public ScriptContext(
		Workspace workspace,
		ClasspathManager classpathManager,
		boolean verbose,
		String logFilePath,
		String trimLogFilePath,
		String defaultExcludeFilePath,
		String defaultTrimExcludeFilePath,
		String defaultMethodParameterChangesExcludeFilePath,
		String defaultMethodParameterObfuscationExcludeFilePath,
		String defaultDirectory,
		boolean throwOnFatalError,
		boolean pluginMode,
		boolean implementsClauseMatching
	) {
		super(verbose);
		this.workspace = workspace;
		this.mainClasspathManager = classpathManager;
		super.throwOnFatalError = throwOnFatalError;
		this.pluginMode = pluginMode;
		this.implementsClauseMatching = implementsClauseMatching;
		this.initDefaultPaths(
			trimLogFilePath,
			defaultExcludeFilePath,
			defaultTrimExcludeFilePath,
			defaultMethodParameterChangesExcludeFilePath,
			defaultMethodParameterObfuscationExcludeFilePath,
			defaultDirectory
		);
		if (logFilePath != null && !logFilePath.trim().isEmpty()) {
			this.logFilePath = FileUtils.resolveRelativePath(FileUtils.normalizePathSeparators(logFilePath), this.defaultDirectory);
		} else {
			this.logFilePath = "ZKM_log.txt";
		}

		if (FileUtils.isRelativePath(this.logFilePath)) {
			this.logFilePath = new File(this.defaultDirectory, this.logFilePath).getAbsolutePath();
		} else {
			this.logFilePath = new File(this.logFilePath).getAbsolutePath();
		}

		try {
			File logFile = new File(this.logFilePath);
			this.validateLogFilePaths(this.logFilePath, this.trimLogFilePath);
			File logFileParent = logFile.getParentFile();
			if (!logFileParent.exists()) {
				DirectoryScanner.ensureDirectory(logFileParent.getAbsolutePath());
			}

			AtomicReference<String> errMessageRef = new AtomicReference<>();
			if (!FileUtils.testFileSystemWriteRead(this.logFilePath, errMessageRef)) {
				throw new ProcessingException("Log file \"" + this.logFilePath + "\" could not be created, modified and read. : '" + errMessageRef.get() + "' (B)");
			}

			super.writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(logFile), "UTF-8"), true);
		} catch (IOException ioException) {
			throw new ProcessingException("Log file \"" + this.logFilePath + "\" could not be opened : " + ioException);
		}

		if (defaultDirectory != null) {
			this.logMessage("Default directory set to '" + this.defaultDirectory.getAbsolutePath() + "'" + Options.lineSeparator);
		}
	}

	public ScriptContext(
		Workspace workspace,
		ClasspathManager classpathManager,
		boolean verbose,
		String logFilePath,
		String trimLogFilePath,
		String defaultExcludeFilePath,
		String defaultTrimExcludeFilePath,
		String defaultMethodParameterChangesExcludeFilePath,
		String defaultMethodParameterObfuscationExcludeFilePath,
		String defaultDirectory,
		boolean throwOnFatalError,
		boolean pluginMode
	) {
		this(workspace, classpathManager, verbose, logFilePath, trimLogFilePath, defaultExcludeFilePath, defaultTrimExcludeFilePath, defaultMethodParameterChangesExcludeFilePath, defaultMethodParameterObfuscationExcludeFilePath, defaultDirectory, throwOnFatalError, pluginMode, false);
	}

	public void addMethodParameterChangesExcludeStatement(MethodParameterChangesExcludeStatement statement) {
		this.methodParameterChangesExcludeStatements.add(statement);
	}

	public void addLongEncryptionExcludeStatement(LongEncryptionExcludeStatement statement) {
		this.longEncryptionExcludeStatements.add(statement);
	}

	public String getDefaultMethodParamChangesExcludePath() {
		return this.defaultMethodParamChangesExcludePath;
	}

	public List<ObfuscateExceptionsExcludeStatement> getObfuscateExceptionsExcludeStatements() {
		return this.obfuscateExceptionsExcludeStatements;
	}

	public void addAccessedByReflectionStatement(AccessedByReflectionStatement statement) {
		this.accessedByReflectionStatements.add(statement);
	}

	public void clearObfuscateFlowExclusions() {
		this.obfuscateFlowExcludeStatements = new ArrayList<>();
		this.obfuscateFlowUnexcludeStatements = new ArrayList<>();
	}

	public Workspace getWorkspace() {
		return this.workspace;
	}

	public String getDefaultMethodParamObfuscationExcludePath() {
		return this.defaultMethodParamObfuscationExcludePath;
	}

	public boolean hasMicroEditionClasses() {
		return this.microEditionClassesFound;
	}

	public List<TrimExcludeStatement> getTrimExcludeStatements() {
		return this.trimExcludeStatements;
	}

	public void clearObfuscateExceptionsExclusions() {
		this.obfuscateExceptionsExcludeStatements = new ArrayList<>();
		this.obfuscateExceptionsUnexcludeStatements = new ArrayList<>();
	}

	public void clearMethodParameterObfuscationExclusions() {
		this.methodParameterObfuscationIncludeStatements = new ArrayList<>();
		this.methodParameterObfuscationExcludeStatements = new ArrayList<>();
	}

	public void clearObfuscateReferencesExclusions() {
		this.obfuscateReferencesIncludeStatements = new ArrayList<>();
		this.obfuscateReferencesExcludeStatements = new ArrayList<>();
	}

	public void resetTrimExcludedMemberSet() {
		this.trimExcludedMemberSet = null;
	}

	public void clearRemoveMethodCalls() {
		this.removeMethodCallsIncludeStatements = new ArrayList<>();
		this.removeMethodCallsExcludeStatements = new ArrayList<>();
	}

	public void clearIgnoreMissingReferences() {
		this.phantomClassManager = null;
		this.ignoreMissingReferencesStatements = new ArrayList<>();
	}

	public void clearTrimExclusions() {
		this.trimExcludeStatements = new ArrayList<>();
		this.trimUnexcludeStatements = new ArrayList<>();
		this.trimExcludedMemberSet = null;
	}

	public void clearIntegerEncryptionExclusions() {
		this.integerEncryptionExcludeStatements = new ArrayList<>();
		this.integerEncryptionUnexcludeStatements = new ArrayList<>();
	}

	public List<AccessedByReflectionStatement> getAccessedByReflectionStatements() {
		return this.accessedByReflectionStatements;
	}

	public void addObfuscateReferencesIncludeStatement(ObfuscateReferencesIncludeStatement statement) {
		this.obfuscateReferencesIncludeStatements.add(statement);
	}

	public void setFixedClassManager(FixedClassManager fixedClassManager) {
		this.fixedClassManager = fixedClassManager;
	}

	private void initDefaultPaths(String trimLogFilePath, String defaultExcludePath, String defaultTrimExcludePath, String defaultMethodParamChangesExcludePath, String defaultMethodParamObfuscationExcludePath, String defaultDirectory) {
		if (defaultDirectory != null) {
			this.defaultDirectory = new File(FileUtils.resolveRelativePath(FileUtils.normalizePathSeparators(defaultDirectory), null));
			if (!this.defaultDirectory.exists() || !this.defaultDirectory.isDirectory()) {
				throw new ProcessingException("ERROR: Default directory \"" + this.defaultDirectory.getAbsolutePath() + "\" does not exist or it is not a directory");
			}

			this.defaultDirectory = new File(this.defaultDirectory.getAbsolutePath());
		} else if (Options.ZKM_DEFAULT_DIR != null) {
			this.defaultDirectory = new File(FileUtils.resolveRelativePath(FileUtils.normalizePathSeparators(Options.ZKM_DEFAULT_DIR), null));
			if (!this.defaultDirectory.exists() || !this.defaultDirectory.isDirectory()) {
				throw new ProcessingException(
					"ERROR: Default directory \"" + this.defaultDirectory.getAbsolutePath() + "\" specified by \"ZKM_DEFAULT_DIR\" does not exist or it is not a directory"
				);
			}
		} else {
			this.defaultDirectory = new File(SystemConstants.userDirectory);
		}

		if (trimLogFilePath != null && trimLogFilePath.trim().length() != 0) {
			this.trimLogFilePath = FileUtils.resolveRelativePath(FileUtils.normalizePathSeparators(trimLogFilePath), this.defaultDirectory);
		} else {
			this.trimLogFilePath = "ZKM_TrimLog.txt";
		}

		if (FileUtils.isRelativePath(this.trimLogFilePath)) {
			this.trimLogFilePath = new File(this.defaultDirectory, this.trimLogFilePath).getAbsolutePath();
		} else {
			this.trimLogFilePath = new File(this.trimLogFilePath).getAbsolutePath();
		}

		if (defaultExcludePath != null && defaultExcludePath.trim().length() != 0) {
			this.defaultExcludePath = FileUtils.resolveRelativePath(FileUtils.normalizePathSeparators(defaultExcludePath), this.defaultDirectory);
		} else {
			this.defaultExcludePath = "defaultExclude.txt";
		}

		if (FileUtils.isRelativePath(this.defaultExcludePath)) {
			this.defaultExcludePath = new File(this.defaultDirectory, this.defaultExcludePath).getAbsolutePath();
		} else {
			this.defaultExcludePath = new File(this.defaultExcludePath).getAbsolutePath();
		}

		if (defaultTrimExcludePath != null && defaultTrimExcludePath.trim().length() != 0) {
			this.defaultTrimExcludePath = FileUtils.resolveRelativePath(FileUtils.normalizePathSeparators(defaultTrimExcludePath), this.defaultDirectory);
		} else {
			this.defaultTrimExcludePath = "defaultTrimExclude.txt";
		}

		if (defaultMethodParamChangesExcludePath != null && defaultMethodParamChangesExcludePath.trim().length() != 0) {
			this.defaultMethodParamChangesExcludePath = FileUtils.resolveRelativePath(FileUtils.normalizePathSeparators(defaultMethodParamChangesExcludePath), this.defaultDirectory);
		} else {
			this.defaultMethodParamChangesExcludePath = "defaultMethodParameterChangesExclude.txt";
		}

		if (defaultMethodParamObfuscationExcludePath != null && defaultMethodParamObfuscationExcludePath.trim().length() != 0) {
			this.defaultMethodParamObfuscationExcludePath = FileUtils.resolveRelativePath(FileUtils.normalizePathSeparators(defaultMethodParamObfuscationExcludePath), this.defaultDirectory);
		} else {
			this.defaultMethodParamObfuscationExcludePath = "defaultMethodParameterObfuscationExclude.txt";
		}

		if (FileUtils.isRelativePath(this.defaultTrimExcludePath)) {
			this.defaultTrimExcludePath = new File(this.defaultDirectory, this.defaultTrimExcludePath).getAbsolutePath();
		} else {
			this.defaultTrimExcludePath = new File(this.defaultTrimExcludePath).getAbsolutePath();
		}
	}

	public void addRemoveMethodCallsExcludeStatement(RemoveMethodCallsExcludeStatement statement) {
		this.removeMethodCallsExcludeStatements.add(statement);
	}

	public List<IntegerEncryptionExcludeStatement> getIntegerEncryptionExcludeStatements() {
		return this.integerEncryptionExcludeStatements;
	}

	public void addIntegerEncryptionExcludeStatement(IntegerEncryptionExcludeStatement statement) {
		this.integerEncryptionExcludeStatements.add(statement);
	}

	public void addIgnoreMissingReferencesStatement(IgnoreMissingReferencesStatement statement) {
		this.ignoreMissingReferencesStatements.add(statement);
	}

	public List<LongEncryptionUnexcludeStatement> getLongEncryptionUnexcludeStatements() {
		return this.longEncryptionUnexcludeStatements;
	}

	public PhantomClassManager getPhantomClassManager() {
		return this.phantomClassManager;
	}

	public List<TrimUnexcludeStatement> getTrimUnexcludeStatements() {
		return this.trimUnexcludeStatements;
	}

	public void setClassesLoaded(boolean classesLoaded) {
		this.classesLoaded = classesLoaded;
	}

	public void addTrimUnexcludeStatement(TrimUnexcludeStatement statement) {
		this.trimUnexcludeStatements.add(statement);
	}

	public void addExcludeStatement(ExcludeStatement statement) {
		this.excludeStatements.add(statement);
	}

	public boolean isPluginMode() {
		return this.pluginMode;
	}

	public List<ExistingSerializedClassesStatement> getExistingSerializedClassesStatements() {
		return this.existingSerializedClassesStatements;
	}

	public void setTrimExcludedMemberSet(TrimExcludedMemberSet trimExcludedMemberSet) {
		this.trimExcludedMemberSet = trimExcludedMemberSet;
	}

	public void addIntegerEncryptionUnexcludeStatement(IntegerEncryptionUnexcludeStatement statement) {
		this.integerEncryptionUnexcludeStatements.add(statement);
	}

	public String getDefaultTrimExcludePath() {
		return this.defaultTrimExcludePath;
	}

	public void clearMethodParameterChangesExclusions() {
		this.methodParameterChangesIncludeStatements = new ArrayList<>();
		this.methodParameterChangesExcludeStatements = new ArrayList<>();
	}

	public List<ObfuscateReferencesIncludeStatement> getObfuscateReferencesIncludeStatements() {
		return this.obfuscateReferencesIncludeStatements;
	}

	public String getTrimLogFilePath() {
		return this.trimLogFilePath;
	}

	public void clearExistingSerializedClasses() {
		this.existingSerializedClassesStatements = new ArrayList<>();
	}

	public ClasspathManager getVersionedClasspathManager(Integer releaseVersion) {
		if (releaseVersion == null) {
			return this.mainClasspathManager;
		} else {
			ClassRepository classRepository = this.mainClasspathManager.getClassRepository();
			ClasspathManager resolvedManager = null;
			if (this.versionedClasspathManagers == null) {
				this.versionedClasspathManagers = new ArrayList<>();
				resolvedManager = new ClasspathManager(System.getProperty("java.class.path"), releaseVersion);
				classRepository.addVersionedClasspath(resolvedManager);
				this.versionedClasspathManagers.add(resolvedManager);
			} else {
				int managerCount = this.versionedClasspathManagers.size();
				boolean existingManagerFound = false;

				for (int i = 0; i < managerCount; i++) {
					ClasspathManager versionedManager = this.versionedClasspathManagers.get(i);
					if (versionedManager.getReleaseVersion().equals(releaseVersion)) {
						resolvedManager = versionedManager;
						existingManagerFound = true;
						break;
					}
				}

				if (!existingManagerFound) {
					resolvedManager = new ClasspathManager(System.getProperty("java.class.path"), releaseVersion);
					classRepository.addVersionedClasspath(resolvedManager);
					this.versionedClasspathManagers.add(resolvedManager);
				}

				if (this.versionedClasspathManagers.size() > 1) {
					Collections.sort(this.versionedClasspathManagers, (firstManager, secondManager) -> firstManager.getReleaseVersion() - secondManager.getReleaseVersion());
				}
			}

			return resolvedManager;
		}
	}

	public List<LongEncryptionExcludeStatement> getLongEncryptionExcludeStatements() {
		return this.longEncryptionExcludeStatements;
	}

	public List<ObfuscateExceptionsUnexcludeStatement> getObfuscateExceptionsUnexcludeStatements() {
		return this.obfuscateExceptionsUnexcludeStatements;
	}

	public void addFixedClassesStatement(FixedClassesStatement statement) {
		this.fixedClassesStatements.add(statement);
	}

	public void setMicroEditionClassesFound(boolean microEditionClassesFound) {
		this.microEditionClassesFound = microEditionClassesFound;
	}

	public void addExistingSerializedClassesStatement(ExistingSerializedClassesStatement statement) {
		this.existingSerializedClassesStatements.add(statement);
	}

	public void addObfuscateReferencesExcludeStatement(ObfuscateReferencesExcludeStatement statement) {
		this.obfuscateReferencesExcludeStatements.add(statement);
	}

	public List<StringEncryptionUnexcludeStatement> getEncryptStringLiteralsUnexcludeStatements() {
		return this.encryptStringLiteralsUnexcludeStatements;
	}

	public List<RemoveMethodCallsIncludeStatement> getRemoveMethodCallsIncludeStatements() {
		return this.removeMethodCallsIncludeStatements;
	}

	public boolean isThrowOnFatalError() {
		return super.throwOnFatalError;
	}

	public void addLongEncryptionUnexcludeStatement(LongEncryptionUnexcludeStatement statement) {
		this.longEncryptionUnexcludeStatements.add(statement);
	}

	public void clearExclusions() {
		this.excludeStatements = new ArrayList<>();
		this.unexcludeStatements = new ArrayList<>();
	}

	public void addObfuscateExceptionsUnexcludeStatement(ObfuscateExceptionsUnexcludeStatement statement) {
		this.obfuscateExceptionsUnexcludeStatements.add(statement);
	}

	public String getDefaultExcludePath() {
		return this.defaultExcludePath;
	}

	public List<RemoveMethodCallsExcludeStatement> getRemoveMethodCallsExcludeStatements() {
		return this.removeMethodCallsExcludeStatements;
	}

	public void clearLongEncryptionExclusions() {
		this.longEncryptionExcludeStatements = new ArrayList<>();
		this.longEncryptionUnexcludeStatements = new ArrayList<>();
	}

	public File getDefaultDirectory() {
		return this.defaultDirectory;
	}

	public void addObfuscateExceptionsExcludeStatement(ObfuscateExceptionsExcludeStatement statement) {
		this.obfuscateExceptionsExcludeStatements.add(statement);
	}

	public void addTrimExcludeStatement(TrimExcludeStatement statement) {
		this.trimExcludeStatements.add(statement);
	}

	public ReferenceGraph<String, String> getReferenceGraph() {
		return this.referenceGraph;
	}

	public List<ObfuscateReferencesExcludeStatement> getObfuscateReferencesExcludeStatements() {
		return this.obfuscateReferencesExcludeStatements;
	}

	public void addAccessedByReflectionExcludeStatement(AccessedByReflectionExcludeStatement statement) {
		this.accessedByReflectionExcludeStatements.add(statement);
	}

	public List<ExcludeStatement> getExcludeStatements() {
		return this.excludeStatements;
	}

	public void clearReferenceGraph() {
		this.referenceGraph = new LinkedReferenceGraph<>();
	}

	public void buildPhantomClassManager() {
		if (this.ignoreMissingReferencesStatements.size() > 0) {
			this.phantomClassManager = new PhantomClassManager(this.workspace, this.ignoreMissingReferencesStatements, this);
		}
	}

	public List<ObfuscateFlowExcludeStatement> getObfuscateFlowExcludeStatements() {
		return this.obfuscateFlowExcludeStatements;
	}

	public List<MethodParameterChangesIncludeStatement> getMethodParameterChangesIncludeStatements() {
		return this.methodParameterChangesIncludeStatements;
	}

	public List<StringEncryptionExcludeStatement> getEncryptStringLiteralsExcludeStatements() {
		return this.encryptStringLiteralsExcludeStatements;
	}

	public boolean isPreverifyDisabled() {
		return this.preverifyDisabled;
	}

	public void addMethodParameterChangesIncludeStatement(MethodParameterChangesIncludeStatement statement) {
		this.methodParameterChangesIncludeStatements.add(statement);
	}

	public List<AccessedByReflectionExcludeStatement> getAccessedByReflectionExcludeStatements() {
		return this.accessedByReflectionExcludeStatements;
	}

	public List<ObfuscateFlowUnexcludeStatement> getObfuscateFlowUnexcludeStatements() {
		return this.obfuscateFlowUnexcludeStatements;
	}

	public void clearGroupings() {
		this.groupingsStatements = new ArrayList<>();
	}

	public boolean hasReferenceGraph() {
		return this.referenceGraph != null;
	}

	public void clearAccessedByReflection() {
		this.accessedByReflectionStatements = new ArrayList<>();
		this.accessedByReflectionExcludeStatements = new ArrayList<>();
	}

	public String getLogFilePath() {
		return this.logFilePath;
	}

	public List<MethodParameterObfuscationExcludeStatement> getMethodParameterObfuscationExcludeStatements() {
		return this.methodParameterObfuscationExcludeStatements;
	}

	private void validateLogFilePaths(String logFilePath, String trimLogFilePath) {
		if (logFilePath == null) {
			throw new LookupException("The " + CopyrightConstants.NAME + " log file name cannot be null");
		} else if (FileUtils.IS_UNIX && logFilePath.equals(trimLogFilePath) || !FileUtils.IS_UNIX && logFilePath.equalsIgnoreCase(trimLogFilePath)) {
			throw new LookupException("The " + CopyrightConstants.NAME + " log file name cannot be the same as the Trim log file name. : " + FileUtils.OS_NAME);
		}
	}

	public boolean isImplementsClauseMatching() {
		return this.implementsClauseMatching;
	}

	public void addGroupingsStatement(GroupingsStatement statement) {
		this.groupingsStatements.add(statement);
	}

	public List<GroupingsStatement> getGroupingsStatements() {
		return this.groupingsStatements;
	}

	public List<UnexcludeStatement> getUnexcludeStatements() {
		return this.unexcludeStatements;
	}

	public void addMethodParameterObfuscationExcludeStatement(MethodParameterObfuscationExcludeStatement statement) {
		this.methodParameterObfuscationExcludeStatements.add(statement);
	}

	public void addReferences(ReferenceGraph<String, String> referenceGraph) {
		for (Entry<String, Set<String>> entry : referenceGraph.getAdjacencyEntries()) {
			this.referenceGraph.addReferences(entry.getKey(), entry.getValue());
		}
	}

	public boolean hasLoadedClasses() {
		return this.classesLoaded;
	}

	public void setPreverifyDisabled(boolean preverifyDisabled) {
		this.preverifyDisabled = preverifyDisabled;
	}

	@Override
	public void close() {
		super.close();
		if (this.mainClasspathManager != null) {
			this.mainClasspathManager.closeJrtFileSystem();
		}
	}

	public List<IntegerEncryptionUnexcludeStatement> getIntegerEncryptionUnexcludeStatements() {
		return this.integerEncryptionUnexcludeStatements;
	}

	public List<MethodParameterObfuscationIncludeStatement> getMethodParameterObfuscationIncludeStatements() {
		return this.methodParameterObfuscationIncludeStatements;
	}

	public void addEncryptStringLiteralsExcludeStatement(StringEncryptionExcludeStatement statement) {
		this.encryptStringLiteralsExcludeStatements.add(statement);
	}

	public void addObfuscateFlowExcludeStatement(ObfuscateFlowExcludeStatement statement) {
		this.obfuscateFlowExcludeStatements.add(statement);
	}

	public void addRemoveMethodCallsIncludeStatement(RemoveMethodCallsIncludeStatement statement) {
		this.removeMethodCallsIncludeStatements.add(statement);
	}

	public void addMethodParameterObfuscationIncludeStatement(MethodParameterObfuscationIncludeStatement statement) {
		this.methodParameterObfuscationIncludeStatements.add(statement);
	}

	public void addEncryptStringLiteralsUnexcludeStatement(StringEncryptionUnexcludeStatement statement) {
		this.encryptStringLiteralsUnexcludeStatements.add(statement);
	}

	public List<FixedClassesStatement> getFixedClassesStatements() {
		return this.fixedClassesStatements;
	}

	public void addUnexcludeStatement(UnexcludeStatement statement) {
		this.unexcludeStatements.add(statement);
	}

	public void clearEncryptStringLiteralsExclusions() {
		this.encryptStringLiteralsExcludeStatements = new ArrayList<>();
		this.encryptStringLiteralsUnexcludeStatements = new ArrayList<>();
	}

	public List<MethodParameterChangesExcludeStatement> getMethodParameterChangesExcludeStatements() {
		return this.methodParameterChangesExcludeStatements;
	}

	public void addObfuscateFlowUnexcludeStatement(ObfuscateFlowUnexcludeStatement statement) {
		this.obfuscateFlowUnexcludeStatements.add(statement);
	}

	public TrimExcludedMemberSet getTrimExcludedMemberSet() {
		return this.trimExcludedMemberSet;
	}

	public void clearFixedClasses() {
		this.fixedClassesStatements = new ArrayList<>();
		this.fixedClassManager = null;
	}

	public FixedClassManager getFixedClassManager() {
		return this.fixedClassManager;
	}
}
