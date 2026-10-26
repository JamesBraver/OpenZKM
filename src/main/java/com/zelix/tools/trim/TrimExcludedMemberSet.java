package com.zelix.tools.trim;

import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.utils.GlobalUtils;
import java.util.Iterator;
import java.util.Set;

public class TrimExcludedMemberSet {
	private final Set<ClassNode> classes;
	private final Set<FieldNode> fields;
	private final Set<MethodNode> methods;

	public TrimExcludedMemberSet(Set<ClassNode> classes, Set<FieldNode> fields, Set<MethodNode> methods) {
		this.classes = GlobalUtils.newHashSet(classes);
		this.fields = GlobalUtils.newHashSet(fields);
		this.methods = GlobalUtils.newHashSet(methods);
	}

	public boolean containsMethod(MethodNode methodNode) {
		return this.methods.contains(methodNode);
	}

	public void removeClass(ClassNode classNode) {
		this.classes.remove(classNode);

		for (FieldNode fieldNode : classNode.getFieldNodes()) {
			this.fields.remove(fieldNode);
		}

		for (MethodNode methodNode : classNode.getMethodNodes()) {
			this.methods.remove(methodNode);
		}
	}

	public Set<MethodNode> getMethods() {
		return this.methods;
	}

	public void retain(TrimExcludes trimExcludes) {
		Iterator<FieldNode> fieldIterator = this.fields.iterator();

		while (fieldIterator.hasNext()) {
			FieldNode field = fieldIterator.next();
			if (!trimExcludes.isFieldExcluded(field)) {
				fieldIterator.remove();
			}
		}

		Iterator<MethodNode> methodIterator = this.methods.iterator();

		while (methodIterator.hasNext()) {
			MethodNode method = methodIterator.next();
			if (!trimExcludes.isMethodExcluded(method)) {
				methodIterator.remove();
			}
		}
	}
}
