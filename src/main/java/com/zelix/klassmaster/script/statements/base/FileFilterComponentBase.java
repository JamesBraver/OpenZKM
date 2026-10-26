package com.zelix.klassmaster.script.statements.base;

import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.base.ScriptNode;
import com.zelix.klassmaster.script.statements.javaccInterface.FileFilterComponentAcceptor;
import com.zelix.klassmaster.script.statements.javaccInterface.PathFilter;
import com.zelix.klassmaster.script.statements.token.StringLiteral;
import com.zelix.utils.FileUtils;
import com.zelix.utils.StringUtils;

public abstract class FileFilterComponentBase extends AbstractScriptNode implements PathFilter {
	private final boolean negated;
	private String directoryPattern;
	private String definition;
	private String fileNamePattern;

	public FileFilterComponentBase(int nodeType, boolean negated) {
		super(nodeType);
		this.negated = negated;
	}

	private static int lastSeparatorIndex(String path) {
		int slashIndex = path.lastIndexOf(47);
		int bangIndex = path.lastIndexOf("!");
		return Math.max(slashIndex, bangIndex);
	}

	private String normalizePath(String path) {
		String normalizedPath = path.replace('\\', '/');
		if (FileUtils.FILE_SEPARATOR_CHAR != '\\') {
			normalizedPath = path.replace(FileUtils.FILE_SEPARATOR_CHAR, '/');
		}

		if (!FileUtils.IS_UNIX) {
			normalizedPath = normalizedPath.toUpperCase();
		}

		return normalizedPath;
	}

	@Override
	public final void parseArguments(ScriptNode parent, ScriptContext scriptContext) {
		this.getChildrenCount();
		StringLiteral stringLiteral = (StringLiteral)this.getChild(0);
		this.definition = stringLiteral.getText();
		String normalizedDefinition = this.normalizePath(this.definition);
		int separatorIndex = lastSeparatorIndex(normalizedDefinition);
		this.directoryPattern = normalizedDefinition.substring(0, separatorIndex + 1);
		this.directoryPattern = "*" + this.directoryPattern;
		this.fileNamePattern = normalizedDefinition.substring(separatorIndex + 1);
		((FileFilterComponentAcceptor)parent).addComponent(this);
	}

	@Override
	public boolean matches(String path) {
		String normalizedPath = this.normalizePath(path);
		int separatorIndex = lastSeparatorIndex(normalizedPath);
		String fileName = normalizedPath.substring(separatorIndex + 1);
		boolean matched = StringUtils.matchesWildcard(normalizedPath.substring(0, separatorIndex + 1), this.directoryPattern) && StringUtils.matchesWildcard(fileName, this.fileNamePattern);
		return this.negated ? !matched : matched;
	}

	@Override
	public String getDefinition() {
		return (this.negated ? "!" : "") + "\"" + this.definition + "\"";
	}
}
