package com.zelix.tools.flow;

import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.exclusion.ObfuscateFlowExcludeStatement;
import com.zelix.klassmaster.script.statements.exclusion.ObfuscateFlowUnexcludeStatement;
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

public class FlowObfuscateExclude extends InclusionExclusionManager {
	private ChangeLogContext changeLogContext;

	public FlowObfuscateExclude(
		Workspace workspace, List<ObfuscateFlowExcludeStatement> exclusionStatements, List<ObfuscateFlowUnexcludeStatement> inclusionStatements, ChangeLogContext changeLogContext, ScriptContext scriptContext
	) {
		super(workspace, exclusionStatements, inclusionStatements, scriptContext);
		this.changeLogContext = changeLogContext;
		if (workspace.hasClasses()) {
			this.initializeProcessableMaps(workspace.getClasses(), workspace.getWorkspaceClassesSize());
			this.applyScriptRules();
		}
	}

	public void includeClassForFlowFields(ClassNode classNode, String reason) {
		if (super.excludedClasses.containsKey(classNode)) {
			ClassNode removedClass = super.excludedClasses.remove(classNode);
			super.processableClasses.put(classNode, classNode);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				super.logWriter
					.println("\tUNEXCLUDING from getting flow obfuscation fields class \"" + this.formatClassForDisplay(classNode) + "\" because of \"" + reason + "\"");
			}
		}
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
				super.logWriter.println("\tExcluding from flow obfuscation method \"" + exclusionMessage);
				if (Options.ZKM_TREAT_EXCEPTION_OBFUSCATION_AS_FLOW_OBFUSCATION) {
					super.logWriter.println("\tExcluding from exception obfuscation method \"" + exclusionMessage);
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
				super.logWriter.println("\tUNEXCLUDING from flow obfuscation class \"" + this.formatClassForDisplay(classNode) + "\" because of \"" + reason + "\"");
			}
		}

		ArrayEnumeration<MethodNode> methods = classNode.getMethodEnumeration();

		while (methods.hasMoreElements()) {
			MethodNode method = methods.nextElement();
			ClassNode previousMethodOwner = this.excludedMethods.remove(method);
			if (previousMethodOwner != null) {
				this.processableMethods.put(method, previousMethodOwner);
			}
		}
	}

	public void excludeClassForFlowFields(ClassNode classNode, String reason) {
		if (super.processableClasses.containsKey(classNode)) {
			if (this.changeLogContext != null && this.changeLogContext.hasFlowObfuscationEntry(classNode.getInternalName())) {
				if (super.logWriter != null) {
					super.scriptContext
						.logWarning(
							"Could NOT exclude class \""
								+ this.formatClassForDisplay(classNode)
								+ "\" from getting flow obfuscation fields because change log \""
								+ this.changeLogContext.getLogFileName()
								+ "\" specifies that it does : \""
								+ reason
								+ "\"."
						);
				}

				return;
			}

			ClassNode removedClass = super.processableClasses.remove(classNode);
			super.excludedClasses.put(classNode, classNode);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				super.logWriter.println("\tExcluding from getting flow obfuscation fields class \"" + this.formatClassForDisplay(classNode) + "\" because of \"" + reason + "\"");
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
						"\tUNEXCLUDING from flow obfuscation method \""
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

	public final boolean excludeClassAndMethods(ClassNode classNode, String reason, boolean alsoExcludeExceptions) {
		Object removedProcessableClass = super.processableClasses.remove(classNode);
		if (removedProcessableClass != null) {
			super.excludedClasses.put(classNode, classNode);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				String exclusionMessage = this.formatClassForDisplay(classNode) + "\" because of \"" + reason + "\"";
				super.logWriter.println("\tExcluding from flow obfuscation class \"" + exclusionMessage);
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
			super.logWriter.println("\tObfuscate Flow Exclusion parameters:");

			for (RenameFilterParameter verboseExclusionParameter : distinctExclusionParameters) {
				super.logWriter.println("\t\t\"" + verboseExclusionParameter + "\"");
			}
		}

		for (RenameFilterParameter distinctExclusionParameter : distinctExclusionParameters) {
			if (distinctExclusionParameter.isMethodTarget() || distinctExclusionParameter.isClassTarget() || distinctExclusionParameter.isFieldTarget() && distinctExclusionParameter.isWildcardField()) {
				if (distinctExclusionParameter.isClassTarget() && distinctExclusionParameter.isLinkClass()) {
					super.scriptContext
						.logWarningInternal("Parameter '" + distinctExclusionParameter + "' in 'obfuscateFlowExclude' statement cannot use the '<link>' syntax. It will be ignored.", true);
				} else if (distinctExclusionParameter.isMethodTarget() && distinctExclusionParameter.hasLinkMethodNamePattern()) {
					super.scriptContext
						.logWarningInternal("Parameter '" + distinctExclusionParameter + "' in 'obfuscateFlowExclude' statement cannot use the '<link>' syntax. It will be ignored.", true);
				} else if (distinctExclusionParameter.isMethodTarget() && distinctExclusionParameter.isClassSpecifierRecursive()) {
					super.scriptContext
						.logWarningInternal("Parameter '" + distinctExclusionParameter + "' in 'obfuscateFlowExclude' statement cannot use the '^' syntax. It will be ignored.", true);
				} else {
					if (distinctExclusionParameter.isMethodTarget() && distinctExclusionParameter.hasIncludeSignatureClasses()) {
						super.scriptContext
							.logWarningInternal("Parameter '" + distinctExclusionParameter + "' in 'obfuscateFlowExclude' cannot use the '+signatureClasses' keyword. It will be ignored.", true);
					} else if (distinctExclusionParameter.isClassTarget() && distinctExclusionParameter.isClassWildcard()) {
						super.scriptContext.logWarningInternal("Parameter '" + distinctExclusionParameter + "' in 'obfuscateFlowExclude' cannot use the '+' tag. The tag will be ignored.", true);
					}

					distinctExclusionParameter.excludeFlowObfuscation(this);
				}
			} else {
				super.scriptContext
					.logWarningInternal(
						"Parameter '" + distinctExclusionParameter + "' in 'obfuscateFlowExclude' statement is not a class or method exclusion parameter. It will be ignored.", true
					);
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
					"An 'obfuscateFlowUnexclude' statement can only operate on the results of an 'obfuscateFlowExclude' statement. No 'obfuscateFlowExclude' statement is in effect so the 'obfuscateFlowUnexclude' statement will be ignored.",
					true
				);
		} else {
			Collections.sort(distinctInclusionParameters, new ExcludeSorter());
			if (super.scriptContext.isVerbose() && distinctInclusionParameters.size() > 0) {
				super.logWriter.println("\tObfuscate Flow Unexclusion parameters:");

				for (RenameFilterParameter verboseInclusionParameter : distinctInclusionParameters) {
					super.logWriter.println("\t\t\"" + verboseInclusionParameter + "\"");
				}
			}

			for (RenameFilterParameter distinctInclusionParameter : distinctInclusionParameters) {
				if (distinctInclusionParameter.isMethodTarget() || distinctInclusionParameter.isClassTarget() || distinctInclusionParameter.isFieldTarget() && distinctInclusionParameter.isWildcardField()) {
					if (distinctInclusionParameter.isClassTarget() && distinctInclusionParameter.isLinkClass()) {
						super.scriptContext
							.logWarningInternal("Parameter '" + distinctInclusionParameter + "' in 'obfuscateFlowUnexclude' statement cannot use the '<link>' syntax. It will be ignored.", true);
					} else if (distinctInclusionParameter.isMethodTarget() && distinctInclusionParameter.hasLinkMethodNamePattern()) {
						super.scriptContext
							.logWarningInternal("Parameter '" + distinctInclusionParameter + "' in 'obfuscateFlowUnexclude' statement cannot use the '<link>' syntax. It will be ignored.", true);
					} else if (distinctInclusionParameter.isMethodTarget() && distinctInclusionParameter.isClassSpecifierRecursive()) {
						super.scriptContext
							.logWarningInternal("Parameter '" + distinctInclusionParameter + "' in 'obfuscateFlowUnexclude' statement cannot use the '^' syntax. It will be ignored.", true);
					} else {
						if (distinctInclusionParameter.isMethodTarget() && distinctInclusionParameter.hasIncludeSignatureClasses()) {
							super.scriptContext
								.logWarningInternal("Parameter '" + distinctInclusionParameter + "' in 'obfuscateFlowUnexclude' cannot use the '+signatureClasses' keyword. It will be ignored.", true);
						} else if (distinctInclusionParameter.isClassTarget() && distinctInclusionParameter.isClassWildcard()) {
							super.scriptContext.logWarningInternal("Parameter '" + distinctInclusionParameter + "' in 'obfuscateFlowUnexclude' cannot use the '+' tag. The tag will be ignored.", true);
						}

						distinctInclusionParameter.includeFlowObfuscation(this);
					}
				} else {
					super.scriptContext
						.logWarningInternal(
							"Parameter '" + distinctInclusionParameter + "' in 'obfuscateFlowUnexclude' statement is not a class or method exclusion parameter. It will be ignored.", true
						);
				}
			}
		}
	}

	public boolean isClassFlowExcluded(ClassNode classNode) {
		return super.excludedClasses.containsKey(classNode);
	}
}
