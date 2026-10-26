package com.zelix.klassmaster.script.statements.command;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.base.OutputStatement;
import com.zelix.logging.AbstractLogger;
import com.zelix.utils.function.ZKMConsumer;
import com.zelix.workspace.Workspace;
import java.io.File;

public class SaveAllStatement extends OutputStatement {
	public SaveAllStatement(int nodeType) {
		super(nodeType);
	}

	@Override
	public String getCommandName() {
		return "saveAll";
	}

	@Override
	public void saveWorkspace(Workspace workspace, File outputTarget, AbstractLogger logger, ScriptContext scriptContext, ZKMConsumer consumer) {
		workspace.writeWorkspace(this.getArchiveCompressionMode(), this.isDeleteEmptyDirectories(), this.isDeleteXMLComments(), this.getLastModifiedTime(), outputTarget, logger, scriptContext, consumer);
	}
}
