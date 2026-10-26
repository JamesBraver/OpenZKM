package com.zelix.ui.frames.tools.obfuscate;

import com.zelix.klassmaster.config.FilterConfig;
import com.zelix.klassmaster.config.ObfuscateConfig;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.ui.GUI;
import com.zelix.utils.function.ZKMConsumer;
import java.io.File;

public class ScriptObfuscateOptionsFrame extends WrappedObfuscateOptionsFrame {
	public ScriptObfuscateOptionsFrame(String title, String okButtonToolTip, GUI gui, ObfuscateConfig obfuscateConfig, FilterConfig filterConfig, ScriptContext scriptContext, ZKMConsumer resultConsumer) {
		super(title, okButtonToolTip, gui, obfuscateConfig, filterConfig, scriptContext, resultConsumer);
	}

	@Override
	protected boolean openChangeLogIn(String fileName, File file) {
		return true;
	}

	@Override
	protected boolean openChangeLogOut(String fileName, File file) {
		return true;
	}
}
