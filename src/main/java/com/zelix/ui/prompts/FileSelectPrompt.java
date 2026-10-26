package com.zelix.ui.prompts;

import com.zelix.ui.base.BasePrompt;
import com.zelix.ui.components.panels.FileSelectPanel;
import javax.swing.JFrame;

public class FileSelectPrompt extends BasePrompt {
	public final FileSelectPanel fileSelectPanel;

	public FileSelectPrompt(FileSelectPanel fileSelectPanel, JFrame parent, String title, boolean modal) {
		super(parent, title, modal);
		this.fileSelectPanel = fileSelectPanel;
	}
}
