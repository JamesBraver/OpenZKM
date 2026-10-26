package com.zelix.klassmaster.changelogs.parser.base;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.config.Options;

public interface ChangeLogsNode {
	String LINE_SEPARATOR = Options.lineSeparator;

	void setParent(ChangeLogsNode parent);

	void close();

	void dispose();

	void interpret(ChangeLogsNode parent, AbstractChangeLogContext context);

	ChangeLogsNode getChild(int index);

	int getChildCount();

	void open();

	void addChild(ChangeLogsNode child, int index);
}
