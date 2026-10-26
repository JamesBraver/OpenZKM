package com.zelix.klassmaster.changelogs.parser.name;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNodeBase;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.NameReceiver;
import java.util.Vector;

public class ChangeLogNameListNode extends ChangeLogsNodeBase implements NameReceiver {
	private Vector<String> nameParts = new Vector<>();

	public ChangeLogNameListNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	public String getQualifiedName() {
		StringBuffer nameBuffer = new StringBuffer();
		int namePartCount = this.nameParts.size();

		for (int namePartIndex = 0; namePartIndex < namePartCount; namePartIndex++) {
			nameBuffer.append(this.nameParts.elementAt(namePartIndex));
			if (namePartIndex < namePartCount - 1) {
				nameBuffer.append(".");
			}
		}

		return nameBuffer.toString();
	}

	@Override
	public void setName(String name) {
		this.nameParts.addElement(name);
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		int childCount = this.getChildCount();

		for (int i = 0; i < childCount; i++) {
			this.getChild(i).interpret(this, context);
		}

		((NameReceiver)parent).setName(this.getQualifiedName());
	}
}
