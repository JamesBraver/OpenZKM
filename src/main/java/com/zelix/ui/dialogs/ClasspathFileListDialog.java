package com.zelix.ui.dialogs;

import com.zelix.HelpManager;
import com.zelix.UIConfig;
import com.zelix.klassmaster.KlassMaster;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.logging.AbstractLogger;
import com.zelix.repository.ClasspathManager;
import com.zelix.ui.components.FileListView;
import com.zelix.ui.components.PlatformAdaptiveScrollPanel;
import com.zelix.ui.components.panels.BeveledPanel;
import com.zelix.ui.components.panels.FileSelectPanel;
import com.zelix.ui.filefilters.FileFilterBase;
import com.zelix.ui.filefilters.FoldersAndArchives;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.prompts.CopyablePrompt;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.function.ZKMConsumer;
import com.zelix.utils.references.AtomicReference;
import java.awt.Container;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class ClasspathFileListDialog extends FileListDialog {
	private File currentDirectory = new File(SystemConstants.userDirectory);
	public ClasspathManager classpathManager;
	public ZKMConsumer resultConsumer;
	public UIConfig uiConfig;

	public ClasspathFileListDialog(JFrame parentFrame, String title, ClasspathManager classpathManager, String message, String okButtonText, String okButtonToolTip, UIConfig uiConfig, ZKMConsumer resultConsumer) {
		super(parentFrame, title, classpathManager.getClasspathElements(), message, okButtonText, okButtonToolTip);
		this.classpathManager = classpathManager;
		this.resultConsumer = resultConsumer;
		this.uiConfig = uiConfig;
		this.centerOnParent();
		GuiUtils.requestFocusInWindow(super.okButton);
	}

	@Override
	public final void onOk() throws IOException {
		int entryCount = super.fileListModel.getSize();
		StringBuilder classpathBuffer = new StringBuilder();

		for (int i = 0; i < entryCount; i++) {
			File entry = super.fileListModel.getElementAt(i);
			if (!entry.isAbsolute()) {
				classpathBuffer.append(entry.getPath().trim());
			} else {
				classpathBuffer.append(entry.getAbsolutePath().trim());
			}

			if (i < entryCount - 1) {
				classpathBuffer.append(SystemConstants.pathSeparator);
			}
		}

		String classpath = classpathBuffer.toString();
		String rawClasspath = this.classpathManager.getRawClasspath();
		if (rawClasspath.equals(classpath) || this.applyClasspath(classpath, super.fileListModel.elements())) {
			if (!rawClasspath.equals(classpath)) {
				this.saveLastClasspath(classpath);
				this.classpathManager.setClasspath(classpath);
				this.classpathManager.initializeBootClasspath(new AtomicReference<>(), new AtomicReference<>());
			}

			super.confirmed = true;
			this.close();
			this.resultConsumer.accept(this.classpathManager, 1);
		}
	}

	public boolean applyClasspath(String classpath, Enumeration<File> files) {
		boolean classpathValid = false;
		AtomicReference<String> errorMessageRef = new AtomicReference<>();
		if (!ClasspathManager.normalizeAndExpandClasspath(classpath, new File(SystemConstants.userDirectory), new AtomicReference<>(), (AbstractLogger)null, errorMessageRef)) {
			GuiUtils.createMessageDialog(this, "Invalid classpath", "Invalid classpath", errorMessageRef.get());
		} else {
			StringBuilder classpathBuffer = new StringBuilder();

			while (files.hasMoreElements()) {
				File file = files.nextElement();
				classpathBuffer.append(file.getAbsolutePath());
			}

			String rawClasspath = this.classpathManager.getRawClasspath();
			this.classpathManager.setClasspath(classpathBuffer.toString());
			if (!this.classpathManager.initializeBootClasspath(new AtomicReference<>(), new AtomicReference<>())) {
				this.classpathManager.setClasspath(rawClasspath);
				this.classpathManager.initializeBootClasspath(new AtomicReference<>(), new AtomicReference<>());
				GuiUtils.createMessageDialog(
					this, "Incomplete classpath", "Incomplete classpath", "No path to java.lang.Object. (Note that current 'java.home' is '" + SystemConstants.javaHome + "')"
				);
			}

			classpathValid = true;
		}

		return classpathValid;
	}

	public void saveLastClasspath(String classpath) {
		this.uiConfig.setLastClassPath(classpath);
		this.uiConfig.saveConfig();
	}

	@Override
	protected void initGui(Object initArg1, Object initArg2, Object initArg3, Object initArg4, Object initArg5, Object initArg6, Object initArg7) {
		super.fileMap = GlobalUtils.newHashMap();
		List<String> classpathElements = (List<String>)initArg1;
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
		if (classpathElements != null) {
			for (String classpathElement : classpathElements) {
				this.addFile(classpathElement);
			}
		}

		listPanel.add(new PlatformAdaptiveScrollPanel(super.fileListView), "pathLst");
		super.fileListView.setSelectionMode(2);
		super.addButton = new JButton("Add");
		super.addButton.setToolTipText(HelpManager.getToolTip("BROWSE_CLASSPATH"));
		listPanel.add(super.addButton, "browseBtn");
		super.removeButton = new JButton("Remove");
		super.removeButton.setToolTipText(HelpManager.getToolTip("REMOVE_FROM_CLASSPATH"));
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
		FileFilterBase[] fileFilters = new FileFilterBase[]{new FoldersAndArchives()};
		String initialDirectoryPath = this.currentDirectory.getAbsolutePath();
		FileSelectPanel fileSelectPanel = new FileSelectPanel(this.currentDirectory, true, 3, fileFilters, 0, false);
		if (fileSelectPanel.showOpenDialog(this, "Classpath") == 1) {
			if (!initialDirectoryPath.equals(fileSelectPanel.getCurrentDirectory().getAbsolutePath())) {
				this.currentDirectory = fileSelectPanel.getCurrentDirectory();
			}

			File[] selectedFiles = fileSelectPanel.getFiles();

			for (int i = 0; i < selectedFiles.length; i++) {
				File file = selectedFiles[i];
				String filePath = file.getAbsolutePath();
				if (!file.exists()) {
					new CopyablePrompt(this, "File Error", "'" + filePath + "' does not exist");
					break;
				}

				if (this.addFile(filePath)) {
					this.updateButtonStates();
				}
			}
		}
	}

	@Override
	protected void openHelpGUI() {
		HelpManager.showHelp("017");
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
