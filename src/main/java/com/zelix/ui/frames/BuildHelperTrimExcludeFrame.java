package com.zelix.ui.frames;

import lombok.SneakyThrows;

import com.zelix.HelpManager;
import com.zelix.klassmaster.config.RuleFilterConfig;
import com.zelix.klassmaster.config.TrimConfig;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.ui.GUI;
import com.zelix.ui.dataset.ClassNodeEntry;
import com.zelix.ui.frames.tools.trim.WrappedTrimExcludeFrame;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.utils.function.ZKMConsumer;
import com.zelix.workspace.Workspace;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.JButton;

public class BuildHelperTrimExcludeFrame extends WrappedTrimExcludeFrame {
	public static String[] frameLayoutProperties = new String[]{
		"layout.minWidth=btnWidth*6+35",
		"layout.minHeight=250",
		"mainPnl.left=5",
		"mainPnl.top=5",
		"mainPnl.right=container.right-5",
		"mainPnl.bottom=container.bottom-btnHeight-extraPnl.defaultHeight-20",
		"extraPnl.left=5",
		"extraPnl.top=mainPnl.bottom+10",
		"extraPnl.right=container.right-5",
		"btnWidth=max(helpBtn.defaultWidth, previousBtn.defaultWidth, okBtn.defaultWidth, testBtn.defaultWidth, skipBtn.defaultWidth, cancelBtn.defaultWidth)",
		"btnHeight=max(helpBtn.defaultHeight, previousBtn.defaultHeight, okBtn.defaultHeight, testBtn.defaultHeight, skipBtn.defaultHeight, cancelBtn.defaultHeight)",
		"previousBtn.top=extraPnl.bottom+5",
		"previousBtn.centerX=container.width*10/100",
		"previousBtn.width=btnWidth",
		"previousBtn.height=btnHeight",
		"okBtn.top=previousBtn.top",
		"okBtn.centerX=container.width*26/100",
		"okBtn.width=btnWidth",
		"okBtn.height=btnHeight",
		"testBtn.top=previousBtn.top",
		"testBtn.centerX=container.width*42/100",
		"testBtn.width=btnWidth",
		"testBtn.height=btnHeight",
		"skipBtn.top=previousBtn.top",
		"skipBtn.centerX=container.width*58/100",
		"skipBtn.width=btnWidth",
		"skipBtn.height=btnHeight",
		"cancelBtn.top=previousBtn.top",
		"cancelBtn.centerX=container.width*74/100",
		"cancelBtn.width=btnWidth",
		"cancelBtn.height=btnHeight",
		"helpBtn.top=previousBtn.top",
		"helpBtn.centerX=container.width*90/100",
		"helpBtn.width=btnWidth",
		"helpBtn.height=btnHeight"
	};
	public JButton skipButton;
	public JButton previousButton;

	public BuildHelperTrimExcludeFrame(
		String title, GUI gui, List<ClassNodeEntry> classNodes, RuleFilterConfig ruleFilterConfig, Workspace workspace, TrimConfig trimConfig, ScriptContext scriptContext, ZKMConsumer resultConsumer
	) {
		super(title, gui, classNodes, ruleFilterConfig, workspace, trimConfig, scriptContext, resultConsumer, 2);
	}

	public void onSkip() {
		super.skipCloseCallback = true;
		this.close();
		super.resultConsumer.accept(4);
	}

	public final void onPrevious() {
		super.skipCloseCallback = true;
		this.close();
		this.applyToConfig();
		super.resultConsumer.accept(2);
	}

