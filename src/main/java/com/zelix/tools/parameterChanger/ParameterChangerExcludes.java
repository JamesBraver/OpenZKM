package com.zelix.tools.parameterChanger;

import com.zelix.exceptions.LexicalException;
import com.zelix.exceptions.ParsingException;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.changelogs.parser.context.ChangeLogContext;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.constant.ApplicationEntryPoints;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.ScriptParser;
import com.zelix.klassmaster.script.base.AbstractScriptNode;
import com.zelix.klassmaster.script.statements.base.ExclusionInclusionStatement;
import com.zelix.klassmaster.script.statements.exclusion.DefaultMethodParameterChangesExcludeInput;
import com.zelix.klassmaster.script.statements.exclusion.DefaultMethodParameterObfuscationExcludeInput;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import com.zelix.repository.Hierarchy;
import com.zelix.tools.autoReflection.AutoReflectionExcludes;
import com.zelix.tools.excludes.DefaultExcludes;
import com.zelix.tools.excludes.ExcludeStatementManager;
import com.zelix.tools.excludes.FixedClassManager;
import com.zelix.tools.excludes.InclusionExclusionManager;
import com.zelix.tools.trim.TrimExcludedMemberSet;
import com.zelix.utils.FileUtils;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.IntegerUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.utils.enumerations.ArrayEnumeration;
import com.zelix.utils.references.AtomicReference;
import com.zelix.workspace.ClassHierarchyNode;
import com.zelix.workspace.Workspace;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;

public class ParameterChangerExcludes extends InclusionExclusionManager {
	public static final String DEFAULT_CHANGE_EXCLUSIONS = " *.* extends java.util.ResourceBundle <init>() and //Resources "
		+ Options.lineSeparator
		+ "                              *.* $deserializeLambda$(java.lang.invoke.SerializedLambda) and //Serialized lambda"
		+ Options.lineSeparator
		+ "                              interface *.* implements java.lang.annotation.Annotation public *(*) //Annotations"
		+ Options.lineSeparator
		+ "                              ;"
		+ Options.lineSeparator;
	public static final String DEFAULT_OBFUSCATION_EXCLUSIONS = " *.* extends java.util.ResourceBundle <init>() and //Resources "
		+ Options.lineSeparator
		+ "                              *.* $deserializeLambda$(java.lang.invoke.SerializedLambda) and //Serialized lambda"
		+ Options.lineSeparator
		+ "                              *.* lambda$*(*) and //Serialized lambda"
		+ Options.lineSeparator
		+ "                              interface *.* implements java.lang.annotation.Annotation public *(*) and //Annotations"
		+ Options.lineSeparator
		+ "                              *.* *(@*.* *) //Parameter annotations "
		+ Options.lineSeparator
		+ "                              ;"
		+ Options.lineSeparator;
	private final boolean isObfuscationMode;
	private final String modeDisplayName;
	private final String modeLowerCase;
	private final boolean hasActiveRules;

