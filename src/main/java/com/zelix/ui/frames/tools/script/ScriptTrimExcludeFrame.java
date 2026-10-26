package com.zelix.ui.frames.tools.script;

import com.zelix.HelpManager;
import com.zelix.klassmaster.config.RuleFilterConfig;
import com.zelix.klassmaster.config.TrimConfig;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.ui.GUI;
import com.zelix.ui.dataset.ClassNodeEntry;
import com.zelix.ui.frames.tools.trim.TrimExcludeFrame;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.utils.function.ZKMConsumer;
import com.zelix.workspace.Workspace;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Vector;
import javax.swing.JButton;

public class ScriptTrimExcludeFrame extends TrimExcludeFrame {
	public static String[] layoutProperties = new String[]{
		"layout.minWidth=btnWidth*5+30",
		"layout.minHeight=250",
		"mainPnl.left=5",
		"mainPnl.top=5",
		"mainPnl.right=container.right-5",
		"mainPnl.bottom=container.bottom-btnHeight-extraPnl.defaultHeight-20",
		"extraPnl.left=5",
		"extraPnl.top=mainPnl.bottom+10",
		"extraPnl.right=container.right-5",
		"btnWidth=max(previousBtn.defaultWidth, okBtn.defaultWidth, skipBtn.defaultWidth, cancelBtn.defaultWidth, helpBtn.defaultWidth)",
		"btnHeight=max(previousBtn.defaultHeight, okBtn.defaultHeight, skipBtn.defaultHeight, cancelBtn.defaultHeight, helpBtn.defaultHeight)",
		"previousBtn.top=extraPnl.bottom+5",
		"previousBtn.centerX=container.width*10/100",
		"previousBtn.width=btnWidth",
		"previousBtn.height=btnHeight",
		"okBtn.top=previousBtn.top",
		"okBtn.centerX=container.width*30/100",
		"okBtn.width=btnWidth",
		"okBtn.height=btnHeight",
		"skipBtn.top=previousBtn.top",
		"skipBtn.centerX=container.width*50/100",
		"skipBtn.width=btnWidth",
		"skipBtn.height=btnHeight",
		"cancelBtn.top=previousBtn.top",
		"cancelBtn.centerX=container.width*70/100",
		"cancelBtn.width=btnWidth",
		"cancelBtn.height=btnHeight",
		"helpBtn.top=previousBtn.top",
		"helpBtn.centerX=container.width*90/100",
		"helpBtn.width=btnWidth",
		"helpBtn.height=btnHeight"
	};
	public JButton previousButton;
	public JButton skipButton;

	public ScriptTrimExcludeFrame(
		String title, GUI gui, Vector<ClassNodeEntry> classNodes, RuleFilterConfig ruleFilterConfig, Workspace workspace, TrimConfig trimConfig, ScriptContext scriptContext, ZKMConsumer resultConsumer
	) {
		super(title, gui, classNodes, ruleFilterConfig, workspace, trimConfig, scriptContext, resultConsumer, 2);
	}

	public void onPrevious() {
		super.skipCloseCallback = true;
		this.close();
		this.applyToConfig();
		super.resultConsumer.accept(2);
	}

	public void onSkip() {
		super.skipCloseCallback = true;
		this.close();
		super.resultConsumer.accept(4);
	}

	@Override
	public void addFrameButtons(PowerLayout layout, Container container) {
		this.previousButton = new JButton("Previous");
		this.previousButton.setToolTipText(HelpManager.getToolTip("PREVIOUS_SCREEN"));
		super.okButton = new JButton("Next");
		super.okButton.setToolTipText(HelpManager.getToolTip("OK_TRIM_EXCLUDE"));
		this.skipButton = new JButton("Skip");
		this.skipButton.setToolTipText(HelpManager.getToolTip("SKIP_TO_NEXT"));
		super.cancelButton = new JButton("Cancel");
		super.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		super.helpButton = new JButton("Help");
		super.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
		ScriptTrimExcludeFrame.ButtonActionListener buttonActionListener = new ScriptTrimExcludeFrame.ButtonActionListener(this);
		this.previousButton.addActionListener(buttonActionListener);
		super.okButton.addActionListener(buttonActionListener);
		this.skipButton.addActionListener(buttonActionListener);
		super.cancelButton.addActionListener(buttonActionListener);
		super.helpButton.addActionListener(buttonActionListener);
		ScriptTrimExcludeFrame.ButtonKeyListener buttonKeyListener = new ScriptTrimExcludeFrame.ButtonKeyListener(this);
		this.previousButton.addKeyListener(buttonKeyListener);
		super.okButton.addKeyListener(buttonKeyListener);
		this.skipButton.addKeyListener(buttonKeyListener);
		super.cancelButton.addKeyListener(buttonKeyListener);
		super.helpButton.addKeyListener(buttonKeyListener);
		container.add(this.previousButton, "previousBtn");
		container.add(super.okButton, "okBtn");
		container.add(this.skipButton, "skipBtn");
		container.add(super.cancelButton, "cancelBtn");
		container.add(super.helpButton, "helpBtn");
		layout.setProperties(layoutProperties);
	}

	public static class ButtonKeyListener extends KeyAdapter {
		public final ScriptTrimExcludeFrame scriptTrimExcludeFrame;

		public ButtonKeyListener(ScriptTrimExcludeFrame scriptTrimExcludeFrame) {
			this.scriptTrimExcludeFrame = scriptTrimExcludeFrame;
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == 10) {
				if (e.getSource() == this.scriptTrimExcludeFrame.previousButton) {
					this.scriptTrimExcludeFrame.onPrevious();
				} else if (e.getSource() == this.scriptTrimExcludeFrame.okButton) {
					this.scriptTrimExcludeFrame.onOk();
				} else if (e.getSource() == this.scriptTrimExcludeFrame.skipButton) {
					this.scriptTrimExcludeFrame.onSkip();
				} else if (e.getSource() == this.scriptTrimExcludeFrame.cancelButton) {
					this.scriptTrimExcludeFrame.onCancel();
				} else if (e.getSource() == this.scriptTrimExcludeFrame.helpButton) {
					this.scriptTrimExcludeFrame.showHelp();
				}
			}
		}
	}

	public static class ButtonActionListener implements ActionListener {
		public final ScriptTrimExcludeFrame scriptTrimExcludeFrame;

		public ButtonActionListener(ScriptTrimExcludeFrame scriptTrimExcludeFrame) {
			this.scriptTrimExcludeFrame = scriptTrimExcludeFrame;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Object eventSource = e.getSource();
			if (eventSource == this.scriptTrimExcludeFrame.previousButton) {
				this.scriptTrimExcludeFrame.onPrevious();
			} else if (eventSource == this.scriptTrimExcludeFrame.okButton) {
				this.scriptTrimExcludeFrame.onOk();
			} else if (eventSource == this.scriptTrimExcludeFrame.skipButton) {
				this.scriptTrimExcludeFrame.onSkip();
			} else if (eventSource == this.scriptTrimExcludeFrame.cancelButton) {
				this.scriptTrimExcludeFrame.onCancel();
			} else if (eventSource == this.scriptTrimExcludeFrame.helpButton) {
				this.scriptTrimExcludeFrame.showHelp();
			}
		}
	}
}
