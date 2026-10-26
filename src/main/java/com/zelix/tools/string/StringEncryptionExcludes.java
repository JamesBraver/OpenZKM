package com.zelix.tools.string;

import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.exclusion.StringEncryptionExcludeStatement;
import com.zelix.klassmaster.script.statements.exclusion.StringEncryptionUnexcludeStatement;
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

public class StringEncryptionExcludes extends InclusionExclusionManager {
	private MultiMap<ClassNode, FieldNode> excludedFieldsByClass = new MultiMap<>();
	private MultiMap<ClassNode, MethodNode> excludedMethodsByClass = new MultiMap<>();

	public StringEncryptionExcludes(
		Workspace workspace, List<StringEncryptionExcludeStatement> excludeStatements, List<StringEncryptionUnexcludeStatement> unexcludeStatements, ScriptContext scriptContext
	) {
		super(workspace, excludeStatements, unexcludeStatements, scriptContext);
		if (workspace.hasClasses()) {
			this.initializeTargets(workspace.getClasses(), workspace.getWorkspaceClassesSize());
			this.applyScriptRules();
		}
	}

	public final void includeFieldStrings(FieldNode fieldNode, String reason) {
		if (fieldNode.isString()) {
			ClassNode ownerClass = fieldNode.getOwnerClass();
			if (super.excludedClasses.containsKey(ownerClass)) {
				super.scriptContext
					.logWarning(
						"Cannot UNexclude initialization String of field \""
							+ ExcludeStatementManager.formatFieldForDisplay(fieldNode, this)
							+ "\" in class \""
							+ this.formatClassForDisplay(ownerClass)
							+ "\" because class is excluded from all String encryption."
					);
			} else {
				ClassNode removedOwnerClass = super.excludedFields.remove(fieldNode);
				if (removedOwnerClass != null) {
					super.processableFields.put(fieldNode, removedOwnerClass);
					this.excludedFieldsByClass.remove(fieldNode.getOwnerClass(), fieldNode);
					if (super.scriptContext.isVerbose() && super.logWriter != null) {
						super.logWriter
							.println(
								"\tUNEXCLUDING initialization String of field \""
									+ ExcludeStatementManager.formatFieldForDisplay(fieldNode, this)
									+ "\" in class \""
									+ this.formatClassForDisplay(ownerClass)
									+ "\" from String Encryption because of \""
									+ reason
									+ "\""
							);
					}
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

	public Enumeration<MethodNode> getExcludedMethodsForClass(ClassNode classNode) {
		List<MethodNode> excludedMethods = this.excludedMethodsByClass.get(classNode);
		return (Enumeration<MethodNode>)(excludedMethods != null ? Collections.enumeration(excludedMethods) : new EmptyEnumeration<>());
	}

	@Override
	public final boolean excludeClass(ClassNode classNode, String reason) {
		Object removedClass = super.processableClasses.remove(classNode);
		if (removedClass != null) {
			super.excludedClasses.put(classNode, classNode);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				super.logWriter.println("\tExcluding from String Encryption class \"" + this.formatClassForDisplay(classNode) + "\" because of \"" + reason + "\"");
			}
		}

		ArrayEnumeration<FieldNode> fieldEnumeration = classNode.getFieldEnumeration();

		while (fieldEnumeration.hasMoreElements()) {
			FieldNode fieldNode = fieldEnumeration.nextElement();
			this.excludedFieldsByClass.put(classNode, fieldNode);
			if (super.processableFields.remove(fieldNode) != null) {
				super.excludedFields.put(fieldNode, classNode);
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

	@Override
	public final void includeClass(ClassNode classNode, String reason) {
		Object removedClass = super.excludedClasses.remove(classNode);
		if (removedClass != null) {
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				super.logWriter.println("\tUNEXCLUDING from String Encryption class \"" + this.formatClassForDisplay(classNode) + "\" because of \"" + reason + "\"");
			}

			super.processableClasses.put(classNode, classNode);
		}

		List<FieldNode> removedExcludedFields = this.excludedFieldsByClass.removeAll(classNode);
		if (removedClass == null && removedExcludedFields != null && super.scriptContext.isVerbose() && super.logWriter != null) {
			super.logWriter
				.println(
					"\tUNEXCLUDING from String Encryption initialization Strings of all fields of class \""
						+ this.formatClassForDisplay(classNode)
						+ "\" because of \""
						+ reason
						+ "\""
				);
		}

		ArrayEnumeration<FieldNode> fieldEnumeration = classNode.getFieldEnumeration();

		while (fieldEnumeration.hasMoreElements()) {
			FieldNode fieldNode = fieldEnumeration.nextElement();
			if (super.excludedFields.remove(fieldNode) != null) {
				super.processableFields.put(fieldNode, classNode);
			}
		}

		List<MethodNode> removedExcludedMethods = this.excludedMethodsByClass.removeAll(classNode);
		if (removedClass == null && removedExcludedMethods != null && super.scriptContext.isVerbose() && super.logWriter != null) {
			super.logWriter
				.println(
					"\tUNEXCLUDING from String Encryption all Strings referenced by methods of class \"" + this.formatClassForDisplay(classNode) + "\" because of \"" + reason + "\""
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

	public boolean hasClassWithExcludedFields(ClassNode classNode) {
		return this.excludedFieldsByClass.containsKey(classNode);
	}

	public Enumeration<FieldNode> getExcludedFieldsForClass(ClassNode classNode) {
		List<FieldNode> excludedFields = this.excludedFieldsByClass.get(classNode);
		return (Enumeration<FieldNode>)(excludedFields != null ? Collections.enumeration(excludedFields) : new EmptyEnumeration<>());
	}

	public final void excludeMethodStrings(MethodNode methodNode, String reason) {
		ClassNode removedOwnerClass = this.processableMethods.remove(methodNode);
		if (removedOwnerClass != null) {
			this.excludedMethods.put(methodNode, removedOwnerClass);
			this.excludedMethodsByClass.put(removedOwnerClass, methodNode);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				ClassNode ownerClass = methodNode.getMethodOwner();
				super.logWriter
					.println(
						"\tExcluding all String constants in method \""
							+ ExcludeStatementManager.formatMethodForDisplay(methodNode, this)
							+ "\" in class \""
							+ this.formatClassForDisplay(ownerClass)
							+ "\" from String Encryption because of \""
							+ reason
							+ "\""
					);
			}
		}
	}

	public boolean hasClassWithExcludedMethods(ClassNode classNode) {
		return this.excludedMethodsByClass.containsKey(classNode);
	}

	private final void applyScriptRules() {
		int exclusionStatementCount;
		if (super.exclusionStatements == null) {
			exclusionStatementCount = 0;
		} else {
			exclusionStatementCount = super.exclusionStatements.size();
		}

		HashMap<RenameFilterParameter, RenameFilterParameter> seenExclusionParams = GlobalUtils.newHashMap();
		Vector<RenameFilterParameter> distinctExclusionParams = new Vector<>();

		for (int exclusionIndex = 0; exclusionIndex < exclusionStatementCount; exclusionIndex++) {
			Enumeration<RenameFilterParameter> exclusionParamEnumeration = super.exclusionStatements.get(exclusionIndex).getRenameFilterParameters();

			while (exclusionParamEnumeration.hasMoreElements()) {
				RenameFilterParameter exclusionFilterParam = exclusionParamEnumeration.nextElement();
				if (!seenExclusionParams.containsKey(exclusionFilterParam)) {
					seenExclusionParams.put(exclusionFilterParam, exclusionFilterParam);
					distinctExclusionParams.addElement(exclusionFilterParam);
				}
			}
		}

		Collections.sort(distinctExclusionParams, new ExcludeSorter());
		if (super.scriptContext.isVerbose() && distinctExclusionParams.size() > 0) {
			super.logWriter.println("\tString Encryption Exclusion parameters:");

			for (RenameFilterParameter loggedExclusionParam : distinctExclusionParams) {
				super.logWriter.println("\t\t\"" + loggedExclusionParam + "\"");
			}
		}

		for (RenameFilterParameter exclusionParam : distinctExclusionParams) {
			if (!exclusionParam.isClassTarget() && !exclusionParam.isFieldTarget() && !exclusionParam.isMethodTarget()) {
				super.scriptContext
					.logWarningInternal(
						"Parameter '" + exclusionParam + "' in 'stringEncryptionExclude' statement is not a class, field or method exclusion parameter. It will be ignored.", true
					);
			} else if (exclusionParam.isClassTarget() && exclusionParam.isLinkClass()) {
				super.scriptContext
					.logWarningInternal("Parameter '" + exclusionParam + "' in 'stringEncryptionExclude' statement cannot use the '<link>' syntax. It will be ignored.", true);
			} else if (exclusionParam.isFieldTarget() && exclusionParam.isClassSpecifierRecursive()) {
				super.scriptContext
					.logWarningInternal("Parameter '" + exclusionParam + "' in 'stringEncryptionExclude' statement cannot use the '^' syntax. It will be ignored.", true);
			} else {
				if (exclusionParam.isClassTarget() && exclusionParam.isClassWildcard()) {
					super.scriptContext.logWarningInternal("Parameter '" + exclusionParam + "' in 'stringEncryptionExclude' cannot use the '+' tag. The tag will be ignored.", true);
				}

				exclusionParam.excludeStringEncryption(this);
			}
		}

		int inclusionStatementCount;
		if (super.inclusionStatements == null) {
			inclusionStatementCount = 0;
		} else {
			inclusionStatementCount = super.inclusionStatements.size();
		}

		HashMap<RenameFilterParameter, RenameFilterParameter> seenInclusionParams = GlobalUtils.newHashMap();
		Vector<RenameFilterParameter> distinctInclusionParams = new Vector<>();

		for (int inclusionIndex = 0; inclusionIndex < inclusionStatementCount; inclusionIndex++) {
			Enumeration<RenameFilterParameter> inclusionParamEnumeration = super.inclusionStatements.get(inclusionIndex).getRenameFilterParameters();

			while (inclusionParamEnumeration.hasMoreElements()) {
				RenameFilterParameter inclusionFilterParam = inclusionParamEnumeration.nextElement();
				if (!seenInclusionParams.containsKey(inclusionFilterParam)) {
					seenInclusionParams.put(inclusionFilterParam, inclusionFilterParam);
					distinctInclusionParams.addElement(inclusionFilterParam);
				}
			}
		}

		if (distinctInclusionParams.size() > 0 && distinctExclusionParams.size() == 0) {
			super.scriptContext
				.logWarningInternal(
					"A 'stringEncryptionUnexclude' statement can only operate on the results of a 'stringEncryptionExclude' statement. No 'stringEncryptionExclude' statement is in effect so the 'stringEncryptionUnexclude' statement will be ignored.",
					true
				);
		} else {
			Collections.sort(distinctInclusionParams, new ExcludeSorter());
			if (super.scriptContext.isVerbose() && distinctInclusionParams.size() > 0) {
				super.logWriter.println("\tString Encryption Unexclusion parameters:");

				for (RenameFilterParameter loggedInclusionParam : distinctInclusionParams) {
					super.logWriter.println("\t\t\"" + loggedInclusionParam + "\"");
				}
			}

			for (RenameFilterParameter inclusionParam : distinctInclusionParams) {
				if (!inclusionParam.isClassTarget() && !inclusionParam.isFieldTarget() && !inclusionParam.isMethodTarget()) {
					super.scriptContext
						.logWarningInternal(
							"Parameter '" + inclusionParam + "' in 'stringEncryptionUnexclude' statement is not a class, field or method exclusion parameter. It will be ignored.", true
						);
				} else if (inclusionParam.isClassTarget() && inclusionParam.isLinkClass()) {
					super.scriptContext
						.logWarningInternal("Parameter '" + inclusionParam + "' in 'stringEncryptionUnexclude' statement cannot use the '<link>' syntax. It will be ignored.", true);
				} else if (inclusionParam.isFieldTarget() && inclusionParam.isClassSpecifierRecursive()) {
					super.scriptContext
						.logWarningInternal("Parameter '" + inclusionParam + "' in 'stringEncryptionUnexclude' statement cannot use the '^' syntax. It will be ignored.", true);
				} else {
					if (inclusionParam.isClassTarget() && inclusionParam.isClassWildcard()) {
						super.scriptContext.logWarningInternal("Parameter '" + inclusionParam + "' in 'stringEncryptionUnexclude' cannot use the '+' tag. The tag will be ignored.", true);
					}

					inclusionParam.includeStringEncryption(this);
				}
			}
		}
	}

	public final void excludeFieldStrings(FieldNode fieldNode, String reason) {
		if (fieldNode.isString()) {
			ClassNode removedOwnerClass = super.processableFields.remove(fieldNode);
			if (removedOwnerClass != null) {
				super.excludedFields.put(fieldNode, removedOwnerClass);
				this.excludedFieldsByClass.put(removedOwnerClass, fieldNode);
				if (super.scriptContext.isVerbose() && super.logWriter != null) {
					ClassNode ownerClass = fieldNode.getOwnerClass();
					super.logWriter
						.println(
							"\tExcluding initialization String of field \""
								+ ExcludeStatementManager.formatFieldForDisplay(fieldNode, this)
								+ "\" in class \""
								+ this.formatClassForDisplay(ownerClass)
								+ "\" from String Encryption because of \""
								+ reason
								+ "\""
						);
				}
			}
		}
	}

	public final void includeMethodStrings(MethodNode methodNode, String reason) {
		ClassNode ownerClass = methodNode.getMethodOwner();
		if (super.excludedClasses.containsKey(ownerClass)) {
			super.scriptContext
				.logWarning(
					"Cannot UNexclude String constants in method \""
						+ ExcludeStatementManager.formatMethodForDisplay(methodNode, this)
						+ "\" in class \""
						+ this.formatClassForDisplay(ownerClass)
						+ "\" because class is excluded from all String encryption."
				);
		} else {
			ClassNode removedOwnerClass = this.excludedMethods.remove(methodNode);
			if (removedOwnerClass != null) {
				this.processableMethods.put(methodNode, removedOwnerClass);
				this.excludedMethodsByClass.remove(methodNode.getMethodOwner(), methodNode);
				if (super.scriptContext.isVerbose() && super.logWriter != null) {
					super.logWriter
						.println(
							"\tUNEXCLUDING all String constants in method \""
								+ ExcludeStatementManager.formatMethodForDisplay(methodNode, this)
								+ "\" in class \""
								+ this.formatClassForDisplay(ownerClass)
								+ "\" from String Encryption because of \""
								+ reason
								+ "\""
						);
				}
			}
		}
	}
}
