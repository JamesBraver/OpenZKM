package com.zelix.ui.process;

import lombok.SneakyThrows;

import com.zelix.HelpManager;
import com.zelix.klassmaster.config.ObfuscateConfig;
import com.zelix.klassmaster.config.ReferenceObfuscateConfig;
import com.zelix.klassmaster.constant.CopyrightConstants;
import com.zelix.klassmaster.constant.SystemConstants;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.ui.GUI;
import com.zelix.ui.frames.tools.obfuscate.ObfuscateOptionsFrame;
import com.zelix.ui.frames.tools.obfuscate.references.ObfuscateReferencesFrame;
import com.zelix.utils.function.OptionalConsumer;
import com.zelix.utils.function.ZKMConsumer;
import com.zelix.workspace.PackageManager;
import java.io.IOException;

public class Obfuscate extends ExcludeContext {
	public ReferenceObfuscateConfig referenceObfuscateConfig;

	public Obfuscate(GUI gui, PackageManager packageManager, ScriptContext scriptContext) {
		super(gui, packageManager, scriptContext);
	}

	private void handleReferencesResult(Integer choice) throws IOException {
		int stepResultCode = choice;
		this.referenceObfuscateConfig.saveToFile(SystemConstants.userDirectory, "ZKM_OB_REF.ser");
		if (stepResultCode == 1) {
			this.restoreGui();
			super.gui.obfuscate(super.excludeConfig.getParameters(), this.referenceObfuscateConfig == null ? null : this.referenceObfuscateConfig.getParameters(), super.obfuscateConfig, super.scriptContext, null);
		} else if (stepResultCode == 2 || stepResultCode == 5) {
			Obfuscate.ObfuscateOptionsListener obfuscateOptionsListener = new Obfuscate.ObfuscateOptionsListener(this);
			this.showObfuscateOptions(CopyrightConstants.NAME + " - " + this.getToolName() + " Options", super.gui, super.obfuscateConfig, obfuscateOptionsListener);
		}
	}

	public void showReferencesOptions(String title, GUI gui, ZKMConsumer resultConsumer) {
		this.referenceObfuscateConfig = ReferenceObfuscateConfig.loadFromFile(SystemConstants.userDirectory, "ZKM_OB_REF.ser");
		new ObfuscateReferencesFrame(title, gui, this.referenceObfuscateConfig, super.scriptContext, resultConsumer, 3);
	}

	@Override
	public String getToolName() {
		return "Obfuscate";
	}

	@Override
	@SneakyThrows
	public void handleOptionsResult(ObfuscateConfig config, Integer choice) {
		if (choice == 1) {
			super.obfuscateConfig.saveToFile(SystemConstants.userDirectory, "ZKM_SO.ser");
			if (super.obfuscateConfig.obfuscateReferences == 1) {
				Obfuscate.ObfuscateReferencesListener obfuscateReferencesListener = new Obfuscate.ObfuscateReferencesListener(this);
				this.showReferencesOptions(CopyrightConstants.NAME + " - Reference Obfuscation Inclusions", super.gui, obfuscateReferencesListener);
			} else {
				this.referenceObfuscateConfig = null;
				this.restoreGui();
				super.gui.obfuscate(super.excludeConfig.getParameters(), null, super.obfuscateConfig, super.scriptContext, null);
			}
		} else {
			this.showNameExclusionsFrame();
		}
	}

	@Override
	public String getObfuscateConfigFileName() {
		return "ZKM_SO.ser";
	}

	@Override
	public void showObfuscateOptions(String title, GUI gui, ObfuscateConfig config, ZKMConsumer listener) {
		new ObfuscateOptionsFrame(title, HelpManager.getToolTip("PERFORM_OBFUSCATE"), gui, config, super.excludeConfig, super.scriptContext, listener);
	}

	public static void handleReferencesResult(Obfuscate obfuscate, Integer choice) throws IOException {
		obfuscate.handleReferencesResult(choice);
	}

	public static class ObfuscateReferencesListener extends OptionalConsumer {
		public final Obfuscate obfuscate;

		public ObfuscateReferencesListener(Obfuscate obfuscate) {
			this.obfuscate = obfuscate;
		}

		@SneakyThrows
		public void acceptObfuscateReferences(Integer choice) {
			Obfuscate.handleReferencesResult(this.obfuscate, choice);
		}

		@Override
		public void accept(Object arg) {
			this.acceptObfuscateReferences((Integer)arg);
		}

		@Override
		public void accept() {
			this.obfuscate.restoreGui();
		}
	}

	public static class ObfuscateOptionsListener extends OptionalConsumer {
		public final Obfuscate obfuscate;

		public ObfuscateOptionsListener(Obfuscate obfuscate) {
			this.obfuscate = obfuscate;
		}

		public void acceptObfuscateOptions(ObfuscateConfig obfuscateConfig, Integer choice) {
			this.obfuscate.handleOptionsResult(obfuscateConfig, choice);
		}

		@Override
		public void accept(Object arg, Object choice) {
			this.acceptObfuscateOptions((ObfuscateConfig)arg, (Integer)choice);
		}

		@Override
		public void accept() {
			this.obfuscate.restoreGui();
		}
	}
}
