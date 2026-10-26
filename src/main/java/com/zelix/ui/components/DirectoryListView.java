package com.zelix.ui.components;

import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.comparators.FileComparator;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class DirectoryListView extends FileListView<File> implements FileSelectorComponent, KeyListener, MouseListener, PropertyChangeListener {
	private DefaultListModel<File> listModel;
	private FileFilter fileFilter;
	private boolean multipleSelection;
	private int fileSelectionMode;
	public boolean updatingSelection;
	private File[] files;
	private File currentDirectory;

	public DirectoryListView(File directory, boolean multipleSelection, int fileSelectionMode, FileFilter fileFilter) {
		this.setShowFullPath(false);
		this.listModel = new DefaultListModel<>();
		this.setModel(this.listModel);
		this.fileFilter = fileFilter;
		this.multipleSelection = multipleSelection;
		if (multipleSelection) {
			this.setSelectionMode(2);
		} else {
			this.setSelectionMode(0);
		}

		this.fileSelectionMode = fileSelectionMode;
		this.setDirectory(directory);
		DirectoryListView.SelectionChangeListener selectionListener = new DirectoryListView.SelectionChangeListener(this);
		this.addListSelectionListener(selectionListener);
		this.addMouseListener(this);
		this.addKeyListener(this);
	}

	public void setFileFilter(FileFilter fileFilter) {
		if (this.fileFilter != fileFilter) {
			this.fileFilter = fileFilter;
			List<File> selectedFiles = this.getSelectedValuesList();
			this.refreshFileList();
			ArrayList<Integer> retainedIndices = new ArrayList<>(selectedFiles.size());

			for (int fileIndex = 0; fileIndex < selectedFiles.size(); fileIndex++) {
				int modelIndex = this.listModel.indexOf(selectedFiles.get(fileIndex));
				if (modelIndex > -1) {
					retainedIndices.add(modelIndex);
				}
			}

			int[] retainedIndexArray = new int[retainedIndices.size()];

			for (int i = 0; i < retainedIndices.size(); i++) {
				retainedIndexArray[i] = retainedIndices.get(i);
			}

			this.setSelectedIndices(retainedIndexArray);
			if (retainedIndexArray.length > 0) {
				this.ensureIndexIsVisible(retainedIndexArray[0]);
			}
		}
	}

	public void goToParentDirectory() {
		if (this.currentDirectory != null) {
			File parentDirectory = this.currentDirectory.getParentFile();
			if (parentDirectory != null) {
				this.setDirectory(parentDirectory);
				this.firePropertyChange("directoryChanged", null, parentDirectory);
			}
		}
	}

	private void onSelectionChanged(ListSelectionEvent e) {
		int[] selectedIndices = this.getSelectedIndices();
		boolean anyItemSelected = false;
		this.updatingSelection = true;

		for (int i = 0; i < selectedIndices.length; i++) {
			int selectedIndex = selectedIndices[i];
			File selectedFile = this.listModel.get(selectedIndex);
			switch (this.fileSelectionMode) {
				case 1:
					if (selectedFile.isDirectory()) {
						this.removeSelectionInterval(selectedIndex, selectedIndex);
					} else {
						anyItemSelected = true;
					}
					break;
				case 2:
					if (!selectedFile.isDirectory()) {
						this.removeSelectionInterval(selectedIndex, selectedIndex);
					} else {
						anyItemSelected = true;
					}
					break;
				case 3:
					anyItemSelected = true;
			}
		}

		this.updatingSelection = false;
		if (anyItemSelected) {
			this.firePropertyChange("selectionChanged", null, this.getSelectedFiles());
		}
	}

	public File getCurrentDirectory() {
		return this.currentDirectory;
	}

	public void setDirectory(File directory) {
		if (this.currentDirectory == null || !this.currentDirectory.equals(directory)) {
			this.currentDirectory = directory;
			this.refreshFileList();
			if (this.listModel.getSize() > 0) {
				this.ensureIndexIsVisible(0);
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == 10) {
			List<File> selectedFiles = this.getSelectedValuesList();
			if (selectedFiles.size() == 1) {
				File selectedFile = selectedFiles.get(0);
				if (!selectedFile.isDirectory()) {
					if (this.fileSelectionMode == 1 || this.fileSelectionMode == 3) {
						this.firePropertyChange("finalSelection", null, selectedFile);
					}
				} else {
					this.setDirectory(selectedFile);
					this.firePropertyChange("directoryChanged", null, selectedFile);
				}
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	private void refreshFileList() {
		if (this.fileFilter != null) {
			this.files = this.currentDirectory.listFiles(this.fileFilter);
		} else {
			this.files = this.currentDirectory.listFiles();
		}

		if (this.files == null) {
			this.files = new File[0];
		}

		FileComparator fileComparator = FileComparator.getInstance();
		Arrays.sort(this.files, fileComparator);
		DefaultListModel<File> newListModel = new DefaultListModel<>();

		for (int i = 0; i < this.files.length; i++) {
			newListModel.addElement(this.files[i]);
		}

		this.listModel = newListModel;
		this.setModel(newListModel);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2) {
			int clickedIndex = this.locationToIndex(e.getPoint());
			if (clickedIndex > -1) {
				File clickedFile = this.listModel.get(clickedIndex);
				if (clickedFile.isDirectory()) {
					this.setDirectory(clickedFile);
					this.firePropertyChange("directoryChanged", null, clickedFile);
				} else if ((this.fileSelectionMode == 1 || this.fileSelectionMode == 3) && this.getSelectedValuesList().size() == 1) {
					this.firePropertyChange("finalSelection", null, clickedFile);
				}
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("directoryChanged")) {
			File changedDirectory = (File)evt.getNewValue();
			this.setDirectory(changedDirectory);
		} else if (evt.getPropertyName().equals("fileFilterChanged")) {
			this.setFileFilter((FileFilter)evt.getNewValue());
		} else if (evt.getPropertyName().equals("finalSelection")) {
			this.firePropertyChange("finalSelection", null, evt.getNewValue());
		}
	}

	public void setSelectedFile(File file, boolean shouldScroll) {
		if (file != null && this.listModel.contains(file)) {
			this.setSelectedValue(file, shouldScroll);
		}
	}

	public boolean createDirectory(String directoryName) {
		File newDirectory = new File(this.currentDirectory, directoryName);
		boolean directoryCreated = newDirectory.mkdir();
		if (directoryCreated) {
			this.refreshFileList();
			this.setSelectedValue(newDirectory, true);
			GuiUtils.requestFocusInWindow(this);
		}

		return directoryCreated;
	}

	public File[] getSelectedFiles() {
		List<File> selectedFiles = this.getSelectedValuesList();
		File[] selectedFilesArray = new File[selectedFiles.size()];
		return selectedFiles.toArray(selectedFilesArray);
	}

	public static void dispatchSelectionChanged(DirectoryListView directoryListView, ListSelectionEvent e) {
		directoryListView.onSelectionChanged(e);
	}

	public static class SelectionChangeListener implements ListSelectionListener {
		public final DirectoryListView directoryListView;

		public SelectionChangeListener(DirectoryListView directoryListView) {
			this.directoryListView = directoryListView;
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {
			DirectoryListView.dispatchSelectionChanged(this.directoryListView, e);
		}
	}
}
