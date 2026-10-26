package com.zelix.ui.components.panels;

import com.zelix.ui.base.BasePanel;
import com.zelix.ui.base.BasePrompt;
import com.zelix.ui.components.DirectoryComboBox;
import com.zelix.ui.components.DirectoryListView;
import com.zelix.ui.components.FileSelectorComponent;
import com.zelix.ui.components.FilesDisplayField;
import com.zelix.ui.components.FilterComboBox;
import com.zelix.ui.components.PlatformAdaptiveScrollPanel;
import com.zelix.ui.components.UpLevelButton;
import com.zelix.ui.filefilters.FileFilterBase;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.prompts.FileSelectPrompt;
import com.zelix.ui.utils.FileChooserUtils;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.comparators.FileComparator;
import com.zelix.utils.io.DirectoryFilter;
import java.awt.Container;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class FileSelectPanel extends BasePanel implements FileSelectorComponent, PropertyChangeListener {
	private static File defaultDirectory = new File(System.getProperty("user.dir"));
	private static String defaultFolderName = "New Folder Name";
	public static String panelLayout = "midPoint=container.width/2;directoryComboBox.top=5;directoryComboBox.left=5;directoryComboBox.right=container.right-5;upFolderButton.top=directoryComboBox.bottom+5;upFolderButton.left=container.left+5;upFolderButton.right=midPoint-3;newFolderButton.top=directoryComboBox.bottom+5;newFolderButton.left=upFolderButton.right+5;newFolderButton.right=container.right-5;fileList.left=5;fileList.right=container.right-5;fileList.top=upFolderButton.bottom+10;fileList.bottom=container.bottom-lowerComponetsHeight;filesDisplay.top=fileList.bottom+10;filesDisplay.right=container.right-5;filterComboBox.left=5;filterComboBox.top=filesDisplay.bottom+5;filterComboBox.right=container.right-5;lowerComponetsHeight=filesDisplay.defaultHeight+filterComboBox.defaultHeight+20;";
	public static String layoutWithoutFileNameLabel = "filesDisplay.left=5";
	public static String layoutWithFileNameLabel = "filesDisplayLbl.left=5;filesDisplayLbl.centerY=filesDisplay.centerY;filesDisplay.left=filesDisplayLbl.right+1";
	public static String[] promptLayout = new String[]{
		"layout.minWidth=btnWidth*3+20",
		"btnWidth=max(approveBtn.defaultWidth,cancelBtn.defaultWidth)",
		"btnHeight=max(approveBtn.defaultHeight,cancelBtn.defaultHeight)",
		"fileSelector.top=5",
		"fileSelector.left=5",
		"fileSelector.right=container.right-5",
		"fileSelector.bottom=container.bottom-approveBtn.defaultHeight-10",
		"approveBtn.bottom=container.bottom-5",
		"approveBtn.centerX=container.width*25/100",
		"approveBtn.width=btnWidth",
		"cancelBtn.bottom=approveBtn.bottom",
		"cancelBtn.centerX=container.width*75/100",
		"cancelBtn.width=btnWidth"
	};
	public int resultCode = -1;
	private boolean multiSelection;
	private boolean fileNameInput;
	private DirectoryComboBox directoryComboBox;
	private DirectoryListView directoryListView;
	private FilesDisplayField filesDisplayField;
	private BasePrompt prompt;
	private UpLevelButton upLevelButton;
	private FileSelectPanel.NewFolderButton newFolderButton;
	private JButton cancelButton;
	private JButton approveButton;
	private FilterComboBox filterComboBox;

	public FileSelectPanel(File initialDirectory, boolean multiSelection, int selectionType, FileFilterBase[] filters, int filterIndex, boolean fileNameInput) {
		if (initialDirectory == null || !initialDirectory.isDirectory()) {
			initialDirectory = defaultDirectory;
		}

		this.multiSelection = multiSelection;
		this.fileNameInput = fileNameInput;
		this.initGui(initialDirectory, multiSelection, selectionType, filters, filterIndex, fileNameInput);
	}

	public int showOpenDialog(JFrame parent, String title) {
		return this.showFileSelectDialog(
			parent, title, "OK", this.multiSelection ? "Open selected files" : "Open selected file", "Cancel", "Cancel", "List files at this level", "Selected files"
		);
	}

	public File[] getFilesRecursively(File file) {
		ArrayList<File> files = new ArrayList<>();
		files.add(file);
		this.addFilesRecursively(file, files);
		files.size();
		Collections.sort(files, FileComparator.getInstance());
		return files.toArray(new File[files.size()]);
	}

	public void setSelectedFile(File file, boolean shouldScroll) {
		this.directoryListView.setSelectedFile(file, shouldScroll);
	}

	public File[] listSubdirectories(File directory) {
		File[] subdirectories = null;
		if (directory.isDirectory() && directory.exists()) {
			subdirectories = directory.listFiles(new DirectoryFilter());
		}

		return subdirectories;
	}

	private void addFilesRecursively(File file, ArrayList<File> files) {
		if (file.isDirectory()) {
			File[] subdirectories = this.listSubdirectories(file);
			if (subdirectories != null) {
				for (int i = 0; i < subdirectories.length; i++) {
					files.add(subdirectories[i]);
					this.addFilesRecursively(subdirectories[i], files);
				}
			}
		} else {
			files.add(file);
		}
	}

	protected void approve() {
		File[] files = this.filesDisplayField.getFiles();
		if (files != null && files.length > 0) {
			this.resultCode = 1;
		}

		this.prompt.setVisible(false);
		this.prompt.dispose();
	}

	public void goToParentDirectory() {
		this.directoryListView.goToParentDirectory();
	}

	public File getFile() {
		return this.filesDisplayField.getFile();
	}

	public int showFileSelectDialog(JFrame parent, String title, String approveText, String approveTooltip, String cancelText, String cancelTooltip, String directoryComboBoxTooltip, String filesDisplayTooltip) {
		this.prompt = new FileSelectPrompt(this, parent, title, true);
		Container contentPane = this.prompt.getContentPane();
		PowerLayout contentPaneLayout = new PowerLayout(contentPane);
		contentPane.setLayout(contentPaneLayout);
		contentPane.add(this, "fileSelector");
		this.approveButton = new JButton(approveText);
		this.approveButton.setToolTipText(approveTooltip);
		contentPane.add(this.approveButton, "approveBtn");
		this.cancelButton = new JButton(cancelText);
		this.cancelButton.setToolTipText(cancelTooltip);
		contentPane.add(this.cancelButton, "cancelBtn");
		contentPaneLayout.setProperties(promptLayout);
		FileSelectPanel.ApproveCancelListener approveCancelListener = new FileSelectPanel.ApproveCancelListener(this);
		this.approveButton.addActionListener(approveCancelListener);
		this.cancelButton.addActionListener(approveCancelListener);
		this.approveButton.addKeyListener(approveCancelListener);
		this.cancelButton.addKeyListener(approveCancelListener);
		this.directoryListView.addPropertyChangeListener(new FileSelectPanel.FinalSelectionListener(this));
		this.filesDisplayField.addPropertyChangeListener(new FileSelectPanel.FileNameChangeListener(this));
		this.directoryComboBox.setToolTipText(directoryComboBoxTooltip);
		this.filesDisplayField.setToolTipText(filesDisplayTooltip);
		GuiUtils.packWindow(this.prompt);
		this.prompt.centerOnParent();
		return this.resultCode;
	}

	public File getCurrentDirectory() {
		return this.directoryListView.getCurrentDirectory();
	}

	public void setFile(File file) {
		if (file != null) {
			this.filesDisplayField.setFile(file);
			this.filesDisplayField.selectAll();
			GuiUtils.requestFocusInWindow(this.filesDisplayField);
		}
	}

	public File[] getFiles() {
		return this.filesDisplayField.getFiles();
	}

	public String getCurrentDirectoryPath() {
		return this.getCurrentDirectory().getAbsolutePath();
	}

	protected void cancel() {
		this.resultCode = 0;
		this.prompt.setVisible(false);
		this.prompt.dispose();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("selectionChanged")) {
			this.firePropertyChange("selectionChanged", evt.getOldValue(), evt.getNewValue());
		} else if (evt.getPropertyName().equals("finalSelection")) {
			this.firePropertyChange("finalSelection", evt.getOldValue(), evt.getNewValue());
		}
	}

	public int showSaveDialog(JFrame parent, String title) {
		return this.showFileSelectDialog(parent, title, "Save", "Save file in current folder", "Cancel", "Cancel save", "Save in", null);
	}

	public void createNewFolder() {
		String folderName = JOptionPane.showInputDialog(this, defaultFolderName, defaultFolderName, 3);
		if (folderName != null && folderName.trim().length() > 0 && !this.directoryListView.createDirectory(folderName)) {
			JOptionPane.showMessageDialog(this, "'" + folderName + "' could not be created", "Error", 0);
		}
	}

	public void initGui(File initialDirectory, boolean multiSelection, int selectionType, FileFilterBase[] filters, int filterIndex, boolean fileNameInput) {
		PowerLayout mainLayout = new PowerLayout(this);
		this.setLayout(mainLayout);
		this.directoryListView = new DirectoryListView(initialDirectory, multiSelection, selectionType, filters[filterIndex]);
		this.directoryComboBox = new DirectoryComboBox(initialDirectory);
		this.upLevelButton = new UpLevelButton();
		this.newFolderButton = new FileSelectPanel.NewFolderButton();
		this.add(this.directoryComboBox, "directoryComboBox");
		this.add(this.upLevelButton, "upFolderButton");
		this.add(this.newFolderButton, "newFolderButton");
		this.add(new PlatformAdaptiveScrollPanel(this.directoryListView), "fileList");
		String layoutStyles = panelLayout;
		if (fileNameInput) {
			JLabel fileNameLabel = new JLabel("File name:");
			this.add(fileNameLabel, "filesDisplayLbl");
			layoutStyles = layoutStyles + layoutWithFileNameLabel;
		} else {
			layoutStyles = layoutStyles + layoutWithoutFileNameLabel;
		}

		this.filesDisplayField = new FilesDisplayField(initialDirectory, fileNameInput, selectionType, multiSelection);
		this.add(this.filesDisplayField, "filesDisplay");
		this.filterComboBox = new FilterComboBox(filterIndex, filters);
		this.add(this.filterComboBox, "filterComboBox");
		this.filesDisplayField.setFont(this.filterComboBox.getFont());
		this.directoryListView.addPropertyChangeListener(this.directoryComboBox);
		this.directoryListView.addPropertyChangeListener(this.filesDisplayField);
		this.directoryComboBox.addPropertyChangeListener(this.directoryListView);
		this.filesDisplayField.addPropertyChangeListener(this.directoryListView);
		this.directoryListView.addPropertyChangeListener(this.filesDisplayField);
		this.filesDisplayField.addPropertyChangeListener(this);
		this.directoryListView.addPropertyChangeListener(this);
		this.filterComboBox.addPropertyChangeListener(this.directoryListView);
		FileSelectPanel.FolderActionListener folderActionListener = new FileSelectPanel.FolderActionListener(this);
		FileSelectPanel.FolderEnterKeyListener folderEnterKeyListener = new FileSelectPanel.FolderEnterKeyListener(this);
		this.upLevelButton.addActionListener(folderActionListener);
		this.upLevelButton.addKeyListener(folderEnterKeyListener);
		this.newFolderButton.addActionListener(folderActionListener);
		this.newFolderButton.addKeyListener(folderEnterKeyListener);
		mainLayout.applyStyles(layoutStyles);
	}

	public static JButton getCancelButton(FileSelectPanel fileSelectPanel) {
		return fileSelectPanel.cancelButton;
	}

	public static UpLevelButton getUpLevelButton(FileSelectPanel fileSelectPanel) {
		return fileSelectPanel.upLevelButton;
	}

	public static FileSelectPanel.NewFolderButton getNewFolderButton(FileSelectPanel fileSelectPanel) {
		return fileSelectPanel.newFolderButton;
	}

	public static JButton getApproveButton(FileSelectPanel fileSelectPanel) {
		return fileSelectPanel.approveButton;
	}

	public static BasePrompt getPrompt(FileSelectPanel fileSelectPanel) {
		return fileSelectPanel.prompt;
	}

	public static class FolderActionListener implements ActionListener {
		public final FileSelectPanel fileSelectPanel;

		public FolderActionListener(FileSelectPanel fileSelectPanel) {
			this.fileSelectPanel = fileSelectPanel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == FileSelectPanel.getUpLevelButton(this.fileSelectPanel)) {
				this.fileSelectPanel.goToParentDirectory();
			} else if (e.getSource() == FileSelectPanel.getNewFolderButton(this.fileSelectPanel)) {
				this.fileSelectPanel.createNewFolder();
			}
		}
	}

	public static class FinalSelectionListener implements PropertyChangeListener {
		public final FileSelectPanel fileSelectPanel;

		public FinalSelectionListener(FileSelectPanel fileSelectPanel) {
			this.fileSelectPanel = fileSelectPanel;
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName().equals("finalSelection") && (File)evt.getNewValue() != null) {
				this.fileSelectPanel.resultCode = 1;
				FileSelectPanel.getPrompt(this.fileSelectPanel).setVisible(false);
				FileSelectPanel.getPrompt(this.fileSelectPanel).dispose();
			}
		}
	}

	public static class ApproveCancelListener extends KeyAdapter implements ActionListener {
		public final FileSelectPanel fileSelectPanel;

		public ApproveCancelListener(FileSelectPanel fileSelectPanel) {
			this.fileSelectPanel = fileSelectPanel;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == FileSelectPanel.getApproveButton(this.fileSelectPanel)) {
				this.fileSelectPanel.approve();
			} else if (e.getSource() == FileSelectPanel.getCancelButton(this.fileSelectPanel)) {
				this.fileSelectPanel.cancel();
			}
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == 10) {
				if (e.getSource() == FileSelectPanel.getApproveButton(this.fileSelectPanel)) {
					this.fileSelectPanel.approve();
				} else if (e.getSource() == FileSelectPanel.getCancelButton(this.fileSelectPanel)) {
					this.fileSelectPanel.cancel();
				}
			}
		}
	}

	public static class FolderEnterKeyListener extends KeyAdapter {
		public final FileSelectPanel fileSelectPanel;

		public FolderEnterKeyListener(FileSelectPanel fileSelectPanel) {
			this.fileSelectPanel = fileSelectPanel;
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == 10) {
				if (e.getSource() == FileSelectPanel.getUpLevelButton(this.fileSelectPanel)) {
					this.fileSelectPanel.goToParentDirectory();
				} else if (e.getSource() == FileSelectPanel.getNewFolderButton(this.fileSelectPanel)) {
					this.fileSelectPanel.createNewFolder();
				}
			}
		}
	}

	public static class FileNameChangeListener implements PropertyChangeListener {
		public final FileSelectPanel fileSelectPanel;

		public FileNameChangeListener(FileSelectPanel fileSelectPanel) {
			this.fileSelectPanel = fileSelectPanel;
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName().equals("fileNameChanged")) {
				String fileName = (String)evt.getNewValue();
				if (fileName != null && fileName.length() != 0) {
					FileSelectPanel.getApproveButton(this.fileSelectPanel).setEnabled(true);
				} else {
					FileSelectPanel.getApproveButton(this.fileSelectPanel).setEnabled(false);
				}
			}
		}
	}

	public static class NewFolderButton extends JButton {
		private static String newFolderText = "New";

		public NewFolderButton() {
			Icon newFolderIcon = FileChooserUtils.getNewFolderIcon();
			if (newFolderIcon != null) {
				this.setIcon(newFolderIcon);
			} else {
				this.setText(newFolderText);
			}

			this.setToolTipText("New Folder");
		}

		@Override
		public float getAlignmentY() {
			return 0.5F;
		}

		@Override
		public Insets getMargin() {
			return new Insets(1, 1, 1, 1);
		}
	}
}
