package com.zelix.ui.components;

import com.zelix.klassmaster.config.Options;
import com.zelix.ui.utils.FileChooserUtils;
import java.awt.Component;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.filechooser.FileSystemView;

public class FileListView<E> extends PlatformAdaptiveList<E> {
	public boolean showFullPath = true;
	public String defaultPath = System.getProperty("user.dir");
	public File defaultDirectory = new File(this.defaultPath);

	public FileListView(ListModel<E> model) {
		super(model);
		this.initCellRenderer();
	}

	public FileListView() {
		this.initCellRenderer();
	}

	public void setShowFullPath(boolean showFullPath) {
		this.showFullPath = showFullPath;
	}

	private void initCellRenderer() {
		this.setCellRenderer(Options.ENABLE_PATCHES ? new PatchedFileCellRenderer(this) : new SimpleCellRenderer(this));
	}

	public static class PatchedFileCellRenderer extends DefaultListCellRenderer {
		private final FileListView<?> fileListView;
		private final Map<String, Icon> extensionIconMap;

		public PatchedFileCellRenderer(FileListView<?> fileListView) {
			this.fileListView = fileListView;
			this.extensionIconMap = new HashMap<>();
			this.extensionIconMap.put("jar", new ImageIcon(PatchedFileCellRenderer.class.getResource("/icons/jar.png")));
			this.extensionIconMap.put("xml", new ImageIcon(PatchedFileCellRenderer.class.getResource("/icons/xml.png")));
		}

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			String fileName = value.toString();
			String path = fileName;
			if (fileName.equals(".")) {
				path = this.fileListView.defaultPath;
			} else if (fileName.equals("..")) {
				path = this.fileListView.defaultDirectory.getParent();
				if (path == null) {
					path = this.fileListView.defaultPath;
				}
			}

			File file = new File(path);
			if (!file.isDirectory()) {
				String ext = this.getFileExtension(file);
				this.setIcon(this.extensionIconMap.getOrDefault(ext, FileSystemView.getFileSystemView().getSystemIcon(new File(System.getProperty("user.home")))));
			} else {
				this.setIcon(new ImageIcon(PatchedFileCellRenderer.class.getResource("/icons/folder.png")));
			}

			if (fileName.equals(".") || fileName.equals("..")) {
				this.setText(fileName);
			} else if (this.fileListView.showFullPath) {
				this.setText(file.getAbsolutePath());
			} else {
				this.setText(file.getName());
			}

			return this;
		}

		private String getFileExtension(File file) {
			String name = file.getName();
			int lastDot = name.lastIndexOf('.');
			return lastDot > 0 && lastDot < name.length() - 1 ? name.substring(lastDot + 1).toLowerCase() : "";
		}
	}

	public static class SimpleCellRenderer extends DefaultListCellRenderer {
		public final FileListView<?> fileListView;
		public FileChooserUtils fileChooserUtils;

		public SimpleCellRenderer(FileListView<?> fileListView) {
			this.fileListView = fileListView;
			this.fileChooserUtils = new FileChooserUtils();
		}

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			String fileName = value.toString();
			String path = fileName;
			if (fileName.equals(".")) {
				path = this.fileListView.defaultPath;
			} else if (fileName.equals("..")) {
				path = this.fileListView.defaultDirectory.getParent();
				if (path == null) {
					path = this.fileListView.defaultPath;
				}
			}

			File file = new File(path);
			Icon icon = this.fileChooserUtils.getFileIcon(file);
			if (icon != null) {
				this.setIcon(icon);
			}

			if (fileName.equals(".") || fileName.equals("..")) {
				this.setText(fileName);
			} else if (this.fileListView.showFullPath) {
				this.setText(file.getAbsolutePath());
			} else {
				this.setText(file.getName());
			}

			return this;
		}
	}
}
