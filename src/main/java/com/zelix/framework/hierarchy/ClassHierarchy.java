package com.zelix.framework.hierarchy;

import com.zelix.framework.signature.MethodSignature;
import com.zelix.klassmaster.script.statements.annotation.ComplexAnnotationSpecifier;
import com.zelix.repository.ClassProvider;
import java.util.Set;

public interface ClassHierarchy {
	boolean isInstanceOf(String className, String target, ComplexAnnotationSpecifier annotationSpecifier);

	boolean hasHierarchySnapshot();

	boolean isObjectMethodSignature(MethodSignature methodSignature);

	boolean isSubclassOf(String className, String target);

	boolean isAssignableFrom(String className, String target);

	ClassProvider getClassProvider();

	Set<String> getAnnotationTypes(String className, Integer innerClassOrdinal, boolean matchOnOriginalNames);

	boolean isMethodNameObfFinish();

	boolean isSuperclassOf(String className, String target);

	boolean isInterfaceOf(String className, String target, ComplexAnnotationSpecifier annotationSpecifier);

	boolean isClassInstanceOf(String className, String target);

	boolean isFieldNameObfFinish();

	boolean isInterfaceImplementedBy(String className, String target, ComplexAnnotationSpecifier annotationSpecifier);

	String getBinaryName(String internalName);

	boolean isOriginalSuperclassOf(String className, String target, ComplexAnnotationSpecifier annotationSpecifier);
}
