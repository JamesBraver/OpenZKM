package com.zelix.ui.frames.tools.trim;

import lombok.SneakyThrows;

import com.zelix.exceptions.ProcessingException;
import com.zelix.exceptions.TrimClassNotFoundException;
import com.zelix.klassmaster.config.RuleFilterConfig;
import com.zelix.klassmaster.config.TrimConfig;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import com.zelix.klassmaster.script.statements.exclusion.TrimExcludeStatement;
import com.zelix.logging.AbstractLogger;
import com.zelix.logging.GuiLoggerBase;
import com.zelix.ui.GUI;
import com.zelix.ui.dataset.ClassNodeEntry;
import com.zelix.ui.prompts.CopyablePrompt;
import com.zelix.ui.prompts.ScrollableMessagePrompt;
import com.zelix.utils.function.OptionalConsumer;
import com.zelix.utils.function.ZKMConsumer;
import com.zelix.workspace.Workspace;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Vector;
import javax.swing.JButton;

public abstract class WrappedTrimExcludeFrame extends TrimExcludeFrame {
	public JButton testButton;

	public WrappedTrimExcludeFrame(
		String title, GUI gui, List<ClassNodeEntry> classNodes, RuleFilterConfig ruleFilterConfig, Workspace workspace, TrimConfig trimConfig, ScriptContext scriptContext, ZKMConsumer resultConsumer, int parameterCategory
	) {
		super(title, gui, classNodes, ruleFilterConfig, workspace, trimConfig, scriptContext, resultConsumer, parameterCategory);
	}

	public final void onTestTrim() throws IOException {
		this.applyToConfig();
		List<?> ruleStrings = super.filterConfig.getParameters();
		if (super.workspace.isEmpty()) {
			new CopyablePrompt(this, "No classes opened", "The are no classes opened. Use the \"File | Open\" menu on the Browser window.");
		} else if (!super.workspace.hasClasses()) {
			new CopyablePrompt(
				this, "Only module-info.class files opened", "The are no classes other than module-info opened. Use the \"File | Open\" menu on the Browser window."
			);
		} else {
			Vector<ExclusionInclusionStatement> excludeStatements = new Vector<>(1);
			String statementText = TrimExcludeStatement.buildStatementText(ruleStrings, 12);
			if (statementText != null) {
				ExclusionInclusionStatement statement;
				try {
					statement = TrimExcludeStatement.parseTrimExcludeStatement(statementText, super.scriptContext);
				} catch (ProcessingException processingException) {
					super.scriptContext.logWarning(processingException.getMessage());
					new CopyablePrompt(this, "Error", "Unexpected error in exclude parameters. (3) See the log for more detail.");
					return;
				}

				excludeStatements.addElement(statement);
			}

			GuiLoggerBase logger = new GuiLoggerBase(this, super.scriptContext);
			WrappedTrimExcludeFrame.TrimPreviewListener previewListener = new WrappedTrimExcludeFrame.TrimPreviewListener(this);
			WrappedTrimExcludeFrame.TrimPreviewTask previewTask = new WrappedTrimExcludeFrame.TrimPreviewTask(this, excludeStatements, logger, previewListener);
			this.setWaitCursor();
			new Thread(previewTask).start();
		}
	}

	public static class TrimPreviewListener extends OptionalConsumer {
		public final WrappedTrimExcludeFrame wrappedTrimExcludeFrame;

		public TrimPreviewListener(WrappedTrimExcludeFrame wrappedTrimExcludeFrame) {
			this.wrappedTrimExcludeFrame = wrappedTrimExcludeFrame;
		}

		@Override
		public void accept() {
			this.wrappedTrimExcludeFrame.resetCursor();
		}
	}

	public static class TrimPreviewTask implements Runnable {
		public final WrappedTrimExcludeFrame wrappedTrimExcludeFrame;
		public final Vector<ExclusionInclusionStatement> excludeStatements;
		public final AbstractLogger logger;
		public final ZKMConsumer completionCallback;

		public TrimPreviewTask(WrappedTrimExcludeFrame wrappedTrimExcludeFrame, Vector<ExclusionInclusionStatement> excludeStatements, AbstractLogger logger, ZKMConsumer completionCallback) {
			this.wrappedTrimExcludeFrame = wrappedTrimExcludeFrame;
			this.excludeStatements = excludeStatements;
			this.logger = logger;
			this.completionCallback = completionCallback;
		}

		@Override
		@SneakyThrows
		public void run() {
			ByteArrayOutputStream reportBuffer = new ByteArrayOutputStream();
			PrintWriter printWriter = new PrintWriter(reportBuffer);

			try {
				this.wrappedTrimExcludeFrame.workspace.previewTrimAnalysis(this.excludeStatements, null, printWriter, this.logger, this.completionCallback, this.wrappedTrimExcludeFrame.trimConfig, this.wrappedTrimExcludeFrame.scriptContext);
				printWriter.close();
				String reportText = reportBuffer.toString();
				if (reportText.length() > -1) {
					new ScrollableMessagePrompt(this.wrappedTrimExcludeFrame, "Test trim report", "Test Trim Report...", reportText, false, true, false);
				}
			} catch (TrimClassNotFoundException trimClassNotFoundException) {
				this.logger.error("FILE ERROR:", trimClassNotFoundException.getMessage());
			} catch (ProcessingException processingException) {
				this.logger.error("ERROR:", processingException.getMessage());
			}
		}
	}
}
