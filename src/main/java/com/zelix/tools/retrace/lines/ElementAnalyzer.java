package com.zelix.tools.retrace.lines;

import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.tools.retrace.ChangeLogInput;
import com.zelix.tools.retrace.MethodCandidate;
import com.zelix.tools.retrace.StacktraceElement;
import com.zelix.utils.collections.KeyValuePair;
import java.util.List;
import java.util.StringTokenizer;

public class ElementAnalyzer extends AbstractLineAnalyzer {
	public ElementAnalyzer(StacktraceElement element, ChangeLogInput changeLogInput) {
		super.stacktraceElement = element;
		if (element.isValid()) {
			this.analyzeEntry(element, changeLogInput);
			if (super.resolved) {
				String methodName = element.getMethodName();
				String methodDescriptor = element.getMethodDescriptor();
				if (methodDescriptor != null) {
					List<String> parameterTypes = TypeWriter.getReadableParameterTypes(methodDescriptor);
					StringBuilder parameterTypesBuilder = new StringBuilder();
					String[] deobfuscatedParameterTypes = this.deobfuscateParameterTypes(parameterTypes, parameterTypesBuilder, changeLogInput);
					String returnType = TypeWriter.descriptorToHumanReadable(methodDescriptor);
					String deobfuscatedReturnType = null;
					if (returnType != null && returnType.length() > 0) {
						deobfuscatedReturnType = this.deobfuscateClassName(returnType, changeLogInput);
					}

					if (methodName.equals("<init>")) {
						int lastDotIndex = super.className.lastIndexOf(".");
						String simpleClassName;
						if (lastDotIndex == -1) {
							simpleClassName = super.className;
						} else {
							simpleClassName = super.className.substring(lastDotIndex + 1);
						}

						super.methodCandidates = new MethodCandidate[1];
						super.methodCandidates[0] = new MethodCandidate(simpleClassName, deobfuscatedParameterTypes);
					} else if (methodName.equals("<clinit>")) {
						super.methodCandidates = new MethodCandidate[1];
						super.methodCandidates[0] = new MethodCandidate("static", deobfuscatedParameterTypes);
					} else {
						String[] oldMethodNames = changeLogInput.getOldMethodNames(super.className, methodName, deobfuscatedReturnType, parameterTypesBuilder.toString());
						if (oldMethodNames != null && oldMethodNames.length != 0) {
							super.methodCandidates = new MethodCandidate[oldMethodNames.length];

							for (int i = 0; i < oldMethodNames.length; i++) {
								super.methodCandidates[i] = new MethodCandidate(oldMethodNames[i], deobfuscatedParameterTypes);
							}
						} else {
							super.methodCandidates = new MethodCandidate[1];
							super.methodCandidates[0] = new MethodCandidate(methodName, deobfuscatedParameterTypes);
						}
					}
				} else if (methodName.equals("<init>")) {
					super.methodCandidates = new MethodCandidate[1];
					int noDescriptorDotIndex = super.className.lastIndexOf(".");
					String noDescriptorSimpleClassName;
					if (noDescriptorDotIndex == -1) {
						noDescriptorSimpleClassName = super.className;
					} else {
						noDescriptorSimpleClassName = super.className.substring(noDescriptorDotIndex + 1);
					}

					super.methodCandidates[0] = new MethodCandidate(noDescriptorSimpleClassName, null);
				} else if (methodName.equals("<clinit>")) {
					super.methodCandidates = new MethodCandidate[1];
					super.methodCandidates[0] = new MethodCandidate("static", AbstractLineAnalyzer.EMPTY_PARAMETER_TYPES);
				} else {
					List<KeyValuePair<String, String>> oldMethodCandidates = changeLogInput.getOldMethodCandidates(super.className, methodName);
					if (oldMethodCandidates != null) {
						super.methodCandidates = new MethodCandidate[oldMethodCandidates.size()];

						for (int candidateIndex = 0; candidateIndex < oldMethodCandidates.size(); candidateIndex++) {
							KeyValuePair<String, String> methodCandidate = oldMethodCandidates.get(candidateIndex);
							String[] candidateParameterTypes = AbstractLineAnalyzer.EMPTY_PARAMETER_TYPES;
							String parameterTypesText = methodCandidate.getValue();
							if (parameterTypesText != null) {
								StringTokenizer parameterTypesTokenizer = new StringTokenizer(parameterTypesText, ", ");
								candidateParameterTypes = new String[parameterTypesTokenizer.countTokens()];
								int parameterIndex = 0;

								while (parameterTypesTokenizer.hasMoreTokens()) {
									candidateParameterTypes[parameterIndex++] = parameterTypesTokenizer.nextToken();
								}
							}

							super.methodCandidates[candidateIndex] = new MethodCandidate(methodCandidate.getKey(), candidateParameterTypes);
						}
					} else {
						super.methodCandidates = new MethodCandidate[1];
						String fallbackMethodDescriptor = element.getMethodDescriptor();
						if (fallbackMethodDescriptor != null) {
							List<String> fallbackParameterTypes = TypeWriter.getReadableParameterTypes(fallbackMethodDescriptor);
							super.methodCandidates[0] = new MethodCandidate(methodName, fallbackParameterTypes.toArray(new String[fallbackParameterTypes.size()]));
						} else {
							super.methodCandidates[0] = new MethodCandidate(methodName, null);
						}
					}
				}
			}
		}
	}
}
