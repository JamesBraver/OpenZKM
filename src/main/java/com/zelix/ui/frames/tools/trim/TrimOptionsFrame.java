package com.zelix.ui.frames.tools.trim;

import lombok.SneakyThrows;

import com.zelix.HelpManager;
import com.zelix.klassmaster.KlassMaster;
import com.zelix.klassmaster.config.TrimConfig;
import com.zelix.ui.base.BaseFrame;
import com.zelix.ui.components.panels.BorderedPanel;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.ui.utils.GuiUtils;
import com.zelix.utils.function.ZKMConsumer;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;

public class TrimOptionsFrame extends BaseFrame {
	public static final String[] layoutProperties = new String[]{
		"container.defaultBottomPad=10",
		"deleteSourceFileAttributesChk.top=10",
		"deleteSourceFileAttributesChk.left=10",
		"deleteDeprectedAttributesChk.top=deleteSourceFileAttributesChk.bottom+10",
		"deleteDeprectedAttributesChk.left=deleteSourceFileAttributesChk.left",
		"deleteAnnotationAttributesChk.top=deleteDeprectedAttributesChk.bottom+10",
		"deleteAnnotationAttributesChk.left=deleteSourceFileAttributesChk.left",
		"deleteExceptionAttributesChk.top=deleteAnnotationAttributesChk.bottom+10",
		"deleteExceptionAttributesChk.left=deleteAnnotationAttributesChk.left",
		"deleteDebugExtensionAttributesChk.top=deleteExceptionAttributesChk.bottom+10",
		"deleteDebugExtensionAttributesChk.left=deleteExceptionAttributesChk.left",
		"deleteUnknownAttributesChk.top=deleteDebugExtensionAttributesChk.bottom+10",
		"deleteUnknownAttributesChk.left=deleteDebugExtensionAttributesChk.left"
	};
	public BorderedPanel mainPanel = new BorderedPanel(true);
	public JCheckBox deleteSourceFileAttributes = new JCheckBox("delete Source File attributes");
	public JCheckBox deleteDeprecatedAttributes = new JCheckBox("delete Deprecated attributes");
	public JCheckBox deleteAnnotationAttributes = new JCheckBox("delete annotation attributes");
	public JCheckBox deleteExceptionAttributes = new JCheckBox("delete Exception attributes");
	public JCheckBox deleteDebugExtensionAttributes = new JCheckBox("delete Source Debug Extension attributes");
	public JCheckBox deleteUnknownAttributes = new JCheckBox("delete unknown attributes");
	public TrimConfig trimConfig;
	public JFrame parentFrame;
	public ZKMConsumer resultConsumer;
	public JButton helpButton;
	public boolean skipCloseCallback;
	public JButton okButton;
	public JButton previousButton;
	public JButton cancelButton;

	public TrimOptionsFrame(String title, String okButtonText, String okButtonToolTip, JFrame parentFrame, TrimConfig trimConfig, ZKMConsumer resultConsumer) {
		this.trimConfig = trimConfig;
		this.parentFrame = parentFrame;
		this.resultConsumer = resultConsumer;
		this.setFont(KlassMaster.getFont());
		this.initScreenContent(title, okButtonText, okButtonToolTip);
		GuiUtils.requestFocusInWindow(this.okButton);
	}

	public void saveOptionsToConfig() {
		this.trimConfig.deleteSourceFileAttributes = this.deleteSourceFileAttributes.isSelected();
		this.trimConfig.deleteDeprecatedAttributes = this.deleteDeprecatedAttributes.isSelected();
		this.trimConfig.deleteAnnotationAttributes = this.deleteAnnotationAttributes.isSelected();
		this.trimConfig.deleteExceptionAttributes = this.deleteExceptionAttributes.isSelected();
		this.trimConfig.deleteDebugExtensionAttributes = this.deleteDebugExtensionAttributes.isSelected();
		this.trimConfig.deleteUnknownAttributes = this.deleteUnknownAttributes.isSelected();
	}

	public void createButtons(String okButtonText, String okButtonToolTip) {
		this.previousButton = new JButton("Previous");
		this.previousButton.setToolTipText(HelpManager.getToolTip("PREVIOUS_SCREEN"));
		this.okButton = new JButton(okButtonText);
		this.okButton.setToolTipText(okButtonToolTip);
		this.cancelButton = new JButton("Cancel");
		this.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		this.helpButton = new JButton("Help");
		this.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
	}

