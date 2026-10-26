package com.zelix.klassmaster.changelogs;

import com.zelix.framework.AccessNode;
import java.io.Serializable;

public abstract class AbstractMemberKey implements Serializable {
	public final String name;
	public final String descriptor;
	public final int accessFlags;

	public AbstractMemberKey(String name, String descriptor, int accessFlags) {
		this.name = name;
		this.descriptor = descriptor;
		this.accessFlags = accessFlags;
	}

	public boolean isPrivate() {
		return AccessNode.isPrivate(this.accessFlags);
	}

	public String getDescriptor() {
		return this.descriptor;
	}

	public String getName() {
		return this.name;
	}
}
