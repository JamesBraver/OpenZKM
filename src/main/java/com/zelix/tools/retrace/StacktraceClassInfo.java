package com.zelix.tools.retrace;

import com.zelix.exceptions.ClassResolveException;
import com.zelix.exceptions.ClassResolveNotFoundException;
import com.zelix.exceptions.StacktraceAnalysisException;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.repository.ClassResolver;
import com.zelix.utils.GlobalUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StacktraceClassInfo implements StacktraceLine {
	private List<StacktraceMethodInfo> methods = new ArrayList<>();
	private StacktraceElement element;
	private ClassNode classNode;

	public StacktraceClassInfo(StacktraceElement element, ClassResolver resolver, boolean classNotInChangeLog) {
		this.element = element;
		if (resolver != null && this.isValid()) {
			String binaryClassName = element.getClassName();
			String binaryMethodDescriptor = element.getMethodDescriptor();

			try {
				this.classNode = resolver.resolveClass(GlobalUtils.binaryToInternalName(binaryClassName));
			} catch (ClassResolveNotFoundException ignoredClassResolveNotFound) {
				if (classNotInChangeLog) {
					element.setError("Class '" + binaryClassName + "' not found in the change log or the obfuscated bytecode classpath.");
					return;
				}

				throw new StacktraceAnalysisException(
					"Class '" + binaryClassName + "' is in the change log but NOT in the obfuscated bytecode classpath. The obfuscated bytecode classpath is incorrect or incomplete."
				);
			} catch (ClassResolveException classResolveException) {
				throw new StacktraceAnalysisException(classResolveException.getMessage());
			}

			if (binaryMethodDescriptor != null) {
				MethodSignature methodSignature = new MethodSignature(element.getMethodName(), GlobalUtils.binaryToInternalName(binaryMethodDescriptor));
				MethodNode methodNode = this.classNode.findMethodNode(methodSignature);
				if (methodNode == null) {
					if (classNotInChangeLog) {
						element.setError("Method '" + methodSignature.toHumanReadableString() + "' in class '" + binaryClassName + "' not found.");
						return;
					}

					throw new StacktraceAnalysisException(
						"Error analyzing '"
							+ element.getLine()
							+ "'. Class '"
							+ this.classNode.getBinaryName()
							+ "' contains no matching method '"
							+ methodSignature.toHumanReadableString()
							+ "'. Obfuscated bytecode classpath is incorrect."
					);
				}

				StacktraceMethodInfo methodInfo = new StacktraceMethodInfo(element.getMethodName() + GlobalUtils.binaryToInternalName(binaryMethodDescriptor), methodNode);
				this.methods.add(methodInfo);
			} else {
				this.findMatchingMethods(this.classNode, classNotInChangeLog);
			}
		}
	}

	@Override
	public boolean isValid() {
		return this.element.isValid();
	}

	@Override
	public String getLine() {
		return this.element.getLine();
	}

	public ClassNode getClassNode() {
		return this.classNode;
	}

	public List<StacktraceMethodInfo> getMethods() {
		return this.methods;
	}

	public StacktraceElement getElement() {
		return this.element;
	}

	private void findMatchingMethods(ClassNode classNode, boolean classNotInChangeLog) {
		List<AbstractMethodNode> candidateMethods = classNode.findMethodsByName(this.element.getMethodName());
		if (this.element.getLineNumber() > -1) {
			int lineNumber = this.element.getLineNumber();
			Iterator<AbstractMethodNode> methodIterator = candidateMethods.iterator();

			while (methodIterator.hasNext()) {
				MethodNode methodNode = (MethodNode)methodIterator.next();
				if (!methodNode.hasLineNumber(lineNumber)) {
					methodIterator.remove();
				}
			}
		}

		for (AbstractMethodNode candidateMethod : candidateMethods) {
			this.methods.add(new StacktraceMethodInfo(candidateMethod.getNameAndDescriptor(), (MethodNode)candidateMethod));
		}

		if (this.methods.size() == 0) {
			if (!classNotInChangeLog) {
				throw new StacktraceAnalysisException(
					"Error analyzing '"
						+ this.element.getLine()
						+ "'. Class '"
						+ classNode.getBinaryName()
						+ "' contains no matching method '"
						+ this.element.getMethodName()
						+ "'"
						+ (this.element.getLineNumber() > -1 ? " with line number " + this.element.getLineNumber() + "." : ".")
						+ " Please check that you have specified the correct obfuscated bytecode."
				);
			}

			this.element
				.setError(
					"Class '"
						+ classNode.getBinaryName()
						+ "' contains no matching method '"
						+ this.element.getMethodName()
						+ "'"
						+ (this.element.getLineNumber() > -1 ? " with line number " + this.element.getLineNumber() + "." : ".")
						+ " The classpath may be incorrect."
				);
		}
	}

	@Override
	public boolean hasLineNumber() {
		return this.element.hasLineNumber();
	}

	@Override
	public int getLineNumber() {
		return this.element.getLineNumber();
	}

	@Override
	public String getClassName() {
		if (this.classNode != null) {
			return this.classNode.getBinaryName();
		} else {
			return this.element.getClassName() != null ? this.element.getClassName() : null;
		}
	}
}
