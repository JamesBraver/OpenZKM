package com.zelix.framework.listener;

import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.utils.GlobalUtils;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;

public class ReferenceSetContainer {
	private final HashSet<ClassNode> classReferences = GlobalUtils.newHashSet(13);
	private final HashSet<ClassNode> signatureClassReferences = GlobalUtils.newHashSet(13);
	private final HashSet<FieldNode> fieldReferences = GlobalUtils.newHashSet(13);
	private final HashSet<MethodNode> methodReferences = GlobalUtils.newHashSet(13);

	public Enumeration<ClassNode> getSignatureClassReferences() {
		return Collections.enumeration(this.signatureClassReferences);
	}

	public Enumeration<MethodNode> getMethodReferences() {
		return Collections.enumeration(this.methodReferences);
	}

	public HashSet<ClassNode> getClassReferenceSet() {
		return this.classReferences;
	}

	public Enumeration<FieldNode> getFieldReferences() {
		return Collections.enumeration(this.fieldReferences);
	}

	public Enumeration<ClassNode> getClassReferences() {
		return Collections.enumeration(this.classReferences);
	}

	public HashSet<FieldNode> getFieldReferenceSet() {
		return this.fieldReferences;
	}

	public HashSet<ClassNode> getSignatureClassReferenceSet() {
		return this.signatureClassReferences;
	}

	public HashSet<MethodNode> getMethodReferenceSet() {
		return this.methodReferences;
	}
}
