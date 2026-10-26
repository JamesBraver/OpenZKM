package com.zelix.ui.frames;

import com.zelix.HelpManager;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.klassmaster.KlassMaster;
import com.zelix.klassmaster.script.statements.command.PathConfig;
import com.zelix.klassmaster.script.statements.javaccInterface.PathFilter;
import com.zelix.logging.GuiLoggerBase;
import com.zelix.ui.base.BaseFrame;
import com.zelix.ui.components.FileListView;
import com.zelix.ui.components.PlatformAdaptiveScrollPanel;
import com.zelix.ui.components.panels.FileSelectPanel;
import com.zelix.ui.filefilters.ClassesAndDescriptor;
import com.zelix.ui.filefilters.FileFilterBase;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.prompts.CopyablePrompt;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.ArchiveUtils;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.TempFileManager;
import com.zelix.utils.function.ZKMConsumer;
import com.zelix.utils.io.ClassFilenameFilter;
import com.zelix.utils.references.AtomicReference;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class OpenClassesFrame extends BaseFrame implements ActionListener, KeyListener, PropertyChangeListener {
	public HashSet<File> selectedFiles = GlobalUtils.newHashSet();
	public final JFrame parentFrame;
	public boolean isOpenMode;
	public ZKMConsumer callback;
	public JButton addAllButton;
	public JButton cancelButton;
	public JCheckBox openNestedArchivesCheckBox;
	public ClasspathResource[] selectedResources;
	public JButton removeAllButton;
	public boolean resultDelivered;
	private FileListView<File> selectedFilesListView;
	public JPanel dirPanel;
	public FileSelectPanel fileSelectPanel;
	public JLabel selectedDirLabel;
	public JButton removeButton;
	public DefaultListModel<File> selectedFilesModel;
	public JButton okButton;
	public JButton helpButton;
	public JButton addButton;
	public ClasspathResource[] classResources;

	public OpenClassesFrame(JFrame parentFrame, String title, String okButtonText, boolean isOpenMode, String initialDirectory, boolean openNestedArchives, ClasspathResource[] initialResources, ZKMConsumer callback) {
		this.parentFrame = parentFrame;
		this.isOpenMode = isOpenMode;
		this.callback = callback;
		this.initializeUI(title, okButtonText, initialDirectory, initialResources);
		this.setAddEnabled(false);
		this.setAddAllEnabled(false);
		this.setRemoveEnabled(false);
		this.setRemoveAllEnabled(initialResources != null && initialResources.length > 0);
		this.openNestedArchivesCheckBox.setSelected(openNestedArchives);
		GuiUtils.setVisible(this, true);
		GuiUtils.requestFocusInWindow(this.okButton);
	}

	public void setDefaultCursor() {
		Cursor defaultCursor = new Cursor(0);
		this.setCursor(defaultCursor);
	}

	public void initializeUI(String title, String okButtonText, String initialDirectory, ClasspathResource[] initialResources) {
		Container contentPane = this.getContentPane();
		PowerLayout powerLayout = new PowerLayout(contentPane);
		contentPane.setLayout(powerLayout);
		this.addWindowListener(new OpenClassesFrame.WindowClosingListener(this));
		FileFilterBase[] fileFilters = new FileFilterBase[]{new ClassesAndDescriptor()};
		File startDirectory = null;
		if (initialDirectory != null) {
			File candidateDirectory = new File(initialDirectory);
			if (candidateDirectory.exists() && candidateDirectory.isDirectory()) {
				startDirectory = candidateDirectory;
			}
		}

		this.fileSelectPanel = new FileSelectPanel(startDirectory, true, 3, fileFilters, 0, false);
		contentPane.add(this.fileSelectPanel, "fileSelector");
		this.fileSelectPanel.addPropertyChangeListener(this);
		this.selectedDirLabel = new JLabel("Selected Folders and Archives");
		this.dirPanel = new JPanel();
		this.dirPanel.setLayout(new BorderLayout());
		contentPane.add(this.dirPanel, "dirPanel");
		this.selectedFilesModel = new DefaultListModel<>();
		this.selectedFiles = GlobalUtils.newHashSet();
		this.selectedFilesListView = new FileListView<>(this.selectedFilesModel);
		this.selectedFilesListView.setSelectionMode(2);
		this.selectedFilesListView.setToolTipText(HelpManager.getToolTip("OPEN_SELECTED_LIST"));
		this.dirPanel.add(new PlatformAdaptiveScrollPanel(this.selectedFilesListView), "Center");
		contentPane.add(this.selectedDirLabel, "selectedDirLbl");
		if (initialResources != null && initialResources.length > 0) {
			for (int i = 0; i < initialResources.length; i++) {
				File resourceFile = new File(initialResources[i].getResourceName());
				this.selectedFilesModel.addElement(resourceFile);
				this.selectedFiles.add(resourceFile);
			}
		}

		this.openNestedArchivesCheckBox = new JCheckBox("open nested archives", false);
		this.openNestedArchivesCheckBox.setToolTipText(HelpManager.getToolTip("OPEN_NESTED"));
		contentPane.add(this.openNestedArchivesCheckBox, "openNestedArchivesChk");
		this.selectedFilesListView.addListSelectionListener(e -> this.setRemoveEnabled(this.getSelectedCount() != 0));
		this.selectedFilesListView.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					OpenClassesFrame.this.removeSelectedFiles();
					OpenClassesFrame.this.setRemoveEnabled(false);
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
			}

			@Override
			public void mouseReleased(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
			}

			@Override
			public void mouseExited(MouseEvent e) {
			}
		});
		this.selectedFilesListView.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == 10 || e.getKeyCode() == 127) {
					OpenClassesFrame.this.removeSelectedFiles();
					OpenClassesFrame.this.setRemoveEnabled(false);
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}
		});
		this.addButton = new JButton(">");
		this.addButton.setToolTipText(HelpManager.getToolTip("OPEN_ADD_SELECTION"));
		OpenClassesFrame.AddSelectionListener addSelectionListener = new OpenClassesFrame.AddSelectionListener(this);
		this.addButton.addActionListener(addSelectionListener);
		this.addButton.addKeyListener(addSelectionListener);
		contentPane.add(this.addButton, "addBtn");
		this.addAllButton = new JButton(">>");
		this.addAllButton.setToolTipText(HelpManager.getToolTip("OPEN_ADD_SELECTION_PLUS"));
		this.addAllButton.addActionListener(addSelectionListener);
		this.addAllButton.addKeyListener(addSelectionListener);
		contentPane.add(this.addAllButton, "addPlusBtn");
		this.removeButton = new JButton("<");
		this.removeButton.setToolTipText(HelpManager.getToolTip("OPEN_REMOVE_SELECTION"));
		OpenClassesFrame.RemoveSelectionListener removeSelectionListener = new OpenClassesFrame.RemoveSelectionListener(this);
		this.removeButton.addActionListener(removeSelectionListener);
		this.removeButton.addKeyListener(removeSelectionListener);
		contentPane.add(this.removeButton, "removeBtn");
		this.removeAllButton = new JButton("<<");
		this.removeAllButton.setToolTipText(HelpManager.getToolTip("OPEN_REMOVE_SELECTION_PLUS"));
		this.removeAllButton.addActionListener(removeSelectionListener);
		this.removeAllButton.addKeyListener(removeSelectionListener);
		contentPane.add(this.removeAllButton, "removeAllBtn");
		StringBuffer layoutConstraints = new StringBuffer(
			"layout.minWidth=majorBtnWidth*4+25;layout.minHeight=300;minorBtnHeight=max(addBtn.defaultHeight, addPlusBtn.defaultHeight, removeBtn.defaultHeight, removeAllBtn.defaultHeight);minorBtnWidth=max(addBtn.defaultWidth, addPlusBtn.defaultWidth, removeBtn.defaultWidth, removeAllBtn.defaultWidth);halfMinorBtnWidth=minorBtnWidth/2;containerCenterX=container.width*50/100;fileSelector.top=1;fileSelector.left=5;fileSelector.bottom=container.bottom-majorBtnHeight-10;fileSelector.right=container.width/2 - halfMinorBtnWidth - 5;selectedDirLbl.top=1;selectedDirLbl.left=container.width/2 + halfMinorBtnWidth + 10;dirPanel.top=selectedDirLbl.bottom+2;dirPanel.left=selectedDirLbl.left;dirPanel.bottom=openNestedArchivesChk.top;dirPanel.right=container.width-5;addBtn.centerX=containerCenterX;addBtn.centerY=container.height*30/100;addBtn.width=minorBtnWidth;addBtn.height=minorBtnHeight;addPlusBtn.centerX=containerCenterX;addPlusBtn.centerY=container.height*43/100;addPlusBtn.width=minorBtnWidth;addPlusBtn.height=minorBtnHeight;removeBtn.centerX=containerCenterX;removeBtn.centerY=container.height*56/100;removeBtn.width=minorBtnWidth;removeBtn.height=minorBtnHeight;removeAllBtn.centerX=containerCenterX;removeAllBtn.centerY=container.height*69/100;removeAllBtn.width=minorBtnWidth;removeAllBtn.height=minorBtnHeight;openNestedArchivesChk.left = selectedDirLbl.left;openNestedArchivesChk.bottom = container.height-majorBtnHeight-10;"
		);
		this.createButtons(powerLayout, okButtonText, layoutConstraints, contentPane);
		powerLayout.applyStyles(layoutConstraints.toString());
		this.setTitle(title);
		this.setIconImage(KlassMaster.getIcon(this));
		this.setSize(550, 400);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension frameSize = this.getSize();
		int centeredX = screenSize.width / 2 - frameSize.width / 2;
		int centeredY = screenSize.height / 2 - frameSize.height / 2;
		this.setLocation(centeredX, centeredY);
	}

	public void setAddEnabled(boolean enabled) {
		this.addButton.setEnabled(enabled);
	}

	public void notifySelection(ArchiveNode[] archiveNodes, ClasspathResource[] jadResources, ClasspathResource[] xmlResources, Set<ArchiveNode> archiveNodeSet) {
		String currentDirectoryPath = this.fileSelectPanel.getCurrentDirectoryPath();
		this.callback.accept(this.classResources, archiveNodes, jadResources, xmlResources, this.selectedResources, new AtomicReference<>(currentDirectoryPath), this.openNestedArchivesCheckBox.isSelected(), archiveNodeSet);
	}

	public void createButtons(PowerLayout powerLayout, String okButtonText, StringBuffer layoutConstraints, Container container) {
		this.okButton = new JButton(okButtonText);
		this.okButton.setToolTipText(HelpManager.getToolTip("OK_OPEN"));
		this.okButton.addActionListener(this);
		this.okButton.addKeyListener(this);
		container.add(this.okButton, "okBtn");
		this.cancelButton = new JButton("Cancel");
		this.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		this.cancelButton.addActionListener(this);
		this.cancelButton.addKeyListener(this);
		container.add(this.cancelButton, "cancelBtn");
		this.helpButton = new JButton("Help");
		this.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
		this.helpButton.addActionListener(this);
		this.helpButton.addKeyListener(this);
		container.add(this.helpButton, "helpBtn");
		layoutConstraints.append(
			"okBtn.top=fileSelector.bottom+5;okBtn.centerX=container.width*16/100;okBtn.width=majorBtnWidth;okBtn.height=majorBtnHeight;cancelBtn.top=okBtn.top;cancelBtn.centerX=container.width*50/100;cancelBtn.width=majorBtnWidth;cancelBtn.height=majorBtnHeight;helpBtn.top=okBtn.top;helpBtn.centerX=container.width*84/100;helpBtn.width=majorBtnWidth;helpBtn.height=majorBtnHeight;majorBtnHeight=max(okBtn.defaultHeight, cancelBtn.defaultHeight, helpBtn.defaultHeight);majorBtnWidth=max(okBtn.defaultWidth, cancelBtn.defaultWidth, helpBtn.defaultWidth)"
		);
	}

	public void enableRemoveAll() {
		this.removeAllButton.setEnabled(true);
		GuiUtils.requestFocusInWindow(this.fileSelectPanel);
	}

	public void setRemoveAllEnabled(boolean enabled) {
		this.removeAllButton.setEnabled(enabled);
	}

	public void setRemoveEnabled(boolean enabled) {
		this.removeButton.setEnabled(enabled);
	}

	public final void cancel() {
		this.resultDelivered = true;
		this.close();
		this.callback.accept();
	}

	public void resetRemoveButton() {
		GuiUtils.requestFocusInWindow(this.fileSelectPanel);
		this.removeButton.setEnabled(false);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object eventSource = e.getSource();
		if (eventSource == this.okButton) {
			this.onOkPressed();
		} else if (eventSource == this.cancelButton) {
			this.cancel();
		} else if (eventSource == this.helpButton) {
			this.showHelp();
		}
	}

	public void removeSelectedFiles() {
		List<File> filesToRemove = this.selectedFilesListView.getSelectedValuesList();

		for (int i = 0; i < filesToRemove.size(); i++) {
			this.selectedFiles.remove(filesToRemove.get(i));
			this.selectedFilesModel.removeElement(filesToRemove.get(i));
		}

		if (this.selectedFilesModel.getSize() == 0) {
			this.removeAllButton.setEnabled(false);
		}
	}

	public void onOkPressed() {
		this.resultDelivered = true;
		if (this.isOpenMode) {
			this.openSelectedClasses();
		} else {
			this.confirmSelection();
		}
	}

	public void clearSelectedFiles() {
		this.selectedFilesModel = new DefaultListModel<>();
		this.selectedFiles = GlobalUtils.newHashSet();
		this.selectedFilesListView.setModel(this.selectedFilesModel);
		this.dirPanel.validate();
	}

	public void addAllFiles() {
		this.setWaitCursor();
		File[] files = this.fileSelectPanel.getFiles();

		for (int fileIndex = 0; fileIndex < files.length; fileIndex++) {
			files[fileIndex].getAbsolutePath();
			File[] recursiveFiles = this.fileSelectPanel.getFilesRecursively(files[fileIndex]);

			for (int recursiveFileIndex = 0; recursiveFileIndex < recursiveFiles.length; recursiveFileIndex++) {
				File file = recursiveFiles[recursiveFileIndex];
				if (this.selectedFiles.add(file)) {
					this.selectedFilesModel.addElement(file);
				}
			}
		}

		this.removeAllButton.setEnabled(true);
		this.setDefaultCursor();
	}

	public void enableRemoveAllForAddAll() {
		this.removeAllButton.setEnabled(true);
		this.enableRemoveAll();
	}

	protected final void showHelp() {
		HelpManager.showHelp("018");
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == 10) {
			Object eventSource = e.getSource();
			if (eventSource == this.okButton) {
				this.onOkPressed();
			} else if (eventSource == this.cancelButton) {
				this.cancel();
			} else if (eventSource == this.helpButton) {
				this.showHelp();
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	public int getSelectedCount() {
		int[] selectedIndices = this.selectedFilesListView.getSelectedIndices();
		return selectedIndices == null ? 0 : selectedIndices.length;
	}

	@Override
	public Action getAction() {
		return new OpenClassesFrame.EscapeAction(this);
	}

	public void disableRemoveButtons() {
		this.removeButton.setEnabled(false);
		this.removeAllButton.setEnabled(false);
		this.resetRemoveButton();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("selectionChanged")) {
			File[] newSelectionFiles = (File[])evt.getNewValue();
			if (newSelectionFiles != null && newSelectionFiles.length > 0) {
				this.setAddEnabled(true);
				this.setAddAllEnabled(true);
			} else {
				this.setAddEnabled(false);
				this.setAddAllEnabled(false);
			}
		} else if (evt.getPropertyName().equals("finalSelection")) {
			File finalSelectionFile = (File)evt.getNewValue();
			if (this.selectedFiles.add(finalSelectionFile)) {
				this.selectedFilesModel.addElement(finalSelectionFile);
			}

			this.setRemoveEnabled(true);
			this.setRemoveAllEnabled(true);
		}
	}

	public void addSelectedFiles() {
		File[] files = this.fileSelectPanel.getFiles();

		for (int i = 0; i < files.length; i++) {
			if (this.selectedFiles.add(files[i])) {
				this.selectedFilesModel.addElement(files[i]);
			}
		}

		this.removeAllButton.setEnabled(true);
	}

	@Override
	public final void close() {
		super.close();
		this.parentFrame.setEnabled(true);
		this.parentFrame.toFront();
		if (!this.resultDelivered) {
			this.callback.accept();
		}
	}

	public String[] getSelectedPaths() {
		int selectedCount = this.selectedFilesModel.getSize();
		String[] selectedPaths = new String[selectedCount];

		for (int i = 0; i < selectedCount; i++) {
			selectedPaths[i] = this.selectedFilesModel.getElementAt(i).getAbsolutePath();
		}

		return selectedPaths;
	}

	public void openSelectedClasses() {
		this.setWaitCursor();
		String[] selectedPaths = this.getSelectedPaths();
		HashMap<String, ArchiveNode> archiveCache = GlobalUtils.newHashMap(27);
		ArrayList<ClasspathResource> jadResourceList = new ArrayList<>();
		ArrayList<ClasspathResource> xmlResourceList = new ArrayList<>();
		LinkedHashSet<ClasspathResource> allClassResources = new LinkedHashSet<>();
		LinkedHashSet<ArchiveNode> nestedArchives = new LinkedHashSet<>(13);
		if (selectedPaths.length != 0) {
			this.selectedResources = new ClasspathResource[selectedPaths.length];

			for (int resourceIndex = 0; resourceIndex < selectedPaths.length; resourceIndex++) {
				this.selectedResources[resourceIndex] = new ClasspathResource(selectedPaths[resourceIndex]);
			}

			String fileSeparator = System.getProperty("file.separator");
			GuiLoggerBase logger = new GuiLoggerBase(this, null);

			for (int pathIndex = 0; pathIndex < selectedPaths.length; pathIndex++) {
				File selectedFile = new File(selectedPaths[pathIndex]);
				if (selectedFile.exists()) {
					if (selectedFile.isDirectory()) {
						String[] classFileNames = selectedFile.list(new ClassFilenameFilter());
						if (classFileNames != null) {
							for (int classFileIndex = 0; classFileIndex < classFileNames.length; classFileIndex++) {
								String classFilePath = selectedPaths[pathIndex] + fileSeparator + classFileNames[classFileIndex];
								allClassResources.add(new ClasspathResource(classFilePath));
							}
						}
					} else if (selectedFile.getName().endsWith(".jad")) {
						jadResourceList.add(new ClasspathResource(selectedFile));
					} else if (FileUtils.isXmlFile(selectedFile.getName())) {
						xmlResourceList.add(new ClasspathResource(selectedFile));
					} else {
						boolean copyFailed = false;
						File managedTempFile = null;

						try {
							managedTempFile = FileUtils.copyToManagedTempFile(selectedFile);
						} catch (IOException ioException) {
							copyFailed = true;
							new CopyablePrompt(this, "File Error", "Could not create temporary file in '" + TempFileManager.TEMP_DIRECTORY_PATH + "' : " + ioException + " (2)");
						}

						if (!copyFailed) {
							ArchiveUtils.processArchive(
								managedTempFile,
								allClassResources,
								archiveCache,
								new PathConfig(selectedFile.getAbsolutePath(), PathFilter.MATCH_ALL, null, null),
								this.openNestedArchivesCheckBox.isSelected(),
								new ArrayList<>(1),
								new ArrayList<>(1),
								GlobalUtils.newHashSet(7),
								GlobalUtils.newHashSet(7),
								nestedArchives,
								null,
								null,
								null,
								null,
								null,
								selectedFile.getAbsolutePath(),
								null,
								null,
								null,
								logger
							);
						}
					}
				}
			}

			if (allClassResources.size() == 0) {
				new CopyablePrompt(this, "Error", "No class files were found in the selected directories or archives");
			} else {
				ArchiveNode[] archiveNodes = new ArchiveNode[archiveCache.size()];
				Iterator<ArchiveNode> archiveIterator = archiveCache.values().iterator();

				for (int archiveIndex = 0; archiveIndex < archiveNodes.length; archiveIndex++) {
					archiveNodes[archiveIndex] = archiveIterator.next();
				}

				this.classResources = new ClasspathResource[allClassResources.size()];
				this.classResources = allClassResources.toArray(this.classResources);
				ClasspathResource[] jadResources = new ClasspathResource[jadResourceList.size()];
				jadResourceList.toArray(jadResources);
				ClasspathResource[] xmlResources = new ClasspathResource[xmlResourceList.size()];
				xmlResourceList.toArray(xmlResources);
				this.close();
				this.notifySelection(archiveNodes, jadResources, xmlResources, nestedArchives);
			}
		} else {
			new CopyablePrompt(this, "Error", "No directories or archives were selected");
		}

		this.setDefaultCursor();
	}

	public void setWaitCursor() {
		Cursor waitCursor = new Cursor(3);
		this.setCursor(waitCursor);
	}

	public void setAddAllEnabled(boolean enabled) {
		this.addAllButton.setEnabled(enabled);
	}

	public void confirmSelection() {
		String[] selectedPaths = this.getSelectedPaths();
		if (selectedPaths.length != 0) {
			this.classResources = new ClasspathResource[selectedPaths.length];

			for (int i = 0; i < selectedPaths.length; i++) {
				this.classResources[i] = new ClasspathResource(selectedPaths[i]);
			}

			this.resultDelivered = true;
			this.close();
			this.notifySelection((ArchiveNode[])null, (ClasspathResource[])null, (ClasspathResource[])null, null);
		} else {
			new CopyablePrompt(this, "Error", "No directories or archives were selected");
		}
	}

	public static class RemoveSelectionListener implements ActionListener, KeyListener {
		public final OpenClassesFrame openClassesFrame;

		public RemoveSelectionListener(OpenClassesFrame openClassesFrame) {
			this.openClassesFrame = openClassesFrame;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == this.openClassesFrame.removeButton) {
				this.openClassesFrame.removeSelectedFiles();
				this.openClassesFrame.resetRemoveButton();
			} else {
				this.openClassesFrame.clearSelectedFiles();
				this.openClassesFrame.disableRemoveButtons();
			}
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == 10) {
				if (e.getSource() == this.openClassesFrame.removeButton) {
					this.openClassesFrame.removeSelectedFiles();
					this.openClassesFrame.resetRemoveButton();
				} else {
					this.openClassesFrame.clearSelectedFiles();
					this.openClassesFrame.disableRemoveButtons();
				}
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}
	}

	public static class AddSelectionListener implements ActionListener, KeyListener {
		public final OpenClassesFrame openClassesFrame;

		public AddSelectionListener(OpenClassesFrame openClassesFrame) {
			this.openClassesFrame = openClassesFrame;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == this.openClassesFrame.addButton) {
				this.openClassesFrame.addSelectedFiles();
				this.openClassesFrame.enableRemoveAll();
			} else {
				this.openClassesFrame.addAllFiles();
				this.openClassesFrame.enableRemoveAllForAddAll();
			}
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == 10) {
				if (e.getSource() == this.openClassesFrame.addButton) {
					this.openClassesFrame.addSelectedFiles();
					this.openClassesFrame.enableRemoveAll();
				} else {
					this.openClassesFrame.addAllFiles();
					this.openClassesFrame.enableRemoveAllForAddAll();
				}
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}

		@Override
		public void keyTyped(KeyEvent e) {
		}
	}

	public static class EscapeAction extends AbstractAction {
		public final OpenClassesFrame openClassesFrame;

		public EscapeAction(OpenClassesFrame openClassesFrame) {
			this.openClassesFrame = openClassesFrame;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (this.openClassesFrame.isEnabled()) {
				this.openClassesFrame.cancel();
			}
		}
	}

	public static class WindowClosingListener extends WindowAdapter {
		public final OpenClassesFrame openClassesFrame;

		public WindowClosingListener(OpenClassesFrame openClassesFrame) {
			this.openClassesFrame = openClassesFrame;
		}

		@Override
		public void windowClosing(WindowEvent e) {
			this.openClassesFrame.close();
		}
	}
}
