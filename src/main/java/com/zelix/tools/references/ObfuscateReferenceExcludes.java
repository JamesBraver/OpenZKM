package com.zelix.tools.references;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractFieldNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.MemberNode;
import com.zelix.framework.analyzer.StaticBlockAnalyzer;
import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.instructions.WideConstantPoolInsnNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.framework.reference.ReferenceGraphMap;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.framework.wrappers.MethodNode;
import com.zelix.klassmaster.config.Options;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.exclusion.ObfuscateReferencesExcludeStatement;
import com.zelix.klassmaster.script.statements.exclusion.ObfuscateReferencesIncludeStatement;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import com.zelix.tools.excludes.ExcludeStatementManager;
import com.zelix.tools.excludes.FixedClassManager;
import com.zelix.tools.excludes.base.AbstractReferenceExcludeManager;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.HierarchicalMultiMap;
import com.zelix.utils.dataset.MemberContext;
import com.zelix.utils.enumerations.ArrayEnumeration;
import com.zelix.workspace.Workspace;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

public class ObfuscateReferenceExcludes extends AbstractReferenceExcludeManager {
	private ReferenceGraph<AbstractFieldNode, InstructionListNode> includedFieldReferences = new ReferenceGraph<>();
	private ReferenceGraph<AbstractMethodNode, InstructionListNode> includedMethodReferences = new ReferenceGraph<>();
	private ReferenceGraph<MemberNode, InstructionListNode> memberReferences;
	private List<AbstractClassNode> annotationSearchClasses;

	public ObfuscateReferenceExcludes(
		Workspace workspace,
		ReferenceGraph<MemberContext, InstructionListNode> references,
		List<ObfuscateReferencesIncludeStatement> includeStatements,
		List<ObfuscateReferencesExcludeStatement> excludeStatements,
		ScriptContext scriptContext
	) {
		super(workspace, includeStatements, excludeStatements, scriptContext);
		this.memberReferences = new ReferenceGraph<>(references.size());

		for (Entry<MemberContext, Set<InstructionListNode>> adjacencyEntry : references.getAdjacencyEntries()) {
			this.memberReferences.addReferences(adjacencyEntry.getKey().getMemberNode(), adjacencyEntry.getValue());
		}

		if (workspace.hasClasses()) {
			int estimatedMemberCount = Math.max(this.memberReferences.size() / 5 + this.memberReferences.size(), 100);
			this.initializeTargetSets(estimatedMemberCount);
			if ((includeStatements == null || includeStatements.size() == 0) && excludeStatements != null && excludeStatements.size() > 0) {
				this.populateTargetSets(super.excludedClasses, super.includedFields, super.includedMethods, true);
			} else {
				this.populateTargetSets(super.includedClasses, super.excludedFields, super.excludedMethods, false);
			}

			this.applyScriptRules();
		}

		this.memberReferences = null;
		this.annotationSearchClasses = null;
	}

	public final void includeFieldReference(AbstractFieldNode field, InstructionListNode referencingInstruction, String reason) {
		if (this.includedFieldReferences.isReferencedBy(field, null)) {
		}

		if (referencingInstruction == null) {
			this.includedFieldReferences.removeMember(field);
		}

		this.markFieldIncluded(field, reason);
		if (this.includedFieldReferences.addReference(field, referencingInstruction) && super.scriptContext.isVerbose() && super.logWriter != null) {
			StringBuilder messageBuilder = new StringBuilder();
			messageBuilder.append("\tMatched for obfuscate references inclusion field \"");
			messageBuilder.append(ExcludeStatementManager.formatFieldForDisplay(field, this));
			messageBuilder.append("\" in class \"");
			messageBuilder.append(this.formatClassForDisplay(field.getClassNode()));
			if (referencingInstruction != null) {
				messageBuilder.append("\" referenced from \"");
				messageBuilder.append(ExcludeStatementManager.formatMethodForDisplay(referencingInstruction.getMethodNode(), this));
				messageBuilder.append("\" in class \"");
				messageBuilder.append(this.formatClassForDisplay(referencingInstruction.getClassNode()));
				messageBuilder.append("\"");
			} else {
				messageBuilder.append("\" referenced from any methods");
			}

			messageBuilder.append(" because of \"");
			messageBuilder.append(reason);
			messageBuilder.append("\"");
			super.logWriter.println(messageBuilder.toString());
		}
	}

	public final void markMethodExcluded(AbstractMethodNode method, String reason) {
		if (super.includedMethods.remove(method)) {
			super.excludedMethods.add(method);
		}
	}

