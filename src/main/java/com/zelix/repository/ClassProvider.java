package com.zelix.repository;

import com.zelix.framework.AbstractClassNode;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.PhantomClassManager;

public class ClassProvider {
	public ClassRepository classRepository;

	public ClassProvider(ClassRepository classRepository) {
		this.classRepository = classRepository;
	}

	public AbstractClassNode getClassNode(String internalName, String context, PhantomClassManager phantomClassManager) {
		return this.getClassNode(internalName, null, true, context, phantomClassManager);
	}

	public AbstractClassNode getClassNode(String internalName) {
		return this.getClassNode(internalName, true, null);
	}

	public AbstractClassNode getClassNode(String internalName, boolean throwIfNotFound) {
		return this.getClassNode(internalName, throwIfNotFound, null);
	}

	public AbstractClassNode getClassNode(String internalName, Integer innerClassOrdinal, String context, PhantomClassManager phantomClassManager) {
		return this.getClassNode(internalName, innerClassOrdinal, true, context, phantomClassManager);
	}

	public AbstractClassNode getClassNode(String internalName, boolean throwIfNotFound, String context) {
		return this.getClassNode(internalName, null, throwIfNotFound, context, null);
	}

	public AbstractClassNode getClassNode(String internalName, Integer innerClassOrdinal, boolean throwIfNotFound, String context, PhantomClassManager phantomClassManager) {
		AbstractClassNode classNode = ClassHierarchyNode.getClassNodeByInternalName(internalName);
		if (classNode == null) {
			return this.classRepository.loadClassInternalNoSystem(internalName, innerClassOrdinal, throwIfNotFound, context, phantomClassManager);
		} else {
			return innerClassOrdinal != null && classNode.hasInnerClasses() ? classNode.findInnerClassByOrdinal(innerClassOrdinal) : classNode;
		}
	}

	public void close() {
		if (this.classRepository != null) {
			this.classRepository.close();
		}
	}

	public AbstractClassNode getClassNode(String internalName, Integer innerClassOrdinal) {
		return this.getClassNode(internalName, innerClassOrdinal, true, (String)null);
	}

	public AbstractClassNode getClassNode(String internalName, Integer innerClassOrdinal, boolean throwIfNotFound, String context) {
		return this.getClassNode(internalName, innerClassOrdinal, throwIfNotFound, context, null);
	}

	public AbstractClassNode getClassNode(String internalName, String context) {
		return this.getClassNode(internalName, true, context);
	}

	public AbstractClassNode getClassNode(String internalName, Integer innerClassOrdinal, String context) {
		return this.getClassNode(internalName, innerClassOrdinal, true, context);
	}
}
