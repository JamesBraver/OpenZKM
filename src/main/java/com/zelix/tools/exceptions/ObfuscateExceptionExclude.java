package com.zelix.tools.exceptions;

import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.exclusion.ObfuscateExceptionsExcludeStatement;
import com.zelix.klassmaster.script.statements.exclusion.ObfuscateExceptionsUnexcludeStatement;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import com.zelix.tools.excludes.ExcludeStatementManager;
import com.zelix.tools.excludes.InclusionExclusionManager;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.comparators.ExcludeSorter;
import com.zelix.utils.enumerations.ArrayEnumeration;
import com.zelix.workspace.Workspace;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class ObfuscateExceptionExclude extends InclusionExclusionManager {
	private ChangeLogContext changeLogContext;

	public ObfuscateExceptionExclude(
		Workspace workspace, List<ObfuscateExceptionsExcludeStatement> exclusionStatements, List<ObfuscateExceptionsUnexcludeStatement> inclusionStatements, ChangeLogContext changeLogContext, ScriptContext scriptContext
	) {
		super(workspace, exclusionStatements, inclusionStatements, scriptContext);
		this.changeLogContext = changeLogContext;
		if (workspace.hasClasses()) {
			this.initializeProcessableMaps(workspace.getClasses(), workspace.getWorkspaceClassesSize());
			this.applyScriptRules();
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
			super.logWriter.println("\tObfuscate Exception Exclusion parameters:");

			for (RenameFilterParameter verboseExclusionParameter : distinctExclusionParameters) {
				super.logWriter.println("\t\t\"" + verboseExclusionParameter + "\"");
			}
		}

		for (RenameFilterParameter distinctExclusionParameter : distinctExclusionParameters) {
			if (!distinctExclusionParameter.isMethodTarget() && !distinctExclusionParameter.isClassTarget()) {
				super.scriptContext
					.logWarningInternal(
						"Parameter '" + distinctExclusionParameter + "' in 'obfuscateExceptionsExclude' statement is not a class or method exclusion parameter. It will be ignored.", true
					);
			} else if (distinctExclusionParameter.isClassTarget() && distinctExclusionParameter.isLinkClass()) {
				super.scriptContext
					.logWarningInternal("Parameter '" + distinctExclusionParameter + "' in 'obfuscateExceptionsExclude' statement cannot use the '<link>' syntax. It will be ignored.", true);
			} else if (distinctExclusionParameter.isMethodTarget() && distinctExclusionParameter.hasLinkMethodNamePattern()) {
				super.scriptContext
					.logWarningInternal("Parameter '" + distinctExclusionParameter + "' in 'obfuscateExceptionsExclude' statement cannot use the '<link>' syntax. It will be ignored.", true);
			} else if (distinctExclusionParameter.isMethodTarget() && distinctExclusionParameter.isClassSpecifierRecursive()) {
				super.scriptContext
					.logWarningInternal("Parameter '" + distinctExclusionParameter + "' in 'obfuscateExceptionsExclude' statement cannot use the '^' syntax. It will be ignored.", true);
			} else {
				if (distinctExclusionParameter.isMethodTarget() && distinctExclusionParameter.hasIncludeSignatureClasses()) {
					super.scriptContext
						.logWarningInternal("Parameter '" + distinctExclusionParameter + "' in 'obfuscateExceptionsExclude' cannot use the '+signatureClasses' keyword. It will be ignored.", true);
				} else if (distinctExclusionParameter.isClassTarget() && distinctExclusionParameter.isClassWildcard()) {
					super.scriptContext.logWarningInternal("Parameter '" + distinctExclusionParameter + "' in 'obfuscateExceptionsExclude' cannot use the '+' tag. The tag will be ignored.", true);
				}

				distinctExclusionParameter.excludeExceptionObfuscation(this);
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
					"An 'obfuscateUnexceptionsExclude' statement can only operate on the results of an 'obfuscateExceptionsExclude' statement. No 'obfuscateExceptionsExclude' statement is in effect so the 'obfuscateExceptionsUnexclude' statement will be ignored.",
					true
				);
		} else {
			Collections.sort(distinctInclusionParameters, new ExcludeSorter());
			if (super.scriptContext.isVerbose() && distinctInclusionParameters.size() > 0) {
				super.logWriter.println("\tObfuscate Exception Unexclusion parameters:");

				for (RenameFilterParameter verboseInclusionParameter : distinctInclusionParameters) {
					super.logWriter.println("\t\t\"" + verboseInclusionParameter + "\"");
				}
			}

			for (RenameFilterParameter distinctInclusionParameter : distinctInclusionParameters) {
				if (!distinctInclusionParameter.isMethodTarget() && !distinctInclusionParameter.isClassTarget()) {
					super.scriptContext
						.logWarningInternal(
							"Parameter '" + distinctInclusionParameter + "' in 'obfuscateExceptionsUnexclude' statement is not a class or method exclusion parameter. It will be ignored.", true
						);
				} else if (distinctInclusionParameter.isClassTarget() && distinctInclusionParameter.isLinkClass()) {
					super.scriptContext
						.logWarningInternal("Parameter '" + distinctInclusionParameter + "' in 'obfuscateExceptionsUnexclude' statement cannot use the '<link>' syntax. It will be ignored.", true);
				} else if (distinctInclusionParameter.isMethodTarget() && distinctInclusionParameter.hasLinkMethodNamePattern()) {
					super.scriptContext
						.logWarningInternal("Parameter '" + distinctInclusionParameter + "' in 'obfuscateExceptionsUnexclude' statement cannot use the '<link>' syntax. It will be ignored.", true);
				} else if (distinctInclusionParameter.isMethodTarget() && distinctInclusionParameter.isClassSpecifierRecursive()) {
					super.scriptContext
						.logWarningInternal("Parameter '" + distinctInclusionParameter + "' in 'obfuscateExceptionsUnexclude' statement cannot use the '^' syntax. It will be ignored.", true);
				} else {
					if (distinctInclusionParameter.isMethodTarget() && distinctInclusionParameter.hasIncludeSignatureClasses()) {
						super.scriptContext
							.logWarningInternal("Parameter '" + distinctInclusionParameter + "' in 'obfuscateExceptionsUnexclude' cannot use the '+signatureClasses' keyword. It will be ignored.", true);
					} else if (distinctInclusionParameter.isClassTarget() && distinctInclusionParameter.isClassWildcard()) {
						super.scriptContext
							.logWarningInternal("Parameter '" + distinctInclusionParameter + "' in 'obfuscateExceptionsUnexclude' cannot use the '+' tag. The tag will be ignored.", true);
					}

					distinctInclusionParameter.includeExceptionObfuscation(this);
				}
			}
		}
	}

	@Override
	public final boolean excludeClass(ClassNode classNode, String reason) {
		return this.excludeClassAndMethods(classNode, reason, false);
	}

	@Override
	public final void includeClass(ClassNode classNode, String reason) {
		if (super.excludedClasses.remove(classNode) != null) {
			super.processableClasses.put(classNode, classNode);
			if (super.scriptContext.isVerbose()) {
				super.logWriter.println("\tUNEXCLUDING from exception obfuscation class \"" + this.formatClassForDisplay(classNode) + "\" because of \"" + reason + "\"");
			}
		}

		ArrayEnumeration<MethodNode> methodEnumeration = classNode.getMethodEnumeration();

		while (methodEnumeration.hasMoreElements()) {
			MethodNode methodNode = methodEnumeration.nextElement();
			ClassNode removedMethodOwner = this.excludedMethods.remove(methodNode);
			if (removedMethodOwner != null) {
				this.processableMethods.put(methodNode, removedMethodOwner);
			}
		}
	}

	public final void initializeProcessableMaps(Enumeration<ClassNode> classes, int classCount) {
		super.processableClasses = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount));
		super.excludedClasses = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount));
		this.processableMethods = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));
		this.excludedMethods = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(classCount * 5));

		while (classes.hasMoreElements()) {
			ClassNode classNode = classes.nextElement();
			super.processableClasses.put(classNode, classNode);
			ArrayEnumeration<MethodNode> methodEnumeration = classNode.getMethodEnumeration();

			while (methodEnumeration.hasMoreElements()) {
				MethodNode methodNode = methodEnumeration.nextElement();
				this.processableMethods.put(methodNode, methodNode.getMethodOwner());
			}
		}
	}

	public final void includeMethod(MethodNode methodNode, String reason) {
		ClassNode removedMethodOwner = this.excludedMethods.remove(methodNode);
		if (removedMethodOwner != null) {
			this.processableMethods.put(methodNode, removedMethodOwner);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				ClassNode methodOwner = methodNode.getMethodOwner();
				super.logWriter
					.println(
						"\tUNEXCLUDING from exception obfuscation method \""
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

	public final boolean excludeClassAndMethods(ClassNode classNode, String reason, boolean alsoExcludeExceptions) {
		Object removedProcessableClass = super.processableClasses.remove(classNode);
		if (removedProcessableClass != null) {
			super.excludedClasses.put(classNode, classNode);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				String exclusionMessage = this.formatClassForDisplay(classNode) + "\" because of \"" + reason + "\"";
				super.logWriter.println("\tExcluding from exception obfuscation class \"" + exclusionMessage);
				if (alsoExcludeExceptions || Options.ZKM_TREAT_EXCEPTION_OBFUSCATION_AS_FLOW_OBFUSCATION) {
					super.logWriter.println("\tExcluding from exception obfuscation class \"" + exclusionMessage);
				}
			}
		}

		ArrayEnumeration<MethodNode> methodEnumeration = classNode.getMethodEnumeration();

		while (methodEnumeration.hasMoreElements()) {
			MethodNode methodNode = methodEnumeration.nextElement();
			ClassNode removedMethodOwner = this.processableMethods.remove(methodNode);
			if (removedMethodOwner != null) {
				this.excludedMethods.put(methodNode, removedMethodOwner);
			}
		}

		return removedProcessableClass != null;
	}

	public final void excludeMethod(MethodNode methodNode, String reason) {
		ClassNode removedMethodOwner = this.processableMethods.remove(methodNode);
		if (removedMethodOwner != null) {
			this.excludedMethods.put(methodNode, removedMethodOwner);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				ClassNode methodOwner = methodNode.getMethodOwner();
				String exclusionMessage = ExcludeStatementManager.formatMethodForDisplay(methodNode, this)
					+ "\" in class \""
					+ this.formatClassForDisplay(methodOwner)
					+ "\" because of \""
					+ reason
					+ "\"";
				super.logWriter.println("\tExcluding from exception obfuscation method \"" + exclusionMessage);
			}
		}
	}
}
