package com.zelix.ui.components;

import com.zelix.utils.FileUtils;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

public class FilesDisplayField extends JTextField implements FileSelectorComponent, ActionListener, PropertyChangeListener, DocumentListener {
	private static String fileNameQuote = "\"";
	private File currentDirectory;
	private Document document;
	private int fileSelectionMode;
	private boolean multipleSelection;
	private File[] selectedFiles;
	private boolean textEdited;

	public FilesDisplayField(File directory, boolean editable, int fileSelectionMode, boolean multipleSelection) {
		this.currentDirectory = directory;
		this.document = new PlainDocument();
		this.setDocument(this.document);
		this.document.addDocumentListener(this);
		this.setEditable(editable);
		this.fileSelectionMode = fileSelectionMode;
		this.multipleSelection = multipleSelection;
		this.addActionListener(this);
		this.setText("");
	}

	public File[] getFiles() {
		if (this.textEdited) {
			String text = this.getText().trim();
			if (text.length() > 0) {
				File[] files = new File[1];
				if (FileUtils.isRelativePath(text)) {
					files[0] = new File(this.currentDirectory, text);
				} else {
					files[0] = new File(text);
				}

				return files;
			} else {
				return null;
			}
		} else {
			return this.selectedFiles;
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (this.fileSelectionMode == 1 || this.fileSelectionMode == 3) {
			this.firePropertyChange("finalSelection", null, this.getFile());
		}
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		this.onDocumentChanged(e);
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		this.onDocumentChanged(e);
	}

	public File getFile() {
		if (this.textEdited) {
			String text = this.getText().trim();
			if (text.length() > 0) {
				File file;
				if (FileUtils.isRelativePath(text)) {
					file = new File(this.currentDirectory, text);
				} else {
					file = new File(text);
				}

				return file;
			} else {
				return null;
			}
		} else {
			return this.selectedFiles != null && this.selectedFiles.length > 0 ? this.selectedFiles[0] : null;
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("selectionChanged")) {
			File[] oldFiles = this.selectedFiles;
			Object[] newValues = (Object[])evt.getNewValue();
			int valueCount = newValues.length;
			File[] newFiles = new File[valueCount];
			System.arraycopy(newValues, 0, newFiles, 0, valueCount);
			this.setFiles(newFiles);
			this.textEdited = false;
			this.firePropertyChange("selectionChanged", oldFiles, newFiles);
		} else if (evt.getPropertyName().equals("directoryChanged")) {
			this.currentDirectory = (File)evt.getNewValue();
		}
	}

	private void setFiles(File[] files) {
		if (files != null && files.length != 0) {
			this.selectedFiles = files;
			int fileCount = files.length;
			ArrayList<File> fileList = new ArrayList<>();
			StringBuffer textBuffer = new StringBuffer();

			for (int i = 0; i < fileCount; i++) {
				File file = files[i];
				if (fileCount > 1) {
					textBuffer.append(fileNameQuote);
				}

				textBuffer.append(file.getName());
				if (fileCount > 1) {
					textBuffer.append(fileNameQuote);
				}

				if (i < fileCount - 1) {
					textBuffer.append(" ");
				}

				fileList.add(file);
			}

			this.selectedFiles = fileList.toArray(new File[fileList.size()]);
			this.setText(textBuffer.toString());
		} else {
			this.selectedFiles = new File[0];
			this.setText("");
		}
	}

	public void setFile(File file) {
		File[] files = new File[]{file};
		this.setFiles(files);
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		this.onDocumentChanged(e);
	}

	public void onDocumentChanged(DocumentEvent e) {
		this.textEdited = true;
		this.firePropertyChange("fileNameChanged", null, this.getText().trim());
	}
}
