package com.zelix.logging;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.ui.base.BaseFrame;
import com.zelix.ui.prompts.CopyablePrompt;
import com.zelix.ui.prompts.ErrorPrompt;
import java.io.PrintWriter;

public class GuiLoggerBase extends AbstractLogger {
	public final BaseFrame frame;
	private final ScriptContext scriptContext;

	public GuiLoggerBase(BaseFrame frame, ScriptContext scriptContext) {
		this.frame = frame;
		this.scriptContext = scriptContext;
	}

	@Override
	public void info(String prefix, String message) {
		this.warn(prefix, message);
	}

	@Override
	public void errorWithDetails(String prefix, String message, String details) {
		if (this.scriptContext != null) {
			message = message + this.scriptContext.getIndentationSuffix();
			PrintWriter scriptWriter = this.scriptContext.getWriter();
			scriptWriter.println(prefix + " : " + message);
			scriptWriter.println(details);
		}

		ErrorPrompt.show(this.frame, prefix, message, details, true);
	}

	@Override
	public void warn(String prefix, String message) {
		if (this.scriptContext != null) {
			message = message + this.scriptContext.getIndentationSuffix();
			this.scriptContext.getWriter().println(prefix + " : " + message);
		}

		if (message.length() <= 90) {
			CopyablePrompt.show(this.frame, prefix, message, true);
		} else {
			ErrorPrompt.show(this.frame, prefix, prefix, message, true);
		}
	}

	@Override
	public void fatal(String prefix, String message) {
		if (this.scriptContext != null) {
			message = message + this.scriptContext.getIndentationSuffix();
			this.scriptContext.getWriter().println(prefix + " : " + message);
		}

		if (message.length() <= 90) {
			CopyablePrompt.show(this.frame, prefix, message, true);
		} else {
			ErrorPrompt.show(this.frame, prefix, prefix, message, true);
		}

		System.err.println();
		System.err.println(prefix + " : " + message);
		this.frame.close();
	}

	@Override
	public void fatalErrorWithDetails(String prefix, String message, String details) {
		if (this.scriptContext != null) {
			message = message + this.scriptContext.getIndentationSuffix();
			PrintWriter scriptWriter = this.scriptContext.getWriter();
			scriptWriter.println(prefix + " : " + message);
			scriptWriter.println(details);
		}

		ErrorPrompt.show(this.frame, prefix, message, details, true);
		System.err.println();
		System.err.println(prefix + " : " + message);
		this.frame.close();
	}

	@Override
	public void error(String prefix, String message) {
		if (this.scriptContext != null) {
			message = message + this.scriptContext.getIndentationSuffix();
			this.scriptContext.getWriter().println(prefix + " : " + message);
		}

		if (message.length() <= 90) {
			CopyablePrompt.show(this.frame, prefix, message, true);
		} else {
			ErrorPrompt.show(this.frame, prefix, prefix, message, true);
		}
	}

	@Override
	public void message(String prefix, String message) {
		if (this.scriptContext != null) {
			message = message + this.scriptContext.getIndentationSuffix();
			this.scriptContext.getWriter().println(prefix + " : " + message);
		}

		if (message.length() <= 90) {
			CopyablePrompt.show(this.frame, prefix, message, true);
		} else {
			ErrorPrompt.show(this.frame, prefix, prefix, message, true);
		}
	}
}