	public final boolean isFieldIncluded(AbstractFieldNode field) {
		return this.includedFieldReferences.hasMember(field);
	}

	public final void markMethodIncluded(AbstractMethodNode method, String reason) {
		if (super.excludedMethods.remove(method)) {
			super.includedMethods.add(method);
		}
	}

	public Set<InstructionListNode> getReferencingInstructions(MemberNode member) {
		return GlobalUtils.newHashSet(this.memberReferences.getReferences(member));
	}

	private final void applyScriptRules() {
		int includeStatementCount;
		if (super.exclusionStatements == null) {
			includeStatementCount = 0;
		} else {
			includeStatementCount = super.exclusionStatements.size();
		}

		HashSet<RenameFilterParameter> seenIncludeFilters = GlobalUtils.newHashSet();
		ArrayList<RenameFilterParameter> distinctIncludeFilters = new ArrayList<>();

		for (int includeStatementIndex = 0; includeStatementIndex < includeStatementCount; includeStatementIndex++) {
			Enumeration<RenameFilterParameter> includeParameterEnumeration = super.exclusionStatements.get(includeStatementIndex).getRenameFilterParameters();

			while (includeParameterEnumeration.hasMoreElements()) {
				RenameFilterParameter includeFilter = includeParameterEnumeration.nextElement();
				if (seenIncludeFilters.add(includeFilter)) {
					distinctIncludeFilters.add(includeFilter);
				}
			}
		}

		if (super.scriptContext.isVerbose() && distinctIncludeFilters.size() > 0) {
			super.logWriter.println("\tobfuscateReferencesInclude parameters:");

			for (int includeLogIndex = distinctIncludeFilters.size() - 1; includeLogIndex >= 0; includeLogIndex--) {
				super.logWriter.println("\t\t" + distinctIncludeFilters.get(includeLogIndex));
			}
		}

		for (int includeFilterIndex = distinctIncludeFilters.size() - 1; includeFilterIndex >= 0; includeFilterIndex--) {
			RenameFilterParameter distinctIncludeFilter = distinctIncludeFilters.get(includeFilterIndex);
			if (this.isValidReferenceParameter(distinctIncludeFilter, "obfuscateReferencesInclude")) {
				this.logContainedInWarnings(distinctIncludeFilter, "obfuscateReferencesInclude");
				distinctIncludeFilter.includeObfuscateReference(this);
			}
		}

		int excludeStatementCount;
		if (super.excludeStatements == null) {
			excludeStatementCount = 0;
		} else {
			excludeStatementCount = super.excludeStatements.size();
		}

		HashMap<RenameFilterParameter, RenameFilterParameter> seenExcludeFilters = GlobalUtils.newHashMap();
		Vector<RenameFilterParameter> distinctExcludeFilters = new Vector<>();

		for (int excludeStatementIndex = 0; excludeStatementIndex < excludeStatementCount; excludeStatementIndex++) {
			Enumeration<RenameFilterParameter> excludeParameterEnumeration = super.excludeStatements.get(excludeStatementIndex).getRenameFilterParameters();

			while (excludeParameterEnumeration.hasMoreElements()) {
				RenameFilterParameter excludeFilter = excludeParameterEnumeration.nextElement();
				if (!seenExcludeFilters.containsKey(excludeFilter)) {
					seenExcludeFilters.put(excludeFilter, excludeFilter);
					distinctExcludeFilters.addElement(excludeFilter);
				}
			}
		}

		if (super.scriptContext.isVerbose() && distinctExcludeFilters.size() > 0) {
			super.logWriter.println("\tobfuscateReferencesExclude parameters:");

			for (int excludeLogIndex = distinctExcludeFilters.size() - 1; excludeLogIndex >= 0; excludeLogIndex--) {
				super.logWriter.println("\t\t\"" + distinctExcludeFilters.elementAt(excludeLogIndex) + "\"");
			}
		}

		for (int excludeFilterIndex = distinctExcludeFilters.size() - 1; excludeFilterIndex >= 0; excludeFilterIndex--) {
			RenameFilterParameter distinctExcludeFilter = distinctExcludeFilters.elementAt(excludeFilterIndex);
			if (this.isValidReferenceParameter(distinctExcludeFilter, "obfuscateReferencesExclude")) {
				this.logContainedInWarnings(distinctExcludeFilter, "obfuscateReferencesExclude");
				distinctExcludeFilter.excludeObfuscateReference(this);
			}
		}
	}