	public final void showHelp() {
		HelpManager.showHelp("032");
	}

	public void initScreenContent(String title, String okButtonText, String okButtonToolTip) {
		Container contentPane = this.getContentPane();
		PowerLayout layout = this.initLayout(title, okButtonText, okButtonToolTip, contentPane);
		TrimOptionsFrame.ButtonActionListener actionListener = new TrimOptionsFrame.ButtonActionListener(this);
		this.previousButton.addActionListener(actionListener);
		this.okButton.addActionListener(actionListener);
		this.cancelButton.addActionListener(actionListener);
		this.helpButton.addActionListener(actionListener);
		TrimOptionsFrame.ButtonKeyListener keyListener = new TrimOptionsFrame.ButtonKeyListener(this);
		this.previousButton.addKeyListener(keyListener);
		this.okButton.addKeyListener(keyListener);
		this.cancelButton.addKeyListener(keyListener);
		this.helpButton.addKeyListener(keyListener);
		contentPane.add(this.mainPanel, "mainPnl");
		this.createMainPanel();
		contentPane.add(this.previousButton, "previousBtn");
		contentPane.add(this.okButton, "okBtn");
		contentPane.add(this.cancelButton, "cancelBtn");
		contentPane.add(this.helpButton, "helpBtn");
		this.showFrame(layout);
	}

	public final void onOk() throws IOException {
		if (this.validateOptions()) {
			this.skipCloseCallback = true;
			this.close();
			this.saveOptionsToConfig();
			this.resultConsumer.accept(this.trimConfig, 1);
		}
	}

	public void loadOptionsFromConfig() {
		this.deleteSourceFileAttributes.setSelected(this.trimConfig.deleteSourceFileAttributes);
		this.deleteDeprecatedAttributes.setSelected(this.trimConfig.deleteDeprecatedAttributes);
		this.deleteAnnotationAttributes.setSelected(this.trimConfig.deleteAnnotationAttributes);
		this.deleteExceptionAttributes.setSelected(this.trimConfig.deleteExceptionAttributes);
		this.deleteDebugExtensionAttributes.setSelected(this.trimConfig.deleteDebugExtensionAttributes);
		this.deleteUnknownAttributes.setSelected(this.trimConfig.deleteUnknownAttributes);
	}

	public void showFrame(PowerLayout layout) {
		layout.applyStyles(
			"mainPnl.top=0;mainPnl.left=0;mainPnl.bottom=container.bottom-btnHeight-15;mainPnl.right=container.right;layout.minWidth=btnWidth*5+30;" + this.getButtonStyles()
		);
		this.parentFrame.setEnabled(false);
		this.loadOptionsFromConfig();
		this.setIconImage(KlassMaster.getIcon(this));
		GuiUtils.packWindow(this);
		Dimension frameSize = this.getSize();
		Point parentLocation = this.parentFrame.getLocationOnScreen();
		Dimension parentSize = this.parentFrame.getSize();
		int locationX = parentSize.width / 2 - frameSize.width / 2 + parentLocation.x;
		int locationY = parentSize.height / 2 - frameSize.height / 2 + parentLocation.y;
		locationX = Math.max(0, locationX);
		locationY = Math.max(0, locationY);
		this.setLocation(locationX, locationY);
		GuiUtils.setVisible(this, true);
	}

	@Override
	public Action getAction() {
		return new TrimOptionsFrame.EscapeAction(this);
	}

	public boolean validateOptions() {
		return true;
	}

	@Override
	public final void close() {
		super.close();
		if (!this.skipCloseCallback) {
			this.resultConsumer.accept();
		}
	}

	public PowerLayout initLayout(String title, String okButtonText, String okButtonToolTip, Container contentPane) {
		this.setTitle(title);
		PowerLayout layout = new PowerLayout(contentPane);
		contentPane.setLayout(layout);
		this.createButtons(okButtonText, okButtonToolTip);
		TrimOptionsFrame.WindowClosingListener windowListener = new TrimOptionsFrame.WindowClosingListener(this);
		this.addWindowListener(windowListener);
		return layout;
	}

	public final void onPrevious() throws IOException {
		if (this.validateOptions()) {
			this.skipCloseCallback = true;
			this.close();
			this.saveOptionsToConfig();
			this.resultConsumer.accept(this.trimConfig, 2);
		}
	}

