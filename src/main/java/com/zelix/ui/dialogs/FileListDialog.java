package com.zelix.ui.dialogs;

import lombok.SneakyThrows;

import com.zelix.HelpManager;
import com.zelix.klassmaster.config.Options;
import com.zelix.ui.base.BaseDialog;
import com.zelix.ui.components.FileListView;
import com.zelix.ui.components.panels.FileSelectPanel;
import com.zelix.ui.filefilters.AllFiles;
import com.zelix.ui.filefilters.FileFilterBase;
import com.zelix.ui.filefilters.TextFile;
import com.zelix.ui.prompts.CopyablePrompt;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public abstract class FileListDialog extends BaseDialog implements ActionListener, KeyListener, ListSelectionListener {
	protected static final String[] listPanelProperties = new String[]{
		"upperBtnWidth=max(browseBtn.defaultWidth, removeBtn.defaultWidth, upBtn.defaultWidth, downBtn.defaultWidth)",
		"msgLbl.top=2",
		"msgLbl.left=5",
		"msgLbl.right=container.right-5",
		"pathLst.left=5",
		"pathLst.right=container.right-upperBtnWidth-10",
		"pathLst.top=msgLbl.bottom+2",
		"pathLst.bottom=container.bottom-5",
		"browseBtn.left=pathLst.right+5",
		"browseBtn.right=container.right-5",
		"browseBtn.bottom=removeBtn.top-3",
		"browseBtn.width=upperBtnWidth",
		"removeBtn.left=pathLst.right+5",
		"removeBtn.right=container.right-5",
		"removeBtn.bottom=upBtn.top-10",
		"removeBtn.width=upperBtnWidth",
		"upBtn.right=container.right-5",
		"upBtn.top=pathLst.centerY+5",
		"upBtn.width=upperBtnWidth",
		"downBtn.right=container.right-5",
		"downBtn.width=upperBtnWidth",
		"downBtn.top=upBtn.bottom+3"
	};
	public JButton importButton;
	public JLabel messageLabel;
	public JButton removeButton;
	public DefaultListModel<File> fileListModel;
	public HashMap<String, File> fileMap;
	public JButton downButton;
	public JButton okButton;
	public boolean confirmed;
	public FileListView<File> fileListView;
	public JButton upButton;
	public JButton helpButton;
	public JButton addButton;
	public JButton exportButton;
	public JButton cancelButton;

	public FileListDialog(JFrame parentFrame, String title, List<String> initialPaths, String message, String okButtonText, String okButtonToolTip) {
		super(parentFrame, title, initialPaths, message, okButtonText, okButtonToolTip);
		this.centerOnParent();
		GuiUtils.requestFocusInWindow(this.okButton);
	}

	protected void updateButtonStates() {
		int fileCount = this.fileListModel.getSize();
		List<File> selectedFiles = this.fileListView.getSelectedValuesList();
		int minSelectedIndex = this.fileListModel.getSize();
		int maxSelectedIndex = 0;

		for (int i = 0; i < selectedFiles.size(); i++) {
			Object selectedFile = selectedFiles.get(i);
			int selectedIndex = this.fileListModel.indexOf(selectedFile);
			if (selectedIndex < minSelectedIndex) {
				minSelectedIndex = selectedIndex;
			}

			if (selectedIndex > maxSelectedIndex) {
				maxSelectedIndex = selectedIndex;
			}
		}

		if (this.fileListView.getSelectedIndex() == -1) {
			this.removeButton.setEnabled(false);
			this.upButton.setEnabled(false);
			this.downButton.setEnabled(false);
		} else {
			this.removeButton.setEnabled(true);
			this.upButton.setEnabled(maxSelectedIndex > selectedFiles.size() - 1);
			this.downButton.setEnabled(minSelectedIndex + selectedFiles.size() < fileCount);
		}

		if (this.upButton.hasFocus()) {
			if (!this.upButton.isEnabled()) {
				if (this.downButton.isEnabled()) {
					GuiUtils.requestFocusInWindow(this.downButton);
				} else {
					GuiUtils.requestFocusInWindow(this.addButton);
				}
			}
		} else if (this.downButton.hasFocus() && !this.downButton.isEnabled()) {
			if (this.upButton.isEnabled()) {
				GuiUtils.requestFocusInWindow(this.upButton);
			} else {
				GuiUtils.requestFocusInWindow(this.addButton);
			}
		}
	}

	public abstract void onOk() throws IOException;

	public final void moveSelectedDown() {
		List<File> selectedFiles = this.fileListView.getSelectedValuesList();
		int[] selectedIndices = new int[selectedFiles.size()];
		int[] movedIndices = new int[selectedFiles.size()];

		for (int i = 0; i < selectedFiles.size(); i++) {
			Object selectedFile = selectedFiles.get(i);
			int fileIndex = this.fileListModel.indexOf(selectedFile);
			selectedIndices[i] = fileIndex;
		}

		Arrays.sort(selectedIndices);
		int lastIndex = this.fileListModel.getSize() - 1;
		int maxSwapIndex = lastIndex;

		for (int selectionIndex = selectedIndices.length - 1; selectionIndex >= 0; selectionIndex--) {
			int sourceIndex = selectedIndices[selectionIndex];
			if (sourceIndex < lastIndex && sourceIndex < maxSwapIndex) {
				File swappedFile = this.fileListModel.getElementAt(sourceIndex + 1);
				this.fileListModel.setElementAt(this.fileListModel.getElementAt(sourceIndex), sourceIndex + 1);
				this.fileListModel.setElementAt(swappedFile, sourceIndex);
				movedIndices[selectionIndex] = sourceIndex + 1;
				maxSwapIndex = sourceIndex + 1;
			} else {
				movedIndices[selectionIndex] = sourceIndex;
				maxSwapIndex = sourceIndex - 1;
			}
		}

		this.fileListView.clearSelection();
		this.fileListView.setSelectedIndices(movedIndices);
		this.fileListView.ensureIndexIsVisible(movedIndices[movedIndices.length - 1]);
		this.updateButtonStates();
	}

	public final void moveSelectedUp() {
		List<File> selectedFiles = this.fileListView.getSelectedValuesList();
		int[] selectedIndices = new int[selectedFiles.size()];
		int[] movedIndices = new int[selectedFiles.size()];

		for (int i = 0; i < selectedFiles.size(); i++) {
			Object selectedFile = selectedFiles.get(i);
			int fileIndex = this.fileListModel.indexOf(selectedFile);
			selectedIndices[i] = fileIndex;
		}

		Arrays.sort(selectedIndices);
		int minSwapIndex = 0;

		for (int selectionIndex = 0; selectionIndex < selectedIndices.length; selectionIndex++) {
			int sourceIndex = selectedIndices[selectionIndex];
			if (sourceIndex > 0 && sourceIndex > minSwapIndex) {
				File swappedFile = this.fileListModel.getElementAt(sourceIndex - 1);
				this.fileListModel.setElementAt(this.fileListModel.getElementAt(sourceIndex), sourceIndex - 1);
				this.fileListModel.setElementAt(swappedFile, sourceIndex);
				movedIndices[selectionIndex] = sourceIndex - 1;
				minSwapIndex = sourceIndex - 1;
			} else {
				movedIndices[selectionIndex] = sourceIndex;
				minSwapIndex = sourceIndex + 1;
			}
		}

		this.fileListView.clearSelection();
		this.fileListView.setSelectedIndices(movedIndices);
		this.fileListView.ensureIndexIsVisible(movedIndices[0]);
		this.updateButtonStates();
	}

	public final void removeSelected() {
		List<File> selectedFiles = this.fileListView.getSelectedValuesList();
		int minRemovedIndex = this.fileListModel.getSize();

		for (int i = 0; i < selectedFiles.size(); i++) {
			Object selectedFile = selectedFiles.get(i);
			int fileIndex = this.fileListModel.indexOf(selectedFile);
			if (fileIndex < minRemovedIndex) {
				minRemovedIndex = fileIndex;
			}

			this.fileMap.remove(((File)selectedFile).getAbsolutePath());
			this.fileListModel.removeElement(selectedFile);
		}

		int remainingCount = this.fileListModel.getSize();
		if (remainingCount > minRemovedIndex) {
			this.fileListView.setSelectedIndex(minRemovedIndex);
		} else if (remainingCount > 0) {
			this.fileListView.setSelectedIndex(remainingCount - 1);
		} else {
			GuiUtils.requestFocusInWindow(this.addButton);
		}

		this.updateButtonStates();
	}

	@Override
	protected abstract void initGui(Object initArg1, Object initArg2, Object initArg3, Object initArg4, Object initArg5, Object initArg6, Object initArg7);

	protected boolean addFile(String path) {
		File file = new File(path);
		boolean fileAdded = this.fileMap.put(path, file) == null;
		if (fileAdded) {
			this.fileListModel.addElement(file);
			this.fileListView.setSelectedIndex(this.fileListModel.getSize() - 1);
			this.fileListView.ensureIndexIsVisible(this.fileListModel.getSize() - 1);
		} else {
			int existingIndex = this.fileListModel.indexOf(file);
			this.fileListView.setSelectedIndex(existingIndex);
			this.fileListView.ensureIndexIsVisible(existingIndex);
		}

		return fileAdded;
	}

	protected final void exportFileList() {
		FileFilterBase[] fileFilters = new FileFilterBase[]{new TextFile(), new AllFiles()};
		FileSelectPanel fileSelectPanel = new FileSelectPanel(FileUtils.USER_DIRECTORY, false, 1, fileFilters, 0, true);
		if (fileSelectPanel.showOpenDialog(this, "Select export destination") == 1) {
			File selectedFile = fileSelectPanel.getFile();
			BufferedWriter writer = null;

			try {
				String fileListText = this.getFileListText();
				writer = new BufferedWriter(new FileWriter(selectedFile));
				writer.write(fileListText, 0, fileListText.length());
			} catch (IOException ioException) {
				new CopyablePrompt(this, "File Error", "Couldn't save \"" + selectedFile.getAbsolutePath() + "\" : " + ioException.getMessage());
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException ignored) {
					}
				}
			}
		}
	}

	public void createButtons(String okButtonText, String okButtonToolTip, StringBuffer layoutProperties, Container container) {
		this.okButton = new JButton(okButtonText);
		this.okButton.setToolTipText(okButtonToolTip);
		container.add(this.okButton, "okBtn");
		this.cancelButton = new JButton("Cancel");
		this.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		container.add(this.cancelButton, "cancelBtn");
		this.importButton = new JButton("Import");
		this.importButton.setToolTipText(HelpManager.getToolTip("IMPORT_DIALOG"));
		container.add(this.importButton, "importBtn");
		this.exportButton = new JButton("Export");
		this.exportButton.setToolTipText(HelpManager.getToolTip("EXPORT_DIALOG"));
		container.add(this.exportButton, "exportBtn");
		this.helpButton = new JButton("Help");
		this.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
		container.add(this.helpButton, "helpBtn");
		this.okButton.addKeyListener(this);
		this.cancelButton.addKeyListener(this);
		this.helpButton.addKeyListener(this);
		this.okButton.addActionListener(this);
		this.cancelButton.addActionListener(this);
		this.importButton.addActionListener(this);
		this.exportButton.addActionListener(this);
		this.helpButton.addActionListener(this);
		layoutProperties.append(
			"btnHeight=max(okBtn.defaultHeight, cancelBtn.defaultHeight, importBtn.defaultHeight, exportBtn.defaultHeight, helpBtn.defaultHeight);btnWidth=max(okBtn.defaultWidth, cancelBtn.defaultWidth, importBtn.defaultWidth, exportBtn.defaultWidth, helpBtn.defaultWidth);okBtn.centerX=container.width*10/100;okBtn.bottom=bottom=container.bottom-10;okBtn.height=btnHeight;okBtn.width=btnWidth;cancelBtn.centerX=container.width*30/100;cancelBtn.top=okBtn.top;cancelBtn.height=btnHeight;cancelBtn.width=btnWidth;importBtn.centerX=container.width*50/100;importBtn.top=okBtn.top;importBtn.height=btnHeight;importBtn.width=btnWidth;exportBtn.centerX=container.width*70/100;exportBtn.top=okBtn.top;exportBtn.height=btnHeight;exportBtn.width=btnWidth;helpBtn.centerX=container.width*90/100;helpBtn.top=okBtn.top;helpBtn.height=btnHeight;helpBtn.width=btnWidth"
		);
	}

	@Override
	@SneakyThrows
	public void actionPerformed(ActionEvent e) {
		Object eventSource = e.getSource();
		if (eventSource == this.removeButton) {
			this.removeSelected();
		} else if (eventSource == this.upButton) {
			this.moveSelectedUp();
		} else if (eventSource == this.downButton) {
			this.moveSelectedDown();
		} else if (eventSource == this.addButton) {
			this.browseFiles();
		} else if (eventSource == this.okButton) {
			this.onOk();
		} else if (eventSource == this.cancelButton) {
			this.onConfirm();
		} else if (eventSource == this.importButton) {
			this.importFileList();
		} else if (eventSource == this.exportButton) {
			this.exportFileList();
		} else if (eventSource == this.helpButton) {
			this.openHelpGUI();
		}
	}

	public abstract void browseFiles();

	protected abstract void openHelpGUI();

	@Override
	@SneakyThrows
	public void keyPressed(KeyEvent e) {
		Object eventSource = e.getSource();
		if (e.getKeyCode() == 10) {
			if (eventSource == this.removeButton) {
				this.removeSelected();
			} else if (eventSource == this.upButton) {
				this.moveSelectedUp();
			} else if (eventSource == this.downButton) {
				this.moveSelectedDown();
			} else if (eventSource == this.addButton) {
				this.browseFiles();
			} else if (eventSource == this.okButton) {
				this.onOk();
			} else if (eventSource == this.cancelButton) {
				this.onConfirm();
			} else if (eventSource == this.importButton) {
				this.importFileList();
			} else if (eventSource == this.exportButton) {
				this.exportFileList();
			} else if (eventSource == this.helpButton) {
				this.openHelpGUI();
			} else if (eventSource == this.fileListView) {
			}
		} else if (e.getKeyCode() == 127 && eventSource == this.fileListView) {
			this.removeSelected();
		}
	}

	@Override
	public final void keyReleased(KeyEvent e) {
	}

	@Override
	public final void keyTyped(KeyEvent e) {
	}

	private List<File> readFileList(BufferedReader reader, List<String> missingPaths) throws IOException {
		ArrayList<File> files = new ArrayList<>();

		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (line.length() > 0 && !line.startsWith("//")) {
				if (line.length() >= 3 && line.startsWith("\"") && line.endsWith("\"")) {
					line = line.substring(1, line.length() - 1);
				}

				File file = new File(line);
				if (file.exists()) {
					files.add(file);
				} else {
					missingPaths.add(line);
				}
			}
		}

		return files;
	}

	@Override
	protected abstract void onConfirm();

	@Override
	public void close() {
		super.close();
	}

	public void setFiles(List<File> files, List<String> missingPaths) {
		this.fileListModel.clear();
		this.fileMap.clear();

		for (File file : files) {
			if (this.fileMap.put(file.getAbsolutePath(), file) == null) {
				this.fileListModel.addElement(file);
			}
		}

		if (this.fileListModel.getSize() > 0) {
			this.fileListView.setSelectedIndex(this.fileListModel.getSize() - 1);
			this.fileListView.ensureIndexIsVisible(this.fileListModel.getSize() - 1);
		}

		if (!missingPaths.isEmpty()) {
			int displayedPathCount = Math.min(20, missingPaths.size());
			GuiUtils.createMessageDialog(
				this, "FILE ERROR: Files not found", "These strings did not match an existing file.", GlobalUtils.toQuotedString(missingPaths.subList(0, displayedPathCount))
			);
		}
	}

	@Override
	public final void valueChanged(ListSelectionEvent e) {
		if (!e.getValueIsAdjusting()) {
			this.updateButtonStates();
		}
	}

	protected final void importFileList() {
		FileFilterBase[] fileFilters = new FileFilterBase[]{new TextFile(), new AllFiles()};
		FileSelectPanel fileSelectPanel = new FileSelectPanel(FileUtils.USER_DIRECTORY, false, 1, fileFilters, 0, true);
		if (fileSelectPanel.showOpenDialog(this, "Select import file") == 1) {
			File selectedFile = fileSelectPanel.getFile();
			ArrayList<String> missingPaths = new ArrayList<>();
			BufferedReader reader = null;

			try {
				reader = new BufferedReader(new FileReader(selectedFile));
				List<File> files = this.readFileList(reader, missingPaths);
				this.setFiles(files, missingPaths);
			} catch (IOException ioException) {
				new CopyablePrompt(this, "File Error", "Couldn't open or read \"" + selectedFile.getAbsolutePath() + "\" : " + ioException.getMessage());
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException ignored) {
					}
				}
			}
		}
	}

	protected String getFileListText() {
		int fileCount = this.fileListModel.getSize();
		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < fileCount; i++) {
			File file = this.fileListModel.getElementAt(i);
			if (!file.isAbsolute()) {
				builder.append(file.getPath().trim());
			} else {
				builder.append(file.getAbsolutePath().trim());
			}

			if (i < fileCount - 1) {
				builder.append(Options.lineSeparator);
			}
		}

		return builder.toString();
	}
}
