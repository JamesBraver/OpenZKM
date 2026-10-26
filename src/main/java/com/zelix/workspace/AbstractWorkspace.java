package com.zelix.workspace;

import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.framework.signature.FieldSignature;
import com.zelix.framework.signature.MethodParameterKey;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import java.util.Enumeration;
import java.util.List;

public interface AbstractWorkspace extends ClassHierarchy {
	List<String> getOverrideFieldNames();

	List<String> getOverrideMethodNames();

	Enumeration<ClassNode> getClassesByMethodSignature(MethodSignature methodSignature);

	boolean isResourceRegistered(String internalName, String fieldName);

	MethodNode getMethodBySignature(ClassNode classNode, MethodSignature methodSignature);

	Enumeration<ClassNode> getClasses();

	Enumeration<ClassNode> getClassesByMethodName(String methodName);

	Enumeration<ClassNode> getClassesByFieldName(String fieldName);

	FieldNode[] getFieldsByClassAndName(String internalName, String fieldName);

	boolean containsFieldSignature(FieldSignature fieldSignature, ClassNode classNode);

	void registerField(ClassNode classNode, FieldNode fieldNode);

	boolean containsMethodSignature(MethodSignature methodSignature, ClassNode classNode);

	MethodNode[] getMethodNodesByParameterKey(ClassNode classNode, MethodParameterKey parameterKey);

	FieldNode getFieldByNameAndDescriptor(String internalName, String fieldName, String descriptor);

	void registerMethod(MethodNode methodNode, ClassNode classNode);

	FieldNode getFieldBySignatureAndClass(ClassNode classNode, FieldSignature fieldSignature);

	FieldNode[] getFieldsBySignature(FieldSignature fieldSignature);
}