	public void createMainPanel() {
		PowerLayout layout = new PowerLayout(this.mainPanel);
		this.mainPanel.setLayout(layout);
		this.mainPanel.add(this.deleteSourceFileAttributes, "deleteSourceFileAttributesChk");
		this.mainPanel.add(this.deleteDeprecatedAttributes, "deleteDeprectedAttributesChk");
		this.mainPanel.add(this.deleteAnnotationAttributes, "deleteAnnotationAttributesChk");
		this.mainPanel.add(this.deleteExceptionAttributes, "deleteExceptionAttributesChk");
		this.mainPanel.add(this.deleteDebugExtensionAttributes, "deleteDebugExtensionAttributesChk");
		this.mainPanel.add(this.deleteUnknownAttributes, "deleteUnknownAttributesChk");
		layout.setProperties(layoutProperties);
	}

	public String getButtonStyles() {
		return "btnWidth=max(previousBtn.defaultWidth,okBtn.defaultWidth,cancelBtn.defaultWidth,helpBtn.defaultWidth);btnHeight=max(previousBtn.defaultHeight,okBtn.defaultHeight,cancelBtn.defaultHeight,helpBtn.defaultHeight);previousBtn.width=btnWidth;previousBtn.height=btnHeight;previousBtn.centerX=container.width*12/100;previousBtn.bottom=container.bottom-5;okBtn.width=btnWidth;okBtn.height=btnHeight;okBtn.centerX=container.width*37/100;okBtn.bottom=container.bottom-5;cancelBtn.width=btnWidth;cancelBtn.height=btnHeight;cancelBtn.centerX=container.width*62/100;cancelBtn.bottom=container.bottom-5;helpBtn.width=btnWidth;helpBtn.height=btnHeight;helpBtn.centerX=container.width*87/100;helpBtn.bottom=container.bottom-5;";
	}

	public final void onCancel() {
		this.skipCloseCallback = true;
		this.close();
		this.resultConsumer.accept();
	}

	public static class ButtonActionListener implements ActionListener {
		public final TrimOptionsFrame trimOptionsFrame;

		public ButtonActionListener(TrimOptionsFrame trimOptionsFrame) {
			this.trimOptionsFrame = trimOptionsFrame;
		}

		@Override
		@SneakyThrows
		public void actionPerformed(ActionEvent e) {
			Object eventSource = e.getSource();
			if (eventSource == this.trimOptionsFrame.previousButton) {
				this.trimOptionsFrame.onPrevious();
			} else if (eventSource == this.trimOptionsFrame.okButton) {
				this.trimOptionsFrame.onOk();
			} else if (eventSource == this.trimOptionsFrame.cancelButton) {
				this.trimOptionsFrame.onCancel();
			} else if (eventSource == this.trimOptionsFrame.helpButton) {
				this.trimOptionsFrame.showHelp();
			}
		}
	}

	public static class ButtonKeyListener extends KeyAdapter {
		public final TrimOptionsFrame trimOptionsFrame;

		public ButtonKeyListener(TrimOptionsFrame trimOptionsFrame) {
			this.trimOptionsFrame = trimOptionsFrame;
		}

		@Override
		@SneakyThrows
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == 10) {
				Object eventSource = e.getSource();
				if (eventSource == this.trimOptionsFrame.previousButton) {
					this.trimOptionsFrame.onPrevious();
				} else if (eventSource == this.trimOptionsFrame.okButton) {
					this.trimOptionsFrame.onOk();
				} else if (eventSource == this.trimOptionsFrame.cancelButton) {
					this.trimOptionsFrame.onCancel();
				} else if (eventSource == this.trimOptionsFrame.helpButton) {
					this.trimOptionsFrame.showHelp();
				}
			}
		}
	}

	public static class WindowClosingListener extends WindowAdapter {
		public final TrimOptionsFrame trimOptionsFrame;

		public WindowClosingListener(TrimOptionsFrame trimOptionsFrame) {
			this.trimOptionsFrame = trimOptionsFrame;
		}

		@Override
		public void windowClosing(WindowEvent e) {
			this.trimOptionsFrame.close();
			this.trimOptionsFrame.resultConsumer.accept();
		}
	}

	public static class EscapeAction extends AbstractAction {
		public final TrimOptionsFrame trimOptionsFrame;

		public EscapeAction(TrimOptionsFrame trimOptionsFrame) {
			this.trimOptionsFrame = trimOptionsFrame;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (this.trimOptionsFrame.isEnabled()) {
				this.trimOptionsFrame.onCancel();
			}
		}
	}
}
