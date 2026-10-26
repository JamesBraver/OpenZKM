package com.zelix.workspace;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.hierarchy.ClassHierarchy;
import com.zelix.framework.reference.ReferenceGraphMap;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.annotation.ComplexAnnotationSpecifier;
import com.zelix.klassmaster.script.statements.exclusion.IgnoreMissingReferencesStatement;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.references.AtomicReference;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class PhantomClassManager implements ClassHierarchy {
	private final Set<String> ignoredClassNames = GlobalUtils.newHashSet();
	private final ReferenceGraphMap<String, String, String> ignoredReferences = new ReferenceGraphMap<>();
	private final Workspace workspace;
	private final ScriptContext scriptContext;
	private final List<RenameFilterParameter> referenceFilterParams;

	public PhantomClassManager(Workspace workspace, List<IgnoreMissingReferencesStatement> ignoreStatements, ScriptContext scriptContext) {
		this.workspace = workspace;
		this.scriptContext = scriptContext;
		this.referenceFilterParams = this.collectReferenceFilters(ignoreStatements);
		this.validateReferenceFilters();
	}

	private List<RenameFilterParameter> collectReferenceFilters(List<IgnoreMissingReferencesStatement> ignoreStatements) {
		HashSet<RenameFilterParameter> seenFilters = GlobalUtils.newHashSet();
		ArrayList<RenameFilterParameter> distinctFilters = new ArrayList<>();

		for (int i = 0; i < ignoreStatements.size(); i++) {
			Enumeration<RenameFilterParameter> statementFilters = ignoreStatements.get(i).getRenameFilterParameters();

			while (statementFilters.hasMoreElements()) {
				RenameFilterParameter filter = statementFilters.nextElement();
				if (seenFilters.add(filter)) {
					distinctFilters.add(filter);
				}
			}
		}

		return distinctFilters;
	}

	@Override
	public final boolean isInstanceOf(String className, String target, ComplexAnnotationSpecifier annotationSpecifier) {
		return this.workspace.isInstanceOf(className, target, annotationSpecifier);
	}

	public ScriptContext getScriptContext() {
		return this.scriptContext;
	}

	@Override
	public final boolean hasHierarchySnapshot() {
		return this.workspace.hasHierarchySnapshot();
	}

	@Override
	public final boolean isObjectMethodSignature(MethodSignature methodSignature) {
		return this.workspace.isObjectMethodSignature(methodSignature);
	}

	public boolean isInIgnoredClasses(String internalName) {
		return this.ignoredClassNames.contains(internalName);
	}

	@Override
	public final boolean isSubclassOf(String className, String target) {
		return this.workspace.isSubclassOf(className, target);
	}

	private void validateReferenceFilters() {
		Iterator<RenameFilterParameter> filterIterator = this.referenceFilterParams.iterator();

		while (filterIterator.hasNext()) {
			RenameFilterParameter filter = filterIterator.next();
			if (filter.isPackageTarget()) {
				this.scriptContext.logMessageInternal("Parameter '" + filter + "' in 'ignoreMissingReferences' statement is a 'package' level parameter. It will be ignored.", true);
				filterIterator.remove();
			} else if (filter.isClassTarget() && filter.isLinkClass()) {
				this.scriptContext.logWarningInternal("Parameter '" + filter + "' in 'ignoreMissingReferences' statement cannot use the '<link>' syntax. It will be ignored.", true);
				filterIterator.remove();
			} else if (filter.isMethodTarget() && filter.hasLinkMethodNamePattern()) {
				this.scriptContext.logWarningInternal("Parameter '" + filter + "' in 'ignoreMissingReferences' statement cannot use the '<link>' syntax. It will be ignored.", true);
				filterIterator.remove();
			} else if (filter.isClassTarget() && filter.hasContainingClause()) {
				this.scriptContext
					.logWarningInternal("Parameter '" + filter + "' in 'ignoreMissingReferences' statement cannot use the 'containing{...}' syntax. It will be ignored.", true);
			} else if (filter.isClassTarget() && filter.hasExtendsClause()) {
				this.scriptContext.logWarningInternal("Parameter '" + filter + "' in 'ignoreMissingReferences' statement cannot use the 'extends' syntax. It will be ignored.", true);
			} else if (filter.isClassTarget() && filter.isAnnotationType()) {
				this.scriptContext
					.logWarningInternal(
						"Parameter '" + filter + "' in 'ignoreMissingReferences' statement cannot use the standalone annotation syntax. It will be ignored.", true
					);
			} else if (filter.isClassTarget() && filter.hasImplementsClause()) {
				this.scriptContext.logWarningInternal("Parameter '" + filter + "' in 'ignoreMissingReferences' statement cannot use the 'implements' syntax. It will be ignored.", true);
			} else if (filter.isMethodTarget() && filter.hasIncludeSignatureClasses()) {
				this.scriptContext.logWarningInternal("Parameter '" + filter + "' in 'ignoreMissingReferences' cannot use the '+signatureClasses' keyword. It will be ignored.", true);
			} else if (filter.isFieldTarget() && filter.isClassSpecifierRecursive()) {
				this.scriptContext
					.logWarningInternal(
						"Parameter '" + filter + "' in 'ignoreMissingReferences' specification of containing class will ignored. However, field specification will be effective.",
						true
					);
			} else if (filter.isMethodTarget() && filter.isClassSpecifierRecursive()) {
				this.scriptContext
					.logWarningInternal(
						"Parameter '" + filter + "' in 'ignoreMissingReferences' specification of containing class will ignored. However, method specification will be effective.",
						true
					);
			}
		}
	}

	@Override
	public final boolean isAssignableFrom(String className, String target) {
		return this.workspace.isAssignableFrom(className, target);
	}

	@Override
	public Set<String> getAnnotationTypes(String className, Integer innerClassOrdinal, boolean matchOnOriginalNames) {
		return this.workspace.getAnnotationTypes(className, innerClassOrdinal, matchOnOriginalNames);
	}

	@Override
	public ClassProvider getClassProvider() {
		return this.workspace.getClassProvider();
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

	@Override
	public final boolean isClassInstanceOf(String className, String target) {
		return this.workspace.isClassInstanceOf(className, target);
	}

	public boolean isFieldIgnored(AbstractClassNode classNode, String fieldName, String fieldDescriptor, AtomicReference<String> matchedRule) {
		matchedRule.setNull();
		if (this.referenceFilterParams.isEmpty()) {
			matchedRule.set("No explicit specifications so ALL ignored by default");
			this.ignoredReferences.addReference(classNode.getInternalName(), fieldName, fieldDescriptor);
			return true;
		} else {
			for (RenameFilterParameter filter : this.referenceFilterParams) {
				if (filter.isFieldTarget() && filter.matchesFieldByNameAndType(this, classNode, fieldName, fieldDescriptor)) {
					matchedRule.set(filter.toRuleString());
					this.ignoredReferences.addReference(classNode.getInternalName(), fieldName, fieldDescriptor);
					return true;
				}

				if (filter.isClassTarget() && filter.isClassWildcard()) {
					matchedRule.set(filter.toRuleString());
					this.ignoredReferences.addReference(classNode.getInternalName(), fieldName, fieldDescriptor);
					return true;
				}
			}

			return false;
		}
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
	public final boolean isOriginalSuperclassOf(String className, String target, ComplexAnnotationSpecifier annotationSpecifier) {
		return this.workspace.isOriginalSuperclassOf(className, target, annotationSpecifier);
	}

	@Override
	public String getBinaryName(String internalName) {
		return this.workspace.getBinaryName(internalName);
	}

	public boolean isClassIgnored(String internalName, AtomicReference<String> matchedRule) {
		matchedRule.setNull();
		if (this.referenceFilterParams.isEmpty()) {
			matchedRule.set("No explicit specifications so ALL ignored by default");
			this.ignoredClassNames.add(internalName);
			return true;
		} else {
			for (RenameFilterParameter filter : this.referenceFilterParams) {
				if (filter.isClassTarget() && filter.matchesPackageAndSimpleName(internalName)) {
					matchedRule.set(filter.toRuleString());
					this.ignoredClassNames.add(internalName);
					return true;
				}

				if ((filter.isFieldTarget() || filter.isMethodTarget()) && filter.matchesClassSimpleName(internalName)) {
					matchedRule.set(filter.toRuleString());
					this.ignoredClassNames.add(internalName);
					return true;
				}
			}

			return false;
		}
	}

	public boolean isMethodIgnored(AbstractClassNode classNode, String methodName, String methodDescriptor, AtomicReference<String> matchedRule) {
		matchedRule.setNull();
		if (this.referenceFilterParams.isEmpty()) {
			matchedRule.set("No explicit specifications so ALL ignored by default");
			this.ignoredReferences.addReference(classNode.getInternalName(), methodName, methodDescriptor);
			return true;
		} else {
			for (RenameFilterParameter filter : this.referenceFilterParams) {
				if (filter.isMethodTarget() && filter.matchesMethodInClass(this, classNode, methodName, methodDescriptor)) {
					matchedRule.set(filter.toRuleString());
					this.ignoredReferences.addReference(classNode.getInternalName(), methodName, methodDescriptor);
					return true;
				}

				if (filter.isClassTarget() && filter.isClassWildcard()) {
					matchedRule.set(filter.toRuleString());
					this.ignoredReferences.addReference(classNode.getInternalName(), methodName, methodDescriptor);
					return true;
				}
			}

			return false;
		}
	}
}
