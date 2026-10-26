package com.zelix.ui.frames.tools.obfuscate;

import com.zelix.HelpManager;
import com.zelix.klassmaster.config.FilterConfig;
import com.zelix.klassmaster.config.ObfuscateConfig;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.ui.GUI;
import com.zelix.utils.function.ZKMConsumer;
import javax.swing.JButton;

public class WrappedObfuscateOptionsFrame extends ObfuscateOptionsFrame {
	public WrappedObfuscateOptionsFrame(String title, String okButtonToolTip, GUI gui, ObfuscateConfig obfuscateConfig, FilterConfig filterConfig, ScriptContext scriptContext, ZKMConsumer resultConsumer) {
		super(title, okButtonToolTip, gui, obfuscateConfig, filterConfig, scriptContext, resultConsumer);
	}

	@Override
	public void createButtons(String okButtonToolTip) {
		super.previousButton = new JButton("Previous");
		super.previousButton.setToolTipText(HelpManager.getToolTip("PREVIOUS_SCREEN"));
		super.okButton = new JButton("Next");
		super.okButton.setToolTipText(okButtonToolTip);
		super.cancelButton = new JButton("Cancel");
		super.cancelButton.setToolTipText(HelpManager.getToolTip("CANCEL_DIALOG"));
		super.helpButton = new JButton("Help");
		super.helpButton.setToolTipText(HelpManager.getToolTip("SCREEN_HELP"));
	}
}
