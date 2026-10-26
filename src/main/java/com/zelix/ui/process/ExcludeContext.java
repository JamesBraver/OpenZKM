package com.zelix.ui.process;

import com.zelix.exceptions.ClasspathException;
import com.zelix.exceptions.FileException;
import com.zelix.klassmaster.config.FilterConfig;
import com.zelix.klassmaster.config.ObfuscateConfig;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.ui.GUI;
import com.zelix.ui.dataset.ClassNodeEntry;
import com.zelix.ui.frames.tools.ObfuscateFrame;
import com.zelix.ui.prompts.CopyablePrompt;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.function.OptionalConsumer;
import com.zelix.utils.function.ZKMConsumer;
import com.zelix.workspace.PackageManager;
import java.util.List;

public abstract class ExcludeContext {
	public GUI gui;
	public PackageManager packageManager;
	public ScriptContext scriptContext;
	public final FilterConfig excludeConfig;
	public ObfuscateConfig obfuscateConfig;

	public ExcludeContext(GUI gui, PackageManager packageManager, ScriptContext scriptContext) {
		this.gui = gui;
		this.packageManager = packageManager;
		this.scriptContext = scriptContext;
		gui.setEnabled(false);
		gui.setDisable(true);
		this.excludeConfig = FilterConfig.loadFromFile(SystemConstants.userDirectory, "ZKM_EX.ser");
		this.showNameExclusionsFrame();
	}

	public final void restoreGui() {
		this.gui.setEnabled(true);
		this.gui.toFront();
		this.gui.setDisable(false);
	}

	public abstract String getToolName();

	public void handleExclusionsResult(Integer choice, ZKMConsumer listener) {
		if (choice == 1) {
			this.obfuscateConfig = new ObfuscateConfig(SystemConstants.userDirectory, this.getObfuscateConfigFileName());
			this.excludeConfig.saveToFile(SystemConstants.userDirectory, "ZKM_EX.ser");
			this.showObfuscateOptions(CopyrightConstants.NAME + " - " + this.getToolName() + " Options", this.gui, this.obfuscateConfig, listener);
		}
	}

	public abstract void handleOptionsResult(ObfuscateConfig config, Integer choice);

	public final void showNameExclusionsFrame() {
		ExcludeContext.ObfuscateExcludeListener obfuscateExcludeListener = new ExcludeContext.ObfuscateExcludeListener(this);
		List<ClassNodeEntry> mainClassEntries = null;

		try {
			mainClassEntries = this.gui.getMainMethodClasses();
		} catch (ClasspathException classpathException) {
			new CopyablePrompt(
				this.gui,
				"Classpath Error",
				"Error while determining classes with main methods : Class '" + GlobalUtils.internalToBinaryName(classpathException.getClassName()) + "' not found."
			);
		} catch (FileException fileException) {
			new CopyablePrompt(this.gui, "File Error", "Error while determining classes with main methods : '" + fileException.getMessage() + "'");
		}

		new ObfuscateFrame(CopyrightConstants.NAME + " - " + this.getToolName() + " Name Exclusions", this.gui, this.excludeConfig, this.packageManager, mainClassEntries, this.scriptContext, obfuscateExcludeListener);
	}

	public abstract String getObfuscateConfigFileName();

	public abstract void showObfuscateOptions(String title, GUI gui, ObfuscateConfig config, ZKMConsumer listener);

	public static class ObfuscateExcludeListener extends OptionalConsumer {
		public final ExcludeContext excludeContext;

		public ObfuscateExcludeListener(ExcludeContext excludeContext) {
			this.excludeContext = excludeContext;
		}

		@Override
		public void accept(Object arg) {
			this.acceptObfuscateOptions((Integer)arg);
		}

		public void acceptObfuscateOptions(Integer choice) {
			ExcludeContext.ObfuscateExcludeListener.ObfuscateOptionsListener obfuscateOptionsListener = new ExcludeContext.ObfuscateExcludeListener.ObfuscateOptionsListener(this);
			this.excludeContext.handleExclusionsResult(choice, obfuscateOptionsListener);
		}

		@Override
		public void accept() {
			this.excludeContext.restoreGui();
		}

		public static class ObfuscateOptionsListener extends OptionalConsumer {
			public final ExcludeContext.ObfuscateExcludeListener obfuscateExcludeListener;

			public ObfuscateOptionsListener(ExcludeContext.ObfuscateExcludeListener obfuscateExcludeListener) {
				this.obfuscateExcludeListener = obfuscateExcludeListener;
			}

			public void acceptObfuscateOptions(ObfuscateConfig obfuscateConfig, Integer choice) {
				this.obfuscateExcludeListener.excludeContext.handleOptionsResult(obfuscateConfig, choice);
			}

			@Override
			public void accept(Object arg, Object choice) {
				this.acceptObfuscateOptions((ObfuscateConfig)arg, (Integer)choice);
			}

			@Override
			public void accept() {
				this.obfuscateExcludeListener.excludeContext.restoreGui();
			}
		}
	}
}