	public ParameterChangerExcludes(
		boolean isObfuscationMode,
		Workspace workspace,
		int keepInnerClassInfo,
		List<? extends ExclusionInclusionStatement> methodParameterChangesIncludeStatements,
		List<? extends ExclusionInclusionStatement> methodParameterChangesExcludeStatements,
		TrimExcludedMemberSet trimExcludedMemberSet,
		Set<MethodNode> methodsReferencingStrings,
		Map<MethodNode, String> entryPointSignatures,
		Set<MethodNode> indyReferencedMethods,
		Set<MethodNode> bootstrapReferencedMethods,
		MultiMap<MethodNode, MethodNode> methodReferenceMap,
		DefaultExcludes defaultExcludes,
		AutoReflectionExcludes autoReflectionExcludes,
		FixedClassManager fixedClassManager,
		ScriptContext scriptContext
	) throws IOException {
		super(workspace, methodParameterChangesIncludeStatements, methodParameterChangesExcludeStatements, scriptContext);
		this.isObfuscationMode = isObfuscationMode;
		if (isObfuscationMode) {
			this.modeDisplayName = "Method Parameter Obfuscation";
		} else {
			this.modeDisplayName = "Method Parameter List Changing";
		}

		this.modeLowerCase = this.modeDisplayName.toLowerCase();
		if (workspace.hasClasses()) {
			if (methodParameterChangesIncludeStatements != null && methodParameterChangesIncludeStatements.size() != 0) {
				this.initializeProcessableMaps(workspace.getClasses(), workspace.getWorkspaceClassesSize());
			} else {
				this.initializeExcludedMaps(workspace.getClasses(), workspace.getWorkspaceClassesSize());
			}

			this.applyIncludeExcludeStatements();
			this.excludeTrimExcludedMethods(trimExcludedMemberSet);
			this.excludeMethodsFromSet(methodsReferencingStrings, "Determined to be accessed by Reflection");
			this.excludeMethodsFromMap(entryPointSignatures);
			this.excludeMethodsFromSet(defaultExcludes.getRetainedPrefixMethods(), "Specified as having a retained prefix");
			if (isObfuscationMode) {
				this.excludeMethodsFromSet(indyReferencedMethods, "Referenced by invokedynamic");
				this.excludeMethodsFromSet(bootstrapReferencedMethods, "Referenced in bootstrap initialization");
			} else {
				HashSet<MethodNode> lambdaMetaFactoryMethods = new HashSet<>(indyReferencedMethods);
				if (!Options.ZKM_INDY_PARAM_CHANGING) {
					this.excludeMethodsFromSet(indyReferencedMethods, "Referenced by invokedynamic");
				} else {
					HashSet<MethodNode> indyExcludedMethods = new HashSet<>(13);

					for (MethodNode indyMethod : indyReferencedMethods) {
						if (!indyMethod.isLambdaMetaFactoryMarked() || indyMethod.getLambdaSamParamCount() == -1) {
							lambdaMetaFactoryMethods.remove(indyMethod);
							indyExcludedMethods.add(indyMethod);
						}
					}

					if (!indyExcludedMethods.isEmpty()) {
						this.excludeMethodsFromSet(indyExcludedMethods, "Referenced by invokedynamic");
					}
				}

				HashSet<MethodNode> bootstrapExcludedMethods = new HashSet<>(bootstrapReferencedMethods);
				bootstrapExcludedMethods.removeAll(lambdaMetaFactoryMethods);
				this.excludeMethodsFromSet(bootstrapExcludedMethods, "Referenced in bootstrap initialization");
			}

			this.excludeAutoReflectionMethods(autoReflectionExcludes);
			this.excludeFixedClassMethods(fixedClassManager, methodReferenceMap);
			if (keepInnerClassInfo != 1) {
				this.excludeSyntheticAccessMethods(workspace.getClasses(), keepInnerClassInfo, defaultExcludes);
			}

			this.excludeMultiReleaseMethods(workspace.getClasses());
			this.excludeRecordMethods(workspace.getClasses());
			this.hasActiveRules = trimExcludedMemberSet != null
				|| methodParameterChangesIncludeStatements != null && methodParameterChangesIncludeStatements.size() > 0
				|| methodParameterChangesExcludeStatements != null && methodParameterChangesExcludeStatements.size() > 0;
		} else {
			this.hasActiveRules = false;
		}
	}

	public static String toObjectArrayDescriptor(MethodNode methodNode) {
		return "([Ljava/lang/Object;)" + methodNode.getReturnDescriptor();
	}

	public static String toObjectArrayDescriptor(MethodSignature methodSignature) {
		return "([Ljava/lang/Object;)" + methodSignature.getReturnDescriptor();
	}

	public static boolean isProcessableMethod(AbstractMethodNode methodNode) {
		return methodNode.isWorkspaceNode()
			&& !methodNode.isNative()
			&& !methodNode.isClinit()
			&& methodNode.getParameterCount() + (methodNode.isStatic() ? 0 : 1) <= 253
			&& (!methodNode.isPublic() || !methodNode.isStatic() || !methodNode.getMethodSignature().equals(ApplicationEntryPoints.MAIN_METHOD_SIGNATURE));
	}

	private void excludeAutoReflectionMethods(AutoReflectionExcludes autoReflectionExcludes) {
		if (autoReflectionExcludes != null) {
			Enumeration<MethodNode> excludedMethodEnumeration = autoReflectionExcludes.getExcludedMethods();

			while (excludedMethodEnumeration.hasMoreElements()) {
				MethodNode methodNode = excludedMethodEnumeration.nextElement();
				if (isProcessableMethod(methodNode)) {
					this.includeMethod(methodNode, "Specified as accessed by Reflection");
				}
			}
		}
	}

	private void excludeMethodsFromSet(Set<MethodNode> methodNodes, String reason) {
		for (AbstractMethodNode methodNode : methodNodes) {
			if (isProcessableMethod(methodNode)) {
				this.includeMethod((MethodNode)methodNode, reason);
			}
		}
	}

