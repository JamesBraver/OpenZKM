package com.zelix.ui.dialogs;

import lombok.SneakyThrows;

import com.zelix.HelpManager;
import com.zelix.UIConfig;
import com.zelix.repository.ClasspathManager;
import com.zelix.utils.function.ZKMConsumer;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFrame;

public class BuildHelperClasspathDialog extends ClasspathFileListDialog {
	private static String containerStyles = "btnHeight=max(previousBtn.defaultHeight, okBtn.defaultHeight, cancelBtn.defaultHeight, importBtn.defaultHeight, exportBtn.defaultHeight, helpBtn.defaultHeight);btnWidth=max(previousBtn.defaultWidth, okBtn.defaultWidth, cancelBtn.defaultWidth, importBtn.defaultWidth, exportBtn.defaultWidth, helpBtn.defaultWidth);previousBtn.centerX=container.width*10/100;previousBtn.bottom=container.bottom-10;previousBtn.height=btnHeight;previousBtn.width=btnWidth;okBtn.centerX=container.width*26/100;okBtn.top=previousBtn.top;okBtn.height=btnHeight;okBtn.width=btnWidth;cancelBtn.centerX=container.width*42/100;cancelBtn.top=previousBtn.top;cancelBtn.height=btnHeight;cancelBtn.width=btnWidth;importBtn.centerX=container.width*58/100;importBtn.top=previousBtn.top;importBtn.height=btnHeight;importBtn.width=btnWidth;exportBtn.centerX=container.width*74/100;exportBtn.top=previousBtn.top;exportBtn.height=btnHeight;exportBtn.width=btnWidth;helpBtn.centerX=container.width*90/100;helpBtn.top=previousBtn.top;helpBtn.height=btnHeight;helpBtn.width=btnWidth;layout.minWidth=btnWidth * 7";
	public JButton previousButton;

	public BuildHelperClasspathDialog(JFrame parentFrame, String title, ClasspathManager classpathManager, String message, String okButtonText, String okButtonToolTip, UIConfig uiConfig, ZKMConsumer resultConsumer) {
		super(parentFrame, title, classpathManager, message, okButtonText, okButtonToolTip, uiConfig, resultConsumer);
	}

	@Override
	public void createButtons(String okButtonText, String okButtonToolTip, StringBuffer layoutProperties, Container container) {
		this.previousButton = new JButton("Previous");
		this.previousButton.setToolTipText(HelpManager.getToolTip("PREVIOUS_SCREEN"));
		container.add(this.previousButton, "previousBtn");
		super.okButton = new JButton(okButtonText);
		super.okButton.setToolTipText(okButtonToolTip);
		container.add(super.okButton, "okBtn");
		super.cancelButton = new JButton("Cancel");
		super.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		container.add(super.cancelButton, "cancelBtn");
		super.importButton = new JButton("Import");
		super.importButton.setToolTipText(HelpManager.getToolTip("IMPORT_DIALOG"));
		container.add(super.importButton, "importBtn");
		super.exportButton = new JButton("Export");
		super.exportButton.setToolTipText(HelpManager.getToolTip("EXPORT_DIALOG"));
		container.add(super.exportButton, "exportBtn");
		super.helpButton = new JButton("Help");
		super.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
		container.add(super.helpButton, "helpBtn");
		this.previousButton.addKeyListener(this);
		super.okButton.addKeyListener(this);
		super.cancelButton.addKeyListener(this);
		super.importButton.addKeyListener(this);
		super.helpButton.addKeyListener(this);
		this.previousButton.addActionListener(this);
		super.okButton.addActionListener(this);
		super.cancelButton.addActionListener(this);
		super.importButton.addActionListener(this);
		super.exportButton.addActionListener(this);
		super.helpButton.addActionListener(this);
		layoutProperties.append(containerStyles);
	}

	@Override
	@SneakyThrows
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this.previousButton) {
			this.onPrevious();
		} else {
			super.actionPerformed(e);
		}
	}

	public void onPrevious() throws IOException {
		super.confirmed = true;
		this.close();
		super.resultConsumer.accept(null, 2);
	}

	@Override
	@SneakyThrows
	public void keyPressed(KeyEvent e) {
		Object eventSource = e.getSource();
		if (e.getKeyCode() == 10) {
			if (eventSource == this.previousButton) {
				this.onPrevious();
			} else {
				super.keyPressed(e);
			}
		} else {
			super.keyPressed(e);
		}
	}
}
