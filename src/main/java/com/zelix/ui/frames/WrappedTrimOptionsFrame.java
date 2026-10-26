package com.zelix.ui.frames;

import lombok.SneakyThrows;

import com.zelix.HelpManager;
import com.zelix.klassmaster.config.TrimConfig;
import com.zelix.ui.frames.tools.trim.TrimOptionsFrame;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.utils.function.ZKMConsumer;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFrame;

public class WrappedTrimOptionsFrame extends TrimOptionsFrame {
	public JButton skipButton;

	public WrappedTrimOptionsFrame(String title, String okButtonText, String okButtonToolTip, JFrame parentFrame, TrimConfig trimConfig, ZKMConsumer resultConsumer) {
		super(title, okButtonText, okButtonToolTip, parentFrame, trimConfig, resultConsumer);
	}

	public void onSkip() throws IOException {
		super.skipCloseCallback = true;
		this.close();
		super.resultConsumer.accept(super.trimConfig, 4);
	}

	@Override
	public void createButtons(String okButtonText, String okButtonToolTip) {
		super.previousButton = new JButton("Previous");
		super.previousButton.setToolTipText(HelpManager.getToolTip("PREVIOUS_SCREEN"));
		super.okButton = new JButton(okButtonText);
		super.okButton.setToolTipText(okButtonToolTip);
		this.skipButton = new JButton("Skip");
		this.skipButton.setToolTipText(HelpManager.getToolTip("SKIP_TO_NEXT"));
		super.cancelButton = new JButton("Cancel");
		super.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		super.helpButton = new JButton("Help");
		super.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
	}

	@Override
	public void initScreenContent(String title, String okButtonText, String okButtonToolTip) {
		Container contentPane = this.getContentPane();
		PowerLayout layout = this.initLayout(title, okButtonText, okButtonToolTip, contentPane);
		WrappedTrimOptionsFrame.ButtonActionListener buttonActionListener = new WrappedTrimOptionsFrame.ButtonActionListener(this);
		super.previousButton.addActionListener(buttonActionListener);
		super.okButton.addActionListener(buttonActionListener);
		this.skipButton.addActionListener(buttonActionListener);
		super.cancelButton.addActionListener(buttonActionListener);
		super.helpButton.addActionListener(buttonActionListener);
		WrappedTrimOptionsFrame.ButtonKeyListener buttonKeyListener = new WrappedTrimOptionsFrame.ButtonKeyListener(this);
		super.previousButton.addKeyListener(buttonKeyListener);
		super.okButton.addKeyListener(buttonKeyListener);
		this.skipButton.addKeyListener(buttonKeyListener);
		super.cancelButton.addKeyListener(buttonKeyListener);
		super.helpButton.addKeyListener(buttonKeyListener);
		contentPane.add(super.mainPanel, "mainPnl");
		this.createMainPanel();
		contentPane.add(super.previousButton, "previousBtn");
		contentPane.add(super.okButton, "okBtn");
		contentPane.add(this.skipButton, "skipBtn");
		contentPane.add(super.cancelButton, "cancelBtn");
		contentPane.add(super.helpButton, "helpBtn");
		this.showFrame(layout);
	}

	@Override
	public String getButtonStyles() {
		return "btnWidth=max(previousBtn.defaultWidth,okBtn.defaultWidth,skipBtn.defaultWidth,cancelBtn.defaultWidth,helpBtn.defaultWidth);btnHeight=max(previousBtn.defaultHeight,okBtn.defaultHeight,skipBtn.defaultHeight,cancelBtn.defaultHeight,helpBtn.defaultHeight);previousBtn.width=btnWidth;previousBtn.height=btnHeight;previousBtn.centerX=container.width*10/100;previousBtn.bottom=container.bottom-5;okBtn.width=btnWidth;okBtn.height=btnHeight;okBtn.centerX=container.width*30/100;okBtn.bottom=container.bottom-5;skipBtn.width=btnWidth;skipBtn.height=btnHeight;skipBtn.centerX=container.width*50/100;skipBtn.bottom=container.bottom-5;cancelBtn.width=btnWidth;cancelBtn.height=btnHeight;cancelBtn.centerX=container.width*70/100;cancelBtn.bottom=container.bottom-5;helpBtn.width=btnWidth;helpBtn.height=btnHeight;helpBtn.centerX=container.width*90/100;helpBtn.bottom=container.bottom-5;";
	}

	public static class ButtonActionListener implements ActionListener {
		public final WrappedTrimOptionsFrame scriptTrimFrame;

		public ButtonActionListener(WrappedTrimOptionsFrame scriptTrimFrame) {
			this.scriptTrimFrame = scriptTrimFrame;
		}

		@Override
		@SneakyThrows
		public void actionPerformed(ActionEvent e) {
			Object eventSource = e.getSource();
			if (eventSource == this.scriptTrimFrame.previousButton) {
				this.scriptTrimFrame.onPrevious();
			} else if (eventSource == this.scriptTrimFrame.okButton) {
				this.scriptTrimFrame.onOk();
			} else if (eventSource == this.scriptTrimFrame.skipButton) {
				this.scriptTrimFrame.onSkip();
			} else if (eventSource == this.scriptTrimFrame.cancelButton) {
				this.scriptTrimFrame.onCancel();
			} else if (eventSource == this.scriptTrimFrame.helpButton) {
				this.scriptTrimFrame.showHelp();
			}
		}
	}

	public static class ButtonKeyListener extends KeyAdapter {
		public final WrappedTrimOptionsFrame scriptTrimFrame;

		public ButtonKeyListener(WrappedTrimOptionsFrame scriptTrimFrame) {
			this.scriptTrimFrame = scriptTrimFrame;
		}

		@Override
		@SneakyThrows
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == 10) {
				Object eventSource = e.getSource();
				if (eventSource == this.scriptTrimFrame.previousButton) {
					this.scriptTrimFrame.onPrevious();
				} else if (eventSource == this.scriptTrimFrame.okButton) {
					this.scriptTrimFrame.onOk();
				} else if (eventSource == this.scriptTrimFrame.skipButton) {
					this.scriptTrimFrame.onSkip();
				} else if (eventSource == this.scriptTrimFrame.cancelButton) {
					this.scriptTrimFrame.onCancel();
				} else if (eventSource == this.scriptTrimFrame.helpButton) {
					this.scriptTrimFrame.showHelp();
				}
			}
		}
	}
}
