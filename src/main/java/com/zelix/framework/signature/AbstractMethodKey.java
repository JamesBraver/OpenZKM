package com.zelix.framework.signature;

import com.zelix.framework.writer.base.TypeWriter;
import java.util.List;
import java.util.Map;

public abstract class AbstractMethodKey {
	public final String name;
	public final String parameterDescriptor;

	public AbstractMethodKey(AbstractMethodKey methodKey) {
		this.name = methodKey.getName();
		this.parameterDescriptor = methodKey.getParameterDescriptor();
	}

	public AbstractMethodKey(String name, String descriptor) {
		this.name = name.intern();
		this.parameterDescriptor = MethodSignature.extractParameterDescriptor(descriptor).intern();
	}

	public final String toHumanReadableString() {
		List<String> parameterTypeNames = TypeWriter.getReadableParameterTypes(this.parameterDescriptor);
		StringBuilder builder = new StringBuilder();
		int parameterCount = parameterTypeNames.size();

		for (int i = 0; i < parameterCount; i++) {
			builder.append(parameterTypeNames.get(i));
			if (i < parameterCount - 1) {
				builder.append(", ");
			}
		}

		return this.name + "(" + builder.toString() + ")";
	}

	public boolean isConstructor() {
		return this.name.equals("<init>");
	}

	public final String getName() {
		return this.name;
	}

	public final String getRemappedParameters(Map<String, String> classNameMappings) {
		return MethodSignature.formatRemappedParameters(this.parameterDescriptor, classNameMappings);
	}

	public abstract String getReturnDescriptor();

	public boolean isStaticInitializer() {
		return this.name.equals("<clinit>");
	}

	public final String getParameterDescriptor() {
		return this.parameterDescriptor;
	}

	public final String getSignature(Map<String, String> classNameMappings) {
		return this.name + this.getRemappedParameters(classNameMappings);
	}
}
