package com.zelix.utils.dataset;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.MemberNode;

public class MemberContext {
	private final MemberNode memberNode;
	private final AbstractClassNode contextClass;
	private final int cachedHashCode;

	public MemberContext(MemberNode memberNode, AbstractClassNode contextClass) {
		this.memberNode = memberNode;
		this.contextClass = contextClass;
		this.cachedHashCode = memberNode.hashCode() ^ contextClass.hashCode();
	}

	public boolean isWorkspaceNode() {
		return this.memberNode.isWorkspaceNode();
	}

	public MemberNode getMemberNode() {
		return this.memberNode;
	}

	public String getOwnerPackageName() {
		return this.memberNode.getClassNode().getPackageName();
	}

	public AbstractClassNode getContextClass() {
		return this.contextClass;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof MemberContext)) {
			return false;
		} else {
			MemberContext memberContext = (MemberContext)o;
			return this.memberNode == memberContext.memberNode && this.contextClass == memberContext.contextClass;
		}
	}

	public String getMemberDescriptor() {
		return this.memberNode.getDescriptor();
	}

	@Override
	public int hashCode() {
		return this.cachedHashCode;
	}

	public String getOwnerClassSourcePath() {
		return this.memberNode.getClassNode().getSourcePath();
	}

	public boolean isField() {
		return this.memberNode.isField();
	}

	public String getMemberNameDisplay() {
		return this.memberNode.getNameDisplay();
	}
}
