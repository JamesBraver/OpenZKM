package com.zelix.klassmaster.changelogs.parser.entry;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNodeBase;
import java.util.ArrayList;

public class ChangeLogLineNumberChangeNode extends ChangeLogsNodeBase {
	public Integer oldLineNumber;
	public AbstractChangeLogContext context;
	public ArrayList<Integer> newLineNumbers;

	public ChangeLogLineNumberChangeNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	public void addNewLineNumber(String lineNumber) {
		this.newLineNumbers.add(this.context.getCachedInteger(Integer.parseInt(lineNumber)));
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		this.context = context;
		int childCount = this.getChildCount();
		this.newLineNumbers = new ArrayList<>(childCount - 1);

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			this.getChild(childIndex).interpret(this, context);
		}

		if (context.isLineNumberChangeSupported()) {
			ChangeLogClassChangeNode classChangeNode = (ChangeLogClassChangeNode)parent;

			for (int lineNumberIndex = 0; lineNumberIndex < this.newLineNumbers.size(); lineNumberIndex++) {
				classChangeNode.addLineNumberMapping(this.newLineNumbers.get(lineNumberIndex), this.oldLineNumber);
			}
		}
	}

	public void setOldLineNumber(String lineNumber) {
		this.oldLineNumber = this.context.getCachedInteger(Integer.parseInt(lineNumber));
	}
}
