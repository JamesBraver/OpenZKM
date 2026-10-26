package com.zelix.klassmaster.changelogs.parser.signature;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNodeBase;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.TypeSignatureReceiver;
import java.util.Vector;

public class ChangeLogQualifiedTypeNode extends ChangeLogsNodeBase {
	public Vector<String> nameParts = new Vector<>();
	public int arrayLevel;

	public ChangeLogQualifiedTypeNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	public String getTypeName() {
		StringBuilder typeNameBuilder = new StringBuilder();
		int namePartCount = this.nameParts.size();
		if (namePartCount != 0) {
			for (int namePartIndex = 0; namePartIndex < namePartCount; namePartIndex++) {
				String namePart = this.nameParts.elementAt(namePartIndex);
				typeNameBuilder.append(namePart);
				if (namePartIndex < this.nameParts.size() - 1) {
					typeNameBuilder.append(".");
				}
			}
		}

		for (int levelIndex = 0; levelIndex < this.arrayLevel; levelIndex++) {
			typeNameBuilder.append("[]");
		}

		return typeNameBuilder.toString();
	}

	public void incrementArrayLevel() {
		this.arrayLevel++;
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		int childCount = this.getChildCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			this.getChild(childIndex).interpret(this, context);
		}

		((TypeSignatureReceiver)parent).setTypeSignature(this.getTypeName());
	}

	public void addNamePart(String namePart) {
		this.nameParts.addElement(namePart);
	}
}
