package com.zelix.tools.removeCall;

import com.zelix.framework.AbstractClassNode;
import com.zelix.framework.AbstractMethodNode;
import com.zelix.framework.MemberNode;
import com.zelix.framework.code.InstructionListNode;
import com.zelix.framework.reference.ReferenceGraph;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.exclusion.RemoveMethodCallsExcludeStatement;
import com.zelix.klassmaster.script.statements.exclusion.RemoveMethodCallsIncludeStatement;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import com.zelix.tools.excludes.ExcludeStatementManager;
import com.zelix.tools.excludes.base.AbstractReferenceExcludeManager;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.MultiMap;
import com.zelix.workspace.Workspace;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class RemoveMethodCallExcludes extends AbstractReferenceExcludeManager {
	private MultiMap<AbstractMethodNode, InstructionListNode> callSitesByMethod;
	private ReferenceGraph<AbstractMethodNode, InstructionListNode> matchedCallSites;

	public RemoveMethodCallExcludes(
		Workspace workspace,
		MultiMap<AbstractMethodNode, InstructionListNode> callSitesByMethod,
		List<RemoveMethodCallsIncludeStatement> includeStatements,
		List<RemoveMethodCallsExcludeStatement> excludeStatements,
		ScriptContext scriptContext
	) {
		super(workspace, includeStatements, excludeStatements, scriptContext);
		this.callSitesByMethod = callSitesByMethod;
		if (workspace.hasClasses()) {
			Set<AbstractMethodNode> methodsWithCallSites = this.callSitesByMethod.keySet();
			int initialCapacity = GlobalUtils.getInitialCapacity(methodsWithCallSites.size());
			super.excludedMethods = GlobalUtils.newHashSet(initialCapacity);
			super.includedMethods = GlobalUtils.newHashSet(initialCapacity);
			this.matchedCallSites = new ReferenceGraph<>(initialCapacity);
			if ((includeStatements == null || includeStatements.size() == 0) && excludeStatements != null && excludeStatements.size() > 0) {
				super.includedMethods.addAll(methodsWithCallSites);

				for (AbstractMethodNode methodNode : methodsWithCallSites) {
					this.matchedCallSites.addReference(methodNode, null);
				}
			} else {
				super.excludedMethods.addAll(methodsWithCallSites);
			}

			this.applyIncludeExcludeFilters();
		}

		this.callSitesByMethod = null;
	}

	public final void excludeMethod(AbstractMethodNode methodNode, InstructionListNode callSite, String reason) {
		if (this.matchedCallSites.hasMember(methodNode)) {
			if (callSite == null) {
				StringBuilder messageBuffer = new StringBuilder();
				messageBuffer.append("\tMatched for remove method calls exclusion method \"");
				messageBuffer.append(ExcludeStatementManager.formatMethodForDisplay(methodNode, this));
				messageBuffer.append("\" in class \"");
				messageBuffer.append(this.formatClassForDisplay(methodNode.getClassNode()));
				messageBuffer.append("\" referenced from any methods because of \"");
				messageBuffer.append(reason);
				messageBuffer.append("\"");
				super.logWriter.println(messageBuffer.toString());
				this.markExcluded(methodNode);
				this.matchedCallSites.removeMember(methodNode);
			} else {
				if (this.matchedCallSites.isReferencedBy(methodNode, null)) {
					this.matchedCallSites.removeReference(methodNode, null);
					this.matchedCallSites.addReferences(methodNode, this.callSitesByMethod.get(methodNode));
				}

				boolean referenceRemoved = this.matchedCallSites.removeReference(methodNode, callSite);
				if (!this.matchedCallSites.hasMember(methodNode)) {
					this.markExcluded(methodNode);
				}

				if (referenceRemoved && super.scriptContext.isVerbose() && super.logWriter != null) {
					StringBuilder callSiteMessageBuffer = new StringBuilder();
					callSiteMessageBuffer.append("\tMatched for remove method calls exclusion method \"");
					callSiteMessageBuffer.append(ExcludeStatementManager.formatMethodForDisplay(methodNode, this));
					callSiteMessageBuffer.append("\" in class \"");
					callSiteMessageBuffer.append(this.formatClassForDisplay(methodNode.getClassNode()));
					callSiteMessageBuffer.append("\" contained in \"");
					callSiteMessageBuffer.append(ExcludeStatementManager.formatMethodForDisplay(callSite.getMethodNode(), this));
					callSiteMessageBuffer.append("\" in class \"");
					callSiteMessageBuffer.append(this.formatClassForDisplay(callSite.getClassNode()));
					callSiteMessageBuffer.append("\"");
					callSiteMessageBuffer.append(" because of \"");
					callSiteMessageBuffer.append(reason);
					callSiteMessageBuffer.append("\"");
					super.logWriter.println(callSiteMessageBuffer.toString());
				}
			}
		}
	}

	private void warnOnHigherLevelScope(RenameFilterParameter filterParameter, String statementName) {
		if (filterParameter.isContainedInMethodTarget() && filterParameter.hasContainedInClause()) {
			super.scriptContext
				.logWarningInternal(
					"Remove Method Calls : Parameter '"
						+ filterParameter
						+ "' in '"
						+ statementName
						+ "' has a 'containedIn' clause which matches higher levels. The higher level specification will be ignored.",
					true
				);
		}

		if (filterParameter.isContainedInMethodTarget() && filterParameter.hasExplicitPackageOrClass()) {
			super.scriptContext
				.logWarningInternal(
					"Remove Method Calls : Parameter '"
						+ filterParameter
						+ "' in '"
						+ statementName
						+ "' cannot match higher levels (e.g. containing class or package). The higher level specification will be ignored.",
					true
				);
		}
	}

	public boolean isCallSiteMatched(AbstractMethodNode methodNode, InstructionListNode callSite) {
		return this.matchedCallSites.isReferencedBy(methodNode, null) || this.matchedCallSites.isReferencedBy(methodNode, callSite);
	}

	public final void markExcluded(AbstractMethodNode methodNode) {
		if (super.includedMethods.remove(methodNode)) {
			super.excludedMethods.add(methodNode);
		}
	}

	public Set<InstructionListNode> getCallSites(MemberNode memberNode) {
		return GlobalUtils.newHashSet(this.callSitesByMethod.get((AbstractMethodNode)memberNode));
	}

	@Override
	public Enumeration<AbstractClassNode> getAnnotationSearchClasses() {
		return null;
	}

	public final void markIncluded(AbstractMethodNode methodNode) {
		if (super.excludedMethods.remove(methodNode)) {
			super.includedMethods.add(methodNode);
		}
	}

	private final void applyIncludeExcludeFilters() {
		int includeStatementCount = super.exclusionStatements == null ? 0 : super.exclusionStatements.size();
		HashSet<RenameFilterParameter> seenIncludeFilters = GlobalUtils.newHashSet();
		ArrayList<RenameFilterParameter> distinctIncludeFilters = new ArrayList<>();

		for (int includeStatementIndex = 0; includeStatementIndex < includeStatementCount; includeStatementIndex++) {
			Enumeration<RenameFilterParameter> includeFilterEnumeration = super.exclusionStatements.get(includeStatementIndex).getRenameFilterParameters();

			while (includeFilterEnumeration.hasMoreElements()) {
				RenameFilterParameter includeFilter = includeFilterEnumeration.nextElement();
				if (seenIncludeFilters.add(includeFilter)) {
					distinctIncludeFilters.add(includeFilter);
				}
			}
		}

		if (super.scriptContext.isVerbose() && distinctIncludeFilters.size() > 0) {
			super.logWriter.println("\tremoveMethodCallsInclude parameters:");

			for (int includeLogIndex = distinctIncludeFilters.size() - 1; includeLogIndex >= 0; includeLogIndex--) {
				super.logWriter.println("\t\t" + distinctIncludeFilters.get(includeLogIndex));
			}
		}

		for (int includeApplyIndex = distinctIncludeFilters.size() - 1; includeApplyIndex >= 0; includeApplyIndex--) {
			RenameFilterParameter distinctIncludeFilter = distinctIncludeFilters.get(includeApplyIndex);
			if (this.validateFilterParameter(distinctIncludeFilter, "removeMethodCallsInclude")) {
				this.warnOnHigherLevelScope(distinctIncludeFilter, "removeMethodCallsInclude");
				distinctIncludeFilter.includeRemoveMethodCall(this);
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
			Enumeration<RenameFilterParameter> excludeFilterEnumeration = super.excludeStatements.get(excludeStatementIndex).getRenameFilterParameters();

			while (excludeFilterEnumeration.hasMoreElements()) {
				RenameFilterParameter excludeFilter = excludeFilterEnumeration.nextElement();
				if (!seenExcludeFilters.containsKey(excludeFilter)) {
					seenExcludeFilters.put(excludeFilter, excludeFilter);
					distinctExcludeFilters.addElement(excludeFilter);
				}
			}
		}

		if (super.scriptContext.isVerbose() && distinctExcludeFilters.size() > 0) {
			super.logWriter.println("\tremoveMethodCallsExclude parameters:");

			for (int excludeLogIndex = distinctExcludeFilters.size() - 1; excludeLogIndex >= 0; excludeLogIndex--) {
				super.logWriter.println("\t\t\"" + distinctExcludeFilters.elementAt(excludeLogIndex) + "\"");
			}
		}

		for (int excludeApplyIndex = distinctExcludeFilters.size() - 1; excludeApplyIndex >= 0; excludeApplyIndex--) {
			RenameFilterParameter distinctExcludeFilter = distinctExcludeFilters.elementAt(excludeApplyIndex);
			if (this.validateFilterParameter(distinctExcludeFilter, "removeMethodCallsExclude")) {
				this.warnOnHigherLevelScope(distinctExcludeFilter, "removeMethodCallsExclude");
				distinctExcludeFilter.excludeRemoveMethodCall(this);
			}
		}
	}

	public final void includeMethod(AbstractMethodNode methodNode, InstructionListNode callSite, String reason) {
		if (this.matchedCallSites.isReferencedBy(methodNode, null)) {
		}

		if (callSite == null) {
			this.matchedCallSites.removeMember(methodNode);
		}

		this.markIncluded(methodNode);
		if (this.matchedCallSites.addReference(methodNode, callSite) && super.scriptContext.isVerbose() && super.logWriter != null) {
			StringBuilder messageBuffer = new StringBuilder();
			messageBuffer.append("\tMatched for remove method calls inclusion method \"");
			messageBuffer.append(ExcludeStatementManager.formatMethodForDisplay(methodNode, this));
			messageBuffer.append("\" in class \"");
			messageBuffer.append(this.formatClassForDisplay(methodNode.getClassNode()));
			if (callSite != null) {
				messageBuffer.append("\" contained in \"");
				messageBuffer.append(ExcludeStatementManager.formatMethodForDisplay(callSite.getMethodNode(), this));
				messageBuffer.append("\" in class \"");
				messageBuffer.append(this.formatClassForDisplay(callSite.getClassNode()));
				messageBuffer.append("\"");
			} else {
				messageBuffer.append("\" contained in any methods");
			}

			messageBuffer.append(" because of \"");
			messageBuffer.append(reason);
			messageBuffer.append("\"");
			super.logWriter.println(messageBuffer.toString());
		}
	}

	private boolean validateFilterParameter(RenameFilterParameter filterParameter, String statementName) {
		boolean valid = true;
		if (!filterParameter.isMethodTarget()) {
			super.scriptContext
				.logWarningInternal(
					"Remove Method Calls : Parameter '" + filterParameter + "' in '" + statementName + "' statement is not a method exclusion parameter. It will be ignored.", true
				);
			valid = false;
		}

		if (filterParameter.isClassTarget() && filterParameter.isLinkClass()) {
			super.scriptContext
				.logWarningInternal("Remove Method Calls : Parameter '" + filterParameter + "' in '" + statementName + "' statement cannot use the '<link>' syntax. It will be ignored.", true);
			valid = false;
		}

		if (filterParameter.isMethodTarget() && filterParameter.hasLinkMethodNamePattern()) {
			super.scriptContext
				.logWarningInternal("Remove Method Calls : Parameter '" + filterParameter + "' in '" + statementName + "' statement cannot use the '<link>' syntax. It will be ignored.", true);
			valid = false;
		}

		if (filterParameter.isMethodTarget() && filterParameter.hasIncludeSignatureClasses()) {
			super.scriptContext
				.logWarningInternal(
					"Remove Method Calls : Parameter '" + filterParameter + "' in '" + statementName + "' cannot use the '+signatureClasses' keyword. It will be ignored.", true
				);
			valid = false;
		}

		if (!filterParameter.isContainedInMethodTarget()) {
			super.scriptContext
				.logWarningInternal(
					"Remove Method Calls : Parameter '"
						+ filterParameter
						+ "' in '"
						+ statementName
						+ "' has a 'containedIn' clause which does not contain a method specification parameter. It will be ignored.",
					true
				);
			valid = false;
		}

		if (filterParameter.isContainedInMethodTarget() && filterParameter.isContainedInLinkMethod()) {
			super.scriptContext
				.logWarningInternal(
					"Remove Method Calls : Parameter '" + filterParameter + "' in '" + statementName + "' has a 'containedIn' clause which uses the '<link>' syntax. It will be ignored.", true
				);
			valid = false;
		}

		if (filterParameter.isContainedInMethodTarget() && filterParameter.hasContainedInSignatureClasses()) {
			super.scriptContext
				.logWarningInternal(
					"Remove Method Calls : Parameter '"
						+ filterParameter
						+ "' in '"
						+ statementName
						+ "' has a 'containedIn' clause which uses the '+signatureClasses' keyword. It will be ignored.",
					true
				);
			valid = false;
		}

		return valid;
	}
}