	private ExclusionInclusionStatement parseExclusionStatement(ScriptContext scriptContext, BufferedReader reader) throws IOException {
		ScriptParser scriptParser = new ScriptParser(reader);

		AbstractScriptNode parsedScriptNode;
		try {
			if (this.isObfuscationMode()) {
				parsedScriptNode = scriptParser.parseDefaultMethodParameterObfuscationExcludeInput();
			} else {
				parsedScriptNode = scriptParser.parseDefaultMethodParameterChangesExcludeInput();
			}

			parsedScriptNode.parseArguments(null, scriptContext);
		} finally {
			try {
				reader.close();
			} catch (IOException ignored) {
			}
		}

		ExclusionInclusionStatement exclusionStatement;
		if (this.isObfuscationMode()) {
			exclusionStatement = ((DefaultMethodParameterObfuscationExcludeInput)parsedScriptNode).getParsedStatement();
		} else {
			exclusionStatement = ((DefaultMethodParameterChangesExcludeInput)parsedScriptNode).getParsedStatement();
		}

		return exclusionStatement;
	}

	private void excludeMultiReleaseMethods(Enumeration<ClassNode> classNodes) {
		while (classNodes.hasMoreElements()) {
			ClassNode classNode = classNodes.nextElement();
			if (classNode.hasInnerClassInfo()) {
				for (MethodNode methodNode : classNode.getMethodNodes()) {
					if (isProcessableMethod(methodNode)) {
						this.includeMethod(methodNode, "Multi-release versions of containing class");
					}
				}
			}
		}
	}

	public final void excludeMethodWithReason(MethodNode methodNode, String reason) {
		if (!methodNode.isClinit()) {
			ClassNode methodOwner = this.processableMethods.remove(methodNode);
			if (methodOwner != null) {
				this.excludedMethods.put(methodNode, methodOwner);
				super.scriptContext
					.logWarning(
						"Including method \""
							+ ExcludeStatementManager.formatMethodForDisplay(methodNode, this)
							+ "\" in class \""
							+ this.formatClassForDisplay(methodNode.getMethodOwner())
							+ "\" for possible "
							+ this.modeLowerCase
							+ " because \""
							+ reason
							+ "\""
					);
			}
		}
	}

	private void excludeTrimExcludedMethods(TrimExcludedMemberSet trimExcludedMemberSet) {
		if (trimExcludedMemberSet != null) {
			this.excludeMethodsFromSet(trimExcludedMemberSet.getMethods(), "Direct specification in Trim exclusion");
		}
	}

	private void excludeFixedClassMethods(FixedClassManager fixedClassManager, MultiMap<MethodNode, MethodNode> methodReferenceMap) {
		if (fixedClassManager != null) {
			ReferenceGraph<MethodNode, MethodNode> reverseReferenceGraph = new ReferenceGraph<>();

			for (Entry<MethodNode, List<MethodNode>> referenceEntry : methodReferenceMap.entrySet()) {
				MethodNode referencedMethod = referenceEntry.getKey();

				for (MethodNode referencingMethod : referenceEntry.getValue()) {
					reverseReferenceGraph.addReference(referencingMethod, referencedMethod);
				}
			}

			Enumeration<ClassNode> fixedClassEnumeration = fixedClassManager.enumerateFixedClasses();

			while (fixedClassEnumeration.hasMoreElements()) {
				ClassNode fixedClassNode = fixedClassEnumeration.nextElement();
				String fixedClassReason = "Fixed class '" + this.formatClassForDisplay(fixedClassNode) + "'";

				for (MethodNode methodNode : fixedClassNode.getMethodNodes()) {
					this.includeMethod(methodNode, fixedClassReason);
					Set<MethodNode> accessedMethods = reverseReferenceGraph.getReferences(methodNode);
					if (accessedMethods != null) {
						for (AbstractMethodNode accessedMethod : accessedMethods) {
							if (accessedMethod.isWorkspaceNode() && !fixedClassManager.isInFixedClasses((ClassNode)accessedMethod.getClassNode())) {
								String accessReason = "Fixed class '" + this.formatClassForDisplay(fixedClassNode) + "' accesses it";
								this.includeMethod((MethodNode)accessedMethod, accessReason);
							}
						}
					}
				}
			}
		}
	}

	public final void initializeExcludedMaps(Enumeration<ClassNode> classNodes, int capacity) {
		this.initializeMapsWithCapacity(capacity);

		while (classNodes.hasMoreElements()) {
			ClassNode classNode = classNodes.nextElement();
			super.excludedClasses.put(classNode, classNode);
			ArrayEnumeration<MethodNode> methodEnumeration = classNode.getMethodEnumeration();

			while (methodEnumeration.hasMoreElements()) {
				MethodNode methodNode = methodEnumeration.nextElement();
				if (isProcessableMethod(methodNode)) {
					this.excludedMethods.put(methodNode, methodNode.getMethodOwner());
				} else {
					this.processableMethods.put(methodNode, methodNode.getMethodOwner());
				}
			}
		}
	}

