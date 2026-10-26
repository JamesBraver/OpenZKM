package com.zelix.framework.hierarchy;

import com.zelix.framework.signature.MethodSignature;
import com.zelix.framework.wrappers.ClassNode;
import com.zelix.klassmaster.script.ScriptContext;
import com.zelix.klassmaster.script.statements.annotation.ComplexAnnotationSpecifier;
import com.zelix.klassmaster.script.statements.exclusion.GroupingsStatement;
import com.zelix.klassmaster.script.statements.specifier.RenameFilterParameter;
import com.zelix.repository.ClassProvider;
import com.zelix.utils.GlobalUtils;
import com.zelix.utils.collections.DualKeyMap;
import com.zelix.utils.collections.readOnly.ReadOnlyRegistryView;
import com.zelix.workspace.Workspace;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class GroupingsHierarchy implements ClassHierarchy {
	private DualKeyMap<String, RenameFilterParameter, RenameFilterParameter> groupingParameters = new DualKeyMap<>();
	private DualKeyMap<String, ClassNode, ClassNode> groupingClasses = new DualKeyMap<>();
	private Workspace workspace;
	private List<GroupingsStatement> groupingsStatements;
	private ScriptContext scriptContext;

	public GroupingsHierarchy(Workspace workspace, List<GroupingsStatement> groupingsStatements, ScriptContext scriptContext) {
		this.workspace = workspace;
		this.groupingsStatements = groupingsStatements;
		this.scriptContext = scriptContext;
		if (workspace.hasClasses()) {
			this.buildGroupingParameters();
			this.resolveGroupingClasses();
		}
	}

	@Override
	public final boolean isInstanceOf(String className, String target, ComplexAnnotationSpecifier annotationSpecifier) {
		return this.workspace.isInstanceOf(className, target, annotationSpecifier);
	}

	@Override
	public final boolean hasHierarchySnapshot() {
		return this.workspace.hasHierarchySnapshot();
	}

	@Override
	public final boolean isObjectMethodSignature(MethodSignature methodSignature) {
		return this.workspace.isObjectMethodSignature(methodSignature);
	}

	private final void buildGroupingParameters() {
		int nextGroupingNumber = 0;
		int statementCount = this.groupingsStatements.size();

		for (int statementIndex = 0; statementIndex < statementCount; statementIndex++) {
			ArrayList<ArrayList<RenameFilterParameter>> groupingParameterLists = this.groupingsStatements.get(statementIndex).getGroupingParameters();

			for (int groupIndex = 0; groupIndex < groupingParameterLists.size(); groupIndex++) {
				String groupKey = "GROUPING_" + nextGroupingNumber++;
				ArrayList<RenameFilterParameter> groupParameters = groupingParameterLists.get(groupIndex);

				for (int parameterIndex = 0; parameterIndex < groupParameters.size(); parameterIndex++) {
					RenameFilterParameter filterParameter = groupParameters.get(parameterIndex);
					this.groupingParameters.put(groupKey, filterParameter, filterParameter);
				}
			}
		}
	}

	public void printGroupingParameters() {
		PrintWriter writer = this.scriptContext.getWriter();
		ArrayList<String> sortedGroupKeys = new ArrayList<>();
		Enumeration<String> groupKeyEnumeration = this.groupingParameters.getGroupKeys();

		while (groupKeyEnumeration.hasMoreElements()) {
			String groupKey = groupKeyEnumeration.nextElement();
			sortedGroupKeys.add(groupKey);
		}

		Collections.sort(sortedGroupKeys);
		writer.println("\tGroupings parameters:");

		for (int i = 0; i < sortedGroupKeys.size(); i++) {
			String sortedGroupKey = sortedGroupKeys.get(i);
			writer.println("\t\t\"" + sortedGroupKey + "\"");
			Iterator<RenameFilterParameter> filterIterator = this.groupingParameters.getGroup(sortedGroupKey).keySet().iterator();

			while (filterIterator.hasNext()) {
				writer.println("\t\t\t\"" + filterIterator.next().toRuleString() + "\"");
			}
		}
	}

	@Override
	public final boolean isSubclassOf(String className, String target) {
		return this.workspace.isSubclassOf(className, target);
	}

	@Override
	public Set<String> getAnnotationTypes(String className, Integer innerClassOrdinal, boolean matchOnOriginalNames) {
		return this.workspace.getAnnotationTypes(className, innerClassOrdinal, matchOnOriginalNames);
	}

	@Override
	public final boolean isAssignableFrom(String className, String target) {
		return this.workspace.isAssignableFrom(className, target);
	}

	@Override
	public ClassProvider getClassProvider() {
		return this.workspace.getClassProvider();
	}

	public ReadOnlyRegistryView<String, ClassNode, ClassNode> getGroupingClasses() {
		return new ReadOnlyRegistryView<>(this.groupingClasses);
	}

	@Override
	public final boolean isMethodNameObfFinish() {
		return this.workspace.isMethodNameObfFinish();
	}

	@Override
	public final boolean isSuperclassOf(String className, String target) {
		return this.workspace.isSuperclassOf(className, target);
	}

	public PrintWriter getWriter() {
		return this.scriptContext.getWriter();
	}

	public boolean isVerbose() {
		return this.scriptContext.isVerbose();
	}

	@Override
	public final boolean isInterfaceOf(String className, String target, ComplexAnnotationSpecifier annotationSpecifier) {
		return this.workspace.isInterfaceOf(className, target, annotationSpecifier);
	}

	private final void resolveGroupingClasses() {
		ArrayList<String> sortedGroupKeys = new ArrayList<>();
		Enumeration<String> groupKeyEnumeration = this.groupingParameters.getGroupKeys();

		while (groupKeyEnumeration.hasMoreElements()) {
			String groupKey = groupKeyEnumeration.nextElement();
			sortedGroupKeys.add(groupKey);
		}

		Collections.sort(sortedGroupKeys);

		for (int i = 0; i < sortedGroupKeys.size(); i++) {
			String sortedGroupKey = sortedGroupKeys.get(i);

			for (RenameFilterParameter filterParameter : this.groupingParameters.getGroup(sortedGroupKey).keySet()) {
				if (!filterParameter.isClassTarget()) {
					this.scriptContext.logWarningInternal("Parameter '" + filterParameter + "' in 'groupings' statement is not a class exclusion parameter. It will be ignored.", true);
				} else if (filterParameter.isLinkClass()) {
					this.scriptContext.logWarningInternal("Parameter '" + filterParameter + "' in 'groupings' statement cannot use the '<link>' syntax. It will be ignored.", true);
				} else {
					if (filterParameter.isClassWildcard()) {
						this.scriptContext.logWarningInternal("Parameter '" + filterParameter + "' in 'groupings' statement cannot use the '+' tag. The tag will be ignored.", true);
					}

					HashSet<ClassNode> matchingClasses = GlobalUtils.newHashSet();
					filterParameter.collectMatchingClasses(this, matchingClasses);
					if (matchingClasses.size() > 0) {
						for (ClassNode classNode : matchingClasses) {
							this.groupingClasses.put(sortedGroupKey, classNode, classNode);
						}
					}
				}
			}
		}
	}

	@Override
	public final boolean isClassInstanceOf(String className, String target) {
		return this.workspace.isClassInstanceOf(className, target);
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

	public Enumeration<ClassNode> getClasses() {
		return this.workspace.getClasses();
	}
}
