package com.zelix.tools.autoReflection;

import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.FieldNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.exclusion.AccessedByReflectionExcludeStatement;
import com.zelix.klassmaster.script.statements.exclusion.AccessedByReflectionStatement;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import com.zelix.tools.excludes.ExcludeStatementManager;
import com.zelix.tools.excludes.InclusionExclusionManager;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.enumerations.ArrayEnumeration;
import com.zelix.workspace.Workspace;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class AutoReflectionExcludes extends InclusionExclusionManager {
	public AutoReflectionExcludes(
		Workspace workspace, List<AccessedByReflectionStatement> reflectionStatements, List<AccessedByReflectionExcludeStatement> reflectionExcludeStatements, boolean verbose, ScriptContext scriptContext
	) {
		super(workspace, reflectionStatements, reflectionExcludeStatements, scriptContext);
		if (workspace.hasClasses()) {
			if (reflectionStatements != null && reflectionStatements.size() != 0) {
				this.initializeProcessableMaps(workspace.getClasses(), workspace.getWorkspaceClassesSize());
			} else {
				this.initializeExcludedMaps(workspace.getClasses(), workspace.getWorkspaceClassesSize());
			}

			this.applyScriptRules();
		}
	}

	public final void includeMethodBecause(MethodNode methodNode, String reason) {
		ClassNode removedMethodOwner = this.excludedMethods.remove(methodNode);
		if (removedMethodOwner != null) {
			this.processableMethods.put(methodNode, removedMethodOwner);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				ClassNode methodOwner = methodNode.getMethodOwner();
				super.logWriter
					.println(
						"\tMatched as NOT accessed by Reflection method \""
							+ ExcludeStatementManager.formatMethodForDisplay(methodNode, this)
							+ "\" in class \""
							+ this.formatClassForDisplay(methodOwner)
							+ "\" because of \""
							+ reason
							+ "\""
					);
			}
		}
	}

	public final void includeFieldBecause(FieldNode fieldNode, String reason) {
		ClassNode fieldOwner = fieldNode.getOwnerClass();
		ClassNode removedFieldOwner = super.excludedFields.remove(fieldNode);
		if (removedFieldOwner != null) {
			super.processableFields.put(fieldNode, removedFieldOwner);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				super.logWriter
					.println(
						"\tMatched as NOT accessed by Reflection field \""
							+ ExcludeStatementManager.formatFieldForDisplay(fieldNode, this)
							+ "\" in class \""
							+ this.formatClassForDisplay(fieldOwner)
							+ "\" because of \""
							+ reason
							+ "\""
					);
			}
		}
	}

	@Override
	public final boolean excludeClass(ClassNode classNode, String reason) {
		Object removedClass = super.processableClasses.remove(classNode);
		if (removedClass != null) {
			super.excludedClasses.put(classNode, classNode);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				super.logWriter.println("\tMatched as accessed by Reflection class \"" + this.formatClassForDisplay(classNode) + "\" because of \"" + reason + "\"");
			}
		}

		return removedClass != null;
	}

	@Override
	public final void includeClass(ClassNode classNode, String reason) {
		if (super.excludedClasses.remove(classNode) != null) {
			super.processableClasses.put(classNode, classNode);
			if (super.scriptContext.isVerbose()) {
				super.logWriter.println("\tMatched as NOT accessed by Reflection class \"" + this.formatClassForDisplay(classNode) + "\" because of \"" + reason + "\"");
			}
		}
	}

	public final void excludeMethodBecause(MethodNode methodNode, String reason) {
		ClassNode removedMethodOwner = this.processableMethods.remove(methodNode);
		if (removedMethodOwner != null) {
			this.excludedMethods.put(methodNode, removedMethodOwner);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				ClassNode methodOwner = methodNode.getMethodOwner();
				super.logWriter
					.println(
						"\tMatched as accessed by Reflection method \""
							+ ExcludeStatementManager.formatMethodForDisplay(methodNode, this)
							+ "\" in class \""
							+ this.formatClassForDisplay(methodOwner)
							+ "\" because of \""
							+ reason
							+ "\""
					);
			}
		}
	}

	private void initializeMapsWithCapacity(int classCount) {
		super.processableClasses = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount));
		super.excludedClasses = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount));
		super.processableFields = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));
		super.excludedFields = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));
		this.processableMethods = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));
		this.excludedMethods = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));
	}

	public final void initializeExcludedMaps(Enumeration<ClassNode> classes, int classCount) {
		this.initializeMapsWithCapacity(classCount);

		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			super.excludedClasses.put(classNode, classNode);
			ArrayEnumeration<FieldNode> fieldEnumeration = classNode.getFieldEnumeration();

			while (fieldEnumeration.hasMoreElements()) {
				FieldNode fieldNode = fieldEnumeration.nextElement();
				super.excludedFields.put(fieldNode, fieldNode.getOwnerClass());
			}

			ArrayEnumeration<MethodNode> methodEnumeration = classNode.getMethodEnumeration();

			while (methodEnumeration.hasMoreElements()) {
				MethodNode methodNode = methodEnumeration.nextElement();
				this.excludedMethods.put(methodNode, methodNode.getMethodOwner());
			}
		}
	}

	public final void excludeFieldBecause(FieldNode fieldNode, String reason) {
		ClassNode removedFieldOwner = super.processableFields.remove(fieldNode);
		if (removedFieldOwner != null) {
			super.excludedFields.put(fieldNode, removedFieldOwner);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				ClassNode fieldOwner = fieldNode.getOwnerClass();
				super.logWriter
					.println(
						"\tMatched as accessed by Reflection field \""
							+ ExcludeStatementManager.formatFieldForDisplay(fieldNode, this)
							+ "\" in class \""
							+ this.formatClassForDisplay(fieldOwner)
							+ "\" because of \""
							+ reason
							+ "\""
					);
			}
		}
	}

	public final void initializeProcessableMaps(Enumeration<ClassNode> classes, int classCount) {
		this.initializeMapsWithCapacity(classCount);

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

	private final void applyScriptRules() {
		int exclusionStatementCount;
		if (super.exclusionStatements == null) {
			exclusionStatementCount = 0;
		} else {
			exclusionStatementCount = super.exclusionStatements.size();
		}

		HashMap<RenameFilterParameter, RenameFilterParameter> seenExclusionFilters = GlobalUtils.newHashMap();
		Vector<RenameFilterParameter> distinctExclusionFilters = new Vector<>();

		for (int exclusionStatementIndex = 0; exclusionStatementIndex < exclusionStatementCount; exclusionStatementIndex++) {
			Enumeration<RenameFilterParameter> exclusionFilterEnumeration = super.exclusionStatements.get(exclusionStatementIndex).getRenameFilterParameters();

			while (exclusionFilterEnumeration.hasMoreElements()) {
				RenameFilterParameter exclusionFilter = exclusionFilterEnumeration.nextElement();
				if (!seenExclusionFilters.containsKey(exclusionFilter)) {
					seenExclusionFilters.put(exclusionFilter, exclusionFilter);
					distinctExclusionFilters.addElement(exclusionFilter);
				}
			}
		}

		if (super.scriptContext.isVerbose() && distinctExclusionFilters.size() > 0) {
			super.logWriter.println("\taccessedByReflection parameters:");

			for (int exclusionLogIndex = distinctExclusionFilters.size() - 1; exclusionLogIndex >= 0; exclusionLogIndex--) {
				super.logWriter.println("\t\t" + distinctExclusionFilters.elementAt(exclusionLogIndex));
			}
		}

		for (int exclusionFilterIndex = distinctExclusionFilters.size() - 1; exclusionFilterIndex >= 0; exclusionFilterIndex--) {
			RenameFilterParameter distinctExclusionFilter = distinctExclusionFilters.elementAt(exclusionFilterIndex);
			if (!distinctExclusionFilter.isClassTarget() && !distinctExclusionFilter.isMethodTarget() && !distinctExclusionFilter.isFieldTarget()) {
				super.scriptContext
					.logWarningInternal(
						"Auto Reflection Handling : Parameter '"
							+ distinctExclusionFilter
							+ "' in 'accessedByReflection' statement is not a class, field or method exclusion parameter. It will be ignored.",
						true
					);
			} else if (distinctExclusionFilter.isClassTarget() && distinctExclusionFilter.isLinkClass()) {
				super.scriptContext
					.logWarningInternal(
						"Auto Reflection Handling : Parameter '" + distinctExclusionFilter + "' in 'accessedByReflection' statement cannot use the '<link>' syntax. It will be ignored.", true
					);
			} else if (distinctExclusionFilter.isMethodTarget() && distinctExclusionFilter.hasLinkMethodNamePattern()) {
				super.scriptContext
					.logWarningInternal(
						"Auto Reflection Handling : Parameter '" + distinctExclusionFilter + "' in 'accessedByReflection' statement cannot use the '<link>' syntax. It will be ignored.", true
					);
			} else if (distinctExclusionFilter.isMethodTarget() && distinctExclusionFilter.hasIncludeSignatureClasses()) {
				super.scriptContext
					.logWarningInternal(
						"Auto Reflection Handling : Parameter '" + distinctExclusionFilter + "' in 'accessedByReflection' cannot use the '+signatureClasses' keyword. It will be ignored.", true
					);
			} else {
				distinctExclusionFilter.excludeAutoReflection(this);
			}
		}

		int inclusionStatementCount;
		if (super.inclusionStatements == null) {
			inclusionStatementCount = 0;
		} else {
			inclusionStatementCount = super.inclusionStatements.size();
		}

		HashMap<RenameFilterParameter, RenameFilterParameter> seenInclusionFilters = GlobalUtils.newHashMap();
		ArrayList<RenameFilterParameter> distinctInclusionFilters = new ArrayList<>();

		for (int inclusionStatementIndex = 0; inclusionStatementIndex < inclusionStatementCount; inclusionStatementIndex++) {
			Enumeration<RenameFilterParameter> inclusionFilterEnumeration = super.inclusionStatements.get(inclusionStatementIndex).getRenameFilterParameters();

			while (inclusionFilterEnumeration.hasMoreElements()) {
				RenameFilterParameter inclusionFilter = inclusionFilterEnumeration.nextElement();
				if (!seenInclusionFilters.containsKey(inclusionFilter)) {
					seenInclusionFilters.put(inclusionFilter, inclusionFilter);
					distinctInclusionFilters.add(inclusionFilter);
				}
			}
		}

		if (super.scriptContext.isVerbose() && distinctInclusionFilters.size() > 0) {
			super.logWriter.println("\taccessedByReflectionExclude parameters:");

			for (int inclusionLogIndex = distinctInclusionFilters.size() - 1; inclusionLogIndex >= 0; inclusionLogIndex--) {
				super.logWriter.println("\t\t\"" + distinctInclusionFilters.get(inclusionLogIndex) + "\"");
			}
		}

		for (int inclusionFilterIndex = distinctInclusionFilters.size() - 1; inclusionFilterIndex >= 0; inclusionFilterIndex--) {
			RenameFilterParameter distinctInclusionFilter = distinctInclusionFilters.get(inclusionFilterIndex);
			if (!distinctInclusionFilter.isClassTarget() && !distinctInclusionFilter.isMethodTarget() && !distinctInclusionFilter.isFieldTarget()) {
				super.scriptContext
					.logWarningInternal(
						"Auto Reflection Handling : Parameter '"
							+ distinctInclusionFilter
							+ "' in 'accessedByReflectionExclude' statement is not a class, field or method exclusion parameter. It will be ignored.",
						true
					);
			} else if (distinctInclusionFilter.isClassTarget() && distinctInclusionFilter.isLinkClass()) {
				super.scriptContext
					.logWarningInternal(
						"Auto Reflection Handling : Parameter '" + distinctInclusionFilter + "' in 'accessedByReflectionExclude' statement cannot use the '<link>' syntax. It will be ignored.",
						true
					);
			} else if (distinctInclusionFilter.isMethodTarget() && distinctInclusionFilter.hasLinkMethodNamePattern()) {
				super.scriptContext
					.logWarningInternal(
						"Auto Reflection Handling : Parameter '" + distinctInclusionFilter + "' in 'accessedByReflectionExclude' statement cannot use the '<link>' syntax. It will be ignored.",
						true
					);
			} else if (distinctInclusionFilter.isMethodTarget() && distinctInclusionFilter.hasIncludeSignatureClasses()) {
				super.scriptContext
					.logWarningInternal(
						"Auto Reflection Handling : Parameter '" + distinctInclusionFilter + "' in 'accessedByReflectionExclude' cannot use the '+signatureClasses' keyword. It will be ignored.",
						true
					);
			} else {
				distinctInclusionFilter.includeAutoReflection(this);
			}
		}
	}
}
