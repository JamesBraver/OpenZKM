package com.zelix.klassmaster.changelogs.parser.entry;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogNameChangeNodeBase;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;

public class ChangeLogFieldNameChangeNode extends ChangeLogNameChangeNodeBase {
	private String newFieldName;
	private String oldFieldName;
	private String fieldType;

	public ChangeLogFieldNameChangeNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	public void setFieldType(String fieldType) {
		this.fieldType = fieldType;
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		if (context.isFieldNameChangeSupported()) {
			int childCount = this.getChildCount();

			for (int childIndex = 0; childIndex < childCount; childIndex++) {
				this.getChild(childIndex).interpret(this, context);
			}

			if (this.newFieldName == null) {
				this.newFieldName = this.oldFieldName;
			}

			((ChangeLogClassChangeNode)parent).addFieldMapping(this.newFieldName, this.oldFieldName, this.fieldType, this.getAccessFlags());
		}
	}

	public void setNewFieldName(String newFieldName) {
		this.newFieldName = newFieldName;
	}

	public void setOldFieldName(String oldFieldName) {
		this.oldFieldName = oldFieldName;
	}
}
