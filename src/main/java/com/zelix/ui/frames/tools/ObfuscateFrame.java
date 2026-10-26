package com.zelix.ui.frames.tools;

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
import java.util.List;
import javax.swing.JButton;

public class ObfuscateFrame extends ExcludeFrame {
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
		"btnWidth=max(okBtn.defaultWidth, cancelBtn.defaultWidth, helpBtn.defaultWidth)",
		"btnHeight=max(okBtn.defaultHeight, cancelBtn.defaultHeight, helpBtn.defaultHeight)",
		"okBtn.top=extraPnl.bottom+5",
		"okBtn.centerX=container.width*16/100",
		"okBtn.width=btnWidth",
		"okBtn.height=btnHeight",
		"cancelBtn.top=okBtn.top",
		"cancelBtn.centerX=container.width*50/100",
		"cancelBtn.width=btnWidth",
		"cancelBtn.height=btnHeight",
		"helpBtn.top=okBtn.top",
		"helpBtn.centerX=container.width*84/100",
		"helpBtn.width=btnWidth",
		"helpBtn.height=btnHeight"
	};

	public ObfuscateFrame(String title, GUI gui, FilterConfig filterConfig, PackageManager packageManager, List<ClassNodeEntry> classNodes, ScriptContext scriptContext, ZKMConsumer resultConsumer) {
		super(title, gui, filterConfig, packageManager, classNodes, scriptContext, resultConsumer, 1);
	}

	@Override
	public void addFrameButtons(PowerLayout layout, Container container) {
		super.okButton = new JButton("Next");
		super.okButton.setToolTipText(HelpManager.getToolTip("OK_EXCLUDE"));
		super.cancelButton = new JButton("Cancel");
		super.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		super.helpButton = new JButton("Help");
		super.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
		ObfuscateFrame.ButtonActionListener buttonActionListener = new ObfuscateFrame.ButtonActionListener(this);
		super.okButton.addActionListener(buttonActionListener);
		super.cancelButton.addActionListener(buttonActionListener);
		super.helpButton.addActionListener(buttonActionListener);
		ObfuscateFrame.ButtonEnterKeyListener buttonEnterKeyListener = new ObfuscateFrame.ButtonEnterKeyListener(this);
		super.okButton.addKeyListener(buttonEnterKeyListener);
		super.cancelButton.addKeyListener(buttonEnterKeyListener);
		super.helpButton.addKeyListener(buttonEnterKeyListener);
		container.add(super.okButton, "okBtn");
		container.add(super.cancelButton, "cancelBtn");
		container.add(super.helpButton, "helpBtn");
		layout.setProperties(layoutProperties);
	}

	public static class ButtonActionListener implements ActionListener {
		public final ObfuscateFrame obfuscateFrame;

		public ButtonActionListener(ObfuscateFrame obfuscateFrame) {
			this.obfuscateFrame = obfuscateFrame;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Object source = e.getSource();
			if (source == this.obfuscateFrame.okButton) {
				this.obfuscateFrame.onOk();
			} else if (source == this.obfuscateFrame.cancelButton) {
				this.obfuscateFrame.onCancel();
			} else if (source == this.obfuscateFrame.helpButton) {
				this.obfuscateFrame.showHelp();
			}
		}
	}

	public static class ButtonEnterKeyListener extends KeyAdapter {
		public final ObfuscateFrame obfuscateFrame;

		public ButtonEnterKeyListener(ObfuscateFrame obfuscateFrame) {
			this.obfuscateFrame = obfuscateFrame;
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == 10) {
				if (e.getSource() == this.obfuscateFrame.okButton) {
					this.obfuscateFrame.onOk();
				} else if (e.getSource() == this.obfuscateFrame.cancelButton) {
					this.obfuscateFrame.onCancel();
				} else if (e.getSource() == this.obfuscateFrame.helpButton) {
					this.obfuscateFrame.showHelp();
				}
			}
		}
	}
}
