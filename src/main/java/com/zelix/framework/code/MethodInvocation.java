package com.zelix.framework.code;

import com.zelix.framework.instructions.WideConstantPoolInsnNode;
import com.zelix.framework.writer.reflect.ReflectionMethodPattern;
import java.util.Iterator;
import java.util.List;

public class MethodInvocation {
	private final InstructionListNode instructionListNode;
	private final ReflectionMethodPattern pattern;
	private final WideConstantPoolInsnNode invokeNode;
	private final String sourcePath;
	private final String methodLocation;
	private List<String> targetClassNames;
	private String memberName;

	public MethodInvocation(
		InstructionListNode instructionListNode, ReflectionMethodPattern pattern, WideConstantPoolInsnNode invokeNode, String sourcePath, String methodLocation, List<String> targetClassNames, String memberName
	) {
		this.instructionListNode = instructionListNode;
		this.pattern = pattern;
		this.invokeNode = invokeNode;
		this.sourcePath = sourcePath;
		this.methodLocation = methodLocation;
		if (targetClassNames != null) {
			for (int i = 0; i < targetClassNames.size(); i++) {
				String targetClassName = targetClassNames.get(i);
				if (targetClassName != null) {
					targetClassNames.set(i, targetClassName.replace('.', '/'));
				}
			}
		}

		this.targetClassNames = targetClassNames;
		this.memberName = memberName;
	}

	public InstructionListNode getInstructionListNode() {
		return this.instructionListNode;
	}

	public WideConstantPoolInsnNode getInvokeNode() {
		return this.invokeNode;
	}

	public ReflectionMethodPattern getPattern() {
		return this.pattern;
	}

	public boolean hasAllTargetClassNames() {
		if (this.targetClassNames == null) {
			return false;
		} else {
			Iterator<String> targetClassNameIterator = this.targetClassNames.iterator();

			while (targetClassNameIterator.hasNext()) {
				if (targetClassNameIterator.next() == null) {
					return false;
				}
			}

			return true;
		}
	}

	public List<String> getTargetClassNames() {
		return this.targetClassNames;
	}

	public String getMemberName() {
		return this.memberName;
	}

	public String getSourcePath() {
		return this.sourcePath;
	}

	public String getMethodLocation() {
		return this.methodLocation;
	}
}