	public final void markFieldExcluded(AbstractFieldNode field, String reason) {
		if (super.includedFields.remove(field)) {
			super.excludedFields.add(field);
		}
	}

	private void logContainedInWarnings(RenameFilterParameter parameter, String statementName) {
		if (parameter.isContainedInMethodTarget() && parameter.hasContainedInClause()) {
			super.scriptContext
				.logWarningInternal(
					"Reference Obfuscation : Parameter '"
						+ parameter
						+ "' in '"
						+ statementName
						+ "' has a 'containedIn' clause which matches higher levels. The higher level specification will be ignored.",
					true
				);
		}

		if (parameter.isContainedInMethodTarget() && parameter.hasExplicitPackageOrClass()) {
			super.scriptContext
				.logWarningInternal(
					"Reference Obfuscation : Parameter '"
						+ parameter
						+ "' in '"
						+ statementName
						+ "' cannot match higher levels (e.g. containing class or package). The higher level specification will be ignored.",
					true
				);
		}
	}

	public final void includeMethodAnyReference(AbstractMethodNode method, String reason) {
		if (this.includedMethodReferences.addReference(method, (InstructionListNode)null)) {
			if (Options.logObfuscateRefInclusions && super.scriptContext.isVerbose() && super.logWriter != null) {
				StringBuilder messageBuilder = new StringBuilder();
				messageBuilder.append("\tMatched for obfuscate references inclusion method \"");
				messageBuilder.append(ExcludeStatementManager.formatMethodForDisplay(method, this));
				messageBuilder.append("\" in class \"");
				messageBuilder.append(this.formatClassForDisplay(method.getClassNode()));
				messageBuilder.append("\" referenced from any methods because of \"");
				messageBuilder.append(reason);
				messageBuilder.append("\"");
				super.logWriter.println(messageBuilder.toString());
			}

			super.includedMethods.add(method);
		}
	}

	public final void excludeMethodReference(AbstractMethodNode method, InstructionListNode referencingInstruction, String reason) {
		if (this.includedMethodReferences.hasMember(method)) {
			if (referencingInstruction == null) {
				if (super.scriptContext.isVerbose() && super.logWriter != null) {
					StringBuilder anyMethodsMessageBuilder = new StringBuilder();
					anyMethodsMessageBuilder.append("\tMatched for obfuscate references exclusion method \"");
					anyMethodsMessageBuilder.append(ExcludeStatementManager.formatMethodForDisplay(method, this));
					anyMethodsMessageBuilder.append("\" in class \"");
					anyMethodsMessageBuilder.append(this.formatClassForDisplay(method.getClassNode()));
					anyMethodsMessageBuilder.append("\" referenced from any methods because of \"");
					anyMethodsMessageBuilder.append(reason);
					anyMethodsMessageBuilder.append("\"");
					super.logWriter.println(anyMethodsMessageBuilder.toString());
				}

				this.markMethodExcluded(method, reason);
				this.includedMethodReferences.removeMember(method);
			} else {
				if (this.includedMethodReferences.isReferencedBy(method, null)) {
					this.includedMethodReferences.removeReference(method, null);
					this.includedMethodReferences.setReferences(method, this.memberReferences.getReferences(method));
				}

				boolean referenceRemoved = this.includedMethodReferences.removeReference(method, referencingInstruction);
				if (!this.includedMethodReferences.hasMember(method)) {
					this.markMethodExcluded(method, reason);
				}

				if (referenceRemoved && super.scriptContext.isVerbose() && super.logWriter != null) {
					StringBuilder specificMethodMessageBuilder = new StringBuilder();
					specificMethodMessageBuilder.append("\tMatched for obfuscate references exclusion method \"");
					specificMethodMessageBuilder.append(ExcludeStatementManager.formatMethodForDisplay(method, this));
					specificMethodMessageBuilder.append("\" in class \"");
					specificMethodMessageBuilder.append(this.formatClassForDisplay(method.getClassNode()));
					specificMethodMessageBuilder.append("\" referenced from \"");
					specificMethodMessageBuilder.append(ExcludeStatementManager.formatMethodForDisplay(referencingInstruction.getMethodNode(), this));
					specificMethodMessageBuilder.append("\" in class \"");
					specificMethodMessageBuilder.append(this.formatClassForDisplay(referencingInstruction.getClassNode()));
					specificMethodMessageBuilder.append("\"");
					specificMethodMessageBuilder.append(" because of \"");
					specificMethodMessageBuilder.append(reason);
					specificMethodMessageBuilder.append("\"");
					super.logWriter.println(specificMethodMessageBuilder.toString());
				}
			}
		}
	}

