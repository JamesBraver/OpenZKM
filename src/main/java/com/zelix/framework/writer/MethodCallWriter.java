package com.zelix.framework.writer;

import com.zelix.enums.TypeTags;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.listener.ReferenceUpdateListener;
import com.zelix.framework.writer.base.AbstractConstantPoolWriter;
import com.zelix.framework.writer.base.AbstractMemberRefInfoWriter;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.collections.MultiMap;
import com.zelix.workspace.AbstractWorkspace;

public class MethodCallWriter extends AbstractMethodCallWriter {
	public static final TypeTags TAG = TypeTags.METHODREF;

	public MethodCallWriter(
		AbstractMemberRefInfoWriter memberRefInfoWriter, ClassInfoWriter owner, NameAndTypeWriter target, MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences
	) {
		super(memberRefInfoWriter, owner, target, classReferences);
	}

	public MethodCallWriter(int index, AbstractConstantPoolWriter constantPool, ClassInfoWriter owner, NameAndTypeWriter target, AbstractWorkspace workspace, ClassProvider classProvider) {
		super(index, constantPool, owner, target, workspace, classProvider);
	}

	public MethodCallWriter(int index, AbstractConstantPoolWriter constantPool, ClassInfoWriter owner, NameAndTypeWriter target, AbstractMethodNode targetMember) {
		super(index, constantPool, owner, target, targetMember);
	}

	@Override
	public TypeTags getTypeTag() {
		return TAG;
	}
}
