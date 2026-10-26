package com.zelix.ui.dialogs.stacktrace;

import com.zelix.HelpManager;
import com.zelix.UIConfig;
import com.zelix.klassmaster.KlassMaster;
import com.zelix.klassmaster.config.Options;
import com.zelix.ui.components.FileListView;
import com.zelix.ui.components.PlatformAdaptiveScrollPanel;
import com.zelix.ui.components.panels.BeveledPanel;
import com.zelix.ui.components.panels.FileSelectPanel;
import com.zelix.ui.dialogs.FileListDialog;
import com.zelix.ui.filefilters.AllFiles;
import com.zelix.ui.filefilters.FileFilterBase;
import com.zelix.ui.filefilters.TextFile;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.prompts.CopyablePrompt;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.function.ZKMConsumer;
import java.awt.Container;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class ChangeLogsListDialog extends FileListDialog {
	public List<String> changeLogPaths;
	public String originalChangeLogs;
	public ZKMConsumer resultConsumer;
	public UIConfig uiConfig;

	public ChangeLogsListDialog(JFrame parentFrame, String title, List<String> changeLogPaths, String originalChangeLogs, String message, String okButtonText, String okButtonToolTip, UIConfig uiConfig, ZKMConsumer resultConsumer) {
		super(parentFrame, title, changeLogPaths, message, okButtonText, okButtonToolTip);
		this.changeLogPaths = changeLogPaths;
		this.originalChangeLogs = originalChangeLogs;
		this.resultConsumer = resultConsumer;
		this.uiConfig = uiConfig;
		this.centerOnParent();
		GuiUtils.requestFocusInWindow(super.okButton);
	}

	@Override
	public final void onOk() {
		int fileCount = super.fileListModel.getSize();
		ArrayList<String> changeLogPaths = new ArrayList<>(fileCount);
		StringBuilder pathBuffer = new StringBuilder();

		for (int i = 0; i < fileCount; i++) {
			File file = super.fileListModel.getElementAt(i);
			if (!file.isAbsolute()) {
				String relativePath = file.getPath().trim();
				changeLogPaths.add(relativePath);
				pathBuffer.append(relativePath);
			} else {
				String absolutePath = file.getAbsolutePath().trim();
				changeLogPaths.add(absolutePath);
				pathBuffer.append(absolutePath);
			}

			if (i < fileCount - 1) {
				pathBuffer.append(Options.PATH_SEPARATOR);
			}
		}

		String pathsText = pathBuffer.toString();
		if (this.originalChangeLogs.equals(pathsText) || this.validateChangeLogs(pathsText, super.fileListModel.elements())) {
			super.confirmed = true;
			this.close();
			this.resultConsumer.accept(changeLogPaths, pathsText, 1);
		}
	}

	@Override
	protected void initGui(Object initArg1, Object initArg2, Object initArg3, Object initArg4, Object initArg5, Object initArg6, Object initArg7) {
		super.fileMap = GlobalUtils.newHashMap();
		List<String> changeLogPaths = (List<String>)initArg1;
		String message = (String)initArg2;
		String okButtonText = (String)initArg3;
		String okButtonToolTip = (String)initArg4;
		Container container = this.getContentPane();
		PowerLayout rootLayout = new PowerLayout(container);
		container.setLayout(rootLayout);
		BeveledPanel listPanel = new BeveledPanel(false, true, 5, 5);
		PowerLayout listLayout = new PowerLayout(listPanel);
		listPanel.setLayout(listLayout);
		super.messageLabel = new JLabel(message);
		listPanel.add(super.messageLabel, "msgLbl");
		super.fileListView = new FileListView<>(new DefaultListModel<>());
		super.fileListModel = (DefaultListModel<File>)super.fileListView.getModel();
		if (changeLogPaths != null) {
			for (String changeLogPath : changeLogPaths) {
				this.addFile(changeLogPath);
			}
		}

		listPanel.add(new PlatformAdaptiveScrollPanel(super.fileListView), "pathLst");
		super.fileListView.setSelectionMode(2);
		super.addButton = new JButton("Add");
		super.addButton.setToolTipText(HelpManager.getToolTip("ADD_CHANGELOG"));
		listPanel.add(super.addButton, "browseBtn");
		super.removeButton = new JButton("Remove");
		super.removeButton.setToolTipText(HelpManager.getToolTip("REMOVE_CHANGELOG"));
		listPanel.add(super.removeButton, "removeBtn");
		super.upButton = new JButton("Move Up");
		super.upButton.setToolTipText(HelpManager.getToolTip("MOVE_UP_IN_LIST"));
		listPanel.add(super.upButton, "upBtn");
		super.downButton = new JButton("Move Down");
		super.downButton.setToolTipText(HelpManager.getToolTip("MOVE_DOWN_IN_LIST"));
		listPanel.add(super.downButton, "downBtn");
		listLayout.setProperties(FileListDialog.listPanelProperties);
		container.add(listPanel, "listPnl");
		super.fileListView.addListSelectionListener(this);
		super.fileListView.addKeyListener(this);
		super.removeButton.addActionListener(this);
		super.upButton.addActionListener(this);
		super.downButton.addActionListener(this);
		super.addButton.addActionListener(this);
		super.removeButton.addKeyListener(this);
		super.upButton.addKeyListener(this);
		super.downButton.addKeyListener(this);
		super.addButton.addKeyListener(this);
		this.updateButtonStates();
		StringBuffer layoutProperties = new StringBuffer(
			"layout.minWidth=btnWidth*6+25;layout.minHeight=250;listPnl.top=5;listPnl.left=5;listPnl.right=container.right-5;listPnl.bottom=container.bottom-btnHeight-20;"
		);
		this.createButtons(okButtonText, okButtonToolTip, layoutProperties, container);
		rootLayout.applyStyles(layoutProperties.toString());
		this.setIconImage(KlassMaster.getIcon(this));
		GuiUtils.packWindow(this);
	}

	@Override
	public void browseFiles() {
		File initialDirectory = null;
		if (this.uiConfig.getLastChangeLogs() != null) {
			File lastChangeLogsDirectory = new File(this.uiConfig.getLastChangeLogs());
			if (lastChangeLogsDirectory.exists() && lastChangeLogsDirectory.isDirectory()) {
				initialDirectory = lastChangeLogsDirectory;
			}
		}

		FileFilterBase[] fileFilters = new FileFilterBase[]{new TextFile(), new AllFiles()};
		FileSelectPanel fileSelectPanel = new FileSelectPanel(initialDirectory, true, 1, fileFilters, 0, false);
		if (fileSelectPanel.showOpenDialog(this, "Select Change Log") == 1) {
			File[] selectedFiles = fileSelectPanel.getFiles();

			for (int i = 0; i < selectedFiles.length; i++) {
				File file = selectedFiles[i];
				String absolutePath = file.getAbsolutePath();
				if (!file.exists()) {
					new CopyablePrompt(this, "File Error", "'" + absolutePath + "' does not exist");
					break;
				}

				if (this.addFile(absolutePath)) {
					this.updateButtonStates();
				}

				String parentDirectory = file.getParent();
				if (parentDirectory != null) {
					this.uiConfig.setLastChangeLogs(parentDirectory);
					this.uiConfig.saveConfig();
				}
			}
		}
	}

	@Override
	protected void openHelpGUI() {
		HelpManager.showHelp("039");
	}

	public boolean validateChangeLogs(String changeLogs, Enumeration<File> files) {
		return true;
	}

	@Override
	protected void onConfirm() {
		super.confirmed = true;
		this.close();
		this.resultConsumer.accept();
	}

	@Override
	public void close() {
		super.close();
		if (!super.confirmed) {
			this.resultConsumer.accept();
		}
	}
}
