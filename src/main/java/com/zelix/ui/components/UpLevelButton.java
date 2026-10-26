package com.zelix.ui.components;

import com.zelix.ui.utils.FileChooserUtils;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Icon;
import javax.swing.JButton;

public class UpLevelButton extends JButton implements FileSelectorComponent, PropertyChangeListener {
	private static String upText = "Up";

	public UpLevelButton() {
		Icon upFolderIcon = FileChooserUtils.getUpFolderIcon();
		if (upFolderIcon != null) {
			this.setIcon(upFolderIcon);
		} else {
			this.setText(upText);
		}

		this.setToolTipText("Up one level");
	}

	@Override
	public float getAlignmentY() {
		return 0.5F;
	}

	@Override
	public Insets getMargin() {
		return new Insets(1, 1, 1, 1);
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
	}
}
