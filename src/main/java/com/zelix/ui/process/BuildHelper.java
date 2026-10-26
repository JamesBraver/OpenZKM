package com.zelix.ui.process;

import lombok.SneakyThrows;

import com.zelix.HelpManager;
import com.zelix.UIConfig;
import com.zelix.exceptions.ClasspathException;
import com.zelix.exceptions.FileException;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.klassmaster.config.FilterConfig;
import com.zelix.klassmaster.config.ObfuscateConfig;
import com.zelix.klassmaster.config.ReferenceObfuscateConfig;
import com.zelix.klassmaster.config.RuleFilterConfig;
import com.zelix.klassmaster.config.TrimConfig;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassLoadingStatus;
import com.zelix.repository.ClasspathManager;
import com.zelix.ui.GUI;
import com.zelix.ui.dataset.ClassNodeEntry;
import com.zelix.ui.dialogs.BuildHelperClasspathDialog;
import com.zelix.ui.dialogs.BuildHelperDialog;
import com.zelix.ui.dialogs.SavingDialog;
import com.zelix.ui.dialogs.script.ScriptBuildHelperDialog;
import com.zelix.ui.dialogs.script.ScriptOutputDialog;
import com.zelix.ui.frames.BuildHelperTrimExcludeFrame;
import com.zelix.ui.frames.WrappedOpenClassesFrame;
import com.zelix.ui.frames.WrappedTrimOptionsFrame;
import com.zelix.ui.frames.tools.ObfuscateFrame;
import com.zelix.ui.frames.tools.obfuscate.WrappedObfuscateOptionsFrame;
import com.zelix.ui.frames.tools.obfuscate.references.ObfuscateReferencesFrame;
import com.zelix.ui.prompts.CopyablePrompt;
import com.zelix.ui.prompts.ErrorPrompt;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.function.OptionalConsumer;
import com.zelix.utils.function.ZKMConsumer;
import com.zelix.utils.references.AtomicReference;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class BuildHelper extends ScriptWriterContext {
	public ClasspathResource[] selectedResources;
	private ArchiveNode[] archiveNodes;
	private ClasspathResource[] jadResources;
	private ClasspathResource[] xmlResources;

	public BuildHelper(GUI gui, ScriptContext scriptContext, UIConfig uiConfig) {
		super(gui, scriptContext, uiConfig);
	}

	@Override
	public void restoreGui() {
		super.gui.setEnabled(true);
		super.gui.toFront();
		super.gui.setDisable(false);
	}

	private void promptOpenClasses(ClasspathResource[] selectedResources) {
		BuildHelper.OpenClassesListener openClassesListener = new BuildHelper.OpenClassesListener(this);
		super.gui.setEnabled(false);
		new WrappedOpenClassesFrame(
			super.gui, "Build Helper - Open Classes", "Next", true, super.uiConfig.getLastRunningPath(), super.uiConfig.getOpenNestedArchives(), selectedResources, openClassesListener
		);
	}

	private void showScriptOutput(String scriptText) {
		BuildHelper.ScriptOutputListener scriptOutputListener = new BuildHelper.ScriptOutputListener(this);
		new ScriptOutputDialog(super.gui, "Build Helper - Equivalent ZKM Script", scriptText, super.uiConfig, scriptOutputListener);
	}

	@Override
	public void showObfuscateOptions() {
		this.promptObfuscate(new ObfuscateConfig(SystemConstants.userDirectory, "ZKM_SO.ser"));
	}

	private void promptOpenClassesNoDefaults() {
		this.promptOpenClasses(null);
	}

	private void handleTrimResult(TrimConfig trimConfig, Integer stepResult) throws IOException {
		int stepResultCode = stepResult;
		super.trimConfig = trimConfig;
		if (stepResultCode == 1) {
			super.trimConfig.saveToFile(SystemConstants.userDirectory, "ZKM_TO.ser");
			super.skipTrim = false;
			BuildHelper.TrimStatus trimStatus = new BuildHelper.TrimStatus(this);
			super.gui.setDisable(true);
			super.gui.setEnabled(true);
			super.gui.toFront();
			if (super.skipTrimExclude) {
				super.gui.trim(new Vector<>(), super.trimConfig, super.scriptContext, trimStatus);
			} else {
				super.gui.trim(super.trimExcludeConfig.getParameters(), super.trimConfig, super.scriptContext, trimStatus);
			}
		} else if (stepResultCode == 2) {
			this.promptTrimExclude();
		} else if (stepResultCode == 4) {
			super.skipTrim = true;
			this.promptExclude();
		}
	}

	private void handleObfuscateResult(ObfuscateConfig obfuscateConfig, Integer stepResult) throws IOException {
		int stepResultCode = stepResult;
		super.obfuscateConfig = obfuscateConfig;
		if (stepResultCode == 1) {
			super.obfuscateConfig.saveToFile(SystemConstants.userDirectory, "ZKM_SO.ser");
			if (super.obfuscateConfig.obfuscateReferences == 1) {
				BuildHelper.ObfuscateReferencesListener obfuscateReferencesListener = new BuildHelper.ObfuscateReferencesListener(this);
				this.promptObfuscateReferences("Build Helper - Reference Obfuscation Inclusions", super.gui, obfuscateReferencesListener);
			} else {
				super.referenceObfuscateConfig = null;
				BuildHelper.ObfuscateStatus obfuscateStatus = new BuildHelper.ObfuscateStatus(this);
				super.gui.setDisable(true);
				super.gui.setEnabled(true);
				super.gui.toFront();
				super.gui.obfuscate(super.excludeConfig.getParameters(), null, super.obfuscateConfig, super.scriptContext, obfuscateStatus);
			}
		} else if (stepResultCode == 2) {
			this.promptExclude();
		}
	}

	private void handleBuildHelperInfoResult(Integer stepResult) {
		int stepResultCode = stepResult;
		if (stepResultCode == 2) {
			if (this.selectedResources == null) {
				this.promptOpenClassesNoDefaults();
			} else {
				this.promptOpenClasses(this.selectedResources);
			}
		} else if (stepResultCode == 1) {
			this.promptTrimExclude();
		}
	}

	private void promptExclude() {
		if (super.excludeConfig == null) {
			super.excludeConfig = FilterConfig.loadFromFile(SystemConstants.userDirectory, "ZKM_EX.ser");
		}

		BuildHelper.ObfuscateExcludeListener obfuscateExcludeListener = new BuildHelper.ObfuscateExcludeListener(this);
		List<ClassNodeEntry> mainClassEntries = null;

		try {
			mainClassEntries = super.gui.getMainMethodClasses();
		} catch (ClasspathException classpathException) {
			new CopyablePrompt(
				super.gui,
				"Classpath Error",
				"Error while determining classes with main methods : Class '" + GlobalUtils.internalToBinaryName(classpathException.getClassName()) + "' not found."
			);
		} catch (FileException fileException) {
			new CopyablePrompt(super.gui, "File Error", "Error while determining classes with main methods : '" + fileException.getMessage() + "'");
		}

		new ObfuscateFrame("Build Helper - Obfuscate Name Exclusions", super.gui, super.excludeConfig, super.gui.getPackageManager(), mainClassEntries, super.scriptContext, obfuscateExcludeListener);
	}

	private void showBuildFinishedDialog() {
		BuildHelper.ZKMScriptListener zkmscriptListener = new BuildHelper.ZKMScriptListener(this);
		new ScriptBuildHelperDialog(super.gui, "Build Helper", "ZKM Script", "Exit", "003", "013", 300, 250, zkmscriptListener);
	}

	@Override
	public void promptOutputArchivePath(String defaultPath) {
		BuildHelper.SaveClassesListener saveClassesListener = new BuildHelper.SaveClassesListener(this);
		new SavingDialog(super.gui, "Build Helper - Save Classes", "Next", HelpManager.getToolTip("SAVE_CLASSES"), defaultPath, saveClassesListener);
	}

	private void handleTrimExcludeResult(Integer stepResult) {
		int stepResultCode = stepResult;
		if (stepResultCode == 1) {
			super.trimExcludeConfig.saveToFile(SystemConstants.userDirectory, "ZKM_TEX.ser");
			super.skipTrimExclude = false;
			if (super.trimConfig == null) {
				this.promptCurrentTrim();
			} else {
				this.promptTrim(super.trimConfig);
			}
		} else if (stepResultCode == 2) {
			this.showBuildHelperInfo();
		} else if (stepResultCode == 4) {
			super.skipTrimExclude = true;
			if (super.trimConfig == null) {
				this.promptCurrentTrim();
			} else {
				this.promptTrim(super.trimConfig);
			}
		}
	}

	private void promptObfuscate(ObfuscateConfig obfuscateConfig) {
		BuildHelper.ObfuscateOptionsListener obfuscateOptionsListener = new BuildHelper.ObfuscateOptionsListener(this);
		new WrappedObfuscateOptionsFrame("Build Helper - Obfuscate Options", HelpManager.getToolTip("PERFORM_OBFUSCATE"), super.gui, obfuscateConfig, super.excludeConfig, super.scriptContext, obfuscateOptionsListener);
	}

	private void handleStartDialogResult(Integer stepResult) {
		if (super.classpathManager == null) {
			this.promptCurrentClasspath();
		} else {
			this.promptClasspath(super.classpathManager);
		}
	}

	private void handleScriptOutputResult(Integer stepResult) {
		if (stepResult == 2) {
			this.showBuildFinishedDialog();
		}
	}

	private void handleClasspathResult(ClasspathManager classpathManager, Integer stepResult) {
		super.classpathManager = classpathManager;
		if (stepResult == 1) {
			if (this.selectedResources == null) {
				this.promptOpenClassesNoDefaults();
			} else {
				this.promptOpenClasses(this.selectedResources);
			}
		} else {
			this.showStartDialog();
		}
	}

	private void promptTrimExclude() {
		if (super.trimExcludeConfig == null) {
			super.trimExcludeConfig = RuleFilterConfig.loadFromFile(SystemConstants.userDirectory, "ZKM_TEX.ser");
		}

		BuildHelper.TrimExcludeListener trimExcludeListener = new BuildHelper.TrimExcludeListener(this);
		List<ClassNodeEntry> mainClassEntries = null;

		try {
			mainClassEntries = super.gui.getMainMethodClasses();
		} catch (ClasspathException classpathException) {
			new CopyablePrompt(
				super.gui,
				"Classpath Error",
				"Error while determining classes with main methods : Class '" + GlobalUtils.internalToBinaryName(classpathException.getClassName()) + "' not found."
			);
		} catch (FileException fileException) {
			new CopyablePrompt(super.gui, "File Error", "Error while determining classes with main methods : '" + fileException.getMessage() + "'");
		}

		new BuildHelperTrimExcludeFrame("Build Helper - Trim Exclusions", super.gui, mainClassEntries, super.trimExcludeConfig, super.gui.getWorkspace(), super.trimConfig, super.scriptContext, trimExcludeListener);
	}

	private void handleSaveClassesResult(File outputFile, Integer stepResult) {
		super.outputArchivePath = outputFile.getAbsolutePath();
		int stepResultCode = stepResult;
		if (stepResultCode == 1) {
			if (super.outputArchivePath != null) {
				super.uiConfig.setLastSavingPath(super.outputArchivePath);
			}

			super.uiConfig.saveConfig();
			super.gui.saveAll(outputFile, new OptionalConsumer() {
				@Override
				public void accept() {
					BuildHelper.this.showBuildFinishedDialog();
				}
			});
		} else if (stepResultCode == 2) {
			this.promptObfuscate(super.obfuscateConfig);
		}
	}

	private void promptCurrentClasspath() {
		this.promptClasspath(super.gui.getClasspathManager());
	}

	@Override
	public void handleStepResult(Integer stepResult) throws IOException {
		int stepResultCode = stepResult;
		super.referenceObfuscateConfig.saveToFile(SystemConstants.userDirectory, "ZKM_OB_REF.ser");
		if (stepResultCode == 1) {
			BuildHelper.ReferenceObfuscateStatus referenceObfuscateStatus = new BuildHelper.ReferenceObfuscateStatus(this);
			super.gui.setDisable(true);
			super.gui.setEnabled(true);
			super.gui.toFront();
			super.gui.obfuscate(super.excludeConfig.getParameters(), super.referenceObfuscateConfig == null ? null : super.referenceObfuscateConfig.getParameters(), super.obfuscateConfig, super.scriptContext, referenceObfuscateStatus);
		} else if (stepResultCode == 2 || stepResultCode == 5) {
			this.promptObfuscate(super.obfuscateConfig);
		}
	}

	private void promptClasspath(ClasspathManager classpathManager) {
		BuildHelper.ClasspathListener classpathListener = new BuildHelper.ClasspathListener(this);
		new BuildHelperClasspathDialog(
			super.gui, "Build Helper - Classpath", classpathManager, HelpManager.getToolTip("CLASSPATH_MSG"), "Next", HelpManager.getToolTip("SET_CLASSPATH"), super.uiConfig, classpathListener
		);
	}

	private void promptTrim(TrimConfig trimConfig) {
		BuildHelper.TrimOptionsListener trimOptionsListener = new BuildHelper.TrimOptionsListener(this);
		new WrappedTrimOptionsFrame("Build Helper - Trim Options", "Next", HelpManager.getToolTip("PERFORM_TRIM"), super.gui, trimConfig, trimOptionsListener);
	}

	private void handleOpenClassesResult(
		ClasspathResource[] classes,
		ArchiveNode[] archiveNodes,
		ClasspathResource[] jadResources,
		ClasspathResource[] xmlResources,
		ClasspathResource[] selectedResources,
		AtomicReference<String> runningPathRef,
		Boolean openNestedArchives,
		Set<ArchiveNode> archiveNodeSet,
		int stepResult
	) {
		if (classes != null) {
			super.openResources = classes;
			super.openResourcesChanged = true;
		}

		if (stepResult == 1) {
			this.archiveNodes = archiveNodes;
			this.jadResources = jadResources;
			this.xmlResources = xmlResources;
			this.selectedResources = selectedResources;
			String lastRunningPath = runningPathRef.get();
			if (lastRunningPath != null) {
				super.uiConfig.setLastRunningPath(lastRunningPath);
			}

			super.uiConfig.saveConfig();
			BuildHelper.OpenClassesStatus openClassesStatus = new BuildHelper.OpenClassesStatus(this);
			super.gui.onClassesUpdate(super.openResources, this.archiveNodes, this.jadResources, this.xmlResources, runningPathRef, openNestedArchives, archiveNodeSet, openClassesStatus);
		} else if (super.classpathManager == null) {
			this.promptCurrentClasspath();
		} else {
			this.promptClasspath(super.classpathManager);
		}
	}

	private void promptCurrentTrim() {
		this.promptTrim(super.trimConfig);
	}

	@Override
	public void promptOutputArchiveLastPath() {
		this.promptOutputArchivePath(super.uiConfig.getLastSavingPath());
	}

	public void promptObfuscateReferences(String title, GUI gui, ZKMConsumer resultConsumer) {
		super.referenceObfuscateConfig = ReferenceObfuscateConfig.loadFromFile(SystemConstants.userDirectory, "ZKM_OB_REF.ser");
		new ObfuscateReferencesFrame(title, gui, super.referenceObfuscateConfig, super.scriptContext, resultConsumer, 3);
	}

	@Override
	public void showStartDialog() {
		BuildHelper.BuildHelperStartListener buildHelperStartListener = new BuildHelper.BuildHelperStartListener(this);
		new ScriptBuildHelperDialog(super.gui, "Build Helper", "Next", "Cancel", "002", "012", 500, 400, buildHelperStartListener);
	}

	private void handleExcludeResult(Integer stepResult) {
		int stepResultCode = stepResult;
		if (stepResultCode == 1) {
			super.excludeConfig.saveToFile(SystemConstants.userDirectory, "ZKM_EX.ser");
			if (super.obfuscateConfig == null) {
				this.showObfuscateOptions();
			} else {
				this.promptObfuscate(super.obfuscateConfig);
			}
		} else if (stepResultCode == 2) {
			this.promptCurrentTrim();
		}
	}

	private void showBuildHelperInfo() {
		BuildHelper.BuildHelperDialogStatus buildHelperDialogStatus = new BuildHelper.BuildHelperDialogStatus(this);
		new BuildHelperDialog(super.gui, "Build Helper", "Next", HelpManager.getToolTip("OK_PROCEED"), "004", 350, 250, buildHelperDialogStatus);
	}

	public static void handleClasspathResult(BuildHelper buildHelper, ClasspathManager classpathManager, Integer stepResult) {
		buildHelper.handleClasspathResult(classpathManager, stepResult);
	}

	public static void promptTrim(BuildHelper buildHelper, TrimConfig trimConfig) {
		buildHelper.promptTrim(trimConfig);
	}

	public static void handleTrimExcludeResult(BuildHelper buildHelper, Integer stepResult) {
		buildHelper.handleTrimExcludeResult(stepResult);
	}

	public static void showBuildHelperInfo(BuildHelper buildHelper) {
		buildHelper.showBuildHelperInfo();
	}

	public static void handleScriptOutputResult(BuildHelper buildHelper, Integer stepResult) {
		buildHelper.handleScriptOutputResult(stepResult);
	}

	public static void handleOpenClassesResult(
		BuildHelper buildHelper,
		ClasspathResource[] classes,
		ArchiveNode[] archiveNodes,
		ClasspathResource[] jadResources,
		ClasspathResource[] xmlResources,
		ClasspathResource[] selectedResources,
		AtomicReference<String> runningPathRef,
		Boolean openNestedArchives,
		Set<ArchiveNode> archiveNodeSet,
		int stepResult
	) {
		buildHelper.handleOpenClassesResult(
			classes, archiveNodes, jadResources, xmlResources, selectedResources, runningPathRef, openNestedArchives, archiveNodeSet, stepResult
		);
	}

	public static void promptOpenClasses(BuildHelper buildHelper, ClasspathResource[] selectedResources) {
		buildHelper.promptOpenClasses(selectedResources);
	}

	public static void showScriptOutput(BuildHelper buildHelper, String scriptText) {
		buildHelper.showScriptOutput(scriptText);
	}

	public static void handleExcludeResult(BuildHelper buildHelper, Integer stepResult) {
		buildHelper.handleExcludeResult(stepResult);
	}

	public static void handleStartDialogResult(BuildHelper buildHelper, Integer stepResult) {
		buildHelper.handleStartDialogResult(stepResult);
	}

	public static void handleTrimResult(BuildHelper buildHelper, TrimConfig trimConfig, Integer stepResult) throws IOException {
		buildHelper.handleTrimResult(trimConfig, stepResult);
	}

	public static void handleSaveClassesResult(BuildHelper buildHelper, File outputFile, Integer stepResult) {
		buildHelper.handleSaveClassesResult(outputFile, stepResult);
	}

	public static void handleBuildHelperInfoResult(BuildHelper buildHelper, Integer stepResult) {
		buildHelper.handleBuildHelperInfoResult(stepResult);
	}

	public static void handleObfuscateResult(BuildHelper buildHelper, ObfuscateConfig obfuscateConfig, Integer stepResult) throws IOException {
		buildHelper.handleObfuscateResult(obfuscateConfig, stepResult);
	}

	public static void promptExclude(BuildHelper buildHelper) {
		buildHelper.promptExclude();
	}

	public static class TrimStatus extends ClassLoadingStatus {
		public final BuildHelper buildHelper;

		public TrimStatus(BuildHelper buildHelper) {
			this.buildHelper = buildHelper;
		}

		@Override
		public void accept() {
			if (this.isSuccess()) {
				BuildHelper.promptExclude(this.buildHelper);
			} else {
				BuildHelper.promptTrim(this.buildHelper, this.buildHelper.trimConfig);
			}
		}
	}

	public static class OpenClassesStatus extends ClassLoadingStatus {
		public final BuildHelper buildHelper;

		public OpenClassesStatus(BuildHelper buildHelper) {
			this.buildHelper = buildHelper;
		}

		@Override
		public void accept() {
			if (this.isSuccess()) {
				BuildHelper.showBuildHelperInfo(this.buildHelper);
			} else {
				String errorDetailSuffix = this.getErrorMessage() != null ? " : " + this.getErrorMessage() + "." : "";
				new ErrorPrompt(
					this.buildHelper.gui,
					"Serious Error",
					"Class not found.",
					"Error while opening classes."
						+ errorDetailSuffix
						+ " Cannot proceed to next step. Please adjust your "
						+ CopyrightConstants.NAME
						+ " classpath or class file selections."
				);
				BuildHelper.promptOpenClasses(this.buildHelper, this.buildHelper.selectedResources);
			}
		}
	}

	public static class ReferenceObfuscateStatus extends ClassLoadingStatus {
		public final BuildHelper buildHelper;

		public ReferenceObfuscateStatus(BuildHelper buildHelper) {
			this.buildHelper = buildHelper;
		}

		@Override
		public void accept() {
			if (this.buildHelper.outputArchivePath == null) {
				this.buildHelper.promptOutputArchiveLastPath();
			} else {
				this.buildHelper.promptOutputArchivePath(this.buildHelper.outputArchivePath);
			}
		}
	}

	public static class BuildHelperDialogStatus extends ClassLoadingStatus {
		public final BuildHelper buildHelper;

		public BuildHelperDialogStatus(BuildHelper buildHelper) {
			this.buildHelper = buildHelper;
		}

		public void acceptHelperChoice(Integer choice) {
			BuildHelper.handleBuildHelperInfoResult(this.buildHelper, choice);
		}

		@Override
		public void accept(Object arg) {
			this.acceptHelperChoice((Integer)arg);
		}

		@Override
		public void accept() {
			this.buildHelper.restoreGui();
		}
	}

	public static class ObfuscateStatus extends ClassLoadingStatus {
		public final BuildHelper buildHelper;

		public ObfuscateStatus(BuildHelper buildHelper) {
			this.buildHelper = buildHelper;
		}

		@Override
		public void accept() {
			if (this.buildHelper.outputArchivePath == null) {
				this.buildHelper.promptOutputArchiveLastPath();
			} else {
				this.buildHelper.promptOutputArchivePath(this.buildHelper.outputArchivePath);
			}
		}
	}

	public static class TrimExcludeListener extends OptionalConsumer {
		public final BuildHelper buildHelper;

		public TrimExcludeListener(BuildHelper buildHelper) {
			this.buildHelper = buildHelper;
		}

		public void acceptTrimExclusions(Integer choice) {
			BuildHelper.handleTrimExcludeResult(this.buildHelper, choice);
		}

		@Override
		public void accept(Object arg) {
			this.acceptTrimExclusions((Integer)arg);
		}

		@Override
		public void accept() {
			this.buildHelper.restoreGui();
		}
	}

	public static class ObfuscateExcludeListener extends OptionalConsumer {
		public final BuildHelper buildHelper;

		public ObfuscateExcludeListener(BuildHelper buildHelper) {
			this.buildHelper = buildHelper;
		}

		public void acceptObfuscateExclusions(Integer choice) {
			BuildHelper.handleExcludeResult(this.buildHelper, choice);
		}

		@Override
		public void accept(Object arg) {
			this.acceptObfuscateExclusions((Integer)arg);
		}

		@Override
		public void accept() {
			this.buildHelper.restoreGui();
		}
	}

	public static class ScriptOutputListener extends OptionalConsumer {
		public final BuildHelper buildHelper;

		public ScriptOutputListener(BuildHelper buildHelper) {
			this.buildHelper = buildHelper;
		}

		@Override
		public void accept(Object arg) {
			this.acceptScriptOutput((Integer)arg);
		}

		public void acceptScriptOutput(Integer choice) {
			BuildHelper.handleScriptOutputResult(this.buildHelper, choice);
		}

		@Override
		public void accept() {
			this.buildHelper.restoreGui();
		}
	}

	public static class ZKMScriptListener extends OptionalConsumer {
		public final BuildHelper buildHelper;

		public ZKMScriptListener(BuildHelper buildHelper) {
			this.buildHelper = buildHelper;
		}

		public void acceptZkmScript(Integer choice) {
			BuildHelper.showScriptOutput(this.buildHelper, this.buildHelper.generateScript("Build Helper", this.buildHelper.selectedResources));
		}

		@Override
		public void accept(Object arg) {
			this.acceptZkmScript((Integer)arg);
		}

		@Override
		public void accept() {
			this.buildHelper.restoreGui();
		}
	}

	public static class TrimOptionsListener extends OptionalConsumer {
		public final BuildHelper buildHelper;

		public TrimOptionsListener(BuildHelper buildHelper) {
			this.buildHelper = buildHelper;
		}

		public void acceptTrimOptions(TrimConfig trimConfig, Integer choice) throws IOException {
			BuildHelper.handleTrimResult(this.buildHelper, trimConfig, choice);
		}

		@Override
		public void accept(Object arg, Object choice) throws IOException {
			this.acceptTrimOptions((TrimConfig)arg, (Integer)choice);
		}

		@Override
		public void accept() {
			this.buildHelper.restoreGui();
		}
	}

	public static class SaveClassesListener extends OptionalConsumer {
		public final BuildHelper buildHelper;

		public SaveClassesListener(BuildHelper buildHelper) {
			this.buildHelper = buildHelper;
		}

		@Override
		public void accept(Object arg, Object choice) {
			this.acceptSaveClasses((File)arg, (Integer)choice);
		}

		public void acceptSaveClasses(File file, Integer choice) {
			BuildHelper.handleSaveClassesResult(this.buildHelper, file, choice);
		}

		@Override
		public void accept() {
			this.buildHelper.restoreGui();
		}
	}

	public static class ObfuscateReferencesListener extends OptionalConsumer {
		public final BuildHelper buildHelper;

		public ObfuscateReferencesListener(BuildHelper buildHelper) {
			this.buildHelper = buildHelper;
		}

		@SneakyThrows
		public void acceptObfuscateReferences(Integer choice) {
			this.buildHelper.handleStepResult(choice);
		}

		@Override
		public void accept(Object arg) {
			this.acceptObfuscateReferences((Integer)arg);
		}

		@Override
		public void accept() {
			this.buildHelper.restoreGui();
		}
	}

	public static class ClasspathListener extends OptionalConsumer {
		public final BuildHelper buildHelper;

		public ClasspathListener(BuildHelper buildHelper) {
			this.buildHelper = buildHelper;
		}

		@Override
		public void accept(Object arg, Object choice) {
			this.acceptClasspath((ClasspathManager)arg, (Integer)choice);
		}

		public void acceptClasspath(ClasspathManager classpathManager, Integer choice) {
			BuildHelper.handleClasspathResult(this.buildHelper, classpathManager, choice);
		}

		@Override
		public void accept() {
			this.buildHelper.restoreGui();
		}
	}

	public static class ObfuscateOptionsListener extends OptionalConsumer {
		public final BuildHelper buildHelper;

		public ObfuscateOptionsListener(BuildHelper buildHelper) {
			this.buildHelper = buildHelper;
		}

		public void acceptObfuscateOptions(ObfuscateConfig obfuscateConfig, Integer choice) throws IOException {
			BuildHelper.handleObfuscateResult(this.buildHelper, obfuscateConfig, choice);
		}

		@Override
		public void accept(Object arg, Object choice) throws IOException {
			this.acceptObfuscateOptions((ObfuscateConfig)arg, (Integer)choice);
		}

		@Override
		public void accept() {
			this.buildHelper.restoreGui();
		}
	}

	public static class BuildHelperStartListener extends OptionalConsumer {
		public final BuildHelper buildHelper;

		public BuildHelperStartListener(BuildHelper buildHelper) {
			this.buildHelper = buildHelper;
		}

		@Override
		public void accept(Object arg) {
			this.acceptNextStep((Integer)arg);
		}

		public void acceptNextStep(Integer choice) {
			BuildHelper.handleStartDialogResult(this.buildHelper, choice);
		}

		@Override
		public void accept() {
			this.buildHelper.restoreGui();
		}
	}

	public static class OpenClassesListener extends OptionalConsumer {
		public final BuildHelper buildHelper;

		public OpenClassesListener(BuildHelper buildHelper) {
			this.buildHelper = buildHelper;
		}

		public void acceptPrevious(
			ClasspathResource[] classes, ArchiveNode[] archiveNodes, ClasspathResource[] jadResources, ClasspathResource[] xmlResources, ClasspathResource[] selectedResources, AtomicReference<String> runningPathRef
		) {
			BuildHelper.handleOpenClassesResult(
				this.buildHelper,
				classes,
				(ArchiveNode[])null,
				(ClasspathResource[])null,
				(ClasspathResource[])null,
				(ClasspathResource[])null,
				runningPathRef,
				this.buildHelper.uiConfig.getOpenNestedArchives(),
				null,
				2
			);
		}

		@Override
		public void accept(Object classes, Object archiveNodes, Object jadResources, Object xmlResources, Object selectedResources, Object runningPathRef) {
			this.acceptPrevious(
				(ClasspathResource[])classes,
				(ArchiveNode[])archiveNodes,
				(ClasspathResource[])jadResources,
				(ClasspathResource[])xmlResources,
				(ClasspathResource[])selectedResources,
				(AtomicReference<String>)runningPathRef
			);
		}

		@Override
		public void accept(Object classes, Object archiveNodes, Object jadResources, Object xmlResources, Object selectedResources, Object runningPathRef, Object openNestedArchives, Object archiveNodeSet) {
			this.acceptOpenClasses(
				(ClasspathResource[])classes,
				(ArchiveNode[])archiveNodes,
				(ClasspathResource[])jadResources,
				(ClasspathResource[])xmlResources,
				(ClasspathResource[])selectedResources,
				(AtomicReference<String>)runningPathRef,
				(Boolean)openNestedArchives,
				(Set<ArchiveNode>)archiveNodeSet
			);
		}

		public void acceptOpenClasses(
			ClasspathResource[] classes,
			ArchiveNode[] archiveNodes,
			ClasspathResource[] jadResources,
			ClasspathResource[] xmlResources,
			ClasspathResource[] selectedResources,
			AtomicReference<String> runningPathRef,
			Boolean openNestedArchives,
			Set<ArchiveNode> archiveNodeSet
		) {
			BuildHelper.handleOpenClassesResult(this.buildHelper, classes, archiveNodes, jadResources, xmlResources, selectedResources, runningPathRef, openNestedArchives, archiveNodeSet, 1);
		}

		@Override
		public void accept() {
			this.buildHelper.restoreGui();
		}
	}
}
