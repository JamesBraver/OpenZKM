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

public class InterfaceMethodCallWriter extends AbstractMethodCallWriter {
	public static final TypeTags TYPE_TAG = TypeTags.INTERFACE_METHODREF;

	public InterfaceMethodCallWriter(
		int index, AbstractConstantPoolWriter constantPool, ClassInfoWriter owner, NameAndTypeWriter target, AbstractWorkspace workspace, ClassProvider classProvider
	) {
		super(index, constantPool, owner, target, workspace, classProvider);
	}

	public InterfaceMethodCallWriter(int index, AbstractConstantPoolWriter constantPool, ClassInfoWriter owner, NameAndTypeWriter target, AbstractMethodNode targetMember) {
		super(index, constantPool, owner, target, targetMember);
	}

	public InterfaceMethodCallWriter(
		AbstractMemberRefInfoWriter memberRefInfoWriter, ClassInfoWriter owner, NameAndTypeWriter target, MultiMap<ClassInfoWriter, ReferenceUpdateListener> classReferences
	) {
		super(memberRefInfoWriter, owner, target, classReferences);
	}

	@Override
	public TypeTags getTypeTag() {
		return TYPE_TAG;
	}
}
