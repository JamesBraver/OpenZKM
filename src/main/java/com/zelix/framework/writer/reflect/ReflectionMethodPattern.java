package com.zelix.framework.writer.reflect;

import com.zelix.enums.SymbolType;
import com.zelix.framework.writer.base.AbstractMethodCallWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ReflectionMethodPattern {
	private List<ReflectionParameter> parameters = new ArrayList<>();
	private final int parameterCount;
	private final MethodKind methodType;
	private final SymbolType targetSymbolType;
	private final boolean invokedOnClassInstance;
	private final boolean isMemberAccessOrCreation;
	private final ReflectionRewriteSnippet rewriteSnippet;
	private final RewriteScope rewriteScope;
	private final String methodSignature;

	public ReflectionMethodPattern(
		int parameterCount, MethodKind methodType, SymbolType targetSymbolType, boolean invokedOnClassInstance, boolean isMemberAccessOrCreation, String methodSignature, ReflectionRewriteSnippet rewriteSnippet, RewriteScope rewriteScope
	) {
		this.parameterCount = parameterCount;
		this.methodType = methodType;
		this.targetSymbolType = targetSymbolType;
		this.invokedOnClassInstance = invokedOnClassInstance;
		this.isMemberAccessOrCreation = isMemberAccessOrCreation;
		this.rewriteSnippet = rewriteSnippet;
		this.rewriteScope = rewriteScope;
		this.methodSignature = methodSignature;
	}

	public ReflectionMethodPattern(int parameterCount, MethodKind methodType, SymbolType targetSymbolType, boolean invokedOnClassInstance, String methodSignature) {
		this(parameterCount, methodType, targetSymbolType, invokedOnClassInstance, true, methodSignature);
	}

	public ReflectionMethodPattern(int parameterCount, MethodKind methodType, SymbolType targetSymbolType, boolean invokedOnClassInstance, String methodSignature, ReflectionRewriteSnippet rewriteSnippet, RewriteScope rewriteScope) {
		this(parameterCount, methodType, targetSymbolType, invokedOnClassInstance, true, methodSignature, rewriteSnippet, rewriteScope);
	}

	public ReflectionMethodPattern(int parameterCount, MethodKind methodType, SymbolType targetSymbolType, boolean invokedOnClassInstance, boolean isMemberAccessOrCreation, String methodSignature) {
		this(parameterCount, methodType, targetSymbolType, invokedOnClassInstance, isMemberAccessOrCreation, methodSignature, null, RewriteScope.NONE);
	}

	public static int getTargetClassParameterIndex(ReflectionMethodPattern reflectionPattern) {
		if (reflectionPattern.isInvokedOnClassInstance()) {
			return reflectionPattern.getParameterSpecCount();
		} else {
			for (int i = 0; i < reflectionPattern.getParameterSpecCount(); i++) {
				if (reflectionPattern.getParameter(i).isTargetClass()) {
					return i;
				}
			}

			return -1;
		}
	}

	public static int getMemberNameParameterIndex(ReflectionMethodPattern reflectionPattern) {
		for (int i = 0; i < reflectionPattern.getParameterSpecCount(); i++) {
			ReflectionParameter parameter = reflectionPattern.getParameter(i);
			if (parameter.isFieldNameParameter() || parameter.isMethodNameParameter()) {
				return i;
			}
		}

		return -1;
	}

	public static boolean isAtomicUpdaterPattern(ReflectionMethodPattern reflectionPattern) {
		return reflectionPattern == AbstractMethodCallWriter.ATOMIC_INT_UPDATER_PATTERN || reflectionPattern == AbstractMethodCallWriter.ATOMIC_LONG_UPDATER_PATTERN;
	}

	public static int getFieldTypeClassParamIndex(ReflectionMethodPattern reflectionPattern) {
		for (int i = 0; i < reflectionPattern.getParameterSpecCount(); i++) {
			if (reflectionPattern.getParameter(i).isFieldTypeClass()) {
				return i;
			}
		}

		return -1;
	}

	public static String getAtomicUpdaterFieldType(ReflectionMethodPattern reflectionPattern) {
		if (reflectionPattern == AbstractMethodCallWriter.ATOMIC_INT_UPDATER_PATTERN) {
			return "I";
		} else {
			return reflectionPattern == AbstractMethodCallWriter.ATOMIC_LONG_UPDATER_PATTERN ? "J" : null;
		}
	}

	public List<ReflectionParameter> getParameters() {
		return Collections.unmodifiableList(this.parameters);
	}

	public void addParameter(int parameterIndex, ReflectionLookup lookupType, boolean isArray, boolean isTargetClass, boolean isFieldTypeClass) {
		if (this.parameters.size() > 0) {
			ReflectionParameter lastParameter = this.parameters.get(this.parameters.size() - 1);
			if (lastParameter.getParameterIndex() >= parameterIndex) {
				throw new IllegalArgumentException("Parameter details must be added in ascending position order. " + lastParameter.getParameterIndex() + " > " + parameterIndex);
			}
		}

		this.parameters.add(new ReflectionParameter(parameterIndex, lookupType, isArray, isTargetClass, isFieldTypeClass));
	}

	public int getMethodTypeStackOffset() {
		return this.methodType.getValue();
	}

	public boolean isMethodType() {
		return this.targetSymbolType == SymbolType.METHOD_TYPE;
	}

	public boolean isMemberAccessOrCreation() {
		return this.isMemberAccessOrCreation;
	}

	public boolean isAllConstructorsCallType() {
		return this.targetSymbolType == SymbolType.ALL_CONSTRUCTORS_CALL_TYPE;
	}

	public boolean isInvokedOnClassInstance() {
		return this.invokedOnClassInstance;
	}

	public boolean isFunctionalInterfaceMethodType() {
		return this.targetSymbolType == SymbolType.FUNCTIONAL_INTERFACE_METHOD_TYPE;
	}

	public int getParameterSpecCount() {
		return this.parameters.size();
	}

	public RewriteScope getRewriteScope() {
		return this.rewriteScope;
	}

	public boolean hasMethodNameParameter() {
		Iterator<ReflectionParameter> parameterIterator = this.parameters.iterator();

		while (parameterIterator.hasNext()) {
			if (parameterIterator.next().isMethodNameParameter()) {
				return true;
			}
		}

		return false;
	}

	public boolean isSpecificConstructorCallType() {
		return this.targetSymbolType == SymbolType.SPECIFIC_CONSTRUCTOR_CALL_TYPE;
	}

	public boolean isFieldType() {
		return this.targetSymbolType == SymbolType.FIELD_TYPE;
	}

	public void addParameter(int parameterIndex, ReflectionLookup lookupType, boolean isArray, boolean isTargetClass) {
		this.addParameter(parameterIndex, lookupType, isArray, isTargetClass, false);
	}

	public void addParameter(int parameterIndex, ReflectionLookup lookupType, boolean isArray) {
		this.addParameter(parameterIndex, lookupType, isArray, false, false);
	}

	public boolean matchesScope(boolean hasRenamedNames, boolean hasRenamedFields, boolean hasRenamedMethods) {
		switch (this.rewriteScope) {
			case ALL_CLASSES:
			case ALL_LIST_RESOUCE_BUNDLE:
				return hasRenamedNames;
			case ALL_PUBLIC_FIELDS:
			case ALL_FIELDS_OF_CLASS:
			case ALL_INSTANCE_FIELDS_OF_CLASS:
			case ALL_STATIC_FIELDS_ACCESSIBLE_FROM_CLASS:
			case ALL_INSTANCE_FIELDS_ACCESSIBLE_FROM_CLASS:
				return hasRenamedFields;
			case ALL_PUBLIC_METHODS:
			case ALL_STATIC_METHODS_ACCESSIBLE_FROM_CLASS:
			case ALL_VIRTUAL_METHODS_ACCESSIBLE_FROM_CLASS:
			case ALL_METHODS_OF_CLASS:
				return hasRenamedMethods;
			case NONE:
				return false;
			default:
				return false;
		}
	}

	public String getMethodSignature() {
		return this.methodSignature;
	}

	public boolean isMethodTypeType() {
		return this.targetSymbolType == SymbolType.METHOD_TYPE_TYPE;
	}

	public boolean hasFieldNameParameter() {
		Iterator<ReflectionParameter> parameterIterator = this.parameters.iterator();

		while (parameterIterator.hasNext()) {
			if (parameterIterator.next().isFieldNameParameter()) {
				return true;
			}
		}

		return false;
	}

	public boolean isMethodHandleType() {
		return this.targetSymbolType == SymbolType.METHOD_HANDLE_TYPE;
	}

	public ReflectionRewriteSnippet getRewriteSnippet() {
		return this.rewriteSnippet;
	}

	public boolean hasRewriteSnippet() {
		return this.rewriteSnippet != null;
	}

	public ReflectionParameter getParameter(int parameterIndex) {
		return this.parameters.get(parameterIndex);
	}

	public boolean isConstructorType() {
		switch (this.targetSymbolType) {
			case DEFAULT_CONSTRUCTOR_CALL_TYPE:
			case SPECIFIC_CONSTRUCTOR_CALL_TYPE:
			case ALL_CONSTRUCTORS_CALL_TYPE:
				return true;
			case METHOD_HANDLE_TYPE:
				if (this.getMethodName().equals("findConstructor")) {
					return true;
				}

				return false;
			default:
				return false;
		}
	}

	public boolean isDefaultConstructorCallType() {
		return this.targetSymbolType == SymbolType.DEFAULT_CONSTRUCTOR_CALL_TYPE;
	}

	public String getMethodName() {
		return this.methodSignature.substring(0, this.methodSignature.indexOf(40));
	}

	public boolean isObjectNameType() {
		return this.targetSymbolType == SymbolType.OBJECT_NAME_TYPE;
	}
}
