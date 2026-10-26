package com.zelix.ui.frames.tools;

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

public class TrimFrame extends WrappedTrimExcludeFrame {
	public static String[] layoutProperties = new String[]{
		"layout.minWidth=btnWidth*6+35",
		"layout.minHeight=250",
		"mainPnl.left=5",
		"mainPnl.top=5",
		"mainPnl.right=container.right-5",
		"mainPnl.bottom=container.bottom-btnHeight-extraPnl.defaultHeight-20",
		"extraPnl.left=5",
		"extraPnl.top=mainPnl.bottom+5",
		"extraPnl.right=container.right-5",
		"btnWidth=max(okBtn.defaultWidth, testBtn.defaultWidth, cancelBtn.defaultWidth, helpBtn.defaultWidth)",
		"btnHeight=max(, okBtn.defaultHeight, testBtn.defaultHeight, cancelBtn.defaultHeight, helpBtn.defaultHeight)",
		"okBtn.top=extraPnl.bottom+5",
		"okBtn.centerX=container.width*12/100",
		"okBtn.width=btnWidth",
		"okBtn.height=btnHeight",
		"testBtn.top=okBtn.top",
		"testBtn.centerX=container.width*37/100",
		"testBtn.width=btnWidth",
		"testBtn.height=btnHeight",
		"cancelBtn.top=okBtn.top",
		"cancelBtn.centerX=container.width*62/100",
		"cancelBtn.width=btnWidth",
		"cancelBtn.height=btnHeight",
		"helpBtn.top=okBtn.top",
		"helpBtn.centerX=container.width*87/100",
		"helpBtn.width=btnWidth",
		"helpBtn.height=btnHeight"
	};

	public TrimFrame(
		String title, GUI gui, List<ClassNodeEntry> classNodes, RuleFilterConfig ruleFilterConfig, Workspace workspace, TrimConfig trimConfig, ScriptContext scriptContext, ZKMConsumer resultConsumer
	) {
		super(title, gui, classNodes, ruleFilterConfig, workspace, trimConfig, scriptContext, resultConsumer, 2);
	}

	@Override
	public void addFrameButtons(PowerLayout layout, Container container) {
		super.okButton = new JButton("Next");
		super.okButton.setToolTipText(HelpManager.getToolTip("OK_TRIM_EXCLUDE"));
		super.testButton = new JButton("Test");
		super.testButton.setToolTipText(HelpManager.getToolTip("TEST_TRIM_EXCLUDE"));
		super.cancelButton = new JButton("Cancel");
		super.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		super.helpButton = new JButton("Help");
		super.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
		TrimFrame.ButtonActionListener buttonActionListener = new TrimFrame.ButtonActionListener(this);
		super.okButton.addActionListener(buttonActionListener);
		super.testButton.addActionListener(buttonActionListener);
		super.cancelButton.addActionListener(buttonActionListener);
		super.helpButton.addActionListener(buttonActionListener);
		TrimFrame.ButtonEnterKeyListener buttonEnterKeyListener = new TrimFrame.ButtonEnterKeyListener(this);
		super.okButton.addKeyListener(buttonEnterKeyListener);
		super.testButton.addKeyListener(buttonEnterKeyListener);
		super.cancelButton.addKeyListener(buttonEnterKeyListener);
		super.helpButton.addKeyListener(buttonEnterKeyListener);
		container.add(super.okButton, "okBtn");
		container.add(super.testButton, "testBtn");
		container.add(super.cancelButton, "cancelBtn");
		container.add(super.helpButton, "helpBtn");
		layout.setProperties(layoutProperties);
	}

	public static class ButtonEnterKeyListener extends KeyAdapter {
		public final TrimFrame trimFrame;

		public ButtonEnterKeyListener(TrimFrame trimFrame) {
			this.trimFrame = trimFrame;
		}

		@Override
		@SneakyThrows
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == 10) {
				if (e.getSource() == this.trimFrame.okButton) {
					this.trimFrame.onOk();
				} else if (e.getSource() == this.trimFrame.testButton) {
					this.trimFrame.onTestTrim();
				} else if (e.getSource() == this.trimFrame.cancelButton) {
					this.trimFrame.onCancel();
				} else if (e.getSource() == this.trimFrame.helpButton) {
					this.trimFrame.showHelp();
				}
			}
		}
	}

	public static class ButtonActionListener implements ActionListener {
		public final TrimFrame trimFrame;

		public ButtonActionListener(TrimFrame trimFrame) {
			this.trimFrame = trimFrame;
		}

		@Override
		@SneakyThrows
		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if (source == this.trimFrame.okButton) {
				this.trimFrame.onOk();
			} else if (source == this.trimFrame.testButton) {
				this.trimFrame.onTestTrim();
			} else if (source == this.trimFrame.cancelButton) {
				this.trimFrame.onCancel();
			} else if (source == this.trimFrame.helpButton) {
				this.trimFrame.showHelp();
			}
		}
	}
}
