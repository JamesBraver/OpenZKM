package com.zelix.tools.longEncryption;

import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.exclusion.LongEncryptionExcludeStatement;
import com.zelix.klassmaster.script.statements.exclusion.LongEncryptionUnexcludeStatement;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import com.zelix.tools.excludes.ExcludeStatementManager;
import com.zelix.tools.excludes.InclusionExclusionManager;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.comparators.ExcludeSorter;
import com.zelix.utils.enumerations.ArrayEnumeration;
import com.zelix.utils.enumerations.EmptyEnumeration;
import com.zelix.workspace.Workspace;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class LongEncryptionExcludes extends InclusionExclusionManager {
	private MultiMap<ClassNode, FieldNode> excludedFieldsByClass = new MultiMap<>();
	private MultiMap<ClassNode, MethodNode> excludedMethodsByClass = new MultiMap<>();

	public LongEncryptionExcludes(Workspace workspace, List<LongEncryptionExcludeStatement> excludeStatements, List<LongEncryptionUnexcludeStatement> unexcludeStatements, ScriptContext scriptContext) {
		super(workspace, excludeStatements, unexcludeStatements, scriptContext);
		if (workspace.hasClasses()) {
			this.initializeTargets(workspace.getClasses(), workspace.getWorkspaceClassesSize());
			this.applyScriptRules();
		}
	}

	public final void includeFieldConstants(FieldNode field, String reason) {
		if (field.isLongType()) {
			ClassNode ownerClass = field.getOwnerClass();
			if (super.excludedClasses.containsKey(ownerClass)) {
				super.scriptContext
					.logWarning(
						"Cannot UNexclude initialization value of field \""
							+ ExcludeStatementManager.formatFieldForDisplay(field, this)
							+ "\" in class \""
							+ this.formatClassForDisplay(ownerClass)
							+ "\" because class is excluded from all Long encryption."
					);
			} else {
				ClassNode removedOwnerClass = super.excludedFields.remove(field);
				if (removedOwnerClass != null) {
					super.processableFields.put(field, removedOwnerClass);
					this.excludedFieldsByClass.remove(field.getOwnerClass(), field);
					if (super.scriptContext.isVerbose() && super.logWriter != null) {
						super.logWriter
							.println(
								"\tUNEXCLUDING initialization value of field \""
									+ ExcludeStatementManager.formatFieldForDisplay(field, this)
									+ "\" in class \""
									+ this.formatClassForDisplay(ownerClass)
									+ "\" from Long Constant Encryption because of \""
									+ reason
									+ "\""
							);
					}
				}
			}
		}
	}

	public Enumeration<FieldNode> getExcludedFieldsForClass(ClassNode classNode) {
		List<FieldNode> excludedFields = this.excludedFieldsByClass.get(classNode);
		return (Enumeration<FieldNode>)(excludedFields != null ? Collections.enumeration(excludedFields) : new EmptyEnumeration<>());
	}

	public boolean hasClassWithExcludedFields(ClassNode classNode) {
		return this.excludedFieldsByClass.containsKey(classNode);
	}

	@Override
	public final boolean excludeClass(ClassNode classNode, String reason) {
		Object removedClass = super.processableClasses.remove(classNode);
		if (removedClass != null) {
			super.excludedClasses.put(classNode, classNode);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				super.logWriter.println("\tExcluding from Long Constant Encryption class \"" + this.formatClassForDisplay(classNode) + "\" because of \"" + reason + "\"");
			}
		}

		ArrayEnumeration<FieldNode> fieldEnumeration = classNode.getFieldEnumeration();

		while (fieldEnumeration.hasMoreElements()) {
			FieldNode fieldNode = fieldEnumeration.nextElement();
			if (fieldNode.isLongType()) {
				this.excludedFieldsByClass.put(classNode, fieldNode);
				if (super.processableFields.remove(fieldNode) != null) {
					super.excludedFields.put(fieldNode, classNode);
				}
			}
		}

		ArrayEnumeration<MethodNode> methodEnumeration = classNode.getMethodEnumeration();

		while (methodEnumeration.hasMoreElements()) {
			MethodNode methodNode = methodEnumeration.nextElement();
			this.excludedMethodsByClass.put(classNode, methodNode);
			if (this.processableMethods.remove(methodNode) != null) {
				this.excludedMethods.put(methodNode, classNode);
			}
		}

		return removedClass != null;
	}

	private final void applyScriptRules() {
		int exclusionStatementCount;
		if (super.exclusionStatements == null) {
			exclusionStatementCount = 0;
		} else {
			exclusionStatementCount = super.exclusionStatements.size();
		}

		HashMap<RenameFilterParameter, RenameFilterParameter> seenExclusionParameters = GlobalUtils.newHashMap();
		Vector<RenameFilterParameter> distinctExclusionParameters = new Vector<>();

		for (int exclusionStatementIndex = 0; exclusionStatementIndex < exclusionStatementCount; exclusionStatementIndex++) {
			Enumeration<RenameFilterParameter> exclusionParameterEnumeration = super.exclusionStatements.get(exclusionStatementIndex).getRenameFilterParameters();

			while (exclusionParameterEnumeration.hasMoreElements()) {
				RenameFilterParameter exclusionParameter = exclusionParameterEnumeration.nextElement();
				if (!seenExclusionParameters.containsKey(exclusionParameter)) {
					seenExclusionParameters.put(exclusionParameter, exclusionParameter);
					distinctExclusionParameters.addElement(exclusionParameter);
				}
			}
		}

		Collections.sort(distinctExclusionParameters, new ExcludeSorter());
		if (super.scriptContext.isVerbose() && distinctExclusionParameters.size() > 0) {
			super.logWriter.println("\tLong Constant Encryption Exclusion parameters:");

			for (RenameFilterParameter exclusionParameterToDisplay : distinctExclusionParameters) {
				super.logWriter.println("\t\t\"" + exclusionParameterToDisplay + "\"");
			}
		}

		for (RenameFilterParameter exclusionParameterToApply : distinctExclusionParameters) {
			if (!exclusionParameterToApply.isClassTarget() && !exclusionParameterToApply.isFieldTarget() && !exclusionParameterToApply.isMethodTarget()) {
				super.scriptContext
					.logWarningInternal(
						"Parameter '" + exclusionParameterToApply + "' in 'encryptLongConstantsExclude' statement is not a class, field or method exclusion parameter. It will be ignored.", true
					);
			} else if (exclusionParameterToApply.isClassTarget() && exclusionParameterToApply.isLinkClass()) {
				super.scriptContext
					.logWarningInternal("Parameter '" + exclusionParameterToApply + "' in 'encryptLongConstantsExclude' statement cannot use the '<link>' syntax. It will be ignored.", true);
			} else if (exclusionParameterToApply.isFieldTarget() && exclusionParameterToApply.isClassSpecifierRecursive()) {
				super.scriptContext
					.logWarningInternal("Parameter '" + exclusionParameterToApply + "' in 'encryptLongConstantsExclude' statement cannot use the '^' syntax. It will be ignored.", true);
			} else {
				if (exclusionParameterToApply.isClassTarget() && exclusionParameterToApply.isClassWildcard()) {
					super.scriptContext
						.logWarningInternal("Parameter '" + exclusionParameterToApply + "' in 'encryptLongConstantsExclude' cannot use the '+' tag. The tag will be ignored.", true);
				}

				exclusionParameterToApply.applyLongEncryptionExclusion(this);
			}
		}

		int inclusionStatementCount;
		if (super.inclusionStatements == null) {
			inclusionStatementCount = 0;
		} else {
			inclusionStatementCount = super.inclusionStatements.size();
		}

		HashMap<RenameFilterParameter, RenameFilterParameter> seenInclusionParameters = GlobalUtils.newHashMap();
		Vector<RenameFilterParameter> distinctInclusionParameters = new Vector<>();

		for (int inclusionStatementIndex = 0; inclusionStatementIndex < inclusionStatementCount; inclusionStatementIndex++) {
			Enumeration<RenameFilterParameter> inclusionParameterEnumeration = super.inclusionStatements.get(inclusionStatementIndex).getRenameFilterParameters();

			while (inclusionParameterEnumeration.hasMoreElements()) {
				RenameFilterParameter inclusionParameter = inclusionParameterEnumeration.nextElement();
				if (!seenInclusionParameters.containsKey(inclusionParameter)) {
					seenInclusionParameters.put(inclusionParameter, inclusionParameter);
					distinctInclusionParameters.addElement(inclusionParameter);
				}
			}
		}

		if (distinctInclusionParameters.size() > 0 && distinctExclusionParameters.size() == 0) {
			super.scriptContext
				.logWarningInternal(
					"A 'encryptLongConstantsUnexclude' statement can only operate on the results of a 'encryptLongConstantsExclude' statement. No 'encryptLongConstantsExclude' statement is in effect so the 'encryptLongConstantsUnexclude' statement will be ignored.",
					true
				);
		} else {
			Collections.sort(distinctInclusionParameters, new ExcludeSorter());
			if (super.scriptContext.isVerbose() && distinctInclusionParameters.size() > 0) {
				super.logWriter.println("\tLong Constant Encryption Unexclusion parameters:");

				for (RenameFilterParameter inclusionParameterToDisplay : distinctInclusionParameters) {
					super.logWriter.println("\t\t\"" + inclusionParameterToDisplay + "\"");
				}
			}

			for (RenameFilterParameter inclusionParameterToApply : distinctInclusionParameters) {
				if (!inclusionParameterToApply.isClassTarget() && !inclusionParameterToApply.isFieldTarget() && !inclusionParameterToApply.isMethodTarget()) {
					super.scriptContext
						.logWarningInternal(
							"Parameter '" + inclusionParameterToApply + "' in 'encryptLongConstantsUnexclude' statement is not a class, field or method exclusion parameter. It will be ignored.", true
						);
				} else if (inclusionParameterToApply.isClassTarget() && inclusionParameterToApply.isLinkClass()) {
					super.scriptContext
						.logWarningInternal("Parameter '" + inclusionParameterToApply + "' in 'encryptLongConstantsUnexclude' statement cannot use the '<link>' syntax. It will be ignored.", true);
				} else if (inclusionParameterToApply.isFieldTarget() && inclusionParameterToApply.isClassSpecifierRecursive()) {
					super.scriptContext
						.logWarningInternal("Parameter '" + inclusionParameterToApply + "' in 'encryptLongConstantsUnexclude' statement cannot use the '^' syntax. It will be ignored.", true);
				} else {
					if (inclusionParameterToApply.isClassTarget() && inclusionParameterToApply.isClassWildcard()) {
						super.scriptContext
							.logWarningInternal("Parameter '" + inclusionParameterToApply + "' in 'encryptLongConstantsUnexclude' cannot use the '+' tag. The tag will be ignored.", true);
					}

					inclusionParameterToApply.applyLongEncryptionInclusion(this);
				}
			}
		}
	}

	public final void initializeTargets(Enumeration<ClassNode> classes, int classCount) {
		super.processableClasses = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount));
		super.excludedClasses = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount));
		super.processableFields = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));
		super.excludedFields = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));
		this.processableMethods = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));
		this.excludedMethods = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));

		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			super.processableClasses.put(classNode, classNode);
			ArrayEnumeration<FieldNode> fieldEnumeration = classNode.getFieldEnumeration();

			while (fieldEnumeration.hasMoreElements()) {
				FieldNode fieldNode = fieldEnumeration.nextElement();
				super.processableFields.put(fieldNode, fieldNode.getOwnerClass());
			}

			ArrayEnumeration<MethodNode> methodEnumeration = classNode.getMethodEnumeration();

			while (methodEnumeration.hasMoreElements()) {
				MethodNode methodNode = methodEnumeration.nextElement();
				this.processableMethods.put(methodNode, methodNode.getMethodOwner());
			}
		}
	}

	@Override
	public final void includeClass(ClassNode classNode, String reason) {
		Object removedClass = super.excludedClasses.remove(classNode);
		if (removedClass != null) {
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				super.logWriter.println("\tUNEXCLUDING from Long Constant Encryption class \"" + this.formatClassForDisplay(classNode) + "\" because of \"" + reason + "\"");
			}

			super.processableClasses.put(classNode, classNode);
		}

		List<FieldNode> removedExcludedFields = this.excludedFieldsByClass.removeAll(classNode);
		if (removedClass == null && removedExcludedFields != null && super.scriptContext.isVerbose() && super.logWriter != null) {
			super.logWriter
				.println(
					"\tUNEXCLUDING from Long Constant Encryption initialization values of all fields of class \""
						+ this.formatClassForDisplay(classNode)
						+ "\" because of \""
						+ reason
						+ "\""
				);
		}

		ArrayEnumeration<FieldNode> fieldEnumeration = classNode.getFieldEnumeration();

		while (fieldEnumeration.hasMoreElements()) {
			FieldNode fieldNode = fieldEnumeration.nextElement();
			if (fieldNode.isLongType() && super.excludedFields.remove(fieldNode) != null) {
				super.processableFields.put(fieldNode, classNode);
			}
		}

		List<MethodNode> removedExcludedMethods = this.excludedMethodsByClass.removeAll(classNode);
		if (removedClass == null && removedExcludedMethods != null && super.scriptContext.isVerbose() && super.logWriter != null) {
			super.logWriter
				.println(
					"\tUNEXCLUDING from Long Constant Encryption all long constants referenced by methods of class \""
						+ this.formatClassForDisplay(classNode)
						+ "\" because of \""
						+ reason
						+ "\""
				);
		}

		ArrayEnumeration<MethodNode> methodEnumeration = classNode.getMethodEnumeration();

		while (methodEnumeration.hasMoreElements()) {
			MethodNode methodNode = methodEnumeration.nextElement();
			if (this.excludedMethods.remove(methodNode) != null) {
				this.processableMethods.put(methodNode, classNode);
			}
		}
	}

	public final void excludeMethodConstants(MethodNode method, String reason) {
		ClassNode ownerClass = this.processableMethods.remove(method);
		if (ownerClass != null) {
			this.excludedMethods.put(method, ownerClass);
			this.excludedMethodsByClass.put(ownerClass, method);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				ClassNode displayOwnerClass = method.getMethodOwner();
				super.logWriter
					.println(
						"\tExcluding all Long constants in method \""
							+ ExcludeStatementManager.formatMethodForDisplay(method, this)
							+ "\" in class \""
							+ this.formatClassForDisplay(displayOwnerClass)
							+ "\" from Long Constant Encryption because of \""
							+ reason
							+ "\""
					);
			}
		}
	}

	public final void excludeFieldConstants(FieldNode field, String reason) {
		if (field.isLongType()) {
			ClassNode ownerClass = super.processableFields.remove(field);
			if (ownerClass != null) {
				super.excludedFields.put(field, ownerClass);
				this.excludedFieldsByClass.put(ownerClass, field);
				if (super.scriptContext.isVerbose() && super.logWriter != null) {
					ClassNode displayOwnerClass = field.getOwnerClass();
					super.logWriter
						.println(
							"\tExcluding initialization value of field \""
								+ ExcludeStatementManager.formatFieldForDisplay(field, this)
								+ "\" in class \""
								+ this.formatClassForDisplay(displayOwnerClass)
								+ "\" from Long Constant Encryption because of \""
								+ reason
								+ "\""
						);
				}
			}
		}
	}

	public boolean hasClassWithExcludedMethods(ClassNode classNode) {
		return this.excludedMethodsByClass.containsKey(classNode);
	}

	public Enumeration<MethodNode> getExcludedMethodsForClass(ClassNode classNode) {
		List<MethodNode> excludedMethods = this.excludedMethodsByClass.get(classNode);
		return (Enumeration<MethodNode>)(excludedMethods != null ? Collections.enumeration(excludedMethods) : new EmptyEnumeration<>());
	}

	public final void includeMethodConstants(MethodNode method, String reason) {
		ClassNode ownerClass = method.getMethodOwner();
		if (super.excludedClasses.containsKey(ownerClass)) {
			super.scriptContext
				.logWarning(
					"Cannot UNexclude Long constants in method \""
						+ ExcludeStatementManager.formatMethodForDisplay(method, this)
						+ "\" in class \""
						+ this.formatClassForDisplay(ownerClass)
						+ "\" because class is excluded from all Long Constant Encryption."
				);
		} else {
			ClassNode removedOwnerClass = this.excludedMethods.remove(method);
			if (removedOwnerClass != null) {
				this.processableMethods.put(method, removedOwnerClass);
				this.excludedMethodsByClass.remove(method.getMethodOwner(), method);
				if (super.scriptContext.isVerbose() && super.logWriter != null) {
					super.logWriter
						.println(
							"\tUNEXCLUDING all Long constants in method \""
								+ ExcludeStatementManager.formatMethodForDisplay(method, this)
								+ "\" in class \""
								+ this.formatClassForDisplay(ownerClass)
								+ "\" from Long Constant Encryption because of \""
								+ reason
								+ "\""
						);
				}
			}
		}
	}
}
