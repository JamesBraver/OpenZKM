package com.zelix.tools.retrace.lines;

import com.zelix.exceptions.StacktraceAnalysisException;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.tools.retrace.ChangeLogInput;
import com.zelix.tools.retrace.MethodCandidate;
import com.zelix.tools.retrace.StacktraceClassInfo;
import com.zelix.tools.retrace.StacktraceEntry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class EntriesAnalyzer extends AbstractLineAnalyzer {
	public EntriesAnalyzer(HashSet<StacktraceEntry> entries, ChangeLogInput changeLogInput) {
		boolean firstEntry = true;
		ArrayList<MethodCandidate> candidates = null;

		for (StacktraceEntry entry : entries) {
			if (firstEntry) {
				StacktraceClassInfo classInfo = entry.getClassInfo();
				super.stacktraceElement = classInfo.getElement();
				super.previousResolved = entry.isTraceBreak();
				if (!super.stacktraceElement.isValid()) {
					break;
				}

				candidates = new ArrayList<>(entries.size());
				MethodNode methodNode = entry.getMethod();
				this.analyzeEntry(classInfo, changeLogInput);
				MethodCandidate[] entryCandidates = this.getMethodCandidates(methodNode, changeLogInput);

				for (int candidateIndex = 0; candidateIndex < entryCandidates.length; candidateIndex++) {
					candidates.add(entryCandidates[candidateIndex]);
				}
			} else {
				MethodNode subsequentMethodNode = entry.getMethod();
				MethodCandidate[] subsequentEntryCandidates = this.getMethodCandidates(subsequentMethodNode, changeLogInput);

				for (int subsequentCandidateIndex = 0; subsequentCandidateIndex < subsequentEntryCandidates.length; subsequentCandidateIndex++) {
					candidates.add(subsequentEntryCandidates[subsequentCandidateIndex]);
				}
			}

			firstEntry = false;
		}

		int destinationIndex = 0;
		if (candidates != null) {
			super.methodCandidates = new MethodCandidate[candidates.size()];
			Iterator<MethodCandidate> candidateIterator = candidates.iterator();

			while (candidateIterator.hasNext()) {
				super.methodCandidates[destinationIndex++] = candidateIterator.next();
			}
		}
	}

	public MethodCandidate[] getMethodCandidates(MethodNode methodNode, ChangeLogInput changeLogInput) {
		StringBuilder parameterTypesBuilder = new StringBuilder();
		List<String> parameterTypes = methodNode.getReadableParameterTypes();
		String[] deobfuscatedParameterTypes = this.deobfuscateParameterTypes(parameterTypes, parameterTypesBuilder, changeLogInput);
		String returnType = methodNode.getReadableReturnType();
		String deobfuscatedReturnType = this.deobfuscateClassName(returnType, changeLogInput);
		MethodCandidate[] candidates;
		if (methodNode.isConstructor()) {
			int lastDotIndex = super.className.lastIndexOf(".");
			String simpleClassName;
			if (lastDotIndex == -1) {
				simpleClassName = super.className;
			} else {
				simpleClassName = super.className.substring(lastDotIndex + 1);
			}

			candidates = new MethodCandidate[]{new MethodCandidate(simpleClassName, deobfuscatedParameterTypes)};
		} else if (methodNode.isClinit()) {
			candidates = new MethodCandidate[]{new MethodCandidate("static", deobfuscatedParameterTypes)};
		} else {
			String methodName = methodNode.getName();
			String[] oldMethodNames = changeLogInput.getOldMethodNames(super.className, methodName, deobfuscatedReturnType, parameterTypesBuilder.toString());
			if (oldMethodNames != null && oldMethodNames.length != 0) {
				candidates = new MethodCandidate[oldMethodNames.length];

				for (int i = 0; i < oldMethodNames.length; i++) {
					candidates[i] = new MethodCandidate(oldMethodNames[i], deobfuscatedParameterTypes);
				}
			} else {
				if (changeLogInput.isOldClassNameMapped(super.className)) {
					StringBuilder parameterListBuilder = new StringBuilder();

					for (int parameterIndex = 0; parameterIndex < parameterTypes.size(); parameterIndex++) {
						parameterListBuilder.append(parameterTypes.get(parameterIndex));
						if (parameterIndex < parameterTypes.size() - 1) {
							parameterListBuilder.append(", ");
						}
					}

					throw new StacktraceAnalysisException(
						"Method '"
							+ returnType
							+ " "
							+ methodName
							+ "("
							+ parameterListBuilder.toString()
							+ ")' is in the bytecode of class with original name '"
							+ super.className
							+ "' but doesn't appear in the change log. Change log does not correspond to obfuscated bytecode."
					);
				}

				candidates = new MethodCandidate[]{new MethodCandidate(methodName, deobfuscatedParameterTypes)};
			}
		}

		return candidates;
	}
}
