package com.zelix.logging;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.ui.GUI;
import com.zelix.ui.base.BaseFrame;

public class GuiLogger extends GuiLoggerBase {
	public final GUI gui;

	public GuiLogger(GUI gui, BaseFrame frame, ScriptContext scriptContext) {
		super(frame, scriptContext);
		this.gui = gui;
	}

	@Override
	public void errorWithDetails(String prefix, String message, String details) {
		super.errorWithDetails(prefix, message, details);
		String outputLine = prefix + this.separator(prefix) + message;
		GUI.getLogWriter(this.gui).println(outputLine);
		GUI.getLogWriter(this.gui).println(details);
		GUI.getLogWriter(this.gui).flush();
		this.gui.close();
	}

	@Override
	public void error(String prefix, String message) {
		super.error(prefix, message);
		String outputLine = prefix + this.separator(prefix) + message;
		GUI.getLogWriter(this.gui).println(outputLine);
		GUI.getLogWriter(this.gui).flush();
		this.gui.close();
	}
}