	private boolean validateFilterParameter(RenameFilterParameter filterParameter, String statementName) {
		if (!filterParameter.isMethodTarget()) {
			super.scriptContext
				.logWarningInternal(
					this.modeDisplayName + " : Parameter '" + filterParameter + "' in '" + statementName + "' statement is not a method exclusion parameter. It will be ignored.", true
				);
			return false;
		} else if (filterParameter.isClassTarget() && filterParameter.isLinkClass()) {
			super.scriptContext
				.logWarningInternal(
					this.modeDisplayName + " : Parameter '" + filterParameter + "' in '" + statementName + "' statement cannot use the '<link>' syntax. It will be ignored.", true
				);
			return false;
		} else if (filterParameter.isMethodTarget() && filterParameter.hasLinkMethodNamePattern()) {
			super.scriptContext
				.logWarningInternal(
					this.modeDisplayName + " : Parameter '" + filterParameter + "' in '" + statementName + "' statement cannot use the '<link>' syntax. It will be ignored.", true
				);
			return false;
		} else if (filterParameter.isMethodTarget() && filterParameter.hasIncludeSignatureClasses()) {
			super.scriptContext
				.logWarningInternal(
					this.modeDisplayName + " : Parameter '" + filterParameter + "' in '" + statementName + "' cannot use the '+signatureClasses' keyword. It will be ignored.", true
				);
			return false;
		} else {
			return true;
		}
	}

	private void revertToProcessable(Enumeration<ClassNode> classNodes) {
		while (classNodes.hasMoreElements()) {
			ClassNode classNode = classNodes.nextElement();

			for (MethodNode methodNode : classNode.getMethodNodes()) {
				if (isProcessableMethod(methodNode) && (!methodNode.isConstructor() && !methodNode.isNameChanged() || methodNode.isConstructor() && !classNode.isNameChanged())) {
					this.includeMethodWithReason(
						methodNode,
						methodNode.isConstructor()
							? "No trimExclude, methodParameterChangesInclude or methodParameterChangesExclude statements active and class name not obfuscated"
							: "No trimExclude, methodParameterChangesInclude or methodParameterChangesExclude statements active and method name not obfuscated"
					);
				}
			}
		}
	}

