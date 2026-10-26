package com.zelix.tools.integerEncryption;

import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.exclusion.IntegerEncryptionExcludeStatement;
import com.zelix.klassmaster.script.statements.exclusion.IntegerEncryptionUnexcludeStatement;
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

public class IntegerEncryptionExcludes extends InclusionExclusionManager {
	private MultiMap<ClassNode, FieldNode> excludedFieldsByClass = new MultiMap<>();
	private MultiMap<ClassNode, MethodNode> excludedMethodsByClass = new MultiMap<>();

	public IntegerEncryptionExcludes(
		Workspace workspace, List<IntegerEncryptionExcludeStatement> excludeStatements, List<IntegerEncryptionUnexcludeStatement> unexcludeStatements, ScriptContext scriptContext
	) {
		super(workspace, excludeStatements, unexcludeStatements, scriptContext);
		if (workspace.hasClasses()) {
			this.initializeTargets(workspace.getClasses(), workspace.getWorkspaceClassesSize());
			this.applyScriptRules();
		}
	}

	public Enumeration<FieldNode> getExcludedFieldsForClass(ClassNode classNode) {
		List<FieldNode> excludedFields = this.excludedFieldsByClass.get(classNode);
		return (Enumeration<FieldNode>)(excludedFields != null ? Collections.enumeration(excludedFields) : new EmptyEnumeration<>());
	}

	public Enumeration<MethodNode> getExcludedMethodsForClass(ClassNode classNode) {
		List<MethodNode> excludedMethods = this.excludedMethodsByClass.get(classNode);
		return (Enumeration<MethodNode>)(excludedMethods != null ? Collections.enumeration(excludedMethods) : new EmptyEnumeration<>());
	}

