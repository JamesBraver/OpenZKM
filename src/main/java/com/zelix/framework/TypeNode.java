package com.zelix.framework;

import com.zelix.framework.writer.base.MemberTypeWriter;

public interface TypeNode {
	void updateMemberReference(MemberTypeWriter oldReference, MemberTypeWriter newReference);
}
