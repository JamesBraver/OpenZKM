package com.zelix.ui.process;

import com.zelix.HelpManager;
import com.zelix.UIConfig;
import com.zelix.exceptions.ClasspathException;
import com.zelix.exceptions.FileException;
import com.zelix.exceptions.ProcessingException;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.framework.wrappers.LibraryClassNode;
import com.zelix.klassmaster.config.*;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClasspathManager;
import com.zelix.repository.LibraryClasspathScanner;
import com.zelix.ui.GUI;
import com.zelix.ui.dataset.ClassNodeEntry;
import com.zelix.ui.dialogs.BuildHelperClasspathDialog;
import com.zelix.ui.dialogs.SaveAllDialog;
import com.zelix.ui.dialogs.script.ScriptBuildHelperDialog;
import com.zelix.ui.dialogs.script.ScriptOutputDialog;
import com.zelix.ui.frames.WrappedOpenClassesFrame;
import com.zelix.ui.frames.WrappedTrimOptionsFrame;
import com.zelix.ui.frames.tools.obfuscate.ScriptObfuscateOptionsFrame;
import com.zelix.ui.frames.tools.obfuscate.references.ObfuscateReferencesFrame;
import com.zelix.ui.frames.tools.script.ScriptObfuscateExcludeFrame;
import com.zelix.ui.frames.tools.script.ScriptTrimExcludeFrame;
import com.zelix.ui.prompts.CopyablePrompt;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.function.OptionalConsumer;
import com.zelix.utils.function.ZKMConsumer;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.PackageManager;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class ZKMScriptHelper extends ScriptWriterContext {
	private LibraryClasspathScanner classpathScanner;
	private PackageManager packageManager;
	private Vector<ClassNodeEntry> classNodeEntries;

	public ZKMScriptHelper(GUI gui, ScriptContext scriptContext, UIConfig uiConfig) {
		super(gui, scriptContext, uiConfig);
	}

	@Override
	public void restoreGui() {
		super.gui.setEnabled(true);
		super.gui.toFront();
		super.gui.setDisable(false);
	}

	private void promptCurrentTrim() {
		this.promptTrim(super.trimConfig);
	}

	private void promptOpenClasses(ClasspathResource[] openResources) {
		ZKMScriptHelper.OpenClassesListener openClassesListener = new ZKMScriptHelper.OpenClassesListener(this);
		super.gui.setEnabled(false);
		new WrappedOpenClassesFrame(
			super.gui, "ZKM Script Helper - \"open\" Statement", "Next", false, super.uiConfig.getLastRunningPath(), super.uiConfig.getOpenNestedArchives(), openResources, openClassesListener
		);
	}

	@Override
	public void showObfuscateOptions() {
		this.promptObfuscate(new ObfuscateConfig(SystemConstants.userDirectory, "ZKM_SO.ser"));
	}

	private void showScriptOutput(String scriptText) {
		ZKMScriptHelper.ScriptOutputListener scriptOutputListener = new ZKMScriptHelper.ScriptOutputListener(this);
		new ScriptOutputDialog(super.gui, "ZKM Script Helper", scriptText, super.uiConfig, scriptOutputListener);
	}

	private void handleStartDialogResult(Integer stepResult) {
		if (super.classpathManager == null) {
			this.promptCurrentClasspath();
		} else {
			this.promptClasspath(super.classpathManager);
		}
	}

	public final void promptObfuscateReferences() {
		ZKMScriptHelper.ObfuscateReferencesListener obfuscateReferencesListener = new ZKMScriptHelper.ObfuscateReferencesListener(this);
		super.referenceObfuscateConfig = ReferenceObfuscateConfig.loadFromFile(SystemConstants.userDirectory, "ZKM_OB_REF.ser");
		new ObfuscateReferencesFrame("ZKM Script Helper - \"obfuscateReferencesInclude\" Statement", super.gui, super.referenceObfuscateConfig, super.scriptContext, obfuscateReferencesListener, 3);
	}

	private void handleTrimResult(TrimConfig trimConfig, Integer stepResult) {
		int stepResultCode = stepResult;
		super.trimConfig = trimConfig;
		if (stepResultCode == 1) {
			super.trimConfig.saveToFile(SystemConstants.userDirectory, "ZKM_TO.ser");
			super.skipTrim = false;
			this.promptExclude();
		} else if (stepResultCode == 2) {
			this.startClassScan();
		} else if (stepResultCode == 4) {
			super.skipTrim = true;
			this.promptExclude();
		}
	}

	@Override
	public void promptOutputArchivePath(String defaultPath) {
		ZKMScriptHelper.SaveScriptListener saveScriptListener = new ZKMScriptHelper.SaveScriptListener(this);
		new SaveAllDialog(super.gui, "ZKM Script Helper - \"saveAll\" Statement", "Next", HelpManager.getToolTip("OK_PROCEED"), defaultPath, saveScriptListener);
	}

	private void promptExclude() {
		if (super.excludeConfig == null) {
			super.excludeConfig = FilterConfig.loadFromFile(SystemConstants.userDirectory, "ZKM_EX.ser");
		}

		ZKMScriptHelper.ObfuscateExcludeListener obfuscateExcludeListener = new ZKMScriptHelper.ObfuscateExcludeListener(this);
		new ScriptObfuscateExcludeFrame("ZKM Script Helper - \"exclude\" Statement", super.gui, super.excludeConfig, this.packageManager, this.classNodeEntries, super.scriptContext, obfuscateExcludeListener);
	}

	private void handleObfuscateResult(ObfuscateConfig obfuscateConfig, Integer stepResult) {
		int stepResultCode = stepResult;
		super.obfuscateConfig = obfuscateConfig;
		if (stepResultCode == 1) {
			super.obfuscateConfig.saveToFile(SystemConstants.userDirectory, "ZKM_SO.ser");
			if (super.obfuscateConfig.obfuscateReferences == 1) {
				this.promptObfuscateReferences();
			} else {
				super.referenceObfuscateConfig = null;
				if (super.outputArchivePath == null) {
					this.promptOutputArchiveLastPath();
				} else {
					this.promptOutputArchivePath(super.outputArchivePath);
				}
			}
		} else if (stepResultCode == 2) {
			this.promptExclude();
		}
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

	private void promptOpenClassesNoDefaults() {
		this.promptOpenClasses(null);
	}

	private void promptCurrentClasspath() {
		this.promptClasspath(super.gui.getClasspathManager());
	}

	private void handleScriptOutputResult(Integer stepResult) {
		if (stepResult == 2) {
			this.promptOutputArchivePath(super.outputArchivePath);
		}
	}

	private void promptTrim(TrimConfig trimConfig) {
		ZKMScriptHelper.ScriptTrimListener scriptTrimListener = new ZKMScriptHelper.ScriptTrimListener(this);
		new WrappedTrimOptionsFrame("ZKM Script Helper - \"trim\" Statement", "Next", HelpManager.getToolTip("OK_PROCEED"), super.gui, trimConfig, scriptTrimListener);
	}

	private void startClassScan() {
		if (super.trimExcludeConfig == null) {
			super.trimExcludeConfig = RuleFilterConfig.loadFromFile(SystemConstants.userDirectory, "ZKM_TEX.ser");
		}

		ZKMScriptHelper.TrimExcludeListener trimExcludeListener = new ZKMScriptHelper.TrimExcludeListener(this);
		ZKMScriptHelper.TrimExcludeFrameListener trimExcludeFrameListener = new ZKMScriptHelper.TrimExcludeFrameListener(this);
		ZKMScriptHelper.MainClassesTask mainClassesTask = new ZKMScriptHelper.MainClassesTask(this, trimExcludeFrameListener, trimExcludeListener);
		new Thread(mainClassesTask).start();
	}

	private void promptObfuscate(ObfuscateConfig obfuscateConfig) {
		ZKMScriptHelper.ObfuscateOptionsListener obfuscateOptionsListener = new ZKMScriptHelper.ObfuscateOptionsListener(this);
		new ScriptObfuscateOptionsFrame(
			"ZKM Script Helper - \"obfuscate\" Statement", HelpManager.getToolTip("OK_PROCEED"), super.gui, obfuscateConfig, super.excludeConfig, super.scriptContext, obfuscateOptionsListener
		);
	}

	private void handleClasspathResult(ClasspathManager classpathManager, Integer stepResult) {
		super.classpathManager = classpathManager;
		if (stepResult == 1) {
			if (super.openResources == null) {
				this.promptOpenClassesNoDefaults();
			} else {
				this.promptOpenClasses(super.openResources);
			}
		} else {
			this.showStartDialog();
		}
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
			if (super.openResources == null) {
				this.promptOpenClassesNoDefaults();
			} else {
				this.promptOpenClasses(super.openResources);
			}
		} else if (stepResultCode == 4) {
			super.skipTrimExclude = true;
			if (super.trimConfig == null) {
				this.promptCurrentTrim();
			} else {
				this.promptTrim(super.trimConfig);
			}
		}
	}

	@Override
	public void promptOutputArchiveLastPath() {
		this.promptOutputArchivePath(super.uiConfig.getLastSavingPath());
	}

	@Override
	public void showStartDialog() {
		ZKMScriptHelper.StartDialogListener startDialogListener = new ZKMScriptHelper.StartDialogListener(this);
		new ScriptBuildHelperDialog(super.gui, "ZKM Script Helper", "Next", "Cancel", "001", "011", 500, 400, startDialogListener);
	}

	private void handleSaveScriptResult(File scriptFile, Integer stepResult) {
		if (scriptFile != null) {
			super.outputArchivePath = scriptFile.getAbsolutePath();
		}

		int stepResultCode = stepResult;
		if (stepResultCode == 1) {
			if (super.outputArchivePath != null) {
				super.uiConfig.setLastSavingPath(super.outputArchivePath);
			}

			super.uiConfig.saveConfig();
			this.showScriptOutput(this.generateScript("ZKM Script Helper", super.openResources));
		} else if (stepResultCode == 2) {
			if (super.obfuscateConfig.obfuscateReferences == 1) {
				this.promptObfuscateReferences();
			} else {
				this.promptObfuscate(super.obfuscateConfig);
			}
		}
	}

	private void promptClasspath(ClasspathManager classpathManager) {
		ZKMScriptHelper.ClasspathDialogListener classpathDialogListener = new ZKMScriptHelper.ClasspathDialogListener(this);
		new BuildHelperClasspathDialog(
			super.gui,
			"ZKM Script Helper - \"classpath\" Statement",
			classpathManager,
			HelpManager.getToolTip("CLASSPATH_MSG"),
			"Next",
			HelpManager.getToolTip("OK_PROCEED"),
			super.uiConfig,
			classpathDialogListener
		);
	}

	public LibraryClasspathScanner getClasspathScanner(ClasspathResource[] openResources) throws IOException {
		if (super.openResourcesChanged) {
			File[] resourceFiles = new File[openResources.length];

			for (int i = 0; i < openResources.length; i++) {
				resourceFiles[i] = openResources[i].getAsFile();
			}

			this.classpathScanner = new LibraryClasspathScanner(resourceFiles);
			super.openResourcesChanged = false;
		}

		return this.classpathScanner;
	}

	private void handleOpenClassesResult(ClasspathResource[] openResources, AtomicReference<String> currentDirectoryRef, Boolean openNestedArchives, int stepResult) {
		if (openResources != null) {
			super.openResources = openResources;
			super.openResourcesChanged = true;
		}

		String lastRunningPath = currentDirectoryRef.get();
		if (lastRunningPath != null) {
			super.uiConfig.setLastRunningPath(lastRunningPath);
		}

		if (stepResult == 1) {
			super.uiConfig.setOpenNestedArchives(openNestedArchives);
			super.uiConfig.saveConfig();
			this.startClassScan();
		} else if (super.classpathManager == null) {
			this.promptCurrentClasspath();
		} else {
			this.promptClasspath(super.classpathManager);
		}
	}

	private void promptTrimExclude(ZKMConsumer resultConsumer) {
		new ScriptTrimExcludeFrame("ZKM Script Helper - \"trimExclude\" Statement", super.gui, this.classNodeEntries, super.trimExcludeConfig, super.gui.getWorkspace(), super.trimConfig, super.scriptContext, resultConsumer);
	}

	public static void handleSaveScriptResult(ZKMScriptHelper zKMScriptHelper, File scriptFile, Integer stepResult) {
		zKMScriptHelper.handleSaveScriptResult(scriptFile, stepResult);
	}

	public static void handleTrimResult(ZKMScriptHelper zKMScriptHelper, TrimConfig trimConfig, Integer stepResult) {
		zKMScriptHelper.handleTrimResult(trimConfig, stepResult);
	}

	public static void handleExcludeResult(ZKMScriptHelper zKMScriptHelper, Integer stepResult) {
		zKMScriptHelper.handleExcludeResult(stepResult);
	}

	public static void handleOpenClassesResult(
		ZKMScriptHelper zKMScriptHelper, ClasspathResource[] openResources, AtomicReference<String> currentDirectoryRef, Boolean openNestedArchives, int stepResult
	) {
		zKMScriptHelper.handleOpenClassesResult(openResources, currentDirectoryRef, openNestedArchives, stepResult);
	}

	public static Vector<ClassNodeEntry> setClassNodeEntries(ZKMScriptHelper zKMScriptHelper, Vector<ClassNodeEntry> classNodeEntries) {
		zKMScriptHelper.classNodeEntries = classNodeEntries;
		return zKMScriptHelper.classNodeEntries;
	}

	public static void promptTrimExclude(ZKMScriptHelper zKMScriptHelper, ZKMConsumer resultConsumer) {
		zKMScriptHelper.promptTrimExclude(resultConsumer);
	}

	public static PackageManager setPackageManager(ZKMScriptHelper zKMScriptHelper, PackageManager packageManager) {
		zKMScriptHelper.packageManager = packageManager;
		return zKMScriptHelper.packageManager;
	}

	public static void handleScriptOutputResult(ZKMScriptHelper zKMScriptHelper, Integer stepResult) {
		zKMScriptHelper.handleScriptOutputResult(stepResult);
	}

	public static void handleObfuscateResult(ZKMScriptHelper zKMScriptHelper, ObfuscateConfig obfuscateConfig, Integer stepResult) {
		zKMScriptHelper.handleObfuscateResult(obfuscateConfig, stepResult);
	}

	public static void handleTrimExcludeResult(ZKMScriptHelper zKMScriptHelper, Integer stepResult) {
		zKMScriptHelper.handleTrimExcludeResult(stepResult);
	}

	public static void handleClasspathResult(ZKMScriptHelper zKMScriptHelper, ClasspathManager classpathManager, Integer stepResult) {
		zKMScriptHelper.handleClasspathResult(classpathManager, stepResult);
	}

	public static void handleStartDialogResult(ZKMScriptHelper zKMScriptHelper, Integer stepResult) {
		zKMScriptHelper.handleStartDialogResult(stepResult);
	}

	public static class ScriptTrimListener extends OptionalConsumer {
		public final ZKMScriptHelper zKMScriptHelper;

		public ScriptTrimListener(ZKMScriptHelper zKMScriptHelper) {
			this.zKMScriptHelper = zKMScriptHelper;
		}

		@Override
		public void accept(Object arg, Object choice) {
			this.acceptTrimConfig((TrimConfig)arg, (Integer)choice);
		}

		public void acceptTrimConfig(TrimConfig trimConfig, Integer step) {
			ZKMScriptHelper.handleTrimResult(this.zKMScriptHelper, trimConfig, step);
		}

		@Override
		public void accept() {
			this.zKMScriptHelper.restoreGui();
		}
	}

	public static class ObfuscateReferencesListener extends OptionalConsumer {
		public final ZKMScriptHelper zKMScriptHelper;

		public ObfuscateReferencesListener(ZKMScriptHelper zKMScriptHelper) {
			this.zKMScriptHelper = zKMScriptHelper;
		}

		@SneakyThrows
		public void acceptStep(Integer step) {
			this.zKMScriptHelper.handleStepResult(step);
		}

		@Override
		public void accept(Object arg) {
			this.acceptStep((Integer)arg);
		}

		@Override
		public void accept() {
			this.zKMScriptHelper.restoreGui();
		}
	}

	public static class OpenClassesListener extends OptionalConsumer {
		public final ZKMScriptHelper zKMScriptHelper;

		public OpenClassesListener(ZKMScriptHelper zKMScriptHelper) {
			this.zKMScriptHelper = zKMScriptHelper;
		}

		public void acceptPreviousStep(
			ClasspathResource[] classResources, ArchiveNode[] archiveNodes, ClasspathResource[] jadResources, ClasspathResource[] xmlResources, ClasspathResource[] selectedFileResources, AtomicReference<String> runningPathRef
		) {
			ZKMScriptHelper.handleOpenClassesResult(this.zKMScriptHelper, classResources, runningPathRef, this.zKMScriptHelper.uiConfig.getOpenNestedArchives(), 2);
		}

		@Override
		public void accept(Object classes, Object archiveNodes, Object jadResources, Object xmlResources, Object selectedResources, Object runningPathRef) {
			this.acceptPreviousStep(
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
			this.acceptNextStep(
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

		public void acceptNextStep(
			ClasspathResource[] classResources,
			ArchiveNode[] archiveNodes,
			ClasspathResource[] jadResources,
			ClasspathResource[] xmlResources,
			ClasspathResource[] selectedFileResources,
			AtomicReference<String> runningPathRef,
			Boolean openNestedArchives,
			Set<ArchiveNode> archiveNodeSet
		) {
			ZKMScriptHelper.handleOpenClassesResult(this.zKMScriptHelper, classResources, runningPathRef, openNestedArchives, 1);
		}

		@Override
		public void accept() {
			this.zKMScriptHelper.restoreGui();
		}
	}

	public static class ScriptOutputListener extends OptionalConsumer {
		public final ZKMScriptHelper zKMScriptHelper;

		public ScriptOutputListener(ZKMScriptHelper zKMScriptHelper) {
			this.zKMScriptHelper = zKMScriptHelper;
		}

		public void acceptStep(Integer step) {
			ZKMScriptHelper.handleScriptOutputResult(this.zKMScriptHelper, step);
		}

		@Override
		public void accept(Object arg) {
			this.acceptStep((Integer)arg);
		}

		@Override
		public void accept() {
			this.zKMScriptHelper.restoreGui();
		}
	}

	public static class ClasspathDialogListener extends OptionalConsumer {
		public final ZKMScriptHelper zKMScriptHelper;

		public ClasspathDialogListener(ZKMScriptHelper zKMScriptHelper) {
			this.zKMScriptHelper = zKMScriptHelper;
		}

		@Override
		public void accept(Object arg, Object choice) {
			this.acceptClasspathManager((ClasspathManager)arg, (Integer)choice);
		}

		public void acceptClasspathManager(ClasspathManager classpathManager, Integer step) {
			ZKMScriptHelper.handleClasspathResult(this.zKMScriptHelper, classpathManager, step);
		}

		@Override
		public void accept() {
			this.zKMScriptHelper.restoreGui();
		}
	}

	public static class SaveScriptListener extends OptionalConsumer {
		public final ZKMScriptHelper zKMScriptHelper;

		public SaveScriptListener(ZKMScriptHelper zKMScriptHelper) {
			this.zKMScriptHelper = zKMScriptHelper;
		}

		@Override
		public void accept(Object arg, Object choice) {
			this.acceptScriptFile((File)arg, (Integer)choice);
		}

		public void acceptScriptFile(File scriptFile, Integer step) {
			ZKMScriptHelper.handleSaveScriptResult(this.zKMScriptHelper, scriptFile, step);
		}

		@Override
		public void accept() {
			this.zKMScriptHelper.restoreGui();
		}
	}

	public static class ObfuscateOptionsListener extends OptionalConsumer {
		public final ZKMScriptHelper zKMScriptHelper;

		public ObfuscateOptionsListener(ZKMScriptHelper zKMScriptHelper) {
			this.zKMScriptHelper = zKMScriptHelper;
		}

		@Override
		public void accept(Object arg, Object choice) {
			this.acceptObfuscateConfig((ObfuscateConfig)arg, (Integer)choice);
		}

		public void acceptObfuscateConfig(ObfuscateConfig obfuscateConfig, Integer step) {
			ZKMScriptHelper.handleObfuscateResult(this.zKMScriptHelper, obfuscateConfig, step);
		}

		@Override
		public void accept() {
			this.zKMScriptHelper.restoreGui();
		}
	}

	public static class ObfuscateExcludeListener extends OptionalConsumer {
		public final ZKMScriptHelper zKMScriptHelper;

		public ObfuscateExcludeListener(ZKMScriptHelper zKMScriptHelper) {
			this.zKMScriptHelper = zKMScriptHelper;
		}

		public void acceptStep(Integer step) {
			ZKMScriptHelper.handleExcludeResult(this.zKMScriptHelper, step);
		}

		@Override
		public void accept(Object arg) {
			this.acceptStep((Integer)arg);
		}

		@Override
		public void accept() {
			this.zKMScriptHelper.restoreGui();
		}
	}

	public static class TrimExcludeListener extends OptionalConsumer {
		public final ZKMScriptHelper zKMScriptHelper;

		public TrimExcludeListener(ZKMScriptHelper zKMScriptHelper) {
			this.zKMScriptHelper = zKMScriptHelper;
		}

		public void acceptStep(Integer step) {
			ZKMScriptHelper.handleTrimExcludeResult(this.zKMScriptHelper, step);
		}

		@Override
		public void accept(Object arg) {
			this.acceptStep((Integer)arg);
		}

		@Override
		public void accept() {
			this.zKMScriptHelper.restoreGui();
		}
	}

	public static class TrimExcludeFrameListener extends OptionalConsumer {
		public final ZKMScriptHelper zKMScriptHelper;

		public TrimExcludeFrameListener(ZKMScriptHelper zKMScriptHelper) {
			this.zKMScriptHelper = zKMScriptHelper;
		}

		@Override
		public void accept(Object arg) {
			this.acceptResultConsumer((ZKMConsumer)arg);
		}

		public void acceptResultConsumer(ZKMConsumer resultConsumer) {
			ZKMScriptHelper.promptTrimExclude(this.zKMScriptHelper, resultConsumer);
		}
	}

	public static class StartDialogListener extends OptionalConsumer {
		public final ZKMScriptHelper zKMScriptHelper;

		public StartDialogListener(ZKMScriptHelper zKMScriptHelper) {
			this.zKMScriptHelper = zKMScriptHelper;
		}

		public void acceptStep(Integer step) {
			ZKMScriptHelper.handleStartDialogResult(this.zKMScriptHelper, step);
		}

		@Override
		public void accept(Object arg) {
			this.acceptStep((Integer)arg);
		}

		@Override
		public void accept() {
			this.zKMScriptHelper.restoreGui();
		}
	}

	public static class MainClassesTask implements Runnable {
		public final ZKMScriptHelper zkmscriptHelper;
		public final ZKMConsumer completionCallback;
		public final ZKMConsumer trimExcludeListener;

		public MainClassesTask(ZKMScriptHelper zkmscriptHelper, ZKMConsumer completionCallback, ZKMConsumer trimExcludeListener) {
			this.zkmscriptHelper = zkmscriptHelper;
			this.completionCallback = completionCallback;
			this.trimExcludeListener = trimExcludeListener;
		}

		@Override
		@SneakyThrows
		public void run() {
			try {
				this.zkmscriptHelper.gui.setupWaitCursorSafety();
				LibraryClasspathScanner classpathScanner = this.zkmscriptHelper.getClasspathScanner(this.zkmscriptHelper.openResources);
				Vector<ClassNodeEntry> mainClassEntries = null;
				List<LibraryClassNode> mainMethodClasses = classpathScanner.getClassesWithMethod("main([Ljava/lang/String;)V");
				List<LibraryClassNode> appletSubclasses = classpathScanner.getDirectSubclasses("java/applet/Applet");
				if (mainMethodClasses != null && mainMethodClasses.size() > 0) {
					mainClassEntries = new Vector<>(Math.max(5, mainMethodClasses.size()));

					for (int mainClassIndex = 0; mainClassIndex < mainMethodClasses.size(); mainClassIndex++) {
						mainClassEntries.addElement(new ClassNodeEntry(mainMethodClasses.get(mainClassIndex), true));
					}
				}

				if (appletSubclasses != null && appletSubclasses.size() > 0) {
					if (mainClassEntries == null) {
						mainClassEntries = new Vector<>(appletSubclasses.size());
					}

					for (int appletIndex = 0; appletIndex < appletSubclasses.size(); appletIndex++) {
						mainClassEntries.addElement(new ClassNodeEntry(appletSubclasses.get(appletIndex), false));
					}
				}

				ZKMScriptHelper.setClassNodeEntries(this.zkmscriptHelper, mainClassEntries);
				ZKMScriptHelper.setPackageManager(this.zkmscriptHelper, classpathScanner.buildPackageManager());
				this.completionCallback.accept(this.trimExcludeListener);
			} catch (ClasspathException classpathException) {
				new CopyablePrompt(
					this.zkmscriptHelper.gui,
					"Classpath Error",
					"Error while determining classes with main methods : Class '" + GlobalUtils.internalToBinaryName(classpathException.getClassName()) + "' not found"
				);
				this.zkmscriptHelper.restoreGui();
			} catch (FileException fileException) {
				new CopyablePrompt(this.zkmscriptHelper.gui, "File Error", "Error while determining classes with main methods : " + fileException.getMessage());
				this.zkmscriptHelper.restoreGui();
			} catch (ProcessingException processingException) {
				new CopyablePrompt(this.zkmscriptHelper.gui, "File Error", "Error while determining classes with main methods : " + processingException.getMessage());
				this.zkmscriptHelper.restoreGui();
			} finally {
				this.zkmscriptHelper.gui.resetCursorSafety();
			}
		}
	}
}