	@Override
	public void addFrameButtons(PowerLayout layout, Container container) {
		this.previousButton = new JButton("Previous");
		this.previousButton.setToolTipText(HelpManager.getToolTip("PREVIOUS_SCREEN"));
		super.okButton = new JButton("Next");
		super.okButton.setToolTipText(HelpManager.getToolTip("OK_TRIM_EXCLUDE"));
		super.testButton = new JButton("Test");
		super.testButton.setToolTipText(HelpManager.getToolTip("TEST_TRIM_EXCLUDE"));
		this.skipButton = new JButton("Skip");
		this.skipButton.setToolTipText(HelpManager.getToolTip("SKIP_TO_NEXT"));
		super.cancelButton = new JButton("Cancel");
		super.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		super.helpButton = new JButton("Help");
		super.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
		BuildHelperTrimExcludeFrame.ButtonActionListener buttonActionListener = new BuildHelperTrimExcludeFrame.ButtonActionListener(this);
		this.previousButton.addActionListener(buttonActionListener);
		super.okButton.addActionListener(buttonActionListener);
		super.testButton.addActionListener(buttonActionListener);
		this.skipButton.addActionListener(buttonActionListener);
		super.cancelButton.addActionListener(buttonActionListener);
		super.helpButton.addActionListener(buttonActionListener);
		BuildHelperTrimExcludeFrame.ButtonKeyListener buttonKeyListener = new BuildHelperTrimExcludeFrame.ButtonKeyListener(this);
		this.previousButton.addKeyListener(buttonKeyListener);
		super.okButton.addKeyListener(buttonKeyListener);
		super.testButton.addKeyListener(buttonKeyListener);
		this.skipButton.addKeyListener(buttonKeyListener);
		super.cancelButton.addKeyListener(buttonKeyListener);
		super.helpButton.addKeyListener(buttonKeyListener);
		container.add(this.previousButton, "previousBtn");
		container.add(super.okButton, "okBtn");
		container.add(super.testButton, "testBtn");
		container.add(this.skipButton, "skipBtn");
		container.add(super.cancelButton, "cancelBtn");
		container.add(super.helpButton, "helpBtn");
		layout.setProperties(frameLayoutProperties);
	}

	public static class ButtonKeyListener extends KeyAdapter {
		public final BuildHelperTrimExcludeFrame buildHelperTrimExcludeFrame;

		public ButtonKeyListener(BuildHelperTrimExcludeFrame buildHelperTrimExcludeFrame) {
			this.buildHelperTrimExcludeFrame = buildHelperTrimExcludeFrame;
		}

		@Override
		@SneakyThrows
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == 10) {
				Object eventSource = e.getSource();
				if (e.getSource() == this.buildHelperTrimExcludeFrame.previousButton) {
					this.buildHelperTrimExcludeFrame.onPrevious();
				} else if (eventSource == this.buildHelperTrimExcludeFrame.okButton) {
					this.buildHelperTrimExcludeFrame.onOk();
				} else if (eventSource == this.buildHelperTrimExcludeFrame.testButton) {
					this.buildHelperTrimExcludeFrame.onTestTrim();
				} else if (eventSource == this.buildHelperTrimExcludeFrame.skipButton) {
					this.buildHelperTrimExcludeFrame.onSkip();
				} else if (eventSource == this.buildHelperTrimExcludeFrame.cancelButton) {
					this.buildHelperTrimExcludeFrame.onCancel();
				} else if (eventSource == this.buildHelperTrimExcludeFrame.helpButton) {
					this.buildHelperTrimExcludeFrame.showHelp();
				}
			}
		}
	}

	public static class ButtonActionListener implements ActionListener {
		public final BuildHelperTrimExcludeFrame buildHelperTrimExcludeFrame;

		public ButtonActionListener(BuildHelperTrimExcludeFrame buildHelperTrimExcludeFrame) {
			this.buildHelperTrimExcludeFrame = buildHelperTrimExcludeFrame;
		}

		@Override
		@SneakyThrows
		public void actionPerformed(ActionEvent e) {
			Object eventSource = e.getSource();
			if (eventSource == this.buildHelperTrimExcludeFrame.previousButton) {
				this.buildHelperTrimExcludeFrame.onPrevious();
			} else if (eventSource == this.buildHelperTrimExcludeFrame.okButton) {
				this.buildHelperTrimExcludeFrame.onOk();
			} else if (eventSource == this.buildHelperTrimExcludeFrame.testButton) {
				this.buildHelperTrimExcludeFrame.onTestTrim();
			} else if (eventSource == this.buildHelperTrimExcludeFrame.skipButton) {
				this.buildHelperTrimExcludeFrame.onSkip();
			} else if (eventSource == this.buildHelperTrimExcludeFrame.cancelButton) {
				this.buildHelperTrimExcludeFrame.onCancel();
			} else if (eventSource == this.buildHelperTrimExcludeFrame.helpButton) {
				this.buildHelperTrimExcludeFrame.showHelp();
			}
		}
	}
}