	public void excludeMethodsFromCollection(Collection<AbstractMethodNode> methods, String reason, ChangeLogContext changeLogContext, Hierarchy hierarchy, FixedClassManager fixedClassManager) {
		for (AbstractMethodNode method : methods) {
			if (method.isWorkspaceNode()) {
				boolean exclusionApplied = false;
				AbstractMethodNode baseMethod = null;
				if (!method.isPrivate() && !method.isStatic()) {
					baseMethod = hierarchy.getBaseMethod(method);
				}

				if (!this.isMethodExcluded((MethodNode)method)) {
					this.excludeMethodWithReason((MethodNode)method, reason);
					exclusionApplied = true;
					if (fixedClassManager != null) {
						fixedClassManager.recordMethodChange((MethodNode)method, changeLogContext.getLogFileName(), false);
					}
				}

				if (baseMethod != null && baseMethod.isWorkspaceNode() && !this.isMethodExcluded((MethodNode)baseMethod)) {
					this.excludeMethodWithReason(
						(MethodNode)baseMethod,
						reason + " for a linked method in the class '" + ExcludeStatementManager.formatClassForDisplay(method.getClassNode(), this.workspace) + "'"
					);
					exclusionApplied = true;
					if (fixedClassManager != null) {
						fixedClassManager.recordMethodChange((MethodNode)baseMethod, changeLogContext.getLogFileName(), false);
					}
				}

				if (exclusionApplied) {
					ClassHierarchyNode ownerHierarchyNode = ClassHierarchyNode.getNodeByInternalName(method.getInternalName());
					MethodSignature methodSignature = method.getMethodSignature();
					AtomicReference<List<ClassHierarchyNode>> implementationsRef = hierarchy.getMethodImplementationsRef(ownerHierarchyNode, methodSignature);
					if (implementationsRef != null) {
						for (ClassHierarchyNode implementingClassNode : implementationsRef.get()) {
							if (implementingClassNode.isWorkspaceClass() && implementingClassNode != ownerHierarchyNode) {
								MethodNode implementationMethod = this.workspace.getMethodBySignature(implementingClassNode.getClassNodeIfWorkspace(), methodSignature);
								if (implementationMethod != null) {
									this.excludeMethodWithReason(
										implementationMethod, reason + " for a linked method in the class '" + ExcludeStatementManager.formatClassForDisplay(method.getClassNode(), this.workspace) + "'"
									);
									if (fixedClassManager != null) {
										fixedClassManager.recordMethodChange(implementationMethod, changeLogContext.getLogFileName(), false);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public void applyDefaultExclusions() {
		if (!this.hasActiveRules) {
			this.revertToProcessable(this.workspace.getClasses());
		}
	}

	@Override
	public final boolean excludeClass(ClassNode classNode, String reason) {
		Object removedClass = super.processableClasses.remove(classNode);
		if (removedClass != null) {
			super.excludedClasses.put(classNode, classNode);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				super.logWriter
					.println("\tMatched as included for possible method parameter changes \"" + this.formatClassForDisplay(classNode) + "\" because of \"" + reason + "\"");
			}
		}

		return removedClass != null;
	}

	public ExclusionInclusionStatement loadDefaultExclusions(ScriptContext scriptContext) throws IOException {
		String defaultExclusionsPath;
		if (this.isObfuscationMode) {
			defaultExclusionsPath = scriptContext.getDefaultMethodParamObfuscationExcludePath();
		} else {
			defaultExclusionsPath = scriptContext.getDefaultMethodParamChangesExcludePath();
		}

		boolean usingInternalDefaults = false;

		BufferedReader reader;
		try {
			reader = FileUtils.createBufferedReader(new File(defaultExclusionsPath), Options.ZKM_SCRIPT_ENCODING);
			scriptContext.logRaw("Using '" + defaultExclusionsPath + "' for default method parameter " + (this.isObfuscationMode ? "obfuscation" : "changes") + " exclusions", true);
		} catch (IOException ioException) {
			reader = new BufferedReader(
				new StringReader(
					(this.isObfuscationMode ? "methodParameterObfuscationExclude" : "methodParameterChangesExclude")
						+ (this.isObfuscationMode ? DEFAULT_OBFUSCATION_EXCLUSIONS : DEFAULT_CHANGE_EXCLUSIONS)
				)
			);
			usingInternalDefaults = true;
			scriptContext.logRaw(
				"Using internal default method parameter "
					+ (this.isObfuscationMode ? "obfuscation" : "changes")
					+ " exclusions. Error opening file '"
					+ defaultExclusionsPath
					+ "' : "
					+ ioException,
				true
			);
		}

		try {
			return this.parseExclusionStatement(scriptContext, reader);
		} catch (ParsingException parsingException) {
			if (!usingInternalDefaults) {
				return this.fallbackToDefaultExclusions(scriptContext, parsingException);
			}
		} catch (LexicalException lexicalException) {
			if (!usingInternalDefaults) {
				return this.fallbackToDefaultExclusions(scriptContext, lexicalException);
			}
		}

		return null;
	}

	public final void includeMethodWithReason(MethodNode methodNode, String reason) {
		ClassNode methodOwner = this.excludedMethods.remove(methodNode);
		if (methodOwner != null) {
			this.processableMethods.put(methodNode, methodOwner);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				super.logWriter
					.println(
						"\tEXcluding from possible "
							+ this.modeLowerCase
							+ " method \""
							+ ExcludeStatementManager.formatMethodForDisplay(methodNode, this)
							+ "\" in class \""
							+ ExcludeStatementManager.getFormattedClassSignature(methodNode.getMethodOwner(), this.workspace, false)
							+ "\" because of \""
							+ reason
							+ "\""
					);
			}
		}
	}

	@Override
	public final void includeClass(ClassNode classNode, String reason) {
		if (super.excludedClasses.remove(classNode) != null) {
			super.processableClasses.put(classNode, classNode);
			if (super.scriptContext.isVerbose()) {
				super.logWriter
					.println("\tMatched as excluded from possible method parameter changes \"" + this.formatClassForDisplay(classNode) + "\" because of \"" + reason + "\"");
			}
		}
	}

	public final void initializeProcessableMaps(Enumeration<ClassNode> classNodes, int capacity) {
		this.initializeMapsWithCapacity(capacity);

		while (classNodes.hasMoreElements()) {
			ClassNode classNode = classNodes.nextElement();
			super.processableClasses.put(classNode, classNode);
			ArrayEnumeration<MethodNode> methodEnumeration = classNode.getMethodEnumeration();

			while (methodEnumeration.hasMoreElements()) {
				MethodNode methodNode = methodEnumeration.nextElement();
				this.processableMethods.put(methodNode, methodNode.getMethodOwner());
			}
		}
	}

	private void excludeMethodsFromMap(Map<MethodNode, String> reasonByMethod) {
		for (Entry<MethodNode, String> reasonEntry : reasonByMethod.entrySet()) {
			MethodNode methodNode = reasonEntry.getKey();
			if (isProcessableMethod(methodNode)) {
				this.includeMethod(methodNode, reasonEntry.getValue());
			}
		}
	}

	public final void includeMethod(MethodNode methodNode, String reason) {
		ClassNode methodOwner = this.excludedMethods.remove(methodNode);
		if (methodOwner != null) {
			this.processableMethods.put(methodNode, methodOwner);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				ClassNode displayOwnerClass = methodNode.getMethodOwner();
				super.logWriter
					.println(
						"\tMatched as EXcluded from possible "
							+ this.modeLowerCase
							+ " \""
							+ ExcludeStatementManager.formatMethodForDisplay(methodNode, this)
							+ "\" in class \""
							+ this.formatClassForDisplay(displayOwnerClass)
							+ "\" because of \""
							+ reason
							+ "\""
					);
			}
		}
	}

	private void excludeSyntheticAccessMethods(Enumeration<ClassNode> classNodes, int keepInnerClassInfo, DefaultExcludes defaultExcludes) {
		String keepInnerClassInfoReason = "'keepInnerClassInfo specified as '" + (keepInnerClassInfo == 0 ? "true" : "ifNameNotObfuscated") + "'";

		while (classNodes.hasMoreElements()) {
			ClassNode classNode = classNodes.nextElement();

			for (MethodNode methodNode : classNode.getMethodNodes()) {
				if (methodNode.isSynthetic() && !methodNode.isNameChanged()) {
					String methodDisplayName = methodNode.getNameDisplay();
					if (methodDisplayName.startsWith("access$") && IntegerUtils.isInteger(methodDisplayName.substring(7))) {
						if (keepInnerClassInfo != 0 && keepInnerClassInfo == 2 && !defaultExcludes.isPackageExcluded(classNode.getPackageName()) && !defaultExcludes.isClassExcluded(classNode)) {
						}

						this.includeMethod(methodNode, keepInnerClassInfoReason);
					}
				}
			}
		}
	}

	public final void excludeMethodDueToOther(MethodNode excludedMethod, MethodNode linkedMethod) {
		if (!excludedMethod.isClinit()) {
			ClassNode methodOwner = this.excludedMethods.remove(excludedMethod);
			if (methodOwner != null) {
				this.processableMethods.put(excludedMethod, methodOwner);
				if (super.scriptContext.isVerbose() && super.logWriter != null) {
					super.logWriter
						.println(
							"\tEXcluding from possible "
								+ this.modeLowerCase
								+ " method \""
								+ ExcludeStatementManager.getFormattedMethodSignature(excludedMethod, false, this.workspace)
								+ "\" in class \""
								+ ExcludeStatementManager.getFormattedClassSignature(excludedMethod.getMethodOwner(), this.workspace, false)
								+ "\" because method \""
								+ ExcludeStatementManager.getFormattedMethodSignature(linkedMethod, false, this.workspace)
								+ "\" in class \""
								+ ExcludeStatementManager.getFormattedClassSignature(linkedMethod.getMethodOwner(), this.workspace, false)
								+ "\" has been excluded"
						);
				}
			}
		}
	}

	public void applyChangeLogParamObfuscation(ChangeLogContext changeLogContext, Hierarchy hierarchy, DefaultExcludes defaultExcludes, FixedClassManager fixedClassManager) {
		if (changeLogContext != null) {
			Set<AbstractMethodNode> obfuscatedParameterMethods = changeLogContext.getParameterObfuscatedMethods();
			if (obfuscatedParameterMethods != null) {
				this.excludeMethodsFromCollection(obfuscatedParameterMethods, "Input ChangeLog specifies obfuscated parameters", changeLogContext, hierarchy, fixedClassManager);
			}
		}
	}

	public final void excludeMethod(MethodNode methodNode, String reason) {
		ClassNode methodOwner = this.processableMethods.remove(methodNode);
		if (methodOwner != null) {
			this.excludedMethods.put(methodNode, methodOwner);
			if (super.scriptContext.isVerbose() && super.logWriter != null) {
				ClassNode displayOwnerClass = methodNode.getMethodOwner();
				super.logWriter
					.println(
						"\tMatched as included for possible "
							+ this.modeLowerCase
							+ " \""
							+ ExcludeStatementManager.formatMethodForDisplay(methodNode, this)
							+ "\" in class \""
							+ this.formatClassForDisplay(displayOwnerClass)
							+ "\" because of \""
							+ reason
							+ "\""
					);
			}
		}
	}

	public void applyChangeLogParamChanges(ChangeLogContext changeLogContext, Hierarchy hierarchy, FixedClassManager fixedClassManager) {
		if (changeLogContext != null) {
			List<AbstractMethodNode> changedParameterMethods = changeLogContext.getChangedParameterMethods();
			this.excludeMethodsFromCollection(changedParameterMethods, "Input ChangeLog specifies changed parameters", changeLogContext, hierarchy, fixedClassManager);
		}
	}

	private final void applyIncludeExcludeStatements() throws IOException {
		int includeStatementCount;
		if (super.exclusionStatements == null) {
			includeStatementCount = 0;
		} else {
			includeStatementCount = super.exclusionStatements.size();
		}

		HashSet<RenameFilterParameter> seenInclusionParameters = GlobalUtils.newHashSet();
		ArrayList<RenameFilterParameter> distinctInclusionParameters = new ArrayList<>();

		for (int statementIndex = 0; statementIndex < includeStatementCount; statementIndex++) {
			Enumeration<RenameFilterParameter> inclusionParameterEnumeration = super.exclusionStatements.get(statementIndex).getRenameFilterParameters();

			while (inclusionParameterEnumeration.hasMoreElements()) {
				RenameFilterParameter inclusionParameter = inclusionParameterEnumeration.nextElement();
				if (seenInclusionParameters.add(inclusionParameter)) {
					distinctInclusionParameters.add(inclusionParameter);
				}
			}
		}

		if (super.scriptContext.isVerbose() && distinctInclusionParameters.size() > 0) {
			super.logWriter.println("\t" + (this.isObfuscationMode ? "methodParameterObfuscationInclude" : "methodParameterChangesInclude") + " parameters:");

			for (int inclusionDisplayIndex = distinctInclusionParameters.size() - 1; inclusionDisplayIndex >= 0; inclusionDisplayIndex--) {
				super.logWriter.println("\t\t" + distinctInclusionParameters.get(inclusionDisplayIndex));
			}
		}

		for (int inclusionApplyIndex = distinctInclusionParameters.size() - 1; inclusionApplyIndex >= 0; inclusionApplyIndex--) {
			RenameFilterParameter inclusionParameterToApply = distinctInclusionParameters.get(inclusionApplyIndex);
			if (this.validateFilterParameter(inclusionParameterToApply, this.isObfuscationMode ? "methodParameterObfuscationInclude" : "methodParameterChangesInclude")) {
				this.warnOnHigherLevelScope(inclusionParameterToApply, this.isObfuscationMode ? "methodParameterObfuscationInclude" : "methodParameterChangesInclude");
				inclusionParameterToApply.includeParameterChanger(this);
			}
		}

		int excludeStatementCount;
		if (super.inclusionStatements == null) {
			excludeStatementCount = 0;
		} else {
			excludeStatementCount = super.inclusionStatements.size();
		}

		HashSet<RenameFilterParameter> seenExclusionParameters = GlobalUtils.newHashSet();
		ArrayList<RenameFilterParameter> distinctExclusionParameters = new ArrayList<>();

		for (int excludeStatementIndex = 0; excludeStatementIndex < excludeStatementCount; excludeStatementIndex++) {
			Enumeration<RenameFilterParameter> exclusionParameterEnumeration = super.inclusionStatements.get(excludeStatementIndex).getRenameFilterParameters();

			while (exclusionParameterEnumeration.hasMoreElements()) {
				RenameFilterParameter exclusionParameter = exclusionParameterEnumeration.nextElement();
				if (seenExclusionParameters.add(exclusionParameter)) {
					distinctExclusionParameters.add(exclusionParameter);
				}
			}
		}

		if (super.scriptContext.isVerbose() && distinctExclusionParameters.size() > 0) {
			super.logWriter.println("\t" + (this.isObfuscationMode ? "methodParameterObfuscationExclude" : "methodParameterChangesExclude") + " parameters:");

			for (int exclusionDisplayIndex = distinctExclusionParameters.size() - 1; exclusionDisplayIndex >= 0; exclusionDisplayIndex--) {
				super.logWriter.println("\t\t\"" + distinctExclusionParameters.get(exclusionDisplayIndex) + "\"");
			}
		}

		for (int exclusionApplyIndex = distinctExclusionParameters.size() - 1; exclusionApplyIndex >= 0; exclusionApplyIndex--) {
			RenameFilterParameter exclusionParameterToApply = distinctExclusionParameters.get(exclusionApplyIndex);
			if (this.validateFilterParameter(exclusionParameterToApply, this.isObfuscationMode ? "methodParameterObfuscationExclude" : "methodParameterChangesExclude")) {
				this.warnOnHigherLevelScope(exclusionParameterToApply, this.isObfuscationMode ? "methodParameterObfuscationExclude" : "methodParameterChangesExclude");
				exclusionParameterToApply.excludeParameterChanger(this);
			}
		}

		ExclusionInclusionStatement defaultStatement = this.loadDefaultExclusions(super.scriptContext);
		if (defaultStatement != null) {
			HashMap<RenameFilterParameter, RenameFilterParameter> seenDefaultParameters = GlobalUtils.newHashMap();
			ArrayList<RenameFilterParameter> distinctDefaultParameters = new ArrayList<>();
			Enumeration<RenameFilterParameter> defaultParameterEnumeration = defaultStatement.getRenameFilterParameters();

			while (defaultParameterEnumeration.hasMoreElements()) {
				RenameFilterParameter defaultParameter = defaultParameterEnumeration.nextElement();
				if (!seenDefaultParameters.containsKey(defaultParameter)) {
					seenDefaultParameters.put(defaultParameter, defaultParameter);
					distinctDefaultParameters.add(defaultParameter);
				}
			}

			Collections.sort(distinctDefaultParameters);
			if (super.scriptContext.isVerbose() && distinctDefaultParameters.size() > 0) {
				super.logWriter.println("\tDefault method parameter " + (this.isObfuscationMode ? "obfuscation" : "changes") + " exclusion parameters:");

				for (int defaultDisplayIndex = distinctDefaultParameters.size() - 1; defaultDisplayIndex >= 0; defaultDisplayIndex--) {
					super.logWriter.println("\t\t\"" + distinctDefaultParameters.get(defaultDisplayIndex) + "\"");
				}
			}

			for (int defaultApplyIndex = distinctDefaultParameters.size() - 1; defaultApplyIndex >= 0; defaultApplyIndex--) {
				RenameFilterParameter defaultParameterToApply = distinctDefaultParameters.get(defaultApplyIndex);
				if (this.validateFilterParameter(defaultParameterToApply, this.isObfuscationMode ? "methodParameterObfuscationExclude" : "methodParameterChangesExclude")) {
					this.warnOnHigherLevelScope(defaultParameterToApply, this.isObfuscationMode ? "methodParameterObfuscationExclude" : "methodParameterChangesExclude");
					defaultParameterToApply.excludeParameterChanger(this);
				}
			}
		}
	}

	private void warnOnHigherLevelScope(RenameFilterParameter filterParameter, String statementName) {
		if (filterParameter.hasExplicitPackageOrClass()) {
			super.scriptContext
				.logWarningInternal(
					this.modeDisplayName
						+ " : Parameter '"
						+ filterParameter
						+ "' in '"
						+ statementName
						+ "' cannot match higher levels (e.g. containing class or package). The higher level specification will be ignored.",
					true
				);
		}
	}

	public final void excludeMethodSilently(MethodNode methodNode) {
		ClassNode methodOwner = this.processableMethods.remove(methodNode);
		if (methodOwner != null) {
			this.excludedMethods.put(methodNode, methodOwner);
		}
	}

	public boolean isObfuscationMode() {
		return this.isObfuscationMode;
	}

	private void initializeMapsWithCapacity(int capacity) {
		super.processableClasses = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(capacity));
		super.excludedClasses = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(capacity));
		this.processableMethods = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(capacity * 5));
		this.excludedMethods = GlobalUtils.newHashMap(GlobalUtils.getInitialCapacity(capacity * 5));
	}

	private void excludeRecordMethods(Enumeration<ClassNode> classNodes) {
		while (classNodes.hasMoreElements()) {
			ClassNode classNode = classNodes.nextElement();
			if (classNode.getSuperclassName().equals("java/lang/Record")) {
				for (MethodNode methodNode : classNode.getMethodNodes()) {
					if (isProcessableMethod(methodNode)) {
						this.includeMethod(methodNode, "Containing class is a Record");
					}
				}
			}
		}
	}

	private ExclusionInclusionStatement fallbackToDefaultExclusions(ScriptContext scriptContext, Throwable cause) throws IOException {
		String defaultExclusionsPath;
		if (this.isObfuscationMode) {
			defaultExclusionsPath = scriptContext.getDefaultMethodParamObfuscationExcludePath();
		} else {
			defaultExclusionsPath = scriptContext.getDefaultMethodParamChangesExcludePath();
		}

		if (cause != null) {
			System.err.println("\"" + defaultExclusionsPath + "\" had a parse error and will be ignored. See \"" + scriptContext.getLogFilePath() + "\" for more detail.");
			scriptContext.logWarning(
				"\""
					+ defaultExclusionsPath
					+ "\" had a parse error :"
					+ Options.lineSeparator
					+ cause.getMessage()
					+ Options.lineSeparator
					+ "Will use the internal default method parameter "
					+ (this.isObfuscationMode ? "obfuscation" : "changes")
					+ " statement and ignore \""
					+ defaultExclusionsPath
					+ "\"."
			);
		}

		BufferedReader reader = new BufferedReader(
			new StringReader(
				(this.isObfuscationMode ? "methodParameterObfuscationExclude" : "methodParameterChangesExclude")
					+ (this.isObfuscationMode ? DEFAULT_OBFUSCATION_EXCLUSIONS : DEFAULT_CHANGE_EXCLUSIONS)
			)
		);

		try {
			return this.parseExclusionStatement(scriptContext, reader);
		} catch (ParsingException ignoredParse) {
		} catch (LexicalException ignoredLexical) {
		}

		return null;
	}
}