	public boolean isMethodReferenceIncluded(AbstractMethodNode method, InstructionListNode referencingInstruction) {
		if (!this.includedMethodReferences.hasMember(method)) {
			return false;
		} else {
			return this.includedMethodReferences.isReferencedBy(method, null) ? true : this.includedMethodReferences.isReferencedBy(method, referencingInstruction);
		}
	}

	private void initializeTargetSets(int memberCount) {
		super.includedClasses = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(memberCount));
		super.excludedClasses = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(memberCount));
		super.excludedFields = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(memberCount * 5));
		super.includedFields = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(memberCount * 5));
		super.excludedMethods = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(memberCount * 5));
		super.includedMethods = GlobalUtils.newHashSet(GlobalUtils.getInitialCapacity(memberCount * 5));
	}

	public HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> filterReferencesToObfuscate(
		ReferenceGraphMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> references, StaticBlockAnalyzer staticBlockAnalyzer, FixedClassManager fixedClassManager
	) {
		HierarchicalMultiMap<MemberContext, InstructionListNode, WideConstantPoolInsnNode> referencesToObfuscate = new HierarchicalMultiMap<>(Options.ZKM_USE_PARALLEL);

		for (Iterator<Entry<MemberContext, ReferenceGraph<InstructionListNode, WideConstantPoolInsnNode>>> entryIterator = references.getEntrySet().iterator();
			entryIterator.hasNext();
			entryIterator.remove()
		) {
			Entry<MemberContext, ReferenceGraph<InstructionListNode, WideConstantPoolInsnNode>> entry = entryIterator.next();
			MemberContext memberContext = entry.getKey();
			boolean memberIsField = memberContext.isField();
			MemberNode memberNode = memberContext.getMemberNode();
			if (memberIsField && this.isFieldIncluded((AbstractFieldNode)memberNode) || !memberIsField && this.isMethodIncluded((AbstractMethodNode)memberNode)) {
				for (Entry<InstructionListNode, Set<WideConstantPoolInsnNode>> adjacencyEntry : entry.getValue().getAdjacencyEntries()) {
					InstructionListNode instructionNode = adjacencyEntry.getKey();
					if (fixedClassManager != null && fixedClassManager.isInFixedClasses((ClassNode)instructionNode.getClassNode())) {
						if (super.scriptContext.isVerbose()) {
							String className = instructionNode.getClassNameBinary();
							super.scriptContext
								.logMessage(
									"Reference Obfuscation : References in method '"
										+ instructionNode.getMethodNode().getSignature()
										+ "' in class '"
										+ className
										+ "' will not be obfuscated because the containing class '"
										+ className
										+ "' is specified in a 'fixedClasses' statement."
								);
						}
					} else if (memberIsField && this.isFieldReferenceIncluded((AbstractFieldNode)memberNode, instructionNode) || !memberIsField && this.isMethodReferenceIncluded((AbstractMethodNode)memberNode, instructionNode)) {
						AbstractMethodNode methodNode = instructionNode.getMethodNode();
						if (!methodNode.isWorkspaceNode() || methodNode.isClinit() || !staticBlockAnalyzer.isCalledBySuperInitializer((MethodNode)methodNode)) {
							referencesToObfuscate.putAll(memberContext, instructionNode, adjacencyEntry.getValue());
						} else if (super.scriptContext.isVerbose()) {
							super.scriptContext
								.logMessage(
									"Reference Obfuscation : References in method '"
										+ methodNode.getSignature()
										+ "' in class '"
										+ methodNode.getClassNameBinary()
										+ "' will not be obfuscated because the method is called by the static initializer of a super class."
								);
						}
					}
				}
			}
		}

		return referencesToObfuscate;
	}

	@Override
	public Enumeration<AbstractClassNode> getAnnotationSearchClasses() {
		return ArrayEnumeration.newCollectionEnumeration(this.annotationSearchClasses);
	}

	public final boolean isMethodIncluded(AbstractMethodNode method) {
		return this.includedMethodReferences.hasMember(method);
	}

	public final void includeMethodReference(AbstractMethodNode method, InstructionListNode referencingInstruction, String reason) {
		if (this.includedMethodReferences.isReferencedBy(method, null)) {
		}

		if (referencingInstruction == null) {
			this.includedMethodReferences.removeMember(method);
		}

		this.markMethodIncluded(method, reason);
		if (this.includedMethodReferences.addReference(method, referencingInstruction) && super.scriptContext.isVerbose() && super.logWriter != null) {
			StringBuilder messageBuilder = new StringBuilder();
			messageBuilder.append("\tMatched for obfuscate references inclusion method \"");
			messageBuilder.append(ExcludeStatementManager.formatMethodForDisplay(method, this));
			messageBuilder.append("\" in class \"");
			messageBuilder.append(this.formatClassForDisplay(method.getClassNode()));
			if (referencingInstruction != null) {
				messageBuilder.append("\" referenced from \"");
				messageBuilder.append(ExcludeStatementManager.formatMethodForDisplay(referencingInstruction.getMethodNode(), this));
				messageBuilder.append("\" in class \"");
				messageBuilder.append(this.formatClassForDisplay(referencingInstruction.getClassNode()));
				messageBuilder.append("\"");
			} else {
				messageBuilder.append("\" referenced from any methods");
			}

			messageBuilder.append(" because of \"");
			messageBuilder.append(reason);
			messageBuilder.append("\"");
			super.logWriter.println(messageBuilder.toString());
		}
	}

	public boolean isFieldReferenceIncluded(AbstractFieldNode field, InstructionListNode referencingInstruction) {
		if (!this.includedFieldReferences.hasMember(field)) {
			return false;
		} else {
			return this.includedFieldReferences.isReferencedBy(field, null) ? true : this.includedFieldReferences.isReferencedBy(field, referencingInstruction);
		}
	}

	public final void excludeFieldReference(AbstractFieldNode field, InstructionListNode referencingInstruction, String reason) {
		if (this.includedFieldReferences.hasMember(field)) {
			if (referencingInstruction == null) {
				if (super.scriptContext.isVerbose() && super.logWriter != null) {
					StringBuilder anyMethodsMessageBuilder = new StringBuilder();
					anyMethodsMessageBuilder.append("\tMatched for obfuscate references exclusion field \"");
					anyMethodsMessageBuilder.append(ExcludeStatementManager.formatFieldForDisplay(field, this));
					anyMethodsMessageBuilder.append("\" in class \"");
					anyMethodsMessageBuilder.append(this.formatClassForDisplay(field.getClassNode()));
					anyMethodsMessageBuilder.append("\" referenced from any methods because of \"");
					anyMethodsMessageBuilder.append(reason);
					anyMethodsMessageBuilder.append("\"");
					super.logWriter.println(anyMethodsMessageBuilder.toString());
				}

				this.markFieldExcluded(field, reason);
				this.includedFieldReferences.removeMember(field);
			} else {
				if (this.includedFieldReferences.isReferencedBy(field, null)) {
					this.includedFieldReferences.removeReference(field, null);
					this.includedFieldReferences.setReferences(field, this.memberReferences.getReferences(field));
				}

				boolean referenceRemoved = this.includedFieldReferences.removeReference(field, referencingInstruction);
				if (!this.includedFieldReferences.hasMember(field)) {
					this.markFieldExcluded(field, reason);
				}

				if (referenceRemoved && super.scriptContext.isVerbose() && super.logWriter != null) {
					StringBuilder specificMethodMessageBuilder = new StringBuilder();
					specificMethodMessageBuilder.append("\tMatched for obfuscate references exclusion field \"");
					specificMethodMessageBuilder.append(ExcludeStatementManager.formatFieldForDisplay(field, this));
					specificMethodMessageBuilder.append("\" in class \"");
					specificMethodMessageBuilder.append(this.formatClassForDisplay(field.getClassNode()));
					specificMethodMessageBuilder.append("\" referenced from \"");
					specificMethodMessageBuilder.append(ExcludeStatementManager.formatMethodForDisplay(referencingInstruction.getMethodNode(), this));
					specificMethodMessageBuilder.append("\" in class \"");
					specificMethodMessageBuilder.append(this.formatClassForDisplay(referencingInstruction.getClassNode()));
					specificMethodMessageBuilder.append("\"");
					specificMethodMessageBuilder.append(" because of \"");
					specificMethodMessageBuilder.append(reason);
					specificMethodMessageBuilder.append("\"");
					super.logWriter.println(specificMethodMessageBuilder.toString());
				}
			}
		}
	}

	public final void markFieldIncluded(AbstractFieldNode field, String reason) {
		if (super.excludedFields.remove(field)) {
			super.includedFields.add(field);
		}
	}

	public final void populateTargetSets(Set<AbstractClassNode> classSet, Set<AbstractFieldNode> fieldSet, Set<AbstractMethodNode> methodSet, boolean markReferencedAnywhere) {
		HashSet<AbstractClassNode> staticClasses = GlobalUtils.newHashSet();

		for (MemberNode member : this.memberReferences.getMembers()) {
			AbstractClassNode classNode = member.getClassNode();
			if (member.isField()) {
				AbstractFieldNode field = (AbstractFieldNode)member;
				fieldSet.add(field);
				if (markReferencedAnywhere) {
					this.includedFieldReferences.addReference(field, (InstructionListNode)null);
				}
			} else {
				AbstractMethodNode method = (AbstractMethodNode)member;
				methodSet.add(method);
				if (markReferencedAnywhere) {
					this.includedMethodReferences.addReference(method, (InstructionListNode)null);
				}
			}

			if (member.isStatic()) {
				classSet.add(classNode);
				staticClasses.add(classNode);
			}
		}

		this.annotationSearchClasses = new ArrayList<>(staticClasses);
	}

	public final void includeFieldAnyReference(AbstractFieldNode field, String reason) {
		if (this.includedFieldReferences.addReference(field, (InstructionListNode)null)) {
			if (Options.logObfuscateRefInclusions && super.scriptContext.isVerbose() && super.logWriter != null) {
				StringBuilder messageBuilder = new StringBuilder();
				messageBuilder.append("\tMatched for obfuscate references inclusion field \"");
				messageBuilder.append(ExcludeStatementManager.formatFieldForDisplay(field, this));
				messageBuilder.append("\" in class \"");
				messageBuilder.append(this.formatClassForDisplay(field.getClassNode()));
				messageBuilder.append("\" referenced from any methods because of \"");
				messageBuilder.append(reason);
				messageBuilder.append("\"");
				super.logWriter.println(messageBuilder.toString());
			}

			super.includedFields.add(field);
		}
	}

	private boolean isValidReferenceParameter(RenameFilterParameter parameter, String statementName) {
		boolean parameterValid = true;
		if (!parameter.isMethodTarget() && !parameter.isFieldTarget()) {
			super.scriptContext
				.logWarningInternal(
					"Reference Obfuscation : Parameter '" + parameter + "' in '" + statementName + "' statement is not a field or method exclusion parameter. It will be ignored.", true
				);
			parameterValid = false;
		}

		if (parameter.isClassTarget() && parameter.isLinkClass()) {
			super.scriptContext
				.logWarningInternal(
					"Reference Obfuscation : Parameter '" + parameter + "' in '" + statementName + "' statement cannot use the '<link>' syntax. It will be ignored.", true
				);
			parameterValid = false;
		}

		if (parameter.isMethodTarget() && parameter.hasLinkMethodNamePattern()) {
			super.scriptContext
				.logWarningInternal(
					"Reference Obfuscation : Parameter '" + parameter + "' in '" + statementName + "' statement cannot use the '<link>' syntax. It will be ignored.", true
				);
			parameterValid = false;
		}

		if (parameter.isMethodTarget() && parameter.hasIncludeSignatureClasses()) {
			super.scriptContext
				.logWarningInternal(
					"Reference Obfuscation : Parameter '" + parameter + "' in '" + statementName + "' cannot use the '+signatureClasses' keyword. It will be ignored.", true
				);
			parameterValid = false;
		}

		if (!parameter.isContainedInMethodTarget()) {
			super.scriptContext
				.logWarningInternal(
					"Reference Obfuscation : Parameter '"
						+ parameter
						+ "' in '"
						+ statementName
						+ "' has a 'containedIn' clause which does not contain a method specification parameter. It will be ignored.",
					true
				);
			parameterValid = false;
		}

		if (parameter.isContainedInMethodTarget() && parameter.isContainedInLinkMethod()) {
			super.scriptContext
				.logWarningInternal(
					"Reference Obfuscation : Parameter '" + parameter + "' in '" + statementName + "' has a 'containedIn' clause which uses the '<link>' syntax. It will be ignored.", true
				);
			parameterValid = false;
		}

		if (parameter.isContainedInMethodTarget() && parameter.hasContainedInSignatureClasses()) {
			super.scriptContext
				.logWarningInternal(
					"Reference Obfuscation : Parameter '"
						+ parameter
						+ "' in '"
						+ statementName
						+ "' has a 'containedIn' clause which uses the '+signatureClasses' keyword. It will be ignored.",
					true
				);
			parameterValid = false;
		}

		return parameterValid;
	}
}
