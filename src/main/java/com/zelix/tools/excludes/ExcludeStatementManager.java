package com.zelix.tools.excludes;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.MemberNode;
import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.writer.base.TypeWriter;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.annotation.ComplexAnnotationSpecifier;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.GlobalUtils;
import com.zelix.workspace.Workspace;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

public abstract class ExcludeStatementManager implements ClassHierarchy {
	protected final Workspace workspace;
	protected final List<? extends ExclusionInclusionStatement> exclusionStatements;
	protected final ScriptContext scriptContext;
	protected final PrintWriter logWriter;

	public ExcludeStatementManager(Workspace workspace, List<? extends ExclusionInclusionStatement> fixedClassesStatements, ScriptContext scriptContext) {
		this.workspace = workspace;
		this.exclusionStatements = fixedClassesStatements;
		this.scriptContext = scriptContext;
		this.logWriter = scriptContext.getWriter();
	}

	public static String getPackageNameForMatching(AbstractClassNode classNode) {
		return Options.ZKM_MATCH_ON_ORIGINAL ? classNode.getPackageNameInternal() : classNode.getPackageName();
	}

	public static final String formatFieldForDisplay(AbstractFieldNode fieldNode, ClassHierarchy classHierarchy) {
		return getFormattedFieldSignature(fieldNode, true, classHierarchy);
	}

	public static String getFormattedMethodSignature(AbstractMethodNode methodNode, boolean includeAnnotations, ClassHierarchy classHierarchy) {
		StringBuilder signatureBuilder = new StringBuilder();
		if (includeAnnotations) {
			signatureBuilder.append(methodNode.getAnnotationsAsString(classHierarchy));
		}

		signatureBuilder.append(methodNode.getAccessFlagsString());
		signatureBuilder.append(methodNode.getSignature());
		if (methodNode.isNameChanged() || methodNode.isDescriptorChanged()) {
			signatureBuilder.append(" (");
			signatureBuilder.append(methodNode.toDeclarationStringSimple(false).trim());
			signatureBuilder.append(')');
		}

		return signatureBuilder.toString();
	}

	public static String getDescriptorForMatching(MemberNode memberNode) {
		return Options.ZKM_MATCH_ON_ORIGINAL ? memberNode.getOriginalDescriptor() : memberNode.getDescriptor();
	}

	public static final String getFormattedFieldSignature(AbstractFieldNode fieldNode, boolean includeAnnotations, ClassHierarchy classHierarchy) {
		StringBuilder signatureBuilder = new StringBuilder();
		if (includeAnnotations) {
			signatureBuilder.append(fieldNode.getAnnotationsAsString(classHierarchy));
		}

		signatureBuilder.append(fieldNode.getAccessFlagsString());
		String descriptor = fieldNode.getOriginalDescriptor();
		if (descriptor.length() == 1) {
			signatureBuilder.append(TypeWriter.getPrimitiveName(descriptor));
		} else {
			signatureBuilder.append(GlobalUtils.typeDescriptorToBinaryName(descriptor));
		}

		signatureBuilder.append(" ");
		signatureBuilder.append(fieldNode.getOriginalName());
		if (fieldNode.isNameChanged() || fieldNode.isDescriptorChanged()) {
			signatureBuilder.append(" (");
			String readableType = fieldNode.getReadableType();
			signatureBuilder.append(readableType);
			signatureBuilder.append(" ");
			signatureBuilder.append(fieldNode.getNameDisplay());
			signatureBuilder.append(')');
		}

		return signatureBuilder.toString();
	}

	public static String getNameForMatching(AbstractMethodNode methodNode) {
		return Options.ZKM_MATCH_ON_ORIGINAL ? methodNode.getOriginalName() : methodNode.getName();
	}

	public static String getSimpleNameForMatching(AbstractClassNode classNode) {
		return Options.ZKM_MATCH_ON_ORIGINAL ? classNode.getSimpleName() : classNode.getSimpleNameInternal();
	}

