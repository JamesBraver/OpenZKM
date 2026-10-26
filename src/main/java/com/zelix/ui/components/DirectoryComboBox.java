package com.zelix.ui.components;

import com.zelix.ui.utils.FileChooserUtils;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JList;

public class DirectoryComboBox extends JComboBox<File> implements FileSelectorComponent, ItemListener, PropertyChangeListener {
	private File[] rootDrives;
	private DefaultComboBoxModel<File> comboBoxModel;
	public boolean updatingSelection;
	private File currentDirectory;
	private ArrayList<File> parentDirectories;

	public DirectoryComboBox(File directory) {
		this.initRootDrives();
		this.setDirectory(directory);
		this.addItemListener(this);
	}

	public void initRootDrives() {
		this.rootDrives = File.listRoots();
		this.comboBoxModel = new DefaultComboBoxModel<>();
		this.setModel(this.comboBoxModel);

		for (int i = 0; i < this.rootDrives.length; i++) {
			this.comboBoxModel.addElement(this.rootDrives[i]);
		}

		this.setRenderer(new DirectoryListCellRenderer(this));
		this.setToolTipText("Current folder being listed");
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange() == 1 && !this.updatingSelection) {
			File selectedDirectory = (File)this.getSelectedItem();
			if (selectedDirectory.exists()) {
				this.currentDirectory = selectedDirectory;
				this.firePropertyChange("directoryChanged", null, selectedDirectory);
			} else {
				this.updatingSelection = true;
				this.setSelectedItem(this.currentDirectory);
				this.updatingSelection = false;
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("directoryChanged")) {
			this.updatingSelection = true;
			this.setDirectory((File)evt.getNewValue());
			this.updatingSelection = false;
		}
	}

	public void setDirectory(File directory) {
		this.currentDirectory = directory;
		this.parentDirectories = new ArrayList<>();

		for (File parentDirectory = this.currentDirectory.getParentFile(); parentDirectory != null; parentDirectory = parentDirectory.getParentFile()) {
			this.parentDirectories.add(parentDirectory);
		}

		Collections.reverse(this.parentDirectories);
		File rootDirectory = null;
		if (this.parentDirectories.size() > 0) {
			rootDirectory = this.parentDirectories.get(0);
		}

		DefaultComboBoxModel<File> newComboBoxModel = new DefaultComboBoxModel<>();

		for (int driveIndex = 0; driveIndex < this.rootDrives.length; driveIndex++) {
			newComboBoxModel.addElement(this.rootDrives[driveIndex]);
			if (rootDirectory != null && rootDirectory.equals(this.rootDrives[driveIndex])) {
				for (int parentIndex = 1; parentIndex < this.parentDirectories.size(); parentIndex++) {
					newComboBoxModel.addElement(this.parentDirectories.get(parentIndex));
				}

				newComboBoxModel.addElement(this.currentDirectory);
			}
		}

		this.comboBoxModel = newComboBoxModel;
		this.setModel(this.comboBoxModel);
		this.setSelectedItem(this.currentDirectory);
	}

	public static class DirectoryListCellRenderer extends DefaultListCellRenderer {
		public final DirectoryComboBox directoryComboBox;
		public DirectoryListCellRenderer.IndentedIcon indentedIcon;
		public FileChooserUtils fileChooserUtils;

		public DirectoryListCellRenderer(DirectoryComboBox directoryComboBox) {
			this.directoryComboBox = directoryComboBox;
			this.indentedIcon = new DirectoryListCellRenderer.IndentedIcon();
			this.fileChooserUtils = new FileChooserUtils();
		}

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			File directory = (File)value;
			if (directory == null) {
				this.setText("");
				return this;
			} else {
				int depth = 0;
				if (index != -1) {
					for (File parentDirectory = directory.getParentFile(); parentDirectory != null; parentDirectory = parentDirectory.getParentFile()) {
						depth++;
					}
				}

				Icon icon = this.fileChooserUtils.getFileIcon(directory);
				this.indentedIcon.icon = icon;
				this.indentedIcon.depth = depth;
				this.setIcon(this.indentedIcon);
				return this;
			}
		}

		public static class IndentedIcon implements Icon {
			public Icon icon = null;
			public int depth = 0;

			@Override
			public int getIconHeight() {
				return this.icon == null ? 0 : this.icon.getIconHeight();
			}

			@Override
			public int getIconWidth() {
				return this.icon == null ? 0 : this.icon.getIconWidth() + this.depth * 10;
			}

			@Override
			public void paintIcon(Component c, Graphics g, int x, int y) {
				if (this.icon != null) {
					this.icon.paintIcon(c, g, x + this.depth * 10, y);
				}
			}
		}
	}
}
