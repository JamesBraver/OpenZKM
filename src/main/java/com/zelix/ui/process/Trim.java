package com.zelix.ui.process;

import lombok.SneakyThrows;

import com.zelix.HelpManager;
import com.zelix.exceptions.ClasspathException;
import com.zelix.exceptions.FileException;
import com.zelix.klassmaster.config.RuleFilterConfig;
import com.zelix.klassmaster.config.TrimConfig;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.ui.GUI;
import com.zelix.ui.dataset.ClassNodeEntry;
import com.zelix.ui.frames.tools.TrimFrame;
import com.zelix.ui.frames.tools.trim.TrimOptionsFrame;
import com.zelix.ui.prompts.CopyablePrompt;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.function.OptionalConsumer;
import com.zelix.utils.function.ZKMConsumer;
import com.zelix.workspace.Workspace;
import java.util.List;

public class Trim {
	private final TrimConfig trimConfig = new TrimConfig(SystemConstants.userDirectory, "ZKM_TO.ser");
	private GUI gui;
	private Workspace workspace;
	private ScriptContext scriptContext;
	private final RuleFilterConfig ruleFilterConfig;

	public Trim(GUI gui, Workspace workspace, ScriptContext scriptContext) {
		this.gui = gui;
		this.workspace = workspace;
		this.scriptContext = scriptContext;
		gui.setEnabled(false);
		gui.setDisable(true);
		this.ruleFilterConfig = RuleFilterConfig.loadFromFile(SystemConstants.userDirectory, "ZKM_TEX.ser");
		this.showTrimExclusionsFrame();
	}

	@SneakyThrows
	public void handleTrimOptionsResult(TrimConfig trimConfig, Integer choice) {
		int stepResultCode = choice;
		trimConfig.saveToFile(SystemConstants.userDirectory, "ZKM_TO.ser");
		if (stepResultCode == 1) {
			this.restoreGui();
			this.gui.trim(this.ruleFilterConfig.getParameters(), trimConfig, this.scriptContext, null);
		} else {
			this.showTrimExclusionsFrame();
		}
	}

	public void restoreGui() {
		this.gui.setEnabled(true);
		this.gui.toFront();
		this.gui.setDisable(false);
	}

	public void handleTrimExclusionsResult(Integer choice, ZKMConsumer resultConsumer) {
		if (choice == 1) {
			this.ruleFilterConfig.saveToFile(SystemConstants.userDirectory, "ZKM_TEX.ser");
			this.showTrimOptionsFrame(CopyrightConstants.NAME + " - Trim Options", this.gui, this.trimConfig, resultConsumer);
		}
	}

	public void showTrimOptionsFrame(String title, GUI gui, TrimConfig trimConfig, ZKMConsumer resultConsumer) {
		new TrimOptionsFrame(title, "OK", HelpManager.getToolTip("PERFORM_TRIM"), gui, trimConfig, resultConsumer);
	}

	public void showTrimExclusionsFrame() {
		Trim.TrimExcludeListener trimExcludeListener = new Trim.TrimExcludeListener(this);
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

		new TrimFrame(CopyrightConstants.NAME + " - Trim Exclusions", this.gui, mainClassEntries, this.ruleFilterConfig, this.workspace, this.trimConfig, this.scriptContext, trimExcludeListener);
	}

	public static class TrimExcludeListener extends OptionalConsumer {
		public final Trim trim;

		public TrimExcludeListener(Trim trim) {
			this.trim = trim;
		}

		public void acceptTrimOptions(Integer choice) {
			Trim.TrimExcludeListener.TrimOptionsListener trimOptionsListener = new Trim.TrimExcludeListener.TrimOptionsListener(this);
			this.trim.handleTrimExclusionsResult(choice, trimOptionsListener);
		}

		@Override
		public void accept(Object arg) {
			this.acceptTrimOptions((Integer)arg);
		}

		@Override
		public void accept() {
			this.trim.restoreGui();
		}

		public static class TrimOptionsListener extends OptionalConsumer {
			public final Trim.TrimExcludeListener trimExcludeListener;

			public TrimOptionsListener(Trim.TrimExcludeListener trimExcludeListener) {
				this.trimExcludeListener = trimExcludeListener;
			}

			public void acceptTrimOptions(TrimConfig trimConfig, Integer choice) {
				this.trimExcludeListener.trim.handleTrimOptionsResult(trimConfig, choice);
			}

			@Override
			public void accept(Object arg, Object choice) {
				this.acceptTrimOptions((TrimConfig)arg, (Integer)choice);
			}

			@Override
			public void accept() {
				this.trimExcludeListener.trim.restoreGui();
			}
		}
	}
}
