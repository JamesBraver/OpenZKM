package com.zelix.ui.frames.tools.script;

import com.zelix.HelpManager;
import com.zelix.klassmaster.config.FilterConfig;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.ui.GUI;
import com.zelix.ui.dataset.ClassNodeEntry;
import com.zelix.ui.frames.ExcludeFrame;
import com.zelix.ui.layouts.PowerLayout;
import com.zelix.utils.function.ZKMConsumer;
import com.zelix.workspace.PackageManager;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Vector;
import javax.swing.JButton;

public class ScriptObfuscateExcludeFrame extends ExcludeFrame {
	public static String[] layoutProperties = new String[]{
		"layout.minWidth=btnWidth*4+25",
		"layout.minHeight=250",
		"mainPnl.left=5",
		"mainPnl.top=5",
		"mainPnl.right=container.right-5",
		"mainPnl.bottom=container.bottom-btnHeight-extraPnl.defaultHeight-20",
		"extraPnl.left=5",
		"extraPnl.top=mainPnl.bottom+5",
		"extraPnl.right=container.right-5",
		"btnWidth=max(previousBtn.defaultWidth, okBtn.defaultWidth, cancelBtn.defaultWidth, helpBtn.defaultWidth)",
		"btnHeight=max(previousBtn.defaultHeight, okBtn.defaultHeight, cancelBtn.defaultHeight, helpBtn.defaultHeight)",
		"previousBtn.top=extraPnl.bottom+5",
		"previousBtn.centerX=container.width*12/100",
		"previousBtn.width=btnWidth",
		"previousBtn.height=btnHeight",
		"okBtn.top=previousBtn.top",
		"okBtn.centerX=container.width*37/100",
		"okBtn.width=btnWidth",
		"okBtn.height=btnHeight",
		"cancelBtn.top=previousBtn.top",
		"cancelBtn.centerX=container.width*62/100",
		"cancelBtn.width=btnWidth",
		"cancelBtn.height=btnHeight",
		"helpBtn.top=previousBtn.top",
		"helpBtn.centerX=container.width*87/100",
		"helpBtn.width=btnWidth",
		"helpBtn.height=btnHeight"
	};
	public JButton previousButton;

	public ScriptObfuscateExcludeFrame(
		String title, GUI gui, FilterConfig filterConfig, PackageManager packageManager, Vector<ClassNodeEntry> classNodes, ScriptContext scriptContext, ZKMConsumer resultConsumer
	) {
		super(title, gui, filterConfig, packageManager, classNodes, scriptContext, resultConsumer, 1);
	}

	public final void onPrevious() {
		if (this.validateParameters()) {
			super.skipCloseCallback = true;
			this.close();
			this.applyToConfig();
			super.resultConsumer.accept(2);
		}
	}

	@Override
	public void addFrameButtons(PowerLayout layout, Container container) {
		this.previousButton = new JButton("Previous");
		this.previousButton.setToolTipText(HelpManager.getToolTip("PREVIOUS_SCREEN"));
		super.okButton = new JButton("Next");
		super.okButton.setToolTipText(HelpManager.getToolTip("OK_EXCLUDE"));
		super.cancelButton = new JButton("Cancel");
		super.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		super.helpButton = new JButton("Help");
		super.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
		ScriptObfuscateExcludeFrame.ButtonActionListener buttonActionListener = new ScriptObfuscateExcludeFrame.ButtonActionListener(this);
		this.previousButton.addActionListener(buttonActionListener);
		super.okButton.addActionListener(buttonActionListener);
		super.cancelButton.addActionListener(buttonActionListener);
		super.helpButton.addActionListener(buttonActionListener);
		ScriptObfuscateExcludeFrame.ButtonKeyListener buttonKeyListener = new ScriptObfuscateExcludeFrame.ButtonKeyListener(this);
		this.previousButton.addKeyListener(buttonKeyListener);
		super.okButton.addKeyListener(buttonKeyListener);
		super.cancelButton.addKeyListener(buttonKeyListener);
		super.helpButton.addKeyListener(buttonKeyListener);
		container.add(this.previousButton, "previousBtn");
		container.add(super.okButton, "okBtn");
		container.add(super.cancelButton, "cancelBtn");
		container.add(super.helpButton, "helpBtn");
		layout.setProperties(layoutProperties);
	}

	public static class ButtonKeyListener extends KeyAdapter {
		public final ScriptObfuscateExcludeFrame scriptObfuscateExcludeFrame;

		public ButtonKeyListener(ScriptObfuscateExcludeFrame scriptObfuscateExcludeFrame) {
			this.scriptObfuscateExcludeFrame = scriptObfuscateExcludeFrame;
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == 10) {
				if (e.getSource() == this.scriptObfuscateExcludeFrame.previousButton) {
					this.scriptObfuscateExcludeFrame.onPrevious();
				} else if (e.getSource() == this.scriptObfuscateExcludeFrame.okButton) {
					this.scriptObfuscateExcludeFrame.onOk();
				} else if (e.getSource() == this.scriptObfuscateExcludeFrame.cancelButton) {
					this.scriptObfuscateExcludeFrame.onCancel();
				} else if (e.getSource() == this.scriptObfuscateExcludeFrame.helpButton) {
					this.scriptObfuscateExcludeFrame.showHelp();
				}
			}
		}
	}

	public static class ButtonActionListener implements ActionListener {
		public final ScriptObfuscateExcludeFrame scriptObfuscateExcludeFrame;

		public ButtonActionListener(ScriptObfuscateExcludeFrame scriptObfuscateExcludeFrame) {
			this.scriptObfuscateExcludeFrame = scriptObfuscateExcludeFrame;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Object eventSource = e.getSource();
			if (eventSource == this.scriptObfuscateExcludeFrame.previousButton) {
				this.scriptObfuscateExcludeFrame.onPrevious();
			} else if (eventSource == this.scriptObfuscateExcludeFrame.okButton) {
				this.scriptObfuscateExcludeFrame.onOk();
			} else if (eventSource == this.scriptObfuscateExcludeFrame.cancelButton) {
				this.scriptObfuscateExcludeFrame.onCancel();
			} else if (eventSource == this.scriptObfuscateExcludeFrame.helpButton) {
				this.scriptObfuscateExcludeFrame.showHelp();
			}
		}
	}
}
