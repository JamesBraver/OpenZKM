package com.zelix.ui.utils;

import java.io.File;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalIconFactory;

public class FileChooserUtils {
	private JFileChooser fileChooser;

	public FileChooserUtils() {
		try {
			this.fileChooser = new JFileChooser();
		} catch (Throwable ignored) {
		}
	}

	public static Icon getUpFolderIcon() {
		try {
			Icon upFolderIcon = UIManager.getIcon("FileChooser.upFolderIcon");
			if (upFolderIcon == null) {
				upFolderIcon = MetalIconFactory.getFileChooserUpFolderIcon();
			}

			return upFolderIcon;
		} catch (Throwable ignored) {
			return null;
		}
	}

	public static Icon getNewFolderIcon() {
		try {
			Icon newFolderIcon = UIManager.getIcon("FileChooser.newFolderIcon");
			if (newFolderIcon == null) {
				newFolderIcon = MetalIconFactory.getFileChooserNewFolderIcon();
			}

			return newFolderIcon;
		} catch (Throwable ignored) {
			return null;
		}
	}

	public Icon getFileIcon(File file) {
		if (this.fileChooser != null) {
			try {
				return this.fileChooser.getIcon(file);
			} catch (Throwable ignored) {
			}
		}

		return null;
	}
}
