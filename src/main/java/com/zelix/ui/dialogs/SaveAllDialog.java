package com.zelix.ui.dialogs;

import lombok.SneakyThrows;

import com.zelix.HelpManager;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.utils.function.ZKMConsumer;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFrame;

public class SaveAllDialog extends SavingDialog implements ActionListener, KeyListener {
	private static String[] containerStyles = new String[]{
		"layout.minWidth=btnWidth*4+25",
		"layout.minHeight=300",
		"fileSelector.left=5",
		"fileSelector.top=0",
		"fileSelector.bottom=container.bottom-btnHeight-15",
		"fileSelector.right=container.right-5",
		"btnWidth=max(okBtn.defaultWidth, cancelBtn.defaultWidth, previousBtn.defaultWidth, helpBtn.defaultWidth)",
		"btnHeight=max(okBtn.defaultHeight, cancelBtn.defaultHeight, previousBtn.defaultHeight, helpBtn.defaultHeight)",
		"previousBtn.centerX=container.width*12/100",
		"previousBtn.bottom=container.bottom-5",
		"previousBtn.width=btnWidth",
		"okBtn.centerX=container.width*37/100",
		"okBtn.bottom=container.bottom-5",
		"okBtn.width=btnWidth",
		"cancelBtn.centerX=container.width*62/100",
		"cancelBtn.bottom=container.bottom-5",
		"cancelBtn.width=btnWidth",
		"helpBtn.centerX=container.width*87/100",
		"helpBtn.bottom=container.bottom-5",
		"helpBtn.width=btnWidth"
	};
	public JButton previousButton;

	public SaveAllDialog(JFrame parentFrame, String title, String okButtonText, String okButtonToolTip, String defaultPath, ZKMConsumer resultConsumer) {
		super(parentFrame, title, okButtonText, okButtonToolTip, defaultPath, resultConsumer);
	}

	@Override
	protected void createButtons(String okButtonText, String okButtonToolTip, PowerLayout layout, Container container) {
		this.previousButton = new JButton("Previous");
		this.previousButton.setToolTipText(HelpManager.getToolTip("PREVIOUS_SCREEN"));
		container.add(this.previousButton, "previousBtn");
		super.okButton = new JButton(okButtonText);
		super.okButton.setToolTipText(okButtonToolTip);
		container.add(super.okButton, "okBtn");
		super.okButton.setEnabled(false);
		super.cancelButton = new JButton("Cancel");
		super.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		container.add(super.cancelButton, "cancelBtn");
		super.helpButton = new JButton("Help");
		super.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
		container.add(super.helpButton, "helpBtn");
		this.previousButton.addActionListener(this);
		super.okButton.addActionListener(this);
		super.cancelButton.addActionListener(this);
		super.helpButton.addActionListener(this);
		this.previousButton.addKeyListener(this);
		super.okButton.addKeyListener(this);
		super.cancelButton.addKeyListener(this);
		super.helpButton.addKeyListener(this);
		layout.setProperties(containerStyles);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object eventSource = e.getSource();
		if (eventSource instanceof JButton) {
			JButton button = (JButton)eventSource;
			if (button == this.previousButton) {
				this.onPrevious();
			} else if (button == super.okButton) {
				this.onOk();
			} else if (button == super.cancelButton) {
				this.onConfirm();
			} else if (button == super.helpButton) {
				this.openHelpGUI();
			}
		}
	}

	@SneakyThrows
	public void onPrevious() {
		File selectedFile = super.fileSelectPanel.getFile();
		super.confirmed = true;
		this.close();
		super.resultConsumer.accept(selectedFile, 2);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == 10) {
			if (e.getSource() == super.okButton || e.getSource() == super.fileSelectPanel) {
				this.onOk();
			} else if (e.getSource() == this.previousButton) {
				this.onPrevious();
			} else if (e.getSource() == super.cancelButton) {
				this.onConfirm();
			} else if (e.getSource() == super.helpButton) {
				this.openHelpGUI();
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	@SneakyThrows
	public void onOk() {
		File selectedFile = super.fileSelectPanel.getFile();
		if (this.confirmOverwrite(selectedFile, "The script will save some files to the same folder from which they were opened. Continue?")) {
			super.confirmed = true;
			this.close();
			super.resultConsumer.accept(selectedFile, 1);
		}
	}
}
