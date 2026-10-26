package com.zelix.ui.frames;

import com.zelix.HelpManager;
import com.zelix.framework.tree.ArchiveNode;
import com.zelix.framework.tree.ClasspathResource;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.utils.function.ZKMConsumer;
import com.zelix.utils.references.AtomicReference;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JFrame;

public class WrappedOpenClassesFrame extends OpenClassesFrame {
	public JButton previousButton;

	public WrappedOpenClassesFrame(JFrame parentFrame, String title, String okButtonText, boolean isOpenMode, String initialDirectory, boolean openNestedArchives, ClasspathResource[] initialResources, ZKMConsumer callback) {
		super(parentFrame, title, okButtonText, isOpenMode, initialDirectory, openNestedArchives, initialResources, callback);
	}

	@Override
	public void createButtons(PowerLayout powerLayout, String okButtonText, StringBuffer layoutConstraints, Container container) {
		this.previousButton = new JButton("Previous");
		this.previousButton.setToolTipText(HelpManager.getToolTip("PREVIOUS_SCREEN"));
		this.previousButton.addActionListener(this);
		this.previousButton.addKeyListener(this);
		container.add(this.previousButton, "previousBtn");
		super.okButton = new JButton(okButtonText);
		super.okButton.setToolTipText(HelpManager.getToolTip("OK_OPEN"));
		super.okButton.addActionListener(this);
		super.okButton.addKeyListener(this);
		container.add(super.okButton, "okBtn");
		super.cancelButton = new JButton("Cancel");
		super.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		super.cancelButton.addActionListener(this);
		super.cancelButton.addKeyListener(this);
		container.add(super.cancelButton, "cancelBtn");
		super.helpButton = new JButton("Help");
		super.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
		super.helpButton.addActionListener(this);
		super.helpButton.addKeyListener(this);
		container.add(super.helpButton, "helpBtn");
		layoutConstraints.append(
			"previousBtn.top=fileSelector.bottom+5;previousBtn.centerX=container.width*12/100;previousBtn.width=majorBtnWidth;previousBtn.height=majorBtnHeight;okBtn.top=previousBtn.top;okBtn.centerX=container.width*37/100;okBtn.width=majorBtnWidth;okBtn.height=majorBtnHeight;cancelBtn.top=previousBtn.top;cancelBtn.centerX=container.width*62/100;cancelBtn.width=majorBtnWidth;cancelBtn.height=majorBtnHeight;helpBtn.top=previousBtn.top;helpBtn.centerX=container.width*87/100;helpBtn.width=majorBtnWidth;helpBtn.height=majorBtnHeight;majorBtnHeight=max(previousBtn.defaultHeight, okBtn.defaultHeight, cancelBtn.defaultHeight, helpBtn.defaultHeight);majorBtnWidth=max(previousBtn.defaultWidth, okBtn.defaultWidth, cancelBtn.defaultWidth, helpBtn.defaultWidth)"
		);
	}

	public void onPrevious() {
		super.resultDelivered = true;
		this.close();
		String currentDirectoryPath = super.fileSelectPanel.getCurrentDirectoryPath();
		super.callback.accept(super.classResources, (ArchiveNode[])null, (ClasspathResource[])null, (ClasspathResource[])null, (ClasspathResource[])null, new AtomicReference<>(currentDirectoryPath));
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object eventSource = e.getSource();
		if (eventSource == super.okButton) {
			this.onOkPressed();
		} else if (eventSource == super.cancelButton) {
			this.cancel();
		} else if (eventSource == this.previousButton) {
			this.onPrevious();
		} else if (eventSource == super.helpButton) {
			this.showHelp();
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == 10) {
			Object eventSource = e.getSource();
			if (eventSource == this.previousButton) {
				this.onPrevious();
			} else if (eventSource == super.okButton) {
				this.onOkPressed();
			} else if (eventSource == super.cancelButton) {
				this.cancel();
			} else if (eventSource == super.helpButton) {
				this.showHelp();
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}
}
