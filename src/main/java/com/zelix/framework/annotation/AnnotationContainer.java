package com.zelix.framework.annotation;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.constant.ConstantPoolVisitor;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.repository.ClassProvider;
import com.zelix.tools.trim.TrimExcludes;
import com.zelix.workspace.AbstractWorkspace;
import com.zelix.workspace.PhantomClassManager;
import java.io.PrintWriter;
import java.util.Set;

public interface AnnotationContainer {
	String getName();

	void resolveAnnotations(AbstractWorkspace workspace, ClassProvider classProvider, PhantomClassManager phantomClassManager, ScriptContext scriptContext);

	void updateConstants();

	void acceptVisitor(ConstantPoolVisitor visitor);

	boolean trimExcludedAnnotations(TrimExcludes trimExcludes, ScriptContext scriptContext, PrintWriter printWriter);

	void syncNames();

	void collectClassReferences(Set<AbstractClassNode> classNodes);

	void collectAllReferences(Set<ClassNode> classReferences, Set<ClassNode> signatureClassReferences, Set<FieldNode> fieldReferences, Set<MethodNode> methodReferences);
}
