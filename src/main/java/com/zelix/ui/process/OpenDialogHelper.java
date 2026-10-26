package com.zelix.ui.process;

import com.zelix.HelpManager;
import com.zelix.UIConfig;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.repository.ClassLoadingStatus;
import com.zelix.repository.ClasspathManager;
import com.zelix.ui.GUI;
import com.zelix.ui.dialogs.ClasspathFileListDialog;
import com.zelix.ui.frames.WrappedOpenClassesFrame;
import com.zelix.utils.function.OptionalConsumer;
import com.zelix.utils.references.AtomicReference;
import java.util.Set;

public class OpenDialogHelper {
	public static String dialogTitle = "Open Dialog Helper";
	private GUI gui;
	private UIConfig uiConfig;
	private ClasspathResource[] openResources;
	private boolean openResourcesSelected;

	public OpenDialogHelper(GUI gui, UIConfig uiConfig) {
		this.gui = gui;
		this.uiConfig = uiConfig;
		gui.setEnabled(false);
		gui.setDisable(true);
		this.showClasspathDialog(gui.getClasspathManager());
	}

	private void handleClasspathResult(ClasspathManager classpathManager, Integer choice) {
		if (choice == 1) {
			if (this.openResources == null) {
				this.openClasses();
			} else {
				this.showOpenClassesFrame(this.openResources);
			}
		}
	}

	public void restoreGui() {
		this.gui.setEnabled(true);
		this.gui.toFront();
		this.gui.setDisable(false);
	}

	private void showClasspathDialog(ClasspathManager classpathManager) {
		OpenDialogHelper.ClasspathFileListListener classpathFileListListener = new OpenDialogHelper.ClasspathFileListListener(this);
		new ClasspathFileListDialog(
			this.gui, dialogTitle + " - Classpath", classpathManager, HelpManager.getToolTip("CLASSPATH_MSG"), "Next", HelpManager.getToolTip("OK_PROCEED"), this.uiConfig, classpathFileListListener
		);
	}

	private void showOpenClassesFrame(ClasspathResource[] openResources) {
		OpenDialogHelper.OpenClassesFrameListener openClassesFrameListener = new OpenDialogHelper.OpenClassesFrameListener(this);
		this.gui.setEnabled(false);
		new WrappedOpenClassesFrame(this.gui, dialogTitle + " - Open Classes", "OK", true, this.uiConfig.getLastRunningPath(), this.uiConfig.getOpenNestedArchives(), null, openClassesFrameListener);
	}

	private void openClasses() {
		this.showOpenClassesFrame(null);
	}

	private void showGuiClasspathDialog() {
		this.showClasspathDialog(this.gui.getClasspathManager());
	}

	private void handleOpenClassesResult(
		ClasspathResource[] classes,
		ArchiveNode[] archiveNodes,
		ClasspathResource[] jadFiles,
		ClasspathResource[] allClasspathResources,
		AtomicReference<String> runningPath,
		Boolean openNestedArchives,
		Set<ArchiveNode> nestedArchives,
		int step
	) {
		if (classes != null) {
			this.openResources = classes;
			this.openResourcesSelected = true;
		}

		String lastRunningPath = runningPath.get();
		if (lastRunningPath != null) {
			this.uiConfig.setLastRunningPath(lastRunningPath);
		}

		if (step == 1) {
			this.gui.onClassesUpdate(this.openResources, archiveNodes, jadFiles, allClasspathResources, runningPath, openNestedArchives, nestedArchives, (ClassLoadingStatus)null);
		} else {
			ClasspathManager classpathManager = this.gui.getClasspathManager();
			if (classpathManager == null) {
				this.showGuiClasspathDialog();
			} else {
				this.showClasspathDialog(classpathManager);
			}
		}
	}

	public static void handleClasspathResult(OpenDialogHelper openDialogHelper, ClasspathManager classpathManager, Integer choice) {
		openDialogHelper.handleClasspathResult(classpathManager, choice);
	}

	public static UIConfig getUiConfig(OpenDialogHelper openDialogHelper) {
		return openDialogHelper.uiConfig;
	}

	public static void handleOpenClassesResult(
		OpenDialogHelper openDialogHelper,
		ClasspathResource[] classes,
		ArchiveNode[] archiveNodes,
		ClasspathResource[] jadFiles,
		ClasspathResource[] allClasspathResources,
		AtomicReference<String> runningPath,
		Boolean openNestedArchives,
		Set<ArchiveNode> nestedArchives,
		int step
	) {
		openDialogHelper.handleOpenClassesResult(classes, archiveNodes, jadFiles, allClasspathResources, runningPath, openNestedArchives, nestedArchives, step);
	}

	public static class ClasspathFileListListener extends OptionalConsumer {
		public final OpenDialogHelper openDialogHelper;

		public ClasspathFileListListener(OpenDialogHelper openDialogHelper) {
			this.openDialogHelper = openDialogHelper;
		}

		@Override
		public void accept(Object arg, Object choice) {
			this.acceptClasspath((ClasspathManager)arg, (Integer)choice);
		}

		public void acceptClasspath(ClasspathManager classpathManager, Integer step) {
			OpenDialogHelper.handleClasspathResult(this.openDialogHelper, classpathManager, step);
		}

		@Override
		public void accept() {
			this.openDialogHelper.restoreGui();
		}
	}

	public static class OpenClassesFrameListener extends OptionalConsumer {
		public final OpenDialogHelper openDialogHelper;

		public OpenClassesFrameListener(OpenDialogHelper openDialogHelper) {
			this.openDialogHelper = openDialogHelper;
		}

		public void acceptPrevious(
			ClasspathResource[] classResources, ArchiveNode[] archiveNodes, ClasspathResource[] jadResources, ClasspathResource[] xmlResources, ClasspathResource[] selectedFileResources, AtomicReference<String> runningPathRef
		) {
			OpenDialogHelper.handleOpenClassesResult(this.openDialogHelper, classResources, null, null, null, runningPathRef, OpenDialogHelper.getUiConfig(this.openDialogHelper).getOpenNestedArchives(), null, 2);
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
			ClasspathResource[] classResources,
			ArchiveNode[] archiveNodes,
			ClasspathResource[] jadResources,
			ClasspathResource[] xmlResources,
			ClasspathResource[] selectedFileResources,
			AtomicReference<String> runningPathRef,
			Boolean openNestedArchives,
			Set<ArchiveNode> archiveNodeSet
		) {
			OpenDialogHelper.handleOpenClassesResult(this.openDialogHelper, classResources, archiveNodes, jadResources, xmlResources, runningPathRef, openNestedArchives, archiveNodeSet, 1);
		}

		@Override
		public void accept() {
			this.openDialogHelper.restoreGui();
		}
	}
}