	public final void excludeMethodConstants(MethodNode method, String reason) {
		ClassNode removedMethodOwner = this.processableMethods.remove(method);
		if (removedMethodOwner != null) {
			this.excludedMethods.put(method, removedMethodOwner);
			this.excludedMethodsByClass.put(removedMethodOwner, method);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				ClassNode methodOwner = method.getMethodOwner();
				super.logWriter
					.println(
						"\tExcluding all Integer constants in method \""
							+ ExcludeStatementManager.formatMethodForDisplay(method, this)
							+ "\" in class \""
							+ this.formatClassForDisplay(methodOwner)
							+ "\" from Integer Constant Encryption because of \""
							+ reason
							+ "\""
					);
			}
		}
	}

	public final void excludeFieldConstants(FieldNode field, String reason) {
		if (field.isIntLikeType()) {
			ClassNode removedFieldOwner = super.processableFields.remove(field);
			if (removedFieldOwner != null) {
				super.excludedFields.put(field, removedFieldOwner);
				this.excludedFieldsByClass.put(removedFieldOwner, field);
				if (super.scriptContext.isVerbose() && super.logWriter != null) {
					ClassNode fieldOwner = field.getOwnerClass();
					super.logWriter
						.println(
							"\tExcluding initialization value of field \""
								+ ExcludeStatementManager.formatFieldForDisplay(field, this)
								+ "\" in class \""
								+ this.formatClassForDisplay(fieldOwner)
								+ "\" from Integer Constant Encryption because of \""
								+ reason
								+ "\""
						);
				}
			}
		}
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
			super.logWriter.println("\tInteger Constant Encryption Exclusion parameters:");

			for (RenameFilterParameter verboseExclusionParameter : distinctExclusionParameters) {
				super.logWriter.println("\t\t\"" + verboseExclusionParameter + "\"");
			}
		}

		for (RenameFilterParameter exclusionParameterToApply : distinctExclusionParameters) {
			if (!exclusionParameterToApply.isClassTarget() && !exclusionParameterToApply.isFieldTarget() && !exclusionParameterToApply.isMethodTarget()) {
				super.scriptContext
					.logWarningInternal(
						"Parameter '" + exclusionParameterToApply + "' in 'encryptIntegerConstantsExclude' statement is not a class, field or method exclusion parameter. It will be ignored.", true
					);
			} else if (exclusionParameterToApply.isClassTarget() && exclusionParameterToApply.isLinkClass()) {
				super.scriptContext
					.logWarningInternal("Parameter '" + exclusionParameterToApply + "' in 'encryptIntegerConstantsExclude' statement cannot use the '<link>' syntax. It will be ignored.", true);
			} else if (exclusionParameterToApply.isFieldTarget() && exclusionParameterToApply.isClassSpecifierRecursive()) {
				super.scriptContext
					.logWarningInternal("Parameter '" + exclusionParameterToApply + "' in 'encryptIntegerConstantsExclude' statement cannot use the '^' syntax. It will be ignored.", true);
			} else {
				if (exclusionParameterToApply.isClassTarget() && exclusionParameterToApply.isClassWildcard()) {
					super.scriptContext
						.logWarningInternal("Parameter '" + exclusionParameterToApply + "' in 'encryptIntegerConstantsExclude' cannot use the '+' tag. The tag will be ignored.", true);
				}

				exclusionParameterToApply.excludeIntegerEncryption(this);
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
					"A 'encryptIntegerConstantsUnexclude' statement can only operate on the results of a 'encryptIntegerConstantsExclude' statement. No 'encryptIntegerConstantsExclude' statement is in effect so the 'encryptIntegerConstantsUnexclude' statement will be ignored.",
					true
				);
		} else {
			Collections.sort(distinctInclusionParameters, new ExcludeSorter());
			if (super.scriptContext.isVerbose() && distinctInclusionParameters.size() > 0) {
				super.logWriter.println("\tInteger Constant Encryption Unexclusion parameters:");

				for (RenameFilterParameter verboseInclusionParameter : distinctInclusionParameters) {
					super.logWriter.println("\t\t\"" + verboseInclusionParameter + "\"");
				}
			}

			for (RenameFilterParameter inclusionParameterToApply : distinctInclusionParameters) {
				if (!inclusionParameterToApply.isClassTarget() && !inclusionParameterToApply.isFieldTarget() && !inclusionParameterToApply.isMethodTarget()) {
					super.scriptContext
						.logWarningInternal(
							"Parameter '" + inclusionParameterToApply + "' in 'encryptIntegerConstantsUnexclude' statement is not a class, field or method exclusion parameter. It will be ignored.",
							true
						);
				} else if (inclusionParameterToApply.isClassTarget() && inclusionParameterToApply.isLinkClass()) {
					super.scriptContext
						.logWarningInternal("Parameter '" + inclusionParameterToApply + "' in 'encryptIntegerConstantsUnexclude' statement cannot use the '<link>' syntax. It will be ignored.", true);
				} else if (inclusionParameterToApply.isFieldTarget() && inclusionParameterToApply.isClassSpecifierRecursive()) {
					super.scriptContext
						.logWarningInternal("Parameter '" + inclusionParameterToApply + "' in 'encryptIntegerConstantsUnexclude' statement cannot use the '^' syntax. It will be ignored.", true);
				} else {
					if (inclusionParameterToApply.isClassTarget() && inclusionParameterToApply.isClassWildcard()) {
						super.scriptContext
							.logWarningInternal("Parameter '" + inclusionParameterToApply + "' in 'encryptIntegerConstantsUnexclude' cannot use the '+' tag. The tag will be ignored.", true);
					}

					inclusionParameterToApply.includeIntegerEncryption(this);
				}
			}
		}
	}

	@Override
	public final boolean excludeClass(ClassNode classNode, String reason) {
		Object removedProcessableClass = super.processableClasses.remove(classNode);
		if (removedProcessableClass != null) {
			super.excludedClasses.put(classNode, classNode);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				super.logWriter.println("\tExcluding from Integer Constant Encryption class \"" + this.formatClassForDisplay(classNode) + "\" because of \"" + reason + "\"");
			}
		}

		ArrayEnumeration<FieldNode> fieldEnumeration = classNode.getFieldEnumeration();

		while (fieldEnumeration.hasMoreElements()) {
			FieldNode fieldNode = fieldEnumeration.nextElement();
			if (fieldNode.isIntLikeType()) {
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

		return removedProcessableClass != null;
	}

	public final void includeFieldConstants(FieldNode field, String reason) {
		if (field.isIntLikeType()) {
			ClassNode fieldOwner = field.getOwnerClass();
			if (super.excludedClasses.containsKey(fieldOwner)) {
				super.scriptContext
					.logWarning(
						"Cannot UNexclude initialization value of field \""
							+ ExcludeStatementManager.formatFieldForDisplay(field, this)
							+ "\" in class \""
							+ this.formatClassForDisplay(fieldOwner)
							+ "\" because class is excluded from all Integer encryption."
					);
			} else {
				ClassNode removedFieldOwner = super.excludedFields.remove(field);
				if (removedFieldOwner != null) {
					super.processableFields.put(field, removedFieldOwner);
					this.excludedFieldsByClass.remove(field.getOwnerClass(), field);
					if (super.scriptContext.isVerbose() && super.logWriter != null) {
						super.logWriter
							.println(
								"\tUNEXCLUDING initialization value of field \""
									+ ExcludeStatementManager.formatFieldForDisplay(field, this)
									+ "\" in class \""
									+ this.formatClassForDisplay(fieldOwner)
									+ "\" from Integer Constant Encryption because of \""
									+ reason
									+ "\""
							);
					}
				}
			}
		}
	}

	@Override
	public final void includeClass(ClassNode classNode, String reason) {
		Object removedExcludedClass = super.excludedClasses.remove(classNode);
		if (removedExcludedClass != null) {
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				super.logWriter.println("\tUNEXCLUDING from Integer Constant Encryption class \"" + this.formatClassForDisplay(classNode) + "\" because of \"" + reason + "\"");
			}

			super.processableClasses.put(classNode, classNode);
		}

		List<FieldNode> removedExcludedFields = this.excludedFieldsByClass.removeAll(classNode);
		if (removedExcludedClass == null && removedExcludedFields != null && super.scriptContext.isVerbose() && super.logWriter != null) {
			super.logWriter
				.println(
					"\tUNEXCLUDING from Integer Constant Encryption initialization values of all fields of class \""
						+ this.formatClassForDisplay(classNode)
						+ "\" because of \""
						+ reason
						+ "\""
				);
		}

		ArrayEnumeration<FieldNode> fieldEnumeration = classNode.getFieldEnumeration();

		while (fieldEnumeration.hasMoreElements()) {
			FieldNode fieldNode = fieldEnumeration.nextElement();
			if (fieldNode.isIntLikeType() && super.excludedFields.remove(fieldNode) != null) {
				super.processableFields.put(fieldNode, classNode);
			}
		}

		List<MethodNode> removedExcludedMethods = this.excludedMethodsByClass.removeAll(classNode);
		if (removedExcludedClass == null && removedExcludedMethods != null && super.scriptContext.isVerbose() && super.logWriter != null) {
			super.logWriter
				.println(
					"\tUNEXCLUDING from Integer Constant Encryption all Integer constants referenced by methods of class \""
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

	public boolean hasClassWithExcludedMethods(ClassNode classNode) {
		return this.excludedMethodsByClass.containsKey(classNode);
	}

	public boolean hasClassWithExcludedFields(ClassNode classNode) {
		return this.excludedFieldsByClass.containsKey(classNode);
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

	public final void includeMethodConstants(MethodNode method, String reason) {
		ClassNode methodOwner = method.getMethodOwner();
		if (super.excludedClasses.containsKey(methodOwner)) {
			super.scriptContext
				.logWarning(
					"Cannot UNexclude Integer constants in method \""
						+ ExcludeStatementManager.formatMethodForDisplay(method, this)
						+ "\" in class \""
						+ this.formatClassForDisplay(methodOwner)
						+ "\" because class is excluded from all Integer Constant Encryption."
				);
		} else {
			ClassNode removedMethodOwner = this.excludedMethods.remove(method);
			if (removedMethodOwner != null) {
				this.processableMethods.put(method, removedMethodOwner);
				this.excludedMethodsByClass.remove(method.getMethodOwner(), method);
				if (super.scriptContext.isVerbose() && super.logWriter != null) {
					super.logWriter
						.println(
							"\tUNEXCLUDING all Integer constants in method \""
								+ ExcludeStatementManager.formatMethodForDisplay(method, this)
								+ "\" in class \""
								+ this.formatClassForDisplay(methodOwner)
								+ "\" from Integer Constant Encryption because of \""
								+ reason
								+ "\""
						);
				}
			}
		}
	}
}