	public static final String getFormattedClassSignature(AbstractClassNode classNode, ClassHierarchy classHierarchy, boolean includePermittedSubclasses) {
		StringBuilder signatureBuilder = new StringBuilder();
		if (includePermittedSubclasses) {
			signatureBuilder.append(classNode.getAnnotationsAsString(classHierarchy));
		}

		if (signatureBuilder.length() > 0 && signatureBuilder.charAt(signatureBuilder.length() - 1) != ' ') {
			signatureBuilder.append(' ');
		}

		signatureBuilder.append(classNode.getAccessFlagsString());
		if (signatureBuilder.length() > 0 && signatureBuilder.charAt(signatureBuilder.length() - 1) != ' ') {
			signatureBuilder.append(' ');
		}

		signatureBuilder.append(classNode.getClassNameBinary());
		if (classNode.isNameChanged()) {
			signatureBuilder.append(" (");
			signatureBuilder.append(classNode.getBinaryName());
			signatureBuilder.append(')');
		}

		return signatureBuilder.toString();
	}

	public static final String formatClassForDisplay(AbstractClassNode classNode, ClassHierarchy classHierarchy) {
		return getFormattedClassSignature(classNode, classHierarchy, true);
	}

	public static String getNameForMatching(AbstractFieldNode fieldNode) {
		return Options.ZKM_MATCH_ON_ORIGINAL ? fieldNode.getOriginalName() : fieldNode.getNameDisplay();
	}

	public static String formatMethodForDisplay(AbstractMethodNode methodNode, ClassHierarchy classHierarchy) {
		return getFormattedMethodSignature(methodNode, true, classHierarchy);
	}

	@Override
	public final boolean isInstanceOf(String className, String target, ComplexAnnotationSpecifier annotationSpecifier) {
		return this.workspace.isInstanceOf(className, target, annotationSpecifier);
	}

	@Override
	public final boolean hasHierarchySnapshot() {
		return this.workspace.hasHierarchySnapshot();
	}

	protected final String formatClassForDisplay(AbstractClassNode classNode) {
		return getFormattedClassSignature(classNode, this.workspace, true);
	}

	@Override
	public boolean isObjectMethodSignature(MethodSignature methodSignature) {
		return this.workspace.isObjectMethodSignature(methodSignature);
	}

	public boolean shouldMatchCurrentFieldName() {
		return !Options.ZKM_MATCH_ON_ORIGINAL || !this.workspace.isFieldNameObfFinish();
	}

	@Override
	public final boolean isSubclassOf(String className, String target) {
		return this.workspace.isSubclassOf(className, target);
	}

	@Override
	public ClassProvider getClassProvider() {
		return this.workspace.getClassProvider();
	}

	@Override
	public final boolean isAssignableFrom(String className, String target) {
		return this.workspace.isAssignableFrom(className, target);
	}

	@Override
	public final Set<String> getAnnotationTypes(String className, Integer innerClassOrdinal, boolean matchOnOriginalNames) {
		return this.workspace.getAnnotationTypes(className, innerClassOrdinal, matchOnOriginalNames);
	}

	@Override
	public final boolean isMethodNameObfFinish() {
		return this.workspace.isMethodNameObfFinish();
	}

	@Override
	public final boolean isSuperclassOf(String className, String target) {
		return this.workspace.isSuperclassOf(className, target);
	}

	@Override
	public final boolean isInterfaceOf(String className, String target, ComplexAnnotationSpecifier annotationSpecifier) {
		return this.workspace.isInterfaceOf(className, target, annotationSpecifier);
	}

	public boolean shouldMatchCurrentMethodName() {
		return !Options.ZKM_MATCH_ON_ORIGINAL || !this.workspace.isMethodNameObfFinish();
	}

	@Override
	public final boolean isClassInstanceOf(String className, String target) {
		return this.workspace.isClassInstanceOf(className, target);
	}

	@Override
	public final boolean isFieldNameObfFinish() {
		return this.workspace.isFieldNameObfFinish();
	}

	@Override
	public final boolean isInterfaceImplementedBy(String className, String target, ComplexAnnotationSpecifier annotationSpecifier) {
		return this.workspace.isInterfaceImplementedBy(className, target, annotationSpecifier);
	}

	@Override
	public String getBinaryName(String internalName) {
		return this.workspace.getBinaryName(internalName);
	}

	@Override
	public final boolean isOriginalSuperclassOf(String className, String target, ComplexAnnotationSpecifier annotationSpecifier) {
		return this.workspace.isOriginalSuperclassOf(className, target, annotationSpecifier);
	}
}
