package com.zelix.ui.dialogs.stacktrace;

import com.zelix.HelpManager;
import com.zelix.UIConfig;
import com.zelix.repository.ClasspathManager;
import com.zelix.ui.dialogs.ClasspathFileListDialog;
import com.zelix.utils.function.ZKMConsumer;
import javax.swing.JFrame;

public class ObfuscatedBytecodeListDialog extends ClasspathFileListDialog {
	public ObfuscatedBytecodeListDialog(JFrame parentFrame, String title, ClasspathManager classpathManager, String message, String okButtonText, String okButtonToolTip, UIConfig uiConfig, ZKMConsumer resultConsumer) {
		super(parentFrame, title, classpathManager, message, okButtonText, okButtonToolTip, uiConfig, resultConsumer);
	}

	@Override
	public void saveLastClasspath(String classpath) {
		super.uiConfig.setLastClasspathPath(classpath);
		super.uiConfig.saveConfig();
	}

	@Override
	protected void openHelpGUI() {
		HelpManager.showHelp("038");
	}
}
