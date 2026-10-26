package com.zelix.klassmaster.changelogs.parser.base;

import com.zelix.klassmaster.changelogs.parser.javaccInterface.NameReceiver;
import com.zelix.klassmaster.changelogs.parser.javaccInterface.TypeSignatureReceiver;

public abstract class ChangeLogSignatureNodeBase extends ChangeLogsNodeBase implements NameReceiver, TypeSignatureReceiver {
	public String methodName;
	public String typeSignature;

	public ChangeLogSignatureNodeBase(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	@Override
	public final void setName(String name) {
		this.methodName = name;
	}

	@Override
	public final void setTypeSignature(String typeSignature) {
		this.typeSignature = typeSignature;
	}
}
