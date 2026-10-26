package com.zelix.klassmaster.changelogs.parser.entry;

import com.zelix.klassmaster.changelogs.AbstractChangeLogContext;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogNameChangeNodeBase;
import com.zelix.klassmaster.changelogs.parser.base.ChangeLogsNode;

public class ChangeLogMethodNameChangeNode extends ChangeLogNameChangeNodeBase {
	private String parameterChangeLookupData;
	private String newMethodSignature;
	private String newMethodName;
	private String parameterChangeData;
	private String oldMethodName;
	private String oldMethodSignature;
	private String methodReturnType;
	private boolean parameterObfuscated;

	public ChangeLogMethodNameChangeNode(int nodeKindIndex) {
		super(nodeKindIndex);
	}

	public void setParameterChangeLookupData(String parameterChangeLookupData) {
		this.parameterChangeLookupData = parameterChangeLookupData;
	}

	public void setOldMethodSignature(String oldMethodName, String oldMethodSignature) {
		this.oldMethodName = oldMethodName;
		this.oldMethodSignature = oldMethodSignature;
	}

	@Override
	public void interpret(ChangeLogsNode parent, AbstractChangeLogContext context) {
		int childCount = this.getChildCount();

		for (int childIndex = 0; childIndex < childCount; childIndex++) {
			this.getChild(childIndex).interpret(this, context);
		}

		if (this.newMethodName == null) {
			this.newMethodName = this.oldMethodName;
		}

		((ChangeLogClassChangeNode)parent)
			.addMethodMapping(
				this.oldMethodName,
				this.oldMethodSignature,
				this.methodReturnType,
				this.getAccessFlags(),
				this.newMethodName,
				this.newMethodSignature,
				this.parameterObfuscated,
				this.parameterChangeData,
				this.parameterChangeLookupData,
				this.getManufactured()
			);
	}

	public void setMethodReturnType(String methodReturnType) {
		this.methodReturnType = methodReturnType;
	}

	public void setParameterChangeData(String parameterChangeData) {
		this.parameterChangeData = parameterChangeData;
	}

	public void setNewMethodName(String newMethodName) {
		this.newMethodName = newMethodName;
	}

	public void setNewMethodSignature(String newMethodName, String newMethodSignature, boolean parameterObfuscated) {
		if (this.newMethodName == null) {
			this.newMethodName = newMethodName;
			this.newMethodSignature = newMethodSignature;
			this.parameterObfuscated = parameterObfuscated;
		} else {
			this.parameterObfuscated = parameterObfuscated;
		}
	}
}
